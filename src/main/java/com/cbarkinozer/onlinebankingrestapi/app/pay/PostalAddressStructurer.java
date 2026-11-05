package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.Validation;
import javax.validation.Validator;

public final class PostalAddressStructurer {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Set<String> COUNTRIES = new HashSet<>(Arrays.asList(Locale.getISOCountries()));
    private static final Pattern BOX = Pattern.compile("(?i)^P\\.O\\. Box \\d+$");
    private static final Pattern STREET = Pattern.compile("^([0-9]+[A-Za-z]?)\\s+(.+?\\b(?:Lane|Street|Road|Avenue|Way|Drive|Close|Court|Place|Square|Boulevard|Terrace|Park|Road|Gardens|Crescent))(?:(?:,\\s*|\\s+)(.*))?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern POSTCODE = Pattern.compile("\\d{4,5}");
    private static final Pattern UK_POSTCODE = Pattern.compile("(?i)(?<![A-Za-z0-9])(?:GIR\\s+0AA|(?:[A-PR-UWYZ][0-9][0-9]?|[A-PR-UWYZ][A-HK-Y][0-9][0-9]?|[A-PR-UWYZ][0-9][A-HJKPSTUW]|[A-PR-UWYZ][A-HK-Y][0-9][ABEHMNPRVWXY])\\s+[0-9][ABD-HJLNP-UW-Z]{2})(?![A-Za-z0-9])");

    private PostalAddressStructurer() { }

    public static PostalAddress structure(PaymentInstruction.Party party, String role) {
        if (party.getPostalAddress() != null) {
            validate(party.getPostalAddress(), role);
            checkCountry(party.getPostalAddress().getCountry(), party.getIban().substring(0, 2), role);
            return party.getPostalAddress();
        }
        List<String> remaining = new ArrayList<>(party.getAddressLines());
        PostalAddress address = new PostalAddress();
        String country = null;
        for (int i = remaining.size() - 1; i >= 0; i--) {
            String line = remaining.get(i).trim();
            int lastSpace = line.lastIndexOf(' ');
            String token = line.substring(lastSpace + 1).toUpperCase(Locale.ROOT);
            if (COUNTRIES.contains(token)) {
                checkCountry(token, party.getIban().substring(0, 2), role);
                country = token;
                if (lastSpace < 0) remaining.remove(i);
                else remaining.set(i, line.substring(0, lastSpace));
            }
        }
        String town = null;
        int streetIndex = -1;
        for (int i = 0; i < remaining.size(); i++) {
            if (STREET.matcher(remaining.get(i)).matches()) { streetIndex = i; break; }
        }
        if (streetIndex >= 0) {
            for (int i = 0; i < streetIndex; i++) {
                if (remaining.get(i).matches("[A-Za-z]+(?: [A-Za-z]+)*")) {
                    address.setBuildingName(remaining.remove(i));
                    streetIndex--;
                    break;
                }
            }
        }
        for (int i = 0; i < remaining.size(); i++) {
            Matcher street = STREET.matcher(remaining.get(i));
            if (street.matches()) {
                address.setBuildingNumber(street.group(1));
                address.setStreetName(street.group(2));
                if (street.group(3) != null && !street.group(3).trim().isEmpty())
                    town = parseLocality(street.group(3).replaceFirst("^,\\s*", "").trim(), address, town, party.getIban().substring(0, 2), role);
                remaining.remove(i);
                break;
            }
        }
        for (int i = 0; i < remaining.size(); i++) {
            if (BOX.matcher(remaining.get(i)).matches()) {
                address.setPostBox(remaining.remove(i));
                i--;
            }
        }
        for (int i = 0; i < remaining.size(); i++) {
            if (hasPostcode(remaining.get(i).trim())) {
                String line = remaining.get(i).trim();
                boolean postcodeOnly = isPostcodeOnly(line);
                town = parseLocality(line, address, town, party.getIban().substring(0, 2), role);
                if (postcodeOnly && i > 0 && isLoneTown(remaining.get(i - 1))) {
                    town = town(town, remaining.remove(i - 1), role);
                    i--;
                } else if (postcodeOnly && i + 1 < remaining.size() && isLoneTown(remaining.get(i + 1))) {
                    town = town(town, remaining.remove(i + 1), role);
                }
                remaining.remove(i);
                i--;
            }
        }
        if (town == null || town.isEmpty()) throw new AddressIncompleteException(role, "town name is missing");
        address.setTownName(town);
        address.setCountry(country != null ? country : (address.getCountry() != null
                ? address.getCountry() : party.getIban().substring(0, 2)));
        checkCountry(address.getCountry(), party.getIban().substring(0, 2), role);
        if (!remaining.isEmpty()) address.setAddressLines(new ArrayList<>(remaining));
        if (remaining.size() > 2) throw new AddressIncompleteException(role, "more than two hybrid address lines remain");
        validate(address, role);
        return address;
    }

    private static String parseLocality(String value, PostalAddress address, String existing, String ibanCountry, String role) {
        Matcher postcode = UK_POSTCODE.matcher(value);
        boolean uk = postcode.find();
        if (!uk) postcode = POSTCODE.matcher(value);
        if (!uk && !postcode.find()) return town(existing, value, role);
        int start = postcode.start();
        String code = postcode.group().replaceAll("\\s+", " ");
        address.setPostCode(uk ? code.toUpperCase(Locale.ROOT) : code);
        List<String> before = words(value.substring(0, start));
        List<String> after = words(value.substring(postcode.end()));
        if (!after.isEmpty() && COUNTRIES.contains(after.get(after.size() - 1).toUpperCase(Locale.ROOT))) {
            checkCountry(after.get(after.size() - 1).toUpperCase(Locale.ROOT), ibanCountry, role);
            address.setCountry(after.remove(after.size() - 1).toUpperCase(Locale.ROOT));
        }
        if (!before.isEmpty() && COUNTRIES.contains(before.get(before.size() - 1).toUpperCase(Locale.ROOT))) {
            checkCountry(before.get(before.size() - 1).toUpperCase(Locale.ROOT), ibanCountry, role);
            address.setCountry(before.remove(before.size() - 1).toUpperCase(Locale.ROOT));
        }
        before.addAll(after);
        return before.isEmpty() ? existing : town(existing, String.join(" ", before), role);
    }

    private static void checkCountry(String stated, String ibanCountry, String role) {
        if (!stated.equals(ibanCountry))
            throw new AddressIncompleteException(role, "stated country " + stated + " conflicts with IBAN country " + ibanCountry);
    }

    private static List<String> words(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(trimmed.split("\\s+")));
    }

    private static boolean hasPostcode(String value) {
        return UK_POSTCODE.matcher(value).find() || POSTCODE.matcher(value).find();
    }

    private static boolean isPostcodeOnly(String value) {
        return UK_POSTCODE.matcher(value).matches() || POSTCODE.matcher(value).matches();
    }

    private static boolean isLoneTown(String value) {
        return value.matches("[A-Za-z]+(?: [A-Za-z]+)*") && !COUNTRIES.contains(value.toUpperCase(Locale.ROOT));
    }

    private static String town(String existing, String candidate, String role) {
        if (existing != null && !existing.equals(candidate)) {
            throw new AddressIncompleteException(role, "two different town names were found");
        }
        return candidate;
    }

    private static void validate(PostalAddress address, String role) {
        if (address.getCountry() != null && !COUNTRIES.contains(address.getCountry()))
            throw new AddressIncompleteException(role, "country is not an ISO 3166 alpha-2 code");
        var violations = VALIDATOR.validate(address);
        if (!violations.isEmpty()) {
            throw new AddressIncompleteException(role, violations.iterator().next().getPropertyPath() + " is invalid");
        }
    }
}
