# Mobile Application

The Mobile Application serves Customers independently of the administration system.

## Identity

**Customer**: A person identity that can use the Mobile Application. It is distinct from an Admin User and does not inherit administrator roles, navigation, or session contracts. _Avoid_: Mobile User, Admin User, End User, App User

**Customer Session**: The authenticated login state of a Customer. It is independent of an Admin Session and can be refreshed or revoked without changing the Customer identity.

**Customer Online Presence**: The Customer-level Redis indication that any Customer Session has called `GET /customer/user-info` within the previous 15 minutes. Each call renews the shared lifetime; one session's logout does not remove it, and active IP fields change only when presence was absent.

**Customer Access Baseline Role**: The mandatory built-in `customer` role retained by every active Customer. _Avoid_: Customer Admin Role, Mobile User Role

**Country Calling Code**: The international telephone prefix that qualifies a Customer's national phone number. Customer registration and login currently support only `+1` and `+234`; interface language is independent of this value. _Avoid_: Area Code, Registration Market, Language Code

**Customer Phone Identity**: The unique pairing of `dialCode` and a normalized national `phone`, used for registration and login. `phone` contains digits only for the national number; a Customer has no separate username or concatenated full-phone value. _Avoid_: Username, Full Phone String

**Phone Validation**: Backend validation of a Customer Phone Identity using Google libphonenumber and the country rule implied by `dialCode`. Mobile limits selection to supported dialing codes but does not own the final validity decision.

**Customer Password Rule**: A Customer password is 8-64 characters, contains at least three of uppercase letters, lowercase letters, digits, and special characters, and contains no whitespace. It is stored with BCrypt and the same rule applies at registration and every password change. Customer self-service supports only authenticated current-password-to-new-password change; success revokes every Customer Session, and there is no forgotten-password recovery flow.

**Customer Profile**: The Customer-maintained `nickname`, `avatar`, and optional `email`. It excludes Customer Phone Identity, Invitation Code, Referral Relationships, Referral Reward Tier, Customer Account Status, financial values, and login password.

**Customer Account Status**: The enabled or disabled access state of a Customer. Disabled status revokes Customer Sessions and blocks login, refresh, and Customer-initiated operations, but does not delete the identity, cancel existing financial records, remove Referral Relationships, suppress passive Referral Rewards, pause Qualified Direct Referral counting, or pause automatic Referral Reward Tier Progression. Re-enabling requires a new login. A financial restriction is a distinct concern. _Avoid_: Reward Status, Financial Freeze

## Finance

**Platform Currency**: The single currency used for all Customer balances and financial records. The current Platform Currency is Nigerian naira (`NGN`) for every Customer, independent of Country Calling Code.

**Financial Amount Rule**: Every Platform Currency amount has exactly two fractional digits and uses `HALF_UP` rounding for calculated values. Financial amounts are fixed-point values, not binary floating-point values.

**Deposit**: A Customer's request to add funds through a Deposit Channel, including its pending, paid, or cancelled lifecycle. The Chinese product label remains “充值”. _Avoid_: Recharge, Top Up

**Deposit Channel**: Configuration consumed by deployed Deposit Channel Strategy code for a payment-provider route. It is selectable only while enabled and while the chosen Deposit Amount Option is within its configured range; Admin persistence does not prove that current code supports it, and later update, disablement, or deletion does not change existing transactions.

**Deposit Channel Snapshot**: The immutable channel display and execution configuration copied into a Payment Transaction at creation. Payment initiation, callbacks, and history use this snapshot rather than the channel's later state.

**Deposit Gateway**: The Admin-configured provider payment endpoint copied into a Deposit Channel Snapshot and inspected by Deposit Channel Strategies, expected in most cases to be a URL. It is not a persisted strategy identity. _Avoid_: Strategy Key, Channel Name

**Deposit Amount Option**: A predefined amount a Customer must select when initiating a Deposit, with its associated gift amount. The selected amount and gift are snapshotted into the Deposit and do not change with later option administration.

**Deposit Principal**: The amount the Customer successfully pays through the Deposit Channel. It is the base for Referral Level Rewards and Referral Tier Rewards and excludes any Deposit Gift.

**Deposit Credit**: The total amount added to the Customer's balance for a paid Deposit, consisting of the Deposit Principal plus any applicable Deposit Gift.

**Customer Deposit Total**: The cumulative Deposit Principal of a Customer's paid Deposits, stored as `depositTotal`. It excludes Deposit Gift, Registration Gift, Referral Rewards, and every other balance movement, and never decreases while completed Deposits remain final.

**Registration Gift**: A one-time income credited to a newly registered Customer from the configured Setting. It is recorded as its own ledger type and exists only when the registration transaction commits.

**Payment Transaction**: The one-to-one provider-facing payment process for a Deposit, including initiation, payment URL, callback handling, and completion state. It is distinct from the Deposit business record. _Avoid_: Deposit Transaction, Recharge Transaction

**Payment Redirect URL**: The Mobile destination to which the payment provider redirects the Customer's browser after payment. Mobile supplies it as `redirectUrl` for a Deposit request, subject to the backend's configured Mobile-origin boundary. _Avoid_: Return URL, Callback URL

**Payment Callback URL**: The backend-generated endpoint through which the payment provider reports a transaction result. It is not supplied by Mobile or Admin and is distinct from the Customer browser's `redirectUrl`. _Avoid_: Return URL

**Withdrawal**: A Customer's request to remove funds from their balance through a Withdrawal Account. The Chinese product label remains “提现”; the later bank-payment action is a Payout. _Avoid_: Cash Out, Payout

**Withdrawal Account**: One of a Customer's saved cardholder, bank, account-number, and payment-password combinations used to submit a Withdrawal. A Customer may own multiple Withdrawal Accounts, and each account has its own independent payment password. Deleting one releases its Bank Account Identity; a later binding is a new Withdrawal Account rather than restoration of the deleted one.

**Withdrawal Account Payment Password**: The six-digit credential owned by one Withdrawal Account and required to submit a Withdrawal or change that account's details or password. It can be changed only by proving the current value, has no reset workflow, and is temporarily locked for that account after five consecutive failures. _Avoid_: Customer Payment Password, Withdrawal PIN

**Bank Account Identity**: The pairing of a Withdrawal Bank code and a country-neutral bank account identifier, globally unique among active Withdrawal Accounts. Deletion releases it for an independent binding by any Customer; the account identifier does not assume a fixed national format. _Avoid_: Account Number alone

**Withdrawal Bank**: A bank available to Withdrawal Accounts and identified by a unique, immutable bank code. A disabled bank cannot be selected for a new account or Withdrawal but remains meaningful to existing accounts and historical Withdrawals. _Avoid_: Affiliated Bank

**Withdrawal Amount Option**: A predefined amount a Customer may select when submitting a Withdrawal. Customers cannot enter an arbitrary amount, and each option must remain within the configured Withdrawal range.

**Withdrawal Fee**: The percentage-based charge calculated from the requested Withdrawal amount. Its calculated amount and resulting net payout are fixed in the Withdrawal at submission.

**Withdrawal Processing Window**: The daily time range Mobile shows to tell Customers when Withdrawals are processed. It is not a submission window; Customers may submit a Withdrawal at any time. _Avoid_: Withdrawal Time Limit, Submission Window

**Withdrawal Hold**: The full requested Withdrawal amount removed from available balance when a pending Withdrawal is created. The non-final Withdrawal represents the held funds; approval and Payout do not deduct them again.

**Withdrawal Release**: Restoration of a Withdrawal Hold when the pending Withdrawal is rejected. It is not a Deposit refund or reversal. _Avoid_: Withdrawal Refund

**Customer Withdrawal**: The persisted Withdrawal request with one-way `pending -> rejected` or `pending -> approved -> paid` progression. Rejected and paid Withdrawals are final, and Customers cannot cancel them.

**Withdrawal Account Snapshot**: The immutable account-holder, bank, and account-number facts copied by the backend from the Customer's selected Withdrawal Account when a Withdrawal is submitted. Later account changes or deletion do not alter it.

**Payout**: The operational act of sending an approved Withdrawal to the Customer's bank account. In the current scope it is performed manually outside Novum after export. _Avoid_: Withdrawal

## Referrals

**Referral Reward Tier**: A Customer's reward-rate tier, advanced by the number of Qualified Direct Referrals and used to calculate rewards on a direct referral's Deposits. The tiers are regular, gold, platinum, and diamond; regular has a zero Referral Tier Reward rate. _Avoid_: Membership Tier, Benefit Tier, Invitation Benefit Tier

**Referral Reward Tier Progression**: An ascending progression from regular to gold to platinum to diamond. Each newly qualified direct referral increases the count by one and can advance the Customer by at most one tier.

**Managed Referral Reward Tier**: One of the three Admin-maintained Referral Reward Tiers: gold, platinum, or diamond. Regular is the Customer's implicit default and is not a managed tier record.

**Qualified Direct Referral**: A directly referred Customer whose first successful Deposit, at any amount, has completed. Each Customer contributes at most once to the direct inviter's qualification count. A completed Deposit is final in the current scope.

**Invitation Code**: A globally unique six-digit numeric code assigned to a Customer for registration binding. Leading zeroes are significant; `000000` is invalid. _Avoid_: Referral Code, Invite Token

**Invitation Code Immutability**: A Customer's Invitation Code is assigned at creation and cannot be changed through Customer or Admin workflows. Only an audited exceptional data repair may correct an invalid assignment.

**Referrer**: The immutable direct inviter of a Customer, stored as `referrerId`. It is the sole source of truth for direct invitation ownership; the three-level relationship records are derived from it. _Avoid_: `pid`, Parent Account

**Referral Relationship**: One derived ancestry record from an ancestor Customer to a referred Customer at exactly L1, L2, or L3. The ancestor/referred pair is unique and supports team and reward queries without recursive traversal.

**Referral Level Reward**: A Deposit-funded reward independently calculated for the depositor's L1, L2, and L3 referrers using the configured rate for each relationship level. _Avoid_: Level Rebate, Level Commission

**Referral Tier Reward**: A Deposit-funded reward calculated only for the depositor's direct referrer using that referrer's current Referral Reward Tier rate. _Avoid_: Tier Rebate, Benefit Reward

**Deposit Completion**: The one-time atomic transition, initiated by a validated provider callback or Admin Deposit Completion, that marks the Payment Transaction completed and the Deposit paid, credits the Customer balance, records the balance ledger, applies both Referral Reward programs, and evaluates first-Deposit qualification. Repeated completion attempts cannot perform these effects again.

**Final Deposit**: A completed Deposit whose funds, qualification effects, and Referral Rewards are final. The current scope has no Deposit refund or reversal workflow.

**Customer Balance Ledger**: The append-only record of Customer balance movements. Entries can never be updated or deleted; every balance change atomically creates a new entry.

**Ledger Reference**: The identity of the business record that caused a Customer Balance Ledger Entry. The entry type determines what resource the reference identifies, and the same type and reference can affect one Customer only once. _Avoid_: Generic Source

**Deposit Ledger Entry**: An income entry linked to a Payment Transaction. Deposit Principal uses type `DEPOSIT`; a nonzero Deposit Gift uses a separate type `DEPOSIT_GIFT`.

**Referral Reward Ledger Entry**: An independent income entry for one Referral Level Reward or Referral Tier Reward, linked to the Payment Transaction that caused it. The two programs are never merged into one ledger entry.

**Platform-Funded Referral Reward**: A Referral Level Reward or Referral Tier Reward credited by the platform in addition to the depositor's Deposit Credit. It never reduces the depositor's principal, gift, or balance credit, and no separate platform-funds balance gates its settlement.

**Reward Settlement Order**: During Deposit Completion, the system credits the Deposit, records first-Deposit qualification, upgrades the direct referrer's Referral Reward Tier when a threshold is reached, and then calculates the Referral Level Reward and Referral Tier Reward using the resulting current tier and the latest committed reward-rate configuration read by that completion transaction.

**Referral Level Reward Rate**: One of the backend-owned Setting values that determines the L1, L2, or L3 rate for Referral Level Reward calculation. Mobile consumes displayed configuration through APIs and does not own the setting key.
