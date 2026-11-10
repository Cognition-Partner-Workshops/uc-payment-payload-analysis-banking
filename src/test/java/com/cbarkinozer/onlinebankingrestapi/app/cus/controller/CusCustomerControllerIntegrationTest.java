package com.cbarkinozer.onlinebankingrestapi.app.cus.controller;

import com.cbarkinozer.onlinebankingrestapi.OnlinebankingrestapiApplication;
import com.cbarkinozer.onlinebankingrestapi.app.config.H2TestProfileJPAConfig;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerUpdateDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.cus.enums.CusCustomerRoleType;
import com.cbarkinozer.onlinebankingrestapi.app.gen.IntegrationTestBase;
import com.cbarkinozer.onlinebankingrestapi.app.sec.security.JwtUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.UrlHandlerFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {OnlinebankingrestapiApplication.class, H2TestProfileJPAConfig.class})
public class CusCustomerControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE_PATH = "/api/v1/customers";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void authenticateAdmin() {
        CusCustomer admin = new CusCustomer();
        admin.setId(1L);
        admin.setIdentityNo(11111111111L);
        admin.setPassword("test-only-password");
        admin.setRoleType(CusCustomerRoleType.ADMIN);
        JwtUserDetails principal = JwtUserDetails.create(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .addFilters(context.getBean(UrlHandlerFilter.class)).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void shouldFindAllCustomers() throws Exception {
        MvcResult result = mockMvc.perform(
                get(BASE_PATH).content("").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertTrue(isSuccess);
    }

    @Test
    void shouldFindCustomerById() throws Exception {
        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/7").content("7L").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertTrue(isSuccess);
    }

    @Test
    void shouldNotFindCustomerByIdWhenUsrUserIdIsNotExist() throws Exception {
        MvcResult result = mockMvc.perform(
                get(BASE_PATH + "/9").content("9L").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertFalse(isSuccess);
    }

    @Test
    void shouldSaveCustomer() throws Exception {

        CusCustomerSaveDto cusCustomerSaveDto = new CusCustomerSaveDto();
        cusCustomerSaveDto.setName("Test");
        cusCustomerSaveDto.setSurname("Test");
        cusCustomerSaveDto.setIdentityNo(33333333333L);
        cusCustomerSaveDto.setPassword("test-only-password");

        String content = objectMapper.writeValueAsString(cusCustomerSaveDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/save-customer/").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertTrue(isSuccess);
    }

    @Test
    void shouldNotSaveCustomer_WhenIdentityNo_IsNotUnique() throws Exception {

        CusCustomerSaveDto cusCustomerSaveDto = new CusCustomerSaveDto();
        cusCustomerSaveDto.setName("Test");
        cusCustomerSaveDto.setSurname("Test");
        cusCustomerSaveDto.setIdentityNo(11111111111L);
        cusCustomerSaveDto.setPassword("test-only-password");

        String content = objectMapper.writeValueAsString(cusCustomerSaveDto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/save-customer/").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertFalse(isSuccess);
    }

    @Test
    void shouldUpdateCustomer() throws Exception {

        CusCustomerUpdateDto cusCustomerUpdateDto = new CusCustomerUpdateDto();
        cusCustomerUpdateDto.setName("Test");
        cusCustomerUpdateDto.setSurname("Test");

        String content = objectMapper.writeValueAsString(cusCustomerUpdateDto);

        MvcResult result = mockMvc.perform(
                put(BASE_PATH +"/update-customer/").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertTrue(isSuccess);
    }

    @Test
    void shouldNotUpdateCustomer_WhenFields_AreNull() throws Exception {

        CusCustomerUpdateDto cusCustomerUpdateDto = new CusCustomerUpdateDto();

        cusCustomerUpdateDto.setName(" ");
        cusCustomerUpdateDto.setSurname("");

        String content = objectMapper.writeValueAsString(cusCustomerUpdateDto);

        MvcResult result = mockMvc.perform(
                put(BASE_PATH +"/update-customer/").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertFalse(isSuccess);
    }

    @Test
    void shouldDeleteCustomer() throws Exception {

        MvcResult result = mockMvc.perform(
                delete(BASE_PATH +"/1").content("1").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertTrue(isSuccess);
    }

    @Test
    void shouldNoDeleteCustomer_WhenId_DoesNotExist() throws Exception {

        MvcResult result = mockMvc.perform(
                delete(BASE_PATH +"/9999").content("9999").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound()).andReturn();

        boolean isSuccess = isSuccess(result);

        assertFalse(isSuccess);
    }

    @Test
    void shouldNotSaveCustomer_WhenFields_AreMissing() throws Exception {
        CusCustomerSaveDto dto = new CusCustomerSaveDto();
        dto.setName(" ");
        dto.setSurname("Test");
        dto.setPassword("test-only-password");

        String content = objectMapper.writeValueAsString(dto);

        MvcResult result = mockMvc.perform(
                post(BASE_PATH+"/save-customer/").content(content).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest()).andReturn();

        assertFalse(isSuccess(result));
    }
}
