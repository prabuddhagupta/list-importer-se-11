package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils

import java.text.SimpleDateFormat

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

AsicBanned script = new AsicBanned(context)
script.initParsing()
//script.debug();

class AsicBanned {
    final ScrapianContext context
    final String mainUrl = "https://data.gov.au/preview-map/proxy/_0d/https://data.gov.au/data/dataset/e08a07dc-e1e7-4ab9-95c0-a7930d2f6a39/resource/741da9e3-7e0c-458e-830c-c518698e1788/download/bd_per_201909.csv"

    AsicBanned(context) {
        this.context = context
    }


//------------------------------Initial part----------------------//

    def initParsing() {
        def html = context.invoke([url: mainUrl])

        def rows = csvParser(html.toString())
        rows.each { row ->
            def name = row.bd_per_name
            def city = sanitize(row.bd_per_add_local)
            def state = sanitize(row.bd_per_add_state)
            def postalCode = sanitize(row.bd_per_add_pcode)
            def startDate = row.bd_per_start_dt
            def endDate = row.bd_per_end_dt

            def aliases = name.split(/(?i)(?:a\/k\/a|\baka\b|Formerly)/)
            name = aliases[0].toString().replaceAll(/\(/, "").replaceAll(/(?s)\s+/, " ").trim()
            createEntity(name, aliases, city, state, postalCode, startDate, endDate)
        }
    }

    def createEntity(name, aliases, city, state, postalCode, startDate, endDate) {
        def entity, type

        type = detectEntityType(name)
        if (type.equals("P")) {
            name = personNameReformat(name)
        }

        entity = context.findEntity(["name": name])
        if (!entity) {
            entity = context.getSession().newEntity()
            name = StringUtils.remove(name, ",")
            if (name) {
                entity.setName(name)
                entity.setType(type)
            }
        }
        for (int i = 1; i < aliases.length; i++) {
            def alias = StringUtils.removeEnd(aliases[i].toString().trim(), ",")
            entity.addAlias(alias)
        }
        ScrapeAddress address = new ScrapeAddress()
        address.setCountry("Australia")
        if (state) {
            address.setProvince(state)
        }
        if (city) {
            address.setCity(city)
        }
        if (postalCode) {
            address.setPostalCode(postalCode)
        }
        entity.addAddress(address)
        ScrapeEvent event = new ScrapeEvent()
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy")
        if (startDate && endDate) {
            java.util.Date date1 = sdf.parse(startDate);
            java.util.Date date2 = sdf.parse(endDate);
            sdf.format(date1)
            sdf.format(date2)
            if (date1.compareTo(date2) > 0) {
                startDate = sdf.format(date2)
                endDate = sdf.format(date1)
            }
        }

        if (startDate) {
            def sDate = context.parseDate(new StringSource(startDate), ["dd/MM/yyyy"] as String[])
            event.setDate(sDate)
        }
        if (endDate) {
            def eDate = context.parseDate(new StringSource(endDate), ["dd/MM/yyyy"] as String[])
            event.setEndDate(eDate)
        }
        event.setDescription("This entity appears on the Australian Securities and Investments Commission list of banned or disqualified persons. This person is banned from involvement in the management of a corporation, from auditing Self-Managed Superannuation Funds and from practicing in the Australian Financial Services or credit industry.")
        entity.addEvent(event)

    }

    def detectEntityType(name) {
        def type = context.determineEntityType(name)
        if (type.equals("P")) {
            if (name =~ /(?i)\b(?:L\.L\.C|\s+&\s+)\b/) {
                return "O"
            }
        }
        return type
    }

    def personNameReformat(name) {
        name = name.toString().replaceAll(/(?i)(.*)~(.*)/, "\$2 \$1")
        return name.toString().trim()
    }

    def csvParser(String text) {
        text = text.replaceAll(/""/, "\" \"")
        text = text.replaceAll("^\"[^\n]+", "")
        text = text.replaceAll(/("[^"\n]+")/, { a, b -> return a.replaceAll(/,/, '~') }).replaceAll(/"/, '')

        def results = []

        def text1 = text =~ /(?s)^\s*([^\n]+)(.*)/
        def colNames = text1[0][1].split(/\s*,\s*/).collect({
            return it.trim().toLowerCase().replaceAll("/|\\s+", "_")
        })

        def text2 = text1[0][2] =~ /([^\n]+)/
        while (text2.find()) {
            def row = [:]
            def lineArr = text2.group(1).trim().split(/\s*,\s*/)
            (0..<colNames.size()).each() {
                row[colNames[it]] = lineArr[it]
            }
            results.add(row)
        }

        return results
    }

    def sanitize(data) {
        return data.toString().replaceAll(/(?i)ADDRESS UNKNOWN/, "").trim()
    }
}
