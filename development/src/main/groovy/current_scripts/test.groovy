package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader

/**
 * Date: 20/09/2018
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Test script = new Test(context)
script.initParsing()

class Test {
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("92e68ce6243d668ff4c8d665108d88dfc0b633ce")
    final ScrapianContext context
    final String mainUrl = "https://www.rijksoverheid.nl/binaries/rijksoverheid/documenten/rapporten/2015/08/27/nationale-terrorismelijst/ned-terrorismelijst.ods"

    //TODO: remove this if address-parser is not used
    final addressParser

    final String url = "https://applications.labor.ny.gov/EDList/searchResults.do"

    Test(context) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
    }

    def initParsing() {
        def text = "Showanomori Technology Center\n" +
            "1-1-110 Tsutsujigaoka, Akishima-shi\n" +
            "Tokyo 196-0012\n" +
            "Japan"

        def parsedAddress = addressParser.parseAddress([text: text, force_country: true])
        println("D")
    }



    def invoke(url, cache = false, headersMap = [:], cleanSpaceChar = false, tidy = false, miscData = [:]) {
        Map data = [url: url, tidy: tidy, headers: headersMap, cache: cache, clean: cleanSpaceChar]
        data.putAll(miscData)
        return context.invoke(data)

    }

}