package com.rdc.importer.misc;

import java.util.ArrayList;
import java.util.List;

import com.rdc.importer.misc.PepType;

public enum MonitoredListRecordDataField {
    BIRTHDATE("Date of Birth", null, null),
    NATIONALITY("Nationality", 500, null),
    CITIZENSHIP("Citizenship", 50, null),
    ALIAS("Alias", 500, null),
    OCCUPATION("Occupation", 500, null),
    POSITION("Position", 500, null),
    LANGUAGE("Language", 200, null),
    IMAGE_URL("Image URL", 2500, null),
    ENTITY_URL("Entity URL", 2500, null),
    ASSOCIATION("Association", 200, null),
    EVENT("Event", null, null),
    PHYSICAL_DESCRIPTION("Physical Description", 500, null),
    HAIR_COLOR("Hair Color", 200, null),
    EYE_COLOR("Eye Color", 200, null),
    HEIGHT("Height", 200, null),
    WEIGHT("Weight", 200, null),
    COMPLEXION("Complexion", 200, null),
    SEX("Sex", 1, new String[]{"M:Male", "F:Female"}),
    BUILD("Build", 200, null),
    RACE("Race", 200, null),
    SCARS_MARKS("Scars/Marks", 500, null),
    DECEASED("Deceased", null, null),
    IDENTIFICATION("Identification", null, null),
    PEP_TYPE("PEP Type", 3, pepTypes()),
    PEP_SCORE("PEP Score", 20, null),
    RELATIONSHIP("Relationship", null, null),
    NEGATIVE_NEWS("Negative News", 15, null),
    ADDRESS("Address", null, null),
    REMARKS("Remark", 2000, null),
    SOURCE("Source", null, null);

    private static String[] pepTypes() {
        List<String> pepTypes = new ArrayList<String>();
        for (PepType pepType : PepType.getRdcPepTypes()) {
            pepTypes.add(pepType.getCode() + ":" + pepType.getCode() + " - " + pepType.getDescription());
        }
        return pepTypes.toArray(new String[pepTypes.size()]);
    }

    private Integer fieldLength;
    private String fieldLabel;
    private String[] possibleValues;

    MonitoredListRecordDataField(String fieldLabel, Integer fieldLength, String[] possibleValues) {
        this.fieldLabel = fieldLabel;
        this.fieldLength = fieldLength;
        this.possibleValues = possibleValues;
    }

    public String getName() {
        return name();
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public Integer getFieldLength() {
        return fieldLength;
    }

    public String[] getPossibleValues() {
        return possibleValues;
    }

    public static MonitoredListRecordDataField getEnum(String value) {
        return Enum.valueOf(MonitoredListRecordDataField.class, value);
    }
}
