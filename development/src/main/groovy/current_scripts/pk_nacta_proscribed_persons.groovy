package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

NACTA script = new NACTA(context)
script.initParsing()

class NACTA {
    ScrapianContext context = new ScrapianContext()
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def url = "https://nfs.punjab.gov.pk"
    //static def url = "https://nfs.punjab.gov.pk/?counter=3224&page=17"
    def lastPage

    NACTA(context) {
        this.context = context
    }

    def initParsing() {

        //Invoke html
        def html = invoke(url)

        //Get the total no of page
        getTotalPageNo(html)

        //Get all the page data
        getAllPageData()
    }

    def getTotalPageNo(def html) {
        //Total Page Number
        def lastpageMatcher = html =~ /(?ism)\d+(?=" aria-label="last")/
        if (lastpageMatcher.find()) {
            lastPage = lastpageMatcher.group(0)
            lastPage = Integer.parseInt(lastPage)
        }
    }

    def getAllPageData() {
        //Invoke all pages (From 1st Page to the last Page)
        def pageUrl
        for (def i = 1; i <= lastPage; i++) {
            pageUrl = url + "/?counter=3490&page=" + i
            def pageHtml = invoke(pageUrl)
            //Get data from table
            getDataFromTable(pageHtml)
        }
    }

    def getDataFromTable(def html) {

        def tableMatcher
        def tableData
        def rowMatcher
        def rowData
        def firstName
        def lastName
        def province
        def identificationValue
        def identificationValueList
        //Table matcher
        tableMatcher = html =~ /(?ism)<\/thead>(.*?)<\/table>/
        if (tableMatcher.find()) {
            tableData = tableMatcher.group(1)
        }

        //Row matcher
        rowMatcher = tableData =~ /(?ism)<tr>(.*?)<\/tr>/
        while (rowMatcher.find()) {
            rowData = rowMatcher.group(1)
            def aliasList = []
            def alias

            // Data matcher - firstName, lastName, identidicationValue, province
            def matcher1 = rowData =~ /(?ism)(<td.*?){2}>(.*?)<\/td>\s*\n<td>(.*?)<\/td>\s*\n<td>(.*?)<\/td>\s*\n<td>(.*?)<\/td>/
            if (matcher1.find()) {
                firstName = matcher1.group(2)
                lastName = matcher1.group(3)
                identificationValue = matcher1.group(4)
                province = matcher1.group(5)
                province = province.replaceAll(/amp;/, " ").trim()
            }

            //Name & Identification Value Sanitize
            firstName = nameSanitize(firstName)
            lastName = nameSanitize(lastName)
            identificationValue = identificationValueSanitize(identificationValue)

            //Separate Alias from firstname
            (firstName, alias) = separateAlias(firstName)
            alias.each {
                aliasList.add(it)
            }

            //Separate Alias by 'Alias' from firstName
            (firstName, alias) = separateAliasbyAlias(firstName)
            alias.each {
                aliasList.add(it)
            }

            //Separate Alias by 'Slash' from firstName
            (firstName, alias) = separateAliasBySlash(firstName)
            alias.each {
                aliasList.add(it)
            }

            //Separate Alias from lastname
            (lastName, alias) = separateAlias(lastName)
            alias.each {
                aliasList.add(it)
            }

            //Separate Alias by 'Alias' from lastname
            (lastName, alias) = separateAliasbyAlias(lastName)
            alias.each {
                aliasList.add(it)
            }

            //Separate Alias by 'Slash' from lastname
            (lastName, alias) = separateAliasBySlash(lastName)
            alias.each {
                aliasList.add(it)
            }
            //first Name as alias
            aliasList.add(firstName.toString().replaceAll(/&#xA0;/,"").replaceAll(/\(.*?\)/, "").replaceAll(/\(|\)/, "").replaceAll(/,\s*$/,"").trim())

            //Merge firstName and lastName to get entityName
            def entityName = firstName + " " + lastName

            //Separate Bracket alias from entityname
            (entityName, alias) = separateBracketAlias(entityName)
            aliasList.add(alias)

            //Sanitize entityName
            entityName = entityName.replaceAll(/(?ism)&#xA0;?/, " ").replaceAll(/\(|\)/, "").trim()
            entityName = entityName.replaceAll(/,/, " ").trim()

            //Remove element from aliasList if aliasList == alias
            aliasList.removeAll { it.equals("Alias") }
            //Remove element from aliasList if empty
            aliasList.removeAll { it.equals(" ") }


            //Separate identificationValue by '/'
            identificationValueList = separateIdentificationValue(identificationValue)


            //Create Entity
            if (entityName) {
                createEntity(entityName, aliasList, identificationValueList, province)
            }
        }
    }

    def nameSanitize(def name) {

        name = name.toString().replaceAll(/(?ism)^N\.A$/, " ").trim()
        name = name.toString().replaceAll(/(?ism)Major \(R\)/, " ").trim()
        name = name.toString().replaceAll(/(?ism)Doctor|Engineer|Captain|Dr\.|Ex-MLA|Major|Capt:|Advocate/, "").trim()
        name = name.replaceAll(/(?ism)unknown|Absent|late/, "").trim()
        name = name.replaceAll(/(?ism)\./, " ")
        name = name.replaceAll(/(?ism)s\/o(.*)|r\/o(.*)/, "").trim()
        name = name.replaceAll(/(?ism)&#x2013;/, "").trim()
        name = name.replaceAll(/(?ism)&#x9;/, "").trim()
        name = name.replaceAll(/(?ism)\(\s*(Afghan National|Shifted to Muzafar Ghar|Shifted Bahwalpur)\s*\)/, "").trim()
        name = name.replaceAll(/(?ism)caste.*/, "").trim()
        name = name.replaceAll(/(?ism)Afghan National/, "").trim()
        name = name.replaceAll(/(?ism)\(CNIC Father\)/, "").trim()
        name = name.replaceAll(/(?ism)ex president BSF/, "").trim()
        name = name.replaceAll(/(?ism)Shifted Bahawalpur/, "").trim()
        name = name.replaceAll(/(?ism)^0$/, "").trim()
        return name
    }

    def identificationValueSanitize(def identificationValue) {
        identificationValue = identificationValue.replaceAll(/(?ism)&#xA0;|&#xA0/, " ").trim()
        identificationValue = identificationValue.replaceAll(/(?ism)&#x2B;/, "+").trim()
        identificationValue = identificationValue.replaceAll(/(?ism)Resident of Pakistan|dead|asd|NIL/, " ").trim()
        identificationValue = identificationValue.replaceAll(/(?ism)^o$/, " ").trim()

        return identificationValue
    }

    def separateAlias(def name) {
        def aliasMatcher
        def totalAlias
        def aliasList

        //Separate Alias by '@'
        if (name.toString().contains("@")) {
            if ((aliasMatcher = name =~ /(?ism)@(.*?)($|\()/)) {
                totalAlias = aliasMatcher[0][1]
                name = name.toString().replaceAll(/@[\s|\n]*$totalAlias/, "")
                aliasList = totalAlias.split("@").collect({ its -> return its })
            }
        }

        return [name, aliasList]
    }

    def separateAliasbyAlias(def name) {
        def aliasMatcher
        def totalAlias
        def aliasList

        //Separate Alias by 'Alias'
        if (name.toString().contains("alias") || name.toString().contains("Alias") || name.toString().contains("ALIAS")) {
            if ((aliasMatcher = name =~ /(?ism)alias(.*?)($|\()/)) {
                totalAlias = aliasMatcher[0][1]
                if (!totalAlias.equals(")")) {
                    totalAlias = totalAlias.toString().replaceAll(/(?s)\)/,"")
                    name = name.toString().replaceAll(/(?ism)\(?\s?alias[\s|\n]*$totalAlias/, "")
                    aliasList = totalAlias.split("(?i)alias").collect({ its -> return its })
                }
            }
        }
        //Separate Alias by 'code name'
        else if (name.toString().contains("code name")) {
            if ((aliasMatcher = name =~ /(?ism)code name(.*?)($|\()/)) {
                totalAlias = aliasMatcher[0][1]
                name = name.toString().replaceAll(/(?ism)\(?\s?code name[\s|\n]*$totalAlias/, "")
                aliasList = totalAlias.split("(?i)code name").collect({ its -> return its })

            }
        }

        //Separate Alias by 'Comma'
        aliasList.each {
            if (it.contains(",")) {
                aliasList = it.split(",").collect({ its -> return its })
            }
        }

        return [name, aliasList]
    }

    def separateAliasBySlash(def name) {
        def totalAlias
        def aliasList = []

        //Separate Alias by '/'
        if (name.toString().contains("/")) {
            def aliasMatcher2 = name =~ /(?ism)[^|\/]+(.*?)\//
            while (aliasMatcher2.find()) {
                totalAlias = aliasMatcher2.group(0)
                name = name.toString().replaceAll(/(?ism)$totalAlias/, "")
                aliasList.add(totalAlias.toString().replaceAll(/\//, "").trim())
            }
        }

        return [name, aliasList]
    }

    def separateBracketAlias(def name) {
        def totalAlias
        def aliasList = []

        // Separate Bracket Alias
        if (name.toString().contains("(")) {
            def aliasMatcher = name =~ /(?ism)(\(.*?\))/
            while (aliasMatcher.find()) {
                totalAlias = aliasMatcher.group(1)
                aliasList.add(totalAlias.toString().replaceAll(/\(|\)/, "").trim())
                name = name.toString().replaceAll(/\($totalAlias\)/, "")
            }
        }

        // Merge the Bracket Alias
        aliasList = aliasList.join(" ")

        return [name, aliasList]
    }

    def separateIdentificationValue(def identificationValue) {
        def valueList = []

        //Separate identificationValue by '/'
        if (identificationValue.toString().contains("/")) {
            valueList = identificationValue.split("/").collect({ its -> return its })
        } else {
            valueList.add(identificationValue)
        }
        return valueList
    }

    def createEntity(def entityName, def aliasList, def identificationValueList, def province) {

        def entity = null
        entity = context.findEntity("name": entityName)
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(entityName)
            entity.setType("P")
        }

        //Add Alias
        if (aliasList) {
            aliasList.each {
                if (it) {
                    entity.addAlias(it.replaceAll(/(?s)\s+|\)/, " ").trim())
                }
            }
        }
        //Add Address
        ScrapeAddress scrapeAddress = new ScrapeAddress()
        scrapeAddress.setProvince(province)
        scrapeAddress.setCountry("Pakistan")
        entity.addAddress(scrapeAddress)

        //Add Event
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription("This entity appears on the Pakistan National Counter Terrorism Authority list of Proscribed Persons. These entities are individuals about whom either there is a credible intelligence-information or who have a history of being linked to a Proscribed Organization.")
        entity.addEvent(event)


        //Add Identification
        identificationValueList.each {
            ScrapeIdentification scrapeIdentification = new ScrapeIdentification()
            scrapeIdentification.setType("National ID Number")
            scrapeIdentification.setValue(it.toString().replaceAll(/(?s)\s+/, " ").trim())
            if (it) {
                entity.addIdentification(scrapeIdentification)
            }
        }
    }

    def invoke(url, cache = false, tidy = false) {
        return context.invoke([url: url, tidy: tidy, cache: cache])
    }
}