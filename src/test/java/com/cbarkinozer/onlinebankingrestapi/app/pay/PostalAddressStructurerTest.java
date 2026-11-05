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
    void textCountryWinsOtherwiseIbanCountryIsUsed() {
        assertEquals("DE", structure("GB", "1 Willow Lane", "Lindenfels 10000 DE").getCountry());
        assertEquals("GB", structure("GB", "1 Willow Lane", "Merewick 10000").getCountry());
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
