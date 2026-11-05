package com.cbarkinozer.onlinebankingrestapi.app.gen;

import com.cbarkinozer.onlinebankingrestapi.app.gen.dto.RestResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseTest {

    protected ObjectMapper objectMapper;

    protected boolean isSuccess(MvcResult result) throws com.fasterxml.jackson.core.JsonProcessingException,
            UnsupportedEncodingException {

        JsonNode success = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("success");
        assertNotNull(success);
        assertTrue(success.isBoolean());
        return success.booleanValue();
    }

    protected RestResponse getRestResponse(MvcResult result) throws com.fasterxml.jackson.core.JsonProcessingException, UnsupportedEncodingException {
        return objectMapper.readValue(result.getResponse().getContentAsString(), RestResponse.class);
    }

    private boolean isSuccess(RestResponse restResponse) {
        return restResponse.isSuccess();
    }

}
