# Restore the default Customer baseline

In `deploy/sql/08_customer.sql`, initialize one Customer with `username = 'customer'`, BCrypt password for `123456`, `nickname = '客户'`, `avatar = NULL`, `status = 1`, and `del = 0`, together with its RBAC user and active `customer` role binding. Repeated initialization creates no duplicates; it restores a logically deleted or disabled default Customer and missing RBAC records while preserving an existing password, nickname, and avatar.
