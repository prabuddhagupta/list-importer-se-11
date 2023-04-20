import com.rdc.importer.scrapian.model.StringSource
import scrapian_scripts.utils.LabelValueParser
import org.apache.commons.lang.StringUtils

context.setup([socketTimeout: 50000, connectionTimeout: 10000, retryCount: 5]);
context.getSession().setEscape(true)

def jumpPage = context.invoke([url: "https://www.lapdonline.org/lapd-most-wanted/", tidy: true, cache: false]);
context.elementSeek(jumpPage, [element: "form", endText: "<div class=\"grid-isotope\">"]).each { form ->
    context.regexMatches(form, [regex: "<option value=\"(.*?)\">(.*?)</option>"]).each { option ->

        if ((option[1].toString().length() > 0) && (!option[1].toString().substring(0, 3).equalsIgnoreCase("all"))) {

            handleIndexPage(context, "https://www.lapdonline.org/lapd-most-wanted/?division=" + option[1]);
        }
    }
}

def handleIndexPage(context, url) {

    def noticePage = context.invoke([url: url, tidy: true])
    context.elementSeek(noticePage, [startText: "Last updated on :", element: "div", endText: "<div class=\"row no-gutters\">", greedy: true]).each { trs ->
        context.regexMatches(trs, [regex: "<a href=\"(.*?)\" class=\"text-18.*?\""]).each { link ->
            handleEntityPage(context, link[1], "http://www.lapdonline.org");
        }
    }
}

def handleEntityPage(context, entityUrl, baseUrl) {

    def indexPage
    indexPage = context.invoke([url: entityUrl, tidy: true]);

    def labelValueParser = new LabelValueParser();
    def params = [
        labels       : ["Suspect", "Sex", "Descent", "Height", "Weight", "Hair", "Eyes", "Alias",
                        "Date of Birth", "Age", "Weapon", "Tatoos", "Oddities", "DR#", "WANTED FOR",
                        "Contact Info", "CRIME INFORMATION", "CRIME INFO", "WARNING", "NOTE", "Additional Suspects", "Associates",
                        "Aliases", "Last Known Address", "CA Hangouts"],
        dividers     : [":"],
        breakOnLabels: true
    ]

    def entityName = context.regexMatch(indexPage, [regex: "<div class=\"h4\">Most Wanted<\\/div>\\s*\t*?<h1>(.*?)<\\/h1>"]);

    def text
    def dataMap

    indexPage2 = indexPage.toString().replaceAll(/(?s)<strong>Do you.*?div>/, "").replaceAll(/(?s)<div class=\"span9\">\s*<h1>.*?<\/h1>\s*<p>(.*?)<\/p>/, "<tr>CRIME INFO: \$1</tr>").replaceAll(/(?s)<strong>(WANTED FOR)<\/strong>(?:<br>|<br \/>)\s*([^<]*?)(?:<br>|<br \/>|)\s*(?:<hr>|<hr \/>)/, "<tr>\$1\$2</tr>").replaceAll(/<strong>(.*?):<\/strong>(.*?)(?:<br>|<br \/>)/, "<tr>\$1:\$2</tr>");
    imageUrl = context.regexMatch(indexPage, [regex: "<div class=\"h4\">Most Wanted.*?<img.*?alt=\".*?\" src=\"(.*?jpg)\" width=\"270\""]);

    if (entityName =~ /(?:Felix Ugalde|Warren Stern)/) {
        context.elementSeek(new StringSource(indexPage2), [startText: "<div class=\"h4\">Most Wanted</div>", element: "li", endText: "<em>*Age Calculated From Date of Crime</em>", greedy: true]).each { trs ->
            trs = sanitizeRow(trs)
            trs.replace("CRIME INFORMATION", "CRIME INFORMATION:");
            trs.replace("Contact Info", "Contact Info:");
            trs.replace("WANTED FOR", "WANTED FOR:")

            text = labelValueParser.htmlToText(trs, params);
            dataMap = labelValueParser.parse(text, params);

        }
    } else {
        context.elementSeek(new StringSource(indexPage2), [startText: "<div class=\"h4\">Most Wanted</div>", element: "li", endText: "<h2 class=\"h3\">Details</h2>", greedy: true]).each { trs ->
            trs = sanitizeRow(trs)
            trs.replace("CRIME INFORMATION", "CRIME INFORMATION:");
            trs.replace("Contact Info", "Contact Info:");
            trs.replace("WANTED FOR", "WANTED FOR:")

            text = labelValueParser.htmlToText(trs, params);
            dataMap = labelValueParser.parse(text, params);

        }
    }

    if (!StringUtils.isBlank(entityName[1].toString()) && !entityName[1].toString().contains("Unknown") && !entityName[1].toString().contains("Male") && !entityName[1].toString().contains("Female") && !entityName[1].toString().contains("Suspect")) {
        createEntity(context, dataMap, entityName[1], entityUrl, imageUrl[1])
    }
}

def createEntity(context, entityData, entityName, entityUrl, entityImage) {

    def entity = context.getSession().newEntity();
    entityName = entityName.toString().trim()
    entityName = parseName(entityName);
    entity.name = entityName

    entity.type = "P"
    entity.addUrl(entityUrl.toString().trim());
    entity.addImageUrl(entityImage.toString().trim())

    address = entity.newAddress();
    address.setCity("Los Angeles");
    address.setProvince("California")
    address.setCountry("United States");

    sex = getData(entityData, ["Sex"]);
    if (sex) {
        entity.addSex(sex);
    }

    alias = getData(entityData, ["Alias"]);
    if (alias) {
        alias = alias.toString().replaceAll(/"|\.$|^\s/, '')
        alias = alias.toString().replaceAll(/\s+/, ' ')
        entity.addAlias(alias);
    }

    aliases = getData(entityData, ["Aliases"]);
    if (aliases) {
        aliases.split(",").each { aliasName ->
            aliasName = aliasName.toString().replaceAll(/"|\.$|^\s/, '')
            aliasName = aliasName.toString().replaceAll(/\s+/, ' ')
            entity.addAlias(aliasName);
        }

    }

    lastAddress = getData(entityData, ["Last Known Address"]);
    if (lastAddress) {
        address = context.parseAddress(new StringSource(lastAddress));
        entity.addAddress(address);
    }

    weight = getData(entityData, ["Weight"]);
    if (weight) {
        entity.addWeight(weight);
    }

    height = getData(entityData, ["Height"]);
    height = height.toString().replaceAll(/null/, '')
    if (height) {
        entity.addHeight(height);
    }

    hairColor = getData(entityData, ["Hair"]);
    if (hairColor) {
        entity.addHairColor(hairColor)
    }

    eyeColor = getData(entityData, ["Eyes"]);
    if (eyeColor) {
        entity.addEyeColor(eyeColor)
    }

    tattoos = getData(entityData, ["Tatoos"]);
    if (tattoos) {
        entity.addScarsMarks(tattoos)
    }

    def dob = getData(entityData, ["Date of Birth"]);
    if (dob) {
        context.parseDateOfBirthForEntity(entity, new StringSource(dob));
    }

    def crimeInfo = getData(entityData, ["CRIME INFO"]);
    def wantedFor = getData(entityData, ["WANTED FOR"])
    if (wantedFor || crimeInfo) {
        event = entity.newEvent();
        if (wantedFor) {
            event.description = "WANTED FOR: " + wantedFor.toString().replaceAll(/\s+/, " ")
        }

    }

}

def parseName(entityName) {
    if (entityName.contains("-")) {
        entityName = entityName.substring(0, entityName.indexOf("-"))
    }
    if (entityName.toString().contains(",")) {
        def entityNameParts = entityName.split(",");
        entityName = entityNameParts[1].trim() + " " + entityNameParts[0].trim()
    }
    if (entityName.contains(" Jr.")) {
        entityName = entityName.replace(" Jr.", "") + " Jr.";
        entityName = entityName.trim();
    }
    return entityName;
}

def getData(data, keys) {
    def value = null;
    keys.each { key ->
        def dataValue = data[key]
        if (dataValue && !dataValue.equals("NA") && !dataValue.equals("N/A") && !dataValue.equals("NOT KNOWN")) {
            value = dataValue
        }
    }
    return value;
}

def sanitizeRow(trs) {
    trs = trs.replace(/(?i)(?<=Height:)<\/strong>\s(\d{3})<\/li>/, "</strong> 5'05\"</li>")
    trs = trs.replace(/(?i)(?<=Weight:)<\/strong>\s(5'05)<\/li>/, "</strong> 185</li>")
    trs = trs.replace(/.*04\/08\/0197.*/, "")
    return trs
}