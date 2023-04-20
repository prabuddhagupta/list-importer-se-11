package current_scripts

import com.rdc.importer.scrapian.ScrapianContext

// Client site
import com.rdc.rdcmodel.model.RelationshipType

// Developer site
//import com.rdc.importer.misc.RelationshipType


import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 500000, socketTimeout: 500000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

Us_Hhs_Ocr_Data_Breaches script = new Us_Hhs_Ocr_Data_Breaches(context)
script.initParsing()


int i = 1

def nameIdMap = [:]
for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + i++)
    nameIdMap[entity.getName()] = entity
}

for (entity in context.session.getEntitiesNonCache()) {
    for (association in entity.getAssociations()) {

        otherEntity = nameIdMap[association]
        if (otherEntity && !isAlreadyAssociated(entity, otherEntity)) {
            scrapeEntityAssociation = new ScrapeEntityAssociation(otherEntity.getId())
            scrapeEntityAssociation.setRelationshipType(RelationshipType.ASSOCIATE)
            scrapeEntityAssociation.setHashable(otherEntity.getName(), otherEntity.getType())
            entity.addScrapeEntityAssociation(scrapeEntityAssociation)
        }
    }
    entity.getAssociations().clear()
}

def isAlreadyAssociated(entity, otherEntity) {
    Set associations = otherEntity.getScrapeEntityAssociations()
    boolean isAssos = false
    if (associations) {
        associations.each { item ->
            if (((ScrapeEntityAssociation) item).getId().equals(entity.getId())) {
                isAssos = true
            }
        }
    }
    return isAssos
}

class Us_Hhs_Ocr_Data_Breaches {
    final addressParser
    final entityType
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final ScrapianContext context

    def url = 'https://ocrportal.hhs.gov/ocr/breach/breach_report.jsf'


    Us_Hhs_Ocr_Data_Breaches(context) {
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser.updateCities([US: ["SPRINGFIELD"]])

        this.context = context
    }

    def initParsing() {
        def html = invokeUrl(url)
        handleRow(html)

        def headers = getHeader()

        def totalRowMatcher = html =~ /Displaying \d+ - \d+ of (\d+)\)/
        def totalRow
        if (totalRowMatcher.find()) {
            totalRow = totalRowMatcher.group(1)
            totalRow = Integer.parseInt(totalRow)
        }

        def nextHtml
        int rowsNextPage = 100
        while (rowsNextPage <= totalRow) {
            String nextPage = String.valueOf(rowsNextPage)
            def params = getParams(nextPage)
            params["javax.faces.ViewState"] = inputValueParser(html, "ViewState")
            nextHtml = invokePostMethod(url, params, false, headers, false)
            handleRow(nextHtml)
            rowsNextPage += 100

        }
    }


    def handleRow(def html) {
        def rowMatcher = html =~ /(<tr data-ri="\d+".*?<\/tr>)/
        def row
        while (rowMatcher.find()) {
            row = rowMatcher.group(1)
            row = sanitizeRow(row)
            getHandleData(row)
        }
    }


    def getHandleData(def row) {

        def dataMatcher = row =~ /white-space:pre-line;">\s*([A-Za-z0-9\.,;\(\)&\/'`’\-\\"\+ ]+)\s*<\/span><.*?role="gridcell">(.*?)<\/td>.*?role="gridcell">\s*(\d{2,10})\s*<.*?"gridcell">\s*(\d{1,2}\/\d{1,2}\/\d{2,4})\s*<.*?line;">\s*([A-Za-z\/ ]+)\s*</

        def entityName
        def nameList = []
        def address
        def remark
        def event
        def eventDate
        def aliasList = []

        if (dataMatcher.find()) {
            entityName = dataMatcher.group(1)
            (nameList, aliasList) = handleAlias(entityName)
            address = ", " + dataMatcher.group(2).trim() + ", USA"
            remark = dataMatcher.group(3)
            eventDate = dataMatcher.group(4)
            event = dataMatcher.group(5)
        }

        nameList.each { name ->
            createEntity(name, aliasList, address, eventDate, remark, event)
        }
    }

    def handleAlias(def name) {

        name = name.toString().replaceAll(/&amp;/, '&')
        def entityName
        def aliasList = []
        def aliasMatcher
        def alias
        if (name =~ /(?i)[\(\s](?:dba|d\/b\/a\/?|d\.b\.a\.?|-ALIAS-|doing business as)\s/) {
            aliasList = name.split(/(?i)[\s\(]dba\s|\sd\/b\/a\/?\s|\s-ALIAS-\s|\sd\.b\.a\.?\s|\sdoing business as\s/).collect({
                it.toString().trim()
            })
            entityName = aliasList[0]
            aliasList.remove(0)
        } else if (name =~ /\(.*\)/) {
            aliasMatcher = name =~ /(\(.*\))/
            if (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                entityName = name.replaceAll(/\($alias\)/, "")
                aliasList.add(alias)
            }
        } else {
            entityName = name

        }

        def entityNameList = sanitizeName(entityName).split(/ENTITY/).collect({ it.trim() })
        return [entityNameList, aliasList]
    }


    def createEntity(def name, def aliasList, def address, def eventDate, def remark, def eventDes) {

        def entity

        name = sanitizeEntityName(name).trim()
        if (!name.toString().equals("") && !name.toString().isEmpty() && !name.toString().equals("null")) {
            name = name.toString().trim().replaceAll(/,$|[\)\(]]/, '')

            def type = detectEntity(name)
            entity = context.findEntity(["name": name, "type": type])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(type)
            }

            def aliasEntityType
            aliasList.each { it ->
                it = sanitizeAlias(it)
                if (it) {
                    aliasEntityType = detectEntity(it)
                    if (aliasEntityType.toString().equals(type)) {
                        entity.addAlias(it)
                    } else {
                        if (aliasEntityType.equals("P")) {
                            entity.addAssociation(it)
                        } else {
                            entity.addAssociation(it)
                            //create new entity with association
                            def newEntity = context.findEntity(["name": it, "type": aliasEntityType])
                            if (!newEntity) {
                                newEntity = context.getSession().newEntity()
                                newEntity.setName(it)
                                newEntity.setType(aliasEntityType)
                            }
                            newEntity.addAssociation(name)
                            addCommonPartOfEntity(newEntity, address, eventDes, eventDate, remark)
                        }
                    }
                }
            }
            addCommonPartOfEntity(entity, address, eventDes, eventDate, remark)
        }
    }

    def addCommonPartOfEntity(def entity, def address, def eventDes, def eventDate, def remark) {

        def description = "This entity appears on the US Department of Health and Human Services Office for Civil Rights published list of entities currently under investigation for data breaches of unsecured protected health information."

        if (address) {
            if (!address.equals("")) {
                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress)
                }
            }
        }
        if (!remark.toString().isEmpty()) {
            entity.addRemark("Individuals Affected: " + remark)
        }
        //Add Event
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(description + " Type of Breach: " + eventDes)

        //Event Date
        if (!eventDate.toString().isEmpty()) {
            eventDate = context.parseDate(new StringSource(eventDate))
            event.setDate(eventDate)
        }
        entity.addEvent(event)

    }

    def detectEntity(def name) {
        def type
        if (name =~ /(?i)(?:(?:Cancer|Epic|Managed|Total|Senior) Care|(?:Harris|Mono|Chautauqua) County|Epilepsy Florida|Radiology|Florida Blue|Guidehouse|MultiPlan|LogicGate|and Klein|Monitoring|Imaging|Orthopaedics, PA|Dialysis|Metro Washington|Regence)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:HomeMD|Cedar County Iowa|Multnomah County|Sound Generations|ortof MD|for Hope|St\. Margaret|Surgical Oncology|Selected Benefits|(?:Starling|Care) Physicians|Orthopaedic Surgeons|TX ACE|SummaCare|Talbert House|Peninsula Regional|UNC Hospitals|UPMC)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:Renewed Mind|Washington County School District|Upstate HomeCare|Planned Parenthood Los Angeles|TriValley Primary Care|(?:Christi|Aetna) ACE|(?:Eastwood|Care|Norwood) Clinics?|Andor Labs|of Ramsey|BioTel Heart|of California|CVS (?:Caremark|Pharmacy)|Aerospace|for Living|and Gynecology|Care PACE|Blue Cross)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:(Vannucci|BAILEY) DDS|Mayo Clinic|Neurology, PC|Medicine|EYE PC|Hospitals|ODPC|DDS,? PC|Wisconsin|Centers, PC|Fertility|kaiser permanente|KR Perio|Diagnostics|Western Michigan|Michigan ACE|Orthopedics)$/) {
            type = "O"
        } else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$|(?:Elwyn|Electromed|SalusCare)$)/) {
                    type = "O"
                }
            }
        }
        return type
    }

    def sanitizeName(def name) {
        name = name.toString().trim()
        name = name.replaceAll(/(?<=PLLC|Services) and (?=Hofmann|Illinois)|(?<=Inc\.|Atlanta) \/ (?=University|Piedmont)/, " ENTITY ")
        name = name.replaceAll(/(?<=Park Authority) - (?=Jekyll)|(?<=Inc\.)\s*(?=Retiree Health|Health Plan)/, " ENTITY ")
        return name.trim()
    }

    def sanitizeEntityName(def entityName) {
        entityName = entityName.toString().trim().replaceAll(/(?:Board of Supervisors|MD, PC|,\s*MD)$/, "")
        entityName = entityName.toString().trim().replaceAll(/(?i)null/, "")
        entityName = entityName.toString().trim().replaceAll(/(?i)^(?:Mr\.?|Dr\.?)\s/, "")
        entityName = entityName.toString().trim().replaceAll(/(?i)(?:ODPC|\sPhd Pc|, (?:DMD|DC))$/, "")

        return entityName.trim()

    }

    def sanitizeAlias(def alias) {
        alias = alias.toString().trim().replaceAll(/[\)\("]/, "")
        alias = alias.toString().trim().replaceAll(/^'|'$/, "")
        return alias.trim()
    }

    def sanitizeRow(def row) {
        row = row.toString().trim()
        row = row.replaceAll(/\\93|\\94|\\96|[“”–]/, '')
        row = row.replaceAll(/(?<=Island Fire)\/(?=EMS)|(?<=KR Perio), (?=Katherine)/, ' -ALIAS- ')
        row = row.replaceAll(/\sa subsidiary of (?=Professional)|\sits affiliate (?=My Egg)|\sfor and on behalf of its (?=School of Medicine)/, ' -ALIAS- ')

        return row
    }

    def getHeader() {
        def headers = [
            "Accept"          : "application/json, text/javascript, */*; q=0.01",
            "Accept-Language" : "en-US,en;q=0.9",
            "Connection"      : "keep-alive",
            "Content-Type"    : "application/x-www-form-urlencoded; charset=UTF-8",
            "Host"            : "ocrportal.hhs.gov",
            "Origin"          : "https://ocrportal.hhs.gov",
            "Referer"         : "https://ocrportal.hhs.gov/ocr/breach/breach_report.jsf",
            "Sec-Fetch-Dest"  : "empty",
            "Sec-Fetch-Mode"  : "cors",
            "Sec-Fetch-Site"  : "same-origin",
            "X-MicrosoftAjax" : "Delta=true",
            "X-Requested-With": "XMLHttpRequest",
        ]
        return headers
    }

    def inputValueParser(def html, def key) {
        def match = html =~ /${key}[^\n]+?value=['"]([^'"]+)/
        if (match) {
            return match[0][1]
        } else {
            return ""
        }
    }

    def getParams(String nextToken) {
        def param = [:]

        param["javax.faces.partial.ajax"] = "1"
        param["javax.faces.source"] = "ocrForm:reportResultTable"
        param["javax.faces.partial.execute"] = "ocrForm:reportResultTable"
        param["javax.faces.partial.render"] = "ocrForm:reportResultTable"
        param["javax.faces.behavior.event"] = "page"
        param["javax.faces.partial.event"] = "page"
        param["ocrForm:reportResultTable_pagination"] = "1"
        param["ocrForm:reportResultTable_first"] = nextToken
        param["ocrForm:reportResultTable_rows"] = "100"
        param["ocrForm:reportResultTable_skipChildren"] = "1"
        param["ocrForm:reportResultTable_encodeFeature"] = "1"
        param["ocrForm"] = "ocrForm"
        param["ocrForm:reportResultTable_rppDD"] = "100"


        return param
    }

    def sanitizeAddress(def address) {
        address = address.toString().replaceAll(/INCARCERATED\s*\/\s*(?:US MARSHAL CUSTODY|PRISON|CUSTODY)/, "").trim()
        address = address.toString().replaceAll(/,\s*,/, ",").trim()
        address = address.toString().replaceAll(/^(.*)$/, '$1, USA').trim()
        return address
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        return street
    }


    def invokePostMethod(def url, def paramsMap, def cache = false, def headersMap = [:], def tidy = true) {
        return context.invoke([url: url, tidy: tidy, type: "POST", params: paramsMap, headers: headersMap, cache: cache]);
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}