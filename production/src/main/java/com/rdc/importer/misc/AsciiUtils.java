package com.rdc.importer.misc;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;

public class AsciiUtils {

	public static String convertAndValidateCharacters(String theString) throws ValidationException {
		return convertAndValidateCharacters(theString, null);
	}

	public static String convertAndValidateCharacters(String theString, String paramName) throws ValidationException {
		return convertAndValidateCharacters(theString, paramName, null);
	}

	public static String convertAndValidateCharacters(String theString, String paramName, String encoding) throws ValidationException {
		return convertAndValidateCharacters(theString, paramName, encoding, null);
	}

	public static String convertAndValidateCharacters(String theString, String paramName, String encoding, String replace) throws ValidationException {
		if (theString == null) {
			return null;
		}
		if (encoding == null) {
			encoding = "Cp1252";
		}
		String convertedString = swapMsCharacters(theString);
		String after = null;
		try{
			after = new String(convertedString.getBytes(encoding), encoding);
		} catch (UnsupportedEncodingException uee) {
			encoding = "Cp1252";
			try{
				after = new String(convertedString.getBytes(encoding));
			} catch (UnsupportedEncodingException uee2) {
				after = new String(new char[convertedString.length()]).replace('\0', '?'); //pretty certain to cause a validation exception later on, which would be what we want in this case...
			}
		}

		if (StringUtils.isNotBlank(replace)) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < convertedString.length(); j++) {
				if (convertedString.charAt(j) == after.charAt(j)) {
					sb.append(convertedString.charAt(j));
				} else {
					sb.append(replace);
				}
			}
			return sb.toString();
		} else {
			for (int j = 0; j < convertedString.length(); j++) {
				if (convertedString.charAt(j) != after.charAt(j)) {
					throw new ValidationException((paramName == null ? "String" : paramName) + " contains invalid characters [" + convertedString.charAt(j) + "] [\\u" + Integer.toHexString((int)convertedString.charAt(j) | 0x10000).substring(1) + "]. For [" + theString + "]");
				}
			}
			return convertedString;
		}
	}

	// ALT-0145 &#8216; � Left Single Quotation Mark "\u2018"
	// ALT-0146 &#8217; � Right Single Quotation Mark "\u2019"
	// ALT-0147 &#8220; � Left Double Quotation Mark "\u201C"
	// ALT-0148 &#8221; � Right Double Quotation Mark "\u201D"
	// ALT-0150 &#8211; � En Dash "\u2013"

	public static String swapMsCharacters(String str) {
		String result = str;
		if (str != null) {
			result = str.replaceAll("\u2018", "\'").replaceAll("\u2019", "\'").replaceAll("\u201C", "\"").replaceAll("\u201D", "\"").replaceAll("\u2013", "-");
		}

		return result;
	}
}
