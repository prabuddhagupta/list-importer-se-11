package com.se.rdc.core.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FileLogger {
	private static Map<String, Logger> mapStore = Collections.synchronizedMap(
			new HashMap<String, Logger>());

	public synchronized static Logger getLogger(String file)
			throws FileNotFoundException {
		return getLogger(file, false);
	}

	public synchronized static Logger getLogger(String file, boolean isAppend)
			throws FileNotFoundException {
		Logger logger = mapStore.get(file);
		if (logger == null) {
			logger = new Logger(file, isAppend);
			mapStore.put(file, logger);
		}

		return logger;
	}

	public static void close() {
		for (Map.Entry e : mapStore.entrySet()) {
			((Logger) e.getValue()).printWriter.close();
		}
	}

	public static class Logger {
		private final String key;
		private final PrintWriter printWriter;

		private Logger(String file, boolean isAppend) throws FileNotFoundException {
			printWriter = new PrintWriter(new FileOutputStream(file, isAppend));
			this.key = file;
		}

		public PrintWriter getPrintWriter() {
			return printWriter;
		}

		public synchronized void log(String data) {
			printWriter.append(data);
			printWriter.append("\n");
		}

		public void close() {
			printWriter.close();
			mapStore.remove(key);
		}
	}
}