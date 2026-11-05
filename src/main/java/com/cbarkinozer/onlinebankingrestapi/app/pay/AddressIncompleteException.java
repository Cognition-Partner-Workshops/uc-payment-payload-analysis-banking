package com.cbarkinozer.onlinebankingrestapi.app.pay;

public class AddressIncompleteException extends RuntimeException {
    private final String role;
    private final String reason;

    public AddressIncompleteException(String role, String reason) {
        super(role + " postal address: " + reason);
        this.role = role;
        this.reason = reason;
    }

    public String getRole() {
        return role;
    }

    public String getReason() {
        return reason;
    }
}
