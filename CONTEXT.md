# Context Index

Novum contains two related domain contexts. Read only the context relevant to the behavior being changed; read both when work crosses authentication or authorization and the administration system.

## Contexts

- [Server](apps/server/CONTEXT.md): establishes identities, evaluates protected access, and enforces backend APIs.
- [Admin System](apps/admin/CONTEXT.md): manages administrator accounts, RBAC resources, navigation, and dynamic internationalization messages.

## Code Scope

- `apps/server/gnilc-auth/gnilc-auth-core/**` primarily belongs to Server authentication and authorization.
- `apps/admin/**` primarily belongs to Admin System.
- `apps/server/novum-core/**` and `apps/server/gnilc-auth/gnilc-auth-rbac/**` serve both contexts: read Server for enforcement behavior and Admin System for administration behavior.
- `apps/server/deploy/sql/**` may initialize either context; follow the resource being initialized.

## Relationships

- **Admin System -> Server**: administrator credentials establish an access identity; roles and permissions provide authorization facts.
- **Server -> Admin System**: the Server returns authentication or authorization outcomes but does not redefine administrator, menu, or management terminology.
- **Dynamic internationalization -> business resources**: messages supply optional display text and never own the menus or other resources that reference their Message Keys.

## Decisions

- All architectural decisions live in the root [`docs/adr/`](docs/adr/), including decisions scoped to one context.
