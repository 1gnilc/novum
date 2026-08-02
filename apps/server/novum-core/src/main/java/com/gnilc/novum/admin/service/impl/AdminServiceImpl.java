package com.gnilc.novum.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.utils.BeanPropertyUtils;
import com.gnilc.common.utils.PageResult;
import com.gnilc.novum.admin.cache.AdminCacheService;
import com.gnilc.auth.authz.rbac.entity.bo.MenuBo;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.entity.enums.MenuType;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.novum.admin.dao.AdminDao;
import com.gnilc.novum.session.AdminSessionManager;
import com.gnilc.novum.session.SessionTokenPair;
import com.gnilc.novum.admin.entity.bo.AdminBo;
import com.gnilc.novum.admin.entity.dto.AdminDto;
import com.gnilc.novum.admin.entity.dto.AdminPageDto;
import com.gnilc.novum.admin.entity.dto.AdminRoleDto;
import com.gnilc.novum.admin.entity.vo.AdminTokenVo;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.novum.admin.event.AdminEvent;
import com.gnilc.novum.admin.service.AdminService;
import com.gnilc.novum.auth.AccessPrincipalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * 编排后台管理员资料、会话和 RBAC 角色。
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminDao, AdminBo> implements AdminService {
    private static final String ADMIN_DEFAULT_ROLE_CODE = "admin";
    private static final String DEFAULT_HOME_PATH = "/dashboard";
    private static final int USERNAME_MAX_LENGTH = 255;
    private static final int NICKNAME_MAX_LENGTH = 255;
    private static final int PROFILE_TEXT_MAX_LENGTH = 500;
    private static final int HOME_PATH_MAX_LENGTH = 500;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final AdminSessionManager sessionManager;
    private final AdminCacheService cacheService;
    private final RoleService roleService;
    private final MenuService menuService;
    private final RoleMenuService roleMenuService;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final ApplicationEventPublisher eventPublisher;
    private final I18nMessageService messages;

    public AdminServiceImpl(AdminSessionManager sessionManager,
                            AdminCacheService cacheService,
                            RoleService roleService,
                            MenuService menuService,
                            RoleMenuService roleMenuService,
                            UserService userService,
                            UserRoleService userRoleService,
                            ApplicationEventPublisher eventPublisher,
                            I18nMessageService messages) {
        this.sessionManager = sessionManager;
        this.cacheService = cacheService;
        this.roleService = roleService;
        this.menuService = menuService;
        this.roleMenuService = roleMenuService;
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    /**
     * 登录管理员并创建令牌。
     */
    @Override
    public AdminTokenVo login(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }
        AdminBo bo = getAdminByUsername(username);
        if (bo == null || Boolean.FALSE.equals(bo.getStatus())) {
            return null;
        }
        if (!PASSWORD_ENCODER.matches(password, bo.getPassword())) {
            return null;
        }
        SessionTokenPair pair = sessionManager.createSession(bo.getUserId());
        return AdminTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    /**
     * 刷新访问令牌。
     */
    @Override
    public AdminTokenVo refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return null;
        }
        SessionTokenPair pair = sessionManager.refreshSession(refreshToken);
        if (pair == null) {
            return null;
        }
        return AdminTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    /**
     * 登出当前会话。
     */
    @Override
    public boolean logout(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return false;
        }
        return sessionManager.logout(refreshToken);
    }

    /**
     * 查询当前管理员资料。
     */
    @Override
    public AdminVo getUserInfo() {
        Long userId = AccessPrincipalUtils.getUserId();
        AdminVo vo = cacheService.getUserInfo(userId, () -> {
            AdminBo bo = getAdminByUserId(userId);
            if (bo == null) {
                return null;
            }
            AdminVo userInfo = new AdminVo();
            BeanUtils.copyProperties(bo, userInfo);
            userInfo.setDesc(bo.getDescription());
            userInfo.setStatus(null);
            return userInfo;
        });
        if (vo == null) {
            return null;
        }
        vo.setRoleCodes(getRoleCodes(userId));
        return vo;
    }

    @Override
    @Transactional
    public void updateProfile(AdminDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("system.admin.profile.required"));
        String nickname = StringUtils.trimToNull(dto.getNickname());
        Preconditions.checkArgument(nickname != null, messages.get("system.admin.nickname.required"));
        Preconditions.checkArgument(nickname.codePointCount(0, nickname.length()) <= NICKNAME_MAX_LENGTH,
                messages.get("system.admin.nickname.tooLong", NICKNAME_MAX_LENGTH));
        String avatar = StringUtils.trimToNull(dto.getAvatar());
        Preconditions.checkArgument(avatar == null
                        || avatar.codePointCount(0, avatar.length()) <= PROFILE_TEXT_MAX_LENGTH,
                messages.get("system.admin.avatar.tooLong", PROFILE_TEXT_MAX_LENGTH));
        String description = StringUtils.trimToNull(dto.getDesc());
        Preconditions.checkArgument(description == null
                        || description.codePointCount(0, description.length()) <= PROFILE_TEXT_MAX_LENGTH,
                messages.get("system.admin.description.tooLong", PROFILE_TEXT_MAX_LENGTH));

        AdminBo bo = getAdminByUserId(AccessPrincipalUtils.getUserId());
        Preconditions.checkCondition(bo != null,
                messages.get("system.admin.notFound.signIn"));
        lambdaUpdate()
                .set(AdminBo::getNickname, nickname)
                .set(AdminBo::getAvatar, avatar)
                .set(AdminBo::getDescription, description)
                .eq(AdminBo::getId, bo.getId())
                .update();
        eventPublisher.publishEvent(new AdminEvent(AdminEvent.Action.UPDATE, bo.getUserId()));
    }

    @Override
    @Transactional
    public void updatePassword(String oldPassword, String newPassword) {
        Preconditions.checkArgument(StringUtils.isNotBlank(oldPassword),
                messages.get("system.admin.password.current.required"));
        validateStrongPassword(newPassword);

        Long userId = AccessPrincipalUtils.getUserId();
        AdminBo bo = getAdminByUserId(userId);
        Preconditions.checkCondition(bo != null,
                messages.get("system.admin.notFound.signIn"));
        Preconditions.checkArgument(PASSWORD_ENCODER.matches(oldPassword, bo.getPassword()),
                messages.get("system.admin.password.current.incorrect"));

        lambdaUpdate()
                .set(AdminBo::getPassword, PASSWORD_ENCODER.encode(newPassword))
                .eq(AdminBo::getId, bo.getId())
                .update();
        sessionManager.cleanupUserSessions(userId);
    }

    /**
     * 查询当前管理员角色标识。
     */
    @Override
    public List<String> getRoleCodes() {
        return getRoleCodes(AccessPrincipalUtils.getUserId());
    }

    /**
     * 查询当前管理员按钮访问标识。
     */
    @Override
    public List<String> getMenuAccessCodes() {
        return getMenuAccessCodes(AccessPrincipalUtils.getUserId());
    }

    /**
     * 根据用户名查询管理员。
     */
    @Override
    public AdminBo getAdminByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        return lambdaQuery().eq(AdminBo::getUsername, username).one();
    }

    /**
     * 查询用户角色标识。
     */
    @Override
    public List<String> getRoleCodes(Long userId) {
        return cacheService.getRoleCodes(userId,
                () -> Optional.ofNullable(roleService.getRoles(userId)).orElse(List.of()).stream()
                        .map(RoleBo::getCode)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .toList());
    }

    /**
     * 查询用户按钮访问标识。
     */
    @Override
    public List<String> getMenuAccessCodes(Long userId) {
        return cacheService.getMenuAccessCodes(userId,
                () -> Optional.ofNullable(userService.getMenus(userId)).orElse(List.of()).stream()
                        .filter(menu -> menu.getType() == MenuType.BUTTON)
                        .filter(MenuBo::getStatus)
                        .map(MenuBo::getAccessCode)
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .distinct()
                        .toList());
    }

    @Override
    public List<MenuRouteVo> getMenuRoutes() {
        Long userId = AccessPrincipalUtils.getUserId();
        return cacheService.getMenuRoutes(userId, () -> {
            List<Long> roleIds = userRoleService.getRoleIds(userId);
            List<Long> menuIds = roleMenuService.getMenuIds(roleIds);
            return menuService.getMenuRoutes(menuIds);
        });
    }

    /**
     * 创建管理员。
     */
    @Override
    @Transactional
    public void createAdmin(AdminDto dto) {
        validateAdmin(dto, false);
        String username = dto.getUsername();
        String password = dto.getPassword();
        Long userId = userService.createUser();
        AdminBo bo = new AdminBo();
        bo.setUserId(userId);
        bo.setUsername(username);
        bo.setPassword(PASSWORD_ENCODER.encode(password));
        bo.setNickname(dto.getNickname());
        bo.setAvatar(dto.getAvatar());
        bo.setDescription(dto.getDesc());
        bo.setHomePath(StringUtils.defaultIfBlank(dto.getHomePath(), DEFAULT_HOME_PATH));
        bo.setStatus(dto.getStatus());
        save(bo);
        replaceAdminRoles(userId, dto.getRoleCodes());
    }

    /**
     * 更新管理员资料。
     */
    @Override
    @Transactional
    public void updateAdmin(AdminDto dto) {
        AdminBo bo = validateAdmin(dto, true);
        boolean wasEnabled = Boolean.TRUE.equals(bo.getStatus())
                && Boolean.FALSE.equals(dto.getStatus());
        BeanPropertyUtils.copyNonNullProperties(dto, bo);
        if (dto.isAvatarSpecified()) {
            bo.setAvatar(dto.getAvatar());
        }
        if (dto.isDescSpecified()) {
            bo.setDescription(dto.getDesc());
        }

        // 单独处理密码。
        String password = dto.getPassword();
        if (StringUtils.isNotBlank(password)) {
            bo.setPassword(PASSWORD_ENCODER.encode(password));
        } else {
            bo.setPassword(null);
        }

        updateById(bo);
        saveRolesIfProvided(bo.getUserId(), dto.getRoleCodes());
        eventPublisher.publishEvent(new AdminEvent(AdminEvent.Action.UPDATE, bo.getUserId()));

        if (wasEnabled) {
            sessionManager.cleanupUserSessions(bo.getUserId());
        }
    }

    /**
     * 保存管理员角色。
     */
    @Override
    @Transactional
    public void saveAdminRoles(AdminRoleDto dto) {
        AdminBo bo = getById(dto.getId());
        Preconditions.checkCondition(bo != null, messages.get("system.admin.notFound"));
        replaceAdminRoles(bo.getUserId(), dto.getRoleCodes());
    }

    /**
     * 删除管理员。
     */
    @Override
    @Transactional
    public void removeAdmin(Long id) {
        Preconditions.checkArgument(id != null, messages.get("system.admin.selection.required"));
        AdminBo bo = getById(id);
        Preconditions.checkCondition(bo != null, messages.get("system.admin.notFound"));
        Preconditions.checkCondition(!Objects.equals(bo.getUserId(), AccessPrincipalUtils.getUserId()),
                messages.get("system.admin.current.delete"));
        sessionManager.cleanupUserSessions(bo.getUserId());
        bo.setUsername(bo.getUsername() + "_del_" + id);
        updateById(bo);
        removeById(id);
        userService.removeUser(bo.getUserId());
        replaceRoles(bo.getUserId(), List.of());
        eventPublisher.publishEvent(new AdminEvent(AdminEvent.Action.DELETE, bo.getUserId()));
    }

    /**
     * 分页查询管理员。
     */
    @Override
    public PageResult<AdminVo> getAdminPage(AdminPageDto params) {
        IPage<AdminBo> page = lambdaQuery()
                .eq(StringUtils.isNotBlank(params.getUsername()), AdminBo::getUsername, params.getUsername())
                .like(StringUtils.isNotBlank(params.getNickname()), AdminBo::getNickname, params.getNickname())
                .eq(params.getStatus() != null, AdminBo::getStatus, params.getStatus())
                .orderByDesc(AdminBo::getId)
                .page(params.getPage());
        List<AdminVo> vos = page.getRecords().stream()
                .map(bo -> {
                    AdminVo vo = new AdminVo();
                    BeanUtils.copyProperties(bo, vo);
                    vo.setDesc(bo.getDescription());
                    vo.setRoleCodes(getRoleCodes(bo.getUserId()));
                    return vo;
                })
                .toList();
        return PageResult.of(page, vos);
    }

    @Override
    public AdminBo getAdminByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(AdminBo::getUserId, userId)
                .one();
    }

    @Override
    public AdminBo getAdmin(Long id) {
        if (id == null) {
            return null;
        }
        return getById(id);
    }


    /**
     * 校验管理员密码强度。
     */
    private void validateStrongPassword(String password) {
        boolean valid = password != null
                && password.length() >= 8
                && password.length() <= 32
                && password.chars().noneMatch(Character::isWhitespace)
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        Preconditions.checkArgument(valid,
                messages.get("system.admin.password.weak"));
    }

    private AdminBo validateAdmin(AdminDto dto, boolean update) {
        Preconditions.checkArgument(dto != null, messages.get("system.admin.information.required"));
        boolean usernameSpecified = dto.getUsername() != null;
        boolean nicknameSpecified = dto.getNickname() != null;
        BeanPropertyUtils.trimToNull(dto, "password");
        AdminBo admin = update ? getAdmin(dto.getId()) : null;
        if (update) {
            Preconditions.checkCondition(admin != null, messages.get("system.admin.notFound"));
            Preconditions.checkArgument(!usernameSpecified || StringUtils.isNotBlank(dto.getUsername()),
                    messages.get("system.admin.username.required"));
            Preconditions.checkArgument(!nicknameSpecified || StringUtils.isNotBlank(dto.getNickname()),
                    messages.get("system.admin.nickname.required"));
        } else {
            Preconditions.checkArgument(StringUtils.isNotBlank(dto.getUsername()),
                    messages.get("system.admin.username.required"));
            Preconditions.checkArgument(StringUtils.isNotBlank(dto.getPassword()),
                    messages.get("system.admin.password.required"));
            Preconditions.checkArgument(StringUtils.isNotBlank(dto.getNickname()),
                    messages.get("system.admin.nickname.required"));
        }
        Preconditions.checkArgument(dto.getUsername() == null
                        || dto.getUsername().codePointCount(0, dto.getUsername().length()) <= USERNAME_MAX_LENGTH,
                messages.get("system.admin.username.tooLong", USERNAME_MAX_LENGTH));
        Preconditions.checkArgument(dto.getNickname() == null
                        || dto.getNickname().codePointCount(0, dto.getNickname().length()) <= NICKNAME_MAX_LENGTH,
                messages.get("system.admin.nickname.tooLong", NICKNAME_MAX_LENGTH));
        Preconditions.checkArgument(dto.getAvatar() == null
                        || dto.getAvatar().codePointCount(0, dto.getAvatar().length()) <= PROFILE_TEXT_MAX_LENGTH,
                messages.get("system.admin.avatar.tooLong", PROFILE_TEXT_MAX_LENGTH));
        Preconditions.checkArgument(dto.getDesc() == null
                        || dto.getDesc().codePointCount(0, dto.getDesc().length()) <= PROFILE_TEXT_MAX_LENGTH,
                messages.get("system.admin.description.tooLong", PROFILE_TEXT_MAX_LENGTH));
        Preconditions.checkArgument(dto.getHomePath() == null
                        || dto.getHomePath().codePointCount(0, dto.getHomePath().length()) <= HOME_PATH_MAX_LENGTH,
                messages.get("system.admin.homePath.tooLong", HOME_PATH_MAX_LENGTH));
        String username = dto.getUsername();
        if (!update || username != null && !username.equals(admin.getUsername())) {
            Preconditions.checkArgument(getAdminByUsername(username) == null,
                    messages.get("system.admin.username.exists"));
        }
        if (update) {
            boolean disablesCurrentAdmin = Boolean.TRUE.equals(admin.getStatus())
                    && Boolean.FALSE.equals(dto.getStatus())
                    && Objects.equals(admin.getUserId(), AccessPrincipalUtils.getUserId());
            Preconditions.checkCondition(!disablesCurrentAdmin, messages.get("system.admin.current.disable"));
        }
        if (!update || StringUtils.isNotBlank(dto.getPassword())) {
            validateStrongPassword(dto.getPassword());
        }
        return admin;
    }

    /**
     * 替换用户角色。
     */
    private void saveRolesIfProvided(Long userId, List<String> roleCodes) {
        if (roleCodes == null) {
            return;
        }
        replaceAdminRoles(userId, roleCodes);
    }

    private void replaceAdminRoles(Long userId, List<String> roleCodes) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        codes.add(ADMIN_DEFAULT_ROLE_CODE);
        if (roleCodes != null) {
            codes.addAll(roleCodes);
        }
        replaceRoles(userId, codes.stream().toList());
    }

    private void replaceRoles(Long userId, List<String> roleCodes) {
        List<String> codes = roleCodes == null ? List.of() : roleCodes;
        List<Long> roleIds = codes.stream()
                .map(code -> {
                    Preconditions.checkArgument(StringUtils.isNotBlank(code), messages.get("rbac.role.code.required"));
                    RoleBo bo = roleService.getRoleByCode(code);
                    Preconditions.checkCondition(bo != null,
                            messages.get("rbac.role.notFound"));
                    return bo.getId();
                })
                .toList();
        UserRoleDto dto = new UserRoleDto();
        dto.setUserId(userId);
        dto.setRoleIds(roleIds);
        userRoleService.updateUserRole(dto);
    }

}
