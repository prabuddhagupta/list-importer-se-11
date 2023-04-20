package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Au_national_sec script = new Au_national_sec(context)
script.initParsing()

class Au_national_sec {
    final ScrapianContext context

    final String root = "https://www.nationalsecurity.gov.au/"
    final String url = root + "Listedterroristorganisations/Pages/default.aspx"

    Au_national_sec(context) {
        this.context = context
    }

//------------------------------Initial part----------------------//
    def initParsing() {
        def content, row;

        def html = invoke(url)

        def contentMatch = html =~ /(?is)<tr>\s*<td valign="top">(.*?)Listedterroristorganisations\\/Pages\\/sonnenkrieg-division.aspx/
        if (contentMatch) {
            content = contentMatch[0][1]
        }

        def rowMatch = content =~ /(?is)<li\s*class="dfwp-item">(.*?)<\\/li>/
        while (rowMatch.find()) {
            def name, date, alias, eDate;
            row = rowMatch.group(1)
            row = row.replaceAll("\\u00a0"," ")
            row = row.replaceAll(/(?is)to\s*include.*?(?=<)/,"")

            def attributeMatch = row =~ /(?is)title="">([^<]+)<.*?description">[^<]*\b((?:\d+|\w+)\s+[a-z]+\s+\d{4})\.?\s*</
            if (attributeMatch) {
                name = attributeMatch[0][1]
                date = attributeMatch[0][2]
                date = date.toString().replaceAll(/and (?=May 2021)/,"").trim()
            }

            def aliasMatch = name =~ /\(([^\)]+)\)/
            if(aliasMatch){
                alias = aliasMatch[0][1]
                name = name.replaceAll(java.util.regex.Pattern.quote(aliasMatch[0][0]),"")
            }
            createEntity(name,alias,date)
        }
    }


//------------------------------Entity creation part---------------//
    def createEntity(name, alias,date) {
        def entity;
        if (name) {
            name = sanitize(name)
            entity = context.findEntity([name: name, type: "O"])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.type = "O"
            }

            if(alias){
                entity.addAlias(alias)
            }

            def event = new ScrapeEvent()
            event.setDescription("This entity appears on the Australian National Security List of Terrorist Organisations")
            event.category = "TER"
            event.subcategory = "ASC"

            def eDate = context.parseDate(new StringSource(date), ["dd MMM yyyy", "MMM yyyy"] as String[])
            if (eDate) {
                event.setDate(eDate)
            }
            entity.addEvent(event)
        }

        ScrapeAddress addr = new ScrapeAddress()
        addr.setCountry("Australia")
        entity.addAddress(addr)

    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = true, tidy = false, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def sanitize(data) {
        return data.replaceAll(/&amp;/, '&').replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
    }

}