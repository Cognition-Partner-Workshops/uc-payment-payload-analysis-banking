package com.cbarkinozer.onlinebankingrestapi.app.kafka.consumer;

import com.cbarkinozer.onlinebankingrestapi.app.kafka.dto.LogMessage;
import com.cbarkinozer.onlinebankingrestapi.app.log.entity.LogDetail;
import com.cbarkinozer.onlinebankingrestapi.app.log.service.entityservice.LogDetailEntityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaListenerServiceTest {

    @Mock
    private LogDetailEntityService logDetailEntityService;

    @InjectMocks
    private KafkaListenerService kafkaListenerService;

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void shouldRestoreInterruptFlagAndNotPrintStackTraceWhenInterrupted() {
        LogMessage logMessage = new LogMessage();
        logMessage.setMessage("message");

        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        boolean interrupted;
        try {
            Thread.currentThread().interrupt();
            kafkaListenerService.saveLogToDb(logMessage);
            interrupted = Thread.interrupted();
        } finally {
            System.setErr(originalErr);
        }

        assertTrue(interrupted);
        assertFalse(err.toString().contains("InterruptedException"));
        verify(logDetailEntityService).save(any(LogDetail.class));
    }
}
