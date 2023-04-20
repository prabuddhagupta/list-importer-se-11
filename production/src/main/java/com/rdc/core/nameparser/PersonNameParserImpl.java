// Copyright © 2008, 2009, 2010, 2014, 2016 Regulatory DataCorp, Inc. (RDC)

package com.rdc.core.nameparser;

import org.apache.commons.lang.StringUtils;

public class PersonNameParserImpl implements PersonNameParser {
    private static final String SPLIT_REGEX = "[ ]+";
    private static final String prefixes = "Bish,Br,Ch,Dr,Fr,Mr,Miss,Mrs,Ms,Prof,Rev,Sr";
    private static final String suffixes = "Jr,Sr,II,III,IV";

    public PersonName parseName(String input) {
        PersonName personName = new PersonName();
        String name = preprocessForNameCommaSuffix(input, personName);

        if (isCommaSeparatedName(name)) {
            int index = name.indexOf(",");

            // Parse lastname
            String[] tokens = name.substring(0, index).trim().split(SPLIT_REGEX);
            String[] filterTokens = filterPrefixesAndSuffixes(tokens, personName);

            String lastname = "";
            for (String filterToken : filterTokens) {
                lastname += filterToken + " ";
            }
            personName.setLastname(lastname.trim());

            // Parse rest of name
            tokens = name.substring(index + 1).trim().trim().split(SPLIT_REGEX);
            filterTokens = filterPrefixesAndSuffixes(tokens, personName);
            if (filterTokens.length == 1) {
                personName.setFirstname(filterTokens[0]);
            } else if (filterTokens.length > 1) {
                String firstname = "";
                for (int i = 0; i < filterTokens.length - 1; i++) {
                    firstname += filterTokens[i] + " ";
                }
                personName.setFirstname(firstname.trim());
                personName.setMiddlename(filterTokens[filterTokens.length - 1]);
            }
        } else {
            String[] tokens = name.split(SPLIT_REGEX);
            String[] filterTokens = filterPrefixesAndSuffixes(tokens, personName);

            if (filterTokens.length == 1) {
                personName.setLastname(filterTokens[0]);
            } else if (filterTokens.length == 2) {
                if (filterTokens[0].endsWith(",")) {
                    personName.setFirstname(filterTokens[1]);
                    personName.setLastname(filterTokens[0].substring(0, filterTokens[0].length() - 1));
                } else {
                    personName.setFirstname(filterTokens[0]);
                    personName.setLastname(filterTokens[1]);
                }
            } else if (filterTokens.length == 3) {
                if (filterTokens[0].endsWith(",")) {
                    personName.setFirstname(filterTokens[1]);
                    personName.setMiddlename(filterTokens[2]);
                    personName.setLastname(filterTokens[0].substring(0, filterTokens[0].length() - 1));
                } else {
                    personName.setFirstname(filterTokens[0]);
                    personName.setMiddlename(filterTokens[1]);
                    personName.setLastname(filterTokens[2]);
                }
            } else if (filterTokens.length > 3) {
                personName.setFirstname(filterTokens[0]);
                personName.setMiddlename(filterTokens[1]);
                StringBuffer lastname = new StringBuffer();
                for (int i = 2; i < filterTokens.length; i++) {
                    lastname.append(filterTokens[i]).append(" ");
                }
                personName.setLastname(lastname.toString().trim());
            }
        }

        return personName;
    }

    private boolean isCommaSeparatedName(String name) {
        return name.indexOf(",") != -1;
    }

    private String preprocessForNameCommaSuffix(String input, PersonName personName) {
        String[] tokens = filterPrefixesAndSuffixes(input.replaceAll(",", " , ").trim().split(SPLIT_REGEX), personName);
        StringBuffer resultName = new StringBuffer();
        for (int i = 0; i < tokens.length; i++) {
            String namePiece = tokens[i];
            if (i != tokens.length - 1 || (i == tokens.length - 1 && !namePiece.trim().equalsIgnoreCase(","))) {
                resultName.append(namePiece).append(" ");
            }
        }

        return resultName.toString();
    }

    private String[] filterPrefixesAndSuffixes(String[] tokens, PersonName personName) {
        return filterSuffixes(filterPrefixes(tokens, personName), personName);
    }

    private String[] filterPrefixes(String[] tokens, PersonName personName) {
        boolean matchFound = false;
        if (tokens.length > 0) {
            for (String value : prefixes.split(",")) {
                if (tokens[0].equalsIgnoreCase(value) || tokens[0].equalsIgnoreCase(value + ".")) {
                    matchFound = true;
                }
            }
        }

        if (matchFound) {
            personName.setPrefix(tokens[0]);
            String[] result = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, result, 0, tokens.length - 1);
            return result;
        } else {
            return tokens;
        }
    }

    private String[] filterSuffixes(String[] tokens, PersonName personName) {
        boolean matchFound = false;
        if (tokens.length > 0) {
            for (String value : suffixes.split(",")) {
                if (tokens[tokens.length - 1].equalsIgnoreCase(value) || tokens[tokens.length - 1].equalsIgnoreCase(value + ".")) {
                    matchFound = true;
                }
            }
        }

        if (matchFound) {
            personName.setSuffix(tokens[tokens.length - 1]);
            String[] result = new String[tokens.length - 1];
            System.arraycopy(tokens, 0, result, 0, tokens.length - 1);
            return result;
        } else {
            return tokens;
        }
    }

    public String formatName(String... namePieces) {
        StringBuffer sb = new StringBuffer();
        for (String namePiece : namePieces) {
            if (namePiece != null && namePiece.trim().length() > 0) {
                sb.append(namePiece).append(" ");
            }
        }
        return formatName(parseName(sb.toString().trim()));
    }

    public String formatName(PersonName personName) {
        StringBuffer result = new StringBuffer();

        if (!StringUtils.isEmpty(personName.getPrefix())) {
            result.append(personName.getPrefix()).append(" ");
        }
        if (!StringUtils.isEmpty(personName.getFirstname())) {
            result.append(personName.getFirstname()).append(" ");
        }
        if (!StringUtils.isEmpty(personName.getMiddlename())) {
            result.append(personName.getMiddlename()).append(" ");
        }
        if (!StringUtils.isEmpty(personName.getLastname())) {
            result.append(personName.getLastname()).append(" ");
        }
        if (!StringUtils.isEmpty(personName.getSuffix())) {
            result.append(personName.getSuffix()).append(" ");
        }

        if (result.length() > 0) return result.substring(0, result.length() - 1);
        return "";
    }

}

