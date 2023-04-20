package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.util.StringUtil

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Finra_Arbitration_Awards_Twenty_TwentyTwo script = new Finra_Arbitration_Awards_Twenty_TwentyTwo(context)
script.initParsing()


class Finra_Arbitration_Awards_Twenty_TwentyTwo {
    final addressParser
    final entityType
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final ScrapianContext context

    FileOutputStream file = new FileOutputStream("/home/maenul/Documents/Finra.txt")
    PrintStream printStream;
    def url = 'https://www.finra.org/arbitration-mediation/arbitration-awards-online?aao_radios=all&search=&field_case_id_text=&field_core_official_dt%5Bmin%5D=08/20/2022&field_core_official_dt%5Bmax%5D=12/31/2022&field_document_type_tax%5B4224%5D=4224&field_forum_tax=All&field_special_case_type_tax=All&page='
    def prefixPdfUrl = 'https://www.finra.org'
    def testUrl = "https://www.finra.org/sites/default/files/aao_documents/21-02234.pdf"

    Finra_Arbitration_Awards_Twenty_TwentyTwo(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        entityType = moduleFactory.getEntityTypeDetection(context)

        printStream = new PrintStream("/home/maenul/Documents/Finra.txt")
       // addressParser.updateCountries()
        this.context = context
    }

    def initParsing() {
        handleData(url, 0)

        /*def pdfText = sanitizePdfText(pdfToTextConverter(testUrl))
        println pdfText
        captureData(pdfText, "02/02/2022", "08-9876")*/
    }



    def handleData(def link, int lastPage) {

        def mainUrl = link + lastPage
        def html = invokeUrl(mainUrl)
        capturePdfAndDate(html)

        def lastPageTerminateMatcher

        int nextPageNum
        def nextPageUrl
        def nextPageNumMatcher = html =~ /class="page-link">(\d+)<\//
        while (nextPageNumMatcher.find()) {
            nextPageNum = nextPageNumMatcher.group(1).toInteger()

            if (nextPageNum < lastPage) {

            } else if (nextPageNum == lastPage) {
                nextPageNum = nextPageNum + 1
                nextPageUrl = link + nextPageNum

                html = invokeUrl(nextPageUrl)

                if ((lastPageTerminateMatcher = html =~ /<em>(No.*Found)<\/em>/)) {
                    if (lastPageTerminateMatcher[0][1].toString().trim().equals("No Results Found")) {
                        return
                    }
                } else {
                    capturePdfAndDate(html)
                }
                lastPage = nextPageNum
            } else if (nextPageNum > lastPage) {
                nextPageUrl = link + nextPageNum
                html = invokeUrl(nextPageUrl)

                if ((lastPageTerminateMatcher = html =~ /<em>(No.*Found)<\/em>/)) {
                    if (lastPageTerminateMatcher[0][1].toString().trim().equals("No Results Found")) {
                        return
                    }
                } else {
                    capturePdfAndDate(html)
                }
                lastPage = nextPageNum
            }
        }
        return handleData(link, lastPage + 1)
    }

    def duplicate = [] as Set

    def capturePdfAndDate(html) {
        html = html.toString().replaceAll(/(?s)<thead.*?>.*?<\/thead>/, "")
        def separatorRegex = /(?s)<tr>.*?<\/tr>/
        def separatorMatcher = html =~ separatorRegex

        def pdfCount = 0 as int
        while (separatorMatcher.find()) {
            html = separatorMatcher.group()
            handleRow(html)
            pdfCount++
        }
    }

    def handleRow(def row) {
        def pdfDateAddressRegex = /(?is)document\-file\-media"><a href="(.*?\.pdf).*?>(\d+\-\d{3,6})<\/a>.*?Hearing Site: <\/span>.*?<.*?(\d{2}\/\d{2}\/\d{4})/
        def pdfDateAddressMatcher = row =~ pdfDateAddressRegex
        if (pdfDateAddressMatcher.find()) {
            def pdfUrl = prefixPdfUrl + pdfDateAddressMatcher.group(1)
            def text
            text = pdfToTextConverter(pdfUrl)

            def remark = pdfDateAddressMatcher.group(2)
            def date = pdfDateAddressMatcher.group(3)
            printStream.println(pdfUrl)
            captureData(text, date, remark)
        } else {
            def pdfDateAddressRegex1 = /(?is)document\-file\-media"><a href="(.*?\.pdf)".*?>(\d+\-\d{3,6})<.*?(\d{2}\/\d{2}\/\d{4})/
            pdfDateAddressMatcher = row =~ pdfDateAddressRegex1
            if (pdfDateAddressMatcher.find()) {
                def pdfUrl = prefixPdfUrl + pdfDateAddressMatcher.group(1)
                def text = pdfToTextConverter(pdfUrl)
                def remark = pdfDateAddressMatcher.group(2)
                def date = pdfDateAddressMatcher.group(3)
                captureData(text, date, remark)
            }
        }
    }

    def captureData(text, date, remark) {

        text = sanitizePdfText(text)
        def entitiesData
        def entitiesDataMatcher

        def entityNameList
        if ((entitiesDataMatcher = text =~ /(?s)(?:\s(?:vs|Vs|VS|v)\.[\s\n]|Claimant v\.|CLAIMANT|(?:\s{2,}|[\r\n]+)(?:vs\.|V\s*\.|v\.\s*\)?|v\$\.|v\.))(.*?)[\"\(\”]*(?:Respondent\.?[s]?|[R]espondent[s\.]*?|RESPONDENTS|Hearing|\Z)[\"\)\”]*/)) {
            entitiesData = entitiesDataMatcher[0][1]
            entityNameList = handleEntitiesData(entitiesData)
        } else if ((entitiesDataMatcher = text =~ /(?s)[\(]Claimant[s]?[\)](.*)[\(]Respondent[s]?[\)]/)) {
            entitiesData = entitiesDataMatcher[0][1]
            entityNameList = handleEntitiesData(entitiesData)
        } else if ((entitiesDataMatcher = text =~ /(?:Respondent[se]*?)(?:\s*|\s\-\s)Hearin(?:a|g|o)\s+(?:[lL]ocation|Site):.*\s+(?s)(.*?)(?=(?:Nature|Namce) of (?:die|the) Dispute:|\Z)/)) {
            entitiesData = entitiesDataMatcher[0][1]
            entityNameList = handleEntitiesData(entitiesData)
        } else if ((entitiesDataMatcher = text =~ /(?s)(?:Respondent[se]?)(.*?)(?=Hearin(?:g|q|o)\s+(?:Site[s]?|[lL]ocation)[:;\-]|(?i)(?:MATURE|NATURE)\s(?:QF|OF)\s?(?:THE)?\s(?:PI§P»TE\s|DISPUTE)|\Z)/)) {
            entitiesData = entitiesDataMatcher[0][1]
            entityNameList = handleEntitiesData(entitiesData)
        } else if ((entitiesDataMatcher = text =~ /(?s)(?:V\s\.|(?i)\sv\s|v[\.;]|vs\.?|v\$\.|VS\.?)(.*?)(?:Respondent[s\.]*?|\(?Respondent[s\.]?\)?|(?i)NATURE OF THE DISPUTE|Hearing|\Z)/)) {
            entitiesData = entitiesDataMatcher[0][1]
            entityNameList = handleEntitiesData(entitiesData)
        } else if ((entitiesDataMatcher = text =~ /\s*Hearing Site:.*\s+(?s)(.*?)(?=Respondent\.?[s]?|\Z)/)) {
            entitiesData = entitiesDataMatcher[0][1]
            entityNameList = handleEntitiesData(entitiesData)
        }

        def aliasMatcher
        entityNameList.each({ entityName ->
            def alias
            def aliasList = [] as List
            entityName = sanitizeEntityName(entityName)
            if (!entityName.toString().isEmpty()) {
                if (entityName =~ /f\/k\/a|d\/b\/a|a\/k\/a[\/]?|(?i)n\/k\/a[\/]?|\sdba\s|\/(?=[A-Z][a-z]{2,})|alias|D\/B\/A|[\(\s]aka\s|\snka\s|f\/lc\/a/) {
                    aliasList = entityName.split(/f\/k\/a|d\/b\/a|a\/k\/a[\/]?|(?i)n\/k\/a[\/]?|\sdba\s|\/(?=[A-Z][a-z]{2,})|alias|D\/B\/A|[\(\s]aka\s|\snka\s|f\/lc\/a/).collect({
                        it.toString().trim()
                    })
                    entityName = aliasList[0]
                    aliasList.remove(0)
                } else if ((aliasMatcher = entityName =~ /([\"\(].*[\)\"])/)) {
                    alias = aliasMatcher[0][1]
                    aliasList.add(alias)
                    entityName = entityName.toString().replaceAll(/[\"\(]$alias[\"\)]/, "")
                }

                entityName = entityName.toString().replaceAll(/[\)\(\}]/, "").trim()
                entityName = entityName.toString().replaceAll(/\s+/, " ").trim()
                entityName = entityName.replaceAll(/(?i)^Dr\.?\s|(?:Jr[\.]?|Sr\.?)$|^(?:[a-z])$/, "").trim()
                entityName = entityName.replaceAll(/SLASH/, "/").trim()
                entityName = entityName.replaceAll(/(?:\*)$/, "").trim()
                println entityName
                printStream.println(entityName)
                createEntity(entityName, date, remark)
            }
        })
        println("=============================================================================")
        printStream.println("\n=============================================================================")
    }

    def handleEntitiesData(def entitiesData) {

        entitiesData = sanitizeEntitiesData(entitiesData)

        def entityName
        def entityNameList = [] as List
        def entityNameMatcher

        if ((entityNameMatcher = entitiesData =~ /^(.*){0,1}$/)) {
            entityName = entityNameMatcher[0][1].toString().trim()

            if (entityName =~ /\s+and\s|[,;]|(?<=\sInc\.|\sINC\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))|(?<=LLC|Inc\.|Ltd\.)\s*vs.\s*(?=Abraham)/) {
                entityNameList = entityName.toString().split(/\s+and\s|(?<=\sInc\.|\sINC\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))|[,;]|(?<=LLC|Inc\.|Ltd\.)\s*vs.\s*(?=Abraham)/).collect({
                    it.trim()
                })
            } else if (entityName =~ /(?<=& Co\.)\s*(?=UBS)/) {
                entityNameList = entityName.toString().split(/(?<=& Co\.)\s*(?=UBS)/).collect({ it.trim() })
            } else {
                entityNameList.add(entityName)
            }
        } else if (entitiesData =~ /[,;]/) {
            entitiesData = entitiesData.toString().replaceAll(/\s+/, " ").trim()
            entityNameList = entitiesData.toString().split(/[,;]|(?<=LLC|Inc\.|\sINC\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z\']+|[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))|\s+and\s|(?<=Corp\.)\s(?=National)/).collect({
                it.trim()
            })
        } else {
            def nameList = [] as List
            entityNameMatcher = entitiesData =~ /(.*)/
            while (entityNameMatcher.find()) {
                entityName = entityNameMatcher.group(1).trim()
                if ((entityName =~ /\sand\s|(?<=\sInc\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))/)) {
                    nameList = entityName.toString().split(/\s+and\s+|(?<=\sInc\.|LLC|Corp\.|\sSr\.|\sJr\.|Incorporated)\s(?=(?:[A-Z][a-z0-9]{2,}|[A-Z][A-Z0-9]{2,}))/).collect({
                        it.trim()
                    })
                    nameList.each { entityNameList.add(it.toString().trim()) }
                } else {
                    entityNameList.add(entityName)
                }
            }
        }
        return entityNameList
    }


    //def createEntity(def name, def aliasList, def address, def eventDate, def entityUrl) {
    def createEntity(def name, def eventDate, def remark) {

        def entity = null

        def description = 'This entity appears on the Financial Industry Regulatory Authority’s list of Arbitration Awards.'

        if (!name.toString().equals("") || !name.toString().isEmpty()) {
            def entityType = detectEntity(name)
            entity = context.findEntity(["name": name, "type": entityType])
            if (!entity) {
                entity = context.getSession().newEntity()
                entity.setName(name)
                entity.setType(entityType)
            }

            if (!remark.toString().isEmpty()) {
                entity.addRemark("Case Number: " + remark)
            }

            ScrapeEvent event = new ScrapeEvent()
            event.setDescription(description)

            if (eventDate) {
                eventDate = context.parseDate(new StringSource(eventDate), ["MM/dd/yyyy"] as String[])
                event.setDate(eventDate)
            }

            entity.addEvent(event)
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            scrapeAddress.setCountry("UNITED STATES")
            entity.addAddress(scrapeAddress)
        }
    }

    def detectEntity(def name) {
        def type
        if (name =~ /^(?:Bruno|Budwal|Calardo|Carapella|Christakos|Fusco|Jean|Kerr|Weiss|Michael|Miller|Moro|Quinzi|Torrillo|Torres|Geake|Lambert|Locy|Pedersen|Whopper|Wilhelm)$/) {
            type = "P"
        } else if (name =~ /^(?:Schifano|Caruso|Daigneau|Hornberger|Johnson|Juarez|Milano|Ohman|Halsne|Parmigiani|Peter|Shea|Stapleton|Stephens|Yasnis|Smutko|Fifer|Cobby)$/) {
            type = "P"
        } else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:PLLC$|\sLLC$|LLC$|LLC\.$|Inc\.$|L\.L\.C\.$|Corporation$|INC$|Corp\.$|Corp$|INC\$|Company$|LP$|LLP$|America$|Gallery$|L\.P\.$|Services, LLC$|Services$|Co\.$|(?:Schwab|Comerica)\sBank$)/) {
                    type = "O"
                }
            } else if (name =~ /(?i)(?:Arms$)/) {
                type = "P"
            }
        }
        return type
    }

    def sanitizePdfText(def pdfText) {
        pdfText = pdfText.toString().trim()
            .replaceAll(/[\“\”]/, "\"")
            .replaceAll(/[\.]+/, ".")
            .replaceAll(/(?i),\s*(?=L\.L\.P|L\.L\.C|Inc\.?|LLC|LLLP|JR\.?|SR\.?|Rose|a\/Ida|LLP|ET|LTD|FENNER|PIERCE|a\/k\/a|AKA|LP|III|& Smith|Berland|Sailer|IV|II|N\.A\.)/, " ")
            .replaceAll(/(?i),\s*(?=VIDEO|P\.S\.C\.,|LIMITED|SINCLAIRE|Sachs|James|Ill|Max|Esq\.|Ins\.|Keegan|L P|Titus|P C|Quick|Buckman|Corp\.|d\/b\/a|Lufkin)/, " ")
            .replaceAll(/(?i),\s*(?=lnc\.|Individually|LYNCH|PLC|of Puerto Rico|hereinafter|Fanner|Trustee|L L C\.|Smith|N\.C\.|L\.P\.|,?\s?n\/k\/a|NA|Baker)/, " ")
            .replaceAll(/(?i),\s*(?=Larry|Mather|L\.C\.|Komara|as successor|Piven|Vogel|Banks|Atkinson|Williams|Stearns|Pierchalski|Kelly|Weedon &|Kottler|Unell|Pearlstein)/, " ")
            .replaceAll(/(?i),\s*(?=Tenner|Services|IT|Wick|Grubman|& Sons|Edwards|Feimer|f\/n\/a|Lee|Florance|WiMte|Steams|f k\.a\.|k\/n\/a|a division|Thomas|Copeland)/, " ")
            .replaceAll(/(?i),\s*(?=Wierenga|Langone Jr\.|TV|LUC|Watts Inc\.|a Georgia|a subsidiary|also known|now known|as\s+Successor|ffk\/a|successor in|tfkte)/, " ")
            .replaceAll(/,\s*(?=(?:Rothschild|Oxford|Phillips|Strand|Thalman|Morgan|Dyer|Sterling|Manin|Block|Schroeder|Nicolaus|Grey|McKenney|Hueglin) &|King|Nussbaum|Dawkins|PEABODY|A Division|Lemon and)/, " ")
            .replaceAll(/,\s*(?=(?:Townsend|Fisher) &|A\s+Limited Partnership|a California)/, " ")
            .replaceAll(/(?i)(?<=Winslow|Lucien|Miller|S\.A\.|Stephen J|Stifel|Adams|Hess),\s*(?=Moore|Hess|Evans|Stirling|Johnson|Lehman|Taormina|Nicholas)/, " ")
            .replaceAll(/(?i)(?<=T|Kirkpatrick|Smith|Lynch|Luther|Cadaret|Gould),\s*(?=Barney|Pettis|Polian|Pierce|Merrill|Grant|Ambroson)/, " ")
            .replaceAll(/(?s)Awards are rendered.*/, "")
            .replaceAll(/(?s)(?<=LLC|Inc\.)\s*.*Awards are rendered.*/, "")
            .replaceAll(/___{2,}.*/, "")
            .replaceAll(/(?s)FINRA.*20-02657.*/, '\nRespondent\nMerrill Lynch Pierce Fenner & Smith Inc.')
            .replaceAll(/(?s)FINRA.*19-02519.*/, '\nRespondent\nCommonwealth Financial Network and Robert James Messett d/b/a Messett Financial')
            .replaceAll(/(?s)FINRA.*20-01582.*/, '\nRespondent\nCharles Schwab & Co. Inc. and James F. Albert')
            .replaceAll(/(?s)FINRA.*17-03260.*/, '\nRespondent\nMarc Alan Ferries, James O. Sturdivant')
            .replaceAll(/(?s)FINRA.*19-03088.*/, '\nRespondent\nInteractive Financial Advisors Inc.,Joanne Woiteshek and Rick Peterbok')
            .replaceAll(/(?s)FINRA.*18-02393.*/, '\nRespondent\nMichael Aaron Manaster')
            .replaceAll(/(?s)FINRA.*20-03832.*/, '\nRespondent\nE*Trade Securities LLC')
            .replaceAll(/(?s)FINRA.*17-03115.*/, '\nRespondent\nJoseph Walter Modero')
            .replaceAll(/(?s)FINRA.*19-00926.*/, '\nRespondent\nScott Richard Reynolds')
            .replaceAll(/(?s)FINRA.*20-00922.*/, '\nRespondent\nPershing LLC')
            .replaceAll(/(?s)FINRA.*17-03513.*/, '\nRespondent\nSunset Financial Services Inc.')
            .replaceAll(/(?s)FINRA.*20-02985.*/, '\nRespondent\nChase Investment Services Corp.')
            .replaceAll(/(?s)FINRA.*21-00863.*/, '\nRespondent\nKestra Investment Services LLC d/b/a NFP Securities Inc.')
            .replaceAll(/(?s)FINRA.*19-00961.*/, '\nRespondent\nA.G.P. d/b/a Alliance Global Partners d/b/a Euro Pacific Capital Inc.,')
            .replaceAll(/(?s)FINRA.*19-03552.*/, '\nRespondent\nPeachCap Securities Inc.,David Harrison Miller, Shelley Long Eddy')
            .replaceAll(/(?s)FINRA.*19-01459.*/, '\nRespondent\nCharles Schwab & Co. Inc. ,Charles Schwab Futures Inc.')
            .replaceAll(/(?s)FINRA.*20-00151.*/, '\nRespondents\nWorden Capital Management LLC, Matthew Gates, Brandon Pflaum,Cesar Pozo, Miguel Cabarcas')
            .replaceAll(/(?s)FINRA.*19-02176.*/, '\nRespondents\nCharles Schwab & Co. Inc., David Pathe')
            .replaceAll(/(?s)FINRA.*19-00509.*/, '\nRespondents\nUBS Financial Services Inc.,' +'Michael Martin, and' + 'Patrick Donnelly')
            //.replaceAll(/(?s)FINRA.*19-03741.*/, '\nRespondents\nJoseph Stone Capital L.L.C., James Vincent Pardy, Sebastian Wyczawski, Garrett Gaylor, David Khezri')
            .replaceAll(/(?s)FINRA.*18-00566.*/, '\nRespondents\nJames C. Scanlon')
            .replaceAll(/(?s)FINRA.*19-00001.*/, '\nRespondents\nCOR Clearing LLC')
            .replaceAll(/(?s)FINRA.*21-02849.*/, '\nRespondents\nWells Fargo Clearing Services LLC')
            .replaceAll(/(?s)FINRA.*21-01614.*/, '\nRespondents\nJanney Montgomery Scott LLC')
            .replaceAll(/(?s)FINRA.*21-02022.*/, '\nRespondents\nBoenning & Scattergood Inc., Robert Sean Crotty')
            .replaceAll(/(?s)FINRA.*20-04126.*/, '\nRespondents\nCharles Schwab & Co. Inc., Pinnacle Associates Ltd.')
            .replaceAll(/(?s)FINRA.*19-02084.*/, '\nRespondents\nCetera Investment Services LLC, Smansa Chu')
            .replaceAll(/(?s)FINRA.*19-01585.*/, '\nRespondents\nMorgan Stanley, Victor Tsutsumi,Nicolas Pablo Lumermann')
            .replaceAll(/(?s)FINRA.*: 21-01144.*/, '\nRespondents\nMorgan Stanley, Yuji Fujii, Anthony Vincent Valente,Ronnie Guidone')
            .replaceAll(/(?s)FINRA.*17-02804.*/, '\nRespondents\nMichael Anthony Schiavello, Vasilios Takos, George Anthony Tapikenis')
            .replaceAll(/(?s)FINRA.*19-03167.*/, '\nRespondents\nEdward Jones, and John C. Downey')
            .replaceAll(/(?s)FINRA.*21-02234.*/, '\nRespondents\nOppenheimer & Co. Inc.')
            .replaceAll(/(?s)FINRA.*21-00526.*/, '\nRespondents\nAndrew James Stout')
            .replaceAll(/(?s)1<EB\SGF>BA!2<DH>9<E.*/, '\nRespondent\nAXA Advisors LLC')
            .replaceAll(/(?s)FINRA.*20-00954.*/, '\nRespondent\nRockwell Global Capital LLC, Bruce Guarino, Douglas Guarino')
            .replaceAll(/(?s)Clerk of the.*2021-007635.*/, '\nRespondent\nTD AMERITRADE INC, MATT FISCHER, NEIL S. BARITZ, JUDGE DANIEL MARTIN')
            .replaceAll(/(?is)(?:C a s e|3E|\/SE|TASK|CASE|CAfiK|CASK|Casa)[\s\-]*(?:SUMMARY|INFORMATION|Sunmiary\:|Sununary\.|S u m m a r y|SUWARY|g—MY).*/, "")
            .replaceAll(/(?is)(?:Nature|tSlATURE) of\s(?:(?:the|tiie) Dispute|DISPUTE).*/, '')
            .replaceAll(/(?is)^.*?(?=(?:VS|v)\.?[\s\n]*Respondent|Name of Respondent[s]?)/, '')
            .replaceAll(/(?is)(?:RFrPFFsnyT4TTON|REPRFSF\.iVTATfnN|IMflPRFSENTATION|RBPRgSBMTATION|REPRESETSTA\*yiON|REFRESENtAtlQN|REPRESENTATION|REPRESENTATTON)\s(?:OF|Op)\s(?:PARTIES|PARTTRS|PARTTFS|PARTjfES).*/, '')
            .replaceAll(/(?is)(?:REPRESENTATION|PRESENTATION|RKPRESKNTATTON|REPRFSF\.iVTATfnN)\s(?:OF|Op)\s(?:PARTIES|PARTIK\.S|PARTIF\^).*/, '')
            .replaceAll(/(?is)(?:PFPRF\.SFNTATUW|ng Siftv|RPPRRWNTATTON|(?<=LLC)\s*REPRESENTATION OF PARTIES\:).*/, "")
            .replaceAll(/(?s)(?:INTRODUCTION|ORDER VACATING ARBITRATION|Per the order of|THE DISPUTE|David Baiocco and Kathleen|ATTORNEYS\:|Rrst scheduiad|PF\.PPF\.S1T\.NT ATTHNf|pir\.Piir\.CT\.NT).*$/, '')
            .replaceAll(/(?s)(?:WHEREAS, Claimants|R F y \. s f f \. i TTn|OF PRTTFS|REPRE\!|Rqtresentation|REPRESENTATTON f}T\( r\^\^W\^|RBPRESEMTAT\^OH|RBPRgSBMTATION|REPRESETSTA\*yiON).*$/, '')
            .replaceAll(/(?s)(?:AWARD\s*(\bo\b|Romaine)|REPRESENTATION|Claimant Subrais&ion|REFRESENtAtlQN|BEPRRSEWTATTQW|For claimants Albert B|File #94\-01).*$/, '')
            .replaceAll(/(?s)(?:SUMMARY OF ISSUES|Name Public\/Industry|This matter was|In a claim filed|CF PB\*crir\%3|For Claimants PnTTiIJTVl|Statement of.*:|QSJlBBSBaSM).*$/, '')
            .replaceAll(/(?s)(?:Pursuant to a).*$/, '')
            .replaceAll(/(?i)\s*\(s\s*\)/, 's')
            .replaceAll(/(?i)Case Number: \d{2}-\d{5}/, '')
            .replaceAll(/"Cobby" Shapira/, 'Shapira f/k/a Cobby')
            .replaceAll(/R\^gpfinrfuntii Wmmng Situ|PftaprmfenTa.*D\sC|RraprmHpnt.*fte|(?<=Aubrey\s)\*\-(?=\sDallas)/, "Respondent Hearing Site")
            .replaceAll(/Hem-ing Site!|ff earing Site:|ng Sitft|\bring\b|Siie:|Hflaring Sitft\-|Hearino_Site:|Tsarina Sitp\'|ing \.Site\.:/, "Hearing Site:")
            .replaceAll(/Number\- 03-08862|(Respondent|RespoDdeDt)\(s\):|RftspnnHent|Pagp NiimhpT" 01-05485|Resoontfents|Respon\(ient/, 'Respondents')
            .replaceAll(/PftaprmfenTa.*D\sC|itf \* Milwaukee, Wisconsin|Ree\s|Resnondent|Reondents|Resuondents|Ftospondents|RftspnnHent/, 'Respondents')
            .replaceAll(/(?i)\"([A-Z0-9 ]+)\"/, '$1')
            .replaceAll(/Date\s?(?:Fi led|Filed|filed|R I e d|F i led)\s*\:?|as Personal|(?<=Clark)\s*In the Matter of|(?<=(?:Jr\.|Inc\.,))\s*(?:REPRESENTATION|Respondent\.)|v.*\n98\-00289|Arbitration No\.\n97\-00267/, ' And;')
            .replaceAll(/RFrPFFsnyT4TTON|REPRFSF\.iVTATfnN|IMflPRFSENTATION|RBPRgSBMTATION|REPRESETSTA*yiON|REFRESENtAtlQN/, 'REPRESENTATION')
            .replaceAll(/(?<=Thomas Lomas)\s(?=For Claimant\:)/, '\nREPRESENTATION\n')
            .replaceAll(/(?i)Nature.*dispute:/, 'Nature of the Dispute:')
            .replaceAll(/(?i)(?<=01\-01\s159)\s*(?=Edward D\. Jones)|Rum «1 > lents/, '\nRespondents ')
            .replaceAll(/(?i)ClaiTnr«nt/, 'Claimants')
            .replaceAll(/(?i)(vs\.) (?:\d{2}\-\d{5})/, '')
            .replaceAll(/(?i)Resnon|And;\nREPRESENTATION|Resoondents|RBipondenl[\|]i\)|T\?f<jpnnHpntS|ftp\.spnndp\.nts/, 'Respondent')
            .replaceAll(/(?is)^.*(?=Respondents\s*\\/\s*Counter[\-]*Claimants)/, '')
            .replaceAll(/(?<=Respondent)\s*\/\s*Counter\-Claimant/, '')
            .replaceAll(/(?i)(?<=Respondents)\s*\/\s*Counter[\-]*Claimants|Dated 12\/5\/18/, '')
            .replaceAll(/(?i)Trad\^star/, 'Tradestar')
            .replaceAll(/(?i)(?:v\.|and)\s(?:No|Case Number)[\:\.]?\s\d{2}\-\d{5}/, 'v.')
            .replaceAll(/2\/y>\^|94\-(?:02828|02813)|(?s)Consolidated with.*?(?:Respondents)/, '')
            .replaceAll(/Case NO\.\-Q\-1\-04978|a Member of Travelers Group|(?s)98\-01917.*/, '')
            .replaceAll(/(?s)(?:\(s\):\s*Frances Chlapowski.*|(?<=Scott Paul)\s*Respondents).*/, '')
            .replaceAll(/(?is)(?:Attomeys[\:\;]|Attorneys[;\:]|v\.\s*Cross\-Respondent|Jn or about).*/, '')
            .replaceAll(/(?s)(?:Counter Claimant\s\nDavid Pathe|Cross Claimant\nRonnie Guidone|(?<=Florida)\n\s*vs\.\nThird-Party).*/, '')
            .replaceAll(/(?<=Services)\.(?=\nCitizens)/, '')
            .replaceAll(/(?s)FINRA Dispute Resolution Services\s+Arbitration No\. 18-03614.*Award Page 2 of 9/, '')
            .replaceAll(/f\/n\/a|(?<=Silverman) individually and as Trustee of|, as a Control Person of\s+(?=Corinthian)/, ' f/k/a ')
            .replaceAll(/(?i)\"\/\^7\/f1|1\?17\?\s{2}/, '')


        return pdfText.trim()
    }

    def sanitizeEntitiesData(def entities) {
        entities = entities.toString().trim()
            .replaceAll(/(?i)\"([A-Z\.\’ ]+)\"/, '$1')
            .replaceAll(/(?i)(Gerds)\"/, '$1')
            .replaceAll(/\s*[\r\n]+\s*(?=Reynolds|International|Stanislau|John Murphy)/, " ")
            .replaceAll(/(?<=(?:Jon|Lorraine|Daniel)\s[A-Z]\.|FINANCIAL|EARL|National|formerly known|Garrett|Fenner)\s*[\r\n]+/, " ")
            .replaceAll(/(?<=\s[A-Z][\.\-]|A\.G\.|Prudential|Wachovia|Services|Fenner|Smith|Capital|First|Stanley)\s*[\r\n]+/, " ")
            .replaceAll(/(?<=a\/k\/a|Linda|Citicorp|Edward Alan|James Harry|Stanley Dean|Salomon|Wolff|Amro|John|Ray|Henry)\s*[\r\n]+/, " ")
            .replaceAll(/(?<=interest to|interest|Caroline|CIBC|American|TD|Trust for|known as|liability)\s*[\r\n]+/, " ")
            .replaceAll(/(?<=Securities|Morgan)\s*[\r\n]+(?=Corp\.|Stanley|Co\.)/, " ")
            .replaceAll(/(?i)(?<=f\/k\/a|d\/b\/a|n\/k\/a|a\/k\/a)\s*[\r\n]+/, " ")
            .replaceAll(/(?i)\s*[\r\n]+\s*(?=f\/k\/a|d\/b\/a|n\/k\/a|a\/k\/a|f\/n\/a|New York|Corp\.)/, " ")
            .replaceAll(/(?i)\s*[\r\n]+\s*(?=doing business as|successor|Fenner|Of Shu|Caldbick|Sica|Rauscher|Anderson|B\. Elsoffer|Demiranda)/, " ")
            .replaceAll(/(?i)\s*[\r\n]+\s*(?=Harold|[A-Z]\. (?:Yoo|Cunningham|Creadon)|McKenna|Services|known as|and Wheat|Stepanek|Alers|the United)/, " ")
            .replaceAll(/(?i)\s*[\r\n]+\s*(?=Samo|DiGeronimo|Licata|Constantine|P\. Vanderzee|Grubman|Associates|Birmingham|Limited Partnership)/, " ")
            .replaceAll(/(?i)(?<=[A-Z][a-z]{3,10} [A-Z]\.)\s*[\r\n]+/, " ")
            .replaceAll(/(?i)(?<=(?:Corporation|Inc\.))\s+(?=d\/b\/a|f\/k\/a)/, " ")
            .replaceAll(/(?i)(?<=Mutual|Investor|Global|Wealth|Target Maturity|Taryn Lynn|Dimitrios|Division of|Services of)\s*[\r\n]+/, " ")
            .replaceAll(/(?i)(?<=Rainey|Farmers|alias|Edwards|Franklin|Samuel|Sagepoint|Fargo|America A|Holdings|BB&T)\s*[\r\n]+/, " ")
            .replaceAll(/(?i)(?<=Todd|Joseph|certain|Janney|Merrill|dissolved Florida|Stearns|Lynch|Robert|Armodios|Hank|Robinson)\s*[\r\n]+/, " ")
            .replaceAll(/(?i)(?<=Syndicate|Network for|CRT|Fernandez-|Ellwood|William|MML|of First of|midwest|Corinthian)\s*[\r\n]+/, " ")
            .replaceAll(/(?i)(?<=Raymond|(?<!(?:[A-Z])\.)\sJordan)\s*[\r\n]+/, " ")
            .replaceAll(/\s*[\r\n]+\s*(?=Group|Company|as successor|Connelly|Donovan|South Inc\.|Retirement|Bernstein|Connection|Equities|Corporation)/, " ")
            .replaceAll(/\s*[\r\n]+\s*(?=Fisher|Asiel|J\. Wertheimer|Hooper|Kuhlman|Zilbersher|of the United|Gruss|Lehman|of Puerto Rico|Rico)/, " ")
            .replaceAll(/(?<=Fenner|Agee|Individual|Feltl|Tulcin|Bank|Lufkin|Quick|Robert|Payne)\sand\s(?=Aline|Trust|Smith|Leach|Institutional|Company|Wolf|Jenrette|Reilly)/, " AND ")
            .replaceAll(/(?<=Oppenheimer|Sachs|Fred L\. Dowd|Edwards|James|Morrison|Schwab|Gruntal|Nicolaus|Baird)\sand\s(?=Sons|Co\. Inc\.|Company|Co\.|Associates)/, " AND ")
            .replaceAll(/(?<=Wescott|Fahnestock|Little|Kinnard|Donaldson|Bruder|Brown|Donnelly|Fargo|Dewitt|Stern)\s+and\s+(?=Sons|Co\. Inc\.|Company|Co\.|Associates)/, " AND ")
            .replaceAll(/(?<=White|Life|Research|Services|Monschein|Insurance|Turner|Gant)\sand\s(?=Sons|Co\. Inc\.|Company|Co\.|Associates|Annuity|Management|Trading)/, " AND ")
            .replaceAll(/(?<=John|Scott|Dave|Robert|King|Evans|Fotis|Houston|Frederick)\s+and\s+(?=Georgina|Elaine|Judy|Sharon|Elizabeth|Trudy|Anna|Members|Crocker|Michael|Linda)/, " AND ")
            .replaceAll(/(?<=Joseph|Tile|Timothy|Rodney|Hunt|Patrick|Grady|D|James P\.)\s+and\s+(?=Jill|Stone|Feit|Elizabeth|Linda|Dorothy|Hatch|D|Donna)/, " AND ")
            .replaceAll(/(?<=Edwin P\.|Camilo|Thomas|Jessie|Elijah|Daniel|Joseph|Nathan|Loizzo)\s+and\s+(?=Fanta|Bobbie|Maria|Judith|Florida|Valerie|Eva|Lewis|Walls)/, " AND ")
            .replaceAll(/(?<=Sam|John|Spears|Martin|Partnership|Michael|Traders)\s+and\s+(?=Esther|Jane|Schneider|Kaye|Mountain|Lorraine|Dealers)/, " AND ")
            .replaceAll(/(?<=Life|Rodecker|Stuart|Insurance|Securities)\s+and\s+(?=Accident|Annuity|Company|Financial|Investments)/, " AND ")
            .replaceAll(/(?<=Lynch|Voss|Thalmann|Frederick|Conn|Anthony|Ash|Wegard)\s+and\s+(?=Co\. Inc\.|Co\.)/, " AND ")
            .replaceAll(/\s+and\s+(?=Co\. Inc\.|Co\.|Corp\.|Company|Associates)/, " AND ")
            .replaceAll(/(?i)(?<=[A-Z]{2,10})&|&(?=[A-Z]+)/, ' & ')
            .replaceAll(/([A-Z][a-z]+ [A-Z]\.)\s+(?:and|&)\s+([A-Z][a-z]+ [A-Z]\.) ([A-Z][a-z]+)/, '$1 $3 , $2 $3')
            .replaceAll(/(?<=HILLSTROM|TAUB|KRAUS|CRAVENS|DUNN)\s+AND\s+(?=SHAUN|JASON|JOHN|OPPENHEIMER|NAZAN)/, " and ")
            .replaceAll(/([A-Z]\. [A-Z][a-z]+) & (?=Company)/, '$1 AMP ')
            .replaceAll(/(?<=Services|Company|Blarcom|Sun|Schafranick|Stone|Ruggiero|Provenzano) & (?=RBC|Patnck|Sovereign|Sunlogic|State|Smith|Monitor|Olde|First)/, ' and ')
            .replaceAll(/(?<=Lewin|Alesandro|Mercurio|Smith) & (?=Daniel|Nicholas|Dean|Gabrieto)/, ' and ')
            .replaceAll(/(?<=Trust) &\s+(?=A\.S\.I\.T\.)/, ' and ')
            .replaceAll(/([A-Z]\. [A-Z][a-z]+) & ([A-Z][a-z]+ [A-Z]\.)/, '$1 and $2 ')
            .replaceAll(/([A-Z][a-z]{2,}+)(&)\s*/, '$1 $2 ')
            .replaceAll(/\s+(&)\s+/, ' $1 ')
            .replaceAll(/(?i)(?<=& Co\. Inc\.|& Co\.)\s*(?=John|Kevin|Miguel|UBS|Steven|Purshe|Raymond|Richard|Spencer|Elizabeth|Carl|Todd|Thomas|Kenneth|Lawrence)/, "\n")
            .replaceAll(/(?i)(?<=& Co\. Inc\.|& Co\.)\s*(?=Great\-West|Peter Mark|[A-Za-z]+\s[A-Z]\.)/, "\n")
            .replaceAll(/(?i)(?<=& Co\. Inc\.|& Co\.|Corp\.)\s*(?=First|Newbridge|National|Wells|Ladenburg|Stephen|Sean|Royce|Maxim|Tasin|David|Jonathan|Marc|Robert)/, "\n")
            .replaceAll(/(?i)(?<=& Co\. Inc\.|& Co\.|Corp\.)\s(?=Estate of|Lombard)/, "\n")
            .replaceAll(/(?<=(?:Securities|Services|Advisors|Markets|Sons|Winter|Fitzner|Corporation))\s(?=Steven|Mark|Isselle|Newton|Harold|Michael [A-Z]\.|Fidelity)/, "\n")
            .replaceAll(/(?<=(?:Services)|Robinson)\s(?=John [A-Z]\.|Maria|Midrange|Robert)/, "\n")
            .replaceAll(/(?<=(?:Service[s]?))\s(?=[A-Z][a-z]+\s[A-Z]\.|John|Banc|Amy|Citizens)/, "\n")
            .replaceAll(/(?<=(?:Capital|Stanley))\s(?=Western|The GMS|Wade|Adam Lang|Amy Lynn|George Joseph|Peter John|Robert Ruffo|Cantella &)/, "\n")
            .replaceAll(/(?<=(?:Ann Levine|W\. Rosenthal))\s(?=James J\.|James)/, "\n")
            .replaceAll(/\((USA|UK|U\.S\.|Canada)\)/, '$1')
            .replaceAll(/(?<=(?:Corporation|Corp\.|Vooren|Barry Jr\.|Securities Inc\.))\s(?=(?:JRA|James|Thomas|Nathan &|Credit Suisse|arid First))/, ",")
            .replaceAll(/(?<=(?:Glen Jones|Jean Naus|Cooke Sr\.|Tomlin|DW3 Inc\.|Corp\.))\s+(?=(?:Thomas|Nancy|Lori Jo|Wachovia|James|Societe))/, ",")
            .replaceAll(/(?<=(?:F\. Spiegel|Ann Levine))\s+(?=(?:Jeffrey|James J\.))/, ",")
            .replaceAll(/(?<=(?:Rosenthal|Lebenthal))\s+(?=(?:James Coyle|James B\.))/, ",")
            .replaceAll(/(?<=(?:Jon|Lorraine)\s[A-Z]\.)\s+/, " ")
            .replaceAll(/(?<=Gunnar & Co\.)\n(?=LLC)/, " ")
            .replaceAll(/(?i)\"([A-Z\’ ]+)\"/, '$1')
            .replaceAll(/(?i)(Gerds)\"/, '$1')
            .replaceAll(/(?i)(?<=Corp\.|Inc\.|LLC\.?|& Co\. LP|& Co\.)\s+(?:and)\s+(?=Viney K\.|Vinton|Allianz|Brian|Fidelity| Lake K\.|Wachovia|Anthony L\.|Todd Edward|Goldman|Barton)/, ",")
            .replaceAll(/(?i)(?<=Corp\.|Inc\.|LLC\.?|& Co\. LP|& Co\.)\s+(?:and)\s+(?=Frank)/, ",")
            .replaceAll(/(?i)(?<=Corp\.|Inc\.[\)]?|LLC\.?|& Co\. LP|L\.P\.\))\s+(?:and)\s+(?=[A-Z][a-z0-9]+)/, ",")
            .replaceAll(/(?<=DW (?:Ltd\.?|Inc\.?|Incorporated|Corporation))\s+(?:and)\s+(?=[A-Z][a-z]+|Matt|Scott|Mark|[A-Za-z]+\s[A-Z]\.)/, ",")
            .replaceAll(/(?<=DW (?:Ltd\.?|Inc\.?|Incorporated)|Dean Witter)\s+(?=William|[A-Za-z]+\s[A-Z]\.)/, ",")
            .replaceAll(/(?<=Securities Inc\.), and(?=\n)/, " ")
            .replaceAll(/(?<=Stanley|LTD|John Kitchin|Simonetti|Services|L\.L\.C\.)\s+(?=(?:Scott Danial Swaylik|Carlos Vargas|New York|TD|Herbert|James))/, "\n")
            .replaceAll(/(?<=L\.L\.C\.)\s+(?=(?:James Vincent))/, ",")
            .replaceAll(/(?<=Stanley)\s+(?=(?:Francisco))/, "\n")


        return entities.trim()
    }

    def sanitizeEntityName(def entityName) {
        entityName = entityName.toString().trim()
            .replaceAll(/\s+/, " ")
            .replaceAll(/\"Chip\"/, "")
            .replaceAll(/\[|\($/, "")
            .replaceAll(/^and\s*|\sand$|\("|\((?=f\/k\/a)|a Limited Partnership|^And$|•/, "")
            .replaceAll(/^As Successor.*to\s|Claimant|vs\.|(?i)\sDeceased$|\sAnd$/, "")
            .replaceAll(/(?:formerly|\(?now) known as|as successor to|(?<=Advisors)\s*formerly\s*(?=A\.G\.)|individually and as agent for|(?<=Rosenfeld|Lambert) as agent for (?=Morgan)/, "ALIAS")
            .replaceAll(/(?:Investment\ss)/, "Investments")
            .replaceAll(/((?:Investment|Edward))\s*s/, '$1s')
            .replaceAll(/(?:Ina)$/, 'Inc.')
            .replaceAll(/(?:l\? atUiew|l̂ atUiew)/, "Matthew")
            .replaceAll(/lmtiaz/, "Imtiaz")
            .replaceAll(/iVIerrill/, "Merrill")
            .replaceAll(/A l \^/, "Alex")
            .replaceAll(/as Successor.*Owner of|as Trustee of.*|^Trust$|on behalf of|\scollectively$|(?i)Individually and|collectively$/, "")
            .replaceAll(/\sAMP\s/, " & ")
            .replaceAll(/(?<=[A-Z][a-z]{2,15})\sAND\s(?=[A-Z][a-z]+)/, " and ")
            .replaceAll(/\s+/, " ")
            .replaceAll(/the\s/, "The ")

        return entityName.trim()
    }

    def sanitizeAlias(def alias) {
        alias = alias.toString().trim()
            .replaceAll(/[\)\(\"]/, "")
            .replaceAll(/(?:lSchifanom)/, "Schifano")
        return alias.trim()
    }

    def street_sanitizer = { street ->
        fixStreet(street)
    }

    def fixStreet(street) {
        street = street.toString().trim()
        street = street.toString().replaceAll(/,$/, "").trim()
        return street
    }

    def pdfToTextConverter(def pdfUrl) {
        /*List<String> pages = ocrReader.getText(pdfUrl)
        return pages*/
       /* try {
            List<String> pages = ocrReader.getText(pdfUrl)
            return pages
        } catch (NoClassDefFoundError e) {*/
            def pdfFile = invokeBinary(pdfUrl)
            def pmap = [:] as Map
            pmap.put("1", "-layout")
            pmap.put("2", "-enc")
            pmap.put("3", "UTF-8")
            pmap.put("4", "-eol")
            pmap.put("5", "dos")
            def pdfText = context.transformPdfToText(pdfFile, pmap)
        //println pdfText

        if(!pdfText.toString().contains("FINRA")){
            List<String> pages = ocrReader.getText(pdfUrl)
           // println pages
            return pages
        }
            return pdfText
       /* } catch (IOException e) {
            return "PDF has no page"
        }*/
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = true, clean = true, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def invokeUrl(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}