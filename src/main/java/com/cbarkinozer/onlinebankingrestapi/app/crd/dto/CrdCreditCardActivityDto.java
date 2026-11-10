package com.cbarkinozer.onlinebankingrestapi.app.crd.dto;

import com.cbarkinozer.onlinebankingrestapi.app.gen.util.AccountDisclosure;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.cbarkinozer.onlinebankingrestapi.app.crd.enums.CrdCreditCardActivityType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CrdCreditCardActivityDto {

    private Long id;
    private Long crdCreditCardId;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    @JsonSerialize(using = AccountDisclosure.TextSerializer.class)
    private String description;
    private CrdCreditCardActivityType cardActivityType;
}
