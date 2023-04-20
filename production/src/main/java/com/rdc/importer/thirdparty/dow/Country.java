//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.16 at 01:11:15 PM EDT 
//


package com.rdc.importer.thirdparty.dow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;>ContentRestriction">
 *       &lt;attribute ref="{}CountryId use="required""/>
 *       &lt;attribute ref="{}DJIIRegionCode use="required""/>
 *       &lt;attribute ref="{}ISO2CountryCode"/>
 *       &lt;attribute ref="{}ISO3CountryCode"/>
 *       &lt;attribute name="IsTerritory" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *             &lt;maxLength value="5"/>
 *             &lt;enumeration value="False"/>
 *             &lt;enumeration value="True"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="ParentCountryCode">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;maxLength value="3"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="CountryProfile" type="{}ContentRestriction" />
 *       &lt;attribute ref="{}action use="required""/>
 *       &lt;attribute ref="{}date use="required""/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
@XmlRootElement(name = "Country")
public class Country {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "CountryId", required = true)
    protected String countryId;
    @XmlAttribute(name = "DJIIRegionCode", required = true)
    protected String djiiRegionCode;
    @XmlAttribute(name = "ISO2CountryCode")
    protected String iso2CountryCode;
    @XmlAttribute(name = "ISO3CountryCode")
    protected String iso3CountryCode;
    @XmlAttribute(name = "IsTerritory", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String isTerritory;
    @XmlAttribute(name = "ParentCountryCode")
    protected String parentCountryCode;
    @XmlAttribute(name = "CountryProfile")
    protected String countryProfile;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String action;
    @XmlAttribute(required = true)
    protected String date;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the countryId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCountryId() {
        return countryId;
    }

    /**
     * Sets the value of the countryId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCountryId(String value) {
        this.countryId = value;
    }

    /**
     * Gets the value of the djiiRegionCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDJIIRegionCode() {
        return djiiRegionCode;
    }

    /**
     * Sets the value of the djiiRegionCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDJIIRegionCode(String value) {
        this.djiiRegionCode = value;
    }

    /**
     * Gets the value of the iso2CountryCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getISO2CountryCode() {
        return iso2CountryCode;
    }

    /**
     * Sets the value of the iso2CountryCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setISO2CountryCode(String value) {
        this.iso2CountryCode = value;
    }

    /**
     * Gets the value of the iso3CountryCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getISO3CountryCode() {
        return iso3CountryCode;
    }

    /**
     * Sets the value of the iso3CountryCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setISO3CountryCode(String value) {
        this.iso3CountryCode = value;
    }

    /**
     * Gets the value of the isTerritory property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsTerritory() {
        return isTerritory;
    }

    /**
     * Sets the value of the isTerritory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsTerritory(String value) {
        this.isTerritory = value;
    }

    /**
     * Gets the value of the parentCountryCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getParentCountryCode() {
        return parentCountryCode;
    }

    /**
     * Sets the value of the parentCountryCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParentCountryCode(String value) {
        this.parentCountryCode = value;
    }

    /**
     * Gets the value of the countryProfile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCountryProfile() {
        return countryProfile;
    }

    /**
     * Sets the value of the countryProfile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCountryProfile(String value) {
        this.countryProfile = value;
    }

    /**
     * Gets the value of the action property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAction(String value) {
        this.action = value;
    }

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDate(String value) {
        this.date = value;
    }

}
