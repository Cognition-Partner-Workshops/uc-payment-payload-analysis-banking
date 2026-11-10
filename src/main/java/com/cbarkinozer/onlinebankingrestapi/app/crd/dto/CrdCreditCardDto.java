package com.cbarkinozer.onlinebankingrestapi.app.crd.dto;

import com.cbarkinozer.onlinebankingrestapi.app.gen.util.AccountDisclosure;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CrdCreditCardDto {

    private Long id;
    private Long cusCustomerId;
    @JsonSerialize(using = AccountDisclosure.NumberSerializer.class)
    @Schema(type = "string", example = "****1234")
    private Long cardNo;
    private LocalDate expireDate;
    private BigDecimal totalLimit;
    private BigDecimal availableCardLimit;
    private BigDecimal currentDebt;
    private BigDecimal minimumPaymentAmount;
    private LocalDate cutoffDate;
    private LocalDate dueDate;
    private LocalDate cancelDate;
}
