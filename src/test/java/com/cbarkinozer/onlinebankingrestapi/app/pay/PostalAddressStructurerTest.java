package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostalAddressStructurerTest {
    private final PostalAddressStructurer structurer = new PostalAddressStructurer();

    private PostalAddress structure(String iban, String bic, String... lines) {
        PaymentInstruction.Party party = new PaymentInstruction.Party();
        party.setName("Wren Paper Ltd");
        party.setIban(iban);
        party.setBic(bic);
        party.setAddressLines(List.of(lines));
        return structurer.structure("debtor", party);
    }

    @Test
    void townAfterCommaAndCountryWithPostcode() {
        PostalAddress a = structure("ES3583010000000000100000", "OTTRES2LXXX", "47 Willow Lane, Solavilla", "10000 ES");
        assertEquals("Willow Lane", a.getStreetName());
        assertEquals("47", a.getBuildingNumber());
        assertEquals("Solavilla", a.getTownName());
        assertEquals("10000", a.getPostCode());
        assertEquals("ES", a.getCountry());
        assertTrue(a.getAddressLines().isEmpty());
    }

    @Test
    void postBoxAndTownBeforePostcode() {
        PostalAddress a = structure("DE00OTTR0000000000100001", "OTTRDE2LXXX", "P.O. Box 201", "Lindenfels 10001", "DE");
        assertEquals("P.O. Box 201", a.getPostBox());
        assertEquals("Lindenfels", a.getTownName());
        assertEquals("10001", a.getPostCode());
        assertEquals("DE", a.getCountry());
    }

    @Test
    void townAfterPostcodeTakesCountryFromIbanWhenBicAgrees() {
        PostalAddress a = structure("NL00OTTR0000100002", "OTTRNL2LXXX", "9 Willow Lane", "10002 Rietdam");
        assertEquals("Rietdam", a.getTownName());
        assertEquals("NL", a.getCountry());
        assertThrows(AddressIncompleteException.class,
                () -> structure("NL00OTTR0000100002", "OTTRDE2LXXX", "9 Willow Lane", "10002 Rietdam"));
    }

    @Test
    void buildingNameStreetAndTownOnOneLine() {
        PostalAddress a = structure("IE00OTTR0000000000100003", "OTTRIE2LXXX", "Juniper House", "221B Willow Lane", "Glenmere 10003 IE");
        assertEquals("Juniper House", a.getBuildingName());
        assertEquals("221B", a.getBuildingNumber());
        assertEquals("Glenmere", a.getTownName());
        PostalAddress single = structure("GB00OTTR0000000000100004", "OTTRGB2LXXX", "5 Willow Lane Merewick 10004");
        assertEquals("Willow Lane", single.getStreetName());
        assertEquals("Merewick", single.getTownName());
        assertEquals("10004", single.getPostCode());
    }

    @Test
    void ukPostcodeAndHybridLines() {
        PostalAddress a = structure("GB00OTTR0000000000100005", "OTTRGB2LXXX", "Rue des Fleurs 12", "Merewick SW1A 1AA");
        assertEquals("SW1A 1AA", a.getPostCode());
        assertEquals("Merewick", a.getTownName());
        assertEquals(List.of("Rue des Fleurs 12"), a.getAddressLines());
    }

    @Test
    void neverGuessesTown() {
        var e = assertThrows(AddressIncompleteException.class,
                () -> structure("FR00OTTR0000000000000100240", "OTTRFR2LXXX", "Warehouse Seven", "P.O. Box 540", "FR"));
        assertTrue(e.getReason().startsWith("no town"));
        assertThrows(AddressIncompleteException.class,
                () -> structure("GB00OTTR0000000000100005", "OTTRGB2LXXX", "5 Willow Lane Merewick"));
        assertThrows(AddressIncompleteException.class,
                () -> structure("GB00OTTR0000000000100005", "OTTRGB2LXXX", "Merewick"));
    }
}
