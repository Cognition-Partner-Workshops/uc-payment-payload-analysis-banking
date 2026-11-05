package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Replays every recorded payment through the current renderer and compares each payload with
 * its recording. Address regions are masked; everything else must be byte for byte the same.
 * Writes the parity table and the operations list and checks them against the committed copies.
 */
class PaymentReplayTest {
    private static final Path ROOT = Path.of("payloads/recorded");
    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private static final Pattern XML_ADDRESS = Pattern.compile("<PstlAdr>.*?</PstlAdr>");
    private static final Pattern EVENT_ADDRESS = Pattern.compile(
            "\"postalAddress\" : \\{[^{}]*\\}|\"addressLines\" : \\[[^\\]]*\\]");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9]+");
    private static final String[] FORMATS = {"MT103", "pacs.008", "event"};

    @Test
    void replayRecordedPayments() throws Exception {
        JsonNode manifest = PaymentCorpus.MAPPER.readTree(ROOT.resolve("manifest.json").toFile());
        assertEquals(240, manifest.get("count").asInt());
        assertEquals(240, manifest.get("payments").size());
        int[] identical = new int[3];
        int[] addressOnly = new int[3];
        int[] mismatched = new int[3];
        int ibanCountries = 0;
        List<String> mismatches = new ArrayList<>();
        List<String> operations = new ArrayList<>();
        Set<String> held = new TreeSet<>();
        Set<String> recordedNoTown = new TreeSet<>();
        for (JsonNode entry : manifest.get("payments")) {
            String id = entry.get("id").asText();
            if (!entry.get("creditorTownPresent").asBoolean()) recordedNoTown.add(id);
            Path directory = ROOT.resolve(id);
            PaymentInstruction input = PaymentCorpus.MAPPER.readValue(directory.resolve("input.json").toFile(), PaymentInstruction.class);
            String[] before = {read(directory, "mt103.txt"), read(directory, "pacs008.xml"), read(directory, "event.json")};
            PaymentPayloads output;
            try {
                output = new PaymentRenderer().render(input);
            } catch (AddressIncompleteException e) {
                held.add(id);
                PaymentInstruction.Party party = e.getParty().equals("debtor") ? input.getDebtor() : input.getCreditor();
                operations.add("| " + id + " | " + e.getParty() + " | " + party.getName() + " | "
                        + String.join(" / ", party.getAddressLines()) + " | " + e.getReason() + " |");
                for (int f = 0; f < 3; f++) {
                    mismatched[f]++;
                    mismatches.add("| " + id + " | " + FORMATS[f] + " | not rendered, held for operations: " + e.getMessage() + " |");
                }
                continue;
            }
            String[] after = {output.getMt103(), output.getPacs008(), output.getEvent()};
            String[] masked = {null, null, null};
            for (int f = 0; f < 3; f++) {
                if (before[f].equals(after[f])) {
                    identical[f]++;
                    continue;
                }
                String reason = f == 0 ? mt103Difference(before[f], after[f], input)
                        : f == 1 ? pacs008Difference(before[f], after[f])
                        : eventDifference(before[f], after[f]);
                if (reason == null) addressOnly[f]++;
                else {
                    mismatched[f]++;
                    mismatches.add("| " + id + " | " + FORMATS[f] + " | " + reason + " |");
                }
            }
            for (String role : new String[]{"debtor", "creditor"}) {
                PaymentInstruction.Party party = role.equals("debtor") ? input.getDebtor() : input.getCreditor();
                PostalAddress address = new PostalAddressStructurer().structure(role, party);
                List<String> original = tokens(String.join(" ", party.getAddressLines()));
                List<String> structured = tokens(String.join(" ", values(address)));
                if (!original.contains(address.getCountry())) {
                    ibanCountries++;
                    structured.remove(address.getCountry());
                }
                Collections.sort(original);
                Collections.sort(structured);
                if (!original.equals(structured)) {
                    mismatches.add("| " + id + " | all | " + role + " address content changed: " + original + " -> " + structured + " |");
                    mismatched[0]++;
                }
            }
        }
        int compared = 240 * 3;
        StringBuilder report = new StringBuilder("# Recorded payment parity\n\n")
                .append("Generated by `PaymentReplayTest` from `payloads/recorded` (").append(manifest.get("count").asInt())
                .append(" payments, 3 payloads each). Address regions are masked: MT103 fields 50/59 below the account line,")
                .append(" pacs.008 `<PstlAdr>`, event `addressLines`/`postalAddress`. Everything else is compared byte for byte.\n\n")
                .append("| Format | Payloads compared | Identical | Changed only where the rule says | Mismatches |\n")
                .append("|---|---|---|---|---|\n");
        for (int f = 0; f < 3; f++) {
            report.append("| ").append(FORMATS[f]).append(" | 240 | ").append(identical[f]).append(" | ")
                    .append(addressOnly[f]).append(" | ").append(mismatched[f]).append(" |\n");
        }
        report.append("| **Total** | **").append(compared).append("** | **").append(sum(identical)).append("** | **")
                .append(sum(addressOnly)).append("** | **").append(sum(mismatched)).append("** |\n\n")
                .append("Addresses with no stated country that took the country from the IBAN (IBAN and BIC country agree): ")
                .append(ibanCountries).append(" of 480 parties.\n\n## Mismatches\n\n| Payment | Format | Reason |\n|---|---|---|\n");
        mismatches.forEach(m -> report.append(m).append('\n'));
        StringBuilder ops = new StringBuilder("# Payments held for operations: town missing\n\n")
                .append("These recorded payments have an address with no town that can be found without guessing.")
                .append(" The renderer refuses them (HTTP 422, nothing published). Obtain the town from the instructing party")
                .append(" and resubmit.\n\n| Payment | Party | Name | Address lines as received | Reason |\n|---|---|---|---|---|\n");
        operations.forEach(o -> ops.append(o).append('\n'));

        check("parity-report.md", report.toString());
        check("operations-missing-town.md", ops.toString());
        assertEquals(compared, sum(identical) + sum(addressOnly) + sum(mismatched), report.toString());
        assertEquals(recordedNoTown, held, "held payments must be exactly the recordings without a creditor town");
        assertEquals(held.size() * 3, mismatches.size(), "every mismatch must be a payment held for a missing town\n" + report);
        operations.forEach(o -> assertTrue(o.contains("no town"), o));
    }

    private static String mt103Difference(String before, String after, PaymentInstruction input) {
        List<String> oldParties = new ArrayList<>();
        List<String> newParties = new ArrayList<>();
        if (!maskMt(before, oldParties).equals(maskMt(after, newParties))) {
            return "content outside fields 50/59 differs";
        }
        if (!after.contains("\n:50F:/") || !after.contains("\n:59F:/")) return "not option 50F/59F";
        PaymentInstruction.Party[] parties = {input.getDebtor(), input.getCreditor()};
        for (int i = 0; i < 2; i++) {
            String[] lines = newParties.get(i).split("\n");
            if (lines.length > 4) return "more than 4 numbered lines";
            StringBuilder name = new StringBuilder();
            String previous = "1";
            for (String line : lines) {
                if (line.length() > 35) return "line longer than 35: " + line;
                String number = line.substring(0, 1);
                if (!line.startsWith(number + "/") || number.compareTo(previous) < 0) return "bad line numbering: " + line;
                previous = number;
                if (number.equals("1")) name.append(name.length() > 0 ? " " : "").append(line.substring(2));
            }
            String town = lines[lines.length - 1];
            if (!town.matches("3/[A-Z]{2}/.+") || !COUNTRIES.contains(town.substring(2, 4))) return "no 3/CC/Town line";
            if (!name.toString().equals(parties[i].getName())) return "name changed";
        }
        return null;
    }

    private static String maskMt(String text, List<String> parties) {
        StringBuilder masked = new StringBuilder();
        StringBuilder party = null;
        for (String line : text.split("\n", -1)) {
            Matcher m = Pattern.compile(":(50K|50F|59|59F):(/.*)").matcher(line);
            if (m.matches()) {
                masked.append(":5x:").append(m.group(2)).append("\n<address>\n");
                party = new StringBuilder();
                parties.add("");
                continue;
            }
            if (party != null && !line.startsWith(":")) {
                party.append(party.length() > 0 ? "\n" : "").append(line);
                parties.set(parties.size() - 1, party.toString());
                continue;
            }
            party = null;
            masked.append(line).append('\n');
        }
        return masked.toString();
    }

    private static String pacs008Difference(String before, String after) throws Exception {
        if (!XML_ADDRESS.matcher(before).replaceAll("<PstlAdr/>").equals(XML_ADDRESS.matcher(after).replaceAll("<PstlAdr/>"))) {
            return "content outside PstlAdr differs";
        }
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        NodeList addresses = factory.newDocumentBuilder().parse(new InputSource(new StringReader(after)))
                .getElementsByTagNameNS("*", "PstlAdr");
        if (addresses.getLength() != 2) return "expected 2 PstlAdr";
        for (int i = 0; i < 2; i++) {
            Element address = (Element) addresses.item(i);
            if (address.getElementsByTagNameNS("*", "TwnNm").getLength() != 1) return "PstlAdr without TwnNm";
            NodeList country = address.getElementsByTagNameNS("*", "Ctry");
            if (country.getLength() != 1 || !COUNTRIES.contains(country.item(0).getTextContent())) return "PstlAdr without ISO Ctry";
            if (address.getElementsByTagNameNS("*", "AdrLine").getLength() > 2) return "more than 2 AdrLine";
        }
        return null;
    }

    private static String eventDifference(String before, String after) throws Exception {
        if (!EVENT_ADDRESS.matcher(before).replaceAll("<address>").equals(EVENT_ADDRESS.matcher(after).replaceAll("<address>"))) {
            return "content outside the party address differs";
        }
        JsonNode payment = PaymentCorpus.MAPPER.readTree(after).get("payment");
        for (String role : new String[]{"debtor", "creditor"}) {
            JsonNode address = payment.get(role).get("postalAddress");
            if (address == null || payment.get(role).has("addressLines")) return role + " has no postalAddress";
            if (!address.hasNonNull("townName") || !COUNTRIES.contains(address.path("country").asText())) return role + " missing town or country";
            if (address.path("addressLines").size() > 2) return role + " has more than 2 addressLines";
        }
        return null;
    }

    private static List<String> values(PostalAddress a) {
        List<String> values = new ArrayList<>();
        for (String v : new String[]{a.getStreetName(), a.getBuildingNumber(), a.getBuildingName(), a.getPostBox(),
                a.getPostCode(), a.getTownName(), a.getCountry()}) {
            if (v != null) values.add(v);
        }
        values.addAll(a.getAddressLines());
        return values;
    }

    private static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN.matcher(text);
        while (m.find()) tokens.add(m.group());
        return tokens;
    }

    private static void check(String name, String generated) throws Exception {
        Path target = Path.of("target/parity");
        Files.createDirectories(target);
        Files.writeString(target.resolve(name), generated);
        Path committed = Path.of("docs/payments").resolve(name);
        assertTrue(Files.exists(committed), "missing " + committed + "; copy it from " + target.resolve(name));
        assertEquals(Files.readString(committed), generated, committed + " is stale; copy it from " + target.resolve(name));
    }

    private static String read(Path directory, String file) throws Exception {
        return new String(Files.readAllBytes(directory.resolve(file)), StandardCharsets.UTF_8);
    }

    private static int sum(int[] values) {
        return values[0] + values[1] + values[2];
    }
}
