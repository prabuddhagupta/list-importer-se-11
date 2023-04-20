package com.rdc.importer.scrapian.request;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.Resource;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class EasySSLProtocolSocketFactory implements SecureProtocolSocketFactory
{
   private SSLContext sslcontext = null;
   private static String ssl = "SSL";
   private static Logger logger = LogManager.getLogger(EasySSLProtocolSocketFactory.class);

   @Resource
   public void setConfiguration(Configuration configuration) {
       ssl = configuration.getString("com.rdc.importer.ssl", "TLSv1.2");
   }
   
   public EasySSLProtocolSocketFactory()
   { super(); }

   private static SSLContext createEasySSLContext()
   {
      try
      {
    	 //logger.info("Returning context with SSL type: " + ssl);
         SSLContext context = SSLContext.getInstance(ssl);//SSLContext.getInstance("SSL");
         context.init(  null,
                        new TrustManager[] { new EasyX509TrustManager()}, null );
         return context;
      }
      catch (Exception e)
      {
         LogManager.getLogger(SimpleHttpClient.class).error( e.getMessage(), e );
         throw new HttpClientError(e.toString());
      }
   }

   //-------------------------------------------------------------------------
   private SSLContext getSSLContext()
   {
      if( this.sslcontext == null)
        { this.sslcontext = createEasySSLContext(); }
      return this.sslcontext;
   }
   //--------------------------------------------------------------------------
   public Socket createSocket( String host, int port, InetAddress clientHost, int clientPort)
   throws IOException, UnknownHostException
   {
      return getSSLContext().getSocketFactory().createSocket( host, port, clientHost, clientPort );
   }
   //--------------------------------------------------------------------------
   public Socket createSocket( final String host,
                               final int port,
                               final InetAddress localAddress,
                               final int localPort,
                               final HttpConnectionParams params )
   throws IOException, UnknownHostException, ConnectTimeoutException
   {
      if( params == null)
        { throw new IllegalArgumentException("Parameters may not be null"); }
      int timeout = params.getConnectionTimeout();
      SocketFactory socketfactory = getSSLContext().getSocketFactory();
      if( timeout == 0)
        { return socketfactory.createSocket(host, port, localAddress, localPort); }
      else
      {
         Socket socket = socketfactory.createSocket();
         SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
         SocketAddress remoteaddr = new InetSocketAddress(host, port);
         socket.bind(localaddr);
         socket.connect(remoteaddr, timeout);
         return socket;
      }
   }
   //--------------------------------------------------------------------------
   public Socket createSocket(String host, int port)
   throws IOException, UnknownHostException
   { return getSSLContext().getSocketFactory().createSocket( host, port ); }
   //--------------------------------------------------------------------------
   public Socket createSocket( Socket socket, String host, int port, boolean autoClose)
   throws IOException, UnknownHostException
   { return getSSLContext().getSocketFactory().createSocket( socket, host, port, autoClose ); }
   //--------------------------------------------------------------------------
   public boolean equals(Object obj)
   {  return ((obj != null) && obj.getClass().equals(EasySSLProtocolSocketFactory.class)); }
   //--------------------------------------------------------------------------
   public int hashCode()
   {  return EasySSLProtocolSocketFactory.class.hashCode(); }
   //--------------------------------------------------------------------------
   private static class EasyX509TrustManager implements X509TrustManager
   {
      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

      @Override
      public X509Certificate[] getAcceptedIssuers()
      { return null; }
   }
}
