package com.cbarkinozer.onlinebankingrestapi.app.gen.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public final class AccountDisclosure {
    private static final Set<String> ACCOUNT_LABELS = Set.of("account", "acct", "iban", "pan", "card");
    private static final Set<String> AMOUNT_LABELS = Set.of("amount", "balance", "price", "total");

    private AccountDisclosure() {}

    public static String mask(Object value) {
        if (value == null) return null;
        String number = value.toString().replaceAll("[\\s-]+", "");
        if (number.isEmpty()) return number;
        return "****" + number.substring(Math.max(0, number.length() - 4));
    }

    public static String redact(String text) {
        if (text == null) return null;
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            if (tokenStart(text, index)) {
                int ibanEnd = ibanEnd(text, index);
                if (ibanEnd > index) {
                    result.append(mask(text.substring(index, ibanEnd)));
                    index = ibanEnd;
                    continue;
                }
                if (digit(text.charAt(index))) {
                    NumberToken token = numberToken(text, index);
                    boolean labelled = accountLabel(text, index);
                    boolean account = token.digits >= 13 || (token.digits >= 5
                            && (labelled || commaNeighbour(text, index, token.end)));
                    boolean boundary = token.end == text.length() || !Character.isLetterOrDigit(text.charAt(token.end));
                    String value = text.substring(index, token.end);
                    result.append(account && boundary && !token.amount && !amountLabel(text, index) ? mask(value) : value);
                    index = token.end;
                    continue;
                }
            }
            result.append(text.charAt(index++));
        }
        return result.toString();
    }

    private static boolean tokenStart(String text, int index) {
        if (index == 0) return true;
        char previous = text.charAt(index - 1);
        return previous != '*' && !Character.isLetterOrDigit(previous)
                && !(previous == '.' && index > 1 && digit(text.charAt(index - 2)));
    }

    private static boolean digit(char value) {
        return value >= '0' && value <= '9';
    }

    private static int digitsEnd(String text, int start) {
        int end = start;
        while (end < text.length() && digit(text.charAt(end))) end++;
        return end;
    }

    private static int ibanEnd(String text, int start) {
        if (start + 4 > text.length() || !Character.isLetter(text.charAt(start))
                || !Character.isLetter(text.charAt(start + 1))
                || !digit(text.charAt(start + 2)) || !digit(text.charAt(start + 3))) return start;
        int end = start;
        while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) end++;
        int length = end - start;
        if (length == 4) {
            while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '-')) {
                int next = end + 1;
                while (next < text.length() && Character.isLetterOrDigit(text.charAt(next))) next++;
                int group = next - end - 1;
                if (group == 0 || group > 4 || length + group > 34) break;
                length += group;
                end = next;
                if (group < 4) break;
            }
        }
        return length >= 15 && length <= 34 ? end : start;
    }

    private static NumberToken numberToken(String text, int start) {
        int end = digitsEnd(text, start);
        NumberToken token = new NumberToken(end, end - start);
        boolean thousands = token.digits <= 3;
        while (token.end < text.length()) {
            char separator = text.charAt(token.end);
            int next = token.end + 1;
            if (Character.isWhitespace(separator)) {
                while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
            }
            int groupEnd = digitsEnd(text, next);
            int group = groupEnd - next;
            if (group == 0) break;
            if (separator == '.' || (separator == ',' && (group <= 2 || (thousands && group == 3)))) {
                token.amount = true;
                token.end = groupEnd;
                token.digits += group;
                if (separator == '.' || group <= 2) break;
            } else if (separator == '-' || Character.isWhitespace(separator)) {
                token.end = groupEnd;
                token.digits += group;
            } else {
                break;
            }
        }
        return token;
    }

    private static boolean commaNeighbour(String text, int start, int end) {
        if (end < text.length() && text.charAt(end) == ',') {
            int next = end + 1;
            while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
            int digits = digitsEnd(text, next);
            if (digits - next >= 5 && !(digits < text.length() && text.charAt(digits) == '.')) return true;
            if (next < text.length() && ibanEnd(text, next) > next) return true;
        }
        int previous = start - 1;
        while (previous >= 0 && Character.isWhitespace(text.charAt(previous))) previous--;
        if (previous < 0 || text.charAt(previous) != ',') return false;
        int digits = previous;
        while (digits > 0 && digit(text.charAt(digits - 1))) digits--;
        return previous - digits >= 5;
    }

    private static int prefixEnd(String text, int start) {
        while (start > 0 && (Character.isWhitespace(text.charAt(start - 1))
                || ":=#.\'\"".indexOf(text.charAt(start - 1)) >= 0)) start--;
        return start;
    }

    private static int wordStart(String text, int end) {
        while (end > 0 && Character.isLetter(text.charAt(end - 1))) end--;
        return end;
    }

    private static boolean accountLabel(String text, int start) {
        int end = prefixEnd(text, start);
        int word = wordStart(text, end);
        String label = text.substring(word, end).toLowerCase(Locale.ROOT);
        if (label.equals("number") || label.equals("no")) {
            end = prefixEnd(text, word);
            word = wordStart(text, end);
            label = text.substring(word, end).toLowerCase(Locale.ROOT);
        }
        return ACCOUNT_LABELS.contains(label);
    }

    private static boolean amountLabel(String text, int start) {
        int end = prefixEnd(text, start);
        if (end > 0 && Character.getType(text.charAt(end - 1)) == Character.CURRENCY_SYMBOL) return true;
        return AMOUNT_LABELS.contains(text.substring(wordStart(text, end), end).toLowerCase(Locale.ROOT));
    }

    private static final class NumberToken {
        private int end;
        private int digits;
        private boolean amount;

        private NumberToken(int end, int digits) {
            this.end = end;
            this.digits = digits;
        }
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
