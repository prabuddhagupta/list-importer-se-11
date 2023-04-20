package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeDob
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent

/**
 * Date: 02 April 2019
 * */


context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_ak_dps_active_warrants script = new Us_ak_dps_active_warrants(context)
script.initParsing()

//-----Debug area starts------------//
//script.handleDetailsPage("...")
//-----Debug area ends---------------//

class Us_ak_dps_active_warrants {
    final ScrapianContext context
    final String url = "http://dps.alaska.gov/getmedia/178d6f58-89d4-491b-92ea-e6d0403d86ec/ASTActiveWarrantsToPublish;.aspx"

    Us_ak_dps_active_warrants(context) {
        this.context = context
    }

    private enum FIELDS
    {
        EVNT_DETAILS, DOB
    }
//------------------------------Initial part----------------------//
    def initParsing() {
        def html = pdfToTextConverter(url)

        html = fixInitialData(html)

        def rowMatch = html =~ /(?ms)(^[A-Z]+[^\d\n]+\b\d{2}\b[^\n]+\n.*?)(?=^[A-Z]+[^\d\n]+\b\d{2}\b|\Z)/
        def row
        while (rowMatch.find()) {
            row = rowMatch.group(1).replaceAll(/(?ms)^\s{50,}(\d+[A-Z]*\d*|\bR\b)/, "")
            if (row =~ /\w+/) {
                handleDetailsPage(row)
            }
        }

    }

    def handleDetailsPage(row) {

        def multiLine = row =~ /[^\n]+/
        def line
        int counter = 1
        def lastName = ""
        def firstName = ""
        def middleName = ""
        def finalName
        def age
        def des = ""
        def attrMap = [:]
        while (multiLine.find()) {
            line = multiLine.group(0)
            if (counter == 1) {
                def lastNameMatch = line =~ /(?m)(^[A-Z\-\']+)/
                if (lastNameMatch.find()) {

                    lastName += lastNameMatch[0][1]
                }

                def firstNameMatch = line =~ /^[A-Z\-\']+\s*([A-Z\-]+)/
                if (firstNameMatch.find()) {

                    firstName += firstNameMatch[0][1]
                }

                def middleNameMatch = line =~ /^[A-Z\-\']+\s*[A-Z\-]+(.*?)(?=\b(?:M|F|U)\b\s*\d{2})/
                if (middleNameMatch.find()) {

                    middleName += middleNameMatch[0][1].toString().trim()
                }

                def ageMatch = line =~ /(?sm)(?<=\b(?:M|F|U)\b)\s*(\d{2})/
                if (ageMatch.find()) {
                    age = ageMatch.group(1).trim()
                }
                def desMatch = line =~ /(?sm)\d+\.?\s*(.*?)(?=FELONY|MISDEMEANOR)/
                while (desMatch.find()) {
                    des += desMatch.group(1).replaceAll(/\d+\.?\d+/, "").replaceAll(/(?s)\s+/, " ")
                    attrMap[FIELDS.EVNT_DETAILS] = des
                }

            } else {
                def lastNameMatch = line =~ /(?m)(^[A-Z\-]+)/

                if (lastNameMatch.find()) {
                    if(lastName =~/\-/ ){
                        lastName += lastNameMatch[0][1]
                    }
                    else if (lastNameMatch[0][1].toString().length()<=2){
                        lastName+=lastNameMatch[0][1]
                    }
                    else
                        lastName=lastName+" "+lastNameMatch[0][1]
                }

                def firstNameMatch = line =~ /(?is)^\s{10,15}([A-Z]+)/
                if (firstNameMatch.find() ) {
                    if(firstNameMatch[0][1].toString().length()<=2){
                        firstName += firstNameMatch[0][1]
                    }
                    else
                        firstName = firstName+" "+firstNameMatch[0][1]
                }

                def middleNameMatch= line =~ /(?s)^\s{20,28}([A-Z]+)/
                if (middleNameMatch.find()) {
                    if(middleNameMatch[0][1].toString().length()<=3) {
                        middleName += middleNameMatch[0][1].toString().trim()
                    }
                    else
                        middleName= middleName+" "+middleNameMatch[0][1].toString()

                    line = line.replaceAll(/(?i)\bR\b/, "")
                }

                else

                    middleNameMatch=line=~/(?i)(?:(?:^[A-Z]+\s*)|(?:^\s*[A-Z]+))\s{2,}(\b[A-Z]+(?<!VIOLATION|M|F)\b)/
                if (middleNameMatch.find()) {
                    if(middleNameMatch[0][1].toString().length()<=2) {
                        middleName += middleNameMatch[0][1].toString().trim()
                    }
                    else
                        middleName= middleName+" "+middleNameMatch[0][1].toString()

                    line = line.replaceAll(/(?i)\bR\b/, "")
                }



                def desMatch = line =~ /(?s)^\s{40,70}([A-Z\-]+)/

                if (desMatch.find()) {
                    des += desMatch.group(1).trim()
                    attrMap[FIELDS.EVNT_DETAILS] = des
                }
            }
            counter++

        }
        finalName = firstName + " " + middleName + " " + lastName

        if (age) {
            age = getDobFromAge(age)
            attrMap[FIELDS.DOB] = age
        }
        attrMap[FIELDS.EVNT_DETAILS] = sanitizeEvent(attrMap[FIELDS.EVNT_DETAILS])
        createPersonEntity(finalName, attrMap)

    }

    def createPersonEntity(name, attrMap = [:]) {
        if ((name)) {
            def key = [name, attrMap[FIELDS.DOB]]
            def entity = context.findEntity(key)

            if (!entity) {
                entity = context.newEntity(key)

                name = camelCaseConverter(name)
                entity.setName(name)
                entity.type = "P"
            }
            createEntityCommonCore(entity, attrMap)
        }
    }

    def camelCaseConverter(name) {
        //only for person type //\w{2,}: II,III,IV etc ignored
        name = name.replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, { a, b, c, d ->
            return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
        })

        return name
    }

    def createEntityCommonCore(ScrapeEntity entity, attrMap) {
        def eventDetails = "This entity appears on the Alaska Department of Public Safety Active Warrants list for"
        def description
        entity.addDateOfBirth(new ScrapeDob(attrMap[FIELDS.DOB]))

        ScrapeAddress addr = new ScrapeAddress()
        addr.setProvince("ALASKA")
        addr.setCountry("UNITED STATES")
        entity.addAddress(addr)

        ScrapeEvent event = new ScrapeEvent()
        event.category = "FUG"
        event.subcategory = "WTD"
        if (!attrMap[FIELDS.EVNT_DETAILS].toString().isEmpty()) {
            description = eventDetails + " " + attrMap[FIELDS.EVNT_DETAILS] + "."
            event.setDescription(description)
        } else {
            description = eventDetails + "."
            event.setDescription(description)
        }
        entity.addEvent(event)

    }
//------------------------------Filter part------------------------//

    def fixInitialData(html) {
        html = html.replaceAll(/(?s)\bAST\b.*?\bDetachment\b/, "")
        html = html.replaceAll(/\n\d+\/\d+\/\d+.*?\n/, "")
        html = html.replaceAll(/(?s)Last\s*name.*?Amount/, "")
        html = html.replaceAll(/\bANN\b\s*\bJ\b\s*\bN\b/, "")
        html = html.replaceAll("\\u000c", "")
        html = html.replaceAll(/(?i)detachment[^:]+:\s*\d+/, "")
        html = html.replaceAll(/(?ism)^\s+$/, "")

        return html
    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = true, tidy = false, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = false, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def getDobFromAge(def age) {
        def year, dobYear
        Calendar cal = Calendar.getInstance()
        year = cal.get(Calendar.YEAR)
        dobYear = year - new Integer(age).intValue()
        dobYear = dobYear.toString()
        dobYear = "-/-/" + dobYear
        return dobYear

    }

    def pdfToTextConverter(pdfUrl) {
        def pdfFile = invokeBinary(pdfUrl, null, null, true)

        def pmap = [:] as Map
        pmap.put("1", "-layout")
        pmap.put("2", "-enc")
        pmap.put("3", "UTF-8")
        pmap.put("4", "-eol")
        pmap.put("5", "dos")
        //pmap.put("6", "-raw")

        def pdfText = context.transformPdfToText(pdfFile, pmap).toString().replaceAll(/\r\n/, "\n")
        return pdfText
    }

    def sanitize(data) {
        return data.replaceAll(/&amp;/, '&').replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
    }

    def sanitizeEvent(data) {
        return data.toString().trim()
    }
}