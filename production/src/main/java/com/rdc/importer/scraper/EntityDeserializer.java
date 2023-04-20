package com.rdc.importer.scraper;

import java.util.UUID;

import com.rdc.importer.scrapian.util.EhCacheUtil;
import com.rdc.scrape.ScrapeSource;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.springframework.stereotype.Component;

import com.rdc.scrape.ScrapeAddress;
import com.rdc.scrape.ScrapeAlias;
import com.rdc.scrape.ScrapeDeceased;
import com.rdc.scrape.ScrapeDob;
import com.rdc.scrape.ScrapeEntity;
import com.rdc.scrape.ScrapeEntityAssociation;
import com.rdc.scrape.ScrapeEvent;
import com.rdc.scrape.ScrapeIdentification;
import com.rdc.scrape.ScrapePepType;
import com.rdc.scrape.ScrapePosition;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.SetNestedPropertiesRule;

@Component
public class EntityDeserializer extends Deserializer {

    protected void configureDigester(Digester digester) {
        digester.addObjectCreate("entities", EntityListCache.class);
        digester.addObjectCreate("entities/entity", ScrapeEntity.class);
        digester.addCallMethod("entities/entity/id", "setId", 0);
        digester.addCallMethod("entities/entity/name", "setName", 0);
        digester.addCallMethod("entities/entity/type", "setType", 0);
        digester.addCallMethod("entities/entity/language", "addLanguage", 0);
        digester.addCallMethod("entities/entity/data_source_id", "setDataSourceId", 0);
        digester.addCallMethod("entities/entity/url", "addUrl", 0);
        digester.addCallMethod("entities/entity/image_url", "addImageUrl", 0);
        digester.addCallMethod("entities/entity/alias", "addAlias", 0);
        digester.addCallMethod("entities/entity/nationality", "addNationality", 0);
        digester.addCallMethod("entities/entity/citizenship", "addCitizenship", 0);
        digester.addCallMethod("entities/entity/occupation", "addOccupation", 0);
        digester.addCallMethod("entities/entity/height", "addHeight", 0);
        digester.addCallMethod("entities/entity/weight", "addWeight", 0);
        digester.addCallMethod("entities/entity/eye_color", "addEyeColor", 0);
        digester.addCallMethod("entities/entity/complexion", "addComplexion", 0);
        digester.addCallMethod("entities/entity/hair_color", "addHairColor", 0);
        digester.addCallMethod("entities/entity/scars_marks", "addScarsMarks", 0);

        digester.addObjectCreate("entities/entity/dob", ScrapeDob.class);
        SetNestedPropertiesRule rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/dob", rule);
        digester.addSetNext("entities/entity/dob", "addDateOfBirth");
        
        digester.addObjectCreate("entities/entity/position", ScrapePosition.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/position", rule);
        digester.addSetNext("entities/entity/position", "addPosition");
          
        digester.addObjectCreate("entities/entity/deceased", ScrapeDeceased.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/deceased", rule);
        digester.addSetNext("scrapeEntities/entity/deceased", "addDeceased");

        digester.addCallMethod("entities/entity/sex", "addSex", 0);
        digester.addCallMethod("entities/entity/build", "addBuild", 0);
        digester.addCallMethod("entities/entity/race", "addRace", 0);
        digester.addCallMethod("entities/entity/remark", "addRemark", 0);
        digester.addCallMethod("entities/entity/association", "addAssociation", 0);

        digester.addObjectCreate("entities/entity/address", ScrapeAddress.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/address", rule);
        digester.addBeanPropertySetter("entities/entity/address/raw_format", "rawFormat");
        digester.addBeanPropertySetter("entities/entity/address/postal_code", "postalCode");
        digester.addBeanPropertySetter("entities/entity/address/birth_place", "birthPlace");
        digester.addSetNext("entities/entity/address", "addAddress");
        
        digester.addObjectCreate("entities/entity/detailedAlias", ScrapeAlias.class);
        digester.addBeanPropertySetter("entities/entity/detailedAlias/name", "name");       
        digester.addBeanPropertySetter("entities/entity/detailedAlias/type", "typeByCode");
        digester.addBeanPropertySetter("entities/entity/detailedAlias/language", "language");
        digester.addBeanPropertySetter("entities/entity/detailedAlias/script", "script");
        digester.addBeanPropertySetter("entities/entity/detailedAlias/quality", "quality");
        digester.addSetNext("entities/entity/detailedAlias", "addDetailedAlias");

        digester.addObjectCreate("entities/entity/event", ScrapeEvent.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/event", rule);
        digester.addBeanPropertySetter("entities/entity/event/end_date", "endDate");
        digester.addSetNext("entities/entity/event", "addEvent");

        digester.addObjectCreate("entities/entity/identification", ScrapeIdentification.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/identification", rule);
        digester.addSetNext("entities/entity/identification", "addIdentification");
        
        digester.addObjectCreate("entities/entity/pepType", ScrapePepType.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/pepType", rule);
        digester.addSetNext("entities/entity/pepType", "addPepType");
        
        digester.addObjectCreate("entities/entity/relationship", ScrapeEntityAssociation.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/relationship", rule);
        digester.addSetNext("entities/entity/relationship", "addScrapeEntityAssociation");

        digester.addObjectCreate("entities/entity/source", ScrapeSource.class);
        rule = new SetNestedPropertiesRule();
        rule.setAllowUnknownChildElements(true);
        digester.addRule("entities/entity/source", rule);
        digester.addBeanPropertySetter("entities/entity/source/name", "name");
        digester.addBeanPropertySetter("entities/entity/source/url", "url");
        digester.addBeanPropertySetter("entities/entity/source/description", "description");
        digester.addSetNext("entities/entity/source", "addSource");

        digester.addSetNext("entities/entity", "addEntity");
    }
   
    public static class EntityListCache {
        public Cache cache;
        private int counter = 0;

        public void addEntity(ScrapeEntity e) {
            if (cache == null) {
                String cacheName =  UUID.randomUUID().toString();
                cache = EhCacheUtil.addCache("deserialized_" + cacheName);
            }
            cache.put(new Element(counter++, e));
        }

        public Cache getEntitiesCache() {
            if (cache == null) {
                String cacheName = UUID.randomUUID().toString();
                cache = EhCacheUtil.addCache("deserialized_" + cacheName);
            }
            return cache;
        }
    }

    @Override
    protected Cache getEntityCache(Object digestedResults) {
        EntityListCache entityList = (EntityListCache) digestedResults;
        return entityList.getEntitiesCache();
    }
}