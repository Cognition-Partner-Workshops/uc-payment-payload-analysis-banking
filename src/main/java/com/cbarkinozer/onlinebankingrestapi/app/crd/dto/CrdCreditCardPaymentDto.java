package com.cbarkinozer.onlinebankingrestapi.app.crd.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrdCreditCardPaymentDto {

    @NotNull
    private Long crdCreditCardId;
    @NotNull
    @Positive
    private BigDecimal amount;
}
