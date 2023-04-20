package com.se.rdc.utils;

import com.se.rdc.core.utils.CompressionUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

/**
 * Created by omar on 1/16/16.
 */

public class SimpleCachedObject {
	private long timeOut = TimeUnit.DAYS.toSeconds(60);
	private File diskFile = new File("./tmp/cached_data.ser");
	private String backupExt = ".bak";
	private DataStore dataStore = new DataStore();
	private boolean initFinished = false;
	private boolean doWaitForLoading = true;
	private boolean ignoreCache = false;
	private int compressionLevel = Deflater.NO_COMPRESSION;

	public void init() {
		//open existing data file to load old un-expired data from it or create new one
		startThread(true);
	}

	public void close() {
		//Now write out all updated cached data
		startThread(false);
	}

	public void setIsIgnoreCache(boolean val) {
		this.ignoreCache = val;
	}

	public SimpleCachedObject enableCompression(int level) {
		//level: -1 to 9
		if (level > -2 && level < 10) {
			compressionLevel = level;
		}

		return this;
	}

	private void startThread(boolean isInit) {
		Thread thread = new Thread(getRunnable(isInit));
		thread.start();
	}

	private Runnable getRunnable(final boolean isInit) {
		final Runnable runnable = new Runnable() {
			@Override public void run() {
				if (isInit) {
					initDeSerializerTask(0);
				} else {
					initSerializerTask();
				}
				initFinished = true;
			}
		};

		return runnable;
	}

	public SimpleCachedObject setTimeOut(double hour) {
		this.timeOut = (long) (hour * 3600);

		return this;
	}

	public SimpleCachedObject setStoreFile(String filePath) {
		diskFile = new File(filePath);

		return this;
	}

	public void remove(String key) {
		if (!ignoreCache) {
			waitTillDataLoaded();
			dataStore.cachedData.remove(key);
		}
	}

	public void put(String key, Object value) {
		if (!ignoreCache) {
			waitTillDataLoaded();

			if (value instanceof Serializable) {
				try {
					dataStore.cachedData.put(key,
							CompressionUtils.compress(value, compressionLevel));
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			} else {
				System.out.println(
						value.getClass() + " is not a Serializable hence the data can't be saved in cache. You could wrap it into 'toString()' or implement Serializable in it.");
			}
		}
	}

	public HashMap<String, byte[]> getAll() {
		if (!ignoreCache) {
			waitTillDataLoaded();
		}
		return dataStore.cachedData;
	}

	public Object get(String key) {
		if (!ignoreCache) {
			waitTillDataLoaded();

			try {
				byte[] value = dataStore.cachedData.get(key);
				if (value != null) {
					return CompressionUtils.decompressToObject(value);
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		return null;
	}

	/**
	 * For bigger cahed data it will wait till
	 * the cached data is completely loaded when a map-get call invoked.
	 * With this method you could skip that checking.
	 *
	 * @param isWait
	 */
	public void isWaitForCacheLoading(boolean isWait) {
		doWaitForLoading = isWait;
	}

	private void waitTillDataLoaded() {
		while (!initFinished && doWaitForLoading && !ignoreCache) {
			//Wait while data is not yet loaded
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			}
		}
	}

	private synchronized void initDeSerializerTask(int readCounter) {
		try {
			FileInputStream fileIn;
			if (readCounter == 0) {
				fileIn = new FileInputStream(getFile());
			} else {
				fileIn = new FileInputStream(getBackUpFile());
			}
			ObjectInputStream in = new ObjectInputStream(fileIn);
			DataStore oldDataStore = (DataStore) in.readObject();

			boolean isTimeExpired = (Calendar.getInstance().getTimeInMillis() / 1000 - oldDataStore.initTime) > timeOut;
			if (!isTimeExpired) {
				dataStore = oldDataStore;
			}

			in.close();
			fileIn.close();
		} catch (Exception e) {
			System.out.println("Cache read exception: " + e.getMessage());
			if (readCounter == 0) {
				System.out.println("Reading from backup cache");
				initDeSerializerTask(1);
			} else if (readCounter == 1) {
				e.printStackTrace();
			}
		}
	}

	private synchronized void initSerializerTask() {
		try {
			FileOutputStream fileOut = new FileOutputStream(getFile());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(dataStore);
			out.close();
			fileOut.close();
			createBackUp();
		} catch (Exception e) {
			System.out.println("Cache write exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private File getFile() throws IOException {
		if (!diskFile.exists()) {
			diskFile.getParentFile().mkdirs();
			diskFile.createNewFile();
		}

		return diskFile;
	}

	private File getBackUpFile() throws IOException {
		File diskFile = new File(this.diskFile.getAbsolutePath() + backupExt);
		if (!diskFile.exists()) {
			diskFile.getParentFile().mkdirs();
			diskFile.createNewFile();
		}

		return diskFile;
	}

	public void createBackUp() throws IOException {
		Files.copy(diskFile.toPath(), getBackUpFile().toPath(),
				StandardCopyOption.REPLACE_EXISTING);
	}

	private static class DataStore implements Serializable {
		private static final long serialVersionUID = 42L;
		long initTime = Calendar.getInstance().getTimeInMillis() / 1000;
		HashMap<String, byte[]> cachedData = new HashMap<>();
	}
}