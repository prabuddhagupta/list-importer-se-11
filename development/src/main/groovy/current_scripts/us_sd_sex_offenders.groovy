package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.io.FileUtils
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait


import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

context.setup([connectionTimeout: 500000, socketTimeout: 500000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

Us_Sd_Sex_Offenders script = new Us_Sd_Sex_Offenders(context)
script.initParsing()

class Us_Sd_Sex_Offenders {
    final addressParser
    final entityType
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final ScrapianContext context
    def url = 'https://sor.sd.gov/Home/Search?d=t'

    // ========================================= CHROME DRIVER CONTROLLER DEFINITION =========================================
    class ChromeDriverEngine {
        /**
         * Local installation path of the Chrome WebDriver
         */
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
                "--blink-settings=imagesEnabled=false" // Don't load images
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
         * This method retrieves a web element by using its id
         * @param id : String
         * @return WebElement
         */
        WebElement findById(String id) {
            By by = new By.ById(id)
            return driver.findElement(by)
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

    Us_Sd_Sex_Offenders(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser.updateCities([US: ["SPRINGFIELD", "RAPID CITY", "NORTH SIOUX CITY"]])

        this.context = context
    }

    def initParsing() {
        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(this.url, 5000)
        getData(engine)
        engine.shutdown()

    }


    def getData(ChromeDriverEngine engine) {

        WebElement btn = engine.findById("btnSearchFull")
        btn.click()
        engine.wait(30000)

        def nextPage = engine.findByXpath("//*[@id=\"pager1\"]/a[3]")
        nextPage.click()
        getData(engine.getSource())

        engine.wait(2000)
        //Code for next page
        while (nextPage) {
            def lastPage = engine.findByXpath("//*[@class=\"k-link k-pager-nav k-state-disabled\"]")
            if (lastPage) {
                break
            }

            nextPage = engine.findByXpath("//*[@id=\"pager1\"]/a[3]")
            nextPage.click()

            getData(engine.getSource())
            engine.wait(1000)
        }
    }

    def getData(def html) {

        def tableMatcher = html =~ /(<table role="grid">.*<\\/tbody><\\/table>)/
        def table
        if (tableMatcher.find()) {
            table = tableMatcher.group(1)
        }

        def row
        def rowMatcher = table =~ /(<tr class="(?:k-master-row|k-alt k-master-row)".*?<\/td><\/tr>)/
        while (rowMatcher.find()) {
            row = rowMatcher.group(1)
            handleRow(row)
        }
    }
    def i = 1

    def handleRow(def row) {
        def entityName
        def dob
        def street, city, zipCode
        def address

        def dataMatcher
        if ((dataMatcher = row =~ /showOffenderDetailFromGrid\(\d{1,5}\)"><u>(.*?)<\/u><\/a><\/td><td class="" role="gridcell">(\d{2}\/\d{1,2}\/\d{2,4})<\/td><td class="" role="gridcell">(.*?)<\/td><td class="" role="gridcell">(.*?)<\/td><td class="" role="gridcell">(.*?)<\/td>/)) {
            entityName = dataMatcher[0][1]
            entityName = entityName.toString().replaceAll(/([A-Z\-\s]+)\s*,\s*([A-Z\-\s]+)/, '$2 $1').trim()
            dob = dataMatcher[0][2]
            street = dataMatcher[0][3]
            city = dataMatcher[0][4]
            zipCode = dataMatcher[0][5]
            address = ", " + street + ", " + city + ", " + "SD" + ", USA " + zipCode
        }

        createEntity(entityName, address, dob)
    }


    def createEntity(def name, def address, def dateOfBirth) {

        def entity = null
        def description = "This entity appears on the South Dakota Sex Offender Registry"

        if (!name.toString().equals("") || !name.toString().isEmpty() || !name.toString().equals("null")) {
            def type = entityType.detectEntityType(name)
            entity = context.findEntity(["name": name, "type": type])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(type)
            }

            //Add Event
            ScrapeEvent event = new ScrapeEvent()
            event.setDescription(description)
            entity.addEvent(event)

            //Address
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            address = sanitizeAddress(address)
            if (!address.equals("")) {
                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress)
                }
            }

            //Date of Birth
            if (!dateOfBirth.toString().isEmpty()) {
                dateOfBirth = context.parseDate(new StringSource(dateOfBirth))
                entity.addDateOfBirth(dateOfBirth)
            }
        }
    }

    def sanitizeAddress(def address) {
        address = address.toString().replaceAll(/INCARCERATED\s*\/\s*(?:US MARSHAL CUSTODY|PRISON|CUSTODY|UNKNOWN)/, "").trim()
        address = address.toString().replaceAll(/(?:UNLOCATABLE|UNKNOWN)/, "").trim()
        address = address.toString().replaceAll(/NO\. SIOUX CITY/, "NORTH SIOUX CITY").trim()
        address = address.toString().replaceAll(/,\s*,/, ",").trim()
        address = address.replaceAll(/(?s)\s+/, " ").trim()
        return address.trim()
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().trim()
        street = street.toString().replaceAll(/,$/, "").trim()

        return street
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}