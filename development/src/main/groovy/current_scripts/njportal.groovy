package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.opencsv.CSVWriter


context.setup([connectionTimeout: 30000, socketTimeout: 55000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/77.0.3865.90 Chrome/77.0.3865.90 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Njportal script = new Njportal(context)
script.initParsing()

class Njportal {
    final entityType
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context
    final def mainUrl = "https://www.njportal.com/DOR/BusinessNameSearch/Search/BusinessName"

    String filePath = "/home/maenul/Documents/njportal.csv"

    File file = new File(filePath);
    FileWriter outputfile = new FileWriter(file)
    int n = 1
    int i = 1

    Njportal(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context
    }

    def initParsing() {
        def html, end
        html = invoke(mainUrl)
        html = getParam(html)
    }


    def getParam(def html) {
        def headers = [:]
        def reqData
        def reqDataMatcher = html =~ /__RequestVerificationToken.+?value="(.+?)"/
        if (reqDataMatcher.find()) {
            reqData = reqDataMatcher.group(1)
        }
        for (def i = "a"; i <= "z"; ++i) {
            def params = getParamsForNext(reqData, i + "%")
            html = invokePost(mainUrl, params, false, headers, false)
            getDataFromTable(html)

        }
        return html
    }

    def getDataFromTable(def html) {
        def rowMatcher = html =~ /(?ism)<tr>.+?<\/tr>/
        def rowData
        while (rowMatcher.find()) {
            rowData = rowMatcher.group(0)
            def name, entityId, city, type, date
            def nameMatcher = rowData =~ /(?ism)<tr>.+?<td>(.+?)<\/td>/
            if (nameMatcher.find()) {
                name = nameSanitize(nameMatcher.group(1))
            }

            def idMatcher = rowData =~ /(?ism)<tr>.+?<td>(\d{8,})<\/td>/
            if (idMatcher.find()) {
                entityId = idMatcher.group(1)
            }

            def cityTypeMatcher = rowData =~ /(?ism)<td>.+?hidden-phone">(.*?)<\\/td>.+?Profit Corporation">(.+?)<\\/abbr>.+?hidden-phone">(\d{1,}\\/\d{1,}\\/\d{2,})</
            if (cityTypeMatcher.find()) {
                city = cityTypeMatcher.group(1)
                type = cityTypeMatcher.group(2)
                date = cityTypeMatcher.group(3)
            }
            writeToCSV(name, entityId, city, type, date)
        }
    }

    def writeToCSV(String... args) {
        try {
            String[] header;
            if (i == 1) {
                header = ["Name", "entityId", "city", "type", "date"]
                i++
            }
            CSVWriter writer = new CSVWriter(outputfile);
            writer.writeNext(header)
            // adding header to csv
            writer.writeNext(args);
            writer.flush()
            //writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    def nameSanitize(def name) {
        name = name.toString().replaceAll(/&amp;/, "&")
        name = name.toString().replaceAll(/&#39;/, "'").trim()
        return name
    }


    def createEntity(def entityName) {

        def entity = null
        entity = context.findEntity([name: entityName])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(entityName)
            entity.setType("O")
        }
    }

    def invokePost(def url, def paramsMap, def cache = false, def headersMap = [:], def tidy = true) {
        return context.invoke([url: url, tidy: tidy, type: "POST", params: paramsMap, headers: headersMap, cache: cache]);
    }


    def getParamsForNext(def reqData, def num) {
        def param = [:]
        param["__RequestVerificationToken"] = reqData
        param["BusinessName"] = num

        param["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        param["Accept-Encoding"] = "gzip, deflate, br"
        param["Accept-Language"] = "en-US,en;q=0.9"
        param["Cache-Control"] = "max-age=0"
        param["Connection"] = "keep-alive"
        param["Content-Length"] = "180"
        param["Content-Type"] = "application/x-www-form-urlencoded"
        param["Host"] = "www.njportal.com"
        param["Origin"] = "https://www.njportal.com"
        param["Referer"] = "https://www.njportal.com/DOR/BusinessNameSearch/Search/BusinessName"

        param["sec-ch-ua-mobile"] = "?0"
        param["sec-ch-ua-platform"] = "Linux"
        param["Sec-Fetch-Dest"] = "document"
        param["Sec-Fetch-Mode"] = "navigate"
        param["Sec-Fetch-Site"] = "same-origin"
        param["Sec-Fetch-User"] = "?1"
        param["Upgrade-Insecure-Requests"] = "1"
        param["User-Agent"] = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36"

        return param
    }


    def invoke(url, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}




