package com.rdc.importer.scrapian.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rdc.importer.misc.ResourceLocatorService;
import com.rdc.importer.scrapian.model.ByteArraySource;
import com.rdc.importer.scrapian.model.ScrapianRequest;
import com.rdc.importer.scrapian.model.StringSource;

@Component
@Scope("prototype")
public class UrlRequestInvoker implements RequestInvoker {
    private Logger logger = LogManager.getLogger(UrlRequestInvoker.class);
    private HttpClient client;
    private Integer retryCount;
    private Integer retryWait;
    private ResourceLocatorService resourceLocatorService;
    private String proxyServer;
    private String userAgent = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)";

    @Resource
    public void setResourceLocatorService(ResourceLocatorService resourceLocatorService) {
        this.resourceLocatorService = resourceLocatorService;
    }

    @Resource
    public void setConfiguration(Configuration config) {
        proxyServer = config.getString("com.rdc.importer.proxyServer");
    }

    public void setUserAgent( String userAgent )
    {
       if( StringUtils.isNotBlank( userAgent ) )
         { this.userAgent = userAgent; }
    }

    static {
    	Protocol.registerProtocol( "https", new Protocol( "https", new EasySSLProtocolSocketFactory(), 443));
        //Protocol.registerProtocol("https", new Protocol("https", new UntrustedSSLProtocolSocketFactory(), 443));
    }
    
    public void open(int socketTimeout, int connectionTimeout, Integer retryCount, boolean multithread) {
    	open(socketTimeout, connectionTimeout, retryCount, multithread, null);
    }

    public void open(int socketTimeout, int connectionTimeout, Integer retryCount, boolean multithread, Integer retryWait) {
        if (multithread) {
            HttpConnectionManagerParams params = new HttpConnectionManagerParams();
            params.setDefaultMaxConnectionsPerHost(10);
            params.setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, connectionTimeout);
            params.setParameter(HttpConnectionParams.SO_TIMEOUT, socketTimeout);
            MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
            connectionManager.setParams(params);
            client = new HttpClient(connectionManager);
        } else {
            client = new HttpClient(new SimpleHttpConnectionManager());
            client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, connectionTimeout);
            client.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, socketTimeout);
        }
        if (StringUtils.isNotBlank(proxyServer)) {
            int port = 80;
            String server = proxyServer;
            if (proxyServer.contains(":")) {
                port = Integer.valueOf(proxyServer.substring(proxyServer.indexOf(":") + 1));
                server = proxyServer.substring(0, proxyServer.indexOf(":"));
            }
            client.getHostConfiguration().setProxy(server, port);
        }

		if (retryWait != null) {
			this.retryWait = retryWait;
		} else {
			this.retryWait = 2500;
		}
        this.retryCount = retryCount;
    }

    public StringSource invoke(ScrapianRequest request, String encoding) throws Exception {
        if (encoding == null)
            return new StringSource(new String(download(request)));
        else
            return new StringSource(new String(download(request), encoding));
    }

    public ByteArraySource invokeBinary(ScrapianRequest request) throws Exception {
        return new ByteArraySource(download(request));
    }

    private byte[] download(ScrapianRequest request) throws Exception {
        if (request.getUrl().startsWith("file:")) {
            InputStream fileResource = resourceLocatorService.getResource(request.getUrl());
            return IOUtils.toByteArray(fileResource);
        } else {
            return executeRequest(request);
        }
    }

    private byte[] executeRequest(ScrapianRequest request) throws Exception {
        final int maxRetries = retryCount != null ? retryCount : 0;
        int wait = retryWait != null ? retryWait : 2500;
        for (int tries = 0; true; ++tries) {
            final HttpMethod method = createHttpMethod(request);
            if(StringUtils.isNotBlank(request.getRemoteSiteCookies())){
            	method.setRequestHeader("Cookie", request.getRemoteSiteCookies());
            }
            try {
                return runMethod(method, request.isIgnoreRedirects());
            } catch (final Exception e) {
                if (tries == maxRetries) {
                    throw e;
                }
                Thread.sleep(wait); // give server time to come back up
                logger.warn(e.getMessage() + ". Retrying...");
            }
        }
    }

    // releases 'method' before exiting
    private byte[] runMethod(HttpMethod method, boolean ignoreRedirects) throws Exception {
        URI uri = method.getURI();
        logger.info("Invoking[" + uri + "]");
        int statusCode = client.executeMethod(method);
        StatusLine sLine = method.getStatusLine();
        String statusLine = (sLine == null) ? "" : sLine.toString();
        String errorMessage = "Error invoking[" + uri + "] " + statusLine;
        if (statusCode != HttpStatus.SC_OK) {
            if (!(method instanceof GetMethod) && (statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_MOVED_PERMANENTLY)) {
                Header location = method.getResponseHeader("location");
                if (location == null) {
                    method.releaseConnection();
                    throw new Exception(errorMessage);
                } else {
                    if(!ignoreRedirects) {
                        String redirectUrl = location.getValue();
                        ScrapianRequest scrapianRedirectRequest = null;
                        String uriStr = uri.toString();

                        // Fully qualified case
                        if(StringUtils.startsWithIgnoreCase(redirectUrl, "http")) {
                            scrapianRedirectRequest = new ScrapianRequest(redirectUrl);
                        }

                        // Offset from site root case
                        else if(StringUtils.startsWithIgnoreCase(redirectUrl, "/")) {
                            int offset = "https://".length();
                            int pathIdx = uriStr.indexOf(uri.getPath(), offset);
                            String baseUrl = uriStr.substring(0, pathIdx);
                            redirectUrl = baseUrl + redirectUrl;
                            scrapianRedirectRequest = new ScrapianRequest(redirectUrl);
                        }

                        // Use last full path
                        else {
                            String baseUrl = uriStr.substring(0, uriStr.lastIndexOf("/"));
                            redirectUrl = baseUrl + "/" + redirectUrl;
                            scrapianRedirectRequest = new ScrapianRequest(redirectUrl);
                        }

                        // Redirects are always GET
                        scrapianRedirectRequest.setType("GET");
                        HttpMethod redirectMethod = createHttpMethod(scrapianRedirectRequest);
                        logger.info("Following Redirect [" + redirectUrl + "]");
                        method.releaseConnection();
                        return runMethod(redirectMethod, ignoreRedirects);
                    }
                }
            } else {
                method.releaseConnection();
                throw new Exception(errorMessage);
            }
        }
        byte[] response = IOUtils.toByteArray(method.getResponseBodyAsStream());
        method.releaseConnection();
        return response;
    }

    private HttpMethod createHttpMethod(ScrapianRequest request) {
        HttpMethod method;

        boolean isPost = "post".equalsIgnoreCase(request.getType());
        if (isPost) {
            method = new PostMethod(request.getUrl());
        } else {
            method = new GetMethod(request.getUrl());
            method.setFollowRedirects(true);
        }
        System.out.println( "\nUSER AGENT " + userAgent );
        method.addRequestHeader("User-Agent", userAgent );

        if(request.getHeaders() != null) {
            Map<String, String> headers = request.getHeaders();
            for(String header : headers.keySet()) {
                method.addRequestHeader(header, headers.get(header));
            }
        }

        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> entry : request.getParameters().entrySet()) {
                NameValuePair nameValuePair = new NameValuePair(entry.getKey(), entry.getValue());
                if (isPost) {
                    ((PostMethod) method).addParameter(nameValuePair);
                } else {
                    params.add(nameValuePair);
                }
            }
            if (!isPost) {
                method.setQueryString(params.toArray(new NameValuePair[params.size()]));
            }
        }
        return method;
    }


    private static class UntrustedSSLProtocolSocketFactory implements SecureProtocolSocketFactory {
        private SSLContext sslcontext = null;

        private static SSLContext createEasySSLContext() {
            try {
                SSLContext context = SSLContext.getInstance("SSL");
                context.init(null, new TrustManager[]{new UntrustedX509TrustManager(null)}, null);
                return context;
            } catch (Exception e) {
                throw new HttpClientError(e.toString());
            }
        }

        private SSLContext getSSLContext() {
            if (this.sslcontext == null) {
                this.sslcontext = createEasySSLContext();
            }
            return this.sslcontext;
        }

        public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException {
            return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
        }

        public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params) throws IOException {
            if (params == null) {
                throw new IllegalArgumentException("Parameters may not be null");
            }
            int timeout = params.getConnectionTimeout();
            SocketFactory socketfactory = getSSLContext().getSocketFactory();
            if (timeout == 0) {
                return socketfactory.createSocket(host, port, localAddress, localPort);
            } else {
                Socket socket = socketfactory.createSocket();
                SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
                SocketAddress remoteaddr = new InetSocketAddress(host, port);
                socket.bind(localaddr);
                socket.connect(remoteaddr, timeout);
                return socket;
            }
        }

        public Socket createSocket(String host, int port) throws IOException {
            return getSSLContext().getSocketFactory().createSocket(host, port);
        }

        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        public boolean equals(Object obj) {
            return ((obj != null) && obj.getClass().equals(UntrustedSSLProtocolSocketFactory.class));
        }

        public int hashCode() {
            return UntrustedSSLProtocolSocketFactory.class.hashCode();
        }
    }

    private static class UntrustedX509TrustManager implements X509TrustManager {
        private X509TrustManager standardTrustManager = null;

        public UntrustedX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
            super();
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keystore);
            TrustManager[] trustmanagers = factory.getTrustManagers();
            if (trustmanagers.length == 0) {
                throw new NoSuchAlgorithmException("no trust manager found");
            }
            this.standardTrustManager = (X509TrustManager) trustmanagers[0];
        }

        public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
            standardTrustManager.checkClientTrusted(certificates, authType);
        }

        public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
            if ((certificates != null) && (certificates.length == 1)) {
                certificates[0].checkValidity();
            } else {
                standardTrustManager.checkServerTrusted(certificates, authType);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return this.standardTrustManager.getAcceptedIssuers();
        }
    }
}