package com.rdc.importer.misc;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

//import sun.misc.BASE64Encoder;
import java.util.Base64;

import com.rdc.importer.misc.AliasQuality;
import com.rdc.importer.misc.AliasType;

public class AliasMonitoredListRecordData extends MonitoredListRecordData implements Serializable {
  public static final String ID = "ID";
  
  //public static final String HASH = "HASH";
  
  public static final String ALIAS = "ALIAS";
  
  public static final String SCRIPT = "SCRIPT";
  
  public static final String LANGUAGE = "LANGUAGE";
  
  public static final String TYPE = "TYPE";

  public static final String NAME = "NAME";
  
  public static final String QUALITY = "QUALITY";
  
  //private String name;
  
  private int id;
  
  //private String script;
  
  //private AliasType type;
  
  //private String language;

  public String hash() {
    try {
      MessageDigest algorithm = MessageDigest.getInstance("SHA-256");
      algorithm.reset();
      byte[] tmp =algorithm.digest(this.toString().getBytes());

        String encodedString = Base64.getEncoder().encodeToString(tmp);
        return encodedString;
    } 
    catch (Exception e) {
      return null;
    }
  }
  
  public String getName() {
    return this.getAttribute(AliasMonitoredListRecordData.NAME);
    //return name;
  }

  public void setName(String name) {
    this.setAttribute(AliasMonitoredListRecordData.NAME, name);
    //this.name = name;
  }

  public String getScript() {
    return this.getAttribute(AliasMonitoredListRecordData.SCRIPT);
    //return script;
  }

  public void setScript(String script) {
    this.setAttribute(AliasMonitoredListRecordData.SCRIPT, script);
    //this.script = script;
  }

  public AliasType getType() {    
    try {
      return AliasType.valueOf(this.getAttribute(AliasMonitoredListRecordData.TYPE));
    }
    catch (Exception e) {
      return AliasType.AKA;
    }
  }

  public void setType(AliasType type) {
    this.setAttribute(AliasMonitoredListRecordData.TYPE, type.getCode());
    //this.type = type;
  }
  
  public void setTypeByCode(String type) {
    this.setAttribute(AliasMonitoredListRecordData.TYPE, type);
    //this.type = AliasType.valueOf(type);
  } 

  public String getQuality() {    
	  return this.getAttribute(AliasMonitoredListRecordData.QUALITY);
  }

  public void setQuality(String quality) {
    this.setAttribute(AliasMonitoredListRecordData.QUALITY, quality);
  }

//  public void setQuality(AliasQuality quality) {
//    this.setAttribute(AliasMonitoredListRecordData.QUALITY, quality.getCode());
//  }
  
//  public void setQualityByCode(String quality) {
//    this.setAttribute(AliasMonitoredListRecordData.QUALITY, quality);
//  }
  
  public boolean equals(Object obj) {
    if (obj == null) return false;
    try {
      if (((AliasMonitoredListRecordData)obj).hash().equals(this.hash())) return true;
    }
    catch (Exception e) {
      return false;
    }
    return false;
  }
  
  public String toString() {
    return "ScrapeAlias [class=AliasMonitoredListRecordData, name=" + this.getName() + ", language=" + this.getLanguage() + ", script=" + this.getScript() + ", type=" + this.getType() + "]";
  }

  public String getLanguage() {
    return this.getAttribute(AliasMonitoredListRecordData.LANGUAGE);
    //return language;
  }

  public void setLanguage(String language) {
    this.setAttribute(AliasMonitoredListRecordData.LANGUAGE, language);
    //this.language = language;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public MonitoredListRecordDataField getField() {
    return MonitoredListRecordDataField.ALIAS;
  }

  @Override
  public boolean isTransient(String attributeName) {
    return false;
  }  
  
	public String getSortedAttributes() {
		StringBuilder sb = new StringBuilder();
		sb.append(getField());
		Map<String, String> theMap;
		Map<String, String> theTempMap = new HashMap<String, String>();
		theTempMap.putAll(getAttributeMap());
		if (theTempMap != null) {
			//we want to uppercase all names (so the hashing is impervious to change of case) for all but the local script type
			String type = theTempMap.get("TYPE");
			boolean loc = "LOC".equals(type);
			
			String name = theTempMap.remove("NAME");
			if (name != null) {
				theTempMap.put("VALUE", name);
			}
			theMap = sortByKey(theTempMap);

			for (Map.Entry<String, String> entry : theMap.entrySet()) {
				if (!(
						"EMENDED".equals(entry.getKey()) || 
						("TYPE".equals(entry.getKey()) && AliasType.AKA.getCode().equals(entry.getValue())) || 
						("QUALITY".equals(entry.getKey()) && "0".equals(entry.getValue())) 
						)) {
					sb.append(entry.getKey() + (!loc && entry.getKey().equals("VALUE") ? entry.getValue().toUpperCase() : entry.getValue()));
				}
			}
		}
		return sb.toString();
	}
}
