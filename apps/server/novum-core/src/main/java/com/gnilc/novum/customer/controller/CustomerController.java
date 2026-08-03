package com.gnilc.novum.customer.controller;

import com.alibaba.fastjson2.JSONObject;
import com.gnilc.common.utils.R;
import com.gnilc.novum.customer.entity.vo.CustomerTokenVo;
import com.gnilc.novum.customer.entity.vo.CustomerVo;
import com.gnilc.novum.customer.service.CustomerService;
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

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/login")
    public R<CustomerTokenVo> login(@RequestBody JSONObject body) {
        String username = body.getString("username");
        String password = body.getString("password");
        return R.success(customerService.login(username, password));
    }

    @PostMapping("/refresh")
    public R<CustomerTokenVo> refresh(
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        return R.success(customerService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public R<?> logout(
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        customerService.logout(refreshToken);
        return R.success();
    }

    @GetMapping("/user-info")
    public R<CustomerVo> getUserInfo() {
        return R.success(customerService.getUserInfo());
    }

}
