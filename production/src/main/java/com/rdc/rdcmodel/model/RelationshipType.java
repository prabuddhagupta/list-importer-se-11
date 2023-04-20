package com.rdc.rdcmodel.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//import com.rdc.core.model.CodedEnum;
//import com.rdc.importer.misc.CodedEnum;


public enum RelationshipType implements /*CodedEnum,*/ Serializable {

    ASSOCIATE("Associate"),
    AGENT_REPRESENTATIVE("Agent/Representative"),
    AUNT("Aunt"),
    BROTHER("Brother"),
    BROTHER_IN_LAW("Brother-in-law"),
    CHILD("Child"),
    COUSIN("Cousin"),
    COLLEAGUE("Colleague"),
    DAUGHTER("Daughter"),
    DAUGHTER_IN_LAW("Daughter-in-law"),
    EMPLOYEE("Employee"),
    EMPLOYER("Employer"),
    FAMILY_MEMBER("Family Member"),
    FATHER("Father"),
    FATHER_IN_LAW("Father-in-law"),
    FINANCIAL_ADVISER("Financial Adviser"),
    FRIEND("Friend"),
    GRANDDAUGHTER("Granddaughter"),
    GRANDFATHER("Grandfather"),
    GRANDMOTHER("Grandmother"),
    GRANDSON("Grandson"),
    HUSBAND("Husband"),
    LEGAL_ADVISER("Legal Adviser"),
    MOTHER("Mother"),
    MOTHER_IN_LAW("Mother-in-law"),
    NEPHEW("Nephew"),
    NIECE("Niece"),
    POLITICAL_ADVISER("Political Adviser"),
    SISTER("Sister"),
    SISTER_IN_LAW("Sister-in-law"),
    SON("Son"),
    SON_IN_LAW("Son-in-law"),
    STEP_DAUGHTER("Step-Daughter"),
    STEP_SON("Step-Son"),
    STEP_FATHER("Step-father"),
    STEP_MOTHER("Step-mother"),
    UNCLE("Uncle"),
    UNMARRIED_PARTNER("Unmarried Partner"),
    WIFE("Wife"),
    NEGATIVE_NEWS("Negative News"),
    ASC_SIP("Associated Special Interest Person"),
    SAME_SEX_PARTNER("Same-sex Spouse"),
    SENIOR_OFFICIAL("Senior Official"),
    SHAREHOLDER_OWNER("Shareholder/Owner"),
    PARENT_COMPANY("Parent Company"),
    SUBSIDIARY("Subsidiary"),
    ASSET("Asset");
    
    

    static final long serialVersionUID = 0L;

    private String description;

    private static List<RelationshipType> family = new ArrayList<RelationshipType>();

    static {
        family.add(AUNT);
        family.add(BROTHER);
        family.add(BROTHER_IN_LAW);
        family.add(CHILD);
        family.add(COUSIN);
        family.add(DAUGHTER);
        family.add(DAUGHTER_IN_LAW);
        family.add(FAMILY_MEMBER);
        family.add(FATHER);
        family.add(FATHER_IN_LAW);
        family.add(GRANDDAUGHTER);
        family.add(GRANDFATHER);
        family.add(GRANDMOTHER);
        family.add(GRANDSON);
        family.add(HUSBAND);
        family.add(MOTHER);
        family.add(MOTHER_IN_LAW);
        family.add(NEPHEW);
        family.add(NIECE);
        family.add(SISTER);
        family.add(SISTER_IN_LAW);
        family.add(SON);
        family.add(SON_IN_LAW);
        family.add(STEP_DAUGHTER);
        family.add(STEP_SON);
        family.add(STEP_FATHER);
        family.add(STEP_MOTHER);
        family.add(UNCLE);
        family.add(WIFE);
    }

    private static List<RelationshipType> closeAssociates = new ArrayList<RelationshipType>();

    static {
        closeAssociates.add(AGENT_REPRESENTATIVE);
        closeAssociates.add(ASSOCIATE);
        closeAssociates.add(COLLEAGUE);
        closeAssociates.add(EMPLOYEE);
        closeAssociates.add(EMPLOYER);
        closeAssociates.add(FINANCIAL_ADVISER);
        closeAssociates.add(FRIEND);
        closeAssociates.add(LEGAL_ADVISER);
        closeAssociates.add(POLITICAL_ADVISER);
        closeAssociates.add(UNMARRIED_PARTNER);
    }

    private static List<RelationshipType> gco = new ArrayList<RelationshipType>();

    static {
        gco.add(EMPLOYEE);
        gco.add(ASSOCIATE);
    }

    private static List<RelationshipType> associated = new ArrayList<RelationshipType>();

    static {
    	associated.add(ASSOCIATE);
    }

    RelationshipType(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }

    public static List<RelationshipType> getFamilyRelationships() {
        return family;
    }

    public static List<RelationshipType> getCloseAssociateRelationships() {
        return closeAssociates;
    }

    public static List<RelationshipType> getGcoRelationships() {
        return gco;
    }

    public static List<RelationshipType> getAssociatedRelationships() {
        return associated;
    }

    public static RelationshipType getEnumByCode(String code) {
        for (RelationshipType value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
