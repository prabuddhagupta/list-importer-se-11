package com.se.rdc.core.utils;

import org.h2.jdbc.JdbcConnection;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class H2DbHandler {
	private final String DB_CONNECTION_ROOT = "jdbc:h2:";
	private final String CONNECTION_URL;
	private JdbcConnection connection;
	ScheduledExecutorService executor;
	private volatile boolean isShutDownCalled = false;

	public H2DbHandler(String dbPathUrl, ScheduledExecutorService executorService)
			throws SQLException {
		CONNECTION_URL = DB_CONNECTION_ROOT + dbPathUrl + newConfig().build();
		connection = getNewDBConnection();

		if (executorService == null) {
			executor = Executors.newSingleThreadScheduledExecutor(getThreadFactory());
		} else {
			executor = executorService;
		}
	}

	private ThreadFactory getThreadFactory() {
		return new ThreadFactory() {
			@Override public Thread newThread(@NotNull Runnable r) {
				return new Thread(r, H2DbHandler.class.getName());
			}
		};
	}

	public Config newConfig() {
		return new Config();
	}

	public void setExecutor(ScheduledExecutorService executorService) {
		if (executorService != null) {
			executor.shutdown();//shutdown prev executor
			executor = executorService;
		}
	}

	private boolean tableExist(Connection conn, String tableName)
			throws SQLException {
		boolean tExists = false;
		try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName,
				null)) {
			while (rs.next()) {
				String tName = rs.getString("TABLE_NAME");
				if (tName != null && tName.equals(tableName)) {
					tExists = true;
					break;
				}
			}
		}
		return tExists;
	}

	public void createOrOpenTable(String tableName, String sqlQuery)
			throws SQLException {
		if (!tableExist(getConnection(), tableName)) {
			executeUpdate(sqlQuery);
		}
	}

	/**
	 * Close the PreparedStatement instance when work is done
	 *
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	public PreparedStatement getAllDataSet(String tableName) throws SQLException {
		return buildQuery("SELECT * FROM " + tableName);
	}

	public int removeAll(String tableName) throws SQLException {
		return executeUpdate("TRUNCATE TABLE " + tableName);
	}

	public PreparedStatement buildQuery(String sqlQuery) throws SQLException {
		return getConnection().prepareStatement(sqlQuery);
	}

	public int executeUpdate(String sql) throws SQLException {
		int affectedRows = 0;
		PreparedStatement createPreparedStatement = getConnection().prepareStatement(
				sql);
		affectedRows = createPreparedStatement.executeUpdate();
		createPreparedStatement.close();

		return affectedRows;
	}

	protected JdbcConnection getConnection() {
		synchronized (this) {
			//TODO: remove this synchronized block when done with different process
		}
		return connection;
	}

	private JdbcConnection getNewDBConnection() throws SQLException {
		if (connection != null && !connection.isClosed()) {
			return connection;
		}

		try {
			connection = (JdbcConnection) DriverManager.getConnection(CONNECTION_URL);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		connection.setAutoCommit(true);

		return connection;
	}

	protected int getCurrentTotalConnections() throws SQLException {
		int rows = 0;
		ResultSet rs = buildQuery(
				"select count(*) from information_schema.sessions").executeQuery();
		rs.next();
		rows = rs.getInt(1);
		rs.close();

		return rows;
	}

	public ScheduledFuture<?> setPeriodicAutoCommit(final Runnable optionalTask,
			long delayTime, TimeUnit unit) {
		Runnable wrapperTask = new Runnable() {
			@Override public void run() {
				if (optionalTask != null) {
					optionalTask.run();
				}
				try {
					connection.commit();

					synchronized (this) {
						if (!isShutDownCalled && connection.getSession().hasPendingTransaction()) {
							connection.getSession().close();
							connection = getNewDBConnection();
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		};

		return executor.scheduleWithFixedDelay(wrapperTask, delayTime, delayTime,
				unit);
	}

	public boolean isShutdownCalled() throws SQLException {
		return isShutDownCalled;
	}

	public void close(boolean isCloseExecutor)
			throws SQLException, InterruptedException {
		isShutDownCalled = true;
		synchronized (this) {
		}
		connection.commit();
		if (getCurrentTotalConnections() < 2) {
			connection.createStatement().execute("SHUTDOWN COMPACT");
			//			connection.close();
			//			Thread.sleep(60000);
		} else {
			connection.getSession().close();
		}
		if (isCloseExecutor && executor != null) {
			executor.shutdown();
		}
	}

	public interface DataLoadListener {
		void onPostDataLoad(long rowCount, QUERY_STATUS status, Object returnValue);
	}

	public enum QUERY_STATUS {
		IN_PROGRESS, DONE, PARTIAL, ERROR
	}

	public static class ATTR {
		private interface VALUES {
			String getValue();
		}

		public enum CREDENTIAL implements VALUES {
			DEFAULT("", "");
			String val;
			private String userName = ";USER=";
			private String pass = ";PASSWORD=";

			CREDENTIAL(String userName, String pass) {
				this.userName += userName;
				this.pass += pass;
				val = this.userName + this.pass;
			}

			public String getValue() {
				return val;
			}
		}

		public enum LOCK_TYPE implements VALUES {
			SOCKET("SOCKET"), FILE("FILE");

			String val = ";FILE_LOCK=";

			LOCK_TYPE(String s) {
				val += s;
			}

			public String getValue() {
				return val;
			}
		}

		public enum AUTO_SERVER implements VALUES {
			TRUE("TRUE"), FALSE("FALSE");

			String val = ";AUTO_SERVER=";

			AUTO_SERVER(String s) {
				val += s;
			}

			public String getValue() {
				return val;
			}
		}

		public enum CLOSE_ON_EXIT implements VALUES {
			TRUE("TRUE"), FALSE("FALSE");

			String val = ";DB_CLOSE_ON_EXIT=";

			CLOSE_ON_EXIT(String s) {
				val += s;
			}

			public String getValue() {
				return val;
			}
		}

		public enum CLOSE_DELAY_DURATION implements VALUES {
			ZERO("0"), ONE_MIN("60"), FIVE_MIN("300"), TEN_MIN("600");

			String val = ";DB_CLOSE_DELAY=";

			CLOSE_DELAY_DURATION(String s) {
				val += s;
			}

			public String getValue() {
				return val;
			}
		}

		public enum HOT_CACHE_SIZE implements VALUES {
			NONE("0"), ONE_GB("1024"), HALF_OF_MEMORY(
					(Runtime.getRuntime().totalMemory() / 2) + "");

			String val = ";CACHE_SIZE=";

			HOT_CACHE_SIZE(String s) {
				val += s;
			}

			public String getValue() {
				return val;
			}
		}
	}

	public class Config {
		private final Map<Class<? extends ATTR.VALUES>, ATTR.VALUES> DEFAULT_ATTRS = new HashMap<>();

		//setting default values
		{
			DEFAULT_ATTRS.put(ATTR.CREDENTIAL.class, ATTR.CREDENTIAL.DEFAULT);
			DEFAULT_ATTRS.put(ATTR.LOCK_TYPE.class, ATTR.LOCK_TYPE.FILE);
			DEFAULT_ATTRS.put(ATTR.AUTO_SERVER.class, ATTR.AUTO_SERVER.TRUE);
			DEFAULT_ATTRS.put(ATTR.CLOSE_DELAY_DURATION.class,
					ATTR.CLOSE_DELAY_DURATION.ZERO);
			DEFAULT_ATTRS.put(ATTR.CLOSE_ON_EXIT.class, ATTR.CLOSE_ON_EXIT.FALSE);
			DEFAULT_ATTRS.put(ATTR.HOT_CACHE_SIZE.class, ATTR.HOT_CACHE_SIZE.NONE);
		}

		public void enableRemoteConnection() {
			DEFAULT_ATTRS.put(ATTR.LOCK_TYPE.class, ATTR.LOCK_TYPE.FILE);
		}

		public String build() throws SQLException {
			StringBuilder sb = new StringBuilder();
			for (ATTR.VALUES entry : DEFAULT_ATTRS.values()) {
				sb.append(entry.getValue());
			}

			return sb.toString();
		}

		public H2DbHandler getHandler() {
			return H2DbHandler.this;
		}
	}
}
