package com.rdc.importer.scrapian.model;

import java.util.Date;

//import com.rdc.wss.model.type.BatchType;
//import com.rdc.wss.model.type.PortfolioNameType;
//import com.rdc.wss.model.type.PfStatusType;

public class PortfolioEntry {
    private Integer bizUnitId;
    private String firmNumber;
    private Integer inquiryId;
    private Integer batchId;
    private Integer batchSequenceNumber;
    private Date recordInDate;
    private Boolean portfolioMonitoring;
    private String customerReportingId;
    private String customerTrackingId;
    private String nameType;
    private String inquiryName;
    private String lastName;
    private String firstName;
    private String middleName;
    private String businessName;
    private String inquiryNameU;
    private String lastNameU;
    private String firstNameU;
    private String middleNameU;
    private String businessNameU;
    private String deliveryLine;
    private String city;
    private String postalCode;
    private String province;
    private String provinceCode;
    private String country;
    private String countryCode;
    private Date dateOfBirth;
    private Integer age;
    private String userName;
    //private BatchType accountType;
    private Date submittedDate;
    private String globalSearch;
    private String identifier;
    private String inquiryNotes;
    private Boolean lookForward;
    //private PfStatusType pfStatus;    

//    public PfStatusType getPfStatus() {
//        return pfStatus;
//    }
//
//    public void setPfStatus(PfStatusType pfStatus) {
//        this.pfStatus = pfStatus;
//    }

    public String getFirmNumber() {
        return firmNumber;
    }

    public void setFirmNumber(String firmNumber) {
        this.firmNumber = firmNumber;
    }

//    public BatchType getAccountType() {
//        return accountType;
//    }
//
//    public void setAccountType(BatchType accountType) {
//        this.accountType = accountType;
//    }

    public String getFormattedInquiryName() {
      if (inquiryNameU != null && inquiryNameU.trim().length() > 0) {
        return inquiryNameU;
      }
      else {
        return inquiryName;
      }
    }

    public Integer getBizUnitId() {
        return bizUnitId;
    }

    public void setBizUnitId(Integer bizUnitId) {
        this.bizUnitId = bizUnitId;
    }

    public Integer getInquiryId() {
        return inquiryId;
    }

    public void setInquiryId(Integer inquiryId) {
        this.inquiryId = inquiryId;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public Date getRecordInDate() {
        return recordInDate;
    }

    public void setRecordInDate(Date recordInDate) {
        this.recordInDate = recordInDate;
    }

    public Boolean getPortfolioMonitoring() {
        return portfolioMonitoring;
    }

    public void setPortfolioMonitoring(Boolean portfolioMonitoring) {
        this.portfolioMonitoring = portfolioMonitoring;
    }

    public String getCustomerReportingId() {
        return customerReportingId;
    }

    public void setCustomerReportingId(String customerReportingId) {
        this.customerReportingId = customerReportingId;
    }

    public String getCustomerTrackingId() {
        return customerTrackingId;
    }

    public void setCustomerTrackingId(String customerTrackingId) {
        this.customerTrackingId = customerTrackingId;
    }

    public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }

    public String getInquiryName() {
        return inquiryName;
    }

    public void setInquiryName(String inquiryName) {
        this.inquiryName = inquiryName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }


    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getDeliveryLine() {
        return deliveryLine;
    }

    public void setDeliveryLine(String deliveryLine) {
        this.deliveryLine = deliveryLine;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getBatchSequenceNumber() {
        return batchSequenceNumber;
    }

    public Date getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    public void setBatchSequenceNumber(Integer batchSequenceNumber) {
        this.batchSequenceNumber = batchSequenceNumber;
    }

    public String getGlobalSearch() {
        return globalSearch;
    }

    public void setGlobalSearch(String globalSearch) {
        this.globalSearch = globalSearch;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setLookForward(Boolean lookForward) {
        this.lookForward = lookForward;
    }

    public Boolean getLookForward() {
        return lookForward;
    }

    public String getInquiryNotes() {
        return inquiryNotes;
    }

    public void setInquiryNotes(String inquiryNotes) {
        this.inquiryNotes = inquiryNotes;
    }

    public String getInquiryNameU() {
      return inquiryNameU;
    }

    public void setInquiryNameU(String inquiryNameU) {
      this.inquiryNameU = inquiryNameU;
    }

    public String getLastNameU() {
      return lastNameU;
    }

    public void setLastNameU(String lastNameU) {
      this.lastNameU = lastNameU;
    }

    public String getFirstNameU() {
      return firstNameU;
    }

    public void setFirstNameU(String firstNameU) {
      this.firstNameU = firstNameU;
    }

    public String getMiddleNameU() {
      return middleNameU;
    }

    public void setMiddleNameU(String middleNameU) {
      this.middleNameU = middleNameU;
    }

    public String getBusinessNameU() {
      return businessNameU;
    }

    public void setBusinessNameU(String businessNameU) {
      this.businessNameU = businessNameU;
    }
    
}
