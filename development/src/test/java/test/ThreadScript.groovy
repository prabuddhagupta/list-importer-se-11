package test

import com.rdc.importer.scrapian.model.StringSource
import com.se.rdc.core.ScrapianContextSE
import com.se.rdc.core.utils.TaskTimer
import com.rdc.importer.scrapian.util.Tasker

/**
 * Created by arjuda on 4/27/17.
 */
class ThreadScript {
	def names = [] as HashSet;
	def base_url = "http://www.police.nsw.gov.au/can_you_help_us/wanted";
	static ScrapianContextSE context = new ScrapianContextSE();
	static Tasker tasker = new Tasker();

	public static void main(String[] args) {
		def sTime = new TaskTimer().startTrack("xyz");

		context.cached_data.setStoreFile("/media/arjuda/Volume_E/Projects/RDCScrapper/output/tmp/cached_data1.db");
		context.cached_data.init();
		ThreadScript threadScript = new ThreadScript();
		threadScript.init();
		tasker.runFailedTasks();
		sTime.endTrack("xyz");
		context.cached_data.close();
		tasker.close();
	}


	def init() {
		context.setup([connectionTimeout: 100000, socketTimeout: 200000, retryCount: 1, multithread: true]);
		def mainpage = fetch_data([url: base_url, type: "get"]);

		def links = context.regexMatches(new StringSource(mainpage), [regex: "http://www.police.nsw.gov.au/can_you_help_us/wanted/[^\"]*"]);
		Set unique = new HashSet();
		links.each { link ->
			unique.add(link[0].toString());
		}

		unique.eachWithIndex { url, i ->
			tasker.execute({
				create_entity(url)
			})
		}

	}

	def init2() {
		context.setup([connectionTimeout: 100000, socketTimeout: 200000, retryCount: 1, multithread: true]);
		def mainpage = fetch_data([url: base_url, type: "get"]);

		def links = context.regexMatches(new StringSource(mainpage), [regex: "http://www.police.nsw.gov.au/can_you_help_us/wanted/[^\"]*"]);
		Set unique = new HashSet();
		links.each { link ->
			unique.add(link[0].toString());
		}
		unique.each { url ->
			//def page = fetch_data([url: url, type: "get"]);
			create_entity(url);
		}

	}

	def fetch_data(params) {
		//return new File( args[1] ).text;
		return context.invoke(params).toString();
	}

	def create_entity(page) {
		def entity;
		println "\n\tFETCHING " + page;

		def data = fetch_data([url: page, type: "GET"]);
		def s;
		if (data == null) {
			println("FAILED TO RETRIEVE $page"); return;
		}

		s = sanitize(parse_data("<dt.*?>Full\\s+Name\\s*[:]?(.*?)</dd>", 1, true, data));
		println s;

		if (names.contains(s)) {
			return;
		}

		names.add(s);

		entity = context.getSession().newEntity();
		entity.setType("P");
		entity.setName(context.formatName(s));
		entity.addUrl(page);

		s = sanitize(parse_data("<dt.*?>\\s*Date\\s+of\\s+Birth\\s*[:]?(.*?)</dd>", 1, true, data));

		s = context.parseDate(new StringSource(s), "dd/MM/yyyy");

		//entity.addDateOfBirth( s );

		s = sanitize(parse_data("<dt.*?>\\s*Gender\\s*[:]?(.*?)</dd>", 1, true, data));
		entity.addSex(s);

		s = sanitize(parse_data("<dt.*?>\\s*Eyes\\s*[:]?(.*?)</dd>", 1, true, data));
		entity.addEyeColor(s);

		s = sanitize(parse_data("<dt.*?>\\s*Hair\\s*[:]?(.*?)</dd>", 1, true, data));
		entity.addHairColor(s);

		s = sanitize(parse_data("<dt.*?>\\s*Build\\s*[:]?(.*?)</dd>", 1, true, data));
		entity.addBuild(s);

		s = sanitize(parse_data("<dt.*?>\\s*Height\\s*[:]?(.*?)</dd>", 1, true, data));
		entity.addHeight(s);

		s = sanitize(parse_data("<dt.*?>\\s*Wanted\\s+for\\s*[:]?(.*?)</dd>", 1, true, data));
		entity.newEvent().setDescription(s);

		s = sanitize(parse_data("<p.*?>.*?\\s*Distinguishing\\s+features\\s*[:]?(.*?)</p>", 1, true, data));
		entity.addRemark(s);

		s = parse_data("<div\\s+class=(?:\"|')wantedProfileImage(?:\"|').*?>\\s*" +
				"<img\\s+.*?\\s+src=(?:\"|')(.*?)(?:\"|').*?/>\\s*</div>", 1,
				false, sanitize(data));

		entity.addImageUrl(base_url + "/" + s);
	}

	def parse_data(regex, group_idx, nohtml, data) {
		def match = get_match(regex, data);
		def result = "";
		while (match.find()) {
			result = match.group(group_idx).trim();
			if (nohtml) { result = strip_html(result).trim(); }
			break;
		}

		return result;
	}
//----------------------------------------------------------------------------
	def get_match(regex, data) {
		return java.util.regex.Pattern.compile(regex,
				java.util.regex.Pattern.DOTALL |
						java.util.regex.Pattern.CASE_INSENSITIVE)
				.matcher(data);
	}
//----------------------------------------------------------------------------
	def sanitize(data) {
		def str = '';

		if (data != null) {
			str = data.replace('\n', ' ').replaceAll('\r', ' ')
					.replaceAll('\u00A0', ' ')
					.replaceAll('\302', ' ')
					.replaceAll(' +', ' ')
					.replaceAll('\\s+', ' ').trim();
		}
		return str;
	}
//----------------------------------------------------------------------------
	def strip_html(data) {
		def str = "";

		if (data != null) {
			str = data.replaceAll("<[^>]*?>", " ")
					.replaceAll("&[^;]*?;", " ")
					.replaceAll('\\s+', ' ').trim();
		}

		return str;
	}
}
