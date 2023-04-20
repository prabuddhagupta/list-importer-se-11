package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 50000, socketTimeout: 25000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8"
context.session.escape = true

BeFederalPolice script = new BeFederalPolice(context)
script.initParsing()
//script.handleDetailsPage("https://www.police.be/5998/en/wanted/flor-bressers")
class BeFederalPolice {
    final ScrapianContext context

    BeFederalPolice(context) {
        this.context = context
    }
    //required data
    String URL, DOB, GENDER, IMG_URL, NATIONALITY, EVENT, ALIAS

    final rootUrl = "https://www.police.be"
    //def mainUrl = "https://www.police.be/5998/en/wanted/overview/category/belgiums-most-wanted-by-fast-351"
    def mainUrl = "https://www.police.be/5998/en/wanted/most-wanted"

//------------------------------Initial part----------------------//
    def initParsing() {
        def html = invoke(mainUrl).toString()
        html = formatHtml(html)
        def lastPageMatch = html =~ /(?ism)last">\n\s*<a href="\?page=(.*?)" title="Go to last page"/
        int lastPage
        if (lastPageMatch) {
            lastPage = Integer.valueOf(lastPageMatch.group(1))
        }

        for (int i = 0; i <= lastPage; i++) {
            def currentPageUrl = "https://www.police.be/5998/en/wanted/most-wanted?page=" + i
            def html2 = invoke(currentPageUrl).toString()
            html2 = formatHtml(html2)
            def dataLinkMatch = html2 =~ /(?ism)a href="([^"]+wanted\/[^"]+)"/
            while (dataLinkMatch) {
                def dataLink = rootUrl + dataLinkMatch.group(1)
                handleDetailsPage(dataLink)
            }
        }
    }

    String formatHtml(String html) {
        html = html.replaceAll(/(?is)^.+?<h1 class="page-header">Wanted<\/h1>|(?is)<footer.+?>.+/, "").trim()
        html = html.replaceAll(/\s*\n\s+/, "\n").trim()
        html = html.replaceAll(/(?is)^(.+?)(<div class="four\Wcol four\Wcol[^>]+.+?>.+)/, '$2')
        return html
    }

    def handleDetailsPage(dataLink) {
        def html, name, event, imgUrl
        URL = DOB = GENDER = IMG_URL = NATIONALITY = EVENT = ALIAS = null
        dataLink = dataLink.replaceAll(/http:/, "https:")
        html = invoke(dataLink).toString()
        def nameMatch = html =~ /(?s)<h1 >\s+.+?\s+<span class="field field--name.*?>(.+?)<\/span>.+?<\/h1>/

        if (nameMatch) {
            name = sanitize(nameMatch[0][1])
            name = name.toString().replaceAll(/(?i)<sub>.*?<\/sub>/, "")//println("N: $name")
        }

        def aliasMatch = html =~ /(?i)alias:\s*([^<]+)/
        if (aliasMatch) {
            ALIAS = sanitize(aliasMatch[0][1])
        }

        def genderMatch = html =~ /(?i)\b(MEN|WOMEN)\b/
        if (genderMatch) {
            GENDER = sanitize(genderMatch[0][1])
        }

        def dobMatch = html =~ /(?i)Date of birth:*\s*([^<]+)/
        if (dobMatch) {
            DOB = sanitize(dobMatch[0][1])
        }

        //def nationalityMatch = html =~ /(?i)nationality:\s*([^<]+)/
        def nationalityMatch = html =~ /(?i)li>Nationality\s*:\s*(.*?)</
        if (nationalityMatch) {
            NATIONALITY = sanitize(nationalityMatch[0][1])
        }

        def imgUrlMatch = html =~ /"wanted-main-image"[^>]+url\(([^"]+)/
        if (imgUrlMatch) {
            imgUrl = imgUrlMatch[0][1]
            if (!(imgUrl =~ /^http/))
                imgUrl = rootUrl + imgUrlMatch[0][1]
            else
                imgUrl = imgUrlMatch[0][1]

            IMG_URL = sanitize(imgUrl)
        }

        def eventDesMatch = html =~ /(?s)<div class="field field--name-field-intro field--type-text-long field--label-hidden field--item">(.+?)<\/div>/
        if (eventDesMatch) {
            event = eventDesMatch[0][1]
            if (!(event =~ /(?i)sentence/)) {
                eventDesMatch = html =~ /(?i)<p>(.*?(?:sentence)?[^<]+)<\/p>/
                event = sanitize(eventDesMatch[0][1])
            }
            EVENT = sanitize(event)

        }
        URL = dataLink
        sanitizeAll(name)
        createEntity(name)

    }

    def createEntity(def name) {
        if (name == null) {
            return
        }
        def entity = null
        entity = context.findEntity(["name": name]);
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
        }
        if (ALIAS) {
            ALIAS = sanitize(ALIAS)
            entity.addAlias(ALIAS)
        }

        entity.setType("P")

        def defaultDes = "This entity appears on the Belgium Federal Police list of Most Wanted Fugitives."
        if (EVENT) {
            defaultDes = defaultDes + " " + EVENT.trim()
        }

        ScrapeEvent obj = new ScrapeEvent()
        obj.setDescription(defaultDes)
        obj.setCategory("FUG")
        obj.setSubcategory("WTD")
        entity.addEvent(obj)

        if (GENDER) {
            entity.addSex(GENDER)
        }

        if (URL) {
            entity.addUrl(URL)
        }

        if (IMG_URL) {
            entity.addImageUrl(IMG_URL)
        }

        if (NATIONALITY) {
            entity.addNationality(NATIONALITY)
        }

        if (DOB) {
            DOB = sanitize(DOB)
            def date = context.parseDate(new StringSource(DOB), ["dd/M/yyyy", "dd-MM-yyyy"] as String[])
            entity.addDateOfBirth(date)
        }

        ScrapeAddress addressObj = new ScrapeAddress()
        addressObj.setCountry("BELGIUM")
        entity.addAddress(addressObj)//println(" ENTITY: $entity.name $entity.imageUrls\n")
    }

    def invoke(url, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    def sanitize(data) {
        data = data.replaceAll(/(?s)<.+?>|\W+$|^\W+/, "")
        return data.replaceAll(/(?s)\s+/, " ").trim()
    }

    def sanitizeAll(name) {
        if (DOB) {
            DOB = DOB.replaceAll(/(?i)[A-Z]+/, "").trim()
        }
        if (ALIAS) {
            ALIAS = ALIAS.replaceAll(/(?i)Nationality:.+/, "").trim()
        }
        if (GENDER) {
            GENDER = GENDER.replaceAll(/(?i)^MEN/, "Male")
            GENDER = GENDER.replaceAll(/(?i)^WOMEN/, "Female")
        }
        if (EVENT) {
            EVENT = EVENT.replaceAll(/[^\.]$/, ".")
        }
        if (name == "Rony VAN WEYENBERG") {
            DOB = "19/12/1967"
        }
    }
}