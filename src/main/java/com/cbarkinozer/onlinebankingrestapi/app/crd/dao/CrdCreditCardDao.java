package com.cbarkinozer.onlinebankingrestapi.app.crd.dao;

import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardDetailsDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.entity.CrdCreditCard;
import com.cbarkinozer.onlinebankingrestapi.app.gen.enums.GenStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrdCreditCardDao extends JpaRepository<CrdCreditCard,Long> {

    List<CrdCreditCard> findAllByStatusType(GenStatusType statusType);

    CrdCreditCard findByCardNoAndCvvNoAndExpireDateAndStatusType(Long cardNo, Long cvvNo, LocalDate expireDate, GenStatusType statusType);

    List<CrdCreditCard> findAllByCusCustomerIdAndStatusType(Long cusCustomerId, GenStatusType statusType);

    Optional<CrdCreditCard> findByIdAndCusCustomerId(Long id, Long cusCustomerId);

    Optional<CrdCreditCard> findByCardNoAndStatusType(Long cardNo, GenStatusType statusType);

    @Query(
            " select " +
                    " new com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardDetailsDto( " +
                    " cusCustomer.name," +
                    " cusCustomer.surname," +
                    " crdCreditCard.cardNo," +
                    " crdCreditCard.expireDate," +
                    " crdCreditCard.currentDebt," +
                    " crdCreditCard.minimumPaymentAmount," +
                    " crdCreditCard.cutoffDate," +
                    " crdCreditCard.dueDate " +
                    ") " +
                    " from CrdCreditCard crdCreditCard " +
                    " left join CusCustomer cusCustomer on crdCreditCard.cusCustomerId = cusCustomer.id " +
                    " where crdCreditCard.id = :creditCardId "
    )
    CrdCreditCardDetailsDto getCreditCardDetails(Long creditCardId);
}
