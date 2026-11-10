package com.cbarkinozer.onlinebankingrestapi.app.crd.service;

import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountActivityDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccAccountSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyActivityRequestDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.*;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCard;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCardActivity;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice.CrdCreditCardActivityEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice.CrdCreditCardEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrdCreditCardServiceTest {

    @Mock
    private  CrdCreditCardEntityService crdCreditCardEntityService;

    @Mock
    private  CrdCreditCardValidationService crdCreditCardValidationService;

    @Mock
    private  CrdCreditCardActivityEntityService crdCreditCardActivityEntityService;

    @InjectMocks
    private CrdCreditCardService crdCreditCardService;

    @Test
    void shouldFindAllCreditCards() {

        CrdCreditCard crdCreditCard = mock(CrdCreditCard.class);
        List<CrdCreditCard> crdCreditCardList = new ArrayList<>();
        crdCreditCardList.add(crdCreditCard);

        when(crdCreditCardEntityService.findAllActiveCreditCardList()).thenReturn(crdCreditCardList);

        List<CrdCreditCardDto> result = crdCreditCardService.findAllCreditCards();

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindCreditCardById() {

        Long id = 1L;

        CrdCreditCard crdCreditCard = mock(CrdCreditCard.class);
        when(crdCreditCard.getId()).thenReturn(id);

        when(crdCreditCardEntityService.getOwnedCardOrThrow(id)).thenReturn(crdCreditCard);

        CrdCreditCardDto crdCreditCardDto = crdCreditCardService.findCreditCardById(id);

        assertEquals(id, crdCreditCardDto.getId());
    }

    @Test
    void shouldGetCardDetails() {

        CrdCreditCardDetailsDto crdCreditCardDetailsDto = mock(CrdCreditCardDetailsDto.class);

        CrdCreditCard card = card();
        when(crdCreditCardEntityService.getOwnedCardOrThrow(1L)).thenReturn(card);

        when(crdCreditCardEntityService.getCreditCardDetails(1L)).thenReturn(crdCreditCardDetailsDto);

        CrdCreditCardDetailsDto result = crdCreditCardService.getCardDetails(1L);

        assertNotNull(result);
    }

    @Test
    void shouldSaveCreditCard() {

        CrdCreditCardSaveDto crdCreditCardSaveDto = mock(CrdCreditCardSaveDto.class);
        when(crdCreditCardSaveDto.getEarning()).thenReturn(BigDecimal.valueOf(1000));
        when(crdCreditCardSaveDto.getCutOffDay()).thenReturn(15);

        CrdCreditCard crdCreditCard = mock(CrdCreditCard.class);

        when(crdCreditCard.getId()).thenReturn(1L);

        when(crdCreditCardEntityService.save(any())).thenReturn(crdCreditCard);

        CrdCreditCardDto result = crdCreditCardService.saveCreditCard(crdCreditCardSaveDto);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldCancelCreditCard() {

        CrdCreditCard crdCreditCard = mock(CrdCreditCard.class);

        when(crdCreditCardEntityService.getOwnedCardOrThrow(anyLong())).thenReturn(crdCreditCard);

        crdCreditCardService.cancelCreditCard(1L);

        verify(crdCreditCardEntityService).getOwnedCardOrThrow(anyLong());
        verify(crdCreditCardEntityService).save(any());
    }

    @Test
    void shouldFindCreditCardActivityBetweenDates() {

        CrdCreditCardActivity crdCreditCardActivity = mock(CrdCreditCardActivity.class);
        List<CrdCreditCardActivity> crdCreditCardActivityList = new ArrayList<>();
        crdCreditCardActivityList.add(crdCreditCardActivity);

        Long id =1L;
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusMonths(1);

        when(crdCreditCardActivityEntityService.findCreditCardActivityBetweenDates(
                id,startDate.atStartOfDay(),endDate.atStartOfDay())
        ).thenReturn(crdCreditCardActivityList);

        List<CrdCreditCardActivityDto> result = crdCreditCardService.findCreditCardActivityBetweenDates(
                id,startDate,endDate);

        assertEquals(1, result.size());
    }

    @Test
    void shouldSpendMoney() {

        CrdCreditCardSpendDto crdCreditCardSpendDto = mock(CrdCreditCardSpendDto.class);
        CrdCreditCard card = card();
        LocalDate expireDate = LocalDate.now().plusYears(1);
        card.setCusCustomerId(1L);
        card.setCvvNo(111L);
        card.setExpireDate(expireDate);
        when(crdCreditCardSpendDto.getCvvNo()).thenReturn(111L);
        when(crdCreditCardSpendDto.getExpireDate()).thenReturn(expireDate);
        when(crdCreditCardEntityService.getCurrentCustomerId()).thenReturn(1L);
        when(crdCreditCardEntityService.findActiveCardByCardNo(0L)).thenReturn(Optional.of(card));
        stubSaves(card);

        when(crdCreditCardSpendDto.getAmount()).thenReturn(BigDecimal.valueOf(200));

        CrdCreditCardActivityDto result = crdCreditCardService.spendMoney(crdCreditCardSpendDto);

        assertEquals(BigDecimal.valueOf(200),result.getAmount());
    }

    @Test
    void shouldRefundMoney() {

        CrdCreditCard card = card();
        CrdCreditCardActivity activity = new CrdCreditCardActivity();
        activity.setCrdCreditCardId(1L);
        activity.setAmount(BigDecimal.valueOf(200));
        when(crdCreditCardActivityEntityService.getByIdWithControl(1L)).thenReturn(activity);
        when(crdCreditCardEntityService.getOwnedCardOrThrow(1L)).thenReturn(card);
        stubSaves(card);

        CrdCreditCardActivityDto result = crdCreditCardService.refundMoney(1L);

        assertEquals(BigDecimal.valueOf(200),result.getAmount());
    }

    @Test
    void shouldReceivePayment() {

        CrdCreditCardPaymentDto crdCreditCardPaymentDto = mock(CrdCreditCardPaymentDto.class);
        CrdCreditCard card = card();
        when(crdCreditCardPaymentDto.getCrdCreditCardId()).thenReturn(1L);
        when(crdCreditCardEntityService.getOwnedCardOrThrow(1L)).thenReturn(card);
        stubSaves(card);

        when(crdCreditCardPaymentDto.getAmount()).thenReturn(BigDecimal.valueOf(200));

        CrdCreditCardActivityDto result = crdCreditCardService.receivePayment(crdCreditCardPaymentDto);

        assertEquals(BigDecimal.valueOf(200),result.getAmount());

    }
    private CrdCreditCard card() {
        CrdCreditCard card = new CrdCreditCard();
        card.setId(1L);
        card.setCutoffDate(LocalDate.now().plusMonths(1));
        card.setCurrentDebt(BigDecimal.valueOf(500));
        card.setAvailableCardLimit(BigDecimal.valueOf(1000));
        return card;
    }

    private void stubSaves(CrdCreditCard card) {
        when(crdCreditCardEntityService.save(card)).thenReturn(card);
        when(crdCreditCardActivityEntityService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}