package current_scripts

import com.rdc.importer.misc.RelationshipType
import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification

// Client site
import com.rdc.rdcmodel.model.RelationshipType

import java.text.SimpleDateFormat

// Developer site
//import com.rdc.importer.misc.RelationshipType

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Enforcement_and_Disciplinary script = new Enforcement_and_Disciplinary(context)
script.initParsing()

int i = 1;

def nameIdMap = [:];
for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + i++);
    nameIdMap[entity.getName()] = entity
}

for (entity in context.session.getEntitiesNonCache()) {
    for (association in entity.getAssociations()) {

        otherEntity = nameIdMap[association];
        if (otherEntity && !isAlreadyAssociated(entity, otherEntity)) {
            scrapeEntityAssociation = new ScrapeEntityAssociation(otherEntity.getId());
            scrapeEntityAssociation.setRelationshipType(RelationshipType.ASSOCIATE);
            scrapeEntityAssociation.setHashable(otherEntity.getName(), otherEntity.getType());
            entity.addScrapeEntityAssociation(scrapeEntityAssociation);
        }
    }
    entity.getAssociations().clear();
}

def isAlreadyAssociated(entity, otherEntity) {
    Set associations = otherEntity.getScrapeEntityAssociations();
    boolean isAssos = false;
    if (associations) {
        associations.each { item ->
            if (((ScrapeEntityAssociation) item).getId().equals(entity.getId())) {
                isAssos = true;
            }
        }
    }
    return isAssos;
}

class Enforcement_and_Disciplinary {
    final addressParser
    final entityType
    ScrapianContext context = new ScrapianContext()
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def url = "https://www.dos.pa.gov/BusinessCharities/Charities/Resources/Pages/Enforcement-and-Disciplinary-Actions.aspx"

    Enforcement_and_Disciplinary(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        this.context = context
        addressParser.updateCities([US: ["Monroeville", "Fleetwood", "Tionesta", "Laureldale", "Sharon Hill", "Meshoppen", "Mechanicsburg", "Middletown", "Leechburg", "Blairsville", "Tyrone", "Walnutport", "West Pittston", "Laureldale", "Chester Heights", "Stroudsburg", "Tunkhannok", "Bernville", "Sewickley", "Clarks Summit", "Mount Carmel", "Intercession_City", "Orange_Park", "Shiloh", "Warminster", "Newton Square", "Phoenixville", "Wapallopen", "Langhorne", "Parsippany", "Hartville", "Sinking Spring", "Morgantown", "Holmes", "Coal Township", "New Orleans", "York", "Butler", "Clovis", "Allentown", "Pittsburgh", "Rock Hill", "Vacaville", "Scranton", "Harrisburg", "Camden", "Boca Raton", "Columbus", "Boston", "Media", "Alexandria", "Dickson", "Erie", "Pottsville", "Pompano Beach", "Meadville", "Johnstown", "Orange", "Reading", "Moline", "Mobile", "Joliet", "Lebanon", "Kingston", "McKeesport", "Boco Raton", "Sunrise", "Ramsey", "Easton", "Portland", "Clawson", "Santa Clarita", "Oakland", "Flint"],
        ])
    }

    def initParsing() {
        //Invoke html
        def html = invoke(url)
        //Get Data from the Table
        getDataFromTable(html)
    }

    def getDataFromTable(def html) {
        def tableData
        def rowData
        //Table Matcher
        def tableMatcher = html =~ /(?ism)<tbody.*?<\/tbody>/
        while (tableMatcher.find()) {
            tableData = tableMatcher.group(0)
            //Row Matcher
            def rowMatcher = tableData =~ /(?ism)<tr>(\n|\s)?<td\s(style|rowspan).*?\/tr>/
            while (rowMatcher.find()) {
                rowData = rowMatcher.group(0)
                def respondent
                def date
                def typeOfOrder
                def entityUrl
                def respondentMatcher = rowData =~ /<td.*?>\??(?:<a.*?target="_blank">)?(.*?)(?:<\/a>)?(<\/br>)?<\/td>\n?.*[\d]{1,2}\/[\d]{2,4}.*\n?\s?.*<\/tr>/
                def dateMatcher = rowData =~ /[\d]{1,2}\/[\d]{1,2}\/[\d]{2,4}/
                def typeOfOrderMatcher = rowData =~ /\/[\d]{2,4}.*<\/td>\n?\s?<td.*?>(.*)<\/td><\/tr>/
                def entityUrlMatcher = rowData =~ /<tr.*<a href="(.*\.pdf)\"/
                // collecting data from respondent column
                if (respondentMatcher.find()) {
                    respondent = respondentMatcher.group(1)
                }
                respondent = sanitizeName(respondent).toString()
                def address
                (respondent, address) = AddressFinder(respondent.toString())
                def addressList = []
                if (address) {
                    addressList = address.split(/\s+and\s+/)
                }

                //Entity url
                if (entityUrlMatcher.find()) {
                    entityUrl = "https://www.dos.pa.gov" + entityUrlMatcher.group(1)
                }

                // collecting data from date column
                if (dateMatcher.find()) {
                    date = dateMatcher.group()
                }

                // collecting data from type of order column
                if (typeOfOrderMatcher.find()) {
                    typeOfOrder = typeOfOrderMatcher.group(1)
                }
                typeOfOrder = sanitizeName(typeOfOrder).toString().replaceAll(/^[\?]/, "")
                typeOfOrder = finalSanitize(typeOfOrder)

                def unCorrectedNameList = []
                def nameList = []
                def name = respondent
                if (name.contains("24/7") || name.contains("Bikers and Belly") || name.contains("Christy and Rick") || name.contains("Dyer and Associates") || name.contains("Museum and") || name.contains("Hook and Ladder") || name.contains("Arts and Humanities") || name.contains("Hope and Help") || name.contains("Water and") || name.contains("and Development") || name.contains("Dedication and Everlasting") || name.contains("Alliance for Health") || name.contains("Rise and Shine") || name.contains("​Business and Professional") || name.contains(" and Education") || name.contains("Champion Publishing") || name.contains("American Society for Metabolic and Bariatric") || name.contains("American Trade and Convention") || name.contains(" Rescue and Adoption") || name.contains(" Firefighters and Paramedics") || name.contains(" Police and Sherrifs") || name.contains(" Education and ") || name.contains(" Religion and Peace") || name.contains(" O'Brien and Company") || name.contains(" Disabled and Handicapped") || name.contains(" Autism and Families") || name.contains(" Arts and Recreation") || name.contains("Poets and Writers") || name.contains(" Choir and Chorale") || name.contains(" Historical and Preservation") || name.contains(" Veterans and Patriots") || name.contains(" Associate and Resource") || name.contains("Tiger Ranch")||name.contains("Frederick A. Simone") ||name.contains("No Kill Shelter")) {
                    if (name.contains(";")) {
                        unCorrectedNameList = name.split(";")
                    } else {
                        unCorrectedNameList.add(name)
                    }
                } else if (name =~ /(?ism)^(Impact.+?Group)\sand\s(.+)$/) {
                    def nameMacher = name =~ /(?ism)^(Impact.+?Group)\sand\s(.+)\u0024/
                    if (nameMacher.find()) {
                        nameList.add(nameMacher.group(1))
                        nameList.add(nameMacher.group(2))
                    }
                } else {
                    if(name =~ /(?ism)Mediation Ministries and Litigation|North Central Community and Economic|Jewish Adoptions and Family Care|on Taxation and Economic Policy|Action on Smoking and Health/){
                        unCorrectedNameList.add(name)
                    }
                    else{
                        unCorrectedNameList = name.split(/and\/or|\/|\sand\s/)
                    }
                }

                unCorrectedNameList.each {
                    it = it.toString().trim()
                    if (it.toString().contains("a.k.a") || it.toString().contains("Inc") || it.toString().contains("Ltd") || it.toString().contains("The ") || it.toString().contains("the ") || it.toString().contains("Water and People") || it.toString().contains("and Development") ||it.contains("Linzy & Partners")||it.contains("PPL Electric Utilities Corporation")) {
                        nameList.add(it.toString().trim())
                    } else {

                        if (it =~ /(?ism)^([\w]+)\,\s([\w]+)\s?(?:\,|and)\s([\w]+)\,\s([\w]+)\u0024/) {
                            def nameMacher = it =~ /(?ism)^([\w]+)\,\s([\w]+)\s?(?:\,|and)\s([\w]+)\,\s([\w]+)\u0024/
                            if (nameMacher.find()) {
                                nameList.add(nameMacher.group(2) + " " + nameMacher.group(1))
                                nameList.add(nameMacher.group(4) + " " + nameMacher.group(3))
                            }
                        } else if (it =~ /(?ism)(.*?)\,\s*(.*?)\s*\u0024/) {
                            if(it.contains("Bikers and Belly Dancers Ride Against Cancer")){
                                def name1 = it.toString().split(/(?ism),|and (?=Steve)/)
                                nameList.addAll(name1)
                            }
                            else if (it =~ /(?ism)(.*?)\,\s*(.*?)\s+and\s+(.+?)\s*\u0024/) {
                                def nameMatcher = it =~ /(?ism)(.*?)\,\s*(.*?)\s+and\s+(.+?)\s*\u0024/
                                if (nameMatcher.find()) {
                                    def name1 = nameMatcher.group(2) + " " + nameMatcher.group(1)
                                    nameList.add(name1)
                                    def name2 = nameMatcher.group(3) + " " + nameMatcher.group(1)
                                    nameList.add(name2)

                                } else {
                                    nameList.add(it)
                                }
                            }else if(it =~ /(?ism)(O&P Rescue|Community.*?Corporation|Hunting 4 Hope|United.*?Ministeries), (William Ura|Kelly.*?Theatre|Donald Miller|Columbus.*?Ministries)/) {
                                def matcher = it =~ /(?ism)(O&P Rescue|Community.*?Corporation|Hunting 4 Hope|United.*?Ministeries), (William Ura|Kelly.*?Theatre|Donald Miller|Columbus.*?Ministries)/
                                if(matcher.find()) {
                                    nameList.add(matcher.group(1))
                                    nameList.add(matcher.group(2))
                                }
                            } else if (it =~ /(?ism)(.*?)\,\s*(.*?)\s*$/) {
                                it = nameFixer(it)
                                nameList.add(it)
                            } else {
                                nameList.add(it)
                            }
                        } else {
                            nameList.add(it)
                        }
                    }
                }
                nameList.each {
                    if (it) {
                        def aliasList = []
                        (it, aliasList) = aliasChecker(it)
                        it = finalSanitize(it)
                        if(it && !it.toString().contains("null"))
                            createEntity(it, addressList, aliasList, entityUrl, typeOfOrder, date)
                    }
                }
            }
        }
    }

    def nameFixer(def name) {
        if (!name.toString().contains(" Inc") && !name.toString().contains(" Ltd") && !name.toString().contains("The ") && !name.toString().contains("(The)") && !name.toString().contains("True Majority, a project of") && !name.toString().contains("LLC")&& !name.toString().contains("Big Sisters of Morris")&& !name.toString().contains(" LTD")&& !name.toString().contains(" LLP")&& !name.toString().contains("Water and People")&& !name.toString().contains("Alliance for Health") && !name.toString().contains("Linzy & Partners")&& !name.toString().contains("Veterans, Fund, The")&& !name.toString().contains("PPL Electric Utilities Corporation")&& !name.toString().contains("Rossi, O'Brien and Company")&& !name.toString().contains("House of Mercy")) {
            def nameMatcher = name =~ /(?ism)(.*?)\,\s*(.*?)\s*$/
            if (nameMatcher.find()) {
                name = nameMatcher.group(2) + " " + nameMatcher.group(1)
                name = name.replaceAll(/\,/, "")
            }
        }
        return name
    }

    def AddressFinder(respondent) {
        def address
        if (respondent =~ /(,|\.)?[\w \-]+\,\s*Sewickley.*/) {
            def addressMatcher = respondent =~ /(,|\.)?[\w \-]+\,\s*Sewickley.*/
            while (addressMatcher.find()) {
                address = addressMatcher.group(0).replaceAll(/^\,/, "").trim()
                respondent = respondent.toString().replaceAll(/(,|\.)?[\w \-]+\,\s*Sewickley.*/, "").replaceAll(/^\?/, "") trim()
            }
        } else if (respondent =~ /(,\s+(\d+|PO).*)?(,|\.)[\w \-]+\,\s*(PA|CA|SC|VA|LA|DC|WA|MO,|FL|IL|GA|NY|WY|MD|NJ|AL|TX|MI|OR|OH|WI)\s*[0-9]{0,5}/) {
            def addressMatcher = respondent =~ /(,\s+(\d+|PO).*)?(,|\.)[\w \-]+\,\s*(PA|CA|SC|VA|LA|DC|WA|MO,|FL|IL|GA|NY|WY|MD|NJ|AL|TX|MI|OR|OH|WI)\s*[0-9]{0,5}/
            while (addressMatcher.find()) {
                address = addressMatcher.group(0).replaceAll(/^\,/, "").trim()
                respondent = respondent.toString().replaceAll(/(,\s+(\d+|PO).*)?(,|\.)[\w \-]+\,\s*(PA|CA|SC|VA|LA|DC|WA|MO,|FL|IL|GA|NY|WY|MD|NJ|AL|TX|MI|OR|OH|WI)\s*[0-9]{0,5}/, "").replaceAll(/^\?/, "").trim()
            }
        }
        return [respondent, address]
    }

    def finalSanitize(def name) {
        name = name.toString().trim()
        name = name.toString().replaceAll(/\,\s?$/, "").trim()
        if (name.toString().endsWith("(The)") || name.toString().endsWith("The") || name.toString().endsWith("the")) {
            def nameMatcher = name =~ /(?ism)(.*?)\s*(\(?the\)?)\s*\u0024/
            if (nameMatcher.find()) {
                name = nameMatcher.group(2) + " " + nameMatcher.group(1)
                name = name.replaceAll(/\(/, "").replaceAll(/\)/, "")
            }
        }
        if (name =~ /(.+?\,\s?)(The),\s(\d+)/) {
            def nameMatcher = name =~ /(.+?\,\s?)(The),\s(\d+)/
            if (nameMatcher.find()) {
                name = nameMatcher.group(2) + " " + nameMatcher.group(1) + nameMatcher.group(3)
                name = name.replaceAll(/\(/, "").replaceAll(/\)/, "")
            }
        } else if (name =~ /(?ism)([\w \.\-\,]+Inc.\s)(The[\w \.\-]+)/) {
            def nameMatcher = name =~ /(?ism)([\w \.\-\,]+Inc.\s)(The[\w \.\-]+)/
            if (nameMatcher.find()) {
                name = nameMatcher.group(2) + " " + nameMatcher.group(1)
            }
        }
        name = name.toString().replaceAll(/\,$/, "").trim()
        name = name.toString().replaceAll(/\;$/, "").trim()
        name = name.toString().replaceAll(/\u200B/, "")
        name = name.toString().replaceAll(/\!/, "")
        name = name.toString().replaceAll(/its Director\,?/, "")
        name = name.toString().replaceAll(/(?ism) Sr\./,"")
        name = name.toString().replaceAll(/(?ism) Jr\./,"")
        name = name.replaceAll(/(?ism)Mary Beth;/,"Mary Beth")
        return name
    }

    def sanitizeName(def name) {
        name = name.toString().trim()
        name = name.toString()
            .replaceAll(/(?i)(\(PDF\))/, "")
            .replaceAll(/<f.*?font>/, "")
            .replaceAll(/<strong.*?strong>/, "")
            .replaceAll(/(?s)\s+/, " ")
            .replaceAll(/<p.*?>/, "")
            .replaceAll(/<\/p>/, "")
            .replaceAll(/<br>/, "")
            .replaceAll(/<a.*?>/, "")
            .replaceAll("\\?", "")
            .replaceAll("&#58;", ":")
            .replaceAll("\\&amp;", "&")
            .replaceAll("\\&quot;", "")
            .replaceAll(/\)/, "\\) ")
            .replaceAll(/Jr\./, "")
            .replaceAll(/individually.*/, "")
            .replaceAll(/(?i)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)/, "a.k.a")
            .replaceAll(/<\/a>/, "").trim()
        name = name.replaceAll(/(?ism), Consent Agreement and Order\.pdf/,"")
        name = name.toString().replaceAll(/\,$/, "").trim()
        name = name.toString() replaceAll(/(?ism)(Aspers.+Club)(.+EMS)/, { def a, b, c -> return b })
        name = name.replaceAll(/(?ism)([^,]) (PA \d{5})/,{def a,b,c-> return b+", "+c})
        name = name.replaceAll(/(?ism), NY, 10018/,", NY 10018")
        name = name.replaceAll(/(?ism)Association of Chester, PA 19016/,"Association of Chester, Chester, PA 19016")
        name = name.replaceAll(/(?ism)Tiger Ranch, Inc. Lin Marie/,"Tiger Ranch, Inc. ; Lin Marie")
        name = name.replaceAll(/(?ism)A. Simone, M.D. Foundation,/,"A. Simone ; M.D. Foundation,")
        name = name.replaceAll(/(?ism)(Friends Thrift Shop), (Murrysville Christian Concern a\.k\.a)/,{def a,b,c-> return c+" "+b})
        name = name.replaceAll(/(?ism)No Kill Shelter, Samantha Frey/,"No Kill Shelter; Samantha Frey")
        return name
    }

    def aliasChecker(def name) {
        def aliasList = []
        def alias
        if (name.contains("Incorporated in California as")) {
            def aliasMatcher = name =~ /(?i)Incorporated in California as:(.*)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/(?i)Incorporated in California as:(.*)/, "").replaceAll(/&amp;/, "&").trim()
                alias = finalSanitize(alias)
                aliasList.add(alias)
            }
        }
        if (name =~ /(?ism)\s?(Community Theatre.*?Corporation)\,\s(.+)/) {
            def aliasMatcher = name =~ /(?ism)\s?(Community Theatre.*?Corporation)\,\s(.+)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(2)
                name = aliasMatcher.group(1)
                alias = alias.trim()
                aliasList.add(alias)
            }
        }
        if (name.toString().contains("(") && !(name.toString().contains("(The)"))) {
            def aliasMatcher = name =~ /(?i)\((.*?)\)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/(?i)\((.*?)\)/, "").replaceAll(/&amp;/, "&").trim()
                alias = finalSanitize(alias)
                alias = nameFixer(alias)
                aliasList.add(alias)
            }
            if (!(name.contains(")"))) {
                aliasMatcher = name =~ /(?i)\((.*)/
                while (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1)
                    name = name.toString().replaceAll(/(?i)\((.*)/, "").replaceAll(/&amp;/, "&").trim()
                    alias = finalSanitize(alias)
                    alias = nameFixer(alias)
                    aliasList.add(alias)
                }
            }
        }
        if (name =~ /(?i)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)(?:\:|\s|\.|\,|\;)?(.*)/) {
            def aliasMatcher = name =~ /(?i)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)(?:\:|\s|\.|\,|\;)?(.*)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/(?i)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)(?:\:|\s|\.|\,|\;)?(.*)/, "").replaceAll(/^\?/, "").trim()
                if (alias =~ /(?i)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)(?:\:|\s|\.|\,|\;)?(.*)/) {
                    alias = alias.split(/(?i)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)/)
                    alias.each { it ->
                        it = finalSanitize(it)
                        it = nameFixer(it)
                        aliasList.add(it)
                    }
                } else {
                    alias = finalSanitize(alias)
                    alias = nameFixer(alias)
                    aliasList.add(alias)
                }
            }
        }
        if (name =~ /(?i)(.*)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)/) {
            def aliasMatcher = name =~ /(?i)(.*)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/(?i)(.*)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)/, "").replaceAll(/^\?/, "").trim()
                if (alias =~ /(?i)(.*)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)/) {
                    alias = alias.split(/(?i)(?:a[\.\/]?k[\.\/]?a|f[\.\/]?k[\.\/]?a|(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a|n[\.\/]?k[\.\/]?a)/)
                    alias.each { it ->
                        it = finalSanitize(it)
                        it = nameFixer(it)
                        aliasList.add(it)
                    }
                } else {
                    alias = finalSanitize(alias)
                    alias = nameFixer(alias)
                    aliasList.add(alias)
                }
            }
        }
        name = nameFixer(name)
        return [name, aliasList]
    }

    def createEntity(def name, def addressList, def aliasList, def entityUrl, def typeOfOrder, def date) {
        def entity
        if (!name.toString().isEmpty()) {
            name = finalSanitize(name)
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType]);
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }
            def aliasEntityType, alias
            aliasList.each {
                if (it) {
                    it = sanitizeName(it)
                    it = it.replaceAll(/(?s)\s+/, " ").trim()
                    aliasEntityType = detectEntity(it)
                    if (aliasEntityType.toString().equals(entityType)) {
                        entity.addAlias(it)
                    } else {
                        entity.addAssociation(it)
                        def newEntity = context.findEntity(["name": it, "type": aliasEntityType]);
                        if (!newEntity) {
                            newEntity = context.getSession().newEntity();
                        }
                        newEntity.setName(it)
                        newEntity.setType(aliasEntityType)
                        newEntity.addAssociation(name)
                        addCommonPart(newEntity, addressList, entityUrl, typeOfOrder, date)
                    }
                }
            }
            addCommonPart(entity, addressList, entityUrl, typeOfOrder, date)
        }
    }

    def addCommonPart(def entity, def addressList, def entityUrl, def typeOfOrder, def date) {
        if (addressList) {
            def scrapeAddressList = []
            addressList.each {
                it = sanitizeAddress(it).trim()
                it = sanitizeData(it)
                if (it) {
                    def addrMap = addressParser.parseAddress([text: it, force_country: true])
                    ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                    if (scrapeAddress) {
                        scrapeAddressList.add(scrapeAddress)
                    }
                }
                entity.addAddresses(scrapeAddressList)
            }
        } else {
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            scrapeAddress.setProvince("Pennsylvania")
            scrapeAddress.setCountry("UNITED STATES")
            entity.addAddress(scrapeAddress)
        }
        if (entityUrl) {
            entity.addUrl(entityUrl)
        }
        ScrapeEvent event = new ScrapeEvent()
        typeOfOrder = typeOfOrder.replaceAll(/(?s)\s+/, " ").trim()
        if (typeOfOrder) {
            typeOfOrder = "This entity appears on the Pennsylvania Department of State’s list of Enforcement and Disciplinary Actions. Type of Order: " + typeOfOrder
            event.setDescription(typeOfOrder)
        } else {
            typeOfOrder = "This entity appears on the Pennsylvania Department of State’s list of Enforcement and Disciplinary Actions."
            event.setDescription(typeOfOrder)
        }

        def remark
        if (date) {

            date =  context.parseDate(new StringSource(date), ["MM/d/yy"] as String[])

            def currentDate = new Date().format("MM/dd/yyyy")
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy")
            def date1 = simpleDateFormat.parse(currentDate)
            def date2 = simpleDateFormat.parse(date)

            if (date2.compareTo(date1) > 0) {
                remark = date
                date = null
                entity.addRemark(remark)
            } else {
                event.setDate(date)
            }
        }
        entity.addEvent(event)

    }

    def detectEntity(def name) {
        def type
        if(name =~ /(?i)(?:Kute)/){
            type = "P"
        }
        else  if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:Aid|Creativity|\bVets\b|\bObesity\b|\bHomeless\b|Sanctuary|Kayla|Change|Purpose|Life|Welfare|Term|Dynamics|Abuse|Cub|Rebound|Speak|Police|Boys|Charities|Opportunity|Pennsylvania|Vision|Mission|Alternatives|Adoptions|Camp|Embassy|Christie|Geek|Fall|Policy|Publications|Cure|Brotherhood|Studios|Forum|Powerkids|Workshop|Hightower|Families|Citizens|Wishes|Nationalist|Brothers|Cancer|Autism|Hope|Americans|Clinic|Medicine|Animals|Future)/) {
                    type = "O"
                }
            }
            if (type.equals("O")) {
                if (name =~ /(?i)(?:stephen\sL.|Thomas\sShields|Kute)|Jason\sHunt/) {
                    type = "P"
                }
            }
        }
        return type
    }

    def sanitizeAddress(address) {
        address = address.toString().trim()
        address = address.toString().replaceAll("Orange Park, FL 32073", "Orange_Park, FL 32073")
        address = address.toString().replaceAll("Intercession City, FL 33848", "Intercession_City, FL 33848")
        address = address.toString().replaceAll("\\u00e4", "a")
        address = address.toString().replaceAll("\\u00f6", "o")
        address = address.toString().replaceAll("Last Known Address: ", "")
        address = address.toString().replaceAll("Alletown", "Allentown")
        address = address.toString().replaceAll("Post Office", "")
        address = address.toString().replaceAll("(?i)District", "")
        address = address.toString().replaceAll("district", "")
        address = address.toString().replaceAll("Republique du Congo", "Republic of the Congo")
        address = address.toString().replaceAll("(?i)\bNP\b", "New Providence")
        address = address.toString().replaceAll("(^P|^O)\\.", " ")
        address = address.toString().replaceAll("Zip Code", "")
        address = address.toString().replaceAll("Hubei Province", "Hubei")
        address = address.toString().replaceAll("Etobicoke \\(Ontario\\)", "Etobicoke, Ontario")
        address = address.toString().replaceAll("&quot", "\"")
        address = address.toString().replaceAll("zona 9, Edificio Plaza del Sol", ",zona 9, Edificio Plaza del Sol,Guatemala City,Guatemala")
        address = address.toString().replaceAll(", ,", ",")
        address = address.toString().replaceAll(",,", ",")
        address = address.toString().replaceAll(";", "")
        address = address.toString().replaceAll("Alternate address", "")
        address = address.toString().replaceAll("Ioannina-Athens", ", Beesd").replaceAll("The Netherlands", "Netherlands")
        address = address.toString().replaceAll(" ,", ",")
        address = address.toString().replaceAll("\\(\\)", "")
        address = address.toString().replaceAll(/'/, "\\u2019")
        address = address.toString().replaceAll(/^,/, "").trim()
        address = address + ", United States"

        return address
    }

    def sanitizeData(data) {
        data = data.toString().replaceAll('&aacute;', "a")
        data = data.toString().replaceAll('&atilde;', "a")
        data = data.toString().replaceAll("&eacute;", "e")
        data = data.toString().replaceAll("&oacute;", "o")
        data = data.toString().replaceAll("&uacute;", "u")
        data = data.toString().replaceAll("&ntilde;", "n")
        data = data.toString().replaceAll("&auml;", "a")
        data = data.toString().replaceAll("&ouml;", "o")
        data = data.toString().replaceAll("&iacute;", "i ")
        data = data.toString().replaceAll("&ordm;", "")
        data = data.toString().replaceAll("&deg;", "")
        data = data.toString().replaceAll("&#39;", "'")
        data = data.toString().replaceAll("&#39;", "'")
        data = data.toString().replaceAll("\\ufffd", "e")
        data = data.toString().replaceAll("\\u0142", "")
        data = data.toString().replaceAll("&ndash;", "-")
        data = data.toString().replaceAll(/(?s)\s+/, " ").trim()
        return data
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().replaceAll("City", "").replaceAll("city", "").replaceAll("Province", "").replaceAll("People's Republic of", "")
            .replaceAll(",\\s*,", ",").replaceAll(",\\s*,\\s*,", ",")
        street = street.toString().replaceAll(/(?ism),\s*$/, "")
        street = street.toString().replaceAll("United State of", "")
        street = street.toString().replaceAll("The Republic of", "")
        street = street.toString().replaceAll("\\(,Canada,Canada\\)", "")
        street = street.toString().replaceAll(/(?ism)Managing Director.*?ltd.?/, "")
        street = street.toString().replaceAll(/(?s)\s+/, " ").trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        street = street.toString().replaceAll(/\(/, "").trim()
        return street
    }

    def invoke(url, cache = false, tidy = false) {
        return context.invoke([url: url, tidy: tidy, cache: cache])
    }
}