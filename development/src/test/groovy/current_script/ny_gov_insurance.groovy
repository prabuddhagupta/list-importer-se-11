package current_script

import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeSource

import java.util.Map.Entry
/**
 * Date: 26/05/2015
 * */

context.setup([socketTimeout: 80000, connectionTimeout: 10000, retryCount: 5]);
context.getSession().setEscape( true )
context.session.encoding = "UTF-8";

class Cpy_ny_gov_global{

  static baseUrl = "http://www.dfs.ny.gov/insurance/";
  static url1 = "http://www.dfs.ny.gov/insurance/das-dfs.htm";
  static url2 = "http://www.dfs.ny.gov/insurance/das.htm";

  def static dupPersonMap =[:]
  def static globalOrgMap = [:];
  def static dedupMap = [:]


  static lastAddr = ""//Some html block only contains "same as above"
}

scriptRunner();

//----------- Debug starts-------------------//
//str = """
//LP. Corazza Agency Inc.               871-7 Connetquot Avenue
//(Agent)                               Islip Terrace, NY 11752               License
//                                                                            Revoked
//Leonard P. Corazza                    Same as above
//(Sublicensee)
//Respondents collected insurance premium payments from insureds and failed to remit or
//otherwise properly account for such premium payments. Respondents' agency appointment
//with an insurer was terminated for cause due to misappropriation of funds. Respondents also
//issued two forged temporary New York State insurance identification cards for a motor
//vehicle, knowing the cards did not actually represent an insurance policy as indicated.
//[Order issued April 29, 2013.
//"""
//
//url = "http://www.dfs.ny.gov/insurance/da/2011--2020/da20120621.pdf"
//urls = ["http://www.dfs.ny.gov/insurance/da/2001_2010/da20070823.pdf"]
////Cpy_ny_gov_global.populateDupMap();
//urls.each{url->
//  def er=entityCollector(url);
//  //   // er = pdfDataRowParser(pdfTextFix(str))
//  //    //er = htmlDataRowPrser(str)
//  createEntity(er, " 06/22/2001", url, url);
//}
//fWriter.close();
//-------------------------Debug ends--------------------------//


def scriptRunner(){
  Cpy_ny_gov_global.dupPersonMap = [:];
  inItSearch(Cpy_ny_gov_global.url1);
  inItSearch(Cpy_ny_gov_global.url2);
}

def inItSearch(srcUrl){
  def errorUrl = srcUrl =~ /\b\/das\.htm$/
  if(errorUrl.find())
    return
  html = context.invoke([url: srcUrl, tidy: true, cache: false]);
  def data = html=~ /(da\\/.*\\/da\d*.*(?:pdf|htm))">\s*(\d\d\\/\d\d\\/\d\d\d\d)/;

  while(data.find()){
    def url = Cpy_ny_gov_global.baseUrl+data.group(1).replaceAll(/(?i)&amp;/,'&');
    def date = data.group(2);
    createEntity(entityCollector(url), date, url, srcUrl);
  }
}

def entityCollector(url){
  if(url=~/(?i)\.pdf/){
    return pdfParser(url);
  }else{
    return htmlParser(url);
  }
}

def htmlParser(url){
  def html = context.invoke([url: url, tidy: false]);
  def match = html=~/(?s)((?-i:\bLICENSEE\b))[^\n]+(.*?)(?=(?-i:\1)|\z)/;
  List result = [];
  def found = false;

  while(match.find()){
    found = true;
    result.addAll(htmlDataRowPrser(match.group(2)));
  }

  if(found){
    return result;
  }else{
    return htmlOneParser(html);
  }
}

def htmlOneParser(html){
  html = html.toString().replaceAll(/(?s)<table.*?<\/tr>/, "")
      .replaceAll(/(?is)>\s*Total.*?<\/td>/, "");

  return htmlOneDataRowParser(html);
}

def htmlOneDataRowParser(text){
  text = aliasFixer(text);
  def comNameArr = [];
  def fineArr = [];
  def found = false;
  def match = text =~ /(?s)<tr(.*?)<\/tr/ ;

  while(match.find()){
    def txtVal = match.group(1);
    found=true;

    def matchn = txtVal =~ /<td[^>]+>\s*(?:<[^>]+>\s*)*((?:[^<]+(?:<\/?(br|li)>\s*)+)+[^<]+)/;
    if(matchn){
      comNameArr = matchn[0][1].split(/(?s)(?:<[^>]+>\s*)+/).collect{return it.trim();};
      fineArr = matchn[1][1].split(/(?s)(?:<[^>]+>\s*)+/).collect{return it.trim();};

    }else if((matchn = txtVal =~ /(?s)<td[^>]+>(.*?)<\/td>[^>]+>(.*?)<\/td/)){

      compName = matchn[0][1].replaceAll(/(?s)(?:<[^>]+>\s*)+/, '').trim();
      fine = matchn[0][2].replaceAll(/(?s)(?:<[^>]+>\s*)+/, '').trim();
      if(compName){
        comNameArr.add(compName);
      }
      fineArr.add(fine);
    }
  }

  if(!found){
    //ie: http://www.dfs.ny.gov/insurance/da/1997_2000/da20000731.htm
    match = text =~ /(?i)((?:\b(?:(?-i:[A-Z1-3])\w+|of|and)\b\s*|\([^\(\)]+\)\s*)+)\s+(?:have\s+)?each\s+been\b.*?(\$[^\s]+)/
    if(match){
      //multiple entries with each
      def names =  match[0][1].replaceAll(/(?s)\s+/, ' ').split(/(?i)\s+and\s+/).collect({ it.trim() });
      comNameArr.addAll(names);
      fineArr.add(match[0][2].trim())
    }else{
      match = text =~ /(?i)(.*(?=FINED|AGREES)|(?<=FINES).*?(?=\$)).*?(\$[\d,]+(?:\s*mill\w*)?)/
      if(match){
        comNameArr.add(match[0][1].trim())
        fineArr.add(match[0][2].trim())
      }else{
        match = text =~ /(?s)(?is)fined\s+are\s+as\s+follows.*?<p>(.*?)<\/p>/;
        if(match){
          comNameArr = match[0][1].split(/(?i)<[^\w]*br[^\w]*>/).collect{return it.trim();};
          fineArr.add("Fines are not defined");
        }
      }
    }
  }

  return createEntityArray(comNameArr, [""], fineArr);
}

def htmlDataRowPrser(txt){
  def orgName, address, penalty;
  txt = aliasFixer(txt);
  txt = txt =~ /(?s)<tr(.*?)<\/tr/ //initial tr group to chk 3 consec td

  if(txt){
    txt = txt[0].toString().replaceAll(/(?:\\r|\\n)+/, ' ') =~ /(?s)<td[^>]+>(.*?)<\/td.*?<td[^>]+>(.*?)<\/td.*?<td[^>]+>(.*?)<\/td/
    if(txt){
      orgName = txt[0][1];
      address = txt[0][2];
      penalty = txt[0][3];
	  if(!penalty){
		  penalty = '';
	  }
    }else{
      println("Please recheck rexreg pattern for html");
    }
  }

  def nameArr = []

  orgName = orgName.replaceAll(/(?s)\(.*?\)[\s\.]*/,"")
      .replaceAll(/(?si)<[^>]+>\s*\b(?=Corporation|Company|Inc)/, " ")
      .replaceAll(/(?i)c\/o/, "")
      .replaceAll(/(?i)&nbsp;/, " ")

  orgName.split(/(?is)(?<!\/)(?:<[^>]+>\s*)+\s*\b(?!(of|brokerage|Services?|co|america|Ins|new york))\b|(?<=Dorfman\b)\s*(,|\band\b)\s*\b(?!inc)/).each{val->
    val = val.replaceAll(/(?i)^\s*and\s*$/, "")
    val = sanitize(val)

    if(val){
      nameArr.add(val);
    }
  };

  def addrArr = []
  address.split(/(?<=(?:[\w\s]+,[\s\w]+|above\b))\s*<[^>]+>/).each{val->
    val = val.replaceAll(/(?<!\b(?-i:[A-Z]{2}|\babove)\b)\s*(?s)(?:<[^>]+>\s*)+/,',').replaceAll(/(?i)&nbsp;/," ").replaceAll(/^[,\s]*|[,\s]*$/,'')
    val = sanitize(val)

    if(val){
      addrArr.add(val);
    }
  };

  def penaltyArr = []
  penalty.replaceAll(/(?i)&nbsp;/,"").split(/(?is)(?<!\band\b)\s*(?:<[^>]+>\s*)+/).each{val->
    val = sanitize(val);
    if(val){
      penaltyArr.add(val);
    }
  };

  return createEntityArray(nameArr, addrArr, penaltyArr);
}

def pdfParser(pdfUrl){

  def monthMap = [january:"01",february:"02",march:"03",april:"04",may:"05",june:"06",july:"07",august:"08",september:"09",october:"10",november:"11",december:"12"]
  def monthList =/(?i)(?:January|February|March|April|may|june|july|august|september|october|november|december)/
  def result = []
  def text = pdfToTextConverter(pdfUrl);

  text = text.replaceAll(/(?i)individually\s+and/,'').replaceAll(/\uFFFD/, '-')
  def match = text=~/(?s)((?-i:\bLICENSEE\b))[^\n]+(.*?)(?=(?-i:\1)|\z)/;

  def mArr = []
  while(match.find()){
    def date = ""
    match.group(0).replaceAll(/(?is)\[[^\]]+?(${monthList})\s*(\d+)\s*,\s*(\d+)[^\]]+\]/,{a,b,c,d->
      date = monthMap[b.toLowerCase()]+"/"+c+"/"+d
    })
    def txtArr = pdfTextFix(match.group(2).trim());
    mArr.add([txtArr, date]);
  }

  mArr.each{val->
    def eArray = pdfDataRowParser(val[0])
    eArray.collect{it->
      it.add(val[1]);
    }
    result.addAll(eArray);
  }

  return result;

}

def pdfDataRowParser(valArr){
  def text = valArr[0]
  def extraSpace = valArr[1]
  def lenMatch = text =~ /\A([^\n]*?\s{2,})/
  int nameBlockLen = 0;

  if(lenMatch){
    nameBlockLen = lenMatch[0][1].length() - extraSpace;
  }else{
    println "---malformed data----"
    //throw new Exception("Malformed pdf data")
  }

  def lines = text =~ /([^\n]+)/
  def dataArr = [];
  while(lines.find()){
    def match = ''
    int hlf = ((int)nameBlockLen)/2
    def line = lines.group(1).replaceAll(/^\s{0,${hlf}}(?=\S)/, '');
    if (nameBlockLen == 0) {
      match = line =~ /(?m)^((?:[^\n](?!@)){1,})[^\S\n]*(.*?)(?i)(?=(?>~|=|$))([^\n]*)/
    } else {
      match = line =~ /(?m)^((?:[^\n](?!@)){1,${nameBlockLen}})[^\S\n]*(.*?)(?i)(?=(?>~|=|$))([^\n]*)/
    }
    def len =  match.count;

    if(len > 0){
      dataArr.add(match[0])
    }else{
      dataArr.add(["", lines.group(1), "", ""]);
    }
  }

  List nameArr = [], addrArr = [], penaltyArr = [];
  def addrCaptured = false;
  def isAddrAvailable = false;
  int newAddrLine = 0;
  def confirmedNames = 0;
  def minimumNames = 0;
  def name = "";
  def addr = "";
  def pan = "";

  for(List arr : dataArr){
    def tName = arr[1]//.trim();
    def tAddr = arr[2]//.trim();

    tAddr = tAddr.replaceAll(/(?i)(?<!\d)(\d?)(\d)\s+(?=street|avenue)/,{a,b,c->
      c = Integer.parseInt(c)
      if(c > 3){
        c = c+"th"
      }
      else{
        if(c == 3){
          c= c+"rd"
        }
        else if(c == 2){
          c = c+"nd"
        }
        else{
          if(b){
            b = Integer.parseInt(b)
            if(b==1){
              c = c+"th"
            }
            else{
              c = c+"st"
            }
          }
          else{
            c = c+"st"
          }
        }
      }

      return b+c+' '
    })

    def tPan = arr[3].trim();

    def newNamePush = false;
    if(addrCaptured){
      if(name ){
        //continuous name check: do nothing here
        if(tName =~ /^(?i)(?:brokerage(\s+(?:corp|inc|ltd))?|agency|services\s*(?:inc.)?)|rdc|inc/){
          newNamePush = false;
        }else if(confirmedNames > 0){
          newNamePush = true;
        }else{
          newNamePush = true;
        }
        if(newNamePush){
          name =pdfNamesFix(name);
          if(name){
            //new name push
            if(!arrayFullStringContains(nameArr,name)){
              nameArr.add(name);
            }else{
              int lId = addrArr.size()-1;
              addrArr.remove(lId);
            }
            addrCaptured = false;
            name = "";
          }
          confirmedNames = 0;
        }
      }else{
        int lId = addrArr.size()-1;
        if(lId > 0 && !addrArr[lId-1].equals(addrArr[lId])){
          //Continuous address rows
          addrArr[lId-1] = addrArr[lId-1]+", "+addrArr[lId]
          addrArr.remove(lId);
          addrCaptured = false;
        }
      }
    }

    tName = tName
        .replaceAll(/(.*)\s{3,}(.*)/,{ //fixing extra addr
          a,b,c->
          tAddr = c.trim()+tAddr;
          return b;
        })
        .replaceAll(/(?i).*?Adjuster\b.*?(?=\s{3}|\n|$)/, '')
        .replaceAll(/(?im)(?>\(|.*?\))(?:.(?!\b(?>inc)\b))*[\.\s]*$/,{
          confirmedNames++;
          minimumNames++;
          return "";
        })
        .replaceAll(/(?im)Agent(?:.(?!\binc))*$/, "")
        .replaceAll(/(?i)c\/o|\s+sa\s*$/, "")
        .replaceAll(/(?m)^[\.\s]*$/, '')
        .replaceAll(/(?i)\bAn\b.*/, '')
        .trim();

    if(tName){
      name+= " " + tName;
      name = name.trim();
    }

    tAddr = tAddr
        .replaceAll(/(?m)^[\D\s]*�\s*/,'')
        .replaceAll("\\\\",'')
    tAddr = tAddr.replaceAll(/(?i)(.*?)(?<!(?:suite|south))\b\s{3,}(.*)/,{ //fixing extra penalty
      a,b,c->
      if(tPan){
        return b+" "+c;
      }else{
        tPan = c.trim()+tPan
        return b;
      }
    })
    .replaceAll(/(?i)^[\s-]*\bllc\b/,'')
    .replaceAll(/[,\s]*$/,'')
    .replaceAll(/[=\^]|(?i)(?>Attention|Broker).*|.*?Compliance/, '')
    .trim();

    if(tAddr){
      isAddrAvailable = true;
      addr += ", " + tAddr;
      if(tAddr =~ /[\.,]([\w\s]+|\d+)|(?-i:\d+|[A-Z]{2})\s*$/ && newAddrLine>0){
        addr = addr.replaceAll(/^[,\s]*/,'')
        addrArr.add(addr);
        addrCaptured = true;
        newAddrLine = 0;
        addr = "";
      }else if(tAddr =~ /(?i)(?:same)?\s+as\s+abov/){
        addr = addrArr[addrArr.size()-1]
        addrArr.add(addr);
        addrCaptured = true;
        newAddrLine = 0;
        addr = "";
      }
    }else{
      isAddrAvailable = false;
    }

    if(tPan){
      if(pan && tPan =~/(?i)(?>~|Corrective|License)/){//|Revoked : removed
        penaltyArr.add(pan);
        pan = "";
      }
      pan = pan + " " + tPan.replaceAll(/~|=/,'').replaceAll(/\band\s*$/,'')
      pan = pan.trim()
    }
    newAddrLine++;
  }

  //Last name|pan|addr push|fix
  int nLen = nameArr.size();
  int aLen = addrArr.size();

  name =pdfNamesFix(name);
  if(name){
    if(!isAddrAvailable && nLen >= aLen){
      len = nLen-1;
      if(confirmedNames>0){
        nameArr.add(name);
      }
      else if(len>=0 && (len>minimumNames-1)){
        nameArr[len] = nameArr[len]+" "+name;
      }else{
        nameArr.add(name);
      }
    }else{
      if(!arrayFullStringContains(nameArr,name)){
        nameArr.add(name);
      }else{
        int lId = aLen-1;
        if(lId>=1){
          addrArr.remove(lId);
        }
      }
    }
  }

  if(pan){
    plen  = penaltyArr.size();
    if(plen<nameArr.size()||plen<1){
      penaltyArr.add(pan);
    }else{
      penaltyArr[plen-1] = penaltyArr[plen-1]+" "+pan
    }
  }
  nLen = nameArr.size();
  aLen = addrArr.size();

  if(aLen<nLen && addr){
    addrArr.add(addr.replaceAll(/^[,\s]*/,'').trim());
  }

  //Continuous address rows fix
  aLen = addrArr.size();
  int lId = aLen-1;
  if(lId > 0 && !addrArr[lId-1].equals(addrArr[lId]) && nLen<aLen){
    addrArr[lId-1] = addrArr[lId-1]+", "+addrArr[lId]
    addrArr.remove(lId);
  }
  /*println nameArr;
   println addrArr;
   println penaltyArr;
   println "----";*/

  return createEntityArray(nameArr, addrArr, penaltyArr);
}

def arrayFullStringContains(List arr, str){
  for(String s: arr){
    if(s.equals(str)){
      return true;
    }
  }

  return false;
}

def pdfNamesFix(name){
  name = name.replaceAll(/(?i)(?>\(Agent.*| Broker.*?A(?>pplication|nd))/, '')
  name = name.replaceAll(/(?i)\b(inc\.?)\s*(?>broker.*)/, '$1')
  name = name.replaceAll(/(?i)(?>application).*/, '')
  name = name.replaceAll(/[\^=]|\[[^\]]+\]/, '')

  return name.trim()
}

def pdfTextFix(text){
  def tmp=text;
  def extraSpace = 0;

  text = text.replaceAll(/(?ims)^Maxson Young.*/, '')
  text = text.replaceAll(/(?i)[^\n]*?(?>(?<!\s{3})During|(?s:(?<=\s\s)Infinex)|(?<!\d)1\s+STATE\s+STREET|While|Respondent(?!\s{3}).*|In\s*connection|After)(?s).*/, '')
  text = text.replaceAll(/(?si)[^\S\n]*\b(?>nd|th|(?-i:s)t|(?-i:r)d)\b[^\S\n]*/,'')//(?si)[\S\n]*\b(?>nd|th|st|rd)\b[\S\n]*
  text = text.replaceAll(/(?i)(\$[\d,]+\s*(?:fine)?\w*)/,'~$1')
  text = text.replaceAll(/(?i)AAA.*?Company/, ',')//this is in address field
      .replaceAll(/(?im)^.*?Excess Line(?:.*broker)?(?:.*?(?:\)|and|\s{2}))?/, { a->
        str="";len=a.length();
        (0..<len).each{ str+=" "; }
        return str;
      })
      .replaceAll(/(?i)\b((?>Caro?line|Raoul|E.|Multi-Line))\s+/,'$1 ')//few name's space fix
  text = text.replaceAll(/(?ms)\s+$/, '')

  //penalty recognizer token
  text = text.replaceAll(/(?i)(?<=[^\n\S]{2})\b(?=(?>a(?>ll|nd)\b|Corrective|License|order|pr(?>oviding|emium)|re(?>fund|voke)))/,'=')

  text = aliasFixer(text);
  //Separate entity
  text = text.replaceAll(/(?i)(?<=\b(?>Company|inc)\b)([\.\s]*)=?(?>\b(?>and)\b|,)(?!\s*\b(?>ltd|inc)\b)/, '$1-NEW-')
  text = text.replaceAll(/(?mi)^[\s=]?\band\b(?!\s+\b(?>new))/,'-NEW-')
  text = text.replaceAll(/(?i)\b(?:t[\/\.](?:b[\/\.]?)+a[\/\.]?)/, "-NEW-")
  text = text.replaceAll(/(?i)(?<=\b((?<=.)LP|Inc|Ltd|Limited|plc|Corp(?:oration)?|Company|Partnership(\s{1,2}[VI]{1,3})?|S\.A|P\.L\.C|Incorporated|LLC)\b\.?(?!\.))(?:\s*(?:\band\b|,+))?(?!\s*(?:\band\b|,+))(?!\s*\b(Limited|LLC|Inc|of|Ltd|insurance|services))/, "-NEW-")
  text = text.replaceAll(/(?i)-new-([^\n]*)(?=\n\s*\b(LP|Inc|Ltd|Limited|plc|Corp(?:oration)?|Company|Partnership(\s{1,2}[VI]{1,3})?|S\.A|P\.L\.C|Incorporated|LLC|of (?:company|new york|texas|america))\b)/, '$1')
  text = text.replaceAll(/(?i)-new-(?=(\s*|[^\n]*\n\s*)(-alias-))/, '')
  text = text.replaceAll(/(?i)(?<=(new york|group)\b)\s*\band\b\s*(?=united)/, "-NEW-")

  //address recognizer token
  text = text.replaceAll(/(?i)(?=(?>same\s+as\s+abov))/, '@')
      .replaceAll(/(?mi)^\s*((?>Robert W.|Handel J|Larry L.|Thomas P.)(?:\s+\w+){1,2}\s*$)/, '$1    @same as above')//few malformed name-addr fix

  //continuous name row token
  text = text.replaceAll(/(?i)(\b(?>inc|Co|Insurance Brokerage|(?>Fox|Kurbatsky) Insurance|of\s{2,}))\b/, '^$1')

  //pre specific fix
  text = text.replaceAll(/(23-11)\s+(24)/, '$1!#$2')

  //pdf specific fixing
  text = text.replaceAll(/(?i)[^\n\S](?=(?>c\/o|(?>1(?>0(?>0|400)|1|2(?>5|318?)|3|482|501|7(?>2(?>00|2)|5|82)|80?)|2(?>055|4|86)|3(?>111|3)|4(?>0[10]|8|185)|5(?>11?|5|901|80|)|6(?>57|8|200|)|7(?>0(?>00|1)|25)|801|9128)\b|Louis|One\s+(?>Chase|Nationwide|Hartford)|PO\s+Box|Two\s+(?>Westbrook)|Western Orchard))/, {
    extraSpace = 2;
    return "   ";
  })

  //re specific fix
  text = text.replaceAll(/!#/, ' ')

  text = text.replaceAll(/(?i)(?<=\s(?>line))(\s+)\b(?!(?>ltd))/, {a,b->
    extraSpace = 2;
    return b+"   ";
  })

  //misc replce
  text = text.replaceAll(/(?i)(?<=\S)[^\S\n]+\(Broker\)/, '')
  text = text.replaceAll(/(?i)in the.*?New York|General.*?and/, ' ');
  text = text.replaceAll(/(?i)Adams\s+18\s+Fl/,'Adams 18 Fl');

  return [text.trim(), extraSpace];
}

def pdfToTextConverter(pdfUrl){
  def pdfFile = context.invokeBinary([url: pdfUrl, clean: false]);
 // println "PDF url: "+pdfUrl

  def pmap = [:] as Map;
 // pmap.put("1", "-layout")
  pmap.put("2", "-raw")
//  pmap.put("3", "-enc")
//  pmap.put("4", "UTF-8")
//  pmap.put("5", "-eol")
//  pmap.put("6", "dos")

  pdfText = context.transformPdfToText(pdfFile, null).toString();

  return pdfText;
}

def createEntityArray(List nameArr, addrArr, penaltyArr){
  //return: [[name, address, penalty],[..],....]
  List result = [];
  if(nameArr.size()<1){
    return result;
  }
  def currentAddr = ''
  if(addrArr[0]){
  currentAddr = sanitize(addrArr[0]);
  currentAddr = currentAddr.replaceAll(/(?i)(.*?)\s*same\s+as\s+above\b\s*(.*)/,{a,b,c->
    return (b+Cpy_ny_gov_global.lastAddr+c).replaceAll(/^[,\s]*|[,\s]*$/,"")
  })
  }

  def currentPenalty = penaltyArr[0];

  nameArr.eachWithIndex{ def val,id->
    List eArr = [];
    val = sanitize(postNameFix(val));
    eArr.add(val);

    if(id >= addrArr.size()){
      eArr.add(currentAddr);
    }else{
      def sameAddr = false;//addrArr[id] =~ /(?i)same\s+as\s+above\b/;
      currentAddr = addrArr[id].replaceAll(/(?i)(.*?)\s*same\s+as\s+above\b\s*(.*)/,{a,b,c->
        sameAddr = true;
        return (b+", "+currentAddr+", "+c).replaceAll(/^[,\s]*|[,\s]*$/,"")
      })

      if(sameAddr){
        eArr.add(currentAddr);
      }else{
        currentAddr = sanitize(postAddressFix(addrArr[id]))
        eArr.add(currentAddr);
      }
    }

    if(id < penaltyArr.size()){
      eArr.add(penaltyArr[id]);
    }else{
      eArr.add(currentPenalty)
    }

    result.add(eArr);
  }
  Cpy_ny_gov_global.lastAddr = currentAddr;

  //name concat which start with alias keyword
  def resArrSize = result.size();

  for( int resNameId = 1;resNameId<resArrSize;resNameId++){
    if(result[resNameId][0] =~ /(?i)^-alias-/||result[resNameId-1][0] =~ /(?i)-alias-\s*$/){
      result[resNameId-1][0] = result[resNameId-1][0]+result[resNameId][0]

      if(!(result[resNameId][1].equals(result[resNameId-1][1]))){
        result[resNameId-1][1] = result[resNameId-1][1]+";"+result[resNameId][1]
      }

      result.remove(resNameId)
      resArrSize = result.size();
      resNameId--;
    }
  }
  return result;
}

def postNameFix(def name){
  name = name.replaceAll(/(?i)\bof\b\s*$/, '')//remove ending mis-words
      .replaceAll(/^-NEW-\s*|-NEW-?\s*$/, '')
      .replaceAll(/(?i)(?<=health|agency|Tours)\s+(?=inc)/, ', ')
      .replaceAll(/(?i)inc$/, "Inc.")
      .replaceAll(/(?i)^(agency|Albright|Associates|Brokers?)$/, '')
      .replaceAll(/(?i)^Agency,\s*Inc.$/,'')
      .replaceAll(/(?i)Serge Kichenama$/, "Serge Kichenama and Associates")
      .replaceAll(/Martin, Lester I Brokerage/, "Martin, Lester Insurance Brokerage")
      .replaceAll(/Andy S./,"Andy S. Albright")
      .replaceAll(/(?i)David Peters Insurance$/, "David Peters Insurance Brokers")
      .replaceAll(/(?i)Press\s+Release.*?\d[\s-]+/, '')
      .replaceAll(/^[\s,;]*|[\s,&;]*$/, '')
      .replaceAll(/^\bA\b\s+Mutual.*/, '')
      .replaceAll(/\band\b\s*$/, '')
      .replaceAll(/(?i)^(?>Sublicensee\b.*|Agent)$/, '')//defines mal-blocks
      .replaceAll(/(?i)^l$/,"")  //check this later
      .replaceAll(/(?i)(?:each, by and through|individually)\s*$/,"")
      .replaceAll(/(?i)Preferred Auto Insurance$/,"Preferred Auto Insurance Agency, Inc.")
      .replaceAll(/(?i)Employers Insurance of Wausau$/,"Employers Insurance of Wausau, A Mutual Company")
      .replaceAll(/(?i)Foster Niagara Insurance$/,"Foster Niagara Insurance Agency, Inc.")
      .replaceAll(/(?i)^Kurbatsky Insurance$/,"Boris Kurbatsky Insurance")
      .replaceAll(/(?i)^Herman J. Sapio Insurance Service$/,"Herman J. Sapio Insurance Service of Western New York")
      .replaceAll(/(?i)New York Erminio J. Sapio/,"Erminio J. Sapio")
      .replaceAll(/(?i)^\b(?:and|inc|broker\b)\b/,"")
      .replaceAll(/^(?i)United\s*Healthcare/, "UnitedHealthcare")
      .replaceAll(/(?i)(?<=\b(?:Attaullah|New York)\b)\./,"")
      .replaceAll(/(?i)General Security Property and Casualty Insurance Company/,"General Security and Property Casualty Insurance Company")
      .replaceAll(/(?i)United States Fidelity & Guaranty Company/,"United States Fidelity and Guaranty Company")
      .replaceAll(/(?i)has\s+been$/,"")
      .replaceAll(/(?i).*Licenses.*/, '')

  return name;
}

def postAddressFix(def addr){
  addr = addr.replaceAll(/-NEW-/,'')
      .replaceAll(/^[,\s]+/,'')
      .replaceAll(/,+/,',')
      .replaceAll(/(?<=\d)\s*,\s*\b((?>th|rd|st|nd))\b[,\s]*/,'$1 ')
      .replaceAll(/(?s)\s+/, ' ')
      .replaceAll(/(?i)\bAgent\b.*/, '')

  return addr;
}

def personNameReformat(name){
  //Regroup person name by comma

  def exToken = "(?:[js]r|I{2,3})"
  return name.replaceAll(/(?i)\s*(.*?)\s*,\s*\b((?:(?!${exToken}\b)[^,])+)s*(?:,\s*\b(${exToken})\b)?\s*$/, '$2 $1 $3').trim();
}

def createEntity(eArray, dateSrc, url, srcUrl){

  eArray.each{vArr->
	 // println vArr[0] + ":" + vArr[1] + ":" + vArr[2];
    def data
    data = vArr[0] =~ /^-License$/
    if(!data.find()) {

      def names = vArr[0].trim();
      //  if(names =~ /^-License$/)

      def addr = vArr[1].trim();
      if (!vArr[2]) {
        vArr[2] = '';
      }
      def penalty = vArr[2].trim();
      def date = dateFixerUtil(vArr[3] ? vArr[3] : dateSrc);

      names = preNameFixBeforeSplit(names)
      def nameList = names.split(/(?i)\s*-NEW-\s*/);
      nameList.each { nAlias ->
        def entity = null;
        def entityType = null;
        if (nAlias) {
          def nameAliasList = nAlias.split(/(?i)\s*-ALIAS-\s*/).collect({ postNameFix(it) });
          def name = nameAliasList[0];

          if (name) {
            def dedupAlias = ""

            entityType = detectEntityType(name);
            if (entityType.equals("O")) {
              //entity = context.findEntity(["name": name, "type": "O"]);
              def dOrg = name;
              def dedupOname = deDupOrgAliasMapping(name);
              if (dedupOname) {
                name = dedupOname;
              }
              if (name) {
                entity = detectDupOrgEntity(name, entityType, date, url, srcUrl, addr, penalty)
                createEntityCore(entity, entityType, date, url, srcUrl, addr, penalty);
              }
              if (dedupOname) {
                addIgnoredCaseAlias(dOrg, entity);
              }
            }

            if (entityType.equals("P")) {
              name = personNameReformat(name);
              name = sanitize(personNameFix(name));
              def dedupPname = deDupPersonValue(name);

              if (dedupPname) {
                dedupAlias = name;
                name = dedupPname;
              }
              entity = Cpy_ny_gov_global.dupPersonMap[name];
              if (entity) {
                createEntityCore(entity, entityType, date, url, srcUrl, addr, penalty);
              }
            }

            if (!entity && name) {
              entity = context.getSession().newEntity();
              entity.setName(name);
              entity.type = entityType;

              if (entityType.equals("P")) {
                Cpy_ny_gov_global.dupPersonMap[name] = entity;
              }

              createEntityCore(entity, entityType, date, url, srcUrl, addr, penalty);
            }

            if (dedupAlias) {
              //entity.addAlias(dedupAlias);
              addIgnoredCaseAlias(dedupAlias, entity);
            }

            //Different type alias detector
            def aliasSize = nameAliasList.size();
            (1..<aliasSize).each { aliasId ->
              def alias = nameAliasList[aliasId].trim();
              if (alias) {
                def aliasType = detectEntityType(alias);
                if (entityType.equals(aliasType)) {
                  // entity.addAlias(alias);
                  addIgnoredCaseAlias(alias, entity);
                } else {
                  def assoAlias = ""
                  entity.addAssociation(alias);
                  //create new entity with alias
                  def dedupOname = deDupOrgAliasMapping(alias);
                  if (dedupOname) {
                    assoAlias = alias
                    alias = dedupOname;
                  }

                  entity = detectDupOrgEntity(alias, aliasType, date, url, srcUrl, addr, penalty)
                  if (dedupOname) {
                    //entity.addAlias(assoAlias);
                    addIgnoredCaseAlias(assoAlias, entity);
                  }
                  createEntityCore(entity, aliasType, date, url, srcUrl, addr, penalty);
                }
              }
            }
          }
        }
      }
    }
  }
}

def personNameFix(def name){
  name = name.replaceAll(/(?i)Angela Osan/,"Angela M. Osan")
      .replaceAll(/(?i)Lewis, Thomas J. III$/, "Thomas J. Lewis III")
      .replaceAll(/(?i)Campo, John G., Jr.$/, "John G. Campo, Jr.")
      .replaceAll(/(?i)Lawley, William J. Jr.$/, "William J. Lawley Jr.")
      .replaceAll(/^(?i)broker/, "")
      .replaceAll(/(?i)andColeman Elliott\b/, "Coleman Elliott")
      .replaceAll(/^\.$/, "")

  return name
}

def preNameFixBeforeSplit(def name){
  name = name.replaceAll(/(?i)Charbonneau, Laurie Collins, Stephanie R./, "Laurie Charbonneau-NEW-Stephanie R. Collins")
      .replaceAll(/^(?i)inc\.$/, "Robertson Taylor (North America) Inc.")
      .replaceAll(/(?i)Vytra Healthcare Long Island/, "Vytra Healthcare of Long Island")
      .replaceAll(/(?i)UnitedHealthcare Insurance Company of New York UnitedHealthcare of New York Inc./,"UnitedHealthcare Insurance Company of New York-NEW-UnitedHealthcare of New York Inc.")
      .replaceAll(/(?i)Co\.Stuart/,"Co.-NEW-Stuart")

  return name
}

def addAddress(String addr){
  ScrapeAddress pAddr = new ScrapeAddress();
  pAddr.setCountry("UNITED STATES");
  // pAddr.setProvince("NY");
  def address = addr=~ /(.+?)\s*,\s*([^\d,]+)[,\s]*([A-Z]{2}|)[,\s]*(\d*)\s*$/;

  if(!address){
    address = addr=~ /(.*?)[,\s]*([^\d,]+)[\s,]+([A-Z]{2})[\s,]+([-\d]+)\s*$/
  }else{
    address = addr=~ /(.+?)\s*,\s*([^\d,]+)[,\s]*([A-Z]{2}|)[,\s]*(\d*)\s*$/;//again: change it later
  }

  if(address){
    street = address[0][1].trim();
    city = address[0][2].trim();
    state = address[0][3].trim();
    postal = address[0][4].trim();

    if(state){
      pAddr.setProvince(state);
    }
    if(city){
      pAddr.setCity(city);
    }
    if(street){
      pAddr.setAddress1(street);
    }
    if(postal){
      pAddr.setPostalCode(postal);
    }
  }else{
    if(addr){
      pAddr.setAddress1(sanitize(addr));
    }
  }
  return pAddr
}

def createEntityCore(def entity,entityType ,date, url, srcUrl,addr,penalty){
  def pAddr = addAddress(addr);
  entity.addAddress(pAddr);
  if(penalty){
  entity.addRemark("Penalty: "+penalty.replaceAll(/&nbsp;/, " "));
  }

  event = entity.newEvent();
  event.setDate(date.trim());
  event.setDescription("This entity appears on the New York State Insurance Department list of Disciplinary Actions");
  entity.addUrl(url);

  List srcs = entity.getSources();
  def srcFound = false;
  for(ScrapeSource src : srcs){
    if(src.getUrl().equals(srcUrl)){
      srcFound = true;
      break;
    }
  }
  if(!srcFound){
    def nSrc = entity.newSource()
    nSrc.setUrl(srcUrl);
    nSrc.setName("Disciplinary Actions");
    nSrc.setDescription("Disciplinary Actions taken by the New York State Insurance Department and New York State Department of Financial Services");
  }
}

def aliasFixer(text){
  //alias recognizer token
  text = text.replaceAll(/(?i)\b(?>[dt][\/\.]b[\/\.]?a[\/\.]?|[fa][\/\.]k[\/\.]?a[\/\.]?)/, "-ALIAS-")
  text = text.replaceAll(/(?i)\b(known as|formerly)\b/, "-ALIAS-")
  text = text.replaceAll(/(?i)\btba\b/, "-ALIAS-")
  return text;
}

def sanitize(name){
  return name.replaceAll(/\\n/,"")
      .replaceAll(/<[^>]+>/,"")
      .replaceAll(/(?s)\s+/," ")
      .replaceAll(/&amp;/, '&')
      .replaceAll(/&#146;/, "'")
      .replaceAll(/&#\d+;/, '')
      .replaceAll(/&quot;/, "'")
      .trim();
}

def detectEntityType(name){
  def entityType = context.determineEntityType(name);
  if(entityType.equals("P")){
    type = name =~ /(?i)\b(A(?>merichoice|gency|surion)|B(?>onds?|rokerage|ureau)|C(?>DPHP|IGNA|ommunity|H(?>P|emPlan))|E(?>xcellus|nterprise|mpire)|F(?>MG)|GHI|H(?>IP|ealth)|Insurance|Life|M(?>i(?>ssissippi|innesota)|DNY)|N(?>YLCare|etworks)|Oxford|P(?>HS|rudential)|SITY|Travel|U(?>ni(?>vera|care)|SA)|Vytra|Wellcare|York)/;

    if(type){
      entityType = "O";
    }

    if(name =~ /(?i)\b(?:Axiom|New Jersey|Discount)\b/){
      entityType = "O";
    }
  }

  if(entityType.equals("O")){
    if(name =~ /(?i)\b(?:Elizabeth|bond)\b/){
      entityType = "P";
    }
  }

  return entityType;
}

def deDupPersonValue(name){
  if(name =~ /(?i)Anthony\b Rodriquez/)
  {
    return "Anthony Rodriguez";
  }
}

def deDupOrgAliasMapping(name){
  if(name =~ /(?i)Rinehart Insurance/)
  {
    return "Rinehart Agency";
  }
  if(name =~ /(?i)Royal Indemnity Company/)
  {
    return "Royal Insurance Co. of America";
  }
  if(name =~ /(?i)Suffolk Insurance/)
  {
    return "Suffolk Agency, Inc.";
  }
  if(name =~ /(?i)Transamerica Life Insurance Company of New York/)
  {
    return "Transamerica Financial Life Insurance Company";
  }
  if(name =~ /(?i)BiCounty Brokerage South Inc/)
  {
    return "BiCounty Brokerage Corp.";
  }
  if(name =~ /(?i)Response Indemnity Company/)
  {
    return "Response Insurance Company";
  }
  if(name =~ /(?i)Oxford Health (?:Insurance|plans)[\W\s]*(?:of New York)?[\W\s]*Inc/)
  {
    return "Oxford Health Plans (NY), Inc.";
  }
  if(name =~ /(?i)General Security Insurance Company/)
  {
    return "General Security and Property Casualty Insurance Company";
  }
  if(name =~ /(?i)CIGNA (?:Life Insurance Company|Healthcare)\s*of\s*(?:New York|ny)[\W\s]*(inc)?/)
  {
    return "CIGNA Healthcare of New York Inc.";
  }
  if(name =~ /(?i)Blue Ridge Insurance Companies/)
  {
    return "Blue Ridge Insurance Company";
  }
  if(name =~ /(?i)Acordia of Ohio/)
  {
    return "Wells Fargo Insurance Services of Ohio LLC";
  }
  if(name =~ /(?i)York International Agency LLC/)
  {
    return "York International Agency, Inc.";
  }
  if(name =~ /(?i)UnitedHealthcare of New York, Inc./)
  {
    return "UnitedHealthcare Insurance Company of New York";
  }

}

def detectDupOrgEntity(name,entityType,date,url,srcUrl,addr,penalty){
  def entity;
  def alias;

  if(Cpy_ny_gov_global.globalOrgMap.size()>0){

    //here \W also includes "&"; "(?<=\b\S+)s\b"->remove plural form
    def conflictFreeTokens = /(?i)\b(?:Community Blue\b|limited|ltd|com?(?:pany)?|and|Ind(?:ustr(?:ies|y))?)\b|\W|inc|corp(oration)?|(?<=\b\S+)s\b/
    //optionalPairTokens ares optional and will vary among scripts
    def optionalPairTokens = /(?i)\b(?:investments?|New York|NY|New Jersey|NJ|Insurance|Indemnity|ins|Plan|Services)\b/

    for (Entry entry : Cpy_ny_gov_global.globalOrgMap.entrySet()) {
      def k = entry.getKey();
      def v = entry.getValue();

      if(k.equalsIgnoreCase(name)){
        return v
      }else{
        def kr = k.replaceAll(/${optionalPairTokens}/, "X").replaceAll(/${conflictFreeTokens}/,'')
        def nr = name.replaceAll(/${optionalPairTokens}/, "X").replaceAll(/${conflictFreeTokens}/,'')
        if(kr.equalsIgnoreCase(nr)){
          if(k.length() < name.length()){
            v.name = name
            alias = k;
            entity = v;
            Cpy_ny_gov_global.globalOrgMap.remove(k);
            Cpy_ny_gov_global.globalOrgMap[name] = v;
            break;
          }
          else{
            alias = name;
            entity = v;
            break;
          }
        }
      }
    }

    if(alias){
      return addIgnoredCaseAlias(alias,entity);
    }
  }

  entity = context.getSession().newEntity();
  entity.setName(sanitize(name));
  entity.type = "O";
  Cpy_ny_gov_global.globalOrgMap[name] = entity;

  return entity;
}

def addIgnoredCaseAlias(def alias, def entity){
  def aliasList = entity.getAliases();

  for(String aliasName:aliasList){
    if(aliasName.equalsIgnoreCase(alias) || alias.equalsIgnoreCase(entity.name)){
      return entity;
    }
  }
  entity.addAlias(postAliasFix(alias))

  return entity;
}

def postAliasFix(alias){
  alias = alias.replaceAll(/(?i)^Acordia of Ohio$/,"Acordia of Ohio LLC")
      .replaceAll(/(?i)^Suffolk Insurance$/,"Suffolk Insurance Agency")

  return alias
}

def dateFixerUtil(String dateStr){
  //dateStr must contain real date witch should match month/day/year format
	if(dateStr.indexOf("/") <0) {
		dateStr = dateStr.substring(4, 6) + "/" + dateStr.substring(6, 8) + "/" + dateStr.substring(0, 4);
	}
	
	
  dateStr = dateStr.replaceAll(/[-\/\.]+/,'/').replaceAll(/(?s)\s+/,'').replaceAll(/^(\d*?)\/?(\d*?)\/?((?:\b\d+\b)?)$/, { a,m,d,y->
    def cal = Calendar.getInstance()

    m = m?Integer.parseInt(m):0;
    d = d?Integer.parseInt(d):0;
    y = y?Integer.parseInt(y):0;

    int cy = cal.get(Calendar.YEAR);
    boolean yCurrent = false;
    boolean mCurrent = false;

    //year fixer
    if(y>=cy){
      y = cy;
      yCurrent = true;
    }else if(y<1990){
      def y2 = y/10;
      if(y2>99){
        //not 2digit but less than 1990
        y = 1990;
      }else{
        //2digit fixer
        if(y>89){
          //<1990 and >89 and grater than current 2digit year
          y = 1990;
        }else if(y>=(cy%100)){
          //grater than eq current 2digit year
          y = cy;
          yCurrent = true;
        }else if(y<10){
          y = "200"+y;
        }else{
          y = "20"+y;
        }
      }
    }

    //month fixer
    if(m<13 && m>0){
      if(yCurrent){
        int cm = cal.get(Calendar.MONTH) + 1;
        if(m>=cm){
          m = cm;
          mCurrent = true;
        }
      }
      if(m<10){
        m = "0"+m;
      }
    }else{
      m = "01";
    }

    //day fixer
    if(d<32 && d>0){
      if(yCurrent && mCurrent){
        int cd = cal.get(Calendar.DAY_OF_MONTH);
        if(d>=cd){
          d = cd-1;
        }
      }
      if(d<10){
        d = "0"+d;
      }
    }else{
      d = "01"
    }

    return m+"/"+d+"/"+y;
  });

  return dateStr;
}