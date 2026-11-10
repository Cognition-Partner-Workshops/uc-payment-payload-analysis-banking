package com.cbarkinozer.onlinebankingrestapi.app.sec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

@Data
public class SecLoginRequestDto {

    @NotNull
    @Positive
    private Long identityNo;
    @NotBlank
    private String password;
}
