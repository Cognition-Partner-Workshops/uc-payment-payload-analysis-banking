package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentReplayTest {
    private static final Set<String> COUNTRIES = new HashSet<>(Arrays.asList(Locale.getISOCountries()));
    private static final Pattern CONTENT_TOKEN = Pattern.compile("[A-Za-z]+|\\d+");

    @Test
    void replay() throws Exception {
        Path root = Path.of("payloads/recorded");
        JsonNode manifest = PaymentCorpus.MAPPER.readTree(root.resolve("manifest.json").toFile());
        assertEquals(240, manifest.get("count").asInt());
        List<Row> rows = new ArrayList<>();
        List<String> operations = new ArrayList<>();
        List<String> unexpected = new ArrayList<>();
        Map<String, String> semanticFailures = new LinkedHashMap<>();
        int ibanCountryCount = 0;
        for (JsonNode entry : manifest.get("payments")) {
            String id = entry.get("id").asText();
            Path directory = root.resolve(id);
            PaymentInstruction input = PaymentCorpus.MAPPER.readValue(directory.resolve("input.json").toFile(), PaymentInstruction.class);
            for (Map.Entry<String, PaymentInstruction.Party> party : Map.of("debtor", input.getDebtor(), "creditor", input.getCreditor()).entrySet()) {
                try {
                    PostalAddress address = PostalAddressStructurer.structure(party.getValue(), party.getKey());
                    assertDerived(party.getValue(), address);
                    if (party.getValue().getAddressLines().stream().noneMatch(line -> Arrays.asList(line.split("\\s+")).contains(address.getCountry())))
                        ibanCountryCount++;
                } catch (AddressIncompleteException failure) {
                    operations.add(id + " | " + party.getKey() + " | " + party.getValue().getAddressLines() + " | " + failure.getMessage());
                } catch (AssertionError failure) {
                    semanticFailures.put(id, party.getKey() + ": " + failure.getMessage());
                }
            }
            try {
                PaymentPayloads output = new PaymentRenderer().render(input);
                if (id.equals("PAY000004") || id.equals("PAY000005")) {
                    Path dump = Path.of("target/parity", id); Files.createDirectories(dump);
                    Files.writeString(dump.resolve("mt103.txt"), output.getMt103());
                    Files.writeString(dump.resolve("pacs008.xml"), output.getPacs008());
                    Files.writeString(dump.resolve("event.json"), output.getEvent());
                }
                compare(rows, unexpected, semanticFailures, id, "MT103", Files.readString(directory.resolve("mt103.txt")), output.getMt103(), input);
                compare(rows, unexpected, semanticFailures, id, "pacs.008", Files.readString(directory.resolve("pacs008.xml")), output.getPacs008(), input);
                compare(rows, unexpected, semanticFailures, id, "event", Files.readString(directory.resolve("event.json")), output.getEvent(), input);
            } catch (AddressIncompleteException failure) {
                boolean expected = !entry.get("creditorTownPresent").asBoolean();
                for (String format : List.of("MT103", "pacs.008", "event"))
                    rows.add(new Row(id, format, "mismatch", "held for operations (no town; not guessed): " + failure.getMessage()));
                if (!expected) unexpected.add(id + ": " + failure.getMessage());
            }
        }
        writeReport(rows, operations, ibanCountryCount);
        assertEquals(6, operations.size(), "operations list");
        assertTrue(unexpected.isEmpty(), String.join("\n", unexpected));
    }

    private void compare(List<Row> rows, List<String> unexpected, Map<String, String> semanticFailures, String id, String format,
                         String before, String after, PaymentInstruction input) {
        if (semanticFailures.containsKey(id)) {
            String reason = "address content was dropped: " + semanticFailures.get(id);
            rows.add(new Row(id, format, "mismatch", reason));
            unexpected.add(id + " " + format + ": " + reason);
            return;
        }
        try {
            if (format.equals("MT103")) {
                assertTrue(after.contains(":50F:/" + input.getDebtor().getIban() + "\n1/" + input.getDebtor().getName()));
                assertTrue(after.contains(":59F:/" + input.getCreditor().getIban() + "\n1/" + input.getCreditor().getName()));
                assertTrue(after.matches("(?s).*:50F:.*\\n3/[A-Z]{2}/[^\\n]+.*:59F:.*\\n3/[A-Z]{2}/[^\\n]+.*"));
                assertEquals(maskMt(before), maskMt(after));
            } else if (format.equals("pacs.008")) {
                assertEquals(maskPacs(before), maskPacs(after));
                Matcher addresses = Pattern.compile("<PstlAdr>(.*?)</PstlAdr>").matcher(after);
                int count = 0;
                while (addresses.find()) {
                    String address = addresses.group(1); count++;
                    assertTrue(address.contains("<TwnNm>")); assertTrue(address.matches("(?s).*<Ctry>[A-Z]{2}</Ctry>.*"));
                    assertTrue(occurrences(address, "<AdrLine>") <= 2);
                }
                assertEquals(2, count);
            } else {
                assertEquals(1, PaymentCorpus.MAPPER.readTree(before).get("schemaVersion").asInt());
                assertEquals(2, PaymentCorpus.MAPPER.readTree(after).get("schemaVersion").asInt());
                assertEquals(maskEvent(before), maskEvent(after));
                JsonNode payment = PaymentCorpus.MAPPER.readTree(after).get("payment");
                for (String role : List.of("debtor", "creditor")) {
                    JsonNode address = payment.get(role).get("postalAddress");
                    assertNotNull(address.get("townName")); assertTrue(COUNTRIES.contains(address.get("country").asText()));
                    assertTrue(!address.has("addressLines") || address.get("addressLines").size() <= 2);
                }
            }
            rows.add(new Row(id, format, before.equals(after) ? "identical" : "changed-only-per-rule",
                    format.equals("event") ? "address nodes plus schemaVersion line only" : "address region only"));
        } catch (AssertionError failure) {
            String reason = failure.getMessage() == null ? "parity assertion failed" : failure.getMessage().replace('\n', ' ');
            rows.add(new Row(id, format, "mismatch", reason)); unexpected.add(id + " " + format + ": " + reason);
        } catch (Exception failure) {
            rows.add(new Row(id, format, "mismatch", failure.toString())); unexpected.add(id + " " + format + ": " + failure);
        }
    }

    private void assertDerived(PaymentInstruction.Party party, PostalAddress address) {
        String original = String.join(" | ", party.getAddressLines());
        assertTrue(original.contains(address.getTownName()));
        for (String value : Arrays.asList(address.getBuildingName(), address.getStreetName(), address.getBuildingNumber(),
                address.getPostBox(), address.getPostCode())) if (value != null) assertTrue(original.contains(value), value);
        if (address.getAddressLines() != null) for (String line : address.getAddressLines()) assertTrue(party.getAddressLines().contains(line));
        String structured = Arrays.asList(address.getBuildingName(), address.getStreetName(), address.getBuildingNumber(),
                address.getPostBox(), address.getPostCode(), address.getTownName(), address.getCountry(),
                address.getAddressLines() == null ? "" : String.join(" ", address.getAddressLines()))
                .stream().filter(value -> value != null).collect(Collectors.joining(" "));
        for (String line : party.getAddressLines()) {
            if (COUNTRIES.contains(line.trim().toUpperCase(Locale.ROOT))) {
                assertEquals(address.getCountry(), line.trim().toUpperCase(Locale.ROOT));
                continue;
            }
            Matcher tokens = CONTENT_TOKEN.matcher(line);
            while (tokens.find()) {
                String token = tokens.group();
                assertTrue(Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(token) + "(?![A-Za-z0-9])").matcher(structured).find(),
                        "missing address token: " + token);
            }
        }
        assertTrue(original.matches("(?s).*(^|[ |])" + address.getCountry() + "($|[ |]).*")
                || address.getCountry().equals(party.getIban().substring(0, 2)));
    }

    private String maskMt(String value) {
        return value.replaceAll("(?s):50[FK]:.*?(?=:52A:)", "<DEBTOR-ADDRESS>\\n")
                .replaceAll("(?s):59F?:.*?(?=:70:)", "<CREDITOR-ADDRESS>\\n");
    }

    private String maskPacs(String value) {
        return value.replaceAll("(?s)<PstlAdr>.*?</PstlAdr>", "<PstlAdr>MASKED</PstlAdr>");
    }

    private String maskEvent(String value) {
        return value.replaceAll("(?m)^  \\\"schemaVersion\\\" : [12],$", "  \\\"schemaVersion\\\" : MASKED,")
                .replaceAll("(?m)^      \\\"addressLines\\\" : \\[.*]$", "      \\\"ADDRESS\\\" : MASKED")
                .replaceAll("(?s)(?m)^      \\\"postalAddress\\\" : \\{.*?^      \\}$", "      \\\"ADDRESS\\\" : MASKED");
    }

    private int occurrences(String value, String token) { return (value.length() - value.replace(token, "").length()) / token.length(); }

    private void writeReport(List<Row> rows, List<String> operations, int ibanCountryCount) throws Exception {
        Path target = Path.of("target/parity"); Files.createDirectories(target);
        StringBuilder csv = new StringBuilder("id,format,category,reason\n");
        for (Row row : rows) csv.append(row.id).append(',').append(row.format).append(',').append(row.category).append(',')
                .append('"').append(row.reason.replace("\"", "\"\"")).append("\"\n");
        Files.writeString(target.resolve("parity.csv"), csv);
        StringBuilder report = new StringBuilder("# Payment address parity report\n\n")
                .append("Masked regions: MT103 party address blocks; pacs.008 PstlAdr nodes; event address nodes plus the schemaVersion line (1 to 2). All other bytes must match.\n\n")
                .append("| Format | Payloads compared | Identical | Changed only where rule says | Mismatches |\n")
                .append("|---|---:|---:|---:|---:|\n");
        for (String format : List.of("MT103", "pacs.008", "event", "total")) {
            List<Row> selected = format.equals("total") ? rows : rows.stream().filter(r -> r.format.equals(format)).toList();
            report.append("| ").append(format).append(" | ").append(selected.size()).append(" | ")
                    .append(selected.stream().filter(r -> r.category.equals("identical")).count()).append(" | ")
                    .append(selected.stream().filter(r -> r.category.equals("changed-only-per-rule")).count()).append(" | ")
                    .append(selected.stream().filter(r -> r.category.equals("mismatch")).count()).append(" |\n");
        }
        Map<String, List<Row>> byPayment = new LinkedHashMap<>();
        for (Row row : rows) byPayment.computeIfAbsent(row.id, ignored -> new ArrayList<>()).add(row);
        long identicalPayments = byPayment.values().stream().filter(payment -> payment.stream().allMatch(r -> r.category.equals("identical"))).count();
        long changedPayments = byPayment.values().stream().filter(payment -> payment.stream().allMatch(r -> r.category.equals("changed-only-per-rule"))).count();
        long heldOrMismatchedPayments = byPayment.size() - identicalPayments - changedPayments;
        report.append("\n## Payment-level summary\n\n")
                .append("| Payments compared | Identical | Changed only per rule | Held for operations or mismatched |\n")
                .append("|---:|---:|---:|---:|\n")
                .append("| ").append(byPayment.size()).append(" | ").append(identicalPayments).append(" | ")
                .append(changedPayments).append(" | ").append(heldOrMismatchedPayments).append(" |\n");
        report.append("\n## Mismatches\n\n");
        rows.stream().filter(r -> r.category.equals("mismatch")).forEach(r -> report.append("- ").append(r.id).append(" / ").append(r.format).append(": ").append(r.reason).append('\n'));
        report.append("\n## Operations list\n\n"); operations.forEach(op -> report.append("- ").append(op).append('\n'));
        report.append("\nAddresses whose country came from IBAN: **").append(ibanCountryCount).append("**\n");
        Files.writeString(target.resolve("parity-report.md"), report);
    }

    private static final class Row {
        final String id; final String format; final String category; final String reason;
        Row(String id, String format, String category, String reason) { this.id = id; this.format = format; this.category = category; this.reason = reason; }
    }
}
