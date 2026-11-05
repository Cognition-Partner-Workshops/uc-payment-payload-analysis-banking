package com.cbarkinozer.onlinebankingrestapi.app.crd.service.entityservice;

import com.cbarkinozer.onlinebankingrestapi.app.crd.dao.CrdCreditCardDao;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardDetailsDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCard;
import com.cbarkinozer.onlinebankingrestapi.app.crd.enums.CrdErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.service.BaseEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CrdCreditCardEntityService extends BaseEntityService<CrdCreditCard, CrdCreditCardDao> {

    public CrdCreditCardEntityService(CrdCreditCardDao dao) {
        super(dao);
    }

    public List<CrdCreditCard> findAllByStatusType(GenStatusType statusType){
        return getDao().findAllByStatusType(statusType);
    }

    public List<CrdCreditCard> findAllActiveCreditCardList() {
        return getDao().findAllByCusCustomerIdAndStatusType(getCurrentCustomerId(), GenStatusType.ACTIVE);
    }

    public CrdCreditCard getOwnedCardOrThrow(Long cardId) {

        Long currentCustomerId = getCurrentCustomerId();

        if (cardId == null || currentCustomerId == null) {
            throw new ItemNotFoundException(CrdErrorMessage.CREDIT_CARD_NOT_FOUND);
        }

        return getDao().findByIdAndCusCustomerId(cardId, currentCustomerId)
                .orElseThrow(() -> new ItemNotFoundException(CrdErrorMessage.CREDIT_CARD_NOT_FOUND));
    }

    public Optional<CrdCreditCard> findActiveCardByCardNo(Long cardNo) {
        return getDao().findByCardNoAndStatusType(cardNo, GenStatusType.ACTIVE);
    }

    /** Runs in its own transaction so the count survives the rollback of the rejected spend. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedCvvAttempt(Long cardId) {

        getDao().findById(cardId).ifPresent(crdCreditCard -> {
            Integer failedCount = crdCreditCard.getFailedCvvAttemptCount();
            crdCreditCard.setFailedCvvAttemptCount(failedCount == null ? 1 : failedCount + 1);
            save(crdCreditCard);
        });
    }

    public CrdCreditCard findByCardNoAndCvvNoAndExpireDate(Long cardNo, Long cvvNo, LocalDate expireDate){
        return getDao().findByCardNoAndCvvNoAndExpireDateAndStatusType(cardNo, cvvNo, expireDate, GenStatusType.ACTIVE);
    }

    public CrdCreditCardDetailsDto getCreditCardDetails(Long creditCardId) {
        return getDao().getCreditCardDetails(creditCardId);
    }
}
