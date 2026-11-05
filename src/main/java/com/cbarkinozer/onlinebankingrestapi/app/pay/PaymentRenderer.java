package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import org.springframework.stereotype.Service;

@Service
public class PaymentRenderer {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public PaymentPayloads render(PaymentInstruction p) {
        if (p == null) throw new IllegalArgumentException("Payment is required");
        var violations = VALIDATOR.validate(p);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        PostalAddress debtor = PostalAddressStructurer.structure(p.getDebtor(), "debtor");
        PostalAddress creditor = PostalAddressStructurer.structure(p.getCreditor(), "creditor");
        return new PaymentPayloads(mt103(p, debtor, creditor), pacs008(p, debtor, creditor), event(p, debtor, creditor));
    }

    private String mt103(PaymentInstruction p, PostalAddress debtor, PostalAddress creditor) {
        return "{4:\n:20:" + p.getEndToEndId() + "\n:23B:CRED\n:32A:"
                + p.getValueDate().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + p.getCurrency() + p.getAmount().setScale(2).toPlainString().replace('.', ',')
                + "\n:50F:/" + p.getDebtor().getIban() + "\n" + mtParty("debtor", p.getDebtor(), debtor)
                + "\n:52A:" + p.getDebtor().getBic() + "\n:57A:" + p.getCreditor().getBic()
                + "\n:59F:/" + p.getCreditor().getIban() + "\n" + mtParty("creditor", p.getCreditor(), creditor)
                + "\n:70:" + wrap(p.getRemittanceInfo(), 35) + "\n:71A:" + p.getChargeBearer() + "\n-}\n";
    }

    private String mtParty(String role, PaymentInstruction.Party party, PostalAddress address) {
        List<String> lines = new ArrayList<>(numbered("1/", party.getName(), 33));
        List<String> details = new ArrayList<>();
        if (address.getBuildingName() != null) details.add(address.getBuildingName());
        if (address.getBuildingNumber() != null || address.getStreetName() != null)
            details.add(join(address.getBuildingNumber(), address.getStreetName(), " "));
        if (address.getPostBox() != null) details.add(address.getPostBox());
        if (address.getPostCode() != null) details.add(address.getPostCode());
        if (address.getAddressLines() != null) details.addAll(address.getAddressLines());
        if (!details.isEmpty()) lines.addAll(numberedDetails(details));
        lines.add("3/" + address.getCountry() + "/" + address.getTownName());
        if (lines.size() > 4) throw new AddressIncompleteException(role, "MT103 address exceeds four numbered lines");
        return String.join("\n", lines);
    }

    private List<String> numberedDetails(List<String> details) {
        List<String> lines = new ArrayList<>();
        String current = "";
        for (String detail : details) {
            String candidate = current.isEmpty() ? detail : current + ", " + detail;
            if (candidate.length() <= 31) {
                current = candidate;
            } else {
                if (!current.isEmpty()) lines.add("2/" + current);
                if (detail.length() > 31) {
                    List<String> split = numbered("2/", detail, 33);
                    lines.addAll(split.subList(0, split.size() - 1));
                    current = split.get(split.size() - 1).substring(2);
                } else current = detail;
            }
        }
        if (!current.isEmpty()) lines.add("2/" + current);
        return lines;
    }

    private String join(String first, String second, String separator) {
        if (first == null) return second;
        if (second == null) return first;
        return first + separator + second;
    }

    private List<String> numbered(String prefix, String value, int maxLength) {
        List<String> result = new ArrayList<>();
        String remaining = value;
        int width = maxLength - prefix.length();
        while (remaining.length() > width) {
            int cut = remaining.lastIndexOf(' ', width);
            if (cut <= 0) cut = width;
            result.add(prefix + remaining.substring(0, cut));
            remaining = remaining.substring(cut).trim();
        }
        result.add(prefix + remaining);
        return result;
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
                + "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\"><FIToFICstmrCdtTrf><GrpHdr>"
                + tag("MsgId", p.getEndToEndId()) + tag("CreDtTm", created) + tag("NbOfTxs", "1")
                + "<SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf></GrpHdr><CdtTrfTxInf><PmtId>"
                + tag("InstrId", p.getEndToEndId()) + tag("EndToEndId", p.getEndToEndId()) + "</PmtId>"
                + "<IntrBkSttlmAmt Ccy=\"" + p.getCurrency() + "\">" + p.getAmount().setScale(2).toPlainString()
                + "</IntrBkSttlmAmt>" + tag("IntrBkSttlmDt", p.getValueDate().toString())
                + tag("ChrgBr", Map.of("SHA", "SHAR", "OUR", "DEBT", "BEN", "CRED").get(p.getChargeBearer()))
                + xmlParty("Dbtr", p.getDebtor(), debtor) + account("DbtrAcct", p.getDebtor())
                + agent("DbtrAgt", p.getDebtor()) + agent("CdtrAgt", p.getCreditor())
                + xmlParty("Cdtr", p.getCreditor(), creditor) + account("CdtrAcct", p.getCreditor())
                + "<RmtInf>" + tag("Ustrd", p.getRemittanceInfo()) + "</RmtInf></CdtTrfTxInf></FIToFICstmrCdtTrf></Document>\n";
    }

    private String xmlParty(String name, PaymentInstruction.Party party, PostalAddress address) {
        StringBuilder result = new StringBuilder("<").append(name).append(">").append(tag("Nm", party.getName())).append("<PstlAdr>");
        if (address.getStreetName() != null) result.append(tag("StrtNm", address.getStreetName()));
        if (address.getBuildingNumber() != null) result.append(tag("BldgNb", address.getBuildingNumber()));
        if (address.getBuildingName() != null) result.append(tag("BldgNm", address.getBuildingName()));
        if (address.getPostBox() != null) result.append(tag("PstBx", address.getPostBox()));
        if (address.getPostCode() != null) result.append(tag("PstCd", address.getPostCode()));
        result.append(tag("TwnNm", address.getTownName())).append(tag("Ctry", address.getCountry()));
        if (address.getAddressLines() != null) for (String line : address.getAddressLines()) result.append(tag("AdrLine", line));
        return result.append("</PstlAdr></").append(name).append(">").toString();
    }

    private String account(String name, PaymentInstruction.Party p) {
        return "<" + name + "><Id>" + tag("IBAN", p.getIban()) + "</Id></" + name + ">";
    }

    private String agent(String name, PaymentInstruction.Party p) {
        return "<" + name + "><FinInstnId>" + tag("BICFI", p.getBic()) + "</FinInstnId></" + name + ">";
    }

    private String tag(String name, String value) {
        return "<" + name + ">" + value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;") + "</" + name + ">";
    }

    private String event(PaymentInstruction p, PostalAddress debtor, PostalAddress creditor) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "OutboundPaymentCreated"); event.put("schemaVersion", 1);
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("debtor", eventParty(p.getDebtor(), debtor)); payment.put("creditor", eventParty(p.getCreditor(), creditor));
        payment.put("amount", p.getAmount()); payment.put("currency", p.getCurrency()); payment.put("valueDate", p.getValueDate());
        payment.put("remittanceInfo", p.getRemittanceInfo()); payment.put("chargeBearer", p.getChargeBearer()); payment.put("endToEndId", p.getEndToEndId());
        event.put("payment", payment);
        try { return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event) + "\n"; }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize payment event", e); }
    }

    private Map<String, Object> eventParty(PaymentInstruction.Party party, PostalAddress address) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", party.getName()); result.put("iban", party.getIban()); result.put("bic", party.getBic());
        Map<String, Object> postal = new LinkedHashMap<>();
        postal.put("buildingName", address.getBuildingName()); postal.put("streetName", address.getStreetName());
        postal.put("buildingNumber", address.getBuildingNumber()); postal.put("postBox", address.getPostBox());
        postal.put("postCode", address.getPostCode()); postal.put("townName", address.getTownName()); postal.put("country", address.getCountry());
        if (address.getAddressLines() != null && !address.getAddressLines().isEmpty()) postal.put("addressLines", address.getAddressLines());
        postal.values().removeIf(java.util.Objects::isNull); result.put("postalAddress", postal); return result;
    }
}
