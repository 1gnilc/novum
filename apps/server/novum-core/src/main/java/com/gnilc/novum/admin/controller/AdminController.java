package com.gnilc.novum.admin.controller;

import com.alibaba.fastjson2.JSONObject;
import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.R;
import com.gnilc.novum.admin.entity.dto.AdminDto;
import com.gnilc.novum.admin.entity.dto.AdminPageDto;
import com.gnilc.novum.admin.entity.dto.AdminRoleDto;
import com.gnilc.novum.admin.entity.vo.AdminTokenVo;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.novum.admin.service.AdminService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台管理员 API。
 */
@RestController
@RequestMapping("/sys/admin")
public class AdminController {
    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private final AdminService adminService;
    private final I18nMessageService messages;

    public AdminController(AdminService adminService, I18nMessageService messages) {
        this.adminService = adminService;
        this.messages = messages;
    }

    /**
     * 分页查询管理员。
     */
    @PostMapping("/page")
    public R<PageResult<AdminVo>> getAdminPage(@RequestBody(required = false) AdminPageDto dto) {
        return R.success(adminService.getAdminPage(dto));
    }

    /**
     * 创建管理员。
     */
    @PostMapping("/create")
    public R<?> createAdmin(@RequestBody AdminDto dto) {
        adminService.createAdmin(dto);
        return R.success();
    }

    /**
     * 更新管理员资料。
     */
    @PostMapping("/update")
    public R<?> updateAdmin(@RequestBody AdminDto dto) {
        adminService.updateAdmin(dto);
        return R.success();
    }

    /**
     * 保存管理员角色。
     */
    @PostMapping("/roles/save")
    public R<?> saveAdminRoles(@RequestBody AdminRoleDto dto) {
        adminService.saveAdminRoles(dto);
        return R.success();
    }

    /**
     * 删除管理员。
     */
    @PostMapping("/remove/{id}")
    public R<?> removeAdmin(@PathVariable("id") Long id) {
        adminService.removeAdmin(id);
        return R.success();
    }

    /**
     * 登录管理员。
     */
    @PostMapping("/login")
    public R<AdminTokenVo> login(@RequestBody(required = false) JSONObject body) {
        String username = body == null ? null : body.getString("username");
        String password = body == null ? null : body.getString("password");
        AdminTokenVo vo = adminService.login(username, password);
        if (vo == null) {
            return R.error(ResponseCode.AUTHENTICATION_FAILED,
                    messages.get("system.admin.login.invalidCredentials"));
        }
        return R.success(vo);
    }

    /**
     * 刷新访问令牌。
     */
    @PostMapping("/refresh")
    public ResponseEntity<R<?>> refresh(
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return sessionExpired();
        }
        AdminTokenVo vo = adminService.refresh(refreshToken);
        if (vo == null) {
            return sessionExpired();
        }
        return ResponseEntity.ok(R.success(vo));
    }

    /**
     * 登出当前会话。
     */
    @PostMapping("/logout")
    public ResponseEntity<R<?>> logout(
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return unauthorized();
        }
        if (!adminService.logout(refreshToken)) {
            return unauthorized();
        }
        return ResponseEntity.ok(R.success());
    }

    /**
     * 查询当前管理员资料。
     */
    @GetMapping("/user-info")
    public R<AdminVo> getAdminUserInfo() {
        return R.success(adminService.getUserInfo());
    }

    /**
     * 更新当前管理员资料。
     */
    @PostMapping("/user-info/update")
    public R<?> updateProfile(@RequestBody AdminDto dto) {
        adminService.updateProfile(dto);
        return R.success();
    }

    /**
     * 更新当前管理员密码。
     */
    @PostMapping("/password/update")
    public R<?> updatePassword(@RequestBody(required = false) JSONObject body) {
        String oldPassword = body == null ? null : body.getString("oldPassword");
        String newPassword = body == null ? null : body.getString("newPassword");
        adminService.updatePassword(oldPassword, newPassword);
        return R.success();
    }

    /**
     * 查询当前管理员角色标识。
     */
    @GetMapping("/role-codes")
    public R<List<String>> getRoleCodes() {
        return R.success(adminService.getRoleCodes());
    }

    /**
     * 查询当前管理员按钮访问标识。
     */
    @GetMapping("/menu/access-codes")
    public R<List<String>> getMenuAccessCodes() {
        return R.success(adminService.getMenuAccessCodes());
    }

    /**
     * 查询当前管理员导航路由树。
     */
    @GetMapping("/menu/routes")
    public R<List<MenuRouteVo>> getMenuRoutes() {
        return R.success(adminService.getMenuRoutes());
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
