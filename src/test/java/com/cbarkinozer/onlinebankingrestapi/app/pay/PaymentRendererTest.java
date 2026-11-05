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
    void rendersStructuredAddressesAndEscapesXml() throws Exception {
        PaymentInstruction p = PaymentCorpus.instruction(3, new Random(7));
        PostalAddress address = new PostalAddress();
        address.setStreetName("Fish & Chip Street"); address.setBuildingNumber("8");
        address.setTownName("A&B Town"); address.setCountry("GB");
        p.getDebtor().setAddressLines(null); p.getDebtor().setPostalAddress(address);
        PaymentPayloads output = renderer.render(p);
        var factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        var xml = factory.newDocumentBuilder().parse(new InputSource(new StringReader(output.getPacs008())));
        assertEquals("Fish & Chip Street", xml.getElementsByTagNameNS("*", "StrtNm").item(0).getTextContent());
        assertEquals("A&B Town", xml.getElementsByTagNameNS("*", "TwnNm").item(0).getTextContent());
        assertTrue(output.getMt103().contains(":50F:/")); assertTrue(output.getMt103().contains(":59F:/"));
        assertTrue(output.getEvent().contains("\"postalAddress\""));
        assertFalse(output.getEvent().contains("\"addressLines\" :"));
    }

    @Test
    void rejectsEmptyNullAndMalformedInstructions() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(null));
        assertThrows(ConstraintViolationException.class, () -> renderer.render(new PaymentInstruction()));
        PaymentInstruction emptyAddress = PaymentCorpus.instruction(1, new Random(7));
        emptyAddress.getDebtor().setAddressLines(List.of());
        assertThrows(ConstraintViolationException.class, () -> renderer.render(emptyAddress));
        PaymentInstruction invalidAmount = PaymentCorpus.instruction(1, new Random(7));
        invalidAmount.setAmount(new BigDecimal("-1.00"));
        assertThrows(ConstraintViolationException.class, () -> renderer.render(invalidAmount));
        PaymentInstruction excessivePrecision = PaymentCorpus.instruction(1, new Random(7));
        excessivePrecision.setAmount(new BigDecimal("1.001"));
        assertThrows(ConstraintViolationException.class, () -> renderer.render(excessivePrecision));
        PaymentInstruction injection = PaymentCorpus.instruction(1, new Random(7));
        injection.setRemittanceInfo("Invoice\n:71A:OUR");
        assertThrows(ConstraintViolationException.class, () -> renderer.render(injection));
    }

    @Test
    void rejectsMt103AddressesOverFourNumberedLines() {
        PaymentInstruction p = PaymentCorpus.instruction(2, new Random(7));
        PostalAddress address = new PostalAddress();
        address.setBuildingName("A Building Name That Uses Space");
        address.setStreetName("A Very Long Street Name That Uses More Space"); address.setBuildingNumber("1234567890123456");
        address.setPostBox("P.O. Box 123456"); address.setPostCode("12345"); address.setTownName("Merewick"); address.setCountry("GB");
        p.getDebtor().setAddressLines(null); p.getDebtor().setPostalAddress(address);
        AddressIncompleteException failure = assertThrows(AddressIncompleteException.class, () -> renderer.render(p));
        assertTrue(failure.getMessage().contains("four numbered lines"));
    }
}
