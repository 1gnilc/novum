# Server

The Server establishes identities, evaluates access to protected targets, and enforces application APIs. Admin-specific vocabulary remains canonical in [`apps/admin/CONTEXT.md`](../admin/CONTEXT.md), while Customer vocabulary remains canonical in [`apps/mobile/CONTEXT.md`](../mobile/CONTEXT.md); neither is duplicated here.

## Authentication And Authorization

**Authentication**: Confirmation of which principal initiated an access. Successful authentication establishes identity but does not grant access by itself.

**Authorization**: Preparation of the facts and permission sets needed to decide whether an access is allowed.

**Permission Checking**: Evaluation of whether granted permissions satisfy required permissions. It does not discover permission sources or handle a denied outcome. _Avoid_: Authorization check

**Granted Permission**: A permission available to an access identity for one authorization decision. It may come from roles, groups, system identities, temporary grants, or anonymous defaults.

**Required Permission**: A permission demanded by a protected target for one authorization decision.

**Public Access Permission**: A permission granted to every access without a role binding, including anonymous access. It does not imply that a corresponding navigation item is visible.

**Access Context**: The authorization facts for one access: its environment, identity, target, and optional attributes. Runtime objects such as requests, connections, and caches are not access facts.

**Access Environment**: The execution environment in which an authorization decision occurs, such as Servlet, messaging, or a scheduled task. It prevents unrelated environments from contributing facts to one decision.

**Access Identity**: The identity fact participating in authorization, such as a user, anonymous visitor, system identity, service account, or task identity. _Avoid_: User, account

**Access Target**: The protected destination of an access, optionally qualified to distinguish variants such as an HTTP method or operation.

**Access Decision**: The allow or deny result produced by permission checking for one access context.

**Access Denied Handling**: The environment-specific action taken after a denied access decision, such as returning an HTTP response, rejecting a message, or stopping a task.

## Image Storage

**Managed Image**: An image uploaded through Novum and confirmed as available for application use. Business resources reference it by its Image Object Key rather than by a public URL.

**Image Object Key**: The stable storage identity of a Managed Image. A public presentation URL may change without changing this identity. _Avoid_: Image URL, Image ID

**Image Upload**: The lifecycle that reserves storage for an image and ends when the stored object is confirmed as a Managed Image or the reservation expires.

## Settings

**Setting**: A backend-owned runtime business rule persisted as a unique uppercase constant name and string value. Project code accesses a Setting through its centralized named constant rather than repeating its name literal. _Avoid_: Business Setting, Application Property, Feature Flag

**Setting Log**: The append-only record of one Admin change to a Setting, retaining its previous value, new value, Admin identity, and creation time.

## Client Network Identity

**Client IP**: The IP recorded for a Customer request. Resolve the first valid address in `X-Forwarded-For`; if absent or invalid, use the Servlet remote address; if neither is valid, use `unknown`. _Avoid_: Proxy IP
