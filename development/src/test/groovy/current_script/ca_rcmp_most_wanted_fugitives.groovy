package current_script

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.Tasker
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent
import java.time.YearMonth
import org.apache.commons.lang.StringUtils
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.concurrent.ThreadPoolExecutor

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

CaRcmpMostWantedFugitives script = new CaRcmpMostWantedFugitives(context)
script.initParsing()

class CaRcmpMostWantedFugitives{

    static urlInput = "http://www.rcmp-grc.gc.ca/en/wanted";
    def urlRoot = "http://www.rcmp-grc.gc.ca/en/"

    final ScrapianContext context
    final Tasker tasker

    CaRcmpMostWantedFugitives(context) {
        this.context = context
        // addressParser = moduleFactory.getGenericAddressParser(context)
    }

    private enum FIELDS
    {
        URL, DOB, POB, EYE_COLOR, GENDER, HAIR_COLOR, HEIGHT, IMG_URL, EVENT, SCARS, WEIGHT
    }


//------------------------------Initial part----------------------//
    def initParsing() {
        def html = invoke(urlInput)
        handleDetailsPage(html)
    }

    def getHtml(detailUrl) {
        def html = invoke(detailUrl)
        return html
    }

    def handleDetailsPage(html) {
        def url
        def detailUrl
        def urlMatch = html =~ /<a href="((?:wanted)[^"]+)"/
        while (urlMatch.find()) {
            url = urlMatch.group(1)
            detailUrl = urlRoot + url
            detailInfoCollect(detailUrl)
        }
    }

    def detailInfoCollect(detailUrl){

        def valMap = [:]
        def name
        def alias
        def detailUrlHtml
        detailUrlHtml = getHtml(detailUrl)
        detailUrlHtml = detailUrlHtml.replace("&#160;"," ")

        // name
        def nameMatch = detailUrlHtml =~ /<h1(?:[^>]+>)([^<]*)/ ///\bName.*?value">(.+?)</
        if ( nameMatch.find()){
            name = nameMatch.group(1)
        }

        // alias
        def aliasMatch = detailUrlHtml =~   /Aliases:\s*(?:<[^>]*>)*([^<]*)/
        if ( aliasMatch.find()){
            alias = sanitize(aliasMatch.group(1))
        }

        // image url
        def urlImgMatch = detailUrlHtml =~ /(?i)<img.*?\\/([^"]+(?:jpg|png))"/
        if (urlImgMatch.find()){
            def urlImg = urlImgMatch.group(1)
            valMap[FIELDS.IMG_URL] = urlRoot + urlImg
        }

        // dob
//        def dobMatch = detailUrlHtml =~ /Born:\s*(?:<[^>]*>)*([^<]*)/
//        if ( dobMatch.find()){
//            def dob = dobMatch.group(1)
////            Date date = new Date(dob)
////            SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy")
////            dob= formatter.format(date)
////            valMap[FIELDS.DOB] = dob
//
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
//            dob = YearMonth.parse(dob, formatter)
//            valMap[FIELDS.DOB] = dob
//        }

        // pob
        def pobMatch = detailUrlHtml =~ /Place of Birth:\s*(?:<[^>]*>)*([^<]*)/
        if ( pobMatch.find()){
            def pob = sanitize(pobMatch.group(1))
            valMap[FIELDS.POB] = pob
        }

        // height
        def heightMatch = detailUrlHtml =~ /Height:\s*(?:<[^>]*>)*([^<]*)/
        if ( heightMatch.find()){
            def height = heightMatch.group(1)
            valMap[FIELDS.HEIGHT] = height
        }

        // weight
        def weightMatch = detailUrlHtml =~ /Weight:\s*(?:<[^>]*>)*([^<]*)/
        if ( weightMatch.find()){
            def weight = weightMatch.group(1)
            valMap[FIELDS.WEIGHT] = weight
        }

        // eye color match
        def eyeMatch = detailUrlHtml =~ /Eye Colour:\s*(?:<[^>]*>)*([^<]*)/
        if ( eyeMatch.find()){
            def eye = eyeMatch.group(1)
            valMap[FIELDS.EYE_COLOR] = eye
        }

        // hair color match
        def hairMatch = detailUrlHtml =~ /Hair Colour:\s*(?:<[^>]*>)*([^<]*)/
        if ( hairMatch.find()){
            def hair = hairMatch.group(1)
            valMap[FIELDS.HAIR_COLOR] = hair
        }

        // gender / sex
        def genderMatch = detailUrlHtml =~ /Sex:\s*(?:<[^>]*>)*([^<]*)/
        if ( genderMatch.find()){
            def gender = sanitize(genderMatch.group(1))
            valMap[FIELDS.GENDER] = gender
        }

        // scars / marks
        def scarsMatch = detailUrlHtml =~ /(?:Scars|Tattoos):\s*(?:<[^>]*><ul><li>)*(.*?)<\\/ul>/
        if ( scarsMatch.find()){
            def scars = sanitize(scarsMatch.group(1))
            valMap[FIELDS.SCARS] = scars
        }

        // event description
        def eventMatch = detailUrlHtml =~ /WANTED<(?:.*list-group-item">)([^<]*)/
        if ( eventMatch.find()){
            def eventDescription = sanitize(eventMatch.group(1))
            valMap[FIELDS.EVENT] = eventDescription
        }

        // detail url
        valMap[FIELDS.URL] = detailUrl  //WANTED<(?:.*list-group-item">)([^<]*)

        createEntity(name,alias,valMap)
    }

    def createEntity(def name, def alias, def map){

        def entity
        if (entity == null) {
            entity = context.getSession().newEntity()
            entity.setName(name)

//            if(alias){
//                alias.split ("/").each{
//                    def nameSwapMatch = it =~ /(.*?),(.*)/
//                    if(nameSwapMatch.find())
//                        it = nameSwapMatch.group(2)+" "+nameSwapMatch.group(1)
//
//                    entity.addAlias(it)
//                }
//            }

            entity.setType("P")

            if(map[FIELDS.URL]){
                entity.addUrl(map[FIELDS.URL])
            }

            if(map[FIELDS.HAIR_COLOR]){
                entity.addHairColor(map[FIELDS.HAIR_COLOR])
            }

            if(map[FIELDS.EYE_COLOR]){
                entity.addEyeColor(map[FIELDS.EYE_COLOR])
            }

            if(map[FIELDS.HEIGHT]){
                entity.addHeight(map[FIELDS.HEIGHT])
            }

            if(map[FIELDS.WEIGHT]){
                entity.addWeight(map[FIELDS.WEIGHT])
            }

            if(map[FIELDS.IMG_URL]){
                entity.addImageUrl(map[FIELDS.IMG_URL])
            }

            if(map[FIELDS.DOB]){
                entity.addDateOfBirth(map[FIELDS.DOB])
            }

//            if(map[FIELDS.POB]){
//                ScrapeAddress obj = new ScrapeAddress();
//                obj.setAddress1(map[FIELDS.STREET])
//                obj.setProvince(map[FIELDS.PROVIENCE])
//                obj.setCity(map[FIELDS.CITY])
//                obj.setCountry(map[FIELDS.COUNTRY])
//                obj.setPostalCode(map[FIELDS.POSTAL_CODE])
//                entity.addAddress(obj)
//            }

//            if(map[FIELDS.SCARS]){
//                //(?:<[^>]+>)+
//                map[FIELDS.SCARS].split (/(?:<[^>]+>)+/).each{
//                    entity.addScarsMarks(it)
//                }
//
//            }

            if(map[FIELDS.GENDER]){
                entity.addSex(map[FIELDS.GENDER])
            }

            def description = ( "This entity appears on the Royal Canadian Mounted Police List of Most Wanted Fugitives. Wanted For: " + map[FIELDS.EVENT])
            if(description){
                ScrapeEvent obj = new ScrapeEvent()
                obj.setDescription(sanitize(description))
                entity.addEvent(obj)
            }
        }
    }

    def invoke(url, isPost = false, isBinary = false, cache = true, postParams = [:], headersMap = [:], cleanSpaceChar = false, tidy = false, miscData = [:]) {
        Map data = [url: url, tidy: tidy, headers: headersMap, cache: cache, clean: cleanSpaceChar]
        if (isPost) {
            data.type = "POST"
            data.params = postParams
        }
        data.putAll(miscData)

        try {
            if (isBinary) {
                return context.invokeBinary(data)
            } else {
                return context.invoke(data)
            }

        } catch (InterruptedException ignored) {
            System.err.println("Interrupted exception for: " + url)
            throw new Exception(ignored)

        } catch (e) {
         /*   def executor = tasker.getExecutorService()
            if (executor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor) executor).setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
            }
            executor.shutdownNow()*/
            System.err.println("Error invoking: " + url)
            throw new Exception(e)
        }
    }

    def sanitize(data) {
        return data.replaceAll(/&amp;/, '&').replaceAll(/&quot;/,"\"").replaceAll(/;/," ").replaceAll(/\r\n/, "\n").replaceAll(/(?s)\s{2,}/, " ").replaceAll(/(?i)(?:N\\/A|Unknown)/, "").trim()
    }
}

