package com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice;

import com.cbarkinozer.onlinebankingrestapi.app.acc.dao.AccAccountDao;
import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.service.BaseEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccAccountEntityService extends BaseEntityService<AccAccount, AccAccountDao> {

    public AccAccountEntityService(AccAccountDao accAccountDao){
        super(accAccountDao);
    }

    public List<AccAccount> findAllActiveAccounts() {

        Long currentCustomerId = getCurrentCustomerId();

        List<AccAccount> accAccountList = getDao().findAllByCustomerIdAndStatusType(currentCustomerId, GenStatusType.ACTIVE);

        return accAccountList;
    }

    public AccAccount getOwnedAccountOrThrow(Long accountId) {

        Long currentCustomerId = getCurrentCustomerId();

        if (accountId == null || currentCustomerId == null) {
            throw new ItemNotFoundException(AccErrorMessage.ACCOUNT_NOT_FOUND);
        }

        return getDao().findByIdAndCustomerId(accountId, currentCustomerId)
                .orElseThrow(() -> new ItemNotFoundException(AccErrorMessage.ACCOUNT_NOT_FOUND));
    }

    public AccAccount getOwnedActiveAccountByIbanOrThrow(String ibanNo) {

        Long currentCustomerId = getCurrentCustomerId();

        if (ibanNo == null || currentCustomerId == null) {
            throw new ItemNotFoundException(AccErrorMessage.ACCOUNT_NOT_FOUND);
        }

        return getDao().findByIbanNoAndCustomerId(ibanNo, currentCustomerId)
                .filter(accAccount -> accAccount.getStatusType() == GenStatusType.ACTIVE)
                .orElseThrow(() -> new ItemNotFoundException(AccErrorMessage.ACCOUNT_NOT_FOUND));
    }

    public List<AccAccount> findAccountByCustomerId(Long customerId) {

        if (customerId == null || !customerId.equals(getCurrentCustomerId())) {
            throw new ItemNotFoundException(AccErrorMessage.ACCOUNT_NOT_FOUND);
        }

        List<AccAccount> accAccountList = getDao().findAllByCustomerId(customerId);

        if (accAccountList.isEmpty()){
            throw new ItemNotFoundException(AccErrorMessage.ACCOUNT_NOT_FOUND);
        }

        return accAccountList;
    }

    public Optional<AccAccount> findAccountByIbanNo(Long id, String ibanNo) {
        Optional<AccAccount> accountOptional = getDao().findByIbanNo(id,ibanNo);

        return accountOptional;
    }

    public Optional<AccAccount> findAccountById(Long id) {

        Optional<AccAccount> accountOptional = getDao().findById(id);

        return accountOptional;
    }
}
