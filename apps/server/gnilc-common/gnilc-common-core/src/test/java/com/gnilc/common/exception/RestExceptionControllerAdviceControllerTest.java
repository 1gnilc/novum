package com.gnilc.common.exception;

import com.gnilc.common.i18n.I18nMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionControllerAdviceControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/common/messages");
        messageSource.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(messageSource, "zh-CN");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new RestExceptionHandlingConfiguration.RestExceptionControllerAdvice(messages))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void commonExceptionsRetainTheirBusinessCodes() throws Exception {
        mvc.perform(get("/test/argument"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("Invalid argument."));

        mvc.perform(get("/test/condition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002))
                .andExpect(jsonPath("$.error").value("The requested operation is not allowed in the current state."));
    }

    @Test
    void authenticationExceptionsRetainTheirTransportAndBusinessCodes() throws Exception {
        mvc.perform(get("/test/authentication"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001))
                .andExpect(jsonPath("$.error").value("Incorrect username or password."));

        mvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20002))
                .andExpect(jsonPath("$.error").value("Unauthorized."));
    }

    @Test
    void malformedRequestsReturnProfessionalEnglishMessages() throws Exception {
        mvc.perform(post("/test/body")
                        .header(ACCEPT_LANGUAGE, "en-US")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("The request body is malformed."));

        mvc.perform(get("/test/number")
                        .header(ACCEPT_LANGUAGE, "en-US")
                        .param("value", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("A request parameter has an invalid format."));
    }

    @Test
    void validationAndUnsupportedMediaTypeUseTheCommonErrorFormat() throws Exception {
        mvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("Name is required."))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].code").value("NotBlank"))
                .andExpect(jsonPath("$.data[0].message").value("Name is required."));

        mvc.perform(post("/test/body")
                        .header(ACCEPT_LANGUAGE, "en-US")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("The request content type is not supported."));
    }

    @Test
    void unexpectedFailuresDoNotExposeImplementationDetails() throws Exception {
        mvc.perform(get("/test/runtime").header(ACCEPT_LANGUAGE, "en-US"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.error").value("An unexpected error occurred."));
    }

    @Test
    void unsupportedRequestLocaleFallsBackToChinese() throws Exception {
        mvc.perform(post("/test/body")
                        .header(ACCEPT_LANGUAGE, "fr-FR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("请求体格式错误。"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/argument")
        void argument() {
            throw new InvalidArgumentException("Invalid argument.");
        }

        @GetMapping("/test/condition")
        void condition() {
            throw new IllegalConditionException("The requested operation is not allowed in the current state.");
        }

        @GetMapping("/test/authentication")
        void authentication() {
            throw new AuthenticationFailedException("Incorrect username or password.");
        }

        @GetMapping("/test/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("Unauthorized.");
        }

        @GetMapping("/test/runtime")
        void runtime() {
            throw new RuntimeException("database password leaked");
        }

        @PostMapping(value = "/test/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        void body(@RequestBody Map<String, Object> body) {
        }

        @GetMapping("/test/number")
        void number(@RequestParam("value") Integer value) {
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody TestRequest request) {
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRequest {
        @NotBlank(message = "Name is required.")
        private String name;
    }
}
