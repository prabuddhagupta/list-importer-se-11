package com.rdc.importer.formatters;

import com.rdc.importer.formatters.Report;

abstract public class GridReportFormatter {

    public enum ColumnAlignment {
        LEFT, CENTER, RIGHT
    }

    private boolean inHeader = false;
    private boolean inFooter = false;

    public void beginFormat() throws Exception {
    }

    public void endFormat() throws Exception {
    }

    public void beginGrid() throws Exception {
    }

    public void endGrid() throws Exception {
    }

    public void beginRow() {
    }

    public void endRow() throws Exception {
    }

    public abstract void beginColumn();

    public abstract void setColumnType(Report.DataType t);

    public void setColumnAlignment(ColumnAlignment t) {
    }

    public void endColumn() {
    }  
    public void endColumn(String linkName) {
        endColumn();
    }
    public void setColumnWidths(int[] width) {
    }

    public void setNoOfColumns(int t) {
    }

    public void beginHeader() {
        inHeader = true;
    }

    public boolean isInHeader() {
        return inHeader;
    }

    public void endHeader() {
        inHeader = false;
    }

    public void beginFooter() {
        inFooter = true;
    }

    public boolean isInFooter() {
        return inFooter;
    }

    public void endFooter() {
        inFooter = false;
    }

    public void print(Object obj) throws Exception {
    }

    public void printWrappedColumn(String text) throws Exception {
        setColumnType(Report.DataType.WRAPPED_TEXT);
        beginColumn();
        if (text != null) {
            print(text);
        } else {
            print(" ");
        }
        endColumn();
    }

    public void printColumn(Object obj) throws Exception {
        beginColumn();
        if (obj != null) {
            print(obj);
        } else {
            print(" ");
        }
        endColumn();
    }

    public void printIntColumn(Object obj) throws Exception {
        beginColumn();
        setColumnType(Report.DataType.INTEGER);
        if (obj != null) {
            print(obj);
        } else {
            print(" ");
        }
        endColumn();
    }

    public void printHyperLinkColumn(Object obj, String linkName) throws Exception {
        setColumnType(Report.DataType.HYPER_LINK);
        beginColumn();
        if (obj != null) {
            print(obj);
        } else {
            print(" ");
        }
        endColumn(linkName);
    }

    public abstract void setTitle(String title);

    public abstract Object format(Report report) throws Exception;

    public abstract Object getResults();
}

