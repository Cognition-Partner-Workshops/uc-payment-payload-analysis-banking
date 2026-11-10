package com.cbarkinozer.onlinebankingrestapi.app.loa.service;

import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.LoaCalculateLateFeeResponseDto;
import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.LoaCalculateLoanResponseDto;
import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.LoaLoanDto;
import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.LoaApplyLoanDto;
import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.LoaPayInstallmentResponseDto;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoan;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoanPayment;
import com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice.LoaLoanEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice.LoaLoanPaymentEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoaLoanServiceTest {

    @Mock
    private LoaLoanValidationService loaLoanValidationService;

    @Mock
    private LoaLoanEntityService loaLoanEntityService;

    @Mock
    private LoaLoanPaymentEntityService loaLoanPaymentEntityService;

    @InjectMocks
    private LoaLoanService loaLoanService;

    @Test
    void shouldCalculateLoan() {

        LoaCalculateLoanResponseDto result = loaLoanService.calculateLoan(24, BigDecimal.valueOf(3000));

        assertNotNull(result);
    }

    @Test
    void shouldCalculateLateFee() {

        LoaLoan loan = loan();
        loan.setDueDate(LocalDate.now().minusDays(5));
        when(loaLoanEntityService.getOwnedLoanOrThrow(1L)).thenReturn(loan);
        when(loaLoanValidationService.controlIsLoanDueDatePast(loan.getDueDate())).thenReturn(5L);

        LoaCalculateLateFeeResponseDto result = loaLoanService.calculateLateFee(1L);

        assertNotNull(result);
    }

    @Test
    void shouldFindLoanById() {

        Long id = 1L;

        LoaLoan loaLoan = loan();
        loaLoan.setId(id);

        when(loaLoanEntityService.getOwnedLoanOrThrow(id)).thenReturn(loaLoan);

        LoaLoanDto loaLoanDto = loaLoanService.findLoanById(id);

        assertEquals(id, loaLoanDto.getId());
    }

    @Test
    void shouldApplyLoan() {

        LoaApplyLoanDto request = new LoaApplyLoanDto();
        request.setInstallmentCount(24);
        request.setPrincipalLoanAmount(BigDecimal.valueOf(3000));
        request.setMonthlySalary(BigDecimal.valueOf(5000));
        when(loaLoanEntityService.save(any())).thenAnswer(invocation -> {
            LoaLoan saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        LoaLoanDto result = loaLoanService.applyLoan(request);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldPayInstallment() {

        LoaLoan loan = loan();
        when(loaLoanEntityService.getOwnedLoanOrThrow(1L)).thenReturn(loan);
        when(loaLoanEntityService.save(loan)).thenReturn(loan);
        when(loaLoanPaymentEntityService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoaPayInstallmentResponseDto result =
                loaLoanService.payInstallment(1L);

        assertEquals(1L, result.getLoanId());
        assertEquals(BigDecimal.valueOf(100), result.getPaymentAmount());
        assertEquals(BigDecimal.valueOf(900), result.getRemainingPrincipal());
    }

    @Test
    void shouldPayLoanOff() {

        LoaLoan loaLoan = loan();

        when(loaLoanEntityService.getOwnedLoanOrThrow(anyLong())).thenReturn(loaLoan);
        when(loaLoanEntityService.save(loaLoan)).thenReturn(loaLoan);

        loaLoanService.payLoanOff(1L);

        verify(loaLoanEntityService).getOwnedLoanOrThrow(anyLong());
        verify(loaLoanEntityService).save(any());
    }

    private LoaLoan loan() {
        LoaLoan loan = new LoaLoan();
        loan.setDueDate(LocalDate.now().plusMonths(1));
        loan.setPrincipalLoanAmount(BigDecimal.valueOf(1000));
        loan.setRemainingPrincipal(BigDecimal.valueOf(1000));
        loan.setMonthlyInstallmentAmount(BigDecimal.valueOf(100));
        return loan;
    }
}