package com.cbarkinozer.onlinebankingrestapi.app.acc.service;

import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyTransferSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccMoneyTransfer;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccMoneyTransferType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccMoneyTransferEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.GenBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccMoneyTransferServiceOwnershipTest {

    private static final Long CURRENT_CUSTOMER_ID = 7L;
    private static final Long OTHER_CUSTOMER_ID = 99L;
    private static final Long ACCOUNT_ID_FROM = 1L;
    private static final Long ACCOUNT_ID_TO = 2L;

    @Mock
    private AccMoneyTransferEntityService accMoneyTransferEntityService;

    @Mock
    private AccAccountActivityService accAccountActivityService;

    @Mock
    private AccAccountValidationService accAccountValidationService;

    @Mock
    private AccAccountEntityService accAccountEntityService;

    @InjectMocks
    private AccMoneyTransferService accMoneyTransferService;

    private AccMoneyTransferSaveDto accMoneyTransferSaveDto;

    @BeforeEach
    void setUp() {
        accMoneyTransferSaveDto = new AccMoneyTransferSaveDto();
        accMoneyTransferSaveDto.setAccountIdFrom(ACCOUNT_ID_FROM);
        accMoneyTransferSaveDto.setAccountIdTo(ACCOUNT_ID_TO);
        accMoneyTransferSaveDto.setAmount(BigDecimal.valueOf(100));
        accMoneyTransferSaveDto.setTransferType(AccMoneyTransferType.DUE);
        accMoneyTransferSaveDto.setDescription("test");

        when(accAccountEntityService.getCurrentCustomerId()).thenReturn(CURRENT_CUSTOMER_ID);
        when(accMoneyTransferEntityService.save(any(AccMoneyTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AccAccount accountOwnedBy(Long customerId) {
        AccAccount accAccount = new AccAccount();
        accAccount.setId(ACCOUNT_ID_FROM);
        accAccount.setCustomerId(customerId);
        return accAccount;
    }

    @Test
    void shouldNotTransferMoney_WhenSourceAccount_DoesNotBelongToCurrentCustomer() {

        when(accAccountEntityService.findAccountById(ACCOUNT_ID_FROM))
                .thenReturn(Optional.of(accountOwnedBy(OTHER_CUSTOMER_ID)));

        assertThrows(GenBusinessException.class,
                () -> accMoneyTransferService.transferMoney(accMoneyTransferSaveDto));

        verify(accAccountActivityService, never()).moneyOut(any());
        verify(accAccountActivityService, never()).moneyIn(any());
        verify(accMoneyTransferEntityService, never()).save(any());
    }

    @Test
    void shouldTransferMoney_WhenSourceAccount_BelongsToCurrentCustomer() {

        when(accAccountEntityService.findAccountById(ACCOUNT_ID_FROM))
                .thenReturn(Optional.of(accountOwnedBy(CURRENT_CUSTOMER_ID)));

        accMoneyTransferService.transferMoney(accMoneyTransferSaveDto);

        verify(accAccountActivityService).moneyOut(any());
        verify(accAccountActivityService).moneyIn(any());
        verify(accMoneyTransferEntityService).save(any(AccMoneyTransfer.class));
    }
}
