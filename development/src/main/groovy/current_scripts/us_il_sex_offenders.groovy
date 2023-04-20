package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

import java.util.concurrent.TimeUnit

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_Il_Sex_Offenders script = new Us_Il_Sex_Offenders(context)
script.initParsing()

class Us_Il_Sex_Offenders {

    final addressParser
    final entityType
    ScrapianContext context

    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")

    final def root = 'https://www.maine.gov/ag'
    final def url = 'https://isp.illinois.gov/Sor'
    final def path = '/home/mahadi/Downloads/Sex_Offender_Registry.csv'
    static def tempDir = System.getProperty("java.io.tmpdir") + "/Illions"

    def nameList = []

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
                "--blink-settings=imagesEnabled=false", // Don't load images
            )
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", Us_Il_Sex_Offenders.tempDir)
            options.setExperimentalOptions("prefs", prefs)

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
    }

    Us_Il_Sex_Offenders(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        this.context = context
    }

    def initParsing() {
        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(this.url, 5000)
        getData(engine)
        engine.shutdown()
        setDefaultAddress()
    }

    def getData(ChromeDriverEngine engine) {

        WebElement btn2 = engine.findByXpath("/html/body/div[1]/main/div/div[1]/div/div/div/form/button[1]")
        btn2.click()
        Thread.sleep(10000)

        WebElement btn3 = engine.findByXpath("//*[@id=\"downloadall\"]")
        btn3.click()
        Thread.sleep(120000)

        new File(tempDir).listFiles().each { f ->

            def line
            if (f.toString().endsWith("Registry.csv")) {
                f.withReader { reader ->
                    while ((line = reader.readLine()) != null) {
                        handleDetailsData(line)
                    }
                }
            }
        }
        new File(tempDir).deleteDir()
    }

    def i = 1

    def handleDetailsData(def line) {
        def entityName, address, height, weight, race, gender, dob
        def dataList = []
        def eventList = []
        dataList = line.toString().split(/,/).collect { it }
        int size = dataList.size()

        if (i >= 2) {
            entityName = dataList.get(1) + " " + dataList.get(2) + " " + dataList.get(0)
            address = ", " + dataList.get(3) + ", " + dataList.get(4) + ", " + dataList.get(5) + " " + dataList.get(6)
            address = sanitizeAddress(address)
            height = dataList.get(8)
            weight = dataList.get(9)
            race = dataList.get(10)
            gender = dataList.get(11)
            dob = dataList.get(12)

            for (int i = size - 1; i >= 18; i--) {
                eventList.add(dataList.get(i))
            }
            createEntity(entityName, address, height, weight, race, gender, dob, eventList)
            i++
        } else {
            i++
            return
        }
    }


    def createEntity(def name, def address, def height, def weight, def race, def sex, def dateOfBirth, def eventList) {
        def entity = null

        name = sanitizeEntityName(name)
        if (!name.toString().equals("") || !name.toString().isEmpty()) {
            entity = context.findEntity(["name": name])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType("P")
                nameList.add(name)
            }
            ScrapeAddress scrapeAddress = new ScrapeAddress()

            if ((!address.toString().equals(""))) {
                address = address.toString().replaceAll(/^(.*)$/, '$1, USA')
                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                entity.addAddress(scrapeAddress)
            }

            eventList.each { eventDes ->
                eventDes = eventDes.toString().replaceAll(/\s+/, '')
                if (!eventDes.isEmpty()) {
                    ScrapeEvent event = new ScrapeEvent()
                    event.setDescription(eventDes)
                    entity.addEvent(event)
                }
            }

            entity.addSex(sex)
            entity.addHeight(height)
            entity.addWeight(weight)
            entity.addRace(race)

            dateOfBirth = dateOfBirth.toString().trim().replaceAll(/^Date of Birth$/, '')
            if (!dateOfBirth.toString().isEmpty() || !dateOfBirth.toString().equals("null") || !dateOfBirth.toString().equals("")) {
                dateOfBirth = context.parseDate(new StringSource(dateOfBirth))
                entity.addDateOfBirth(dateOfBirth)
            }
        }

    }

    def setDefaultAddress() {
        def entity = null
        nameList.each { it ->
            entity = context.findEntity("name": it)

            if (entity.getAddresses().isEmpty()) {
                ScrapeAddress scrapeAddress = new ScrapeAddress()
                scrapeAddress.setCountry("UNITED STATES")
                entity.addAddress(scrapeAddress)
            }
        }
    }

    def sanitizeEntityName(def entityName) {
        entityName = entityName.toString().trim()
        entityName = entityName.replaceAll(/\sAND\s/, " and ").trim()
        entityName = entityName.replaceAll(/\sSLASH\s/, "/").trim()
        entityName = entityName.replaceAll(/^null$/, "").trim()

        return entityName.toString().trim()
    }

    def sanitizeAddress(def address) {
        address = address.toString().trim()
        address = address.replaceAll(/(?i)(?:out of state|Department of (?:Human Services|Corrections))/, "")
        address = address.replaceAll(/(?:UNKNOWN|UNK ADDRESS|(?:HMLS|[0]{4,5}) HOMELESS)/, "")
        address = address.replaceAll(/[,\s]+(?![A-Z0-9])/, ",")
        address = address.replaceAll(/,\s*$/, "").trim()
        address = address.replaceAll(/(?<=[A-Z]{2,4})\s+[0]{4,}$/, "").trim()

        return address.toString().trim()
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        return street
    }
}