# Novum Mobile Reference Page Audit

[中文版本](mobile-reference-pages_zh-CN.md)

## Purpose

This document preserves the reusable findings from the Mobile reference-site review. It is an input to product discovery, not an implementation specification and not a requirement to reproduce the source site pixel for pixel.

The resulting implementation-ready page and interaction decisions are maintained in [`finance-referral-mobile-ui.md`](finance-referral-mobile-ui.md). That document is self-contained and takes precedence over this observational audit for UI implementation.

- Source: `https://www.novumaivip.com`
- Captured: 2026-08-08
- Required capture environment: AdsPower profile 37 / `k1f658vy`
- Capture viewport: 430 x 932 CSS pixels
- Capture mode: authenticated read-only navigation; no Deposit, Withdrawal, account-binding, password-change, or other mutating form was submitted
- Visual companion: [`ui-style-guide`](ui-style-guide/README.md)
- Repository-owned assets: [`ui-style-guide/assets`](ui-style-guide/assets)

The login credentials and account-specific values observed during capture are intentionally excluded. The findings below are self-contained so implementation does not depend on the capture computer, its AdsPower installation, or an active reference-site session.

## Reference URLs

| Area | Exact reference URL | Observed result |
| --- | --- | --- |
| Home | `https://www.novumaivip.com/#/pages/index/index` | Five-item tab shell; a direct refresh could remain on a loading screen |
| Login | `https://www.novumaivip.com/#/pages/index/login` | Phone and password login with a calling-code prefix and Register entry |
| Registration | `https://www.novumaivip.com/#/pages/index/reg` | Phone, password, password confirmation, invitation code, verification code, privacy agreement, and Register action |
| Personal Center | `https://www.novumaivip.com/#/pages/my/usercenter` | Account summary plus Recharge, Withdrawal, Bank, Balance Details, Invite, and password entries |
| Deposit initiation | `https://www.novumaivip.com/#/pages/money/recharge` | Selected amount summary, fixed amount grid, payment-channel radio list, and primary payment action |
| Withdrawal submission | `https://www.novumaivip.com/#/pages/money/fundWithdraw` | Withdrawal account, balance, amount, fee, net amount, submit action, and processing guidance |
| Alternate withdrawal layouts | `https://www.novumaivip.com/#/pages/money/fundWithdraw2`, `https://www.novumaivip.com/#/pages/money/withdraw2` | Similar field order with inconsistent labels and theme values; not separate Novum requirements |
| Withdrawal Account list | `https://www.novumaivip.com/#/pages/my/bindbankcard` | Empty-state list with an icon-only Add action in the navigation bar |
| Withdrawal Account creation | `https://www.novumaivip.com/#/pages/my/addbankinfo` | Holder, account number, bank selection, payment password, password confirmation, and submit action |
| Balance and transaction records | `https://www.novumaivip.com/#/pages/money/moneylog` | One page with ledger rows and Recharge Record / Withdrawal Record tabs |
| Team overview | `https://www.novumaivip.com/#/pages/commission/index` | Invitation code/link copy actions, team counters, and L1-L3 summary cards |
| Team-level detail | `https://www.novumaivip.com/#/pages/commission/myteam` | Historical/detail route; the captured account had no detail data and direct navigation did not produce a useful layout |
| Invitation sharing | `https://www.novumaivip.com/#/pages/commission/poster` | Copy Code / Copy Link actions, invitation facts, and referral-program explanation |
| Password change | `https://www.novumaivip.com/#/pages/my/setpwd` | Current password, new password, confirmation, and submit action; also exposes a source-site-wide withdrawal-password entry |
| Gold explanation | `https://www.novumaivip.com/#/pages/index/main?id=10` | Three qualified L1 referrals and a 13% direct-referral tier rate |
| Platinum explanation | `https://www.novumaivip.com/#/pages/index/main?id=11` | Ten qualified L1 referrals and a 16% direct-referral tier rate |
| Diamond explanation | `https://www.novumaivip.com/#/pages/index/main?id=12` | Twenty qualified L1 referrals and a 20% direct-referral tier rate |
| Fund tab candidate | `https://www.novumaivip.com/#/pages/fund/list` | The current tab is configured as `alert::To be opened`; it does not provide a usable Fund page |
| Historical settings routes | `https://www.novumaivip.com/#/pages/my/setemail`, `https://www.novumaivip.com/#/pages/my/set`, `https://www.novumaivip.com/#/pages/my/language` | Present in the earlier asset capture but did not render usable current pages during this review |

## Reusable Interaction Findings

### Authentication

- Keep the calling code visually separate from the national phone input.
- Keep password confirmation as a Mobile form check during registration; it is not an additional persisted credential field.
- The reference registration page includes a graphical verification code and privacy agreement. Neither is a confirmed Novum requirement and neither may be implemented without a later decision.
- The reference password copy describes a weaker 6-16 alphanumeric rule. Novum must use its confirmed 8-64 strong Customer Password Rule instead.
- Navigating directly to the source login and registration routes could clear or replace the source session. Novum routing must not introduce that side effect unless logout is explicitly requested.

### Personal Center And Fund

- Reuse the idea of a compact account summary followed by clear task entries.
- Do not copy the source information architecture that places all finance operations in Personal Center while leaving Fund unopened.
- Novum should make Fund the real balance, Deposit, Withdrawal, and Ledger workspace. My should own Profile, Withdrawal Accounts, password, language, and session actions.
- Account identifiers and monetary summaries must use stable labels and NGN formatting rather than bare numbers.

### Deposit

- Reuse the single-page selection order: current amount, predefined amount options, enabled channels, then the primary action.
- Show the selected option's Deposit Gift and resulting Deposit Credit; the source page displays only principal.
- Use a currency formatter and responsive grid. The source uses oversized fixed cells and ungrouped large numbers.
- Use a radio/check indicator with a full-row touch target for channel selection.
- Do not show the primary tab bar on the focused payment form if it competes with or obscures the primary action.
- Provide explicit loading, empty-channel, unsupported-channel, initiation-failure, and already-paid states instead of a blank page or indefinite spinner.

### Withdrawal

- Reuse the order: Withdrawal Account, available balance, Withdrawal Amount Option, fee, net amount, then submit.
- Novum must use predefined Withdrawal Amount Options rather than the source free-form amount field.
- Display the configured processing times as informational guidance only. Do not copy the source weekday restriction, 72-hour promise, hard-coded limits, or hard-coded 25% fee.
- Use `Withdrawal Fee`, not the source's inconsistent `commission` or French `Frais de traitement` labels.
- Keep explanatory copy concise and scannable; do not center multiple long bold paragraphs beneath the form.

### Withdrawal Accounts

- Reuse the list-plus-Add entry pattern and the creation-field order.
- Give the icon-only Add action an accessible name and a stable touch target.
- Novum supports multiple Withdrawal Accounts and must expose edit, delete, and payment-password-change actions per account.
- Payment-password fields must use secure six-digit input. The source renders these fields as ordinary text and must not be copied.
- Novum creation and deletion additionally require the Customer login password under the confirmed rules.

### Ledger And Histories

- Reuse compact chronological rows and lightweight filters.
- Keep Deposit history, Withdrawal history, and the Customer Balance Ledger as distinct domain views even if they share presentation components.
- Use Novum's confirmed statuses and ledger type labels; source terms such as `To be reviewed`, `Transfer rebate`, and `commission` are not canonical.
- Show signed amount, resulting balance, business label, and timestamp without exposing the Admin-only ledger remark.

### Team, Invitation, And Referral Reward Tiers

- Reuse one-tap Copy actions for Invitation Code and invitation link.
- Reuse a compact overview of team size, qualified direct referrals, and L1-L3 results, but avoid three narrow cards that force labels into cramped columns.
- The source invitation page says L1-L3 rewards apply only to the first recharge. Novum's confirmed rule applies Referral Level Reward and Referral Tier Reward to every successful Deposit; source copy must not be reused.
- The three tier pages corroborate the confirmed initial values: Gold `3 / 13.00%`, Platinum `10 / 16.00%`, and Diamond `20 / 20.00%`.
- The source tier pages also require external chat groups, screenshot check-ins, manual application, and automatic downgrade for inactivity. These behaviors are outside Novum and directly conflict with its confirmed automatic, non-decreasing tier progression.

### Password

- Reuse the current password, new password, confirmation, and primary submit structure.
- Password confirmation remains a Mobile form field and is not sent as an independent domain credential after client validation succeeds.
- Do not copy the source Customer-wide withdrawal-password entry. Novum payment passwords belong independently to Withdrawal Accounts.
- On successful login-password change, clear local Customer credentials and return to Login because the backend revokes every Customer Session.

## Visual Direction

Reuse primarily:

- Dark canvas `#0E2D42`;
- Navigation surface `#1E2947`;
- Working surface `#334364`;
- Primary action orange `#FB6F30`;
- Selection accent `#6757D4`;
- Success `#8CDE4C` and danger `#FA3747` where semantically appropriate;
- Existing repository-owned logos, navigation icons, tier badges, invitation imagery, and empty-state assets when they fit the confirmed page.

Improve rather than copy:

- Use the application's sans-serif typography instead of the source's accidental Times default.
- Keep ordinary cards at 8px radius or less and avoid the source's 21-28px card and pill treatment.
- Reduce oversized fields, amounts, and buttons so one viewport carries useful context.
- Use neutral and semantic contrast so the application is not a one-note dark-blue interface.
- Prevent the fixed tab bar from occluding long content or empty states.
- Replace centered red prose blocks with concise localized rows, notices, or validation messages.
- Provide stable loading, empty, error, disabled, selected, and submitted states; never leave a route as a blank canvas with only a spinner.

## Portable Asset Use

- Treat the repository files under [`ui-style-guide/assets`](ui-style-guide/assets) as the durable source for reusable reference assets.
- During implementation, copy only selected assets into the owning Mobile asset directory. Runtime code must not import from `docs` or hotlink the reference site's CDN.
- Keep the copied asset's purpose clear in its destination filename and avoid duplicating the same bitmap under several names.
- Do not add authenticated reference screenshots containing phone numbers, Invitation Codes, balances, transaction IDs, or other account data. This report preserves their useful structure without those values.
