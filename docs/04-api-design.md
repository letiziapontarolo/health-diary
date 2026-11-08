# Health Diary — REST API Design Document

**Document ID:** API-004  
**Version:** 1.0 (design baseline)  
**Status:** Design baseline; OpenAPI implementation and contract tests pending  
**Related documents:** [01-requirements.md](01-requirements.md), [02-architecture.md](02-architecture.md), [03-database-design.md](03-database-design.md)  
**Stack:** Spring Boot, Spring Security server-side sessions, PostgreSQL; React/TypeScript client  
**Product language:** Italian; identifiers and technical documentation: English

## 1. Purpose, scope, and principles

This document specifies the **local MVP** HTTP contract for Health Diary using **synthetic demonstration data only**. It is a design baseline, not an exported OpenAPI specification or implemented endpoint inventory. It covers authentication, profiles, shared/private catalogs, medical events, laboratory results, documents, calendar, dashboard, and common API behavior. Planned medication management, vaccinations, hospitalizations, surgeries, measurements, clinical interpretations, notifications, and PDF export are **out of scope**.

- Base path: `/api`; JSON request/response properties use `camelCase`; database columns use `snake_case`.
- Paths name plural resources in `kebab-case`; IDs are UUID strings; all client-facing dates use `YYYY-MM-DD`, local times `HH:mm`, and instants ISO 8601 with an explicit offset or `Z`.
- Use explicit request/response DTOs; **never serialize JPA entities**.
- The authenticated owner comes from the server session, never from a client-supplied `userId`.
- Mutations use CSRF protection. Requests to protected endpoints require a valid session.
- `GET` requests are safe; mutations have explicit `POST`, `PUT`, `PATCH`, or `DELETE` semantics.
- New resources normally return `201 Created` with `Location` and a response DTO; successful deletions return `204 No Content`.
- The frontend uses relative `/api/...` URLs through the Vite development proxy; no browser-to-backend cross-origin configuration is required for the local MVP.

## 2. HTTP conventions and common contracts

### 2.1 Status codes

| HTTP status | Meaning / typical use |
|---|---|
| `200 OK` | Successful read or update with a body |
| `201 Created` | Resource created; include `Location` |
| `204 No Content` | Successful deletion, unlink, logout, or no-body update |
| `400 Bad Request` | Malformed JSON, invalid query syntax, malformed UUID |
| `401 Unauthorized` | Missing, expired, or invalid authenticated session |
| `403 Forbidden` | Validly authenticated user lacks action permission; missing/invalid CSRF token |
| `404 Not Found` | Missing resource **or resource owned by another user** |
| `409 Conflict` | Unique-key conflict, duplicate link, incompatible current resource state |
| `413 Content Too Large` | Upload exceeds configured limit |
| `415 Unsupported Media Type` | Unsupported content type or rejected file format |
| `422 Unprocessable Content` | Syntactically valid request violates field/domain rules |
| `429 Too Many Requests` | Login/registration rate limit, if configured |
| `500 Internal Server Error` | Unexpected failure; no stack traces or sensitive data in response |

Authentication failures should not reveal whether an email exists. For owner-scoped resources, prefer `404` over `403` to avoid existence disclosure. Unexpected database constraint violations must be mapped to stable, safe responses, not raw SQL messages.

### 2.2 Error format: RFC 9457 Problem Details

Content type: `application/problem+json`. Standard properties are `type`, `title`, `status`, `detail`, and `instance`. Application extensions are `code` (stable machine-readable identifier) and optional `fieldErrors`.

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 422,
  "detail": "One or more fields contain invalid values.",
  "instance": "/api/medical-events",
  "code": "VALIDATION_ERROR",
  "fieldErrors": [
    { "field": "eventDate", "message": "La data è obbligatoria." }
  ]
}
```

The frontend uses `code`/`fieldErrors`, **not the English `title` text**, to select Italian UI messages. Stable codes include `VALIDATION_ERROR`, `EMAIL_ALREADY_EXISTS`, `AUTHENTICATION_REQUIRED`, `INVALID_CREDENTIALS`, `CSRF_INVALID`, `RESOURCE_NOT_FOUND`, `DUPLICATE_CATALOG_ENTRY`, `INVALID_EVENT_CATEGORY`, `INVALID_RESULT_PARAMETER`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, and `RESOURCE_CONFLICT`. The exact mapping and localized field messages are documented in the implementation OpenAPI spec.

### 2.3 Validation and PATCH behavior

- Frontend: React Hook Form + Zod for immediate feedback. Backend: Jakarta Bean Validation, service-level business validation, and PostgreSQL constraints.
- Request fields that identify related resources (`serviceId`, `doctorId`, `facilityId`, `parameterId`, `unitId`, `documentId`) must resolve to accessible records.
- `PATCH` uses a **documented partial JSON object**, not JSON Patch (RFC 6902). An **omitted** field means “unchanged”; explicit `null` clears a nullable field; explicit `null` on required fields is rejected. Implement DTO presence tracking so omitted and `null` are not conflated.
- Whitelist writable fields per endpoint; reject unknown/read-only properties rather than silently accepting `userId`, `id`, computed status, or file storage paths.
- Date/time validation follows the configured local application timezone (initially `Europe/Rome`); ambiguous daylight-saving transitions need an explicit implementation policy before release.
- All `DELETE` operations that permanently remove user data require frontend confirmation; destructive account deletion additionally requires current-password verification server-side.

### 2.4 Pagination and sorting

`page` is zero-based (default `0`); `size` defaults to `20`, maximum `100`. Sort keys are whitelisted per resource. Use a stable secondary sort (e.g., `id`) for repeatable pagination.

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Medical event default ordering is **scheduled: nearest first; completed/cancelled: newest first**, using **effective** status rather than a stale stored `status`. API lists that are inherently small (e.g., measurement units) may return arrays without pagination. Calendar queries are bounded by date range instead of page number; enforce a maximum interval to avoid unbounded responses.

## 3. Authentication and profile

### 3.1 Authentication endpoints

| Method | Path | Description | Success |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | Bootstrap CSRF token (also before login/register) | `200` |
| `POST` | `/api/auth/register` | Create account, **do not auto-login** | `201` |
| `POST` | `/api/auth/login` | Authenticate; establish server-side session | `200` |
| `POST` | `/api/auth/logout` | Invalidate **current** session only | `204` |
| `GET` | `/api/auth/me` | Current authenticated user summary | `200` |

`POST /api/auth/register` example:

```json
{
  "firstName": "Mario",
  "lastName": "Rossi",
  "dateOfBirth": "1998-05-14",
  "email": "mario.rossi@example.com",
  "password": "UnaPasswordMoltoLunga!"
}
```

Response `201` (never include password or password hash):

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "Mario",
  "lastName": "Rossi",
  "email": "mario.rossi@example.com"
}
```

Login request: `{ "email": "mario.rossi@example.com", "password": "UnaPasswordMoltoLunga!" }`. On success, return the authenticated user summary and set a session cookie. Do not expose a bearer token. On failure, use a generic invalid-credentials response.

CSRF response contract (proposed):

```json
{ "headerName": "X-CSRF-TOKEN", "token": "<opaque-token>" }
```

The frontend reads the token from this response and sends it in the declared header on every unsafe request, **including login and logout**. Refresh CSRF state after authentication changes or token rejection. Spring Security CSRF repository and cookie/header behavior must be tested together; the token is not an authentication credential.

### 3.2 Profile endpoints

| Method | Path | Description | Success |
|---|---|---|---|
| `GET` | `/api/profile` | Full current-user profile | `200` |
| `PATCH` | `/api/profile` | Edit first/last name, birth date, optional height/city | `200` |
| `PUT` | `/api/profile/email` | Change email with current password | `200` |
| `PUT` | `/api/profile/password` | Change password with current password | `204` |
| `DELETE` | `/api/profile` | Delete account and private data, current password required | `204` |

Profile response fields: `id`, `firstName`, `lastName`, `dateOfBirth`, `email`, `heightCm` (nullable), `city` (nullable), `createdAt`, `updatedAt`. Initials-based avatar is computed by the frontend; no photo URL.

Examples:

```json
{ "heightCm": 168.5, "city": "Torino" }
```

```json
{ "newEmail": "nuovo@example.com", "currentPassword": "<current-password>" }
```

```json
{ "currentPassword": "<current-password>", "newPassword": "<new-password>" }
```

Password policy: 12–128 characters, no artificial character-class requirement, reject known weak/compromised values using the agreed local development policy. Password change invalidates **other** active sessions while preserving the current session; email change preserves the current session. Account deletion invalidates all sessions and coordinates database/filesystem cleanup. No email verification or password-reset-by-email in MVP.

## 4. Medical service catalog

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/medical-services` | List accessible shared + personal services |
| `GET` | `/api/medical-services/{id}` | Service detail |
| `POST` | `/api/medical-services` | Create personal service |
| `PATCH` | `/api/medical-services/{id}` | Rename, archive/reactivate, or change category if unreferenced |
| `DELETE` | `/api/medical-services/{id}` | Hard-delete personal **unreferenced** service |

Filters: `category=LABORATORY|SPECIALIST|DIAGNOSTIC`, `q`, and `includeArchived` (default `false` for new-event selectors). Sort alphabetically. Example item:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Emocromo completo",
  "category": "LABORATORY",
  "iconKey": "blood-test",
  "isCustom": false,
  "isActive": true
}
```

Create body: `{ "name": "Analisi personalizzate", "category": "LABORATORY", "iconKey": "blood-test" }` (`iconKey` optional or `null`). The same optional field is editable via `PATCH` for personal services. Archive body: `{ "isActive": false }`. The backend accepts only known keys from the approved icon catalog (400/422 for invalid keys); `GET` responses include `iconKey` as a string or `null`. The frontend resolves it to a Lucide component or bundled SVG and falls back to a category icon if absent or unknown. Icon assets are never uploaded through these endpoints. Global records are read-only. Names are case-insensitively unique within their scope/category; the service layer also rejects personal duplicates of accessible global entries. Archived entries remain in historical views but are excluded from new-event selection. A referenced personal service may be renamed/archived, not hard-deleted; category cannot change while referenced.

## 5. Doctors and facilities

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/doctors?q=...` | Search user's doctors for autocomplete |
| `POST` | `/api/doctors` | Create doctor |
| `PATCH` | `/api/doctors/{id}` | Edit doctor |
| `DELETE` | `/api/doctors/{id}` | Delete if unreferenced |
| `GET` | `/api/facilities?q=...` | Search user's facilities |
| `POST` | `/api/facilities` | Create facility |
| `PATCH` | `/api/facilities/{id}` | Edit facility |
| `DELETE` | `/api/facilities/{id}` | Delete if unreferenced |

Minimum create body: `{ "name": "Dott.ssa Rossi" }` or `{ "name": "Laboratorio Centrale" }`. All records are private to the current user. Duplicate merging is supported by the domain design, but a dedicated merge endpoint and transaction contract remain to be finalized before implementation; never silently delete referenced records.

## 6. Medical events

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/medical-events` | Search/filter/paginate current user's events |
| `GET` | `/api/medical-events/{id}` | Event detail, including computed fields |
| `POST` | `/api/medical-events` | Create event general information |
| `PATCH` | `/api/medical-events/{id}` | Edit general information, reschedule, set/restore manual status |
| `DELETE` | `/api/medical-events/{id}` | Permanently delete event, results and links; **keep documents** |

Filters: `category`, `serviceId`, `status` (effective), `doctorId`, `facilityId`, `dateFrom`, `dateTo`, `page`, `size`, and approved `sort` keys. Optional free-text `q` for service-name search is subject to a bounded, owner-scoped query. Default page size is 20.

Create request:

```json
{
  "serviceId": "550e8400-e29b-41d4-a716-446655440001",
  "eventDate": "2027-02-15",
  "eventTime": "10:30",
  "doctorId": null,
  "facilityId": null,
  "notes": "Portare gli esami precedenti"
}
```

Create response `201`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440010",
  "service": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Emocromo completo",
    "category": "LABORATORY"
  },
  "eventDate": "2027-02-15",
  "eventTime": "10:30",
  "doctor": null,
  "facility": null,
  "status": "SCHEDULED",
  "statusMode": "AUTO",
  "notes": "Portare gli esami precedenti",
  "reportText": null,
  "reportNotExpected": false,
  "missingReport": false
}
```

`status` in responses is **effective** status. For `statusMode=AUTO`, calculate from local event date/time; for `MANUAL`, use the explicitly selected `SCHEDULED`, `COMPLETED`, or `CANCELLED`. Today's untimed event remains scheduled until the next day; a timed event completes when its time passes. A cancellation is a manual state; restore via `{ "statusMode": "AUTO" }`, which recomputes effective status. Manual override example: `{ "statusMode": "MANUAL", "status": "CANCELLED" }`. Reject attempts to write a contradictory `status` when switching to AUTO.

`missingReport` is derived only for completed events with no report text, no linked report document, no lab results, and `reportNotExpected=false`. It is not writable. A computed `COMPLETED` status is **not clinical confirmation** that a procedure occurred. Multiple same-service/same-date events are allowed. Cancelled events stay in history but are omitted from calendar results.

### 6.1 Section-based event management (confirmed decision API-01)

Create general event data first, then manage lab results, report text, and document links through independent endpoints. A laboratory event may be saved without results. Section updates must not overwrite unrelated sections.

## 7. Laboratory parameters, units, and results

### 7.1 Catalogs

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/medical-services/{serviceId}/parameters` | Accessible parameters for a LABORATORY service |
| `POST` | `/api/medical-services/{serviceId}/parameters` | Create personal parameter for service |
| `PATCH` | `/api/lab-parameters/{id}` | Edit/archive personal parameter |
| `DELETE` | `/api/lab-parameters/{id}` | Delete unreferenced personal parameter |
| `GET` | `/api/measurement-units` | List predefined measurement units |

Parameter DTO: `id`, `serviceId`, `name`, `isCustom`, `isActive`. Units: `id`, `symbol`, `name`. Global entries are read-only. Archived parameters remain valid for historical results.

### 7.2 Event results

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/medical-events/{eventId}/results` | List results in position order |
| `POST` | `/api/medical-events/{eventId}/results` | Append result |
| `PATCH` | `/api/medical-events/{eventId}/results/{resultId}` | Edit result |
| `DELETE` | `/api/medical-events/{eventId}/results/{resultId}` | Delete result |
| `PUT` | `/api/medical-events/{eventId}/results/order` | Atomically reorder **all** event results |

Example numeric result request:

```json
{
  "parameterId": "550e8400-e29b-41d4-a716-446655440020",
  "valueType": "NUMERIC",
  "numericValue": 95,
  "unitId": "550e8400-e29b-41d4-a716-446655440030",
  "intervalType": "RANGE",
  "lowerBound": 70,
  "upperBound": 100,
  "lowerInclusive": true,
  "upperInclusive": true
}
```

Result response adds `id`, `position`, expanded `parameter` (`id`, `name`), and expanded `unit` (`id`, `symbol`) or `null`. Result values use `valueType=NUMERIC|TEXT|UNAVAILABLE`. Numeric uses `numericValue` and optional `unitId`; text uses `textValue`; unavailable has neither. Reference interval types: `RANGE`, `LOWER_BOUND`, `UPPER_BOUND`, `TEXT`, or `null`, with corresponding bounds/inclusivity or `textInterval`. Numeric bounds must be coherent and share the result's measurement scale. No diagnosis or automatic clinical interpretation.

Only LABORATORY events accept results. A parameter must belong to the event's service and be global or owned by the user; units must be valid for that parameter under the final unit-compatibility policy. Repeated use of the same parameter within an event is allowed. For reordering:

```json
{
  "resultIds": [
    "550e8400-e29b-41d4-a716-446655440041",
    "550e8400-e29b-41d4-a716-446655440040"
  ]
}
```

The request must list **exactly** the event's results once each; update positions transactionally. The API for historical parameter series/charts is an implementation follow-up: define a read-only, owner-scoped endpoint and exact unit-grouping response before building charts. For MVP, do not silently convert units or mix unknown units with explicit units.

## 8. Report text

| Method | Path | Description |
|---|---|---|
| `PUT` | `/api/medical-events/{eventId}/report` | Create/replace text report and `reportNotExpected` flag |
| `DELETE` | `/api/medical-events/{eventId}/report` | Clear report text (preserve linked documents) |

Example:

```json
{ "reportText": "Referto della visita specialistica.", "reportNotExpected": false }
```

`DELETE` clears `reportText`; it does **not** delete linked report files or automatically change `reportNotExpected`. The user may mark a report as not expected without entering text. Report text has no version history; destructive edits/deletions require the appropriate UI confirmation.

## 9. Documents and event links

### 9.1 Document archive

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/documents` | Search/filter/paginate owned document metadata |
| `GET` | `/api/documents/{id}` | Document metadata and linked-event summary |
| `POST` | `/api/documents` | Upload file and create metadata |
| `PATCH` | `/api/documents/{id}` | Update editable metadata |
| `PUT` | `/api/documents/{id}/file` | Replace binary, preserve document ID and links |
| `GET` | `/api/documents/{id}/file` | Authenticated file retrieval (inline/download policy) |
| `DELETE` | `/api/documents/{id}` | Delete metadata, links, and binary after confirmation |

`POST` uses `multipart/form-data`: part `file` (binary) and part `metadata` (JSON; `application/json`) with fields `title`, `category`, optional `customCategory`, `description`, `documentDate`, `expiryDate`. File limits: PDF/JPEG/PNG, maximum **10 MB** initially (configurable). The server validates actual file content, stores binaries outside publicly served paths, and assigns opaque `storageKey` values. `storageKey` and filesystem paths **must never appear in client DTOs**.

Document response fields: `id`, `title`, `originalFilename`, `mimeType`, `fileSize`, `category`, `customCategory`, `description`, `documentDate`, `expiryDate`, `createdAt`, `updatedAt`. Category codes must match the finalized DB constraint; `OTHER` supports custom labels as specified in database design. Example list filters: `category`, `q`, `expiryFrom`, `expiryTo`, `page`, `size`.

Binary responses must use a safe `Content-Type`, `Content-Disposition`, `X-Content-Type-Options: nosniff`, and private cache policy. Avoid exposing raw user filenames unsafely in headers. Replacement is staged: write and validate new binary, commit metadata, then remove old binary with retryable cleanup. DB transactions cannot atomically commit filesystem operations. Deleting a document removes its event links; deleting an event **does not** delete the document.

### 9.2 Event-document links

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/medical-events/{eventId}/documents` | List linked documents and link-specific roles |
| `POST` | `/api/medical-events/{eventId}/documents` | Link existing owned document |
| `PATCH` | `/api/medical-events/{eventId}/documents/{documentId}` | Change link's `isReport` flag |
| `DELETE` | `/api/medical-events/{eventId}/documents/{documentId}` | Unlink only; retain archived document |

Link request: `{ "documentId": "550e8400-e29b-41d4-a716-446655440050", "isReport": true }`. Link role is **per event**, not a document-global property. Reject duplicate links with `409`. Enforce same-owner event/document association at both service and DB levels where designed.

## 10. Calendar and dashboard

### 10.1 Calendar

`GET /api/calendar?from=2027-03-01&to=2027-03-31&includeDocumentExpiries=true`

Optional filters: `category`, `status`, `includeDocumentExpiries` (default `true`); `from` and `to` are required inclusive local dates. Combine event and document-expiry projections without inserting duplicate calendar rows in the database. **Always exclude cancelled medical events**, even when the status filter requests them; history remains available through `/api/medical-events`.

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440060",
      "type": "MEDICAL_EVENT",
      "title": "Visita cardiologica",
      "date": "2027-03-10",
      "time": "09:30",
      "category": "SPECIALIST",
      "status": "SCHEDULED"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440061",
      "type": "DOCUMENT_EXPIRY",
      "title": "Scadenza certificato medico",
      "date": "2027-03-15",
      "time": null
    }
  ]
}
```

Calendar views (day/week/month/year), colors, and UI-specific grouping are frontend concerns. The API returns source records with stable `type` and identifiers; clicking a day can prefill a create-event form without a special endpoint.

### 10.2 Dashboard

`GET /api/dashboard` returns a compact owner-scoped summary; proposed fields:

```json
{
  "upcomingEvents": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440070",
      "title": "Visita dermatologica",
      "eventDate": "2027-04-12",
      "eventTime": "15:00"
    }
  ],
  "upcomingDocumentExpiries": [],
  "recentEvents": [],
  "missingReportsCount": 2
}
```

Dashboard values are derived from owned records and effective status. The compact calendar can reuse `/api/calendar` with a bounded date interval. Medication/therapy widgets are **post-MVP** and must not appear in the current dashboard contract.

## 11. Security, sessions, and operational behavior

- Use Spring Security **server-side sessions**, not JWT. Cookie: `HttpOnly`, `SameSite=Lax`, `Secure` when HTTPS is used. No authentication secrets in local storage.
- Session inactivity timeout: **30 minutes**. Multiple concurrent sessions allowed. Logout invalidates only current session. Password change invalidates all *other* sessions while preserving current session; account deletion invalidates all sessions.
- Choose and integration-test Spring Session JDBC or an explicitly scoped local session registry before implementing cross-session invalidation. Any session tables are infrastructure, not clinical-domain entities.
- CSRF is enabled on all unsafe methods, including login and logout. `GET /api/auth/csrf` provides token bootstrap; after login/logout, refresh token as needed. Verify behavior with actual Spring Security configuration.
- Enforce owner-scoped repository queries; never use raw IDs alone for private-resource access. Validate catalog visibility, doctor/facility ownership, result parameter/service matching, and event/document same-owner links.
- Do not log passwords, CSRF tokens, session cookies, health payloads, document content, or sensitive exception details. Keep uploads private and prevent path traversal and unsafe file serving.
- No public deployment, external integrations, email flows, or real patient data in MVP. If deployed later, revisit HTTPS, secure cookie settings, reverse proxy, CORS, secret management, rate limits, retention, and privacy controls.

## 12. OpenAPI and test strategy

Springdoc OpenAPI will expose the implemented API and Swagger UI in the development environment. The OpenAPI output must accurately document request/response DTOs, enum values, nullability, multipart uploads, session-cookie security, CSRF header, response codes, and Problem Details errors. This Markdown document records **design intent**; implemented OpenAPI plus automated contract/integration tests must verify it.

Test with JUnit 5, Mockito, MockMvc, and Testcontainers/PostgreSQL. Essential cases:

1. Registration, login, CSRF bootstrap, logout, session expiry, and concurrent-session invalidation.
2. Cross-user access to events, doctors, facilities, catalogs, results, documents, and file downloads; return `404` for private foreign IDs.
3. Duplicate email/catalog entries and archived catalog behavior.
4. Event effective status at past/future/today boundaries, manual overrides, cancellation restoration, and DST transitions.
5. Lab result types, reference interval combinations, parameter/service consistency, duplicate parameters, and atomic reorder.
6. Upload size/content validation, replacement failures, orphan-file cleanup, link/unlink semantics, and account deletion cleanup.
7. Omitted-vs-null PATCH semantics, pagination limits, stable sorting, and consistent Problem Details.
8. Calendar exclusion of cancelled events, document-expiry projection, and missing-report derivation.

## 13. Implementation follow-ups (explicit, not hidden assumptions)

The following details require a **small implementation decision or contract refinement**, not a change to the approved MVP scope:

1. Finalize exact predefined document category codes and whether `OTHER` may omit a custom label, consistent with `03-database-design.md`.
2. Finalize the read-only historical lab-series endpoint and unit-compatibility metadata for charting; do not claim it exists until specified.
3. Finalize the merge transaction/endpoints for duplicate doctors/facilities, preserving event links.
4. Specify a safe file retrieval disposition policy (inline vs attachment), and the precise `multipart/form-data` parser contract in OpenAPI.
5. Confirm Spring Session persistence choice, CSRF repository/token lifecycle, login/logout integration, and cookie settings through integration tests.
6. Define exact query bounds for calendar and the list of supported sort/filter combinations, including performance of effective-status filtering.
7. Define the policy for ambiguous/nonexistent local times at daylight-saving transitions.
8. Resolve any discrepancies between illustrative JSON samples and final generated OpenAPI schemas before frontend/backend implementation.

## 14. Decision record and traceability

- **API-01 (confirmed):** Section-based event management. Create an event first, then manage results, report text, and document links independently.
- **API-02:** REST `/api` + JSON `camelCase`, UUID IDs, ISO date/time formats.
- **API-03:** Server-side sessions with CSRF protection; no JWT.
- **API-04:** RFC 9457 Problem Details with stable `code` and optional `fieldErrors`.
- **API-05:** Owner derived from session; private foreign resources return `404` where appropriate.
- **API-06:** Pagination defaults to 20/max 100; event status is derived when in AUTO mode.
- **API-07:** File upload uses private storage and multipart requests; event-document links are independent resources.

**Next phase:** UI/UX design (`05-ui-ux-design.md`), followed by implementation planning, bootstrap, Flyway migrations, Spring Boot controllers/services, and React features. Do not present this document as a finished implementation or tested OpenAPI contract.
