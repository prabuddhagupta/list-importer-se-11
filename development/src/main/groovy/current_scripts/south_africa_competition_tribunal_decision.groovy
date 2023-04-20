
package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent

/**
 * Date: 07/02/18
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;
SA_Competition script = new SA_Competition(context)
script.init()

//-----Debug area starts------------//
//script.handleDetailsPage("http://www.comptrib.co.za/cases/complaint/retrieve_case/1722")
//nameList = script.parsingNameFromPdf("http://www.comptrib.co.za/assets/Uploads/SCI-EXECUTIVE-SUMMARY-FINAL-05-June-2014-011502.pdf")
//-----Debug area ends---------------//

class SA_Competition {
    final ScrapianContext context
    final String root = "http://www.comptrib.co.za"
    final String url1 = root + "/cases-archived"
    final String url2 = root + "/cases-current"
    final String splitTag = "~SPLIT~"
    final String aliasTag = "~ALIAS~"
    def urlList = []

    SA_Competition(context) {
        this.context = context
    }

    private enum FIELDS
    {
        EVNT_DATE, EVNT_DETAILS, URL
    }

//------------------------------Initial part----------------------//

    def init() {
        initParsing(url1)
        initParsing(url2)
    }

    def initParsing(indexUrl) {
        def html = invoke(indexUrl)
        handleDetailsPage(indexUrl)

        def nextPageUrl;
        def nextPageUrlMatch = html =~ /(?is)<a href="([^"]+)"\s+title="go to page/
        while (nextPageUrlMatch.find()) {
            nextPageUrl = root + "/" + nextPageUrlMatch.group(1).replaceAll("%2F", "/")
            handleDetailsPage(nextPageUrl)
        }
    }


    def handleDetailsPage(def indexUrl) {

        def html = invoke(indexUrl)

        def val = /<td[^>]+>(.*?)<\/td>/
        def rowMatch = html =~ /(?s)<tr>\s*<td(.*?)<\/tr>/


        while (rowMatch.find()) {
            def attrMap = [:]
            def names, des, url

            def type
            def row = rowMatch.group(1)
            def typeMatch = row =~ /(?ism)>\s*(Complaint Referral from Commission|Complaint Referral from Complainant|Consent Order|Settlement Agreement)\s*<\/td>/
            if(typeMatch.find()){
                type = typeMatch.group(1).toString()

                println type
                def dataMatch = rowMatch.group(1) =~ /(?is)<td><a href="([^"]+)">.*?<\/td>\s*<td>(.*?)<\/td>\s*<td>\s*<div[^>]+>([^<]+)<\/div>\s*<\/td>\s*<td(?:[^>]+)?>(.*?)<\/td>/
                if (dataMatch.find()) {
                    url = root + dataMatch[0][1].trim()
                    def urlSource = context.invoke([url: url, tidy: true, cache: true])
                    def respondentMatch
                    def nameMatch
                    if (urlSource =~ /(?i)case parties/){
                        nameMatch = urlSource =~ /(?is)case\s*parties(.*?)case\s*type/
                        names = nameMatch[0][1]
                        names = sanitizeName(names)
                    }else {
                        nameMatch = urlSource =~ /(?is)<h1>(.*?)<\/h1>/
                        names = nameMatch[0][1]
                    }
                    if (urlSource =~ /(?is)Applicant.*Respondent|complainant.*Respondent/) {
                        respondentMatch = urlSource =~ /(?is)respondent<\/span>:(.*?)<\/td/
                        names = respondentMatch[0][1]
                        names = sanitizeName(names)
                    }

                    des = dataMatch[0][3].replaceAll(/(?i)_(?=penalty)/, " ").trim()
                    attrMap[FIELDS.EVNT_DATE] = dataMatch[0][4].trim()

                }
                if (!(urlList.contains(url))) {
                    urlList.add(url)
                    attrMap[FIELDS.URL] = [url]

                    def eventDes = "This entity appears on the South Africa Competition Tribunal ";
                    if (indexUrl =~ /(?i)complaints/) {
                        eventDes = eventDes + "Complaints list. Status: " + des

                    } else {
                        eventDes = eventDes + "list of consent orders. Status: " + des
                    }
                    attrMap[FIELDS.EVNT_DETAILS] = eventDes

                    names = sanitize(names)
                    names.split(/(?is)(?<!of)\s*<br>|;|(?<=\(pty\) ltd),|<br\s*\/>\s*|<br>\s*/).each { def name ->
                        /*if (name =~ /(?i)first/){
                            println("D")
                        }*/

                        if (!(name =~ /(?i)Competition Commission/)) {
                            name = aliasFixingWithTag(name)
                            name = camelCaseConverter(name)
                            name = finalNameFilter(name)
                            def aliasList = name.split(/(?i)$aliasTag/).collect({ it ->
                                return it.trim()
                            })
                            name = aliasList[0]
                            aliasList.remove(0)
                            createEntity(name, attrMap, aliasList)
                        }
                    }
                }
            }
        }
    }

    def finalNameFilter(def name) {
        name = name.replaceAll(/\r\n/, "\n")
        name = name.replaceAll(/(?i),\s*Petzetakis/, "")
        //from format initial name
        name = name.replaceAll(/(?im)a division of\s*/, "")
        name = name.replaceAll(/(?is)(?:All the members of)?/, "")
        name = name.replaceAll(/(?is)(?:Duly)?\s*Represented By.*/, "")
        name = name.replaceAll(/(?is)Appointed\s*Dealers\s*Of/, "")
        name = name.replaceAll(/(?is)\((?:In Liquidation|Sole Proprietorship)\)/, "")
        name = name.replaceAll(/(?is)\(Pty\)Ltd/, "(Pty) Ltd")
        //name = name.replaceAll(/Sasol L Chemic Cal Indus Stries Lim Mited/, "Sasol Chemical Industries Limited")
        name = name.replaceAll(/(?i)indus\s+Stries/, "Industries")
        name = name.replaceAll(/(?i)Lim\s+Mited/, "Limited")
        name = name.replaceAll(/(?i)Chemic\s+L\s+Cal/, "Chemical")
        name = name.replaceAll(/Attorneys s/, "Attorneys")
        name = name.replaceAll(/-On-/, "- On -")
        name = name.replaceAll(/(?i)\bsa\b/, "South Africa")
        name = name.replaceAll(/(?im)^\s*The Competition Commission\s*$/, "The Competition Commission Of South Africa")
        name = name.replaceAll(/(?i)\bNo\b\S*$/, "")
        name = name.replaceAll(/(?i)\(known as/, "known as")
        name = name.replaceAll(/(?i)\(REPRESENTED BY THE PUBLIC/, "REPRESENTED BY THE PUBLIC")
        name = name.replaceAll(/(?ism)Ltd\)\s*$/,"Ltd")
        name = name.replaceAll(/(?ism)Ltd\.\s*$/,"Ltd")
        name = name.replaceAll(/(?ism)\(\s*\)/,"")
        return name

    }

//------------------------------Filter part------------------------//
    def formatInitialNamesStr(nameStr) {
        nameStr = nameStr.replaceAll(/(?im)(,|\band\b|&)\s*(?:\d+)?\s*(?:other|another|members)s?\s*$/, "")
        nameStr = nameStr.replaceAll(/(?im)<br>/, "")

        return sanitize(nameStr)
    }

    def aliasFixingWithTag(nameStr) {
        def aliasTokens = /(?i)\b(?:t\/a|and aka|aka|also|Trading As|known as|Formerly|(?<=commission|Association)\s*and|(?<=limited|ltd),|(?<=limited|ltd)\s*and|(?<=limited|ltd)\s*(?=\w+|\(\w+)|previously\s+(?:named?)?)\b/
        nameStr = nameStr.replaceAll(/(?i)$aliasTokens/, aliasTag)

        return nameStr
    }

    def parseNSplitUniqueNames(nameStr) {
        //format name
        nameStr = formatInitialNamesStr(nameStr)
        //split name
        nameStr = nameStr.replaceAll(/(?im)(\S{2,}\s+\S+\s*)(?:,)(?=\s*\w+[\s\-]+[\S\s]+)/, { a, b -> b + splitTag })
        //	nameStr = nameStr.replaceAll(/(?im)(\S{2,}\s+\S+\s*)(?:\band\b)(?=\s*\w+[\s\-]+(?!$aliasTag))/, { a, b -> b + splitTag })
//        nameStr = nameStr.replaceAll(/(?i)(?<=Limited)\s+&\s+(?=community)/, splitTag)
//        nameStr = nameStr.replaceAll(/(?i)(?<=\b(?:limited|ltd|cc|Attorneys|no)\b)\s+(?:\band\b)/, splitTag)

        return new HashSet<String>(Arrays.asList(nameStr.split(splitTag)))
    }

//------------------------------Entity creation part---------------//
    def createEntity(name, attrMap, aliasList = []) {
        def entity;
        if (name) {
            def entityType = detectEntityType(name)
            if (entityType == "O") {
                entity = createOrgEntity(name, aliasList)

            } else {
                entity = createPersonEntity(name, aliasList)
            }
            createEntityCommonCore(entity, attrMap)
        }
    }

    def createOrgEntity(name, aliasList = []) {
        def entity = null
        def dupEntity = null
        def scrubbedName = name.replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll(/(?i)and|&/, " ")
                .replaceAll(/(?i)(?<=commission)\s+(?=south africa)/, " OF ")
                .replaceAll(/(?im)(?<=wire)\s+product\b\s+/, " Products ")
                .replaceAll("\\s+", " ").toUpperCase().trim()

        //dup almost same type of entity & add it in alias field ie:
        HashMap<String, List<ScrapeEntity>> entitiesMap = context.getSession().getEntitiesMap();
        List<ScrapeEntity> scrapeEntityList = entitiesMap.get(scrubbedName);
        if (scrapeEntityList != null) {
            for (ScrapeEntity scrapeEntity : scrapeEntityList) {
                if (scrapeEntity != null) {
                    dupEntity = scrapeEntity
                }
            }
        }

        entity = context.findEntity(["name": name, "type": "O"])
        if (!entity && !dupEntity) {
            entity = context.getSession().newEntity()
            name = sanitizeName(name)
            entity.setName(name)
            entity.type = "O"
        }

        if (dupEntity) {
            entity = dupEntity
            entity.addAlias(name)
        }

        aliasList.each { alias ->
            if (alias) {
                alias = camelCaseConverter(alias)
                alias = sanitizeAlias(alias)
                entity.addAlias(alias)
            }
        }

        return entity
    }

    def createPersonEntity(name, aliasList = []) {

        def entity = context.findEntity(["name": name, "type": "P"])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.name = name
            entity.type = "P"
            aliasList.each { alias ->
                alias = camelCaseConverter(alias)
                entity.addAlias(alias)
            }
        }
        return entity
    }

    def camelCaseConverter(name) {
        //only for person type //\w{2,}: II,III,IV etc ignored
        name = name.replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, { a, b, c, d ->
            return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
        })

        return name
    }

    def createEntityCommonCore(ScrapeEntity entity, attrMap) {
        if (attrMap[FIELDS.EVNT_DETAILS]) {
            def event = new ScrapeEvent()
            event.setDescription(attrMap[FIELDS.EVNT_DETAILS])
            event.category = "REG"
            event.subcategory = "ACT"
            def date = context.parseDate(new StringSource(attrMap[FIELDS.EVNT_DATE]), ["yyyy-MM-dd"] as String[])
            if (date) {
                event.setDate(date)
            }
            entity.addEvent(event)
        }

        def address = new ScrapeAddress()
        address.country = "SOUTH AFRICA"
        entity.addAddress(address)

        attrMap[FIELDS.URL].each { def url ->
            entity.addUrl(url)
        }
    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = true, tidy = true, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = true, clean = false, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def invokePost(url, paramsMap, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        try {
            Map dataMap = [url: url, type: "POST", params: paramsMap, tidy: tidy, headers: headersMap, cache: cache]
            dataMap.putAll(miscData)
            return context.invoke(dataMap)
        } catch (Exception e) {
            println(e.getMessage())
        }
    }

    def pdfToTextConverter(pdfUrl) {
        def pmap = [:] as Map
        pmap.put("1", "-enc")
        pmap.put("2", "UTF-8")
        pmap.put("3", "-eol")
        pmap.put("4", "dos")
        pmap.put("5", "-layout")
        def pdfFile = invokeBinary(pdfUrl)
        def pdfText = context.transformPdfToText(pdfFile, pmap).toString()
        return pdfText
    }

    def detectEntityType(name) {
        def entityType = context.determineEntityType(name)
        if (entityType.equals("P")) {
            if (name =~ /(?i)\b(?:Society|cc|Energie|Towers|System|Life|Enterprise|Shop|Support|Court|Private|Investments?|Treasury|city|Fortune|Fund|Towers|Commission|Media|Pioneer|Momentum|Beperk|Health|pty|Investment|Consortium|usa|Printing|council|Energy|Plastics|Quarries|and|Appliances|Dis-Chem Pharmacies|Green Hygiene|Mica Barberton|Pele Kaofela|Petzetakis|Premier Fmcg|Timrite|Tshwane Fire Sprinklers)\b/) {
                entityType = "O"

            } else if (name =~ /(?i)\b(?:Medical|Safcol|Bank|Government|Pipes?|Chemicals?|Plant|Bay|Star|School|Newco|Doors|Portfolio|Shopping|Partnership|Brand|Mall|Finance|The|Bar|Liberty|Gas|Germany|Distributors?|Americana|Iron|Fishing|Reinforcing|Nippon|Martinair|ag)\b/) {
                entityType = "O"

            } else if (name =~ /(?i)\b(?:Hardware|Developments|Stores?|Pcc|Motors|Food|Data|Dynamic|Inputs|Bakeries|Farms?|Park|Pharmacy|Holding|Optimum|Marketing|Investec|Man Se|Schulz|Oosthuizen|Mweb|Nestle|New Holdco|Mua|Senwes|Meletse|Merafe|Jaguar|Kwambonambi|Lakeview|Crushers|Lonehill|Maersk|Lekoa|Producer|Forbes|Cargo|Cellular|Venture|south|dealer|cycl[ei]|Attorneys|Construction)/) {
                entityType = "O"

            } else if (name =~ /(?i)\b\w\.\w\b\.?/) {
                entityType = "O"
            }
        }

        return entityType
    }

    def sanitize(data) {
        return data.toString().replaceAll(/&amp;/, '&').replaceAll(/(?i)\(Formely Tornotype Ltd\)/,"Formely Tornotype Ltd").replaceAll(/(?i)\bGEPF\b/,"GOVERNMENT EMPLOYMENT PENSION FUND").replaceAll(/(?i)Transnet Soc Limited/, "Transnet Soc").replaceAll(/(?i)Government Employees Pension Fund \(Represented By/, "Government Employees Pension Fund ").replaceAll(/(?i)TQ and A Properties \(Pty\) Ltd, \(Third\)/, "TQ and A Properties (Pty) Ltd")
                .replaceAll(/(?is)\d+\s*others|\(Previously|\(Trading as|\(\d+\\/\d+\\/\d+\)|\(In Business Rescue And 6 Other Target Firms\)|\(Rf\)|\(Formerly Tornotype Ltd\)|\(represented.*limited\)|\(represented.*development\)/, "")
                .replaceAll(/(?is)\(In Provisional Liquidation\)|\(Logistics\)|\(Maseve\)|\(Nu-Pro Feeds\)|, duly represented by/, "").replaceAll("\\u2019", "'").replaceAll("\\u00eb", "e")
                .replaceAll(/(?ism)Now Known as/,"known as")
                .replaceAll(/(?ism)\(now/,"known as")
                .replaceAll(/(?s)\s+/, " ").trim()
    }

    def sanitizeAlias(alias) {
        return alias.toString().replaceAll(/,$/, "").replaceAll(/\($/, "").trim()
    }

    def sanitizeName(name) {
        return name.toString().replaceAll(/(?is)<span[^:]+:/, "").replaceAll(/(?is):<\/b><\/td>\s*<td>/,"").replaceAll(/(?is):<br>\s*/,"").replaceAll(/(?is)<\/td>\s*<\/tr>\s*<\/table>\s*<b>/,"").replaceAll(/(?i)\(first\)|\(second\)|\(third\)|\(fourth\)|\(fifth\)|\(sixth\)|\(seventh\)|\(eighth\)|\(ninth\)|\(tenth\)|\(eleventh\)|\(twelfth\)|\(thirteenth\)|\(fourteenth\)|\(fifteenth\)|\(sixteenth\)|\(seventeenth\)|\(eighteenth\)|\(nineteenth\)|\(twentieth\)|\(twenty-first\)|\(twenty-second\)|\(twenty-third\)/,"").replaceAll(/,$/, "").replaceAll(/\($/, "").trim()
    }
}

