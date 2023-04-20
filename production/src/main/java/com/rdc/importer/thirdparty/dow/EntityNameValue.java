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
 *         &lt;element ref="{}Prefix" minOccurs="0"/>
 *         &lt;element ref="{}EntityName" minOccurs="0"/>
 *         &lt;element ref="{}Suffix" minOccurs="0"/>
 *         &lt;element ref="{}OriginalScriptName" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}SingleStringName" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="SpellingVariation" type="{}EntitySpellingVariation" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "prefix",
    "entityName",
    "suffix",
    "originalScriptName",
    "singleStringName",
    "spellingVariation"
})
@XmlRootElement(name = "EntityNameValue")
public class EntityNameValue {

    @XmlElement(name = "Prefix")
    protected String prefix;
    @XmlElement(name = "EntityName")
    protected String entityName;
    @XmlElement(name = "Suffix")
    protected String suffix;
    @XmlElement(name = "OriginalScriptName")
    protected List<OriginalScriptName> originalScriptName;
    @XmlElement(name = "SingleStringName")
    protected List<SingleStringName> singleStringName;
    @XmlElement(name = "SpellingVariation")
    protected List<EntitySpellingVariation> spellingVariation;

    /**
     * Gets the value of the prefix property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the value of the prefix property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrefix(String value) {
        this.prefix = value;
    }

    /**
     * Gets the value of the entityName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Sets the value of the entityName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEntityName(String value) {
        this.entityName = value;
    }

    /**
     * Gets the value of the suffix property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Sets the value of the suffix property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSuffix(String value) {
        this.suffix = value;
    }

    /**
     * Gets the value of the originalScriptName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the originalScriptName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOriginalScriptName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OriginalScriptName }
     * 
     * 
     */
    public List<OriginalScriptName> getOriginalScriptName() {
        if (originalScriptName == null) {
            originalScriptName = new ArrayList<OriginalScriptName>();
        }
        return this.originalScriptName;
    }

    /**
     * Gets the value of the singleStringName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the singleStringName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSingleStringName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SingleStringName }
     * 
     * 
     */
    public List<SingleStringName> getSingleStringName() {
        if (singleStringName == null) {
            singleStringName = new ArrayList<SingleStringName>();
        }
        return this.singleStringName;
    }

    /**
     * Gets the value of the spellingVariation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the spellingVariation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSpellingVariation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EntitySpellingVariation }
     * 
     * 
     */
    public List<EntitySpellingVariation> getSpellingVariation() {
        if (spellingVariation == null) {
            spellingVariation = new ArrayList<EntitySpellingVariation>();
        }
        return this.spellingVariation;
    }

}