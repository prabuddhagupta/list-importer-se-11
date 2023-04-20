package scrapian_scripts.utils.modules

import com.rdc.importer.scrapian.request.HttpInvoker
import org.apache.commons.compress.utils.IOUtils
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

import java.text.SimpleDateFormat
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class OcrReader {
    private final rootUrl = "https://www.onlineocr.net"
    private final uploadUrl = "https://www.onlineocr.net/FileHandler.ashx"
    private final context

    OcrReader(context = null) {
        this.context = context
    }

    private void setDefaultOptions(Map options, id) {
        def setBool = {key, dv ->
            options[key] = options[key] == null ? dv : (options[key].toString() =~ /(?i)\btrue\b/).find()
        }

        def setStr = {key, dv ->
            options[key] = options[key] != null ?: dv
        }

        setBool("cache", true)
        setBool("reuse_network_downloads", true)
        setBool("force_ocr", false)
        setStr("key", id)

        //hidden key: options set key
        options["is_def_set"] = true
    }

    /*
    *  Options:
    * ---------
    * cache: true/false
    * reuse_network_downloads: true/false
    * force_ocr: false:true
    * key: empty string (set if any preferred value: else getKey() will be used)
    * */

    List<String> getText(String dataUrl, options = [:]) {
        File tmpFile = new File(System.getProperty("java.io.tmpdir"), dataUrl.replaceAll(/[:\/]/, "_"))
        setDefaultOptions(options, tmpFile.path)
        def data

        if (options["cache"] && (data = cachedData(options.key))) {
            context.info("Loading from cache: " + dataUrl)
            return data
        }

        def hpi = new HttpInvoker(dataUrl)
        if ((dataUrl =~ /(?i)(?:[^\/\\]+\.(?:pdf|png|jpg|BMP|TIFF|GIF))$/)) {
            data = hpi.data

        } else {
            context.info("Checking file name from response")
            def response = hpi.getHttpResponse()
            def contentType = response.getFirstHeader("Content-Disposition")

            if (!(contentType =~ /(?i)filename=".*[^\/\\]+\.(?:pdf|png|jpg|BMP|TIFF|GIF)$/)) {
                throw new Exception("Not a valid(pdf,png,jpg,bmp,tiff,gif) file URL")
            }
            data = response.entity.content
        }

        if (!options["reuse_network_downloads"] || !tmpFile.exists()) {
            OutputStream outputStream = new FileOutputStream(tmpFile, false)
            IOUtils.copy(data, outputStream)
            outputStream.flush()
            outputStream.close()
        }
        hpi.close()

        return getText(tmpFile, options)
    }

    List<String> getText(File dataFile, Map options = [:]) {
        options.is_def_set ?: setDefaultOptions(options, dataFile.path)
        def data

        if (options.cache && (data = cachedData(dataFile.getPath()))) {
            context.info("Loading from cache: " + dataFile)
            return data
        }

        List dataList = startParsingJob(getPdfPages(dataFile), options)
        cachedData(options.key, dataList)

        return dataList
    }

    private def startParsingJob(collectedFiles, options) {
        final List<String> dataList = []
        def pool = Executors.newWorkStealingPool()
        List<Future<String>> futures = []

        collectedFiles.each {final File dataFile ->
            futures.add(pool.submit(new Callable<String>() {
                @Override
                String call() throws Exception {
                    String data;
                    if (options.force_ocr) {
                        return tryNetworkParsing(dataFile)
                    }

                    data = tryLocalParsing(dataFile)
                    if (!data) {
                        data = tryNetworkParsing(dataFile)
                    }

                    return data
                }
            }))
        }

        futures.each {
            dataList.add(it.get())
        }
        pool.shutdownNow()

        return dataList
    }

    private def tryLocalParsing(file) {
        if (isPdf(file)) {
            PDDocument document = PDDocument.load(file);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            document.close()
            return text.trim()

        } else {
            return ""
        }
    }

    private def tryNetworkParsing(file) {
        def dataMap = [:]
        dataMap["fileNameEx"] = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()).toString()
        dataMap.dataFile = file

        def data = uploadFile(dataMap)
        data = covertToText(data, dataMap)
        data = downloadProcessedData(data, dataMap)

        dataMap.hpi.close()
        return data
    }

    private def isPdf(File file) {
        return (file.getName() =~ /(?i)\.pdf$/).find()
    }

    private def getPdfPages(File file) throws IOException {
        def fileList = []

        //only PDF could be sliced into pages
        if (isPdf(file)) {
            PDDocument document = PDDocument.load(file)
            int count = document.getNumberOfPages()

            if (count > 1) {
                String dir = file.canonicalPath.replaceAll(/\.\w+$/, "/")
                new File(dir).mkdirs()
                Splitter splitter = new Splitter()
                List<PDDocument> pages = splitter.split(document)
                Iterator<PDDocument> iterator = pages.listIterator()
                int i = 0

                while (iterator.hasNext()) {
                    PDDocument pd = iterator.next()
                    def pdfFile = dir + i + ".pdf"
                    fileList.add(new File(pdfFile))
                    pd.save(pdfFile)
                    pd.close()
                    i++
                }

            } else {
                fileList.add(file)
            }
            document.close()

        } else {
            fileList.add(file)
        }

        return fileList
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