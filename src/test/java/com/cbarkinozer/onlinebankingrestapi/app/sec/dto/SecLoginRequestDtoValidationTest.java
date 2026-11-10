package com.cbarkinozer.onlinebankingrestapi.app.sec.dto;

import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerSaveDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecLoginRequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void loginWithIdentityNoAndPassword_isValid() {
        SecLoginRequestDto dto = new SecLoginRequestDto();
        dto.setIdentityNo(11111111111L);
        dto.setPassword("test-only-password");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void loginWithoutPassword_isRejected() {
        SecLoginRequestDto dto = new SecLoginRequestDto();
        dto.setIdentityNo(11111111111L);
        dto.setPassword(" ");

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void loginWithoutIdentityNo_isRejected() {
        SecLoginRequestDto dto = new SecLoginRequestDto();
        dto.setPassword("test-only-password");

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void registerWithNegativeIdentityNo_isRejected() {
        CusCustomerSaveDto dto = new CusCustomerSaveDto();
        dto.setName("Test");
        dto.setSurname("Test");
        dto.setIdentityNo(-1L);
        dto.setPassword("test-only-password");

        assertFalse(validator.validate(dto).isEmpty());
    }
}
