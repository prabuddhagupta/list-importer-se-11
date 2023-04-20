package com.rdc.importer.misc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ResourceLocatorService {
    private static final String MAX_HTTP_CONNECTIONS = "com.rdc.core.resource.MaxHttpConnections";
    private static final String SOCKET_TIMEOUT = "com.rdc.core.resources.SocketTimeout";
    private static final String CONNECTION_TIMEOUT = "com.rdc.core.resources.ConnectionTimeout";

    private Map<String, ResourceLocator> resourceLocators = new HashMap<String, ResourceLocator>();

    @Resource(name = "configuration")
    public void setConfig(Configuration config) {
        mapResourceLocator(new FileResourceLocator(), "file");
        mapResourceLocator(new CompressedFileResourceLocator(), "zipfile");
        mapResourceLocator(new HttpResourceLocator(config), "http", "https");
        resourceLocators.put(null, new ClasspathResourceLocator());
    }

    private void mapResourceLocator(ResourceLocator resourceLocator, String... protocols) {
        for (String protocol : protocols) {
            resourceLocators.put(protocol, resourceLocator);
        }
    }

    public InputStream getResource(String url) throws IOException {
        ResourceLocator resourceLocator = null;
        if (url.indexOf("://") != -1) {
            String protocol = url.substring(0, url.indexOf("://"));
            resourceLocator = resourceLocators.get(protocol);
        }

        if (resourceLocator == null) {
            resourceLocator = resourceLocators.get(null);
        }

        return resourceLocator.getResource(url);
    }

    private interface ResourceLocator {
        public InputStream getResource(String url) throws IOException;
    }

    private class ClasspathResourceLocator implements ResourceLocator {

        public InputStream getResource(String url) throws IOException {
            InputStream resource = ResourceLocatorService.class.getResourceAsStream(url);
            if (resource != null) {
                return resource;
            } else {
                throw new IOException("Classpath Resource [" + url + "] Not Found");
            }
        }
    }

    private class FileResourceLocator implements ResourceLocator {

        public InputStream getResource(String url) throws IOException {
            String filename = url.indexOf("://") != -1 ? url.substring(url.indexOf("://") + "://".length()) : url;
            File file = new File(filename);
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            }
            throw new IOException("File Resource [" + filename + "] Not Found");
        }
    }

    private class CompressedFileResourceLocator implements ResourceLocator {

        public InputStream getResource(String url) throws IOException {
            String filename = url.indexOf("://") != -1 ? url.substring(url.indexOf("://") + "://".length()) : url;
            File file = new File(filename);
            if (file.exists() && file.isFile()) {
                if (BZip2Utils.isCompressedFilename(file.getName())){
                    InputStream inputStream = new FileInputStream(file);
                    return new BZip2CompressorInputStream(inputStream);
                }
                if (GzipUtils.isCompressedFilename(file.getName())){
                    InputStream inputStream = new FileInputStream(file);
                    return new GzipCompressorInputStream(inputStream);
                }
                if (XZUtils.isCompressedFilename(file.getName())){
                    InputStream inputStream = new FileInputStream(file);
                    return new XZCompressorInputStream(inputStream);
                }
                return new FileInputStream(file);
            } else {
                File bzFile = new File(filename + ".bz2");
                if (bzFile.exists() && bzFile.isFile()) {
                    InputStream inputStream = new FileInputStream(bzFile);
                    return new BZip2CompressorInputStream(inputStream);
                } else {
                    File gzFile = new File(filename + ".gz");
                    if (gzFile.exists() && gzFile.isFile()) {
                        InputStream inputStream = new FileInputStream(gzFile);
                        return new GzipCompressorInputStream(inputStream);
                    } else {
                        File xzFile = new File(filename + ".xz");
                        if (xzFile.exists() && xzFile.isFile()) {
                            InputStream inputStream = new FileInputStream(xzFile);
                            return new XZCompressorInputStream(inputStream);
                        }
                    }
                }
            }
            throw new IOException("File Resource [" + filename + "] Not Found");
        }
    }

    private class HttpResourceLocator implements ResourceLocator {
        private HttpClient httpClient;

        private HttpResourceLocator(Configuration config) {
            HttpConnectionManagerParams params = new HttpConnectionManagerParams();
            params.setDefaultMaxConnectionsPerHost(config.getInt(MAX_HTTP_CONNECTIONS, 20));
            params.setSoTimeout(config.getInt(SOCKET_TIMEOUT, 10000));
            params.setConnectionTimeout(config.getInt(CONNECTION_TIMEOUT, 4000));
            MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
            connectionManager.setParams(params);
            httpClient = new HttpClient(connectionManager);
        }

        public InputStream getResource(String url) throws IOException {
            GetMethod get = new GetMethod(url);
            try {
                int rc = httpClient.executeMethod(get);
                if (rc == HttpStatus.SC_OK) {
                    return new ByteArrayInputStream(get.getResponseBody());
                } else {
                    throw new IOException("Error Reading Resource [" + url + "]: " + HttpStatus.getStatusText(rc));
                }
            } catch (Exception e) {
                throw new IOException("Error Reading Resource [" + url + "]", e);
            } finally {
                get.releaseConnection();
            }
        }
    }


}


