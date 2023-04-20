package com.rdc.importer.misc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rdc.importer.misc.CodedEnum;

public enum PepType implements CodedEnum, Serializable {

    AMB("Ambassador"),
    ASC("Close Associate"),
    ASO("Associated Organization"),
    CAB("Cabinet or Government Agency Head"),
    FAM("Family Member"),
    GCO("Senior Executive or Influential Functionary in a Government Owned Enterprise"),
    HOS("Head of State"),
    IFO("International Financial, Economic, or Industry Organization Official"),
    IGO("International Governmental Organization Official"),
    INF("Senior Official In Government Infrastructure Division or Agency"),
    INT("Intelligence Services Official"),
    ISO("International Sporting Official"),
    JUD("Senior Official in Judicial Branch"),
    LAB("Labor Union Official"),
    LEG("Senior Official in Legislative Branch"),
    MIL("Senior Official in Military Branch"),
    MUN("Municipal Level Employee"),
    NGO("Non-Governmental Organization Official"),
    NIO("Senior Official In Non-Infrastructure Division or Agency"),
    PFA_PEP("PFA PEP"),
    PFA_RCA("PFA RDA"),
    PIO("Political Interest Group Official"),
    POL("Senior Official of Political Party"),
    PSO("Police Service Official"),
    REG("Regional Official"),
    REL("Religious Official"),
    WOR("High Net Worth Individual");

    private static List<PepType> rdcPeps = new ArrayList<PepType>();

    static {
        rdcPeps.addAll(Arrays.asList(values()));
        rdcPeps.remove(PFA_PEP);
        rdcPeps.remove(PFA_RCA);
        rdcPeps.remove(ASO);
    }

    static final long serialVersionUID = 0L;

    private String description;

    PepType(String description) {
        this.description = description;
    }

    public String getName() {
        return name();
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }

    public static PepType[] getRdcPepTypes() {
        return rdcPeps.toArray(new PepType[rdcPeps.size()]);
    }

    public static PepType getEnumByCode(String code) {
        for (PepType value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
