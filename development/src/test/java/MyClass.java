import com.rdc.github.service.GitHubServiceImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.Collection;

public class MyClass {


	private static final Logger logger = LogManager.getLogger(GitHubServiceImpl.class);
	private static String GIT_HUB_URL = "https://github.com/";
	private static String repoUser = "RegDC";
	private static String repoName = "scripts";
	private static String version = "f557839a4dce6f4bfc4082923861ba7d2aa1f71a";
	private static String scriptName = "utils/modules/ModulesFactory.groovy";

	public static void main(String[] args) {

	}


	//- Pull the "git_clone" repo to latest HEAD
	public void getLatestHead(){


	}

	private boolean pull(String localDir, String OAuth2Token) throws Exception {
		Git git = null;
		FileRepository db = null;
		try {
			db = new FileRepository(localDir + "/.git/");
			git = new Git(db);
			CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(OAuth2Token, "");
			git.pull().setCredentialsProvider(credentialsProvider).call();
			logger.info("Pull from remote done on " + localDir);
		} catch (Exception e) {
			throw e;
		} finally {
			if (git != null) {
				git.close();
			}
			if (db != null) {
				db.close();
			}
		}
		return true;
	}

	private String cloneRepositoryToVersion(String repoUser, String repoName, String OAuth2Token, String version, String localDir, boolean doPull, String lastHeadVersion) {
		String retVal = null;
		if (StringUtils.isBlank(repoUser) || StringUtils.isBlank(repoName) || StringUtils.isBlank(OAuth2Token)) {
			logger.error("Necessary values were not set!");
			return null;
		}
		if (StringUtils.isBlank(version)) {
			version = "HEAD";
		}
		String passedLocalDir = localDir;
		localDir = localDir.replaceAll("\\\\", "/");
		if (!localDir.endsWith("/")) {
			localDir = localDir + "/";
		}
		localDir += repoUser + "/" + repoName + "/";
		if (!new File(localDir + "/.git/").exists() || version.toUpperCase().equals("HEAD")) {
			boolean redoHead = true;
			if (version.toUpperCase().equals("HEAD") && new File(localDir + "/.git/").exists()) {
				Git git = null;
				FileRepository db = null;
				try {
					String localVersion = null;
					String remoteVersion = null;

					if(StringUtils.isNotBlank(lastHeadVersion)){
						localVersion = lastHeadVersion;
						logger.info("Last head gitRevision passed in was " + lastHeadVersion);
					} else {
						db = new FileRepository(localDir + "/.git/");
						git = new Git(db);
						localVersion = git.getRepository().getRef("HEAD").getObjectId().getName();
						logger.info("Got local gitRevision properly, was " + localVersion);
					}

					Collection<Ref> refs;
					CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(OAuth2Token, "");
					refs = Git.lsRemoteRepository().setHeads(true).setTags(false).setRemote(GIT_HUB_URL + repoUser + "/" + repoName + ".git").setCredentialsProvider(credentialsProvider).call();

					for (Ref ref : refs) {
						if (ref.getName().contains("master")) {
							remoteVersion = ref.getObjectId().getName();
							logger.info("Got remote gitRevision properly, was " + remoteVersion);
						}
					}
					if (localVersion.equals(remoteVersion)) {
						redoHead = false;
						logger.info("Wanted to update to HEAD, but checked versions and this HEAD already seen.");
					}
					retVal = remoteVersion;
				} catch (Exception e) {
					logger.error(e);
				} finally {
					if(git != null){
						git.close();
					}
					if(db != null){
						db.close();
					}
				}
			}
			if (redoHead) {
				if (!new File(localDir + "/.git/").exists()) {
					boolean cloned = cloneRepository(repoUser, repoName, OAuth2Token, passedLocalDir);
					if (!cloned) {
						logger.error("Could not clone the repo, a necessary step for setting to the right gitRevision!");
						return null;
					}
				} else {
					try {
						pull(localDir, OAuth2Token);
					} catch (Exception e) {
						logger.error("Could not perform pull on the repo, a necessary step for setting to the right gitRevision!", e);
						return null;
					}
				}
			}

			if (retVal == null && version.toUpperCase().equals("HEAD")) {
				Git git = null;
				FileRepository db = null;
				try {
					db = new FileRepository(localDir + "/.git/");
					git = new Git(db);
					retVal = git.getRepository().getRef("HEAD").getObjectId().getName();
					logger.info("Got local gitRevision properly with special check for HEAD, was " + retVal);
				} catch (Exception e) {
					logger.error(e);
				} finally {
					if (git != null) {
						git.close();
					}
					if (db != null) {
						db.close();
					}
				}
			}
		}
		if (!version.toUpperCase().equals("HEAD")) {
			Git git = null;
			FileRepository db = null;
			try {
				db = new FileRepository(localDir + "/.git/");
				git = new Git(db);
				Ref ref = git.reset().setMode(ResetCommand.ResetType.HARD).setRef(version).call();
				logger.info("Reset(hard): Local repo HEAD now points to " + ref.toString());
				retVal = ref.toString();
			} catch (Exception e) {
				if (doPull && (e.getMessage().contains("Missing unknown") || e.getMessage().contains("Cannot read"))) {
					try {
						logger.info("Requested gitRevision was not found in clone.  Attempting to correct");
						pull(localDir, OAuth2Token);
						return cloneRepositoryToVersion(repoUser, repoName, OAuth2Token, version, passedLocalDir, false, lastHeadVersion);
					} catch (Exception e2) {
						logger.error("Error after trying to perform pull on the repo", e2);
						return null;
					}
				}
				logger.error("Error after trying to perform reset to " + version + " on the repo", e);
				return null;
			} finally {
				if (git != null) {
					git.close();
				}
				if (db != null) {
					db.close();
				}
			}
		}
		return retVal;
	}




	/**Always removes the current clone and makes a new clone
	 * The clone will be made in folder: localDir + / + repoUser + / + repoName
	 * Returns true only if operation succeeded
	 */

	public boolean cloneRepository(String repoUser, String repoName, String OAuth2Token, String localDir) {
		if (StringUtils.isBlank(repoUser) || StringUtils.isBlank(repoName) || StringUtils.isBlank(OAuth2Token)) {
			logger.error("Necessary values were not set!");
			return false;
		}
		CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(OAuth2Token, "");
		localDir = localDir.replaceAll("\\\\", "/");
		if (!localDir.endsWith("/")) {
			localDir = localDir + "/";
		}
		localDir += repoUser + "/" + repoName + "/";

		File destinationFile = new File(localDir);
		Git git = null;
		try {
			FileUtils.deleteDirectory(destinationFile);
			git = Git.cloneRepository().setURI(GIT_HUB_URL + repoUser + "/" + repoName + ".git").setBranch("master").setDirectory(destinationFile).setCredentialsProvider(credentialsProvider)
					.call();
			logger.info("Repository from " + GIT_HUB_URL + repoUser + "/" + repoName + " cloned to " + localDir);
		} catch (Exception e) {
			logger.error(e);
			return false;
		} finally {
			if(git != null){
				git.close();
			}
		}
		return true;
	}

}
