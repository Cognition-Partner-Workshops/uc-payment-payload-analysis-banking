package com.cbarkinozer.onlinebankingrestapi.app.gen;

import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.sec.security.JwtUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Sql("/legacy-fixtures.sql")
public class IntegrationTestBase extends BaseTest {

    @BeforeEach
    void authenticateCustomer() {
        CusCustomer customer = new CusCustomer();
        customer.setId(1L);
        customer.setIdentityNo(11111111111L);
        customer.setPassword("test-only-password");
        JwtUserDetails principal = JwtUserDetails.create(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
