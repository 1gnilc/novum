-- 客户身份初始化脚本。
-- 依赖先执行 01 至 07。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS nv_customer (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    del tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
    create_time datetime(6) NOT NULL COMMENT '创建时间（UTC）',
    update_time datetime(6) DEFAULT NULL COMMENT '更新时间（UTC）',
    user_id bigint NOT NULL COMMENT 'RBAC 全局用户 ID',
    username varchar(320) NOT NULL COMMENT '登录用户名',
    password varchar(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    nickname varchar(255) NOT NULL COMMENT '昵称',
    avatar varchar(500) DEFAULT NULL COMMENT '头像地址',
    status tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用状态',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户';

UPDATE az_role
SET del = 0,
    name = '客户',
    remark = '客户基础角色',
    built_in = 1
WHERE code = 'customer';

INSERT INTO az_role (del, create_time, update_time, code, name, remark, built_in)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'customer', '客户', '客户基础角色', 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_role WHERE code = 'customer'
);

SET @default_customer_existing_user_id := (
    SELECT user_id
    FROM nv_customer
    WHERE username = 'customer'
    LIMIT 1
);

INSERT INTO az_user (id, del, create_time, update_time)
SELECT @default_customer_existing_user_id, 0, UTC_TIMESTAMP(6), NULL
WHERE @default_customer_existing_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_user WHERE id = @default_customer_existing_user_id
  );

INSERT INTO az_user (del, create_time, update_time)
SELECT 0, UTC_TIMESTAMP(6), NULL
WHERE @default_customer_existing_user_id IS NULL;

SET @default_customer_user_id := COALESCE(
    @default_customer_existing_user_id,
    LAST_INSERT_ID()
);

INSERT INTO nv_customer (
    del, create_time, update_time, user_id, username, password,
    nickname, avatar, status
)
SELECT
    0,
    UTC_TIMESTAMP(6),
    NULL,
    @default_customer_user_id,
    'customer',
    '$2y$10$vjUNB/mAmPcweognGYbnyOeeQQzjL5DCQeThxucH1pC6nJfskup7G',
    '客户',
    NULL,
    1
WHERE @default_customer_existing_user_id IS NULL;

UPDATE nv_customer
SET del = 0,
    status = 1
WHERE username = 'customer'
  AND (del <> 0 OR status <> 1);

INSERT INTO az_user (id, del, create_time, update_time)
SELECT customer.user_id, 0, UTC_TIMESTAMP(6), NULL
FROM nv_customer customer
LEFT JOIN az_user user ON user.id = customer.user_id
WHERE customer.del = 0
  AND user.id IS NULL;

UPDATE az_user user
JOIN nv_customer customer ON customer.user_id = user.id
SET user.del = 0
WHERE customer.del = 0
  AND user.del <> 0;

SET @customer_role_id := (
    SELECT id
    FROM az_role
    WHERE code = 'customer'
      AND del = 0
    LIMIT 1
);

UPDATE az_user_role binding
JOIN (
    SELECT candidate.user_id, candidate.role_id, MIN(candidate.id) AS id
    FROM az_user_role candidate
    JOIN nv_customer customer ON customer.user_id = candidate.user_id
    LEFT JOIN az_user_role active_binding
        ON active_binding.user_id = candidate.user_id
        AND active_binding.role_id = candidate.role_id
        AND active_binding.del = 0
    WHERE customer.del = 0
      AND candidate.role_id = @customer_role_id
      AND candidate.del <> 0
      AND active_binding.id IS NULL
    GROUP BY candidate.user_id, candidate.role_id
) existing_binding ON existing_binding.id = binding.id
SET binding.del = 0,
    binding.update_time = UTC_TIMESTAMP(6);

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, customer.user_id, @customer_role_id
FROM nv_customer customer
WHERE customer.del = 0
  AND @customer_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_user_role binding
      WHERE binding.user_id = customer.user_id
        AND binding.role_id = @customer_role_id
  );
