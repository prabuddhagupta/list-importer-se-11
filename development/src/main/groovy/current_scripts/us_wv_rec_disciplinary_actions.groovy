package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Disciplinary_Actions script = new Disciplinary_Actions(context)
script.initParsing()
script.staticEntity()


class Disciplinary_Actions {
    final entityType
    final def ocrReader
    ScrapianContext context = new ScrapianContext()
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def url = "https://rec.wv.gov/About/Pages/Disciplinary-Actions.aspx"

    Disciplinary_Actions(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context

    }

    def initParsing() {
        //Invoke html
        def html = invoke(url)
        //html = html.toString().replaceAll(/(?s)<ul><li><a title="Complaint - Jeff Spickler L-20-014".*/, "")
        html = html.toString().replaceAll(/<\/li><li><a href="\/Education\/.*22-018.pdf.*?<\/a><\/li>/, "")
        html = html.toString().replaceAll(/<li><a href="\/About\/Documents\/.*?Cathy%20E\.%20Smith\.pdf.*?<\/a><\/li>/, "")
        html = html.toString().replaceAll(/<li><a href="\/About\/Documents\/.*?WV-0024561\.pdf.*?<\/a><\/li>/, "")
        html = html.toString().replaceAll(/<li><a href="\/About\/Documents\/.*?CONSENT%20DECREE\.pdf.*?<\/a>/, "")
        html = html.toString().replaceAll(/<\/p><ul><li><a href="\/About\/Documents\/.*?C-21-028\.pdf.*?<\/a>/, "")
        //Get Data from the Table
        getDataFromHtml(html)
    }

    def getDataFromHtml(def html) {
        def Data
        def root = "https://rec.wv.gov"
        def dataMatcher = html =~ /(?ism)<div class="housebkg">.+<div style="clear:both;"><\/div>\s*\n*<\/div>\s*\n*<\/div>\s*\n*<\/div>\s*\n*<div.*?>/
        //def dataMatcher = html =~ /(?ism)<div class="housebkg">.+<br><\/p>/
        while (dataMatcher.find()) {
            Data = dataMatcher.group(0)

            //Start Capturing Entity Url
            def pdfurl
            def pdfList = []
            def urlMatcher = Data =~ /(?im)href="(.+?pdf)"/
            while (urlMatcher.find()) {
                pdfurl = urlMatcher.group(1)

                pdfurl = pdfurl.trim()
                pdfList.add(root + pdfurl)
            }
            def uniquepdf = pdfList.toUnique()
            //End of capturing entity url

            uniquepdf.each {
                it = it.toString().replaceAll(/\s+/, "").trim()
                def pdfData = pdfToTextConverter(it)

                //Starting name and alias capture
                def nameList = []
                def aliasList = []
                def nameDataMacher = pdfData =~ /(?ism)\s+(?:MATTER OF:|v\.)\s*.*?(?:\([A-Z]\))?\n*(.+?)\n*\s*(?:Respondent\.|CONSENT DECREE|Rapondent\,|Respondents\.)/
                if (nameDataMacher.find()) {
                    def nameData = sanitizeName(nameDataMacher.group(1))
                    def nameMacher = nameData =~ /(?ism)([\w\'\.\(\)\-]+[\s\w\,\.\'\(\)\-\/]+?)License.+?\n?.+?\d{7}/
                    while (nameMacher.find()) {
                        def name = nameMacher.group(1).toString().replaceAll(/\band\b/, "").trim()
                        name = sanitizeName(name)
                        nameList.add(name)
                    }
                }
                // Ending name and alias capture

                //Creating Entity
                nameList.each { name ->
                    (name, aliasList) = aliasChecker(name)
                    createEntity(name, aliasList, it)
                }

            }
        }
    }

    def staticEntity() {
        createEntity("Elizabeth Ann Rhodes", "", "https://rec.wv.gov/About/Documents/Consent%20Decree%20C-21-028.pdf")
        createEntity("Nancy Jean Boggs", "", "https://rec.wv.gov/About/Documents/Consent%20Decree%20C-21-028.pdf")
        createEntity("MICHELLE L. MARTIN", "", "https://rec.wv.gov/About/Documents/Complaint%20No.%20P-16-004%20-%20Michelle%20L.%20Martin%20WV-0024561.pdf")

    }

    def createEntity(def name, def aliasList, def entityUrl) {

        def entity = null

        name = name.toString().replaceAll(/APR 1 4 2021\s+West Virginia\s+Real Estate Commission/, "")
        name = name.replaceAll(/MAR 1 1 2021\s+West Virginia\s+Real Estate Commission/,"")
        name = name.replaceAll(/NOV 2 9 1021\s+West Virginia\s+MARGARET ANN OSBORNE,\s+Real Estate Commission/, "MARGARET ANN OSBORNE")
        if (!name.toString().isEmpty()) {
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType]);
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }
            aliasList.each {
                if (it) {
                    it = it.replaceAll(/(?s)\s+/, " ").trim()
                    entity.addAlias(it)
                }
            }

            //adding address
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            scrapeAddress.setProvince("West Virginia")
            scrapeAddress.setCountry("United States")
            entity.addAddress(scrapeAddress)

            //adding entity url
            if (entityUrl) {
                entity.addUrl(entityUrl)
            }

            //adding event description
            ScrapeEvent event = new ScrapeEvent()
            def status = "This entity appears on the West Virginia Real Estate Commission list of Disciplinary Actions."
            event.setDescription(status)
            entity.addEvent(event)
        }
    }

    def detectEntity(def name) {
        def type
        if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:Aid|Creativity|\bVets\b|\bObesity\b|\bHomeless\b|Sanctuary|Kayla|Change|Purpose|Life|Welfare|Term|Dynamics|Abuse|Cub|Rebound|Speak|Police|Boys|Charities|Opportunity|Pennsylvania|Vision|Mission|Alternatives|Adoptions|Camp|Embassy|Christie|Geek|Fall|Policy|Publications|Cure|Brotherhood|Studios|Forum|Powerkids|Workshop|Hightower|Families|Citizens|Wishes|Nationalist|Brothers|Cancer|Autism|Hope|Americans|Clinic|Medicine|Animals|Future)/) {
                    type = "O"
                }
            }
            if (type.equals("O")) {
                if (name =~ /(?i)(?:stephen\sL.|\bSANDRA\b|Thomas\sShields)|Jason\sHunt/) {
                    type = "P"
                }
            }
        }
        return type
    }

    def sanitizeName(def name) {
        name = name.toString().replaceAll(/(?im)(?:Formal\s*)?Complaint.*?\s*?.*[\w]\-\d{2}\-\d{2,3}(?:\([\w]\))?/, "").trim()
        name = name.toString().replaceAll(/(?im)Consolidated.*?\s*?.*[\w]\-\d{2}\-\d{2,3}(?:\([\w]\))?/, "")
        name = name.toString().replaceAll(/(?i)consolidated with/, "")
        name = name.replaceAll(/Associate Broker/, "")
        name = name.replaceAll(/(?i)RECEIVED/, "")
        name = name.replaceAll(/\,\s*\,/, ",")
        name = name.replaceAll(/(?i)\sJR\./, "")
        name = name.toString().replaceAll(/Respondent/, "")
        name = name.replaceAll(/\,$/, "")
        name = name.replaceAll(/WeLEWLED\s+and\s+6tcy 11 I 21t1\s+(?=EVE ELIZABETH LEOMBRUNO,)/, "")
        name = name.replaceAll(/(?<=EVE ELIZABETH LEOMBRUNO,)\s+West Virginia/, "")
        name = name.trim()
        return name
    }

    def aliasChecker(def name) {
        name = name.toString().replaceAll(/(?i)(?:\ba[\.\/]?k[\.\/]?a\b|\bf[\.\/]?k[\.\/]?a\b|\b(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a\b|\bn[\.\/]?k[\.\/]?a\b)/, "a.k.a")
        def aliasList = []
        def alias
        if (name.toString().contains("(") && !(name.toString().contains("(a.k.a"))) {
            if (!(name.toString().contains(")"))) {
                def aliasMatcher = name =~ /(?i)\((.*)/
                while (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1)
                    name = name.toString().replaceAll(/(?i)\((.*)/, "").trim()
                    aliasList.add(alias)
                }
            } else {
                def aliasMatcher = name =~ /(?i)\((.*?)\)/
                while (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1)
                    name = name.toString().replaceAll(/(?i)\((.*?)\)/, "").trim()
                    aliasList.add(alias)
                }
            }
        }
        if (name =~ /(?i)a\.k\.a(.+)/) {
            def aliasMatcher = name =~ /(?i)a\.k\.a(.+)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/(?i)a\.k\.a(.+)/, "").trim()
                if (alias =~ /(?i)a\.k\.a(.+)/) {
                    alias = alias.split(/(?i)a\.k\.a/)
                    alias.each { it ->
                        aliasList.add(it)
                    }
                } else {
                    aliasList.add(alias)
                }
            }
        }
        if (name =~ /(?i)(.+)a\.k\.a/) {
            def aliasMatcher = name =~ /(?i)(.+)a\.k\.a/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/(?i)(.+)a\.k\.a/, "").trim()
                if (alias =~ /(?i)(.+)a\.k\.a/) {
                    alias = alias.split(/(?i)a\.k\.a/)
                    alias.each { it ->

                        aliasList.add(it)
                    }
                } else {

                    aliasList.add(alias)
                }
            }
        }
        if (name =~ /\/[\w\-\s\,\.\']+/) {
            def aliasMatcher = name =~ /\/[\w\-\s\,\.\']+/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/\/[\w\-\s\,\.\']+/, "").trim()
                aliasList.add(alias)
            }
        }
        return [name, aliasList]
    }

    def pdfToTextConverter(def pdfUrl) {
        List<String> pages = ocrReader.getText(pdfUrl)
        return pages
    }

    def invoke(url, cache = false, tidy = false) {
        return context.invoke([url: url, tidy: tidy, cache: cache])
    }

}