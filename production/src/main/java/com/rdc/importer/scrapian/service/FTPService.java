package com.rdc.importer.scrapian.service;

import com.rdc.importer.misc.StreamUtils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
/**
 * Created by IntelliJ IDEA.
 * User: rhickey
 * Date: 4/27/11
 * Time: 10:18 AM
 * To change this template use File | Settings | File Templates.
 */
@Component
@Scope("prototype")
public class FTPService implements FileXferService {

    private FTPClient ftp = new FTPClient();
    private boolean login = false;
    private boolean connected = false;
    private Logger logger = LogManager.getLogger(FTPService.class);
    private int firstTimeout = 10000;
    private int secondTimeout = 20000;
    private int sleepTime = 300000;

    public FTPClient getFtp() {
        return ftp;
    }

    public boolean isLogin() {
        return login;
    }

    public boolean isConnected() {
        return connected;
    }
    
    @Resource
    public void setConfiguration(Configuration configuration) {
    	firstTimeout = configuration.getInteger("com.rdc.list_importer.ftp.firstTimeout", 10000);
    	secondTimeout = configuration.getInteger("com.rdc.list_importer.ftp.secondTimeout", 20000);
    	sleepTime = configuration.getInteger("com.rdc.list_importer.ftp.sleepTime", 300000);
    }

    public boolean getConnection(String serverName, String userName, String password)throws Exception{

         boolean error = false;
         int reply;
         logger.info("Attempting connection to " + serverName + " with a timeout of " + firstTimeout + " millis...");
         ftp.setDefaultTimeout(firstTimeout);
         ftp.connect(serverName);
         reply = ftp.getReplyCode();

            if(ftp.getReplyString() == null || !FTPReply.isPositiveCompletion(reply)){
               logger.info("Connection Refused - Disconnecting");
               ftp.disconnect();
               throw new Exception("FTP Connection Failed - ReplyCode: " + ftp.getReplyString());
            }
            connected = true;
            
            try{
            	this.login = ftp.login(userName, password);
            } catch (Exception e){
            	if (e instanceof SocketTimeoutException){
            		try{
            		ftp.logout();
            		} catch (Exception e2){
            			logger.error("Received error when trying logout", e2);
            		}
            		logger.error("Not logged in. Disconnecting from server to begin retry");
            		disconnectFromServer();
            		logger.error("Disconnected. Sleeping for " + sleepTime + " millis.");
            		Thread.sleep(sleepTime);            		
            		
                    logger.info("Attempting connection to " + serverName + " with a timeout of " + secondTimeout + " millis");
                    ftp = new FTPClient();
                    ftp.setDefaultTimeout(secondTimeout);
                    ftp.connect(serverName);
                    reply = ftp.getReplyCode();

                       if(ftp.getReplyString() == null || !FTPReply.isPositiveCompletion(reply)){
                          logger.info("Connection Refused - Disconnecting");
                          ftp.disconnect();
                          throw new Exception("FTP Connection Failed - ReplyCode: " + ftp.getReplyString());
                       }
                       connected = true;
            		this.login = ftp.login(userName, password);            			
            	}
            }
            
            if(login){
                error = false;
                logger.info("Login success!");
            }else{
                if(isConnected()){
                    ftp.disconnect();
                }
                throw new Exception("List Importer FTP Login Failed - Server [" + serverName + "] - ReplyCode: " + ftp.getReplyString());
            }

         return error;
    }

    public FTPFile[] getFileList() throws Exception{
        FTPFile[] files = ftp.listFiles();
        return files;
    }

    public void getFile(String src, OutputStream dest, boolean isBlackList) throws Exception{
        logger.info("Attempting to copy file: " + src);
        ftp.setControlEncoding("UTF-8");
        ftp.setDataTimeout(20000);


        try{

            if(isBlackList){
                ftp.changeWorkingDirectory("/upload/blacklists/");
            }

            ftp.setFileType(ftp.BINARY_FILE_TYPE);

            if(!ftp.retrieveFile(src, dest)){
                logger.info("Failed!");
                throw new FTPCopyFailedException("Failed to copy file from ftp: " + src + ", ReplyCode: " + ftp.getReplyString(),
                        ftp.getReplyCode());
            }else{
               logger.info("Success!");
            }
        }catch(FTPConnectionClosedException ce){
            throw new Exception("Could not copy file from server < Connection is Closed > - ReplyCode: " + ftp.getReplyString() + "\n" + ce.getMessage());
        }catch (CopyStreamException cs){
            throw new Exception("File Transfer Exception < Copy Stream Exception > - ReplyCode: " + ftp.getReplyString() + "\n" + cs.getMessage());

        }catch(IOException ie){
            throw new Exception("Could not compy file < IO Exception > - ReplyCode: " + ftp.getReplyString() + "\n" + ie.getMessage());
        }finally{
            StreamUtils.closeQuietly(dest);
        }
    }

    public void logoutFromServer()throws Exception{
        if(this.isLogin()){
            try{
                ftp.logout();
                logger.info("Logged out from FTP Server");
            }catch(Exception e){
                throw new Exception("Could not log out from FTP server - ReplyCode: " + ftp.getReplyString());
            }
        }
    }

    public void disconnectFromServer() throws Exception{
       if(ftp.isConnected()){
           try{
               ftp.disconnect();
               logger.info("Disconnected from FTP Server");
               connected = false;
           }catch(Exception ex){
               throw new Exception("Could not disconnect form FTP Server - ReplyCode: " + ftp.getReplyString());
           }
       }
    }

    public void deleteFile(String fileName) throws Exception{

        try{
           boolean result;
           logger.info("Removing " + fileName + " from FTP Server.");
           result = ftp.deleteFile(fileName);
           if (!result) {
                int replyCode = ftp.getReplyCode();
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
