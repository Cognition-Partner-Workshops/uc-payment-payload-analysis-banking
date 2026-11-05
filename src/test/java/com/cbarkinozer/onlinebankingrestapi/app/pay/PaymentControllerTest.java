package com.cbarkinozer.onlinebankingrestapi.app.pay;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest {
    @Test
    void rendersAndPublishesOnlyOnCreate() throws Exception {
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PaymentController(new PaymentRenderer(), publisher)).build();
        PaymentInstruction p = PaymentCorpus.instruction(1, new Random(7));
        String body = PaymentCorpus.MAPPER.writeValueAsString(p);
        mvc.perform(post("/api/v1/outbound-payments/payloads").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mt103").isString());
        verifyNoInteractions(publisher);
        mvc.perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(publisher).publish(eq(p.getEndToEndId()), eq(new PaymentRenderer().render(p).getEvent()));
    }

    @Test
    void refusesAddressWithoutTownAndPublishesNothing() throws Exception {
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PaymentController(new PaymentRenderer(), publisher)).build();
        PaymentInstruction p = PaymentCorpus.instruction(1, new Random(7));
        p.getCreditor().setAddressLines(java.util.List.of("Warehouse Seven", "P.O. Box 301", "FR"));
        mvc.perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON)
                        .content(PaymentCorpus.MAPPER.writeValueAsString(p)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("creditor address: no town")));
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsEmptyAndMalformedRequests() throws Exception {
        PaymentPublisher publisher = mock(PaymentPublisher.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PaymentController(new PaymentRenderer(), publisher)).build();
        for (String body : new String[]{"", "{}", "{broken"}) {
            mvc.perform(post("/api/v1/outbound-payments").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(publisher);
    }
}
