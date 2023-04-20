package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification

//Date: 04/11/19 dd/mm/yy

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

UkraineSanctionProgram script = new UkraineSanctionProgram(context)
script.initParsing()

class UkraineSanctionProgram {

    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
//eef7c8953b3554121a3e9f88f18d3d0edcc328cb")
    final ScrapianContext context
    final addressParser
    def tokens

    final String url = "http://www.sdfm.gov.ua/content/file/Site_docs/Black_list/BlackListFullEng.pdf"

    UkraineSanctionProgram(context) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
//    addressParser.reloadData()
        addressParser.updateCities([IT: ["Casalbuttano", "Giovanni", "Carvano", "Mortara PV"],
                                    LY: ["al Aziziyya", "Garabulli", "Jalo", "Zawiya", "AlBayda", "Doma", "Benghazi"],
                                    ID: ["Pacitan", "East Lombok", "Kudus", "Demak", "Cianjur", "Dacusuman Surakarta", "Lombok", "Tulungagung", "Sumedang", "Sukoharjo", "Solo"],
                                    EG: ["Beni-Suef", "Kafr Al-Shaykh", "Qaylubiyah", "Sharqiyah"],
                                    CD: ["Rutshuru", "Bouadriko", "Garabulli", "Bibwe", "Ariwara", "Mambasa", "Minembwe", "Kabylie", "Walikale", "Haut-Uolo", "Bas-Uolo"],
                                    PK: ["Waziristan", "Ganj District"],
                                    AF: ["Dehrawood", "Lalpura", "Nahr-e Saraj", "Nad-e-Ali", "Baghlan", "Jaldak wa Tarnak", "Pul-e- Khumri or Jadid District", "Lashkar Gah"],
                                    SA: ["al-Duwadmi", "Saqra", "Al Baraka", "Medina"],
                                    IQ: ["Khan Dari", "heet", "Babylon", "Za'Faraniya", "Eastern Karrda", "Al-Khalis", "Karh", "Karkh", "Tall 'Afar", "Al-Dur", "Al-Qaim=/(?i)Al-\\s*Qaim/", "Poshok", "Dujail", "Al Hassaka", "Hashmiya District", "Dora", "tikrit",
                                         "Salah al- Din", "Sulaymaniyah", "Daura", "Hartha District", "Al-Masbah", "Nassiryah", "Al-Doura", "Milla", "Al-Hurriya"],
                                    DZ: ["Meftah", "Faidh El Batma", "Anser", "Kef Rih"],
                                    SY: ["Bludan", "Qalamun"],
                                    DE: ["Beckum", "Haselunne", "Langen", "Hauzenberg"],
                                    KM: ["Moroni"],
                                    CI: ["Gagnoa", "Grand- Bassam", "Bouadriko", "Bohi"],
                                    TN: ["Ben Aoun", "Nabul", "ghardimaou", "Menzel Jemil=/(?i)Menzel\\s*Jemil/", "Manzil Tmim", "Manzal Tmim", "Tataouene", "Tabarka", "Bizerta", "Ben Guerdane"],
                                    RW: ["Rubavu", "Gaseke", "Bigogwe", "Kinyami", "Butera", "Ndusu"],
                                    SS: ["Yirol", "Yei", "Malualkon", "Bor", "Mayom", "Akobo"],
                                    YE: ["Dahyan", "Sadah"],
                                    KE: ["Lamu Island", "Dadaab"],
                                    QA: ["Al-Laqtah"],
                                    CF: ["Ndele", "Boda", "Bria", "Nana-Grebizi", "Birao", "Mbaiki"],
                                    PH: ["Tuburan", "Patikul", "Punta", "Atimonana"],
                                    FR: ["Ploemeur", "Normandy", "Bourg la Reine"],
                                    ML: ["Abeibara", "Al Moustarat", "Tabankort", "Elkhalil"],
                                    UG: ["Kampala"],
                                    GE: ["Akhmeta"],
                                    BA: ["Kiseljak", "Travnick"],
                                    RU: ["Ust-Dzheguta", "Iki-Burulskiy"],
                                    KP: ["Musan", "Hyongjesan-Guyok", "Mangyongdae-guyok", "Mangungdae-gu"],
                                    XK: ["Kacanik", "Kaqanik"],
                                    CH: ["Le Paquier-Montbarry"],
                                    UK: ["Heath"],
                                    TT: ["Mount Hope"],
                                    TJ: ["Varzob"],
                                    EH: ["Laayoune"],
                                    KW: ["Al-Khitan", "Warah"],
                                    SD: ["Kabkabiya", "Tine", "El-Fasher"],
                                    TR: ["Reyhanli"]
        ])
        addressParser.updateStates([RU: ["Ingushetia", "Karachayevo-Cherkessia", "Tatarstan", "Kalmykia"],
                                    CG: ["Ituri"],
                                    AF: ["Konar"],
                                    RW: ["Mudende"],
                                    IQ: ["Ninawa", "Saladin", "Babylon"],
                                    SS: ["Lakes State"],
                                    MA: ["Negri Sembilan"],
                                    KP: ["Kangdong", "Pyongyang"],
                                    SY: ["Lattakia"],
                                    CD: ["Ituri"],
                                    MY: ["Negri Sembilan"],
                                    UK: ["Cardiff"],
                                    PH: ["Jolo Island", "Manila", "Kalmykia"]
        ])
        addressParser.updatePostalCodes([NL: [/(?s)\b\d{4}\s+[A-Z0-9]{2}\b/],
                                         BA: [/\b\d{2}\s\d{3}\b/]])

        tokens = addressParser.getTokens()
    }

    def initParsing() {
        def text = pdfToTextConverter(url)
        text = filterPdfText(text)
        def block
        def blockMatch = text =~ /(?ism)(Number(.*?(?=Number\s*in|\Z)))/
        while (blockMatch.find()) {
            block = blockMatch.group(1)
            handleDetailsPage(block)
        }
    }

    def filterPdfText(text) {
        text = text.replaceAll(/\r\n/, "\n")
        text = text.replaceAll(/(?smi)^\s*\d+$/, "")
        text = text.replaceAll(/(?ims)^\s+#\d{1,}.*?\d{1,}\,\s*\d{4}\n*$/, "")
        text = text.replaceAll(/(?ims)^\s*$/, "")
        text = text.replaceAll(/(?ims)file:([^\n]+)\s*.*?\d{3}\s*$/, "")
        text = text.replaceAll(/(?ims)^list.*?(?=number)/, "")
        text = text.replaceAll(/(?is)10\s*jun\. 1982/, "10 jun. 1982")


        return text
    }

    def handleDetailsPage(srcText) {
        def nameList
        def name
        def addressList = []
        def aliasList = []
        def identityList = []
        def dateList = []
        def nationality
        def remark
        def count = 1
        def nameMatch = srcText =~ /(?is)(name\s*\d.*?(?=Possible))/

        while (nameMatch.find()) {
            name = nameMatch.group(1)

            def nameFullMatch = name =~ /:(.*?[^;]+)/
            def nameFull = ""

            while (nameFullMatch.find()) {
                nameFull = nameFull + " " + nameFullMatch.group(1)
            }
            name = sanitizeName(nameFull)

            if (!((name =~ /(?m)^\s*[^\w]+\s*$/) || (name =~ /(?i)\b(?:39)\b/) || (name =~ /(?i)\b(dateofissue.*)\b/))) {
                if (count == 1)
                    nameList = name
                else if (!(name =~ /(?i)\((?:russian|original).*?[^\)]+\)/))
                    aliasList.add(name)
            }
            count++
        }

        if (nameList) {
            def type
            def typeMatch = srcText =~ /(?i)type[^;]+(\d)\s*;/
            if (typeMatch.find()) {
                def etype = typeMatch[0][1]
                if (etype.equals("2"))
                    type = "P"
                else if (etype.equals("1"))
                    type = "O"
            }
            def addressData
            def addressMatch = srcText =~ /(?is)(?:Address|Birth\\/ Location Place)\s\(c\d+\):([^;]+)/
            while (addressMatch) {
                addressData = sanitizeAddress(addressMatch.group(1))
                def prison = addressData
                if (!(prison =~ /(?i)in\s*prison\b/)) {
                    def passportMatch = addressData =~ /(?i)passport.*/
                    while (passportMatch.find()) {
                        def passpport = passportMatch.group(0)
                        addressData = addressData.replaceAll(/$passpport/, "")
                        def passportNo = passpport =~ /(?:[A-Z]*[-\/\s]*[A-Z]*\s*\d+\/*){5,}\b(?!\))/
                        if (passportNo.find())
                            identityList.add(passportNo.group(0).trim())
                    }

                    def address = addressData.replaceAll(/\(([?-i:A-Z]{2})\)/, { a, b -> return b }).replaceAll(/(?m)\([^\)]+\)/, "").replaceAll(/\bc\)/, ",").replaceAll(/^Branch.*?[^:]:/, "")
                    def dobMatch
                    if ((dobMatch = address =~ /DOB.*?(\d{4})/)) {
                        def date = context.parseDate(new StringSource(dobMatch[0][1]), ["yyyy"] as String[])
                        dateList.add(date)
                    }

                    def nationMatch
                    if ((nationMatch = address =~ /Nationality:(.*)/))
                        nationality = sanitizeNationality(nationMatch[0][1])
                    address = addressFixer(address)
                    address = splitAddress(address)
                    address.split("-SPLIT-").each {
                        it = it.trim()
                        if (it)
                            addressList.add(sanitizeAddress(it))
                    }
                }
            }
            def pobMatch = srcText =~ /(?is)birth\\/\s*location\s*place\s*\(c\d{1,2}\):(.*?)(?=\()/
            def pob
            def pobList
            while (pobMatch.find()) {
                pob = pobMatch.group(1)
                pob = sanitizePob(pob)
            }
            pobList = pob.toString().split(/;/)


            def dob
            def dobMatch = srcText =~ /(?is)date of birth\\/.*?\(c\d+\):\s*(.*?)(?=\(c\d{1,2})/
            while (dobMatch.find()) {
                dob = sanitizeDate(dobMatch.group(1))
                def dateOfBirth = dob.split(/;|or|,|\-/)
                dateOfBirth.each {
                    it = it.trim()
                    it.split(/(?<=\d{4}\b)\s*,/).each {
                        def date = context.parseDate(new StringSource(it), ["dd MMM. yyyy", "MMM. dd yyyy", "MMMM d, yyyy", "dd MMMM. yyyy", "yyyyMMdd", "MMMM dd,yyyy", "ddMMM yyyy", "ddMMM. yyyy", "MMM. yyyy", "dd MMM.yyyy", "dd MMM. yyyy", "dd MMM yyyy", "dd MMMM yyyy", "dd.MM.yyyy", "yyyy/MM/dd", "yyyy", "MMMM dd yyyy"] as String[])
                        dateList.add(date)
                    }
                }
            }

            def nationalityMatch = srcText =~ /(?is)nationality\s\(c\d+\):([^;]+)/
            while (nationalityMatch.find()) {
                def nation = nationalityMatch.group(1).trim()
                if (!(nation =~ /(?m)^\s*[^\w]+\s*$/))
                    nationality = sanitizeNationality(nation)
            }

            def identityMatch
            def dateInMatch
            def identity
            def identityTextMatch = srcText =~ /(?i)Passport No\.\s*\(c\d+\):([^;]+)/
            while (identityTextMatch.find()) {
                identityMatch = identityTextMatch.group(1)
                identity = identityMatch =~ /(?:[A-Z]*[-\/\s]*[A-Z]*\s*\d+\/*){5,}\b(?!\))/
                while (identity.find()) {
                    dateInMatch = identity =~ /(?:0[1-9]|[1-2][0-9]|3[0-1])-(?:0[1-9]|1[0-2])-\d{4}/
                    if (!dateInMatch.find())
                        identityList.add(identity.group(0).trim())
                }
            }

            def remarkMatch = srcText =~ /(?is)extra\sinformation\s\(c\d+\):(.*)/
            while (remarkMatch.find()) {
                remark = remarkMatch.group(1)
                remark = sanitizeRemark(remark)
            }

            def eventDescription = "This entity appears on the Ukraine State Committee for Financial Monitoring UN Sanctions Program list of persons related to commitment of terrorist activity or persons to whom international sanctions were applied."
            def eventDate
            def eventDateMatcher = srcText =~ /(?ism)inclusion date \(C2\):\s(\d+)\;/
            while (eventDateMatcher.find()) {
                eventDate = eventDateMatcher.group(1)
                eventDate = eventDate.toString().replaceAll(/(\d{4})(\d{2})(\d{2})/, '$1/$2/$3')
            }

            createEntity(nameList, type, aliasList, eventDescription, addressList, pobList, dateList, identityList, remark, eventDate, nationality)
        }
    }

    def createEntity(name, type, aliasList, description, addressList, pobList, dateList, identityList, remark, eventDate, nationality) {
        def entity
        if (type.equals("O"))
            entity = context.findEntity([name: name, type: type])

        //remove duplicate entity of same person by using dob and address
        if (type.equals("P")) {
            if (dateList)
                entity = context.findEntity([name: name, type: type, dob: dateList[0]])
        }

        if (!entity) {
            def addrKey = [name, addressList[0]]
            entity = context.findEntity(addrKey)
            if (!entity) {
                entity = context.newEntity(addrKey)
                if (type.equals("P"))
                    name = personNameReFormat(name)

                //feedback 13/12/18 (abbreviation) should be as alias except countries in parentheses
                def countryName = /KENYA|INDONESIA|PAKISTAN|UNION OF THE COMOROS|TANZANIA|SOMALIA/
                def nameAliasMatch = name =~ /(?i)\((?!$countryName)(?:.*)\)$|\/(?:[^$]+)$/
                while (nameAliasMatch.find()) {
                    def nameAlias = nameAliasMatch.group(0)
                    aliasList.add(sanitizeNameAlias(nameAlias))
                    name = name.replaceAll(/\(?$nameAlias\)?/, "")
                }

                entity.setName(name)
                entity.setType(type)
                aliasList?.each {
                    alias ->
                        if (alias) {
                            if (type.equals("P"))
                                alias = personNameReFormat(alias)
                            entity.addAlias(alias)
                        }
                }
            }
        }

        ScrapeAddress scrapeAddress = new ScrapeAddress()
        def street_sanitizer = { street ->
            fixStreet(street)
        }


        if (addressList) {
            addressList.each {
                addr ->

                    if (addr) {
                        def address = addressParser.parseAddress([text: addr, force_country: true, ignored_cities: getIgnoredCitiesTokens()])
                        if (!(address[tokens.CITY]) && address[tokens.STATE] == "Baghdad") {
                            address[tokens.CITY] = "Baghdad"
                            address[tokens.STATE] = ""
                        }
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
        if (pobList) {
            pobList.each { placeofBirth ->
                if ((placeofBirth =~ /\w+/)) {
                    def addrMap = addressParser.parseAddress([text: placeofBirth])
                    ScrapeAddress addressObj = addressParser.buildAddress(addrMap)
                    if (addressObj) {
                        addressObj.setBirthPlace(true)
                        entity.addAddress(addressObj)
                    }
                }

            }
        }


        if (dateList.size() > 0) {
            dateList.each {
                date ->
                    if (date)
                        entity.addDateOfBirth(date)
            }

        }

        ScrapeIdentification identification = new ScrapeIdentification()
        identityList.each {
            identity ->
                if (identity) {
                    identification.setType("Passport No")
                    identity = identity.replaceAll(/(?s)\s+/, " ")
                    identification.setValue(identity)
                    entity.addIdentification(identification)
                }
        }

        if (nationality)
            entity.addNationality(nationality)

        if (remark)
            entity.addRemark(remark)

        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(description)
        if (eventDate) {
            eventDate = context.parseDate(new StringSource(eventDate), ["yyyy/MM/dd"] as String[])
            event.setDate(eventDate)
        }
        event.setCategory("WLT")
        event.setSubcategory("SAN")
        entity.addEvent(event)

        return entity

    }

    def fixStreet(street) {
        return street.replaceAll(/or Baghlan/, "Baghlan")
            .replaceAll(/(?i)\bCity|Kandahar|Ninevah|Iraq|Sudan|Al-Khalis|Nassiriyah|Baghdad|Soldier\b/, "")
            .replaceAll(/(?i)Welling,\s*of\b/, "").replaceAll(/(?i)Pul-e- Khumri District/, "")
            .replaceAll(/(?i)(?:Coted`Ivoire|\bor$|(?:Reportedly located|in custody|Believed to be)\s*in)/, "")
            .replaceAll(/(?i)(?<=village)\s*,\s*district/, "")
            .replaceAll(/(?i)Kunduz\s*District\s*$/, "")
            .replaceAll(/(?i)^\s*(?:,|district|of|Alt|Region|East|Lower|Southeast|area|County)\s*$/, "")
            .replaceAll(/(?i)^\s*(?:district|state|South|Northern|Governate|territory|region|Support network in)\s*$/, "")
            .replaceAll(/(?i)^,\s*/, "")
            .replaceAll(/(?i),\s*(?:State|north|Governorate)\s*$/, "")
            .replaceAll(/^\s*\d+\s*$/, "")
            .replaceAll(/(?i)^Yemen,\s*AI-\s*Mukala$/, "")
            .replaceAll(/\/\s*,\s*Northern$/, "")
            .replaceAll(/(?i)\s*(?:Union of the|the|Republic of|P\.?O\.?\s*Box|Commune\s*,\s*Prefecture)\s*$/, "")
            .replaceAll(/^\s*\((.(?!\)))+\s*$/, "")
            .replaceAll(/,?\s*\(.*?\.be\b/, "")
            .replaceAll(/(?i),\s*Lahore.*?District$/, "")
            .replaceAll(/(?i)^\s*Reported address.*?expelled\sfrom$/, "")
            .replaceAll(/(?i)Also\sreported.*?2007$/, "")
            .replaceAll(/(?i)^since.*?\d{4}\s*$/, "")
            .replaceAll(/(?i)\s*active.*?\d{4}\s*$/, "")
            .replaceAll(/(?i)\b(?:Listed|Governorate)\b\s*$/, "")
            .replaceAll(/-\s*$/, "")
            .replaceAll(/,\s*$/, "")
            .replaceAll(/(?s)\s+/, " ").trim()
    }

    def addressFixer(def address) {
        address = address.replaceAll(/(?:DOB|Telephone).*/, "").replaceAll(/(?i)al-Awja,?\s*Near\s*Tikrit/, "tikrit").replaceAll(/(?is)reported.*?(?<=nov)\.\d{4}/, "SOMALIA").replaceAll(/(?i)Reportedly/, "").replaceAll(/(?i)\bEstimated to be in\b/, "").replaceAll(/El Gharbia/, "Gharbia")
        address = address.replaceAll(/(?i)(?:Previous|Approximately|Province|circa|suburb\s*of|near Steel Bridge,|(?:or)?\s*near|Wilaya\s*of|\bna\b|\bOperates in\b)/, "")
        address = address.replaceAll(/and Jebel Akhdar/, "Jebel Akhdar").replaceAll(/As Sulaymaniyah/, "Sulaymaniyah").replaceAll(/(?i)rakrang.*?(?=chilgol)/, "").replaceAll(/(?i)(?<=1-Dong)\s*(?=Pyongchon)/, ",")
        address = address.replaceAll(/North Hamgyo`ng/, "North Hamgyong").replaceAll(/Salah Eldin/, "Salah al- Din").replaceAll(/(?i)jalan.*?district,/, "").replaceAll(/(?i)(?<=karachayevo)-\s*(?=Cherkessia)/, "-")
        address = address.replaceAll(/Za'Faraniya, Industrial/, "Za'Faraniya").replaceAll(/\bIn\sKhalil\b/, "Elkhalil").replaceAll(/(?i)entrance\s(?:to|of)/, "")
        address = address.replaceAll(/House #179/, "House #179,").replaceAll(/Salah Eldin/, "Saladin").replaceAll(/(?i)c\/o\sAlfa.*marketing,/, "").replaceAll(/(?i)c\\/o\strading.*?jordan/, "").replaceAll(/(?is)salah aldin.*awja/, "al-awja")
        address = address.replaceAll(/International Airport, General Street\b/, "House #179,").replaceAll(/Pakistan,\s*Phone\s*\d+/, "Pakistan,")
        address = address.replaceAll(/CV\s+Maastricht/, "CV,Maastricht").replaceAll(/\balmykia\b/, "Kalmykia").replaceAll(/(?i)Jonglei State/, "Jonglei").replaceAll(/(?i) Lattakia Governorate/, " Lattakia")
        address = address.replaceAll(/(?i)stichting.*?nederland/, "").replaceAll(/(?i)\.\s*chamber.*?77/, "").replaceAll(/(?i)located\s*in\b/, "")
        address = address.replaceAll(/(?:either\s*Mosul or|Guiberoua or Niagbrahio\/ Guiberoua or)/, "")
        return address
    }

    def splitAddress(def addr) {
        addr = addr.replaceAll(/(?i)\b(?:\w{1,3}\b\)|(?<=Afghanistan)\s*,\s*(?=\bin\b)|\band\b\s+(?=northern)|-\s+(?=C\/o))/, "-SPLIT-")
        addr = addr.replaceAll(/(?<=Syria)\s*.*\s*(?=(and the\s*)?Arabian Peninsula)/, "-SPLIT-")
        addr = addr.replaceAll(/(?i)\s*(?<=Lebanon),\s*(?=Syria)/, "-SPLIT-")
        addr = addr.replaceAll(/(?<=Kuwait)\s*.*\s*(?=Qatar)/, "-SPLIT-")
        addr = addr.replaceAll(/(?i)(?<=Saudi Arabia)\s*.*\s*(?=Kuwait)/, "-SPLIT-")
        addr = addr.replaceAll(/,\s*or\s*/, "-SPLIT-")

        return addr
    }

    def getIgnoredCitiesTokens() {
        def tokens = [/P\W*O\W+Box/, /post/, /Governorate/, /Northern/, /City/, /Terrace/, /South/, /bridge/, /area/, /prefecture/, /Steel/, /floor/, /\s*Al-Awja\s*/, /sudan/, /house/, /Wazir Akbar Khan/, /Avenue/, /(?i)\bSaemul\s*1-Dong\b/, /Airport/, /Sq/, /Street/, /Richardson/, /\bthe\b/, /Region/, /Worth/, /road/, /Soldier/, /Karrda/, /Camp Sarah Khatoon/, /Square/, /Listed/]

        return tokens
    }

    def personNameReFormat(name) {
        def nameSwapMatch = name =~ /(.*?),(.*)/
        if (nameSwapMatch.find())
            name = nameSwapMatch.group(2) + " " + nameSwapMatch.group(1)
        return name.trim()
    }

    def sanitizeName(name) {
        return name.replaceAll(/(?i)\((?:original|dob|previously|formerly|\d+).*\)/, "").replaceAll(/,\s*$/, "").replaceAll(/(?i)\s*born.*/, "").replaceAll(/(?s)\s+/, " ").trim()
    }

    def sanitizeRemark(remark) {
        return remark.replaceAll(/\u0451/, "e").replaceAll(/,$/, "").replaceAll(/(?is)\s+/, " ").replaceAll(/\n/, "").trim()
    }

    def sanitizeDate(def dateOfBirth) {
        dateOfBirth = dateOfBirth.toString().replaceAll(/\n/, "")
            .replaceAll(/(?i)\b(?:Approximately|born|Circa|between.*and|c[\s.]*)\b/, "")
            .replaceAll(/(?is);\s*(?:Birth\\/\s*Location\s*Place|Nationality|Passport No\.?|Address|Citizenship|Identif\.?\s*number|Appointment|Extra\s*information|340\s*address)/, "")
            .replaceAll(/(?i)\b[\s,]*or\b/, ",")
            .replaceAll(/\(.*?\)/, "")
            .replaceAll(/239\u000CPassport No./, "")
            .replaceAll(/166\u000CPassport No./, "")
            .replaceAll(/\b(\d{4})(\d{2})(\d{2})\b/, "\$1/\$2/\$3")
            .replaceAll(/(\d{4})\/(\d{4})/, "\$1,\$2")
            .replaceAll(/(?i)sept\./, "sep").trim()

        return dateOfBirth
    }

    def sanitizeAddress(data) {
        return data.replaceAll(/&amp;/, '&').replaceAll(/&quot;/, "\"").replaceAll(/;/, " ").replaceAll(/(?i)(?:N\\/A|Unknown)/, "").replaceAll(/\n/, " ").replaceAll(/(?s)\s{2,}/, " ").trim()
    }

    def sanitizeNationality(nationality) {
        return nationality.replaceAll(/\n/, "").replaceAll(/(?is)passport[^$]+/, "").replaceAll(/(?s)\s+/, " ").trim()
    }

    def sanitizeNameAlias(nameAlias) {
        return nameAlias.replaceAll(/\(|\)|\//, "").replaceAll(/,\s*$/, "").trim()
    }

    def pdfToTextConverter(pdfUrl) {
        def pdfFile = invoke(pdfUrl, false, true)
        def pmap = [:] as Map
        pmap.put("1", "-layout")
        pmap.put("2", "-enc")
        pmap.put("3", "UTF-8")
        pmap.put("4", "-eol")
        pmap.put("5", "dos")

        def pdfText = context.transformPdfToText(pdfFile, pmap)
        return pdfText.toString().replaceAll(/\r\n/, "\n").replaceAll("\\u000a,", " ")
    }

    def invoke(url, cache = false, postParams = [:], headersMap = [:], cleanSpaceChar = false, tidy = false, miscData = [:]) {
        Map data = [url: url, tidy: tidy, headers: headersMap, cache: cache, clean: cleanSpaceChar]
        data.putAll(miscData)
        return context.invokeBinary(data)
    }

    def sanitizePob(data) {
        return data.toString().replaceAll(/(?is);\s*(?:Nationality|Passport No\.?|\d{1,3}\s*Address|Address|Citizenship|Identif\.?\s*number|Appointment|Extra\s*information|340\s*address)/, "").replaceAll(/(?s)\s+/, " ").trim()
    }
}