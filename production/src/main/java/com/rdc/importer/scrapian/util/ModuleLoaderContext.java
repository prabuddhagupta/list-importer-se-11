package com.rdc.importer.scrapian.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ModuleLoaderContext {
  public static boolean isRemoteEnv = true;
  public static String HEAD_REV = "HEAD";
  public static List<Callable> preActions = new ArrayList<>();
}