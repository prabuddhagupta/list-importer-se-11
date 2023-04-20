package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils

import java.text.SimpleDateFormat
import java.util.regex.Pattern

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_ga_oig_exclusions script = new Us_ga_oig_exclusions(context)
script.initParsing()


class Us_ga_oig_exclusions {
    final entityType
    final ScrapianContext context
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final String url = "https://dch.georgia.gov/office-inspector-general/georgia-oig-exclusions-list"
//"https://dch.georgia.gov/divisionsoffices/office-inspector-general/georgia-oig-exclusions-list"

    final String splitTag = "~SPLIT~"

    Us_ga_oig_exclusions(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context
    }

    def initParsing() {
        def html = invoke(url)
        def xlxsMatch = html =~ /Download this xls file.+?<a\s+.+?href="([^>]+)"/
        def xlxsUrl = xlxsMatch[0][1]
        def spreadsheet = context.invokeBinary([url: xlxsUrl]);
        def xml = context.transformSpreadSheet(spreadsheet, [validate: true, escape: true, headers: ["last_name", "first_name", "business_name", "general", "state", "sancdate"]]);
        def rows = new XmlSlurper().parseText(xml.value)
        rows.row.eachWithIndex { item, index ->

            if (index > 1) {
                def lastName = sanitize(item.last_name.toString())
                def firstName = sanitize(item.first_name.toString())

                def businessName = sanitize(item.business_name.toString())
                businessName = sanitizeBusName(businessName)

                def aliasMatch = businessName =~ /(?is)\b(?:DBA|aka)\b\s(.*?$)/
                def aliasList = []
                if (aliasMatch) {
                    def removeAliasPart = Pattern.quote(aliasMatch[0][0])
                    def alias = sanitize(aliasMatch[0][1])
                    alias = alias.replaceAll(/(?s)\s+/, " ").trim()
                    aliasList.add(alias)
                    businessName = businessName.replaceAll(/$removeAliasPart/, "")
                }

                businessName = sanitizeBusName(businessName)

                def name = firstName + " " + businessName + " " + lastName
                name = sanitizeName(name)

                def secondAliasMatch = name =~ /(?s)\((.*?)\)/
                if (secondAliasMatch) {
                    def removeAliasPart = Pattern.quote(secondAliasMatch[0][0])
                    def secondAlias = sanitize(secondAliasMatch[0][1])
                    secondAlias = secondAlias.replaceAll(/(?s)\s+/, " ").trim()
                    aliasList.add(secondAlias)
                    name = name.replaceAll(/$removeAliasPart/, "")
                }

                def eventDate = sanitize(item.sancdate.toString())
                eventDate = sanitizeDate(eventDate)

                createEntity(name, aliasList, eventDate)

            }
        }
    }

    def createEntity(name, aliasList = [], eventDate) {
        def entity
        def type

        if (name =~ /(?:PHARMACY|TRANS)$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
        }
        entity = context.findEntity([name: name, type: type])

        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(type)
        }

        aliasList.each {
            if (it) {
                it = it.replaceAll(/(?is)[\/&]\s*/, splitTag)
                it.split(/$splitTag/).each { def alias ->
                    alias = alias.replaceAll(/(?s)\s$/, "").trim()
                    entity.addAlias(alias)
                }
            }
        }
        ScrapeAddress address = new ScrapeAddress()
        address.setProvince("Georgia")
        address.setCountry("UNITED STATES")
        entity.addAddress(address)

        ScrapeEvent event = new ScrapeEvent()

        if (StringUtils.isNoneBlank(eventDate)) {
            eventDate = context.parseDate(new StringSource(eventDate), ["yyyy/MM/dd"] as String[])

            def currentDate = new Date().format("MM/dd/yyyy")
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy")
            def date1 = simpleDateFormat.parse(currentDate)
            def date2 = simpleDateFormat.parse(eventDate)

            if (date2.compareTo(date1) > 0) {
                def remark = eventDate
                eventDate = null
                entity.addRemark(remark)
            } else event.setDate(eventDate)
        }


        event.setDescription("This entity appears on the Georgia Department of Community Health Office of Inspector General List of Excluded Entities.")
        entity.addEvent(event)
    }

    def invoke(url, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def sanitize(data) {
        return data.replaceAll(/&amp;|&amp;amp;/, '&').replaceAll("\\u0450", "e").replaceAll(/\r\n/, "\n").replaceAll(/\s+/, " ").trim()
    }

    def sanitizeName(name) {
        return name.toString().replaceAll(/\u015F/, "s").replaceAll(/\u011F/, "g").replaceAll(/\u0107/, "c").replaceAll(/\u00ef/, "i")
            .replaceAll(/\u015e/, "S").replaceAll(/\u00d6/, "O").replaceAll(/\u00f6/, "o").replaceAll(/\u00fc/, "u")
            .replaceAll(/\u00eb/, "e").trim()

    }

    def sanitizeDate(date) {
        date = date.toString().toString().replace("2050731.0", "20150731")
        date = date.replaceAll(/(?is)(\d)\.(\d+)[a-z]\d/, '$1$2').trim()
        if (date.size() == 7) {
            date = date.replaceAll(/(?<=$date)/, '0')
        }
        date = date.replaceAll(/(\d{4})(\d{2})(\d{2})/, '$1/$2/$3')

        return date

    }

    def sanitizeBusName(def name) {
        name = name.replaceAll("SZB", "")
        name = name.replaceAll(/(?is)\,\s*$/, "")
        name = name.replaceAll(/(?=BIOLAB|BRADLY-SIMMONS|MICHELLE-GRAVES|SELLERS|ROSA LEGER- CEDENO|HEATHER BAILEY|CAROL OR|ESSENCE OF|JOYCE HAYES|GRANT-HARRIS|LANG-THOMAS)/, "AKA ")
        return name
    }

}