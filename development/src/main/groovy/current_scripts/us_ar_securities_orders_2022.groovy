package current_scripts

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 50000, socketTimeout: 100000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36"])
context.session.encoding = "UTF-8";
context.session.escape = true;

debug = true

if (debug) {
    handleIndexPage("https://securities.arkansas.gov/legal-category/orders?f=1&legal_category=268&legal_year=2022&f_post_type=adapt_dev_legal&posts_per_page=9")
} else {
    handleIndexPage(context.scriptParams.indexUrl);
}

def handleIndexPage(indexUrl) {
    yearMatch = indexUrl =~ /legal_year=(\d+)&/
    def year
    if (yearMatch) {
        year = yearMatch[0][1]
    }
    indexPage = context.invoke([url: indexUrl, cache: false, tidy: false, clean: false]);
    def matches = context.regexMatches(indexPage, [regex: ">(\\d+)<\\/a><\\/li>"]);
    if (matches) {
        matches.each { next ->
            nextPage = next[1]
            pageUrl = "https://securities.arkansas.gov/legal-category/orders/page/$nextPage/?f=1&legal_category=268&legal_year=$year&f_post_type=adapt_dev_legal&posts_per_page=9%2F"
            pageSource = context.invoke([url: pageUrl, cache: false, tidy: false, clean: false]);
            handleOtherPage(pageSource)
        }
    }
    indexPage = indexPage.replace(/<\/p><\/div> <a/, "</p><a")
    indexPage = indexPage.replace(/<div class="post-content"><\/div> <a/, "</p><a")
    context.elementSeek(indexPage.stripFormatting(), [element: "div", startText: "</noscript></div>", endText: "<!-- posts-container", greedy: false]).each { div ->
        entry = context.regexMatch(div, [regex: "(?s)<h4.*?>(.*)</h4>\\s*<h6.*>(.*)<\\/h6>(?:<div)?.*<a href=\\\"(.*\\.pdf)\\\".*post-link\\\">(.*)</a>"]);

        name = new StringSource(entry[1].toString().toUpperCase().trim()).stripHtmlTags().stripEntities().convertNbsp();
        eventDate = entry[2];
        entityUrl = entry[3];
        caseNumber = entry[4];
        dataProcess(name, entityUrl, eventDate, caseNumber)
    }
}


def handleOtherPage(pageSource) {
    pageSource = pageSource.replace(/(?:<\/p><\/div> <a|<div class="post-content"><\/div>\s*<a)/, "</p><a")
    context.elementSeek(pageSource.stripFormatting(), [element: "div", startText: "</noscript></div>", endText: "<!-- posts-container", greedy: false]).each { div ->
        entry = context.regexMatch(div, [regex: "(?s)<h4.*?>(.*)</h4>\\s*<h6.*>(.*)<\\/h6>(?:<div)?.*<a href=\\\"(.*\\.pdf)\\\".*post-link\\\">(.*)</a>"]);

        name = new StringSource(entry[1].toString().toUpperCase().trim()).stripHtmlTags().stripEntities().convertNbsp();
        eventDate = entry[2];
        entityUrl = entry[3];
        caseNumber = entry[4];
        dataProcess(name, entityUrl, eventDate, caseNumber)
    }
}

def dataProcess(name, entityUrl, eventDate, caseNumber) {
    name.toString().split("-BREAK-").each { nameChunk ->
        nameSource = new StringSource(nameChunk);
        if (nameSource.toString().endsWith(" WITH") || nameSource.toString().contains(" WITH ")) {
            nameStr = nameSource.toString()
            idx = nameStr.indexOf(" WITH")
            nameStr = nameStr.substring(0, idx)
            nameSource = new StringSource(nameStr)
        }
        addEntity(nameSource, entityUrl, eventDate, caseNumber)
    }
}

def addEntity(name, url, eventDate, caseNumber) {

    name = name.toString().trim();
    if (name.length() > 1) {
        name = name.toString().replaceAll(/, LLC/, " LLC").trim()
        name = name.toString().replaceAll(/, INC\./, " INC").trim()
        name = name.toString().replaceAll(/LLC, AND/, "LLC AND").trim()
        name = name.toString().replaceAll(/, AND/, ",").trim()
        name = name.toString().replaceAll(/(?i)\sAND\s/, " , ").trim()
        name = name.toString().replaceAll(/(?i), A SERIES OF /, " DBA ").trim()
        name = name.toString().replaceAll(/\sand\s/, " , ").trim()


        def nameList = []

        if (name.toString().contains(",")) {
            def nameSplit = name.toString().split(",")
            nameSplit.each {
                if (it) {
                    nameList.add(it.toString().trim())
                }
            }
        } else {
            nameList.add(name)
        }

        nameList.each {
            if (it) {
                it = it.toString().replaceAll(/A\/K\/A/, "DBA").trim()
                it = it.toString().replaceAll(/D\/B\/A/, "DBA").trim()

                def entityName = it.toString()
                def aliasList = []

                if (entityName.toString().contains(" DBA ")) {
                    def nameSplitter = entityName.toString().split("DBA")
                    entityName = nameSplitter[0]
                    def alias = nameSplitter[1]
                    aliasList.add(alias.toString().trim())
                }

                def entityType = determineEntityType(entityName);

                def entity = context.findEntity([name: entityName, type: entityType]);
                if (entity == null) {
                    entity = context.getSession().newEntity()
                    entity.setName(sanitize(entityName))
                    entity.setType(entityType)

                    aliasList.each {
                        entity.addAlias(sanitize(it.toString()))
                    }
                    def address = new ScrapeAddress()
                    address.setProvince("ARKANSAS")
                    address.setCountry("UNITED STATES")
                    entity.addAddress(address)
                }

                url = url.toString()
                if (!entity.getUrls().contains(url)) {
                    entity.addUrl(sanitize(url));

                    def event = new ScrapeEvent();
                    event.setDate(context.parseDate(eventDate))
                    event.setDescription("Entity appears on the Arkansas Securities Dept. Administrative Orders site. Case number: " + caseNumber.toString())
                    entity.addEvent(event)
                }
            }
        }
    }
}

def determineEntityType(name) {
    def nameUpper = name.toString().toUpperCase();
    def entityType = context.determineEntityType(nameUpper, ["LP", "FUND", "BANK", "EQUITY",]);
    ["EXCHANGE", "ENTERPRISE", "EASY", ".COM", "PARTNERSHIP", "MORTGAGE", "INVESTMENTS",
     "INFORMATION", ".NET", "PLLC", "LIQUIDATION", "SURGERY", "FUNDING", "FINANCIAL",
     "GLOBAL", "CORPORATATION", "LENDING", "EASY BAND", "CONSULTANTS", "FOUNDATION",
     "MEDICAL", "PARTNERS", "PROGRAMS", "INSTITUTE", "TOYOTA", "PROPERTIES", "CENTER",
     "CAMP DAVID", "EPISCOPAL", "COMMUNICORP", "CO-OP", "PORTLAND", "CITIZENS", "CHARITABLE", "VENTURE",
     "INVESTMENT", "COMPANIES", "PROJECT", "WEALTH", "SIDNEY EUGENE BANKS", "DEFT"].each { token ->
        if (nameUpper.contains(token)) {
            entityType = "O"
        }
    }
    if (name.startsWith("THE ")) {
        entityType = "O"
    }
    return entityType
}

def sanitize(data) {
    return data.replaceAll(/(?s)\s+/, " ").trim()
}