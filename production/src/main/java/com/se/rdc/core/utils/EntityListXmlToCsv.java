package com.se.rdc.core.utils;

import com.rdc.scrape.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Author: Omar 01/12/2014
 */

public class EntityListXmlToCsv {
    private LinkedHashMap<String, Integer> collumnTitleValueMap;
    private String outputFilePath;
    private List<ScrapeEntity> entitiesList;
    private String unicodeComma = "~";// + ((char) 8218); //&#8218; or \u201a; it's actually a low quotation

    public EntityListXmlToCsv(String outputFilePath,
                              List<ScrapeEntity> entities) {
        entitiesList = entities;
        this.outputFilePath = outputFilePath;
        this.collumnTitleValueMap = getCollumnTitleCount();
    }

    private LinkedHashMap<String, Integer> getCollumnTitleCount() {
        LinkedHashMap<String, Integer> valueMap = new LinkedHashMap<String, Integer>(
            28);
        valueMap.put("Name", 1);
        valueMap.put("Type", 1);
        valueMap.put("Source_Id", 1);
        valueMap.put("Address", 1);
        valueMap.put("Events", 1);
        valueMap.put("Date_Of_Birth", 1);
        valueMap.put("Sources", 1);
        valueMap.put("Url", 1);
        valueMap.put("Image_url", 1);
        valueMap.put("Aliases", 1);
        valueMap.put("Position", 1);
        valueMap.put("Languages", 1);
        valueMap.put("Citizenship", 1);
        valueMap.put("Nationality", 1);
        valueMap.put("Occupation", 1);
        valueMap.put("Height", 1);
        valueMap.put("Weight", 1);
        valueMap.put("Eye_color", 1);
        valueMap.put("Complexion", 1);
        valueMap.put("Hair_color", 1);
        valueMap.put("Scars_marks", 1);
        valueMap.put("Sex", 1);
        valueMap.put("Build", 1);
        valueMap.put("Race", 1);
        valueMap.put("Physical_description", 1);
        valueMap.put("Remark", 1);
        valueMap.put("Association", 1);
        valueMap.put("Identifications", 1);

        for (ScrapeEntity scrapeEntity : entitiesList) {
            countEntityValueTitles(scrapeEntity, valueMap);
        }

        return valueMap;
    }

    private String getTitles() {
        String result = "";
        StringBuffer buf = new StringBuffer();
        Set<Entry<String, Integer>> set = collumnTitleValueMap.entrySet();
        for (Entry<String, Integer> e : set) {
            int len = e.getValue();
            String key = e.getKey();

            if (len < 2) {
                //result += key + ",";
                buf.append(key).append(",");
            } else {
                int itr = 1;

                while (itr <= len) {
                    buf.append(key).append("_").append(itr).append(",");
                    //result += key + "_" + itr + ",";
                    itr++;
                }
            }
        }
        result = buf.toString();
        return result;
    }

    private void changeMapValue(LinkedHashMap<String, Integer> valueMap,
                                String key, Integer value) {
        int savedValue = valueMap.get(key);
        if (savedValue < value) {
            valueMap.put(key, value);
        }
    }

    private void countEntityValueTitles(ScrapeEntity scrapeEntity,
                                        LinkedHashMap<String, Integer> valueMap) {
        changeMapValue(valueMap, "Address", scrapeEntity.getAddresses().size());

        changeMapValue(valueMap, "Events", scrapeEntity.getEvents().size());

        changeMapValue(valueMap, "Date_Of_Birth",
            scrapeEntity.getDateOfBirths().size());

        changeMapValue(valueMap, "Sources", scrapeEntity.getSources().size());

        changeMapValue(valueMap, "Url", scrapeEntity.getUrls().size());

        changeMapValue(valueMap, "Image_url", scrapeEntity.getImageUrls().size());

        changeMapValue(valueMap, "Aliases", scrapeEntity.getAliases().size());

        changeMapValue(valueMap, "Position", scrapeEntity.getPositions().size());

        changeMapValue(valueMap, "Languages", scrapeEntity.getLanguages().size());

        changeMapValue(valueMap, "Citizenship",
            scrapeEntity.getCitizenships().size());

        changeMapValue(valueMap, "Nationality",
            scrapeEntity.getNationalities().size());

        changeMapValue(valueMap, "Occupation",
            scrapeEntity.getOccupations().size());

        changeMapValue(valueMap, "Height", scrapeEntity.getHeights().size());

        changeMapValue(valueMap, "Weight", scrapeEntity.getWeights().size());

        changeMapValue(valueMap, "Eye_color", scrapeEntity.getEyeColors().size());

        changeMapValue(valueMap, "Complexion",
            scrapeEntity.getComplexions().size());

        changeMapValue(valueMap, "Hair_color", scrapeEntity.getHairColors().size());

        changeMapValue(valueMap, "Scars_marks",
            scrapeEntity.getScarsMarks().size());

        changeMapValue(valueMap, "Sex", scrapeEntity.getSexes().size());

        changeMapValue(valueMap, "Build", scrapeEntity.getBuilds().size());

        changeMapValue(valueMap, "Race", scrapeEntity.getRaces().size());

        changeMapValue(valueMap, "Physical_description",
            scrapeEntity.getPhysicalDescriptions().size());

        changeMapValue(valueMap, "Remark", scrapeEntity.getRemarks().size());

        changeMapValue(valueMap, "Association",
            scrapeEntity.getAssociations().size());

        changeMapValue(valueMap, "Identifications",
            scrapeEntity.getIdentifications().size());
    }

    public void writeToCsv() throws IOException {
        String csvFilePath = outputFilePath + ".csv";
        File outputFile = new File(csvFilePath);
        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        }
        PrintWriter printWriter = new PrintWriter(outputFile, "UTF-8");

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        printWriter.write(getTitles() + "\n");

        for (ScrapeEntity scrapeEntity : entitiesList) {
            printWriter.write(getEntityString(scrapeEntity) + "\n");
        }

        printWriter.flush();
        printWriter.close();
    }

    private void addExtraValueToBuffer(StringBuffer xmlBuffer, String key,
                                       int currentSize) {
        int savedValue = collumnTitleValueMap.get(key);
        for (int i = currentSize; i < savedValue; i++) {
            xmlBuffer.append(",");
        }
    }

    /**
     * Based on: ScrapeEntity.toXml()
     */
    private String preserveComma(String name) {
        //    return name.replaceAll("(.*?,.*)", "\"$1\"");
        return name.replaceAll(",", unicodeComma);
    }

    private void addGroupNode(StringBuffer nodeBuffer, String name, Object val,
                              boolean isSecondPart, int secondPartMaxSize) {

        if (val != null && !val.equals("")) {

            String delim = isSecondPart ? "\n" : "";

            //remove double quote as it is used in main cell block to
            String value = preserveComma(val.toString()).replaceAll("\"", "");

            if (isSecondPart && value.length() < secondPartMaxSize) {
                StringBuffer spaceBuffer = new StringBuffer(value);
                for (int i = value.length(); i <= secondPartMaxSize; i++) {
                    spaceBuffer.append(" ");
                }
                value = spaceBuffer.toString();
            }

            nodeBuffer.append(delim + " | " + name + ": " + value);
        }
    }

    private String getEntityString(ScrapeEntity scrapeEntity) {
        StringBuffer xmlBuffer = new StringBuffer();

        String name = sanitize(preserveComma(scrapeEntity.getName()));
        xmlBuffer.append(name);
        xmlBuffer.append(",");

        String type = scrapeEntity.getType();
        if (type != null) {
            xmlBuffer.append(type);
        }
        xmlBuffer.append(",");

        String dataSourceId = scrapeEntity.getDataSourceId();
        if (dataSourceId != null) {
            xmlBuffer.append(dataSourceId);
        }
        xmlBuffer.append(",");

        List<ScrapeAddress> addresses = scrapeEntity.getAddresses();
        for (ScrapeAddress address : addresses) {
            StringBuffer addressNode = new StringBuffer("\"");

            addGroupNode(addressNode, "raw_format", address.getRawFormat(), false, 0);
            addGroupNode(addressNode, "address1", address.getAddress1(), false, 0);
            addGroupNode(addressNode, "city", address.getCity(), true, 20);
            addGroupNode(addressNode, "province", address.getProvince(), true, 20);
            addGroupNode(addressNode, "postal_code", address.getPostalCode(), true,
                20);
            addGroupNode(addressNode, "country", address.getCountry(), true, 20);
            addGroupNode(addressNode, "birth_place", address.getBirthPlace(), true,
                20);

            String nodeStr = addressNode.toString();

            if (!nodeStr.matches(
                "(?s)^[\"|\\s]+(?:raw_format|address1).*+$")) { //only 2nd part found
                nodeStr = nodeStr.replaceAll("\\n", "");
            } else {
                nodeStr = nodeStr.replaceFirst("\\n\\s*\\|\\s*", "__").replaceAll("\\n",
                    "").replaceAll("__", "\n");
            }
            nodeStr = nodeStr.replaceFirst("\\s*\\|\\s*", "").trim() + "\"";

            xmlBuffer.append(nodeStr + ",");
        }
        addExtraValueToBuffer(xmlBuffer, "Address",
            scrapeEntity.getAddresses().size());

        List<ScrapeEvent> events = scrapeEntity.getEvents();
        for (ScrapeEvent event : events) {
            StringBuffer eventNode = new StringBuffer();

            addNode(eventNode, "description", event.getDescription());
            addNode(eventNode, "date", event.getDate());
            addNode(eventNode, "end_date", event.getEndDate());
            addNode(eventNode, "category", event.getCategory());
            addNode(eventNode, "subcategory", event.getSubcategory());
            addNode(eventNode, "eventtags", event.getEventtags());

            xmlBuffer.append(eventNode.toString() + ",");
        }
        addExtraValueToBuffer(xmlBuffer, "Events", scrapeEntity.getEvents().size());

        List<ScrapeDob> dateOfBirths = scrapeEntity.getDateOfBirths();
        for (ScrapeDob dob : dateOfBirths) {
            StringBuffer dobNode = new StringBuffer();

            addNode(dobNode, "year", dob.getYear());
            addNode(dobNode, "month", dob.getMonth());
            addNode(dobNode, "day", dob.getDay());
            addNode(dobNode, "circa", dob.getCirca());

            xmlBuffer.append(dobNode.toString() + ",");
        }
        addExtraValueToBuffer(xmlBuffer, "Date_Of_Birth",
            scrapeEntity.getDateOfBirths().size());

        List<ScrapeSource> sources = scrapeEntity.getSources();
        for (ScrapeSource source : sources) {
            StringBuffer sourceNode = new StringBuffer();

            addNode(sourceNode, "name", source.getName());
            addNode(sourceNode, "url", source.getUrl());
            addNode(sourceNode, "description", source.getDescription());

            xmlBuffer.append(sourceNode.toString() + ",");
        }
        addExtraValueToBuffer(xmlBuffer, "Sources",
            scrapeEntity.getSources().size());

        appendXmlNodes(xmlBuffer, "url", scrapeEntity.getUrls());
        addExtraValueToBuffer(xmlBuffer, "Url", scrapeEntity.getUrls().size());

        appendXmlNodes(xmlBuffer, "image_url", scrapeEntity.getImageUrls());
        addExtraValueToBuffer(xmlBuffer, "Image_url",
            scrapeEntity.getImageUrls().size());

        appendXmlNodes(xmlBuffer, "alias", scrapeEntity.getAliases());
        addExtraValueToBuffer(xmlBuffer, "Aliases",
            scrapeEntity.getAliases().size());

        appendXmlNodes(xmlBuffer, "position", scrapeEntity.getPositions());
        addExtraValueToBuffer(xmlBuffer, "Position",
            scrapeEntity.getPositions().size());

        appendXmlNodes(xmlBuffer, "language", scrapeEntity.getLanguages());
        addExtraValueToBuffer(xmlBuffer, "Languages",
            scrapeEntity.getLanguages().size());

        appendXmlNodes(xmlBuffer, "citizenship", scrapeEntity.getCitizenships());
        addExtraValueToBuffer(xmlBuffer, "Citizenship",
            scrapeEntity.getCitizenships().size());

        appendXmlNodes(xmlBuffer, "nationality", scrapeEntity.getNationalities());
        addExtraValueToBuffer(xmlBuffer, "Nationality",
            scrapeEntity.getNationalities().size());

        appendXmlNodes(xmlBuffer, "occupation", scrapeEntity.getOccupations());
        addExtraValueToBuffer(xmlBuffer, "Occupation",
            scrapeEntity.getOccupations().size());

        appendXmlNodes(xmlBuffer, "height", scrapeEntity.getHeights());
        addExtraValueToBuffer(xmlBuffer, "Height",
            scrapeEntity.getHeights().size());

        appendXmlNodes(xmlBuffer, "weight", scrapeEntity.getWeights());
        addExtraValueToBuffer(xmlBuffer, "Weight",
            scrapeEntity.getWeights().size());

        appendXmlNodes(xmlBuffer, "eye_color", scrapeEntity.getEyeColors());
        addExtraValueToBuffer(xmlBuffer, "Eye_color",
            scrapeEntity.getEyeColors().size());

        appendXmlNodes(xmlBuffer, "complexion", scrapeEntity.getComplexions());
        addExtraValueToBuffer(xmlBuffer, "Complexion",
            scrapeEntity.getComplexions().size());

        appendXmlNodes(xmlBuffer, "hair_color", scrapeEntity.getHairColors());
        addExtraValueToBuffer(xmlBuffer, "Hair_color",
            scrapeEntity.getHairColors().size());

        appendXmlNodes(xmlBuffer, "scars_marks", scrapeEntity.getScarsMarks());
        addExtraValueToBuffer(xmlBuffer, "Scars_marks",
            scrapeEntity.getScarsMarks().size());

        appendXmlNodes(xmlBuffer, "sex", scrapeEntity.getSexes());
        addExtraValueToBuffer(xmlBuffer, "Sex", scrapeEntity.getSexes().size());

        appendXmlNodes(xmlBuffer, "build", scrapeEntity.getBuilds());
        addExtraValueToBuffer(xmlBuffer, "Build", scrapeEntity.getBuilds().size());

        appendXmlNodes(xmlBuffer, "race", scrapeEntity.getRaces());
        addExtraValueToBuffer(xmlBuffer, "Race", scrapeEntity.getRaces().size());

        appendXmlNodes(xmlBuffer, "physical_description",
            scrapeEntity.getPhysicalDescriptions());
        addExtraValueToBuffer(xmlBuffer, "Physical_description",
            scrapeEntity.getPhysicalDescriptions().size());

        appendXmlNodes(xmlBuffer, "remark", scrapeEntity.getRemarks());
        addExtraValueToBuffer(xmlBuffer, "Remark",
            scrapeEntity.getRemarks().size());

        appendXmlNodes(xmlBuffer, "association", scrapeEntity.getAssociations());
        addExtraValueToBuffer(xmlBuffer, "Association",
            scrapeEntity.getAssociations().size());

        Set<ScrapeIdentification> identifications = scrapeEntity.getIdentifications();
        for (ScrapeIdentification identification : identifications) {
            StringBuffer idNode = new StringBuffer();

            addNode(idNode, "type", identification.getType());
            addNode(idNode, "location", identification.getLocation());
            addNode(idNode, "country", identification.getCountry());
            addNode(idNode, "value", identification.getValue());

            xmlBuffer.append(idNode.toString() + ",");
        }
        addExtraValueToBuffer(xmlBuffer, "Identifications",
            scrapeEntity.getIdentifications().size());

        return xmlBuffer.toString().replaceAll("(?s)(?>;\\s*,|,\\s*[\\|-])",
            ",").replaceAll("(?s)(?>;\\s*-)", "; ");
    }

    private void addNode(StringBuffer nodeBuffer, String name, Boolean bool) {
        addNode(nodeBuffer, name, bool != null ? bool.toString() : null);
    }

    private void appendXmlNodes(StringBuffer nodeBuffer, String string,
                                Set<?> values) {
        int size = 0;
        if (values != null) {
            size = values.size();
        }

        if (values != null && size > 0) {
            if (size > 1) {
                for (Object value : values) {
                    nodeBuffer.append(sanitize(preserveComma(value.toString())));
                    nodeBuffer.append(",");
                }
            } else {
                Object val = values.toArray()[0];

                if (val != null) {
                    nodeBuffer.append(
                        sanitize(preserveComma(values.toArray()[0].toString())));
                    nodeBuffer.append(",");
                }
            }
        }
    }

    private void addNode(StringBuffer nodeBuffer, String name, String value) {
        if (value != null && !value.equals("")) {
            nodeBuffer.append(" - " + name + ": " + sanitize(preserveComma(value)));
        }
    }

    private String sanitize(String value) {
        value = value.replaceAll("\"", "'");

        return value;
    }
}
