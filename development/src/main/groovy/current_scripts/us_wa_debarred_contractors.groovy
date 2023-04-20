package current_scripts

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.service.FTPCopyFailedException
import com.rdc.importer.scrapian.service.FileXferService
import com.rdc.importer.scrapian.service.SFTPService
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent
import org.apache.commons.io.FileUtils
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply

context.setup([connectionTimeout: 20000, socketTimeout: 40000, retryCount: 1, multithread: true])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

USDEBCON script = new USDEBCON(context)
//script.whenDownloadFileUsingJsch_thenSuccess()

script.initParsing()
int i = 1
def nameIdMap = [:]
for (ScrapeEntity entity : context.session.getEntitiesNonCache()) {
    entity.setId('' + i++);
    nameIdMap[entity.getName()] = entity
}
for (entity in context.session.getEntitiesNonCache()) {
    for (association in entity.getAssociations()) {

        otherEntity = nameIdMap[association];
        if (otherEntity && !isAlreadyAssociated(entity, otherEntity)) {
            scrapeEntityAssociation = new ScrapeEntityAssociation(otherEntity.getId());
            scrapeEntityAssociation.setRelationshipType(RelationshipType.ASSOCIATE);
            scrapeEntityAssociation.setHashable(otherEntity.getName(), otherEntity.getType());
            entity.addScrapeEntityAssociation(scrapeEntityAssociation);
        }
    }
    entity.getAssociations().clear();
}

def isAlreadyAssociated(entity, otherEntity) {
    Set associations = otherEntity.getScrapeEntityAssociations();
    boolean isAssos = false;
    if (associations) {
        associations.each { item ->
            if (((ScrapeEntityAssociation) item).getId().equals(entity.getId())) {
                isAssos = true;
            }
        }
    }
    return isAssos;
}

class USDEBCON {
    final ScrapianContext context
    def moduleLoaderVersion = context.scriptParams.moduleLoaderVersion
    final def moduleFactory = ModuleLoader.getFactory(moduleLoaderVersion)
    final entityType
    final def mainUrl = "https://secure.lni.wa.gov/debarandstrike/ContractorDebarList.aspx"
    USDEBCON(context) {
        this.context = context
        entityType = moduleFactory.getEntityTypeDetection(context)
    }

    //File downloadedFile = getLatestFileFromSFTP();


    def idType = "Unified Business Identifier (UBI)"
    def event_des = "This entity appears on the Washington List of Debarred Contractors."
    def entityName, UBI, debarBegins, debarEnds, principals

    def initParsing() {
        println(getLatestFileFromSFTP())
        File downloadedFile = getLatestFileFromSFTP();

        println "File Path: " + downloadedFile
        def pathToFile = "file://" + downloadedFile.getPath()
        println "File Path: " + pathToFile

        URL file_src = new URL(pathToFile)
        InputStream csv = file_src.openStream()

        int line = 0
        csv.eachLine {
            //dont take the headline
            if (line > 0) {
                it = it.replaceAll(/(?i)\,\"/, ";").trim()
                it = it.replaceAll(/\"|amp;|apos;/, "").trim()
                it = it.replaceAll(/(?s)\s+/, " ").trim()
                def details = it.split(";")
                getData(details)

            }
            line++
        }
        csv.close()

    }

    def getData(def line) {
        debarBegins = ""
        debarEnds = ""
        principals = null
        def companyName = line[0]
        UBI = line[1]

        if (line[3] =~ /(?i)[A-Z]+/) {
            principals = line[3].split(",").toList()
        }
        debarBegins = line[6]
        debarEnds = line[7]

        if (principals == null) {
            createEntity(companyName, null)
        } else {
            principals.each {
                if (it) {
                    createEntity(companyName, it)
                    createEntity(it, companyName)
                }
            }
            principals.clear()
        }


    }

    def getAlias(def name) {
        name = name.replaceAll(/(?i)Steel Trowel Inc., The/, "The Steel Trowel Inc.")
        name = name.replaceAll(/\(/, "-FKA-")
        name = name.replaceAll(/(?i)\s+(?:DBA|A.?K.?A)\b/, "-FKA-")
        name = name.replaceAll(/(?i)^\W+|\W+$/, "").trim()
        def aliasList = name.split("-FKA-").toList()
        name = aliasList.remove(0)
        entityName = formatName(name)

        return aliasList
    }

    def formatName(def name) {
        name = name.toString().toUpperCase().trim()
        name = name.replaceAll(/(?i)\,\s+(LLC|INC)(\W|$)/, ' $1')
        name = name.replaceAll(/(.+)\,(.+)/, /$2 $1/)
        name = name.replaceAll(/^DBA\s+|(?i)\s+(?:DBA|A.?K.?A)\b.+/, "").trim()
        name = name.replaceAll(/,$/, "").trim()
        return name.trim()
    }

    //===================Create Entity========================

    def createEntity(def name, def association) {
        def alias
        entityName = ""
        if (name) {
            alias = getAlias(name)
        }
        if (entityName == "") {
            return
        }
        def entity = null
        entity = context.findEntity([name: entityName])
        def etype = typeDetection(entityName)
        if (!entity) {
            entity = context.getSession().newEntity()
            entity.setName(entityName)
            entity.setType(etype)
        }
        if (association) {
            def assc = formatName(association)
            entity.addAssociation(assc)
        }
        alias.each {
            if (it) {
                def atype = typeDetection(it)
                if (atype == etype) {
                    it = it.toString().replaceAll(/(?i)\,/, "").trim()
                    it = it.replaceAll(/(?s)\s+/, " ").trim()
                    entity.addAlias(it.trim())
                } else {
                    entity.addAssociation(it.trim())
                    def new_entity = context.findEntity([name: it])
                    if (!new_entity) {
                        new_entity = context.getSession().newEntity()
                        new_entity.setName(it)
                        new_entity.setType(atype)
                        new_entity.addAssociation(entityName.trim())
                        addCommonPart(new_entity)
                    }

                }
            }
        }

        addCommonPart(entity)


    }

    def typeDetection(def inp) {
        def orgMatcher = inp =~ /(?i)(^[A-Z]+$|JP\'Z|\&|Painting|ART|Coating|Floor|MECHANICAL|ROOFING|S C C|Tile)/
        if (orgMatcher) {
            return "O"
        } else {
            return entityType.detectEntityType(inp)
        }
    }

    def addCommonPart(def entity) {
        if (UBI) {
            def identification = entity.newIdentification()
            identification.type = idType
            identification.value = UBI
            entity.addIdentification(identification)
        }
        if (debarBegins) {
            ScrapeEvent event = new ScrapeEvent()
            event.setDescription(event_des)
            def date = context.parseDate(new StringSource(debarBegins), ["MM/dd/yyyy"] as String[])
            event.setDate(date)
            if (debarEnds) {
                date = context.parseDate(new StringSource(debarEnds), ["MM/dd/yyyy"] as String[])
                event.setEndDate(date)
            }
            entity.addEvent(event)
        }

        ScrapeAddress address = new ScrapeAddress()
        address.setCity("Washington")
        address.setCountry("United States")
        entity.addAddress(address)
    }

    def getLatestFileFromSFTP() {
        boolean fileUploaded = false;
        /*def serverName = context.scriptParams.serverName
        def username = context.scriptParams.userName
        def password = context.scriptParams.password*/
        def serverName = "ftp.rdc.com"
        def port = 22
        def username = "li_data"
        def password = "a2+GJPsh(69pbKGd"
        FileXferService ftp = new SFTPService();
        //def fileName = context.scriptParams.fileName
        def fileName = "DebarList.csv"
        File file = new File('/tmp/' + fileName);
        try {
            ftp.getConnection(serverName, username, password)
            ftp.navigateToFileDir('./li_internal_data');
            FileOutputStream fos = new FileOutputStream(file);
            ftp.getFile(fileName, fos, false);
            fileUploaded = true
        } catch (FTPCopyFailedException ex) {
            FileUtils.deleteQuietly(file);
            context.info("Unable to retrieve file " + file.getName() + " from FTP server. FTP reply code = " + ex.getReplyCode());
            throw ex;
        } catch (SftpException ex) {
            FileUtils.deleteQuietly(file);
            def message = "No file " + file.getName() + " found on FTP server. " + "location: " + username
            context.info(message);
          //  def emailTo = ["MA_KYC_ListImporterDevs@moodys.com"] as String[]
            //context.getEmailUtil().sendEmailWithAttachment(emailTo, message, message, null, null);
        } finally {
            if (ftp.isConnected()) {
                ftp.disconnectFromServer();
            }
        }
        if (fileUploaded) {
            return file;
        }
        return null;
    }

    def getFileFromFTP() {
// Create an instance of FTPClient
        def serverName = "ftp.rdc.com"
        def port = 22
        def username = "li_data"
        def password = "a2+GJPsh(69pbKGd"
        FTPClient ftp = new FTPClient();
        try {
            // Establish a connection with the FTP URL
            println "Connecting................"

            ftp.connect(serverName, port);
            boolean isSuccess = ftp.login(username, password);
            ftp.enterLocalPassiveMode();
            ftp.setStrictReplyParsing(false);
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            // Enter user details : user name and password


            if (isSuccess) {
                println "Connected....."
                // Fetch the list of names of the files. In case of no files an
                // empty array is returned
                String[] filesFTP = ftp.listNames();
                int count = 1;
                // Iterate on the returned list to obtain name of each file
                for (String file : filesFTP) {
                    System.out.println("File " + count + " :" + file);
                    count++;
                }
            }

            ftp.logout();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                ftp.disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    def connectServer() {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(1000 * 60 * 30);
        ftpClient.setControlEncoding("UTF-8");
        ftpClient.enterLocalPassiveMode(); //Set passive mode, file transfer port setting
        try {
            ftpClient.connect("172.17.0.16", 21);
            ftpClient.login("ftp-user", "123456");
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE); // Set file transfer mode to binary
            final int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return ftpClient;
    }

   /* ChannelSftp setupSFTP() throws JSchException {
        def serverName = "ftp.rdc.com"
        def port = 22
        def username = "li_data"
        def password = "a2+GJPsh(69pbKGd"
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        JSch jsch = new JSch();
//        jsch.setKnownHosts("/Users/maenul/.ssh/known_hosts");
        Session jschSession = jsch.getSession(username, serverName);
        jschSession.setPassword(password);
        jschSession.setConfig(config);
        jschSession.connect();
        return (ChannelSftp) jschSession.openChannel("sftp");
    }

    def downloadFile() throws JSchException, SftpException {
        ChannelSftp channelSftp = setupSFTP();
        channelSftp.connect();

        String remoteFile = "./li_internal_data/DebarList.csv";
        String localDir = "/home/maenul/Documents/";


        channelSftp.get(remoteFile, localDir + "DebarList.csv");
        channelSftp.exit();
    }*/
}