package current_scripts

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import scrapian_scripts.utils.AddressMappingUtil

import java.text.SimpleDateFormat

context.setup([socketTimeout: 50000, connectionTimeout: 10000, retryCount: 5, userAgent: 'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) Gecko/20100101 Firefox/35.0'])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true
agencyMap = [:]
blockText = []
wltText = []
denText = []
addressMapper = new AddressMappingUtil()


sdf = new SimpleDateFormat('MM/dd/yyyy')
today = new Date()
_1900C = Calendar.getInstance()
_1900C.set(1900, 0, 1, 0, 0, 0)
_1900 = _1900C.getTime()


fillAgencyMap()
initPdfParsing()



def fillAgencyMap() {
    agencyMap['ISN'] = 'Nonproliferation Sanctions'
    agencyMap['DPL'] = 'Denied Persons List'
    agencyMap['UVL'] = 'Unverified List'
    agencyMap['EL'] = 'Entity List'
    agencyMap['DTC'] = 'ITAR Debarred'
    wltText.add('ISN')
    denText.add('DPL')
    denText.add('UVL')
    denText.add('EL')
    denText.add('DTC')
    blockText.add("FSE")
    blockText.add("ISA")
    blockText.add("PLC")
    blockText.add("561")
    blockText.add("SSI")
    blockText.add("SDN")

}

class Us_ecr {
    static root = "http://export.gov/ecr/eg_main_023148.asp"
    static url = root + "/ENF/"

    static rootUrl = " https://www.state.gov/key-topics-bureau-of-international-security-and-nonproliferation/nonproliferation-sanctions/"//"https://www.state.gov/key-topics-bureau-of-international-security-and-nonproliferation/nonproliferation-sanctions/"

    //static pdfUrl = "https://www.state.gov/wp-content/uploads/2019/06/MASTER-Sanctions-chart-June-2019.pdf"

    static aliasTag = "~ALIAS~"
}

enum FIELDS_CSL
{
    NAME, TYPE, CITY, STATE, COUNTRY, PO, ADDR1, PCOUNTRY, PSTATE, PCITY, PADDR1, TITLE, DOB, POB, NATIONALITY, CITIZENSHIP, START_DATE, END_DATE, EVNT_DETAILS, EVNT_CAT, URL, WEIGHT, REMARK_SP, IDENTIFICATION, POSITION, REMARK_REGNO, REMARK_VT, REMARK_VF, REMARK_VO, REMARKS, ASSOCIATION
}

//------------------------------pdf part----------------------//
def initPdfParsing() {
    def html
    def pdfUrlMatch
    def pdfFile
    def pdfText
    def attrMap
    def dupAttrMap
    def aliasArr
    def name
    def pdfUrl
    def sanctionType = /(?:Executive|INKSNA|Missile|Nuclear|Export|Iran|INPA|CBW|Transfer|Chemical)/


    html = context.invoke([url: Us_ecr.rootUrl, tidy: true, cache: false])

    pdfUrlMatch = html =~/href="(.*?chart-\d+\.\d+\.\d+[-_\.\d]+?\.pdf)"/
    if (pdfUrlMatch.find()){

        pdfUrl = pdfUrlMatch.group(1)
    }

    //pdfUrlMatch = html =~ /(?i)<a href="([^"]+)"[^>]*>Complete list of sanctioned entities/
//    if (pdfUrlMatch) {
    //    pdfFile = context.invokeBinary([url: Us_ecr.pdfUrl])

    pdfFile = context.invokeBinary([url: pdfUrl, cache:false])
    pdfText = context.transformPdfToText(pdfFile, null)

    pdfText = sanitizePdfText(pdfText)

    pdfText.findAll(/(?ims)^$sanctionType.*?(?=^$sanctionType|\Z)/).each { data ->

        def row = rowFixing(data)

        //  if (row.find(/\/\d+\s{2,}Active|(?:Expired|Expired on|Lifted on|Lifted|Terminated on|Waived on) \d{1,2}\\/\d{1,2}\\/\d{1,4}/)) {
        if (row.find(/\d+\s{2,}(?:Active|placeholder|procurement ban)|(?:Expired on|Expired|Lifted on|Lifted|Terminated on|Waived on) \d{1,2}\/\d{1,2}\/\d{1,4}/)) {

            aliasArr = []
            dupAttrMap = [:]
            name = ""

            attrMap = getPdfData(row)

            name = attrMap[FIELDS_CSL.NAME]
            alias = attrMap[FIELDS_CSL.NAME].find(/.(?:aka|also known as).*/)
            if (alias) {
                alias = sanitize(alias
                    .replaceAll(/.(?:aka|also known as).|[^\w]$/, "")
                    .replaceAll(/~/, ",")
                    .replaceAll(/"/, ""))
                if (alias) {
                    aliasArr = new HashSet<String>(Arrays.asList(alias.split(";")))
                }
                name = attrMap[FIELDS_CSL.NAME].replaceAll(/\s*.(?:aka|also known as).*/, "")
            }

            attrMap[FIELDS_CSL.URL] = ["http://bit.ly/1iwxkfX"]

            if (name.toString().contains("[DPRK Munitions Industry Department (MID) Official]")){
                name = "Rim Ryong Nam"
                attrMap[FIELDS_CSL.REMARKS] = "DPRK Munitions Industry Department (MID) Official"

            }
            createEntity(name, attrMap, aliasArr)

        }
    }
}

def rowFixing(def row){

    row = row.toString().replaceAll(/(INKSNA.*?(?:ssa Trading|Light|Sabbagh))(.*)\s+(.*?any)\s+(.+)/,'$1 $3 $2 $4')
    row = row.replaceAll(/(INKSNA.*?Energy)(.*)\s+(.+?)and\s+(.*?2021)\s+(.+)/,'$1 $3 $2 $4 $5')
    row = row.replaceAll(/(INKSNA.*?gjun)(.+)\s+(.+)\s+(.+?any)\s+(.+)/,'$1 $3 $4 $2 $5')
    row = row.replaceAll(/(INKSNA.*?DPRK)(.+)\s+(.+?try)\s+(.+)\s+(.+?\))\s+Federal Register\s+(.+)\s+(.+)/,'$1 $3 $5 $6 $7 $2 $4')
    row = row.replaceAll(/(INKSNA.*?228,)(\n.*?)(November 25, 2020)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA.*?228)(\n.*?)(November 25, 2020)/,'$1 $3 $2')
    row = row.replaceAll(/(?<=Vol. 85, No. 228,)\s+(?=Nov)/," ")
    row = row.replaceAll(/(INKSNA.*?193,)(\n.+)(October 5, 2020)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA   Raybeam.*?193)(\n.+?)(Oct.+)/,'$1 $3 $2')
    row = row.replaceAll(/(?<=Vol. 85, No. 193,)\s+(?=October 5, 2020)/," ")
    row = row.replaceAll(/(INKSNA.*?31,)(\n.+)(February.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Executive.+?April)(\n.+?)(10, 2019, Federal)/,'$1 $3 $2')
    row = row.replaceAll(/(Executive.*?Astronautics.*?231,)(\n.*?)(Decem.+)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA.*?Shanghai.*?March)(\n.*?)(30, 2017, Federal)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA\s+Korea Namhung.*?128,)(\n.+?)(\d.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Executive.*?Handasieh.*?142,)(\n.+?)(\d+\\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Executive.*?Syrian Arab.*?142,)(\n.+?)(\d+\\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Executive.*?Noor Afza.*?49,)(\n.+?)(\d+\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA.*?Islamic Republic.*?103)(\n.+?)(\d+\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA.*?Islamic Revolutionary.*?103)(\n.+?)(\d+\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA.*?Scientific Studies.*?103)(\n.+?)(\d+\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Executive.*?Second Academy of.*?173)(\n.+?)(\d+\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(INKSNA.*?R and M International.*?206)(\n.+?)(\d+\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Iran.+?Challen.+77,)(\n.+?)(\d+.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Executive.+?Defense Industries.+63,)(\n.+?)(\d+\\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(INPA.+?Elmstone Services.+67,)(\n.+?)(\d+\\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Chemical and.*?Berge Aris.+35,)(\n.+?)(\d+\/.+)/,'$1 $3 $2')
    row = row.replaceAll(/(Missile.*?\\/00   )(placeholder)(.+)(\n.+?)(of.+?02)(.+)/,'$1 $2 $5 $3 $4    $6')
    row = row.replaceAll(/(.+?)(procurement ban ended)(.+)(\n.+?)(2002.+?ions)(.+)(\n.+?)(removed.+)\s+(Building)/,'$1 $2 $5 $8 $3 $6 $4 $7 \n                     $9')
    row = row.replaceAll(/(?<=Vol. 64, No. 82,)    (?=04\/29\/99, \[PDF\])/," ")
    row = row.replaceAll(/(.+?)(procurement ban ended)(.+)(\n.+?)(2002.+?ions)(.+)(\n.+?)(removed.+)/,'$1 $2 $5 $8 $3 $6 $4 $7')
    row = row.replaceAll(/(.+)(procurement ban)(.+Vol.+)(\n.+?)(ended.+?other)(.+)(\n.+?)(sanctions remain)/,'$1 $2 $5 $8 $3 $6 $4 $7')
    row = row.replaceAll(/(?<=Vol. 64, No. 240,)\s+(?=12\/15\/99, \[PDF\])/," ")

    return row

}

def sanitizePdfText(def pdfText){

    pdfText = pdfText.toString()
    pdfText = pdfText.replaceAll(/Updated.*?\s+Sanction\s+Entity\s+Location of\s+Date imposed\s+Status\/Date of\s+Federal Register\n.*notice/,"")
    pdfText = pdfText.replaceAll(/\u000C/,"")
    pdfText = pdfText.replaceAll(/(?i),\s*also|operating in the|Middle East and|eurasia|to be in the Middle/,"")
    pdfText = pdfText.replaceAll(/Originally based in|Previously residing|All provisions expired as/, "placeholder   ")
    pdfText = pdfText.replaceAll(/(?i)Weapons Control/,"Weapons Control  ")
    pdfText = pdfText.replaceAll(/\ufffd/, "-")
    pdfText = pdfText.replaceAll(/(?i)Vol\./, "    Vol.")
    pdfText = pdfText.replaceAll(/(?s)(?<=Private|Inc\.|Tech),/,"")
    pdfText = pdfText.replaceAll(/(?<=International Trade Co.)\s+Vol. 86, No. 13/,"")
    pdfText = pdfText.replaceAll(/INKSNA   Ningbo Zhongjun            China    1\/13\/2021    Active/,"INKSNA   Ningbo Zhongjun            China    1/13/2021    Active                    Vol. 86, No. 13")
    pdfText = pdfText.replaceAll(/sub-unit, or subsidiary\s+Federal Register\s+thereof/,"")
    pdfText = pdfText.replaceAll(/and any\s+Federal Register\s+successor, sub-unit, or\s+subsidiary thereof/,"")
    pdfText = pdfText.replaceAll(/(?:successor, )?(?:unit, or)? subsidiary thereof\s+Federal Register/,"")
    pdfText = pdfText.replaceAll(/successor, sub-unit, or\s+Federal Register\s+subsidiary thereof/,"")
    pdfText = pdfText.replaceAll(/\s+(?:and any successor, sub-|successor, sub-unit, or|any successor, sub-unit, or|any successor, subunit, or|subsidiary thereof;|unit, or subsidiary thereof|sub-unit, or subsidiary|thereof|subunit, or subsidiary|and any successor,)/,"")
    pdfText = pdfText.replaceAll(/subunit,\s+or subsidiary/,"")
    pdfText = pdfText.replaceAll(/(?<=Vol. 86, No. 151,)\s+(?=August)/," ")

    return pdfText
}

def getPdfData(data) {

    def attrMap = [:]
    def dataMatch
    def countries = /North Korean?|Iraq|Bulgaria|Taiwan|Cuba|Singapore|Mexico|Germany|UK|South Korean?|Switzerland|Venezuela|Sudan|Uganda|Malaysia|Egypt|Belarus|Sudan|Eritrea|Turkey|Burma|United Arab|Saudi Arabia|Syria|Jordan|India|Macedonia|Moldova|Kazakhstan|Pakistan|China|Russia|United Arab|Iran|Sri Lanka|Czech Republic|South Africa|Hong Kong|Middle East/

    // first line
    data.find(/(?m)^.*$/) { firstLine ->

        dataMatch = firstLine
            .replaceFirst(/\s(?=($countries|\w+)\s{2,}\d\d?\/\d\d?\/\d\d\d?\d?)/, "   ")
            .replaceAll(/\s{3,}|(?<=^Missile Sanctions|^Executive Order|^Transfer of Lethal|^Weapons Control|^CBW Act|^INPA|^Iran and syria|^Iran-Iraq Arms|^Export|^Nuclear|^INKSNA)\s/, "|")
            .split(/\|/)

        attrMap[FIELDS_CSL.REMARK_SP] = dataMatch[0]
        attrMap[FIELDS_CSL.NAME] = dataMatch[1]

        attrMap[FIELDS_CSL.COUNTRY] = dataMatch[2]
        attrMap[FIELDS_CSL.START_DATE] = dataMatch[3]
        attrMap[FIELDS_CSL.EVNT_CAT] = dataMatch[4]
        attrMap[FIELDS_CSL.REMARK_REGNO] = dataMatch[5]
        data = data.replace(firstLine, "")
    }
    data.findAll(/(?m)^.*$/).each { line ->
        dataMatch = line.replaceAll(/\s{3,}/, "|").split(/\|/)
        if (line.find(/^\S/)) {
            attrMap[FIELDS_CSL.REMARK_SP] += " " + dataMatch[0]
        }
        if (line.find(/^(\S\s?)*\s{2,30}\S/)) {
            attrMap[FIELDS_CSL.NAME] += " " + dataMatch[1]
        }
        if (line.find(/\s{50}/)) {
            attrMap[FIELDS_CSL.REMARK_REGNO] += " " + dataMatch.last()
        }
    }

    attrMap = cleanupPdfData(attrMap)


    return attrMap
}

def cleanupPdfData(attrMap) {

    if (attrMap[FIELDS_CSL.NAME]) {
        attrMap[FIELDS_CSL.NAME] = attrMap[FIELDS_CSL.NAME].trim().replaceAll(/-\s|\s-/, "-")
        attrMap[FIELDS_CSL.NAME] = attrMap[FIELDS_CSL.NAME].toString().replaceAll(/(?: and any| and$)/,"").replaceAll(/(?s)\s+/, " ").trim()
        attrMap[FIELDS_CSL.NAME] = attrMap[FIELDS_CSL.NAME].toString().replaceAll(/(?<=\(ARZ\)) \(Kaliningrad\)/,"")
        attrMap[FIELDS_CSL.NAME] = attrMap[FIELDS_CSL.NAME].toString().replaceAll(/,$/,"")
    }
    if (attrMap[FIELDS_CSL.COUNTRY] && !attrMap[FIELDS_CSL.COUNTRY].find(/placeholder|Middle East/)) {
        attrMap[FIELDS_CSL.COUNTRY] = [attrMap[FIELDS_CSL.COUNTRY]
                                           .replaceAll(/^UK$/, "UNITED KINGDOM")
                                           .replaceAll(/^United Arab$/, "UNITED ARAB EMIRATES")
                                           .replaceAll(/^North Korean$/, "CHINA")]
    } else {
        attrMap[FIELDS_CSL.COUNTRY] = null
    }

    if (attrMap[FIELDS_CSL.START_DATE]) {

        attrMap[FIELDS_CSL.START_DATE].find(/(?<=\d?\d\/\d\d\/)\d\d(?!\d\d)/) { year ->

            if (year.toInteger() > 90) {

                century = "19"
            } else {
                century = "20"
            }
            attrMap[FIELDS_CSL.START_DATE] = attrMap[FIELDS_CSL.START_DATE].replaceAll(/(?<=\d?\d\/\d\d\/)\d\d/, century + year)
        }
        attrMap[FIELDS_CSL.START_DATE] = sanitize(attrMap[FIELDS_CSL.START_DATE])
//        attrMap[FIELDS_CSL.EVNT_CAT] = 'WLT'
    }


    if (attrMap[FIELDS_CSL.EVNT_CAT].toString().contains("procurement ban ended")){

        attrMap[FIELDS_CSL.REMARKS] = attrMap[FIELDS_CSL.EVNT_CAT]
    }


    if (!attrMap[FIELDS_CSL.EVNT_CAT].equals("Active")) {

        attrMap[FIELDS_CSL.END_DATE] = sanitizeDate(attrMap[FIELDS_CSL.EVNT_CAT])

        attrMap[FIELDS_CSL.END_DATE].find(/(?<=\\/)\d{2}\u0024/) { year ->
            if (year.toInteger() > 90) {
                century = "19"
            } else {
                century = "20"
            }
            attrMap[FIELDS_CSL.END_DATE] = attrMap[FIELDS_CSL.END_DATE].replaceAll(/(?<=\\/)\d{2}\u0024/, century + year)
        }
        attrMap[FIELDS_CSL.END_DATE] = sanitize(attrMap[FIELDS_CSL.END_DATE])
    }

    if (attrMap[FIELDS_CSL.REMARK_SP]) {
        attrMap[FIELDS_CSL.REMARK_SP] = attrMap[FIELDS_CSL.REMARK_SP].replaceAll(/Executive Order/, "E.O.").replaceAll(/CBW/, "Chemical and Biological Weapons")
        attrMap[FIELDS_CSL.REMARK_SP] = attrMap[FIELDS_CSL.REMARK_SP].toString().replaceAll(/(?s)\s+/, " ").trim()
    }
    if (attrMap[FIELDS_CSL.REMARK_REGNO]) {

        attrMap[FIELDS_CSL.REMARK_REGNO] =
            sanitize(attrMap[FIELDS_CSL.REMARK_REGNO]
                .toString().trim()
                .replaceAll(/~/, ",")
                .replaceAll(/(?i)(?<!,)\s(?=Vol\.)|(?<!,)\s(?=\d\d\/\d\d\/\d\d)/, ", ")
                .replaceAll(/(?i),?\s*(Federal Register;?|Press Release|\[?(HTML|PDF).*\]?)/, "")
                .replaceAll(/( Manu and| and$| Research$| Ltd.$| Association$|,\s*Industrial Trading$|Register, Fact sheet, Statement|,? Federal)/,"")
                .replaceAll(/(?<=2019|2020|231|35|63|128|\/07|10, 2019|229),$/,"")
                .replaceAll(/Vol. 85, No. 31, February 14, 2020, Industrial Trading/,"Vol. 85, No. 31, February 14, 2020")
                .replaceAll(/,?\s*Register$| Nonproliferation/,"")
                .replaceAll(/(?<=Vol. 65, No. 231,) ,/,"")
                .replaceAll(/(?s)\s+/, " ").trim())
    }

    return attrMap
}

def checkDuplicatePdfEntity(name) {
    def entity = null
    def type
    name = nameFilter(name)
    name = nameFilter(sanitize(name))
    if (name) {
        name = name.replaceAll(/\(.*?\)/, "").trim()
        type = detectEntityType(name)
        entity = context.findEntity(["name": name, "type": type])
        if (!entity) {
            name = name
                .replaceAll(/(?i)(?<=\s)Co\.?(?=\s|$)/, "Company")
                .replaceAll(/(?i)(?<=\s)Corp\.?(?=\s|$)/, "Corporation")
                .replaceAll(/(?i)(?<=\s)Ltd\.?(?=\s|$)/, "Limited")
            entity = context.findEntity(["name": name, "type": "O"])
        }
    }
    if (entity) {
        return name
    } else {
        return null
    }
}

//------------------------------Entity creation part---------------//
def createEntity(def eName, def attrMap, def aliasList = []) {

    if (eName) {

        eName = nameFilter(eName)

        nameArr = eName.split(";").collect() { name ->

            name = nameFilter(sanitize(name))
            name = sanitizeEntityName(name)

            if (name) {

                //name.find(/\(.*?\)/) { abbr ->
                name.find(/(?i)\([a-z& ]+\)$/) { abbr ->

                    aliasList.add(abbr.replaceAll(/\(|\)/, ""))
                    //name = name.replaceAll(/\(.*?\)/, "").trim()
                    name = name.replaceAll(/(?i)\([a-z& ]+\)$/, "").trim()
                }

                def entityType = attrMap[FIELDS_CSL.TYPE] ? (attrMap[FIELDS_CSL.TYPE] == 'Individual' ? 'P' : (attrMap[FIELDS_CSL.TYPE] == 'Entity' ? 'O' : detectEntityType(name))) : detectEntityType(name)
                def finalAliasList = []

                aliasList.each { alias ->
                    alias = aliasFixer(sanitize(alias))

                    if (alias) {

                        finalAliasList.add(alias)
                    }
                }
                createEntityCommonCore(name, finalAliasList, attrMap)
            }
        }
    }
}

def aliasFixer(def alias) {
    alias = alias.replaceAll(/(?i)a\.?k\.?a\.?/, "").replaceAll(/^\(/, "")
        .replaceAll(/^\d+$/, "")

    return alias
}

def aliasFixingWithTag(def nameStr) {
    def aliasTokens = /\b(?:(?>d[-\/\.]?b[-\/\.]?[ao]\b|[af][-\/\.]?k[-\/\.]?a\b)[-\/\.]?|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\s*\/\s*a)|(?>previously|also|former?ly)(?: known as)?\b)\s*/
    nameStr = nameStr
    //general alias inside braces  B (C AKA D) E ???
        .replaceAll(/(?is)(.*?)\s*\(([^\(\)]*?)$aliasTokens([^\(\)]*)\)\s*(.*)/, { a, b, c, d, e ->
        return b + e + Us_ecr.aliasTag + c + d //???
    })
    //org aliassss inside braces
        .replaceAll(/(?i)\b(?<=llc|inc)\.?\s*(?!\.)\(([^\)]+)\)/, { a, b ->
        return Us_ecr.aliasTag + " " + b
    })
    //aliases inside bracket: NAME [AKA ALIAS]
        .replaceAll(/(?is)(.*?)\s*[\(\[]+$aliasTokens(.*?)[\)\]]+$/, { a, b, c ->
        return b + Us_ecr.aliasTag + c
    })

    return nameStr
}

def nameFilter(def name) {
    name = name.replaceAll(/(?i)^the following.*?:/, "")
        .replaceAll(/(?i)and nuclear.*?ammonia plants/, "")
        .replaceAll(/(?i)^and\s/, "")

    return name
}

def createOrgEntity(def name, def aliasList = []) {
    def entity = context.findEntity(["name": name, "type": "O"])

    if (!entity) {
        entity = context.getSession().newEntity()
        entity.setName(sanitize(name))
        entity.type = "O"
    }

    aliasList.each { alias ->
        entity.addAlias(alias)
    }

    return entity
}

def createPersonEntity(def name, def aliasList = []) {
    def entity = context.findEntity([name: name, "type": "P"])
    if (!entity) {
        entity = context.getSession().newEntity()
        entity.name = camelCaseConverter(personNameReformat(sanitize(name)))
        entity.type = "P"
    }
    aliasList.each { alias ->
        entity.addAlias(camelCaseConverter(personNameReformat(sanitize(alias))))
    }

    return entity
}

def personNameReformat(name) {
    def exToken = "(?:[js]r|I{2,3})"
    return name.replaceAll(/(?i)\s*(.*?)\s*,\s*\b((?:(?!$exToken\b)[^,])+)s*(?:,\s*\b($exToken)\b)?\s*$/, '$2 $1 $3').trim()
}

def camelCaseConverter(def name) {
    //only for person type //\w{2,}: II,III,IV etc ignored
    name = name.replaceAll(/\'/, 'apostrophyholder')

    name = name.replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, { a, b, c, d ->
        return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
    })

    name = name.replaceAll(/apostrophyholder/, '\'')
}

def createEntityCommonCore(name, aliasList, attrMap) {

    def entity, type
    String[] format = [
        "MM/dd/yyyy",
        "MM-dd-yyyy",
        "yyyy-MM-dd",
        "yyyy"
    ]
    type = detectEntityType(name)

    entity = context.findEntity(["name": name, "type": type])

    if (type.equals("P")){
        name = personNameReformat(sanitize(name))
    }

    if (name.toString().contains("Sun Creative Zhejiang")){
        name = "Sun Creative (Zhejiang) Technologies, Inc."
    }

    if (!entity) {
        entity = context.getSession().newEntity()

        if (name.toString().contains(" LLC")){
            entity.name = sanitize(name)

        }else{
            entity.name = camelCaseConverter(sanitize(name))
        }

        entity.type = type

        aliasList.each { alias ->

            entity.addAlias(camelCaseConverter(sanitize(alias)))
        }
        def event = new ScrapeEvent()
        //fix all values in attrMap

        if (attrMap[FIELDS_CSL.START_DATE]) {
            def sDate = context.parseDate(new StringSource(attrMap[FIELDS_CSL.START_DATE]), format)
            if (sDate && sdf.parse(sDate).compareTo(today) <= 0 && sdf.parse(sDate).compareTo(_1900) > 0) {
                event.setDate(sDate)
            }
        }

        if (attrMap[FIELDS_CSL.END_DATE]) {
            def eDate = context.parseDate(new StringSource(attrMap[FIELDS_CSL.END_DATE]), format)
            event.setEndDate(eDate)
        }
        String desc="This entity appears on the Department of State Nonproliferation Sanctions List. " + "Sanction - " + attrMap[FIELDS_CSL.REMARK_SP]+"." + " Federal Register Notice: " + attrMap[FIELDS_CSL.REMARK_REGNO]
        desc=desc.replaceAll(/\W+$/,"").replaceAll(/(?s)\s+/," ").trim()
        event.setDescription(desc)

        if (attrMap[FIELDS_CSL.EVNT_CAT]) {
//        event.setCategory(attrMap[FIELDS_CSL.EVNT_CAT])
            if (attrMap[FIELDS_CSL.EVNT_CAT] == 'Active') {
                event.setCategory("WLT")
                event.setSubcategory('SAN')
            } else {
                event.setCategory("FOS")
                event.setSubcategory('ASC')
            }
        }
        if (entity.events.size() == 0 || attrMap[FIELDS_CSL.EVNT_CAT]) {
            entity.addEvent(event)
        }

        def address = new ScrapeAddress()
        if (attrMap[FIELDS_CSL.COUNTRY]) {
            attrMap[FIELDS_CSL.COUNTRY].each { country ->
                address.country = addressMapper.mapCountry(country)
            }
        }
        if (attrMap[FIELDS_CSL.CITY]) {
            attrMap[FIELDS_CSL.CITY].each { city ->
                address.city = city
            }
        }
        if (attrMap[FIELDS_CSL.STATE]) {
            attrMap[FIELDS_CSL.STATE].each { state ->
                address.province = state
            }
        }
        if (attrMap[FIELDS_CSL.PO]) {
            attrMap[FIELDS_CSL.PO].each { po ->
                address.postalCode = po
            }
        }
        if (attrMap[FIELDS_CSL.ADDR1]) {
            attrMap[FIELDS_CSL.ADDR1].each { addr1 ->
                address.address1 = addr1
            }
        }
        if (attrMap[FIELDS_CSL.COUNTRY] || attrMap[FIELDS_CSL.CITY] || attrMap[FIELDS_CSL.STATE] || attrMap[FIELDS_CSL.PO] || attrMap[FIELDS_CSL.ADDR1]) {
            entity.addAddress(address)
        }

    } else {

        def event = new ScrapeEvent()
        //fix all values in attrMap

        if (attrMap[FIELDS_CSL.START_DATE]) {
            def sDate = context.parseDate(new StringSource(attrMap[FIELDS_CSL.START_DATE]), format)
            if (sDate && sdf.parse(sDate).compareTo(today) <= 0 && sdf.parse(sDate).compareTo(_1900) > 0) {
                event.setDate(sDate)
            }
        }
        if (attrMap[FIELDS_CSL.END_DATE]) {
            def eDate = context.parseDate(new StringSource(attrMap[FIELDS_CSL.END_DATE]), format)
            event.setEndDate(eDate)
        }
        String desc="This entity appears on the Department of State Nonproliferation Sanctions List. " + "Sanction - " + attrMap[FIELDS_CSL.REMARK_SP]+"." + " Federal Register Notice: " + attrMap[FIELDS_CSL.REMARK_REGNO]
        desc= desc.replaceAll(/^\W+$/,"")
        event.setDescription()
        if (attrMap[FIELDS_CSL.EVNT_CAT]) {
//        event.setCategory(attrMap[FIELDS_CSL.EVNT_CAT])
            if (attrMap[FIELDS_CSL.EVNT_CAT] == 'Active') {
                event.setCategory("WLT")
                event.setSubcategory('SAN')
            } else {
                event.setCategory("FOS")
                event.setSubcategory('ASC')
            }
        }
        if (entity.events.size() == 0 || attrMap[FIELDS_CSL.EVNT_CAT]) {
            entity.addEvent(event)
        }
    }

    if (attrMap[FIELDS_CSL.REMARKS])
        entity.addRemark(attrMap[FIELDS_CSL.REMARKS])
}

def detectEntityType(name) {


    def entityType = context.determineEntityType(name)
    if (entityType.equals("P")) {
        if (name =~ /(?i)\b(?:Intelcom|Proexcom|Arsenal|Avialinii|ADVANCED|AL(?:-)?HARAM(?:A|E)(?:I|Y)N|ASSA|ACCESORIOS|AL-AQSA|ATLANTIC|Aerospace|Airline|Aqsa|BANK|Beijing|Belvneshpromservice|COFFEE|COMMITTEE|Complex|Computer|Create|DREAM|DUBAI|Desire)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Artisticas|Residencial|Commission|Commercial|EGASSA|Establishment|Elegance|ENERGY|Enterprise|Equipment|FAMILY|FARM|FOUNDATION|GSKB|HUMAN|IG(HA)?THA|IGASA|IGASE|IGASSA|IGATHA|IIRO|IRAN|ISLAMIC|Industrial|Industry|Infotec|Institut|Institute|Instruments?)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Spain|Singapore|Bellamax|Beltechexport|Plant|Companies|Airfreight|Defense|Office|Freedom|Exchange|Production|FortuneFondation|Fox|Nuclear|LIGHT|Operation|LOYAL|MAGHAZEHE|MEC|Material|NICO|ORGANIZATION|Intelligence|Project|Pharmaceuticals|PCI|PETROLEUM|Plan|Property|SQUAD|SOCIETY|SBIG|SHIG|STORE|Systema?|SA)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Investment|Indonesia|Ryongaksan|Sino-Ki|Sukhoy|Telstar|Aeronautics|Xinshidai|Grafit|Integral|Continuity|Profesional|Elecomponents|Sourcing|Electric|Interior|Tahrike|Supply|Stichting|TRANSNEFT|Tech|Technical|UCB|UNION|UVZ|VOLUNTEER|Video|or|Woodford)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Humanitare|Fond|Treasure|Restaurante|Glavkosmos|Handasieh|Hizballah|Huazhong Cnc|Hezbollah|Megatrade|Mikrosam|Travel|Dynamics|Finance|Tools|Party|Electronica|AL-?ISLAMIYAH?|B\.V\.?|Ag|Government|CHEMISTRY|LTDA\.?|labs?|Construction|LAJNAT|LAHORE|LA|LASHKAR|GROUPE|LIBERATION)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Global|Industria|Medical|Army|Money|Race|Movement|Telecommunications|University|Mobile|Security|League|GRANDE|CENTE?RE?|JSC|council|FUND|ANSARUL|HAYAT|saba|TURATH|Corps|trust|Volunteers)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Aluminat|Argoplast|Asaib Ahl Haq|Budaya Kita Sdn Bhd|Elektronik|Elektroteknik|Technic|Faratech|Military|Kay Marine|Khataib Hezbollah|Design Bureau|Lebanese Hizballah|Negin Parto|Century|Npo Mashinostroyeniya|Pars Amayesh Sanaat|Rock Chemie|Import-Export|Academy|Spc Supachoke|Strategic Force|Armed Forces|Tarh O Palayesh|Metallist|Yasa Part)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Design|Ikco|Display|Kuusiaaren Sarnetex|Nasosy Ampika|Ulrichshofer Vertriebs|Results|Noun Nasreddine|Npc|Abris|Abris-Key|Abris-Technology|Digital|Aerotechnic|Aktsionernoe Obschestvo|Used Car|Photonics|Al-Faris)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Alpha Lam|Rare Earths|Angstrem-M|Importers And Exporters|Ao\s(?:Druzhba|Gazmash|Gazprom Promgaz)|Apex\s(?:Kazakhstan|St\. Petersburg|Yekaterinburg)|Aquanika|Kompozit Kimya|Aviton|Vehicle|Gmbh|Bitreit|Chernomorneftegaz|Cjsc)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Corporacion|Ports|Railway|Cybernet|Desarrollos Empresariales|Djsc|Dm Link|Peoples Republic|Biochem|Eez Sdn|Elmont Intl|Glavgosekspertiza|Shipyard|Fimco Fze|Uprdor Taman|Gazprom\s(?:Mezhregiongaz|Neft)|Hakim Nur|In The Sea|Industrio|Fzco|Fze)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Concern|Priborostroeniya Otkrytoe|Magnetar|Magtech|Marinatec|Maxitechgroup|Megel|Mekom|Melkom|Narinco|Empire|Semiconductor|Oao|Ojsc|Olkebor Oy|Ooo|Obshchestvo Vneshneekonomicheskoe|Pao Krasnoyarskgazprom|Steel Mills|Pjsc|Resort)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Rosneft|Commercio|Satco|Sdb Ire Ras|Elmayan And Sons|Offshore|Petrochemicals|Fzc|Skylinks|Sl Desarrollos|Sm Way|Source Com|Specelkom|Spekelectrongroup|Sputnik E|Stroygazmontazh|Stroytransgaz Holding|Optical|Surgutneftegas|Syarikat Penghantaran|Transoil)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\b(?:Transphere|Uralvagonzavod|Solution|Bazalt|Navy|Chemical|Munitions|Yarra Anstalt|Zao Yamalgazinvest|Zener Lebanon|Zte Parsian|Company|\sCo\.?\s|Corporation|Corps?\.|Ltd\.?|Limited)\b/) {
            entityType = "O"
        } else if (name =~ /(?i)\sS\.R\.O\.(?:\s|$)/) {
            entityType = "O"
        } else if (name =~ /(?i)\sA\.G\.(?:\s|$)/) {
            entityType = "O"
        } else if (name =~ /(?i)Agroplast/) {
            entityType = "O"
        }

        //border-less matcher
        if (name =~ /^\s*(?-i)[A-Z'\.-]+\s*$|^WWW\.|\d+|\bS\.[AC]\.|\bCIA\.|\bE\.U\.|\bD\.O\.O\.|\Bexport\b/) {
            entityType = "O"
        }
    }
    if (entityType.equals("O")) {
        if (name =~ /(?i)(?:Rafie, Mohammad|Mohammad Rafie|Santos Isidro de la Paz|AGNESE, SABA|SABA AGNESE|AMBAYE, SABA|SABA AMBAYE|Brian Douglas Woodford|MAGHAZEHE, BAHRAM|BAHRAM MAGHAZEHE|MAGHAZEHE, BEN|BEN MAGHAZEHE|MAGHAZEHE, BENJAMIN|BENJAMIN MAGHAZEHE)/) {
            entityType = "P"
        }
    }

    if (name =~ /(?:Asa\'ib Ahl al\-Haq|Aviazapchast|Al Jaysh al Sha\'bi|Fifth Border Guard Regiment|Kata\'ib Sayyid al\-Shuhada)/){
        entityType = "O"
    }


    return entityType
}

def sanitize(data) {
    data = data.toString().replaceAll(/^F\.K\.A\. /,"")
    data = data.replaceAll(/(?<=33rd TsNIII|Gosniiokht)\)\.?/,"")
    data = data.replaceAll(/\(Chinese individual\)/,"")
    data = data.replaceAll(/(?<=Ministry of Defense) \(Syria\)/,"")
    data = data.replaceAll(/(?i)&amp;/, '&').replaceAll(/"/, '').replaceAll(/(?i)entity/, '').trim()
    data = data.replaceAll(/(?:\(individual\)|\(individual in China\))/,"")
    data = data.replaceAll(/(?<=T-Rubber Co.),/,"")
    return data
}

// remove country name from Entity's
def sanitizeEntityName(def name){

    name = name.toString().replaceAll(/(?i)\((?:china|turkey|russia|iraq|Kursk|Yarmouk|Pakistan)\)$/,"") .trim()

    return name
}

def sanitizeDate(data) {

    data = data.toString().replaceAll(/procurement ban ended.+/,"")
    return data.replaceAll(/(?ims)^(.*)(?=\s\d{1,2}\\/\d{1,2}\\/\d{1,4})/, "")
}