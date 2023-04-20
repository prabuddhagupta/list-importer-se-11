package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.importer.scrapian.util.Tasker
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

context.setup([socketTimeout: 30000, connectionTimeout: 20000, retryCount: 1, multithread: true]);
context.session.encoding = "UTF-8";
context.getSession().setEscape(true);

Uk_unauthorised script = new Uk_unauthorised(context);
script.init();

//-----------------debug area-------------
//script.debug();
//
//-----------------debug area-------------

class Uk_unauthorised {
    final ScrapianContext context
    final addressParser
    final factoryRevision = "92e68ce6243d668ff4c8d665108d88dfc0b633ce"
    final moduleFactory = ModuleLoader.getFactory(factoryRevision)
    final Tasker tasker

    def config = [:]
    def currentConfig
    def newEntityCount = new AtomicInteger(0)

    //Actual url: https://www.fca.org.uk/consumers/unauthorised-firms-individuals
    def baseUrl = "https://www.fca.org.uk";
    //def postUrl = "https://www.fca.org.uk/views/ajax"
    def postUrl = "https://www.fca.org.uk/views/ajax?_wrapper_format=drupal_ajax"
    def http_headers = [
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Cookie': 'cookie-agreed=2; has_js=1'
    ]

    def nameMap = new ConcurrentHashMap(100, 0.75, 1)
    def addrSet = new ConcurrentHashMap(100, 0.75, 1)
    def urlsMap = [:]
    def preFilledFormData = [
        view_name      : "component_glossary",
        view_display_id: "component_glossary_news_warnings",
        view_args      : "all",
        view_base_path : "news-warnings-glossary",
        items_per_page : "100",
        page           : "0"
    ]

    Uk_unauthorised(context, threadsCount = 0) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
        tasker = new Tasker(threadsCount)

        config["firms_individuals"] = [:]
        config["firms_individuals"]["eventDescription"] =
            "This entity is not approved or authorized by the FCA to conduct regulated activities.";
        config["firms_individuals"]["dateColumn"] = 1;
        config["firms_individuals"]["sourceColumn"] = -1;

        currentConfig = config["firms_individuals"];
        nameMap.putAll(configFileParser(context.scriptParams.configFile))

        // addressParser.reloadData();

        addressParser.updatePostalCodes([
            "GB": [
                /(?-is)\b[A-Z][A-Z]?[O\d][A-Z\d]?(?:[\s-]*[A-Z\d]{1,2}[^cikmov\W\d]{1,2})\b/,
                /\bEC2(?:A|Y)\b/, /(?i)(?:P\.?O\.?\s*Box)?\s*\d{5}/,
                /\bG&4\s5PA\b/,
                /\bWLJ\s6BD\b/, /W1J\s8DJ1/, /\bSE1\b/,
                /\bEC3R\b/,
                /\bSA1\s+5QQ\b/,
                /\bW1J\b/, /\bL21\b/, /\bEC3\b/,
                /\b[A-Z]{3}\s+\d[A-Z]{2}\b/,
                /\bLY 7TG\b/, /\bBH13 7\b/, /\bEC2R 8\b/,
                /\bKY1-110(?:7|4)\b/,
                /\bE14\b/, /\bEC2N\b/, /W1g\s0PH/],
            "LI": [/(?s)\b(?:L[il]-)?94(?:8[5-9]|9[0-8])\b/,
                   /\bFL\d{4}\b/],
            "FR": [/\b(?-i)(?:F-)?\d{5}\b/],
            "CH": [/\b(?-i)(?:CH)?\d{4}\b/],
            "US": [/\b(?-i)TX\d{5}\b/, /\bDE\s*\d{5}\b/,
                   /(?-i)\bNY\s*\d{5}(?:-\d{4})?\b/, /\bTX\s*\d{5}\b/,
                   /FL\s*\d{5}/,
                   /60601/, /V6B\s+2W9/],
            "VG": [/(?i)(?:P\.O\.\s*Box)?\s*\d{4}/],
            "AE": [/(?i)(?:P\.?O\.?\s*Box)?\s*\d{4}/],
            "LU": [/\b(?i)(?:L-?)?U?\d{4}\b/],
            "CY": [/\b(?i)d{4}\b/, /\b\d{4}\b/],
            "ZA": [/\b(?i)d{4}\b/],
            "CZ": [/\b(?i)CZ\d{5}\b/, /\bCZ-331\s+14\b/],
            "DE": [/\b(?:DE)?\d{5}\b/],
            "IE": [/\b\d{4}\b/, /\b[A-Z]\d+\s+[A-Z]{2}\d+\b/],
            "CN": [/\b\d{5}\b/],
            "AT": [/\b[A-Z]\d{4}\b/, /\b\d{4}\b/],
            "BE": [/\b\d{4}\b/],
            "ES": [/\b\d{5}\b/],
            "DM": [/\b\d{5}\b/],
            "CA": [/\b[a-z]\d[a-z]\s*\d[a-z]\d\b/],
            "GR": [/\b\d{3}\s*\d{2}\b/],
            "MX": [/\bCP\s*\d{5}\b/],
            "DK": [/\bDK\s*\d{4}\b/],
            "IT": [/\bI-\d{5}\b/],
            "HR": [/\b\d{5}\b/],
            "PT": [/\b\d{4}-\d{3}\b/],
            "MH": [/\bMH\s*\d{5}\b/]
        ])
        addressParser.updateCities(["GB": ["Swansea"]], ["US": ["NEW YORK=/NY/"]])

        // addressParser.updateStates(["US": ["NEW YORK=/NY/"]])

    }

    def debug() {
        def urlList = ["https://www.fca.org.uk/news/warnings/assurance-et-capital-partners-clone-eea-authorised-firm"]
        urlList.each { url ->
            addEntity(context, new StringSource("Berrington fund management"), url, "05/07/2000", "hagjgajgdshsdgsd")
        }

    }

    def init() {
        //we will make extra iterations due to the fact that server sometimes do not return full set of data in any single iterations
        def extraIteration = 3
        def curItr = 0
        def totalItr = 0;

        //initial itr
        init_core();

        //println("total count:" + newEntityCount.get())
        newEntityCount.set(0)

        while (curItr < extraIteration) {
            init_core();

            totalItr++
            println("-------------EntityCount : " + newEntityCount.get())
            println("-------------Current Iteration : " + totalItr)

            if (newEntityCount.get() > 0) {
                curItr = 0;
                newEntityCount.set(0)

            } else {
                curItr++
            }
        }

        tasker.runFailedTasks()
        tasker.shutdown()
//        if (addrSet.size() > 0) {
//            println("Un-parseable addresses:\n-------------------------\n")
//            addrSet.each { k, v -> println(k) }
//        }
    }

    def init_core() {
        int maxPage = 1
        def curPage = 0
        def ucharMap = [:]
        preFilledFormData["page"] = "0"

        def textFormatter = { String text ->
            return text.replaceAll(/^.*?"data":"/, "")
                .replaceAll(/\\\//, "/")
                .replaceAll(/\\n/, "\n")
                .replaceAll(/","[^,]+$/, "")
                .replaceAll(/(?i)\\u(\w{4})/,
                {
                    def cv = ucharMap[it[1]]
                    if (!cv) {
                        cv = ucharMap[it[1]] = (char) Integer.parseInt(it[1], 16)
                    }
                    return cv;
                })
        }

        while (curPage <= maxPage) {
            def indexPage = context.invoke([url: postUrl, type: "POST", params: preFilledFormData, tidy: false, headers: http_headers, cache: true]);
            indexPage = textFormatter(indexPage.value)
            handleIndexPage(new StringSource(indexPage))

            curPage++;
            preFilledFormData["page"] = curPage + ""
            try {
                if (maxPage == 1) {
                    //maxPage = (indexPage =~ /(?i)=(\d+)\W+last/)[0][1] as int
                    maxPage = (indexPage =~ /(?i)=(\d+)\".+?last/)[0][1] as int
                }
            } catch (e) {
                println(indexPage)
                throw e;
            }
        }
    }

    def handleIndexPage(indexPage) {
        def myCount = 0;
        context.regexMatches(indexPage, [regex: "<table[^>]+?>(.*?)</table>"]).each { link ->
            if (!link[1].toString().contains("alerts-ticker")) {
                myCount += handleLetterPage(link[1]);
            }
        }

        return myCount;
    }

    def handleLetterPage(letterPage) {
        def lCount = 0;
        def firmSection = letterPage;
        if (firmSection.toString().contains("There are currently no firms")) {
            return 0;
        }

        context.elementSeek(firmSection, [element: "tr", startText: "<tbody>", endText: "</tbody>", greedy: false]).each { tr ->
            def cellArray = context.elementSeek(tr, [element: "td", greedy: false]);
            if (cellArray) {
                lCount++;
                def link = context.regexMatch(cellArray[0], [regex: "<a href=\"(.*?)\"(?:.*?)>(.*?)</a>.*"]);

                final def entityUrl;
                if (link) {
                    def urlPart = link[1].toString().trim();
                    if (urlsMap.containsKey(urlPart)) {
                        return;
                    } else {
                        urlsMap.put(urlPart, 1)
                    }
                    entityUrl = baseUrl + urlPart;
                }
                final def entityName = cellArray[0].stripHtmlTags().trim();
                final def eventDate;
                final def sourceHtml;

                if ((currentConfig["dateColumn"] >= 0) && cellArray[currentConfig["dateColumn"]]) {
                    eventDate = cellArray[currentConfig["dateColumn"]].stripHtmlTags().trim().toString();
                }
                if ((currentConfig["sourceColumn"] >= 0) && cellArray[currentConfig["sourceColumn"]]) {
                    sourceHtml = cellArray[currentConfig["sourceColumn"]].trim().toString();
                }

                tasker.execute({
                    addEntity(context, entityName, entityUrl, eventDate, sourceHtml);
                }, 2)
            }
        }

        return lCount;
    }

    def addEntity(context, entityName, entityUrl = "", eventDate = "", sourceHtml = "") {
        def name;
        def addrList = []
        def assocList = []

        if (entityName.getValue().length() > 0 && !entityName.equals("Back to top")) {
            def remarks = []
            def aliases = []
            def entity;
            def valMap = nameMap.get(entityName.getValue());
            def entityType = "P";

            if (valMap != null) {
                entityName = valMap[0]
                if (valMap[2] != "") {
                    remarks.add(valMap[2]);
                }

                List<ScrapeEntity> theList = context.session.getEntitiesByName(entityName);
                if (theList != null && theList.size() > 0) {
                    entity = theList.get(0);
                } else {
                    entity = context.session.newEntity();
                }

                entityName = finalSanitize(entityName)
                entity.setName(entityName);
                def splitUp = []
                if (valMap[1] != "") {
                    def tempAlias = valMap[1];
                    if (tempAlias.indexOf("_-_") > 0) {
                        splitUp = tempAlias.split("_-_");
                    } else {
                        splitUp = new String[1];
                        splitUp[0] = tempAlias;
                    }
                    splitUp.each { split ->

                        split = finalSanitize(split)
                        split = split.replaceAll(/(?i).*Clone of.*/, "")
                        if (split) {
                            entity.addAlias(split.replaceAll(/&amp;#039;/, "").trim())
                        }
                    }
                }

                createEntityCommonCore(entity, entityUrl, eventDate, sourceHtml, addrList, remarks)

            } else {
                entityName = entityName.replace("\\(Mr\\)", "").replace("&#226;&#128;&#147;", "-").replace("&#226;&#128;&#153;", "'").trim();
                entityName = entityName.replace("\\u2013", "-");     //en dash
                entityName = entityName.replace("\\u00f3", "o");
                entityName = entityName.replace("info- ", "info - ")
                entityName = entityName.replace("Group www.", "Group - www.")
                entityName = entityName.replace(", www\\.", " - www.")
                entityName = entityName.replace(", (?:or|at) ", " - ")
                entityName = entityName.replace(" . The following ", " - The following ")

                //alias detection
                entityName = entityName.replace("[,]* (also )?trading as ", " - trading as ")
                entityName = entityName.replace("(?i)(?:also)?(,|) t/a ", " a/k/a ")
                entityName = entityName.replace("(?i)(?:(?:previously)?\\s*also|sometimes|now) known as", "a/k/a")
                entityName = entityName.replace(", aka ", " aka ").replace("[?:\\s\\(]aka", "a/k/a")
                entityName = entityName.replace("a\\.k\\.a[\\.]*", "a/k/a")
                entityName = entityName.replace("\\(?previously", "a/k/a")
                entityName = entityName.replace(" - a/k/a", " a/k/a")
                entityName = entityName.replace(/(?i)(?:claims to be|trading name of|trading as)/, "a/k/a")

                if (entityName.getValue().contains(/\/fca.org.uk\/news\/warnings\//)) {
                    entityName = entityName.replace(/^.*?\/fca.org.uk\/news\/warnings\//, '');
                }

                if (!entityName.getValue().contains(".com")) {
                    entityName = entityName.replace("a/k/a", " TEMP_AKA ")
                    entityName = entityName.replace("([^/])/([^/])", "\$1 - \$2")
                    entityName = entityName.replace(" TEMP_AKA ", "a/k/a")

                }


                def detailPage = context.invoke([url: entityUrl, tidy: false, headers: ['Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'], cache: true]).toString()

                Thread.sleep(5000)
                detailPage = detailPageFix(detailPage)

                //general entity type detection
                if (detailPage =~ /(?i)this\W*(?:cloned?|unauthorised)\W*firm|an organisation identifying itself/) {
                    entityType = "O"
                } else if (detailPage =~ /(?i)We believe this firm\b[^\.]+without our authorisation/) {
                    entityType = "O"
                }

                //re-fix type for person
                if (detailPage =~ /(?i)\(\W*(?:cloned?|unauthorised)\W*individual\W*\)|Clone individual details/) {
                    entityType = "P"
                } else if (detailPage =~ /(?i)We\s*believe\s*a\s*fraudster/) {
                    entityType = "P"
                } else if (detailPage =~ /(?i)We\s*believe\s*this\s*individual/) {
                    entityType = "P"
                }


                if (entityName =~ /(?is)\[email\s*protected\]/) {
                    def entityMatch = detailPage =~ /(?is)"dc:title"\s*content="([^"]*)"/

                    if (entityMatch)
                        entityName = entityMatch[0][1].toString()
                }

                name = entityName.toString().trim();
                if (name.endsWith("*")) {
                    name = name.substring(0, name.length() - 1).trim();
                }
                if (isMissingRightParen(name)) {
                    name += ")"
                }
                if (entityName =~ /^EEA authorised firm$/) {
                    def nameMatch = detailPage =~ /(?is)property="og:title"\s*content="([^"]*)/
                    name = nameMatch[0][1].toString()
                }
                name = name.replaceAll(/\(Geneva; Switzerland\)/, "").trim()
                println("Entiy Name : " + name)
                context.regexMatches(new StringSource(name), [regex: "\\((.*?)\\)"]).each { peren ->
                    def perenAliases = extractAliases(peren[1])

                    def perenRemarks = extractRemarks(peren[1])
                    aliases.addAll(perenAliases)
                    remarks.addAll(perenRemarks)

                    if ((perenAliases.size() > 0) || (perenRemarks.size() > 0)) {
                        name = name.replace(peren[0].toString(), "").toString();
                    }
                }

                if (name.contains("a/k/a ")) {
                    def splitUp = name.split("a/k/a");
                    name = splitUp[0];
                    for (int i = 1; i < splitUp.length; i++) {
                        def aliasList = splitUp[i].toString().split(/,|(?i)(?<=pro)\s+and\s+(?=capital)/)
                        aliasList.each { alias ->
                            if (alias)
                                aliases.add(alias)
                        }
                    }
                }

                def firstPart = true;
                name.split(" [-/] ").each { part ->
                    if (firstPart) {
                        name = part
                        firstPart = false;
                    } else {
                        if (looksLikeRemark(part)) {
                            remarks.add(part.trim())
                        } else {

                            aliases.add(part.trim())
                        }
                    }
                }
                name = fixData(name)


                detailPage = detailPage.replaceAll(/(?is)<h(\d)>(EEA|FSA|FCA|UK)-?\s*(?:Authorised|Registered)\s*(Firm|companies)\s*Details.*/, "<h\$1>")
                detailPage = detailPage.replaceAll(/(?is)<h(\d)>Genuine\s*firm\s*(?:details)?.*/, "<h\$1>")

                //For capturing FCA|FSA|EEA registration
                if (entityName.toString() =~ /(?i)\s*cloned?\s*(?:firm)?\s*/) {
                    //def cloneMatch = detailPage =~ /(?is)<h3>.*?\(([^\)]*)/
                    def cloneMatch = detailPage =~ /(?is)<h3>.*?\(([^\)]*)\)+\s*<\/h3>/

                    if (cloneMatch) {
                        remarks.add(cloneMatch[0][1].toString().trim())
                    } else if ((cloneMatch = detailPage =~ /(?is)<h3>.*?\(([^\)]*)\)\s*\//)) {
                        remarks.add(cloneMatch[0][1].toString().trim())
                    } else if ((cloneMatch = detailPage =~ /(?is)<h3>.*?\(([^\)]*)\)+\s*(?:<b>|trading|may)/)) {
                        remarks.add(cloneMatch[0][1].toString().trim())
                    } else if ((cloneMatch = detailPage =~ /(?is)<h3>.*?\(([^\)]*)\)+\s*<br/)) {
                        remarks.add(cloneMatch[0][1].toString().trim())
                    } else if ((cloneMatch = detailPage =~ /(?is)<h2>.*?\(([^\)]*)\)<\/h2>/)) {
                        remarks.add(cloneMatch[0][1].toString().trim())
                    }
                }

                // For storing names under individual tags in associations
                def assocMatch = detailPage =~ /(?s)Individuals:(.*?)(?:Email:|Address:|Tel:)/
                if (assocMatch) {
                    def tempassocList = filterAssoc(assocMatch[0][1].toString().trim()).split(/\n|,|\band\b/)
                    assocList.addAll(tempassocList)
                }

                entityType = detectEntityType(name, entityType)

                def aEntity;

                name = filterName(sanitize(name));
                def nameList
                if (name =~ /\w+\s+\/\w+/) {
                    nameList = name.split("/")
                } else {
                    nameList = [name]
                }

                nameList.each {
                    name = it.toString()
                    List<ScrapeEntity> theList = context.session.getEntitiesByName(name);
                    if (theList != null && theList.size() > 0) {
                        entity = theList.get(0);
                    } else {
                        if (entityType.equals("O")) {
                            entity = createOrgEntity(name)
                        } else {
                            entity = createPersonEntity(name)
                        }
                    }

                    //website parse & add to alias
                    def webMatch = detailPage =~ /(?i)website(?:<\/b>)?:\s*(?:<[^>]+>\s*)*([^<]+)</
                    if (webMatch) {
                        if (webMatch[0][0].toString() =~ /,<$/) {
                            //	webMatch = detailPage =~ /(?is)website(?:<\/b>)?:\s*(?:<[^>]+>\s*)(.*?)<br\s*\/>/
                            webMatch = detailPage =~ /(?is)website(?:<\/b>)?:\s*(?:<[^>]+>\s*)(.*?)<\/?\s*(?:br|p)\s*\/?>/
                        }
                    }

                    webMatch.reset()
                    while (webMatch.find()) {
                        def webs = webMatch.group(1).replaceAll(/(?i)Firm name:.*|(?i)Address.*|(?i)Email.*|^\w+$/, "").trim()
                        webs = webs.replaceAll(/(?i)\s+AND\s+/, ",;").trim()
                        if (webs) {
                            webs.split(/\s*[,;]\s*/).each {
                                it = filterWebsite(it)
                                if (it) {
                                    it = it.replaceAll(/(?i)email.*|(?i)Telephone.*|(?i)Three Rose Lions o.*/, "").replaceAll("\\u200B", "").replaceAll(/\#/, "").replaceAll(/\[/, "")
                                    if (!it.isEmpty()) {
                                        it = finalSanitize(it)
                                        it = it.replaceAll(/(?i).*Clone.*/, "")
                                        if (it) {
                                            entity.addAlias(it.replaceAll(/&amp;#039;/, "").trim())
                                        }
                                    }
                                }
                            }
                        }
                        //alias detect from detailspage
                        def aliasTokens = /\b(?:(?>d[-\/\.]?b[-\/\.]?[ao]\b|[af][-\/\.]?k[-\/\.]?a\b)[-\/\.]?|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\s*\/\s*a)|(?>previously|also|former?ly)(?: known as)?\b)\s*/
                        //def aliasMatch = detailPage =~ /(?is)container copy-highlighted.*?<h3>([^<]+)<\/h3>/
                        def aliasMatch = detailPage =~ /(?is)"(?:component\s*)?copy-highlighted".*?<h3>([^<]+)<\/h3>/
                        if (aliasMatch) {
                            def match = aliasMatch[0][1] =~ /(.*)$aliasTokens(.*)/
                            if (match) {
                                def alias = filterAlias(match[0][2].replaceAll(/\)$/, "").replaceAll(/:/, "").replaceAll(/\(BEL\)/, ""))
                                if (alias) {
                                    def aliasList = alias.split(/,|\bor\b|\/|\band\b/)
                                    aliasList.each {
                                        aliases.add(it)
                                    }
                                }
                            }
                        }
                        //else if ((aliasMatch = detailPage =~ /(?is)container copy-highlighted.*?<h3>(.*?)<\/h3>/)) {
                        else if ((aliasMatch = detailPage =~ /(?is)"copy-highlighted".*?<h3>(.*?)<\/h3>/)) {
                            def match = aliasMatch[0][1] =~ /(.*)$aliasTokens(.*)/
                            if (match) {
                                def alias = filterAlias(match[0][2].replaceAll(/\)$/, "").replaceAll(/:/, ""))
                                if (alias) {
                                    def aliasList = alias.split(/,|\bor\b|\/|\band\b/)
                                    aliasList.each {
                                        aliases.add(it)
                                    }
                                }
                            }
                        }
                    }

                    /*      if(entityUrl =~/(?is)https:\/\/www.fca.org.uk\/news\/warnings\/dominion-financial-assets-clone-authorised-firm/) {
                              println("D")
                          }*/
                    addrList = addressParser(detailPage)
                    aliases.each { alias ->
                        alias = filterName(sanitize(alias))
                        def aliasType = detectEntityType(alias, entityType);
                        if (aliasType.equals(entityType)) {
                            alias = finalSanitize(alias)
                            it = it.replaceAll(/(?i).*Clone.*/, "")
                            if (alias) {
                                entity.addAlias(alias.replaceAll(/&amp;#039;/, "").trim())
                            }

                        } else {
                            entity.addAssociation(alias.trim())
                            if (aliasType.equals("O")) {
                                aEntity = createOrgEntity(alias)
                                aEntity.addAssociation(name.trim());
                                createEntityCommonCore(aEntity, entityUrl, eventDate, sourceHtml, addrList, remarks, assocList)

                            } else {
                                aEntity = createPersonEntity(alias)
                                aEntity.addAssociation(name.trim());
                                createEntityCommonCore(aEntity, entityUrl, eventDate, sourceHtml, addrList, remarks, assocList)
                            }
                        }
                    }

                    createEntityCommonCore(entity, entityUrl, eventDate, sourceHtml, addrList, remarks, assocList)
                }
            }
        }
    }

    def createOrgEntity(name, aliasList = []) {
        name = finalSanitize(name)
        def entity = context.findEntity(["name": name, "type": "O"]);
        if (!entity) {
            entity = context.getSession().newEntity();
            name = filterName(sanitize(name));
            entity.setName(name);
            entity.type = "O";
            //entity creation counter
            newEntityCount.incrementAndGet()
        }

        return entity;
    }
// As working on regex would be risky at this point,(April,2021)
// the method deals with entity
// and it's details sanitization except for Addresses
    def finalSanitize(String inp) {
        //Set = [\u0159, \u1EA3, \u1EC1, \u1ED1, \uFB01]
        inp = inp.replaceAll(/^\+\d+.+?\d+$|\;|(?i)^(and|or)\b|^www$|(?i)isnot.*/, "").trim()
        inp = inp.replaceAll(/(?i)Telephone.+|(?i)Address.*|(?i)Emails.*/, "").trim()
        inp = inp.replaceAll("a/k/a", "").trim()
        inp = inp.replaceAll(/(?i)\(Mergers &amp;\)/, "Mergers &amp;")
        inp = inp.replaceAll(/\(.+?(?:\)|$)/, "").trim()
        inp = inp.replaceAll(/&amp;/, "&").trim()
        inp = inp.replaceAll(/(?s)\s+|^\W+|\W+$|&amp;#039;|\)|\(|(?:\u0159|\u1EA3|\u1EC1|\u1ED1|\uFB01)/, " ").trim()
        return inp.replaceAll(/\s+/, " ").trim()

    }

    def createPersonEntity(name, aliasList = []) {
        name = finalSanitize(name)
        def entity = context.getSession().newEntity();
        entity.name = name
        entity.type = "P";

        //entity creation counter
        newEntityCount.incrementAndGet()

        return entity;
    }
//Set = [\u0159, \u1EA3, \u1EC1, \u1ED1, \uFB01]
    def createEntityCommonCore(entity, entityUrl, eventDate, sourceHtml, addrList, remark, assocList = []) {
        if (entityUrl.length() > 0) {
            entityUrl = entityUrl.replace("%20", "").replace("hhhtp:", "hhtp:");
            // some bad URLs
            if (entityUrl.startsWith("http")) {
                entity.addUrl(entityUrl);
            }
        }

        def event = new ScrapeEvent()
        event.setDescription(currentConfig["eventDescription"]);
        if (eventDate.length() == 10) {
            event.setDate(context.parseDate(new StringSource(eventDate), "dd/MM/yyyy"));
        } else if (eventDate.length() == 8) {
            event.setDate(context.parseDate(new StringSource(eventDate), "dd/MM/yy"));
        }

        //correction for LI-648 //as of 9/17/2014 it was provable here that this is the date: http://www.fca.org.uk/news/warnings/bagnestone
        if (entity.getName() == 'Bagnestone' && eventDate.endsWith("14")) {
            event.setDate(context.parseDate(new StringSource('16/11/2004'), "dd/MM/yyyy"));
        }
        if (entity.getName() == 'Equidebt Limited' && event.getDate() == '06/17/2016') {
            event.setDate(null);
        }
        entity.addEvent(event)

        if (sourceHtml?.length() > 0) {
            setSource(entity, sourceHtml)
        }


        if (entity.getName() =~ /Anthony\s*Williams/) {
            //fix for Anthony Williams replacing skipton from street and adding it in association
            entity.addAssociation("Skipton Asset Management")
        }

        Set curRemarks = entity.getRemarks()
        def dupRemarkFinder = { rem ->
            def found = false;
            curRemarks.any {
                rem = rem.replaceAll(/(?i)\(|\)/, "").trim()
                it = finalSanitize(it)
                if (it ==~ /(?i)$rem/) {
                    found = true;
                    return true;
                }
            }
            return found;
        }

        remark.each { rem ->
            rem = filterRemark(rem)

            if (rem.toString() =~ /\w+/) {
                rem = rem.toString().substring(0, 1).toUpperCase() + rem.toString().substring(1)
                rem = sanitize(rem);
                if (!dupRemarkFinder(rem)) {
                    entity.addRemark(rem)
                }
            }
        }

        assocList.each { assoc ->
            if (assoc)
                entity.addAssociation(assoc.trim())
        }

        if (addrList.size > 0) {
            addrList.each { scrapeAddress ->
                scrapeAddress = finalAddressSantize(scrapeAddress)
                if (scrapeAddress) {
                    entity.addAddress(scrapeAddress);
                }
            }
        }
        println("Entity:$entity.name\nAliases: $entity.aliases\nAddress:$entity.addresses\n")
    }

    def finalAddressSantize(def address) {
        String address1 = address.address1
        String country = address.country
        String postals = address.postalCode
        String city = address.city
        if (address1) {
            address1 = address1.replaceAll(/^\W+|-|\W+$/, "").trim()
            address1 = address1.replaceAll(/(?i)(?:null|$postals|$country|United Kingdom|United States|\,|The$|$city$|\W+$)/, "").trim()
            address.address1 = address1.replaceAll(/\s+/, " ").trim()

        }
        if (city =~ /(?i)(\d+|floor)/ && address1 != null) {
            address1 += " " + city
            address.address1 = address1.replaceAll(/\W+$|\;|-/, " ").trim()
            address.address1 = address1.replaceAll(/\s+/, " ").trim()
            address.city = null
        }
        return address
    }

    def addressParser(detailPage) {
        def addr;
        def addrList = [];

        //few initial fixes
        detailPage = detailPage.replaceAll(/(?i)purporting to be[^\n]+?address is(.*)/, "Address:\$1")

        def addr1Match = detailPage =~ /(?is)>(?:previous)?\s*Address(?:es)?(?:\s*\d+\s*)?:\s*(?:<[^>]*>\s*)*([^:]+)(?:<[^>]*>\s*){1,3}[\w\s]+:/
        if (addr1Match.find()) {
            addr = addr1Match[0][1]
            detailPage
            if (detailPage =~ /<h3>AXN Trading Ltd/) {
                def rmvaddr = addr1Match.group(0).toString()
                detailPage = detailPage.replaceAll(/$rmvaddr/, "<b>Address:</b>")
                if ((addr1Match = detailPage =~ /(?is)>(?:previous)?\s*Address(?:es)?(?:\s*\d+\s*)?:\s*(?:<[^>]*>\s*)*([^:]+)(?:<[^>]*>\s*){1,3}[\w\s]+:/)) {
                    addr = addr + "-split-" + addr1Match.group(1).toString()
                }
            }

        } else if ((addr1Match = detailPage =~ /(?is)>(?:previous)?\s*Address(?:es)?(.*?)<h2/)) {
            addr = addr1Match[0][1]

        } else if ((addr1Match = detailPage =~ /(?is)>(?:previous)?\s*Address(?:es)?(?:\s*\d+\s*)?:\s*(?:<[^>]*>\s*)*(.*?)how\s*to\s*protect/)) {
            addr = addr1Match[0][1]

        } else if ((addr1Match = detailPage =~ /(?s)<h2>Accer Mergers &amp; Acquisitions<\/h2>(.*?)Tel/)) {
            //For capturing address of Accer Mergers & Accusitions
            addr = addr1Match[0][1]
            addr = addr.toString().replaceAll(/<p>/, "")
                .replaceAll(/<br\s\/>/, "")
                .replaceAll(/\n/, ",")

        } else if ((addr1Match = detailPage =~ /(?i)address\s*\([^\)]+\)\s*:\s*(?:<[^>]*>\s*)*([^:]+)(?:<[^>]*>\s*){1,3}(?=[^<>:]+:)/)) {
            int i = 1;
            addr1Match.reset();
            while (addr1Match.find()) {

                def a = addr1Match.group(1).trim()
                if (i > 1) {
                    addr = addr + '-split-' + a
                } else {
                    addr = a
                }
                i++;
            }
        } else if ((addr1Match = detailPage =~ /(?is)>Address\s*stated:([^<]*)/)) {
            addr = addr1Match[0][1]

        } else if ((addr1Match = detailPage =~ /(?is)(?<!(?:email|web) )address(?:\s*<[^>]*>\s*)*:(?:\s*<[^>]*>\s*)*([^<]+)<[^>]+>\n*/)) {
            addr = addr1Match[0][1]

        } else if ((addr1Match = detailPage =~ /(?is)>address(?:es)?\b<[^>]*>:(?:<[^>]*>)*(.*?)telephone number/)) {
            addr = addr1Match[0][1]
        } else {
            if (detailPage =~ /(?i)address/) {
                println "Un-captureable address format!"
            }
        }

        if (addr) {
            def ukPostalPattern = /(?-is)\b[A-Z][A-Z]?[OCLWY\d][A-Z\d]?(?:[\s-]*[A-Z\d]{1,2}[^cikmov\W\d]{1,2})\b/
            addr = preAddrFix(addr)
            //split non-postal patterned uk city endings
            addr = addr.replaceAll(/(?i)\b(?:London)\b(?!(?:<[^>]+>|\W)*$ukPostalPattern)(?!\s*Bridge)/, '$0-split-')
            addr = addr.replaceAll(/(?is)($ukPostalPattern)\s*<br\s*\/?>/, '$1-split-')
            //change
            addr = addr.replaceAll(/London-split-\s*-\s*England/, "London,England")
            //major split fixing
            addr = addr.replaceAll(/(?i)\b(Hong Kong|Canada|CB23 6DW|Taiwan|GU34 2PZ|Austria|SO22 5QX|N1 7GU|M1 5JW|Japan|Czech Republic|Singapore|UK|SW1H0RG|BS1PL|Arab Emirates|United Kingdom|KY1-1104|Switzerland|Taipei City 10018)\b\s*<[^>]+>/, '$1-split-')

            //br tag fixing
            addr = addr.replaceAll(/(?i)<br\s*\/>\n+(?=(?:\w+[,\s.-]*){1,4}(?:<|$))/, ',')
            //change
            addr = addr.replaceAll(/(?i)(?:Leon LP)\s*<br\s*\/?>/, '$0,')
            addr = addr.replaceAll(/(?is)UNITED KINGDOM \(Authorised firms address\)/, 'UNITED KINGDOM')
            addr = addr.replaceAll(/(?i)(?<!tower)<br\s*\/?>(?!,)\n?/, '-split-')

            //change
            addr = addr.replaceAll(/(?i)((?-i:[A-Z][A-Z\d]{2,})\.?\s*)(?=[,-]*\s*(?:dashwood|sheikh|Addax|30 st|124 new))/, '$1-split-')
            addr = addr.replaceAll(/(?i)(\b(?:japan|us)\b\s*-)(?!\d{5,})/, '$1-split-')
            addr = addr.replaceAll(/(?i)\/\s+(?=po\s*(?:box)?\s*\d+)/, '-split-')
            addr = addr.replaceAll(/\band\b(?=\s*(?:the Grenadines|Olsson|\d))\b/, "&")
            addr = addr.replaceAll(/(?s)Manhattan/, "Manhattan,New York,")
            addr = addr.replaceAll(/(?s)Hesperange/, "Hesperange,Luxembourg City")
            addr = addr.replaceAll(/(?is)\(sic\)\s*\(this\s*is\s*the\s*Companies\s*House\s*address\s*of\s*the\s*authorised\s*firm\)/, "")
            addr = addr.replaceAll(/(?is)([a-z]{2}\d{2}\s*\d[a-z]{2})\s*,(\s*Dashwood\s*House)/, "\$1split-\$2")
            addr = addr.replaceAll(/(?s)&\s*25\s*Park\s*Lane/, "and 25 Park Lane")

            //replacing & with and for splitting
            addr = addr.replaceAll(/(?s)&\s*37th\s*Floor/, "and 37th Floor")
            addr = addr.replaceAll(/(?s)&\s*116\s*Ballars\s*Road/, "and 116 Ballars Road")
            addr = addr.replaceAll(/(?s)&\s*236\s*Priory\s*Road/, "and 236 Priory Road")
            addr = addr.replaceAll(/(?s)-split-,\s*South\s*Africa/, ",South Africa")
            addr = addr.replaceAll(/(?s)8008\s*Zurich/, "8008,Zurich")
            addr = addr.replaceAll(/(?s)\s*\(also\s*using.*?\)/, "")
            addr = addr.replaceAll(/(?s)London-split-\s*W1J\s*8DJ1,\s*England(?:-split-|<br\s*\/?>)/, ",W1J 8DJ1,London,England")

            //fixing london Split for proper address matching so that single address remains intact for London keyword
            addr = addr.replaceAll(/(?s)London-split-,\s*SE1/, "London,SE1")
            addr = addr.replaceAll(/(?s)London-split-\s*Wall/, "London Wall")
            addr = addr.replaceAll(/(?is)London-split-,\s*United\s*Kingdom/, "London,United Kingdom")
            addr = addr.replaceAll(/(?s)London-split-,\s*W1J/, "London,W1J")
            addr = addr.replaceAll(/(?s)London-split-\s*Riverside/, "London Riverside")
            addr = addr.replaceAll(/(?s)London-split-\s*Place/, "London Place")
            addr = addr.replaceAll(/(?s)London-split-\s*E14/, "London,E14")
            addr = addr.replaceAll(/(?s)London-split-\s*Wall/, "London Wall")
            addr = addr.replaceAll(/(?s)London-split-,\s*London/, "London")
            addr = addr.replaceAll(/(?s)London-split-\s*EC?3/, "London,EC3")
            addr = addr.replaceAll(/(?s)London-split-\s*Court/, "London Court")
            addr = addr.replaceAll(/(?s)London-split-\s*Road/, "London Road")
            addr = addr.replaceAll(/(?s)London-split-,\s*Greater\s*London/, "London,Greater London")
            addr = addr.replaceAll(/(?s)London-split-,\s*England/, "London,England")
            addr = addr.replaceAll(/(?s)i+\)/, "") // replacing roman letters
            addr = addr.replaceAll(/(?is)-\s*Sheikh/, "-split- Sheikh")
            addr = addr.replaceAll(/(?is):<\/strong><\/p>/, "")
            addr = addr.replaceAll(/(?is),\s*Swansea\s*SA1\s*SQQ/, ", Swansea, SA1 SQQ")
            addr = addr.replaceAll(/(?is)SA1 SQQ/, "SA1 5QQ")
            addr = addr.replaceAll(/(?is)GU22\s+6DJ\.\s+Addax/, "GU22 6DJ.-split- Addax")
            addr = addr.replaceAll(/(?is)&\s+29\s+Earlsfort/, "and 29 Earlsfort")
            addr = addr.replaceAll(/(?is)3rd\s+Floor;\s+31\s+Southampton/, "3rd Floor, 31 Southampton")
            addr = addr.replaceAll(/(?is)SK3\s+8AK,\s+30\s+St\s+Mary/, "SK3 8AK,-split- 30 St Mary")
            addr = addr.replaceAll(/(?is)EC3A\s+8EP,\s+124\s+New\s+Bond/, "EC3A 8EP,-split- 124 New Bond")
            addr = addr.replaceAll(/(?is)E3\s+5LU,\s+1\s+Victoria\s+Square/, "E3 5LU,-split- 1 Victoria Square")
            addr = addr.replaceAll(/(?is)<p><strong>Phone:\s*<\/strong>\s*01256\s*244\s*293/, "")
            addr = addr.replaceAll(/(?is)\(address does not exist\)/, "")
            addr = addr.replaceAll(/(?is)\(companies house no: 3365807\)/, "")
            addr = addr.replaceAll(/(?is)2449:\s*Rue\s*Due\s*Rhone\s*14/, "2449:-split- Rue Due Rhone 14")
            addr = addr.replaceAll(/(?is)Exeter\s*United Kingdom/, "Exeter,United Kingdom")
            addr = addr.replaceAll(/(?is)United States: 85 W Madison Street,/, "United States:-split- 85 W Madison Street,")
            addr = addr.replaceAll(/(?is)KA8 8AZ & 7 Alloway Place/, "KA8 8AZ -split- 7 Alloway Place")
            addr = addr.replaceAll(/(?-i)(\b[A-Z]\w*(?=\d)\w*\b\s+\w*(?=\d)\w*\b)\s*\/(.*?)(\b[A-Z]\w*(?=\d)\w*\b\s+\w*(?=\d)\w*\b)\s*\//, "\$1 -split- \$2 \$3 -split-")
            // for splitting three addresses separated by / (i.e. Street, London, W1B 5NL/ 207 Regent Street, W1B 3HH/ 57 Hay Market)
            addr = addr.replaceAll(/(?-i)(\b[A-Z]\w*(?=\d)\w*\b\s+\w*(?=\d)\w*\b)\s*\//, "\$1 -split-")
            // for splitting two addresses separated by / (i.e. 0-22 Wenlock Road, N1 7GV / 2nd Floor, 145-157 St)
            addr = addr.replaceAll(/EC3A 8BF & 5th Secretary's Lane/, "EC3A 8BF -split- 5th Secretary's Lane")
            addr = addr.replaceAll(/London\s*EC2R\s*7HJ\s*&\s*125\s*Old\s*Broad\s*Street/, "London EC2R 7HJ -split- 125 Old Broad Street")
            addr = addr.replaceAll(/London, W1B 5NL & 207 Regent Street/, "London, W1B 5NL -split- 207 Regent Street")
            addr = addr.replaceAll(/EC2M 7PY|C2M7PY/, "EC2M 7PY,London")
            addr = addr.replaceAll(/London-split-, W1g 0PH/, "London, W1g 0PH")

            addr = addr.replaceAll(/London-split-,?\s*UK(?:-split-)?/, "London, UK")
            addr = addr.replaceAll(/London-split-,\s*UK,\s*EC4R 8AN;/, "London, UK, EC4R 8AN;-split-")
            addr = addr.replaceAll(/Greater\s*London-split-,\s*Middlesex,\s*HA9\s*0JD/, "Greater London, Middlesex, HA9 0JD")
            addr = addr.replaceAll(/London-split- Lloyds Building/, "London Lloyds Building")

            //garbage
            addr = addr.replaceAll(/<.+?>|-split-/, "")
            addr = addr.replaceAll(/(?s)\n/, ";")
            addr = addr.replaceAll(/(?i)(?:ST.?|Saint) VINCENT/, ",SAINT VINCENT")
            addr = addr.replaceAll(/(?i)SAINT VINCENT & the Grenadines\W+(?:ST.|Saint)\W+VINCENT AND THE GRENADINES/, ",SAINT VINCENT & THE GRENADINES")
            addr = addr.replaceAll(/(?i)VINCENT AND THE GRENADINES/, "VINCENT & THE GRENADINES")
            addr = addr.replaceAll(/(?i)Company No..+|Hong Kong Gold and Silver Commercial Building|4XFX is owned and operated by GRF EUROPE OU\W+/, "").trim()
            addr = addr.replaceAll(/(?is)Firm name:.+|(?i)Email.*|(?i)The republic of|Company house number 06247592/, "")
            addr = addr.replaceAll(/St Crispin's House; Duke Street; Norwich; NR3 1PD/, "St Crispin's House, Duke Street, Norwich, NR3 1PD, UK")
            addr = addr.replaceAll(/said to be based in Brussels\)/, "Brussels")
            addr = addr.replaceAll(/11 Harvest Bank; Hyde Heath; Amersham; Buckinghamshire HP6 5RD/, "11 Harvest Bank, Hyde Heath, Amersham, Buckinghamshire HP6 5RD")
            addr = addr.replaceAll(/1 Red wood Crescent; East Kilbride; Scotland G&amp;4 5PA/, "1 Red wood Crescent, East Kilbride, Scotland, G4 5PA")
            addr = addr.replaceAll(/Trust Company Complex, Ajeltake Road Ajeltake Island Majuro, MARSHALLS ISLAND, MH 96960/, "Trust Company Complex, Ajeltake Road Ajeltake Island, Majuro, MARSHALLS ISLAND, MH 96960")
            addr = addr.replaceAll(/C\/o Suite 305, Griffith Corporate Centre, Beachmont, Kingstown\W+"/, "C/o Suite 305, Griffith Corporate Centre, Beachmont, Kingstown")
            addr = addr.replaceAll(/11 Harvest Bank; Hyde Heath; Amersham; Buckinghamshire HP6 5RD/, "11 Harvest Bank, Hyde Heath, Amersham, Buckinghamshire HP6 5RD")
            addr = addr.replaceAll(/15 Stratton St;Mayfair;, London,United Kingdom, W1J 8LQ/, "15 Stratton St,Mayfair, London,United Kingdom, W1J 8LQ")
            addr = addr.replaceAll(/\b(Unit G25 Waterfront Studios|Street|\bSt|The Shard|Avenue|Floor|Suite \w+|Lane|Court|London)\W+/, '$1,')
            addr = addr.replaceAll(/(?:ř|ả|ề|ố|ﬁ)/, "")
            addr = addr.replaceAll(/(?ism)\(.+?\)/, "").trim()
            addr = addr.replaceAll(/\(/, "")
            addr = addr.replaceAll(/%/, "").trim()
            addr = addr.replaceAll(/(?s)\s+/, " ").trim()
            addr = addr.replaceAll(/(?i)MARSHALLS ISLAND\b/, "MARSHALLS ISLANDS")
            addr = addr.replaceAll(/(?i)\bUSA\b|(?i)United States of America|(?i)\bUS\b/, ",United States").trim()
            addr = addr.replaceAll(/(?i)\W+New York\W+NY/, ",NY,").trim()
            addr = addr.replaceAll(/(?i);Fitzrovia;London,United Kingdom;W1W 8AF/, ",Fitzrovia,London,United Kingdom,W1W 8AF")
            addr = addr.replaceAll(/(?i)Unit 111088;2nd/, "Unit 111088,2nd")
            addr = addr.replaceAll(/(?i)Canada Square\W+/, "Canada Square,")
            addr = addr.replaceAll(/(?i)Floor\W+151 Madison Avenue,/, "Floor 151 Madison Avenue,")
            addr = addr.replaceAll(/(?i)\W*517 District/, "517,")
            addr = addr.replaceAll(/(?i)709-710\W+7\/F\W+Tower 1/, "709-710, 7/F, Tower 1")
            addr = addr.replaceAll(/(?i)^(\d+-\w*)\;/, '$1,')
            addr = addr.replaceAll(/(?i)Lansdowne Place.+?Holdenhurst Road.+?Bournemouth/, "Lansdowne Place,Holdenhurst Road,Bournemouth")
            addr = addr.replaceAll(/(?i)(Unit\s+)(\w+)\W+/, '$1$2,')
            addr = addr.replaceAll(/(?i)Berndern/, ",Brandon,")
            //punctuations
            addr = addr.replaceAll(/(?s)\;\W+/, ";")
            addr = addr.replaceAll(/\,+|\,\W+\,+/, ",").trim()
            addr = addr.replaceAll(/^\W+|\W+$|&amp;/, "").trim()
            addr = addr.replaceAll(/(?s)\s+/, " ").trim()
            addr = addr.replaceAll(/(?i)(3 Piccadilly Pl|The Link|10 Fleet Pl|7 Soundwell Road|60 THREADNEEDLE STREET|THE SOUTH QUAY BUILDING|30 Finsbury Square|Place|47 Whitedown Lane\W+Alton|The Gherkin|Kewstoke|Street\W+Cowden|The Mission House|Colwick Business Estate|35 Churchill Park|Staple Hill|Bristol|Montana Leon LP|Mansfield|Rear of 168|Gateway|LEVEL \d+|DE CV\W+AVDA|Lancaster Road|Business Park|Lyoner Strasse|Suite 306,310|Time Square|1050 Bruxells, 1050|350 5th Ave|Hopwell Center|\d+th fl\w*r|floor.?\W*\d{1,2}|\d+\/[A-Z]+|405 Kings Road|Hill Crest\W+Villa|Office \d{1,2}|Regus|Air Park|House|205 East)\W+/, '$1,')
            addr = addr.replaceAll(/(\;\W*)(\w+)(\W*\;\W*)/, ',$2,')

            //garbage
            addr = addr.replaceAll(/(?i)Avenue,33rd Floor/, "Avenue 33rd Floor")
            addr = addr.replaceAll(/(?i)Suite 305,Griffith Corporate Centre;Beachmont;PO Box 1510;Kingstown;/, "Suite 305,Griffith Corporate Centre,Beachmont,PO Box 1510,Kingstown,")
            addr = addr.replaceAll(/(?i)Putney Wharf;London,SW15 2JX/, "Putney Wharf,London,SW15 2JX")
            addr = addr.replaceAll(/(?i)1574 Sofia;R.A;Hristo Smirnenski;112 Geo Milev Str.;Partner Floor,Office 1/, "1574 Sofia,R.A,Hristo Smirnenski,112 Geo Milev Str.,Partner Floor,Office 1")
            addr = addr.replaceAll(/(?i)Island Majuro\W*/, "Island, Majuro,")
            addr = addr.replaceAll(/(?i)44 Downside Heights Skerries Co. Dublin K34 N920/, "44 Downside Heights Skerries Co., Dublin, K34 N920")
            addr = addr.replaceAll(/(?i)(Street|Building|Road Leyton|Langbourn|Str|Millennium Plaza|Hatton Garden|Centre Beachmont|Burj Khalifa|Complex|tower|Ave|Aveneue|Road|Business Center|The Pines \/ 7a|^\d+|\,\s*\d{2,3}|Huttisstrasse 8)\s*\;/, '$1,')
            addr = addr.replaceAll(/(?i)PO BOX 362 Road Town/, "PO BOX 362 Road Town,")
            addr = addr.replaceAll(/(?i)MARSHALLS ISLANDS/, "MARSHALL ISLANDS")
            //split add
            def addSplit = { a, b ->
                addr = addr.replaceAll(/$a\W+$b/, a + "-split-" + b)

            }
            addSplit("UK", "Mainzer")
            addSplit("Switzerland", "40")
            addSplit("Ireland", "PO Box")
            addr = addr.replaceAll(/(?i)NY$/, "New York, United States")
            addr = addr.replaceAll(/(?i)(Street|Fitzrovia|Mary Axe|Square|Centre|Ave|Aveneue|Road|57\/59 Haymarket)\s*\;/, '$1,')
            addr = addr.replaceAll(/(?s)\,\s*\,/, ",")
            addr = addr.replaceAll(/(?s)\s+|Tel:.*|Trading as.*/, " ").trim()
            println("Addr: $addr\n")
            addr.split(/(?is)(?:<\/[^>]+>\s*<[^\/>]+>|\band\b|(?<!(?:&amp));|(?-i:-?split-)+)/).each {
                def addrString ->
                    addrString = miscAddrFix(addrString)
                    addrString = addrString.toString().replaceAll(/South Africa,\s*UK/, "South Africa")
                    addrString = addrString.toString().replaceAll(/Ireland,\s+D02\s+RY98,\s+UK$/, "Ireland, D02 RY98")
                    addrString = addrString.toString().replaceAll(/(?is)bt systems ltd \(([^\)]+)\)/, { a, b -> return b })
                    addrString = addrString.toString().replaceAll(/(?is)\(this was a previous[^\)]+\)/, "")

                    if (addrString) {
                        addrString = addrString.replaceAll(/^\W+|\W+$|&amp;/, "").trim()
                        def addrMap = addressParser.parseAddress([text: addrString, force_country: true]);
                        def street_sanitizer = { street ->
                            // println("S:")
                            return street.replaceAll(/(?i)\bPO Box\W*$\b|(?:,\s*){2,}.*$/, '')
                                .replaceAll(/(?:\bUK\b\W*)+$/, "")
                                .replaceAll(/^[\s,-]+|\W+$/, "")
                                .replaceAll(/^Skipton\s*Asset\s*Management,/, "")//modifying skipton asset management
                                .replaceAll(/\s*Andersen\s*Consulting\s*\(UK\),/, "")//removing andersen consulting from street
                                .replaceAll(/P\.?O\.?\s*Box/, "")
                                .replaceAll(/United\s*Kingdom/, "")
                                .replaceAll(/Based\s*in/, "")
                                .replaceAll(/\bN\.?Y\b/, "")
                                .replaceAll(/Republic\s*of/, "")
                                .replaceAll(/Dublin,?\s*(?:1|2)/, "")
                                .replaceAll(/,\s*Dublin\s*,$/, "")
                                .replaceAll(/(?i)New\s*York/, "")
                                .replaceAll(/operating\s*from/, "")
                                .replaceAll(/,\s*\(same\s*as\s*FCA\s*authorised\s*firm/, "")
                                .replaceAll(/UK\)/, "")
                                .replaceAll(/\(EEA\s*Authorised\s*Firm[\u2019]s\s*Address/, "")
                                .replaceAll(/This\s*firm\s*also\s*claims.*/, "")
                                .replaceAll(/believed\s*to\s*be\s*claiming.*/, "")
                                .replaceAll(/Greater$/, "")
                                .replaceAll(/,\s*Usa$/, "")
                                .replaceAll(/(?is)^based\s+in$/, "")
                                .replaceAll(/(?is)\(former\s*address\s*of\s*FCA\s*authorised\s*firm$/, "")
                                .replaceAll(/,(?!\s)/, ", ")
                                .replaceAll(/^\d\./, "")
                                .replaceAll(/\bBR\b/, "")
                                .replaceAll(/\bCH\b/, "")
                                .replaceAll(/EC2A/, "")
                                .replaceAll(/\(using FCA Authorised Firm.?s address/, "")
                                .replaceAll(/EX10\s*9XH/, "")
                                .replaceAll(/Based in Geneva/, "")
                                .replaceAll(/^London$/, "")
                                .replaceAll(/(?i)\bIl\b/, "")
                                .replaceAll(/\(7 Av\/34 St/, ",7 Av/34 St")
                                .replaceAll(/(?i)\(clone of.*/, "")
                                .replaceAll(/(?i)\W*$/, "")
                                .replaceAll(/(?i)^(?:USA|Hong\s*Kong|Sweden|Switzerland|Belgium)\)/, "")
                                .replaceAll(/(?i),\s*SE$/, "")
                                .replaceAll(/(?i)^stated/, "")
                                .replaceAll(/\[s$/, "")
                                .replaceAll(/(?s)\s+/, " ")

                        }

                        def addressObj = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
                        if (addressObj) {
                            //Fixing street name when the street contains part of the city
                            def streetMatch = (addr =~ /^(.*?),/)
                            if (streetMatch) {
                                def street = streetMatch[0][1].toString()
                                if (addressObj?.city) {
                                    if ((street =~ /$addressObj.city/))
                                        addressObj.address1 = street
                                }
                            }
                            addressObj.address1 = addressObj.address1?.replaceAll(/(?i):?\s*(?:<\/b>)?\s*Based\s*in\s*(?:Frankfurt|Geneva|Manhattan)/, "")

                            if ((streetMatch = (addr =~ /London\s*Wall|London\s*Bridge/))) {
                                addressObj.address1 = addressObj.address1.toString().replaceAll(/(?i)(?<!London\s)Wall/, "London Wall")
                                addressObj.address1 = addressObj.address1.toString().replaceAll(/(?i)(?<!London\s)Bridge/, "London Bridge")
                            }

                            if (addressObj.postalCode) {
                                addressObj.postalCode = addressObj.postalCode.toString().replaceAll(/P\.?O\.?\s*Box/, "").trim()
                            }

                            if (addressObj.postalCode && addressObj.address1) {
                                if (addressObj.address1 =~ /,\s*8DJ1\s*$/) {
                                    addressObj.address1 = addressObj.address1.replaceAll(/,\s*8DJ1\s*$/, "")
                                    addressObj.postalCode = addressObj.postalCode.replaceAll(/^W1J\s*/, "W1J 8DJ1")
                                }
                            }
                            addressObj.address1 = addressObj.address1?.replaceAll(/(?i)^London$/, "")?.trim()
                            addrList.add(addressObj)

                        } else {
                            def na = new ScrapeAddress();
                            na.rawFormat = addrString.replaceAll("\\u1EA7", "")
                                .replaceAll("\\u1ED1", "")
                                .replaceAll("\\u1EA3", "")
                                .replaceAll("\\u1EC1", "")
                            if (!na =~ /(?i)n\/a/ && !na =~ /(?i)unknown/) {

                                addrList.add(na)
                            }
                            addrSet.put(addrString, 1)
                        }
                    }
            }
        }

        return addrList
    }

    def preAddrFix(addr) {
        addr = addr.replaceAll(/\bNY\b/, "NY,")
        addr = addr.replaceAll(/NY10016/, "NY, 10016")
        addr = addr.replaceAll(/NY10004/, "NY, 10004,")
        addr = addr.replaceAll(/NY10010/, "NY, 10010")
        addr = addr.replaceAll(/NY10006/, "NY, 10006")
        addr = addr.replaceAll(/NY10107/, "NY, 10107")
        addr = addr.replaceAll(/NY11747/, "NY, 11747")
        addr = addr.replaceAll(/(?is)\btel\b:.*?Address/, "")
        addr = addr.replaceAll(/(?is)<[^>]+>\s*\b(?:tel|be aware|website|ephone|e-mail).*/, "")
        addr = addr.replaceAll(/(?is)\((?:clon|fca|genuine|one|both)[^\)]+\)/, "")
        addr = addr.replaceAll(/(?is)(?<=\b(?:\d|Dometius|building|tsui|kowloon|group|floor)\b)<br\s*\/?>\n/, ",")
        addr = addr.replaceAll(/(?i)Telephone number:.*/, "")
        addr = addr.replaceAll(/(?i)(Street|Ave|Aveneue|Road)\s*\;/, '$1,')

        //change
        addr = addr.replaceAll(/(?im)(?:the genuine|Quest Financial).*$/, "")
        addr = addr.replaceAll(/(?is)<\/[^>]+>\s*<[^\/>]+>$/, "")
        addr = addr.replaceAll(/(?is)<h3>[^\n]+/, "")
        addr = addr.replaceAll(/(?i)<[^>]+>.*?\bLtd\b/, "")
        addr = addr.replaceAll(/(?i)Address[^:]+:|\bCity Of\b/, "")
        addr = addr.replaceAll(/(?i)\(\s*registered[^)]*+\)/, '')
        addr = addr.replaceAll(/(?i)(?:telephone|Website):.*/, "")
        addr = addr.replaceAll(/(?i)the\swebsite(.*?)(?=spyrou\s*kyprianou\s84)/, "")
        addr = addr.replaceAll(/\u00a0/, " ")
        addr = addrAppend(addr, "Rue Ragwee", "Luxembourg", true)

        //hardcoded fix
        addr = addr.replaceAll(/(?i)(London)\s*(1 64 Piccadilly Gardens)/, '$2 $1')

        return addr
    }

    def addrAppend(addrStr, srcToken, appendToken, isAppendLast = false) {
        if (isAppendLast) {
            return addrStr.replaceAll(/(?i)(\b$srcToken\b)((?!\b$appendToken\b).*)$/, '$1$2, ' + appendToken)
        }
        return addrStr.replaceAll(/(?i)(\b$srcToken\b)((?!\b$appendToken\b).*)$/, '$1,' + appendToken + ', $2')
    }

    def miscAddrFix(text) {
        text = text.replaceAll(/<[^>]+>/, "")
        text = text.replaceAll(/(?:,|\band\b)\s*$/, "")
        text = text.replaceAll(/&amp;/, "&")
        text = text.replaceAll(/(?i)^\s*at:/, "")
        text = text.replaceAll(/(?i)^\s*(?:Claims\s*to\s*be)?\s*based\s*in\s*(?:\s*the\s*)?/, "")
        text = text.replaceAll(/\n/, "")
        text = text.replaceAll(/^\W+/, "")
        text = text.trim()

        if (text =~ /(?is)\b[a-z][a-z]?\d[a-z\d]?(?:[\s-]*\d[^cikmov\W\d]{1,2})?\b(?=\W*$)/) {
            text = text + ", UK"
        }

        //a few hardcoded fixing
        text = text.replaceAll(/(?i)\b(london|Meisenweg|Port Elizabeth|Scotland|Tokyo|Baarerstrasse|(?-i:NW)|Washington|Grand Cayman|Harrow)\b(?=\s*\w)(?!\W+road\b)/, '$1,')
        text = text.replaceAll(/(Lake Towers.*)/, '$1, UAE')
        text = text.replaceAll(/(?i)(?<=\w)\s*\b(Triesen|Chicago|Kgs. Lyngby|Cedex)\b/, ',$1')
        text = text.replaceAll(/(?i)(\bMeisenweg\b.*)$/, '$1, Austria')
        text = text.replaceAll(/(?i)(\b(?:Regenet\s+Street|Hampshire|West Sussex|London|Dorset)\b.*)(?<!united kingdom)$/, '$1, UK')
        text = text.replaceAll(/\s*\n+\s*/, ', ')
        text = text.replaceAll(/(?i)(\bDublin\s*\d+\b.*)$/, '$1, Dublin')
        text = text.replaceAll(/(?i)(Taipei)\s*City/, '$1')
        text = text.replaceAll(/(?i)(\bNeuilly Sur Seine\b.*)$/, '$1, France')
        text = text.replaceAll(/(?i)(\bZug\b.*)$/, '$1, Switzerland')
        text = text.replaceAll(/(?i)Zug,\s*Switzerland,\s*Switzerland/, "Zug,Switzerland")
        text = text.replaceAll(/(?-i)\bZIP\b|:|^\s*\(\w+\)/, '')
        text = text.replaceAll(/(?-i)Kista, SE/, "Kista, Sweden")
        text = text.replaceAll(/(?-i)Long Island City, NY/, "Long Island City, NY, USA ")
        text = text.replaceAll(/(?-i)(\W+(?:NY|FL|PA|WA|UT)\W+\d{5})\s*$/, '$1, USA')
        text = text.replaceAll(/(?-i)^\d+\)|\bOR\b/, "")

        //hardcoded fixing
        //fix for city name as Lugano
        text = text.replaceAll(/CH6900\s*Lugano/, "Lugano CH6900")

        //final garbage striping
        text = text.replaceAll(/(?i)Head Office Address\W*/, "")

        return text.trim();
    }

    def detectEntityType(name, entityType) {
        if (entityType.equals("P")) {
            if (name =~ /(?i)\b(?:Assurance|Blackmore|Blundell|Brock Hartwick|Browne|Knight|Brothers|Affiliates|Banque|research|finance|Investments?|wealth|bank|Corporate|fund|lp|Appointments?|Financials?|Acquisitions?|Securities|Milestone|Media|Link|Limited|Associates|Ltd\.?|Langley|Addlerley|Lettner|Hall|Lucent|Caveo|Wurth|Neumann|Barclay|Berrington|Morgan|Broadgate|Credit|Fridge|Fielding|Hargreaves|Puchta|Pichler|Findlay|Kravitz|Pierce|Grisman)\b/) {
                entityType = "O";

            } else if (name =~ /(?i)\b(?:advisor[sy]|money|Distributors?|Nexus|UK|Returns|Equit(?:ies|y)|Ventures|Consolidators|law|driver|diamond|Analysis|Private|Line|delta|Universal|locator|Corporation|Asset|Department|Corp\.?|Firm|Lending|Online|Agency|Lends|pension|insure|(?i)Morgan Stanley|Dimensio|Eurozone|Banco|Accounts|Stone|Sachs|Dragon|Bentall|Brachmann|Strauss|Lloyd|Lynch)\b/) {
                entityType = "O";

            } else if (name =~ /(?i)\b(?:Burton Reed|Caixa Bancaja|positive|friendly|Compliance|euro|Society|return|Bridging|prime|Release|connect|life|Management|Trust|Services?|International|Inc\.?|Properties|Holdings|History|Union|Technologies?|Systems?|Association|Company|Carriers|Business|Incorporated|Diary|Finanz|Mergers|Suisse|Quid)\b/) {
                entityType = "O";

            } else if (name =~ /(?i)\b(?:house|insurance|invest|capitals?|americans?|Administration|Mutual|Partnership|Beatty|Intl|Generation|Source|Advice|index|cash|over|claims?|Organisation|change|Group|Facility|Trading|Poor|Forex|Standard|Machines?|Repossession|Alliances?)\b/) {
                entityType = "O";

            } else if (name =~ /(?i)\b(?:Developments?|Sou?lutions?|cars?|Initiatives?|Noble|Networks|Commodities|Fidelity|Review|Newcastle|lansdown|transfer|red|Bainbridge|leads?|digital|Resources?|Informations?|Swiss|Managers?|Fulfillment|Austria|Commerce|Plc)\b/) {
                entityType = "O";

            } else if (name =~ /(?i)(?:payday|markets?|trade|option|uIrwinnion|Global|Sunbird|Advis[eo]rs?|instant|Direct|tradition|loans?|funds?|points?|Financ|Consult|broker|Europe|place|Partners?|(?-i)Mercator\sAmalgamated|(?-i)Holy\sCross\sGreen|(?-i)Spectrum\sLand|(?-i)Sterling\sManhattan)/) {
                entityType = "O";

            } else if (name =~ /(?i)(?:Industrial|Inves(?:tor|co)|Exchange|Profit|Property|today|World|Strateg(?:ies|y)|Assistance|plans?|Recruit|insurance|Click|Institute|west|\.com|\.net|Morrison,\s+Kirkland,\s+Long)/) {
                entityType = "O";
            }

            //Now borderless tokens
            else if (name =~ /(?i)(?:\band\b|&|\d+|\-|(?-i:[A-Z]{1}\.[A-Z])|www\.)/) {
                entityType = "O";
            } else if (name =~ /(?im)^\s*\w+\s*$/) {
                entityType = "O";
            } else if (name =~ /\b(?-i:[A-Z]){2,5}\b/) {
                entityType = "O";
            }
        }

        return entityType;
    }

    def setSource(entity, sourceHtml) {
        def matches = context.regexMatches(new StringSource(sourceHtml), [regex: "<td><a (?:.*?)href=\"(.*?)\"(?:.*?)>(.*?)</a>\\s*\\((.*?)\\s*\\)</td>"]);
        if (matches) {
            if (matches[0][1] && matches[0][2]) {
                def source = entity.newSource();
                source.setName(matches[0][2].toString().trim())
                source.setUrl(matches[0][1].toString().trim())
            }

            if (matches[0][3]) {
                def address = entity.newAddress();
                address.setCountry(matches[0][3].toString().trim());
            }
        }
    }

    def looksLikeRemark(namePart) {
        def remarkStrs = ["also believed ", "falsely ", "trading ", "t/a ", "clone", "cloning ", "involved ", "no connection ",
                          "misusing ", "link with ", "links with ", "note: ", "not the ", "tel: ", "currently representing", "based in ",
                          "fca formerly ", "bogus employee", "incorporated in", "owned and ", "formerly ", "claiming to ", "the following are"]

        def isRemark = false;
        remarkStrs.each { searchText ->
            if (StringUtils.containsIgnoreCase(namePart.toString(), searchText)) {
                isRemark = true;
            }
        }

        return isRemark;
    }

    def extractRemarks(namePart) {
        def remarks = []
        namePart.split(" [/-] ").each { remark ->
            if (looksLikeRemark(remark)) {
                remarks.add(remark.toString().trim())
            }
        }
        return remarks;
    }

    def extractAliases(namePart) {
        def urlStrs = [".com", ".uk", ".info", ".org", ".net", ".gr", ".eu", ".us", ".tv", ".biz", "a/k/a", "www.", ".pl"]
        def aliases = []

        def partStr = namePart

        if (!partStr.toString().contains("Bonds &amp; ")) {
            partStr = partStr.replace(" &amp; ", " - ").replace("; ", " - ").replace(" / ", " - ")
        }
        if (isMissingRightParen(partStr.toString())) {
            partStr = new StringSource(partStr.toString() + ")")
        }

        urlStrs.each { suffix ->
            partStr.split(" - ").each { alias ->
                if (alias.toString().contains(suffix)) {
                    if (alias.toString().startsWith("at ")) {
                        alias = alias.toString().substring(2)
                    }
                    alias.replace("a/k/a ", "")
                    aliases.add(alias.toString().trim())
                }
            }
        }
        return aliases;
    }

    def isMissingRightParen(text) {
        def countRight = 0;
        def countLeft = 0;
        text.toCharArray().each { chr ->
            if (chr == ')') {
                countRight++;
            } else if (chr == '(') {
                countLeft++;
            }
        }

        return (countRight < countLeft)
    }

    def filterName(def name) {
        name = name.replaceAll(/\s*,\s*$/, "")
        if (name =~ /(?m)^\s*[^\(]+\)/) {
            name = name.replaceAll(/\)/, "")
        }
        name = name.replaceAll(/(?i):\s*clone\s*(?:firm|of*)/, "")
        name = name.replaceAll(/(?i)Three Rose Lions only use.+/, "").trim()
        name = name.replaceAll(/(?i)^www$/, "").trim()

        return name
    }

    def filterRemark(def remark) {
        remark = remark.replaceAll(/<br\s*\/?>/, "")
        remark = remark.replaceAll(/(?i)%20/, " ")
        remark = remark.replaceAll(/(?i)^cloned?\s*(?:firm)?$/, "")
        remark = remark.replaceAll(/^USA$/, "")
        remark = remark.replaceAll(/^ICC$/, "")
        remark = remark.replaceAll(/^Hang\s*Seng$/, "")
        remark = remark.replaceAll(/^BEL$/, "")
        remark = remark.replaceAll(/^(?i)(?:for other|Company|Mailing).*/, "")
        remark = remark.replaceAll(/\u00a0/, " ")

        return remark
    }

    def filterAssoc(str) {
        str = str.replaceAll(/<br\s*\/?>/, "")
        str = str.replaceAll(/<?\/?b>/, "")
        str = str.replaceAll(/<\/?p>/, "")
        str = str.replaceAll(/\u00a0/, " ")
        return str
    }

    def filterWebsite(str) {
        str = str.replaceAll(/(?s)\(.*?$/, "")
        str = str.replaceAll(/(?s)^.*?\)$/, "")
        str = str.replaceAll(/\([^\)]*\)$/, "")
        str = str.replaceAll(/(?i)E.?mail.*?$/, "")
        str = str.replaceAll(/^w$/, "")
        str = str.replaceAll(/^Please note the scammers are not using a clone site but are directing consumers to the genuine firm.?s site in the call.$/, "")
        str = str.replaceAll(/\u00a0/, " ")
        str = str.replaceAll(/\s*FSA\s*<br\s*$/, "")
        str = str.replaceAll(/<\/?b>/, "")
        return str
    }

    def filterAlias(str) {
        str = str.replaceAll(/(?i)(?:FSA)?\s*(?:FCA|EEA)\s*(?:authorised|registered)\s*firm\s*(?:details)?/, "")
        str = str.replaceAll(/(?i)^authorised firm$/, "")
        str = str.replaceAll(/(?s)\(.*?$/, "")
        str = str.replaceAll(/(?i)use\s*Direct\s*Brokers/, "Direct Brokers")
        str = str.replaceAll(/(?i)^FCA$/, "")
        str = str.replaceAll(/^uses?\s*(?:the\s*names?)?/, "")
        str = str.replaceAll(/^calling\s*itself/, "")
        str = str.replaceAll(/Andrew Walker Associates/, "A Walker Associates")
        str = str.replaceAll(/<?\/?b>/, "")
        str = str.replaceAll(/\u00a0/, " ")
        str = str.replaceAll(/\s*FSA\s*EEA\s*authorised<br\s*/, "")
        str = str.replaceAll(/\s*FSA\s*<br\s*/, "")
        str = str.replaceAll(/>/, "")
        str = str.replaceAll(/(?i)Three Rose Lions only uses.+/, "")

        return str
    }

    def sanitize(data) {
        data = data.replaceAll(/\s+/, " ").replaceAll(/&amp;/, '&').replaceAll(/\+|\[|\:/, "").trim()
        data = data.replaceAll(/^\W+|\W+$/, "").trim()
        return data
    }

    def configFileParser(def filePath) {
        def valMap = [:]
        if (filePath) {
            text = new File(filePath).getText();
            text = text =~ /(?-i)(.*?)\|(.*?)\|(.*?)\|(.*)/
            while (text.find()) {
                valMap[text.group(1).trim()] = [
                    text.group(2).trim(),
                    text.group(3).trim(),
                    text.group(4).trim()
                ]
            }
        }

        return valMap
    }

    def detailPageFix(str) {
        str = str.replaceAll(/\u00a0/, " ")
        return str
    }

//fixes for uk_authorised
    def fixData(def str) {

        if (str =~ /dgfinancialservicesltd@gmail.com/) {
            str = "D&G Financial Services Ltd."
        }

        if (str =~ /Hargreave\s*Hale/) {
            str = "Hargreave Hale Limited"
        }

        str = str.replaceAll(/&#039;/, "'")
        str = str.replaceAll(/&quot;/, "\"")

        return str
    }
}