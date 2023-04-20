package com.se.rdc.core;

import com.rdc.importer.scraper.EntityValidationService;
import com.rdc.importer.scrapian.ScrapianEngine;
import com.rdc.importer.scrapian.ScrapianScript;
import com.rdc.importer.scrapian.resolver.ScrapianResolver;
import com.rdc.importer.scrapian.util.EhCacheUtil;
import com.rdc.scrape.ScrapeSession;
import com.se.rdc.core.utils.*;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import net.sf.ehcache.Cache;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

public class ScrapianEngineSE extends ScrapianEngine {
    private static String[] cmdArgs;
    private ScrapianResolver scrapianResolver;
    private static List<Thread> threadList = new ArrayList<>();
    private ThreadWaitNotifier notifier = new ThreadWaitNotifier();
    private static String scriptName;

    public static void main(String[] args) throws Exception {
        TaskTimer.startTrack("script");
        cmdArgs = args.clone();
        ScrapianEngineSE scrapianEngineSE = new ScrapianEngineSE();
        try {
            scriptName = cmdArgs[0].replaceAll("^.*[:/\\\\](.*)\\.\\w+$", "$1");
            scrapianEngineSE.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //time calculation
        TaskTimer.endTrack("script");

        //Wait for all threads to finish and then stop ehcache
        for (Thread t : threadList) {
            t.join();
        }

        try {
            EhCacheUtil.getCacheManager().shutdown();
        } catch (Exception ignored) {
        }
    }

    public void init() throws Exception {
        if (cmdArgs.length < 1) {
            System.out.println(
                    "Usage: com.rdc.importer.scrapian.ScrapianEngine <script key>");
        }

        super.setEntityValidationService(new EntityValidationService());
        scrapianResolver = new ScrapianResolverSE(cmdArgs);
        super.setScrapianResolver(scrapianResolver);

        ScrapeSessionSE scrapeSession = new ScrapeSessionSE(cmdArgs[0]);
        scrape(cmdArgs[0], scrapeSession, null, false);

        handleOutput(scrapeSession);
    }

    public void scrape(final String key, ScrapeSession scrapeSession,
                       HashMap<String, Object> map, boolean useResolver) throws Exception {
        ScrapianScript script = scrapianResolver.resolveScript(key);
        final URL url = script.getUrl();

        GroovyScriptEngine gse = new GroovyScriptEngine(new ResourceConnector() {
            public URLConnection getResourceConnection(String resourceName)
                    throws ResourceException {
                if (key.equals(resourceName)) {
                    try {
                        //logger.info("Fetching Script [" + url + "]");
                        return url.openConnection();
                    } catch (Exception e) {
                        throw new ResourceException(e.getMessage(), e);
                    }
                }
                throw new ResourceException("Resource[" + resourceName + "] not found");
            }
        });

        final ScrapianContextSE scrapianContext = new ScrapianContextSE(scrapianResolver);
        scrapianContext.setScriptParams(script.getParams());
        scrapianContext.setScrapeSession(scrapeSession);

        scrapianContext.cached_data.setCompressionLevel(
                Deflater.BEST_COMPRESSION).setBaseDir(
                "./" + AppConstant.REPORT_DIR + scriptName).init();
        //context.configureLog4J();

        Binding binding = new Binding();
        binding.setVariable("context", scrapianContext);
        //binding.setVariable("args", cmdArgs);
        //binding.setVariable("beanFactory", beanFactory);
        if (map != null && map.size() > 0) {
            for (Map.Entry<String, Object> e : map.entrySet()) {
                binding.setVariable(e.getKey(), e.getValue());
            }
        }

        //now run the script
        try {
            gse.run(key, binding);
        } catch (Exception e) {
            e.printStackTrace(FileLogger.getLogger("run_error.txt").getPrintWriter());
            e.printStackTrace();
        } finally {
            scrapianContext.cached_data.close();
            FileLogger.close();
        }
    }

    private void handleOutput(final ScrapeSession scrapeSession)
            throws Exception {

        //primary validation
        validate(scrapeSession.getEntities());

        String outputFilePath = scriptName + "/" + scriptName;

        //add execution nane param in output file's name if "tag" param is present in program's args
        Matcher nameMatch = Pattern.compile("(?i)\\stag=([^,\\]]+)").matcher(
                Arrays.toString(cmdArgs));
        if (nameMatch.find()) {
            outputFilePath += "_" + nameMatch.group(1);
        }

        //Run csv, xml and zip creation in a separate threads
        final String finalOutputFilePath = outputFilePath;
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createOutputFiles(finalOutputFilePath, scrapeSession);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "output_worker_thread");
        threadList.add(t1);
        t1.start();

        notifier.doWait();
        // Secondary xml validation
        new SecondaryValidator().xmlTagValuesValidation(
                AppConstant.REPORT_DIR + outputFilePath + ".xml",
                scrapeSession.getEncoding());

        // script character validation
//        new ScriptCharacterValidator().validate(new URI(cmdArgs[0]),
//                scrapeSession.getEncoding());

        // output Xml schema validation
        //new XmlSchemaValidator(outputXmlPath, "config/ScrapeEntity.xsd").validate();
    }

    private void createOutputFiles(final String outputFilePath,
                                   final ScrapeSession scrapeSession) throws Exception {

        //Sort data
        String sorted = System.getProperty("sorted");
        if (sorted == null || sorted.equalsIgnoreCase("true")) {
            Collections.sort(scrapeSession.getEntitiesNonCache(),
                    new EntityComparator());
        }

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                //custom entity list to csv conversion
                try {
                    new EntityListXmlToCsv(AppConstant.REPORT_DIR + outputFilePath,
                            scrapeSession.getEntitiesNonCache()).writeToCsv();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "output_worker_csv");
        t1.start();

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                //write out xml file
                File outputFile = new File(
                        AppConstant.REPORT_DIR + outputFilePath + ".xml");
                if (outputFile.getParentFile() != null) {
                    outputFile.getParentFile().mkdirs();
                }
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(outputFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    scrapeSession.dump(outputStream, false, true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(outputStream);
                }
                notifier.doNotify();
            }
        }, "output_worker_xml");
        t2.start();

        //wait before starting next thread
        t1.join();
        t2.join();

        //write to official csv format and zip it with xml file
        new ExportReportSE().toReport(scrapeSession.getEntities(),
                cmdArgs[0].replaceAll(".*?:/*(?=/)", ""), scriptName);
    }

    private void validate(Cache cache) {
        System.out.println("\n\nTotal entities: " + cache.getSize());
        List<String> errors = new EntityValidationService().validate(cache);
        if (!errors.isEmpty()) {
            StringBuilder buffer = new StringBuilder();
            for (String error : errors) {
                buffer.append(error).append("\n");
            }
            System.out.println("\nVALIDATION ERRORS\n" + buffer);
        }
    }

    public static String getScriptsOutputReportDir() throws Exception {
        if (scriptName != null) {
            return AppConstant.REPORT_DIR + scriptName + "/";
        }

        throw new Exception("Call main method before using this method.");
    }
}