package com.se.rdc.core.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;

public class SecondaryValidator {
	// extra xml validation
	public void xmlTagValuesValidation(String fileName, String encoding)
			throws IOException {
		String outputContents = "";

		// used for faulty org type detection
		List<String> fullPersonWords = new ArrayList<String>();
		// partial_name:full name
		HashMap<String, String> partialPersonWords = new HashMap<String, String>();
		// full name:{partial_name:suffixSet}
		SortedMap<String, HashMap<String, HashSet<String>>> personNames = new TreeMap<String, HashMap<String, HashSet<String>>>();

		//		byte[] encoded;
		//		try {
		//			encoded = Files.readAllBytes(Paths.get(fileName));
		//			outputContents = StandardCharsets.UTF_8.decode(
		//					ByteBuffer.wrap(encoded)).toString();
		//		} catch (IOException e) {
		//		}

		String name, tagName, errName;

		String sorceIdRegex = "<data_source_id>";
		String eachLineRegex = "(?s)<name>([^<>]*+)</name>(.*?)</entity>";
		//(null,etc|unformatted space|tailing non-word char)
		String valueValidator = "(?i)(^\\W*+(?:null|unknown|none|n[-\\/]a|and)\\W*+$|((?s:\\s{2,}|^\\s|\\s$)|[^\\S\\u0020])|(^[^\\w\\s.\\(\\[&#]|[^\\w\\s\\.\\)\\];\\/]$|&[^&]++(;)$))";
		String tagRegex = "<([^<>]++)>([^<>]*+)</\\1>";
		String escapeValidator = "(?i)(&amp;(?:#\\d|[a-z])[^><&\\s]*?;)";
		String unEscapedRegex = "(&|<|>|')[^;]+?(?=&|$)";
		String aliasRegex = "(?i)(\\b(?:(?>d[-\\/\\.]?b[-\\/\\.]?[ao]\\b|[af][-\\/\\.]?k[-\\/\\.]?a\\b)[-\\/\\.]?|(?>trading|now known|doing business|operating) as|(?>may also use|trading name of|o\\s*\\/\\s*a)|(?>previously|also|former?ly)(?: known as)?)\\b\\s*)";
		String nameFormatRegex = aliasRegex + "|" + valueValidator + "|" + escapeValidator;

		// for faulty Org type detection
		String personMatcherRegex = "(?i)\\b(?>mrs?|[js]r|Dr|prof(?:essor)?)\\b";

		Matcher matcher;
		Matcher tagMatcher;
		Matcher validMatcher;

		java.util.regex.Pattern tagPattern = java.util.regex.Pattern.compile(
				tagRegex);
		java.util.regex.Pattern nameFormatPattern = java.util.regex.Pattern.compile(
				nameFormatRegex);
		java.util.regex.Pattern valueValidatorPattern = java.util.regex.Pattern.compile(
				valueValidator);
		java.util.regex.Pattern escapeValidatorPattern = java.util.regex.Pattern.compile(
				escapeValidator);
		java.util.regex.Pattern unEscapeValidatorPattern = java.util.regex.Pattern.compile(
				unEscapedRegex);

		// for faulty Org type detection
		java.util.regex.Pattern personMatcherPattern = java.util.regex.Pattern.compile(
				personMatcherRegex);

		ArrayList<String> printableErrors = new ArrayList<>();
		ArrayList<String> tagErrors = new ArrayList<>();
		ArrayList<String> duplicateNames = new ArrayList<>();
		ArrayList<String> namesList = new ArrayList<>();

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(fileName), encoding));

		//iterate xml file contents
		String lf = System.getProperty("line.separator");
		StringBuilder lineBuilder = new StringBuilder();

		for (; (outputContents = reader.readLine()) != null; ) {
			lineBuilder.append(outputContents);
			lineBuilder.append(lf);
			if (!outputContents.contains("</entity>")) {
				continue;
			}

			outputContents = lineBuilder.toString();
			lineBuilder = new StringBuilder();

			// initialize flag for duplicate names
			Matcher sourceIdMatcher = java.util.regex.Pattern.compile(
					sorceIdRegex).matcher(outputContents);
			boolean isSourceIdAvailable = sourceIdMatcher.find();
			matcher = java.util.regex.Pattern.compile(eachLineRegex).matcher(
					outputContents);

			// per line iteration
			while (matcher.find()) {
				name = matcher.group(1).trim();
				if (name.length() < 1) {

					// empty name checker
					printableErrors.add(
							buildErrString(name, "name", "Error", "Empty name"));
					tagErrors.add(name + "empty");
				} else {

					// duplicate names checker
					if (!isSourceIdAvailable) {
						if (namesList.contains(name)) {
							if (!duplicateNames.contains(name)) {
								duplicateNames.add(name);
							}
						} else {
							namesList.add(name);
						}
					}

					// Name with malformed value check
					validMatcher = nameFormatPattern.matcher(name);
					while (validMatcher.find()) {
						int len = validMatcher.groupCount();
						for (int i = 1; i <= len; i++) {
							String grpValue = validMatcher.group(i);
							if (grpValue != null) {
								errName = name + grpValue;
								if (!tagErrors.contains(errName)) {
									tagErrors.add(errName);
									printableErrors.add(
											buildErrString(name, "name", "Contains Error", grpValue));
								}
							}
						}
					}

					// Name value xml unescaped "&|<|>|'" validator
					validMatcher = unEscapeValidatorPattern.matcher(name);
					while (validMatcher.find()) {
						errName = name + validMatcher.group(1);
						if (!tagErrors.contains(errName)) {
							tagErrors.add(errName);

							printableErrors.add(
									buildErrString(name, "name", "Contains unescaped char",
											validMatcher.group(
													1) + " | Unescaped xml literal found. Add: \"context.getSession().setEscape( true );\" after import statements in the script."));
						}
					}
				}
				tagMatcher = tagPattern.matcher(matcher.group(2));

				while (tagMatcher.find()) {
					tagName = tagMatcher.group(1);
					if (tagMatcher.group(2).trim().length() < 1) {

						// empty tag value checker
						printableErrors.add(
								buildErrString(name, tagName, "Value Error", "Empty value"));
						tagErrors.add(name + tagName + "empty");
					} else {

						// tag invalid value validator
						validMatcher = valueValidatorPattern.matcher(tagMatcher.group(2));
						while (validMatcher.find()) {
							errName = tagName + validMatcher.group(1);
							if (!tagErrors.contains(errName)) {
								tagErrors.add(errName);
								String val;
								if (validMatcher.group(2) != null) {
									val = "Contains invalid space(multiple/terminal spaces|\\n|\\r|\\t) chars. Please remove spaces, use: value.replaceAll(/(?s)\\s+/, \" \").trim()";

								} else if (validMatcher.group(3) != null) {
									val = "Contains invalid terminal chars => "+(validMatcher.group(4) != null ? validMatcher.group(
											4) : validMatcher.group(3));

								} else {
									val = validMatcher.group(1);
								}
								printableErrors.add(
										buildErrString(name, tagName, "Value Error", val));
							}
						}

						// Faulty org type checker
						if (tagName.equals("type") && tagMatcher.group(2).equals(
								"P") && !personMatcherPattern.matcher(name).find()) {
							faultyOrgTypeDetection(fullPersonWords, partialPersonWords,
									personNames, name);
						}

						// tag value xml unescaped "&|<|>|'" validator
						validMatcher = unEscapeValidatorPattern.matcher(
								tagMatcher.group(2));
						while (validMatcher.find()) {
							errName = tagName + validMatcher.group(1);
							if (!tagErrors.contains(errName)) {
								tagErrors.add(errName);
								printableErrors.add(buildErrString(name, tagName, "Char Error",
										validMatcher.group(
												1) + " | Unescaped xml literal found. Add: \"context.getSession().setEscape( true );\" after import statements in the script."));
							}
						}

						// tag value xml escape chr validator
						validMatcher = escapeValidatorPattern.matcher(tagMatcher.group(2));
						while (validMatcher.find()) {
							errName = tagName + validMatcher.group(1);
							if (!tagErrors.contains(errName)) {
								tagErrors.add(errName);
								printableErrors.add(buildErrString(name, tagName, "XML Error",
										validMatcher.group(1)));
							}
						}

					}
				}
			}
			outputContents = "";
		}

		if (tagErrors.size() > 0) {
			System.out.println(
					"\n\n========= Output xml errors list ================");
			for (String error : printableErrors) {
				System.out.println(error);
			}
			printableErrors = tagErrors = null;
		}

		// Duplicates print
		if (duplicateNames.size() > 0) {
			System.out.println(
					"\n\n==== Warning! Duplicate names found (" + duplicateNames.size() + ") ==========");
			for (String dname : duplicateNames) {
				System.out.println(dname);
			}
			duplicateNames = null;
		}

		// Faulty org type print
		if (personNames.size() > 0) {
			System.out.println(
					"\n\n=== Warning! Probable entity type mismatch found (" + personNames.size() + ") ====");
			Set<Entry<String, HashMap<String, HashSet<String>>>> set = personNames.entrySet();
			for (Entry<String, HashMap<String, HashSet<String>>> dname : set) {
				if (dname.getValue().size() > 0) {
					System.out.println(dname);
				} else {
					System.out.println(dname.getKey());
				}
			}
		}
		personNames = null;
		System.out.println("\n\n");
	}

	private String buildErrString(String name, String tagName, String errType,
			String errValue) {
		return "Name: " + name + " || Tag: " + tagName + " || " + errType + ": " + errValue;
	}

	// Faulty org type detection
	public void faultyOrgTypeDetection(List<String> fullPersonWords,
			HashMap<String, String> partialPersonWords,
			SortedMap<String, HashMap<String, HashSet<String>>> personNames,
			String name) {

		Matcher wordsMatcher = java.util.regex.Pattern.compile(
				"\\b(\\w{2,})\\b").matcher(name);// min 2 char word are considered
		java.util.regex.Pattern partialWordPattern = java.util.regex.Pattern.compile(
				"(\\w{3,}?)((?>an|en|i(?>ng|on)|ly|ments?|ors?|s)\\b|(?=([A-Z][a-z]*)|$))");

		List<String> tmpFullWords = new ArrayList<>();
		HashMap<String, String> tmpPartialWords = new HashMap<String, String>();
		HashMap<String, HashSet<String>> tmpPartialNameSuffixMap = new HashMap<String, HashSet<String>>();
		boolean found = false;

		while (wordsMatcher.find()) {
			String word = wordsMatcher.group(1);
			String wordLcase = word.toLowerCase();
			if (fullPersonWords.contains(wordLcase)) {
				found = true;
				break;
			} else {
				Matcher pMatcher = partialWordPattern.matcher(word);
				while (pMatcher.find()) {
					String pName = pMatcher.group(1);
					String suffix = pMatcher.group(2);
					String exSuffix = pMatcher.group(3);

					if (!pName.isEmpty()) {
						String pNameLcase = pName.toLowerCase();
						String pFullName = partialPersonWords.get(pNameLcase);
						if (pFullName != null) {
							found = true; // dont add the name
							suffix = exSuffix == null ? (suffix.isEmpty() ? "full_match" : suffix) : exSuffix;
							setHashMapSet(personNames.get(pFullName), pNameLcase, suffix);

						} else {
							// add in tmpPartial
							pFullName = tmpPartialWords.get(pNameLcase);
							if (pFullName != null) {
								suffix = exSuffix == null ? (suffix.isEmpty() ? "full_match" : suffix) : exSuffix;
								setHashMapSet(tmpPartialNameSuffixMap, pNameLcase, suffix);

							} else {
								// create new object array
								tmpPartialWords.put(pNameLcase, name);
							}
						}
					}
				}
				if (!found) {
					tmpFullWords.add(wordLcase);
				}
			}
		}
		if (!found) {
			// add pertial words
			fullPersonWords.addAll(tmpFullWords);
			partialPersonWords.putAll(tmpPartialWords);

			// Add in final list
			personNames.put(name, tmpPartialNameSuffixMap);
		}
	}

	private void setHashMapSet(HashMap<String, HashSet<String>> hmSet, String key,
			String suffixValue) {
		HashSet<String> hashSet = hmSet.get(key);
		if (hashSet != null) {
			hashSet.add(suffixValue);
		} else {
			hashSet = new HashSet<String>();
			hashSet.add(suffixValue);
			hmSet.put(key, hashSet);
		}
	}
}
