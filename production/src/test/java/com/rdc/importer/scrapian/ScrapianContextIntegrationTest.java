package com.rdc.importer.scrapian;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.*;

import com.rdc.scrape.CachedEntities;

public class ScrapianContextIntegrationTest {
    
    @Test
    public void deserializeCachedEntitiesTest()
    {
        try
        {
        Configuration configuration=new BaseConfiguration();
        ScrapianContext scrapianContext=new ScrapianContext();
        scrapianContext.setConfiguration(configuration);
        CachedEntities cachedEntities=scrapianContext.deserializeCachedEntities("us_hi_sex_offenders");
        FileWriter fileWriter=new FileWriter("/tmp/cachedEntities.txt");
        fileWriter.write(cachedEntities.toString());
        fileWriter.close();
        }catch(Exception ex){
            ex.printStackTrace();
            fail("deserializeCachedEntities failed because "+ex);
        }
        
    }

}
