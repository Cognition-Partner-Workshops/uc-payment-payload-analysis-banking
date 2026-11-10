package com.cbarkinozer.onlinebankingrestapi.app.loa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoaApplyLoanDto {

    @NotNull
    @Positive
    private Integer installmentCount;
    @NotNull
    @Positive
    private BigDecimal principalLoanAmount;
    @NotNull
    @Positive
    private BigDecimal monthlySalary;

}
