package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.cbarkinozer.onlinebankingrestapi.OnlinebankingrestapiApplication;
import com.cbarkinozer.onlinebankingrestapi.app.config.H2TestProfileJPAConfig;
import javax.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {OnlinebankingrestapiApplication.class, H2TestProfileJPAConfig.class})
class ErrorDisclosureTest {
    @Autowired ErrorAttributes attributes;

    @Test
    void defaultErrorsDoNotExposePathsMessagesOrTracesEvenWhenRequested() throws Exception {
        String iban = "GB82WEST12345698765432";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/accounts/" + iban);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Failure for " + iban);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalArgumentException(iban));
        String body = PaymentCorpus.MAPPER.writeValueAsString(attributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.of(ErrorAttributeOptions.Include.values())));
        assertFalse(body.contains(iban), body);
        assertTrue(body.contains("404"), body);
    }
}
