package com.gnilc.novum.customer.controller;

import com.gnilc.common.exception.AuthenticationFailedException;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.common.exception.UnauthorizedException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.customer.entity.vo.CustomerTokenVo;
import com.gnilc.novum.customer.entity.vo.CustomerVo;
import com.gnilc.novum.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest {
    private final CustomerService service = mock(CustomerService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(source, "en-US");
        mvc = MockMvcBuilders.standaloneSetup(
                new CustomerController(service))
                .setControllerAdvice(
                        new RestExceptionHandlingConfiguration.RestExceptionControllerAdvice(messages))
                .build();
    }

    @Test
    void loginReturnsTokensOrTheSharedCredentialFailure() throws Exception {
        when(service.login("customer", "secret"))
                .thenReturn(CustomerTokenVo.of("access", "refresh"));
        doThrow(new AuthenticationFailedException("Incorrect username or password."))
                .when(service).login(null, null);

        mvc.perform(post("/customer/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"customer\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));

        mvc.perform(post("/customer/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001))
                .andExpect(jsonPath("$.error").value("Incorrect username or password."));
    }

    @Test
    void refreshAndLogoutReturnHttp401ForInvalidRefreshTokens() throws Exception {
        when(service.refresh("good")).thenReturn(CustomerTokenVo.of("new", "good"));
        when(service.refresh(null)).thenThrow(new UnauthorizedException(
                "Your login has expired. Please sign in again."));
        doThrow(new UnauthorizedException("Unauthorized."))
                .when(service).logout(null);

        mvc.perform(post("/customer/refresh").header("X-Refresh-Token", "good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new"));
        mvc.perform(post("/customer/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20002));
        mvc.perform(post("/customer/logout").header("X-Refresh-Token", "good"))
                .andExpect(status().isOk());
        mvc.perform(post("/customer/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20002));
    }

    @Test
    void userInfoReturnsTheCustomerContract() throws Exception {
        CustomerVo customer = new CustomerVo();
        customer.setId(7L);
        customer.setUserId(8L);
        customer.setUsername("customer");
        customer.setNickname("Customer");
        customer.setRoleCodes(List.of("customer"));
        when(service.getUserInfo()).thenReturn(customer);

        mvc.perform(get("/customer/user-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.username").value("customer"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("customer"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist());
    }
}
