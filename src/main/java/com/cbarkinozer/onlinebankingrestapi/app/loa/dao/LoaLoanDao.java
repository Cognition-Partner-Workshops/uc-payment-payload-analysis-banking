package com.cbarkinozer.onlinebankingrestapi.app.loa.dao;

import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoaLoanDao extends JpaRepository<LoaLoan,Long> {

    Optional<LoaLoan> findByIdAndCustomerId(Long id, Long customerId);
}
