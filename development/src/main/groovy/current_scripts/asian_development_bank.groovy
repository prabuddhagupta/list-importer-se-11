package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils

import java.util.regex.Pattern

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36"])
//context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

AsianBank asianBank = new AsianBank(context)
asianBank.initParsing()

class AsianBank {
    final entityType
    final addressParser
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context
    def root = 'http://lnadbg4.adb.org'
    // def mainUrl = root + '/oga0009p.nsf/sancALL3P?OpenView&count=999'
    def mainUrl = "http://lnadbg4.adb.org/oga0009p.nsf/sancALL1P?OpenView&count=999"

    AsianBank(ScrapianContext context) {

        entityType = moduleFactory.getEntityTypeDetection(context)
//    entityType.addTokensintoFile(new File("/home/badrul/Apps/RDCScrapper/assets/config/orgTokensCleaned.txt"), [ "Asociacion"])
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
//    addressParser.reloadData()
//    addressParser.updateCountries([ES: ["Spain"]])
        addressParser.updateCities([
            NL: ["BEESD"],
            CN: ["LONGKOU"],
            CN: ["Quezaltenango"],
            BO: ["SAN PEDRO"],
            CN: ["Ji'an","Wendeng"]
        ])
    }
    def row = 0
    //------------------------------Initial part----------------------//
    def initParsing() {
        def sourceHtml = context.invoke([url: mainUrl, cache: false])
        def pageMatcher = sourceHtml =~ /(?i)Previous Page[^\"]+href="([^>]+)"/
        def pageLink = "", prev = ""
        def table = fixHtml(sourceHtml)
        getData(table)
        while (pageMatcher.find()) {
            pageLink = root + pageMatcher.group(1)
            pageLink = pageLink.replaceAll(/&amp;/, "&")
            if (pageLink == prev) {
                break
            }
            sourceHtml = context.invoke([url: pageLink, cache: false])
            table = fixHtml(sourceHtml)
            getData(table)
            pageMatcher = sourceHtml =~ /(?i)Previous Page[^\"]+href="([^>]+)"/
            prev = pageLink
        }


    }

    def getData(def sourceHtml) {

        def blockMatcher = sourceHtml =~ /(?si)<tr[^>]+><td>(?:.+?)<\/td>(.+?)<\/tr>/
        while (blockMatcher.find()) {
            row++
            def entityName, descr, date, address, remark, alias, remarks
            def blockData = blockMatcher.group(1).replaceAll(/(?smi)\A<td><img.+?src="\/icons\/ecblank.gif".+?<\\/td>/, "")
                .replaceAll(/(?smi)<td><img.+?src="\/icons\/ecblank.gif".+?<\\/td>/, "")
            def entityDetails
            // if ((entityDetails = blockData =~ /(?smi)<td>[^>]+>(.+?)<\\/font><\\/td>(?:<td>[^>]+>(.+?)<\\/font><\\/td>)?<td>(?:.+?)<\\/td>(?:<td>(?:.+?)<\\/td>)?<td>(?:.+?)<\\/td><td>[^>]+>(.+?)<\\/font><\\/td><td>[^>]+>(.+)<\\/font>/)) {
            if ((entityDetails = blockData =~ /(?smi)<td>[^>]+>(.+?)<\/font><\/td>(?:<td>[^>]+>(.+?)<\/font><\/td>)?(?:<td>(?:.+?)<\/td>)?(?:<td>(?:.+?)<\/td>)?(?:<td>(?:.+?)?<\/td>)(?:<td>[^>]+>(.+?)<\/font>)?<\/td><td>[^>]+>(.+)<\/font>/)) {

                entityName = sanitizeName(entityDetails[0][1])
                address = sanitizeAddress(fixUnicode(entityDetails[0][2].toString()
                    .replaceAll(/address unknown/, "")
                    .replaceAll(/n\/a/, "")
                    .replaceAll(/Asturias Espa.a/, "Asturias Espain")
                    .replaceAll(/(?i)\s*PRC$/, "People's Republic of CHINA")
                    .replaceAll(/(\d+)(United)/, "\$1, \$2")
                    .replaceAll(/Espain/, "Spain")
                    .replaceAll(/DHAKA-1209/, "DHAKA,1209")
                    .replaceAll(/AZ1010, Baku/, "AZ1010 Baku")
                    .replaceAll(/(?s)\bTIN\b.+/, "")
                    .replaceAll(/(?s)\bOffice Addresses:\s*/, "")
                    .replaceAll(/(?si)CERCADO DE LIMA\s*(?<!Peru)/, "CERCADO DE, LIMA")

                ))

                date = entityDetails[0][3].toString().replaceAll(/(?i)<br>/, "").trim()
                descr = fixUnicode(entityDetails[0][4].toString().replaceAll(/(?i)<br>|\n/, " ").trim())
                descr = descr.toString().replaceAll(/<td>.*?">/, "")
                descr = descr.replaceAll(/&#8217;/, "\'")
            }

            if (!(entityName =~ /(\((?:\"?\w+\-?\"?)\)$)|\(\w+.?.?\s*\w+.?\)$/)) {
                def aliasMatcher
                if ((aliasMatcher = entityName =~ /\([^\)]+\)\)?$/)) {
                    def aliasData = aliasMatcher[0]
                    entityName = entityName.replaceAll(Pattern.quote(aliasData), "")

                    def remarkMatcher
                    if ((remarkMatcher = aliasData =~ /\((.+?\d{2,}.+?\)?)\)/)) {
                        remark = remarkMatcher[0][1].trim()
                        if (remark.contains("also known as")) {
                            def remarkSplit = remark.split(",")
                            aliasData = remarkSplit[0]
                            remark = remarkSplit[1].trim()
                        }
                    } else {
                        alias = sanitizeAlias(aliasData)
                    }
                } else if ((aliasMatcher = entityName =~ /(?i)\((reg\..+)/)) {
                    remark = aliasMatcher[0][1].toString().replaceAll(/\*\s*$/, "")
                    entityName = entityName.replaceAll(Pattern.quote(remark), "")
                }
            }
            def aliasList = entityName.split(/(?i)(?:\b(?:f.?n.?a.?)\b|\bFORMERLY\b|ALSO|\bdba\b|\b((?:a.?|f.?)k.?a.?)|((?:also|formerly)\s+known\s+as)\b)/).collect({ its ->
                return its
            })

            entityName = aliasList[0]
            aliasList.remove(0)
            def remarkMatcher
            if ((remarkMatcher = entityName =~ /(?i)(?:\((reg\..+))|(?i)\(((?=n°)[^\)]+)\)$|(?i)\((Bangladeshi.+?)\)$/)) {
                if (remarkMatcher[0][1]) {
                    remarks = remarkMatcher[0][1].toString().replaceAll(/(?s)\s+/, " ").trim()
                    entityName = entityName.toString().replaceAll(Pattern.quote(remarkMatcher[0][1].replaceAll(/(?s)\s+/, " ").trim()), "")
                }
                if (remarkMatcher[0][2]) {
                    remarks = remarkMatcher[0][2].toString().replaceAll(/(?s)\s+/, " ").trim()
                    entityName = entityName.toString().replaceAll(Pattern.quote(remarkMatcher[0][2].replaceAll(/(?s)\s+/, " ").trim()), "")
                }
                if (remarkMatcher[0][3]) {
                    remarks = remarkMatcher[0][3].toString().replaceAll(/(?s)\s+/, " ").trim()
                    entityName = entityName.toString().replaceAll(Pattern.quote(remarkMatcher[0][3].replaceAll(/(?s)\s+/, " ").trim()), "")
                }
            }
            def aliasMatcher
            if ((aliasMatcher = entityName =~ /(?i)\(((?!\b\w{2}\b\)|n°|RUSSIA)[^\)]+)\)\s*$/)) {
                aliasList.add(aliasMatcher[0][1].toString().replaceAll(/(?s)\s+/, " "))
                entityName = entityName.toString().replaceAll(Pattern.quote("(" + aliasMatcher[0][1] + ")".replaceAll(/(?s)\s+/, " ")), "")
            }

            entityName = sanitizeName(entityName)
            (date, descr) = sanitizeDateAndDescr(entityName, date, descr)
            entityName = entityName.split(/;/)
            entityName.each { name ->
                def al = name.replaceAll(/^(.+?\()([^\)]+)$/, '$2')
                name = name.replaceAll(/\([^\)]+$/, "").trim()
                aliasList.add(al)
                createEntity(name, descr, address, date, remark, aliasList, alias, remarks)
                aliasList.clear()
            }

        }
    }

    def createEntity(name, descr, address, date, remark, aliasList, alias, remarks) {
        def entity, startDate, endDate, type
        def fixEntityName = fixUnicode(name).replaceAll(/Zeng, Xianglin/, "Xianglin Zeng").trim()

        type = detectEntity(fixEntityName)

        if (name != fixEntityName) {
            if (type == 'P') {
                aliasList.add(personNameReformat(name))
            } else {
                aliasList.add(name)
            }
            name = fixEntityName
        }
        if (type == 'P') {
            name = personNameReformat(name)
            name = name.replaceAll(/^(.+?)(,)(.+?)$/, '$3 $1').trim()
        }

        if (StringUtils.isBlank(name) || name == "and") {
            return
        }
        entity = context.findEntity(["name": name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(type)
        }

        if (remarks) {
            entity.addRemark(remarks)
        }

        ScrapeEvent event = new ScrapeEvent()
        def dateList = date.toString().split(/\|/).collect {
            it = it.replaceAll(/(?i)Non ADB.*/, "")
            if (it.replaceAll(/\s+/, " ").trim().size() > 1)
                return it.replaceAll(/\s+/, " ").trim()
        }

        if (dateList.size() > 1) {
            endDate = dateList[1].trim()
        }
        if (dateList.size() >= 1)
            if (dateList[0] != null)
                startDate = dateList[0].trim()

        if (startDate) {
            def sDate = context.parseDate(new StringSource(startDate), ["dd/MMM/yyyy"] as String[])
            event.setDate(sDate)
        }
        if (endDate != 'No Date') {
            def eDate = context.parseDate(new StringSource(endDate), ["dd/MMM/yyyy"] as String[])
            event.setEndDate(eDate)
        }
        descr = descr.replaceAll(/(?s)\s+/, " ").trim()
        if (descr) {
            event.description = "The Asian Development Bank (ADB) declares firms and individuals ineligible to participate in ADB-financed activity for committing fraudulent or corrupt acts as defined by ADB's Anticorruption Policy. The entity in this record appears on the ADB's sanctioned/debarred entities list. Grounds: $descr"
        } else {
            event.description = "The Asian Development Bank (ADB) declares firms and individuals ineligible to participate in ADB-financed activity for committing fraudulent or corrupt acts as defined by ADB's Anticorruption Policy. The entity in this record appears on the ADB's sanctioned/debarred entities list."
        }
        event.description = event.description.replaceAll(/(?s)\s+/, " ").trim()
        entity.addEvent(event)


        if (name.contains("Ðoàn Anh") || name.contains("Hoang Mai Construction Import and Export Joint Stock")) {
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            scrapeAddress.setAddress1("c/o Hoang Mai Construction Import and Export Joint Stock Company, Xom Bo, Commune Thanh Liet, District Thanh Tri")
            scrapeAddress.setCity("Hanoi")
            scrapeAddress.setCountry("VIETNAM")

            entity.addAddress(scrapeAddress)

            ScrapeAddress scrapeAddress2 = new ScrapeAddress()
            scrapeAddress2.setAddress1("Room 3016, Building B, Vinaconex Building Nguyen Xien (Phong 3016 Toa B Vinaconex 2 Nguyen Xien), Dai Kim Ward, Hoang Mai District (Phyung Dai Kim, Quan Hoang Mai)")
            scrapeAddress2.setPostalCode("100000")
            scrapeAddress2.setCity("Hanoi")
            scrapeAddress2.setCountry("VIETNAM")

            entity.addAddress(scrapeAddress2)

            ScrapeAddress scrapeAddress3 = new ScrapeAddress()
            scrapeAddress3.setAddress1("No. 14, TT6B Urban West Nam Linh Dam, Hoang Mai (So 14, TT6B Khu do thi Tay Nam Linh Dam, Hoang Mai)")
            scrapeAddress3.setPostalCode("100000")
            scrapeAddress3.setCity("Hanoi")
            scrapeAddress3.setCountry("VIETNAM")

            entity.addAddress(scrapeAddress3)

            ScrapeAddress scrapeAddress4 = new ScrapeAddress()

            scrapeAddress4.setAddress1("No. 2/20, Lane 255, Hope Street(So 2/20, Ngo 255, Pho Vong) Dong Tam Ward, Hai Ba Trung District")
            scrapeAddress4.setCity("Hanoi")
            scrapeAddress4.setCountry("VIETNAM")

            entity.addAddress(scrapeAddress4)

        } else if (address && address != "null") {
            address = sanitizeAddress(address)
            def remarkMatcher
            if ((remarkMatcher = address =~ /(?is)(Registration No\.\s*\d+)|\((Reg\. No\..+?)\)/)) {
                def remarkData = remarkMatcher[0][1]
                if (!remarkData) {
                    remarkData = remarkMatcher[0][2].toString().replaceAll(/\*\s*/, "")
                }
                if (remarkData) {
                    entity.addRemark(remarkData.trim())
                }
                address = address.toString().replaceAll(Pattern.quote(remarkData), "").replaceAll(/\(\)/, "").trim()
            }
            if ((remarkMatcher = address =~ /(?is)\((ID No\..+?)\)/)) {
                def remarkData = remarkMatcher[0][1]
                if (remarkData) {
                    entity.addRemark(remarkData.trim())
                }
                address = address.toString().replaceAll(Pattern.quote(remarkMatcher[0][0]), "").trim()
            }

            def splitText = /(?:Lao PDR Office:|Myanmar office:|Cambodia office:|Vietnam office:)\s*(?=(?:#17|#12|31BT|5th))/
            address.split(/(?si)(?<=LIMA|PHILIPPINES|ARGENTINA)\s*(?=C\\/O|CALLE PATERA)|(?si)(?=NUSUPBEKOV)|(?is)(?<=Afghanistan)\s*(?=HEAD OFFICE)|$splitText|(?is)(?<=CHINA)\s*(?=23 Building)|(?i)(?=Orucoc Kucesi|Apt 6)|(?s)(?<=Vietnam)\s+(?=NO. 40)|(?s)(?:\s*Residential Address:\s*)|(?s)(?=Sinamangal)|(?s)(?<=China)\s*(?=5TH FLOOR)|(?is)(?<=Canada)\s*(?=SUITE 1402)|\(previous address:\s(?=No\.271-1)|(?s)(?<=CHINA)\s*(?=5914 GARTRELL)|Former Address:|(?s)(?<=CHINA);/).each {

                def addr = it.toString().replaceAll(/(?s)\s+/, " ").trim().replaceAll(/(?i)\bSpain\b/, " Spain")
                addr = addr.toString().replaceAll(/(?ism)BLOCK B97\(BLK 69\)/, "BLOCK B97 BLK 69")
                addr = addr.toString().replaceAll(/(?ism) \(INCAT S. de R.L. de C.V.\)/, "")
                addr = addr.replaceAll(/\(Room 311 Lanchuang Mansion\)/, "")
                addr = addr.toString().replaceAll(/(?ism), in front of Hospital San Jorge, same place as Hotel La Estancia, Tegucigalpa, HONDURAS/, ", Tegucigalpa, HONDURAS").trim()
                addr = addr.toString().replaceAll(/\s*(?:12C34|111803|555A151505|319|11|17|23)$/, "").trim()
                addr = addr.toString().replaceAll(/^null$/, "").trim()

                //println "debug it debug it : "+addr
                def street_sanitizer = { street ->
                    fixStreet(street)
                }
                if (StringUtils.isNotBlank(addr)) {
                    def addrMap = addressParser.parseAddress([text: addr, force_country: true])
                    //println "debugging : "+addrMap
                    ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                    if (scrapeAddress) {
                        if (scrapeAddress.city) {
                            scrapeAddress.city = scrapeAddress.city.replaceAll(/(?s)\s+/, " ").trim()
                        }
                        entity.addAddress(scrapeAddress)
                    }
                }
            }
        }


        if (remark) {
            entity.addRemark(remark)
        }
        if (aliasList) {
            aliasList.each { value ->
                value = value.replaceAll(/(?s)\s+/, " ")
                    .replaceAll(/\.$|d.?b.?a.?/, "")
                    .replaceAll(/\s*,\s*$/, "").trim()
                def remarkMatcher

                if (value) {
                    if ((remarkMatcher = value =~ /\(?(N°\s*\d+.+)/)) {
                        entity.addRemark(remarkMatcher[0][1].toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\)$/, "").trim())
                        value = value.replaceAll(Pattern.quote(remarkMatcher[0][1].toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\)$/, "").trim()), "")
                    }
                    if ((remarkMatcher = value =~ /(?i)(\bReg\b.+)/)) {
                        entity.addRemark(remarkMatcher[0][1].toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\)$/, "").trim())
                        value = value.replaceAll(Pattern.quote(remarkMatcher[0][1].toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\*\s*$/, "").replaceAll(/\)$/, "").trim()), "")
                    }
                    if (type == "P") {
                        value = personNameReformat(value)
                    }
                    entity.addAlias(value.replaceAll(/\(\)/, "").replaceAll(/(FORMERLY KNOWN AS\s*)|(FORMERLY\s*)|(known as\s*)/, "").replaceAll(/\)\s*$/, "").replaceAll(/TRACHTECH/, "TRACHTECH)").replaceAll(/(?s)\s+/, " ").trim())
                }
            }
        }
        if (alias) {
            alias.each { value ->
                value = value.replaceAll(/(?s)\s+/, " ")
                    .replaceAll(/\.$|d.?b.?a.?/, "")
                    .replaceAll(/\s*,\s*$/, "").trim()
                if (value) {
                    def remarkMatcher
                    if ((remarkMatcher = value =~ /\(?(N°\s*\d+.+)/)) {
                        entity.addRemark(remarkMatcher[0][1].toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\)$/, "").trim())
                        value = value.replaceAll(Pattern.quote(remarkMatcher[0][1].toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\)$/, "").trim()), "")
                    }
                    entity.addAlias(value.replaceAll(/\(\)/, "").replaceAll(/(FORMERLY KNOWN AS\s*)|(FORMERLY\s*)|(known as\s*)/, "").replaceAll(/\)\s*$/, "").replaceAll(/(?s)\s+/, " ").trim())
                }
            }
        }
    }

    def fixHtml(data) {
        def htmlMatcher = data =~ /(?si)<div id="viewcontainer(?:.+?)(<table.+<\\/table>)\s*<\\/div>\s*<br>/
        if (htmlMatcher) {
            data = htmlMatcher[0][1].toString().replaceAll(/(?si)<tr>(.+?)<\\/tr>/, "")
                .replaceAll(/(?si)\A<table[^>]+>/, "")
                .replaceAll(/(?sim)<tr valign="top"( bgcolor="#EFEFEF")?><td colspan="9">\s+<table(.+?)<tr valign="top">\s*<td>\s*<a href="\/oga0009p.nsf\/sancALL3P\?OpenView(.+?)<\/table>(.+?)<\/td>\s*<\/tr>/, "")
                .replaceAll(/<\/table>/, "")
                .replaceAll(/&#65288;(?:A2001-2008)&#65289;/, "")
        }
        return data
    }

    def fixUnicode(data) {
        data = data.replaceAll(/\u00ed/, "i")
            .replaceAll(/\u00f1/, "n")
            .replaceAll(/[\u00d5-\u00d6]/, "O")
            .replaceAll(/\u00d3/, "O")
            .replaceAll(/[\u00c1-\u00c3]/, "A")
            .replaceAll(/\u00e1/, "a")
            .replaceAll(/\u00c7/, "C")
            .replaceAll(/\u00c9/, "E")
            .replaceAll(/\u00ca/, "E")
            .replaceAll(/\u00cd/, "I")
            .replaceAll(/\u00d1/, "N")
            .replaceAll(/\u00f3/, "o")
            .replaceAll(/\u00e9/, "e")
            .replaceAll(/\u00fa/, "u")
            .replaceAll(/\u015e/, "S")
            .replaceAll(/\u017d/, "Z")
        return data
    }

    def personNameReformat(name) {
        def exToken = "(?:[js]r|I{2,3})"
        return name.replaceAll(/(?i)\s*(.*?)\s*,\s*\b((?:(?!$exToken\b)[^,])+)s*(?:,\s*\b($exToken)\b)?\s*$/, '$2 $1 $3').trim()
    }

    def detectEntityType(name) {
        def type = context.determineEntityType(name)
        if (type.equals("P")) {
            if (name =~ /(?i)(?:S\.\s*A\.|ENTREPRISE|NIDSECURITY SA|M\/S|M&R Heavy Equipment Road Builders|MODERN SERVIS MMC\/MODERN SERVIZ MMC|S\.L\.|d\.o\.o\.|Interkomerc A\.D\.|R\.L\.|E\.I\.R\.L\.|DUTCHMED B\.V\.|Enterprise|IDEMIA|FRANCE|BRANCH|OFFICE|S\.R\.L\.|HIFAB OY|S\.A\.E\.|D\.O\.O\.|IM DISTRIBUCIONES|Vikadiza Ingenieros S\.A\.C\.|Waira & Power|Neo Soft S\.R\.L\.|OTV|LTDA|S\.A\.E\.)/) {
                type = "O"
            }
        }
        return type
    }

    def detectEntity(name) {
        def type
        if (name =~ /^\S+$|(?si)OT PERU|PURIHOLI NIGERIA|PT CITRA GADING ASRITAMA|CAISSON AE|INPRECO Ingenieria Prefabricados y Construccion|IM DISTRIBUCIONES|BITTOHIN CHASI SOMAJ KALLYAN SANGSTHA|Construcciones Arquitectonicas Construsarq|BRC Constructions|ACE SCUTMADEIRA SISTEMAS DE GESTAO E CONTROLO DE TRÀFEGO|AKTOR BULGARIA SA|Amazonas Energia SAC|BWSC Japan/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
        }

        return type
    }

    def fixStreet(street) {
        street = street.replaceAll(/\s*,\s*$/, "")
            .replaceAll(/(?s)\s+/, " ")
            .replaceAll(/(?i)Email:|&quot\W*/, "")
            .replaceAll(/(?i)WEST\s*$/, "")
            .replaceAll(/(?i)[a-zA-z\.]+@\w+\.\w+/, "")
            .replaceAll(/(?i)State\s+of\s*$/, "")
            .replaceAll(/(?i)PO BOX\s*$/, "")
            .replaceAll(/\W+$/, "")
            .trim()
        return street
    }

    def sanitizeName(data) {
        data = data.toString().replaceAll(/&amp;#65288;|&#65288;/, "(")
        data = data.toString().replaceAll(/&amp;#65289;|&#65289;/, ")")
        data = data.replaceAll(/&amp;/, "&").replaceAll(/(?i)<br>/, "")
            .replaceAll(/\ufffd/, "")
            .replaceAll(/&quot;/, "\"")
            .replaceAll(/&#8220;/, "\"")
            .replaceAll(/&#8221;/, "\"")
            .replaceAll(/&#8211;/, "-")
            .replaceAll(/&#27743;|&#35199;|&#30465;|&#21326;|&#23041;|&#24314;|&#26448;|&#26377;|&#38480;|&#20844;|&#21496;/, "")
            .replaceAll(/&#381;/, "Z")
            .replaceAll(/&#304;/, "I")
            .replaceAll(/\*/, "")
            .replaceAll(/&#350;/, "S")
            .replaceAll(/&#360;/, "U")
            .replaceAll(/\(&#23665.+/, "")
            .replaceAll(/(?i)\(M(r|s).\)$|/, "")
            .replaceAll(/\s*\(\s*$/, "")
            .replaceAll(/&#8217;/, "'")
            .replaceAll(/\s*\(\s*$/, "")
            .replaceAll(/\(\)/, "")
            .replaceAll(/\(\*/, "")
            .replaceAll(/[\[|\(||\{]$/, "")
            .trim()
        data = data.replaceAll(/(?s)\s+/, " ").trim()
        data = data.replaceAll(/(?i)\(Madecor affiliates, including, but not limited to: /, ";")
        data = data.replaceAll(/\(\d+\)/, ";").trim()
        data = data.replaceAll(/;+/, ";")
        return data
    }

    def sanitizeAddress(data) {
        if (data) {
            data = data.toString().replaceAll(/YANCHENG JIANGSU/, "YANCHENG, JIANGSU")
            data = data.toString().replaceAll(/Macau/, "Macau, China")
            data = data.replaceAll(/(?is)CHINA\s+/, "CHINA;")
            data = data.replaceAll(/(?i)KAMPALA/, "KAMPALA, Uganda")
            data = data.replaceAll(/(?i)\s+Dhaka$/, ",Dhaka, Bangladesh")
            data = data.replaceAll(/(?i)(DHAKA,\s*\d+)$/, '$1, Bangladesh')
            data = data.replaceAll(/(?i)DISTRICT CHUADANG-7200/, "DISTRICT CHUADANG-7200, Bangladesh")
            data = data.replaceAll(/(?i)JAKARTA SELATAN$/, "JAKARTA SELATAN, Indonesia")
            data = data.replaceAll(/LOME$/, "LOME, Togo")
            data = data.replaceAll(/Jakarta Barat 11520/, "Jakarta Barat 11520, Indonesia")
            data = data.replaceAll(/(CHISINAU MD-\d+)$/, '$1,Moldova')
            return data.replaceAll(/&#8211;/, "-")
                .replaceAll(/(?i)<br>|\n/, " ")
                .replaceAll(/\ufffd/, " ")
                .replaceAll(/&#8220;/, "\"")
                .replaceAll(/&#8221;/, "\"")
                .replaceAll(/&#8211;/, "-")
                .replaceAll(/&#65292;/, ",")
                .replaceAll(/\(&#20013;.+/, "")
                .replaceAll(/(?s)\s+/, " ")
                .replaceAll(/,\s*$/, "")
                .replaceAll(/&#8217;/, "'")
                .replaceAll(/&#778;/, "")
                .replaceAll(/&#352;/, "S")
                .replaceAll(/&#353;/, "s")
                .replaceAll(/&#350;/, "S")
                .replaceAll(/&#381;/, "Z")
                .replaceAll(/&#65288;/, "(")
                .replaceAll(/&#321;/, "L")
                .replaceAll(/&#263;/, "C")
                .replaceAll(/&#269;/, "c")
                .replaceAll(/&#286;/, "G")
                .replaceAll(/&amp;/, "&")
                .replaceAll(/(?si)Region Khujand/, "Region, Khujand")
                .replaceAll(/(?is)(People's|Peoiple's) Republic of/, "")
                .replaceAll(/(?is)Arab Republic of/, "")
                .replaceAll(/(?is)CITY,?/, "")
                .replaceAll(/(?is)PROVINCE,?/, "")
                .replaceAll(/(?is)THANAPARA POST OFFICE ISHURDI DISTRICT/, "THANAPARA POST OFFICE ISHURDI")
                .replaceAll(/(?i)ZIP CODE/, "")
                .replaceAll(/1070 JINSHAN/, "1070, JINSHAN")
                .replaceAll(/(?i)135-815 Korea, Republic of/, "135-815 Korea")
                .replaceAll(/Co\.,/, "Co.")
                .replaceAll(/(?s)\s+/, " ")
                .replaceAll(/\bSAR\b/, "")
                .replaceAll(/(?si)LAST KNOWN ADDRESS:/, "")
                .replaceAll(/&#8239;/, " ")
                .replaceAll(/&quot;A&quot;/, "\"A\"")
                .replaceAll(/Peshawar PAKISTAN/, "Peshawar, PAKISTAN")
                .replaceAll(/,(?= 1ST FLOOR A, TC BUILDING| TC BUILDING, MONROVIA 1000)/, "")
                .replaceAll(/61 Jinchuan Road Central, Xingan County Ji'an Jiangxi 33130 CHINA/, "61 Jinchuan Road Central, Xingan County, Ji'an, Jiangxi 33130, CHINA")
                .replaceAll(/New Panvel Navi Mumbai 410206 INDIA/, "New Panvel Navi, Mumbai 410206, INDIA")
                .replaceAll(/(?:Debarred|<td><font size="1" face="Verdana">Debarred)/, "")
                .replaceAll(/\(311$/, "")
                .replaceAll(/271-1\)$/, "")
                .replaceAll(/\(258311-1$/, "")
                .replaceAll(/\(undeliverable\)$/, "")
                .replaceAll(/\s38\(111111$/, "")
                .replaceAll(/\s-44$/, "")
                .replaceAll(/\s-303-1$/, "")
                .replaceAll(/\s-271-1$/, "")
                .replaceAll(/\sA-05$/, "")
                .replaceAll(/\s-19-1$/, "")
                .replaceAll(/\(Room 1111, 11th Floor, Lanchuang Building\)/, "")
                .replaceAll(/\(according to contract; fictitious\)/, "")
                .replaceAll(/Address not disclosed by the sanctioned company\./, "")
                .replaceAll(/&#\d+;/, "")
                .trim()
        }
    }

    def sanitizeAlias(data) {
        data = data.toString().replaceAll(/\(|\)/, "")
            .replaceAll(/(formerly|also|CURRENTLY)\s+known\s+as:?\s+/, "")
            .replaceAll(/also\s+adopting\s+the\s+name\s+of/, "")
            .replaceAll(/formerly\s+/, "")
            .replaceAll(/a\.k\.a\.\s+/, "")
            .replaceAll(/also doing business as /, "")
            .replaceAll(/previous name: /, "")
            .replaceAll(/name change from /, "")
        return data = data.split(/,/)
    }

    def sanitizeDateAndDescr(def name, def date, def descr) {

        name = name.toString()
        descr = descr.toString()
        date = date.toString().replaceAll(/^.+<img src.+$/, "")
        date = date.replaceAll(/(?i)(?:Pakistan|People's Republic of China|India)/, "")


        if (name.equals("CROSSWORDS LTD.")) {
            date = "8/Jul/2021 |  28/Jun/2027"
            descr = descr.replaceAll(/^.+$/, "")
        }

        return [date, descr]
    }
}