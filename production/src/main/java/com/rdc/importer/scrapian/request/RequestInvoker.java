package com.rdc.importer.scrapian.request;

import com.rdc.importer.scrapian.model.ScrapianRequest;
import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.model.ByteArraySource;

public interface RequestInvoker {

    public StringSource invoke(ScrapianRequest request, String encoding) throws Exception;

    public ByteArraySource invokeBinary(ScrapianRequest scrapianRequest) throws Exception;
}
