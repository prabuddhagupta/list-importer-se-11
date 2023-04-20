package current_script

import com.rdc.importer.scrapian.request.HttpInvoker
import org.apache.commons.compress.utils.IOUtils
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument

import java.text.SimpleDateFormat

class ImageToTextConverter2 {
  private final rootUrl = "https://www.onlineocr.net"
  private final uploadUrl = "https://www.onlineocr.net/FileHandler.ashx"
  private final context

  ImageToTextConverter2(context = null) {
    this.context = context
  }

  /*
  *  Options:
  * ---------
  * cache: true/false
  * delete_file_on_exit: true/false
  *
  * */

  String getText(String dataUrl, options = [:]) {
    def cacheEnabled = options["cache"] && options["cache"].toString() =~ /(?i)\btrue\b/
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

    return getText(tmpFile, options, cacheEnabled, dataMap)
  }

  String getText(File dataFile, cacheEnabled = true, options = [:], dataMap = [:]) {
    cacheEnabled = !cacheEnabled && options["cache"] && options["cache"].toString() =~ /(?i)\btrue\b/
    def deleteFileOnExit = options["delete_file_on_exit"] && options["delete_file_on_exit"].toString() =~ /(?i)\btrue\b/
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


  def getSplittedPDF(File file, String dir, String dirName) throws IOException {
    PDDocument document = PDDocument.load(file)
    int count = document.getNumberOfPages()
    if (count > 1) {

      Splitter splitter = new Splitter()
      List<PDDocument> pages = splitter.split(document)
      Iterator<PDDocument> iterator = pages.listIterator()
      char i = 'A'

      while (iterator.hasNext()) {
        PDDocument pd = iterator.next()
        pd.save(dir + i++ + ".pdf")
      }
    }
    document.close()

    File directory = new File(dirName)
    def data = ""
//        File[] fList = directory.listFiles();
    List<File> fList = Arrays.asList(directory.listFiles())
    Collections.sort(fList, new Comparator<File>() {
      @Override
      int compare(File f1, File f2) {
        String s1 = f1.getName()
        String s2 = f2.getName()
        return s1.compareTo(s2)
      }
    })

//        println(fList)
    for (File singleFile : fList) {
      if (singleFile.isFile()) {
//                println(singleFile.length())
        def pdfText = getText(singleFile)
        data = data + pdfText
      }
    }
    System.out.println("Multiple pdf's created")
    return data.toString().replaceAll(/\r\n/, "\n")
  }

}