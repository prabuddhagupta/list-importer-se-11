package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils

//import com.rdc.importer.misc.RelationshipType

context.setup([connectionTimeout: 500000, socketTimeout: 500000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

Us_Ca_Doj_Ag_Data_Security_Breaches script = new Us_Ca_Doj_Ag_Data_Security_Breaches(context)
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

class Us_Ca_Doj_Ag_Data_Security_Breaches {
    final addressParser
    final entityType
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final ScrapianContext context

    def url = 'https://oag.ca.gov/privacy/databreach/list'

    Us_Ca_Doj_Ag_Data_Security_Breaches(context) {
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context
    }

    def initParsing() {
        def html = invokeUrl(url)

        def tableMatcher = html =~ /(?s)<tbody>.*?<\/tbody>/
        def table
        if (tableMatcher.find()) {
            table = tableMatcher.group(0)
            handleRow(table)
        }
    }

    def handleRow(def table) {
        def rowMatcher = table =~ /(?s)<tr class=".*?">.*?<\\/tr>/
        def row
        while (rowMatcher.find()) {
            row = rowMatcher.group(0)
            getHandleData(row)
        }
    }

    def getHandleData(def row) {
        row = row.toString().replaceAll(/.*(?:sb24-183108|sb24-194945).*/, '')
        row = sanitizeHtmlText(row)

        def tempUrl = ""
        def entityUrlList
        def name
        def aliasList = []

        def entityName = ""
        def entityNameList = []
        def entityNameAndUrlMatcher = row =~ /href="(https:\/\/.*?)">(.*?)<\/a>/

        if (entityNameAndUrlMatcher.find()) {
            tempUrl = entityNameAndUrlMatcher.group(1)
            name = entityNameAndUrlMatcher.group(2).toString().trim()
            if (name =~ /,/) {
                entityNameList = name.split(/,/).collect({ it.toString().trim() })
            } else {
                entityNameList.add(name)
            }
        }

        def breachDateList = []
        def breachDateMatcher = row =~ /content="\d{4}-\d{1,2}-\d{1,2}T[^>]+>\s*(\d{1,2}\/\d{1,2}\/\d{2,4})\s*<\/span>/
        while (breachDateMatcher.find()) {
            breachDateList.add(breachDateMatcher.group(1))
        }

        def eventDate
        def eventDateMatcher = row =~ /field-created active"\s*>\s*(\d{1,2}\/\d{1,2}\/\d{2,4})\s*</
        if (eventDateMatcher.find()) {
            eventDate = eventDateMatcher.group(1)
        }

        entityUrlList = captureEntityUrl(tempUrl)

        entityNameList.each { it ->
            (entityName, aliasList) = handleAlias(it)

            if (!entityName.toString().equals("")) {
                entityName = sanitizeName(entityName).trim()
                createEntity(entityName, aliasList, breachDateList, eventDate, entityUrlList)
            }
        }
    }

    def captureEntityUrl(String tempUrl) {

        def entityList = []

        if (StringUtils.isNotBlank(tempUrl)) {
            def html = invokeUrl(tempUrl)

            html = html.toString().replaceAll(/(?s).*(?=<div class="content clearfix">)/, '')
            html = html.toString().replaceAll(/(?s)(<div class="content clearfix">.*?<\/div>.*?)(?=<\/section>).*/, '$1')

            def entityUrlMatcher = html =~ /(?i)<a href="(.*?pdf)"\s*type="application\/pdf/

            while (entityUrlMatcher.find()) {
                entityList.add(entityUrlMatcher.group(1).trim())
            }
        }
        return entityList
    }

    def handleAlias(def name) {

        name = name.toString().replaceAll(/&amp;/, '&')
        def entityName
        def aliasList = []
        def aliasMatcher
        def alias
        if (name =~ /(?i)[\s,](?:dba|d\/b\/a\/?|d\.b\.a\.?|\/dba\/|doing business as|its affiliates|[\/] also known as|and its subsidiaries|obo)\s|\sformerly knwon as\s/) {
            aliasList = name.split(/(?i)[,\s]dba\s|\sobo\s|\sd\/b\/a\/?\s|\sd\.b\.a\.?\s|\sdoing business as\s|\sits affiliates\s|\/dba\/|[\/] also known as|\sand its subsidiaries\s|\sformerly knwon as\s/).collect({
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
        return [entityName, aliasList]
    }

    def createEntity(def name, def aliasList, def breachDateList, eventDate, def entityUrlList) {
        def entity

        if ((!name.toString().isEmpty()) || (!name.toString().equals("")) || (!name.toString().equals("null"))) {
            name = name.toString().replaceAll(/,$|[\)\(]/, '').trim()

            def type = detectEntity(name)
            entity = context.findEntity(["name": name, "type": type])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(type)
            }
            def aliasEntityType
            aliasList.each { it ->
                if (!it.toString().equals("") || !it.toString().isEmpty() || !it.toString().equals("null")) {
                    it = sanitizeAlias(it).trim()
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
                                addCommonPartOfEntity(newEntity, eventDate, breachDateList, entityUrlList)
                            }
                        }
                    }
                }
            }
            addCommonPartOfEntity(entity, eventDate, breachDateList, entityUrlList)
        }
    }

    def addCommonPartOfEntity(def entity, def eventDate, def breachDateList, def entityUrlList) {

        def description = "This entity appears on the California Department of Justice Attorney General published list of entities in the data security breaches database."

        ScrapeAddress scrapeAddress = new ScrapeAddress()
        scrapeAddress.setCountry("UNITED STATES")
        scrapeAddress.setProvince("CALIFORNIA")
        entity.addAddress(scrapeAddress)

        breachDateList.each { breachDate ->
            if (!breachDate.toString().isEmpty() || !breachDate.toString().equals("")) {
                entity.addRemark("Date of Breach: " + breachDate)
            }
        }
        //Add Event
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(description)

        //Event Date
        if (!eventDate.toString().isEmpty()) {
            eventDate = context.parseDate(new StringSource(eventDate))
            event.setDate(eventDate)
        }
        entity.addEvent(event)
        entityUrlList.each { entityUrl ->
            entity.addUrl(entityUrl)
        }
    }

    def detectEntity(def name) {
        def type
        if (name =~ /(?i)(?:Publications|District|Signs|Gaming|Benefits|Abel HR|(?:Taylor|Ally|Comerica|Compass|West|Mechanics) Bank|Blue Cross|School|Hotels|Genetics|BioTel Heart|California|Cosmetology|Tags|Imaging|Senior Living|YOGA|Gear|Games)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:Casper Golf|Senior Care|Brothers|Aerospace|Academies|of Law|Blueshield|Kitchen|Fundraising|Club|Credco|Ditrict|Schools|Vineyards|(?:Epic|Dependable) Foods|Barbecue Pit|Charlotte|San (?:Diego|Joaquin)|ECS Tuning|FSB)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:NAC|Pharmacy|County|Circuits|Tools|Forever|Brands|Chapel|Camps|Wild|Footwear|Grower|Breast Cancer|Philharmonic|Prep|CPA[s]?|(?:Mechanics|Synchrony) Bank|and Kiener|(Hoag|Endocrine) Clinic)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:College|and Winery|Humboldt|Los Angeles|Sacramento|INFINITI|China Bistro|'N Fly|PC|Diagnostics|Donnelley|Theatres|Automotive|Palace|FUTURE|Shoemakers|of Music|Symphony|YMCA|Serpents|ULC|Physicians|Experts|Professionals)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:Spiral Toys|Armory|Radiology|Fusion|HealthWorks|Hospitals|Indian Tribe|Urology Austin|Water|Pathology|Car Wash|Therapy|Bankers Life|Vineyard|Savory Spice|Head Start|Fine Papers|Internal Medicine)$/) {
            type = "O"
        } else if (name =~ /(?i)(?:Pitco Foods|Materials|Paratransit|Generations|Optometry|Auctioneers|Sarku Japan|HQ|Winery|Morgan Stanley|Handler Sturm|Monterey|Eileen Fisher|W Janke|Roses|Mr\. Cooper|\sEA|\sCLUB)$/) {
            type = "O"
        } else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$)/) {
                    type = "O"
                }
            }
        }
        return type
    }

    def sanitizeName(def name) {
        name = name.toString().replaceAll(/(?i),\s*(?:DDS|Jr\. CPA, CVA|LAc|D\.D\.S\.)/, ' ').trim()
        name = name.toString().trim().replaceAll(/"“"|^and\s/, "")
        name = name.toString().trim().replaceAll(/&#039;/, "'")
        name = name.toString().trim().replaceAll(/&quot;/, '"')
        name = name.toString().trim().replaceAll(/,$|[\)\(‘]/, '')
        name = name.toString().trim().replaceAll(/(?:Certified Public Accountant|and our affiliates|and its subsidiaries|and\s*\/\s*or.*|it affiliates and subsidiaries|on behalf of Retailers.*)$/, '').trim()
        name = name.toString().trim().replaceAll(/(?:on behalf of relevant Data Owner)$/, '').trim()
        name = name.toString().trim().replaceAll(/\s(?:DDS|LAc|MD| M\.D\.)$/, '')
        name = name.toString().trim().replaceAll(/(?<=d).*?(?=TERRA)/, 'o')
        name = name.toString().trim().replaceAll(/\s?(?:and its affiliates|on behalf of|and Affiliates).*$/, '')
        name = name.toString().trim().replaceAll(/(?:LLCo)$/, 'LLC')
        name = name.toString().replaceAll(/\s+/, ' ')
        return name.trim()
    }

    def sanitizeAlias(def alias) {
        alias = alias.toString().trim().replaceAll(/[\)\("”“]/, "")
        alias = alias.toString().trim().replaceAll(/'\S+'$/, "")
        alias = alias.toString().trim().replaceAll(/^(?:formerly|as|known as)\s/, "")
        alias = alias.toString().trim().replaceAll(/^['"“]|[!'"]$|'\S+'$/, "")
        alias = alias.toString().trim().replaceAll(/&quot;|&#039;|hereinafter referred to collectively as|formerly known as/, '')
        alias = alias.toString().trim().replaceAll(/on behalf of (?=Oracle)|^(?:the Firm|on behalf of billers in the attached schedules)$|^d\/b\/a\s|\sand its affiliates$/, '')
        return alias.trim()
    }

    def sanitizeHtmlText(def html) {
        html = html.toString().replaceAll(/(?:&quot;)/, '"')
        html = html.toString().replaceAll(/&#039;/, "'")
        html = html.toString().replaceAll(/(?i),\s*(?:DDS|Jr\., CPA, CVA|M\.D\., (?:F\.A\.C\.P\.|P\.A\.))/, ' ')
        html = html.toString().replaceAll(/(?i),\s*(?=Molohan|Cohen|Agee|P\.A\.|S\.C\.|SPC\.|Vivacity|c\/o|C,\s|L\.L\.P\.|U\.S\.|III|Milone)/, ' ')
        html = html.toString().replaceAll(/(?i),\s*(?=M\.D\.|D\.D\.S\.|EA|Certified|d\/b\/a|dba|doing business as|formerly|together|L\.P\.|LAc|a Delaware|a Medical|a NY|a Nielsen| and D\s|Local 522|Andelson|Loya|Ruud)/, ' ')
        html = html.toString().replaceAll(/(?i),\s*(?=PLLC|LLC|LLC\.|Inc\.?|L\.L\.C\.|P\.C\.|N\.A\.|CPA|Corp\.|Professional|A Psychological|LLP|Ltd\.|IPA\.|FSB|LP|PC|APC|Optometric|TRA|S\.I\.|Limited|Southern)/, ' ')
        html = html.toString().replaceAll(/(?i),\s*(?=Britton|Hutchison|The University|California|Berkeley|on behalf of KUSC|Air|Rail &|Sheet Metal|it affiliates|Fresno|de Jong|an Aon Company|Del Rey|Bernsen &)/, ' ')
        html = html.toString().replaceAll(/, (?:a division of|a subsidiary of|owner of the|on behalf of the insurance brokers listed in Exhibit A of|and its subsidiaries, including|a wholly owned subsidiary of)/, ' d/b/a ')
        html = html.toString().replaceAll(/(?:and its subsidiaries, including|and its related subsidiaries and affiliates including|through its website|and its administrative services entity)/, ' d/b/a')
        html = html.toString().replaceAll(/(?:together with its affiliates|- BL is the marketing brand of)/, ' d/b/a')
        html = html.toString().replaceAll(/(?<=PLLC|LLC\.?|Inc\.?|L\.L\.C\.|P\.C\.|N\.A\.|CPA|Corp\.)\s*(?=[\("']+[A-Z]+[\)"']+)/, ' d/b/a ')
        html = html.toString().replaceAll(/on behalf of(?=\s(?:Rocket|Stripe|Community Memorial|the Royal|KDW))|, abd(?=\s)/, ' d/b/a')
        html = html.toString().replaceAll(/(?:on behalf of 9 Dairy Queen franchise locations|, as well as entities that are franchisees|, on behalf of itself and its applicable subsidiaries)/, '')
        html = html.toString().replaceAll(/\((?:as data maintainer|as data owner|subsidiaries of Iberdrola USA)\)|, on behalf of relevant data owner\(s\)(?=<\/a>)/, '')
        html = html.toString().replaceAll(/\(&quot;APA&quot;\)d\/b\/a The Retirement Advantage Inc\. \(&quot;TRA&quot;\)/, 'APA d/b/a The Retirement Advantage Inc. d/b/a TRA')
        html = html.toString().replaceAll(/\(“OCL”\), a predecessor-in-interest to Relias LLC \(“Relias”\)/, 'd/b/a OCL d/b/a Relias LLC d/b/a Relias')
        html = html.toString().replaceAll(/Bankers Life \(BL\)/, 'Bankers Life d/b/a BL')
        html = html.toString().replaceAll(/(?<=Casualty Co\.), Medicare Supplement.*ins\. co/, ' d/b/a Colonial Penn Life Insurance Co. d/b/a Bankers Conseco Life Insurance Company a NY licensed Ins. co')
        html = html.toString().replaceAll(/\(a trade name under which both CardioNet LLC and LifeWatch Inc\. operate\)/, ' d/b/a CardioNet, LLC d/b/a LifeWatch, Inc. operate ')
        html = html.toString().replaceAll(/(?<=Napa|Agency),\s*(?=Health and|Comprehensive)|(<=Inc\.)(?=d\/b\/a)/, ' ')
        html = html.toString().replaceAll(/(?<=County), (?=Anaheim)/, '-')
        html = html.toString().replaceAll(/(?<=LLC) and (?=Home Warranty)|sold by (?=Colonial)|\sc\/o\s/, ' d/b/a ')
        html = html.toString().replaceAll(/(?<=Limited|Corporation|LLC|\.com|\(APA\)|Fund)\s*;|\/ (?=SF Fire Credit)|(?<=Systems Inc\.) and (?=Santa Fe)/, ',')
        html = html.toString().replaceAll(/(?<=LLC|Company)\sand (?=Forefront|Transamerica)/, ',')
        html = html.toString().replaceAll(/(?i),\s*(?=M\.D\.|Certified|d\/b\/a|dba|doing business as|formerly)/, ' ')
        html = html.toString().replaceAll(/(?:Servi ces)/, 'Services')
        html = html.toString().replaceAll(/(?:Conpany)/, 'Company')
        html = html.toString().replaceAll(/(?:located in Ventura, CA|on its behalf and on behalf of relevant data owners)/, '')
        html = html.toString().replaceAll(/and(?=\sRochester|\sRady)/, ',')
        html = html.toString().replaceAll(/(?i),\s*(?=and its|its affilitates|on behalf of|a political subdivision)/, ' ')
        html = html.toString().replaceAll(/(?i),\s*(?=Inc\. operate)/, ' ')

        return html.trim()
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

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}