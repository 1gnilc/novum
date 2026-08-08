# Customer Finance And Referral Requirements

## Document Status

- Status: discovery complete; pending explicit shared-understanding confirmation before implementation
- Working branch: `codex/customer-finance-referral-spec`
- Scope: Customer identity and profile, invitations, Referral Rewards, deposits, withdrawals, balance ledger, business configuration, Admin management, and Mobile flows
- Implementation: blocked until the grilling session reaches shared understanding and the user explicitly confirms the specification
- Source of truth during discovery: every confirmed requirement and decision is recorded here as soon as it is resolved

## Existing System Baseline

- The canonical identity term is **Customer**. A Customer is independent of an Admin User and uses an independent Customer Session.
- The current Customer implementation is a minimal baseline with `username`, password, nickname, avatar, status, login, refresh, logout, and `GET /customer/user-info`; the confirmed target model removes `username` in favor of Customer Phone Identity.
- The current Customer record references the global RBAC user through `user_id` and retains the mandatory built-in `customer` role.
- Registration, phone identity, email, balances, invitations, Referral Rewards, deposits, withdrawals, ledger entries, IP metadata, and online presence are not implemented yet.
- The existing default bootstrap Customer and its automatic recovery behavior will be removed. ADR 0020 supersedes ADR 0016; the built-in `customer` role and mandatory role binding remain.

## Canonical Terminology

The following source terms have been normalized to the confirmed canonical language:

| Source term | Problem | Candidate canonical term |
| --- | --- | --- |
| 客户用户 | Redundant with the existing domain term | Customer / 客户 |
| 区号 | Confirmed as an international telephone prefix, independent of language | Country Calling Code / 电话国家或地区代码 |
| 充值 | Confirmed Customer-facing funds-addition lifecycle | Deposit; Chinese product label remains “充值” |
| 提现 | Confirmed Customer request lifecycle, distinct from the later bank payment | Withdrawal; Chinese product label remains “提现” |
| 福利等级 | Renamed around the rule it controls: rewards earned from qualified direct referrals | Referral Reward Tier / 推荐奖励等级 |
| 奖金比例、返利 | Split into two confirmed reward programs | Referral Level Reward / 推荐层级奖励; Referral Tier Reward / 推荐等级奖励 |
| 邀请有效人数、邀请充值人数 | One direct referral contributes once after satisfying the confirmed Deposit rule | Qualified Direct Referral / 有效直接推荐客户 |
| 充值交易 | Confirmed provider-facing lifecycle, distinct from the Deposit | Payment Transaction / 支付交易 |
| 客户流水 | Immutable balance movement record | Customer Balance Ledger Entry / 客户余额流水 |
| 业务配置 | Runtime business rules stored as name/value pairs | Setting / 业务配置项 |

## Field Naming

Entity and domain terms stay explicit, such as `ReferralRewardTier` and `PaymentTransaction`. Field names rely on their owning record's context and omit repeated domain prefixes. This table is the authoritative field-name map for the current scope; future fields follow the same rule and are added here before implementation.

- Use readable words rather than opaque abbreviations. Standard identifiers such as `id`, `ip`, `url`, and `txnId` are allowed.
- Use one field name for one fact across persistence, backend DTOs, and APIs when those layers carry the same value. Database columns use the normal snake_case form of these names.
- In an owning record, `amount` means that record's primary amount; `total` means a cumulative amount; `gift`, `credit`, `fee`, and `netAmount` retain their defined financial meanings.

| Owning record | Field names |
| --- | --- |
| Customer | `dialCode`, `phone`, `password`, `nickname`, `avatar`, `email`, `inviteCode`, `balance`, `depositTotal`, `withdrawTotal`, `workTotal`, `workIncome`, `inviteCount`, `qualifiedCount`, `teamCount`, `tier`, `autoTier`, `referrerId`, `registerIp`, `registerLocation`, `activeIp`, `activeLocation`, `deposited`, `status`, `userId` |
| ReferralRelationship | `ancestorId`, `referredId`, `level` |
| ReferralRewardTier | `tier`, `threshold`, `rate` |
| WithdrawalAccount | `customerId`, `holder`, `accountNo`, `bankCode`, `payPassword` |
| WithdrawalBank | `name`, `code`, `status` |
| WithdrawalAmountOption | `amount` |
| CustomerWithdrawal | `customerId`, `holder`, `accountNo`, `bankCode`, `bankName`, `amount`, `fee`, `netAmount`, `appliedAt`, `reviewedAt`, `paidAt`, `status`, `rejectReason`, `remark` |
| DepositChannel | `title`, `name`, `gateway`, `code`, `secret`, `min`, `max`, `status` |
| DepositAmountOption | `amount`, `gift` |
| Deposit | `customerId`, `amount`, `gift`, `credit`, `txnId`, `channelId`, `paidAt`, `status` |
| PaymentTransaction | `txnId`, `channelId`, `channelTitle`, `channelName`, `gateway`, `code`, `secret`, `status`, `source`, `reason`, `payUrl`, `redirectUrl`, `callbackUrl`, `completedAt` |
| PaymentCallbackLog | `txnId`, `payload`, `result`, `message`, `ip`, `httpStatus`, `truncated`, `receivedAt` |
| CustomerBalanceLedger | `customerId`, `amount`, `direction`, `type`, `refId`, `before`, `after`, `remark` |
| Setting | `name`, `value` |
| SettingLog | `name`, `oldValue`, `newValue`, `adminId` |

## Customer Account And Profile

### Required Data

- Country Calling Code: only `+1` and `+234` are currently supported.
- `dialCode`: `+1` or `+234`.
- `phone`: normalized national phone number containing digits only; presentation separators such as spaces, hyphens, and parentheses are removed before validation and persistence.
- Strong login password stored as a password hash.
- Nickname, defaulting to the national phone number without the calling code.
- Avatar, using the existing Managed Image and Image Object Key convention.
- Email, editable through profile update, syntactically validated, not verification-code validated, and allowed to duplicate another Customer's email.
- Globally unique six-digit numeric Invitation Code; leading zeroes are significant and `000000` is invalid.
- Available balance.
- Cumulative paid Deposit Principal (`depositTotal`), excluding Deposit Gift, Registration Gift, and Referral Rewards.
- Cumulative withdrawal amount.
- Cumulative worked amount.
- Cumulative work earnings.
- Count of directly invited registrations (L1).
- Count of Qualified Direct Referrals: directly referred Customers whose first successful Deposit at any amount has completed.
- Team size across L1, L2, and L3.
- Referral Reward Tier (`tier`): regular, gold, platinum, or diamond.
- Automatic reward-tier upgrade enabled state (`autoTier`).
- Immutable direct inviter reference (`referrerId`; `pid` in the source description).
- Registration IP address and its location.
- Active IP address and its location.
- Whether the Customer has ever completed a Deposit (`deposited`).
- Enabled or disabled Customer Account Status. Disabled status restricts Customer access and active operations but does not suppress passive Referral Rewards.
- Existing RBAC user reference and mandatory Customer baseline role remain part of the current identity model unless explicitly superseded.

### Defaults

- Available balance defaults to zero.
- Cumulative deposit, cumulative withdrawal, cumulative worked amount, cumulative work earnings, L1 registration count, Qualified Direct Referral count, and L1-L3 team size default to zero in the database; registration code must not redundantly assign them.
- Referral Reward Tier defaults to regular.
- Automatic reward-tier upgrade defaults to enabled.
- Ever-deposited state defaults to false.
- Account status defaults to enabled.
- Registration gift may credit a newly registered Customer. Its amount comes from a Setting and is normally an integer amount.
- When the configured registration gift is greater than zero, successful registration credits it in the same transaction and writes one `REGISTRATION_GIFT` income ledger entry.
- Registration failure rolls back the Customer, invitation closure, and gift together; no partial registration state remains.
- A repeated request after a committed registration cannot create another Customer because Customer Phone Identity is unique and cannot issue a second gift.

### Registration And Login

- Registration is impossible without a valid invitation code.
- Invitation Code generation retries on a uniqueness collision and never falls back to a duplicate code.
- An Invitation Code is permanently immutable after Customer creation; neither Customer nor Admin workflows can change it.
- Exceptional correction is limited to an audited data-repair operation and is not a product API.
- Registration rejects a duplicate Customer Phone Identity.
- The submitted Invitation Code must belong to an existing, enabled Customer.
- The current scope does not attempt to determine whether two different phone identities belong to the same natural person. It does not use IP address, device fingerprinting, phone similarity, or identity verification to detect multi-account self-referral.
- A new Customer has no prior invitation relationships, and invitation binding is immutable after registration; the normal registration workflow therefore cannot create an invitation cycle.
- Every Customer is created through registration; the system has no bootstrap Customer account or automatic Customer-account recovery path.
- Registration binds the new Customer to the inviter and materializes invitation relationships up to three levels.
- Frontend and backend both support the same two Country Calling Codes: `+1` and `+234`.
- Hausa and Yoruba are interface languages only. They do not create distinct phone regions, Customer identities, phone-validation rules, or business markets.
- The backend validates the complete dialing code and national number with Google libphonenumber using the country rule implied by `dialCode`.
- The backend does not implement its own length-only phone regular expression.
- Mobile exposes only the supported dialing-code constants, `+1` and `+234`; backend validation is authoritative.
- Registration and login accept `dialCode` and `phone` as separate values.
- The canonical Customer Phone Identity is the pairing of `dialCode` and normalized `phone`.
- The Customer table enforces a unique constraint across `dialCode` and `phone`.
- Customer has no independent `username` field, and the backend does not persist a redundant concatenated full-phone or username value.
- Login uses Customer Phone Identity and password.
- Customer passwords are 8-64 characters, contain at least three of uppercase letters, lowercase letters, digits, and special characters, and contain no whitespace.
- BCrypt stores the password hash. Registration, login-password changes, and any other Customer password-setting flow use the same rule.
- Customer self-service password change requires an authenticated Customer to submit the correct current password and a new password. There is no forgotten-password, SMS, email, or unauthenticated reset flow.
- A successful self-service password change revokes every Customer Session, including the caller's current session, and Mobile returns to login.
- The new password may equal the current password; successful verification still stores a newly generated BCrypt hash.
- Nickname defaults to the phone number without calling code.
- Email is not accepted or required during registration; it is optional profile data added or changed later.

### Profile Update

- A Customer can update only `nickname`, `avatar`, and `email` through Profile.
- Customer Phone Identity, Invitation Code, Referral Relationships, Referral Reward Tier, Customer Account Status, balances, and cumulative values are not Profile-editable.
- Login-password change is a separate capability and is not part of Profile update.
- Trim `nickname`; require a nonblank result of at most 255 Unicode code points.
- Trim `avatar`; store an existing Managed Image Object Key rather than a public URL, allow null to clear it, and limit it to 500 Unicode code points.
- Trim `email`; allow null to clear it, limit it to 255 Unicode code points, and validate non-null values as email syntax.
- Duplicate email addresses are allowed.
- No email verification code is required.

## Invitation Relationships

### Invitation Relationship Record

- Persist only L1, L2, and L3 relationships.
- Each record contains the ancestor Customer, invited descendant Customer, and relationship level.
- Customer stores immutable `referrerId` as the sole authoritative direct-inviter fact; the three-level relationship records are derived from it.
- Registration writes the Customer and its derived relationship records in one transaction.
- Normal Customer and Admin workflows cannot change `referrerId` after registration.
- On registration, write one relationship row for each existing ancestor at L1, L2, and L3, up to a maximum of three rows: direct referrer -> new Customer at L1, the direct referrer's referrer -> new Customer at L2, and the next ancestor -> new Customer at L3.
- Enforce uniqueness for each ancestor Customer and referred Customer pair.
- The closure supports team statistics and reward lookup without recursive query traversal.

### Counters

- Direct invited registration count covers L1 only.
- Qualified Direct Referral count covers direct referrals only. A Customer qualifies once when their first successful Deposit at any amount completes.
- The qualifying event sets Customer `deposited` to true, increments the direct referrer's `qualifiedCount` once, and checks the direct referrer's automatic Referral Reward Tier upgrade.
- These qualification effects apply even when the direct referrer's Customer Account Status is disabled.
- A completed Deposit is final in the current scope. Deposit cancellation occurs only before completion; no Deposit refund, reversal, or post-completion adjustment workflow exists.
- Team size is the total distinct Customers across L1, L2, and L3.

## Referral Reward Tiers

### Tier Data

- Managed Referral Reward Tiers are gold, platinum, and diamond; regular is the Customer default baseline.
- Regular's Referral Tier Reward rate is implicitly `0.00%`; a regular Customer receives only any applicable L1 Referral Level Reward.
- Each managed tier has a threshold expressed as the number of Qualified Direct Referrals.
- Initial thresholds are `GOLD = 3`, `PLATINUM = 10`, and `DIAMOND = 20` Qualified Direct Referrals.
- Each managed tier has a reward rate.
- Initial reward rates are `GOLD = 13.00%`, `PLATINUM = 16.00%`, and `DIAMOND = 20.00%`.
- Regular is implicit and has no Referral Reward Tier configuration record.
- The configuration contains exactly one record for each of gold, platinum, and diamond. Admin cannot add or remove tier identities.
- Managed-tier progression is fixed: gold < platinum < diamond.
- Qualified Direct Referral thresholds must be strictly increasing in that order.
- Referral Tier Reward rates must also be strictly increasing: gold < platinum < diamond.
- Managed-tier reward rates must satisfy `0.00 <= GOLD < PLATINUM < DIAMOND <= 100.00`.
- Do not cap the combined L1 Referral Level Reward and Referral Tier Reward rate; both are independent Platform-Funded Referral Rewards.
- Admin may modify the threshold and reward rate of an existing managed tier.

### Automatic Upgrade

- A Customer automatically upgrades when the count of Qualified Direct Referrals reaches the next tier threshold.
- Disabled Customer Account Status does not pause qualification counting or automatic tier upgrades.
- Each qualification event adds exactly one Qualified Direct Referral and can advance the Customer by at most one tier.
- Referral Reward Tier Progression is strictly ascending: regular -> gold -> platinum -> diamond. A single event never skips multiple tiers.
- Once earned, a tier never automatically decreases when thresholds are later raised or the qualifying count changes.
- An Admin may manually change a Customer's tier.
- A manual tier change sets Customer `autoTier` to false.
- When an Admin explicitly enables `autoTier`, immediately compare `qualifiedCount` with managed tier thresholds. Keep the current tier when it is higher; otherwise raise it to the highest currently qualified tier, never lowering the Customer.
- An Admin must explicitly re-enable automatic upgrades before later qualifying events can upgrade the Customer again.

## Referral Rewards

### Referral Level Reward

- Every successful Deposit, including the first and all later Deposits, triggers evaluation of Referral Level Rewards.
- The depositor's L1, L2, and L3 ancestors may receive independently configured percentages of the deposited amount.
- Initial rates are L1 10%, L2 2%, and L3 1%.
- For chain A -> B -> C -> D -> E and E depositing 10,000 at the initial rates, D receives 1,000, C receives 200, B receives 100, and A receives nothing because A is beyond L3.
- The three Referral Level Reward rates are Settings, not rows in a dedicated configuration table.
- Their backend-owned names are `REFERRAL_LEVEL_REWARD_L1_RATE`, `REFERRAL_LEVEL_REWARD_L2_RATE`, and `REFERRAL_LEVEL_REWARD_L3_RATE`.

### Referral Tier Reward

- Every successful Deposit, including the first and all later Deposits, triggers evaluation of the direct referrer's Referral Tier Reward.
- Only the depositor's L1 inviter may receive this reward.
- Initial tier rates are gold `13.00%`, platinum `16.00%`, and diamond `20.00%`.
- For chain A -> B -> C -> D -> E, only D is eligible for E's Referral Tier Reward. D receives the percentage belonging to D's current tier.
- Referral Tier Reward stacks with Referral Level Reward. A direct referrer receives both the L1 Referral Level Reward and the Referral Tier Reward when eligible.
- An eligible referrer receives Referral Level Reward and Referral Tier Reward even while that referrer's Customer Account Status is disabled.
- Example: with L1 10%, L2 2%, L3 1%, and a gold Referral Tier Reward of 13%, a Deposit of 10,000 grants the direct referrer 2,300 total, L2 200, and L3 100; total referral-reward expense is 2,600.
- Referral Reward settlement does not read or debit a separate platform-funds balance and cannot fail for platform-funds insufficiency.
- Referral Level Rewards and Referral Tier Rewards are Platform-Funded Referral Rewards: they are additional platform expense and never reduce the depositor's Deposit Principal or Deposit Credit.
- Reward rates are not snapshotted when a Deposit is created. Deposit Completion reads the latest committed L1-L3 Setting rates and managed-tier rates once within its transaction and uses that consistent set for all reward calculations.
- A rate change applies to every Deposit completed after the Admin update commits, including a Deposit created earlier while the old rates were active.
- Reward settlement order is: complete the Deposit credit; record first-Deposit qualification; evaluate and apply the direct referrer's automatic tier upgrade; then calculate and credit Referral Level Reward and Referral Tier Reward using the resulting current tier.

## Customer Withdrawal Account

### Required Data

- Customer reference.
- Cardholder name (`holder`), required after trimming and collapsing consecutive whitespace to one space.
- Normalized `holder` length is 1-100 Unicode characters. Reject control characters but do not restrict names to ASCII, force uppercase, or blacklist other punctuation.
- The current scope does not verify the submitted cardholder name through a bank or account-resolution provider.
- Bank account number.
- Affiliated bank code referencing a Withdrawal Bank.
- A Customer may bind multiple Withdrawal Accounts.
- Each Withdrawal Account has its own independent six-digit payment password; the password is not shared across the Customer's other accounts.
- The normalized pairing of `bankCode` and `accountNo` is globally unique among active Withdrawal Accounts. One active real bank account cannot be bound to multiple Customers or duplicated under one Customer.
- `accountNo` is a country-neutral bank account identifier. The current scope does not require a fixed length, digits-only content, NUBAN validation, or any other country-specific format.
- Normalize `accountNo` by removing all whitespace characters and converting ASCII letters to uppercase. The normalized value must be non-empty and no longer than 64 characters; otherwise its character format is unrestricted.
- Persist only the normalized `accountNo` and use that value in the global `bankCode` plus `accountNo` unique constraint.
- A Customer may update an account's `holder`, `bankCode`, or `accountNo` only after verifying that account's current payment password.
- An update may select only an enabled Withdrawal Bank and must recheck global `bankCode` plus normalized `accountNo` uniqueness.
- Historical Customer Withdrawals retain their submitted account snapshot and do not change when the source Withdrawal Account changes.
- A Withdrawal Account whose bank is later disabled remains visible and may be deleted or changed to an enabled bank, but it cannot be used to submit a new Withdrawal.
- Mobile marks such an account unavailable, and the backend rechecks the current Withdrawal Bank status during Withdrawal submission.
- A payment password is set when its Withdrawal Account is created. It can be changed only by supplying the correct current password and a valid new password.
- A payment password is exactly six ASCII digits. Leading zeroes and all digit combinations, including repeated or sequential values, are allowed; there is no weak-password blacklist.
- There is no forgotten-password reset or recovery workflow for a Withdrawal Account payment password.
- Creating a Withdrawal Account requires verification of the Customer's current login password before setting the new account's payment password.
- Deleting a Withdrawal Account requires verification of the Customer's current login password rather than the account's payment password.
- Creation and deletion are independent operations. A later binding always creates a new Withdrawal Account record and never restores a deleted record.
- Enforce Bank Account Identity uniqueness with `bankCode` and normalized `accountNo`.
- Logical deletion atomically sets the repository-standard `del` flag and rewrites the stored account number as the original normalized value followed by `_del_` and the deleted Withdrawal Account ID.
- Rewriting `accountNo` releases the original unique key so a later binding can insert a new record with the original account number. The persistence column must have room for the deletion suffix in addition to the 64-character active input limit.
- Any Customer may bind a Bank Account Identity after its prior Withdrawal Account has been deleted. Deleted history does not reserve permanent ownership for the former Customer.
- Store each Withdrawal Account payment password as a BCrypt hash, independently from the Customer login-password hash. Do not add a server-side pepper.
- Never return or log a payment-password plaintext value or hash.
- Track payment-password verification failures independently for each Withdrawal Account in Redis.
- Withdrawal submission, account-detail update, and payment-password change share the account's failure count.
- Five consecutive incorrect attempts lock payment-password verification for that account for 30 minutes. While locked, reject verification without running BCrypt.
- A successful verification immediately clears the account's failure count.
- The lock does not affect Customer login or another Withdrawal Account. Account creation and deletion verify the login password and do not participate in this payment-password limit.
- Before the fifth failure, each incorrect attempt renews the Redis failure-count TTL to 30 minutes. The count disappears when 30 minutes pass after the latest incorrect attempt.
- The fifth failure replaces the counting state with an independent 30-minute lock period.

## Withdrawal Bank

- Fields: bank name, bank code, and enabled status.
- Initial source data is [`banks.json`](banks.json).
- Source JSON must be converted to SQL initialization data.
- Initialization inserts the dataset only when the Withdrawal Bank table is empty; if it contains any rows, initialization skips the dataset.
- Admin provides create, update, delete, and unpaginated list operations.
- `code` is the globally unique, immutable business identity of a Withdrawal Bank. Trim surrounding whitespace but otherwise treat it as an opaque value.
- `name` is required and editable but is not unique.
- The source JSON `id` is source metadata and is not imported; the database generates the Withdrawal Bank primary key.
- `status` defaults to enabled. Mobile bank-selection lists contain only enabled banks.
- Admin deletion uses the repository's standard logical-delete behavior and is allowed only when no Withdrawal Account references the bank.
- A referenced bank cannot be deleted and may only be disabled. Existing Withdrawal Accounts and historical Customer Withdrawals must remain displayable after disablement.

### Source Data Audit

- `banks.json` currently contains 445 records with `id`, `name`, and `code` values.
- No record has a missing or empty field, and there are no duplicate IDs, codes, exact names, or case-insensitive names.
- Bank codes have lengths of 3, 4, 6, 8, or 9 characters; only eight records match the `NGR` plus three digits shape.
- Treat source bank codes as opaque values. Do not impose a fixed `NGR` or numeric-only format rule.

## Withdrawal Amount Option

- Each option contains an amount.
- Admin provides create, update, delete, and unpaginated list operations.
- Lists are sorted from smallest to largest amount.
- `amount` is a positive two-decimal NGN value and is unique across all Withdrawal Amount Options.
- Physically delete an option. Customer Withdrawals store an amount snapshot rather than an option reference, so deletion does not affect history and the same amount may later be created as a new option.
- Do not add enabled status, logical-delete state, or an `_del_` uniqueness helper to Withdrawal Amount Options.
- Mobile allows selection only from existing Withdrawal Amount Options and does not provide free-form Withdrawal amount input.
- Mobile submits the selected `amount`; the backend requires an exact match to an existing option and independently enforces the configured minimum and maximum.
- Admin may create or update an option only when its amount is within the current configured minimum and maximum.

## Withdrawal Configuration

- Withdrawal fee rate, stored under the uppercase Setting constant `WITHDRAWAL_FEE_RATE`.
- The setting is a percentage value from `0.00` inclusive to `100.00` exclusive; for example, `5.00` means five percent.
- On submission, calculate `fee = amount * rate / 100` with the platform `HALF_UP` two-decimal rule, snapshot the result in `fee`, and set `netAmount = amount - fee`.
- A valid Withdrawal requires `amount > fee`. Later setting changes do not alter submitted Withdrawal snapshots.
- The current scope supports only this percentage fee and has no fixed or compound fee component.
- Withdrawal Processing Window, initially described as 09:00 to 12:00 and displayed by Mobile to tell Customers when Withdrawals are processed.
- Customers may submit Withdrawals at any time; the processing window never blocks submission.
- The processing window is informational only. It does not restrict Admin approval, rejection, or paid-state operations.
- Store the daily window as `WITHDRAWAL_PROCESSING_START_TIME` and `WITHDRAWAL_PROCESSING_END_TIME`, initially `09:00` and `12:00`, using 24-hour `HH:mm` values.
- The processing-window configuration has no timezone value or timezone-conversion behavior. Mobile displays the configured times directly.
- The displayed window applies every calendar day, including weekends.
- Minimum withdrawal amount.
- Maximum withdrawal amount.
- Store them under Setting constants `WITHDRAWAL_MIN_AMOUNT` and `WITHDRAWAL_MAX_AMOUNT` as two-decimal NGN values.
- `WITHDRAWAL_MIN_AMOUNT` must be greater than `0.00`, and `WITHDRAWAL_MAX_AMOUNT` must be greater than or equal to the minimum.
- Update both range settings atomically. Reject the entire update if any existing Withdrawal Amount Option would fall outside the proposed range.
- Admin must update or delete every conflicting option before narrowing the range; the configuration update never modifies, deletes, or hides options automatically.
- These values are stored as Settings.
- Mobile uses one authenticated Withdrawal configuration query when opening the page. It returns `feeRate`, `minAmount`, `maxAmount`, `processingStart`, `processingEnd`, and `amounts`.
- Serialize fee rate and amounts as decimal strings, sort `amounts` ascending, and return only amount values rather than Withdrawal Amount Option IDs.
- Do not expose Setting names or a timezone field. Missing or invalid settings produce a stable business error rather than a silent default.

## Customer Withdrawal

### Snapshot Data

- Customer reference.
- Cardholder name at submission time.
- Bank account number at submission time.
- Affiliated bank code at submission time.
- Bank name at submission time, used only as historical display and export data.
- Requested amount.
- Withdrawal fee.
- Net payout amount.
- Application time.
- Review time, set when a pending Withdrawal is approved or rejected.
- Paid time, set when an approved Withdrawal is marked paid.
- Status: pending, rejected, approved, or paid.
- Rejection reason.
- Admin remark.

### Workflow

- Mobile submits only `accountId`, `amount`, and `payPassword` when requesting a Withdrawal.
- The backend derives `customerId` from the Customer Session and loads a non-deleted Withdrawal Account by both `accountId` and `customerId`; it never accepts a client-supplied Customer identity or account snapshot.
- Before creating the Withdrawal, the backend verifies the selected account's payment password and confirms that its current Withdrawal Bank is enabled.
- The backend copies `holder`, `accountNo`, `bankCode`, and the current Withdrawal Bank `name` as `bankName` into the new Customer Withdrawal.
- Submission atomically locks the Customer, requires `balance >= amount`, subtracts the full requested `amount` from available balance, creates the pending Customer Withdrawal, and writes a `WITHDRAWAL_HOLD` expense ledger entry.
- A Customer may have any number of pending or approved Withdrawals while available balance remains sufficient. Do not add a daily count or daily amount limit.
- Serialize concurrent submissions for the same Customer through the Customer row lock so each request validates the balance left by earlier committed holds.
- Do not add a Withdrawal submission idempotency key or duplicate-request detector. Two otherwise identical requests that both reach the backend create independent Withdrawals when sufficient balance remains.
- Approval and paid-state processing do not deduct balance again.
- When `APPROVED -> PAID` succeeds, increment Customer `withdrawTotal` by the requested `amount`, not `netAmount`, in the same transaction as the state update and `paidAt`.
- Pending, approved, and rejected Withdrawals do not contribute to `withdrawTotal`; `fee` remains a separate snapshot value.
- Rejection atomically restores the full requested `amount` to available balance and writes a `WITHDRAWAL_RELEASE` income ledger entry.
- Withdrawal Release cancels a pending balance hold and is not a Deposit refund or reversal.
- Do not add a separate frozen-balance field; the non-final Customer Withdrawal is the source of truth for held funds.
- A successful submission snapshots the selected withdrawal account information into the Withdrawal record, so later account changes do not alter historical requests.
- Admin state transitions are limited to `PENDING -> REJECTED` or `PENDING -> APPROVED -> PAID`.
- `REJECTED` and `PAID` are terminal. An approved Withdrawal cannot be rejected or returned to pending, and a Customer cannot cancel a Withdrawal.
- Set `reviewedAt` on either approval or rejection and `paidAt` only when marking an approved Withdrawal paid.
- `rejectReason` is required for rejection and must be empty in every other status.
- Normalize `rejectReason` by trimming surrounding whitespace; require 1-500 Unicode characters and reject control characters.
- Every state-transition update includes the required current status in its database `WHERE` clause. If exactly one row is not updated, fail with a stable state-changed business error and roll back.
- Execute all effects of one operation in the same transaction, including state, timestamps, rejection reason, Withdrawal Release, Customer balance, and ledger writes.
- A repeated or competing Admin operation does not return no-op success; it fails once the expected source status no longer matches.
- An Admin `remark` can be changed through an independent operation at any status. Trim it, accept at most 500 Unicode characters, reject control characters, and persist an empty result as null.
- Remark updates do not use the Withdrawal state as a precondition, and concurrent updates use last-write-wins behavior.
- Payment execution is outside the project. Admin exports approved data and sends it to the bank for manual payment.
- The project records the paid state but does not call a bank payout API in the current scope.
- Do not store the Admin identity that reviews, marks paid, or edits a remark. Do not add Admin actor fields or a Withdrawal action-log table.

### Admin Queries And Export

- Admin provides withdrawal management and filtering.
- Filters are limited to exact Customer `dialCode` plus `phone`, `status`, minimum and maximum `amount`, and independent start/end ranges for `appliedAt`, `reviewedAt`, and `paidAt`.
- Do not add filters for Withdrawal ID, `holder`, `accountNo`, or `bankCode` in the current scope.
- The Admin list is paginated and always sorted by `appliedAt DESC, id DESC`; the current scope has no client-selected sort field.
- Time-range inputs are RFC 3339 UTC instants. Each range is start-inclusive and end-exclusive; either boundary may be omitted, and when both exist the start must be earlier than the end.
- A null `reviewedAt` or `paidAt` does not match when its corresponding range filter is active. Admin converts display time to UTC before calling the API; the backend never interprets a filter using the server's local timezone.
- Export applies the same active query filters and produces a spreadsheet.
- Export ignores pagination and uses the same fixed sort order for all matching rows.
- Export columns, in order, are Withdrawal ID, Country Calling Code, Phone, Account Holder, Account Number, Bank Name, Bank Code, Requested Amount, Fee, Net Amount, Currency, Status, Rejection Reason, Remark, Applied At, Reviewed At, and Paid At.
- Write Phone, Account Number, and Bank Code as text; write amounts as numeric cells displayed with two decimal places; derive Currency as constant `NGN`; write status as `PENDING`, `REJECTED`, `APPROVED`, or `PAID`; and write timestamps as UTC ISO text.
- Generate the `.xlsx` file in the backend and return it directly as the export endpoint's HTTP file response. Do not upload Withdrawal exports to S3 or return an object-storage URL.
- If the active filters match no Withdrawals, do not start the file response. Return a stable “no export data” business Message for Admin to display.
- Before writing the response, count matching rows. Limit one export to 100,000 rows and return a stable “export data exceeds the limit; narrow the filters” business Message when exceeded.
- For an accepted export, query in batches and write the workbook as a stream rather than loading every row into memory. Do not create an asynchronous job or retain a generated file.
- The source mention of import was a dictation error. The current scope supports export only and has no spreadsheet import for Withdrawals or bank payout results.
- Bank payout remains outside Novum; after external payment, Admin manually performs the `APPROVED -> PAID` transition.
- Admin may batch-mark up to 500 selected approved Withdrawals as paid. There is no batch approval or batch rejection operation.
- Every row in a paid batch must still satisfy `status = APPROVED`; process the full batch in one transaction and roll back all rows if any conditional update fails.
- Use one `paidAt` instant for the batch and aggregate requested amounts by Customer before updating each Customer's `withdrawTotal`.

### Mobile Queries

- Provide a paginated list of the authenticated Customer's own Withdrawals, fixed to `appliedAt DESC, id DESC`.
- Accept an optional single `status`; omitting it returns every status.
- Derive Customer identity from the Customer Session and never accept a `customerId` query parameter.
- Provide a detail query that returns a Withdrawal only when it belongs to the authenticated Customer.
- Both responses use the immutable Withdrawal Account Snapshot and never return a payment-password value or hash.
- List fields are `id`, `bankName`, full `accountNo`, `amount`, `fee`, `netAmount`, `status`, and `appliedAt`.
- Detail adds `holder`, `bankCode`, `rejectReason`, `reviewedAt`, and `paidAt`.
- Return the complete snapshotted `accountNo` without masking. Do not return the internal Admin `remark` to Customer.
- Serialize all financial values as two-decimal strings.

## Deposit Channel

### Required Data

- Display title.
- `title`: Customer-facing Mobile display name.
- `name`: globally unique Admin-facing internal name.
- `gateway`: required Admin-configured provider endpoint value, expected in most cases to be a URL but treated globally as an opaque string.
- Trim surrounding whitespace from `gateway`, require 1-2048 characters, and reject control characters. Do not globally require HTTP/HTTPS, alter case or slashes, or otherwise normalize its format; each strategy validates what it supports.
- `code`: provider merchant or channel code, unique together with `gateway`.
- `secret`: provider secret paired with `code` and used by the strategy, stored as plaintext in the Deposit Channel record.
- Minimum amount.
- Maximum amount.
- Enabled or disabled status.
- Normalize surrounding and consecutive whitespace in `title` and `name`; require 1-100 Unicode characters and reject control characters. `title` is not unique, while `name` is unique ignoring case.
- Trim `code`, require 1-128 characters, reject control characters, preserve case, and impose no fixed format.
- Enforce `(gateway, code)` uniqueness case-sensitively against their stored values.
- Require two-decimal NGN range values with `min > 0.00` and `max >= min`.
- Deposit Amount Options are global rather than owned by a channel. A channel supports an option when `min <= amount <= max`.
- Mobile shows only options supported by at least one enabled channel and, after amount selection, only enabled channels supporting that amount.
- Deposit creation rechecks that the option exists, the channel is enabled, and the amount is within its current range.
- Admin may retain an option unsupported by all enabled channels; it remains visible in Admin but is hidden from Mobile. Channel-range changes never modify or delete options.
- Admin provides create, update, logical delete, and unpaginated list operations for Deposit Channels.
- `code` is immutable after creation. Admin may update `title`, `name`, `gateway`, `secret`, `min`, `max`, and `status`.
- Do not persist a separate strategy identifier. Registered strategies inspect a Deposit Channel Snapshot built from the selected channel to decide whether they support it.
- Any channel may be logically deleted, including a previously used channel. Disabled or deleted channels cannot initiate new Deposits.
- Channel update, disablement, or deletion never changes an existing Payment Transaction because the transaction owns an immutable Deposit Channel Snapshot.
- Logical deletion atomically sets `del`, rewrites `name` to `{name}_del_{channelId}`, and rewrites `code` to `{code}_del_{channelId}` while preserving `gateway`.
- The deletion rewrite releases both unique identities so a later create may reuse the original `name`, `gateway`, and `code`. Active-channel `code` immutability does not prohibit this internal deletion rewrite.
- Authorized Admin APIs may return the current `secret` as plaintext rather than a mask or `secretSet` indicator.
- Admin's unpaginated channel list omits `secret`; the single-channel detail/edit query returns it as plaintext. Mobile never receives it.
- Require `secret` on creation. On update, omission preserves the current value, a non-empty value replaces it, and an empty string is invalid.
- Redact `secret` from request, exception, payment-initiation, and callback logs even though it is stored and returned to authorized Admin as plaintext.

## Deposit Amount Option

- `amount` is a unique, positive, two-decimal NGN value.
- `gift` is a non-negative, two-decimal NGN value associated with that amount.
- Admin provides create, update, physical delete, and unpaginated list operations. Lists are sorted by `amount` ascending.
- Mobile cannot enter a free-form Deposit amount and must select an existing option.
- Deposit creation snapshots the selected option's current `amount` and `gift`; later option updates or deletion do not alter it.
- Deposits do not reference the option record. Physical deletion permits a later new option to reuse the same amount, and the option has no status or logical-delete field.

## Deposit

### Required Data

- Customer reference.
- Requested amount.
- Deposit Principal: the amount successfully paid through the Deposit Channel.
- Deposit Gift, when applicable.
- Actual credited amount (Deposit Credit): Deposit Principal plus Deposit Gift.
- Transaction ID.
- Deposit channel reference.
- Settlement time.
- Status: pending payment, paid, or cancelled.
- Referral rewards use Deposit Principal and never Deposit Gift as their calculation base.
- Deposit Completion writes a `DEPOSIT` income ledger entry for Deposit Principal and, when Deposit Gift is greater than zero, a separate `DEPOSIT_GIFT` income ledger entry.
- Both Deposit ledger entries reference the same Payment Transaction, and together their amounts equal Deposit Credit.
- Deposit Completion increments Customer `depositTotal` by Deposit Principal only. The field never decreases in the current scope because paid Deposits have no refund, reversal, or adjustment lifecycle.

## Payment Transaction

- A Payment Transaction is distinct from the Deposit and represents only the provider-facing payment process.
- One Deposit currently has exactly one Payment Transaction; retrying the same Deposit through another transaction or channel is not part of the confirmed model.

### Required Data

- Transaction ID (`txnId`), generated by `IdUtils.generateTxnId()` in the format `PTyyyyMMddHHmmssSSSNNNNNN`, where `SSS` is milliseconds and `NNNNNN` is a six-digit random number.
- Deposit channel reference.
- Status: in progress, completed, or closed.
- Payment URL.
- Payment Redirect URL (`redirectUrl`).
- Callback URL.
- Completion source (`source`): null before completion, `PROVIDER` when completed from a validated provider callback, or `ADMIN` when completed through Admin Deposit Completion.
- Immutable Deposit Channel Snapshot: `channelTitle`, `channelName`, `gateway`, `code`, and plaintext `secret`, copied when the Payment Transaction is created.
- One-to-many Payment Callback Logs for provider callback diagnosis; no callback-log payload is stored on Payment Transaction itself.
- Completion time.
- Callback logs retain the confirmed raw-payload fields and append-only history defined below.

### Deposit Flow

1. Mobile selects a deposit amount and deposit channel, then submits them with a Payment Redirect URL.
2. The backend validates the amount option and channel, builds the immutable Deposit Channel Snapshot, and requires exactly one initiation strategy to match before opening a database transaction.
3. In the first short database transaction, the backend creates the `PENDING` Deposit and `IN_PROGRESS` Payment Transaction with the channel snapshot, then commits both records before any external request.
4. After the first transaction commits, the backend calls the matched external-channel strategy without holding a database transaction.
5. When the external channel returns a payment URL, the backend saves the initiation result in a second short database transaction and returns the payment URL to Mobile.
6. If initiation fails, the backend uses another short database transaction and expected-status conditions to change the Payment Transaction from `IN_PROGRESS` to `CLOSED` and the Deposit from `PENDING` to `CANCELLED`.
7. Mobile redirects the Customer to the payment URL; Mobile's responsibility for this flow ends at navigation.
8. After customer payment, the external channel redirects the browser to the Payment Redirect URL supplied during initiation.
9. The external channel separately calls the backend callback URL with payment-order data.
10. The backend strategy normalizes the channel-specific callback, validates it, and runs one idempotent completion flow that updates the deposit and transaction, credits balances, writes ledger entries, updates invitation qualification, and grants rewards.

- A provider callback may arrive before the initiation request returns. If the callback has already completed the Payment Transaction and Deposit, Deposit Completion wins and later initiation-success persistence or failure cleanup must not reverse or close either completed record.
- If that delayed initiation call returns a valid `payUrl`, persist it only while the stored value is null, allowing either `IN_PROGRESS` or `COMPLETED`, without changing completion status or metadata. When the second transaction observes `COMPLETED/PAID`, return `PAID` with null `payUrl`; otherwise return `PENDING` with the persisted URL.
- If a `payUrl` is already stored, the same value is acceptable; a different value is an internal data conflict and must be logged and returned as a stable internal failure.
- While `DepositChannelStrategy.initiate(...)` is blocked, the current initiation request does not observe callback progress. If the call throws or times out, `DepositService` first attempts the paired expected-state close rather than inferring the Provider outcome.
- If that paired close succeeds, return the stable initiation failure to Mobile. If it fails, reload both records: return a successful paid result when they are `COMPLETED/PAID`, and return a stable internal state-conflict error for every other pair.
- The Mobile initiation response contains `txnId`, `status`, and nullable `payUrl`. Normal initiation returns `PENDING` with the URL; any initiation path that observes an already `COMPLETED/PAID` pair returns `PAID` with null URL, so Mobile skips Provider navigation and displays the completed result.
- A valid provider success callback may also arrive after initiation handling has already produced `CLOSED/CANCELLED`. Provider-confirmed payment wins, so Deposit Completion may conditionally transition that pair to `COMPLETED/PAID` and apply the financial effects once.
- An initiation failure is returned to Mobile only after the conditional close transaction succeeds. A stale expected status indicates that another flow already changed the records and must not be overwritten.
- Every two hours, a recovery job scans every Payment Transaction created more than 40 minutes earlier that still has the exact `IN_PROGRESS/PENDING` pair, regardless of whether `payUrl` is null, then conditionally changes it to `CLOSED/CANCELLED` without retrying Provider initiation.
- A valid late Provider callback may still complete a pair closed by this recovery job. Mobile cannot retry the closed Deposit and must create a new Deposit for another initiation attempt.
- Deposit Completion is atomic and idempotent: the Customer balance, Deposit state, Payment Transaction state, ledger entries, both Referral Reward programs, and first-Deposit qualification commit together or not at all.
- A repeated callback or manual completion request returns the existing completion result and cannot credit funds or rewards a second time.
- After Admin Deposit Completion, a valid provider success callback is still parsed, authenticated, amount-checked, and logged, but it cannot replace `source = ADMIN`, `reason`, or `completedAt` and cannot repeat financial effects. Return the provider's success acknowledgment to stop retries.
- Every callback request associated with an existing transaction, including duplicates and rejected payloads, remains available in Payment Callback Log for diagnosis.
- Strategy selection, initiation, and callback processing use the Payment Transaction's immutable channel snapshot after transaction creation and do not reload mutable execution values from Deposit Channel.
- Deposit Channel update, disablement, or deletion affects only future transaction creation. Existing callbacks continue from their snapshots.
- Mobile supplies only the Payment Redirect URL used by the provider to return the Customer's browser after payment. The backend accepts it only when its origin is listed by Setting `PAYMENT_REDIRECT_ORIGINS`.
- Mobile and Admin cannot supply the Payment Callback URL. The backend builds it from typed deployment configuration and `txnId`, and persists it in the Payment Transaction before calling the provider.
- Persist the validated `redirectUrl` on the same Payment Transaction so the strategy receives both provider URLs from the committed transaction.
- The public callback base address is infrastructure configuration rather than a Setting; only the Payment Redirect Origin allowlist is business-managed configuration.

### Deposit Channel Strategy Contract

- Name the provider strategy interface `DepositChannelStrategy`.
- `DepositService` is the application-workflow module. Its Mobile initiation entry receives the current `customerId` and `DepositInitiateRequest`, then owns validation, object construction, unique strategy selection, both database transactions, Provider invocation, and initiation-result handling.
- Constructor-inject `List<DepositChannelStrategy>` directly into `DepositService`; do not add a separate strategy-dispatch Service.
- Resolve exactly one initiation strategy before the first transaction, retain that selected Bean in the local workflow, and call it after the first transaction commits without evaluating `supportsInitiate` again.
- `DepositService` also owns `CallbackWrapper`, callback strategy selection, zero/multiple/match-error logging, and fallback HTTP responses before delegating a uniquely matched callback.
- Provide `supportsInitiate(DepositChannel channel, Deposit deposit)` to determine initiation support and `DepositInitiateResult initiate(DepositChannel channel, Deposit deposit, PaymentTransaction transaction)` to initiate provider payment.
- Pass the already committed persistent Deposit as `deposit`.
- Pass as `channel` the same Deposit Channel object loaded and validated before the first transaction; retain that in-memory value across the commit and do not reload the mutable channel before `initiate`.
- Pass the already committed Payment Transaction as `transaction`, including persisted `redirectUrl`, `callbackUrl`, and its immutable channel snapshot.
- Treat all three arguments as read-only. A strategy cannot modify, persist, or re-query any of them.
- Continue to use the immutable channel snapshot on Payment Transaction as the authority for later callback execution and history.
- `DepositInitiateResult` currently contains only `payUrl`. Do not add a provider order ID, raw response, or other unconfirmed fields.
- Require `payUrl` to be an absolute HTTP or HTTPS URL of at most 2048 characters, without control characters or user information; preserve its path, query, and fragment.
- Missing or invalid Provider `payUrl` is a stable initiation failure. The Mobile Controller wraps the standard result in the project's normal API response.
- Provide `supportsCallback(HttpServletRequest request)` because callback formats differ by provider and the strategy must inspect the provider request.
- Provide `void callback(HttpServletRequest request, HttpServletResponse response)`. It parses and authenticates provider-specific data, converts it into the standard Deposit Completion input, invokes the shared Deposit Completion method, and writes the provider-specific HTTP status, headers, and body directly.
- After a strategy is selected by callback request shape, it must load the Payment Transaction identified by the callback path and verify that the immutable channel snapshot belongs to that strategy before authentication or Deposit Completion.
- If the request matches strategy A while the transaction snapshot belongs to strategy B, strategy A appends a `REJECTED` Callback Log with `message = CHANNEL_STRATEGY_MISMATCH`, writes its Provider-specific HTTP 400 failure response, and performs no completion work. Do not try another strategy after this mismatch.
- A strategy cannot write or commit the response until callback parsing, authentication, logging, and any Deposit Completion work have finished.
- The strategy writes the Provider-specific status, content type, charset, headers, and body exactly once for every handled result, including rejected and duplicate callbacks.
- The callback Controller never wraps a callback response in the project's `R` JSON format.
- If `callback` lets an unexpected exception escape, the callback Controller writes HTTP 500 with plain-text `error` only when the response is not committed. If it is already committed, do not write again and record an application error without raw payload.
- Before strategy selection, wrap the Servlet request in `CallbackWrapper`, a repeatable-body `HttpServletRequest` wrapper caching at most 64 KiB.
- `CallbackWrapper.getInputStream()` and `getReader()` create a fresh reader over the cached bytes on every call while preserving method, path, query, headers, encoding, and remote address.
- Pass the same `CallbackWrapper` instance, through the declared `HttpServletRequest` type, to every `supportsCallback` call and the one selected strategy's `callback` call.
- `supportsCallback` may read request data but cannot write a response, mutate state, or access persistence.
- Use the cached bytes as the Payment Callback Log payload source. When the body exceeds 64 KiB, skip every strategy; for a known transaction, append a `REJECTED` HTTP 413 log before responding.
- After `callback` returns, the Controller must not wrap or write another response. The strategy cannot rely on a hidden global response object.
- Admin Deposit Completion constructs the same standard Deposit Completion input without passing through a provider callback strategy.
- The shared Deposit Completion method owns all status, balance, ledger, referral, qualification, source, completion-time, transaction, and idempotency behavior for both entry points.
- The standard Deposit Completion input contains only `txnId`, `amount`, `source`, `reason`, and optional `callbackLog`.
- Provider input uses its normalized callback amount, `source = PROVIDER`, null `reason`, and a required pending Callback Log. Admin input uses the stored Deposit Principal, `source = ADMIN`, required `reason`, and null `callbackLog`.
- The shared method requires input `amount` to equal stored Deposit Principal, excludes Deposit Gift from comparison, and rejects any source-specific field mismatch.
- Customer identity, Deposit Gift, reward rates, target states, balances, and completion time are never caller-controlled inputs. The method loads and locks authoritative data, derives all effects, and obtains the completion time from the backend UTC clock.
- Initiation and callback selection require exactly one matching strategy and never use first-match ordering.
- For callback selection, `DepositService` evaluates every registered Provider strategy exactly once. Zero matches append a `REJECTED` Callback Log with `message = NO_STRATEGY`, then return HTTP 400 and plain-text `unsupported`.
- Multiple callback matches are a code/configuration defect. `DepositService` appends an `ERROR` log with `message = MULTIPLE_STRATEGIES`, then returns HTTP 500 and plain-text `error`.
- If any callback support predicate throws, `DepositService` stops selection, appends an `ERROR` log with `message = STRATEGY_MATCH_ERROR`, and returns HTTP 500 and plain-text `error`.
- `DepositService` writes these fallback responses because no unique Provider strategy owns them. Log persistence failure still produces HTTP 500, and application logs may include the strategy class and exception but never raw payload.
- Zero initiation matches produce a stable unsupported-channel business error. Multiple initiation matches are an internal code defect caused by overlapping support predicates; fail without processing and record an internal error.
- Deposit Channel create, update, and enable operations do not inspect registered strategy code or validate a strategy match. A callback with zero or multiple runtime matches is logged and cannot complete the Deposit.
- Each real strategy owns its provider-specific parsing and authentication rules; the Mock strategy contract is fixed below.

### Simulated Channel

- No real third-party channel is currently available.
- Implement `MockDepositChannelStrategy` for development and testing after the specification is approved.
- Match the strategy only when the Deposit Channel Snapshot `code` is exactly `MOCK` and `gateway` is a valid HTTP or HTTPS URL.
- Initialize the development mock channel with `gateway` set to `https://httpbin.org/anything`.
- Initiate mock payment by sending an HTTP POST with JSON containing `txnId`, Deposit Principal, Payment Redirect URL, and Payment Callback URL to the snapshot `gateway`.
- On an HTTP 2xx response, require a valid `url` value and return it as the normalized `payUrl`.
- Treat network failure, timeout, non-2xx response, or a missing or invalid response `url` as initiation failure.
- Use a five-second connection timeout and a fifteen-second response timeout from typed backend deployment configuration.
- Never automatically retry a provider initiation request after timeout, network failure, or non-2xx response.
- Automated tests use a local HTTP stub and never depend on public network availability.
- `MockDepositChannelStrategy` implements the provider callback contract. During testing, a developer may manually send a provider-format request to the public callback endpoint; a successful completion from that request uses `source = PROVIDER`.
- Receive Mock provider callbacks through unauthenticated `POST /api/payment/callback/{txnId}` with JSON fields `txnId`, `amount`, and `status`.
- Require the body `txnId` to equal the path value, require a two-decimal Deposit Principal string in `amount`, and currently accept only `status = SUCCESS`.
- Authenticate header `X-Mock-Signature` as lowercase hexadecimal HMAC-SHA256 over the exact raw UTF-8 request body, keyed by the Payment Transaction snapshot `secret`, using constant-time comparison.
- Do not add a timestamp or nonce to the Mock protocol; repeated valid payloads are safe through `txnId`-scoped Deposit Completion idempotency.
- Return HTTP 200 with text `success` for a valid first or duplicate success callback, HTTP 400 for invalid format or amount, and HTTP 401 for an invalid signature. Retain every attempt in the Callback Log.
- Admin Deposit Completion remains an independent Admin feature using `source = ADMIN`; it is not a substitute for implementing or testing the strategy callback contract.
- The simulated implementation must demonstrate the extension path for a future real channel without adding compatibility paths for hypothetical providers.

### Admin Deposit Completion

- Admin Deposit Completion is an independent Admin financial feature, not a provider callback and not part of `MockDepositChannelStrategy`.
- It loads the existing Deposit and Payment Transaction, performs its own Admin-side validation, and constructs the same standard Deposit Completion input produced after a provider callback is normalized.
- It accepts `txnId` in the request path and requires only `reason` in the request body, containing 1-500 Unicode characters after trimming.
- It may complete the paired states `IN_PROGRESS/PENDING` or `CLOSED/CANCELLED`; an existing `payUrl` is not required.
- Admin cannot submit or override Customer identity, Deposit Principal, Deposit Gift, status, or completion source. The backend obtains every financial value from the existing Deposit and Payment Transaction snapshots.
- A request against `COMPLETED/PAID` returns the existing completion result without another credit. Every other mismatched state pair is a data error and is rejected.
- It invokes the single shared Deposit Completion method rather than duplicating status transitions, balance credit, ledger entries, Referral Rewards, qualification, tier progression, transaction handling, or idempotency logic.
- Successful Admin completion sets Payment Transaction `source` to `ADMIN`; provider callback completion sets it to `PROVIDER`. The field remains null until completion.
- Require RBAC permission and matching Admin button access code `finance:deposit:complete` for Admin Deposit Completion.
- Successful Admin completion persists the submitted `reason` on Payment Transaction. Do not add `adminId`, `adminName`, or a separate Admin action log.
- Provider callback completion leaves `reason` null. Provider callback attempts remain in the Callback Log, while Admin Deposit Completion does not create a fake Callback Log entry.

### Payment Callback Log

- Store each provider callback attempt as its own `PaymentCallbackLog` row associated by `txnId`; do not append callback payloads into one Payment Transaction text field.
- Logs are append-only and cannot be updated or deleted.
- Retain separate rows for first success, duplicate success, format error, amount mismatch, authentication failure, and state conflict.
- Admin views the logs from Payment Transaction detail in fixed order `receivedAt DESC, id DESC`.
- Admin Deposit Completion never creates a Payment Callback Log.
- Store `txnId`, raw `payload`, `result`, optional `message`, Provider `ip`, returned `httpStatus`, `truncated`, and UTC `receivedAt`.
- Use result values `COMPLETED`, `DUPLICATE`, `REJECTED`, and `ERROR`.
- Preserve the original callback body in `payload` without masking or removing sensitive values. Do not store request headers, including signatures, Authorization, or Cookies.
- Record at most the first 64 KiB of the request body. Set `truncated = true`, skip business processing, and return HTTP 413 when the body exceeds that limit.
- Limit `message` to 500 Unicode characters and never persist a stack trace in it.
- Protect log access with RBAC permission and matching button access code `finance:deposit:callback-log`.
- Do not embed logs or payloads in Payment Transaction list or detail responses. Query them through paginated `GET /admin/payment-transactions/{txnId}/callback-logs`, with default size 20, maximum size 100, and fixed order `receivedAt DESC, id DESC`.
- Only an authorized Admin receives the complete stored payload. Mobile has no access, and Callback Log export is outside the current scope.
- For a first successful callback, insert its `COMPLETED` log in the same transaction as every Deposit Completion effect; log failure rolls back completion and returns HTTP 500.
- For duplicate or rejected callbacks, insert the final log in an independent short transaction before returning the normal callback response.
- If callback processing fails and its main transaction rolls back, insert an `ERROR` log in an independent short transaction and return HTTP 500.
- If any non-success log cannot be persisted, return HTTP 500 instead of the otherwise expected 200, 400, or 401. Record the database failure in application logs without copying the raw callback payload.
- Insert every log directly with its final `result`; never create a `PROCESSING` row or update a log later.
- Validate callback path `txnId` through the shared `IdUtils` format check. Return HTTP 400 for an invalid format and HTTP 404 when a validly formatted ID has no Payment Transaction.
- Do not create a Payment Callback Log without an existing transaction. Write only `txnId`, request IP, body byte count, and outcome to the application security log, excluding payload and request headers.
- “Retain every callback attempt” applies only to requests associated with an existing Payment Transaction.

## Customer Balance Ledger

### Currency

- The single Platform Currency is Nigerian naira (`NGN`).
- Every Customer uses NGN, including Customers whose Country Calling Code is `+1`.
- Country Calling Code affects phone identity only and never selects an account currency.
- Multi-currency balances, conversion, and exchange rates are outside the current scope.
- All NGN amounts use exactly two fractional digits.
- Database amount columns use `DECIMAL(19,2)` fixed-point storage rather than binary floating-point types, with maximum supported amount `99999999999999999.99`.
- Java uses `BigDecimal`. Reject Customer or Admin inputs with more than two fractional digits rather than rounding them; apply `HALF_UP` only to calculated monetary results.
- API amount values are transported as decimal strings to preserve precision.
- `float` and `double` are not valid representations for financial amounts.

### Required Data

- Customer reference.
- Amount.
- Direction: income or expense.
- Type: `DEPOSIT`, `DEPOSIT_GIFT`, `WITHDRAWAL_HOLD`, `WITHDRAWAL_RELEASE`, `PENALTY`, `REGISTRATION_GIFT`, `WORK_EARNING`, `REFERRAL_LEVEL_REWARD`, or `REFERRAL_TIER_REWARD`.
- Business reference (`refId`), stored as a string so it can hold either a Payment Transaction `txnId` or a numeric business-record ID.
- Balance before the movement.
- Balance after the movement.
- Remark.

### Integrity Rules

- Every entry has one non-null `refId`; its `type` determines the referenced resource.
- `DEPOSIT`, `DEPOSIT_GIFT`, `REFERRAL_LEVEL_REWARD`, and `REFERRAL_TIER_REWARD` reference the triggering Payment Transaction `txnId`.
- `WITHDRAWAL_HOLD` and `WITHDRAWAL_RELEASE` reference the Customer Withdrawal database ID.
- `REGISTRATION_GIFT` references the registered Customer ID.
- `PENALTY` and `WORK_EARNING` reference their owning business-record IDs; the current scope defines no creation workflow for those still-undefined modules.
- Enforce uniqueness on `(customerId, type, refId)` so one business source cannot produce the same ledger effect twice for one Customer.
- Persist `direction` as `INCOME` or `EXPENSE`; it is not an API-only derived value. Business code must write the value that corresponds to the ledger `type`.
- Income types are `DEPOSIT`, `DEPOSIT_GIFT`, `WITHDRAWAL_RELEASE`, `REGISTRATION_GIFT`, `WORK_EARNING`, `REFERRAL_LEVEL_REWARD`, and `REFERRAL_TIER_REWARD`. Expense types are `WITHDRAWAL_HOLD` and `PENALTY`.
- Every ledger `amount` is greater than `0.00`; direction expresses income or expense, so a ledger amount is never negative. A zero balance movement creates no ledger row.
- `before` and `after` are non-negative two-fraction-digit values. The calling business workflow provides values satisfying `after = before + amount` for income or `after = before - amount` for expense.
- The calling business workflow owns Customer locking, sufficient-balance validation, balance calculation, and balance mutation. The Ledger module only appends the supplied record in that caller-owned transaction.
- `remark` is nullable immutable supplemental text supplied only by the calling business workflow. Trim it, accept at most 500 Unicode characters, reject control characters, and return it only to Admin.
- Current automatic workflows use null `remark` rather than duplicating facts derivable from `type` or `refId`; Customers cannot submit or modify it.
- The Customer Balance Ledger is append-only: no existing entry may be updated or deleted.
- Every balance change updates the Customer balance and inserts its ledger entry in the same database transaction.
- Admin workflows provide ledger query and export only; they do not edit or delete ledger entries.

## Settings

- Persist settings as name/value pairs.
- Setting names are uppercase constants owned by the backend and persisted in the `name` column.
- Backend code reads a setting through its centrally declared constant, never through a repeated string literal.
- Initial required settings include registration gift, withdrawal fee, withdrawal time window, minimum withdrawal amount, maximum withdrawal amount, and Payment Redirect Origins.
- Confirmed Referral Level Reward settings are `REFERRAL_LEVEL_REWARD_L1_RATE`, `REFERRAL_LEVEL_REWARD_L2_RATE`, and `REFERRAL_LEVEL_REWARD_L3_RATE`.
- Store the Payment Redirect Origin allowlist under `PAYMENT_REDIRECT_ORIGINS` as one string containing origins separated by ASCII commas.
- The complete current registry contains exactly `REGISTRATION_GIFT_AMOUNT`, `WITHDRAWAL_FEE_RATE`, `WITHDRAWAL_PROCESSING_START_TIME`, `WITHDRAWAL_PROCESSING_END_TIME`, `WITHDRAWAL_MIN_AMOUNT`, `WITHDRAWAL_MAX_AMOUNT`, `PAYMENT_REDIRECT_ORIGINS`, `REFERRAL_LEVEL_REWARD_L1_RATE`, `REFERRAL_LEVEL_REWARD_L2_RATE`, and `REFERRAL_LEVEL_REWARD_L3_RATE`.
- Treat amount and rate keys as `BigDecimal`, processing-time keys as `LocalTime`, and `PAYMENT_REDIRECT_ORIGINS` as a normalized `List<URI>`.
- Normalize percentage Setting values to two-fraction-digit strings. Persist percentage columns outside Setting as `DECIMAL(5,2)`, use `BigDecimal` in Java, and return rates as two-fraction-digit API strings.
- Keep the database schema limited to `name` and string `value`; do not add a persisted type column.
- Define every supported setting once as a typed `SettingKey<T>` containing its uppercase name, parser, formatter, and validator, and expose them through the centralized `SettingKeys` owner.
- Business code reads through `settingService.get(SettingKeys.SOME_KEY)` and receives the declared Java type. It cannot pass a raw setting name or parse a stored value itself.
- Register every supported key in one registry for Admin lookup. Reject unknown names rather than allowing arbitrary configuration creation.
- Let the owning domain Service validate and atomically update settings with cross-key or database-dependent invariants, such as the Withdrawal range pair.
- Do not cache Settings in Redis or process memory. Batch-load every key required by one workflow, parse each value once, and reuse it only within that workflow.
- A committed Admin update becomes visible to the next database read without cache invalidation or a stale-value window.
- Missing or invalid stored values produce stable business errors rather than silent defaults.
- Initialize `REGISTRATION_GIFT_AMOUNT` to canonical value `0.00`. Registration still succeeds at zero and creates neither a balance credit nor a `REGISTRATION_GIFT` ledger entry.
- Initialize `WITHDRAWAL_FEE_RATE` to canonical value `0.00`; the default fee is zero and `netAmount` equals the requested Withdrawal amount.
- Initialize Referral Level Reward rates to canonical percentage values `10.00` for L1, `2.00` for L2, and `1.00` for L3.
- Initialize `WITHDRAWAL_MIN_AMOUNT` to `1000.00` and `WITHDRAWAL_MAX_AMOUNT` to `1000000.00`.
- Initialize `WITHDRAWAL_PROCESSING_START_TIME` to `09:00` and `WITHDRAWAL_PROCESSING_END_TIME` to `12:00`.
- Parse both Withdrawal processing-time values strictly as 24-hour `HH:mm`, but do not compare their ordering or reject equal, descending, or cross-midnight-looking pairs. They remain display-only values.
- Do not insert `PAYMENT_REDIRECT_ORIGINS` in shared initialization data because its valid value is deployment-specific. The registered key remains visible to Admin and its first validated save creates the missing row.
- Development and automated-test data may explicitly use `http://localhost:6088`; production must configure the real Mobile HTTPS Origin. A missing value blocks only Deposit initiation with a stable business error and does not prevent application startup or unrelated workflows.
- Append one immutable `SettingLog` for each Setting value actually changed by Admin, in the same transaction as the Setting update. Its domain fields are only `name`, `oldValue`, `newValue`, and `adminId`; use standard `createTime` as the change time rather than adding `changedAt`.
- Use null `oldValue` when Admin first creates the registered `PAYMENT_REDIRECT_ORIGINS` row. Do not log initialization SQL or unchanged submitted values.
- Do not store `adminName`, modify or delete logs, or mask old/new values. The fixed Setting registry contains no secret values.
- Let Admin query Setting Logs through a paginated list with exact `name`, exact `adminId`, and `createTime` range filters, fixed order `createTime DESC, id DESC`, and complete old/new values. Do not provide detail, export, update, or delete operations.

## IP Address And Online Presence

### Client IP

- Add a shared utility that resolves the request client's IP address.
- The relevant archived `IpUtils.java` source shape and the confirmed Novum adaptation are preserved in [`client-ip-resolution.md`](client-ip-resolution.md); implementation must not depend on an external absolute path.
- Resolve `X-Forwarded-For` first. If it contains a comma-separated list, use the first valid address from the left.
- When `X-Forwarded-For` is blank, `unknown`, or contains no valid address, use `request.getRemoteAddr()`.
- When neither source yields a valid IPv4 or IPv6 address, persist `unknown`.
- Do not parse `Forwarded`, `Proxy-Client-IP`, `WL-Proxy-Client-IP`, or other compatibility headers.
- Do not configure trusted proxies or proxy CIDR allowlists in the current scope.

### IP Location

- Use IP2Location LITE DB3 IPv6 BIN for offline IPv4 and IPv6 lookup of country, region, and city.
- If no location can be resolved, store `unknown`.
- DB5 latitude/longitude fields are not required.
- The currently supplied `apps/server/novum-core/src/main/resources/IP2LOCATION-LITE-DB3.BIN.zip` is the IPv4-only DB3 package and has zero IPv6 records. Replace it with the DB3 IPv6 BIN package before implementation completes.
- The expected LITE download package code is `DB3LITEBINIPV6`, subject to the exact code shown in the account Download page; verify that the downloaded BIN has IPv6 records.
- Initial database download requires a free IP2Location LITE account. Automated monthly updates use the account's `DOWNLOAD_TOKEN`; the token belongs only in deployment secrets.
- The BIN file is deployment data, not source code. Do not commit it to Git.
- Retain the required IP2Location LITE attribution and redistribution terms with the deployed product.

### Online Presence

- Add a dedicated Customer online-presence service.
- `GET /customer/user-info` checks the Customer's Redis online marker on every call.
- If the marker exists, no online transition or active-IP database update occurs.
- If the marker does not exist, mark the Customer online and update active IP plus active-IP location on the Customer record.
- The marker uses a sliding 15-minute TTL. Every `GET /customer/user-info` renews it to 15 minutes.
- An existing marker renewal does not update `activeIp` or `activeLocation`.
- Customer Online Presence is aggregated by Customer across all Customer Sessions.
- Any session's `GET /customer/user-info` call creates or renews the shared marker.
- Logging out one session does not remove the shared marker. The Customer becomes offline 15 minutes after the last qualifying call from every session.
- Only `GET /customer/user-info` counts as Customer online activity. Other authenticated business requests and the global authentication filter do not create or renew presence.
- Use Redis key `customer:online:{customerId}`.

## Admin Application

Confirmed Admin capabilities:

- Query a paginated Customer list and Customer detail, with no Admin create or delete operation and no generic Customer edit form.
- Change a selected Customer's login password without the old password, enforce the Customer Password Rule, and revoke all of that Customer's sessions after commit.
- Enable or disable a Customer without deleting the identity; disabling revokes Customer Sessions and blocks active Customer operations without cancelling existing financial records or passive referral effects.
- Manage Withdrawal Banks: create, update, delete, and unpaginated list.
- Manage Withdrawal Amount Options: create, update, delete, and unpaginated ascending list.
- Manage Customer Withdrawals: query/filter, approve, reject, edit remark, mark paid as permitted by the confirmed state machine, and export the filtered result to a spreadsheet.
- Query paginated Deposit lists and individual details without editing or deleting Deposit or Payment Transaction records, and export all Deposits matching the active filters to `.xlsx`.
- Perform independent Admin Deposit Completion through the shared completion workflow and inspect permission-gated Payment Callback Logs separately.
- Manage Settings through separate Registration, Withdrawal, Payment, and Referral Level Reward groups rather than generic key/value CRUD.
- Update the threshold and reward rate of the fixed GOLD, PLATINUM, and DIAMOND Referral Reward Tier records; tier identities cannot be created, removed, or renamed.
- Manage Deposit Channels through create, update, logical delete, detail, and unpaginated list operations, including the confirmed secret rules.
- Manage Deposit Amount Options through create, update, physical delete, and unpaginated amount-ascending list operations.
- Manage all three Referral Level Reward rates as one atomic configuration group.
- Manually change a Customer's Referral Reward Tier; doing so disables automatic tier upgrades.
- Re-enable a Customer's automatic tier upgrades.
- Do not allow Admin to edit Customer Phone Identity, Invitation Code, Referrer, Customer Profile, balances, or cumulative values.

## Mobile Application

Confirmed flow requirements:

- Register with a required invitation code.
- Log in using the confirmed phone identity model.
- Update Customer profile including email.
- Retrieve withdrawal configuration.
- Select a deposit amount and channel, submit a Payment Redirect URL, receive a payment URL, and navigate to it.
- View a paginated history containing only the current Customer's Deposits, optionally filtered by one Deposit status and always ordered by `createTime DESC, id DESC`.
- Open a Deposit by `txnId`; when it remains `PENDING` and has a persisted `payUrl`, allow the Customer to continue payment.
- Do not expose Deposit Channel execution configuration, Payment Callback Logs, or other Provider diagnostics to Mobile.
- View a paginated Customer Balance Ledger containing only the authenticated Customer's entries, optionally filtered by `direction` and one ledger `type`.
- Use the existing independent Mobile architecture, Vant component library, static routes, and Customer Session contract.

### Primary Navigation

- Keep the existing five primary tabs: `Home`, `Market`, `Team`, `Fund`, and `My`.
- `Team` owns invitation sharing, team data, Referral Reward Tier status, and tier progression.
- `Fund` is a fully functional finance workspace for Customer balance, Deposit initiation, Withdrawal submission, Deposit history, Withdrawal history, and the Customer Balance Ledger. Do not copy the reference site's unopened Fund placeholder.
- `My` owns Customer Profile, Withdrawal Accounts, Customer login-password change, interface language, and logout.
- Preserve the existing `Home` and `Market` module boundaries. This finance and referral scope does not invent or redefine their work-business behavior.
- Login, registration, detail, and submission screens are secondary routes rather than primary tabs.

### Primary-Tab Access

- `Home` and `Market` are public. `Team`, `Fund`, and `My` require an authenticated Customer Session.
- Keep all five primary tabs visible while unauthenticated; do not hide protected destinations from the tab bar.
- When an unauthenticated Customer selects or directly opens a protected route, show the route-derived global login prompt and do not expose that page's business data.
- The login action opens the public Login route with the full intended route encoded as its redirect target. Successful login returns to that intended route.
- Cancelling the login prompt returns to `Home`; do not leave the Customer on the protected route.
- Preserve static client-side routes and the existing global login-prompt mechanism. Do not add authentication router guards or immediately redirect every protected navigation to Login.

### Authentication Routes

- Provide exactly two public authentication pages in the current scope: `Login` and `Registration`.
- Keep them as separate secondary routes with reciprocal navigation entries and no primary tab bar.
- Do not add forgotten-password recovery, SMS verification, graphical verification, a standalone privacy-agreement page, or a registration agreement checkbox.
- Keep authenticated Customer login-password change under `My`, outside the public authentication routes.
- A back action returns to the valid public source route when one exists; otherwise it returns to `Home`.

### Login Page

- Show a back action and the interface-language selector.
- Accept a Country Calling Code and national phone separately. Offer only `+1` and `+234`, default the selector to `+234`, use a numeric phone-input mode, and remove presentation spaces, hyphens, and parentheses before submission; backend libphonenumber validation remains authoritative.
- Use a masked password field with a visibility toggle and `current-password` autocomplete semantics.
- Provide the primary login action and a link to `Registration`. Do not add remember-me, forgotten-password, verification, or third-party-login controls.
- Lock the form and primary action while a login request is pending so the page cannot submit duplicate requests.
- Use one localized invalid-credentials error that does not distinguish an unknown Customer Phone Identity from a wrong password. Use a separate stable business error for a disabled Customer.
- After successful login, replace the route with a valid internal redirect target when supplied; otherwise enter `Home`.
- Continue to render and accept the Login form when a Customer Session already exists. Do not automatically redirect an authenticated Customer away from `Login`.
- When authenticated Customer A successfully logs in with Customer B's credentials, replace the current browser's locally held access token, refresh token, and Customer information with B's new Customer Session. The same replacement rule applies when A logs in as A again.
- Do not revoke A's prior server-side Customer Session merely because the local Login succeeds; it follows the existing expiry and explicit-revocation rules.
- If the new Login attempt fails, preserve A's current local Customer Session and Customer information unchanged.

### Registration Page

- Show a back action, the interface-language selector, and navigation to `Login`.
- Accept a Country Calling Code and national phone separately. Offer only `+1` and `+234`, default the selector to `+234`, use a numeric phone-input mode, and apply the same presentation-separator normalization as Login; backend libphonenumber validation remains authoritative.
- Use masked password and password-confirmation fields with visibility controls and `new-password` autocomplete semantics. Mobile validates the confirmed Customer Password Rule and equality before submission; confirmation remains Mobile-only and is not an independent backend field.
- Require an Invitation Code of exactly six ASCII digits, preserve leading zeroes, and reject `000000`.
- When an invitation link supplies the Invitation Code, prefill it but allow the Customer to edit it until Registration is submitted.
- Do not collect nickname, avatar, or email. The backend continues to default nickname to the normalized national phone.
- Lock the form and primary action while the request is pending.
- Return distinct stable localized business errors for duplicate Customer Phone Identity and invalid Invitation Code.
- After Registration commits, Mobile calls the standard Login API with the submitted `dialCode`, `phone`, and password; the Registration API does not return a Customer Session.
- Preserve a valid internal redirect target while navigating from Login to Registration. After automatic Login succeeds, replace the current local Customer Session under D174 and enter that target, or `Home` when no target exists.
- If automatic Login fails, keep Registration committed, navigate to Login with `dialCode` and `phone` prefilled, never carry the password, and show a localized registration-succeeded/login-required message.
- When a Customer Session existed before Registration, a failed automatic Login preserves it and a successful automatic Login replaces it under D174.
- Do not add a standalone Registration-success route.

### Team Routes

- Use `Team Overview` as the `Team` primary-tab page. It presents Invitation Code and invitation-link copy actions, total team size, Qualified Direct Referral count, current Referral Reward Tier and progression, and L1/L2/L3 member-count summaries.
- Provide one reusable `Team Members` secondary route parameterized by `L1`, `L2`, or `L3`; do not duplicate one page implementation per level.
- Provide a `Referral Tier Details` secondary route presenting the regular, gold, platinum, and diamond thresholds and rates, the Customer's current tier, and the confirmed progression rules.
- Keep Invitation Code and invitation-link actions on `Team Overview`. Do not add an invitation-poster or image-generation page in the current scope.
- Hide the primary tab bar on both Team secondary routes, and return from them to `Team Overview`.

### Team Overview

- Present the current Referral Reward Tier, its latest reward rate, and the Customer's automatic-tier enabled state.
- For a Customer below diamond, present the next tier's threshold, current Qualified Direct Referral count, remaining count, and a progress indicator. At diamond, present the localized highest-tier state instead of a nonexistent next threshold.
- Present distinct summary values for direct invited registrations, Qualified Direct Referrals, and total L1-L3 team size.
- Present the L1, L2, and L3 member counts independently. Selecting one opens the shared `Team Members` route with that level.
- Present the six-digit Invitation Code with a copy action.
- Build the invitation link from the current Mobile Origin and the router-resolved Registration route with `inviteCode` as its query parameter. The generated URL must respect the deployed router base and hash/history mode rather than hard-coding either form. Provide a copy action.
- Give a localized success or failure result for every copy attempt.
- Provide a route entry to `Referral Tier Details`.
- When `autoTier` is false, display that automatic progression is disabled but do not let the Customer change it.
- Do not display balance, cumulative Referral Reward amounts, or work-business data on Team Overview.

### Team Members

- Accept exactly one relationship-level route value: `L1`, `L2`, or `L3`; reject every other value.
- Return and display only avatar, masked nickname, masked Customer Phone Identity, registration time, and the level-appropriate qualification state for each member. Never return the raw nickname or full phone to this Mobile query.
- Mask nickname on the backend by Unicode code-point count: one code point becomes `*`, two become `**`, and three or more retain only the first and last code points with exactly `***` between them. For example, `Amy` becomes `A***y` and `Chinedu` becomes `C***u`.
- Apply the same nickname rule to a numeric default nickname; do not reconstruct or expose its original phone value. Mobile displays the returned mask without applying another algorithm.
- Format the masked phone as Country Calling Code plus national phone with only its final four digits visible and every preceding digit replaced by `*`.
- Show `Qualified Direct Referral` or not-yet-qualified only for L1 members. Do not apply that direct-referral term to L2 or L3.
- Order by registration time descending and ID descending, and use paginated Mobile infinite loading.
- Provide initial-loading, loading-more, empty-list, and load-failure states.
- Do not add search, filters, or a team-member detail route.
- Do not expose Deposit amounts, balance, Referral Reward Tier, Customer Account Status, or other financial or account-administration data.

### Referral Tier Details

- Present the Customer's current Referral Reward Tier, Qualified Direct Referral count, and automatic-tier enabled state.
- Present tiers in the fixed order `REGULAR`, `GOLD`, `PLATINUM`, `DIAMOND`. Show regular with fixed threshold `0` and rate `0.00%`; load the current committed thresholds and rates for the three Managed Referral Reward Tiers rather than hard-coding their initial values.
- Visually distinguish reached, current, and unreached tiers.
- Below diamond, present progress against the next tier using current count, threshold, remaining count, and a progress indicator. At diamond, omit next-tier progress.
- Explain concisely that progression uses Qualified Direct Referrals, never automatically decreases, and may be disabled after Admin tier management.
- Explain that Referral Tier Reward goes only to the depositor's direct L1 referrer, applies to every successful Deposit, and stacks with that same referrer's L1 Referral Level Reward.
- When `autoTier` is false, present the disabled state without a Customer control.
- Do not include the reference site's group-chat, screenshot check-in, manual application, or inactivity-downgrade rules.
- Keep the page read-only apart from navigation.

### Fund Routes

- Use `Fund Overview` as the `Fund` primary-tab page. It presents the Customer's available balance and entries to Deposit initiation, Withdrawal submission, Deposit history, Withdrawal history, and the Customer Balance Ledger.
- Provide `Deposit Initiation`, `Deposit History`, and `Deposit Details` secondary routes. Address Deposit Details by `txnId` and allow continuation only under the confirmed pending-Deposit rule.
- Provide `Withdrawal Submission`, `Withdrawal History`, and `Withdrawal Details` secondary routes. Withdrawal Details presents the confirmed immutable account snapshot, amount values, lifecycle state, and Customer-visible review outcome.
- Provide a separate `Balance Ledger` secondary route. Do not merge Deposit history, Withdrawal history, and Customer Balance Ledger into one domain view.
- Do not expose a Customer-facing Payment Transaction, Payment Callback Log, or payment-diagnostics route.
- Hide the primary tab bar on every Fund secondary route, and return from them to `Fund Overview`.

### Fund Overview

- Present available balance as the primary value, formatted in NGN with two fractional digits.
- Present `depositTotal` as cumulative paid Deposit Principal, excluding Deposit Gift and every reward, and `withdrawTotal` as cumulative requested amount of `PAID` Withdrawals only.
- Provide primary actions for Deposit and Withdrawal.
- Provide separate entries to Deposit History, Withdrawal History, and Customer Balance Ledger.
- Refresh the Customer finance summary on page entry and through pull-to-refresh by reusing `GET /customer/user-info`, including its confirmed Customer Online Presence behavior.
- Do not display a frozen balance; non-final Withdrawals remain the source of truth for held funds.
- Do not display worked amount, work earnings, or a cumulative Referral Reward value.
- Do not embed recent record rows that duplicate the three dedicated history routes.

### Deposit Initiation

- Present the Customer's current available balance.
- Present only Deposit Amount Options currently supported by at least one enabled Deposit Channel, ordered by amount ascending. Do not provide free-form amount input.
- For each option, present Deposit Principal and present Deposit Gift only when it is greater than zero.
- Start with no amount selected; require an explicit Customer selection.
- After amount selection, present only enabled channels currently supporting that amount. Show Customer-facing `title` only, never internal `name`, `gateway`, `code`, `secret`, or configured range.
- Use an explicit full-row radio selection and start with no channel selected even when only one is available.
- When an amount change makes the selected channel ineligible, clear the channel selection.
- Once both values are selected, present Deposit Principal, Deposit Gift, and expected Deposit Credit as their sum.
- Disable the initiation action until both amount and channel are explicitly selected.
- Distinguish no available amounts, no available channel for the selected amount, and catalog-load failure; never leave the page blank.
- Generate `redirectUrl` internally from the current Mobile Origin and the router-resolved Deposit History route, preserving the deployed base and hash/history mode. Do not expose it as Customer input.
- Submit only selected amount, `channelId`, and generated `redirectUrl`; do not submit Deposit Gift, Deposit Credit, or Customer identity.
- Lock amount, channel, and submit controls while initiation is pending to prevent duplicate taps, but do not add a request idempotency key.
- For `PENDING` with a valid `payUrl`, navigate the current browser window to that URL. Do not open a new tab, embed an iframe, or poll payment status from this page.
- For `PAID` with null `payUrl`, skip Provider navigation and open Deposit Details for the returned `txnId`.
- On initiation failure, remain on the page, present the stable error, and permit a new Deposit attempt. If the selected option, channel, or range became stale, reload the catalog and clear invalid selections.
- After Provider browser redirection to Deposit History, refresh its records on entry so the Customer can inspect the latest state and open details.
- Do not display, copy, or persist `payUrl` in Mobile; use it only for the immediate navigation resulting from that response.

### Deposit History

- Provide one segmented status filter with `ALL`, `PENDING`, `PAID`, and `CANCELLED`; only one state may be active and `ALL` omits the API status filter.
- Present `txnId`, channel title, Deposit Principal, positive Deposit Gift, Deposit Credit, Deposit status, creation time, and `paidAt` when paid in each row.
- Format every amount as NGN with two fractional digits.
- Open Deposit Details by selecting the row. Do not place Continue Payment on the list.
- Use fixed `createTime DESC, id DESC` paginated infinite loading and pull-to-refresh. Refresh the first page automatically when entered from a Provider redirect.
- Provide initial-loading, loading-more, filter-empty, and load-failure states.
- Do not add amount, date, text, or other filters.
- Do not display or copy `payUrl`, Payment Transaction status, or Provider diagnostics.

### Deposit Details

- Present `txnId` with a copy action, channel title, Deposit Principal, positive Deposit Gift, Deposit Credit, Deposit status, creation time, and non-null paid time.
- Omit the Gift row when zero and the paid-time row when null.
- For `PENDING` with non-null `payUrl`, provide Continue Payment. Lock the action after selection and navigate the current window directly without another confirmation dialog.
- For `PENDING` with null `payUrl`, provide no payment action, present that the link is currently unavailable, and allow manual detail refresh.
- For `CANCELLED`, prohibit retry of that Deposit and provide an entry to create a new Deposit.
- For `PAID`, provide no payment action.
- A manual refresh may observe a valid late Provider callback changing pending or cancelled to paid; always render the latest backend state and do not poll automatically.
- Never display, copy, or persist `payUrl`, Payment Transaction status, Callback Logs, `gateway`, `code`, `secret`, or other Provider diagnostics.
- Treat a missing or foreign `txnId` as the same inaccessible result and provide navigation back to Deposit History.

### Withdrawal Submission

- Present available balance and the configured daily processing window as direct `HH:mm-HH:mm` values with concise copy that Withdrawals are processed during that window but may be submitted at any time. Do not append or convert a timezone.
- Start with no Withdrawal Account selected. Open a bottom-sheet single selector listing eligible accounts by holder, Withdrawal Bank name, and full account number.
- Include only active Withdrawal Accounts whose current Withdrawal Bank is enabled. Keep an account with a disabled bank visible only in Withdrawal Account management.
- When no eligible account exists, present an empty state with navigation to Withdrawal Account Creation.
- Present the configured Withdrawal amounts in ascending order with no free-form input and no initial selection.
- Disable an amount above the current available balance while keeping backend balance validation authoritative.
- After amount selection, present requested amount, fee rate, preview fee, and preview net amount. Mobile calculates the preview with the confirmed two-fraction-digit `HALF_UP` rule; the backend independently recalculates and snapshots authoritative values.
- Keep the backend invariant `amount > fee`. If rounding would violate it, disable that amount in Mobile and present the localized Customer-facing result that the amount is currently unavailable; do not expose a configuration-error diagnosis.
- Keep payment-password entry and submission unavailable until both account and amount are selected.
- Distinguish configuration-load failure, account-load failure, no amount options, no eligible accounts, and insufficient-balance states.
- After account and amount selection, collect that Withdrawal Account's exactly six-digit payment password with Vant PasswordInput and NumberKeyboard.
- Do not auto-submit when all six digits are entered. Require an explicit Withdrawal submission action and do not add another confirmation dialog.
- Submit only `accountId`, amount, and `payPassword`; never submit Customer identity, account snapshot, fee, or net amount.
- Lock the page and action during submission. Do not add an idempotency key or automatically retry the mutating request.
- On incorrect payment password, clear only the password, preserve account and amount choices, and present the stable error. For a locked account, present a distinct 30-minute-lock result without exposing Redis or failure-count internals.
- When balance, account, bank, amount option, or configuration became stale, clear the password and refresh the affected authoritative data.
- On success, present confirmation and replace the route with Withdrawal Details addressed by the returned Withdrawal ID.
- On transport timeout or connection loss, treat the submission result as unknown: clear the payment password and direct the Customer to Withdrawal History to verify before creating another request.

### Withdrawal History

- Provide horizontally scrollable single-status tabs for `ALL`, `PENDING`, `REJECTED`, `APPROVED`, and `PAID`; `ALL` omits the API status filter.
- Present Withdrawal Bank name, full account number, requested amount, fee, net amount, Withdrawal status, and application time in each row.
- Format every amount as NGN with two fractional digits.
- Open Withdrawal Details by selecting the row.
- Use fixed `appliedAt DESC, id DESC` paginated infinite loading and pull-to-refresh.
- Provide initial-loading, loading-more, filter-empty, and load-failure states.
- Do not allow Customer cancellation, editing, or resubmission of an existing Withdrawal.
- Do not add amount, date, bank, text, or other filters.
- Keep holder, bank code, rejection reason, review time, paid time, and Admin remark out of list rows; Customer-visible detail fields belong on Withdrawal Details, while Admin remark is never returned.

### Withdrawal Details

- Present Withdrawal ID with a copy action.
- Present the immutable Withdrawal Account Snapshot: holder, Withdrawal Bank name, bank code, and full account number.
- Present requested amount, snapshotted fee amount, and snapshotted net amount. Do not present the current or historical fee-rate Setting as though it were part of the snapshot.
- Present status and a status timeline: application time for pending; application and review times plus rejection reason for rejected; application and review times for approved; and application, review, and paid times for paid.
- For rejected, state that the complete requested amount has been released back to available balance. For approved, state that manual bank Payout is pending.
- Format every amount as NGN with two fractional digits.
- Keep historical snapshot presentation unchanged after Withdrawal Account edit or deletion or Withdrawal Bank disablement.
- Allow manual status refresh without automatic polling.
- Do not provide cancellation, editing, resubmission, Withdrawal Account mutation, or receipt-download actions.
- Never return or display Admin remark.
- Treat a missing or foreign Withdrawal ID as the same inaccessible result and provide navigation back to Withdrawal History.

### Balance Ledger

- Provide a segmented direction filter with `ALL`, `INCOME`, and `EXPENSE` and a separate optional single-type menu.
- When a direction is selected, include only its valid ledger types in the type menu. Clear an existing type selection when it is incompatible with the new direction.
- Cover exactly the current types: `DEPOSIT`, `DEPOSIT_GIFT`, `WITHDRAWAL_HOLD`, `WITHDRAWAL_RELEASE`, `PENALTY`, `REGISTRATION_GIFT`, `WORK_EARNING`, `REFERRAL_LEVEL_REWARD`, and `REFERRAL_TIER_REWARD`.
- Present a localized title derived from `type`, a signed display amount derived from `direction`, balance before, balance after, and creation time in each row.
- Keep API amounts positive and add `+` or `-` only at the Mobile presentation boundary. Format amount and balances as NGN with two fractional digits.
- Use fixed `createTime DESC, id DESC` paginated infinite loading and pull-to-refresh.
- Provide initial-loading, loading-more, filter-empty, and load-failure states.
- Do not add a detail route, amount or time filters, or Customer mutation operations.
- Never expose Ledger ID, Customer ID, `refId`, `remark`, or a persisted title.

### My Routes

- Use `My Overview` as the `My` primary-tab page. It presents a compact Customer summary and entries to Customer Profile, Withdrawal Accounts, Customer login-password change, interface language, and logout.
- Provide a `Profile Edit` secondary route limited to `nickname`, `avatar`, and `email`.
- Provide a `Withdrawal Accounts` secondary list route, with child routes for Withdrawal Account creation, Withdrawal Account editing, and that account's payment-password change.
- Provide a separate `Login Password Change` secondary route.
- Keep interface-language selection on `My Overview` as an in-place selector rather than a dedicated route.
- Perform Withdrawal Account deletion through a login-password confirmation dialog and logout through a confirmation dialog; neither action receives a dedicated route.
- Hide the primary tab bar on every My secondary route, and return each route to its direct parent.
- Use Vant [`PasswordInput`](https://vant-ui.github.io/vant/#/zh-CN/password-input) together with [`NumberKeyboard`](https://vant-ui.github.io/vant/#/zh-CN/number-keyboard) for six-digit Withdrawal Account payment-password entry. Do not render a payment password as visible ordinary text.

### Mobile UI Specification

- Treat [`docs/design/finance-referral-mobile-ui.md`](../../design/finance-referral-mobile-ui.md) as the implementation-ready authority for Mobile route paths, composition, controls, responsive behavior, page states, My-page interactions, localization, reference adoption, and asset selection in this scope.
- Keep this requirements document authoritative for domain terminology, validation, permissions, transaction boundaries, state transitions, API data exposure, and every other business rule. A UI decision cannot override it.
- UI and page-design decisions in the linked document are closed and do not require further question-by-question confirmation. Reopen discovery only when implementation reveals an unresolved core business rule or a contradiction with this specification.
- Preserve the current `Home` and `Market` work-business boundaries; the linked design governs only their shared shell behavior in this scope.

## UI Reference And Browser Constraint

- Reference site: `https://www.novumaivip.com`.
- The repository-owned page audit at [`docs/design/mobile-reference-pages.md`](../../design/mobile-reference-pages.md) records every relevant exact page URL, observed interaction, source mismatch, and design recommendation from the 2026-08-08 review.
- The repository-owned Mobile UI specification at [`docs/design/finance-referral-mobile-ui.md`](../../design/finance-referral-mobile-ui.md) converts those findings into concrete page, state, localization, responsive, and asset decisions without requiring another reference-site session.
- Only AdsPower profile number 37 / profile ID `k1f658vy` may be used to open the reference site for this work.
- AdsPower connection details are stored in `docs/dev/ads.env.local` for an authorized local recapture, but implementation and review must remain possible without that local file, profile, or machine.
- If the reference site requires authentication, use the credentials supplied by the user; credentials must not be copied into this specification, source code, logs, or commits.
- The reference site's layout and styles are inspiration, not a pixel-for-pixel target.
- Improve weak layout and visual details for a more coherent result.
- Use `docs/design/ui-style-guide` primarily for color guidance.
- Treat the checked-in page audit, style guide, tokens, asset manifest, and asset files as the durable reference. Do not make a later developer depend on temporary screenshots, browser history, an authenticated account, or files outside the repository.
- Reuse selected assets from `docs/design/ui-style-guide/assets` by copying only the needed files into the owning application's static asset folder during implementation.
- Runtime code must not import assets from `docs` or hotlink reference-site CDN assets; copy selected repository-owned files into the owning application first.
- The public Vant component references for payment-password entry are captured directly in the My route requirements; implementation uses the repository's installed Vant dependency and does not depend on downloading source from those documentation pages.
- Do not commit authenticated screenshots containing Customer identifiers, Invitation Codes, balances, transaction IDs, or other account data. Preserve reusable layout findings as repository text and non-account-specific assets instead.
- No other browser profile or browser environment may open the reference site.

## Delivery Constraints

- Core business discovery asks one question at a time and waits for the user's answer before continuing.
- Mobile UI and page design are delegated to the repository-owned Mobile UI specification and do not require further individual questions. Ask again only for a newly discovered unresolved core business boundary.
- Questions answerable from the repository are resolved by repository inspection rather than asking the user.
- Each question includes a recommended answer.
- Confirmed terminology is recorded immediately in the owning `CONTEXT.md` glossary.
- Confirmed requirements and decisions are recorded immediately in this document.
- ADRs are created only for hard-to-reverse, surprising architectural choices that resolve a genuine trade-off.
- Business implementation does not begin until the user confirms shared understanding.
- Backend, Admin, and Mobile constants and APIs must have one authoritative definition at their owning boundary; generated or mapped frontend definitions may consume backend semantics without duplicating business truth.
- Existing architecture decisions for Customer/Admin identity separation, Customer sessions, Mobile independence, managed images, UTC instants, error messages, and authorization remain in force.

## Decision Log

### D001: Separate Phone Identity From Interface Language

- Customer registration and login accept only the Country Calling Codes `+1` and `+234`.
- Hausa and Yoruba do not distinguish phone identity or phone validation. They remain interface-language choices independent of the Customer's Country Calling Code.
- A `+234` phone number cannot become two accounts merely by selecting Hausa for one registration and Yoruba for another.

### D002: Replace Username With Customer Phone Identity

- Customer Phone Identity is the unique pairing of Country Calling Code and normalized national phone number.
- Registration and login transmit the two identity values separately.
- The Customer table stores the two values separately and enforces their joint uniqueness.
- The existing `username` field and username-based Customer APIs are replaced; no concatenated username or full-phone duplicate is persisted.

### D003: Remove The Bootstrap Customer

- Remove the default `customer` account and its automatic recovery behavior.
- All Customer identities must be created through phone-and-invitation registration.
- Retain the built-in `customer` role and require its binding for every active Customer.
- ADR 0020 supersedes ADR 0016 so the obsolete default-account invariant is not restored later.

### D004: Name Customer Recharge As Deposit

- Use `Deposit` as the canonical English domain and code term for the Customer-facing recharge lifecycle.
- Use `Deposit`, `DepositChannel`, and `DepositAmountOption` for the corresponding concepts.
- Keep “充值” as the Chinese product label.
- Do not use `Recharge` or `TopUp` as alternate code terminology.

### D005: Separate Deposit From Payment Transaction

- `Deposit` is the Customer-facing Deposit business record.
- `PaymentTransaction` is the provider-facing payment process containing initiation, payment URL, callback, and completion state.
- One Deposit currently corresponds to exactly one Payment Transaction.
- Do not use `DepositTransaction` or `RechargeTransaction` as alternate names.

### D006: Name Customer Withdrawal And Bank Payout Separately

- Use `Withdrawal` for the Customer-requested withdrawal lifecycle and keep “提现” as its Chinese product label.
- Use `WithdrawalAccount`, `WithdrawalBank`, `WithdrawalAmountOption`, and `CustomerWithdrawal` for the corresponding concepts.
- Reserve `Payout` for the later act of sending approved funds to the Customer's bank account; Payout execution remains outside Novum in the current scope.
- Do not use `CashOut` as alternate code terminology or use `Payout` as the name of the Customer request.

### D007: Name The Tier After Its Reward Function

- Rename the source “福利等级” and `membershipTier` concept to `Referral Reward Tier` / “推荐奖励等级”.
- A Referral Reward Tier advances from Qualified Direct Referral counts and determines the reward rate applied to a direct referral's Deposits.
- Use the Customer fields `tier` and `autoTier`.
- Tier values are `REGULAR`, `GOLD`, `PLATINUM`, and `DIAMOND`.
- Do not use `MembershipTier`, `BenefitTier`, or `InvitationBenefitTier` as alternate terminology because the tier is neither a paid membership nor an authorization level.

### D008: Distinguish Referral Reward Programs

- Name the relationship-level program `Referral Level Reward` / “推荐层级奖励”. It independently evaluates the depositor's L1, L2, and L3 referrers at configured level rates.
- Name the direct-referrer tier program `Referral Tier Reward` / “推荐等级奖励”. It evaluates only the depositor's direct referrer using that referrer's current Referral Reward Tier rate.
- Use “奖励” rather than “返利” in canonical Chinese terminology because these payments arise from referrals rather than a Customer's own purchase rebate.
- Do not use `LevelRebate`, `TierRebate`, or generic `ReferralReward` where the program must be distinguished.

### D009: Stack Referral Level And Tier Rewards

- Referral Level Rewards and Referral Tier Rewards are cumulative rather than mutually exclusive.
- An eligible direct referrer receives both the configured L1 Referral Level Reward and the Referral Tier Reward determined by the referrer's current Referral Reward Tier.
- L2 and L3 referrers remain eligible only for their configured Referral Level Rewards.
- Reward-budget calculations, ledger entries, and completion transactions must account for both rewards independently.

### D010: Use NGN As The Platform Currency

- Use Nigerian naira (`NGN`) for every Customer balance and financial record.
- Country Calling Code does not determine currency; Customers registered with either `+1` or `+234` use NGN.
- Multi-currency balances, currency conversion, and exchange rates are not part of the current scope.

### D011: Use Fixed Two-Decimal NGN Amounts

- Store every financial amount with exactly two fractional digits using fixed-point decimal storage.
- Serialize API amount values as decimal strings.
- Do not represent financial amounts with `float` or `double`.
- Round calculated amounts with `HALF_UP` before persisting or using them in a subsequent calculation.

### D012: Make Invitation Codes Globally Unique

- Assign every Customer one globally unique six-digit numeric Invitation Code.
- Preserve leading zeroes; reject `000000`.
- Enforce uniqueness at the database boundary as well as in generation logic.
- Retry generation after a collision; never issue or reuse a duplicate code.

### D013: Keep Invitation Codes Immutable

- Assign an Invitation Code once at Customer creation and never change it through normal product workflows.
- Do not provide Customer or Admin edit operations for Invitation Codes.
- Permit only an explicitly audited exceptional data repair for an invalid assignment.

### D014: Do Not Infer Multi-Account Self-Referral

- Reject registration when Customer Phone Identity already exists.
- Require the Invitation Code to resolve to an existing, enabled Customer.
- Do not infer that different phone identities belong to the same natural person through IP address, device data, or heuristic matching.
- Identity verification, device fingerprinting, and anti-abuse detection are outside the current scope.

### D015: Make `referrerId` The Direct-Inviter Source Of Truth

- Name the direct inviter field `referrerId`; do not use `pid` in project-owned domain interfaces.
- `referrerId` is immutable after registration and is the only authoritative direct relationship.
- Derive the persisted L1-L3 relationship closure from `referrerId` in the same registration transaction.
- Rewards, counters, and team membership resolve direct ancestry from this source and do not treat closure rows as an editable relationship graph.

### D016: Persist A Three-Level Referral Closure

- Persist one Referral Relationship row from every ancestor to the newly registered Customer for each applicable L1, L2, and L3 relationship.
- For A -> B -> C -> D, D's registration produces C -> D L1, B -> D L2, and A -> D L3.
- Restrict records to L1-L3 and enforce uniqueness for the ancestor/referred Customer pair.
- Use this closure for team and reward queries instead of recursive traversal.

### D017: Qualify A Direct Referral On First Successful Deposit

- A Customer becomes a Qualified Direct Referral of their direct referrer when the Customer's first successful Deposit of any amount completes.
- Each Customer increments their direct referrer's Qualified Direct Referral count at most once.
- That event sets Customer `deposited` to true and evaluates automatic Referral Reward Tier upgrade.
- Completed Deposits are final in the current scope; no Deposit refund, reversal, or post-completion adjustment workflow exists.

### D018: Store Referral Level Reward Rates As Settings

- Store the three fixed Referral Level Reward rates in Settings, not a dedicated configuration table.
- Persist uppercase constant names `REFERRAL_LEVEL_REWARD_L1_RATE`, `REFERRAL_LEVEL_REWARD_L2_RATE`, and `REFERRAL_LEVEL_REWARD_L3_RATE`.
- Declare each name once in backend code and access the value through that constant rather than repeating string literals.
- Mobile consumes business values through APIs and does not own setting keys.

### D019: Make Deposit Completion Atomic And Idempotent

- A Payment Transaction may transition to completed only once.
- Customer balance credit, Deposit payment state, Payment Transaction state, ledger entries, Referral Level Rewards, Referral Tier Rewards, and first-Deposit qualification are one database transaction.
- Duplicate callbacks and manual completion requests return the existing result without repeating financial effects.
- Preserve every callback attempt, including duplicates and invalid payloads, in the callback log.

### D020: Use Deposit Principal As The Reward Base

- Calculate both Referral Level Rewards and Referral Tier Rewards from the Deposit Principal successfully paid by the Customer.
- Deposit Gift is added to the Customer's Deposit Credit but is excluded from both reward calculations.
- Example: a 10,000.00 Deposit Principal with a 500.00 Deposit Gift produces 10,500.00 Deposit Credit, while rewards use 10,000.00.

### D021: Keep Referral Reward Ledger Entries Separate

- Write one income ledger entry for each Referral Level Reward and one income ledger entry for each Referral Tier Reward.
- Use ledger types `REFERRAL_LEVEL_REWARD` and `REFERRAL_TIER_REWARD`.
- Link both entries to the Payment Transaction that triggered Deposit Completion.
- Do not merge the two reward programs into one ledger entry; separate entries preserve auditability and independent reporting.

### D022: Credit Registration Gift Atomically Once

- Read the registration-gift Setting and credit the configured amount only within the successful registration transaction.
- Write a separate `REGISTRATION_GIFT` income ledger entry when the amount is greater than zero.
- Roll back Customer creation, Referral Relationship creation, balance credit, and the gift ledger together on any registration failure.
- A later duplicate request for the same Customer Phone Identity is rejected by the unique identity constraint and cannot issue another gift.

### D023: Fund Referral Rewards Outside The Deposit Credit

- The platform funds every Referral Level Reward and Referral Tier Reward as an additional balance credit to the eligible referrer.
- The depositor's Deposit Principal and Deposit Credit are not reduced by referral rewards.
- Reward ledger entries represent platform expense credited as referrer income and remain separate from the depositor's Deposit ledger entry.

### D024: Apply A Newly Earned Tier To The Triggering Deposit

- Within the atomic Deposit Completion transaction, process the depositor's first-Deposit qualification before calculating the direct referrer's Referral Tier Reward.
- If that qualification reaches a Referral Reward Tier threshold and automatic upgrade is enabled, upgrade the direct referrer before reward calculation.
- The triggering Deposit immediately uses the upgraded tier rate.
- Referral Level Reward calculation remains based on the configured L1-L3 rates and is not tier-dependent.

### D025: Advance Referral Reward Tiers One Step At A Time

- A successful first Deposit qualifies at most one direct referral Customer for the inviter.
- Automatic Referral Reward Tier Progression is ascending and advances at most one tier for each newly qualified direct referral.
- The progression order is regular -> gold -> platinum -> diamond; no single qualification event skips multiple tiers.

### D026: Keep Completed Deposits Final

- The current scope has no Deposit refund, reversal, or post-completion adjustment business workflow.
- A completed Deposit, its balance credit, first-Deposit qualification, and both Referral Reward programs are final.
- Payment cancellation exists only before Deposit Completion and never produces completed financial effects.

### D027: Keep The Customer Balance Ledger Append-Only

- Customer Balance Ledger entries are immutable after insertion and cannot be deleted.
- Every balance-changing workflow atomically writes a new ledger entry together with the Customer balance update.
- There is no refund, reversal, or ledger-correction workflow in the current scope.
- Admin workflows can query and export ledger entries but cannot edit or delete them.

### D028: Split Deposit Principal And Gift Ledger Entries

- On Deposit Completion, write a `DEPOSIT` income ledger entry for Deposit Principal.
- When Deposit Gift is greater than zero, write a separate `DEPOSIT_GIFT` income ledger entry.
- Link both entries to the same Payment Transaction.
- The sum of the Deposit ledger-entry amounts equals the Customer's Deposit Credit.

### D029: Fix The Managed Referral Reward Tier Set

- Persist exactly three managed Referral Reward Tier records: `GOLD`, `PLATINUM`, and `DIAMOND`.
- `REGULAR` is the implicit default Customer tier and has no managed configuration record.
- Do not allow Admin to add, remove, or rename tier identities.
- Keep the fixed progression `GOLD < PLATINUM < DIAMOND`; enforce strictly increasing Qualified Direct Referral thresholds.
- Initialize the thresholds as `GOLD = 3`, `PLATINUM = 10`, and `DIAMOND = 20` Qualified Direct Referrals.
- Initialize the reward rates as `GOLD = 13.00%`, `PLATINUM = 16.00%`, and `DIAMOND = 20.00%`.
- Enforce strictly increasing reward rates in the same order: `GOLD < PLATINUM < DIAMOND`. Reject the entire Admin update when the submitted set violates this relationship.
- Require the complete rate set to satisfy `0.00 <= GOLD < PLATINUM < DIAMOND <= 100.00`.
- Do not cap the sum of a tier rate and the independently configured L1 Referral Level Reward rate.
- Admin may update only the threshold and reward rate for an existing managed tier.

### D030: Give Regular Tier No Referral Tier Reward

- `REGULAR` has an implicit `0.00%` Referral Tier Reward rate and no managed tier configuration record.
- A regular direct referrer still receives any applicable L1 Referral Level Reward.
- Only `GOLD`, `PLATINUM`, and `DIAMOND` add a Referral Tier Reward.

### D031: Use Contextual Concise Field Names

- Keep entity and canonical domain names explicit; shorten only fields whose owner already provides the missing context.
- Use the Field Naming table as the single source of truth for persistence, DTO, and API field names.
- Replace earlier long Customer field candidates with `tier`, `autoTier`, and `deposited`; retain `referrerId` because it is already concise and unambiguous.
- Future field names must follow this same convention and must not introduce parallel long aliases.

### D032: Re-Enable Automatic Tier Upgrade Immediately

- An Admin tier change sets Customer `autoTier` to false.
- When an Admin explicitly enables `autoTier`, immediately compare `qualifiedCount` with managed tier thresholds.
- Preserve the current tier when it exceeds the calculated tier; otherwise raise it to the highest currently qualified tier.
- This re-enable operation may advance more than one tier because it is an explicit Admin action, not a single qualification event.

### D033: Normalize Phone Identity Fields

- Send `dialCode` and `phone` as separate API fields.
- Persist `phone` as digits-only national notation with display separators removed.
- Enforce Customer Phone Identity uniqueness on `(dialCode, phone)`.
- Do not persist a concatenated full-phone or username alias.

### D034: Validate Phones With Libphonenumber

- Use Google libphonenumber in the backend to validate the complete Customer Phone Identity for the selected `dialCode`.
- Do not maintain custom fixed-length phone-number regular expressions as the authoritative rule.
- Mobile limits dialing-code selection to `+1` and `+234`, while backend validation is final.

### D035: Use One Strong Customer Password Rule

- Enforce an 8-64 character password.
- Require at least three of uppercase letters, lowercase letters, digits, and special characters.
- Reject all whitespace characters.
- Store only a BCrypt hash.
- Apply the same rule during registration and every password change.

### D036: Resolve Client IP From X-Forwarded-For Then Remote Address

- Resolve Client IP from the leftmost valid address in `X-Forwarded-For`.
- When that header is missing or has no valid address, fall back to `request.getRemoteAddr()`.
- Use `unknown` only when neither source is a valid IPv4 or IPv6 address.
- Do not process `Forwarded` or legacy proxy headers, and do not add trusted-proxy configuration in the current scope.

### D037: Use IP2Location LITE For Offline IP Location

- Use IP2Location LITE DB3 IPv6 BIN and its Java SDK for offline IPv4 and IPv6 country, region, and city lookup.
- Replace the supplied IPv4-only DB3 ZIP with the DB3 IPv6 package and verify that the BIN contains IPv6 records.
- Return `unknown` when lookup cannot resolve a location.
- Accept the operational requirement for one free LITE account and deployment-managed monthly database updates.
- Keep any download token in deployment secrets, never in source code or committed configuration.
- Keep the BIN database outside Git and preserve required attribution and license terms.

### D038: Renew Customer Online Presence For Fifteen Minutes

- `GET /customer/user-info` creates a missing Customer Online Presence marker with a 15-minute TTL and updates Customer `activeIp` and `activeLocation`.
- When the marker exists, renew its TTL to 15 minutes without updating the Customer row.
- With no further `GET /customer/user-info` call, presence expires after 15 minutes.

### D039: Aggregate Online Presence Across Customer Sessions

- Maintain one Customer-level online marker shared by all Customer Sessions.
- A qualifying call from any session creates or renews the marker.
- Logging out one session does not delete the marker or mark the Customer offline.
- The Customer becomes offline 15 minutes after the last qualifying call from all sessions.

### D040: Let User Info Own Online Activity

- Only `GET /customer/user-info` creates or renews Customer Online Presence.
- Do not update presence from the global authentication filter or other authenticated business APIs.
- Use Redis key `customer:online:{customerId}`.

### D041: Keep Withdrawal Bank Codes Stable

- Use the trimmed Withdrawal Bank `code` as a globally unique, immutable business identity and do not validate it against a fixed format.
- Let the database generate the primary key rather than importing the source JSON `id`.
- Keep bank names editable and non-unique.
- A Withdrawal Bank has an enabled status; Mobile offers only enabled banks for new selection.
- Logically delete an unreferenced bank, but reject deletion once a Withdrawal Account references it. Disable referenced obsolete banks while preserving existing account and withdrawal display.

### D042: Allow Multiple Withdrawal Accounts Per Customer

- A Customer may bind multiple Withdrawal Accounts.
- Each Withdrawal Account owns an independent six-digit payment password.
- Do not model the payment password as one Customer-wide credential or copy one shared password across accounts.

### D043: Bind A Bank Account To Only One Customer

- Enforce global uniqueness for the normalized `bankCode` and `accountNo` pair.
- Reject duplicate binding by the same Customer and cross-Customer reuse of the same real bank account.
- Treat the bank code as part of bank-account identity; the same account-number digits at different banks do not conflict.

### D044: Keep Bank Account Numbers Country-Neutral

- Do not impose a fixed length, digits-only rule, NUBAN check, or other country-specific validation on `accountNo`.
- Preserve the field as a general bank account identifier so banks from other countries can be introduced without changing its domain meaning.
- Define only country-neutral normalization and input-safety constraints in the current scope.

### D045: Canonicalize Bank Account Numbers Minimally

- Remove every whitespace character from `accountNo` and convert ASCII letters to uppercase.
- Require the normalized result to contain 1-64 characters, but impose no other character-format rule.
- Persist the normalized result rather than the submitted presentation form.
- Evaluate global `bankCode` and `accountNo` uniqueness against that persisted normalized value.

### D046: Protect Withdrawal Account Changes With Its Password

- Allow a Customer to update a Withdrawal Account's holder, bank, and account number only after verifying that account's current payment password.
- Require the replacement bank to be enabled and recheck global Bank Account Identity uniqueness.
- Keep historical Withdrawal account snapshots unchanged by later source-account edits.
- Set the account's payment password during binding and change it only through a current-password-to-new-password flow.
- Do not provide a forgotten-password reset or recovery flow for the payment password.

### D047: Use Login Password For Withdrawal Account Creation And Deletion

- Verify the Customer's current login password when creating or deleting a Withdrawal Account.
- Creation sets a new independent payment password after login-password verification.
- Deletion does not require the Withdrawal Account payment password.
- Continue to require the account's current payment password for account-detail edits and payment-password changes.

### D048: Rebind Deleted Bank Accounts As New Records

- Treat Withdrawal Account creation and deletion as independent operations; never restore a deleted record during later binding.
- Enforce uniqueness with `bankCode` and normalized `accountNo`; do not add a separate `delId` field.
- During logical deletion, atomically set `del` and rewrite `accountNo` to `{normalizedAccountNo}_del_{withdrawalAccountId}`.
- The suffixed deleted value releases the original unique key and distinguishes multiple deleted historical records.
- Rebinding inserts a new Withdrawal Account with the original normalized account number, a new ID, and a new payment password.

### D049: Do Not Reserve Deleted Bank Account Ownership

- After a Withdrawal Account is deleted, any Customer may independently bind its released Bank Account Identity.
- Do not consult deleted Withdrawal Account history to preserve or enforce ownership by the former Customer.
- Continue to reject duplicate Bank Account Identity only while an active Withdrawal Account owns it.

### D050: Hash Payment Passwords With BCrypt

- Store only a BCrypt hash for each Withdrawal Account's six-digit payment password.
- Do not add a server-side pepper or reversible encryption.
- Keep payment-password hashing and verification independent of the Customer login-password hash.
- Never expose payment-password plaintext or hashes through an API response or application log.

### D051: Lock Payment Password Verification Per Account

- Maintain one Redis-backed failure count for each Withdrawal Account across Withdrawal submission, account-detail update, and payment-password change.
- Lock that account's payment-password verification for 30 minutes after five consecutive incorrect attempts.
- Reject attempts during the lock without running BCrypt, and clear the failure count immediately after successful verification.
- Do not apply this lock to Customer login, other Withdrawal Accounts, or account creation and deletion that use the login password.

### D052: Expire Partial Payment Password Failures After Inactivity

- Give a failure count below five a sliding 30-minute Redis TTL.
- Renew that TTL after each incorrect payment-password attempt.
- Clear an incomplete failure count automatically 30 minutes after the latest incorrect attempt.
- On the fifth failure, replace the partial-count state with the separate 30-minute verification lock.

### D053: Allow Any Six-Digit Payment Password

- Require exactly six ASCII digits for a Withdrawal Account payment password.
- Allow leading zeroes, repeated digits, sequential digits, and every other six-digit combination.
- Do not maintain a weak-payment-password blacklist.

### D054: Block New Withdrawals Through Disabled Banks

- Keep an existing Withdrawal Account visible when its Withdrawal Bank becomes disabled.
- Allow the Customer to delete it or change it to an enabled bank after the normal credential check.
- Prevent Mobile selection and backend Withdrawal submission through the disabled bank.
- Preserve every already-submitted Customer Withdrawal and its account snapshot regardless of later bank status.

### D055: Keep Cardholder Name Validation Country-Neutral

- Require `holder`, trim surrounding whitespace, and collapse consecutive whitespace to one space.
- Accept 1-100 Unicode characters after normalization and reject control characters.
- Do not require ASCII letters, force uppercase, or blacklist otherwise valid punctuation.
- Do not perform bank-side account-holder verification in the current scope.

### D056: Build Withdrawal Snapshots On The Backend

- Accept only `accountId`, `amount`, and `payPassword` from Mobile when submitting a Withdrawal.
- Derive `customerId` from the Customer Session and load the active Withdrawal Account by both account and Customer identity.
- Verify the account payment password and current Withdrawal Bank enabled state before submission.
- Copy account details from the persisted Withdrawal Account into the Customer Withdrawal; never trust client-supplied snapshot fields.

### D057: Snapshot The Withdrawal Bank Name

- Store `bankName` together with `bankCode`, `holder`, and `accountNo` in each Customer Withdrawal.
- Copy the current Withdrawal Bank name during backend submission rather than accepting it from Mobile.
- Treat `bankName` as historical display and export data while `bankCode` remains the stable bank identity.
- Later bank rename, disablement, or deletion does not change the stored Withdrawal snapshot.

### D058: Charge A Percentage Withdrawal Fee

- Store the percentage as Setting `WITHDRAWAL_FEE_RATE`, where `5.00` represents five percent.
- Accept a rate from `0.00` inclusive to `100.00` exclusive and do not support a fixed or compound fee.
- At submission, calculate `fee = amount * rate / 100` with `HALF_UP` to two decimal places and set `netAmount = amount - fee`.
- Require `amount > fee` and snapshot both `fee` and `netAmount` so later configuration changes do not alter the Withdrawal.

### D059: Keep Withdrawal Submission Open At All Times

- Treat 09:00-12:00 as a Withdrawal Processing Window shown in Mobile, not as a Withdrawal Submission Window.
- Allow Customers to submit a Withdrawal at any time of day.
- Do not reject or disable submission merely because the current time is outside the displayed processing window.

### D060: Keep The Processing Window Informational

- Use the Withdrawal Processing Window only as Customer-facing operational information.
- Allow Admin approval, rejection, and paid-state operations at any time.
- Do not enforce the configured window as a backend time restriction for either Customer or Admin workflows.

### D061: Configure A Daily Processing Time Without Timezone

- Use Settings `WITHDRAWAL_PROCESSING_START_TIME` and `WITHDRAWAL_PROCESSING_END_TIME` with initial `HH:mm` values `09:00` and `12:00`.
- Apply the displayed processing window every calendar day, including weekends.
- Do not add a timezone setting, return a timezone field, or convert the configured time values between timezones.
- Let Mobile display the configured start and end values directly.

### D062: Require A Configured Withdrawal Amount Option

- Do not allow Customers to enter an arbitrary Withdrawal amount; Mobile offers existing Withdrawal Amount Options only.
- Submit `amount` and require the backend to match it exactly to an existing option.
- Independently enforce Settings `WITHDRAWAL_MIN_AMOUNT` and `WITHDRAWAL_MAX_AMOUNT` during submission.
- Permit Admin creation or update of an amount option only while its amount is within the current configured range.

### D063: Keep The Withdrawal Range Consistent With Its Options

- Require a positive minimum and a maximum greater than or equal to that minimum.
- Update `WITHDRAWAL_MIN_AMOUNT` and `WITHDRAWAL_MAX_AMOUNT` together as one atomic operation.
- Reject the update when any existing Withdrawal Amount Option would be outside the proposed range.
- Require Admin to modify or delete conflicting options first; never mutate or hide them as a side effect of the range update.

### D064: Physically Delete Unique Withdrawal Amount Options

- Require every Withdrawal Amount Option to have a unique, positive, two-decimal NGN `amount`.
- Keep historical Customer Withdrawals independent by storing only their amount snapshot, not an option foreign key.
- Physically delete options and permit a later creation to reuse the same amount as a new record.
- Do not add option status, logical deletion, or a deletion suffix solely to support uniqueness.

### D065: Hold Withdrawal Funds At Submission

- In one transaction, lock the Customer, require sufficient available balance, deduct the full requested amount, create the pending Customer Withdrawal, and write a `WITHDRAWAL_HOLD` expense ledger entry.
- Do not deduct balance again during approval or paid-state processing.
- On rejection, restore the full held amount and write a `WITHDRAWAL_RELEASE` income ledger entry in the same transaction.
- Treat Withdrawal Release as cancellation of a hold rather than a Deposit refund or reversal.
- Represent held funds through the non-final Customer Withdrawal and do not add a separate Customer frozen-balance field.

### D066: Make Withdrawal Review States One-Way

- Allow only `PENDING -> REJECTED` and `PENDING -> APPROVED -> PAID`.
- Treat `REJECTED` and `PAID` as terminal; do not allow rollback, post-approval rejection, or Customer cancellation.
- Replace `approvedAt` with `reviewedAt`, setting it on either approval or rejection, and add `paidAt` for the paid transition.
- Require `rejectReason` only for rejection and require it to be empty in all other states.
- Permit `remark` updates independently in every state.

### D067: Guard Withdrawal Transitions In The Database

- Include the expected source status in the database `WHERE` clause for every Withdrawal state update.
- Require exactly one affected row; otherwise throw a stable state-changed business error and roll back the operation.
- Keep the state update and all related timestamps, rejection fields, balance release, Customer update, and ledger entries in one transaction.
- Do not treat a repeated or competing transition as idempotent success after its expected source status has changed.

### D068: Count Requested Amount When A Withdrawal Is Paid

- Increment Customer `withdrawTotal` only during a successful `APPROVED -> PAID` transition.
- Add the requested Withdrawal `amount`, not the net payout after fee.
- Do not count pending, approved, or rejected Withdrawals, and keep the fee as a separate snapshot value.
- Update `withdrawTotal`, Withdrawal status, and `paidAt` in one transaction.

### D069: Export Withdrawals Without Spreadsheet Import

- Treat the source word “import” as a dictation error and implement filtered spreadsheet export only.
- Do not import Withdrawals or bank payout results from a spreadsheet.
- Keep bank payout outside Novum and let Admin manually mark an externally paid, approved Withdrawal as paid.

### D070: Keep Withdrawal Filters Focused

- Support exact Customer `dialCode` plus `phone`, one Withdrawal `status`, and minimum and maximum `amount`.
- Support independent start and end ranges for `appliedAt`, `reviewedAt`, and `paidAt`.
- Do not add Withdrawal ID, holder, account-number, or bank-code filters in the current scope.
- Apply the same filter set to the Admin list and export.

### D071: Fix Withdrawal List And Export Ordering

- Paginate the Admin Withdrawal list and sort it by `appliedAt DESC, id DESC`.
- Do not expose client-selected sorting in the current scope.
- Ignore pagination during export and preserve the same fixed ordering across all matching records.

### D072: Filter Withdrawal Instants With Half-Open UTC Ranges

- Accept RFC 3339 UTC instants for every Admin Withdrawal time-range boundary.
- Use start-inclusive, end-exclusive ranges and permit either boundary to be omitted; require start earlier than end when both are present.
- Exclude null `reviewedAt` or `paidAt` values whenever that timestamp's range filter is active.
- Convert UI times to UTC in Admin and never interpret API filter values with the backend host's local timezone.

### D073: Export Stable Withdrawal Columns

- Export these ordered columns: Withdrawal ID, Country Calling Code, Phone, Account Holder, Account Number, Bank Name, Bank Code, Requested Amount, Fee, Net Amount, Currency, Status, Rejection Reason, Remark, Applied At, Reviewed At, and Paid At.
- Preserve Phone, Account Number, and Bank Code as text so leading zeroes remain intact.
- Write financial values as numeric cells with two-decimal display and derive Currency as `NGN`.
- Use stable uppercase Withdrawal status names and UTC ISO text for timestamps.

### D074: Stream Withdrawal Exports Directly

- Generate `.xlsx` in the backend and send it directly in the export endpoint's HTTP response.
- Do not upload the workbook to S3, return an object-storage URL, or retain an export artifact after the response.
- When no rows match the active filters, do not begin the file response and instead return a stable “no export data” business Message for the frontend to display.

### D075: Cap Direct Withdrawal Exports

- Count matching rows before beginning the file response.
- Return “no export data” for zero rows and reject more than 100,000 rows with a stable Message instructing Admin to narrow the filters.
- Query accepted exports in batches and stream workbook output without loading all rows into memory.
- Do not add asynchronous export jobs or retain generated export files.

### D076: Batch Mark Approved Withdrawals Paid

- Support batches of at most 500 selected Withdrawals for `APPROVED -> PAID` only; do not add batch approval or rejection.
- Require every conditional update to match `status = APPROVED` and execute the complete batch in one transaction.
- Roll back the entire batch when any row cannot transition.
- Apply one `paidAt` instant to the batch and aggregate requested amounts by Customer before incrementing each `withdrawTotal`.

### D077: Allow Multiple Non-Final Withdrawals

- Allow one Customer to hold multiple pending or approved Withdrawals simultaneously.
- Apply no daily Withdrawal count or total limit beyond the configured per-Withdrawal range and available balance.
- Serialize concurrent submissions with a Customer row lock and validate each request against the balance remaining after earlier holds.
- Keep review and balance release independent for each Withdrawal.

### D078: Do Not Deduplicate Withdrawal Submissions

- Do not accept an idempotency key, persist a request key, or detect retry-equivalent Withdrawal submissions.
- Treat every accepted request as a new Withdrawal and a new Withdrawal Hold.
- When two identical requests both reach the backend, allow both to succeed if the serialized balance checks pass.

### D079: Validate Withdrawal Review Text Consistently

- Trim `rejectReason`, require 1-500 Unicode characters, reject control characters, and accept it only during rejection.
- Trim `remark`, allow up to 500 Unicode characters, reject control characters, and store an empty result as null.
- Update `remark` independently in any Withdrawal state without a state precondition.
- Resolve concurrent remark writes with last-write-wins behavior.

### D080: Return Complete Mobile Withdrawal Configuration

- Provide one authenticated query returning `feeRate`, `minAmount`, `maxAmount`, `processingStart`, `processingEnd`, and ascending `amounts`.
- Serialize financial values and the fee rate as decimal strings and expose amount values without option IDs.
- Do not expose Setting names or a timezone field.
- Fail with a stable business error when required configuration is missing or invalid rather than substituting defaults.

### D081: Let Customers Query Their Own Withdrawals

- Provide a paginated Mobile list fixed to `appliedAt DESC, id DESC`, with an optional single status filter.
- Derive Customer identity only from the Customer Session and do not accept `customerId` from Mobile.
- Restrict detail lookup to a Withdrawal owned by the authenticated Customer.
- Use stored account snapshots in both responses and never expose payment-password plaintext or hashes.

### D082: Return Full Account Number In Mobile Withdrawal History

- Return `id`, `bankName`, full `accountNo`, `amount`, `fee`, `netAmount`, `status`, and `appliedAt` in the Mobile list.
- Add `holder`, `bankCode`, `rejectReason`, `reviewedAt`, and `paidAt` in detail.
- Do not mask the snapshotted account number, but never expose the internal Admin `remark`.
- Serialize every financial value as a two-decimal string.

### D083: Do Not Persist Withdrawal Admin Actors

- Do not add `reviewedBy`, `paidBy`, `remarkBy`, `remarkAt`, or equivalent Admin actor fields.
- Do not create a Withdrawal action-log table.
- Retain only the confirmed Withdrawal status, timestamps, rejection reason, and current Admin remark.

### D084: Manage Deposit Amount Options As Snapshotted Choices

- Let Admin create, update, physically delete, and query an unpaginated amount-ascending list.
- Require unique positive two-decimal `amount` and non-negative two-decimal `gift`.
- Restrict Mobile to existing options and do not allow free-form Deposit amounts.
- Snapshot `amount` and `gift` into the Deposit at creation so later option changes do not affect it.
- Do not reference the option from Deposit or add option status or logical deletion; allow a later new option to reuse a deleted amount.

### D085: Filter Deposit Choices By Enabled Channel Range

- Keep Deposit Amount Options global and treat a channel as supporting an option when its amount falls inclusively within the channel range.
- Show Mobile only options supported by at least one enabled channel, then show only compatible enabled channels after amount selection.
- Revalidate option existence, channel enabled state, and inclusive range during backend Deposit creation.
- Permit unsupported options to remain Admin-visible but Mobile-hidden, and never mutate options as a side effect of channel-range changes.

### D086: Separate Deposit Channel Display, Endpoint, And Provider Identity

- Use `title` only as the Customer-facing Mobile display name and `name` as the globally unique Admin-facing internal name.
- Use `gateway` as an Admin-configured provider payment endpoint, usually a URL, and not as a strategy selector.
- Use `code` as the provider merchant or channel code and enforce uniqueness on `gateway` plus `code`.
- Treat `secret` as the provider credential paired with `code` and available only to the selected strategy.

### D087: Store Deposit Channel Secret As Plaintext

- Persist `secret` directly in the Deposit Channel table without encryption.
- Do not introduce an encryption component, deployment encryption key, or key-rotation workflow for this field.

### D088: Let Admin Retrieve Deposit Channel Secret

- Allow an authorized Admin API to return the current Deposit Channel `secret` as plaintext.
- Do not replace the value with masking text or a `secretSet` boolean in every Admin read contract.

### D089: Limit Plaintext Channel Secret Exposure

- Omit `secret` from the Admin channel list and every Mobile response, but return plaintext from the authorized single-channel detail/edit query.
- Require a non-empty secret on creation; on update, omit it to preserve the current value or send a non-empty replacement, and reject an empty string.
- Redact the secret from request, exception, initiation, and callback logs despite its permitted Admin detail response.

### D090: Allow Deposit Channel Administration Without Rewriting Transactions

- Let Admin create, update, logically delete, and query an unpaginated Deposit Channel list.
- Keep `code` immutable while allowing updates to `title`, `name`, `gateway`, `secret`, range, and status.
- Allow logical deletion whether or not the channel was previously used.
- Prevent new initiation through a disabled or deleted channel.
- Preserve existing transaction execution and history through its immutable Deposit Channel Snapshot rather than the current channel row.

### D091: Let Deposit Channel Strategies Inspect Transaction Snapshots

- Do not add or persist a separate Deposit Channel Strategy identifier.
- Pass the immutable Deposit Channel Snapshot to registered strategies and let each strategy decide support from `gateway`, `code`, and other snapshot values.
- Use the same support-selection model for payment initiation and callback processing.
- Keep `(gateway, code)` as the configured-channel unique identity.

### D092: Require Exactly One Deposit Channel Strategy Match

- Require exactly one supporting strategy for both initiation and callback processing and never select the first match by registration order.
- Treat zero matches as a stable unsupported-channel business outcome.
- Treat multiple matches as an internal code defect in strategy support predicates, record the internal failure, and refuse processing.
- Do not run strategy matching when Admin creates, updates, or enables a Deposit Channel. Channel administration persists configuration, while strategy compatibility belongs to deployed code and is evaluated only at runtime.
- Preserve rejected callback attempts in the callback log.

### D093: Keep Deposit Gateway Format Strategy-Owned

- Treat `gateway` as a required opaque channel value even though most gateways are expected to be URLs.
- Trim surrounding whitespace, accept 1-2048 characters, and reject control characters.
- Do not globally require HTTP/HTTPS or normalize case, paths, or trailing slashes.
- Let each Deposit Channel Strategy decide whether the gateway format is supported; the simulated strategy may require an HTTP or HTTPS URL.
- Evaluate `(gateway, code)` uniqueness against the trimmed stored gateway value.

### D094: Validate Deposit Channel Identity And Range

- Normalize surrounding and consecutive whitespace in `title` and `name`, accept 1-100 Unicode characters, and reject control characters.
- Keep `title` non-unique and enforce case-insensitive uniqueness for `name`.
- Trim `code`, accept 1-128 characters, reject control characters, preserve case, and impose no fixed format.
- Enforce case-sensitive uniqueness on stored `gateway` plus `code`.
- Require two-decimal NGN values satisfying `0.00 < min <= max`.

### D095: Snapshot Deposit Channel Configuration Per Transaction

- Copy `channelTitle`, `channelName`, `gateway`, `code`, and plaintext `secret` into Payment Transaction when it is created.
- Keep the snapshot immutable and use it for strategy matching, payment initiation, callback verification, and historical display.
- Retain `channelId` only as historical origin metadata; do not require the current channel row for transaction execution.
- Let channel edits, disablement, and logical deletion affect only future transaction creation.
- Do not block `gateway` or `secret` updates because an in-progress transaction exists.

### D096: Release Deposit Channel Unique Keys On Deletion

- During logical deletion, atomically set `del`, rewrite `name` to `{name}_del_{channelId}`, and rewrite `code` to `{code}_del_{channelId}` while leaving `gateway` unchanged.
- Release case-insensitive `name` uniqueness and case-sensitive `(gateway, code)` uniqueness so a later create can reuse the original values.
- Treat the code rewrite as an internal deletion operation and the sole exception to active-channel code immutability.
- Keep existing Payment Transactions unaffected because their snapshots retain the original channel values.

### D097: Persist Payment Records Before Calling The Provider

- Validate the selected amount option and channel, build the channel snapshot, and resolve exactly one initiation strategy before opening a database transaction.
- In a first short transaction, create the `PENDING` Deposit and `IN_PROGRESS` Payment Transaction and commit them before calling the external provider.
- Call the provider outside any database transaction so a slow external request does not hold database locks and a concurrent callback can find the committed transaction.
- Persist a successful initiation result in a second short transaction.
- On initiation failure, use expected-status updates in one short transaction to close the Payment Transaction and cancel the Deposit together.
- If a callback completes the records before initiation-result handling finishes, completion wins; later success persistence or failure cleanup must not reverse the completed state.

### D098: Generate Payment Transaction Business IDs With IdUtils

- Add `IdUtils` as the canonical shared utility for business-ID generation.
- Initially expose only `txnId` generation and format-validation methods; add no unused methods for hypothetical business records.
- Generate `txnId` as `PT` followed by `yyyyMMddHHmmssSSS` and a six-digit random number, producing a 25-character identifier.
- Format the timestamp in UTC, independent of the host machine's timezone.
- Generate the random suffix with `SecureRandom` over the inclusive range `000000` through `999999`, left-padding with zeroes to exactly six digits.
- Generate the identifier before the first Payment Transaction database transaction so the same value can identify the Payment Transaction, Deposit, provider initiation request, and later callback.
- Enforce a database unique index on `txnId`.
- If the first short creation transaction encounters a `txnId` uniqueness collision, generate another ID and retry the entire creation transaction, up to three total attempts and always before calling the provider.
- If all three attempts collide, return a stable business error and do not call the external provider.
- Expose static `boolean isValidTxnId(String txnId)`. Return false rather than throwing for null or invalid input.
- Require exactly 25 ASCII characters, uppercase prefix `PT`, a strictly valid 17-digit `yyyyMMddHHmmssSSS` timestamp segment, and a final six-digit suffix that may be `000000`.
- Do not compare the timestamp to the current clock and do not query persistence; the owning Service checks transaction existence separately.

### D099: Separate Payment Redirect And Callback URLs

- Mobile submits the Payment Redirect URL that the provider uses to return the Customer's browser after payment.
- Trim the Payment Redirect URL, require 1-2048 characters, parse it as an absolute HTTP or HTTPS URL, and reject control characters or user information.
- Match its normalized scheme, host, and effective port exactly against the configured allowlist; never use string-prefix matching.
- Preserve its path, query, and fragment, including the fragment required by Mobile's hash routing.
- In production, allow only HTTPS origins. Development may explicitly configure HTTP loopback origins using `localhost` or `127.0.0.1` and the actual Mobile port.
- Store the allowlist as Setting `PAYMENT_REDIRECT_ORIGINS`, with multiple origin values separated by ASCII commas.
- When saving the setting, split on ASCII commas and trim each item. Require at least one item and reject empty items rather than silently discarding them.
- Require every item to be an origin only: `scheme://host[:port]`, with no path, query, fragment, or user information.
- Canonicalize scheme and host to lowercase, remove the scheme's default port and a trailing slash, then reject any duplicate canonical origin.
- Persist the canonical origins joined by commas without spaces.
- Validate and save the complete setting atomically. If any origin is invalid, reject the update and preserve the previous value.
- Never accept the Payment Callback URL from Mobile or Admin input.
- Build the Payment Callback URL from a typed public callback-base configuration plus the transaction-specific callback path containing `txnId`.
- Persist the generated Payment Callback URL with the Payment Transaction in the first short transaction, before initiating payment.
- Persist Mobile's validated `redirectUrl` on that Payment Transaction in the same transaction.
- Validate the callback base at application startup. Production requires HTTPS; development may explicitly use an HTTP loopback address. Reject user information, query, and fragment components.
- Reject a generated Payment Callback URL longer than 2048 characters.
- Keep the public callback base in typed deployment configuration; it is not a Setting.

### D100: Simulate Provider Initiation Through MockDepositChannelStrategy

- Select `MockDepositChannelStrategy` only for a channel snapshot whose `code` is exactly `MOCK` and whose `gateway` is a valid HTTP or HTTPS URL.
- Use `https://httpbin.org/anything` as the development mock channel gateway.
- POST `txnId`, Deposit Principal, Payment Redirect URL, and Payment Callback URL as JSON to the configured gateway.
- Accept only HTTP 2xx with a valid response `url`, and expose that value as the standard `payUrl`.
- Treat network failure, timeout, non-2xx, and missing or invalid `url` as initiation failures.
- Keep automated tests deterministic by substituting a local HTTP stub for the public endpoint.
- Exercise the simulated provider callback by manually sending a provider-format request to the public callback endpoint; this remains a provider-source completion even though a developer initiated the test request.
- Keep Admin Deposit Completion as a separate Admin-source entry point into the shared completion workflow.

### D101: Let Valid Provider Payment Override Initiation Failure

- Treat `CLOSED/CANCELLED` as the result of payment-initiation failure, not proof that the provider did not accept or settle payment.
- Permit a fully validated provider success callback to complete either `IN_PROGRESS/PENDING` or the `CLOSED/CANCELLED` pair.
- Perform the paired expected-status updates and every Deposit Completion financial effect in one transaction.
- Keep an already completed transaction idempotent and return its existing completion result without repeating financial effects.
- Reject every other mismatched Payment Transaction and Deposit state combination and retain the callback attempt in the diagnostic log.

### D102: Bound Provider Initiation Without Automatic Retry

- Configure a five-second connection timeout and a fifteen-second response timeout through typed backend deployment configuration rather than Settings.
- Do not automatically retry an initiation request after timeout, network failure, or HTTP non-2xx because the provider may already have created an order whose response was lost.
- Close the local initiation according to the confirmed failure flow and return the initiation error to Mobile.
- Allow the Customer to submit another request that creates a new independent Deposit and Payment Transaction.
- Continue to accept a fully validated late success callback for the original transaction under the confirmed provider-payment-wins rule.

### D103: Reuse One Deposit Completion Workflow

- Name the independent Admin financial operation `Admin Deposit Completion` / `人工补单`; never call it a manual callback.
- Replace Payment Transaction `callbackType` with contextual field `source`, which is null before completion and then immutable as `PROVIDER` or `ADMIN`.
- Let provider strategies normalize callback payloads before invoking the shared Deposit Completion method.
- Let Admin Deposit Completion perform its own validation, construct the same standard input, and invoke that same method directly.
- Keep all financial effects, state transitions, timestamps, completion source, transactions, and idempotency inside the shared method.
- Record provider attempts in the Callback Log. Record Admin completion through `source = ADMIN` and its required `reason`; never fabricate a provider callback log for an Admin action.

### D104: Limit Admin Deposit Completion Input

- Address the target by `txnId` in the request path and require only a trimmed 1-500 character Unicode `reason` in the request body.
- Permit the paired states `IN_PROGRESS/PENDING` and `CLOSED/CANCELLED`, without requiring an existing `payUrl`.
- Derive Customer identity, Deposit Principal, Deposit Gift, status, and completion source from existing records; Admin cannot submit or override them.
- Return the existing result for `COMPLETED/PAID` without repeating financial effects.
- Reject every other Payment Transaction and Deposit state combination as inconsistent data.

### D105: Authorize Admin Completion Without Actor Snapshot

- Protect Admin Deposit Completion with RBAC permission and matching button access code `finance:deposit:complete`.
- Persist only the required `reason` with `source = ADMIN` and `completedAt` when Admin completion succeeds.
- Do not add `adminId`, `adminName`, or a separate Admin action log for this operation.
- Keep `reason` null for provider-completed transactions.
- Write source, reason, completion time, and all Deposit Completion financial effects in the same transaction.
- A repeated completion attempt cannot overwrite the original source, reason, or completion time.

### D106: Preserve Admin Completion When A Provider Callback Arrives Later

- Always run a later provider callback through strategy matching, payload parsing, authentication, and amount validation before treating it as a duplicate.
- Retain every callback attempt in the Callback Log, including one received after Admin Deposit Completion.
- When the transaction is already `COMPLETED/PAID` with `source = ADMIN`, do not repeat financial effects or replace its source, reason, or completion time.
- Mark the log outcome as a duplicate success received after Admin Deposit Completion.
- Return the provider-specific success acknowledgment so the provider stops retrying the already completed transaction.

### D107: Authenticate The Mock Provider Callback

- Expose unauthenticated `POST /api/payment/callback/{txnId}` for provider callbacks; Customer and Admin sessions are not required.
- Accept Mock JSON fields `txnId`, `amount`, and `status`, requiring path/body transaction IDs to match, `amount` to be a two-decimal string equal to Deposit Principal, and `status` to be `SUCCESS`.
- Read `X-Mock-Signature` as lowercase hexadecimal HMAC-SHA256 over the exact raw UTF-8 body, keyed by the immutable snapshot `secret`, and compare it in constant time.
- Return HTTP 200 and `success` for valid first and duplicate success callbacks, HTTP 400 for format or amount errors, and HTTP 401 for authentication failure.
- Retain every attempt in the Callback Log, including rejected and duplicate requests.
- Do not add timestamp or nonce replay protection to the Mock protocol because Deposit Completion is idempotent by `txnId`; real strategies may impose their provider-specific replay rules.

### D108: Store One Row Per Provider Callback Attempt

- Remove `callbackLog` from Payment Transaction and create the one-to-many `PaymentCallbackLog` resource.
- Associate each row by `txnId` and append one row for every callback attempt against a known transaction.
- Never update or delete a callback-log row.
- Distinguish first success, duplicate success, malformed payload, amount mismatch, authentication failure, and state conflict as separate attempts.
- Query a transaction's logs for Admin display using `receivedAt DESC, id DESC`.
- Exclude Admin Deposit Completion because it is not a provider callback.

### D109: Preserve Raw Callback Payloads

- Store `txnId`, `payload`, `result`, `message`, `ip`, `httpStatus`, `truncated`, and `receivedAt` on each Payment Callback Log.
- Use result values `COMPLETED`, `DUPLICATE`, `REJECTED`, and `ERROR`.
- Preserve `payload` without redaction so diagnosis retains the provider's submitted content up to the confirmed size limit; accept that provider-sensitive body values will therefore remain in the database.
- Never store request headers, including signature, Authorization, or Cookie values.
- Limit the recorded body to 64 KiB. For a larger body, retain the first 64 KiB, mark it truncated, reject business processing, and respond with HTTP 413.
- Store the resolved Provider IP, the actual returned HTTP status, and a UTC receipt time.
- Keep an optional diagnostic message of at most 500 Unicode characters without stack traces.

### D110: Restrict Raw Callback Log Access

- Protect log access and its Admin button with `finance:deposit:callback-log`.
- Keep callback logs and payloads out of Payment Transaction list and detail responses.
- Query them only through paginated `GET /admin/payment-transactions/{txnId}/callback-logs`, defaulting to 20 rows and limiting a page to 100 rows.
- Apply fixed order `receivedAt DESC, id DESC`.
- Return the full stored raw payload only to an authorized Admin; never expose the resource to Mobile.
- Do not implement Callback Log export in the current scope.

### D111: Make Callback Logging Part Of Completion Reliability

- Insert the first successful callback's `COMPLETED` log in the same transaction as Deposit Completion so either the log and all financial effects commit together or neither does.
- Roll back Deposit Completion and return HTTP 500 if that log insert fails.
- Persist duplicate and rejected outcomes in independent short transactions before returning their normal protocol responses.
- After an unexpected callback-processing rollback, persist an `ERROR` log in an independent short transaction and return HTTP 500.
- Return HTTP 500 when any non-success log insert fails rather than hiding the missing diagnostic record behind a normal 200, 400, or 401 response.
- Insert a final immutable row once; do not create or later update a processing-state log.
- Application error logs may describe log-persistence failure but cannot include the raw callback payload.

### D112: Reject Unknown Callback Transaction IDs Without Database Logs

- Require callback path `txnId` to match the confirmed `PT` plus 23-digit, 25-character business-ID format through the shared `IdUtils` validator.
- Return HTTP 400 for invalid format and HTTP 404 when the format is valid but no Payment Transaction exists.
- Do not create an unassociated Payment Callback Log, preventing random public requests from growing the callback-log table.
- Write only transaction ID, request IP, body byte count, and outcome to the application security log; never include payload, signature, or other headers.
- Limit the every-attempt persistence guarantee to callbacks associated with an existing Payment Transaction.

### D113: Use One Minimal Deposit Completion Input

- Pass only `txnId`, `amount`, `source`, `reason`, and optional `callbackLog` into the shared Deposit Completion method.
- For Provider completion, use the strategy-normalized callback amount, `source = PROVIDER`, null reason, and a required pending Callback Log.
- For Admin completion, use Deposit Principal loaded from the stored Deposit, `source = ADMIN`, required reason, and no Callback Log.
- Require the input amount to equal stored Deposit Principal and never compare or include Deposit Gift.
- Reject invalid source-specific field combinations.
- Do not accept caller-provided Customer identity, gift, reward rates, target states, balance values, or completion timestamps.
- Let the shared method load and lock authoritative records, derive every financial effect, and obtain completion time from the backend UTC clock.

### D114: Pass Initiation URLs Through PaymentTransaction

- Use `DepositInitiateResult initiate(DepositChannel channel, Deposit deposit, PaymentTransaction transaction)` while keeping `supportsInitiate(DepositChannel channel, Deposit deposit)` unchanged.
- Persist Mobile's validated `redirectUrl` beside the backend-generated `callbackUrl` on Payment Transaction in the first short transaction.
- Pass the already committed Payment Transaction as a read-only argument so the strategy can use both URLs without a hidden database query.
- Continue to pass the same pre-transaction Deposit Channel object and committed Deposit as read-only arguments; a strategy cannot modify, persist, or re-query any of the three objects.

### D115: Replay Callback Bodies Through CallbackWrapper

- Name the repeatable Servlet request wrapper `CallbackWrapper`.
- Cache at most 64 KiB of raw request bytes before strategy selection and return a fresh stream or reader for every `getInputStream()` or `getReader()` call.
- Preserve the original request's method, path, query, headers, encoding, and remote address.
- Pass the same wrapper as `HttpServletRequest` to every `supportsCallback` check and the single selected strategy's `callback` method.
- Keep `supportsCallback` side-effect free: it may inspect request data but cannot write a response, mutate state, or access persistence.
- Reuse the cached bytes as the raw Callback Log payload.
- If the body exceeds 64 KiB, invoke no strategy; append a `REJECTED` HTTP 413 log for a known transaction and return HTTP 413.

### D116: Let A Callback Strategy Own Its Provider Response

- Complete callback parsing, authentication, Callback Log persistence, and any Deposit Completion work before committing a Provider success response.
- Let the selected strategy write the Provider-specific status, content type, charset, headers, and body exactly once, including handled rejection and duplicate outcomes.
- Never wrap a callback response in the project's normal `R` JSON contract.
- If an unexpected exception escapes `callback`, write HTTP 500 with plain-text `error` only while the response remains uncommitted.
- If the response is already committed, do not write again; record the unexpected failure in application logs without raw payload.

### D117: Keep Deposit Workflow In DepositService

- Name the Provider interface `DepositChannelStrategy` and inject `List<DepositChannelStrategy>` directly into `DepositService`.
- Do not add a separate pass-through strategy-dispatch Service.
- Let the Mobile initiation entry accept current `customerId` and `DepositInitiateRequest`, then own validation, Deposit and Payment Transaction construction, unique Provider selection, both database transactions, Provider invocation, and result or failure persistence.
- Resolve the unique initiation strategy once before the first transaction, keep the selected Bean in the local workflow, and call it after commit without another support check.
- Let `DepositService` own callback wrapping, unique strategy selection, zero/multiple/match-error Callback Logs, and fallback responses before calling the unique Provider adapter.
- Keep Provider adapters limited to Provider protocol variation: support checks, external initiation, callback parsing and authentication, shared completion invocation, and Provider-specific response writing.
- Keep `DepositCompletionService` as an internal module reused by Provider callbacks and Admin Deposit Completion; do not expose it to Mobile callers.

### D118: Verify Callback Strategy Against The Channel Snapshot

- Treat `supportsCallback(HttpServletRequest)` as request-protocol recognition only; it does not prove that the callback belongs to the referenced Payment Transaction.
- After unique request-shape selection, require the selected `DepositChannelStrategy` to load the Payment Transaction and verify that its immutable channel snapshot belongs to that strategy.
- On mismatch, append a `REJECTED` Callback Log with `message = CHANNEL_STRATEGY_MISMATCH`, return the strategy's Provider-specific HTTP 400 failure response, and perform no authentication-dependent completion or financial effects.
- Do not fall through to another strategy. A request-format match and transaction-channel mismatch is an invalid callback, not an alternate dispatch path.

### D119: Preserve A Late Initiation URL But Return The Completed State

- When a callback completes the Payment Transaction before `initiate()` returns a valid `payUrl`, Deposit Completion remains authoritative and the later initiation result cannot alter any completed state or metadata.
- Persist the valid `payUrl` only when the stored value is null, allowing the Payment Transaction status to be either `IN_PROGRESS` or `COMPLETED`.
- When the second transaction observes `COMPLETED/PAID`, return Mobile success with `txnId`, `status = PAID`, and null `payUrl`; do not navigate the Customer to the Provider after payment has already completed.
- When the second transaction observes `IN_PROGRESS/PENDING`, return `status = PENDING` with the persisted `payUrl`. A callback that commits after that observation is an ordinary race and does not invalidate the response.
- If the stored `payUrl` already equals the returned value, treat the result as consistent. If it contains a different value, record an internal data-conflict error and return a stable internal failure without overwriting either URL.

### D120: Reconcile Initiation Exceptions Through Expected-State Close

- Do not attempt to observe callback state while `DepositChannelStrategy.initiate(...)` is blocked. Reconcile only after the call returns or throws.
- After an initiation exception or timeout, first atomically attempt the paired `IN_PROGRESS/PENDING -> CLOSED/CANCELLED` transition using expected-state conditions.
- If the close succeeds, return the stable initiation failure to Mobile. If it fails, reload both records because another flow changed at least one expected state.
- Return Mobile success with `txnId`, `status = PAID`, and null `payUrl` when the reloaded pair is `COMPLETED/PAID`; Mobile skips Provider navigation and displays the completed result.
- Treat every other reloaded state pair as an internal state conflict. Do not overwrite it or report an inferred Provider outcome.
- Keep Provider-facing `DepositInitiateResult` limited to `payUrl`. The Mobile API response separately contains `txnId`, `status`, and nullable `payUrl`; normal initiation returns `PENDING` with the persisted URL.

### D121: Expose Customer Deposit History To Mobile

- Provide a paginated Deposit list scoped exclusively to the authenticated Customer; never accept a caller-supplied `customerId`.
- Support an optional single-status filter covering `PENDING`, `PAID`, and `CANCELLED`, with omission meaning all statuses. Do not add amount or date filters in the current scope.
- Use fixed order `createTime DESC, id DESC` and the repository's `currentPage` and `pageSize` pagination contract.
- Provide a single-Deposit detail query addressed by `txnId` and constrained by the authenticated Customer's ownership.
- When a Deposit is `PENDING` and its Payment Transaction has a persisted `payUrl`, expose the URL so Mobile can offer Continue Payment. Do not offer that action for `PAID`, `CANCELLED`, or a pending record whose URL is not yet available.
- Use the same Mobile representation for list rows and details: `txnId`, `amount`, `gift`, `credit`, `channelTitle`, `status`, `createTime`, `paidAt`, and nullable `payUrl`.
- Return every amount as a two-fraction-digit decimal string. Return `payUrl` only for `PENDING`; use null for every other status.
- Do not return persistence IDs, `customerId`, `channelId`, `channelName`, or the Payment Transaction status. Mobile presents only the Customer-facing Deposit lifecycle.
- Never expose `gateway`, `code`, `secret`, `callbackUrl`, Callback Logs, or other Provider diagnostics to Mobile.

### D122: Keep Admin Deposit Records Read-Only And Exportable

- Provide a paginated Admin Deposit list and a detail query addressed by `txnId`; aggregate the associated Customer, Deposit, Payment Transaction, and immutable Deposit Channel Snapshot for display.
- Do not allow Admin to edit or delete a Deposit or Payment Transaction. Admin Deposit Completion and permission-gated Callback Log inspection remain separate operations.
- Export every Deposit matching the active Admin filters and ignore pagination while preserving the list's fixed ordering.
- Follow the confirmed Withdrawal Export delivery flow: pre-count rows, return a stable “no export data” business Message for zero rows, reject more than 100,000 rows, query accepted exports in batches, stream `.xlsx` directly in the HTTP response, and retain no generated file.
- Do not upload Deposit exports to S3 or return a storage URL. Do not include `secret`, raw callback payloads, or Callback Logs; Callback Log export remains outside the current scope.

### D123: Keep Admin Deposit Filters Focused

- Apply the same filter object to the paginated Admin list and Deposit Export.
- Support exact `txnId`, exact Customer `dialCode` plus `phone`, one Deposit `status`, one completion `source`, and exact `channelId`.
- Require `dialCode` and `phone` together when filtering by Customer identity; do not interpret either field alone.
- Support independent minimum and maximum `amount` filters against Deposit Principal only; do not apply them to Deposit Gift or Deposit Credit.
- Support independent start and end ranges for `createTime` and `paidAt`. A null `paidAt` does not match an active paid-time range.
- Accept RFC 3339 UTC instants, use start-inclusive and end-exclusive ranges, permit either boundary to be omitted, and require start earlier than end when both are present.
- Fix list and export order to `createTime DESC, id DESC`; do not accept caller-selected sorting.
- Do not add nickname or channel-title fuzzy search, gift/credit ranges, or other filters in the current scope.

### D124: Keep The Admin Deposit List Operationally Focused

- Return `txnId`, `customerId`, `dialCode`, `phone`, `nickname`, `amount`, `gift`, `credit`, `channelId`, `channelTitle`, `channelName`, `status`, `source`, `createTime`, and `paidAt` for each Admin Deposit list row.
- Use Deposit `status` as the single Customer-facing lifecycle status and do not duplicate the derivable Payment Transaction status in the list.
- Return null `source` before completion and serialize every API amount as a two-fraction-digit decimal string.
- Keep `customerId` and `channelId` so Admin can navigate to their corresponding details.
- Do not return `payUrl`, `gateway`, `code`, `secret`, `redirectUrl`, `callbackUrl`, Admin completion `reason`, or Callback Logs in the list.

### D125: Add Payment Diagnostics Only To Admin Deposit Detail

- Return every Admin list field plus `txnStatus`, `gateway`, `code`, `payUrl`, `redirectUrl`, `callbackUrl`, and `reason` from the Deposit detail query.
- Use `txnStatus` for the Payment Transaction's actual state and keep `status` for the Customer-facing Deposit state.
- Return `reason` only when `source = ADMIN`; otherwise use null.
- Use Deposit `paidAt` as the API completion timestamp and do not duplicate Payment Transaction `completedAt` in this representation.
- Keep Callback Logs behind their existing independent endpoint and permission.
- Never return the Payment Transaction snapshot `secret` from Deposit detail. It remains backend-only execution data even though Deposit Channel management detail may return the current channel's plaintext `secret` under its separately authorized workflow.

### D126: Fix Deposit Export Columns And Cell Types

- Export these ordered columns: Transaction ID, Customer ID, Dial Code, Phone, Nickname, Principal, Gift, Credit, Currency, Channel ID, Channel Title, Channel Name, Channel Code, Deposit Status, Transaction Status, Completion Source, Admin Completion Reason, Created At, and Paid At.
- Use `Dial Code` as the concise export label for Customer `dialCode`; current values are `+1` and `+234`.
- Write Transaction ID, Customer ID, Dial Code, Phone, Channel ID, and Channel Code as text so identifiers and leading zeroes remain intact.
- Write Principal, Gift, and Credit as numeric cells displayed with two decimal places, and derive Currency as constant `NGN`.
- Write stable uppercase enum values for both statuses and Completion Source. Leave null source, reason, and paid time as blank cells.
- Write timestamps as UTC ISO text.
- Do not export `gateway`, `secret`, `payUrl`, `redirectUrl`, `callbackUrl`, raw callback payloads, or Callback Logs.

### D127: Separate Deposit Page Access From Sensitive Actions

- Control Admin Deposit list and detail through the page menu grant and their backend endpoint permissions; do not create list or detail Button Menu access codes.
- Use `finance:deposit:export` for the Deposit Export button.
- Continue to use `finance:deposit:complete` for Admin Deposit Completion and `finance:deposit:callback-log` for Callback Log access.
- Do not add edit or delete access codes because Deposit and Payment Transaction records are read-only.

### D128: Reference Each Ledger Entry By Its Originating Business Record

- Add non-null string field `refId` to Customer Balance Ledger and let ledger `type` determine the referenced resource; do not add a redundant `refType`.
- Map Deposit Principal, Deposit Gift, Referral Level Reward, and Referral Tier Reward entries to Payment Transaction `txnId`.
- Map Withdrawal Hold and Withdrawal Release to Customer Withdrawal ID, and Registration Gift to Customer ID.
- Reserve `PENALTY` and `WORK_EARNING` for their corresponding business-record IDs, but add no producer while those modules remain undefined.
- Use the complete ledger type set `DEPOSIT`, `DEPOSIT_GIFT`, `WITHDRAWAL_HOLD`, `WITHDRAWAL_RELEASE`, `PENALTY`, `REGISTRATION_GIFT`, `WORK_EARNING`, `REFERRAL_LEVEL_REWARD`, and `REFERRAL_TIER_REWARD`; do not retain an ambiguous generic `WITHDRAWAL` type.
- Enforce unique `(customerId, type, refId)` to prevent duplicate same-type effects from one business source while allowing different ledger types to reference the same source.

### D129: Persist Ledger Direction

- Store `direction` on every Customer Balance Ledger row as `INCOME` or `EXPENSE`; do not treat it as presentation-only derived data.
- Require business code to keep `direction` consistent with `type`. A contradictory pair is a code defect, not a supported business state.
- Map `DEPOSIT`, `DEPOSIT_GIFT`, `WITHDRAWAL_RELEASE`, `REGISTRATION_GIFT`, `WORK_EARNING`, `REFERRAL_LEVEL_REWARD`, and `REFERRAL_TIER_REWARD` to `INCOME`.
- Map `WITHDRAWAL_HOLD` and `PENALTY` to `EXPENSE`.
- Require income rows to satisfy `after = before + amount` and expense rows to satisfy `after = before - amount`.

### D130: Keep Ledger Type-Direction Validation In Business Code

- Do not add a database `CHECK` constraint for the Customer Balance Ledger `type` and `direction` mapping.
- Treat an inconsistent pair as an application-code defect and require the owning balance workflow to validate the pair before persistence.
- The database persists the supplied canonical values and does not provide a fallback for this code error.

### D131: Keep Balance Validation Outside The Ledger Module

- Require every ledger `amount` to be a positive two-fraction-digit value. Never encode expense as a negative amount, and create no ledger row for a zero movement.
- Require non-negative two-fraction-digit `before` and `after` values, with income represented as `after = before + amount` and expense as `after = before - amount`.
- Treat these as the data contract supplied by the calling business workflow rather than balance rules evaluated by the Ledger module.
- Let the Deposit, Withdrawal, Registration, Referral Reward, Penalty, or Work workflow own Customer locking, sufficient-balance checks, before/after calculation, and Customer balance mutation.
- Let the Ledger module only append the fully supplied entry inside the caller's existing transaction; it does not load or update Customer balance and does not open an independent transaction.
- Do not add database `CHECK` constraints for these application-owned rules.

### D132: Derive Ledger Titles From Type

- Do not persist or return a free-form `title` field on Customer Balance Ledger.
- Treat ledger `type` as the canonical title identity and let Mobile and Admin map it through their own localization resources.
- Changing a Customer's locale changes the displayed ledger title; do not freeze one locale's rendered text in a ledger row.
- Keep nullable `remark` only for entry-specific supplemental information that cannot be derived from `type` or `refId`.

### D133: Keep Ledger Remarks Optional And Immutable

- Store nullable `remark` as entry-specific supplemental text supplied by the business workflow that creates the ledger entry.
- Trim the value, accept at most 500 Unicode characters, reject control characters, and persist an empty normalized result as null.
- Do not accept `remark` from Customer requests, and do not provide any update operation after the ledger row is created.
- Let Admin return the complete stored value; never expose ledger `remark` to Mobile.
- Keep `remark` null in current automatic workflows when every relevant fact is already available from `type` and `refId`; do not duplicate a localized title or source identity in it.

### D134: Expose Only The Customer's Own Ledger To Mobile

- Provide a paginated Mobile Customer Balance Ledger list scoped exclusively to the authenticated Customer; never accept a caller-supplied `customerId`.
- Support optional `direction` and one optional ledger `type`. Do not add amount or time filters in the current scope.
- Use fixed order `createTime DESC, id DESC` and the repository's `currentPage` and `pageSize` pagination contract.
- Do not provide a separate ledger detail endpoint because each immutable list row carries the complete Mobile representation.
- Do not provide Customer ledger create, update, or delete operations.

### D135: Keep The Mobile Ledger Representation Minimal

- Return only `type`, `direction`, `amount`, `before`, `after`, and `createTime` for each Mobile ledger row.
- Serialize `amount`, `before`, and `after` as two-fraction-digit decimal strings and let Mobile localize the title from `type`.
- Do not return Ledger database ID, `customerId`, `title`, `remark`, or `refId`.
- Keep `remark` Admin-only. Keep `refId` private from Mobile because a Referral Reward entry references another Customer's triggering Payment Transaction.

### D136: Keep Admin Ledger Filters Focused

- Apply the same filter object to the paginated Admin Customer Balance Ledger list and Ledger Export.
- Support exact Customer `dialCode` plus `phone`, one `direction`, one ledger `type`, and exact `refId`.
- Require `dialCode` and `phone` together when filtering by Customer identity; do not interpret either field alone.
- Support independent minimum and maximum `amount` filters.
- Support independent start and end ranges for `createTime`, accepting RFC 3339 UTC instants with start-inclusive and end-exclusive semantics. Permit either boundary to be omitted and require start earlier than end when both are present.
- Fix list and export order to `createTime DESC, id DESC`; do not accept caller-selected sorting.
- Do not add `before` or `after` ranges, remark search, or other filters in the current scope.
- Keep Admin Ledger operations read-only: query and export only, with no create, update, or delete workflow.

### D137: Return Complete Ledger Rows To Admin

- Return `id`, `customerId`, `dialCode`, `phone`, `nickname`, `type`, `direction`, `amount`, `before`, `after`, `refId`, `remark`, and `createTime` for each Admin Ledger list row.
- Use `id` as the stable Admin table-row identity and keep `customerId` for navigation to Customer detail.
- Serialize `amount`, `before`, and `after` as two-fraction-digit decimal strings.
- Return the complete `refId` and `remark` to Admin.
- Do not return `title`; Admin localizes the display title from `type`.
- Do not provide a separate Admin Ledger detail endpoint because the list representation already contains every Admin-visible field.

### D138: Stream Filtered Ledger Exports Directly

- Export these ordered columns: Ledger ID, Customer ID, Dial Code, Phone, Nickname, Type, Direction, Amount, Before Balance, After Balance, Currency, Reference ID, Remark, and Created At.
- Write Ledger ID, Customer ID, Dial Code, Phone, and Reference ID as text.
- Write Amount, Before Balance, and After Balance as numeric cells displayed with two decimal places, and derive Currency as constant `NGN`.
- Write stable uppercase enum values for Type and Direction, and write Created At as UTC ISO text.
- Apply the active Admin Ledger filters and fixed ordering while ignoring pagination.
- Follow the confirmed direct-export flow: pre-count rows, return a stable “no export data” business Message for zero rows, reject more than 100,000 rows, query accepted exports in batches, stream `.xlsx` directly in the HTTP response, and retain no generated file.
- Do not upload Ledger exports to S3 or return a storage URL.

### D139: Separate Ledger Page Access From Export

- Control the Admin Ledger list through its page menu grant and backend list-endpoint permission; do not create a list Button Menu access code.
- Use `finance:ledger:export` for the Ledger Export button.
- Do not add detail, create, update, or delete access codes because those Ledger operations do not exist.

### D140: Define Setting Types In Code

- Keep Setting persistence as unique uppercase `name` plus string `value`; do not add a database type column.
- Define each supported setting once as a typed `SettingKey<T>` under centralized `SettingKeys`, including its name, parser, canonical formatter, and validator.
- Require business code to call typed `settingService.get(SettingKeys.SOME_KEY)`; do not expose raw-name lookup or repeated value parsing to callers.
- Maintain one registry of the supported keys for Admin lookup and reject every unknown setting name. Admin cannot create arbitrary keys.
- Normalize and validate an Admin-supplied raw value through its registered key before persistence.
- Keep cross-setting and database-dependent validation in the owning domain Service and update the affected settings in one transaction.
- Return stable missing-setting and invalid-setting business errors; do not silently substitute fallback values at read time.

### D141: Register Only The Current Setting Keys

- Register exactly these ten current keys: `REGISTRATION_GIFT_AMOUNT`, `WITHDRAWAL_FEE_RATE`, `WITHDRAWAL_PROCESSING_START_TIME`, `WITHDRAWAL_PROCESSING_END_TIME`, `WITHDRAWAL_MIN_AMOUNT`, `WITHDRAWAL_MAX_AMOUNT`, `PAYMENT_REDIRECT_ORIGINS`, `REFERRAL_LEVEL_REWARD_L1_RATE`, `REFERRAL_LEVEL_REWARD_L2_RATE`, and `REFERRAL_LEVEL_REWARD_L3_RATE`.
- Use `REGISTRATION_GIFT_AMOUNT` as the canonical registration-gift setting name.
- Declare amount and percentage-rate values as `BigDecimal`, processing times as `LocalTime`, and redirect origins as normalized `List<URI>`.
- Do not create unused placeholder keys. A future setting requires both a new typed `SettingKey<T>` and registry entry.

### D142: Default Registration Gift To Zero

- Initialize `REGISTRATION_GIFT_AMOUNT` as canonical string `0.00`.
- Let registration complete normally when the value is zero, without changing Customer balance or creating a `REGISTRATION_GIFT` Ledger entry.
- Allow Admin to set a positive value later through the validated Setting workflow.

### D143: Default Withdrawal Fee Rate To Zero

- Initialize `WITHDRAWAL_FEE_RATE` as canonical percentage string `0.00`.
- At the default rate, calculate `fee = 0.00` and `netAmount = amount`.
- Allow Admin to update the rate within the confirmed range from `0.00` inclusive to `100.00` exclusive.

### D144: Seed Referral Level Reward Rates

- Initialize `REFERRAL_LEVEL_REWARD_L1_RATE` to `10.00`, `REFERRAL_LEVEL_REWARD_L2_RATE` to `2.00`, and `REFERRAL_LEVEL_REWARD_L3_RATE` to `1.00`.
- Interpret each value as a percentage of Deposit Principal, so `10.00` means ten percent rather than `0.10%`.

### D145: Update Referral Level Reward Rates Atomically

- Allow each Referral Level Reward rate independently from `0.00` through `100.00`, inclusive.
- Do not require L1, L2, and L3 rates to be descending, and do not cap their sum at `100.00`; each reward is an independent platform-funded credit.
- Require Admin to submit all three rates together and update them in one transaction so a partial rate set cannot be committed.

### D146: Seed The Withdrawal Range

- Initialize `WITHDRAWAL_MIN_AMOUNT` to `1000.00` NGN and `WITHDRAWAL_MAX_AMOUNT` to `1000000.00` NGN.
- Continue to require every Withdrawal Amount Option to remain within the configured range and reject a range reduction while conflicting options exist.

### D147: Require Deployment-Specific Payment Redirect Origins

- Do not insert `PAYMENT_REDIRECT_ORIGINS` through shared initialization SQL because the repository has no environment-specific SQL initialization and no one Origin is valid for every deployment.
- Keep the registered key visible in Admin even while its row is missing, and let its first validated Admin save create that known setting. This is not arbitrary-key creation.
- Let development and automated-test fixtures explicitly use `http://localhost:6088`, matching Mobile `.env.development` at the time of this decision.
- Require production deployment to configure the actual Mobile HTTPS Origin; never use the reference site's origin as a default.
- Do not fail application startup when the setting is missing. Reject only Deposit initiation with a stable “payment redirect origins not configured” business error; unrelated workflows remain available.

### D148: Administer Settings By Domain Group

- Do not expose generic Setting key/value CRUD to Admin. Keep generic `SettingService` as an internal backend capability only.
- Provide separate Registration, Withdrawal, Payment, and Referral Level Reward configuration sections with their own query and save APIs.
- Let Registration expose `giftAmount`; Withdrawal expose `feeRate`, `processingStart`, `processingEnd`, `minAmount`, and `maxAmount`; Payment expose `redirectOrigins`; and Referral Level Reward expose `l1Rate`, `l2Rate`, and `l3Rate`.
- Do not accept setting names from Admin requests, support setting deletion, or allow arbitrary-key creation.
- Update all five Withdrawal values together in one transaction, including its cross-setting and Withdrawal Amount Option validation.
- Update all three Referral Level Reward rates together in one transaction.
- Let the Payment configuration's first validated save create its registered missing `PAYMENT_REDIRECT_ORIGINS` row.

### D149: Use One Monetary Precision Across The Domain

- Store every NGN amount as `DECIMAL(19,2)` and represent it in Java with `BigDecimal`.
- Apply this precision to Customer balance and cumulative amounts, Deposit, Withdrawal, Customer Balance Ledger, amount options, rewards, and monetary Setting values.
- Support values through `99999999999999999.99` and reject larger values at the owning validation boundary.
- Reject Customer or Admin monetary input with more than two fractional digits; do not silently round submitted values.
- Use the confirmed `HALF_UP` rule only when an arithmetic result must be reduced to two fractional digits.
- Keep percentage and time values under their own precision and format rules.

### D150: Use One Percentage Precision Across The Domain

- Represent every percentage in Java with `BigDecimal` and exactly two fractional digits; for example, `13.00` means thirteen percent.
- Store percentage table columns as `DECIMAL(5,2)` and percentage Setting values as canonical two-fraction-digit strings.
- Reject Admin percentage input with more than two fractional digits instead of rounding it.
- Return percentage API values as two-fraction-digit decimal strings.
- Calculate a monetary result as `amount * rate / 100`, then apply `HALF_UP` once when reducing the result to the domain's two-fraction-digit monetary scale.
- Keep each percentage's permitted range and cross-rate relationships under its owning business rule.

### D151: Validate Processing Times Independently

- Parse `WITHDRAWAL_PROCESSING_START_TIME` and `WITHDRAWAL_PROCESSING_END_TIME` strictly as 24-hour `HH:mm` values.
- Do not enforce `processingStart < processingEnd`, reject equal values, or infer same-day versus cross-midnight semantics.
- Persist and return each valid value as configured because the pair is informational only and controls no submission or processing behavior.
- Continue to update both values within the atomic Withdrawal configuration save.

### D152: Read Settings Directly Without Cache

- Do not cache Settings in Redis or process-local memory in the current scope.
- Batch-query all keys required by one workflow and parse each value once for reuse within that workflow or transaction; do not issue one query per key.
- Let a committed Admin save become visible to every subsequent read immediately, without invalidation events or a stale-value interval.
- Keep the database as the single authoritative setting source.

### D153: Audit Admin Setting Changes In SettingLog

- Use canonical entity names `Setting` for the configuration table and `SettingLog` for its append-only Admin change history. Do not introduce alternate entity names.
- Keep `SettingLog` domain fields limited to `name`, `oldValue`, `newValue`, and `adminId`; use the repository's standard `createTime` as the change timestamp and do not add `changedAt` or `adminName`.
- Insert one log for each value actually changed by Admin in the same transaction as its Setting update. Use one shared `createTime` instant for every changed key in one grouped save.
- Use null `oldValue` for the first Admin creation of registered key `PAYMENT_REDIRECT_ORIGINS`.
- Do not create logs for initialization SQL or unchanged submitted values.
- Never update or delete a SettingLog row. Store complete old and new values because the fixed registry contains no secrets.

### D154: Query SettingLog Without Mutating Or Exporting It

- Provide a paginated Admin SettingLog list with exact `name`, exact `adminId`, and independent `createTime` start/end filters.
- Accept RFC 3339 UTC instants with start-inclusive and end-exclusive semantics, permit either boundary to be omitted, and require start earlier than end when both are present.
- Use fixed order `createTime DESC, id DESC` and return complete `oldValue` and `newValue` values.
- Do not provide a separate detail endpoint, spreadsheet export, update, or deletion.

### D155: Preserve Passive Referral Rewards For Disabled Customers

- Treat disabled Customer Account Status as an access restriction, not as forfeiture of existing passive financial rights.
- During Deposit Completion, calculate and credit every otherwise eligible Referral Level Reward and Referral Tier Reward even when the recipient Customer is disabled.
- When a disabled Customer's direct referral completes a first Deposit, increment the Customer's `qualifiedCount` and apply any resulting automatic tier upgrade under the same rules as for an enabled Customer.
- Do not alter or bypass existing Referral Relationships because of Customer Account Status.
- A future requirement to freeze Customer funds or rewards must introduce an explicit financial restriction rather than overloading Customer Account Status.

### D156: Credit Platform-Funded Rewards Without A Funding Account

- Do not introduce a separate platform balance, funding account, reserve, or insufficient-platform-funds branch for Referral Rewards.
- Credit each Referral Level Reward and Referral Tier Reward directly as platform-funded Customer balance income during Deposit Completion.
- If any reward balance update or ledger insert fails technically, roll back the entire Deposit Completion transaction; never skip only the failed reward or complete the Deposit partially.

### D157: Accumulate Only Paid Deposit Principal

- Define Customer `depositTotal` as the cumulative Deposit Principal of all paid Deposits owned by that Customer.
- Increment `depositTotal` by the stored Deposit Principal within the same atomic Deposit Completion transaction.
- Exclude Deposit Gift, Registration Gift, Referral Rewards, and every other balance movement from `depositTotal`.
- Never decrease `depositTotal` in the current scope because a completed Deposit is final and no refund, reversal, or post-completion adjustment exists.

### D158: Resolve Reward Rates At Deposit Completion

- Do not snapshot Referral Level Reward rates or Referral Reward Tier rates when creating a Deposit or Payment Transaction.
- Within Deposit Completion, read the latest committed L1-L3 Setting rates and managed-tier rates once and reuse that consistent value set throughout the transaction.
- Apply an Admin rate update to every Deposit whose completion transaction reads after that update commits, including Deposits created before the update.
- Continue to use the Deposit Gift snapshotted from the Deposit Amount Option at Deposit creation; it does not follow this reward-rate rule.

### D159: Close Stale Pending Deposit Initiations

- Run a recovery task every two hours for stale pending Deposit initiations.
- Select every Payment Transaction created more than 40 minutes before the scan whose paired states remain exactly `IN_PROGRESS/PENDING`.
- Do not use `payUrl` presence or absence as a selection condition; a stale pending transaction with a saved URL is also closed.
- Use expected-state database conditions to atomically change the pair to `CLOSED/CANCELLED`; a concurrent state change makes that candidate a no-op.
- Never call `DepositChannelStrategy.initiate(...)` again for a stale transaction because the Provider may already have created an order.
- Continue to permit a later validated Provider success callback to complete the recovered `CLOSED/CANCELLED` pair.
- Do not expose retry for the same closed Deposit to Mobile. A Customer starts a new Deposit for a new payment attempt.

### D160: Limit Customer Profile Editing To Profile Data

- Allow a Customer to edit only `nickname`, `avatar`, and optional `email` through the Profile update capability.
- Keep Customer Phone Identity and Invitation Code immutable through Profile, and do not expose Referral Relationships, Referral Reward Tier, Customer Account Status, financial balances, or cumulative values as Profile-editable fields.
- Keep login-password change in a separate workflow rather than accepting password fields in Profile update.
- Require trimmed nonblank `nickname` with at most 255 Unicode code points.
- Store optional `avatar` as a Managed Image Object Key with at most 500 Unicode code points; trim it and let null clear the avatar.
- Store optional `email` with at most 255 Unicode code points; trim it, let null clear it, validate syntax when present, allow duplicates, and require no verification code.

### D161: Change Customer Password Only From An Authenticated Session

- Provide Customer self-service password change with only `oldPassword` and `newPassword` inputs from an authenticated Customer Session.
- Require `oldPassword` to match the current BCrypt hash and `newPassword` to satisfy the common Customer Password Rule.
- Do not provide a forgotten-password, SMS recovery, email recovery, or unauthenticated reset workflow.
- Permit `newPassword` to equal `oldPassword`; encode and persist a new BCrypt hash after successful verification.
- After commit, revoke all sessions belonging to the Customer, including the calling session, and require Mobile to return to login.

### D162: Let Admin Replace A Customer Login Password

- Provide a dedicated Admin operation accepting only `customerId` and `newPassword`; do not require or accept the Customer's old password.
- Validate `newPassword` with the same Customer Password Rule and persist only a newly generated BCrypt hash.
- Never expose the existing Customer password or its hash to Admin through lists, details, forms, or API responses.
- After the password update commits, revoke every session belonging to that Customer.
- Do not add `reason`, a dedicated password-change log table, or password-specific audit fields; use the Admin system's existing permission enforcement and operation logging.

### D163: Use Customer Status Only For Access Control

- Let Admin change a Customer only between `ENABLED` and `DISABLED`; do not support deleting a Customer identity.
- After a disable operation commits, revoke every session belonging to that Customer. Reject new login, token refresh, and every authenticated Customer-initiated business request while status remains disabled.
- Do not cancel or mutate existing Deposits or Withdrawals because of disablement, and continue Provider callbacks and Admin processing under their existing rules.
- Continue passive Referral Rewards, Qualified Direct Referral counting, and automatic Referral Reward Tier Progression while the Customer is disabled.
- Re-enabling does not create or restore a session; the Customer must log in again.
- Do not add a disable reason or dedicated Customer status-history table; use existing Admin permission enforcement and operation logging.

### D164: Keep Strategy Matching Out Of Channel Administration

- Treat Deposit Channel as configuration consumed by deployed Deposit Channel Strategy code, not as a business record that can prove strategy compatibility.
- Do not call `supportsInitiate(...)` or inspect registered strategy Beans when Admin creates, updates, or enables a channel, including when an enabled channel's `gateway` changes.
- Permit Admin to save an enabled channel even while the currently deployed code has zero or multiple matching strategies.
- Continue to require exactly one match during each actual Deposit initiation. Return the confirmed unsupported-channel result for zero matches and the confirmed internal code-defect result for multiple matches before creating Deposit records.
- Continue to perform callback strategy matching and diagnostic logging only when an actual callback request arrives.

### D165: Keep Admin Customer Management Read-Mostly

- Provide a paginated Admin Customer list and a Customer detail query.
- Do not let Admin create or delete Customer identities and do not expose a generic Customer edit operation.
- Keep Admin mutations limited to the separately confirmed login-password replacement, enabled/disabled status change, manual Referral Reward Tier change, and automatic-tier enablement change.
- Do not let Admin edit Customer Phone Identity, Invitation Code, Referrer, Customer Profile, balances, or cumulative values.

### D166: Preserve External References Inside The Repository

- An external website or source file may inform the specification, but an absolute local path, temporary browser session, or external asset host cannot be the only source of a requirement.
- Record exact public page URLs and the reusable conclusions from reference-site inspection in repository documentation.
- Preserve any required reference-code behavior as a repository-owned excerpt or self-contained algorithm, and make the confirmed adaptation explicit when the source differs from Novum.
- Keep reusable visual tokens, manifests, and selected source assets under repository paths; application runtime code copies owned assets into its own module rather than importing from documentation.
- Exclude credentials and authenticated account data from portable artifacts.

### D167: Keep Five Primary Mobile Tabs And Make Fund Functional

- Retain `Home`, `Market`, `Team`, `Fund`, and `My` as the five primary Mobile tabs.
- Put invitation, team, and Referral Reward Tier concerns under `Team`; put Customer finance concerns under `Fund`; and put profile, Withdrawal Account, credential, language, and session concerns under `My`.
- Keep login, registration, detail, and submission screens outside the primary tab set.
- Do not redefine `Home` or `Market` work-business behavior in this scope.
- Implement `Fund` as a real finance workspace instead of reproducing the reference site's unopened placeholder.

### D168: Keep Protected Tabs Visible And Prompt For Login

- Allow public access to `Home` and `Market`; require an authenticated Customer Session for `Team`, `Fund`, and `My`.
- Keep every primary tab visible while unauthenticated and use the existing route-derived global login prompt when a protected route is selected or opened directly.
- Do not expose protected business data before authentication.
- Carry the intended full route through Login and return to it after successful authentication.
- Return to `Home` when the Customer cancels the login prompt.
- Keep static routes and do not add an authentication router guard.

### D169: Keep Public Authentication To Login And Registration

- Provide separate public `Login` and `Registration` secondary routes without the primary tab bar, and link them to each other.
- Do not add forgotten-password recovery, SMS or graphical verification, a standalone privacy-agreement page, or an agreement checkbox in the current scope.
- Keep authenticated Customer password change under `My`.
- Return from an authentication page to its valid public source route, or to `Home` when no such source exists.

### D170: Use A Compact Team Route Hierarchy

- Keep invitation actions, team totals, Referral Reward Tier progression, and L1/L2/L3 summaries on `Team Overview`.
- Use one level-parameterized `Team Members` secondary route for all three relationship levels.
- Use one `Referral Tier Details` secondary route for the four tiers and confirmed progression rules.
- Do not create an invitation-poster or image-generation route.
- Hide the primary tab bar on Team secondary routes and return from them to `Team Overview`.

### D171: Separate Fund Workflows And Histories

- Use `Fund Overview` for available balance and entry points to the finance workflows and records.
- Provide separate Deposit initiation, history, and detail routes; address Deposit Details by `txnId` and apply the confirmed continue-payment rule there.
- Provide separate Withdrawal submission, history, and detail routes using the confirmed Customer-visible Withdrawal data.
- Keep the Customer Balance Ledger as its own route rather than merging it with either business-record history.
- Do not provide Customer-facing Payment Transaction, Callback Log, or payment-diagnostics routes.
- Hide the primary tab bar on Fund secondary routes and return from them to `Fund Overview`.

### D172: Keep My Account Tasks In A Shallow Hierarchy

- Use `My Overview` for Customer summary, Profile, Withdrawal Account, login-password, language, and logout entry points.
- Provide secondary routes for Profile editing, Withdrawal Account list, creation, editing, per-account payment-password change, and Customer login-password change.
- Keep language selection, Withdrawal Account deletion confirmation, and logout confirmation as in-place interactions rather than dedicated routes.
- Hide the primary tab bar on My secondary routes and return each route to its direct parent.
- Build six-digit payment-password entry with the installed Vant `PasswordInput` and `NumberKeyboard` components, following the recorded public documentation URLs, and never expose the value as ordinary visible text.

### D173: Keep Login Focused On Customer Phone Identity

- Show back and language actions, default the Country Calling Code to `+234`, and limit selection to `+1` and `+234`.
- Accept a normalized national phone and masked password, with backend phone validation authoritative.
- Provide only the primary login and Registration navigation actions; omit remember-me, recovery, verification, and third-party-login controls.
- Lock duplicate submission, keep invalid-credentials errors non-enumerating, and distinguish disabled Customer status through a stable business error.
- Navigate to a valid internal redirect target after success, or to `Home` when none exists.
- Allow `Login` to remain usable while a Customer Session already exists rather than redirecting an authenticated Customer away from the page.

### D174: Let A Successful Login Replace Only The Current Local Session

- Treat valid credentials for another Customer entered while authenticated as an allowed account switch in the current browser.
- On success, replace the locally held tokens and Customer information with the newly returned Customer Session, whether the identity is the same or different.
- Do not implicitly revoke the previously held server-side Customer Session; leave it to the existing expiry and explicit-revocation rules.
- On failure, preserve the current local Customer Session and Customer information without change.

### D175: Keep Registration Limited To Identity, Credential, And Invitation

- Collect only Country Calling Code, national phone, password, password confirmation, and required Invitation Code.
- Apply the confirmed phone normalization and password validation in Mobile while keeping backend validation authoritative; keep confirmation out of the backend domain input.
- Preserve six-digit Invitation Code leading zeroes, reject `000000`, and allow a value prefilled from an invitation link to be edited before submission.
- Let the backend default nickname from national phone and defer avatar and email to Customer Profile.
- Lock duplicate submission and distinguish duplicate-phone from invalid-invitation business errors.

### D176: Log In Through The Standard API After Registration

- Keep Registration and Login APIs separate. After Registration commits, Mobile calls the standard Login API with the newly registered credentials.
- Carry a valid internal redirect target through Login and Registration, then enter it after automatic Login; otherwise enter `Home`.
- If automatic Login fails, do not roll back Registration. Open Login with only Country Calling Code and national phone prefilled and present the registration-succeeded/login-required result.
- Preserve a pre-existing local Customer Session on automatic-Login failure, and replace it under D174 on success.
- Do not create a Registration-success route.

### D177: Make Team Overview The Referral Summary

- Show the current Referral Reward Tier, latest rate, automatic-progression state, and next-tier progress, with a terminal highest-tier state for diamond.
- Keep direct registrations, Qualified Direct Referrals, total L1-L3 team size, and each relationship-level count visibly distinct.
- Open the shared Team Members route from each L1/L2/L3 count.
- Keep Invitation Code and invitation-link copy actions on the page and return localized copy outcomes.
- Generate the link through the current Mobile Origin and router-resolved Registration route using `inviteCode`, preserving the deployed base and history mode.
- Link to Referral Tier Details, keep automatic progression read-only, and exclude balance, reward totals, and work-business values.

### D178: Minimize Team Member Disclosure

- Use one paginated L1/L2/L3 Team Members list ordered by registration time and ID descending.
- Return and display only avatar, masked nickname, masked phone, registration time, and L1-only qualification state; never return raw nickname or full phone to this query.
- Keep Qualified Direct Referral status exclusive to L1.
- Provide infinite-loading states without search, filtering, member details, financial values, Referral Reward Tier, or Customer Account Status.

### D179: Mask Team Nicknames On The Backend

- Count nickname length in Unicode code points.
- Return `*` for one code point and `**` for two code points; for three or more, return the first code point, exactly `***`, and the final code point.
- Apply the same rule when the stored nickname is the default numeric national phone.
- Make the backend response authoritative and let Mobile display it without remasking.

### D180: Show Current Tier Configuration And Confirmed Rules

- Present all four Referral Reward Tiers in ascending order, with fixed regular values and current backend-managed values for gold, platinum, and diamond.
- Show reached/current/unreached state and next-tier progress, ending that progression display at diamond.
- Explain Qualified Direct Referral progression, no automatic downgrade, L1-only Referral Tier Reward, every-Deposit application, and stacking with the L1 Referral Level Reward.
- Display automatic progression as read-only and exclude the reference site's external-group, screenshot, manual-application, and inactivity-downgrade rules.

### D181: Keep Fund Overview Focused On Available Funds

- Lead with NGN available balance and show only cumulative paid Deposit Principal and cumulative `PAID` Withdrawal amount as supporting totals.
- Provide Deposit, Withdrawal, Deposit History, Withdrawal History, and Customer Balance Ledger entry points.
- Refresh through `GET /customer/user-info` on entry and pull-to-refresh so the existing online-presence rule remains authoritative.
- Do not invent frozen balance, mix in work or reward totals, or duplicate recent-record lists on the overview.

### D182: Require Explicit Deposit Amount And Channel Selection

- Show only amount options supported by an enabled channel, ordered by amount, and never allow free-form amount input.
- Show a positive Deposit Gift on its option and show the selected Principal, Gift, and expected Credit summary.
- Require explicit amount and channel choices with no defaults, including when only one channel is available.
- Filter channels by the selected amount, expose only Customer-facing titles, and clear an ineligible channel after an amount change.
- Disable initiation until both selections exist and distinguish empty amounts, empty channels, and catalog-load failure.

### D183: Generate The Payment Redirect And Navigate In Place

- Generate the Payment Redirect URL from the current Mobile Origin and router-resolved Deposit History route; never ask the Customer to enter it.
- Submit only amount, channel ID, and redirect URL, and lock duplicate taps without introducing an idempotency key.
- Navigate the current window immediately for a pending result with `payUrl`; do not use a new tab, iframe, or client polling.
- Open Deposit Details without external navigation when initiation returns an already-paid result.
- Keep initiation failures on the form, allow a new Deposit, and reload the catalog when the selected configuration became stale.
- Refresh Deposit History after Provider return and never display, copy, or persist `payUrl` in Mobile.

### D184: Keep Deposit History Customer-Facing

- Offer only All/Pending/Paid/Cancelled single-status filtering and fixed creation-time/ID descending pagination.
- Show transaction ID, channel title, Principal, positive Gift, Credit, Deposit status, creation time, and paid time, with NGN two-fraction-digit formatting.
- Open details from each row and reserve Continue Payment for the detail page.
- Support pull-to-refresh and automatic first-page refresh after Provider return, with complete loading, empty, and failure states.
- Do not add other filters or expose payment URLs, Payment Transaction state, or Provider diagnostics.

### D185: Make Deposit Detail Actions State-Specific

- Show copyable transaction ID and the confirmed Customer-facing Deposit fields, omitting zero Gift and null paid time.
- Continue payment only for pending with an available URL, with one locked direct current-window action and no visible URL.
- For pending without a URL, allow manual refresh; for cancelled, create a new Deposit rather than retrying the old one; for paid, offer no payment action.
- Let manual refresh reveal valid late completion without automatic polling.
- Hide every Provider diagnostic and treat missing and foreign transaction IDs as the same inaccessible state.

### D186: Preview Withdrawal From Explicit Account And Amount Choices

- Show available balance and the display-only processing window while allowing submission at any time.
- Require explicit selection of an active account with an enabled bank and an ascending configured amount; do not default either choice or allow a free-form amount.
- Show holder, bank, and full account number in the account selector, and route an empty account state to account creation.
- Disable amounts above available balance and amounts whose rounded fee would leave no positive net amount, while keeping backend checks authoritative.
- Preview rate, fee, and net amount with the platform rounding rule and present an unavailable Customer message rather than exposing a configuration diagnosis.
- Keep password and submit unavailable until both selections exist, and distinguish each loading, empty, and insufficient-balance state.

### D187: Require Explicit Password Submission Without Automatic Retry

- Collect the selected account's six-digit payment password through Vant PasswordInput and NumberKeyboard, then require an explicit submit action without another confirmation dialog.
- Send only account ID, amount, and payment password; lock duplicate taps without an idempotency key or automatic request retry.
- Clear the password after an incorrect or locked result while preserving valid selections, and refresh stale authoritative balance, account, bank, option, or configuration data.
- Navigate to the returned Withdrawal Details on success.
- Treat transport timeout or connection loss as an unknown result and direct the Customer to Withdrawal History before another attempt.

### D188: Keep Withdrawal History Compact And Read-Only

- Offer only All/Pending/Rejected/Approved/Paid single-status tabs and fixed application-time/ID descending pagination.
- Show bank name, full account number, requested amount, fee, net amount, status, and application time with NGN two-fraction-digit formatting.
- Open details from the row and provide complete loading, empty, failure, and pull-to-refresh states.
- Do not expose Customer mutation controls or additional search filters.
- Reserve holder, bank code, rejection reason, review time, and paid time for detail, and never return Admin remark.

### D189: Present Withdrawal Detail From Its Immutable Snapshot

- Show copyable Withdrawal ID, the complete stored account snapshot, requested amount, fee amount, net amount, and state-specific timestamps.
- Show rejection reason and released-funds guidance only for rejected, and pending-manual-Payout guidance for approved.
- Do not present a fee-rate value that was not snapshotted, and do not change history after account or bank administration.
- Allow manual refresh without polling or Customer mutation and never expose Admin remark.
- Treat missing and foreign Withdrawal IDs as the same inaccessible state.

### D190: Present Ledger Movements Without Business References

- Filter by one optional direction and one optional compatible type, clearing the type when a direction change makes it invalid.
- Cover the nine confirmed ledger types and derive each localized title from type.
- Show a direction-derived signed amount, before balance, after balance, and creation time with NGN two-fraction-digit formatting.
- Use fixed creation-time/ID descending infinite pagination and complete refresh, loading, empty, and failure states.
- Do not add detail or mutation operations, amount or time filters, or expose IDs, business references, remarks, or persisted titles.

### D191: Make The Repository Mobile UI Specification Implementation-Ready

- Use `docs/design/finance-referral-mobile-ui.md` as the single detailed source for Mobile page composition, route paths, controls, responsive rules, states, localized presentation, reference adoption, and asset eligibility in this scope.
- Keep this business specification authoritative whenever a presentation choice touches terminology, validation, access, API exposure, state, money, or transaction behavior.
- Complete the remaining UI decisions without page-by-page confirmation and reopen discovery only for an unresolved core business boundary or contradiction.
- Keep every source URL, adopted finding, and needed asset decision in repository documentation so implementation remains independent of the capture computer and authenticated reference session.
- Do not add an ADR for these page-level decisions because they are reversible presentation choices and do not meet the architectural-decision threshold.

## Core Boundary Audit

The specification was re-read from the first requirement through D166 after the Setting rename. Customer identity, invitation closure, Withdrawal state and balance handling, Deposit/Payment separation, Provider strategy boundaries, callback logging, Ledger structure, Setting storage, IP lookup, and online presence are internally consistent.

The focused Core Boundary Audit is closed. No unresolved contradiction remains in the audited business workflows.

The Mobile page inventory, navigation, interactions, fields, states, localization, reference mapping, and asset decisions are now closed through D191 and the linked Mobile UI specification. This design pass found no new unresolved core business boundary.

## Open Decision Queue

No Mobile UI or page-design decision remains open. Add a new question here only when implementation discovery identifies a core business rule that cannot be resolved from this specification, the applicable `CONTEXT.md`, or existing ADRs.
