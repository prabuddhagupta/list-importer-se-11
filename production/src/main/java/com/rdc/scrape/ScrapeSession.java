package com.rdc.scrape;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import com.rdc.importer.misc.InvalidXmlCharFilterWriter;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.rdc.importer.scraper.Deserializer;
import com.rdc.importer.scrapian.model.ThirdPartyListSource;
import com.rdc.importer.scrapian.util.EhCacheUtil;
import com.rdc.importer.misc.ValidationException;

public class ScrapeSession implements Serializable {
    private List<ScrapeEntity> entities = new ArrayList<ScrapeEntity>();
    protected Cache entitiesCache;
    private HashMap<String, List<ScrapeEntity>> entitiesMap = new HashMap<String, List<ScrapeEntity>>();
    private List<ScrapeEntity> newEntitiesList = new LinkedList<ScrapeEntity>();
    private boolean escape;
    private boolean escapeSpecial;
    private String encoding;
    private boolean ignoreScrapeSession = false;
    private boolean isThirdPartySupplier = false;
    private boolean entitiesRemoved = false;
    private ThirdPartyListSource thirdPartyListSource;
    private String remoteSiteCookies;
    private String remoteUrl;
    private int counter = 0;
    private String listName;
    
    private Deserializer entityDeserializer = null;
    
    public ScrapeSession(String listName) {
        this.listName = listName != null ? listName.toLowerCase() : null;
        newCache();
    }
    
    public boolean isIgnoreScrapeSession() {
        return ignoreScrapeSession;
    }

    public void setIgnoreScrapeSession(boolean ignoreScrapeSession) {
        this.ignoreScrapeSession = ignoreScrapeSession;
    }

    public boolean isThirdPartySupplier() {
        return isThirdPartySupplier;
    }

    public void setIsThirdPartySupplier(boolean isThirdPartySupplier) {
        this.isThirdPartySupplier = isThirdPartySupplier;
    }

    public ThirdPartyListSource getThirdPartyListSource() {
		return thirdPartyListSource;
	}

	public void setThirdPartyListSource(ThirdPartyListSource thirdPartyListSource) {
		this.thirdPartyListSource = thirdPartyListSource;
	}

	public Deserializer getEntityDeserializer() {
        return entityDeserializer;
    }

    public void setEntityDeserializer(Deserializer entityDeserializer) {
        this.entityDeserializer = entityDeserializer;
    }

    public synchronized ScrapeEntity newEntity() {
        ScrapeEntity entity = new ScrapeEntity();
        synchronized(entities){
            if(entities.size() == 0){
                getEntitiesNonCache(); //copies from cache back to array
            }
            entities.add(entity);
            newEntitiesList.add(entity);
        }
        return entity;
    }

	public synchronized void removeEntity(ScrapeEntity scrapeEntity) throws Exception {
		synchronized (entities) {
	        if(entities.size() == 0){
	            getEntitiesNonCache(); //copies from cache back to array
	        }
        	List<ScrapeEntity> listy = getEntitiesMap().get(scrapeEntity.getScrubbedName().toUpperCase());

			entities.remove(scrapeEntity);
        	if(listy.size() == 1){
        		int size = entitiesMap.size();
        		entitiesMap.remove(scrapeEntity.getScrubbedName().toUpperCase());
        		if(size - entitiesMap.size() != 1){
        			throw new Exception("Did not remove properly");
        		}
        	} else if (listy.size() > 1){
        		int size = listy.size();
        		listy.remove(scrapeEntity);
        		if(size - listy.size() != 1){
        			throw new Exception("Did not remove properly");
        		}
        	} else {
        		throw new Exception("Could not find entity");
        	}
		}
	}

	public synchronized void addEntity(ScrapeEntity scrapeEntity) throws Exception {
		if(StringUtils.isBlank(scrapeEntity.getName())){
			throw new Exception("Entity name cannot be blank");
		}
		synchronized (entities) {
		    if(entities.size() == 0){
		        getEntitiesNonCache(); //copies from cache back to array
		    }
			entities.add(scrapeEntity);
			List<ScrapeEntity> list = entitiesMap.get(scrapeEntity.getScrubbedName().toUpperCase());
			if (list == null) {
				list = new ArrayList<ScrapeEntity>();
				entitiesMap.put(scrapeEntity.getScrubbedName().toUpperCase(), list);
			}
			list.add(scrapeEntity);
		}
	}
    
    public synchronized void changeEntityName(ScrapeEntity scrapeEntity, String newName) throws Exception {
    	synchronized(entities){
            if(entities.size() == 0){
                getEntitiesNonCache(); //copies from cache back to array
            }
        	List<ScrapeEntity> listy = getEntitiesMap().get(scrapeEntity.getScrubbedName().toUpperCase());
        	
        	if(listy.size() == 1){
        		int size = entitiesMap.size();
        		entitiesMap.remove(scrapeEntity.getScrubbedName().toUpperCase());
        		if(size - entitiesMap.size() != 1){
        			throw new Exception("Did not remove properly");
        		}
        		scrapeEntity.setName(newName);
    			List<ScrapeEntity> list = entitiesMap.get(scrapeEntity.getScrubbedName().toUpperCase());
    			if (list == null) {
    				list = new ArrayList<ScrapeEntity>();
    				entitiesMap.put(scrapeEntity.getScrubbedName().toUpperCase(), list);
    			}
    			list.add(scrapeEntity);
        	} else if (listy.size() > 1){
        		int size = listy.size();
        		listy.remove(scrapeEntity);
        		if(size - listy.size() != 1){
        			throw new Exception("Did not remove properly");
        		}
        		scrapeEntity.setName(newName);
    			List<ScrapeEntity> list = entitiesMap.get(scrapeEntity.getScrubbedName().toUpperCase());
    			if (list == null) {
    				list = new ArrayList<ScrapeEntity>();
    				entitiesMap.put(scrapeEntity.getScrubbedName().toUpperCase(), list);
    			}
    			list.add(scrapeEntity);
        	} else {
        		throw new Exception("Could not find entity");
        	}
        	
    	}
    }

    public List<ScrapeEntity> getEntitiesByName(String name) {
        List<ScrapeEntity> foundEntities = new ArrayList<ScrapeEntity>();
        synchronized (entities) {
            for (ScrapeEntity scrapeEntity : getEntitiesNonCache()) {
                if (name.equals(scrapeEntity.getName())) {
                    foundEntities.add(scrapeEntity);
                }
            }
        }
        return foundEntities;
    }
    
    public void prepCache() {
        synchronized (entities) {
            if (entities.size() != 0) {
                entitiesCache.removeAll();
                for (ScrapeEntity entity : entities) {
                    entitiesCache.put(new Element(counter++, entity));
                }
                entitiesCache.flush();
            }
        }
    }

    public Cache getEntities() {
         synchronized (entities) {
            if (entities.size() != 0) {
                prepCache();
                entities.clear();
            }
            return entitiesCache;
        }
    }

    public void updateEntityInCache(Object key, ScrapeEntity entity) throws Exception {
        if (key != null && entity != null) {
            entitiesCache.put(new Element(key, entity));
        }
    }

    public List<ScrapeEntity> getEntitiesNonCache() {
        synchronized (entities) {
            if (entities.size() == 0 && entitiesCache != null && entitiesCache.getSize() > 0) {
                for (Object key : entitiesCache.getKeys()) {
                    entities.add((ScrapeEntity) entitiesCache.get(key).getValue());
                }
            }
            return entities;
        }
    }
    
    public void setEntities(Cache ents) {
        synchronized (entities) {
            entities.clear();
            if(entitiesCache != null){
                entitiesCache.removeAll();
            }
            this.entitiesCache = ents;
        }
    }
    
	public HashMap<String, List<ScrapeEntity>> getEntitiesMap() {
		synchronized (entities) {
			if (newEntitiesList.size() > 0) {
				Iterator<ScrapeEntity> iterator = newEntitiesList.iterator();
				while (iterator.hasNext()) {
					ScrapeEntity entity = iterator.next();
					if (entity.getScrubbedName() != null) {
						List<ScrapeEntity> list = entitiesMap.get(entity.getScrubbedName().toUpperCase());
						if (list == null) {
							list = new ArrayList<ScrapeEntity>();
							entitiesMap.put(entity.getScrubbedName().toUpperCase(), list);
						}
						list.add(entity);
						iterator.remove();
					}
				}
			}
			return entitiesMap;
		}
	}
	
	public void newCache(){
	    if(listName != null){
	        entitiesCache = EhCacheUtil.addCache("scrape_entity_" + listName);
	    }
	}

    public boolean isEscape() {
        return escape;
    }

    public void setEscape(boolean escape) {
        this.escape = escape;
    }

    public boolean isEscapeSpecial() {
		return escapeSpecial;
	}

	public void setEscapeSpecial(boolean escapeSpecial) {
		this.escapeSpecial = escapeSpecial;
	}

	public String getRemoteSiteCookies() {
        return remoteSiteCookies;
    }

    public void setRemoteSiteCookies(String remoteSiteCookies) {
        this.remoteSiteCookies = remoteSiteCookies;
    }

    public String toStringSpecial(boolean prettyPrint) throws Exception {
        StringWriter writer = new StringWriter();
        InvalidXmlCharFilterWriter filter = new InvalidXmlCharFilterWriter(writer);
        PrintWriter printWriter = new PrintWriter(filter);
        printWriter.println("<entities>");
        prepCache();
        for (Object key : entitiesCache.getKeys()) {
            ScrapeEntity entity = (ScrapeEntity) (entitiesCache.get(key).getValue());
            printWriter.println(entity.toXml(escape, escapeSpecial, prettyPrint));
        }
        printWriter.println("</entities>");
        printWriter.flush();
        printWriter.close();
        return writer.toString();
    }

    public void dump(OutputStream output) throws Exception {
        dump(output, true, false);    
    }
    
    public void dump(OutputStream output, boolean prettyPrint, boolean doCharCheck) throws Exception {
        String scrapeXml = null;
        String scrapeXmlAfter = "";
        StringBuilder sb = new StringBuilder();
        TreeSet<String> set = new TreeSet<String>();
        boolean inDetail = false;
        if (encoding == null) {
            encoding = System.getProperty("file.encoding", "windows-1252");
        }
        if (doCharCheck) {
            CircularFifoBuffer buf = new CircularFifoBuffer(16);
            scrapeXml = "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n" + StringEscapeUtils.unescapeXml(toStringSpecial(prettyPrint)); //unescaping here partly leads to the very approximate positions quoted later
            
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                IOUtils.copy(new StringReader(scrapeXml), os, encoding);
                scrapeXmlAfter = new String(os.toByteArray(), "windows-1252");
                
                for (int j = 0; j < scrapeXml.length(); j++) {
                    buf.add(scrapeXml.charAt(j));
                    if(circBufToString(buf).startsWith("<detailedAlias>")){
                        inDetail = true;
                    }
                    if(circBufToString(buf).equals("</detailedAlias>")){
                        inDetail = false;
                    }
                    if (!inDetail && scrapeXml.charAt(j) != scrapeXmlAfter.charAt(j) && scrapeXmlAfter.charAt(j) == '?') {
                        sb.append((sb.length() > 0 ? "\n" : "") + listName + " contains invalid characters [" + scrapeXml.charAt(j) + "] [\\u" + Integer.toHexString((int) scrapeXml.charAt(j) | 0x10000).substring(1) + "]. at very approx. position " + j);
                        set.add("\\u" + Integer.toHexString((int) scrapeXml.charAt(j) | 0x10000).substring(1));
                    }
                }
                scrapeXmlAfter = null;
            }  catch (Exception e) {
                e.printStackTrace();
            }
            scrapeXml =  "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n" + toStringSpecial(prettyPrint);
        } else {
            scrapeXml =  "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n" + toStringSpecial(prettyPrint);
        }
        IOUtils.copy(new StringReader(scrapeXml), output, encoding);
        output.flush();
        
        if (sb.length() > 0) {
            throw new ValidationException(sb.toString() + "\n Set = " + set.toString());
        }
    }

    public String circBufToString(CircularFifoBuffer in){
        StringBuilder sb = new StringBuilder();
        Object[] ar = in.toArray();
        for(Object ob : ar){
            sb.append((Character)(ob));
        }
        return sb.toString();
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    @Override
    public String toString() {
        return "ScrapeSession [entities=" + entities + ", entitiesMap=" + entitiesMap + ", newEntitiesList=" + newEntitiesList + ", escape=" + escape
                + ", escapeSpecial=" + escapeSpecial + ", encoding=" + encoding + ", ignoreScrapeSession=" + ignoreScrapeSession
                + ", isThirdPartySupplier=" + isThirdPartySupplier + ", entitiesRemoved=" + entitiesRemoved + ", thirdPartyListSource="
                + thirdPartyListSource + ", remoteSiteCookies=" + remoteSiteCookies + ", remoteUrl=" + remoteUrl
                + ", entityDeserializer=" + entityDeserializer + "]";
    }
    
    
    
}
