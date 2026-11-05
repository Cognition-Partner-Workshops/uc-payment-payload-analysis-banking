package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

final class PaymentCorpus {
    static final long SEED = 20261101L;
    static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    static final String[] COUNTRIES = {"GB", "DE", "FR", "NL", "IE", "ES"};
    static final String[] TOWNS = {"Merewick", "Lindenfels", "Valbrune", "Rietdam", "Glenmere", "Solavilla"};
    static final String[] NAMES = {"Aster Components", "Mallow Textiles", "Cinder Tools", "Fernlight Goods",
            "Wren Paper", "Bracken Ceramics", "Larkspur Parts", "Ember Glass"};

    static PaymentInstruction instruction(int index, Random random) {
        PaymentInstruction p = new PaymentInstruction();
        int debtorCountry = random.nextInt(COUNTRIES.length);
        int creditorCountry = (debtorCountry + 1 + random.nextInt(5)) % COUNTRIES.length;
        p.setDebtor(party(index, debtorCountry, false, random));
        p.setCreditor(party(index + 240, creditorCountry, index % 40 == 0, random));
        p.setAmount(BigDecimal.valueOf(100 + random.nextInt(9000000), 2));
        p.setCurrency(new String[]{"EUR", "GBP", "USD"}[random.nextInt(3)]);
        p.setValueDate(LocalDate.of(2026, 10, 1).plusDays(random.nextInt(31)));
        p.setRemittanceInfo("Invoice " + (76000 + index) + " / " + new String[]{"parts", "freight", "materials", "services"}[index % 4]);
        p.setChargeBearer(new String[]{"SHA", "OUR", "BEN"}[index % 3]);
        p.setEndToEndId(String.format("PAY%06d", index + 1));
        return p;
    }

    private static PaymentInstruction.Party party(int index, int country, boolean noTown, Random random) {
        PaymentInstruction.Party p = new PaymentInstruction.Party();
        p.setName(NAMES[random.nextInt(NAMES.length)] + (index % 9 == 0 ? " & Sons" : " Ltd"));
        String cc = COUNTRIES[country];
        String[] bankParts = {"OTTR", "83001742", "83001", "OTTR", "OTTR", "8301"};
        int[] bbanLengths = {18, 18, 23, 14, 18, 20};
        String prefix = bankParts[country];
        String bban = prefix + String.format("%0" + (bbanLengths[country] - prefix.length()) + "d", 100000L + index);
        String numeric = (bban + cc + "00").chars().mapToObj(c -> Character.isLetter(c)
                ? Integer.toString(c - 'A' + 10) : Character.toString((char)c)).reduce("", String::concat);
        p.setIban(cc + String.format("%02d", 98 - new BigInteger(numeric).mod(BigInteger.valueOf(97)).intValue()) + bban);
        p.setBic("OTTR" + cc + "2LXXX");
        String town = TOWNS[country];
        String street = (1 + random.nextInt(180)) + " Willow Lane";
        String postal = String.format("%05d", 10000 + index);
        if (noTown) {
            p.setAddressLines(List.of("Warehouse Seven", "P.O. Box " + (300 + index), cc));
        } else {
            switch (index % 5) {
                case 0: p.setAddressLines(List.of(street + ", " + town, postal + " " + cc)); break;
                case 1: p.setAddressLines(List.of("P.O. Box " + (200 + index), town + " " + postal, cc)); break;
                case 2: p.setAddressLines(List.of(street, postal + " " + town)); break;
                case 3: p.setAddressLines(List.of("Juniper House", street, town + " " + postal + " " + cc)); break;
                default: p.setAddressLines(List.of(street + " " + town + " " + postal)); break;
            }
        }
        return p;
    }
}
