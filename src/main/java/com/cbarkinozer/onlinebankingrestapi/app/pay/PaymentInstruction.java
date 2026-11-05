package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class PaymentInstruction {
    @NotNull @Valid
    private Party debtor;
    @NotNull @Valid
    private Party creditor;
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;
    @NotNull @Pattern(regexp = "[A-Z]{3}")
    private String currency;
    @NotNull
    private LocalDate valueDate;
    @NotBlank @Size(max = 140) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
    private String remittanceInfo;
    @NotNull @Pattern(regexp = "SHA|OUR|BEN")
    private String chargeBearer;
    @NotBlank @Size(max = 16) @Pattern(regexp = "[A-Za-z0-9]+")
    private String endToEndId;

    @Data
    public static class Party {
        @NotBlank @Size(max = 35) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+")
        private String name;
        @NotNull @Pattern(regexp = "[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}")
        private String iban;
        @NotNull @Pattern(regexp = "[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?")
        private String bic;
        @Size(min = 1, max = 3)
        private List<@NotBlank @Size(max = 35) @Pattern(regexp = "[A-Za-z0-9 /?:().,'+&-]+") String> addressLines;
        @Valid
        private PostalAddress postalAddress;

        @JsonIgnore
        @AssertTrue(message = "exactly one of addressLines or postalAddress must be supplied")
        public boolean hasExactlyOneAddressForm() {
            return (addressLines != null) ^ (postalAddress != null);
        }
    }
}
