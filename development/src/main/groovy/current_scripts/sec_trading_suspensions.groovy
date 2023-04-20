import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import scrapian_scripts.utils.AddressMappingUtil

import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * Date: 17 March 2016
 * */

context.setup([socketTimeout: 50000, connectionTimeout: 10000, retryCount: 5])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

debug = false
date = '2000'
searchStr = '34-42937'
//-----Debug area starts------------//
//printPdfUrl();
//debug();
//getData("https://www.sec.gov/litigation/suspensions/suspensionsarchive/susparch2012.shtml")
//getHtml("https://www.sec.gov/litigation/suspensions.shtml")
//-----Debug area ends---------------//

stateList = new ArrayList<String>()
CAStateList = new ArrayList<String>()
countryList = new ArrayList<String>()
countries = ''
sdf = new SimpleDateFormat('MM/dd/yyyy')
sdf2 = new SimpleDateFormat('yyyy')
addressMapper = new AddressMappingUtil()
initParsing()

class Sec_Trading {
    static root = "https://www.sec.gov"
    static url = root + "/litigation/suspensions.shtml"

    static splitTag = "~SPLIT~"
    static aliasTag = "~ALIAS~"

    static globalOrgAliasList = [:]
}

enum FIELDS_SEC
{
    START_DATE, END_DATE, STREET, CITY, STATE, COUNTRY, COUNTRY2, EXTRA_ADDRESS, EVENT_DESC, ENTITY_URL
}

def debug() {
    //url = "https://www.sec.gov/litigation/suspensions/2008/34-57486-o.pdf"
    //url = "https://www.sec.gov/litigation/suspensions/34-51305-o.pdf"
    //url = "https://www.sec.gov/litigation/suspensions/2010/34-62307-o.pdf"
    //url = "https://www.sec.gov/litigation/suspensions/2015/34-75309-o.pdf"
    //url = "https://www.sec.gov/litigation/suspensions/2016/34-76963-o.pdf"
    //url = "https://www.sec.gov/litigation/suspensions/2014/34-71465-o.pdf"
    url = "https://www.sec.gov/litigation/suspensions/34-38895.txt"
    def nameArr = htmlParser(invoke(url), "Discovery Zone, Inc")
//	nameArr[0].each {
//		println it
//	}
}

def prepStateList() {

    String type = ''
    String country = ''
    boolean atUsStates = false
    new File('/RDC/Internal_Data/List_Importer/shared/addressMap.txt').getText('Cp1252').eachLine { line ->
        if (line.startsWith("**")) {
            type = line.substring(2)
            if (type.contains("StateMap")) {
                country = type.substring(0, 2)
                if (country == 'US') {
                    atUsState = true
                } else {
                    atUsState = false
                }
            } else {
                atUsState = false
            }
        } else {
            if (atUsState) {
                String[] splitUp = line.split('\t')
                stateList.add(splitUp[1])
            }
        }
    }

}

def prepCAStateList() {

    String type = ''
    String country = ''
    boolean atCaStates = false
    new File('/RDC/Internal_Data/List_Importer/shared/addressMap.txt').getText('Cp1252').eachLine { line ->
        if (line.startsWith("**")) {
            type = line.substring(2)
            if (type.contains("StateMap")) {
                country = type.substring(0, 2)
                if (country == 'CA') {
                    atCaStates = true
                } else {
                    atCaStates = false
                }
            } else {
                atCaStates = false
            }
        } else {
            if (atCaStates) {
                String[] splitUp = line.split('\t')
                CAStateList.add(splitUp[1])
            }
        }
    }

}

def prepCountryList() {

    String type = ''
    boolean atCountry = false
    new File('/RDC/Internal_Data/List_Importer/shared/addressMap.txt').getText('Cp1252').eachLine { line ->

        if (line.startsWith("**")) {
            type = line.substring(2)
        } else {
            String[] splitUp = line.split('\t')
            if (type == 'CountryMap') {
                countryList.add(splitUp[1])
            }
        }
    }

}

//------------------------------Initial part----------------------//
def initParsing() {
    prepStateList()
    prepCAStateList()
    prepCountryList()
    sb = new StringBuilder()
    index = 0
    countryList.each { country ->
        if (country != 'GEORGIA' && country != 'JERSEY' && country != 'MEXICO') {
            //it was overriding the state of Georgia in US, New Jersey, New Mexico
            sb.append((index++ > 0 ? "|" : "") + country)
        }
    }
    countries = sb.toString().replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")

    def html = invoke(Sec_Trading.url)
    def uMatch = html =~ /(?ism)<tr>\s*(<td>[^\r\n]+)[\r\n]++(<td>[^\r\n]+)[\r\n]+(<td>(?:[^>]+>){0,3}(?:<\/td>)*)[\r\n]+(?=<\/tr>)/

    getData(html, uMatch)

    def yearUrlMatch = html =~ /(?is)\|\s*<a href="([^"]+)">\d+<\/a>/

    while (yearUrlMatch.find()) {
        def yearUrl = Sec_Trading.root + yearUrlMatch.group(1)
        if (!debug || yearUrl.contains(date)) {
            html = invoke(yearUrl)
            uMatch = html =~ /(?ism)(<td\s*\w*>[^\r\n]+)[\r\n]++(<td\s*\w*>[^\r\n]+)[\r\n]+(<td\s*\w*>(?:[^>]+>){0,3}(?:<\/td>)*)[\r\n]+(?=<\/tr>)/
            getData(html, uMatch)
        }
    }
}

def getData(def html, uMatch) {
    def name
    def pdfUrl

    //def uMatch = html =~ /(?ism)<tr valign="top">\s*(<td\s*\w*>[^\r\n]+)[\r\n]++(<td\s*\w*>[^\r\n]+)[\r\n]+(<td\s*\w*>(?:[^>]+>){0,3}(?:<\/td>)*)[\r\n]+(?=<\/tr>)/
    while (uMatch.find()) {
        def urlArr = []
        def attrMap = [:]
        def dataArr = []
        def nameArr = []
        def sDate, lastDate
        def releaseUrlMatch = uMatch.group(1) =~ /<a href="([^"]+)/
        def releaseurl = Sec_Trading.root + releaseUrlMatch[0][1]
        urlArr.add(releaseurl)

        sDate = uMatch.group(2).replaceAll(/<[^>]+>/, "")
        sDate = dateParser(sDate)

        def order = uMatch.group(3)
        parsed = false
        if ((pdfUrlMatch = order =~ /(?i)see also[^"]+"([^"]+)"/)) {
            pdfUrl = Sec_Trading.root + pdfUrlMatch[0][1]
            urlArr.add(pdfUrl)
            if (!debug || pdfUrl.contains(searchStr)) {
                dataArr = pdfUrlParser(pdfUrl)

                parsed = true
            }

        } else {
            if (!debug || releaseurl.contains(searchStr)) {
                html = invoke(releaseurl)
                name = order.replaceAll(/<[^>]+>/, "")
                dataArr = htmlParser(html, name)
                parsed = true
            }
        }


        if (!debug || parsed) {
            nameArr = dataArr[0]
            nameArr.each { def nameAddrAlias ->
                def addrMatch, city, street, state, country
                name = nameAddrAlias[0].trim()
                name = aliasFixingWithTag(name)


                def aliasList = name.split(Sec_Trading.aliasTag).collect({ it ->
                    return sanitize(it)
                })

                name = aliasList[0]
                name = name.replaceAll(/\(The\)/, '')
                name = name.replaceAll(/\($/, '')

                attrMap[FIELDS_SEC.EXTRA_ADDRESS] = null

                def extraAddress = null
                def match = name =~ /\((\b(?:(?>d[-\\/\.]?b[-\\/\.]?[ao]\b|[naf][-\\/\.]?k[-\\/\.]?a\b)[-\\/\.]?|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\s*\\/\s*a)|(?>previously|also|former?ly)(?: known as)?\b)\s*.*?)\)/
                if (match.find()) {
                    def match2 = name =~ /(Laura \(RONALD S|M \(2003|Nelson \(L|Wright \(G)/
                    if (!match2.find()) {
                        def extraText = match.group(1)
                        name = name.replaceAll(/\($extraText\)/, '')

                        if (extraText.toUpperCase() == 'USA') {
                            extraAddress = new ScrapeAddress()
                            extraAddress.setCountry("UNITED STATES")
                            attrMap[FIELDS_SEC.EXTRA_ADDRESS] = extraAddress
                        } else if (extraText.toUpperCase() == 'CHINA') {
                            extraAddress = new ScrapeAddress()
                            extraAddress.setCountry("CHINA")
                            attrMap[FIELDS_SEC.EXTRA_ADDRESS] = extraAddress
                        } else if (extraText =~ /[A-Z]{3}/) {
                            aliasList.add(extraText)
                        } else if (extraText.length() > 1) {
                            def possibleState = extraText.substring(extraText.length() - 2, extraText.length())
                            def possibleMappedState = addressMapper.normalizeState(possibleState)
                            if (possibleState != possibleMappedState) {

                                extraAddress = new ScrapeAddress()
                                extraAddress.setCity(extraText.substring(0, extraText.length() - 3).replaceAll(/(,|,\s|\s)$/, ''))
                                extraAddress.setProvince(possibleMappedState)
                                extraAddress.setCountry("UNITED STATES")
                                attrMap[FIELDS_SEC.EXTRA_ADDRESS] = extraAddress
                            }
                        }
                    }
                }

//                name = name.replaceAll(/\)$/, '')
                name = name.replaceAll(/\s{2,}/, ' ')
                name = name.replaceAll(/\s,/, ',')

                aliasList.remove(0)

                addr = nameAddrAlias[1]
                country = "UNITED STATES"
                //non-us country detection
                if (addr) {

                    addr.toString().replaceAll(/(?i)(.*?),([^,]*?(?:Australia|Cyprus|Canada|China|Ecuador|Germany|Israel|Japan|Malaysia|Poland|South Africa|Thailand|Netherlands|United Kingdom$countries))/, { a, b, c ->
                        addr = b
                        country = c.toUpperCase().trim()
                        return
                    })

                    if ((addrMatch = addr =~ /^([^,]+)[^,]*$/)) {
                        city = addrMatch[0][1]
                        stateList.each { statey ->
                            if (city.toUpperCase().trim() == statey.toUpperCase()) {
                                state = statey
                                city = ''
                            }
                        }
                        CAStateList.each { statey ->
                            if (city.trim().toUpperCase() == statey.toUpperCase()) {
                                state = statey
                                country = 'CANADA'
                                city = ''
                            }
                        }
                        countryList.each { countryt ->
                            if (city.toUpperCase().trim() == countryt) {
                                city = ''
                                country = countryt
                            }
                        }
                        if (city.toUpperCase().contains("PROVINCE")) {
                            state = city
                            city = ''
                        }

                    } else if ((addrMatch = addr =~ /([^,]+),\s*(.*)/)) {
                        city = addrMatch[0][1]
                        state = addrMatch[0][2].trim()
                        //find country from state
                        CAStateList.each { statey ->
                            if (state.trim().toUpperCase() == statey.toUpperCase()) {
                                state = statey
                                country = 'CANADA'
                            }
                        }
                    } else if ((addrMatch = addr =~ /([^,]+),\s*([^,]+),(.*)/)) {
                        street = addrMatch[0][1]
                        city = addrMatch[0][2]
                        country = addrMatch[0][3].trim()
                    }

                    if (street) {
                        attrMap[FIELDS_SEC.STREET] = street
                    } else {
                        attrMap[FIELDS_SEC.STREET] = null
                    }
                    if (city) {
                        attrMap[FIELDS_SEC.CITY] = city
                    } else {
                        attrMap[FIELDS_SEC.CITY] = null
                    }
                    if (state) {
                        attrMap[FIELDS_SEC.STATE] = state
                    } else {
                        attrMap[FIELDS_SEC.STATE] = null
                    }
                } else {

                    attrMap[FIELDS_SEC.STREET] = null
                    attrMap[FIELDS_SEC.CITY] = null
                    attrMap[FIELDS_SEC.STATE] = null
                }
                if (country) {
                    attrMap[FIELDS_SEC.COUNTRY] = country
                }

                def alias = nameAddrAlias[2]
                if (alias) {
                    aliasList.add(alias)
                }

                def event = "This entity appears on the US Securities and Exchange Commission's list of trading suspensions. "// + nameAddrAlias[3]
                lastDate = dateParser(dataArr[1])


                if (lastDate && lastDate =~ /\d/) {
                    attrMap[FIELDS_SEC.END_DATE] = lastDate
                }
                attrMap[FIELDS_SEC.START_DATE] = sDate
                attrMap[FIELDS_SEC.ENTITY_URL] = urlArr
                attrMap[FIELDS_SEC.EVENT_DESC] = event.trim()

                addr = nameAddrAlias[4]
                if (addr) {
                    attrMap[FIELDS_SEC.COUNTRY2] = addr
                }
                createEntity(name, attrMap, aliasList)
            }
        }
    }
}

def htmlParser(def html, def name) {
    def nameArr = []
    def alias, addr, endDate
    name = filterName(name)
    def nameToRegexPattern = Pattern.quote(name)

    def aliasMatch = html =~ /(?i)(?<=\w+)\s+$nameToRegexPattern.*?\(stock symbol\s*([^\)]+)\)/
    if (aliasMatch) {
        alias = aliasMatch[0][1]
    }

    addr = addrMatcher(html, name)
    addr2 = addrMatcher2(html, name)

    def eventDesc = collectEventDescription(name, html, true)
    endDate = endDateMatcher(html)
    nameArr.add([name, addr, alias, eventDesc, addr2])

    return [nameArr, endDate]
}

def pdfUrlParser(def pdfUrl) {
    def pdfTxt
    def nameArr = []

    def nameSection, date
    if (pdfUrl =~ /htm\b$/) {
        pdfText = invoke(pdfUrl)
    } else {
        pdfTxt = pdfToTextConverter(pdfUrl)
    }

    //name match
    def nameSectionMatch = pdfText =~ /(?is)(in the matter of\b.*?)(?:file\b\s+no|\d+\-\d\b\s*)/
    if (nameSectionMatch) {
        nameSection = nameSectionMatch[0][1].trim()

    } else if ((nameSectionMatch = pdfText =~ /(?is)terminating.*?on([^\r\n]+)\r\n\r\n(.*?)(?=\r\n\s*\r\n)/)) {
        date = nameSectionMatch[0][1].trim()
        nameSection = nameSectionMatch[0][2].trim()

    } else if ((nameSectionMatch = pdfText =~ "(?is)[\\u005f]+\\u005f(.*?)file no\\b")) {
        nameSection = nameSectionMatch[0][1].trim()

    }
    if (nameSection) {
        nameSection = filterName(nameSection)
        nameSection = splitNames(nameSection)

        nameSection.split(Sec_Trading.splitTag).each {
            def addr
            it = it.replaceAll(/(?is)\s+/, " ").trim()

            //address matcher
            addr = addrMatcher(pdfText, it)
            addr2 = addrMatcher2(pdfText, it)

            //alias matcher
            def alias
            def quoteOfIt = Pattern.quote(it)
            def aliasMatch = pdfText =~ /(?ism)$quoteOfIt\s*\(\"(\w{40})\s*\d?\"\)/
            if (aliasMatch) {
                alias = aliasMatch[0][1]
            }

            //eventMatcher
            def eventDesc
            pdfText = pdfText.replaceAll(/(?is)^.*?(?:file\b\s+no|\d+\-\d\b\s*)/, "")
            eventDesc = collectEventDescription(it, pdfText)

            nameArr.add([it, addr, alias, eventDesc, addr2])
        }
    } else {

    }

    if (!date) {
        date = endDateMatcher(pdfText)
    }

    return [nameArr, date]
}

def endDateMatcher(def text) {
    def lastDate
    if ((dateMatch = text =~ /(?is)suspended.*?through.*?on\s*(\w+\s*\d+,\s*\d{4})/)) {
        lastDate = dateMatch[0][1]

    } else if ((dateMatch = text =~ /(?is)(?:terminat|conclud)[ie](?:ng)?.*?on.*?(\w+\s*\d+,\s*\d{4})/)) {
        //(?is)terminating.*?on\s*(\w+\s*\d+,\s*\d{4})
        lastDate = dateMatch[0][1]

    } else if ((dateMatch = text =~ /\w+\s*\d+,\s*\d{4}\s*through.*?(\w+\s*\d+,\s*\d{4}\s*)/)) {
        lastDate = dateMatch[0][1]

    }
    if (lastDate) {
        lastDate = lastDate.replaceAll(/(?is)\s+/, ' ')
    }

    return lastDate
}

def addrMatcher(def text, def name) {
    def addr = ""

    text = text.replaceAll(/Vancouver, BC/, 'Vancouver, British Columbia').replaceAll(/ Ft\. /, ' Ft ')

    //only take 2words max. from name
    name.replaceAll(/(?sm)^((?:\b[^\s,]+\b\s*){1,2})[\s\W]/, { a, partOfName ->
        name = partOfName.trim()
        return
    })

    def addrMatch1 = text =~ /(?ism)\b$name\b.*?principal\s*place.*?\b(?:in|listed as)\b\s*((?:(?-i:[A-Z])(?-i:(?:[a-z']|\-[A-Z])+)[\s,]+)+(?:(?-i:[A-Z])(?-i:[a-z']+))+)/
    if (addrMatch1) {
        addr = addrMatch1[0][1].replaceAll(/\s+/, ' ').trim()
        if (debug) {
            context.info("found address by 1 " + addr)
        }
    } else if ((addrMatch1 = text =~ /(?is)\b$name\b.*?\bin\b\s*((?:(?-i:[A-Z])(?-i:[a-z']+)[\s,]+)+(?:(?-i:[A-Z])(?-i:[a-z']+))+)/)) {
        addr = addrMatch1[0][1].replaceAll(/\s+/, ' ').replaceAll(/(?i)(.*?) State/, '$1').trim()
        if (debug) {
            context.info("found address by 2 " + addr + ", " + name)
        }
        if (addr.toUpperCase().contains(name.toUpperCase())) {
            addr = ''
        } else if (addr.toUpperCase().contains('REGIONAL OFFICE')) {
            if ((addrMatch1 = text =~ /(?is)\b$name\b.*?\bof\b\s*((?:(?-i:[A-Z])(?-i:[a-z']+)[\s,]+)+(?:(?-i:[A-Z])(?-i:[a-z']+))+)/)) {
                addr = addrMatch1[0][1].replaceAll(/\s+/, ' ').replaceAll(/(?i)(.*?) State/, '$1').trim()
            } else {
                addr = ''
            }
        }
    } else if ((addrMatch1 = text =~ /(?is)\b$name\b.*?\bin\b\s*((?:(?-i:[A-Z])(?-i:[a-z]+)))\./)) {
        addr = addrMatch1[0][1].replaceAll(/\s+/, ' ').replaceAll(/(?i)(.*?) State/, '$1').trim()
        if (debug) {
            context.info("found address by 3 " + addr + ", " + name)
        }

    } else if ((addrMatch1 = text =~ /(?i)(?<=\w)\s++$name\b[^\r\n]+?(?:[\r\n]{3,}+\s?(?=\S)[^\r\n]*?)?(?-i:of)\s*((?:(?-i:[A-Z])(?-i:[a-z]+)[\s,]*)+,\s*(?:(?-i:[A-Z])(?-i:[a-z]+)[\s]*)+)/)) {
        addr = addrMatch1[0][1].replaceAll(/\s+/, ' ').trim()
        if (debug) {
            context.info("found address by 3.2 " + addr)
        }
    }

    if (addr && addr =~ /(?i)\b(inc|corp)\b/) {

        addr = ""
    }

    return addr.replace(/People's Republic/, "CHINA")
}

def addrMatcher2(def text, def name) {
    def addr

    //only take 2words max. from name
    name.replaceAll(/(?sm)^((?:\b[^\s,]+\b\s*){1,2})[\s\W]/, { a, partOfName ->
        name = partOfName.trim()
        return
    })

    if ((addrMatch1 = text =~ /(?is)\b$name\b.*?\sa(?:n|)\s+((?:[^\s]+\s){1,3})(?:corporation|company)/)) {
        addr = addrMatch1[0][1].replaceAll(/\s+/, ' ').trim()
        //context.info("found address by 4: " + addr + " for " + name + ", text was " + text)

    } else {
        //context.info("did NOT found address by 4 for " + name + ", text was " + text)
    }

    if (addr && addr =~ /(?i)\b(inc|corp)\b/) {
        addr = ""
    }

    return addr
}

def splitNames(def names) {
    if (names.find("INTERNATIONAL DEVELOPMENT\nAND ENVIRONMENTAL HOLDINGS")) {
        names = names.replaceAll(/\n/, ' ')
        return names
    }
    names = names.replaceAll(/(?im),\s*$(?!\s*(?:inc|corp|\(|n\/k\/a))/, Sec_Trading.splitTag)
    names = names.replaceAll(/(?i),*+\s*\band\b(?!\s*(?:inc|ltd|corp))/, Sec_Trading.splitTag)
    names = names.replaceAll(/(?i)(?<=inc\.)\s*,\s*(?=Cyberkinetics)/, Sec_Trading.splitTag)
    names = names.replaceAll(/(?im)(?<=\b(?:inc|ltd|corp)\b)\.?\s*\band\b(?!\s*(?:\w+\s+\w+\s*$))/, Sec_Trading.splitTag)

    if (!(names.contains(Sec_Trading.splitTag))) {
        names = names.replaceAll(/(?im)(?<=\b(?:LP|AG|Inc|America|bank|Ltd|Limited|plc|co|Agricultural|Corp(?:oration)?|Company|Partnership|S\.A|P\.L\.C|Incorporated|LLC|group|Technologies|BanCorp|Trust)\b\.{0,2}+|\))\s*(?:\band\b|\n)\s*+(?!\b(?:Limited|LLC|Inc|(?:[afn][\.\\/]?k|d[\.\\/]?b)[\.\\/]?a)\b|^\s*+\()/, Sec_Trading.splitTag)
    } else {
        names = names.replaceAll(/(?i)(?<=(?:\)|(?:inc|corp)\.?))\s*\n\s*(?!\()/, Sec_Trading.splitTag)
    }

    return names
}

def filterName(def name) {
    name = name.replaceAll(/(?i)in the matter of\s*\r\n/, "")
    name = name.replaceAll(/(?im)^\s{1,10}(?!\s*(?:order|trading))/, "")
    name = name.replaceAll(/(?i)\s{4,}[^\r\n]+/, "")
    name = name.replaceAll(/(?i)in the matter of\s*/, "")
    name = name.replaceAll("\\u003a", "")
    name = name.replaceAll(/(?m)^\s*/, "")
    name = name.replaceAll(/(?m)f\/n\/a/, "a/k/a")
    name = name.replaceAll(/(?is)certain\s*companies.*?pink sheets\s*/, "")
    name = name.replaceAll(/(?i)in\s*re\b\s*/, "")
    name = name.replaceAll(/^((?-i:[A-Z]){3,}\s*)(\r\n)/, { a, b, c -> return b + ' ' })
    name = name.replaceAll(/(?is)(?<=kimberlite)\s*\n\s*(?=international)/, ' ')
    name = name.replaceAll(/(?im)\s*\n\s*(?=solutions, )/, ' ')
    name = name.replaceAll(/(?i)\(continued.*?page\)/, "")
    name = name.replaceAll(/<[^>]+>/, "") //for html part remove ex: <p>
    name = name.replaceAll(/(?is)\s*release.*/, "")

    return name
}

def pdfToTextConverter(pdfUrl) {
    def pdfFile = invokeBinary(pdfUrl)


    def pmap = [:] as Map
    pmap.put("1", "-layout")
    pmap.put("2", "-raw")
    pmap.put("3", "-enc")
    pmap.put("4", "UTF-8")
    pmap.put("5", "-eol")
    pmap.put("6", "dos")

    pdfText = context.transformPdfToText(pdfFile, null).toString()

    return pdfText
}

//------------------------------Entity creation part---------------//
def createEntity(def name, def attrMap, def aliasList = []) {
    def entity = null
    name = name.toString().replaceAll(/,\s*$/, "")

    if (name) {
        entity = createOrgEntity(name, aliasList)
        def address = new ScrapeAddress()
        if (attrMap[FIELDS_SEC.STREET]) {
            address.address1 = attrMap[FIELDS_SEC.STREET]
        }
        if (attrMap[FIELDS_SEC.CITY]) {
            address.city = attrMap[FIELDS_SEC.CITY]
        }
        if (attrMap[FIELDS_SEC.STATE]) {
            address.province = attrMap[FIELDS_SEC.STATE]
        }
        if (attrMap[FIELDS_SEC.COUNTRY]) {
            address.country = attrMap[FIELDS_SEC.COUNTRY]
        }
        entity.addAddress(address)

        if (attrMap[FIELDS_SEC.COUNTRY2]) {
            //context.info('for entity ' + entity.name + ", addr2 = " + attrMap[FIELDS_SEC.COUNTRY2])
            testAddr = attrMap[FIELDS_SEC.COUNTRY2].toUpperCase().trim()
            if (testAddr == 'BRITISH VIRGIN ISLANDS') {
                testAddr = 'VIRGIN ISLANDS, BRITISH'
            }

            //check US states
            foundState = false
            stateList.each { state ->
                if (testAddr.contains(state.toUpperCase())) {
                    foundState = true
                    def address2 = new ScrapeAddress()
                    address2.province = state
                    address2.country = 'UNITED STATES'
                    found = false
                    entity.getAddresses().each { checkAddress ->
                        if (state == checkAddress.getProvince()) {
                            found = true
                        }
                    }
                    if (!found) {
                        entity.addAddress(address2)
                    }
                }
            }
            //check CA states
            if (!foundState) {
                CAStateList.each { state ->
                    if (testAddr.contains(state.toUpperCase())) {
                        foundState = true
                        def address2 = new ScrapeAddress()
                        address2.province = state
                        address2.country = 'CANADA'
                        found = false
                        entity.getAddresses().each { checkAddress ->
                            if (state == checkAddress.getProvince()) {
                                found = true
                            }
                        }
                        if (!found) {
                            entity.addAddress(address2)
                        }
                    }
                }
            }
            //check countries
            if (!foundState) {
                countryList.each { country ->
                    if (testAddr.contains(country.toUpperCase())) {
                        foundState = true
                        def address2 = new ScrapeAddress()
                        address2.country = country
                        found = false
                        entity.getAddresses().each { checkAddress ->
                            if (country == checkAddress.getCountry()) {
                                found = true
                            }
                        }
                        if (!found) {
                            entity.addAddress(address2)
                        }
                    }
                }
            }
            //check for 'CANADIAN'
            if (!foundState) {
                if (testAddr == 'CANADIAN') {
                    foundState = true
                    def address2 = new ScrapeAddress()
                    address2.country = 'CANADA'
                    found = false
                    entity.getAddresses().each { checkAddress ->
                        if ('CANADA' == checkAddress.getCountry()) {
                            found = true
                        }
                    }
                    if (!found) {
                        entity.addAddress(address2)
                    }
                }
            }
            if (!foundState) {
                context.info('could not map state for ' + testAddr + ", name = " + entity.name)
            }
        }

        if (attrMap[FIELDS_SEC.EXTRA_ADDRESS]) {
            entity.addAddress(attrMap[FIELDS_SEC.EXTRA_ADDRESS])
        }


        attrMap[FIELDS_SEC.ENTITY_URL].each { def url ->
            entity.addUrl(sanitize(url))
        }

        def event = new ScrapeEvent()
        if (attrMap[FIELDS_SEC.START_DATE]) {
            def stDate

            stDate = context.parseDate(new StringSource(attrMap[FIELDS_SEC.START_DATE]), ["MM/dd/yyyy","yyyy"] as String[])

            event.setDate(stDate)
        }
        if (attrMap[FIELDS_SEC.END_DATE]) {
            def enDate

            enDate = context.parseDate(new StringSource(attrMap[FIELDS_SEC.END_DATE]), ["MM/dd/yyyy","yyyy"] as String[])

            event.setEndDate(enDate)
        }



        if (event.getDate() != null && event.getEndDate() != null) {
            try {
                if(event.getDate().toString()=~/\d+\/\d+/){
                    sDate = sdf.parse(event.getDate())
                }else{
                    sDate = sdf2.parse(event.getDate())
                }

                if(event.getEndDate().toString()=~/\d+\/\d+/){
                    eDate = sdf.parse(event.getEndDate())
                }else{
                    eDate = sdf2.parse(event.getEndDate())
                }

                if (sDate.compareTo(eDate) > 0) {
                    context.info("Removing end date for " + entity.getName() + ": " + event.getEndDate() + " before " + event.getDate())
                    event.setEndDate(null)
                }
            } catch (Exception e) {
                context.error("Date parse issue: ", e)
            }
        }

        event.setDescription(attrMap[FIELDS_SEC.EVENT_DESC])
        event.setCategory('SEC')
        event.setSubcategory('SPD')
        entity.addEvent(event)
    }
}

def createOrgEntity(def name, def aliasList = []) {
    def entity = null
    entity = context.findEntity(["name": name, "type": "O"])

    if (!entity) {
        entity = context.getSession().newEntity()
        entity.setName(sanitizeName(name))
        entity.type = "O"
    }
    aliasList.each { alias ->
        if (alias.toUpperCase().trim() != 'THE') {
            entity.addAlias(alias)
        }
    }

    return entity
}

def aliasFixingWithTag(def nameStr) {
    def aliasTokens = /\b(?:(?>d[-\/\.]?b[-\/\.]?[ao]\b|[naf][-\/\.]?k[-\/\.]?a\b)[-\/\.]?|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\s*\/\s*a)|(?>previously|also|former?ly)(?: known as)?\b)\s*/

    //general alias inside braces
    nameStr = nameStr.replaceAll(/(?is)(.*?)\s*\(([^\(\)]*?)$aliasTokens([^\(\)]*)\)\s*(.*)/, { a, b, c, d, e ->
        return b + e + Sec_Trading.aliasTag + c + d
    })
    /*//org aliassss inside braces
        .replaceAll(/(?i)\b(?<=llc|inc)\.?\s*(?!\.)\(([^\)]+)\)/, { a, b ->
        return Sec_Trading.aliasTag + " " + b
    })*/
    //remaining alias fixing
        .replaceAll(/(?i)[,;\s]*${aliasTokens}[,;\s]*/, Sec_Trading.aliasTag)

    return nameStr
}

def collectEventDescription(def name, def dataText, def isHtml = false) {
    def finalDescription = "", desMatch
    name.replaceAll(/(?sm)^((?:\b[^\s,]+\b\s*){1,2})[\s\W]/, { a, partOfName ->
        name = partOfName.trim()
        return
    })

    if (!isHtml) {
        // remove page footer with page no
        dataText = dataText.replaceAll(/(?sm)^\s++\b\d++\s+$/, '\n')
        desMatch = dataText =~ /(?is)it appears[^\r\n]*?(?:[\r\n]{3,}+\s?(?=\S)[^\r\n]*?)?$name\b.*?[\r\n]{3,}(?=[^\r\n\S])/

    } else {
        desMatch = dataText =~ /(?is)The Securities and Exchange.*?$name.*(?=The (?:Commission|division) temporarily)/
    }

    if (desMatch) {
        finalDescription = desMatch[0].replaceAll(/\s+/, ' ').trim()
    }

    return finalDescription
}

//------------------------------Misc utils part---------------------//
def invoke(def url, def headersMap = [:], def tidy = true) {
    def html = cachedData(url)
    if (!html) {
        html = context.invoke([url: url, tidy: tidy, headers: headersMap]).toString()
        cachedData(url, html)
    } else {

    }

    return html
}

def invokeBinary(def url, def clean = false) {
    def bin = cachedData(url)
    if (!bin) {
        bin = context.invokeBinary([url: url, clean: clean])
        cachedData(url, bin)
    }

    return bin
}

def dateParser(def dateStr) {
    def date, month, year
    dateStr = dateStr.toString().replaceAll(/Ju1\./,"Jul.")
    def dateMatch = dateStr =~ /(\w+)\.?\s+(\d+),\s+(\d+)/
    if (dateMatch) {
        month = getMonthNo(dateMatch[0][1])
        date = dateMatch[0][2]
        invalidDateMatch = date =~ /^\d$/
        if (invalidDateMatch) {
            date = '0' + date
        }
        year = dateMatch[0][3]
    }
    def finalDate = month + "/" + date + "/" + year
    if (finalDate =~ /null/) {
        return  year
    }
    return finalDate
}

def getMonthNo(def monthName) {
    if (monthName =~ /(?i)\bjan(?:uary)?\b/) {
        return "01"
    } else if (monthName =~ /(?i)\bfeb(?:ruary)?\b/) {
        return "02"
    } else if (monthName =~ /(?i)\bmar(?:ch)?\b/) {
        return "03"
    } else if (monthName =~ /(?i)\bapr(?:il)?\b/) {
        return "04"
    } else if (monthName =~ /(?i)\bmay\b/) {
        return "05"
    } else if (monthName =~ /(?i)\bjune?\b/) {
        return "06"
    } else if (monthName =~ /(?i)\bjuly?\b/) {
        return "07"
    } else if (monthName =~ /(?i)\baug(?:ust)?\b/) {
        return "08"
    } else if (monthName =~ /(?i)\bsep(?:tember)?\b/) {
        return "09"
    } else if (monthName =~ /(?i)\boct(?:ober)?\b/) {
        return "10"
    } else if (monthName =~ /(?i)\bnov(?:ember)?\b/) {
        return "11"
    } else if (monthName =~ /(?i)\bdec(?:ember)?\b/) {
        return "12"
    }
}

def sanitize(data) {
    return data.toString().replaceAll(/&amp;/, '&').trim()
}

def sanitizeName(data) {
    return data.toString().replaceAll(/&amp;/, '&').replaceAll(/\("?\b[A-Z]+\b"?\)/, "").trim()
}

def cachedData(def key, def value = null) {
    //This method or it's caller can be safely deleted in production environment
    try {
        //if value is not present then it will only get otherwise it wll put into cache
        if (!value) {
            return context.cached_data.get(key)
            //if key doesn't exist, it'll return "null"
        } else {
            context.cached_data.put(key, value)
            return true
        }
    } catch (e) {
    }

    return null
}