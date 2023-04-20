package com.rdc.importer.scrapian.util;

import scrapian_scripts.utils.modules.ModulesFactoryLocal;

public class ModuleLoader {

  private static volatile ModulesFactoryLocal instance;

  public static ModulesFactoryLocal getFactory() {
    return getFactory(ModuleLoaderContext.HEAD_REV);
  }

  public static ModulesFactoryLocal getFactory(String version) {
    if (instance == null) {
      instance = new ModulesFactoryLocal(version);
    }

    return instance;
  }
}