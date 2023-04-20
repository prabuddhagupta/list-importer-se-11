package current_script

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.request.HttpInvoker
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.compress.utils.IOUtils
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

import java.text.SimpleDateFormat

/**
 * Date: 07/19/2018
 * */


context.setup([connectionTimeout: 20000, socketTimeout: 25000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

India_irda script = new India_irda(context)
script.initParsing()

//-----Debug area starts------------//
//script.handleDetailsPage("https://www.irdai.gov.in/ADMINCMS/cms/frmGeneral_Layout.aspx?page=PageNo2395","https://www.irdai.gov.in/ADMINCMS/cms/Uploadedfiles/warning letter to Star Health.pdf")
//-----Debug area ends---------------//

class India_irda {
  final ScrapianContext context

  final String root = "https://www.irdai.gov.in"
  final String url = root + "/ADMINCMS/cms/frmGeneral_List.aspx?DF=WAP&mid=30.1"


  India_irda(context) {
    this.context = context
  }

//------------------------------Initial part----------------------//
  def initParsing() {
    def html = invoke(url, false)

    def indexUrlMatch = html =~ /(?i)"UCMenu1_Menu1-menuItem015.*?location.href='([^']+)'/
    while (indexUrlMatch.find()) {
      def indexUrl = sanitize(root + indexUrlMatch.group(1))
      html = invoke(indexUrl, false)
      def paramMap = getParams(html)
      if (html =~ /(?i)incorrect syntax near the keyword/) {
        return
      }
      html = invokePost(indexUrl, paramMap)
      html = sanitize(html)
      def yearMatch = html =~ /(?is)<tr>\n<td>\d+\-\d+\-\d+.*?<\/tr>/
      while (yearMatch.find()) {
        def year, detailsUrl, pdfUrl
        year = yearMatch.group(0)
        def detailsUrlMatch = year =~ /(?i)'([^']+)'>Preview/
        if (detailsUrlMatch) {
          detailsUrl = root + '/ADMINCMS/cms/' + detailsUrlMatch.group(1)
        }

        def pdfUrlMatch = year =~ /(?i)window\.open\('([^']+pdf)'\)/
        if (pdfUrlMatch) {
          pdfUrl = "https://www.irdai.gov.in/ADMINCMS/cms/" + pdfUrlMatch[0][1]
        }

        handleDetailsPage(detailsUrl, pdfUrl)


      }
    }
  }

  def handleDetailsPage(srcUrl, pdfUrl) {
    def name, nameMatch, date;
    def html = invoke(srcUrl)
    html = sanitize(html)

    if ((nameMatch = html =~ /(?is)>\s*in\s*(?:the)?\s*matter of\s+([^<\n]+)</)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\/s\.?/, "").trim()

    } else if ((nameMatch = html =~ /(?i)^\s*(?:final order\s+)?in\s*(?:the)?\s*matter\s*of\s*((\w+[^\n]+))\n/)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\\/s\.?/, "").trim()
    } else if ((nameMatch = html =~ /(?i)>final\s+order\s+in\s+the\s+matter\s+of([^<]+)<\//)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\\/s\.?/, "").trim()

    } else if ((nameMatch = html =~ /(?i)<title>\s*(?:warning|modification)\s*(?:-|\()(.+)\)*\n/)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\\/s\.?/, "").trim()
    } else if ((nameMatch = html =~ /(?i)(?:officer|ceo|director)[\s,]*(?:<[^>]+>)+(?:\s*<[^>]+>\s*)+([^<]+)</)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\\/s\.?/, "").trim()
    } else if ((nameMatch = html =~ /(?is)<title>\n((?!(?:final order|levy of|warning|delay|Directions u\/s))\w+\s+[^\n]+\S+)\n<\/title>/)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\\/s\.?/, "").trim()
    } else if ((nameMatch = html =~ /(?i)<td class="layouttitle" align="center">([^<]+)</)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\\/s\.?/, "").trim()
    }


    if (name =~ /(?i)violation/) {
      /*ImageToTextConverter img = new ImageToTextConverter();
    pdfUrl = pdfUrl.trim().replaceAll(/\s/,"%20")
    def text = img.getText(pdfUrl)*/

      nameMatch = html =~ /(?i)<td class="notetext">\s*<div style[^>]+>(?:<[^>]+>\s*)+[^<]+(?:<[^>]+>\s*)+[^<]+(?:<[^>]+>\s*)+([^<]+)/
      //(?i)<td class="notetext">\s*<div style[^>]+>(?:<[^>]+>\s*)+[^<]+(?:<[^>]+>\s*)+[^<]+(?:<[^>]+>\s*)+([^<]+)
      if (nameMatch) {
        name = nameMatch[0][1]
      }
    }


    if ((!name || (name =~ /(?im)^\s*(?:imposing|Insurer warned|delay in|(?:Contravention|opening) of)/)) && pdfUrl) {
      pdfUrl = pdfUrl.trim().replaceAll(/\s/, "%20")
      pdfUrl = pdfUrl.replaceAll(/\.+/, ".")
      def pdfText = ""
      pdfText = pdfToTextConverter(pdfUrl)

      if (pdfText)
        name = getPdfName(pdfText)
    }

    def aliasList = []

    if (name) {
      aliasList = name.split(/(?i)(?:\(formerly|subsequently known as)/).collect({it ->
        return fixName(it)
      })

      name = aliasList[0]
      aliasList.remove(0)
    }

    def dateMatch = html =~ /(?i)<td align="right"><[^>]*>date:<[^>]*>([^<]+)/
    if (dateMatch) {
      date = dateMatch[0][1]
      date = date.replaceAll(/&nbsp;/, "")
    }

    // println name + "====" + date
    if (name)
      createEntity(name, aliasList, pdfUrl, date)
  }

  def getPdfName(def text) {
    //https://www.irdai.gov.in/ADMINCMS/cms/Uploadedfiles/ORD-NewIndia-250108.pdf
    def name
    def nameMatch = text =~ /(?im)^\s*(?:Final Order)?\s*[il]n\s+the\s+matter\s+of\s+([^\n]+)\n/
    if (nameMatch) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\/s\.?/, "").trim()
    } else if ((nameMatch = text =~ /(?i)^\s*in\s+the\s+matter\s+relating\s+to\s+([^\n]+)\n/)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\/s\.?/, "").trim()
    } else if ((nameMatch = text =~ /(?im)(?:ceo|director|officer|chairman|parasnis)\b[,\s]*\n\s*([^\n]+)/)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\/s\.?/, "").trim()
    } else if ((nameMatch = text =~ /(?im)^\s+against[\s\n]+([^\n]+)/)) {
      name = nameMatch[0][1]
      name = name.toString().replaceAll(/(?i)^m\\/s\.?/, "").trim()
    }

    return name

  }

  def getParams(html) {
    def param = [:]
    def viewState, eventTarget, eventArgument, eventValidation, hdmid, hdmImage;

    def viewStateMatch = html =~ /(?i)viewstate"\s*value="([^"]+)"/
    if (viewStateMatch) {
      viewState = viewStateMatch[0][1]
    }

    def eventTargetMatch = html =~ /(?i)__EVENTTARGET"\s*value="([^"]*)"/
    if (eventTargetMatch) {
      eventTarget = eventTargetMatch[0][1]
    }

    def eventArgumentMatch = html =~ /(?i)__EVENTARGUMENT"\s*value="([^"]*)"/
    if (eventArgumentMatch) {
      eventArgument = eventArgumentMatch[0][1]
    }

    def eventValidationMatch = html =~ /(?i)__EVENTVALIDATION"\s*value="([^"]*)"/
    if (eventValidationMatch) {
      eventValidation = eventValidationMatch[0][1]
    }

    def hdmidMatch = html =~ /(?i)Hdnmid".*?value="([^"]*)"/
    if (hdmidMatch) {
      hdmid = hdmidMatch[0][1]

    }

    def hdmImageMatch = html =~ /(?i)Hdm_img".*?value="([^"]*)"/
    if (hdmImageMatch) {
      hdmImage = hdmImageMatch[0][1]

    }

    param["__EVENTTARGET"] = "Ddl_Year"
    param["__EVENTARGUMENT"] = eventArgument
    param["__LASTFOCUS"] = ""
    param["__VIEWSTATE"] = viewState
    param["__EVENTVALIDATION"] = eventValidation
    param["UCTops1:Txt_Search"] = ""
    param["Hdnmid"] = hdmid
    param["Hdm_img"] = hdmImage
    param["Ddl_Year"] = "ALL"

    return param
  }

//------------------------------Filter part------------------------//
  def fixName(def name) {
    name = name.replaceAll(/(?i)in the matter of/, "")
    name = name.replaceAll(/(?i)Levy of penalty.*/, "")
    name = name.replaceAll(/(?i)Final Order in matter of/, "")
    name = name.replaceAll(/(?i)regarding.*/, "")
    name = name.replaceAll(/(?i)^\s*(?:order.*?against|the matter of|Penalty Order -|the\s*matter\s*of|corporate agent)/, "")
    name = name.replaceAll(/(?i)\(\s*(?:Ref|on complaint|\d+).*\)/, "")
    name = name.replaceAll(/(?i)expenses of Management.*/, "")
    name = name.replaceAll(/(?i)Imposing a penalty.*Insurance Act, 1938/, "")
    name = name.replaceAll(/(?i)revocation of.*?corporate agent/, "")
    name = name.replaceAll(/(?i)Directions under Section 34 of.*?\d+/, "")
    name = name.replaceAll(/(?i)Certificate of Registration.*/, "")
    name = name.replaceAll(/(?i)^(?:Personal hearing|order|Directions to)\s*/, "")
    name = name.replaceAll(/(?is),.*?floor/, "")
    name = name.replaceAll(/(?i)^m\\/s\.?/, "").trim()
    name = name.replaceAll(/(?i)^advertisement\s-/, "").trim()


    name = name.replaceAll(/<[^>]+>/, "")
    name = name.replaceAll(/\([^\)]+\)/, "")

    name = name.replaceAll(/(?m)[,\s]*$/, "")

    return sanitize(name)

  }

//------------------------------Entity creation part---------------//
  def createEntity(name, aliasList, url, date) {
    if (name) {
      def entity = null
      entity = context.findEntity([name: name, type: "O"])
      if (!entity) {
        entity = context.getSession().newEntity()

        entity.setName(sanitize(name))
        entity.type = "O"
      }
      if (aliasList.size() > 0) {
        aliasList.each {
          it = it.replaceAll(/\)/, "")
          entity.addAlias(it)
        }
      }

      ScrapeEvent event = new ScrapeEvent()
      def eDate = context.parseDate(new StringSource(date), ["dd-MM-yyyy"] as String[])
      if (eDate) {
        event.setDate(eDate)
      }
      event.setDescription("This entity appears on the India Insurance Regulatory and Development Authority list of Warnings and Penalties.")
      entity.addEvent(event)

      if (url)
        entity.addUrl(url)

      ScrapeAddress address = new ScrapeAddress()
      address.setCountry("India")
      entity.addAddress(address)
    }
  }

//------------------------------Misc utils part---------------------//
  def invoke(url, cache = true, tidy = false, headersMap = [:], miscData = [:]) {
    Map dataMap = [url: url, tidy: tidy, headers: headersMap, cache: cache]
    dataMap.putAll(miscData)
    return context.invoke(dataMap)
  }

  def invokePost(url, paramsMap, headersMap = [:], cache = true, tidy = false, miscData = [:]) {
    Map dataMap = [url: url, type: "POST", params: paramsMap, tidy: tidy, headers: headersMap, cache: cache]
    dataMap.putAll(miscData)
    return context.invoke(dataMap)
  }

  def invokeBinary(url, type = null, paramsMap = null, cache = true, clean = false, miscData = [:]) {
    //Default type is GET
    Map dataMap = [url: url, type: type, params: paramsMap, clean: clean, cache: cache]
    dataMap.putAll(miscData)
    return context.invokeBinary(dataMap)
  }

  def pdfToTextConverter(pdfUrl) {
    def pdfFile = invokeBinary(pdfUrl)
    if(pdfUrl =~ /India-070110.pdf/){
      println "dfdf"
    }

    def pmap = [:] as Map
    pmap.put("1", "-layout")
    pmap.put("2", "-enc")
    pmap.put("3", "UTF-8")
    pmap.put("4", "-eol")
    pmap.put("5", "dos")
    //pmap.put("6", "-raw")``````

    def pdfText = context.transformPdfToText(pdfFile, pmap)

    // pdfText = imageToTextConverter.getText(pdfUrl)
    if (!(pdfText =~ /\w+/)) {
      ImageToTextConverter img = new ImageToTextConverter();
      def tmpFile = new File(/*System.getProperty("java.io.tmpdir")*/ "./tmp", pdfUrl.replaceAll(/.*\/([^\/]+$)/, '$1'))
      if (!tmpFile.exists()) {
        tmpFile.parentFile.mkdirs()
        tmpFile.withOutputStream {it.write(pdfFile.value)}
      }
      pdfText = img.getText(tmpFile)
    }

    return pdfText.toString().replaceAll(/\r\n/, "\n")
  }

  def sanitize(data) {
    return data.toString().replaceAll(/&amp;/, '&').replaceAll(/\r\n/, "\n").replaceAll(/\s{2,}/, " ").trim()
  }

  class ImageToTextConverter {
    private final rootUrl = "https://www.onlineocr.net"
    private final uploadUrl = "https://www.onlineocr.net/FileHandler.ashx"
    private final context

    ImageToTextConverter(context = null) {
      this.context = context
    }

    String getText(String dataUrl, cacheEnabled = true) {
      def data
      if (cacheEnabled && (data = cachedData(dataUrl))) {
        context.info("Loading from cache: " + dataUrl)
        return data
      }

      def srcUrlMatch, name
      def hpi = new HttpInvoker(dataUrl)
      if ((srcUrlMatch = dataUrl =~ /(?i)([^\/\\]+\.(?:pdf|png|jpg|BMP|TIFF|GIF))$/)) {
        name = srcUrlMatch[0][1].toString().trim()
        data = hpi.data

      } else {
        context.info("Checking file name from response")
        def response = hpi.getHttpResponse()
        def check = response.getFirstHeader("Content-Disposition")

        def fileNameMatch = check =~ /filename="([^"]+)/
        if (fileNameMatch) {
          name = fileNameMatch[0][1].toString().trim()
        }
        if (!(name =~ /(?i)[^\/\\]+\.(?:pdf|png|jpg|BMP|TIFF|GIF)$/)) {
          throw new Exception("Not a valid(pdf,png,jpg,bmp,tiff,gif) file URL")
        }
        data = response.entity.content
      }

      File tmpFile = new File(System.getProperty("java.io.tmpdir"), name)
      OutputStream outputStream = new FileOutputStream(tmpFile, false)
      IOUtils.copy(data, outputStream)
      outputStream.flush()
      outputStream.close()
      def dataMap = [hpi: hpi, key: dataUrl]

      return getText(tmpFile, true, false, dataMap)
    }

    String getText(File dataFile, deleteFileOnExit = false, cacheEnabled = true, dataMap = [:]) {
      def data
      if (cacheEnabled && (data = cachedData(dataFile.getPath()))) {
        context.info("Loading from cache: " + dataFile)
        return data
      }

      dataMap["fileNameEx"] = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()).toString()
      dataMap.dataFile = dataFile

      data = uploadFile(dataMap)
      data = covertToText(data, dataMap)
      data = downloadProcessedData(data, dataMap)

      if (deleteFileOnExit) {
        dataFile.delete()
      }
      dataMap.hpi.close()
      cachedData(dataMap.key ? dataMap.key : dataFile.getPath(), data)

      return data
    }

    private def createSession(dataMap, reNew = false) {
      if (!dataMap.hpi || reNew) {
        dataMap.hpi = new HttpInvoker(rootUrl)
      } else {
        dataMap.hpi.setUrl(rootUrl)
      }
      HttpInvoker hpi = dataMap.hpi

      hpi.addPresetHeaderSet()
      String html = hpi.getStringData()

      def viewStateMatch = html =~ /(?is)id="_+VIEWSTATE"\s*value="([^"]*)/
      def viewStateValue = ""
      if (viewStateMatch) {
        viewStateValue = viewStateMatch[0][1].toString().trim()
      }
      def viewStateGenMatch = html =~ /(?is)id="_+VIEWSTATEGENERATOR"\s*value="([^"]*)/
      def viewStateGenValue = ""
      if (viewStateGenMatch) {
        viewStateGenValue = viewStateGenMatch[0][1].toString().trim()
      }

      def eventValidationMatch = html =~ /(?is)id="_+EVENTVALIDATION"\s*value="([^"]*)/
      def eventValidationValue = ""
      if (eventValidationMatch) {
        eventValidationValue = eventValidationMatch[0][1].toString().trim()
      }

      dataMap["viewStateValue"] = viewStateValue
      dataMap["viewStateGenValue"] = viewStateGenValue
      dataMap["eventValidationValue"] = eventValidationValue

      MultipartEntity mpe = new MultipartEntity()
      mpe.addPart("__EVENTTARGET", new StringBody(""))
      mpe.addPart("__EVENTARGUMENT", new StringBody(""))
      mpe.addPart("__VIEWSTATE", new StringBody(viewStateValue))
      mpe.addPart("__VIEWSTATEGENERATOR", new StringBody(viewStateGenValue))
      mpe.addPart("__EVENTVALIDATION", new StringBody(eventValidationValue))
      mpe.addPart("ctl00\$MainContent\$comboLanguages", new StringBody("ENGLISH"))
      mpe.addPart("ctl00\$MainContent\$comboOutput", new StringBody("Text Plain (txt)"))
      mpe.addPart("ctl00\$MainContent\$hdnFullPath", new StringBody("-1"))
      mpe.addPart("ctl00\$MainContent\$hdnFileName", new StringBody("-1"))
      mpe.addPart("ctl00\$MainContent\$hdnFileNameEx", new StringBody(dataMap.fileNameEx))
      mpe.addPart("files[]", new FileBody(dataMap.dataFile))

      return mpe
    }

    private String uploadFile(dataMap) {
      MultipartEntity mpe = createSession(dataMap)
      HttpInvoker hpi = dataMap.hpi

      //continuously check upload progress
      def wait = 500
      while (true) {
        hpi.setUrl(uploadUrl)
        hpi.setMultiPartPostParams(mpe)

        //set headers again
        hpi.addPresetHeaderSet()
        def data = hpi.getStringData()

        //corrupted file size check
        if (data =~ /"size":0/) {
          if (wait > 1000) {
            context.info(data)
            throw new Exception("Please check the input file size!")
          } else {
            hpi.close()
            mpe = createSession(dataMap, true)
          }
        }

        //check for success flags
        if (data =~ /"success":true/) {
          if (data =~ /original_name":"C:\\\\uploaded_doc/) {
            dataMap.deleteUrl = (data =~ /delete_url":"([^"]+)/)[0][1]
            return data

          } else {
            //wait 1sec before next call
            Thread.sleep(wait)
            wait += wait
          }
        } else {
          context.info(data)
          throw new Exception("OCR api's file upload failed!")
        }
      }
    }

    private String covertToText(data, Map dataMap) {
      HttpInvoker hpi = dataMap.hpi
      def serverPathMatch = data =~ /\"original_name\":"([^"]+)/
      def serverLoc
      if (serverPathMatch) {
        serverLoc = serverPathMatch[0][1].toString().trim()
      }

      def formData = dataMap.formData = [ctl00$MainContent$ScriptManager1: "ctl00\$MainContent\$UpdatePanel2|ctl00\$MainContent\$btnOCRConvert",
                                         ctl00$MainContent$comboLanguages: "ENGLISH",
                                         ctl00$MainContent$comboOutput   : "Text Plain (txt)",
                                         ctl00$MainContent$hdnFullPath   : serverLoc,
                                         ctl00$MainContent$hdnFileName   : dataMap.dataFile.getName(),
                                         ctl00$MainContent$hdnFileNameEx : dataMap.fileNameEx,
                                         __EVENTTARGET                   : "",
                                         __EVENTARGUMENT                 : "",
                                         __VIEWSTATE                     : dataMap.viewStateValue,
                                         __VIEWSTATEGENERATOR            : dataMap.viewStateGenValue,
                                         __EVENTVALIDATION               : dataMap.eventValidationValue,
                                         __ASYNCPOST                     : "true",
                                         ctl00$MainContent$btnOCRConvert : "CONVERT"]

      hpi.setUrl(rootUrl)
      hpi.setPostParams(formData)
      hpi.addPresetHeaderSet()

      //start conversion
      data = hpi.getStringData()

      formData.remove("ctl00\$MainContent\$btnOCRConvert")
      formData.replace("ctl00\$MainContent\$ScriptManager1", "ctl00\$MainContent\$ScriptManager1|ctl00\$MainContent\$Timer1")
      formData.replace("__EVENTTARGET", "ctl00\$MainContent\$Timer1")

      //continuously check for conversion progress
      def wait = 500
      while (!(data =~ /(?is)Download\s*Output\s*File/)) {
        Thread.sleep(wait)
        formData = updateProgressFormData(formData, data)
        hpi.setPostParams(formData)
        data = hpi.getStringData()
        wait += wait
      }

      return data
    }

    private String downloadProcessedData(String value, dataMap) {
      //now download the final converted text data
      HttpInvoker hpi = dataMap.hpi
      def formData = dataMap.formData = updateProgressFormData(dataMap.formData, value)
      def ocrResultMatch = value =~ /_txtOCRResultText"[^>]+>([^<]+)/
      def ocrResult
      if (ocrResultMatch) {
        ocrResult = ocrResultMatch[0][1].toString().trim()
      }

      formData["files[]"] = ""
      formData["ctl00\$MainContent\$txtOCRResultText"] = ocrResult
      formData["__EVENTTARGET"] = "ctl00\$MainContent\$lnkBtnDownloadOutput"
      formData.remove("ctl00\$MainContent\$ScriptManager1")
      formData.remove("__ASYNCPOST")

      hpi.setPostParams(formData)
      String textData = hpi.getStringData()
      //fixing null character for the text data
      textData = textData.replaceAll(/\u0000/, "")

      //delete the data from server
      //our HttpInvoker currently doesn't support DELETE call
      /*
      final delUrl = dataMap.deleteUrl
      final hi = hpi //use the same hpi instance
      new Thread({
          hi.setUrl(delUrl)
          hi.addPresetHeaderSet()
          hi.getData()
          hi.close()
      }).start()
      */

      return textData
    }

    private LinkedHashMap updateProgressFormData(formaData, text) {
      def updatedEventViewMatch = text =~ /__VIEWSTATE\|([^\|]+)/
      if (updatedEventViewMatch) {
        formaData.replace("__VIEWSTATE", updatedEventViewMatch[0][1].toString().trim())
      }

      def eventValidMatch = text =~ /__EVENTVALIDATION\|([^\|]+)/

      if (eventValidMatch) {
        formaData.replace("__EVENTVALIDATION", eventValidMatch[0][1].toString().trim())
      }

      return formaData
    }

    private cachedData(key, value = null) {
      if (context?.hasProperty("cached_data")) {
        try {
          //if "value" is not present then it will only "get" otherwise it wll "put" into the cache
          if (!value) {
            //if key doesn't exist, it'll return "null"
            return context.cached_data.get(key)
          } else {
            context.cached_data.put(key, value)
            return true
          }
        } catch (e) {
        }
      }

      return null
    }
  }

}
