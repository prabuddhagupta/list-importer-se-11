package current_script

import com.rdc.importer.scrapian.model.StringSource
import scrapian_scripts.utils.AddressMappingUtil
import scrapian_scripts.utils.LabelValueParser

import java.text.SimpleDateFormat
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit;

context.getSession().setEscape( true );

form = context.scriptParams.form;
baseUrl = context.scriptParams.baseUrl;
state = context.scriptParams.state;
stateAbbreviation = context.scriptParams.stateAbbreviation;
sdf = new SimpleDateFormat("MM/dd/yyyy");
indexUrl = baseUrl + "search_cnty.htm";
if (stateAbbreviation.equals("KY")) {
  indexUrl = "http://kspsor.state.ky.us/sor/html/SORSearch.htm";
  context.setup([socketTimeout: 30000, connectionTimeout: 10000, retryCount: 50, multithread: true]);
} else if (stateAbbreviation.equals("ID"))  {
  context.setup([socketTimeout: 30000, connectionTimeout: 10000, retryCount: 5, multithread: true]);
  indexUrl = baseUrl + "search_cnty.html";
} else
  context.setup([socketTimeout: 30000, connectionTimeout: 10000, retryCount: 5, multithread: true]);

searchUrl = baseUrl + "SOR?form=" + form + "&srt=1&cnt=";
addressMapper = new AddressMappingUtil();


handleIndexPage(indexUrl);
//handleEntityPage("https://www.isp.idaho.gov/sor_id/SOR?id=26534&sz=400", "Test name", "10/10/2000", "test", "test", "2000");
//handleEntityPage("https://www.isp.idaho.gov/sor_id/SOR?id=23393&sz=1671", "Test name", "10/10/2000", "test", "test", "2000");
//handleCountyPage("NIOBRARA");

def handleIndexPage(indexUrl) {
  def indexPage = context.invoke([url: indexUrl, tidy: false]);
  indexPage.replace("<DIVCLASS.*?>", "");
  
  indexPage = filterPage(indexPage);
  indexPage = new StringSource(indexPage);
  indexPage = context.tidy(indexPage, null);

  def startTextStr = "Select County";
  if (stateAbbreviation.equals("KY")) {
    indexPage.replace("name=\"cnt\"", "Select County");
    indexPage.stripFormatting().replace("ZIP Code</span>.*\$", "")
    startTextStr = "County<";
  } else {
    indexPage = context.invoke([url: baseUrl+"county.html", tidy: true]);
  }

  context.elementSeek(indexPage, [element: "option", startText: startTextStr, greedy: false]).each {option ->
    if (option.toString().contains("selected")) {
      return;
    }
    def county = context.regexMatch(option, [regex: "value=\"(.*?)\""]);
    if (county[1].toString()) {
      handleCountyPage(county[1].toString());
    }
  }
}

def handleCountyPage(county) {//println( "COUNTY $county" );
  def gotOne = true;

  for (page = 1; gotOne; page++) {
    def aSearchUrl = "";
    if (stateAbbreviation.equals("ID"))  {
      aSearchUrl = searchUrl + county.replace(" ", "+") + "&rad=A&page=" + page;
    } else{
      aSearchUrl = searchUrl + county.replace(" ", "+") + "&page=" + page;
    }
    searchPage = context.invoke([url: aSearchUrl, tidy: false]);
    searchPage = filterPage(searchPage);
    searchPage = new StringSource(searchPage);
    searchPage = context.tidy(searchPage, null);
    
    gotOne = handleSearchResultsPage(searchPage);
	if(!gotOne){
		context.info("Found no results from this page (will retry once):\n" + searchPage.toString());
		Thread.sleep(15 * 1000);
		searchPage = context.invoke([url: aSearchUrl, tidy: false]);
		searchPage = filterPage(searchPage);
		searchPage = new StringSource(searchPage);
		searchPage = context.tidy(searchPage, null);
		
		gotOne = handleSearchResultsPage(searchPage);
	}
  }
}

def handleSearchResultsPage(searchPage) {
  def gotOne = false;
  pool = Executors.newFixedThreadPool(8);

  context.elementSeek(searchPage, [element: "tr", startText: "Status<"]).each {tr ->
    tr.stripFormatting();
    tr.replace("</span> </td>", "</span></td>");
    tr.replace("<span class=\".*?\" /></td>", "<span></span></td>");
    context.regexMatches(tr, [regex: "<td(?:.*?)</td><td(?:.*?)<a href=\"(.*?)\".*?>([^<>]+?)<.*?<td.*?>(.*?)</td.*?><td.*?>(.*?)</td.*?><td.*?>(.*?)<.*?>\\s*(\\d*?)\\s*<"]).each {detail ->
      // the regex will pull in a <td tag if it's an empty zip code, clean that up
      detail[6].replace("^<.*\$", "");
      detail[2].replace( "\\s{2,}"," " ).trim();
      thread = {c -> pool.submit(c as Callable) };
      thread
      {
        handleEntityPage(baseUrl + detail[1].replace("&amp;", "&").toString(), detail[2].toString(), detail[3].toString(), detail[4].toString(), detail[5].toString(), detail[6].toString());
      };
      gotOne = true;
    }
  }
  pool.shutdown();
  pool.awaitTermination( 3600, TimeUnit.SECONDS);
  return gotOne;
}

def handleEntityPage(entityUrl, entityName, dob, street, city, zip) {
  try {
    def entityPage = context.invoke([url: entityUrl, tidy: false]);
    
    def ePage = entityPage;
    entityPage = filterPage(entityPage);
    entityPage = new StringSource(entityPage);
    entityPage = context.tidy(entityPage, null);
    synchronized (this) {
      def labelValueParams = [
        labels: [
          "Reg ID", "Name", "Aliases", "Date of Birth", "Birth Date", "Place of Birth", "Birth Place", "Sex", "Race",
          "Height", "Weight", "Eye Color", "Hair Color", "Ethnicity"
        ],
        dividers: [":"],
        multiLineValues: true,
        multiLineSeparator: ";",
        breakOnLabels: true,
        replaceElements: ["</tr>": "\n\n", "<br>": " "]
      ]
      def profileSection = context.regexMatch(entityPage, [regex: "^(.*?)(Photo Date|Reg Type:)"]);
      def labelValueParser = new LabelValueParser();
      def text = labelValueParser.htmlToText(profileSection[1], labelValueParams)
      def dataMap = labelValueParser.parse(text, labelValueParams);

      entityName = context.formatName(entityName);
      def srcId = (entityUrl =~ /[\?&]id=(\d+)/)[0][1];
      def entity = context.findEntity([name: entityName, id: srcId]);
      if(entity == null){
        entity = context.getSession().newEntity();        
      }else{
        return;
      }
      entity.setDataSourceId(srcId);
      entity.name = entityName;
      entity.type = "P";

      if (dataMap["Race"]) {
        def race = dataMap["Race"].replaceAll(/(?i)Unknown/,'').trim();
        if(race.length() > 0){
          entity.addRace(race);
        }
      }

      if (dataMap["Sex"]) {
        entity.addSex(dataMap["Sex"]);
      }

      if (dataMap["Date of Birth"]) {
        def dobSource = new StringSource((String) dataMap["Date of Birth"]);
        dobSource.replace("(.*?) (\\d+),(\\d+)", "\$1 \$2, \$3");
        context.parseDateOfBirthForEntity(entity, dobSource);
      }

      if (dataMap["Birth Date"]) {
        context.parseDateOfBirthForEntity(entity, new StringSource((String) dataMap["Birth Date"]));
      }

      if (dataMap["Height"]) {
        entity.addHeight(dataMap["Height"]);
      }

      if (dataMap["Weight"]) {
        entity.addWeight(dataMap["Weight"]);
      }

      if (dataMap["Eye Color"]) {
        def color = dataMap["Eye Color"].replaceAll(/(?i)Unknown/,'').trim();
        if(color.length() > 0){
          entity.addEyeColor(color);
        }
      }

      if (dataMap["Hair Color"]) {
        def hcolor = dataMap["Hair Color"].replaceAll(/(?i)Unknown/,'').trim();
        if(hcolor.length() > 0){
          entity.addHairColor(hcolor);
        }
      }

      def addr = entity.newAddress();
      addr.setProvince(stateAbbreviation);
      addr.setCountry("US");
      if (street.trim() && !street.equals("NON-COMPLIANT") && !street.equals("ABSCONDED")) {
        addr.setAddress1(street.replaceAll(/&amp;/,'&').trim());
      }
      if (zip.trim()) {
        addr.setPostalCode(zip);
      }
      if (city.trim()) {
        addr.setCity(city);
      }
      
      def addrFound = false;
      //WY,KY
      def addrReg = ePage =~ /(?s)(?:Address.*?)?Home:[^\n]*?<(?:div|td)[^>]*?>\s*([^\n<]+)\s+([^\n<]+?)\s+(\w+)\s+(\d+)/
      if(!addrReg){
        //ID
        addrReg = ePage =~/(?s)Primary Address.*?<span.*?>\s*([^<]+)\s*<br[^>]+?>\s*([^,<]+)\s*,\s*(\w+)\s*([\d-]+)/
      }else{
        addrFound = true;
      }
      
      if(addrFound || addrReg){
        (0..<addrReg.count).each {
          if(it > 0){
            addr = entity.newAddress();			
            addr.setCountry("US");
          }
          if (addrReg[it][1].trim() && !addrReg[it][1].equals("NON-COMPLIANT") && !addrReg[it][1].equals("ABSCONDED")) {
            addr.setAddress1(addrReg[it][1].replaceAll(/&amp;/,'&').trim());
          }
		  
		  def state = stateAbbreviation
		  
		  if(addrReg[it][3] && addrReg[it][3].trim() != state){
			  state =  addressMapper.normalizeState(addrReg[it][3]); 
		  }
		  addr.setProvince(state);
		  
          addr.setPostalCode(addrReg[it][4]);
          addr.setCity(addrReg[it][2]);
        }
      }

      // WY and ID
      def aliasSection = context.regexMatch(entityPage, [regex: "(?s)Alias(?:es)?:(.*?)(?:Date of Birth|Birth Date)"]);
      handleAliases(entity, aliasSection);
      // KY
      aliasSection = context.regexMatch(entityPage, [regex: "Aliases</b>(?:.*?)<table(.*?)</table>"]);
      handleAliases(entity, aliasSection);

      def eventInfo = ePage =~ /(?s)Conviction.*?<\/tr>(.*?)<\/t[^rd]/;
      if (eventInfo) {
        def eRegex = eventInfo[0][1] =~ /(?s)<(?:span|div)[^>]*?>\s*([^<>]*)(?:\s*<[^<>]+>\s*){2}<td[^>]+>(\w+)\s+(\d+)[^\d]+(\d+)/
        if(eRegex){//WY & ID
         List eList = [];
          (0..<eRegex.count).each{
            def month = getMonthVal(eRegex[it][2]);            
            if(month != null){
              def eStr = eRegex[it][1] + month + eRegex[it][3] + eRegex[it][4];
              if(!eList.contains(eStr)){
                def event = entity.newEvent();
                event.setDescription("Sex Crime: " + eRegex[it][1].replaceAll(/&amp;/,'&').trim());
                event.setDate(month+"/"+eRegex[it][3]+"/"+eRegex[it][4]);
				if(!verifyDate(event.getDate())){
					event.setDate(null);
				}
                eList.add(eStr);
              }
            }
          }
        }
      } else {
        //KY
        eventInfo = ePage =~ /(?si)Offenses?:.*?<td.*?>(.*?)<\/table/
        if(eventInfo){
          def eRegex = eventInfo[0][1] =~ /(?s)<span.*?(?:<a[^>]*?)?>(.*?)\s*<\/span/
          if(eRegex){
           List eList = [];
            (0..<eRegex.count).each{
              def eStr = eRegex[it][1].replaceAll(/&nbsp;|<\/?[^>]+?>|\*+|^\s*-\s*/,'').replaceAll(/&amp;/,'&');
              eStr = eStr.replaceAll(/^\s*-\s*/,'').trim();
              if(!eList.contains(eStr) && eStr.length() > 0){
                def event = entity.newEvent();
                event.setDescription("Sex Crime: " + eStr);
                eList.add(eStr);
              }
            }
          }
        }else{
          def event = entity.newEvent();
          event.setDescription("Entity appears on the " + state + " Sex Offenders site");
        }
      }

      def imageUrl = context.regexMatch(entityPage, [regex: "<img src=\"((?:photos|../photos)/.*?)\""]);
      if (imageUrl) {
        entity.addImageUrl(baseUrl + imageUrl[1].toString());
      }

      entity.addUrl(entityUrl);
    }
  } catch (Exception e) {
    context.error("ERROR: [" + entityUrl + "]:" + e.message);
  }
}

def handleAliases(entity, aliasSection) {
  if (aliasSection) {
    context.elementSeek(aliasSection[1], [element: "tr"]).each {tr ->
      context.regexMatches(tr, [regex: "<td(?:.*?)>(.*?)</td>"]).each {alias ->
        alias[1].replace( "(?i)(?:^|,?\\s+)\\s*(?:&nbsp;|null|unknown|none|n/?a)(?:,[^\n]*)?(?=\\s|\$)", "" );
        if (alias[1].trim().toString().length() > 1) {
          entity.addAlias(context.formatName(alias[1].toString()));
        }
      }
    }
  }
}

def filterPage(page) {
  page = page.toString();
  page = page.replaceAll(/<script([\s\S]*?)script>/, '');
  page = page.replaceAll(/<style([\s\S]*?)style>/, '');
  page = page.replaceAll(/<gcse:search><\/gcse:search>/, '');
  page = page.replaceAll(/&nbsp;/, ' ');
  page = page.replaceAll(/<\/?span.*?>/, '');
  return page;
}

def getMonthVal(String val){
  def list = [
    "JAN":"01",
    "FEB":"02",
    "MAR":"03",
    "APR":"04",
    "MAY":"05",
    "JUN":"06",
    "JUL":"07",
    "AUG":"08",
    "SEP":"09",
    "OCT":"10",
    "NOV":"11",
    "DEC":"12"
    ];
  return list[val.replaceAll(/^\s*(.{3}).*$/, '$1').toUpperCase()];
}

def boolean verifyDate(String date){
	Date today = new Date();
	try{
		Date fromSite = sdf.parse(date);		
		return fromSite.before(today);
	} catch (Exception e){
		return false;
	}
}
