package com.cbarkinozer.onlinebankingrestapi.app.cus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

@Data
public class CusCustomerSaveDto {

    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    @NotNull
    @Positive
    private Long identityNo;
    @NotBlank
    private String password;

}
