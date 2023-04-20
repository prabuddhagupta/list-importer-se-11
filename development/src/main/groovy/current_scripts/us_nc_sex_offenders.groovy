import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import groovy.transform.Field
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import scrapian_scripts.utils.AddressMappingUtil

import java.text.ParseException
import java.util.concurrent.TimeUnit

context.setup([socketTimeout: 60000, connectionTimeout: 40000, retryCount: 5])
context.getSession().setEscape(true);
addressMapper = new AddressMappingUtil();
@Field
WebDriver driver

/*Selenium Part Start*/
String CHROME_DRIVER_PATH = "/usr/bin/chromedriver"
ChromeOptions options

System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH)

options = new ChromeOptions()

// You can comment out or remove any switch you want
options.addArguments(
    //"--headless", // Use this switch to use Chromium without any GUI
    "--disable-gpu",
    "--ignore-certificate-errors",
    "--window-size=1366,768", // Default window-size
    "--silent",
    // "--blink-settings=imagesEnabled=false" // Don't load images
)

driver = new ChromeDriver(options)
driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)

void shutdown() {
    driver.close()
    driver.quit()
}

/*Selenium Part End*/

def baseUrl = 'http://sexoffender.ncsbi.gov'

AUP_URL = baseUrl + '/disclaimer.aspx'
SEARCH_URL = baseUrl + '/search.aspx'
RESULT_URL = baseUrl + '/results.aspx'
DETAIL_URL = baseUrl + '/details.aspx?SRN='
IMAGE_URL = baseUrl + '/photo.aspx?srn='

//@Field
//String filePath = "/home/fabian/Documents/RDCScrapper/output.txt"
//@Field
//FileOutputStream outputStream = new FileOutputStream(filePath)
//@Field
//PrintStream output = new PrintStream(outputStream, true);

init(AUP_URL)


def init(baseUrl) {
    driver.navigate().to(baseUrl)
    Thread.sleep(20000)
    getEntityIds(driver).each { id, city -> createEntity(id, city) }
    shutdown()
}

def createEntity(entityId, city) {

    def url = DETAIL_URL + entityId

    /*driver.navigate().to(url)
    Thread.sleep(1000)
    def x = getRootNode(driver.getPageSource())*/

    def content = invokeUrl(url)
    def x = getRootNode(content)

    def html = x[0]
    // def page = new StringSource(x[1])
    def page = x[1]

    if (getDetail('name', html) == 'null') return

    def entity = context.getSession().newEntity()
    entity.setDataSourceId(entityId)
    entity.addUrl(url)
    entity.addImageUrl(IMAGE_URL + entityId)
    entity.setPersonName(context.parseName(getDetail('name', html)))
    entity.addSex(getDetail('sex', html))
    entity.addHeight(getDetail('height', html))
    entity.addRace(getDetail('race', html))
    entity.addWeight(getDetail('weight', html))

    def hair_color = getDetail('hair', html)
    if (StringUtils.isNotEmpty(hair_color) || StringUtils.isNotBlank(hair_color)) {
        entity.addHairColor(hair_color)
    }

    def eye_color = getDetail('eyes', html)
    if (StringUtils.isNotEmpty(eye_color) || StringUtils.isNotBlank(eye_color)) {
        entity.addHairColor(eye_color)
    }
    addAddress(entity, city, html)
    addEvents(entity, html)

    def markSec = context.regexMatch(page, [regex: 'MarkTattoo\">(.*?)</span>'])[1]
    //def markSec = context.regexMatch(page, [regex: 'NCICScarMarkTattoo">(.*?)</span>'])[1]
    getMatches(markSec, /(.*?)<BR>/).each { entity.addScarsMarks(it) }

    def aliasSec = context.regexMatch(page, [regex: 'alias">(.*?)</span>'])[1]
    getMatches(aliasSec, /(.*?)<BR>/).each { alias ->
        entity.addPersonAlias(context.parseName(alias))
    }

    def dob = getDetail('dob', html).replace('-', '/')
    if (isDate(dob)) entity.addDateOfBirth(dob)
}

def getDetail(detailName, html) {

    def details = [
        name  : html.'**'.find { it.@id == 'name' },
        sex   : html.'**'.find { it.@id == 'sex' },
        race  : html.'**'.find { it.@id == 'race' },
        height: html.'**'.find { it.@id == 'height' },
        weight: html.'**'.find { it.@id == 'weight' },
        hair  : html.'**'.find { it.@id == 'hair' },
        eyes  : html.'**'.find { it.@id == 'eyes' },
        dob   : html.'**'.find { it.@id == 'birthdate' },
    ]
    format(details[detailName])
}

def addEvents(entity, html) {
    html.'**'.find { it.@id == 'conviction_grid' }.tr.each { eventRow ->

        def table = eventRow.td.table
        def desc = format(table.tr[5].td[1])
        def date = format(table.tr[2].td[1]).replace('-', '/')
        def event = new ScrapeEvent();
        event.setDescription(desc)
        event.setDate(date)
        entity.addEvent(event);
    }
}

def addAddress(entity, city, html) {
    def div = html.'**'.find { it.span.@id == 'address' }
    if (!div) return
    def addrSet = false;
    def rawAddr = div.text().split(/\n/)[0]
    def addr1Match = rawAddr.replace(city, '') =~ /(.+),/
    def stateMatch = rawAddr =~ /, (\w{2}) \d{5}/
    def zipMatch = rawAddr =~ /, \w{2} (\d{5})/

    def addr = new ScrapeAddress();
    if (!city.contains("UNKNOWN")) {
        addrSet = true;
        addr.setCity(city)
    }
    if (addr1Match && !addr1Match[0][1].contains("UNKNOWN")) {
        addrSet = true;
        addr.setAddress1(addr1Match[0][1])
    }
    if (stateMatch && !stateMatch[0][1].contains("YY")) {
        addrSet = true;
        addr.setProvince(stateMatch[0][1])
    }
    if (zipMatch && !zipMatch[0][1].contains("00000")) {
        addrSet = true;
        addr.setPostalCode(zipMatch[0][1])
    }

    if (addrSet) {

        possibleState = addr.getProvince()
        if (possibleState) {
            possibleState = possibleState.replaceAll(/([A-Z])\.([A-Z])\./, '$1$2');
            possibleMappedState = addressMapper.normalizeState(possibleState);
            if (possibleState != possibleMappedState) {
                addr.setProvince(possibleMappedState);
                addr.setCountry("UNITED STATES");
            }
        }

        addr.setRawFormat(rawAddr)
        entity.addAddress(addr);
    }
}

def scrapeIds(html) {
    html = getRootNode(html)[0]

    def ids = [:]
    def re = /SRN=(.*)$/

    html.'**'.findAll { it.name() == 'tr' }.each { row ->

        if (row.td[0] == 'Name') return
        def entityId
        if ((row.td[0].a.@href =~ re)) {
            entityId = (row.td[0].a.@href =~ re)[0][1]
            ids[(entityId)] = format(row.td[3])
        }

    }
    ids
}


def getRootNode(content) {

    def parser = new org.ccil.cowan.tagsoup.Parser()
    parser.setFeature(parser.namespacesFeature, false)

    def slurper = new XmlSlurper(parser)
    [slurper.parse(new StringReader(content.toString())), content]

}


def getEntityIds(WebDriver driver) {
    def ids = [:]
    def content

    By locator = By.id("county")

    for (int i = 1; i < new Select(driver.findElement(locator)).getOptions().size(); i++) {
        new Select(driver.findElement(locator)).selectByIndex(i);
        Thread.sleep(2000)

        System.out.println("Options:" + new Select(driver.findElement(locator)).getFirstSelectedOption().getText());
        def buttonClick = driver.findElement(By.id("searchbutton1"))
        buttonClick.click()
        Thread.sleep(3000)

        content = driver.getPageSource()
        ids.putAll(scrapeIds(content))

        int lastPage

        def lastPageMatcher = driver.getPageSource() =~ />(\d+)<\/a><\/td>\s*<\/tr>\s*<\/tbody>\s*<\/table>/

        if (lastPageMatcher.find()) {
            lastPage = Integer.parseInt(lastPageMatcher.group(1))
        }
        int k = 1

        while (lastPage > k) {
            new WebDriverWait(driver, 3).until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"DaGrid\"]/tbody/tr[1]/td/a[$k]"))).click();
            ids.putAll(scrapeIds(driver.getPageSource()))
            k++
        }
        Thread.sleep(2000)
        driver.get("http://sexoffender.ncsbi.gov/search.aspx")
    }
    ids
}

def format(node) {
    str = node.toString().replace('\n', ' ')
    str = str.replaceAll('\u00A0', ' ') // &nbsp;
    str = str.replaceAll('\302', ' ') // &nbsp;
    str = str.replaceAll(' +', ' ')
    str = str.replaceAll('UNKNOWN', '').trim()
    str = str.replaceAll(/^null$/, '')

    return str.trim()

}

def isDate(s) {
    def format = 'MM/dd/yyyy'
    def date = null
    try {
        date = Date.parse(format, s)
    }
    catch (ParseException e) {
        return false
    }
    def now = new Date()
    def then = Date.parse(format, '01/01/1900')
    date.after(then) && date.before(now)
}

def getMatches(source, regex) {
    matches = []
    context.regexMatches(source, [regex: regex]).each { match ->
        matches.add(format(match[1]))
    }
    matches
}

def invokeUrl(url, type = null, paramsMap = null, cache = true, clean = true, miscData = [:]) {
    Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
    dataMap.putAll(miscData)
    return context.invoke(dataMap)
}

