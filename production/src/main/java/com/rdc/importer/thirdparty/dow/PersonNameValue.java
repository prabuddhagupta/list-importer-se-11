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
 *         &lt;element ref="{}TitleHonorific" minOccurs="0"/>
 *         &lt;element ref="{}FirstName" minOccurs="0"/>
 *         &lt;element ref="{}MiddleName" minOccurs="0"/>
 *         &lt;element ref="{}Surname" minOccurs="0"/>
 *         &lt;element ref="{}MaidenName" minOccurs="0"/>
 *         &lt;element ref="{}Suffix" minOccurs="0"/>
 *         &lt;element ref="{}OriginalScriptName" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}SingleStringName" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="SpellingVariation" type="{}PersonSpellingVariation" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
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
    "titleHonorific",
    "firstName",
    "middleName",
    "surname",
    "maidenName",
    "suffix",
    "originalScriptName",
    "singleStringName",
    "spellingVariation"
})
@XmlRootElement(name = "PersonNameValue")
public class PersonNameValue {

    @XmlElement(name = "TitleHonorific")
    protected String titleHonorific;
    @XmlElement(name = "FirstName")
    protected String firstName;
    @XmlElement(name = "MiddleName")
    protected String middleName;
    @XmlElement(name = "Surname")
    protected String surname;
    @XmlElement(name = "MaidenName")
    protected MaidenName maidenName;
    @XmlElement(name = "Suffix")
    protected String suffix;
    @XmlElement(name = "OriginalScriptName")
    protected List<OriginalScriptName> originalScriptName;
    @XmlElement(name = "SingleStringName")
    protected List<SingleStringName> singleStringName;
    @XmlElement(name = "SpellingVariation")
    protected List<PersonSpellingVariation> spellingVariation;
    @XmlAttribute
    protected String fieldaction;

    /**
     * Gets the value of the titleHonorific property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitleHonorific() {
        return titleHonorific;
    }

    /**
     * Sets the value of the titleHonorific property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitleHonorific(String value) {
        this.titleHonorific = value;
    }

    /**
     * Gets the value of the firstName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the value of the firstName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFirstName(String value) {
        this.firstName = value;
    }

    /**
     * Gets the value of the middleName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Sets the value of the middleName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMiddleName(String value) {
        this.middleName = value;
    }

    /**
     * Gets the value of the surname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets the value of the surname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSurname(String value) {
        this.surname = value;
    }

    /**
     * Gets the value of the maidenName property.
     * 
     * @return
     *     possible object is
     *     {@link MaidenName }
     *     
     */
    public MaidenName getMaidenName() {
        return maidenName;
    }

    /**
     * Sets the value of the maidenName property.
     * 
     * @param value
     *     allowed object is
     *     {@link MaidenName }
     *     
     */
    public void setMaidenName(MaidenName value) {
        this.maidenName = value;
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
     * {@link PersonSpellingVariation }
     * 
     * 
     */
    public List<PersonSpellingVariation> getSpellingVariation() {
        if (spellingVariation == null) {
            spellingVariation = new ArrayList<PersonSpellingVariation>();
        }
        return this.spellingVariation;
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
