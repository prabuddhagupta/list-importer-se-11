package com.se.rdc.core.utils;

import org.h2.jdbc.JdbcConnection;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class H2DbBackupHandler extends H2DbHandler {
	private AtomicBoolean isUseBackupConnection = new AtomicBoolean(false);
	private static volatile String BACKUP_CONNECTION_URL;
	private JdbcConnection backupConnection; //use only to close the mem if conn is only one

	public H2DbBackupHandler(String dbPathUrl,
			ScheduledExecutorService executorService) throws SQLException {
		super(dbPathUrl, executorService);
	}

	public void initBackupProcess() {
		//BACKUP_CONNECTION_URL =
	}
}