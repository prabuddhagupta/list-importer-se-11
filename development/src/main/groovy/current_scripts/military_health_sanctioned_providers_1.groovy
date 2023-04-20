package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.io.FileUtils
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.Keys
import org.openqa.selenium.JavascriptExecutor

context.setup([connectionTimeout: 25000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

MILITARY_HEALTH script = new MILITARY_HEALTH(context)
script.initParsing()

class MILITARY_HEALTH {
    final ScrapianContext context
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def root = "https://www.health.mil"
    static def url = "https://www.health.mil/Military-Health-Topics/Access-Cost-Quality-and-Safety/Quality-And-Safety-of-Healthcare/Program-Integrity/Sanctioned-Providers"
    final addressParser
    final entityType
    final CAPTCHA_SOLVE_TIMEOUT = 70000

    // ========================================= CHROME DRIVER CONTROLLER DEFINITION =========================================
    class ChromeDriverEngine {
        String CHROME_DRIVER_PATH = "/usr/bin/chromedriver"
        ChromeOptions options
        WebDriver driver

        ChromeDriverEngine() {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH)

            options = new ChromeOptions()

            options.addArguments(
                "--disable-gpu",
                "--ignore-certificate-errors",
                "--window-size=1366,768", // Default window-size
                "--silent",
                "--blink-settings=imagesEnabled=false" // Don't load images
            )

            driver = new ChromeDriver(options)
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
        }

        void get(String URL, int sleepTime = 0) {
            System.out.println("[SELENIUM] Invoking: [" + URL + "]")
            driver.get(URL)

            if (sleepTime != 0) {
                wait(sleepTime)
            }
        }

        void takeScreenshot() throws IOException {
            System.out.println("Taking Screenshot ...")
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)
            String fileName = "screenshot_" + LocalDateTime.now().toString() + ".png"
            FileUtils.copyFile(screenshot, new File(fileName))
        }

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

        List < WebElement > findAllByXpath(String xpath) {
            By by = new By.ByXPath(xpath)
            return driver.findElements(by)
        }

        WebElement findById(String id) {
            By by = new By.ById(id)
            return driver.findElement(by)
        }

        void wait(int time) {
            try {
                Thread.sleep(time)
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
        }

        void shutdown() {
            driver.close()
            driver.quit()
        }

        void goBack() {
            driver.navigate().back()
        }

        String getSource() {
            return driver.getPageSource()
        }

        void waitForElementToBeClickable(String xpath) {
            WebDriverWait wait = new WebDriverWait(driver, 10)
            wait.until(ExpectedConditions.elementToBeClickable(new By.ByXPath(xpath)))
        }
    }
    // ========================================= CHROME DRIVER CONTROLLER DEFINITION =========================================

    MILITARY_HEALTH(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.reloadData()
    }

    def initParsing() {
        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(this.url, 2000)

        getCases(engine)

        engine.shutdown()
    }

    def counter = 0

    def getCases(ChromeDriverEngine engine) {

        counter++

        WebElement viewAllButton = engine.findByXpath("//*[@id=\"btnViewAll\"]")
        viewAllButton.click()

        engine.wait(2000)

        def nextPage

        def page = 1
        while (page < 10) {

            if (page > 5) {
                nextPage = engine.findByXpath("//*[@id=\"pagecolumns_0_content_1_dpResults\"]/a[7]")
            } else {
                nextPage = engine.findByXpath("//*[@id=\"pagecolumns_0_content_1_dpResults\"]/a[8]")
            }

            if (nextPage.isEnabled()) {
                def noThanksButton = engine.findByXpath("//*[@id=\"acsMainInvite\"]/div/a[1]")

                if (noThanksButton != null) {
                    noThanksButton.click()
                    engine.wait(1000)
                }

                def html = engine.getSource()

                def block = html =~ /(?ism)<section id="pagecolumns_0_content_1_lvResults_section_(.*?)<\/section>/

                while (block.find()) {

                    def companyNameBlock = block =~ /(?ism)<dt>Companies:<\/dt>\s+<dd>(.*?)<\/dd>/
                    def peopleNameBlock = block =~ /(?ism)<dt>People:<\/dt>\s+<dd>(.*?)<\/dd>/

                    def name

                    if (companyNameBlock.find()) {
                        name = companyNameBlock.group(1)

                    } else if (peopleNameBlock.find()) {
                        name = peopleNameBlock.group(1)
                    }

                    def associatedNameList = []
                    def associatedValue = block =~ /(?ism)<dt>Companies:<\/dt>\s+<dd>(.*?)<\/dd>\s+<dt>People:<\/dt>\s+<dd>(.*?)<\/dd>/

                    if (associatedValue.find()) {
                        associatedNameList.add(associatedValue.group(2))
                    }

                    def addressValue = block =~ /(?ism)<dt>Addresses:<\/dt>\s+<dd>(.*?)<\/dd>/

                    def addressList = []

                    if (addressValue.find()) {
                        addressList.add(addressValue.group(1))
                    }

                    def summaryValue, summary

                    summaryValue = block =~ /(?ism)<dt>Summary:<\/dt>\s+<dd>(.*?)<\/dd>/

                    if (summaryValue.find()) {
                        summary = summaryValue.group(1)
                    }

                    def termValue, term

                    termValue = block =~ /(?ism)<dt>Term:<\/dt>\s+<dd>(.*?)<\/dd>/

                    if (termValue.find()) {
                        term = termValue.group(1)
                    }

                    def dateValue, date

                    dateValue = block =~ /(?ism)<h3>(.*?)<a/

                    if (dateValue.find()) {
                        date = dateValue.group(1)
                        date = date.toString().replaceAll(/-/, '').trim()

                        if (date) {
                            String[] date_string = date.split('/')

                            if (date_string.size() >= 3) {
                                if (date_string[0].size() == 1) {
                                    date_string[0] = '0' + date_string[0]
                                }

                                if (date_string[1].size() == 1) {
                                    date_string[1] = '0' + date_string[1]
                                }
                                date = date_string[0] + '/' + date_string[1] + '/' + date_string[2]
                            }
                        }
                    }

                    name = name.toString().replaceAll(/(?ism)&amp;/, '&').trim()
                    name = name.toString().replaceAll(/(?ism)null/, '').trim()

                    name = removable_title(name)

                    summary = summary.toString().replaceAll(/None provided\./, '').trim()
                    summary = summary.toString().replaceAll("None Provided.", '').trim()

                    def description = "Summary: " + summary + ". " + "Term: " + term

                    createEntity(name, associatedNameList, addressList, description, date)
                }
                nextPage.click()
                engine.wait(3000)
            } else break
            page++
        }

        engine.wait(5000)
    }

    def createEntity(def name, def associationList, def addressList, def summary, def date) {

        def entity = null
        def description = "This entity appears on the Military Health System list of Sanctioned Providers. "

        description += summary

        description = description.toString().replaceAll(/(?s)\s+/, " ")

        if (!name.toString().isEmpty()) {
            def entityType = detectEntity(name)

            name = name_santitize(name)
            entity = context.findEntity("name": name, "type": entityType)
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }

            associationList.each {
                if (it) {
                    it = removable_title(it)
                    it = name_santitize(it)
                    it = it.toString().replaceAll("James Jaramillo", "James DC Jaramillo")
                    it = it.toString().replaceAll(/(?s)\s+/, " ").trim()
                    entity.addAssociation(it)
                }
            }

            if (name.toString().contains("Fernando Garcia-Dorta")) {
                addressList = []
                addressList.add("40 NE 2nd Avenue, Deerfield Beach, Florida 33441-3504")
                addressList.add("4800 Linton Blvd., Bldg B, Del Ray Beach, Florida 33445-6584")
            }

            if (name.toString().contains("One For Autism")) {
                addressList = []
                addressList.add("1216 West Avenue, Suite 1-6, San Antonio, Texas 78201-4044")
                addressList.add("12003 Huebner Road, San Antonio, Texas 78230")
            }

            if (name.toString().contains("Health Visions Corporation") && addressList.toString().contains("13 Don Benito St., Dona Justa Village, Angono, Riza 1930, Philippines")) {
                addressList = []
                addressList.add("13 Don Benito St., Dona Justa Village, Angono, Riza 1930, Philippines")
                addressList.add("Bldg 3022, Zambales Highway, Upper Cubi, Subic Bay Freeport Zone, Olongapo City, Philippines")
            }

            if (name.toString().contains("Krantinath Raikhelkar")) {
                addressList = []
                addressList.add("2929 N. Central, #2100, Phoenix, Arizona 85012")
                addressList.add("1204127 Trimbak, #5, Shivajinasar Pune 4, India")
            }

            if (name.toString().contains("Adul Rermgosakul")) {
                addressList = []
                addressList.add("11464 Log Jump Trail, Ellicott City, Maryland 21043, UNITED STATES")
            }

            if (name.toString().contains("Great Falls Eye Surgery Center")) {
                addressList = []
                addressList.add("1717 Fourth Street South, Great Falls, Montana 59405, UNITED STATES")
            }

            if (name.toString().contains("William R. Hancock")) {
                addressList = []
                addressList.add("31 Pine Knolls Drive, Sedona, Arizona 86336")
                addressList.add("1152 Noth Craycroft, Tucson, Arizona 85712")
                addressList.add("7355 North Oracle, #205, Tucson, Arizona 85704")
                addressList.add("549 Plaza Circle, Road A, Litchfield Park, Arizona 85340")
                addressList.add("9600 E. Catalina Highway, Tucson, Arizona 85749")
            }

            if (name.toString().contains("Senador V. Fandino")) {
                addressList = []
                addressList.add("2939 Alta View Drive, Suite J, San Diego, California 92139")
                addressList.add("9360 Activity Road, Suite A, San Diego, California 92126")
            }

            if (name.toString().contains("Harvey Leonard Nissman")) {
                addressList = []
                addressList.add("1004 First Colonial, #103, Virginia Beach, Virginia 23454")
                addressList.add("2877 Guardian Lane, #715, Virginia Beach, Virginia 23452")
                addressList.add("1701 Will-O-Wisp Drive, Virginia Beach, Virginia 23454")
            }

            if (name.toString().contains("Nicholas Bachynsky")) {
                addressList = []
                addressList.add("20801 Biscayne Blvd., Miami, Florida 33180")
                addressList.add("6535 SW Freeway, Houston, Texas 77074")
                addressList.add("2699 Lee Road, Winter Park, Florida 32789")
                addressList.add("1110 Pine Circle, Seabrook, Texas 77586")
            }

            for (int i = 0; i < addressList.size(); i++) {

                def addrMap = addressParser.parseAddress([text: addressList[i], force_country: true])
                ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress)
                }
            }

            ScrapeEvent event = new ScrapeEvent()

            event.setDate(date)

            description = description.toString().replaceAll(';', '.')

            event.setDescription(description)
            entity.addEvent(event)
        }
    }

    def removable_title(def name) {
        name = name.toString().replaceAll(/\sDC|\sDPM|\sPhD|\sMD|\sDPM|\sPA|\sABA|\sSLP|\sDO|\sASW|\sLCSW|\sDMD|\sMHC|\sLPC|\sMSW|\sMFCC|\sRJ|\sJr\.|III|II/, '').trim()
        return name
    }

    def name_santitize(def name) {
        name = name.toString().replaceAll(',', '').trim()

        return name
    }

    def street_sanitizer = {
        street ->
            fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().replaceAll(/(?s)\s+/, " ").trim()
        return street
    }

    def detectEntity(def fullName) {
        def type
        if (fullName =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(fullName)
            if (type.equals("P")) {
                if (fullName =~ /(?i)(?:Beaverbrook|Assisted|Pharmacy|Ann's Loving Care|Physicians|Training|Associated|Clinic)/) {
                    type = "O"
                }
            } else if (fullName =~ /(?i)(?:Franklin Lombardo|Alexandra|Anna Marie|Bernadette|Hunt|Catherine|Chase|Fortune|O'Shea|Freddie|Godfrey|Royal|Kelly|Allotey|Morris|Priscilla|Ruben|Timothy|Velma|H. Yu)/) {
                type = "P"
            }
        }
        return type
    }
}