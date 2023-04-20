package com.rdc.importer.scrapian.request;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.net.*;
import javax.net.ssl.*;

import java.security.cert.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.httpclient.protocol.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.params.ClientPNames;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.log4j.LogManager;
import java.util.zip.GZIPInputStream;

public final class SimpleHttpClient
{
   private static final int DEFAULT_PORT = 80;

   static
   {
      Protocol.registerProtocol( "https", new Protocol( "https", new EasySSLProtocolSocketFactory(), 443));
   }
   private SimpleHttpClient(){}

   public static boolean isEmpty( String lStr )
   {
      return ( lStr == null || lStr.trim().length() < 1 );
   }
   //--------------------------------------------------------------------------
   private static void process_props( HttpClient pClient, Properties pProps )
   {
      HttpClient lClient         = pClient;
      Properties lProps          = new Properties();
      String     lConfigFilePath = System.getProperty( "http.config" );
      String     lProxyHost      = null;
      int        lProxyPort      = DEFAULT_PORT;

      // see if we have to use external properties file ( see sample at the bottom )
      //
      if( ! isEmpty( lConfigFilePath ) &&
          new File( lConfigFilePath ).exists() )
      {
         try
         {
            lProps.loadFromXML( new FileInputStream( lConfigFilePath ) );
            //System.out.println( lProps );
         }
         catch( Exception e )
         {
            LogManager.getLogger(SimpleHttpClient.class).error( e );
            throw new RuntimeException( "Failed to load " + lConfigFilePath );
         }
      }
      else

      if( pProps != null && pProps.size() > 0  )
        { lProps = pProps; }
      else
        { return; }

      // proxy setup
      //
      String lPropVal = lProps.getProperty( "proxy" );

      if( ! isEmpty( lPropVal ) )
      {
         String [] ss = lPropVal.split( ":" );
         lProxyHost = ss[ 0 ];

         if( ss.length > 1 && ! isEmpty( ss[ 1 ] ) )
           { lProxyPort = Integer.valueOf( ss[ 1 ] ); }

         lClient.getHostConfiguration().setProxy( lProxyHost, lProxyPort );
      }

      lPropVal = lProps.getProperty( "proxy.user" );

      if( ! isEmpty( lPropVal ) )
      {
         String lProxyUser = lPropVal.trim();
         String lProxyPwd  = lProps.getProperty( "proxy.pwd" );

         if( ! isEmpty( lProxyUser ) && ! isEmpty( lProxyPwd ) )
         {
            Credentials lCredentials = new UsernamePasswordCredentials( lProxyUser, lProxyPwd );
            AuthScope   lAuthScope = new AuthScope( lProxyHost, lProxyPort );
            lClient.getState().setProxyCredentials( lAuthScope, lCredentials );
         }
      }
   }
   //--------------------------------------------------------------------------
   private static HttpClient get_client( Properties pProps )
   {
      HttpClient lClient = new HttpClient( new SimpleHttpConnectionManager() );
      //HttpClient lClient = new HttpClient( new MultiThreadedHttpConnectionManager() );

      if( pProps.size() > 0 )
      {
         // socket and connetion timeouts setup
         //
          Object lPropVal = pProps.get( "cookiePolicy" );
          
          try
          {
             pProps.remove( "cookiePolicy" );
             if( lPropVal instanceof String )
             {
                 if(lPropVal.equals("true")){
                 lClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
                         CookiePolicy.BROWSER_COMPATIBILITY);
                 }
             } else if( lPropVal instanceof Boolean )
             {
                 if(lPropVal.equals(true)){
                 lClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
                         CookiePolicy.BROWSER_COMPATIBILITY);
                 }
             }
          }
          catch( Exception e )
          {
             LogManager.getLogger(SimpleHttpClient.class).error
             ( "Invalid cookiePolicy parameter " + e.getMessage() );
          }         
          
          lPropVal = pProps.get( "socketTimeout" );

          try
          {
             if( lPropVal instanceof String )
             {
                lClient.getParams()
                       .setParameter( HttpConnectionParams.SO_TIMEOUT,
                                      Integer.parseInt( ( String )lPropVal) );
             }
             else
             if( lPropVal instanceof Number )
             {
                lClient.getParams()
                       .setParameter( HttpConnectionParams.SO_TIMEOUT,
                                      ( ( Number )lPropVal ).intValue() );
             }
          }
          catch( Exception e )
          {
             LogManager.getLogger(SimpleHttpClient.class).error
             ( "Invalid socketTimeout parameter " + e.getMessage() );
             pProps.remove( "socketTimeout" );
          }

         lPropVal = pProps.get( "connectionTimeout" );

         try
         {
            if( lPropVal instanceof String )
            {
               lClient.getParams()
                      .setParameter( HttpConnectionParams.CONNECTION_TIMEOUT,
                                     Integer.parseInt( ( String )lPropVal) );
            }
            else
            if( lPropVal instanceof Number )
            {
               lClient.getParams()
                      .setParameter( HttpConnectionParams.CONNECTION_TIMEOUT,
                                     ( ( Number )lPropVal ).intValue() );
            }
         }
         catch( Exception e )
         {
            LogManager.getLogger(SimpleHttpClient.class).error
            ( "Invalid connectionTimeout parameter " + e.getMessage() );
            pProps.remove( "connectionTimeout" );
         }
      }
      return lClient;
   }
   //--------------------------------------------------------------------------
   //-- calls HTTP POST
   //--------------------------------------------------------------------------
   public static String invoke( Map<String,Object> pParams ) throws Exception
   {
      return invoke( pParams, ( Map<String,String> )null, null );
   }
   public static String invoke( Map<String,Object> pParams, String encoding ) throws Exception
   {
	      return invoke( pParams, ( Map<String,String> )null, encoding );
	   }
   //--------------------------------------------------------------------------
   public static byte [] invokeBinary( Map<String,Object> pParams ) throws Exception
   {
      String lUrl = null;

      if( pParams != null &&
          pParams.size() > 0 && !
          isEmpty( ( String )pParams.get( "url" ) ) )
      {
         lUrl = ( String )pParams.get( "url" );
         pParams.remove( "url" );
      }
      else
      {
         throw new RuntimeException( "URL is empty" );
      }

      if( ! is_already_encoded( lUrl ) )
        { lUrl = URIUtil.encodeQuery( lUrl ); } //System.out.println( lUrl );

      Properties lProps = new Properties();
      HttpMethod lMethod = new GetMethod( lUrl );

      lProps.putAll( pParams );
      HttpClient lClient = get_client( lProps );
      process_props( lClient, lProps );

      return execute_request( lClient, lMethod, lProps );
   }
   //--------------------------------------------------------------------------
   public static String invoke( Map<String,Object> pParams,
                                Map<String,String> pHeaders, String encoding ) throws Exception
   {
      String lUrl = null;

      if( pParams != null &&
          pParams.size() > 0 && !
          isEmpty( ( String )pParams.get( "url" ) ) )
      {
         lUrl = ( String )pParams.get( "url" );
         pParams.remove( "url" );
      }
      else
      {
         throw new RuntimeException( "URL is empty" );
      }

      return invoke( lUrl, pParams, pHeaders, encoding );
   }
   //--------------------------------------------------------------------------
   //-- calls HTTP POST
   //--------------------------------------------------------------------------
   public static String invoke( String pUrl, Map<String,Object> pParams,
                                             Map<String,String> pHeaders, String encoding ) throws Exception
   {
      String     lUrl    = pUrl;

      if( ! is_already_encoded( lUrl ) )
        { lUrl = URIUtil.encodeQuery( lUrl ); } //System.out.println( lUrl );

      HttpMethod lMethod = get_http_method( pParams, lUrl );
      Properties lProps  = new Properties();
      lProps.putAll( pParams );
      HttpClient lClient = get_client( lProps );
      process_props( lClient, lProps );

      add_parameters( lMethod, pParams );

      if( pParams.get( "headers" ) != null && pHeaders == null )
      {
         pHeaders = ( Map<String,String> )pParams.get( "headers" );
      }

      if( pHeaders != null && pHeaders.size() > 0 )
      {
         for( Map.Entry<String,String> header : pHeaders.entrySet() )
         {
             lMethod.addRequestHeader( header.getKey(), header.getValue() );
         }
      }

      byte [] lResult = execute_request( lClient, lMethod, lProps );

		pParams.put("responseHeaders", lProps.get("responseHeaders"));
		if (lProps.get("responseHeaders") != null && ((HashMap) lProps.get("responseHeaders")).get("Content-Encoding") != null) {
			String theEncoding = (String) ((ArrayList) ((HashMap) lProps.get("responseHeaders")).get("Content-Encoding")).get(0);
			if ("gzip".equals(theEncoding)) {
				InputStream is = new ByteArrayInputStream(lResult);
				GZIPInputStream gis = new GZIPInputStream(is);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len;
				while ((len = gis.read(buffer)) != -1) {
					os.write(buffer, 0, len);
				}
				is.close();
				os.close();
				gis.close();
				lResult = os.toByteArray();
			}
		}
      
      if( lResult == null )
      {
         LogManager.getLogger(SimpleHttpClient.class).info
         ( "SimpleHttpClient.invoke(): execute() returned null");
      }
      return ( lResult != null ? (StringUtils.isNotBlank(encoding) ? new String( lResult , encoding) : new String(lResult)) : null );
   }
   //--------------------------------------------------------------------------
   private static byte [] execute_request( HttpClient pClient, 
                                           HttpMethod pMethod,
                                           Properties pProps )
   {
      int lRetryCount = pProps.get( "retryCount" ) == null ?
                        3 :
                        (( Number )pProps.get( "retryCount" )).intValue();
      byte [] lResult = null;

      for( int i = 0; i < lRetryCount; i++ )
      {
         try
         {
            if( ( lResult = execute( pClient, pMethod, pProps ) ) != null )
              { break; }
         }
         catch( Exception e )
         {
            if( i >= lRetryCount )
              { throw new RuntimeException( e.getMessage() ); }
            System.out.println( "Attempt " + ( i + 1 ) );
            try{ Thread.sleep( 2500 ); }catch( Exception ie ){}
         }
      }

      if( lResult == null )
      {
         String lUrl = null;
         try
         { lUrl = pMethod.getURI().toString(); }
         catch( Exception e )
         { lUrl = "<unknown>"; }
         System.out.println( " request to " + lUrl + " returned null");
      }
      return lResult;
   }
   //--------------------------------------------------------------------------
   private static byte [] execute( HttpClient pClient, HttpMethod pMethod, Properties pProps )
   {
      return execute( pClient, pMethod, true, pProps );
   }
   //--------------------------------------------------------------------------
   private static byte [] execute( HttpClient pClient, HttpMethod pMethod, boolean pRecurse,
                                                                           Properties pProps )
   {
      HttpMethod lMethod = pMethod;
      HttpClient lClient = pClient;

      byte [] lOutput = null;
      boolean allowRedirects = true;
      if (pProps != null) {
          String redirectParam = (String) pProps.get("allowRedirects");
          if (redirectParam != null) {
             if (redirectParam.equalsIgnoreCase("false"))
                allowRedirects = false;
             pProps.remove("allowRedirects");
          }
      }

      try
      {
          lMethod.setFollowRedirects(allowRedirects);
          lClient.executeMethod( lMethod );
         //System.out.printf( "HTTP STATUS %d\n", lMethod.getStatusCode());

         Map<String, ArrayList<String>> lResponseHeaders = dump_headers( lMethod, false );
         if( pProps != null )
           { pProps.put( "responseHeaders", lResponseHeaders ); }

         if( lMethod.getStatusCode() == HttpStatus.SC_OK )
         {
            lOutput = get_binary_data( lMethod );
         }
         else

         if( allowRedirects && ( lMethod.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY ||
             lMethod.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY ) )
         {
            String lRedirectUrl = lMethod.getResponseHeader("location").getValue();
            String lFinalUrl = null,
                   lBaseUrl  = null;

            if( isEmpty( lRedirectUrl ) )
            {
               throw new
                     RuntimeException("Error invoking[" + lMethod.getURI() +
                                      "] " + ( lMethod.getStatusLine() != null ?
                                               lMethod.getStatusLine().toString() : ""));
            }

            if( lRedirectUrl.toLowerCase().startsWith( "http" ) )
              { lFinalUrl = lRedirectUrl; }
            else
            // Offset from site root case
            if( lRedirectUrl.toLowerCase().startsWith( "/" ) )
            {
                lBaseUrl = lMethod.getURI().toString()
                                  .substring( 0,
                                              lMethod.getURI().toString()
                                                     .indexOf( lMethod.getURI().getPath() )
                                            );
                lFinalUrl = lBaseUrl + "/" + lRedirectUrl;
            }
            else
            // Use last full path
            {
                lBaseUrl = lMethod.getURI().toString()
                                  .substring(0, lMethod.getURI().toString().lastIndexOf("/"));
                lFinalUrl = lBaseUrl + "/" + lRedirectUrl;
            }

            lMethod = new GetMethod( lFinalUrl );
            lClient.executeMethod( lMethod );

            if( lMethod.getStatusCode() == HttpStatus.SC_OK )
              { lOutput = get_binary_data( lMethod ); }
         }
         else

         // some servers does not allow POST, switch to GET
         // call once recursively execute()
         //
         if( lMethod.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED && pRecurse )
         {
            lOutput = execute( pClient,
                               new GetMethod( lMethod.getURI().getEscapedURI() ), false, pProps );
         }
         else
         {
            LogManager.getLogger(SimpleHttpClient.class).info
            ( "HTTP STATUS " + lMethod.getStatusCode() );
            System.out.printf( "HTTP STATUS %d %s\n", lMethod.getStatusCode(),
                                                      lMethod.getStatusText());
         }
      }
      catch( Exception ioe )
      {
         LogManager.getLogger(SimpleHttpClient.class).error( ioe );
      }
      finally
      {
         lMethod.releaseConnection();
      }

      return lOutput;
   }
   //--------------------------------------------------------------------------
   private static byte [] get_binary_data( HttpMethod pMethod ) throws Exception
   {
      return org.apache.commons.io.IOUtils.toByteArray( pMethod.getResponseBodyAsStream() );
   }
   //--------------------------------------------------------------------------
   private static boolean is_already_encoded( String pUrl )
   {
      String lDecoded = null;

      try
      {
         lDecoded = URIUtil.decode( pUrl );
      }
      catch( Exception e )
      {
         //System.err.println( "decode failed for URL " + pUrl + " " + e );
      }

      return ( lDecoded != null );
   }
   //--------------------------------------------------------------------------
   private static Map<String, ArrayList<String>> dump_headers( HttpMethod pMethod,
                                                                  boolean lRequest )
   {
      Header [] headers = lRequest ? pMethod.getRequestHeaders() :
                                     pMethod.getResponseHeaders();

      Map<String, ArrayList<String>> lHeaders = new HashMap<String, ArrayList<String>>();

      for( Header h : headers )
      {
         final String lKey = h.getName();
         final String lVal = h.getValue();

         if( ! lHeaders.containsKey( lKey ) )
         {  final ArrayList<String> lAlist = new ArrayList<String>();
            lAlist.add( lVal );
            lHeaders.put( lKey, lAlist );
         }
         else
         {
            lHeaders.get( lKey ).add( lVal );
         }
      }

      return lHeaders;
   }
   //--------------------------------------------------------------------------
   private static HttpMethod get_http_method( Map<String,Object> pParams, String pUrl )
   {
      HttpMethod lMethod = null;
      String lType = null;

      try
      {
         if( ! isEmpty( ( String )pParams.get( "type" ) ) )
         {
            lType = ( String )pParams.get( "type" );

            // there could be other http methods besides post and get but for now
            // we are ignoring them
            //
            if( lType.equalsIgnoreCase( "POST" ) )
              { lMethod = new PostMethod( pUrl ); }
            else
              { lMethod = new GetMethod( pUrl ); }

            pParams.remove( "type" );
         }
         else
         {
            lMethod = new GetMethod( pUrl );
            lType = "GET";
         }

      }
      catch( Exception e )
      {
         LogManager.getLogger(SimpleHttpClient.class).error( e );
      }
      return lMethod;
   }
   //--------------------------------------------------------------------------
   private static void add_parameters( HttpMethod pMethod, Map<String,Object> pParams )
   {
      Iterator iterator = pParams.entrySet().iterator();

      StringBuilder lUrlParams = new StringBuilder();

      while( iterator.hasNext() )
      {
         String key = null, value = null;
         Object obj = null;

         // prevent system from crashing when passed parameter is not string
         try
         {
            Map.Entry<String, Object> pair = ( Map.Entry<String,Object> )iterator.next();
            key = pair.getKey();
            obj = pair.getValue();

            if( obj instanceof String && ! key.equalsIgnoreCase( "type" )
                                      && ! key.equalsIgnoreCase( "proxy" )
                                      && ! key.equalsIgnoreCase( "allowRedirects" ))
            {
               value = ( String )obj;

               if( pMethod instanceof PostMethod )
               {
                  ( ( PostMethod )pMethod ).addParameter( key, value );
               }
               else

               if( pMethod instanceof GetMethod )
               {
                  lUrlParams.append( key + '=' + value + '&' );
               }
            }
         }
         catch( Exception e )
         {
            LogManager.getLogger(SimpleHttpClient.class).error( "SimpleHttpClient.add_parameter(): " + e );
            LogManager.getLogger(SimpleHttpClient.class).info( "skipping key " + key );
         }
      }

      if( pParams.get( "multipart" ) != null &&
          pParams.get( "multipart" ) instanceof Map &&
          pMethod instanceof PostMethod )
      {
         Map<String,String> lMap = ( Map<String,String> )pParams.get( "multipart" );

         if( lMap.size() > 0 )
         {
            Part [] lParts = new Part[ lMap.size() ];
            int i = 0;
            for( Map.Entry<String,String> pair : lMap.entrySet() )
            { //System.out.println( "MULTIPART " + pair.getKey() + '\n' + pair.getValue() );
               lParts[ i++ ] = new StringPart( pair.getKey(), pair.getValue() );
            }
            ( ( PostMethod )pMethod ).setRequestEntity( new MultipartRequestEntity( lParts, pMethod.getParams()));
         }
      }

      if( lUrlParams.length() > 0 && pMethod instanceof GetMethod )
      {
         lUrlParams.deleteCharAt( lUrlParams.length() - 1 );
         pMethod.setQueryString( pMethod.getQueryString() + "&" + lUrlParams.toString() );
         //try{ System.out.println( "GET URI " + pMethod.getURI().toString() ); }
         //catch( Exception e ){ LogManager.getLogger(SimpleHttpClient.class).error( e ); }
      }
   }
   //--------------------------------------------------------------------------
   public static void main( String [] args ) throws Exception
   {
      if( args.length < 1 )
      {
         System.out.println( "\njava -Dhttp.config=\"my-config-file-path.xml\" t.TestHttpClient <url>" );
         System.out.println( "IMPORTANT! make sure your url parameters starts with http or https" );
         return;
      }

      System.out.println( invoke( args[0], ( Map<String,Object> )null,
                                           ( Map<String,String> )null, null ) );
   }
}

/*
  content of http.properties.xml file

   <?xml version="1.0" encoding="UTF-8"?>
   <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
   <properties>
      <entry key="proxy">proxy.rdc.com:8080</entry>
      <entry key="proxy.user"></entry>
      <entry key="proxy.pwd"></entry>
   </properties>
*/
