package test

import com.rdc.importer.scrapian.ScrapianContext
import com.se.rdc.core.ScrapianContextSE

/**
 * Created by ws5103 on 4/27/17.
 */
context = new ScrapianContextSE();
context.setup([connectionTimeout: 20000, socketTimeout: 20000, retryCount: 1])
context.session.encoding = "UTF-8"; //change it according to web page's encoding
context.session.escape = true
AdaptiveAddressParser1 script = new AdaptiveAddressParser1(context)
script.init()
public class AdaptiveAddressParser1 {
    private ScrapianContext context;
    HttpClient client = new HttpClient()
    String googleUrl="https://www.google.com/search?q=zawad"

    static enum TOKENS {
        DELIMITER, COUNTRY, STATE, CITY, ZIP, COUNTRY_CODE, ADDR_STR
    }

    AdaptiveAddressParser1(ScrapianContext context){
        this.context = context
    }

    public void init(){
        def html = client.sendGet(googleUrl)

        println(html)



    }
    def invoke(url, cache = true, tidy = true, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def invokePost(url, paramsMap, headersMap = [:], cache = true, tidy = true, miscData = [:]) {
        Map dataMap = [url: url, type: "POST", params: paramsMap, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def invokeBinary(url, type = null, paramsMap = null, cache = true, clean = false, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }
}
