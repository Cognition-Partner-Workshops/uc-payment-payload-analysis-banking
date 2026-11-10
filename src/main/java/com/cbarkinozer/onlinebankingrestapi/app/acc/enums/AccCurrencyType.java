package com.cbarkinozer.onlinebankingrestapi.app.acc.enums;

public enum AccCurrencyType {
    TL("TRY"),
    USD("USD"),
    EURO("EUR");

    private final String isoCode;

    AccCurrencyType(String isoCode) {
        this.isoCode = isoCode;
    }

    public String getIsoCode() {
        return isoCode;
    }
}
