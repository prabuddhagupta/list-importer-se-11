package com.rdc.importer.scrapian;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.scrape.ScrapeAddress;
import com.rdc.scrape.ScrapeSession;

import junit.framework.TestCase;

public class ScrapianContextFuncTest extends TestCase {
    /*
        select i.itemname, 
        CASE WHEN p.PersonName IS NOT NULL THEN p.PersonName ELSE o.OrgName END AS entityName,
        ea.address1
        from item i(nolock), monitoredlist m(nolock), entity e(nolock), entityaddress ea(nolock), person p(nolock), organization o(nolock)
        where i.itemformatid=4
        and i.itemid = e.itemid
        and i.itemfoldername = m.foldername
        and m.SystemManaged = 1
        and e.entityid = ea.entityid
        and ea.address1 is not null
        and ea.address2 is null
        and ea.city is null
        and ea.province is null
        and ea.country is null
        and ea.postalcode is null
        and e.personid *= p.personid
        and e.organizationid *= o.organizationid
        and e.entityactive = 1
     */

    public void testParseAddress() throws IOException {
        ScrapianContext scrapianContext = (ScrapianContext) new ClassPathXmlApplicationContext("list_importer-test-context.xml").getBean("scrapianContext");
        scrapianContext.setScrapeSession(new ScrapeSession(null));
        BufferedReader br = new BufferedReader(new FileReader("c:\\temp\\address\\addresses.txt"));
        PrintWriter pw = new PrintWriter("c:\\temp\\address\\addresses_result.txt");
        pw.println("ItemName\tEntityName\tOriginal\tIndex\tRawFormat\tAddress1\tCity\tProvince\tCountry\tPostalCode");
        String line;
        while ((line = br.readLine()) != null) {
            List<String> lineParts = new ArrayList<String>();
            for (String part : line.split("\t")) {
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    lineParts.add(part.substring(1, part.length() - 1));
                }
            }

            List<ScrapeAddress> addresses = scrapianContext.parseAddresses(new StringSource(lineParts.get(2)));
            for (int i = 0; i < addresses.size(); i++) {
                ScrapeAddress address = addresses.get(i);
                pw.println(lineParts.get(0) + "\t" + lineParts.get(1) + "\t" + lineParts.get(2) + "\t" + i + "\t" + address.getRawFormat() + "\t" + address.getAddress1() + "\t" + address.getCity() + "\t" + address.getProvince() + "\t" + address.getCountry() + "\t" + address.getPostalCode());
            }
        }
    }
}
