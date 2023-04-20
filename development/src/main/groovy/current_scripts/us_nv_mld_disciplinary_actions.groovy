package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 3, multithread: true, userAgent: "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_nv_mld_disciplinary_actions script = new Us_nv_mld_disciplinary_actions(context)
script.initParsing()

class Us_nv_mld_disciplinary_actions {

    final ScrapianContext context
    final entityType
    def moduleLoaderVersion = context.scriptParams.moduleLoaderVersion
    final def moduleFactory = ModuleLoader.getFactory(moduleLoaderVersion)
    static def root = "http://mld.nv.gov"

    Us_nv_mld_disciplinary_actions(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
    }

    def initParsing() {
        def html = context.invoke(url: Us_nv_mld_disciplinary_actions.root + "/Enforcement/Home/", tidy: false, cache: false)
        getDatafromRoot(html)
    }

    def getDatafromRoot(def html) {

        def html1
        def urlEach = html =~ "(?ism)<li style=.+?href=\"(.+?)\".+?<\\/li>"
        while (urlEach.find()) {
            def yoyo = Us_nv_mld_disciplinary_actions.root + urlEach.group(1)
            if (!yoyo.contains("/Proposed_Consent_and_Settlement_Agreements/")) {
                html1 = context.invoke(url: Us_nv_mld_disciplinary_actions.root + urlEach.group(1).toString(), tidy: false, cache: false)
            }
            if (yoyo.contains("2020") | yoyo.contains("2021") | yoyo.contains("2019") | yoyo.contains("2017")) {
                getDatafromEachWithUrl(html1)
            } else if (yoyo.contains("2018")) {
                getDatafromEach2018(html1)
            } else {
                getDatafromEachWithoutUrl(html1)
            }
        }


    }

    def getDatafromEachWithUrl(def htmlData) {


        def tableData = htmlData =~ "(?ism)<tbody.+?tbody>"

        if (tableData.find()) {

            def tableBlock = tableData.group(0)
            def rowBlock = tableBlock =~ /(?ism)<tr>.+?tr>/

            while (rowBlock.find()) {

                def aliasList = []
                def totalAlias, aliasMatcher

                def row = rowBlock.group(0)

                def row2020 = row =~ "(?ism)<tr>.+?<td.+?a href=\"(.+?)\".+?title=\"(.+?)\".+?center;\">(.+?)<\\/td>"
                // def rowWithUrl = row =~ "(?ism)<tr>.+?<td.+?a (?:href|title)=\"(.+?)\".+?(?:title|href)=\"(.+?)\".+?style.+?style.+?>(.+?)<\\/td>"
                def rowWithUrl = row =~ /(?ism)a href="([^>]+)">(.+?)<\/a>.+?<\/td>\s+<td.*?>(\d+\/\d+\/\d+)/
                while (row2020.find()) {
                    def name = row2020.group(2)
                    name = sanitizeName(name)

                    def date = row2020.group(3)

                    def pdfUrl = row2020.group(1)


                    if (name =~ /(?si)(\s+DBA\s+|\b(?:aka|d\/b\/a)\b)/) {
                        name = name.replaceAll(/(?is)\b(dba|aka|d\/b\/a)\b/, "dba")

                        if ((aliasMatcher = name =~ /(?ism)(?:dba|aka|d\/b\/a)(.+?)$/)) {
                            totalAlias = aliasMatcher[0][1]
                            name = name.toString().replace(/$totalAlias/, "")
                                .replaceAll(" dba", "")
                                .replaceAll(" aka", "")
                                .replaceAll(" d/b/a", "")
                            name = sanitizeName(name)
                            aliasList = totalAlias.split(" dba| aka").collect({ its ->
                                its = sanitizeAlias(its)
                                return its
                            })
                        }
                    }

                    def eventDes = "This entity appears on the Nevada Mortgage Lending Division's list of Enforcement Actions."
                    def multi = name.split(/(?is)\band\s+(?!Company|Inc)/)
                    multi.each { n ->
                        createEntity(name, date, eventDes, pdfUrl, aliasList)

                    }
                }

                while (rowWithUrl.find()) {

                    def name = rowWithUrl.group(2)
                    name = sanitizeName(name)
                    //println(name)

                    def date = rowWithUrl.group(3)

                    def pdfUrl = rowWithUrl.group(1)

                    def eventDes = "This entity appears on the Nevada Mortgage Lending Division's list of Enforcement Actions."

                    def multipleNameList = []
                    multipleNameList = name.toString().split(/;|\s+and\s+(?!Associates|Company)/)
                        .collect({ its -> return its })

                    multipleNameList.each {
                        def aliasList1 = []
                        def totalAlias1, aliasMatcher1

                        if (it =~ /(?si)\b(DBA|aka|d\/b\/a)\b/) {
                            it = it.replaceAll(/(?is)\b(dba|aka|d\/b\/a)\b/, "dba")

                            if ((aliasMatcher1 = it =~ /(?ism)(?:dba|aka|d\/b\/a)(.+?)$/)) {
                                totalAlias1 = aliasMatcher1[0][1]
                                it = it.toString().replace(/$totalAlias1/, "")
                                    .replace(" dba", "")
                                    .replace(" aka", "")
                                    .replace(" d/b/a", "")
                                it = sanitizeName(it)
                                aliasList1 = totalAlias1.split(" dba| aka").collect({ its ->
                                    its = sanitizeAlias(its)
                                    return its
                                })
                            }
                        }
                        if (StringUtils.isNotEmpty(it)) {
                            createEntity(it, date, eventDes, pdfUrl, aliasList1)
                        }
                    }

                }
            }

        }

    }

    def getDatafromEach2018(def htmlData) {

        def tableData = htmlData =~ "(?ism)<tbody.+?tbody>"

        if (tableData.find()) {

            def tableBlock = tableData.group(0)

            def rowBlock = tableBlock =~ /(?ism)<tr>.+?tr>/

            while (rowBlock.find()) {

                def row = rowBlock.group(0)
                def rowWithUrl = row =~ "(?ism)<tr>.+?<td.+?a (?:href|title)=\"(.+?)\".+?(?:title|href)=\"(.+?)\".+?colspan.+?colspan.+?>(.+?)<\\/td>"
                while (rowWithUrl.find()) {

                    def name = rowWithUrl.group(1)
                    name = sanitizeName(name)

                    def date = rowWithUrl.group(3)

                    def pdfUrl = rowWithUrl.group(2)

                    def eventDes = "This entity appears on the Nevada Mortgage Lending Division's list of Enforcement Actions."


                    def multipleNameList = []

                    multipleNameList = name.toString().split(/;|\s+and\s+(?!Associates|Company)/)
                        .collect({ its -> return its })

                    multipleNameList.each {
                        def aliasList = []
                        def totalAlias, aliasMatcher

                        if (it.contains("dba") | it.contains("aka")) {

                            if ((aliasMatcher = it =~ /(?ism)(?:dba|aka)(.+?)$/)) {
                                totalAlias = aliasMatcher[0][1]
                                it = it.toString().replace(/$totalAlias/, "")
                                    .replaceAll(" dba", "")
                                    .replaceAll(" aka", "")
                                it = sanitizeName(it).trim()
                                it = it.replaceAll(/,$/, "").trim()
                                aliasList = totalAlias.split(" dba| aka").collect({ its ->
                                    its = sanitizeAlias(its)
                                    return its
                                })
                            }
                        }
                        if (name =~ /2020010/) {
                            //println("#2")
                        }
                        createEntity(it, date, eventDes, pdfUrl, aliasList)
                    }
                }
            }
        }
    }

    def getDatafromEachWithoutUrl(def htmlData) {

        def tableData = htmlData =~ "(?ism)<tbody.+?tbody>"

        if (tableData.find()) {

            def tableBlock = tableData.group(0)
            def rowBlock = tableBlock =~ /(?ism)<tr>.+?tr>/

            while (rowBlock.find()) {

                def row = rowBlock.group(0)

                def rowWithOutUrl = row =~ "(?ism)<tr>.+?headers.+?heading.+?>(.+?)<\\/td>.+?headers.+?headers.+?>(.+?)<\\/td>"

                while (rowWithOutUrl.find()) {
                    def name = rowWithOutUrl.group(1)

                    name = sanitizeName(name).trim()
                    name = name.replaceAll(/,$/, "").trim()


                    def date = rowWithOutUrl.group(2)
                    def eventDes = "This entity appears on the Nevada Mortgage Lending Division's list of Enforcement Actions."

                    def multipleNameList = []

                    multipleNameList = name.toString().split(/;/)
                        .collect({ its -> return its })

                    multipleNameList.each {
                        def aliasList = []
                        def totalAlias, aliasMatcher
                        if (it.contains("dba") | it.contains("aka") | it.contains("f/k/a") | it.contains("fka")) {

                            if ((aliasMatcher = it =~ /(?ism)(?:dba|aka|f\/k\/a|fka)(.+?)$/)) {
                                totalAlias = aliasMatcher[0][1]
                                it = it.toString().replace(/$totalAlias/, "")
                                    .replaceAll(" dba", "")
                                    .replaceAll(" aka", "")
                                    .replaceAll(" fka", "")
                                    .replaceAll("f/k/a", "")
                                it = sanitizeName(it).trim()
                                it = it.replaceAll(/,$/, "").trim()

                                aliasList = totalAlias.split(" dba| aka|f/k/a| fka").collect({ its ->
                                    its = sanitizeAlias(its)
                                    return its
                                })
                            }
                        }

                        createEntity(it, date, eventDes, aliasList)
                    }

                }
            }

        }

    }


    def createEntity(def name, def eventDate, def eventDes, def entityUrl, def aliasList) {
        def entity = null
        name = name.replaceAll(/,$/, "").trim()

        entity = context.findEntity([name: name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            def entityType = detectEntity(name)
            entity.setType(entityType)
        }

        //Address
        ScrapeAddress address = new ScrapeAddress()
        address.setProvince("Nevada")
        address.setCountry("UNITED STATES")
        entity.addAddress(address)

        //Add EntityUrl

        if (entityUrl) {
            entity.addUrl(sanitizeUrl(root + entityUrl))
        }


        if (eventDate) {
            eventDate = context.parseDate(new StringSource(eventDate), ["MM/dd/yyyy"] as String[])
            ScrapeEvent scrapeEvent = new ScrapeEvent()
            scrapeEvent.setDate(eventDate)
            scrapeEvent.setDescription(eventDes)
            entity.addEvent(scrapeEvent)
        }

        //Alias

        aliasList.each {
            if (it) {
                entity.addAlias(it)
            }
        }


    }

    String sanitizeUrl(String url) {
        url = url.replaceAll(/\.pdf.+$/, ".pdf")
        url = url.replaceAll(/(?s)\s+/, " ").trim()
        return url.replaceAll(/\s+/, "%20")
    }

    def createEntity(def name, def eventDate, def eventDes, def aliasList) {
        def entity = null
        name = name.toString().replaceAll(/,$/, "")
        entity = context.findEntity([name: name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            def entityType = detectEntity(name)
            entity.setType(entityType)
        }

        //Address
        ScrapeAddress address = new ScrapeAddress()
        address.setProvince("Nevada")
        address.setCountry("UNITED STATES")
        entity.addAddress(address)


        if (eventDate) {
            eventDate = context.parseDate(new StringSource(eventDate), ["MM/dd/yyyy"] as String[])
            ScrapeEvent scrapeEvent = new ScrapeEvent()
            scrapeEvent.setDate(eventDate)
            scrapeEvent.setDescription(eventDes)
            entity.addEvent(scrapeEvent)
        }

        //Alias

        aliasList.each {
            if (it) {
                entity.addAlias(it)
            }
        }

    }


    def detectEntity(def name) {
        def type

        if (name =~ /(?i)(?:KRKABOB|Evofi One|Homekeepers RSVP)/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
        }
        return type
    }

    //Sanitize Part

    def sanitizeName(def name) {
        name = name.toString().replaceAll(/\r\n/, "").trim()
        name = name.toString().replaceAll(/(?ism)&amp;/, "&").trim()
        name = name.toString().replaceAll(/(?ism)&quot;/, "").trim()
        name = name.toString().replaceAll(/(?ism)&nbsp;/, "").trim()
        name = name.toString().replaceAll(/(?ism)<span.+?>/, "").trim()
        name = name.toString().replaceAll(/(?ism)<\/span>.+?-/, "").trim()
        name = name.toString().replaceAll(/(?ism),$/, "").trim()
        name = name.toString().replaceAll(/\(pdf\)/, "").trim()
        name = name.toString().replaceAll(/(?ism)Consent Order for/, "").trim()
        name = name.toString().replaceAll(/(?ism)- Order to Cease and Desist/, "").trim()
        name = name.toString().replaceAll(/(?ism)Order to Cease and Desist/, "").trim()
        name = name.toString().replaceAll(/(?ism)Final Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)to Revoke Mortgage Agent License/, "").trim()
        name = name.toString().replaceAll(/(?ism)Order Denying License/, "").trim()
        name = name.toString().replaceAll(/(?ism)- Final Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)- Consent Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)- Cease and Desist/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Joseph Wagner/, ";Joseph Wagner").trim()
        name = name.toString().replaceAll(/(?ism)Argus Lending - Kenneth Pittman/, "Argus Lending dba Kenneth Pittman").trim()
        name = name.toString().replaceAll(/(?ism)and David Lohrey/, ";David Lohrey").trim()
        name = name.toString().replaceAll(/(?ism)and Damian Roland Falcone/, "dba Damian Roland Falcone").trim()
        name = name.toString().replaceAll(/(?ism)and Alessandro "Alex" Ciaccio/, ";Alessandro Alex Ciaccio").trim()
        name = name.toString().replaceAll(/(?ism)and Ann Vaughn/, ";Ann Vaughn").trim()
        name = name.toString().replaceAll(/(?ism)and Justin Robert Lantzman/, ";Justin Robert Lantzman").trim()
        name = name.toString().replaceAll(/(?ism)and Scott Krelle/, ";Scott Krelle").trim()
        name = name.toString().replaceAll(/(?ism)and Joel Armstrong/, ";Joel Armstrong").trim()
        name = name.toString().replaceAll(/(?ism)and Ira L. Meltzer/, ";Ira L. Meltzer").trim()
        name = name.toString().replaceAll(/(?ism), NV Property 1, LLC, Secured Asset Management, LLC and Michael Eckerman/, ";NV Property 1, LLC;Secured Asset Management, LLC ;Michael Eckerman").trim()
        name = name.toString().replaceAll(/(?<=Gindt), (?=1802 North)/, ";").trim()
        name = name.toString().replaceAll(/(?<=LLC), (?=PheasantFerguson)/, ";").trim()
        name = name.toString().replaceAll(/(?ism)and Mitchell Ginsberg/, ";Mitchell Ginsberg").trim()
        name = name.toString().replaceAll(/(?ism)and Mitchell Ginsberg/, ";Mitchell Ginsberg").trim()
        name = name.toString().replaceAll(/(?ism)and Kenneth Pittman/, ";Kenneth Pittman").trim()
        name = name.toString().replaceAll(/(?ism)and Robert Alan Rink Jr./, ";Robert Alan Rink Jr.").trim()
        name = name.toString().replaceAll(/(?ism)Robert C. Welch III.+?dated 9-14-2018/, "Robert C. Welch.III").trim()
        name = name.toString().replaceAll(/(?ism)and Shawn Michael Clem/, ";Shawn Michael Clem").trim()
        name = name.toString().replaceAll(/(?ism)- Findings of Fact, Conclusions of Law, and Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Richard Shrigley/, ";Richard Shrigley").trim()
        name = name.toString().replaceAll(/(?ism)- Stipulation of Settlement and Order Approving Stipulation/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Roderick Rickert/, ";Roderick Rickert").trim()
        name = name.toString().replaceAll(/(?ism)and Ira L. Meltzer/, ";Ira L. Meltzer").trim()
        name = name.toString().replaceAll(/(?ism)and Realty Inc./, ";Realty Inc.").trim()
        name = name.toString().replaceAll(/(?ism)and Joel Armstrong/, ";Joel Armstrong").trim()
        name = name.toString().replaceAll(/(?ism)and Robert Allan Rink, Jr./, ";Robert Allan Rink, Jr.").trim()
        name = name.toString().replaceAll(/(?ism)and Paul Wesley Filer/, ";Paul Wesley Filer").trim()
        name = name.toString().replaceAll(/(?ism), Wynn Investor Network, Inc., and Daniel Arguello/, ";Wynn Investor Network, Inc.;Daniel Arguello").trim()
        name = name.toString().replaceAll(/(?ism)Final Order to Revoke Mortgage Agent License/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Christopher Biaggi/, ";Christopher Biaggi").trim()
        name = name.toString().replaceAll(/(?ism), Catherine Hyde and Kimberly Erin Leonas/, ";Catherine Hyde;Kimberly Erin Leonas").trim()
        name = name.toString().replaceAll(/(?ism)and George Kalivretenos/, ";George Kalivretenos").trim()
        name = name.toString().replaceAll(/(?ism)and Alan Maynor and Jan Maynor/, ";Alan Maynor;Jan Maynor").trim()
        name = name.toString().replaceAll(/(?ism)and Milo Lewis/, ";Milo Lewis").trim()
        name = name.toString().replaceAll(/(?ism)- Amended Consent Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Jamil Louis/, ";Jamil Louis").trim()
        name = name.toString().replaceAll(/(?ism)and Christopher Wages and Miles Godbey/, ";Christopher Wages;Miles Godbey").trim()
        name = name.toString().replaceAll(/(?ism), Adham Ramsey Sbeih and John Robert Ingoglia/, ";Adham Ramsey Sbeih;John Robert Ingoglia").trim()
        name = name.toString().replaceAll(/(?ism)and Orlando Vera/, ";Orlando Vera").trim()
        name = name.toString().replaceAll(/(?ism)and Benjamin Donlon aka Ben Donlon/, ";Benjamin Donlon aka Ben Donlon").trim()
        name = name.toString().replaceAll(/(?ism)and Farjallah Yazbek and Somer Forraj/, ";Farjallah Yazbek;Somer Forraj").trim()
        name = name.toString().replaceAll(/(?ism)Consent Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Ithacannes, LLC,/, ";Ithacannes, LLC").trim()
        name = name.toString().replaceAll(/(?ism)and Francisco De La Chesnaye/, ";Francisco De La Chesnaye").trim()
        name = name.toString().replaceAll(/(?ism)and Mark E. Hamlin/, ";Mark E. Hamlin").trim()
        name = name.toString().replaceAll(/(?ism)and Gabriel Ramallo/, ";Gabriel Ramallo").trim()
        name = name.toString().replaceAll(/(?ism)and Jeffrey B. Guinn/, ";Jeffrey B. Guinn").trim()
        name = name.toString().replaceAll(/(?ism)- Decision and Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)- Summary Suspension of License/, "").trim()
        name = name.toString().replaceAll(/(?ism)and A.J. Banford, Jr./, ";A.J. Banford, Jr.").trim()
        name = name.toString().replaceAll(/(?ism)& Services and Bobby Doyle/, ";Services and Bobby Doyle").trim()
        name = name.toString().replaceAll(/(?ism)and A and F Enterprise Inc./, ";A and F Enterprise Inc.").trim()
        name = name.toString().replaceAll(/(?ism)and Virgie Vincent/, ";Virgie Vincent").trim()
        name = name.toString().replaceAll(/(?ism)and Jose Avila/, ";Jose Avila").trim()
        name = name.toString().replaceAll(/(?ism), CSR Services and Xochitl Cervantes/, ";CSR Services;Xochitl Cervantes").trim()
        name = name.toString().replaceAll(/(?ism)- Settlement Agreement/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Minerva E. Young and David Young/, ";Minerva E. Young;David Young").trim()
        name = name.toString().replaceAll(/(?ism)and Gustave Anaya/, ";Gustave Anaya").trim()
        name = name.toString().replaceAll(/(?ism)and Tom Nieman/, ";Tom Nieman").trim()
        name = name.toString().replaceAll(/(?ism), Jose Benjamin Rodriguez/, ";Jose Benjamin Rodriguez").trim()
        name = name.toString().replaceAll(/(?ism), and Nevada Sky Premier, LLC/, ";Nevada Sky Premier, LLC").trim()
        name = name.toString().replaceAll(/(?ism)and R. Gregory Ernst,/, ";R. Gregory Ernst").trim()
        name = name.toString().replaceAll(/(?ism), Final Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Marisol Perez/, ";Marisol Perez").trim()
        name = name.toString().replaceAll(/(?ism)and Mandy Peacock/, ";Mandy Peacock").trim()
        name = name.toString().replaceAll(/(?ism)Decision and Order/, "").trim()
        name = name.toString().replaceAll(/(?ism), Jose Castro and Angeles Castro,/, ";Jose Castro;Angeles Castro").trim()
        name = name.toString().replaceAll(/(?ism), Order to Cease and Desist/, "").trim()
        name = name.toString().replaceAll(/(?ism), Decision and Order/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Carl Otto Nathaniel Holm/, ";Carl Otto Nathaniel Holm").trim()
        name = name.toString().replaceAll(/(?ism)and Harvey Collins/, ";Harvey Collins").trim()
        name = name.toString().replaceAll(/(?ism), Roslyn Phoenix/, ";Roslyn Phoenix").trim()
        name = name.toString().replaceAll(/(?ism), Deborah Paaren and Danijela Mikulic/, ";Deborah Paaren;Danijela Mikulic").trim()
        name = name.toString().replaceAll(/(?ism), Jeff Strum and Gail Strum/, ";Jeff Strum;Gail Strum").trim()
        name = name.toString().replaceAll(/(?ism), American Home Services,/, ";American Home Services").trim()
        name = name.toString().replaceAll(/(?ism)Capital Legal Group and Capital Media/, "Capital Legal Group dba Capital Media").trim()
        name = name.toString().replaceAll(/(?ism), and Thinh Nguyen/, ";Thinh Nguyen").trim()
        name = name.toString().replaceAll(/(?ism)and Yvonne Chacon/, ";Yvonne Chacon").trim()
        name = name.toString().replaceAll(/(?ism)and Marsha Tolentino/, ";Marsha Tolentino").trim()
        name = name.toString().replaceAll(/(?ism)and Ray Donald/, ";Ray Donald").trim()
        name = name.toString().replaceAll(/(?ism)and Angela Gavilan/, ";Angela Gavilan").trim()
        name = name.toString().replaceAll(/(?ism)and Patricia Bascom/, ";Patricia Bascom").trim()
        name = name.toString().replaceAll(/(?ism)and Kristy Sinsara/, ";Kristy Sinsara").trim()
        name = name.toString().replaceAll(/(?ism), Edmundo and Martha Polo/, ";Edmundo;Martha Polo").trim()
        name = name.toString().replaceAll(/(?ism), Amended Final Order/, "").trim()
        name = name.toString().replaceAll(/(?ism), Jeff Strum, Gail Strum/, ";Jeff Strum;Gail Strum").trim()
        name = name.toString().replaceAll(/(?ism)and Bobby Vavla/, ";Bobby Vavla").trim()
        name = name.toString().replaceAll(/(?ism), a Missouri Corporation/, "").trim()
        name = name.toString().replaceAll(/(?ism), Chona Mejia/, ";Chona Mejia").trim()
        name = name.toString().replaceAll(/(?ism), William Vinson and Nichole Soria/, ";William Vinson;Nichole Soria").trim()
        name = name.toString().replaceAll(/(?ism), Property Scout of Nevada, LLC, Raul Estrada/, ";Property Scout of Nevada, LLC;Raul Estrada").trim()
        name = name.toString().replaceAll(/(?ism), Joseph Yorkus and James R. Bartczak/, ";Joseph Yorkus;James R. Bartczak").trim()
        name = name.toString().replaceAll(/(?ism)and Maria D. Davila/, ";Maria D. Davila").trim()
        name = name.toString().replaceAll(/(?ism)and Lori Blake/, ";Lori Blake").trim()
        name = name.toString().replaceAll(/(?ism)and Marsha Tolentino/, ";Marsha Tolentino").trim()
        name = name.toString().replaceAll(/(?ism)Stipulated Settlement Agreement/, "").trim()
        name = name.toString().replaceAll(/(?ism), Stipulated Settlement Agreement/, "").trim()
        name = name.toString().replaceAll(/(?ism)and Frank Albert Curtis/, ";Frank Albert Curtis").trim()
        name = name.toString().replaceAll(/(?ism)and Miriam Fimbres/, ";Miriam Fimbres").trim()
        name = name.toString().replaceAll(/(?s)\s+/, " ").trim()
        name = name.toString().replaceAll(/,$/, "").trim()
        name = name.toString().replaceAll(/-/, "").trim()
        name = name.toString().replaceAll(/(?ism)FillipinoAmerican/, "Fillipino-American").trim()
        name = name.toString().replaceAll(/-$/, "").trim()
        name = name.toString().replaceAll(/(?ism)Amended/, "").trim()
        name = name.toString().replaceAll(/(?ism)Sussex Group, Inc.,/, "Sussex Group, Inc.").trim()
        name = name.toString().replaceAll(/(?ism)\.\W*$/, "_COMMAA")
        name = name.replaceAll(/&\S+;\s*$|\W+$/, "").trim()
        name = name.replaceAll(/_COMMAA/, ".").trim()
        name = name.replaceAll(/(?i)\bInc$/, "Inc.")
        name = name.replaceAll(/AlexanderOwens/, "Alexander-Owens")
        name = name.replaceAll(/\;\s*Services\s+and\s+/, "& Services;")
        name = name.replaceAll(/(& )(Brooke Bedson|Mount Olympus Title)/, ';$2')
        return name.replaceAll(/(?s)\s+/, " ").trim()
    }


    def sanitizeAlias(def alias) {
        alias = alias.toString().replaceAll(/\(pdf\)/, "")
        alias = alias.toString().replaceAll(/(?ism)Focus 2000 Financial Corp.+?$/, "Focus 2000 Financial Corp")
        alias = alias.toString().replaceAll(/-/, "").trim()
        alias = alias.toString().replaceAll(/-$/, "").trim()
        alias = alias.toString().replaceAll(/(?s)\s+/, " ").trim()
        alias = alias.replaceAll(/&\S+;\s*$/, "").trim()

        return alias
    }


}