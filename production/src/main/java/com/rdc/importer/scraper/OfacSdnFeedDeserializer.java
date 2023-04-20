package com.rdc.importer.scraper;


import com.rdc.importer.scrapian.ScrapianEngine;
import com.rdc.importer.scrapian.resolver.ScrapianResolver;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import net.sf.ehcache.Cache;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.digester.Digester;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

//import com.rdc.core.spring.SpringManager;
import com.rdc.importer.scrapian.ScrapianContext;
import com.rdc.importer.scrapian.ScrapianScript;
import com.rdc.importer.thirdparty.sdn.Sanctions;
//import com.rdc.rdcmodel.dao.MonitoredListDao;
//import com.rdc.rdcmodel.model.monitoredlist.LI_Scripts;
import com.rdc.scrape.ScrapeSession;

@Component
public class OfacSdnFeedDeserializer extends Deserializer {	
	//private static MonitoredListDao monitoredListDao;
	//private HashMap<String, String> monitoredListOfacSdnProgramMap;
    private ScrapianResolver scrapianResolver;
    private Configuration configuration;
    private boolean me = false;
	
    private static Logger logger = LogManager.getLogger(OfacSdnFeedDeserializer.class);
	   

    @Resource
    public void setConfiguration(Configuration configuration) {
        me = configuration.getBoolean("com.rdc.importer.me", false);
    }

//    @Resource
//    public void setMonitoredListDao(final MonitoredListDao monitoredListDao) {
//        this.monitoredListDao = monitoredListDao;
//    }

    @Resource
    public void setScrapianResolver(ScrapianResolver scrapianResolver) {
        this.scrapianResolver = scrapianResolver;
    }
    
//	@Override
//	public Cache deserializeToCache(InputStream inputStream, String encoding) throws Exception {
//		encoding = "UTF-8";
//		Reader reader = getReaderWithoutBOM(inputStream, encoding);
//		return deserialize(reader);
//	}
	
//	public Cache deserialize(Reader reader) throws Exception {
//		//monitoredListOfacSdnProgramMap = monitoredListDao.getMonitoredListOfacSdnProgramMap();
//
//		JAXBContext jaxbContext = JAXBContext.newInstance(Sanctions.class);
//
//		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//		Sanctions root =  (Sanctions)jaxbUnmarshaller.unmarshal(reader);
//        ScrapianScript script1 = null;
//		URL tempUrl = null;
//		//LI_Scripts script = null;
//		boolean useResolver = false;
//		final String key = "ofac_advanced_sub_process";
//        if(configuration ==null) {
//            configuration = new ScrapianResolver();//(Configuration) SpringManager.getInstance().getBeanFactory().getBean("configuration");
//            me = configuration.getBoolean("com.rdc.importer.me", false);
//        }
//        if(me){
//            useResolver = true;
//        }
//        if (useResolver) {
//            if(scrapianResolver ==null) {
//                scrapianResolver = (ScrapianResolver) SpringManager.getInstance().getBeanFactory().getBean("scrapianResolver");//new ScrapianResolver();
//            }
//            script1 = scrapianResolver.resolveScript(key);
//            tempUrl = script1.getUrl();
//        }
//        else {
//            ScrapianEngine se = new ScrapianEngine();
//            script = monitoredListDao.getLiScriptConfigByFolderName(key);
//            if (script == null) {
//                throw new Exception("Script config was not found for " + key);
//            }
//            if(script.getFileLocation() == null){
//                throw new Exception("Script file location not set for " + key);
//            }
//            if (script.getFileLocation().toUpperCase().contains("GITHUB.COM")) { //ex: https://github.com/RegDC/scripts/
//                tempUrl = se.getGitHubFileURL(script.getFileLocation(),script.getScriptName(),script.getFileVersion());
//            } else {
//                tempUrl = new URL(script.getFileLocation() + (script.getFileLocation().endsWith("/") ? "" : "/") + script.getScriptName() + (org.apache.commons.lang.StringUtils.isNotBlank(script.getFileVersion()) ? "?revision=" + script.getFileVersion() : (script.getFileLocation().startsWith("file://") ? "" : "?view=co")));
//            }
//        }
//        final URL url = tempUrl;
//        GroovyScriptEngine gse = new GroovyScriptEngine(new ResourceConnector() {
//            public URLConnection getResourceConnection(String resourceName) throws ResourceException {
//                if (key.equals(resourceName)) {
//                    try {
//                        logger.info("Fetching Script [" + url + "]");
//                        return url.openConnection();
//                    } catch (Exception e) {
//                        throw new ResourceException(e.getMessage(), e);
//                    }
//                }
//                throw new ResourceException("Resource[" + resourceName + "] not found");
//            }
//        });
//
//        ScrapianContext scrapianContext = (ScrapianContext) SpringManager.getInstance().getBeanFactory().getBean("scrapianContext");
//
//        ScrapeSession theSess = new ScrapeSession("ofac_sdn_feed");
//        scrapianContext.setScrapeSession(theSess);
//
//        Binding binding = new Binding();
//        binding.setVariable("context", scrapianContext);
//        binding.setVariable("root", root);
//        binding.setVariable("monitoredListOfacSdnProgramMap", monitoredListOfacSdnProgramMap);
//        gse.run(key, binding);
//
//		return theSess.getEntities();
//	}

    @Override
    protected void configureDigester(Digester digester) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected Cache getEntityCache(Object digested) {
        // TODO Auto-generated method stub
        return null;
    }
}
