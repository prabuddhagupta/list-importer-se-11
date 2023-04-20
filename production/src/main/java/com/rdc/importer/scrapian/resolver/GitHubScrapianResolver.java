package com.rdc.importer.scrapian.resolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.rdc.importer.scrapian.ScrapianDataList;
import com.rdc.importer.scrapian.ScrapianScript;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

public class GitHubScrapianResolver implements ScrapianResolver {
    private HttpClient httpClient;
    private String subversionConfigUrl;

    @PostConstruct
    public void init() {
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(1);
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setParams(params);
        httpClient = new HttpClient(connectionManager);
    }

    @Resource
    public void setConfiguration(Configuration configuration) {
        subversionConfigUrl = configuration.getString("com.rdc.importer.scrapian.configUrl");
    }

    public ScrapianScript resolveScript(String scriptName) throws Exception {
        ScrapianConfig scrapianConfig = getScrapianConfig();

        ScrapianScript scrapianScript = new ScrapianScript();

        String url = scrapianConfig.getUrlForScript(scriptName);
        if (url == null) {
            throw new Exception("Unable to locate[" + scriptName + "]");

        }
        scrapianScript.setUrl(new URL(url));
        scrapianScript.setParams(scrapianConfig.getParamsForScript(scriptName));
        return scrapianScript;
    }

    public ScrapianDataList resolveDataList(String dataListName) throws Exception {
        ScrapianDataList scrapianDataList = new ScrapianDataList();

        String url = getScrapianConfig().getUrlForDataList(dataListName);
        if (url == null) {
            throw new Exception("Unable to locate[" + dataListName + "]");
        }

        byte[] bytes = fetchUrl(url);
        scrapianDataList.setDataList(IOUtils.readLines(new ByteArrayInputStream(bytes)));
        return scrapianDataList;
    }

    private byte[] fetchFileUrl(String url) throws IOException {
        String file = (new URL(url)).getFile();
        return IOUtils.toByteArray(new FileInputStream(file));
    }
    private byte[] fetchUrl(String url) throws IOException {
        if ((new URL(url)).getProtocol().equalsIgnoreCase("file"))
            return fetchFileUrl(url);

        GetMethod get = new GetMethod(url);
        int rc = httpClient.executeMethod(get);
        if (rc == HttpStatus.SC_OK) {
            return IOUtils.toByteArray(get.getResponseBodyAsStream());
        } else {
            throw new IOException("Invalid status code[" + rc + "]");
        }
    }

    public synchronized ScrapianConfig getScrapianConfig() throws IOException, SAXException {
        return ScrapianConfig.create(new ByteArrayInputStream(fetchUrl(subversionConfigUrl)));
    }
}
