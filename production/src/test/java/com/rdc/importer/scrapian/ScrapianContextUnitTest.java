package com.rdc.importer.scrapian;

import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.service.AddressParser;
import com.rdc.scrape.ScrapeAddress;
import com.rdc.scrape.ScrapeEntity;
import com.rdc.scrape.ScrapeSession;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScrapianContextUnitTest extends TestCase {


    public void testParsePersonNames() {

        ScrapianContext scrapianContext = new ScrapianContext();
        scrapianContext.setScrapeSession(new ScrapeSession(null));

        StringSource nameSource = new StringSource("Thomas F. Baxter, Mark Shapeton, Jimmy Bulger, James Joseph Bulger, James J. Bulger, Jr., James Joseph Bulger, Jr., Tom Harris, Tom Marshall, Ernest E. Beaudreau, Harold W. Evers, Robert William Hanson, \"Whitey\"");

        String[] names = scrapianContext.splitCommaPersonNames(nameSource);
        assertEquals(12, names.length);

        nameSource = new StringSource("john smith");
        assertEquals(1, scrapianContext.splitCommaPersonNames(nameSource).length);
    }

    public void testFindEntity() {
        ScrapianContext context = new ScrapianContext();
        context.setScrapeSession(new ScrapeSession(null));
        ScrapeEntity e = context.getSession().newEntity();
        e.setType("O");
        e.setName("FIRST STATE BANK");
        ScrapeAddress a = e.newAddress();
        a.setRawFormat("ALSIP,ILLINOIS");

        Map params = new HashMap();
        params.put("type", "O");
        params.put("name", "FIRST STATE BANK");
        ScrapeAddress compareAddress = new ScrapeAddress();
        compareAddress.setRawFormat("ALSIP,ILLINOIS");
        params.put("address", compareAddress);
        ScrapeEntity find = context.findEntity(params);
    }

    public void testJoin() {
        ScrapianContext context = new ScrapianContext();
        assertEquals("a b c", context.joinStrings(Arrays.asList("a", "b", "c")));
        assertEquals("a c", context.joinStrings(Arrays.asList("a", "", "c")));
        assertEquals("a c", context.joinStrings(Arrays.asList("a", null, "c")));
        assertEquals("a c d", context.joinStrings(Arrays.asList("a", null, "c", "d")));
        assertEquals("a d", context.joinStrings(Arrays.asList(" a", null, "  ", "d ")));
        assertEquals("a ; b ; c ; d", context.joinStrings(Arrays.asList(null, " a ", null, " b ", null, " c ", null, " d ", null), " ; "));
        assertEquals("a;b;c;d", context.joinStrings(Arrays.asList(null, " a ", null, " b ", null, " c ", null, " d ", null), ";"));
        assertEquals("a ;b ;c ;d", context.joinStrings(Arrays.asList(null, " a ", null, " b ", null, " c ", null, " d ", null), " ;"));
        assertEquals("a b c d", context.joinStrings(Arrays.asList(null, " a ", null, " b ", null, " c ", null, " d ", null)));
        assertEquals("52 Main St, over the corner, Wilmington", context.joinStrings(Arrays.asList(" 52 Main St ", "over the corner", "Wilmington "), ", "));
        assertEquals("a", context.joinStrings(Arrays.asList("a", null), ";"));

    }

    public void testParseAddress() throws IOException {
        ScrapianContext scrapianContext = new ScrapianContext();
        AddressParser addressParser =new AddressParser();
        scrapianContext.setAddressParser(addressParser);
        BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/test_data/parse_address.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().equals("")) {
                String[] parts = line.split("\\|");
                List<ScrapeAddress> addressses = scrapianContext.parseAddresses(new StringSource(parts[0]));
                ScrapeAddress address = addressses.get(Integer.parseInt(parts[1]));
                assertAddrressPart(parts[2], address.getRawFormat());
                assertAddrressPart(parts[3], address.getAddress1());
                assertAddrressPart(parts[4], address.getCity());
                assertAddrressPart(parts[5], address.getProvince());
                assertAddrressPart(parts[6], address.getCountry());
                assertAddrressPart(parts[7], address.getPostalCode());

            }
        }
    }

    private void assertAddrressPart(String s1, String s2) {
        if ("null".equals(s1)) {
            s1 = null;
        }
        if ("null".equals(s2)) {
            s2 = null;
        }
        assertEquals(s1, s2);
    }

}
