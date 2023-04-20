package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import com.rdc.scrape.ScrapeIdentification
import org.apache.commons.lang.StringUtils

context.setup([connectionTimeout: 50000, socketTimeout: 50000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_In_Fssa_Medicaid_Exclusions script = new Us_In_Fssa_Medicaid_Exclusions(context)
script.initParsing()

def class Us_In_Fssa_Medicaid_Exclusions {

    final addressParser
    final entityType
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final ScrapianContext context
    final String mainUrl = "https://www.in.gov/fssa/ompp/files/OMPP_Terminations.xlsx"

    Us_In_Fssa_Medicaid_Exclusions(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        this.context = context
    }

    def initParsing() {
        def spreadsheet = context.invokeBinary([url: mainUrl, clean: false])
        handleCsvData(spreadsheet)
    }

    def handleCsvData(def spreadsheet) {

        def xml = context.transformSpreadSheet(spreadsheet, [validate: false, escape: true, headers: ['Provider_Name', 'NPI', 'Service_Location_Address', 'Termination_Effective_Date']])
        def rows = new XmlSlurper().parseText(xml.toString());

        for (int i = 2; i < rows.row.size(); i++) {
            def row = rows.row[i]
            handleRowData(row)
        }

    }

    def handleRowData(def row) {

        def name = row.Provider_Name.text().trim()
        def identification = row.NPI.text().trim()
        def identificationList = identification.toString().split(/\/(?=[0-9])/).collect { it.trim() }

        def address = row.Service_Location_Address.text().trim()
        def addressList = address.toString().split(/(?<=[A-Z])(?:\s|\Z)/).collect { it.trim() }

        def eventDate = row.Termination_Effective_Date.text().trim()

        createEntity(name, identificationList, addressList, eventDate)
    }

    def nameSanitize(def name) {

        name = name.replaceAll(/(,\s?)MD|M\.D\.|PA|DC|DO|DDS|PhD|APN|APRN/, " ").trim()
        name = name.replaceAll(/,$/, " ").trim()
        name = name.replaceAll(/\s\s\s\w$/, " ").trim()
        name = name.replaceAll(/,\s*$/, " ").trim()

        return name
    }

    def createEntity(def entityName, def identificationList, def addressList, def eventDate) {

        def entity = null
        entityName = nameSanitize(entityName)
        def entityType = detectEntity(entityName)

        entity = context.findEntity("name": entityName, type: entityType)
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(entityName)
            entity.setType(entityType)
        }

        //NPI
        identificationList.each { it ->
            def npi

            if (!it.toString().contains("N/A")) {
                npi = new BigDecimal(it).toPlainString()

                ScrapeIdentification scrapeIdentification = new ScrapeIdentification()
                scrapeIdentification.setValue(npi)
                scrapeIdentification.setType("National Provider Identification")
                entity.addIdentification(scrapeIdentification)
            }
        }

        //Address
        addressList.each { address ->
            address = address.toString().replaceAll(/(.*)$/, ', $1, United States')

            if (StringUtils.isNotBlank(address) || StringUtils.isNotEmpty(address)) {
                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress)
                }
            }
        }

        ScrapeEvent event = new ScrapeEvent()
        event.setDescription("This entity appears on the Indiana Family and Social Services Administration list of terminated providers.")
        //Date
        eventDate = context.parseDate(eventDate)
        event.setDate(eventDate)
        entity.addEvent(event)
    }

    def detectEntity(def name) {
        def type

        if (name =~ /(?i)Inc\.|Personal|Pathology|Management|Pharmacy|Laboratory|Medical|Medic|Laboratories|Accredited|Universal|Home|Limited|Center|store|Health|Services|Company|Ltd|LLC|L\.P\.|INC|insurance|association|corp |corporation/) {
            type = "O"
        } else {
            if (name =~ /David A. Hall|Nightingale Hospice|Centrad/) {
                type = "O"
            } else {
                type = "P"
            }
        }
        return type
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        return street
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }
}