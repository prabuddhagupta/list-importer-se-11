package current_script

import com.rdc.importer.scrapian.model.StringSource
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils
import scrapian_scripts.utils.GenericAddressParserFactory


context.setup([socketTimeout: 50000, connectionTimeout: 10000, retryCount: 5, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/60.0.3112.78 Chrome/60.0.3112.78 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

addressParser = GenericAddressParserFactory.getGenericAddressParser(context)
//addressParser.reloadData()
tokens = addressParser.getTokens()

initParsing()

class Uk_fca {
  static root = "https://www.fca.org.uk"
  static url = root + "/publications/search-results?p_search_term=&np_category=notices and decisions-"

  static splitTag = "~SPLIT~"
  static aliasTag = "~ALIAS~"
}

enum FIELDS_UK_FCA
{
  EVNT_DATE, EVNT_DETAILS, URL, STREET, CITY, STATE, ZIP, COUNTRY
}

//------------------------------Initial part----------------------//

def initParsing() {
  def searchCatagoriesList = ["Decision Notices", "Final Notices", "Supervisory Notices", "Cancellation Notices"]
  searchCatagoriesList.each {it ->
    searchType = it.replaceAll(/\s+/, "+")
    url = "https://www.fca.org.uk/publications/search-results?p_search_term=&np_category=notices+and+decisions-" + searchType + "&start="
    handleDetailsPage(url, it)
  }
  url = "https://www.fca.org.uk/publications/search-results?p_search_term=FSA+Application+Refusal&start="
  handleDetailsPage(url, 'Application Refusal')

  url = "https://www.fca.org.uk/publications/search-results?p_search_term=FSA+Approved+Person+Refusals&start="
  handleDetailsPage(url, 'Approved Person Refusals')
}

def handleDetailsPage(def pageUrl, def searchCatagory) {
  def startPage = 1

  loopVar = true
  loopVar2 = true
  while (loopVar || loopVar2) {
    def nextPageUrl = pageUrl + startPage
    html = context.invoke([url: nextPageUrl, tidy: true])
    tempBool = detailsPageParsing(html, searchCatagory)
    if (tempBool) {
      loopVar = true
    } else {
      if (loopVar) {
        loopVar = false
      } else {
        loopVar2 = false
      }
    }
    startPage += 10
  }
}

/**
 *  In most cases, names can be extracted from this pattern: "${Category-name}: ${Enitity-name}" (categories refer to those in searchCatagoriesList)
 *  There are entries that do not follow this pattern, so extractNames handles all cases.
 */
def extractNamesFromColon(names) {
  int split = names.indexOf(':')

  // if nothing's found, -1 + 1 resolves to zero
  return names.substring(split + 1).trim()
}

def detailsPageParsing(html, searchCatagory) {

  endMatch = html =~ /0 search results/
  if (endMatch.find()) {
    return false
  }

  def rowMatch = html =~ /(?is)<h4 class="search-item__title">(.*?)<\/li>/
  rowCount = 0
  while (rowMatch.find()) {
    rowCount++
    def names, url
    def attrMap = [:]
    def urlArr = []
    def row = rowMatch.group(1)
    //def nameUrlMatch = row =~ /(?i)<a class="search-item__clickthrough" href="([^"]+)"[^>]*?>[^:]+:\s*([^<]+)/
    row = row.replaceAll("<strong>", "").replaceAll("</strong>", "") // get rid of <strong> tags to allow regex to work
    def nameUrlMatch = row =~ /(?i)<a class="search-item__clickthrough" href="([^"]+)"[^>]*?>([^<]+)/

    if (nameUrlMatch) {
      names = nameUrlMatch[0][2]
      url = nameUrlMatch[0][1]
      names = extractNamesFromColon(names)
      if (names =~ /(?i)American Pickup Trucks Ltd/) {
        println(names)
      }
    }

    urlArr.add(url)

    def eventDateMatch = row =~ /(?i)Published:\s*([^<]+)/
    if (eventDateMatch) {
      def date = eventDateMatch[0][1]
      attrMap[FIELDS_UK_FCA.EVNT_DATE] = date
    }

    attrMap[FIELDS_UK_FCA.URL] = urlArr

    def eventDes = "This entity appears on the UK Financial Conduct Authority's list of " + searchCatagory + "."
    eventDes = eventDes.replaceAll(/(?i)\bnotice\b/, "Notices")

    def eventSummaryMatch = row =~ /(?is)<div class="search-item__body">([^<]+)</
    if (eventSummaryMatch) {
      eventDes = eventDes + " " + eventSummaryMatch[0][1]
      eventDes = eventDes.replaceAll(/\bwe\b/, "the UK FCA").replaceAll(/\bWe\b/, "The UK FCA").replaceAll(/\bOur\b/, "The UK FCA").replaceAll(/\bour\b/, "the UK FCA")
      eventDes = StringUtils.removeEnd(eventDes, ":").trim()
    }

    attrMap[FIELDS_UK_FCA.EVNT_DETAILS] = eventDes

    names = aliasFixingWithTag(names)
    def nameList = parseNSplitUniqueNames(names)

    getAddress(attrMap)

    nameList.each {name ->
      def aliasList = []
      aliasList = name.split(Uk_fca.aliasTag).collect({it ->
        return formatIndividualName(it)
      })
      name = aliasList[0]
      aliasList.remove(0)

      createEntity(name, attrMap, aliasList)
    }
  }
  return rowCount != 0
}

def formatIndividualName(def name) {
  def monthToken = /(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)?/
  def aliasTokens = /\b(?:(?>d[-\/\.]?b[-\/\.]?[ao]\b|[af][-\/\.]?k[-\/\.]?a\b)[-\/\.]?|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\s*\/\s*a)|(?>previously|also|former?ly)(?: known as)?\b)\s*/
  name = name.replaceAll(/(?i)^\s*\b(?:m[sr]s?|dr)\b\.?/, "")
      .replaceAll(/(?i)\((?!$aliasTokens)[^\)]+\)\s*$/, "")
      .replaceAll("\\u00f6", "o")
      .replaceAll("\\u00e9", "e")
      .replaceAll("\\u2018", "")
      .replaceAll("\\u2019", "")
      .replaceAll(/(?is)\s+-\s+.*$/, "")
      .replaceAll(/(?i)\s*(?:${monthToken})?\s*\d+[^\s]+\s+(?:${monthToken})?\s*(?:\d+)?\s*$/, "")
      .replaceAll(/(?i)RigneyTopps/, "Rigney Topps")
      .replaceAll(/(?i)Final Notice.*/, "")
      .replaceAll(/(?i)variation/, "")
      .replaceAll(/(?i)^\s*\bA\b\s*$/, "")
      .replaceAll(/(?i),\s*UK branch\s*$/, "")
      .replaceAll("\\ufffd", "'")
      .replaceAll(/,\s*$/, "")
      .replaceAll(/(?i).*Branch\s*\/|.*Agents\s*,/, "")
      .replaceAll(/-/, "")

  return sanitize(name)
}

def parseNSplitUniqueNames(def nameStr) {
  nameStr = nameStr
      .replaceAll(/(?i)(?<=\b(?:Provincial|Chhabra)\b)\s+\band\b\s+(?=\b(?:Mr|Sameer)\b)/, Uk_fca.splitTag)
      .replaceAll(/(?i)(?<=plc)\s*(?:,|\band\b)\s+(?!uk)/, Uk_fca.splitTag)
      .replaceAll(/(?i)\band\b\s*(?=m[rs]s?)/, Uk_fca.splitTag)
      .replaceAll(/(?i)(?<=\b(?:limited|ltd))\b\s*(?:\/|\band\b)\s*/, Uk_fca.splitTag)

  //Hardcoded split
      .replaceAll(/(?i)(?<=Arulananthan)\s*and\s*(?=Victors)/, Uk_fca.splitTag)
      .replaceAll(/(?i)(?<=Flanagan)\s*,\s*(?=GMF)/, Uk_fca.splitTag)

  //returning as unique value set
  return new HashSet<String>(Arrays.asList(nameStr.split(Uk_fca.splitTag)))
}

//------------------------------Entity creation part---------------//
def createEntity(def name, def attrMap, def aliasList = []) {
  if (name) {
    def entityType = detectEntityType(name)

    //Group two alias types
    def orgAliasList = []
    def personAliasList = []
    aliasList.each {alias ->
      if (alias) {
        def aliasType = detectEntityType(alias)
        if (aliasType.equals("O")) {
          orgAliasList.add(alias)
        } else {
          personAliasList.add(alias)
        }
      }
    }

    def entity, aEntity
    if (entityType.equals("O")) {
      entity = createOrgEntity(name, orgAliasList)
      //different alias type
      personAliasList.each {alias ->
        aEntity = createPersonEntity(alias)
        createEntityCommonCore(aEntity, attrMap)
      }
    } else {
      entity = createPersonEntity(name, personAliasList)
      //different alias type
      def olSize = orgAliasList.size()
      if (olSize > 0) {
        //Add all alias association
        orgAliasList.each {alias ->
          entity.addAssociation(alias)
          aEntity = createOrgEntity(alias)
          createEntityCommonCore(aEntity, attrMap)
        }
      }
    }
    createEntityCommonCore(entity, attrMap)
  }
}

def createOrgEntity(def name, def aliasList = []) {
  def entity = null
  entity = context.findEntity(["name": name, "type": "O"])
  if (!entity) {
    entity = context.getSession().newEntity()
    entity.setName(sanitize(name))
    entity.type = "O"
  }
  aliasList.each {alias ->
    entity.addAlias(alias)
  }

  return entity
}

def createPersonEntity(def name, def aliasList = []) {
  def entity = null
  name = personNameReformat(sanitize(name))
  name = camelCaseConverter(name)

  if (!(name =~ /(?i)Ashok Kumar Sharma/)) {

    entity = context.findEntity(["name": name, "type": "P"])
  }

  if (!entity) {
    entity = context.getSession().newEntity()
    entity.name = name
    entity.type = "P"
  }

  aliasList.each {alias ->
    entity.addAlias(personNameReformat(alias))
  }

  return entity
}

def personNameReformat(name) {
  def exToken = "(?:[js]r|I{2,3})"
  return name.replaceAll(/(?i)\s*(.*?)\s*,\s*\b((?:(?!$exToken\b)[^,])+)s*(?:,\s*\b($exToken)\b)?\s*$/, '$2 $1 $3').trim()
}

def camelCaseConverter(def name) {
  //only for person type //\w{2,}: II,III,IV etc ignored
  name = name.replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, {a, b, c, d ->
    return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
  })

  return name
}

def createEntityCommonCore(entity, attrMap) {
  def event = new ScrapeEvent()
  def date = context.parseDate(new StringSource(attrMap[FIELDS_UK_FCA.EVNT_DATE]), "dd/MM/yyyy")
  if (date) {
    event.setDate(date)
  }

  event.setDescription(sanitize(attrMap[FIELDS_UK_FCA.EVNT_DETAILS]))
  entity.addEvent(event)

  attrMap[FIELDS_UK_FCA.URL].each {url ->
    entity.addUrl(url)
  }

  def addr = new ScrapeAddress()
  attrMap[FIELDS_UK_FCA.STREET] = attrMap[FIELDS_UK_FCA.STREET].toString().replaceAll(/(?s)\s+/, " ").trim()
  attrMap[FIELDS_UK_FCA.STREET] = StringUtils.removeEnd(attrMap[FIELDS_UK_FCA.STREET].toString(), ",")
  attrMap[FIELDS_UK_FCA.CITY] = attrMap[FIELDS_UK_FCA.CITY].toString().replaceAll(/(?s)\s+/, " ").trim()

  if (attrMap[FIELDS_UK_FCA.COUNTRY]) {
    if (attrMap[FIELDS_UK_FCA.STREET].toString() != "null") {
      addr.setAddress1(attrMap[FIELDS_UK_FCA.STREET])
    }
    if (attrMap[FIELDS_UK_FCA.CITY].toString() != "null") {
      addr.setCity(attrMap[FIELDS_UK_FCA.CITY])
    }
    if (attrMap[FIELDS_UK_FCA.ZIP].toString() != "null") {
      addr.setPostalCode(attrMap[FIELDS_UK_FCA.ZIP])
    }
    if (attrMap[FIELDS_UK_FCA.COUNTRY].toString() != "null") {
      addr.setCountry(attrMap[FIELDS_UK_FCA.COUNTRY])
    }
    entity.addAddress(addr)
  }
}

def aliasFixingWithTag(def nameStr) {
  def aliasTokens = /\b(?:(?>d[-\/\.]?b[-\/\.]?[ao]\b|[af][-\/\.]?k[-\/\.]?a\b)[-\/\.]?|t\/a|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\s*\/\s*a)|(?>previously|also|former?ly)\s*(?: known as|carrying on business as)?\b)\s*/
  nameStr = nameStr

  //general alias inside braces
      .replaceAll(/(?is)(.*?)\s*\(([^\(\)]*?)$aliasTokens([^\(\)]*)\)\s*(.*)/, {a, b, c, d, e ->
    return b + e + Uk_fca.aliasTag + c + d
  })

  //same initial words in alias active names separated via (and or ,)
      .replaceAll(/(?i)(\b(\w{2,}?(?=(?-i:[A-Z])|\b))(?:(?!aka)[^,;\n])*?)(?:\s*(?:,|\band\b)\s*)+(?=\2(?:(?-i:[A-Z])|\b))/, {a, b, c ->
    return b + Uk_fca.aliasTag
  })

  //pure org tag fixing
      .replaceAll(/(?i)\b(?<=llc|inc)(\.?\s*(?!\.)\s+)(?!$Uk_fca.aliasTag)(.+)/, {a, b, c ->
    return b + Uk_fca.aliasTag + " " + c
  })
  //remaining alias fixing
      .replaceAll(/(?i)[,;\s]*${aliasTokens}[,;\s]*/, Uk_fca.aliasTag)

  return nameStr
}

def getAddress(attrMap) {
  def pdfFile
  def pdfText
  def address
  def addrMatch
  def parsedAddress
  def pmap = ["1": "-l",
              "2": "1",
              "3": "-layout"] as Map
  gbPostalCode = /(?is)\b[a-z][a-z]?\d[a-z\d]?(?:[\s-]+\d[^cikmov\W\d]{2})?\b(?=\W*$)/
  // skip these bad addresses
  badAddresses = "16 The Courtyard, Buntsford Drive, B60 3DJ|" +
      "Suite 10, Argyle House, 1st Floor, 29-31 Euston Road, NW1 2SD|" +
      "SolicitorsSolicitors|BrightonFairway|GlasgowGlasgow|" +
      "formerly of PO BOX 4012 Byron House 10" +
      "Trench Side Cottage, Ravensworth Park Estate, NE11 0HQ"

  try {
    pdfFile = context.invokeBinary([url: attrMap[FIELDS_UK_FCA.URL][0]])
    pdfText = context.transformPdfToText(pdfFile, pmap)

    pdfText = pdfText.toString().replaceAll(/\r/, "")

    // capture address from pdf
//    addrMatch = pdfText =~ /(?is)(?:Address|Of): +(.*?)(?=\n{2,}|\n\w*?:)/
    addrMatch = pdfText =~ /(?ims)^(?:Address|Of)\s*:\s*(.*?)(?=^(?:\w+[^\n]+:))/

    if (addrMatch.find()) {
      // cleanup address
      address = addrMatch.group(1).replaceAll(/ {2,}/, "")
          .replaceAll(/ *\n/, ", ")
          .replaceAll(/\s+,/, ",")
          .replaceAll(/(?<=\w)\s(?=/ + gbPostalCode + ")", ",")
          .replaceAll(/\ufffd/, "-")
          .replaceAll(/(?s)\s+/, " ").trim()
      address = StringUtils.removeEnd(address, ",")
      address = address.toString().replaceAll(/(?i)(?:11 april 2002|_+|,{2,}|final notice,)/,"")
      // special cases
      if (address.equals("Kuhlmann House, Lancaster Way, Fradley Park, Lichfield, Staffordshire, WS18 8SX.")) {
        address = "Kuhlmann House Lancaster Way Fradley Park, Lichfield, Staffordshire, WS18 8SX"
      }
      if (address.equals("The Garden Suite, Pine Grange, Bath Road, Bournemouth., BH1 2PF")) {
        address = "The Garden Suite, Pine Grange, Bath Road, Bournemouth, BH1 2PF"
      }
      if (address.equals("12 The Broadway, Wembley, HA9 8JU, FSA, Reference")) {
        address = "12 The Broadway, Wembley, HA9 8JU"
      }
      if (address.find(badAddresses)) {
        return
      }


      parsedAddress = addressParser.parseAddress([text: address, force_country: true])

      if (parsedAddress[tokens.COUNTRY]) {
        attrMap[FIELDS_UK_FCA.STREET] = parsedAddress[tokens.ADDR_STR]
        attrMap[FIELDS_UK_FCA.CITY] = parsedAddress[tokens.CITY]
        attrMap[FIELDS_UK_FCA.STATE] = parsedAddress[tokens.STATE]
        attrMap[FIELDS_UK_FCA.ZIP] = parsedAddress[tokens.ZIP]
        attrMap[FIELDS_UK_FCA.COUNTRY] = parsedAddress[tokens.COUNTRY]

        // cleanup parsed address
        if (attrMap[FIELDS_UK_FCA.STREET]) {
          trailingComma = attrMap[FIELDS_UK_FCA.STREET].lastIndexOf(',')
          if (trailingComma != -1) {
            attrMap[FIELDS_UK_FCA.STREET] = attrMap[FIELDS_UK_FCA.STREET].substring(0, trailingComma)
          }
        }
      } else {
        attrMap[FIELDS_UK_FCA.COUNTRY] = "UNITED KINGDOM"
      }
    }
  } catch (Exception e) {
    context.error("ERROR: [" + attrMap[FIELDS_UK_FCA.URL][0] + "] " + e.message)
  }
}

//------------------------------Misc utils part---------------------//
def detectEntityType(name) {
  def entityType = context.determineEntityType(name)
  if (entityType.equals("P")) {

    if (name =~ /(?i)\b(?:Agents|Amerivalue|Associated|Asscociates|Ameriloan|Acuity|AOERO|Apex|AG|Society)\b/) {
      entityType = "O"
    } else if (name =~ /@/) {
      entityType = "O"
    } else if (name =~ /(?i)\b(?:Castle|City|Chartered|Heaps|Insurance|Investments?|Islands?|Motors?|Opm|Philip|Purchase|Resolution|Sales?|State|Variation|village)\b/) {
      return "O"
    } else if (name =~ /(?i)\b(?:Delta|field|mcnulty|park|finance|exchange|Protection|Parkers?)\b/) {
      return "O"
    } else if (name =~ /(?i)\b(?:Solicitors?|Insurepay|Garage|Consultancy|Protect|Assurance|Partnership|Greenacre|Pool|PFC|cars?|Goldfinch)\b/) {
      return "O"
    }

    //Now borderless tokens
    else if (name =~ /(?i)(?:bank|money|fields?)\b/) {
      entityType = "O"
    }
  }

  return entityType
}

def sanitize(data) {
  return data.replaceAll(/&amp;/, '&').trim()
}