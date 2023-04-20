package current_scripts

/***
 * This script requires US VPN
 */

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.util.ConcurrentHashSet
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader


context.setup([connectionTimeout: 25000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

USFLSexOffenders script = new USFLSexOffenders(context)

/**
 * debugParsing() should be called when dev wants to
 * read saved data and create entity from local storage.
 * In production this should be commented out.
 *
 * In production, initParsing() should be called.
 * toggle the boolean value `PRODUCTION` to
 * change between functions
 */

boolean PRODUCTION = true

if (PRODUCTION)
    script.initParsing()
else
    script.debugParsing()

class USFLSexOffenders {
    final ScrapianContext context
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def root = "https://offender.fdle.state.fl.us"
    static def url = "https://offender.fdle.state.fl.us/offender/sops/offenderSearch.jsf"
    final addressParser
    final entityType
    // Error list will contain the search keys that were failed while executing
    def errorList = []

    ConcurrentHashSet<String> globalEntityMap = new ConcurrentHashSet<>()
    File cachedKeysFile
    /**
     * CACHING -> true: save each of the entities in local storage
     *            to a corresponding txt file
     *         -> false: do not save data in local storage
     */
    boolean CACHING = true

    // ========================================= CHROME DRIVER INITIALIZATION =========================================
    class ChromeDriverEngine {
        String CHROME_DRIVER_PATH = "/usr/bin/chromedriver"
        ChromeOptions options
        WebDriver driver

        ChromeDriverEngine() {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH)

            options = new ChromeOptions()
            options.addArguments(
                "--headless",
                "--disable-gpu",
                "--ignore-certificate-errors",
                "--window-size=1366,768",
                "--silent",
                "--blink-settings=imagesEnabled=false"
            )

            driver = new ChromeDriver(options)
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
        }

        ChromeDriverEngine(ArrayList<String> args) {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH)

            options = new ChromeOptions()
            options.addArguments(args)
            driver = new ChromeDriver(options)
        }

        void get(String URL, int sleepTime) {
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
            try {
                By by = new By.ByXPath(xpath)
                return driver.findElement(by)
            } catch (Exception e) {
                return null
            }
        }

        List<WebElement> findAllByXpath(String xpath) {
            try {
                By by = new By.ByXPath(xpath)
                return driver.findElements(by)
            } catch (Exception e) {
                return null
            }
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

        String getSource() {
            return driver.getPageSource()
        }

        void waitForElementToBeClickable(String xpath) {
            WebDriverWait wait = new WebDriverWait(driver, 60)
            wait.until(ExpectedConditions.elementToBeClickable(new By.ByXPath(xpath)))
        }

        void waitForElementToPresent(String xpath) {
            WebDriverWait wait = new WebDriverWait(driver, 11)
            wait.until(ExpectedConditions.presenceOfElementLocated(new By.ByXPath(xpath)))
        }
    }
// ========================================= CHROME DRIVER INITIALIZATION =========================================

    class Exec implements Runnable {
        String searchKey
        int page

        Exec(String searchKey, int page) {
            this.searchKey = searchKey
            this.page = page
        }

        @Override
        void run() {
            try {
                startQuery(searchKey, page)
            } catch (Exception e) {
                println("Error")
                e.printStackTrace()
            }
        }

        def printData(def args, def outputFile) {
            println("============================= " + entityCtr++ + " =============================")
            args.each { key, value ->
                println(key + ": " + value)
                outputFile.append("$key: $value |")
            }
            outputFile.append("\n\n")
            println("============================= " + "*****" + " =============================\n\n")
        }


        // All data capturing should be done in this method
        def parseData(def engine) {
            def dataMap = [:]

            def keys = [
                "Name",
                "Status",
                "Date of Birth",
                "Race",
                "Sex",
                "Hair",
                "Eyes",
                "Height",
                "Weight",
            ]

            def pageHTML = engine.getSource()

            def dataBlockMatcher = pageHTML =~ /(?ism)Primary Information.*?Link to FDLE/

            if (dataBlockMatcher.find()) {
                def block = dataBlockMatcher.group(0)
                keys.each {
                    key ->
                        def matcher = block =~ /(?sm)$key:(.*?>){4}(.*?)</
                        if (matcher.find()) {
                            dataMap[key] = trim(matcher.group(2))
                        }
                }

                def aliasMatcher = block =~ /(?sm)Aliases(.*?>){3}(.*?)</
                if (aliasMatcher.find())
                    dataMap["Aliases"] = trim(aliasMatcher.group(2))

                def addressMatcher = block =~ /(?sm)Address<\/span><span.*?"boldBlack">(.*?)<\/span>\s*<\/td>/
                def addresses = []

                while (addressMatcher.find()) {
                    def address = addressMatcher.group(1)
                    address = address.replaceAll(/<.*?>/, ",")
                    address = address.replaceAll(/\s+/, " ").trim()
                    address = address.replaceAll(/,\s*,/, ",")
                    address = address.replaceAll(/,+/, ", ")
                    address = address.replaceAll(/\s+/, " ").trim()
                    addresses.add(address)
                }

                if (!addresses.isEmpty())
                    dataMap["Addresses"] = addresses

                def offenseBlockMatcher = block =~ /(?sm)Crime Information.*?tbody(.*?)<\/span>Victim Information/

                if (offenseBlockMatcher.find()) {
                    def offenseBlock = offenseBlockMatcher.group(1)

                    def rowMatcher = offenseBlock =~ /(?sm)<tr.*?>(.*?)<\/tr>/
                    def offenses = []
                    while (rowMatcher.find()) {
                        def offense = [:]

                        def row = rowMatcher.group(1)
                        def dateMatcher = row =~ /(?sm)<td.*?>(.*?)(\d{1,2}\/\d{1,2}\/\d{4})(.*?)<\/td>/
                        if (dateMatcher.find()) {
                            offense["Date"] = dateMatcher.group(2).trim()
                        }

                        def offenseMatcher = row =~ /(?sm)<td.*?>(.*?">Crime Description<\/span><a.*?title=".*?>)(.*?)<\/a>/
                        def offenseMatcher2 = row =~ /(?sm)<td.*?>(.*?">Crime Description<\/span>)(.*?)<\/.*?>/

                        if (offenseMatcher.find()) {
                            offense["Info"] = trim(offenseMatcher.group(2))
                        } else if (offenseMatcher2.find()) {
                            offense["Info"] = trim(offenseMatcher2.group(2))
                        }

                        def caseNoMatcher = row =~ /(?ism)Court\s*Case\s*Number<\/span><span\s*class="boldBlack">(.*?)(?:<)/
                        if (caseNoMatcher.find()) {
                            offense["Case No"] = trim(caseNoMatcher.group(1))
                        }

                        offenses.add(offense)
                    }

                    dataMap["Offenses"] = offenses
                }

                def imageURLMatcher = pageHTML =~ /(?ism)<img id="sopsFlyer.*?image".*?src="(.*?CallImage.*?)(?:")/
                if (imageURLMatcher.find()) {
                    dataMap["Image URL"] = trim(imageURLMatcher.group(1))
                }

                return dataMap
            }
        }

        void startQuery(String searchKey, int page) {
            ChromeDriverEngine engine = new ChromeDriverEngine()

            try {
                // FILE TO SAVE OUTPUT only when CACHING is true
                File out = null

                if (CACHING)
                    out = new File("us_fl_tmp/out-us-fl-$searchKey-.txt")

                // Getting the landing page
                engine.get(url, 5000)
                engine.waitForElementToBeClickable("//input[@id='offenderSearchForm:firstName']")

                // Finding input field
                engine.findByXpath("//input[@id='offenderSearchForm:firstName']").sendKeys(String.valueOf(searchKey))

                // Pushing search button
                engine.findById("offenderSearchForm:offenderSearchBtn").click()
                engine.wait(10000)

                // Get number of pages
                def indexBlock = engine.findByXpath("//span[@class='ui-paginator-current']").getText()
                def pageMatcher = indexBlock =~ /(?ism)Page:\s*\d+\/(\d+)/

                def numOfPages = null
                if (pageMatcher.find())
                    numOfPages = Integer.valueOf(pageMatcher.group(1).trim())

                def numOfEntities = 0
                String flyerElementPath = "//span[text()='View Flyer']/.."

                while (page <= numOfPages) {
                    while (numOfEntities == 0) {
                        // Getting the list of 'View Flyer' element
                        println("Key: $searchKey - Waiting while all flyers load...")
                        List<WebElement> elements = engine.findAllByXpath(flyerElementPath)
                        engine.wait(5000)
                        numOfEntities = elements.size()
                    }

                    // Iterating through every contacts
                    for (int i = 1; i <= numOfEntities; i++) {
                        println("Key $searchKey: Entity $i of $numOfEntities | Page: $page of $numOfPages")
                        def element = engine.findByXpath(
                            "($flyerElementPath)[$i]"
                        )

                        def entityKey = engine.findByXpath(
                            "(//a[text()='Track Offender'])[$i]"
                        )

                        engine.wait(200)

                        // Avoid duplicate invoking
                        def entityTracker = entityKey.getAttribute("href")
                        println("Entity Key: $entityTracker")

                        if (globalEntityMap.contains(entityTracker)) {
                            println("Entity already exists")
                            continue
                        } else {
                            if (CACHING)
                                cachedKeysFile.append(entityTracker + "\n\n")
                            globalEntityMap.add(entityTracker)
                        }

                        if (element) {
                            element.click()
                        } else
                            break

                        engine.wait(5000)

                        engine.waitForElementToBeClickable("//a[@id='dialogFlyerClose']//i")

                        def dataMap = parseData(engine)
                        dataMap["Entity URL"] = entityTracker

                        if (CACHING)
                            printData(dataMap, out)

                        createEntity(dataMap)

                        def closeButton = engine.findByXpath("//a[@id='dialogFlyerClose']//i")

                        engine.wait(200)

                        if (closeButton != null) {
                            closeButton.click()
                            engine.wait(1000)
                        } else
                            break
                    }

                    numOfEntities = 0
                    page++

                    engine.waitForElementToBeClickable("//a[@aria-label='Next Page']")
                    def nextPageElement = engine.findByXpath("//a[@aria-label='Next Page']")

                    if (nextPageElement != null) {
                        JavascriptExecutor jse = (JavascriptExecutor) engine.driver
                        jse.executeScript("arguments[0].click();", nextPageElement)
                        Thread.sleep(9999)
                    } else {
                        println("Finished searching for: $searchKey")
                        break
                    }

                }

                engine.shutdown()

            } catch (Exception e) {
                println("Error occurred while searching for: $searchKey, page: $page. Shutting down the browser instance")
                errorList.add(
                    [searchKey, page]
                )
                engine.shutdown()
                e.printStackTrace()
            }
        }
    }

    USFLSexOffenders(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.reloadData()
        if (CACHING)
            cachedKeysFile = new File("cache_us_fl.txt")
    }

    // load data from cache
    def loadCacheData() {
        try {
            def currentList = cachedKeysFile.text.split("\n\n")
            currentList.each {
                if (it.trim().size() > 1)
                    globalEntityMap.add(it)
            }
            println("Total cached data found: " + globalEntityMap.size())
        } catch (Exception e) {

        }
    }

    def initParsing() {
        if (CACHING)    loadCacheData()

        // Initiating thread pool to access multiple pages concurrently
        ExecutorService executorService = Executors.newFixedThreadPool(6)

        for (char searchKey = 'a'; searchKey <= 'z'; searchKey++) {
            Runnable exec = new Exec(String.valueOf(searchKey), 1) // Page No. -> 1
            executorService.execute(exec)
            Thread.sleep(30000)

        }

        println("Checking for interrupted queries")
        while (!errorList.isEmpty()) {
            def query = errorList.pop()
            println("Starting interrupted query: $query")
            Runnable exec = new Exec(query[0], query[1])
            executorService.execute(exec)
            Thread.sleep(30000)
        }

        // Making sure everything was captured by rerunning queries
        for (char searchKey = 'a'; searchKey <= 'z'; searchKey++) {
            Runnable exec = new Exec(String.valueOf(searchKey), 1) // Page No. -> 1
            executorService.execute(exec)
            Thread.sleep(30000)

        }

        // Shutting down the service
        executorService.shutdown()

        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (InterruptedException e) {
            e.printStackTrace()
        }
    }

    // TODO: DELETE THE FUNCTION
    def entityCtr = 0

    /**
     * This function is used to read entity data from txt files
     * IT SHOULD ONLY BE USED IN DEVELOPMENT
     */
    def debugParsing() {
        for (char searchKey = 'a'; searchKey <= 'z'; searchKey++) {
            File data = new File("us_fl_tmp/out-us-fl-$searchKey-.txt")
            def entityList = data.text.split("\n\n")
            def keys = [
                "Name",
                "Status",
                "Date of Birth",
                "Race",
                "Sex",
                "Hair",
                "Eyes",
                "Height",
                "Weight",
                "Aliases",
                "Addresses",
                "Offenses",
                "Image URL",
                "Entity URL"
            ]

            for (int i = 0; i < entityList.size(); i++) {
                def dataMap = [:]
                keys.each {
                    key ->
                        def matcher = entityList[i] =~ /(?sm)($key:)(.*?)(?:\|)/
                        if (matcher.find()) {
                            dataMap[key] = matcher.group(2).trim()
                        }
                }

                // Format aliases
                def aliases = dataMap["Aliases"]
                if (aliases != null) {
                    def alias = []
                    def splitted = aliases.split(",")
                    splitted.each {
                        alias.add(it.trim())
                    }
                    dataMap["Aliases"] = alias
                }


                // Format address
                def addr = dataMap["Addresses"]
                def splited = []
                if (addr != null) {
                    addr = addr.trim()
                    addr = erase(addr, /^\[/)
                    addr = erase(addr, /\]$/)
                    addr = addr.replaceAll(/County,/, "County|")
                    def split = addr.split("\\|")
                    split.each {
                        if (it.trim().size() > 1)
                            splited.add(it)
                    }

                    dataMap["Addresses"] = splited
                }

                // Format offenses
                def offense = dataMap["Offenses"]
                if (offense != null) {
                    offense = erase(offense, /^\[/)
                    offense = erase(offense, /\]$/)

                    def matcher = offense =~ /(?sm)\[.*?\]/
                    def offenseList = []
                    while (matcher.find()) {
                        def offenseData = [:]
                        def ofData = matcher.group(0)
                        def dateMatcher = ofData =~ /Date:(.*?),/
                        if (dateMatcher.find()) {
                            offenseData["Date"] = dateMatcher[0][1]
                        }

                        def infoM = ofData =~ /Info:(.*?)(, Case No:|$)/
                        if (infoM.find()) {
                            offenseData["Info"] = infoM[0][1]
                        }
                        def caseNoM = ofData =~ /Case No:(.*?)\]/
                        if (caseNoM.find()) {
                            offenseData["Case No"] = caseNoM[0][1]
                        }

                        offenseList.add(offenseData)
                    }

                    dataMap["Offenses"] = offenseList
                }

                createEntity(dataMap)
            }
        }
    }

    def santizeName(def name) {
        if (name == null)
            return null

        name = name.replaceAll(/(?i),?\s?(\bJr\b\.?|\bSr\b\.?)\s?/, "")
        name = name.replaceAll(/^`/, "").trim()
        name = name.replaceAll(/`/, "'").trim()
        name = name.replaceAll(/&amp;/, "&").trim()
        name = name.replaceAll(/-/, " ")
        name = name.replaceAll(/\?/, "")
        name = name.replaceAll(/&apos;/, "'")
        name = erase(name, /Not Available/)
        name = name.replaceAll(/\+/, "")
        name = name.replaceAll(/\s+/, " ").trim()

        return name
    }

    def createEntity(def dataMap) {
        String name = santizeName(dataMap["Name"])

        def dob = dataMap["Date of Birth"]
        dob = cleanData(dob)

        if (name == null || name.trim().size() <= 0)
            return

        def entityType = "P"
        ScrapeEntity entity = null

        entity = context.findEntity("name": name, "type": entityType, "dob": dob)

        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(entityType)
        }

        def aliases = dataMap["Aliases"]
        if (aliases != null) {
            aliases.each {
                it = santizeName(it.trim())
                if (it.size() > 1)
                    entity.addAlias(it)
            }
        }

        if (dob != null) {
            if (dob.trim().size() > 1)
                entity.addDateOfBirth(dob)
        }

        def height = dataMap["Height"]
        height = cleanData(height)
        if (height != null) {
            if (height.trim().size() > 1)
                entity.addHeight(height)
        }

        def weight = dataMap["Weight"]
        weight = cleanData(weight)
        if (weight != null) {
            if (weight.trim().size() > 1)
                entity.addWeight(weight)
        }

        def race = dataMap["Race"]
        race = cleanData(race)
        if (race != null) {
            entity.addRace(race)
        }

        def gender = dataMap["Sex"]
        gender = cleanData(gender)
        if (gender != null) {
            entity.addSex(gender)
        }

        def hair = dataMap["Hair"]
        hair = cleanData(hair)
        if (hair != null) {
            if (hair.trim().size() > 1)
                entity.addHairColor(hair)
        }

        def eyeColor = dataMap["Eyes"]
        eyeColor = cleanData(eyeColor)
        if (eyeColor != null) {
            if (eyeColor.trim().size() > 1)
                entity.addEyeColor(eyeColor)
        }

        def addresses = dataMap["Addresses"]
        if (addresses != null) {
            addresses.each {
                address ->
                    def scrapeAddress = formatAddress(address)
                    if (scrapeAddress != null) {
                        entity.addAddress(scrapeAddress)
                    }
            }
        } else {
            def scrapeAddress = formatAddress(null)
            entity.addAddress(scrapeAddress)
        }

        // Events
        def offenses = dataMap["Offenses"]
        if (offenses != null) {
            for (int i = 0; i < offenses.size(); i++){
                def offense = offenses[i]

                def eventDes = offense["Info"]

                if (eventDes != null) {
                    ScrapeEvent scrapeEvent = new ScrapeEvent()

                    eventDes = cleanEvent(eventDes)
                    scrapeEvent.setDescription(eventDes)

                    def date = offense["Date"]
                    if (date != null) {
                        def sdate = context.parseDate(new StringSource(date), ["MM/dd/yyyy"] as String[])
                        scrapeEvent.setDate(sdate)
                    }

                    entity.addEvent(scrapeEvent)

                    // Remarks
                    def caseNo = offense["Case No"]
                    caseNo = cleanData(caseNo)
                    if (caseNo != null) {
                        caseNo = trim(caseNo)

                        entity.addRemark("Case No: $caseNo")
                    }
                } else {
                    ScrapeEvent scrapeEvent = new ScrapeEvent()
                    scrapeEvent.setDescription("This entity appears on the Florida State Sex Offender list.")

                    entity.addEvent(scrapeEvent)
                }
            }
        } else {
            ScrapeEvent scrapeEvent = new ScrapeEvent()
            scrapeEvent.setDescription("This entity appears on the Florida State Sex Offender list.")

            entity.addEvent(scrapeEvent)
        }

        // Image URL
        def imageURL = dataMap["Image URL"]
        if (imageURL != null) {
            imageURL = erase(imageURL, /&amp;pfdrid.*/)
            imageURL = trim(imageURL)
            entity.addImageUrl(root + imageURL)
        }
    }

    def cleanEvent(def description) {
        description = description.replaceAll(/;/, ",")
        description = trim(description)
        description = description.replaceAll(/\($/, "")
        description = description.replaceAll(/&amp;/, "&")

        return description
    }

    def cleanData(def data) {
        if (data == null)
            return null

        data = data.replaceAll(/(?i)unknown\/?/, "")
        data = data.replaceAll(/(?i)Not Available/, "")
        data = data.replaceAll(/`/, "")
        data = data.replaceAll(/\*/, "")
        data = data.replaceAll(/&amp;/, "")
        data = trim(data)

        if (data.size() < 2) {
            return null
        }

        return data
    }

    def formatAddress(def address) {
        ScrapeAddress scrapeAddress = new ScrapeAddress();

        if (address == null) {
            scrapeAddress.setProvince("FLORIDA")
            scrapeAddress.setCountry("UNITED STATES")
            return scrapeAddress
        }

        address = address.replaceAll(/(?i)Unknown County/, "")
        address = address.replaceAll(/(?i)Transient\s*,/, "")
        address = address.replaceAll(/(?i)HOMELESS\/TRANSIENT\//, "")
        address = address.replaceAll(/(?i)TRANSIENT\/US/, "")
        address = address.replaceAll(/(?i)Transient[:\-]?/, "&")
        address = address.replaceAll(/&amp;/, "&")
        address = address.replaceAll(/(?i).*INCARCERATED.*/, "")
        address = address.replaceAll(/(?i).*Out of State.*/, "")
        address = address.replaceAll(/(?i).*Unknown.*/, "")
        address = address.replaceAll(/(?i).*deported to.*/, "")
        address = address.replaceAll(/YY \d{5}/, "")
        address = address.replaceAll(/`/, "")
        address = address.replaceAll(/\?/, "")
        address = trim(address)
        address = address.replaceAll(/#$/, "")
        address = address.replaceAll(/@$/, "")
        address = address.replaceAll(/-$/, "")
        address = trim(address)

        def split = address.split(",")

        if (split.size() >= 4) {
            if (split.size() > 4) {
                def streetAddressSize = split.size() - 3
                address = ""

                for (int i = 0; i < streetAddressSize; i++) {
                    address += split[i]

                    if (i != streetAddressSize - 1)
                        address += "##"
                }

                for (int i = streetAddressSize; i < split.size(); i++) {
                    address += ", " + split[i]
                }

                split = address.split(",")
                def size = split.size()

            }

            def postalAddress = split[2]
            def zipMatcher = postalAddress =~ /[\d\-]{4,}/
            if (zipMatcher.find()) {
                def zip = zipMatcher[0]
                def province = erase(postalAddress, /$zip/)

                zip = erase(zip, /,/)
                zip = erase(zip, /0{5}/)
                zip = trim(zip)

                province = erase(province, /,/)
                province = trim(province)

                def parseAddress = addressParser.parseAddress([text: "Address1, $province, United States", force_country: true])

                scrapeAddress.setProvince(parseAddress[parseAddress.keySet()[3]])

                if (zip.size() > 1)
                    scrapeAddress.setPostalCode(zip)
            }

            scrapeAddress.setCity(trim(split[1]))
            scrapeAddress.setCountry("UNITED STATES")

            def streetAddress = split[0]
            streetAddress = streetAddress.replaceAll(/##/, ", ")
            streetAddress = trim(streetAddress)
            streetAddress = streetAddress.replaceAll(/(?i)^\w+\sCounty/, "")
            streetAddress = streetAddress.replaceAll(/^,/, "")
            streetAddress = trim(streetAddress)
            streetAddress = streetAddress.replaceAll(/#$/, "")
            streetAddress = streetAddress.replaceAll(/@$/, "")
            streetAddress = streetAddress.replaceAll(/-$/, "")
            streetAddress = trim(streetAddress)

            if (streetAddress.size() > 1)
                scrapeAddress.setAddress1(streetAddress)

        } else if (split.size() == 3) {
            def formatMatcher = address =~ /(?i)(.*?)(, )([A-Z][A-Z])(\s\d{5,})(,)([\w \-]+)/

            if (formatMatcher.find()) {
                def city = trim(formatMatcher.group(1))
                def province = trim(formatMatcher.group(3))

                def zip = trim(formatMatcher.group(4))
                zip = erase(zip, /0{5}/)

                def street = trim(formatMatcher.group(6))
                street = street.replaceAll(/(?i)^[\w\-]+\sCounty/, "")
                street = trim(street)

                def parseAddress = addressParser.parseAddress([text: "Address1, $province, United States", force_country: true])

                scrapeAddress.setProvince(parseAddress[parseAddress.keySet()[3]])
                scrapeAddress.setCity(city)
                if (zip.size() > 1)
                    scrapeAddress.setPostalCode(zip)

                if (street.size() > 1)
                    scrapeAddress.setAddress1(street)

                scrapeAddress.setCountry("UNITED STATES")
            } else {
                scrapeAddress.setProvince("FLORIDA")
                scrapeAddress.setCountry("UNITED STATES")
            }

        } else {
            scrapeAddress.setProvince("FLORIDA")
            scrapeAddress.setCountry("UNITED STATES")
        }

        return scrapeAddress

    }

    String erase(String source, def pattern) {
        return source.replaceAll(pattern, "")
    }

    String trim(String text) {
        return text.replaceAll(/\s+/, " ").trim()
    }
}