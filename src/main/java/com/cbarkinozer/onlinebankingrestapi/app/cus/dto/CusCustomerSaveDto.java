package com.cbarkinozer.onlinebankingrestapi.app.cus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CusCustomerSaveDto {

    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    @NotNull
    @Min(value = 10000000000L, message = "identityNo must be an 11-digit number")
    @Max(value = 99999999999L, message = "identityNo must be an 11-digit number")
    private Long identityNo;
    @NotBlank
    private String password;

}
