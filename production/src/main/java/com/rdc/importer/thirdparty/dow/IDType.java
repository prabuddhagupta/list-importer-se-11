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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}IDValue"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{}IdentificationTypeId use="required""/>
 *       &lt;attribute ref="{}fieldaction"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "idValue"
})
@XmlRootElement(name = "IDType")
public class IDType {

    @XmlElement(name = "IDValue", required = true)
    protected IDValue idValue;
    @XmlAttribute(name = "IdentificationTypeId", required = true)
    protected String identificationTypeId;
    @XmlAttribute
    protected String fieldaction;

    /**
     * Gets the value of the idValue property.
     * 
     * @return
     *     possible object is
     *     {@link IDValue }
     *     
     */
    public IDValue getIDValue() {
        return idValue;
    }

    /**
     * Sets the value of the idValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link IDValue }
     *     
     */
    public void setIDValue(IDValue value) {
        this.idValue = value;
    }

    /**
     * Gets the value of the identificationTypeId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdentificationTypeId() {
        return identificationTypeId;
    }

    /**
     * Sets the value of the identificationTypeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdentificationTypeId(String value) {
        this.identificationTypeId = value;
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

}