package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import org.apache.commons.io.FileUtils

import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.io.*

context.setup([connectionTimeout: 10000000, socketTimeout: 20000000, retryCount: 200, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Pdf_Downloader script = new Pdf_Downloader(context)
script.initParsing()


class Pdf_Downloader {

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
    def mainUrl = "http://kfi.ky.gov/new_bulletin.aspx?bullid=3"

    def url = 'https://www.finra.org/arbitration-mediation/arbitration-awards-online?aao_radios=all&field_case_id_text=&search=&field_forum_tax=All&field_special_case_type_tax=All&field_core_official_dt%5Bmin%5D=&field_core_official_dt%5Bmax%5D=&field_document_type_tax%5B4224%5D=4224&page='
    def prefexPdfUrl = 'https://www.finra.org'

    Pdf_Downloader(context) {
        this.context = context
    }
    def k = 1
    def i = 1

    def initParsing() {
        def html, start, end
        html = invokeUrl(mainUrl)
        //handleData(html)
        //       html = sanitizeHtml(html)
        //html = html.toString().replaceAll(/(?is)^.+Documents\/MackhouseCapital.pdf/, "")
        //html = html.toString().replaceAll(/(?is)^.+Inc Et Al 101411\.pdf/, "")
        //html = html.toString().replaceAll(/(?is)^.+Thomas Smith III 011609\.pdf/, "")
        //html = html.toString().replaceAll(/(?is)^.+Opportunity 100206\.pdf/, "")
        // html = html.toString().replaceAll(/(?is)^.+Napper 081105\.pdf/, "")
//        html = html.toString().replaceAll(/(?is)^.+Sunset Capital/, "")
        //html = html.replace(/(?is)^.*14AH0021B<\/a>/, "")
        //html = html.replace(/(?is)^.*13AH0227<\/a>/, "")
        //html = html.replace(/(?is)^.*11AH 0076 \(2\)<\\/a>/, "")
        // html = html.replace(/(?is)^.*101411<\/a>/, "")
        //html = html.replace(/(?is)^.*081105<\/a>/, "")

        // println html
        def pdfUrl
        def pdfUrlMatcher
        //pdfUrlMatcher = html =~ /(?i)<a\s(?:ti.*)?href="(.*\.pdf)".*>/
        pdfUrlMatcher = html =~ /(?i)href='(Documents.*?\.pdf)'/
        //def i = 223
        // String url = "http://kfi.ky.gov/Documents/Crossroad%20Community%20Church%20Inc%20Brian%20Tome%202020-AH-0026.pdf";
/*
        try {
            //downloadUsingNIO(url, "/Users/pankaj/sitemap.xml");

            downloadUsingStream(url, "/home/mahadi/Downloads/pdfDownload/Kentucky.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        while (pdfUrlMatcher.find()) {
            pdfUrl = headerUrl + pdfUrlMatcher.group(1)
            pdfUrl = pdfUrl.toString().replaceAll(/\s+/, "%20")
            pdfUrl = pdfUrl.toString().replaceAll(/\(/, "%28")
            pdfUrl = pdfUrl.toString().replaceAll(/\)/, "%29")
            println k++ + "\t\t\t" + pdfUrl

            //URLConnection urlc = url.openConnection();
            //Make sure that this directory exists
            String dirName = "/home/mahadi/Downloads/New Kentucky"
            try {
                if (i < 10) {
                    //saveFileFromUrlWithCommonsIO("/home/sekh/Kentucky/Kentucky00" + i + ".pdf", pdfUrl)
                    pdfDownlaod("/home/sekh/Kentucky/Kentucky00" + i + ".pdf", pdfUrl)
                    //saveFileFromUrlWithJavaIO("/home/mahadi/Downloads/New Kentucky/202012/Kentucky00" + i + ".pdf", pdfUrl)
                    //downloadUsingStream(pdfUrl, "/home/mahadi/Downloads/New Kentucky/202012/Kentucky00" + i + ".pdf")
                } else if (i <= 99) {
                    //saveFileFromUrlWithCommonsIO("/home/sekh/Kentucky/Kentucky0" + i + ".pdf", pdfUrl)
                    pdfDownlaod("/home/sekh/Kentucky/Kentucky0" + i + ".pdf", pdfUrl)
                    // saveFileFromUrlWithJavaIO("/home/mahadi/Downloads/New Kentucky/202012/Kentucky0" + i + ".pdf", pdfUrl)
                    //downloadUsingStream( pdfUrl, "/home/mahadi/Downloads/New Kentucky/202012/Kentucky0" + i + ".pdf")
                } else {
                    pdfDownlaod("/home/sekh/Kentucky/Kentucky" + i + ".pdf", pdfUrl)
                    //saveFileFromUrlWithJavaIO("/home/mahadi/Downloads/New Kentucky/202012/Kentucky" + i + ".pdf", pdfUrl)
                    //downloadUsingStream(pdfUrl,"/home/mahadi/Downloads/New Kentucky/202012/Kentucky" + i + ".pdf")

                }
                i++
                System.out.println("finished");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

    def handleData(link) {
        // Page numbers
        for (def i = 0; i < 3912; i += 80) {
            link = url + i.toString()
            downloadFinraPdfs(invokeUrl(link))
        }
        //capturePdfAndDate(invokeUrl(link))
    }

    def downloadFinraPdfs(html) {
        def separatorRegex = /(?s)<tr>.*/
        def separatorMatcher = html =~ separatorRegex
        if (separatorMatcher.find()) html = separatorMatcher.group()
        def pdfDateRegex = /(?s)href="(.*?\.pdf)/
        def pdfDateMatcher = html =~ pdfDateRegex
        while (pdfDateMatcher.find()) {
            def pdfUrl = prefexPdfUrl + pdfDateMatcher.group(1)

            def pdfNameMatcher

            // def text = pdfToTextConverter(pdfUrl)
            if ((pdfNameMatcher = pdfUrl =~ /^.*aao_documents\/(\d+.*)/)) {
                def pdfName
                pdfName = pdfNameMatcher[0][1]
                println pdfName
                try {
                    saveFileFromUrlWithCommonsIO("/home/mahadi/Downloads/FINRA/Finra_" + pdfName, pdfUrl)
                    System.out.println("finished");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void downloadUsingStream(String file, String urlStr) throws IOException {

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
    // Using Commons IO library
    // Available at http://commons.apache.org/io/download_io.cgi
    def saveFileFromUrlWithCommonsIO(String fileName, String fileUrl) throws MalformedURLException, IOException {
        //println fileUrl
        try {
            FileUtils.copyURLToFile(fileUrl, fileName)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}
