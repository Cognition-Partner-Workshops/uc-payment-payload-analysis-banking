package com.cbarkinozer.onlinebankingrestapi.app.acc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccMoneyTransferType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccMoneyTransferSaveDto {

    @NotNull
    private Long accountIdFrom;
    @NotNull
    private Long accountIdTo;
    @NotNull
    @Positive
    private BigDecimal amount;
    private String description;
    @NotNull
    private AccMoneyTransferType transferType;
}
