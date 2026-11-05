package com.cbarkinozer.onlinebankingrestapi.app.crd.dto;

import com.cbarkinozer.onlinebankingrestapi.app.gen.util.AccountDisclosure;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@RequiredArgsConstructor
public class CrdCreditCardDetailsDto {

    private final String customerName;
    private final String customerSurname;
    @JsonSerialize(using = AccountDisclosure.NumberSerializer.class)
    @Schema(type = "string", example = "****1234")
    private final Long cardNo;
    private final LocalDate expireDate;
    private final BigDecimal currentDebt;
    private final BigDecimal minimumPaymentAmount;
    private final LocalDate cutoffDate;
    private final LocalDate dueDate;
    private List<CrdCreditCardActivityDto> crdCreditCardActivityDtoList;
}
