-- S3 image lifecycle and administration resources.
-- Depends on scripts 01 through 09.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_image (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    del tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time datetime(6) NOT NULL COMMENT '创建时间（UTC）',
    update_time datetime(6) DEFAULT NULL COMMENT '更新时间（UTC）',
    object_key varchar(500) NOT NULL COMMENT 'S3 对象键',
    content_type varchar(100) NOT NULL COMMENT 'MIME 类型',
    content_length bigint NOT NULL COMMENT '文件字节数',
    status varchar(16) NOT NULL COMMENT 'PENDING 或 READY',
    expires_at datetime(6) DEFAULT NULL COMMENT '待完成上传过期时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_object_key (object_key),
    KEY idx_status_create_time (status, create_time),
    KEY idx_pending_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='托管图片';

ALTER TABLE sys_admin
    MODIFY COLUMN avatar varchar(500) DEFAULT NULL COMMENT '头像 S3 对象键';
ALTER TABLE nv_customer
    MODIFY COLUMN avatar varchar(500) DEFAULT NULL COMMENT '头像 S3 对象键';
UPDATE sys_admin SET avatar = NULL WHERE avatar LIKE 'http://%' OR avatar LIKE 'https://%';
UPDATE nv_customer SET avatar = NULL WHERE avatar LIKE 'http://%' OR avatar LIKE 'https://%';

UPDATE az_role
SET del = 0,
    name = '图片管理员',
    remark = '管理托管图片',
    built_in = 1,
    update_time = UTC_TIMESTAMP(6)
WHERE code = 'image:manager';

INSERT INTO az_role (del, create_time, update_time, code, name, remark, built_in)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'image:manager', '图片管理员', '管理托管图片', 1
WHERE NOT EXISTS (SELECT 1 FROM az_role WHERE code = 'image:manager');

DROP TEMPORARY TABLE IF EXISTS image_permission_seed;
CREATE TEMPORARY TABLE image_permission_seed (
    code varchar(255) NOT NULL,
    target_identifier varchar(500) NOT NULL,
    target_qualifier varchar(255) NOT NULL,
    PRIMARY KEY (code)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO image_permission_seed (code, target_identifier, target_qualifier)
VALUES
    ('POST:/image/presign', '/image/presign', 'POST'),
    ('POST:/image/finalize', '/image/finalize', 'POST'),
    ('POST:/image/page', '/image/page', 'POST'),
    ('POST:/image/remove/{id}', '/image/remove/{id}', 'POST');

INSERT INTO az_permission (
    del, create_time, update_time, code, name, target_identifier,
    target_qualifier, public_access, built_in
)
SELECT 0, UTC_TIMESTAMP(6), NULL, seed.code, seed.code,
       seed.target_identifier, seed.target_qualifier, 0, 1
FROM image_permission_seed seed
WHERE NOT EXISTS (SELECT 1 FROM az_permission current_permission WHERE current_permission.code = seed.code);

UPDATE az_permission permission
JOIN image_permission_seed seed ON seed.code = permission.code
SET permission.del = 0,
    permission.name = seed.code,
    permission.target_identifier = seed.target_identifier,
    permission.target_qualifier = seed.target_qualifier,
    permission.public_access = 0,
    permission.built_in = 1,
    permission.update_time = UTC_TIMESTAMP(6);

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, role.id, permission.id
FROM az_role role
JOIN az_permission permission
  ON permission.code IN ('POST:/image/presign', 'POST:/image/finalize')
 AND permission.del = 0
WHERE role.code IN ('admin', 'customer')
  AND role.del = 0
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission binding
      WHERE binding.role_id = role.id
        AND binding.permission_id = permission.id
        AND binding.del = 0
  );

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, role.id, permission.id
FROM az_role role
JOIN az_permission permission
  ON permission.code IN ('POST:/image/page', 'POST:/image/remove/{id}')
 AND permission.del = 0
WHERE role.code = 'image:manager'
  AND role.del = 0
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission binding
      WHERE binding.role_id = role.id
        AND binding.permission_id = permission.id
        AND binding.del = 0
  );

DROP TEMPORARY TABLE IF EXISTS image_menu_seed;
CREATE TEMPORARY TABLE image_menu_seed (
    name varchar(255) NOT NULL,
    parent_name varchar(255) NOT NULL,
    type varchar(16) NOT NULL,
    access_code varchar(255) DEFAULT NULL,
    path varchar(500) DEFAULT NULL,
    component varchar(255) DEFAULT NULL,
    icon varchar(255) DEFAULT NULL,
    menu_order int NOT NULL,
    title varchar(255) NOT NULL,
    PRIMARY KEY (name)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO image_menu_seed
    (name, parent_name, type, access_code, path, component, icon, menu_order, title)
VALUES
    ('Image', 'System', 'menu', NULL, '/system/image', '/system/image/index', 'lucide:image', 60,
     'menu.system.image.title'),
    ('ImageRemove', 'Image', 'button', 'system:image:remove', NULL, NULL, NULL, 1,
     'menu.system.image.remove');

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, access_code, name, path,
    component, affix_tab, hide_in_menu, icon, `order`, title, built_in
)
SELECT 0, UTC_TIMESTAMP(6), NULL, parent.id, seed.type, 1, seed.access_code,
       seed.name, seed.path, seed.component, 0, 0, seed.icon, seed.menu_order, seed.title, 1
FROM image_menu_seed seed
JOIN az_menu parent ON parent.name = seed.parent_name AND parent.del = 0
WHERE seed.name = 'Image'
  AND NOT EXISTS (SELECT 1 FROM az_menu current_menu WHERE current_menu.name = seed.name);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, access_code, name, path,
    component, affix_tab, hide_in_menu, icon, `order`, title, built_in
)
SELECT 0, UTC_TIMESTAMP(6), NULL, parent.id, seed.type, 1, seed.access_code,
       seed.name, seed.path, seed.component, 0, 0, seed.icon, seed.menu_order, seed.title, 1
FROM image_menu_seed seed
JOIN az_menu parent ON parent.name = seed.parent_name AND parent.del = 0
WHERE seed.name = 'ImageRemove'
  AND NOT EXISTS (SELECT 1 FROM az_menu current_menu WHERE current_menu.name = seed.name);

UPDATE az_menu current_menu
JOIN image_menu_seed seed ON seed.name = current_menu.name
JOIN az_menu parent ON parent.name = seed.parent_name AND parent.del = 0
SET current_menu.del = 0,
    current_menu.pid = parent.id,
    current_menu.type = seed.type,
    current_menu.status = 1,
    current_menu.access_code = seed.access_code,
    current_menu.path = seed.path,
    current_menu.component = seed.component,
    current_menu.icon = seed.icon,
    current_menu.`order` = seed.menu_order,
    current_menu.title = seed.title,
    current_menu.built_in = 1,
    current_menu.update_time = UTC_TIMESTAMP(6);

SET @image_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'image:manager' AND del = 0 LIMIT 1
);
SET @default_admin_user_id := (
    SELECT user_id FROM sys_admin WHERE username = 'admin' AND del = 0 LIMIT 1
);

INSERT INTO az_role_menu (del, create_time, update_time, role_id, menu_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @image_manager_role_id, menu.id
FROM az_menu menu
WHERE menu.del = 0
  AND menu.name IN ('System', 'Image', 'ImageRemove')
  AND @image_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_role_menu binding
      WHERE binding.role_id = @image_manager_role_id
        AND binding.menu_id = menu.id
        AND binding.del = 0
  );

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @default_admin_user_id, @image_manager_role_id
WHERE @default_admin_user_id IS NOT NULL
  AND @image_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_user_role binding
      WHERE binding.user_id = @default_admin_user_id
        AND binding.role_id = @image_manager_role_id
        AND binding.del = 0
  );

DROP TEMPORARY TABLE IF EXISTS image_i18n_seed;
CREATE TEMPORARY TABLE image_i18n_seed (
    message_key varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    locale varchar(20) NOT NULL,
    i18n_value text NOT NULL,
    PRIMARY KEY (message_key, locale)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO image_i18n_seed (message_key, locale, i18n_value)
VALUES
    ('menu.system.image.title', 'en-US', 'Images'),
    ('menu.system.image.title', 'zh-CN', '图片管理'),
    ('menu.system.image.remove', 'en-US', 'Delete image'),
    ('menu.system.image.remove', 'zh-CN', '删除图片');

INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT 'admin', seed.message_key, seed.locale, seed.i18n_value, UTC_TIMESTAMP(6)
FROM image_i18n_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n current_message
    WHERE current_message.message_key = seed.message_key
      AND current_message.locale COLLATE utf8mb4_unicode_ci = seed.locale
);

DROP TEMPORARY TABLE IF EXISTS image_i18n_seed;
DROP TEMPORARY TABLE IF EXISTS image_menu_seed;
DROP TEMPORARY TABLE IF EXISTS image_permission_seed;
