package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.request.HttpInvoker
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent


import java.nio.file.Files
import java.nio.file.Paths

context.setup([socketTimeout: 100000, connectionTimeout: 100000, retryCount: 5]);
context.session.encoding = "UTF-8"
context.getSession().setEscape( true )

Ncis script = new Ncis(context)
script.initParsing()

class Ncis {
    final def moduleFactory = ModuleLoader.getFactory("debd04acd9425a218804951f50eae60548a7a875")
    final ScrapianContext context
    final def ocrReader
    def invoker = new HttpInvoker()

    Ncis(context) {
        this.context = context
        ocrReader = moduleFactory.getOcrReader(context)
    }

    def initParsing() {
        def html = invoke("https://www.ncis.navy.mil/Resources/Have-You-Seen-Us/")
        //  def html = invoke("https://www.ncis.navy.mil/Resources/Have-You-Seen-Us/")

        handleIndexPage(html)
    }

    def handleIndexPage(indexPage) {
        def imageUrlMatch = indexPage =~ /(?sm)\/portals\/25\/Images\/RESOURCES\/WANTED.*?jpg|\/Portals\/25[^"]*Wanted[^"].*?pdf/
        //def imageUrlMatch = indexPage =~ /(?sm)\/portals\/25\/Images\/RESOURCES\/WANTED.*?jpg|\/Portals\/25\/Wanted.*?pdf/
        // def imageUrlMatch = indexPage =~ /(?sm)\/portals\/25\/Images\/RESOURCES\/WANTED.*?jpg/
        // def imageUrlMatch = indexPage =~ /(?sm)\/portals\/.*?\.jpg/
        def imageUrl
        def subUrl

        while (imageUrlMatch.find()) {
            subUrl = imageUrlMatch.group(0).trim()



            imageUrl = "http://www.ncis.navy.mil" + subUrl
            imageUrl = imageUrl.replaceAll(" ", "%20")


            if (!imageUrl.contains("height")) {
                handleEntityPdf(imageUrl)
            }
        }
    }

    def handleEntityPdf(url) {
        invoker.setUrl(url)

        def text = getText()


        text = sanitizationText(text)

        text = text.toString().replaceAll("\\ufffd", "").replaceAll(/\[|\]/, "").replaceAll(/(?is)^\bd\b/, "")
        matchEntities(text, url)

    }



    def matchEntities(text, url) {

        text = text.replaceAll("NAVAL .*", " ").trim()
        text = text.replaceAll("FOR INFORMATION.*"," ").trim()
        text = text.replaceAll("(?i)Bair", "Hair").trim()

        def nameMatch = text =~ /^\s*\S.*/
        def name
        if (nameMatch.find()) {
            name = nameMatch.group(0).trim()
            name = name.replaceAll("(?i)height:.*", " ").trim()
        }
        def aliasMatch = text =~ /(?ism)(?<=Aliases).*?(?=Wanted)/
        def alias
        if (aliasMatch.find()) {
            alias = aliasMatch.group(0)
            alias = aliasSanitize(alias)
        }

        def heightMatch = text =~ /(?i)(?<=height:)\s*(\d|')+/
        // def heightMatch = text =~ /(?i)(?<=height:)(\d|')+/
        def height
        while (heightMatch.find()) {
            height = heightMatch.group(0).trim()
        }

        def weightMatch = text =~ /(?is)weight:(.*?)(?=hair|remarks|Technician)/
        // def weightMatch = text =~ /(?is)weight:(.*?)(?=hair)/
        def weight
        while (weightMatch.find()) {
            weight = weightMatch.group(1).trim()
        }

        def hairMatch = text =~ /(?i)(?<=hair:).*/
        def hair
        while (hairMatch.find()) {
            hair = hairMatch.group(0).trim()
        }

        def eyesMatch = text =~ /(?i)(?<=eyes:).*/
        def eyes
        while (eyesMatch.find()) {
            eyes = eyesMatch.group(0).trim()
        }

        def complexionMatch = text =~ /(?is)complexion:(.*?)(?=wanted)/
        def complexion
        while (complexionMatch.find()) {
            complexion = complexionMatch.group(1).trim()
        }

        def wantedMatch = text =~ /(?is)wanted for:\s*([^$]+)(?:dob)|wanted for:\s*([^\u0024]+)/
        //def wantedMatch = text =~ /(?is)wanted for:\s*([^\u0024]+)/
        def wanted
        while (wantedMatch.find()) {
            if(wantedMatch.group(1)){
                wanted = wantedMatch.group(1).trim()
            }else{
                wanted = wantedMatch.group(2).trim()
            }

        }

        createEntity(name, alias, height, weight, hair, eyes, complexion, wanted, url)
    }

    def createEntity(name, alias, height, weight, hair, eyes, complexion, wanted, url) {
        if (wanted) {

            def entity
            def address = "United States"
            entity = context.getSession().newEntity()
            if (name) {
                entity.setName(name)
            }
            def aliasList = alias.toString().split(/\n/).collect({return it.trim()})
            aliasList.each {
                if (it && !it.toString().equals("null")) {
                    entity.addAlias(it.trim())
                }
            }

            entity.type = "P"
            entity.addImageUrl(url)
            if (height) {
                entity.addHeight(sanitize(height))
            }
            if (weight) {
                entity.addWeight(weight)
            }
            if (hair) {
                entity.addHairColor(sanitize(hair))
            }
            if (eyes) {
                entity.addEyeColor(eyes)
            }
            if (complexion) {
                entity.addComplexion(complexion)
            }
            ScrapeAddress scrapeAddress = new ScrapeAddress()
            scrapeAddress.setCountry(address)
            entity.addAddress(scrapeAddress)


            def wantedList

            ScrapeEvent event = new ScrapeEvent()
            if (wanted) {
                wanted = wanted.replaceAll(/(?ism)tic's.*|;cb/, " ").trim()
                wantedList = wantedSeperator(wanted)
                wantedList.each {
                    if (!it.equals("")) {
                        it = it.toString().trim()
                        if (it.contains("Larceny")) {
                            it = "Larceny"
                        }

                        event = entity.newEvent()
                        event.setDescription(sanitize("Wanted for: " + it))
                        entity.addEvent(event)
                    }
                }
            }
        }
    }

    def wantedSeperator(def wanted) {

        def wantedList

        if (wanted.contains("and")) {
            wantedList = wanted.split("and")
        } else if (wanted.contains("-")) {
            wantedList = wanted.split("-")
        } else if(wanted.contains(",")) {
            wantedList = wanted.split(",")
        }else
        {
            wantedList = [wanted]
        }

        return wantedList
    }

    def getText() {

        def imageData = invoker.getData()
        File file = new File("person.jpg")
        def outputStream = new FileOutputStream(file);
        int read = 0;
        byte[] bytes = new byte[1024];

        while ((read = imageData.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read)
        }

        def text = ocrReader.getText(file, [cache: false])

        return text
    }

    def sanitize(data) {
        return data.toString().replaceAll(/(?s)\s+/, " ").trim()
    }

    def sanitizationText(def text){

        text = text.toString().replaceAll(/Weight:2301bs/,"Weight:230 lbs")
        text = text.replaceAll(/Leonel Melia/,"Leonel Mejia")
        text = text.replaceAll(/Weight:2101bs/,"Weight:210 lbs")
        text = text.replaceAll(/Weight:2201bs/,"Weight:220 lbs")

        return text
    }

    def aliasSanitize(def alias) {
        def aliasMatcher = alias =~ /[a-zA-Z]([a-zA-Z]|\s)+[a-zA-Z]/
        if (aliasMatcher.find()) {
            return aliasMatcher.group(0)
        }
        return alias
    }

    def invoke(url, cache = false, headersMap = [:], cleanSpaceChar = false, tidy = false, miscData = [:]) {
        Map data = [url: url, tidy: tidy, headers: headersMap, cache: cache, clean: cleanSpaceChar]
        data.putAll(miscData)
        return context.invoke(data)

    }

}