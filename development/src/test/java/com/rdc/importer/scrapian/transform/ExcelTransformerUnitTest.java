package com.rdc.importer.scrapian.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.ByteArraySource;
import com.rdc.importer.scrapian.transform.ExcelTransformer;
import groovy.util.XmlSlurper;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jwarren
 * Date: 11/21/13
 * Time: 2:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelTransformerUnitTest extends TestCase {
    private static String XLS_FILE = "/test_data/excel_transformation_test.xls";
    private static String XLSX_FILE = "/test_data/excel_transformation_test.xlsx";

    @Test
    public void testTransformXls() throws Exception {
        ExcelTransformer transformer = new ExcelTransformer();
        Map params = new HashMap();
        params.put("validate", new Boolean(true));
        params.put("escape", new Boolean(true));

        System.out.println("Transforming Excel 97-2003 file " + XLS_FILE + " ...");
        ScrapianSource scrapianSource = new ByteArraySource(IOUtils.toByteArray(ExcelTransformer.class.getResourceAsStream(XLS_FILE)));
        StringSource source = (StringSource) transformer.transform(scrapianSource, params);
        assertNotNull(source);
        List rows = (new XmlSlurper().parseText(source.getValue())).children().list();
        assertEquals(926, rows.size());
    }

    @Test
    public void testTransformXlsx() throws Exception {
        ExcelTransformer transformer = new ExcelTransformer();
        Map params = new HashMap();
        params.put("validate", new Boolean(true));
        params.put("escape", new Boolean(true));

        System.out.println("Transforming Excel 2007 file " + XLSX_FILE + " ...");
        ScrapianSource scrapianSource = new ByteArraySource(IOUtils.toByteArray(ExcelTransformer.class.getResourceAsStream(XLSX_FILE)));
        StringSource source = (StringSource) transformer.transform(scrapianSource, params);
        assertNotNull(source);
        List rows = (new XmlSlurper().parseText(source.getValue())).children().list();
        assertEquals(926, rows.size());
    }
}
