package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent

import com.rdc.rdcmodel.model.RelationshipType


context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_ri_dob script = new Us_ri_dob(context)
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

class Us_ri_dob {
    final entityType
    final def ocrReader
    ScrapianContext context = new ScrapianContext()
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    static def url = "https://dbr.ri.gov/decisions/decisions_banking.php"

    Us_ri_dob(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        this.context = context

    }

    def initParsing() {
        //Invoke html
        def html = invoke(url)
        //Get Data from the Table
        getDataFromHtml(html)
    }

    def getDataFromHtml(def html) {
        def pdf_url
        def pdfMacher = html =~ /(?i)<a href="(.*?1999-2008\.pdf)" class="qh__file-list__link"/
        if (pdfMacher.find()) {
            pdf_url = pdfMacher.group(1)
        }
        pdf_url = pdf_url.trim()
        def pdfData = pdfToTextConverter(pdf_url)

        pdfData = pdfData.toString().replaceAll(/(?ism)\A.*?(?=Number)/, "")
        pdfData = dataFix(pdfData)

        def pdfrow
        def pdfrowMacher = pdfData =~ /(?im)\n\s*?[\w][\w\s\,\.\&\–\-\/\:\"\“\”\;\']+?\s+(\d{1,2}\-\d{1,4}|\send)/
        while (pdfrowMacher.find()) {
            pdfrow = pdfrowMacher.group()
            pdfrow = pdfrow.replaceAll(/\s{7,}/, "      ")
            pdfrow = pdfrow.replaceAll(/\s{1,4}/, " ")
            pdfrow = rowFixer(pdfrow)


            def aliasList = []
            def name
            def nameList = []
            def nameMacher = pdfrow =~ /(?ism)(.+?)(?=Consent\s(?:Agreement\s?|Order)|Order (?:\–|\-)\s?License Suspension|Order\s?(?:\–|\-)\s*License\s*Revocation|Order to Cease|Supplemental Order|sVacating|Order\s?(?:\–|\-)\s*Emergency|Emergency Order\s?(?:\–|\-)?\s*|Order - (?:Cease|Consent|Stipulation|Reinstatement)|Letter of|Decision|Approval to Conduct|Order Vacating|(?<!License)\s{2,}Revocation)/
            if (nameMacher.find()) {
                name = nameMacher.group(1)
                name = name.trim()
                name = name.replaceAll(/\n/, " ").replaceAll(/\s+/, " ").replaceAll(/^\d{4}/, "")
                if (!name.contains("DBA Financial Services")) {
                    name = name.toString().replaceAll(/(?i)(?:\ba[\.\/]?k[\.\/]?a\b|\bf[\.\/]?k[\.\/]?a\b|\bn[\.\/]?k[\.\/]?a\b|and\sits\sAffiliates\:)/, "a.k.a")
                    name = name.toString().replaceAll(/(?i)\b(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a\b|\band\sits\saffiliate\b/, "d.b.a")
                    name = name.replaceAll(/\s?\:\s/, " a.k.a ")
                }
                if (name.contains("Streamline Mortgage and Financial Services")) {
                    nameList = name.split(/\band\b/)
                } else if (name.contains("Pawtucket Credit Union, Pawtucket Credit Union Financial Services")) {
                    def name1
                    def name2
                    def nmacher = name =~ /(Pawtucket.+?)\,\s(Pawtucket.+?LLC.+Services)\,\s*(Robert.+?)\,\s*Individually\s*(Karl.+?)\, \s*Individually/
                    if (nmacher.find()) {
                        name1 = nmacher.group(1) + " a.k.a " + nmacher.group(3) + " a.k.a " + nmacher.group(4)
                        name2 = nmacher.group(2) + " a.k.a " + nmacher.group(3) + " a.k.a " + nmacher.group(4)
                    }
                    nameList.add(name1)
                    nameList.add(name2)
                } else if (name =~ /(Secure.+?\.)\,\s*(Secure.+?)\,\s*(Secure.+?\,\sInc\.)/) {
                    def nMacher = name =~ /(Secure.+?\.)\,\s*(Secure.+?)\,\s*(Secure.+?\,\sInc\.)/
                    if (nMacher.find()) {
                        nameList.add(nMacher.group(1))
                        nameList.add(nMacher.group(2))
                        nameList.add(nMacher.group(3))
                    }
                } else if (name =~ /(?ism)(Secure.+Corp\.)\s*(SMC\s*Lending)/) {
                    def nameMach = name =~ /(?ism)(Secure.+Corp\.)\s*(SMC\s*Lending)/
                    if (nameMach.find()) {
                        def temp = nameMach.group(1) + " d.b.a " + nameMach.group(2)
                        nameList.add(temp)
                    }

                } else if (name.contains("Sun National Mortgage and Funding LLC & Sun Consultants") | name.contains("United Financial Mortgage Corp")) {
                    nameList = name.split(/\s\&/)
                } else if (name =~ /(Aegis.+?Equity)\;\s*(Aegis.+?Corp\.)\sand\s(Aegis.+?Co\.)/) {
                    def nMacher = name =~ /(Aegis.+?Equity)\;\s*(Aegis.+?Corp\.)\sand\s(Aegis.+?Co\.)/
                    if (nMacher.find()) {
                        nameList.add(nMacher.group(1))
                        nameList.add(nMacher.group(2))
                        nameList.add(nMacher.group(3))
                    }
                } else if (name =~ /(Arlington.+?Corp\.)\s*(Windsor.+?Financial\sMortgage)/) {
                    def nMacher = name =~ /(Arlington.+?Corp\.)\s*(Windsor.+?Financial\sMortgage)/
                    if (nMacher.find()) {
                        def name1 = nMacher.group(1) + " a.k.a " + nMacher.group(2)
                        nameList.add(name1)
                    }
                } else if (!name.contains("d.b.a") && !name.contains("a.k.a") && !name.contains("Taylor, Bean")) {
                    def nMacher1 = name =~ /(?ism)([\w\,]+\s[\w]+.*?)\s\&\s([\w]+\s[\w]+.*?(?<!Inc\.|LLC))$/
                    if (nMacher1.find()) {
                        name = nMacher1.group(1)
                        nameList.add(nMacher1.group(2))
                    }
                    if (name =~ /(?ism)(.+?(?:Inc.|Corp.(?!\sof)))\s([\w]+\s[\w]+.*)/) {
                        def nMacher = name =~ /(?ism)(.+?(?:Inc.|Corp.(?!\sof)))\s([\w]+\s[\w]+.*)/
                        if (nMacher.find()) {
                            nameList.add(nMacher.group(1))
                            nameList.add(nMacher.group(2))
                        }
                    } else {
                        nameList.add(name)
                    }
                } else {
                    nameList.add(name)
                }
            }
            def eventDate
            def dateMacher = pdfrow =~ /(?ism)(\d{1,2}\/\d{1,2}\/\d{2}|[A-z]+(?!\s123)\s*\d{1,2}(?:\,|\.)?\s*\d{2,4})/
            if (dateMacher.find()) {
                eventDate = dateMacher.group(1)
                eventDate = eventDate.toString().replaceAll(/(?i)([A-Z]+)(\d{1,2}\,\s\d{4})/, { def a, b, c -> return b + ' ' + c })
            }
            nameList.each {
                it = it.toString().replaceAll(/\,$/, "")
                (it, aliasList) = aliasChecker(it)
                createEntity(it, aliasList, pdf_url, eventDate)
            }
        }
        //For HTML table
        def tableData
        def tableMacher = html =~ /(?ism)<table.*\/table>/
        if (tableMacher.find()) {
            tableData = tableMacher.group()
            tableData = tableData.replaceAll(/(?ism)<\!\-\-\-.*?\-\-\->/, "")
            tableData = tableData.replaceAll(/(?ism)<\/tr>\s*?\n*?\s*?<td>/, "<\\/tr>\n<tr><td>")
            tableData = tableData.replaceAll(/(?ism)<\/td>\s*?\n*?\s*?<tr>/, "<\\/td>\n<\\/tr><tr>")
            def rowdata
            def rowMacher = tableData =~ /(?ism)<tr>\s*\n*\s*<td.+?<\/td>\s*\n*\s*<\/tr>/
            while (rowMacher.find()) {
                rowdata = rowMacher.group()
                rowdata = rowMacher.toString().replaceAll(/&amp;(?=F|\sThomas)/, "&")
                def aliasList = []
                def name
                def nameList = []
                def nameMacher = rowdata =~ /(?ism)<tr>\s*\n*\s*<td.*?>(.+?)<\/td>(\s*|\n*)<td>/
                if (nameMacher.find()) {
                    name = nameMacher.group(1)
                    name = name.replaceAll(/\n/, " ").replaceAll(/\s+/, " ")
                    name = name.toString().replaceAll(/(?i)(?:\ba[\.\/]?k[\.\/]?a\b|\bf[\.\/]?k[\.\/]?a\b|\bn[\.\/]?k[\.\/]?a\b)/, "a.k.a")
                    if (!name.contains("DBA Financial Services, Inc.")) name = name.toString().replaceAll(/(?i)\b(?:t[\.\/]?)?d[\.\/]?b[\.\/]?a\b/, "d.b.a")
                }

                if (name =~ /(?ism)([\w\-]+\s[\w\s\-\,\.]+)\sand\s([\w\-]+\s[\w\s\,\.]+)/) {
                    def nameSplitter = name =~ /(?ism)([\w\-]+\s[\w\s\-\,\.]+)\sand\s([\w\-]+\s[\w\s\,\.]+)/
                    if (nameSplitter.find()) {
                        nameList.add(nameSplitter.group(1))
                        nameList.add(nameSplitter.group(2))
                    }
                } else if (name =~ /(Providence.+?Corp\.)\,\s*(Eric\sCouture)\,\s(Gaudreault.+?Inc\.)/) {
                    def nMacher = name =~ /(Providence.+?Corp\.)\,\s*(Eric\sCouture)\,\s(Gaudreault.+?Inc\.)/
                    if (nMacher.find()) {
                        nameList.add(nMacher.group(1))
                        nameList.add(nMacher.group(2))
                        nameList.add(nMacher.group(3))
                    }
                } else if (name =~ /(Tower.+?Inc\.)\,\s*(F&F\sAssociates)\s*\&\s(Thomas.+?Fuoco)/) {
                    def nMacher = name =~ /(Tower.+?Inc\.)\,\s*(F&F\sAssociates)\s*\&\s(Thomas.+?Fuoco)/
                    if (nMacher.find()) {
                        nameList.add(nMacher.group(1))
                        nameList.add(nMacher.group(2))
                        nameList.add(nMacher.group(3))
                    }

                } else if (name.contains("United Financial Mortgage Corp")) {
                    nameList = name.split(/\s\&/)
                } else if (!name.contains("d.b.a") && !name.contains("a.k.a") && !name.contains("Taylor, Bean")) {
                    def nMacher1 = name =~ /(?ism)([\w\,]+\s[\w]+.*?)\s\&\s([\w]+\s[\w]+.*?(?<!Inc\.|LLC))$/
                    if (nMacher1.find()) {
                        name = nMacher1.group(1)
                        nameList.add(nMacher1.group(2))

                    }
                    if (name =~ /(?ism)(.+?(?:Inc.|Corp.(?!\sof)))\s([\w]+\s[\w]+.*)/) {
                        def nMacher = name =~ /(?ism)(.+?(?:Inc.|Corp.(?!\sof)))\s([\w]+\s[\w]+.*)/
                        if (nMacher.find()) {
                            nameList.add(nMacher.group(1))
                            nameList.add(nMacher.group(2))
                        }
                    } else {
                        nameList.add(name)
                    }
                } else {
                    nameList.add(name)
                }
                def entity_url
                def urlMacher = rowdata =~ /(?ism)<td>\s*\n*\s*<a href=\"(.+?\.pdf).*?>/
                if (urlMacher.find()) {
                    entity_url = urlMacher.group(1)
                    entity_url = entity_url.trim()
                    entity_url = "https://dbr.ri.gov" + entity_url
                    entity_url = entity_url.toString().replaceAll(/&amp;/, '&')
                }
                def eventDate
                def dateMacher = rowdata =~ /(?ism)<td>(?:\s*|\n*)([\w]+(?:\s*|\n*)\d{1,2}\,(?:\s*|\n*)\d{4})(?:\s*|\n*)</
                if (dateMacher.find()) {
                    eventDate = dateMacher.group(1)
                    eventDate = eventDate.replaceAll(/\n/, " ").replaceAll(/\s+/, " ")
                }
                nameList.each {
                    (it, aliasList) = aliasChecker(it)
                    createEntity(it, aliasList, entity_url, eventDate)
                }
            }
        }
    }

    def dataFix(def pdfData) {
        pdfData = pdfData.replaceAll(/(?ism)Order\s+Name.+?Number/, "")
        pdfData = pdfData.replaceAll(/20001/, "2001")
        pdfData = pdfData.toString().replaceAll(/(?ism)(Americas Money Transfers)(\s+Revocation)/, { def a, b, c -> return b + c + '        Revocation   end\n' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Central Pacific Mortgage)\s*(Order \- License)\s*(\s\w+\s\d{1,2}\,\s+\d{4})\s*([\w\s\,\-\,\.]+?)\s*(07\-132)\s*(Revocation)\s(Pacific\sMortgage\sCompany)/, { def a, b, c, d, e, f, g, h -> return b + ' ' + e + ' ' + h + '  ' + c + ' ' + g + ' ' + d + ' ' + f })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Emergency\sOrder\s?(?:\-|\–))(.*?)((?:License)?\sSuspension)/, { def a, b, c, d -> return b + d + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\s?(?:-|–)\s?License)(.+?)(Revocation of Debt|Revocation|Suspension)/, { def a, b, c, d -> return b + ' ' + d + ' ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Central Pacific Mortgage)\s*(Order - License Revocation)\s*(\s\w+\s\d{1,2}\,\s+\d{4})\s*(Company dba New England)\s*(07\-132)\s*(Pacific Mortgage\s*Company)/, { def a, b, c, d, e, f, g -> return b + ' ' + e + ' ' + g + '  ' + c + ' ' + d + ' ' + f })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Emergency\sOrder)\s?([\w].*?)(Suspending)(.*?)(License)/, { def a, b, c, d, e, f -> return c + ' ' + b + ' ' + d + ' ' + f + ' ' + e })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Consent\sOrder\s?\-)\s?([\w].*?)(License\sSurrender)/, { def a, b, c, d -> return '  ' + b + ' ' + d + ' ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\-License\sRevocation\sof\sDebt)\s+?([\w].*?)\s{10}(.*?\-\d{1,3}\s+?)(.+?)(Management\s*?License)/, { def a, b, c, d, e, f -> return c + ' ' + e + '              ' + b + ' ' + f + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)((?<!-\s)Letter of)\s*(\d{1,2}\/\d{1,2}\/\d{2}|[A-z]+\s*\d{1,2}\,\s*\d{2,4}\s*\d{1,2}\-\d{1,4})\s*([\w\s\,\.\-]+?)(Admonishment|Admonition)/, { def a, b, c, d, e -> return d + ' ' + b + ' ' + e + ' ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\s\-\sEmergency)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(Suspension)/, { def a, b, c, d, e, f -> return c + ' ' + e + '            ' + b + ' ' + f + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\s\-\sCease\s(?:and|&))\s+?([\d]?[A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(Desist)/, { def a, b, c, d, e, f -> return c + ' ' + e + '             ' + b + ' ' + f + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\sto\sCease)([A-Z\.\s\,\-\:]+?)(Unsafe\sand)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(U[\w\s]+?Practices)/, { def a, b, c, d, e, f, g, h -> return c + ' ' + e + ' ' + g + '              ' + b + ' ' + d + ' ' + h + ' ' + f })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Supplemental)([A-Z\.\s\,\-\:]+?)(Order\sto\sCease)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(U[\w\s]+?Practices)/, { def a, b, c, d, e, f, g, h -> return c + ' ' + e + ' ' + g + '              ' + b + ' ' + d + ' ' + h + ' ' + f })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\sto\sCease)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(U[\w\s]+?Practices)/, { def a, b, c, d, e, f -> return c + ' ' + e + '             ' + b + ' ' + f + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\sVacating)([A-Z\.\s\,\-\:]+?)(Emergency\sOrder)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(Suspending\s*?License)/, { def a, b, c, d, e, f, g, h -> return c + ' ' + e + ' ' + g + '              ' + b + ' ' + d + ' ' + h + '   ' + f })
        pdfData = pdfData.toString().replaceAll(/(?im)((?<!Consent )Order\s\-(?!\sLicense|\sConsent|\sSuspension|\sStipulation))\s+?([A-Z\.\s\,\&\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+?(Reinstatement)\s([\w\s]+?Mortgage)/, { def a, b, c, d, e, f, g -> return c + ' ' + e + ' ' + g + '             ' + b + ' ' + f + '   ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Emergency\sOrder\s+Withdrawing)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+?(Approval\sto\s+Conduct\sBusiness)/, { def a, b, c, d, e, f -> return c + ' ' + e + '             ' + b + ' ' + f + '   ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Emergency\sOrder\s)([A-Z\.\s\,\-\:]+?)(Suspending)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(License)/, { def a, b, c, d, e, f, g, h -> return c + ' ' + e + ' ' + g + '              ' + b + ' ' + d + ' ' + h + '   ' + f })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Approval\sto)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(\DConduct\sBusiness)\s*?(Mortgage)/, { def a, b, c, d, e -> return c + ' ' + e + '             ' + b + ' ' + d + '      end' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Emergency\sOrder(?!\s\-|\sSuspending))\s{50,}([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s{5,8}(Withdrawing)/, { def a, b, c, d, e, f -> return c + ' ' + e + '             ' + b + ' ' + f + '   ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\s\-\sLicense\sSuspension)([A-Z\.\s\,\-\:]+?)(and)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(Notice\sof\sIntent\sto\s*?Revoke\sLicense)/, { def a, b, c, d, e, f, g, h -> return c + ' ' + e + ' ' + g + '              ' + b + ' ' + d + ' ' + h + '   ' + f })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Decision\s-)([A-Z\.\s\,\-\:]+?)(Unlicensed\sCheck)\s+?([A-Z\.\s\,\-\:]+?)\s{2,}(.+?(?:\-\d{1,4}|\d{4})\s+?(?!\s+\d{1,2}\-\d{1,4}))(\D.+?)\s+(Cashing)/, { def a, b, c, d, e, f, g, h -> return c + ' ' + e + ' ' + g + '              ' + b + ' ' + d + ' ' + h + '   ' + f })
        pdfData = pdfData.toString().replaceAll(/(?ism)(?ism)(Order\s\-\sLicense Revocation\s+)((?=dba First)[\w].+?)(\w+\s\d{1,2}\,\s+\d{4}\s+\d{1,2}\-\d{1,4})\s+([\w]+)/, { def a, b, c, d, e -> return c + ' ' + e + '      ' + b + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\s\-\sEmergency\sSuspension.+?)(Credit Corp.: Home 123 Corporation)/, { def a, b, c -> return c + '     ' + b })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s+(Order\s?(?:\-|\–)\s*License\sRevocation)(\s+?(?:\d{1,2}\/\d{1,2}\/\d{2}|\w+\s*\d{1,2}\,\s*\d{4})\s*\d{1,2}\-\d{1,4})\s*?(\sFinancial\sGroup|\sMortgage\sCorporation|\sFinancial\sMortgage\sCompany|\sEquity\sFirst\sFinancial\sCorp\.|\sMassachusetts\sd\/b\/a\sCFS\sMortgages|\sStreamline\sDirect\sMortgage|\sUpland\sMortgage|\sCommerce\sFinancial\sGroup|\sEngland\sFunding|\sNorth\spro\sServices|\sCompany\,\sInc\.|\sChampion\sMortgage\sCompany|\sEngland\sFunding|\sCorp\.\sdba\sCFIC\sHome\sMortgage|\sInc\.\sdba SWMC\,\sInc\.|\sInc\.|\sHomes|\sLLC\sdba NMS\sMortgage|\sdba\sFarah\sFinancial\s+Resources\,\sInc\.|\sSMT\sMortgage\,\sLLC|\sLLC|\sFHB\sFunding|\sInvestment\sCorp\.|\sMortgage\sCorp\.|\sCorp\.|\sNationwide\sMortgage|\sCorporation|\sServices\,\sInc\.|\sNorth\spro\sServices|\sSMC\sLending|\sFHB\sFunding|\sFunding\,\sLLC|\sFunding|\sBrokers\/CMB|\sL\.|\sUnion\sFinancial|\sMortgage|\sFinancial(?!\sServices|\sMortgage|\sGroup)|\sCompany|\sHomes|\sNetwork)/, { def a, b, c, d -> return d + '    ' + b + ' ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s+(Order\s?\-\sLicense\sRevocation|Order\s(?:\–|\-)\sLicense\sSuspension)(.+?)(\s\w+\s*\d{1,2}\,\s*\d{4}\s*\d{1,2}\-\d{1,4}|\s+\d{1,2}\/.*?\-\d{1,4})/, { def a, b, c, d -> return c + '       ' + b + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Consent\sAgreement(?!(?:\s+\w+\s\d{1,2}\,|\s+\d{1,2}\/\d{1,2}\/\d{2})))\s*([A-Z\s\.\,]+)(\-)\s*([A-Z\s\.\,]+)(Letter of Admonishment)\s*(\s\w+\s\d{1,2}\,\s+\d{4}\s*\d{1,2}\-\d{1,4})/, { def a, b, c, d, e, f, g -> return c + ' ' + e + '       ' + b + ' ' + d + ' ' + f + '               ' + g })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s+(Order\s?(?:\-|\–)\sLicense\sRevocation)(\s+?(?:\d{1,2}\/\d{1,2}\/\d{2}|\w+\s\d{1,2}\,\s+\d{4})\s*\d{1,2}\-158)\s*?(\sLoans)/, { def a, b, c, d -> return ' ' + d + '    ' + b + ' ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Consent\s(?:Order|Agreement))(\s+?(?:\d{1,2}\/\d{1,2}\/\d{2}|\w+\s\d{1,2}(?:\,|\.)\s+\d{4})\s*\d{1,2}\-\d{1,4})\s*?(\,\sInc\.|\s*\.\sLLC|\s*Company\,\sLLC|\s*Lending(?!\sTree)|\sServices\s*\,\s*Inc\.|\sConsultants\,\sInc\.\s\&\sRichard\s*A\.\s*Verduchi\s\&\sAnthony\sR\.\sVerduchi|\sFinancial\sMortgage\sCompany|\sServices\,\sInc\.|\sCrestar\sMortgage\sCo|\sInc\.|\sUpland\sMortgage|\sdba\sMortgage\sAdvocates|\sdba\sPrime\sMortgage\sCompany|\sdba\sAuto\sLoan|\sHome\sLoans|\sColon\sFinancial|\sLending|\sSouthern\sNew\sEngland|\sCrestar\sMortgage\sCo|\s*Inc\.\s*dba HFS Home Mortgage|\swww\.eastwest\.com|Corp\.|\sOlympic\sFunding|\sFunding\sLLC|\sBMSLOAN\.com|\sVOMC\sMortgage\sServices|\sAMI\sMortgage|\sdba\sAridio\sExpress|\sFHB\sFunding|\sServices\,\sInc\.|\sCommunications|\s\bCo\b|\sInc\.|\sCommunications|\sRhode\sIsland|\sVision\sFunding|\sLending|\s*\,\sInc\.|\sCorporation|\s*\bServices\b|\s\bAmerica(?!\s*First|n)\b|\sCo\.\sLLC|\sCo\.|\sMortgage(?!\sOptions|\sFinancial|\sAlternatives|\s*Amenities|\s*Associates|\sPartners|it|\s*Trust|\s*USA)|\sLLC|\sEngland|\sHF\sHomestead\sFunding\sCo|\sMarket(?!\s*One)|\sRI|\sIsland|\sCompany)/, { def a, b, c, d -> return '  ' + d + '    ' + b + ' ' + c + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Consent\s(?:Order|Agreement))(\s+?(?:\d{1,2}\/\d{1,2}\/\d{2}|\w+\s*\d{1,2}\,\s+\d{4})\s*\d{1,2}\-\d{1,4}[A-Z])\s*(Home\sLoans|Vision\sFunding)/, { def a, b, c, d -> return ' ' + d + '    ' + b + ' ' + c + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s*(Mortgage\sOptions.+?\,)\s*(Inc\.)\s*(Consent\s*Agreement)\s*(\s\w+\s\d{1,2}\,\s+\d{4}\s*\d{1,2}\-\d{1,4})\s*(Corp\.)/, { def a, b, c, d, e, f -> return '\n ' + b + ' ' + c + ' ' + f + '        ' + d + '          ' + e })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Decision)(\s+?(?:\d{1,2}\/\d{1,2}\/\d{2}|\w+\s\d{1,2}\,\s+\d{4})\s*\d{1,2}\-\d{1,4})\s*?(\sLtd\.|\s\&\sGo)/, { def a, b, c, d -> return d + '    ' + b + ' ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s{1,33}(Order\s?(?:\-|\–)\s?License\sRevocation)\s+(\s[A-Z\s\,\.\-\']+)(\s+(?:\w+\s\d{1,2}\,\s\d{4,5}\s+\d{1,3}\-\d{1,4}|\s\d{1,2}\/\d{1,2}\/\d{2,4}\s+\s+\d{1,3}\-\d{1,4}))/, { def a, b, c, d -> return c + '       ' + b + '       ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s*(Order\s?-\s*License Revocation)\s*(\s\w+\s\d{1,2}\,\s+\d{4,5}\s*\d{1,2}\-\d{1,4}[A-Z]?|\s\d{1,2}\/\d{1,2}\/\d{2}\s*\d{1,2}\-\d{1,4}[A-Z]?)\s*(\sServices\,\sInc\.|\sMortgage\sCorporation|\sEngland\sFinancial|\sFinancial\sServices\sof\sRhode\s*Island|\sInternational\,\sInc\.|\sGroup\,\sInc\.|\sFinancing\,\s*Inc\.|\sExchange\,\sInc\.|Mortgage lenders(?=\s*Freedom)|\sFinancial\sGroup|\sServices\sof\sRhode\s*Island|\sMortgage\sServices|\sFinancial\sMortgage\sCompany|\sPinnacle\sResidential\sMortgage|\sPennsylvania|\sSadek\sEquities|\sCornerstone\sCapital\sFunding|\sAdvantage\sAmerica|\sStreamline\sDirect\sMortgage|\sCross\sCountry\sMortgage|\sPinnacle\sResidential\sMortgage|\sAcceptance\sCorporation|\sFirst\sNew\sEngland\sFinancial|\sGroup\,\sLLC|\sdba\sFFS\sInc\.|\sdba\sHT\sMortgage|\sdba\sIsland\sGreen\sMortgage|\sFinancial\sMortgage\sCompany|\sdba\sFirst\sCapital\sCorporation|dba\sNational\sEquity\sLenders|\s*Inc\,\s*dba\s*Noreastmt\s*\.com|\sMillenium\sMortgage|\sdba\sUMT\sCorp\.|\sSafe\sCredit|\sFinancial\sMortgage|\sApproval\sMortgage\sCo\.|\sInvestment\,\sInc\.|\sOwn\sInterest\sRate\"|\sHomeworks\sFinancial\,\sInc\.|\sAcademy\sFunding\sSource|\sStar\sMortgage|\sStone\sCastle\sHome\sLoans|\sMailbox\srentals|\sInternational\sMortgage\sCo\.|\sdba\s*Ampro\s*Mortgage|\sNetwork|\sCapital(?!\sFinancial)|\s\bHomes\b|\sFinancial(?!\sServices|\sMortgage|\sGroup))/, { def a, b, c, d -> return ' ' + d + '                ' + b + '           ' + '          ' + c + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s+(Emergency\sOrder\s\-\s?License\s+Suspension)([\w\s\;\-\'\,]+?)(\s+?\s\w+\s\d{1,2}\,\s+\d{4}\s+\d{1,2}\-\d{1,4})/, { def a, b, c, d -> return c + '       ' + b + '       ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s+((?:Emergency\sOrder|\sOrder)\s\-\s?(?:License)?\sSuspension)(\s+?(?:\s\d{1,2}\/\d{1,2}\/\d{2}|\s\w+\s\d{1,2}\,\s+\d{4})\s*\d{1,2}\-\d{1,4})\s*?(\sBrokers\s*Conduit\s*and\s*its\s*affiliate\s*American\s*Home\s*Mortgage\s*Acceptance|Services\,\sInc\.|\sFunding\sLLC|\sNew\sEngland\sPacific\sMortgage|\sCorporation)/, { def a, b, c, d -> return d + '       ' + b + '       ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s*(Order\s\-\sConsent)\s*(\s\w+\s\d{1,2}\,\s+\d{4}\s*\d{1,2}\-\d{1,4})\s*(Company\,\sInc\.|Company)\s*(Order)\s+/, { def a, b, c, d, e -> return '\n ' + d + ' ' + b + ' ' + e + ' ' + c + '\n   ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s*(Order\s\-\sConsent(?!\sOrder))\s*(.+?)(\s\w+\s\d{1,2}\,\s+\d{4}\s*\d{1,2}\-\d{1,4})\s*(Order)/, { def a, b, c, d, e -> return '\n ' + c + '       ' + b + ' ' + e + '           ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s*(Consent\sOrder\s\-)\s*(.+?)\s*(\s\d{1,2}\/\d{1,2}\/\d{2}\s*\d{1,2}\-\d{1,4})\s*(\sLicense\sSurrender)\s*(Network\,\sInc\.)/, { def a, b, c, d, e, f -> return ' ' + c + ' ' + f + '          ' + b + '           ' + e + '           ' + d })
        pdfData = pdfData.toString().replaceAll(/(?ism)((?<!-\s)Letter of(?!\s*Admonishment))\s*([\w\s\,\.\-]+?)\s*(\d{1,2}\/\d{1,2}\/\d{2}|[A-z]+\s*\d{1,2}\,\s*\d{2,4})\s*(Admonishment|Admonition)/, { def a, b, c, d, e -> return ' ' + c + ' ' + b + ' ' + e + ' ' + d + '                  end' + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\s\-\s?License\sSuspension)(\s+?(?:\s\d{1,2}\/\d{1,2}\/\d{2}|\s\w+\s\d{1,2}\,\s+\d{4})\s*\d{1,2}\-\d{1,4}[A-Z]?)\s*(\sServices\,\s*Inc\.|Corp\.\sand\sAegis\sWholesale\sCo\.|Lenders\sNetwork\,\sInc\.|Firststreet\.Com|Lisboa\,\sInc\.)/, { def a, b, c, d -> return ' ' + d + '             ' + b + ' ' + c + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)((?<!-\s)Letter of(?!\s*Admonishment|\s*Admonition))\s*(\d{1,2}\-\d{1,4})\s*(\d{1,2}\/\d{1,2}\/\d{2}\s*\d{1,2}\-\d{1,4}|[A-z]+\s*\d{1,2}\,\s*\d{2,4})\s*([\w\s\,\.\-]+?)\s*\s*(Admonishment|Admonition)/, { def a, b, c, d, e, f -> return e + ' ' + b + ' ' + f + ' ' + d + '  ' + c })
        pdfData = pdfData.toString().replaceAll(/(?ism)((?<!-\s)Letter of(?!\s*Admonishment|\s*Admonition))\s*([\w\s\,\.\-]+?)\s*(\d{1,2}\/\d{1,2}\/\d{2}\s*\d{1,2}\-\d{1,4}|[A-z]+\s*\d{1,2}\,\s*\d{2,4}\s*\d{1,2}\-\d{1,4})\s*(Admonishment|Admonition)/, { def a, b, c, d, e -> return c + ' ' + b + ' ' + e + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(Economy\sMortgage.+?)\s+(Order\s\-\sLicense\sRevocation)\s*(\s\w+\s\d{1,2}\,\s+\d{4}\s*\d{1,2}\-\d{1,4})\s*(Own\sInterest\sRate\”)/, { def a, b, c, d, e -> return '\n ' + b + ' ' + e + '         ' + c + ' ' + d })
        pdfData = pdfData.toString().replaceAll(/(\s\w+\s\d{1,2}\,\s+\d{4})\s*(.+?)\s*(Letter of Admonishment)\s*(\d{1,2}\-\d{1,4})\s*/, { def a, b, c, d, e -> return c + ' ' + d + ' ' + b + ' ' + e + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Consent Agreement)\s*([A-Z\.\s\,\-\']+)\s*(\-\sLetter of)\s*(\s\w+\s\d{1,2}\,\s+\d{4,5}\s*\d{1,2}\-\d{1,4})\s*(Admonishment)/, { def a, b, c, d, e, f -> return c + ' ' + b + ' ' + d + ' ' + f + ' ' + e })
        pdfData = pdfData.toString().replaceAll(/(?ism)\s*(Loans\sfor\sResidential\sHomes)\s*(Letter\s*of(?!\s*Admonishment))\s*(Mortgage\s*Corp\. d\/b\/a\s*Loans\s*for)\s*(\s\w+\s\d{1,2}\,\s+\d{4})\s*(Admonishment)\s*(Homes)/, { def a, b, c, d, e, f, g -> return '\n ' + b + ' ' + d + ' ' + g + ' ' + c + '  ' + f + ' ' + e + '     end\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Consent\sAgreement)\s*([\w][\w\s\,]+?)\s*(\-\sLetter\sof)\s*([A-z]+\s*\d{1,2}\,\s*\d{2,4}\s*\d{1,2}\-\d{1,4})\s*(dba\sNoreastmtg\.com|Inc\.)\s*( Admonishment)/, { def a, b, c, d, e, f, g -> return c + ' ' + f + ' ' + b + ' ' + d + ' ' + g + ' ' + e + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Pawtucket Credit Union\,\s*Pawtucket.+?)\s*(Consent Order)\s*([A-z]+\s*\d{1,2}\,\s*\d{2,4}\s*\d{1,2}\-\d{1,4})\s*(Harbor.+?Kozak\,\s*Individually)/, { def a, b, c, d, e -> return b + ' ' + e + ' ' + c + ' ' + d + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Order\s\-\sCease\sand\sDesist\s*[A-z]+\s*\d{1,2}\,\s*\d{2,4}\s*\d{1,2}\-\d{1,4})\s*(Financial\sGroup\,\sInc\.)/, { def a, b, c -> return c + ' ' + b + '\n ' })
        pdfData = pdfData.toString().replaceAll(/(?i)(.*00-0245.*)(Corporation of)\s*(Minnesota)/, { def a, b, c, d -> return c + ' ' + d + ' ' + b })
        pdfData = pdfData.toString().replaceAll(/(?ism)(Consent\sAgreement\s*[A-z]+\s*\d{1,2}\,?\s*\d{2,4}\s*\d{1,2}\-\d{1,4})\s*(\s*Lending(?!\sTree)|\sNetwork\,\sInc\.|\s*\.\s*LLC|\sCompany\,\sLLC|\,\sInc\.|\sEquity\,\sLLC|\sGroup\,\sInc\.)/, { def a, b, c -> return c + ' ' + b + '\n ' })
        return pdfData
    }

    def rowFixer(def rowData) {
        rowData = rowData.toString().replaceAll(/(?ism)\s+(Emergency\sOrder\s+\-\s?License\sSuspension)\s+(\s.+?)\s(\d{1,2}\/\d{1,2}.+?\-\d{1,4})/, { def a, b, c, d -> return c + '       ' + b + '       ' + d })
        return rowData
    }

    def aliasChecker(def name) {
        def alias
        def aliasList = []
        if (name.toString().contains("Carolina Mortgage Brokers/CMB")) {
            def temp = name.split(/\//)
            name = temp[0]
            aliasList.add(temp[1])
        }
        if (name.toString().contains("The Lending Group, Inc. d.b.a Lending Group, Inc. Residential Mortgage lenders")) {
            def aMacher = name =~ /(?ism)(The.+?Inc\.)\sd.b.a(.+?Inc\.)\s(Resi.+?lenders)/
            if (aMacher.find()) {
                name = aMacher.group(1)
                aliasList.add(aMacher.group(2))
                aliasList.add(aMacher.group(3))
            }
        }
        if (name =~ /(?i)(?:d\.b\.a)(.+?)/) {

            def aliasMatcher = name =~ /(?i)(d\.b\.a.+)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                alias = alias.trim().replaceAll(/^d\.b\.a/, "d/b/a")
                name = name.toString().replaceAll(/(?i)(d\.b\.a.+)/, "").trim()
                if (alias =~ /(?i)(d\.b\.a.+)/) {
                    alias = alias.split(/(?i)d\.b\.a/)
                    alias.each { it ->
                        it = "d.b.a " + it
                        aliasList.add(it)
                    }
                } else {
                    aliasList.add(alias)
                }
            }
        }
        if (name =~ /(?i)a\.k\.a(.+?)/) {

            def aliasMatcher = name =~ /(?i)a\.k\.a(.+)/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)
                name = name.toString().replaceAll(/(?i)a\.k\.a(.+)/, "").trim()
                if (alias =~ /(?i)a\.k\.a(.+)/) {
                    alias = alias.split(/(?i)a\.k\.a/)
                    alias.each { it ->
                        aliasList.add(it)
                    }
                } else {
                    aliasList.add(alias)
                }
            }
        }

        if (name =~ /(?i)(.+)a\.k\.a/) {

            def aliasMatcher = name =~ /(?i)(.+)a\.k\.a/
            while (aliasMatcher.find()) {
                alias = aliasMatcher.group(1)

                name = name.toString().replaceAll(/(?i)(.+)a\.k\.a/, "").trim()
                if (alias =~ /(?i)(.+)a\.k\.a/) {
                    alias = alias.split(/(?i)a\.k\.a/)
                    alias.each { it ->

                        aliasList.add(it)
                    }
                } else {
                    aliasList.add(alias)
                }
            }
        }
        return [name, aliasList]
    }

    def createEntity(def name, def aliasList, def entityUrl, def eventDate) {
        name = name.toString().replaceAll(/\set\sal\./, "").trim()
        name = name.toString().replaceAll(/&amp;/, "&").trim()
        name = name.toString().replaceAll(/\,$/, "").trim()
        name = name.toString().replaceAll(/\s+/, " ").trim()
        name = name.toString().replaceAll(/\(.Licensee.\)/, "").replaceAll(/and Notice of Intent to Revoke License/, "").replaceAll(/In the Matter of/, "").replaceAll(/.Pick Your Own Interest Rate./, "").trim()

        name.split(/(?<=Detomasis|Associates|Corp\.)\s&\s(?=Domenic|Thomas|William)/).each { entityName ->
            def entity
            if (!entityName.toString().isEmpty()) {
                def entityType = detectEntity(entityName)
                entity = context.findEntity(["name": entityName, "type": entityType]);
                if (!entity) {
                    entity = context.getSession().newEntity()
                    entity.setName(entityName.trim())
                    entity.setType(entityType)
                }
                def aliasEntityType, alias
                aliasList.each {
                    if (it) {
                        aliasEntityType = detectEntity(it)
                        it = it.toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\,$/, "").trim()
                        if (it.toString().contains("d.b.a") | it.toString().contains("d/b/a")) {
                            it = it.toString().replaceAll(/d\.b\.a/, "").replaceAll(/d\/b\/a/, "").trim()
                            entity.addAlias(it)
                        } else if (aliasEntityType.toString().equals(entityType)) {
                            entity.addAlias(it)
                        } else {

                            entity.addAssociation(it)
                            def newEntity = context.findEntity(["name": it, "type": aliasEntityType]);
                            if (!newEntity) {
                                newEntity = context.getSession().newEntity();
                            }
                            newEntity.setName(it)
                            newEntity.setType(aliasEntityType)
                            newEntity.addAssociation(name)
                            addCommonPartOfEntity(newEntity, entityUrl, eventDate)
                        }
                    }
                }
                addCommonPartOfEntity(entity, entityUrl, eventDate)
            }
        }
    }

    def addCommonPartOfEntity(def entity, def entityUrl, def eventDate) {
        //adding address
        ScrapeAddress scrapeAddress = new ScrapeAddress()
        scrapeAddress.setProvince("Rhode Island")
        scrapeAddress.setCountry("UNITED STATES")
        entity.addAddress(scrapeAddress)

        //adding entity url
        if (entityUrl) {
            entity.addUrl(entityUrl)
        }

        //adding event description
        ScrapeEvent event = new ScrapeEvent()
        def status = "This entity appears on the Rhode Island Division of Banking’s list of Enforcement Actions."
        event.setDescription(status)
        if (eventDate) {
            eventDate = eventDate.toString().replaceAll(/\,/, " ").replaceAll(/\./, " ").replaceAll(/\s+/, " ").replaceAll(/Aril/, "April")
            eventDate = context.parseDate(new StringSource(eventDate), ["MMM dd yy", "MM/dd/yy"] as String[])
            event.setDate(eventDate)
        }
        entity.addEvent(event)
    }

    def detectEntity(def name) {
        def type
        if (name =~ /\s(?:NEWPORT|Bank)$/) {
            type = "O"
        }else if (name =~ /^\S+$/) {
            type = "O"
        } else {
            type = entityType.detectEntityType(name)
            if (type.equals("P")) {
                if (name =~ /(?i)(?:Aid|\bCoastway\b|Americas|FS\sCARD|Alternative\sL\.|Creativity|Pharmacy|\bVets\b|\bObesity\b|\bHomeless\b|Sanctuary|Kayla|Change|Purpose|Life|Welfare|Term|Dynamics|Abuse|Cub|Rebound|Speak|Police|Boys|Charities|Opportunity|Pennsylvania|Vision|Mission|Alternatives|Adoptions|Camp|Embassy|Christie|Geek|Fall|Policy|Publications|Cure|Brotherhood|Studios|Forum|Powerkids|Workshop|Hightower|Families|Citizens|Wishes|Nationalist|Brothers|Cancer|Autism|Hope|Americans|Clinic|Medicine|Animals|Future)/) {
                    type = "O"
                }
            }
            if (type.equals("O")) {
                if (name =~ /(?i)(?:stephen\sL.|\bSANDRA\b|Thomas\sShields)|Jason\sHunt/) {
                    type = "P"
                }
            }
        }
        return type
    }

    def pdfToTextConverter(def pdfUrl) {
        def pdfFile = invokeBinary(pdfUrl)
        def pmap = [:] as Map
        pmap.put("1", "-layout")
        pmap.put("2", "-enc")
        pmap.put("3", "UTF-8")
        pmap.put("4", "-eol")
        pmap.put("5", "dos")
        // pmap.put("6", "-raw")
        def pdfText = context.transformPdfToText(pdfFile, pmap)
        return pdfText
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def invoke(url, cache = false, tidy = false) {
        return context.invoke([url: url, tidy: tidy, cache: cache])
    }
}