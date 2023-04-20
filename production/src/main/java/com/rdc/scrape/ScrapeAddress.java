package com.rdc.scrape;

import java.io.Serializable;

public class ScrapeAddress implements Serializable {
    private String rawFormat;
    private String address1;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private Boolean birthPlace;
    private String type;

	public ScrapeAddress(ScrapeAddress in) {
		this.rawFormat = in.rawFormat;
		this.address1 = in.address1;
		this.city = in.city;
		this.province = in.province;
		this.postalCode = in.postalCode;
		this.country = in.country;
		this.birthPlace = in.birthPlace;
		this.type = in.type;
	}
	
	public ScrapeAddress() {
	}
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRawFormat() {
        return rawFormat;
    }

    public void setRawFormat(String rawFormat) {
        this.rawFormat = rawFormat;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Boolean getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(Boolean birthPlace) {
        this.birthPlace = birthPlace;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScrapeAddress that = (ScrapeAddress) o;

        if (address1 != null ? !address1.equals(that.address1) : that.address1 != null) {
            return false;
        }
        if (birthPlace != null ? !birthPlace.equals(that.birthPlace) : that.birthPlace != null) {
            return false;
        }
        if (city != null ? !city.equals(that.city) : that.city != null) {
            return false;
        }
        if (country != null ? !country.equals(that.country) : that.country != null) {
            return false;
        }
        if (rawFormat != null ? !rawFormat.equals(that.rawFormat) : that.rawFormat != null) {
            return false;
        }
        if (postalCode != null ? !postalCode.equals(that.postalCode) : that.postalCode != null) {
            return false;
        }
        if (province != null ? !province.equals(that.province) : that.province != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (rawFormat != null ? rawFormat.hashCode() : 0);
        result = 31 * result + (address1 != null ? address1.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (province != null ? province.hashCode() : 0);
        result = 31 * result + (postalCode != null ? postalCode.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (birthPlace != null ? birthPlace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ScrapeAddress [rawFormat=" + rawFormat + ", address1=" + address1 + ", city=" + city + ", province=" + province + ", postalCode="
                + postalCode + ", country=" + country + ", birthPlace=" + birthPlace + ", type=" + type + "]";
    }
    
    
}
