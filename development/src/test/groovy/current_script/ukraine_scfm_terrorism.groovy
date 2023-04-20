package current_script

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification
import org.apache.commons.lang.StringUtils
import scrapian_scripts.utils.GenericAddressParserFactory

import java.util.concurrent.ThreadPoolExecutor

/**
 * Date: 13/09/2018
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

UkraineScfm script = new UkraineScfm(context)
script.initParsing()

class UkraineScfm {
    //most recent git-revision no that is related to this script; default value: "HEAD"
    final def moduleFactory = ModuleLoader.getFactory("519ae4bb0ae45b0152e0eaefe281525b60ebaa1e")
    final ScrapianContext context
    final addressParser

    final String url = "http://www.sdfm.gov.ua/content/file/Site_docs/2010/07.10.2010/BlackListEng183.pdf"

    UkraineScfm(context) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
        def xlsLocation = GenericAddressParserFactory.getScriptLocation(GenericAddressParserFactory.fileLocation, "standard_addresses", null)
        addressParser = GenericAddressParserFactory.getGenericAddressParser(context, xlsLocation)
        addressParser.updateCities([RU: ["Grozny", "Makhachkala", "Nazran"],
                                    RW: ["Kigali", "Ngoma"],
                                    YE: ["Mukalla", "Sanaa"],
                                    UZ: ["Tashkent"],
                                    CA: ["Ottawa"],
                                    IT: ["Casalbuttano", "Giovanni", "Naples", "Carvano"],
                                    UG: ["entebbe"],
                                    GB: ["Birmingham", "Midlands", "Manchester"],
                                    PH: ["Jolo Sulu", "Zamboanga"],
                                    XK: ["Orahovac"],
                                    LY: ["Gdabia", "Benghazi", "Bengasi", "al Aziziyya"],
                                    KP: ["Pyongyang"],
                                    ID: ["Pacitan", "Jombang", "Lamongan", "East Lombok", "Pemalang", "Kudus", "Cianjur", "Serang", "Dacusuman Surakarta"],
                                    EG: ["Beni-Suef", "Kafr Al-Shaykh", "El Behira", "Dhliya", "Minya", "Qaylubiyah"],
                                    CD: ["Butembo"],
                                    PK: ["Sargodha", "Lahore", "Quetta", "Chaman"],
                                    AF: ["Ghazni", "Konduz", "Qandahar", "Panjwae", "Keshim", "Bagrami", "Suroobi", "Taliqan", "Qarabajh", "Daman", "Adehrawood village", "Arghistan", "Shawali Kott", "Spinboldak", "Dehrawood", "Arghandaab", "Zurmat", "Pashtoon Zarghoon", "Kandahar", "Darzab", "Shinwar", "Shawalikott"],
                                    SO: ["Galgala", "Badhan", "Hargeysa", "Kismaayo", "Mogadishu", "Bossaso"],
                                    SA: ["Al-Taif", "Buraydah", "Jeddah", "Madinah", " Al-Medinah", "Medina", "Oneiza", "Uneizah"],
                                    IQ: ["al-Awja", "Hilla", "Kairkuk", "Yousufia", "Khan Dari", "heet", "Baiji", "Jeddah", "Tikrit", "Babylon", "Dora", "Karrda", "al zubair"],
                                    UK: ["Gloucester"],
                                    IR: ["Tehran"],
                                    MA: ["Marrakesh", "Essaouria", "fez"],
                                    DZ: ["Algiers", "Medea", "Bordj Kiffane", "oum bouaghi", "Tighennif", "Meftah", "Touggourt", "Chrea", "Anser", "Bologhine", "Rouiba", "Algeri", "ghardaia", "Faidh El Batma"],
                                    SD: ["Al-Bawgah"],
                                    PH: ["Atimonana", "Siasi", "Zamboanga", "Bindoy"],
                                    GQ: ["Malabo"],
                                    BE: ["Schaerbeek", "Etterbeek=/(?i)(?<!\\d\\s)\\betterbeek\\b(?!\\s*\\d+)/"],
                                    SY: ["Zabadani", "Bludan"],
                                    DE: ["Ulm", "Neunkirchen", "Beckum"],
                                    JO: ["Al-Zarqaa", "Amman"],
                                    BA: ["Sarajevo", "Zenica"],
                                    BH: ["Muharraq"],
                                    GE: ["tbilisi", "Duisi"],
                                    SE: ["Kista"], TZ: ["Zanzibar"], KM: ["Moroni"], YE: ["Sadah"], QA: ["Doha"],
                                    MY: ["MuarJohor", "Klang", "Tiram"],
                                    FR: ["Roubaix"],
                                    CG: ["Rutshuru"],
                                    TN: ["Ben Aoun", "beja", " Hekaima Al- Mehdiya", "Tunisi", "Feriana", "Al-Mohamedia", "ghardimaou", "Menzel Jemil", "Menzel Temime", "Baja", "Tataouene", "Koubellat", "Menzel Temine", "Menzel Bouzelfa", "Tabarka", "Bizerta"]])
        addressParser.updateStates([RU: ["Chechnya", "Daghestan", "Ingushetia"],
                                    AF: ["Uruzgan", "Kandahar", "Ningarhar", "Konar"],
                                    PH: ["Tulay", "Tawi Tawi"],
                                    CG: ["Ituri"], MY: ["selang"]])
    }

//------------------------------Initial part----------------------//
    def initParsing() {
        def text = pdfToTextConverter(url)
        def block
        def blockMatch = text =~ /(?is)number\sin(.*?)\n(?=number\sin)/
        while (blockMatch.find()) {
            block = blockMatch.group(1)
            block = block.toString().replaceAll(/(?i)(;)(?:(?=\spostal\saddress|\sjakarta|\smogadishu))/, ",").replaceAll(/(?i)\s(?=Dob:)/, ";").replaceAll(/(?i)(?<=expressway);/, ", USA;")
                    .replaceAll(/(?-i)\bOR\s+(?=\d+)/, "Oregon ").replaceAll(/(?-i)\bMO\s+(?=\d+)/, "Missouri ").replaceAll(/(?i)ugandan/, "uganda")
                    .replaceAll(/(?i)address[^:]+: (saudi);/, "\$1 Saudi Arabia").replaceAll(/(?i)birth\\/ location place[^:]+: (iraqi)/, "\$1 Iraq").replaceAll(/(?i)Afghanhistan/, "Afghanistan")
            handleDetailsPage(block)
        }
    }

    def handleDetailsPage(srcText) {
        def nameList = []
        def name
        def addressList = []
        def aliasList = []
        def tempName
        def address
        def dateOfBirth
        def nationality
        def identity
        def remark
        def nameMatch = srcText =~ /(?is)name\s*1\s*[^:]+:([^;]+;(?:\sname\s*2\s*[^:]+:[^;]+;)?(?:\sname\s*3\s*[^:]+:\s*[^;]+;)?)/
        while (nameMatch.find()) {
            name = nameMatch.group(1)
            name = nameSanitize(name)
            name = StringUtils.removeStart(name, ".").trim()
            name = StringUtils.removeEnd(name, ",").trim()
            if (name =~ /(?-i)\bAKA/) {
                name = name.toString().replaceAll(/(?-i)\b-AKA|AKA/, "").trim()
            }
            if (!name.isEmpty()) {
                nameList.add(name)
            }

        }
        tempName = nameList.remove(0)
        aliasList = nameList

        def addressMatch = srcText =~ /(?is)(?:Address|Birth\\/ Location Place)\s\(c\d+\):([^;]+)/
        while (addressMatch) {
            address = addressMatch.group(1)
            address = sanitizeAddress(address)
            address = StringUtils.removeStart(address, "-").trim()
            if (!(address =~ /(?i)\bpassport|current|\bas\b|\bbe\b|\bna\b|located|Gold|Ariwara|Ivorian|june|Arthur|POB|Guiberoua|^circa$|^A\s\d+$|^M\s\d+$|^\w+\s\d+$|Property|last|of no\b|Democratic|\bid\b|^previous$|^\d+$|^, \.$|Mehala/)) {
                addressList.add(address)
            }
        }

        def dobMatch = srcText =~ /(?is)date\sof\sbirth\\/\sregistration\s\(c\d+\):([^;]+)/
        while (dobMatch.find()) {
            dateOfBirth = dobMatch.group(1).trim()
            if (dateOfBirth =~ /\d{8}/) {
                dateOfBirth = dateOfBirth.replaceAll(/(\d{4})(\d{2})(\d{2})/, "\$1/\$2/\$3")

            }
        }
        def nationalityMatch = srcText =~ /(?is)nationality\s\(c\d+\):([^;]+)/
        def splitNationality
        while (nationalityMatch.find()) {
            nationality = nationalityMatch.group(1)
            nationality = sanitizeNationality(nationality)
            splitNationality = nationality.split(/,/)
        }
        def identityMatch = srcText =~ /(?i)(?:passport|travel)\s*(?:Number|no|document|substitute)?(?:\s\("Reiseausweise"\))?\.?\:?\s*((?-i:[A-Z]*)\-?\s{0,1}(?:\d{2,})\\/?\w*)/
        while (identityMatch.find()) {
            identity = identityMatch.group(1)
            identity = sanitizeIdentity(identity)
        }
        def remarkMatch = srcText =~ /(?is)extra\sinformation\s\(c\d+\):([^;]+)/
        while (remarkMatch.find()) {
            remark = remarkMatch.group(1)
            remark = sanitizeRemark(remark)
            remark = StringUtils.removeStart(remark, ":").trim()
            remark = StringUtils.removeEnd(remark, ":").trim()
            remark = StringUtils.removeEnd(remark, "%").trim()
        }

        def eventDescription = "This entity appears on the Ukraine State Committee for Financial Monitoring Terrorism list of persons related to commitment of terrorist activity or persons to whom international sanctions were applied."

        createEntity(tempName, aliasList, eventDescription, addressList, dateOfBirth, splitNationality, identity, remark)

    }

    def createEntity(name, aliasList, description, addressList, dob, splitNationality, identity, remark) {
        def entity = null
        def type = detectEntityType(name)
        if (type.equals("O")) {
            entity = context.findEntity("name": name, type: type)
        }
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(type)
            aliasList?.each {
                alias ->
                    if (alias =~ /(?-i)\bAKA/) {
                        alias = alias.toString().replaceAll(/(?-i)\b-AKA|AKA/, "").trim()
                    }
                    if (type.equals("P")) {
                        alias = sanitizeAlias(alias)
                        alias = personNameReformat(alias)
                    }
                    entity.addAlias(alias)
            }

        }

        ScrapeAddress scrapeAddress = new ScrapeAddress()
        def street_sanitizer = { street ->
            return street.replaceAll(/(?s)\s+/, " ").trim()
        }
        if (addressList.size() > 0) {
            addressList.each {
                addr ->

                    if (addr) {
                        def address = addressParser.parseAddress([text: addr, force_country: true])

                        scrapeAddress = addressParser.buildAddress(address, [street_sanitizer: street_sanitizer])
                        if (scrapeAddress) {
                            entity.addAddress(scrapeAddress)
                        }
                    }
            }
        } else {

            scrapeAddress.setCountry("Ukraine")
            entity.addAddress(scrapeAddress)
        }

        ScrapeEvent event = new ScrapeEvent()
        def date = context.parseDate(new StringSource(dob), ["yyyy/MM/dd", "dd MMMM. yyyy", "dd MMM. yyy"] as String[])
        if (date) {
            entity.addDateOfBirth(date)
        }

        event.setDescription(description)
        event.setDate("10/07/2010")
        event.setCategory("WLT")
        event.setSubcategory("SAN")
        if (identity != null) {
            ScrapeIdentification identification = new ScrapeIdentification()
            identification.setType("Passport No")
            identification.setValue(identity)
            entity.addIdentification(identification)
        }

        entity.addEvent(event)
        splitNationality.each {
            if (it != null && !it.toString().isEmpty()) {
                def nationality = it.toString().trim()
                entity.addNationality(nationality)
            }
        }


        if (remark != null) {
            entity.addRemark(remark)
        }

        return entity

    }

    def personNameReformat(name) {
        return name.replaceAll(/(?i)^\s*([^,]+),\s*([^,]+),?(\s*[js]r\.?|\s*I{2,3})?$/, '$2 $1 $3').trim()
    }

    def sanitizeAlias(alias) {
        return alias.toString().replaceAll(/(?i)^dr\.?|^md/, "").trim()
    }

//------------------------------Misc utils part---------------------//
    def invoke(url, isPost = false, isBinary = false, cache = false, postParams = [:], headersMap = [:], cleanSpaceChar = false, tidy = false, miscData = [:]) {
        Map data = [url: url, tidy: tidy, headers: headersMap, cache: cache, clean: cleanSpaceChar]
        data.putAll(miscData)
        return context.invokeBinary(data)
    }

    def pdfToTextConverter(pdfUrl) {
        def pdfFile = invoke(pdfUrl, false, true)

        def pmap = [:] as Map
        pmap.put("1", "-layout")
        pmap.put("2", "-enc")
        pmap.put("3", "UTF-8")
        pmap.put("4", "-eol")
        pmap.put("5", "dos")
        //pmap.put("6", "-raw")

        def pdfText = context.transformPdfToText(pdfFile, pmap)

        return pdfText.toString().replaceAll(/\r\n/, "\n")
    }

    def detectEntityType(name) {
        def type
        if (name =~ /(?i)\b(?:THE|DENTAL|I\.?A\.?\.?C|TRAINING|DES|ISLAMIC|PRODUCTION|TECHNIQUE|ADMINISTRATION|IMPORT|CENTRE|HEADQUARTERS|STATION|COOPERATION|ORIENTAL|INSTITUTE|UNIVERSITY|ENERGY|MUNICIPALITY|EXCHANGE|COMPUTER|COMPANIES|FOUNDATION|TIZ PARS|TIZ PARS|7TH|PROJECTS?|EQUIPMENT|ORGANIZATION|TOUS|BAKERIES|AL-QAIDA|BIF|GENERAL|FONDATION|INDUSTRIAL|HEALTH|HOUSE|ENTERPRISE|ESTABLISHMENT|TELECOMMUNICATIONS|MOVEMENT|AL-ITIHAAD|HARAMA[IY]N|INVESTMENTS|TAIBAH|inc|Hospital|NATIONAL|MANUFACTURING|AL QA'IDA|AL QAQA|DEPARTMENT|ll[pc]|l\.p|DRUGS|al\sqa[ei]da|COMMITTEE|MEDICAL|BANKS?|AIRBAS|LTD|S.A|P\.C|L\.L\.C|\s+&\s+)\b/) {
            return "O"

        } else {
            type = context.determineEntityType(name)
        }

        return type
    }

    def nameSanitize(name) {
        return name.toString().replaceAll(/(?i)name\s2\s\(c7\):/, "").replaceAll(/(?i)name\s3\s\(c8\):/, "").replaceAll(/(?i)^\\./, "")
                .replaceAll(/;/, "").replaceAll(/(?s)\s+/, " ").replaceAll(/\n/, "")
                .replaceAll(/(?s)\s+/, " ")
                .replaceAll(/&quot;/, "").toUpperCase().trim()

    }

    def sanitizeIdentity(identity) {
        return identity.toString().replaceAll(/\n/, "").trim()
    }

    def sanitizeRemark(remark) {

        return remark.toString().replaceAll(/(?is)\\s+/, " ").replaceAll(/\n/, "").trim()
    }

    def sanitizeAddress(address) {
        return address.toString().replaceAll(/(?i)al-mukallah?/, "Mukalla").replaceAll(/(?i)al-owja/, "Al-Awja")
                .replaceAll(/\(.*?\)/, "").replaceAll(/\b(?i)a\.?k\.?A\.?:?\b/, "").replaceAll(/(?is),\s*ai-\s*mukala/, "")
                .replaceAll(/(?i)\bfirst flo\b|\bcity\b|\bOffice\b|\bthe\b|\bCentral\b|\bpost\b|\bjava\b|\bEl\b|\bfirst floor\b/, "")
                .replaceAll(/(?i)Branch Address:/, "").replaceAll(/(?i)al-Madinah/, "Madinah")
                .replaceAll(/(?i)\bdistrict|province|near|suburb of|either|\bor\b/, "").replaceAll(/(?i)In prison in|b\)|(?i)chamber[^;]+|wilaya\s*(?:\([^\)]+\)\s*)?of|union of/, "")
                .replaceAll(/(?s)\s+/, " ").trim()
    }

    def sanitizeNationality(nationality) {
        return nationality.toString().replaceAll(/\n/, "").replaceAll(/(?s)\s+/, " ")
                .replaceAll(/(?is)passport[^$]+|nationality:/, "").trim()
    }
}