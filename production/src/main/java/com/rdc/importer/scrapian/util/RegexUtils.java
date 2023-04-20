package com.rdc.importer.scrapian.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
    private static Map<String, Pattern> patternCache = Collections.synchronizedMap(new HashMap<String, Pattern>());

    private static Pattern getPattern(String regex) {
        Pattern pattern = patternCache.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
            patternCache.put(regex, pattern);
        }
        return pattern;
    }

    public static Matcher getMatcher(String regex, String input) {
        Pattern pattern = getPattern(regex);
        return pattern.matcher(input);
    }

    public static boolean containsMatch(String regex, String input) {
        return input != null && getMatcher(regex, input).find();
    }

    public static List<List<String>> extractMatches(String regex, String input) {
        List<List<String>> matchGroups = new ArrayList<List<String>>();
        if (input != null) {
            Matcher matcher = getMatcher(regex, input);
            while (matcher.find()) {
                List<String> values = new ArrayList<String>();
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    values.add(matcher.group(i));
                }
                if (!values.isEmpty()) {
                    matchGroups.add(values);
                }
            }
        }
        return matchGroups;
    }

    public static String replaceMultiLineAll(String input, String regex, String replace) {
        Pattern pattern = getPattern(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll(replace);
    }

    public static String escapeRegex(String input) {
        input = input.replaceAll("\\\\", "\\\\\\\\");
        input = input.replaceAll("\\^", "\\\\^");
        input = input.replaceAll("\\[", "\\\\[");
        input = input.replaceAll("\\.", "\\\\.");
        input = input.replaceAll("\\|", "\\\\|");
        input = input.replaceAll("\\?", "\\\\?");
        input = input.replaceAll("\\*", "\\\\*");
        input = input.replaceAll("\\+", "\\\\+");
        input = input.replaceAll("\\(", "\\\\(");
        input = input.replaceAll("\\)", "\\\\)");
        input = input.replaceAll("\\s", "\\\\s");
        input = input.replaceAll("\\$", "\\\\\\$");
        return input;
    }
}
