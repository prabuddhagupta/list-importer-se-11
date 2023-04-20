

package com.rdc.importer.scrapian;

import com.rdc.importer.scraper.Deserializer;
import com.rdc.importer.scraper.EntityDeserializer;
import com.rdc.importer.scraper.EntityValidationService;
import com.rdc.importer.scrapian.resolver.ScrapianResolver;
import com.rdc.importer.scrapian.util.EhCacheUtil;
import com.rdc.importer.scrapian.util.GitHubCacheUtil;
import com.rdc.scrape.ScrapeSession;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import net.sf.ehcache.Cache;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScrapianEngine implements BeanFactoryAware {
    private ScrapianResolver scrapianResolver;
    private static Logger logger = Logger.getLogger(ScrapianEngine.class);
    private BeanFactory beanFactory;
    private String [] cmdArgs;
    private String localBaseDir;
    private boolean me = false;

	   private static String DESERIALIZER_THIRD_PARTY = "thirdparty";
	   private static String DESERIALIZER_DOW = "dj_soc";
	   private static EntityValidationService entityValidationService;

    @Resource
    public void setConfiguration(Configuration configuration) {
        System.setProperty("com.rdc.importer.scrapian.PdfWorkDir", configuration.getString("com.rdc.importer.scrapian.PdfWorkDir", "/tmp"));
        me = configuration.getBoolean("com.rdc.importer.me", false);
        localBaseDir = configuration.getString("com.rdc.importer.scrapian.localBaseDir");
    }

    @Resource
    public void setScrapianResolver(ScrapianResolver scrapianResolver) {
        this.scrapianResolver = scrapianResolver;
    }
    
    @Resource
    public void setEntityValidationService(EntityValidationService entityValidationService) {
        this.entityValidationService = entityValidationService;
    }
    
    public static void main(String[] args) throws Exception {   
        if ("svn".equals(System.getProperty("scrapian.resolver"))) {
            if (System.getProperty("com.rdc.importer.scrapian.configUrl") == null) {
                System.setProperty("com.rdc.importer.scrapian.configUrl", "http://svn/viewvc/java/list_importer/trunk/scrapian/src/main/resources/scrapian-config-qa.xml?view=co");
            }
        } else {
            System.setProperty("scrapian.resolver", "classpath");
        }


        ScrapianEngine scrapianEngine = (ScrapianEngine) new ClassPathXmlApplicationContext("list_importer-context.xml").getBean("scrapianEngine");

        scrapianEngine.setCmdArgs(args);
        if (args.length > 0) {
            ScrapeSession scrapeSession = new ScrapeSession(args[0]);
            scrapianEngine.scrape(args[0], scrapeSession);

            int size = scrapeSession.getEntities().getSize();

            if (scrapeSession.isIgnoreScrapeSession()) {
                logger.info("Ignoring scrape session for list[" + args[0] + "] and exiting");
                System.exit(0);
            }

            //Note: the 3rd party groovy scripts do not populate the ScrapeSession entities, so there's not much
            // point in continuing from here.
			if (scrapeSession.isThirdPartySupplier()) {}



            InputStream input;
            Exception ex = null;
            if (args.length > 1) {
                File outputFile = new File((StringUtils.isNotBlank(scrapianEngine.getLocalBaseDir()) && !args[1].replaceAll("\\\\", "/").contains("/") ? scrapianEngine.getLocalBaseDir() : "") + args[1]);
                outputFile.getParentFile().mkdirs();
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                try {
                    scrapeSession.dump(outputStream, true, true);
                } catch (Exception e) {
                    logger.info(e.getMessage());
                    ex = e;
                } finally {

                    IOUtils.closeQuietly(outputStream);
                }
                input = new FileInputStream(outputFile);
            } else {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try{
                    scrapeSession.dump(output, false, true);
                } catch (Exception e) {
                    ex = e;
                } 
                input = new ByteArrayInputStream(output.toByteArray());
            }
            
			try{
	            logger.info("Making EXP report");
	            String fileName = args[1].replaceAll("\\\\", "/");
                String baseDir = fileName.contains("/") ? fileName.substring(0, fileName.lastIndexOf('/') +1) + "EXP/": (StringUtils.isNotBlank(scrapianEngine.getLocalBaseDir()) ? scrapianEngine.getLocalBaseDir() + "EXP/" : "/EXP/");
	            fileName = fileName.contains("/") ? fileName.substring(fileName.lastIndexOf('/') +1) : fileName;
				new ExportReport().new ExpRunnable().toReport(scrapeSession.getEntities(), args[0], baseDir, fileName.replace(".xml", ""));
				}
				catch(Exception e){
					e.printStackTrace();
				}

            EntityValidationService entityValidationService = new EntityValidationService();
            List<String> errors = entityValidationService.validate(scrapeSession.getEntities());
            if (!errors.isEmpty()) {
                StringBuffer buffer = new StringBuffer();
                for (String error : errors) {
                    buffer.append(error).append("\n");
                }
                throw new Exception("Validation Errors Occurred:" + errors);
            }
            if(ex != null){
                throw ex;
            }

            Deserializer entityDeserializer = new EntityDeserializer();
            Cache deserialized = entityDeserializer.deserializeToCache(input, scrapeSession.getEncoding());
            if(deserialized != null){
                EhCacheUtil.getCacheManager().removeCache(deserialized.getName());
            }
            scrapeSession.getEntities().removeAll();
        } else {
            System.out.println("Usage: com.rdc.importer.scrapian.ScrapianEngine <script key> <outputfile> <deserializer>");
        }
        System.exit(0);
    }

    public void scrape(final String key, ScrapeSession scrapeSession, HashMap<String, Object> map) throws Exception {
    	scrape(key, scrapeSession, map, false);
    }

	public void scrape(final String key, ScrapeSession scrapeSession, HashMap<String, Object> map, boolean useResolver) throws Exception {
		URL tempUrl = null;
		ScrapianScript script1 = null;
		useResolver = true;
		if (useResolver) {
			script1 = scrapianResolver.resolveScript(key);
			tempUrl = script1.getUrl();
		} else {}

		final URL url = tempUrl;
        GroovyScriptEngine gse = new GroovyScriptEngine(new ResourceConnector() {
            public URLConnection getResourceConnection(String resourceName) throws ResourceException {
                if (key.equals(resourceName)) {
                    try {
                        logger.info("Fetching Script [" + url + "]");
                        return url.openConnection();
                    } catch (Exception e) {
                        throw new ResourceException(e.getMessage(), e);
                    }
                }
                throw new ResourceException("Resource[" + resourceName + "] not found");
            }
        });

        ScrapianContext scrapianContext = (ScrapianContext) beanFactory.getBean("scrapianContext");
        if(useResolver){
            scrapianContext.setScriptParams(script1.getParams());
        } else {}
        scrapianContext.setScrapeSession(scrapeSession);

        Binding binding = new Binding();
        binding.setVariable("context", scrapianContext);
        binding.setVariable("args", cmdArgs);
        binding.setVariable("beanFactory", beanFactory);
        binding.setVariable("chromeUrl", System.getProperty("com.rdc.importer.selenium.chromeUrl"));
		if (map != null && map.size() > 0) {
			Iterator<Entry<String, Object>> i = map.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, Object> e = i.next();
				binding.setVariable((String) e.getKey(), e.getValue());
			}
		}
        gse.run(key, binding);
    }

    //To read files from github --Start
    public URL getGitHubFileURL(String fileLocation, String scriptName, String fileVersion) throws Exception {
        URL tempUrl = null;
        Pattern p = Pattern.compile("https*://github\\.com/([^/]+)/([^/]+)(?:/|$)");
        Matcher m = p.matcher(fileLocation);
        if (m.find()) {
            String gitHubRepoUser = m.group(1);
            String gitHubRepoName = m.group(2);

            String gitHubPath = GitHubCacheUtil.getResourceFilePath(gitHubRepoUser, gitHubRepoName, scriptName,
                    StringUtils.isNotBlank(fileVersion) ? fileVersion : "HEAD");
            if (StringUtils.isNotBlank(gitHubPath)) {
                tempUrl = new URL(gitHubPath);
            } else {
                throw new Exception("Could not get script from gitHub for " + fileLocation + " " + scriptName + " " + fileVersion);
            }
        } else {
            throw new Exception("Could not find needed data from Script.getFileLocation() for gitHub retrieval");
        }
        return tempUrl;
    }

    public String getGitHubURLAsString(String fileLocation, String scriptName, String fileVersion) throws Exception{
     return getGitHubFileURL(fileLocation,scriptName,fileVersion).toString();
    }
    //To read files from github --End

    private static void validateEntities(ScrapeSession scrapeSession) throws Exception {
        List<String> errors = entityValidationService.validate(scrapeSession.getEntities());
        if (!errors.isEmpty()) {
            StringBuffer buffer = new StringBuffer();
            for (String error : errors) {
                buffer.append(error).append("\n");
            }
            logger.info("Validation Errors Occurred:\n" + errors);
        }
    }

    public void scrape(final String key, ScrapeSession scrapeSession) throws Exception {
    	scrape(key, scrapeSession, null);
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public String getLocalBaseDir() {
        return localBaseDir;
    }

    public void setLocalBaseDir(String localBaseDir) {
        this.localBaseDir = localBaseDir;
    }

    public String[] getCmdArgs() {
        return cmdArgs;
    }

    public void setCmdArgs(String[] cmdArgs) {
        this.cmdArgs = cmdArgs;
    }
}
