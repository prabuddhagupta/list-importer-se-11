package com.rdc.importer.scrapian.util;

import org.apache.commons.lang.StringUtils;

public class AsciiUtils {

	private AsciiUtils() {

	}

	public static String convertAndValidateAsciiCharacters(String theString)
			throws Exception {
		return convertAndValidateAsciiCharacters(theString, null);
	}

	public static String convertAndValidateAsciiCharacters(String theString,
			String paramName) throws Exception {
		return convertAndValidateAsciiCharacters(theString, paramName, null);
	}

	public static String convertAndValidateAsciiCharacters(String theString,
			String paramName, String replace) throws Exception {
		if (theString == null) {
			return null;
		}
		String convertedString = swapMsCharacters(theString);
		int size = convertedString.length();
		if (StringUtils.isNotBlank(replace)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < size; i++) {
				if (((convertedString.charAt(
						i) & 0xff00) == 0 || (int) convertedString.charAt(i) == 8364)) {
					sb.append(convertedString.charAt(i));
				} else {
					sb.append(replace);
				}
			}
			return sb.toString();
		} else {
			for (int i = 0; i < size; i++) {
				if (!((convertedString.charAt(
						i) & 0xff00) == 0) && (int) convertedString.charAt(i) != 8364) {
					throw new Exception(
							(paramName == null ? "String" : paramName) + " contains invalid characters [" + convertedString.charAt(
									i) + "] [" + (int) convertedString.charAt(
									i) + "]. For [" + theString + "]");
				}
			}
			return convertedString;
		}
	}

	// ALT-0145   &#8216;   �    Left Single Quotation Mark       "\u2018"
	// ALT-0146   &#8217;   �    Right Single Quotation Mark      "\u2019"
	// ALT-0147   &#8220;   �    Left Double Quotation Mark       "\u201C"
	// ALT-0148   &#8221;   �    Right Double Quotation Mark      "\u201D"
	// ALT-0150   &#8211;   �    En Dash                          "\u2013"

	public static String swapMsCharacters(String str) {
		String result = str;
		if (str != null) {
			result = str.replaceAll("\u2018", "\'").replaceAll("\u2019",
					"\'").replaceAll("\u201C", "\"").replaceAll("\u201D",
					"\"").replaceAll("\u2013", "-");
		}

		return result;
	}
}
