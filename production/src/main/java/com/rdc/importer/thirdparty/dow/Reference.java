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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for Reference complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Reference">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;>ContentRestriction">
 *       &lt;attribute ref="{}fieldaction"/>
 *       &lt;attribute name="SinceDay">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;maxLength value="2"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="SinceMonth">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;maxLength value="3"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="SinceYear">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;maxLength value="4"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="ToDay">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;maxLength value="2"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="ToMonth">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;maxLength value="3"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="ToYear">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;maxLength value="4"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Reference", propOrder = {
    "value"
})
public class Reference {

    @XmlValue
    protected String value;
    @XmlAttribute
    protected String fieldaction;
    @XmlAttribute(name = "SinceDay")
    protected String sinceDay;
    @XmlAttribute(name = "SinceMonth")
    protected String sinceMonth;
    @XmlAttribute(name = "SinceYear")
    protected String sinceYear;
    @XmlAttribute(name = "ToDay")
    protected String toDay;
    @XmlAttribute(name = "ToMonth")
    protected String toMonth;
    @XmlAttribute(name = "ToYear")
    protected String toYear;

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
     * Gets the value of the fieldaction property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFieldaction() {
        return fieldaction;
    }

    /**
     * Sets the value of the fieldaction property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFieldaction(String value) {
        this.fieldaction = value;
    }

    /**
     * Gets the value of the sinceDay property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSinceDay() {
        return sinceDay;
    }

    /**
     * Sets the value of the sinceDay property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSinceDay(String value) {
        this.sinceDay = value;
    }

    /**
     * Gets the value of the sinceMonth property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSinceMonth() {
        return sinceMonth;
    }

    /**
     * Sets the value of the sinceMonth property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSinceMonth(String value) {
        this.sinceMonth = value;
    }

    /**
     * Gets the value of the sinceYear property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSinceYear() {
        return sinceYear;
    }

    /**
     * Sets the value of the sinceYear property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSinceYear(String value) {
        this.sinceYear = value;
    }

    /**
     * Gets the value of the toDay property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToDay() {
        return toDay;
    }

    /**
     * Sets the value of the toDay property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToDay(String value) {
        this.toDay = value;
    }

    /**
     * Gets the value of the toMonth property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToMonth() {
        return toMonth;
    }

    /**
     * Sets the value of the toMonth property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToMonth(String value) {
        this.toMonth = value;
    }

    /**
     * Gets the value of the toYear property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToYear() {
        return toYear;
    }

    /**
     * Sets the value of the toYear property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToYear(String value) {
        this.toYear = value;
    }

}
