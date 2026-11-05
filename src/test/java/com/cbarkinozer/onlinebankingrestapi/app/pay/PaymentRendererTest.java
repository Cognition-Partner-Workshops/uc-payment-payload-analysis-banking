package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import javax.validation.ConstraintViolationException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import static org.junit.jupiter.api.Assertions.*;

class PaymentRendererTest {
    private final PaymentRenderer renderer = new PaymentRenderer();

    @Test
    void writesStructuredAddressesAndEscapesXml() throws Exception {
        PaymentInstruction p = PaymentCorpus.instruction(1, new Random(7));
        p.getDebtor().setName("Aster & Sons");
        p.getDebtor().setAddressLines(List.of("Juniper House", "12 Willow Lane", "Merewick 10001 GB"));
        PaymentPayloads output = renderer.render(p);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var xml = factory.newDocumentBuilder().parse(new InputSource(new StringReader(output.getPacs008())));
        assertEquals("Aster & Sons", xml.getElementsByTagNameNS("*", "Nm").item(0).getTextContent());
        assertEquals("Merewick", xml.getElementsByTagNameNS("*", "TwnNm").item(0).getTextContent());
        assertEquals("GB", xml.getElementsByTagNameNS("*", "Ctry").item(0).getTextContent());
        assertTrue(output.getPacs008().contains("<PstlAdr><StrtNm>Willow Lane</StrtNm><BldgNb>12</BldgNb>"
                + "<BldgNm>Juniper House</BldgNm><PstCd>10001</PstCd><TwnNm>Merewick</TwnNm><Ctry>GB</Ctry></PstlAdr>"));
        String mt103 = output.getMt103();
        assertEquals(":50F:/" + p.getDebtor().getIban() + "\n1/Aster & Sons\n2/Juniper House, 12 Willow Lane,\n2/10001\n3/GB/Merewick\n",
                mt103.substring(mt103.indexOf(":50F:"), mt103.indexOf(":52A:")));
        assertTrue(output.getMt103().contains(":59F:/"));
        var event = PaymentCorpus.MAPPER.readTree(output.getEvent()).get("payment");
        assertEquals("Merewick", event.get("debtor").get("postalAddress").get("townName").asText());
        assertFalse(event.get("debtor").has("addressLines"));
        assertEquals(1, PaymentCorpus.MAPPER.readTree(output.getEvent()).get("schemaVersion").asInt());
    }

    @Test
    void refusesAddressWithoutTownOrTooLongForOptionF() {
        PaymentInstruction p = PaymentCorpus.instruction(1, new Random(7));
        p.getCreditor().setAddressLines(List.of("Warehouse Seven", "P.O. Box 301", "FR"));
        var e = assertThrows(AddressIncompleteException.class, () -> renderer.render(p));
        assertEquals("creditor", e.getParty());
        p.getCreditor().setAddressLines(List.of("Unit Twelve Northern Logistics Park", "Gate Four Loading Bay Seven Annex",
                "1 Willow Lane, Rietdam"));
        assertThrows(AddressIncompleteException.class, () -> renderer.render(p));
    }

    @Test
    void rejectsEmptyAndNullInstructions() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(null));
        assertThrows(ConstraintViolationException.class, () -> renderer.render(new PaymentInstruction()));
    }

    @Test
    void rejectsEmptyAddressesAndInvalidAmountsAndFieldInjection() {
        PaymentInstruction p = PaymentCorpus.instruction(1, new Random(7));
        p.getDebtor().setAddressLines(List.of());
        assertThrows(ConstraintViolationException.class, () -> renderer.render(p));
        p.getDebtor().setAddressLines(List.of("1 Willow Lane"));
        p.setAmount(new BigDecimal("-1.00"));
        assertThrows(ConstraintViolationException.class, () -> renderer.render(p));
        p.setAmount(new BigDecimal("1.001"));
        assertThrows(ConstraintViolationException.class, () -> renderer.render(p));
        p.setAmount(new BigDecimal("1.00"));
        p.setRemittanceInfo("Invoice\n:71A:OUR");
        assertThrows(ConstraintViolationException.class, () -> renderer.render(p));
    }
}
