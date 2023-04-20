package com.rdc.scrape;

import com.rdc.core.nameparser.PersonName;
import org.junit.Assert;
import org.junit.Test;


public class ScrapeEntityTest {
    @Test public void testSetPersonName() {
        final String name = "John J. Jingleheimer-Schmidt";
        final String[] sname = name.split(" ");
        final PersonName pname = new PersonName(sname[0], sname[1], sname[2]);
        final ScrapeEntity entity = new ScrapeEntity();
        entity.setPersonName(pname);
        Assert.assertEquals(name, entity.getName());
    }
}