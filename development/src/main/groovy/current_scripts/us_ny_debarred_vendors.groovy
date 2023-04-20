package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification
import org.apache.commons.lang.StringUtils

/**
 * Date: 07/09/17
 * */

context.setup([connectionTimeout: 50000, socketTimeout: 50000, retryCount: 3, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true

Ny_Debarred script = new Ny_Debarred(context)
script.initParsing1()
script.initParsing2()


class Ny_Debarred {
    final ScrapianContext context
    final addressParser
    final factoryRevision = "d897871d8906be0173bebbbf155199ff441dd8d3"
    final moduleFactory = ModuleLoader.getFactory(factoryRevision)
    final String root1 = "http://www.nycsca.org/Business/GettingStarted/Pages/DisqualifiedFirms.aspx"
    final String root2 = "https://omig.ny.gov"
    final String url2 = "https://apps.omig.ny.gov/exclusions/ex_formatted_list.aspx"
    // final String url2 = root2 + "/fraud/medicaid-exclusions"

    Ny_Debarred(context) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.updateCities(["US": ["St. Albans"]])
    }

    private enum FIELDS
    {
        EVNT_DATE, EVNT_DES1, EVNT_DES2, ASSOCIATION, ADDR, ID1, ID2, URLTYPE, URL
    }

//------------------------------Initial part----------------------//
    def init() {
        initParsing1()
        initParsing2()
    }

    def initParsing1() {
        def html = invoke(root1)
        def uMatch = html =~ /(?is)"([^"]+)"\s*target="_blank"><[^>]+>Disqualified/
        if (uMatch.find()) {

            def dUrl = uMatch.group(1)
            TraverseNextPage(dUrl)
        }
    }

    def initParsing2() {
        def html = null
        try {
            html = invoke(url2)
        } catch (Exception e) {

        }

        if (html) {

            def row = ""
            def rowMatch = html =~ /(?is)<tr>\s*(<td>.*?)<\/tr>/
            while (rowMatch.find()) {
                def attrMap = [:]
                def aliasList = [];
                def name, id, date;
                row = rowMatch.group(1)
                def attrValMatch = row =~ /(?is)<td>([^<]*)<\/td>\s*<td>([^<]*)<\/td>\s*<td>([^<]*)<\/td>\s*<td>([^<]*)<\/td>\s*<td>([^<]*)<\/td>\s*/
                if (attrValMatch) {
                    name = attrValMatch[0][1].replaceAll(/(?i)\b(?:d\s*p\s*m|md\.?|dds\s*(?:pc))/, "").replaceAll(/(?i)\ba\\/ka\\//, "A/KA").trim()
                    if (!(attrValMatch[0][2] =~ /(?i)none|unknown|n\/a/)) {
                        attrMap[FIELDS.ID1] = attrValMatch[0][2].toString().trim()
                    }
                    if (!(attrValMatch[0][3] =~ /(?i)none|unknown|n\/a/)) {
                        attrMap[FIELDS.ID2] = attrValMatch[0][3].toString().trim()
                    }

                    attrMap[FIELDS.EVNT_DATE] = attrValMatch[0][5]
                    attrMap[FIELDS.EVNT_DES2] = "This entity appears on the New York Medicaid Fraud Exclusions Debarred List."
                    aliasList = name.split(/(?i)\b(?:A\\/?K\\/?A\\/D\\/?b\\/?a|A\\/?K\\/?A\\/?|\\/?D\\/?b\\/?a)\b/).collect({ it ->
                        return formatIndividualName(it)
                    })
                    name = aliasList[0]
                    aliasList.remove(0)

                    attrMap[FIELDS.URL] = url2

                    createEntity(name, attrMap, aliasList)

                }

            }
        }
    }

    def getParams(def html, def isFirstPage, def eventTarget) {
        def paramMap = [:]

        paramMap["__EVENTTARGET"] = eventTarget
        paramMap["__EVENTARGUMENT"] = getIdVal(html, "__EVENTARGUMENT")
        paramMap["__VIEWSTATE"] = getIdVal(html, "__VIEWSTATE")
        paramMap["__VIEWSTATEGENERATOR"] = getIdVal(html, "__VIEWSTATEGENERATOR")
        paramMap["__EVENTVALIDATION"] = getIdVal(html, "__EVENTVALIDATION")
        paramMap["ctl00\$menu\$mainMenu_ClientState"] = "false"
        paramMap["ctl00\$menu\$regMenu_ClientState"] = ""
        paramMap["ctl00\$menu\$subcontractorMenu_ClientState"] = ""
        paramMap["ctl00\$content\$firmName"] = ""
        paramMap["ctl00\$content\$city"] = ""
        paramMap["ctl00\$content\$state"] = ""
        paramMap["ctl00\$content\$MWLBECertified"] = ""
        paramMap["ctl00\$content\$Over1TradeCode"] = ""
        paramMap["ctl00\$content\$ddWicks"] = ""
        paramMap["ctl00\$content\$Zip"] = ""
        paramMap["ctl00\$content\$hiddenCategoryList"] = ""
        paramMap["ctl00\$content\$hiddenCategory"] = ""
        paramMap["ctl00\$content\$rblTradeCodeOperator"] = "OR"

        if (isFirstPage) {
            paramMap["ctl00\$content\$Search"] = "Search"
        }

        return paramMap;
    }

    def getIdVal(def html, def id) {
        def match = html =~ /(?i)$id"\s+value="([^"]*)"/
        if (match.find()) {
            return match[0][1]
        }

    }

    def handleDetailsPage(def html) {
        html = html.toString().replaceAll(/\r\n/, "\n")
        def match = html =~ /(?ism)<td>([^<\n]+)<\/td><td>(.*?)<\/td>\n<\/tr>/
        while (match.find()) {
            def name, addr, date, association, eDes;
            def aliasList = []
            def attrMap = [:]
            name = match.group(1)

            def otherValMatch = match.group(2) =~ /(?ism)address">(.*?)<\/span>.*?contactName">(.*?)<\/span>.*?startdate(.*?)<\/span>.*?status">(.*?)<\/span>/
            if (otherValMatch) {
                addr = otherValMatch[0][1].replaceAll(/\|/, ",").replaceAll(/<[^>]+>/, ",")
                addr = addr.concat(", USA")
                attrMap[FIELDS.ADDR] = addr
                attrMap[FIELDS.ASSOCIATION] = otherValMatch[0][2].trim()
                attrMap[FIELDS.EVNT_DATE] = dateFixer(otherValMatch[0][3])
                attrMap[FIELDS.EVNT_DES1] = otherValMatch[0][4]

            }


            aliasList = name.split(/\//).collect({ it ->
                return formatIndividualName(it)
            })
            name = aliasList[0]
            aliasList.remove(0)
            attrMap[FIELDS.URLTYPE] = root1
            createEntity(name, attrMap, aliasList)

        }
    }

    def TraverseNextPage(srcUrl) {

        def pageSection = ""
        def html = invoke(srcUrl)

        def paramVal = getParams(html, true, "")
        def reqUrl = "https://dobusiness.nycsca.org/Supplier/SearchDSISupplier.aspx?q=zM+D+gVXLgNIlhmAIp9RzEBThtNPMIdrVzPjO1Iw5IN/B0Ov2E0psGTY5D9VBv0eR8r05FKD7Hk1422VV+pgeA=="
        html = invokePost(reqUrl, paramVal)
        handleDetailsPage(html)
        def pageSectionMatch = html =~ /(?ism)datagridpage">\s*<td(.*?)<\/td>/
        if (pageSectionMatch) {
            pageSection = pageSectionMatch[0][1].replaceAll(/(?ism)<td.*?<span>\d+<\/span>/, "")
            //pageSection = pageSection.replaceAll(/(?i)&nbsp;<a href="javascript:__/, "")
            //pageSection = pageSection.replaceAll(/(?i)<span>\d+<\/span>/,"nonlink")
        }
        def pageLink;
        def pageLinkMatch = pageSection =~ /(?ism)dopostback\(([^\)]+)\)/
        while ((html =~ /(?ism)\.\.\.<\/a>/) && !(pageSection.equals(""))) {
            int i = 0
            while (pageLinkMatch.find()) {
                pageLink = pageLinkMatch.group(1).replaceAll(/(?ism)&#\d+;,*/, "")
                paramVal = getParams(html, false, pageLink)
                try {
                    html = invokePost(reqUrl, paramVal)
                    handleDetailsPage(html)
                } catch (Exception e) {

                }
                i++
            }

            pageSectionMatch = html =~ /(?ism)datagridpage">\s*<td(.*?)<\/td>/
            if (pageSectionMatch) {
                pageSection = pageSectionMatch[0][1].replaceAll(/(?ism).*?<span>\d+<\/span>/, "")
                //pageSection = pageSection.replaceAll(/(?i)&nbsp;<a href="javascript:__/, "")
                //pageSection = pageSection.replaceAll(/(?i)<span>\d+<\/span>/,"nonlink")
            }
            pageLinkMatch = pageSection =~ /(?ism)dopostback\(([^\)]+)\)/

        }
        handleDetailsPage(html)
    }

    def getAttributeVal(def text, def string) {
        def match = text =~ /(?ism)$string">(.*?)<\/span>/
        if (match.find()) {
            return match[0][1]

        }
    }

//------------------------------Filter part------------------------//
    def dateFixer(def dateStr) {
        def date, month, year;
        def dateMatch = dateStr =~ /(\d+)\/(\d+)\/(\d+)/
        if (dateMatch) {
            month = dateMatch[0][1]
            date = dateMatch[0][2]
            if (dateMatch[0][1] =~ /(?m)^\d$/) {
                month = '0' + dateMatch[0][1]
            }
            if (dateMatch[0][2] =~ /(?m)^\d$/) {
                date = '0' + dateMatch[0][2]
            }
            year = dateMatch[0][3]

            return month + '/' + date + '/' + year
        }


    }

    def formatIndividualName(name) {

        //Adding extra dot after few org keywords
        name = name.replaceAll(/(?i)\b(?>corp|inc|ltd)$/, { a ->
            return a.toString().trim() + "."
        })
        name = name
        //--- example regex start
        //remove last ending braces
            .replaceAll(/^([^\(]+)\)\s*$/, '$1')
        //--- example regex ends
            .replaceAll(/[\s,;]*$/, '')
            .replaceAll(/\s{2,}/, ' ')
        return name.trim()
    }

//------------------------------Entity creation part---------------//
    def createEntity(name, attrMap, aliasList = []) {
        if (name) {
            def entity;

            if (attrMap[FIELDS.URLTYPE].equals(root1)) {
                entity = createOrgEntity(name, aliasList)
            } else {
                def entityType = detectEntityType(name)

                if (entityType == "O") {
                    entity = createOrgEntity(name, aliasList)

                } else {
                    entity = createPersonEntity(name, aliasList)

                }
            }
            createEntityCommonCore(entity, attrMap)
        }
    }


    def createOrgEntity(name, aliasList = []) {
        def entity = null
        entity = context.findEntity(["name": name, "type": "O"])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(sanitize(name))
            entity.type = "O"
        }
        aliasList.each { alias ->
            alias = sanitizeAlias(alias)
            entity.addAlias(alias)
        }

        return entity
    }

    def createPersonEntity(name, aliasList = []) {
        def entity = null

        if (StringUtils.isNotBlank(name)) {
            name = personNameReformat(sanitize(name))
            name = camelCaseConverter(name)
            entity = context.findEntity(["name": name, "type": "P"])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.name = name
                entity.type = "P"
                aliasList.each { alias ->
                    entity.addAlias(personNameReformat(alias))
                }
            }
        }
        return entity
    }

    def personNameReformat(name) {
        //delete extraneous letter
        name = name.replaceAll(/(?i)\b(?:,\s*d\.d\.s\.,\s+p\.c\.|pc\s+dds|rn|rp[at]|LPN\/RN|TBI)\b/, "")
        name = name.replaceAll(/(?i)[\s,]+\b(?:p\.?[tc]|md|d\.?d\.?s\.?|p\.?h\.?d|lpn|d[or])\b\.?$/, "")
        name = name.replaceAll(/(?i)\b(?:physician|Dental|sp|p\.?h\.?d\.?)\b/, "")
        name = name.replaceAll(/,/, "")

        def exToken = "(?:[js]r|I{2,3})"
        def replaceVar = ""
        //remove jr/sr by replace var
        name = name.replaceAll(/(?i)(.*?)\s+(${exToken}\.?)\s*$/, { a, b, c -> replaceVar = c; return b })

        name = name.replaceAll(/(?im)^(\w+)\s+(\w+)\s*$/, '$2 $1')
        //for two words name
        name = name.replaceAll(/(?ims)(.*?)(\S+\s+\S+)\s*$/, '$2 $1')
        //more than two words name

        //concat again with jr,sr,etc
        name = name.concat(" " + replaceVar)
        name = name.toString().replaceAll(/\s*-\s*$/, '')
        name = name.toString().replaceAll(/\s+/, ' ').trim()

        return name
    }

    def camelCaseConverter(name) {
        //only for person type //\w{2,}: II,III,IV etc ignored
        name = name.toString().replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, { a, b, c, d ->
            return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
        })
        return name.trim()
    }


    def createEntityCommonCore(ScrapeEntity entity, attrMap) {
        def eventDes;

        if (attrMap[FIELDS.EVNT_DES1]) {
            eventDes = "This entity appears on the New York List of Debarred Vendors. Status: " + attrMap[FIELDS.EVNT_DES1]
        } else {
            eventDes = attrMap[FIELDS.EVNT_DES2]
        }

        def event = new ScrapeEvent()
        event.setDescription(eventDes)
        if (attrMap[FIELDS.EVNT_DATE]) {
            event.setDate(attrMap[FIELDS.EVNT_DATE])
        }
        event.setCategory("REG")
        event.setSubcategory("ACT")
        if (event) {
            entity.addEvent(event)
        }

        if (attrMap[FIELDS.ASSOCIATION]) {
            entity.addAssociation(attrMap[FIELDS.ASSOCIATION])
        }

        if (attrMap[FIELDS.ADDR]) {
            def addrMap = addressParser.parseAddress([text: attrMap[FIELDS.ADDR]])

            ScrapeAddress addressObj = addressParser.buildAddress(addrMap)
            if (addressObj) {
                entity.addAddress(addressObj)
            } else {
                //Address is not parse-able; either add then as raw addr or reformat the input addr string
                println("===>" + attrMap[FIELDS.ADDR])
            }
        } else {
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            scrapeAddress.setProvince("New York")
            scrapeAddress.setCountry("UNITED STATES")
            entity.addAddress(scrapeAddress)
        }

        if (attrMap[FIELDS.URL]) {
            entity.addUrl(attrMap[FIELDS.URL])
        }

        // if (attrMap[FIELDS.ID1]) {
        if ((StringUtils.isNotBlank(attrMap[FIELDS.ID1])) || (StringUtils.isNotEmpty(attrMap[FIELDS.ID1]))) {
            ScrapeIdentification id = new ScrapeIdentification();
            id.setType("License Number")
            id.setValue(attrMap[FIELDS.ID1])
            entity.addIdentification(id)
        }

//        if (attrMap[FIELDS.ID2]) {
        if ((StringUtils.isNotBlank(attrMap[FIELDS.ID2])) || (StringUtils.isNotEmpty(attrMap[FIELDS.ID2]))) {
            ScrapeIdentification id = new ScrapeIdentification();
            id.setType("NPI Number")
            id.setValue(attrMap[FIELDS.ID2])
            entity.addIdentification(id)
        }
    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = false, tidy = false, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def invokePost(url, paramsMap, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, type: "POST", params: paramsMap, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def detectEntityType(name) {
        def entityType = context.determineEntityType(name)

        if (entityType.equals("P")) {
            if (name =~ /(?i)\b(?:and|of|new|Comprehensive|new\s*york|medical|aid|consultant|health|Designs?|institute|therapy|Family|Shoes?|Medicine|PHARMACY|Life|Home)\b/) {
                entityType = "O"

            } else if (name =~ /(?i)(?:care|state|Avenue|Hudson)\b/) {
                entityType = "O"

            } else if (name =~ /(?i)^\b(?:\w+)\b$/) {
                entityType = "O"

            } else if (name =~ /(?i)(?:optic|Orthopedic|&)/) {
                entityType = "O"

            }
        }

        return entityType
    }

    def sanitize(data) {
        return data.toString().replaceAll(/\s*\-$/, '').replaceAll(/&amp;/, '&').trim()
    }

    def sanitizeAlias(alias) {
        alias = alias.toString().replaceAll(/\s*\-$/, '')
        alias = alias.replaceAll(/(?s)\s+/, " ").trim()
        return alias.trim()
    }
}