package com.rdc.importer.scrapian.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import org.springframework.stereotype.Component;
import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.scrape.ScrapeAddress;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

@Component
public class AddressParser {
    private List<String> usSubDivisionCodes = new ArrayList<String>();

    private Pattern postalCodePattern = Pattern.compile("(.*?)[,\\s](\\d{5}(-\\d{4})?$)");

    public List<ScrapeAddress> parseAddresses(StringSource addressSource, String addressSeparator, String addressPartSeparator, boolean birth, boolean escape) {
        String addressString = unescapeXml(addressSource.toString(), escape);
        List<ScrapeAddress> addresses = new ArrayList<ScrapeAddress>();
        for (String address : addressString.split(addressSeparator)) {
            ScrapeAddress scrapeAddress = parseAddress(new StringSource(address), addressPartSeparator, birth, escape);
            if (scrapeAddress != null) {
                addresses.add(scrapeAddress);
            }
        }
        return addresses;
    }

    public ScrapeAddress parseAddress(StringSource address, String separator, boolean birth, boolean escape) {
        if (address.isBlank()) {
            return null;
        }

        String addressString = unescapeXml(address.toString(), escape).trim();

        Matcher matcher = postalCodePattern.matcher(addressString);
        String postal = null;
        if (matcher.matches()) {
            postal = matcher.group(2);
            addressString = matcher.group(1);
        }

        List<String> addressParts = new ArrayList<String>();
        for (String part : addressString.split(separator)) {
            addressParts.add(part.trim());
        }

        int countryIndex = -1;
        String country = null;
        for (int i = addressParts.size() - 1; i >= 0; i--) {
            String addressPart = addressParts.get(i);

                country = addressPart;
                addressParts.remove(i);
                countryIndex = i;
        }
        // remove all parts to the right of country
        if (countryIndex != -1 && addressParts.size() > countryIndex) {
            addressParts = addressParts.subList(0, countryIndex);
        }

        String province = null;
        for (int i = addressParts.size() - 1; i >= 0; i--) {
            String addressPart = addressParts.get(i);
                    province = addressPart;
                    addressParts.remove(i);
                    break;
        }

        ScrapeAddress scrapeAddress = new ScrapeAddress();
        if (country != null || province != null) {
            if (postal != null) {
                scrapeAddress.setPostalCode(postal);
            }
            if (province != null) {
                scrapeAddress.setProvince(escapeXml(province, escape));
            }
            if (country != null) {
                scrapeAddress.setCountry(escapeXml(country, escape));
            }

            if (addressParts.size() == 1 && !Pattern.compile("\\d").matcher(addressParts.get(0)).find()) {
                String city = addressParts.get(0);
                scrapeAddress.setCity(escapeXml(city, escape));
            } else if (addressParts.size() > 0) {
                String remainnigAddressParts = StringUtils.join(addressParts, ", ");
                scrapeAddress.setRawFormat(escapeXml(remainnigAddressParts, escape));
            }
        } else {
            scrapeAddress.setRawFormat(address.toString());
        }

        scrapeAddress.setBirthPlace(birth);
        return scrapeAddress;
    }


    private String unescapeXml(String s, boolean escape) {
        if (!escape) {
            return StringEscapeUtils.unescapeXml(s);
        }
        return s;
    }

    private String escapeXml(String s, boolean escape) {
        if (!escape) {
            return StringEscapeUtils.escapeXml(s);
        }
        return s;
    }
}
