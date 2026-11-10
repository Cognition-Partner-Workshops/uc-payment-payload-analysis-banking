package com.cbarkinozer.onlinebankingrestapi.app.acc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccMoneyActivityRequestDto {

    @NotNull
    private Long accountId;
    @NotNull
    @Positive
    private BigDecimal amount;

}
