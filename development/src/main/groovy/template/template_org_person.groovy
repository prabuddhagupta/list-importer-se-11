package template

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.importer.scrapian.util.Tasker
import com.rdc.scrape.*
import groovy.util.slurpersupport.NodeChild
import org.ccil.cowan.tagsoup.Parser
import scrapian_scripts.utils.ImageToTextConverter

import java.util.concurrent.ThreadPoolExecutor

/**
 * Date: 
 * */

//--------Some best practises (remove this block on code submission/while refactoring)-----------//
/**
 * 1. For varying formatted data block, try to convert the data blocks into a common format then apply a common regex to parse that standard block.
 * 1.1. Write "if-else" style block matching for varying data blocks while attaching sample document/comment input string for each patterns
 * 2. Use rule 1 in all types of data filtering scenarios
 * 3. All variables linked inside tasker.execute({..}) from outside must be declared final. ie.
 *    while(..){def x = "dadadada" ; tasker.execute({do(x)},2) }*
 * ++should be replaced with++
 *    while(..){final def x = "dadadada" ; tasker.execute({do(x)},2) }*
 * 4. All class fields/variables should be declared final.
 * 5. All Collections must be turned into "Collections.synchronized.." collections
 *
 *
 * more utils methods are in "Util.UsefulGroovyMethod" class
 * */

context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

Template script = new Template(context)
script.initParsing()

//-----Debug area starts------------//
//script.handleDetailsPage("...")
//-----Debug area ends---------------//

class Template {
  //TODO: remove this if Module factory is not used
  //most recent git-revision no that is related to this script; default value: "HEAD"
  //final factoryRevision = "c41068d8645600e1e387d7792377085ff9b52b02"(grab in ocrreader)  //"519ae4bb0ae45b0152e0eaefe281525b60ebaa1e" (Omar's latest commit version)
  final factoryRevision = "2e152dc346d45606ebe038f651859d38c54887c8"//"debd04acd9425a218804951f50eae60548a7a875"//
  final moduleFactory = ModuleLoader.getFactory(factoryRevision)

  //TODO: remove this if address-parser is not used
  final addressParser

  final ScrapianContext context

  //TODO: remove this if tasker class is not used
  final Tasker tasker
  final int taskRetryCount = 10


  //TODO: remove this if pdf/image to text conversion is not used
  final def ocrReader

  //TODO: remove this if not used
  //final HtmlTokenParser tokenParser = new HtmlTokenParser(context, this.&sanitize) /*It might not be needed in all script*/

  //TODO: remove this if not used
  final ImageToTextConverter imageToTextConverter = new ImageToTextConverter(context)

  final String root = "http://www.dbo.ca.gov"
  final String url = root + "/ENF/"

  //TODO: remove if not used
  final String splitTag = "~SPLIT~"
  final String aliasTag = "~ALIAS~"

  //TODO: chk the feasibility to replace it with concurrent optimised hashmap
  final List globalOrgAliasList = Collections.synchronizedList([])

  Template(context, threadsCount = 0) {
    this.context = context
    addressParser = moduleFactory.getGenericAddressParser(context)
    //--alternative load addressParser via GenericAddressParserFactory----
    //def xlsLocation = GenericAddressParserFactory.getScriptLocation(GenericAddressParserFactory.fileLocation, "standard_addresses", factoryRevision)
    //addressParser = GenericAddressParserFactory.getGenericAddressParser(context, xlsLocation)

    tasker = new Tasker(threadsCount)
    ocrReader = moduleFactory.getOcrReader(context)
  }

  private enum FIELDS
  {
    NAME_BLOCK, BUILD, CITY, DOB, EVNT_DATE, EVNT_DETAILS, EYE_COLOR, GENDER, HAIR_COLOR, HEIGHT, IMG_URL, RACE, REMARKS, SCARS, SKIN, SRC_URL, STATE, URL, WEIGHT, ZIP
  }

//------------------------------Initial part----------------------//
  def initParsing() {
    def html = invoke(url)

    //----example codes start-----
    def uMatch = html =~ /\/ENF\/List\/(?-i:[A-Z]+)[^"]+/
    while (uMatch.find()) {
      //variables linked/used inside of tasker.execute({..}) block from outside, must be declared final
      final def url = root + uMatch.group(0)
      tasker.execute({handleDetailsPage(url)}, taskRetryCount)
    }

    //close the script to release resources
    close()
  }

  def handleDetailsPage(srcUrl) {
    def html = invoke(srcUrl)
    def attrMap = [:]
    //---------example codes start------------
    attrMap[FIELDS.URL] = [srcUrl]
    attrMap[FIELDS.EVNT_DETAILS] = ["This person appears on the Northern California Most Wanted Fugitives List", event]

    //uncomment the following block if necessary

    //setting short named vars for option keys (optional/saving space)

//		def s = HtmlTokenParser.OPTIONS.UNIQUE_START_REGEX_TOKEN
//		def id = HtmlTokenParser.OPTIONS.ID
//		def ct = HtmlTokenParser.OPTIONS.CAPTURE_TYPE
//		//def nv = HtmlTokenParser.CAPTURE_TYPE.NAME_VALUE_PAIR
//		def qv = HtmlTokenParser.CAPTURE_TYPE.QUOTED_VALUE
//		def cc = HtmlTokenParser.CAPTURE_TYPE.CUSTOM_CAPTURE
//		def sc = HtmlTokenParser.OPTIONS.PRIVATE_SANITIZE_CLOSURE
//		def th = HtmlTokenParser.OPTIONS.IS_TRUNCATE_HTML
//		def bs = HtmlTokenParser.OPTIONS.IS_BR_SPLIT
//		def ts = HtmlTokenParser.OPTIONS.IS_TAG_STRIP
//		def urlSanitizer = { data ->
//			return root + data.replaceAll(/&amp;/, "&").trim()
//		}
//
//		//LinkedHashMap: we want data to be parsed from html in the order as set in the map
//		LinkedHashMap tokensMap = [
//				1 : [(id): FIELDS.NAME_BLOCK, (s): /SuspectName"/],
//				2 : [(id): FIELDS.URL, (s): /IMPoster"[^\n]+href/, (ct): qv, (sc): urlSanitizer],
//				3 : [(id): FIELDS.IMG_URL, (s): /imThumbnail1[^\n]+src/, (ct): qv, (sc): urlSanitizer],
//				4 : [(id): FIELDS.RACE, (s): /Race/],
//				5 : [(id): FIELDS.GENDER, (s): /Gender/],
//				6 : [(id): FIELDS.HEIGHT, (s): /Height/],
//				7 : [(id): FIELDS.WEIGHT, (s): /Weight/],
//				8 : [(id): FIELDS.HAIR_COLOR, (s): /Hair/],
//				9 : [(id): FIELDS.EYE_COLOR, (s): /Eyes?/],
//				10: [(id): FIELDS.DOB, (s): /SuspectAge/, (sc): this.&convertAgeToDate],
//				11: [(id): FIELDS.SCARS, (s): /Other:/, (bs): true],
//				12: [(id): FIELDS.EVNT_DETAILS, (s): /mostwantedsummary"[^>]*>(.*?)<\//, (ct): cc, (ts): true],
//
//				//few misc fill
//				13: [(id): FIELDS.IMG_URL, (s): /"nsZoomPic/, (ct): qv, (th): false],/*if we truncate html than we might not find pdf */
//				14: [(id): FIELDS.URL, (s): /<a\s+href="(files\/[^"]+?\bpdf)"/, (ct): cc, (sc): urlSanitizer]
//				//we also could use add method for adding more data
//		]

    //Now parse the data
    tokenParser.parse(html, tokensMap, attrMap)

    dataBlockHandler(attrMap[FIELDS.NAME_BLOCK], attrMap)
    //----example codes end-----

    dataBlockHandler(names, attrMap)
  }

  def dataBlockHandler(names, attrMap) {

    names = formatInitialNamesStr(names)
    names = aliasFixingWithTag(names)

    def nameList = parseNSplitUniqueNames(names)
    nameList.each {name ->
      name = sanitize(name)
      def aliasList = []

      aliasList = name.split(aliasTag).collect({it ->
        return formatIndividualName(it)
      })
      name = aliasList[0]
      aliasList.remove(0)
      createEntity(name, attrMap, aliasList)
    }
  }

//------------------------------Filter part------------------------//
  def formatInitialNamesStr(nameStr) {
    nameStr = nameStr.trim()
        .replaceAll(/(?i),(?=\s*\b(Inc|Ltd|ll[pc]|lp)\b)/, "")

    //--- example regex start
        .replaceAll(/(?i)^http:\/\//, '')
        .replaceAll(/&#233;/, 'e')
        .replaceAll(/[\(,\s\.]*$/, '')
        .replaceAll(/(?i)[\(\s]*\bthe\b[\s\)]*/, '')
        .replaceAll(/(?-is)\s*-\s*[0-9A-Z]{5,}/, ';')
        .replaceAll(/(?si)(-->|- Order).*/, '')
    //--- example regex ends

    return sanitize(nameStr)
  }

  def aliasFixingWithTag(nameStr) {
    def aliasTokens = /\b(?:(?>d[-\/\.]?b[-\/\.]?[ao]\b|[af][-\/\.]?k[-\/\.]?a\b)[-\/\.]?|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\s*\/\s*a)|(?>previously|also|former?ly)(?: known as)?\b)\s*/
    nameStr = nameStr

    //--- example regex start
    //general alias inside braces
        .replaceAll(/(?is)(.*?)\s*\(([^\(\)]*?)$aliasTokens([^\(\)]*)\)\s*(.*)/, {a, b, c, d, e ->
      return b + e + aliasTag + c + d
    })
    //org aliassss inside braces
        .replaceAll(/(?i)\b(?<=llc|inc)\.?\s*(?!\.)\(([^\)]+)\)/, {a, b ->
      return aliasTag + " " + b
    })

    //same initial words in alias active names separated via (and or ,)
        .replaceAll(/(?i)(\b(\w{2,}?(?=(?-i:[A-Z])|\b))(?:(?!aka)[^,;\n])*?)(?:\s*(?:,|\band\b)\s*)+(?=\2(?:(?-i:[A-Z])|\b))/, {a, b, c ->
      return b + aliasTag
    })

    //Misc Pos-Backward and neg-forward check with only ,
        .replaceAll(/(?i)(?<=(?>Advisors|Negotiation|Films)\b\.?(?!\.))\s*(?:,)\s*\b(?!(Limited|LLC|Inc))/, aliasTag)

    //--- example regex ends

    //pure org tag fixing
        .replaceAll(/(?i)\b(?<=llc|inc)(\.?\s*(?!\.)\s+)(?!$aliasTag)(.+)/, {a, b, c ->
      return b + aliasTag + " " + c
    })
    //remaining alias fixing
        .replaceAll(/(?i)[,;\s]*${aliasTokens}[,;\s]*/, aliasTag)


    return nameStr
  }

  def parseNSplitUniqueNames(nameStr) {
    nameStr = nameStr
    //Pos-Backward and neg-forward check with (and|,|\n) in between
        .replaceAll(/(?im)(?<=\b(LP|AG|Inc|Ltd|Limited|plc|Corp(?:oration)?|Company|Partnership|S\.A|P\.L\.C|Incorporated|LLC)\b\.?+)\s*(?:\band\b|,++|\n)\s*+(?!\b(?:Limited|LLC|Inc|[afn][\.\/]?k[\.\/]?a)\b|^\s*+\()/, splitTag)

    //---example regex---

    //Misc Pos-Backward and neg-forward check with and|,
        .replaceAll(/(?i)(?<=(?>co-op)\b\.?(?!\.))\s*(?:\band\b|,+)\s*\b(?!(Limited|LLC|Inc))/, splitTag)
    //fixing new lined and semicoloned names
        .replaceAll(/\s*(?>,?\s*\n|;)\s*/, splitTag)
    //fixing name inside braces
        .replaceAll(/(.*?)\(\s*((?:\s*\b\w+\b){2,})\s*\)(.*)/, {a, b, c, d ->
      return b + d + splitTag + c
    })
    //fix multi domain str
        .replaceAll(/\b\s*(?>and\b|,)\s*(?=[\w\.]+\.(?>com|net|biz|org)\b)/, splitTag)

    //--- example regex ends---

    //returning as unique value set
    return new HashSet<String>(Arrays.asList(nameStr.split(splitTag)))
  }

  def formatIndividualName(name) {

    //Adding extra dot after few org keywords
    name = name.replaceAll(/(?i)\b(?>corp|inc|ltd)$/, {a ->
      return a + "."
    })
    name = name
    //--- example regex start
    //remove last ending braces
        .replaceAll(/^([^\(]+)\)\s*$/, '$1')
    //--- example regex ends

        .replaceAll(/[\s,;]*$/, '')
        .replaceAll(/\s{2,}/, ' ')
    return name.trim()
  }

//------------------------------Entity creation part---------------//
  def createEntity(name, attrMap, aliasList = []) {
    if (name) {
      def entityType = detectEntityType(name)
      //Group two alias types
      TreeSet orgAliasList = []
      TreeSet personAliasList = []
      aliasList.each {alias ->
        if (alias) {
          def aliasType = detectEntityType(alias)
          if (aliasType == "O") {
            orgAliasList.add(alias)
          } else {
            personAliasList.add(alias)
          }
        }
      }

      def entity
      if (entityType == "O") {
        entity = createOrgEntity(name, orgAliasList)
        handleDifferentAliasAssociation(entity, personAliasList, attrMap)

      } else {
        entity = createPersonEntity(name, personAliasList)
        handleDifferentAliasAssociation(entity, orgAliasList, attrMap)
      }
      createEntityCommonCore(entity, attrMap)
    }
  }

  def handleDifferentAliasAssociation(parentEntity, TreeSet diffTypeAliasList, attrMap, isAllDiffAliasesAreAliasToEachOther = true) {
    if (diffTypeAliasList.size() > 0) {
      //Add all aliases as an association
      diffTypeAliasList.each {alias ->
        parentEntity.addAssociation(alias)
      }

      def diffEntityCreator = parentEntity.type == "O" ? this.&createPersonEntity : this.&createOrgEntity
      def aEntity
      //Following might not be the case for all scenarios; change the "true/false" if necessary
      if (isAllDiffAliasesAreAliasToEachOther) {
        def name = diffTypeAliasList[0]
        diffTypeAliasList.remove(name)
        aEntity = diffEntityCreator(name, diffTypeAliasList)
        createEntityCommonCore(aEntity, attrMap)
        aEntity.addAssociation(parentEntity.name)
      } else {
        diffTypeAliasList.each {newName ->
          aEntity = diffEntityCreator(newName)
          createEntityCommonCore(aEntity, attrMap)
          aEntity.addAssociation(parentEntity.name)
        }
      }
    }
  }

  def createOrgEntity(name, aliasList = []) {

    def entity = null
    entity = detectDupOrgEntity(name, aliasList)
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

  def createPersonEntity(name, aliasList = []) {
    def entity = context.getSession().newEntity()
    name = personNameReformat(sanitize(name))
    name = camelCaseConverter(name)
    entity.name = name
    entity.type = "P"
    aliasList.each {alias ->
      entity.addAlias(personNameReformat(alias))
    }

    return entity
  }

  def personNameReformat(name) {

    //Regroup person name by comma
    /**
     abc, sdf, jr --match
     abc, sdf jr -- not match
     C. Conway Felton, III -- not match
     O'Dowd, Jr., Charles T. -- match
     * */
    def exToken = "(?:[js]r|I{2,3})"
    return name.replaceAll(/(?i)\s*(.*?)\s*,\s*\b((?:(?!$exToken\b)[^,])+)s*(?:,\s*\b($exToken)\b)?\s*$/, '$2 $1 $3').trim()
  }

  def camelCaseConverter(name) {
    //only for person type //\w{2,}: II,III,IV etc ignored
    name = name.replaceAll(/\b(?:((?i:x{0,3}(?:i+|i[vx]|[vx]i{0,3})))|(\w)(\w+))\b/, {a, b, c, d ->
      return b ? b.toUpperCase() : c.toUpperCase() + d.toLowerCase()
    })

    return name
  }

  def detectDupOrgEntity(name, aliasList) {

    def entity
    def dupOrg = getDupOrgEntityNameAlias(name, aliasList, globalOrgAliasList)
    if (dupOrg.size() > 0) {
      entity = context.findEntity(["name": dupOrg[0], "type": "O"])
      aliasList = dupOrg[1]
      if (dupOrg[2]) {
        entity.addAlias(dupOrg[0])
        entity.name = name
      }
    }

    return entity
  }

  def getDupOrgEntityNameAlias(name, aliasList, orgEntityAliasGlobalList) {
    //return: [duplicate entity name, [aliases to add], is name-alias swap active(boolean)]

    aliasList.add(name)
    def tmpAlias = aliasList.clone()
    def orgListIndex = 0
    LinkedHashSet orgListSingleArr = []
    def dupFlag = false
    def orgListSize = orgEntityAliasGlobalList.size()
    def dupName = ""
    def swapFlag = false
    for (alias in aliasList) {
      if (!dupFlag) {
        for (int i = 0; i < orgListSize; i++) {
          orgListSingleArr = orgEntityAliasGlobalList[i]
          def found = false
          for (oVal in orgListSingleArr) {
            if (oVal.equalsIgnoreCase(alias)) {
              found = true
              tmpAlias.remove(alias)
              dupFlag = true
              orgListIndex = i
              break
            }
          }
          if (found) {
            break
          }

        }
      } else {
        for (oVal in orgListSingleArr) {
          if (oVal.equalsIgnoreCase(alias)) {
            tmpAlias.remove(alias)
          }
        }
      }
    }
    aliasList.remove(name)
    if (dupFlag) {
      dupName = orgListSingleArr[0]
      orgListSingleArr = orgListSingleArr + tmpAlias
      if (!dupName.equalsIgnoreCase(name)) {
        if (orgListSingleArr[0].compareTo(name) < 1) {
          orgListSingleArr.add(name)
        } else {
          orgListSingleArr = [name] + orgListSingleArr
          swapFlag = true
        }
      }
      orgEntityAliasGlobalList[orgListIndex] = orgListSingleArr
      return [dupName, tmpAlias, swapFlag]
    } else {
      orgEntityAliasGlobalList.add([name] + aliasList)
    }
    return []
  }

  def detectDupPersonEntity(name) {
  }

  def createEntityCommonCore(ScrapeEntity entity, attrMap) {

    //----example codes start-----
    attrMap[FIELDS.EVNT_DETAILS].each {des ->
      des = sanitize(des)
      def codes = detectEventCode(des, /(?i)^This person appears/, "FUG", "WTD")
      codes.each {code ->
        def event = new ScrapeEvent()
        event.setDescription(des)
        event.category = code[0]
        event.subcategory = code[1]
        def date = context.parseDate(new StringSource(attrMap[FIELDS.EVNT_DATE]))
        if (date) {
          event.setDate(date)
        }
        entity.addEvent(event)
      }
    }

    //-----------way 1----------------
    def address = new ScrapeAddress()
    address.city = attrMap[FIELDS.CITY]
    address.province = attrMap[FIELDS.STATE]
    address.postalCode = attrMap[FIELDS.ZIP]
    address.country = "United States"
    entity.addAddress(address)
    //-----------way 2---------------------
    def addrMap = addressParser.parseAddress([text: addrStr, /*force_country: true*/])
    def street_sanitizer = {street ->
      return street.replaceAll(/(?s)\s+/, " ")
          .replaceAll(/^[\s,-]+|\W+$/, "")
    }
    ScrapeAddress addressObj = addressParser.buildAddress(addrMap, [street_sanitizer: street_sanitizer])
    if (addressObj) {
      addressObj.setBirthPlace(true)

      if (addressObj.address1) {
        //fix it if needed else remove this
      }
      if (addressObj.city) {
        //fix it if needed else remove this
      }
      if (addressObj.province) {
        //fix it if needed else remove this
      }
      if (addressObj.postalCode) {
        //fix it if needed else remove this
      }
      entity.addAddress(addressObj)
    } else {
      //Address is not parse-able; either add then as raw addr or reformat the input addr string
      println(addrStr)
    }

    attrMap[FIELDS.URL].each {
      entity.addUrl(urlSanitize(it))
    }

    attrMap[FIELDS.IMG_URL].each {
      entity.addImageUrl(urlSanitize(it))
    }
    attrMap[FIELDS.DOB].each {
      entity.addDateOfBirth(new ScrapeDob(it))
    }

    attrMap[FIELDS.GENDER].each {
      entity.addSex(it)
    }

    attrMap[FIELDS.HEIGHT].each {
      entity.addHeight(it)
    }

    attrMap[FIELDS.WEIGHT].each {
      entity.addWeight(it)
    }

    attrMap[FIELDS.EYE_COLOR].each {
      entity.addEyeColor(it)
    }

    attrMap[FIELDS.HAIR_COLOR].each {
      entity.addHairColor(it)
    }

    attrMap[FIELDS.RACE].each {
      entity.addRace(it)
    }

    attrMap[FIELDS.BUILD].each {
      entity.addBuild(it)
    }

    attrMap[FIELDS.SKIN].each {
      entity.addComplexion(it)
    }

    attrMap[FIELDS.SCARS].each {
      entity.addScarsMarks(it)
    }

    def nSrc = new ScrapeSource()
    nSrc.setUrl(attrMap[FIELDS.SRC_URL])
    nSrc.setName("California Department of Corporations")
    nSrc.setDescription("Enforcement actions issued by the California Department of Corporations.")
    entity.addSource(nSrc)
    //----example codes end-----
  }

  def detectEventCode(data, defaultRegex, category, subCategory) {
    def catCodes = []

    if (data =~ defaultRegex) {
      catCodes.add([category, subCategory])
    }

    if (data =~ /(?i)\b(?:Murder|Manslaughter)\b/) {
      catCodes.add(["MUR", subCategory])
    }

    if (data =~ /(?i)\b(?:Fraud)\b/) {
      catCodes.add(["FRD", subCategory])
    }

    if (data =~ /(?i)\b(?:Sex(?:ual)?|Molest(?:ing|ation))\b/) {
      catCodes.add(["SEX", subCategory])
    }

    if (data =~ /(?i)\b(?:Kidnap(?:ping)?)\b/) {
      catCodes.add(["KID", subCategory])
    }

    if (data =~ /(?i)\b(?:Assault|Battery)\b/) {
      catCodes.add(["AST", subCategory])
    }

    if (data =~ /(?i)\b(?:Posession|Distribut(?:e|ion))\b/) {
      catCodes.add(["DTF", subCategory])
    }

    if (data =~ /(?i)\b(?:Firearm|Handgun)\b/) {
      catCodes.add(["IGN", subCategory])
    }

    if (data =~ /(?i)\b(?:Gang)\b/) {
      catCodes.add(["ORG", subCategory])
    }

    if (data =~ /(?i)\b(?:Counterfeit(?:ing)?)\b/) {
      catCodes.add(["CTF", subCategory])
    }

    if (data =~ /(?i)\b(?:Copyright)\b/) {
      catCodes.add(["CPR", subCategory])
    }

    if (data =~ /(?i)\b(?:Robbery)\b/) {
      catCodes.add(["ROB", subCategory])
    }

    if (data =~ /(?i)\b(?:Burglary)\b/) {
      catCodes.add(["BUR", subCategory])
    }

    if (data =~ /(?is)\b(?:Money\s+Laundering)\b/) {
      catCodes.add(["MLA", subCategory])
    }

    return catCodes
  }

//------------------------------Misc utils part---------------------//
  def invoke(url, isPost = false, isBinary = false, cache = true, postParams = [:], headersMap = [:], cleanSpaceChar = false, tidy = false, miscData = [:]) {
    Map data = [url: url, tidy: tidy, headers: headersMap, cache: cache, clean: cleanSpaceChar]
    if (isPost) {
      data.type = "POST"
      data.params = postParams
    }
    data.putAll(miscData)

    try {
      if (isBinary) {
        return context.invokeBinary(data)
      } else {
        return context.invoke(data)
      }

    } catch (InterruptedException ignored) {
      System.err.println("Interrupted exception for: " + url)

    } catch (e) {
      def executor = tasker.getExecutorService()
      if (executor instanceof ThreadPoolExecutor) {
        ((ThreadPoolExecutor) executor).setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
      }
      executor.shutdownNow()
      System.err.println("Error invoking: " + url)
      throw new Exception(e)
    }
  }

  // check UsefulGroovyMethods.groovy class for old pdfToTextConverter; Using this new one is highly recommended
  def pdfToTextConverter(pdfUrl) {
    //Way 1.-> no options: (if local parsing fails it uses online ocr) use smart decision
    List<String> pages = ocrReader.getText(pdfUrl)

    //Way 2. -> use local parser hence faster but with cache on/off; use cache off if you need to get updated data
    //List<String> pages = ocrReader.getText(pdfUrl, [force_ocr:false, cache:false])

    //Way 3. -> better formatted data but slower due to online ocr latency
    //List<String> pages = ocrReader.getText(pdfUrl, [force_ocr:true, cache:false])

    //now iterate the pdf/image pages to extract the desired texts
    pages.each {text ->
      //do it
    }
  }

  def processXlsBinaryData(xlsBinarySource) {
    def xml = context.transformSpreadSheet(xlsBinarySource, [validate: true, escape: true])
    def rows = new XmlSlurper(new Parser()).parseText(xml.toString())

    //----sample implementation------
    Map countries = [:]
    rows.row.each {NodeChild row ->
      def code = row.code.toString().trim()
      if (code) {
        def full_name = row.country_name.toString().trim()
        def regex = row.regex.toString().trim()
        def words_count = (row.search_words_count?.toString().trim()) ?: 0
        countries[code] = [full_name, regex, words_count]
      }
    }

    return countries
  }

  def detectEntityType(name) {
    def entityType = context.determineEntityType(name)
    if (entityType.equals("P")) {
      //Alphabetically sorted Misc keywords lists

      /*----example codes start-----

      if (name =~ /(?i)\b(?:AFLFX|AGD|ASAP|Advance|American|Ameridebt|Asgaard|Awaken|Amerivalue|Associated|Asscociates|Ameriloan|Acuity|AOERO|Apex)\b/) {
        entityType = "O"
      }

      //Now borderless tokens
      else if (name =~ /(?i)(?:TC[AM]|now|go|Cards?|deposit|equity|calMed|LPU|ll[oc]|kids?|better|game|check)/) {
        entityType = "O"
      }

      ----example codes end-----*/
    }

    return entityType
  }

  def urlSanitize(input) {
    StringBuilder resultStr = new StringBuilder()
    for (char ch : input.toString().toCharArray()) {
      if (ch > 127 || ch < 0) {
        resultStr.append('%')
        def tempResult = String.format("%04x", new BigInteger(1, ch.toString().getBytes('UTF-8')))
//may need to change based on website encoding
        resultStr.append(tempResult.substring(0, 2) + '%' + tempResult.substring(2, 4))
      } else if (ch > 123) {
        resultStr.append('%')
        def tempResult = String.format("%02x", new BigInteger(1, ch.toString().getBytes('UTF-8')))
        resultStr.append(tempResult)
      } else {
        resultStr.append(ch)
      }
    }
    return resultStr.toString()
  }

  def sanitize(data) {
    return data.replaceAll(/&amp;/, '&').replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
  }

  def close() {
    //wait for all submitted tasks to finish and re-run all failed tasks
    tasker.runFailedTasks()
    tasker.shutdown()
  }

  def log(
      final fileName,
      final data, final isOnlyUniqueData = true, final isAppendNewLine = true) {
    //this method and its callers could be removed in production

    tasker.execute({
      try {
        context.logToFile(fileName, data, isOnlyUniqueData, isAppendNewLine)
      } catch (Exception e) {
        println(e.getMessage())
      }
    })
  }
}