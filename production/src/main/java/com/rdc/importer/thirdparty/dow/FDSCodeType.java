//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.16 at 01:11:15 PM EDT 
//


package com.rdc.importer.thirdparty.dow;

import java.util.ArrayList;
import java.util.List;
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
 *         &lt;element ref="{}FDSCodeValue" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{}FDSTypeId use="required""/>
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
    "fdsCodeValue"
})
@XmlRootElement(name = "FDSCodeType")
public class FDSCodeType {

    @XmlElement(name = "FDSCodeValue", required = true)
    protected List<FDSCodeValue> fdsCodeValue;
    @XmlAttribute(name = "FDSTypeId", required = true)
    protected String fdsTypeId;
    @XmlAttribute
    protected String fieldaction;

    /**
     * Gets the value of the fdsCodeValue property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fdsCodeValue property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFDSCodeValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FDSCodeValue }
     * 
     * 
     */
    public List<FDSCodeValue> getFDSCodeValue() {
        if (fdsCodeValue == null) {
            fdsCodeValue = new ArrayList<FDSCodeValue>();
        }
        return this.fdsCodeValue;
    }

    /**
     * Gets the value of the fdsTypeId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFDSTypeId() {
        return fdsTypeId;
    }

    /**
     * Sets the value of the fdsTypeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFDSTypeId(String value) {
        this.fdsTypeId = value;
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
