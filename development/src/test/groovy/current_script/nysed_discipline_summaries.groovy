package current_script

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils

context.session.encoding = "UTF-8";
context.setup([socketTimeout: 30000, connectionTimeout: 10000, retryCount: 5])
context.getSession().setEscape(true);

defaultAction = "Entity appears on NY's Summaries of Regents Actions On Professional Misconduct and Discipline List"
stateList = ["AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL",
             "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT",
             "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI",
             "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"]

baseUrl = "http://www.op.nysed.gov/opd/"
indexUrl = "http://www.op.nysed.gov/opd/rasearch.htm"
monthUrl = "http://www.op.nysed.gov/opd/jul10.htm"
monthsFor94 = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "sep", "nov", "dec"]
patternBlockCapture = /(?is)<(\w++)[^>]+?class="(item|header_with_line_above)(?:\s+[^"]+)?"[^>]*>.*?.(?=<\1[^>]+?\2|mailto)/

handleIndexPage(indexUrl)

//-----debug---
//handleMonthPage("http://www.op.nysed.gov/opd/jun02.htm", patternBlockCapture)
//-----debug---

def handleIndexPage(pageUrl) {
    resultPage = context.invoke([url: pageUrl, tidy: true])

    context.regexMatches(resultPage, [regex: "[a-zA-Z]{3,7}\\d{2,4}\\.html?"]).each { month ->
        monthUrl = baseUrl + month[0];
        handleMonthPage(monthUrl, patternBlockCapture)
    }

    monthsFor94.each() {
        month = baseUrl + "${it}" + "94.htm"
        handleMonthPage(month, patternBlockCapture)
    }
}

def handleMonthPage(monthUrl, pattern) {
    def data = monthPage = context.invoke([url: monthUrl, tidy: true]).replace("[a&\\s]amp;", "&").replace(";", ",").replace(/,(?!\s)/, ', ');
    monthPage.replace("(?<=Sklar|Brock),", "")
    monthPage.replace("(?i)FORMERLY KNOWN AS", "A.K.A")
    monthPage.replace(", Jr", " Jr")
    monthPage = context.regexMatch(monthPage, [regex: "<body>(.*?)</body>"])

    def entries = context.regexMatches(monthPage[1], [regex: pattern])
    if (entries.equals(null)) {
        throw new Exception("Regex failed to find entities in page, " + monthUrl)
    }
    entries.each { entry ->
        entity = handleEntityPage(entry, monthUrl)
    }

}

def handleEntityPage(block, pageUrl) {
    block = block.toString().trim().replaceAll(/";"/, ",").replaceAll(/\[|\]/, "").replaceAll(/(?i),(?=\sd\/b\/a)/, "")
            .replaceAll(/(?i)(?=\s\d+<a)/, ",").replaceAll(/\A(<(\w+)[^>]+>.*?)(?=<\/\2>)/, "\$1,").replaceAll(/\s(?=Levittown)/, ",").replaceAll(/(?:Newman, Flynn,(\s))/, "\$1Inc")
    def capturingNameAndAddress = block =~ /\A<(\w+)[^>]+>((?:[^,]+,(?=\W*\b(?i)(?:Inc|P\.C|P\.T|Ltd|LLC|Newman|Flynn|Burton|Adest|LLP|L\.L\.C|CPAS?)\b[^\w,]*))*[^,]+,)(.*?)(?=<\\/\1|br\b)/
    def name
    def address
    if (capturingNameAndAddress.find()) {
        name = capturingNameAndAddress.group(2).replaceAll(/<(\w+)[^>]+>/, "").replaceAll(/<\\/\w+>/, "")
                .replaceAll("(?i)&amp;", "&").replaceAll(/(?i)#\d+|Profession:\s?\w+|\n|<\w+>/, "")
                .replaceAll(/<!.*/, "").toUpperCase().trim()
        address = capturingNameAndAddress.group(3).replaceAll(/<\/\w+>/, "").replaceAll(/(?s)(\n|\r).*$/, "")
                .replaceAll(/<[^>]+>/, "").replaceAll(/Profession:.*/, "").replaceAll(/</, "").replaceAll(/(?i)manufacturer\sof\sdrugs\sand\\/or\sdevices,\s/, "").toUpperCase()
    }
    if (name =~ /(?i)LEHMAN/) {
        name = name.toString().replaceAll(/(?i)inc/, "")
    }
    if (name =~ /(?i)PHYSIOFITNESS/) {
//        println(name)
    }

    def capturingProfession = block =~ /(?i)<[^>]+>(?=Profession:?)(.*)(?=<[^>]+>)/
    def profession
    while (capturingProfession.find()) {
        profession = capturingProfession.group(1).replaceAll(/<\/span>|<\/strong>/, "")
    }

    def capturingAcionDate = block =~ /(?i)(?:<[^>]+>)?(?=R(?:<[^>]+>)?egents Action Dat(?:<\\/\w+>)?e:?)(.*)(?=<[^>]+>)/
    def actionDate
    while (capturingAcionDate.find()) {
        actionDate = capturingAcionDate.group(1).replaceAll(/<\/span>/, "").replaceAll(/^[^:]+:/, "").replaceAll(/([^(]+\().*/, "\$1").replaceAll(/\(/, "")
//.replaceAll(/[^:]+:|[^"]+">/,"").replaceAll(/<\\/\w+>|<\\/\w+>\)|\)/,"")
    }

    def capturingAction = block =~ /(?i)(?:<[^>]+>)?(?=Actions?\s?(?:<\\/\w+>)?:|Action(?:<\\/\w+>))(.*)(?=<[^>]+>)/
    def action
    while (capturingAction.find()) {
        action = capturingAction.group(1).replaceAll(/<\/span>/, "").replaceAll(/Actions?\s?:|<\\/\w+>/, "").replaceAll(/<[^>]+>/, "").replaceAll(/(?i)Summary:/, "").trim()
    }

    def capturingSummary = block =~ /(?i)(?:<[^>]+>)?(?=Summ?ary(?:<\\/\w+>)?:)(.*)(?=<[^>]+>)/
    def summary
    while (capturingSummary.find()) {
        summary = capturingSummary.group(1).replaceAll(/<\/span>/, "").replaceAll(/(?i)<\\/\w+>/, "").replaceAll(/(?i)Summ?ary:/, "").replaceAll(/<\w+>/, "").replaceAll(/\s\s/, "").replaceAll(/<[^>]+>/, "").trim()
    }

    if (name.size() == 0)
        return null

    def entity = handleName(name, profession)
    if (!entity)
        return null

    entity.addUrl(pageUrl)
    handleLocation(entity, address)
    if (entity.getAddresses() == null || entity.getAddresses().size() == 0) {
        def addr = entity.newAddress()
        addr.setProvince("New York")
        addr.setCountry("UNITED STATES")
    }
    handleSection(entity, actionDate, action, summary, pageUrl)
}

def handleName(name, profession) {
    if (name.trim().size() == 0)
        return null

    if (name.contains("/")) {
        name = name.replaceAll("/", ".");
    }
    aliases = name.split(/(?i)\b(?:D\.B\.A|[FA]\.?K\.A|A\\/KA|AKA|A\\/k\\/A|A\.KA)\b/)
    //(/(?i)(?:[daf][\.\/]*[bk][\.\/]*a)\.?/)
    name = StringUtils.removeEnd(aliases[0].trim(), ",").trim()
    def type = detectEntityType(name, profession)
    def entity = context.findEntity(["name": name, "type": type]);
    if (entity == null) {
        entity = context.getSession().newEntity()
        entity.setName(name)
        entity.setType(type)
    }

    for (int i = 1; i < aliases.length; i++) {
        def alias = StringUtils.removeEnd(aliases[i].toString().trim(), ",")
        addAlias(entity, alias)
    }

    return entity
}

def handleLocation(entity, location) {
    def loc = clean(location).replaceAll("(?i)&amp;", "&")
            .replaceAll(" AND ", ";").replaceAll(/(?i)c\spharmacist,|lic.\sNO.\s\d+,|cal.\sNO.\s\d+|reg.\sNO.\s\d+/, "").replaceAll(/\s{2,}/, ' ').trim();
    loc = loc.replaceAll(/(?i)(little\s+neck);(?=woodside)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(ROCKY\s+POINT),\s+(BRIDGEHAMPTON);(?=SOUND BEACH)/, "\$2 AND ")
    loc = loc.replaceAll(/(?i)(peekskill);(?=beacon)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(medford);(?=brooklyn)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(Rochester);(?=Brockport)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(plainview)\s+(?=ny)/, "\$1, ")
    loc = loc.replaceAll(/(?i)(Catskill);(?=Leeds)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(LEVITTOWN )(?=ny)/, "\$1, ")
    loc = loc.replaceAll(/(?i)(Tonawanda);(?=North Tonawanda)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(Cortlandt Manor);(?=Mahopac)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(Kings Park);(?=Smithtown)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(Cicero);(?=Syracuse)/, "\$1 AND ")
    loc = loc.replaceAll(/(?i)(Port Washington);(?=Centereach)/, "\$1 AND ")

    def addresses = splitAndTrim(loc, ";")
    def tmpAddr = []

    try {
        addresses.each {
            def multipleCityMtach
            if ((multipleCityMtach = it =~ /(?i)(.*?)\band\b(.*),(.*)/)) {
                def province = multipleCityMtach[0][3]
                tmpAddr.add(multipleCityMtach[0][1] + ',' + province)
                tmpAddr.add(multipleCityMtach[0][2] + ',' + province)
            }
        }
        if (tmpAddr.size() > 0) {
            addresses = tmpAddr
        }
    } catch (ConcurrentModificationException exception) {

    } catch (Throwable throwable) {

    }

    for (String address : addresses) {
        def addr = entity.newAddress()
        addr.setProvince("New York")
        def streetAddr1 = ""
        if ((elements = address.split(",")).size() > 1) { //more than one element
            if (elements[0] == 'LACHINE' && (elements[1] == ' QUEBEC' || elements[1] == 'QUEBEC')) {
                addr.setCity("LACHINE");
                addr.setProvince('QUEBEC');
                addr.setCountry('CANADA');
                entity.addAddress(addr)
                break
            }

            if (elements[-1].trim() in stateList) {//last element is a US state
                if (elements.size() > 2) {
                    int a;
                    for (a = 0; a < elements.size() - 2; a++) {
                        if (elements[a].contains("A/K/A")) {
                            als = elements[a].trim().split("A/K/A")
                            for (String l : als) {
                                addAlias(entity, l)
                            }
                        } else if (elements[a].contains("D/B/A")) {
                            entity.type = 'O'
                            als = elements[a].trim().split("D/B/A")
                            for (String l : als) {
                                addAlias(entity, l)
                            }
                        } else if (elements[a].contains("D.B.A")) {
                            entity.type = 'O'
                            als = elements[a].trim().split("D.B.A")
                            for (String l : als) {
                                addAlias(entity, l)
                            }
                        } else {
                            streetAddr1 += " " + elements[a]
                        }
                    }
                    streetAddr1 = streetAddr1.trim().replaceAll(/(?s)\s+/, " ").trim()
                    if (streetAddr1.startsWith("INC."))
                        streetAddress1 = streetAddr1.replaceFirst("INC.", "")
                    //fixme this should be part of entity name
                    if (streetAddr1.startsWith("P.C."))
                        streetAddress1 = streetAddr1.replaceFirst("P.C.", "")
                    if (!streetAddr1.equals("JR."))
                        addr.setAddress1(streetAddr1.trim())
                }
                if (elements[-2].contains("A/K/A")) {
                    al = elements[-2].trim().split("A/K/A")
                    for (String a : al) {
                        addAlias(entity, a)
                    }
                } else if (elements[-2].contains("D/B/A")) {
                    entity.type = 'O'
                    al = elements[-2].trim().split("D/B/A")
                    for (String a : al) {
                        addAlias(entity, a)
                    }
                } else if (elements[-2].contains("D.B.A")) {
                    entity.type = 'O'
                    al = elements[-2].trim().split("D.B.A")
                    for (String a : al) {
                        addAlias(entity, a)
                    }
                } else {
                    addr.setCity(elements[-2].trim())
                }
                addr.setProvince(elements[-1].trim())

            } else { //last element is not state abbrev, check to see if zip code is attached
                zip = elements[-1].trim().split(" ")
                if (zip[0] && zip[0].trim() in stateList) {//if so, set postal code and state
                    addr.setProvince(zip[0].trim())
                    addr.setPostalCode(zip[1].trim())
                    addr.setCity(elements[-2].trim())
                    for (int x = 0; x < elements.size() - 2; x++) {
                        if (elements[x].contains("A/K/A")) {
                            elems = elements[x].trim().split("A/K/A")
                            for (String v : elems) {
                                addAlias(entity, v)
                            }
                        }
                        streetAddr1 += " " + elements[x].trim()
                    }
                    addr.setAddress1(streetAddr1.trim())
                } else { //since it appears on the NY site, default address to NY
                    addr.setProvince("New York")
                }
            }
        }
        addr.setCountry("UNITED STATES")
        entity.addAddress(addr)
    }
}

def addAlias(entity, alias) {
    alias = alias.trim()
    if (!entity.equals("INC.") && alias.length() > 0)
        entity.addAlias(alias)
}

def clean(input) {
    while (input.startsWith(".") || input.startsWith(",") || input.endsWith(".") || input.endsWith(",")) {
        if (input.startsWith(".") || input.startsWith(","))
            input = input.substring(1, input.length()).trim()
        if (input.endsWith(".") || input.endsWith(","))
            input = input.substring(0, input.length() - 1).trim()
        if (input.equals(",") || input.equals(".")) {
            input = ""
        }
    }
    return input;
}

def handleSection(entity, actionDate, action, summary, pageUrl) {
    ScrapeEvent event = entity.newEvent();
    actionDate = actionDate.toString().trim().replaceAll(/(?:(?:[^<]+)?<\\/\w+>\s)(.*)/, "\$1")
    action = action.toString().trim().replaceAll(/(?:[^<]+<\/\w+>\s)(.*)/, "\$1")
    summary = summary.toString().trim().replaceAll(/(?:[^<]+<\/\w+>\s)(.*)/, "\$1")
    summary = StringUtils.removeEnd(summary, ",")

    def finalAction
    if (action == null) {
        finalAction = defaultAction + " " + summary
    } else if (summary == null) {
        finalAction = action + " " + defaultAction
    } else {
        finalAction = action + " " + summary
    }
    finalAction = finalAction.toString().replaceAll(/(?s)\s+/, " ").trim()
    def date = context.parseDate(new StringSource(actionDate))
    event.setDescription(finalAction)
    event.setDate(date)
    entity.addEvent(event)
}

def splitAndTrim(input, splitter) {
    if (!input)
        return null;

//    String[] splits;
    def splits = []
    if (!input.contains(splitter)) {
//        splits = new String[1];
        splits[0] = input.trim();
        return splits;
    }

    splits = input.split(splitter);
    int s;
    for (s = 0; s < splits.length; s++)
        splits[s] = splits[s].trim();
    return splits;
}

def detectEntityType(name, profession) {
    def type
    if (name =~ /(?i)\b(?:THE|DENTAL|PHARMACY|inc|P\.C|BOULEVARD|AL-CARE|DDS|Inc|LLP|PC|SERVICE|Hospital|Clinic|PHYSICAL THERAPY|DEPARTMENT|ll[pc]|l\.p|DRUGS|CORP(?:ORATION)?|PHARMACADE|HOSPITAL|HOME|SURGICAL|LTD|P\.C|L\.L\.C|\s+&\s+)\b/) {
        return "O"

    } else if (profession =~ /(?i)\b(?:Accountant|Acun?punc?t?urist|Anesthetist|Architect|Assistant|Audiologist|Counselor|Cytotechnologist|Designer|Diet[ei]tian|Dispenser|Engineer|Hiropractor|Hygienist|Intern|Midwifer?y?|Nurse|Nutritionist|Optomes?trist|Pathologist|Pharmact?i?st|Physicist|Podiatrist|Psychoanalyst|Psycholo?gist|Reporter|Rmacist|Surveyor|Technician|Technologist|Therapist|Trainer|Veterinarian)\b/) {
        return "P"

    } else if (profession =~ /(?i)\b(?:misdemeanor|Larceny)\b/) {
        //some special case taken from summary block
        return "P"

    } else if (profession =~ /(?i)\b(?:Veterinary|Optometry|Acupunc?ture|Podiatry|Chiropractic|Dispensing|Psychology|Analgesia|Conscious Sedation)\b/) {
        //not highly reliable set
        type = "P"

    } else {
        type = context.determineEntityType(name)
    }

    return type
}