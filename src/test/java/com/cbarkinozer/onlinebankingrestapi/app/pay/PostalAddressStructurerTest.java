package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.util.List;
import javax.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostalAddressStructurerTest {
    @Test
    void structuresEveryCorpusShape() {
        PostalAddress box = structure("NL", "P.O. Box 201", "Rietdam 10001", "NL");
        assertEquals("P.O. Box 201", box.getPostBox()); assertEquals("Rietdam", box.getTownName());
        PostalAddress street = structure("GB", "117 Willow Lane", "10002 Solavilla");
        assertEquals("117", street.getBuildingNumber()); assertEquals("Willow Lane", street.getStreetName());
        PostalAddress building = structure("DE", "Juniper House", "8 Willow Lane", "Lindenfels 10003 DE");
        assertEquals("Juniper House", building.getBuildingName()); assertEquals("DE", building.getCountry());
        PostalAddress inline = structure("FR", "74 Willow Lane Rietdam 10004");
        assertEquals("Rietdam", inline.getTownName()); assertEquals("10004", inline.getPostCode());
        PostalAddress comma = structure("ES", "47 Willow Lane, Solavilla", "10000 ES");
        assertEquals("Solavilla", comma.getTownName()); assertEquals("ES", comma.getCountry());
    }

    @Test
    void structuresUkPostcodesInStreetAndLocalityLines() {
        PostalAddress inline = structure("GB", "10 Downing Street, London SW1A 2AA", "GB");
        assertEquals("10", inline.getBuildingNumber()); assertEquals("Downing Street", inline.getStreetName());
        assertEquals("London", inline.getTownName()); assertEquals("SW1A 2AA", inline.getPostCode());
        assertEquals("GB", inline.getCountry());

        PostalAddress separate = structure("GB", "221B Baker Street", "London", "NW1 6XE");
        assertEquals("221B", separate.getBuildingNumber()); assertEquals("Baker Street", separate.getStreetName());
        assertEquals("London", separate.getTownName()); assertEquals("NW1 6XE", separate.getPostCode());

        PostalAddress townBeforeCode = structure("GB", "221B Baker Street", "London NW1 6XE");
        PostalAddress codeBeforeTown = structure("GB", "221B Baker Street", "NW1 6XE London");
        assertEquals("London", townBeforeCode.getTownName()); assertEquals("London", codeBeforeTown.getTownName());
        assertEquals("NW1 6XE", townBeforeCode.getPostCode()); assertEquals("NW1 6XE", codeBeforeTown.getPostCode());

        assertEquals("M1 1AE", structure("GB", "M1 1AE", "Manchester").getPostCode());
        assertEquals("EC1V 9HX", structure("GB", "EC1V 9HX London").getPostCode());
        assertEquals("B33 8TH", structure("GB", "B33 8TH", "Birmingham").getPostCode());
        assertEquals("M1 1AE", structure("GB", "m1 1ae", "Manchester").getPostCode());
    }

    @Test
    void acceptsUkPostcodeOnlyLinesAdjacentToTheirTown() {
        PostalAddress townBefore = structure("GB", "NW1 6XE", "London");
        PostalAddress townAfter = structure("GB", "London", "NW1 6XE");
        PostalAddress afterStreet = structure("GB", "221B Baker Street", "London", "NW1 6XE");
        assertEquals("London", townBefore.getTownName()); assertEquals("London", townAfter.getTownName());
        assertEquals("London", afterStreet.getTownName()); assertEquals("NW1 6XE", afterStreet.getPostCode());
    }

    @Test
    void rejectsUkPostcodeWithoutAnAcceptedTown() {
        assertThrows(AddressIncompleteException.class, () -> structure("GB", "Unit 4", "SW1A 2AA", "GB"));
        assertThrows(AddressIncompleteException.class, () -> structure("GB", "London", "Unit 4", "GB"));
        AddressIncompleteException guessedTown = assertThrows(AddressIncompleteException.class,
                () -> structure("FR", "8 Willow Lane", "Old Mill", "FR"));
        assertTrue(guessedTown.getReason().contains("town name is missing"));
    }

    @Test
    void rejectsMissingOrAmbiguousTownAndTooManyLeftovers() {
        assertThrows(AddressIncompleteException.class, () -> structure("FR", "Warehouse Seven", "P.O. Box 540", "FR"));
        AddressIncompleteException ambiguous = assertThrows(AddressIncompleteException.class,
                () -> structure("GB", "1 Willow Lane, Merewick", "10000 Otherplace"));
        assertTrue(ambiguous.getReason().contains("two different town"));
        AddressIncompleteException leftovers = assertThrows(AddressIncompleteException.class,
                () -> structure("GB", "Unit A1", "Floor B2", "Zone C3", "Merewick 10000"));
        assertTrue(leftovers.getReason().contains("more than two hybrid"));
    }

    @Test
    void statedCountryMustMatchIbanOtherwiseIbanCountryIsUsed() {
        assertEquals("DE", structure("DE", "1 Willow Lane", "Lindenfels 10000 DE").getCountry());
        assertEquals("GB", structure("GB", "1 Willow Lane", "Merewick 10000").getCountry());
        AddressIncompleteException conflict = assertThrows(AddressIncompleteException.class,
                () -> structure("DE", "1 Main Street, Springfield CA"));
        assertEquals("stated country CA conflicts with IBAN country DE", conflict.getReason());
        assertEquals("Springfield", structure("CA", "1 Main Street, Springfield CA").getTownName());
        assertThrows(AddressIncompleteException.class, () -> structure("DE", "1 Main Street", "Springfield 12345 CA"));
        assertThrows(AddressIncompleteException.class, () -> structure("DE", "1 Main Street", "Springfield CA 12345 DE"));
        assertThrows(AddressIncompleteException.class, () -> structure("DE", "1 Main Street, Springfield", "CA"));
    }

    @Test
    void preservesNonIsoRegionTokensInsteadOfUsingThemAsCountry() {
        PostalAddress comma = structure("DE", "1 Main Street, Springfield NY");
        assertEquals("DE", comma.getCountry()); assertEquals("Springfield NY", comma.getTownName());
        PostalAddress postcode = structure("DE", "1 Main Street", "Springfield 12345 TX");
        assertEquals("DE", postcode.getCountry()); assertEquals("Springfield TX", postcode.getTownName());
    }

    @Test
    void acceptsStructuredInputAndValidatesExactlyOneAddressForm() {
        PaymentInstruction.Party party = party("GB", List.of("1 Willow Lane", "Merewick 10000"));
        PostalAddress supplied = structure("GB", "1 Willow Lane", "Merewick 10000");
        party.setAddressLines(null); party.setPostalAddress(supplied);
        assertSame(supplied, PostalAddressStructurer.structure(party, "debtor"));
        assertTrue(Validation.buildDefaultValidatorFactory().getValidator().validate(party).isEmpty());
        party.setAddressLines(List.of("duplicate"));
        assertFalse(Validation.buildDefaultValidatorFactory().getValidator().validate(party).isEmpty());
        party.setAddressLines(null); party.setPostalAddress(null);
        assertFalse(Validation.buildDefaultValidatorFactory().getValidator().validate(party).isEmpty());
        party.setPostalAddress(supplied); supplied.setCountry("DE");
        AddressIncompleteException conflict = assertThrows(AddressIncompleteException.class,
                () -> PostalAddressStructurer.structure(party, "debtor"));
        assertEquals("stated country DE conflicts with IBAN country GB", conflict.getReason());
    }

    private PostalAddress structure(String ibanCountry, String... lines) {
        return PostalAddressStructurer.structure(party(ibanCountry, List.of(lines)), "debtor");
    }

    private PaymentInstruction.Party party(String ibanCountry, List<String> lines) {
        PaymentInstruction.Party party = new PaymentInstruction.Party();
        party.setName("Test Party"); party.setIban(ibanCountry + "00AAAAAAAAAAAAAAA");
        party.setBic("OTTRGB2LXXX"); party.setAddressLines(lines); return party;
    }
}
