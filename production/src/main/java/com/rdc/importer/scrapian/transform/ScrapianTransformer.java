package com.rdc.importer.scrapian.transform;

import java.util.Map;

import com.rdc.importer.scrapian.model.ScrapianSource;

public interface ScrapianTransformer {
    public ScrapianSource transform(ScrapianSource scrapianSource, Map<String, Object> parameters) throws Exception;
}
