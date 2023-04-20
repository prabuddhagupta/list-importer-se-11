package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Date: 22 Dec 2017
 * needs VPN
 * */

context.setup([connectionTimeout: 35000, socketTimeout: 45000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_ga_atlpd_most_wanted script = new Us_ga_atlpd_most_wanted(context)
script.initParsing()

//-----Debug area starts------------//
//script.handleDetailsPage("...")
//-----Debug area ends---------------//

class Us_ga_atlpd_most_wanted {
    final ScrapianContext context
    final def moduleFactory = ModuleLoader.getFactory("7dd0501f220ad1d1465967a4e02f6ff17d7d9ff9")
    //a2d905c86b1424c8656d0432d41029977e4f81cd
    final def ocrReader
    final mainUrl = "https://www.atlantapd.org/community/crime-stoppers/most-wanted"
    final String root = "https://www.atlantapd.org"
    //  final String url = root + "/i-want-to/most-wanted"

    Us_ga_atlpd_most_wanted(context) {
        this.context = context
        ocrReader = moduleFactory.getOcrReader(context)
    }

    int i = 0
    def entityName, DOB
//------------------------------Initial part----------------------//
    def initParsing() {
        def html = invoke(mainUrl)
        def dataBlockMatch = html =~ /(?is)\bMost\b\s*\bWanted\b(.*?)\bCold-Homicide\b/
        def dataBlock
        if (dataBlockMatch) {
            dataBlock = dataBlockMatch[1][1].toString().trim()
        }

        def imgUrl = [], entityUrl = []
        //Match all entity url from the main url
        def urlMatch = dataBlock =~ /(?is)href='([^']+)'/
        while (urlMatch.find()) {
            def img_url, entity_url
            (img_url, entity_url) = handleDetailsPage(urlMatch.group(1).toString().trim())
            imgUrl[i - 1] = img_url
            entityUrl[i - 1] = entity_url
        }
        def mainPosterLink
        def LinkMatcher = html =~ /(?ism)<div class=" image_widget".+?img alt="(.+?)"\s+class="img_item".+?src="(.+?)"/

        //download the Main Poster
        def path = "/tmp/Us_ga_atlpd_most_wanted.jpeg"
        if (LinkMatcher.find()) {
            mainPosterLink = root + LinkMatcher.group(2)
        }
        downloadImage(mainPosterLink, path)
        //def path = "/home/sekh/Documents/Us_ga_atlpd_most_wanted.jpeg"
        File f = new File(path)
        def text = pdfToTextConverter(f)
        def data = formatText(text)
        getData(data, imgUrl, entityUrl)
        // Files.delete(Paths.get(path))
    }

    def getData(String text, def imgUrl, def entityUrl) {
        def names = [], dob = [], reasons = []
        def lines = text.split(/\n/)
        int i = 1, l

        lines.each {
            //println("I $i: $it")
            List n = it.split(/\s{2,}/)
            if (i == 1) {
                names.addAll(n)
                i++
            } else if (i == 2) {
                dob.addAll(n)
                i++
            } else {
                reasons.addAll(n)
                i = 1
            }
        }
        i = 0
        l = names.size()
        while (i < l) {
            String name = names[i]
            if (!name) {
                continue
            } else {
                name = sanitizeName(name)
                String reason = reasons[i]
                if (reason) {
                    reason = reason.replaceAll(/June.*/, "Aggravated Assault")
                }
                createEntity(name, dob[i], reason, imgUrl[i], entityUrl[i])
                i++
            }
        }
    }

    String sanitizeName(String name) {

        return name.trim()
    }

    def handleDetailsPage(srcUrl) {
        def html = invoke(srcUrl)

        def img_url, entity_url, imageLink
        def linkMatcher = html =~ /(?ism)<div class=" image_widget".+?img alt="(.+?)"\s+class="img_item".+?src="(.+?)"/
        if (linkMatcher.find()) {
            imageLink = root + linkMatcher.group(2)
        }
        img_url = imageLink
        entity_url = srcUrl
        i++
        return [img_url, entity_url]
    }

    def formatText(String text) {
        text = text.replaceAll(/(?s).*APD NEEDS YOUR HELP FINDING THESE INDIVIDUAL\s{3,}rE/, "").trim()
        //remove empty lines
        text = text.replaceAll(/(?ism)^[ \t]*\r?(\n|$)/, "").trim()
        def linesMatcher = text =~ /(?m).*/
        def lines, data = ""
        //make the text into scraping friendly data
        while (linesMatcher.find()) {
            lines = linesMatcher.group().trim()
            lines = lines.replaceAll(/CONTACT|CRIME|STOPPERS|ANONYMOUSLY:|404-577-TIPS|GET REWARD UP TO|\(8477\)|\$2,000|www\..*/, "").trim()
            if (lines =~ /^\w+.+/) {
                data += lines + "\n"
            }
        }
        data = data.replaceAll(/(LYNWOOD|HENDERSON)/, '$1 ')
        data = data.replaceAll(/Homicide December\s+\d+/, "Homicide")
        data = data.replaceAll(/JR\./, "")
        data = data.replaceAll(/\nAggravated Assault$/, "  Aggravated Assault")
        data = data.replaceAll(/CASH\s+\(Gun\)/, "").trim()
        data = data.replaceAll(/(?ism)\(Gun\)/, "")
        data = data.replaceAll(/(?ism)(Vehicular Homicide\s*)(December 1994)(\s*Felony Murder.*?\w\s{2,})(\w.*?)$/, '$1 $4 $3')
        data = data.replaceAll(/(?ism)(DEMARQUAVUS)(.*?)(BARBER)(.*?)(January 1995)/, '$1 $3 $2 $5 $4')
        data = data.replaceAll(/(?ism)CASH\s{2,}Aggravated Assault.*?\n/, "Aggravated Assault   Aggravated Assault   Aggravated Assault\n")
        data = data.replaceAll(/(?ism)Aggravated Assault Aggravated Assault/, "Aggravated Assault   Aggravated Assault")
        data = data.replaceAll(/(?i)Armed Robbery\s{2,}Aggravated Assault/, "Armed Robbery")
        data = data.replaceAll(/Vehicular Homicide\s{2,}December 1994/, "Vehicular Homicide         Aggravated Assault")
        //data = data.replaceAll(/\bTATE\b/, "June 1994")
        data = data.replaceAll(/(?<=D'ANTHONY TATE|MCKINNON)/, "   ")
        data = data.replaceAll(/(?i)\s{2,}June 1994\s{2,}Aggravated Assault/, "")
        data = data.replaceAll(/DEMETRIUS MCDOWELL FERNANDO FELTON\s{4}/, "DEMETRIUS MCDOWELL   FERNANDO FELTON")
        data = data.replaceAll(/(?ism)^[ \t]*\r?(\n|$)/, "")

        return data
    }

//------------------------------Entity creation part---------------//

    def createEntity(String name, String dob, String reason, String imgUrl, String entityUrl) {
        // println("N: $name D: $dob R: $reason\n")
        if (name == "") {
            return
        }
        def entity = context.findEntity([name: name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name.trim())
            entity.setType("P")
        }
        if (entity.name =~ /SAYVON/) {
            dob = "June 1994"
        }
        if (dob) {
            dob = dob.trim()
            DOB = context.parseDate(new StringSource(dob), ["MMM yyyy"] as String[])
            // println("DOB: $DOB")
            if (DOB) {
                entity.addDateOfBirth(DOB)
            }

        }
        if (imgUrl) {
            entity.addImageUrl(imgUrl)
        }
        if (entityUrl) {
            entity.addUrl(entityUrl)
        }
        def address = new ScrapeAddress()
        address.city = "Atlanta"
        address.province = "Georgia"
        address.country = "United States"
        entity.addAddress(address)
        def desc = "This entity appears on the Atlanta Police Department Most Wanted list."
        if (reason) {
            desc += "Wanted For: $reason"
        }
        def event = new ScrapeEvent()
        event.category = "FUG"
        event.subcategory = "WTD"
        event.setDescription(desc)
        entity.addEvent(event)
    }

//------------------------------Misc utils part---------------------//
    def invoke(url, cache = false, tidy = false, headersMap = [:], miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }

    //=========defaults===================
    def downloadImage(def imageLink, def path) {
        InputStream inn = new URL(imageLink).openStream()
        Files.copy(inn, Paths.get(path), StandardCopyOption.REPLACE_EXISTING)
    }

    def pdfToTextConverter(def pdfUrl) {
        try {
            List<String> pages = ocrReader.getText(pdfUrl)
            return pages
        } catch (NoClassDefFoundError e) {
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
        catch (IOException e) {
            return "PDF has no page"
        }
        catch (Exception e) {
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
}