package com.rdc.importer.scrapian.resolver;

import com.rdc.importer.scrapian.ScrapianDataList;
import com.rdc.importer.scrapian.ScrapianEngine;
import com.rdc.importer.scrapian.ScrapianScript;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class ClasspathScrapianResolver implements ScrapianResolver {

    private String scriptBase = "/scrapian_scripts/";
    private String dataListBase = "/scrapian_datalist/";

    public void setScriptBase(String scriptBase) {
        this.scriptBase = scriptBase;
    }

    public ScrapianScript resolveScript(String scriptName) throws Exception {
        ScrapianScript scrapianScript = new ScrapianScript();
        scrapianScript.setKey(scriptName);

        try {
            URL url = ScrapianEngine.class.getResource(scriptBase + scriptName + ".groovy");
            if (url == null) {
                throw new Exception("Unable to locate[" + scriptName + "]");
            }
            scrapianScript.setUrl(url);
        } catch (IOException e) {
            throw new Exception("Unable to locate[" + scriptName + "]");
        }

        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("params.")) {
                scrapianScript.getParams().put(key.substring(7), (String) entry.getValue());
            }
        }
        return scrapianScript;
    }

    public ScrapianDataList resolveDataList(String dataListName) throws Exception {
        ScrapianDataList scrapianDataList = new ScrapianDataList();
        scrapianDataList.setKey(dataListName);

        InputStream fileStream = ScrapianEngine.class.getResourceAsStream(dataListBase + dataListName + ".txt");
        scrapianDataList.setDataList(IOUtils.readLines(fileStream));


        return scrapianDataList;
    }
}