package com.rdc.importer.scrapian.model;

public interface ScrapianSource<F> {

    public F getValue();

    public String serialize() throws Exception;

}
