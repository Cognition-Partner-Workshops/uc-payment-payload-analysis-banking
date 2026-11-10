package com.cbarkinozer.onlinebankingrestapi.app.cus.dto;

import lombok.Data;

@Data
public class CusCustomerPasswordUpdateDto {

    private String currentPassword;
    private String newPassword;
}
