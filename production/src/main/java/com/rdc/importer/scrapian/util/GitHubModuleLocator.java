package com.rdc.importer.scrapian.util;

import com.rdc.importer.scrapian.ScrapianEngine;

import java.util.concurrent.Callable;

public class GitHubModuleLocator {
  private static final String gitHubRepo = "https://github.com/RegDC/scripts";

  static {
    //execute any configuration tasks
    try {
      if (ModuleLoaderContext.preActions.size() > 0) {
        for (Callable preAction : ModuleLoaderContext.preActions) {
          preAction.call();
        }

      }
    } catch (Exception ignored) {
    }
  }


  public static String getLocalPath(String fileName, String version, boolean isFindInModuleDir) throws Exception {
    String moduleDir = isFindInModuleDir ? "utils/modules/" : "";
    String scriptLocation = new ScrapianEngine().getGitHubURLAsString(gitHubRepo, moduleDir + fileName, version);

    return scriptLocation.replaceAll("file:/", "");
  }

  public static String getLocalPath(String fileName) throws Exception {
    return getLocalPath(fileName, "HEAD", true);
  }

  public static String getLocalPath(String fileName, String version) throws Exception {
    return getLocalPath(fileName, version, true);
  }
}
