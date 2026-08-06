package com.gnilc.novum.customer.service.impl;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.common.exception.AuthenticationFailedException;
import com.gnilc.common.exception.UnauthorizedException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.customer.entity.bo.CustomerBo;
import com.gnilc.novum.customer.entity.vo.CustomerTokenVo;
import com.gnilc.novum.customer.entity.vo.CustomerVo;
import com.gnilc.novum.session.CustomerSessionManager;
import com.gnilc.novum.session.SessionTokenPair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {
    private static final long CUSTOMER_ID = 41L;
    private static final long USER_ID = 84L;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final CustomerSessionManager sessions = mock(CustomerSessionManager.class);
    private final RoleService roles = mock(RoleService.class);
    private CustomerServiceImpl customers;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.US);
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        customers = org.mockito.Mockito.spy(new CustomerServiceImpl(
                sessions,
                roles,
                new I18nMessageService(source, "en-US")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(USER_ID));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void loginCreatesAnIndependentCustomerSessionForValidCredentials() {
        CustomerBo customer = customer();
        doReturn(customer).when(customers).getCustomerByUsername("customer");
        when(sessions.createSession(USER_ID)).thenReturn(SessionTokenPair.of("access", "refresh"));

        CustomerTokenVo token = customers.login("customer", "123456");

        assertThat(token.getAccessToken()).isEqualTo("access");
        assertThat(token.getRefreshToken()).isEqualTo("refresh");
        verify(sessions).createSession(USER_ID);
    }

    @Test
    void loginRejectsInvalidCredentialsAndDisabledCustomers() {
        assertThatThrownBy(() -> customers.login(null, "123456"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");
        assertThatThrownBy(() -> customers.login("customer", " "))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");

        CustomerBo customer = customer();
        customer.setStatus(false);
        doReturn(customer).when(customers).getCustomerByUsername("customer");
        assertThatThrownBy(() -> customers.login("customer", "123456"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");

        customer.setStatus(true);
        assertThatThrownBy(() -> customers.login("customer", "wrong"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");
        verify(sessions, never()).createSession(any());
    }

    @Test
    void refreshAndLogoutDelegateToTheCustomerSessionManager() {
        when(sessions.refreshSession("refresh"))
                .thenReturn(SessionTokenPair.of("new-access", "refresh"));
        when(sessions.logout("refresh")).thenReturn(true);

        CustomerTokenVo token = customers.refresh("refresh");

        assertThat(token.getAccessToken()).isEqualTo("new-access");
        assertThat(token.getRefreshToken()).isEqualTo("refresh");
        customers.logout("refresh");
        verify(sessions).logout("refresh");
    }

    @Test
    void refreshRejectsMissingAndInvalidRefreshTokens() {
        when(sessions.refreshSession("invalid")).thenReturn(null);

        assertThatThrownBy(() -> customers.refresh(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your login has expired. Please sign in again.");
        assertThatThrownBy(() -> customers.refresh("invalid"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your login has expired. Please sign in again.");
    }

    @Test
    void logoutRejectsMissingAndInvalidRefreshTokens() {
        when(sessions.logout("invalid")).thenReturn(false);

        assertThatThrownBy(() -> customers.logout(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Unauthorized.");
        assertThatThrownBy(() -> customers.logout("invalid"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Unauthorized.");
    }

    @Test
    void getUserInfoReturnsOnlyCustomerFieldsAndRoleCodes() {
        CustomerBo customer = customer();
        String objectKey = "images/2026/08/05/customer.webp";
        customer.setAvatar(objectKey);
        doReturn(customer).when(customers).getCustomerByUserId(USER_ID);
        RoleBo baseline = new RoleBo();
        baseline.setCode("customer");
        RoleBo extra = new RoleBo();
        extra.setCode("member");
        when(roles.getRoles(USER_ID)).thenReturn(List.of(baseline, extra));

        CustomerVo info = customers.getUserInfo();

        assertThat(info.getId()).isEqualTo(CUSTOMER_ID);
        assertThat(info.getUserId()).isEqualTo(USER_ID);
        assertThat(info.getUsername()).isEqualTo("customer");
        assertThat(info.getNickname()).isEqualTo("Customer");
        assertThat(info.getAvatar()).isEqualTo(objectKey);
        assertThat(info.getAvatarUrl()).isNull();
        assertThat(info.getRoleCodes()).containsExactly("customer", "member");
    }

    private CustomerBo customer() {
        CustomerBo customer = new CustomerBo();
        customer.setId(CUSTOMER_ID);
        customer.setUserId(USER_ID);
        customer.setCreateTime(Instant.parse("2026-01-02T03:04:05Z"));
        customer.setUsername("customer");
        customer.setPassword(PASSWORD_ENCODER.encode("123456"));
        customer.setNickname("Customer");
        customer.setStatus(true);
        return customer;
    }
}
