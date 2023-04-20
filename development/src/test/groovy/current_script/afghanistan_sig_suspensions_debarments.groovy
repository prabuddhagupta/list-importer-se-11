package current_script

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.lang.StringUtils
import scrapian_scripts.utils.ImageToTextConverter

import java.util.concurrent.ThreadPoolExecutor

/**
 * Date: 29/08/2018
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

AfghanistanSigSuspensions script = new AfghanistanSigSuspensions(context)
script.initParsing()

//-----Debug area starts------------//
//script.handleDetailsPage("...")
//-----Debug area ends---------------//

class AfghanistanSigSuspensions {
    //most recent git-revision no that is related to this script; default value: "HEAD"
    final def moduleFactory = ModuleLoader.getFactory("4620a677deca8ef6baf93aa48269de0caa5e6be3")
    final ScrapianContext context

    //TODO: remove this if address-parser is not used
    final addressParser

    //TODO: remove this if not used
    final ImageToTextConverter imageToTextConverter = new ImageToTextConverter(context)

    final String url = "https://www.sigar.mil/investigations/suspensioncases/index.aspx?SSR=3&SubSSR=19&WP=Suspension%20and%20Debarment%20Cases"
    final String address = "Afghanistan"
    final String description = "This entity appears on the Afghanistan Special Inspector General list of Suspensions, Debarments, and Special Entity Designations."

    //TODO: chk the feasibility to replace it with concurrent optimised hashmap
    final List globalOrgAliasList = Collections.synchronizedList([])

    AfghanistanSigSuspensions(context) {
        this.context = context
        addressParser = moduleFactory.getGenericAddressParser(context)
    }

    private enum FIELDS
    {
        NAME_BLOCK, BUILD, CITY, DOB, EVNT_DATE, EVNT_DETAILS, EYE_COLOR, GENDER, HAIR_COLOR, HEIGHT, IMG_URL, RACE, REMARKS, SCARS, SKIN, SRC_URL, STATE, URL, WEIGHT, ZIP
    }

//------------------------------Initial part----------------------//
    def initParsing() {
        def html = invoke(url)
        handleDetailsPage(html)
    }

    def handleDetailsPage(html) {
        def name
        def splitBrFromName
        def nameList = []
        def nameMatch = html =~ /(?is)<\/tr>\s*<tr>\s*<td>(.*?)<\/td>/
        while (nameMatch.find()) {
            name = nameMatch.group(1)
            name = nameSanitize(name)
            splitBrFromName = name.split(/<[^>]+>/)
            nameList.add(splitBrFromName)
        }
        nameList.each {
            it ->
                it.each {
                    createEntity(it)
                }
        }

    }

    def createEntity(name) {
        def entity
        ScrapeEvent event = new ScrapeEvent()
        ScrapeAddress scrapeAddress = new ScrapeAddress()
        scrapeAddress.setAddress1(address)
        event.setDescription(description)
        def type = detectEntityType(name)
        def aliases = name.split(/(?i)\b(?:D\.B\.A|[FA]\.?K\.A|A\\/KA|AKA|A\\/k\\/A|A\.KA)\b/)
        name = StringUtils.removeEnd(aliases[0].trim(), ",").trim()
        if (type.equals("P")) {
            name = personNameReformat(name)
        }
        if (type.equals("O")) {
            entity = context.findEntity("name": name, type: type)
        }
        if (entity == null) {
            entity = context.getSession().newEntity()
            entity.setName(name)
            entity.setType(type)
            entity.addAddress(scrapeAddress)
            entity.addEvent(event)
            for (int i = 1; i < aliases.length; i++) {
                def alias = StringUtils.removeEnd(aliases[i].toString().trim(), ",")
                alias = sanitizeAlias(alias)
                alias = StringUtils.removeEnd(alias, ",")
                entity.addAlias(alias)
            }
        }
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

    def detectEntityType(name) {
        def type
        if (name =~ /(?i)\b(?:THE|MIDDLE EAST|BMCSC|INTERMAAX|CARS|ACADEMY|GLOBAL|PRODUCTION|BUNKERING|TECHNO|LABORATORY|PRODUCTIONS|ORGANIZATION|LTC|OPERATIONS|ADVERTISING|AMERICAN|FOUNDATION|U-PVC|BUREAU|L\.L\.C|\s+&\s+)\b/) {
            return "O"

        } else {
            type = context.determineEntityType(name)
        }

        return type
    }

    def nameSanitize(name) {
        return name.toString().replaceAll(/\n/, "").toUpperCase().trim()

    }

    def personNameReformat(name) {

        //Regroup person name by comma
        /**
         abc, sdf, jr --match
         abc, sdf jr -- not match
         C. Conway Felton, III -- not match
         O'Dowd, Jr., Charles T. -- match
         * */
//        def exToken = "(?:[js]r|I{2,3})"
//        return name.replaceAll(/(?i)\s*(.*?)\s*,\s*\b((?:(?!$exToken\b)[^,])+)\s*(?:,\s*\b($exToken)\b)?\s*$/, '$2 $1 $3').trim()
        return name.replaceAll(/(?i)^\s*([^,]+),\s*([^,]+),?(\s*[js]r\.?|\s*I{2,3})?\u0024/, '$2 $1 $3').trim()
    }

    def sanitizeAlias(alias) {
        return alias.toString().replaceAll(/(?s)\s+/, " ").replaceAll(/\./, "").replaceAll("\\u201d|\\u201c", "").trim()
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