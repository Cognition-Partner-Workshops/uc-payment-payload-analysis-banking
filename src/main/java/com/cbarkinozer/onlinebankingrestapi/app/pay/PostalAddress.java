package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** ISO 20022 PostalAddress24 subset, in schema element order. */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"streetName", "buildingNumber", "buildingName", "postBox", "postCode", "townName", "country", "addressLines"})
public class PostalAddress {
    private String streetName;
    private String buildingNumber;
    private String buildingName;
    private String postBox;
    private String postCode;
    private String townName;
    private String country;
    private List<String> addressLines = new ArrayList<>();
}
