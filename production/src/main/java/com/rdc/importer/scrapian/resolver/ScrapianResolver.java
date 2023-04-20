package com.rdc.importer.scrapian.resolver;

import com.rdc.importer.scrapian.ScrapianDataList;
import com.rdc.importer.scrapian.ScrapianScript;

public interface ScrapianResolver {
    public ScrapianScript resolveScript(String scriptName) throws Exception;

    public ScrapianDataList resolveDataList(String dataListName) throws Exception;
}
