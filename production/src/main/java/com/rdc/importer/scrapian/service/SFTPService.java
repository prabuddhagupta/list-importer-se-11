package com.rdc.importer.scrapian.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
/**
 * Created by IntelliJ IDEA.
 * User: rhickey
 * Date: 4/27/11
 * Time: 10:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class SFTPService implements FileXferService {

	
	private Session session;
    private ChannelSftp sftpChannel;

    @Override
    public boolean getConnection(String host, String username, String password) throws Exception {
    	boolean error = false;
        try {
        	JSch jsch = new JSch();
        	session = jsch.getSession(username, host, 22);
        	session.setPassword(password);

        	// Set Strict HostKeyChecking to no so we don't get the unknown host key exception
        	java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("MaxAuthTries", "2");
            session.setConfig(config);
            session.connect(30000);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(30000);
        } catch (Throwable e) {
            throw new Exception("SFTPServer=[" + host + ":" + 22 + "] refused connection.", e);
        }
        return error;
    }

    public void cd(String directory) throws Exception {
        try {
            sftpChannel.cd(directory.startsWith("/") ? directory : "/" + directory);
        } catch (Throwable e) {
            throw new Exception("Error changing to WorkingDirectory[=" + directory + "]", e);
        }
    }

    public void put(String filename, File file) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            sftpChannel.put(fis, filename);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private static class JschLogger implements com.jcraft.jsch.Logger {
        private Logger logger = Logger.getLogger(SFTPService.class);

        public boolean isEnabled(int level) {
            return true;
        }

        public void log(int level, String message) {
            switch (level) {
                case DEBUG:
                    logger.debug(message);
                    break;
                case INFO:
                    logger.info(message);
                    break;
                case ERROR:
                case FATAL:
                    logger.error(message);
                    break;
                case WARN:
                    logger.warn(message);
            }
        }
    }

	@Override
	public boolean isLogin() {
		if(session != null)
			return true;
		return false;
	}

	@Override
	public boolean isConnected() {
		if(session != null)
			return true;
		return false;
	}

	@Override
	public FTPFile[] getFileList() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getFile(String src, OutputStream dest, boolean isBlackList) throws Exception {
		sftpChannel.get(src, dest);
		
	}

	@Override
	public void logoutFromServer() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void disconnectFromServer() throws Exception {
		
		if (sftpChannel != null) {
			try {
				sftpChannel.quit();
			} catch (Exception ignore) {
				// Ignore
            }
            try {
                sftpChannel.disconnect();
            } catch (Exception ignore) {
                // Ignore
            }
        }
        if (session != null) {
            try {
                session.disconnect();
            } catch (Exception ignore) {
                // Ignore
            }
        }
		
	}

	@Override
	public void deleteFile(String fileName) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void navigateToFileDir(String workingDir) throws Exception {
		sftpChannel.cd(workingDir);
		
	}

}
