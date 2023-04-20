package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_me_ag_data_breach_notices_archive script = new Us_me_ag_data_breach_notices_archive(context)
script.initParsing()

int i = 1;

def nameIdMap = [:];
for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + i++);
    nameIdMap[entity.getName()] = entity
}

for (entity in context.session.getEntitiesNonCache()) {
    for (association in entity.getAssociations()) {

        otherEntity = nameIdMap[association];
        if (otherEntity && !isAlreadyAssociated(entity, otherEntity)) {
            scrapeEntityAssociation = new ScrapeEntityAssociation(otherEntity.getId());
            scrapeEntityAssociation.setRelationshipType(RelationshipType.ASSOCIATE);
            scrapeEntityAssociation.setHashable(otherEntity.getName(), otherEntity.getType());
            entity.addScrapeEntityAssociation(scrapeEntityAssociation);
        }
    }
    entity.getAssociations().clear();
}

def isAlreadyAssociated(entity, otherEntity) {
    Set associations = otherEntity.getScrapeEntityAssociations();
    boolean isAssos = false;
    if (associations) {
        associations.each { item ->
            if (((ScrapeEntityAssociation) item).getId().equals(entity.getId())) {
                isAssos = true;
            }
        }
    }
    return isAssos;
}

class Us_me_ag_data_breach_notices_archive {

    final addressParser
    final entityType
    ScrapianContext context

    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")

    final def root = 'https://www.maine.gov/ag'
    final def url = 'https://www.maine.gov/ag/consumer/identity_theft/index.shtml'

    def xlsxUrl2
    def xlsxUrl1

    Us_me_ag_data_breach_notices_archive(context) {
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.updateCities([US: ["Lake Buena Vista", "Washington", "Dorval", "Latham", "West Des Moines", "Worcester", "Miami Gardens", "Arlington", "Franklin Park", "St. Petersburg", "Worcester", "Shepherdstown", "Overland Park"]])
        addressParser.updateCities([IL: ["Ra'anana"]])
        addressParser.updateStates([US: ["D.C."]])
        addressParser.updateStates([CA: ["QC"]])
        addressParser.updateStates([IL: ["Israel"]])

        this.context = context
    }

    def initParsing() {
        def html = invokeUrl(url)

        def xlsxUrlMatcher1, xlsxUrlMatcher2

        xlsxUrlMatcher1 = html =~ /<a href="\.\.\/\.\.(\/docs.*?Form_Data\.xlsx)">Maine Data Breach/
        if (xlsxUrlMatcher1.find()) {
            xlsxUrl1 = root + xlsxUrlMatcher1[0][1]
            def spreadsheet = invokeBinary(xlsxUrl1)
            handleCsvData1(spreadsheet)
        }

        xlsxUrlMatcher2 = html =~ /<a href="\.\.\/\.\.(\/docs.*?Spreadsheet\.xlsx)">Maine Data Breach/
        if (xlsxUrlMatcher2.find()) {
            xlsxUrl2 = root + xlsxUrlMatcher2[0][1]
            def spreadsheet = invokeBinary(xlsxUrl2)
            handleCsvData2(spreadsheet)
        }
    }

    def handleCsvData1(def spreadsheet) {


        def xml = context.transformSpreadSheet(spreadsheet, [validate: false, escape: true, headers: ['Completed_Date', 'Entity_Name', 'Street_Address', 'City', 'State', 'Zip_Code', 'Educational', 'Financial_Services', 'Governmental_Entity_in_Maine', 'Other_Governmental_Entity', 'Health_Care', 'Other_Commercial', 'Not-for-Profit', 'POS_Vendor', 'Name', 'Title', 'Firm_name', 'Telephone_Number', 'Email_Address', 'Relationship_to_entity', 'Total_number_of_people_affected', 'Total_number_of_Maine_residents_affected', 'Exceed_thousand_notification', 'Date_Breach_Occurred', 'Date_Breach_Discovered', 'Loss_of_device', 'Internal_system_breach', 'Insider_wrongdoing', 'External_system_breach', 'Inadvertent_disclosure', 'Other', 'If_other_specify', 'Social_Security_Number', 'ID_number', 'Financial_account_number', 'Written', 'Electronic', 'Telephone', 'Substitute_notice', 'Date_of_consumer_notification', 'Attachment', 'List_dates_of_any_previous_breach_notifications', 'theft_protection_services_offered', 'duration', 'provider', 'description_of_service']])

        def rows = new XmlSlurper().parseText(xml.value);

        for (int i = 1; i < rows.row.size(); i++) {

            def row = rows.row[i]
            handleRowData1(row)
        }
    }

    def handleCsvData2(def spreadsheet) {

        def xml = context.transformSpreadSheet(spreadsheet, [validate: false, escape: true, headers: ['Company_Whose_Data_Was_Breached', 'Company_Contact_Information', 'Attorney', 'Date_of_Breach', 'Date_of_Notification', 'Type_of_Information', 'Number_of_Maine_Residents_Affected']])

        def rows = new XmlSlurper().parseText(xml.value);

        for (int i = 2; i < rows.row.size(); i++) {

            def row = rows.row[i]
            handleRowData(row)
        }
    }

    def handleRowData1(def itemT2) {
        def name1, name2, name3, addressT2, street, city, zip, state, dateOfConsumerNotification, dateOfBreachOccurred, dateOfBreachDiscovered
        def entityNameListT2 = [] as List
        def entityNameListT3 = [] as List

        street = itemT2.Street_Address.text()
        city = itemT2.City.text()
        state = itemT2.State.text()
        zip = itemT2.Zip_Code.text()
        addressT2 = sanitizeAddress("$street, $city, $state $zip").trim()

        name1 = sanitizeNameAddress(itemT2.Entity_Name.text()).trim()

        entityNameListT2.add(name1)
        name2 = sanitizeNameAddress(itemT2.Name.text()).trim()
        name2 = fixName2(name2, addressT2)
        entityNameListT2.add(name2)

        name3 = sanitizeNameAddress(itemT2.Firm_name.text()).trim()

        dateOfConsumerNotification = sanitizeDate(itemT2.Date_of_consumer_notification.text())
        dateOfBreachOccurred = sanitizeDate(itemT2.Date_Breach_Occurred.text())
        dateOfBreachDiscovered = sanitizeDate(itemT2.Date_Breach_Discovered.text())

        def dateOfConsumerNotificationList = [] as List
        def dateOfBreachOccurredList = [] as List
        def dateOfBreachDiscoveredList = [] as List

        if (dateOfConsumerNotification) {
            def validDateChecker = dateOfConsumerNotification =~ /^(?:(?:January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|July|Jul|August|Aug|September|Sep|October|Oct|November|Nov|December|Dec) \d{1,2}\s*,\s*\d{4}|\d{1,2}\s*[\/\.\-]\s*\d{1,2}\s*[\/\.\-]\s*\d{2,4}|(?:(?:(?:January|February|March|April|May|June|July|August|September|October|November|December) \d{1,2}\s*,\s*\d{4}|\d{1,2}\s*[\/\.\-]\s*\d{1,2}\s*[\/\.\-]\s*\d{2,4})\s*(?:and|&)\s*(?:(?:January|February|March|April|May|June|July|August|September|October|November|December) \d{1,2}\s*,\s*\d{4}|\d{1,2}\s*[\/\.\-]\s*\d{1,2}\s*[\/\.\-]\s*\d{2,4})))$/
            if (!validDateChecker.find()) {

                dateOfBreachOccurredList.add(dateOfConsumerNotification)
            } else {

                dateOfConsumerNotificationList.add(separateDate(dateOfConsumerNotification))
                dateOfConsumerNotificationList = separateDate(dateOfConsumerNotification)
            }
        }
        if (!dateOfBreachOccurred.isEmpty()) {
            dateOfBreachOccurredList.add(dateOfBreachOccurred)
        }
        if (!dateOfBreachDiscovered.isEmpty()) {
            dateOfBreachDiscoveredList.add(dateOfBreachDiscovered)
        }

        entityNameListT2 = separateName(name1)

        entityNameListT2.each({ entityName ->
            def aliasT2List = [] as List
            if (!entityName.toString().isEmpty()) {
                (entityName, aliasT2List) = separateAlias(entityName)

                def name2List = []
                name2List = separateName(name2)
                name2List.each { name ->
                    def name2AliasList = [] as List
                    if (!name.toString().isEmpty()) {
                        (name, name2AliasList) = separateAlias(name)
                        aliasT2List.add(name)
                        name2AliasList.each { alias ->
                            if (!alias.isEmpty()) {
                                aliasT2List.add(alias)
                            }
                        }
                    }
                }

                def name3List = []
                name3List = separateName(name3)
                name3List.each { name ->
                    def name3AliasList = [] as List
                    if (!name.toString().isEmpty()) {
                        (name, name3AliasList) = separateAlias(name)
                        aliasT2List.add(name)
                        name3AliasList.each { alias ->
                            if (!alias.isEmpty()) {
                                aliasT2List.add(alias)
                            }
                        }
                    }
                }
                entityName = entityName.toString().replaceAll(/\s+/, " ").trim()
                entityName = entityName.replaceAll(/(?i)^Dr\.?\s(?!DeLuca)|(?:Jr\.?|Sr\.?|Esq\.?)$/, "").trim()
                entityName = entityName.replaceAll(/Esq\.?/, "").trim()
                entityName = sanitizeName(entityName)

                createEntity2(entityName, aliasT2List, addressT2, dateOfConsumerNotificationList, dateOfBreachOccurredList, dateOfBreachDiscoveredList, xlsxUrl1)
            }
        })
        entityNameListT3 = separateName(name2)

        entityNameListT3.each({ entityName ->
            def aliasT3List = [] as List
            if (!entityName.toString().isEmpty()) {

                (entityName, aliasT3List) = separateAlias(entityName)

                def name3List = []
                name3List = separateName(name3)
                name3List.each { name ->
                    def name3AliasList = [] as List
                    if (!name.toString().isEmpty()) {
                        (name, name3AliasList) = separateAlias(name)
                        aliasT3List.add(name)
                        name3AliasList.each { alias ->
                            if (!alias.isEmpty()) {
                                aliasT3List.add(alias)
                            }
                        }
                    }
                }
                entityName = entityName.toString().replaceAll(/\s+/, " ").trim()
                entityName = entityName.replaceAll(/(?i)^Dr\.?\s|(?:Jr\.?|Sr\.?|Esq\.?)$/, "").trim()
                entityName = entityName.replaceAll(/Esq\.?/, "").trim()
                entityName = sanitizeName(entityName)

                createEntity2(entityName, aliasT3List, addressT2, dateOfConsumerNotificationList, dateOfBreachOccurredList, dateOfBreachDiscoveredList, xlsxUrl1)
            }
        })
    }

    def separateAlias(def entityName) {
        def alias, aliasMatcher
        def aliasList = [] as List
        if (entityName =~ /f\/k\/a|k\/n\/a|\(d\/b\/a|d\/b\/a|a\/k\/a[\/]?|(?i)n\/k\/a[\/]?|dba|d\.b\.a\.|\/(?=[A-Z][a-z]{2,})|alias|D\/B\/A|[\(\s]aka\s|DBA|\snka\s|f\/lc\/a/) {

            aliasList = entityName.split(/f\/k\/a|k\/n\/a|\(d\/b\/a|d\/b\/a|a\/k\/a[\/]?|(?i)n\/k\/a[\/]?|\sdba\s|d\.b\.a\.|\/(?=[A-Z][a-z]{2,})|alias|D\/B\/A|[\(\s]aka\s|DBA|\snka\s|f\/lc\/a/).collect({
                it.toString().trim()
            })
            entityName = aliasList[0]
            aliasList.remove(0)


        } else if ((aliasMatcher = entityName =~ /([\"\(].*[\)\"])/)) {
            alias = aliasMatcher[0][1]
            alias = alias.toString().replaceAll(/[\(\)\"]/, "")

            aliasList.add(alias)
            entityName = entityName.toString().replaceAll(/[\"\(]$alias[\"\)]/, "")
            entityName = entityName.toString().replaceAll(alias, "")
        }
        return [entityName, aliasList]
    }

    def handleRowData(def item) {
        def name, address
        def aliasMatcher, addressMatcher
        def nameAddress = sanitizeNameAddress(item.Company_Whose_Data_Was_Breached.text()).toString().trim()

        if ((addressMatcher = nameAddress =~ /,\s+((?:R\. Prof\. Fernando da Fonseca|Office of the VP & General Counsel|Office of the General Counsel|Les Collines de|One Baylor Plaza|Sunnyvale|Bell Gardens|One Casino Drive|One Atlantic Street|Miami Gardens|One Caesars Palace Drive|Hannah Administration Building|One State Farm Plaza|Long Beach|One Dave Thomas|One Campus Drive|Office of Audit|One Chestnut Place|One Campus Road|One Marcus Square|Buffalo Grove|Tabor Center|Upper Main Street|Brunswick Police Dept|Maine Mall).*)/)) {

            address = sanitizeAddress(addressMatcher.group(1))
            name = nameAddress.replaceAll(/,\s+((?:R\. Prof\. Fernando da Fonseca|Office of the VP & General Counsel|Office of the General Counsel|Les Collines de|One Baylor Plaza|Sunnyvale|Bell Gardens|One Casino Drive|One Atlantic Street|Miami Gardens|One Caesars Palace Drive|Hannah Administration Building|One State Farm Plaza|Long Beach|One Dave Thomas|One Campus Drive|Office of Audit|One Chestnut Place|One Campus Road|One Marcus Square|Buffalo Grove|Tabor Center|Upper Main Street|Brunswick Police Dept|Maine Mall).*)/, "").toString().trim()
        } else if ((addressMatcher = nameAddress =~ /(?<=(?:\sLLC|\sLtd\.?|\sInc\.?|\sCorp\.?|\sIncorporated|\.com|\sCo\.?))(?:,|;|\sand\s)\s+(?!=dba)(.*)/)) {

            address = sanitizeAddress(addressMatcher.group(1))
            name = nameAddress.replaceAll(/(?i)(?<=(?:\sLLC|\sLtd\.?|\sInc\.?|\sCorp\.?|\sIncorporated|\.com|\sCo\.?))(?:,|;|\sand\s)\s+(?!=dba)(.*)/, "").toString().trim()
        } else if ((addressMatcher = nameAddress =~ /(,\s+(?:(?:ZIP|B6LPA,|P\.?O\.? Box|Munkedamsveien) )?\d+.*)/)) {

            address = sanitizeAddress(addressMatcher.group(1))
            name = nameAddress.replaceAll(/,\s+(?:(?:ZIP|B6LPA,|P\.?O\.? Box|Munkedamsveien) )?\d+.*/, "").toString().trim()
        } else if ((addressMatcher = nameAddress =~ /,\s+((?:Twin Falls, Idaho|Silver Spring|One Rotary Center|Rockville, MD|Wadsworth, TX 77483|Austin, TX|NY, NY|Atlanta, Georgia|Chicago, Illinois|NV 89109|Minneapolis, MN|Raleigh, NC 27695|Gibbsboro, NJ|Illinois|Boca Raton, Florida|Michigan|Las Vegas, Nevada|Auburn, AL 36849|University Park.*))/)) {

            address = sanitizeAddress(addressMatcher.group(1))
            name = nameAddress.replaceAll(/,\s+(?:Twin Falls, Idaho|Silver Spring|One Rotary Center|Rockville, MD|Wadsworth, TX 77483|Austin, TX|NY, NY|Atlanta, Georgia|Chicago, Illinois|NV 89109|Minneapolis, MN|Raleigh, NC 27695|Gibbsboro, NJ|Illinois|Boca Raton, Florida|Michigan|Las Vegas, Nevada|Auburn, AL 36849|University Park.*)/, "").toString().trim()
        } else {

            name = nameAddress
            address = "Maine"
        }

        def temp = address =~ /,?((?:d\/b\/a|dba|n\/k\/a|nka|a\/k\/a|aka|f\/k\/a|fka).*?),\s*(.*)/
        if (temp.find()) {
            name += temp.group(1).trim()
            address = temp.group(2).trim()
        }

        if (address.toString().contains("dba")) {
            def alias = address =~ /(?ism)(.*?),(.*)/
            if (alias.find()) {
                name += alias.group(1).trim()
                address = alias.group(2).trim()
            }
        } else if (address.toString().contains("Mid-American") || address.toString().contains("Accu-Pay Payroll")) {
            def alias = address =~ /(?ism)(.*?),(.*)/
            if (alias.find()) {
                name += ", " + alias.group(1).trim()
                address = alias.group(2).trim()
            }
        }
        name = sanitizeName(name)


        def dateOfNotificationList = [] as List
        def dateOfBreachList = [] as List

        def dateOfNotification = sanitizeDate(item.Date_of_Notification.text())
        def dateOfBreach = sanitizeDate(item.Date_of_Breach.text())
        def typeOfInformation = sanitizeDate(item.Type_of_Information.text())

        dateOfNotificationList = separateDate(dateOfNotification)

        dateOfBreachList = separateDate(dateOfBreach)

        def entityNameList = [] as List
        entityNameList = separateName(name)

        entityNameList.each({ entityName ->
            def alias
            def aliasList = [] as List
            if (!entityName.toString().isEmpty()) {
                if (entityName =~ /f\/k\/a|k\/n\/a|\(d\/b\/a|d\/b\/a|a\/k\/a[\/]?|(?i)n\/k\/a[\/]?|dba|d\.b\.a\.|\/(?=[A-Z][a-z]{2,})|alias|D\/B\/A|[\(\s]aka\s|DBA|\snka\s|f\/lc\/a/) {

                    aliasList = entityName.split(/f\/k\/a|k\/n\/a|\(d\/b\/a|d\/b\/a|a\/k\/a[\/]?|(?i)n\/k\/a[\/]?|\sdba\s|d\.b\.a\.|\/(?=[A-Z][a-z]{2,})|alias|D\/B\/A|[\(\s]aka\s|DBA|\snka\s|f\/lc\/a/).collect({
                        it.toString().trim()
                    })
                    entityName = aliasList[0]
                    aliasList.remove(0)
                } else if ((aliasMatcher = entityName =~ /([\"\(].*[\)\"])/)) {
                    alias = aliasMatcher[0][1]

                    aliasList.add(alias)
                    entityName = entityName.toString().replaceAll(/[\"\(]$alias[\"\)]/, "")
                    entityName = entityName.toString().replaceAll(alias, "")
                }
                entityName = entityName.toString().replaceAll(/\s+/, " ").trim()
                entityName = entityName.replaceAll(/(?i)^Dr\.?\s|(?:Jr\.?|Sr\.?)$/, "").trim()
                entityName = sanitizeName(entityName)

                createEntity(entityName, aliasList, address, dateOfNotificationList, dateOfBreachList, typeOfInformation, xlsxUrl2)
            }
        })
    }

    def createEntity2(def name, def aliasList, def address, def dateOfConsumerNotificationList, def dateOfBreachOccurredList, def dateOfBreachDiscoveredList, def xlsxUrl2) {
        def entity = null

        if (!name.toString().equals("") || !name.toString().isEmpty()) {
            name = sanitizeEntityName(name)
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }

            def aliasEntityType
            aliasList.each {
                it = sanitizeAlias(it)

                if (it) {
                    aliasEntityType = detectEntity(it)
                    if (aliasEntityType.toString().equals(entityType)) {

                        entity.addAlias(it)
                    } else {
                        if (aliasEntityType.equals("P")) {
                            entity.addAssociation(it)
                        } else {

                            entity.addAssociation(it)
                        }
                        //create new entity with association
                        def newEntity = context.findEntity(["name": it, "type": aliasEntityType])
                        if (!newEntity) {
                            newEntity = context.getSession().newEntity()
                            newEntity.setName(it)
                            newEntity.setType(aliasEntityType)
                        }
                        newEntity.addAssociation(name)
                        addCommonPartOfEntity2(newEntity, address, dateOfConsumerNotificationList, dateOfBreachOccurredList, dateOfBreachDiscoveredList, xlsxUrl2)
                    }
                }
            }
            addCommonPartOfEntity2(entity, address, dateOfConsumerNotificationList, dateOfBreachOccurredList, dateOfBreachDiscoveredList, xlsxUrl2)
        }
    }

    def addCommonPartOfEntity2(def entity, def address, def eventDateList, def remark1List, def remark2List, def entityUrl) {

        def description = 'This entity appears on the Maine Attorney General published list of data breach notices.'
        //Add Address
        ScrapeAddress scrapeAddress = new ScrapeAddress()
        if (address.toString() != "null" || address != "") {
            address = address.toString().replaceAll(/(?s)\s+/, ' ').trim()

            def addrMap = addressParser.parseAddress([text: address, force_country: true])
            scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
            if (scrapeAddress) {
                entity.addAddress(scrapeAddress)
            }
        } else {
            scrapeAddress.setCountry("Maine")
        }
        //Add Event
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(description)
        eventDateList.each { eventDate ->
            if (eventDate) {
                eventDate = context.parseDate(new StringSource(eventDate), ["dd.MM.yyyy", "dd.M.yyyy", "d.MM.yyyy", "MMM d, yyyy", "M/d/yy", "M/dd/yy", "MM/d/yy", "MM/dd/yy", "dd-MMM-yy", "dd-MM-yyyy", "MMM-yy", "MMMM yyyy", "MM/yy", "dd/MM/yyyy"] as String[])
                event.setDate(eventDate)
            }
        }
        //Add Remark
        remark1List.each { remark ->
            if (remark) {
                remark = remark.toString().replaceAll(/(?s)\s+/, ' ').trim()
                entity.addRemark("Date of Breach Occurred: $remark")
            }
        }
        remark2List.each { remark ->
            remark = remark.toString().replaceAll(/(?s)\s+/, ' ').trim()
            if (remark) {
                entity.addRemark("Date of Breach Discovered: $remark")
            }
        }
        //Add URL
        entity.addEvent(event)
        if (entityUrl) {
            entity.addUrl(entityUrl)
        }
    }

    def createEntity(def name, def aliasList, def address, def dateOfNotification, def dateOfBreach, def typeOfInformation, def xlsxUrl2) {
        def entity = null

        if (!name.toString().equals("") || !name.toString().isEmpty()) {
            name = sanitizeEntityName(name)
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }

            def aliasEntityType
            aliasList.each {
                it = sanitizeAlias(it)

                if (it) {
                    aliasEntityType = detectEntity(it)
                    if (aliasEntityType.toString().equals(entityType)) {
                        entity.addAlias(it)
                    } else {
                        if (aliasEntityType.equals("P")) {
                            entity.addAssociation(it)
                        } else {

                            entity.addAssociation(it)
                        }
                        //create new entity with association
                        def newEntity = context.findEntity(["name": it, "type": aliasEntityType])
                        if (!newEntity) {
                            newEntity = context.getSession().newEntity()
                            newEntity.setName(it)
                            newEntity.setType(aliasEntityType)
                        }
                        newEntity.addAssociation(name)
                        addCommonPartOfEntity(newEntity, address, dateOfNotification, dateOfBreach, typeOfInformation, xlsxUrl2)
                    }
                }
            }
            addCommonPartOfEntity(entity, address, dateOfNotification, dateOfBreach, typeOfInformation, xlsxUrl2)
        }
    }

    def addCommonPartOfEntity(def entity, def address, def eventDate, def remark1, def remark2, def entityUrl) {

        def description = 'This entity appears on the Maine Attorney General published list of data breach notices.'
        //Add Address
        ScrapeAddress scrapeAddress = new ScrapeAddress()
        if (address.toString() != "null" || address != "") {

            address = address.toString().replaceAll(/(?s)\s+/, ' ').trim()
            def addrMap = addressParser.parseAddress([text: address, force_country: true])
            scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
            if (scrapeAddress) {
                entity.addAddress(scrapeAddress)
            }
        } else {
            scrapeAddress.setCountry("Maine")
        }
        //Add Event
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(description)
        if (eventDate) {
            eventDate.each { date ->
                date = context.parseDate(new StringSource(date), ["dd.MM.yyyy", "dd.M.yyyy", "d.MM.yyyy", "MMM d, yyyy", "M/d/yy", "M/dd/yy", "MM/d/yy", "MM/dd/yy", "dd-MMM-yy", "dd-MM-yyyy", "MMM-yy", "MMMM yyyy", "MM/yy", "dd/MM/yyyy"] as String[])
                event.setDate(date)
            }
        }
        //Add Remark
        if (remark1 || remark2) {
            remark1.each { remark ->
                remark = remark.toString().replaceAll(/(?s)\s+/, ' ').trim()
                remark2 = remark2.toString().replaceAll(/(?s)\s+/, ' ').trim()
                if (!remark2.isEmpty()) {
                    entity.addRemark("Date of Breach: $remark. Type of Information: $remark2")
                } else {
                    entity.addRemark("Date of Breach: $remark.")
                }
            }
        }
        //Add URL
        entity.addEvent(event)
        if (entityUrl) {
            entity.addUrl(entityUrl)
        }
    }
    def detectEntity(def name) {
        def type
        if (name =~ /^(?:Bruno|Budwal|Calardo|Carapella|Christakos|Fusco|Jean|Kerr|Weiss|Michael|Miller|Moro|Torrillo|Torres|Geake|Lambert|Locy|Pedersen|Quinzi|Whopper|Wilhelm)$/) {
            type = "P"
        } else if (name =~ /^(?:Schifano|Caruso|Daigneau|Hornberger|Johnson|Juarez|Milano|Ohman|Halsne|Parmigiani|Peter|Shea|Stapleton|Stephens|Yasnis|Smutko|Fifer|Steven|Scott)$/) {
            type = "P"
        } else if (name =~ /^(?:Ned|John|Joe|Jimmy|Gullmetti|Buckman|Firm|Le|Dadco|Abernathy)$/) {
            type = "P"
        } else if (name =~ /^(?:Bart Huffman|Bart Walker|Bruce Radke|Ezra D\. Church|Ryan Barker|Tim Diamond)$/) {
            type = "P"
        } else if (name =~ /^\S+$|Course Trends/) {
            type = "O"
        } else if (name =~ /^(?:Beatitudes Campus|Bell Helicopter|Baker Hostetler|Baker McKenzie|Affinity Gaming|Affy Tapple|Akin Gump|Aero Grow|Amazing Grass|Anastazia Beverly Hills|Applebees Clovis|Arent Fox|Art of Tea|Aspex Pos|Atlantic Cigar|Backcountry Gear|Bigfoot Gun Belts|Blackwing Ostrich Meat|Boomerang Tags|Braintree PSP|Bruegger's Bagels|Buca Di Beppo|Cadence Aerospace|Condé Nast|Crown Castle|DTI Title|Diocese of San Diego|Discover Card|Drug Dependence|Drummond Woodsum|Eckert Seamans|Eddie Bauer|Excellus BCBS|Faegre Baker Daniels|Fisher Vineyard|Five Below|Fix My Blinds|Gallagher NAC|Gibbsboro NJ|Goplae San Francisco|Gordon Thomas Honeywell|Greenberg Traurig|Hall of Fame|Husch Blackwell|In Vogue Incorp|J Crew|Kahr Arms|Luggage Pros|Mamacitas San Antonio|marty's newton|McDonald Hopkins|Milwaukee Bucks|Morgan Stanley|Morrison Rothman|Neiman Marcus|Neue Galerie|Red Roof Inns|Rural King|Sabre FOS|Saratoga Sweets|Seeds of Peace|Sheridan Healthcorp|Single Digits|Sloppy Joes|Spring Hill|Squire Patton Boggs|Tischer BMW Porsche|Tim Hortons|Tio Networks|Urology Austin|Wells Fargo|White Lodging|Whiting Turner|Wideners Reloading|Willie T's|Wilson Elser|Wizard Labs|Wynden Stark|Yummie by Heather Thomson|Zehnders of Frankenmuth|barbeque renew|id experts|Warby Parker|Mrs\. Fields Gifts|Finish Line)$/) {
            type = "O"
        } else if (name =~ /(?:Bank|College|Lawn NJ|Club|Southern Idaho|Holy Cross|Street|Park 'N Fly|Pearl Izumi|Plant Therapy|Polsinelli PC|RR Donnelley|End Button Champ|Santoro CPA|Sonic Drive-In|Croix Hospice|Physicians PC|Mount Kisco|Price PC|Street Greetings|Wild Beauty|Wharton Farm|nbkc bank|Scratch Kitchen|Farm to Feet|Blue Shield of NJ|Lifetime Care)$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$|^Citigroup|Prison$|Plantation|Works$|Carpet$|Guo Ji$|Guangming$|Shoes$|CLUBS$|Brothers$|Payroll$|Clinic$|Vacations$|website$|Photo$|BlueShield$|Vegas$|FCU$|Anywhere$|Fundraising$|Only$|Firearms&|Coffee|Café$|Credco$|Bbq$|Burritos|Tuning$|Prints$|Armory$|Arms$|Hotels|Ecommerce|Trees$|Maine|Coral$|Casino|French|Eats|Gadget|Box|Cream|Western|Bangor|Warehouse|Digital|Pilgrim|Brewery|Wiehe|Sports|Online|Whistle|Bookman$|Flask$|ICSC|Grill$|Indigo|lore$|Fellowship|Scientific|Schools?|Web$|Suspension|Provence|Lewis Brisbois Bisgaard and Smith|Instruments|Concessions|Glockstore|Magnum Boots|Certifications|Makeup Geek|Malley's Chocolates|Stores|Automotive|Nursingwear|Payments|NorthStar Anesthesia|Northeast Arc|Rheumatology|Fulbright|Farms$|Credentialing|Bistro$|Buffet$|Clinic|Winery|Pharmacy|Plymouth Harbor on Sarasota Bay|Living|Premera Blue Cross|Prescriptions|Bar|Diagnostics|TOURS|Fundraising|Reliable Respiratory|Zoo$|Steakhouse|Shoemakers|Schurman Fine Papers|Shepherdstown|Cosmetics|Smartphone|Southern Tide|Catalogs|Goodsync$|Footwear|Tieks by Gavrieli|Times Three Clothier|Distributors|Title Nine|Promotional|Township|Traci England-Nelson|Troutman Pepper|Viator Tours and attractions|Virginia Gift Brands|Vistaprint USD|Volcano eCigs)/) {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:Kahr)/) {
                type = "P"
            }
        }
        return type
    }
    def sanitizeNameAddress(def text) {
        text = text.toString().trim()
        text = text.replaceAll(/(?s)\s+/, " ")
        text = text.replaceAll(/\s*,\s*(?=L\.L\.P|L\.L\.C\.?|Lda\.|P\.A\.|P\.C\.|Inc\.?|Co\.?|LLC|LLLP|L\.P\.|JR\.?|SR\.?|N\.A\.|S\.C\.|LLP|ET|PLLC|PSC|LTD|Ltd|PA|PC|PLC|NV|NJ|a\/k\/a|AKA|LP|Plc|s\.c\.|III|II|Incorporated|INc\.|inc\.|n\/k\/a|f\/k\/a|o\/b\/o|CFP|CPA|S\.A\.|d\/b\/a|D\/B\/A|d\.b\.a\.|k\/n\/a|Esq\.?|and|&|WV)/, " ")
        //CSV 1
        text = text.replaceAll(/\(Payment Ingegrator \(paper\)/, "")
        text = text.replaceAll(/\(paper\)/, "")
        text = text.replaceAll(/\(email\)/, "")
        text = text.replaceAll(/\(email (?:&|and) paper\)/, "")
        text = text.replaceAll(/\(paper & email\)/, "")
        text = text.replaceAll(/\(tax preparation company\)/, "")
        text = text.replaceAll(/UPDATE|MD, |affiliate of |, M\.D\./, "")
        text = text.replaceAll(/(?i)\(Update\)/, "")
        text = text.replaceAll(/3rd party service provider /, "")
        text = text.replaceAll(/; \(800\) 953-5499/, "")
        text = text.replaceAll(/, 800-299-7812/, "")
        text = text.replaceAll(/; 800-255-2755/, "")
        text = text.replaceAll(/\(\:\)/, '')
        text = text.replaceAll(/, the company behind (French Tip Dip)/, 'dba $1')
        text = text.replaceAll(/and its affiliates, collectively |collectively /, "")
        text = text.replaceAll(/; inquiries@eyebuydirect\.com/, '')
        text = text.replaceAll(/Weil, Akman, Baylin & Coleman P\.A\./, "Weil Akman Baylin & Coleman P.A.")
        text = text.replaceAll(/The Lifetime Healthcare Companies, including its.*?(, 165 Court.*)/, 'The Lifetime Healthcare Companies dba Excellus BCBS dba Lifetime Health Medical Group dba Lifetime Benefit Solutions dba Lifetime Care dba The MedAmerica Companies dba Univera Healthcare $1')
        text = text.replaceAll(/Advisory Research180 North Stentson Avenue/, "Advisory Research, 180 North Stentson Avenue")
        text = text.replaceAll(/ADP \(Automatic Data Processing/, "ADP (Automatic Data Processing)")
        text = text.replaceAll(/(SallieMae), (300 Continental Drive, Newark, DE 19713) \((SmartyPig LLC d\/b\/a Q2 Labs)\)/, '$1 d/b/a $3, $2')
        text = text.replaceAll(/(Tio Networks)(,.*?)\(re: .*/, '$1 d/b/a Alliance Data d/b/a Florida Power & Light Company $2')
        text = text.replaceAll(/RRTS \(Roadrunner Transportation Systems Inc\./, 'Roadrunner Transportation Systems Inc. dba RRTS')
        text = text.replaceAll(/\(Amedysis and MACTEC\)/, 'Amedysis dba MACTEC')
        text = text.replaceAll(/\(Office of the VP & General Counsel\)/, ', Office of the VP & General Counsel,')
        text = text.replaceAll(/(Dane Street LLC)(, 3815.*?MA 02130).*/, '$1 dba Harvard Pilgrim $2')
        text = text.replaceAll(/The City o Bozeman, Montana(, 121 N.*?MT 59715)/, 'The City of Bozeman$1')
        text = text.replaceAll(/The Information and Referral Federation/, 'The Information AND Referral Federation')
        text = text.replaceAll(/DiCicco, Gulman/, 'DiCicco Gulman')
        text = text.replaceAll(/\(L\'Occitane\)/, 'dba L\'Occitane')
        text = text.replaceAll(/(Blackhawk Consulting Group) \(Provides.*/, '$1 dba Advocate Medical Group dba Dreyer Medical Clinic')
        text = text.replaceAll(/(Fidelity Investments), (William.*?Duserick).*?\d+.*?Oracle Corporation\)/, '$1 dba $2 dba Oracle Corporation')
        text = text.replaceAll(/Quayside Publishing Group \(Qbookshop\.com, Qbookshop\.net, Motorbooks\.com and WalterFoster\.com\).*/, 'Quayside Publishing Group dba Qbookshop.com dba Qbookshop.net dba Motorbooks.com dba WalterFoster.com').trim()
        text = text.replaceAll(/\(\"Primedia\"\)/, ' dba Primedia')
        text = text.replaceAll(/, Management Co\. for (Aansazi.*)/, ' dba $1')
        text = text.replaceAll(/Altria Group's payroll vendor/, 'Altria Group')
        text = text.replaceAll(/Citi, Citigroup/, 'Citi Citigroup')
        text = text.replaceAll(/Anthem \(Connextions.*/, 'Anthem dba Connextions Call Center dba Anthem\'s Medicare business customer service')
        text = text.replaceAll(/(Tim Hortons).*/, '$1 dba Computershare')
        text = text.replaceAll(/RR Donnelley and its customer/, 'RR Donnelley dba')
        text = text.replaceAll(/Inc\. 8000 Hub Parkway/, 'Inc., 8000 Hub Parkway')
        text = text.replaceAll(/A dovosopm pf the Retirement Advantage Inc\./, 'dba Retirement Advantage Inc.')
        text = text.replaceAll(/Friedman, Leavitt/, 'Friedman Leavitt')
        text = text.replaceAll(/\(\"Norcom\"\)/, 'dba Norcom')
        text = text.replaceAll(/TrueNet Communications Inc\. 7666Blanding Blvd/, 'TrueNet Communications Inc., 7666 Blanding Blvd')
        text = text.replaceAll(/(TIO Networks USA Inc\.) on behelf.*/, '$1 dba Bank of America dba PageOnce dba Southern Management Corporation')
        text = text.replaceAll(/(Tio Networks)(.*?)\(with (.*?), (.*?) & (.*?)\)/, '$1 dba $3 dba $4 dba $5, $2')
        text = text.replaceAll(/\(Rockville, MD\)/, ', Rockville, MD')
        text = text.replaceAll(/Tio Networks, Return Mail Processing/, 'Tio Networks')
        text = text.replaceAll(/P\.O\.Box/, 'P.O. Box')
        text = text.replaceAll(/PO Box \S?,/, '')
        text = text.replaceAll(/(Mary Ruth Buchness,) MD, Dermatologist PC;/, '$1')
        text = text.replaceAll(/Hayden, Narey & Persich/, 'Hayden Narey & Persich')
        text = text.replaceAll(/ 2121 47th St\., Moline/, ', 2121 47th St., Moline')
        text = text.replaceAll(/Health and Human Services/, 'Health AND Human Services')
        text = text.replaceAll(/Nose and Throat/, 'Nose AND Throat')
        text = text.replaceAll(/Wilbraham, Lawler & Buba.*/, 'Wilbraham Lawler & Buba')
        text = text.replaceAll(/Inc\.1313 Dolley Madison Blvd/, 'Inc., 1313 Dolley Madison Blvd')
        text = text.replaceAll(/Sixty Hotels,206 Spring Street/, 'Sixty Hotels, 206 Spring Street')
        text = text.replaceAll(/Re: Mrs\. Prindables/, 'Mrs. Prindables')
        text = text.replaceAll(/, and (Accu-Pay Payroll and Bookkeeping Svcs Inc\.)/, ', Accu-Pay Payroll AND Bookkeeping Svcs Inc.')
        text = text.replaceAll(/Rutherford, MacDonald & Olson/, 'Rutherford MacDonald & Olson')
        text = text.replaceAll(/Hotel[s]? and Resort[s]?/, 'Hotels AND Resorts')
        text = text.replaceAll(/Remington, Lodging/, 'Remington Lodging')
        text = text.replaceAll(/Clinic PC 95 Collier Road/, 'Clinic PC, 95 Collier Road')
        text = text.replaceAll(/, Sabre Hospitality Solutions is service provider/, 'dba Sabre Hospitality Solutions')
        text = text.replaceAll(/Health and Wellness LLC/, 'Health AND Wellness LLC')
        text = text.replaceAll(/6701 Nob Hill Road/, ', 6701 Nob Hill Road')
        text = text.replaceAll(/Bechtel Oil, Gas & Chemicals Construction/, 'Bechtel Oil Gas & Chemicals Construction')
        text = text.replaceAll(/- parent company to Woodwick/, 'dba Woodwick')
        text = text.replaceAll(/Vertex Wireless \(Vertex\)/, 'Vertex Wireless dba Vertex')
        text = text.replaceAll(/(?:thru|through) Aptos/, '(Aptos)')
        text = text.replaceAll(/(?:thru|through) ADP/, 'dba ADP')
        text = text.replaceAll(/Bambeco Inc\.621 East/, 'Bambeco Inc., 621 East')
        text = text.replaceAll(/Bambeco Inc\., 621 East Pratt St.*/, 'Bambeco Inc. dba Aptos, 621 East Pratt St., Suite 300, Baltimore, MD 21202')
        text = text.replaceAll(/Hutchinson and Bloodgood/, 'Hutchinson AND Bloodgood')
        text = text.replaceAll(/Kahr Arms, Auto-Ordnance, Magnum research - \(Aptos\)/, 'Kahr Arms dba Aptos, Auto-Ordnance dba Aptos, Magnum research dba Aptos')
        text = text.replaceAll(/Lanier, Westerfield, Deal/, 'Lanier Westerfield Deal')
        text = text.replaceAll(/(Aptos) \(for (Atwood.*?L\.P\.,.*?73703)/, '$1 dba $2')
        text = text.replaceAll(/RTS Title & Escrow & DTI Title & Escrow/, 'RTS Title & Escrow, DTI Title, Escrow')
        text = text.replaceAll(/(Aptos Inc\.)(,.*?) \(3rd.*?((?:Mrs Prindables|Affy Tapple))\)/, '$1 dba $3 $2')
        text = text.replaceAll(/King, McNamara, Moriarty/, 'King McNamara Moriarty')
        text = text.replaceAll(/Bomberg, Roach & Hanson/, 'Bomberg Roach & Hanson')
        text = text.replaceAll(/Scungio and McAllister/, 'Scungio AND McAllister')
        text = text.replaceAll(/Guilmartin, DiPiro & Sokolowski/, 'Guilmartin DiPiro & Sokolowski')
        text = text.replaceAll(/Prime Inc\.,2740 N\. Mayfair/, 'Prime Inc., 2740 N. Mayfair')
        text = text.replaceAll(/\(part of The Wendy's Company\)/, '(The Wendy\'s Company)')
        text = text.replaceAll(/Massachusetts Eye and Ear Infirmary Inc\./, 'Massachusetts Eye AND Ear Infirmary Inc.')
        text = text.replaceAll(/,105 Sanford Street/, ', 105 Sanford Street')
        text = text.replaceAll(/\(dba Taos Footwear\)/, 'dba Taos Footwear')
        text = text.replaceAll(/Kolodzey & Cox CPAs PC;/, 'Kolodzey & Cox CPAs PC,')
        text = text.replaceAll(/\(and Magnum Boots\)/, 'dba Magnum Boots')
        text = text.replaceAll(/,513 Franklin Avenue/, ', 513 Franklin Avenue')
        text = text.replaceAll(/, dba GQR Global Markets\/City Internships/, 'dba GQR Global Markets SLASH City Internships')
        text = text.replaceAll(/Block and Company/, 'Block AND Company')
        text = text.replaceAll(/234 Kinderkamack Road/, ', 234 Kinderkamack Road')
        text = text.replaceAll(/5 Crescent Drive/, ', 5 Crescent Drive')
        text = text.replaceAll(/The Kantar Group, HR Operations/, 'The Kantar Group')
        text = text.replaceAll(/Matrix Service Co\..*?OK 74135/, 'Matrix dba Matrix Service Co. dba Matrix Service dba Matrix PDM Engineering dba Matrix North American Construction Ltd dba Matrix No. American Construction Inc., 5100 E. Skelly Dr., Ste. 700, Tulsa, OK 74135')
        text = text.replaceAll(/Landry's Inc..*?Mississippi/, 'Landry\'s Inc. dba Golden Nugget Atlantic City LLC dba Golden Nuttet Lake Charles LLC dba GNL Corp dba GNLV Corp.dba Riverboat Corporation of Mississippi')
        text = text.replaceAll(/Sheptoff, Reuber & Co\./, 'Sheptoff Reuber & Co.')
        text = text.replaceAll(/id experts.*/, 'id experts dba Kicky Pants, 10300 SW Greenburg Road, Ste 570, Portland')
        text = text.replaceAll(/(id experts.*?)(,\s*10300.*?)\((o\/b\/o Kicky Pants.*)\)/, '$1 $3 $2')
        text = text.replaceAll(/Oak Hill Citgo Gas and Convenience/, 'Oak Hill Citgo Gas AND Convenience')
        text = text.replaceAll(/JB Autosports Inc\../, 'JB Autosports Inc.,')
        text = text.replaceAll(/Acclaim Technical Services Inc\., \("ATS"\)/, 'Acclaim Technical Services Inc. dba ATS')
        text = text.replaceAll(/Catenacci,Markowitz,Delandri, Rosner & Company,/, 'Catenacci Markowitz Delandri Rosner & Company')
        text = text.replaceAll(/Mary Ruth Buchness, Dermatologist PC;/, 'Mary Ruth Buchness,')
        text = text.replaceAll(/Calendar and Novelty Company/, 'Calendar AND Novelty Company')
        text = text.replaceAll(/Wilderness Hotel and Golf Resort/, 'Wilderness Hotel AND Golf Resort')
        text = text.replaceAll(/operator of the ShowTix4U website/, 'ShowTix4U')
        text = text.replaceAll(/,1211 Avenue/, ', 1211 Avenue')
        text = text.replaceAll(/Bed, Bath & Beyond/, 'Bed Bath & Beyond')
        text = text.replaceAll(/Bar and Grill/, 'Bar AND Grill')
        text = text.replaceAll(/Genworth, Data Security Team/, 'Genworth Data Security Team')
        text = text.replaceAll(/(Medical Informatics Engineering), (6302.*?46804) and (.*?) \(a .*?(Medical.*?Engineering)\)/, '$1 and $3 dba $4, $2')
        text = text.replaceAll(/Hershey Entertainment and Resorts Company;/, 'Hershey Entertainment AND Resorts Company,')
        text = text.replaceAll(/FireKeepers Casino Hotel; Battle Creek/, 'FireKeepers Casino Hotel and Battle Creek')
        text = text.replaceAll(/Huneeus Vintners LLC through Missing/, 'Huneeus Vintners LLC dba Missing')
        text = text.replaceAll(/\(Sally Beauty\)/, 'dba Sally Beauty')
        text = text.replaceAll(/Cental States Southeast and Southwest Areas Health and Welfare and Pension Funds/, 'Central States Southeast & Southwest Areas Health & Welfare & Pension Funds')
        text = text.replaceAll(/Law Department,/, '')
        text = text.replaceAll(/through Premera/, 'Premera')
        text = text.replaceAll(/Premera Blue Cross.*?Connexion Ins Solutions.*?WA 98043/, 'Premera Blue Cross dba Academe Inc. dba LifeWise Health Plan of Arizona dba Connexion Ins Solutions dba LifeWise Health Plan of Washington dba LifeWise Health Plan of Oregon dba LifeWise Assurance Co. dba Vivacity Inc., 7001 220th St. SW, MS 355, Mountlake Terrace, WA 98043')
        text = text.replaceAll(/State Medical Association322 Canal/, 'State Medical Association, 322 Canal')
        text = text.replaceAll(/\(sponsored an ERISA group plan administered by Anthem Inc\.\)/, 'dba ERISA dba Anthem Inc.')
        text = text.replaceAll(/C & K Systems \/ Goodwill Industries/, 'C & K Systems/Goodwill Industries')
        text = text.replaceAll(/Assn\. 1615 L St\./, 'Assn., 1615 L St.')
        text = text.replaceAll(/Viator Tours and attractions/, 'Viator Tours AND attractions')
        text = text.replaceAll(/ Value Pet Supplies; 167 Industrial Park/, ' Value Pet Supplies, 167 Industrial Park')
        text = text.replaceAll(/\(d\/b\/a Power Plant Services; 3131 W\. Soffel Ave/, 'd/b/a Power Plant Services, 3131 W. Soffel Av')
        text = text.replaceAll(/E-Conolight LLC1501 96th Street/, 'E-Conolight LLC, 1501 96th Street')
        text = text.replaceAll(/American Residuals and Talent/, 'American Residuals AND Talent')
        text = text.replaceAll(/\(operator of Breyer Horses website\)/, 'dba Breyer Horses website')
        text = text.replaceAll(/Advanced Data Processing Inc\. and its clients.*?Dept\./, 'Advanced Data Processing Inc. dba City of Alexandria Virginia dba City of Alexandria Fire Dept.')
        text = text.replaceAll(/AB Acquisition LLC, parent company of New.*/, 'AB Acquisition LLC dba New Albertson\'s Inc.')
        text = text.replaceAll(/Dworken, Hillman, LaMorte & Sterczala/, 'Dworken Hillman LaMorte & Sterczala')
        text = text.replaceAll(/California State University, East Bay/, 'California State University East Bay')
        text = text.replaceAll(/\(Lasko\)/, 'dba Lasko')
        text = text.replaceAll(/Sterne, Agee & Leach/, 'Sterne Agee & Leach')
        text = text.replaceAll(/Montana Dept\. of Public Health & Human Services; www\.dphhs\.mt\.gov/, 'Montana Dept. of Public Health & Human Services dba www.dphhs.mt.gov')
        text = text.replaceAll(/Michael's Stores Inc\..*?1\/27\/14 notice/, 'Michael\'s Stores Inc. dba Aaron Brothers')
        text = text.replaceAll(/Guys American Kitchen and Bar/, 'Guys American Kitchen AND Bars')
        text = text.replaceAll(/Wolf and Co\./, 'Wolf AND Co.')
        text = text.replaceAll(/McKenna, Long & Aldridge/, 'McKenna Long & Aldridge')
        text = text.replaceAll(/\(Update dated 3\/14\/14 bringing # of affected Maine.*/, '')
        text = text.replaceAll(/(Mannix Marketing) \(maintains data for (.*?), (.*?), (.*?) and (.*? Planet)/, '$1 dba $2 dba $3 dba $3 dba $4 dba $5')
        text = text.replaceAll(/\(Background checks for Marsh& McLennan Companies/, 'dba Marsh & McLennan Companies')
        text = text.replaceAll(/The Scotts Company LLC \/ Datapak/, 'The Scotts Company LLC/Datapak')
        text = text.replaceAll(/; Irving, TX 75039/, ', Irving, TX 75039')
        text = text.replaceAll(/\(State securities regulators as part of the North American Securities Admin's Assn\.\)/, '')
        text = text.replaceAll(/\(do the POS system for The Common Market\)/, '').trim()
        text = text.replaceAll(/\(Notice of Possible Breach\)/, '').trim()
        text = text.replaceAll(/(Crescent Mortgage Company).*/, '$1, 6600 Peachtree Dunwoody RD, 600 Embassy Row, Suite 650, Atlanta, GA 30328').trim()
        text = text.replaceAll(/on behalf of Entities listed in Exhibit 1.*/, '').trim()
        text = text.replaceAll(/Trump Hotel Collection \(Trump International Hotel & Tower Las Vegas/, 'Trump Hotel Collection dba Trump International Hotel & Tower Las Vegas').trim()
        text = text.replaceAll(/(?:on behalf of its clients|, via its vendor|o\/b\/o|, a wholly owned subsidiary of|, c\/o|c\/o|, on behalf of|operator of |t\/d\/b\/a|through its website|and their affiliates)/, 'dba')
        text = text.replaceAll(/a subsidiary of/, "dba")
        text = text.replaceAll(/division of |subsidiary of /, '')
        text = text.replaceAll(/(?:dba)/, 'n/k/a')
        //CSV 2
        text = text.replaceAll(/(?:collectively "Arms)$/, "collectively \"Armstrong\")")
        text = text.replaceAll(/Carol A\. Romej, J\.D\., L\.L\.M\./, 'Carol A Romej')
        text = text.replaceAll(/TD Bank  Bangor ,Fairfield,Auburn/, 'TD Bank Bangor')
        text = text.replaceAll(/Unversity/, 'University')
        text = text.replaceAll(/Stanwich Mortgage Loan Trust A, C and D/, 'Stanwich Mortgage Loan Trust A C AND D')
        text = text.replaceAll(/University of Vermont Health Network - Elizabethtown Community Hospital/, 'University of Vermont Health Network dba Elizabethtown Community Hospital')
        text = text.replaceAll(/(Lewis Brisbois Bisgaard & Smith LLP), 1700 Lincoln Street, Suite 4000, Denver, CO 80203/, '$1')
        text = text.replaceAll(/\(the parent company of (Wealth Enhancement Advisory Services LLC).*/, 'dba $1')
        text = text.replaceAll(/Wisconsin Health Care LIability Insurance Plan by (.*)/, 'Wisconsin Health Care Liability Insurance Plan dba $1')
        text = text.replaceAll(/This notice is being provided by Tablet Inc\..*/, 'Tablet Inc.')
        text = text.replaceAll(/John SMith/, 'John Smith')
        text = text.replaceAll(/Lewis Rice LL/, 'Lewis Rice LLC')
        text = text.replaceAll(/Ticher BMW Porche/, 'Tischer BMW Porsche')
        text = text.replaceAll(/Willie TS/, 'Willie T\'s')
        text = text.replaceAll(/Vice President, Operations/, '')
        //AND
        text = text.replaceAll(/(?<=Windows|Keppel|Industrial|Golf|Bank|Trust|Walters|Drye|Seymour|Haynes|Lundy)\s+and\s+(?=Boone|Pease|Warren|Mason|Doors|Koryak|Applied|Country|Trust|Investment|Bookman)/, ' AND ')
        text = text.replaceAll(/(?<=Fame|Toyota|Investments|Manufacturing|Edelman|Bisgaard|Ropes|Heath|Adams|Education|Rollie)\s+and\s+(?=Helens|Reese|Lyman|Gray|Smith|Dicker|Museum|Lexus|Insurance|Distributing|Ministry)/, ' AND ')
        text = text.replaceAll(/(?<=Downtown Brewery)\s+and\s+(?=Rest)/, ' AND ')
        text = text.replaceAll(/\s+and\s+(?=Company)/, ' AND ')
        text = text.replaceAll(/Esq\.?/, '')
        //COMMA
        text = text.replaceAll(/(?<=Augusta|Morgan|Hoge|Fenton|Smith|Hall|Killian|Skadden|Arps|Slate|Sheppard|Mullin|Orrick|City of Fremont|City of Athens|Amerman|Cadwalader)\s*,\s*(?=Heerington|Richter|Mullin|Meagher|Arps|Slate|Heath|Render|Gambrell|Maine|Lewis|Fenton|Jones|Nebraska|OH|Ginder|Wickersham)/, ' ')
        text = text.replaceAll(/(?<=Ciccone|DiGiovine|Hnilo|Eshel|City of Sunrise|Kearns|Leone|Concord|Russell|Cooke County)\s*,\s*(?=Koseff|Hnilo|Jordan|Aminov|Florida|Brinen|McDonnell|Massachusetts|Brier|Texas)/, ' ')
        text = text.replaceAll(/,\s*(?=Phelps|Sater|Seymour|Edelman|Elser|Moskowitz|Render|Killian|Heath|Donelson|Bearman|Caldwell|Spadafora|Pearson|Bradley|Wallace|Pillsbury|Herrington)/, ' ')
        //SLASH
        text = text.replaceAll(/(?<=Group|Labor|BGGMC LLC|Moussa)\s*\/\s*(?=Nicole|Eversept|Hotel)/, ' dba ')
        text = text.replaceAll(/(?<=\(APA\))\s*;\s*(?=Global)/, ' and ')
        text = text.replaceAll(/(Personal Touch Holding Corp\.) and its direct and indirect.*?(Personal Touch.*?), (.*?) and (.*)/, '$1 dba $2 dba $3 dba $4')
        text = text.replaceAll(/, its affiliates and subsidiaries/, '')
        text = text.replaceAll(/and its subsidiaries/, '')
        text = text.replaceAll(/and\/or its Affiliates/, '')
        text = text.replaceAll(/(.*?) \(as data maintainer\) on behalf of (.*?) \(as data owner\)/, '$1 dba $2')
        text = text.replaceAll(/a predecessor-in-interest to/, 'dba')
        text = text.replaceAll(/(Personal Touch Holding Corp\.) and its direct and indirect subsidiaries (.*?VA Inc\.)/, '$1 ($2)')
        text = text.replaceAll(/Kenneth D\. Pierce \/Jeffrey Boogay/, 'Kenneth D. Pierce dba Monaghan Leahy LLP, Jeffrey Boogay dba Mullen Coughlin LLC')
        text = text.replaceAll(/Monaghan Leahy LLP\/Mullen Coughlin LLC/, '')
        text = text.replaceAll(/\(P\.C\.\)/, 'P.C.')
        text = text.replaceAll(/(?:trrstysrtrt|jmhjmj|retewrtwer|wetrwertewrter|trwertwertwert).*/, '')
        text = text.replaceAll(/(DLA Piper LLP) \(US\)/, '$1')
        text = text.replaceAll(/TD Bank Bangor ,Fairfield,Auburn/, 'TD Bank Bangor')
        text = text.replaceAll(/Bonny L. Hutchins,Reynolds,House,Buzzell/, 'Bonny L. Hutchins, Bonny Reynolds, Bonny House, Bonny Buzzell')
        text = text.replaceAll(/Dr\. DeLuca, Dr\. Marciano/, 'Dr. DeLuca Dr. Marciano')
        text = text.replaceAll(/, A Codorus Valley Company/, '')
        text = text.replaceAll(/Bombas LLS/, 'Bombas LLC')
        text = text.replaceAll(/Cael Patten/, 'Carl Patten')
        text = text.replaceAll(/Czuprynksi/, 'Czuprynski')
        text = text.replaceAll(/Womble Bond Dickinson \(US\) LLP\./, 'Womble Bond Dickinson LLP.')
        text = text.replaceAll(/WilsonElser/, 'Wilson Elser')
        text = text.replaceAll(/\([\"\“](.*?)[\"\”]\)/, 'dba $1')
        text = text.replaceAll(/\(d\/b\/a\/?(.*?)\)/, 'dba $1')
        return text.toString().trim()
    }

    def sanitizeEntityName(def entityName) {
        entityName = entityName.toString().trim()
        entityName = entityName.replaceAll(/\sAND\s/, " and ").trim()
        entityName = entityName.replaceAll(/\sSLASH\s/, "/").trim()
        entityName = entityName.replaceAll(/(?i)^(?:Dr|Sr|Jr|Mrs|Mr)\.\s/, "").trim()
        entityName = entityName.replaceAll(/(DeLuca Dr\. Marciano & Associates P\.C\.)/, 'Dr. $1').trim()
        entityName = entityName.replaceAll(/Fields Gifts/, 'Mrs. Fields Gifts')
        return entityName.toString().trim()
    }

    def sanitizeAddress(def address) {
        address = address.toString().trim()
        address = address.replaceAll(/One Old Country Road/, "1 Old Country Road")
        address = address.replaceAll(/los Angeles/, "Los Angeles")
        address = address.replaceAll(/Ontario, Ontario/, "Ontario")
        address = address.replaceAll(/Plymouth Meeting PA/, "Plymouth Meeting, PA")
        address = address.replaceAll(/3300 Stelzer Road Columbus/, "3300 Stelzer Road, Columbus")
        address = address.replaceAll(/3300 Stelzer Road Columbus/, "3300 Stelzer Road, Columbus")
        address = address.replaceAll(/10300 SW Greenburg Road, Ste 570, Portland/, "10300 SW Greenburg Road, Ste 570, Portland, OR 97223")
        address = address.replaceAll(/10990 Roe Ave, Overland, Park/, "10990 Roe Ave, OVERLAND PARK")
        address = address.replaceAll(/Media PA 19063/, "Media, PA 19063")
        address = address.replaceAll(/1212 5th Street Coralville/, "1212 5th Street, Coralville")
        address = address.replaceAll(/P\.O\. Box 641048, Pullman, WA/, "P.O. Box 641048, Pullman, WA 99164")
        address = address.replaceAll(/Suite 100 Colorado Springs/, "Suite 100, Colorado Springs")
        address = address.replaceAll(/Suite 2200; Seattle/, "Suite 2200, Seattle")
        address = address.replaceAll(/SE Suite 114; Cedar Rapids/, "SE Suite 114, Cedar Rapids")
        address = address.replaceAll(/Suite 4 Commack/, "Suite 4, Commack")
        address = address.replaceAll(/Ste 300; Kansas City/, "Ste 300, Kansas City")
        address = address.replaceAll(/500 So Garland Road.*/, "500 S Garland Rd, Enid, OK 73703")
        address = address.replaceAll(/5530 St\. Patrick Street.*/, "5530 Rue Saint-Patrick #2210, Montreal, QC H4E1A8, Canada")
        address = address.replaceAll(/(Suite \d+)\s(?!,)/, '$1, ')
        address = address.replaceAll(/(Suite \d+);/, '$1,')
        address = address.replaceAll(/VA 22727: 540-948-2272/, 'VA 22727')
        address = address.replaceAll(/71 South Wacker, Illinois 60606/, '71 South Wacker, Chicago, Illinois 60606')
        address = address.replaceAll(/\(Email\)/, '').trim()
        address = address.replaceAll(/NC 28201-1122; PO#122004A/, 'NC 1122').trim()
        address = address.replaceAll(/S\. Portland, Maine 04106/, 'South Portland, ME 04106').trim()
        address = address.replaceAll(/Munkedamsveien 35, 6 fl.*/, 'Munkedamsvelen 35, 6th Floor, Vika, 0250 Oslo, Norway').trim()
        address = address.replaceAll(/(?<=(?:Road|Rd.?|Drive|Dr\.|Ave\.?|Avenue|Pkwy|Parkway|Street|St\.|Blvd\.?))[\s;]/, ', ').trim()
        address = address.replaceAll(/(?ism)^(.+?)$/, { def a, b -> return b + ", USA" })
        address = address.replaceAll(/R. Prof. Fernando da Fonseca.*/, "Rua Prof. Fernando da Fonseca, Edifício, Rotunda Visc. de Alvalade, 6 piso, 1600-616 Lisboa, Portugal")
        address = address.replaceAll(/Quebec, Canada H4E1A8, USA/, "Quebec, Canada H4E1A8")
        address = address.replaceAll(/China 518000 \(China\), USA/, "China 518000, China")
        address = address.replaceAll(/Canada, USA/, "Canada")
        address = address.replaceAll(/Silver Spring, USA/, "Silver Spring, MD, USA")
        // CSV 2
        address = address.replaceAll(/(?i)Tortola, N\/A, British Virgin Islands.*/, "Tortola, BRITISH VIRGIN ISLANDS")
        address = address.replaceAll(/(?i)Ra'anana, Israel, Israel.*/, "Ra'anana, Israel")
        address = address.replaceAll(/(?i)Info not provided.*/, "USA")
        address = address.replaceAll(/(?i)Not provided.*/, "USA")
        address = address.replaceAll(/(?i)Not stated.*/, "USA")
        address = address.replaceAll(/(?i)See attached notifications.*/, "USA")
        address = address.replaceAll(/N\/A.*/, "USA")
        address = address.replaceAll(/District of Columbia/, "DC")
        address = address.replaceAll(/8 East Washington Street, Athens, Ohio 45701/, "8 E Washington St, Athens, OH 45701")
        address = address.replaceAll(/932 Washington Street, Norwood, Massachusetts 02062/, "932 Washington St, Norwood, MA 02062")
        address = address.replaceAll(/1821 SE Washington Blvd, Bartlesville, OKLAHOMA 74006/, "1821 SE Washington Blvd, Bartlesville, OK 74006")
        address = address.replaceAll(/Washington Street/, "Washington St")
        address = address.replaceAll(/West Virginia/, "WV")
        address = address.replaceAll(/(?i)Virgina/, "VA")
        address = address.replaceAll(/(?i)Massechusetts/, "MA")
        address = address.replaceAll(/Harmondsworth, West Drayton UB7 0GB, UK/, 'Harmondsworth, West Drayton, UB7 0GB, UK')
        if (address.contains("Canada") || address.contains("QC") || address.contains("UK") || address.contains("China") || address.contains("Israel") || address.contains("British") || address.contains("Ontario") || address.contains("Norway") || address.contains("Bahamas")) {
            address = address.replaceAll(/(?i), USA/, "")
        }
        if (address != ~/(?m)^,/) {
            address = address.replaceAll(/(.*)/, ', $1')
        }
        address = address.replaceAll(/,\s*,/, ",")
        address = address.replaceAll(/(?i)(?:ALABAMA(?=,.*?,\s{0,3}USA))/, "AL").trim()
        address = address.replaceAll(/(?i)(?:ALASKA(?=,.*?,\s{0,3}USA))/, "AK").trim()
        address = address.replaceAll(/(?i)(?:AMERICAN SAMOA(?=,.*?,\s{0,3}USA))/, "AS").trim()
        address = address.replaceAll(/(?i)(?:ARIZONA(?=,.*?,\s{0,3}USA))/, "AZ").trim()
        address = address.replaceAll(/(?i)(?:ARKANSAS(?=,.*?,\s{0,3}USA))/, "AR").trim()
        address = address.replaceAll(/(?i)(?:CALIFORNIA(?=,.*?,\s{0,3}USA))/, "CA").trim()
        address = address.replaceAll(/(?i)(?:COLORADO(?=,.*?,\s{0,3}USA))/, "CO").trim()
        address = address.replaceAll(/(?i)(?:CONNECTICUT(?=,.*?,\s{0,3}USA))/, "CT").trim()
        address = address.replaceAll(/(?i)(?:DELAWARE(?=,.*?,\s{0,3}USA))/, "DE").trim()
        address = address.replaceAll(/(?i)(?:FLORIDA(?=,.*?,\s{0,3}USA))/, "FL").trim()
        address = address.replaceAll(/(?i)(?:GEORGIA(?=,.*?,\s{0,3}USA))/, "GA").trim()
        address = address.replaceAll(/(?i)(?:GUAM(?=,.*?,\s{0,3}USA))/, "GU").trim()
        address = address.replaceAll(/(?i)(?:HAWAII(?=,.*?,\s{0,3}USA))/, "HI").trim()
        address = address.replaceAll(/(?i)(?:IDAHO(?=,.*?,\s{0,3}USA))/, "ID").trim()
        address = address.replaceAll(/(?i)(?:ILLINOIS(?=,.*?,\s{0,3}USA))/, "IL").trim()
        address = address.replaceAll(/(?i)(?:INDIANA(?=,.*?,\s{0,3}USA))/, "IN").trim()
        address = address.replaceAll(/(?i)(?:IOWA(?=,.*?,\s{0,3}USA))/, "IA").trim()
        address = address.replaceAll(/(?i)(?:KANSAS(?=,.*?,\s{0,3}USA))/, "KS").trim()
        address = address.replaceAll(/(?i)(?:KENTUCKY(?=,.*?,\s{0,3}USA))/, "KY").trim()
        address = address.replaceAll(/(?i)(?:LOUISIANA(?=,.*?,\s{0,3}USA))/, "LA").trim()
        address = address.replaceAll(/(?i)(?:MAINE(?=,.*?,\s{0,3}USA))/, "ME").trim()
        address = address.replaceAll(/(?i)(?:MARYLAND(?=,.*?,\s{0,3}USA))/, "MD").trim()
        address = address.replaceAll(/(?i)(?:MASSACHUSETTS(?=,.*?,\s{0,3}USA))/, "MA").trim()
        address = address.replaceAll(/(?i)(?:MICHIGAN(?=,.*?,\s{0,3}USA))/, "MI").trim()
        address = address.replaceAll(/(?i)(?:MINNESOTA(?=,.*?,\s{0,3}USA))/, "MN").trim()
        address = address.replaceAll(/(?i)(?:MISSISSIPPI(?=,.*?,\s{0,3}USA))/, "MS").trim()
        address = address.replaceAll(/(?i)(?:MISSOURI(?=,.*?,\s{0,3}USA))/, "MO").trim()
        address = address.replaceAll(/(?i)(?:MONTANA(?=,.*?,\s{0,3}USA))/, "MT").trim()
        address = address.replaceAll(/(?i)(?:NEBRASKA(?=,.*?,\s{0,3}USA))/, "NE").trim()
        address = address.replaceAll(/(?i)(?:NEVADA(?=,.*?,\s{0,3}USA))/, "NV").trim()
        address = address.replaceAll(/(?i)(?:NEW HAMPSHIRE(?=,.*?,\s{0,3}USA))/, "NH").trim()
        address = address.replaceAll(/(?i)(?:NEW JERSEY(?=,.*?,\s{0,3}USA))/, "NJ").trim()
        address = address.replaceAll(/(?i)(?:NEW MEXICO(?=,.*?,\s{0,3}USA))/, "NM").trim()
        address = address.replaceAll(/(?i)(?<=, New York,\s)(?:NEW YORK(?=,.*?,\s{0,3}USA))/, "NY").trim()
        address = address.replaceAll(/(?i)(?:NORTH CAROLINA(?=,.*?,\s{0,3}USA))/, "NC").trim()
        address = address.replaceAll(/(?i)(?:NORTH DAKOTA(?=,.*?,\s{0,3}USA))/, "ND").trim()
        address = address.replaceAll(/(?i)(?:NORTHERN MARIANA(?=,.*?,\s{0,3}USA))/, "MP").trim()
        address = address.replaceAll(/(?i)(?:OHIO(?=,.*?,\s{0,3}USA))/, "OH").trim()
        address = address.replaceAll(/(?i)(?:OKLAHOMA(?=,.*?,\s{0,3}USA))/, "OK").trim()
        address = address.replaceAll(/(?i)(?:OREGON(?=,.*?,\s{0,3}USA))/, "OR").trim()
        address = address.replaceAll(/(?i)(?:PENNSYLVANIA(?=,.*?,\s{0,3}USA))/, "PA").trim()
        address = address.replaceAll(/(?i)(?:PUERTO RICO(?=,.*?,\s{0,3}USA))/, "PR").trim()
        address = address.replaceAll(/(?i)(?:RHODE ISLAND(?=,.*?,\s{0,3}USA))/, "RI").trim()
        address = address.replaceAll(/(?i)(?:SOUTH CAROLINA(?=,.*?,\s{0,3}USA))/, "SC").trim()
        address = address.replaceAll(/(?i)(?:SOUTH DAKOTA(?=,.*?,\s{0,3}USA))/, "SD").trim()
        address = address.replaceAll(/(?i)(?:TENNESSEE(?=,.*?,\s{0,3}USA))/, "TN").trim()
        address = address.replaceAll(/(?i)(?:TEXAS(?=,.*?,\s{0,3}USA))/, "TX").trim()
        address = address.replaceAll(/(?i)(?:UTAH(?=,.*?,\s{0,3}USA))/, "UT").trim()
        address = address.replaceAll(/(?i)(?:VERMONT(?=,.*?,\s{0,3}USA))/, "VT").trim()
        address = address.replaceAll(/(?i)(?:VIRGINIA(?=,.*?,\s{0,3}USA))/, "VA").trim()
        address = address.replaceAll(/(?i)(?:VIRGIN ISLANDS(?=,.*?,\s{0,3}USA))/, "VI").trim()
        address = address.replaceAll(/(?i)(?:WASHINGTON(?=,.*?,\s{0,3}USA))/, "WA").trim()
        address = address.replaceAll(/(?i)(?:WEST VIRGINIA(?=,.*?,\s{0,3}USA))/, "WV").trim()
        address = address.replaceAll(/(?i)(?:WISCONSIN(?=,.*?,\s{0,3}USA))/, "WI").trim()
        address = address.replaceAll(/(?<!,)\s((?:AL|AK|AS|AZ|AR|CA|CO|CT|DC|DE|FL|GA|GU|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|MP|OH|OK|OR|PA|PR|RI|SC|SD|TN|TX|UT|VT|VA|VI|WA|WV|WI)\s*(?:[\d\-]+)?,\s*USA)/, ', $1').trim()
        address = address.replaceAll(/,$/, "").trim()
        address = address.replaceAll(/(?<!,)\sWA, D\.?C\.?(?!,\s)/, ", DC, WA,").trim()
        address = address.replaceAll(/\sWA, D\.?C\.?(?!,\s)/, " DC, WA,").trim()
        address = address.replaceAll(/NY, NY/, "New York, NY").trim()
        address = address.replaceAll(/1200 New York Avenue, NW, WA, DC 20005/, '1200 New York Ave NW, DC, WA, 20005').trim()
        address = address.replaceAll(/Ave, NW/, 'Ave NW').trim()
        address = address.toString().replaceAll(/(?s)\s+/, " ").trim()
        return address.toString().trim()
    }

    def sanitizeName(def name) {
        name = name.toString().trim()
        name = name.replaceAll(/(?s)\s+/, " ")
        name = name.replaceAll(/(?s)and\/or its subsidiary,/, "dba")
        name = name.replaceAll(/(?s)\(payroll vendor, ADP\)/, "dba ADP")
        name = name.replaceAll(/(?s)\(Training Academy\)/, "")
        name = name.replaceAll(/(?s)\(Real Estate Agency in Indiana\)/, "")
        name = name.replaceAll(/(?s)International Association.*?Workers/, "International Association of Sheet Metal Air Rail & Transport Workers")
        name = name.replaceAll(/Malcom &Baker/, 'Malcom & Baker')
        name = name.replaceAll(/Czuprynksi/, 'Czuprynski')
        name = name.replaceAll(/Garret/, 'Garrett')
        name = name.replaceAll(/Lewis Rice LL$/, 'Lewis Rice LLC')
        name = name.replaceAll(/Long Beach Pharacy/, 'Long Beach Pharmacy')
        name = name.replaceAll(/Magills GV Glockstore/, 'Magills GS Glockstore')
        name = name.replaceAll(/Haggety/, 'Haggerty')
        name = name.replaceAll(/Western Bagel Baking C$/, 'Western Bagel Baking Corp.')
        name = name.replaceAll(/Kristpoher/, 'Kristopher')
        name = name.replaceAll(/Bruce(?! Radke)/, 'Bruce Radke')
        name = name.replaceAll(/Jack(?=$)/, 'Jack Mason')
        name = name.replaceAll(/Jenny(?=$)/, 'Jenny Baldwin')
        name = name.replaceAll(/Katy(?=$)/, 'Katy Tester')
        name = name.replaceAll(/Richard(?=$)/, 'Richard Goldberg')
        name = name.replaceAll(/Robert(?=$)/, 'Robert Slaughter')
        name = name.replaceAll(/Ryan(?=$)/, 'Ryan Myhre')
        name = name.replaceAll(/Stacey(?=$)/, 'Stacey Beall')

        return name.toString().trim()
    }

    def fixName2(def name, def address) {
        name = name.toString().trim()
        address = address.toString().trim()

        if (address.contains("74 maple Street") & name.equals("Tim")) {
            name = "Tim Callow"
        }
        if (address.contains("600 Grant Street") & name.equals("Tim")) {
            name = "Tim Diamond"
        }

        return name
    }

    def sanitizeAlias(def alias) {
        alias = alias.toString().trim()
        alias = alias.replaceAll(/[\)\(\"]/, "")
        alias = alias.replaceAll(/(?s)\s+/, " ").trim()
        alias = alias.replaceAll(/Dunn/, "Dun").trim()
        alias = alias.replaceAll(/\sAND\s/, " and ").trim()
        alias = alias.replaceAll(/\sSLASH\s/, "/").trim()
        alias = alias.replaceAll(/Smooth-On Inc\. and Reynolds Advanced Materials/, "").trim()
        alias = alias.replaceAll(/N\/A/, '')
        return alias.toString().trim()
    }

    def sanitizeDate(def date) {
        date = date.toString().trim()
        date = date.toString().replaceAll(/by /, "").trim()
        date = date.toString().replaceAll(/(?:None|N\/A)/, "").trim()
        date = date.toString().replaceAll(/\(Electronic\), Written to follow as soon as feasible/, "").trim()
        date = date.toString().replaceAll(/\s*\/\s*/, "/").trim()
        date = date.toString().replaceAll(/4\/25\/217/, "4/25/2017").trim()
        date = date.toString().replaceAll(/4\/25\/217/, "4/25/2017").trim()
        date = date.toString().replaceAll(/5\/28\/215/, "5/28/2015").trim()
        date = date.toString().replaceAll(/March 31,2020/, "March 31, 2020").trim()
        date = date.toString().replaceAll(/^.*?((?:January|February|March|April|May|June|July|August|September|October|November|December) \d{1,2}\s*,\s*\d{2,4})\s*(?:and|to|through)\s*((?:January|February|March|April|May|June|July|August|September|October|November|December) \d{1,2}\s*,\s*\d{2,4})\.*/, '$1 - $2').trim()
        date = date.toString().replaceAll(/(?s)\s+/, " ").trim()
        date = date.toString().replaceAll(/[,#\?]$/, "").trim()
        return date.toString().trim()
    }

    def separateDate(def date) {
        date = date.toString().trim()

        def dateList = [] as List

        if (date =~ /(?:\sand\s|\s&\s)/) {
            dateList = date.split(/(?:\sand\s|\s&\s)/).collect({
                it.toString().trim()
            })

        } else {
            dateList.add(date)
        }
        return dateList
    }

    def separateName(def name) {
        def nameList = []

        if (name =~ /(?:,|(?<=\s)and(?=\s)|;)/) {
            nameList = name.split(/(?:,|(?<=\s)and(?=\s)|;)/).collect({
                it.toString().trim()
            })
        } else {
            nameList.add(name)
        }
        return nameList
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        return street
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = false, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = false, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}