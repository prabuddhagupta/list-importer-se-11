package scrapian_scripts.utils

import com.rdc.importer.scrapian.ScrapianContext

import java.util.regex.Matcher

public class HtmlTokenParser {
	ScrapianContext context;
	def defaultSanitizeClosure = { it.trim() };
	def isFlattenResultSetGlobal = true;

	HtmlTokenParser(sContext) {
		context = sContext;
	}

	HtmlTokenParser(sContext, sanitizeClosure) {
		context = sContext;
		defaultSanitizeClosure = sanitizeClosure;
	}

	static enum OPTIONS {
		UNIQUE_START_REGEX_TOKEN,/*main mandatory match token*/
		CAPTURE_TYPE,/*define capture type if needed*/
		IS_BR_SPLIT, IS_TAG_STRIP,/*(Default:false) in some cases, you may need to use these*/
		IS_TRUNCATE_HTML,/*(Default:true) should we truncate the matching html since smaller html will leads to faster subsequent match, if set true it will reset to original html state*/
		PRIVATE_SANITIZE_CLOSURE,/*in case you need to define separate sanitizer for a specific option*/
		IS_FLATTEN_RESULT_SET,/*(Default:global rule) Merge all nested results set into single set*/
		ID/*Optional:Used to identify the Option map*/
	}

	static enum CAPTURE_TYPE {
		NAME_VALUE_PAIR, QUOTED_VALUE, CUSTOM_CAPTURE
	}

	/**
	 * tokenKeyMap = [field_key1: optionsMap1, field_key2:optionsMap2,...]
	 * ie, [FIELDS.EYE_COLOR: options]
	 * */
	def parse(srcHtml, Map<Object, Map> tokensMap, Map<Enum, Set> resultMap = [:]) {
		def tmpHtml = srcHtml;
		def prevKv;//Store previous key, value
		def isFullHtml = true;

		tokensMap.each { k, v ->
			def startRegex = v[OPTIONS.UNIQUE_START_REGEX_TOKEN]
			if (!startRegex) {
				throw new Exception("UNIQUE_START_REGEX_TOKEN must be present for html token parsing.")
			}

			k = v[OPTIONS.ID] ?: k
			def isTruncate = v[OPTIONS.IS_TRUNCATE_HTML] == null || v[OPTIONS.IS_TRUNCATE_HTML] == true
			def tryCount = 0;
			List dataList

			try {
				//Retry with full html if parsing via partial-html fails
				while (tryCount < 2) {
					dataList = searchToken(srcHtml, v)
					if (dataList[0].size() > 0) {
						if (tryCount == 1 && !prevKv[2]) {
							context.warn("Set IS_TRUNCATE_HTML to false for: " + prevKv[1][OPTIONS.UNIQUE_START_REGEX_TOKEN])
						}
						break;

					} else if (tryCount == 0) {
						if (isFullHtml || srcHtml =~ startRegex) {
							break;
						} else {
							srcHtml = tmpHtml;
							isFullHtml = true;
						}
					}
					tryCount++;
				}
				//Store data as previous data
				prevKv = [k, v, isFullHtml]

				//evaluate truncation
				if (isTruncate) {
					srcHtml = dataList[1]
					isFullHtml = false;
				} else {
					srcHtml = tmpHtml;
					isFullHtml = true;
				}

				if (dataList[0].size() < 1) {
					context.error("No match found for: " + startRegex)
					return;
				}

				//Set data into result set
				def prevResultSet = resultMap[k]
				if (prevResultSet) {
					prevResultSet.addAll(dataList[0])
				} else {
					resultMap[k] = dataList[0]
				}
			} catch (e) { context.error(e.getMessage(), e) }
		}

		//return result with remaining html
		return [resultMap, srcHtml];
	}

	private searchToken(html, Map<Enum, Object> optionsMap) {
		//return [value, remaining html]
		def startRegex = optionsMap[OPTIONS.UNIQUE_START_REGEX_TOKEN];
		def sanitizeClosure = optionsMap[OPTIONS.PRIVATE_SANITIZE_CLOSURE] ? optionsMap[OPTIONS.PRIVATE_SANITIZE_CLOSURE] : defaultSanitizeClosure;
		Matcher dataMatch;
		Set resultSet = [];
		def lastCapture = /(.*+)/

		if (!optionsMap[OPTIONS.CAPTURE_TYPE]) {//Type: Inter tag value
			//ie.startRegex: Race, Sample data: ...Race:</span> <span id="SuspectRace">Asian<br>African</span>
			//ie.startRegex: SuspectName, Sample data: ..id="SuspectName">Johnny Khanh Nguyen</span>
			dataMatch = html =~ /(?s)$startRegex[^<>]*+(?:>|(?=\s*<))\s*(?:<[^<>]+>\s*)*\s*(?=\S)((?:[^<>]+(?:<\W*(?i)br\W*>)*+)++)(?=<\/)$lastCapture/

		} else if (optionsMap[OPTIONS.CAPTURE_TYPE] == CAPTURE_TYPE.QUOTED_VALUE) {
			//ie.startRegex: nsZoomPic, Sample data: ..onclick="nsZoomPic('https://northerncaliforniamostwanted.org/files/IMF100002/160.jpg');return.."
			//ie.startRegex: IMThumbnailImage.*?src, Sample data: ..class="IMThumbnailImage" src="files/IMT200002/160.png" />
			dataMatch = html =~ /(?s)$startRegex[^<>'"]*['"]+([^'"<>]+)['"]$lastCapture/

		} else if (optionsMap[OPTIONS.CAPTURE_TYPE] == CAPTURE_TYPE.NAME_VALUE_PAIR) {
			//ie. <..name=".." value="capture"
			dataMatch = html =~ /(?s)$startRegex[^<>]+?(?i)\bvalue=['"]([^'"]+)$lastCapture/

		} else if (optionsMap[OPTIONS.CAPTURE_TYPE] == CAPTURE_TYPE.CUSTOM_CAPTURE) {
			//ie. startKeyName with pre-set capture group
			dataMatch = html =~ /(?s)$startRegex$lastCapture/
		}
		//We could have added so many other types but those are not necessary as we could use custom type, hence adding more would be an overkill

		while (true) {
			def matchFound = false

			dataMatch.each { match ->
				matchFound = true
				def tagRemover = optionsMap[OPTIONS.IS_TAG_STRIP] ? { v -> v.replaceAll(/\s*<[^<>]+>\s*/, ' ') } : false

				def res = [];
				match[1..-2]/*ignore first and-last index*/.each { itrVal ->
					itrVal = sanitizeClosure(itrVal)

					def valueCollector = { val ->
						if (optionsMap[OPTIONS.IS_BR_SPLIT]) {
							val = val.split(/\s*<[^\w>]*br[^\w>]*>\s*/).findAll {
								if (tagRemover) {
									it = sanitizeClosure(tagRemover(it));
								}
								if (it) {
									return true;
								}
							}
							if (val.size() > 0) {
								res.add(val)
							}

						} else {
							if (tagRemover) {
								val = sanitizeClosure(tagRemover(val))
							} else {
								val = val.trim()
							}
							if (val) {
								res.add(val)
							}
						}
					}

					if (!(itrVal instanceof String)) {
						itrVal.each { valueCollector(it) }
					} else {
						valueCollector(itrVal)
					}
				}

				resultSet.add(res);
				html = match[-1];
			}

			if (!matchFound) {
				break;
			}

			//re-inquire for more match
			dataMatch = dataMatch.pattern().matcher(html);
		}

		if ((optionsMap[OPTIONS.IS_FLATTEN_RESULT_SET] == null && isFlattenResultSetGlobal) || optionsMap[OPTIONS.IS_FLATTEN_RESULT_SET]) {
			resultSet = resultSet.flatten();
		}

		return [resultSet, html];
	}
}