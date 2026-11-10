package com.cbarkinozer.onlinebankingrestapi.app.acc.dto;

import com.cbarkinozer.onlinebankingrestapi.app.gen.util.AccountDisclosure;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccAccountType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccCurrencyType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccAccountDto {

    private Long id;
    private Long customerId;
    @JsonSerialize(using = AccountDisclosure.NumberSerializer.class)
    private String ibanNo;
    private BigDecimal currentBalance;
    private AccCurrencyType currencyType;
    private AccAccountType accountType;
    private GenStatusType statusType;
}