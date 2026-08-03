# Separate baseline access from management roles

Every Admin User retains the built-in `admin` role for self-service and administration-shell access, while `rbac:manager` and `i18n:manager` grant their respective management capabilities. Initialization grants the management roles to the default administrator, but ordinary Admin Users do not inherit them merely by being able to sign in. Every role replacement and unbind entry point rejects removal of the `admin` binding instead of silently restoring it.
