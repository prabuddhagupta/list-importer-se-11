package com.rdc.importer.scraper;

import com.rdc.scrape.*;

import net.sf.ehcache.Cache;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class EntityValidationService {
	private static final String[] domainTypes = {".ac", ".ad", ".ae", ".af", ".ag", ".ai", ".al", ".am", ".an", ".ao", ".aq", ".ar", ".arpa", ".as", ".at", ".au", ".aw", ".ax", ".az", ".ba", ".bb", ".bd", ".be", ".bf", ".bg", ".bh", ".bi", ".bj", ".bm", ".bn", ".bo", ".bq", ".br", ".bs", ".bt", ".bv", ".bw", ".by", ".bz", ".ca", ".cc", ".cd", ".cf", ".cg", ".ch", ".ci", ".ck", ".cl", ".cm", ".cn", ".co", ".com", ".cr", ".cs", ".cu", ".cv", ".cw", ".cx", ".cy", ".cz", ".dd", ".de", ".dj", ".dk", ".dm", ".do", ".dz", ".ec", ".edu", ".ee", ".eg", ".eh", ".er", ".es", ".et", ".eu", ".fi", ".fj", ".fk", ".fm", ".fo", ".fr", ".ga", ".gb", ".gd", ".ge", ".gf", ".gg", ".gh", ".gi", ".gl", ".gm", ".gn", ".gov", ".gp", ".gq", ".gr", ".gs", ".gt", ".gu", ".gw", ".gy", ".hk", ".hm", ".hn", ".hr", ".ht", ".hu", ".id", ".ie", ".il", ".im", ".in", ".int", ".io", ".iq", ".ir", ".is", ".it", ".je", ".jm", ".jo", ".jp", ".ke", ".kg", ".kh", ".ki", ".km", ".kn", ".kp", ".kr", ".kw", ".ky", ".kz", ".la", ".lb", ".lc", ".li", ".lk", ".lr", ".ls", ".lt", ".lu", ".lv", ".ly", ".ma", ".mc", ".md", ".me", ".mg", ".mh", ".mil", ".mk", ".ml", ".mm", ".mn", ".mo", ".mp", ".mq", ".mr", ".ms", ".mt", ".mu", ".mv", ".mw", ".mx", ".my", ".mz", ".na", ".nc", ".ne", ".net", ".nf", ".ng", ".ni", ".nl", ".no", ".np", ".nr", ".nu", ".nz", ".om", ".org", ".pa", ".pe", ".pf", ".pg", ".ph", ".pk", ".pl", ".pm", ".pn", ".pr", ".ps", ".pt", ".pw", ".py", ".qa", ".re", ".ro", ".rs", ".ru", ".rw", ".sa", ".sb", ".sc", ".sd", ".se", ".sg", ".sh", ".si", ".sj", ".sk", ".sl", ".sm", ".sn", ".so", ".sr", ".ss", ".st", ".su", ".sv", ".sx", ".sy", ".sz", ".tc", ".td", ".tf", ".tg", ".th", ".tj", ".tk", ".tl", ".tm", ".tn", ".to", ".tp", ".tr", ".tt", ".tv", ".tw", ".tz", ".ua", ".ug", ".uk", ".us", ".uy", ".uz", ".va", ".vc", ".ve", ".vg", ".vi", ".vn", ".vu", ".wf", ".ws", ".ye", ".yt", ".yu", ".za", ".zm", ".zw" };

	private static Logger logger = LogManager.getLogger(EntityValidationService.class);
	Calendar sqlMaxCal;

	public List<String> validate(Cache scrapeEntities) {
		List<String> validationExceptions = new ArrayList<String>();
		int counter = 0;
		sqlMaxCal = Calendar.getInstance();
		sqlMaxCal.set(10000, 0, 1, 0, 0, 0); //the second after 12/31/9999

		for (Object key : scrapeEntities.getKeys()) {
			ScrapeEntity scrapeEntity = (ScrapeEntity) (scrapeEntities.get(key).getValue());
			if(counter++ % 10000 == 0){
        		//logger.info("Validating # " + counter);
			}
			try {
				validateEntity(scrapeEntity);
			} catch (ValidationException e) {
				validationExceptions.add("Entity[" + scrapeEntity.getName() + "] " + e.getMessage());
			}
		}
		return validationExceptions;
	}

	private void validateEntity(ScrapeEntity scrapeEntity) throws ValidationException {
		validateRequired("name", scrapeEntity.getName());
		if(scrapeEntity.getName().startsWith(",")){
			throw new ValidationException("name", "Entity Name cannot start with a comma.");
		}
		validateLength("name", scrapeEntity.getName(), 250);
		validateLength("data_source", scrapeEntity.getDataSourceId(), 20);
		validateEvents(scrapeEntity.getEvents());
		for (ScrapeDob scrapeDob : scrapeEntity.getDateOfBirths()) {
			try {
				if (Integer.parseInt(scrapeDob.getYear()) < 1900 || Integer.parseInt(scrapeDob.getYear()) > Calendar.getInstance().get(Calendar.YEAR)) {
					throw new ValidationException("dob", "Invalid dob[" + scrapeDob.getYear() + "]");
				}

				if (StringUtils.isNotBlank(scrapeDob.getDay()) && ! new String("-").equals(scrapeDob.getDay()) ) {
					Integer.parseInt(scrapeDob.getDay());
				}
				if (StringUtils.isNotBlank(scrapeDob.getMonth()) && ! new String("-").equals(scrapeDob.getMonth()) ) {
					Integer.parseInt(scrapeDob.getMonth());
				}
			} catch (NumberFormatException e) {
				throw new ValidationException("dob", "Invalid dob[" + scrapeDob.getDay() + "][" + scrapeDob.getMonth() + "][" + scrapeDob.getYear() + "]");
			}
		}
		for (ScrapeDeceased scrapeDeceased : scrapeEntity.getDeceased()) {
			if (StringUtils.isNotBlank(scrapeDeceased.getYear())) {
				try {
					if (Integer.parseInt(scrapeDeceased.getYear()) < 1900 || Integer.parseInt(scrapeDeceased.getYear()) > Calendar.getInstance().get(Calendar.YEAR)) {
						throw new ValidationException("deceased", "Invalid deceased date [" + scrapeDeceased.getYear() + "]");
					}

					if (scrapeDeceased.getDay() != null && !new String("-").equals(scrapeDeceased.getDay())) {
						Integer.parseInt(scrapeDeceased.getDay());
					}
					if (scrapeDeceased.getMonth() != null && !new String("-").equals(scrapeDeceased.getMonth())) {
						Integer.parseInt(scrapeDeceased.getMonth());
					}
				} catch (NumberFormatException e) {
					throw new ValidationException("deceased", "Invalid deceased date [" + scrapeDeceased.getDay() + "][" + scrapeDeceased.getMonth() + "][" + scrapeDeceased.getYear() + "]");
				}
			}
		}
		for (ScrapePosition pos : scrapeEntity.getPositions()) {
			if (StringUtils.isNotBlank(pos.getFromYear())) {
				try {
					if (Integer.parseInt(pos.getFromYear()) < 1900 || Integer.parseInt(pos.getFromYear()) > Calendar.getInstance().get(Calendar.YEAR)) {
						throw new ValidationException("position", "Invalid position from date [" + pos.getFromYear() + "]");
					}

					if (pos.getFromDay() != null && !new String("-").equals(pos.getFromDay())) {
						Integer.parseInt(pos.getFromDay());
					}
					if (pos.getFromMonth() != null && !new String("-").equals(pos.getFromMonth())) {
						Integer.parseInt(pos.getFromMonth());
					}
				} catch (NumberFormatException e) {
					throw new ValidationException("position", "Invalid position date [" + pos.getFromDay() + "][" + pos.getFromMonth() + "][" + pos.getFromYear() + "]");
				}
			}
			if (StringUtils.isNotBlank(pos.getToYear())) {
				try {
					if (Integer.parseInt(pos.getToYear()) < 1900 || Integer.parseInt(pos.getToYear()) > 9999) {
						throw new ValidationException("position", "Invalid position to date [" + pos.getToYear() + "]");
					}

					if (pos.getToDay() != null && !new String("-").equals(pos.getToDay())) {
						Integer.parseInt(pos.getToDay());
					}
					if (pos.getToMonth() != null && !new String("-").equals(pos.getToMonth())) {
						Integer.parseInt(pos.getToMonth());
					}
				} catch (NumberFormatException e) {
					throw new ValidationException("position", "Invalid position date [" + pos.getToDay() + "][" + pos.getToMonth() + "][" + pos.getToYear() + "]");
				}
			}

			if (StringUtils.isNotBlank(pos.getFromYear()) && StringUtils.isNotBlank(pos.getToYear())) {
				String from = StringUtils.isBlank(pos.getFromMonth()) || new String("-").equals(pos.getFromMonth()) ? "1" : pos.getFromMonth();
				from = from + "/";
				from = from + (StringUtils.isBlank(pos.getFromDay()) || new String("-").equals(pos.getFromDay()) ? "1" : pos.getFromDay());
				from = from + "/";
				from = from + pos.getFromYear();
				String to = StringUtils.isBlank(pos.getToMonth()) || new String("-").equals(pos.getToMonth()) ? "12" : pos.getToMonth();
				to = to + "/";
				to = to + (StringUtils.isBlank(pos.getToDay()) || new String("-").equals(pos.getToDay()) ? "31" : pos.getToDay());
				to = to + "/";
				to = to + pos.getToYear();
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				try {
					Date fromDate = sdf.parse(from);
					Date toDate = sdf.parse(to);
					if (fromDate.after(toDate)) {
						throw new ValidationException("position", "To date [" + to + "] before from date [" + from + "]");
					}
				} catch (ParseException e) {
					//ignore this will be handled above
				}
			}
			validateLength("position", pos.getDescription(), 500);
		}
		validateAttributeLength("EyeColor", scrapeEntity.getEyeColors(), 50);
		validateAttributeLength("HairColor", scrapeEntity.getHairColors(), 50);
		validateAttributeLength("Height", scrapeEntity.getHeights(), 50);
		validateAttributeLength("Weight", scrapeEntity.getWeights(), 50);
		validateAttributeLength("ScarsMarks", scrapeEntity.getScarsMarks(), 500);
		validateAttributeLength("Languages", scrapeEntity.getLanguages(), 200);
		validateAttributeLength("Nationalities", scrapeEntity.getNationalities(), 150);
		validateAttributeLength("Complexion", scrapeEntity.getComplexions(), 50);
		validateAttributeLength("Occupations", scrapeEntity.getOccupations(), 400);
		validateAttributeLength("PhysicalDescription", scrapeEntity.getPhysicalDescriptions(), 500);
		validateAttributeLength("Races", scrapeEntity.getRaces(), 50);
		validateAttributeLength("Remarks", scrapeEntity.getRemarks(), 2000);
		validateAttributeLength("Citizenships", scrapeEntity.getCitizenships(), 50);
		validateAttributeLength("Builds", scrapeEntity.getBuilds(), 200);
		validateAttributeLength("Associations", scrapeEntity.getAssociations(), 200);
		validateAttributeLength("Aliases", scrapeEntity.getAliases(), 300);
		validateAddresses(scrapeEntity.getAddresses());
		validateUrls("imageUrls", scrapeEntity.getImageUrls());
		validateUrls("entityUrls", scrapeEntity.getUrls());
		validateIdentifications(scrapeEntity.getIdentifications());
		validateSources(scrapeEntity.getSources());
	}

	private void validateIdentifications(Set<ScrapeIdentification> identifications) throws ValidationException {
		for (ScrapeIdentification identification : identifications) {
			validateRequired("identification.type", identification.getType());
			validateLength("identification.type", identification.getType(), 100);
			validateRequired("identification.value", identification.getValue());
			validateLength("identification.value", identification.getValue(), 250);
			validateLength("identification.location", identification.getLocation(), 100);
			validateLength("identification.country", identification.getCountry(), 100);
		}
	}

	private void validateAddresses(List<ScrapeAddress> addresses) throws ValidationException {
		for (ScrapeAddress address : addresses) {
			validateLength("address1", address.getAddress1(), 200);
			validateLength("city", address.getCity(), 200);
			validateLength("country", address.getCountry(), 100);
			validateLength("postalCode", address.getPostalCode(), 20);
			validateLength("province", address.getProvince(), 200);

			if (StringUtils.isBlank(address.getAddress1())) {
				validateLength("rawFormat", address.getRawFormat(), 200);
			} else {
				validateLength("rawFormat", address.getRawFormat(), 1020);
			}

			validateLength("type", address.getType(), 10);

			if (StringUtils.isBlank(address.getAddress1()) && StringUtils.isBlank(address.getCity()) &&
					StringUtils.isBlank(address.getCountry()) && StringUtils.isBlank(address.getPostalCode()) &&
					StringUtils.isBlank(address.getProvince()) && StringUtils.isBlank(address.getRawFormat())) {
				throw new ValidationException("address", "All fields are blank");
			}
		}
	}

	private void validateSources(List<ScrapeSource> sources) throws ValidationException {
		for (ScrapeSource source : sources) {
			validateLength("name", source.getName(), 500);
			validateLength("url", source.getUrl(), 1000);
			validateLength("description", source.getDescription(), 1000);

			if (StringUtils.isBlank(source.getName()) && StringUtils.isBlank(source.getUrl()) &&
					StringUtils.isBlank(source.getDescription())) {
				throw new ValidationException("source", "All fields are blank");
			}
		}
	}

	private void validateRequired(String key, String value) throws ValidationException {
		if (StringUtils.isBlank(value) || "null".equals(value)) {
			throw new ValidationException(key, "Required field");
		}
	}

	private void validateUrls(String key, Set<String> urls) throws ValidationException {
		for (String url : urls) {
			if(url == null){
				throw new ValidationException(key, "Please don't send a null URL");
			}
			String temp = url.toString();
			if (!temp.startsWith("http")) {
				String tempLower = temp.toLowerCase();
				if (!tempLower.contains("www.")) {
					boolean matchDomain = false;
					for (String tempDomain : domainTypes) {
						if (tempLower.contains(tempDomain)) {
							matchDomain = true;
							break;
						}
					}
					if (!matchDomain) {
						throw new ValidationException(key, "Url must start with [http] or contain [www.] or [.domain] where domain is valid - Value:" + temp);
					}
				}
			}
			validateLength(key, temp, 2500);
		}
	}

	//    private void validateUrls(String key, Set<CompactCharSequence> urls) throws ValidationException {
	//        for (CompactCharSequence url : urls) {
	//        	String temp = url.toString();
	//            if (!temp.startsWith("http")) {
	//                throw new ValidationException(key, "Url must start with[http] - Value:" + temp);
	//            }
	//            validateLength(key, temp, 200);
	//        }
	//    }

	private void validateAttributeLength(String key, Set<String> attributes, int length) throws ValidationException {
		for (String attribute : attributes) {
			validateLength(key, attribute, length);
		}

	}

	private void validateEvents(List<ScrapeEvent> events) throws ValidationException {
		for (ScrapeEvent event : events) {
			if (StringUtils.isNotBlank(event.getDate())) {
				validateDate("event.date", event.getDate(), false);
			}
			if (StringUtils.isNotBlank(event.getEndDate())) {
				validateDate("event.endDate", event.getEndDate(), true);
			}

			if (StringUtils.isNotBlank(event.getDate()) && StringUtils.isNotBlank(event.getEndDate())) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
				simpleDateFormat.setLenient(false);
				Date startDate;
				Date endDate;
				try {
					startDate = simpleDateFormat.parse(event.getDate());
					endDate = simpleDateFormat.parse(event.getEndDate());
					if(startDate.after(endDate)){
						throw new ValidationException("event.endDate", "Invalid date exception[start:" + event.getDate() + ", end:" + event.getEndDate() + "]: Event end date must be after start date");
					}
				} catch (ParseException e){
					//ignore, any issue would have been caught before
				}
			}

			if (StringUtils.isNotBlank(event.getDescription())) {
				validateLength("event.description", event.getDescription(), 1000);
			}
		}
	}

	private void validateLength(String field, String input, int length) throws ValidationException {
		if (input != null) {
			if (input.length() > length) {
				throw new ValidationException(field, "Invalid length[" + input.length() + "] - greater than[" + length + "] - Value:" + input);
			}
		}

	}

	private void validateDate(String field, String inputDate, boolean allowFutures) throws ValidationException {
		if (inputDate != null) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
			simpleDateFormat.setLenient(false);
			Date date;
			try {
				date = simpleDateFormat.parse(inputDate);
			} catch (ParseException e) {
				throw new ValidationException(field, "Invalid date exception[" + inputDate + "]:" + e.getMessage() + "]");
			}
			String newDate = simpleDateFormat.format(date);
			Calendar input = Calendar.getInstance();
			input.setTime(date);
			Calendar startDate = Calendar.getInstance();
			startDate.set(Calendar.YEAR, 1900);
			startDate.set(Calendar.DATE, 1);
			startDate.set(Calendar.MONTH, 0);
			Calendar endDate = Calendar.getInstance();
			if (!inputDate.equals(newDate) || !input.after(startDate) || (!allowFutures && !input.before(endDate)) || (allowFutures && !input.before(sqlMaxCal))) {
				throw new ValidationException(field, "Invalid date[" + inputDate + "] - must be in mm/dd/yyyy format " + (allowFutures ? "and be before 12/31/9999 " : "and be before now ") + "and after 1/1/1900");
			}
		}
	}

	public static class ValidationException extends Exception {
		private String field;

		public ValidationException(String field, String message) {
			super("Property[" + field + "]:" + message);
			this.field = field;
		}

		public String getField() {
			return field;
		}
	}

	public static void main(String[] args) throws ValidationException {
		EntityValidationService service = new EntityValidationService();
		service.validateDate("test", "04/29/2009", false);
	}
}