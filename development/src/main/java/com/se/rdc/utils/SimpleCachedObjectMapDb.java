package com.se.rdc.utils;

import com.se.rdc.core.utils.CompressionUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

public class SimpleCachedObjectMapDb {
	private long diskCacheExpireTime = TimeUnit.DAYS.toMillis(30);
	private long memoryCacheExpireTime = TimeUnit.MINUTES.toMillis(5);
	private double inMemoryHeapThreshold = 0.3;//0.3=30% of max heap size
	private long inMemoryEntryThreshold = 10000;
	private long executorInvokeInterval = TimeUnit.MINUTES.toMillis(5);
	private File diskFile = new File("./tmp/cached_data.db");//TODO: change it
	private String backupExt = ".bak";
	private final String dbStoreName = "db_store";
	private final String configStoreName = "db_config";
	private DB diskDb;
	private DB memoryDb;
	private HTreeMap<String, byte[]> cacheMap;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(
			Runtime.getRuntime().availableProcessors() - 1);
	private ThreadWaitNotifier waitNotifier = new ThreadWaitNotifier();
	private Boolean initFinished = false;
	private boolean doWaitForLoading = true;
	private boolean ignoreCache = false;
	private int compressionLevel = Deflater.NO_COMPRESSION;
	private boolean isSafeMode = false;
	private boolean isUsingBackupFile = false;

	public SimpleCachedObjectMapDb() {
	}

	public SimpleCachedObjectMapDb(boolean isSafeMode) {
		this.isSafeMode = isSafeMode;
	}

	public void init() {
		//open existing data file to load old un-expired data from it or create new one
		startThread("Cache_init", true);
	}

	public void close() {
		//Now write out all updated cached data
		startThread("Cache_close", false);
	}

	public void setIsIgnoreCache(boolean val) {
		this.ignoreCache = val;
	}

	public SimpleCachedObjectMapDb enableCompression(int level) {
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
	 *
	 * @param isWait
	 */
	public void isWaitForCacheLoading(boolean isWait) {
		doWaitForLoading = isWait;
	}

	public SimpleCachedObjectMapDb setDiskCacheExpireTime(int value, TimeUnit unit) {
		this.diskCacheExpireTime = unit.toMillis(value);

		return this;
	}

	public SimpleCachedObjectMapDb setStoreFile(String filePath) {
		diskFile = new File(filePath);
		diskDb = getDiskDb();

		return this;
	}

	private void startThread(String name, boolean isInit) {
		Thread thread = new Thread(getRunnable(isInit), name);
		thread.start();
	}

	private Runnable getRunnable(final boolean isInit) {
		final Runnable runnable = new Runnable() {
			@Override public void run() {
				if (isInit) {
					memoryDb = DBMaker.memoryDirectDB().make();
					diskDb = getDiskDb();
					initDbStore();
				} else {
					try {
						closeDbStore();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				initFinished = true;
				waitNotifier.doNotify();
			}
		};

		return runnable;
	}

	private DBMaker.Maker getDiskDbMaker(File diskFile) {
		DBMaker.Maker diskPtr = DBMaker.fileDB(
				diskFile).fileMmapEnableIfSupported().fileMmapPreclearDisable();
		if (isSafeMode) {
			diskPtr = diskPtr.transactionEnable();
		}

		return diskPtr;
	}

	private DB getDiskDb() {
		//try to create files
		if (!diskFile.exists()) {
			diskFile.getParentFile().mkdirs();
		}

		DB diskDb = null;
		DBMaker.Maker diskPtr = getDiskDbMaker(diskFile);
		try {
			diskDb = diskPtr.make();
			return diskDb;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				System.out.println("Reading from backup cache");
				DBMaker.Maker diskPtrBak = getDiskDbMaker(getBackUpFile());
				diskDb = diskPtrBak.make();
				isUsingBackupFile = true;
				return diskDb;

			} catch (Exception ex) {
				System.out.println("Can't recover from backup cache :(");
				ex.printStackTrace();
				diskFile.delete();
			}
		}

		return diskPtr.make();
	}

	public void remove(String key) {
		if (!ignoreCache) {
			waitTillDataLoaded();
			cacheMap.remove(key);
		}
	}

	public void put(String key, Object value) {
		if (!ignoreCache) {
			waitTillDataLoaded();

			if (value instanceof Serializable) {
				try {
					cacheMap.put(key, CompressionUtils.compress(value, compressionLevel));
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			} else {
				System.out.println(
						value.getClass() + " is not a Serializable hence the data can't be saved in cache. You could wrap it into 'toString()' or implement Serializable in it.");
			}
		}
	}

	public Object get(String key) {
		if (!ignoreCache) {
			waitTillDataLoaded();

			try {
				byte[] value = (byte[]) cacheMap.get(key);
				if (value != null) {
					return CompressionUtils.decompressToObject(value);
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		return null;
	}

	public HTreeMap<String, byte[]> getAll() {
		if (!ignoreCache) {
			waitTillDataLoaded();
		}

		return cacheMap;
	}

	public void waitTillDataLoaded() {
		while (!initFinished && doWaitForLoading && !ignoreCache) {
			//Wait while data is not yet loaded
			try {
				waitNotifier.doWait();
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			}
		}
	}

	private synchronized void initDbStore() {
		HTreeMap<String, byte[]> diskCache = diskDb.hashMap(
				dbStoreName).keySerializer(Serializer.STRING).valueSerializer(
				Serializer.BYTE_ARRAY).createOrOpen();

		cacheMap = memoryDb.hashMap(dbStoreName).keySerializer(
				Serializer.STRING).valueSerializer(Serializer.BYTE_ARRAY).expireMaxSize(
				inMemoryEntryThreshold).expireStoreSize(
				(long) (Runtime.getRuntime().maxMemory() * inMemoryHeapThreshold)).expireAfterGet(
				memoryCacheExpireTime).expireExecutor(executor).expireExecutorPeriod(
				executorInvokeInterval).expireOverflow(diskCache).createOrOpen();

		/* check for expiration date of disk db */
		HTreeMap<String, Long> configMap = diskDb.hashMap(configStoreName,
				Serializer.STRING, Serializer.LONG).createOrOpen();

		//using configStoreName as expire key
		Object timeObj = configMap.get(configStoreName);
		if (timeObj != null) {
			long initTime = (long) timeObj;
			boolean isTimeExpired = (Calendar.getInstance().getTimeInMillis() - (long) timeObj) > diskCacheExpireTime;
			if (isTimeExpired) {
				configMap.put(configStoreName,
						Calendar.getInstance().getTimeInMillis());
				diskCache.clear();
			} else {
				cacheMap.putAll(diskCache);
			}
		} else {
			configMap.put(configStoreName, Calendar.getInstance().getTimeInMillis());
		}
	}

	private void closeDbStore() throws IOException, InterruptedException {
		waitTillDataLoaded();
		if (!memoryDb.isClosed()) {
			cacheMap.clearWithExpire();
			memoryDb.close();
		}

		if (!diskDb.isClosed()) {
			//diskDb.getStore().compact();
			//diskDb.commit();
			diskDb.close();
			//createBackUp();
			executor.shutdown();
		}
	}

	private File getBackUpFile() throws IOException {
		File diskFile = new File(this.diskFile.getAbsolutePath() + backupExt);
		if (!diskFile.exists()) {
			diskFile.getParentFile().mkdirs();
			diskFile.createNewFile();
		}

		return diskFile;
	}

	private void createBackUp() {
		try{
		if (isUsingBackupFile) {
			Files.copy(getBackUpFile().toPath(), diskFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} else {
			Files.copy(diskFile.toPath(), getBackUpFile().toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		}
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	private class ThreadWaitNotifier {
		private Object lockObject = new Object();
		private boolean resumeSignal = false;

		public void doWait() {
			synchronized (lockObject) {
				//Changing if block into while block
				while (!resumeSignal) {
					try {
						lockObject.wait();
					} catch (InterruptedException e) {
					}
				}

				resumeSignal = false;
			}
		}

		public void doNotify() {
			synchronized (lockObject) {
				resumeSignal = true;
				lockObject.notify();
			}
		}
	}
}