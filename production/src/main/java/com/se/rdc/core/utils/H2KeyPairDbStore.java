package com.se.rdc.core.utils;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//TODO: change column name into final variable + compact db size
public class H2KeyPairDbStore extends H2DbBackupHandler {
	private final String storeName;
	private long expirationTime = TimeUnit.DAYS.toMillis(30);

	public H2KeyPairDbStore(String dbPathUrl, String storeName,
			ScheduledExecutorService executor) throws SQLException {
		super(dbPathUrl, executor);
		this.storeName = storeName;
		String sqlQuery = "CREATE TABLE IF NOT EXISTS " + storeName + "(ID VARCHAR(100000) primary key, value BLOB, date TIMESTAMP)";
		executeUpdate(sqlQuery);
	}

	public H2KeyPairDbStore setExpirationTime(long milliSeconds) {
		expirationTime = milliSeconds;
		return this;
	}

	public byte[] get(String key) throws SQLException {
		byte[] retValue = new byte[0];
		PreparedStatement statement = buildQuery(
				"SELECT * FROM " + storeName + " WHERE ID=(?)");
		statement.setString(1, key);
		ResultSet result = statement.executeQuery();

		if (result.next()) {
			retValue = result.getBytes(2);
		}
		result.close();
		statement.close();

		return retValue;
	}

	public Future collectAllData(Map<String, byte[]> outputMap,
			H2DbHandler.DataLoadListener listener) throws SQLException {
		return collectAllData(outputMap, 0, listener);
	}

	public Future collectAllData(final Map<String, byte[]> outputMap,
			final long initialDataLoadThreshold, final H2DbHandler.DataLoadListener listener)
			throws SQLException {

		Runnable task = new Runnable() {
			@Override public void run() {
				try {
					//TaskTimer.startTrack("D");
					PreparedStatement statement = getAllDataSet(storeName);
					ResultSet result = statement.executeQuery();
					//TaskTimer.endTrackLaps("D");
					int cnt = 0;
					while (result.next()) {
						outputMap.put(result.getString(1), result.getBytes(2));
						cnt++;
						if (cnt == initialDataLoadThreshold) {
							listener.onPostDataLoad(cnt, H2DbHandler.QUERY_STATUS.PARTIAL, null);
						}
					}
					//TaskTimer.endTrackLaps("D");

					result.close();
					statement.close();
					listener.onPostDataLoad(cnt, H2DbHandler.QUERY_STATUS.DONE, null);
				} catch (SQLException e) {
					e.printStackTrace();
					listener.onPostDataLoad(0, H2DbHandler.QUERY_STATUS.ERROR, null);
				}
			}
		};

		return executor.submit(task);
	}

	public void putAll(Map<String, byte[]> sourceMap, boolean removeFromSource)
			throws SQLException {
		String insertQuery = "MERGE INTO " + storeName + " (ID,VALUE,DATE) VALUES (?,?,?)";
		PreparedStatement statement = getConnection().prepareStatement(insertQuery);

		Iterator<Map.Entry<String, byte[]>> itr = sourceMap.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<String, byte[]> e = itr.next();
			statement.setString(1, e.getKey());
			statement.setBlob(2, new ByteArrayInputStream(e.getValue()));
			statement.setTimestamp(3,
					new Timestamp(Calendar.getInstance().getTimeInMillis()));
			statement.addBatch();
			if (removeFromSource) {
				itr.remove();
			}
		}
		executeBatchStatement(statement);
	}

	public void put(String key, byte[] value) throws SQLException {
		String insertQuery = "MERGE INTO " + storeName + " (ID,VALUE,DATE) VALUES (?,?,?)";

		PreparedStatement statement = getConnection().prepareStatement(insertQuery);
		statement.setString(1, key);
		statement.setBlob(2, new ByteArrayInputStream(value));
		statement.setTimestamp(3,
				new Timestamp(Calendar.getInstance().getTimeInMillis()));
		executeStatement(statement);
	}

	public int removeExpired() throws SQLException {
		String query = "DELETE FROM " + storeName + " where " + expirationTime + "<DATEDIFF(millisecond, DATE, NOW())";
		return executeUpdate(query);
	}

	public int removeAll() throws SQLException {
		return removeAll(storeName);
	}

	public void remove(String key) throws SQLException {
		String insertQuery = "DELETE FROM " + storeName + "  WHERE ID=(?)";
		PreparedStatement statement = getConnection().prepareStatement(insertQuery);
		statement.setString(1, key);
		executeStatement(statement);
	}

	private void executeStatement(PreparedStatement statement)
			throws SQLException {
		statement.executeUpdate();
		statement.close();
	}

	private void executeBatchStatement(PreparedStatement statement)
			throws SQLException {
		statement.executeBatch();
		statement.close();
	}
}