package com.rdc.importer.scrapian.transform;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.StringSource;
import org.apache.commons.lang.StringUtils;

public class DelimitedTextTransformer implements ScrapianTransformer {
    private boolean validate;
    private boolean escape;
    private boolean quoted;
    private List<String> headers;
    private String delimiter = ",";

    public ScrapianSource transform(ScrapianSource scrapianSource, Map<String, Object> parameters) throws Exception {
        this.validate = (parameters.get("validate") != null) ? (Boolean) parameters.get("validate") : false;
        this.escape = (parameters.get("escape") != null) ? (Boolean) parameters.get("escape") : false;
        this.quoted = (parameters.get("quoted") != null) ? (Boolean) parameters.get("quoted") : false;
        this.headers = (List<String>) parameters.get("headers");
        this.delimiter = (String) parameters.get("delimiter");
        return new StringSource(transform((String) scrapianSource.getValue()));
    }

    private String transform(String text) throws IOException {
        StringBuffer xmlBuffer = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes())));

        if (headers == null) {
            headers = extractHeaders(br.readLine());
        }

        xmlBuffer.append("<rows>\n");
        String line;
        while ((line = br.readLine()) != null) {

            List<String> columnValues = getColumnValues(line);
            if (!emptyRow(columnValues)) {
                xmlBuffer.append("<row>");
                for (int i = 0; i < headers.size(); i++) {

                    String columnValue;
                    if (i > columnValues.size() - 1) {
                        columnValue = "";
                    } else {
                        columnValue = columnValues.get(i);
                    }
                    XmlTranformUtils.addNode(xmlBuffer, headers.get(i), columnValue, validate, escape);
                }
                xmlBuffer.append("</row>\n");
            }
        }

        xmlBuffer.append("</rows>\n");
        return xmlBuffer.toString();
    }

    private boolean emptyRow(List<String> columnValues) {
        for (String columnValue : columnValues) {
            if (StringUtils.isNotBlank(columnValue)) {
                return false;
            }
        }
        return true;
    }

    private List<String> extractHeaders(String line) {
        List<String> ret = new ArrayList<String>();
        List<String> columnValues = getColumnValues(line);
        for (String columnValue : columnValues) {
            ret.add(XmlTranformUtils.flattenNodeName(columnValue));
        }
        return ret;
    }

    private List<String> getColumnValues(String line) {
        List<String> ret = new ArrayList<String>();
        String[] columnValues = line.split(quoted ? "\"" + delimiter + "\"" : delimiter);
        for (String columnValue : columnValues) {
            columnValue = columnValue.trim();
            if (quoted || (columnValue.startsWith("\"") && columnValue.endsWith("\""))) {
                columnValue = StringUtils.strip(columnValue, "\"");
            }
            ret.add(columnValue);
        }       
        return ret;
    }
}
