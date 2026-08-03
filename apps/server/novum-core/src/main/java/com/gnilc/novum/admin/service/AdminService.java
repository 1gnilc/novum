package com.gnilc.novum.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.novum.admin.entity.bo.AdminBo;
import com.gnilc.novum.admin.entity.dto.AdminDto;
import com.gnilc.novum.admin.entity.dto.AdminPageDto;
import com.gnilc.novum.admin.entity.dto.AdminRoleDto;
import com.gnilc.novum.admin.entity.vo.AdminTokenVo;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;

import java.util.List;

/**
 * 后台管理员应用服务。
 */
public interface AdminService extends IService<AdminBo> {
    /**
     * 使用用户名和密码登录。
     */
    AdminTokenVo login(String username, String password);

    /**
     * 使用刷新令牌刷新访问令牌。
     */
    AdminTokenVo refresh(String refreshToken);

    /**
     * 登出刷新令牌对应的会话。
     */
    void logout(String refreshToken);

    /**
     * 查询当前管理员资料。
     */
    AdminVo getUserInfo();

    /**
     * 更新当前管理员的基本资料。
     */
    void updateProfile(AdminDto dto);

    /**
     * 更新当前管理员密码并撤销其全部会话。
     */
    void updatePassword(String oldPassword, String newPassword);

    /**
     * 查询当前管理员角色标识。
     */
    List<String> getRoleCodes();

    /**
     * 查询当前管理员按钮访问标识。
     */
    List<String> getMenuAccessCodes();

    /**
     * 查询当前管理员导航路由树。
     */
    List<MenuRouteVo> getMenuRoutes();

    /**
     * 根据用户名查询管理员。
     */
    AdminBo getAdminByUsername(String username);

    /**
     * 查询用户角色标识。
     */
    List<String> getRoleCodes(Long userId);

    /**
     * 查询用户按钮访问标识。
     */
    List<String> getMenuAccessCodes(Long userId);

    /**
     * 分页查询管理员。
     */
    PageResult<AdminVo> getAdminPage(AdminPageDto dto);

    /**
     * 创建管理员。
     */
    void createAdmin(AdminDto dto);

    /**
     * 更新管理员资料。
     */
    void updateAdmin(AdminDto dto);

    /**
     * 保存管理员角色。
     */
    void saveAdminRoles(AdminRoleDto dto);

    /**
     * 删除管理员。
     */
    void removeAdmin(Long id);

    /**
     * 根据 user_id 查询管理员。
     */
    AdminBo getAdminByUserId(Long userId);

    /**
     * 根据管理员 ID 查询管理员。
     */
    AdminBo getAdmin(Long id);
}
