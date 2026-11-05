package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import org.springframework.stereotype.Service;

@Service
public class PaymentRenderer {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final PostalAddressStructurer STRUCTURER = new PostalAddressStructurer();
    private static final int MT_LINE = 35;
    private static final int MT_PARTY_LINES = 4;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

    public PaymentPayloads render(PaymentInstruction p) {
        if (p == null) {
            throw new IllegalArgumentException("Payment is required");
        }
        var violations = VALIDATOR.validate(p);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        PostalAddress debtor = STRUCTURER.structure("debtor", p.getDebtor());
        PostalAddress creditor = STRUCTURER.structure("creditor", p.getCreditor());
        return new PaymentPayloads(mt103(p, debtor, creditor), pacs008(p, debtor, creditor), event(p, debtor, creditor));
    }

    private String mt103(PaymentInstruction p, PostalAddress debtor, PostalAddress creditor) {
        return "{4:\n:20:" + p.getEndToEndId() + "\n:23B:CRED\n:32A:"
                + p.getValueDate().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + p.getCurrency() + p.getAmount().setScale(2).toPlainString().replace('.', ',')
                + "\n:50F:/" + p.getDebtor().getIban() + "\n" + partyText("debtor", p.getDebtor(), debtor)
                + "\n:52A:" + p.getDebtor().getBic()
                + "\n:57A:" + p.getCreditor().getBic()
                + "\n:59F:/" + p.getCreditor().getIban() + "\n" + partyText("creditor", p.getCreditor(), creditor)
                + "\n:70:" + wrap(p.getRemittanceInfo(), 35)
                + "\n:71A:" + p.getChargeBearer() + "\n-}\n";
    }

    private String partyText(String role, PaymentInstruction.Party p, PostalAddress a) {
        List<String> details = new ArrayList<>();
        if (a.getBuildingName() != null) details.add(a.getBuildingName());
        if (a.getStreetName() != null) details.add(a.getBuildingNumber() + " " + a.getStreetName());
        if (a.getPostBox() != null) details.add(a.getPostBox());
        if (a.getPostCode() != null) details.add(a.getPostCode());
        details.addAll(a.getAddressLines());
        String town = "3/" + a.getCountry() + "/" + a.getTownName();
        if (town.length() > MT_LINE) {
            throw new AddressIncompleteException(role, "town too long for MT103 option F");
        }
        List<String> lines = new ArrayList<>();
        numbered("1/", List.of(p.getName()), lines);
        numbered("2/", details, lines);
        lines.add(town);
        if (lines.size() > MT_PARTY_LINES) {
            throw new AddressIncompleteException(role, "address too long for MT103 option F");
        }
        return String.join("\n", lines);
    }

    private void numbered(String prefix, List<String> segments, List<String> lines) {
        int width = MT_LINE - prefix.length();
        StringBuilder line = new StringBuilder();
        for (String segment : segments) {
            for (String word : (segment + ",").split(" ")) {
                if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                    lines.add(prefix + line);
                    line.setLength(0);
                }
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0) {
            line.setLength(line.length() - 1);
            lines.add(prefix + line);
        }
    }

    private String wrap(String value, int width) {
        StringBuilder result = new StringBuilder();
        for (int start = 0; start < value.length(); start += width) {
            if (start > 0) result.append('\n');
            result.append(value, start, Math.min(start + width, value.length()));
        }
        return result.toString();
    }

    private String pacs008(PaymentInstruction p, PostalAddress debtor, PostalAddress creditor) {
        String created = p.getValueDate() + "T00:00:00Z";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">"
                + "<FIToFICstmrCdtTrf><GrpHdr>" + tag("MsgId", p.getEndToEndId())
                + tag("CreDtTm", created) + tag("NbOfTxs", "1")
                + "<SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf></GrpHdr>"
                + "<CdtTrfTxInf><PmtId>" + tag("InstrId", p.getEndToEndId())
                + tag("EndToEndId", p.getEndToEndId()) + "</PmtId>"
                + "<IntrBkSttlmAmt Ccy=\"" + p.getCurrency() + "\">"
                + p.getAmount().setScale(2).toPlainString() + "</IntrBkSttlmAmt>"
                + tag("IntrBkSttlmDt", p.getValueDate().toString())
                + tag("ChrgBr", Map.of("SHA", "SHAR", "OUR", "DEBT", "BEN", "CRED").get(p.getChargeBearer()))
                + xmlParty("Dbtr", p.getDebtor(), debtor) + account("DbtrAcct", p.getDebtor())
                + agent("DbtrAgt", p.getDebtor()) + agent("CdtrAgt", p.getCreditor())
                + xmlParty("Cdtr", p.getCreditor(), creditor) + account("CdtrAcct", p.getCreditor())
                + "<RmtInf>" + tag("Ustrd", p.getRemittanceInfo()) + "</RmtInf>"
                + "</CdtTrfTxInf></FIToFICstmrCdtTrf></Document>\n";
    }

    private String xmlParty(String name, PaymentInstruction.Party p, PostalAddress a) {
        StringBuilder address = new StringBuilder();
        optional(address, "StrtNm", a.getStreetName());
        optional(address, "BldgNb", a.getBuildingNumber());
        optional(address, "BldgNm", a.getBuildingName());
        optional(address, "PstBx", a.getPostBox());
        optional(address, "PstCd", a.getPostCode());
        optional(address, "TwnNm", a.getTownName());
        optional(address, "Ctry", a.getCountry());
        a.getAddressLines().forEach(line -> address.append(tag("AdrLine", line)));
        return "<" + name + ">" + tag("Nm", p.getName()) + "<PstlAdr>" + address
                + "</PstlAdr></" + name + ">";
    }

    private void optional(StringBuilder xml, String name, String value) {
        if (value != null) xml.append(tag(name, value));
    }

    private String account(String name, PaymentInstruction.Party p) {
        return "<" + name + "><Id>" + tag("IBAN", p.getIban()) + "</Id></" + name + ">";
    }

    private String agent(String name, PaymentInstruction.Party p) {
        return "<" + name + "><FinInstnId>" + tag("BICFI", p.getBic()) + "</FinInstnId></" + name + ">";
    }

    private String tag(String name, String value) {
        return "<" + name + ">" + value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;") + "</" + name + ">";
    }

    private String event(PaymentInstruction p, PostalAddress debtor, PostalAddress creditor) {
        ObjectNode payment = mapper.valueToTree(p);
        payment.set("debtor", withPostalAddress((ObjectNode) payment.get("debtor"), debtor));
        payment.set("creditor", withPostalAddress((ObjectNode) payment.get("creditor"), creditor));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "OutboundPaymentCreated");
        event.put("schemaVersion", 1);
        event.put("payment", payment);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize payment event", e);
        }
    }

    private ObjectNode withPostalAddress(ObjectNode party, PostalAddress address) {
        ObjectNode result = mapper.createObjectNode();
        party.fields().forEachRemaining(field -> {
            if (field.getKey().equals("addressLines")) {
                result.set("postalAddress", mapper.valueToTree(address));
            } else {
                result.set(field.getKey(), field.getValue());
            }
        });
        return result;
    }
}
