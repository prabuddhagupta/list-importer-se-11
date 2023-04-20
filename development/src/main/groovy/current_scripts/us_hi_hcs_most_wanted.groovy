package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent

context.setup([connectionTimeout: 20000, socketTimeout: 4000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Us_hi_hcs_most_wanted script = new Us_hi_hcs_most_wanted(context)
script.initParsing();

class Us_hi_hcs_most_wanted {

    final ScrapianContext context
    final def ocrReader
    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")
    final entityType

    def mainUrl = "http://honolulucrimestoppers.org/wanteds.aspx?P=wanteds&ID=606&PageNum=1&F="

    def ent_name, reason, dob, alias, date, event_des, height, weight, race, hair, eyes, sex, imgUrl
    def des = "This entity appears on the Hawaii Honolulu Coast Crime Stoppers list of Most Wanted."
    def noP, state
    def pageUrls = []

    Us_hi_hcs_most_wanted(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
        ocrReader = moduleFactory.getOcrReader(context)
    }

    def initParsing() {
        def html = invoke(mainUrl)
        def noPr = html =~ /(?ism)>page\s*\d+\s*of\s*(\d+)</
        if (noPr.find()) {
            noP = Integer.parseInt(noPr.group(1))
        }
        get_pageUrls(noP)
        int i = 1
        while (i <= noP) {

            def url = pageUrls[i]
            html = invoke(url)
            def tabler = html =~ /(?ism)<div class="col-sm-5".+?>(.+?)<\/table>/
            def table, imgr, sexr, eyer, hr, wr, hgtr, dater, namer, resr, racer, ager
            while (tabler.find()) {
                ent_name = ""
                reason = ""
                dob = ""
                alias = ""
                date = ""
                height = ""
                weight = ""
                race = ""
                hair = ""
                eyes = ""
                sex = ""
                imgUrl = ""
                table = tabler.group(1)
                def sr = table =~ /(?ism)<div class="noprint".+?>(\w+)/
                if (sr.find()) {
                    state = sr.group(1).trim()
                }
                def arrested = state =~ /(?is)Arrested/
                if (!arrested) {
                    imgr = table =~ /(?ism)<img class.+?src="(.+?)"/
                    resr = table =~ /(?ism)<td style="background-color:.+?div style=".+?>(.+?)(?:<br>|<\/div>)/
                    namer = table =~ /(?ism)<span xclass="spanlink".+?>(.+?)<\/span>/
                    sexr = table =~ /(?ism)<b>Gender.+?<td>(.+?)<\/td>/
                    racer = table =~ /(?ism)<b>Race.+?<td.+?>(.+?)<\/td>/
                    //dobr = table =~ /(?ism)<b>DOB.+?<td.+?>(.+?)<\/td>/
                    ager = table =~ /(?i)<b>Age.+?<td>(\d+)<\/td>/
                    hgtr = table =~ /(?ism)<b>Height.+?<td>(.+?)<\/td>/
                    wr = table =~ /(?ism)<b>Weight.+?<td.+?>(.+?)<\/td>/
                    hr = table =~ /(?ism)<b>Hair.+?<td>\s*(\w+)<\/td>/
                    eyer = table =~ /(?ism)<b>Eyes.+?<td.+?>(\w+)<\/td>/
                    dater = table =~ /(?ism)>Wanted\s+as.+?(\d+.+?\d+)<\/div>/


                    if (resr.find()) {
                        reason = resr.group(1)
                    }
                    if (namer.find()) {
                        ent_name = namer.group(1).trim()
                    }
                    if (sexr.find()) {
                        sex = sexr.group(1)
                    }
                    if (racer.find()) {
                        race = racer.group(1)
                    }
                    if (ager.find()) {
                        dob = getDobFromAge(ager.group(1))
                    }
                    if (hgtr.find()) {
                        height = hgtr.group(1)
                    }
                    if (wr.find()) {
                        weight = wr.group(1)
                        weight = weight.replaceAll(/\+/, "")
                    }
                    if (hr.find()) {
                        hair = hr.group(1)
                    }
                    if (eyer.find()) {
                        eyes = eyer.group(1)
                    }
                    if (dater.find()) {
                        date = dater.group(1)
                    }
                    if (imgr.find()) {
                        imgUrl = imgr.group(1)
                    }
                    sanitize_data()
                    createEntity(ent_name, reason)
                }
            }
            i++;
        }
    }

    def get_pageUrls(def np) {
        def root = "http://honolulucrimestoppers.org/wanteds.aspx?P=wanteds&ID=606&PageNum="
        def tail = "&F="
        int i = 1
        while (i <= np) {
            pageUrls[i] = root + i + tail
            i++
        }
    }

    def sanitize_data() {
        def n = ent_name.split(",")
        ent_name = ""
        int i = n.length - 1
        while (i >= 0) {
            ent_name += n[i].trim() + " "
            i--
        }
        //reason
        reason = reason.replaceAll(/(?s)\s+/, " ").trim()
    }

    //---------------------Entity Creation--------------------------//

    def createEntity(def name, def reason) {
        event_des = ""
        event_des = des + " Reason: " + reason
        def entity = null
        name = name.replaceAll(/(?s)\s+/, " ").trim()
        entity = context.findEntity([name: name])
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(name)
        }
        def Etype = "P"
        entity.setType(Etype)
        if (imgUrl != "") {
            entity.addImageUrl(imgUrl)
        }
        entity.addHeight(height)

        if (dob != "Unknown") {
            dob = context.parseDate(new StringSource(dob))
            entity.addDateOfBirth(dob)
        }
        entity.addSex(sex)
        entity.addHairColor(hair)
        if (race != "Unknown") {
            entity.addRace(race)
        }
        entity.addEyeColor(eyes)
        entity.addWeight(weight)
        //Event
        ScrapeEvent event = new ScrapeEvent()
        event.setDescription(event_des)
        if (date) {
            //date = context.parseDate(new StringSource(date), ["MM/dd/yyyy"] as String[])
            date = context.parseDate(new StringSource(date))
            event.setDate(date)
        }
        entity.addEvent(event)
        //Address
        ScrapeAddress address = new ScrapeAddress()
        address.setProvince("Hawaii")
        address.setCountry("UNITED STATES")
        entity.addAddress(address)

    }

    def getDobFromAge(def age) {
        def year, dobYear;
        Calendar cal = Calendar.getInstance();
        year = cal.get(Calendar.YEAR);
        dobYear = year - new Integer(age).intValue();
        dobYear = dobYear.toString()
       // dobYear = "-/-/" + dobYear
        return dobYear;

    }
//-------------------------------------------------------------------------------------//
    def invoke(url, headersMap = [:], cache = false, tidy = false, miscData = [:]) {
        Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
        dataMap.putAll(miscData)
        return context.invoke(dataMap)
    }
}