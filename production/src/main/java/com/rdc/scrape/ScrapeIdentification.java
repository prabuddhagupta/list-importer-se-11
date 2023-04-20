package com.rdc.scrape;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

public class ScrapeIdentification implements Serializable {
    private String type;
    private String location;
    private String country;
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScrapeIdentification that = (ScrapeIdentification) o;

        if (country != null ? !country.equals(that.country) : that.country != null) {
            return false;
        }
        if (location != null ? !location.equals(that.location) : that.location != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString(){
        return ("" + (StringUtils.isNotBlank(type) ? type + " " : "") 
                + (StringUtils.isNotBlank(value) ? value + " "  : "") 
                + (StringUtils.isNotBlank(country) ? country + " "  : "") 
                + (StringUtils.isNotBlank(location) ? location : "")).trim() ;
    }
}