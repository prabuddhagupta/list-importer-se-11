package com.rdc.scrape;

import com.rdc.core.nameparser.PersonName;
import com.rdc.core.nameparser.PersonNameParser;
import com.rdc.core.nameparser.PersonNameParserImpl;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.Serializable;
import java.util.*;

public class ScrapeEntity implements Serializable {
    private String id;
    private String name;
    private String scrubbedName;
    private String type;
    private List<ScrapeAddress> addresses = new ArrayList<ScrapeAddress>();
    private List<ScrapeDob> dateOfBirths = new ArrayList<ScrapeDob>();
    private List<ScrapeEvent> events = new ArrayList<>();
    private List<ScrapeSource> sources = new ArrayList<ScrapeSource>();
    private List<ScrapeDeceased> deceased = new ArrayList<ScrapeDeceased>();
    private Set<ScrapePosition> positions = new LinkedHashSet<ScrapePosition>();
    private Set<ScrapeIdentification> identifications = new LinkedHashSet<ScrapeIdentification>();
    private Set<ScrapePepType> pepTypes = new LinkedHashSet<ScrapePepType>();
    private Set<String> remarks = new LinkedHashSet<String>();
    private Set<String> languages = new LinkedHashSet<String>();
    private Set<String> citizenships = new LinkedHashSet<String>();
    private Set<String> nationalities = new LinkedHashSet<String>();
    private Set<String> urls = new LinkedHashSet<String>();
    private Set<String> imageUrls = new LinkedHashSet<String>();
    private Set<String> aliases = new LinkedHashSet<String>();
    private Set<ScrapeAlias> detailedAliases = new LinkedHashSet<ScrapeAlias>();
    private Set<String> occupations = new LinkedHashSet<String>();
    private Set<String> heights = new LinkedHashSet<String>();
    private Set<String> weights = new LinkedHashSet<String>();
    private Set<String> eyeColors = new LinkedHashSet<String>();
    private Set<String> complexions = new LinkedHashSet<String>();
    private Set<String> hairColors = new LinkedHashSet<String>();
    private Set<String> scarsMarks = new LinkedHashSet<String>();
    private Set<String> sexes = new LinkedHashSet<String>();
    private Set<String> builds = new LinkedHashSet<String>();
    private Set<String> races = new LinkedHashSet<String>();
    private Set<String> physicalDescriptions = new LinkedHashSet<String>();
    private Set<String> associations = new LinkedHashSet<String>();
    private Set<ScrapeEntityAssociation> scrapeEntitiesAssociations = new LinkedHashSet<ScrapeEntityAssociation>(); //these are the RELATIONSHIPS
    private String dataSourceId;
    private byte[] hash;
    private static PersonNameParser personNameParser;
    private MessageDigest algorithm;
    private boolean prettyPrint = true;


    public void makeHash() throws Exception {
        String temp = toXml(true);
        byte[] encodedPassword = temp.getBytes("UTF-8");
        if (algorithm == null) {
            try {
                algorithm = MessageDigest.getInstance("SHA-256");
            } catch (final NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
        algorithm.reset();
        hash = algorithm.digest(encodedPassword);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setScrapeEntityAssociations(Set<ScrapeEntityAssociation> scrapeEntitiesAssociations) {
        this.scrapeEntitiesAssociations = scrapeEntitiesAssociations;
    }

    public void setAddresses(List<ScrapeAddress> addresses) {
        this.addresses = addresses;
    }

    public String getScrubbedName() {
        if (scrubbedName == null && name != null) {
            scrubbedName = name.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim();
        }
        return scrubbedName;
    }

    public void setPersonName(PersonName personName) {
        this.name = getPersonNameParser().formatName(personName);
        scrubbedName = name.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }

    public void setOrgName(String name) {
        this.name = name;
        scrubbedName = name.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }

    /**
     * @deprecated For people, the ordering of name pieces is
     * important.  This method doesn't make that
     * clear.  Use {@link #setPersonName(PersonName)}
     * or {@link #setOrgName(String)} instead. To create
     * a PersonName, you can use
     * {@link com.rdc.importer.scrapian.ScrapianContext#parseName(String...)},
     * or you can construct one yourself.
     */
    @Deprecated
    public void setName(String name) {
        if (name != null) {
            name = name.replaceAll("\\s+", " ");
        }
        this.name = name;
        scrubbedName = name.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public ScrapeIdentification newIdentification() {
        ScrapeIdentification id = new ScrapeIdentification();
        identifications.add(id);
        return id;
    }

    public Set<ScrapeIdentification> getIdentifications() {
        return identifications;
    }

    public void addIdentification(ScrapeIdentification identification) {
        identifications.add(identification);
    }

    public Set<ScrapePepType> getPepType() {
        return pepTypes;
    }

    public void addPepType(ScrapePepType pepType) {
        pepTypes.add(pepType);
    }

    public ScrapeAddress newAddress() {
        ScrapeAddress address = new ScrapeAddress();
        addresses.add(address);
        return address;
    }

    public void addAddress(ScrapeAddress scrapeAddress) {
        if (!addresses.contains(scrapeAddress)) {
            addresses.add(scrapeAddress);
        }
    }

    public void addAddresses(List<ScrapeAddress> scrapeAddresses) {
        for (ScrapeAddress scrapeAddress : scrapeAddresses) {
            if (!addresses.contains(scrapeAddress)) {
                addresses.add(scrapeAddress);
            }
        }
    }

    public List<ScrapeAddress> getAddresses() {
        return addresses;
    }

    public ScrapeEvent newEvent() {
        ScrapeEvent event = new ScrapeEvent();
        events.add(event);
        return event;
    }

    public void addEvent(ScrapeEvent event) {
        if (!events.contains(event)) {
            events.add(event);
        }
    }

    public List<ScrapeEvent> getEvents() {
        return events;
    }

    public ScrapeSource newSource() {
        ScrapeSource src = new ScrapeSource();
        sources.add(src);
        return src;
    }

    public ScrapeAlias newDetailedAlias() {
        ScrapeAlias alias = new ScrapeAlias();
        detailedAliases.add(alias);
        return alias;
    }

    public void addSource(ScrapeSource scrapeSource) {
        if (!sources.contains(scrapeSource)) {
            sources.add(scrapeSource);
        }
    }

    public List<ScrapeSource> getSources() {
        return sources;
    }

    public ScrapeDob newDateOfBirth() {
        ScrapeDob dob = new ScrapeDob();
        dateOfBirths.add(dob);
        return dob;
    }

    /**
     * @param date See {@link ScrapeDob#ScrapeDob(String)}.
     */
    public void addDateOfBirth(String date) {
        ScrapeDob dob = new ScrapeDob(date);
        addDateOfBirth(dob);
    }

    public void addDateOfBirth(ScrapeDob dateOfBirth) {
        if (!dateOfBirths.contains(dateOfBirth)) {
            dateOfBirths.add(dateOfBirth);
        }
    }

    public List<ScrapeDob> getDateOfBirths() {
        return dateOfBirths;
    }

    public void addScrapeEntityAssociation(ScrapeEntityAssociation scrapeEntityAssociation) {
        scrapeEntitiesAssociations.add(scrapeEntityAssociation);
    }

    public Set<ScrapeEntityAssociation> getScrapeEntityAssociations() {
        return scrapeEntitiesAssociations;
    }

    public void addAssociation(String asociation) {
        associations.add(asociation);
    }

    public void addRemark(String remark) {
        if (remark != null && remark.length() > 2000) {
            remark = WordUtils.abbreviate(remark, 1975, 1997, "...");
        }
        remarks.add(remark);
    }

    public void addLanguage(String value) {
        languages.add(value);
    }

    public void addLanguages(String values) {
        languages.addAll(Arrays.asList(values.split(",")));
    }

    public void addCitizenship(String citizenship) {
        citizenships.add(citizenship);
    }

    public void addNationality(String value) {
        nationalities.add(value);
    }

    public void addUrl(String value) {
        urls.add(value);
    }

    public void addImageUrl(String value) {
        imageUrls.add(value);
    }

    public void addPersonAlias(PersonName alias) {
        String aliasFormatted = getPersonNameParser().formatName(alias).trim();
        if (name == null || !name.trim().equalsIgnoreCase(aliasFormatted)) {
            aliases.add(aliasFormatted);
        }
    }

    public void addOrgAlias(String alias) {
        if (name == null || !name.trim().equalsIgnoreCase(alias.trim())) {
            aliases.add(alias);
        }
    }

    /**
     * @deprecated See {@link #setName(String)}.
     */
    @Deprecated
    public void addAlias(String value) {
        if (name == null || !name.trim().equalsIgnoreCase(value.trim())) {
            aliases.add(value);
        }
    }

    public void addDetailedAlias(ScrapeAlias value) {
        if (name == null || !name.trim().equalsIgnoreCase(value.getName().trim()) ||
            ((StringUtils.equalsIgnoreCase(value.getScript(), "Latn") && !name.trim().equals(value.getName().trim())))) {
            detailedAliases.add(value);
        }
    }

    public void addPosition(String value) {
        ScrapePosition position = new ScrapePosition();
        position.setDescription(value);
        positions.add(position);
    }

    public void addPosition(ScrapePosition position) {
        positions.add(position);
    }

    public void addOccupation(String value) {
        occupations.add(value);
    }

    public void addHeight(String value) {
        heights.add(value);
    }

    public void addWeight(String value) {
        weights.add(value);
    }

    public void addEyeColor(String value) {
        eyeColors.add(value);
    }

    public void addComplexion(String value) {
        complexions.add(value);
    }

    public void addHairColor(String value) {
        hairColors.add(value);
    }

    public void addScarsMarks(String value) {
        scarsMarks.add(value);
    }

    public void addSex(String value) {
        sexes.add(value);
    }

    public void addBuild(String value) {
        builds.add(value);
    }

    public void addRace(String value) {
        races.add(value);
    }

    public void addPhysicalDescription(String value) {
        physicalDescriptions.add(value);
    }

    public Set<String> getRemarks() {
        return remarks;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public Set<String> getNationalities() {
        return nationalities;
    }

    public Set<String> getCitizenships() {
        return citizenships;
    }

    public Set<String> getUrls() {
        return urls;
    }

    public Set<String> getImageUrls() {
        return imageUrls;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public Set<ScrapeAlias> getDetailedAliases() {
        return detailedAliases;
    }

    public Set<ScrapePosition> getPositions() {
        return positions;
    }

    public Set<String> getOccupations() {
        return occupations;
    }

    public Set<String> getHeights() {
        return heights;
    }

    public Set<String> getWeights() {
        return weights;
    }

    public Set<String> getEyeColors() {
        return eyeColors;
    }

    public Set<String> getComplexions() {
        return complexions;
    }

    public Set<String> getHairColors() {
        return hairColors;
    }

    public Set<String> getScarsMarks() {
        return scarsMarks;
    }

    public Set<String> getSexes() {
        return sexes;
    }

    public Set<String> getBuilds() {
        return builds;
    }

    public Set<String> getRaces() {
        return races;
    }

    public Set<String> getPhysicalDescriptions() {
        return physicalDescriptions;
    }

    public Set<String> getAssociations() {
        return associations;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public ScrapeDeceased newDeceased() {
        ScrapeDeceased deceasedNew = new ScrapeDeceased();
        deceased.add(deceasedNew);
        return deceasedNew;
    }

    public void addDeceased(ScrapeDeceased deceasedIn) {
        if (!deceased.contains(deceasedIn)) {
            deceased.add(deceasedIn);
        }
    }

    public List<ScrapeDeceased> getDeceased() {
        return deceased;
    }

    public void setDeceased(List<ScrapeDeceased> deceased) {
        this.deceased = deceased;
    }

    public String toXml(boolean escape) throws Exception {
        return toXml(escape, false, true);
    }

    public String toXml(boolean escape, boolean escapeSpecial) throws Exception {
        return toXml(escape, escapeSpecial, true);
    }

    public String toXml(boolean escape, boolean escapeSpecial, boolean prettyPrint) throws Exception {
        this.prettyPrint = prettyPrint;
        StringBuffer xmlBuffer = new StringBuffer();
        xmlBuffer.append((prettyPrint ? "\t" : "") + "<entity>" + (prettyPrint ? "\n" : ""));
        if (StringUtils.isBlank(name)) {
            throw new Exception("Entity name cannot be blank");
        }
        xmlBuffer.append((prettyPrint ? "\t\t" : "") + "<name>").append(escape ? StringEscapeUtils.escapeXml(name) : (escapeSpecial ? escapeSpecial(name) : name)).append("</name>" + (prettyPrint ? "\n" : ""));
        if (id != null) {
            xmlBuffer.append((prettyPrint ? "\t\t" : "") + "<id>").append(escape ? StringEscapeUtils.escapeXml(id) : (escapeSpecial ? escapeSpecial(id) : id)).append("</id>" + (prettyPrint ? "\n" : ""));
        }
        if (type != null) {
            xmlBuffer.append((prettyPrint ? "\t\t" : "") + "<type>").append(type).append("</type>" + (prettyPrint ? "\n" : ""));
        }
        if (dataSourceId != null) {
            xmlBuffer.append((prettyPrint ? "\t\t" : "") + "<data_source_id>").append(dataSourceId).append("</data_source_id>" + (prettyPrint ? "\n" : ""));
        }
        for (ScrapeAddress address : addresses) {
            StringBuffer addressNode = new StringBuffer();
            addressNode.append((prettyPrint ? "\t\t" : "") + "<address>" + (prettyPrint ? "\n" : ""));
            addNode(addressNode, "raw_format", address.getRawFormat(), escape, escapeSpecial);
            addNode(addressNode, "address1", address.getAddress1(), escape, escapeSpecial);
            addNode(addressNode, "city", address.getCity(), escape, escapeSpecial);
            addNode(addressNode, "province", address.getProvince(), escape, escapeSpecial);
            addNode(addressNode, "postal_code", address.getPostalCode(), escape, escapeSpecial);
            addNode(addressNode, "country", address.getCountry(), escape, escapeSpecial);
            addNode(addressNode, "type", address.getType(), escape, escapeSpecial);
            addNode(addressNode, "birth_place", address.getBirthPlace());
            addressNode.append((prettyPrint ? "\t\t" : "") + "</address>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(addressNode.toString());
        }
        for (ScrapeEvent event : events) {
            StringBuffer eventNode = new StringBuffer();
            eventNode.append((prettyPrint ? "\t\t" : "") + "<event>" + (prettyPrint ? "\n" : ""));
            addNode(eventNode, "description", event.getDescription(), escape, escapeSpecial);
            addNode(eventNode, "date", event.getDate(), escape, escapeSpecial);
            addNode(eventNode, "end_date", event.getEndDate(), escape, escapeSpecial);
            addNode(eventNode, "category", event.getCategory(), escape, escapeSpecial);
            addNode(eventNode, "subcategory", event.getSubcategory(), escape, escapeSpecial);
            addNode(eventNode, "eventtags", event.getEventtags(), escape, escapeSpecial);
            eventNode.append((prettyPrint ? "\t\t" : "") + "</event>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(eventNode.toString());
        }
        for (ScrapeDob dob : dateOfBirths) {
            StringBuffer dobNode = new StringBuffer();
            dobNode.append((prettyPrint ? "\t\t" : "") + "<dob>" + (prettyPrint ? "\n" : ""));
            addNode(dobNode, "year", dob.getYear(), escape, escapeSpecial);
            addNode(dobNode, "month", dob.getMonth(), escape, escapeSpecial);
            addNode(dobNode, "day", dob.getDay(), escape, escapeSpecial);
            addNode(dobNode, "circa", dob.getCirca());
            dobNode.append((prettyPrint ? "\t\t" : "") + "</dob>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(dobNode.toString());
        }
        for (ScrapeDeceased deceasee : deceased) {
            StringBuffer deceasedNode = new StringBuffer();
            deceasedNode.append((prettyPrint ? "\t\t" : "") + "<deceased>" + (prettyPrint ? "\n" : ""));
            addNode(deceasedNode, "year", deceasee.getYear(), escape, escapeSpecial);
            addNode(deceasedNode, "month", deceasee.getMonth(), escape, escapeSpecial);
            addNode(deceasedNode, "day", deceasee.getDay(), escape, escapeSpecial);
            addNode(deceasedNode, "dead", deceasee.getDead());
            deceasedNode.append((prettyPrint ? "\t\t" : "") + "</deceased>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(deceasedNode.toString());
        }

        for (ScrapeSource source : sources) {
            StringBuffer sourceNode = new StringBuffer();
            sourceNode.append((prettyPrint ? "\t\t" : "") + "<source>" + (prettyPrint ? "\n" : ""));
            addNode(sourceNode, "name", source.getName(), escape, escapeSpecial);
            addNode(sourceNode, "url", source.getUrl(), escape, escapeSpecial);
            addNode(sourceNode, "description", source.getDescription(), escape, escapeSpecial);
            sourceNode.append((prettyPrint ? "\t\t" : "") + "</source>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(sourceNode.toString());
        }

        appendXmlNodes(xmlBuffer, "url", urls, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "image_url", imageUrls, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "alias", aliases, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "language", languages, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "citizenship", citizenships, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "nationality", nationalities, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "occupation", occupations, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "height", heights, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "weight", weights, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "eye_color", eyeColors, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "complexion", complexions, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "hair_color", hairColors, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "scars_marks", scarsMarks, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "sex", sexes, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "build", builds, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "race", races, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "physical_description", physicalDescriptions, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "remark", remarks, escape, escapeSpecial);
        appendXmlNodes(xmlBuffer, "association", associations, escape, escapeSpecial);
        for (ScrapeIdentification identification : identifications) {
            StringBuffer idNode = new StringBuffer();
            idNode.append((prettyPrint ? "\t\t" : "") + "<identification>" + (prettyPrint ? "\n" : ""));
            addNode(idNode, "type", identification.getType(), escape, escapeSpecial);
            addNode(idNode, "location", identification.getLocation(), escape, escapeSpecial);
            addNode(idNode, "country", identification.getCountry(), escape, escapeSpecial);
            addNode(idNode, "value", identification.getValue(), escape, escapeSpecial);
            idNode.append((prettyPrint ? "\t\t" : "") + "</identification>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(idNode.toString());
        }
        for (ScrapePepType pepType : pepTypes) {
            StringBuffer idNode = new StringBuffer();
            idNode.append((prettyPrint ? "\t\t" : "") + "<pepType>" + (prettyPrint ? "\n" : ""));
            addNode(idNode, "type", pepType.getType(), escape, escapeSpecial);
            addNode(idNode, "level", pepType.getLevel(), escape, escapeSpecial);
            idNode.append((prettyPrint ? "\t\t" : "") + "</pepType>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(idNode.toString());
        }
        for (ScrapeAlias alias : detailedAliases) {
            StringBuffer idNode = new StringBuffer();
            idNode.append((prettyPrint ? "\t\t" : "") + "<detailedAlias>" + (prettyPrint ? "\n" : ""));
            addNode(idNode, "name", alias.getName(), escape, escapeSpecial);
            if (alias.getType() != null) {
                addNode(idNode, "type", alias.getType().getCode(), escape, escapeSpecial);
            }
            addNode(idNode, "script", alias.getScript(), escape, escapeSpecial);
            addNode(idNode, "language", alias.getLanguage(), escape, escapeSpecial);
            addNode(idNode, "quality", alias.getQuality(), escape, escapeSpecial);
            idNode.append((prettyPrint ? "\t\t" : "") + "</detailedAlias>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(idNode.toString());
        }
        for (ScrapeEntityAssociation sea : scrapeEntitiesAssociations) {
            StringBuffer idNode = new StringBuffer();
            idNode.append((prettyPrint ? "\t\t" : "") + "<relationship>" + (prettyPrint ? "\n" : ""));
            addNode(idNode, "description", sea.getRelationshipType().getCode(), escape, escapeSpecial);
            addNode(idNode, "id", sea.getId(), escape, escapeSpecial);
            addNode(idNode, "hashable", sea.getHashable(), escape, escapeSpecial);
            idNode.append((prettyPrint ? "\t\t" : "") + "</relationship>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(idNode.toString());
        }
        for (ScrapePosition pos : positions) {
            StringBuffer idNode = new StringBuffer();
            idNode.append((prettyPrint ? "\t\t" : "") + "<position>" + (prettyPrint ? "\n" : ""));
            addNode(idNode, "description", pos.getDescription(), escape, escapeSpecial);
            addNode(idNode, "fromYear", pos.getFromYear(), escape, escapeSpecial);
            addNode(idNode, "fromMonth", pos.getFromMonth(), escape, escapeSpecial);
            addNode(idNode, "fromDay", pos.getFromDay(), escape, escapeSpecial);
            addNode(idNode, "toYear", pos.getToYear(), escape, escapeSpecial);
            addNode(idNode, "toMonth", pos.getToMonth(), escape, escapeSpecial);
            addNode(idNode, "toDay", pos.getToDay(), escape, escapeSpecial);
            idNode.append((prettyPrint ? "\t\t" : "") + "</position>" + (prettyPrint ? "\n" : ""));
            xmlBuffer.append(idNode.toString());
        }
        xmlBuffer.append((prettyPrint ? "\t" : "") + "</entity>");
        return xmlBuffer.toString();
    }

    private void addNode(StringBuffer buffer, String nodeName, Boolean value) {
        if (value != null) {
            buffer.append((prettyPrint ? "\t\t\t" : "") + "<").append(nodeName).append(">").append(value.toString()).append("</").append(nodeName).append(">" + (prettyPrint ? "\n" : ""));
        }
    }

    private void appendXmlNodes(StringBuffer xmlBuffer, String elementName, Set<String> values, boolean escape, boolean escapeSpecial) {
        if (values != null && values.size() > 0) {
            StringBuffer node = new StringBuffer();
            for (String value : values) {
                if (escape) {
                    value = StringEscapeUtils.escapeXml(value);
                } else if (escapeSpecial) {
                    value = escapeSpecial(value);
                }
                node.append((prettyPrint ? "\t\t" : "") + "<").append(elementName).append(">").append(value).append("</").append(elementName).append(">" + (prettyPrint ? "\n" : ""));
            }
            xmlBuffer.append(node.toString());
        }
    }

    private void addNode(StringBuffer buffer, String nodeName, String value, boolean escape, boolean escapeSpecial) {
        if (value != null && !value.equals("")) {
            if (escape) {
                value = StringEscapeUtils.escapeXml(value);
            } else if (escapeSpecial) {
                value = escapeSpecial(value);
            }
            buffer.append((prettyPrint ? "\t\t\t" : "") + "<").append(nodeName).append(">").append(value).append("</").append(nodeName).append(">" + (prettyPrint ? "\n" : ""));
        }
    }

    private PersonNameParser getPersonNameParser() {
        if (personNameParser == null) {
            personNameParser = new PersonNameParserImpl();
        }
        return personNameParser;
    }

    private String escapeSpecial(String in) {
        if (in == null) return in;
        return in.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;");
    }

    @Override
    public String toString() {
        return "ScrapeEntity [id=" + id + ", name=" + name + ", scrubbedName=" + scrubbedName + ", type=" + type + ", addresses=" + addresses
            + ", dateOfBirths=" + dateOfBirths + ", events=" + events + ", sources=" + sources + ", deceased=" + deceased + ", identifications="
            + identifications + ", remarks=" + remarks + ", languages=" + languages + ", citizenships=" + citizenships + ", nationalities="
            + nationalities + ", urls=" + urls + ", imageUrls=" + imageUrls + ", aliases=" + aliases + ", positions=" + positions
            + ", occupations=" + occupations + ", heights=" + heights + ", weights=" + weights + ", eyeColors=" + eyeColors + ", complexions="
            + complexions + ", hairColors=" + hairColors + ", scarsMarks=" + scarsMarks + ", sexes=" + sexes + ", builds=" + builds + ", races="
            + races + ", physicalDescriptions=" + physicalDescriptions + ", associations=" + associations + ", scrapeEntitiesAssociations="
            + scrapeEntitiesAssociations + ", dataSourceId=" + dataSourceId + ", hash=" + Arrays.toString(hash) + ", algorithm=" + algorithm
            + "]";
    }

    /**
     * These are useful (for deduping on name, type, url and DOB), but do NOT contain ALL the fields
     @Override public boolean equals(Object o) {
     if (this == o) return true;
     if (o == null || getClass() != o.getClass()) return false;

     ScrapeEntity that = (ScrapeEntity) o;

     if (!name.equals(that.name)) return false;
     if (!type.equals(that.type)) return false;
     if (dateOfBirths != null ? !dateOfBirths.equals(that.dateOfBirths) : that.dateOfBirths != null) return false;
     return urls.equals(that.urls);

     }

     @Override public int hashCode() {
     int result = name.hashCode();
     result = 31 * result + type.hashCode();
     result = 31 * result + (dateOfBirths != null ? dateOfBirths.hashCode() : 0);
     result = 31 * result + urls.hashCode();
     return result;
     }
     */
}
