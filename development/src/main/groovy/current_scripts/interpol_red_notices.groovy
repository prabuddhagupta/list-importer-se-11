package current_scripts

import com.opencsv.CSVWriter
import groovy.transform.Field
/* Notes
* For the given url the data comes from the network url
* The response is a json value
* I have used regex to parse the entity detail from response
* I have used OPEN CSV library to dump the data into CSV
* */
String givenUrl = "https://www.interpol.int/How-we-work/Notices/View-Red-Notices"
def html = invoke(givenUrl)
@Field
def cc = [:] //Mapping the Country abbriviation to CountryName
def ccR = html =~ /option value="(\w{2})">([^<]+)/
while (ccR.find()) {
    def code = ccR.group(1)
    def country = ccR.group(2)
    cc.put(code, country)
}

@Field
String filePath = "/tmp/output.csv"
@Field
File file = new File(filePath);
@Field
FileWriter outputfile = new FileWriter(file)
@Field
int i=1

cc.each { key, val ->
    def network_url = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key"
    jsonData = invoke(network_url)
    jsonData = jsonData.replaceAll(/\s+\n/, "\n").trim()
    captureData(jsonData, val)
    // throw new Exception(">>>>>>>>>>>>>")
}
println("TOTAL ENTITIES: $i")

def captureData(String source, def country) {
    // requirement
    def name, dob, height, weight, nationality, eyecolor, pob = null, charges = null,tot
    def totR=source=~/total":(\d+)/
    if(totR.find()){
        tot=totR.group(1).toInteger()
    }
    def entityUrlMatcher = source =~ /(?ism)"self"\W+"href":"([^"]+)/
    while (entityUrlMatcher.find()&&tot--) {
        def ent_url = entityUrlMatcher.group(1).trim()
        def html = invoke(ent_url)
        def nameRegex = html =~ /forename":"([^"]+).+?name":"(.+?)"/
        if (nameRegex.find()) {
            name = nameRegex.group(2) + " " + nameRegex.group(1)
        }
        def dobRegex = html =~ /date_of_birth":"([^"]+)/
        if (dobRegex.find()) {
            dob = dobRegex.group(1)
        }
        def chargeMatch = html =~ /charge":"([^"]+)/
        if (chargeMatch.find()) {
            charges = cleanData(chargeMatch.group(1))
        }
        def pobMatch = html =~ /place_of_birth":"([^"]+)/
        if (pobMatch.find()) {
            pob = pobMatch.group(1) + "," + country
        }else {
            pob=country
        }
        def hr = html =~ /(?i)Height":"([^"]+)/
        if (hr.find()) {
            height = hr.group(1) + "Meters"
        }
        def wr = html =~ /(?i)Height":"([^"]+)/
        if (wr.find()) {
            weight = wr.group(1) + "Kilograms"
        }
        def er = html =~ /eyes_colors_id":"([^"]+)/
        if (er.find()) {
            eyecolor = er.group(1)
            println(eyecolor)
        }
        def nationR = html =~ /(?i)nationalities":"([^"]+)/
        if (nationR.find()) {
            nationality = nationR.group(1)
            println(nationality)
        }
        createEntity(name, dob, height, weight, nationality, eyecolor, pob, charges)
    }

}

def invoke(def url) {
    println("INVOKING $url")
    return url.toURL().text

}

def cleanData(def data) {
    return data.replaceAll("\\r\\n", "\n")
}

def createEntity(def name, def dob, def height, def weight, def nationality, def eyecolor, def pob, def charges) {
    //this method will dump data to CSV
    writeToCSV(name, dob, height, weight, nationality, eyecolor, pob, charges)
    i++
}

def writeToCSV(String... args) {
    try {
        String[] header;
        if (i == 1) {
            header = ["Name", "Date Of Birth", "Height", "Weight", "Nationality", "Eye Color", "Place Of Birth", "Charges"]
            i++
        }
        CSVWriter writer = new CSVWriter(outputfile);
        writer.writeNext(header)
        // adding header to csv
        writer.writeNext(args);
        writer.flush()
        //writer.close();
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}