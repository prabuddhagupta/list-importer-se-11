package com.se.rdc.core;

import com.rdc.github.service.GitHubServiceImpl;
import com.rdc.importer.misc.ResourceLocatorService;
import com.rdc.importer.scrapian.ScrapianContext;
import com.rdc.importer.scrapian.model.ByteArraySource;
import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.request.UrlRequestInvoker;
import com.rdc.importer.scrapian.resolver.ScrapianResolver;
import com.rdc.importer.scrapian.service.AddressParser;
import com.rdc.importer.scrapian.service.EntityTypeService;
import com.rdc.importer.scrapian.transform.PdfToTextTransformer;
import com.rdc.importer.scrapian.util.GitHubCacheUtil;
import com.rdc.importer.scrapian.util.ModuleLoaderContext;
import com.rdc.scrape.ScrapeSession;
import com.se.rdc.core.utils.CachedDbObject;
import com.se.rdc.core.utils.FileLogger;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.commons.configuration.*;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Omar on 11/17/2015.
 */
public class ScrapianContextSE extends ScrapianContext {
    //from rdc framework
    private String proxy_key = "com.rdc.importer.proxyServer";
    private static String pdf_dir_key = "com.rdc.importer.scrapian.PdfWorkDir";
    private String url_req_bean_key = "urlRequestInvoker";

    private static String gitHub_CacheBaseDir_key = "com.rdc.importer.gitHub.CacheBaseDir";
    private static String gitHub_localCloneDir_key = "com.rdc.importer.gitHub.localCloneDir";
    private static String gitHub_OAuth2Token_key = "com.rdc.importer.gitHub.OAuth2Token";
    private static String gitHub_remote_key = "com.rdc.github.remoteurl";

    //github "RDC" access token
    private static String gitHub_OAuth2Token = "e4aa11c82d515061d8abe8d2d555e3c885274de0";


    //custom keys
    private String params_print_key = "params-print";
    private String cache_key_isactive = "cache";
    private String cache_key_key = "cache_key";

    public CachedDbObject cached_data = new CachedDbObject();
    private boolean isPrintParamsActive = false;
    private HashMap<String, Map> loggerMap = new HashMap<>();
    private ScrapianResolver scrapianResolver;

    public ScrapianContextSE() throws IOException, NoSuchFieldException, IllegalAccessException {
        //fix output dir
        String userDir = System.getProperty("user.dir").replaceAll("(?i)(?<=[/\\\\]RDCScrapper\\b).*$", "/output");
        System.setProperty("user.dir", userDir);

        //mainly get called from temporary test scripts
        initConfiguration(new ScrapianResolverSE());
    }

    ScrapianContextSE(ScrapianResolver scrapianResolver) throws IllegalAccessException, NoSuchFieldException, IOException {
        initConfiguration(scrapianResolver);
    }

    public synchronized void logToFile(String fileName, String data, boolean isOnlyUniqueData, boolean isAppendNewLine) throws Exception {
        PrintWriter logger;
        if (isOnlyUniqueData) {
            Map setMap = loggerMap.get(fileName);
            if (setMap == null) {
                setMap = new HashMap();
                loggerMap.put(fileName, setMap);
            }
            if (setMap.put(data, true) == null) {
                logger = FileLogger.getLogger(
                    ScrapianEngineSE.getScriptsOutputReportDir() + fileName).getPrintWriter();
            } else {
                return;
            }

        } else {
            logger = FileLogger.getLogger(
                ScrapianEngineSE.getScriptsOutputReportDir() + fileName).getPrintWriter();
        }

        logger.append(data);
        if (isAppendNewLine) {
            logger.append("\n");
        }
    }

    enum INVOKE_TYPE {
        INVOKE0, INVOKE0_E, INVOKE, BINARY
    }

    private void initConfiguration(ScrapianResolver scrapianResolver) throws IllegalAccessException, NoSuchFieldException, IOException {
        this.scrapianResolver = scrapianResolver;

        //some initial print
        //System.out.println("Data caching is enabled for network calls. To disable caching for a network invoke, set 'cache:false' in invoke's params map.\n");

        setPdfDirectory();

        //set a default bean factory
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        UrlRequestInvoker invoker = getWrappedInvoker();
        beanFactory.addBean(url_req_bean_key, invoker);
        setBeanFactory(beanFactory);

        //set misc config
        setProxyConfiguration(invoker);
        setAddressParser(new AddressParser());
        //setPersonNameParser(new PersonNameParser());

        EntityTypeService typeService = new EntityTypeService();
        typeService.setScrapianResolver(this.scrapianResolver);
        setEntityType(typeService);

        //set GitHub configurations if its running via production module or if ModuleLoader is used
        try {
            Class.forName("scrapian_scripts.utils.modules.ModulesFactory");
            ModuleLoaderContext.preActions.add(() -> {
                setGitHubConfig();
                return 0;
            });
        } catch (Exception e) {
            setGitHubConfig();
        }

        //set params print
        //setParamsPrintFlag();

    }

    private static void setPdfDirectory() throws NoSuchFieldException, IllegalAccessException, IOException {
        String workDir = System.getProperty(pdf_dir_key);

        if (workDir == null) {
            String tmpDir = System.getProperty("java.io.tmpdir");

            String executableName;
            String osPath = null;
            String osName = System.getProperty("os.name");
            if (osName.equals("Mac OS X")) {
                osPath = "/pdftotext/osx/";
                executableName = "pdftotext";
            } else if (osName.startsWith("Windows")) {
                osPath = "/pdftotext/win32/";
                executableName = "pdftotext.exe";
            } else if (osName.equals("Linux")) {
                osPath = "/pdftotext/linux64/";
                executableName = "pdftotext";
            } else {
                throw new RuntimeException("Unknown operating system [" + osName + "]");
            }

            File pdftotextExe = new File(tmpDir, "li_pdf_dir/" + executableName);
            if (!pdftotextExe.exists()) {
                pdftotextExe.getParentFile().mkdirs();
                FileUtils.copyFile(new File("../assets/pdf" + osPath + executableName).getCanonicalFile(),
                    pdftotextExe);
                pdftotextExe.setExecutable(true);
            }

            PdfToTextTransformer transformerInstance = new PdfToTextTransformer();
            Field field_pdftotextExe = getField(transformerInstance, "pdftotextExe");

            if (field_pdftotextExe.get(transformerInstance) == null) {
                //private static void initExeExtraction() - is still not executed
                System.setProperty(pdf_dir_key,
                    pdftotextExe.getParentFile().getAbsolutePath());

            } else {
                field_pdftotextExe.set(transformerInstance, pdftotextExe);
            }
        }
    }

    private static Field getField(Object instance, String name) throws NoSuchFieldException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);

        return field;
    }

    /**
     * Left for future reference only
     * Tokens list is moved into: ../assets/config/predefined_org_tokens.txt
     */

    //	public String determineEntityType(String entityName) {
    //		if (Pattern.compile(
    //				"(?i)\\b(?:AB|ACCESS|AFFILIATED|AGENCY|AIR|AIRLINES|AIRWAYS|ALLIANCE|AMERICA|AREA|APARTMENT|ASSET|ASSOC|ASSOCIATES|ASSOCIATION|AVIATION)\\b").matcher(
    //				entityName).find()) {
    //			return "O";
    //
    //		} else if (Pattern.compile(
    //				"(?i)\\b(?:BANCO|BUSINESS|BUYERS|CAPITAL|CAPTIAL|CENTER|CO|COASTAL|COMMERCE|COMMUNICATIONS|COMPANY|COMPUTERS|CONDO|CONSULTANTS|CONTRACTING|CORP|CORPORATION|CREDIT)\\b").matcher(
    //				entityName).find()) {
    //			return "O";
    //
    //		} else if (Pattern.compile(
    //				"(?i)\\b(?:DEVELOPMENT|EHOUSING|ELECTRONIC|ELECTRONICS|ENGINEERING|ENTERPRISES|ESTATE|EXCAHNGE|EXPORT|EXPRESS|FINANCIAL|FIRM|FIRST|FORCLOSURE|FUNDING|FURNITURE)\\b").matcher(
    //				entityName).find()) {
    //			return "O";
    //
    //		} else if (Pattern.compile(
    //				"(?i)\\b(?:GMAC|GROUP|HOLDINGS|HOMES|HOUSING|IMPERIAL|INC|INCORPORATED|INDUSTRIES|INT'L|INTERNATIONAL|LABORATORIES|LIMITED|LINES|LLC|LLP|LOANS|LOGISTICS|LTD)\\b").matcher(
    //				entityName).find()) {
    //			return "O";
    //
    //		} else if (Pattern.compile(
    //				"(?i)\\b(?:MANAGEMENT|MANAGERS|MARKETING|METALS|MGMT|MODIFICATION|MORTGAGE|MORTGAGES|NATIONAL|NETWORK|PARTNERS|PLC|PRODUCTS|PROPERTIES|REALTY|RENT|RENTAL|RENTALS|RESOURCES|RETREATS)\\b").matcher(
    //				entityName).find()) {
    //			return "O";
    //
    //		} else if (Pattern.compile(
    //				"(?i)\\b(?:SERVICE|SERVICES|SHIPPING|SOLUTIONS|SPECIALISTS|SUPPLIES|SYSTEMS|TECHNOLOGIE|TECHNOLOGIES|TECHNOLOGY|TELECOM|TRADE|TRADING|TRANSALLIANCE|TRANSPORT|TRUST)\\b").matcher(
    //				entityName).find()) {
    //			return "O";
    //
    //		} else if (Pattern.compile("(?i)\\.\\b(?:com|net|edu|gov|org)\\b").matcher(
    //				entityName).find()) {
    //			return "O";
    //
    //		}
    //
    //		return "P";
    //	}
    @Override
    public void setScrapeSession(ScrapeSession scrapeSession) {
        super.setScrapeSession(scrapeSession);
        //fix ehcache timeout
        CacheConfiguration cacheConfig = scrapeSession.getEntities().getCacheConfiguration();
        long time = TimeUnit.DAYS.toSeconds(30);
        cacheConfig.setTimeToIdleSeconds(time);
        cacheConfig.setTimeToLiveSeconds(time);
    }

    public StringSource invoke0(Map<String, Object> params) throws Exception {
        return checkFromCache(INVOKE_TYPE.INVOKE0, params, null,
            StringSource.class);
    }

    public StringSource invoke0(Map<String, Object> params, String encoding)
        throws Exception {
        return checkFromCache(INVOKE_TYPE.INVOKE0_E, params, encoding,
            StringSource.class);
    }

    public StringSource invoke(Map<String, Object> params) throws Exception {
        return checkFromCache(INVOKE_TYPE.INVOKE, params, null, StringSource.class);
    }

    public ByteArraySource invokeBinary(Map<String, Object> params)
        throws Exception {
        return checkFromCache(INVOKE_TYPE.BINARY, params, null,
            ByteArraySource.class);
    }

    private <T extends ScrapianSource> T checkFromCache(INVOKE_TYPE invokeType, Map<String, Object> params, String encoding, Class<T> tClass)
        throws Exception {

        Object cache = params.remove(cache_key_isactive);
        Object cache_key = params.remove(cache_key_key);

        boolean isCacheActive = cache == null || (boolean) cache;
        T valueStr;
        String key = cache_key == null ? params.toString() : cache_key.toString();

        if (isCacheActive) {
            Object value = cached_data.get(key);
            if (value == null) {
                valueStr = createInvoke(invokeType, params, encoding, tClass);
                cached_data.put(key, valueStr.getValue());

            } else {
                System.out.println("Loading from cache: " + params.get("url"));
                if (tClass.equals(StringSource.class)) {
                    valueStr = tClass.cast(new StringSource((String) value));
                } else {
                    valueStr = tClass.cast(new ByteArraySource((byte[]) value));
                }
            }

        } else {
            valueStr = createInvoke(invokeType, params, encoding, tClass);
            cached_data.put(key, valueStr.getValue());
        }

        return valueStr;
    }

    private <T extends ScrapianSource> T createInvoke(INVOKE_TYPE invokeType, Map<String, Object> params, String encoding, Class<T> tClass)
        throws Exception {

        System.out.print("Invoking: " + params.toString());
        T output;

        if (invokeType == INVOKE_TYPE.INVOKE0) {
            output = tClass.cast(super.invoke0(params));

        } else if (invokeType == INVOKE_TYPE.INVOKE0_E) {
            //unfortunately, we need to use reflection because IDE somehow don't detect invoke0 with multi-params
            ScrapianContext super_ = new ScrapianContext();
            Method method = super_.getClass().getMethod("invoke0", Map.class,
                String.class);
            output = tClass.cast(method.invoke(super_, params, encoding));

        } else if (invokeType == INVOKE_TYPE.INVOKE) {
            output = tClass.cast(super.invoke(params));

        } else {
            output = tClass.cast(super.invokeBinary(params));
        }
        System.out.print("\n");

        return output;
    }

    private void setProxyConfiguration(UrlRequestInvoker invoker) {
        String config = System.getProperty(proxy_key);

        if (config != null) {
            if (!config.trim().isEmpty()) {
                SystemConfiguration configuration = new SystemConfiguration();
                configuration.setProperty(proxy_key, config);
                setConfiguration(configuration);
                invoker.setConfiguration(configuration);
            }
        }
    }

    public void setGitHubConfig() {
        String localGithubCache = System.getProperty("user.home") + "/Github_cache/local_clone/";

//        SystemConfiguration configuration = new SystemConfiguration();
        Configuration configuration = new BaseConfiguration();
        configuration.setProperty(gitHub_CacheBaseDir_key, System.getProperty("java.io.tmpdir") + "/");
        configuration.setProperty(gitHub_localCloneDir_key, localGithubCache);
        configuration.setProperty(gitHub_OAuth2Token_key, gitHub_OAuth2Token);
        configuration.setProperty(gitHub_remote_key, "https://github.com/");

        new GitHubCacheUtil().setConfiguration(configuration);
        new GitHubServiceImpl().setConfiguration(configuration);
    }

    private UrlRequestInvoker getWrappedInvoker() {
        //UrlRequestInvoker is modified to wrap logger methods
        UrlRequestInvoker invoker = new UrlRequestInvoker();

        ResourceLocatorService resLocator = new ResourceLocatorService();
        resLocator.setConfig(new BaseConfiguration());
        invoker.setResourceLocatorService(resLocator);

        try {
            //Using reflection to modify the logger field
            Field loggerField = invoker.getClass().getDeclaredField("logger");
            loggerField.setAccessible(true);
            Logger logger = (Logger) loggerField.get(invoker);

            //wrap logger instance
            logger = new WrappedLogger(logger);
            loggerField.set(invoker, logger);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return invoker;
    }

    private void setParamsPrintFlag() {
        String isActive = System.getProperty(params_print_key);

        if (isActive != null && isActive.equalsIgnoreCase("false")) {
            isPrintParamsActive = false;
        }
    }

    private void configureLog4J() {
//        String log4JPropertyFile = "../assets/config/log4j.properties";
        String log4JPropertyFile = "D:\\SEBPO\\list-importer-se-11\\assets\\config\\log4j.properties";
        try {
            System.out.println("Log4j file path: " + log4JPropertyFile);
            PropertyConfigurator.configure(log4JPropertyFile);
            //info("Wow! I'm configured!");
        } catch (Exception e) {
            //DAMN! I'm not....
        }
    }

    private class WrappedLogger extends Logger {
        private final Logger logger;

        WrappedLogger(Logger logger) {
            super("w_logger");//This name is optional
            this.logger = logger;
        }

        @Override
        public void info(Object message) {
            if (!message.toString().contains("Invoking")) {
                logger.info(message);
            }
        }

        @Override
        public void warn(Object message) {
            logger.warn(message);
        }
    }
}