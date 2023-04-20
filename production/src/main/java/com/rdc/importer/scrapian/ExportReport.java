package com.rdc.importer.scrapian;

// Copyright © 2008, 2009, 2010, 2012, 2014 Regulatory DataCorp, Inc. (RDC)

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sf.ehcache.Cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.rdc.scrape.ScrapeAddress;
import com.rdc.scrape.ScrapeAlias;
import com.rdc.scrape.ScrapeDeceased;
import com.rdc.scrape.ScrapeDob;
import com.rdc.scrape.ScrapeEntity;
import com.rdc.scrape.ScrapeEntityAssociation;
import com.rdc.scrape.ScrapeEvent;
import com.rdc.scrape.ScrapeIdentification;
import com.rdc.scrape.ScrapePepType;
import com.rdc.scrape.ScrapePosition;
import com.rdc.scrape.ScrapeSource;

public class ExportReport {

	private class MLDate {
		String month;
		String date;
		String year;
		String other;

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (StringUtils.isNotBlank(month)) {
				sb.append(month).append("/");
			}
			if (StringUtils.isNotBlank(date)) {
				sb.append(date).append("/");
			}
			if (StringUtils.isNotBlank(year)) {
				sb.append(year);
			}
			if (StringUtils.isNotBlank(other)) {
				sb.append((sb.length() > 0 ? " " : "") + other);
			}
			return sb.toString();
		}
	}

	class ExpRunnable {
		private short idxIndex = 0;

		private short NAME_IDX = idxIndex++;
		short TYPE_IDX = idxIndex++;
		short SOURCE_IDX = idxIndex++;

		short ADDRESS_IDX = idxIndex++;
		short ALIAS_IDX = idxIndex++;
		short ASSOCIATION_IDX = idxIndex++;
		short BIRTHDATE_IDX = idxIndex++;
		short BUILD_IDX = idxIndex++;
		short CITIZENSHIP_IDX = idxIndex++;
		short CHANGE_TYPE_IDX = idxIndex++;
		short COMPLETED_IDX = idxIndex++;
		short COMPLEXION_IDX = idxIndex++;
		short DECEASED_IDX = idxIndex++;
		short ENTITY_URL_IDX = idxIndex++;
		short EVENT_IDX = idxIndex++;
		short EYE_COLOR_IDX = idxIndex++;
		short HAIR_COLOR_IDX = idxIndex++;
		short HEIGHT_IDX = idxIndex++;
		short IDENTIFICATION_IDX = idxIndex++;
		short IMAGE_URL_IDX = idxIndex++;
		short LANGUAGE_IDX = idxIndex++;
		short NATIONALITY_IDX = idxIndex++;
		short NEGATIVE_NEWS_IDX = idxIndex++;
		short OCCUPATION_IDX = idxIndex++;
		short PEP_SCORE_IDX = idxIndex++;
		short PEP_TYPE_IDX = idxIndex++;
		short PHYSICAL_DESCRIPTION_IDX = idxIndex++;
		short POSITION_IDX = idxIndex++;
		short RACE_IDX = idxIndex++;
		short RELATIONSHIP_IDX = idxIndex++;
		short REMARKS_IDX = idxIndex++;
		short SCARS_MARKS_IDX = idxIndex++;
		short SEX_IDX = idxIndex++;
		short SOURCES_IDX = idxIndex++;
		short WEIGHT_IDX = idxIndex++;
		short USER_IDX = idxIndex++;
		private short UPDATED_IDX = idxIndex++;
		int NUMBER_OF_COLUMNS = idxIndex;

		private static final String DELIM = ",";
		private static final String LINE_FEED = "\n";

		private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		HashMap<String, List> fromRels = new HashMap<String, List>();

		public void toReport(Cache sEntities, String listName, String baseDir, String fileName) {
			try {
				String timeStamp = "_" + new Date().getTime();
				fileName = fileName.replaceAll("^.*?:(/|\\\\)", "") + timeStamp + ".csv";
				String fileNameBase = "EXP_" + fileName.replaceAll("\\..*", "");
				int fileCounter = 1;
				int rowCount = 0;
				boolean closed = false;
				ArrayList<String> files = new ArrayList<String>();
				
				File outfile = new File(baseDir + fileName);
				files.add(fileName);
				
				FileOutputStream fos = new FileOutputStream(outfile);
				String[] row = new String[NUMBER_OF_COLUMNS];
				writeHeader(fos, row, listName);

				byte[] byteBuffer = new byte[30000];
				
	            for (Object key : sEntities.getKeys()) {
	                ScrapeEntity rec = (ScrapeEntity) (sEntities.get(key).getObjectValue());
					for(ScrapeEntityAssociation sea : rec.getScrapeEntityAssociations()){
						ScrapeEntityAssociation seaOut = new ScrapeEntityAssociation();
						seaOut.setId(rec.getId());
						seaOut.setRelationshipType(sea.getRelationshipType());
						seaOut.setHashable(rec.getName(), rec.getType());
						List listy = fromRels.get(sea.getId());
						if(listy == null){
							listy = new ArrayList<ScrapeEntityAssociation>();
							fromRels.put(sea.getId(), listy);
						}
						listy.add(seaOut);
					}
				}

                for (Object key : sEntities.getKeys()) {
                    ScrapeEntity rec = (ScrapeEntity) (sEntities.get(key).getObjectValue());
					if(closed == true){
						outfile = new File(baseDir + fileName.replace(".csv", "_" + fileCounter + ".csv"));
						files.add(fileName.replace(".csv", "_" + fileCounter + ".csv"));
						fileCounter++;
						fos = new FileOutputStream(outfile);
						writeHeader(fos, row, listName);
						closed = false;
					}
					writeSEData(fos, rec, row, byteBuffer);
					if(++rowCount % 100000 == 0){
						fos.close();						
						closed = true;
					}
				}

				if(!closed){
					fos.close();
				}
				String zipFileName = fileNameBase + ".zip";
				ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(baseDir + zipFileName));

				for(String filyName : files){
				zipOutputStream.putNextEntry(new ZipEntry(filyName));

				FileInputStream in = new FileInputStream(baseDir + filyName);

				int len;
				byte[] buffer = new byte[1024];
				while ((len = in.read(buffer)) > 0) {
					zipOutputStream.write(buffer, 0, len);
				}

				in.close();
				zipOutputStream.closeEntry();

				outfile = new File(baseDir + filyName);
				FileUtils.deleteQuietly(outfile);
				}
				zipOutputStream.close();

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		private void writeSEData(OutputStream outputStream, ScrapeEntity scrapeEntity, String[] row, byte[] byteBuffer) throws IOException {
			cleanRow(row);
			row[NAME_IDX] = scrapeEntity.getName();
			row[SOURCE_IDX] = scrapeEntity.getDataSourceId();
			row[TYPE_IDX] = scrapeEntity.getType();
			// row[CHANGE_TYPE_IDX] = scrapeEntity.getChangeType().getName();
			// row[COMPLETED_IDX] = scrapeEntity.isCompleted() ? "Y" : "N";
			processSEAttributes(row, scrapeEntity);
			row[USER_IDX] = "auto";

			Date date = new Date();
			if (date != null) {
				row[UPDATED_IDX] = dateFormat.format(date);
			}
			writeStringBuffer(outputStream, row);
		}

		private void cleanRow(String[] row) {
			for (int ii = 0; ii < row.length; ii++) {
				row[ii] = "";
			}

		}

		void processSEAttributes(String[] row, ScrapeEntity se) {
			if (se == null) {
				return;
			}

			for (ScrapeDob dob : se.getDateOfBirths()) {
				MLDate birthDate = new MLDate();
				birthDate.year = dob.getYear();
				birthDate.month = dob.getMonth();
				birthDate.date = dob.getDay();
				if (dob.getCirca() != null) {
					birthDate.other = "Circa";
				}
				row[BIRTHDATE_IDX] += (StringUtils.isNotBlank(row[BIRTHDATE_IDX]) ? "|" : "") + birthDate.toString();
			}
			for (String nationality : se.getNationalities()) {
				row[NATIONALITY_IDX] += (StringUtils.isNotBlank(row[NATIONALITY_IDX]) ? "|" : "") + nationality;
			}
			for (String citizenship : se.getCitizenships()) {
				row[CITIZENSHIP_IDX] += (StringUtils.isNotBlank(row[CITIZENSHIP_IDX]) ? "|" : "") + citizenship;
			}
			for (String alias : se.getAliases()) {
				row[ALIAS_IDX] += (StringUtils.isNotBlank(row[ALIAS_IDX]) ? "|" : "") + alias;
			}
			for (ScrapeAlias alias : se.getDetailedAliases()) {
				row[ALIAS_IDX] += (StringUtils.isNotBlank(row[ALIAS_IDX]) ? "|" : "") + alias.getName() + (StringUtils.isNotBlank(alias.getQuality()) && !"0".equals(alias.getQuality()) ? ", Q: " + alias.getQuality() : "") + (StringUtils.isNotBlank(alias.getScript()) ? ", S: " + alias.getScript() : "") + ((StringUtils.isNotBlank(alias.getScript()) && StringUtils.isNotBlank(alias.getLanguage())) ? ", L: " + alias.getLanguage() : "");
			}
			for (String occupation : se.getOccupations()) {
				row[OCCUPATION_IDX] += (StringUtils.isNotBlank(row[OCCUPATION_IDX]) ? "|" : "") + occupation;
			}
			for (ScrapePosition position : se.getPositions()) {
				MLDate posDateStart = new MLDate();
				MLDate posDateEnd = new MLDate();
				posDateStart.year = position.getFromYear();
				posDateStart.month = position.getFromMonth();
				posDateStart.date = position.getFromDay();
				String posStartString = posDateStart.toString();
				posDateEnd.year = position.getToYear();
				posDateEnd.month = position.getToMonth();
				posDateEnd.date = position.getToDay();
				String posEndString = posDateEnd.toString();
				row[POSITION_IDX] += (StringUtils.isNotBlank(row[POSITION_IDX]) ? "|" : "") + position.getDescription() + (posStartString.length() > 0 ? " From " + posStartString : "") + (posEndString.length() > 0 ? " To " + posEndString : "");
			}
			for (String language : se.getLanguages()) {
				row[LANGUAGE_IDX] += (StringUtils.isNotBlank(row[LANGUAGE_IDX]) ? "|" : "") + language;
			}
			for (String image : se.getImageUrls()) {
				row[IMAGE_URL_IDX] += (StringUtils.isNotBlank(row[IMAGE_URL_IDX]) ? "|" : "") + image;
			}
			for (String entityUrls : se.getUrls()) {
				row[ENTITY_URL_IDX] += (StringUtils.isNotBlank(row[ENTITY_URL_IDX]) ? "|" : "") + entityUrls;
			}
			for (String association : se.getAssociations()) {
				row[ASSOCIATION_IDX] += (StringUtils.isNotBlank(row[ASSOCIATION_IDX]) ? "|" : "") + association;
			}
			for (ScrapeEvent event : se.getEvents()) {
				String category = event.getCategory();
				String subCategory = event.getSubcategory();
				row[EVENT_IDX] += (StringUtils.isNotBlank(row[EVENT_IDX]) ? "|" : "") + (category != null ? category : "") + (category != null || subCategory != null ? "/" : "") + (subCategory != null ? subCategory : "") + " " + event.getDescription() + (event.getDate() != null ? " From " + event.getDate() : "") + (event.getEndDate() != null ? " To " + event.getEndDate() : "");
			}
			for (String physicalDescription : se.getPhysicalDescriptions()) {
				row[PHYSICAL_DESCRIPTION_IDX] += (StringUtils.isNotBlank(row[PHYSICAL_DESCRIPTION_IDX]) ? "|" : "") + physicalDescription;
			}
			for (String hairColor : se.getHairColors()) {
				row[HAIR_COLOR_IDX] += (StringUtils.isNotBlank(row[HAIR_COLOR_IDX]) ? "|" : "") + hairColor;
			}
			for (String eyeColor : se.getEyeColors()) {
				row[EYE_COLOR_IDX] += (StringUtils.isNotBlank(row[EYE_COLOR_IDX]) ? "|" : "") + eyeColor;
			}
			for (String height : se.getHeights()) {
				row[HEIGHT_IDX] += (StringUtils.isNotBlank(row[HEIGHT_IDX]) ? "|" : "") + height;
			}
			for (String weight : se.getWeights()) {
				row[WEIGHT_IDX] += (StringUtils.isNotBlank(row[WEIGHT_IDX]) ? "|" : "") + weight;
			}
			for (String complexion : se.getComplexions()) {
				row[COMPLEXION_IDX] += (StringUtils.isNotBlank(row[COMPLEXION_IDX]) ? "|" : "") + complexion;
			}
			for (String sex : se.getSexes()) {
				row[SEX_IDX] += (StringUtils.isNotBlank(row[SEX_IDX]) ? "|" : "") + sex;
			}
			for (String build : se.getBuilds()) {
				row[BUILD_IDX] += (StringUtils.isNotBlank(row[BUILD_IDX]) ? "|" : "") + build;
			}
			for (String race : se.getRaces()) {
				row[RACE_IDX] += (StringUtils.isNotBlank(row[RACE_IDX]) ? "|" : "") + race;
			}
			for (String scars : se.getScarsMarks()) {
				row[SCARS_MARKS_IDX] += (StringUtils.isNotBlank(row[SCARS_MARKS_IDX]) ? "|" : "") + scars;
			}
			for (ScrapeDeceased dead : se.getDeceased()) {
				MLDate deceasedDate = new MLDate();
				deceasedDate.year = dead.getYear();
				deceasedDate.month = dead.getMonth();
				deceasedDate.date = dead.getDay();
				if (!dead.getDead()) {
					deceasedDate.other = "NOT DEAD";
				}
				row[DECEASED_IDX] += (StringUtils.isNotBlank(row[DECEASED_IDX]) ? "|" : "") + deceasedDate.toString();
			}
			for (ScrapeIdentification si : se.getIdentifications()) {
				row[IDENTIFICATION_IDX] += (StringUtils.isNotBlank(row[IDENTIFICATION_IDX]) ? "|" : "") + si.getType() + ":" + si.getValue() + (StringUtils.isNotBlank(si.getLocation()) ? ":" + si.getLocation() : "") + (StringUtils.isNotBlank(si.getCountry()) ? ":" + si.getCountry() : "");
			}
			for (ScrapePepType spt : se.getPepType()) {
				row[PEP_TYPE_IDX] += (StringUtils.isNotBlank(row[PEP_TYPE_IDX]) ? "|" : "") + spt.getType() + (StringUtils.isNotBlank(spt.getLevel()) ? ":" + spt.getLevel() : "");
			}
			for (ScrapeEntityAssociation sea : se.getScrapeEntityAssociations()){
				row[RELATIONSHIP_IDX] += (StringUtils.isNotBlank(row[RELATIONSHIP_IDX]) ? "|" : "") + sea.getRelationshipType().getDescription() + " to " + sea.getId() + ":" + sea.getHashable();
			}
			List<ScrapeEntityAssociation> lister = fromRels.get(se.getId());
			if (lister != null) {
				for (ScrapeEntityAssociation sea : lister) {
					row[RELATIONSHIP_IDX] += (StringUtils.isNotBlank(row[RELATIONSHIP_IDX]) ? "|" : "") + sea.getRelationshipType().getDescription() + " from " + sea.getId() + ":" + sea.getHashable();
				}
			}
			for (ScrapeAddress address : se.getAddresses()) {
				row[ADDRESS_IDX] += (StringUtils.isNotBlank(row[ADDRESS_IDX]) ? "|" : "") + (address.getBirthPlace() != null && address.getBirthPlace() ? "BIRTH " : "") + (StringUtils.isNotBlank(address.getAddress1()) ? " S: " + address.getAddress1() + " " : "") + (StringUtils.isNotBlank(address.getCity()) ? " C: " + address.getCity() + " " : "") + (StringUtils.isNotBlank(address.getProvince()) ? " P: " + address.getProvince() + " " : "") + (StringUtils.isNotBlank(address.getCountry()) ? " R: " + address.getCountry() + " " : "") + (StringUtils.isNotBlank(address.getPostalCode()) ? " Z: " + address.getPostalCode() + " " : "").replaceAll("(\\s){2,}", " ");
			}
			for (String remark : se.getRemarks()) {
				row[REMARKS_IDX] += (StringUtils.isNotBlank(row[REMARKS_IDX]) ? "|" : "") + remark;
			}
			for (ScrapeSource source : se.getSources()) {
				row[SOURCES_IDX] += (StringUtils.isNotBlank(row[SOURCES_IDX]) ? "|" : "") + (StringUtils.isNotBlank(source.getName()) ? source.getName() : "") + (StringUtils.isNotBlank(source.getUrl()) ? (StringUtils.isNotBlank(source.getUrl()) ? ":" : "") + source.getUrl() : "") + (StringUtils.isNotBlank(source.getDescription()) ? ":" + source.getDescription() : "");
			}
		}

		private void writeHeader(OutputStream outputStream, String[] row, String listName) throws IOException {
			cleanRow(row);
			writeStringBuffer(outputStream, row);
			cleanRow(row);
			row[0] = "   Monitored List Name";
			writeStringBuffer(outputStream, row);
			cleanRow(row);
			row[0] = "  " + listName;
			writeStringBuffer(outputStream, row);
			row = new String[NUMBER_OF_COLUMNS];
			row[NAME_IDX] = "Name";
			row[TYPE_IDX] = "Type";
			row[SOURCE_IDX] = "Source ID";
			row[ADDRESS_IDX] = "Address";
			row[ALIAS_IDX] = "Alias";
			row[ASSOCIATION_IDX] = "Association";
			row[BIRTHDATE_IDX] = "Birthdate";
			row[BUILD_IDX] = "Build";
			row[CITIZENSHIP_IDX] = "Citizenship";
			row[CHANGE_TYPE_IDX] = "Change_Type";
			row[COMPLETED_IDX] = "Completed";
			row[COMPLEXION_IDX] = "Complexion";
			row[DECEASED_IDX] = "Deceased";
			row[ENTITY_URL_IDX] = "Entity_Url";
			row[EVENT_IDX] = "Event";
			row[EYE_COLOR_IDX] = "Eye_Color";
			row[HAIR_COLOR_IDX] = "Hair_Color";
			row[HEIGHT_IDX] = "Height";
			row[IDENTIFICATION_IDX] = "Identification";
			row[IMAGE_URL_IDX] = "Image_Url";
			row[LANGUAGE_IDX] = "Language";
			row[NATIONALITY_IDX] = "Nationality";
			row[NEGATIVE_NEWS_IDX] = "Negative_News";
			row[OCCUPATION_IDX] = "Occupation";
			row[PEP_SCORE_IDX] = "PEP_Score";
			row[PEP_TYPE_IDX] = "PEP_Type";
			row[PHYSICAL_DESCRIPTION_IDX] = "Physical_Description";
			row[POSITION_IDX] = "Position";
			row[RACE_IDX] = "Race";
			row[RELATIONSHIP_IDX] = "Relationship";
			row[REMARKS_IDX] = "Remarks";
			row[SCARS_MARKS_IDX] = "Scars_Marks";
			row[SEX_IDX] = "Sex";
			row[SOURCES_IDX] = "Sources";
			row[WEIGHT_IDX] = "Weight";
			row[USER_IDX] = "User";
			row[UPDATED_IDX] = "Last Updated";
			writeStringBuffer(outputStream, row);
		}

		private void writeStringBuffer(OutputStream outputStream, String row[]) throws IOException {
			StringBuilder sb = new StringBuilder(1500);
			if (row != null) {
				boolean needsEscaping = false;
				for (String cell : row) {
					if (cell != null) {
						needsEscaping = cell.contains(",") || cell.contains("\"") || cell.contains("\n");
						sb.append((needsEscaping ? "\"" : "") + cell.replaceAll("\"", "\"\"") + (needsEscaping ? "\"" : "")).append(DELIM);
					} else {
						sb.append(DELIM);
					}
				}
				sb.append(LINE_FEED);
				outputStream.write(sb.toString().getBytes());
			}
		}
	}
}
