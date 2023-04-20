package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent

/**
 * Date: Mar 28, 2018
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_id_doc_most_wanted script = new Us_id_doc_most_wanted(context)
script.initParsing()

//-----Debug area starts------------//
//script.handleDetailsPage("...")
//-----Debug area ends---------------//

class Us_id_doc_most_wanted {
    final ScrapianContext context
    final String url = "https://www.idoc.idaho.gov/content/probation_and_parole/fugitive_recovery/most_wanted"

    Us_id_doc_most_wanted(context) {
        this.context = context
    }

    private enum FIELDS
    {
        EVNT_DATE, EVNT_DETAILS, EYE_COLOR, GENDER, HAIR_COLOR, HEIGHT, IMG_URL, URL, WEIGHT
    }

//------------------------------Initial part----------------------//
    def initParsing() {
        def html = invoke(url)

        def entityDataMatch = html =~ /(?s)(last-name-value".*?)clear:both/

        while (entityDataMatch.find()) {
            handleDetailsPage(entityDataMatch.group(1).toString().trim())
        }
    }

    def handleDetailsPage(data) {
        def attrMap = [:]
        def nameMatch = data =~ /(?s)last-name-value">\s*<[^>]+>([^<]+)/
        def name

        if (nameMatch) {
            name = nameMatch[0][1].toString().trim()
        }

        def aliasMatch = data =~ /(?s)Alias:\s*<[^>]+>\s*<[^>]+>([^<]+)/
        def aliasList = []
        if (aliasMatch) {
            aliasList = new ArrayList(Arrays.asList(aliasMatch[0][1].toString().trim().split(/,/)))
        }

        name = name.replaceAll(/(&quot;)(\w+)\1/, {
            aliasList.add(it[2].toString().trim())
            return ""
        })

        def imgUrlMatch = data =~ /(?s)src="([^"]+)/
        def imgUrl
        if (imgUrlMatch) {
            imgUrl = imgUrlMatch[0][1].toString().trim()
        }

        attrMap[FIELDS.URL] = url

        if (imgUrl) {
            attrMap[FIELDS.IMG_URL] = imgUrl
        }

        def genderMatch = data =~ /(?s)Sex:\s*<[^>]+>\s*<[^>]+>([^<]+)/
        def gender
        if (genderMatch) {
            gender = genderMatch[0][1].toString().trim()
        }

        if (gender) {
            attrMap[FIELDS.GENDER] = gender
        }

        def heightMatch = data =~ /(?s)Height:\s*<[^>]+>\s*<[^>]+>([^<]+)/
        def height
        if (heightMatch) {
            height = heightMatch[0][1].toString().trim()
        }

        if (height) {
            attrMap[FIELDS.HEIGHT] = height
        }

        def weightMatch = data =~ /(?s)Weight:\s*<[^>]+>\s*<[^>]+>([^<]+)/
        def weight
        if (weightMatch) {
            weight = weightMatch[0][1].toString().trim()
        }

        if (weight) {
            attrMap[FIELDS.WEIGHT] = weight
        }

        def hairMatch = data =~ /(?s)Hair:\s*<[^>]+>\s*<[^>]+>([^<]+)/
        def hairColor
        if (hairMatch) {
            hairColor = hairMatch[0][1].toString().trim()
        }

        if (hairColor) {
            attrMap[FIELDS.HAIR_COLOR] = hairColor
        }

        def eyesMatch = data =~ /(?s)Eyes:\s*<[^>]+>\s*<[^>]+>([^<]+)/
        def eyeColor
        if (eyesMatch) {
            eyeColor = eyesMatch[0][1].toString().trim()
        }

        if (eyeColor) {
            attrMap[FIELDS.EYE_COLOR] = eyeColor
        }

        def eventDateMatch = data =~ /(?s)Wanted\s*as\s*of:\s*<[^>]+>\s*<[^>]+>([^<]+)/
        def eventDate
        if (eventDateMatch) {
            eventDate = eventDateMatch[0][1].toString().trim()
        }

        if (eventDate) {
            attrMap[FIELDS.EVNT_DATE] = formatDate(eventDate)
        }

        def eventDataBlockMatch = data =~ /(?s)Warrant:(.*)/
        def eventData = "This entity appears on the Idaho Department of Corrections Most Wanted list."
        if (eventDataBlockMatch) {
            def eventDataBlock = eventDataBlockMatch[0][1].toString().trim()
            def eventDataMatch = eventDataBlock =~ /(?s)content">(?:<[^>]+>)?([^<]+)/

            while (eventDataMatch.find()) {
                eventData = eventData + "; " + eventDataMatch.group(1).toString().trim()
            }
            eventData = eventData.replaceAll(/\.\;/, ".")
            eventData = eventData.replaceAll(/;\s*$/, "")
        }

        attrMap[FIELDS.EVNT_DETAILS] = eventData

        if (name) {
            createPersonEntity(name, attrMap, aliasList)
        }
    }

//------------------------------Filter part------------------------//
    def formatDate(date) {
        date = date.replaceAll(/(\w+)\s*(\d{4})/, {
            getMonthNo(it[1]) + "/" + "01" + "/" + it[2]
        })

        date = date.replaceAll(/(\w+)\s*(\d{1,2}),\s*(\d{4})/, {
            getMonthNo(it[1]) + "/" + it[2] + "/" + it[3]
        })

        date = date.replaceAll(/(\d+)\/(\d+)\/\b(\d{2})\b/, {
            it[1] + "/" + it[2] + "/" + "20" + it[3]
        })

        date = date.replaceAll(/\b(\d{1})\b\//, "0\$1/")

        return date.toString().trim()
    }

    /**
     * Month name to decimal converter
     * @param monthName
     * @return month decimal no
     */
    def getMonthNo(def monthName) {
        if (monthName =~ /(?i)\bjan(?:uary)?\b/) {
            return "01"
        } else if (monthName =~ /(?i)\bfeb(?:ruary)?\b/) {
            return "02"
        } else if (monthName =~ /(?i)\bmar(?:ch)?\b/) {
            return "03"
        } else if (monthName =~ /(?i)\bapr(?:il)?\b/) {
            return "04"
        } else if (monthName =~ /(?i)\bmay\b/) {
            return "05"
        } else if (monthName =~ /(?i)\bjune?\b/) {
            return "06"
        } else if (monthName =~ /(?i)\bjuly?\b/) {
            return "07"
        } else if (monthName =~ /(?i)\baug(?:ust)?\b/) {
            return "08"
        } else if (monthName =~ /(?i)\bsep(?:tember)?\b/) {
            return "09"
        } else if (monthName =~ /(?i)\boct(?:ober)?\b/) {
            return "10"
        } else if (monthName =~ /(?i)\bnov(?:ember)?\b/) {
            return "11"
        } else if (monthName =~ /(?i)\bdec(?:ember)?\b/) {
            return "12"
        }
    }

//------------------------------Entity creation part---------------//

    def createPersonEntity(name, attrMap = [:], aliasList = []) {
        def entity = context.getSession().newEntity()
        name = personNameReformat(sanitize(name))
        name = camelCaseConverter(name)
        entity.name = name
        entity.type = "P"
        aliasList.each { alias ->
            entity.addAlias(personNameReformat(alias))
        }

        createEntityCommonCore(entity, attrMap)
    }

    def personNameReformat(name) {

        //Regroup person name by comma
        /**
         abc, sdf, jr --match
         abc, sdf jr -- not match
         C. Conway Felton, III -- not match
         O'Dowd, Jr., Charles T. -- match
         * */
        def exToken = "(?:[js]r|I{2,3})"
        return name.replaceAll(/(?i)\s*(.*?)\s*,\s*\b((?:(?!$exToken\b)[^,])+)s*(?:,\s*\b($exToken)\b)?\s*$/, '$2 $1 $3').trim()
    }

    def camelCaseConverter(name) {
        //only for person type //\w{2,}: II,III,IV etc ignored
        name = name.replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, { a, b, c, d ->
            return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
        })

        return name
    }

    def createEntityCommonCore(ScrapeEntity entity, attrMap) {

        if (attrMap[FIELDS.URL]) {
            entity.addUrl(urlSanitize(attrMap[FIELDS.URL]))
        }

        if (attrMap[FIELDS.IMG_URL]) {
            entity.addImageUrl(urlSanitize(attrMap[FIELDS.IMG_URL]))
        }

        if (attrMap[FIELDS.GENDER]) {
            entity.addSex(sanitize(attrMap[FIELDS.GENDER]))
        }

        if (attrMap[FIELDS.EYE_COLOR]) {
            entity.addEyeColor(sanitize(attrMap[FIELDS.EYE_COLOR]))
        }

        if (attrMap[FIELDS.HAIR_COLOR]) {
            entity.addHairColor(sanitize(attrMap[FIELDS.HAIR_COLOR]))
        }

        if (attrMap[FIELDS.HEIGHT]) {
            entity.addHeight(sanitize(attrMap[FIELDS.HEIGHT]))
        }

        if (attrMap[FIELDS.WEIGHT]) {
            entity.addWeight(sanitize(attrMap[FIELDS.WEIGHT]))
        }

        ScrapeEvent event = new ScrapeEvent()
        event.category = "FUG"
        event.subcategory = "WTD"
        event.setDescription(sanitize(attrMap[FIELDS.EVNT_DETAILS]))
        if (attrMap[FIELDS.EVNT_DATE]) {
            event.setDate(sanitize(attrMap[FIELDS.EVNT_DATE]))
        }
        entity.addEvent(event)

    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = true, tidy = false, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def urlSanitize(input) {
        StringBuilder resultStr = new StringBuilder()
        for (char ch : input.toString().toCharArray()) {
            if (ch > 127 || ch < 0) {
                resultStr.append('%')
                def tempResult = String.format("%04x", new BigInteger(1, ch.toString().getBytes('UTF-8')))
//may need to change based on website encoding
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
        return data.replaceAll(/&amp;/, '&').replaceAll(/&nbsp;/, "").replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
    }
}