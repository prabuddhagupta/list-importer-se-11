package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification


context.setup([connectionTimeout: 10000, socketTimeout: 10000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

ArMojTerrorismList script = new ArMojTerrorismList(context)
script.initParsing()

class ArMojTerrorismList {

    final addressParser
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context

    def headerUrl = 'https://www.salinecountysheriff.com/'
    def mainUrl = 'https://repet.jus.gob.ar/'

    ArMojTerrorismList(context) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.updateCities([IR: ["Aligodarz", "Najafabad", "Isfahan"], LB: [" Kfar Fila"]])
    }

    def initParsing() {
        def html
        html = invokeUrl(mainUrl)
        html = html.toString().replaceAll(/(?s)^.*<a name="personas"><\/a>Personas\s*<\/h2>/, "")
        html = html.toString().replaceAll(/(?s)(?s)<h2><a name="entidades"><\\/a>Entidades<\\/h2>.*$/, "")

        def block
        def blockMatcher = html =~ /(?s)(<div class="hidden service-item-tags\">.*?<\/div>\s<\/div>)\s*(?=<div class="col-xs-12|<\/div>)/

        while (blockMatcher.find()) {
            block = blockMatcher.group(1)
            if (!(block =~ /Fuente\:\sUN/)) {
                handleBlockData(block)
            }
        }
    }

    def handleBlockData(def blockData) {
        def entityName
        def entityNameMatcher
        if ((entityNameMatcher = blockData =~ /<h5>(.*)<\/h5>/)) {
            entityName = entityNameMatcher[0][1].toString().trim()
        }

        def gender
        def genderMatcher
        if ((genderMatcher = blockData =~ /(?i)Género:\s?([A-Z]+)\s?</)) {
            gender = genderMatcher[0][1]
        }

        def alias
        def aliasList = [] as List
        def aliasMatcher
        aliasMatcher = blockData =~ /(?i)Alias:\s?([A-Za-z ]+)\s?</
        while (aliasMatcher.find()) {
            alias = aliasMatcher.group(1)
            aliasList.add(alias)
        }

        def dob
        def dobList = [] as List
        def dobMatcher

        if ((dobMatcher = blockData =~ /(?s)<li>Fecha de Nacimiento.*?Año:\s*(\d+)\s*</)) {
            dob = dobMatcher.group(1)
            dobList.add(dob)
        } else {
            dobMatcher = blockData =~ /(?i)Fecha:\s*(\d+\/\d+\/\d+)\s*</
            while (dobMatcher.find()) {
                dob = dobMatcher.group(1)
                dobList.add(dob)
            }
        }

        def eventDate
        def eventDateMatcher
        if ((eventDateMatcher = blockData =~ /Alta:\s(.*\d)\s</)) {
            eventDate = eventDateMatcher[0][1]
        }
        def position
        def positionMatcher
        if ((positionMatcher = blockData =~ /Cargo.*\s.*\s*<li>(.+)</)) {
            position = positionMatcher[0][1]
        }

        def identification
        def identificationList = [] as List
        def identificationMatcher = blockData =~ /(?i)Tipo:\sPasaporte.*\s.*Número:\s*([A-Z0-9]+)\s*</
        while (identificationMatcher.find()) {
            identification = identificationMatcher.group(1)
            identificationList.add(identification)
        }

        blockData = blockData.toString().replaceAll(/(?i)Tipo:\sPasaporte.*\s.*Número:\s*[A-Z0-9]+\s*</, "")

        def remark
        def remarkList = [] as List
        def remarkMatcher = blockData =~ /(?i)Número:\s*([A-Z0-9]+)\s*</
        while (remarkMatcher.find()) {
            remark = remarkMatcher.group(1)
            remarkList.add(remark)
        }

        def addressList = [] as List
        def address
        def addressMatcher
        if ((addressMatcher = blockData =~ /(?s)(Lugar de Nacimiento:.*?)(?=Última Actualización)/)) {
            address = addressMatcher[0][1]
            addressList = handleAddress(address)
        }
        createEntity(entityName, aliasList, dobList, gender, eventDate, remarkList, identificationList, addressList, position)
    }

    def handleAddress(def addressBlock) {

        def addressList = [] as List
        def address
        def addressMatcher

        if ((addressMatcher = addressBlock =~ /Ciudad:(.*?)<\/li>\s.*Provincia:(.*)<\/li>\s<li>País:(.*)<\/li>/)) {
            address = addressMatcher[0][1].toString().trim() + ", " + addressMatcher[0][2].toString().trim() + ", " + addressMatcher[0][3].toString().trim()
            addressList.add(address)
        } else {
            addressMatcher = addressBlock =~ /Ciudad:(.*?)<\/li>\s.*?País:(.*)</
            while (addressMatcher.find()) {
                address = addressMatcher.group(1).toString().trim() + ", " + addressMatcher.group(2).toString().trim()
                addressList.add(address)
            }
        }
        return addressList
    }

    def createEntity(entityName, aliasList, dobList, gender, eventDate, remarkList, identificationList, addressList, position) {

        def eventDescription = "This entity appears on the Argentina Ministry of Justice Public Registry of Persons and Entities linked to acts of terrorism and financing of terrorism."

        def entity = null

        ScrapeAddress scrapeAddress = new ScrapeAddress()
        ScrapeEvent event = new ScrapeEvent()
        ScrapeIdentification identification = new ScrapeIdentification()

        if (!entityName.toString().isEmpty()) {
            entityName = entityName.toString().replaceAll(/\s+/, " ").trim()
            entityName = entityName.toString().replaceAll(/í/, "i").trim()
            entity = context.findEntity(["name": entityName, "type": "P"])
            if (!entity){
                entity = context.getSession().newEntity()
                entity.setName(entityName)
                entity.setType("P")
            }

            if (eventDate) {
                eventDate = context.parseDate(new StringSource(eventDate), ["dd/MM/yyyy"] as String[])
                event.setDate(eventDate)
            }
            if (eventDescription) {
                event.setDescription(eventDescription)
                entity.addEvent(event)
            }
            dobList.each { dob ->
                if (!dob.toString().isEmpty()) {
                    if (dob =~ /^\d{4}$/) {
                        def dateOfBirth = context.parseDate(new StringSource(dob), ["yyyy"] as String[])
                        if (dateOfBirth) {
                            entity.addDateOfBirth(dateOfBirth)
                        }
                    } else {
                        def dateOfBirth = context.parseDate(new StringSource(dob), ["dd/MM/yyyy"] as String[])
                        if (dateOfBirth) {
                            entity.addDateOfBirth(dateOfBirth)
                        }
                    }
                }
            }
            aliasList.each {
                it = it.toString().replaceAll(/\s+/, " ").trim()
                if (!it.toString().contains("null")) {
                    entity.addAlias(it)
                }
            }
            remarkList.each {
                if (!it.toString().contains("null")) {
                    entity.addRemark(it)
                }
            }

            if (!position.toString().contains("null")) {
                position = position.toString().replaceAll(/\s+/, " ").trim()
                entity.addPosition(position)
            }

            identificationList.each {
                if (!it.toString().contains("null")) {
                    identification.setType("Passport Number")
                    identification.setValue(it)
                    entity.addIdentification(identification)
                }
            }

            if (gender) {
                entity.addSex(gender)
            }

            addressList.each { address ->
                if (!address.toString().isEmpty()) {
                    address = sanitizeAddress(address)
                    def addrMap = addressParser.parseAddress([text: address, force_country: true])
                    scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                    entity.addAddress(scrapeAddress)
                }
            }
        }
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().replaceAll(/(?s)\s+/, " ").trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        street = street.toString().replaceAll(/\(/, "").trim()
        return street
    }

    def sanitizeAddress(def address) {
        address = address.toString().trim()
        address = address.toString().replaceAll(/,$/, "")
        address = address.toString().replaceAll(/Libano/, "Lebanon")
        address = address.toString().replaceAll(/Isfahán/, "Isfahan")

        return address.trim()
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = true, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

}
