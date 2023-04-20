package current_script

import org.apache.commons.lang.StringUtils
import scrapian_scripts.utils.LabelValueParser

import java.util.regex.Matcher

context.session.encoding = "UTF-8";
context.setup([socketTimeout: 30000, connectionTimeout: 10000, retryCount: 5]);
context.getSession().setEscape( true );

defaultAction = "Entity appears on NY's Summaries of Regents Actions On Professional Misconduct and Discipline List"
stateList = ["AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL",
        "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT",
        "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI",
        "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"]

baseUrl = "http://www.op.nysed.gov/opd/"
indexUrl = "http://www.op.nysed.gov/opd/rasearch.htm"
monthUrl = "http://www.op.nysed.gov/opd/jul10.htm"
monthsFor94 = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "sep", "nov", "dec"]

patternUniversal = "(?is)<(\\w+)[^<>]+?class=\"(?:item|header_with_line_above)(?:\\s+[^\"]+)?\"[^<>]*>(.*?),\\s+(?!(?:i\\.?n\\.?c|l\\.?[lt]\\.?[pcd]|[ad][\\./][kb][\\./]a|cpa|p\\.c)\\.?)(.*?)(</\\1>.*?(?=<\\1[^<>]+?class=\"(?:item|header_with_line_above)(?:\\s+[^\"]+)?\"[^<>]*>)|mailto)"
handleIndexPage(context, indexUrl)

//debug:
//handleMonthPage(context, "http://www.op.nysed.gov/opd/dec95.htm", patternUniversal);


def handleIndexPage(context, pageUrl) {
  resultPage = context.invoke([url: pageUrl, tidy: true]);

  context.regexMatches(resultPage, [regex: "[a-zA-Z]{3,7}\\d{2,4}\\.html?"]).each { month -> 
    monthUrl = baseUrl + month[0];
    handleMonthPage(context, monthUrl, patternUniversal);
  }

  monthsFor94.each() {
    month = baseUrl + "${it}" + "94.htm"
    handleMonthPage(context, month, patternUniversal);
  }
}

def handleMonthPage(context, monthUrl, pattern) {
  monthPage = context.invoke([url: monthUrl, tidy: true]).replace("[a&\\s]amp;", "&").replace(";", ",").replace(/,(?!\s)/, ', ');
  monthPage.replace("(?<=Sklar|Brock),","");
  monthPage.replace("(?i)FORMERLY KNOWN AS","A.K.A");
  monthPage.replace(", Jr"," Jr");
  monthPage = context.regexMatch(monthPage, [regex: "<body>(.*?)</body>"]);

  def entries = context.regexMatches(monthPage[1], [regex: pattern])
  if(entries.equals(null)){
    throw new Exception("Regex failed to find entities in page, "+monthUrl);
  }
  entries.each { entry ->
    section = entry[4].toString()
    section.replaceAll("&nbsp;", " ")
    entity = handleEntityPage(context, entry[2].convertNbsp(), entry[3].convertNbsp(), section, monthUrl);    
  }
}

def handleEntityPage(context, name, location, section, pageUrl) {
	name = name.stripXmlTags().stripXmlAttributes().toString().trim().replaceAll(/(?s)\n.*$/, '').toUpperCase()
		//  if((name.indexOf("SCHAPIRO") >= 0 || name.indexOf("NOMAAN") >= 0 )) {
		location = location.stripXmlTags().stripXmlAttributes().toString().trim().replaceAll(/(?s)(\n|\r).*$/, '').toUpperCase()
		name = name.replaceAll("(?i)&amp;", "&").replaceAll(/[\(\)]|(?<!&)#|\d+\s*$/, '').replaceAll(/\s{2,}/, ' ').trim();
		loc = clean(location).replaceAll("(?i)&amp;", "&").replaceAll(" AND ", ";").replaceAll(/\s{2,}/, ' ').trim();

		if (name.size() == 0)
				return null

		entity = handleName(name)
		if (!entity)
				return null

		entity.addUrl(pageUrl)
		handleLocation(entity, loc)
		if (entity.getAddresses() == null || entity.getAddresses().size() == 0) {
				addr = entity.newAddress();
				addr.setProvince("New York")
				addr.setCountry("UNITED STATES")
		}
		handleSection(entity, section, pageUrl)
	//}
}



def handleName(name) {
  if (name.trim().size() == 0)
    return null

  if (name.contains("/")) {
    name = name.replaceAll("/", ".");
  }

  aliases = name.split("(?:D\\.B\\.A|[FA]\\.?K\\.A)");
  name = StringUtils.removeEnd(aliases[0].trim(), ",");

  if (name.contains("HOSPITAL") || name.contains("RITE AID") || name.contains("PHARMACY"))
    type = "O";
  else
    type = context.determineEntityType(name)
    
  def entity = context.findEntity(["name": name, "type": type]);
  if(entity == null){
    entity = context.getSession().newEntity()
  }
  
  entity.setName(name)
  entity.setType(type)
  for (int i = 1; i < aliases.length; i++) {
    addAlias(entity, aliases[i]);
  }

  return entity
}

def handleLocation(entity, loc) {
  addresses = splitAndTrim(loc, ";")

  for (String address: addresses) {
    addr = entity.newAddress()
    addr.setProvince("New York")
    streetAddr1 = ""
    if ((elements = address.split(",")).size() > 1) { //more than one element
        if(elements[0]=='LACHINE' && (elements[1]==' QUEBEC' || elements[1]=='QUEBEC')  ){
            addr.setCity("LACHINE");
            addr.setProvince('QUEBEC');
            addr.setCountry('CANADA');
            entity.addAddress(addr)
            break;
        }
      if (elements[-1].trim() in stateList) {//last element is a US state
        if (elements.size() > 2) {
          int a;
          for (a = 0; a < elements.size() - 2; a++) {
            if (elements[a].contains("A/K/A")) {
              als = elements[a].trim().split("A/K/A")
              for (String l: als) {
                addAlias(entity, l)
              }
            } else if (elements[a].contains("D/B/A")) {
              entity.type = 'O'
              als = elements[a].trim().split("D/B/A")
              for (String l: als) {
                addAlias(entity, l)
              }
            } else if (elements[a].contains("D.B.A")) {
                entity.type = 'O'
                als = elements[a].trim().split("D.B.A")
                for (String l: als) {
                  addAlias(entity, l)
                }
            } else {
              streetAddr1 += " " + elements[a]
            }
          }
          streetAddr1 = streetAddr1.trim()
          if (streetAddr1.startsWith("INC."))
            streetAddress1 = streetAddr1.replaceFirst("INC.", "")               //fixme this should be part of entity name
          if (streetAddr1.startsWith("P.C."))
            streetAddress1 = streetAddr1.replaceFirst("P.C.", "")
          if (!streetAddr1.equals("JR."))
            addr.setAddress1(streetAddr1.trim())
        }
        if (elements[-2].contains("A/K/A")) {
          al = elements[-2].trim().split("A/K/A")
          for (String a: al) {
            addAlias(entity, a)
          }
        } else if (elements[-2].contains("D/B/A")) {
          entity.type = 'O'
          al = elements[-2].trim().split("D/B/A")
          for (String a: al) {
            addAlias(entity, a)
          }
        } else if (elements[-2].contains("D.B.A")) {
          entity.type = 'O'
          al = elements[-2].trim().split("D.B.A")
          for (String a: al) {
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
              for (String v: elems) {
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
    addr.setCountry("UNITED STATES");
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

def handleSection(entity, pageText, pageUrl) {
   event = entity.newEvent();

    // Using this for date search as to not change the other regexes
    def scrubbedPageText = pageText.replaceAll("<strong>","").replaceAll("</strong>","").replaceAll("<span>","").replaceAll("</span>","").replaceAll("</p>","").replaceAll("<p>","")

    //Attempting to add a date to every entity
    def eventDateMatcher = scrubbedPageText =~ /Action Date(.*?):(.+)/

    if (eventDateMatcher.find())
    {
        def dateText = eventDateMatcher[0][2]
        // scrub date further down
        eventDateMatcher = dateText =~ /(.*?),([\s\d]+)/

        // try to add the date to fit our format
        if (eventDateMatcher.find()) {
            dateText = eventDateMatcher[0][1].toString().trim() + eventDateMatcher[0][2]

            try {
                Date date = Date.parse("MMMM dd yyyy", dateText.trim())
                event.setDate(date.format('MM/dd/yyyy'))
            } catch (Exception e) {
                context.info(entity.name + ", " + e.getMessage())
            }
        }
    }

    if(pageUrl.indexOf("jul17.htm")>=0 && (entity.name.indexOf("MARITZA") >= 0 || entity.name.indexOf("NOMAAN") >= 0 ))
    {
        def eventDesc;

        Matcher eventActionMatch = pageText =~ /(?<=)(Action:\<\\/(strong|span)>)(.*(?=\<))/
        if (eventActionMatch.find()) {
            eventDesc = eventActionMatch.group(3).trim();
        }

        Matcher eventSummaryMatch = pageText =~ /(?<=)(Summary:\<\\/(strong|span)>)(.*(?=\<))/
        if (eventSummaryMatch.find()) {
            eventDesc = eventDesc + eventSummaryMatch.group(3).trim();
        }


        if (eventDesc && eventDesc.trim().length() > 0) {
            event.setDescription(eventDesc)
        } else {
            event.setDescription(defaultAction);
        }
        entity.addEvent(event)

    }else{
        def params = [
                //labels: ["Regents Action Date", "Action", "Summary"],
                labels: ["Action", "Summary"],
                dividers: [":"],
                replaceElements: ["<br />": "\n\n", "</p>": "\n\n", "<ul>": "\n\n", "</h3>": "\n\n"],
                breakOnLabels: false
        ]
        action = ""
        def labelValueParser = new LabelValueParser();
        def text = labelValueParser.htmlToText(pageText, params);
        def dataMap = labelValueParser.parse(text, params);

        if (!dataMap)
            event.setDescription(defaultAction);
        if (dataMap["Action"])
            action = dataMap["Action"].toString()
        if (dataMap["Summary"])
            action = (action + " " + dataMap["Summary"]).toString().replaceAll(/\s{2,}/, ' ').trim()


        if (action.trim().length() == 0)
            event.setDescription(defaultAction);
        else{
            event.setDescription(action.replaceAll("&#146;", "'"))
        }

        //event.date = dataMap["Regents Action Date"]
        entity.addEvent(event)
    }
}


def splitAndTrim(input, splitter) {
  if (!input)
    return null;

  String[] splits;
  if (!input.contains(splitter)) {
    splits = new String[1];
    splits[0] = input.trim();
    return splits;
  }

  splits = input.split(splitter);
  int s;
  for (s = 0; s < splits.length; s++)
    splits[s] = splits[s].trim();
  return splits;
}
