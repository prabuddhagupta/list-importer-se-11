package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 1000000000, socketTimeout: 1000000000, retryCount: 200, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_Wi_Dsps_Disciplinary_Decisions script = new Us_Wi_Dsps_Disciplinary_Decisions(context)
script.initParsing()


class Us_Wi_Dsps_Disciplinary_Decisions {

    final addressParser
    final entityType

    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final ScrapianContext context

    def headerUrl = "https://online.drl.wi.gov"
    def mainUrl = headerUrl + "/orders/searchorders.aspx"

    Us_Wi_Dsps_Disciplinary_Decisions(context) {
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context
    }

    def initParsing() {
        def html = invokeUrl(mainUrl)
        getParam(html)
    }

    def getParam(def html) {
        def headers = getHeader()
        def __VIEWSTATE, __VIEWSTATEGENERATOR, __LASTFOCUS, __EVENTTARGET, __EVENTARGUMENT

        def viewStateMatcher = html =~ /(?ism)<input type="hidden" name="__VIEWSTATE".*?VALUE="(.*?)"/
        def viewStateGeneratorMatcher = html =~ /(?ism)<input type="hidden" name="__VIEWSTATEGENERATOR".*?VALUE="(.*?)"/
        def lastFocusMatcher = html =~ /(?ism)<input type="hidden" name="__LASTFOCUS".*?VALUE="(.*?)"/
        def eventTargetMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTTARGET".*?VALUE="(.*?)"/
        def eventArgumentMatcher = html =~ /(?ism)<input type="hidden" name="__EVENTARGUMENT".*?VALUE="(.*?)"/
        if (viewStateMatcher.find()) {
            __VIEWSTATE = viewStateMatcher.group(1)
        }
        if (viewStateGeneratorMatcher.find()) {
            __VIEWSTATEGENERATOR = viewStateGeneratorMatcher.group(1)
        }
        if (lastFocusMatcher.find()) {
            __LASTFOCUS = lastFocusMatcher.group(1)
        }
        if (eventTargetMatcher.find()) {
            __EVENTTARGET = eventTargetMatcher.group(1)
        }
        if (eventArgumentMatcher.find()) {
            __EVENTARGUMENT = eventArgumentMatcher.group(1)
        }

        html = html.toString().replaceAll(/(?is).*(?=ctl00[$]cphMainContent[$]ddlProfession).*width:525px;">/, "")
        html = html.toString().replaceAll(/(?is)(?<=Interior Designer<\\/option>).*/, "")
        def professionMatcher = html =~ /(?i)value="(\d+)"/
        while (professionMatcher.find()) {
            def profId = professionMatcher.group(1)
            def params = getParams(__VIEWSTATE, __VIEWSTATEGENERATOR, __LASTFOCUS, __EVENTTARGET, __EVENTARGUMENT, profId)
            html = invokePost(mainUrl, params, false, headers, false)
            handleHtml(html)
        }
    }


    def handleHtml(html) {
        def headers = getHeader()
        def __VIEWSTATE, __VIEWSTATEGENERATOR, __LASTFOCUS, __EVENTTARGET, __EVENTARGUMENT
        handleIndividualHtml(html)

        def isInvoked = false

        if (html =~ /Page\$2/) {
            __LASTFOCUS = ""
            def paramMatcher = html =~ /(\\/wEPD.*)(?=\|8).*(1A.*D5)/

            if (paramMatcher.find()) {
                __VIEWSTATE = paramMatcher.group(1)
                __VIEWSTATEGENERATOR = paramMatcher.group(2)

            }

            def nextPageMatcher = html =~ /(ctl00[$]cphMainContent[$]gvResults)','(.*?')/

            while (nextPageMatcher.find()) {
                __EVENTTARGET = nextPageMatcher.group(1)
                __EVENTARGUMENT = nextPageMatcher.group(2).toString().replaceAll(/'/, '')
                def page = nextPageMatcher.group(2).toString().replaceAll(/Page\$(.*)/, '$1')
                def params = getParamsForNext(__VIEWSTATE, __VIEWSTATEGENERATOR, __LASTFOCUS, __EVENTTARGET, __EVENTARGUMENT)

                html = invokePost(mainUrl, params, false, headers, false)
                html = html.toString().replaceAll(/<td>.*?'Page\$(?:First)'.*?'Page\$\d+'.*?<td>(?=<span>\d+<\/span>)/, "")

                if (page =~ /\d{1,3}1'/) nextPageMatcher = html =~ /(ctl00[$]cphMainContent[$]gvResults)','(Page\$\d+')/
                handleIndividualHtml(html)
            }
        }
    }

    def handleIndividualHtml(html) {
        def all_Matcher = html =~ /href="\.\.\s*(\/.+\.pdf)\s*".*?<td>\s*(\d{1,2}\/\d{1,2}\/\d{2,4})\s*<\/td><td>\s*(.*?)\s*<\/td><td>\s*(.*?)\s*<\/td>/
        while (all_Matcher.find()) {
            def respondent = all_Matcher.group(3)
            def date = all_Matcher.group(2)
            def entityUrl = headerUrl + all_Matcher.group(1)
            handleEntityNameAndAlias(respondent, date, entityUrl)
        }
    }

    def handleEntityNameAndAlias(def respondent, def eventDate, def entityUrl) {
        def alias
        def aliasList = []
        def aliasMatcher
        def entityName
        def respondentList

        respondent = sanitizeRespondent(respondent)

        if (respondent =~ /(?i)^(.+),\s{0,}(.+),\s{0,}(.+)$/) {
            entityName = respondent.toString().replaceAll(/(?i)^(.+),\s*(.+),\s*(.+)$/, '$2 $3 $1')
            createEntity(entityName, aliasList, eventDate, entityUrl)
        } else if (respondent =~ /(?i)^(.+),\s{0,}(.+)$/) {
            if ((aliasMatcher = respondent =~ /\((.+)\)/)) {
                alias = aliasMatcher[0][1]
                aliasList.add(alias)
                respondent = respondent.toString().replaceAll(/$alias/, "").replaceAll(/[\(\)]/, "")
            }
            entityName = respondent.toString().replaceAll(/(?i)^(.+),\s*(.+)$/, '$2 $1')
            createEntity(entityName, aliasList, eventDate, entityUrl)
        } else {
            if (respondent =~ /\/Nellcor/) {
                if ((aliasMatcher = respondent =~ /((?:\\/Nellcor).+$)/)) {
                    alias = aliasMatcher[0][1]
                    entityName = respondent.toString().replaceAll(/$alias/, "")
                    alias = alias.toString().replaceAll(/^\//, "")
                    aliasList = alias.toString().split(/\//).collect({ it -> return it.trim() })
                    createEntity(entityName, aliasList, eventDate, entityUrl)
                }
            } else {
                if ((aliasMatcher = respondent =~ /\((.+)\)/)) {
                    alias = aliasMatcher[0][1]
                    aliasList.add(alias)
                    entityName = respondent.toString().replaceAll(/$alias/, "").replaceAll(/[\(\)]/, "")
                } else if ((aliasMatcher = respondent =~ /(DBA\s.+)/)) {
                    alias = aliasMatcher[0][1]

                    entityName = respondent.toString().replaceAll(/$alias/, "")
                    alias = alias.toString().replaceAll(/DBA\s/, "")
                    aliasList.add(alias)
                } else {
                    entityName = respondent.toString()
                }
            }

            createEntity(entityName, aliasList, eventDate, entityUrl)
        }
    }

    def createEntity(def name, def aliasList, def eventDate, def entityUrl) {
        def eventDescription = "This entity appears on the Wisconsin Department of Safety and Professional Services list of Disciplinary Decisions."

        def entity = null
        ScrapeAddress address = new ScrapeAddress()
        address.setCountry('UNITED STATES')
        address.setProvince("Wisconsin")

        ScrapeEvent event = new ScrapeEvent()

        name = sanitizeName(name)
//        textFileContent.add(name)

        if (!name.toString().isEmpty()) {
            def entityType = detectEntity(name)

            entity = context.findEntity(["name": name, "type": entityType])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }

            def aliasEntityType, alias
            aliasList.each { it ->
                if (it) {
                    entity.addAlias(it)
                }
            }
            if (eventDate) {
                eventDate = context.parseDate(new StringSource(eventDate), ["MM/dd/yyyy", "MM/d/yyyy", "M/dd/yyyy", "M/d/yyyy", "M/dd/yy", "MM/dd/yy", "MMM dd, yyyy", "MMM d, yyyy"] as String[])
                event.setDate(eventDate)
                if (eventDescription) {
                    event.setDescription(eventDescription)
                }
                entity.addEvent(event)
                entity.addUrl(entityUrl)
            } else {
                if (eventDescription) {
                    event.setDescription(eventDescription)
                }
                entity.addEvent(event)
                entity.addUrl(entityUrl)
            }
            entity.addAddress(address)
        }
    }

    def detectEntity(def name) {
        def type
        if(name =~/(?ism)MEMORIAL PARK|Forever Young Skin Care Llc/){
            type = "O"
        }else if (name =~ /^bud|^larry|^jay|Laporte-Millman|Avvair/) {
            type = "P"
        } else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|\sInc\.?$|L\.L\.C\.$|\sLTC$|Corporation$|\sInvestigation[s]?$|\sart$|\sCorp\.?$|\sBOUTIQUE$|\sRETURNS$|\sLOUNGE$|\sTangles$|\sFL$|Company$|\sLP$|\sDrug[s]?$|\sLLP$|\sAmerica$|\sGallery$|Fnl Hm$|L\.P\.$|\sServices$|\sCo\.?$|Puritan Bennett$|\sNails$|\sPharmacy$|\sNail$|^Nail\s|^Nails\s|\sFACE$|Waukesha$|By Cathy$)/) {
                    type = "O"
                } else if (name =~ /^(?:Starrs Sister)$|(?i)\s(?:Stylist|N Motion|Etc|Quarters|Barbershop|Off Main|HREADING|ATTRACTIONS|Spas|Sams|Extraordinaire|Designs|CLIPS|Milwaukee|Cuts|SHEARS)$/) {
                    type = "O"
                } else if (name =~ /\s(?:CORNELL|STYLES|Clippers|Electrical|II|NAIL BAR|Electrolysis)$/) {
                    type = "O"
                } else if (name =~ /(?ism)(?:Central Ba|Excells House|Living Hair |Loras Divine Hair Appointme|Hair Experts|Uptown Nails and Hair|Hair Beautique|Forever Young Skin Care|Golden Sands Resp Care|Hair Port|Hair Raisers|Hedda Hair|Jaz N Hair|Beauty Chateau|Hot Spot|LASER PLUS|Marys Chair|Medi-Kare Prescription P|Victorias Hair And Body|Great Clips For|Guardian Pharmacy|HAIR FORCE ONE|HAIR REGULATO|Hair Designers|Hair Elegance|College|Beauty l|Cost Cutters|Absolute|A Cut Of|Accent Fashions|Artistic Styles|DIAMOND CUT|Glamour Shots)/) {
                    type = "O"
                }
            } else if (name =~ /\sNgo$|(?i)\s(?:Story|F Hunt|M Hunt|K Vu|Mcvicker|Walker|Mason|D\.c\.|(?:L|J|E)\sWest|Croix|Nguyen|Molitor|Mai Pham|C Pack|L Chambers|S Golden|T Ngo|(?:K|S)\sChurch|F Royal|Neumeyer|Major|Matsche Sc|(?:A|B)\sRichter|Shields|St Marie|Pride|St Cloud|Park|Jo Gold|Chase|J Court|St Catherine|E Golden)$/) {
                type = "P"
            } else if (name =~ /(?i)(?:Allen Link|J Cash)$/) {
                type = "P"
            } else if (name =~ /Arthur Mines|Jessica L Baptist|Mary Lou Behring|Amanda M Rue|Carol J Sams|Forever Young/) {
                type = "P"
            }
        }
        if (name =~ /Cost Cutters Walmart|Hair Paradise|MENS HAIR HOUSE|Martins College Of Cosmetology|Mode Beauty Shoppe|flip n styles|A Shear Delight/) {
            type = "O"
        }
        return type
    }

    def sanitizeRespondent(def respondent) {
        respondent = respondent.toString().trim()
        respondent = respondent.toString().replaceAll(/(?i),\s*(?=L\.L\.P|L\.L\.C|Inc\.?|LLC|LLP|LTD|LIMITED|Skin|Nails)/, " ")
        respondent = respondent.toString().replaceAll(/[,]+/, ",")
        respondent = respondent.toString().replaceAll(/&amp;/, "&")
        respondent = respondent.toString().replaceAll(/&quot;/, "'")
        respondent = respondent.toString().replaceAll(/@NAIL/, "NAIL")
        respondent = respondent.toString().replaceAll(/(?i)\(USA\)/, "USA")
        respondent = respondent.toString().replaceAll(/(?i)\(The\)/, "").trim()
        respondent = respondent.toString().replaceAll(/(?i)\(The\)|\/Barb/, "").trim()
        respondent = respondent.toString().replaceAll(/(?i)Inc Par Pharmaceutical/, "Par Pharmaceutical Inc").trim()
        return respondent.trim()
    }

    def sanitizeName(def name) {
        name = name.toString().trim()
        name = name.toString().replaceAll(/^,$/, "")
        name = name.toString().replaceAll(/\s+/, " ").trim()
        name = name.toString().replaceAll(/Zo&#235; K Van Oss/, 'Zoe K. Van Oss')
        return name.trim()
    }

    def getParams(__VIEWSTATE, __VIEWSTATEGENERATOR, __LASTFOCUS, __EVENTTARGET, __EVENTARGUMENT, profId) {
        def param = [:]

        param["__VIEWSTATE"] = __VIEWSTATE
        param["__VIEWSTATEGENERATOR"] = __VIEWSTATEGENERATOR
        param["__LASTFOCUS"] = __LASTFOCUS
        param["__EVENTTARGET"] = __EVENTTARGET
        param["__EVENTARGUMENT"] = __EVENTARGUMENT

        param["ctl00\$cphMainContent\$Search"] = "rbProfession"
        param["ctl00\$cphMainContent\$ddlSortDropDownList"] = "Profession"
        param["ctl00\$cphMainContent\$ddlBoard"] = ""
        param["ctl00\$cphMainContent\$ddlProfession"] = profId
        param["ctl00\$cphMainContent\$btnSearch"] = "Search"
        param["ctl00\$cphMainContent\$txtLastName"] = ""
        param["ctl00\$cphMainContent\$txtFirstName"] = ""
        param["ctl00\$cphMainContent\$txtOrgName"] = ""
        param["ctl00\$cphMainContent\$ddlOrderStartMonth"] = ""
        param["ctl00\$cphMainContent\$ddlOrderStartYear"] = ""
        param["ctl00\$cphMainContent\$ddlOrderEndMonth"] = ""
        param["ctl00\$cphMainContent\$ddlOrderEndYear"] = ""
        param["ctl00\$cphMainContent\$txtCity"] = ""
        param["ctl00\$cphMainContent\$ddlCounty"] = ""
        return param
    }

    def getParamsForNext(__VIEWSTATE, __VIEWSTATEGENERATOR, __LASTFOCUS, __EVENTTARGET, __EVENTARGUMENT) {
        def param = [:]
        param["__VIEWSTATE"] = __VIEWSTATE
        param["__VIEWSTATEGENERATOR"] = __VIEWSTATEGENERATOR
        param["__LASTFOCUS"] = __LASTFOCUS
        param["__EVENTTARGET"] = __EVENTTARGET
        param["__EVENTARGUMENT"] = __EVENTARGUMENT
        return param
    }

    def getHeader() {
        def headers = [
            "Accept"         : "application/json, text/javascript, */*; q=0.01",
            "Accept-Language": "en-US,en;q=0.9",
            "Connection"     : "keep-alive",
            "Content-Type"   : "application/x-www-form-urlencoded; charset=UTF-8",
            "Host"           : "online.drl.wi.gov",
            "Origin"         : "https://online.drl.wi.gov",
            "Referer"        : "https://online.drl.wi.gov/orders/searchorders.aspx",
            "Sec-Fetch-Dest" : "empty",
            "Sec-Fetch-Mode" : "cors",
            "Sec-Fetch-Site" : "same-origin",
            "X-MicrosoftAjax": "Delta=true",
        ]
        return headers
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



