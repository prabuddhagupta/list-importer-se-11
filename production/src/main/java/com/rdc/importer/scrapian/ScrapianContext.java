package com.rdc.importer.scrapian;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.rdc.core.nameparser.PersonName;
import com.rdc.core.nameparser.PersonNameParser;
import com.rdc.core.nameparser.PersonNameParserImpl;
import com.rdc.importer.scrapian.model.*;
import com.rdc.importer.scrapian.request.RequestInvoker;
import com.rdc.importer.scrapian.request.SimpleHttpClient;
import com.rdc.importer.scrapian.request.UrlRequestInvoker;
import com.rdc.importer.scrapian.service.AddressParser;
import com.rdc.importer.scrapian.service.EntityTypeService;
import com.rdc.importer.scrapian.transform.*;
import com.rdc.importer.scrapian.util.RegexUtils;
import com.rdc.importer.scrapian.util.ScrapianCacheUtil;
import com.rdc.scrape.*;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.ccil.cowan.tagsoup.Parser;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Component
@Scope("prototype")
public class ScrapianContext implements BeanFactoryAware {

    private static final String[] suffixes = {"JR", "SR", "III", "IV"};
    private static final String[] defaultDatePatterns = {
        "MM/dd/yy", "MM/dd/yyyy", "MMMMM dd, yyyy", "MMM. dd, yyyy", "MM-dd-yy",
        "MM-dd-yyyy", "MM.dd.yyyy", "MMM dd, yyyy", "dd MMMMM yyyy",
        "dd-MMM-yy", "yyyy", "MMMMM dd yyyy", "MMM. dd yyyy", "ddMMMyy"};
    private static final Logger logger =
        LogManager.getLogger(ScrapianContext.class);

    private WebClient webClient;
    private UrlRequestInvoker invoker;
    private Map<String, String> scriptParams = new HashMap<String, String>();
    private Map<String, ScrapeEntity> keyedEntityCache = new HashMap<String, ScrapeEntity>();
    private ScrapeSession scrapeSession = new ScrapeSession(null);
    private PersonNameParser personNameParser = new PersonNameParserImpl();
    private Map<String, Object> connectionParams = new HashMap<String, Object>();
    private AddressParser addressParser;
    private EntityTypeService entityTypeService;
    private BeanFactory beanFactory;
    private String proxyServer;
    private Configuration configuration;
    public static String NO_CITY = "NoCity";
    public static String FIRST_FOUR_ZIP = "FirstFourZip";
    public static String FIRST_FOUR_ZIP_NO_CITY = "FirstFourZipNoCity";
    public static String NO_MIDDLE_NAME = "NoMiddleName";
    public static String PROVINCE_ONLY = "ProvinceOnly";

    public void setScrapeSession(ScrapeSession scrapeSession) {
        this.scrapeSession = scrapeSession;
    }

    public void setScriptParams(Map<String, String> scriptParams) {
        this.scriptParams = scriptParams;
    }

    @Resource
    public void setConfiguration(final Configuration config) {
        configuration = config;
        proxyServer = config.getString("com.rdc.importer.proxyServer");
    }

    @Resource
    public void setAddressParser(AddressParser addressParser) {
        this.addressParser = addressParser;
    }

    @Resource
    public void setPersonNameParser(PersonNameParser personNameParser) {
        this.personNameParser = personNameParser;
    }

    @Resource
    public void setEntityType(EntityTypeService entityTypeService) {
        this.entityTypeService = entityTypeService;
    }

    public Map<String, String> getScriptParams() {
        return scriptParams;
    }

    public void setup(Map<String, Object> params) {
        this.connectionParams = params;
    }

    public ScrapeSession getSession() {
        return scrapeSession;
    }

    public ScrapeEntity findEntity(Map<String, Object> params) {
        // if has name, do name lookup first vs hashmap
        boolean nameFilter = false;
        String scrubbedName = "";
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if ("name".equals(entry.getKey())) {
                nameFilter = true;
                scrubbedName = scrubPunct(entry.getValue().toString()).toUpperCase();
            }
        }
        if (nameFilter) {
            HashMap<String, List<ScrapeEntity>> entitiesMap = scrapeSession.getEntitiesMap();
            List<ScrapeEntity> scrapeEntityList = entitiesMap.get(scrubbedName);
            if (scrapeEntityList != null) {
                for (ScrapeEntity scrapeEntity : scrapeEntityList) {
                    if (scrapeEntity != null) {
                        if (checkAgainstParams(scrapeEntity, params)) {
                            return scrapeEntity;
                        }
                    }
                }
            }
            return null;
        } else {
            for (ScrapeEntity scrapeEntity : scrapeSession.getEntitiesNonCache()) {
                if (checkAgainstParams(scrapeEntity, params)) {
                    return scrapeEntity;
                }
            }
        }
        return null;
    }

    private boolean checkAgainstParams(ScrapeEntity scrapeEntity, Map<String, Object> params) {
        boolean match = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if ("type".equals(entry.getKey()) && !scrapeEntity.getType().equals(entry.getValue().toString())) {
                match = false;
                break;
            }
            if ("name".equals(entry.getKey()) && !scrapeEntity.getScrubbedName().equalsIgnoreCase(scrubPunct(entry.getValue().toString()))) {
                match = false;
                break;
            }
            if ("dob".equals(entry.getKey()) && (entry.getValue() instanceof ScrapeDob ? !scrapeEntity.getDateOfBirths().contains(entry.getValue()) : !scrapeEntity.getDateOfBirths().contains(new ScrapeDob((String) entry.getValue())))) {
                match = false;
                break;
            }
            if ("address".equals(entry.getKey()) && !scrapeEntity.getAddresses().contains(entry.getValue())) {
                match = false;
                break;
            }
            if ("addressSpecial".equals(entry.getKey()) && !containsAddressSpecial(scrapeEntity.getAddresses(), (ScrapeAddress) entry.getValue(), "")) {
                match = false;
                break;
            }
            if ("addressSpecialNoCity".equals(entry.getKey()) && !containsAddressSpecial(scrapeEntity.getAddresses(), (ScrapeAddress) entry.getValue(), NO_CITY)) {
                match = false;
                break;
            }
            if ("addressSpecialFirstFourZip".equals(entry.getKey()) && !containsAddressSpecial(scrapeEntity.getAddresses(), (ScrapeAddress) entry.getValue(), FIRST_FOUR_ZIP)) {
                match = false;
                break;
            }
            if ("addressSpecialFirstFourZipNoCity".equals(entry.getKey()) && !containsAddressSpecial(scrapeEntity.getAddresses(), (ScrapeAddress) entry.getValue(), FIRST_FOUR_ZIP_NO_CITY)) {
                match = false;
                break;
            }
            if ("addressSpecialStateOnly".equals(entry.getKey()) && !containsAddressSpecial(scrapeEntity.getAddresses(), (ScrapeAddress) entry.getValue(), PROVINCE_ONLY)) {
                match = false;
                break;
            }
            if ("nameNoMiddleName".equals(entry.getKey()) && !containsNameSpecial(scrapeEntity.getScrubbedName(), scrubPunct(entry.getValue().toString()), NO_MIDDLE_NAME)) {
                match = false;
                break;
            }
            if ("id".equals(entry.getKey()) && (scrapeEntity.getDataSourceId() == null || !scrapeEntity.getDataSourceId().equals(entry.getValue().toString()))) {
                match = false;
                break;
            }
        }
        return match;
    }

    public ScrapeEntity newEntity(List<String> keys) {
        ScrapeEntity entity = scrapeSession.newEntity();
        keyedEntityCache.put(createCacheKey(keys), entity);
        return entity;
    }

    private String scrubPunct(String in) {
        if (in == null) {
            return in;
        }
        return in.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }

    private boolean containsAddressSpecial(List<ScrapeAddress> addresses, ScrapeAddress addressToCheck, String type) {
        for (ScrapeAddress address : addresses) {
            if (equalsAddressSpecial(address, addressToCheck, type)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNameSpecial(String name, String nameToCheck, String type) {
        if (name == null && nameToCheck == null) {
            return true;
        }
        if (name == null || nameToCheck == null) {
            return false;
        }
        if (name.equalsIgnoreCase(nameToCheck)) {
            return true;
        }
        return removeMiddle(name).equalsIgnoreCase(removeMiddle(nameToCheck));
    }

    //really, remove second token
    private String removeMiddle(String in) {
        if (in != null && in.split(" ").length > 2) {
            String[] splitUp = in.split(" ");
            String retVal = splitUp[0] + " ";
            for (int j = 2; j < splitUp.length; j++) {
                retVal += splitUp[j] + " ";
            }
            return retVal.replaceAll("\\s+", " ").trim();
        }
        return in;
    }

    // remove punctuation
    // only compare first five of postalcode
    // not compare on country
    private boolean equalsAddressSpecial(ScrapeAddress one, ScrapeAddress two, String type) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        if (!PROVINCE_ONLY.equals(type)) {
            if (!(one.getPostalCode() == null && two.getPostalCode() == null)) {
                if (one.getPostalCode() == null || two.getPostalCode() == null) {
                    return false;
                }
                int size = (FIRST_FOUR_ZIP.equals(type) || FIRST_FOUR_ZIP_NO_CITY.equals(type)) ? 4 : 5;
                if (!one.getPostalCode().equals(two.getPostalCode())) {
                    if (!(trimPostalCode(scrubPunct(one.getPostalCode()), size).equals(trimPostalCode(scrubPunct(two.getPostalCode()), size)))) {
                        return false;
                    }
                }
            }

            if (one.getAddress1() != null ? !scrubPunct(one.getAddress1()).equals(scrubPunct(two.getAddress1())) : two.getAddress1() != null) {
                return false;
            }
            if (one.getBirthPlace() != null ? !one.getBirthPlace().equals(two.getBirthPlace()) : two.getBirthPlace() != null) {
                return false;
            }

            if (!(NO_CITY.equals(type) || FIRST_FOUR_ZIP_NO_CITY.equals(type))) {
                if (one.getCity() != null ? !scrubPunct(one.getCity()).equals(scrubPunct(two.getCity())) : two.getCity() != null) {
                    return false;
                }
            }
        }

        return one.getProvince() != null ? scrubPunct(one.getProvince()).equals(
            scrubPunct(two.getProvince())) : two.getProvince() == null;
    }

    private String trimPostalCode(String postalCode, int size) {
        if (postalCode == null || postalCode.length() <= size) {
            return postalCode;
        }
        return postalCode.substring(0, size);
    }

    private String createCacheKey(List<String> keys) {
        StringBuffer cacheKey = new StringBuffer();
        for (String key : keys) {
            if (key != null) {
                cacheKey.append(key);
            } else {
                cacheKey.append(Math.random());
            }
        }
        return cacheKey.toString();
    }

    public ScrapeEntity findEntity(List<String> keys) {
        return keyedEntityCache.get(createCacheKey(keys));
    }

    public GPathResult getRootNode(final String xml)
        throws IOException, SAXException {
        final Parser parser = new Parser();
        final XmlSlurper slurper = new XmlSlurper(parser);
        return slurper.parse(new StringReader(xml));
    }

    public GPathResult getRootNode(final SgmlPage page)
        throws IOException, SAXException {
        return getRootNode(page.asXml());
    }

    public GPathResult getRootNode(final StringSource xml)
        throws IOException, SAXException {
        return getRootNode(xml.toString());
    }

    private WebClient getWebClient() {
        if (webClient == null
            || Boolean.FALSE.equals(connectionParams.get("keepSession"))) {
            if (StringUtils.contains(proxyServer, ':')) {
                final String proxy = proxyServer.split(":")[0];
                final int port = Integer.parseInt(proxyServer.split(":")[1]);
                //webClient = new WebClient(BrowserVersion.BEST_SUPPORTED, proxy, port);
                webClient = new WebClient(BrowserVersion.FIREFOX_3_6, proxy, port);
            } else {
                webClient = new WebClient(BrowserVersion.FIREFOX_3_6);
                //webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
            }
        }
        return webClient;
    }

    /**
     * @param 'url' required; 'headers', 'ignoreRedirects' optional
     */
   /* public Page getPage(final Map<String, Object> params) throws Exception {
        if (Thread.interrupted()) { // called often; good place to check
            throw new InterruptedException();
        }

        final Map<String, String> headers =
            (Map<String, String>) params.get("headers");
        final String url = (String) params.get("url");
        final String ignore = (String) params.get("ignoreRedirects");
        final boolean redirect = !StringUtils.equalsIgnoreCase(ignore, "true");

        final WebClient client = getWebClient();
       // final boolean wasRedirectEnabled = client.isRedirectEnabled();
        final boolean wasRedirectEnabled = client.isRedirectEnabled();

        if (url == null) {
            throw new IllegalArgumentException("key 'url' is required");
        }

        client.setRedirectEnabled(redirect);

        if (headers != null) {
            for (final String key : headers.keySet()) {
                client.addRequestHeader(key, headers.get(key));
            }
        }

        logger.info("Invoking[" + url + "]");
        final Page page = webClient.getPage(url);

        client.setRedirectEnabled(wasRedirectEnabled);

        if (headers != null) {
            for (final String key : headers.keySet()) {
                client.removeRequestHeader(key);
            }
        }
        return page;
    }*/
    private synchronized RequestInvoker getInvoker() {
        if (invoker == null || Boolean.FALSE.equals(connectionParams.get("keepSession"))) {
            Integer socketTimeout = connectionParams.get("socketTimeout") == null ? 20000 : (Integer) connectionParams.get("socketTimeout");
            Integer connectionTimeout = connectionParams.get("connectionTimeout") == null ? 5000 : (Integer) connectionParams.get("connectionTimeout");
            Integer retryWait = connectionParams.get("retryWait") == null ? 2500 : (Integer) connectionParams.get("retryWait");
            Integer retryCount = (Integer) connectionParams.get("retryCount");
            boolean multithread = connectionParams.get("multithread") == null ? false : (Boolean) connectionParams.get("multithread");
            invoker = (UrlRequestInvoker) beanFactory.getBean("urlRequestInvoker");
            invoker.setUserAgent((String) connectionParams.get("userAgent"));
            invoker.open(socketTimeout, connectionTimeout, retryCount, multithread, retryWait);
        }
        return invoker;
    }

    public StringSource invoke0(Map<String, Object> params) throws Exception {
        // params.put( "proxy", proxyServer );

        //System.out.println( params );

        String lRetVal = SimpleHttpClient.invoke(params);
        Map<String, ArrayList> lHeaders = (Map<String, ArrayList>) params.get("responseHeaders");

        if (lHeaders != null && lHeaders.get("Set-Cookie") != null) {
            Map<String, Object> lCookieMap = new HashMap<String, Object>();
            lCookieMap.put("Cookie", lHeaders.get("Set-Cookie").get(0));
            params.put("headers", lCookieMap);
        }

        StringSource source = new StringSource(lRetVal);
        return source;
    }

    public StringSource invoke0(Map<String, Object> params, String encoding) throws Exception {
        //   params.put( "proxy", proxyServer );
        //System.out.println( params );

        String lRetVal = SimpleHttpClient.invoke(params, encoding);
        Map<String, ArrayList> lHeaders = (Map<String, ArrayList>) params.get("responseHeaders");

        if (lHeaders != null && lHeaders.get("Set-Cookie") != null) {
            Map<String, Object> lCookieMap = new HashMap<String, Object>();
            lCookieMap.put("Cookie", lHeaders.get("Set-Cookie").get(0));
            params.put("headers", lCookieMap);
        }

        StringSource source = new StringSource(lRetVal);
        return source;
    }

    public StringSource invoke(Map<String, Object> params) throws Exception {
        if (Thread.interrupted()) { // called often; good place to check
            throw new InterruptedException();
        }

        ScrapianRequest scrapianRequest = new ScrapianRequest(params.get("url").toString());
        scrapianRequest.setRemoteSiteCookies(scrapeSession.getRemoteSiteCookies());
        if (params.get("type") != null) {
            scrapianRequest.setType(params.get("type").toString());
        }

        if (params.get("params") != null) {
            scrapianRequest.setParameters((Map<String, String>) params.get("params"));
        }

        if (params.get("headers") != null) {
            scrapianRequest.setHeaders((Map<String, String>) params.get("headers"));
        }

        if (StringUtils.equalsIgnoreCase((String) params.get("ignoreRedirects"), "true")) {
            logger.debug("ignoring redirects");
            scrapianRequest.setIgnoreRedirects(true);
        }

        StringSource source = getInvoker().invoke(scrapianRequest, scrapeSession.getEncoding());

        Boolean tidy = (Boolean) params.get("tidy");
        if (Boolean.TRUE == tidy) {
            source = tidy(source, params);
        }
        Boolean clean = (Boolean) params.get("clean");
        if (clean == null || clean == Boolean.TRUE) {
            String cleanSource = source.getValue().replaceAll("&#160;", " ");
            cleanSource = cleanSource.replaceAll("[ \t]{2,}", " ");
            cleanSource = RegexUtils.replaceMultiLineAll(cleanSource, "^\\s*[\\r\\n]*", "");
            source = new StringSource(cleanSource);
        }
        return source;
    }

    public ByteArraySource invokeBinary(Map<String, Object> params) throws Exception {
        ScrapianRequest scrapianRequest = new ScrapianRequest(params.get("url").toString());
        scrapianRequest.setRemoteSiteCookies(scrapeSession.getRemoteSiteCookies());
        if (params.get("type") != null) {
            scrapianRequest.setType(params.get("type").toString());
        }
        if (params.get("params") != null) {
            scrapianRequest.setParameters((Map<String, String>) params.get("params"));
        }

        return getInvoker().invokeBinary(scrapianRequest);
    }

    public ScrapianSource transformSpreadSheet(ByteArraySource source, Map<String, Object> params) throws Exception {
        return new ExcelTransformer().transform(source, params);
    }

    public ScrapianSource transformPdfToText(ByteArraySource source, Map<String, Object> params) throws Exception {
        PdfToTextTransformer pdfToTextTransformer = new PdfToTextTransformer();
        if (StringUtils.isNotBlank(scrapeSession.getEncoding()))
            pdfToTextTransformer.setEncoding(scrapeSession.getEncoding());
        return pdfToTextTransformer.transform(source, params);
    }

    public ScrapianSource transformDelimitedText(StringSource source, Map<String, Object> params) throws Exception {
        return new DelimitedTextTransformer().transform(source, params);
    }

    public ScrapianSource regexMatch(ScrapianSource source, Map<String, Object> params) throws Exception {
        return new RegexMatchTransformer().transform(source, params);
    }

    public ListSource<ListSource<StringSource>> regexMatches(ScrapianSource source, Map<String, Object> params) throws Exception {
        return new RegexMatchesTransformer().transform(source, params);
    }

    public StringSource tidy(ScrapianSource source, Map<String, Object> params) throws Exception {
        TidyTransformer tidyTransformer = new TidyTransformer();
        if (StringUtils.isNotBlank(scrapeSession.getEncoding()))
            tidyTransformer.setEncoding(scrapeSession.getEncoding());
        return tidyTransformer.transform(source, params);
    }

    public ScrapianSource elementSeek(ScrapianSource source, Map<String, Object> params) throws Exception {
        return new ElementSeekTransformer().transform(source, params);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable e) {
        logger.error(message, e);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void debug(String debug) {
        logger.debug(debug);
    }

    public void parseDateOfBirthForEntity(ScrapeEntity entity, StringSource dobSource) {
        ScrapeDob scrapeDob = parseDateOfBirth(dobSource);
        if (scrapeDob != null) {
            entity.addDateOfBirth(scrapeDob);
        }
    }

    public ScrapeDob parseDateOfBirth(StringSource dobSource) {
        if (dobSource != null && !dobSource.isBlank()) {
            String dob = parseDate(dobSource);
            if (dob != null) {
                return new ScrapeDob(dob);
            }
        }
        return null;
    }

    public String parseDate(String date) {
        return parseDate(new StringSource(date));
    }

    public String parseDate(StringSource date) {
        return parseDate(date, defaultDatePatterns);
    }

    public String parseDate(StringSource dateInput, String[] datePatterns) {
        Date resultDate = null;
        for (String pattern : datePatterns) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            simpleDateFormat.setLenient(false);

            try {
                resultDate = simpleDateFormat.parse(dateInput.toString());
                if ("yyyy".equals(pattern)) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(resultDate);
                    return "-/-/" + c.get(Calendar.YEAR); // dateinput is just year if it made it past parsing
                }
                break;
            } catch (Exception e) {
                //ignore.
            }
        }

        if (resultDate != null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
            return simpleDateFormat.format(resultDate);
        } else {
            return null;
        }
    }

    public String joinStrings(List<String> parts) {
        return joinStrings(parts, " ");
    }

    public String joinStrings(List<String> parts, String delimiter) {
        List<String> notBlankList = new ArrayList<String>();
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                notBlankList.add(part.trim());
            }
        }
        return StringUtils.join(notBlankList, delimiter);
    }

    /**
     * @deprecated Since {@link ScrapeEntity#setName(String)} is deprecated,
     * there is no use for this method.
     */
    @Deprecated
    public String formatName(String... namePieces) {
        return personNameParser.formatName(namePieces);
    }

    public PersonName parseName(String... namePieces) {
        return personNameParser.parseName(StringUtils.join(namePieces, " "));
    }

    public String[] splitCommaPersonNames(StringSource nameSource) {
        if (nameSource.toString().contains(",")) {
            String[] nameParts = nameSource.toString().split(",");
            if (nameParts != null && nameParts.length > 1) {
                for (int i = 0; i < nameParts.length; i++) {
                    String namePart = nameParts[i].trim();
                    if (!namePart.contains(" ")) {
                        if (namePart.endsWith(".")) {
                            namePart = namePart.substring(0, namePart.length() - 1);
                        }
                        for (String suffix : suffixes) {
                            if (namePart.equalsIgnoreCase(suffix)) {
                                if (i != 0) {
                                    nameParts[i - 1] = nameParts[i - 1] + " " + namePart;
                                }
                                nameParts[i] = null;
                            }
                        }
                    }
                }
                List<String> names = new ArrayList<String>();
                for (String namePart : nameParts) {
                    if (!StringUtils.isBlank(namePart)) {
                        names.add(replaceAnd(namePart.trim()));
                    }
                }
                return names.toArray(new String[names.size()]);
            }
        }

        return new String[]{replaceAnd(nameSource.toString().trim())};
    }

    public String determineEntityType(String entityName) throws Exception {
        return this.entityTypeService.determineEntityType(entityName);
    }

    public String determineEntityType(String entityName, List<String> additionalOrgTokens) throws Exception {
        return this.entityTypeService.determineEntityType(entityName, additionalOrgTokens);
    }


    // replace "and" appearing at the beginning or at the end of the name
    private String replaceAnd(String name) {
        name = name.replaceAll("^[aA][nN][dD]\\s", "");
        name = name.replaceAll("\\s[aA][nN][dD]$", "");
        return name;
    }

    public List<ScrapeAddress> parseAddresses(StringSource address) {
        return this.addressParser.parseAddresses(address, ";", ",", false, scrapeSession.isEscape());
    }

    public List<ScrapeAddress> parseAddresses(StringSource address, boolean birth) {
        return this.addressParser.parseAddresses(address, ";", ",", birth, scrapeSession.isEscape());
    }

    public List<ScrapeAddress> parseAddresses(StringSource addressSource, String addressSeparator, String addressPartSeparator, boolean birth) {
        return this.addressParser.parseAddresses(addressSource, addressSeparator, addressPartSeparator, birth, scrapeSession.isEscape());
    }

    public ScrapeAddress parseAddress(StringSource address) {
        return this.addressParser.parseAddress(address, ",", false, scrapeSession.isEscape());
    }

    public ScrapeAddress parseAddress(StringSource address, boolean birth) {
        return this.addressParser.parseAddress(address, ",", birth, scrapeSession.isEscape());
    }

    public ScrapeAddress parseAddress(String address, boolean birth) {
        return parseAddress(new StringSource(address), birth);
    }

    public boolean isNumeric(StringSource value) {
        return value != null && isNumeric(value.toString());
    }

    public boolean isNumeric(String value) {
        if (value != null) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (Exception ignore) {
                // Ignore
            }
        }
        return false;
    }

    public boolean containsIgnoreCase(StringSource data, String value) {
        return data != null && containsIgnoreCase(data.toString(), value);
    }

    public boolean containsIgnoreCase(String data, String value) {
        return data != null && data.toLowerCase().contains(value.toLowerCase());
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void deleteCachedEntities(String sourceKey) {
        String cachedEntityDirectory = configuration.getString("cached.entity.directory", "/tmp/list_importer_cache/");
        ScrapianCacheUtil.deleteCachedEntities(cachedEntityDirectory, sourceKey);
    }

    public void serializeCachedEntities(String sourceKey, List<String> historyList) {
        String cachedEntityDirectoryStr = configuration.getString("cached.entity.directory", "/tmp/list_importer_cache/");
        File cachedEntityDirectory = new File(cachedEntityDirectoryStr);
        if (!cachedEntityDirectory.exists()) {
            cachedEntityDirectory.mkdirs();
        }
        CachedEntities cachedEntities = new CachedEntities();
        cachedEntities.setHistoryList(historyList);
        cachedEntities.setEntityList(scrapeSession.getEntitiesNonCache());
        try {
            ObjectOutputStream oss = new ObjectOutputStream(new FileOutputStream(cachedEntityDirectoryStr + sourceKey + ".obj"));
            oss.writeObject(cachedEntities);
            oss.close();
        } catch (Exception ex) {
            logger.error("Unable to store cached entiries to disk for list importer " + sourceKey + " because " + ex);
        }
    }

    public CachedEntities deserializeCachedEntities(String sourceKey) throws IOException {
        ObjectInputStream ois = null;
        String cachedEntityDirectory = configuration.getString("cached.entity.directory", "/tmp/list_importer_cache/");
        String cachedEntityFileStr = cachedEntityDirectory + sourceKey + ".obj";
        File cachedFile = new File(cachedEntityFileStr);
        if (!cachedFile.exists()) {
            logger.info("Cached entity file not found");
        }
        try {
            ois = new ObjectInputStream(new FileInputStream(cachedFile));
            return ((CachedEntities) ois.readObject());
        } catch (Exception ex) {
            logger.info("Cannot return a cached entities object because " + ex);
            return null;
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
    }
}
