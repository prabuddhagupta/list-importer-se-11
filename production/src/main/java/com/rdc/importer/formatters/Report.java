package com.rdc.importer.formatters;

import com.rdc.importer.formatters.GridReportFormatter;


public abstract class Report {
    public enum DataType { STRING, INTEGER, FLOAT, WRAPPED_TEXT,HYPER_LINK }
    private String title;
    private GridReportFormatter formatter;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public GridReportFormatter getFormatter() {
        return formatter;
    }

    public void setFormatter(GridReportFormatter formatter) {
        this.formatter = formatter;
    }

    public abstract void beginFormat() throws Exception;
    public abstract void endFormat() throws Exception;
}
