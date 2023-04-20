package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

import java.text.SimpleDateFormat

/**
 Needs VPN
 03/12/2021

 * */

context.setup([connectionTimeout: 120000, socketTimeout: 120000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_albama_suspended script = new Us_albama_suspended(context)
script.initParsing()

def class Us_albama_suspended {

    final addressParser
    final entityType
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context
    final String root = "https://medicaid.alabama.gov"
    final String mainUrl = root + "/content/8.0_Fraud/8.7_Suspended_Providers.aspx"


    Us_albama_suspended(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        //addressParser.reloadData()
//        addressParser.updateCities([LB: ["Al Ghubayrah", "Bodai", "Beirut", "Ghobeiry=/(?i)Ghobeiry\\s+(?!center)/"],
//                                    QA: ["Ummsulal Ali", "Al Khartiyath", "Al Rayyan Al Jadeed", "Al Ganam Al Jadeed", "Al Sailiyya", "Muaither", "Al Azeeziya"],
//                                    IR: ["Dezfol"],
//                                    PK: ["Quetta"]
//        ])
        this.context = context
    }
    //------------------------------Initial part----------------------//

    def initParsing() {
        //Invoking from URL
        //comment start
        def html = context.invoke([url: mainUrl, tidy: false, cache: false])
        def pdfUrl, pdfUrlMatcher
        // if ((pdfUrlMatcher = html =~ /(?ism)<a href="\.+\/\.+(\/documents.+?21\.pdf)"/)) {
        if ((pdfUrlMatcher = html =~ /(?ism)<a href="\.+\/\.+(\/documents.+?22\.pdf)"/)) {
            pdfUrl = root + pdfUrlMatcher[0][1]
        }
        //comment end
        def pdfText
        pdfText = pdfToTextConverter(pdfUrl)
        pdfText = sanitizeData(pdfText)
        pdfText = rowFixing(pdfText)
        getDataFromPDF(pdfText)
    }

    def createEntity(entityName, eventDate, occupation2, aliasList, address) {

        if (entityName == null) return
        if (entityName.trim().size() < 1) return

        def entity = null
        def entityType = detectEntity(entityName)
        if (entityType.toString().equals("P")) {
            entityName = sanitizeName(formatName(entityName))
        }
        entityName = sanitizeAlias(entityName)

        entity = context.findEntity(["name": entityName]);
        if (!entity) {
            entity = context.getSession().newEntity();
            entity.setName(entityName)
            entity.setType(entityType)
        }
        def aliasEntityType, alias
        aliasList.each {
            if (it) {
                it = sanitizeAlias(it)
                aliasEntityType = detectEntity(it)
                if (aliasEntityType.toString().equals(entityType)) {
                    entity.addAlias(it)
                } else {
                    if (aliasEntityType.equals("P")) {
                        it = sanitizeAlias(it)
                        it = formatName(it)
                        entity.addAssociation(it)
                    } else {
                        it = sanitizeAlias(it)
                        entity.addAssociation(it)

                        //create new entity with association

                        def newEntity = context.findEntity(["name": it, "type": "O"]);
                        if (!newEntity) {
                            newEntity = context.getSession().newEntity();
                            newEntity.setName(it)
                            newEntity.setType(aliasEntityType)
                        }
                        newEntity.addAssociation(entityName)
                        addCommonPartOfEntity(newEntity, address, eventDate, occupation2)
                    }
                }
            }
        }//

        addCommonPartOfEntity(entity, address, eventDate, occupation2)
    }


    def addCommonPartOfEntity(def entity, def address, def eventDate, def occupation2) {

        //def eventDescription = "This entity appears on the Alabama Medicaid Agency List of Suspended Providers. The suspension was initiated by " + description
        def eventDescription = "This entity appears on the Alabama Medicaid Agency List of Suspended Providers."
        ScrapeEvent event = new ScrapeEvent()
        if (eventDate) {
            eventDate = context.parseDate(new StringSource(eventDate), ["MM/dd/yy"] as String[])

            def isFuture = isFutureDate(eventDate)

            if (isFuture)
                entity.addRemarks("The event date is a future date: " + eventDate)
            else
                event.setDate(eventDate)
        }
        if (eventDescription) {
            event.setDescription(eventDescription)
        }
        entity.addEvent(event)

        if (address) {
            def addrMap = addressParser.parseAddress([text: address, force_country: true])
            def scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
            if (scrapeAddress) {
                entity.addAddress(scrapeAddress)
            }
        } else {
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            scrapeAddress.setProvince("Alabama")
            scrapeAddress.setCountry("UNITED STATES")
            entity.addAddress(scrapeAddress)
        }
        if (occupation2) {
            occupation2 = occupation2.toString().replaceAll(/(?i)Owner,.+/, "Owner").trim()
            entity.addOccupation(occupation2.trim())
        }
        //printlln("Assiociate: $entity.associations FN: $entity.name O: $occupation2 A: $entity.aliases\n")
    }

    def detectEntity(def name) {
        def type
        if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:Phamacy|Druggists|Homecare|Neuromedical|Clinic|Medicine|Hearing\s+Aid|Medi\-Screens)/) {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:Ndolo|Templin-Veal|Sharpe|Terri Jackson|Christie L|William House|Michelle Elizabeth|Ben Parker|Margaret Leigh|Philip L|Ashley Nicole|Honey|Logic|Tangella Jackson|Kathy)/) {
                type = "P"
            }
        }
        return type
    }

    def getDataFromPDF(def pdfText) {
        pdfText = pdfText.toString().replaceAll(/(?ism)(OCCUPATIONAL \w+\s+providers\s+)(AUDIOLOGY PROVIDERS)/, '$1' + '\nthis_will_be_replaced\n' + '$2')
        def ocTable
        def ocTableMatcher = pdfText =~ /(?ism)(^\S[A-Z\s\/]+\n).*?(?=(?:^\S[A-Z\s\/]+\n|\Z))/

        while (ocTableMatcher.find()) {
            def occupation
            ocTable = ocTableMatcher.group(0)
            occupation = ocTableMatcher.group(1)
            ocTable = ocTable.toString().replaceAll(/$occupation/, /$occupation/ + "\n")

            def row

            //New row capturing regex
            def rowMatcher = ocTable =~ /(?ism)(?:(?:\S+\s)|(?:\S+\s)+\s{19,})+\s+\d+\\/\d+\\/\d+/

            while (rowMatcher.find()) {
                //Occupation
                def occupation2
                occupation2 = occupation

                def aliasList = []
                row = rowMatcher.group(0)
                //def name, eventDate, description
                def name, eventDate

                //Name
                def dataMatcher = row =~ /(?ism)^(.+?)\s+(\d+\\/\d+\\/\d+)\s*$/
                if (dataMatcher.find()) {
                    name = dataMatcher.group(1).toString().trim()
                    name = sanitizeName(name)
                    //EVENT
                    eventDate = dataMatcher.group(2)
                }
                def alias, aliasMatcher
                // FKA ALIAS//
                if (name.toString().contains("fka")) {
                    if ((aliasMatcher = name =~ /(?ism)\((fka.*?)\)/)) {
                        alias = aliasMatcher[0][1]
                        name = name.toString().replaceAll(/$alias/, "")
                            .replaceAll(/\(\)/, "").trim()
                        alias = alias.toString().replaceAll(/\bfka\b/, "").trim()
                    }
                    if (alias) {
                        aliasList.add(alias)
                    }
                }
                if (name =~ /\baka\b/) {
                    if ((aliasMatcher = name =~ /(?ism)^.*?\((.*?)(?:\)|$)/)) {
                        alias = aliasMatcher[0][1]
                        name = name.toString().replaceAll(/$alias/, "")
                            .replaceAll(/\(/, "").replaceAll(/\)/, "")

                        alias = aliasMatcher[0][1].toString().replaceAll("aka", "")
                    }
                    if (alias.toString() =~ /\,|\band/) {
                        aliasList = alias.toString().split(/(?i),(?!\s*inc\.*|LLC|III)|\s+and\s+/).collect({ its -> return its })
                        aliasList.each {
                            it = it.toString().replaceAll(/\n/, " ").replaceAll("&", "").trim()
                        }
                    } else if (alias.toString().contains("&")) {
                        aliasList = alias.toString().split(/&/).collect({ its -> return its })
                    } else {
                        if (alias) {
                            aliasList.add(alias)
                        }
                    }
                } else if (name.toString().contains("a.k.a")) {
                    if ((aliasMatcher = name =~ /(?ism)(?<=a.k.a.)(.*?)$/)) {
                        alias = aliasMatcher[0][1].toString().trim()
                        name = name.toString().replaceAll(/$alias/, "").replaceAll("a.k.a.", "")

                        if (alias) {
                            aliasList.add(alias)
                        }
                    }
                }
                //BRACKET ALIASES
                else if (name.toString().contains("(")) {

                    if (name =~ /(?i)(?:Parker Gay|Douglas Schaffer|Muratta|Richard Dempsey)/) {
                        name = name.toString().replaceAll(/(?i)\s+owner/, ", owner")
                    }
                    //,Owner
                    if (name =~ /(?ism)\(.*?, owner[s]?(?:\)|\n)/) {

                        if ((aliasMatcher = name =~ /(?ism)CCMC\),\s*LLC\s*(.*?)(?=, owner[s]?(?:\)|\n))/)) {

                            alias = aliasMatcher[0][1].toString().replaceAll(/\(/, "")
                            name = name.toString().replaceAll(/$alias/, "")
                                .replaceAll(/(?i)\(, owner[s]?\)/, "").trim()
                            alias = alias.toString().replaceAll(/(?i), [d]?m\.*d\.*/, "").replaceAll(/(?i)\s*owner/, "")
                                .replaceAll(/(?i)MD/, "").trim()

                            if (alias =~ /(?ism),\s+(?!Inc\.*|LLC.?|III)/) {
                                aliasList = alias.toString().split(/(?ism),\s+(?!Inc\.|III)/).collect({ its -> return its })
                                aliasList.each {
                                    it = it.toString().replaceAll(/,/, "").replaceAll(/&/, "").trim()
                                }
                            } else {
                                if (alias) {
                                    aliasList.add(alias)
                                }
                            }
                        } else if ((aliasMatcher = name =~ /(?ism)\((.*?)(?=, owner[s]?(?:\)|\n))/)) {
                            alias = aliasMatcher[0][1]
                            name = name.toString().replaceAll(/(?ism)(?<=\()$alias/, "")
                                .replaceAll(/(?i)\(, owner[s]?\)/, "")
                            alias = alias.toString().replaceAll(/(?i), [d]?m\.*d\.*/, "").replaceAll(/(?i)\s*owner/, "")
                                .replaceAll(/(?i)MD/, "").trim()

                            if (alias =~ /(?ism),\s+(?!Inc\.)/) {
                                aliasList = alias.toString().split(/(?ism),\s+(?!Inc\.|III)/).collect({ its -> return its })
                                aliasList.each {
                                    it = it.toString().replaceAll(/,/, "").replaceAll(/&/, "").trim()
                                }
                            } else {
                                if (alias) {
                                    aliasList.add(alias)
                                }
                            }
                        }
                    }
                    //Owner ,
                    else if ((aliasMatcher = name =~ /(?ism)\((?:owner|owner\/operator|officer Manager\/owner|Vice president),(.*?)(?:\)|$)/)) {
                        alias = aliasMatcher[0][1].toString().trim()
                        name = name.toString().replaceAll(/$alias/, "")
                            .replaceAll(/(?i)\(Vice President,*/, "")
                            .replaceAll(/(?i)\(owner,\s*\)/, "")
                            .replaceAll(/(?i)\(owner\/operator,\s*\)/, "")
                            .replaceAll(/(?i)\(officer manager\/owner,\s*\)/, "")
                            .trim()
                        if (alias) {
                            aliasList.add(alias)
                        }
                    } else if ((aliasMatcher = name =~ /(?ism)\((.*?), owner[s]?\s*&\s*Pharmacist\)/)) {
                        alias = aliasMatcher[0][1].toString().trim()
                        name = name.toString().replaceAll(/$alias/, "")
                        name = name.toString().replaceAll(/(?ism)\(.*?, owner[s]?\s*&\s*Pharmacist\)$/, "").trim()
                        if (alias) {
                            aliasList.add(alias)
                        }
                    }
                    //DME OWNER
                    else {
                        name = name.toString().replaceAll(/(?ism)(\(.*?\))/, "").trim()
                    }
                }
                //Owner without brackets
                if (name.toString().contains("Owner")) {
                    if ((aliasMatcher = name =~ /(?ism)owner of\s+(?!Counseling services)(.*?)$/)) {
                        alias = aliasMatcher[0][1].toString().trim()
                        name = name.toString().replaceAll(/$alias/, "")
                            .replaceAll(/(?i)counselor, owner of/, "")
                            .replaceAll(/(?i)Counselor & owner of/, "")
                            .replaceAll(/(?i)owner of/, "")
                            .replaceAll(/(?ism),\s*$/, "").trim()
                        if (alias) {
                            aliasList.add(alias)
                        }
                    }
                }
                //ADDRESS
                def address, addressMatcher
                if ((addressMatcher = name =~ /(?ism)(,\s*\d+.*?(?=\()|,\s*\d+.*?$)/)) {
                    address = addressMatcher[0][1]
                    name = name.toString().replaceAll(/$address/, "")
                    address = address.toString().replaceAll(/^,/, "").trim()
                } else if ((addressMatcher = name =~ /(?ism)(,\s*\S+,\s*Alabama)/)) {
                    address = addressMatcher[0][1]
                    name = name.toString().replaceAll(/$address/, "")
                    address = address.toString().replaceAll(/^,/, "").trim()
                }
                //DBA ALIAS
                if (name.toString().contains("dba")) {
                    if ((aliasMatcher = name =~ /(?ism)(?<=dba)(.*?)$/)) {
                        alias = aliasMatcher[0][1].trim()
                        name = name.toString().replaceAll(/$alias/, "")
                            .replaceAll(/\bdba\b/, "").trim()
                    }
                    if (alias) {
                        aliasList.add(alias)
                    }
                }

                //OCCUPATION
                def otherOccupation
                if (occupation2 =~ /(?i)OTHER/) {
                    otherOccupation = fixOccupation(name)
                    occupation2 = otherOccupation
                    name = name.toString().replaceAll(/$occupation2/, "").replaceAll(/(?ism),\s*$/, "").trim()
                } else {
                    name = occSanitize(name)
                    def removeString, removeMatcher
                    if ((removeMatcher = name =~ /(?im)^.*?,\s+(?!(?:[Js]r\.|\bI{1,3}\b|\bIV\b|\bV\b)).*?,\s+(?!(?:\b\w\.|[Js]r\.|I{1,3}|IV|V))(.*?)$/)) {
                        removeString = removeMatcher[0][1].toString().replaceAll(/^\W+|\W+$/, "")
                        name = name.toString().replaceAll(/$removeString/, "").replaceAll(/(?ism),\s*$/, "").trim()
                    } else if ((removeMatcher = name =~ /(?ism)^[^,]+,\s+(?!(?:[Js]r\.|\bI{1,3}\b|\bIV\b|\bV\b))[^,]+,(?!\s+Jr\.|\s+C\.|\s+I{1,3}$)(.*?)$/)) {
                        // } else if ((removeMatcher = name =~ /(?ism)^[^,]+,\s+(?!(?:[Js]r\.|\bI{1,3}\b|\bIV\b|\bV\b))[^,]+,(?!\s+Jr\.|\s+I{1,3}$)(.*?)$/)) {
                        removeString = removeMatcher[0][1].toString().replaceAll(/^\W+|\W+$/, "")
                        name = name.toString().replaceAll(/$removeString/, "").replaceAll(/(?ism),\s*$/, "").trim()
                    } else {
                        name = name.toString().replaceAll(/&\s*$/, "")
                    }
                }
                if (name.toString().contains("/")) {
                    if ((aliasMatcher = name =~ /(?ism)\/(.*?)$/)) {
                        alias = aliasMatcher[0][1]
                        name = name.toString().replaceAll(/$alias/, "").replaceAll(/\//, "").trim()
                    }
                    if (alias) {
                        aliasList.add(alias)
                    }
                }
                if ((aliasMatcher = name =~ /(?ism)Inc\.\s+(craig &.*)/)) {
                    alias = aliasMatcher[0][1]
                    name = name.toString().replaceAll(/$alias/, "").trim()
                    if (alias) {
                        aliasList.add(alias)
                    }
                }
                if (name =~ /(?i)white, jimm/) {
                    name = name.toString().replaceAll(/White, Jimmy, C\./, "Jimmy C. White")
                }
                name = name.toString().replaceAll(/(?is)Westmoreland\)|^\W+/, "").trim()
                name = sanitizeName(name)
                createEntity(name, eventDate, occupation2, aliasList, address)
            }
        }
    }


    def formatName(String name) {
        def fNameMatcher, ext = ""
        //
        // println("FORMAT: "+context.formatName(name))
        name = name.replaceAll(/(?s),\s*$/, "").trim()
        if (name =~ /(?i)(?:\b[js]r\b|\bI{1,3}\b|\bIV\b)\W*$/) {
            ext = name.replaceAll(/(?i)^(.*?)(\,\s*)(\b[js]r\b|\bI{1,3}\b|\bIV\b)(\W*)$/, '$2$3$4')
            name = name.replaceAll(/$ext/, "")//
        }

        if (name =~ /(?im)^([^,]+),([^,]+)$/) {
            //printlln("here2")
            name = name.toString().replaceAll(/(?im)^([^,]+),([^,]+)$/, '$2 $1')

        }
        if (name =~ /(?im)^([^,]+),([^,]+),([^,]+)$/) {
            name = name.replaceAll(/(?im)^([^,]+),([^,]+),([^,]+)$/, '$3 $1 $2')
        }
        if (name =~ /(?i)(?:\b[js]r\b|\bI{1,3}\b|\bIV\b|\bV\b)/) {
            if ((fNameMatcher = name =~ /(?im)^([^,]+),([^,]+),\s*(\b[js]r\b\.|\bI{1,3}\b|\bIV\b|\bV\b),*$/)) {
                name = name.toString().replaceAll(/(?im)^([^,]+),([^,]+),\s*(\b[js]r\b\.|\bI{1,3}\b|\bIV\b|\bV\b),*$/, '$2 $1, $3')
            } else if ((fNameMatcher = name =~ /(?im)(^.+?,\s+(?:\bJr\b\.|\bI{1,3}\b|\bIV\b|\bV\b)),(.+?)$/)) {
                name = name.toString().replaceAll(/,\s*$/, "")
                name = name.toString().replaceAll(/(?im)(^.+?,\s+(?:\bJr\b\.|\bI{1,3}\b|\bIV\b|\bV\b)),(.+?)$/, '$2 $1')
            } else if ((fNameMatcher = name =~ /(?im)^([^,]+),(?!\s+Jr\.|\s+I{1,3}$)(.*?)$/)) {
                name = name.toString().replaceAll(/(?im)^([^,]+),(?!\s+Jr\.|\s+I{1,3}$)(.*?)$/, '$2 $1')
            }
        } else if (name =~ /,/) {
            //printlln("Here")
            name = name.toString().replaceAll(/(?im)^([^,]+),([^,]+)$/, '$2 $1')
        }
        name = name.trim() + ext
        //
        return name.replaceAll(/(?s)\s+/, " ").replaceAll(/\s+Jr$/, ", Jr")
    }

    def sanitizeAlias(def data) {
        data = data.toString().replaceAll(/(?s)\s+/, " ").replaceAll(/^\s*&/, "")
            .replaceAll("&amp;", "&")
            .replaceAll("&apos;", "'")
            .replaceAll(/^(Jr\.?\s+|II+\s+)(.+?)$/, '$2, $1')
            .replaceAll(/(?is),\s*$/, "").trim()
        return data
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        return street
    }

    def fixOccupation(def otherName) {
        def occOther, occMatcher2
        if ((occMatcher2 = otherName =~ /(?im)^.*?,.*?,\s+(?!(?:[Js]r\.|I{1,3}|IV|V|[A-Z]\.))(.*?)$/)) {
            occOther = occMatcher2[0][1]
        } else if ((occMatcher2 = otherName =~ /(?ism)^[^,]+,[^,]+,(?!\s+Jr\.|\s+I{1,3}$)(.*?)$/)) {
            occOther = occMatcher2[0][1]
        } else if (otherName =~ /(?i)Home Health Agency/) {
            return
        } else if ((occMatcher2 = otherName =~ /(?ism)^[^,]+,\s(home.*?agency)$/)) {
            occOther = occMatcher2[0][1]
        }
        return occOther
    }

    def occSanitize(def data) {
        data = data.toString().replaceAll(/(?i)\bMD\b/, "")
            .replaceAll(/(?ism),\s*$/, "")
            .replaceAll(/(?ism),(?=\s+w\.)/, "")
            .replaceAll(/(?i)Thompson, Jr., Jimmy Homer/, "Thompson, Jimmy Homer, Jr.")
            .replaceAll(/(?i) M\.*D\.*,/, "")
            .replaceAll(/(?i)Pharmacist/, "").trim()
        return data
    }

    def pdfToTextConverter(def pdfUrl) {
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
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def sanitizeName(def data) {
        data = data.toString().replaceAll(/(?i)\sRN,*\s*/, "")
            .replaceAll(/(?i)DMD|MLC|Medicare|Medicaid|MFCU\s*&*|Pharmacy Board/, "")
            .replaceAll(/(?i)\)\s*Pharmacy/, "), Pharmacy")
            .replaceAll(/(?i)\sRN\s*&*\s*/, "")
            .replaceAll(/(?i)LPN,*/, "")
            .replaceAll(/(?i)\s*RN\s*&\s*LPN /, "")
            .replaceAll(/(?i)RN\s*\/\s*LPN$/, "")
            .replaceAll(/(?i)\bCRNP\b/, "")
            .replaceAll(/(?i)RN $ CRNA/, "")
            .replaceAll(/(?i) LPN & RN/, "")
            .replaceAll(/(?i),* DO\b,*/, "")
            .replaceAll(/(?i) PC,*/, "")
            .replaceAll(/(?i)\sLPN\s*&*\s*/, "")
            .replaceAll(/(?i),*&\s*ARNP|&\s*$/, "")
            .replaceAll(/(?s)\s+/, " ")
            .replaceAll(/\/$|^\W+|\,\s*$/, "")
            .replaceAll(/(?i)\,\s*(LLC\W*|Inc\W*)/, ', $1')
        return data.trim()
    }

    def sanitizeData(def data) {
        data = data.toString().replaceAll(/(?ism)\A.*?(?=Phys)/, "")
            .replaceAll(/(?ism)Note.*?\Z/, "")
            .replaceAll("\\u000c", "")
            .replaceAll(/(?ism)^\s{50,}([^\n]+)\n^((?:\S+\s)+)(\s{2,}\d+\/\d+\/\d+\s{2,})([^\n]+\n)/, { def a, b, c, d, e -> c + d + b + ' ' + e })
            .replaceAll(/\r\n/, "\n")
            .replaceAll(/(?ism)LLC\s{5,}\(Patrick E\.\nIfediba, MD, Owner\)/, "LLC (Patrick E. Ifediba, MD, Owner)")
            .replaceAll(/(?ism)Medicaid, MFCU, &\s*Medicare/, "Medicaid, MFCU, & Medicare")

        return data
    }

    def isFutureDate(String date) {
        def currentDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date())
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy")

        return simpleDateFormat.parse(currentDate).before(simpleDateFormat.parse(date))
    }

    def rowFixing(def data) {
        data = data.toString().replaceAll(/(?ism)^((?:Children's Den|Adams, John|Gieger Transfer|Advantage Medical|Med Care Ren|Baker, Je|Clavert, Kathy|Chance, Mer|Skeels, Ka|Harden, La|Hawkins, Chris|Huerta, Christie Lee,|Johnson, Christie L.,|Johnson, Tchernavia M.|McClernon, Kath|Plowman, Am|Smith, Chris|Waters-South|Physicians Pain|Bradford-Port|Giddens, Tracy|Home Buddies).+?(?:ad, Montgomery,|dba Global|\(Ambulance|, & Marcus|Medical Equipment|, Jessica Ann|McClernon, &|, Meredith Shay|, Lauren Danielle|ta, Christie|ins, Christie L. Hawkins, Christie|is & Tchernavia|Clavert, &|and Amanda|and Felicia|er & Xiulu|, Owner of|Lynn Giddens-|Direct Service)).+?((?:Alabama \(Marc|Compounding|Compa|Johnson, O|Medical Ma|Westm|Kathy Gayle E|Gas|Womack,|L. Johnson,|Lee Huerta, &|McKinnis J|Gail D|Monique W|Ruan,|Counseling|Cannon and|Provider).+?(?:y\)|er\)|ers\)|Cannon\)|armacy|Counselor|nson|L. Smith\)|hurst\)|p\)|land\)|L. Johnson\)|CEO\)|nn\)))(\s*)(\d\d\/\d\d\/\d\d)/, '$1 $2     $4')
        data = data.toString().replaceAll(/(?ism)^((?:Northside Phar|Elder, Kathy|Care Compl|Owens, Beni|Collins, Mit).+?(?:Jeremy Adams,|rt, & Kathy|Ifediba,|\/Alabama|& Billy Jean)).+?((?:Owner|Gayle|MD, O|Angels,|Col).+?(?:CEO\)|cClernon\)|ner\)|Inc\.|ins\)))(\s*)(\d\d\/\d\d\/\d\d)/, '$1 $2     $4')
        data = data.toString().replaceAll(/(?ism)^(Covenant, Medical.+?Enterprises, Inc\.,).+?(Owner\))(\s*)(\d\d\\/\d\d\\/\d\d)/, '$1 $2     $4')
        data = data.toString().replaceAll(/(?ism)^(Cooper, Cass.+?, Cassandra Dawn).+?(Sapp\))(\s*)(\d\d\\/\d\d\\/\d\d)/, '$1 $2     $4')
        data = data.toString().replaceAll(/(?ism)^(Stewart, Ma.+?& Markiesha Latoya).+?(Stewart\))(\s*)(\d\d\\/\d\d\\/\d\d)/, '$1 $2     $4')
        data = data.toString().replaceAll(/(?ism)^(Advanced Healthcare.+?D, Birmingham,).+?(Alabama)(\s*)(\d\d\\/\d\d\\/\d\d)/, '$1 $2     $4')
        data = data.toString().replaceAll(/(?ism)^(Robertson, Stacey Nobles, Nursing.+?RN, LPN, and).+?(NA)(\s*)(\d\d\\/\d\d\\/\d\d)/, '$1 $2     $4')
        data = data.toString().replaceAll(/(?ism)^(Henry, William E.+?ment Services Co\.;).+?(Radiology Technician,.+?, & State).+?(Repre.+?)(\s*)(\d\d\\/\d\d\\/\d\d)/, '$1 $2 $3     $5')
        data = data.toString().replaceAll(/(?ism)^(Gervais, Isabell.+?Doctor of).+?(Holistic.+?and Doctor of).+?(Naturopathic Me.+?Debra Lynn).+?(Goodman, D.+?arr, Debrah).+?(Goodman-Starr,.+?)(\s*)(\d\d\\/\d\d\\/\d\d)/, '$1 $2 $3 $4 $5     $7')
        return data
    }
}