package com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice;

import com.cbarkinozer.onlinebankingrestapi.app.loa.dao.LoaLoanDao;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoan;
import com.cbarkinozer.onlinebankingrestapi.app.loa.enums.LoaErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.service.BaseEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoaLoanEntityService extends BaseEntityService<LoaLoan, LoaLoanDao> {

    public LoaLoanEntityService(LoaLoanDao dao){
        super(dao);
    }

    public LoaLoan getOwnedLoanOrThrow(Long loanId) {

        Long currentCustomerId = getCurrentCustomerId();

        if (loanId == null || currentCustomerId == null) {
            throw new ItemNotFoundException(LoaErrorMessage.LOAN_NOT_FOUND);
        }

        return getDao().findByIdAndCustomerId(loanId, currentCustomerId)
                .orElseThrow(() -> new ItemNotFoundException(LoaErrorMessage.LOAN_NOT_FOUND));
    }

}
