package com.rdc.importer.scrapian.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class StringSource implements ScrapianSource {
    private String value;

    public StringSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String serialize() {
        return value;
    }

    public String toString() {
        return serialize();
    }

    public StringSource stripXmlTags() {
        strip("<[^>]*>");
        return this;
    }

    public StringSource stripXmlAttributes() {
        return replace("(<[\\w]+)([^>]*)", "$1");
    }

    public StringSource stripHtmlTags() {
        return stripXmlTags();
    }

    public StringSource stripHtmlAttributes() {
        return stripXmlAttributes();
    }

    public StringSource stripSpaces() {
        strip("\\s");
        return this;
    }

    public StringSource stripFormatting() {
        strip("\\n|\\r|\\t");
        return this;
    }

    public StringSource convertNbsp() {
        replace("&nbsp;", " ");
        replace("&#160;", " ");
        return this;
    }

    public StringSource stripEntities() {
        return stripEntities(null);
    }
    public StringSource stripEntities(String regEx) {
        if(regEx == null){
            regEx = "&((.|^;)*?);";
        }
        strip(regEx);
        return this;
    }

    public StringSource stripPunctuation() {
        if (value != null) {
            String result = value.trim();
            String[] punctuations = {",", ".", "?", "!", ";"};

            for (String punctuation : punctuations) {
                if (result.startsWith(punctuation)) {
                    result = result.substring(1);
                }

                if (result.endsWith(punctuation)) {
                    result = result.substring(0, result.length() - 1);
                }
            }
            value = result;
        }

        return this;
    }

    private void strip(String regexp) {
        if (value != null) {
            value = value.replaceAll(regexp, "");
        }
    }

    public StringSource trim() {
        if (value != null) {
            value = value.trim();
        }
        return this;
    }

    public StringSource replace(String regexp, String replacement) {
        if (value != null) {
            value = value.replaceAll(regexp, replacement);
        }
        return this;
    }


    public boolean isBlank() {
        return StringUtils.isBlank(value);
    }

    public boolean matches(String regexp) {
        return value != null && value.matches(regexp);
    }

    public ListSource<StringSource> split(String delim) {
        List<StringSource> ret = new ArrayList<StringSource>();
        for (String s : value.split(delim)) {
            ret.add(new StringSource(s));
        }
        return new ListSource<StringSource>(ret);
    }

    public int hashCode() {
        String value = toString();
        if (value != null) {
            return value.hashCode();
        }
        return super.hashCode();
    }

    public boolean equals(Object that) {
        if (that instanceof StringSource) {
            String thisToString = this.toString();
            String thatToString = that.toString();
            return thisToString != null && thatToString != null && thisToString.equals(thatToString);
        }
        return false;
    }
}
