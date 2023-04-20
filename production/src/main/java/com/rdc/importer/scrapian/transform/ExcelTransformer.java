package com.rdc.importer.scrapian.transform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.*;

import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.StringSource;

import groovy.lang.Closure;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelTransformer implements ScrapianTransformer {
    private boolean validate;
    private boolean escape;
    private List<String> headers;
    private Closure headerClosure;
    private Closure validRowClosure;
    private String sheet;
    private String password;


    public ScrapianSource transform(ScrapianSource scrapianSource, Map<String, Object> parameters) throws Exception {
        setParams(parameters);
        Workbook workbook = createWorkbookFromStream(getStream(scrapianSource), null);
        if (sheet != null) {
            return parseSheet(workbook, sheet);
        } else {
            return parseSheet(workbook, workbook.getSheetName(0));
        }
    }

    public ScrapianSource[] transformMultiSheets(ScrapianSource scrapianSource, Map<String, Object> parameters)
        throws Exception {
        setParams(parameters);
        Workbook workbook = createWorkbookFromStream(getStream(scrapianSource), null);
        int numberOfSheets = workbook.getNumberOfSheets();
        ScrapianSource[] sources = new ScrapianSource[numberOfSheets];
        for (int i = 0; i < numberOfSheets; i++) {
            sources[i] = parseSheet(workbook, workbook.getSheetName(i));
        }
        return sources;
    }

    private void setParams(Map<String, Object> parameters) {
        this.validate = (parameters.get("validate") != null) ? (Boolean) parameters.get("validate") : false;
        this.escape = (parameters.get("escape") != null) ? (Boolean) parameters.get(
            "escape") : false;
        this.headerClosure = (Closure) parameters.get("headerClosure");
        this.validRowClosure = (Closure) parameters.get("validRowClosure");
        this.sheet = (String) parameters.get("sheet");
        this.headers = (List<String>) parameters.get("headers");
        this.password = (String) parameters.get("password");
    }

    //To get the inputstream from scrapianSource
    private InputStream getStream(ScrapianSource scrapianSource) {
        InputStream inputStream = new ByteArrayInputStream((byte[]) scrapianSource.getValue());
        return inputStream;
    }

    private Workbook createWorkbookFromStream(InputStream inputStream, String passwordIn) throws Exception {
        if (passwordIn == null) {
            passwordIn = password;
        }
        if (StringUtils.isNotEmpty(passwordIn)) {
            org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword(passwordIn);
        } else {
            org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword("");
        }
        POIFSFileSystem fs = null;
        Workbook workbook = null;
        try {
            fs = new POIFSFileSystem(inputStream);
            try {
                workbook = WorkbookFactory.create(fs);
            } catch (IllegalArgumentException iae) {
                if (iae.getMessage().contains("The supplied POIFSFileSystem does not contain a BIFF8 'Workbook' entry. Is it really an excel file?")) {

                    EncryptionInfo info = new EncryptionInfo(fs);
                    Decryptor d = Decryptor.getInstance(info);

                    try {
                        if (!d.verifyPassword(passwordIn)) {
                            throw new RuntimeException("Unable to process: document is encrypted");
                        }

                        inputStream = d.getDataStream(fs);
                        workbook = WorkbookFactory.create(inputStream);
                        if (workbook == null) {
                            throw iae;
                        }
                    } catch (GeneralSecurityException ex) {
                        throw new RuntimeException("Unable to process encrypted document", ex);
                    }
                }
            }
        } catch (OfficeXmlFileException oxfe) {
            inputStream.reset();
            workbook = WorkbookFactory.create(inputStream);
        }
        return workbook;
    }

    private StringSource parseSheet(Workbook workbook, String name) throws Exception {
        StringBuffer xmlBuffer = new StringBuffer();
        Sheet sheet = createSheetByName(workbook, name);
        if (sheet != null) {
            Iterator rowIterator = sheet.rowIterator();

            if (headers == null) {
                headers = extractHeaders(rowIterator);
            }
            xmlBuffer.append("<rows>\n");
            while (rowIterator.hasNext()) {
                Row row = (Row) rowIterator.next();
                List<String> columnValues = getColumnValues(row);
                if (validRow(columnValues)) {
                    xmlBuffer.append("<row>");
                    for (int i = 0; i < headers.size(); i++) {
                        String text;
                        if (i > columnValues.size() - 1) {
                            text = "";
                        } else {
                            text = columnValues.get(i);
                        }
                        XmlTranformUtils.addNode(xmlBuffer, headers.get(i), text, validate, escape);
                    }
                    xmlBuffer.append("</row>\n");
                }
            }
            xmlBuffer.append("</rows>\n");
        }
        return new StringSource(xmlBuffer.toString());
    }

    private boolean validRow(List<String> columnValues) {
        if (validRowClosure != null) {
            return !emptyRow(columnValues) && (Boolean) validRowClosure.call(columnValues);
        } else {
            return !emptyRow(columnValues);
        }
    }

    private boolean emptyRow(List<String> columnValues) {
        for (String columnValue : columnValues) {
            if (StringUtils.isNotBlank(columnValue)) {
                return false;
            }
        }
        return true;
    }

    private List<String> extractHeaders(Iterator rowIterator) {
        List<String> flattenedHeaderValues = new ArrayList<String>();
        List<String> headerValues = getHeaderValues(rowIterator);
        for (String headerValue : headerValues) {
            flattenedHeaderValues.add(XmlTranformUtils.flattenNodeName(headerValue));
        }
        return flattenedHeaderValues;
    }

    private List<String> getHeaderValues(Iterator rowIterator) {
        List<String> columnValues;
        if (headerClosure != null) {
            while (rowIterator.hasNext()) {
                Row row = (Row) rowIterator.next();
                columnValues = getColumnValues(row);
                if ((Boolean) headerClosure.call(columnValues)) {
                    return cleanupEmptyColumns(columnValues);
                }
            }
        } else {
            if (rowIterator.hasNext()) {
                Row row = (Row) rowIterator.next();
                columnValues = getColumnValues(row);
                return cleanupEmptyColumns(columnValues);
            }
        }
        return null;
    }

    private List<String> cleanupEmptyColumns(List<String> columnValues) {
        List<String> headers = new ArrayList<String>();
        for (int i = 0; i < columnValues.size(); i++) {
            if (columnValues.get(i).equals("")) {
                headers.add("column_" + i);
            } else {
                headers.add(columnValues.get(i));
            }
        }
        return headers;
    }

    private List<String> getColumnValues(Row row) {
        List<String> columnValues = new ArrayList<String>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell columnCell = row.getCell(i);
            String columnValue = "";
            if (columnCell != null && columnCell.toString() != null) {
                columnValue = columnCell.toString().trim();
                if (columnValue.startsWith("\"") && columnValue.endsWith("\"")) {
                    columnValue = columnValue.substring(1, columnValue.length() - 1).trim();
                }
                columnValue = columnValue.replace((char) 160, ' ').trim();
            }
            columnValues.add(columnValue);
        }
        return columnValues;
    }

    public Sheet createSheet(InputStream inputStream, String passwordIn) throws Exception {
        Workbook workbook = createWorkbookFromStream(inputStream, passwordIn);
        if (sheet != null) {
            return workbook.getSheet(sheet);
        } else {
            return workbook.getSheetAt(0);
        }
    }

    private Sheet createSheetByName(Workbook workbook, String name) {
        if (name != null) {
            return workbook.getSheet(name);
        }
        return null;
    }
}