package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 25000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

SaRbpa rbpa = new SaRbpa(context)
rbpa.initParsing()


class SaRbpa {

    final ScrapianContext context
    def pdfUrl
    def root = 'https://www.resbank.co.za'
    def mainUrl = root + '/PrudentialAuthority/AML-CFT/Pages/Enforcement.aspx'

    SaRbpa(ScrapianContext context) {
        this.context = context
    }
//------------------------------Initial part----------------------//
    def initParsing() {
        def sourceHtml = context.invoke([url: mainUrl, cache: false])
        def pdfLinkMatcher = sourceHtml =~ /(?s)<a\s+href="([^"]+)">click here<\\/a>/
        if (pdfLinkMatcher) {
            pdfUrl = root + pdfLinkMatcher[0][1]
        }
        def text = pdfConverter(pdfUrl)
        parsePdf(text, pdfUrl)

        parseHtml()
    }

    def parseHtml() {
        def name, date, descr, url,invokingUrl="https://www.resbank.co.za/PrudentialAuthority/FinancialSectorRegulation/Pages/Administrative-Penalties.aspx"
        def sourceHtml = context.invoke([url: invokingUrl])
        def blockMatcher = sourceHtml =~ /(?si)<h1.+?Regulatory Actions.+?<tbody>(.+?)<\\/tbody>/
        def blockData
        if (blockMatcher) {
            blockData = blockMatcher[0][1].toString().replaceAll(/(?si)\A<tr.+?<\\/tr>/, "")
            def entityMatcher = blockData =~ /(?is)(<tr.+?<\\/tr>)/
            while (entityMatcher.find()) {
                def entityData = entityMatcher.group(1)
                def dataMatcher = entityData =~ /(?si)(\d{4}\-\d{2}-\d{2}).+?<td.+?<\\/td>\s*<td[^>]+>(.+?)<\\/td>.+?<span[^>]+>(.+?)<\\/span>.+?<a\s*href="(.+?)">/
                if (dataMatcher) {
                    date = dataMatcher[0][1]
                    name = dataMatcher[0][2].toString().replaceAll(/(?si)<[^>]+>/, "")
                    descr = dataMatcher[0][3]
                    url = root + dataMatcher[0][4]
                    if (name)
                        createEntity(sanitizeValue(name), date, sanitizeValue(descr), url,invokingUrl)
                }
            }
        }
    }

    def parsePdf(text, url) {

        text = fixPdf(text)
        // Block Matching From Total Data
        def blockMatcher = text =~ /(?ms)(^\s*\d+.*?)(?=^\s*\d+\s{3}|\Z)/
        while (blockMatcher.find()) {
            def blockData = blockMatcher.group(1)
            def date, name, aSanction

            def blockDataLineMatcher = blockData =~ /[^\n]+\n?/
            int lineCount = 0
            while (blockDataLineMatcher.find()) {
                def lineText = blockDataLineMatcher.group(0)
                if (lineCount == 0) {
                    // Line matcher For First Line Only
                    def lineMatcher = lineText =~ /(?m)^\s?\d+\s+(.*?)\s{2,}(.*?)\s{2,}(.*?)\s{2,}/
                    if (lineMatcher) {
                        name = lineMatcher[0][1]
                        date = lineMatcher[0][2]
                        aSanction = lineMatcher[0][3]
                    }
                }
                // Line matcher For After First Line Only
                else {
                    def lineMatcher
                    // name and sanction
                    if ((lineMatcher = lineText =~ /^\s{6,12}((?:\S+\s)+)\s*((?:\S+\s)+)/)) {
                        name += ' ' + lineMatcher[0][1]
                        aSanction += ' ' + lineMatcher[0][2]

                    }
                    // name only
                    else if ((lineMatcher = lineText =~ /^\s{2,12}((?:\S+|\S+\s)+)\u0024/)) {
                        name += ' ' + lineMatcher[0][1]
                    }
                    // Sanction only
                    else if ((lineMatcher = lineText =~ /^\s{50,70}((?:\S+\s)+)/)) {
                        aSanction += ' ' + lineMatcher[0][1]
                    }
                }

                lineCount++;
            }
            if (name)
                createEntity(sanitizeValue(name), date, sanitizeValue(aSanction), url, mainUrl)
        }

    }

    def createEntity(name, date, aSanction, url, mainUrl) {

        def entity

        entity = context.findEntity("name": name, type: 'O')

        if (entity == null) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType("O")
        }

        ScrapeAddress scrapeAddress = new ScrapeAddress()
        scrapeAddress.setCountry("SOUTH AFRICA")
        entity.addAddress(scrapeAddress)

        ScrapeEvent event = new ScrapeEvent()

        def eventDate = context.parseDate(new StringSource(date), 'yyyy-MM-dd')
        if (eventDate) {
            event.setDate(eventDate)
        }
        def description
        if (mainUrl=="https://www.resbank.co.za/PrudentialAuthority/FinancialSectorRegulation/Pages/Administrative-Penalties.aspx") {
            description = 'This entity appears on the South African Reserve Bank Prudential Authority\'s list of Regulatory Actions. Outcome: '
            if (mainUrl) {
                entity.addUrl(mainUrl)
            }
        } else {
            description = 'This entity appears on the South African Reserve Bank Prudential Authority\'s list of Administrative Sanctions. Administrative Sanction: '
            if (url) {
                entity.addUrl(url)
            }
        }
        event.setDescription(description + aSanction)

        entity.addEvent(event)
    }

    def fixPdf(text) {
        text = text.replaceFirst(/(?s)\A(.*?)(?=\s*\bNo\b)/, '')
            .replaceAll(/(?m)Page.*/, '')
            .replaceAll(/\u000C/, '')
            .replaceAll(/&amp;/, '&').replaceAll(/&nbsp;/, '')
            .replaceAll(/(?i)(No\s+Bank|Date of SARB|media release|Administrative Sanction|Details pertaining to non-compliance with the|provisions of the FIC Act)/, '')
            .replaceAll(/(?ms)^\s*$/, '')
        return text
    }
    //------------------------------Misc utils part---------------------//
    def sanitizeValue(data) {
        if (!data)
            return null
        return data.replaceAll(/(?s)\s+/, " ")
            .replaceAll('\\ufffd', 'e')
            .replaceAll('\\u200B', '')
            .trim()
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
}