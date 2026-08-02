package com.gnilc.novum.customer.controller;

import com.alibaba.fastjson2.JSONObject;
import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.R;
import com.gnilc.novum.customer.entity.vo.CustomerTokenVo;
import com.gnilc.novum.customer.entity.vo.CustomerVo;
import com.gnilc.novum.customer.service.CustomerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer 会话与自助 API。
 */
@RestController
@RequestMapping("/customer")
public class CustomerController {
    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";

    private final CustomerService customerService;
    private final I18nMessageService messages;

    public CustomerController(CustomerService customerService, I18nMessageService messages) {
        this.customerService = customerService;
        this.messages = messages;
    }

    @PostMapping("/login")
    public R<CustomerTokenVo> login(@RequestBody(required = false) JSONObject body) {
        String username = body == null ? null : body.getString("username");
        String password = body == null ? null : body.getString("password");
        CustomerTokenVo token = customerService.login(username, password);
        return token == null
                ? R.error(ResponseCode.AUTHENTICATION_FAILED,
                messages.get("system.customer.login.invalidCredentials"))
                : R.success(token);
    }

    @PostMapping("/refresh")
    public ResponseEntity<R<?>> refresh(
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return sessionExpired();
        }
        CustomerTokenVo token = customerService.refresh(refreshToken);
        return token == null ? sessionExpired() : ResponseEntity.ok(R.success(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<R<?>> logout(
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        if (StringUtils.isBlank(refreshToken) || !customerService.logout(refreshToken)) {
            return unauthorized();
        }
        return ResponseEntity.ok(R.success());
    }

    @GetMapping("/user-info")
    public R<CustomerVo> getUserInfo() {
        return R.success(customerService.getUserInfo());
    }

    private ResponseEntity<R<?>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.error(ResponseCode.UNAUTHORIZED, messages.get("system.auth.unauthorized")));
    }

    private ResponseEntity<R<?>> sessionExpired() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.error(ResponseCode.UNAUTHORIZED, messages.get("system.auth.session.expired")));
    }
}
