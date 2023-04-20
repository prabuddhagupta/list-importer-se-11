package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

India_mca_vanishing_companies script = new India_mca_vanishing_companies(context)
script.initParsing()

class India_mca_vanishing_companies {
    ScrapianContext context = new ScrapianContext()
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")


    India_mca_vanishing_companies(context) {
        this.context = context
    }

    def initParsing() {

        def pdfText = pdfToTextConverter("file:////home/maenul/Documents/vanish_south.pdf")
        handlePdfOne(pdfText)
//        println(pdfText)
    }

    def handlePdfOne(def data) {

        data = sanitizePdfOne(data)

        //data += '\n1\n'
        println(data)

        //def matches = context.regexMatches(indexPage, [regex: ">(\\d+)<\\/a><\\/li>"]);

        def blocks = data =~ /(?is)([\s\n]\d{1,3}[\n\s]*\w+.*?\w+\n.*?)(?=-BREAK-)/

//        println(blocks.group())

//        if(blocks){
//            blocks.each {block -> println "==================\n$block"}
//        }

        def blockList = data.split(/-BREAK-/).collect()
        blockList.each {println "============================\n$it"}
        //println "==========================================================================="
        while (blocks.find()) {
            def block = blocks.group(1)

            println("--------------------------------------------------------------------------------------------")
            print(block)
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
        }
    }

    def sanitizePdfOne(def pdf) {
        pdf = pdf.toString().replaceAll(//, '')
//        pdf = pdf.toString().replaceAll(/8\. Karpagambal/,'8\nKarpagambal')
        pdf = pdf.toString().replaceAll(/8 Panggo Exports Ltd/, '8\nPanggo Exports Ltd')
        pdf = pdf.toString().replaceAll(/(?=\s\d\s)/, '-BREAK-')
//        pdf = pdf.toString().replaceAll(/(?ism)Vanishing Companies : Southern Region\s*Name of the\s*Company\(including\s*Issue\s*its old name in\s*Date of\s*Size\s*Names of Promoters\/\s*S.No.\s*Address\s*case the Company\s*Incorporation \(Rs in\s*Directors\s*has changed its\s*Crores\)\s*name\)/, '')

        return pdf.toString().trim()
    }

    def pdfToTextConverter(def pdfUrl) {
        def pdfFile = invokeBinary(pdfUrl)
        def pmap = [:] as Map
        pmap.put("1", "-layout")
        pmap.put("2", "-enc")
        pmap.put("3", "UTF-8")
        pmap.put("4", "-eol")
        pmap.put("5", "dos")
        pmap.put("6", "-raw")
        def pdfText = context.transformPdfToText(pdfFile, pmap)
        return pdfText
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }


}