package com.cbarkinozer.onlinebankingrestapi.app.crd.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CrdCreditCardSaveDto {

    @NotNull
    @PositiveOrZero
    private BigDecimal earning;
    @NotNull
    @Min(1)
    @Max(31)
    private Integer cutOffDay;
}
