package com.rdc.importer.scraper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import net.sf.ehcache.Cache;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.digester.Digester;

import com.rdc.scrape.ScrapeEntity;

/**
 * Created by IntelliJ IDEA.
 * User: rhickey
 * Date: 4/27/11
 * Time: 10:18 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Deserializer {
    private Digester digester;

    protected abstract void configureDigester(Digester digester);
    
    protected abstract Cache getEntityCache(Object digested);
    
    public synchronized Cache deserializeToCache(InputStream inputStream, String encoding) throws Exception {
        digester = new Digester();
        configureDigester(digester);
        Reader reader = getReaderWithoutBOM(inputStream, encoding);
        Object digested = digester.parse(reader);
        return getEntityCache(digested);
    }

    public Reader getReaderWithoutBOM(InputStream is, String encoding) throws Exception {
        if (StringUtils.isEmpty(encoding))
            encoding = Charset.defaultCharset().name();
        BufferedInputStream bis = new BufferedInputStream(is);
        bis.mark(0);
        int bomLen = getBOMLength(bis, encoding);
        bis.reset();
        long skipped = bis.skip(bomLen);
        if (skipped != bomLen) {
            throw new Exception("Error stripping BOM from input stream.") ;
        }
        return new InputStreamReader(bis, encoding);
    }

    private int getBOMLength(InputStream is, String encoding) throws Exception {

        byte[] bytes = new byte[5];
        int len = 0;
        int read = is.read(bytes);
        byte[] bracket = new String("<").getBytes(encoding);
        for (int i = 0; i < 5; i++) {
            if (bytes[i] == bracket[0]) {
                len = i;
                break;
            }
        }
        return len;
    }
}
