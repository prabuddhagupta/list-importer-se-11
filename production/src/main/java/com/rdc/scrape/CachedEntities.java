package com.rdc.scrape;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CachedEntities implements Serializable {
    
    private static final long serialVersionUID = 1L;
    List<ScrapeEntity> entityList=new ArrayList<ScrapeEntity>();
    List<String>historyList=new ArrayList<String>();
    public List<ScrapeEntity> getEntityList() {
        return entityList;
    }
    public void setEntityList(List<ScrapeEntity> entityList) {
        this.entityList = entityList;
    }
    public List<String> getHistoryList() {
        return historyList;
    }
    public void setHistoryList(List<String> historyList) {
        this.historyList = historyList;
    }
    @Override
    public String toString() {
        return "CachedEntities [entityList=" + entityList + ", historyList=" + historyList + "]";
    }
    
    
    
}
