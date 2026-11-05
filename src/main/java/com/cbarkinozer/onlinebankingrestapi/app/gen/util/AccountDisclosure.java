package com.cbarkinozer.onlinebankingrestapi.app.gen.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AccountDisclosure {
    private static final Pattern IBAN = Pattern.compile(
            "(?<![A-Za-z0-9])(?:[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}"
            + "|[A-Z]{2}[0-9]{2}(?:[ -][A-Z0-9]{4}){2,7}(?:[ -][A-Z0-9]{1,4})?)(?![A-Za-z0-9])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LABELLED_ACCOUNT = Pattern.compile(
            "(\\b(?:account|acct|iban|pan|card)(?:\\s*+(?:number|no\\.?))?\\s*+[:=#]?\\s*+)([0-9](?:[ -]?[0-9]){4,})(?![A-Za-z0-9]|[.,][0-9]|[ -][0-9])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile(
            "(?<![A-Za-z0-9])(?<![0-9][.,])[0-9]{13,}(?![A-Za-z0-9]|[.,][0-9])");
    private static final Pattern GROUPED_NUMBER = Pattern.compile(
            "(?<![A-Za-z0-9*])(?<![0-9][.,])[0-9]+(?:[ -][0-9]+)+(?:[.,][0-9]+)?(?![A-Za-z0-9]|[.,][0-9]|[ -][0-9])");
    private static final Pattern AMOUNT_PREFIX = Pattern.compile(
            "(?:\\b(?:amount|balance|price|total)\\s*[:=]?|\\p{Sc})\\s*$", Pattern.CASE_INSENSITIVE);

    private AccountDisclosure() {}

    public static String mask(Object value) {
        if (value == null) return null;
        String number = value.toString().replaceAll("[\\s-]+", "");
        if (number.isEmpty()) return number;
        return "****" + number.substring(Math.max(0, number.length() - 4));
    }

    public static String redact(String text) {
        if (text == null) return null;
        text = LABELLED_ACCOUNT.matcher(text).replaceAll(match ->
                Matcher.quoteReplacement(match.group(1) + mask(match.group(2))));
        text = replace(IBAN, text);
        String source = text;
        text = GROUPED_NUMBER.matcher(text).replaceAll(match -> {
            String number = match.group();
            String prefix = source.substring(Math.max(0, match.start() - 32), match.start());
            boolean amount = number.contains(".") || number.contains(",") || AMOUNT_PREFIX.matcher(prefix).find();
            return Matcher.quoteReplacement(amount || number.replaceAll("[ -]", "").length() < 13 ? number : mask(number));
        });
        return replace(ACCOUNT_NUMBER, text);
    }

    private static String replace(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(mask(matcher.group())));
        matcher.appendTail(result);
        return result.toString();
    }

    public static class NumberSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator output, SerializerProvider provider) throws IOException {
            output.writeString(mask(value));
        }
    }

    public static class TextSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator output, SerializerProvider provider) throws IOException {
            output.writeString(redact(value));
        }
    }
}
