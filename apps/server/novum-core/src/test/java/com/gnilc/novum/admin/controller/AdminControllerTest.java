package com.gnilc.novum.admin.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.exception.AuthenticationFailedException;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.common.exception.UnauthorizedException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.admin.entity.dto.AdminDto;
import com.gnilc.novum.admin.entity.vo.AdminTokenVo;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.novum.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {
    private final AdminService service = mock(AdminService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(source, "en-US");
        mvc = MockMvcBuilders.standaloneSetup(
                new AdminController(service))
                .setControllerAdvice(
                        new RestExceptionHandlingConfiguration.RestExceptionControllerAdvice(messages))
                .build();
    }

    @Test
    void loginReturnsTokenOrAuthenticationBusinessError() throws Exception {
        when(service.login("admin", "secret"))
                .thenReturn(AdminTokenVo.of("access", "refresh"));
        doThrow(new AuthenticationFailedException("Incorrect username or password."))
                .when(service).login(null, null);

        mvc.perform(post("/sys/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));

        mvc.perform(post("/sys/admin/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001))
                .andExpect(jsonPath("$.error").value("Incorrect username or password."));
    }

    @Test
    void loginAndExpiredSessionResponsesPassThroughLocalizedServiceMessages() throws Exception {
        doThrow(new AuthenticationFailedException("用户名或密码错误。"))
                .when(service).login(null, null);
        when(service.refresh(null))
                .thenThrow(new UnauthorizedException("登录已过期，请重新登录。"));

        mvc.perform(post("/sys/admin/login")
                        .header("Accept-Language", "zh-CN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("用户名或密码错误。"));

        mvc.perform(post("/sys/admin/refresh")
                        .header("Accept-Language", "zh-CN"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20002))
                .andExpect(jsonPath("$.error").value("登录已过期，请重新登录。"))
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录。"));

        doThrow(new AuthenticationFailedException("Incorrect username or password."))
                .when(service).login(null, null);
        mvc.perform(post("/sys/admin/login")
                        .header("Accept-Language", "fr-FR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Incorrect username or password."));
    }

    @Test
    void refreshAndLogoutUseHttp401ForInvalidRefreshToken() throws Exception {
        when(service.refresh("good")).thenReturn(AdminTokenVo.of("new", "good"));
        when(service.refresh(null)).thenThrow(new UnauthorizedException(
                "Your login has expired. Please sign in again."));
        doThrow(new UnauthorizedException("Unauthorized."))
                .when(service).logout(null);

        mvc.perform(post("/sys/admin/refresh").header("X-Refresh-Token", "good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new"));
        mvc.perform(post("/sys/admin/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20002))
                .andExpect(jsonPath("$.error")
                        .value("Your login has expired. Please sign in again."))
                .andExpect(jsonPath("$.message")
                        .value("Your login has expired. Please sign in again."));
        mvc.perform(post("/sys/admin/logout").header("X-Refresh-Token", "good"))
                .andExpect(status().isOk());
        mvc.perform(post("/sys/admin/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized."));
    }

    @Test
    void profileAndAuthorizationReadRoutesPreserveResponseShape() throws Exception {
        AdminVo admin = new AdminVo();
        admin.setUsername("alice");
        when(service.getUserInfo()).thenReturn(admin);
        when(service.getRoleCodes()).thenReturn(List.of("admin"));
        when(service.getMenuAccessCodes()).thenReturn(List.of("user:create"));
        MenuRouteVo route = new MenuRouteVo();
        route.setName("Dashboard");
        when(service.getMenuRoutes()).thenReturn(List.of(route));

        mvc.perform(get("/sys/admin/user-info"))
                .andExpect(jsonPath("$.data.username").value("alice"));
        mvc.perform(get("/sys/admin/role-codes"))
                .andExpect(jsonPath("$.data[0]").value("admin"));
        mvc.perform(get("/sys/admin/menu/access-codes"))
                .andExpect(jsonPath("$.data[0]").value("user:create"));
        mvc.perform(get("/sys/admin/menu/routes"))
                .andExpect(jsonPath("$.data[0].name").value("Dashboard"));
    }

    @Test
    void currentProfileUpdateAcceptsAdminDtoAndDelegatesToCurrentUserService() throws Exception {
        doNothing().when(service).updateProfile(any());

        mvc.perform(jsonPost("/sys/admin/user-info/update", """
                        {
                          "id": 99,
                          "username": "ignored",
                          "nickname": "Alice",
                          "avatar": "https://example.test/alice.png",
                          "desc": "Platform administrator",
                          "status": false,
                          "roleCodes": ["ignored"]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        var captor = org.mockito.ArgumentCaptor.forClass(AdminDto.class);
        verify(service).updateProfile(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("Alice");
        assertThat(captor.getValue().getAvatar()).isEqualTo("https://example.test/alice.png");
        assertThat(captor.getValue().getDesc()).isEqualTo("Platform administrator");
    }

    @Test
    void currentPasswordUpdateAcceptsOnlySimplePasswordParameters() throws Exception {
        mvc.perform(jsonPost("/sys/admin/password/update", """
                        {
                          "id": 99,
                          "oldPassword": "Initial#123",
                          "newPassword": "Changed#456"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(service).updatePassword("Initial#123", "Changed#456");
    }

    @Test
    void managementRoutesDelegateAllCommands() throws Exception {
        when(service.getAdminPage(any())).thenReturn(new PageResult<>());

        mvc.perform(post("/sys/admin/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));
        mvc.perform(jsonPost("/sys/admin/create", "{\"username\":\"alice\"}"))
                .andExpect(status().isOk());
        mvc.perform(jsonPost("/sys/admin/update", "{\"id\":2}"))
                .andExpect(status().isOk());
        mvc.perform(jsonPost("/sys/admin/roles/save", "{\"id\":2,\"roleCodes\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(post("/sys/admin/remove/2")).andExpect(status().isOk());

        verify(service).createAdmin(any());
        verify(service).updateAdmin(any());
        verify(service).saveAdminRoles(any());
        verify(service).removeAdmin(2L);
    }

    @Test
    void adminUpdateDistinguishesOmittedAndExplicitNullProperties() throws Exception {
        mvc.perform(jsonPost("/sys/admin/update", "{\"id\":2}"))
                .andExpect(status().isOk());
        mvc.perform(jsonPost("/sys/admin/update", "{\"id\":2,\"avatar\":null,\"desc\":null}"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(AdminDto.class);
        verify(service, org.mockito.Mockito.times(2)).updateAdmin(captor.capture());
        AdminDto omitted = captor.getAllValues().get(0);
        assertThat(omitted.isAvatarSpecified()).isFalse();
        assertThat(omitted.isDescSpecified()).isFalse();
        AdminDto explicitNull = captor.getAllValues().get(1);
        assertThat(explicitNull.isAvatarSpecified()).isTrue();
        assertThat(explicitNull.getAvatar()).isNull();
        assertThat(explicitNull.isDescSpecified()).isTrue();
        assertThat(explicitNull.getDesc()).isNull();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder jsonPost(
            String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
