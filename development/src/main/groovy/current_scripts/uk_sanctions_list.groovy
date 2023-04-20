import com.rdc.importer.scrapian.model.StringSource
import com.rdc.rdcmodel.model.EntityAttributeType
import com.rdc.scrape.*
import org.apache.commons.lang.StringUtils
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFSheet

context.session.escape = true
context.setup([socketTimeout: 60000, connectionTimeout: 10000, retryCount: 5])
//url = context.scriptParams.url;
url = "https://ofsistorage.blob.core.windows.net/publishlive/2022format/ConList.xlsx"

columns = ["name_6", "name_1", "name_2", "name_3", "name_4", "name_5", "title", "name_non_latin_script", "non_latin_script_type", "non_latin_script_language", "dob", "town_of_birth", "country_of_birth",
           "nationality", "passport_number", "passport_details", "national_identification_number", "national_identification_details", "position", "address_1", "address_2", "address_3", "address_4",
           "address_5", "address_6", "post_zip_code", "country", "other_information", "group_type", "alias_type", "alias_quality", "regime", "listed_on", "uk_sanctions_list_date_designated", "last_updated", "group_id"];

def transFormedheaders = columns;
def xlsx = new URL(url).openStream()
Workbook workbook = WorkbookFactory.create(xlsx)
XSSFSheet rows = workbook.getSheetAt(0)
int length = rows.size()

akaList = [];
int i = 0

rows.each() { Row row ->
    if (i > 2) {
        // def groupId = row.group_id.text().trim();
        def groupId = row.getCell(35).toString()

        if (groupId.equals("Group ID")) {
            validateHeader(row, columns);
        } else if (groupId.endsWith(".0")) {
            groupId = removeTralingDecimalAndZero(groupId)
        }

        if (StringUtils.isNotBlank(groupId) && StringUtils.isNumeric(groupId)) {
            if (!row.getCell(29).toString().equalsIgnoreCase("Primary Name")) {
                akaList.add(row)
            } else {
                entity = processEntity(row, groupId, context, false)
            }
        }
    }
    i++
}

akaList.each() { row ->
    //def groupId = row.group_id.text().trim();
    def groupId = row.getCell(35).toString().trim()
    if (groupId.endsWith(".0")) {
        groupId = removeTralingDecimalAndZero(groupId)
    }
    processEntity(row, groupId, context, true);

}

private def processEntity(row, groupId, context, isAlias) {
    def keyList = getKey(groupId);
    def entity = null;
    if (entity == null) {
        entity = context.findEntity(keyList);
    }
    if (entity == null) {
        entity = context.newEntity(keyList);
        entity.setDataSourceId(groupId);
        if (groupId) {
            def scrapeId = new ScrapeIdentification();
            scrapeId.setType("HM Treasury Group ID");
            scrapeId.setValue(groupId);
            entity.addIdentification(scrapeId);
        }
        entity.setType("Individual".equals(row.getCell(28).toString().trim()) ? "P" : "O");
        if (row.getCell(0).toString().contains("ENTERPRISE")) {
            entity.setType("O")
        }
    }
    if (isAlias && entity == null) {
        entity = context.newEntity(keyList);
        entity.setDataSourceId(groupId);
        if (groupId) {
            def scrapeId = new ScrapeIdentification();
            scrapeId.setType("HM Treasury Group ID");
            scrapeId.setValue(groupId);
            entity.addIdentification(scrapeId);
        }
        entity.setType("Individual".equals(row.getCell(28).toString().trim()) ? "P" : "O");
        if (row.getCell(0).toString().contains("ENTERPRISE")) {
            entity.setType("O")
        }
    }

    extractNameAndAliases(entity, row, isAlias)
    extractAddresses(entity, row);
    extractNationalIdAndDetails(entity, row)
    //extractListedDate(entity, row);
    extractEventDate(entity, row);
    // extractAssociations(entity, row);
    extractRemarks(entity, row);

    if (entity.getType().equals("P")) {
        extractDatesOfBirth(entity, row);
        extractPositions(entity, row);
        extractNationalities(entity, row);
        extractBirthAddresses(entity, row);
        extractPassportsIdAndDetails(entity, row);
        extractSex(entity, row);
    }
    processKnowledgeTag(entity)
    return entity;
}

private def getKey(groupId) {
    groupIdKey = [groupId.trim()]
    return groupIdKey;
}

private def extractNameAndAliases(entity, row, isAlias) {
    surname = row.getCell(0).toString().toUpperCase()//row.getCell(0).toString().toUpperCase()
    //[row.name_1.text(), row.name_2.text(), row.name_3.text(), row.name_4.text(), row.name_5.text(), surname]
    //row.alias_type.text().trim()
    name = context.joinStrings([row.getCell(1).toString(), row.getCell(2).toString(), row.getCell(3).toString(), row.getCell(4).toString(), row.getCell(5).toString(), surname]);
    name = sanitinzeName(name)
    if (StringUtils.isNotBlank(name)) {
        if (!StringUtils.isBlank(entity.getName()) && isAlias && row.getCell(29).toString().trim().equalsIgnoreCase("Primary name variation")) {
            entity.addAlias(sanitinzeName(name))
        } else if (StringUtils.isBlank(entity.getName()) && !isAlias && row.getCell(29).toString().trim().equalsIgnoreCase("Primary name")
            || (isAlias && row.getCell(29).toString().trim().equalsIgnoreCase("Primary name variation"))
            || row.getCell(35).toString().trim() == "12644" && surname == "SYRIAN PETROLEUM COMPANY"
        ) {
            if (name.contains("\"")) {
                name = name.replaceAll("\"\$", "").trim()
            }
            if (StringUtils.isNotBlank(name)) {
                entity.setName(name)
            }
        } else if (!name.equals(entity.getName())) {
            entity.addAlias(name)
        }
        alias = sanitinzeName(row.getCell(7).toString().trim())//row.name_non_latin_script.text()
        if (StringUtils.isNotBlank(alias)) {
            entity.addAlias(alias)
        }
        if (entity.name != null) {
            if (entity.name.contains("A.K.A")) {
                def n = entity.name.split("A.K.A")
                entity.name = sanitinzeName(n[0])
                entity.addAlias(sanitinzeName(n[1]))
            }
        }
    }

}

private extractAddresses(entity, row) {

    def address1 = (removeTralingDecimalAndZero(row.getCell(19).toString().trim()))
    def address2 = (removeTralingDecimalAndZero(row.getCell(20).toString().trim()))
    def address3 = (removeTralingDecimalAndZero(row.getCell(21).toString().trim()))
    address3 = convertScientificNotationToLong(address3).toString()
    def address4 = (removeTralingDecimalAndZero(row.getCell(22).toString().trim()))
    def address5 = (removeTralingDecimalAndZero(row.getCell(23).toString().trim()))
    def address6 = (removeTralingDecimalAndZero(row.getCell(24).toString().trim()))
    def postalCode = (removeTralingDecimalAndZero(row.getCell(25).toString().trim()))


    address = context.joinStrings([address1, address2, address3, address4, address5]);
    address = removeGarbage(address)

    //
    city = removeGarbage(address6)
    post = postalCode.replaceAll(/^(?:[^\d]*)?([\d]{1,20})(?:.*)?$/, '$1');
    country = row.getCell(26).toString().trim().replaceAll(/Ukranian SSR/, 'UKRAINE').replaceAll(/^(Autonomous SSR|SSR)$/, 'RUSSIAN FEDERATION');
    if (country.startsWith("United States of America")) {
        country = "United States of America"
    }
    tmatch = city =~ /(?i)(.*?), Uzbekistan/
    if (tmatch.find()) {
        city = tmatch.group(1);
        country = "UZBEKISTAN"
    }
    if (address1 == "Occupies several rooms in the former Ptitsyn-Zalogina estate near the Taganskaya metro station (in Moscow, Russia)") {
        address = "Occupies several rooms in the former Ptitsyn-Zalogina estate near the Taganskaya metro station";
        country = "RUSSIAN FEDERATION";
        city = "Moscow";
    }
    def addresses = removeGarbage(address + city + post + country)

    if (StringUtils.isNotBlank(addresses)) {
        ScrapeAddress scrapeAddress = new ScrapeAddress();
        scrapeAddress.setAddress1(removeGarbage(address.trim()) == "" ? null : address);
        scrapeAddress.setPostalCode(removeGarbage(post.trim()) == "" ? null : post);
        scrapeAddress.setCity(removeGarbage(city.trim()) == "" ? null : city.trim());
        scrapeAddress.setCountry(removeGarbage(country.trim()) == "" ? null : country.trim());
        if (StringUtils.isNotBlank(address) && StringUtils.isBlank(post) && StringUtils.isBlank(city) && StringUtils.isBlank(country)) {
            country2 = address1 =~ /(,[^,]+$)/
            while (country2.find()) {
                if (country2.group(1).find("DPRK")) {
                    scrapeAddress.setCountry("KOREA, DEMOCRATIC PEOPLE'S REPUBLIC OF")
                } else if (country2.group(1).find("Singapore")) {
                    scrapeAddress.setCountry("SINGAPORE")
                } else if (country2.group(1).find("China")) {
                    scrapeAddress.setCountry("CHINA")
                } else if (country2.group(1).find("Somoa")) {
                    scrapeAddress.setCountry("SOMOA")
                } else if (country2.group(1).find("Marshall Islands")) {
                    scrapeAddress.setCountry("Marshall Islands")
                }
                address = removeGarbage(address.replaceAll(country2.group(1), ''))
                if (StringUtils.isNotBlank(address)) {
                    scrapeAddress.setAddress1(address)
                }
            }
        }
        entity.addAddress(scrapeAddress)
    }
}

private extractDatesOfBirth(ScrapeEntity person, row) {
    def dob = row.getCell(10).toString().trim()
    if (dob.startsWith("00/00")) {
        ScrapeDob scrapeDob = new ScrapeDob();
        def parts = dob.split("/");
        if (parts.length == 3) {
            if (parts[2].toInteger() > 0) {
                scrapeDob.setYear(parts[2]);
            } else {
                scrapeDob.setYear(null);
            }
        }

        if (scrapeDob.getYear() != null) {
            person.addDateOfBirth(scrapeDob);
        }
    } else if (dob.startsWith("00/")) {
        ScrapeDob scrapeDob = new ScrapeDob();
        def parts = dob.split("/");
        if (parts.length == 3) {
            scrapeDob.setMonth(parts[1] == "00" ? null : parts[1]);
            scrapeDob.setYear(parts[2] == "0000" ? null : parts[2]);
        }
        if (scrapeDob.getYear() != null) {
            person.addDateOfBirth(scrapeDob);
        }
    } else {
        def parsedDate = context.parseDate(new StringSource(dob), (String[]) ["dd/MM/yyyy"])
        if (StringUtils.isNotBlank(parsedDate)) {
            def parts = parsedDate.split("/");
            if (parts.length == 3) {
                ScrapeDob scrapeDob = new ScrapeDob();
                scrapeDob.setDay(parts[1] == "00" ? null : parts[1]);
                scrapeDob.setMonth(parts[0] == "00" ? null : parts[0]);
                scrapeDob.setYear(parts[2] == "0000" ? null : parts[2]);

                if (scrapeDob.getYear() != null) {
                    person.addDateOfBirth(scrapeDob);
                }
            }
        } else if (StringUtils.isBlank(parsedDate)) {
            parsedDate1 = context.parseDate(new StringSource(dob), (String[]) ["dd-MMM-yyyy"])
            if (StringUtils.isNotBlank(parsedDate1)) {
                def parts = parsedDate1.split("/");
                if (parts.length == 3) {
                    ScrapeDob scrapeDob = new ScrapeDob();
                    scrapeDob.setDay(parts[1] == "00" ? null : parts[1]);
                    scrapeDob.setMonth(parts[0] == "00" ? null : parts[0]);
                    scrapeDob.setYear(parts[2] == "0000" ? null : parts[2]);

                    if (scrapeDob.getYear() != null) {
                        person.addDateOfBirth(scrapeDob);
                    }
                }
            } else if (StringUtils.isBlank(parsedDate1)) {
                parsedDate2 = dob//row.dob.text().trim()
                if (StringUtils.isNotBlank(parsedDate2) && parsedDate2.indexOf('/') < 0 && parsedDate2.length() == 4) {
                    ScrapeDob scrapeDob = new ScrapeDob();
                    scrapeDob.setYear(parsedDate2);
                    person.addDateOfBirth(scrapeDob);
                }

                if (parsedDate2.length() == 7 && parsedDate2.indexOf('/') > 0) {
                    parsedDate3 = context.parseDate(new StringSource(parsedDate2), (String[]) ["MM/yyyy"])
                    if (StringUtils.isNotBlank(parsedDate3)) {
                        def parts = parsedDate3.split("/");
                        if (parts.length == 3) {
                            ScrapeDob scrapeDob = new ScrapeDob();
                            scrapeDob.setDay(null);
                            scrapeDob.setMonth(parts[0] == "00" ? null : parts[0]);
                            scrapeDob.setYear(parts[2] == "0000" ? null : parts[2]);
                            if (scrapeDob.getYear() != null) {
                                person.addDateOfBirth(scrapeDob);
                            }
                        }
                    }
                }
            }
        }
    }
}

private extractBirthAddresses(person, row) {
    rawTown = row.getCell(11).toString().trim();
    rawCountry = row.getCell(12).toString().trim();
    townHasList = (rawTown.contains("(1)") || rawTown.contains("(a)"));
    countryHasList = (rawCountry.contains("(1)") || rawCountry.contains("(a)"));
    townList = (rawTown.split("(?:\\(\\d{1,3}\\)|\\(\\w{1}\\))\\s+") as List);
    countryList = (rawCountry.split("(?:\\(\\d{1,3}\\)|\\(\\w{1}\\))\\s+") as List);
    if (townHasList) {
        townList.remove("");
    }
    if (countryHasList) {
        countryList.remove("");
    }

    if (townHasList && countryHasList && (townList.size() == countryList.size())) {
        townList.eachWithIndex { town, i ->
            createBirthAddress(person, town.trim(), countryList.get(i).trim());
        }
    } else if (townHasList && !countryHasList) {
        townList.each() { town ->
            createBirthAddress(person, town.trim(), rawCountry.trim());
        }
    } else if (!townHasList && countryHasList) {
        countryList.each() { country ->
            createBirthAddress(person, rawTown.trim(), country.trim());
        }
    } else if (!townHasList && !countryHasList) {
        createBirthAddress(person, rawTown.trim(), rawCountry.trim());
    } else if (townHasList && countryHasList && (townList.size() > countryList.size())) {
        country = countryList.get(0);
        townList.eachWithIndex { town, i ->
            mark = "(" + (i + 1) + ")";
            foundIndex = rawCountry.contains(mark);
            if (foundIndex) {
                possibles = rawCountry.substring(rawCountry.indexOf(mark) + mark.length()).trim().split("\\(\\d{1,3}\\)\\s+") as List;
                possibles.remove("to ");
                country = possibles.get(0);
            }
            createBirthAddress(person, town.trim(), country.trim());
        }
    } else {
        createBirthAddress(person, rawTown.trim(), rawCountry.trim());
    }
}

private createBirthAddress(person, town, country) {
    town = removeGarbage(town).trim()
    if (StringUtils.isNotBlank(town) || StringUtils.isNotBlank(country)) {
        ScrapeAddress scrapeAddress = new ScrapeAddress();
        if (StringUtils.isNotBlank(town)) {
            tmatch = town =~ /(?i)(.*?), Uzbekistan/
            if (tmatch.find()) {
                town = tmatch.group(1);
                country = "UZBEKISTAN"
            }
            scrapeAddress.setCity(town);
        }
        country = removeGarbage(country)
        if (StringUtils.isNotBlank(country)) {
            scrapeAddress.setCountry(country.trim().replaceAll(/Ukranian SSR/, 'UKRAINE').replaceAll(/^(Autonomous SSR|SSR)$/, 'RUSSIAN FEDERATION'));
        }
        scrapeAddress.setBirthPlace(Boolean.TRUE);
        if (StringUtils.isNotBlank(scrapeAddress.country)) {
            person.addAddress(scrapeAddress);
        }

    }
}

private extractPositions(entity, row) {
    row.getCell(18).toString().trim().split("\\(\\d{1,3}\\)").each() { pos ->
        pos = removeGarbage(pos)
        if (StringUtils.isNotBlank(pos) && !"Mr".equalsIgnoreCase(pos)) {
            entity.addPosition(pos);
        }
    }
    row.getCell(6).toString().split("\\(\\d{1,3}\\)").each() { pos ->
        pos = removeGarbage(pos)
        if (StringUtils.isNotBlank(pos) && !"Mr".equalsIgnoreCase(pos)) {
            entity.addPosition(pos);
        }
    }
}

private extractNationalities(entity, row) {
    row.getCell(13).toString().split("\\(\\d{1,3}\\)").each() { nation ->
        nation = removeGarbage(nation)
        if (StringUtils.isNotBlank(nation)) {
            entity.addNationality(nation);
        }
    }
}

private extractSex(scrapeEntity, row) {
    def text = row.getCell(27).toString().trim()//row.other_information.text();
    if (scrapeEntity.getType().equals("P") && StringUtils.isNotBlank(text)) {
        if (text.toUpperCase().contains("FEMALE")) {
            scrapeEntity.addSex("F");
        } else if (text.toUpperCase().contains("MALE")) {
            scrapeEntity.addSex("M");
        }
    }
}

private extractNationalIdAndDetails(scrapeEntity, row) {
    details = row.getCell(17).toString().trim()//row.national_identification_details.text().trim()
    details = details.toString().replaceAll(/\s+/, ' ')
    def NID = row.getCell(16).toString().trim()//row.national_identification_number.text()
    NID.split("(?:\\(\\d+\\)|\\(\\w{1}\\))").each() { text ->
        identification = new ScrapeIdentification()
        text = removeGarbage(text)
        if (StringUtils.isNotBlank(text)) {
            text = text.replaceAll(/(\d)\.(\d+?)E\d+/, '$1$2')
                .replaceAll(/(?i)2e\+01081/, '281082701081')
                .replaceAll(/(?i)2.81083E\+11/, '281082701081')
                .replaceAll(/^0+(?=\d+$)/, '')
            text = (removeTralingDecimalAndZero(text))
            identification.type = "National Identification Number"
            identification.value = text
            scrapeEntity.addIdentification(identification);
        }
    }
    details = removeGarbage(details)
    if (StringUtils.isNotBlank(details)) {
        scrapeEntity.addRemark("National Identification Details: $details".trim())
    }
}

private extractRemarks(scrapeEntity, row) {
    text = row.getCell(27).toString().trim();
    text = text.toString().replaceAll(/\(Gender\)\:.+|Gender \[\w+\]/, " ").trim()
    text = text.toString().replaceAll(/(?s)\s+/, ' ')
    if (StringUtils.isNotBlank(text)) {

        if (text =~ /(?i)IMO number/) {
            def imoMatcher = text =~ /(?i)\(?\s*IMO number\s*\)?\s*:\s*(\d{5,15})/
            if (imoMatcher) {
                imoNumber = imoMatcher[0][1].toString().trim()
                identification = new ScrapeIdentification()
                identification.type = "IMO Number"
                identification.value = imoNumber
                scrapeEntity.addIdentification(identification)
            }
        }
        text = text.replaceAll(/(?i)\(?\s*IMO number\s*\)?\s*:\s*(\d{5,15})/, "").replaceAll(/\s+/, " ")
        scrapeEntity.addRemark("Other Info: " + text);
        if (text =~ /Gender\s+/) {
            println(scrapeEntity.name)
            println(text)
            System.exit(1)
        }
    }
}

private extractAssociations(scrapeEntity, row) {
    text = row.getCell(31).toString().trim()//row.regime.text().trim();
    if (StringUtils.isNotBlank(text)) {
        scrapeEntity.addAssociation(text);
    }
}

private extractListedDate(scrapeEntity, row) {
    def text = row.listed_on.text().trim();
    text = text.toString().replaceAll(/\s+/, ' ')
    if (StringUtils.isNotBlank(text)) {
        String listedDate = context.parseDate(new StringSource(text), (String[]) ["dd/MM/yyyy"])
        if (StringUtils.isBlank(listedDate)) {
            listedDate = context.parseDate(new StringSource(text), (String[]) ["dd-MMM-yyyy"])
        }
        if (listedDate) {
            scrapeEntity.addRemark(EntityAttributeType.LISTED_ON.getName() + ": " + listedDate);
        }
    }
}

private extractEventDate(entity, row) {

    def text = row.getCell(33).toString().trim()//row.uk_sanctions_list_date_designated.text()..trim();
    otherInfo = row.getCell(27).toString().trim();

    ScrapeEvent event = new ScrapeEvent()
    if (StringUtils.isNotBlank(text)) {
        String eventDate = context.parseDate(new StringSource(text), (String[]) ["dd/MM/yyyy"])
        if (StringUtils.isBlank(eventDate)) {
            eventDate = context.parseDate(new StringSource(text), (String[]) ["dd-MMM-yyyy"])
        }
        if (eventDate) {
            event.setDate(eventDate)
            event.setDescription("This entity appears on the UK HM Treasury Office of Financial Sanctions Implementation published list of all asset freeze targets.");
            entity.addEvent(event)
        }
    }
}

private extractPassportsIdAndDetails(person, row) {
    passportDetails = row.getCell(15).toString().trim()//row.passport_details.text().trim()
    passportDetails = passportDetails.toString().replaceAll(/\s+/, ' ').trim()
    passportDetails = removeGarbage(passportDetails).trim()
    if (StringUtils.isNotBlank(passportDetails)) {
        person.addRemark("Passport Details: " + passportDetails)
    }
    row.getCell(14).toString().split("(?:\\(\\d+\\)|\\(\\w{1}\\))").each() { passport ->
        addPassportNumber(passport, person)
    }

}

private void addPassportNumber(String passport, person) {
    passport = removeGarbage(passport.toString().trim())
    if (StringUtils.isNotBlank(passport)) {
        identification = new ScrapeIdentification()
        identification.type = "Passport Number"
        passport = removeTralingDecimalAndZero(passport)
        passport = convertScientificNotationToLong(passport)
        identification.value = passport
        //
        if (identification.value.equals("0000092405")) {
            identification.value = "92405"
        }
        person.addIdentification(identification);
    }
}

private validateHeader(row, columns) {
    String rowText = row.text().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    columns.each() { col ->
        colText = col.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (rowText.indexOf(colText) != 0) {
            throw new Exception("UK_SANCTIONS: Invalid Header - Could not find [" + col + "]");
        } else {
            rowText = rowText.substring(colText.length())
        }
    }
}

private removeTralingDecimalAndZero(data) {
    def cleanData = data.trim()
    if (data.endsWith(".0")) {
        cleanData = data.replaceAll("\\.0", "")
    }
    return cleanData;
}

private convertScientificNotationToLong(number) {
    def longPassportNumbers = number.find(/\d\.\d+E\d/)

    if (longPassportNumbers) {
        def bigNumber = new BigDecimal(longPassportNumbers).toBigInteger()
        return bigNumber;
    }
    return number;
}

private void processKnowledgeTag(ScrapeEntity entity) {
    List<ScrapeEvent> temp = entity.events
    if (temp != null) {
        for (ScrapeEvent event : temp) {
            def ids = entity.identifications
            ids.each { idf ->
                if (idf.type.contains("IMO")) {
                    if (event.getDescription()) {
                        event.setEventtags("#Vessel")
                    }
                }
            }
            entity.addEvent(event);
        }
    }
}

def sanitinzeName(String name) {
    name = name.replaceAll(/(?ism)\bnull\b/, "").trim()
    return name.replaceAll(/(?ism)\s+/, " ")
}

String removeGarbage(String data) {
    data = data.replaceAll(/\bnull\b/, "")
    data = data.replaceAll(/^\W+|\,\W*$|^\s*\,|^-|\s*-\s*$/, "")
    return data.replaceAll(/(?s)\s+/, " ").trim()
}