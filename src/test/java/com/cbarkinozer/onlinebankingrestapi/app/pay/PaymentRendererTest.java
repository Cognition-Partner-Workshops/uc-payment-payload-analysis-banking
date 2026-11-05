package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.databind.JsonNode;
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
        p.getDebtor().setIban("GB" + p.getDebtor().getIban().substring(2));
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
    void rendersUkPostcodeAsStructuredAddress() throws Exception {
        PaymentInstruction p = PaymentCorpus.instruction(3, new Random(7));
        p.getDebtor().setAddressLines(List.of("10 Downing Street, London SW1A 2AA", "GB"));
        p.getDebtor().setIban("GB" + p.getDebtor().getIban().substring(2));
        PaymentPayloads output = renderer.render(p);
        assertTrue(output.getMt103().contains("3/GB/London"));
        assertTrue(output.getPacs008().contains("<PstCd>SW1A 2AA</PstCd><TwnNm>London</TwnNm><Ctry>GB</Ctry>"));
        JsonNode event = PaymentCorpus.MAPPER.readTree(output.getEvent());
        assertEquals(2, event.get("schemaVersion").asInt());
        JsonNode address = event.get("payment").get("debtor").get("postalAddress");
        assertEquals("SW1A 2AA", address.get("postCode").asText());
        assertEquals("London", address.get("townName").asText());
        assertEquals("GB", address.get("country").asText());
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
    void wrapsTownWithCountryOnlyOnFirstLine() {
        PaymentInstruction p = PaymentCorpus.instruction(3, new Random(7));
        PostalAddress address = new PostalAddress();
        address.setTownName("Stratford Saint Andrew Parish Town"); address.setCountry("GB");
        p.getDebtor().setAddressLines(null); p.getDebtor().setPostalAddress(address);
        p.getDebtor().setIban("GB" + p.getDebtor().getIban().substring(2));
        String block = renderer.render(p).getMt103().split(":50F:/[^\\n]+\\n")[1].split(":52A:")[0];
        List<String> town = block.lines().filter(line -> line.startsWith("3/")).toList();
        assertEquals(2, town.size());
        assertTrue(town.get(0).startsWith("3/GB/"));
        assertEquals(address.getTownName(), town.get(0).substring(5) + " " + town.get(1).substring(2));
        assertTrue(block.lines().allMatch(line -> line.length() <= 35));
    }

    @Test
    void packsDetailsToFitWithLongNameWithoutLosingContent() {
        PaymentInstruction p = PaymentCorpus.instruction(3, new Random(7));
        p.getDebtor().setName("International Payment Trading Group");
        PostalAddress address = new PostalAddress();
        address.setBuildingNumber("1"); address.setStreetName("Extraordinary Willow Lane");
        address.setPostCode("12345"); address.setTownName("London"); address.setCountry("GB");
        p.getDebtor().setAddressLines(null); p.getDebtor().setPostalAddress(address);
        p.getDebtor().setIban("GB" + p.getDebtor().getIban().substring(2));
        String block = renderer.render(p).getMt103().split(":50F:/[^\\n]+\\n")[1].split(":52A:")[0];
        assertEquals(4, block.lines().count());
        assertTrue(block.contains("2/1 Extraordinary Willow Lane 12345\n"));
        assertEquals(p.getDebtor().getName(), String.join(" ", block.lines().filter(line -> line.startsWith("1/")).map(line -> line.substring(2)).toList()));
        assertTrue(block.lines().allMatch(line -> line.length() <= 35));
    }

    @Test
    void rejectsMt103AddressesOverFourNumberedLines() {
        PaymentInstruction p = PaymentCorpus.instruction(2, new Random(7));
        PostalAddress address = new PostalAddress();
        address.setBuildingName("A Building Name That Uses Space");
        address.setStreetName("A Very Long Street Name That Uses More Space"); address.setBuildingNumber("1234567890123456");
        address.setPostBox("P.O. Box 123456"); address.setPostCode("12345"); address.setTownName("Merewick"); address.setCountry("GB");
        p.getDebtor().setAddressLines(null); p.getDebtor().setPostalAddress(address);
        p.getDebtor().setIban("GB" + p.getDebtor().getIban().substring(2));
        AddressIncompleteException failure = assertThrows(AddressIncompleteException.class, () -> renderer.render(p));
        assertEquals("address too long for MT103 option F", failure.getReason());
    }
}
