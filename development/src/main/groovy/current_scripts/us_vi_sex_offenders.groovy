package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import org.apache.commons.io.FileUtils
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait


import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

context.setup([connectionTimeout: 500000, socketTimeout: 500000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

Us_Vi_Sex_Offenders script = new Us_Vi_Sex_Offenders(context)
script.initParsing()

class Us_Vi_Sex_Offenders {
    final addressParser
    final entityType
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context
    final CAPTCHA_SOLVE_TIMEOUT = 70000
    def texts = []

    //def url = 'https://sor.sd.gov'
    def url = 'https://sex-offender.vsp.virginia.gov/sor/supplementalSearchResults.html'
    def subUrl = '?search.type=com.vsp.sor.pub.web.action.SupplementalSearchResultsAction1635919703811&amp;searchid=x93485&amp;d-6484321-p='

    def createFile() {
        def newFile = new File("Dakota.txt")
        def row = ''
        texts.each { row = "$row\n************************************************************\n$it" }
        newFile.write(row)
    }

    def addTextsToList(text) {
        texts.add(text)
    }
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
                "--blink-settings=imagesEnabled=true", // Don't load images
                '--disable-extensions',
                'disable-infobars',
                'start-maximized'
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
         * This method takes a screenshot of the current window and saves
         * it to the 'output/' directory
         * @throws IOException
         */
        void takeScreenshot() throws IOException {
            System.out.println("Taking Screenshot ...")
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)
            String fileName = "screenshot_" + LocalDateTime.now().toString() + ".png"
            FileUtils.copyFile(screenshot, new File(fileName))
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
         * This method retrieves a web element by using it's tagName
         * in the current window.
         * It returns null if no element is found with the xpath.
         *
         * @param xpath : The xpath of the element
         * @return: null or WebElement
         */
        WebElement findByTagName(String tagName) {
            By by = new By.ByTagName(tagName)
            WebElement webElement

            try {
                webElement = driver.findElement(by)
            } catch (Exception e) {
                println("WebElement doesn't exist")
                webElement = null
            }

            return webElement == null ? null : webElement
        }

        WebElement findByCssSelector(String xpath) {
            By by = new By.ByCssSelector(xpath)
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
         * This method retrieves web elements by their xpath
         * in the current window and returns a list of elements.
         *
         * @param xpath : The xpath of the elements
         * @return: List<WebElement>
         */
        List<WebElement> findAllByXpath(String xpath) {
            By by = new By.ByXPath(xpath)
            return driver.findElements(by)
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
         * This method stops the selenium driver and closes the window(s)
         */
        void extendWindows() {
            Dimension d = new Dimension(1080, 720)
            //Resize current window to the set dimension
            driver.manage().window().setSize(d)
        }

        /**
         * This method navigates to the previous webpage in the current window.
         */
        void goBack() {
            driver.navigate().back()
        }

        /**
         * This page returns the HTML document of the current webpage as String
         * @return: String HTML document
         */
        String getSource() {
            return driver.getPageSource()
        }

        /**
         * This method makes the selenium webdriver to wait for a particular element to be
         * clickable for further proceeding
         * @param xpath : The xpath String of the element
         */
        void waitForElementToBeClickable(String xpath) {
            WebDriverWait wait = new WebDriverWait(driver, 30)
            wait.until(ExpectedConditions.elementToBeClickable(new By.ByXPath(xpath)))
        }
    }

    Us_Vi_Sex_Offenders(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser.updateCities([US: ["SPRINGFIELD"]])

        this.context = context
    }

    def initParsing() {
        // def html = invokeUrl(url)
        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(this.url, 5000)

//        new WebDriverWait(engine.driver, 60).until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("//iframe[starts-with(@name, 'a-')]"))
//        new WebDriverWait(engine.driver, 20).until(ExpectedConditions.elementToBeClickable(By.cssSelector("#recaptcha-anchor > div.recaptcha-checkbox-checkmark"))).click();
//        //WebElement element = engine.findByXpath("//*[@id=\"recaptcha-anchor\"]/div[4]")
//      /*  WebElement element = engine.findByCssSelector("#recaptcha-anchor > div.recaptcha-checkbox-checkmark")
//        element.click()
//        engine.wait(50000)

        boolean wait = true

        if (wait)
            engine.wait(CAPTCHA_SOLVE_TIMEOUT)

        /* WebElement btn = engine.findByXpath("//*[@id=\"button\"]")
         btn.click()
         engine.wait(20000)*/
        WebElement acceptButton = engine.findByXpath("//*[@value=\"Accept\"]")
        acceptButton.click()
        engine.wait(2000)

        def nextPageUrl
        for(int i=2; i<100; i++) {
            if (i <= 50) {
                nextPageUrl = "https://sex-offender.vsp.virginia.gov/sor/supplementalSearchResults.html?search.type=com.vsp.sor.pub.web.action.SupplementalSearchResultsAction1636552255831&searchid=x93485&d-6484321-p=" + i
                engine.get(nextPageUrl, 2000)
                getDataHtml(engine.getSource())
            } else if (i > 50) {
                nextPageUrl = "https://sex-offender.vsp.virginia.gov/sor/supplementalSearchResults.html?search.type=com.vsp.sor.pub.web.action.SupplementalSearchResultsAction1636552255831&searchid=x93485&d-6484321-p=51"
                engine.get(nextPageUrl, 15000)
                getDataHtml(engine.getSource())

                boolean wait1 = true

                if (wait1)
                    engine.wait(CAPTCHA_SOLVE_TIMEOUT)

                /* WebElement btn = engine.findByXpath("//*[@id=\"button\"]")
                 btn.click()
                 engine.wait(20000)*/
                WebElement acceptButton2 = engine.findByXpath("//*[@value=\"Accept\"]")
                acceptButton2.click()
                engine.wait(10000)
                for(i=52; i<100; i++) {
                        nextPageUrl = "https://sex-offender.vsp.virginia.gov/sor/supplementalSearchResults.html?search.type=com.vsp.sor.pub.web.action.SupplementalSearchResultsAction1636552255831&searchid=x93485&d-6484321-p=" + i
                        engine.get(nextPageUrl, 2000)
                        getDataHtml(engine.getSource())
                    }
                }
        }

        /* for (; true;) {

             def nextPage = engine.findByXpath("//*[@id=\"mainArea\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[2]/td[2]/table/tbody/tr/td/table[2]/tbody/tr/td/span[2]/a[7]")

             if (nextPage != null) {
                 nextPage.click()

                 def firstRowData = engine.findByXpath("//*[@id=\"offenderList\"]/tbody/tr[1]")
                 // println(firstRowData.getText())
                 getDataHtml(engine.getSource())
                 Thread.sleep(1000)
             } else break
         }

         Thread.sleep(1000)*/

        /*def lastPageMatcher = engine.getSource() =~ /searchid=x93485&amp;d-6484321-p=(\d+)">Last<\/a>]<\/span>/
        def lastPage
        if (lastPageMatcher.find()) {
            lastPage = lastPageMatcher.group(1)
        }
        //Code for next page
        def last = Integer.parseInt(lastPage)

        def nextPageUrl
        def htmlSource*/
       /* WebElement nextPage
        for (int i = 1; i < last; i++) {
            //WebElement nextPage = engine.findByXpath("//*[@id=\"pager1\"]/a[3]/span")

            //WebElement nextPage = engine.findByXpath("//*[@class=\"k-link k-pager-nav\"]/span")
            if(i==1){
                nextPage = engine.findByXpath("//*[@id=\"mainArea\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[2]/td[2]/table/tbody/tr/td/table[2]/tbody/tr/td/span[2]/a[5]")
                nextPage.click()
                Thread.sleep(1000)
            }else {
                nextPage = engine.findByXpath("//*[@id=\"mainArea\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[2]/td[2]/table/tbody/tr/td/table[2]/tbody/tr/td/span[2]/a[7]")
                nextPage.click()
                Thread.sleep(1000)
            }
          //Thread.sleep(2000)
            getDataHtml(engine.getSource())
            Thread.sleep(1000)
        }*/
        println engine.getSource()
        // getData(engine.getSource())
        //println engine
        engine.shutdown()

        // println html
    }


    def getData(def html) {
        // ChromeDriverEngine engine = new ChromeDriverEngine()
        // WebElement checkBox = engine.findById("recaptcha-anchor")
        // WebDriverWait wait = new WebDriverWait(engine.driver, 120);
        //wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("//iframe[starts-with(@name, 'a-')]"))
        //WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"recaptcha-anchor\"]")))
        /* WebElement element = engine.findByXpath("//*[@id=\"recaptcha-anchor\"]/div[4]")
         element.click()
         engine.wait(50000)
         boolean wait = true

         if (wait)
             engine.wait(CAPTCHA_SOLVE_TIMEOUT)

         WebElement btn = engine.findByXpath("//*[@id=\"button\"]")
         btn.click()
         engine.wait(20000)*/


        /*def lastPageMatcher = html =~ /searchid=x93485&amp;d-6484321-p=(\d+)">Last<\/a>]<\/span>/
        def lastPage
        if (lastPageMatcher.find()) {
            lastPage = lastPageMatcher.group(1)
        }
        //Code for next page
        def last = Integer.parseInt(lastPage)*/

        /*def nextPageUrl
        def htmlSource*/
        // for (int i = 2; i < 5; i++) {
        //WebElement nextPage = engine.findByXpath("//*[@id=\"pager1\"]/a[3]/span")
        // WebElement nextPage = engine.findByXpath("//*[@id=\"mainArea\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[2]/td[2]/table/tbody/tr/td/table[2]/tbody/tr/td/span[4]/a")
        //WebElement nextPage = engine.findByXpath("//*[@class=\"k-link k-pager-nav\"]/span")
        //nextPage.click()
//            Thread.sleep(20000)
//            getData(engine.getSource())
//            Thread.sleep(10000)
        /* nextPageUrl = url+"?search.type=com.vsp.sor.pub.web.action.SupplementalSearchResultsAction1635929407997&searchid=x93485&d-6484321-p=2"
         htmlSource = invokeUrl(nextPageUrl)
         getDataHtml(htmlSource)*/
        //}
        /* WebElement btn = engine.findById("btnContinue")
         btn.click()
         engine.wait(2000)

         WebElement btn2 = engine.findById("btnSearchFull")
         btn2.click()
         Thread.sleep(60000)
         //WebElement element = engine.findByXpath("//*[@id=\"pager1\"]/div/select")
         Select se = new Select(engine.findByXpath("//*[@id=\"pager1\"]/div/select"))
         se.selectByValue("1")
         getData(engine.getSource())
         engine.wait(30000)

         def lastPageMatcher = engine.getSource() =~ /class="k-link k-pager-nav k-pager-last" data-page="(\d+)"/
         def lastPage
         if (lastPageMatcher.find()) {
             lastPage = lastPageMatcher.group(1)
         }
         //Code for next page
         def last = Integer.parseInt(lastPage)
         for (int i = 1; i < last; i++) {
             WebElement nextPage = engine.findByXpath("//*[@id=\"pager1\"]/a[3]/span")
             WebElement nextPage = engine.findByXpath("//*[@id="mainArea"]/table/tbody/tr[2]/td[2]/table/tbody/tr[2]/td[2]/table/tbody/tr/td/table[2]/tbody/tr/td/span[4]/a[1]")
             //WebElement nextPage = engine.findByXpath("//*[@class=\"k-link k-pager-nav\"]/span")
             nextPage.click()
             Thread.sleep(20000)
             getData(engine.getSource())
             Thread.sleep(10000)
         }
 */
    }

    def getDataHtml(def html) {

        println html
        println "========================================================================================"
        addTextsToList(i++ + "Source: " + html)
        createFile()
        /* def tableMatcher = html =~ /(<table role="grid">.*<\\/tbody><\\/table>)/
         def table
         if (tableMatcher.find()) {
             table = tableMatcher.group(1)
         }
         println table

         def row
         def rowMatcher = table =~ /(<tr class="(?:k-master-row|k-alt k-master-row)".*?<\/td><\/tr>)/
         while (rowMatcher.find()) {
             row = rowMatcher.group(1)
             handleRow(row)
         }*/
    }
    def i = 1

    def handleRow(def row) {
        def entityName
        def dob
        def street, city, zipCode
        def address

        def dataMatcher
        //def dataMatcher=row=~/showOffenderDetailFromGrid\(\d{1,5}\)"><u>(\s?[A-Z\-]+, [A-Z]+\s?)<\/u>.*?>(\d{2}\/\d{1,2}\/\d{2,4})<.*?gridcell">([A-Z0-9\/ \.#\(\),]+)<.*?gridcell">(.*)<.*?gridcell">(.*)<\/td>/
        // if((dataMatcher =row=~/showOffenderDetailFromGrid\(\d{1,5}\)"><u>(\s?[A-Z\-]+, [A-Z]+\s?)<\/u>.*?>(\d{2}\/\d{1,2}\/\d{2,4})<.*?gridcell">([A-Z0-9\/ \.#\(\),]+)<.*?gridcell">([A-Z0-9 \.#\(\)]+)<.*?gridcell">(\d{5})<\/td>/)){
        if ((dataMatcher = row =~ /showOffenderDetailFromGrid\(\d{1,5}\)"><u>(\s*[A-Z\-\.\s]+,\s*[A-Z\-\.\s]+\s*)<\/u>.*?>\s*(\d{2}\/\d{1,2}\/\d{2,4})\s*<.*?gridcell">\s*([A-Z0-9\/\s\.#\(\),]+)\s*<.*?gridcell">(.*?)<\/td><.*?gridcell">(.*)<\/td>/)) {
            entityName = dataMatcher[0][1]
            entityName = entityName.toString().replaceAll(/([A-Z\-]+),\s*([A-Z\-]+)/, '$2 $1').trim()
            dob = dataMatcher[0][2]
            street = dataMatcher[0][3]
            city = dataMatcher[0][4]
            zipCode = dataMatcher[0][5]
            address = ", " + street + ", " + city + " " + zipCode
        }

        println row
        println "========================================================================================"
        addTextsToList(i++ + "\nRow: $row" + "\n\nEntity Name: $entityName\n" + "DOB: $dob\n" + "Address: $address")
        createFile()
        createEntity(entityName, address, dob)
    }


    def createEntity(def name, def address, def dateOfBirth) {

        def entity = null


        if (!name.toString().equals("") || !name.toString().isEmpty() || !name.toString().equals("null")) {
            def type = entityType.detectEntityType(name)
            entity = context.findEntity(["name": name, "type": type])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(type)
            }
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            address = sanitizeAddress(address)
            if (!address.equals("")) {
                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress)
                }
            }
            if (!dateOfBirth.toString().isEmpty()) {
                dateOfBirth = context.parseDate(new StringSource(dateOfBirth))
                entity.addDateOfBirth(dateOfBirth)
            }
        }
    }

    def sanitizeAddress(def address) {
        address = address.toString().replaceAll(/INCARCERATED\s*\/\s*(?:US MARSHAL CUSTODY|PRISON|CUSTODY)/, "").trim()
        address = address.toString().replaceAll(/,\s*,/, ",").trim()
        address = address.toString().replaceAll(/^(.*)$/, '$1, USA').trim()
        return address
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