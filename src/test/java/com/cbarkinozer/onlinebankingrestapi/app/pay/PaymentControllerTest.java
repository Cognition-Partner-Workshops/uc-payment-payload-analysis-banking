package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccCurrencyType;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exception.GenCustomizedResponseEntityExceptionHandler;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest {

    private static MockMvc mvc(PaymentPublisher publisher, AccAccountEntityService accounts) {
        return MockMvcBuilders.standaloneSetup(new PaymentController(new PaymentRenderer(), publisher, accounts))
                .setControllerAdvice(new GenCustomizedResponseEntityExceptionHandler())
                .build();
    }

    private static AccAccount ownedAccount(String isoCurrency) {
        AccAccount account = new AccAccount();
        account.setCurrencyType(Arrays.stream(AccCurrencyType.values())
                .filter(type -> type.getIsoCode().equals(isoCurrency)).findFirst().orElse(AccCurrencyType.TL));
        return account;
    }

    @Test
    void rendersAndPublishesOnlyOnCreate() throws Exception {
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        AccAccountEntityService accounts = mock(AccAccountEntityService.class);
        PaymentInstruction p = PaymentCorpus.instruction(1, new Random(7));
        p.setCurrency("USD");
        when(accounts.getOwnedActiveAccountByIbanOrThrow(p.getDebtor().getIban())).thenReturn(ownedAccount("USD"));
        var mvc = mvc(publisher, accounts);
        String body = PaymentCorpus.MAPPER.writeValueAsString(p);
        mvc.perform(post("/api/v1/outbound-payments/payloads").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mt103").isString());
        verifyNoInteractions(publisher);
        mvc.perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(publisher).publish(eq(p.getEndToEndId()), eq(new PaymentRenderer().render(p).getEvent()));
    }

    @Test
    void rejectsDebtorAccountNotOwnedByCaller() throws Exception {
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        AccAccountEntityService accounts = mock(AccAccountEntityService.class);
        when(accounts.getOwnedActiveAccountByIbanOrThrow(anyString()))
                .thenThrow(new ItemNotFoundException(AccErrorMessage.ACCOUNT_NOT_FOUND));
        String body = PaymentCorpus.MAPPER.writeValueAsString(PaymentCorpus.instruction(1, new Random(7)));
        mvc(publisher, accounts).perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsCurrencyThatDoesNotMatchDebtorAccount() throws Exception {
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        AccAccountEntityService accounts = mock(AccAccountEntityService.class);
        PaymentInstruction p = PaymentCorpus.instruction(1, new Random(7));
        p.setCurrency("USD");
        when(accounts.getOwnedActiveAccountByIbanOrThrow(anyString())).thenReturn(ownedAccount("TRY"));
        String body = PaymentCorpus.MAPPER.writeValueAsString(p);
        mvc(publisher, accounts).perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsEmptyAndMalformedRequests() throws Exception {
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        AccAccountEntityService accounts = mock(AccAccountEntityService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PaymentController(new PaymentRenderer(), publisher, accounts)).build();
        for (String body : new String[]{"", "{}", "{broken"}) {
            mvc.perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(publisher, accounts);
    }
}
