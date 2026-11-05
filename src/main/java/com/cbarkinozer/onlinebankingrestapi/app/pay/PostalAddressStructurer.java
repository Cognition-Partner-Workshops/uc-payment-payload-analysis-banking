package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits legacy free-text address lines into structured ISO 20022 elements using fixed
 * layout patterns only. Every value comes verbatim from the input lines; there is no town
 * list, so a line that does not show where the town is never yields a town.
 */
public class PostalAddressStructurer {
    static final int MAX_ADDRESS_LINES = 2;

    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private static final String DESIGNATORS = "Lane|Street|St|Road|Rd|Avenue|Ave|Way|Drive|Close|Court|Place|"
            + "Square|Row|Terrace|Crescent|Hill|Gardens|Grove|Walk|Parade|Boulevard|Quay|Wharf";
    private static final Pattern STREET = Pattern.compile(
            "(\\d+[A-Za-z]?) ((?:[A-Za-z'-]+ )*?(?:" + DESIGNATORS + "))(?:(,)? (.+))?");
    private static final Pattern POST_BOX = Pattern.compile("(?i)P\\.? ?O\\.? Box [A-Za-z0-9]+");
    private static final Pattern POSTCODE = Pattern.compile(
            "\\d{4,5}|[A-Za-z]{1,2}\\d[A-Za-z\\d]? \\d[A-Za-z]{2}");
    private static final Pattern LEADING_POSTCODE = Pattern.compile("(" + POSTCODE + ") (.+)");
    private static final Pattern TRAILING_POSTCODE = Pattern.compile("(.+?) (" + POSTCODE + ")");
    private static final Pattern WORDS = Pattern.compile("[A-Za-z][A-Za-z' -]*");

    public PostalAddress structure(String role, PaymentInstruction.Party party) {
        PostalAddress address = new PostalAddress();
        List<String> lines = new ArrayList<>();
        party.getAddressLines().forEach(line -> lines.add(line.trim().replaceAll(" +", " ")));
        List<String> unclassified = new ArrayList<>();
        String statedCountry = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] tokens = line.split(" ");
            String last = tokens[tokens.length - 1];
            if (COUNTRIES.contains(last)) {
                if (statedCountry != null && !statedCountry.equals(last)) {
                    throw new AddressIncompleteException(role, "states two countries (" + statedCountry + ", " + last + ")");
                }
                statedCountry = last;
                line = line.substring(0, Math.max(0, line.length() - 3)).trim();
                if (line.isEmpty()) continue;
            }
            if (POST_BOX.matcher(line).matches()) {
                address.setPostBox(set(role, "post box", address.getPostBox(), line));
                continue;
            }
            String postCode = null;
            String rest = line;
            Matcher m;
            if (POSTCODE.matcher(line).matches()) {
                postCode = line;
                rest = "";
            } else if ((m = LEADING_POSTCODE.matcher(line)).matches() && !STREET.matcher(line).matches()) {
                postCode = m.group(1);
                rest = m.group(2);
            } else if ((m = TRAILING_POSTCODE.matcher(line)).matches()) {
                postCode = m.group(2);
                rest = m.group(1);
            }
            if (postCode != null) {
                address.setPostCode(set(role, "post code", address.getPostCode(), postCode));
            }
            if (rest.isEmpty()) continue;
            Matcher street = STREET.matcher(rest);
            if (street.matches()) {
                address.setBuildingNumber(set(role, "building number", address.getBuildingNumber(), street.group(1)));
                address.setStreetName(set(role, "street", address.getStreetName(), street.group(2)));
                String tail = street.group(4);
                if (tail != null) {
                    boolean townPosition = street.group(3) != null || postCode != null;
                    if (townPosition && WORDS.matcher(tail).matches()) {
                        address.setTownName(set(role, "town", address.getTownName(), tail));
                    } else {
                        unclassified.add(tail);
                    }
                }
            } else if (postCode != null && WORDS.matcher(rest).matches()) {
                address.setTownName(set(role, "town", address.getTownName(), rest));
            } else if (WORDS.matcher(rest).matches() && address.getBuildingName() == null
                    && i + 1 < lines.size() && STREET.matcher(lines.get(i + 1)).matches()) {
                address.setBuildingName(rest);
            } else {
                unclassified.add(rest);
            }
        }
        address.setCountry(country(role, party, statedCountry));
        if (address.getTownName() == null) {
            throw new AddressIncompleteException(role, "no town in address lines " + lines);
        }
        if (unclassified.size() > MAX_ADDRESS_LINES) {
            throw new AddressIncompleteException(role, "more than " + MAX_ADDRESS_LINES + " unstructured lines " + unclassified);
        }
        address.setAddressLines(unclassified);
        return address;
    }

    private String country(String role, PaymentInstruction.Party party, String stated) {
        String ibanCountry = party.getIban().substring(0, 2);
        String bicCountry = party.getBic().substring(4, 6);
        if (stated != null) {
            return stated;
        }
        if (!ibanCountry.equals(bicCountry) || !COUNTRIES.contains(ibanCountry)) {
            throw new AddressIncompleteException(role, "no country in address and IBAN/BIC countries disagree");
        }
        return ibanCountry;
    }

    private String set(String role, String field, String current, String value) {
        if (current != null) {
            throw new AddressIncompleteException(role, "two values for " + field + " (" + current + ", " + value + ")");
        }
        return value;
    }
}
