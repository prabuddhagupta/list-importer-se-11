package com.rdc.importer.scrapian;

import com.rdc.importer.scrapian.resolver.ClasspathScrapianResolver;
import com.rdc.scrape.ScrapeAddress;
import com.rdc.scrape.ScrapeEntity;
import com.rdc.scrape.ScrapeEvent;
import com.rdc.scrape.ScrapeSession;

import junit.framework.TestCase;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class GroovyScriptsUnitTest extends TestCase {

    private ScrapianEngine createScrapeEngine() {
        System.setProperty("scrapian.resolver", "classpath");
        ScrapianEngine scrapianEngine = new ScrapianEngine();
        scrapianEngine.setScrapianResolver(new ClasspathScrapianResolver() {
            {
                setScriptBase("/test_scripts/");
            }
        });
        scrapianEngine.setBeanFactory(new BeanFactory(){

            public Object getBean(String s) throws BeansException {
                return new ScrapianContext();
            }

            public Object getBean(String s, Class aClass) throws BeansException {
                return null;  
            }

            public Object getBean(Class aClass) throws BeansException {
                return null;  
            }

            public Object getBean(String s, Object[] objects) throws BeansException {
                return null;  
            }

            public boolean containsBean(String s) {
                return false;  
            }

            public boolean isSingleton(String s) throws NoSuchBeanDefinitionException {
                return false;  
            }

            public boolean isPrototype(String s) throws NoSuchBeanDefinitionException {
                return false;  
            }

            public boolean isTypeMatch(String s, Class aClass) throws NoSuchBeanDefinitionException {
                return false;  
            }

            public Class getType(String s) throws NoSuchBeanDefinitionException {
                return null;  
            }

            public String[] getAliases(String s) {
                return new String[0];  
            }
        });

        return scrapianEngine;
    }

    public void testUsMarshalsHtml() throws Exception {
        ScrapianEngine scrapianEngine = createScrapeEngine();
        ScrapeSession session = new ScrapeSession(null);
        scrapianEngine.scrape("us_marshals_html_test", session, null, true);

        assertNotNull(session.getEntitiesNonCache());
        assertEquals(1, session.getEntitiesNonCache().size());
        ScrapeEntity entity = session.getEntitiesNonCache().iterator().next();
        assertEquals("Test Name", entity.getName());
        assertEquals("Male", entity.getSexes().iterator().next());
        assertEquals("White", entity.getRaces().iterator().next());
        assertEquals("5'07\"", entity.getHeights().iterator().next());
        assertEquals("160 LBS", entity.getWeights().iterator().next());
        assertEquals("Blonde", entity.getHairColors().iterator().next());
        assertEquals("TAT Right Forearm \"DW\" on Wrist", entity.getScarsMarks().iterator().next());
        assertEquals("Charge: Amphetamine - Sell; Date of Warrant: December 04, 1995", entity.getEvents().iterator().next().getDescription());
        assertEquals("MO - Missouri", entity.getAddresses().iterator().next().getRawFormat());
    }


    public void testNyMostWantedHtml() throws Exception {
        ScrapianEngine scrapianEngine = createScrapeEngine();
        ScrapeSession session = new ScrapeSession(null);
        scrapianEngine.scrape("newyork_most_wanted_html_test", session, null, true);

        assertNotNull(session.getEntitiesNonCache());
        assertEquals(1, session.getEntitiesNonCache().size());
        ScrapeEntity entity = session.getEntitiesNonCache().iterator().next();

        assertEquals("Druce Young", entity.getName());
        assertEquals("M", entity.getSexes().iterator().next());
        assertEquals("Black", entity.getRaces().iterator().next());
        assertEquals("5' 4\"", entity.getHeights().iterator().next());
        assertEquals("185 lbs.", entity.getWeights().iterator().next());
        assertEquals("Gray", entity.getHairColors().iterator().next());
        assertEquals("Gold tooth on top left of mouth", entity.getScarsMarks().iterator().next());
        assertEquals("Wanted For:Parole Violation;Crime of Conviction:Assault 2 and Rape 3;Remarks:Current offense involved forcible sexual intercourse and assault of an adult female victim.",
                entity.getEvents().iterator().next().getDescription());
    }

    public void testIcrimeWatchOffendersHtml() throws Exception {
        ScrapianEngine scrapianEngine = createScrapeEngine();
        ScrapeSession session = new ScrapeSession(null);
        scrapianEngine.scrape("icrimewatch_sex_offenders_html_test", session, null, true);

        assertNotNull(session.getEntitiesNonCache());
        assertEquals(1, session.getEntitiesNonCache().size());
        ScrapeEntity entity = session.getEntitiesNonCache().iterator().next();
        assertEquals("SAMUEL ZOCCOLI", entity.getName());
        assertEquals("M", entity.getSexes().iterator().next());
        assertEquals("Caucasian", entity.getRaces().iterator().next());
        assertEquals("6'02''", entity.getHeights().iterator().next());
        assertEquals("170lbs", entity.getWeights().iterator().next());
        assertEquals("Brown", entity.getHairColors().iterator().next());
        assertEquals("Sex Crime: 53a-72a(a)(2) - Sexual assault 3rd kindred",
                entity.getEvents().iterator().next().getDescription());

        // TODO: Why is scars and marks missing?
//        assertEquals("Gold tooth on top left of mouth", entity.getScarsMarks().iterator().next());
    }

//    public void testUsNySexOffendersHtml() throws Exception {
//        ScrapianEngine engine = createScrapeEngine();
//        ScrapeSession session = new ScrapeSession(null);
//        engine.scrape("us_ny_sex_offender_html_test", session);
//        assertNotNull(session.getEntitiesNonCache());
//        assertEquals(10, session.getEntitiesNonCache().size());
//    }

    public void testDeaDoctorsTest() throws Exception {
        ScrapianEngine scrapianEngine = createScrapeEngine();
        ScrapeSession session = new ScrapeSession(null);
        scrapianEngine.scrape("dea_doctors_test", session, null, true);

        assertEquals(45, session.getEntitiesNonCache().size());

        ScrapeEntity entity = session.getEntitiesNonCache().iterator().next();
        assertEquals("Julian A. ABBEY", entity.getName());
        assertNotNull(entity.getAddresses());
        ScrapeAddress address = entity.getAddresses().iterator().next();
        assertEquals("Saugus", address.getCity());
        assertEquals("MA", address.getProvince());
        assertEquals("United States", address.getCountry());
        assertNotNull(entity.getEvents());
        ScrapeEvent scrapeEvent = entity.getEvents().iterator().next();
        assertNotNull(entity.getRemarks());
        assertEquals("Pled Guilty; Possession with intent to distribute", scrapeEvent.getDescription());
        assertEquals("10/28/2005", scrapeEvent.getDate());
        assertNotNull(entity.getPositions());
        String position = entity.getPositions().iterator().next().getDescription();
        assertEquals("MD", position);
        String remark = entity.getRemarks().iterator().next().toString();
        assertEquals("Julian A. Abbey, MD, age 47, of Saugus, MA, pled guilty on April 30, 2008, to possession with intent to distribute controlled substances. Abbey was sentenced to five years probation.", remark);
    }

    public void testPdfScrapeTest() throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) {
            ScrapianEngine scrapianEngine = createScrapeEngine();
            ScrapeSession session = new ScrapeSession(null);
            scrapianEngine.scrape("pdf_scrape_test", session, null, true);
            assertNotNull(session.getEntitiesNonCache());
            assertEquals(1, session.getEntitiesNonCache().size());
            ScrapeEntity entity = session.getEntitiesNonCache().iterator().next();
            assertEquals("JORGE MANUEL CARRANZA", entity.getName());
            assertEquals("MALE", entity.getSexes().iterator().next());
            assertEquals("WHITE", entity.getRaces().iterator().next());
            assertEquals("5'05\"", entity.getHeights().iterator().next());
            assertEquals("160 pounds", entity.getWeights().iterator().next());
            assertEquals("BLACK", entity.getHairColors().iterator().next());
            assertEquals("BROWN", entity.getEyeColors().iterator().next());
            assertEquals("PERU", entity.getAddresses().iterator().next().getRawFormat());
            String[] aliases = entity.getAliases().toArray(new String[entity.getAliases().size()]);
          //  assertEquals("JORGE MANUEL CARRANZA", aliases[0]); //was equal to main name, which we now disallow
            assertEquals("JORGE MANUEL CARRANZA VIVANCE", aliases[0]);
        }
    }

    public void testVaSccDelinqPdf() throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) {
            ScrapianEngine scrapianEngine = createScrapeEngine();
            ScrapeSession session = new ScrapeSession(null);
            scrapianEngine.scrape("va_scc_delinq_test", session, null, true);
            assertNotNull(session.getEntitiesNonCache());
            assertEquals(276, session.getEntitiesNonCache().size());
            ScrapeEntity entity = session.getEntitiesNonCache().iterator().next();
            assertEquals("Thomas W. Huddleston", entity.getName());
            assertEquals("06/18/2010", entity.getEvents().iterator().next().getDate());
        }
    }

    public void testUsExcludedParties() throws Exception {
        ScrapianEngine engine = createScrapeEngine();
        ScrapeSession session = new ScrapeSession(null);
        engine.scrape("us_excluded_parties_html_test", session, null, true);
        assertNotNull(session.getEntitiesNonCache());

        assertTrue(session.getEntitiesNonCache().size() > 0);
        for(ScrapeEntity entity : session.getEntitiesNonCache()) {
            assertNotNull(entity.getName());
        }
    }
    
    public void testDowJonesAntiCorruptionScrape() throws Exception {
    	/*
    	ScrapianEngine scrapianEngine = createScrapeEngine();
    	ScrapeSession session = new ScrapeSession(null);
    	scrapianEngine.scrape("dow_jones_anticorruption_test", session);
    	*/
    	
    }
    
}
