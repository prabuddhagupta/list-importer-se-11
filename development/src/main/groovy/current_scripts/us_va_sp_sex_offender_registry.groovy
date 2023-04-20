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
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.util.concurrent.TimeUnit

context.setup([connectionTimeout: 25000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

UsVaSpSexOffenderRegistry script = new UsVaSpSexOffenderRegistry(context)

script.initParsing()

class UsVaSpSexOffenderRegistry {
    final ScrapianContext context
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def root = "https://sex-offender.vsp.virginia.gov"
    static def url = "https://sex-offender.vsp.virginia.gov/sor/supplementalSearchResults.html"
    final entityType
    final CAPTCHA_SOLVE_TIMEOUT = 120000

    // ========================================= CHROME DRIVER INITIALIZATION =========================================
    class ChromeDriverEngine {
        String CHROME_DRIVER_PATH = "/usr/bin/chromedriver"
        ChromeOptions options
        WebDriver driver

        ChromeDriverEngine() {
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH)

            options = new ChromeOptions()
            options.addArguments(
//                    "--headless",
                    "--disable-gpu",
                    "--ignore-certificate-errors",
                    "--window-size=1366,768",
                    "--silent",
//                    "--blink-settings=imagesEnabled=false" // Don't load images
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

        List<WebElement> findAllByXpath(String xpath) {
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
    // ========================================= CHROME DRIVER INITIALIZATION =========================================


    UsVaSpSexOffenderRegistry(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)

    }


    def initParsing() {
        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(url, 5)

        getData(engine)


        engine.shutdown()
    }

    def getData(ChromeDriverEngine engine) {
        def lastPage
        boolean wait = true

        if (wait)
            engine.wait(CAPTCHA_SOLVE_TIMEOUT)

        WebElement accpetButton = engine.findByXpath("//*[@value=\"Accept\"]")
        accpetButton.click()
        engine.wait(1000)

        def firstRowData1 = engine.findByXpath("//*[@id=\"offenderList\"]/tbody/tr[1]")
        engine.wait(1000)

        def lastPageMatch = engine.findByXpath("//*[@id=\"mainArea\"]/table/tbody/tr[2]/td[2]/table/tbody/tr[2]/td[2]/table/tbody/tr/td/table[2]/tbody/tr/td/span[2]/a[6]")
        lastPageMatch = lastPageMatch.getAttribute("href")
        lastPageMatch = lastPageMatch.toString()

        def lastPageMatcher = lastPageMatch =~ /(?ism)\d-p=(.*)/
        if (lastPageMatcher.find()) {
            lastPage = lastPageMatcher.group(1)
        }

        int lastNum = Integer.valueOf(lastPage)
        def html1 = engine.getSource()
        def block1 = html1 =~ /(?ism)<tr class=(.*)<\\/td><\\/tr>/

        if (block1.find()) {
            def each_block = block1 =~ /(?ism)<tr class=(.*?)<\\/td><\\/tr>/

            while (each_block.find()) {
                def all_column_data = each_block =~ /(?ism)<td style="width: 10%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 03%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 08%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 15%">(.*?)<\\/td>\n<td style="width: 30%">(.*?)<\\/td>\n<td style="width: 14%">(.*?)<\\/td><\\/tr>/

                if (all_column_data.find()) {
                    def first_name = all_column_data.group(1)
                    def last_name = all_column_data.group(2)
                    def middle_name = all_column_data.group(3)
                    def date_of_birth = all_column_data.group(5).toString()
                    def date_of_conviction = all_column_data.group(6)
                    def offence = "Sex Crime : " + all_column_data.group(9).toString()
                    def code_of_virginia = all_column_data.group(10)

                    def entity_name = last_name + ' ' + middle_name + ' ' + first_name
                    def identification = code_of_virginia.toString()

                    createEntity(entity_name, date_of_conviction, offence, date_of_birth)
                }
            }
        }

        for (; true;) {
            int i
            for (i = 2; i <= lastNum; i++) {

                if (i % 50 == 1) {
                    def pageNo = engine.findByXpath("//*[@title=\"Go to page " + i + "\"]")
                    pageNo.click()

                    wait = true

                    if (wait)

                        engine.wait(CAPTCHA_SOLVE_TIMEOUT)

                    accpetButton = engine.findByXpath("//*[@value=\"Accept\"]")
                    accpetButton.click()
                    engine.wait(1000)

                    def firstRowData = engine.findByXpath("//*[@id=\"offenderList\"]/tbody/tr[1]")
                    engine.wait(500)

                    def html = engine.getSource()
                    def block = html =~ /(?ism)<tr class=(.*)<\\/td><\\/tr>/
                    if (block.find()) {

                        def each_block = block =~ /(?ism)<tr class=(.*?)<\\/td><\\/tr>/

                        while (each_block.find()) {
                            def all_column_data = each_block =~ /(?ism)<td style="width: 10%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 03%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 08%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 15%">(.*?)<\\/td>\n<td style="width: 30%">(.*?)<\\/td>\n<td style="width: 14%">(.*?)<\\/td><\\/tr>/

                            if (all_column_data.find()) {
                                def first_name = all_column_data.group(1)
                                def last_name = all_column_data.group(2)
                                def middle_name = all_column_data.group(3)
                                def date_of_birth = all_column_data.group(5).toString()
                                def date_of_conviction = all_column_data.group(6)
                                def offence = "Sex Crime : " + all_column_data.group(9).toString()
                                def code_of_virginia = all_column_data.group(10)
                                def entity_name = last_name + ' ' + middle_name + ' ' + first_name

                                def identification = code_of_virginia.toString()

                                createEntity(entity_name, date_of_conviction, offence, date_of_birth)

                            }
                        }
                    }

                } else {
                    def pageNo = engine.findByXpath("//*[@title=\"Go to page " + i + "\"]")
                    pageNo.click()
                    engine.wait(1000)
                    def firstRowData = engine.findByXpath("//*[@id=\"offenderList\"]/tbody/tr[1]")
                    engine.wait(500)
                    def html = engine.getSource()
                    def block = html =~ /(?ism)<tr class=(.*)<\\/td><\\/tr>/

                    if (block.find()) {
                        def each_block = block =~ /(?ism)<tr class=(.*?)<\\/td><\\/tr>/

                        while (each_block.find()) {
                            def all_column_data = each_block =~ /(?ism)<td style="width: 10%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 03%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 08%">(.*?)<\\/td>\n<td style="width: 05%">(.*?)<\\/td>\n<td style="width: 15%">(.*?)<\\/td>\n<td style="width: 30%">(.*?)<\\/td>\n<td style="width: 14%">(.*?)<\\/td><\\/tr>/

                            if (all_column_data.find()) {
                                def first_name = all_column_data.group(1)
                                def last_name = all_column_data.group(2)
                                def middle_name = all_column_data.group(3)
                                def date_of_birth = all_column_data.group(5).toString()
                                def date_of_conviction = all_column_data.group(6)
                                def offence = "Sex Crime : " + all_column_data.group(9).toString()
                                def code_of_virginia = all_column_data.group(10)
                                def entity_name = last_name + ' ' + middle_name + ' ' + first_name

                                def identification = code_of_virginia.toString()

                                createEntity(entity_name, date_of_conviction, offence, date_of_birth)
                            }
                        }
                    }
                }


            }

            Thread.sleep(4000)
            break
        }
    }

    def createEntity(def name, def event_date, def description, def dateOfBirth) {
        def entity = null

        def entityName = name
        def entityType = detectEntity(name)

        entityName = entityName.toString().trim()
        entity = context.findEntity("name": entityName, "type": entityType)
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(entityName)
            entity.setType(entityType)
        }

        if (dateOfBirth) {
            if (dateOfBirth.size() > 0) {
                entity.addDateOfBirth("-/-/" + dateOfBirth)
            }
        }

        ScrapeAddress scrapeAddress = new ScrapeAddress()
        scrapeAddress.setCountry("United States")
        scrapeAddress.setProvince("Virginia")
        entity.addAddress(scrapeAddress)

        ScrapeEvent event = new ScrapeEvent()
        if (description) {
            event.setDescription(description.toString())
        }

        if (event_date != null) {
            def sDate = context.parseDate(new StringSource(event_date), ["MM/dd/yyyy"] as String[])
            event.setDate(sDate)
        }
        entity.addEvent(event)
    }

    def detectEntity(def name) {
        def type
        if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:Phamacy|Druggists|Homecare|Neuromedical|Clinic|Medicine|Hearing\s+Aid|Medi\-Screens|Softech|India|Trustee)/) {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:Christie L|William House|Ben Parker|Margaret Leigh|Philip L|Ashley Nicole|Honey|Logic|Tangella Jackson|Kathy)/) {
                type = "P"
            }
        }
        return type
    }


    def invoke(url, cache = false, tidy = false, clean = false) {
        return context.invoke([url: url, tidy: tidy, cache: cache, clean: clean])
    }


    def invokeBinary(url, type = null, paramsMap = null, cache = true, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }
} 