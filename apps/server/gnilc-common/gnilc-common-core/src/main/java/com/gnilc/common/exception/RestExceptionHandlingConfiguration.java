package com.gnilc.common.exception;

import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.i18n.SupportedLocale;
import com.gnilc.common.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;

/**
 * Opt-in configuration for the common REST exception handling policy.
 *
 * <p>Applications control activation by explicitly importing this configuration.</p>
 */
@Import(I18nMessageService.class)
public class RestExceptionHandlingConfiguration {

    @Bean
    public LocaleResolver localeResolver(
            @Value("${app.i18n.default-locale:en-US}") String defaultLocale) {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SupportedLocale.locales());
        resolver.setDefaultLocale(SupportedLocale.fromLanguageTagOrDefault(defaultLocale));
        return resolver;
    }

    @Bean
    RestExceptionControllerAdvice restExceptionControllerAdvice(I18nMessageService i18nMessageService) {
        return new RestExceptionControllerAdvice(i18nMessageService);
    }

    @RestControllerAdvice
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Conditional(ExplicitImportOnlyCondition.class)
    public static final class RestExceptionControllerAdvice {

        private final Logger log = LoggerFactory.getLogger(RestExceptionControllerAdvice.class);
        private final I18nMessageService i18nMessageService;

        public RestExceptionControllerAdvice(I18nMessageService i18nMessageService) {
            this.i18nMessageService = i18nMessageService;
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public R<?> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
            BindingResult bindingResult = exception.getBindingResult();
            List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                    .map(error -> new FieldError(
                            error.getField(),
                            error.getCode(),
                            error.getDefaultMessage()))
                    .toList();
            String message = fieldErrors.stream()
                    .map(FieldError::getMessage)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(i18nMessageService.get("validation.argument.invalid"));
            log.warn("Request validation failed: {}", message);
            return R.error(ResponseCode.ARGUMENT_INVALID.getCode(), message, fieldErrors);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<R<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
            log.warn("Request body could not be read: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID,
                            i18nMessageService.get("validation.body.malformed")));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<R<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
            log.warn("Request parameter has an invalid format: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID,
                            i18nMessageService.get("validation.parameter.format.invalid")));
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<R<?>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
            log.warn("Request content type is not supported: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID,
                            i18nMessageService.get("validation.media.type.unsupported")));
        }

        @ExceptionHandler(InvalidArgumentException.class)
        public R<?> handleInvalidArgument(InvalidArgumentException exception) {
            log.warn("Invalid argument: {}", exception.getMessage());
            return R.error(ResponseCode.ARGUMENT_INVALID, exception.getMessage());
        }

        @ExceptionHandler(IllegalConditionException.class)
        public R<?> handleIllegalCondition(IllegalConditionException exception) {
            log.warn("Illegal condition: {}", exception.getMessage());
            return R.error(ResponseCode.ILLEGAL_CONDITION, exception.getMessage());
        }

        @ExceptionHandler(AuthenticationFailedException.class)
        public R<?> handleAuthenticationFailed(AuthenticationFailedException exception) {
            log.warn("Authentication failed: {}", exception.getMessage());
            return R.error(ResponseCode.AUTHENTICATION_FAILED, exception.getMessage());
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<R<?>> handleUnauthorized(UnauthorizedException exception) {
            log.warn("Unauthorized request: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(R.error(ResponseCode.UNAUTHORIZED, exception.getMessage()));
        }

        @ExceptionHandler(UnknownErrorException.class)
        public ResponseEntity<R<?>> handleUnknownError(UnknownErrorException exception) {
            log.error("Application error", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(ResponseCode.ERROR, exception.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<R<?>> handleUnexpectedException(Exception exception) {
            log.error("Unhandled exception", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(ResponseCode.ERROR,
                            i18nMessageService.get("common.unexpected.error")));
        }
    }

    /**
     * Prevents component scanning from activating the advice. The configuration's
     * {@link Bean} method remains the only registration path.
     */
    static final class ExplicitImportOnlyCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return false;
        }
    }
}
