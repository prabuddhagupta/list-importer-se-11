package com.rdc.importer.scrapian.util;

import groovy.lang.GroovyClassLoader;

import java.io.File;

public class ModuleLoader {
  //function for remote environment
  public static Object getFactory(String version) throws Exception {
    return RemoteModuleLoader.getModuleFactory(version);
  }

  public static Object getFactory() throws Exception {
    return getFactory(ModuleLoaderContext.HEAD_REV);
  }
}

class RemoteModuleLoader {
  private static volatile Object instance;
  private static volatile String version_key;
  private static String fileName = "ModulesFactory.groovy";

  static synchronized Object getModuleFactory(String version) throws Exception {
    if (instance != null) {
      if (!version.equals(version_key)) {
        version_key = version;
        instance = loadFactory(version);
      }

      return instance;
    }

    return instance = loadFactory(version);
  }

  private static Object loadFactory(String version) throws Exception {
    Class c = new GroovyClassLoader().parseClass(new File(GitHubModuleLocator.getLocalPath(fileName, version)));
    return c.getConstructor(String.class).newInstance(version);
  }
}