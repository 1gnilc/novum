-- 客户会话权限初始化脚本。
-- 依赖先执行 08_customer.sql。

SET NAMES utf8mb4;

UPDATE az_permission
SET del = 0,
    name = code,
    target_identifier = '/customer/login',
    target_qualifier = 'POST',
    public_access = 1,
    built_in = 1
WHERE code = 'POST:/customer/login';

INSERT INTO az_permission (
    del, create_time, update_time, code, name, target_identifier,
    target_qualifier, public_access, built_in
)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'POST:/customer/login', 'POST:/customer/login',
       '/customer/login', 'POST', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_permission WHERE code = 'POST:/customer/login'
);

UPDATE az_permission
SET del = 0,
    name = code,
    target_identifier = '/customer/refresh',
    target_qualifier = 'POST',
    public_access = 1,
    built_in = 1
WHERE code = 'POST:/customer/refresh';

INSERT INTO az_permission (
    del, create_time, update_time, code, name, target_identifier,
    target_qualifier, public_access, built_in
)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'POST:/customer/refresh', 'POST:/customer/refresh',
       '/customer/refresh', 'POST', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_permission WHERE code = 'POST:/customer/refresh'
);

UPDATE az_permission
SET del = 0,
    name = code,
    target_identifier = '/customer/logout',
    target_qualifier = 'POST',
    public_access = 1,
    built_in = 1
WHERE code = 'POST:/customer/logout';

INSERT INTO az_permission (
    del, create_time, update_time, code, name, target_identifier,
    target_qualifier, public_access, built_in
)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'POST:/customer/logout', 'POST:/customer/logout',
       '/customer/logout', 'POST', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_permission WHERE code = 'POST:/customer/logout'
);

UPDATE az_permission
SET del = 0,
    name = code,
    target_identifier = '/customer/user-info',
    target_qualifier = 'GET',
    public_access = 0,
    built_in = 1
WHERE code = 'GET:/customer/user-info';

INSERT INTO az_permission (
    del, create_time, update_time, code, name, target_identifier,
    target_qualifier, public_access, built_in
)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'GET:/customer/user-info', 'GET:/customer/user-info',
       '/customer/user-info', 'GET', 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_permission WHERE code = 'GET:/customer/user-info'
);

SET @customer_role_id := (
    SELECT id FROM az_role WHERE code = 'customer' AND del = 0 LIMIT 1
);
SET @customer_user_info_permission_id := (
    SELECT id
    FROM az_permission
    WHERE code = 'GET:/customer/user-info'
      AND del = 0
    LIMIT 1
);

UPDATE az_role_permission
SET del = 0,
    update_time = UTC_TIMESTAMP(6)
WHERE role_id = @customer_role_id
  AND permission_id = @customer_user_info_permission_id
  AND del <> 0
ORDER BY id
LIMIT 1;

INSERT INTO az_role_permission (
    del, create_time, update_time, role_id, permission_id
)
SELECT 0, UTC_TIMESTAMP(6), NULL,
       @customer_role_id, @customer_user_info_permission_id
WHERE @customer_role_id IS NOT NULL
  AND @customer_user_info_permission_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_role_permission binding
      WHERE binding.role_id = @customer_role_id
        AND binding.permission_id = @customer_user_info_permission_id
        AND binding.del = 0
  );
