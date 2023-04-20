package com.rdc.importer.misc;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.rdc.importer.misc.AliasType;

public abstract class MonitoredListRecordData implements Comparable<MonitoredListRecordData>, Serializable {
    private Date createdDateTime;
    private Integer recordDataIdentifier;
    private Map<String, String> attributeMap = new HashMap<String, String>();
    private final int EQUAL = 0;
    
    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Integer getRecordDataIdentifier() {
        return recordDataIdentifier;
    }
    
	public int compareTo(MonitoredListRecordData o) {
		if(this == o){
			return EQUAL;
		}
		return this.getField().name().compareTo(o.getField().name());
	}
	
	public Integer getEmendedPointer(){
		return getAttributeAsInteger("EMENDED");
	}
	
	public void setEmendedPointer(Integer da_id){
		setAttribute("EMENDED", da_id);
	}
	
	public boolean isEqualForDiff(MonitoredListRecordData monitoredListRecordData){
		Map<String, String> inMap = monitoredListRecordData.getAttributeMap();
		
		if(inMap == null || attributeMap == null){
			return false;
		}
		
		if (!getField().equals(monitoredListRecordData.getField())) {
				return false;
		}
		
		if(getField().equals(MonitoredListRecordDataField.ALIAS)){
			if ((this instanceof StringMonitoredListRecordData && getField().equals(MonitoredListRecordDataField.ALIAS)) && monitoredListRecordData instanceof AliasMonitoredListRecordData) {
				return this.getAttribute("VALUE").equals(((AliasMonitoredListRecordData) monitoredListRecordData).getName());
			} else if ((monitoredListRecordData instanceof StringMonitoredListRecordData && getField().equals(MonitoredListRecordDataField.ALIAS)) && this instanceof AliasMonitoredListRecordData) {
				return monitoredListRecordData.getAttribute("VALUE").equals(((AliasMonitoredListRecordData) this).getName());			
			}
		}
		
		Map<String, String> theMap = sortByKey(attributeMap); //Sort by field name
		Map<String, String> theInMap = sortByKey(inMap); //Sort by field name
		
		
		for (Map.Entry<String, String> entry : theMap.entrySet()) {
			String key = entry.getKey();
			String inVal = theInMap.get(key);
			if( !entry.getValue().equals(inVal) && !("FILE_ID".equals(key) || "EMENDED".equals(key) || (getField().equals(MonitoredListRecordDataField.ALIAS) && "TYPE".equals(key) && inVal == null && entry.getValue().equals(AliasType.AKA.getCode()))) ){
				return false;
			}
		}
		
		for (Map.Entry<String, String> entry : theInMap.entrySet()) {
			String key = entry.getKey();
			String val = theMap.get(key);
			if( !entry.getValue().equals(val)  && !("FILE_ID".equals(key) || "EMENDED".equals(key) ||  (getField().equals(MonitoredListRecordDataField.ALIAS) && "TYPE".equals(key) &&   val == null && entry.getValue().equals(AliasType.AKA.getCode()))) ){
				return false;
			}
		}
		
		return true;
	}
	
	public String getSortedAttributes() {
		Map<String, String> theMap = sortByKey(attributeMap); //Sort by field name
		StringBuilder sb = new StringBuilder();
		sb.append(getField());
		for (Map.Entry<String, String> entry : theMap.entrySet()) {
			if( !("EMENDED".equals(entry.getKey()) || "FILE_ID".equals(entry.getKey()))) {
			sb.append(entry.getKey() + entry.getValue());
			}
		}
		return sb.toString();
	}
	
	public static Map<String, String> sortByKey(Map<String, String> map) {
		List<Map.Entry<String, String>> list = new LinkedList<Map.Entry<String, String>>(map.entrySet());
		
		Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
			public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
				if(o1.getKey().compareTo(o2.getKey()) == 0){
					return o1.getValue().compareTo(o2.getValue());
				}
				return o1.getKey().compareTo(o2.getKey());
			}
		});

		Map<String, String> result = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

    public void setRecordDataIdentifier(Integer recordDataIdentifier) {
        this.recordDataIdentifier = recordDataIdentifier;
    }

    public String getAttribute(String attributeName) {
        return attributeMap.get(attributeName);
    }

    public Boolean getAttributeAsBoolean(String attributeName) {
        String attribute = getAttribute(attributeName);
        return attribute == null ? false : Boolean.valueOf(attribute);
    }

    public Integer getAttributeAsInteger(String attributeName) {
        String attribute = getAttribute(attributeName);
        return attribute == null ? null : Integer.valueOf(attribute);
    }

    public void setAttribute(String attributeName, String attributeValue) {
        if (StringUtils.isNotBlank(attributeValue)) {
            attributeMap.put(attributeName, attributeValue);
        }
    }

    public void setAttribute(String attributeName, Boolean attributeValue) {
        if (attributeValue != null) {
            attributeMap.put(attributeName, attributeValue.toString());
        }
    }

    public void setAttribute(String attributeName, Integer attributeValue) {
        if (attributeValue != null) {
            attributeMap.put(attributeName, attributeValue.toString());
        }
    }

    public Map<String, String> getAttributeMap() {
        return attributeMap;
    }

    public abstract MonitoredListRecordDataField getField();

    public abstract boolean isTransient(String attributeName);

    
}
