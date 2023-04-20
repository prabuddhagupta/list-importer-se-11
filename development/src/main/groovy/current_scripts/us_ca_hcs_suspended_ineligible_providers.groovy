package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification
import scrapian_scripts.utils.StateMappingUtil

import java.text.SimpleDateFormat
import java.util.regex.Pattern

context.setup([connectionTimeout: 60000, socketTimeout: 60000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko)Chrome/103.0.5060.134 Safari/537.36 Edg/103.0.1264.77"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

Us_ca_hcs_suspended script = new Us_ca_hcs_suspended(context)
script.initParsing()

def class Us_ca_hcs_suspended {
    final entityType
    final ScrapianContext context

    def moduleLoaderVersion = context.scriptParams.moduleLoaderVersion
    final def moduleFactory = ModuleLoader.getFactory(moduleLoaderVersion)


    final def mainUrl = "https://files.medi-cal.ca.gov/pubsdoco/SandILanding.aspx"
    final def csvUrl = "https://files.medi-cal.ca.gov/pubsdoco/Publications/masters-MTP/zOnlineOnly/susp100-49_z03/suspall.xlsx"
    //def filePath  = System.getProperty("user.home")+"/Downloads/suspall.xlsx"
    def filePath  = "/home/maenul/Downloads/suspall1.xls"
    final addressParser
    final String splitTag = "~SPLIT~"

    Us_ca_hcs_suspended(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        this.context = context
    }

    def initParsing() {
//        def html = context.invoke([url: mainUrl, tidy: false, cache: false])
//        def csvUrl, csvUrlMatcher
//        if ((csvUrlMatcher = html =~ /(?ism)<li><a href="(\\/pubsdoco\/Publications.+?xlsx)"/)) {
//            csvUrl = "https://files.medi-cal.ca.gov" + csvUrlMatcher[0][1]
//            println csvUrl
//        }
       // def spreadsheet = context.invokeBinary([url: csvUrl])
        def spreadsheet = context.invokeBinary([url: "file:////"+ filePath])
        def xml = context.transformSpreadSheet(spreadsheet, [validate: true, escape: true])

        xml = xml.replace("\\u001E", "")
        xml = xml.replace(/&/, "&amp;")
        def rows = new XmlSlurper().parseText(xml.value)
        rows.row.each { item ->

            def orgName, licenseNumberList = [], providerNumberList = [], name, type, aliasList = [], addressList = []
            def lastName = sanitize(item.last_name.toString())
            def firstName = sanitize(item.first_name.toString())
            def middleName = sanitize(item.middle_name.toString())
            def alias = sanitize(item.aka_also_known_as_dba_doing_business_as.toString())
            alias = sanitizeAlias(alias)
            def address = sanitize(item.addresses.toString())
            address = sanitizeAddress(address)
            def licenseNumber = sanitize(item.license_number.toString())
            licenseNumber = sanitizeID(licenseNumber)
            def providerNumber = sanitize(item.provider_number.toString())
            providerNumber = sanitizeID(providerNumber)
            def eventDate = sanitize(item.date_of_suspension.toString())

            if (licenseNumber) {
                licenseNumberList.add(licenseNumber)
            }

            if (providerNumber) {
                providerNumberList.add(providerNumber)
            }


            if (!firstName) {
                orgName = lastName
            } else
                name = firstName + " " + middleName + " " + lastName


            if (alias) {
                aliasList.add(alias)
            }
            if (address) {
                addressList.add(address)
            }
            name = secondAliasPattern(name, aliasList)
            orgName = secondAliasPattern(orgName, aliasList)

            if (name) {
                name = sanitizeName(name)
                createEntity(name, aliasList, addressList, licenseNumberList, providerNumberList, eventDate, type = "P")
            }

            if (orgName) {
                orgName = sanitizeorgName(orgName)
                createEntity(orgName, aliasList, addressList, licenseNumberList, providerNumberList, eventDate, type = "O")
            }

        }
    }

    def detectEntity(def fullName) {
        def type
        if (fullName =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(fullName)
            if (type.equals("P")) {
                if (fullName =~ /(?i)(?:Pharmaceuticals|Pharmacy)/) {
                    type = "O"
                }
            } else if (fullName =~ /(?i)(?:^Aase Jurgensen|^Aileen no|^Alan sha)/) {
                type = "P"
            }
        }
        return type
    }


    def createEntity(name, aliasList = [], addressList = [], licenseNumberList = [], providerNumberList = [], eDate, type) {
        def entity = null
        def entityType = detectEntity(name)
        entity = context.findEntity("name": name, "type": entityType)

        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(entityType)
        }

        aliasList.each {
            if (it) {
                it.split(/$splitTag/).each { def alias ->
                    alias = alias.replaceAll(/(?s)\s$/, "").trim()
                    alias = alias.replaceAll(/(?ism)AKAs:/, "").trim()
                    alias = fixAlias(alias)
                    alias = swapAliasNames(alias)
                    entity.addAlias(alias)
                }
            }
        }


        addressList.each {
            if (it) {
                it.split(/$splitTag/).each { def address ->
                    address = address.replaceAll(/(?s)\s$/, "").trim()
                    address = address.replaceAll(/(?s)\s+/, " ").trim()
                    StateMappingUtil st = new StateMappingUtil();
                    address = address.replaceAll(/(?ims)(.*?)\b([A-Z]{2})\b[\s\.\,]*$/, { a, b, c -> return b + st.normalizeState(c) })
                    address = address.replaceAll(/(?ims)(.*?)\b([A-Z]{2})\b([\,\s]*\d+[\-\d+]*)\s*$/, { a, b, c, d -> return b + st.normalizeState(c) + d })
                    if (address) {
                        address = sanitize(address)
                        // address = address.toString().toLowerCase()
                        def parsedAdrs = addressParser.parseAddress([text: address, force_country: true])
                        ScrapeAddress scrapeAddress = addressParser.buildAddress(parsedAdrs, [street_sanitizer: street_sanitizer])
                        if (scrapeAddress) {
                            entity.addAddress(scrapeAddress)
                        } else {
                            ScrapeAddress sc = new ScrapeAddress()
                            sc.setAddress1(address)
                            sc.setCountry("US")
                            entity.addAddress(sc)
                        }

                    }

                }
            }
        }

        licenseNumberList.each {
            if (it) {
                it = it.replaceAll(/(?is)(?:\,|\/)\s*/, splitTag)
                it.split(/$splitTag/).each { def idValue ->
                    ScrapeIdentification si = new ScrapeIdentification()
                    si.setType("License Number")
                    si.setValue(idValue)
                    entity.addIdentification(si)
                }
            }
        }

        providerNumberList.each {
            if (it) {
                it = it.replaceAll(/(?is)(?:\,|\/)\s*/, splitTag)
                it.split(/$splitTag/).each { def idValue ->
                    ScrapeIdentification si = new ScrapeIdentification()
                    // si.setType("Provider Number")
                    si.setType("National Provider Identifier")
                    si.setValue(idValue.replaceAll(/(?s)\s+/, " ").trim())
                    entity.addIdentification(si)
                }
            }
        }


        def event = new ScrapeEvent()
        eDate = context.parseDate(new StringSource(eDate), ["MM/dd/yyyy", "dd-MMM-yyyy"] as String[])
        def currentDate = new Date().format('MM/dd/yyyy')
        SimpleDateFormat sdformat = new SimpleDateFormat("MM/dd/yyyy")
        def d1 = sdformat.parse(currentDate)

        def remark
        if (eDate) {
            def d2 = sdformat.parse(eDate)
            //If date > current date, then add it to remark
            if (d2.compareTo(d1) > 0) {
                remark = eDate
                eDate = eDate.replaceAll(/(?ism).*/, "").trim()
            }
        }

        if (eDate.equals("03/13/0209")) {
            eDate = eDate.replaceAll(/(?ism).*/, "").trim()
        }

        if (eDate.equals("01/01/1753")) {
            eDate = eDate.replaceAll(/(?ism).*/, "").trim()
        }

        event.setDate(eDate)

        //Add Remark
        if (remark) {
            entity.addRemark("The following date is a future date : " + remark)
        }
        event.setDescription("This entity appears on the California Health Care Services list of Suspended and Ineligible Providers.")
        entity.addEvent(event)
    }

    def invoke(url, headersMap = [:], cache = true, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def sanitize(data) {
        return data.replaceAll(/&amp;|&amp;amp;/, '&').replaceAll("\\u0450", "e").replaceAll(/\r\n/, "\n")
            .replaceAll(/(?ism)n\/a/, "").replaceAll(/(?ism)&apos;/, "'")
            .replaceAll(/\s+/, " ").trim()
    }


    def sanitizeID(idValue) {
        idValue = idValue.replaceAll(/(?sm)^\,\s*/, "")
        idValue = idValue.replaceAll(/(?sm)\,\s*\,\s*/, ",")
        idValue = idValue.replaceAll(/(?sm)\s*\-\s*\,\s*/, "-")
        idValue = idValue.replaceAll(/(?sm)\s*\,\s*\-\s*/, "-")
        idValue = idValue.replaceAll(/(?ism)^\-(\S+)\,\s*(\d+\-\S+)/, '$1-$2')
        idValue = idValue.replaceAll(/(?ism)n\/a|SAT#&#8217;s,/, "")
        idValue = idValue.replaceAll(/(?ism)(\*)(\d{3,})/, { def a, b, c -> return c })
        idValue = idValue.replaceAll(/(?ism)(DL19271)(\*)/, { def a, b, c -> return b })
        idValue = idValue.replaceAll(/\.0$/, "").trim()
        idValue = idValue.replaceAll(/(?s)\s+/, " ").trim()
        return idValue
    }

    def sanitizeorgName(def name) {
        name = name.replaceAll(/(?ism)&#8217;/, "'").trim()
        name = name.replaceAll(/(?ism),$/, "'")
        name = name.replaceAll(/(?s)\s+/, " ").trim()
        return name
    }

    def sanitizeName(name) {
        name = name.replaceAll(/(?ism)&#\d{2,};/, "'")
        name = name.replaceAll(/(?ism)\bm[rs]\b\.?/, "")
        name = name.replaceAll(/(?ism)\bPh D\s*\b/, "")
        name = name.replaceAll(/(?ism)^\bCA\s*\b/, "")
        name = name.replaceAll(/(?ism)Reotutar-Doctor/, "Reotutar")
        name = name.replaceAll(/(?im)^(.+?)\s*\b(?:([js]r|IV|I{2,}V?)\b\.?)((?:\s\S+)+)$/, '$1$3 $2')
        name = name.replaceAll(/(?ism)n\/a/, "")
        name = name.replaceAll(/(?ism)&apos;/, "'")
        name = name.replaceAll(/(?s)\s+/, " ").trim()
        return name
    }

    def sanitizeAlias(alias) {
        alias = alias.replaceAll(/(?ism)&quot|&#8221;/, "'")
        alias = alias.replaceAll(/(?ism)&#\d{2,};/, "'")
        alias = alias.replaceAll(/(?ism)';Queen';/, "'Queen'")
        alias = alias.replaceAll(/(?ism)\b(?:co)?[\-]*owner[^;]+/, "")
        alias = alias.replaceAll(/(?ism)^\;/, "")
        alias = alias.replaceAll(/(?is)aka[\s,]*$/, "")
        alias = alias.replaceAll(/Rehabilitationdba:/, "Rehabilitation dba:")
        alias = alias.replaceAll(/(?ism)^[\;]*\s*\b(?:aka|dba|Director|General Partner|nee|President|Vice President &, Administrator|Administrators|CEO)\b\s*[\:]*/, "")
        alias = alias.replaceAll(/(?ism)[\;]*\s*\b(?:aka|dba)\b\s*[\:]*/, splitTag)
        alias = alias.replaceAll(/(?ism)\;.*?\:\s*[\,]*/, splitTag)
        alias = alias.replaceAll(/(?ism)^\s*\bn\.d\b\./, "")
        alias = alias.replaceAll(/(?ism)Castillo, Maria Angelita\,\s*/, "")
        alias = alias.replaceAll(/(?ism)\(Chris\)/, "Chris")
        alias = alias.replaceAll(/(?ism)[\;]*\s*\;\s*$/, "")
        alias = alias.replaceAll(/(?ism)(?:\,\s*$|^\s*\,\s*)/, "")
        alias = alias.replaceAll(/(?ism)^\s*/, "")
        alias = alias.replaceAll(/(?ism)Sheen, John D. Lu, Diana/, "Sheen, John D. ; Lu, Diana")
        alias = alias.replaceAll(/(?ism)Mohulla, Aiman Solomon, David/, "Mohulla, Aiman ; Solomon, David")
        alias = alias.replaceAll(/(?ism)Shih, Michael Shou, Hsien/, "Shih, Michael ; Shou, Hsien")
        //EZ
        alias = alias.replaceAll(/(?ism)EZ/, "")
            .replaceAll(/(?ism)Basir, Wahed, Bassir, Wahedulla/, "Basir, Wahed;Bassir, Wahedulla ")
        alias = alias.replaceAll(/(?ism)\;/, splitTag)
        alias = alias.replaceAll(/(ism)Lowe, II, Ellis County/, "Lowe II, Ellis County")
        alias = alias.replaceAll(/(?ism)Leona Mary Poe, Augley, Keller, Knights, Waging, Weging and Downs/, "Leona Mary Poe, Leona Mary Augley, Leona Mary Keller, Leona Mary Knights, Leona Mary Waging, Leona Mary Weging, Leona Mary Downs")
        alias = alias.replaceAll(/(?i),\s*\b(?=maria|jade|Buford|Benfield|Owen|Josefina|Kathleen|Ana\sMaria|Ellis|Jerry\sFein|Leona\sMary)\b/, splitTag)

//        //alias = alias.replaceAll(/(?i)akas/,"")
        return alias
    }

    def fixAlias(alias) {

        //formatting
        alias = alias.replaceAll(/(?ism)[\,]*\s*\bM\.D\b\.\s*$/, "")
        alias = alias.replaceAll(/(?ism)[\,]*\s*\bjr\b\s*\.\s*\,/, " Jr.,")
        alias = alias.replaceAll(/(?ism)([^,]+),\s+([^,]+)[,\s]*(\b(?:[IV]+|[Js]r)\b\.*)((?:\s\S+))/, '$2$4 $1 $3')
        alias = alias.replaceAll(/(?ism)([^,]+),\s+([^,]+)[,\s]*(\b(?:[IV]+|[Js]r)\b\.*)/, '$2$1 $3')
        alias = alias.replaceAll(/(?ism)Angel Jr. Michel/, "Michel Angel Jr.")
        alias = alias.replaceAll(/(?ism)Robert Van Sweatt, Jr./, "Robert Van Sweatt Jr.")
        alias = alias.replaceAll(/Lowe, II/, "Lowe II")
        alias = alias.replaceAll(/(?ism)\,\s*$/, "")
        return alias
    }

    def swapAliasNames(alias) {
        if (alias =~ /(?i)\,(?!\s*.*?\b(?:LLC|INC|&|THE|Corp|Svc|Corporation|Associates|Still\s&\sHinshaw)\b)/) {
            alias = alias.replaceAll(/(?im)^([^,]+),([^,]+)$/, '$2 $1')
        }
        alias = alias.replaceAll(/(?ism)^\s*/, "")
        alias = alias.replaceAll(/(?s)\s+/, " ").trim()
        return alias
    }

    def secondAliasPattern(data, aliasList = []) {

        def secondAliasMatch = data =~ /(?ism)[\,]*\s*\b(?:dba|Aka)\b(.*?)$/
        if (secondAliasMatch) {
            def removeAliasPart = Pattern.quote(secondAliasMatch[0][0])
            def secondAlias = sanitize(secondAliasMatch[0][1])
            secondAlias = secondAlias.replaceAll(/(?s)\s+/, " ").trim()
            aliasList.add(secondAlias)
            data = data.replaceAll(/$removeAliasPart/, "")
        }
        return data

    }

    def sanitizeAddress(address) {
        address = address.replaceAll(/(?is)\,\s*and\/or\s*\,/, splitTag)
        address = address.replaceAll(/(?s)[\,]*\s*\bor\b\s*[\,]*/, splitTag)
        address = address.replaceAll(/(?ism)(?<=(?-i:[A-Z][A-Z]))\b[\s\,]*\s*\b[a]*and\b\s*[\,]*/, splitTag)
        address = address.replaceAll(/(?ism)(?<=CA, 915 Sonora Rd\.)[\,]*\s*\band\b\s*[\,]*/, splitTag)
        address = address.replaceAll(/(?ism)(?<=Inglewood, CA\.)[\,]*\s*\band\b\s*[\,]*/, splitTag)
        address = address.replaceAll(/(?ism)(?<=, NY\s10019)[\,]*\s*\band\b\s*[\,\s]*(?=\d+)/, splitTag)
        address = address.replaceAll(/(?ism)(?<=Ogden, Utah)[\s\,]*\s*\band\b\s*[\,\s]*(?=\d+)/, splitTag)
        address = address.replaceAll(/(?ism)[\,\s]*\b(?<=ca)\b[\s\,]*&/, splitTag)
        address = address.replaceAll(/(?ism)[\,\s]*\b(?<=ca)\b[\s\,]*qnd[\s\,]*/, splitTag)
        address = address.replaceAll(/(?ism)\b[ca]{1}\s*$/, "CA")
        address = address.replaceAll(/7551 Timberlake Way, Suite 240, 7275 E. Southgate Drive, Ste. 401, Sacramento, CA 95823-2632, Sacramento, CA, 95823-5422/, "7551 Timberlake Way, Suite 240, Sacramento, CA, 95823-2632 ~SPLIT~ 7275 E. Southgate Drive, Ste. 401, Sacramento, CA, 95823-5422")
        address = address.replaceAll(/(?ism)(?<=Stockton, CA)\,\s(?=Flentroy, Pamela)/, splitTag)
        address = address.replaceAll(/(?ism)(?<=Fresno, CA)\,\s(?=Griepp, Heidi Lyn)/, splitTag)
        address = address.replaceAll(/(?ism)(?<=Denver, CO 80256-0001)\,\s(?=Denver, Colorado)/, splitTag)
        address = address.replaceAll(/(?ism)(?<=Los Angeles, CA)\,\s\.\,\s(?=Lindsay, Benita)/, splitTag)
        address = address.replaceAll(/(?ism)(?<=Baldwin Park, CA)\;\s(?=805 E\.)/, splitTag)
        address = address.replaceAll(/(?ism)(?<=San Jose, CA)\sand(?=101 First)/, splitTag)
        address = address.replaceAll(/(?ism)Leandro, CA, Professional Medical, Transportation/, "Leandro, CA")
        address = address.replaceAll(/(?ism)(?<=San Pedro, CA)\,\s(?=Salcedo, Gina Marie)/, splitTag)
        address = address.replaceAll(/(?ism)#26496-112, Federal Correctional , Institution - Dublin, /, "")
        address = address.replaceAll(/(?ism)\,\s*other$/, "")
        address = address.replaceAll(/(?ism)\bAttn\b[\s\:\s]*.*?(?=\breg\b|\b\S*\d+|P.O. Box)/, "")
        address = address.replaceAll(/(?sm)\b(CA)\b[\,\s]*(?:Hospice Service|Numbers|Cardiologist|Psychiatrist|Community Podiatry, Group|Pediatrician|Billing Service|Osteopath|Clinic Administrator|Clinical Psychologist|Unlicensed Nurse Assistant|Supplier|Employee|Lab Technician|Hospital|Medical Transportation)/, "CA")
        address = address.replaceAll(/(?ism)[\,\s]*(?:numbers|Registered Nurse)\s*$/, "")
        address = address.replaceAll(/(?ism)[\(]*Address not available[\)]*/, "")
        address = address.replaceAll(/(?ism)Costa Mesa, CA, 915 Sonora Rd/, "Costa Mesa, 915 Sonora Rd, CA")
        address = address.replaceAll(/(?ism)Baja Califórnia, México/, "Baja California, Mexico")
        address = address.replaceAll(/(?ism)Pella, IO/, "Pella, IA")
        address = address.replaceAll(/(?ism)Sioux City, IO/, "Sioux City, IA")
        address = address.replaceAll(/(?ism)Great Neck, NT/, "Great Neck, NY")
        address = address.replaceAll(/(?ism)Bridgeport, CZ/, "Bridgeport, CA")
        address = address.replaceAll(/(?ism)Saipan, MP/, "Saipan, Northern Mariana Islands")
        address = address.replaceAll(/(?ism)Ellison Bay, WS/, "Ellison Bay, WI")
        address = address.replaceAll(/(?ism)Phillip[p]*ines/, "Philippines")
        address = address.replaceAll(/(?ism)W\. Virginia/, "Virginia, US")
        address = address.replaceAll(/(?ism)Washington, DC/, "Washington DC, US")
        address = address.replaceAll(/(?ism)WA, 98198/, "Washington, 98198, US")
        address = address.replaceAll(/(?ism)\s*\,\s*$/, "")
        // not to break P.O Box
        address = address.replaceAll(/(?is)(?<=box\b)\s+(?=\b\d{4,5})/, "")
        //Sanitize some extra data
        address = address.replaceAll(/(?ism)&#8217;/, "'")
        address = address.replaceAll(/(?ism)&#8211;/, "-")
        address = address.replaceAll(/(?ism)&#189;/, "½")
        address = address.replaceAll(/(?ism)&#233;/, "e").replaceAll(/#42835-298; 8049/, "8049")
        address = address.replaceAll(/(?ism)\;/, splitTag)
        address = address.replaceAll(/(?ism)((?:Inmate #T69588 , |Inmate Reg.#50840-198, *|Terminal Island, |I\.D\. #X09262,\s*)*(\s)*c\/o\s)(.*?)((?:(?:Tecso|, for Wome|G. Mandi,|#G06539,).+?))*((?: P\.O\. Box\s*|CDC X3|#G16|Miss.*?|GSMCCF|FSP #P|FCI Dublin, |83-|#G0|#071)*\d{3,}.+)/, { def a, b, c, d, e, f -> return f })
        address = address.replaceAll(/(?ism)((?:Richard J. D.*?|I\.D\. #X09262, )*c\/o:*(?: (?:Leo|Lt. Cob)*.*?(?:Center,|dinator,))*)(.+)/, { def a, b, c -> return c })
        address = address.replaceAll(/(?ism)^(.+?)$/, { def a, b -> return b + ", US" })
        address = address.replaceAll(/(?ism)(1600 West Ave\., Ste\. J|435 West Arden Ave\., Ste\. #350|1925 College Ave\.|3220 West 71st St|148 Mossglen Cir\.|60 Stephanie Dr., Unit C206|1700 California St., Ste. #570|532 Main St|11427 S. Avalon Blvd\.|42129 Rd\. 126|210 S. Locust St|3478 Main St|1340 Tully Rd., Ste\.)/, { def a, b -> return b + ", California, US" })


        return address
    }

    def street_sanitizer = { street ->
        return street.replaceAll(/(?is)(?<=box)(?=\d{4,5})/, " ")
            .replaceAll(/(?s)\s+/, " ").trim()
    }

}