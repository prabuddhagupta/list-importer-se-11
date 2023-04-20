package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

import java.nio.file.Files
import java.nio.file.Paths

context.setup([connectionTimeout: 50000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Disciplinary scripts = new Disciplinary(context)
//debug
//scripts.pdfToTextConverter("https://plb.iowa.gov/sites/default/files/20-028%20Abboud%2C%20Mark%20F..pdf")
scripts.initParsing()

class Disciplinary {
    final addressParser
    final ScrapianContext context
    final def ocrReader
    static def rootUrl = "https://plb.iowa.gov/board/real-estate-sales-brokers"
//"https://plb.iowa.gov/real-estate-sales-brokers/disciplinary-index"
    def dataUrl = "https://plb.iowa.gov/disciplinary-index/real-estate-sales-&-brokers"
    final def moduleFactory = ModuleLoader.getFactory("7dd0501f220ad1d1465967a4e02f6ff17d7d9ff9")
    final entityType
    final CITIES = ["Macksburg", "Agoura Hills", "Ft. Dodge", "Dubuque", "Ottumwa", "Storm Lake", "Ames", "Ankeny", "Peterson", "Council Bluffs", "Iowa City",
                    "Washington", "Cedar Rapids", "Mason City", "Muscatine", "Reinbeck", "Clive", "West Des Moines", "Des Moines",
                    "Keota", "Coralville", "Slater", "Gladbrook", "Cresco", "Lincoln", "Spirit Lake", "Pella", "Nashua", "Marion",
                    "Whittemore", "Davenport", "Albia", "Waterloo", "Delaware", "Montezuma", "Keokuk", "Newton", "Manson",
                    "Le Mars", "Hampton", "Fort Dodge", "Oskaloosa", "Clear Lake", "Gilbert", "Sioux City", "Adel", "Altoona", "Tipton",
                    "Logan", "Fairfield", "Bettendorf", "North Liberty", "Greenfield", "Maquoketa", "New Hampton", "Burlington",
                    "West Burlington", "Avoca", "Algona", "Union", "West Union", "Beaver", "Iowa Falls", "Leon", "Pleasanton", "Grinnell",
                    "Spencer", "Lenox", "Monroe", "Knoxville", "Robins", "Forest City", "Charles City", "Independence", "St. Charles", "Joice",
                    "Terril", "Elk Horn", "Birmingham", "Cherokee", "Humboldt", "Sumner", "Harlan", "Sioux Rapids", "Gray", "Hiawatha", "Radcliffe",
                    "Decorah", "Dyersville", "Chicago", "Colo", "Missouri Valley", "Moravia", "Grimes", "Dana", "Granger", "Center Point", "Pleasant Hill",
                    "Boone", "Vinton", "Toledo", "Sigourney", "Indianola", "Salem", "Springville", "Centerville", "Waukee", "Benton", "Mount Vernon",
                    "Preston", "Clarion", "Marshalltown", "Blue Grass", "Osceola", "Okoboji", "Orient", "Carroll", "Onawa", "Latimer", "Linden",
                    "Norwalk", "Lansing", "Franklin", "Emmetsburg", "Johnston", "Edgewood", "Charlotte", "Greene", "Riverside", "Cedar Falls",
                    "Gilman", "Sheffield", "Superior", "Fremont", "Dayton", "Grant", "Jefferson", "Williams", "Brooklyn", "Aplington", "Parkersburg",
                    "Monmouth", "Atlantic", "Kalona", "Chariton", "Neola", "Cambridge", "Eldora", "Keswick", "North English", "Windsor Heights",
                    "State Center", "Bloomfield", "Solon", "Perry", "Shenandoah", "Anamosa", "Brighton", "Stratford", "Camanche", "Clinton", "Bedford",
                    "Clarinda", "Hills", "Manchester", "Fredericksburg", "Augusta", "Redfield", "Asbury", "Batavia", "Waverly", "Peosta", "Jesup", "Urbandale",
                    "West Bend", "Swisher", "Bellevue", "Oelwein", "Marquette", "Grundy Center", "Chester", "St. Paul", "Denver", "Sac City", "Elgin",
                    "Maxwell", "Walford", "Elkader", "Sheldon", "Unionville", "La Porte City", "Tama", "Fort Madison", "Pocahontas", "Estherville",
                    "Westwood", "West Liberty", "Ashton", "Underwood", "Arnolds Park", "Collins", "Rockwell", "Rockwell City", "Nevada", "Hastings",
                    "Plainfield", "Wallingford", "Corwith", "Keosauqua", "Milford", "Stanley", "Harris", "Orange City", "Denison", "Alta", "Sibley",
                    "Walnut", "Russell", "Colfax", "Guthrie Center", "New London", "Sioux Center", "Harcourt", "Monticello", "Columbus Junction",
                    "Creston", "Thor", "De Soto", "New Sharon", "Sergeant Bluff", "Madrid", "Ocheyedan", "Marengo", "Aurora", "Rock Valley", "Glidden",
                    "Mallard", "Farnhamville", "Panora", "Crescent", "Libertyville", "Waukon", "Holstein", "Dunlap", "Story City", "Rock Rapids", "Van Meter",
                    "Webster", "Webster City", "Earlham", "Griswold", "Plymouth", "Britt", "Shellsburg", "Danville", "West Branch", "Dysart", "Victor", "Tiffin",
                    "McGregor", "Guttenberg", "Aurelia", "Treynor", "Spillville", "Cascade", "Red Oak", "Battle Creek", "Mechanicsville", "St. Ansgar", "Davis City",
                    "Remsen", "Moville", "Traer", "Ogden", "Carlisle", "Eagle Grove", "Coin", "Winthrop", "Ida Grove", "Mapleton", "Baxter", "Akron", "Humeston",
                    "Elkhart", "Kelley", "Royal", "Luxemburg", "Le Claire", "Searsboro", "Lime Springs", "Schaller", "Little Rock", "Wall Lake", "Anthon", "Lucas",
                    "Delhi", "Pleasantville", "Stanton", "Harper", "Stuart", "Bondurant", "Corydon", "Williamsburg", "Corning", "Monona", "Henderson", "Tripoli",
                    "Vail", "Waterville", "Glenwood", "Hamilton", "Northwood", "Lamoni", "Laurel", "Barnes City", "Belle Plaine", "Sully", "Alburnett", "Hartley",
                    "Hawarden", "Eldridge", "Wapello", "Emerson", "Essex", "Coon Rapids", "Central City", "Winterset", "Arthur", "Dawson", "Van Horne", "Durant",
                    "Keystone", "Wadena", "Galt", "Winfield", "Mediapolis", "Shelby", "Mondamin", "Hayesville", "Milo", "Polk City", "Carter Lake", "Lake Mills",
                    "Rowley", "Lakeside", "Oakland", "Fayette", "Manning", "West Point", "Sanborn", "Leland", "Garden Grove", "Letts", "Blanchard", "Menlo",
                    "Farmington", "Belmond", "Hinton", "Springbrook", "Lake City", "Buffalo", "Buffalo Center", "Madison", "Dana Point", "Houston", "Kansas City",
                    "Visalia", "DeWitt", "Omaha", "Gretna", "Dyserville", "Runnells", "Northfield", "Lincoln", "Delano", "Glencoe", "Champaign",
                    "Eden Prairie", "Kahoka", "Dolliver", "Albert Lea", "Truxton", "Sarasota", "Mountain Home", "Norwalk", "Moline", "Macon",
                    "Jackson", "Bennington", "Excelsior", "St. Louis", "Sioux Falls", "Fulton", "Novato", "Wellington", "Chaska", "Oak Brook",
                    "Clinton", "Maple Plain", "Adair", "Queen Creek", "Apple Valley", "Emmetsburgh", "Edina", "Bricelyn", "Papillion", "Candler",
                    "Brookfield", "CORALVILLE", "Athol", "Dension", "Mt. Pleasant", "Canton", "Wellman", "Camdenton",
                    "Toddville", "Prairie Village", "La Vista", "Galena", "Rock Island", "Chanhassen", "Rose", "Mesa", "Lakeville", "Plattsmouth",
                    "Santa Monica", "Blue Earth", "Prairie du Chien", "Maryville", "Nebraska City", "Mound City", "Green Bay", "Marietta", "Wabasha",
                    "Dakota Dunes", "Inverness", "Mission Woods", "Fairmont", "O'Neil", "Saint Charles", "Pipestone", "Bucyrus", "Detroit Lakes",
                    "Tarkio", "Skokie", "Le Mars", "Taylor Ridge", "Luverne", "Coal Valley", "Mankato", "Andalusia", "Fortuna", "Troy"]

    Disciplinary(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        this.context = context
        ocrReader = moduleFactory.getOcrReader(context)
        addressParser.reloadData()
        addressParser.updateCities([US: CITIES])
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.replaceAll(/,/, " ")
        street = street.replaceAll(/_/, " ")
        street = street.replaceAll(/\s+/, " ").trim()
        street = street.replaceAll(/3"IStreet/, "3rd Street")
        street = street.replaceAll(/1"/, "1st").trim()
        street = street.replaceAll(/1212"` Ave NW/, "121 2nd Ave NW")
        street = street.replaceAll(/2"I?/, "2nd")
        street = street.replaceAll(/3"I?/, "3rd")
        street = street.replaceAll(/1500 " Ave SE, #108/, "1500-2nd Ave SE, #100")
        street = street.replaceAll(/302 "1 Street/, "302-3rd Street")
        street = street.replaceAll(/SALESPERSON/, "")
        street = street.replaceAll(/2 L\.C\. 1511 Monroe/, "1511 Monroe")
        street = street.replaceAll(/2 Heartland Realtors/, "")
        street = street.replaceAll(/516 Elm Street P.O. Box 148 burg/, "516 Elm Street P.O. Box 148")
        street = street.replaceAll(/^88 20 & 89 23 Frank P. Joens Jr. R. R. 1 Box 3C$/, "R. R. 1 Box 3C")
        street = street.replaceAll(/SETTI. ;EMENT AGREEMENl/, "")
        street = street.replaceAll(/^71 & B JeT$/, "HIWAY 71 & B JCT")
        street = street.replaceAll(/192ndd/, "192nd")
        street = street.replaceAll(/222 W. 21 d Street/, "222 W. 2nd Street")
        street = street.replaceAll(/300Y2 Front Street/, "300 1/2 Front Street")
        street = street.replaceAll(/2-15t Street/, "2-1st Street")
        street = street.replaceAll(/41115' Ave SE/, "411 1st Ave SE")
        street = street.replaceAll(/934B. South Rd. Prairie du Chien/, "934B. South Marquette Rd. Prairie du Chien")
        street = street.replaceAll(/113 Terre! Aye/, "113 Terrel Ave")
        street = street.replaceAll(/1 BEST REALTORS 2940 104TH STREET/, "2940 104TH STREET")
        street = street.replaceAll(/ACCEPTING WITHDRAWAL/, "")
        street = street.replaceAll(/901 Black.*Road Lo va/, "901 Black Hawk Road")
        street = street.replaceAll(/3501 WESTOWNPARKWAY/, "3501 WESTOWN PARKWAY")
        street = street.replaceAll(/0 SMITH 1108 8TH STREET/, "1108 8TH STREET")
        street = street.replaceAll(/3211 East-35:J' St. Court/, "3211 East 35th St. Court")
        street = street.replaceAll(/2500-41w'/, "2500-41st")
        street = street.replaceAll(/109 N. Is' Street/, "109 N. 1st Street")
        street = street.replaceAll(/^30 East$/, "Highway 30 East")
        street = street.replaceAll(/1032 Woo.*y Ave/, "1032 Woodbury Ave")
        street = street.replaceAll(/PIUTESTREET/, "PIUTE STREET")
        street = street.replaceAll(/CHA.*INFORMAL/, "")
        street = street.replaceAll(/0igna Quazar Capital Corporation/, "")
        street = street.replaceAll(/1 BEST REALTORS/, "")
        street = street.replaceAll(/20481H SW/, "204 8th SW")
        street = street.replaceAll(/825409 607 S. 1st Street/, "607 S. 1st Street")
        street = street.replaceAll(/10DOCRE015 Robert K. Miell Iowa County Jail/, "")
        street = street.replaceAll(/SETFLEIVIENT/, "")
        street = street.replaceAll(/711 127 Street/, "711 S 127 Street")
        street = street.replaceAll(/42o-11th Avenue North/, "1420-11th Avenue North")
        street = street.replaceAll(/261615t Ave NE/, "2616 1st Ave NE")
        street = street.replaceAll(/0. Green 116 W Franklin Street/, "116 W Franklin Street")
        street = street.replaceAll(/00888 Valentina K. Martin/, "")
        street = street.replaceAll(/90-039 Wendell G. Harms 90-040/, "")
        street = street.replaceAll(/0. Kunzman/, "")
        street = street.replaceAll(/Conclusions/, "")
        street = street.replaceAll(/150142nd'/, "1501 42nd")
        street = street.replaceAll(/26161st Ave NE/, "2616 1st Ave NE")
        street = street.replaceAll(/21-The Professional Group/, "")
        street = street.replaceAll(/417i' VO Brooke Gillum/, "")
        street = street.replaceAll(/9325 Bishop Drive Ste. 130 IN/, "9325 Bishop Drive Ste. 130")
        street = street.replaceAll(/\bLEIST\b/, "")
        street = street.replaceAll(/WESTOWNPKWY/, "WESTOWN PKWY")
        street = street.trim()


        return street
    }
    def missingUrls = []

    def initParsing() {
        def html = invokeUrl(dataUrl)
        def lastPageNumber = getLastPageNumber(html)
        //For first page only
        captureDiv(html)
        //For other pages
        capturePdfForOtherPages(lastPageNumber)
        println("URLS:\n\n" + missingUrls + "\nSize " + missingUrls.size())
    }
    def pageNumber;

    def capturePdfForOtherPages(def lastPageNumber) {
        for (int i = 1; i <= lastPageNumber; i++) {
            pageNumber = i
            def pageUrl = dataUrl + "?t=&l=&c=All&field_year_value=&page=$i"
            def html = invokeUrl(pageUrl)
            captureDiv(html)
        }
    }

    def captureDiv(def html) {
        //Div Capture
        def divCaptureMatcher = html =~ /(?s)<td headers="view-title-table-column"(.+?)<\/tr>/
        while (divCaptureMatcher.find()) {
            def divBlock = divCaptureMatcher.group(0)
            def date = captureDate(divBlock)
            capturePdf(divBlock, date)
        }
    }

    //For Date Capturing
    def captureDate(def divBlock) {
        def date
        def dateMatcher = divBlock =~ /(?<="datetime">)\d{4}/
        if (dateMatcher.find()) {
            date = dateMatcher.group(0)
        }
        return date
    }

    def pdfCounter = 0

    def capturePdf(def divBlock, def date) {
        //PDF link Matcher
        def pdfLinkMatcher = divBlock =~ "(?<=pdf-download-link-container\"><a href=')([^']+)"
        pdfCounter++
        if (pdfLinkMatcher.find()) {
            def pdfLink = "https://plb.iowa.gov" + pdfLinkMatcher.group(1)
            def pdfData = ""
            try {
                pdfData = pdfToTextConverter(pdfLink)
            }
            catch (Exception e) {
                println(" O C R F A I L E D ")
                println(e.toString())
                missingUrls.add(pdfLink)
            }
            def staticNameList = getStaticName(pdfLink)
            def staticAddress = getStaticAddress(pdfLink)


            def blockMatcher1 = pdfData =~ "(?ism)(In the Matter of|In RE|InRE:|IN RE|INRE)((.*?\\n){30})"
            if (blockMatcher1.find()) {
                def block2
                def first15Linesblock = blockMatcher1.group(0)
                first15Linesblock = first15Linesblock.replaceAll(/(?ism)Real Estate Comission.*/, "").trim()
                def blockMatcher = first15Linesblock =~ "(?ism)(In RE:|InRE:|In the Matter of|IN RE|INRE).*?(Respondent|Applicant|On this|The Iowa)"
                if (blockMatcher.find()) {
                    def block = blockMatcher.group(0)
                    block = sanitizeBlock(block)
                    //Get Address
                    def address

                    if (staticAddress != null) {
                        address = staticAddress
                    } else {
                        address = getAddress(block)
                        address = address.trim()
                        def address2

                        if (address.equals("null")) {
                            def addressHandler = first15Linesblock =~ /(?ism)(?<=(Respondent|Applicant|On this|The Iowa)).*/
                            if (addressHandler.find()) {
                                block2 = addressHandler.group(0)
                                address2 = getAddress(block2)
                                address = address2
                            }
                        }

                        address = sanitizeAddress(address)
                    }

                    // Fixing other country issues
                    address += ', US'

                    //Name Matcher
                    def nameList
                    if (staticNameList) {
                        nameList = staticNameList
                    } else {
                        nameList = getName(block, block2)
                    }
                    //CreateEntity
                    nameList.each {
                        if (it != null && !it.equals("null")) {
                            //Get Alias
                            def alias
                            (it, alias) = getAlias(it)
                            it = sanitizeName(it)
                            def entityTypeFromName = getEntityType(it)
                            //createEntity(name, alias, address, date)
                            ScrapeAddress scrapeAddress

                            if (it != null && !it.equals("null")) {
                                // Parsing Address
                                if (!address.equals("null")) {
                                    def addrMap = addressParser.parseAddress([text: address, force_country: true])
                                    scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                                }
                                // Create Entity
                                createEntity(it, alias, entityTypeFromName, scrapeAddress, date, pdfLink)
                            }
                        }
                    }
                }
            }
        }
    }


    def createEntity(def entityName, def alias, def entityType, def address, def date, def entityURL) {
        def entity = null
        entity = context.findEntity("name": entityName, "type": entityType)
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(entityName)
            entity.setType(entityType)
        }
        if (alias != null) {
            entity.addAlias(alias)
        }

        entity.addAddress(address)
        entity.addUrl(entityURL)

        // Adding event
        ScrapeEvent scrapeEvent = new ScrapeEvent()
        scrapeEvent.setDescription("This entity appears on the Iowa Professional Licensing Board list of Real Estate Disciplinary Actions.")

        // Add date to event if have any
        if (!date.equals("null") && date != null) {
            date = "01/01/" + date
            def sDate = context.parseDate(new StringSource(date), ["MM/dd/yyyy", "MM/dd/yyyy"] as String[])
            scrapeEvent.setDate(sDate)
        }

        // Adding event to entity
        entity.addEvent(scrapeEvent)
    }

    def getEntityType(String name) {
        def organizationPattern = /(?ism)\b(inc\.?|Realty|New|Land|Financial|Realtors?|LLC\.?|PLLC\.?|Auctioneering|Properties|Resources?|Associates?|Partners|Neughbours|Company|Auction|Estate|ENTERPRISES?|Performers|Partners|PC|Banker|Corp\.?|Co\.|Agency|Associate|Group|Management|USA|FLAT|Group|EXECUTIVES|LTD|Service|Co\.?|Sales|American?|Iowa|Asociates|Professionals|Commercial|L\.?C\.?|Team|Re\/Max| \d+ |Homes 'N More|Referral|RR1|Motel|Homes?|Consulting|Brown 3|Bank|Development|Consulting|Country|State|Depot|Property|Agricultural|Capital|Corporation|FACILITY|Investments?)\b/

        def organizationMatcher = name =~ organizationPattern

        if (organizationMatcher.find()) {
            return "O"
        } else {
            return "P"
        }
    }

    def getName(def block, def block2) {
        def address = getAddress(block)
        // Replacing slash with whitespace to avoid exception in pattern matching
        address = address.replaceAll("\\\\", " ")
        block = block.replaceAll("\\\\", " ")
        def address2 = getAddress(block2)
        try {
            def nameBlock = block.toString().replaceAll(/(?ism)$address/, " ").trim()
                .replaceAll(/(?ism)In the Matter of:?|In RE:?|InRE:?|IN RE:?|INRE:?/, " ").trim()
                .replaceAll(/(?ism)Respondent|Applicant|On this|The Iowa/, " ").trim()
            def nameBlock2
            if (block2) {
                block2 = sanitizeBlock(block2)
                def matcher = block2 =~ /(?ism)(.*?)\n/
                if (matcher.find()) {
                    nameBlock2 = matcher.group(1)
                }
                nameBlock2 = nameBlock2.toString().replaceAll(/(?ism)^\d.*/, " ").trim()
            }
            //Remove empty line in the block
            if (nameBlock =~ /(?m)^[ \t]*\r?(\n|$)/) {
                nameBlock = nameBlock.toString().replaceAll(/(?m)^[ \t]*\r?(\n|,$)/, "").trim()
            }
            nameBlock = nameBlock.toString().replaceAll(/(?ism)^(.*?)\n*\s*(a\/k\/a|dba.*)/, { def a, b, c -> return b + " " + c }).trim()
            def multipleNameList = nameBlock.split(/(?ism)\n/).collect({ its -> return its })
            if (nameBlock2) {
                multipleNameList.add(nameBlock2)
            }

            return multipleNameList
        } catch (Exception e) {
        }

    }

    def getAlias(def name) {
        def alias
        name = name.toString().trim()
        def aliasMatcher = name =~ /(?ism)(a[\.\/]?k[\.\/]?a|d[\.\/]?b[\.\/]?a:?)(.*)/
        if (aliasMatcher.find()) {
            def delete = aliasMatcher.group(0)
            alias = aliasMatcher.group(2).trim()
            name = name.replaceAll(/$delete/, " ").trim()
        }
        return [name, alias]
    }

    def getAddress(def block) {
        def address = "null"
        def addressMatcher = block =~ "(?ism)(P\\.?\\s?O\\.?\\s?Box\\s?|Box\\s|RR\\s|W\\.\\s|South Hwy\\s)?\\d{1,}.*(?=Respondent|Applicant)"
        def addressMatcher2 = block =~ "(?ism)(P\\.?\\s?O\\.?\\s?Box\\s?|Box\\s|RR\\s|W\\.\\s|South Hwy\\s)?\\d{1,}.*\\d"
        if (addressMatcher.find()) {
            address = addressMatcher.group(0)
        } else if (addressMatcher2.find()) {
            address = addressMatcher2.group(0)
        }

        return address
    }

    def sanitizeAddress(def address) {
        address = concatenateLines(address)
        address = address.replaceAll(/.*(?<=Inc\.|Inc |Pvt\.|Pvt )/, "").trim()
        address = address.replaceAll(/On this.*/, "").trim()
        address = address.replaceAll(/(?i)The Iowa Real Estate.*/, " ").trim()
        address = address.replaceAll(/\) DISCIPLINARY CASE|\) CASE NUMBER: 13-093|DECISION AND ORDER|DISCIPLINARY CASE|AND CONSENT ORDER IN A|DISCIPLINARY\. CASE|\) CASE NUMBER: 13-018|On July 26, 2013|13REC005 BONNER K. TUINSTRA Expired|CONCLUSIONS|LAW,|AND CONSENT ORDER IN A|ORDER IN A DISCIPLINARY|,CASE|CEASE AND DESIST ORDER|CONSENT AGREEEMENT AND|CEASE AND DESIST ORDER \./, "").trim()
        address = address.replaceAll(/DISCIPLINARY\. CASE|CONSENT AGREEEMENT AND|CEASE AND DESIST ORDER|BY AGREEMENT|BY AGREEMENT|SETTLEMENT,AGREEMENT|DISCIPLINARY CASE|\?%" " \?%\?%" " \?%\?%" " \?%\?%" " " 110\?%1" 1111|DISCIPLINARY CASE/, "").trim()
        address = address.replaceAll(/DISCIPLINARY CASE|AND CONSENT ORDER jN A|AND CONSENT ORDER IN A/, "").trim()
        address = address.replaceAll(/(?i)Pursuant*/, " ").trim()
        address = address.replaceAll(/^.*LLC\.?/, " ").trim()
        address = address.replaceAll(/(?i)^.*inc\.?/, " ").trim()
        address = address.replaceAll(/(?i)^.*Realty/, " ").trim()
        address = address.replaceAll(/(?i)^.*Company\.?/, " ").trim()
        address = address.replaceAll(/(?i)^.*Ltd\.?/, " ").trim()
        address = address.replaceAll(/(?i)^.*Real Estate[ ]?,?/, " ").trim()
        address = address.replaceAll(/SE.*LEMENT/, " ").trim()
        address = address.replaceAll(/Iowa Code.*/, " ").trim()
        address = address.replaceAll(/AGREE.*NT/, " ").trim()
        address = address.replaceAll(/DISCIPLINARY/, " ").trim()
        address = address.replaceAll(/BY/, " ").trim()
        address = address.replaceAll(/\s{2,}/, " ").trim()
        address = address.replaceAll(/\s{1,},/, ",").trim()
        address = address.replaceAll(/Runneils/, "Runnells").trim()
        address = address.replaceAll(/Urbandaie/, "Urbandale").trim()
        address = address.replaceAll(/Do'liver/, "Dolliver").trim()
        address = address.replaceAll(/salesperson license.*/, " ").trim()
        address = address.replaceAll(/oln Ave.*(?=Suite B Spirit Lake|Ste\. B)/, "1710 Lincoln Ave, ")
        address = address.replaceAll(/844394 COMBINED/, "").trim()
        address = address.replaceAll(/Mo'ine/, "Moline").trim()
        address = address.replaceAll(/ Mn\.? /, " MN ").trim()
        address = address.replaceAll(/\)/, "").trim()
        address = address.replaceAll(/SETTL.*Nrr/, " ").trim()
        address = address.replaceAll(/ 1D /, " ID ").trim()
        address = address.replaceAll(/INTENT TO ISSUE/, " ").trim()
        address = address.replaceAll(/DESMOINES|Des Hoines/, "Des Moines")
        address = address.replaceAll(/LaVista/, "La Vista")
        address = address.replaceAll(/.*17A.*/, " ").trim() // Destructive
        address = address.replaceAll(/.*firm,? license.*/, "").trim() // Destructive
        address = address.replaceAll(/21 PREFERRED/, "").trim()
        address = address.replaceAll(/\/Better Homes and Garden/, "").trim()
        address = address.replaceAll(/'LEl\\JIENT/, "").trim()
        address = address.replaceAll(/Antes, IA/, "Ames, IA").trim()
        address = address.replaceAll(/(?i)LeMars/, "Le Mars").trim()
        address = address.replaceAll(/Lucerne/, "Luverne").trim()
        address = address.replaceAll(/(?i)Taylorridge/, "Taylor Ridge").trim()
        address = address.replaceAll(/\(B00099/, "").trim()
        address = address.replaceAll(/(520561|801413|804887).*to/, "").trim()
        address = address.replaceAll(/ AND( 1N A)? /, "").trim()
        address = address.replaceAll(/LICENSE/, "").trim()
        address = address.replaceAll(/APPLICATION/, "").trim()
        address = address.replaceAll(/^3, P.C. 27/, "P. C. 27")
        address = address.replaceAll(/^1, Desoto, Wisconsin 54624/, "Desoto, Wisconsin 54624")
        address = address.replaceAll(/.*F01869. Her license.*/, "").trim()
        address = address.replaceAll(/^700 Iowa Administrative Code.*/, "").trim()
        address = address.replaceAll(/^lude one copy of the listing,.*/, "").trim()
        address = address.replaceAll(/\b21 Preferred\b/, "")
        address = address.replaceAll(/\bA.REEMENT\b/, "")
        address = address.replaceAll(/306 li\*y\./, "306 Hwy.")
        address = address.replaceAll(/IN RE:.*$/, "")
        address = address.replaceAll(/Water 100/, "Waterloo")
        address = address.replaceAll(/SKI-ELEMENT/, "")
        // Fixing street format for Scrapian Address
        if (address.toLowerCase().contains("manchester")) { // Chester is a Substring of Manchester
            def staticCity = "Manchester"
            address = address.replaceAll(/(?i)$staticCity,?/, ",$staticCity,")
        } else if (address.toLowerCase().contains("west des moines")) { // des moines is a substring of West des moines
            def staticCity = "West Des Moines"
            address = address.replaceAll(/(?i)$staticCity,?/, ",$staticCity,")
            address = address.split(",West Des Moines,")
            address = address[0].replaceAll(/\s/, "_") + ",West Des Moines," + address[1]
        } else if (address.toLowerCase().contains("unionville")) { // union is a substring of Unionville
            def staticCity = "Unionville"
            address = address.replaceAll(/(?i)$staticCity,?/, ",$staticCity,")
        } else {
            for (String city : CITIES) {
                if (address.toLowerCase().contains(city.toLowerCase())) {
                    address = address.replaceAll(/(?i)\b$city,?\b/, ",$city,")
                }
            }
        }

        def addressMatcher = address =~ /(P\.?O\.?\s?Box\s?|RR\s|Box\s|W\.\s|South Hwy\s)?[0-9]*?.*\d{4,5}[\d -]+/

        if (addressMatcher.find()) {
            address = addressMatcher.group(0)

            return address
        } else {
            return "Iowa"
        }
    }

    def sanitizeName(def name) {
        if (!name.equals("null")) {
            name = name.replaceAll(/[,-]*$/, "").trim()
                .replaceAll(/The Iowa Real Estate Commission.*/, " ").trim()
                .replaceAll(/On March.*/, " ").trim()
                .replaceAll(/AND$/, " ").trim()
                .replaceAll(/(?i)Relmax/, "Re/Max").trim()
                .replaceAll(/ .?alty/, " Realty").trim()
                .replaceAll(/NOTICE/, " ").trim()
                .replaceAll(/COMMISSION/, " ").trim()
                .replaceAll(/Sole Properietor/, "")
                .replaceAll(/(?ism)\b(Jr|Sr|Ms|Mr|Mrs)\.?\b/, "")
                .replaceAll(/Lepic roeger/, "Lepic Kroeger")
                .replaceAll(/>/, "")
                .replaceAll(/[^A-Za-z0-9]$/, "")
                .replaceAll(/^Signal$/, "")
                .replaceAll(/\bBY$\b/, "")
                .replaceAll(/Decorah\\/Iowa Realty/, "Decorah Iowa Realty")
                .replaceAll(/Clark\\/Iowa Realty/, "Clark Iowa Realty")
                .replaceAll(/(?i)in the matter/, "").trim()
                .replaceAll(/\bmerica\b/, "America")
                .replaceAll(/^A-1 Real Estate Service$/, "A-1 Real Estate Services")
                .replaceAll(/Co!dwell/, "Coldwell")
                .replaceAll(/^Bradley S. Rosch I$/, "Bradley S. Rosch")
                .replaceAll(/\bENTERPROSES\b/, "ENTERPRISES")
                .replaceAll(/Mel Fdster/, "Mel Foster")
                .replaceAll(/NI Dodge II/, "NP Dodge II")
                .replaceAll(/Pursuant to Iowa Code Section/, "")
                .replaceAll(/Vicki ariscoe/, "Vicki L. Briscoe")
                .replaceAll(/(?i)SALESPERSON LICENSE|Hwy/, "")
                .replaceAll(/(?i)^Ole's$/, "Ole's 5 Star Realty, LLC")
                .replaceAll(/^Stephanie$/, "Stephanie Bishop")

            name = name =~ /[A-Za-z0-9].*[1A-Za-z\.]/

            if (name.find()) {
                return name.group(0)
            }
        }
    }

    def getStaticName(def url) {
        def key = url.replaceAll(/^.*\//, "")
        def statics = ["Baier12-370_01.pdf"                                                        : ["Tim J. Baier"],
                       "Beedon13-220.pdf"                                                          : ["Joseph Beedon"],
                       "Beider13-250.pdf"                                                          : ["Daniel A. Beider"],
                       "20-067%20Sires%2C%20Vennessa%20S.pdf"                                      : ["Venessa S. Sires"],
                       "Bittner13-077.pdf"                                                         : ["Daniel Bittner"],
                       "Bru12-171_0003.pdf"                                                        : ["Diane J. Bru"],
                       "Forbes12-256.pdf"                                                          : ["Wendy Frieden", "Frieden Property Management"],
                       "Girard13-161.pdf"                                                          : ["John D. Girard", "Schramm And Associates, Inc."],
                       "Juhl13-086.pdf"                                                            : ["Kari Juhl"],
                       "King12-399.pdf"                                                            : ["Vincent King", "ZL, LLC"],
                       "Lang13-192.pdf"                                                            : ["Lucky Lang II"],
                       "Malone12-094_0002.pdf"                                                     : ["Patrick D. Malone", "Malone Real Estate"],
                       "MarcusMillichap12-390.pdf"                                                 : ["Marcus & Millichap R.E. Invest. Services of Chicago, Inc."],
                       "McCleland13-012.pdf"                                                       : ["Rhonda C. McCleland", "Genesis 2 LC."],
                       "O%27Byrne12-422.pdf"                                                       : ["Hugh E. O'Byrne"],
                       "Pageler13-040.pdf"                                                         : ["Lisa Pageler", "Le Mars 1st Choice, LLC"],
                       "Parker13-094.pdf"                                                          : ["Lora M. Parker", "Why USA Parker Realty"],
                       "Peters13-068.pdf"                                                          : ["Charles A. Peters, Jr."],
                       "Schueler%2012-319.pdf"                                                     : ["James B. Schueler", "Hawkeye Heritage Real Estate"],
                       "Sogard13-101.pdf"                                                          : ["Roger D. Sogard", "Fjelland Real Estate & Farm Management, LTD"],
                       "TargetRealty13-051_0010.pdf"                                               : ["Target Realty LLC"],
                       "Teaney13-108.pdf"                                                          : ["G. Reid Teaney", "Apartment Realty Advisors a/k/a Central States, Inc."],
                       "Traver13-130.pdf"                                                          : ["Amy Traver", "Premier Realty Group, Inc."],
                       "Tuinstra13-059.pdf"                                                        : ["BONNER K. TUINSTRA"],
                       "Wildermuth12-278.pdf"                                                      : ["Elinor F. Wildennuth", "Mel Foster Co. Inc. of Illinois"],
                       "Wojewoda13-168.pdf"                                                        : ["Bobbi Jo Wojewoda"],
                       "Swingle13-252.pdf"                                                         : ["Jeremie Swingle", "Golden Circle Real Estate Group, LLC"],
                       "Adler%2C%20Thomas%2014-003.pdf"                                            : ["Thomas M. Adler"],
                       "Banwart%2C%20Robert%2013-294.pdf"                                          : ["Robert D. Banwart", "Peoples Company of Indianola"],
                       "Bray%2C%20Leria%2014-020.pdf"                                              : ["Leria Bray", "NP Dodge Real Estate Sales, Inc."],
                       "Caldwell-Owen%2C%20Erin%2013-225.pdf"                                      : ["Erin Caldwell-Owen", "Lora L. Ahrens-Olerich"],
                       "Casady%2C%20Nancy%2014-019.pdf"                                            : ["Nancy A. Casady", "Advantage Real Estate"],
                       "Cipale%2C%20Charles%2013-243%202014.pdf"                                   : ["Charles S. Cipale"],
                       "Grant%2C%20Marcia%2013-262.pdf"                                            : ["Marcia J. Grant", "Grant Real Estate Services, LLC"],
                       "Hartzler%2C%20Chad%2014-002.pdf"                                           : ["Chad Hartzler"],
                       "Hunziker%2C%20Gary%2013-286.pdf"                                           : ["Gary J. Hunziker", "Hunziker Property Management"],
                       "Lake%2C%20Kathy%2014-042.pdf"                                              : ["Kathy L. Lake", "Re/Max People"],
                       "Mathis%2C%20Sharon%2013-229.pdf"                                           : ["Sharon K. Mathis"],
                       "Moen%2C%20Jennifer%2014-016.pdf"                                           : ["Jennifer Moen", "Creative Consulting Real Estate, LLC"],
                       "Perry%2C%20Philip%2013-274.pdf"                                            : ["Philip Perry", "Perry Reid Properties-Management, L.L.C."],
                       "Preul%2C%20Ryan%2013-109.pdf"                                              : ["Ryan S. Preul"],
                       "Radocaj%2C%20Cindy%2014-026.pdf"                                           : ["Cindy A. Radocaj"],
                       "Reber%2C%20Matthew%2013-271.pdf"                                           : ["Rural Properties of Iowa, LLC"],
                       "Redman%2C%20David%2013-288.pdf"                                            : ["DAID REDMAN"],
                       "Advantage12-141.pdf"                                                       : ["Advantage Realty"],
                       "Allen12-200.pdf"                                                           : ["Jere L. Allen", "Sunbelt Hotel Brokers, LLC"],
                       "Collins12-006.pdf"                                                         : ["Tammy Collins", "QCA Progressive Partners"],
                       "Covington10-508.pdf"                                                       : ["Jodi Covington"],
                       "David12-153.pdf"                                                           : ["Julie C. David", "Al Alloway, Inc."],
                       "Ewers11-314.pdf"                                                           : ["William F. Ewers"],
                       "Grant12-061.pdf"                                                           : ["Robert L. Grant"],
                       "Halblom12-113.pdf"                                                         : ["Joyce Halblom"],
                       "Hansch12-047.pdf"                                                          : ["Teresa J. Hansch"],
                       "Jacobsen11-222.pdf"                                                        : ["Joe Jacobsen", "Land and Home Realty"],
                       "Nelson%2C%20Glen%2012-107_0.pdf"                                           : ["Glen C. Nelson", "Property Connection Real Estate"],
                       "Curran%2C%20James%2013-163.pdf"                                            : ["James Curan"],
                       "Vaith%2C%20Lola%2013-209.pdf"                                              : ["Lola D. Vaith", "Century 21 Preferred"],
                       "Ruhl%20Jr%2C%20Charles%2014-001.pdf"                                       : ["Charles A. Ruhl, Jr."],
                       "Schultheis%2C%20Timothy%2014-071.pdf"                                      : ["Timnonthy C. Schulthels", "Schult Investments LLC"],
                       "Kohles%2C%20Andrew%2014-106.pdf"                                           : ["Andrew J. Kohles", "Coldwell Banker Mid-America Group"],
                       "Palmer%2C%20Myong-Hui%2014-122.pdf"                                        : ["Myong-Hui Palmar"],
                       "Gillum%2C%20Brooke%2014-127.pdf"                                           : ["Brooke Gillum"],
                       "Todd%2C%20Mark%2010-167.pdf"                                               : ["Mark A. Todd"],
                       "Kline%2C%20Phyllis%2013-226.pdf"                                           : ["Phyllis J. Kline", "KLC Property Management Solutions, LC."],
                       "McGee%2C%20Sylvia%2013-289.pdf"                                            : ["Sylvia McGee"],
                       "de%20Bruin%2C%20Dylan%2014-092.pdf"                                        : ["Dylan de Bruin"],
                       "Sieren%2C%20Lisa%2014-099.pdf"                                             : ["Lisa S. Sieren"],
                       "Kahle12-064.pdf"                                                           : ["Sara A. Kahle", "Mid Continent Realty Investment, Inc."],
                       "Kelchen12-057%2C12-133.pdf"                                                : ["Kelli S. Kelchen", "Click Call Move, Inc."],
                       "Kelchen12-057%2C12-133_0.pdf"                                              : ["Kelli S. Kelchen", "Click Call Move, Inc."],
                       "Kunzman11-295.pdf"                                                         : ["William O. Kunzman"],
                       "North12-135.pdf"                                                           : ["North Central Commercial Real Estate LLC"],
                       "Rogers12-095.pdf"                                                          : ["Pamela J. Rogers", "Lande Real Estate"],
                       "Sattler12-091%2012-142.pdf"                                                : ["Sattler Realty"],
                       "Sattler12-091%2012-142_0.pdf"                                              : ["Sattler Realty"],
                       "Skaff-GreggA12-001.pdf"                                                    : ["Julie Skaff-Gregg", "Associated Brokers Realty, Inc."],
                       "Thomas12-048.pdf"                                                          : ["SCOTT E. THOMAS, SR.", "Associates Realty, LLC"],
                       "Von%20Hollen10-509.pdf"                                                    : ["Michael C. Von Hollen", "Von Matt Partners Corporation"],
                       "Althoff09-257.pdf"                                                         : ["Allison D. Althoff"],
                       "Appelquist10-019.pdf"                                                      : ["Rhonda Appelquist"],
                       "Ashburn09-066.pdf"                                                         : ["Matthew A. Ashburn", "RKH Investments"],
                       "Avalon10-179.pdf"                                                          : ["Avalon Real Estate Company"],
                       "Bemrich10-281.pdf"                                                         : ["Michelle Bemrich"],
                       "Brown%2CDonald09-010.pdf"                                                  : ["Donald L. Brown, Jr.", "Mid-American Referral Company"],
                       "BuddyLeeLLC10-013.pdf"                                                     : ["Buddy Lee, LLC"],
                       "Bull10-380.pdf"                                                            : ["Colleen Bull", "Asset Relocation Services, Inc."],
                       "Burns10-212.pdf"                                                           : ["Matthew Burns", "Iowa Realty"],
                       "Century21HansenA10-022.pdf"                                                : ["Century 21 Hansen Realty"],
                       "Brock10-478plus.pdf"                                                       : ["Katherine A. Brock", "Albia Realty Company"],
                       "Dressel09-285.pdf"                                                         : ["Robert Dressel, Jr."],
                       "Fiester09-254.pdf"                                                         : ["Paula A. Fiester"],
                       "Firzlaff10-463.pdf"                                                        : ["Lindsay M. Firzlaff"],
                       "Gentz10-345.pdf"                                                           : ["Lorna J. Gentz", "Grinnell Realty"],
                       "Godwin10-282.pdf"                                                          : ["Mary Godwin", "Godwin Realty"],
                       "GoedeA10-036.pdf"                                                          : ["John W. Goede", "Century 21 Jacobsen Real Estate, Inc."],
                       "Humiston09-345.pdf"                                                        : ["Linda J. Humiston"],
                       "Jackson09-191.pdf"                                                         : ["Krista M. Jackson"],
                       "JohnsonBrandon09-134.pdf"                                                  : ["Brandon Johnson", "Godwin Realty"],
                       "KLC%20Property%20Management.pdf"                                           : ["KLC Property Management Solution, L.C."],
                       "14-154%20Hughes%2C%20Mary.pdf"                                             : ["Mary J. Hughes"],
                       "14-073%20Fischer%2C%20Julie.pdf"                                           : ["Julie A. Fischer", "Premier Realty Group, Inc."],
                       "14-058%20Brandt%2C%20Mark.pdf"                                             : ["Mark Brandt"],
                       "Reed%2C%20Janet%2013-259.pdf"                                              : ["Janet Reed"],
                       "Konchar10-194.pdf"                                                         : ["Jon R. Konchar", "FNBC Iowa Realty"],
                       "Martin10-263.pdf"                                                          : ["Curtis G. Martin", "Re/Max Oelwein Realty"],
                       "Meadows09-292.pdf"                                                         : ["Howard T. Meadows"],
                       "Melichar10-283.pdf"                                                        : ["Larry D. Melichar", "CBS Home Real Estate Company"],
                       "Miller%2CHenry09-253.pdf"                                                  : ["Henry S, Miller"],
                       "Mosher09-053.pdf"                                                          : ["Rita Mosher", "Associated Brokers Realty"],
                       "MoultonReferrals10-173.pdf"                                                : ["Moulton Referrals, Inc."],
                       "Murphy10-347.pdf"                                                          : ["PATRICK J. MURPHY", "First Realty LTD"],
                       "N%26M%20Brokerage09-163.pdf"                                               : ["N & M Brokerage Services, LLC."],
                       "Nerud09-316.pdf"                                                           : ["Jack D. Nerud"],
                       "Pauling10-329.pdf"                                                         : ["Terry L. Pauling", "Peoples Company of Indonesia"],
                       "Paulson10-417.pdf"                                                         : ["Michelle R. Paulson", "Grinnell Fifth Avenue Realty"],
                       "Pech10-099.pdf"                                                            : ["Tiffany Pech"],
                       "ReidProperties09-209.pdf"                                                  : ["Perry Reid Propet-ties-Mgmt, LLC"],
                       "Reed10-367.pdf"                                                            : ["Jon M. Reed", "Skogman Realty"],
                       "Reisner09-298.pdf"                                                         : ["Cheryl Reisner"],
                       "Robertson09-006and09-113.pdf"                                              : ["Jennifer D. Robertson", "The Realty Dot"],
                       "Robertson09-006and09-113_0.pdf"                                            : ["Jennifer D. Robertson", "The Realty Dot"],
                       "Scott10-270.pdf"                                                           : ["Tiffani S. Scott", "Signature Resources"],
                       "Shetterly09-171.pdf"                                                       : ["Garrett C. Shetterly", "Innovations Realty, LLC."],
                       "Simmons09-156.pdf"                                                         : ["Steven W. Simmons", "REO, LLC."],
                       "Thomas09-003.pdf"                                                          : ["Scott E. Thomas, Sr.", "Associates Realty, LLC"],
                       "Thomas09-003_0.pdf"                                                        : ["Scott E. Thomas, Sr.", "Associates Realty, LLC"],
                       "Thomas09-003_1.pdf"                                                        : ["Scott E. Thomas, Sr.", "Associates Realty, LLC"],
                       "Tribbett09-351.pdf"                                                        : ["Denise K. Tribbett"],
                       "Volz10-168.pdf"                                                            : ["Andrew R. Volz"],
                       "Walker09-353.pdf"                                                          : ["Sharon K. Walker"],
                       "WebbA10-025.pdf"                                                           : ["Philip F. Webb", "The Webb Agency, Inc."],
                       "Westgor09-107.pdf"                                                         : ["Jeffrey W. Westgor", "Westgor & Associates, Inc."],
                       "WestmarkA10-011.pdf"                                                       : ["Laura L, Westmark", "Westmark's Great River Realty Co."],
                       "Wolf10-240.pdf"                                                            : ["Mary Rae Wolf"],
                       "Wymer09-211.pdf"                                                           : ["Cara Wymer", "Okoboji Property Management"],
                       "Young09-207.pdf"                                                           : ["Robert C. Young", "Skogman Realty"],
                       "Zevenbergen09-236.pdf"                                                     : ["Tammy Zevenbergen", "Team Realty Services"],
                       "Zink09-238.pdf"                                                            : ["Annette F. Zink", "Skogman Realty"],
                       "Anderson08-222.pdf"                                                        : ["Rolf O. Anderson"],
                       "BertlingA07-023.pdf"                                                       : ["Molly Jo Bertling", "Coldwell Banker Lee's Town and Country"],
                       "Bucheli07-308.pdf"                                                         : ["Harley E. Bucheli"],
                       "Bunn08-021.pdf"                                                            : ["Lora Bunn"],
                       "Charter08-138.pdf"                                                         : ["Mark Charter", "Signature Real Estate, Inc."],
                       "Clark07-094.pdf"                                                           : ["Charles A. Clark"],
                       "Cooling07-218.pdf"                                                         : ["Sue E. Cooling", "Iowa Realty"],
                       "Cooper07-046.pdf"                                                          : ["Harriette Cooper"],
                       "Draheim08-237.pdf"                                                         : ["Vernell J. Draheim"],
                       "Hahn07-299.pdf"                                                            : ["Todd Hahn", "Von Matt Partners Corporation"],
                       "KKI07-239.pdf"                                                             : ["KKI, LLC"],
                       "Layland08-255.pdf"                                                         : ["Kim Layland", "Innovation Realty, LLC"],
                       "McCartie08-016.pdf"                                                        : ["Donald G. McCartie", "Gatton Realty Company"],
                       "McGrath07-262.pdf"                                                         : ["Laurie McGrath"],
                       "Michelson08-054.pdf"                                                       : ["Bruce V. Michelson"],
                       "Muhs08-104.pdf"                                                            : ["Dean D. Muhs", "Steven J. Welbourne"],
                       "Navarro08-201.pdf"                                                         : ["Melissa V. Navarro", "Oakridge Realtors"],
                       "Neighbours08-149.pdf"                                                      : ["Yolanda Neighbours"],
                       "Ocken08-146.pdf"                                                           : ["Lynn Ocken"],
                       "Plum07-193.pdf"                                                            : ["Diane K. Plum"],
                       "Reed08-208.pdf"                                                            : ["Katrina J. Reed", "Skogman Realty Co."],
                       "Referral07-302.pdf"                                                        : ["The Referral Company"],
                       "Reilly08-133.pdf"                                                          : ["Charles M. Reilly"],
                       "RichterWay08-142.pdf"                                                      : ["Peggy S. Richter Way"],
                       "Swanson07-061.pdf"                                                         : ["VICTORIA S. SWANSON", "HORSTMAN REAL ESTATE"],
                       "VanHorn07-044.pdf"                                                         : ["ROBERT H. VAN HORN", "VAN HORN REAL ESTATE"],
                       "York07-224.pdf"                                                            : ["KentM. York", "Godwin Realty"],
                       "Zalesky07-107.pdf"                                                         : ["Joseph M. Zalesky", "Kopel Realtors & Consultants"],
                       "Zinnel07-243.pdf"                                                          : ["Annette L. Zinnel", "Goodwin Agency"],
                       "Anaya06-167.pdf"                                                           : ["Rosa Anaya", "Dirks Real Estate"],
                       "Barkalow04-165.pdf"                                                        : ["TRACY S. BARKALOW", "RE/MAX PREMIER PROPERTIES"],
                       "Bousselot07-062.pdf"                                                       : ["Tisha M. Bousselot", "River Cities Realtors, Inc."],
                       "Doan05-025.pdf"                                                            : ["R. CONRAD DOAN", "BILL RAMSEY REALTORS, INC."],
                       "Dyer06-090.pdf"                                                            : ["Lynette Dyer", "REO,LLC"],
                       "Edwards07-128.pdf"                                                         : ["Amanda J. Edwards"],
                       "FjellandA06-023.pdf"                                                       : ["Bradley Fjelland", "Fjelland Real Estate and Farm Management"],
                       "Ford07-082.pdf"                                                            : ["Stephen C. Ford"],
                       "Fouch07-170.pdf"                                                           : ["Angelina J. Fouch"],
                       "Gochnauer06-194.pdf"                                                       : ["Chase Gochnauer"],
                       "Hartman07-125.pdf"                                                         : ["Timothy Hartman"],
                       "Hirsch06-173.pdf"                                                          : ["Annette M. Hirsch", "Cardinal City Realty, Inc."],
                       "HorrasA06-014.pdf"                                                         : ["James C. Horras", "Fairfield Real Estate Inc."],
                       "IowaWildlife06-232.pdf"                                                    : ["Richard Waite", "Iowa Wildlilfe Habitat Services"],
                       "KleinA07-006.pdf"                                                          : ["George W. Klein", "Klein Agency LTD."],
                       "KowalTipton05-101.pdf"                                                     : ["Michael William Tipton a/k/a Michael Edward Kowal"],
                       "KubitzA07-017.pdf"                                                         : ["Sharon L. Kubitz", "Ewing Real Estate."],
                       "McAdams07-102.pdf"                                                         : ["Donna S. McAdams"],
                       "McGuireA06-017.pdf"                                                        : ["James J. McGuire", "McGuire Auction Company Inc."],
                       "Millard-Walters07-053.pdf"                                                 : ["Shari Millard-Walters", "Iowa Realty"],
                       "Moseley07-101.pdf"                                                         : ["Karen C. Moseley"],
                       "ParkerA06-024.pdf"                                                         : ["Christopher H. Parker", "AFAB, Inc."],
                       "Peterson04-157.pdf"                                                        : ["STEVEN C. PETERSON", "SCP, Inc."],
                       "Select06-179.pdf"                                                          : ["Select Realty", "Judi E. Anding"],
                       "SieperdaA07-007.pdf"                                                       : ["David G. Sieperda", "Sieperda/Foltz Insurance & Real Estate"],
                       "Spearman06-212.pdf"                                                        : ["Robert W. Spearman", "Re/Max Real Estate Concepts"],
                       "Thompson06-208.pdf"                                                        : ["John P. Thompson", "Merle L. Kopel"],
                       "Torres07-171.pdf"                                                          : ["Vhristopher Torres"],
                       "CBSHome09-147.pdf"                                                         : ["CBS Home Relocation Services, Inc."],
                       "Bassman05-058.pdf"                                                         : ["Steven B. Bassman", "BASSMAN REAL ESTATE"],
                       "Beltramea04-156.pdf"                                                       : ["JOHN BELTRAMEA", "SCP, INC."],
                       "Breding04-147.pdf"                                                         : ["JASON D. BREDING", "Coldwell Banker Mid-America"],
                       "BurkeA05-015.pdf"                                                          : ["Mark A. Burke", "M.B, INC."],
                       "CilekA05-020.pdf"                                                          : ["Michael John Click", "COLDWELL BANKER"],
                       "Dill06-054.pdf"                                                            : ["Richard M. Dill"],
                       "Doyle06-186.pdf"                                                           : ["Monica Doyle"],
                       "Eicher06-018.pdf"                                                          : ["Scott W. Eicher"],
                       "EngelA06-009.pdf"                                                          : ["Bruce R. Engel", "The Engel Agency"],
                       "FazioA05-024.pdf"                                                          : ["Leonard R. Fazio", "RE/MAX A-1 BEST RELATORS"],
                       "FIsherA05-011.pdf"                                                         : ["LINDA L. FIsher", "Buy A Home, Inc."],
                       "Gubser05-100.pdf"                                                          : ["Robert K. Gubser", "GUBSER REAL ESTATE"],
                       "GustafsonA05-016.pdf"                                                      : ["Charles F. Gustafson", "GUSTAFSON REALTY"],
                       "Hatlevig03-147.pdf"                                                        : ["DOUGLAS C. HATLEVIG", "Leonard-Wright Agency"],
                       "Hinn05-120.pdf"                                                            : ["John M. Hinn", "HINN-DOWDEN REALTY"],
                       "HoeinA06-013.pdf"                                                          : ["Eric Hoein", "Hoein Realty"],
                       "LeechA05-010.pdf"                                                          : ["KAROL S. LEECH", "Real Estate Depot"],
                       "Luft06-113.pdf"                                                            : ["Jodie Luft", "Midwest Management Company"],
                       "McManus06-106.pdf"                                                         : ["April McManus"],
                       "Meyer06-204.pdf"                                                           : ["Ann M. Meyer", "Robert Waters"],
                       "MoultonA06-001.pdf"                                                        : ["Heath Moulton", "Moulton Real Estate, Inc."],
                       "NeuwoehnerA05-017.pdf"                                                     : ["Robert Neuwoehner", "AMERICAN REALTY, INC."],
                       "SmithNicholas05-146.pdf"                                                   : ["MICHOLAS O SMITH"],
                       "Smith%2CRobertA05-025.pdf"                                                 : ["Robert L. Smith", "FOUR SEASONS REALTORS, INC."],
                       "Stabe06-013.pdf"                                                           : ["Randall Lee Stabe"],
                       "Stauss05-111.pdf"                                                          : ["Kirk Brownlee Stauss", "DOWDEN-HINN REALTY"],
                       "Sterk06-127.pdf"                                                           : ["Ronald G. Sterk", "Sibley Realty"],
                       "StewartA05-009.pdf"                                                        : ["LARRY R. StEWART", "LARRY STEWART REALTY"],
                       "Swanwick05-056.pdf"                                                        : ["Thomas C. Swanwick", "RE/MAX RIVER RELATORS"],
                       "William05-181.pdf"                                                         : ["Gary K. Willman", "Rightway Realty"],
                       "Wood05-107.pdf"                                                            : ["MATTHEW C. WOOD", "MEL FOSTER REAL ESTATE"],
                       "Zenor05-148.pdf"                                                           : ["Mark Zenor", "BHPM, INC"],
                       "Clark11-100.pdf"                                                           : ["Krista Clark", "Network Realty"],
                       "Currie10-548.pdf"                                                          : ["Jillian Currie"],
                       "DohenyA11-001.pdf"                                                         : ["Robert C. Doheny", "Next Generation Realty, Inc."],
                       "RE%2012-058.pdf"                                                           : ["JANE PAGEL", "A-1 Real Estate Services"],
                       "Waffle%2C%20Kristi%2015-019.pdf"                                           : ["Kristi L. Waffie"],
                       "Groeneweg%2C%20Craig%2015-017.pdf"                                         : ["Craig M. Groeneweg", "Team Realty Services, Inc"],
                       "Greenwood08-274.pdf"                                                       : ["Troy A. Greenwood"],
                       "Griffin08-236.pdf"                                                         : ["Scott A. Griffin"],
                       "Hillyer09-080.pdf"                                                         : ["Kim Hillyer", "J & B Investments, Inc."],
                       "Jacobsen-Smith09-119.pdf"                                                  : ["Shael Jacobsen-Smith", "CBS Home Real Estate"],
                       "KeystoneProperty09-018.pdf"                                                : ["Keystone Property Management Co."],
                       "Minikus%2CCynthia09-021.pdf"                                               : ["Cynthia Minikus"],
                       "O%27Brien-German%2CRuthanne09-110.pdf"                                     : ["Ruthanne O'Brien-German"],
                       "Redmond%2C%20David%2014-149.pdf"                                           : ["David M. Redmond"],
                       "Quazar%2015-027.pdf"                                                       : ["Quazar Capital Corporation"],
                       "Carver%2C%20Harold%2015-060.pdf"                                           : ["Farmland Real Estate & Insurance, LLC"],
                       "Ole%27s%205%20Star%2015-072.pdf"                                           : ["Ole's 5 Star Realty, LLC"],
                       "Patten%2C%20Erik%2008-282.pdf"                                             : ["Erik J. Patten"],
                       "Paulson%2C%20Michelle%20A08-007.pdf"                                       : ["Michelle R. Paulson", "Grinnell Fifth Avenue Realty"],
                       "RandallA08-014.pdf"                                                        : ["Thomas G. Randall", "Tom Randall Real Estate Team, LLC"],
                       "Robinson%2CMargieA09-016.pdf"                                              : ["Margie I. Robinson", "Century 21 The Professional Group"],
                       "Rogers%2CPamela09-084.pdf"                                                 : ["Pamela J. Rogers"],
                       "Rogness-AndersonA08-025.pdf"                                               : ["Rogness-Anderson, Inc."],
                       "Schumacher09-158.pdf"                                                      : ["Alan W. Schumacher", "Jeffrey Denzler"],
                       "SmithA08-019.pdf"                                                          : ["Glen R. Smith", "Smith Land Service Company"],
                       "Snow09-124.pdf"                                                            : ["Christina A. Snow"],
                       "Sparks-Gray%2CBonnie09-079.pdf"                                            : ["Bonnie Sparks-Gray", "Mel Foster Co. Inc."],
                       "SpencerA08-006.pdf"                                                        : ["Ralph E. Spencer III"],
                       "Spiker09-157.pdf"                                                          : ["Lindsey N. Spiker", "Re/Max of Fort Dodge, Inc."],
                       "StewartA08-012.pdf"                                                        : ["Lores L. Stewart"],
                       "Thompson%2CJohn09-051.pdf"                                                 : ["John Thompson", "Kopel Realtors & Associates"],
                       "Timmins%2CNeilA09-008.pdf"                                                 : ["Neil Timmins", "Re/Max Select"],
                       "VanZomeren09-210.pdf"                                                      : ["Elizabeth Van Zomeren"],
                       "Vormelker09-096.pdf"                                                       : ["Ana Y. Vormelker", "VonMatt Partners"],
                       "Wannamaker07-233.pdf"                                                      : ["Richard D. Wannamaker"],
                       "Witt%2CByron09-007.pdf"                                                    : ["Byron J. Witt", "Appraisal & R.R. Services of Iowa"],
                       "Hansen10-445.pdf"                                                          : ["John F. Hansen", "Century 21 Hansen Realty"],
                       "Hansen10-406.pdf"                                                          : ["Rachelle Hansen", "Camelot Realty"],
                       "Helmlinger11-248.pdf"                                                      : ["Carolyn H. Helmlinger", "Caldwell Banker Mid-America Group"],
                       "HinesA10-049.pdf"                                                          : ["Edith L. Hines"],
                       "Hooks10-516.pdf"                                                           : ["Debra A. Hooks", "Ruhl & Ruhl Real Estate"],
                       "Jones11-299.pdf"                                                           : ["Aaron C. Jones"],
                       "Larsen10-504.pdf"                                                          : ["Peggy J. Larsen"],
                       "Lass11-294.pdf"                                                            : ["Trishelle M. Lass", "Blue Water Realty"],
                       "MBA11-081.pdf"                                                             : ["MBA Hotel Brokers Iowa, Inc."],
                       "Malone11-118.pdf"                                                          : ["Patrick D. Malone"],
                       "MartinA10-045.pdf"                                                         : ["Dennis R. Martin", "Dennis Martin CPA, PC"],
                       "MerrillA10-044.pdf"                                                        : ["Debra K. Merrill", "Homestead, Inc."],
                       "Miell10-456.pdf"                                                           : ["Robert K. Miell", "Iowa Country Jail"],
                       "Miller10-418.pdf"                                                          : ["Ardith E. Miller", "Wild Kingdom Financial Service"],
                       "Mowrer10-489.pdf"                                                          : ["Mark Mowrer", "Von Matt Partners Referral"],
                       "Nagle11-025.pdf"                                                           : ["Robert F. Nagle", "Clara Helgeland"],
                       "Nerem11-177.pdf"                                                           : ["Bradley D. Nerem", "Nerem & Associates LTD"],
                       "Nowlin11-151.pdf"                                                          : ["Michelle Tabor", "David W. Morgan"],
                       "Olson10-122.pdf"                                                           : ["Michael R. Olson", "Genesis G2 Heartland Realtors"],
                       "PagelA090-036.pdf"                                                         : ["JANE PAGEL", "A-1 Real Estate Service"],
                       "Parks10-246.pdf"                                                           : ["James A. Parks", "Parks Properties"],
                       "PruntyA11-011.pdf"                                                         : ["Burton A. Prunty", "Red Haw Realty, LLC"],
                       "Realty11-085.pdf"                                                          : ["Realty 1, Inc."],
                       "RippergerA11-015.pdf"                                                      : ["Retta Ripperger"],
                       "RobinsonA11-017.pdf"                                                       : ["Margie I. Robinson", "Century 21-The Professional Group"],
                       "Rogers10-473.pdf"                                                          : ["Kelly Rogers"],
                       "Sprague11-216.pdf"                                                         : ["Hillary Sprague", "Century 21 Premiere Associates, Inc."],
                       "Tompkins11-147.pdf"                                                        : ["Lyle J. Thompkins"],
                       "Vander%20Pol10-492.pdf"                                                    : ["David Vander Pol"],
                       "Wilkison%2C%20Gary%2015-086.pdf"                                           : ["Gary L. Wilkison", "Gearhart Agency Inc."],
                       "Makohoniuk%2C%20Richard%2015-052.pdf"                                      : ["Richard D. Makohoniuk"],
                       "Johnson%2C%20Alan%2014-153.pdf"                                            : ["Alan J. Johnson"],
                       "Rash%2C%20Jack%2014-236.pdf"                                               : ["JACK K. RASH", "STAR PERFORMERS LTD."],
                       "McCulloh%2C%20Christopher%2015-127.pdf"                                    : ["Christopher S. McCulloh", "Richard K. Isaacson"],
                       "Nenow%2C%20Carmen%2015-147.pdf"                                            : ["Carmen M. Nenow", "Louis W. Hovey"],
                       "Zevenbergen%2C%20Arlin%2082-087.pdf"                                       : ["ARLIN ZEVENBERGEN"],
                       "Independent%20Brokers%2015-119.pdf"                                        : ["Independent Brokers Realty LLC"],
                       "Borel%2C%20Isaiah%2015-101.pdf"                                            : ["Isaiah J. Borel"],
                       "Ward%2C%20Stacey%2014-247.pdf"                                             : ["Stacey Ward"],
                       "A%27Hearn%2C%20Barry%2015-204.pdf"                                         : ["Barry P. A'Hearn", "Marcus & Millichap R.E. Invest. Services of Chicago, Inc."],
                       "Petzold%2C%20Andrew%2015-226.pdf"                                          : ["Andrew K. Petzold", "Epic Property Management LC"],
                       "Action%20Realty%2015-245.pdf"                                              : ["Action Realty, Inc."],
                       "15-203%20Goede%2C%20John.pdf"                                              : ["John W. Goede", "Century 21 Jacobsen Real Estate, Inc."],
                       "Herr%2C%20Rhonda%2015-009%2015-022.pdf"                                    : ["Rhonda Jo Herr", "Herr Real Estate & Auction"],
                       "Herr%2C%20Rhonda%2015-009%2015-022_0.pdf"                                  : ["Rhonda Jo Herr", "Herr Real Estate & Auction"],
                       "Hocking%2C%20Carmen%2015-344.pdf"                                          : ["Carmen Hocking"],
                       "Mott%2C%20Jennifer%2016-042.pdf"                                           : ["Randy M. Kozelka", "Randy and Kristin Kozelka Real Estate LLC"],
                       "Horwath%20Hospitality%2015-330.pdf"                                        : ["Horwath Hospitality Investment Advisors, LLC"],
                       "Fillman%2C%20Bruce%2016-141.pdf"                                           : ["Bruce E, Fillman"],
                       "Stoakes%2C%20Martin%2016-187.pdf"                                          : ["Martin P. Stoakes", "Martin Inc"],
                       "Johnson%2C%20Brandon%2016-196.pdf"                                         : ["Brandon Johnson", "Twenty/20 Real Estate"],
                       "Burke%2C%20Chianne%2013-186.pdf"                                           : ["Chianne Burke"],
                       "Optimum%20Commercial%2016-209.pdf"                                         : ["Optimum Commercial Real Estate"],
                       "Sommer%2C%20Jake%2016-284.pdf"                                             : ["Jake Sommer"],
                       "Adams%2C%20Tonya%2016-296.pdf"                                             : ["TONYA ADAMS"],
                       "Pagel%2C%20Jane%2016-262.pdf"                                              : ["Jane Pagel", "A-1 Real Estate Services, LLC"],
                       "17-108%20Schluttenhofer%2C%20Sharon.pdf"                                   : ["Sharon F. Schluttenhofer", "Downes and Associates Inc."],
                       "17-194%20Smith%2C%20Timothy.pdf"                                           : ["Timothy H. Smith"],
                       "14-018%20Sweeney%2C%20John%20P.pdf"                                        : ["John P. Sweeney", "Sweeney Real Estate & Development Co."],
                       "17-044%20Johnson%2C%20Brandon.pdf"                                         : ["Brandon Johnson", "Twenty/20 Real Estate"],
                       "17-129%2017-130%20Lass%2C%20Trishelle.pdf"                                 : ["Trishelle M. Lass"],
                       "17-129%2017-130%20Lass%2C%20Trishelle_0.pdf"                               : ["Trishelle M. Lass"],
                       "17-270%20Cooper%2C%20Michael.pdf"                                          : ["Michael R. Cooper", "GNB Insurance & Real Estate Inc"],
                       "17-045%2017-059%20Temple%2C%20Craig.pdf"                                   : ["Craig A. Temple", "EXP Realty, LLC"],
                       "17-045%2017-059%20Temple%2C%20Craig_0.pdf"                                 : ["Craig A. Temple", "EXP Realty, LLC"],
                       "17-146%20Neppl%2C%20Dennis.pdf"                                            : ["Dennis J. Neppl", "Brown 3, P.C."],
                       "17-249%2017-260%20Bostrom%2C%20Emerson.pdf"                                : ["Emerson A. Bostrom", "Certified Property Management, Inc"],
                       "18-059%20Zierath%2C%20Blake.pdf"                                           : ["Blake Zierath", "Bill Ramsey Realtors, Inc."],
                       "17-116%20Murphy%20-%20Notice%20Of%20Hearing%20Statement%20Of%20Charges.pdf": ["Joan M. Murphy"],
                       "17-257%20Eagle%20Partners.pdf"                                             : ["Eagle Partners, LLC"],
                       "18-122%20Brown%2C%20James.pdf"                                             : ["James Brown", "Brown 3, P.C."],
                       "18-034%20Gorman%2C%20Timothy%20C.pdf"                                      : ["Timothy C. Gorman"],
                       "18-202%20Feltman%20-%20Order%20To%20Release%20License%20Suspension.pdf"    : ["Karen M. Feltman"],
                       "16-369%20Pregler%2C%20Mark.pdf"                                            : ["Mark S. Pregler", "Pregler Properties, LLC"],
                       "18-151%20Lundgren%2C%20Jason%20Y.pdf"                                      : ["Jason Y. Lundgren", "Young Management Corp"],
                       "18-174%20Spindel%2C%20Terry%20L.pdf"                                       : ["Terry L. Spindel"],
                       "19-130%20Wilson%20-%20Order%20To%20Release%20License%20Suspension.pdf"     : ["Chanel L. Wilson"],
                       "19-152%20Ray%20-%20Order%20To%20Release%20License%20Suspension.pdf"        : ["Rachael Ray"],
                       "18-237%20Johnson%20-%20Order%20To%20Release%20License%20Suspension.pdf"    : ["Stacie M. Johnson"],
                       "19-147%20Erselius%2C%20Andrew.pdf"                                         : ["Andrew G. Erselius", "AE Realty, Inc."],
                       "19-215%20Green%2C%20Todd%20O.pdf"                                          : ["Todd O. Green"],
                       "19-135%20Irlbeck%2C%20Benjamin.pdf"                                        : ["Benjamin A. Irlbeck", "Twenty/20 Real Estate"],
                       "Bergen12-169.pdf"                                                          : ["BRUCE A. BERGEN"],
                       "Bermel%2C%20Carol%2014-120.pdf"                                            : ["Carol J. Bermel"],
                       "Clark04-068.pdf"                                                           : ["JOHN CLARK", "WHY USA"],
                       "Collison05-106.pdf"                                                        : ["Peter J. COllison"],
                       "Francis04-111.pdf"                                                         : ["SHIRLEY J. FRANCIS", "HYNDEN REAL ESTATE"],
                       "Leech04-051.pdf"                                                           : ["KAROL S. LEECH", "REAL ESTATE DEPOT"],
                       "MartinA04-129.pdf"                                                         : ["JEFFERY A> MARTIN", "MARTIN ENTERPROSES, INC."],
                       "RobinsonA04-039.pdf"                                                       : ["MARGIE I. ROBINSON", "CENTURY 21 THE PROFESSIONAL GROUP"],
                       "Sheffler04-115.pdf"                                                        : ["JULIA A. SHEFFLER"],
                       "Small04-101.pdf"                                                           : ["ANITA L SMALL", "WOODLAND REALTY"],
                       "Oulman%2C%20Casey%2015-054.pdf"                                            : ["Casey Oulman"],
                       "Blesz03-058.pdf"                                                           : ["DAVID M. BLESZ", "REMAX A-1 BEST REALTORS"],
                       "ButcherA04-054.pdf"                                                        : ["JOANN M. BUTCHER", "RElMAx EXECUTIVES"],
                       "Hunziker02-095.pdf"                                                        : ["DEAN E. HUNZIKER", "HUNZIKER & ASSOCIATES REALTORS"],
                       "Martin03-100.pdf"                                                          : ["BRIAN E. MARTIN", "WHY USA ADVANTAGE REALTY"],
                       "Morris91-021.pdf"                                                          : ["HELEN M. MORRIS"],
                       "Nevins03-029.pdf"                                                          : ["NANCY J. NEVINS", "RE/MAX WEST REALTY, INC"],
                       "Pavano03-077.pdf"                                                          : ["DEBBIE PAVANO", "RUHL & RUHL REAL ESTATE"],
                       "Price02-078.pdf"                                                           : ["MICHELLE R. PRICE"],
                       "Sheren02-088.pdf"                                                          : ["BRADFORD A. SHEREN", "HOMESTYLE ENTERPRISE, INC"],
                       "Stanley04-045andA04-036.pdf"                                               : ["CLARA M. STANLEY", "Stanley Real Estate"],
                       "Stanley04-045andA04-036_0.pdf"                                             : ["CLARA M. STANLEY", "Stanley Real Estate"],
                       "WoernerA04-025.pdf"                                                        : ["MARGARET A. WOERNER", "CROSSROADS, INC."],
                       "Elder02-025.pdf"                                                           : ["JOHN W. ELDER", "CENTRAL REALTY COMPANY"],
                       "Nodland02-038.pdf"                                                         : ["James T. Nodland", "NODLAND REALTY"],
                       "Phippen02-107.pdf"                                                         : ["RICK H. PHIPPEN", "MT. PLEASENT CORRECTIONAL FACILITY"],
                       "Saar02-011.pdf"                                                            : ["DIANE M. SAAR", "THE PROPERTY  BROKERS, LTD"],
                       "Siglin02-026.pdf"                                                          : ["WILLIAM P. SIGLIN", "GODWIN REALTY INC"],
                       "Bougher01-051.pdf"                                                         : ["STEVE L. BOUGHER", "GODWIN REALTY INC."],
                       "Franich01-034.pdf"                                                         : ["GREGORY O. FRANICH", "SYMMETRY MORTGAGE CORP."],
                       "Larson00-130.pdf"                                                          : ["SANDRA K. LARSON", "RE/MAX REAL ESTATE CENTER"],
                       "Schulte02-010.pdf"                                                         : ["EUGENE A. SCHULTE", "SCHULTE REAL ESTATE"],
                       "Hale00-153.pdf"                                                            : ["LARRY J. HALE", "HALE & HALE LTD."],
                       "Keller01-011.pdf"                                                          : ["JAMES D. KELLER", "COLDWELL BANKER MID-AMERICA"],
                       "Lamb01-031.pdf"                                                            : ["R. CRAIG LAMB", "RE/MAX AXTION REALTY"],
                       "Lindman01-044.pdf"                                                         : ["BRETT LINDMAN", "IOWA REALTY Co. INC."],
                       "Vaith00-149.pdf"                                                           : ["LOLA VAITH", "CENTURY 21 PREFERRED"],
                       "Bock99-095.pdf"                                                            : ["STEPHEN BOCK", "REMAX REAL ESTATE CENTER"],
                       "HazellA99-003.pdf"                                                         : ["SCOTT J HAZELL"],
                       "JohnsonBryan99-108.pdf"                                                    : ["BRYAN JOHNSON", "IOWA REALTY Co. INC."],
                       "JohnsonA99-004.pdf"                                                        : ["GREGORY K JOHNSON"],
                       "Lehman99-094.pdf"                                                          : ["DOUGLAS A LEHMAN"],
                       "Perera00-094.pdf"                                                          : ["STEVEN J. PERERA"],
                       "Shelton99-066.pdf"                                                         : ["LELAND C SHELTON"],
                       "Brown99-021.pdf"                                                           : ["DEBRA M. BROWN", "Midwest Realtors"],
                       "Christophersen98-036.pdf"                                                  : ["ALFRED F. CHRISTOPHERSEN", "Iowa Realty Company, Inc."],
                       "Dougherty97-102.pdf"                                                       : ["MARGARET A. DOUGHERTY"],
                       "JohnsonA98-061.pdf"                                                        : ["FRED M. JOHNSON"],
                       "MartinRealEstateA99-002.pdf"                                               : ["AL MARTIN REAL ESTATE, INC", "JAMES P. KOSMAN"],
                       "Rees97-113.pdf"                                                            : ["MALINDA J. REES"],
                       "Stephens98-057.pdf"                                                        : ["DENNIS H. STEPHENS"],
                       "Heher97-110.pdf"                                                           : ["JAMES J. HEHER", "First REalty LTD"],
                       "Hogan-Merrell98-023%20and%20024.pdf"                                       : ["TERRI LYNN HOGAN-MERRELL", "SC P Inc."],
                       "Hogan-Merrell98-023%20and%20024_0.pdf"                                     : ["TERRI LYNN HOGAN-MERRELL", "SC P Inc."],
                       "KrugerA97-095.pdf"                                                         : ["ALLEN L. KRUGER", "Sportsmans Realty"],
                       "Logan98-002.pdf"                                                           : ["HOWARD M. LOGAN", "Howard Logan Real Estate"],
                       "Morgan98-011.pdf"                                                          : ["GARY W. MORGAN", "American Colonial Realty, Inc."],
                       "Anderson97-008.pdf"                                                        : ["DONNA J. ANDERSON", "Central Iowa Realty"],
                       "Bonacci97-035.pdf"                                                         : ["LOUIS J. BONACCI"],
                       "Bryant97-008.pdf"                                                          : ["SANDRA JEAN BRYANT", "Central Iowa Realty"],
                       "GarnerA97-066.pdf"                                                         : ["JOHN F GARNER"],
                       "Leist96-083.pdf"                                                           : ["DOUGLAS J. LEIST"],
                       "McCoyA96-175.pdf"                                                          : ["LEON G. Mc COY", "Clair Clark Real Estate"],
                       "ThomasA96-111.pdf"                                                         : ["NORMAN J. THOMAS", "Thomas Realty & Property Management"],
                       "Brayton95-054.pdf"                                                         : ["DEBRA BRAYTON"],
                       "Burke95-087.pdf"                                                           : ["FRANCES M. BURKE"],
                       "GoodhueA95-098.pdf"                                                        : ["DALE C. GOODHUE"],
                       "Knudsen92-029.pdf"                                                         : ["DONALD E. KNUDSEN"],
                       "Lehman96-006.pdf"                                                          : ["DOUGLAS A. LEHMAN"],
                       "LoganA96-093.pdf"                                                          : ["HOWARD M. LOGAN", "Howard Logan Real Estate"],
                       "MurphyA96-101.pdf"                                                         : ["DARRELL D. MURPHY", "Murphy Real Estate"],
                       "Wubbena95-040.pdf"                                                         : ["DONALD R. WUBBENA"],
                       "CarterA94-034.pdf"                                                         : ["LOIS J. CARTER"],
                       "JohnsonA94-044.pdf"                                                        : ["JOHNSON   SONS, INC.", "DARYL W. JOHNSON"],
                       "JohnsonA94-046.pdf"                                                        : ["JOHNSON AND SONS, INC.", "E. ALAN JOHNSON"],
                       "RuelleA94-055.pdf"                                                         : ["PATRICK J. RUELLE"],
                       "TilleyA94-045.pdf"                                                         : ["JOHNSON   SONS, INC.", "RONALD D. TILLEY"],
                       "Anderson92-093.pdf"                                                        : ["LAVERENE R. ANDERSON", "Coldwell Banker Mid America Group"],
                       "Hunter93-020.pdf"                                                          : ["MARK L. HUNTER"],
                       "Karsten94-055.pdf"                                                         : ["LARRY G. KARSTEN", "Larry Karsten Real Estate Inc."],
                       "Bader92-007.pdf"                                                           : ["Dodd R. Bader"],
                       "Clarey90-073.pdf"                                                          : ["JOHN E. CLAREY"],
                       "Moessner91-020.pdf"                                                        : ["SCOTT BYERS", "VIVIAN MOESSNER"],
                       "Brewer91-034.pdf"                                                          : ["Audrey L. Brewer"],
                       "Kelley%2091-025.pdf"                                                       : ["ROnald L. Kelley"],
                       "Kline92-021.pdf"                                                           : ["LYLE E. KLINE"],
                       "McElmeel91-036.pdf"                                                        : ["John R. McElmeel"],
                       "Sires90-058.pdf"                                                           : ["WILLIAM L. SIRES"],
                       "Smith91-028.pdf"                                                           : ["Alan E. Smith"],
                       "Weber91-019.pdf"                                                           : ["Weber-Carmer, Inc.", "Wayne Weber"],
                       "Zegarac90-078.pdf"                                                         : ["Ralph Zegarac"],
                       "Duffy11-258.pdf"                                                           : ["Timothy P. Duffy", "MBA Hotel Brokers Iowa, Inc"],
                       "ExecutiveProperty10-455.pdf"                                               : ["Executive Property Management, LLC"],
                       "Goodwin11-024.pdf"                                                         : ["Merie J Goodwin"],
                       "Bisenius90-016.pdf"                                                        : ["JAMES W. BISENIUS", "JAMES J. BISENIUS"],
                       "Brandstatter90-065.pdf"                                                    : ["Rex E. Brandstatter"],
                       "BorschukandUnger90-067.pdf"                                                : ["Michael R. Borschuk", "Cyndi A. Unger"],
                       "Downes90-066.pdf"                                                          : ["Gregory J. Dovnes"],
                       "Ferden90-070.pdf"                                                          : ["Alan B. Ferden"],
                       "Hinn90-047.pdf"                                                            : ["Kathleen D. Hinn", "Hinn, Inc."],
                       "Hulshof90-023.pdf"                                                         : ["JOHN A. HULSHOF"],
                       "Martin90-037.pdf"                                                          : ["Martin Inc,", "Valentia K. Martin"],
                       "Rekward90-033.pdf"                                                         : ["Lockard Realty Company", "Mildred Rekward"],
                       "Ross90-072.pdf"                                                            : ["Jerry D. Ross", "Coder Real Estate Inc."],
                       "Rudolph90-036.pdf"                                                         : ["Golden Cities - Paup Realty Corp.", "Clifford C. Rudolph"],
                       "Seehusen90-075.pdf"                                                        : ["Steven R. Seehusen"],
                       "Harms90-038.pdf"                                                           : ["Wendell G. Harms"],
                       "Harms90-038_0.pdf"                                                         : ["Wendell G. Harms"],
                       "Harms90-038_1.pdf"                                                         : ["Wendell G. Harms"],
                       "Kelley90-002.pdf"                                                          : ["Ronald L. Kelley"],
                       "Renoe90-014.pdf"                                                           : ["Van U. Renoe"],
                       "Shipler90-046.pdf"                                                         : ["HAROLD F. SHIPLER"],
                       "Associated88-058.pdf"                                                      : ["Associated Counselors, Inc."],
                       "Bailey88-082.pdf"                                                          : ["Richard P. Bailey"],
                       "Becker88-033.pdf"                                                          : ["Fedrick A. Becker"],
                       "Century2188-052.pdf"                                                       : ["Century 21 United Realty"],
                       "Chandler88-010.pdf"                                                        : ["KENNETH D. CHANDLER"],
                       "Collins89-060.pdf"                                                         : ["Maurice L. Collins"],
                       "Conradi89-009.pdf"                                                         : ["MERLE R. CONRADI"],
                       "Cooper87-062.pdf"                                                          : ["PHYLLIS C. COOPER", "C. J. COOPER"],
                       "Cooper87-062_0.pdf"                                                        : ["PHYLLIS C. COOPER", "C. J. COOPER"],
                       "DeYager87-050.pdf"                                                         : ["Albert DeYager"],
                       "Knudsen88-068.pdf"                                                         : ["Don Knudsen Realty, Inc.", "Donald E. Knudsen"],
                       "LongRicklefsERA88-009.pdf"                                                 : ["Roger Long", "Thomas Ricklefs", "ERA-Algona Realty"],
                       "Feller89-010.pdf"                                                          : ["James L. Feller"],
                       "FirstRealty87-076B.pdf"                                                    : ["First Realty, Ltd"],
                       "Fjelland88-109.pdf"                                                        : ["Kenneth W. Fjelland", "Fjelland Real Estate & Farm Management Ltd"],
                       "Grimm89-012.pdf"                                                           : ["Chris D. Grimm"],
                       "Groves88-093.pdf"                                                          : ["Harlan M. Groves"],
                       "Growthland88-062.pdf"                                                      : ["Growthland Realty, Inc"],
                       "Harbaugh%20and%20Winninger%2083-039.pdf"                                   : ["STEVEN R. HARBAUGH", "MARK L. WINNINGER"],
                       "HarryCrowlCo89-058.pdf"                                                    : ["Harry C. Crowl Company"],
                       "Heberer89-031.pdf"                                                         : ["ROBERT E. HEBERER", "MELVIN E. LEE", "BOLICK REALTY AND CONSTRUCTION FARM"],
                       "Hegstrom89-059.pdf"                                                        : ["William E. Hegstrom"],
                       "Henkenius88-057.pdf"                                                       : ["Donald R. Henkenius"],
                       "Hill87-076.pdf"                                                            : ["Edward C. Hill"],
                       "Holmquist88-064.pdf"                                                       : ["Dorothea M. Holmquist"],
                       "Joens87-010%2C87-024%2C88-02%2C89-023.pdf"                                 : ["Frank P. Joens, Jr."],
                       "Joens87-010%2C87-024%2C88-02%2C89-023_0.pdf"                               : ["Frank P. Joens, Jr."],
                       "Joens87-010%2C87-024%2C88-02%2C89-023_1.pdf"                               : ["Frank P. Joens, Jr."],
                       "Joens87-010%2C87-024%2C88-02%2C89-023_2.pdf"                               : ["Frank P. Joens, Jr."],
                       "Jones87-064.pdf"                                                           : ["Darlene Jones"],
                       "Kopp89-003.pdf"                                                            : ["GEORGE M. KOPP"],
                       "KruseMiller88-056.pdf"                                                     : ["Gary L. Kruse", "Barbara S. Miller"],
                       "Lamphier87-049.pdf"                                                        : ["Michael A. Lamphier"],
                       "Lippons85-093.pdf"                                                         : ["DENNIS R. LIPPON"],
                       "LongRicklefsERA88-009_0.pdf"                                               : ["Roger Long", "Thomas Ricklefs", "ERA-Algona Realty"],
                       "Longman88-006.pdf"                                                         : ["Glen R. Longman"],
                       "KruseMiller88-056_0.pdf"                                                   : ["Gary L. Kruse", "Barbara S. Miller"],
                       "Oldham88-090.pdf"                                                          : ["Monte B. Oldham"],
                       "Oliphant89-018.pdf"                                                        : ["VERN C. OLIPHANT"],
                       "Powers88-014.pdf"                                                          : ["SARAH POWERS"],
                       "LongRicklefsERA88-009_1.pdf"                                               : ["ROger Long", "Thomas Ricklefs", "ERA-ALgona Realty"],
                       "Rolf88-026.pdf"                                                            : ["Raymond F. Rolf", "Betty M. Rolf", "Preferred Triad Realty"],
                       "Rolf88-026_0.pdf"                                                          : ["Raymond F. Rolf", "Betty M. Rolf", "Preferred Triad Realty"],
                       "Shelledy89-042.pdf"                                                        : ["John G. Shelledy"],
                       "Shipler88-079.pdf"                                                         : ["HAROLD SHIPLER"],
                       "Sires89-019.pdf"                                                           : ["WILLIAM L. SIRES"],
                       "Spohr87-077.pdf"                                                           : ["Steve Spohr"],
                       "Strong87-066.pdf"                                                          : ["Wayne C. Strong"],
                       "Sweeney87-038.pdf"                                                         : ["JOHN J. SWEENEY"],
                       "Tucker88-055.pdf"                                                          : ["Charles E. Tucker, Jr."],
                       "Vilmont88-017.pdf"                                                         : ["Key E. Vilmont"],
                       "Vrieze87-056.pdf"                                                          : ["TERRY A. VRIEZE"],
                       "Vrieze89-063.pdf"                                                          : ["TERRY A. VRIEZE"],
                       "Willie88-034.pdf"                                                          : ["KEY-STONE, LTD.", "RICHARD H. WILLIE"],
                       "Harbaugh%20and%20Winninger%2083-039_0.pdf"                                 : ["STEVEN R. HARABAUGH", "MARK L. WINNINGER"],
                       "Woodsmall88-012.pdf"                                                       : ["JAMES W. WOODSMALL"],
                       "Dagnon08-225.pdf"                                                          : ["Michael J. Dagnon"],
                       "El-Orm09-190.pdf"                                                          : ["Milad T. EI-Orm"],
                       "Fishell09-178.pdf"                                                         : ["Aimee Jo Fishell", "Classic Realty, Inc."],
                       "Hartin07-127.pdf"                                                          : ["Otheda Hartin", "Success 100 Real Estate"],
                       "18-162%20Pagliai%20-%20Decision%20And%20Order.pdf"                         : ["Kurt A. Pagliai", "BlackAcre Realty LLC"],
                       "84-031.pdf"                                                                : ["JAMES H. BIEBEE"],
                       "84-063.pdf"                                                                : ["CONNIE BROESDER", "SHIRLEY FELICE"],
                       "84-063_0.pdf"                                                              : ["CONNIE BROESDER", "SHIRLEY FELICE"],
                       "81-06.pdf"                                                                 : ["HAROLD KELDERMAN", "JAKE W. BRAM"],
                       "81-06_0.pdf"                                                               : ["HAROLD KELDERMAN", "JAKE W. BRAM"],
                       "82-11.pdf"                                                                 : ["HAWKEY FARM SERVICE AND REALTY INC.", "FRANCIS C. PRENDERGAST", "MARCELLA M. PRENDERGAST"],
                       "82-11_0.pdf"                                                               : ["HAWKEY FARM SERVICE AND REALTY INC.", "FRANCIS C. PRENDERGAST", "MARCELLA M. PRENDERGAST"],
                       "82-11_1.pdf"                                                               : ["HAWKEY FARM SERVICE AND REALTY INC.", "FRANCIS C. PRENDERGAST", "MARCELLA M. PRENDERGAST"],
                       "AdamsA10-012.pdf"                                                          : ["Gregory J. Adams", "Re/Max Advantage Realty of Dubuque"],
                       "Scotty%27s%20Auction%2012-325.pdf"                                         : ["Scotty's Auction Service"],
                       "Snyder%2C%20Stephanie%2014-050.pdf"                                        : ["Stephanie Snyder"],
                       "Swinney-Riesberg%2C%20Carrie%2014-045.pdf"                                 : ["Carrie Swinney-Riesberg"],
                       "Firkins11-342.pdf"                                                         : ["Jeremy P. Firkins"],
                       "Sansone%2C%20Timothy%2014-069_0.pdf"                                       : ["Timothy Sansone"],
                       "Steffes12-079.pdf"                                                         : ["Nick Steffes"],
                       "Escarza%2C%20Gloria%2014-248.pdf"                                          : ["Gloria Escarza", "Blank and McCune, The Real Estate Co. LLC"],
                       "Archer06-092.pdf"                                                          : ["George S. Archer"],
                       "Tang06-088.pdf"                                                            : ["David Tang", "Godwin Realty"],
                       "Godwin03-002.pdf"                                                          : ["Donald W. Godwin, Jr.", "GODWIN REALTY INC."],
                       "Klein99-040.pdf"                                                           : ["JAMES A. KLEIN"],
                       "Mitchell99-023.pdf"                                                        : ["JAMES L MITCHELL", "IOWA REALTY CO INC"],
                       "Klein97-043.pdf"                                                           : ["JAMES A. KLEIN"],
                       "BrandstatterA94-037.pdf"                                                   : ["REX BRANDSTATTER"],
                       "Stephens13-100.pdf"                                                        : ["Steve L. Stephens"],
                       "Johnson88-102.pdf"                                                         : [" Carol A. Johnson"],
                       "Beerends%2C%20Gregory%2016-206.pdf"                                        : [" Gregory M. Beerends"],
                       "Zach%2016-039.pdf"                                                         : ["Bradley J. Zach"],
                       "Bobb%2C%20Michele%2016-302.pdf"                                            : ["Michele M. Bobb"],
                       "17-222%20Billings%2C%20Pamela.pdf"                                         : ["Pamela C. Billings"],
                       "17-195%20Olson-Kiene.pdf"                                                  : ["Anita M. Olson-Kiene"],
                       "17-190%20Wennerstrom%2C%20Matthew.pdf"                                     : ["Matthew A. Wennerstrom"],
                       "17-278%20Holtman%20-%20Order%20To%20Release%20Suspension.pdf"              : ["Zachary Holtman"],
                       "17-251%20Stults%2C%20Amy.pdf"                                              : ["Amy N. Stults"],
                       "19-133%20Swanson%2C%20Jeremy.pdf"                                          : ["Jeremy Swanson"],
                       "19-171%20Stahler%2C%20Michael%20P.pdf"                                     : ["Micael P. Stahler"],
                       "19-145%20Forret-Munoz%2C%20KayLa.pdf"                                      : ["KayLa Forret-Munoz", "QCA Progressive Partners, LLC"],
                       "Smejkal06-062.pdf"                                                         : ["Kelly L. Smejkal"],
                       "Brayton07-032.pdf"                                                         : ["Marcia E. Brayton"],
                       "Culp10-057.pdf"                                                            : ["Laurie Culp", "Signature Resources, Inc."],
                       "Winninger11-343.pdf"                                                       : ["Theodore J. Winninger", "Harbaugh Winninger Realty, Inc"],
                       "Wetzel%2CCraig09-020.pdf"                                                  : ["Craig A. Wetzel", "First Realty LTD"],
                       "Collier03-018.pdf"                                                         : ["PHYLLIS A. COLLIER", "CENTRAL IOWA REALTY"],
                       "Heath91-032and92-019.pdf"                                                  : ["NORMAN G. HEATH", "IOWALAND REALTY CORPORATION"],
                       "Heath91-032and92-019_0.pdf"                                                : ["NORMAN G. HEATH", "IOWALAND REALTY CORPORATION"],
                       "Seng11-260.pdf"                                                            : ["Nicole L. Seng"],
                       "15-327%20Scarlett%2C%20Patricia.pdf"                                       : ["Patricia A. Scarlett"],
                       "Bray%2C%20Denice%2016-250.pdf"                                             : ["Denice F. Bray"],
                       "SpartzA04-089.pdf"                                                         : ["JOSEPH J. SPARTZ"],
                       "Mathis12-391.pdf"                                                          : ["Sharon K. Mathis"],
                       "Barner%2C%20Barton%2014-023.pdf"                                           : ["Barton S. Barner"],
                       "Rodgers08-105.pdf"                                                         : ["Ronald D. Rodgers", "RE/MAX Farm and Home Realtors"],
                       "Knight07-129.pdf"                                                          : ["Michael W. Knight"],
                       "Bugbee%2C%20Adam%2016-083.pdf"                                             : ["Adam Bugbee"],
                       "Bussanmas93-108.pdf"                                                       : ["Jerry J. Bussanmas", "First Realty a/k/a Better Homes and Garden"],
                       "Tagatz93-128.pdf"                                                          : ["ROBERT E. TAGATZ", "First Realty a/k/a Better Homes and Garden"],
                       "WisconsinMotel09-246_0.pdf"                                                : ["Wisconsin Motel a/k/a Resort Realty"],
                       "Szegda%2C%20Austin%2014-123.pdf"                                           : ["Austin M. Szegda"],
                       "WisconsinMotel09-246.pdf"                                                  : ["Wisconsin Motel a/k/a Resort Realty"],
                       "17-151%20Slater%2C%20Jill.pdf"                                             : ["Jill Slater", "Century 21 Cornells-Simpson Inc"]]

        // Checking if entry exists in map
        if (statics.keySet().contains(key)) {
            def names = statics.get(key)
            return names
        }
        return null
    }

    def getStaticAddress(def url) {
        def key = url.replaceAll(/^.*\//, "")
        def statics = ["Smith13-049.pdf"                                                       : ["2346 260th Avenue; Box 243, DeWitt, IA 52742"],
                       "Winninger11-343.pdf"                                                   : ["112 W. Bremer, Waverly, Iowa 50677"],
                       "Avalon10-179.pdf"                                                      : ["725 Mormon Trek Blvd, Ceder Rapids, Iowa 52246"],
                       "B%26DHoldings10-159.pdf"                                               : ["1211 Foster Road, Iowa City, Iowa 5245"],
                       "20-067%20Sires%2C%20Vennessa%20S.pdf"                                  : ["663 39th Street, Des Moines, IA 50312"],
                       "Burns10-212.pdf"                                                       : ["3501 Westown Parkway, West Des Moines, Iowa"],
                       "Culp10-057.pdf"                                                        : ["306 S. 16th Street, Ames, Iowa 50010"],
                       "Hansel10-288.pdf"                                                      : ["1315 Zenith Drive, Sioux City, Iowa 51103"],
                       "Tribbett09-351.pdf"                                                    : ["3128 Mary Noel Avenue, Bettendorf, Iowa 52722"],
                       "Dagnon08-225.pdf"                                                      : ["RR1, Desoto, Wisconsin 54624"],
                       "Bucheli07-308.pdf"                                                     : ["917 North Walnut Street, Storm Lake, Iowa 50588"],
                       "Steffes12-079.pdf"                                                     : ["340 W Superior #709, Chicago, IL 60654"],
                       "Harmon08-102.pdf"                                                      : ["701 Climer Street, Muscatine, Iowa"],
                       "Nevins03-029.pdf"                                                      : ["232 HIGHWAY 6, WAUKEE, IA 50263"],
                       "Navarro08-201.pdf"                                                     : ["3313 Terrace Drive, Cedar Falls, Iowa 0613-6069"],
                       "Feller%2C%20Mark%2014-113.pdf"                                         : ["622 Parkview Drive, PO Box 652, Dension, IA"],
                       "Buck08-128.pdf"                                                        : ["125 East Zeller, North Liberty, Iowa 52317"],
                       "Knudsen92-029.pdf"                                                     : ["201 S. Commercial, Eagle Grove, Iowa"],
                       "Hunter93-020.pdf"                                                      : ["P O Box 23 , Council Bluffs, Iowa"],
                       "Karsten94-055.pdf"                                                     : ["224 1st Street East PO Box 507, Independence, Iowa 50644"],
                       "Byers93-001.pdf"                                                       : ["211 1st Avenue S.E., Cedar Rapids, IA"],
                       "Grier09-348.pdf"                                                       : ["1276 L Road NW, Swisher, Iowa 52338"],
                       "Wetzel%2CCraig09-020.pdf"                                              : ["5500 Westown Parkway, Ste 120, West Des Moines, Iowa 50266"],
                       "Walters%2C%20Michael%2016-195.pdf"                                     : ["128 3rd Street East, Wabasha, MN"],
                       "Schulte02-010.pdf"                                                     : ["219 9th STREET, LAKE VIEW, IA 51450"],
                       "Beider13-250.pdf"                                                      : ["1207 Hohlfelder Road, Glencoe, IL 60022"],
                       "Kelley%2091-025.pdf"                                                   : ["2750 First Avenue NE, Cedar Rapids, Iowa"],
                       "Bittner13-077.pdf"                                                     : ["409 NE Stoneridge Drive, Ankeny, IA 50021"],
                       "Barner%2C%20Barton%2014-023.pdf"                                       : ["108 Heather Lane, Springville, IA 52336"],
                       "Snyder%2C%20Stephanie%2014-050.pdf"                                    : ["2110 NW Lake Side Ct., Ankeny, IA 50023"],
                       "Swinney-Riesberg%2C%20Carrie%2014-045.pdf"                             : ["25757 Kitty Hawk Ave., Carroll, IA 51401"],
                       "Firkins11-342.pdf"                                                     : ["108 N. Superior Street, Emmetsburg, IA 50536"],
                       "Halblom12-113.pdf"                                                     : ["715 5th Avenue, Grinnell, Iowa 50112"],
                       "BuddyLeeLLC10-013.pdf"                                                 : ["8191 Birchwood Ct., Johnson, Iowa 50131"],
                       "Paulson10-417.pdf"                                                     : ["807-4th Avenue, Grinnell, Iowa 50112"],
                       "BertlingA07-023.pdf"                                                   : ["IOI_1st Street, Mount Vernon, Iowa 52314"],
                       "Von%20Hollen10-509.pdf"                                                : ["34345 Asbury Road, Dubuque, Iowa 52002"],
                       "Brayton07-032.pdf"                                                     : ["8194 Cobblestone, Urbandale, Iowa 50332"],
                       "Bassman05-058.pdf"                                                     : ["6500 HICKMAN ROAD, WINDSOR HEIGHTS, IOWA 50322"],
                       "Smejkal06-062.pdf"                                                     : ["1014 N.W. Rockcrest Rd, Ankeny, Iowa 50021"],
                       "Collier03-018.pdf"                                                     : ["102 E. MAIN STREET, STATE CENTER, IA 50247"],
                       "Godwin03-002.pdf"                                                      : ["6900 UNIVERSITY, DES MOINES, IA 503111541"],
                       "Martin03-100.pdf"                                                      : ["3915 CENTER POINT RD NE, CEDAR RAPIDS, IA 524026404"],
                       "JohnsonA98-061.pdf"                                                    : ["P O Box 252, Shellsburg, Iowa 52332"],
                       "Logan98-002.pdf"                                                       : ["First Trust & Savings Bank, Moville, Iowa 51039"],
                       "LoganA96-093.pdf"                                                      : ["First Trust & Savings Bank, Moville, Iowa 51039"],
                       "Heath91-032and92-019.pdf"                                              : ["North Front & Columbus Str., Humeston, IA 50213"],
                       "Heath91-032and92-019_0.pdf"                                            : ["North Front & Columbus Str., Humeston, IA 50213"],
                       "Spiker09-157.pdf"                                                      : ["Fort Dodge, Iowa 50501"],
                       "Lass11-294.pdf"                                                        : ["Spirit Lake, Iowa 51360"],
                       "MartinA10-045.pdf"                                                     : ["10 East Charles Street, Oelwein, Iowa 50662"],
                       "Seng11-260.pdf"                                                        : ["115 Lovejoy Ave., Waterloo, IA 50701"],
                       "Makohoniuk%2C%20Richard%2015-052.pdf"                                  : ["785 Walnut Ridge Drive, Waukee, IA 50263"],
                       "15-327%20Scarlett%2C%20Patricia.pdf"                                   : ["1108 Beaverlake Blvd, Plattsmouth, NE 68048"],
                       "Hesseltine%2C%20Johanna%2016-090.pdf"                                  : ["4613 E Valdez Drive, Des Moines, IA"],
                       "Beerends%2C%20Gregory%2016-206.pdf"                                    : ["4108 Welker Avenue, Des Moines, IA 50312"],
                       "Bobb%2C%20Michele%2016-302.pdf"                                        : ["518 Bruce Ave., Milan, IL 61264"],
                       "17-222%20Billings%2C%20Pamela.pdf"                                     : ["611 Avenue D, West Point, IA 52656"],
                       "17-278%20Holtman%20-%20Order%20To%20Release%20Suspension.pdf"          : ["2192 Vine Street, West Des Moines, IA 50265"],
                       "17-251%20Stults%2C%20Amy.pdf"                                          : ["1703 W. Spencer Street, Creston, IA 50801"],
                       "18-202%20Feltman%20-%20Order%20To%20Release%20License%20Suspension.pdf": ["1706 Hughes Circle SW, Cedar Rapids, IA 52404"],
                       "MurphyA96-101.pdf"                                                     : ["1st and Main Streets, Coin, Iowa 51636"],
                       "19-133%20Swanson%2C%20Jeremy.pdf"                                      : ["11 Hilltop Drive, Coal Valley, IL 61240"],
                       "19-171%20Stahler%2C%20Michael%20P.pdf"                                 : ["3880 Valley View Drive, Bettendorf, IA 52722"],
                       "Schuil13-029_0008.pdf"                                                 : ["5020 W. Mineral King Ave., Visalia, CA 93291"],
                       "Hoffschneider13-073.pdf"                                               : ["427 W. Beal Street; P.O. Box 85150, Lincoln, NE 68501"],
                       "Haushahn13-093.pdf"                                                    : ["701 Broad Street, Suite 2; P.O. Box 443, Keosauqua, IA 52565"],
                       "Malone12-094_0002.pdf"                                                 : ["Rt 2; Box 79, Kahoka, MO 63445"],
                       "O%27Byrne12-422.pdf"                                                   : ["72894 220th Street, Albert Lea, MN 56007"],
                       "Baier12-370_01.pdf"                                                    : ["2131-200th Street, Greenfield, Iowa 50849-9695"],
                       "Perry%2C%20Philip%2013-274.pdf"                                        : ["9200 Andematt Drive, Suite A, Lincoln, NE 68526"],
                       "Scotty%27s%20Auction%2012-325.pdf"                                     : ["36512 Hwy PP, Macon, MO 63552"],
                       "Collins12-006.pdf"                                                     : ["1225 E. River Drive, #110, Davenport, Iowa 52803"],
                       "Fraise12-218.pdf"                                                      : ["306 E Main St; PO Box 8, New London, Iowa 52645"],
                       "Sansone%2C%20Timothy%2014-069_0.pdf"                                   : ["120 S. Central Ave., Suite 500, St. Louis, MO 63105"],
                       "Weigel12-130.pdf"                                                      : ["800 Guthrie St, Adair, Iowa 50002"],
                       "Padavich09-262.pdf"                                                    : ["4800 University Ave., Cedar Falls, Iowa 50613"],
                       "Anderson08-222.pdf"                                                    : ["131 South Clark Street. P.O. Box 246, Forest City, Iowa 50436-0246"],
                       "Ford07-082.pdf"                                                        : ["Altoona, Iowa"],
                       "McAdams07-102.pdf"                                                     : ["Delta, IA. 52550"],
                       "Thompson06-208.pdf"                                                    : ["585-8th Ave, Marion, IA 52302"],
                       "Hatlevig03-147.pdf"                                                    : ["703 S. Oak Street, Iowa Falls, IA 50126"],
                       "SollenbargerA95-079.pdf"                                               : ["4015 Cottage Grove Avenue, Des Moines, Iowa 50311-3509"],
                       "Winsor%2C%20Lisa%2015-350.pdf"                                         : ["5779 22nd Avenue Drive, Vinton, IA"],
                       "Burd%2C%20Sandra%2016-220.pdf"                                         : ["5315 Mission Woods Ter, Mission Woods, KS"],
                       "Bray%2C%20Denice%2016-250.pdf"                                         : ["9647 Gilles Rd, La Vista, NE 68128"],
                       "Julsen%2C%20Michaela%2016-244.pdf"                                     : ["2530 Jefferson, Bellevue, NE"],
                       "BorschukandUnger90-067.pdf"                                            : ["621 16th Street, Sioux City, IA 51105"],
                       "Remsberg07-281.pdf"                                                    : ["320-36th, West Des Moines, Iowa 50265"],
                       "Small04-101.pdf"                                                       : ["2018 INDIAN HILLS DRIVE, SIOUX CITY,IA 51104-1602"],
                       "Jones11-299.pdf"                                                       : ["PO Box 701, Okoboji, IA 51355"],
                       "Black11-236.pdf"                                                       : ["111 N. Lincoln, Troy, Missouri 63379"],
                       "Jackson09-191.pdf"                                                     : ["5801 Oakwood Drive, Lincoln, Nebraska 68516"],
                       "ReidProperties09-209.pdf"                                              : ["1500 S. 70th Street, Ste. 201, Lincoln, Nebraska 68506"],
                       "RealtyLinc09-005.pdf"                                                  : ["6101 Havelock Ave., Lincoln, Nebraska 68507"],
                       "RealtyLinc11-188.pdf"                                                  : ["2248 Sewell Street, Lincoln, Nebraska 68502"],
                       "UnitedFarm11-152.pdf"                                                  : ["1248 O Street, #1000, P.O. Box 85506, Lincoln, Nebraska 68501"],
                       "Foutch%2C%20Rodney%2015-251.pdf"                                       : ["410 Lincoln Hwy, Missouri Valley, IA 51555"],
                       "18-232%20Gomez%2C%20Jessie%20R.pdf"                                    : ["1618 Lincoln Blvd, Muscatine, IA 52761"],
                       "Bugbee%2C%20Adam%2016-083.pdf"                                         : ["13389 200th Street, Dawson, IA 50066"],
                       "Szegda%2C%20Austin%2014-123.pdf"                                       : ["3484 29th Street, Bettendorf, IA 52722"],
                       "Kopp89-003.pdf"                                                        : ["Iowa"],
                       "Bisenius90-016.pdf"                                                    : ["2207 17th Street, Emmetsburg, IA 50536"],
                       "Phippen02-107.pdf"                                                     : ["1200 E. WASHINGTON, MT. PLEASENT, IA 52641-1804"],
                       "Nowlin11-151.pdf"                                                      : ["Highway 69 East, Lamoni, Iowa 50140"],
                       "Mosher10-330.pdf"                                                      : ["202 East Rochester Street, Ottumwa, Iowa 52501"],
                       "Garrett03-142.pdf"                                                     : ["217 E WASHINGTON STREET, PO BOX 301, CLARINDA, IA 516320302"],
                       "Duffy11-258.pdf"                                                       : ["102 S. 2nd Street, Fairfield, Iowa 5556"]]

        // Checking if entry exists in map
        if (statics.keySet().contains(key)) {
            def address = statics.get(key)
            return address[0]
        }
        return null
    }

    def sanitizeBlock(def block) {
        block = block.replaceAll(/(?i)CASE NUMBER.*\d|CASE NO.*|CASE NOS.*/, "")
        block = block.replaceAll(/COMBINED STATEMENT OF/, "")
        block = block.replaceAll(/CHARGES, INFORMAL/, "")
        block = block.replaceAll(/SETTLEMENT AGREEMENT/, "")
        block = block.replaceAll(/AND CONSENT ORDER IN .*/, "")
        block = block.replaceAll(/DISCIPLINARY CASE|CONSENT AGREEMENT/, "")
        block = block.replaceAll(/(?i)Executive Officer|Sign  ecutive Officer|Ign  Executive Officer|Sign Executive Officer|Sign Executive Officer|ecutive fficer|Execuflve Officer/, "")
        block = block.replaceAll(/DIA NOS.*|DIA NO.*/, "")
        block = block.replaceAll(/(?i)Inactive|ecutive Officer|Execulve of er|CANCELLED|ign   ecutive Officer|Signat cecutive Officer|r  Sig|INACTIVE|FINDINGS OF FACT|CONCLUSIONS OF LAW|DECISION AND ORDER/, "")
        block = block.replaceAll(/(Firm|Broker|Salesperson).*/, "\n")
        block = block.replaceAll(/\)|\(|}/, "")
        block = block.replaceAll(/\s\s\s(,|-|\bsign\b|\bSign\b)/, "")
        block = block.replaceAll(/\bIgna\b/, " ").trim()
        block = block.replaceAll(/\bSigna\b/, " ").trim()
        block = block.replaceAll(/\bS\b(?!\.)/, " ").trim()
        block = block.replaceAll(/(?ism)\n\s*\ba\b\s*\n/, " ").trim()
        block = block.replaceAll(/STIPULATION AND/, " ").trim()
        block = block.replaceAll(/\n\s*\.\s*\n/, " ").trim()
        block = block.replaceAll(/VOLUNTARY SURRENDER/, " ").trim()
        block = block.replaceAll(/OF BROKER\. LICENSE/, " ").trim()
        block = block.replaceAll(/CONCLUSIONS OF LAW,/, " ").trim()
        block = block.replaceAll(/\bSigns\b/, " ").trim()
        block = block.replaceAll(/DIA No\..*?/, " ").trim()
        block = block.replaceAll(/OF BROKER LICENSE/, " ").trim()
        block = block.replaceAll(/CHARGES AND CONSENT ORDER/, " ").trim()
        block = block.replaceAll(/INFORMAL SETTLEMENT/, " ").trim()
        block = block.replaceAll(/AGREEMENT AND CONSENT|OF SALESPERSON LICENSE\./, " ").trim()
        block = block.replaceAll(/ORDER IN A/, " ").trim()
        block = block.replaceAll(/(?i)sole.*?proprietor/, " ").trim()
        block = block.replaceAll(/EXPIRED/, " ").trim()
        block = block.replaceAll(/(?i)(suspended)/, " ").trim()
        block = block.replaceAll(/(?i)(consent)/, " ").trim()
        block = block.replaceAll(/(?i)(order)/, " ").trim()
        block = block.replaceAll(/(?i)(through)/, " ").trim()
        block = block.replaceAll(/(?i)CEASE/, " ").trim()
        block = block.replaceAll(/(?i)DESIST/, " ").trim()
        block = block.replaceAll(/(?i)STIPULATION/, " ").trim()
        block = block.replaceAll(/(?)(B|F|S)[^ ]*\d/, " ").trim()
        block = block.replaceAll(/(?i)NOTICE OF COMISSION.?/, " ").trim()
        block = block.replaceAll(/(?i)AGREEMENT/, " ").trim()
        block = block.replaceAll(/(?i)CHARGES/, " ").trim()
        block = block.replaceAll(/(?i)STATEMENT/, " ").trim()
        block = block.replaceAll(/OF/, " ").trim()
        block = block.replaceAll(/(?i)IN THE MATTER.*?:/, " ").trim()
        block = block.replaceAll(/BROKER/, " ").trim()
        block = block.replaceAll(/(?i)Complaint/, " ").trim()
        block = block.replaceAll(/(?ism)this.*?settle.*?and/, " ").trim()

        return block
    }

    def getLastPageNumber(def html) {
        def lastPageNumber
        def lastPageMatcher1 = html =~ /href="([^"]+)"\s+title="Go to last page"/
        if (lastPageMatcher1.find()) {
            def link = lastPageMatcher1.group(1)
            lastPageNumber = link.replaceAll(/(?:.+?page=)(\d+)/, '$1')
        }
        return Integer.parseInt(lastPageNumber)
    }

    def concatenateLines(def lines) {
        StringBuilder stringBuilder = new StringBuilder("");
        def iterateLinesMatcher = lines =~ ".*"
        while (iterateLinesMatcher.find()) {
            def currentLine = iterateLinesMatcher.group(0).trim()
            stringBuilder.append(currentLine + " ")
        }
        def completeLine = stringBuilder.toString().trim()
        //removing double spaces
        completeLine = completeLine.replaceAll(/\s{2,}/, " ").trim()

        return completeLine
    }

    def pdfToTextConverter(def pdfUrl) {
        try {
            List<String> pages = ocrReader.getText(pdfUrl)
            return pages
        }
        catch (NoClassDefFoundError e) {
            def pdfFile = invokeBinary(pdfUrl)
            def pmap = [:] as Map
            pmap.put("1", "-layout")
            pmap.put("2", "-enc")
            pmap.put("3", "UTF-8")
            pmap.put("4", "-eol")
            pmap.put("5", "dos")
            //pmap.put("6", "-raw")
            def pdfText = context.transformPdfToText(pdfFile, pmap)
            return pdfText
        }
        catch (IOException e) {
            return "PDF has no page"
        }
    }


    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = true, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}