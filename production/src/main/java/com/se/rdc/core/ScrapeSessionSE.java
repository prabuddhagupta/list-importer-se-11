package com.se.rdc.core;

import com.rdc.importer.misc.InvalidXmlCharFilterWriter;
import com.rdc.scrape.ScrapeEntity;
import com.rdc.scrape.ScrapeSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by omar on 6/1/17.
 */
public class ScrapeSessionSE extends ScrapeSession {
	private final String listName;

	public ScrapeSessionSE(String listName) {
		super(listName);
		this.listName = listName;
	}

	public void dump(OutputStream output, boolean prettyPrint,
			boolean doCharCheck) throws Exception {
		String encoding = getEncoding();
		StringBuilder sb = new StringBuilder("Invalid chars found:\n");
		TreeSet<String> set = new TreeSet<String>();

		if (encoding == null) {
			encoding = System.getProperty("file.encoding", "windows-1252");
			setEncoding(encoding);
		}

		StringWriter writer = new StringWriter() {
			@Override public String toString() {
				StringBuffer buf = getBuffer();
				String v = buf.toString();
				buf.delete(0, buf.length());
				return v;
			}
		};
		InvalidXmlCharFilterWriter filter = new InvalidXmlCharFilterWriter(writer);

		prepCache();
		String scrapeXml = "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n<entities>";
		IOUtils.copy(new StringReader(scrapeXml), output, encoding);

		//tag matcher for validator
		Pattern tagPattern = Pattern.compile("<([^<>]+)>([^<>]+)</\\1>");

		for (ScrapeEntity entity : getEntitiesNonCache()) {
			filter.write(entity.toXml(isEscape(), isEscapeSpecial(), prettyPrint));
			String specialString = writer.toString();

			if (doCharCheck) {
				//unescaping here partly leads to the very approximate positions quoted later
				String unescapedXml = StringEscapeUtils.unescapeXml(specialString);
				validateChar(entity.getName(), unescapedXml, tagPattern, sb, set);
			}

			IOUtils.copy(new StringReader("\n" + specialString), output, encoding);
		}

		IOUtils.copy(new StringReader("\n</entities>"), output, encoding);
		output.flush();

		if (set.size() > 0) {
			System.err.println(sb.toString() + "\n Set = " + set.toString());
		}
	}

	private void validateChar(String name, String scrapeXml, Pattern tagPattern,
			StringBuilder sb, TreeSet<String> set) {
		//we don't need to validate "detailedAlias" tag's content
		scrapeXml = scrapeXml.replaceAll("(?s)<(detailedAlias)>.*?</\\1>", "");
		Matcher origMatcher = tagPattern.matcher(scrapeXml);

		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			IOUtils.copy(new StringReader(scrapeXml), os, "windows-1252");
			String scrapeXmlAfter = os.toString();//new String(os.toByteArray(), "windows-1252");
			Matcher afterMatcher = tagPattern.matcher(scrapeXmlAfter);

			while (origMatcher.find() && afterMatcher.find()) {
				scrapeXml = origMatcher.group(2);
				scrapeXmlAfter = afterMatcher.group(2);
				String tag = origMatcher.group(1);

				if (!scrapeXmlAfter.equals(scrapeXml)) {

					for (int j = 0; j < scrapeXml.length(); j++) {
						if (scrapeXml.charAt(j) != scrapeXmlAfter.charAt(
								j) && scrapeXmlAfter.charAt(j) == '?') {
							String unicode = "\\u" + Integer.toHexString(
									(int) scrapeXml.charAt(j) | 0x10000).substring(
									1).toUpperCase();
							sb.append(
									(sb.length() > 0 ? "\n" : "") + "Name: " + name + " Tag: " + tag + ", contains invalid character [" + scrapeXml.charAt(
											j) + "] unicode: " + unicode);
							set.add(unicode);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
