package com.cbarkinozer.onlinebankingrestapi.app.pay;

/** The address cannot be made to meet the structured postal address rule without guessing. */
public class AddressIncompleteException extends RuntimeException {
    private final String party;
    private final String reason;

    public AddressIncompleteException(String party, String reason) {
        super(party + " address: " + reason);
        this.party = party;
        this.reason = reason;
    }

    public String getParty() {
        return party;
    }

    public String getReason() {
        return reason;
    }
}
