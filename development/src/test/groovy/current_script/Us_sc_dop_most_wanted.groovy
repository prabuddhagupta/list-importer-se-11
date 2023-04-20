package current_script

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.request.HttpInvoker
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeDob
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent
import scrapian_scripts.utils.GenericAddressParserFactory

/**
 * Date:
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_sc_dop script = new Us_sc_dop(context)
script.initParsing()

//-----Debug area starts------------//
//script.handleDetailsPage("...")
//-----Debug area ends---------------//

class Us_sc_dop {
    final ScrapianContext context

    final addressParser

    final String root = "https://www.dppps.sc.gov"
    final String url = "https://www.dppps.sc.gov/Offender-Supervision/Most-Wanted"
    final HttpInvoker invoker = new HttpInvoker()

    Us_sc_dop(context) {
        this.context = context
        addressParser = GenericAddressParserFactory.getGenericAddressParser(context)
        addressParser.updateCities([US: ["North Augusta", "Walterboro", "Greenville"]])
    }

    private enum FIELDS
    {
        DOB, EVNT_DATE, EVNT_DETAILS, EYE_COLOR, GENDER, HAIR_COLOR, HEIGHT, IMG_URL, RACE, SCARS, WEIGHT, ADDRESS
    }

//------------------------------Initial part----------------------//
    def initParsing() {
        def html = invoke(url)

        def entityDataBlockMatch = html =~ /(?is)class="photo\s*(?:captured)?">(.*?<\/ul>)/
        def entityDataBlock
        while (entityDataBlockMatch.find()) {
            entityDataBlock = entityDataBlockMatch.group(1).toString().trim()
            handleDetailsPage(entityDataBlock)
        }
    }

    def handleDetailsPage(dataBlock) {
        if (!(dataBlock =~ /(?i)At\s+Large/)) {
            return;
        }
        def nameMatch = dataBlock =~ /(?i)Name:<[^>]+>([^<]+)/
        def attrMap = [:]
        def name
        def alias
        if (nameMatch) {
            name = nameMatch[0][1].toString().trim()
            name = name.replaceAll(/\*$/, "")
        }

        def raceMatch = dataBlock =~ /(?i)Race:<[^>]+>([^<]+)/
        def race
        if (raceMatch) {
            race = raceMatch[0][1].toString().trim()
        }

        if (race) {
            attrMap[FIELDS.RACE] = race
        }

        def genderMatch = dataBlock =~ /(?i)Sex:<[^>]+>([^<]+)/
        def gender
        if (genderMatch) {
            gender = genderMatch[0][1].toString().trim()
        }

        if (gender) {
            attrMap[FIELDS.GENDER] = gender
        }

        def ageMatch = dataBlock =~ /(?i)Age:<[^>]+>([^<]+)/
        def age
        if (ageMatch) {
            age = ageMatch[0][1].toString().trim()
        }

        if (age) {
            attrMap[FIELDS.DOB] = convertAgeToDate(age)
        }

        def heightMatch = dataBlock =~ /(?i)Height:<[^>]+>([^<]+)/
        def height
        if (heightMatch) {
            height = heightMatch[0][1].toString().trim()
        }

        if (height) {
            attrMap[FIELDS.HEIGHT] = height
        }

        def weightMatch = dataBlock =~ /(?i)Weight:<[^>]+>([^<]+)/
        def weight
        if (weightMatch) {
            weight = weightMatch[0][1].toString().trim()
        }

        if (weight) {
            attrMap[FIELDS.WEIGHT] = weight
        }

        if (height =~ /\d{3}/) {
            attrMap[FIELDS.HEIGHT] = weight
            attrMap[FIELDS.WEIGHT] = height
        }

        def hairDataMatch = dataBlock =~ /(?i)Hair:<[^>]+>([^<]+)/
        def hairColor
        if (hairDataMatch) {
            hairColor = hairDataMatch[0][1].toString().trim()
        }

        if (hairColor) {
            attrMap[FIELDS.HAIR_COLOR] = hairColor
        }

        def eyeDataMatch = dataBlock =~ /(?i)Eyes:<[^>]+>([^<]+)/
        def eyeColor
        if (eyeDataMatch) {
            eyeColor = eyeDataMatch[0][1].toString().trim()
        }

        if (eyeColor) {
            attrMap[FIELDS.EYE_COLOR] = eyeColor
        }

        def scarDataMatch = dataBlock =~ /(?i)Scars\/Tattoos:<[^>]+>([^<]+)/
        def scarData
        def scarList = []
        if (scarDataMatch) {
            scarData = scarDataMatch[0][1].toString().trim()
            scarList = scarData.split(/,/)
        }

        attrMap[FIELDS.SCARS] = scarList

        def addressDataMatch = dataBlock =~ /(?i)Last\s*Known\s*Address:<[^>]+>([^<]+)/
        def addressData
        if (addressDataMatch) {
            addressData = addressDataMatch[0][1].toString().trim()
        }

        if (addressData) {
            attrMap[FIELDS.ADDRESS] = fixData(addressData)
        }

        def eventDataMatch = dataBlock =~ /(?i)Notes:<[^>]+>([^<]+)/
        def eventData = "This entity appears on the South Carolina Department of Probation's Most Wanted list. "
        if (eventDataMatch) {
            eventData = eventData + eventDataMatch[0][1].toString().trim()

            if (eventData =~ /(?i)\bAKA\b/) {
                def aliasMatch = eventData =~ /\*\bAKA\b\s*([^$]+)/
                if (aliasMatch) {
                    alias = aliasMatch[0][1].toString().trim()
                }
                eventData = eventData.replaceAll(/\*\bAKA\b\s*([^$]+)/, "").trim()
            }
        }

        attrMap[FIELDS.EVNT_DETAILS] = eventData
        def eventDateMatch = dataBlock =~ /(?i)Absconded:<[^>]+>([^<]+)/
        def eventDate
        if (eventDateMatch) {
            eventDate = eventDateMatch[0][1].toString().trim()
        }

        if (eventDate) {
            attrMap[FIELDS.EVNT_DATE] = dateFormat(eventDate)
        }

        def imgUrlMatch = dataBlock =~ /(?i)src="([^"]+)/
        def imgUrl
        if (imgUrlMatch) {
            imgUrl = root + imgUrlMatch[0][1].toString().trim()
        }

        if (imgUrl) {
            attrMap[FIELDS.IMG_URL] = imgUrl
        }

        if (name) {
            createPersonEntity(name, attrMap, alias)
        }
    }

//------------------------------Filter part------------------------//
    def convertAgeToDate(age) {
        Calendar c = Calendar.getInstance()

        return "-/" + "-/" + (c.get(Calendar.YEAR) - (age as Integer))
    }

    def dateFormat(date) {
        date = date.replaceAll(/(\d+)-/, "\$1/")
        date = date.replaceAll(/(\w+)\s*(\d+),\s*(\d+)/, {
            getMonthNo(it[1]) + "/" + it[2] + "/" + it[3]
        })

        date = date.replaceAll(/(\w+)\s*(\d{4})/, {
            getMonthNo(it[1]) + "/" + "01" + "/" + it[2]
        })

        date = date.replaceAll(/(\d+)\/(\d+)\/(\b\d{2}\b)/, {
            it[1] + "/" + it[2] + "/" + "20" + it[3]
        })

        date = date.replaceAll(/\b(\d{1})\b\//, "0\$1/")

        return date.toString().trim()
    }

    def fixData(data) {
        data = data.replaceAll(/\bN\/A\b/, "")
        data = data.replaceAll(/\bUnknown\b/, "")
        data = data.replaceAll(/\bNone\b.*/, "")
        data = data.replaceAll(/\[[^\]]+]/, "")

        return data
    }

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

    def createPersonEntity(name, attrMap = [:], alias) {
        def entity = context.getSession().newEntity()
        name = camelCaseConverter(name)
        entity.name = name
        entity.type = "P"
        if (alias) {
            entity.addAlias(alias)
        }

        createEntityCommonCore(entity, attrMap)
    }

    def camelCaseConverter(name) {
        //only for person type //\w{2,}: II,III,IV etc ignored
        name = name.replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, { a, b, c, d ->
            return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
        })

        return name
    }

    def createEntityCommonCore(ScrapeEntity entity, attrMap) {
        if (attrMap[FIELDS.IMG_URL]) {
            entity.addImageUrl(urlSanitize(attrMap[FIELDS.IMG_URL]))
        }

        if (attrMap[FIELDS.RACE]) {
            entity.addRace(sanitize(attrMap[FIELDS.RACE]))
        }

        if (attrMap[FIELDS.GENDER]) {
            entity.addSex(sanitize(attrMap[FIELDS.GENDER]))
        }

        if (attrMap[FIELDS.HEIGHT]) {
            entity.addHeight(sanitize(attrMap[FIELDS.HEIGHT]))
        }

        if (attrMap[FIELDS.WEIGHT]) {
            entity.addWeight(sanitize(attrMap[FIELDS.WEIGHT]))
        }

        if (attrMap[FIELDS.EYE_COLOR]) {
            entity.addEyeColor(sanitize(attrMap[FIELDS.EYE_COLOR]))
        }

        if (attrMap[FIELDS.HAIR_COLOR]) {
            entity.addHairColor(sanitize(attrMap[FIELDS.HAIR_COLOR]))
        }

        if (attrMap[FIELDS.DOB]) {
            entity.addDateOfBirth(new ScrapeDob(attrMap[FIELDS.DOB]))
        }

        attrMap[FIELDS.SCARS].each { scar ->
            scar = fixData(scar)
            if (scar) {
                entity.addScarsMarks(sanitize(scar))
            }
        }

        if (attrMap[FIELDS.ADDRESS]) {
            def address = attrMap[FIELDS.ADDRESS]
            address = address.replaceAll(/([A-Z]{2}),/, "\$1")

            if (address =~ /([A-Z]{2})\s*(?:\d{5})?/) {
                address = address + ", US"

                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                def street_sanitizer = { street ->
                    return street.replaceAll(/(?s)\s+/, " ")
                            .replaceAll(/^[\s,-]+|\W+$/, "")
                }
                ScrapeAddress addressObj = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                if (addressObj) {
                    entity.addAddress(addressObj)
                } else {
                    //Address is not parse-able; either add then as raw addr or reformat the input addr string
                    println(address)
                }
            }

        }

        ScrapeEvent event = new ScrapeEvent()
        event.category = "FUG"
        event.subcategory = "WTD"
        event.setDescription(sanitize(attrMap[FIELDS.EVNT_DETAILS]))
        if (attrMap[FIELDS.EVNT_DATE]) {
            event.setDate(attrMap[FIELDS.EVNT_DATE])
        }
        entity.addEvent(event)

    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = true, tidy = false, headersMap = [:], miscData = [:]) {
        /* Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
         dataMap.putAll(miscData)
         return context.invoke(dataMap)*/
        invoker.setUrl(url)
        invoker.autoAcceptAllSslCert()
        return invoker.getStringData()
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
        return data.replaceAll(/&amp;/, '&').replaceAll(/&gt;/,">").replaceAll(/(?s)\s+/, " ").replaceAll(/&quot;/, "\"").replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
    }
}