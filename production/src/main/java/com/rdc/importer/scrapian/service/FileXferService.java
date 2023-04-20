package com.rdc.importer.scrapian.service;

import org.apache.commons.net.ftp.FTPFile;

import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: rhickey
 * Date: 4/27/11
 * Time: 10:18 AM
 * To change this template use File | Settings | File Templates.
 */
public interface FileXferService {
    public boolean isLogin();

    public boolean isConnected();

    public boolean getConnection(String serverName, String userName, String password )throws Exception;

    public FTPFile[] getFileList() throws Exception;

    public void getFile(String src, OutputStream dest, boolean isBlackList) throws Exception;

    public void logoutFromServer() throws Exception;

    public void disconnectFromServer() throws Exception;

    public void deleteFile(String fileName) throws Exception;
    
    public void navigateToFileDir(String workingDir) throws Exception;
}
