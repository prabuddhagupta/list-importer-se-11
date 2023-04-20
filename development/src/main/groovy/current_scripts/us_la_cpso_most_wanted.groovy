package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils

import java.text.SimpleDateFormat


context.setup([connectionTimeout: 10000000, socketTimeout: 10000000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_La_Cpso_Most_Wanted script = new Us_La_Cpso_Most_Wanted(context)
script.initParsing()

class Us_La_Cpso_Most_Wanted {
    final addressParser
    final entityType
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final ScrapianContext context

    final def CITIES = ["Shreveport", "Texarkana", "West Monroe", "Monroe", "New Orleans", "Torrington", "Worland", "Mesquite", "Joaquin", "Natchitoches", "Titusville", "Dailey City",
                        "Leander", "Enid", "Forth Wayne", "Purcell", "Denton", "Tyler", "Marshall", "Ruston", "Carthage", "Mansfield", "Jefferson", "St George",
                        "Grand Prairie", "Hooks", "Gladewater", "Mcleod", "Waskom", "Springhill", "Gilmer", "Winnsboro", "Maumelle", "Fouke", "Euless", "Lockesburg", "Tallulah",
                        "Gulf Port", "New Boston", "oakdale", "Burleson", "Rayne", "Hallsville", "Center", "Thibodeaux", "Lavon", "Baton Rogue", "Garrison", "White Oak", "Baker",
                        "Jennings", "Corsicana", "Kenner", "Killen", "Bryant", "Wylie", "Eunice", "Cypress", "Mt Pleasant", "Camdon", "Whitehouse", "Sanger", "Henagar", "Pineville", "Denham Springs",
                        "Anita", "Sandy", "Magee", "Linden", "Scottsville", "Bivins", "Russellville", "Winters", "Trinity", "Watauga", "Picayunne", "Cabot", "San Augustine", "Foreman", "Broken Bow",
                        "Hillsboro", "Mart", "Ducan", "Cedar City", "Atlantic City", "Zachary", "Jeanerette", "Goodrich", "Allen", "Tatum", "Picayune", "Austell", "Duncanville", "Tolleson", "Okalohoma",
                        "Humble", "Amity", "Dequincy", "Vidor", "Eagle Lake", "Overton", "Diana", "Wiemar", "Timpson", "Hazelhurst", "Ashdown", "West Linn", "Hazlehurst", "Rockaway Beach", "Brookhaven",
                        "Terrell", "Gonzales", "Azle", "Grandview", "Nederland", "Ennis", "Port Lucie", "Rialto", "Pineland", "Idabel", "Millington", "Desoto", "Newberry", "Grambling", "Eldorado",
                        "Dewitt", "Meriden", "Wetumpka", "Lewisville", "Conyers", "Pooler", "Balch Springs", "Aztec", "Pinellas Park", "San Antionio", "Ellijay", "Kaufman", "Marlow", "War Acres",
                        "La Marque", "Fernandina", "Marksville", "Lone Star", "Mcdonough", "Yellville", "Bachsprings", "Emmet", "Beckville", "Taylor", "Alamo", "Rowlett", "Hallandale", "Ovilla",
                        "Hidalgo", "San Benito", "Nash", "St Louis", "Wills Point", "Saluda", "Dixie", "Cedar Hill", "Daytona", "Hubert", "Paramount", "Colfax", "San Elizario", "Wake Village",
                        "Ft Meyers", "Winnfield", "Breaux Bridge", "Coweta", "Springdale", "Balch Spring", "Lilburn", "Jourdanton", "Spring Hill", "Water Valley", "Strong", "Wagoner", "Gerald",
                        "Joshua", "Palestine", "Algonac", "Point Blank", "Tuscoloosa", "Rockwall", "Longview", "Princeton", "Fort Wayne", "Camden", "Baton Rouge", "Greenwood", "Bethany", "Detroit",
                        "Louisville", "Bradley", "Stratford", "Durant", "Livingston", "Fort Smith", "Richmond", "Pattison", "North Little Rock", "Little Rock", "Minden", "Irvine", "Alexandria", "Garland",
                        "Las Cruces", "Lakeland", "Boonville", "Benton", "Lancaster", "Clinton", "Fulton", "Blocton", "Carrollton", "Murfreesboro", "Lubbock", "Prairieville", "La Place", "Haynesville",
                        "Henderson", "Wichita", "Abilene", "Durham", "Gary", "Cleburne", "Toledo", "Pensacola", "Portsmouth", "Lake Jackson", "Tuscaloosa", "Eros", "Lake Charles", "Jasper", "Vicksburg",
                        "Temple", "Hot Springs", "Knoxville", "Geneva", "Buckhannon", "Cleburne", "Vancouver", "Sacramento", "Natchez", "Tucson", "Dayton", "Prescott", "Magnolia", "Huntington", "Monticello",
                        "Montgomery", "Neenah", "Athens", "Keene", "Brownsville", "Thibodaux", "Marion", "Birmingham", "Beaumont", "Clarksville", "Anderson", "Canton", "Bastrop", "Washington dc", "Lafayette",
                        "Madison", "Clearwater", "Elizabeth", "Pipestone", "El Reno", "Gallatin", "Fayetteville", "Santa Fe", "Winona", "Lufkin", "Springfield", "Jonesboro", "Randolph", "Neosho", "Rt Houston",
                        "Duncan", "Canton", "Killeen", "Paris", "San Angelo", "Alexander", "Amarillo", "Franklin", "Greenville", "Fort Stockton", "New Iberia", "Huntsville", "Lincoln", "Baytown", "Hope", "Compton",
                        "Sweetwater", "College Park", "Arlington", "Cushing", "Princton", "Waco", "Nacogdoches", "Bakersfield", "Jackson", "Lodi", "Mount Vernon", "Mission", "Tahlequah", "Flint", "Woodward", "Kilgore",
                        "Pasadena", "Mobile", "Van Buren", "Houma", "Hammond", "Baltimore", "Sherman", "Norman", "Monett", "Sulphur Springs", "Aurora", "Calhoun", "Corinth", "Perry", "Weatherford", "Pittsburg", "Pine Bluff", "Vernon",
                        "Sherman", "Clayton", "Port Arthur", "Opelousas", "Bowling Green", "Arcadia", "Saginaw", "Oxford", "Gretna", "Odessa", "Port Alto", "Port Barree", "Victoria", "Fresno", "Harrison", "Napeleonville", "Council Bluff",
                        "Savannah", "Puyallup", "Naples", "Moore", "Anchorage", "Rochester", "Marietta", "Kokomo", "Sun Valley", "Muskegon", "Roseville", "Laurel", "Topeka", "El Dorado", "West Point", "Kingsville", "West Helena", "Homer",
                        "Bedford", "Grand Island", "Fort Fairfield", "Burlington", "Mesa", "Portland", "Chanute", "San Leandro", "Hobbs", "Plattsmouth", "Columbus", "Havre de Grace", "Bogalusa", "Orange", "Conway", "Hollywood",
                        "Port Gibson", "Florence", "Grants", "San Bernardino", "Weston", "Macon", "Uvalde", "Yorba Linda", "Rock Island", "Hartford City", "Wheatland",

                        "Daingerfield","Bellchase","Sarepta","Pleasant Hill","Spring Montgomery","Vivian","Mooringsport","Hosston","Keithville","Mira","Haughton","Ross","Campti","Castle Rock","Coushatta","Stonewall",
                        "Fort Walton Beach","West Monroe","Cotton Valley","Zwolle","Warsaw","Plain Dealing","Cotton Valley","Rowlett","Sibley","Coushatta","Ringgold"
    ]

    //https://www.caddosheriff.org/warrants/

    def mainUrl = "https://www.caddosheriff.org/warrants/"
    def requestUrl = "https://www.caddosheriff.org/warrants/ajax/func-warrants.php"

    Us_La_Cpso_Most_Wanted(context) {
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.updateCities([US: CITIES])
        this.context = context
    }

    def initParsing() {
        def html

        html = invokeUrl(mainUrl)
        // html=invokeUrl("https://www.caddosheriff.org/warrants/?lastNameChar=A")
        handleHtml(html, mainUrl)
        html = html.replace(/(?s)^.*(?=<div class="card-body ">)/, "")
        html = html.replace(/(?s)<table id="tblBooking".*$/, "")

        def url
        def urlSet = [] as Set
        def urlMatcher = html =~ /href="(.*?)"/


        while (urlMatcher.find()) {
            url = mainUrl + urlMatcher.group(1)
            urlSet.add(url.toString().trim())
        }
        urlSet.each { urlLink ->
            def urlHtml = invokeUrl(urlLink)
            handleHtml(urlHtml, urlLink)
        }
    }

    def handleHtml(def html, def entityUrl) {

        def personBlock
        def personBlockMatcher = html =~ /(?s)(<tr id="inmate.*?<\/tr>)/

        while (personBlockMatcher.find()) {
            personBlock = personBlockMatcher.group(1)
            handlePersonBlock(personBlock, entityUrl)
        }
    }

    def handlePersonBlock(def blockRow, def entityUrl) {

        def id
        def bookingId
        def idAndBookingIdMatcher = blockRow =~ /(?s)onclick="getInmatePreview\((\d+)\)">.*?id="bookingID.*?value="(\d+)?">/

        def date
        def dateMatcher = blockRow =~ /(\d{2,4}\/\d{1,2}\/\d{1,2})/
        if (dateMatcher.find()) {
            date = dateMatcher.group(1)
        }

        def personHtml
        if (idAndBookingIdMatcher.find()) {
            id = idAndBookingIdMatcher.group(1)
            bookingId = idAndBookingIdMatcher.group(1)
            def params = getParams(bookingId, id)
            personHtml = invokePost(requestUrl, params)
        }
        handlePersonHtmlData(personHtml, date, entityUrl)
    }

    def handlePersonHtmlData(def personHtml, def date, def entityUrl) {

        def entityName
        def entityNameMatcher

        if ((entityNameMatcher = personHtml =~ /lastName":"(.*?)","firstName":"(.*?)","middleName":"(.*?)"/)) {
            entityName = entityNameMatcher[0][2] + " " + entityNameMatcher[0][3] + " " + entityNameMatcher[0][1]
            entityName = sanitineName(entityName)
        }

        def gender
        def race
        def genderAndRaceMatcher
        if ((genderAndRaceMatcher = personHtml =~ /"race":"(.*?)","sex":"(.*?)"/)) {
            race = genderAndRaceMatcher[0][1]
            gender = genderAndRaceMatcher[0][2]
        }

        def dob
        def dobMatcher
        if ((dobMatcher = personHtml =~ /"dob":"(.*?)"/)) {
            dob = dobMatcher.group(1)
            dob = dob.toString().replaceAll(/[\\]/, "")
        }

        def height
        def heightMatcher
        if ((heightMatcher = personHtml =~ /"height":"(.*?)"/)) {
            height = heightMatcher[0][1].toString().trim()
            height = height.replaceAll(/^0$/, "")
        }

        def weight
        def weightMatcher
        if ((weightMatcher = personHtml =~ /"weight":"(.*?)"/)) {
            weight = weightMatcher[0][1].toString().trim()
            weight = weight.replaceAll(/^0$/, "")
        }

        def address
        def addressMatcher

        if ((addressMatcher = personHtml =~ /"city":"(.*)?","state":"(.*?)","zip":"(.*?)",/)) {
            address = addressMatcher[0][1] + ", " + addressMatcher[0][2] + " " + addressMatcher[0][3]

        }

        def imageUrl
        def imageUrlMatcher
        if ((imageUrlMatcher = personHtml =~ /<img src="\/&quot;(.*?)\/&quot;"/)) {
            imageUrl = imageUrlMatcher[0][1]
        }

        createEntity(entityName, dob, height, weight, gender, race, imageUrl, address, date, entityUrl)
    }


    def createEntity(def name, def dob, def height, def weight, def gender, def race, def imageUrl, def address, def eventDate, def entityUrl) {
        def entity = null

        ScrapeEvent event = new ScrapeEvent()
        def eventDescription = 'This entity appears on the Louisiana Caddo Parish Sheriffs Office list of Most Wanted.'
        ScrapeAddress scrapeAddress = new ScrapeAddress()

        if (!address.toString().contains("null") && address!=null ) {
            address = sanitizeAddress(address)

            if (!address.toString().isEmpty() ) {
                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])

            } else {
                scrapeAddress.setProvince('Louisiana')
                scrapeAddress.setCountry('United States')
            }
        }

        if (!name.toString().equals("null")) {
            entity = context.findEntity(["name": name])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType("P")
            }

            if (race) {
                entity.addRace(race)
            }
            if (height) {
                entity.addHeight(height)
            }
            if (weight) {
                entity.addWeight(weight)
            }
            if (gender) {
                entity.addSex(gender)
            }
            if (imageUrl) {
                entity.addImageUrl(imageUrl)
            }

            if (eventDate) {
                def remark
                eventDate = context.parseDate(new StringSource(eventDate), ["yyyy/MM/dd"] as String[])

                def currentDate = new Date().format("MM/dd/yyyy")
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy")
                def date1 = simpleDateFormat.parse(currentDate)
                def date2 = simpleDateFormat.parse(eventDate)

                if (date2.compareTo(date1) > 0) {
                    remark = eventDate
                    eventDate = null
                    entity.addRemark(remark)
                } else {
                    event.setDate(eventDate)
                }
            }
            if (dob) {
                def dateOfBirth = context.parseDate(new StringSource(dob), ["MM/dd/yyyy"] as String[])
                if (dateOfBirth) {
                    entity.addDateOfBirth(dateOfBirth)
                }
            }
            event.setDescription(eventDescription)
            event.setDate(eventDate)

            if (scrapeAddress)
                entity.addAddress(scrapeAddress)

            entity.addEvent(event)
            entity.addUrl(entityUrl)
        }
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().replaceAll(/(?s)\s+/, " ").trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        street = street.toString().replaceAll(/\(/, "").trim()

        return street
    }

    def sanitineName(entityName) {
        entityName = entityName.toString().trim()
        entityName = entityName.toString().replaceAll(/\snull\s/, " ")
        entityName = entityName.toString().replaceAll(/\s+/, " ")
        entityName = entityName.toString().replaceAll(/,\s*/, " ")
        return entityName.toString().trim()
    }

    def sanitizeAddress(def address) {
        address = address.toString().trim()
        address = address.toString().replaceAll(/,$/, "")
        address = address.toString().replaceAll(/(?i)(?<=\s(?:LA|TX|AZ|AR|OK|NM|FL|TN|WA|MS|UT|MO|KS|NV|WI|OH|AL|GA|MN|CT|OR|MI|CA|IA|VA|NC|MD|SD|NJ|NB|WV|KY|ME|IL|AK|NY|SC|IN|ND|CO))(\s\d+)\s*/, '$1, USA').trim()
        address = address.toString().replaceAll(/(?i)(?<=\s(?:LA|TX|AZ|AR|OK|NM|FL|TN|WA|MS|UT|MO|KS|NV|WI|OH|AL|GA|OR|MI|CA|IA|VA|NC|MD|NJ|SD|NB|WV|KY|ME|IL|AK|NY|SC|IN|ND|CO))\s*$/, ', USA')
        address = address.toString().replaceAll(/(?:Shreveort|Shreevport|Shrevport|Shrevpeort|S'port|Shereveport|S,port|Shreveprt|Shrevepoart|Shreveoport|Shareveport|Shrveport|Shreveprot)/, "Shreveport")
        address = address.toString().replaceAll(/(?<=(?:Missouri City|Liberty City)), TX/, ", TEXAS")
        address = address.toString().replaceAll(/(?<=Bossier)\s*,|\sCt/, " City,").trim()
        address = address.toString().replaceAll(/Kelleen/, "Killeen").trim()
        address = address.toString().replaceAll(/Basrtop/, "Bastrop").trim()
        address = address.toString().replaceAll(/Waracres/, "War Acres").trim()
        address = address.toString().replaceAll(/Niagra Falls/, "Niagara Falls ").trim()
        address = address.toString().replaceAll(/Lincoln, NB/, "Lincoln, NE").trim()
        address = address.toString().replaceAll(/(?i)Fort[h]?worth|Ft\.?\sWorth|Forth\sWorth/, "Fort Worth").trim()
        address = address.toString().replaceAll(/(?i)Mt.pleasant|Mt. Pleasant/, "Mt Pleasant").trim()
        address = address.toString().replaceAll(/(?i)haude de grace/, "Havre de Grace").trim()
        address = address.toString().replaceAll(/(?i)Forth Wayne/, "Fort Wayne").trim()
        address = address.toString().replaceAll(/(?i)Joquin,/, "Joaquin,").trim()
        address = address.toString().replaceAll(/(?i)Thibedaux|Thidodaux/, "Thibodaux").trim()
        address = address.toString().replaceAll(/Talequah/, "Tahlequah").trim()
        address = address.toString().replaceAll(/Durrant/, "Durant").trim()
        address = address.toString().replaceAll(/Mccleod/, "Mcleod").trim()
        address = address.toString().replaceAll(/Meridan/, "Meriden").trim()
        address = address.toString().replaceAll(/Locksburg/, "Lockesburg").trim()
        address = address.toString().replaceAll(/Russelville/, "Russellville").trim()
        address = address.toString().replaceAll(/Alexanderia/, "Alexandria").trim()
        address = address.toString().replaceAll(/(?:Tuscson|Tuscon)/, "Tucson").trim()
        address = address.toString().replaceAll(/Bivens/, "Bivins").trim()
        address = address.toString().replaceAll(/Prarie/, "Prairie").trim()
        address = address.toString().replaceAll(/Jouquin/, "Joaquin").trim()
        address = address.toString().replaceAll(/(?:Wakevilliage|Wafe Village)/, "Wake Village").trim()
        address = address.toString().replaceAll(/(?<=Midwest City,) OK/, " Oklahoma").trim()
        address = address.toString().replaceAll(/Grerald/, "Gerald").trim()
        address = address.toString().replaceAll(/Ft\.? Smith/, "Fort Smith").trim()
        address = address.toString().replaceAll(/Fortfairfield/, "Fort Fairfield").trim()
        address = address.toString().replaceAll(/Hazelhurst/, "Hazlehurst").trim()
        address = address.toString().replaceAll(/Cass City, MI/, "Cass City, Michigan").trim()
        address = address.toString().replaceAll(/Texerkana|Texarkansa/, "Texarkana").trim()
        address = address.toString().replaceAll(/Schulter/, "Schluter").trim()
        address = address.toString().replaceAll(/Nehawkane,\s*68413/, "Nehawkane, NE 68413, USA").trim()
        address = address.toString().replaceAll(/Plattsmouth, NB/, "Plattsmouth, NEBRASKA").trim()
        address = address.toString().replaceAll(/Kansas City, MO/, "Kansas City, MISSOURI").trim()
        address = address.toString().replaceAll(/Kansas City, KS/, "Kansas City, Kansas").trim()
        address = address.toString().replaceAll(/N Miami, FL 33161/, "North Miami, FL 33161").trim()
        address = address.toString().replaceAll(/^Texarkana$/, "Texarkana, TX, USA").trim()
        address = address.toString().replaceAll(/Murfresboro/, "Murfreesboro").trim()
        address = address.toString().replaceAll(/Sulpher Springs/, "Sulphur Springs").trim()
        address = address.toString().replaceAll(/Vualde/, "Uvalde").trim()
        address = address.toString().replaceAll(/Nachitoches/, "Natchitoches").trim()
        address = address.toString().replaceAll(/Waskon/, "Waskom").trim()
        address = address.toString().replaceAll(/Muskeon/, "Muskegon").trim()
        address = address.toString().replaceAll(/Emmett/, "Emmet").trim()
        address = address.toString().replaceAll(/Spng,/, " Springs,").trim()
        address = address.toString().replaceAll(/Lonestar/, "Lone Star").trim()
        address = address.toString().replaceAll(/Rockedge/, "Rockledge").trim()
        address = address.toString().replaceAll(/(?<=Hartford),(?=\sCT)/, " City,").trim()
        address = address.toString().replaceAll(/(?i),\s*(?=dc)/, " ").trim()
        address = address.toString().replaceAll(/711,/, "71101").trim()
        address = address.toString().replaceAll(/^,\s*71129$|(?:\-2113|\-1429|\-6049|\-4262)$|,\s*La\./, "").trim()
        address = address.toString().replaceAll(/(?i)Garrison|(?<=(?:Queen[s]?|Ore|Dodd|Texas)\sCity,)\s*TX/, " TEXAS").trim()
        address = address.toString().replaceAll(/(?<=(?:Bossier|Oil|Junction)\sCity,)\s*LA/, " LOUISIANA").trim()
        address = address.toString().replaceAll(/Washington DC\s*(?=\d+)/, "Washington dc, Washington ").trim()
        address = address.replaceAll(/Vivian,\s+71082/,"Vivian, 71082, USA")
        address = address.replaceAll(/\s+/," ")
        address = address.replaceAll(/(?<=, \w{2})(?= \d+)/,",")
        address = address.replaceAll(/Roxburry, MA, 02119/,"Roxburry, MA, 02119, USA")
        address = address.trim()
        return address
    }

    def getParams(bookingID, id) {
        def param = [:]

        param["id"] = id
        param["bookingID"] = bookingID
        param["action"] = 'getWarrantPreview'
        return param
    }

    def invokePost(def url, def paramsMap, def cache = false, def headersMap = [:], def tidy = true) {
        return context.invoke([url: url, tidy: tidy, type: "POST", params: paramsMap, headers: headersMap, cache: cache]);
    }
    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}