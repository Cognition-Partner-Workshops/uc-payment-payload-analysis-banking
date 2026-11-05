package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PostalAddress {
    @Size(max = 35) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
    private String buildingName;
    @Size(max = 70) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
    private String streetName;
    @Size(max = 16) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
    private String buildingNumber;
    @Size(max = 16) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
    private String postBox;
    @Size(max = 16) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
    private String postCode;
    @NotBlank @Size(max = 35) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
    private String townName;
    @NotBlank @Pattern(regexp = "[A-Z]{2}")
    private String country;
    @Size(max = 2)
    private List<@NotBlank @Size(max = 70) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+") String> addressLines;
}
