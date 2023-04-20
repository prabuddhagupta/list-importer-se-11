package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
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

context.setup([connectionTimeout: 25000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

Us_Vt_Unlicensed_Professionals_2 script = new Us_Vt_Unlicensed_Professionals_2(context)
script.initParsing()

class Us_Vt_Unlicensed_Professionals_2 {
    final ScrapianContext context
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")

    static def root = "https://sos.vermont.gov"
    static def url = "https://sos.vermont.gov/opr/complaints-conduct-discipline/monthly-discipline-reports/"
    final addressParser
    final entityType
    final def ocrReader
    final captcha_timout = 50000

    // ========================================= CHROME DRIVER CONTROLLER DEFINITION =========================================
    private class ChromeDriverEngine {
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
//                    "--headless", // Use this switch to use Chromium without any GUI
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
         * This method navigates to the previous webpage in the current window.
         */
        void goBack() {
            driver.navigate().back()
        }

        /**
         * This method navigates to the next webpage in the current window.
         */
        void goForward() {
            driver.navigate().forward()
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
            WebDriverWait wait = new WebDriverWait(driver, 10)
            wait.until(ExpectedConditions.elementToBeClickable(new By.ByXPath(xpath)))
        }
    }
    // ========================================= CHROME DRIVER CONTROLLER DEFINITION =========================================

    Us_Vt_Unlicensed_Professionals_2(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        ocrReader = moduleFactory.getOcrReader(context)
        addressParser.reloadData()
    }

    def initParsing() {
        getLink()
        ChromeDriverEngine engine = new ChromeDriverEngine()
        engine.get(this.url, 5000)
        boolean okay = true

//        if(okay) {
//            engine.wait(captcha_timout)
//        }


        def html = engine.getSource()
//        println(html)
        getLink(html)
//        getNameTypeFromPage(engine)
        engine.shutdown()
    }

    def getLink(def html) {

        def linkMatcher, link, pdfLink, pdfText
        def linkCount = 1
        linkMatcher = html =~ /<a rel="noopener" href="(.*?)" target="_blank" title="OPR Press Release/
        while (linkMatcher.find()) {
            link = linkMatcher.group(1)
            pdfLink = root + link
            println(linkCount)
            println(pdfLink)

            def pdfName
            def pdfNameMatcher = pdfLink =~ /\/media\/.*?\/(.*?\.pdf)/
            if (pdfNameMatcher.find()) {
                pdfName = pdfNameMatcher.group(1)
            }

            try {
//                downloadUsingStream("file:/home/fabianparsiageorge/Desktop/SEBPO/RDCScrapper2/RDCScrapper/output/us_vt_unlicensed_professionals_2_pdfs", pdfLink)
                downloadUsingStream(pdfLink, "/home/sekh/us_vt_unlicensed_professionals_2_pdfs")
                System.out.println("finished");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            linkCount++
        }

//        new File("us_vt_unlicensed_professionals_2_pdfs").listFiles().each { File f ->
//            if (f.isFile() && f.getName().endsWith(".pdf")) {
//                pdfText = pdfToTextConverter("file://" + f)
//                println pdfText
//            }
//        }

//            pdfText = pdfToTextConverter(pdfLink)
//            if(linkCount == 1) {
//                pdfText = pdfToTextConverter("/home/fabianparsiageorge/Desktop/SEBPO/RDCScrapper2/RDCScrapper/output/reports/january-2021.pdf")
//                captureData(pdfText)
//            }
//            captureData(pdfText)



    }

    private static void downloadUsingStream(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int count = 0;
        while ((count = bis.read(buffer, 0, 1024)) != -1) {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
    }


    def saveFileFromUrlWithCommonsIO(String fileName, String fileUrl) throws MalformedURLException, IOException {
        //println fileUrl
        FileUtils.copyURLToFile(new URL(fileUrl), new File(fileName));
    }

    public static void saveFileFromUrlWithJavaIO(String fileName, String fileUrl)
            throws MalformedURLException, IOException {
        BufferedInputStream br = null;
        FileOutputStream fout = null;
        try {
            br = new BufferedInputStream(new URL(fileUrl).openStream())
            fout = new FileOutputStream(fileName)
            byte[] data = new byte[1024]
            int count;
            while ((count = br.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } finally {
            if (br != null)
                br.close()
            if (fout != null)
                fout.close()
        }
    }

    def pdfToTextConverter(def pdfUrl) {
//        try {
//            List<String> pages = ocrReader.getText(pdfUrl)
//            return pages
//        } catch (NoClassDefFoundError e) {
            def pdfFile = invokeBinary(pdfUrl)
            def pmap = [:] as Map
            pmap.put("1", "-layout")
            pmap.put("2", "-enc")
            pmap.put("3", "UTF-8")
            pmap.put("4", "-eol")
            pmap.put("5", "dos")
            def pdfText = context.transformPdfToText(pdfFile, pmap)
            return pdfText
//        }
//    catch (IOException e) {
//            println(e)
//            return "PDF has no page"
//        }
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }
}