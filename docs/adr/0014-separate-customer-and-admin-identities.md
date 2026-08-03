# Separate Customer and Admin identities

Treat Customers and Admin Users as distinct identities. The Mobile Application must not reuse the Admin Session endpoints, and Customers do not inherit Admin roles, permissions, or menus merely by being able to sign in.

Keep one global RBAC authorization domain. `sys_admin` and `nv_customer` each reference their own `az_user` record and receive different default roles, but any role explicitly bound to either RBAC user is an authoritative grant regardless of whether the request carries an Admin or Customer token. Do not add identity-domain columns, role-domain columns, principal type attributes, or token-based path filters. Identity separation applies to account records, endpoints, token formats, Redis sessions, and default role bindings; it does not create separate RBAC universes.
