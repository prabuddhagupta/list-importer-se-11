package com.rdc.importer.scrapian;

import java.util.HashMap;
import java.util.Map;

import com.rdc.importer.scrapian.model.ListSource;
import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.transform.ElementSeekTransformer;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

public class ElementSeekTransformerUnitTest extends TestCase {

    public void testStartEnd() throws Exception {

        ElementSeekTransformer transformer = new ElementSeekTransformer();
        Map params = new HashMap();
        params.put("startText", "from trading in securities");
        params.put("endText", "Company Name");
        params.put("element", "a");
        params.put("greedy", false);
        params.put("greedyText", true);

        ScrapianSource scrapianSource = new StringSource(IOUtils.toString(ElementSeekTransformer.class.getResourceAsStream("/test_data/element_seek.html")));

        ListSource source = (ListSource) transformer.transform(scrapianSource, params);
        assertNotNull(source);
        assertEquals(27, source.getValue().size());
    }

    public void testEnd() throws Exception {

        ElementSeekTransformer transformer = new ElementSeekTransformer();
        Map params = new HashMap();
        params.put("endText", "Company Name");
        params.put("element", "a");
        params.put("greedy", false);
        params.put("greedyText", true);

        ScrapianSource scrapianSource = new StringSource(IOUtils.toString(ElementSeekTransformer.class.getResourceAsStream("/test_data/element_seek.html")));

        ListSource source = (ListSource) transformer.transform(scrapianSource, params);
        assertNotNull(source);
        assertEquals(80, source.getValue().size());
    }

    public void testStart() throws Exception {

        ElementSeekTransformer transformer = new ElementSeekTransformer();
        Map params = new HashMap();
        params.put("startText", "from trading in securities");
        params.put("element", "a");
        params.put("greedy", false);
        params.put("greedyText", true);

        ScrapianSource scrapianSource = new StringSource(IOUtils.toString(ElementSeekTransformer.class.getResourceAsStream("/test_data/element_seek.html")));

        ListSource source = (ListSource) transformer.transform(scrapianSource, params);
        assertNotNull(source);
        assertEquals(655, source.getValue().size());
    }

    /**
     * greedy text false will cause it not to jump to last occurrent of endText, but first.
     *
     * @throws Exception
     */
    public void testGreedyTextFalse() throws Exception {
        ElementSeekTransformer transformer = new ElementSeekTransformer();
        Map params = new HashMap();
        params.put("endText", "SearchByCompanyName");
        params.put("element", "a");
        params.put("greedyText", false);

        ScrapianSource scrapianSource = new StringSource(IOUtils.toString(ElementSeekTransformer.class.getResourceAsStream("/test_data/element_seek.html")));

        ListSource source = (ListSource) transformer.transform(scrapianSource, params);
        assertNotNull(source);
        assertEquals(48, source.getValue().size());
    }

    public void testGreedyTrue() throws Exception {
        ElementSeekTransformer transformer = new ElementSeekTransformer();
        Map params = new HashMap();
        params.put("startText", "from trading in securities");
        params.put("element", "a");
        params.put("greedy", true);

        ScrapianSource scrapianSource = new StringSource(IOUtils.toString(ElementSeekTransformer.class.getResourceAsStream("/test_data/element_seek.html")));

        ListSource source = (ListSource) transformer.transform(scrapianSource, params);
        assertNotNull(source);
        assertEquals(1, source.getValue().size());
    }

}
