package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

@Service
public class PaymentRenderer {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public PaymentPayloads render(PaymentInstruction p) {
        if (p == null) {
            throw new IllegalArgumentException("Payment is required");
        }
        var violations = VALIDATOR.validate(p);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return new PaymentPayloads(mt103(p), pacs008(p), event(p));
    }

    private String mt103(PaymentInstruction p) {
        return "{4:\n:20:" + p.getEndToEndId() + "\n:23B:CRED\n:32A:"
                + p.getValueDate().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + p.getCurrency() + p.getAmount().setScale(2).toPlainString().replace('.', ',')
                + "\n:50K:/" + p.getDebtor().getIban() + "\n" + partyText(p.getDebtor())
                + "\n:52A:" + p.getDebtor().getBic()
                + "\n:57A:" + p.getCreditor().getBic()
                + "\n:59:/" + p.getCreditor().getIban() + "\n" + partyText(p.getCreditor())
                + "\n:70:" + wrap(p.getRemittanceInfo(), 35)
                + "\n:71A:" + p.getChargeBearer() + "\n-}\n";
    }

    private String partyText(PaymentInstruction.Party p) {
        return p.getName() + "\n" + String.join("\n", p.getAddressLines());
    }

    private String wrap(String value, int width) {
        StringBuilder result = new StringBuilder();
        for (int start = 0; start < value.length(); start += width) {
            if (start > 0) result.append('\n');
            result.append(value, start, Math.min(start + width, value.length()));
        }
        return result.toString();
    }

    private String pacs008(PaymentInstruction p) {
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
                + xmlParty("Dbtr", p.getDebtor()) + account("DbtrAcct", p.getDebtor())
                + agent("DbtrAgt", p.getDebtor()) + agent("CdtrAgt", p.getCreditor())
                + xmlParty("Cdtr", p.getCreditor()) + account("CdtrAcct", p.getCreditor())
                + "<RmtInf>" + tag("Ustrd", p.getRemittanceInfo()) + "</RmtInf>"
                + "</CdtTrfTxInf></FIToFICstmrCdtTrf></Document>\n";
    }

    private String xmlParty(String name, PaymentInstruction.Party p) {
        StringBuilder address = new StringBuilder();
        p.getAddressLines().forEach(line -> address.append(tag("AdrLine", line)));
        return "<" + name + ">" + tag("Nm", p.getName()) + "<PstlAdr>" + address
                + "</PstlAdr></" + name + ">";
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

    private String event(PaymentInstruction p) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "OutboundPaymentCreated");
        event.put("schemaVersion", 1);
        event.put("payment", p);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize payment event", e);
        }
    }
}
