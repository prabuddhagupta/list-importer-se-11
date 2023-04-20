package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import org.apache.commons.io.FileUtils
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.TimeUnit

context.setup([connectionTimeout: 10000000, socketTimeout: 20000000, retryCount: 200, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

File_Downloader script = new File_Downloader(context)
script.initParsing()

class File_Downloader {

    final addressParser
    final entityType
    final def ocrReader
    ScrapianContext context

    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")

    final def root = 'https://sos.vermont.gov'
    final def url = 'https://sos.vermont.gov/opr/complaints-conduct-discipline/monthly-discipline-reports/'
    final def path = '/home/mahadi/Downloads/Sex_Offender_Registry.csv'
    static def tempDir = System.getProperty("java.io.tmpdir") + "/Illions"
    def xlsxUrl2
    def xlsxUrl1
    def texts = []
    def nameList = []

    def createFile() {
        def newFile = new File("Illions_Sex.txt")
        def rows = ''
        texts.each { rows = "$rows\n==================================================\n$it" }
        //texts.each { rows -> newFile.append("$rows\n==================================================\n") }
        newFile.write(rows)
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
                "--blink-settings=imagesEnabled=false", // Don't load images
            )
            Map<String, Object> prefs = new HashMap<>();
            // prefs.put("download.default_directory", "/home/mahadi/Documents/App/RDCScrapper/output")
            prefs.put("download.default_directory", "/home/sekh/us_vt_unlicensed_professionals_2_pdfs")
            prefs.put("download.prompt_for_download", false)
            prefs.put("plugins.always_open_pdf_externally", true)
            prefs.put("Chrome PDF Viewer", false)
            prefs.put("download.extensions_to_open", "applications/pdf")


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

    File_Downloader(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        this.context = context
    }

    def initParsing() {
//      def html = invokeUrl(url)
        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(this.url, 5000)
        handlePdfDownload(engine)
        getLink(engine)
        sleep(10000)
        engine.shutdown()

    }

    def handlePdfDownload(ChromeDriverEngine engine) {
        // println engine.getSource()
        engine.get("https://sos.vermont.gov/media/i4tlmwx3/2018.pdf")
        /*  WebElement element = engine.findByXpath("/html/body/section[2]/div/div/div[2]/p[4]/a[1]")
          element.click()*/
        engine.wait(10000)
    }

    def getLink(ChromeDriverEngine engine) {
        def html = engine.getSource()

        println html
        def linkMatcher, link, pdfLink, pdfText
        def linkCount = 1
        linkMatcher = html =~ /<a rel="noopener" href="(.*?)" target="_blank" title="OPR Press Release/
        while (linkMatcher.find()) {
            link = linkMatcher.group(1)
            pdfLink = root + link
            engine.get(pdfLink)
            sleep(10000)
            //println(linkCount)
            println(pdfLink)

            /*def pdfName
            def pdfNameMatcher = pdfLink =~ /\/media\/.*?\/(.*?\.(?:pdf|doc[x]?))/
            if (pdfNameMatcher.find()) {
                pdfName = pdfNameMatcher.group(1)
                pdfName = pdfName.replaceAll(/\-/, "_")
                println(pdfName)
            }

            try {
//                downloadUsingStream("file:/home/fabianparsiageorge/Desktop/SEBPO/RDCScrapper2/RDCScrapper/output/us_vt_unlicensed_professionals_2_pdfs", pdfLink)
                pdfDownloader2("/home/sekh/us_vt_unlicensed_professionals_2_pdfs/us_vt_" + pdfName, pdfLink)
//                pdfDownlaod("/home/fabianparsiageorge/Desktop/SEBPO/RDCScrapper2/RDCScrapper/output/us_vt_unlicensed_professionals_2_pdfs/" + pdfName, pdfLink)
                System.out.println("finished");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            linkCount++*/
        }
    }

    def saveFileFromUrlWithCommonsIO(def fileName, def fileUrl) throws MalformedURLException, IOException {
        //println fileUrl
        try {
            FileUtils.copyURLToFile(new URL(fileUrl), new File(fileName))
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    def pdfDownlaod(String filename, String url) {
        while (url) {
            new URL(url).openConnection().with { conn ->
                conn.instanceFollowRedirects = false
                url = conn.getHeaderField("Location")
                if (!url) {
                    new File(filename).withOutputStream { out ->
                        conn.inputStream.with { inp ->
                            out << inp
                            inp.close()
                        }
                    }
                }
            }
        }
    }

    def pdfDownloader2(def toFile, def fromFile) {
        try {

            URL website = new URL(fromFile);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(toFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


