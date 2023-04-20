package current_scripts

import com.rdc.importer.scrapian.ScrapianContext

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent
import org.apache.poi.hssf.usermodel.HSSFSheet


import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

context.setup([connectionTimeout: 25000, socketTimeout: 25000, retryCount: 10, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true;

DisciplinaryActionALCB script = new DisciplinaryActionALCB(context);
script.initParsing()

int assocID = 1;

def nameIdMap = [:]

for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + assocID++);
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

class DisciplinaryActionALCB {
    final ScrapianContext context
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def root = "https://www.mass.gov"
    //static def url = "https://www.mass.gov/dpl-disciplinary-actions/resources"
    static def url = "https://www.mass.gov/division-of-occupational-licensure-dol-disciplinary-actions/resources"
    static final DEFAULT_ADDRESS = "Massachusetts, UNITED STATES"
    final addressParser
    final entityType

    DisciplinaryActionALCB(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        //  addressParser.reloadData()
    }

    def initParsing() {
        def list = []
        def fileType = []
        def html = invoke(url)
        def lastPageIndex = getLastPageIndex(html)
        (list, fileType) = getXLSLinks(lastPageIndex)

        list.eachWithIndex { link, index1 ->

            fileType.eachWithIndex { type, index2 ->
                if (index1 == index2) {

                    if (link.equals("/doc/dpl-nature-codes/download")) {
                        return
                    }
                    parseXLS(root + link, type)
                }
            }

        }
    }

    def getNameAliasTuple(String inputField) {
        def splitter = inputField =~ /(.*[^0-9])(\/)([^0-9]*.*)/

        def names = []
        def entityName = []

        if (splitter.find()) {
            if (splitter.group(1) != null)
                entityName.add(splitter.group(1))
            if (splitter.group(3) != null)
                entityName.add(splitter.group(3))
        } else {
            entityName.add(inputField)
        }

        entityName.each {
            def bracketMatcher = it =~ /(\()(.*?)(\))/
            def commaMatcher = it =~ /(?i),\s(?!\b(LLC|INC|PC|CO)\.?\b)/

            if (bracketMatcher.find()) {
                def alias = bracketMatcher.group(2).replaceAll(/(?ism)\b(aka|dba|fka)\b/, "")
                    .replaceAll(/\s+/, " ")
                    .trim()
                def name = it.replaceAll(alias, "")
                    .replaceAll(/\(/, "")
                    .replaceAll(/(?ism)\b(aka|dba|fka)\b/, "")
                    .replaceAll(/\)/, "")
                    .replaceAll(/\s+/, " ")
                    .trim()
                names.add([name, alias])
            } else if (commaMatcher.find()) {
                def split = it.split(",")

                def name = split[0].trim()
                def alias = split[1].trim()

                names.add([name, alias])
            } else {
                def aliasMatcher = it =~ /(?ism)\b(aka|dba|fka)\b(.*)/

                if (aliasMatcher.find()) {
                    def delete = aliasMatcher.group(0)
                    def alias = aliasMatcher.group(2)
                    it = it.replaceAll(/$delete/, "")

                    names.add([it.trim(), alias.trim()])
                } else {
                    names.add([it.trim(), null])
                }
            }
        }
        return names
    }

    def createNewEntity(String name, String alias, String type, String address, String date, def url) {

        if (name == null || name.trim().equals("") || name == "null")
            return null

        ScrapeEntity entity = null

        entity = context.findEntity("name": name, "type": type)

        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(type)
        }

        if (alias != null) {
            String aliasType = getEntityType(alias)
            if (aliasType.equals(type)) {
                entity.addAlias(alias)
            } else {
                entity.addAssociation(alias)
                ScrapeEntity newEntity = createNewEntity(alias, null, aliasType, address, date, url)
                newEntity.addAssociation(name)
            }
        }

        ScrapeEvent scrapeEvent = new ScrapeEvent();
        scrapeEvent.setDescription("This entity appears on the Massachusetts Division of Professional Licensure's list of Disciplinary Actions.");

        def sdate = context.parseDate(new StringSource(date), ["dd-MMM-yyyy", "MM/dd/yyyy"] as String[])
        scrapeEvent.setDate(sdate)

        entity.addEvent(scrapeEvent)

        def parsedAddress = addressParser.parseAddress([text: address, force_country: true])
        ScrapeAddress scrapeAddress = addressParser.buildAddress(parsedAddress)

        entity.addAddress(scrapeAddress)
        entity.addUrl(url)

        return entity
    }

    def processEntity(String entityName, def address, String date, def url) {
        def nameAliasTuples = getNameAliasTuple(entityName)

        ScrapeEntity firstEntity
        def firstEntityType

        nameAliasTuples.eachWithIndex { tuple, int index ->
            String name, alias
            (name, alias) = tuple

            def entityType = getEntityType(name.toString())

            if (index == 0) {
                firstEntityType = entityType

                firstEntity = createNewEntity(name.toString(), alias, entityType.toString(), address, date, url)
            } else {
                if (entityType.equals(firstEntityType)) {
                    firstEntity.addAlias(name)
                } else {
                    firstEntity.addAssociation(name)
                    ScrapeEntity newEntity = createNewEntity(name, alias, entityType, address, date, url)
                    if (newEntity != null) {
                        newEntity.addAssociation(firstEntity.getName())
                    }
                }
            }
        }
    }

    def santizeName(String name) {
        name = name.replaceAll(/\s+/, " ").trim()
        name = name.replaceAll(/`/, "")
        name = name.replaceAll(/(?i),?\s?\b(Jr|Sr)\.?\b/, "")
        name = name.replaceAll(/&apos;/, "'")
        name = name.replaceAll(/HE-15-012/, "Colin Mackenzie")
        name = name.replaceAll(/^UE, FS$/, "")
        name = name.replaceAll(/As of.*/, "").trim()
        name = name.replaceAll(/Mr\.?\s(?!Hair)/, "")
        name = name.replaceAll(/David Willette "The Gas Man"/, "David Willette/The Gas Man")
        name = name.replaceAll(/Niroza Kin\/ Cathy Nail Design\/ Cathy Spa/, "Niroza Kin/ Cathy Nail Design aka Cathy Spa")
        name = name.replaceAll(/Tip Toe Spa \( Phillip Dang/, "Tip Toe Spa (Phillip Dang)")
        return name
    }

    def getEntityType(def name) {
        def typeMatcher = name =~ /(?i)\b(Physical|Personal|Institute|Simply|Stunning|Bodyworks|Nena's|Capelli|LLP|Estate|Company|New|^Donna$|Visage|Star|House|Novita|Scissor|Aroma|Stylish|Magic|PTCO|Miniluxe|Choice|Chiropractic|Stillisti|wareham|Del Estilo|Virginia|Childs?|Boston|Massge|Face|Yoga|Panthers|Aesthetics?|Medical|Massgae|Group|Maple|Utility|P\.?T\.?|Pureluxe|Image|Lighten|HD|Optimal|Absolute|Frankenhair|Pain|Reflexology|Central|Alternate|Art|Brow|Alterity|Electrical|Glamour|Extreme|Healthy|Bellezza|Workshop|Bodywork|Touche|Relief|Rhythms?|Snip|Modern|America|Shop|Custom|Together|Corporation|Power|Twist|Headlines|Brighter|Viva|Cost|Basbershop|Aristides|Frost|Seaport|Lexington|of|Build|Equinox|Designs?|Cuts|Imagenes|Great|VIP|UE, FS|Hand|Technicuts|Glass|Stone|May Spring|Clips|Chiropratic|Family|Body|Lash|Bar|99|Best|Golden|Stars|Valley|Relaxology|Stylist|Back|Beehive|Relax(ation)?|Room|Foot|bCo\.?|Changes|Nice|Looks?|Shave|London|Acote|and|Euphoria|Blades?|Inc\.?|Century|Styles?|Finest|Downtown|League|Funeral|Massage|Thai|Herbal|Touch|Comfort|Exclusive|Natural|Ancient|Connection|Fitness|Club|Hands|Healing|Therapy|Cherry|Associates|Blossoms?|Elegance|by|Engineers|LLC,?|Cut[sz]?|Bellessimo|Polished|Mini|Advanced|Suburban|Heating|Cosmtique|Sharp|Fantastic|Diva|Celebrity|Barber|Hair|Salon|Creation|Styling|Razor|Barbers?|Barbershop|International|Community|Under|Carlito|Edge|Abracadabra|Electric|Beauty|Outlooks?|Balance|Skylines|Nutrapy|Clips?|Training|Shears?|Mantrap|Skincatering|Ladys?|Deluxe|\s&\s|Brothers|Baoerbershop|Savanha|State|United|B803295|Care|Nails?|Lounge|Brows?|Wellness|Indulgence|Fancy|Spa|Hairport|Mane & Mani|Luxebar|Studios?|Corner|Sons|3D|Place|Unique|Supercuts?|Parlor|Skin|Unisex|Cottage|The|Impressionz|Tech|Center|Joint|Ventures?|Kerbs|Tissue|P\.T|B\.?S|Level|Top|Notch)\b/
        if (typeMatcher.find()) {
            return "O"
        } else {
            return "P"
        }
    }

    def getXLSLinks(int lastPageIndex) {
        def list = []
        def fileType = []
        for (int i = 0; i <= lastPageIndex + 1; i++) {
            def pageURL = url + "?page=" + i

            String html = invoke(pageURL)
            html = html.replaceAll(/\s+/, " ")


            // def downloadLinks = html =~ /(?ism)(<a class="ma__download-link__file-link" href=")(.*?)(">)/
            def downloadLinks = html =~ /(?ism)<a class="ma__download-link__file-link" href="(.*?)">.*?\(((?:PDF|XLS[X]?))\s.*?\d+\.?\d*\skb\)<\/span>/


            while (downloadLinks.find()) {
                list.add(downloadLinks.group(1))
                fileType.add(downloadLinks.group(2))
            }
        }
        return [list, fileType]
    }

    def getLastPageIndex(html) {
        def rootMatcher = html =~ /(?ism)<div class="ma__pagination__container">.*?<\/div>/
        if (rootMatcher.find()) {
            def paginationBlock = rootMatcher.group(0).replaceAll(/\s+/, " ")
            paginationBlock = paginationBlock =~ /(?ism)(page=)([0-9]+)(?=" class="ma__pagination__next js-pagination-next)/

            if (paginationBlock.find())
                return Integer.valueOf(paginationBlock.group(2))
        }
    }

    def parseXLS(def url, def fileType) {


        def spreadsheet = context.invokeBinary([url: url])
        def sheets = download(spreadsheet, fileType)


        if (fileType.toString().equalsIgnoreCase("xlsx")) {
            getDataFromXLSX(sheets)
        } else {
            getDataFromXLS(sheets)
        }

//
//        ExcelTransformer excelTransformer = new ExcelTransformer();
//        def sheets = excelTransformer.transformMultiSheets(spreadsheet, [validate: true, escape: true, headers: ["DOCKET_NUMBER", "NAME_OF_LICENSEE",
//                                                                                                                 "LICENSE_NUMBER", "NATURE_CODE",
        //                                                                                                                "EFFECTIVE_DATE", "DECISION"]])

        //            rows.row.eachWithIndex { it, index ->
//                def name = it["NAME_OF_LICENSEE"].toString().trim()
//
//                if (!name.equals("NAME OF LICENSEE") && name != null && name.size() != 0) {
//                    name = santizeName(name)
//                    def date = it["EFFECTIVE_DATE"].toString().trim()
//
//                    if (!name.equals("")) {
//                        processEntity(name, DEFAULT_ADDRESS, date, url)
//                    }
//                }
//           // }


    }

    private void getDataFromXLS(def sheets) {
        for (int i = 0; i < sheets.numberOfSheets; i++) {

            HSSFSheet sheet = sheets.getSheetAt(i);

            int r = sheet.size()

            int j = 0;
            while (j <= r) {
                def row = sheet.getRow(j)


                if (row) {
                    def name = row.getCell(1).toString().trim()

                    if (!name.equals("NAME OF LICENSEE") && name != null && name.size() != 0) {
                        name = santizeName(name)
                        def date = row.getCell(4).toString().trim()
                        if (!name.equals("")) {
                            processEntity(name, DEFAULT_ADDRESS, date, url)
                        }
                    }
                }
                j++
            }
        }
    }

    private void getDataFromXLSX(sheets) {
        for (int i = 0; i < sheets.numberOfSheets; i++) {

            XSSFSheet sheet = sheets.getSheetAt(i);

            int r = sheet.size()

            int j = 0;
            while (j <= r) {
                def row = sheet.getRow(j)

                if (row) {
                    def name = row.getCell(1).toString().trim()

                    if (!name.equals("NAME OF LICENSEE") && name != null && name.size() != 0) {
                        name = santizeName(name)
                        def date = row.getCell(4).toString().trim()
                        if (!name.equals("")) {
                            processEntity(name, DEFAULT_ADDRESS, date, url)
                        }
                    }
                }
                j++
            }
        }
    }

    def download(def byteSource, def fileType) {

        if (fileType.toString().equalsIgnoreCase("XLSX")) {
            InputStream inputStream = new ByteArrayInputStream((byte[]) byteSource.getValue());
            OPCPackage fs = OPCPackage.open(inputStream);
            XSSFWorkbook wb = WorkbookFactory.create(fs)
            return wb
        } else {
            InputStream inputStream = new ByteArrayInputStream((byte[]) byteSource.getValue());
            POIFSFileSystem fs = new POIFSFileSystem(inputStream);
            Workbook wb = WorkbookFactory.create(fs)
            return wb
        }
    }

    def invoke(url, cache = false, tidy = false) {
        return context.invoke([url: url, tidy: tidy, cache: cache])
    }
}