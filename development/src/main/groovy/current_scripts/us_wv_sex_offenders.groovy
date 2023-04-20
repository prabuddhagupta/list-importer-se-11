import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import scrapian_scripts.utils.LabelValueParser

context.setup([socketTimeout: 30000, connectionTimeout: 10000, retryCount: 5]);
context.session.escape = true;

moduleFactory = ModuleLoader.getFactory("d897871d8906be0173bebbbf155199ff441dd8d3")
addressParser = moduleFactory.getGenericAddressParser(context)

baseUrl = "https://apps.wv.gov";
rootUrl = baseUrl + "/StatePolice/SexOffender/";
agreementUrl = rootUrl + "Disclaimer/AcceptOrDeclineDisclaimer";
searchUrl = rootUrl + "OffenderSearch/AdvancedSearch?LastName=&StreetName=&City=&County=BARBOUR&Limit=None";

handleIndexUrl();

def fixStreet(street) {
    street = street.toString().replaceAll(/(?s)\s+/, " ").trim()
    street = street.toString().replaceAll(/,$/, "").trim()
    street = street.toString().replaceAll(/\(/, "").trim()
    street = street.toString().replaceAll(/\bnull\b/, "").trim()
    return street
}

def handleIndexUrl() {
    agreeHtml = context.invoke([url: rootUrl, tidy: true, cache: false]);

    def paramsMap = [:];
    paramsMap['ContinueToUrl'] = rootUrl;
    paramsMap['button'] = 'Accept';
    searchHtml = context.invoke([url: agreementUrl, tidy: true, type: "post", params: paramsMap, cache: false])

    def paramMap = [:]
    paramMap['LastName'] = '';
    paramMap['StreetName'] = '';
    paramMap['City'] = '';
    paramMap['County'] = 'BARBOUR';

    searchHtml = context.invoke([url: searchUrl, tidy: true, params: paramMap, cache: false]);
    handleSearchPage(searchHtml);
}

def handleSearchPage(searchPage) {
    def href = searchPage =~ /<a[^>]*?href="([^>"]*?OffenderDetails\?sid=[^>"]*?)"[^>]*?>(.*?)<\/a>/
    (0..<href.count).each() { itr ->
        handleEntityPage(href[itr][1], href[itr][2])
    }

//left for debuging purpose
/*  url = "/StatePolice/SexOffender/OffenderDetails?sid=204085"
  handleEntityPage(url,"hilo, nm")*/
}

def handleEntityPage(entityUrl, entityName) {

    def entityPage = context.invoke([url: baseUrl + entityUrl, tidy: true]);

    entityUrl = baseUrl + entityUrl
    def labelValueParams = [
        labels            : ["Date of Birth", "Age", "Gender", "Race",
                             "Height", "Weight", "Eyes", "Hair",],
        dividers          : [":"],
        multiLineValues   : true,
        multiLineSeparator: ";",
        breakOnLabels     : true,
        replaceElements   : ["</tr>": "\n\n", "<br>": " "]
    ]
    def profileSection = context.regexMatch(entityPage, [regex: "(?s)Demographics(.*?)Residence"]);
    def labelValueParser = new LabelValueParser();
    def text = labelValueParser.htmlToText(profileSection[1], labelValueParams)
    def dataMap = labelValueParser.parse(text, labelValueParams);

    entityName = context.formatName(entityName)
    def key = [entityName, dataMap["Date of Birth"]]
    def entity = context.findEntity(key)

    if (!entity) {
        entity = context.newEntity(key)
        entity.name = entityName
        entity.type = "P";
    }
    if (dataMap["Race"] && !(dataMap["Race"] =~ /(?i)unknown/)) {
        entity.addRace(dataMap["Race"]);
    }

    if (dataMap["Gender"] && !(dataMap["Gender"] =~ /(?i)unknown/)) {
        entity.addSex(dataMap["Gender"]);
    }

    if (dataMap["Date of Birth"]) {
        context.parseDateOfBirthForEntity(entity, new StringSource(dataMap["Date of Birth"]));
    }

    if (dataMap["Height"]) {
        entity.addHeight(dataMap["Height"].replaceAll(/(\d)(.*)/, '$1' + '\'' + '$2'));
    }

    if (dataMap["Weight"]) {
        entity.addWeight(dataMap["Weight"]);
    }

    if (dataMap["Eyes"] && !(dataMap["Eyes"] =~ /(?i)unknown/)) {
        entity.addEyeColor(dataMap["Eyes"]);
    }

    if (dataMap["Hair"] && !(dataMap["Hair"] =~ /(?i)unknown/)) {
        entity.addHairColor(dataMap["Hair"]);
    }

    def adrPartMatch = entityPage =~ /(?is)zip(.*?)<\/tbody>/
    if (adrPartMatch) {
        def street
        def city
        def state
        def postCode
        def addrMatch = adrPartMatch[0][1] =~ /(?is)<tr>(.*?)<\/tr>/
        while (addrMatch.find()) {
            def address = addrMatch.group(1) =~ /(?is)<td>(.*?)<\/td>\s*<td>(.*?)<\/td>\s*<td>(.*?)<\/td>\s*<td>(.*?)<\/td>\s*<td>(.*?)<\/td>/
            if (address) {
                if (address[0][1] && !(address[0][1] =~ /(?i)unknown/)) {
                    street = address[0][1].toString().replaceAll(/&amp;/, '&').trim()
                    street = street.replaceAll(/\bnull\b/, '').toString();
                }
                if (address[0][2]) {
                    city = address[0][2].toString();
                }
                if (address[0][4]) {
                    state = address[0][4].toString();
                }
                if (address[0][5]) {
                    postCode = address[0][5].toString();
                }

                address = ", " + street + ", " + city + ", " + state + " " + postCode + ", " + "USA"
                address = address.toString().replaceAll(/,\s*,/, ",")

                def addrMap = addressParser.parseAddress([text: address, force_country: true])
                ScrapeAddress scrapeAddress = addressParser.buildAddress(addrMap, [street_sanitizer: { strt -> fixStreet(strt) }])
                entity.addAddress(scrapeAddress)
            }
        }
    }
    def gotEvent = false;
    context.regexMatches(entityPage, [regex: "(?s)Conviction Date:\\s*([^<]*?)\\s*</div>(?:.*?)Offense:\\s*(.*?)\\s*</"]).each { ev ->
        gotEvent = true;
        event = entity.newEvent();
        ev[1].replace("1/1/1900", "");
        if (ev[1].toString()) {
            event.setDate(context.parseDate(ev[1]));
        }
        event.setDescription("Sex Crime: " + ev[2].toString().replaceAll(/&amp;/, '&'));
    }

    if (!gotEvent) {
        event = entity.newEvent();
        event.setDescription("Entity appears on the West Virginia Sex Offenders site");
    }

    imageUrl = entityPage =~ /(?s)OffenderImage".*?"([^"]+)/;
    if (imageUrl) {
        entity.addImageUrl(baseUrl + imageUrl[0][1]);
    }
    entity.addUrl(entityUrl);
}
