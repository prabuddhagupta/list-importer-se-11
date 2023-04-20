package current_scripts

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.AliasType
import com.rdc.scrape.*
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.WordUtils
import scrapian_scripts.utils.CharMappingUtil

//dev-end
//import com.rdc.importer.misc.AliasType

import java.nio.file.Files
import java.nio.file.Paths

context.session.escape = true
context.setup([socketTimeout: 100000, connectionTimeout: 50000, retryCount: 3, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Safari/537.36"])

dscr = "This entity appears on the Australia Department of Foreign Affairs and Trade (DFAT) published list of all persons and entities who are subject to targeted financial sanctions under Australian sanctions law."

moduleFactory = ModuleLoader.getFactory("92e68ce6243d668ff4c8d665108d88dfc0b633ce")
addressParser = moduleFactory.getGenericAddressParser(context)
addressParser.reloadData()
addressParser.updatePostalCodes([NL: [/\d{4}\s*\w{2}/], DE: [/\d{5}/]])
addressParser.updateStates([CG: ["North Kivu"], AU: ["New South Wales"]])
addressParser.updateCities([ID: ["Sukoharjo", "Sumedang", "Demak", "Kudus", "Masaran", "Pacitan", "Jombang", "Makassar", "Rembang", "Sambi~ Boyolali", "Cianjur"],
                            IR: ["Isfahan", "Zahedan", "Kashan", "Karaj"],
                            IQ: ["Al-Qaim", "Tikrit", "Kubaisi", "Samarra", "Iskandariya", "Abu Ghurayb", "Khan Dari", "Governate", "Harara", "Al Rashidiya", "Milla", "Alexandria", "Babylon", "Baghdad", "Dora"],
                            IN: ["Sambhal"],
                            AU: ["Brisbane", "Evatt"],
                            AF: ["Lalpura", "Keshim", "Kohistan", "Gereshk", "Zaranj", "Giyan", "Dehrawood", "Jaghatu", "Shahjoi", "Sharana", "Deh Rawud", "Chora", "Dai Chopan", "Khas Uruzgan", "Nad-e-Ali", "Kohe Safi", "Alingar", "Zadran", "Kabul", "Kandahar", "Ghanzi", "Nahr-e Saraj Distrcit", "Sha Wali Kot", "Gardez", "Darzab", "Kharwar", "Surkh Rod", "Arghandaab", "Zurmat", "Shinwar", "Tirin Kot", "Lashkar Gah", "Shakardara", "Waghaz", "Washer", "Spin Boldak", "Gelan", "Garmser", "Andar", "Panjwai", "Chaki Wardak"],
                            BA: ["Bujakovici-Skelani", "Municipality of Kalinovik", " Banja Luka", "Derventa", "abokvica", "Cerkazovici", "Bratunac", "Srebrenica", "Marićka", "Kostolomci", "Foča", "Duratovci", "Maricka", "Domanovici", "Donja lijeska", "Brežani", "Nezirovici", "Zlavast", "Rujište", "Rocevic", "Višegrad", "Goles", "Duratovci", "Lokvice", "Paoca", "Vlasenica", "Goražde", "Popovici", "Petrov Gaj", "Cazin", "Jelacici", "Mostar", "Bijeljina"],
                            BE: ["Antwerp"],
                            CD: ["Butembo", "Rutshuru", "Kinshasa", "Mambasa", "Bas-Uolo", "Haut-Uolo", "Bukavu"],
                            CF: ["Nana-Grebizi", "Ndele", "Bimbo", "Mbaiki", "Birao", "Boda"],
                            CN: ["Linjiang"],
                            CH: ["Altstatten"],
                            CG: ["Ariwara", "Manono", "Rutshuru", "Walikale", "Rutshuru Territory"],
                            DE: ["Werl prison", "Bonn", "Ludwigshafen"],
                            DZ: ["Meftah", "Kabylie", "Amenas", "Maskara", "Mahdia", "Faidh El Botma", "Ghardaia", "Algiers"],
                            ES: ["San Sebastian", "Elorrio", "Baracaldo", "Ondarroa", "Asteasu", "Bilbao", "San Sebastian \\(Guipuzcoa\\) ", "Escoriaza \\(Guipuzcoa\\)", "Basauri", " Baracaldo \\(Biscay", "Llodio-Areta", "Guernica \\(Vizcaya"],
                            EG: ["Kafr Al-Shaykh", "Beni-Suef", "Zaqaziq", "Sharqiyah", "Governate"],
                            ER: ["Massaua"],
                            ET: ["Ogaden Region"],
                            FR: ["Bourg la Reine", "Grenoble"],
                            HR: ["Dakovo", "ibenik", "Licka Jesenica", "Hrvatska Kostajnica", "Svinjarevci", "Beli Manastir", "Kordunski Ljeskovac", "Knin", "Brzaja", "Pakrac", "Vojnic", "Bukovica", "Berak", "Rijeka", "Blatuša", "Glina", "Lovas", "Varos/Slanovski", "Velika Peratovica", "Osijek", "Kusonje", "Dubrovnik", "Vukovar"],
                            IT: ["Plebiscito"],
                            ML: ["Anefif", "Amassine", "Al Moustarat", "Tabankort", "In Khalil", "Ménaka"],
                            MA: ["Marrakech", "Essaouria", "Laayoune"],
                            ME: ["Nikšic"],
                            NO: ["Heimdalsgate"],
                            US: ["Las Cruces"],
                            QA: ["Al-Laqtah"],
                            KE: ["Lamu Island", "Majengo Area", "Dadaab"],
                            KR: ["Pyongyang"],
                            KP: ["Pyongyang", "Hungnam", "Musan", "Chongjin", "Kangdong", "Hamhung", "Kaesong", "Wonsan", "Chung-guyok", "Pyongan-Pukto"],
                            KM: ["Moroni"],
                            LB: ["Beirut"],
                            LY: ["Jalo", "Benghazi", "al Aziziyya", "Garabulli", "Bengasi", "Zawiya", "Sirte", "Sirte", "Sabratha", "Khoms"],
                            GA: ["Mouila"],
                            SS: ["Bor", "Wau", "Aweil", "Malualkon", "Yei", "Yirol"],
                            SA: ["Kharj", "Medina", "Daina", "the Arabian Peninsula", "al-Duwadmi", "Tarut", "Saqra", "Jeddah", "Al Baraka", "Al Ihsa", "Buraidah"],
                            SD: ["Kabkabiya", "El-fasher"],
                            SO: ["Hargeysa", "Mogadishu", "Kismaayo"],
                            SY: ["Binnish", "Al Hasakah", "Albu Kamal", "Raqqah", "Damascus", "Az Zabadani", "Al Rawdah", "Khan Shaykhun", "Aleppo", "Homs", "Ghutah", "Deir ez-Zor"],
                            RS: ["Valjevo", "Backa Palanka", "Koznica", "Ravno Selo", "Kruševac", "Sremska Mitrovica", "Ruma", "Zemun"],
                            RW: ["Mugusa", "Ndusu", "Kigali"],
                            RU: ["Grozny", "Khabarovsk"],
                            TD: ["Haraze Mangueigne"],
                            PH: ["Tuburan", "Atimonana"],
                            OM: ["Jebel Akhdar"],
                            ME: ["Niksic", "Šavnik"],
                            UA: ["Makiivka", "Makeevka", "Voroshilovgrad - Luhansk", "Luhansk"],
                            TN: ["Ghardimaou", "Ben Gardane", "Asima"],
                            TW: ["Yun Lin Hsien"],
                            TR: ["Karliova", "Reyhanl?"],
                            PK: ["Chaman", "Mirpur Khas", "karachi", "Sargodha", "Gujranwala", "Okara", "Quetta", "Swat Valley", "Pishin", "Sangrar", "Lahore", "Bahawalpur"],
                            YE: ["Dahyan", "Al Mukalla"]])

def rootURL = "https://www.dfat.gov.au"
def mainURL = "https://www.dfat.gov.au/international-relations/security/sanctions/Pages/consolidated-list"
//Invoke
def urls = new URL(mainURL).openConnection()
def data = urls.getInputStream().getText()
def nat = false
def xlsLink
def xlsMatcher = data =~ /(?ism)href="([^"]*?)"[^>]*?>Consolidated List/
if (xlsMatcher.find()) {
    xlsLink = rootURL + xlsMatcher.group(1)
    println xlsLink
}

//Set Download File path
def xlsFilePath = "/tmp/regulation8_consolidated.xls"
//def xlsFilePath = "/home/maenul/Downloads/regulation8_consolidated.xls"

//Download file
downloadFile(xlsLink, xlsFilePath)
def spreadsheet = context.invokeBinary([url: "file:////" + xlsFilePath])
def xml = context.transformSpreadSheet(spreadsheet, [validate: true, escape: true])
def rows = new XmlSlurper().parseText(xml.value)
cmu = new CharMappingUtil()
countries = []
prepCountries()
citizenshipMap = [:]
prepCitizenshipMap()
addressToken = /(?is)Reportedly\s*located\s*in/
splitAddrToken = /(?i)^\s*(?:Located|Operates)\s*in\s*|\s*Believed\s*to\s*be\s*in|\s*\beither\b/
semiColonToken = /Iraq|Iran|Syria|Rwanda\)/


def getParamMap() {
    def param = [:]
    param[":authority"] = "www.dfat.gov.au"
    param[":method"] = "GET"
    param[":path"] = "/international-relations/security/sanctions/Pages/consolidated-list"
    param[":scheme"] = "https"
    param["accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
    param["accept-language"] = "en-US,en;q=0.9"
    param["cache-control"] = "max-age=0"
    param["cookie"] = "_ga=GA1.3.1972104421.1595430053; _gid=GA1.3.1071275336.1595577010"
    param["if-modified-since"] = "Fri, 24 Jul 2020 07:50:07 GMT"
    param["if-none-match"] = "\"1595577007\""
    param["sec-fetch-dest"] = "document"
    param["sec-fetch-mode"] = "navigate"
    param["sec-fetch-site"] = "none"
    param["sec-fetch-user"] = "?1"
    param["upgrade-insecure-requests"] = "1"

    return param
}

def downloadFile(def link, def path) {
    InputStream inn = new URL(link).openStream()
    Files.deleteIfExists(Paths.get(path))
    Files.copy(inn, Paths.get(path))

}

rows.row.each { row ->

    // Jira PI-18 - new xls file did not have a header for "reference"
    // Re-worked to find to first column where the reference ID is contained - RCH 4/13/2011
    def reference = row.children()[0].text().trim()
    //reference = row.reference.text().trim();
    def key = StringUtils.removeEnd(reference.replaceAll(/[a-zA-Z].*/, ""), ".0")
    if (StringUtils.isBlank(key)) {
        key = reference
    }
    def type = row.type.text().toUpperCase().trim() == "ENTITY" ? "O" : "P"
    def entityKey = [key, type]
    def name = StringUtils.chomp(row.name_of_individual_or_entity.text().trim(), ",").trim()

    name = name.replaceAll("\u2011", "-")
    if (type == "P" && StringUtils.countMatches(name, ",") == 1 && !name.contains(".") &&
        !(name.toUpperCase().contains(" JR") || name.toUpperCase().contains(" SR") ||
            name.contains(" III") || name.contains(" IV"))) {
        def nameparts = name.split(",")
        if (nameparts.length == 2) {
            name = (nameparts[1].trim() + " " + nameparts[0].trim()).trim()
        }
    }

    if (name.trim().length() > 0) {
        def entity = context.findEntity(entityKey)
        if (entity == null) {
            entity = context.newEntity(entityKey)
            entity.setDataSourceId(key)
            if (name.equals("Kala-Electric (aka Kalaye Electric)")) {
                name = "Kala-Electric"
                entity.addAlias("Kalaye Electric")
            }
            entity.setName(latinize(name.trim()))
            entity.setType(type)
        } else {
            if (!row."name_type".text().toUpperCase().trim().equals("ORIGINAL SCRIPT")) {
                if ((name = name.replaceAll("\\(?[\u0600-\u06FF]\\)?", "").trim()).length() > 0) {
                    if (reference =~ /[a-zA-Z]/) {
                        addAlias(entity, name.trim())
                    } else {
                        tempAlias = entity.name
                        context.session.changeEntityName(entity, latinize(name.trim()))
                        addAlias(entity, tempAlias)
                    }
                }
            } else if (row."name_type".text().toUpperCase().trim().equals("ORIGINAL SCRIPT")) {
                ScrapeAlias sa = new ScrapeAlias()
                sa.setName(trimAlias(name))
//                sa.setType(AliasType.LOC)
                script = determineScript(sa.getName())
                if (script) {
                    sa.setScript(script.split(",")[0])
                }
                entity.addDetailedAlias(sa)
            }
        }
        createEvents(entity, row.committees.text())
        createPositions(entity, filterText(row.additional_information.text()))
        createPlacesOfBirth(entity, row.additional_information.text().trim())
        createNationalities(entity, row.additional_information.text().trim())
        createCitizenships(entity, row.citizenship.text().trim())
        createPassports(entity, row.citizenship.text().trim(), row.additional_information.text().trim())
        createRemark(entity, filterText(row.listing_information.text()), filterText(row.additional_information.text()))
        createDobs(entity, StringUtils.removeEnd(row.date_of_birth.text().trim(), ".0"))
        createAddresses(entity, filterText(row.address.text()), false)
        createAddresses(entity, row.place_of_birth.text().trim(), true)


    }
}

def addAlias(entityIn, aliasIn) {
    aliasIn = trimAlias(aliasIn)
    theScript = determineScript(aliasIn)

    if (theScript == 'Latn') {
        aliasIn = aliasIn.replaceAll(/\(.*?:\s*$/, "")
        aliasIn = fixSpaces(aliasIn)
        entityIn.addAlias(latinize(aliasIn))
    } else {
        ScrapeAlias sa = new ScrapeAlias()
        sa.setName(aliasIn)
        // sa.setType(com.rdc.rdcmodel.model.AliasType.AKA);
//        sa.setType(AliasType.AKA)
        if (theScript) {
            sa.setScript(theScript.split(",")[0])
        }
        entityIn.addDetailedAlias(sa)
    }
}

def trimAlias(aliasIn) {
    parenMatch = aliasIn =~ /([^(]*?)\s*\(([^)]*)\)$/
    if (parenMatch.find()) {
        g2 = parenMatch.group(2)
        //if all CAPS keep
        if (!(g2 =~ /^[A-Z]+$/)) {
            if (g2.contains(' ') || g2.length() < 4) {
                return trimAlias2(aliasIn.replaceAll(/\(.*\)/, ''))
            } else if (parenMatch.group(1).contains(g2.substring(0, 4))) {
                //if lower case and first four char match to rest of name (a bit of a hack), keep //Lom-ali Butayev (Butaev)
                return trimAlias2(aliasIn)
            } else {
                return trimAlias2(aliasIn.replaceAll(/\(.*\)/, ''))
            }
        } else {
            return trimAlias2(aliasIn)
        }
    } else {
        //could be of type 'AA (b) CC (d)' -- KEEP parens in this case
        return trimAlias2(aliasIn)
    }
}

def trimAlias2(aliasIn) {
    if (!aliasIn.contains("(") && aliasIn.trim().endsWith(")")) {
        return aliasIn.replaceAll(/\s*\)\s*$/, '').trim()
    } else {
        return aliasIn.trim()
    }
}

def filterText(text) {
    text = text.replaceAll(/\s+/, ' ')
    text = text.replaceAll(/N\/A/, "")

    return text.trim()
}

def fixAddr(str, ScrapeEntity entity) {
    str = str.replaceAll(/(?i)\bprovince\b\s*$|^\W+/, "")
    str = str.replaceAll(/(?i)\(previous\s*address\)/, "")
    str = str.replaceAll(/(?is)Chaman, Baluchistan Province, Pakistan/, "Chaman, Baluchistan, Pakistan")
    str = str.replaceAll(/(?i)^\s*\bEgyptian\b\s*$/, "Egypt")
    str = str.replaceAll(/(?i)^\s*US\s*Custody\s*$/, "US")
    str = str.replaceAll(/(?i)\s*former\s*Yugoslavia\s*$/, "Yugoslavia")
    str = str.replaceAll(/(?i)suburb\s*of\s*Baghdad\s*$/, "Baghdad")
    str = str.replaceAll(/(?i)near\s*Tikrit$/, "Tikrit")
    str = str.replaceAll(/(?i)Cagayan\s*Province\s*,/, "Cagayan,")
    str = str.replaceAll(/(?i)Phillipines\s*$/, "Philippines")
    str = str.replaceAll(/(?i)Pakista\s*$/, "Pakistan")
    str = str.replaceAll(/(?i)^\s*not\s*listed\s*$/, "")
    str = str.replaceAll(/(?i)No\s*3,\s*Shafah\s*$/, "No 3,Shafah,Iran")
    str = str.replaceAll(/(?i)\(off\s*al-Zawiyah\s*Street\),/, "(off al-Zawiyah Street),Iran,")
    str = str.replaceAll(/(?i)Sehanya\s*Dara'a\s*Highway,/, "Sehanya Dara'a Highway,Syria,")
    str = str.replaceAll(/(?i)(?:-|,)?\s*\bDamascus\b\s*-?/, ",Damascus,")
    str = str.replaceAll(/(?i)Moskovskaya\s*obl\s*$/, "Moskovskaya obl,Russia")
    str = str.replaceAll(/(?i)Jakoysky\s*district$/, "Jakoysky,Russia")
    str = str.replaceAll(/(?i)Bakhchisarayski\s*district\s*$/, "Bakhchisarayski,Ukraine")
    str = str.replaceAll(/(?i)city\s*of\s*Yalta/, "Yalta,Ukraine")
    str = str.replaceAll(/(?i)Al-Rib’\s*al-Sharqi\s*$/, "Al-Rib’ al-Sharqi,Yemen")
    str = str.replaceAll(/(?i)Helmand\s*$/, "Helmand,Afghanistan")
    str = str.replaceAll(/(?i)Unity\s*State\s*$/, "Unity State,South Sudan")
    str = str.replaceAll(/(?i)Lakes\s*State\s*$/, "Lakes State,South Sudan")
    str = str.replaceAll(/(?i)^\s*Sudan\s*$/, "North Sudan")
    str = str.replaceAll(/(?i)^\s*\bPOB\b\s*$/, "")
    str = str.replaceAll(/(?i)Lablan village/, "Lablan")
    str = str.replaceAll(/Vedeno Village/, "Vedeno")
    str = str.replaceAll(/(?i)Siddiq Khel village/, "Siddiq Khel")
    str = str.replaceAll(/(?i)Makassar, South Sulawesi, Indonesia/, "Makassar,Sulawesi, Indonesia")
    str = str.replaceAll(/(?i)^\s*Branch\s*Office\s*\d+\s*$/, "")
    str = str.replaceAll(/(?i)^\s*Chairman,\s*Second\s*Economic\s*Committee\s*$/, "")
    str = str.replaceAll(/(?i)^\s*PO\s*Box\s*1584864813\s*$/, "Unknown")
    str = str.replaceAll(/(?i)Wilaya\s*of\s*Blida/, "Blida")
    str = str.replaceAll(/(?i)\(previous(?:ly)?\s*locat(?:ion|ed)\s*(?:in)?\)\s*$/, "")
    str = str.replaceAll(/(?i)\(possible\s*alternative\s*location.*?$/, "")
    str = str.replaceAll(/(?i)\(a\s*territory\s*on\s*the\s*border.*?$/, "")
    str = str.replaceAll(/(?i)\(as\s*at\s*\w{3}\.\s*\d{4}\)\s*$/, "")
    str = str.replaceAll(/(?i)\(as\s*of\s*\w+\s*\d{4}\)\s*$/, "")
    str = str.replaceAll(/(?i)\(since\s*\w+\s*\d{4}\)\s*$/, "")
    str = str.replaceAll(/(?i)\s*Sudan\/South Sudan\s*$/, "South Sudan")
    str = str.replaceAll(/(?i)as\s*of\s*\w{3}\.\s*\d{4}\s*$/, "")
    str = str.replaceAll(/(?i)\((?:residence)?\s*as\s*at.*?\)$/, "")
    str = str.replaceAll(/(?i)\(since.*?\)$/, "")
    str = str.replaceAll(/(?i)^\s*West\s*Bank\s*$/, "West Bank,Israel")
    str = str.replaceAll(/(?i)\(previous\)\s*$/, "")
    str = str.replaceAll(/(?i)\(previous\s*(?:confirmed|address).*?\)\s*$/, "")
    str = str.replaceAll(/(?i)\(possible\s*location\)\s*$/, "")
    str = str.replaceAll(/(?i)\(Believed status\/location:\s*restricted\s*freedom.*?\)$/, "")
    str = str.replaceAll(/(?i)\(formerly\s*resident\s*at\)\s*$/, "")
    str = str.replaceAll(/(?i)\(previous\s*location\s*until\s*\d{4}\)\s*$/, "")
    str = str.replaceAll(/(?i):\s*this\s*aka\s*only\s*$/, "")
    str = str.replaceAll(/(?i)^Address$/, "")
    str = str.replaceAll(/(?i)region/, "")
    str = str.replaceAll(/(?i)Branch Office \d+/, "")
    str = str.replaceAll(/(?ism)p\.?o\.? box \d+(?:\-\d+|\/\d+|, No \d+)?/, "")
    str = str.replaceAll(/(?i)\(location\s*since\s*\d{4}\)/, "")
    str = str.replaceAll(/(?i)\bthe\b\s*\bPhilippines\b/, "Philippines")
    str = str.replaceAll(/(?i)this\s*aka\s*only/, "")
    str = str.replaceAll(/(?i)\(location as at Sep. 2015\)/, "")
    str = str.replaceAll(/Governo?rate/, "")
    str = str.replaceAll(/(?i)possibly|formerly/, "")
    str = str.replaceAll(/(?i)first floor/, "")
    str = str.replaceAll(/(?i)near Steel Bridge/, "")
    str = str.replaceAll(/(?i)Kharsenoy Village/, "Kharsenoy")
    str = str.replaceAll(/(?i)Near Dergey Manday Madrasa in Dergey Manday Village/, "dergey Manday")
    str = str.replaceAll(/(?i)Shakarlab village/, "Shakarlab")
    str = str.replaceAll(/(?is)al watan newspaper - Damascus - duty free zone/, "Damascus")
    str = str.replaceAll(/neighbourhood/, "")
    str = str.replaceAll(/,\s*likely.*2015\./, "")
    str = str.replaceAll(/(?i)known address\:/, "")
    str = str.replaceAll(/(?i)province\s*\(now.*\)/, "")
    str = str.replaceAll(/(?i)\(Factory\)/, "")
    str = str.replaceAll(/(?i)Zardalu Darra village/, "Zardalu Darra")
    str = str.replaceAll(/(?i)Azan village/, "Azan")
    str = str.replaceAll(/(?i)Tehran-5 kilometers into the /, "")
    str = str.replaceAll(/(?i), P\.O\. Box 12445-885/, "")
    str = str.replaceAll(/(?i)P\.O\. Box 155671311/, "")
    str = str.replaceAll(/(?i)Tuburan, Basilan Province, Philippines/, "Tuburan, Basilan, Philippines")
    str = str.replaceAll(/(?i); company number imo \d+/, "")
    str = str.replaceAll(/(?i)province|district/, "")
    str = str.replaceAll(/(?i)sardar village/, "sardar")
    str = str.replaceAll(/(?i)Mirmadaw village/, "Mirmadaw")
    str = str.replaceAll(/(?i)Kayla Village/, "Kayla")
    str = str.replaceAll(/(?i)Naw Deh village/, "Naw Deh")
    str = str.replaceAll(/(?i)Zumbaleh village/, "Zumbaleh")
    str = str.replaceAll(/(?i)Raymah village/, "Raymah")
    str = str.replaceAll(/(?i)Yatimchai village/, "Yatimchai")
    str = str.replaceAll(/(?i)Sheykhan Village/, "Sheykhan")
    str = str.replaceAll(/(?i)Paliran village/, "Paliran")
    str = str.replaceAll(/(?i)Moni village/, "")
    str = str.replaceAll(/(?i)Turshut village/, "Turshut")
    str = str.replaceAll(/(?i)Palaro Village /, "Palaro")
    str = str.replaceAll(/(?i)Village Birkiani/, "Birkiani")
    str = str.replaceAll(/(?i)Federally Administered Tribal Areas \(FATA\)/, "")
    str = str.replaceAll(/(?i)Chegem-1 Village/, "Chegem-1")
    str = str.replaceAll(/(?i)Shin Kalai village/, "Shin Kalai")
    str = str.replaceAll(/(?i)Mirmandaw village/, "Mirmandaw")
    str = str.replaceAll(/(?i)Marghankecha village/, "Marghankecha")
    str = str.replaceAll(/(?i)Yatimak village/, "Yatimak")
    str = str.replaceAll(/(?i)Nawi Deh village/, "Nawi Deh")
    str = str.replaceAll(/(?i)Ntoke Village/, "Ntoke")
    str = str.replaceAll(/(?i)Shunkrai village/, "Shunkrai")
    str = str.replaceAll(/(?i)Srana village/, "Srana")
    str = str.replaceAll(/(?i)Kamkai Village/, "Kamkai")
    str = str.replaceAll(/(?i)Waka Uzbin village/, "Waka Uzbin")
    str = str.replaceAll(/(?i)Sangesar village/, " Sangesar")
    str = str.replaceAll(/(?i)Iki-Burul Village/, "Iki-Burul")
    str = str.replaceAll(/(?i)Batan village/, "Batan")
    str = str.replaceAll(/(?i)Surkhel village/, "Surkhel")
    str = str.replaceAll(/(?i)Daraz Village/, "Daraz")
    str = str.replaceAll(/(?i)Malaghi Village/, "Malaghi")
    str = str.replaceAll(/(?i)Noori Village/, "Noori")
    str = str.replaceAll(/(?i)Lakari village/, "Lakari")
    str = str.replaceAll(/(?i)De Luy Wiyalah village/, "De Luy Wiyalah")
    str = str.replaceAll(/(?i)Poti village/, "Poti")
    str = str.replaceAll(/(?i)Lakh?i village/, "Laki")
    str = str.replaceAll(/(?i)Sultan Kheyl Village/, "")
    str = str.replaceAll(/(?i)Khadzhalmahi Village/, "Khadzhalmahi")
    str = str.replaceAll(/(?i)Ordzhonikidzevskaya village/, "Ordzhonikidzevskaya")
    str = str.replaceAll(/(?i)Tirpas-Selong Village/, "Tirpas-Selong")
    str = str.replaceAll(/(?i)Marghi village/, "Marghi")
    str = str.replaceAll(/(?i)barlach village/, "barlach")
    str = str.replaceAll(/(?i)Shinkalai village/, "Shinkalai")
    str = str.replaceAll(/(?i)Sahl Village/, "Sahl")
    str = str.replaceAll(/(?i)Bande Tumur Village/, "Bande Tumur")
    str = str.replaceAll(/(?i)Tirin Kot city/, "Tirin Kot")
    str = str.replaceAll(/(?i)\(previously listed as\)/, "Sahl")
    str = str.replaceAll(/(?i)\(support network\)/, "")
    str = str.replaceAll(/(?i)primary address/, "")
    str = str.replaceAll(/(?i)\bfor\b|alt pob/, "")
    str = str.replaceAll(/(?i)\( located at\)/, "")
    str = str.replaceAll(/(?i)a\)/, "")
    str = str.replaceAll(/(?i)Karaj Makhsous Road,/, "")
    str = str.replaceAll(/(?i)\bNSW\b/, "New South Wales")
    str = str.replaceAll(/(?i)10 Kevin Street, Evatt, ACT, 2617/, "10 Kevin Street, Evatt, 2617")
    str = str.replaceAll(/(?i)reportedly located in |\s*federally administered tribal areas, /, "")
    str = str.replaceAll(/(?is)Hamhung, South Hamgyong Province, democratic people's republic of korea/, "Hamhung, South Hamgyong, North Korea")
    str = str.replaceAll(/(?i)Kalesija Municipality, Bosnia/, "Kalesija Municipality, BOSNIA AND HERZEGOVINA")
    str = fixSpaces(str).toString().replaceAll(/^\W+/, "").replaceAll(/(?s)\s+/, " ").trim()

    if (str =~ /^\s*\bTel\b/) {
        entity.addRemark(str.toString())
        str = str.replaceAll(/^\s*\bTel\b.*/, "")
    }

    if (str =~ /^\s*Believed status\/location/) {
        entity.addRemark(str)
        str = str.replaceAll(/^\s*Believed status\/location/, "")
    }

    if (str =~ /(?i)\s*Unknown\s*Soldier\s*/) {
        entity.addRemark("Unknown Soldier")
        str = str.replaceAll(/(?i)\s*Unknown\s*Soldier\s*/, "")
    }

    if (str =~ /(?i)\(imprisoned\)\s*$/) {
        entity.addRemark(str.toString())
        str = str.replaceAll(/(?i)\(imprisoned\)\s*$/, "")
    }

    if (str =~ /(?i)^\s*(?:reportedly)?\s*(?:In)?\s*prison\s*in/) {
        entity.addRemark(str.toString())

        str = str.replaceAll(/(?is)^\s*(?:reportedly)?\s*(?:In)?\s*prison\s*in\s*(.*?)(?:\)|$)/, "\$1")
        str = str.replaceAll(/\(.*/, "")?.trim()
    }

    if (str =~ /(?is)\(\s*in\s*prison\s*\)\s*$/) {
        entity.addRemark(str.toString())
        str = str.replaceAll(/(?is)\(\s*in\s*prison\s*\)\s*$/, "")
    }

    if (str =~ /(?i)^\s*Unknown\s*$/) {
        entity.addRemark(str + " Address")
        str = str.replaceAll(/(?i)^\s*Unknown\s*$/, "")
    }

    if (str =~ /(?i)\b(?:Tel|Telephone|phone)\b/) {
        def strMatch = str =~ /(?is)(\b(?:Tel|Telephone|phone)\b.*?)$/

        if (strMatch)
            entity.addRemark(strMatch[0][1].toString())

        str = str.replaceAll(/(?is)\b(?:Tel|Telephone|phone)\b.*?$/, "")
    }

    if (str =~ /(?i)^\s*Alternative\s*POB|As\s*of/) {
        entity.addRemark(str)
    }

    return str
}

def fixNum(str) {
    str = str.replaceAll(/(?i)^\s*\b\d{12}\b\s*$/, "")

    return str
}

def fixSpaces(str) {
    str = str.replaceAll(/(?s)\s+/, " ").trim()

    return str
}

def maxLenFixAddr(str, ScrapeEntity entity) {
    def addrList = []
    if (str =~ /(?<=\w)\s*;\s*(?=\w+)/) {
        addrList = str.toString().split(/;/)

    } else if (str =~ /\bi{2,3}\b\)/) {
        addrList = str.toString().split(/\b(?:i{2,3}|i?v|vi|vi{2,3}|i?x)\b\)/)

    } else if (str =~ /(?i)Telephone/) {

        if ((telMatch = str =~ /(?is)(Telephone.*)/)) {
            entity.addRemark(telMatch[0][1].toString().trim())
            str = str.replaceAll(/(?i)Telephone.*/, "")
            addrList.add(str)
        }
    } else if (str =~ /(?is)\s*Kafia\s*Kingi\s*\(a\s*territory/) {
        addrList.add("Kafia Kingi,South Sudan")
        entity.addRemark(str)

    } else if (str =~ /(?i)\s*\bTel\b\s*/) {
        def tempStr = (tempStringMatch = str =~ /(?i)\s*\b(Tel\b\s*.*)/)[0][1].toString()
        if (tempStr) {
            entity.addRemark(tempStr)
        }
        str = str.replaceAll(/(?i)\s*\bTel\b\s*.*/, "")
        addrList.add(str)
    } else {
        addrList.add(str)
    }

    return addrList
}

private addDob(ScrapeEntity entity, originalText, String formattedDobString) {
    if (StringUtils.isNotBlank(formattedDobString)) {
        ScrapeDob scrapeDob = parseValidateAndCorrectScrapeDob(entity, formattedDobString)
        if (scrapeDob != null && !entity.getDateOfBirths().contains(scrapeDob) && scrapeDob.getYear().length() > 0) {
            entity.addDateOfBirth(scrapeDob)
        }
    }
}

private createDobs(entity, dobRawText) {
    dobLine = StringUtils.strip(StringUtils.chomp(dobRawText.trim()))
    dobLine.eachLine { dob ->
        dob = StringUtils.strip(StringUtils.chomp(dob))
        if (StringUtils.isNotBlank(dob)) {
            ["\n", ";", ">>", " or ", " OR "].each { delimiter ->
                dob = StringUtils.replace(dob, delimiter, " | ").trim()
            }
            dob = cleanCommonAbbreviations(dob)
            dob = (dob =~ /\([a][s] [^\)|^\(]*\)/).replaceAll("|")
            dob = doesStringContainList(dob) ? (dob =~ /a\)\s|\s[a-zA-Z]\)\s/).replaceAll("|") : dob
            dob = StringUtils.chomp(dob.trim(), "|").trim()
            isEuro = detectEuro(dobRawText)

            dob.tokenize("|").each { token ->
                clean = scrubDobText(token).trim()
                if (!"NA".equals(clean.toUpperCase().trim()) && !clean.toUpperCase().contains("BETWEEN") &&
                    !(clean.split("\\s").length == 1 && clean.toUpperCase().endsWith("/NK"))) {
                    if (clean.length() == 4 && StringUtils.isNumeric(clean)) {
                        StringSource source = new StringSource(clean)
                        addDob(entity, dobRawText, context.parseDate(source, (String[]) ["yyyy"]))
                    } else if (StringUtils.countMatches(clean, "-") == 2) {
                        fmt = isEuro ? ["dd-MMM-yyyy", "(dd-MMM-yyyy)", "dd-MM-yy", "(dd-MM-yy)"] : ["MM-dd-yyyy", "(MM-dd-yyyy)"]
                        if (StringUtils.isAlpha(clean.tokenize("-").get(1).toString())) {
                            fmt = ["dd-MMM-yyyy", "(dd-MMM-yyyy)", "dd-MMM-yy", "(dd-MMM-yy)"]
                        }
                        addDob(entity, dobRawText, context.parseDate(new StringSource(clean), (String[]) fmt))
                    } else if (StringUtils.countMatches(clean, "/") == 2 && !clean.toUpperCase().endsWith("/NK")) {
                        fmt = isEuro ? ["dd/MM/yyyy", "(dd/MM/yyyy)", "dd/MMM/yyyy", "(dd/MMM/yyyy)"] : ["MM/dd/yyyy", "(MM/dd/yyyy)"]
                        addDob(entity, dobRawText, context.parseDate(new StringSource(clean), (String[]) fmt))

                    } else if (clean.split("\\s").length == 3) {
                        clean = StringUtils.remove(clean, ",").trim()
                        clean = cleanCommonAbbreviations(clean).trim()
                        date = context.parseDate(new StringSource(clean), (String[]) ["yyyy dd MMM", "dd MMM yyyy", "dd/MM/yyyy"])
                        if (date == null && StringUtils.isAlpha(clean.split("\\s")[1])) {
                            spell = clean.split("\\s")[0] + " " + getMonth(clean.split("\\s")[1]) + " " + clean.split("\\s")[2]
                            date = context.parseDate(new StringSource(spell), (String[]) ["dd MM yyyy"])
                        }
                        addDob(entity, dobRawText, date)
                    } else if (clean.contains(",")) {
                        clean.split(",").each() { part ->
                            if (part.trim().length() == 4) {
                                addDob(entity, dobRawText, context.parseDate(new StringSource(part.trim()), (String[]) ["yyyy"]))
                            } else {
                                fmt = [isEuro ? "dd/MM/yyyy" : "MM/dd/yyyy", "dd MMM yyyy"]
                                addDob(entity, dobRawText, context.parseDate(new StringSource(part.trim()), (String[]) fmt))
                            }
                        }
                    } else if (clean.split("\\s").size() == 2 && StringUtils.isAlpha(clean.split("\\s")[0]) && !StringUtils.isAlpha(clean.split("\\s")[1])) {
                        addDob(entity, dobRawText, getMonth(clean.split("\\s")[0]) + "/-/" + clean.split("\\s")[1])
                    } else if (clean.contains(" (possibly ")) {
                        one = StringUtils.substringBefore(clean, " (possibly ").trim()
                        addDob(entity, dobRawText, context.parseDate(new StringSource(one), (String[]) ["dd MMM yyyy", "yyyy"]))
                        two = StringUtils.substringAfter(clean, " (possibly ").trim()
                        addDob(entity, dobRawText, context.parseDate(new StringSource(two), (String[]) ["dd MMM yyyy)", "dd MMM yyyy", "yyyy"]))
                    } else if (StringUtils.countMatches(clean, "/") > 2 && clean.tokenize().size() > 1) {
                        clean.tokenize().each { piece ->
                            format = StringUtils.removeEnd(StringUtils.removeStart(piece.trim(), "("), ")").trim()
                            if (!format.toUpperCase().endsWith("/NK")) {
                                def date = context.parseDate(new StringSource(format), (String[]) [isEuro ? "dd/MM/yyyy" : "MM/dd/yyyy", "dd/MMM/yyyy"])
                                addDob(entity, dobRawText, date)
                            }
                        }
                    } else if (StringUtils.contains(clean, ".") && clean.trim().contains(" ")) {
                        StringUtils.split(clean, ".").each { dotPart ->
                            if (dotPart.tokenize().size() == 3) {
                                other = StringUtils.remove(dotPart, ",").trim()
                                date = context.parseDate(new StringSource(other), (String[]) ["yyyy dd MMM", "dd MMM yyyy", "dd/MM/yyyy"])
                                if (date == null && StringUtils.isAlpha(clean.split("\\s")[1])) {
                                    spell = other.split("\\s")[0] + " " + getMonth(other.split("\\s")[1]) + " " + other.split("\\s")[2]
                                    date = context.parseDate(new StringSource(spell), (String[]) ["dd MM yyyy"])
                                }
                                addDob(entity, dobRawText, date)
                            }
                        }
                    } else if (clean.contains(" ") && clean.tokenize().size() % 3 == 0) {
                        section = clean.tokenize()
                        int m = 1
                        while (m <= section.size()) {
                            sday = section.get(m - 1)
                            smonth = section.get(m)
                            syear = section.get(m + 1)
                            if (getMonth(smonth).isInteger()) {
                                dateString = sday + " " + getMonth(smonth) + " " + syear
                                def date = context.parseDate(new StringSource(dateString), (String[]) ["dd MM yyyy"])
                                if (date != null) {
                                    addDob(entity, dobRawText, date)
                                }
                            }
                            m += 3
                        }
                    } else if (StringUtils.split(clean, "/").length == 2 && clean.length() == 9) {
                        StringUtils.split(clean, "/").each { year ->
                            if (year.length() == 4 && StringUtils.isNumeric(year)) {
                                addDob(entity, dobRawText, context.parseDate(new StringSource(year.trim()), (String[]) ["yyyy"]))
                            }
                        }
                    } else if (StringUtils.countMatches(clean, ".") == 2 &&
                        context.parseDate(new StringSource(clean.trim()), (String[]) ["dd.MM.yyyy"]) != null) {
                        addDob(entity, dobRawText, context.parseDate(new StringSource(clean.trim()), (String[]) ["dd.MM.yyyy"]))
                    }
                }
            }
        }
    }
}

private def detectEuro(dobRawText) {
    return (dobRawText =~ /(1[3-9]|2[0-9]|3[01])([-\/.])(0\d|1[012]|\d)\2(19|20)\d\d/).getCount() > 0
}

private def scrubDobText(token) {
    cleaned = token.trim()
    ["approximately", "circa", "2959"].each { detectText ->
        cleaned = (cleaned =~ /(?i)/ + detectText).replaceAll("").trim()
    }
    cleaned = StringUtils.strip(cleaned, "c.").trim()
    cleaned = StringUtils.removeEnd(cleaned, ",").trim()
    cleaned = StringUtils.removeEnd(cleaned, ".").trim()
    return cleaned
}

private createAddresses(entity, addressText, placeOfBirth) {
    if (StringUtils.isNotBlank(addressText) && !"NA".equalsIgnoreCase(addressText.trim())) {
        addressText = convertChars(addressText)
        Set<String> addressSetToProcess = new HashSet<String>()
        boolean listDetected = doesStringContainList(addressText)

        if (StringUtils.contains(addressText, ">>") || listDetected) {
            String[] splitAddresses = addressText.split(">>")
            splitAddresses.each() { splitAddress ->
                if (doesStringContainList(splitAddress)) {
                    addressSetToProcess.addAll(new HashSet(breakOutList(splitAddress)))
                } else {
                    addressSetToProcess.add(splitAddress)
                }
            }
        } else {
            addressSetToProcess.add(addressText)
        }

        addressSetToProcess.remove("")
        addressSetToProcess.remove("NA")
        addressSetToProcess.each() { embeddedAddressTemp ->
            detectMultiCountry(embeddedAddressTemp).each { embeddedAddress ->

                if (StringUtils.isNotBlank(embeddedAddress)) {
                    if (embeddedAddress =~ /^\s*\(.*?\)\s*$/) {
                        embeddedAddress = embeddedAddress.replaceAll(/^\s*\((.*?)\)\s*$/, '$1')
                    }

                    def addrList = []

                    //General Address Fixing
                    embeddedAddress = fixAddr(embeddedAddress, entity)
                    addrList.add(embeddedAddress.replaceAll(/\:/, ""))

                    //Capturing Multiple addresses for address size greater than 200
                    if (embeddedAddress.toString().size() > 200) {
                        addrList = maxLenFixAddr(embeddedAddress.toString(), entity)
                    }

                    //Checking if Address contains certain keywords
                    if (embeddedAddress.toString() =~ /$addressToken/) {
                        entity.addRemark(embeddedAddress.toString())
                    }

                    if (embeddedAddress == "Kuibyshev (Samar") {
                        addrList = "Samar"
                    } else if (embeddedAddress.toString() =~ /(?is)\w+;?\s*\(\bb\b\)(?=\s*\w+)/) {
                        addrList = embeddedAddress.toString().split(/\(\bb\b\)/)

                    } else if (embeddedAddress.toString() =~ /^\s*(?:Baghdad|Mosul)\s*or\s*(?:Mosul|Baghdad)\s*$/) {
                        addrList = embeddedAddress.toString().split(/\bor\b/)

                    } else if (embeddedAddress.toString() =~ /^\s*Syria\/Iraq\s*$|Iran\//) {
                        addrList = embeddedAddress.toString().split(/\//)

                    } else if (embeddedAddress.toString() =~ /$splitAddrToken/) {
                        entity.addRemark(embeddedAddress.toString())
                        addrList = embeddedAddress.toString().split(/(?is),(?!\W*\w+$)|\band\b|\bor\b/)

                    } else if (embeddedAddress.toString() =~ /(?is)(?<=$semiColonToken)\s*;\s*(?=\w+|\-)/) {
                        addrList = embeddedAddress.toString().split(/;/)

                    } else if (embeddedAddress.toString() =~ /\b(?:i{2,3}|i?v|vi|vi{2,3}|i?x)\b\)/) {
                        addrList = embeddedAddress.toString().split(/\b(?:i{2,3}|i?v|vi|vi{2,3}|i?x)\b\)/)

                    } else if (embeddedAddress.toString() =~ /,and/) {
                        addrList = embeddedAddress.toString().split(/,and/)

                    } else if (embeddedAddress.toString() =~ /\balt\b/) {
                        addrList = embeddedAddress.toString().split(/\balt\b/)
                    } else if (embeddedAddress.toString() =~ /Hamhung, South Hangyong Province/) {
                        addrList = embeddedAddress.toString().split(/(?is)(?<=north korea);(?= Mangy)/)
                    }


                    addrList.each {
                        it = fixNum(it.toString())
                        if (it.toString()) {
                            //  //println entity.getName() + ": " + it.toString()
                            try {
                                def addrMap = addressParser.parseAddress([text: it.toString(), force_country: true])
                                def street_sanitizer = { street ->
                                    return street.replaceAll(/(?s)\s+/, " ")
                                        .replaceAll(/^[\s,-]+|\W+$|^\W+/, "")
                                        .replaceAll(/(?i)province/, "")
                                        .replaceAll(/(?i)^Located\s*in$/, "")
                                        .replaceAll(/(?i)^with\s*provincial\s*committees\s*in$/, "")
                                        .replaceAll(/(?i)\-Afghan\s*border\s*$/, "Pakistan-Afghan border")
                                        .replaceAll(/^Address:/, "")
                                        .replaceAll(/town\s*of\s*,/, ",")
                                        .replaceAll(/(?i)^\s*\(?(?:located|Believed\s*to\s*be)\s*in.*?$/, "")
                                        .replaceAll(/(?i)^\s*\(last\s*known\s*location$/, "")
                                        .replaceAll(/^\s*\((?:previous|current)?\s*(?:location|residence)\s*as\s*at\s*\w{3}\.\s*\d{4}\s*$/, "")
                                        .replaceAll(/\((?:location|alternative\s*location)\s*as\s*at\s*\w{3}\s*$/, "")
                                        .replaceAll(/(?i)\s*\bLibya\b\s*\(Believed\s*status\/location:\s*in\s*custody\s*in$/, "")
                                        .replaceAll(/(?i)^\s*\(?\s*Operates\s*in\s*$/, "")
                                        .replaceAll(/(?i)^\s*\(?Support\s*network\s*in$/, "")
                                        .replaceAll(/(?i)^\b(either|or)\b/, "")
                                        .replaceAll(/(?i)^\s*\bDistrict\b,?\s*$/, "")
                                        .replaceAll(/(?i)^\s*\(\bdetained\b\s*$/, "")
                                        .replaceAll(/(?i)\(at\s*time\s*of\s*listing\s*$/, "")
                                        .replaceAll(/(?i),\s*Union\s*of\s*the\s*$/, "")
                                        .replaceAll(/(?i)Wilaya\s*\(\)\s*of/, "")
                                        .replaceAll(/(?i)^\s*Northern\s*$/, "")
                                        .replaceAll(/(?i)\(Southern\.\s*Location\s*as.*?$/, "")
                                        .replaceAll(/(?i)^\s*\(in\s*custody\s*$/, "")
                                        .replaceAll(/(?i)^\b(?:i{2,3}|i?v|vi|vi{2,3}|i?x)\b\)/, "")
                                        .replaceAll(/(?i)^\s*Reported\s*to\s*be\s*in\s*as\s*of\s*\d{2}\s*\w{3}\s*\d{4}\s*$/, "")
                                        .replaceAll(/(?i)^Alternative\s*POB:/, "")
                                        .replaceAll(/(?i)^As\s*of\s*\w+\s*\d{4}\s*resides\s*in\s*$/, "")
                                        .replaceAll(/(?i)^\(Coastal\s*area\s*of\.\s*Location.*?$/, "")
                                        .replaceAll(/(?i)\(previous\s*as\s*at\s*\w{3}\s*$/, "")
                                        .replaceAll(/(?i)^\s*Address\s*$/, "")
                                        .replaceAll(/(?i)^\s*Believed\s*location\s*$/, "")
                                        .replaceAll(/(?i)^\s*\(possibly\s*$/, "")
                                        .replaceAll(/(?i)^\s*State\s*$/, "")
                                        .replaceAll(/(?i)^\s*\(a\)/, "")
                                        .replaceAll(/(?i)^Active\s*in\s*as\s*at\s*\w{3}\.\s*\d{4}/, "")
                                        .replaceAll(/(?i)\(location\s*$/, "")
                                        .replaceAll(/(?i)\(from\s*\w{3}\.\s*\d{4}\s*to\s*\w{3}\.\s*\d{4}/, "")
                                        .replaceAll(/(?i),\s*\(domicile\s*from\s*\d{4}\s*to/, "")
                                        .replaceAll(/(?i)\(in\s*detention\s*since\s*\w{3}\.\s*\d{4}/, "")
                                        .replaceAll(/(?i),\s*The\s*\(at\s*the\s*time\s*of\s*listing/, "")
                                        .replaceAll(/(?i)\(this\s*aka\s*only,\s*alt/, "")
                                        .replaceAll(/(?i)\(in\s*prison\s*since\s*\d{4}/, "")
                                        .replaceAll(/(?i)\(remains\s*incarcerated\s*as\s*of\s*May\s*\d{4}/, "")
                                        .replaceAll(/(?i)\(previous\s*location\s*from\s*\w{3}\.\s*\d{4}\s*to\s*May/, "")
                                        .replaceAll(/(?i)\(from\s*birth\s*until\s*\d+\s*\w{3}/, "")
                                        .replaceAll(/(?i)Kutum.*?Resides\s*in/, "")
                                        .replaceAll(/(?i)Kabkabiya\s*and.*?,/, "")
                                        .replaceAll(/(?i)Ntoke Village/, "Ntoke")
                                        .replaceAll(/(?i)and\s*has\s*resided\s*in\s*$/, "")
                                        .replaceAll(/(?s)(?:,|:)\s*,?\s*$/, "")
                                        .replaceAll(/^\s*,/, "")
                                        .replaceAll(/(?i)Governorate/, "")
                                        .replaceAll(/(?i)Region|\:/, "")
                                        .replaceAll(/(?s)\s+/, " ").trim()
                                }
                                def address1 = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                                if (address1) {
                                    address1.setBirthPlace(placeOfBirth)
                                    entity.addAddress(address1)
                                }
                            } catch (Exception e) {
                                //println e.getMessage()
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }
}

def detectMultiCountry(addressIn) {
    addressesOut = []
    if (addressIn.contains("(")) {
        addressesOut.add(addressIn)
        return addressesOut
    }
    dmcLoopGate = true
    while (dmcLoopGate) {
        addMatch = addressIn =~ /^(.*?)(?:,\s*|$)/
        if (addMatch.find()) {
            if (countries.contains(addMatch.group(1).toUpperCase().trim())) {
                addressesOut.add(addMatch.group(1).trim())
                addressIn = addressIn.replaceAll(/^(.*?)(?:,\s*|$)/, '')
                if (addressIn.trim().length() == 0) {
                    dmcLoopGate = false
                }
            } else {
                dmcLoopGate = false
            }
        } else {
            dmcLoopGate = false
        }
    }
    if (addressIn.length() > 0) {
        addressesOut.add(addressIn)
    }
    return addressesOut
}

def prepCountries() {
    countries.add('AFGHANISTAN')
    countries.add('\\ufffdLAND ISLANDS')
    countries.add('ALBANIA')
    countries.add('ALGERIA')
    countries.add('AMERICAN SAMOA')
    countries.add('ANDORRA')
    countries.add('ANGOLA')
    countries.add('ANGUILLA')
    countries.add('ANTARCTICA')
    countries.add('ANTIGUA AND BARBUDA')
    countries.add('ARGENTINA')
    countries.add('ARMENIA')
    countries.add('ARUBA')
    countries.add('AUSTRALIA')
    countries.add('AUSTRIA')
    countries.add('AZERBAIJAN')
    countries.add('BAHAMAS')
    countries.add('BAHRAIN')
    countries.add('BANGLADESH')
    countries.add('BARBADOS')
    countries.add('BELARUS')
    countries.add('BELGIUM')
    countries.add('BELIZE')
    countries.add('BENIN')
    countries.add('BERMUDA')
    countries.add('BHUTAN')
    countries.add('BOLIVIA')
    countries.add('BONAIRE, SINT EUSTATIUS AND SABA')
    countries.add('BOSNIA AND HERZEGOVINA')
    countries.add('BOSNIA')
    countries.add('BOTSWANA')
    countries.add('BOUVET ISLAND')
    countries.add('BRAZIL')
    countries.add('BRITISH INDIAN OCEAN TERRITORY')
    countries.add('BRUNEI')
    countries.add('BULGARIA')
    countries.add('BURKINA FASO')
    countries.add('BURUNDI')
    countries.add('CAMBODIA')
    countries.add('CAMEROON')
    countries.add('CANADA')
    countries.add('CAPE VERDE')
    countries.add('CAYMAN ISLANDS')
    countries.add('CENTRAL AFRICAN REPUBLIC')
    countries.add('CHAD')
    countries.add('CHILE')
    countries.add('CHINA')
    countries.add('CHRISTMAS ISLAND')
    countries.add('COCOS (KEELING) ISLANDS')
    countries.add('COLOMBIA')
    countries.add('COMOROS')
    countries.add('CONGO, REPUBLIC OF THE')
    countries.add('CONGO, THE DEMOCRATIC REPUBLIC OF THE')
    countries.add('COOK ISLANDS')
    countries.add('COSTA RICA')
    countries.add('CROATIA')
    countries.add('CUBA')
    countries.add('CURACAO')
    countries.add('CYPRUS')
    countries.add('CZECH REPUBLIC')
    countries.add('C\\ufffdTE D\'IVOIRE')
    countries.add('DENMARK')
    countries.add('DJIBOUTI')
    countries.add('DOMINICA')
    countries.add('DOMINICAN REPUBLIC')
    countries.add('ECUADOR')
    countries.add('EGYPT')
    countries.add('EL SALVADOR')
    countries.add('EQUATORIAL GUINEA')
    countries.add('ERITREA')
    countries.add('ESTONIA')
    countries.add('ETHIOPIA')
    countries.add('FALKLAND ISLANDS (MALVINAS)')
    countries.add('FAROE ISLANDS')
    countries.add('FIJI')
    countries.add('FINLAND')
    countries.add('FRANCE')
    countries.add('FRENCH GUIANA')
    countries.add('FRENCH POLYNESIA')
    countries.add('FRENCH SOUTHERN TERRITORIES')
    countries.add('GABON')
    countries.add('GAMBIA')
    countries.add('GEORGIA')
    countries.add('GERMANY')
    countries.add('GHANA')
    countries.add('GIBRALTAR')
    countries.add('GREECE')
    countries.add('GREENLAND')
    countries.add('GRENADA')
    countries.add('GUADELOUPE')
    countries.add('GUAM')
    countries.add('GUATEMALA')
    countries.add('GUERNSEY')
    countries.add('GUINEA')
    countries.add('GUINEA-BISSAU')
    countries.add('GUYANA')
    countries.add('HAITI')
    countries.add('HEARD ISLAND AND MCDONALD ISLANDS')
    countries.add('HOLY SEE (VATICAN CITY STATE)')
    countries.add('HONDURAS')
    countries.add('HONG KONG')
    countries.add('HUNGARY')
    countries.add('ICELAND')
    countries.add('INDIA')
    countries.add('INDONESIA')
    countries.add('IRAN, ISLAMIC REPUBLIC OF')
    countries.add('IRAQ')
    countries.add('IRELAND')
    countries.add('ISLE OF MAN')
    countries.add('ISRAEL')
    countries.add('ITALY')
    countries.add('JAMAICA')
    countries.add('JAPAN')
    countries.add('JERSEY')
    countries.add('JORDAN')
    countries.add('KAZAKHSTAN')
    countries.add('KENYA')
    countries.add('KIRIBATI')
    countries.add('KOREA, DEMOCRATIC PEOPLE\'S REPUBLIC OF')
    countries.add('KOREA, REPUBLIC OF')
    countries.add('KUWAIT')
    countries.add('KYRGYZSTAN')
    countries.add('KOSOVO')
    countries.add('LAO PEOPLE\'S DEMOCRATIC REPUBLIC')
    countries.add('LATVIA')
    countries.add('LEBANON')
    countries.add('LESOTHO')
    countries.add('LIBERIA')
    countries.add('LIBYA')
    countries.add('LIECHTENSTEIN')
    countries.add('LITHUANIA')
    countries.add('LUXEMBOURG')
    countries.add('MACAO')
    countries.add('MACEDONIA, THE FORMER YUGOSLAV REPUBLIC OF')
    countries.add('MADAGASCAR')
    countries.add('MALAWI')
    countries.add('MALAYSIA')
    countries.add('MALDIVES')
    countries.add('MALI')
    countries.add('MALTA')
    countries.add('MARSHALL ISLANDS')
    countries.add('MARTINIQUE')
    countries.add('MAURITANIA')
    countries.add('MAURITIUS')
    countries.add('MAYOTTE')
    countries.add('MEXICO')
    countries.add('MICRONESIA, FEDERATED STATES OF')
    countries.add('MOLDOVA, REPUBLIC OF')
    countries.add('MONACO')
    countries.add('MONGOLIA')
    countries.add('MONTENEGRO')
    countries.add('MONTSERRAT')
    countries.add('MOROCCO')
    countries.add('MOZAMBIQUE')
    countries.add('MYANMAR')
    countries.add('NAMIBIA')
    countries.add('NAURU')
    countries.add('NEPAL')
    countries.add('NETHERLANDS')
    countries.add('NEW CALEDONIA')
    countries.add('NEW ZEALAND')
    countries.add('NICARAGUA')
    countries.add('NIGER')
    countries.add('NIGERIA')
    countries.add('NIUE')
    countries.add('NORFOLK ISLAND')
    countries.add('NORTHERN MARIANA ISLANDS')
    countries.add('NORWAY')
    countries.add('OMAN')
    countries.add('PAKISTAN')
    countries.add('PALAU')
    countries.add('PALESTINIAN TERRITORY, OCCUPIED')
    countries.add('PANAMA')
    countries.add('PAPUA NEW GUINEA')
    countries.add('PARAGUAY')
    countries.add('PERU')
    countries.add('PHILIPPINES')
    countries.add('PITCAIRN')
    countries.add('POLAND')
    countries.add('PORTUGAL')
    countries.add('PUERTO RICO')
    countries.add('QATAR')
    countries.add('ROMANIA')
    countries.add('RUSSIAN FEDERATION')
    countries.add('RWANDA')
    countries.add('R\\ufffdUNION')
    countries.add('SAINT BARTH\\ufffdLEMY')
    countries.add('SAINT HELENA')
    countries.add('SAINT KITTS AND NEVIS')
    countries.add('SAINT LUCIA')
    countries.add('SAINT MARTIN (French part)')
    countries.add('SAINT PIERRE AND MIQUELON')
    countries.add('SAINT VINCENT AND THE GRENADINES')
    countries.add('SAMOA')
    countries.add('SAN MARINO')
    countries.add('SAO TOME AND PRINCIPE')
    countries.add('SAUDI ARABIA')
    countries.add('SENEGAL')
    countries.add('SERBIA')
    countries.add('SEYCHELLES')
    countries.add('SIERRA LEONE')
    countries.add('SINGAPORE')
    countries.add('SINT MAARTEN')
    countries.add('SLOVAKIA')
    countries.add('SLOVENIA')
    countries.add('SOLOMON ISLANDS')
    countries.add('SOMALIA')
    countries.add('SOUTH AFRICA')
    countries.add('SOUTH GEORGIA AND THE SOUTH SANDWICH ISLANDS')
    countries.add('SOUTH SUDAN')
    countries.add('SPAIN')
    countries.add('SRI LANKA')
    countries.add('SUDAN')
    countries.add('SURINAME')
    countries.add('SVALBARD AND JAN MAYEN')
    countries.add('SWAZILAND')
    countries.add('SWEDEN')
    countries.add('SWITZERLAND')
    countries.add('SYRIAN ARAB REPUBLIC')
    countries.add('TAIWAN, PROVINCE OF CHINA')
    countries.add('TAJIKISTAN')
    countries.add('TANZANIA, UNITED REPUBLIC OF')
    countries.add('THAILAND')
    countries.add('TIMOR-LESTE')
    countries.add('TOGO')
    countries.add('TOKELAU')
    countries.add('TONGA')
    countries.add('TRINIDAD AND TOBAGO')
    countries.add('TUNISIA')
    countries.add('TURKEY')
    countries.add('TURKMENISTAN')
    countries.add('TURKS AND CAICOS ISLANDS')
    countries.add('TUVALU')
    countries.add('UGANDA')
    countries.add('UKRAINE')
    countries.add('UNITED ARAB EMIRATES')
    countries.add('UNITED KINGDOM')
    countries.add('UNITED STATES')
    countries.add('UNITED STATES MINOR OUTLYING ISLANDS')
    countries.add('URUGUAY')
    countries.add('UZBEKISTAN')
    countries.add('VANUATU')
    countries.add('VENEZUELA')
    countries.add('VIETNAM')
    countries.add('VIRGIN ISLANDS, BRITISH')
    countries.add('VIRGIN ISLANDS, U.S.')
    countries.add('WALLIS AND FUTUNA')
    countries.add('WESTERN SAHARA')
    countries.add('YEMEN')
    countries.add('ZAMBIA')
    countries.add('ZIMBABWE')
}


private def createRemark(entity, listingInfo, additionalInfo) {

    if (StringUtils.isNotBlank(listingInfo)) {
        entity.addRemark(convertChars(listingInfo).replaceAll(/^\W+/, "").trim())
    }

    if (StringUtils.isNotBlank(additionalInfo)) {
        entity.addRemark(convertChars(additionalInfo).replaceAll(/^\W+/, "").trim())
    }
}

private createEvents(entity, committee) throws Exception {
    if (StringUtils.isNotBlank(committee)) {
        ScrapeEvent evt = new ScrapeEvent()
        evt.setDescription(dscr + "Committee: $committee")
        if (!entity.getEvents().contains(evt)) {
            entity.addEvent(evt)
        }
    }
}

private createPositions(entity, originalText) {
    if (StringUtils.isNotBlank(originalText)) {
        text = originalText.trim()
        int titleIndex = text.indexOf("Title:")
        int designationIndex = text.indexOf("Designation:")
        if (text.contains("Title:") && text.contains("Designation:")) {
            // Title and Designation.
            createPosition(entity, text.substring(titleIndex + "Title:".length(), designationIndex).trim())
            createPosition(entity, text.substring(designationIndex + "Designation:".length()).trim())
        } else if (text.contains("Title:") && !text.contains("Designation:")) {
            // Title only.
            createPosition(entity, text.substring(titleIndex + "Title:".length()).trim())
        } else if (text.contains("Designation:") && !text.contains("Title:")) {
            // Designation only.
            createPosition(entity, text.substring(designationIndex + "Designation:".length()).trim())
        }
    }
}

private createPosition(entity, positionText) {
    delimitByAlpha(positionText).each { text ->
        position = removeLeadingAndTrailingPunctuation(text.trim())
        position = StringUtils.strip(position.trim(), ",").trim()
        if (position != null && position.length() > 200) {
            position = WordUtils.abbreviate(position, 200, 210, "...")
        }
        if (StringUtils.isNotBlank(position) && !("Mr".equals(position) || "Mr.".equals(position))) {
            entity.addPosition(position)
        }
    }
}

private createPlacesOfBirth(entity, additionalInfo) {
    if (StringUtils.isNotBlank(additionalInfo)) {
        findAdditionInfo(additionalInfo, "PLACE OF BIRTH").each() { birthPlace ->
            createAddresses(entity, birthPlace, true)
        }

        findAdditionInfo(additionalInfo, "POB").each() { birthPlace ->
            createAddresses(entity, birthPlace, true)
        }
    }
}

private createNationalities(entity, additionalInfo) {
    if (StringUtils.isNotBlank(additionalInfo)) {
        findAdditionInfo(additionalInfo, "NATIONALITY").each() { nationality ->
            if (nationality != "" && nationality != null && nationality.size() < 201) {
                entity.addNationality(nationality)
            }
        }
    }
}

private createCitizenships(entity, citizenshipColumn) {
    if (StringUtils.isNotBlank(citizenshipColumn)) {
        //println("ENTITY: $entity.name $citizenshipColumn\n")
        if (citizenshipColumn == "Kuwaiti citizenship withdrawn in 2002" ||
            citizenshipColumn == "Indonesian (as at Dec. 2003)" ||
            citizenshipColumn == "Possibly Ethiopian") {
            createRemark(entity, "Citizenship: " + citizenshipColumn, null)
        } else if (!"NA".equalsIgnoreCase(citizenshipColumn.trim())) {
            delimitByAlpha(citizenshipColumn).each { citizenship ->
                if (StringUtils.isNotBlank(citizenship)) {
                    // If we find a passport - just add it to the entity
                    if (citizenship.toUpperCase().contains("PASSPORT")) {
                        [" NO PASSPORT", ", PASSPORT", " PASSPORT"].each { ppKey ->
                            if (citizenship.toUpperCase().contains(ppKey)) {
                                citizenship = citizenship.substring(0, citizenship.toUpperCase().indexOf(ppKey)).trim()
                            }
                        }
                    }
                    citizenship = removeLeadingAndTrailingPunctuation(citizenship)
                    citizenship = getAlternativeCitizenship(citizenship).replaceAll(/(?s)\s+/, " ").trim();
                    if (StringUtils.isNotBlank(citizenship)) {
                        entity.addCitizenship(citizenship.length() > 50 ? citizenship.subSequence(0, 49) : citizenship)
                    }
                }
            }
        }
    }
}

private createPassports(entity, citizenshipColumn, additionalInfoColumn) {
    if (StringUtils.isNotBlank(citizenshipColumn) && !"NA".equalsIgnoreCase(citizenshipColumn.trim())) {
        delimitByAlpha(citizenshipColumn).each { citizenship ->
            if (StringUtils.isNotBlank(citizenship)) {
                // If we find a passport - just add it to the entity
                if (citizenship.toUpperCase().contains("PASSPORT")) {
                    passportData = citizenship.trim()
                    if (passportData.toUpperCase().contains(", PASSPORT")) {
                        passportData = passportData.substring(passportData.toUpperCase().indexOf(", PASSPORT")).trim()
                        passportData = removeLeadingAndTrailingPunctuation(passportData).trim()
                    }
                    findAdditionInfo(passportData, "Passport").each { passport ->
                        passport = fixSpaces(passport)
                        def identifcation = new ScrapeIdentification()
                        identifcation.type = "Passport"
                        identifcation.value = passport
                        entity.addIdentification(identifcation)
                    }
                }
            }
        }
    }

    if (StringUtils.isNotBlank(additionalInfoColumn)) {
        findAdditionInfo(additionalInfoColumn, "PASSPORT NUMBER").each() { passport ->
            passport = fixSpaces(passport)
            def identifcation = new ScrapeIdentification()
            identifcation.type = "Passport"
            identifcation.value = passport
            entity.addIdentification(identifcation)
        }

        findAdditionInfo(additionalInfoColumn, "PASSPORT NO.").each() { passport ->
            passport = fixSpaces(passport)
            def identifcation = new ScrapeIdentification()
            identifcation.type = "Passport"
            identifcation.value = passport
            entity.addIdentification(identifcation)
        }
    }
}


private findAdditionInfo(columnText, findThis) {
    findThis = findThis.toUpperCase()
    def returnData = []

    if (StringUtils.isNotBlank(columnText)) {
        StringUtils.split(columnText, "\n").each { line ->
            if (line.toUpperCase().contains(findThis)) {
                StringUtils.split(line, ">>").each { bigToken ->
                    StringUtils.split(bigToken, ";").each { tok ->
                        token = cleanCommonAbbreviations(tok)
                        StringUtils.split(token, ".").each { innerTok ->
                            potential = innerTok.trim()
                            if (potential.toUpperCase().contains(findThis)) {
                                potential = (potential =~ /(?i)national ide.*/).replaceAll("")
                                if (potential.toUpperCase().startsWith(findThis)) {
                                    potential = potential.substring(findThis.length())
                                }
                                potential = removeLeadingAndTrailingPunctuation(potential)
                                if (potential.toUpperCase().contains(findThis) && doesStringContainList(potential)) {
                                    returnData.addAll(breakOutList(potential))
                                } else {
                                    returnData.add(potential.trim())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return returnData
}

private List<String> delimitByAlpha(text) {
    List<String> strings = new ArrayList<String>()
    if (doesStringContainList(text)) {
        // Assume this is a list.
        text.split("a\\)\\s|\\s[a-zA-Z]\\)\\s").each() { token ->
            if (StringUtils.isNotBlank(token)) {
                strings.add(token)
            }
        }
    } else {
        // Assume not a list.
        if (StringUtils.isNotBlank(text)) {
            strings.add(text.trim())
        }
    }
    return strings
}

private doesStringContainList(text) {
    return (text.toUpperCase().startsWith("A) ") || text.toUpperCase().contains(" B) "))
}

private removeLeadingAndTrailingPunctuation(text) {
    if (StringUtils.isNotBlank(text)) {
        punctuations = [",", ".", ";", ":", "-"]
        punctuations.each { txt ->
            text = StringUtils.removeEnd(text.trim(), txt).trim()
            text = StringUtils.removeStart(text.trim(), txt).trim()
        }
    }
    return text
}

private List breakOutList(text) {
    def returnData = []
    delimitByAlpha(text).each { listItem ->
        if (StringUtils.isNotBlank(listItem)) {
            item = removeLeadingAndTrailingPunctuation(listItem)
            returnData.add(item)
        }
    }
    return returnData
}

private cleanCommonAbbreviations(token) {
    token = StringUtils.replace(token, " Jan. ", " Jan ")
    token = StringUtils.replace(token, " Feb. ", " Feb ")
    token = StringUtils.replace(token, " Mar. ", " Mar ")
    token = StringUtils.replace(token, " Apr. ", " Apr ")
    token = StringUtils.replace(token, " Jun. ", " Jun ")
    token = StringUtils.replace(token, " Jul. ", " Jul ")
    token = StringUtils.replace(token, " Aug. ", " Aug ")
    token = StringUtils.replace(token, " Sep. ", " Sep ")
    token = StringUtils.replace(token, " Oct. ", " Oct ")
    token = StringUtils.replace(token, " Nov. ", " Nov ")
    token = StringUtils.replace(token, " Dec. ", " Dec ")
    token = StringUtils.replace(token, " I.D. ", " ID ")
    token = StringUtils.replace(token, " U.S.A. ", " USA ")
    return token
}

private getMonth(month) {
    if (StringUtils.isBlank(month))
        return null
    if (month.toUpperCase().startsWith("JAN"))
        return "1"
    if (month.toUpperCase().startsWith("FEB"))
        return "2"
    if (month.toUpperCase().startsWith("MAR"))
        return "3"
    if (month.toUpperCase().startsWith("APR"))
        return "4"
    if (month.toUpperCase().startsWith("MAY"))
        return "5"
    if (month.toUpperCase().startsWith("JUN"))
        return "6"
    if (month.toUpperCase().startsWith("JUL"))
        return "7"
    if (month.toUpperCase().startsWith("AUG"))
        return "8"
    if (month.toUpperCase().startsWith("SEP"))
        return "9"
    if (month.toUpperCase().startsWith("OCT"))
        return "10"
    if (month.toUpperCase().startsWith("NOV"))
        return "11"
    if (month.toUpperCase().startsWith("DEC"))
        return "12"
    return "-"
}

private def parseValidateAndCorrectScrapeDob(entity, dob) {
    dateParts = dob.split("/")
    ScrapeDob scrapeDob = null
    if (dateParts != null && dateParts.length == 3) {
        scrapeDob = new ScrapeDob()
        scrapeDob.setMonth(dateParts[0].isInteger() ? StringUtils.removeStart(dateParts[0].toString(), "0") : "")
        scrapeDob.setDay(dateParts[1].isInteger() ? StringUtils.removeStart(dateParts[1].toString(), "0") : "")
        scrapeDob.setYear(dateParts[2].isInteger() ? dateParts[2].toString() : "")
    }
    if (scrapeDob != null && scrapeDob.getYear().length() > 0) {

        if (StringUtils.isNumeric(scrapeDob.getYear()) && Integer.parseInt(scrapeDob.getYear()) < 1900) {
            int year = Integer.parseInt(scrapeDob.getYear())
            if (year > 31 && year < 100) {
                scrapeDob.setYear("19" + year)
            }
        }
        if (scrapeDob.getYear().isInteger()) {
            if (1900 > Integer.parseInt(scrapeDob.getYear())) {
                scrapeDob = null
            }
        }
    }


    return scrapeDob
}

private String latinize(String toBeLatinized) {
    StringBuilder retVal = new StringBuilder()
    for (char c : toBeLatinized.toCharArray()) {
        retVal.append(cmu.removeAccents(c))
    }
    return retVal.toString()
}

def determineScript(input) {
    script = ''
    Character.UnicodeScript.each {
        if (it.toString() == 'COMMON') {
            return
        }
        if (containsScriptLetters(input, it)) {
            if (!binding.variables.containsKey("scriptMap")) {
                prepScriptMap()
            }
            theScript = scriptMap[it.toString()]
            if (theScript) {
                script += (script.length() > 0 ? ", " : "") + theScript
            }
        }
    }
    return script
}

def containsScriptLetters(text, script) {
    retVal = false
    for (int i = 0; i < text.length();) {
        int codepoint = text.codePointAt(i)
        i += Character.charCount(codepoint)
        if (Character.UnicodeScript.of(codepoint) == script) {
            retVal = true
        }
    }
    return retVal
}

def prepScriptMap() {
    scriptMap = [:]
    scriptMap['ADLAM'] = 'Adlm'
    scriptMap['CAUCASIAN_ALBANIAN'] = 'Aghb'
    scriptMap['AHOM'] = 'Ahom'
    scriptMap['ARABIC'] = 'Arab'
    scriptMap['IMPERIAL_ARAMAIC'] = 'Armi'
    scriptMap['ARMENIAN'] = 'Armn'
    scriptMap['AVESTAN'] = 'Avst'
    scriptMap['BALINESE'] = 'Bali'
    scriptMap['BAMUM'] = 'Bamu'
    scriptMap['BASSA_VAH'] = 'Bass'
    scriptMap['BATAK'] = 'Batk'
    scriptMap['BENGALI'] = 'Beng'
    scriptMap['BHAIKSUKI'] = 'Bhks'
    scriptMap['BOPOMOFO'] = 'Bopo'
    scriptMap['BRAHMI'] = 'Brah'
    scriptMap['BRAILLE'] = 'Brai'
    scriptMap['BUGINESE'] = 'Bugi'
    scriptMap['BUHID'] = 'Buhd'
    scriptMap['CHAKMA'] = 'Cakm'
    scriptMap['CANADIAN_ABORIGINAL'] = 'Cans'
    scriptMap['CARIAN'] = 'Cari'
    scriptMap['CHAM'] = 'Cham'
    scriptMap['CHEROKEE'] = 'Cher'
    scriptMap['COPTIC'] = 'Copt'
    scriptMap['CYPRIOT'] = 'Cprt'
    scriptMap['CYRILLIC'] = 'Cyrl'
    scriptMap['DEVANAGARI'] = 'Deva'
    scriptMap['DESERET'] = 'Dsrt'
    scriptMap['DUPLOYAN'] = 'Dupl'
    scriptMap['EGYPTIAN_HIEROGLYPHS'] = 'Egyp'
    scriptMap['ELBASAN'] = 'Elba'
    scriptMap['ETHIOPIC'] = 'Ethi'
    scriptMap['GEORGIAN'] = 'Geor'
    scriptMap['GLAGOLITIC'] = 'Glag'
    scriptMap['GOTHIC'] = 'Goth'
    scriptMap['GRANTHA'] = 'Gran'
    scriptMap['GREEK'] = 'Grek'
    scriptMap['GUJARATI'] = 'Gujr'
    scriptMap['GURMUKHI'] = 'Guru'
    scriptMap['HANGUL'] = 'Hang'
    scriptMap['HAN'] = 'Hani'
    scriptMap['HANUNOO'] = 'Hano'
    scriptMap['HATRAN'] = 'Hatr'
    scriptMap['HEBREW'] = 'Hebr'
    scriptMap['HIRAGANA'] = 'Hira'
    scriptMap['ANATOLIAN_HIEROGLYPHS'] = 'Hluw'
    scriptMap['PAHAWH_HMONG'] = 'Hmng'
    scriptMap['KATAKANA_OR_HIRAGANA'] = 'Hrkt'
    scriptMap['OLD_HUNGARIAN'] = 'Hung'
    scriptMap['OLD_ITALIC'] = 'Ital'
    scriptMap['JAVANESE'] = 'Java'
    scriptMap['KAYAH_LI'] = 'Kali'
    scriptMap['KATAKANA'] = 'Kana'
    scriptMap['KHAROSHTHI'] = 'Khar'
    scriptMap['KHMER'] = 'Khmr'
    scriptMap['KHOJKI'] = 'Khoj'
    scriptMap['KANNADA'] = 'Knda'
    scriptMap['KAITHI'] = 'Kthi'
    scriptMap['TAI_THAM'] = 'Lana'
    scriptMap['LAO'] = 'Laoo'
    scriptMap['LATIN'] = 'Latn'
    scriptMap['LEPCHA'] = 'Lepc'
    scriptMap['LIMBU'] = 'Limb'
    scriptMap['LINEAR_A'] = 'Lina'
    scriptMap['LINEAR_B'] = 'Linb'
    scriptMap['LISU'] = 'Lisu'
    scriptMap['LYCIAN'] = 'Lyci'
    scriptMap['LYDIAN'] = 'Lydi'
    scriptMap['MAHAJANI'] = 'Mahj'
    scriptMap['MANDAIC'] = 'Mand'
    scriptMap['MANICHAEAN'] = 'Mani'
    scriptMap['MARCHEN'] = 'Marc'
    scriptMap['MENDE_KIKAKUI'] = 'Mend'
    scriptMap['MEROITIC_CURSIVE'] = 'Merc'
    scriptMap['MEROITIC_HIEROGLYPHS'] = 'Mero'
    scriptMap['MALAYALAM'] = 'Mlym'
    scriptMap['MODI'] = 'Modi'
    scriptMap['MONGOLIAN'] = 'Mong'
    scriptMap['MRO'] = 'Mroo'
    scriptMap['MEETEI_MAYEK'] = 'Mtei'
    scriptMap['MULTANI'] = 'Mult'
    scriptMap['MYANMAR'] = 'Mymr'
    scriptMap['OLD_NORTH_ARABIAN'] = 'Narb'
    scriptMap['NABATAEAN'] = 'Nbat'
    scriptMap['NEWA'] = 'Newa'
    scriptMap['NKO'] = 'Nkoo'
    scriptMap['OGHAM'] = 'Ogam'
    scriptMap['OL_CHIKI'] = 'Olck'
    scriptMap['OLD_TURKIC'] = 'Orkh'
    scriptMap['ORIYA'] = 'Orya'
    scriptMap['OSAGE'] = 'Osge'
    scriptMap['OSMANYA'] = 'Osma'
    scriptMap['PALMYRENE'] = 'Palm'
    scriptMap['PAU_CIN_HAU'] = 'Pauc'
    scriptMap['OLD_PERMIC'] = 'Perm'
    scriptMap['PHAGS_PA'] = 'Phag'
    scriptMap['INSCRIPTIONAL_PAHLAVI'] = 'Phli'
    scriptMap['PSALTER_PAHLAVI'] = 'Phlp'
    scriptMap['PHOENICIAN'] = 'Phnx'
    scriptMap['MIAO'] = 'Plrd'
    scriptMap['INSCRIPTIONAL_PARTHIAN'] = 'Prti'
    scriptMap['REJANG'] = 'Rjng'
    scriptMap['RUNIC'] = 'Runr'
    scriptMap['SAMARITAN'] = 'Samr'
    scriptMap['OLD_SOUTH_ARABIAN'] = 'Sarb'
    scriptMap['SAURASHTRA'] = 'Saur'
    scriptMap['SIGNWRITING'] = 'Sgnw'
    scriptMap['SHAVIAN'] = 'Shaw'
    scriptMap['SHARADA'] = 'Shrd'
    scriptMap['SIDDHAM'] = 'Sidd'
    scriptMap['KHUDAWADI'] = 'Sind'
    scriptMap['SINHALA'] = 'Sinh'
    scriptMap['SORA_SOMPENG'] = 'Sora'
    scriptMap['SUNDANESE'] = 'Sund'
    scriptMap['SYLOTI_NAGRI'] = 'Sylo'
    scriptMap['SYRIAC'] = 'Syrc'
    scriptMap['TAGBANWA'] = 'Tagb'
    scriptMap['TAKRI'] = 'Takr'
    scriptMap['TAI_LE'] = 'Tale'
    scriptMap['NEW_TAI_LUE'] = 'Talu'
    scriptMap['TAMIL'] = 'Taml'
    scriptMap['TANGUT'] = 'Tang'
    scriptMap['TAI_VIET'] = 'Tavt'
    scriptMap['TELUGU'] = 'Telu'
    scriptMap['TIFINAGH'] = 'Tfng'
    scriptMap['TAGALOG'] = 'Tglg'
    scriptMap['THAANA'] = 'Thaa'
    scriptMap['THAI'] = 'Thai'
    scriptMap['TIBETAN'] = 'Tibt'
    scriptMap['TIRHUTA'] = 'Tirh'
    scriptMap['UGARITIC'] = 'Ugar'
    scriptMap['VAI'] = 'Vaii'
    scriptMap['WARANG_CITI'] = 'Wara'
    scriptMap['OLD_PERSIAN'] = 'Xpeo'
    scriptMap['CUNEIFORM'] = 'Xsux'
    scriptMap['YI'] = 'Yiii'
}

def convertChars(input) {
    return input
        .replaceAll(/\u010d/, 'c')
        .replaceAll(/\u0107/, 'c')
        .replaceAll(/\u037e/, ';')
        .replaceAll(/\u010c/, 'C')
        .replaceAll(/\u0628/, 'B')
        .replaceAll(/\u0627/, 'a')
        .replaceAll(/\u06a9/, 'k')
        .replaceAll(/\u0648/, 'v')
        .replaceAll(/\u0631\u064a\u0645\u0629/, 'Raymah')
        .replaceAll(/\u0631/, 'r')
        .replaceAll(/\u0632/, 'z')
        .replaceAll(/\u06cc/, 'y')
        .replaceAll(/&#269;/, 'c')
        .replaceAll(/&#263;/, 'c')
        .replaceAll(/&#894;/, ';')
        .replaceAll(/&#268;/, 'C')
        .replaceAll(/&#1576;/, 'B')
        .replaceAll(/&#1575;/, 'a')
        .replaceAll(/&#1705;/, 'k')
        .replaceAll(/&#1608;/, 'v')
        .replaceAll(/&#1585;&#1610;&#1605;&#1577;/, 'Raymah')
        .replaceAll(/&#1585;/, 'r')
        .replaceAll(/&#1586;/, 'z')
        .replaceAll(/&#1740;/, 'y')
        .replaceAll(/\u0301/, "'")
        .replaceAll(/(?s)\s+/, " ")
}


def prepCitizenshipMap() {
    citizenshipMap['Ugandan'] = 'UGANDA'
    citizenshipMap['Afghan'] = 'AFGHANISTAN'
    citizenshipMap['Afghani'] = 'AFGHANISTAN'
    citizenshipMap['Algerian'] = 'ALGERIA'
    citizenshipMap['British'] = 'UNITED KINGDOM'
    citizenshipMap['Chinese'] = 'CHINA'
    citizenshipMap['Congolese'] = 'Democratic Republic of the Congo'
    citizenshipMap['DPRK'] = "KOREA, DEMOCRATIC PEOPLE'S REPUBLIC OF"
    citizenshipMap['Egyptian'] = 'EGYPT'
    citizenshipMap['Filipino'] = 'PHILIPPINES'
    citizenshipMap['French'] = 'FRANCE'
    citizenshipMap['Georgian'] = 'GEORGIA'
    citizenshipMap['German'] = 'GERMANY'
    citizenshipMap['Indonesian'] = 'INDONESIA'
    citizenshipMap['Iranian'] = 'IRAN, ISLAMIC REPUBLIC OF'
    citizenshipMap['Iran; nationality Iran'] = 'IRAN, ISLAMIC REPUBLIC OF'
    citizenshipMap['Iraqi'] = 'IRAQ'
    citizenshipMap['Jordanian'] = 'JORDAN'
    citizenshipMap['Kuwaiti'] = 'KUWAIT'
    citizenshipMap['Lebanese'] = 'LEBANON'
    citizenshipMap['Libyan'] = 'LIBYA'
    citizenshipMap['Malian'] = 'MALI'
    citizenshipMap['Mauritanian'] = 'MAURITIUS'
    citizenshipMap['Mauritian'] = 'MAURITIUS'
    citizenshipMap['Moroccan'] = 'MOROCCO'
    citizenshipMap['Nigerian'] = 'NIGERIA'
    citizenshipMap['North Korean'] = 'KOREA, DEMOCRATIC PEOPLE\'S REPUBLIC OF'
    citizenshipMap['Norwegian'] = 'NORWAY'
    citizenshipMap['Pakistani'] = 'PAKISTAN'
    citizenshipMap['Palestinian'] = 'PALESTINIAN TERRITORY, OCCUPIED'
    citizenshipMap['State of Palestine'] = 'PALESTINIAN TERRITORY, OCCUPIED'
    citizenshipMap['Philippines'] = 'PHILIPPINES'
    citizenshipMap['Qatari'] = 'QATAR'
    citizenshipMap['Russian'] = 'RUSSIAN FEDERATION'
    citizenshipMap['Rwandan'] = 'RWANDA'
    citizenshipMap['Saudi Arabian'] = 'SAUDI ARABIA'
    citizenshipMap['Senegalese'] = 'SENEGAL'
    citizenshipMap['Serbian'] = 'SERBIA'
    citizenshipMap['Somali'] = 'SOMALIA'
    citizenshipMap['Syrian'] = 'SYRIAN ARAB REPUBLIC'
    citizenshipMap['Tunisian'] = 'TUNISIA'
    citizenshipMap['Turkish'] = 'TURKEY'
    citizenshipMap['Ugandan'] = 'UGANDA'
    citizenshipMap['United Kingdom of Great Britain'] = 'UNITED KINGDOM'
    citizenshipMap['United Kingdom of Great Britain and Northern Ireland'] = 'UNITED KINGDOM'
    citizenshipMap['United Republic of Tanzania'] = 'TANZANIA, UNITED REPUBLIC OF'
    citizenshipMap['United States of America'] = 'UNITED STATES'
    citizenshipMap['Yemeni'] = 'YEMEN'
    citizenshipMap['na'] = ''
    citizenshipMap['of Qatar'] = 'Qatar'
    citizenshipMap['United States and Iranian'] = "a) United States b) Iranian "
    citizenshipMap['German, Moroccan'] = "a) German b) Moroccan "
    citizenshipMap['Malian / Mauritanian'] = "a) Malian b) Mauritanian "
    citizenshipMap["Bahrain (citizenship revoked in Jan. 2015)"] = "BAHRAIN"
    citizenshipMap["Indonesian (as at Dec. 2003)"] = "INDONESIA"
    citizenshipMap["Plot 55A, Upper Kololo Terrace Kampala, Uganda"] = "UGANDA"
    citizenshipMap["Saudi Arabian (this aka only, alt Citizenship Yemeni)"] = "a) SAUDI ARABIA b) YEMEN "
    citizenshipMap["Uzbek; Afghan"] = "a) UZBEKISTAN b) AFGHANISTAN "
    citizenshipMap["Indonesian (as at Dec. 2003)"] = "INDONESIA"
}

String getAlternativeCitizenship(String origCitizenship) {
    def standardCitizenship = citizenshipMap.get(origCitizenship)
    if (standardCitizenship) {
        return standardCitizenship
    } else {
        return origCitizenship
    }
}

/*def invokeBinary(url, type = null, paramsMap = null, cache = true, clean = true, miscData = [:]) {
    //Default type is GET
    paramsMap = getParamMap()
    Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
    dataMap.putAll(miscData)
    return context.invokeBinary(dataMap)
}*/

def invoke(url, cache = false, tidy = false, headersMap = [:], miscData = [:]) {
    Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
    dataMap.putAll(miscData)
    return context.invoke(dataMap)
}

//delete the file
Files.delete(Paths.get(xlsFilePath))