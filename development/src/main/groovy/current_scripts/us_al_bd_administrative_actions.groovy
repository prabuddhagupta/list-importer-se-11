package current_scripts



import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent


context.setup([connectionTimeout: 50000, socketTimeout: 50000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_al_bd_administrative_actions ab=new Us_al_bd_administrative_actions(context)
ab.initParsing()

class Us_al_bd_administrative_actions {
    final ScrapianContext context
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final entityType
    static def rootUrl = "http://www.banking.alabama.gov/"
    def actionUrl = "admin_actions/"
    def mainUrl = "http://www.banking.alabama.gov/bol_admin_action.aspx"

    def html, current_catagory, parentNo, issueDate, entityUrl,eventDes,indexN=0,indexD=0, ent_name

    def catagoryName = []//full code Intact
    def catagory_urls = []//full code---> intact
    def nxt_pg_urls = []//lastly 0
    def pdf_urls_list = []//size 217
    def alias=[]
    def nameMemo=["AAA Sales Inc.\n D.B.A Loan Pro Co. Inc","BAnctrus Morgage Company, LLC.","Christopher Collins","Stevenson Mortgage Consultant LLC","First Trust Mortgage Corp. of America Inc.","Gulfside Mortgage Inc.","RN Reality & associates","Shelby lynn jones","1st Continental Mortgage, Inc.",
                  "Michael R. Pinkard","Dwayne Holcombe","Charles Trujillo","Ceceliya L. W. Boyd","Ashley L. Henderson","Omeshia K. Pettaway","Crystal C. Thomas","Sonja R. Hornbuckle",
                  "Brenda K. Smith","William Lazel MCKinstry","Kathleen G. Palo","Johnathan Goods","Haley Patterson","Sharon Butler Alexander","Rodert Jeffry Gish","Melanie Cranford","Roxana Quintero", "Heather Acton","Brandon James Jones","Gail R Allen","Pamela Bass","Cathy G. Clark","Leigh Ann Smith","Carol Creel","Demarko Antoine King","Jhermira Dickerson","James K. Real","Mayra Rivera","Rinada Cole"]

    def dateMemo=["6-August-2004","3-March-2003","15-August-2006","11-Decemder-2006","6-June-2007","26-September-2011","28-September-2012","27-March-2017","27-march-2017","15-November-2017","11-May-2020","12-August-2020","19-July-2006","17-october-2006","19-November-2007","2-July-2007","14-may-2009","9-october-2012","25-september-2013","18-November-2013","18-November-2013","7-november-2014","30-December-2014","13-January-2016","25-may-2016","12-september-2016","3-october-2016","17-october-2018","17-august-2020","25-June-2014",
                  "16-march-2014","22-June-2017","18-june-2009","27-april-2010","21-october-2010","4-october-2010","2-april-2013","23-january-2015","17-August-2015","28-October-2014","7-June-2013","12-December-2012","16-March-2009"]

    def nameList=[]
    Us_al_bd_administrative_actions(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
        ocrReader = moduleFactory.getOcrReader(context)
        //  parse_PDF("http://www.banking.alabama.gov/pdf/orders/Consent/2017/Will_Worthington_Total_Media_Management%20LLC_9_25_17.pdf")
    }

    def initParsing() {
        html = invoke(mainUrl)
        Thread.sleep(2000)
        getParentUrls()

        int c = 0
        while (c <catagory_urls.size()) {
            current_catagory = catagoryName[c]
            parentNo=c
            main_method(c)
            c++
        }
    }

    //the method stores catagoryNames and the links to further pages from the mainUrl Page

    def getParentUrls() {
        def content_body = html =~ "(?ism)<div.+?_body.+?>(.+?)<\\/div>"

        if (content_body.find()) {
            def div = content_body.group(1)
            def href = div =~ "(?ism)<a.+?\\/(.+?)\">(.+?)<|<a.+?\"(\\w+\\/*\\.\\w+)\">(.+?)<"


            while (href.find()) {
                if (href.group(1) != null) {
                    catagory_urls.add(href.group(1))
                    catagoryName.add(href.group(2))
                } else {
                    catagory_urls.add(href.group(3))
                    catagoryName.add(href.group(4))
                }
            }
        }

    }
//this method traverses all internal links upto EntityUrls(PDF)
    def main_method(int parent) {

        def url = rootUrl + catagory_urls[parent]
        def newPage = invoke(url)
        def content_body = newPage =~ "(?ism)<div.+?_body.+?>(.+?)<\\/div>"

        if (content_body.find()) {
            def div = content_body.group 1
            def href = div =~ "(?ism)<a.+?href.+?\"\\.*\\/*(.+?)\""

            while (href.find()) {
                def item = href.group(1)
                nxt_pg_urls.add(item)
            }
        }
        while (nxt_pg_urls.size() > 0) {
            sanitize_urls(parent)
        }
        nxt_pg_urls.clear()
    }
    //this sanitization is needed due to different urls pattern
    def sanitize_urls(int parent) {
        def url, tail
        tail = nxt_pg_urls.pop()

        if (parent == 4) {
            url = rootUrl + tail
        } else if (parent == 1 && !tail.contains("revocation_orders/")) {
            url = rootUrl + actionUrl + "revocation_orders/" + tail
        } else {
            url = rootUrl + actionUrl + tail
        }

        makePdfList(url, parent)

    }

// this method make the PDF lists for data reading
    def makePdfList(def url, def parent) {
        def pdfUrl
        def newPage = invoke(url)
        def content_body = newPage =~ "(?ism)<div.+?_body.+?>(.+?)<\\/div>"


        if (content_body.find()) {
            def capture = content_body.group(1)
            def pdfMatcher = capture =~ "(?ism)href.+?\\/*(pdf.+?)\""


            while (pdfMatcher.find()) {
                def pdf_part = pdfMatcher.group(1)
                if (parent == 0) {
                    pdf_part = pdf_part.replaceAll("\\s", "%20")
                    if (pdf_part.contains("%20LLC.pdf")) {
                        pdf_part = pdf_part.replaceAll("%20LLC.pdf", "%20%20LLC.pdf")
                    }
                    else if(pdf_part.contains("%20Inc..pdf")&&!pdf_part.contains("%20%20Inc")){
                        pdf_part = pdf_part.replaceAll("%20Inc..pdf", "%20%20Inc..pdf")
                    }
                    pdfUrl = rootUrl + pdf_part
                    pdf_urls_list.add(pdfUrl)

                } else {

                    if (pdf_part.contains(" ")) {
                        pdf_part = pdf_part.replaceAll("\\s", "%20")
                    }
                    else if(pdf_part.contains("amp;")){
                        pdf_part=pdf_part.replaceAll("amp;","")
                    }
                    // URLs invocation has been checked
                    pdf_urls_list.add(rootUrl + pdf_part)

                }

                parse_PDF(rootUrl + pdf_part)
            }
        }

    }

    def mismatch=[]
    def parse_PDF(def url) {
        alias=[]
        nameList=[]
        def name=""
        def d=""
        entityUrl = url
        eventDes = "This entity appears on the Alabama Banking Department's list of Administrative Actions. $current_catagory."
        if(url=="http://www.banking.alabama.gov/pdf/orders/Consent/2017/Will_Worthington_Total_Media_Management%20LLC_9_25_17.pdf"){
            name="WILL WORTHINGTON"
            createEntity(name,eventDes,issueDate,alias,entityUrl)
            name="TOTAL MEDIA MORTGAGE"
            createEntity(name,eventDes,issueDate,alias,entityUrl)
            name="TOTAL MEDIA MANAGEMENT LLC"
            alias.add("NEW SOUTH MORTGAGE")
            createEntity(name,eventDes,issueDate,alias,entityUrl)
        }
        else {
            def pdf = pdfToTextConverter(url.replaceAll(/http:\/\//, "https://"))
            // //(pdf)
            def namereg1 = pdf =~ /(?sm)MATTER\sOF(.+?)CEASE\s\w+\sDESI\w+\sORDER|(?ism)\w+r\sof\W*(.+?)\)\)\n/
            def namereg2 = pdf =~ /(?ism)\w+a\w+r\sof\W*(.+?)ORE*DER|(?ism)\W*Complainant,\W*v..+?\n(\w+.+?)\W*Respondent.+?/
            def namereg3 = pdf =~ /(?ism)Matter\sof\W*(.+)\)\n|(?ism)^\W*I*n\sRE\W+(\w+.+)\n\W*ORDER/
            def namereg4 = pdf =~ /(?ism)matter\sof\W+(.+)\)\)/
            def datereg = pdf =~ /(?ism)this\s(\d+).+?day\sof.+?(\w+).+?(\d{4})\W*/

            if (namereg1.find()) {
                if (namereg1.group(1) != null) {
                    name = namereg1.group(1)

                } else if (namereg1.group(2)) {
                    name = namereg1.group(2)

                }
                //////("---1"+name)
            } else if (namereg2.find()) {
                if (namereg2.group(1) != null) {
                    name = namereg2.group(1)

                } else if (namereg2.group(2)) {
                    name = namereg2.group(2)
                }
                //////("---2"+name)
            } else if (namereg3.find()) {
                if (namereg3.group(1) != null) {
                    name = namereg3.group(1)
                } else if (namereg3.group(2)) {
                    name = namereg3.group(2)
                }
                //////("---3\n$pdf\n"+name)
            } else if (namereg4.find()) {
                name = namereg4.group(1)
                //////("---4"+name)
            } else {
                name = nameMemo[indexN++]
                mismatch.add(url)
            }
            if (name != null) {
                sanitize_name(name)
            }

            if (datereg.find()) {
                d = dateMemo[indexD++]
                //(d)
                issueDate = convertMonth(d)
                //add(issueDate)
            }


            int i = 1
            while (!nameList.empty) {
                def nm = nameList.pop()
                if (i == 1) {
                    createEntity(nm, eventDes, issueDate, alias, entityUrl)
                } else {
                    def noAlias = []
                    createEntity(nm, eventDes, issueDate, noAlias, entityUrl)
                }
                i++;
            }
        }
    }

//Sanitize Name and Alias

    def sanitize_name(String input){

        if (input.contains("SHELBY")) {
            input = nameMemo[indexN++]
        }
        if(input.contains("APLA")) {
            input=nameMemo[indexN++]
        }
        input = input.replaceAll(/(?is)BOL\s.+\d\W+\w*\b|(?i)Case.+\d\W+\b|(?i)license.+?\d\s|(?is)MB\W+.+\d\w*\b|(?is)MC\W+.+\d\w*\W|(?i)ASB\sD.+\d\s|MLO.+?\d\s|(?i)Denial Notice|FRB.+SMB|(?i)INDIVIDUALLY.*/, "").trim()
        input = input.replaceAll(/(?is)\bNo\W+.+\d\b/, "")
        input = input.replaceAll(/(?ism)Further.+|(?ism)RESPONDENT.*|a former officer|FILE|DENIAL NOTICE|ORDER OF PROHIBITION|FROM|REVOCATION\s*NOTICE|(?i) CEASE\s.+?DESIST|(?i)CONSENT|Consent Agreement/, "").trim()
        input = input.replaceAll(/(?i)administrative.+?order?|(?i)administrative|&amp;|ORDER|(?)Agreement|FINAL/, "").trim()
        input = input.replaceAll(/(?ism)n\/a.+|[\)\#]|(?i)No\s*|(?i)\W+of\W*|,$/, "").trim()
        input = input.replaceAll(/[\)\:\~\#]/, "").trim()
        input = input.replaceAll(/\b(?ism)Mr.\s*\b|(?i)President|CEO/, "").trim()
        input = input.replaceAll(/(?i)NICALLISTER MORTGAGE GROUP  B/, /MCALLISTER MORTGAGE GROUP/)
        input = input.replaceAll(/\s\d{1,3}$|&apos;|(?i)\band\b/, "").trim()
        input = input.replaceAll(/(?ism)\d+\W+\w+.+\d+|(?ism)\d+\W+.+(Street|road).*|(?i)\bits\b|^,\s*|\w+\W+albama\W*$/, "").trim()
        input=input.replaceAll(/(?ism)\nCorporation/," CORPORATION")
        input=input.replaceAll(/(?i)BUREAUAMERIC/,"BUREAU Of America")
        input=input.replaceAll(/(?i)A\/ \/A HARE MORTGAGE LLC/,"A/K/A HARE MORTGAGE LLC")
        input=input.replaceAll(/(?i)ALLIANCE HOME MORTGAGE CO,, INC.,/,"ALLIANCE HOME MORTGAGE CO INC")
        input=input.replaceAll(/ALFRED PICCININ./,"ALFRED PICCININI")
        //////(input)
        def aliasregex=/(?ism)a\Wk*\Wa\s+\w+.+|(?ism)D\WB\WA\W*\s.+|(?ism)also\s\w+\sas\s\w+.+/
        def aliasL = input.findAll(/(?ism)a\Wk*\Wa\s+\w+.+|(?ism)D\WB\WA\W*\s.+|(?ism)also\s\w+\sas\s\w+.+/)

        while (!aliasL.empty) {
            def elem = aliasL.pop().toLowerCase().replaceAll(/(?s)\s+|,$/, " ").trim()
            elem=elem.replaceAll(/[\&]|(?i)\band\b/,"a.k.a")
            alias = elem.split(/a\Wk*\Wa\W|d\Wb\Wa\W*|also\s\w+\sas\W*\w+/)
        }
        input=input.replaceAll(/(?ism)a\Wk*\Wa\s+\w+.+|(?ism)D\WB\WA\W*\s.+|(?ism)also\s\w+\sas\s.+/,"").trim()
        //(" ----------------- \n"+alias)
        def lines = input.split("\n")
        int l = lines.length
        int i = 0

        while (i < l) {
            def n = lines[i]
            //  //(n)
            n = n.replaceAll(/^\W+|(?s)\s+|\d+$|,$/, " ").trim()
            def check=n=~/^\W+$|^$/
            if (!check) {
                nameList.add(n.toUpperCase())
                //  //("popping ---> "+nameList.pop())
            }
            i++
        }

    }
    def convertMonth(String d){
        def arr=d.split(/-/)
        String month
        def input=arr[1].toLowerCase()
        if(input.contains("january")){month="01"}
        else if(input.contains("february")){month="02"}
        else if(input.contains("march")){month="03"}
        else if(input.contains("april")){month="04"}
        else if(input.contains("may")){month="05"}
        else if(input.contains("june")){month="06"}
        else if(input.contains("july")){month="07"}
        else if(input.contains("august")){month="08"}
        else if(input.contains("september")){month="09"}
        else if(input.contains("october")){month="10"}
        else if(input.contains("november")){month="11"}
        else if(input.contains("december")){month="12"}
        else{
            return "INVALID"
        }
        return arr[0]+"-"+month+"-"+arr[2]
    }

    def getType(def ent){
        if(ent=~/(?i)L\s*\w\s*C|(?i)I\s*N\s*C|(?i)Services|(?i)Mortgage|(?)Group|(?i)Bank|(?i)Corporation|Co.|&|ASSOCIATES/) {
            return "O"
        }
        else {
            return entityType.detectEntityType(ent)
        }

    }
//----------------------------------------------------------Entity Creation-----------------------------------------------------------------//



    def createEntity(def name, def eventDes, def date, def aliasList,def pdfurl) {
        def entity = null
        entity = context.findEntity([name: name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
        }

        entity.addUrl(pdfurl)

        def Etype = getType(name)
        entity.setType(Etype)

        aliasList.each {
            if (it) {
                it = it.replaceAll(/(?i)\bno\W+\b|\,\s*$/, "").trim()
                entity.addAlias(it.toUpperCase())
                ////////("Inserted A L I A S \n $it")
            }
        }

        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(eventDes)
        if (date) {
            date = context.parseDate(new StringSource(date), ["dd-MM-yyyy"] as String[])
            event.setDate(date)
        }
        entity.addEvent(event)
        //Address
        ScrapeAddress address = new ScrapeAddress()
        address.setProvince("Alabama")
        address.setCountry("UNITED STATES")
        entity.addAddress(address)
    }




//-------------------------------FrameWorks--------------------------------------------/

    def pdfToTextConverter(def pdfUrl) {
        try {
            List<String> pages = ocrReader.getText(pdfUrl)
            return pages
        }
        catch (NoClassDefFoundError e){
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
        catch(IOException e){
            return "PDF has no page"
        }
        catch (Exception e){
            Thread.sleep(10000)
            return "-- RUNTIME ERROR --- $e.message ---"
        }

    }

    def invokeBinary(url, type = null, paramsMap = null, cache = false, clean = true, miscData = [:]) {
        //Default type is GET
        Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
        dataMap.putAll(miscData)
        return context.invokeBinary(dataMap)
    }


    def invoke(url, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}