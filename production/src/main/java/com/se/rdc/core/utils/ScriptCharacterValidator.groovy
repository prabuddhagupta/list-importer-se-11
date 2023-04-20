package com.se.rdc.core.utils

import org.apache.commons.io.IOUtils

class ScriptCharacterValidator {

	def validate(URI filePath, srcEncoding = "UTF-8", destEncoding = "windows-1252", fileEncoding = "UTF-8") {
		def text = new File(filePath.rawSchemeSpecificPart).getText(fileEncoding)
		validate(text, srcEncoding, destEncoding)
	}

	def validate(String text, String srcEncoding, String destEncoding) {
		def stringBuffer = new StringBuffer();
		Set charSet = []

		int i = 1;
		text.split(/\b(?=\w+\b)/).each { word ->
			if (!isValid(word, srcEncoding, destEncoding)) {

				def nsBuffer = new StringBuffer();
				word.each { chr ->
					if (!charSet.contains(chr) && !isValid(chr, srcEncoding, destEncoding)) {
						nsBuffer.append("  - Bad char: [" + chr + "] Replace it with:\\u" + zeroPad(Integer.toHexString((int) chr)) + " || optionally, use visibly equivalent ascii char for replace/replaceAll value\n")
						charSet.add(chr)
					}
				}
				if (nsBuffer.size() > 0) {
					stringBuffer.append(i + ". Bad word=> " + word + "\n")
					stringBuffer.append(nsBuffer)
				}

				i++;
			}
		}

		if (stringBuffer.size() > 0) {
			println("================== Bad characters found in the script ========================")
			println(stringBuffer)
		}
	}

	public boolean isValid(text, srcEncoding, destEncoding) {
		ByteArrayOutputStream os = new ByteArrayOutputStream()
		IOUtils.copy(new StringReader(text), os, srcEncoding)
		def charaAfter = new String(os.toByteArray(), destEncoding)
		os.close();
		return charaAfter.equals(text);
	}

	def zeroPad(input) {
		return "0000".substring(0, 4 - input.length()) + input;
	}
}
