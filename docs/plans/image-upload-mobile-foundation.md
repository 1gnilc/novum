# Image Upload And Mobile Foundation

Status: implemented and verified

This document is the durable record for the `grill-with-docs` session. It captures every explicit requirement, separates confirmed constraints from unresolved decisions, and must be updated as each question is resolved. Feature implementation does not begin until the discovery session reaches shared understanding.

## Delivery Workflow

- Base the work on the latest remote `main` in a new work branch.
- Keep implementation consistent with adjacent repository code.
- Leave changes uncommitted unless a commit is explicitly requested.
- Record every explicit requirement and every resolved discovery decision under `docs/` so context compaction cannot remove them.

## Image Storage

### Fixed Requirements

- Store uploaded images in Cloudflare R2 through its S3-compatible API.
- Use `software.amazon.awssdk:s3` in the backend.
- Keep provider-specific names out of backend classes, methods, variables, functions, configuration prefixes, and public contracts. Use the provider-neutral term `s3`; provider names may appear only where operational documentation must identify the configured service.
- Bind every S3 setting through one dedicated Spring Boot `ConfigurationProperties` type. Store direct values under `app.s3` in the profile-specific `application-dev.yml` and `application-prod.yml`, and keep S3 settings out of the base `application.yml`. Do not create a literal `applicant.yaml` file.
- Include endpoint, region, bucket, access key, secret key, public base URL, presigned-upload expiry, pending retention, and scheduled-cleanup configuration in that properties type.
- Keep development credentials directly in the workstation's `application-dev.yml` and exclude its local changes from delivery with Git `skip-worktree`. Keep production disabled until independent production values are configured; do not copy development credentials into production.
- Start through pnpm or the existing IDEA Spring Boot Run/Debug configuration without an environment loader, shell wrapper, manual `source`, environment-file plugin, or IDEA environment-file mapping.
- Configure `publicBaseUrl` alongside the endpoint, region, bucket, and credentials. It is the base for long-lived public image URLs and must not be hard-coded.
- Implement a dedicated S3 service bean for object-storage operations and explicitly compose it into the runtime IoC container from `novum-core`.
- Follow ADR 0005: reusable infrastructure stays inert until a runtime module explicitly imports the component it needs.

### Upload Workflow

- Authenticated Admin Users and authenticated Customers may request a presigned upload and finalize the resulting upload. Each endpoint must preserve the existing separation between Admin Sessions and Customer Sessions rather than introducing a shared session identity.
- The backend creates a presigned upload request and returns it to the frontend.
- Creating a presigned upload also creates a pending image lifecycle record containing its object key, declared metadata, and expiry.
- Generate object keys on the backend as `images/{UTC yyyy}/{UTC MM}/{UTC dd}/{UUID}.{jpg|png|webp}`. Derive the extension from the accepted MIME type; do not include the original filename, identity type, Admin User or Customer ID, or other caller-controlled path content.
- The frontend uploads the file directly to S3 with the presigned request.
- Frontends expose this sequence as a reusable upload hook or utility rather than duplicating it at each call site.
- Provide an app-local `useImageUpload` in both Admin and Mobile with the same consumer-facing workflow: validate the file, request a presign, PUT directly to S3, finalize, and return the ready image. Each hook uses its own application's request client; Mobile must not depend on Admin.
- After a successful direct upload, the frontend explicitly calls the finalize endpoint.
- Finalization uses S3 object metadata to confirm that the object exists and satisfies the upload constraints before transitioning the image to ready.
- Make finalization idempotent. Finalizing an already-ready image returns the same successful image result.
- Presigned upload URLs expire after 10 minutes by default, with the duration provided through S3 configuration.
- Keep pending image records for 24 hours by default, with the retention provided through S3 configuration.
- Introduce Spring Boot scheduling for a daily cleanup task. It selects expired pending records, attempts to delete each corresponding S3 object, and hard-deletes the record only after S3 deletion succeeds. A failed S3 deletion leaves the record for a later retry.
- A single uploaded file must not exceed 3 MiB (3,145,728 bytes).
- The frontend upload utility rejects an oversized file before requesting a signature.
- The signing endpoint validates the declared byte length and binds the exact `Content-Length` into the presigned upload request.
- Finalization uses `HeadObject` to verify the stored content length. An oversized object is rejected and deleted by the backend rather than becoming a Managed Image.
- Accept JPEG, PNG, and WebP images. The MIME allowlist is `image/jpeg`, `image/png`, and `image/webp`; filename extensions are not authoritative.
- Reject SVG uploads to prevent active SVG content from introducing XSS and related browser-content risks.
- Keep first-phase validation metadata-only: validate the requested MIME against the allowlist, bind the exact `Content-Type` and `Content-Length` into the presigned request, and verify object metadata during finalization. Do not download object bytes, inspect file signatures, or decode images in this phase.
- A long-lived public image URL becomes available after successful finalization and is derived from configured `publicBaseUrl` plus the object key.
- Provide one S3 URL utility method that safely joins configured `publicBaseUrl` and an object key. `ImageService` exposes `getUrl(objectKey)` and delegates URL construction to that utility; controllers, mappers, and other business services do not concatenate image URLs themselves.
- Use a presigned HTTP PUT. The presign request contains `contentType` and `contentLength`.
- Return `objectKey`, `uploadUrl`, literal method `PUT`, caller-settable signed headers including `Content-Type`, and RFC 3339 UTC `expiresAt`. Bind exact content length in the signature while allowing the browser to supply its forbidden `Content-Length` header automatically.
- Finalize with `{ objectKey }`. On success return `id`, `objectKey`, derived `url`, `contentType`, `contentLength`, `status: READY`, and RFC 3339 UTC `createTime`.
- Do not expose a usable public image URL before finalization succeeds.

### Backend Image Management

- Add the image-related backend model and application classes under `apps/server/novum-core`, including an `ImageService`, `ImageController`, persistence classes, and related contracts.
- Add a database table that manages uploaded image records.
- Keep the image table minimal: `id`, `del`, `create_time`, `update_time`, unique `object_key`, `content_type`, `content_length`, `status`, and `expires_at`.
- Do not persist an uploader or owner identity. Image records are not owner-scoped and cannot provide per-uploader audit or queries; authenticated signing and finalization use the unguessable UUID object key to correlate the upload.
- Do not persist a public URL, original filename, image dimensions, or checksum.
- Provide an admin management page for image records.
- Provide a presigned-upload endpoint.
- Provide an administrator-only delete operation that removes the object from S3 through the backend and updates the managed record consistently.
- Deletion does not inspect or rewrite references in other business tables. A deleted object may therefore leave an existing object key that no longer renders; this is an accepted operational risk because deletion is an infrequent, controlled administrator action.
- For administrator deletion, call S3 `DeleteObject` first and logically delete the image record only after S3 succeeds. An S3 failure leaves the database unchanged. If the database update fails after S3 succeeds, a retry is safe because object deletion is idempotent. Logical deletion supports traceability but not object restoration.
- Provide a paginated list endpoint.
- The admin list returns only ready images and shows image preview, image URL, and creation time. Pending uploads remain internal to the upload lifecycle and scheduled cleanup.
- Add an upload action to the Admin image page. It uses the Admin `useImageUpload` and refreshes the list after successful finalization. It needs no additional Button Menu access code because every `image:manager` who can reach the page already has an authenticated Admin upload permission.
- Open the upload action in a Vben form drawer using the Admin application's Element Plus upload component. Allow one JPEG, PNG, or WebP selection with a preview, and start the direct-upload workflow only when the administrator confirms the form.
- Label the image URL column `URL`, render the value on one truncated line instead of displaying the complete URL, and provide an adjacent icon copy action that copies the complete derived URL and reports success.
- Provide an explicit finalize/save-upload operation.
- The list and other API responses expose a derived `url` even though the canonical stored value is the object key.
- Existing business image fields keep their current names and compatible string column types but store only object keys. The project is pre-release, so discard old development rows where necessary; do not migrate legacy absolute URLs or add dual-format compatibility.
- Follow the repository UTC decision for persisted and serialized creation times.
- Frontend action visibility and backend permission enforcement remain separate, following ADR 0008.
- Add a built-in `image:manager` role and grant it the image page, page-query permission, and administrator-delete permission. Assign this role to the default Admin User during initialization.
- Place the Admin page at `System > Image`, with route `/system/image` and component `/system/image/index`.
- Bind `POST:/image/presign` and `POST:/image/finalize` to both built-in baseline roles: `admin` and `customer`.
- Bind `POST:/image/page` and `POST:/image/remove/{id}` to `image:manager`.
- Use `system:image:remove` as the delete Button Menu access code. The list has no button access code: Page Menu grants control navigation visibility and the page endpoint permission independently enforces access.

## Mobile Foundation Components

### NavBar

- Add a reusable Mobile component named `NavBar` based on Vant and informed by `/Volumes/fc/archive/legacy/legacy-web/src/components/BackNavBar.vue`.
- Enable `safe-area-inset-top` by default.
- Expose a `leftArrow` prop and handle its left click with the default `router.back()` behavior.
- Add a `locale` boolean prop whose default is `false`.
- Adapt the complete Vant NavBar contract instead of relying on implicit attribute fallthrough. Reuse Vant's runtime prop definitions so options such as `fixed`, `border`, `placeholder`, `zIndex`, text, disabled states, and `clickable` remain typed and configurable. Keep `fixed=true`, `placeholder=true`, and `safeAreaInsetTop=true` as overridable defaults.
- Forward Vant's `title`, `left`, and `right` slots. The `locale` control retains priority over a caller-provided `left` slot, while the absence of either lets Vant render its native arrow and left text. Re-emit `clickLeft` and `clickRight`; the default router back behavior still runs when the effective control is the back arrow.
- Allow only one left-side control. `locale` has the highest priority: when `locale=true`, show only the current locale's regional flag and suppress all other left content. When `locale=false`, preserve Vant's normal precedence in which a caller-provided `left` slot replaces the native `leftArrow` and `leftText` content; without a custom slot, Vant renders the configured arrow and text normally.
- Give the flag control an accessible current-language label without rendering the language name visually.
- Clicking the locale control opens the bottom language chooser; clicking the back arrow executes the default back behavior.
- Use the legacy component only as a behavioral reference; align the final API and styles with the current Vue 3, TypeScript, Vant, router, preferences, and locale pipeline.

### Locale

- Add a reusable component named `Locale`, informed by `/Volumes/fc/archive/legacy/legacy-web/src/components/Language.vue` and the existing Mobile language selector.
- Open the selector as a bottom popup or action sheet and allow the Customer to switch language.
- Keep the current language in Mobile's existing typed Preferences store and use the existing `loadLocaleMessages(locale)` pipeline so Vue I18n, Vant, Day.js, and the document language remain synchronized.
- Add `apps/mobile/src/locales/locale.ts` as the canonical location for `SUPPORTED_LOCALES`, `DEFAULT_LOCALE`, and `AppLocale`.
- Support exactly four Mobile locales in the first phase: `en-US` (English), `zh-CN` (Simplified Chinese), `ha-NG` (Hausa), and `yo-NG` (Yoruba).
- Order Locale items as `English` (`en-US`), `简体中文` (`zh-CN`), `Hausa` (`ha-NG`), and `Yorùbá` (`yo-NG`). Display each language in its own name and keep the default locale first.
- Set `DEFAULT_LOCALE` to `en-US`. A supported persisted preference or browser locale may still select another supported locale.
- Define and export a supported locale item collection using this shape:

  ```ts
  interface LocaleItem {
    flags: string;
    locale: AppLocale;
    name: string;
  }
  ```

- Locale items include a display name such as `English`, a supported locale value, and a flag asset path.
- Relative flag paths resolve to assets under `apps/mobile/src/assets/flags`; the implementation must use a Vite-safe import strategy. The existing `#` alias resolves to `apps/mobile/src`.
- Name each Mobile locale image after its exact locale tag. Copy and rename `docs/design/novum-ui-style-guide/assets/images/flag-us.png` to `apps/mobile/src/assets/flags/en-US.png`, `language-hausa.png` to `ha-NG.png`, and `language-yoruba.png` to `yo-NG.png`.
- Add `apps/mobile/src/assets/flags/zh-CN.png` using a standard China flag image normalized to the existing US flag asset's 536x282 dimensions.
- Preserve the original style-guide assets so its offline HTML and asset manifest remain valid; do not move or delete the source files.
- Import the renamed files statically from `locale.ts` so Vite emits valid asset URLs in development and production builds.
- Export `getLocaleItem(locale)`, returning the matching `LocaleItem` or `null` when unsupported.
- Reuse or migrate the existing `LanguageSelector`; do not create a second locale state or loading pipeline.

### Tabbar

- Add a reusable Mobile `Tabbar` based on Vant and informed by `/Volumes/fc/archive/legacy/legacy-web/src/components/Tabbar.vue`.
- Include exactly five destinations: Home, Market, Team, Fund, and My (personal center).
- Keep labels localized and navigation integrated with the current static Mobile router.
- Use these route contracts: Home is `home` at `/` and public; Market is `market` at `/market` and public; Team is `team` at `/team` and authenticated; Fund is `fund` at `/fund` and authenticated; My is `my` at `/my` and authenticated.
- Make Tabbar sizing responsive through shared tokens for its height, icon size, and label size. Use `52px / 24px / 12px` on screens narrower than 360px or landscape screens no taller than 500px, `56px / 26px / 13px` on regular phones, and `60px / 28px / 14px` at 430px wide and 700px tall or above. Derive layout bottom spacing from the responsive height token so page content remains unobscured.
- Reuse the existing Home page. Replace the existing Account route contract with My, and provide foundational navigable pages for Market, Team, and Fund without inventing business functionality in this phase.
- Provide the Mobile `useImageUpload` for future Customer upload use cases, but do not add a concrete Mobile upload screen in this phase.

## Mobile Styling

- Introduce Tailwind CSS to `apps/mobile` for cases where a meaningful semantic class name is genuinely difficult to define. It is an escape hatch, not the default styling mechanism.
- Sass support is confirmed: the workspace catalog includes Sass and the installed Vite toolchain can compile Vue `<style lang="scss">` blocks.
- Use BEM naming for every manually defined CSS class, including classes in Vue scoped style blocks. Sass nesting may use `&__element` and `&--modifier` to generate BEM names.
- Reserve camelCase for TypeScript and JavaScript identifiers; do not use camelCase as an alternate CSS class convention.
- Put durable Mobile implementation rules in `apps/mobile/AGENTS.md`, not `apps/mobile/CONTEXT.md`, because repository `CONTEXT.md` files are glossaries only.
- Use Vant as Mobile's only UI component library and keep Mobile independent from Admin infrastructure, following module instructions and ADR 0013.
- Preserve the current project's code style wherever adjacent code establishes a pattern.

## Locale Boundaries

- Extend backend static Spring Message support to `en-US`, `zh-CN`, `ha-NG`, and `yo-NG`.
- Add corresponding Hausa and Yoruba static bundles for common, RBAC, and system messages using Spring's `_ha_NG` and `_yo_NG` resource suffixes.
- Extend backend request locale resolution and the static `I18nMessageService` to recognize all four locales.
- Keep dynamic internationalization unchanged at exactly `en-US` and `zh-CN`. Do not add Hausa or Yoruba rows, form fields, bundle keys, schema behavior, or Admin management columns.
- Decouple `DynamicI18nMessageServiceImpl` from the expanded static `SupportedLocale` set by giving dynamic internationalization its own fixed two-locale collection under its owning `I18nMessageConstants` boundary.
- For `ha-NG` and `yo-NG`, use Vant's English locale because Vant does not provide Hausa or Yoruba bundles.
- For Day.js, load its Yoruba locale for `yo-NG` and use the English locale for `ha-NG`, which Day.js does not support.

## Visual Reference And Browser Environment

- Use only AdsPower profile number 37 / profile ID `k1f658vy` for live website inspection and Mobile browser verification during this work.
- Do not open the reference or local site in any other browser profile or browser environment.
- Use `https://www.novumaivip.com` as a visual and layout reference, not as a design to copy wholesale.
- Improve layout balance, visual coordination, and usability where the reference is weak.
- If the reference site requires authentication, use the credentials supplied in the conversation; never persist those credentials in repository files, logs, fixtures, screenshots, or documentation.
- The latest `main` already contains a read-only measured reference in `docs/design/novum-ui-style-guide`, captured from the required AdsPower profile at 360x800, 430x932, and 768x1024.
- AdsPower Global 8.4.3 and the `adspower-browser` CLI are installed locally, and the Local API has been configured successfully without persisting its key in the repository. Profile number 37 resolves to `k1f658vy`. Its required Chrome 150 kernel is installed and the profile launches successfully. Use only this profile for live reference inspection and Mobile browser verification.

## Development S3 Configuration Audit

- The initial `docs/plans/s3.txt` contained a Cloudflare API token in addition to S3 credentials. The unneeded API token has been removed; the local file retains the S3 Access Key ID, S3 Secret Access Key, two S3 endpoint forms, and development public URL.
- The runtime AWS S3 client uses the S3 Access Key ID and S3 Secret Access Key only.
- Use the first, path-free account-level S3 URL as `endpoint`. The path segment in the second S3 URL is confirmed as the target `bucket`; bind that segment separately and do not configure the SDK with the path-bearing URL.
- Set R2 `region` explicitly to `auto`.
- Use the development public URL as `publicBaseUrl` after confirming it is enabled for the target bucket.
- Direct browser upload requires the development bucket CORS configuration below. It includes the repository's default Admin and Mobile ports (`5077` and `5078`) plus the agreed fallback ports (`5173`, `5183`, and `5193`), with both `localhost` and `127.0.0.1` origins so development can move to an unused port without another bucket change.

  ```json
  [
    {
      "AllowedOrigins": [
        "http://localhost:5077",
        "http://127.0.0.1:5077",
        "http://localhost:5078",
        "http://127.0.0.1:5078",
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:5183",
        "http://127.0.0.1:5183",
        "http://localhost:5193",
        "http://127.0.0.1:5193"
      ],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3600
    }
  ]
  ```

- Deployed Admin and Mobile origins are environment-specific and must be added before browser-direct upload is enabled outside local development.
- The original credential note remains untracked and ignored at `/docs/plans/s3.txt`. Its development values have been migrated directly into the local `application-dev.yml`; the application never reads `s3.txt` at runtime, and Git `skip-worktree` keeps the local credential-bearing profile out of delivery.

## Confirmed Repository Facts

- Mobile already has a typed persisted Preferences store and supports `en-US` and `zh-CN`.
- The backend `SupportedLocale` enum and all current backend message bundles also support only `en-US` and `zh-CN`.
- The AdsPower 37 reference capture shows three entries on the reference language page: English, Hausa, and Yoruba.
- The legacy application declares 25 languages, but those declarations do not establish translation completeness or make all 25 part of the new Mobile scope.
- Locale switching already synchronizes Vue I18n, Vant, Day.js, and the document `lang` attribute.
- Mobile already has a `LanguageSelector` component that can be migrated into or wrapped by `Locale`.
- Mobile routes are static and use the `#` alias for `apps/mobile/src`.
- Tailwind CSS 4 and its Vite plugin are present in the workspace catalog but are not wired into `apps/mobile`.
- `novum-core` already owns application controllers, services, persistence objects, and explicit runtime configuration.
- The currently identified business image references are `admin.avatar` and `customer.avatar`. Both are nullable `varchar(500)` columns and both bootstrap rows use `NULL`; repository tests are the only tracked locations that currently persist sample absolute avatar URLs.
- The workstation has AdsPower Global 8.4.3 and the `adspower-browser` CLI installed. Its Local API is healthy, profile number 37 is confirmed as profile ID `k1f658vy`, the required Chrome 150 kernel is installed, and the profile has been launched successfully.

## Decision Log

- **Upload principals**: Authenticated Admin Users and authenticated Customers may request and finalize image uploads. Admin and Customer authentication remains separate, following ADR 0014 and ADR 0017.
- **Maximum file size**: One uploaded file must not exceed 3 MiB (3,145,728 bytes). Enforce this through frontend preflight, signing-time validation, a presigned exact `Content-Length`, and finalization-time `HeadObject`; delete an oversized object found during finalization.
- **Image formats**: Accept JPEG, PNG, and WebP. Reject SVG to avoid XSS and related active-content risks. Validate by allowed MIME type rather than trusting a filename extension.
- **Content validation scope**: Keep the first phase simple and preserve direct-upload efficiency. Do not download or decode uploaded image content and do not inspect magic bytes; rely on the allowlisted and signed MIME metadata plus finalization-time object metadata checks.
- **Object key**: Generate `images/{UTC yyyy}/{UTC MM}/{UTC dd}/{UUID}.{jpg|png|webp}` on the backend. Derive the extension from MIME and exclude original filenames, principal types, principal IDs, and caller-controlled path fragments.
- **Incomplete upload cleanup**: Presigned URLs expire after a configurable 10 minutes and pending records are retained for a configurable 24 hours. A daily Spring Boot scheduled task deletes the S3 object and then hard-deletes the pending record; S3 deletion failures remain pending for retry. Finalization is idempotent.
- **Administrator deletion**: Delete the S3 object first, then logically delete the image record. Preserve the database record when S3 fails and rely on idempotent object deletion when a database failure requires a retry. A logically deleted record is traceable but not recoverable.
- **Image metadata**: Persist only `id`, `del`, `create_time`, `update_time`, unique `object_key`, `content_type`, `content_length`, `status`, and `expires_at`. Do not persist uploader identity, public URL, original filename, dimensions, or checksum. Records have no uploader ownership or per-uploader audit behavior.
- **S3 configuration**: Manage all S3 settings with one Spring Boot `ConfigurationProperties` type. Store direct profile values separately in `application-dev.yml` and `application-prod.yml`, leaving the base `application.yml` free of S3 settings. Include endpoint, region, bucket, credentials, `publicBaseUrl`, presign expiry, pending retention, cleanup scheduling, and the upload limit.
- **Local S3 configuration**: Keep development credentials directly in the workstation's `application-dev.yml` and mark the tracked path with Git `skip-worktree` so its local values stay out of delivery. Production remains disabled until independent production values are configured.
- **IDEA local startup**: Use the existing native Spring Boot Run/Debug configuration for `NovumBootApplication` without environment-file mappings or wrapper scripts. Direct Run/Debug reads the active profile configuration normally.
- **NavBar left control**: Render at most one left-side control. `locale=true` takes precedence and renders only the regional flag with an accessible language label. Otherwise preserve Vant's native left-slot override and `leftArrow`/`leftText` rendering behavior.
- **NavBar Vant contract**: Reuse Vant's NavBar prop definitions, forward its three named slots and both click events, default `fixed`, `placeholder`, and `safeAreaInsetTop` to `true`, and keep the wrapper-specific locale precedence and default back behavior as the remaining intentional specialization.
- **Tabbar routes**: Use `home` `/` and `market` `/market` as public destinations. Use `team` `/team`, `fund` `/fund`, and `my` `/my` as authenticated destinations. This phase creates only foundational navigable pages for Market, Team, and Fund.
- **Tabbar sizing**: Use responsive shared tokens for height, icon size, and label size: compact and short-landscape `52px / 24px / 12px`, regular `56px / 26px / 13px`, and large-phone `60px / 28px / 14px`. Derive the layout's bottom spacing from the same responsive height token.
- **Mobile locales**: Support `en-US`, `zh-CN`, `ha-NG`, and `yo-NG` in the first phase. Do not copy the legacy application's full 25-language declaration.
- **Locale display names and order**: Present Locale items in this exact order: `English`, `简体中文`, `Hausa`, `Yorùbá`, mapped respectively to `en-US`, `zh-CN`, `ha-NG`, and `yo-NG`.
- **Mobile default locale**: Use `en-US`; supported persisted and browser preferences retain precedence through the existing preference-resolution behavior.
- **Upload API contract**: Use presigned PUT. Presign accepts `contentType` and `contentLength` and returns `objectKey`, `uploadUrl`, method, caller-settable signed headers, and expiry. Finalize accepts only `objectKey` and returns the ready image including its derived URL and metadata; public URL availability starts after successful finalization.
- **Frontend upload hooks**: Admin and Mobile each own a `useImageUpload` with the same workflow but their own request client. Add an upload action to the Admin image page and refresh after success; Mobile exposes the hook without adding an upload screen in this phase.
- **Admin image list scope**: List only ready images. Pending records are lifecycle internals managed by finalization and scheduled cleanup.
- **Image URL conversion**: Centralize `publicBaseUrl` and object-key joining in an S3 URL utility. Expose conversion through `ImageService.getUrl(objectKey)` and prohibit ad hoc URL concatenation in business code.
- **Development R2 endpoint and bucket**: Use the first path-free URL from `docs/plans/s3.txt` as the S3 endpoint and the confirmed path segment from the second URL as the separate bucket value.
- **Development credential handling**: Keep `docs/plans/s3.txt` ignored and keep the populated `application-dev.yml` local through Git `skip-worktree`. Spring binds its direct `app.s3` values into `ConfigurationProperties`.
- **Development bucket CORS**: Deliver the R2 CORS policy as deployment documentation. Allow the repository defaults `5077` and `5078` plus fallback ports `5173`, `5183`, and `5193`, for both `localhost` and `127.0.0.1`; allow `GET`, `PUT`, `POST`, `DELETE`, and `HEAD`, accept all request headers, expose `ETag`, and cache preflight results for 3600 seconds. Add deployed application origins separately for each environment.
- **Locale image naming**: Store Mobile locale images under `apps/mobile/src/assets/flags` and name each file with its locale tag. Copy the style-guide assets to `en-US.png`, `ha-NG.png`, and `yo-NG.png` without removing their source files.
- **Chinese locale image**: Add a standard China flag as `zh-CN.png`, normalized to 536x282.
- **Static versus dynamic backend locales**: Backend static messages and request locale resolution support all four Mobile locales, including new Hausa and Yoruba bundles. Dynamic internationalization remains exactly English and Chinese, using its own fixed locale collection rather than the expanded static `SupportedLocale` enum.
- **Vant locale fallback**: Use Vant English for `ha-NG` and `yo-NG` because Vant has no corresponding locale bundles.
- **Day.js locale mapping**: Use Day.js Yoruba for `yo-NG` and Day.js English for `ha-NG`.
- **Mobile CSS naming**: Use strict BEM for all manually defined CSS classes, including scoped Vue styles. Sass nesting may generate BEM elements and modifiers. CamelCase is for TypeScript and JavaScript identifiers only; Tailwind remains the escape hatch when no meaningful semantic class is available.
- **Image lifecycle**: Signing creates a pending image record. The frontend explicitly finalizes a successful direct upload, and the backend verifies the stored object before making the image ready.
- **Business image references**: Business tables store the image object key directly in their existing semantic field, such as `avatar`. They do not store an image ID or a public URL, and existing field names remain unchanged.
- **Legacy image data**: The project is pre-release and old development data may be discarded. Do not implement URL-to-object-key migration, preserve old absolute URLs, or accept two stored formats. Reset existing data where necessary and use only object keys going forward.
- **Public URL**: APIs derive a long-lived public URL from configured `publicBaseUrl` and the stored object key. The base URL is part of S3 configuration alongside endpoint, bucket, region, and credentials.
- **Reference integrity on deletion**: Administrator deletion does not check or update business-table references. Broken image references after an incorrect deletion are an accepted operational risk.
- **Image administration authorization**: Add built-in `image:manager`, assign it to the default Admin User, and place `/system/image` under System. Bind presign/finalize permissions to both `admin` and `customer`; bind page/remove permissions and the page menu to `image:manager`; use `system:image:remove` for delete-button visibility.

## Implementation Verification

- The work branch was created from the updated `main` commit `e5b8797` as `codex/image-upload-mobile-foundation`. Changes remain uncommitted as required by repository delivery rules.
- A real S3-enabled production startup exposed that the bootstrap application's explicit `@MapperScan` omitted `com.gnilc.novum.image.dao`. The package was added, and `ApplicationContextIT` now enables the S3 composition and asserts that `ImageService` is available so the production IoC path cannot silently regress.
- Final standards and specification review capped every configured upload maximum at 3 MiB, kept avatar object keys available without synthesizing an avatar URL when the optional S3 composition is absent, reduced the internal object-key lookup visibility, aligned the Admin API input with its inline `Pick` convention, switched Mobile Tabbar navigation to canonical route names, and aligned the image SQL deployment command with the existing connection placeholders. Unit and integration tests cover the S3 cap and the unavailable-image-service avatar contract for both Admin Users and Customers.
- `mvn -f apps/server/pom.xml verify` passed all 10 reactor modules after the final review fixes. The bootstrap integration test discovered 51 request mappings, including all four `/image/*` endpoints.
- Mobile verification passed 20 test files and 52 tests. Admin verification passed 65 test files and 481 tests. Workspace type checking, Admin and Mobile production builds, and lint all passed; lint retains only three pre-existing `vue/one-component-per-file` warnings in `apps/mobile/src/test/basic-layout.test.ts`.
- AdsPower profile 37 was the only browser environment used. Mobile passed at 360x800 and 430x932 with four locale choices, correct Hausa switching, five route contracts, authentication prompts, loaded visual assets, no horizontal overflow, and no console or page errors.
- Admin was verified from a production build served at the allowed `http://localhost:5173` origin. A real 19,924-byte PNG completed presign, browser CORS preflight, direct R2 PUT, finalize, list refresh, derived public URL preview, and administrator removal. The R2 preflight returned 204, the PUT returned 200, and every application endpoint returned 200.
- The Admin image list no longer declares duplicate `url` fields. Its preview uses a unique `objectKey` column and a stable 56x56 cover image inside an 80px table row; measured root and image dimensions were both exactly 56x56. Browser console output was empty and the prior VXE duplicate-field warning was absent.
- Every successful browser QA object was deleted through the administrator flow. One upload reservation whose browser PUT never started was hard-deleted as a development-only pending QA record. The final development database contains zero active pending images and zero active ready images.
- `docs/plans/s3.txt`, the local credential-bearing changes in `application-dev.yml`, generated `dist` output, and `dist.zip` are not part of the worktree delivery. No credential or presigned query value is recorded in tracked documentation or source.

## Open Questions

None. Discovery reached shared understanding, implementation was confirmed, and the agreed scope has been implemented.
