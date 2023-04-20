package com.se.rdc.utils

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity

import java.util.zip.ZipInputStream

/**
 * Find out unicode code points for all chars in a given string
 */
def analyzeUnicodeCodePoints(def str) {
  System.out.println("\n==== Unicode code points analysis start ==")
  def blockMatcher = str =~ /(\S++|\s++)/

  while (blockMatcher.find()) {
    def block = blockMatcher.group(1)
    char[] ch = block.toCharArray()
    def finalUnicodeStr = "["

    for (char c : ch) {
      String val

      if (c != "\n") {
        int i = (int) c
        val = Integer.toHexString(i)
        if (val.length() == 2) {
          val = "00" + val
        } else if (val.length() == 3) {
          val = "0" + val
        } else if (val.length() == 1) {
          val = "000" + val
        }
        val = "\\u" + val

      } else {
        val = "\\n"
      }

      finalUnicodeStr += val + ","
    }
    finalUnicodeStr = block + finalUnicodeStr.replaceAll(/,$/, "]")
    if (block =~ /\n/) {
      finalUnicodeStr += "\n"
    }
    print(finalUnicodeStr)
  }

  System.out.println("\n==== Unicode code points analysis end ==")
}
/*
 * Returns unzipped files list after unzipping
 */

def unzip(String zipUrl, File sourceZipFile = null, File destinationDir = null) {
  if (!sourceZipFile) {
    sourceZipFile = new File(zipUrl.replaceAll(/.*\/([^\/]+)/, '$1'))
  }

  if (!sourceZipFile.exists()) {
    def zip_data = ((ScrapianContext) context).invokeBinary([url: zipUrl, clean: false]).getValue()
    sourceZipFile.withOutputStream {ostream -> ostream.write(zip_data)}
    zip_data = null
  }

  if (!destinationDir) {
    def parent = sourceZipFile.getAbsolutePath().replaceAll(/[^\/\\]+$/, '')
    destinationDir = new File(parent)
  }

  def unzippedFiles = []

  final zipInput = new ZipInputStream(new FileInputStream(sourceZipFile))
  zipInput.withStream {
    def entry
    while (entry = zipInput.nextEntry) {
      if (!entry.isDirectory()) {
        final file = new File(destinationDir, entry.name)
        file.parentFile?.mkdirs()

        def output = new FileOutputStream(file)
        output.withStream {output << zipInput}

        unzippedFiles << file
      } else {
        final dir = new File(destinationDir, entry.name)
        dir.mkdirs()

        unzippedFiles << dir
      }
    }
  }
  return unzippedFiles
}

/**
 * Store object in cache for later usage
 * */
def cachedData(def key, def value = null) {
  //This method or it's caller can be safely deleted in production environment
  try {
    //if value is not present then it will only get otherwise it wll put into cache
    if (!value) {
      return context.cached_data.get(key)
      //if key doesn't exist, it'll return "null"
    } else {
      context.cached_data.put(key, value)
      return true
    }
  } catch (e) {
  }

  return null
}

/**
 * Common error logger for groovy scripts
 * */
def errorsLogger(Exception e, Object... indicatorParams) {
  def errMapp = [:]
  def errMatch = e.getStackTrace() =~ /(?:\bt\.[^\(]+\((\w+\.java)|(\w+\.groovy)):(\d+)/

  while (errMatch.find()) {
    def key = errMatch.group(1) ? errMatch.group(1) : errMatch.group(2)
    def val = errMapp[key]
    if (val) {
      val.add(errMatch.group(3))
    } else {
      errMapp[key] = [errMatch.group(3)]
    }
  }
  def msg = indicatorParams.toString() + " => " + e.getMessage() + " : " + errMapp.toString()
  try {
    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("errors.txt", true)))
    out.println(msg)
    out.close()
  } catch (Exception ex) {
    //exception handling left as an exercise for the reader
  }
}

/**
 * only Address "match by relevance" is added
 * addrMatchMap = [relv:80, retain:true, val: ScrapianAddressObj, attr:["street", "city",..]] OR null
 * */
def detectDupPerson(def name, def addrMatchMap) {

  List entityList = context.getSession().getEntities()
  for (ScrapeEntity scrapeEntity : entityList) {
    boolean match = false

    if (scrapeEntity.getType().equals("P") && scrapeEntity.getName().equalsIgnoreCase(name)) {

      //address checking
      if (addrMatchMap) {
        List addrList = scrapeEntity.getAddresses()

        if (addrList.contains(addrMatchMap["val"])) {
          match = true
        } else {
          def inStr = ""
          def inObj = addrMatchMap["val"]
          addrMatchMap["attr"].each {
            if (it.equals("street")) {
              inStr += inObj.getAddress1() + " "
            } else if (it.equals("city")) {
              inStr += inObj.getCity() + " "
            }
          }

          for (ScrapeAddress addrObj : addrList) {
            def cAddr = ""
            addrMatchMap["attr"].each {
              if (it.equals("street")) {
                cAddr += addrObj.getAddress1() + " "
              } else if (it.equals("city")) {
                cAddr += addrObj.getCity() + " "
              }
            }
            if (stringsWordsRelevance(inStr, cAddr) >= addrMatchMap["relv"]) {
              match = true
              if (addrMatchMap["retain"]) {
                scrapeEntity.addAddress(addrMatchMap["val"])
              }
              break
            }
          }
        }
      }
      if (match) {
        return scrapeEntity
      }
    }
  }
  return null
}

/**
 * Compare two strings and find out their words relevance
 **/
def stringsWordsRelevance(def sStr, def bStr) {
  // Return percentage value. ie. 80%
  sStr = sStr.trim()
  bStr = bStr.trim()

  if (sStr.length() > bStr.length()) {
    def tmp = sStr
    sStr = bStr
    bStr = tmp
  }

  sStr = sStr.split(/\s+/) as List
  bStr = bStr.split(/\s+/) as List
  int count = 0

  sStr.each {
    int bLen = bStr.size()
    for (int i = 0; i < bLen; i++) {
      if (it.equalsIgnoreCase(bStr[i])) {
        count++
        bStr.remove(i)
        break
      }
    }
  }

  return (int) (count * 100 / sStr.size())
}

/**
 * Month name to decimal converter
 * @param monthName
 * @return month decimal no
 */
def getMonthNo(def monthName) {
  if (monthName =~ /(?i)\bjan(?:uary)?\b/) {
    return "01"
  } else if (monthName =~ /(?i)\bfeb(?:ruary)?\b/) {
    return "02"
  } else if (monthName =~ /(?i)\bmar(?:ch)?\b/) {
    return "03"
  } else if (monthName =~ /(?i)\bapr(?:il)?\b/) {
    return "04"
  } else if (monthName =~ /(?i)\bmay\b/) {
    return "05"
  } else if (monthName =~ /(?i)\bjune?\b/) {
    return "06"
  } else if (monthName =~ /(?i)\bjuly?\b/) {
    return "07"
  } else if (monthName =~ /(?i)\baug(?:ust)?\b/) {
    return "08"
  } else if (monthName =~ /(?i)\bsep(?:tember)?\b/) {
    return "09"
  } else if (monthName =~ /(?i)\boct(?:ober)?\b/) {
    return "10"
  } else if (monthName =~ /(?i)\bnov(?:ember)?\b/) {
    return "11"
  } else if (monthName =~ /(?i)\bdec(?:ember)?\b/) {
    return "12"
  }
}

/**
 * Fix any sort of date format including last date fixing 2 methods.
 * This should be a complete fixing method.
 * in: invalid month|day/valid year
 * Out: MM/DD/YYYY
 * */
def dateFixerUtil(String dateStr) {
  //dateStr must contain real date witch should match mnth/day/year format
  dateStr = dateStr.replaceAll(/[-\/\.]+/, '/').replaceAll(/(?s)\s+/, '').replaceAll(/^(\d*?)\/?(\d*?)\/?((?:\b\d+\b)?)$/, {a, m, d, y ->
    def cal = Calendar.getInstance()

    m = m ? Integer.parseInt(m) : 0
    d = d ? Integer.parseInt(d) : 0
    y = y ? Integer.parseInt(y) : 0

    int cy = cal.get(Calendar.YEAR)
    boolean yCurrent = false
    boolean mCurrent = false

    //year fixer
    if (y >= cy) {
      y = cy
      yCurrent = true
    } else if (y < 1990) {
      def y2 = y / 10
      if (y2 > 99) {
        //not 2digit but less than 1990
        y = 1990
      } else {
        //2digit fixer
        if (y > 89) {
          //<1990 and >89 and grater than current 2digit year
          y = 1990
        } else if (y >= (cy % 100)) {
          //grater than eq current 2digit year
          y = cy
          yCurrent = true
        } else if (y < 10) {
          y = "200" + y
        } else {
          y = "20" + y
        }
      }
    }

    //month fixer
    if (m < 13 && m > 0) {
      if (yCurrent) {
        int cm = cal.get(Calendar.MONTH) + 1
        if (m >= cm) {
          m = cm
          mCurrent = true
        }
      }
      if (m < 10) {
        m = "0" + m
      }
    } else {
      m = "01"
    }

    //day fixer
    if (d < 32 && d > 0) {
      if (yCurrent && mCurrent) {
        int cd = cal.get(Calendar.DAY_OF_MONTH)
        if (d >= cd) {
          d = cd - 1
        }
      }
      if (d < 10) {
        d = "0" + d
      }
    } else {
      d = "01"
    }

    return m + "/" + d + "/" + y
  })
  return dateStr
}

/**
 * param: age, ie 66 years to MM/DD/yyyy
 */
def convertAgeToDate(age, circa = true, endingYear = 0) {
  Calendar c = Calendar.getInstance()
  def m, d
  if (circa) {
    m = c.get(Calendar.MONTH) + 1
    d = c.get(Calendar.DATE)
  } else {
    m = d = "-"
  }

  return m + "/" + d + "/" + ((endingYear == 0 ? c.get(Calendar.YEAR) : endingYear as Integer) - (age as Integer))
}

/**
 * Just for input/teaxt area tag's value parsing 
 * */
def inputValueParser(def html, def key) {
  //use HtmlTokenParser class
}

/**
 * Common name-vale parser for scrapian scripts
 * example: ice_most_wanted
 * startKeyName|endtKeyName : ie. "gender"
 * html : source 
 * isSanitize=true : whether "<tags>" should be removed
 * brSplit: Whether to split <br> tags
 * Important: make custom sanitize method
 * */
def nameValueParser(html, startKeyName, endKeyName = "", brSplit = false, isSanitize = true) {
  //return [value, remaining html] or [html] if no match

  def regex
  if (endKeyName) {
    //only this pattern should suffice, but this is bit slow compare to the next one
    regex = /(?is)${startKeyName}[^>]+>(.*?)(?=${endKeyName})(.*)/
  } else {
    regex = /(?is)${startKeyName}[^<]*(?:<[^<>]+>\s*(?=<))+<[^<>]+>([^<]+)(.*)/
  }

  def data = html =~ regex
  if (data) {
    def val = sanitize(data[0][1])
    if (brSplit) {
      val = val.split(/\s*<[^\w>]*br[^\w>]*>\s*/).findAll {
        if (isSanitize) {
          it = sanitize(it.replaceAll(/\s*<[^<>]+>\s*/, ''))
        }
        if (it) {
          return true
        }
      }
    } else if (isSanitize) {
      val = sanitize(val.replaceAll(/\s*<[^<>]+>\s*/, ''))
    } else {
      val = val.trim()
    }

    return [val, data[0][2]]
  }
  println regex
  println html
  throw new Exception("No match found for: " + startKeyName)
}

def sanitize(data) {
  //sample sanitize method
  return data.replaceAll(/(?s)\s+/, ' ').trim()
}

/**
 * Usage:
 * -----
 * Globar var: orgEntityAlias = []
 *
 * In create entries:
 //Merging duplicate org entries
 * */
//	def dupOrg = getDupEntityNameAlias(name, aliasList, orgEntityAlias)
//	if (dupOrg.size() > 0) {
//		entity = context.findEntity(["name": dupOrg[0], "type": entity_type]);
//		aliasList = dupOrg[1];
//		if (dupOrg[2]) {
//			entity.addAlias(dupOrg[0]);
//			entity.name = name;
//		}
//	} else { entity = context.findEntity(["name": name, "type": entity_type]); }
//	if (entity == null) {
//		entity = context.session.newEntity();
//		entity.type = entity_type;
//		entity.name = name;
//	}
//	aliasList.each { entity.addAlias(it); }

/**
 * Parse tags that ends in ":"
 * Define a global list as "tagList = [] as List;"
 * @param regex : with one single group. i.e: /<td[^<>]+>\s*([^<\n]+?)\s*:/
 * @return
 */
def parseUniqueTag(html, regex, title = "====", printActive = true) {
  synchronized (this) {
    if (printActive) {
      println "====" + title + "====="
    }
    text = html =~ regex
    (0..<text.count).each {
      if (!tagList.contains(text[it][1])) {
        tagList.add(text[it][1])
        if (printActive) {
          println text[it][1]
        }
      }
    }
  }
}

def pdfToTextConverterOld(pdfUrl) {
  def pdfFile = invoke(pdfUrl, false, true)

  def pmap = [:] as Map
  pmap.put("1", "-layout")
  pmap.put("2", "-enc")
  pmap.put("3", "UTF-8")
  pmap.put("4", "-eol")
  pmap.put("5", "dos")
  //pmap.put("6", "-raw")

  def pdfText = context.transformPdfToText(pdfFile, pmap)

  //TODO: if pdf contains images then use following block otherwise remove this
  if (!pdfText.value.trim()) {
    def tmpFile = new File(System.getProperty("java.io.tmpdir"), pdfUrl.replaceAll(/[:\/]/, "_"))
    if (!tmpFile.exists()) {
      if (!tmpFile.parentFile.exists()) {
        tmpFile.parentFile.mkdirs()
      }
      tmpFile.withOutputStream {it.write(pdfFile.value)}
    }
    pdfText = imageToTextConverter.getText(tmpFile)
  }

  return pdfText.toString().replaceAll(/\r\n/, "\n")
}