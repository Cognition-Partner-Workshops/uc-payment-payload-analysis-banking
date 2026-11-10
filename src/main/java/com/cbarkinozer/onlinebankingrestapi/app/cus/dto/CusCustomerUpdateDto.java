package com.cbarkinozer.onlinebankingrestapi.app.cus.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CusCustomerUpdateDto {

    @NotBlank
    private String name;
    @NotBlank
    private String surname;
}
