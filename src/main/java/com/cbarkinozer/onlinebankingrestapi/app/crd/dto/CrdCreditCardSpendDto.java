package com.cbarkinozer.onlinebankingrestapi.app.crd.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CrdCreditCardSpendDto {

    @NotNull
    private Long cardNo;
    @NotNull
    private Long cvvNo;
    @NotNull
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate expireDate;
    @NotNull
    @Positive
    private BigDecimal amount;
    private String description;

}
