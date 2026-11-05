package com.cbarkinozer.onlinebankingrestapi;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
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

    @Test
    void healthIsPublicAndReflectsReadiness() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/health/readiness")).andExpect(status().isOk());

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
