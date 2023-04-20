package com.rdc.importer.scrapian.model;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.apache.commons.configuration.Configuration;

import javax.annotation.Resource;

@Component
@Scope("prototype")
public class ThirdPartyListSource{

    private Logger logger = LogManager.getLogger(ThirdPartyListSource.class);

    private String sourceFileName;
    private String tmpFileName;
    private String dataDirectory;
    private File tmpFile;
    private InputStream inputStream;
    private String filePrefix = "";
    private String supplier;
    private boolean rescheduled;
    private boolean acceptEventCodes;

    private Configuration configuration;

    @Resource
    public void setConfiguration(Configuration config) {
        this.configuration = config;
        dataDirectory = configuration.getString("com.rdc.importer.dataDirectory") + "/";
    }

    public InputStream getSourceInputStream() throws Exception{
        InputStream is = new FileInputStream(getTmpDirectory() + getTmpFile().getName());
        return is;
    }

    public String getSupplierDirectory() throws Exception {
        if (StringUtils.isBlank(supplier)) {
            logger.warn("Supplier required for ThirdPartyListSource");
            throw new Exception("Exception in getSupplierDirectory - supplier is missing.");
        }
        return dataDirectory + supplier + "/";
    }

    public String getTmpDirectory() throws Exception {
        return getSupplierDirectory() + "tmp/";
    }

    public String getSourceFileName() {
         return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public File getSourceFile() throws Exception{
        return new File(getTmpDirectory() + getTmpFileName());
    }

    public String getTmpFileName() {
        return tmpFileName;
    }

    public void setTmpFile(File tmpFile) {
        this.tmpFile = tmpFile;
        this.tmpFileName = tmpFile.getName();
    }

    public File getTmpFile() throws Exception{
        return this.tmpFile;
    }

    public void removeTmpFile() throws Exception{
        try{
             if(tmpFile.exists() && tmpFile.isFile()){
                 tmpFile.delete();
                 logger.info("Removed Temp File: " + tmpFile.getName());
             }
        }catch(Exception e) {
            throw new Exception("Temp File: [" + tmpFile.getName() + "] does not exist - Cannot remove it. \n" + e);
        }
    }

    public void setInputStream(InputStream inputStream){
         this.inputStream = inputStream;
    }

	public boolean isAcceptEventCodes() {
		return acceptEventCodes;
	}

	public void setAcceptEventCodes(boolean acceptEventCodes) {
		this.acceptEventCodes = acceptEventCodes;
	}

	public boolean isRescheduled() {
		return rescheduled;
	}

	public void setRescheduled(boolean rescheduled) {
		this.rescheduled = rescheduled;
	}

}
