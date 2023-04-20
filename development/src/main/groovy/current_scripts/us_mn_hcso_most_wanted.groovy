package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 20000, socketTimeout: 4000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_mn_hsco_most_wanted script = new Us_mn_hsco_most_wanted(context)
script.initParsing();

class Us_mn_hsco_most_wanted {

    final ScrapianContext context
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
    final entityType
    def final root = "https://www.hennepinsheriff.org"
    def final mainUrl = "https://www.hennepinsheriff.org/jail-warrants/most-wanted/list"

    def ent_name, reason, dob, alias = [], date, event_des, imgUrl, height, weight, race, sex, eyes, hair
    def html, entityUrl
    def des = "This entity appears on the Minnesota Hennepin County Sheriffs Office list of Most Wanted."

    Us_mn_hsco_most_wanted(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
        ocrReader = moduleFactory.getOcrReader(context)
    }

    def initParsing() {
        html = invoke(mainUrl)
        get_data()
    }

    def get_data() {
        def bdr, block, namr, resr
        bdr = html =~ /(?ism)<article class="module-grid__item">\s+(.+?)\s+<\/article>/

        while (bdr.find()) {
            ent_name = ""
            dob = ""
            event_des = ""
            height = ""
            weight = ""
            race = ""
            hair = ""
            eyes = ""
            sex = ""
            alias = []
            block = bdr.group(1)
            def imgr = block =~ /(?ism)<img\ssrc="(.+?)"/
            def detr = block =~ /(?ism)<a\shref="(.+?)"/
            resr = block =~ /(?ism)Warrant:\s*(.+?)</
            namr = block =~ /<h2 class="heading-module">(.+?)</

            if (namr.find()) {
                ent_name = namr.group(1).trim()

            }
            if (imgr.find()) {
                imgUrl = root + imgr.group(1).replaceAll(/&amp.+/, "").trim()
            }
            if (resr.find()) {
                reason = resr.group(1).trim()
            }
            if (detr.find()) {
                entityUrl = root + detr.group(1).trim()

                def details = invoke(entityUrl)

                def al = details =~ /(?ism)Aliases.+?<ul>(.+?)<\/ul>/
                def hghtr = details =~ /(?sm)Height:.+?>(.+?)</
                def sxr = details =~ /(?ism)Gender.+?>(.+?)</
                def wr = details =~ /(?ism)Weight:.+?>(.+?)</
                def hr = details =~ /(?ism)Hair\scolor.+?>(.+?)</
                def eyr = details =~ /(?ism)Eye\s*Color.+?>(.+?)</
                def rcr = details =~ /(?ism)Race.+?>(.+?)</
                def dobr = details =~ /(?ism)Date\sof\sBirth.+?>(.+?)</

                if (al.find()) {
                    alias.add(al.group(1).replaceAll(/<.+?>/, "").trim())
                }
                if (hghtr.find()) {
                    height = hghtr.group(1).replaceAll(/&nbsp\W+|<.+?>/, "").trim()
                }
                if (sxr.find()) {
                    sex = sxr.group(1).replaceAll(/&|nbsp\W*|;/, "") trim()
                }
                if (wr.find()) {
                    weight = wr.group(1).replaceAll(/&|nbsp\W*/, "") trim()
                }
                if (hr.find()) {
                    hair = hr.group(1).replaceAll(/&|nbsp|\W*/, "").trim()
                }
                if (eyr.find()) {
                    eyes = eyr.group(1).replaceAll(/&|nbsp\W*/, "").trim()
                }
                if (rcr.find()) {
                    race = rcr.group(1).replaceAll(/&|nbsp\W*/, "").trim()
                }
                if (dobr.find()) {
                    dob = dobr.group(1).replaceAll(/&|nbsp\W+|\w+day\W+/, "").trim()
                }

                sanitize_data()
                createEntity(ent_name, reason, alias)
            }
        }
    }

    def sanitize_data() {
        if (alias.size() >= 1) {
            String a = alias[0]
            alias = []
            a = a.replaceAll(/\n/, ";").trim()
            def aL = a.split(";")
            int i = 0
            while (i < aL.length) {
                def tmp = sanitize_name(aL[i].trim())
                alias.add(tmp)
                i++
            }
        }
        dob = sanitize_date()
    }

    def sanitize_date() {
        dob = dob.replaceAll(/\,/, "").trim()
        def d = dob.split(" ")
        d[0] = d[0].toLowerCase()
        if (d[0] == "january") {
            d[0] = "1"
        } else if (d[0] == "february") {
            d[0] = "2"
        } else if (d[0] == "march") {
            d[0] = "3"
        } else if (d[0] == "april") {
            d[0] = "4"
        } else if (d[0] == "may") {
            d[0] = "5"
        } else if (d[0] == "june") {
            d[0] = "6"
        } else if (d[0] == "july") {
            d[0] = "7"
        } else if (d[0] == "august") {
            d[0] = "8"
        } else if (d[0] == "september") {
            d[0] = "9"
        } else if (d[0] == "october") {
            d[0] = "10"
        } else if (d[0] == "november") {
            d[0] = "11"
        } else if (d[0] == "december") {
            d[0] = "12"
        } else {
            return dob
        }
        return d[0] + "/" + d[1] + "/" + d[2]

    }


    def sanitize_name(def ent_name) {
        def n = ent_name.split(/\,/)
        ent_name = ""
        int i = n.length - 1
        while (i >= 0) {
            ent_name += n[i].trim() + " "
            i--
        }
        return ent_name.trim()
    }


    //---------------------Entity Creation--------------------------//

    def createEntity(def name, def reason, def alist) {
        event_des = ""
        def entity = null

        name = name.replaceAll(/(?s)\s+/, " ").trim()
        name = name.replaceAll(/;$/, " ").trim()

        entity = context.findEntity([name: name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
        }
        def Etype = "P"
        entity.setType(Etype)
        entity.addDateOfBirth(dob)
        entity.addUrl(entityUrl)
        entity.addImageUrl(imgUrl)
        entity.addEyeColor(eyes)
        entity.addHeight(height)
        entity.addHairColor(hair)
        entity.addWeight(weight)
        entity.addSex(sex)
        entity.addRace(race)

        alist.each {
            if (it) {
                entity.addAlias(it)
            }
        }

        if (!reason.contains(";")) {
            ScrapeEvent event = new ScrapeEvent()
            if (reason == "") {
                event_des = des
            } else {
                event_des = des + " Wanted For:" + reason
            }
            event.setDescription(event_des)
            entity.addEvent(event)
        } else {
            def e = reason.split(";")
            int i = 0;
            while (i < e.length) {
                ScrapeEvent event = new ScrapeEvent()
                event_des = des + " Wanted For:" + e[i]
                event.setDescription(event_des.replaceAll(/(?s)\s+/, " ").trim())
                i++
                entity.addEvent(event)
            }

        }
        //Address
        ScrapeAddress address = new ScrapeAddress()
        address.setProvince("Minnesota")
        address.setCountry("UNITED STATES")
        entity.addAddress(address)

    }
//-------------------------------------------------------------------------------------//
    def invoke(url, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}