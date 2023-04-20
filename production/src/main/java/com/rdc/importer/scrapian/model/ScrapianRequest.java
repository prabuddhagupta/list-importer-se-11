package com.rdc.importer.scrapian.model;

import java.util.Map;

public class ScrapianRequest {
    private String url;
    private String type;
    private boolean ignoreRedirects;
    private Map<String, String> parameters;
    private Map<String, String> headers;
    private String remoteSiteCookies;

    public ScrapianRequest(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public boolean isIgnoreRedirects() {
        return ignoreRedirects;
    }

    public void setIgnoreRedirects(boolean ignoreRedirects) {
        this.ignoreRedirects = ignoreRedirects;
    }

    public String getRemoteSiteCookies() {
        return remoteSiteCookies;
    }

    public void setRemoteSiteCookies(String remoteSiteCookies) {
        this.remoteSiteCookies = remoteSiteCookies;
    }   
}
