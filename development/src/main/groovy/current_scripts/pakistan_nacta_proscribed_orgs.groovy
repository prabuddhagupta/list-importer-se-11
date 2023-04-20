package current_scripts

import com.rdc.importer.misc.RelationshipType
import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent

import java.util.regex.Pattern

// Client site
//import com.rdc.rdcmodel.model.RelationshipType

/**
 * Date: 02/14/2019
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Pakistan_Nacta script = new Pakistan_Nacta(context)
script.initParsing()

int i = 1;
def nameIdMap = [:];
for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + i++);
    nameIdMap[entity.getName()] = entity
}

for (entity in context.session.getEntitiesNonCache()) {
    for (association in entity.getAssociations()) {
        getfullName = script.nameMap(association)
        otherEntity = nameIdMap[getfullName];
        if (otherEntity && !isAlreadyAssociated(entity, otherEntity)) {
            scrapeEntityAssociation = new ScrapeEntityAssociation(otherEntity.getId());
            scrapeEntityAssociation.setRelationshipType(RelationshipType.ASSOCIATE);
            scrapeEntityAssociation.setHashable(otherEntity.getName(), otherEntity.getType());
            entity.addScrapeEntityAssociation(scrapeEntityAssociation);
        }
    }
    if (entity.getName() =~ /(?im)^\b(?:Khair-un-Naas|Millat-e-lslamia|Khuddam-ul-lslam|Ahle Sunnat Wal|Islami Tehreek)\b/)
        entity.getAssociations().clear();
}

def isAlreadyAssociated(entity, otherEntity) {
    Set associations = otherEntity.getScrapeEntityAssociations();
    boolean isAssos = false;
    if (associations) {
        associations.each { item ->
            if (((ScrapeEntityAssociation) item).getId().equals(entity.getId())) {
                isAssos = true;
            }
        }
    }
    return isAssos;
}

class Pakistan_Nacta {
    final factoryRevision = "12f38e5e5fe8a2bd65086fd4aa00fa3a81bf773b"
    final moduleFactory = ModuleLoader.getFactory(factoryRevision)
    final ScrapianContext context
    final String root = "http://nacta.gov.pk"
    // final String url = root + "/proscribed-organizations/"
    //final String url = root + "/proscribed-organizations-3/"
    final String url = "https://nacta.gov.pk/proscribed-organizations-3"

    def pdfUrl //= "https://nacta.gov.pk/wp-content/uploads/2017/08/Proscribed-OrganizationsEng.pdf"
    final def addressParser

    Pakistan_Nacta(context) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.updateCities([PK: ["Layari"]])

    }

//------------------------------Initial part----------------------//
    def initParsing() {
        def html = context.invoke([url: url, cache: false])

        //def pdfUrlMatch = html =~ /(?i)wp-caption alignnone"><a href="([^"]+.pdf)/
        //Updated regEx
        def pdfUrlMatch = html =~ /(?i)href="([^"]+OrganizationsEng-3.pdf)"/

        if (pdfUrlMatch)
            pdfUrl = pdfUrlMatch[0][1]

        def text = pdfConverter(pdfUrl)
        parsePdf(text)
    }

    def parsePdf(text) {
        text = text.toString().replaceAll(/(?ism)\A.+?Name of organization.+?notification$/, "")

        text = fixPdf(text)

        text = matchAffiliatedOrganization(text)
        //def valueMatch = text =~ /(?is)\b\d{1,2}\b\s(?!\S+,\s\b\d{4}\b)((?:\S+[\s,])+)\s{3,}(\d{2}\s\S+[,\s]\d{4})/
        //Updated
        def valueMatch = text =~ /(?is)\b\d{1,2}\b\s(?!\S+,\s\b\d{4}\b)(.+?)\s{3}(\d{2}\s\S+[,\s]\d{4})/
        //(?is)\d+\s((?:\S+[\s,])+)\s{3,}(\d{2}\s\S+[,\s]\d{4})
        while (valueMatch.find()) {
            def associationList = []
            def name, date;
            name = valueMatch.group(1)
            def matchedPart = bracPartMatch(name)
            date = valueMatch.group(2)
            date = fixDate(date)
            if (matchedPart[3]) {
                def assoAlias = bracPartMatch(matchedPart[3])
                associationList.add([matchedPart[3], date, assoAlias[1], assoAlias[2]])
            }

            def aliasArr = matchedPart[0].split(/\//) //Daish/ISIL/IS/ISIS
            if (aliasArr.size() > 1) {
                (1..(aliasArr.size() - 1)).each {
                    matchedPart[1].add(aliasArr[it])
                    matchedPart[0] = matchedPart[0].replaceAll(/\/\b${aliasArr[it].trim()}\b/, "")
                }
            }

            createEntity(matchedPart[0], date, matchedPart[1], matchedPart[2], associationList)
        }
    }


    def matchAffiliatedOrganization(def text) {
        def association, dataMatch, affiliatedOrg, assocMatch, name, date
        def affiliatedOrgMatch = text =~ /(?is)\b(?:3|69|70)\b.*?(?=\b(?:4|70|71)\b)/
        while (affiliatedOrgMatch) {
            def matchedPart = []

            affiliatedOrg = affiliatedOrgMatch.group(0)
            affiliatedOrg = affiliatedOrg.replaceAll(/(?is)\b[243][0123456789]\b[^\n\.]+\d{4}/, "")
            dataMatch = affiliatedOrg =~ /(?is)\d+\s((?:\S+[\s,])+)\s{3,}(\d{2}\s\S+[,\s]\d{4})/
            if (dataMatch) {
                name = dataMatch[0][1]
                date = fixDate(dataMatch[0][2])
                def deletRow = Pattern.quote(dataMatch[0][0])
                text = text.replaceAll(/${deletRow}/, "")

                matchedPart = bracPartMatch(name)
                affiliatedOrg = affiliatedOrg.replaceAll(/${deletRow}/, "")
            }

            def associationList = []
            def associationDate
            assocMatch = affiliatedOrg =~ /\b[iv]+\b\.\s*((?:\S+\s)+)/
            def dateMatch = affiliatedOrg =~ /\d+\s\S+,\s*\d{4}/
            if (dateMatch) {
                associationDate = fixDate(dateMatch[0])
            }
            while (assocMatch.find()) {
                association = assocMatch.group(1)

                def value = bracPartMatch(association)
                associationList.add([value[0], associationDate, value[1], value[2]])
            }

            createEntity(matchedPart[0], date, matchedPart[1], matchedPart[2], associationList)
        }
        return text
    }

    def bracPartMatch(def name) {
        def address, deletPart
        def aliasList = [];
        def assocPart;

        def assMatch = name =~ /(?i)\((?:EX\-?|SplinterGp.\s*of\s*)([^\)]+)\)/
        if (assMatch) {
            assocPart = assMatch[0][1]
            assocPart = assocPart.replaceAll(/(\([^\)]+)$/, '$1' + ")")
            deletPart = Pattern.quote(assMatch[0][0])
            name = name.replaceAll(/$deletPart/, "")
        }

        def bracMatch = name =~ /\(([^\)]+\)?)\)\.?/

        while (bracMatch.find()) {
            def bracPart = bracMatch.group(1)
            deletPart = Pattern.quote(bracMatch.group(0))
            name = name.replaceAll(/$deletPart/, "")
            if (bracPart =~ /(?i)(?:Layari)/) {  //(?i)(?:Afghanistan|Lebanon|Syria|Peninsula|iraq|Uzbekistan)
                address = bracPart
            } else {
                aliasList.add(bracPart)
            }
        }

        def commaPartMatch = name =~ /,\s*([^,]+)$/
        if (commaPartMatch) {
            address = commaPartMatch[0][1].trim()
            name = name.replaceAll(/${commaPartMatch[0][0]}/, "")

        }
        return [name, aliasList, address, assocPart]
    }

//------------------------------Filter part------------------------//
    def fixPdf(def text) {
        text = text.replaceAll(/(?ism)(\d{4}\s{16,20})((?:\S+\s)+)(^\s{30,}\d{2})\s{20,}(\d+\s\S+\s\d{4})\n^\s{30,}([^\n]+)/, { def a, b, c, d, e, f -> return b + '\n' + d + ' ' + c.trim() + ' ' + f + '    ' + e + '\n' })
        //Balawaristan National
        text = text.replaceAll(/(?ism)^\s{0,5}((?:\S+\s)+)\s*^\s{20,}(\d+\s+\S+,\s\d+)\s*(\d+\s(?:\S+[\s,])+\s{3,}\d{2}\s\S+[,\s]\d{4})\s*^\s*(\d{2})\s((?:\S+[^\n\S]*)+)/, { def a, b, c, d, e, f -> return e + ' ' + sanitize(b) + sanitize(f) + '    ' + c + '   ' + d + '\n' })
        //Khair-un-Naas

        //text = text.replaceAll(/(?ism)^\s{30,}([a-z]+\s(?:\S+\s)+)\s*(\s*\d+\s(?:\S+[\s,])+\s{3,}\d{2}\s\S+[,\s]\d{4})\s+(\d+)\s((?:\S+\s)+)\s{3,}(\d{2}\s\S+[,\s]\d{4})/, { def a, b, c, d, e, f -> return c + '     ' + d + ' ' + sanitize(b) + ' ' + e + '    ' + f + '\n' })
        //Khuddam-ul-lslam

        //Updated Regex-20-01-2022
        text = text.replaceAll(/(?ism)^\s{30,}([a-z \(]+)\n(\s*\d+\s*(?:\S+[\s,'\-])+\s{3,}\d{2}\s\S+[,\s]\d{4})\s+(\d+)\s((?:\S+\s)+)\s{3,}(\d{2}\s\S+[,\s]\d{4})/, { def a, b, c, d, e, f -> return c + '     ' + d + ' ' + sanitize(b) + ' ' + e + '    ' + f + '\n' })
        //Al-Qa'ida
        text = text.replaceAll(/(?ism)(\d+\s(?:\S+[\s,])+\s{3,}\d{2}\s\S+[,\s]\d{4})\s*([a-z]+\s(?:\S+\s)+)\s*(\d+)\s*(\d{2}\s\S+[,\s]\d{4})\s*((?:\S+[^\n\S]*)+)/, { def a, b, c, d, e, f -> return b + '     ' + d + ' ' + sanitize(c) + ' ' + f + '    ' + e + '\n' })
        //Balawaristan // new regex

        //Amar bil
        // text = text.replaceAll(/(?ism)Amar bil Maroof Wa Nahi Anil Munkir \(Haji Namdaar/, "")
        // text = text.replaceAll(/(?ism)57 group\)/, "57 Amar bil Maroof Wa Nahi Anil Munkir (Haji Namdaar Group)")

        text = text.toString().replaceAll(/(?ism)(?<=(?:\d{2}))\s+(?=organizations)/, "BR")

        text = text.replaceAll(/(?is)(?<=\(\bjud\b)\)(?!\))/, "))")
        text = text.replaceAll(/(?i)(?<!\()\s+(?=\bex\b)/, "(")
        text = text.replaceAll(/(?im)(?<=\bssp\b)\s(?!\))/, ")")
        text = text.replaceAll(/(?i)(Gilgit)\s+(Baltistan)(?!,)/, '$2' + ' ' + '$1')
        text = text.replaceAll(/(?<!,)\s+(?=Gilgit(?!\s+Baltistan,))/, ",")
        return text

    }

    def fixDate(def text) {
        def date, month, year;
        def dateMatch;

        if ((dateMatch = text =~ /(?i)(\d+)\s+(\w+),\s+(\d+)/)) {
            date = dateMatch[0][1]
            month = getMonthNo(dateMatch[0][2])
            year = dateMatch[0][3]

            if (date =~ /^\s*\d\s*$/) {
                date = '0' + date
            }
            if (month =~ /^\s*\d\s*$/) {
                month = '0' + month

            }
            return month + '/' + date + '/' + year
        }
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

//------------------------------Entity creation part---------------//
    def createEntity(name, date, aliasList, addr, associationList = []) {
        def entity = null

        name = sanitizeName(name)
        if (name) {
            entity = context.findEntity(["name": name, "type": "O"])
            if (!entity) {
                entity = context.getSession().newEntity()
                name = name.replaceAll(/(?m)^([^\(]+)\s*\)\s*$/, '$1')
                name = sanitize(name)
                entity.setName(name)
                entity.type = "O"
            }

            if (aliasList.size() > 0) {
                aliasList.each {
                    it = it.replaceAll(/(?i)\bex\b(?::|-)?/, "").replaceAll(/^(\s*[^\(]+)\)/, { def a, b -> return b }).trim()
                    entity.addAlias(it)
                }
            }
            createEntityCommonCore(entity, addr, date)

            associationList.each {
                def newEntity, assoc
                assoc = it[0]
                assoc = assoc.replaceAll(/(?:\([^\)]+\))+/, "")
                assoc = sanitize(assoc)
                assoc = fixName(assoc)
                entity.addAssociation(assoc)
                assoc = nameMap(assoc)

                newEntity = context.findEntity(["name": assoc, "type": "O"])
                if (!newEntity) {
                    newEntity = context.getSession().newEntity()
                    assoc = fixName(assoc)
                    newEntity.setName(assoc)
                    newEntity.type = "O"

                    if (it[2]) {
                        it[2].each { def assAlias ->
                            newEntity.addAlias(sanitize(assAlias))
                        }
                    }
                    createEntityCommonCore(newEntity, it[3], it[1])
                }
            }
        }
    }

    def createEntityCommonCore(def entity, def addr, def date) {
        ScrapeEvent event = new ScrapeEvent()
        event.category = "TER"
        event.subcategory = "ASC"
        event.setDescription("This entity appears on the Pakistan National Counter Terrorism Authority list of Proscribed Organizations. These entities have been proscribed by the Ministry of Interior Anti-Terrorism Act 1997.")
        event.setDate(date)
        entity.addEvent(event)
        ScrapeAddress scrapeAddress = new ScrapeAddress()
        if (addr) {
            addr.split(/\s*\/\s*/).each {
                def addrMap = addressParser.parseAddress([text: it, force_country: true])
                scrapeAddress = addressParser.buildAddress(addrMap)
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress)
                }
            }

        } else {
            scrapeAddress.country = "Pakistan"
            entity.addAddress(scrapeAddress)
        }
    }

//------------------------------Misc utils part---------------------//
    def sanitize(data) {
        return data.replaceAll(/&amp;/, '&').replaceAll(/&nbsp;/, '').replaceAll(/\s{2,}/, " ").trim()
    }

    def fixName(def name) {
        name = name.replaceAll(/(?i),\s*pakistan/, "")
    }

    def pdfConverter(pdfUrl) {
        def pdfFile = context.invokeBinary([url: pdfUrl, clean: false, cache: false]);

        def pmap = [:] as Map
        pmap.put("1", "-layout")
        pmap.put("2", "-enc")
        pmap.put("3", "UTF-8")
        pmap.put("4", "-eol")
        pmap.put("5", "dos")
        pmap.put("6", "-raw")
        def pdfText = context.transformPdfToText(pdfFile, null)

        return pdfText.toString().replaceAll(/\r\n/, "\n")
    }

    def nameMap(def name) {
        if (name =~ /(?i)\bSSP\b/) {
            return "Sipah-i -Sahaba Pakistan"
        } else if (name =~ /(?i)\bTJP\b/) {
            return "Tehrik-e-Jaffria Pakistan"
        } else if (name =~ /(?i)\bJEM\b/) {
            return "Jaish-e-Muhammad"
        } else if (name =~ /(?i)Jamat-ul-Da'awa/) {
            return "Jamaat-ul-Da'awa"
        } else {
            return name
        }
    }

    def sanitizeName(def name) {
        name = name.toString().replaceAll(/(?ism)^.+?(?=Al\-akhtar trust)/, "").trim()
        name = name.toString().replaceAll(/^\s*Group\)\s*$/, "")
        name = name.toString().replaceAll(/(?<=\-)\s*/, "")
        name = name.toString().replaceAll(/^null$/, "")

        return name.trim()
    }
}