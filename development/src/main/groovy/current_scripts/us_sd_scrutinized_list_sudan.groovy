package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.ScrapianEngine
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent


import java.nio.file.Files
import java.nio.file.Paths

/**
 * Date: 08.22.2019
 * Date: 29.04.2021
 * Dtae:27.01.2022
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_sd_scrutinized_list_sudan script = new Us_sd_scrutinized_list_sudan(context)
script.initParsing()

class Us_sd_scrutinized_list_sudan {
    final ScrapianContext context
    final ocrReader
    final String root = "http://sdlegislature.gov"
    final String url = root + "/Reference_Materials/RequiredReports.aspx"
    final String JsonUrl = "https://sdlegislature.gov/api/Documents/DocumentType?Type=57"
    final def moduleFactory = ModuleLoader.getFactory("7dd0501f220ad1d1465967a4e02f6ff17d7d9ff9")

    //Set Path
    def fileLocation = "https://github.com/RegDC/li-internal-data"
    def subFolderName = "us_sd_scrutinized_list_sudan"
    //def fileName = "/us_sd_scrutinized_list_sudan.pdf"
    def fileName = "/us_sd_scrutinized_list_sudan_2021.pdf"
    def scrape = new ScrapianEngine()
    def pathToFolder = scrape.getGitHubURLAsString(fileLocation, subFolderName, "23fb0d2f67974fe4a07117cc3f74bcaf5985d986")
    //get pdf file from github
    def pathToFile = pathToFolder.replaceAll("file:/", "") + fileName

    Us_sd_scrutinized_list_sudan(context) {
        this.context = context
        ocrReader = moduleFactory.getOcrReader(context)
    }

    private enum FIELDS
    {
        EVNT_DATE, EVNT_DETAIL, URL
    }

//------------------------------Initial part----------------------//

    def initParsing() {

        //Invoke main URL
        def html = invoke(JsonUrl, false)

        //Pdf Matcher
        def pdfUrl
        def pdfUrlMatch = html =~ /(?ism)DocumentId":(\d+),[^}]*?Reports regarding scrutinized/
        if (pdfUrlMatch) {
            pdfUrl = "https://mylrc.sdlegislature.gov/api/Documents/Required%20Report/" + pdfUrlMatch[0][1].toString().trim() + ".pdf"
        }
        //for developers
        //runInDev(pdfUrl)


        //for production

        //Get text from download image
        def pdfText = pdfToTextConverterUsingOCR(pathToFile)
        handleDetailsPage(pdfText, pdfUrl)

    }

    def runInDev(def pdfUrl) {
        def file = new File("/home/sekh/Documents/229487.pdf")
        //def file = new File("/home/sekh/Documents/211582.pdf")
        def pages = pdfToTextConverterUsingOCR(file, true)
        handleDetailsPage(pages, pdfUrl)
    }

    def handleDetailsPage(def data, def pdfLink) {
        data = initialFix(data)
        def dataMatch = data =~ /(?s)(.*?)\s*(\d+\/\d+\/\d+)/
        def nameList = []
        def eventData = "This entity appears on the South Dakota Legislative Research Council list of \"scrutinized companies\". The reason for restriction is that they have been determined to have prohibited business operations in Sudan."

        //Date
        def eventDateList = []
        def eventDateMatcher = data =~ /(?ism)South Dakota Investment Council\s*-\s*(.*?)\n/
        while (eventDateMatcher.find()) {
            def eventDate = eventDateMatcher.group(1).toString().trim()
            eventDate = eventDate.replaceAll(/(?ism)(\d{2})\d{1}(\d{2})\d{1}(\d{4})/, { def a, b, c, d -> return b + "/" + c + "/" + d })
            eventDateList.addAll(eventDate)
        }

        while (dataMatch.find()) {
            nameList = filterName(dataMatch.group(1).toString().trim())
            nameList.each { name ->
                if (name) {
                    def attrMap = [:]
                    attrMap[FIELDS.EVNT_DETAIL] = eventData
                    attrMap[FIELDS.EVNT_DATE] = eventDateList
                    attrMap[FIELDS.URL] = pdfLink

                    def aliasMatch = name =~ /\((.*?)(?:\)|$)/
                    def alias = ""
                    if (aliasMatch) {
                        alias = aliasMatch[0][1].toString().trim()
                    }
                    name = name.replaceAll(/\((.*?)(?:\)|$)/, "").trim()
                    createOrgEntity(name, alias, attrMap)
                }
            }
        }
    }

//------------------------------Filter part------------------------//

    def filterName(name) {
        def nameRetList = []
        name = name.toString()
        name = name.replaceAll(/(?s)\bNo\b\s*\bholdings\b\s*/, "")
        name = name.replaceAll(/(?ism)South Dakota Investment Council.*/, "")
        name = name.replaceAll(/\s*\"/, "\n")
        if (name =~ /\n/) {
            nameRetList = name.toString().split(/\n/)
        } else {
            nameRetList.add(name)
        }
        return nameRetList
    }

    def initialFix(data) {
        data = data.toString()
        data = data.replaceAll(/12\/1512020/, "12/15/2020")
        data = data.replaceAll(/04\/2212021/, "04/22/2021")
        data = data.replaceAll(/0813112021/, "08/31/2021")
        data = data.replaceAll(/06115\/2021/, "06/15/2021")
        data = data.replaceAll(/09\/2112021/, "09/21/2021")
        data = data.replaceAll(/(?is)4009 West.*?(?=South Dakota Investment Council\s*-\s*\d+\/\d+\/\d{4})/, "")
        /*data = data.replaceAll(/(?ism)(SOUTH DAKOTA INVESTMENT COUNCIL|SOUTH\.        IN ST NT COUNCIL)\s*\n.*?Council conference call\..*?tvo/, "")
        data = data.replaceAll(/(?ism)\(additions denoted by .\)/, "")
        data = data.replaceAll(/(?ism),.*?Iran Scrutinized Companies List/, "")*/
        // data = data.replaceAll(/(?ism)Removed effective.*?none\s*none/, "")
        data = data.replaceAll(/(?ism)\u000C/, "")
        data = data.replaceAll(/\uFFFD/, "")
        data = data.replaceAll(/\[|\]/, "")
        data = data.replaceAll(/,\s{5,}/, "")
        data = data.replaceAll(/(?ism)Removed effective.*?none\s*none/, "")
        data = data.replaceAll(/(?i)\(additions denoted by [\^A\*]\)/, "")
        data = data.replaceAll(/(?ism)(?<=\n)\s*(.*?)\s{2,}(.*?)\n/, { def a, b, c -> return b + "\n" })
        //data = data.replaceAll(/Removed effective.*\d{1,2}\/\d{1,2}\/\d{4}\:/, "")

        return data
    }

//------------------------------Entity creation part---------------//

    def createOrgEntity(name, alias, attrMap) {
        name = sanitize(name)
        def entity = context.findEntity([name: name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType("O")
        }
        if (alias) {
            alias = alias.replaceAll(/(?i)\s*\bfka\b\s*/, "")
            entity.addAlias(sanitize(alias))
        }
        createEntityCommonCore(entity, attrMap)
    }

    def createEntityCommonCore(ScrapeEntity entity, attrMap) {
        entity.addUrl(attrMap[FIELDS.URL])

//        ScrapeAddress addr = new ScrapeAddress()
//        addr.setProvince("South Dakota")
//        addr.setCountry("UNITED STATES")
//        entity.addAddress(addr)

        attrMap[FIELDS.EVNT_DATE].each {
            ScrapeEvent event = new ScrapeEvent()
            event.category = "DEN"
            event.subcategory = "ASC"
            event.setDescription(attrMap[FIELDS.EVNT_DETAIL])
            event.setDate(it)
            entity.addEvent(event)
        }

    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = false, tidy = false, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def sanitize(data) {
        data = data.toString().replaceAll(/&amp;/, '&').replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
        data = data.toString().replaceAll(/(?<=Holdings Bhd).*Bhd$/, "").trim()
        return data.trim()

    }

    def pdfToTextConverterUsingOCR(def pdfUrl, def dev) {
        if (!(pdfUrl =~ /(?i)http/) && !dev) {
            pdfUrl = "http://" + pdfUrl
        }
        try {
            List<String> pages = ocrReader.getText(pdfUrl)
            return pages
        } catch (NoClassDefFoundError e) {
            def pdfFile = invokeBinary(pdfUrl)
            def pmap = [:] as Map
            pmap.put("1", "-layout")
            pmap.put("2", "-enc")
            pmap.put("3", "UTF-8")
            pmap.put("4", "-eol")
            pmap.put("5", "dos")
            //pmap.put("6", "-raw")
            def pdfText = context.transformPdfToText(pdfFile, pmap)
            return pdfText
        } catch (Exception e) {
            Thread.sleep(10000)
            return "-- RUNTIME ERROR --- $e.message ---"
        }

    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def download(def pdfURL, def path) {
        Files.deleteIfExists(Paths.get(path))
        InputStream inn = new URL(pdfURL).openStream()
        Files.copy(inn, Paths.get(path))
    }
}