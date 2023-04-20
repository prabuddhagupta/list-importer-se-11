package scrapian_scripts.utils.modules

import com.rdc.importer.scrapian.request.HttpInvoker
import org.apache.commons.compress.utils.IOUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import javax.net.ssl.HttpsURLConnection

class OCRSpaceReader {
    private final rootUrl = "https://www.onlineocr.net"
    private final uploadUrl = "https://www.onlineocr.net/FileHandler.ashx"
    private final context

    OCRSpaceReader(context = null) {
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

        return getText(dataUrl, tmpFile, options)
    }

    List<String> getText(String pdfUrl, File dataFile, Map options = [:]) {
        options.is_def_set ?: setDefaultOptions(options, dataFile.path)
        def data

        if (options.cache && (data = cachedData(dataFile.getPath()))) {
            context.info("Loading from cache: " + dataFile)
            return data
        }

        List dataList = parsePDF(pdfUrl, "7565d1132088957")
        cachedData(options.key, dataList)

        return dataList
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
            } catch (e) {}
        }
    }

    private List<String> parsePDF(String pdfURL, String apiKey) {
        List<String> pdfList = new ArrayList<>()
        try {
            pdfList = sendPost(apiKey, false, pdfURL, "eng")
//            pdfList = sendPost("7565d1132088957", false, pdfURL, "eng")
        } catch (Exception e) {
            e.printStackTrace()
        }
        return pdfList
    }

    private List<String> sendPost(String apiKey, boolean isOverlayRequired, String pdfURL, String language) throws Exception {
        URL url = new URL("https://api.ocr.space/parse/image")
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection()

        // Add request header
        httpsURLConnection.setRequestMethod("POST")
        httpsURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0")
        httpsURLConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")

        JSONObject postDataParams = new JSONObject()

        postDataParams.put("apikey", apiKey)
        postDataParams.put("isOverlayRequired", isOverlayRequired)
        postDataParams.put("url", pdfURL)
        postDataParams.put("language", language)

        // Send POST request
        httpsURLConnection.setDoOutput(true)
        DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream())
        dataOutputStream.writeBytes(getPostDataString(postDataParams))
        dataOutputStream.flush()
        dataOutputStream.close()

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))
        String inputLine
        StringBuffer response = new StringBuffer()

        while ((inputLine = bufferedReader.readLine()) != null) {
            response.append(inputLine)
        }
        bufferedReader.close()

        return getParsedPDF(response.toString())
    }

    private List<String> getParsedPDF(String string) throws ParseException {
        List<String> pdfList = new ArrayList<>()
        JSONArray parsedResults = (JSONArray) stringToJSON(string).get("ParsedResults")

        parsedResults.each {result -> try {
            pdfList.add((String) stringToJSON(result.toString()).get("ParsedText"))
        } catch (ParseException e) {
            e.printStackTrace()
        }
        }

        return pdfList
    }

    private JSONObject stringToJSON(String toConvert) throws ParseException {
        JSONParser jsonParser = new JSONParser()
        JSONObject jsonObject = (JSONObject) jsonParser.parse(toConvert)
        return jsonObject
    }

    private String getPostDataString(JSONObject params) throws Exception {
        StringBuilder result = new StringBuilder()
        boolean first = true

        Iterator<String> iterator = params.keySet().iterator()

        while (iterator.hasNext()) {
            String key = iterator.next()
            Object value = params.get(key)

            if (first) first = false
            else result.append("&")

            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value.toString(), "UTF-8"))

        }
        return result.toString()
    }
}

