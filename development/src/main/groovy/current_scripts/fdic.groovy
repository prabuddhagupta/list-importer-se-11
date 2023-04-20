import com.rdc.importer.scrapian.ScrapianEngine
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils

context.setup([connectionTimeout: 100000, socketTimeout: 200000, retryCount: 5])
context.session.escape = true
context.session.encoding = "UTF-8"
context.session.escapeSpecial = true

def fileLocation = "https://github.com/RegDC/li-internal-data"
def fileName = "fdic"
scrape = new ScrapianEngine()
pathToFolder = scrape.getGitHubURLAsString(fileLocation,fileName,"")//.replace('file:/','file://')
def resultsPage = context.invoke([url: pathToFolder + "/EDOOrders.csv", clean: false])

def rows = csvParser(resultsPage.toString())
rows.each {row ->
    def name = row.respondent
    def city = row.bank_city
    def state = row.bank_state
    def fileUrl = row.file_url
    def description = row.order_title
    def startDate = row.issued_date
    def endDate = row.termination_date

    name = sanitize(name)
    city = sanitize(city)
    state = sanitize(state)
    description = sanitize(description)
    createEntity(name, city, state, fileUrl, description, startDate, endDate)
}

def createEntity(name, city, state, fileUrl, description, startDate, endDate) {
    def entity, type
    if (!name.isEmpty()) {
        def names = name.toString().split(/;/)
        names = names.toList()
        def cities = city.toString().split(/;/)
        cities = cities.toList()
        def states = state.toString().split(/;/)
        states = states.toList()
        def tempCity
        def tempState
        names.eachWithIndex {it, index ->
            def aliases = it.split(/(?i)(?:[fan]\/k\/a\/?|[ad][\.\/][kb][\.\/]a|f\/ka|\baka\b|Formerly|NOW KNOWN AS)|\(nka/)
            def entityName = aliases[0].replaceAll(/\($|\,$/, "")
            entityName = sanitizeName(entityName)
            type = detectEntityType(entityName)
            entity = context.findEntity(["name": entityName])
            if (!entity) {
                entity = context.getSession().newEntity()
                if (entityName) {
                    entity.setName(entityName)
                    entity.setType(type)
                }
            }
            for (int i = 1; i < aliases.length; i++) {
                def alias = StringUtils.removeEnd(aliases[i].toString().trim(), ",")
                alias = alias.replaceAll(/\,$|\:$|\/$/, "")
                entity.addAlias(alias)
            }
            ScrapeAddress scrapeAddress = new ScrapeAddress()

            if (cities[index]) {
                tempCity = cities[index]
                scrapeAddress.setCity(cities[index])
            } else {
                scrapeAddress.setCity(tempCity)
            }
            if (states[index]) {
                tempState = states[index]
                scrapeAddress.setProvince(states[index])
            } else {
                scrapeAddress.setProvince(tempState)
            }
            scrapeAddress.setCountry("USA")
            entity.addAddress(scrapeAddress)
            ScrapeEvent event = new ScrapeEvent()
            if (description && !description.isEmpty()) {
                event.setDescription("This entity appears on the FDIC list of Enforcements. " + description)
            }
            if (startDate) {
                def sDate = context.parseDate(new StringSource(startDate), ["yyyy-MM-dd"] as String[])
                event.setDate(sDate)
            }
            if (endDate) {
                def eDate = context.parseDate(new StringSource(endDate), ["yyyy-MM-dd"] as String[])
                event.setEndDate(eDate)
            }
            if (event.getDescription() != null || event.getDate() != null || event.getEndDate() != null) {
                entity.addEvent(event)
            }
            if (fileUrl) {
                entity.addUrl(fileUrl)
            }
        }
    }

}

def csvParser(String text) {
    text = text.replaceAll(/""/, "\" \"")
    text = text.replaceAll("^\"[^\n]+", "")
    text = text.replaceAll(/("[^"\n]+")/, {a, b -> return a.replaceAll(/,/, '~')}).replaceAll(/"/, '')

    def results = []

    def text1 = text =~ /(?s)^\s*([^\n]+)(.*)/
    def colNames = text1[0][1].split(/\s*,\s*/).collect({
        return it.trim().toLowerCase().replaceAll("/|\\s+", "_")
    })

    def text2 = text1[0][2] =~ /([^\n]+)/
    while (text2.find()) {
        def row = [:]
        def lineArr = text2.group(1).trim().split(/\s*,\s*/, -1)
        (0..<colNames.size()).each() {
            row[colNames[it]] = lineArr[it]
        }
        results.add(row)
    }

    return results
}

def sanitize(data){
    return data.toString().replaceAll(/(?i)Redacted;|N\/A;|&nbsp;/, "").replaceAll(/&amp;nbsp/, "&").replaceAll(/&amp;/, "&").replaceAll(/sect;|\uFFFD/, "").replaceAll(/~/,",")
}

def sanitizeName(data){
    return data.toString().replaceAll(/(?i)(?:merged|successor|cert no).*?$/,"").replaceAll(/(?is)as an.*?$/,"").replaceAll(/(?is)(?<=(?:company|\bllc|limited|\binc\b|bank \()).*?$/,"")
        .replaceAll(/(?is), NATIONAL ASSOCIATION|, n\.a\..*?$|, the.*?$/,"").replaceAll(/(?is), (?:merged|successor).*?$/,"")
        .replaceAll(/(?i)ESTATE OF/,"").replaceAll(/(?s),\s*$|\($/,"").trim()
}

def detectEntityType(name) {
    def type = context.determineEntityType(name)
    if (type.equals("P")) {
        if (name =~ /(?i)bank\b|\b(?:L\.?L\.?C|\busa\b|FLOY FLOYD|INVESTORS|FLOYD MIX|ENGLAND|COMMUNITY|SAVINGS|INTERFINANCIAL|LOAN|AMICUS FSB|AFB&T|FIRSTEXCEL|INATRUST|GREATER|BANCCENTRAL|HOUSTON|Institution|BANKS?|BANKHAVEN|BANKORION|BANKWEST|BANKEAST|BANKCHEROKEE|Countybank|\s+&\s+)\b/) {
            type = "O"
        }
    }
    return type
}