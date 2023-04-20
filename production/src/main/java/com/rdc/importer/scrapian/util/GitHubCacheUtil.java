package com.rdc.importer.scrapian.util;

import java.io.File;

import javax.annotation.Resource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.rdc.github.service.GitHubServiceImpl;

@Component
public class GitHubCacheUtil {
	private static String baseDir;
	private static String localCloneDir;
	private static String OAuth2Token;
	private static GitHubServiceImpl ghsi = new GitHubServiceImpl();
    private static Logger logger = LogManager.getLogger(GitHubCacheUtil.class);
    private static String lastHeadVersion = null;
    
    @Resource
    public void setConfiguration(final Configuration configuration) {
        baseDir = configuration.getString("com.rdc.importer.gitHub.CacheBaseDir");
        localCloneDir = configuration.getString("com.rdc.importer.gitHub.localCloneDir");
        OAuth2Token = configuration.getString("com.rdc.importer.gitHub.OAuth2Token");        
    }
    
    /**
     * For the HEAD version, we seek to avoid recopying to the local DIR every time it is called.  Hence we store the last head version here
     * and pass to the GitHubService.  If no change in version detected, we do no more work (HEAD only).
     */
	public static synchronized String getResourceFilePath(String repoUser, String repoName, String fileName, String version) throws Exception {
		if (StringUtils.isBlank(version)) {
			version = "HEAD";
		}
		
		File wanted = new File(baseDir + repoUser + "/" + repoName + "/" + version + "/" + fileName);
		if (wanted.exists() && !version.toUpperCase().equals("HEAD")) {
			logger.info("Wanted version " + version +  " already found in cache.");
			return "file:///" + wanted.getAbsolutePath();
		} else {
			logger.info("HEAD".equals(version) ? "HEAD requested, checking if need to update" : 
				"Wanted version " + version + " not found in cache, setting clone to version in prep for caching");
			String outVersion = ghsi.cloneRepositoryToVersion(repoUser, repoName, OAuth2Token, version, localCloneDir, lastHeadVersion);
	    boolean same = version.equals("HEAD") && outVersion != null && outVersion.equals(lastHeadVersion) ? true : false;
			
			if (version.toUpperCase().equals("HEAD")) {
				if (same) {
					logger.info("Requesting the HEAD, but there have been no updates since we last checked and cached it.");
				} else {
					if (lastHeadVersion == null) {
						logger.info("Requesting the HEAD, and since first call of this List Importer, refreshing local cache");
					} else {
						logger.info("Requesting the HEAD, and detected a version change so refreshing local cache");
					}
				}
			}
			
			if(version.equals("HEAD")){
				lastHeadVersion = outVersion;
				logger.info("lastHeadVersion now set to " + lastHeadVersion);
			}
			
			boolean madeDir = false;
			if (StringUtils.isNotBlank(outVersion)) {
				File wantedDir = new File(baseDir + repoUser + "/" + repoName + "/" + version + "/");
				if (!wantedDir.exists()) {
					wantedDir.mkdirs();
					madeDir = true;
					logger.info("Made dir " + baseDir + repoUser + "/" + repoName + "/" + version + "/");
				} else {
					if (!version.toUpperCase().equals("HEAD") || (version.toUpperCase().equals("HEAD") && !same)) {
						logger.info("Attempting to clean dir " + baseDir + repoUser + "/" + repoName + "/" + version + "/");
						FileUtils.cleanDirectory(wantedDir);
					}
				}
				if (madeDir || !version.toUpperCase().equals("HEAD") || (version.toUpperCase().equals("HEAD") && !same)) {
					logger.info("Copying clone to " + baseDir + repoUser + "/" + repoName + "/" + version + "/");
					FileUtils.copyDirectory(new File(localCloneDir, repoUser + "/" + repoName + "/"), wantedDir);
				}
				if (wanted.exists()) {
					return "file:///" + wanted.getAbsolutePath();
				}
			} else {
				logger.error("cloneRepositoryToVersion failed for version " + version);
			}
		}
		throw new Exception("Unable to locate[" + fileName + "] in " + repoUser + "/" + repoName + "/ at version: " + version);
	}
}
