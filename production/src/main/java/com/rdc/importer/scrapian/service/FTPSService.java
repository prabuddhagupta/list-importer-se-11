package com.rdc.importer.scrapian.service;

import com.rdc.importer.misc.StreamUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

public class FTPSService implements FileXferService {

    private FTPSClient ftps;
    private boolean login = false;
    private boolean connected = false;
    private Logger logger = LogManager.getLogger(FTPSService.class);

    public FTPSClient getFtp() {
        return ftps;
    }

    public boolean isLogin() {
        return login;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean getConnection(String serverName, String userName, String password)throws Exception{

         boolean error = false;
         int reply;
         logger.info("Attempting connection to " + serverName + "..");

         try {
            ftps = new FTPSClient(true);
         }catch (Exception e) {
             logger.error("FTPSClient - Failed to get FTPs instance",e);
         }

         ftps.setDefaultTimeout(30000);
         ftps.connect(serverName,990);
         logger.info("Issued connection to " + serverName + "..on port: " + ftps.getDefaultPort() +" Passive Host:"+ ftps.getPassiveHost());

         reply = ftps.getReplyCode();

            if(ftps.getReplyString() == null || !FTPReply.isPositiveCompletion(reply)){
               logger.info("Connection Refused - Disconnecting");
               ftps.disconnect();
               throw new Exception("FTP Connection Failed - ReplyCode: " + ftps.getReplyString());
            }
            connected = true;    
            
            try{
            	this.login = ftps.login(userName, password);
            } catch (Exception e){
            	if (e instanceof SocketTimeoutException){
            		Thread.sleep(60000);
            		ftps.setDefaultTimeout(60000);
            		this.login = ftps.login(userName, password);            			
            	}
            }
            
            if(login){
                error = false;
                logger.info("Login success!");
            }else{
                if(isConnected()){
                    ftps.disconnect();
                }
                throw new Exception("List Importer FTP Login Failed - Server [" + serverName + "] - ReplyCode: " + ftps.getReplyString());
            }

         return error;
    }

    public FTPFile[] getFileList() throws Exception{
        FTPFile[] files = ftps.listFiles();
        return files;
    }

   public void getFile(String src, OutputStream dest, boolean isBlackList) throws Exception{
        logger.info("Attempting to copy file: " + src);
        ftps.setControlEncoding("UTF-8");
        ftps.setDataTimeout(120000);


        try{
            ftps.setBufferSize(1000);
            ftps.enterLocalPassiveMode();
            ftps.execPBSZ(0);
            ftps.execPROT("P");
            ftps.setFileType(ftps.BINARY_FILE_TYPE);

            logger.info("Begin file retrieval...");
            if(!ftps.retrieveFile(src, dest)){
                logger.info("Failed!");
                throw new FTPCopyFailedException("Failed to copy file - ReplyCode: " + ftps.getReplyString(),
                        ftps.getReplyCode());
            }else{
               logger.info("Success!");
            }
        }catch(FTPConnectionClosedException ce){
            throw new Exception("Could not copy file from server < Connection is Closed > - ReplyCode: " + ftps.getReplyString() + "\n" + ce.getMessage());
        }catch (CopyStreamException cs){
            throw new Exception("File Transfer Exception < Copy Stream Exception > - ReplyCode: " + ftps.getReplyString() + "\n" + cs.getMessage());

        }catch(IOException ie){
            logger.info(ExceptionUtils.getFullStackTrace(ie));
            throw new Exception("Could not copy file < IO Exception > - ReplyCode: " + ftps.getReplyString() + "\n" + ie.getMessage(), ie);
        }finally{
            StreamUtils.closeQuietly(dest);
        }
    }

    public void logoutFromServer()throws Exception{
        if(this.isLogin()){
            try{
                ftps.logout();
                logger.info("Logged out from FTP Server");
            }catch(Exception e){
                throw new Exception("Could not log out from FTP server - ReplyCode: " + ftps.getReplyString());
            }
        }
    }

    public void disconnectFromServer() throws Exception{
       if(ftps.isConnected()){
           try{
               ftps.disconnect();
               logger.info("Disconnected from FTP Server");
           }catch(Exception ex){
               throw new Exception("Could not disconnect form FTP Server - ReplyCode: " + ftps.getReplyString());
           }
       }
    }

    public void deleteFile(String fileName) throws Exception{

        try{
           boolean result;
           logger.info("Removing " + fileName + " from FTP Server.");
           result = ftps.deleteFile(fileName);
           if (!result) {
                int replyCode = ftps.getReplyCode();
                logger.warn("Could not delete remote file:" + fileName + ", replyCode=" + replyCode);
           }
       }catch(Exception e){
            throw new Exception("Unable to delete file: " + fileName);
       }
    }

	@Override
	public void navigateToFileDir(String workingDir) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
