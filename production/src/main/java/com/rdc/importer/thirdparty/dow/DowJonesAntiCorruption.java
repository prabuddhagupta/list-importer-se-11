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
 *         &lt;element ref="{}GenderList"/>
 *         &lt;element ref="{}DeceasedList"/>
 *         &lt;element ref="{}RecordStatusList"/>
 *         &lt;element ref="{}SubsidiaryList"/>
 *         &lt;element ref="{}NameTypeList"/>
 *         &lt;element ref="{}ScriptLanguagesList"/>
 *         &lt;element ref="{}Description1List"/>
 *         &lt;element ref="{}Description2List"/>
 *         &lt;element ref="{}Description3List" minOccurs="0"/>
 *         &lt;element ref="{}DateTypeList"/>
 *         &lt;element ref="{}RoleTypeList"/>
 *         &lt;element ref="{}OccupationCategoryList"/>
 *         &lt;element ref="{}SanctionsReferencesList"/>
 *         &lt;element ref="{}CountryTypeList"/>
 *         &lt;element ref="{}CountryList"/>
 *         &lt;element ref="{}IdentificationTypeList" minOccurs="0"/>
 *         &lt;element ref="{}FDSTypeList"/>
 *         &lt;element ref="{}DJIIIndustryList"/>
 *         &lt;element ref="{}StakeLevel1List"/>
 *         &lt;element ref="{}StakeLevel2List"/>
 *         &lt;element ref="{}StakeLevel3List"/>
 *         &lt;element ref="{}RelationshipList"/>
 *         &lt;element ref="{}Records"/>
 *         &lt;element ref="{}Associations" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="type" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *             &lt;maxLength value="6"/>
 *             &lt;enumeration value="daily"/>
 *             &lt;enumeration value="weekly"/>
 *             &lt;enumeration value="full"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
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
    "genderList",
    "deceasedList",
    "recordStatusList",
    "subsidiaryList",
    "nameTypeList",
    "scriptLanguagesList",
    "description1List",
    "description2List",
    "description3List",
    "dateTypeList",
    "roleTypeList",
    "occupationCategoryList",
    "sanctionsReferencesList",
    "countryTypeList",
    "countryList",
    "identificationTypeList",
    "fdsTypeList",
    "djiiIndustryList",
    "stakeLevel1List",
    "stakeLevel2List",
    "stakeLevel3List",
    "relationshipList",
    "records",
    "associations"
})
@XmlRootElement(name = "DowJonesAntiCorruption")
public class DowJonesAntiCorruption {

    @XmlElement(name = "GenderList", required = true)
    protected GenderList genderList;
    @XmlElement(name = "DeceasedList", required = true)
    protected DeceasedList deceasedList;
    @XmlElement(name = "RecordStatusList", required = true)
    protected RecordStatusList recordStatusList;
    @XmlElement(name = "SubsidiaryList", required = true)
    protected SubsidiaryList subsidiaryList;
    @XmlElement(name = "NameTypeList", required = true)
    protected NameTypeList nameTypeList;
    @XmlElement(name = "ScriptLanguagesList", required = true)
    protected ScriptLanguagesList scriptLanguagesList;
    @XmlElement(name = "Description1List", required = true)
    protected Description1List description1List;
    @XmlElement(name = "Description2List", required = true)
    protected Description2List description2List;
    @XmlElement(name = "Description3List")
    protected Description3List description3List;
    @XmlElement(name = "DateTypeList", required = true)
    protected DateTypeList dateTypeList;
    @XmlElement(name = "RoleTypeList", required = true)
    protected RoleTypeList roleTypeList;
    @XmlElement(name = "OccupationCategoryList", required = true)
    protected OccupationCategoryList occupationCategoryList;
    @XmlElement(name = "SanctionsReferencesList", required = true)
    protected SanctionsReferencesList sanctionsReferencesList;
    @XmlElement(name = "CountryTypeList", required = true)
    protected CountryTypeList countryTypeList;
    @XmlElement(name = "CountryList", required = true)
    protected CountryList countryList;
    @XmlElement(name = "IdentificationTypeList")
    protected IdentificationTypeList identificationTypeList;
    @XmlElement(name = "FDSTypeList", required = true)
    protected FDSTypeList fdsTypeList;
    @XmlElement(name = "DJIIIndustryList", required = true)
    protected DJIIIndustryList djiiIndustryList;
    @XmlElement(name = "StakeLevel1List", required = true)
    protected StakeLevel1List stakeLevel1List;
    @XmlElement(name = "StakeLevel2List", required = true)
    protected StakeLevel2List stakeLevel2List;
    @XmlElement(name = "StakeLevel3List", required = true)
    protected StakeLevel3List stakeLevel3List;
    @XmlElement(name = "RelationshipList", required = true)
    protected RelationshipList relationshipList;
    @XmlElement(name = "Records", required = true)
    protected Records records;
    @XmlElement(name = "Associations")
    protected Associations associations;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String type;
    @XmlAttribute(required = true)
    protected String date;

    /**
     * Gets the value of the genderList property.
     * 
     * @return
     *     possible object is
     *     {@link GenderList }
     *     
     */
    public GenderList getGenderList() {
        return genderList;
    }

    /**
     * Sets the value of the genderList property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenderList }
     *     
     */
    public void setGenderList(GenderList value) {
        this.genderList = value;
    }

    /**
     * Gets the value of the deceasedList property.
     * 
     * @return
     *     possible object is
     *     {@link DeceasedList }
     *     
     */
    public DeceasedList getDeceasedList() {
        return deceasedList;
    }

    /**
     * Sets the value of the deceasedList property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeceasedList }
     *     
     */
    public void setDeceasedList(DeceasedList value) {
        this.deceasedList = value;
    }

    /**
     * Gets the value of the recordStatusList property.
     * 
     * @return
     *     possible object is
     *     {@link RecordStatusList }
     *     
     */
    public RecordStatusList getRecordStatusList() {
        return recordStatusList;
    }

    /**
     * Sets the value of the recordStatusList property.
     * 
     * @param value
     *     allowed object is
     *     {@link RecordStatusList }
     *     
     */
    public void setRecordStatusList(RecordStatusList value) {
        this.recordStatusList = value;
    }

    /**
     * Gets the value of the subsidiaryList property.
     * 
     * @return
     *     possible object is
     *     {@link SubsidiaryList }
     *     
     */
    public SubsidiaryList getSubsidiaryList() {
        return subsidiaryList;
    }

    /**
     * Sets the value of the subsidiaryList property.
     * 
     * @param value
     *     allowed object is
     *     {@link SubsidiaryList }
     *     
     */
    public void setSubsidiaryList(SubsidiaryList value) {
        this.subsidiaryList = value;
    }

    /**
     * Gets the value of the nameTypeList property.
     * 
     * @return
     *     possible object is
     *     {@link NameTypeList }
     *     
     */
    public NameTypeList getNameTypeList() {
        return nameTypeList;
    }

    /**
     * Sets the value of the nameTypeList property.
     * 
     * @param value
     *     allowed object is
     *     {@link NameTypeList }
     *     
     */
    public void setNameTypeList(NameTypeList value) {
        this.nameTypeList = value;
    }

    /**
     * Gets the value of the scriptLanguagesList property.
     * 
     * @return
     *     possible object is
     *     {@link ScriptLanguagesList }
     *     
     */
    public ScriptLanguagesList getScriptLanguagesList() {
        return scriptLanguagesList;
    }

    /**
     * Sets the value of the scriptLanguagesList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScriptLanguagesList }
     *     
     */
    public void setScriptLanguagesList(ScriptLanguagesList value) {
        this.scriptLanguagesList = value;
    }

    /**
     * Gets the value of the description1List property.
     * 
     * @return
     *     possible object is
     *     {@link Description1List }
     *     
     */
    public Description1List getDescription1List() {
        return description1List;
    }

    /**
     * Sets the value of the description1List property.
     * 
     * @param value
     *     allowed object is
     *     {@link Description1List }
     *     
     */
    public void setDescription1List(Description1List value) {
        this.description1List = value;
    }

    /**
     * Gets the value of the description2List property.
     * 
     * @return
     *     possible object is
     *     {@link Description2List }
     *     
     */
    public Description2List getDescription2List() {
        return description2List;
    }

    /**
     * Sets the value of the description2List property.
     * 
     * @param value
     *     allowed object is
     *     {@link Description2List }
     *     
     */
    public void setDescription2List(Description2List value) {
        this.description2List = value;
    }

    /**
     * Gets the value of the description3List property.
     * 
     * @return
     *     possible object is
     *     {@link Description3List }
     *     
     */
    public Description3List getDescription3List() {
        return description3List;
    }

    /**
     * Sets the value of the description3List property.
     * 
     * @param value
     *     allowed object is
     *     {@link Description3List }
     *     
     */
    public void setDescription3List(Description3List value) {
        this.description3List = value;
    }

    /**
     * Gets the value of the dateTypeList property.
     * 
     * @return
     *     possible object is
     *     {@link DateTypeList }
     *     
     */
    public DateTypeList getDateTypeList() {
        return dateTypeList;
    }

    /**
     * Sets the value of the dateTypeList property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateTypeList }
     *     
     */
    public void setDateTypeList(DateTypeList value) {
        this.dateTypeList = value;
    }

    /**
     * Gets the value of the roleTypeList property.
     * 
     * @return
     *     possible object is
     *     {@link RoleTypeList }
     *     
     */
    public RoleTypeList getRoleTypeList() {
        return roleTypeList;
    }

    /**
     * Sets the value of the roleTypeList property.
     * 
     * @param value
     *     allowed object is
     *     {@link RoleTypeList }
     *     
     */
    public void setRoleTypeList(RoleTypeList value) {
        this.roleTypeList = value;
    }

    /**
     * Gets the value of the occupationCategoryList property.
     * 
     * @return
     *     possible object is
     *     {@link OccupationCategoryList }
     *     
     */
    public OccupationCategoryList getOccupationCategoryList() {
        return occupationCategoryList;
    }

    /**
     * Sets the value of the occupationCategoryList property.
     * 
     * @param value
     *     allowed object is
     *     {@link OccupationCategoryList }
     *     
     */
    public void setOccupationCategoryList(OccupationCategoryList value) {
        this.occupationCategoryList = value;
    }

    /**
     * Gets the value of the sanctionsReferencesList property.
     * 
     * @return
     *     possible object is
     *     {@link SanctionsReferencesList }
     *     
     */
    public SanctionsReferencesList getSanctionsReferencesList() {
        return sanctionsReferencesList;
    }

    /**
     * Sets the value of the sanctionsReferencesList property.
     * 
     * @param value
     *     allowed object is
     *     {@link SanctionsReferencesList }
     *     
     */
    public void setSanctionsReferencesList(SanctionsReferencesList value) {
        this.sanctionsReferencesList = value;
    }

    /**
     * Gets the value of the countryTypeList property.
     * 
     * @return
     *     possible object is
     *     {@link CountryTypeList }
     *     
     */
    public CountryTypeList getCountryTypeList() {
        return countryTypeList;
    }

    /**
     * Sets the value of the countryTypeList property.
     * 
     * @param value
     *     allowed object is
     *     {@link CountryTypeList }
     *     
     */
    public void setCountryTypeList(CountryTypeList value) {
        this.countryTypeList = value;
    }

    /**
     * Gets the value of the countryList property.
     * 
     * @return
     *     possible object is
     *     {@link CountryList }
     *     
     */
    public CountryList getCountryList() {
        return countryList;
    }

    /**
     * Sets the value of the countryList property.
     * 
     * @param value
     *     allowed object is
     *     {@link CountryList }
     *     
     */
    public void setCountryList(CountryList value) {
        this.countryList = value;
    }

    /**
     * Gets the value of the identificationTypeList property.
     * 
     * @return
     *     possible object is
     *     {@link IdentificationTypeList }
     *     
     */
    public IdentificationTypeList getIdentificationTypeList() {
        return identificationTypeList;
    }

    /**
     * Sets the value of the identificationTypeList property.
     * 
     * @param value
     *     allowed object is
     *     {@link IdentificationTypeList }
     *     
     */
    public void setIdentificationTypeList(IdentificationTypeList value) {
        this.identificationTypeList = value;
    }

    /**
     * Gets the value of the fdsTypeList property.
     * 
     * @return
     *     possible object is
     *     {@link FDSTypeList }
     *     
     */
    public FDSTypeList getFDSTypeList() {
        return fdsTypeList;
    }

    /**
     * Sets the value of the fdsTypeList property.
     * 
     * @param value
     *     allowed object is
     *     {@link FDSTypeList }
     *     
     */
    public void setFDSTypeList(FDSTypeList value) {
        this.fdsTypeList = value;
    }

    /**
     * Gets the value of the djiiIndustryList property.
     * 
     * @return
     *     possible object is
     *     {@link DJIIIndustryList }
     *     
     */
    public DJIIIndustryList getDJIIIndustryList() {
        return djiiIndustryList;
    }

    /**
     * Sets the value of the djiiIndustryList property.
     * 
     * @param value
     *     allowed object is
     *     {@link DJIIIndustryList }
     *     
     */
    public void setDJIIIndustryList(DJIIIndustryList value) {
        this.djiiIndustryList = value;
    }

    /**
     * Gets the value of the stakeLevel1List property.
     * 
     * @return
     *     possible object is
     *     {@link StakeLevel1List }
     *     
     */
    public StakeLevel1List getStakeLevel1List() {
        return stakeLevel1List;
    }

    /**
     * Sets the value of the stakeLevel1List property.
     * 
     * @param value
     *     allowed object is
     *     {@link StakeLevel1List }
     *     
     */
    public void setStakeLevel1List(StakeLevel1List value) {
        this.stakeLevel1List = value;
    }

    /**
     * Gets the value of the stakeLevel2List property.
     * 
     * @return
     *     possible object is
     *     {@link StakeLevel2List }
     *     
     */
    public StakeLevel2List getStakeLevel2List() {
        return stakeLevel2List;
    }

    /**
     * Sets the value of the stakeLevel2List property.
     * 
     * @param value
     *     allowed object is
     *     {@link StakeLevel2List }
     *     
     */
    public void setStakeLevel2List(StakeLevel2List value) {
        this.stakeLevel2List = value;
    }

    /**
     * Gets the value of the stakeLevel3List property.
     * 
     * @return
     *     possible object is
     *     {@link StakeLevel3List }
     *     
     */
    public StakeLevel3List getStakeLevel3List() {
        return stakeLevel3List;
    }

    /**
     * Sets the value of the stakeLevel3List property.
     * 
     * @param value
     *     allowed object is
     *     {@link StakeLevel3List }
     *     
     */
    public void setStakeLevel3List(StakeLevel3List value) {
        this.stakeLevel3List = value;
    }

    /**
     * Gets the value of the relationshipList property.
     * 
     * @return
     *     possible object is
     *     {@link RelationshipList }
     *     
     */
    public RelationshipList getRelationshipList() {
        return relationshipList;
    }

    /**
     * Sets the value of the relationshipList property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelationshipList }
     *     
     */
    public void setRelationshipList(RelationshipList value) {
        this.relationshipList = value;
    }

    /**
     * Gets the value of the records property.
     * 
     * @return
     *     possible object is
     *     {@link Records }
     *     
     */
    public Records getRecords() {
        return records;
    }

    /**
     * Sets the value of the records property.
     * 
     * @param value
     *     allowed object is
     *     {@link Records }
     *     
     */
    public void setRecords(Records value) {
        this.records = value;
    }

    /**
     * Gets the value of the associations property.
     * 
     * @return
     *     possible object is
     *     {@link Associations }
     *     
     */
    public Associations getAssociations() {
        return associations;
    }

    /**
     * Sets the value of the associations property.
     * 
     * @param value
     *     allowed object is
     *     {@link Associations }
     *     
     */
    public void setAssociations(Associations value) {
        this.associations = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
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
