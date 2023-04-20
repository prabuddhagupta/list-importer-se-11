package com.rdc.importer.scrapian.transform;

import org.apache.commons.lang.StringEscapeUtils;
import com.rdc.importer.misc.AsciiUtils;
import com.rdc.importer.misc.ValidationException;

public final class XmlTranformUtils {
    public static void addNode(StringBuffer buffer, String nodeName, String value, boolean validate, boolean escape) {
        if (value == null) {
            value = "";
        }
        if (escape) {
            value = StringEscapeUtils.escapeXml(value);
        }

        if (validate) {
            try {
                value = AsciiUtils.convertAndValidateCharacters(value.trim());
            } catch (ValidationException e) {
                e.printStackTrace();
                value = "";
            }
        }
        buffer.append("<").append(nodeName).append(">").append(value).append("</").append(nodeName).append(">");
    }

    public static String flattenNodeName(String node) {
        return node.toLowerCase().replaceAll("[-|\\s]", "_").replaceAll("\\W", "");
    }
}
