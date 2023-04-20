package com.se.rdc.core.utils;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

public class CachedDbObject {
  private long diskCacheExpireTime = TimeUnit.DAYS.toMillis(30);
  private final long memoryCacheExpireTime = TimeUnit.MINUTES.toMillis(5);
  private final double inMemoryHeapThreshold = 0.3;//0.n=>(0.n*100)% of max heap size
  private final long inMemoryEntryThreshold = 100000;
  private String baseDir = ".";
  private String dbFile = "/cache/cached_data";
  private final String dbStoreName = "db_store";/* + also used as a Lock*/
  private HTreeMap<String, byte[]> cacheMap;
  private HTreeMap<String, byte[]> newDataMap;
  private ScheduledExecutorService executor;
  private boolean doWaitForPartialLoading = true;
  private boolean doWaitForFullLoading = true;
  private H2DbHandler.QUERY_STATUS loadStatus;
  private boolean ignoreCache = false;
  private int compressionLevel = Deflater.NO_COMPRESSION;
  private H2KeyPairDbStore h2Db;
  private CountDownLatch dataLatch;

  private void presetForInit() {
    dbFile = baseDir + dbFile;
    dataLatch = new CountDownLatch(2);
    loadStatus = H2DbHandler.QUERY_STATUS.IN_PROGRESS;
    executor = Executors.newScheduledThreadPool(4, getThreadFactory());
  }

  public CachedDbObject init() throws SQLException {
    presetForInit();

    executor.submit(() -> {
      try {
        initDbStore();
      } catch (Exception e) {
        if (!(e instanceof RejectedExecutionException)) {
          e.printStackTrace();
        } else {
          System.out.println(
              "Cache is closed before complete initialization!");
        }
        loadStatus = H2DbHandler.QUERY_STATUS.ERROR;
        //release all latch wait block
        while (dataLatch.getCount() > 0) {
          dataLatch.countDown();
        }
      }
    });

    //add shutdown on main threads ending
    Thread currentThread = Thread.currentThread();
    executor.submit(() -> {
      try {
        if (!currentThread.isInterrupted() && currentThread.isAlive()) {
          currentThread.join();
        }
      } catch (InterruptedException ignored) {
      } finally {
        close();
      }
    });

    return this;
  }

  private void reInit() throws SQLException {
    try {
      closeDbStore(false);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    //release all latch wait block
    while (dataLatch.getCount() > 0) {
      dataLatch.countDown();
    }

    //re-init the cache db
    init();
  }

  public CachedDbObject close() {
    executor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          closeDbStore(true);
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
    });

    return this;
  }

  private ThreadFactory getThreadFactory() {
    return new ThreadFactory() {
      AtomicInteger ai = new AtomicInteger(0);

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r,
            CachedDbObject.class.getName() + " Thread-" + ai.incrementAndGet());
      }
    };
  }

  public CachedDbObject setBaseDir(String baseDir) {
    this.baseDir = baseDir;

    return this;
  }

  public CachedDbObject setIsIgnoreCache(boolean val) {
    this.ignoreCache = val;
    return this;
  }

  public CachedDbObject setCompressionLevel(int level) {
    //level: -1 to 9
    if (level > -2 && level < 10) {
      compressionLevel = level;
    }

    return this;
  }

  /**
   * For bigger cached data it will wait till
   * the cached data is completely loaded when a map-get call invoked.
   * With this method you could skip that checking.
   */
  public CachedDbObject setWaitForCacheLoading(boolean isPartialWait,
                                               boolean isFullWait) {
    doWaitForPartialLoading = isPartialWait;
    doWaitForFullLoading = isFullWait;
    return this;
  }

  public CachedDbObject setDiskCacheExpireTime(int value, TimeUnit unit) {
    this.diskCacheExpireTime = unit.toMillis(value);
    return this;
  }

  public CachedDbObject setDbFile(String filePath) throws SQLException {
    return setDbFile(filePath, false);
  }

  public CachedDbObject setDbFile(String filePath, boolean isTempDir) throws SQLException {
    baseDir = "";

    if (isTempDir) {
      dbFile = new File(System.getProperty("java.io.tmpdir"), filePath).getAbsolutePath();
    } else {
      dbFile = filePath;
    }

    if (executor != null) {
      reInit();
    }

    return this;
  }

  public CachedDbObject removeAll(String key) throws Exception {
    waitTillFullDataLoaded();
    cacheMap.clear();
    h2Db.removeAll();
    newDataMap.clear();

    return this;
  }

  public CachedDbObject remove(String key) throws Exception {
    waitTillFullDataLoaded();
    cacheMap.remove(key);
    h2Db.remove(key);
    newDataMap.remove(key);

    return this;
  }

  public CachedDbObject putAll(Map<String, Object> dataMap) throws Exception {
    waitTillFullDataLoaded();

    for (Map.Entry e : dataMap.entrySet()) {
      Object value = e.getValue();
      String key = (String) e.getKey();
      checkedPut(key, value);
    }

    return this;
  }

  public CachedDbObject put(String key, Object value) throws Exception {
    waitTillFullDataLoaded();
    checkedPut(key, value);

    return this;
  }

  private void checkedPut(String key, Object value) {
    if (value instanceof Serializable) {
      try {
        byte[] compressedValue = CompressionUtils.compress(value,
            compressionLevel);
        cacheMap.put(key, compressedValue);
        newDataMap.put(key, compressedValue);

      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    } else {
      System.out.println(
          value.getClass() + " is not a Serializable hence the data can't be saved in cache. You could wrap it into 'toString()' or implement Serializable in it.");
    }
  }

  public Object get(String key) throws Exception {
    byte[] value = null;

    //we did not init the cache; maybe forgot to do so
    if (loadStatus == null) {
      init();
    }

    if (loadStatus.equals(
        H2DbHandler.QUERY_STATUS.IN_PROGRESS) || loadStatus.equals(
        H2DbHandler.QUERY_STATUS.PARTIAL)) {
      waitTillMinimumDataLoaded();
      value = cacheMap.get(key);

      if (value == null) {
        waitTillFullDataLoaded();
        value = cacheMap.get(key);
      }
    } else {
      value = cacheMap.get(key);
    }

    try {
      if (value != null) {
        return CompressionUtils.decompressToObject(value);
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    return null;
  }

  public HTreeMap<String, byte[]> getAll() throws Exception {
    waitTillFullDataLoaded();
    return cacheMap;
  }

  private synchronized void waitTillMinimumDataLoaded() throws Exception {
    while (!ignoreCache && loadStatus.equals(
        H2DbHandler.QUERY_STATUS.IN_PROGRESS) && doWaitForPartialLoading) {
      if (executor == null) {
        throw new Exception(
            "Init method must be called first to use the cache");
      }
      //Wait while data is not yet loaded
      System.out.println("Waiting for the minimum data cache loading...");
      dataLatch.await();
    }
  }

  private synchronized void waitTillFullDataLoaded() throws Exception {
    while (!ignoreCache && (loadStatus.equals(
        H2DbHandler.QUERY_STATUS.IN_PROGRESS) || loadStatus.equals(
        H2DbHandler.QUERY_STATUS.PARTIAL)) && doWaitForFullLoading) {
      if (executor == null) {
        throw new Exception(
            "Init method must be called first to use the cache");
      }
      //Wait while data is not yet loaded
      System.out.println("Waiting for the full data cache loading...");
      dataLatch.await();
    }
  }

  private String getRandomString(DB db) {
    String value = "xyz";
    while (db.exists(value)) {
      value = String.valueOf(Calendar.getInstance().getTimeInMillis());
    }

    return value;
  }

  private void initDbStore() throws SQLException {
    h2Db = new H2KeyPairDbStore(dbFile, dbStoreName,
        executor).setExpirationTime(diskCacheExpireTime);

    DB mainMemoryDb = DBMaker.memoryDirectDB().make();

    HTreeMap<String, byte[]> diskCache = DBMaker.tempFileDB().make().hashMap(
        dbStoreName).keySerializer(Serializer.STRING).valueSerializer(
        Serializer.BYTE_ARRAY).createOrOpen();

    cacheMap = mainMemoryDb.hashMap(
        getRandomString(mainMemoryDb)).keySerializer(
        Serializer.STRING).valueSerializer(Serializer.BYTE_ARRAY).expireMaxSize(
        inMemoryEntryThreshold).expireStoreSize(
        (long) (Runtime.getRuntime().totalMemory() * inMemoryHeapThreshold)).expireAfterGet(
        memoryCacheExpireTime).expireExecutor(executor).expireExecutorPeriod(
        memoryCacheExpireTime).expireOverflow(diskCache).createOrOpen();

    newDataMap = mainMemoryDb.hashMap(
        getRandomString(mainMemoryDb)).keySerializer(
        Serializer.STRING).valueSerializer(
        Serializer.BYTE_ARRAY).createOrOpen();

    //collect data from h2 db
    h2Db.collectAllData(cacheMap, (long) (inMemoryEntryThreshold * .3),
        new H2DbHandler.DataLoadListener() {

          @Override
          public void onPostDataLoad(long rowCount,
                                     H2DbHandler.QUERY_STATUS status, Object returnValue) {
            setInitFinished(status);

            if (status.equals(H2DbHandler.QUERY_STATUS.DONE)) {
              //add into scheduled task for new data push into h2 db
              h2Db.setPeriodicAutoCommit(putNewDataIntoDb(),
                  memoryCacheExpireTime, TimeUnit.MILLISECONDS);
            }
          }
        });
  }

  private Runnable putNewDataIntoDb() {
    return () -> {
      try {
        if (!h2Db.isShutdownCalled()) {
          h2Db.putAll(newDataMap, true);
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    };
  }

  private void setInitFinished(H2DbHandler.QUERY_STATUS status) {
    loadStatus = status;
    dataLatch.countDown();
    if (status.equals(H2DbHandler.QUERY_STATUS.DONE)) {
      dataLatch.countDown();
    }
  }

  private void closeDbStore(boolean isWaitForFullData) throws Exception {
    if (isWaitForFullData) {
      waitTillFullDataLoaded();
    }
    if (h2Db != null) {
      putNewDataIntoDb().run();
      h2Db.removeExpired();
      h2Db.close(false);
    }
    if (executor != null) {
      executor.shutdownNow();
    }
  }
}