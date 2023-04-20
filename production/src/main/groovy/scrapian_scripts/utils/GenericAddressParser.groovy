package scrapian_scripts.utils

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.ScrapianEngine
import com.rdc.scrape.ScrapeAddress
import groovy.util.slurpersupport.Node
import groovy.util.slurpersupport.NodeChild
import org.ccil.cowan.tagsoup.Parser

import java.util.regex.Pattern

/**
 public/protected methods could be overridden by a sub class to add more custom name/regex values in the core parser.
 */
public class GenericAddressParser {
	private ScrapianContext context;
	private String xlsLocation = ""
	protected boolean isCacheActive = true
	protected Map countries;
	protected Map states;
	protected Map cities;
	protected Map postalRules;
	static enum TOKENS {
		DELIMITER, COUNTRY, STATE, CITY, ZIP, COUNTRY_CODE, ADDR_STR
	}
	protected cityTokens = /(?i)\b(?>CITY|DISTRICT|VILLAGE)\b/
	protected stateTokens = /(?i)\b(?>STATE|PROVINCE)\b/
	protected cityIgnoreFilterList = [/\bP\.?O\.? Box\b/]
	protected cityIgnoreSuffixes = /(?i)\b(?>Roads?)\b/

	GenericAddressParser(ScrapianContext context, xlsLocation = null) {
		this.context = context;
		if (xlsLocation) {
			this.xlsLocation = xlsLocation;
		}
		init();
	}

	public getTokens() {
		return TOKENS;
	}

	public void reloadData() {
		isCacheActive = false
		countries = null
		init();
		isCacheActive = true
	}

	private void init() {
		if(!xlsLocation) {
			String fileLocation = "https://github.com/RegDC/scripts";
			String fileName = "standard_addresses"
			ScrapianEngine scrape = new ScrapianEngine();
			String scriptLocation = scrape.getGitHubURLAsString(fileLocation, fileName, "")
			xlsLocation = scriptLocation.replaceAll("file:/", "");
		}
		if (!countries) {
			def xml = isCacheActive ? cachedData(xlsLocation) : null;
			if (!xml) {
				def spreadsheet = context.invokeBinary([url: 'file://' + new File(xlsLocation + "/Country_Standard.xls").getCanonicalPath(), cache: isCacheActive]);
				xml = context.transformSpreadSheet(spreadsheet, [validate: true, escape: true]);
				countries = collectCountry(xml);

				spreadsheet = context.invokeBinary([url: 'file://' + new File(xlsLocation + "/Province_Standard.xls").getCanonicalPath(), cache: isCacheActive]);
				xml = context.transformSpreadSheet(spreadsheet, [validate: true, escape: true]);
				states = collectCSZ(xml);

				spreadsheet = context.invokeBinary([url: 'file://' + new File(xlsLocation + "/City_Standard.xls").getCanonicalPath(), cache: isCacheActive]);
				xml = context.transformSpreadSheet(spreadsheet, [validate: true, escape: true]);
				cities = collectCSZ(xml);

				spreadsheet = context.invokeBinary([url: 'file://' + new File(xlsLocation + "/Postal_Standard.xls").getCanonicalPath(), cache: isCacheActive]);
				xml = context.transformSpreadSheet(spreadsheet, [validate: true, escape: true]);
				postalRules = collectCSZ(xml);

				cachedData(xlsLocation, [countries, states, cities, postalRules])
			} else {
				countries = xml[0];
				states = xml[1];
				cities = xml[2];
				postalRules = xml[3];
			}
		}
	}

	private Map collectCountry(xml) {
		def rows = new XmlSlurper(new Parser()).parseText(xml.toString());
		Map countries = [:];

		rows.row.each { NodeChild row ->
			def code = row.code.toString().trim();
			if (code) {
				def full_name = row.country_name.toString().trim();
				def regexList = []

				//capture regex blocks
				row.childNodes().eachWithIndex { Node node, i ->
					def val = node.children()[0].toString().replaceAll(/(?i)^\s*null\s*$/, "").trim();

					if (val && i > 2) {
						regexList.add(val);
					}
				}

				def words_count = (row.search_words_count?.toString().trim()) ?: 0;
				countries[code] = [full_name, regexList, words_count]
			}
		}

		return countries;
	}

	//collect city-state-zip common method
	private Map collectCSZ(xml) {
		def rows = new XmlSlurper(new Parser()).parseText(xml.toString());
		Map values = [:];

		rows.row.each { NodeChild cell ->
			def code = cell.country_code.toString().trim();
			if (code) {
				Set oldSet = values[code];
				if (!oldSet) {
					values[code] = oldSet = new HashSet();
				}

				cell.childNodes().eachWithIndex { Node node, i ->
					def val = node.children()[0].toString().replaceAll(/(?i)^\s*null\s*$/, "").trim();

					if (val && i > 0) {
						oldSet.add(val);
					}
				}
			}
		}

		return values;
	}

	def updateCountries(Map dataKeyMap, isAppend = true) {
		updateData(countries, dataKeyMap, isAppend)
	}

	def updateStates(Map dataKeyMap, isAppend = true) {
		updateData(states, dataKeyMap, isAppend)
	}

	def updateCities(Map dataKeyMap, isAppend = true) {
		updateData(cities, dataKeyMap, isAppend)
	}

	def updatePostalCodes(Map dataKeyMap, isAppend = true) {
		updateData(postalRules, dataKeyMap, isAppend)
	}

	//isAppend: if false it will reset the the related key in the map
	private void updateData(Map srcMap, Map newData, isAppend) {
		newData.each { k, v ->
			if (isAppend) {
				def sv = srcMap[k];
				if (sv) {
					sv.addAll(v as Set)
				} else {
					srcMap[k] = v as Set
				}
			} else {
				srcMap[k] = v;
			}
		}
	}

	/**
	 * active options keys are:
	 * ----------------------------
	 * log:true(true):false (print various parsing logs)
	 */
	Map<TOKENS, String> parseAddress(String addr, options = [:]) {
		//returns map of: [country_code, country, state, city, zip, remaining-address] (if country's matched)
		Map result = findCountry(addr);
		if (result[TOKENS.COUNTRY]) {
			return findCountryParts(result)
		} else {
			log("Address parser failed for: " + addr, options["log"])
		}

		return [:];
	}

	private Map findCountryParts(Map valueMap) {
		//collect parts
		if (!valueMap[TOKENS.STATE]) {
			findState(valueMap)
		}

		if (!valueMap[TOKENS.CITY]) {
			findCity(valueMap)
		}

		if (!valueMap[TOKENS.ZIP]) {
			findPostalCodes(valueMap)
		}

		//remove all consecutive non-word characters from address string
		valueMap[TOKENS.ADDR_STR] = valueMap[TOKENS.ADDR_STR]?.replaceAll(/(?:\s*(\W)\s*\1\s*)+/, '$1 ')?.trim();

		return valueMap;
	}

	/**
	 * This method let you define various smart parsing options rather than plain parsing method
	 * options keys:
	 * --------------
	 * text: Actual raw address string
	 * force_country: false(default):true (detect country name from state|city names in the address string)
	 * log:true(true):false (print various parsing logs)
	 * smart: true(default):false //smart parsing enabler, use normal parsing method if you don't need smart parsing
	 * delimiter: ,(default is "comma", if 'smart' is true) //part/word separator
	 * ------following keys will only be effective if "smart" is true------
	 * format_newline: true(default)/false: converts newline into selected delimiter
	 * ignored_cities: Provides a list of city tokens(valid regex'es) to exclude from adding as a city
	 * unique_city: true(default):false (ignore probable city part with similar state/country name. Not applicable for US cities)
	 * **/
	Map<TOKENS, String> parseAddress(Map options) {
		//returns: [country, state, city, zip, remaining-address] (if country's matched)
		if (!options["text"]) {
			throw new Exception("You must pass 'text' key as a parsing option")
		}

		def addrMap = parseAddress(options["text"])
		def isForceCountry = false;
		def delim = options["delimiter"] ? options["delimiter"] : ","
		if (!addrMap[TOKENS.COUNTRY_CODE]) {
			isForceCountry = options["force_country"] && options["force_country"].toString() =~ /(?i)\btrue\b/;
			if (isForceCountry) {
				addrMap[TOKENS.DELIMITER] = delim;
				forceFindCountry(options["text"], addrMap);
			}
		}

		if (addrMap[TOKENS.COUNTRY_CODE]) {
			def isSmart = options["smart"] == null || options["smart"].toString() =~ /(?i)\btrue\b/;

			if (isSmart) {
				def isFormatNewLine = options["format_newline"] == null || options["format_newline"].toString() =~ /(?i)\btrue\b/;
				if (isFormatNewLine) {
					addrMap[TOKENS.ADDR_STR] = addrMap[TOKENS.ADDR_STR].replaceAll(/[\n\r]+/, delim);
				}

				def zipCollector = {
					if (!addrMap[TOKENS.ZIP]) {
						addrMap[TOKENS.ADDR_STR] = addrMap[TOKENS.ADDR_STR].toString().replaceAll(/(.*?\s*$delim+[^$delim]+?)\s*(\b\d{4,10}[\s\/-]*\d*\b)\W*$/, { a, b, c ->
							addrMap[TOKENS.ZIP] = c.replaceAll(/\//, "-").trim();
							return b.trim();
						});
					}
				}
				zipCollector.call();

				//collect city
				def cityCalled = true;
				def cityCollector = {
					if (!addrMap[TOKENS.CITY]) {
						//phase1: filter by city tokens
						addrMap[TOKENS.ADDR_STR] = addrMap[TOKENS.ADDR_STR].toString().replaceAll(/((?:^|.*?$delim+))([^$delim]*(?=$cityTokens)\w+)$delim+(.*+)/, { a, b, c, d ->
							addrMap[TOKENS.CITY] = c;
							return b + d;
						});
						if (addrMap[TOKENS.CITY]) {
							return;
						}

						//phase2: take and/or manipulate last word_section as city
						addrMap[TOKENS.ADDR_STR] = addrMap[TOKENS.ADDR_STR].toString().replaceAll(/(.*)\s*$delim+[^\w$delim]*((?:\b[^\W\d]{2,}\b\s*){1,3})\W*$/, { a, b, c ->

							List ignoredCities = options["ignored_cities"] ? options["ignored_cities"] : cityIgnoreFilterList;
							def ignoreMatch = false;
							ignoredCities.any { regx ->
								if (c =~ formatToRegex(regx, false)) {
									ignoreMatch = true;
									return true;
								}
							}

							if (ignoreMatch) {
								return a;
							} else {
								c = c.trim();
								def isUniqueCity = options["unique_city"] == null || options["unique_city"].toString() =~ /(?i)\btrue\b/;

								//check if its an US 2char STATE code
								if (c =~ /(?i)^\b[a-z]{2}\b$/) {
									if (addrMap[TOKENS.COUNTRY] == "UNITED STATES") {
										def cTmp = c.toUpperCase();
										def state = new StateMappingUtil().normalizeState(cTmp);
										if (state != cTmp) {
											//2words state to Full state found
											if (addrMap[TOKENS.STATE]) {
												//swap state-city value
												addrMap[TOKENS.CITY] = addrMap[TOKENS.STATE]
											} else {
												//search for city again
												cityCalled = false;
											}
											addrMap[TOKENS.STATE] = state;
										} else {
											addrMap[TOKENS.CITY] = c;
										}
									} else {
										//if both chars are of uppercase letters ie.
										// "Zona 10 ,Guatemala City, CA" then change it into "Zona 10 ,CA, Guatemala City" and recall
										if (c =~ /(?-i)^\s*[A-Z]{2}\s*$/) {
											cityCalled = false;
											return a.replaceAll(/^(.*?)($delim[^$delim]+)($delim\W*$c)\W*$/, '$1$3$2');
										} else {
											//Ignore 2-char cities(state) from other countries
											return a;
										}
									}
								} else if (addrMap[TOKENS.COUNTRY] != "UNITED STATES") {
									if (isUniqueCity && (addrMap[TOKENS.COUNTRY] =~ /(?i)^\s*$c\s*$/ || addrMap[TOKENS.STATE] =~ /(?i)^\s*$c\s*$/)) {
										//if city name and state OR country names are equal then ignore the city name and re-call
										cityCalled = false;
									} else if (c =~ /$stateTokens/) {
										if (!addrMap[TOKENS.STATE]) {
											//if city name contains special words, ie. PROVINCE|STATE etc then put it into state field
											addrMap[TOKENS.STATE] = c;
										}
										cityCalled = false;
									} else {
										addrMap[TOKENS.CITY] = c;
									}
								} else {
									addrMap[TOKENS.CITY] = c;
								}

								return b;
							}
						});
					}
				}
				cityCollector.call();
				while (!cityCalled) {
					//call again
					cityCollector.call();
					cityCalled = true;
				}

				//call zip collector again
				zipCollector.call();

				//Now call separate state collector
				//use case: if we miss state from all above steps
				def stateCollector = {
					if (!addrMap[TOKENS.STATE]) {
						if (addrMap[TOKENS.COUNTRY] != "UNITED STATES") {
							//phase1: add state filter by stateTokens
							addrMap[TOKENS.ADDR_STR] = addrMap[TOKENS.ADDR_STR].toString().replaceAll(/((?:^|.*?$delim+))([^$delim]*(?=$stateTokens)\w+)$delim+(.*+)/, { a, b, c, d ->
								addrMap[TOKENS.STATE] = c;
								return b + d;
							});

						} else if (addrMap[TOKENS.COUNTRY] == "UNITED STATES") {
							//we will only collect 2char USA states
							addrMap[TOKENS.ADDR_STR] = addrMap[TOKENS.ADDR_STR].toString().replaceAll(/(.*)\s*$delim+[^\w$delim]*\b((?i:[A-Z]{2}))\b\W*$/, { a, b, c ->
								def cTmp = c.toUpperCase();
								def state = new StateMappingUtil().normalizeState(cTmp);
								if (state != cTmp) {
									//2words state to Full state found
									addrMap[TOKENS.STATE] = state;
									return b;
								} else {
									return a;
								}
							});
						}
					}
				}
				stateCollector.call();
			}
		} else {
			log("Forced address parser failed for: " + options["text"], isForceCountry)
		}

		return addrMap;
	}

	/**
	 * Following optional param keys can be used:
	 * --------------------------------------------
	 * street_sanitizer: a closure to format the street data
	 * apply_default_sanitizer: true(default)|false
	 *
	 * */
	def buildAddress(Map resultMap, Map options = [:]) {
		if (resultMap[TOKENS.COUNTRY]) {
			def address = new ScrapeAddress();
			address.country = resultMap[TOKENS.COUNTRY].trim()

			if (resultMap[TOKENS.STATE]) {
				address.province = resultMap[TOKENS.STATE].trim()
			}

			if (resultMap[TOKENS.CITY]) {
				address.city = resultMap[TOKENS.CITY].trim()
			}

			if (resultMap[TOKENS.ZIP]) {
				address.postalCode = resultMap[TOKENS.ZIP].trim()
			}

			def street = resultMap[TOKENS.ADDR_STR].trim();

			//apply default sanitizer
			if (options["apply_default_sanitizer"] != false) {
				street = sanitizeStreet(street)
			}

			if (options["street_sanitizer"]) {
				street = (options["street_sanitizer"]).call(street);
			}

			if (street) {
				address.address1 = street;
			}

			return address;
		}

		return null;
	}

	def sanitizeStreet(street) {
		return street.replaceAll(/\s*([^\w\s()])\1+/, '$1').replaceAll(/^[^\w()]+|[^\w()]+$/, "");
	}

	/**
	 * isSrcTypePlainString: boolean, Either regex-string or plain-string
	 * */
	protected formatToRegex(srcString, boolean isSrcTypePlainString) {
		if (srcString) {
			if (isSrcTypePlainString) {
				return "(?is)" + Pattern.quote(srcString);

			} else {
				if (srcString[0] != "/") {
					//bordered regex match
					return (/(?is)\b(?:${srcString})\b/)

				} else {
					return (/${srcString.replaceAll(/^[\s\/]+|[\s\/]+$/, "")}/)
				}
			}
		}

		return "";
	}

	protected List valueMatcher(List regexArr, String addrStr) {
		//regexArr: [actual value, regex value]
		def match = false;

		def regex0 = formatToRegex(regexArr[0], false)
		def regex1 = regexArr.size() > 1 ? formatToRegex(regexArr[1], false) : "";

		def regex = regex1 ? regex1 : regex0;
		def matchedPart = "";

		if (addrStr =~ regex) {

			//replace closure
			def replaceClosure = { src, regx ->
				def mPart = "";
				def isReplaced = false;
				if (regx) {
					src = src.replaceAll(/$regx(?!.*?$regx.*?$)/, { a ->
						mPart = a + " ";
						isReplaced = true;
						return "";
					});
				}

				return [isReplaced, src, mPart];
			}

			List addrSec0 = replaceClosure.call(addrStr, regex0);
			List addrSec1 = replaceClosure.call(addrStr, regex1);

			//check whether we should use regex0 first to replace or not
			if (addrSec0[0] && addrSec1[0] && addrSec0[1].length() > addrSec1[1].length()) {
				addrSec0 = addrSec1;
				addrSec1 = replaceClosure.call(addrSec0[1], regex0);
			} else {
				addrSec1 = replaceClosure.call(addrSec0[1], regex1);
			}

			addrStr = addrSec1[1];
			matchedPart = (addrSec0[2] + addrSec1[2]).replaceAll(/(?s)\s+/, " ").trim();
			match = true;
		}

		return [match, addrStr, matchedPart];
	}

	Map<TOKENS, String> forceFindCountry(String addrStr, Map tokensMap) {
		if (!tokensMap || !tokensMap[TOKENS.DELIMITER]) {
			throw new Exception("Provide a delimiter in the options param")
		}

		def delimiter = tokensMap[TOKENS.DELIMITER];
		def found = false;
		def originalAddrStr = addrStr;

		//do some global delimiter fixing
		addrStr = addrStr.replaceAll(/(?-i)(?<=[a-z])(?=\s++[A-Z]{2,}+\b)/, delimiter)
		addrStr = addrStr.replaceAll(/(?i)(?<=\d)(?=\s++[a-z]{3,}+\b)/, delimiter)
		addrStr = addrStr.replaceAll(/(?i)(?<=[a-z])(?=\s++\d++\b)/, delimiter)

		//changing the regex will affect the delimiter checking
		def partsToken = addrStr.split(/(?=\b[^\w\s\.'-]\W*+\w++\b)/)
		def partsSum = ""
		def discardedPartsSum = ""
		def currParts = 1

		//changeable value
		def isDiscardItrPartsSum = true;
		def partsPerItr = 1//min:1

		//secondary match validator
		def isValidMatch = { match ->
			if (match[0]) {
				if (addrStr.contains(delimiter)) {
					if (partsSum =~ /(?:^|\s*$delimiter)\s*${match[2]}\W*$/) {
						return true;
					} else {
						return false;
					}
				} else {
					context.warn("No matching delimiter found! Either provide a valid delimiter in the input address or ignore this warning.")
					return true;
				}
			}

			return false;
		}

		for (int i = partsToken.length - 1; i >= 0; i--) {
			//Search with two words at a time starting from the end
			partsSum = partsToken[i] + partsSum
			if (currParts == partsPerItr) {
				currParts = 1;
			} else {
				currParts++;
				continue;
			}

			def valueFinder = { map, token ->
				map.any { k, valueSet ->
					valueSet.any { String v ->
						def regexArr = v.split("=", 2) as List;
						def match = valueMatcher(regexArr, partsSum);

						if (isValidMatch(match)) {
							def escapedPartsSum = Pattern.quote(partsSum);
							def eDisParts = Pattern.quote(discardedPartsSum);
							def remAddr = dollarSafeReplace(addrStr, /$escapedPartsSum(?=$eDisParts$)/, match[1])
							tokensMap[TOKENS.COUNTRY_CODE] = k;

							def qVal = Pattern.quote(regexArr[0])
							def isAddToken = true;
							if (token == TOKENS.STATE && partsSum =~ /$qVal(?=\W*$cityTokens)/) {
								isAddToken = false;
							} else if (token == TOKENS.CITY && partsSum =~ /$qVal(?=\W*$stateTokens)/) {
								isAddToken = false;
							}
							if (isAddToken) {
								tokensMap[token] = regexArr[0];
								tokensMap[TOKENS.ADDR_STR] = remAddr;
							}

							found = true;
							return true;
						}
					}

					return found;
				}
			}

			valueFinder(states, TOKENS.STATE)
			if (found) {
				break;
			}

			valueFinder(cities, TOKENS.CITY)
			if (found) {
				break;
			}

			if (isDiscardItrPartsSum) {
				discardedPartsSum = partsSum + discardedPartsSum
				partsSum = "";
			}
		}

		if (found) {
			tokensMap[TOKENS.COUNTRY] = countries[tokensMap[TOKENS.COUNTRY_CODE]][0];
			findCountryParts(tokensMap);

			//remove extra delimiter from street
			if (tokensMap[TOKENS.ADDR_STR]) {
				def newValue = new StringBuilder()
				tokensMap[TOKENS.ADDR_STR].split(/(?:\s*$delimiter\s*)++/).each {
					newValue.append(it)
					def ext = " "
					originalAddrStr = originalAddrStr.replaceAll(/^.*?$it(\s*$delimiter\s*)/, { a, b ->
						ext = b;
						return ""
					})
					newValue.append(ext)
				}
				tokensMap[TOKENS.ADDR_STR] = newValue.toString()
			}
		}

		return tokensMap;
	}

	Map findCountry(Map countries = this.countries, String addr) {
		def result = [:]
		if (!addr.trim()) {
			return result;
		}

		result[TOKENS.ADDR_STR] = addr;
		def partsToken = []
		addr.replaceAll(/\S++\s*+/, {
			partsToken.add(it);
		});
		def tokensSum = ""

		for (int i = partsToken.size() - 1; i >= -1;) {
			//Search with two words at a time starting from the end
			tokensSum = (i > 0 ? partsToken[i - 1] : "") + partsToken[i] + tokensSum
			i = i - 2;

			//countries[code] = [full_name, regexList, words_count]
			def found = false;
			countries.any { k, v ->

				def match = [];

				//iterate regex list
				v[1].any {
					match = valueMatcher([v[0], it], tokensSum);
					if (match[0]) {
						return true;
					}
				}

				if (v[1].size() < 1) {
					match = valueMatcher([v[0], ""], tokensSum);
				}

				if (match[0]) {
					//Fix possible similar country mis-match issue
					def validLimit = v[2] instanceof String ? Double.parseDouble(v[2]).intValue() : v[2];
					def extraLimit = validLimit - (tokensSum =~ /\S+/).count
					if (extraLimit > 0) {
						int j = i, l = 0;
						def newTokenSum = tokensSum;
						for (; l <= extraLimit && j >= 0;) {
							newTokenSum = partsToken[j] + newTokenSum
							j--;
							l++;
						}

						//repeat the search for new valid country with new tokens
						owner.countries.any { nk, nv ->
							def vLimit = nv[2] instanceof String ? Double.parseDouble(nv[2]).intValue() : nv[2];
							if (vLimit >= validLimit) {
								def match2 = []

								//iterate regex list
								nv[1].any {
									match2 = valueMatcher([nv[0], it], newTokenSum);
									if (match2[0]) {
										return true;
									}
								}

								if (nv[1].size() < 1) {
									match = valueMatcher([nv[0], ""], tokensSum);
								}

								if (match2[0]) {
									match = match2;
									tokensSum = newTokenSum;
									v = nv;
									k = nk;
									return true;
								}
							}
						}
					}
				}

				if (match[0]) {
					tokensSum = Pattern.quote(tokensSum);
					def remAddr = dollarSafeReplace(addr, /$tokensSum$/, match[1])
					result[TOKENS.COUNTRY] = v[0];
					result[TOKENS.COUNTRY_CODE] = k;
					result[TOKENS.ADDR_STR] = remAddr;
					found = true;
					return true;
				}
			}
			if (found) {
				break;
			}
		}

		return result;
	}

	/**
	 * valueMap: contains country+other info parsed previously
	 */
	Map findCity(Map srcMap = this.cities, Map valueMap) {
		return findCityStateCommon(srcMap, valueMap, TOKENS.CITY);
	}

	/**
	 * valueMap: contains country+other info parsed previously
	 */
	Map findState(Map srcMap = this.states, Map valueMap) {
		return findCityStateCommon(srcMap, valueMap, TOKENS.STATE);
	}

	protected Map findCityStateCommon(Map srcMap, Map<TOKENS, String> valueMap, tokenType) {
		if (!valueMap[TOKENS.COUNTRY_CODE]) {
			throw new Exception("country_code is not found in the data map");
		}
		def valueSet = srcMap[valueMap[TOKENS.COUNTRY_CODE]];

		valueSet.any { v ->
			def regexArr = v.split("=", 2) as List;

			def match = valueMatcher(regexArr, valueMap[TOKENS.ADDR_STR]);
			if (match[0]) {
				//TODO: decide whether to make secondary check via delimiter
				//filtering for cross city/state values
				def qVal = Pattern.quote(match[2])
				def isAddToken = true;

				if (tokenType == TOKENS.STATE && valueMap[TOKENS.ADDR_STR] =~ /$qVal(?=\W*$cityTokens)/) {
					isAddToken = false;
				} else if (tokenType == TOKENS.CITY && (valueMap[TOKENS.ADDR_STR] =~ /$qVal(?=\W*$stateTokens)/ || valueMap[TOKENS.ADDR_STR] =~ /$qVal(?=\W+$cityIgnoreSuffixes)/)) {
					isAddToken = false;
				}

				if (isAddToken) {
					valueMap[tokenType] = regexArr[0];
					valueMap[TOKENS.ADDR_STR] = match[1];

					return true;
				}
				//else: keep iterating
			}
		}

		return valueMap;
	}

	/**
	 * valueMap: contains country+other info parsed previously
	 */
	Map findPostalCodes(Map postalRules = this.postalRules, Map<TOKENS, String> valueMap) {
		if (!valueMap[TOKENS.COUNTRY_CODE]) {
			throw new Exception("Country code's not found in the data map");
		}
		def rulesSet = postalRules[valueMap[TOKENS.COUNTRY_CODE]];

		def regexArr = ["", ""];
		rulesSet.any { v ->
			regexArr[1] = v;
			def match = valueMatcher(regexArr, valueMap[TOKENS.ADDR_STR]);

			if (match[0]) {
				valueMap[TOKENS.ZIP] = match[2];
				valueMap[TOKENS.ADDR_STR] = match[1];
				return true;
			}
		}

		return valueMap;
	}

	private dollarSafeReplace(addr, targetRegex, String replace) {
		if (replace.contains("\$")) {
			def tag = "~_dollar_~";

			replace = replace.replaceAll(/\$/, tag)
			targetRegex = targetRegex.replaceAll(/\$(?!$)/, tag)
			addr = addr.replaceAll(/\$/, tag)

			return addr.replaceAll(targetRegex, replace).replace(tag, "\$");
		}

		return addr.replaceAll(targetRegex, replace);
	}

	private log(msg, isActive) {
		//isActive: true(default):false
		isActive = isActive && isActive.toString() =~ /(?i)\btrue\b/;
		if (isActive) {
			this.context.info(msg);
		}
	}

	private cachedData(key, value = null) {
		//This method or it's caller sections can be safely left as_it_is or commented/deleted in production environment
		try {
			//if value is not present then it will only "get" otherwise it wll "put" into the cache
			if (!value) {
				//if key doesn't exist, it'll return "null"
				return context.cached_data.get(key);
			} else {
				context.cached_data.put(key, value);
				return true;
			}
		} catch (e) {
		}

		return null;
	}
}