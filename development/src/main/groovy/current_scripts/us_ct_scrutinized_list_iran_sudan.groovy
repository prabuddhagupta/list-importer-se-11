package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.text.StringEscapeUtils

import java.util.regex.Pattern

/**
 * Date: 11/03/2017
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

US_Ct_Scruntinized script = new US_Ct_Scruntinized(context)
script.initParsing()

class US_Ct_Scruntinized {
    final ScrapianContext context
    final ocrReader
    final String root = "http://www.ott.ct.gov/"
    final String url = root + "business_internationalinvestment.html"
    final String newUrl  = "https://portal.ct.gov/OTT/Doing-Business/International-Investment-Restrictions-by-State-Law"
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    US_Ct_Scruntinized(context) {
        this.context = context
        ocrReader = moduleFactory.getOcrReader(context)
    }

    def initParsing() {
        def html = invoke(newUrl, false)
        //println html
        def pdfUrlMatch = html =~ /(?i)"([^"]+)">(?:sudan|iran)\s+prohibited/

        if(pdfUrlMatch.find()) {
            def name, associations, fullPart;
            def pdfUrl = "https://portal.ct.gov" + pdfUrlMatch.group(1)
            def pdfText = pdfToTextConverterUsingOCR(pdfUrl)
            pdfText = pdfFilter(pdfText)
            pdfText = StringEscapeUtils.unescapeJava(pdfText)
             println "Pdf Text: "+pdfText

            def associationMatch = pdfText =~ /(?ism)(?<=\n)(\w+[^\n]+subsidiaries:)(.*?)\n(?=[^\n]*?subsidiaries:|\w)/
            while (associationMatch.find()) {
                def ass
                def associationArr = []
                fullPart = Pattern.quote(associationMatch.group(0).trim())
                name = associationMatch.group(1).replaceAll(/(?ism)and\s*the\s*following\s+subsidiaries:/, "").trim()
                associations = associationMatch.group(2)

                def splitAssociation = associations =~ /([^\n]+)/
                while (splitAssociation.find()) {
                    if (splitAssociation.group(1) =~ /^\s*[^\)\(]+\)/) {
                        ass = ass + ' ' + splitAssociation.group(1).trim()
                    } else {
                        ass = splitAssociation.group(1).trim()
                    }

                    if (!(ass =~ /(?m)^\s*[^\n\(]+\([^\)]+$/))
                        ass = ass.toString().replaceAll(/(?ism)"/,"").trim()
                    associationArr.add(ass)
                }

                pdfText = pdfText.replaceAll(/${fullPart}/, "")
                createEntity(name, pdfUrl, associationArr)
            }

            //remaining name parsing
            def nameMatch = pdfText =~ /(?ism)\n\w+[^\n]+/
            while (nameMatch.find()) {
                name = nameMatch.group(0)
                createEntity(name, pdfUrl)
            }
        }
    }

    def pdfFilter(def text) {
        text = text.toString()
        text = text.replaceAll(/(?is)^.*?companies:/, "")
        text = text.replaceAll(/(?ism)Sudan[\n|\s]*Pursuant toSection.*?companies:/,"")
        text = text.replaceAll(/(?ism)(?=Corporation|Corp|Petrochemical|Co \.)/," ")
        text = text.replaceAll(/(?ism)(?<=and|Hongdu|Nile|Oil|de)(?=chemical|Aviation|Ganga|Engineering|Venezuela|Caracas)/," ")

        return text
    }

    def createEntity(name, pdfUrl, associationArr = []) {
        def entity = null
        def alias;
        def aliasMatch = name =~ /\(([^\)]+)\)/
        if (aliasMatch) {
            def aliasPart = Pattern.quote(aliasMatch[0][0])
            alias = aliasMatch[0][1]
            name = name.replaceAll(/$aliasPart/, "")

        }
        entity = context.findEntity([name: name, type: "O"])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(sanitize(name))
            entity.type = "O"
        }

        if (alias) {
            entity.addAlias(alias)
        }

        associationArr.each { def association ->
            if (association) {
                //entity.addAssociation(association)
                def aliasList
                aliasList = association.split(/(?i)\(?(?:[fa]\.?k\.?a)\.?/).collect({ it -> it.replaceAll(/\)/, "") })
                for (int i = 0; i < aliasList.size(); i++) {
                    entity.addAssociation(sanitize(aliasList[i]))
                }

                def entity1
                entity1 = context.getSession().newEntity()
                entity1.setName(sanitize(aliasList[0]))
                entity1.type = "O"
                for (int i = 1; i < aliasList.size(); i++) {
                    entity1.addAlias(sanitize(aliasList[i]))

                }
                createEntityCommonCore(entity1, pdfUrl)
            }
        }
        createEntityCommonCore(entity, pdfUrl)
    }


    def createEntityCommonCore(ScrapeEntity entity, pdfUrl) {

//        def eDate
//        if (pdfUrl =~ /(?i)iran/) {
//            eDate = "09/05/2013"
//        } else {
//            eDate = "02/07/2017"
//        }

        def event = new ScrapeEvent()
        event.setDescription("This entity appears on the Connecticut Office of the State Treasurer list of \"scrutinized companies\". The reason for restriction is that they have been determined to have prohibited business operations in Iran and Sudan.")
        event.category = "DEN"
        event.subcategory = "ASC"
        entity.addEvent(event)
        entity.addUrl(pdfUrl)

//        def address = new ScrapeAddress()
//        address.setProvince("Connecticut")
//        address.setCountry("UNITED STATES")
//        entity.addAddress(address)
    }


//------------------------------Misc utils part---------------------//
    def invoke(url, cache = false, tidy = true, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = true, clean = false, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }

    def pdfToTextConverter(pdfUrl) {
        def pdfFile = invokeBinary(pdfUrl)

        def pmap = [:] as Map
        pmap.put("1", "-layout")
        pmap.put("2", "-enc")
        pmap.put("3", "UTF-8")
        pmap.put("4", "-eol")
        pmap.put("5", "dos")
        //pmap.put("6", "-raw")

        def pdfText = context.transformPdfToText(pdfFile, pmap)
        return pdfText
    }

    def pdfToTextConverterUsingOCR(def pdfUrl) {
        List<String> pages = ocrReader.getText(pdfUrl)
        return pages
    }

    def sanitize(data) {
        return data.replaceAll(/\s+/, " ").replaceAll(/\\Q/, "").replaceAll(/\\E/, "").trim()
    }
}