import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent

/**
 * Date: Mar 19, 2017
 *
 */

context.setup([connectionTimeout: 20000, socketTimeout: 30000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

EU_COMP_ANTITRUST_Case script = new EU_COMP_ANTITRUST_Case(context)
script.initParsing()


class EU_COMP_ANTITRUST_Case {
    final ScrapianContext context
    final String root = "http://ec.europa.eu/competition/elojade/isef/"
    //final String url =  "https://ec.europa.eu/competition/elojade/isef/index.cfm?clear=1&policy_area_id=1"
    final String url = "https://ec.europa.eu/competition/elojade/isef/index.cfm?fuseaction=dsp_result&policy_area_id=1"
    //final String url = "https://webanalytics.europa.eu/ppms.php?action_name=Competition%20Policy&idsite=cb2bdfa0-e173-4518-aad6-cc25628a594f&rec=1&r=525666&h=20&m=10&s=22&url=https%3A%2F%2Fec.europa.eu%2Fcompetition%2Felojade%2Fisef%2Findex.cfm%3Ffuseaction%3Ddsp_result%26policy_area_id%3D1&urlref=https%3A%2F%2Fec.europa.eu%2Fcompetition%2Felojade%2Fisef%2Findex.cfm%3Fclear%3D1%26policy_area_id%3D1&_id=4dc95f472542bee7&_idts=1661346962&_idvc=2&_idn=0&_viewts=1661350150&send_image=1&pdf=1&qt=0&realp=0&wma=0&dir=0&fla=0&java=0&gears=0&ag=0&cookie=1&res=1536x864&dimension1=en&gt_ms=464&pv_id=E9IUPz"

    EU_COMP_ANTITRUST_Case(context) {
        this.context = context
    }

    private enum FIELDS
    {
        EVNT_DATE, EVNT_DETAILS, ASSOCIATIONS, REMARKS, URL
    }

//------------------------------Initial part----------------------//
    def initParsing() {
        def html = invoke(url)
        //println "Html Source: " + html
        def export_list_id_Match = html =~ /name="export_list_id" value="(.*)"/
        def export_list_id

        if (export_list_id_Match) {
            export_list_id = export_list_id_Match[0][1]
            println "List: " + export_list_id
        }

        def fieldnames_Match = html =~ /name="fieldnames" value="(.*)"/
        def fieldnames

        if (fieldnames_Match) {
            fieldnames = fieldnames_Match[0][1]
            fieldnames = fieldnames.toString().trim().replaceAll(/^null$/, "")
            println "Fields: " + fieldnames
        }

        def fromrow_Match = html =~ /value="Next".*case_code asc',\s(\d+)/
        int fromrow

        if (fromrow_Match) {
            fromrow = Integer.parseInt(fromrow_Match[0][1])
            println "From row: " + fromrow
        }
        def last_Match = html =~ /value="Last".*case_code asc',\s(\d+)/
        int last

        if (last_Match) {
            last = Integer.parseInt(last_Match[0][1]) + 50
            println "Last row: " + last
        }

        while (fromrow <= 51) {
            def params = getParams(String.valueOf(fromrow), fieldnames, export_list_id)
            def html2 = invokePost("https://ec.europa.eu/competition/elojade/isef/index.cfm", params, false, getHeader(), false)
            // println "Html Source: " + html2
            fromrow += 50;
            // println "From row: " + fromrow
            getLinks(html2)
        }

        getLinks(html)
    }

    def getLinks(def html) {
        //def iterMatch = html =~ /(?s)Antitrust\s*cases\s*by\s*company(.*?)<br>\s*<br>/
        def iterMatch = html =~ /(?s)<table class="list">(.*)<\/table>/

        if (iterMatch) {
            // println iterMatch
            def iterLinks = iterMatch[0][1].toString().trim()
            if (iterLinks) {
                def letterLinksMatch = iterLinks =~ /<a\s*href="([^"]+)/

                while (letterLinksMatch.find()) {
                    def url = root + letterLinksMatch.group(1).toString().trim()
                    if (url.contains("AT_")) {
                        html = invoke(url, false)
                    }
                    //def dataMatch = html =~ /(?is)<br>\n<br>(.*?)<\/div>/
                   // def dataMatch = html =~ /(?s)<table class="details">(.*?)<\/table>/
                    def dataMatch = html =~ /(?s)<strong>(\s*AT\.\d+.+)<\/table>/

                    if (dataMatch) {
                        println "Data Match: " + dataMatch
                        def data = dataMatch[0][1].toString().trim()
                        println data
                        data = data.replaceAll(/(?is)(?:&nbsp;){1,6}/, "")
                        def entityMatch = data =~ /(?s)(?:&nbsp;){4}.*?<br>\n<br>/

                        while (entityMatch.find()) {
                            println "Entity Match: " + dataMatch
                            handleDetailsPage(entityMatch.group(0).toString())
                        }
                    }
                }
            }
        }
    }

    def handleDetailsPage(data) {
        def name
        def urlList = []
        //	def documentUrlList = []
        def attrMap = [:]
        //def assocList = []
        //	def remarksList = []
        def eventDescList = []
        def eventDateList = []

        def nameMatch = data =~ /(?is)(?:&nbsp;){4}\n(.*?)<br>/
        if (nameMatch) {
            name = nameMatch[0][1].toString().trim()
        }

        def urlMatch = data =~ /(?is)<a\s*href="([^"]+)/
        while (urlMatch.find()) {
            def entityUrl = root + urlMatch.group(1).toString().trim()
            urlList.add(entityUrl)
        }
        urlList.each { url ->
            def entityData = invoke(url)
            /*def assocMatch = entityData =~ /(?is)Companies:(.*?)<\/tr>/
            def assocData
            if (assocMatch) {
                assocData = assocMatch[0][1].toString().trim()
            }
            assocData = assocData.replaceAll(/<[^>]+>/, "")
            assocData = assocData.replaceAll(/<\/w+>/, "")
            assocData = assocData.replaceAll(/\s+/, " ")

            def tempList = assocData.toString().split(/\/(?=\s+\w{2,})/)
            assocList.addAll(tempList)*/

            /*	def docUrlMatchBlock = entityData =~ /(?is)Document<\/th>(.*?)<\/table>/

                if (docUrlMatchBlock) {
                    def dataBlock = docUrlMatchBlock[0][1].toString().trim()
                    def docurlMatch = dataBlock =~ /<a\s*href="([^"]+)/

                    while (docurlMatch.find()) {
                        documentUrlList.add(docurlMatch.group(1).toString().trim())
                    }
                }*/

            /*		def remarkMatch = entityData =~ /(?is)Economic\s*Activity.*?<\/a>([^<]+)/

                    if (remarkMatch) {
                        def remark = remarkMatch[0][1].toString().trim()
                        if (remark) {
                            remarksList.add(remark)
                        }
                    }*/
            def eventDesc = "This entity appears on the EU European Commission\'s list of Competition Antitrust Cases"
            def eventBlockMatch = entityData =~ /(?is)Economic\s*Activity:<[^>]+>(.*?)<\/td>/
            def eventData
            if (eventBlockMatch) {
                eventData = eventBlockMatch[0][1].toString().trim()

                def eventDataMatch = eventData =~ /<\/a>([^<]+)/

                while (eventDataMatch.find()) {
                    def reasonData = eventDataMatch.group(1).toString().trim()
                    reasonData = reasonData.replaceAll(/^\s*\-/, "")

                    if (reasonData) {
                        eventDescList.add(eventDesc + " for" + reasonData)
                    }
                }
            } else {
                eventDescList.add(eventDesc)
            }

            def eventDateMatch = entityData =~ /(?is)eventsTdDate">([^<]+)/
            if (eventDateMatch) {
                def eventDate = eventDateMatch[0][1].toString().trim()
                eventDate = fixDate(eventDate)
                eventDate = eventDate.replaceAll(/\./, "/")

                eventDateList.add(eventDate)
            }

            // This following commented code block is left for future reference only.

            /*def eventDateBlockMatch = entityData =~ /(?is)(eventsTdDate">[\d\.]+<\/td>\s*<td\s*class="eventsTdDocType">Press\s*Release.*?<\/tr>)/
            def eventDate
            if (eventDateBlockMatch) {
                int maxSize = eventDateBlockMatch.count
                def eventDateData = eventDateBlockMatch[maxSize - 1][1].toString().trim()
                def eventUrlMatch = eventData1 =~ /<a\s*href="([^"]+)/

                if (eventUrlMatch) {
                    def eventurl = eventUrlMatch[0][1].toString().trim()
                    eventurl = urlSanitize(eventurl)
                    def urlData
                    if (eventurl =~ /(?i)\.pdf$/) {
                        urlData = pdfToTextConverter(eventurl)
                    } else {
                        if (!(eventurl =~ /en:PDF/))
                            urlData = invoke(eventurl)
                    }

                    def titleMatch = urlData =~ /(?s)<title>(.*?)<\/title>/
                    if (titleMatch) {
                        def title = titleMatch[0][1].toString().trim()
                        title = title.replaceAll(/European Commission\s*\-\s*PRESS RELEASES\s*\-\s*Press\s*release\s*\-\s/, "")
                        title = title.replaceAll(/\./, "")
                        eventDesc = eventDesc + title
                    } else if ((titleMatch = urlData =~ /(?s)(Competition:.*?)\n{2,}.*?([A-z].*?)\n{2,}/)) {
                        eventDesc = eventDesc + titleMatch[0][1].toString() + titleMatch[0][2].toString()
                    }

                    urlData = urlData.toString().replaceAll(/(?s)<!--.*?-->/, "")
                    def bodyMatch = urlData =~ /(?is)I><B>(.*?)<P>/
                    if (bodyMatch) {
                        def body = bodyMatch[0][1].toString().trim()
                        body = body.replaceAll(/<\/?[^>]+>/, "")
                        eventDesc = eventDesc + body
                    } else if ((bodyMatch = urlData =~ /(?is)B><I>(.*?)<P>/)) {
                        def body = bodyMatch[0][1].toString().trim()
                        body = body.replaceAll(/<\/?[^>]+>/, "")
                        eventDesc = eventDesc + body
                    } else if ((bodyMatch = urlData =~ /(?is)A_Standard__34__20_Chapeau">(.*?)<\/p>/)) {
                        eventDesc = eventDesc + bodyMatch[0][1].toString().trim()
                    } else if ((bodyMatch = urlData =~ /(?is)lead'><\w+>([^<]+)/)) {
                        eventDesc = eventDesc + bodyMatch[0][1].toString().trim()
                    } else if ((bodyMatch = urlData =~ /(?is)<p\s*class="A__34__20_Chapeau_P4">(.*?)<\/p>/)) {
                        def bodyData = bodyMatch[0][1].toString().trim()
                        bodyData = bodyData.replaceAll(/<\/?[^>]+>/, "")
                        eventDesc = eventDesc + bodyData
                    } else if ((bodyMatch = urlData =~ /(?is)<p\s*class="A_Standard__35__20_Normal">(.*?)<\/p>/)) {
                        def bodyData = bodyMatch[0][1].toString().trim()
                        bodyData = bodyData.replaceAll(/<\/?[^>]+>/, "")
                        eventDesc = eventDesc + bodyData
                    } else if ((bodyMatch = urlData =~ /(?is)<p\s*class="A___35__20_Normal">(.*?)<\/p>/)) {
                        def bodyData = bodyMatch[0][1].toString().trim()
                        bodyData = bodyData.replaceAll(/<\/?[^>]+>/, "")
                        eventDesc = eventDesc + bodyData
                    } else if ((bodyMatch = urlData =~ /(?is)<\/h(?:1|3)>.*?<p>(.*?)<\/?p>/)) {
                        eventDesc = eventDesc + bodyMatch[0][1].toString().trim()
                    }
                }

                eventDescList.add(eventDesc)
                def eventDateMatch = eventDateData =~ /(?is)eventsTdDate">([^<]+)/

                if (eventDateMatch) {
                    eventDate = eventDateMatch[0][1].toString().trim()
                }

                if (eventDate) {
                    eventDate = fixDate(eventDate)
                    eventDateList.add(eventDate)
                }
            } else {
                eventDateBlockMatch = entityData =~ /(?s)Document<\/th>.*?<tr>(.*?)<\/tr>/

                if (eventDateBlockMatch) {
                    def eventBlock = eventDateBlockMatch[0][1].toString().trim()

                    if (eventBlock) {
                        def eventDateMatch = eventBlock =~ /(?is)eventsTdDate">([^<]+)/
                        if (eventDateMatch) {
                            eventDate = eventDateMatch[0][1].toString().trim()
                            eventDate = fixDate(eventDate)
                            eventDate = eventDate.replaceAll(/\//, ".")
                        }

                        def eventDetMatch = eventBlock =~ /(?is)eventsTdDocType">([^<]+)/
                        if (eventDetMatch) {
                            if (eventDate) {
                                eventDesc = eventDesc + eventDate + ": "
                            }
                            eventDesc = eventDesc + eventDetMatch[0][1].toString().trim()
                        }
                    }
                }

                if (eventDate) {
                    eventDate = eventDate.replaceAll(/\./, "/")
                    eventDateList.add(eventDate)
                }

                //eventDescList.add(eventDesc)
            }*/
        }

        //urlList.addAll(documentUrlList)
        attrMap[FIELDS.URL] = urlList

        //	attrMap[FIELDS.REMARKS] = remarksList

        //attrMap[FIELDS.ASSOCIATIONS] = assocList

        attrMap[FIELDS.EVNT_DETAILS] = eventDescList
        attrMap[FIELDS.EVNT_DATE] = eventDateList

        createEntity(name, attrMap)
    }

    def createEntity(name, attrMap) {
        if (name) {
            def entity
            entity = createOrgEntity(name)

            createEntityCommonCore(entity, attrMap)
        }
    }

    def createOrgEntity(name) {
        def entity = context.findEntity(["name": name, "type": "O"])

        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(sanitize(name))
            entity.type = "O"
        }

        return entity
    }

    def createEntityCommonCore(ScrapeEntity entity, attrMap) {

        attrMap[FIELDS.URL].each {
            entity.addUrl(urlSanitize(it))
        }

        /*	attrMap[FIELDS.ASSOCIATIONS].each {
                entity.addAssociation(sanitize(it))
            }*/

        /*attrMap[FIELDS.REMARKS].each {
            entity.addRemark(sanitize(it))
        }*/

        def iter = 0
        def eventListSize = attrMap[FIELDS.EVNT_DETAILS].size()
        while (iter < eventListSize) {
            def event = new ScrapeEvent()
            event.category = "REG"
            event.subcategory = "ACT"
            event.setDescription(eventSanitize(attrMap[FIELDS.EVNT_DETAILS][iter]))
            if (iter < attrMap[FIELDS.EVNT_DATE].size()) {
                event.setDate(attrMap[FIELDS.EVNT_DATE][iter])
            } else {
                event.setDate(attrMap[FIELDS.EVNT_DATE][0])
            }
            entity.addEvent(event)
            iter++
        }

/*		attrMap[FIELDS.EVNT_DETAILS].each {
			def event = new ScrapeEvent()
			event.category = "REG"
			event.subcategory = "ACT"
			event.setDescription(it)
			attrMap[FIELDS.EVNT_DATE].each {
				event.setDate(it)
			}
			entity.addEvent(event)
		}*/
    }

    def fixDate(date) {
        date = date.replaceAll(/(\d+)\.(\w+)\.(\d+)/, {
            it[2] + "/" + it[1] + "/" + it[3]
        })

        return date
    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = true, tidy = false, headersMap = [:], miscData = [:]) {
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

        def pdfText = context.transformPdfToText(pdfFile, pmap).toString().replaceAll(/\r\n/, "\n")
        return pdfText
    }

    def urlSanitize(input) {
        StringBuilder resultStr = new StringBuilder()
        for (char ch : input.toString().toCharArray()) {
            if (ch > 127 || ch < 0) {
                resultStr.append('%')
                def tempResult = String.format("%04x", new BigInteger(1, ch.toString().getBytes('UTF-8')))
                resultStr.append(tempResult.substring(0, 2) + '%' + tempResult.substring(2, 4))
            } else if (ch > 123) {
                resultStr.append('%')
                def tempResult = String.format("%02x", new BigInteger(1, ch.toString().getBytes('UTF-8')))
                resultStr.append(tempResult)
            } else {
                resultStr.append(ch)
            }
        }
        return resultStr.toString()
    }

    def sanitize(data) {
        return data.replaceAll(/&amp;/, '&').replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
    }

    def eventSanitize(data) {
        data = data.replaceAll(/&amp;/, '&')
        data = data.replaceAll(/;/, "")
        data = data.replaceAll(/,/, "").replaceAll(/(?s)\s+/, " ").trim()

        return data
    }

    def getParams(fromrow, fieldnames, export_list_id) {
        def param = [:]

        param["fuseaction"] = "dsp_result"
        param["sort"] = "case_code asc"
        param["fromrow"] = fromrow
        param["aid_instrument_id"] = ""
        param["case_number"] = ""

        param["case_title"] = ""
        param["case_type_id"] = ""
        param["decision_date"] = "0_fromToPair"
        param["decision_date_from"] = ""
        param["decision_date_to"] = ""
        param["decision_type_id"] = ""
        param["decision_type_text"] = ""
        param["fieldnames"] = fieldnames
        param["legal_basis_id"] = ""
        param["legal_basis_sec_id"] = ""
        param["legal_basis_pri_id"] = ""
        param["nace_code"] = ""
        param["notification_date"] = "0_fromToPair"
        param["notification_date_from"] = ""
        param["notification_date_to"] = ""
        param["pickup_nace_code_mylabel"] = ""
        param["policy_area_id"] = "1"
        param["pa"] = "1"
        param["primary_objective_id"] = ""
        param["primary_objective_id_gber"] = ""
        param["includegberobj_id"] = ""
        param["region_id"] = ""
        param["new_region_id"] = ""
        param["webpub_date"] = "0_fromToPair"
        param["webpub_date_from"] = ""
        param["webpub_date_to"] = ""
        param["member_state"] = ""
        param["includegberobj"] = ""
        param["onlycccases"] = ""
        param["antitrust"] = ""
        param["cartel"] = ""
        param["decision_type_id_1"] = ""
        param["decision_type_id_2"] = ""
        param["doc_title"] = ""
        param["legal_basis_id_1"] = ""
        param["simple_proc_yes"] = ""
        param["simple_proc_no"] = ""
        param["ojpub_date"] = "0_fromToPair"
        param["ojpub_date_from"] = ""
        param["ojpub_date_to"] = ""
        param["merg_not_date"] = "0_fromToPair"
        param["merg_not_date_from"] = ""
        param["merg_not_date_to"] = ""
        param["merg_case_not"] = ""
        param["merg_deadline_date"] = "0_fromToPair"
        param["merg_deadline_date_from"] = ""
        param["merg_deadline_date_to"] = ""
        param["merg_doc_type_id"] = ""
        param["council_reg_1"] = ""
        param["council_reg_2"] = ""
        param["at_case_max"] = ""
        param["at_case_min"] = ""
        param["dg_responsible_id"] = ""
        param["at_doc_date"] = "0_fromToPair"
        param["at_doc_date_from"] = ""
        param["at_doc_date_to"] = ""
        param["proc_phase"] = ""
        param["provisional_deadline"] = ""
        param["simplified"] = ""
        param["export_list_id"] = export_list_id
        return param
    }

    def getHeader() {
        def headers = [
            "Accept"                   : "application/json, text/javascript, */*; q=0.01",
            "Accept-Language"          : "en-US,en;q=0.9",
            "Connection"               : "keep-alive",
            "Content-Type"             : "application/x-www-form-urlencoded; charset=UTF-8",
            "Host"                     : "ec.europa.eu",
            "Origin"                   : "https://ec.europa.eu",
            "Referer"                  : "https://ec.europa.eu/competition/elojade/isef/index.cfm?fuseaction=dsp_result&policy_area_id=1",
            "Sec-Fetch-Dest"           : "document",
            "Sec-Fetch-Mode"           : "navigate",
            "Sec-Fetch-Site"           : "same-origin",
            "Upgrade-Insecure-Requests": "1",
            "X-MicrosoftAjax"          : "Delta=true",
        ]
        return headers
    }

    def invokePost(def url, def paramsMap, def cache = false, def headersMap = [:], def tidy = true) {
        return context.invoke([url: url, tidy: tidy, type: "POST", params: paramsMap, headers: headersMap, cache: cache])
    }
}