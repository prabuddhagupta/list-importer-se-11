package com.rdc.importer.scrapian.request;

import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;


public class MyHttpInvoker {
    private String httpUrl;
    private DefaultHttpClient httpClient;
    static HttpResponse response;
    private HttpUriRequest httpRequest;
    private int retryCount = 3;
    // static int count = 0;
    private Logger logger = LogManager.getLogger(HttpInvoker.class);

    public MyHttpInvoker(String url) throws Exception {
        httpClient = new DefaultHttpClient();
        this.httpUrl = url;
        httpRequest = new HttpGet(getUrl());
    }

    public MyHttpInvoker() {
        httpClient = new DefaultHttpClient();
    }

    public String getCookies() {
        String cookieStr = "";
        List<Cookie> cookies = httpClient.getCookieStore().getCookies();
        for (Cookie c : cookies) {
            cookieStr += c.getName() + "=" + c.getValue() + "; ";
        }
        return cookieStr.replaceAll(";\\s*$", "");
    }

    public String getUrl() throws Exception {
        if (httpUrl == null) {
            throw new Exception("Http Url is not initialized.");
        }
        return httpUrl;
    }

    public void removeProxy() {
        httpClient = new DefaultHttpClient();
    }

    public void setRetryCount(int count) {
        this.retryCount = count;
    }

    public void setConnectionTimeOut(int milisec) {
        httpClient.getParams().setParameter(
            HttpConnectionParams.CONNECTION_TIMEOUT, milisec);
    }

    public void setSocketTimeOut(int milisec) {
        httpClient.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT,
            milisec);
    }

    public void setMultiPartPostParams(HttpEntity mEntity) throws Exception {
        HttpPost httpPost = new HttpPost(getUrl());
        httpPost.setEntity(mEntity);
        httpRequest = httpPost;// setNewRequest(httpPost);
    }

    public void setPostParams(Map<String, String> params) throws Exception {  //prob how to post param via apache
        HttpPost httpPost = new HttpPost(getUrl());
        List<BasicNameValuePair> nmList = new ArrayList<BasicNameValuePair>();

        Set<Map.Entry<String, String>> eSet = params.entrySet();
        for (Map.Entry<String, String> e : eSet) {
            nmList.add(new BasicNameValuePair(e.getKey(), e.getValue()));
            // sEntity.addParameter(e.getKey(), e.getValue());
        }
        UrlEncodedFormEntity sEntity = new UrlEncodedFormEntity(nmList);
        httpPost.setEntity(sEntity);
        httpRequest = setNewRequest(httpPost);
        HttpParams par = httpRequest.getParams();
        for (Map.Entry<String, String> e : eSet) {
            par.setParameter(e.getKey(), e.getValue());
            // sEntity.addParameter(e.getKey(), e.getValue());
        }
        // httpRequest.setParams((HttpParams) params);
    }

    public void setProxy(String proxyIp, int proxyPort, Proxy.Type proxyType) {
        if (proxyType.equals(Proxy.Type.HTTP)) {
            removeProxy();
            HttpHost proxy = new HttpHost(proxyIp, proxyPort);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                proxy);
        } else if (proxyType.equals(Proxy.Type.SOCKS)) {
            removeProxy();
            httpClient.getParams().setParameter("socks.host", proxyIp);
            httpClient.getParams().setParameter("socks.port", proxyPort);
            httpClient
                .getConnectionManager()
                .getSchemeRegistry()
                .register(
                    new Scheme("http", 80, new MySSLSocketFactory1.MySchemeSocketFactory()));
        }
    }

    private HttpUriRequest setNewRequest(HttpUriRequest newReq) {
        Header[] hList = httpRequest.getAllHeaders();
        for (Header h : hList) {
            newReq.addHeader(h);
        }
        return newReq;
    }

    public void setUrl(String httpUrl) throws Exception {
        this.httpUrl = httpUrl;
        httpRequest = new HttpGet(getUrl());// setNewRequest(new
        // GetMethod(httpUrl));
    }

    public void addPresetHeaderSet() {
        httpRequest.setHeader("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        httpRequest.setHeader("Accept-Language", "en-US,en;q=0.8");
        httpRequest.setHeader("Connection", "keep-alive");
        httpRequest
            .setHeader(
                "User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36");
    }

    public void addReqestHeader(String key, String value) {
        httpRequest.setHeader(key, value);
    }

    public String getRequestHeader(String key) {
        return httpRequest.getFirstHeader(key).getValue();
    }

    public HashMap<String, String> getResponseHeaders() throws Exception {
        return headersToMap(httpRequest.getAllHeaders());
    }

    public HashMap<String, String> getRequestHeaders() throws Exception {
        return headersToMap(httpRequest.getAllHeaders());
    }

    private HashMap<String, String> headersToMap(org.apache.http.Header[] headers) {
        HashMap<String, String> hList = new HashMap<String, String>();
        for (org.apache.http.Header h : headers) {
            hList.put(h.getName(), h.getValue());
        }
        return hList;
    }

    public void enableRedirection() {
        httpClient.setRedirectStrategy(new LaxRedirectStrategy());
    }

    public void allowCircularRedirection() {
        httpClient.getParams().setBooleanParameter(
            HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
    }

    public HttpResponse getHttpResponse() throws IOException, Exception {
        // logger.info("Invoking Url: " + httpUrl);

        Callable<HttpResponse> callable = () -> {
            //HttpResponse response = null;
            /*long start = System.currentTimeMillis();
            long end = start + 180 * 1000;*/
            //while (System.currentTimeMillis() < end) {
            logger.info("Invoking Url inside: " + httpUrl);
            response = httpClient.execute(httpRequest);
            /*Thread.sleep(40000);
            wait(40000);*/

            // }
            return response;
        };

        /*Runnable task1 = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Invoking Url: " + httpUrl);
                    response = httpClient.execute(httpRequest);
                    Thread.sleep(40000);
                    System.out.println("Response 2: " + response);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //public HttpResponse getResponse(){ return  response;}
        };*/
        //logger.info(" Inside While Invoking Url: " + httpUrl);

        /*new Thread(task1).start();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        //Future<HttpResponse> future = scheduler.submit(new Callable<HttpResponse>() {
        ScheduledFuture<?> future1 = scheduler.scheduleWithFixedDelay(task1, 60, 20, TimeUnit.SECONDS);
        while (count < retryCount) {
            try {
                System.out.println("count :" + count);
                System.out.println("Invoking Url 1 :" + httpUrl);
                System.out.println("Response :" + response);
                count = retryCount;
                Thread.sleep(60000);
            } catch (Exception e) {
                if (count + 1 < retryCount) {
                    System.out.println("Retrying Url: " + httpUrl);
                }
                future1.cancel(true);
            }
            count++;
        }*/
       /* System.out.println("Runnable: "+response);
        int count = 0;
        while(count < retryCount){
            try {
                System.out.println("Runnable Response: "+ response);
                count = retryCount;
            }catch (Exception e){
                if (count + 1 < retryCount) {
                    System.out.println("Retrying Url: " + httpUrl);
                }
            }
            count++;
        }*/
        //ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        //ExecutorService scheduler = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());;
        ExecutorService scheduler = Executors.newSingleThreadExecutor();
        Future<HttpResponse> future = null;

       // try{
          //future = scheduler.submit(callable);
          future = scheduler.submit(callable);
          Thread.sleep(40000);
       /* }catch (Exception e){
            e.printStackTrace();
        }*/
        int count = 0;
        // HttpResponse response = null;
        if (future == null) return null;

        while (count < retryCount) {
            if (future.isDone()) {
                try {
                    synchronized (this) {
                        response = future.get();
                        System.out.println("Response of Loop: " + response);
                        count = retryCount;
                        System.out.println("Count: " + count);
                        break;
                        //scheduler.shutdown();
                    }
                } catch (Exception e) {
                    if (count + 1 < retryCount) {
                        System.out.println("Retrying Url: " + httpUrl);
                    } /*else {
                            Thread.sleep(120000);
                            httpResponse = httpClient.execute(httpRequest);
                            count = 0;
                            // throw e;
                        }*/
                }
            }else {
                //System.out.println("After " + afterSeconds-- + " seconds,get the future returns value.");
                Thread.sleep(1000);
            }
            count++;
//           scheduler.shutdown();
            // updateCookies();
        }
        // occurs

        /*ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<HttpResponse> countDown = scheduler.schedule(new Callable<HttpResponse>() {
            @Override
            public HttpResponse call() throws Exception {
                int count = 0;
                HttpResponse httpResponse = null;
                while (count < retryCount) {
                    logger.info(" Inside While Invoking Url: " + httpUrl);
                    try {
                        httpResponse = httpClient.execute(httpRequest);
                        count = retryCount;// this will not be executed if exception
                        // occurs
                        // return countDown.get();
                    } catch (Exception e) {
                        if (count + 1 < retryCount) {
                            System.out.println("Retrying Url: " + httpUrl);
                        } else {
                            Thread.sleep(120000);
                            httpResponse = httpClient.execute(httpRequest);
                            count = 0;
                            // throw e;
                        }
                    }
                    // updateCookies();
                    count++;
                }
                return httpResponse;
            }
        }, 10, TimeUnit.SECONDS);
        return countDown.get();*//*
    }*/
        return response;
    }

    public String getStringData() throws IOException, Exception {

        HttpResponse hResponse = getHttpResponse();
        return EntityUtils.toString(hResponse.getEntity(), "UTF-8");
    }

    public InputStream getData() throws IOException, Exception {
        return getHttpResponse().getEntity().getContent();
    }

    public int getStatusCode() throws Exception {
        return this.getHttpResponse().getStatusLine().getStatusCode();

    }

    public void releaseConnection() {
        httpRequest.abort();
    }

    public void close() {
        try {
            if (!httpRequest.isAborted()) {
                httpRequest.abort();
            }
            // httpRequest.releaseConnection();
            httpClient.getConnectionManager().shutdown();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void autoAcceptAllSslCert() throws KeyManagementException,
        NoSuchAlgorithmException {
        /*
         * Protocol.registerProtocol("https", new Protocol("https",
         * getSocketFacotry(), 443));
         */
        httpClient = getNewHttpClient();
    }

    private ProtocolSocketFactory getSocketFacotry()
        throws KeyManagementException, NoSuchAlgorithmException {
        final SSLContext ctx = SSLContext.getInstance("TLS");
        X509TrustManager tm = getForcedTrustManager();
        ctx.init(null, new TrustManager[]{tm}, null);

        SecureProtocolSocketFactory psf = new SecureProtocolSocketFactory() {

            private Socket getSocket() throws IOException {
                return ctx.getSocketFactory().createSocket();
            }

            @Override
            public Socket createSocket(String arg0, int arg1) throws IOException,
                UnknownHostException {
                return getSocket();
            }

            @Override
            public Socket createSocket(String arg0, int arg1, InetAddress arg2,
                                       int arg3) throws IOException, UnknownHostException {
                return getSocket();
            }

            @Override
            public Socket createSocket(String s, int i,
                                       InetAddress inetAddress, int i1,
                                       org.apache.commons.httpclient.params.HttpConnectionParams httpConnectionParams)
                throws IOException, UnknownHostException,
                org.apache.commons.httpclient.ConnectTimeoutException {
                return null;
            }

            @Override
            public Socket createSocket(Socket arg0, String arg1, int arg2,
                                       boolean arg3) throws IOException, UnknownHostException {
                return getSocket();
            }
        };
        return psf;
    }

    private X509TrustManager getForcedTrustManager() {
        X509TrustManager tm = new X509TrustManager() {

            public void checkClientTrusted(X509Certificate[] xcs, String string)
                throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] xcs, String string)
                throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        return tm;
    }

    private DefaultHttpClient getNewHttpClient() {
        try {
            // KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            // trustStore.load(null, null);
            MySSLSocketFactory1 sf = new MySSLSocketFactory1();
            sf.setHostnameVerifier(MySSLSocketFactory1.ALLOW_ALL_HOSTNAME_VERIFIER);

            BasicHttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();

            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params,
                registry);
            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }
}

class MySSLSocketFactory1 extends SSLSocketFactory {
    private static SSLContext sslContext;

    MySSLSocketFactory1() throws NoSuchAlgorithmException,
        KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super(sslContext = SSLContext.getInstance("TLS"));

        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[]{tm}, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port,
                               boolean autoClose) throws IOException, UnknownHostException {
        return sslContext.getSocketFactory().createSocket(socket, host, port,
            autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }

    static class MySchemeSocketFactory implements SchemeSocketFactory {

        @Override
        public Socket createSocket(final HttpParams params) throws IOException {
            if (params == null) {
                throw new IllegalArgumentException(
                    "HTTP parameters may not be null");
            }
            String proxyHost = (String) params.getParameter("socks.host");
            Integer proxyPort = (Integer) params.getParameter("socks.port");

            InetSocketAddress socksaddr = new InetSocketAddress(proxyHost,
                proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
            return new Socket(proxy);
        }

        @Override
        public Socket connectSocket(final Socket socket,
                                    final InetSocketAddress remoteAddress,
                                    final InetSocketAddress localAddress, final HttpParams params)
            throws IOException, UnknownHostException,
            ConnectTimeoutException {
            if (remoteAddress == null) {
                throw new IllegalArgumentException(
                    "Remote address may not be null");
            }
            if (params == null) {
                throw new IllegalArgumentException(
                    "HTTP parameters may not be null");
            }
            Socket sock;
            if (socket != null) {
                sock = socket;
            } else {
                sock = createSocket(params);
            }
            if (localAddress != null) {
                sock.setReuseAddress(HttpConnectionParams
                    .getSoReuseaddr(params));
                sock.bind(localAddress);
            }
            int timeout = HttpConnectionParams.getConnectionTimeout(params);
            try {
                sock.connect(remoteAddress, timeout);
            } catch (SocketTimeoutException ex) {
                throw new ConnectTimeoutException("Connect to "
                    + remoteAddress.getHostName() + "/"
                    + remoteAddress.getAddress() + " timed out");
            }
            return sock;
        }

        @Override
        public boolean isSecure(final Socket sock)
            throws IllegalArgumentException {
            return false;
        }
    }

}