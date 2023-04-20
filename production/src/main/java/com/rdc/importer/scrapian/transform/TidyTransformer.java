package com.rdc.importer.scrapian.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.StringSource;
import org.w3c.tidy.Tidy;

public class TidyTransformer implements ScrapianTransformer {

    private String encoding = "raw";
    
    public StringSource transform(ScrapianSource scrapianSource, Map<String, Object> params) throws Exception {

        Tidy tidy = new Tidy();

        tidy.setInputEncoding(encoding);
        tidy.setOutputEncoding(encoding);
    
        if (params != null) {
            if (Boolean.TRUE.equals(params.get("xhtml"))) {
                tidy.setXHTML(true);
            } else {
                tidy.setXHTML(false);
            }

            if (params.containsKey("fixUri")) {
                tidy.setFixUri((Boolean) params.get("fixUri"));
            }

            if (params.containsKey("quoteAmpersand")) {
                tidy.setQuoteAmpersand((Boolean) params.get("quoteAmpersand"));
            }
        }

        tidy.setForceOutput(true);
        tidy.setShowWarnings(false);
        tidy.setDropFontTags(true);
        tidy.setIndentContent(false);
        tidy.setWrapSection(false);
        
        tidy.setWrapAttVals(false);
        tidy.setWraplen(10000);
        tidy.setNumEntities(true);
        tidy.setQuiet(true);
        tidy.setWord2000(true);
        tidy.setTidyMark(false);


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String fileEncoding = ("raw".equals(encoding) || encoding == null) ? System.getProperty("file.encoding") : encoding;
        tidy.parse(new ByteArrayInputStream(scrapianSource.serialize().getBytes(fileEncoding)), outputStream);
        return new StringSource(outputStream.toString(fileEncoding));
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
