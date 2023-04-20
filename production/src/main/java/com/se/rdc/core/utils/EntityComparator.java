package com.se.rdc.core.utils;

import com.rdc.scrape.ScrapeEntity;

import java.util.Comparator;

public class EntityComparator implements Comparator<ScrapeEntity> {

  @Override
  public int compare(ScrapeEntity entity1, ScrapeEntity entity2) {
    //ascending order
    return entity1.getName().compareTo(entity2.getName());
    
    //Descending order
    //return entity2.getName().compareTo(entity1.getName());
  }

}
