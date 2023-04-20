package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_Cbp_Withhold_Release_Orders script = new Us_Cbp_Withhold_Release_Orders(context)
script.initParsing()


class Us_Cbp_Withhold_Release_Orders {

    final addressParser
    final entityType
    final def ocrReader
    ScrapianContext context

    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")

    def root = 'https://www.cbp.gov'
    def url = 'https://www.cbp.gov/trade/programs-administration/forced-labor/withhold-release-orders-and-findings?_ga=2.123978188.754345784.1623853046-1537900562.1623853046'

    Us_Cbp_Withhold_Release_Orders(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.updateCountries([US: ["NEW JERSEY", "GEORGIA", "New York", "WEST VIRGINIA", "Oklahoma"]])
        this.context = context
    }

    def initParsing() {
        def html = invokeUrl(url)
        getData(html)
    }

    def getData(def html) {
        def headerData, headerMatcher
        def country, countryMatcher

        html = html.toString().replaceAll(/(?is)(<div class="accordion-heading"><h2><a id="Withhold_Release_Orders".*?)(?=<div class="accordion-heading"><h2><a id="Findings").*/, '$1')
        def header = 1
        headerMatcher = html =~ /(?ism)<div class="panel panel-default">.*?<\/div><\/div><\/div>/
        while (headerMatcher.find()) {
            headerData = headerMatcher.group()

            countryMatcher = headerData =~ /(?ism)<h4 class="panel-title">.*?Click to Expand"><\/span><span>(.*?)<\/span><\/h4>/
            if (countryMatcher.find()) {
                country = sanitizeAddress(countryMatcher.group(1))
            }

            def rowMatcher
            rowMatcher = headerData =~ /(?ism)<td><p>(\d{1,2}\/\d{1,2}\/\d{4})<\/p><\/td><td><p>.*?<\/p><\/td><td><p>(.*?)<\/p><\/td>(?:<td><p>(?:Partially Active|Active|Inactive)\.?<\/p><\/td>.*?(?:<a href="(.*?)".*?<\/a>(?:&nbsp;|\. )\| )?<a href="(.*?)".*?<\/a>)?/

            def row = 1
            while (rowMatcher.find()) {
                def date, link, nameMatcher
                def entityNameList, hyperlinkList = []

                date = rowMatcher.group(1)
                entityNameList = handleEntitiesData(rowMatcher.group(2))
                link = sanitizeLink(rowMatcher.group(3))
                hyperlinkList.add(link)
                link = sanitizeLink(rowMatcher.group(4))
                hyperlinkList.add(link)

                def aliasMatcher
                entityNameList.each({ entityName ->
                    def alias
                    def aliasList = [] as List
                    if (!entityName.toString().isEmpty()) {
                        if (entityName =~ /f\/k\/a|d\/b\/a|a\/k\/a[\/]?|(?i)owned by|(?i)n\/k\/a[\/]?|dba|\/[A-Z][a-z]{2,}|alias|D\/B\/A|[\(\s]aka\s|DBA|\snka\s|f\/lc\/a/) {
                            aliasList = entityName.split(/f\/k\/a|d\/b\/a|a\/k\/a[\/]?|(?i)owned by|(?i)n\/k\/a[\/]?|\sdba\s|\/[A-Z][a-z]{2,}|alias|D\/B\/A|[\(\s]aka\s|DBA|\snka\s|f\/lc\/a/).collect({
                                it.toString().trim()
                            })
                            entityName = aliasList[0]
                            aliasList.remove(0)
                        } else if ((aliasMatcher = entityName =~ /([\"\(].*[\)\"])/)) {
                            alias = aliasMatcher[0][1]
                            aliasList.add(alias)
                            entityName = entityName.toString().replaceAll(/[\"\(]$alias[\"\)]/, "")
                        }

                        entityName = entityName.toString().replaceAll(/\s+/, " ").trim()
                        entityName = entityName.replaceAll(/(?i)^Dr\.?\s|(?:Jr\.?|Sr\.?)$/, "").trim()


                        createEntity(entityName, aliasList, date, country, hyperlinkList)
                    }
                })

                row++
            }

            header++
        }

    }

    def createEntity(def name, def aliasList, def eventDate, def address, def entityUrlList) {

        def entity = null

        if (!name.toString().equals("") || !name.toString().isEmpty()) {
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }

            if (aliasList) {
                aliasList = separateBracketAlias(aliasList)
                aliasList.each {
                    if (it) {

                        it.each {
                            if (it) {
                                entity.addAlias(it.toString().replaceAll(/(?s)\s+/, " ").trim())
                            }
                        }
                    }
                }
            }
            addCommonPartOfEntity(entity, eventDate, address, entityUrlList)
        }
    }

    def addCommonPartOfEntity(def entity, def eventDate, def address, def entityUrlList) {

        def description = 'This entity appears on the United States Customs and Border Protection published list of withhold release orders and findings published in the Federal Register.'
        //Add Address
        ScrapeAddress scrapeAddress = new ScrapeAddress()
        if (address.toString() != "null" || address != "") {

            def addrMap = addressParser.parseAddress([text: address, force_country: true])
            scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
            if (scrapeAddress) {
                entity.addAddress(scrapeAddress)
            }
        } else {
            scrapeAddress.setCountry("")
        }

        //Add Event
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(description)
        if (eventDate) {
            eventDate = context.parseDate(new StringSource(eventDate), ["MM/dd/yyyy"] as String[])
            event.setDate(eventDate)
        }

        //Add URL
        entity.addEvent(event)
        if (entityUrlList) {
            entityUrlList.each {
                if (it != "null") {
                    entity.addUrl(it)

                }
            }
        }
    }

    def handleEntitiesData(def entitiesData) {
        def nameMatcher = entitiesData =~ /(?s)<span style="font-size.*?style="color:#333333">(.*?)<\/span><\/span><\/span>/
        if (nameMatcher.find()) {
            entitiesData = sanitizeName(nameMatcher.group(1))
        }
        entitiesData = sanitizeName(entitiesData)

        def entityName
        def entityNameList = [] as List
        def entityNameMatcher

        if ((entityNameMatcher = entitiesData =~ /^(.*){0,1}$/)) {
            entityName = entityNameMatcher[0][1].toString().trim()

            if (entityName =~ /\s+and\s|[,;]|(?<=\sInc\.|\sINC\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))|(?<=LLC|Inc\.|Ltd\.)\s*vs.\s*(?=Abraham)/) {
                entityNameList = entityName.toString().split(/\s+and\s|(?<=\sInc\.|\sINC\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))|[,;]|(?<=LLC|Inc\.|Ltd\.)\s*vs.\s*(?=Abraham)/).collect({
                    it.trim()
                })
            } else if (entityName =~ /(?<=& Co\.)\s*(?=UBS)/) {
                entityNameList = entityName.toString().split(/(?<=& Co\.)\s*(?=UBS)/).collect({ it.trim() })
            } else {
                entityNameList.add(entityName)
            }
        } else if (entitiesData =~ /[,;]/) {
            entitiesData = entitiesData.toString().replaceAll(/\s+/, " ").trim()
            entityNameList = entitiesData.toString().split(/[,;]|(?<=LLC|Inc\.|\sINC\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z\']+|[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))|\s+and\s|(?<=Corp\.)\s(?=National)/).collect({
                it.trim()
            })
        } else {
            def nameList = [] as List
            entityNameMatcher = entitiesData =~ /(.*)/
            while (entityNameMatcher.find()) {
                entityName = entityNameMatcher.group(1).trim()
                if ((entityName =~ /\sand\s|(?<=\sInc\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))/)) {
                    nameList = entityName.toString().split(/\s+and\s+|(?<=\sInc\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))/).collect({
                        it.trim()
                    })
                    nameList.each { entityNameList.add(it.toString().trim()) }
                } else {
                    entityNameList.add(entityName)
                }
            }
        }
        return entityNameList
    }

    def detectEntity(def name) {
        def type
        if (name =~ /^(?:Bruno|Budwal|Calardo|Carapella|Christakos|Fusco|Jean|Kerr|Weiss|Michael|Miller|Moro|Torrillo|Torres|Geake|Lambert|Locy|Pedersen|Quinzi|Whopper|Wilhelm)$/) {
            type = "P"
        } else if (name =~ /^(?:Schifano|Caruso|Daigneau|Hornberger|Johnson|Juarez|Milano|Ohman|Halsne|Parmigiani|Peter|Shea|Stapleton|Stephens|Yasnis|Smutko|Fifer|Steven|Scott)$/) {
            type = "P"
        } else if (name =~ /^(?:Ned|John|Joe|Jimmy|Gullmetti|Buckman|Firm|Le|Dadco|Abernathy)$/) {
            type = "P"
        } else if (name =~ /^\S+$|Hsin Kang Asbestos Mine|Kumar Carpet Pvt\.|Marange Diamond Fields|Miao Chi Tea Farm|Nanhu Tree Farm|Qinghe Farm|Red Star Tea Farm|Fishing Vessel: Da Wang|Fishing Vessels/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$|^Citigroup|Prison$|Plantation|Works$|Carpet$|Guo Ji$|Guangming$|Labor)/) {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:Arms$)/) {
                type = "P"
            }
        }
        return type
    }

    def separateBracketAlias(def aliasList) {

        def newAliasList = []

        aliasList.each {

            if (it.toString().contains("(")) {
                def bracketHandler = it =~ /(?ism)\((.*?)\)/
                if (bracketHandler.find()) {
                    def bracketAlias = bracketHandler.group(0)

                    bracketAlias = bracketAlias.toString().replaceAll(/\(/, '')
                    bracketAlias = bracketAlias.toString().replaceAll(/\)/, '')
                    newAliasList.add(bracketAlias)

                    it = it.toString().replaceAll(/\($bracketAlias\)/, '').trim()

                    newAliasList.add(it)
                }
            } else {
                newAliasList.add(it)
            }
        }
        return [newAliasList]
    }

    def sanitizeAlias(def alias) {
        alias = alias.toString().replaceAll(/\(/, "")
        alias = alias.toString().replaceAll(/\)/, "")
        alias = alias.toString().replaceAll(/(?s)\s+/, " ").trim()
        return alias
    }

    def sanitizeLink(def link) {
        if (link != null) {
            link = link.toString().trim()
            link = root + link
        }
        return link.toString().trim()
    }

    def sanitizeAddress(def address) {
        return address.toString().trim()
    }

    def sanitizeName(def name) {
        name = name.toString().trim()
        name = name.toString().replaceAll(/&nbsp;/, " ").trim()
        name = name.toString().replaceAll(/&amp;/, "&").trim()

        name = name.toString().replaceAll(/; Kathmandu\./, "").trim()
        name = name.toString().replaceAll(/\(of Nagoya, Japan\)/, "").trim()
        name = name.toString().replaceAll(/\(parent company\)/, "").trim()
        name = name.toString().replaceAll(/\(amends D\.O\. #1a(?:,| and) #2 (?:above|below)\)/, "").trim()
        name = name.toString().replaceAll(/\(expands D\.O\. #1a (?:above|below)\)/, "").trim()
        name = name.toString().replaceAll(/Tobacco produced in Malawi and products.*/, "").trim()
        name = name.toString().replaceAll(/All Turkmenistan Cotton or products produced.*/, "").trim()

        name = name.replaceAll(/,\s*(?=L\.L\.P|L\.L\.C\.?|Inc\.?|LLC|LC|LLLP|PC|PLLC|LP|APC|P\.A\.|JR\.?|SR\.?|ESQ\.|LLP|III|II|and)/, " ")
        name = name.replaceAll(/,\s*(?=P\.C\.?|L\.P\.?|LTD\.|Ltd\.|N\.A\.|Jr\.?|Sr\.?|INC\.|LIMITED|L P|Corp\.|L L C\.|L\.P\.|Esq\.)/, " ")
        name = name.replaceAll(/,\s*(?=d\/b\/a|dba|n\/k\/a|nka|a\/k\/a|aka|f\/k\/a|fka)/, " ")


        name = name.replaceAll(/and its subsidiaries and joint ventures/, "").trim()
        name = name.replaceAll(/and its subordinates/, "").trim()
        name = name.replaceAll(/and its Subsidiaries/, "").trim()
        name = name.replaceAll(/and their subsidiaries/, "").trim()
        name = name.replaceAll(/and Subsidiaries/, "").trim()

        name = name.replaceAll(/Xinsheng \(New Life\)/, 'Xinsheng New Life').trim()
        name = name.replaceAll(/(Xingsheng) \(or Xinsheng\) (.*?)(?=(?:,|;|$))/, '$1 $2 a/k/a Xinsheng').trim()
        name = name.replaceAll(/(Sichuan) \(Szechuan\) (.*?)(?=(?:,|;|$))/, '$1 $2 a/k/a Szechuan').trim()
        name = name.replaceAll(/(Wulin) \(or Wuling\) (.*?)(?=(?:,|;|$))/, '$1 $2 a/k/a Wuling').trim()

        name = name.replaceAll(/(?<=Production|Cotton|Trade|Fruits|Agricultural|Manufacturing|Coal|Leather|Bedding|Fur)\sand\s(?=Cloth|Garment|Wool|Iron|Construction|Linen|Business|Vegetable|Trade|Trading)/, " & ")
        name = name.replaceAll(/(?<=Forging|Malawi)\sand\s(?=Pressing|products)/, " & ")

        name = name.toString().replaceAll(/Brightway Holdings Sdn.*?Brightway Group\)/, "Brightway Group dba Brightway Holdings Sdn Bhd dba Laglove Sdn Bhd dba Biopro Sdn Bhd").trim()

        name = name.replaceAll(/d\/b\/a|dba|n\/k\/a|nka|a\/k\/a|aka|f\/k\/a|fka/, "alias")

        name = name.replaceAll(/(?s),\s+,/, ",").trim()
        name = name.replaceAll(/(?s)\s+/, " ").trim()
        return name.toString().trim()
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        return street
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

}