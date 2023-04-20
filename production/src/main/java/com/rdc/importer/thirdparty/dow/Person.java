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
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


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
 *         &lt;element ref="{}Gender" minOccurs="0"/>
 *         &lt;element ref="{}ActiveStatus" minOccurs="0"/>
 *         &lt;element ref="{}Deceased" minOccurs="0"/>
 *         &lt;element ref="{}PersonNameDetails" minOccurs="0"/>
 *         &lt;element ref="{}Descriptions"/>
 *         &lt;element ref="{}DateDetails" minOccurs="0"/>
 *         &lt;element ref="{}RoleDetails" minOccurs="0"/>
 *         &lt;element ref="{}BirthPlaceDetails" minOccurs="0"/>
 *         &lt;element ref="{}AddressDetails" minOccurs="0"/>
 *         &lt;element ref="{}SanctionsReferences" minOccurs="0"/>
 *         &lt;element ref="{}CountryDetails" minOccurs="0"/>
 *         &lt;element ref="{}IdentificationDetails" minOccurs="0"/>
 *         &lt;element ref="{}SourceDetails" minOccurs="0"/>
 *         &lt;element ref="{}ProfileNotes" minOccurs="0"/>
 *         &lt;element ref="{}Images" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute ref="{}recordaction use="required""/>
 *       &lt;attribute ref="{}date use="required""/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "gender",
    //"activeStatus",
    "deceased",
    "personNameDetails",
    //"descriptions",
    "dateDetails",
    "roleDetails",
    "birthPlaceDetails",
    "addressDetails",
    "sanctionsReferences",
    "countryDetails",
    "identificationDetails",
    //"sourceDetails",
    "profileNotes",
    "images"
})
@XmlRootElement(name = "Person")
public class Person {

    @XmlElement(name = "Gender")
    protected Gender gender;
    //@XmlElement(name = "ActiveStatus")
    //protected ActiveStatus activeStatus;
    @XmlElement(name = "Deceased")
    protected Deceased deceased;
    @XmlElement(name = "PersonNameDetails")
    protected PersonNameDetails personNameDetails;
    //@XmlElement(name = "Descriptions", required = true)
    //protected Descriptions descriptions;
    @XmlElement(name = "DateDetails")
    protected DateDetails dateDetails;
    @XmlElement(name = "RoleDetails")
    protected RoleDetails roleDetails;
    @XmlElement(name = "BirthPlaceDetails")
    protected BirthPlaceDetails birthPlaceDetails;
    @XmlElement(name = "AddressDetails")
    protected AddressDetails addressDetails;
    @XmlElement(name = "SanctionsReferences")
    protected SanctionsReferences sanctionsReferences;
    @XmlElement(name = "CountryDetails")
    protected CountryDetails countryDetails;
    @XmlElement(name = "IdentificationDetails")
    protected IdentificationDetails identificationDetails;
    //@XmlElement(name = "SourceDetails")
    //protected SourceDetails sourceDetails;
    @XmlElement(name = "ProfileNotes")
    protected ProfileNotes profileNotes;
    @XmlElement(name = "Images")
    protected Images images;
    @XmlAttribute(required = true)
    protected String id;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String recordaction;
    @XmlAttribute(required = true)
    protected String date;

    /**
     * Gets the value of the gender property.
     * 
     * @return
     *     possible object is
     *     {@link Gender }
     *     
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Sets the value of the gender property.
     * 
     * @param value
     *     allowed object is
     *     {@link Gender }
     *     
     */
    public void setGender(Gender value) {
        this.gender = value;
    }

    /**
     * Gets the value of the activeStatus property.
     * 
     * @return
     *     possible object is
     *     {@link ActiveStatus }
     *     
     */
//    public ActiveStatus getActiveStatus() {
//        return activeStatus;
//    }

    /**
     * Sets the value of the activeStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link ActiveStatus }
     *     
     */
//    public void setActiveStatus(ActiveStatus value) {
//        this.activeStatus = value;
//    }

    /**
     * Gets the value of the deceased property.
     * 
     * @return
     *     possible object is
     *     {@link Deceased }
     *     
     */
    public Deceased getDeceased() {
        return deceased;
    }

    /**
     * Sets the value of the deceased property.
     * 
     * @param value
     *     allowed object is
     *     {@link Deceased }
     *     
     */
    public void setDeceased(Deceased value) {
        this.deceased = value;
    }

    /**
     * Gets the value of the personNameDetails property.
     * 
     * @return
     *     possible object is
     *     {@link PersonNameDetails }
     *     
     */
    public PersonNameDetails getPersonNameDetails() {
        return personNameDetails;
    }

    /**
     * Sets the value of the personNameDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link PersonNameDetails }
     *     
     */
    public void setPersonNameDetails(PersonNameDetails value) {
        this.personNameDetails = value;
    }

    /**
     * Gets the value of the descriptions property.
     * 
     * @return
     *     possible object is
     *     {@link Descriptions }
     *     
     */
//    public Descriptions getDescriptions() {
//        return descriptions;
//    }

    /**
     * Sets the value of the descriptions property.
     * 
     * @param value
     *     allowed object is
     *     {@link Descriptions }
     *     
     */
//    public void setDescriptions(Descriptions value) {
//        this.descriptions = value;
//    }

    /**
     * Gets the value of the dateDetails property.
     * 
     * @return
     *     possible object is
     *     {@link DateDetails }
     *     
     */
    public DateDetails getDateDetails() {
    	return dateDetails;
    }

    /**
     * Sets the value of the dateDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateDetails }
     *     
     */
    public void setDateDetails(DateDetails value) {
    	this.dateDetails = value;
    }
    
    /**
     * Gets the value of the roleDetails property.
     * 
     * @return
     *     possible object is
     *     {@link RoleDetails }
     *     
     */
    public RoleDetails getRoleDetails() {
    	return roleDetails;
    }

    /**
     * Sets the value of the roleDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link RoleDetails }
     *     
     */
    public void setRoleDetails(RoleDetails value) {
        this.roleDetails = value;
    }

    /**
     * Gets the value of the birthPlaceDetails property.
     * 
     * @return
     *     possible object is
     *     {@link BirthPlaceDetails }
     *     
     */
    public BirthPlaceDetails getBirthPlaceDetails() {
        return birthPlaceDetails;
    }

    /**
     * Sets the value of the birthPlaceDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link BirthPlaceDetails }
     *     
     */
    public void setBirthPlaceDetails(BirthPlaceDetails value) {
        this.birthPlaceDetails = value;
    }

    /**
     * Gets the value of the addressDetails property.
     * 
     * @return
     *     possible object is
     *     {@link AddressDetails }
     *     
     */
    public AddressDetails getAddressDetails() {
        return addressDetails;
    }

    /**
     * Sets the value of the addressDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link AddressDetails }
     *     
     */
    public void setAddressDetails(AddressDetails value) {
        this.addressDetails = value;
    }

    /**
     * Gets the value of the sanctionsReferences property.
     * 
     * @return
     *     possible object is
     *     {@link SanctionsReferences }
     *     
     */
    public SanctionsReferences getSanctionsReferences() {
        return sanctionsReferences;
    }

    /**
     * Sets the value of the sanctionsReferences property.
     * 
     * @param value
     *     allowed object is
     *     {@link SanctionsReferences }
     *     
     */
    public void setSanctionsReferences(SanctionsReferences value) {
        this.sanctionsReferences = value;
    }

    /**
     * Gets the value of the countryDetails property.
     * 
     * @return
     *     possible object is
     *     {@link CountryDetails }
     *     
     */
    public CountryDetails getCountryDetails() {
        return countryDetails;
    }

    /**
     * Sets the value of the countryDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link CountryDetails }
     *     
     */
    public void setCountryDetails(CountryDetails value) {
        this.countryDetails = value;
    }

    /**
     * Gets the value of the identificationDetails property.
     * 
     * @return
     *     possible object is
     *     {@link IdentificationDetails }
     *     
     */
    public IdentificationDetails getIdentificationDetails() {
        return identificationDetails;
    }

    /**
     * Sets the value of the identificationDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link IdentificationDetails }
     *     
     */
    public void setIdentificationDetails(IdentificationDetails value) {
        this.identificationDetails = value;
    }

    /**
     * Gets the value of the sourceDetails property.
     * 
     * @return
     *     possible object is
     *     {@link SourceDetails }
     *     
     */
//    public SourceDetails getSourceDetails() {
//        return sourceDetails;
//    }

    /**
     * Sets the value of the sourceDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link SourceDetails }
     *     
     */
//    public void setSourceDetails(SourceDetails value) {
//        this.sourceDetails = value;
//    }

    /**
     * Gets the value of the profileNotes property.
     * 
     * @return
     *     possible object is
     *     {@link ProfileNotes }
     *     
     */
    public ProfileNotes getProfileNotes() {
        return profileNotes;
    }

    /**
     * Sets the value of the profileNotes property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProfileNotes }
     *     
     */
    public void setProfileNotes(ProfileNotes value) {
        this.profileNotes = value;
    }

    /**
     * Gets the value of the images property.
     * 
     * @return
     *     possible object is
     *     {@link Images }
     *     
     */
    public Images getImages() {
        return images;
    }

    /**
     * Sets the value of the images property.
     * 
     * @param value
     *     allowed object is
     *     {@link Images }
     *     
     */
    public void setImages(Images value) {
        this.images = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the recordaction property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRecordaction() {
        return recordaction;
    }

    /**
     * Sets the value of the recordaction property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRecordaction(String value) {
        this.recordaction = value;
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