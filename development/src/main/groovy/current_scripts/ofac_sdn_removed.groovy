package current_scripts

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.*
import org.apache.commons.lang.StringUtils

import java.text.SimpleDateFormat

context.session.escape = true
context.setup([socketTimeout: 50000, connectionTimeout: 10000, retryCount: 5])

//xlsLocation = GenericAddressParserFactory.getScriptLocation(GenericAddressParserFactory.fileLocation, "standard_addresses", "e80d6061710e61dc8c3f80b313752ad9ebaf6807")
//genericAddressParser = GenericAddressParserFactory.getGenericAddressParser(context, xlsLocation)

genericAddressParser = ModuleLoader.getFactory("57841a9d35c7aa315c6e0524b9d6a537978f5faf").getGenericAddressParser(context) //4620a677deca8ef6baf93aa48269de0caa5e6be3

tokens = genericAddressParser.getTokens()
genericAddressParser.reloadData()
genericAddressParser.updatePostalCodes([SE: [/(?s)\b[0-9][0-8][0-4]\s+[0-9][0-9]\b/],
                                        IM: [/(?s)\bIM[0-9]\s+[0-9][A-Z]{2}\b/],
                                        CN: [/(?s)\b[0-9]{6}\b/],
                                        JO: [/(?s)\b[0-9]{5}\b]/]])

genericAddressParser.updateCities([HT: ["Port au Prince=/Port-\\s*au-\\s*Prince/"],
                                   GB: ["March=/(?is)march\\s+(?!wall)/"],
                                   SD: ["Khartoum=/(?i)Khartoum(?:\\s+South|north)?(?!\\s+\\d+)/"],
                                   US: ["Pembroke Pines"],
                                   ID: ["East Jakarta"],
                                   LI: ["Vaduz"],
                                   CO: ["Cali", "Cartago"],
                                   ES: ["Alcala de Henares"]])

genericAddressParser.updateStates([ID: ["Java"],
                                   CO: ["Valle"],
                                   US: ["Washington=/(?is)washington[\\s*,]*dc/"],
                                   DE: ["Nordrhein- Westfalen"]])


context.info("finished building address parser")

comparator = new Comparator<Object>() {
    int compare(Object left, Object right) {
        return right.length().compareTo(left.length())
    }
}

orgEntityAlias = []

month_map =
    [
        JAN: '01',
        FEB: '02',
        MAR: '03',
        APR: '04',
        MAY: '05',
        JUN: '06',
        JUL: '07',
        AUG: '08',
        SEP: '09',
        OCT: '10',
        NOV: '11',
        DEC: '12'
    ]
indx_url = "https://home.treasury.gov/policy-issues/financial-sanctions/specially-designated-nationals-list-sdn-list/archive-of-changes-to-the-sdn-list"
base_url = "https://www.treasury.gov"
//indx_url = "resource-center/sanctions/SDN-List/Pages/archive.aspx"
//indx_url = "policy-issues/financial-sanctions/specially-designated-nationals-list-sdn-list/archive-of-changes-to-the-sdn-list"
//https://home.treasury.gov/policy-issues/financial-sanctions/specially-designated-nationals-list-sdn-list/archive-of-changes-to-the-sdn-list
change_date_pattern = "\\d{2}/\\d{2}/\\d{2,4}:"
section_pattern_94 = "(?:THE FOLLOWING NAMES HAVE BEEN REMOVED FROM THE SDNLIST--|Delete all of the following:)(.*?)[\\*]{0,2}##-##"
old_section_pattern = "Deletion of the following (?:names|name) from Treasury.*?published in the Federal Register:(.*?)##-##"
section_pattern = "[Tt]he following .*?(?:entries|entry|names|name|individual|individuals) ha(?:ve|s) (?:therefore |)been (?:removed|deleted).*?[:-](.*?)##-##"
section_pattern_2004 = "(?is)[Tt]he following .*?(?:entries|entry|names|name|individual|individuals) ha(?:ve|s) (?:therefore |)been (?:removed|deleted).*?[:-](.*?)##-##"
section_pattern_2019 = "(?is)[Tt]he following .*?(?:entries|entry|names|name|individual|individuals) ha(?:ve|s).*(?:removed\\s*|deleted).*?[:-](.*?)##-##"
alias_pattern = "[afn][.]k[.]a[.]*\\s*(.*?)[);]"
dob_pattern = "DOB\\s+(.*?)(?:;|\\(|\$)"
event_desc = "This entity was removed from the US Treasury Department's OFAC SDN list"


duplicate_name_list = [] as Set


duplicate_name_list.add("BIN MUJAHIR")
duplicate_name_list.add("BIN MUHADJR")
duplicate_name_list.add("BIN MUHADJIR")
duplicate_name_list.add("JUMALE")
duplicate_name_list.add("JUMALI")
duplicate_name_list.add("ALI HAJINIA LEILABADI")
duplicate_name_list.add("ALIAS")
duplicate_name_list.add("GHALEHBANI")
duplicate_name_list.add("QALEHBANI")
duplicate_name_list.add("BIZRI")
duplicate_name_list.add("EZZATI")
duplicate_name_list.add("EZATI")
duplicate_name_list.add("ANAPIYAEV")
duplicate_name_list.add("ANAPIYEV")
duplicate_name_list.add("PACHECO PARRA")
duplicate_name_list.add("AL-SABAI")
duplicate_name_list.add("AL-LIBY")
duplicate_name_list.add("NAZIH ABDUL HAMED AL-RAGHIE")
duplicate_name_list.add("Zynthya")
duplicate_name_list.add("Youssef NADA")
duplicate_name_list.add("YASSER MOHAMED")
duplicate_name_list.add("Vitaliy SOKOLENKO")
duplicate_name_list.add("Vitaly SOKOLENKO")
duplicate_name_list.add("Ruth Hilda Rose")
duplicate_name_list.add("Rosanne Phyllis")

total_alias_list = [] as Set
deleted_names = [] as Set

retracted_alias_list = [] as Set
cumulative = 0

if (false) {
    processLinks(["file:///c:/sdnew94.txt"])
} else {
    process()
}

for (entity in context.session.getEntitiesNonCache()) {
    names = []
    names.add(entity.name)
    if (entity.getAliases()) {
        names.addAll(entity.getAliases())
        entity.getAliases().remove(entity.getName())
    }
    newAddresses = []
    nameArray = []

    for (name in names) {
        variants = makeAllNameVariants(name)
        if (variants) {
            nameArray.addAll(variants)
        }
    }
    Collections.sort(names, comparator)
    Collections.sort(nameArray, comparator)

    entName = ''
    for (name in names) {
        entName += cleanNameForRegex(name) + "|"
    }
    entName = entName.replaceAll(/\|$/, '')
    name = ''
    for (named in nameArray) {
        name += cleanNameForRegex(named) + "|"
    }
    name = name.replaceAll(/\|$/, '')
    if (name == '') {
        name = cleanNameForRegex(entity.name)
    }

    addresses = entity.getAddresses()
    for (address in addresses) {
        if (address.address1) {
            address.address1 = address.address1
                .replaceAll(/(?i)(?:${entName})(?:,|\s*\(.*?\),*|\s)/, "")
                .replaceAll(/(?i)(?:${name})(?:,|\s*\(.*?\),*|\s)/, "").trim()
        }
        newAddresses.add(address)
    }

    entity.setAddresses(new ArrayList<ScrapeAddress>())
    for (address in newAddresses) {
        entity.addAddress(address)
    }
}

def cleanNameForRegex(String nameIn) {
    return nameIn.replaceAll(/\(/, '\\\\(')
        .replaceAll(/\)/, '\\\\)')
        .replaceAll(/-\s/, '-(?:\\\\s|)')
        .replaceAll(/\./, '\\\\.')
        .replaceAll(/\[/, '\\\\[')
        .replaceAll(/\]/, '\\\\]')
        .replaceAll(/\*/, '\\\\*')
        .replaceAll(/\+/, '\\\\+')
        .replaceAll(/\?/, '\\\\?')
}

def processLinks(List links) {
    links.eachWithIndex { url, i ->
        println("${++i}. LINK: $url")

        def data = fetch_data([url: url, type: 'get'])


        if (url.contains("new16") || url.contains("new15") || url.contains("new17") || url.contains("new19")) {
            context.info("Calling new due to being 2015/16/17")
            create_entities_new_(data, url)
        } else {
            context.info("Calling OLD due to not being in 2015/16/17")
            create_entities_(data, url)
        }
    }
}

def process() {
//    def data = fetch_data([url: base_url + "/" + indx_url, type: 'get'])
    def data = fetch_data([url: indx_url, type: 'get'])
    def links = parse_links(data)
    def i = 1

    //println data
    println "----" + links + "----"
    links.each
        {
            def link = (it.startsWith(base_url) ? "" : base_url + "/") + it
            if (link =~ /sdnew12.txt/) {
                println "www"
            }
            link = link.replaceAll("//", "/").replaceAll("(http[s]*):/", "\$1://")
            println("${i++} LINK $link")

            // here follow link, extract data and call create_entities
            data = fetch_data([url: link, type: 'get'])
            if (link.contains("new16") || link.contains("new15") || link.contains("new17") || link.contains("new19")) {
                context.info("Calling new due to being 2015/16/17")
                create_entities_new_(data, link)
            } else {
                context.info("Calling OLD due to not being in 2015/16/17")
                create_entities_(data, link)
            }
        }

    entity = context.getSession().newEntity()

    entity.setName('WAHDA BANK')
    entity.setType("O")
    entity.addUrl("https://www.treasury.gov/resource-center/sanctions/SDN-List/Documents/sdnew04.txt")
    se = new ScrapeEvent()
    se.setDescription('This entity was removed from the US Treasury Department\'s OFAC SDN list')
    se.setDate('04/29/2004')
    entity.addEvent(se)
    sa = new ScrapeAddress()
    sa.setCity("Tripoli")
    sa.setCountry("LIBYA")
    entity.addAddress(sa)
    sa = new ScrapeAddress()
    sa.setAddress1("P.O. Box 452, Fadiel Abu Omar Square, El-Berhka")
    sa.setCity("Benghazi")
    sa.setCountry("LIBYA")
    entity.addAddress(sa)
    sa = new ScrapeAddress()
    sa.setCity("Benghazi")
    sa.setCountry("LIBYA")
    entity.addAddress(sa)
}
//-----------------------------------------------------------------------------
def parse_links(data) {
    def match = data =~ /href="(.*?.txt)".*?SDN Changes/
    def links = [] as Set

    while (match.find()) {
        links.add(match.group(1).replace("\"", "").trim())
    }

    return links
}
//-----------------------------------------------------------------------------

def getEntitiesByName(def name) {
    def foundEntitesList
    foundEntitesList = context.getSession().getEntitiesByName(name)

    return foundEntitesList
}

def mergeEntityWithRemovedEntity(def removeEntityList, def entity) {
    removeEntityList.each { rEntity ->
        //context.info("merging " + rEntity.name + " to " + entity.name)
        List<ScrapeEvent> events = new ArrayList()
        events = rEntity.getEvents()
        Set<ScrapeIdentification> ids = rEntity.getIdentifications()
        def urls = rEntity.getUrls()
        List<ScrapeAddress> addrs = rEntity.getAddresses()
        List<ScrapeDob> dobs = rEntity.getDateOfBirths()
        def aliases = rEntity.getAliases()

        events.each { def event ->
            entity.addEvent(event)
        }

        dobs.each { def dob ->
            entity.addDateOfBirth(dob)
        }

        urls.each { def url ->
            entity.addUrl(url)
        }

        ids.each { def id ->
            entity.addIdentification(id)
        }

        aliases.each { def alias ->
            entity.addAlias(alias)
            //context.info("Adding alias " + alias + " for merge to " + entity.name)
        }
    }
}

def createEntityCommonCoreAlreadyExist(def entity, def section, def entityUrl) {
    def event = new ScrapeEvent()

    if (section.event_date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm/dd/yy")
        resultDate = simpleDateFormat.parse(section.event_date)
        event.setDate(resultDate.format("mm/dd/yyyy"))
    }
    event.setDescription(event_desc)
    entity.addEvent(event)

    parse_dob(section.data).each
        {
            if (it) {
                dob = format_date(stripCirca(it))
                sdob = new ScrapeDob()
                splitDate = dob.split("/")
                if (splitDate[0].equals("-") || splitDate[1].equals("-")) {
                    sdob.setCirca(true)
                }
                if (!splitDate[0].equals("-")) {
                    sdob.setMonth(splitDate[0])
                }
                if (!splitDate[1].equals("-")) {
                    sdob.setDay(splitDate[1])
                }

                sdob.setYear(splitDate[2])
                entity.addDateOfBirth(sdob)
            }
        }
    entity.addUrl(entityUrl)
    parse_address_new(entity, section.data)
    parse_position_new(entity, section.data)

}

def createEntityCommonCore(def entity, def section, def entityUrl) {
    def event = new ScrapeEvent()

    if (section.event_date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm/dd/yy")
        resultDate = simpleDateFormat.parse(section.event_date)
        event.setDate(resultDate.format("mm/dd/yyyy"))
    }
    event.setDescription(event_desc)
    entity.addEvent(event)

    parse_dob(section.data).each
        {
            if (it) {
                dob = format_date(stripCirca(it))
                sdob = new ScrapeDob()
                splitDate = dob.split("/")
                if (splitDate[0].equals("-") || splitDate[1].equals("-")) {
                    sdob.setCirca(true)
                }
                if (!splitDate[0].equals("-")) {
                    sdob.setMonth(splitDate[0])
                }
                if (!splitDate[1].equals("-")) {
                    sdob.setDay(splitDate[1])
                }
                sdob.setYear(splitDate[2])
                entity.addDateOfBirth(sdob)
            }
        }
    entity.addUrl(entityUrl)
    parse_identification_new(entity, section.data)
    parse_address_new(entity, section.data)
    parse_position_new(entity, section.data)

}

def sanitizeName(value) {
    return value
        .replaceAll(/(?i):|(?<=^|\s+)\.\s+/, '')
        .replaceAll(/\n|\r|\(.*?\)/, ' ')
        .replaceAll(/\s{2,}/, '')
        .replaceAll(/(?i),\s*P\.*O\.*\s*box.*/, '')
        .replaceAll(/(?i)[Afn][.]K[.]A[.]/, '')
        .replaceAll(/(?is)\schemical\\/oil tanker italy.*/, '')
        .replaceAll(/(?is); vessel.*\u0024/, '')
        .replaceAll(/(?is)(?:bulk|chemical\\/products|crude oil).*?\u0024/, '')
        .trim().toUpperCase()
}

//-----------------------------------------------------------------------------
//-- remove all parsed sections such as name, id, etc. remainder should be
//-- address
//-----------------------------------------------------------------------------
def parse_address(entity, data) {
    if (StringUtils.isBlank(data)) {
        return
    }

    def text = data.replaceAll("\r", " ")
        .replaceAll("\n", " ")
        .replaceAll("National\\s+ID\\s*.*?;", "")
        .replaceAll("POB\\s*.*?(;|\\z)", "")
        .replaceAll("DOB\\s*.*?(?:;|\$|\\()", "")
        .replaceAll("Passport\\s*.*?(;|\\z)", "")
        .replaceAll("Business\\s+Registration\\s+Document.*?(;|\\[|\\z)", "")
        .replaceAll("Vessel\\s+Registration\\s+Identification.*?(?=\\()", "")
        .replaceAll("citizen\\s*.*?;", "")
        .replaceAll("nationality\\s*.*?;", "")
        .replaceAll("^.*?,.*?[(,]", "")           // name
        .replaceAll("\\(?\\s*[afn][.]k[.]a.*?\\)\\s*[;,]*", "") // alias
        .replaceAll("\\(\\s*.*?\\s*\\)", "")       // parentheses
        .replaceAll(".*\\s+flag", "")
        .replaceAll("alt\\s*[.]*|:|\\s+The\\s+", "")
        .replaceAll(".*?\\s*(Politburo|Minister|General)\\s*.*", "")
        .replaceAll("\\(?\\s*(?:individual)\\s*\\)?", "")
        .replaceAll("\\s{2,}", " ")
        .replaceAll("Height .*? inches", "")
        .trim()

    if (StringUtils.isBlank(text)) {
        return
    }

    //println "\nADDRESS TEXT $text \n";

    def s = text.split(";")

    for (ts in s) {
        //println( "\t-> $ts" );

        ss = ts.split(",")
        ss[0] = ss[0].trim()

        if (ss.length < 2) {
            if (ss.length == 1 && find_country(ss[0])) {
                entity.newAddress().setCountry(ss[0])
            }
            continue
        }

        def addr = null
        rawAddr = sanitize(ts.length() < 200 ? ts : ts.substring(0, 199))
            .replaceAll(/(?i)Owner.*?$|,?\s*undetermined/, '')
            .replaceAll(/(?i)\s*\(.*?\)\s*|\s*(?:ex-|transactions|disputed).*/, '')
            .replaceAll(/\s{2,}/, ' ')
            .trim()

        if (StringUtils.isNotBlank(rawAddr)) {
            addr = entity.newAddress()
            addr.setRawFormat(rawAddr)
        }
        if (find_country(ss[0])) {
            if (addr == null) {
                addr = entity.newAddress()
            }
            addr.setCountry(ss[ss.length - 1].trim())
        }

        //ss.each{ println "\t  -> ${it.trim()}"; };
    }
}

def makeAllNameVariants(nameIn) {
    //context.info('manv input = ' + nameIn)
    myNameArray = []
    splitUp = nameIn.split(" ")
    if (splitUp.length > 1) {
        for (int j = 1; j <= splitUp.length - 1; j++) {
            name = ''
            for (int k = -j; k <= -1; k++) {
                name += (k != -j ? ' ' : '') + splitUp[splitUp.length + k]
            }
            for (int i = 0; i < splitUp.length - j; i++) {
                name += (i == 0 ? ', ' : ' ') + splitUp[i]
            }
            myNameArray.add(name)

        }
    }
    //context.info('manv output = ' + myNameArray.toString())
    return myNameArray
}

def parse_address_new(entity, data) {
    //context.info(entity.name + '!!!' + data.toString())
    entNameArray = []
    nameArray = []
    entNameArray.add(entity.getName())
    if (entity.getAliases()) {
        entNameArray.addAll(entity.getAliases())
    }

    for (name in entNameArray) {
        variants = makeAllNameVariants(name)
        if (variants) {
            nameArray.addAll(variants)
        }
    }

    Collections.sort(entNameArray, comparator)
    Collections.sort(nameArray, comparator)
    entName = ''
    for (name in entNameArray) {
        entName += cleanNameForRegex(name) + "|"
    }
    entName = entName.replaceAll(/\|$/, '')
    name = ''
    for (named in nameArray) {
        name += cleanNameForRegex(named) + "|"
    }
    name = name.replaceAll(/\|$/, '')
    if (name == '') {
        name = cleanNameForRegex(entity.name)
    }
    //context.info(entity.name + '!' + entName + '!!' + name + '!!!' + data.toString())
    if (StringUtils.isBlank(data)) {
        return
    }
    def text = data.replaceAll("\r", " ")
        .replaceAll("\n", " ")
        .replaceAll(/(?i)(?:${name})(?:,|\s*\(.*?\),*|\s)/, "")
        .replaceAll(/(?i)(?:${entName})(?:,|\s*\(.*?\),*|\s)/, "")
        .replaceAll("National\\s+ID\\s*.*?;", "")
        .replaceAll("POB\\s*.*?(;|\\z)", "")
        .replaceAll("DOB\\s*.*?(?:;|\$|\\()", "")
        .replaceAll("Passport\\s*.*?(;|\\z)", "")
        .replaceAll("(?i)for\\s*more\\s*information\\s*.*?pdf;?", "")
        .replaceAll("Business\\s+Registration\\s+Document.*?(;|\\[|\\z)", "")
        .replaceAll("Vessel\\s+Registration\\s+Identification.*?(?=\\()", "")
        .replaceAll("citizen\\s*.*?(?:;|\$)", "")
        .replaceAll("nationality\\s*.*?(?:;|\$)", "")
    //.replaceAll("^[^\\d]+[,\\s]*[^\\d#]+[(,]", "")
        .replaceAll("\\(?\\s*[afn][.]k[.]a.*?\\)\\s*[;,]*", "") // alias
        .replaceAll("\\(\\s*.*?\\s*\\)", "")       // parentheses
        .replaceAll(".*\\s+flag", "")
        .replaceAll("alt\\b\\s*[.]*|:|\\s+The\\s+", "")
        .replaceAll(".*?\\s*(Politburo|Minister|General)\\s*.*", "")
        .replaceAll("\\(?\\s*(?:individual)\\s*\\)?", "")
        .replaceAll("\\bvice chairman.*", "")
        .replaceAll("\\s{2,}", " ")
        .replaceAll("Height .*? inches", "")
        .replaceAll(/(?i)Intertrade\s+Company|Director/, "")
        .replaceAll(/(?i)Hong Kong[^\w;]+China/, "Hong Kong")
        .trim()

    if (StringUtils.isBlank(text)) {
        //context.info('was blank');
        return
    }

    //context.info(entity.name + '!!2' + text.toString())
    def s = text.split(";")
    for (ts in s) {
        def ss = ts
            .replaceAll(/(?i).*?is a separate.*?entity.++$/, "")
            .split(",")
        ss[0] = ss[0].trim()

        if (ss.length < 2) {
            if (ss.length == 1 && ss[0].length() > 0 && !(ss[0].contains("elephone") || ss[0].toUpperCase().contains("FAX"))) {
                def country = ''
                res = genericAddressParser.findCountry(ss[0])
                if (res[tokens.COUNTRY]) {
                    country = res[genericAddressParser.getTokens().COUNTRY]
                }
                if (country) {
                    def cAddress = new ScrapeAddress()
                    cAddress.setCountry(country)
                    entity.addAddress(cAddress)
                }
            }
            continue
        }

        def rawAddr = sanitize(ts.length() < 200 ? ts : ts.substring(0, 199))
            .replaceAll(/(?i)Owner.*?$|,?\s*undetermined/, '')
            .replaceAll(/(?i)\s*\(.*?\)\s*|\s*(?:ex-|transactions|disputed).*/, '')
            .replaceAll(/\bCEP\b/, "")
            .replaceAll(/\s{2,}/, ' ')
            .trim()

        if (StringUtils.isNotBlank(rawAddr)) {
            rawAddr = fixState(rawAddr)
            //returns: [country, state, city, zip, remaining-address] (if country's matched)
            addrList = genericAddressParser.parseAddress([text: rawAddr, force_country: true, ignored_cities: getIgnoredCitiesTokens()])
            if (addrList.size() > 2) {//&& !addrList[tokens.DELIMITER]) {

                def address = new ScrapeAddress()
                if (addrList[tokens.COUNTRY]) {
                    if (StringUtils.equalsIgnoreCase("NORTH SUDAN", addrList[tokens.COUNTRY])) {
                        address.country = "SUDAN"
                    } else {
                        address.country = addrList[tokens.COUNTRY]
                    }
                }

                if (addrList[tokens.STATE]) {
                    address.province = sanitize(addrList[tokens.STATE])
                }

                if (addrList[tokens.CITY]) {
                    if (StringUtils.equalsIgnoreCase("KOREA, REPUBLIC OF", addrList[tokens.COUNTRY])) {
                        address.city = rawAddr.find(/[^,]+(?=,\s+Korea,\s+South)/).replaceAll(/\d-*/, "").trim()
                    } else {
                        address.city = sanitize(addrList[tokens.CITY])
                    }
                }

                if (addrList[tokens.ZIP]) {
                    address.postalCode = addrList[tokens.ZIP]
                }

                def street = ''

                if (addrList[tokens.ADDR_STR]) {
                    if (StringUtils.equalsIgnoreCase("KOREA, REPUBLIC OF", addrList[tokens.COUNTRY])) {
                        addrList[tokens.ADDR_STR] = rawAddr.find(/.*(?=,\s$address.city)/)
                    }
                    street = addrList[tokens.ADDR_STR]
                        .replaceAll(/(?s)\s+/, " ")
                        .replaceAll(/^([^\(\)]++)\)/, '$1')
                        .replaceAll(/\(([^\(\)]++)$/, '$1')
                        .replaceAll(/(?i)P\W*O\W+Box\W*$/, "")
                        .replaceAll(/^[\s,-]+|\W+$/, "")
                        .replaceAll(/(?m)^[\),\s]*/, "")
                        .replaceAll(/(?i)^\s*(?:major|ltd\.?)\s*$/, "")
                        .replaceAll(/^(.*)$/, ', $1')
                    if (street.find(/333 7th Ave SW #1102, AB/)) {
                        street = "333 7th Ave SW #1102"
                        address.province = "Alberta"
                    } else if (street.find(/Rua Doutor Fernando Arens 679, Artur Nogueira/)) {
                        street = "Rua Doutor Fernando Arens 679"
                        address.city = "Artur Nogueira"
                        address.province = "SAO PAULO"
                    }
                    if (street) {
                        address.address1 = street
                    }
                }
                entity.addAddress(address)
            }
        }
    }
}

def fixState(def addr) {
    if (addr =~ /\bON\b/) {
        return addr.toString().replaceAll(/\bON\b/, "Ontario")
    } else if (addr =~ /(?i)\bFL\b/) {
        return addr.toString().replaceAll(/(?i)\bFL\b/, "florida")
    } else if (addr =~ /(?i)\bBC\b/) {
        return addr.toString().replaceAll(/(?i)\bBC\b/, "British Columbia")
    } else if (addr =~ /(?i)\bIL\b/) {
        return addr.toString().replaceAll(/(?i)\bIL\b/, "illinois")
    } else if (addr =~ /(?i)\bMA\b/) {
        return addr.toString().replaceAll(/(?i)\bMA\b/, "Massachusetts")
    } else if (addr =~ /(?i)\bTX\b/) {
        return addr.toString().replaceAll(/(?i)\bTX\b/, "Texas")
    } else if (addr =~ /(?i)\bCA\b/) {
        return addr.toString().replaceAll(/(?i)\bCA\b/, "California")
    } else {
        return addr
    }


}

def parse_position_new(entity, data) {
    if (StringUtils.isBlank(data)) {
        return
    }
    def text = data.replaceAll("\r", " ")
        .replaceAll("\n", " ")
        .replaceAll("National\\s+ID\\s*.*?;", "")
        .replaceAll("POB\\s*.*?(;|\\z)", "")
        .replaceAll("DOB\\s*.*?(?:;|\$|\\()", "")
        .replaceAll("Passport\\s*.*?(;|\\z)", "")
        .replaceAll("(?i)for\\s*more\\s*information\\s*.*?pdf;?", "")
        .replaceAll("Business\\s+Registration\\s+Document.*?(;|\\[|\\z)", "")
        .replaceAll("Vessel\\s+Registration\\s+Identification.*?(?=\\()", "")
        .replaceAll("citizen\\s*.*?;", "")
        .replaceAll("nationality\\s*.*?;", "")
        .replaceAll("^[^\\d]+[,\\s]*[^\\d#]+[(,]", "")
        .replaceAll("\\(?\\s*[afn][.]k[.]a.*?\\)\\s*[;,]*", "") // alias
        .replaceAll("\\(\\s*.*?\\s*\\)", "")       // parentheses
        .replaceAll(".*\\s+flag", "")
        .replaceAll("alt\\b\\s*[.]*|:|\\s+The\\s+", "")
        .replaceAll("\\(?\\s*(?:individual)\\s*\\)?", "")
        .replaceAll("\\bvice chairman.*", "")
        .replaceAll("\\s{2,}", " ")
        .replaceAll("Height .*? inches", "")
        .replaceAll(/(?i)Hong Kong[^\w;]+China/, "Hong Kong")
        .trim()

    if (StringUtils.isBlank(text)) {
        return
    }

    def s = text.split(";")
    for (ts in s) {
        def ss = ts
            .replaceAll(/(?i).*?is a separate.*?entity.++$/, "")
            .replaceAll(/(?i),?\s*undetermined/, '')
            .replaceAll(/(?i)\s*\(.*?\)\s*|\s*(?:ex-|transactions|disputed).*/, '')
            .replaceAll(/\s{2,}/, ' ')
            .trim()

        if (ss.contains("Director")) {
            entity.addPosition(ss)
        }
    }
}
//-----------------------------------------------------------------------------
def parse_event_date(data) {
    def match = get_match("^\\s*\\d{1,2}/\\d{1,2}/\\d{1,4}", data)

    while (match.find()) {
        return match.group(0).trim()
    }
    return null
}

def getEntityType(data) {
    match = get_match("(?i)\\(\\s*individual\\s*\\)|(?<=^|\\s+?)(?:hand\\s+)|(?:;\\s+DOB\\s)|(?:HAND, Michael Brian)", sanitize(data))
    while (match.find()) {
        return "P"
        break
    }
    return "O"
}

//-----------------------------------------------------------------------------
def parse_identification(entity, data) {
    def text = data.replaceAll("\r", " ").replaceAll("\n", " ")
    def match, id

    ["(Passport)\\s+(.*?)\\s*\\((.*?)\\)",
     "(National\\s+ID)\\s+(?:No[.]?)?\\s+(.*?)\\s*\\((.*?)\\)",
     "(Business\\s+Registration\\s+Document)\\s+[#]?\\s*([A-Za-z0-9]+)(\\(\\s*[A-Za-z]+\\s*\\))?",
     "(Vessel\\s+Registration\\s+Identification)\\s+(?:IMO)?\\s+([a-zA-Z0-9]+)\\s*(?:\\((.*?)\\))",
     "(V.A.T. Number)\\s+(.*?)\\s*\\((.*?)\\)"
    ].each
        {
            match = get_match(it, text)

            while (match.find()) {
                def type, val, country

                type = sanitize(match.group(1))//.trim()
                val = sanitize(match.group(2))

                if (match.groupCount() > 2 && StringUtils.isNotBlank(match.group(3))) {
                    country = match.group(3).trim()
                }

                if (StringUtils.isBlank(type) || StringUtils.isBlank(val)) {
                    continue
                }

                id = entity.newIdentification()
                id.setType(type)
                id.setValue(val)
                if (StringUtils.isNotBlank(country)) {
                    id.setCountry(country)
                }
            }
        }

    if (!id) {
        match = get_match("\\(\\s*vessel\\s*\\)", text)

        while (match.find()) {
            id = entity.newIdentification()

            id.setType("Vessel Registration Identification")

            match = get_match("(?:;|\\s+)(\\w+?)\\s+flag", text)

            while (match.find()) {
                id.setCountry(match.group(1))
                break
            }

            id.setValue("NO ID")

            return
        }
    }
    if (id && StringUtils.isBlank(id.getValue())) {
        id.setValue("NO ID")
    }

    match = get_match("nationality\\s+(.*?);", text)

    while (match.find()) {
        entity.addNationality(sanitize(match.group(1)))
    }

    match = get_match("citizen\\s+(.*?)(;|\\(individual\\))", text)

    while (match.find()) {
        entity.addCitizenship(sanitize(match.group(1)))
    }
}

def parse_identification_value(data) {
    def text = data.replaceAll("\r", " ").replaceAll("\n", " ")
    def match, val, imoNo
    ScrapeIdentification id
    //context.info(entity.name + ": Parse id data: " + data)

    ["(Passport)\\s+(.*?)\\s*\\((.*?)\\)", "(National\\s+ID)\\s+(?:No[.]?)?\\s+(.*?)\\s*\\((.*?)\\)", "(Business\\s+Registration\\s+Document)\\s+[#]?\\s*([A-Za-z0-9]+)(\\(\\s*[A-Za-z]+\\s*\\))?", "(Vessel\\s+Registration\\s+Identification)\\s+(IMO\\s+[a-zA-Z0-9]+)\\s*\\((.*?)\\)"
     , "(Vessel\\s+Registration\\s+Identification)\\s+(IMO\\s+[a-zA-Z0-9]+);"
     , "(MMSI\\s+[0-9]+)"].each
        {
            match = get_match(it, text)

            while (match.find()) {
                def type

                type = match.group(1).trim()

                if (type.contains("MMSI")) {
                    val = sanitize(match.group(1))
                } else {
                    imoNo = sanitize(match.group(2))
                }
            }
        }
    return imoNo
}

def parse_identification_new(entity, data) {
    def text = data.replaceAll("\r", " ").replaceAll("\n", " ")
    def match
    ScrapeIdentification id
    //context.info(entity.name + ": Parse id data: " + data)

    ["(Passport)\\s+(.*?)\\s*\\((.*?)\\)", "(V.A.T. Number)\\s+(.*?)\\s*\\((.*?)\\)", "(National\\s+ID)\\s+(?:No[.]?)?\\s+(.*?)\\s*\\((.*?)\\)", "(Business\\s+Registration\\s+Document)\\s+[#]?\\s*([A-Za-z0-9]+)(\\(\\s*[A-Za-z]+\\s*\\))?", "(Vessel\\s+Registration\\s+Identification)\\s+(IMO\\s+[a-zA-Z0-9]+)\\s*\\((.*?)\\)"
     , "(Vessel\\s+Registration\\s+Identification)\\s+(IMO\\s+[a-zA-Z0-9]+);"
     , "(MMSI\\s+[0-9]+)"].each
        {
            match = get_match(it, text)

            while (match.find()) {
                def type, val, country

                type = match.group(1).trim()

                if (type.contains("MMSI")) {
                    val = sanitize(match.group(1))
                    type = "Vessel Registration Identification"
                } else {
                    val = sanitize(match.group(2))
                }

                if (match.groupCount() > 2 && StringUtils.isNotBlank(match.group(3)) && match.group(3) != 'vessel') {
                    if (!match.group(3).trim().contains("individual"))
                        country = match.group(3).trim()
                }

                if (StringUtils.isBlank(type) || StringUtils.isBlank(val)) {
                    continue
                }
                /* def aliases = entity.getAliases()
                 def alias
                 aliases.each{
                     if (it.contains("IMO")){
                         alias = it
                     }
                 }*/
                id = new ScrapeIdentification()
                if (type =~ /(?i)vessel registration/) {
                    entity.addAlias(val)
                }
                id.setType(sanitize(type))
                id.setValue(val)

                if (StringUtils.isNotBlank(country)) {
                    id.setCountry(country)
                }
                entity.addIdentification(id)
            }
        }

    match = get_match("(?is)bulk carrier|CRUDE oil tanker|CHEMICAL\\/PRODUCTS TANKER", text)
    while (match.find()) {
        match = get_match("(?is)(bulk carrier|crude oil tanker|chemical\\/products tanker)", text)
        if (match.find()) {
            entity.addRemark(sanitize(match.group(1)))
        }
    }

    match = get_match("\\(\\s*vessel\\s*\\)", text)
    while (match.find()) {

        match = get_match("(?i)GRT\\s(.*?)\\s+flag", text)

        while (match.find()) {
            if (match.group(1) != 'Identified' && match.group(1) != 'Unknown' && match.group(1) != 'Vessel') {
                entity.addRemark("Vessel Flag: " + sanitize(match.group(1)))
            }
        }

        match = get_match("(?is)Former\\s+Vessel\\s+Flag\\s(.*?);", text)
        while (match.find()) {
            if (match.group(1) != 'Identified' && match.group(1) != 'Unknown' && match.group(1) != 'Vessel') {
                entity.addRemark("Former Vessel Flag: " + sanitize(match.group(1)))
            }
        }
    }

    match = get_match("\\(\\s*aircraft\\s*\\)", text)

    while (match.find()) {

        match = get_match("(?is)Aircraft\\sConstruction\\sNumber\\s*(?:\\(.*?\\)|)\\s*(.*?)(?:;|\\(|\$)", text)

        while (match.find()) {
            if (match.group(1) != '' && match.group(1) != 'Identified' && match.group(1) != 'Unknown' && match.group(1) != 'Aircraft') {
                id = new ScrapeIdentification()
                id.setType("Aircraft Construction Number (L/N, S/N, or F/N)")
                id.setValue(match.group(1).trim())
                entity.addIdentification(id)
            }
        }

        match = get_match("(?is)Aircraft\\sManufacturer.s\\sSerial\\sNumber\\s*(?:\\(.*?\\)|)\\s*(.*?)(?:;|\\(|\$)", text)
        while (match.find()) {
            if (match.group(1) != '' && match.group(1) != 'Identified' && match.group(1) != 'Unknown' && match.group(1) != 'Aircraft') {
                id = new ScrapeIdentification()
                id.setType("Aircraft Manufacturer's Serial Number (MSN)")
                id.setValue(match.group(1).trim())
                entity.addIdentification(id)
            }
        }

        match = get_match("(?is)Aircraft\\sManufacture\\sDate\\s(.*?)(?:;|\\(|\$)", text)
        while (match.find()) {
            if (match.group(1) != '' && match.group(1) != 'Identified' && match.group(1) != 'Unknown' && match.group(1) != 'Aircraft') {
                entity.addRemark("Aircraft Manufacture Date: " + sanitize(match.group(1)))
            }
        }
        match = get_match("(?is)Aircraft\\sModel\\s(.*?)(?:;|\\(|\$)", text)
        while (match.find()) {
            if (match.group(1) != '' && match.group(1) != 'Identified' && match.group(1) != 'Unknown' && match.group(1) != 'Aircraft') {
                entity.addRemark("Aircraft Model: " + sanitize(match.group(1)))
            }
        }
        match = get_match("(?is)Aircraft\\sOperator\\s(.*?)(?:;|\\(|\$)", text)
        while (match.find()) {
            if (match.group(1) != '' && match.group(1) != 'Identified' && match.group(1) != 'Unknown' && match.group(1) != 'Aircraft') {
                entity.addRemark("Aircraft Operator: " + sanitize(match.group(1)))
            }
        }
    }


    match = get_match("nationality\\s+(.*?);", text)

    while (match.find()) {
        entity.addNationality(sanitize(match.group(1)))
    }

    match = get_match("nationality\\s+(.*?)(?:;|\\(individual\\))", text)

    while (match.find()) {
        entity.addNationality(sanitize(match.group(1)))
    }

    match = get_match("citizen\\s+(.*?)(?:;|\\(individual\\))", text)

    while (match.find()) {
        entity.addCitizenship(sanitize(match.group(1)))
    }
}

def getIgnoredCitiesTokens() {
    def tokens = [/P\W*O\W+Box/, /Township/, /Avenue/, /Airport/, /Sq/, /Street/, /York/, /road/]

    return tokens
}
//-----------------------------------------------------------------------------
def parse_dob(data) {
    dobs = [] as Set

    def match = get_match(dob_pattern, data)

    while (match.find()) {
        dobs.add(sanitize(match.group(1)))
    }
    return dobs
}
//-----------------------------------------------------------------------------
def parse_alias(data) {
    aliases = [] as Set

    def match = get_match(alias_pattern, data.replaceAll("\n", " ").replaceAll("\r", " "))

    while (match.find()) {
        def alias = ""
        if (match.group(1)) {
            alias = match.group(1).trim().replaceAll("\n", " ").replaceAll("\r", "")
            def ss = alias.split(",")
            alias = (ss.length > 1) ? (ss[1] + ", " + ss[0]) : ss[0]

            //println "ALIAS $alias\n";
            aliases.add(sanitize(alias))
        }
    }

    return aliases
}

def parse_alias_new(data, type) {
    aliases = [] as Set
    def match = get_match(alias_pattern, data.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\\u0022", ""))

    while (match.find()) {
        def alias = ""
        if (match.group(1)) {
            alias = match.group(1).trim().replaceAll("\n", " ").replaceAll("\r", "")
            if (type.equals("P")) {
                def ss = alias.split(",")
                alias = (ss.length > 1) ? (ss[1] + " " + ss[0]) : ss[0]
            }

            aliases.add(sanitize(alias))
        }
    }

    return aliases
}
//-----------------------------------------------------------------------------
def parse_name(data) {
    def match
    /* if (data =~ /(?ism)VOYAGER I/) {
        println("D")
    }*/
    def result = []
    def text = sanitize(data)
        .replaceAll("\\s*[-]*\\s*licensed\\s+pending\\s+removal\\s+by\\s+FAC\\s*[-]*\\s*", "").replaceAll(/PCC \(UK\) \(a\.k\.a\./, "PCC UK (a.k.a.")
    //     .replaceAll(/(?i).*?(?:Islam|Jihad|Palestine|Muslim|Allah|M[uo]hammad).*/, '')       // why are we ignoring these entities?
    //     .replaceAll(/(?i)along\s+with.*?:\s+|(?:^\s+and|\s+-TO-)\s+|.*?(?:\s+FINANCIAL|SPELLING|INFORMATION)\s+.*/, '')  // and these?
        .replaceAll(/(?i)along\s+with.*?:\s+|(?:^\s+and|\s+-TO-)\s+|.*?(?:\s+SPELLING)\s+.*/, '')
        .replaceAll(/(?i)HAS\s+.*?\s+TO\s*|-{2,}|\d+\s+\w+\s+\d+/, '')
        .replaceAll(/(?is)LE DEVELOPPEMENT ET LA\s*COMMERCIALISATION DES PRODUTS AGRICOLES ET D'ELEVAGE/, "LE DEVELOPPEMENT ET LA COMMERCIALISATION")
        .replaceAll(/(?is)PROMOTION HOTELIERE ET\s*touristique au rwanda/, "PROMOTION HOTELIERE ET touristique au rwanda")
    //.replaceAll(/(?i)\d+.*?(?:,|;|$|\s)/, '');
    // println( "NAME TEXT: $text  ORIGINAL DATA: $data\n" );
    textMatch = text =~ /^[a-zA-Z]\d+.*?(?:,|;|$|\s)/
    if (textMatch) {
        tempText = text.replaceAll(textMatch[0], "")
        tempText = tempText.replaceAll(/(?i)(?<!no\.\s|neka\s)\b\d+.*?(?:,|;|\u0024|\s)/, '');
        text = textMatch[0] + tempText
    } else {
        text = text.replaceAll(/(?i)(?<!no\.\s|neka\s)\b\d+.*?(?:,|;|\u0024|\s)/, '');
    }
    ["^\\s*(.*?)\\s*(?=,|\\()",
     "^[^,(]+,\\s*(.*?)\\s*[(,;]",
     "^[\\w ]+\$"
    ].each
        {
            match = get_match(it, text)
            while (match.find()) {
                def name = null
                if (match.groupCount() > 0) {
                    name = match.group(1).trim()
                } else if (match.groupCount() < 1) {
                    name = match.group(0).trim()
                }
                //context.info("Name before replace: " + name);
                name = name.replaceAll(/^(?i)(?:no\.|ur|and|with|to\s+|[\)\(]|and)$/, '').replaceAll(/;\sAircraft\sConstruction.*$/, '')
                //def nameMatch = name =~ /(^[\w]+);/
                def nameMatch = name =~ /(^[\w+\s*]+);/
                if (nameMatch) {
                    name = nameMatch.group(1)
                }
                if (StringUtils.isNotBlank(name) && name.length() < 85) {
                    result.add(name)
                }
                break
            }
        }
    return result
}
//-----------------------------------------------------------------------------
def get_change_date_sections(data) {
    def dateSections = []
    def dates = []
    get_match(change_date_pattern, data).each
        {
            date ->
                dates.add(StringUtils.chop(date.toString()))
        }

    def beginning = true
    data.split(change_date_pattern).each
        {
            match ->
                if (beginning)
                    beginning = false
                else
                    dateSections.add(match.trim())
        }

    if (dateSections.size() < 1) {
        context.info("No change date sections found.")
    }

    def changeSections = []
    for (def i = 0; i < dateSections.size(); i++) {
        def sectionData = dateSections.get(i) + "##-##"
        sectionData = sectionData.replaceAll("[Ll]eaving the following", "The following")
            .replaceAll("[T|t]he following", "##-##The following")
            .replaceAll("THE FOLLOWING", "##-##THE FOLLOWING")
            .replaceAll("Delete all of ##-##The following", "##-##Delete all of the following")
            .replaceAll("Deletion of ##-##The following", "##-##Deletion of the following")
        changeSections.add([dates.get(i), sectionData])
    }
    return changeSections
}
//-----------------------------------------------------------------------------
def get_data_sections(data, link) {
    def result = []
    get_change_date_sections(data).each()
        { changeSection ->

            rawData = changeSection.get(1).replaceAll(/(?s)^.*?\d{4}:(?=\n)/, '').replaceAll(/(?i)-to-|"/, '')
            //context.info('raw:\n' + rawData)
            if (rawData.indexOf("[CUBA] - licensed pending removal by") >= 0)   //kludge to prevent overlooking ZEBETEX on 04/18/95
            {
                rawData = rawData.replaceAll("\\s+-\\s+licensed\\s+pending\\s+removal\\s+by\\s+FAC", "")
            }
            if (rawData.contains("As announced on 01/26/2001"))   //kludge to avoid unwanted paragraph
            {
                startLoc = rawData.indexOf("As announced on 01/26/2001")
                endLoc = rawData.indexOf("removed from OFAC's SDN list.")
                rawData = rawData.substring(0, startLoc) + rawData.substring(endLoc + "removed from OFAC's SDN list.".length())
            } else if (rawData.contains("The below-the-line listing of PETROMED LTD."))   //kludge to avoid unwanted paragraph from 06/26/96
            {
                startLoc = rawData.indexOf("The below-the-line listing of PETROMED LTD.")
                endLoc = rawData.indexOf("England [FRY S&M]")
                rawData = rawData.substring(0, startLoc) + rawData.substring(endLoc + "England [FRY S&M]".length())
            }
            rawData = rawData.replaceAll("As\\s+(?:a)?\\s*result\\s+of\\s+those\\s+updates[,]?", " ")
            // println "Change section date = " + changeSection.get(0) + ", VALUE = " + rawData;
            def dateSections = []
            def sectionList = null
            if (link.contains("sdnew94.txt")) {
                sectionList = context.regexMatches(new StringSource(rawData), [regex: section_pattern_94])
            } else if (link.contains("new19.txt")) {
                sectionList = context.regexMatches(new StringSource(rawData), [regex: section_pattern_2019])
            } else if (link.contains("new04")) {
                rawData = rawData.replaceAll(/(?is)following\.\s*(?=Their assets blocked)/, "following name have been removed ")
                sectionList = context.regexMatches(new StringSource(rawData), [regex: section_pattern_2004])
            } else {
                sectionList = context.regexMatches(new StringSource(rawData), [regex: section_pattern])
                if (sectionList != null)
                    dateSections.addAll(sectionList.getValue())
                sectionList = context.regexMatches(new StringSource(rawData), [regex: old_section_pattern])
            }

            if (sectionList != null)
                dateSections.addAll(sectionList.getValue())
            dateSections.each
                { match ->
                    if (match[1]) {
                        //context.info( "Match[1] : " + match[1]);
                        def s = match[1].split(/(?s)\s*\[(?:[^\]\]]+)\](?!\s*(?:;|\((?!Linked\sTo)|,|-|(?:a|f|n)\.?k\.?a|ltd|inc))(?:\s*\(Linked\sTo.*?\)|)/)
                        for (ss in s.getValue()) { //context.info( "section -->\n" + ss );
                            sss = ss.toString().replaceAll("from.*?list\\s+of[^:]+[:]?", " ").trim()
                            if (sss.startsWith("."))
                                sss = sss.substring(1).trim()
                            if (sss.length() > 0) {
                                //context.info("adding [" + changeSection.get(0) + "]" + sss)
                                result.add(new DataAttrSection(changeSection.get(0), sss))
                            }
                        }
                    }
                }
        }
    return result
}

def get_data_sections_new(data, link) {
    def result = []
    def rawData

    get_change_date_sections(data).each { changeSection ->

        rawData = changeSection.get(1).replaceAll(/(?s)^.*?\d{4}:(?=\n)/, '').replaceAll(/(?i)-to-|"/, '')
        if (rawData.indexOf("[CUBA] - licensed pending removal by") >= 0)   //kludge to prevent overlooking ZEBETEX on 04/18/95
        {
            rawData = rawData.replaceAll("\\s+-\\s+licensed\\s+pending\\s+removal\\s+by\\s+FAC", "")
        }
        if (rawData.contains("As announced on 01/26/2001"))   //kludge to avoid unwanted paragraph
        {
            startLoc = rawData.indexOf("As announced on 01/26/2001")
            endLoc = rawData.indexOf("removed from OFAC's SDN list.")
            rawData = rawData.substring(0, startLoc) + rawData.substring(endLoc + "removed from OFAC's SDN list.".length())
        } else if (rawData.contains("The below-the-line listing of PETROMED LTD."))   //kludge to avoid unwanted paragraph from 06/26/96
        {
            startLoc = rawData.indexOf("The below-the-line listing of PETROMED LTD.")
            endLoc = rawData.indexOf("England [FRY S&M]")
            rawData = rawData.substring(0, startLoc) + rawData.substring(endLoc + "England [FRY S&M]".length())
        }

        rawData = rawData.replaceAll("As\\s+(?:a)?\\s*result\\s+of\\s+those\\s+updates[,]?", " ")
        def dateSections = []
        def sectionList = null

        if (link.contains("sdnew94.txt")) {
            sectionList = context.regexMatches(new StringSource(rawData), [regex: section_pattern_94])
        } else if (link.contains("new19.txt")) {
            sectionList = context.regexMatches(new StringSource(rawData), [regex: section_pattern_2019])
        } else {
            sectionList = context.regexMatches(new StringSource(rawData), [regex: section_pattern])
            if (sectionList != null)
                dateSections.addAll(sectionList.getValue())
            sectionList = context.regexMatches(new StringSource(rawData), [regex: old_section_pattern])
        }

        if (sectionList != null) {
            dateSections.addAll(sectionList.getValue())
        }

        dateSections.each { match ->
            if (match[1]) {
                def s = match[1].split(/(?s)\s*\[(?:[^\]\]]+)\](?!\s*(?:;|\((?!Linked\sTo)|,|-|(?:a|f|n)\.?k\.?a|ltd|inc))(?:\s*\(Linked\sTo.*?(?<!\()\b[\w\.]+\)|)/)
                for (ss in s.getValue()) {
                    def sss = ss.toString().replaceAll("from.*?list\\s+of[^:]+[:]?", " ").trim()
                    if (sss.startsWith("."))
                        sss = sss.substring(1).trim()
                    if (sss.length() > 0) {
                        result.add(new DataAttrSection(changeSection.get(0), sss))
                    }
                }
            }
        }
    }

    return result
}
//-----------------------------------------------------------------------------
def format_date(data) {
    def match = get_match(/(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)/, data)

    def month = "-", day = "-", year

    while (match.find()) {
        month = month_map[match.group(0).toUpperCase()]
        //println "MONTH " + month;
    }

    match = get_match(/(\d{1,2})\s*[a-zA-Z]{3}/, data)
    //
    while (match.find()) {
        day = match.group(1)
        //println "DAY " + day;
    }

    match = get_match(/\s*(\d{2,4})/, data)

    while (match.find()) {
        year = match.group(1)
        if (year && new Integer(year).intValue() < 100) {
            year = "" + (new Integer(year).intValue() + 1900)
        }
        //println "YEAR " + year;
    }

    return month + "/" + day + "/" + year
}
//-----------------------------------------------------------------------------
private def stripCirca(String tmpDOB) {
    ["circa", "c."].each {
        tmpDOB = tmpDOB.toUpperCase().replace(it.toUpperCase(), "").trim()
    }
    return tmpDOB
}
//----------------------------------------------------------------------------
def get_match(regex, data) {
    return java.util.regex.Pattern.compile(regex,
        java.util.regex.Pattern.DOTALL |
            java.util.regex.Pattern.CASE_INSENSITIVE)
        .matcher(data)
}
//-----------------------------------------------------------------------------
def sanitize(data) {
    def str = ''

    if (data != null) {
        str = data.replace('\n', ' ').replaceAll('\r', ' ')
            .replaceAll('\u00A0', ' ')
            .replaceAll('\302', ' ')
            .replaceAll(/\*+/, '')
            .replaceAll(/(?is)\sfloating storage tanker italy.*/, '')
            .replaceAll(/(?is)\schemical\/oil tanker.*/, '')
            .replaceAll('\\s{2,}', ' ').trim()
    }
    return str
}
//----------------------------------------------------------------------------
def fetch_data(params) {
    return context.invoke(params).toString()
}
//----------------------------------------------------------------------------
class DataAttrSection {
    public String event_date
    public String data

    DataAttrSection(date, text) {
        event_date = date
        data = text
    }
}
//-----------------------------------------------------------------------------
def find_country(country) {
    def
        countries = ["Afghanistan", "Albania", "Algeria", "Andorra",
                     "Angola", "Antigua", "Argentina", "Armenia",
                     "Australia", "Austria", "Azerbaijan", "Bahamas",
                     "Bahrain", "Bangladesh", "Barbados", "Belarus",
                     "Belgium", "Belize", "Benin", "Bhutan", "Bolivia",
                     "Bosnia", "Botswana", "Brazil", "Brunei", "Bulgaria",
                     "Burkina", "Burma", "Burundi", "Cambodia", "Cameroon", "Canada",
                     "Cape Verde", "Central African Rep", "Chad", "Chile",
                     "China", "Colombia", "Comoros", "Congo",
                     "Costa Rica", "Croatia", "Cuba", "Cyprus", "Czech Republic",
                     "Denmark", "Djibouti", "Dominica", "Dominican Republic",
                     "East Timor", "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea",
                     "Eritrea", "Estonia", "Ethiopia", "Fiji", "Finland",
                     "France", "Gabon", "Gambia", "Georgia", "Germany",
                     "Ghana", "Greece", "Grenada", "Guatemala", "Guinea",
                     "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hungary",
                     "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland",
                     "Israel", "Italy", "Ivory Coast", "Jamaica", "Japan", "Jordan",
                     "Kazakhstan", "Kenya", "Kiribati", "Korea North", "Korea South",
                     "Kosovo", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon",
                     "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania",
                     "Luxembourg", "Macedonia", "Madagascar", "Malawi", "Malaysia",
                     "Maldives", "Mali", "Malta", "Marshall Islands", "Mauritania",
                     "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco",
                     "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar",
                     "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua",
                     "Niger", "Nigeria", "Norway", "Oman", "Pakistan", "Palau", "Panama",
                     "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Poland",
                     "Portugal", "Qatar", "Romania", "Russiaa", "Rwanda", "St Kitts & Nevis",
                     "St Lucia", "Saint Vincent & the Grenadines", "Samoa", "San Marino",
                     "Sao Tome & Principe", "Saudi Arabia", "Senegal", "Serbia",
                     "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia",
                     "Solomon Islands", "Somalia", "South Africa", "Spain", "Sri Lanka",
                     "Sudan", "Suriname", "Swaziland", "Sweden", "Switzerland",
                     "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand",
                     "Togo", "Tonga", "Trinidad & Tobago", "Tunisia", "Turkey",
                     "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates",
                     "United Kingdom", "United States", "Uruguay", "Uzbekistan", "Vanuatu",
                     "Vatican City", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
        ] as Set
    return countries.contains(country)
}

def getDupEntityNameAlias(def name, def entity_type, def aliasList) {
    //return: [duplicate entity name, [aliases to add], is name-alias swap active(boolean)]
    if (entity_type.equals("O")) {
        def tmpAlias = aliasList.clone()
        def orgListIndex = 0
        def orgListSingleArr = []
        def dupFlag = false
        def orgListSize = orgEntityAlias.size()
        def dupName = ""
        def swapFlag = false

        for (alias in aliasList) {
            if (alias == "BNC") {
                break
            }
            if (!dupFlag) {
                for (int i = 0; i < orgListSize; i++) {
                    orgListSingleArr = orgEntityAlias[i]
                    if (orgListSingleArr.contains(alias)) {
                        tmpAlias.remove(alias)
                        dupFlag = true
                        orgListIndex = i
                        break
                    }
                }
            } else {
                if (orgListSingleArr.contains(alias)) {
                    tmpAlias.remove(alias)
                }
            }
        }
        if (dupFlag) {
            dupName = orgListSingleArr[0]
            if (tmpAlias.contains(name)) {
                tmpAlias.remove(name)
            }
            orgListSingleArr = orgListSingleArr + tmpAlias

            if (!dupName.equals(name)) {
                if (orgListSingleArr[0].compareTo(name) < 1) {
                    orgListSingleArr.add(name)
                } else {
                    orgListSingleArr = [name] + orgListSingleArr
                    swapFlag = true
                }
            }
            orgEntityAlias[orgListIndex] = orgListSingleArr

            return [dupName, tmpAlias, swapFlag]
        } else {
            orgEntityAlias.add([name] + aliasList)
        }
    }
    return []
}


def create_entities_(data, entityUrl) {
    entityUrl = entityUrl.replace(/file:\/\/\/c:\//, 'https://www.treasury.gov/ofac/downloads/')
    def sections = get_data_sections(data, entityUrl)
    def names = [] as Set
    def i = 1

    if (sections.size() < 1) {
        context.warn("No data found.")
        return
    }

    for (section in sections) {
        //context.info("SECTION DATA [" + section.event_date + "] = " + section.data);

        def name = parse_name(section.data)
        def imoNo = parse_identification_value(section.data)

        if (section =~ /(?i)CUCU/) {
//            println name;
        }

        if (!name) {
            context.warn("Can't determine name from " + section.data)
            continue
        }

        def sanitizedName = sanitizeName(name.get(0))
        def type = getEntityType(section.data)

        if (type == "P" && name.size() > 1) {
            sanitizedName = sanitizeName(name.get(1)) + " " + sanitizedName
        }

        def entity = null
        if (type == "O") {
            entity = context.findEntity([name: sanitizedName, type: "O"])
            if (entity != null)
                println("Organization " + sanitizedName + " already included.")
        } else {
            if (names.contains(sanitizedName)) {
                println("Person " + sanitizedName + " already included.")
                continue
            } else {
                names.add(sanitizedName)
            }
        }
        def aliasList = []
        parse_alias(section.data).each {
            if (it) {
                it = sanitize_alias(it)
                aliasList.add(it)
                total_alias_list.add(it)
            }
        }

        boolean isNotFound = true

        for (int value = 0; value < total_alias_list.size(); value++) {
            if (total_alias_list[value].toString().toLowerCase().trim() == sanitizedName.toString().toLowerCase().trim()) {
                isNotFound = false
                break
            }
        }
        //5842

        boolean dupName = true

        for (int dup = 0; dup < duplicate_name_list.size(); dup++) {
            if (sanitizedName.toString().toLowerCase().contains(duplicate_name_list[dup].toString().toLowerCase())) {
                dupName = false
                break
            }
        }

        if (!dupName) {
            deleted_names.add(sanitizedName)
        } else if (!isNotFound) {
            retracted_alias_list.add(sanitizedName)
        } else if (isNotFound) {
            def dup = getDupEntityNameAlias(sanitizedName, type, aliasList)
            if (dup.size() > 0) {
                entity = context.findEntity(["name": dup[0], "type": type])
                aliasList = dup[1]
                if (dup[2]) {
                    entity.name = sanitizedName
                    entity.addAlias(dup[0])
                }
            } else {
                entity = context.findEntity(["name": sanitizedName, "type": type])
            }

            if (entity == null) {
                entity = context.getSession().newEntity()

                entity.setName(sanitizedName)
                entity.setType(type)

                def event = entity.newEvent()

                if (section.event_date) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm/dd/yy")
                    resultDate = simpleDateFormat.parse(section.event_date)
                    event.setDate(resultDate.format("mm/dd/yyyy"))
                }
                event.setDescription(event_desc)
                entity.addEvent(event)
                //context.info(entity.name);
                parse_dob(section.data).each
                    {
                        if (it && !it.toUpperCase().contains("UNKNOWN")) {
                            //context.info("IT: " + it);
                            dob = format_date(stripCirca(it))
                            sdob = new ScrapeDob()
                            splitDate = dob.split("/")
                            if (splitDate[0].equals("-") || splitDate[1].equals("-")) {
                                sdob.setCirca(true)
                            }
                            if (!splitDate[0].equals("-")) {
                                sdob.setMonth(splitDate[0])
                            }
                            if (!splitDate[1].equals("-")) {
                                sdob.setDay(splitDate[1])
                            }

                            sdob.setYear(splitDate[2])
                            //println( "DOB $dob" );
                            entity.addDateOfBirth(sdob)
                        }
                    }
                entity.addUrl(entityUrl)
            }

            aliasList.each {
                entity.addAlias(it)
            }

            //   parse_alias( section.data ).each{ if( it ){ entity.addAlias( it); } };
            parse_identification_new(entity, section.data)
            parse_address_new(entity, section.data)
        }
    }
}

def create_entities_new_(data, entityUrl) {
    entityUrl = entityUrl.replace(/file:\/\/\/c:\//, 'https://www.treasury.gov/ofac/downloads/')
    def sections = get_data_sections_new(data, entityUrl)
    def names = [] as Set
    def i = 1

    if (sections.size() < 1) {
        context.warn("No data found.")
        return
    }

    for (section in sections) {
        def name = parse_name(section.data)

        type = getEntityType(section.data)

        def aliasList = []
        parse_alias_new(section.data, type).each {
            if (it) {
                it = sanitize_alias(it)
                aliasList.add(it)
                total_alias_list.add(it)
            }
        }

        boolean isNotFound = true

        for (int value = 0; value < total_alias_list.size(); value++) {
            if (total_alias_list[value].toString().toLowerCase().trim() == name.toString().toLowerCase().trim()) {
                isNotFound = false
                break
            }
        }

        boolean dupName = true

        for (int dup = 0; dup < duplicate_name_list.size(); dup++) {
            if (name.toString().toLowerCase().contains(duplicate_name_list[dup].toString().toLowerCase())) {
                dupName = false
                break
            }
        }

        if (!dupName) {
            deleted_names.add(name)
        } else if (!isNotFound) {
            retracted_alias_list.add(name)
        } else if (isNotFound) {
            def imoNumber = parse_identification_value(section.data)
            def imoNo
            if (imoNumber =~ /(?i)imo/) {
                imoNo = imoNumber
            }
            if (name.size() > 0) {

                if (!name) {
                    //context.warn("Can't determine name from " + section.data);
                    continue
                }

                def sanitizedName = sanitizeName(name.get(0))

                def type = getEntityType(section.data)

                //context.info(type + " " + sanitizedName + "###" + section.data.toString());

                if (type == "O") {
                    if (sanitizedName =~ /(?i)\w+\s+\w\.\s*(?:$|\w{4,})/) {
                        type = "P"
                    }
                }

                if (type == "P") {
                    if (sanitizedName =~ /(?i)(?:\sM\.*B\.*H\.*|YARD NO|\sCO\.|GUBEREK GRIMBERG E HIJOS)/) {
                        type = "O"
                    }
                }

                if (type == "P" && name.size() > 1) {
                    sanitizedName = sanitizeName(name.get(1)) + " " + sanitizedName
                    //context.info(sanitizedName + "###2" + section.data.toString());
                }

                sanitizedName = sanitizedName.replaceAll(/(?i)^P\.*O\.*\s*BOX.*?\s/, '')
                sanitizedName = sanitizedName.replaceAll(/(?i),\s*P\.*O\.*\s*box.*/, '')

                //context.info(sanitizedName + "###3" + section.data.toString());
                def entity = null
                if (type == "O") {
                    if (name.size() > 1) {
                        context.info(sanitizedName + ", Name1: " + name.get(1))
                    }
                    if (name.size() > 1 && name.get(1) =~ /^\s*(?-i:[A-Z]\.[A-z]|LTD\.|CO\.)/) {
                        sanitizedName = sanitizedName + ", " + name.get(1)
                        sanitizedName = sanitizedName.replaceAll(/(?i)^P\.*O\.*\s*BOX.*?\s/, '')
                        sanitizedName = sanitizedName.replaceAll(/(?i),\s*P\.*O\.*\s*box.*/, '')
                        sanitizedName = sanitizedName.replaceAll(/(?i),\s*L\.*G\.*\s*Smith.*/, ', L.G.')
                        //context.info(sanitizedName + "###4" + section.data.toString());
                    }
                    def key = [sanitizedName, imoNo]
                    entity = context.findEntity(key)
                    if (entity != null) {
                        //println("Organization " + sanitizedName + " already included.")
                    }
                } else {
                    if (names.contains(sanitizedName)) {
                        //  println("Person " + sanitizedName + " already included.")
                        continue
                    } else {
                        names.add(sanitizedName)
                    }
                }

                //Merging duplicate entries
                def removedEntitiyList
                List<ScrapeEntity> en = []
                def dup = getDupEntityNameAlias(sanitizedName, type, aliasList)
                if (dup.size() > 0) {
                    entity = context.findEntity(["name": dup[0], "type": type])
                    aliasList = dup[1]
                    if (dup[2]) {
                        removedEntitiyList = getEntitiesByName(sanitizedName)

                        if (removedEntitiyList.size > 0) {
                            entity.name = sanitizedName
                            removedEntitiyList.each { def rEntity ->
                                mergeEntityWithRemovedEntity(removedEntitiyList, entity)
                                context.getSession().removeEntity(rEntity)
                            }

                        } else {
//                    println(sanitizedName)
                            if (entity != null) {
                                entity.name = sanitizedName
                            }
                        }
                        if (entity != null) {
                            entity.addAlias(dup[0])
                        }
                    }

                } else {
                    def key = [sanitizedName, imoNo]
                    entity = context.findEntity(key)
                    if (imoNo == null) {
                        en = context.getSession().getEntitiesByName(sanitizedName)
                        if (en.size() > 0) {
                            entity = en[0]
                        }
                    }
                }
                if (entity == null) {
                    entity = context.getSession().newEntity()
                    entity.setName(sanitizedName)
                    entity.setType(type)
                    aliasList.each {
                        entity.addAlias(it)
                        //context.info("Adding alias " + it + " to " + entity.name);
                    }
                    createEntityCommonCore(entity, section, entityUrl)
                } else {

                    //context.info("Alias list just before add " + aliasList.toString());
                    aliasList.each {
                        entity.addAlias(it)
                        //context.info("Adding alias " + it + " to " + entity.name);
                    }

                    //context.info("Removing alias " + sanitizedName + " from " + entity.name);
                    //entity.getAliases().remove(sanitizedName)
                    //context.info("Aliases are " + entity.getAliases().toString());
                    createEntityCommonCoreAlreadyExist(entity, section, entityUrl)
                }
            }
        }
    }
}

def sanitize_alias(def alias) {
    alias = alias.toString().replaceAll(/,/, '').trim()
    return alias
}
