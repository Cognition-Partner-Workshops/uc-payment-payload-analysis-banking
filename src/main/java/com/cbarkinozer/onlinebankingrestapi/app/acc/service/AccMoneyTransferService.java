package com.cbarkinozer.onlinebankingrestapi.app.acc.service;

import com.cbarkinozer.onlinebankingrestapi.app.acc.mapper.AccAccountMapper;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyActivityDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyTransferDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.AccMoneyTransferSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccMoneyTransfer;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccAccountActivityType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccMoneyTransferEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.GenBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class AccMoneyTransferService {

    private final AccMoneyTransferEntityService accMoneyTransferEntityService;
    private final AccAccountActivityService accAccountActivityService;
    private final AccAccountValidationService accAccountValidationService;
    private final AccAccountEntityService accAccountEntityService;

    public AccMoneyTransferDto transferMoney(AccMoneyTransferSaveDto accMoneyTransferSaveDto) {

        accAccountValidationService.controlIsMoneyTransferSaveDtoIsNull(accMoneyTransferSaveDto);

        AccMoneyTransfer accMoneyTransfer = AccAccountMapper.INSTANCE.convertToAccMoneyTransfer(accMoneyTransferSaveDto);
        LocalDate localDate = LocalDate.now();
        accMoneyTransfer.setTransferDate(localDate);

        Long accountIdFrom = accMoneyTransfer.getAccountIdFrom();
        Long accountIdTo = accMoneyTransfer.getAccountIdTo();
        BigDecimal amount = accMoneyTransfer.getAmount();

        accAccountValidationService.controlIsAmountPositive(amount);
        accAccountValidationService.controlIsAccountIdExist(accountIdFrom);
        accAccountValidationService.controlIsAccountIdExist(accountIdTo);
        controlIsAccountOwnedByCurrentCustomer(accountIdFrom);

        AccMoneyActivityDto accMoneyActivityDtoOut = AccMoneyActivityDto.builder()
                .accountId(accountIdFrom)
                .amount(amount)
                .activityType(AccAccountActivityType.SEND)
                .build();

        AccMoneyActivityDto accMoneyActivityDtoIn = AccMoneyActivityDto.builder()
                .accountId(accountIdTo)
                .amount(amount)
                .activityType(AccAccountActivityType.GET)
                .build();

        accAccountActivityService.moneyOut(accMoneyActivityDtoOut);
        accAccountActivityService.moneyIn(accMoneyActivityDtoIn);

        accMoneyTransfer = accMoneyTransferEntityService.save(accMoneyTransfer);
        AccMoneyTransferDto accMoneyTransferDto = AccAccountMapper.INSTANCE.convertToAccMoneyTransferDto(accMoneyTransfer);

        return accMoneyTransferDto;
    }

    private void controlIsAccountOwnedByCurrentCustomer(Long accountId) {

        Long currentCustomerId = accAccountEntityService.getCurrentCustomerId();
        Long ownerCustomerId = accAccountEntityService.findAccountById(accountId)
                .map(AccAccount::getCustomerId)
                .orElse(null);

        if (currentCustomerId == null || !Objects.equals(currentCustomerId, ownerCustomerId)) {
            throw new GenBusinessException(AccErrorMessage.ACCOUNT_DOES_NOT_BELONG_TO_CUSTOMER);
        }
    }
}
