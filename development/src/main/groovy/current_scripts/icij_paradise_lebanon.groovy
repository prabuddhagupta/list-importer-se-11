package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils

context.session.encoding = "UTF-8"
context.session.escape = true

ICIJ_Paradise_Papers_Lebanon script = new ICIJ_Paradise_Papers_Lebanon(context)
script.initParsing()
int i = 1;

def nameIdMap = [:];
for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + i++);
    nameIdMap[entity.getName()] = entity
}

for (entity in context.session.getEntitiesNonCache()) {
    for (association in entity.getAssociations()) {
        def relatedEntityName = association.split("~~~~")[0]
        def relationType = association.split("~~~~")[1]
       // println relationType
        otherEntity = nameIdMap[relatedEntityName];
        println relationType
        if (otherEntity && !isAlreadyAssociated(entity, otherEntity)) {
            scrapeEntityAssociation = new ScrapeEntityAssociation(otherEntity.getId());

            if (relationType.toString().contains("Owner")) {
                scrapeEntityAssociation.setRelationshipType(RelationshipType.SHAREHOLDER_OWNER);
            } else {
                println "Relation: "+relationType +"Other Entity Name: "+otherEntity.getName()
                scrapeEntityAssociation.setRelationshipType(RelationshipType.ASSOCIATE);
            }
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

class ICIJ_Paradise_Papers_Lebanon {
    final addressParser
    final entityType
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context


    ICIJ_Paradise_Papers_Lebanon(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context
    }

    def initParsing() {

        //String path = "file:///home/sazzad/Downloads/icij_lebanon.xlsx"
        //String path = "/Downloads/icij_lebanon.xlsx"
        String path = "file://C:\\Users\\Mahadi\\Downloads\\icij_lebanon (1).xlsx"

        File file = new File(System.getProperty("user.home"), path)

        def spreadsheet = invokeBinary(path)
        def xml = context.transformSpreadSheet(spreadsheet, [validate: false, escape: false, headers: ['Type', 'Entity_Name', 'Alias', 'entities_address', 'entities_country', 'addresses_address', 'addresses_name', 'addresses_countries', 'Address_1', 'Address_2', 'City', 'State', 'Country', 'Relationship_type_1', 'Relationship_type_2', 'GRID_Relationship', 'Related_Entity', 'sourceID', 'Entity_URL', 'URL']])
        def rows = new XmlSlurper().parseText(xml.value);
        for (int i = 1; i < rows.row.size(); i++) {
            def row = rows.row[i]
            handleRowData(row)
        }
    }

    def handleRowData(def row) {
        def name = row.Entity_Name.text().trim()
        def entityType = row.Type.text().trim()
        def related_entity = row.Related_Entity.text().trim()
        def relation = row.GRID_Relationship.text().trim()
       // println "Relation: "+relation
        def url = row.URL.text().trim()
        createEntity(name, entityType, url, relation, related_entity)
    }

    def createEntity(name, entityType, url, relation, related_entity) {
        def entity = context.findEntity(["name": name, "type": entityType])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(entityType)

            ScrapeEvent event = new ScrapeEvent()
            def description = "This entity appears in the International Consortium of Investigative Journalists (ICIJ) Offshore Leaks Database as a part of the Paradise Papers - Lebanon corporate registry data set. The Paradise Papers investigation is based on a leak of more than 13.4 million files from one offshore law firm, Appleby, as well as Asiaciti Trust, a Singapore-based provider, and government corporate registries in 19 secrecy jurisdictions."
            event.setDescription(description)
            entity.addEvent(event)

            def remark = "ICIJ Disclaimer: There are legitimate uses for offshore companies and trusts. The ICIJ does not intend to suggest or imply that any persons, companies or other entities included in the ICIJ Offshore Leaks Database have broken the law or otherwise acted improperly. Many people and entities have the same or similar names. The ICIJ suggests you confirm the identities of any individuals or entities located in the database based on addresses or other identifiable information. If you find an error in the database please get in touch with the ICIJ."
            entity.addRemark(remark)
            if (url) {
                entity.addUrl(url)
            }
        }

        if (StringUtils.isNotBlank(related_entity)) {
            entity.addAssociation(related_entity + "~~~~" + relation)
        }
    }


    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = false, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }
}