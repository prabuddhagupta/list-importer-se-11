package com.rdc.importer.scrapian;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ScrapianScript {
    private String key;
    private URL url;
    private Map<String, String> params = new HashMap<String, String>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
