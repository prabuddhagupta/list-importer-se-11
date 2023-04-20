package com.rdc.importer.scrapian.util;

import java.util.UUID;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public class EhCacheUtil {
    private static Logger logger = LogManager.getLogger(EhCacheUtil.class);
    private static CacheManager cm = CacheManager.getInstance();

    public static synchronized Cache addCache(String cacheName) {
        Cache retCache;
        if (cm.cacheExists(cacheName)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                //we'll live
            }
            cacheName += "_" + UUID.randomUUID();
            cm.addCache(cacheName);
            retCache = cm.getCache(cacheName);
        } else {
            cm.addCache(cacheName);
            retCache = cm.getCache(cacheName);
        }
        return retCache;
    }
    
    public static CacheManager getCacheManager(){
        return cm;
    }

    public static void shutdownStartNew() {
        cm.shutdown();
        cm = CacheManager.getInstance();      
    }
}
