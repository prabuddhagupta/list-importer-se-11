package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

import java.util.concurrent.TimeUnit

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

Connotate_Inter_American_Development_Bank ciadb = new Connotate_Inter_American_Development_Bank(context)
ciadb.initParsing()

class Connotate_Inter_American_Development_Bank {
    final ScrapianContext context
    def root = 'https://www.iadb.org'
    def mainUrl = root + '/en/transparency/sanctioned-firms-and-individuals'
    final entityType
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")

    //==================================================CHROME DRIVER CONTROLLER DEFINITION===============================================================//
    class ChromeDriverEngine {

        String CHROME_DRIVER_PATH = "/usr/bin/chromedriver"
        ChromeOptions options
        WebDriver driver

        ChromeDriverEngine() {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH)
            options = new ChromeOptions()

            // You can comment out or remove any switch you want
            options.addArguments(
                //"--headless", // Use this switch to use Chromium without any GUI
                "--disable-gpu",
                "--ignore-certificate-errors",
                "--window-size=1366,768", // Default window-size
                "--silent",
                "--blink-settings=imagesEnabled=false", // Don't load images
            )
            driver = new ChromeDriver(options)
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
        }

        /**
         * This method invokes and retrieves a URL
         * @param URL : The URL address
         * @param sleepTime : Wait {ms} after getting the URL
         */
        void get(String URL, int sleepTime = 0) {
            System.out.println("[SELENIUM] Invoking: [" + URL + "]")
            driver.get(URL)

            if (sleepTime != 0) {
                wait(sleepTime)
            }
        }

        /**
         * This method retrieves a web element by using it's xpath
         * in the current window.
         * It returns null if no element is found with the xpath.
         *
         * @param xpath : The xpath of the element
         * @return: null or WebElement
         */
        WebElement findByXpath(String xpath) {
            By by = new By.ByXPath(xpath)
            WebElement webElement

            try {
                webElement = driver.findElement(by)
            } catch (Exception e) {
                println("WebElement doesn't exist")
                webElement = null
            }

            return webElement == null ? null : webElement
        }

        /**
         * This method pauses the thread for the given amount of time
         * @param time : Sleep time in ms
         */
        void wait(int time) {
            try {
                Thread.sleep(time)
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
        }

        /**
         * This method stops the selenium driver and closes the window(s)
         */
        void shutdown() {
            driver.close()
            driver.quit()
        }

        /**
         * This page returns the HTML document of the current webpage as String
         * @return: String HTML document
         */
        String getSource() {
            return driver.getPageSource()
        }

    }

    Connotate_Inter_American_Development_Bank(ScrapianContext context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
    }

    def initParsing() {

        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(this.mainUrl, 20000)

        handlePagination(engine)
        engine.shutdown()
    }

    def handlePagination(ChromeDriverEngine engine) {

        ChromeDriverEngine tempEngine = engine
        String firstPageHtml = tempEngine.driver.findElement(By.tagName("tbody")).getAttribute("innerHTML")
        handleRow(firstPageHtml)
        sleep(5000)

        def nextPage = engine.getSource().contains("Next ›")
        while (nextPage) {

            String xPath = "//a[@class='d-none d-sm-inline-block']//*[text()='Next ›']"
            WebElement element = engine.findByXpath(xPath)
            JavascriptExecutor executor = (JavascriptExecutor) engine.driver
            executor.executeScript("arguments[0].click();", element)
            String bodyHtml = engine.driver.findElement(By.tagName("tbody")).getAttribute("innerHTML")
            handleRow(bodyHtml)
            Thread.sleep(5000)

            nextPage = engine.getSource().contains("Next ›")
        }
    }

    def handleRow(def sourceHtml) {
        def rowMatcher = sourceHtml =~ /(?is)(<tr>.*?<\/tr>)/
        def row
        while (rowMatcher) {
            row = rowMatcher.group(1)
            handleData(row)
        }
    }

    def handleData(def row) {
        row = fixHtml(row)
        def entityMatcher = row =~ /(?is)<tr><td\s*[^>]+>(.*?)<\/td><td\s*[^>]+>(.*?)<\/td><td\s*[^>]+>(.*?)<\/td><td\s*[^>]+>(.*?)<\/td><td\s*[^>]+>(.*?)<\/td><td\s*[^>]+>(.*?)<\/td><td\s*[^>]+>(.*?)<\/td><td\s*[^>]+>.*?<\/td><td\s*[^>]+>.*?<\/td><td\s*[^>]+>.*?<\/td><td\s*[^>]+>(.*?)<\/td><\/tr>/

        while (entityMatcher.find()) {
            def name, entityType, descr, eventDateFrom, eventDateTo, country, nationality, otherName
            def aliasList = []
            name = entityMatcher.group(1)
            entityType = entityMatcher.group(2)

            nationality = sanitizeNationality(entityMatcher.group(3)).toString().replaceAll(/\s*,$/, "")
            country = sanitizeCountry(entityMatcher.group(4))
            eventDateFrom = entityMatcher.group(5).toString().replaceAll("\\\\", "").replaceAll(/00:00:00/, "")
            eventDateTo = entityMatcher.group(6).replaceAll("\\\\", "").replaceAll(/00:00:00/, "").replaceAll(/Permanent/, "").replaceAll(/Indefinite/, "").replaceAll(/Ongoing/, "")

            descr = entityMatcher.group(7).toString().replaceAll(/\s*,$/, "")
                .replaceAll(/N\/A|Nonspecified/, "").trim()
                .replaceAll(/\s+/, " ").trim()

            otherName = entityMatcher.group(8)
            otherName = otherName.toString().replaceAll(/N\/A|Nonspecified/, "").trim()

            name = sanitizeName(name)

            def aliasMatcher
            def entityName
            def alias
            if (name =~ /(\(.*?\))/) {
                aliasMatcher = name =~ /(\(.*?\))/
                while (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1)

                    if (alias =~ /M&R/) {
                        aliasList = alias.split(/(?=M&R|M and R)/).collect({ it.trim() })

                        entityName = name.replaceAll(/\($alias\)/, "")
                        entityName = entityName.replaceAll(/[\)\(]/, "").trim()
                    } else {
                        entityName = name.replaceAll(/\($alias\)/, "")
                        entityName = entityName.replaceAll(/[\)\(]/, "").trim()
                        aliasList.add(alias)
                    }
                }
            } else if (name =~ /(?i)(?:\b(?:f.?n.?a.?)\b|d\/h\/a|(?:formerly|also)\s*(?:known|know)\s*as|(?:formerly|also)\s*\bd.?b.?a.?\b|\b(?:(?:a.?|f.?)k.?a.?)\b|(?:Currently|also)?\s*doing business (?:as)?|Formerly|(?:also trading.+?of)|also referred as)/) {
                aliasList = name.split(/(?i)(?:\b(?:f\.?n\.?a\.?)\b|d\/h\/a|(?:formerly|also)\s*(?:known|know)\s*as|(?:formerly|also)\s*\bd.?b.?a.?\b|\b(?:(?:a.?|f.?)k.?a.?)\b|(?:Currently|also)?\s*doing business (?:as)?|Formerly|(?:also trading.+?of)|also referred as)/).collect({ its ->
                    return its
                })
                entityName = (aliasList[0])
                aliasList.remove(0)
            } else if (name =~ /(".*")/) {
                aliasMatcher = name =~ /(".*")/
                if (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1)
                    entityName = name.replaceAll(/$alias/, "")
                    aliasList.add(alias)
                }
            } else {
                entityName = name
            }

            if (StringUtils.isNotBlank(otherName) || StringUtils.isNotEmpty(otherName)) {
                aliasList.add(otherName)
            }

            createEntity(entityName, entityType, nationality, country, aliasList, eventDateFrom, eventDateTo, descr)
        }
    }

    def createEntity(entityName, entityType, nationality, country, aliasList, eventDateFrom, eventDateTo, descr) {
        def entity
        descr = 'The entity has been sanctioned by the Inter-America Development Bank (IDB) for involvement in fraud, corruption, collusion or coercion, in violation of the IDB Group\'s anti-corruption policies. ' + descr.replaceAll(/(?s)\s+/, " ").trim()
        entityName = sanitizeName(entityName)
        if (StringUtils.isNotBlank(entityName) || StringUtils.isNotEmpty(entityName)) {

            def type
            if (entityType.toString().equalsIgnoreCase("Individual")) {
                type = "P"
            } else {
                type = "O"
            }
            entity = context.findEntity("name": entityName, type: type)

            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(entityNameFixing(entityName))
                entity.setType(type)
            }
            def address
            if (type == "O") {
                address = nationality + ", " + country
                address.toString().split(/(?:,|\sand\s(?!H))/).each { it ->
                    it = it.toString().replaceAll(/(?:\")/, "").trim()
                    it = it.toString().replaceAll(/(?:\s+)/, " ").trim()
                    if (it) {
                        ScrapeAddress scrapeAddress = new ScrapeAddress()
                        scrapeAddress.setCountry(it.trim())
                        entity.addAddress(scrapeAddress)
                    }
                }
            } else {
                nationality.toString().split(/(?:,|\sand\s(?!H))/).each { it ->
                    if (it) {
                        entity.addNationality(it.trim())
                    }
                }
                if (StringUtils.isEmpty(country)) {
                    nationality.toString().split(/(?:,|\sand\s(?!H))/).each { it ->
                        if (it) {
                            ScrapeAddress scrapeAddress = new ScrapeAddress()
                            scrapeAddress.setCountry(it.trim())
                            entity.addAddress(scrapeAddress)
                        }
                    }
                } else {
                    country.toString().split(/(?:,|\sand\s(?!H))/).each {
                        it = it.toString().replaceAll(/"/, "").trim()
                        if (it) {
                            ScrapeAddress scrapeAddress = new ScrapeAddress()
                            scrapeAddress.setCountry(it.trim())
                            entity.addAddress(scrapeAddress)
                        }
                    }
                }
            }

            if (aliasList) {
                aliasList.each { value ->
                    value = sanitizeAlias(value.toString().trim())
                    if (StringUtils.isNotEmpty(value) || StringUtils.isNotBlank(value)) {
                        entity.addAlias(value)
                    }
                }
            }

            ScrapeEvent event = new ScrapeEvent()
            descr = descr.toString().replaceAll(/\s+/, ' ').trim()
            event.setDescription(descr)

            if (eventDateFrom) {
                eventDateFrom = context.parseDate(new StringSource(eventDateFrom), ["MMM d, yyyy", "MMM dd, yyyy"] as String[])
                event.setDate(eventDateFrom)
            }
            if (eventDateTo) {
                eventDateTo = context.parseDate(new StringSource(eventDateTo), ["MMM d, yyyy", "MMM dd, yyyy"] as String[])
                event.setEndDate(eventDateTo)
            }

            entity.addEvent(event)
        }

    }

    def fixHtml(data) {
        data = data.toString().replaceAll(/(?i)\\u201cm(s|r)\.?\s*|/, "")
        data = data.toString().replaceAll(/(?i)\\u0022m(s|r)\.?\s*|/, "")
        data = data.toString().replaceAll(/(?i)\bir\./, "")
        data = data.toString().replaceAll(/(?<=Ltd\.) (?="Hunan Water")/, "f/k/a")
        return data.toString()
            .replaceAll(/c\\\\/o Desarrollo con Ingenier\\u00eda Contratistas Generales S\.A/, "")
            .replaceAll(/\\u0026/, "&")
            .replaceAll(/\u0130/, "i")
            .replaceAll(/\\u00a0/, "")
            .replaceAll(/\\u201c/, "")
            .replaceAll(/\\u201d/, "")
            .replaceAll(/\\u0022/, "")
            .replaceAll(/\\u2013/, "-")
            .replaceAll(/\\u0027/, "\'")
            .replaceAll(/[\“\”]/, "\'")
            .replaceAll(/\\t/, " ")
            .replaceAll(/\\r/, " ")
            .replaceAll(/\\n/, " ")
            .replaceAll(/\\\//, "/").replaceAll(/\\"/, "'")
    }

    def sanitizeName(data) {
        data = data.toString()
            .replaceAll(/(?:nbsp;)/, '')
            .replaceAll(/(?i)\((Singapore|India|China|Philippines|Nainital|Jaipur|Aligarh|Ludhiana|Hongkong|Kenya|NIGERIA|Beijing|Private|HK|Zimbabwe|Shangai|Harbin|Shenzen|UK|Guangdong)\)/, '$1')
            .replaceAll(/&amp;/, '&')
            .replaceAll(/\"Woodmanmebel\"/, 'Woodmanmebel')
            .replaceAll(/\(CNPJ: 20\. 182\.043\/0001-42\)/, '&')
            .replaceAll(/\s*\(\s*$/, "")
            .replaceAll(/\s*,\s*$/, "").trim()
            .replaceAll(/a\.k\.a\. Ecography Company$/, "")
            .replaceAll(/^(?:Mr\.?|M\/S\.?|Dr\.?|Ms\.?)/, "")
            .replaceAll(/(?<=Rodríguez)&/, "").trim()
            .replaceAll(/\s?&$/, "")
            .replaceAll(/\s+/, " ")
            .trim()
        return data
    }

    def entityNameFixing(def area) {
        area = area.toString().trim()
        area = area.replaceAll(/(?i)\s((?:Singapore|India|China|Philippines|Nainital|Jaipur|Aligarh|Ludhiana|Hongkong|Kenya|NIGERIA|Beijing|Private|HK|Zimbabwe|Shangai|Harbin))\s/, ' ($1) ')
        return area
    }

    def sanitizeAlias(data) {
        data = data.toString().trim()
            .replaceAll(/\s*\(\s*$/, "")
            .replaceAll(/\s*,\s*$/, "")
            .replaceAll(/:/, "")
            .replaceAll(/(?s)\s+/, " ")
            .replaceAll(/^\.\s*/, "")
            .replaceAll(/\)\s*$/, "").replaceAll(/\u0130/, "i")
            .replaceAll(/["\)\(']/, "").trim()
            .replaceAll(/^(?:Mr\.?|M\/S\.?|Dr\.?|Ms\.?)/, "")
            .replaceAll(/(?i)(?:Previously|Formerly|also) known as:?\s|formerly operating as:\s|^a\.k\.a\.\s|also doing business under\s/, "").trim()
            .replaceAll(/(?i)^(?:Formerly)\s/, "").trim()
            .replaceAll(/\s*(?:,|and)$/, "")
            .trim()
        return data
    }

    def sanitizeCountry(data) {
        data = data.toString().replaceAll(/Regional/, '')
        data = data.replaceAll(/N\\/A/, "").trim()
        return data.trim()
    }

    def sanitizeNationality(data) {
        data = data.toString().replaceAll(/Regional/, '')
        data = data.replaceAll(/N\\/A/, "").trim()
        return data.trim()
    }
}