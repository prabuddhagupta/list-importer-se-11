import org.apache.commons.net.ftp.FTPClient;
//import org.apache.commons.net.ftp.FTPFile;

import com.rdc.scrape.ScrapeEntity;
import com.rdc.core.nameparser.PersonName;
import com.rdc.scrape.ScrapeAddress;
import com.rdc.scrape.ScrapeEvent;
import com.rdc.scrape.ScrapeSource
import org.apache.commons.net.ftp.FTPFile;

import java.util.zip.ZipInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.rdc.scrape.ScrapeSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.text.SimpleDateFormat;

import com.rdc.importer.scrapian.service.FTPService;

import org.apache.commons.lang.StringUtils;
/*
 * Zip file is FTP'd to RDC by third party.  
 * FTP file to local server
 * unzip to three files (disq_person.x, disq_act.x, disq_except.x where x is date) in temp directory
 * validate each is formatted correctly
 * row by row combine 3 files into an entity object (driven by the person file)
 * Throws exceptions if no file found via ftp, validation fails, or a 'person' row does not have matching data in 'act'
 * See Jira LI-356 document "Disqualified Directors Feed version 1-2.docx" for format/definition of the 3 files
 * 5/7/2014
 */
debug = false;
String server = context.scriptParams.server;  //ftp.companieshouse.gov.uk
String user = context.scriptParams.user;    //rdc
String password = context.scriptParams.password;  //nXL983kq9
String remoteDir = formatDirString(context.scriptParams.remoteDir); //  /
String serverDir = formatDirString(context.scriptParams.localDir);
String tempDir = serverDir + "tmp/";  //directory on server in which to unzip files...
//String companiesHouse = "http://wck2.companieshouse.gov.uk/dirsec";
String companiesHouse = "http://wck2.companieshouse.gov.uk/dirsec";
sdf2 = new SimpleDateFormat('yyyyMMdd');
sdf3 = new SimpleDateFormat('MM/dd/yyyy');

def formatDirString(String dirString){
    if(dirString!=null && !dirString.endsWith("/")){
        return dirString + "/";
    }
    return dirString;
}

/*
 * Date is received as 20140507 (yearMonthDay) and is converted to month/day/year
 */
def formatDate( String dateString){
    if(dateString.length() > 7) {
        return dateString.substring(4,6) + "/" + dateString.substring(6,8) + "/" + dateString.substring(0,4);
    }
    return "";
}

/*
 * Remove AKA from name (first or last)
 */
def removeAka( String name){
    if ((matchAka = name =~ /(^.*)[ (](?:AKA|A\.K\.A\.|ALSO KNOWN AS) .*/).matches()){
        return matchAka[0][1];
    }
    return name;
}

def unzip ( File self, File destination) {
    if (destination == null) destination = new File(self.parent);

    def unzippedFiles = [];

    final zipInput = new ZipInputStream(new FileInputStream(self));
    zipInput.withStream {
        def entry;
        while(entry = zipInput.nextEntry) {
            if (!entry.isDirectory()) {
                final file = new File(destination, entry.name);
                file.parentFile?.mkdirs();

                def output = new FileOutputStream(file);
                output.withStream { output << zipInput; }

                unzippedFiles << file;
            }
            else {
                final dir = new File(destination, entry.name)
                dir.mkdirs();

                unzippedFiles << dir;
            }
        }
    }
    return unzippedFiles;
}

def zip(outFileName, fileName) {

    fout = new FileOutputStream(outFileName + ".zip");
    zout = new ZipOutputStream(fout);
    ze = new ZipEntry(lastPart(fileName));
    zout.putNextEntry(ze);
    fileInputStream = new FileInputStream(fileName);
    bufferedInputStream = new BufferedInputStream(fileInputStream, 2048);
    int size = -1;
    data1 = new byte[2048]
    while(  (size = bufferedInputStream.read(data1, 0, 2048)) != -1  )
    {
        zout.write(data1, 0, size);
    }
    bufferedInputStream.close();
    zout.closeEntry();

    zout.close();

}

def lastPart(input){
    if(input == null){
        return null;
    }
    lastIndex = input.lastIndexOf("/");
    if(lastIndex != -1 && (lastIndex + 1) < input.size()){
        return input.substring(lastIndex + 1);
    } else {
        return input
    }
}

def clearDirectory(dir) {
    context.info("Clear directory called on " + dir + "\n");
    File directory = new File(dir);
    if (!directory.isDirectory()) {
        context.info("It was not a valid directory\n");
        return;
    }

    File[] files = directory.listFiles();
    File choice = null;
    // get latest file
    for (File file : files) {
        if (file.getName().startsWith("DISQ")) {
            if (choice == null) {
                choice = file;
            } else {
                if (file.lastModified() > choice.lastModified()) {
                    context.info(choice.getName() + " will be deleted.\n");
                    FileUtils.deleteQuietly(choice);

                    choice = file;
                } else {
                    context.info(file.getName() + " will be deleted.\n");
                    FileUtils.deleteQuietly(file);
                }
            }
        }
    }
    if(choice != null){
        context.info(choice.getName() + " will be saved since it is the newest.\n");
    }
}

//for method timing
def benchmark = { closure ->
    double start = System.currentTimeMillis();
    closure.call();
    double now = System.currentTimeMillis();
    (now - start)  / 1000;
}

/*
 * Checks a file line by line, applying the given regular expression.
 * The regEx should check for correct element format, count, and pipe delimitation and apply the proper grouping
 * An empty element is sent to us as "\ " which escaped is "\\ ".  This is converted to a true blank ""
 * The first two matches are skipped.  The first match is the whole line, the second is the row ID (which we do not need)
 */
def String[][] parseAndValidate (File file) {
    retVals = [][];
    j = 0;
    entity = null;
    newEntity = false;
    file.eachLine{line ->
        if(line.startsWith('DISQUALS')){
            return;
        } else if (line.startsWith('1')) {
            line = line.replaceAll(/&/,"&amp;")
            if(entity != null){
                if(newEntity){
                    context.session.addEntity(entity);
                }
                entity = null;
            }
            dob = line.substring(13,21).trim();
            splitUp = line.substring(33).split("<",-1);
            name = splitUp[1] + " " + splitUp[2]
            aliasFirst = false;
            if(splitUp[1].contains("(") || splitUp[1].contains("A.K.A") || splitUp[1].contains("AKA")){
                aliasFirst = true;
            }
            aliasLast = false;
            if(splitUp[2].contains("(") || splitUp[2].contains("A.K.A") || splitUp[2].contains("AKA")){
                aliasLast = true;
            }
            if(aliasFirst && aliasLast){
                //throw new Exception("Encounterd unhandled alias type");
                print "Skipping Alias";
            }

            name = name.replaceAll(/\s{2,}/,' ').trim();

            alias = '';
            aliasMatch = name =~ /.*? (?:(?:\(|)A(?:\.|)K(?:\.|)A(?:\.|)(?:\)|)|\(NOW|\(ALIAS) ([^)]*)(?:\)|$)/
            if(aliasMatch.find()){
                alias = aliasMatch.group(1).replaceAll(/^.*KNOWN AS /,'').replaceAll(/^A(\.|)K(\.|)A(\.|) /,'').replaceAll(/ AKA /,'-SPLIT-').replaceAll(/ &amp; /,'-SPLIT-').replaceAll(/(?i) AND /,'-SPLIT-').replaceAll(/, /,'-SPLIT-');
                name = name.replaceAll(/(?:(?:\(|)A(?:\.|)K(?:\.|)A(?:\.|)(?:\)|)|\(NOW|\(ALIAS) ([^)]*)(?:\)|$)/,'').trim();

            } else {

                aliasMatch = name =~ /.*?\((.*?)\).*/
                if(aliasMatch.find()){
                    alias = aliasMatch.group(1).replaceAll(/^.*KNOWN AS /,'').replaceAll(/^AKA /,'').replaceAll(/ AKA /,'-SPLIT-').replaceAll(/ &amp; /,'-SPLIT-').replaceAll(/(?i) AND /,'-SPLIT-').replaceAll(/, /,'-SPLIT-');
                    name = name.replaceAll(/\(.*?\)/,'').trim();
                }
            }
            name = name.replaceAll(/,$/,'');
            address = new ScrapeAddress();
            if(splitUp[4]){
                address.setAddress1(splitUp[4].trim() + (splitUp[5].length() > 0 ? ", " + splitUp[5].trim() : ""));
            }
            if(splitUp[6]){
                address.setCity(splitUp[6].trim());
            }
            if(splitUp[7]){
                address.setProvince(splitUp[7].trim());
            }
            if(splitUp[8]){
                address.setCountry(splitUp[8].trim());
            }
            postal = line.substring(21,29)
            if(postal.trim() != ''){
                address.setPostalCode(postal.trim());
            }
            if(StringUtils.isBlank(address.getCountry())){
                address.setCountry("UNITED KINGDOM");
            } else if (!address.getCountry().contains("UNITED KINGDOM") && ("_ENGLAND_SCOTLAND_WALES_".contains(address.getCountry()) || "NORTHERN IRELAND".equals(address.getCountry()))){
                address.setCountry(address.getCountry() + ", UNITED KINGDOM");
            }

//			idPart = makeIdFromAddress(address);
//			id = name + (idPart != '' ? idPart : line.substring(1,13));
            id = line.substring(1,13);  //THOMAS WILLIAM FELL
            prefix = splitUp[0];
            entity = context.findEntity(['name':name, 'id':id]);
            newEntity = false;
            if(entity == null){
                newEntity = true;
                entity = new ScrapeEntity();
                entity.setName(name);
                entity.setDataSourceId(id);
                entity.setType('P');
                //entity.addUrl('http://wck2.companieshouse.gov.uk/dirsec');
            }

            if(dob && !(name == 'JASPREET SINGH' && dob == '99981231')){
                entity.addDateOfBirth(sdf3.format(sdf2.parse(dob)));
            }

            if(alias){
                splitUpAlias = alias.split("-SPLIT-");
                splitUpAlias.each{alia ->
                    if(aliasFirst){
                        entity.addAlias(alia.replaceAll(/,$/,'').trim() + " " + splitUp[2].replaceAll(/\(.*?\)/,'').trim());
                    } else if (aliasLast){
                        entity.addAlias(splitUp[1].replaceAll(/\(.*?\)/,'').trim() + " " + alia.replaceAll(/,$/,'').trim());
                    } else {
                        entity.addAlias(alia.replaceAll(/,$/,'').trim());
                    }
                }
            }

            if(prefix){
                if(prefix == 'MR' || prefix == 'MR.'){
                    entity.addSex("M");
                } else if (prefix == 'MRS' || prefix == 'MRS.' || prefix == 'MISS' || prefix == 'MS'){
                    entity.addSex("F");
                } else {
                    entity.addAlias(prefix + " " + name);
                }
            }

            entity.addAddress(address);

            if(splitUp[9]){
                entity.addNationality(splitUp[9])
            }
            remark = '';
            if(splitUp[10]){
                remark += 'Corporate-Number: ' + splitUp[10];
            }
            if(splitUp[11]){
                remark += (remark.length() > 0 ? ";" : "") + 'Country-Registration : ' + splitUp[11];
            }
            if(remark.length() > 0){
                entity.addRemark(remark);
            }
        } else if (line.startsWith('2')) {
            id = line.substring(1,13);
            if(id != entity.getDataSourceId()){
                //context.info(id + ' did not match to ' + entity.getName() + ' properly');
            }
            //println line;
            startDate = line.substring(13,21)
            endDate = line.substring(21,29)
            act = line.substring(29,41).replaceAll(/&/,"&amp;")
            caseType = line.substring(41,71).trim();
            caseNum = line.substring(79, 109).replaceAll(/&/,"&amp;").trim();
            compName = line.substring(109, 269).replaceAll(/&/,"&amp;").trim();
            cortNameLen = line.substring(269,273).trim();
            cortName = '';
            if(cortNameLen != ''){
                cortName = line.replaceAll(/\u00e2\u0080\u0099/, "'").replaceAll(/\u00e2\u20ac\u2122/, "'").replaceAll(/â€™/, "'").substring(273, 273 + new Integer(cortNameLen)).replace("INSOLVENCY SERVICE","").replace("UNDERTAKING","").replaceAll(/&/,"&amp;")
            }
            if(entity.name=="DAINIUS STEPONAVICIUS"){
                context.info("cortname for DAINIUS STEPONAVICIUS: " + cortName)
                cortName.each{
                    context.info( it + ' \\\\u' + Integer.toHexString((int)it))
                }
            } else if(entity.name=="NICOLINA ROBERTS"){
                context.info("compName for NICOLINA ROBERTS: " + compName)
                compName.each{
                    context.info( it + ' \\\\u' + Integer.toHexString((int)it))
                }
            }
            insolvency = false;
            if(caseType == ''){
                if(caseNum.startsWith("INV")){
                    caseType = 'UNDERTAKING'
                    insolvency = true;
                } else if (caseNum =~ /\d{3,}/){
                    caseType = 'ORDER'
                } else if (caseNum =~ /[A-Z]{4,}/){
                    caseType = 'UNDERTAKING'
                }
            } else if(caseType == 'UNDERTAKING' && caseNum.startsWith("INV")){
                insolvency = true;
            }
            event = new ScrapeEvent();
            event.setCategory("REG");
            event.setSubcategory("ACT");
            if(startDate.trim() != '') event.setDate(sdf3.format(sdf2.parse(startDate)));
            if(endDate.trim() != '') event.setEndDate(sdf3.format(sdf2.parse(endDate)));
            event.setDescription( ("UK Disqualified Director under act "
                + act + (caseType != '' ? '; Disqualification Type: ' + caseType : '')
                + (caseNum != '' && (insolvency ||caseType != 'UNDERTAKING') ? '; ' + 'Case Number: ' + caseNum : '')
                + (compName != '' ? '; Company Name: ' + compName : '')
                + (cortName != '' ? '; Court Name: ' + cortName : '')).replaceAll(/\u00e2\u0091\u00a4/, "").replaceAll(/\u00e2\u2018\u00a4/, "").replaceAll(/â‘¤/, "").replaceAll(/\s{2,}/,' ')
            );
            entity.addEvent(event);
        } else if (line.startsWith('3')) {
            id = line.substring(1,13);
            if(id != entity.getDataSourceId()){
                //throw new Exception(id + ' did not match to ' + entity.getName() + ' properly');
            }
            //println line;
            startDate = line.substring(13,21)
            endDate = line.substring(21,29)
            purpose = new Integer(line.substring(29,39));
            if(purpose == 1){
                purpose = 'Promotion'
            } else if(purpose == 2){
                purpose = 'Formation'
            } else if(purpose == 3){
                purpose = 'Directorships or other participation in management of a company'
            } else if(purpose == 4){
                purpose = 'Designated member/member or other participation in management of an LLP'
            } else if(purpose == 5){
                purpose = 'Receivership in relation to a company or LLP'
            } else {
                purpose = ''
            }

            compNameLen = line.substring(39,43).trim();
            compName = '';
            if(compNameLen != ''){
                compName = line.substring(43, 43 + new Integer(compNameLen)).replaceAll(/&/,"&amp;")
            }

            startDate = (startDate.trim() != '') ? sdf3.format(sdf2.parse(startDate)) : '';
            endDate = (endDate.trim() != '') ? sdf3.format(sdf2.parse(endDate)) : '';
            remark = 'Exception ' + (compName != '' ? 'for company ' + compName + ' ': '') +
                ((startDate != '' && endDate != '') ? 'between ' : '') +
                ((startDate != '' && endDate == '') ? 'after ' : '') +
                ((startDate != '' && endDate == '') ? 'until ' : '') +
                ((startDate!='') ? startDate + ' ': '' ) +
                ((startDate != '' && endDate != '') ? 'and ' : '') +
                ((endDate!='') ? endDate + ' ': '' );
            entity.addRemark(remark.trim());
        } else if (line.startsWith('4')) {
            id = line.substring(1,13);
            if(id != entity.getDataSourceId()){
                //throw new Exception(id + ' did not match to ' + entity.getName() + ' properly');
            }
            //println line;
            startDate = line.substring(51,59);
            type =  line.substring(13,43).trim();
            caseNum = line.substring(59,89).trim();

            cortNameLen = line.substring(89,93).trim();
            cortName = '';
            if(cortNameLen != ''){
                cortName = line.substring(93, 93 + new Integer(cortNameLen)).replace("INSOLVENCY SERVICE","").replace("UNDERTAKING","").replaceAll(/&/,"&amp;")
            }
            event = new ScrapeEvent();
            event.setCategory("REG");
            event.setSubcategory("ACT");
            if(startDate.trim() != '') event.setDate(sdf3.format(sdf2.parse(startDate)));
            event.setDescription("UK Disqualified Director Variation"
                + (caseNum != '' ? '; Case Number: ' + caseNum : '')
                + (type != '' ? '; Type: ' + type : '')
                + (cortName != '' ? '; Court Name: ' + cortName : ''));
            entity.addEvent(event);
        } else {
            throw new Exception("Unexpected file contents");
        }

    }
//	for(ScrapeEntity e : context.session.getEntities()){
//		e.setDataSourceId(null);
//	}
}

def makeIdFromAddress(address){
    retVal = '';
    if(address.getAddress1()){
        retVal += address.getAddress1();
    }
    if(address.getCity()){
        retVal += address.getCity();
    }
    if(address.getProvince()){
        retVal += address.getProvince();
    }
    if(address.getCountry()){
        retVal += address.getCountry();
    }
    if(address.getPostalCode()){
        retVal += address.getPostalCode();
    }
    return retVal;
}

//start of script
context.info("Params: serverDir: " + serverDir + ", tempDir: " + tempDir + ", server: " + server + ", user: " + user + ", password: " + password + ", remoteDir: " + remoteDir);
boolean foundFile = false;
boolean downloadOk = true;
String latestFileName;
FTPFile[] goodFiles;
if(!debug){
    ftp_time = benchmark {
        context.info("About to connect....");
        FTPService ftp = new FTPService();
        ftp.getConnection(server, user, password,21);
        ftp.getFtp().changeWorkingDirectory(remoteDir);
        FTPFile[] files = ftp.getFileList();

        if(files.length > 0){
            goodFiles = files.findAll { file -> (file.getName().startsWith("DISQ") ) };

            if(goodFiles != null && goodFiles.length > 0){
                sortedFiles = goodFiles.sort{fileA, fileB -> fileA.getTimestamp() <=> fileB.getTimestamp()};
                latestFileName = sortedFiles[-1].getName(); //file with most recent timestamp


                context.info("Grabbing " + latestFileName + " from FTP");
                File incomingFile = new File(serverDir + latestFileName);
                //incomingFile.withOutputStream { ostream -> retrieveFile(gF.getName(), ostream) }
                incomingFile.withOutputStream { ostream -> ftp.getFile(latestFileName, ostream, false); }
                if (incomingFile.isFile() && incomingFile.length() > 0){
                    context.info("File good");
                    foundFile = true;
                } else {
                    downloadOk = false;
                    context.info("File bad");
                }
            }
        }
        //if all downloaded correctly, delete
//    if(downloadOk){
//        goodFiles.each{gF ->
//            context.info("Deleting File " + gF.getName() + " from FTP site");
//            ftp.deleteFile(remoteDir + gF.getName());
//        }
//    }
        ftp.disconnectFromServer();

        context.info(" ...Done.");
    }

    if(!foundFile || !downloadOk){
        context.info("Could not transfer/find file via FTP");
        throw new Exception("Could not transfer/find file via FTP");
    }
}
String[][] entities;  //who is being reported on

Collection<File> extractedFiles;
foundFile = true;
if(foundFile){
    unzip_time = benchmark {
        //extractedFiles = unzip(new File(serverDir + latestFileName), new File(tempDir));
        //context.info(extractedFiles.size() + " files extracted from archive for " + latestFileName);
    }

    context.info "Validating"
    validate_time = benchmark {
        //String date = latestFileName.substring(4, 12);

        //context.info("person");
        entities = parseAndValidate (new File(serverDir + latestFileName));


        context.info("All files passed validation/parsing");
    }

//    extractedFiles.each{eF ->
//        context.info("Deleting unzipped file " + eF.getName());
//        eF.delete();
//    }

    //clear directory
    //clearDirectory(serverDir);

    //move zip to archive folder
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    datedFolder = serverDir + "DISQ-" + sdf.format(new Date()) + "/";
    folder = new File(datedFolder);
    folder.mkdir();
    //new File(serverDir + latestFileName).renameTo(new File(datedFolder + latestFileName));
    //context.info("Moved " + (serverDir + latestFileName) + " to " + datedFolder + latestFileName);	
    zip(datedFolder + latestFileName, serverDir + latestFileName);
    context.info("Zipped it up");
    deleted = new File(serverDir + latestFileName).delete();
    context.info("Deleted: " + deleted);

}

context.info("ftp: " + ftp_time + (foundFile ? " unzip: " + unzip_time + " parse/validate: " + validate_time  + " total: " + (ftp_time + unzip_time + validate_time) : "") +  " seconds");