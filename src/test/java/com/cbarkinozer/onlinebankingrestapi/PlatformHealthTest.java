package com.cbarkinozer.onlinebankingrestapi;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.sec.security.JwtTokenGenerator;
import com.cbarkinozer.onlinebankingrestapi.app.sec.security.JwtUserDetails;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.location=file:src/main/resources/application.properties,classpath:application.properties")
@AutoConfigureMockMvc
class PlatformHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    @Test
    void healthIsPublicAndReflectsReadiness() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/health/readiness")).andExpect(status().isOk());
        mockMvc.perform(get("/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());

        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        try {
            mockMvc.perform(get("/health")).andExpect(status().isServiceUnavailable());
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void bankingEndpointsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/accounts")).andExpect(status().isUnauthorized());

        CusCustomer customer = new CusCustomer();
        customer.setId(1L);
        customer.setIdentityNo(11111111111L);
        customer.setPassword("test-only-password");
        JwtUserDetails principal = JwtUserDetails.create(customer);
        String token = jwtTokenGenerator.generateJwtToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        assertTrue(jwtTokenGenerator.validateToken(token));
        assertEquals(1L, jwtTokenGenerator.findUserIdByToken(token));
        assertFalse(jwtTokenGenerator.validateToken(""));
        assertFalse(jwtTokenGenerator.validateToken("malformed-token"));
    }

    @Test
    void rootLoggerWritesJsonOnlyToStdout() {
        LoggerContext logging = (LoggerContext) LoggerFactory.getILoggerFactory();
        Iterator<Appender<ILoggingEvent>> appenders = logging.getLogger("ROOT").iteratorForAppenders();
        assertTrue(appenders.hasNext());
        Appender<ILoggingEvent> appender = appenders.next();
        assertTrue(appender instanceof ConsoleAppender);
        ConsoleAppender<?> console = (ConsoleAppender<?>) appender;
        assertEquals("System.out", console.getTarget());
        assertTrue(console.getEncoder() instanceof LogstashEncoder);
        assertFalse(appenders.hasNext());
    }
}
