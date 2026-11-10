package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccCurrencyType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import java.util.Arrays;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.cbarkinozer.onlinebankingrestapi.app.acc.controller.AccAccountController;
import com.cbarkinozer.onlinebankingrestapi.app.acc.dto.*;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.*;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.*;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exception.GenCustomizedResponseEntityExceptionHandler;
import com.cbarkinozer.onlinebankingrestapi.app.gen.util.AccountDisclosure;
import com.cbarkinozer.onlinebankingrestapi.app.kafka.consumer.KafkaListenerService;
import com.cbarkinozer.onlinebankingrestapi.app.kafka.dto.LogMessage;
import com.cbarkinozer.onlinebankingrestapi.app.log.service.entityservice.LogDetailEntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountDisclosureTest {
    private static final String IBAN = "GB82WEST12345698765432";
    private final ObjectMapper json = PaymentCorpus.MAPPER;

    @Test
    void asteriskPrefixesCannotHideFullAccountNumbers() throws Exception {
        for (String value : new String[] {"*12345678", "****12345678", "**1234567890123456", "*GB82WEST12345698765432"}) {
            String expected = value.endsWith("5432") ? "****5432" : value.endsWith("3456") ? "****3456" : "****5678";
            assertEquals(expected, AccountDisclosure.redact(value));
            LogMessage message = new LogMessage();
            message.setMessage(value);
            assertEquals(expected, json.readTree(json.writeValueAsString(message)).get("message").asText());
            assertEquals(value, message.getMessage());
        }
        assertEquals("****5678", AccountDisclosure.redact("****5678"));
        assertEquals("****5678 ****3456", AccountDisclosure.redact("****5678 1234567890123456"));
        assertEquals("*150.00", AccountDisclosure.redact("*150.00"));
        assertEquals("****", AccountDisclosure.redact("****"));
    }

    @Test
    void commaAdjacentAccountsAreSeparateTokensNotDecimalAmounts() throws Exception {
        assertEquals("****5432,****6789",
                AccountDisclosure.redact("TR12000620000000000698765432,TR34000620000000000123456789"));
        for (String value : new String[] {"12345678,87654321", "account 12345678,87654321"}) {
            String expected = value.startsWith("account") ? "account ****5678,****4321" : "****5678,****4321";
            assertEquals(expected, AccountDisclosure.redact(value));
            LogMessage message = new LogMessage();
            message.setMessage(value);
            assertEquals(expected, json.readTree(json.writeValueAsString(message)).get("message").asText());
            assertEquals(value, message.getMessage());
        }
        assertEquals("Amount 12345678,50", AccountDisclosure.redact("Amount 12345678,50"));
        assertEquals("Amount 1,234,567.89", AccountDisclosure.redact("Amount 1,234,567.89"));
    }

    @Test
    void anAmountAfterALabelledAccountIsNotPartOfTheAccountToken() {
        assertEquals("account ****5678 150.00", AccountDisclosure.redact("account 12345678 150.00"));
        assertEquals("acct: ****5678 150,00", AccountDisclosure.redact("acct: 12345678 150,00"));
        assertEquals("account ****5678 150.00", AccountDisclosure.redact("account 1234 5678 150.00"));
        assertEquals("card no. ****5678 150.00", AccountDisclosure.redact("card no. 1234-5678 150.00"));
        assertEquals("amount 12345678 150.00", AccountDisclosure.redact("amount 12345678 150.00"));
        assertEquals("account ****5678", AccountDisclosure.redact("account 1234 5678"));
    }

    @Test
    void groupedNumbersAreMaskedInTextAndSerializedMessagesWithoutChangingInputs() throws Exception {
        for (String value : new String[] {"1234 5678 9012 3456", "1234-5678-9012-3456",
                "1234 5678-9012 3456", "123 456 789 0123456"}) {
            assertEquals("Payer ****3456", AccountDisclosure.redact("Payer " + value));
            LogMessage message = new LogMessage();
            message.setMessage("Payer " + value + "; bill 12345678; amount 12.34");
            assertEquals("Payer ****3456; bill 12345678; amount 12.34",
                    json.readTree(json.writeValueAsString(message)).get("message").asText());
            assertEquals("Payer " + value + "; bill 12345678; amount 12.34", message.getMessage());
        }
        assertEquals("IBAN ****5432", AccountDisclosure.redact("IBAN GB82-WEST-1234-5698-7654-32"));
        assertEquals("Payer ****2345", AccountDisclosure.redact("Payer 123 456 789 012 345"));
        assertEquals("Payer ****3456.", AccountDisclosure.redact("Payer 1234-5678-9012-3456."));
        assertEquals("Payer ****3456.", AccountDisclosure.redact("Payer 1234567890123456."));
        assertEquals("account: ****3456, thanks.", AccountDisclosure.redact("account: 1234567890123456, thanks."));
    }

    @Test
    void labelledGroupedAccountsAreMaskedEvenWhenShorterThanThirteenDigits() {
        assertEquals("account: ****5678", AccountDisclosure.redact("account: 1234 5678"));
        assertEquals("card no. ****5678", AccountDisclosure.redact("card no. 1234-5678"));
    }

    @Test
    void ordinaryShortNumbersAmountsDatesAndEmptyTextAreUnchanged() {
        for (String value : new String[] {"", "1234", "bill 12345678", "codes 1234 5678",
                "Amount 12.34", "Amount 1234.56", "Amount 1 234.56",
                "Amount 1 234 567 890 123.45", "Amount 1 234 567 890 123", "Amount 1,234,567.89",
                "Amount 1234567890123.45", "Date 2026-11-05", "****3456"}) {
            assertEquals(value, AccountDisclosure.redact(value));
        }
        assertNull(AccountDisclosure.redact(null));
    }

    @Test
    void accountEndpointsMaskOutputIncludingHateoasButNotModelValues() throws Exception {
        AccAccountDto dto = new AccAccountDto();
        dto.setId(12L);
        dto.setIbanNo(IBAN);
        AccAccountService service = mock(AccAccountService.class);
        when(service.findAllAccounts()).thenReturn(List.of(dto));
        when(service.findAccountById(12L)).thenReturn(dto);
        when(service.findAccountByCustomerId(3L)).thenReturn(List.of(dto));
        when(service.saveAccount(any())).thenReturn(dto);
        var mvc = MockMvcBuilders.standaloneSetup(new AccAccountController(service,
                mock(AccMoneyTransferService.class), mock(AccAccountActivityService.class))).build();
        for (String path : List.of("", "/12", "/account/customerId/3")) {
            String body = mvc.perform(get("/api/v1/accounts" + path)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertFalse(body.contains(IBAN), body);
            assertTrue(body.contains("****5432"), body);
        }
        String saved = mvc.perform(post("/api/v1/accounts/save-account")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(saved.contains(IBAN), saved);
        assertTrue(saved.contains("****5432"), saved);
        assertTrue(saved.contains("/api/v1/accounts/12"), saved);
        mvc.perform(patch("/api/v1/accounts/12")).andExpect(status().isOk());
        when(service.findAllAccounts()).thenReturn(List.of());
        mvc.perform(get("/api/v1/accounts")).andExpect(jsonPath("$.data").isEmpty());
        assertEquals(IBAN, dto.getIbanNo());
    }

    @Test
    void typedAccountsPreserveInputsAndHandleNullEmptyShortAndAlreadyMaskedValues() throws Exception {
        for (String value : new String[] {"", "123", "****1234", "001234567890"}) {
            AccAccountDto dto = json.readValue("{\"ibanNo\":\"" + value + "\"}", AccAccountDto.class);
            String expected = value.isEmpty() ? "" : "****" + value.substring(Math.max(0, value.length() - 4));
            assertEquals(expected, json.readTree(json.writeValueAsString(dto)).get("ibanNo").asText());
            assertEquals(value, dto.getIbanNo());
        }
        assertTrue(json.readTree(json.writeValueAsString(new AccAccountDto())).get("ibanNo").isNull());
    }

    @Test
    void cardAccountNumbersAreMaskedWithoutChangingNumericSpendInputs() throws Exception {
        long pan = 4111111111111234L;
        CrdCreditCardDto dto = new CrdCreditCardDto();
        dto.setCardNo(pan);
        CrdCreditCardDetailsDto details = new CrdCreditCardDetailsDto("A", "B", pan, null, null, null, null, null);
        for (Object value : List.of(dto, details)) {
            assertEquals("****1234", json.readTree(json.writeValueAsString(value)).get("cardNo").asText());
        }
        assertEquals(pan, dto.getCardNo());
        CrdCreditCardSpendDto input = json.readValue("{\"cardNo\":" + pan + "}", CrdCreditCardSpendDto.class);
        assertEquals(pan, input.getCardNo());
    }

    @Test
    void transferAndCardActivityDescriptionsCannotEchoAccountsButIdsRemainIds() throws Exception {
        AccMoneyTransferDto transfer = AccMoneyTransferDto.builder().accountIdFrom(12L).accountIdTo(13L).build();
        transfer.setDescription("Transfer " + IBAN + " 12345678901234567890123456");
        String body = json.writeValueAsString(transfer);
        assertFalse(body.contains(IBAN), body);
        assertFalse(body.contains("12345678901234567890123456"), body);
        assertTrue(body.contains("****5432"));
        assertEquals(12, json.readTree(body).get("accountIdFrom").asInt());
        assertTrue(transfer.getDescription().contains(IBAN));
        CrdCreditCardActivityDto activity = new CrdCreditCardActivityDto();
        activity.setDescription(IBAN);
        assertEquals("****5432", json.readTree(json.writeValueAsString(activity)).get("description").asText());
        activity.setDescription("");
        assertEquals("", json.readTree(json.writeValueAsString(activity)).get("description").asText());
        activity.setDescription(null);
        assertTrue(json.readTree(json.writeValueAsString(activity)).get("description").isNull());
    }

    @Test
    void paymentResponsesAreMaskedButPublishedWirePayloadsAndAmountsAreUnchanged() throws Exception {
        PaymentInstruction instruction = PaymentCorpus.instruction(1, new Random(7));
        instruction.setAmount(new BigDecimal("123456789012.34"));
        PaymentRenderer renderer = new PaymentRenderer();
        PaymentPayloads wire = renderer.render(instruction);
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        AccAccountEntityService accounts = mock(AccAccountEntityService.class);
        AccAccount debtorAccount = new AccAccount();
        debtorAccount.setCurrencyType(Arrays.stream(AccCurrencyType.values())
                .filter(currency -> currency.getIsoCode().equals(instruction.getCurrency())).findFirst().orElseThrow());
        when(accounts.getOwnedActiveAccountByIbanOrThrow(anyString())).thenReturn(debtorAccount);
        var mvc = MockMvcBuilders.standaloneSetup(new PaymentController(renderer, publisher, accounts)).build();
        for (String path : List.of("/payloads", "")) {
            String body = mvc.perform(post("/api/v1/outbound-payments" + path)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(instruction)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            for (String field : List.of("mt103", "pacs008", "event")) {
                String preview = json.readTree(body).get("data").get(field).asText();
                assertFalse(preview.contains(instruction.getDebtor().getIban()), field);
                assertFalse(preview.contains(instruction.getCreditor().getIban()), field);
                assertTrue(preview.contains("****"), field);
            }
            assertTrue(body.contains("123456789012.34"));
        }
        verify(publisher).publish(instruction.getEndToEndId(), wire.getEvent());
        assertTrue(wire.getMt103().contains(instruction.getDebtor().getIban()));
        assertTrue(wire.getPacs008().contains(instruction.getCreditor().getIban()));
        assertEquals(instruction.getDebtor().getIban(), json.readTree(wire.getEvent())
                .get("payment").get("debtor").get("iban").asText());
    }

    @Test
    void unexpectedErrorsNeverEchoAccounts() throws Exception {
        AccAccountService service = mock(AccAccountService.class);
        when(service.findAllAccounts()).thenThrow(new IllegalStateException("Database rejected " + IBAN));
        var accounts = MockMvcBuilders.standaloneSetup(new AccAccountController(service,
                mock(AccMoneyTransferService.class), mock(AccAccountActivityService.class)))
                .setControllerAdvice(new GenCustomizedResponseEntityExceptionHandler()).build();
        String unexpected = accounts.perform(get("/api/v1/accounts")).andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();
        assertFalse(unexpected.contains(IBAN), unexpected);
    }

    @Test
    void validationErrorsNeverEchoRejectedAccounts() throws Exception {
        var payments = MockMvcBuilders.standaloneSetup(new PaymentController(new PaymentRenderer(), mock(PaymentPublisher.class), mock(AccAccountEntityService.class)))
                .setControllerAdvice(new GenCustomizedResponseEntityExceptionHandler()).build();
        PaymentInstruction invalid = PaymentCorpus.instruction(1, new Random(7));
        invalid.getDebtor().setIban("INVALID-" + IBAN);
        String validation = payments.perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertFalse(validation.contains(IBAN), validation);
    }

    @Test
    void malformedPathArgumentsNeverEchoAccounts() throws Exception {
        var accounts = MockMvcBuilders.standaloneSetup(new AccAccountController(mock(AccAccountService.class),
                mock(AccMoneyTransferService.class), mock(AccAccountActivityService.class)))
                .setControllerAdvice(new GenCustomizedResponseEntityExceptionHandler()).build();
        String malformed = accounts.perform(get("/api/v1/accounts/" + IBAN)).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertFalse(malformed.contains(IBAN), malformed);
    }

    @Test
    void kafkaSmsSerializationMasksAccountMessages() throws Exception {
        LogMessage message = new LogMessage();
        message.setMessage("Account " + IBAN + " 12345678901234567890123456");
        message.setDescription("IBAN GB82 WEST 1234 5698 7654 32; card 378282246310005; account number: 001234567890");
        String encoded = new String(new JsonSerializer<LogMessage>().serialize("logs", message),
                java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(encoded.contains(IBAN), encoded);
        assertFalse(encoded.contains("12345678901234567890123456"), encoded);
        assertFalse(encoded.contains("GB82 WEST"), encoded);
        assertFalse(encoded.contains("378282246310005"), encoded);
        assertFalse(encoded.contains("001234567890"), encoded);
        assertFalse(message.toString().contains(IBAN));
        assertFalse(message.toString().contains("001234567890"));
        assertTrue(message.getMessage().contains(IBAN));
    }

    @Test
    void kafkaConsumerLogMasksAccountsWithoutMutatingInput() {
        LogMessage message = new LogMessage();
        message.setMessage("Account " + IBAN + " 12345678901234567890123456; iban gb82west12345698765432");
        KafkaListenerService listener = spy(new KafkaListenerService(mock(LogDetailEntityService.class)));
        doNothing().when(listener).saveLogToDb(any());
        Logger logger = (Logger) LoggerFactory.getLogger(KafkaListenerService.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            listener.listen(message);
            String logged = appender.list.get(0).getFormattedMessage();
            assertFalse(logged.contains(IBAN), logged);
            assertFalse(logged.contains("12345678901234567890123456"), logged);
            assertFalse(logged.contains("gb82west12345698765432"), logged);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        assertTrue(message.getMessage().contains(IBAN));
    }
}
