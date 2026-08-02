# Mobile Application

The Mobile Application serves Customers independently of the administration system.

## Identity

**Customer**: A person identity that can use the Mobile Application. It is distinct from an Admin User and does not inherit administrator roles, navigation, or session contracts. _Avoid_: Mobile User, Admin User, End User, App User

**Customer Session**: The authenticated login state of a Customer. It is independent of an Admin Session and can be refreshed or revoked without changing the Customer identity.

**Customer Access Baseline Role**: The mandatory built-in `customer` role retained by every active Customer. _Avoid_: Customer Admin Role, Mobile User Role

**Default Customer Baseline**: The recoverable bootstrap Customer, RBAC user, Customer Access Baseline Role, and mandatory binding required after system initialization. Recovery preserves the existing Customer password, nickname, and avatar.
