package com.rdc.rdcmodel.model;

import java.io.Serializable;

public enum AliasType implements Serializable {

    AKA("Also known as"), 
    FKA("Formerly known as"), 
    DBA("Doing business as"),
    MDN("Maiden Name"), 
    SPV("Spelling Variation"),
    LOC("Local Script");

    static final long serialVersionUID = 0L;

    private final String description;

    AliasType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return name();
    }

    public static AliasType getEnumByCode(String code) {
        for (AliasType aliasType : AliasType.values()) {
            if (aliasType.getCode().equals(code)) {
                return aliasType;
            }
        }
        throw new RuntimeException("[" + code + "] is not a valid AliasType Code.");
    }
}