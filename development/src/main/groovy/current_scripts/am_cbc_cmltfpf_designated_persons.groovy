package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.*

import com.rdc.rdcmodel.model.RelationshipType

// Developer site

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Am_Cbc_Cmltfpf_Designated_Persons script = new Am_Cbc_Cmltfpf_Designated_Persons(context)
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

class Am_Cbc_Cmltfpf_Designated_Persons {
    final addressParser
    final entityType
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context

    def url = 'https://www.cba.am/Storage/EN/FDK/20201210%20-%20National%20Designations%20List_eng.pdf'


    Am_Cbc_Cmltfpf_Designated_Persons(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser.updateCities([SY: ["Deir ez-Zor", "Al-Hajar al-Aswad"]])

        this.context = context
    }

    def initParsing() {
        def pdfText = pdfToTextConverter(url)
        handleBlock(pdfText)
        setDefaultAddress()
    }

    def handleBlock(def pdfText) {

        pdfText = pdfText.toString().replaceAll(/(?s)^.*?\(Identifier - “DI”\)/, '')
        pdfText = pdfText.toString().replaceAll(/\s+B\. Entities \(Identifier.*/, '\n')
        pdfText = pdfText.toString().replaceAll(/(?<=30 October 2020\.)\s+(?=DE-001)/, "\n")
        pdfText = pdfText.toString().replaceAll(/(?s)\s+Notice:.*/, "\n")

        def blockMatcher = pdfText =~ /(?s)([A-Z]{2}-\d{3}:.*?)(?=[A-Z]{2}-\d{3}:|\Z)/

        def block

        while (blockMatcher.find()) {
            block = blockMatcher.group(1)
            captureData(block)
        }
    }

    def captureData(def block) {

        block = sanitizeBlock(block)

        def entityName
        def alias
        def dateOfBirth
        def address
        def address1
        def nationality
        def identification1
        def identification2
        def remark
        def eventDate
        def aliasList = []
        def aliasList1 = []
        def addressList = []
        def identificationList = []

        def entityNameMatcher = block =~ /(?s)Name:(.*?)(?=Title:|AKA:)/

        if (entityNameMatcher.find()) {
            entityName = entityNameMatcher.group(1)
            entityName = entityName.toString().replaceAll(/\s+/, " ")


            (entityName, aliasList) = handleAlias(entityName)
            entityName = entityName.toString().replaceAll(/[;`\)\(]|^‘/, '').trim()

        }

        def aliasMatcher = block =~ /(?s)AKA:(.*)(?=Address:)/

        if (aliasMatcher.find()) {
            alias = aliasMatcher.group(1)
            alias = alias.toString().replaceAll(/\s+/, " ")
            alias = alias.toString().replaceAll(/\(\S+\s(Hay’at Tahrir al-Sham|Levantine Front)/, '$1 z)')
            alias = alias.toString().replaceAll(/(?<=The Victory Front|Fath al Sham)|\(formerly called:|-e\)/, ' z)')
            alias = alias.toString().replaceAll(/(?<=Al|Assembly) ;\(\S+ (?=Nusrah|for the)/, ' ')

            if (alias) {
                if (alias =~ /[a-z]\)/) {
                    aliasList1 = alias.toString().split(/\s[a-z]\s*[\);]/).collect({ it.trim() })
                } else {
                    aliasList1.add(alias)
                }
            }

            aliasList1.each { it ->
                it = it.toString().replaceAll(/[\(\);]/, "")
                it = it.toString().replaceAll(/\s+/, " ")
                aliasList.add(it)
            }
        }

        def dobMatcher = block =~ /(?s)Date\s+of\s+Birth:(.*?)(?=Place[\s\S]+of[\S\s]+Birth:)/
        if (dobMatcher.find()) {
            dateOfBirth = dobMatcher.group(1)
            dateOfBirth = dateOfBirth.toString().replaceAll(/\s+/, ' ').trim()
            dateOfBirth = dateOfBirth.toString().replaceAll(/NA;|NA\s;?\(\S+.*/, '').trim()
            dateOfBirth = dateOfBirth.toString().replaceAll(/;/, '').trim()
        }

        def addressMatcher = block =~ /(?s)(?:Address:)((?!NA).*?)(?:Listed\s+on:)/
        if (addressMatcher.find()) {
            address = addressMatcher.group(1)
            address = address.toString().replaceAll(/\s+/, " ")
            address = address.toString().replaceAll(/NA;/, "").trim()

            if (address) {
                if (address =~ /[a-z]\)/) {
                    addressList = address.toString().split(/\s[a-z]\s*[\);]/).collect({ it.trim() })
                } else {
                    addressList.add(address)
                }
            }
        }

        def address1Matcher = block =~ /(?is)Place[\S\s]+Of[\S\s]+Birth:(.*?)(?=Nationality:)/
        if (address1Matcher.find()) {
            address1 = address1Matcher.group(1)
            address1 = address1.toString().replaceAll(/\s+/, " ")
            address1 = address1.toString().replaceAll(/NA;/, "").trim()

            if (address1) {
                addressList.add(address1)
            }
        }

        def nationalityMatcher = block =~ /(?s)Nationality:(.*?)(?=Passport\s+no:)/
        if (nationalityMatcher.find()) {
            nationality = nationalityMatcher.group(1)
        }

        def identificationMatcher = block =~ /(?is)Passport\s+no:(.*?)National\s+identification\s+No:(.*?)Address:/

        if (identificationMatcher.find()) {
            identification1 = identificationMatcher.group(1)
            identification1 = identification1.toString().replaceAll(/NA;/, '')

            identification2 = identificationMatcher.group(2)
            identification2 = identification2.toString().replaceAll(/NA;/, '')

            identificationList.add(identification1)
            identificationList.add(identification2)

        }

        def eventDateMatcher = block =~ /Listed\s+on:\s(\d\s[A-Z][a-z]+\s\d{2,4})/
        if (eventDateMatcher.find()) {
            eventDate = eventDateMatcher.group(1).trim()
        }

        def remarkMatcher = block =~ /(?s)Other\s+information:(.*?)\Z/

        if (remarkMatcher.find()) {
            remark = remarkMatcher.group(1).trim()
            remark = remark.toString().replaceAll(/\s+/, ' ')
            remark = remark.toString().replaceAll(/NA., (?:17|16)/, ' ')
            remark = remark.toString().replaceAll(/NA[\.;]|\([A-Z]{2,4}-\s*\d{2,4}\)/, '')
        }
        createEntity(entityName, aliasList, eventDate, dateOfBirth, addressList, nationality, identificationList, remark)

    }

    def nameList = [] as List

    def createEntity(def name, def aliasList, def eventDate, def dateOfBirth, def addressList, def nationality, def identificationList, def remark) {

        def entity = null

        ScrapeIdentification identification = new ScrapeIdentification()

        if (!name.toString().equals("") || !name.toString().isEmpty()) {
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
                nameList.add(name)
            }

            nationality = commonSanitize(nationality)

            if (!nationality.toString().isEmpty()) {
                entity.addNationality(nationality)
            }

            identificationList.each { identity ->
                identity = identity.toString().replaceAll(/null/, '').trim()

                if (!identity.toString().isEmpty()) {
                    identification.setValue(identity)
                    entity.addIdentification(identification)
                }

            }

            remark = commonSanitize(remark)
            remark = remark.replaceAll(/\s+/, " ")
            remark = remark.replaceAll(/, (?:18|21)/, "")
            if (!remark.toString().isEmpty()) {
                entity.addRemark(remark)
            }

            dateOfBirth = commonSanitize(dateOfBirth)
            if (!dateOfBirth.toString().isEmpty()) {
                dateOfBirth = context.parseDate(new StringSource(dateOfBirth))
                entity.addDateOfBirth(dateOfBirth)
            }


            def aliasEntityType
            aliasList.each {
                it = sanitizeAlias(it)
                if (it) {
                    aliasEntityType = detectEntity(it)
                    if (aliasEntityType.toString().equals(entityType)) {
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
                            addCommonPartOfEntity(newEntity, addressList, eventDate)
                        }
                    }
                }
            }
            addCommonPartOfEntity(entity, addressList, eventDate)
        }
    }

    def addCommonPartOfEntity(def entity, def addressList, def eventDate) {

        def description = 'This entity appears on the Armenia Central Bank Committee on Combatting Money Laundering, Terrorism Financing and Proliferation Financing published list of designated persons pursuant to the United Nations Security Council Resolution 1373.'


        addressList.each { address ->

            address = sanitizeAddress(address)
            if (!address.equals("")) {

                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress)
                }
            }
        }
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(description)

        if (!eventDate.toString().isEmpty() || !eventDate.toString().equals("null")) {
            eventDate = context.parseDate(new StringSource(eventDate))
            event.setDate(eventDate)
        }
        entity.addEvent(event)
    }

    def detectEntity(def name) {
        def type
        if (name =~ /^(?:Usama|Azouv|Bisharah|Bizarah|Bzarah|Deek|Hebron|Jamal|Kabi|Al-Janubi|Abdu-l-Qader|لهله)$/) {
            type = "P"
        } else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$)/) {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:Arms|as-Shahna)$/) {
                type = "P"
            }
        }
        return type
    }

    def setDefaultAddress() {
        def entity = null
        nameList.each {
            entity = context.findEntity("name": it)
            if (entity.addresses.isEmpty()) {
                ScrapeAddress scrapeAddress = new ScrapeAddress()
                scrapeAddress.setCountry("ARMENIA")
                entity.addAddress(scrapeAddress)
            }
        }
    }

    private String commonSanitize(def text) {
        text = text.toString().replaceAll(/NA;/, '').trim()
        text = text.toString().replaceAll(/null/, '').trim()
        return text.trim()
    }

    def handleAlias(def entityName) {
        def aliasList = []
        def alias
        def aliasMatcher
        if (!entityName.toString().isEmpty()) {
            if (entityName =~ /\//) {
                aliasList = entityName.split(/\//).collect({
                    it.toString().trim()
                })
                entityName = aliasList[0]
                aliasList.remove(0)
            }
            if ((aliasMatcher = entityName =~ /(?i)\(([a-z\S \-]+)[\)]/)) {
                alias = aliasMatcher[0][1]
                aliasList.add(alias)

                entityName = entityName.toString().replaceAll(/$alias/, "")
            }
        }
        return [entityName, aliasList]
    }

    def sanitizeAlias(def alias) {
        alias = alias.toString().replaceAll(/[\(\);]/, "")
        alias = alias.toString().replaceAll(/\s+/, " ")

        return alias.trim()
    }


    def sanitizeBlock(def block) {

        block = block.toString().replaceAll(/(\(Shaukat Mahmud\)) (\([\S\s]+\))/, '/$1/$2').trim()
        block = block.toString().replaceAll(/(?<=Khaled Ali Shrshi|al-Razak|Abdullatif Hasani|al-Sharkia|Al-Furqan Brigade|Levant) (\([\S\s]+)/, '/$1').trim()
        block = block.toString().replaceAll(/(?<=Nuri Abu Ali|Ahmad Lahlah|Nadim Bariq|Levant Front|Osama Hasan Husein) ([\)\(][\S\s]+)/, '/$1').trim()
        block = block.toString().replaceAll(/(?<=Mohammad Shukhti|Abdel Mouti|al-Rahmouni|Aref Shirtik|At-Taybani|(?:Nidal|Nizar|Mohammad) Shariba) [\)\(][\S\s]+(?=Title:)/, '').trim()
        block = block.toString().replaceAll(/(?<=Syrian\s);\s*[\)\(][\S\s]+(?=Arab)|(?<=Abdullatif Hasani)\/\(\s\S|(?<=deceased\.),\s[0-9]{2,4}/, '').trim()
        block = block.toString().replaceAll(/(?<=Date of|Date| Birth: 09) ;\(\S.*|(?<=identification no: NA;), 19|(?<=Nationality: NA;), [0-9]/, '').trim()
        block = block.toString().replaceAll(/(?<=Mother – )(\s*\w+\.),\s*[0-9]{2,4}/, '$1').trim()
        return block.toString()
    }

    def sanitizeAddress(def address) {

        address = address.toString().trim()
        address = address.toString().replaceAll(/(?<=Republic);, 9|(?:State of|Suburb of) (?=Damascus|Libya)|Republic of\s(?=Iraq)/, '').trim()
        address = address.toString().replaceAll(/\((?:Support network|Operates in)\)/, '').trim()
        address = address.toString().replaceAll(/^[a-z]\)/, '').trim()
        address = address.toString().replaceAll(/^(.*)$/, ', $1').trim()
        address = address.toString().replaceAll(/;$/, '').trim()
        return address.toString()
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        return street
    }

    def pdfToTextConverter(def pdfUrl) {

        List<String> pages = ocrReader.getText(pdfUrl)
        return pages

    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

}