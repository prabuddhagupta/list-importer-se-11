package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils

import java.text.SimpleDateFormat
import com.rdc.rdcmodel.model.RelationshipType

import java.time.*


context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_Nj_Dbi_Disciplinary_Actions script = new Us_Nj_Dbi_Disciplinary_Actions(context)
script.initParsing()


int i = 1

def nameIdMap = [:]
for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + i++)
    nameIdMap[entity.getName()] = entity
}

for (entity in context.session.getEntitiesNonCache()) {
    for (association in entity.getAssociations()) {

        otherEntity = nameIdMap[association]
        if (otherEntity && !isAlreadyAssociated(entity, otherEntity)) {
            scrapeEntityAssociation = new ScrapeEntityAssociation(otherEntity.getId())
            scrapeEntityAssociation.setRelationshipType(RelationshipType.ASSOCIATE)
            scrapeEntityAssociation.setHashable(otherEntity.getName(), otherEntity.getType())
            entity.addScrapeEntityAssociation(scrapeEntityAssociation)
        }
    }
    entity.getAssociations().clear()
}

def isAlreadyAssociated(entity, otherEntity) {
    Set associations = otherEntity.getScrapeEntityAssociations();
    boolean isAssos = false
    if (associations) {
        associations.each { item ->
            if (((ScrapeEntityAssociation) item).getId().equals(entity.getId())) {
                isAssos = true
            }
        }
    }
    return isAssos
}

class Us_Nj_Dbi_Disciplinary_Actions {
    final addressParser
    final entityType
    ScrapianContext context

    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")

    def url = 'https://www.state.nj.us/dobi/division_banking/bankdivenforce.html'
    def urlInsurance = 'https://www.state.nj.us/dobi/division_insurance/insfines.htm'
    def pdfLink = 'https://www.state.nj.us/dobi/division_banking/'
    def rootInsr = 'https://www.state.nj.us/dobi/division_insurance/'
    def root = 'https://www.state.nj.us'
    def nameList = []

    Us_Nj_Dbi_Disciplinary_Actions(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        this.context = context
        addressParser.updateCities([US: ["Garfield", "Camden", "Delran", "Lodi", "Neptune", "Berlin", "Kearny", "Westampton", "Matawan", "Mercerville", "Mount Royal", "Newark", "Pennsauken", "Paterson", "Irvington", "Elizabeth", "East Brunswick", "Little Ferry", "Lindenwold", "Lodi", "Sicklerville", "Springfield", "Parsippany", "Jersey City", "Laguna Hills", "Lakewood", "Newtown", "Perth Amboy", "Asbury Park", "Harrison", "Paterson & Perth Amboy", "North Brunswick", "Oakhurst", "Bridgeton", "Paterson & Paramus", "Bound Brook", "New Brunswick", "Melbourne", "Burlington", "Linden", "Shrewsbury", "Cranford", "Willston Park", "Philadelphia", "Sewell", "Freehold", "Berlin", "Lee", "Edison", "Ardmore", "Stratford", "Laurel", "Cherry Hill", "Blackwood", "Staten Island", "Roselle", "Sayreville", "Brigantine", "Wayne", "West Chester", "Trenton", "Glen Olden", "Plantation", "Hampton", "Moorestown", "Huntington Valley", "Saddle Brook", "Ocala", "Dunellen", "Neshamic Station", "Bernardsville", "Mendham", "Hasbrouck Heights", "Detroit", "Pine Brook", "Upper Saddle River", "Ewing", "Washington", "Toms River", "Boothwyn", "Collegeville", "Brick", "Woodcliff", "Hamilton Square", "West Islip", "Ramsey", "Basking Ridge", "Longport", "McLean", "Plainfield", "Essex Falls", "West Orange", "Spring Lake", "Dover", "Orange", "Rutherford", "Randolph", "Parlin", "Cape Coral", "Hoboken", "Iselin", "Melville", "Flanders", "Hamilton", "Southampton", "Sea Cliff", "Columbia", "Clark", "Houston", "Succasunna", "Tucson", "Fair Haven", "Union", "Union City", "Hilltop", "Bayonne", "Passaic", "Hazlet", "Bergenfield", "Hammonton", "Point Pleasant", "Wood Ridge", "Cinnaminson", "Deptford", "Merchantville", "Elmwood Park", "Middletown", "Oak Ridge", "Folsom", "Greenvale", "Hackensack", "South Orange", "Miamisburg", "South Plainfield", "Fords", "West Windsor", "Warwick", "River Edge", "Lewisville", "Mount Holly", "Bordentown", "Woodbury", "Irvine", "Norwood", "Navesink", "Colts Neck", "Clifton", "Kinnelon", "Chicago", "Morganville", "Oceanport", "Branchburg", "Bridgewater", "Warren", "Glen Cove", "Glen Head", "Morris Plains", "Stanhope", "Thorofare", "Mantua", "Westfield"]])
    }

    def initParsing() {
         modifyData(invokeUrl(url))

        /*Insurance Part*/
        def html = invokeUrl(urlInsurance)
        def urlMatcher = html =~ /<a href="(insfines\d+\.(?:html|htm))" target="_blank">\d{4} Acti[tv]ity<\/a>/
        def insuranceUrl
        while (urlMatcher.find()) {
            insuranceUrl = urlMatcher.group(1)
            insuranceUrl = rootInsr + insuranceUrl
            modifyEachInsuranceData(insuranceUrl)
        }
        defaultAddress()
    }

    def modifyData(html) {
        modifyDepositoryData(html)
        modifyConsumerData(html)
    }

    def modifyDepositoryData(html) {
        def depositoryRegex = /(?s)(.*?)Office of Consumer Finance Licensee Enforcement/
        def depositoryMatcher = html =~ depositoryRegex
        if (depositoryMatcher.find()) {
            def depository = depositoryMatcher.group(1)
            def sanitizeRegex = /(?ism)(style41">.+?<\/td>)/
            def sanitizeMatcher = depository =~ sanitizeRegex

            def entity
            def aliasList = []
            def href
            def hrefList = []
            def date
            def penalty
            def addressList = []

            while (sanitizeMatcher.find()) {
                def eventDate = ""
                def sanitizedData = sanitizeMatcher.group(1).trim()
                def bankRegex = /(?ism)Institution:(?:\s|<\\/stro.+?rong>)(.+?)</
                def bankMatcher = sanitizedData =~ bankRegex

                if (bankMatcher.find()) entity = bankMatcher.group(1).trim()
                def dateRegex = /[A-Z][a-z]{2,8},?\s*?\d{1,2},\s*?\d{4}/
                def dateMatcher = sanitizedData =~ dateRegex
                if (dateMatcher.find()) date = dateMatcher.group()

                if (StringUtils.isNotBlank(date) || StringUtils.isNotEmpty(date)) {
                    eventDate = date

                } else {
                    eventDate = setDefaultDate(link)
                }
                def hrefRegex = /(?s)href="(.*?)"/
                def hrefMatcher = sanitizedData =~ hrefRegex
                if (hrefMatcher.find()) href = pdfLink + hrefMatcher.group(1).trim()
                hrefList.add(href)
                createEntity(entity, aliasList, addressList, eventDate, hrefList, penalty)
            }
        }
    }

    def modifyConsumerData(html) {
        def consumerRegex = /(?s)(Office of Consumer Finance Licensee Enforcement.*?<\/ul>)/
        def hrefRegex = /(?:<a\s*href="(.*?)")/
        def hrefList = []
        def link = 'https://www.state.nj.us/dobi/division_banking/'

        def consumerMatcher = html =~ consumerRegex
        if (consumerMatcher.find()) {
            def consumer = consumerMatcher.group(1)
            def hrefMatcher = consumer =~ hrefRegex
            while (hrefMatcher.find()) {
                def href = hrefMatcher.group(1)
                href = link + href
                if (href.equals('https://www.state.nj.us/dobi/division_banking/bankdivenforce_2013.html')) {
                    def list = collectArchivedHref(invokeUrl(href), link)
                    hrefList.addAll(list)
                    continue
                }
                hrefList.add(href)
            }
        }
        hrefList.each { modifyEachConsumerData(it) }
    }

    def collectArchivedHref(html, link) {
        def hrefList = []
        def archiveTableRegex = /(?m)Archive.*?<a\s*?(.*)<\\/td>/
        def archiveTableMatcher = html =~ archiveTableRegex
        if (archiveTableMatcher.find()) {
            def archiveTable = archiveTableMatcher.group(1)
            def hrefRegex = /(?m)href="(.*?)"/
            def hrefMatcher = archiveTable =~ hrefRegex
            while (hrefMatcher.find()) {
                def href = hrefMatcher.group(1)
                href = link + href
                hrefList.add(href)
            }
        }
        return hrefList
    }

    def modifyEachConsumerData(link) {
        def date
        def orderRegex = /(?is)(?:<strong>|<br\s\/>\s*?)([\w\s]+.*?(?:(?:Penalty:.*?)?\$[\d,]+|1400927|seq\.|act\.|\(5\)\.|Department\.|license\.|penalt|2008\.\s<\/span>|public\.|revoked|registration\.|offices\.|LLC\.).*?<(?:\/|br))/
        if (link == 'https://www.state.nj.us/dobi/division_banking/ocf/enforcement/2018.html')
            orderRegex = /(?s)(?:<strong>|<br\s\/>)([\w\s]+.*?(?:(?:Penalty:.*?)?\$[\d,]+|license).*?)<(?:\/|br)/

        def orderRegex1 = /(?s)<span class="style45">American.*?January\s23,\s*?2012.*?\$12,500<\/span>/
        def orderRegex2 = /(?s)<strong>Absolute\sHome\s*?Mortgage.*?\$.*?<\/strong>/

        def html = invokeUrl(link)
        def discardRegex = /(?is)Helvetica-Conth, Tahoma">(.*)/
        def discardMatcher = html =~ discardRegex

        def commonBlock
        def uniqueBlock1
        def uniqueBlock2

        if (discardMatcher.find()) {
            def orders = []
            def mainHtml = discardMatcher.group(1)
            def orderMatcher = mainHtml =~ orderRegex
            def orderMatcher1 = mainHtml =~ orderRegex1
            def orderMatcher2 = mainHtml =~ orderRegex2

            if (orderMatcher1.find()) uniqueBlock1 = orderMatcher1.group()
            if (orderMatcher2.find()) uniqueBlock2 = orderMatcher2.group()

            while (orderMatcher.find()) {
                commonBlock = orderMatcher.group()
                def block = commonBlock
                if (uniqueBlock1.toString().contains(commonBlock)) {
                    block = uniqueBlock1
                } else if (uniqueBlock2.toString().contains(commonBlock)) {
                    block = uniqueBlock2
                }
                orders.add(block)
            }
            orders.each { it ->
                def eventDate = ""
                date = captureDate(it)

                if (StringUtils.isNotBlank(date) || StringUtils.isNotEmpty(date)) {
                    eventDate = date

                } else {
                    eventDate = setDefaultDate(link)
                }
                def entities = separateData(it, link)
                entities.each { entityName ->
                    handleEntityNameAndAlias(entityName, captureLink(it, link), eventDate, capturePenalty(it))
                }
            }
        }
    }

    def modifyEachInsuranceData(link) {

        def html
        def addRegex = /(?:investigators|estate plan|insurance|funds|Jersey|inaccurate|Department inquiries)\.|(?:applications|Securities)\.<\/p><\/td>/
        def rowRegex = /(?s)(<tr .*?>.*?<\/tr>)/
        def orderRegex
        def rowMatcher
        def blockMatcher
        def block

        if (link =~ /.*insfines(?:04|05)\.htm/) {
            def eventDate = setDefaultDate(link)

            def entities = []
            def penalties = []
            def removeFromPenalty = /(?:Costs:|Suspension|Restitution|Unlicensed|Revocation|Surrendered|Civil|Reinstatement|Probation|Satisfaction of Judgment)/
            html = sanitizeHtml(invokeUrl(link))
            rowMatcher = html =~ rowRegex
            while (rowMatcher.find()) {

                def row = sanitizeRowFor2004And2005(rowMatcher.group(1))
                (entities, penalties) = getDataFor2004_and_2005(row)

                if ((entities.size() == 1) && (penalties.size() == 2)) {
                    entities.eachWithIndex { entity, index ->
                        entity = entity.toString().replaceAll(/\([a-z]\)/, "").trim()

                        if (entity =~ /^.*,(?!.*(?:Inc\.?|LLC\.?|Co\.|Ltd\.|of New Jersey|\((?:NJ|PA)\))$)/) {
                            entity = entity.replaceAll(/(.*),(.*)/, '$2 $1').trim()

                            penalties.eachWithIndex { penalty, index1 ->
                                penalty = penalty.toString().replaceAll(/$removeFromPenalty.*/, "").trim()

                                if (penalty) {
                                    createEntity(entity, "", "", eventDate, Arrays.asList(link), penalty)
                                }
                            }
                        } else {
                            penalties.eachWithIndex { penalty, index1 ->
                                penalty = penalty.toString().replaceAll(/$removeFromPenalty.*/, "")
                                penalty = penalty.toString().replaceAll(/\s+/, " ")

                                if (penalty) {
                                    createEntity(entity, "", "", eventDate, Arrays.asList(link), penalty)
                                }
                            }
                        }
                    }
                } else {
                    entities.eachWithIndex { entity, index ->

                        penalties.eachWithIndex { penalty, index1 ->
                            entity = entity.toString().replaceAll(/\([a-z]\)/, "").trim()
                            penalty = penalty.toString().replaceAll(/$removeFromPenalty.*/, "")
                            penalty = penalty.toString().replaceAll(/\s+/, " ")

                            if (index == index1) {
                                if (entity =~ /^.*,(?!.*(?:Inc\.?|LLC\.?|Ltd\.|of New Jersey|Co\.|\((?:NJ|PA)\))$)/) {
                                    entity = entity.replaceAll(/(.*),(.*)/, '$2 $1').trim()
                                    createEntity(entity, "", "", eventDate, Arrays.asList(link), penalty)
                                } else {
                                    createEntity(entity, "", "", eventDate, Arrays.asList(link), penalty)
                                }
                            } else {
                                if (entity =~ /^.*,(?!.*(?:Inc\.?|LLC\.?|Co\.|Ltd\.|of New Jersey|\((?:NJ|PA)\))$)/) {
                                    entity = entity.replaceAll(/(.*),(.*)/, '$2 $1').trim()
                                    createEntity(entity, "", "", eventDate, Arrays.asList(link), penalty)
                                } else {
                                    createEntity(entity, "", "", eventDate, Arrays.asList(link), penalty)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            def orders = []
            html = sanitizeHtml(invokeUrl(link))
            orderRegex = /(?s)(?:<p.*?>|\n<br\s\/>).*?(?=(?:\n<br \/|<p).*<\/strong>.*\w+, [A-Z]{2}<(?:br \/|\/p)>|\Z)/
            blockMatcher = html =~ orderRegex

            while (blockMatcher.find()) {
                def date = ""
                def eventDate = ""
                block = blockMatcher.group()


                block = sanitizeBlock(block)

                date = captureDate(block)

                if (StringUtils.isNotBlank(date) || StringUtils.isNotEmpty(date)) {
                    eventDate = date

                } else {
                    eventDate = setDefaultDate(link)
                }

                def entityUrlList = captureLink(block, link)
                def penalty = capturePenalty(block)

                block = block.toString().replaceAll(/(?s)(?<=[A-Za-z]+ \d{1,2}[,\.] \d{4}[\s\.]?<br\s?\/>).*$/, "")
                block = block.replaceAll(/(?s)<a href=.*/, "")
                block = block.replaceAll(/(?s)(?:[A-Z][a-z]{2,11}[,]? \d{1,2}, \d{4}|[A-Z][a-z]{2,11} \d{1,2} \d{4}).*$/, "")
                def entities = separateData(block, link)

                entities.each { entityName ->

                    handleEntityNameAndAliasForInsurance(entityName, entityUrlList, eventDate, penalty)
                }
            }
        }
    }

    def getDataFor2004_and_2005(row) {

        def entities = []
        def penalties = []
        def entityData
        def penaltyData

        def tbDataMacher = row =~ /(?s)(<td.*?<\/td>)\s*(<td(.*?)<\/td>)/

        while (tbDataMacher.find()) {
            def entityName
            def penalty
            entityData = tbDataMacher.group(1)
            entityData = entityData.toString().replaceAll(/<(?:td|p|font).*Tahoma\">/, "")
            penaltyData = tbDataMacher.group(2)
            penaltyData = penaltyData.toString().replaceAll(/<(?:td|p|font).*Tahoma\">/, "")

            def entityNameMacher = entityData =~ /(?m)^(.*)(?:<br \/>|<\/font>)/
            while (entityNameMacher.find()) {
                entityName = entityNameMacher.group(1)
                entities.add(entityName)
            }
            def penaltyMatcher = penaltyData =~ /(?m)^(.*)(?:<br \/>|<\/font>)/
            while (penaltyMatcher.find()) {
                penalty = penaltyMatcher.group(1)
                penalties.add(penalty)
            }
        }

        return [entities, penalties]
    }

    def sanitizeRowFor2004And2005(def row) {
        row = row.toString().replaceAll(/(?<=Inc)<\/font><font size="1" face="Verdana, Arial, Helvetica-Conth, Tahoma">\. <\/font>/, ".<br />")
        row = row.toString().replaceAll(/(?<=(?:Insurance|Ateasha|Muhammed|Michael|Bail|Edward))\s*/, " ")
        row = row.toString().replaceAll(/(?:<\/strong>\n<strong>John A\.)/, " John A.")
        row = row.toString().replaceAll(/(?:<td.*Order adopting Ohio Consent Order<.*)/, "")

        return row.trim()
    }

    def handleEntityNameAndAlias(name, entityUrl, eventDate, penalty) {
        def alias
        def aliasList = []
        def aliasMatcher
        def address
        def addressList = []

        def addressRegex = /((?:Paterson.*?|Mt\.|Ft\.)?[\w\s]+?[A-Z][a-z ]+,\s(?:New\sJersey|NJ|PA|NY|CA|FL|MI|TX|AZ|CT|OH|RI|SC|IL|VA))/
        def addressMatcher = name =~ addressRegex
        if (addressMatcher.find()) {
            address = addressMatcher.group(1).trim()
            name = name.toString().replaceAll(address, "").trim()
        }
        name = sanitizeName(name)
        address = sanitizeAddress(address)
        addressList.add(address)

        if (name =~ /(?i)\s(?:d\/b\/a|T\/A)/) {
            aliasMatcher = name =~ /(?i)\s((?:d\/b\/a|T\/A).+)/
            if (aliasMatcher.find()) {
                alias = aliasMatcher.group(1).trim()
                name = name.toString().replaceAll(/$alias/, "").trim()
                name = sanitizeName(name)
                alias = alias.replaceAll(/(?i)t\/a/, " ").trim()
                if (alias =~ /(?i)d\/b\/a\/?/) {
                    aliasList = alias.toString().split(/(?i)d\/b\/a\/?\s/).collect({ it -> return it.trim() })
                } else {
                    aliasList.add(alias)
                }
            }
        }

        if (name =~ /(\/(?:JDN|Affordable).+)/) {
            aliasMatcher = name =~ /(\/(?:JDN|Affordable).+)/
            if (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/$alias/, "").trim()
                alias = alias.replaceAll("/", "").trim()
                aliasList.add(alias)
            }
        }
        if (name =~ /Nimal|Robes|Williams E\./) {
            aliasMatcher = name =~ /((?:Nimal|Robes|Williams E\.).+)/
            if (aliasMatcher.find()) {
                def name1 = aliasMatcher.group(1)
                name = name.toString().replaceAll(/$name1/, '').trim()
                name = name.toString().replaceAll('&', '').trim()
                createEntity(name1, aliasList, addressList, eventDate, entityUrl, penalty)
            }
        }
        createEntity(name, aliasList, addressList, eventDate, entityUrl, penalty)
    }

    def handleEntityNameAndAliasForInsurance(name, entityUrlList, eventDate, penalty) {

        def alias
        def aliasList = []
        def aliasMatcher
        def address
        def addressRegex
        def addressMatcher
        def addressMatcher1 = name =~ /<\/strong>([\.,]*?[\w\s,]+?\s?[A-Za-z]+, [A-Z]{2};?\s?)<br\s?\/>/

        def additionalAddressEx0 = /(?:Camden, NJ, Pennsauken, NJ and)/
        def additionalAddressEx = /(?:$additionalAddressEx0|Vineland, NJ and Middletown, DE and Key|Northfield, NJ and|Mahwah and|Toms River, Marlboro and|Brick, NJ, Marlboro, NJ and Toms|Baltimore, MD and Fort|Joint Base|\w+?\s?[A-Za-z\.]+, [A-Z]{2}\sand[\s\w]+?\s?(?:and)?)/
        def additionalAddress = /(?:Joint Base`|South Toms|King of|Salt Lake|West (?:Long|Palm)|Upper Saddle|(?:Freehold|Plainfield|Hopelawn) and (?:South|Toms|Perth|Old)|Avon by the|Browns Mills and (?:Mt\.|Mount)|(?:Chester Springs|Lawnside|Westfield|Glen Ridge|Roanoke|Hesperia|Freehold|Camden|Weehawkin|Roselle|Colonia|Waldwick|Hopelawn|East Brunswick|Mahwah|Toms River|Lindenwood|Pelham|Wyckoff) and)/
        def additionalAddress0 = /(?:Kitty|Lincoln|Pompton|Upper|Township of|Cambridge|Park|Far|Oak|Highland|Prospect|Howard|Queens|Cream|Galloway and Perth|Deerfield|East|Little|Jersey|Fair|Blue|Ocean|Long|Altamonte|Beach Haven|Cedar|Newport|Somers|San|West New|New York|Toms|Boca|Lake|North|Lanoka|Saddle|Harve De|Ann|Fort|Point|Beach|National|Port Jefferson)/
        def additionalAddress1 = /(?:Cliffwood|Middle|Coral|Huntingdon|Mays|Asbury|Bloomfield|Mountain|Kendall|Huntington|Floral|Colts|Florham|Delray|Mount|West|New|Little|Palisades|Fresh|Manuel Moreno|Ridgefield|La|Tinton|Perth|Grass|Long Island|South|Ventnor|River|Ho Ho|Carol|Belle|Franklin|Pine|Fairless|Cherry|Gulf|Cape May Court|Cliffside|Miami|Maple|Colonia)/
        def additionalAddress2 = /(?:Newtown|Simi|Basking|Salt|Egg Harbor|Stony|Tewksbury|Morris|Boynton|Kansas|Union|Wall|White|Spring|Bridgeton and|Monroe|Garden|Red|Englewood|Wildwood|Palm|Glen|Millstone|Old|Washington|Lighthouse|Yorktown|Coconut|Egg|Gloucester|Cape|Pym|Seaside|Staten|Green|Jamaica|Elmwood|Overland|Brown|Ozone|Des|Middleburg|Point Pleasant)/
        def additionalAddress3 = /(?:Rapid|City|Bala|Pompano|Kings|Harrington|Laurel|Key|Hamilton|Chester|Fort|Coconut|Lehigh|Burlington and Mt\.|Santa)/

        addressRegex = /((?:Paterson.*?|Mt\.|Ft\.)?(?:$additionalAddressEx|$additionalAddress|$additionalAddress0|$additionalAddress1|$additionalAddress2|$additionalAddress3)?\s?[A-Za-z\-\.]+\s?,\s?(?:New\sJersey|[A-Z]{2})[;,]?$)/
        addressMatcher = name =~ addressRegex

        if (addressMatcher.find()) {
            address = addressMatcher.group(1).trim()
            name = name.toString().replaceAll(address, "").trim()
            address = fixAddress(address)
        }
        name = sanitizeName(name)

        def addressList = []
        def addressMatcher3 = address =~ /(.*?[A-Z]{2})/

        if ((address = ~/(.*?[A-Z]{2})/)) {
            while (addressMatcher3.find()) {
                address = addressMatcher3.group(1)
                addressList.add(address)
            }
        } else {
            addressList.add(address)
        }
        if (name =~ /(?i)\s(?:d\/b\/a|T\/A)/) {
            aliasMatcher = name =~ /(?i)\s((?:d\/b\/a|T\/A).+)/
            if (aliasMatcher.find()) {
                alias = aliasMatcher.group(1).trim()
                name = name.toString().replaceAll(/$alias/, "").trim()
                name = sanitizeName(name)
                alias = alias.replaceAll(/(?i)t\/a/, " ").trim()
                if (alias =~ /(?i)d\/b\/a\/?/) {
                    aliasList = alias.toString().split(/(?i)d\/b\/a\/?\s/).collect({ it -> return it.trim() })
                } else {
                    aliasList.add(alias)
                }
            }
        }

        if (name =~ /(\/(?:JDN|Affordable).+)/) {
            aliasMatcher = name =~ /(\/(?:JDN|Affordable).+)/
            if (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/$alias/, "").trim()
                alias = alias.replaceAll("/", "").trim()
                aliasList.add(alias)
            }
        }

        if (name =~ /Nimal|Robes|Williams E\./) {
            aliasMatcher = name =~ /((?:Nimal|Robes|Williams E\.).+)/
            if (aliasMatcher.find()) {
                def name1 = aliasMatcher.group(1)
                name = name.toString().replaceAll(/$name1/, '').trim()
                name = name.toString().replaceAll('&', '').trim()
                createEntity(name1, aliasList, addressList, eventDate, entityUrlList, penalty)
            }
        }
        createEntity(name, aliasList, addressList, eventDate, entityUrlList, penalty)
    }

    def createEntity(name, aliasList, addressList, eventDate, entityUrlList, penalty) {

        def entity
        entity = null
        name = sanitizeName(name)
        if (StringUtils.isNotBlank(name) || StringUtils.isNotEmpty(name)) {
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType])

            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                nameList.add(name)
                entity.setType(entityType)
            }
            def aliasEntityType
            aliasList.each({
                if (it) {
                    aliasEntityType = detectEntity(it)
                    if (aliasEntityType.toString().equals(entityType)) {
                        entity.addAlias(it)
                    } else {
                        if (aliasEntityType.equals("P")) {
                            entity.addAssociation(it)
                        } else {
                            entity.addAssociation(it)
                            //create new entity with association
                            def newEntity = context.findEntity(["name": it, "type": aliasEntityType])
                            if (!newEntity) {
                                newEntity = context.getSession().newEntity();
                                newEntity.setName(it)
                                newEntity.setType(aliasEntityType)
                            }
                            newEntity.addAssociation(name)
                            addCommonPartOfEntity(newEntity, eventDate, entityUrlList, penalty, addressList)
                        }
                    }
                }
            })
            addCommonPartOfEntity(entity, eventDate, entityUrlList, penalty, addressList)
        }
    }

    def addCommonPartOfEntity(entity, eventDate, entityUrlList, penalty, addressList) {
        def remark
        def eventDescription = "This entity appears on the New Jersey Department of Banking and Insurance list of Enforcement Actions."
        penalty = penalty.toString().replaceAll(/(?i)\s*fine:\s*/, "").trim()
        penalty = penalty.toString().replaceAll(/(?i)^null$/, "").trim()

        if (StringUtils.isNotBlank(penalty) || (StringUtils.isNotEmpty(penalty))) {

            eventDescription = "$eventDescription Penalty: $penalty."
        }
        addressList.each { address ->
            address = sanitizeAddress(address)
            if (StringUtils.isNotBlank(address) || StringUtils.isNotEmpty(address)) {

                address = address.toString().replaceAll(/(.*)/, ', $1')
                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                entity.addAddress(scrapeAddress)
            }
        }
        ScrapeEvent event = new ScrapeEvent()
        if (eventDescription) {

            if (eventDate =~ /^\d{2,4}$/) {
                eventDate = context.parseDate(eventDate)
                event.setDate(eventDate)
            } else {
                eventDate = context.parseDate(new StringSource(eventDate), ["MMMM dd, yyyy", "MMMM d, yyyy", "MMMM, dd, yyyy", "MMMM, d, yyyy"] as String[])

                def currentDate = new Date().format("MM/dd/yyyy")
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy")
                def date1 = simpleDateFormat.parse(currentDate)
                def date2 = simpleDateFormat.parse(eventDate)

                if (date2.compareTo(date1) > 0) {
                    remark = eventDate
                    eventDate = null
                    entity.addRemark(remark)
                } else
                    event.setDate(eventDate.trim())
            }
        }
        event.setDescription(eventDescription)
        entity.addEvent(event)

        entityUrlList.each { entityUrl ->
            if (entityUrl.toString().contains(".pdf")) {
                if (StringUtils.isNotBlank(entityUrl) || StringUtils.isNotEmpty(entityUrl)) {
                    entity.addUrl(entityUrl)
                }
            } else if (entityUrl =~ /\/insfines(?:04|05)/) {
                entity.addUrl(entityUrl)
            }
        }
    }

    def detectEntity(name) {
        def type
        if (name =~ /Rodney B. Culp|Mark G. D'Agostino/) {
            type = "P"
        } else if (name =~ /(?:of Tennessee|Bail Bonds)$/) {
            type = "O"
        } else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.?$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$|Whitaker$|\bbank\b|Fairview| Loan |Sunshine Adjustment)/ || name == 'Sylvander II') {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:\sDel\sSol$|\sHoof\sJr.$|\sTh.*Mt\.$)/) {
                type = "P"
            }
        }
        return type
    }

    def defaultAddress() {
        def entity = null
        nameList.each {
            entity = context.findEntity("name": it)

            if (entity.getAddresses().isEmpty()) {
                ScrapeAddress scrapeAddress = new ScrapeAddress()
                scrapeAddress.setProvince('New Jersey')
                scrapeAddress.setCountry('UNITED STATES')
                entity.addAddress(scrapeAddress)
            }
        }
    }

    def separateData(html, link) {
        if (link =~ /.*insfines\d+\.htm/) {
            html = sanitizeDataForInsurance(html)
        } else {
            html = sanitizeData(html)
        }
        def entities = []
        def uniqueRegex = /((?:Lihua Lin|Skyl.*?NMLS|Prof[\w\s]*?Ser|Wireless|Second|S\.I\.|Christians|Absolute).*?)</
        def uniqueMatcher = html =~ uniqueRegex
        def differentEntityRegex = /,\s*?(Owner|CEO|Officer|Compliance\sOfficer|President|Managing\sMember|Executive\sVice\sPresident|Secretary)/

        if (uniqueMatcher.find()) {
            def entity = uniqueMatcher.group(1).trim()
            entities.add(entity)
            def multipleEntityRegex = /(Susan\sLange,\sCEO|Glenn\sBromley|Max\sPepin|Peter\sStrauss|Robert\sDeFalco|Fabio\sGomez|Marco\sJ\.\sDinello|James\sZ\.\sMalamut|William\sA\.\sMalamut)/
            def multipleEntityMatcher = html =~ multipleEntityRegex
            while (multipleEntityMatcher.find()) {
                def multipleEntity = multipleEntityMatcher.group(1).trim()
                def differentEntityMatcher = multipleEntity =~ differentEntityRegex

                if (differentEntityMatcher.find()) {
                    entities[-1] = entities[-1] + " d/b/a " + multipleEntity
                } else entities.add(multipleEntity)
            }
        } else {
            def separationRegex = /(?:>\s*?)(?!(?:Reference|&nbsp;|NMLS|1300032))([0-6A-Za-z\s,\.&;\/'-\-\)\(]+?)(?:<\/strong>|<br\s?\/>|\Z)/
            def separationMatcher = html =~ separationRegex
            while (separationMatcher.find()) {
                def data = separationMatcher.group(1).trim()


                if (data && data != '&ndash;') {
                    def dataMatcher = data =~ /enwor.+/
                    def dataMatcher1 = data =~ /Research.+/
                    def differentEntityMatcher = data =~ differentEntityRegex
                    if (differentEntityMatcher.find()) {
                        entities[-1] = entities[-1] + " d/b/a " + data
                    } else if (dataMatcher.find() || data == ', LLC') {
                        entities[-1] = entities[-1] + data
                    } else if (dataMatcher1.find() || data == 'Check Cashing Service, Inc.' || data == 'Check Cashing' || data == 'Toms River, New Jersey' || data == 'Highland Lakes, NJ') {
                        entities[-1] = entities[-1] + " " + data
                    } else {
                        if (data.startsWith("d/b/a") || data.startsWith("t/a") || data.startsWith("T/A") || data.startsWith("&ndash;") || data.startsWith("Paterson")) {
                            entities[-1] = entities[-1] + " " + data
                        } else if (data.startsWith("Shrewsbury")) {
                            entities[-1] = entities[-1] + " &ndash; " + data
                        } else entities.add(data)
                    }
                }
            }
        }
        return entities
    }

    def sanitizeDataForInsurance(html) {
        def addtext = /(?:Final\s*Order[s]?|Orders to Show Cause|Notices and Final Orders|Consent Orders|Final and Miscellaneous Orders|Conditional Order Revoking License|Matters Resolved Without Determination of Violation|Cease and Desist Order|Amended Order to Show Cause)/
        def addtext1 = /(?:Order Denying Stay Pending Appeal|Order Denying Stay|Order Suspending License Pending Completion of Administrative Proceedings|Order to Show Cause|Orders Suspending License Pending Administrative Proceedings)/

        html = html.toString().replaceAll(/.*<strong style="color: #06C">(?:<br \/>)?[\s\n]?(?:Conditional Order Revoking License)[\s]?<\/strong>(?:<\/p>|<\/span>)/, "")
        html = html.toString().replaceAll(/.*<strong style="color: #06C">[\s]?(?:Final Orders?|Order[s]? to Show Cause|Consent Orders|Conditional Order Revoking License)[\s]?<\/strong>(?:<\/p>|<\/span>)/, "")
        html = html.toString().replaceAll(/<br \/>\s{2,}(?:Order[s]? Amending Final Order|Order[s]? to Show Cause|Consent Order[s]?).*<\/p>/, "")

        def sanitizeRegex = /(?s)(?:(?:<\/font>)+?(?:<\/strong>|<br\s\/>).*?)((?:<strong>|<br\s\/>)[\w\s].*)/
        def sanitizeMatcher = html =~ sanitizeRegex
        if (sanitizeMatcher.find()) html = sanitizeMatcher.group(1)

        def sanitizeRegex1 = /(?s)(?:class).*?((?:>|<strong>|<br\s\/>)[\w\s].*)/
        def sanitizeMatcher1 = html =~ sanitizeRegex1
        if (sanitizeMatcher1.find()) html = sanitizeMatcher1.group(1)

        def sanitizeRegex2 = /(?s)class.*?(<strong>[\w\s].*)/
        def sanitizeMatcher2 = html =~ sanitizeRegex2
        if (sanitizeMatcher2.find()) html = sanitizeMatcher2.group(1)

        def sanitizeMatcher3 = html =~ sanitizeRegex2
        if (sanitizeMatcher3.find()) html = sanitizeMatcher3.group(1)

        def sanitizeRegex4 = /(?s)(?:<br\s\/>\s+?(<br\s\/>.*))/
        def sanitizeMatcher4 = html =~ sanitizeRegex4
        if (sanitizeMatcher4.find()) html = sanitizeMatcher4.group(1)

        html = html.toString().replaceAll(/\. <\/font><\/td>/, "")
        html = html.toString().replaceAll(/<p><font size="1" face="Verdana, Arial, Helvetica-Conth, Tahoma">/, "")
        html = html.toString().replaceAll(/<\/strong>,<strong> and John J./, '<br /><strong> and John J.')
        html = html.toString().replaceAll(/<\/strong>,<strong>/, ",")
        html = html.toString().replaceAll(/<strong>is alleged to have\s?(?:engaged|failed)?<\/strong>/, "")
        html = html.toString().replaceAll(/<\\/strong>,?\s?(?=[\w\s]+?(?:\s\w+)?, [A-Z]{2};?\s?<br\s?\\/>)/, " ")
        html = html.toString().replaceAll(/<\/b>,?\s*(?=[\w\s]+(?:\s\w+)?, [A-Z]{2};?\s?<br\s?\/>)/, " ")
        html = html.toString().replaceAll(/(?:<\/span>|<\/font>)<\/font>/, " ")
        html = html.toString().replaceAll(/(?:<\/span>|<\/font>)\s*(?=<br \/>)/, "")
        html = html.toString().replaceAll(/(?:<\/span>|<\/font>)\s*<\/p>/, "<br />\n")
        html = html.toString().replaceAll(/<\/strong><span style="font-weight:normal; ">/, "")
        html = html.toString().replaceAll(/<\\/strong>,?\s?(?=[\w\s]+?(?:\s\w+)?, [A-Z]{2};?\s?<br\s?\\/>)/, " ")
        html = html.toString().replaceAll(/<strong>Underwriters/, '<strong>Underwriters')
        html = html.toString().replaceAll(/<\/strong><strong>/, ' ')
        html = html.toString().replaceAll(/<\\/strong>, Old Bridge, NJ(?=\Z|\n)/, ', Old Bridge, NJ<br />')
        html = html.toString().replaceAll(/<\/strong>Browns Mills and Mt\./, 'Browns Mills and Mt.')
        html = html.toString().replaceAll(/<strong>Jose Garcia, <\/strong>/, '</strong>Jose Garcia, ')
        html = html.toString().replaceAll(/<\/strong>, Ft. Lauderdale, FL,/, ', Ft. Lauderdale, FL')
        html = html.toString().replaceAll(/<strong>Cesar Marin/, '</strong>Cesar Marin')
        html = html.toString().replaceAll(/<\/strong>\s*([\w\s]+, [A-Z]{2}) and\s*(?=<strong>)/, '$1</strong>\n')
        html = html.toString().replaceAll(/(?s)<br \/>\s*(?:Respondent was convicted of theft by deception due|Respondent created and submitted false|Respondents are charged with).*/, ' ')
        html = html.toString().replaceAll(/,\s*(?=Brian Mohen|Kelly Bolton|Robert Miller|Sigmar Hessing|and Norman|James Struss|Philip Teseo|Philip Eneo|Virginia Larsen|Rafael Agliata)/, "</strong>\n<strong>")
        html = html.toString().replaceAll(/<(?:span|strong|p).*color: #06C;?.*?>(?:<br \/>)?\s*(?:$addtext|$addtext1)\s*<\/(?:strong|span|p|br \/)>/, "")
        html = html.toString().replaceAll(/<strong.*>Matters Resolved without Determination of Violation<(?:br \/|\/strong)>/, "")
        html = html.toString().replaceAll(/(d\/b\/a\s*Combined Insurance Services, Ltd.), Kingston St. Vincent/, '$1')
        html = html.toString().replaceAll(/<strong>y<br \/>\s*/, '<strong>')
        html = html.toString().replaceAll(/New Jerse<\/strong>/, 'New Jersey</strong>')
        html = html.toString().replaceAll(/<\/strong>Clementon<strong>, <\/strong>NJ/, 'Clementon, NJ')
        html = html.toString().replaceAll(/<\/strong>Chicago, Il/, 'Chicago, IL')
        html = html.toString().replaceAll(/<\/strong>(?=\w+?\s?[A-Za-z]+, [A-Z]{2})/, '')
        html = html.toString().replaceAll(/<\/strong>(?=(?:Washington|Millstone|Feasterville|Perth Amboy|Franklin|Harleysville))/, '')
        html = html.toString().replaceAll(/<strong>[,]?<\/strong>/, '')
        html = html.toString().replaceAll(/<\/strong>., Flemington/, '., Flemington')
        html = html.toString().replaceAll(/<\/strong>Haworth NJ/, 'Haworth, NJ')
        html = html.toString().replaceAll(/PA,\s?<strong>(?=[A-Za-z]+)/, 'PA, <br /><strong>')
        html = html.toString().replaceAll(/PA; and /, 'PA')
        html = html.toString().replaceAll(/PA,\sand<strong>/, 'PA, <br /><strong>')
        html = html.toString().replaceAll(/Lacey<\/strong>, Lawnside/, 'Lacey, Lawnside')
        html = html.toString().replaceAll(/<strong>Federal Risk/, '<br /><strong>Federal Risk')
        html = html.toString().replaceAll(/<\/strong>, Toms River/, ', Toms River')
        html = html.toString().replaceAll(/<\/strong>(?=, Chester Springs)|<strong> <\/strong>&nbsp;&nbsp;/, '')
        html = html.toString().replaceAll(/Pennsylvania/, 'PA')
        html = html.toString().replaceAll(/<\/strong>(\s?,?\s?\w+?\s?[A-Za-z]+, [A-Z]{2},?\s?)(?:and)?\s?(?=<)/, '$1<br /><strong>')
        html = html.toString().replaceAll(/(,\s[A-Z]{2}\s)(?:and)\s?(?=<)/, '$1')
        html = html.toString().replaceAll(/<\/strong>(City Island and Bronx, NY),/, '$1<br /><strong>')
        html = html.toString().replaceAll(/<strong>(?=Jovani|Hagaman|The Medicare|Arden|Brian|Philip|Costello &|Rapid|Alexander|East|Premium|AAS|All Claim|Jose Garcia|Jeffrey|Telecia|Jonathan|Lexington)/, '<br /><strong>')
        html = html.toString().replaceAll(/<strong>(?=Carmen (?:Mason|LeBron)|, Jeffrey Gibbs|Clear|Rapid Release|Insureco Agency|American Bankers|Federal Hill)/, '<br /><strong>')
        html = html.toString().replaceAll(/(?:<strong>&nbsp;<\/strong>)/, '')
        html = html.toString().replaceAll(/NY<\/span><span class="style5"><br \/>/, 'NY<br />')
        html = html.toString().replaceAll(/(?<=\s[A-Z]{2})<strong> <\/strong><br \/>/, '<br />')
        html = html.toString().replaceAll(/\s&nbsp;&nbsp;/, "")
        html = html.toString().replaceAll(/<em>\.<\/em>,/, ".")
        html = html.toString().replaceAll(/<em>, <\/em>/, ", ")
        html = html.toString().replaceAll(/(?=Oxford Health|William O\.|Jack J\.)/, '<strong>')
        html = html.toString().replaceAll(/<\/font><font.*Tahoma">/, '')
        html = html.toString().replaceAll(/<\/strong>,?\s*?((?:Egg Harbor Twp\.|Northfield), NJ)/, ', $1')
        html = html.toString().replaceAll(/<\/strong><strong>Efren Palmer/, 'Efren Palmer')
        html = html.toString().replaceAll(/<\/strong>(Ft\. Wort)/, '$1')
        html = html.toString().replaceAll(/ Sharp, IV,/, ' Sharp')
        html = html.toString().replaceAll(/(?m)<strong>$|and (?=Jerome Sacco)|(?=GMI NA)/, "<br /><strong>")
        html = html.toString().replaceAll(/\s{2}(?<!\n)/, " ")

        return html
    }

    def sanitizeHtml(def html) {
        def addtext = /(?:Final\s*Order[s]?|Orders to Show Cause|Notices and Final Orders|Consent Orders|Final and Miscellaneous Orders|Conditional Order Revoking License|Matters Resolved Without Determination of Violation|Cease and Desist Order|Amended Order to Show Cause)/
        def addtext1 = /(?:Miscellaneous Orders|Notices & Final Orders|Order Denying Stay Pending Appeal|Order Suspending License Pending Completion of Administrative Proceedings|Order to Show Cause|Orders Suspending License Pending Administrative Proceedings)/
        def addtext2 = /(?:Order to Show Cause Seeking Immediate License Suspension)/

        html = html.toString().replaceAll(/(?s)^.*?December(?:<\/a><\/font><\/font>(?:[<\/font>]+)?<br \/>|<\/strong><\/td>)/, "")
        html = html.toString().replaceAll(/<strong>Underwriters at Lloyd&rsquo;s, London, Syndicate 3622, Beazley Furlonge Limited, <\/strong>London, UK<br \/>/, "<strong>Underwriters at Lloyd's London, Syndicate 3622, Beazley Furlonge Limited, London, UK<br />")
        html = html.toString().replaceAll(/.*Genworth Life Insurance Company.*/, "<strong>Genworth Life Insurance Company, Richmond, VA<br />")
        html = html.toString().replaceAll(/.*Business and Individual Insurance Services, Inc. Paramount, CA.*/, "<strong>Business and Individual Insurance Services, Inc. Paramount, CA<br />")
        html = html.toString().replaceAll(/(?<=<br \\/>)\s*<\\/span><br \\/>\s*/, "\n<br />\n")
        html = html.toString().replaceAll(/<\/strong>Santa<strong> <\/strong>Ana/, 'Santa Ana')
        html = html.toString().replaceAll(/(?s)^.*?<\/a><span class="style11">December<\/span><\/font><\/strong><\/td>\s*<\/tr>/, "")
        html = html.toString().replaceAll(/(?s)^.*Please Note:.*Division of Insurance\.<\/td>/, "")
        html = html.toString().replaceAll(/<br \/>\n<br \/>\n(?=<\/p>)/, "")
        html = html.toString().replaceAll(/(?<=funds\.|insurance\.)&nbsp;&nbsp;\s?/, "")
        html = html.toString().replaceAll(/Elizabeth<strong>, <\/strong>NJ/, "Elizabeth, NJ")
        html = html.toString().replaceAll(/Ltd\., <\/strong>Kingston,/, "Ltd., Kingston")
        html = html.toString().replaceAll(/(?<=penalty\.)<br \/>|\(not licensed\)/, "")
        html = html.toString().replaceAll(/<strong>(?:January|February|March|April|June|July|August|September|November|December)<\/strong><\/span>.*/, "<br />\n")
        html = html.toString().replaceAll(/(?s)(?<=<\/table><\/td>)\s+<!.*?End of Blue Navigation Menu.*/, "")
        html = html.toString().replaceAll(/.*<strong style=\"color: #06C[;]?\">(?:<br \/>)?\s*(?:$addtext|$addtext1|$addtext2)\s*<\/strong><\/span>/, "")
        html = html.toString().replaceAll(/.*<strong style="font-weight: bold; color: #06C;.*>\s*(?:$addtext|$addtext1|$addtext2)\s*<\/strong><\/span><\/p>/, "")
        html = html.toString().replaceAll(/<(?:span|strong|p).*color: #06C;?">(?:<br \/>)?\s*(?:$addtext|$addtext1|$addtext2)\s*<\/(?:strong|span|p|br \/)>/, "")
        html = html.toString().replaceAll(/(?:<p>|<strong>)<span class="style2 style1 style5 style10 style6" style="color: #06C">(?:$addtext|$addtext1|$addtext2)(?:<\/span><\/strong>|<\/strong><\/span>)/, "")
        html = html.toString().replaceAll(/(?:<p>|<strong>)\s*(?:$addtext|$addtext1|$addtext2)\s*(?:<\/span><\/strong>|<\/strong><\/span>|<\/strong>(?:<br \/>)?)/, "")
        html = html.toString().replaceAll(/([A-Z][a-z]{2,11})\s*(?=\d{1,2})/, '$1 ')
        html = html.toString().replaceAll(/May13, 2008/, " May 13, 2008")
        html = html.toString().replaceAll(/July 20, 1010/, " July 20, 2010")
        html = html.toString().replaceAll(/(?<=<\/strong>Cranford, NJ)\s*(?=<br \\/>)/, "")
        html = html.toString().replaceAll(/\(NJ PURE\)/, "d/b/a NJ PURE")
        html = html.toString().replaceAll(/Ho-Ho-Kus/, "Ho Ho Kus")
        html = html.toString().replaceAll(/V<\/strong>ictor/, "Victor")
        html = html.toString().replaceAll(/G<\/strong>allagher/, "Gallagher")
        html = html.toString().replaceAll(/Febuary/, "February")
        html = html.toString().replaceAll(/(?<=(?:Broomall|Reading),) OA/, " PA")
        html = html.toString().replaceAll(/&amp;(?=\s[A-Z])/, "&")
        html = html.toString().replaceAll(/<\/b>\., Conshohocken/, "., Conshohocken")
        html = html.toString().replaceAll(/(?:a\/k\/a|\sdba\s|d\/b\/a\s*|d\/b\/s\/|<\/strong><br \/>\s*<strong>&nbsp;&nbsp; t\/a)/, " d/b/a ")
        html = html.toString().replaceAll(/.*<font color="#0066CC" face="Verdana, Arial, Helvetica.*/, "")
        html = html.toString().replaceAll(/, James Blumetti <\/strong>and<strong>/, "</strong>\n<strong>James Blumetti</strong>\n<strong>")
        html = html.toString().replaceAll(/, Gerald Connor <\/strong>and /, "</strong>\n<strong>Gerald Conner</strong>\n<strong>")
        html = html.toString().replaceAll(/(?<=Corporation|Co\.|Inc\.|Chang|[A-Z]\. Uribe|Bigica|LLC)(?:,|\sand) (?=Nicholas|Inter-America|Kathleen|Maurice|New Age|Liberty|Excelsior|The Ohio|West American|American|Kevin J\.|Horizon|Jose D\.|Joseph|Susan)/, "</strong>\n<strong>")
        html = html.toString().replaceAll(/(?<=Major|Harrison|Machado|Morgan|Hohn|Rocco|Kelly|Boxman|Montemurro|Saldana|Leonard|Sebina)(?:,|\sand) (?=Victoria|Kelly|John|24 Seven|Kevin|Christopher|C & S|My Closing|Certified|Any Hour|Capital|Trans)/, "</strong>\n<strong>")
        html = html.toString().replaceAll(/(?<=Padilla|Fleming|McCoy|Nelson|Kim|Calinog)(?:,|\sand) (?=Padilla|Fleming|Brook|Dependable|Sonamu|TLC)/, "</strong>\n<strong>")
        html = html.toString().replaceAll(/,\s*(?=Brian Mohen|Kelly Bolton|Robert Miller|Sigmar Hessing|and Norman|James Struss|Philip Teseo|Philip Eneo|Virginia Larsen|Rafael Agliata|F & M)/, "</strong>\n<strong>")
        html = html.toString().replaceAll(/,(?: |\s*)(?:<\/strong>)/, ", </strong>")
        html = html.toString().replaceAll(/<\/strong>, <strong>Inc./, " Inc.</strong>,<strong>")
        html = html.toString().replaceAll(/<strong>&nbsp; <\/strong>|(?<=Warren)<\/strong>\s*(?=, [A-Z])/, "")
        html = html.toString().replaceAll(/<\/font><\/strong><font face="Verdana, Arial, Helvetica-Conth, Tahoma">/, " ")
        html = html.toString().replaceAll(/.*<\/a><\/strong><\/span><strong>(?:September)<\/strong><\/td>\s*<\/tr>/, " ")
        html = html.toString().replaceAll(/<font color=\"#0066CC\" size=\"2\" face=\"Verdana, Arial, Helvetica-Conth, Tahoma\"><strong>(?:<font size=\"2\">)?\s*(?:$addtext|$addtext1|$addtext2)\s*(?:<\/font>)?<\/strong><\/font><\/p>/, " ")
        html = html.toString().replaceAll(/<(?:span|strong|p).*color: #06C;?.*?>(?:<br \/>)?\s*(?:$addtext|$addtext1|$addtext2)\s*<\/(?:strong|span|p|br \/)>/, "")
        html = html.toString().replaceAll(/<p.*"color: #06C">Notices & Final Orders<\/strong>/, "")
        html = html.toString().replaceAll(/<p.*(?:$addtext|$addtext1|$addtext2).*/, "")
        html = html.toString().replaceAll(/(?s)^.*<\/strong><\/a><\/font><\/p>\s*(?=<table width="100%")/, "")
        html = html.toString().replaceAll(/<td.*(?:(?:Final|Consent) Orders|Penalty).*<\/font><\/td>/, "")
        html = html.toString().replaceAll(/(?s)<!-- InstanceEndEditable --><!-- #BeginLibraryItem "\\/web\\/webdocs\\/dobi.*html>$/, "")
        html = html.toString().replaceAll(/(<tr>\s+.*?Christopher Ritchie)/, '<br />\n$1')
        html = html.toString().replaceAll(/(<br \/>\s*James M\. Cowgill)/, '\n<br />\n$1')
        html = html.toString().replaceAll(/(<tr>\s*<td.*?Joseph Bigica|<br \/>\s*Lincoln National)/, '<br />\n<br />\n$1')
        html = html.toString().replaceAll(/<br \/>\s*<\/strong><\/span><span class="style4"><strong><br \/>/, '<br />\n<br />')
        html = html.toString().replaceAll(/(<span class="style4"><strong>Solomon Weinstein)/, '\n<br />$1')
        html = html.toString().replaceAll(/<strong><br \/>/, '<strong>\n<br />')
        html = html.toString().replaceAll(/<\/strong>Hartford,<strong> <\/strong>CT/, 'Hartford, CT')
        html = html.toString().replaceAll(/<\/strong> Burlington and Mt\./, 'Burlington and Mt.')
        html = html.toString().replaceAll(/\(a New Jersey Corporation\)/, 'a New Jersey Corporation')
        html = html.toString().replaceAll(/(<span class="style5"><strong>Leland Grossman and)/, '<br />\n$1')
        html = html.toString().replaceAll(/<strong>(?=Jose Garcia)/, '<br /><strong>')
        html = html.toString().replaceAll(/\(NJ\), Inc, and United Healthcare/, ', Inc.<br /><strong>United Healthcare')
        html = html.toString().replaceAll(/\(nee Lakesia S. Durant\)/, 'd/b/a nee Lakesia S. Durant')
        html = html.toString().replaceAll(/<font.*?><b>/, '</strong>')
        html = html.toString().replaceAll(/<strong>, <\/strong>((?:NJ|NY))/, ', $1')
        html = html.toString().replaceAll(/(?=<div align="left">.*<strong>James Adams)/, '<br />\n<br />')
        html = html.toString().replaceAll(/<p align="left" class="MsoNormal"><font.*Verdana">(?:<strong>)?(?=First|Four|Michael R\.|Joseph B\.|Christopher R\.|Polina)/, '<strong>')
        html = html.toString().replaceAll(/<\/font><\/strong><font.*Verdana">/, '')
        html = html.toString().replaceAll(/<br \/>\s*(?=Anton Adjustment|Quick Title|Renita|Premier|State Title|John P\.|Business and)/, '<strong>')
        html = html.toString().replaceAll(/(?m)^&nbsp;<br \/>/, '<br />')
        html = html.toString().replaceAll(/<\/strong>NY, NY/, 'New York City, NY')
        html = html.toString().replaceAll(/<br \/>\s*(?=<strong>Thomas A. Lamparillo)/, '\n<br />')
        html = html.toString().replaceAll(/<strong>(?=Cesar Marin)/, '<br /><strong>')
        html = html.toString().replaceAll(/<\/b>(?=\s*\w+?\s?[A-Za-z]+, [A-Z]{2}\s*<)/, '')
        html = html.toString().replaceAll(/(?<=Agency) (?=Framingham)/, ", ")
        html = html.toString().replaceAll(/08505-4703/, "")

        return html.trim()
    }

    def sanitizeBlock(block) {
        block = block.toString()
        block = block.toString().replaceAll(/(?m)^(?:<\/strong><strong>|<(?:p|\font)><\/strong><strong>)/, '<strong>')
        block = block.toString().replaceAll(/(?m)(?:<\/font><br \/>|<\/font><\/strong><br \/>)$/, '<br />')
        return block.trim()
    }

    def sanitizeData(html) {

        def sanitizeRegex = /(?s)(?:(?:<\/font>)+?(?:<\/strong>|<br\s\/>).*?)((?:<strong>|<br\s\/>)[\w\s].*)/
        def sanitizeMatcher = html =~ sanitizeRegex
        if (sanitizeMatcher.find()) html = sanitizeMatcher.group(1)

        def sanitizeRegex1 = /(?s)(?:class).*?((?:>|<strong>|<br\s\/>)[\w\s].*)/
        def sanitizeMatcher1 = html =~ sanitizeRegex1
        if (sanitizeMatcher1.find()) html = sanitizeMatcher1.group(1)

        def sanitizeRegex2 = /(?s)class.*?(<strong>[\w\s].*)/
        def sanitizeMatcher2 = html =~ sanitizeRegex2
        if (sanitizeMatcher2.find()) html = sanitizeMatcher2.group(1)

        def sanitizeMatcher3 = html =~ sanitizeRegex2
        if (sanitizeMatcher3.find()) html = sanitizeMatcher3.group(1)

        def sanitizeRegex4 = /(?s)(?:<br\s\/>\s+?(<br\s\/>.*))/
        def sanitizeMatcher4 = html =~ sanitizeRegex4
        if (sanitizeMatcher4.find()) html = sanitizeMatcher4.group(1)

        html = html.toString().replaceAll(/<strong>is alleged to have\s?(?:engaged|failed)?<\/strong>/, "")
        html = html.toString().replaceAll(/\s{2}(?<!\n)/, " ")

        return html
    }

    def captureLink(html, url) {
        def entityUrlList = []
        def link
        if (url.toString().contains("insfines")) {
            def linkRegex = /href="(.*?)"/
            def linkMatcher = html =~ linkRegex

            while (linkMatcher.find()) {
                def linkPostfix = linkMatcher.group(1)
                if (linkPostfix.startsWith('/dobi/')) {
                    link = root + linkPostfix
                } else if (linkPostfix.startsWith('enforcement')) {
                    link = rootInsr + linkPostfix
                }
                entityUrlList.add(link)
            }
        } else {
            def linkRegex = /href="(.*?)"/
            def linkMatcher = html =~ linkRegex
            def linkPrefix = 'ocf/enforcement/'
            if (linkMatcher.find()) {
                def linkPostfix = linkMatcher.group(1)
                if (linkPostfix.startsWith('2')) {
                    linkPostfix = linkPrefix + linkPostfix
                } else if (linkPostfix.startsWith('enforcement')) {
                    linkPostfix = 'ocf/' + linkPostfix
                }
                link = pdfLink + linkPostfix
            }
            entityUrlList.add(link)
        }
        return entityUrlList
    }

    def captureDate(html) {
        def dateRegex = /(?m)(?!November\s12,\s2004)[A-Z][a-z]{2,8},?\s*?\d{1,2},\s*?\d{4}/
        def dateMatcher = html =~ dateRegex
        if (dateMatcher.find()) return dateMatcher.group()
    }


    def setDefaultDate(def link) {
        def date = ""

        if (link.toString().contains("22")) {
            date = "2022"
        } else if (link.toString().contains("21")) {
            date = "2021"
        } else if (link.toString().contains("20")) {
            date = "2020"
        } else if (link.toString().contains("19")) {
            date = "2019"
        } else if (link.toString().contains("18")) {
            date = "2018"
        } else if (link.toString().contains("17")) {
            date = "2017"
        } else if (link.toString().contains("16")) {
            date = "2016"
        } else if (link.toString().contains("15")) {
            date = "2015"
        } else if (link.toString().contains("14")) {
            date = "2014"
        } else if (link.toString().contains("13")) {
            date = "2013"
        } else if (link.toString().contains("12")) {
            date = "2012"
        } else if (link.toString().contains("11")) {
            date = "2011"
        } else if (link.toString().contains("10")) {
            date = "2010"
        } else if (link.toString().contains("09")) {
            date = "2009"
        } else if (link.toString().contains("08")) {
            date = "2008"
        } else if (link.toString().contains("07")) {
            date = "2007"
        } else if (link.toString().contains("06")) {
            date = "2006"
        } else if (link.toString().contains("05")) {
            date = "2005"
        } else if (link.toString().contains("04")) {
            date = "2004"
        }

        return date.toString()
    }

    def capturePenalty(html) {
        def penaltyRegex = /(?is)(?:Penalt.*?|Fine.*?)(\$\d+(?:,?\d+|\.\d+)).*?</
        def penaltyMatcher = html =~ penaltyRegex
        if (penaltyMatcher.find()) return penaltyMatcher.group(1)
    }

    def sanitizeName(entity) {
        entity = entity.toString().replaceAll(/,\s*(Respondent|President\/CEO|Owner|Officer|Compliance\sOfficer|President|CEO|Managing\sMember|Executive\sVice\sPresident|Secretary)/, " ").trim()
        entity = entity.toString().replaceAll(/^(?:Consent Order|&gt;|Matters Resolved without Determination of Violation|Order Denying Motion to Vacate|Order Denying Stay)$/, " ").trim()
        entity = entity.toString().replaceAll('Respondent', " ").trim()
        entity = entity.toString().replaceAll(/^(?:and\s|Fort Dix|t\/a|July|October)$/, " ").trim()
        entity = entity.toString().replaceAll(/^(?:and\s|t\/a)/, " ").trim()
        entity = entity.toString().replaceAll(/(?:Fort Dix|\sand)$/, " ").trim()
        entity = entity.toString().replaceAll(/&nbsp;/, "").trim()
        entity = entity.toString().replaceAll(/&amp;/, "&").trim()
        entity = entity.toString().replaceAll(/,\s*(?=Inc|Inc\.|LLC|Jr)/, " ").trim()
        entity = entity.toString().replaceAll(/&rsquo;/, "\'").trim()
        entity = entity.toString().replaceAll(/&ndash;/, " ").trim()
        entity = entity.toString().replaceAll(/,\s*?$/, "").trim()
        entity = entity.toString().replaceAll(/\s+/, " ").trim()
        entity = entity.toString().replaceAll(/\(.+$/, " ").trim()
        entity = entity.toString().replaceAll(/,/, "").trim()
        entity = entity.toString().replaceAll(/-$/, "").trim()
        return entity.trim()
    }

    def fixAddress(address) {
        address = address.toString().replaceAll(/Toms River, Marlboro/, " Toms River, NJ, Marlboro, NJ").trim()
        address = address.toString().replaceAll(/Joint Base MDL/, "Joint Base").trim()
        return address.trim()
    }

    def sanitizeAddress(address) {
        address = address.toString().replaceAll(/&amp;/, "&").trim()
        address = address.toString().replaceAll(/\s+/, " ").trim()
        address = address.toString().replaceAll(/Toms River, Marlboro/, " Toms River, NJ, Marlboro, NJ ").trim()
        address = address.toString().replaceAll(/(\s[A-Z]{2});?$/, '$1').trim()
        if (address.contains("UK")) {
            address = address
        } else {
            address = address.toString().replaceAll(/(?<=\s[A-Z]{2})$/, ", UNITED STATES").trim()
        }
        address = address.toString().replaceAll(/(?i)^and\s|\sand$/, "").trim()
        address = address.toString().replaceAll(/(?i)^null$/, "").trim()
        return address.trim()
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().replaceAll("City", "").replaceAll("city", "").replaceAll("Province", "").replaceAll("People's Republic of", "")
            .replaceAll(",\\s*,", ",").replaceAll(",\\s*,\\s*,", ",")
        street = street.toString().replaceAll(/(?ism),\s*$/, "")
        street = street.toString().replaceAll("United State of", "")
        street = street.toString().replaceAll("The Republic of", "")
        street = street.toString().replaceAll("\\(,Canada,Canada\\)", "")
        street = street.toString().replaceAll(/(?ism)Managing Director.*?ltd.?/, "")
        street = street.toString().replaceAll(/(?s)\s+/, " ").trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        street = street.toString().replaceAll(/\(/, "").trim()
        return street
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}