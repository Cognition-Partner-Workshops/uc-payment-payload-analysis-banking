package com.cbarkinozer.onlinebankingrestapi.app.cus.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CusCustomerSaveDtoValidationTest {

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

    private static CusCustomerSaveDto customerWithIdentityNo(Long identityNo) {
        CusCustomerSaveDto dto = new CusCustomerSaveDto();
        dto.setName("Test");
        dto.setSurname("Customer");
        dto.setIdentityNo(identityNo);
        dto.setPassword("test-only-password");
        return dto;
    }

    @ParameterizedTest
    @ValueSource(longs = {10000000000L, 11111111111L, 99999999999L})
    void elevenDigitIdentityNo_isValid(long identityNo) {
        assertTrue(validator.validate(customerWithIdentityNo(identityNo)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 9999999999L, 100000000000L, -11111111111L})
    void identityNoNotElevenDigits_isRejected(long identityNo) {
        assertFalse(validator.validate(customerWithIdentityNo(identityNo)).isEmpty());
    }
}
