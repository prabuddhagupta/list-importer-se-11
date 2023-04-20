package com.rdc.importer.misc;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;

public class StringMonitoredListRecordData extends MonitoredListRecordData implements Serializable {
    MonitoredListRecordDataField field;
    private static final String VALUE = "VALUE";

    public MonitoredListRecordDataField getField() {
        return field;
    }

    public boolean isTransient(String attributeName) {
        return false;
    }

    public void setField(MonitoredListRecordDataField field) {
        this.field = field;
    }

    public String getString() {
        return getAttribute(VALUE);
    }

    public void setString(String string) {
        setAttribute(VALUE, string);
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof StringMonitoredListRecordData)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        StringMonitoredListRecordData rhs = (StringMonitoredListRecordData) obj;
        return new EqualsBuilder()
                .append(getField(), rhs.getField())
                .append(getString(), rhs.getString())
                .isEquals();
    }

    public int hashCode() {
        int result;
        result = getField() != null ? getField().hashCode() : 0;
        result = 31 * result + (getString() != null ? getString().hashCode() : 0);
        return result;
    }
    
	public String getSortedAttributes() {
		Map<String, String> theMap = sortByKey(getAttributeMap()); //Sort by field name
		StringBuilder sb = new StringBuilder();
		sb.append(getField());
		for (Map.Entry<String, String> entry : theMap.entrySet()) {
			if( !("EMENDED".equals(entry.getKey()) || "FILE_ID".equals(entry.getKey()))) {
			sb.append(entry.getKey() + (MonitoredListRecordDataField.ALIAS.equals(field) ? entry.getValue().toUpperCase() :entry.getValue()));
			}
		}
		return sb.toString();
	}
}
