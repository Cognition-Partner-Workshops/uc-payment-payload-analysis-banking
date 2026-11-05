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
    void preservesUnstructuredAddressesAndEscapesXml() throws Exception {
        PaymentInstruction p = PaymentCorpus.instruction(0, new Random(7));
        p.getDebtor().setName("Aster & Sons");
        PaymentPayloads output = renderer.render(p);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var xml = factory.newDocumentBuilder().parse(new InputSource(new StringReader(output.getPacs008())));
        assertEquals("Aster & Sons", xml.getElementsByTagNameNS("*", "Nm").item(0).getTextContent());
        assertEquals(0, xml.getElementsByTagNameNS("*", "TwnNm").getLength());
        assertEquals(0, xml.getElementsByTagNameNS("*", "Ctry").getLength());
        assertTrue(output.getMt103().contains(":50K:/"));
        assertTrue(output.getMt103().contains(":59:/"));
        assertEquals(PaymentCorpus.MAPPER.readTree(PaymentCorpus.MAPPER.writeValueAsString(p)),
                PaymentCorpus.MAPPER.readTree(output.getEvent()).get("payment"));
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
