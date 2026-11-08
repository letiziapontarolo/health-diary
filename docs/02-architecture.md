# Health Diary — Software Architecture Document

**Document ID:** SAD-002  
**Version:** 1.0 (architecture baseline)  
**Status:** Approved design baseline; implementation details subject to validation  
**Related document:** [01-requirements.md](01-requirements.md)  
**Product language:** Italian  
**Source code and technical documentation:** English

## 1. Purpose and architectural scope

This document describes the intended architecture of **Health Diary**, a private multi-user web application for organizing medical events, laboratory results, reports, documents, and appointments. It translates the requirements baseline into component boundaries, technology choices, communication patterns, and operational constraints.

The initial implementation is a **local MVP using synthetic data only**. This architecture is not a claim of production readiness for processing real medical information. Public deployment requires a separate security, privacy, operations, and regulatory assessment.

### 1.1 Architecture principles

1. **Backend-enforced security:** authentication, authorization, validation, and ownership checks are enforced server-side.
2. **Separation of concerns:** presentation, application logic, persistence, and binary file storage have distinct responsibilities.
3. **Incremental delivery:** start with a working vertical slice; add modules without unnecessary infrastructure.
4. **Explicit data contracts:** API DTOs are independent of JPA entities.
5. **Maintainability:** use conventional Java and React patterns, versioned migrations, tests, and documented decisions.
6. **No clinical inference:** the system organizes user-entered information and does not diagnose or recommend treatment.

## 2. System context and deployment topology

### 2.1 Three-tier architecture

```text
                           User's browser
                                 |
                          HTTP(S) / JSON
                                 |
                     +-----------------------+
                     | React + TypeScript    |
                     | Vite SPA              |
                     +-----------+-----------+
                                 |
                         /api/* requests
                                 |
                     +-----------v-----------+
                     | Spring Boot backend   |
                     | REST + Spring Security|
                     | Application services  |
                     +------+-----------+----+
                            |           |
                    JPA / JDBC      File storage API
                            |           |
                 +----------v--+  +-----v----------------+
                 | PostgreSQL  |  | Private filesystem   |
                 | metadata    |  | PDF / JPEG / PNG     |
                 +-------------+  +----------------------+
```

The browser never connects directly to PostgreSQL or to the private document directory. The backend is the sole authority for user identity, resource ownership, business rules, and file access.

### 2.2 Local development topology

| Component | Execution | Typical address | Notes |
|---|---|---|---|
| React/Vite | Host machine | `http://localhost:5173` | Development server and `/api` proxy |
| Spring Boot | Host machine | `http://localhost:8080` | REST API; not directly called by browser during normal development |
| PostgreSQL | Docker Compose | `localhost:5432` | Persistent named Docker volume |
| Document storage | Host filesystem | Configured path | Outside publicly served frontend assets |

**Vite proxy decision:** browser requests use relative URLs such as `fetch('/api/medical-events')`. The Vite development server forwards `/api/*` to Spring Boot. This avoids cross-origin browser API requests during local development. The proxy is **development-only**; a future deployment would need a separate reverse proxy or equivalent same-origin arrangement.

The proxy must preserve request methods, cookies, CSRF headers, and multipart request bodies. Local testing should use a consistent hostname (`localhost`, not a mixture of `localhost` and `127.0.0.1`) to avoid cookie surprises.

### 2.3 Architectural style

The backend is a **modular monolith organized in layers**. Logical modules share one Spring Boot process and one PostgreSQL database; they are not microservices. Calendar and dashboard are primarily read-oriented compositions of existing domain data and do not automatically require dedicated persistence tables.

## 3. Technology baseline

| Concern | Selected technology / approach |
|---|---|
| Frontend | React, TypeScript, Vite |
| Navigation | React Router |
| Server-state management | TanStack Query |
| Local UI state | React hooks; limited context for authentication |
| Forms | React Hook Form + Zod |
| Styling | Tailwind CSS; accessible components as needed |
| Frontend tests | Vitest, React Testing Library; MSW where useful |
| Backend | Java, Spring Boot |
| Authentication/authorization | Spring Security, server-side HTTP sessions |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL |
| Schema migrations | Flyway; Hibernate `ddl-auto: validate` |
| API description | Springdoc OpenAPI / Swagger UI (code-first) |
| Error contract | RFC 9457 Problem Details via `@RestControllerAdvice` |
| Backend tests | JUnit 5, Mockito, Spring Boot Test / MockMvc, Testcontainers |
| Local infrastructure | Docker Compose for PostgreSQL |
| Binary storage | Private filesystem behind a storage abstraction |

Library versions will be pinned to mutually compatible supported releases at project setup rather than assumed in this design document.

## 4. Repository organization

```text
health-diary/
├── frontend/
│   ├── public/
│   ├── src/
│   └── ...
├── backend/
│   ├── src/main/java/com/healthdiary/
│   ├── src/main/resources/
│   └── src/test/
├── docs/
│   ├── 01-requirements.md
│   ├── 02-architecture.md
│   ├── 03-database-design.md
│   ├── 04-api-design.md
│   ├── 05-ui-ux-design.md
│   └── 06-testing-strategy.md
├── compose.yaml
├── .env.example
├── .gitignore
└── README.md
```

Create directories as features are implemented; avoid empty placeholder packages. The `README.md` will document prerequisites, startup, test commands, demo data, and the project’s non-production scope.

## 5. Backend architecture

### 5.1 Package layout

```text
com.healthdiary/
├── HealthDiaryApplication.java
├── controller/
│   ├── auth/
│   ├── profile/
│   ├── catalog/
│   ├── medicalevent/
│   ├── laboratory/
│   └── document/
├── service/
│   ├── auth/
│   ├── profile/
│   ├── catalog/
│   ├── medicalevent/
│   ├── laboratory/
│   └── document/
├── repository/
├── entity/
├── dto/
│   ├── auth/
│   ├── profile/
│   ├── catalog/
│   ├── medicalevent/
│   ├── laboratory/
│   └── document/
├── config/
└── exception/
```

### 5.2 Layer responsibilities

| Layer | Responsibilities | Avoid |
|---|---|---|
| Controller | HTTP mapping, request DTO validation, response status/DTO | Business logic and direct repository access |
| Service | Ownership enforcement, business rules, transactions, orchestration | HTTP-specific concerns and raw response formatting |
| Repository | Scoped database queries and persistence | Authorization decisions based on untrusted client identity |
| Entity | JPA persistence model and relationships | Direct serialization to public API |
| DTO | Explicit request and response contracts | Exposing passwords, internal storage paths, or unnecessary user data |
| Config | Spring Security, storage, CORS/proxy-related deployment settings | Domain business logic |
| Exception | Domain errors and consistent HTTP Problem Details | Leaking stack traces or internal exception messages |

Typical flow: **Controller → Service → Repository → PostgreSQL**. Controllers map DTOs; services determine the current user from trusted security context. All queries for user-owned resources must be scoped to that user, for example `findByIdAndUserId(...)`, with equivalent ownership checks on updates, downloads, and relationship changes.

### 5.3 Logical backend modules

| Module | Scope |
|---|---|
| Auth | Registration, login/logout, session lifecycle, password changes |
| Profile | Personal details, email changes, account deletion |
| Medical catalog | Shared predefined and user-owned custom services; archiving |
| Medical events | Visits, diagnostics, lab sessions, status transitions, clinicians/facilities |
| Laboratory | Parameter catalog, results, reference intervals, historical series |
| Documents | Private metadata, file lifecycle, many-to-many event links |
| Calendar | Date-range projections and category filters |
| Dashboard | Upcoming appointments, compact calendar, shortcuts |

Future modules (medication, vaccinations, hospitalizations, emergency visits, procedures, measurements, allergies, conditions, PDF export) should follow the same boundaries and remain out of the MVP unless explicitly reprioritized.

### 5.4 DTOs and API contracts

Use distinct request and response DTOs, for example `CreateMedicalEventRequest`, `UpdateMedicalEventRequest`, `MedicalEventResponse`, and `MedicalEventSummaryResponse`. A calendar query returns compact event summaries rather than loading all report text, laboratory results, and attachments. Use server-side pagination for medical-event lists (20 items per page by default), filtering, and sorting. Document precise endpoint contracts in `04-api-design.md`.

### 5.5 Transactions and consistency

Use `@Transactional` for related database writes. Removing a laboratory event deletes its results and document links but **does not delete archived documents**. Deleting a document removes all links and its metadata and schedules removal of its physical file. Filesystem operations are not part of a database transaction; section 8 defines failure-handling expectations.

### 5.6 Event status handling

Medical events have `SCHEDULED`, `COMPLETED`, and `CANCELLED` states. The design must distinguish **automatic** and **manual** status selection (for example with `status_mode = AUTO | MANUAL` or an equivalent explicit override field). Automatic state derives from event date, optional time, and a consistent application time zone; events dated today without a time remain scheduled until the next day. A manual override takes precedence. Restoring a cancelled event resumes automatic status computation, as required by the SRS. The database design must capture the necessary fields; derived status may be computed at read time or persisted with a clearly defined update strategy.

## 6. Frontend architecture

### 6.1 Feature-based layout

```text
frontend/src/
├── app/
│   ├── App.tsx
│   ├── router.tsx
│   └── providers/
├── features/
│   ├── auth/
│   ├── profile/
│   ├── dashboard/
│   ├── medical-catalog/
│   ├── medical-events/
│   ├── laboratory/
│   ├── documents/
│   └── calendar/
├── components/
│   ├── ui/
│   ├── layout/
│   └── icons/           # medical icon registry and category fallbacks
├── assets/
│   └── medical-icons/   # approved custom SVG assets
├── hooks/
├── services/
│   └── apiClient.ts
├── types/
├── utils/
├── styles/
└── main.tsx
```

Each feature may contain `pages/`, `components/`, `api/`, `hooks/`, `types/`, and `utils/` as needed. Shared primitives belong in `components/ui`; layout elements belong in `components/layout`. Medical service icons are resolved by an explicit key-to-component/asset registry in `components/icons`; approved SVG assets live in `assets/medical-icons`. The database stores only optional symbolic `icon_key` values, never SVG markup or arbitrary URLs. The backend validates user-selected keys against an approved catalog, and the frontend provides category fallbacks. Avoid moving feature-specific code into global folders prematurely.

### 6.2 Routes and layout

| Route | Access | Purpose |
|---|---|---|
| `/login` | Public | Sign in |
| `/register` | Public | Create account |
| `/dashboard` | Authenticated | Summary |
| `/medical-events` | Authenticated | List and filters |
| `/medical-events/new` | Authenticated | Create event |
| `/medical-events/:id` | Authenticated | Event detail/edit |
| `/documents` | Authenticated | Document archive |
| `/calendar` | Authenticated | Day/week/month/year views |
| `/profile` | Authenticated | Profile and account settings |

The application uses a common authenticated layout with a desktop sidebar and mobile hamburger menu. The medical-events page has category tabs (`Tutte`, `Analisi`, `Visite`, `Diagnostica`). The UI and validation messages are in Italian.

### 6.3 State ownership

- **Local UI state:** field values, open dialogs, temporary filters, mobile navigation (`useState` / `useReducer`).
- **Server state:** lists, details, calendar data, document metadata (TanStack Query with targeted invalidation after mutations).
- **Authentication state:** a small auth provider or query-backed session context populated by `/api/auth/me`.

Do not store session identifiers, passwords, or medical data in browser local storage as an authentication mechanism. TanStack Query caching is in memory by default; avoid persistent browser caches of sensitive data in the MVP.

### 6.4 Forms and navigation safety

Use React Hook Form with Zod for client-side validation; the backend remains authoritative. The medical-event workflow saves general information first, creating the event and unlocking independently savable laboratory results and report/document sections. Warn before discarding unsaved edits; use the browser's unload warning where supported. Preserve entered values after validation errors.

### 6.5 Accessibility

Target WCAG 2.2 AA where applicable: keyboard navigation, focus management in dialogs, explicit labels, contrast, and category text alongside colors. Compliance must be verified rather than assumed.

## 7. Authentication, authorization, and CSRF

### 7.1 Session lifecycle

1. Browser obtains a CSRF token using an unauthenticated-safe endpoint such as `GET /api/auth/csrf`.
2. Login request submits credentials and the required CSRF token.
3. Spring Security authenticates the user, rotates the session identifier as appropriate, and establishes a server-side session.
4. Browser stores the session cookie; React retrieves current-user information from `GET /api/auth/me`.
5. Subsequent requests carry the cookie automatically on the same origin.
6. On session expiry, protected APIs return `401`; React clears authenticated UI state and routes to login.
7. Logout invalidates only the current session; password changes invalidate all **other** sessions while preserving the current one.

**Cookie policy:** `HttpOnly`, `SameSite=Lax`, and `Secure` under HTTPS. The session idle timeout is **30 minutes**, enforced by the server. Multiple concurrent sessions are allowed. Passwords are hashed using bcrypt or Argon2id; no plaintext storage or JWT authentication for the MVP.

**Session registry:** invalidating other sessions requires a reliable session-tracking strategy. The chosen implementation must be tested across concurrent browser sessions; do not assume Spring Security's defaults automatically satisfy this requirement.

### 7.2 CSRF and browser security

CSRF protection remains enabled for state-changing requests, including login and logout. A CSRF endpoint can expose the token to same-origin JavaScript while keeping the **session cookie** HttpOnly. The shared API client attaches the token to mutation requests, refreshes it when authentication/session rotation requires, and handles `403` CSRF failures distinctly from `401` session expiry. `SameSite=Lax` is defense in depth, not a substitute for CSRF validation.

Local Vite proxying removes the normal need for CORS between browser and API; do not enable unrestricted CORS. A later multi-origin deployment would require a narrowly scoped CORS and cookie policy review.

### 7.3 Resource ownership and errors

Every operation on a private resource must verify ownership, including documents, linked events, custom catalog entries, and downloads. Client-provided IDs identify resources but **never** establish authorization. For inaccessible or nonexistent private objects, a consistent `404` response can avoid revealing their existence. Centralize API errors using RFC 9457 Problem Details; never return stack traces or internal file paths.

### 7.4 Logging and secrets

Do not log passwords, session IDs, CSRF tokens, report contents, laboratory values, or raw document contents. Keep credentials out of source control and supply them through environment configuration. Use generic error messages where appropriate while preserving enough non-sensitive diagnostic information for local development.

## 8. Document storage architecture

### 8.1 Metadata versus binary content

PostgreSQL stores document ownership, original filename, media type, storage identifier, category, dates, expiry, and event links. Binary content lives in a **private directory** outside Vite's `public/` and Spring's static resource paths. Stored filenames are generated opaque identifiers; original filenames are metadata only.

Define a storage abstraction, for example `FileStorageService`, so the application layer does not depend on direct filesystem APIs. The initial implementation uses local storage; remote object storage is a future possibility, not an MVP requirement.

### 8.2 Upload and download

Upload via authenticated `multipart/form-data`; enforce CSRF, ownership, configured size limits (initial target **10 MB/file**), allowlisted PDF/JPEG/PNG types, and server-side content validation. Do not trust filename extensions or client-provided MIME types alone. Sanitize download filenames and set safe response headers; consider attachment downloads as the default for untrusted content. Files are accessed through authenticated endpoints such as `GET /api/documents/{id}/download`, never public filesystem URLs.

### 8.3 Replace, unlink, and delete

- **Unlink:** remove an event-document relationship only; retain the archived document and file.
- **Replace:** validate and stage a new file, update the document's storage reference without changing its ID, metadata, or links, then remove the old file only after the new reference is durable.
- **Delete:** require confirmation, show linked events, remove links and metadata, and remove the physical file; recompute affected missing-report indicators.
- **Delete medical event:** remove event links and dependent lab results, but retain documents.

Database transactions cannot atomically commit filesystem operations. Implement staged writes, cleanup after failed operations, and an idempotent reconciliation/cleanup strategy for orphaned or pending files. Never delete the previous binary before a replacement has safely succeeded. The exact lifecycle/state mechanism belongs in database and API design.

## 9. Data persistence and migrations

PostgreSQL is the authoritative store for structured data. Use UUID primary keys where specified in the SRS; enforce foreign keys, uniqueness, and relevant check constraints at the database level in addition to service validation.

- **Shared catalog entries:** represented by nullable owner IDs where the SRS requires predefined entries shared across accounts.
- **Private entities:** carry explicit ownership and are always queried within the authenticated user's scope.
- **Documents:** linked to multiple medical events through a join table, including per-link report semantics.
- **Laboratory results:** preserve result-specific numeric/text values, optional units, and historical reference intervals; charting must not mix uncertain unit compatibility.
- **Historical integrity:** archiving catalog entries must not break existing events; deletion restrictions follow the SRS.

Flyway migrations live in `backend/src/main/resources/db/migration/`, e.g. `V1__create_users_table.sql`. Hibernate uses `ddl-auto: validate`, not `update`. Detailed tables, indexes, constraints, and ER relationships belong in `03-database-design.md`.

## 10. API and error design

The API is REST-oriented, JSON for structured data, and multipart/binary for documents. Use consistent resource names under `/api`, correct HTTP statuses (`201` for creation, `400` validation failures, `401` unauthenticated, `403` forbidden/CSRF, `404` unavailable, `409` conflicts, `500` unexpected failures), and Problem Details for errors. Avoid exposing JPA entities directly. Document the contract with Springdoc OpenAPI and Swagger UI.

Illustrative flow for creating an event:

```text
POST /api/medical-events
  -> session + CSRF verification
  -> request DTO validation
  -> current-user lookup
  -> accessible service lookup
  -> business-rule validation and status initialization
  -> transactional persistence
  -> 201 Created + response DTO
```

Endpoint naming, request/response schemas, pagination parameters, and error examples are specified later in `04-api-design.md`.

## 11. Local environment and configuration

### 11.1 Docker Compose

Compose runs **PostgreSQL only** for initial development. Use a named persistent volume and a database health check. Frontend and backend run on the host for rapid feedback and debugging. Testcontainers supplies isolated PostgreSQL instances for integration tests.

### 11.2 Configuration and secrets

Commit `.env.example` with non-secret placeholders, but exclude `.env`, runtime document storage, build outputs, and IDE-specific files from Git. Illustrative keys:

```dotenv
POSTGRES_DB=health_diary
POSTGRES_USER=health_diary
POSTGRES_PASSWORD=change_me
DB_HOST=localhost
DB_PORT=5432
DOCUMENT_STORAGE_PATH=./storage/documents
```

Compose can read `.env` for interpolation; **Spring Boot does not automatically consume every Compose `.env` variable when started on the host**. Explicitly export environment variables or configure an intentional local loader/profile. Document startup and teardown commands in the README. Do not commit real credentials.

### 11.3 Runtime environments

- **Development:** local frontend/backend, containerized PostgreSQL, synthetic sample data.
- **Test:** automated unit and integration tests, isolated test database.
- **Production:** not defined for the MVP; requires additional deployment, HTTPS, backups, monitoring, security, and privacy design.

## 12. Quality attributes and verification

| Attribute | Architectural approach | Verification direction |
|---|---|---|
| Security | Session auth, CSRF, owner-scoped queries, private file endpoints | Unauthorized cross-user API/file tests |
| Integrity | Transactions, FKs, staged file lifecycle | Rollback, replacement-failure, delete/link tests |
| Performance | Pagination, scoped queries, indexed filters, compact DTOs | Defined-load response-time tests; target ≤2 s for common operations |
| Usability | Italian UI, responsive layout, accessible forms | Keyboard, mobile, and representative user-flow tests |
| Maintainability | Layer boundaries, feature folders, DTOs, migrations | Unit/integration tests, lint/format checks |
| Observability | Structured, non-sensitive error logging | Failure scenarios without sensitive-data leakage |

Automated tests are written incrementally. Backend: JUnit 5, Mockito, MockMvc/Spring Boot Test, Testcontainers. Frontend: Vitest and React Testing Library, with MSW as useful. Test priorities include authorization, status overrides, result validation, document lifecycle, and calendar filtering.

## 13. Architectural decisions (ADR summary)

| ID | Decision | Rationale / consequence |
|---|---|---|
| ADR-001 | Three-tier SPA + modular Spring Boot monolith | Clear responsibilities without microservice overhead |
| ADR-002 | Layer-based backend packages | Conventional separation; modules represented within layers |
| ADR-003 | Feature-based React structure | Feature cohesion and gradual growth |
| ADR-004 | Vite `/api` development proxy | Same-origin browser API calls locally; not a production proxy |
| ADR-005 | Server-side Spring Security sessions | No JWT lifecycle in frontend; CSRF protection required |
| ADR-006 | Explicit request/response DTOs | API stability and data minimization |
| ADR-007 | PostgreSQL + Flyway + schema validation | Reproducible and reviewed schema changes |
| ADR-008 | Private filesystem + storage interface | Simple local MVP; secure access and future replacement |
| ADR-009 | TanStack Query + local React hooks | Distinguish remote data from transient UI state |
| ADR-010 | React Hook Form + Zod, Tailwind CSS | Maintainable forms and responsive styling |

## 14. Follow-up design tasks and boundaries

The architecture baseline is sufficient to begin detailed design. The following are **implementation/design tasks**, not invitations to reopen approved product requirements:

1. **Database design:** entities, keys, constraints, event status mode, ownership, session registry strategy, and file lifecycle metadata.
2. **API design:** exact DTOs, CSRF bootstrap/login/logout contract, ownership errors, upload/download, pagination, and calendar range queries.
3. **UI/UX design:** layout, navigation, form sections, modal behavior, responsive breakpoints, and accessibility acceptance checks.
4. **Testing strategy:** concrete fixtures, concurrency/session tests, file failure injection, and performance test conditions.
5. **Implementation bootstrap:** select compatible dependency versions, configure Spring Security session tracking, Vite proxy, Compose, and environment loading.

**Out of scope for this baseline:** production deployment, cloud storage, mobile-native applications, offline operation, reminders, automatic medical advice, and the future feature modules listed in the SRS.

---

**Baseline conclusion:** Health Diary will be developed as a React/TypeScript SPA communicating through a same-origin development proxy with a Spring Boot modular monolith, PostgreSQL, and private file storage. Security and user ownership are enforced in the backend. The next design artifact is **`03-database-design.md`**.
