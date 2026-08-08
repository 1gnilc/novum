# Customer Finance And Referral Mobile UI Specification

[中文版本](finance-referral-mobile-ui_zh-CN.md)

## Status And Authority

This document is the implementation-ready Mobile page and interaction design for the Customer finance and referral scope. The business rules in [`customer-finance-referral`](../plans/customer-finance-referral/README.md) remain authoritative. This document decides presentation, route composition, controls, states, responsive behavior, localization, and use of reference assets; it must not reinterpret a business invariant.

The design is repository-owned and portable. A developer can implement it without the capture computer, an AdsPower installation, an authenticated reference-site session, or files outside this repository.

## Durable Inputs

- Business specification: [`customer-finance-referral`](../plans/customer-finance-referral/README.md)
- Reference-page audit: [`mobile-reference-pages.md`](mobile-reference-pages.md)
- Measured visual reference: [`ui-style-guide`](ui-style-guide/README.md)
- Reference asset manifest: [`ui-style-guide/data/assets.json`](ui-style-guide/data/assets.json)
- Vant payment-password controls: [`PasswordInput`](https://vant-ui.github.io/vant/#/zh-CN/password-input) and [`NumberKeyboard`](https://vant-ui.github.io/vant/#/zh-CN/number-keyboard)

Exact public reference URLs and the adopted or rejected behavior are repeated below. External pages are evidence, not runtime dependencies.

## Experience Direction

The Mobile application is a compact operational product, not a marketing landing page. Customers should be able to identify their balance, referral progress, record status, and next action without scanning decorative content.

### Visual System

- Retain the existing Light, Dark, and System theme capability.
- Use the reference palette as brand input, not as a one-color page treatment.
- In light mode, use neutral `#F5F7FA` canvas and white working surfaces. Reserve `#0E2D42` and `#1E2947` for brand headers and navigation.
- In dark mode, use `#0E2D42` canvas, `#1E2947` navigation, and `#334364` working surfaces, with neutral borders and semantic status colors breaking up the blue family.
- Use `#FB6F30` for the primary command, `#6757D4` for selection and progress, `#8CDE4C` for success, and `#FA3747` for destructive or failed states.
- Use the existing system sans-serif stack. Do not use the reference site's accidental Times default.
- Keep letter spacing at `0`. Do not scale font size from viewport width.
- Use 12px captions, 14px supporting text, 16px body and controls, 18px page titles, and 24px maximum for a finance summary amount.
- Use an 8px spacing rhythm with 4px only for tightly related label/value pairs.
- Use 4px or 8px radii for ordinary controls and cards. Circular avatars and status dots are the only routine fully round elements.
- Use borders and surface contrast instead of decorative shadows. A popup may use one restrained shadow.
- Use tabular numerals for amounts, counts, phone values, account numbers, transaction IDs, and timestamps.

### Layout

- Support 320px and wider viewports. Validate at 360x800, 430x932, and 768x1024 CSS pixels.
- Constrain page content to 560px and center it on wider Mobile or tablet viewports.
- Use 12px horizontal gutters below 360px, 16px from 360px, and 20px from 600px.
- Keep the primary tab bar at the bottom with safe-area padding. Secondary pages hide it.
- Use a sticky bottom action area only for the page's single primary command. Add bottom content padding equal to the action area plus the safe-area inset.
- Use two-column statistic and amount grids on narrow screens. A three-column amount grid is allowed from 480px when every formatted NGN value fits without truncation.
- Never place a card inside another card. Page sections are unframed; repeated records may use bordered rows or individual cards.
- Let transaction IDs and account numbers wrap with `overflow-wrap: anywhere`; never reduce the font based on viewport width.
- Keep every touch target at least 44x44 CSS pixels.

### Controls

- Use Vant components already owned by Mobile: Form, Field, Button, Cell, ActionSheet, Dialog, Tabs, PullRefresh, List, Skeleton, Empty, Uploader, PasswordInput, and NumberKeyboard.
- Use Vant icons for familiar actions. Use icon-only buttons only for familiar commands such as back, add, copy, visibility, edit, delete, and refresh, with an accessible name and tooltip where the icon is not self-evident.
- Use radio rows for one-of-many choices, tabs for record status views, a menu or sheet for option sets, and dialogs only for destructive confirmation or credential verification.
- A disabled control must retain its dimensions and label. Loading indicators must not resize a button or row.
- Do not show feature descriptions, keyboard shortcuts, visual-design commentary, or tutorial copy inside the application.

## Route Map

| Route name | Suggested path | Access | Primary tab | Parent |
| --- | --- | --- | --- | --- |
| Home | `/` | Public | Home | - |
| Market | `/market` | Public | Market | - |
| Login | `/login` | Public | None | Public source or Home |
| Registration | `/register` | Public | None | Login or public source |
| Team Overview | `/team` | Customer | Team | - |
| Team Members | `/team/members/:level` | Customer | None | Team Overview |
| Referral Tier Details | `/team/tiers` | Customer | None | Team Overview |
| Fund Overview | `/fund` | Customer | Fund | - |
| Deposit Initiation | `/fund/deposits/new` | Customer | None | Fund Overview |
| Deposit History | `/fund/deposits` | Customer | None | Fund Overview |
| Deposit Details | `/fund/deposits/:txnId` | Customer | None | Deposit History |
| Withdrawal Submission | `/fund/withdrawals/new` | Customer | None | Fund Overview |
| Withdrawal History | `/fund/withdrawals` | Customer | None | Fund Overview |
| Withdrawal Details | `/fund/withdrawals/:id` | Customer | None | Withdrawal History |
| Balance Ledger | `/fund/ledger` | Customer | None | Fund Overview |
| My Overview | `/my` | Customer | My | - |
| Profile Edit | `/my/profile` | Customer | None | My Overview |
| Withdrawal Accounts | `/my/withdrawal-accounts` | Customer | None | My Overview |
| Withdrawal Account Creation | `/my/withdrawal-accounts/new` | Customer | None | Withdrawal Accounts |
| Withdrawal Account Edit | `/my/withdrawal-accounts/:id/edit` | Customer | None | Withdrawal Accounts |
| Payment Password Change | `/my/withdrawal-accounts/:id/password` | Customer | None | Withdrawal Accounts |
| Login Password Change | `/my/password` | Customer | None | My Overview |

Define static `new` routes before parameterized routes. Route names, rather than string-concatenated paths, generate internal links. Preserve a validated internal redirect across Login and Registration exactly as defined in the business specification.

## Shared Shell And States

### Primary Navigation

- Keep Home, Market, Team, Fund, and My visible in that order.
- Use stable Vant icons: home, trend/market, friends/team, balance/fund, and contact/my.
- Use the orange primary color for the active tab and a muted neutral for inactive tabs.
- Keep labels visible; do not use icon-only tab navigation.
- Home and Market remain outside this feature's work-business definition.

### Protected-Route Prompt

- Render no protected business data before authentication.
- Present one action sheet with Login as the primary action and Cancel as the secondary action.
- Login retains the intended route. Cancel replaces the route with Home.
- Do not create an authentication router guard; the route-derived shell owns the prompt.

### Data States

Every data page defines these states without changing layout dimensions:

- Initial loading: content-shaped skeletons, not an indefinite centered spinner.
- Refreshing: retain current content and show PullRefresh progress.
- Loading more: append one fixed-height loading row.
- Empty: concise localized title, one sentence at most, and one relevant action when recovery exists.
- Recoverable error: concise localized error with Retry.
- Inaccessible record: one neutral result for missing and foreign resources, with navigation to the owning list.
- Stale mutation input: keep the business error, clear secrets, reload authoritative data, and preserve only still-valid non-secret choices.
- Submitting: lock mutable controls and the primary action until the request settles.
- Unknown mutation result after timeout or connection loss: never auto-retry; direct the Customer to the authoritative list or reload the resource before another attempt.

### Feedback

- Use inline field errors for local validation.
- Use a short toast for successful copy, save, and ordinary completion.
- Use a persistent inline result or dialog for rejected, locked, inaccessible, stale, or unknown-result states that require a decision.
- Present backend-localized business errors without internal exception text.
- Do not show a success state until the owning request has committed successfully.

## Authentication Pages

### Login

- Use a compact brand header containing the repository-owned Novum logo, not a hero or card.
- Place Back and Language actions in the navigation bar.
- Order fields as Country Calling Code plus national phone, then password.
- Render the calling code as a fixed-width selector attached visually to the phone field. Default to `+234` and offer only `+1` and `+234`.
- Use a masked current-password field with a visibility icon.
- Keep the Login button in normal document flow; it becomes sticky only on short viewports where the keyboard would otherwise hide it.
- Put the Registration text action immediately below Login.
- Preserve the current Customer Session during a failed re-login. Replace local session data only after the new Login and Customer-info load both succeed.

### Registration

- Reuse the Login brand header and form geometry so the two pages feel like one flow.
- Order fields as Country Calling Code plus national phone, password, password confirmation, and Invitation Code.
- Use masked new-password controls with visibility icons.
- Show Invitation Code as a six-cell numeric field or one numeric field with tabular digits; it must preserve leading zeroes. Do not use the payment-password keyboard component for Invitation Code.
- Prefill `inviteCode` from the route query and keep it editable.
- Put the Registration button after the form and the Login text action below it.
- During automatic Login, replace the button label with a stable signing-in state. If it fails, route to Login with phone identity only.

## Team Pages

### Team Overview

- Use a restrained top band for current tier, current tier rate, automatic-upgrade status, and next-tier progress.
- Do not use the reference `Member` badges; their wording conflicts with Referral Reward Tier. Use a Vant award/diamond icon and semantic color treatment instead.
- Show direct registrations, Qualified Direct Referrals, and total team size in a two-column grid, with the total spanning the final row when necessary.
- Show L1, L2, and L3 as three full-width rows containing level, count, and chevron. Avoid three narrow cards.
- Present Invitation Code and invitation link as two copy rows. The displayed URL may wrap; the copy control remains fixed at the row edge.
- Place Referral Tier Details as the final navigation row.
- Refresh on entry and support pull-to-refresh. A valid zero-count response is content, not an empty state.

### Team Members

- Use a segmented L1/L2/L3 control synchronized with the route parameter. Changing the segment replaces the route parameter and reloads page one.
- Render each member as a compact row with 40px avatar, backend-masked nickname, backend-masked phone, registration time, and an L1-only qualification badge.
- Use a neutral avatar placeholder when no avatar is available.
- Keep the list read-only and use infinite loading.

### Referral Tier Details

- Lead with current tier, Qualified Direct Referral count, and automatic-upgrade state.
- Use a vertical four-step progression rather than four separate oversized cards.
- Each step shows tier name, threshold, rate, and reached/current/unreached status.
- Show one compact next-tier progress bar below the current step. Diamond uses a terminal highest-tier result.
- Put the confirmed reward and progression rules in concise labeled rows below the progression.
- Do not use the reference tier images because they contain `Member` and fixed numeric labels.

## Fund Pages

### Fund Overview

- Use one balance band with `NGN` available balance at 24px maximum.
- Put Deposit and Withdrawal as two equal primary action buttons directly below the balance.
- Show cumulative Deposit Principal and cumulative paid Withdrawal amount as a two-column definition row.
- Show Deposit History, Withdrawal History, and Balance Ledger as full-width navigation rows.
- Do not render recent records or a frozen-balance value.

### Deposit Initiation

- Order sections as available balance, amount options, eligible channel, selected summary, and primary action.
- Use a stable responsive amount grid. Each option shows Principal and a smaller positive Gift label.
- Show channels as full-width radio rows after amount selection. Use `title` as the only channel identity.
- Show Principal, Gift, and Credit as a three-row definition list so long formatted values never collide.
- Keep the primary action sticky above the safe area on short screens.
- On pending initiation, replace the button content with a fixed-width loading state until browser navigation begins.

### Deposit History

- Use horizontally scrollable All/Pending/Paid/Cancelled tabs.
- Render one bordered record row per Deposit: status and Credit on the first line, transaction ID and channel on the second, Principal/Gift and times below.
- Use semantic status colors only for the status label, not the entire card.
- Tap the row to open details. Do not put Continue Payment on the row.

### Deposit Details

- Use a compact status header followed by definition rows for transaction ID, channel, Principal, positive Gift, Credit, created time, and paid time.
- Put Copy beside transaction ID.
- Pending with URL uses one Continue Payment button. Pending without URL uses a disabled unavailable state plus Refresh.
- Cancelled uses one Start New Deposit action. Paid has no payment action.

### Withdrawal Submission

- Order sections as processing notice, available balance, Withdrawal Account, amount options, fee summary, payment password, and submit action.
- Use an account selection sheet with radio rows showing holder, bank, and full account number.
- Use a stable amount grid with unavailable values disabled in place.
- Show requested amount, fee rate, fee amount, and net amount in a definition list.
- Reveal PasswordInput only after account and amount are valid. NumberKeyboard attaches to that control and must not cover the fee summary or submit action.
- Keep explicit Submit after all six digits; do not submit from keyboard completion.

### Withdrawal History

- Use horizontally scrollable All/Pending/Rejected/Approved/Paid tabs.
- Render status and net amount first, then bank and full account number, then requested amount, fee, and application time.
- Tap the row to open details. No row action mutates or resubmits the Withdrawal.

### Withdrawal Details

- Use a compact status header and an ordered state timeline.
- Present the immutable account snapshot in one unframed section and the amount breakdown in another.
- Put Copy beside Withdrawal ID.
- Rejected shows reason and released-funds notice. Approved shows pending-manual-Payout notice. Paid shows paid time.
- Keep Refresh in the navigation action or one secondary button; never poll automatically.

### Balance Ledger

- Use All/Income/Expense segmented control followed by one type-filter menu.
- Render each row with localized type title and signed amount first, before-to-after balance second, and time last.
- Use success color for income sign and the danger color for expense sign while keeping body text neutral.
- Do not add a detail affordance; rows are complete and noninteractive.

## My Pages

### My Overview

- Present an 80px avatar, nickname, full Customer Phone Identity, and optional email in one compact profile header. The whole header opens Profile Edit.
- Use the repository's neutral avatar placeholder when avatar is null.
- Show Withdrawal Accounts, Login Password Change, and Language as full-width navigation rows.
- Show the current language on the Language row. Open the existing locale action sheet and apply the selection immediately through Mobile preferences.
- Put Logout as a separated danger-text command at the bottom, not inside the profile header.
- Confirm Logout in a dialog. After confirmation, call the backend, always clear local Customer Session data, and replace the route with Login without a redirect target.
- On user-info failure, show a Retry result and do not render stale Customer details as current.
- Do not show roles, permissions, internal Customer ID, username, finance totals, or referral statistics.

### Profile Edit

- Show the current avatar as an 80px circle with a camera edit icon. Use the Managed Image uploader and show upload progress over the stable avatar frame.
- Provide Remove Avatar as a small trash-icon action when an avatar exists; confirm removal only when the Customer has not already replaced it in the current edit session.
- Order fields as nickname and optional email. Show Customer Phone Identity as a read-only identity row above editable fields, not as a disabled form input.
- Normalize and validate nickname and email using the confirmed rules. Show character counts only near their limits.
- Disable Save when no normalized value changed or while an image upload is pending.
- On success, update the authenticated Customer information, toast success, and return to My Overview.
- When leaving with unsaved normalized changes, confirm Discard or Continue Editing.
- Upload failure retains the previously committed avatar and allows retry.

### Withdrawal Accounts

- Use an accessible add icon in the navigation bar and the same Add Account action in the empty state.
- List active accounts without pagination. Each row shows holder, bank name and code, and full normalized account number.
- Mark an account whose bank is disabled as unavailable for Withdrawal. Keep its management actions enabled.
- Selecting a row opens a small action sheet with Edit Account, Change Payment Password, and Delete Account. Do not expose several cramped inline buttons.
- Delete Account opens a destructive confirmation dialog containing account summary and one masked current-login-password field.
- On confirmed deletion, lock the dialog. Success removes the row. Timeout or connection loss closes the secret input, reloads the list, and never blindly retries deletion.

### Withdrawal Account Creation

- Order fields as holder, Withdrawal Bank, account number, current login password, new payment password, and payment-password confirmation.
- Open Withdrawal Bank selection in a searchable sheet because the initialized list contains hundreds of banks. Search locally by case-insensitive bank name or code; selection includes only enabled banks.
- Use a normal text input for the country-neutral account number. Mobile removes whitespace and uppercases ASCII letters for preview, while the backend remains authoritative.
- Mask current login password with `current-password` autocomplete semantics.
- Enter payment password and confirmation sequentially through one PasswordInput and NumberKeyboard surface at a time. Confirmation is Mobile-only.
- Keep Save disabled until account fields, login password, and matching six-digit payment passwords are present.
- On success, clear all secret values, toast success, and replace the route with Withdrawal Accounts.
- For a duplicate active Bank Account Identity, remain on the page, clear both credential types, and highlight bank plus account number.
- On timeout or connection loss, clear all credentials and return to Withdrawal Accounts to verify whether creation committed before another attempt.

### Withdrawal Account Edit

- Prefill holder, current bank, and normalized account number. Never return or prefill a payment password.
- Use the same searchable enabled-bank sheet as creation.
- If the current bank is disabled, show its historical value but require selection of an enabled bank before Save.
- Reveal current payment-password entry after at least one normalized account field changes.
- Save remains disabled until data changed and a complete six-digit current payment password exists.
- Do not place payment-password replacement fields on this page; use the dedicated route.
- Incorrect or locked password clears the password only and preserves edits. Success clears the password, updates the list, and returns to Withdrawal Accounts.
- On timeout or connection loss, clear the password and reload the account before another attempt.

### Payment Password Change

- Show the account's holder, bank, and full account number as read-only context.
- Collect current payment password, new payment password, and new-password confirmation in three sequential PasswordInput stages using one NumberKeyboard.
- Allow any six ASCII digits and allow the new value to equal the current value.
- Confirmation is Mobile-only. Send only current and new payment passwords.
- Incorrect or locked current password clears every payment-password value but retains account context.
- On success, clear all values, toast success, and return to Withdrawal Accounts.
- There is no reset, recovery, or Admin-help entry.

### Login Password Change

- Order fields as current password, new password, and new-password confirmation.
- Use masked fields with visibility icons and appropriate current/new password autocomplete semantics.
- Apply the confirmed strong rule to the new password and allow it to equal the current password. Confirmation is Mobile-only.
- Lock Save during submission and never auto-retry.
- On success, clear all local Customer Session data because the backend revokes every session, then replace the route with Login and show the password-changed result.
- On timeout or connection loss, clear all password fields and local Customer Session data, then open Login with a result-unknown message; do not assume the old session remains valid.
- Do not offer reset or recovery.

## Localization And Formatting

### Supported Locales

Support exactly the existing four interface locales:

- `en-US`: English, default fallback
- `zh-CN`: Simplified Chinese
- `ha-NG`: Hausa
- `yo-NG`: Yoruba

Country Calling Code and interface locale remain independent. Never infer Hausa or Yoruba from `+234`, or English from `+1`.

### Message Ownership

- Keep page labels, control text, empty states, status labels, and ledger titles in Mobile `vue-i18n` resources grouped by cohesive resource.
- Use backend-localized stable business errors from the owning backend message bundles. Do not reproduce internal exception strings in Mobile.
- Do not depend on Admin dynamic messages or `@vben/locales`.
- Use complete translated strings with interpolation. Do not concatenate translated fragments around counts, amounts, times, names, or statuses.
- Every locale file must contain the same key set. CI tests compare locale key parity.
- Allow wrapping to at least two lines for Hausa and Yoruba labels. No button or tab may clip its translated label.

### Domain Formatting

- Format monetary values with `Intl.NumberFormat` using `currency: 'NGN'`, `currencyDisplay: 'code'`, and exactly two fractional digits.
- Format percentages with exactly two fractional digits followed by `%`.
- Display API instants in the device's local timezone through the existing Day.js locale pipeline. Include an absolute date and time; do not use relative time alone.
- Display Withdrawal Processing Window strings exactly as backend `HH:mm` values without timezone conversion.
- Display the authenticated Customer's complete phone identity where confirmed. Display Team Member phone and nickname only from backend-provided masks.
- Keep enum values out of visible copy; map Deposit, Withdrawal, Ledger, Tier, direction, and qualification values through locale resources.

## Reference Adoption Map

| Novum page | Public reference URL | Adopt | Improve or reject |
| --- | --- | --- | --- |
| Login | `https://www.novumaivip.com/#/pages/index/login` | Separate calling code, phone, password, Registration entry | Strong password/session behavior; no source-side session clearing |
| Registration | `https://www.novumaivip.com/#/pages/index/reg` | Ordered identity/password/invitation form | No captcha, verification code, privacy checkbox, or weak password rule |
| My Overview | `https://www.novumaivip.com/#/pages/my/usercenter` | Compact profile and task entries | Move finance tasks to Fund; remove oversized controls and role data |
| Deposit Initiation | `https://www.novumaivip.com/#/pages/money/recharge` | Amount grid, channel radio rows, primary action | Show Gift and Credit; explicit selection; complete states |
| Withdrawal Submission | `https://www.novumaivip.com/#/pages/money/fundWithdraw` | Account, balance, amount, fee, net order | Configured options only; informational processing window; no hard-coded fee or schedule |
| Withdrawal Accounts | `https://www.novumaivip.com/#/pages/my/bindbankcard` | List plus accessible Add entry | Multiple accounts, action sheet management, disabled-bank state |
| Account Creation | `https://www.novumaivip.com/#/pages/my/addbankinfo` | Holder, bank, account, payment-password order | Login-password verification, searchable bank sheet, secure PasswordInput |
| Histories and Ledger | `https://www.novumaivip.com/#/pages/money/moneylog` | Compact chronological rows and filters | Keep Deposit, Withdrawal, and Ledger as separate domain pages |
| Team Overview | `https://www.novumaivip.com/#/pages/commission/index` | Invitation copy, metrics, L1-L3 summaries | Two-column metrics and list rows; Novum every-Deposit reward rules |
| Invitation behavior | `https://www.novumaivip.com/#/pages/commission/poster` | Copy code and link idea only | No poster, QR capture, or first-charge-only copy |
| Password Change | `https://www.novumaivip.com/#/pages/my/setpwd` | Current/new/confirmation sequence | Customer login password only; per-account payment-password route |
| Tier Details | `https://www.novumaivip.com/#/pages/index/main?id=10`, `https://www.novumaivip.com/#/pages/index/main?id=11`, `https://www.novumaivip.com/#/pages/index/main?id=12` | Initial thresholds/rates as corroborating evidence | Dynamic current data; no group, screenshot, manual application, or downgrade rules |
| Fund Overview | `https://www.novumaivip.com/#/pages/fund/list` | Five-tab placement only | Build a functional finance workspace; reject the unopened placeholder |

## Asset Decisions

Implementation copies only selected files into the owning Mobile asset directory and renames them by product purpose. Runtime code never imports from `docs` and never hotlinks the reference CDN.

### Approved Candidates

- `ui-style-guide/assets/images/logo-novum.png`: compact authentication and brand identity.
- `ui-style-guide/assets/images/empty-state.png`: generic empty or inaccessible state after verifying contrast in both themes.
- `ui-style-guide/assets/images/no-data.png`: alternate record-list empty state if it remains legible at the required size.
- Existing Mobile-owned locale flags and avatar placeholder remain authoritative; do not duplicate them from the reference folder.
- Use installed Vant icons before copying a bitmap icon with equivalent meaning.

### Prohibited Reference Assets

- `banner-invite.png`: contains first-charge-only and fixed 10% reward copy that contradicts Novum.
- `qr-invite-large.jpg` and `qr-invite-small.jpg`: account-specific captured QR codes.
- `vip-badge-gold.png`, `vip-badge-platinum.png`, `vip-badge-diamond.png`, `vip-badge-1-static.png`, `vip-level-2-card.png`, and `vip-level-3-card.png`: contain `Member`, fixed numbers, or obsolete VIP semantics.
- `captcha-sample.png`, `captcha-register.png`, and `captcha-register-alt.png`: captcha is not a Novum requirement.
- `login-background.png`: contains a visible stock-asset watermark and does not qualify as a production brand asset.
- `fontello-icons.ttf`: do not add a second icon-font system when Vant already owns icons.
- Any authenticated screenshot or asset containing a phone, Invitation Code, balance, transaction ID, or account-specific state.

## Acceptance Checks

- Exercise every route at 360x800, 430x932, and 768x1024 in both light and dark themes.
- Verify no tab bar, sticky action, NumberKeyboard, dialog, sheet, long translation, amount, phone, account number, or transaction ID overlaps another control.
- Verify protected pages render no business data behind the unauthenticated prompt.
- Verify all forms restore loading locks in `finally` and secret fields clear on the documented outcomes.
- Verify every list has initial, refresh, loading-more, empty, error, and end states.
- Verify all icon-only controls have an accessible name and keyboard activation where native semantics do not provide it.
- Verify focus remains visible, dialogs and sheets trap focus correctly, and Back restores the documented parent.
- Verify the four locale bundles have key parity and that Hausa and Yoruba labels wrap without clipping.
- Verify NGN, percentage, UTC-instant display, and processing-window formatting against representative long and zero values.
- Verify selected repository assets are copied into Mobile and no runtime reference points into `docs` or an external asset host.
