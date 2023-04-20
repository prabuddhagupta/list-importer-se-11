package test

import java.util.regex.Matcher
import java.util.regex.Pattern

class Problem7 {
    HttpClient_B client
    def finalData = [:]
    def headers = ["Name", "Status", "Race", "Sex", "Date Of Birth", "Current Age", "Height", "Weight", "Eye Color", "Hair Color",
                   "Release Date", "Current Address", "Address is Temporary", "Address Last Verified", "Registration Date",
                   "Date of Arrest", "Conviction Date", "Sex Crime", "Crime Location", "Crime Country", "Description",
                   "UCR Code", "Scars, Marks & Tattoos", "Scholls Attending", "Vehicle Information", "Alias Names",
                   "Special Notes"]
    //CsvBuilder csvBuilder = new CsvBuilder("/home/masudbappy/Desktop/problemSeven.csv", headers)
    def quotedKeys = headers.collect({ Pattern.quote(it) })

    def collectingUrls() {
        client = new HttpClient()
        addHeaders()
        def firstContent = client.getU
        def firstContenFormData = []
        def firstMappedData = ["ctl00\$plhBodyArea\$btnAccept":"Accept"]
        Matcher firstMatcher = firstContent =~ /<input type="hidden" name="\w+" id="(\w+)" value="(.*)" \/>/
        while (firstMatcher.find()){
           def key = firstMatcher.group(1)
           def value = firstMatcher.group(2)
            firstMappedData[key]=value
        }
        firstMappedData.each {
            firstContenFormData.add(it)
        }
        firstContenFormData.each {
            println(it)
        }
        client = new HttpClient_B("https://app.alea.gov/Community/wfSexOffenderSearch.aspx")
        client.setPostParams(firstContenFormData)
        addHeaders()
        def formContent = client.sendPost()
        Matcher formMatcher = formContent =~ /<input type="hidden" name="\w+" id="(\w+)" value="(.*)" \/>/
        def formData = []
        def mappedData = ["tl00\$plhBodyAreaHeader\$txtFirstName"  : "", "ctl00\$plhBodyAreaHeader\$txtCity": "", "ctl00\$plhBodyAreaHeader\$txtLastName": "a", "ctl00\$plhBodyAreaHeader\$cboCountyID": "0",
                          "ctl00\$plhBodyAreaHeader\$txtZipCode"   : "", "ctl00\$plhBodyAreaHeader\$btnSearch": "Search", "ctl00\$plhBodyAreaHeader\$txtRadiusAddress": "", "ctl00\$plhBodyAreaHeader\$txtRadiusZipCode": "",
                          "ctl00\$plhBodyAreaHeader\$txtRadiusCity": "", "ctl00\$plhBodyAreaHeader\$cboRadiusDistance": "1", "ctl00\$plhBodyAreaHeader\$txtIdentifier": ""]

        while (formMatcher.find()) {
            def key = formMatcher.group(1)
            def value = formMatcher.group(2)

            mappedData[key] = value
        }
        mappedData.each {
            formData.add(it)
        }

        formData.each { println(it) }
        client = new HttpClient_B("https://app.alea.gov/Community/wfSexOffenderSearch.aspx")
        client.setPostParams(formData)
        addHeaders()
        def content = client.sendPost()
        Matcher matcher = content =~ /(?s)<td colspan="\d" style="text-align: left;">\s+<a id="[^"]+"\shref="([^"]+)">/
        def collectUrls = []
        while (matcher.find()) {
            collectUrls.add(matcher.group(1))
        }

        return collectUrls
    }

    def addHeaders() {
        //return client.setHeaders("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8", "en-US,en;q=0.9", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.62 Safari/537.36")
        return client.setHeaders(Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8',
                'Accept-Encoding': 'gzip, deflate, br',
                'Accept-Language': 'en-US, en; q=0.9',
                Connection: 'keep-alive')
    }

    static void main(args) {
        Problem7 problem7 = new Problem7()
        def urlLists = problem7.collectingUrls()
        def newUrlList = urlLists.collect {
            element ->
                return "https://app.alea.gov/Community/wfSexOffenderFlyer.aspx?ID=" + element
        }

    }


}
