package com.cbarkinozer.onlinebankingrestapi.app.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cbarkinozer.onlinebankingrestapi.app.kafka.dto.LogMessage;
import com.cbarkinozer.onlinebankingrestapi.app.log.entity.LogDetail;
import com.cbarkinozer.onlinebankingrestapi.app.log.service.entityservice.LogDetailEntityService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KafkaListenerServiceTest {

    private final LogDetailEntityService logDetailEntityService = mock(LogDetailEntityService.class);
    private final KafkaListenerService service = new KafkaListenerService(logDetailEntityService);

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void saveLogToDbPreservesInterruptStatusWithoutPrintingStackTrace() {
        PrintStream originalErr = System.err;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        boolean interrupted;
        try {
            System.setErr(new PrintStream(captured, true));
            System.setOut(new PrintStream(captured, true));
            Thread.currentThread().interrupt();
            service.saveLogToDb(message());
            interrupted = Thread.currentThread().isInterrupted();
        } finally {
            System.setErr(originalErr);
            System.setOut(originalOut);
        }

        assertTrue(interrupted, "interrupt status must be restored after catching InterruptedException");
        String output = captured.toString();
        assertFalse(output.contains("java.lang.InterruptedException"), "stack trace must not be dumped to stdout/stderr");
        assertFalse(output.contains("*****"), "debug println output must not be written to stdout");
        verifyPersistedMessage();
    }

    @Test
    void saveLogToDbPersistsMappedMessageWithoutInterruptingNormalProcessing() {
        service.saveLogToDb(message());

        assertFalse(Thread.currentThread().isInterrupted());
        verifyPersistedMessage();
    }

    private LogMessage message() {
        return new LogMessage(42L, "payment processed", "log details", LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    private void verifyPersistedMessage() {
        ArgumentCaptor<LogDetail> captor = ArgumentCaptor.forClass(LogDetail.class);
        verify(logDetailEntityService).save(captor.capture());
        LogDetail saved = captor.getValue();
        LogMessage expected = message();
        assertEquals(expected.getId(), saved.getId());
        assertEquals(expected.getMessage(), saved.getMessage());
        assertEquals(expected.getDescription(), saved.getDetails());
        assertEquals(expected.getDateTime(), saved.getDateTime());
    }
}
