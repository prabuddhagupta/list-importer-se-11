package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import org.apache.commons.io.FileUtils

context.setup([connectionTimeout: 1000000000, socketTimeout: 1000000000, retryCount: 200, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Test_Script_Invoke script = new Test_Script_Invoke(context)
script.initParsing()


class Test_Script_Invoke {

    final addressParser
    final entityType

    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context

    def headerUrl = "http://kfi.ky.gov/"
    //def mainUrl = headerUrl + "/DOPLPublic/FormalActionBrowser.aspx?Bureau=REA"
    //def mainUrl = "https://www.weldsheriff.com/apps1/mostwanted"
    // def mainUrl = "https://www.oid.ok.gov/regulated-entities/real-estate-appraiser-board/disciplinary-orders/2020-disciplinary-orders/"
    //def mainUrl = "http://kfi.ky.gov/new_bulletin.aspx?bullid=3"
   // def mainUrl = "http://www.lapdonline.org/north_hollywood_most_wanted"
    //def mainUrl = "http://www.lapdonline.org/north_hollywood_most_wanted/most_wanted_view/12764"
   // def mainUrl = "https://www.crimestoppersvic.com.au/help-solve-crime/wanted-persons/"
    def mainUrl = "https://www.crimestoppersvic.com.au/help-solve-crime/wanted-persons"
    Test_Script_Invoke(context) {
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context
    }
    def k = 20

    def initParsing() {
        def html, start, end
        html = invokeUrl(mainUrl)
//        html = sanitizeHtml(html)
//        html = html.toString().replaceAll(/(?is)^.+Sunset Capital/, "")
        //html = html.replace(/(?is)^.*14AH0021B<\/a>/, "")
        //html = html.replace(/(?is)^.*13AH0227<\/a>/, "")
        //html = html.replace(/(?is)^.*11AH 0076 \(2\)<\\/a>/, "")
        // html = html.replace(/(?is)^.*101411<\/a>/, "")
        //html = html.replace(/(?is)^.*081105<\/a>/, "")

        println html
//        def pdfUrl
//        def pdfUrlMatcher
//        //pdfUrlMatcher = html =~ /(?i)<a\s(?:ti.*)?href="(.*\.pdf)".*>/
//        pdfUrlMatcher = html =~ /(?i)href='(Documents.*?\.pdf)'/
//        def i = 20
        // String url = "http://kfi.ky.gov/Documents/Crossroad%20Community%20Church%20Inc%20Brian%20Tome%202020-AH-0026.pdf";
/*
        try {
            //downloadUsingNIO(url, "/Users/pankaj/sitemap.xml");

            downloadUsingStream(url, "/home/mahadi/Downloads/pdfDownload/Kentucky.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
       /* while (pdfUrlMatcher.find()) {
            pdfUrl = headerUrl + pdfUrlMatcher.group(1)
            pdfUrl = pdfUrl.toString().replaceAll(/\s+/, "%20")
            pdfUrl = pdfUrl.toString().replaceAll(/\(/, "%28")
            pdfUrl = pdfUrl.toString().replaceAll(/\)/, "%29")
            println k++ + "\t\t\t" + pdfUrl

            //URLConnection urlc = url.openConnection();
            // Make sure that this directory exists
            String dirName = "/home/mahadi/Downloads/New Kentucky"
            try {
                if (i < 20) {
                    saveFileFromUrlWithCommonsIO("/home/mahadi/Downloads/New Kentucky/2019/Kentucky00" + i + ".pdf", pdfUrl)
                } else if (i <= 99) {
                    saveFileFromUrlWithCommonsIO("/home/mahadi/Downloads/New Kentucky/2019/Kentucky0" + i + ".pdf", pdfUrl)
                } else {
                    saveFileFromUrlWithCommonsIO("/home/mahadi/Downloads/New Kentucky/2019/Kentucky" + i + ".pdf", pdfUrl)
                }
                i++
                System.out.println("finished");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        /* def downloadFiles = { sourceUrls ->
             def stagingDir = "/home/mahadi/Downloads/pdfDownload"
             new File(stagingDir).mkdirs()
             sourceUrls.each { sourceUrl ->
                 def filename = sourceUrl.tokenize('/')[-1]
                 def file = new FileOutputStream("$stagingDir/$filename")
                 def protocolUrlTokens = sourceUrl.tokenize(':')
                 def sourceUrlAsURI = new URI(protocolUrlTokens[0],
                     protocolUrlTokens[1..(protocolUrlTokens.size - 1)].join(":"), "\\")
                 def out = new BufferedOutputStream(file)
                 out << sourceUrlAsURI.toURL().openStream()
                 out.close()
             }
         }

         downloadFiles(
             ["http://kfi.ky.gov/Documents/Crossroad%20Community%20Church%20Inc%20Brian%20Tome%202020-AH-0026.pdf","/Kentucky001.pdf"
             ])*/
        //For page 1
        /*html = getParam(html)
        def noMatcher1 = html =~ /(?ism)(\d+) of (\d+)/
        if(noMatcher1.find()){
            start = noMatcher1.group(1)
            start = Integer.parseInt(start)
            end = noMatcher1.group(2)
            end = Integer.parseInt(end)
        }
        //For page 2 to end
        while(start<end){
            html = getParam(html)
            def noMatcher = html =~ /(?ism)(\d+) of (\d+)/
            if(noMatcher.find()){
                start = noMatcher.group(1)
                start = Integer.parseInt(start)
            }
        }
*/
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

    def sanitizeHtml(def html) {
        html = html.toString().replaceAll(/(?i)href='(Documents\/Three Star Leasing Inc.*Et Al?\.pdf)'.*11\/12\/2013/, "")
        html = html.toString().replaceAll(/(?i)href='(Documents\/JMack.*?14AH0021B\.pdf)'.*11\/10\/2014/, "")
        html = html.toString().replaceAll(/(?i)id='details72013'.+?11AH 0076\s\(2\).*?12\/19\/2013/, "")
        html = html.toString().replaceAll(/(?i)href='(Documents\/Morgan.*?101411\.pdf)'.*10\\/14\\/2011/, "")
        html = html.toString().replaceAll(/(?i)href='(Documents\/Cleark Creek.*?081105\.pdf)'.*08\\/11\\/2005/, "")
        html = html.toString().replaceAll(/(?i)href='(Documents\/Burchett Oilco Inc.*?060105\.pdf)'.*06\/01\/2005/, "")
        html = html.toString().replaceAll(/(?i)href='(Documents\/US Petroleum.*?052005\.pdf)'.*05\/20\/2005/, "")
        return html
    }
    // Using Java IO
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
    // Using Commons IO library
    // Available at http://commons.apache.org/io/download_io.cgi
    def saveFileFromUrlWithCommonsIO(String fileName, String fileUrl) throws MalformedURLException, IOException {
        FileUtils.copyURLToFile(new URL(fileUrl), new File(fileName));
    }

    def getParam(def html) {
        def headers = getHeader()

        def __VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, __EVENTTARGET, __EVENTARGUMENT

        def viewStateMatcher = html =~ /(?ism)<input type="hidden" name="__VIEWSTATE".*?VALUE="(.*?)"/
        def viewStateGeneratorMatcher = html =~ /(?ism)<input type="hidden" name="__VIEWSTATEGENERATOR".*?VALUE="(.*?)"/
        // def lastFocusMatcher = html =~ /(?ism)<input type="hidden" name="__LASTFOCUS".*?VALUE="(.*?)"/
        def eventTargetMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTTARGET".*?VALUE="(.*?)"/
        def eventArgumentMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTARGUMENT".*?VALUE="(.*?)"/
        def eventValidationMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTVALIDATION".*?VALUE="(.*?)"/
        if (viewStateMatcher.find()) {
            __VIEWSTATE = viewStateMatcher.group(1)
        }
        if (viewStateGeneratorMatcher.find()) {
            __VIEWSTATEGENERATOR = viewStateGeneratorMatcher.group(1)
        }
        if (eventValidationMatcher.find()) {
            __EVENTVALIDATION = eventValidationMatcher.group(1)
        }
        if (eventTargetMatcher.find()) {
            __EVENTTARGET = eventTargetMatcher.group(1)
        }
        if (eventArgumentMatcher.find()) {
            __EVENTARGUMENT = eventArgumentMatcher.group(1)
        }
        //  println html
//        html = html.toString().replaceAll(/(?is).*(?=ctl00[$]cphMainContent[$]ddlProfession).*width:525px;">/, "")
//        html = html.toString().replaceAll(/(?is)(?<=Interior Designer<\\/option>).*/, "")
//        def professionMatcher = html =~ /(?i)value="(\d+)"/
//        def profId
//
        def params = getParams(__VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, __EVENTTARGET, __EVENTARGUMENT)
        html = invokePost(mainUrl, params, false, headers, false)
        println html
        handleHtml(html)
    }

    def handleHtml(def html) {
        def headers = getHeader()
        def __VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, __EVENTTARGET, __EVENTARGUMENT

        def viewStateMatcher = html =~ /(?ism)<input type="hidden" name="__VIEWSTATE".*?VALUE="(.*?)"/
        def viewStateGeneratorMatcher = html =~ /(?ism)<input type="hidden" name="__VIEWSTATEGENERATOR".*?VALUE="(.*?)"/
        // def lastFocusMatcher = html =~ /(?ism)<input type="hidden" name="__LASTFOCUS".*?VALUE="(.*?)"/
        //def eventTargetMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTTARGET".*?VALUE="(.*?)"/
        def eventArgumentMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTARGUMENT".*?VALUE="(.*?)"/
        def eventValidationMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTVALIDATION".*?VALUE="(.*?)"/

        if (viewStateMatcher.find()) {
            __VIEWSTATE = viewStateMatcher.group(1)
        }
        if (viewStateGeneratorMatcher.find()) {
            __VIEWSTATEGENERATOR = viewStateGeneratorMatcher.group(1)
        }
        if (eventValidationMatcher.find()) {
            __EVENTVALIDATION = eventValidationMatcher.group(1)
        }
        /*  if (eventTargetMatcher.find()) {
              __EVENTTARGET = eventTargetMatcher.group(1)
          }*/
        if (eventArgumentMatcher.find()) {
            __EVENTARGUMENT = eventArgumentMatcher.group(1)
        }

        def nextPageMatcher = html =~ /(ctl00[$]CPH1[$]btnNext)/
//
//            while (nextPageMatcher.find()) {
//                __EVENTTARGET = nextPageMatcher.group(1)

        def params = getParamsForNext(__VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, __EVENTARGUMENT)
        html = invokePost(mainUrl, params, false, headers, false)
        println html
        // }
    }

    def getParams(__VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, __EVENTTARGET, __EVENTARGUMENT) {
        def param = [:]

        param["__VIEWSTATE"] = __VIEWSTATE
        param["__VIEWSTATEGENERATOR"] = __VIEWSTATEGENERATOR
        param["__EVENTVALIDATION"] = __EVENTVALIDATION
        param["__EVENTTARGET"] = __EVENTTARGET
        param["__EVENTARGUMENT"] = __EVENTARGUMENT

        return param
    }

    def getParamsForNext(__VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, __EVENTARGUMENT) {
        def param = [:]
        param["__VIEWSTATE"] = __VIEWSTATE
        param["__VIEWSTATEGENERATOR"] = __VIEWSTATEGENERATOR
        param["__EVENTVALIDATION,"] = __EVENTVALIDATION
        param["__EVENTTARGET"] = "ctl00\$CPH1\$btnNext"
        param["__EVENTARGUMENT"] = __EVENTARGUMENT

        // param["ctl00\$CPH1\$btnNext"] = ""
        return param
    }

    def getHeader() {
        def headers = [
            "Accept"                   : "application/json, text/javascript, */*; q=0.01",
            "Accept-Language"          : "en-US,en;q=0.9",
            "Connection"               : "keep-alive",
            "Content-Type"             : "application/x-www-form-urlencoded",
            // "Cookie"                   : "ASP.NET_SessionId=sad41byijvaewuiofjiseo15; StateOfIdaho=2420286884.47873.0000; TS0134bede=013f9eef6978b64c7e630bdb3e73c86b58aff8261335246117d32902641589bae961b4f7630d05c20d017b6c44f0c95b04e8811486a78059f09c1cb330fcfc08e1fdbe89298394be349fb0923dd7eb9d7572944723",
            "Host"                     : "dopl.idaho.gov",
            "Origin"                   : "https://dopl.idaho.gov",
            "Referer"                  : "https://dopl.idaho.gov/DOPLPublic/FormalActionBrowser.aspx?Bureau=REA",
            "Sec-Fetch-Dest"           : "document",
            "Sec-Fetch-Mode"           : "navigate",
            "Sec-Fetch-Site"           : "same-origin",
            "Sec-Fetch-User"           : "?1",
            "Upgrade-Insecure-Requests": "1",
        ]
        return headers
    }

    def invokePost(def url, def paramsMap, def cache = false, def headersMap = [:], def tidy = true) {
        return context.invoke([url: url, tidy: tidy, type: "POST", params: paramsMap, headers: headersMap, cache: cache]);
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = true, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}
