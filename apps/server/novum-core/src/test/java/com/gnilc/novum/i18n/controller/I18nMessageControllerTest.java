package com.gnilc.novum.i18n.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.exception.RestExceptionHandlingConfiguration;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.novum.i18n.entity.dto.I18nMessageDto;
import com.gnilc.novum.i18n.entity.vo.I18nMessageValueVo;
import com.gnilc.novum.i18n.entity.vo.I18nMessageVo;
import com.gnilc.novum.i18n.entity.vo.I18nMessageItemVo;
import com.gnilc.novum.i18n.service.DynamicI18nMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.hamcrest.Matchers.hasItems;

class I18nMessageControllerTest {

    private final DynamicI18nMessageService service = mock(DynamicI18nMessageService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/common/messages", "i18n/system/messages");
        messageSource.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(messageSource, "zh-CN");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        RestExceptionHandlingConfiguration exceptionHandling =
                new RestExceptionHandlingConfiguration();
        mvc = MockMvcBuilders.standaloneSetup(new I18nMessageController(service))
                .setControllerAdvice(
                        new RestExceptionHandlingConfiguration.RestExceptionControllerAdvice(messages))
                .setLocaleResolver(exceptionHandling.localeResolver("zh-CN"))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void runtimeBundleUsesCategoryWhileAdministrationUsesGlobalMessageKey() throws Exception {
        I18nMessageVo message = message("admin", "menu.dashboard.title", "首页", "Dashboard");
        when(service.getMessageBundle("admin"))
                .thenReturn(Map.of("zh-CN", Map.of("menu", Map.of("title", "首页"))));
        when(service.getSupportedCategories()).thenReturn(List.of("default", "admin"));
        when(service.getMessagePage(any(I18nMessagePageDto.class)))
                .thenReturn(new PageResult<>(
                        List.of(new I18nMessageItemVo("admin", message.getMessageKey(), message.getValues())),
                        1,
                        10,
                        1));
        when(service.getMessageValues(message.getMessageKey())).thenReturn(message);

        mvc.perform(post("/sys/i18n-message/bundle/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zh-CN.menu.title").value("首页"));
        mvc.perform(post("/sys/i18n-message/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("default"))
                .andExpect(jsonPath("$.data[1]").value("admin"));
        mvc.perform(post("/sys/i18n-message/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"admin\",\"key\":\"dashboard\",\"currentPage\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].category").value("admin"))
                .andExpect(jsonPath("$.data.list[0].values[1].locale").value("en-US"));
        mvc.perform(post("/sys/i18n-message/values/menu.dashboard.title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("admin"))
                .andExpect(jsonPath("$.data.messageKey").value("menu.dashboard.title"));

        verify(service).getMessageBundle("admin");
        verify(service).getSupportedCategories();
        verify(service).getMessageValues("menu.dashboard.title");
    }

    @Test
    void createSaveAndRemoveRoutesDelegateDistinctCommands() throws Exception {
        I18nMessageVo saved = message("admin", "menu.home.title", "首页", "Home");
        when(service.createMessage(any(I18nMessageDto.class))).thenReturn(saved);
        when(service.saveMessage(any(I18nMessageDto.class))).thenReturn(saved);

        mvc.perform(post("/sys/i18n-message/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "admin",
                                  "messageKey": "menu.home.title",
                                  "values": [{"locale":"en-US","value":"Home"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageKey").value("menu.home.title"));
        mvc.perform(post("/sys/i18n-message/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "admin",
                                  "messageKey": "menu.home.title",
                                  "values": [{"locale":"en-US","value":"Home"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageKey").value("menu.home.title"));
        mvc.perform(post("/sys/i18n-message/remove/menu.home.title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(service).createMessage(any(I18nMessageDto.class));
        verify(service).saveMessage(any(I18nMessageDto.class));
        verify(service).removeMessage("menu.home.title");
    }

    @Test
    void invalidNestedRequestFieldsReturnLocalizedFieldErrorsWithoutCallingService() throws Exception {
        for (String operation : List.of("create", "save")) {
            mvc.perform(post("/sys/i18n-message/" + operation)
                            .header(ACCEPT_LANGUAGE, "en-US-POSIX")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "category": "",
                                      "messageKey": "",
                                      "values": [{"locale":"","value":"title"}]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10001))
                    .andExpect(jsonPath("$.data[*].field", hasItems(
                            "category", "messageKey", "values[0].locale")))
                    .andExpect(jsonPath("$.data[*].code", hasItems(
                            "NotBlank", "NotBlank", "NotBlank")))
                    .andExpect(jsonPath("$.data[*].message", hasItems(
                            "分类不能为空。", "国际化 key 不能为空。", "语言不能为空。")));
        }

        verifyNoInteractions(service);
    }

    @Test
    void malformedAndBusinessInvalidRequestsUseTheExistingErrorEnvelope() throws Exception {
        mvc.perform(post("/sys/i18n-message/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("请求体格式错误。"));

        when(service.saveMessage(any(I18nMessageDto.class)))
                .thenThrow(new InvalidArgumentException("invalid key"));
        mvc.perform(post("/sys/i18n-message/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"admin\",\"messageKey\":\"menu.title\",\"values\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("invalid key"));

        when(service.createMessage(any(I18nMessageDto.class)))
                .thenThrow(new InvalidArgumentException("existing key"));
        mvc.perform(post("/sys/i18n-message/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"admin\",\"messageKey\":\"menu.title\",\"values\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("existing key"));
    }

    private I18nMessageVo message(String category, String key, String zhCn, String enUs) {
        return new I18nMessageVo(category, key, List.of(
                new I18nMessageValueVo("zh-CN", zhCn),
                new I18nMessageValueVo("en-US", enUs)));
    }
}
