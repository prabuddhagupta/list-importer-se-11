package com.rdc.scrape;

import java.io.Serializable;

import com.rdc.rdcmodel.model.RelationshipType;
import org.apache.commons.lang.StringUtils;

//import com.rdc.importer.misc.RelationshipType;

public class ScrapeEntityAssociation implements Serializable {

	private String id;
	private RelationshipType relationshipType;
    private String hashable;
	
	public ScrapeEntityAssociation(){}
	
	public ScrapeEntityAssociation(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public void setHashable(String name, String type){
		hashable = name + ":" + type;
	}

	public String getHashable() {
		return hashable;
	}

	public void setHashable(String hashable) {
		this.hashable = hashable;
	}

	public RelationshipType getRelationshipType() {
		return relationshipType;
	}
	public void setRelationshipType(RelationshipType relationshipType) {
		this.relationshipType = relationshipType;
	}
	
	public void setDescription(String desc){
		if(StringUtils.isNotBlank(desc)){
			relationshipType = RelationshipType.getEnumByCode(desc);
		}
	}
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScrapeEntityAssociation that = (ScrapeEntityAssociation) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (relationshipType != null ? !relationshipType.equals(that.relationshipType) : that.relationshipType != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (relationshipType != null ? relationshipType.hashCode() : 0);
        return result;
    }
}
