package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
//import com.rdc.scrape.ScrapeEvent

import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeEvent

import java.text.SimpleDateFormat


context.setup([connectionTimeout: 50000, socketTimeout: 50000, retryCount: 20, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_Ky_Dfi_Enforcement_Actions script = new Us_Ky_Dfi_Enforcement_Actions(context)

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

class Us_Ky_Dfi_Enforcement_Actions {
    final addressParser
    final entityType
    //final def ocrReader
    final def ocrSpaceReader
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final ScrapianContext context

    def headerUrl = "https://kfi.ky.gov/"
    //def mainUrl = headerUrl + "/legal/Pages/securities.aspx"
    def mainUrl = "https://kfi.ky.gov/new_bulletin.aspx?bullid=3"
    def testUrl = 'https://kfi.ky.gov/Documents/Sand%20Vegas%20Casino%20Club%20M.S.%20%20F.R.W%202022AH0012.pdf'


    Us_Ky_Dfi_Enforcement_Actions(context) {
        //ocrReader = moduleFactory.getOcrReader(context)
        ocrSpaceReader = moduleFactory.getOCRSpaceReader(context)

        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context
    }

    def initParsing() {
        /*def pdfText = pdfToTextConverter(testUrl)
        println pdfText
        println("*****************************************************************************")
        println sanitizePdfText(pdfText)

        parsePdf_UsKyDfiEnforcementActions("06/20/2022", testUrl, pdfText)*/

        def html = invokeUrl(mainUrl)
        html = sanitizeHtml(html)
        println html
        def dateAndEntityUrl = html =~ /href='(Documents.*?\.pdf)'.*?(\d{1,2}\/\d{1,2}\/\d{4})/
        def date
        def entityUrl
        def pdfText
        while (dateAndEntityUrl.find()) {
            date = dateAndEntityUrl.group(2)
            entityUrl = headerUrl + dateAndEntityUrl.group(1)
            entityUrl = entityUrl.toString().replaceAll(/\s(?=D 2021AH0020)/, "%20%20")
            entityUrl = entityUrl.toString().replaceAll(/\s(?=F\.R\.W 2022AH0012)/, "%20%20")
            entityUrl = entityUrl.toString().replaceAll(/\s/, "%20").replaceAll(/\(/, "%28").replaceAll(/\)/, "%29").replaceAll(/\)/, "%29").replaceAll(/,/, "%2C").trim()

            pdfText = pdfToTextConverter(entityUrl)
            parsePdf_UsKyDfiEnforcementActions(date, entityUrl, pdfText)
        }
    }

    def parsePdf_UsKyDfiEnforcementActions(def eventDate, def entityUrl, def pdfText) {
        //println "Pdf Text: " + pdfText
        pdfText = pdfText.toString().replaceAll(/[\r\n]+/, "\n")

        def pdfTextMatcher = pdfText =~ /(?ism)((?:COMMONWEALTH|The Alabama Securities|FRANKLIN CIRCUIT|CONLMONWEALTil|COMMENWEALTH|PUBLIC PROTECTION|In the matter of)((.*?\n){25,37}))/
        if (pdfTextMatcher.find()) {
            pdfText = pdfTextMatcher.group(1)
            pdfText = sanitizePdfText(pdfText)

            def nameMatcher = pdfText =~ /[\w\S ]+/
            def name
            while (nameMatcher.find()) {
                name = nameMatcher.group(0).trim()
                handleEntityNameAndAlias(name, eventDate, entityUrl)
            }
        }
    }

    def handleEntityNameAndAlias(def entityName, def eventDate, def entityUrl) {

        entityName = sanitizeName(entityName)

        def entityNameList = []
        def alias
        def aliasList = []
        def aliasMatcher

        if (entityName =~ /[,;]/) {
            entityNameList = entityName.toString().split(/[,;]/).collect({ it -> return it.trim() })

        } else if (entityName =~ /and\s(?:CHARLES|INDEPENDENT|ERNEST)/) {
            entityNameList = entityName.toString().split(/and/).collect({ it -> return it.trim() })
        } else {
            entityNameList.add(entityName)
        }
        entityNameList.each { name ->
            name = sanitizeName(name)

            if (name =~ /aka|a\/k\/a|\sa\s|President|PRESIDENT OF|a\/Ida|AKA|Director|D\/B\/A/) {
                aliasMatcher = name =~ /((?:aka|a\\/k\\/a|\sa\s|President|PRESIDENT OF|a\\/Ida|AKA|Director|D\/B\/A).+)/
                if (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1).trim()
                    name = name.toString().replaceAll(/$alias/, "")
                    aliasList = alias.toString().split(/(?:aka|a\/k\/a|^a\s|President|PRESIDENT OF|a\/Ida|AKA|DIB\/A|D\/B\/A|Director|ailc\/a)/).collect({ it -> return it.trim() })
                    createEntity(name, aliasList, eventDate, entityUrl)
                }
            } else if (name =~ /d\\/b\\/a|D\\/B\\/A/) {
                aliasMatcher = name =~ /((?:d\\/b\\/a|D\\/B\\/A).+)/
                if (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1).trim()
                    name = name.toString().replaceAll(/$alias/, "")
                    aliasList = alias.toString().split(/(?:d\\/b\\/a|D\\/B\\/A)/).collect({ it -> return it.trim() })
                    createEntity(name, aliasList, eventDate, entityUrl)
                }
            } else if (name =~ /\s"Wally"/) {
                aliasMatcher = name =~ /(\s"Wally"\s)/
                if (aliasMatcher.find()) {
                    alias = aliasMatcher.group(1).trim()
                    name = name.toString().replaceAll(/$alias/, "").replaceAll(/\s+/, " ")
                    aliasList.add(alias)
                    createEntity(name, aliasList, eventDate, entityUrl)
                }
            } else {
                createEntity(name, aliasList, eventDate, entityUrl)
            }
        }
    }

    def createEntity(def name, def aliasList, def eventDate, def entityUrl) {
        println name
        def entity = null
        if (!name.toString().isEmpty()) {

            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType]);
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }

            def aliasEntityType, alias
            aliasList.each {
                if (it) {
                    it = sanitizeAlias(it)
                    aliasEntityType = detectEntity(it)
                    if (aliasEntityType.toString().equals(entityType)) {
                        entity.addAlias(it)
                    } else {
                        if (aliasEntityType.equals("P")) {
                            it = sanitizeAlias(it)
                            entity.addAssociation(it)
                        } else {
                            it = sanitizeAlias(it)
                            entity.addAssociation(it)
                            //create new entity with association
                            def newEntity = context.findEntity(["name": it, "type": aliasEntityType]);
                            if (!newEntity) {
                                newEntity = context.getSession().newEntity();
                                newEntity.setName(it)
                                newEntity.setType(aliasEntityType)
                            }
                            newEntity.addAssociation(name)
                            addCommonPartOfEntity(newEntity, eventDate, entityUrl)
                        }
                    }
                }

            }
            addCommonPartOfEntity(entity, eventDate, entityUrl)
        }
    }

    def addCommonPartOfEntity(def entity, def eventDate, entityUrl) {
        def eventDescription = "This entity appears on the Kentucky Department of Financial Institutions list of Enforcement Actions."

        ScrapeAddress address = new ScrapeAddress()
        address.setProvince('Kentucky')
        address.setCountry('UNITED STATES')
        entity.addAddress(address)
        ScrapeEvent event = new ScrapeEvent()


        if (eventDate) {
            eventDate = context.parseDate(new StringSource(eventDate), ["MM/dd/yyyy", "MM/d/yyyy", "M/dd/yyyy", "M/d/yyyy"] as String[])

           /* println "Event Date: " + eventDate

            String DATE_FORMAT = "MM/dd/yyyy";

            // SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

            Date date = sdf.parse(eventDate)

            // To TimeZone America/New_York
            //SimpleDateFormat sdfAmerica = new SimpleDateFormat(DATE_FORMAT);
            TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
            sdf.setTimeZone(timeZone);

            String stringDate = sdf.format(date); // Convert to String first
            Date date2 = sdf.parse(stringDate); // Create a new Date object

            eventDate = sdf.format(date2)*/

            event.setDate(eventDate)

            println "Event Date: " + eventDate //+ " Converted Date: " + sdf.format(date2)
            if (eventDescription) {
                event.setDescription(eventDescription)
            }
            entity.addEvent(event)
            entity.addUrl(entityUrl)
        }
    }

    def detectEntity(def name) {
        def type
        if (name =~ /^Wally$/) {
            type = "P"
        } else if (name =~ /CLUB$/) {
            type = "O"
        } else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$|PROSPECT$)/) {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:ACE JEFFERSON|^Thomas Smith III$|BERRY III$)/) {
                type = "P"
            }
        }
        return type
    }

    def sanitizePdfText(def pdfText) {
        pdfText = pdfText.toString().replaceAll(/(?i)\*ifx Is/, "ifx Is")
        pdfText = pdfText.toString().replaceAll(/(?s)COMMONWEALTH OF.*2019-AH-00067\s*.*INSTITUTIONS\s*COMPLAINANT/, '')
        pdfText = pdfText.toString().replaceAll(/(?s)^.*2022-AH-00003.*/, "TYPHON ENERGY LLC\nand JEREMIAH DECKER                  RESPONDENTS")
        pdfText = pdfText.toString().replaceAll(/(?s)^.*2020-AH-00016.*/, "CINDY MADDOX                          RESPONDENT")
        pdfText = pdfText.toString().replaceAll(/(?s)^.*2022-AH-0012.*/, "SAND VEGAS CASINO CLUB,\n" + "MARTIN SCHWARZBERGER,\n" + "and FINN RUBEN WARNKE     RESPONDENTS")
        pdfText = pdfText.toString().replaceAll(/(?ism)[*]+\s*.*/, "")
        pdfText = pdfText.toString().replaceAll(/(?s).*(?=THOMAS J\.GORTER)/, "")
        pdfText = pdfText.toString().replaceAll(/(?s)(?:\n\s{15,}FINAL ORDER OF DEFAULT).*/, '')
        pdfText = pdfText.toString().replaceAll(/(?i)DIV\.|York v\.|COMMISSIONER'S FINDINGS.*\s.*|FINAL ORDER.*\s*.*CIVIL PENALTIES|(?<=v\.)\s{16,}AGREED ORDER/, "")
        pdfText = pdfText.toString().replaceAll(/(?i),\s*(?=L\.L\.P|L\.L\.C|Inc\.?|LLC|JR\.|Rose|a\/Ida|LLP|ET|NICOLAUS|LTD|FENNER|PIERCE|SR\.|a\/k\/a|PRESIDENT|AKA|C|AGEE|LP|III|11)/, " ")
        pdfText = pdfText.toString().replaceAll(/(?i),\s*(?=VIDEO|P\.S\.C\.,|LIMITED|SINCLAIRE)/, " ")
        pdfText = pdfText.toString().replaceAll(/(?is)^.*(?:VS[\.,]|V\.|v\s{8,}|Application of\s?:|IN THE MATTER OF:|IN THE MATTER OF|In Re:|IN THE MATTER\. OF:)/, "")
        pdfText = pdfText.toString().replaceAll(/FINAL ORDER.*INJUNCTIVE\s*RELIEF.*DISGORGEMENT|AGREED ORDER\s*(?=GLOBAL NETWORK PARTNERS)/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:FACTUAL BACKGROUND|PARTIES|CORRECTED FINAL ORDER|DEFAULT ORDER AGAINST|AGREED ORDER\s*The Department|NOTICE OF RESOLUTION AND AGREED ORDER).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:\s{5,}(?:FINAL ORDER|ORDER)\s*This matter having|WHEREAS|AGREED ORDER PURSUANT|\s{4,}This matter is before| \d\s*WHEREAS|FINAL ORDER\s*1\.).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:SETTLEMENT AGREEMENT & ORDER|(?:PRELIMINARY|JACTIJAL)\sBACKGROUND|BACKGROUND|AGREED ORDER\s*(?:1\.|Pursuant|STATEMENT OF FACTS|FINDINGS OF FACT\s|I\.|The Plaintiff|The Department of)).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:I___ADTFIORITY AND|ORDER APPOINTING|STATEMENT OF FACTS|NOTICE OF RESOLUTION\s*AND AMENDED|\s{15,}COMMISSIONER'S FINDINGS OF|TEMPORARY RESTRAINING).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:ORDER SETTING ASIDE|EMERGENCY CEASE AND|ORDER DISMISSING|FINAL ORDER\s*The Commissioner|FINAL ORDER DENYING|DFI'S FINDINGS OF FACT\s*1\..*).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:AGREED ORDER\s*(?:The Department of)|\s{3}Comes now the|\s{3}Whereas a hearing|\s{3}Upon Petition of the|1?\.?\s{2}?On or about|PRELIMINARY).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:AGREED ORDER\s.*The Department of Financial|This Court having|AGREED.*ORDER\s*Petitioner|ORDER.*DOCUMENTS\s*Upon Motion|Upon agreement|AGREED ORDER.*CR 65).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:EMERGENCY.*DESIST|On February 8|On October 30|Upon motion of|Whereas the Complainant|INTRODUCTION|FINAL.*DISMISSAL\s*Respondents|NOTICE.*HEARING\s*The Respondents).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:FINAL.*GRANTING|1\.\sThe Petitioner|FINAL.*DESIST ORDER|EMERGENCY.*AND DESIST|AGREED ORDER\s*A\.|AGREED.*DISMISSAL|FINAL ORDER\s*A hearing).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?: AGREED ORDER\s*STATEMENT OF FACT|JOINT NOTICE.*REGISTRATION|VOLUNTARY.*INJUNCTION).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?i)^\s*AGREED ORDER|^\s*FINAL ORDER|^.*APPLICATION OF:|(?ism)^\s*COMMONWEALTH.*AND\s*ORDER\s*(?=YOUNG OIL)/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)^.*FINANCIAL INSTITUTIONS\s*PETITIONER|^.*Y, _|Serve:\s*(?:Paul W\.|Ruth Howell).*|^.*INSTITUTIONS COMPLAINANT\s*AGREED ORDER/, "")
        pdfText = pdfText.toString().replaceAll(/(?ism)(SECURITIES \(USA\))\s*5(\sLLC\.).*/, '$1$2')
        pdfText = pdfText.toString().replaceAll(/(?i)(ALLIED SYNDICATIONS.*)RESPONDENTS\s*(.*ENERGY GROUP)/, '$1 $2')
        pdfText = pdfText.toString().replaceAll(/(?ism)(?<=CATHOLIC UNITED)\s*(.*MARKET).*(NEUTRAL FUND)/, '$1 $2')
        pdfText = pdfText.toString().replaceAll(/(?<=DON HOWARD)(.*)\s*(.*)\s*(.*LEASING)/, '$1 $2 $3')
        pdfText = pdfText.toString().replaceAll(/(?ism),(\sNICOLAUS & COMPANY),\s*(INCORPORATED).*/, '$1 $2')
        pdfText = pdfText.toString().replaceAll(/(?ism)(MARKETS), \)\s*(LLC).*/, '$1 $2')
        pdfText = pdfText.toString().replaceAll(/(?<=FENNER) \)\s*(& SMITH INCORPORATED)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?<=First Financial Equity).*\s*(Corporation)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?s)(?<=Prichard d\/b\/a)\s(Genuine.*Opportunity).*/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?i)(?<=TREADSTONE LLC),\s*(.*OFFSET)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?i)(?<=Investment Corporation)\s*\)\s(.*Corporation)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?<=PRESIDENT OF)\s(.*INC\.)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?<=ADVISORY TEAM LLC)\s*(.*ADVISORS)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?<=VIRGINIA HIBBS &)\s(.*)\s(.*INC\.)/, ' $1 $2')
        pdfText = pdfText.toString().replaceAll(/(?<=SEAGRAVES and)\s*(.*)\s(.*PORATED\.)/, ' $1 $2')
        pdfText = pdfText.toString().replaceAll(/(DALE LOWE SR.)\s*(a\/k\/a.*LOWE SR.)/, '\n$1 $2')
        pdfText = pdfText.toString().replaceAll(/(?<=Markwell President)\s*(.*Exploration Inc\.)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?s)(?<=P\. ROGERS),.*(Director).*(ROGPEX)/, ' $1 $2')
        pdfText = pdfText.toString().replaceAll(/(?<=of Regions)\s*\)\s*(Financial Corporation),/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?<=AKA CHASE)\s*|(?s)NOTICE OF.*REGISTRATION/, " ")
        pdfText = pdfText.toString().replaceAll(/(?i)(?:One Liberty Plaza).*|689 Winterhill Lane\s.*40509|(?sm)(?:165 Broadway|COMMISSIONER'S FINDINGS|ORDER OF THE EXECUTIVE).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)3719 Sulphur Well.*42214-8207|(?sm)(100C Reynolds.*AGREEMENT AND|Agreed Order\/Settlement.*)/, "")
        pdfText = pdfText.toString().replaceAll(/1121\/2 Sevier.*\s.*\s.*40906|FINAL.*INJUNCTION\s*AND.*RELIEF|(?sm)each individually.*|(?sm)The Plaintiff,.*/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)FINDINGS.*RECOMMENDED ORDER|AGREED.*DESIST|RESTRAINING ORDER|SUMMARY.*REGISTRATIONS|(?:101 Indian|106 Danville).*\s.*Kentucky 40444/, "")
        pdfText = pdfText.toString().replaceAll(/(?sm)(?:The Commonwealth of Kentucky,|FINAL.*DESIST|the Department.*\("DFI"\)).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?is)(EnTema.*Rose).*\),(.* Energy Associates,).*(Westbrook.*Pennsylvania 3)\s*(.*Stan)\s*(.*Pamedis),\s(Bob)\s*(.*)/, '$1\n$2\n$3 $4 $5\n$6 $7')
        pdfText = pdfText.toString().replaceAll(/(?sm)7633 E\. 63x4.*rsyCLERK\s*Tulsa, OK 74133|(?s)ADMINISTRATIVE.*ORDER\s*Serve Wachovia.*/, "")
        pdfText = pdfText.toString().replaceAll(/Serve: Brian C.*\s.*\s.*(?:KY 40223|OK 74133)|7633 E\. 63rd.*\s.*OK 74133|9900 Corporate.*\s.*KY 40223|a wholly owned.*\s*Inc.*\s*.*\) KELSOE/, "")
        pdfText = pdfText.toString().replaceAll(/137 Park.*\s.*SECURITIES|3556.*\s.*Kentucky 42366/, "")
        pdfText = pdfText.toString().replaceAll(/\(USA\)/, "USA")
        pdfText = pdfText.toString().replaceAll(/6371 Richmond.*\s.*|SUMMARY ORDER.*|(?s)^\s*\).*CONSENT ORDER\s*(?=LPL)|1256 AUDUBON.*|The Department.*\("DFI"\).*|ORDER ADDING.*/, "")
        pdfText = pdfText.toString().replaceAll(/(?s)ADMINISTRATIVE CONSENT.*|(?:CRD# 1008601|\(CRD# 2237620|\(CRD# 16507|CRD # 4579793).*|CONSENT ORDER.*|ORDER OF CONTEMPT.*|STOP ORDER.*|607 2.*\s.*42602/, "")
        pdfText = pdfText.toString().replaceAll(/^\s*ORDER.*|^\s*SETTLEMENT.*|AMENDED.*|(?s)(?:^\s*\).*Proceeding|\)\s*FINDINGS OF.*)|(?s)FRANKLIN CIRCUIT.*(Defendants|2129 DEFENDANT)/, "")
        pdfText = pdfText.toString().replaceAll(/(?i)8143 New.*\s.*40222-5466|DEFAULT.*|(?s)(?:AUG 16 2007.*|FRANKLIN CIRCUIT.*CLERK|408 Talbott.*|undertaking.*|33012 East.*|\) EXEMPTION PURSUANT.*)/, "")
        pdfText = pdfText.toString().replaceAll(/(?i)\)\s*ORDER.*|137 Park.*|1676 Dutch.*\s.*42717.*|TO CEASE.*|616 Wellington.*\s.*|303 Oakley.*\s.*|HONORABLE THOMAS.*\s.*CI-00522|OPINION\s*.*REMANDING/, "")
        pdfText = pdfText.toString().replaceAll(/388 Greenwich.*\s.*10013\s*\)|(?<=Leon Jasper)\s*AGREED ORDER|PARTIAL AGREED.*|1287.*\s.*42103.*|(?s)(?:FINAL.*|sFaik!.*_\.|THE AGENT.*|AND ASSESSING.*|(?:AGREED|Agreed).*)/, "")
        pdfText = pdfText.toString().replaceAll(/(?:IMPOSING|REQUIRED|MANAGING|ORDER TO|MAY 1 1 2007|846 q701|MEMORANDUM|NOV 1 2007|APR 18 2007|OGCOF1 '13|"\s*ORDER|Findings of.*).*/, "")
        pdfText = pdfText.toString().replaceAll(/(?:JAN 1,6 2009|Director\s*.*RESPONDENT|RESPONDEN\sTS|DFI'S FINDING OF FACTS\s*1\.|JAN 17 2008).*|ORDER DENYING.*/, "")
        pdfText = pdfText.toString().replaceAll(/(?i)(?:individually|Personally|ENTER E%|4502 Highway.*\s.*|Serve: Ned B.*|SETTLEMENT.*\s.*|^V$|Serve:\s*|(?s)Agent for Service.*KY 42748)/, "")
        pdfText = pdfText.toString().replaceAll(/(?:1026 East.*\s.*|wholly.*of|President\/Director|Crossville.*38557|\).*)|a  REGIONS|a wholly owned|subsidiary of/, "")
        pdfText = pdfText.toString().replaceAll(/(?<=(?:;\sMORGAN|REGIONS FINANCIAL|REGIONS|BRIAN B\.))\s*(.*Inc.|CORPORATION|FINANCIAL CORPORATION|SULLIVAN)/, ' $1')
        pdfText = pdfText.toString().replaceAll(/(?<=OF KENTUCKY)\s*(.*INSTITUTIONS),\s*(.*SECURITIES).*/, ' $1 $2')
        pdfText = pdfText.toString().replaceAll(/(?<=Gregory Smith),.*\s*.*(Owner)\s*/, ' $1 ')
        pdfText = pdfText.toString().replaceAll(/(?<=BRUCE PAGE)\s*(.*INC\.),\s*(.*CO\.),\s*(.*COMPANY)/, ' $1 $2 $3')
        pdfText = pdfText.toString().replaceAll(/HIBBS & HIBBS/, 'HIBBS, HIBBS')
        pdfText = pdfText.toString().replaceAll(/STRA1 EGIES/, 'STRATEGIES')
        pdfText = pdfText.toString().replaceAll(/&\.J/, '& J')
        pdfText = pdfText.toString().replaceAll(/B\. D\./, 'B.D.')
        pdfText = pdfText.toString().replaceAll(/\s+RESPONDENT\nDi13\/A/, ' D/B/A ')
        pdfText = pdfText.toString().replaceAll(/(?s)^.*?2019-AH-0015.*/, "Respondent\nMARK THOMAS LAMKIN")
        pdfText = pdfText.toString().replaceAll(/(?s)^.*?2021-AH-00020.*/, "Respondent\nBLOCKFI LENDING LLC")
        pdfText = pdfText.toString().replaceAll(/(?s)NOTICE AND ORDER\s+RESCINDING.$/, "")
        pdfText = pdfText.toString().replaceAll(/(?i)[\)\(]/, "")

        return pdfText
    }

    def sanitizeHtml(def html) {

        html = html.toString().replaceAll(/(?i)href='(Documents\/Cleark Creek.*?081105\.pdf)'.*08\\/11\\/2005/, "")
        html = html.toString().replaceAll(/(?i)href='(Documents\/Burchett Oilco Inc.*?060105\.pdf)'.*06\/01\/2005/, "")
        html = html.toString().replaceAll(/(?i)href='(Documents\/US Petroleum.*?052005\.pdf)'.*05\/20\/2005/, "")
        html = html.toString().replaceAll(/(?is)<a href='#details72021'.*/, "")

        return html
    }

    def sanitizeName(def name) {
        name = name.toString().replaceAll(/^\s+/, "").trim()
        name = name.toString().replaceAll(/(?i)(?:^Director|RESPONDENT\.?|RESPONDENTS\.?|^SECURITIES|1\s*JUL.*|DEFENDANTS|DEFENDANT\.?|^AND ORDER)$/, "").trim()
        name = name.toString().replaceAll(/(?i)(?:^And\s|\sAnd$|^and$|^\d$|^\d\s|4'.*|\sRESPO\sI$|\s&$|,\sPRINCIPAL$|Process$|ORDER$|ENTERED$|CO-PETITIONERS$|\sPRESIDENT$|^DR\.|^MR\.)/, "").trim()
        name = name.toString().replaceAll(/[,;]$|^V$|, a$|^\.$|(?i)\sET AL\.?/, "").trim()
        name = name.toString().replaceAll(/(?i)^Dr\.?\s|\s(?:Jr\.?|Sr\.?)$/, "").trim()
        name = name.toString().replaceAll(/\sLI C/, " LIC").trim()
        name = name.toString().replaceAll(/^icxx.+|CASE NO.*|3556 Oaklane Drive/, "").trim()
        name = name.toString().replaceAll(/BAFtNETT/, "BARNETT").trim()
        name = name.toString().replaceAll(/ASSEST/, "ASSET").trim()
        name = name.toString().replaceAll(/[\\_]+/, "").trim()
        name = name.toString().replaceAll(/^(?:AGEELIIDEDIE)$/, "").trim()
        name = name.toString().replaceAll(/\s+/, " ").trim()
        return name.trim()
    }

    def sanitizeAlias(def name) {
        name = name.toString().replaceAll(/\.$/, "").trim()
        name = name.toString().replaceAll(/"/, "").trim()
        name = name.toString().replaceAll(/(?s)\s+/, " ").trim()
        return name.trim()
    }

    def pdfToTextConverter(def pdfUrl) {
        List<String> pages = ocrSpaceReader.getText(pdfUrl)
        return pages
    }


    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = false, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}