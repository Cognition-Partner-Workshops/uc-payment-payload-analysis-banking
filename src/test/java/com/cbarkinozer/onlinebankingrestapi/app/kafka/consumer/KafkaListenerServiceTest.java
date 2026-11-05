package com.cbarkinozer.onlinebankingrestapi.app.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cbarkinozer.onlinebankingrestapi.app.kafka.dto.LogMessage;
import com.cbarkinozer.onlinebankingrestapi.app.log.entity.LogDetail;
import com.cbarkinozer.onlinebankingrestapi.app.log.service.entityservice.LogDetailEntityService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
            service.saveLogToDb(new LogMessage());
            interrupted = Thread.currentThread().isInterrupted();
        } finally {
            System.setErr(originalErr);
            System.setOut(originalOut);
        }

        assertTrue(interrupted, "interrupt status must be restored after catching InterruptedException");
        String output = captured.toString();
        assertFalse(output.contains("java.lang.InterruptedException"), "stack trace must not be dumped to stdout/stderr");
        assertFalse(output.contains("*****"), "debug println output must not be written to stdout");
        verify(logDetailEntityService).save(any(LogDetail.class));
    }
}
