import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource

/**
 * Date: 05/10/18
 * */

context.setup([socketTimeout: 50000, connectionTimeout: 10000, retryCount: 5, userAgent:"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36"]);
context.session.encoding = "UTF-8";
context.session.escape = true;

Nj_Most_Wantedx script = new Nj_Most_Wantedx(context)
script.initParsing();

class Nj_Most_Wantedx {
    final ScrapianContext context;
    static root = "http://www.njsp.org/wanted/"

    Nj_Most_Wantedx(context) {
        this.context = context;
    }

    private enum FIELDS
    {
        CITY, COUNTRY, DOB, POB, EYE_COLOR, GENDER, HAIR_COLOR, COMPLEXION, HEIGHT, WEIGHT, IMG_URL, RACE, REMARK, SBI, FBI, NICNO, FPC, UNITFNO, SCAR, URL
    }

//------------------------------Initial part----------------------//

    def initParsing() {
        def html = context.invoke([url: Nj_Most_Wantedx.root, tidy: true])


        //def linkUrlMatch = html =~ /(?is)<a href="([^"]+)"\s+class="[^"]+".*?more information/
        def linkUrlMatch = html =~ /(?i)<a href="([^"]+)"\s+class="[^"]+".*?more information<.+?a><\/div>/
        while (linkUrlMatch.find()) {
            def url = Nj_Most_Wantedx.root + linkUrlMatch.group(1);

            handleDetailsPage(url);
        }
    }

    def handleDetailsPage(def url) {
        def attrMap = [:]
        def html = context.invoke([url: url, tidy: true])
        def name, imgUrl;

        def imageMatches = html =~ /src="(images[^"]+).*?alt/
        imgUrl = root + imageMatches[0][1]
        attrMap[FIELDS.IMG_URL] = imgUrl

        attrMap[FIELDS.URL] = url
        def nameMatch = html =~ /(?is)wantedH1">([^<]+)</
        if (nameMatch) {
            name = nameMatch[0][1]
        }

        def dob = attributeMatch(html, "dob")
        if (dob) {
            attrMap[FIELDS.DOB] = dob
        }

        def height = attributeMatch(html, "Height")
        if (height) {
            attrMap[FIELDS.HEIGHT] = height
        }

        def weight = attributeMatch(html, "Weight")
        if (weight) {
            attrMap[FIELDS.WEIGHT] = weight
        }

        def hair = attributeMatch(html, "Hair")
        if (hair) {
            attrMap[FIELDS.HAIR_COLOR] = hair
        }

        def eyes = attributeMatch(html, "Eyes")
        if (eyes) {
            attrMap[FIELDS.EYE_COLOR] = eyes;
        }

        def skin = attributeMatch(html, "Complexion")
        if (skin) {
            attrMap[FIELDS.COMPLEXION] = skin
        }

        def race = attributeMatch(html, "Race")
        if (race) {
            attrMap[FIELDS.RACE] = race
        }

        def city, country
        def pob = attributeMatch(html, FIELDS.POB)
        if (pob) {

            if (pob =~ /,/) {
                pob = pob.replaceAll(/([^,]+),(.*)$/, { a, b, c -> city = b; country = c })

                attrMap[FIELDS.CITY] = city;
                attrMap[FIELDS.COUNTRY] = country;
            } else if (pob =~ /^\s*\w+(\s+\w+)*\s*$/) {
                attrMap[FIELDS.COUNTRY] = pob
            }
            attrMap[FIELDS.POB] = pob

        }

        def scarList = [];
        def scarMatch = html =~ /(?is)scars,\s*marks\s*and\s*tattoos:(.*?)<\/ul>/
        if (scarMatch) {
            scarMatch = scarMatch[0][1] =~ /(?is)<li>([^<]+)</
            while (scarMatch.find()) {
                scarList.add(scarMatch.group(1))

            }
            attrMap[FIELDS.SCAR] = scarList
        }

        def aka = attributeMatch(html, "AKA",)

        def remark = attributeMatch(html, "Remarks")
        if (remark) {
            attrMap[FIELDS.REMARK] = remark
        }

        def nicNo = null
        if (html =~ /(?i)NIC No/) {
            nicNo = attributeMatch(html, "NIC No",)
            attrMap[FIELDS.NICNO] = nicNo

        }

        def sbiNo = attributeMatch(html, "SBI No")
        if (sbiNo)
            attrMap[FIELDS.SBI] = sbiNo;


        def fbiNo = attributeMatch(html, "FBI No")
        if (fbiNo)
            attrMap[FIELDS.FBI] = fbiNo;


        def fpc = attributeMatch(html, "FPC")
        if (fpc)
            attrMap[FIELDS.FPC] = fpc;


        def fugitiveUnitFileNumber = attributeMatch(html, "Fugitive Unit File Number:")
        if (fugitiveUnitFileNumber)
            attrMap[FIELDS.UNITFNO] = fugitiveUnitFileNumber

        createEntity(name, aka, attrMap)
    }

//------------------------------Entity creation part---------------//
    def createEntity(def name, def dba, def attrMap) {
        def aliasList = dba.replaceAll(/:/, "").split(/,/)
        def entity = createPersonEntity(name, attrMap);
        def aliasSize = aliasList.size();
        (0..<aliasSize).each { id ->
            def alias = aliasList[id];
            if (alias) {
                entity.addAlias(alias.trim());
            }
        }
    }

    def createPersonEntity(name, attrMap) {
        def entity = context.getSession().newEntity();
        entity.name = sanitize(name).replaceAll(/\s*(.*?)\s*,\s*([^,]+)\s*$/, '$2 $1');
        entity.type = "P";
        createEntityCommonCore(entity, attrMap);
        return entity;
    }

    def createEntityCommonCore(entity, attrMap) {
        if (attrMap[FIELDS.POB]) {
            def pobAddr = entity.newAddress();
            pobAddr.setBirthPlace(true);
            if (attrMap[FIELDS.CITY]) {
                pobAddr.city = sanitize(attrMap[FIELDS.CITY])
            }
            if (attrMap[FIELDS.COUNTRY]) {
                pobAddr.country = sanitize(attrMap[FIELDS.COUNTRY])
            }
        }

        if (attrMap[FIELDS.NICNO]) {
            def nic = entity.newIdentification();
            nic.setValue(attrMap[FIELDS.NICNO]);
            nic.setType("NIC NO")
        }

        if (attrMap[FIELDS.SBI]) {
            def sbiNo = entity.newIdentification();
            sbiNo.setValue(attrMap[FIELDS.SBI]);
            sbiNo.setType("SBI")
        }

        if (attrMap[FIELDS.FBI]) {
            def fbiNo = entity.newIdentification();
            fbiNo.setValue(attrMap[FIELDS.FBI]);
            fbiNo.setType("FBI NO")
        }

        if (attrMap[FIELDS.FPC]) {
            def fpc = entity.newIdentification();
            fpc.setValue(attrMap[FIELDS.FPC]);
            fpc.setType("FPC")
        }

        if (attrMap[FIELDS.UNITFNO]) {
            def fugitiveUnitFileNumber = entity.newIdentification();
            fugitiveUnitFileNumber.setValue(attrMap[FIELDS.UNITFNO]);
            fugitiveUnitFileNumber.setType("Fugitive Unit File Number")
        }

        if (attrMap[FIELDS.HEIGHT]) {
            entity.addHeight(attrMap[FIELDS.HEIGHT]);
        }

        if (attrMap[FIELDS.WEIGHT]) {
            entity.addWeight(attrMap[FIELDS.WEIGHT]);
        }

        if (attrMap[FIELDS.HAIR_COLOR]) {
            entity.addHairColor(attrMap[FIELDS.HAIR_COLOR]);
        }

        if (attrMap[FIELDS.EYE_COLOR]) {
            entity.addEyeColor(attrMap[FIELDS.EYE_COLOR]);
        }

        if (attrMap[FIELDS.COMPLEXION]) {
            entity.addComplexion(attrMap[FIELDS.COMPLEXION]);
        }

        if (attrMap[FIELDS.RACE]) {
            entity.addRace(attrMap[FIELDS.RACE]);
        }

        if (attrMap[FIELDS.SCAR]) {

            attrMap[FIELDS.SCAR].each {

                it = it.replaceAll(/^- /, '')
                it = it.replaceAll(/ - /, "; ")
                it = sanitize(it)
                if (it)
                    entity.addScarsMarks(it);
            }
        }

        if (attrMap[FIELDS.REMARK]) {
            entity.addRemark(attrMap[FIELDS.REMARK]);
        }

        if (attrMap[FIELDS.DOB]) {
            context.parseDateOfBirthForEntity(entity, new StringSource(attrMap[FIELDS.DOB]));
        }

        entity.addImageUrl(attrMap[FIELDS.IMG_URL]);
        entity.addUrl(attrMap[FIELDS.URL]);
    }

    def sanitize(data) {
        return data.replaceAll(/&amp;/, '&')
            .replaceAll(/(?i)\b(n\/a|unknown|None\s*(?:known)?|unavailable)\b/, '')
            .replaceAll(/(?s)\s+/, " ")
            .trim();
    }

    def attributeMatch(def html, def string) {
        def data, match;
        match = html =~ /(?is)$string(?:<[^>]+>\n*)+([^<]+)<[^>]+>/
        if (match) {
            data = match[0][1]

        } else if ((match = html =~ /(?is)$string:(?:<[^>]+>)*([^<]+)/)) {
            data = match[0][1]

        }
        if (data)
            return sanitize(data)

    }
}