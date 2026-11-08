# Health Diary — Development Plan

**Status:** Approved plan  
**Version:** 1.0  
**Scope:** Local-only MVP using synthetic demonstration data  
**Related documents:** `01-requirements.md`, `02-architecture.md`, `03-database-design.md`, `04-api-design.md`, `05-ui-ux-design.md`

## 1. Purpose and delivery approach

Deliver Health Diary incrementally as a private, multi-user health diary application. Each milestone should produce a testable, integrated increment rather than a collection of disconnected screens or endpoints. No real patient data, public deployment, diagnostic interpretation, or production-security claims are in scope for the MVP.

The delivery cycle for each feature is: **backend implementation → backend/API tests → frontend implementation → integration → acceptance checks**. Infrastructure and cross-cutting security tasks are addressed as soon as they become necessary, not deferred to the final milestone.

### Engineering principles

- Keep `main` runnable; work in short-lived `feature/...` branches and merge through reviewed pull requests where practical.
- Use English for code, API, database identifiers, documentation, and commit messages; use Italian for UI text and validation feedback.
- Make changes traceable to the requirements, database schema, API contract, and UI/UX specification.
- Use Flyway for all schema changes; do not rely on Hibernate automatic schema mutation.
- Apply per-user ownership checks on every protected resource and related entity.
- Use synthetic demo data only. Never commit credentials, private documents, secrets, or local `.env` files.
- Run automated tests and a brief manual acceptance check before closing a milestone.

## 2. Milestone roadmap

| ID | Milestone | Dependency | Demonstrable outcome |
|---|---|---|---|
| M1 | Project Setup | None | Frontend, backend and PostgreSQL run locally; first API call succeeds |
| M2 | Authentication & Profile | M1 | Users register, log in, manage profile and securely log out |
| M3 | Medical Services | M2 | Shared/custom services, icons, doctors and facilities are manageable |
| M4 | Medical Events | M3 | Users create, browse, update, cancel and delete medical events |
| M5 | Laboratory Results | M4 | Users record results and inspect historical parameter series |
| M6 | Document Archive | M4 | Users securely upload, preview, link and manage documents |
| M7 | Calendar & Dashboard | M4, M5, M6 | Unified calendar and dashboard aggregate existing data |
| M8 | Testing & Polish | All previous | MVP is documented, accessible-oriented and portfolio-ready |

**Dependency note:** M5 and M6 can be developed in either order after M4, but the initial sequence is M5 then M6. M7 must wait until document expiry and report-completeness data are available. Milestone numbers are planning labels, not time estimates.

## 3. M1 — Project Setup

**Goal:** Establish a reproducible local development environment and working end-to-end connection.

### Tasks

1. Create the GitHub repository `health-diary` and the monorepo layout: `backend/`, `frontend/`, `docs/`, `compose.yaml`, `.env.example`, `.gitignore`, `README.md`.
2. Add the six approved design documents to `docs/` and describe the local-only scope in `README.md`.
3. Initialize Spring Boot with Java, Maven, Spring Web, Spring Security, Spring Data JPA, PostgreSQL driver, Flyway, validation, and test dependencies; include Springdoc OpenAPI when compatible.
4. Initialize React + TypeScript + Vite with React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS, Vitest and React Testing Library.
5. Define a PostgreSQL service in Docker Compose, with development-only configuration and persistent local volume.
6. Configure backend database connectivity through environment variables and Spring profiles; use Flyway for a minimal initial migration.
7. Configure the Vite `/api` proxy to the local backend and implement a small, deliberately public health-check endpoint; keep all future application routes protected by default.
8. Add minimal backend and frontend smoke tests, formatting/lint scripts, and startup instructions.
9. Ensure the `.gitignore` excludes local secrets, generated build files, and private uploaded content.

### Acceptance criteria

- `docker compose up -d` starts PostgreSQL; backend connects successfully.
- Flyway applies the initial migration from a clean database.
- Backend starts and the health-check endpoint responds successfully.
- Frontend starts and calls the backend through relative `/api` requests.
- Backend and frontend smoke tests pass.
- A new developer can follow the README to reproduce the local setup.
- No credentials or private data are committed.

**Out of scope:** User registration, health records, document uploads, deployment.

## 4. M2 — Authentication & Profile

**Goal:** Implement authenticated sessions and private user profiles.

### Tasks

- Create `users` schema and persistence model with case-insensitive unique email and agreed profile fields.
- Implement registration, login, logout, current-session user, and CSRF bootstrap endpoints.
- Configure server-side sessions, 30-minute inactivity timeout, HttpOnly/SameSite cookies, secure password hashing, CSRF enforcement, and session fixation protection.
- Implement profile read/update, email change requiring current password, password change requiring current password and invalidation of other sessions, and confirmed account deletion.
- Build Italian-language registration, login, profile, security, and account-management screens.
- Implement protected routes, loading/expired-session states, accessible forms and field-level validation.

### Acceptance criteria

- Registration creates an account but does not automatically log in.
- Login establishes a server-side session; logout invalidates the current session only.
- Unauthenticated requests to private endpoints are rejected.
- CSRF-protected mutations reject missing/invalid tokens.
- Two users cannot access each other's profiles or private resources.
- Email uniqueness, password rules, password change, session expiry, and account deletion are tested.
- Other sessions are invalidated after password change while the current session remains active.

**Implementation decision to finalize:** Choose a concrete session registry/storage strategy supporting other-session invalidation (e.g. Spring Session JDBC), compatible with the local stack.

## 5. M3 — Medical Services

**Goal:** Provide the catalog and private reference data used when creating events.

### Tasks

- Create `medical_services`, `doctors`, and `facilities` tables and Flyway migrations.
- Seed shared predefined services with category and optional `icon_key`; allow user-owned custom services.
- Implement search, creation, editing, archive/reactivation behavior and deletion rules for services.
- Enforce case-insensitive uniqueness per owner/category according to the database design, including archived entries.
- Implement private doctor/facility autocomplete and creation; define safe merge behavior for duplicates.
- Create a frontend medical-icon registry mapping symbolic keys to Lucide components or reviewed local SVG assets, with category fallback.
- Build service-management UI and reusable service/doctor/facility selectors with inline creation.

### Acceptance criteria

- Users see shared services plus only their own custom services.
- `iconKey` is optional and unknown keys fall back gracefully.
- Archived services remain visible on historical records but cannot be chosen for new events.
- Cross-user access and invalid category/name updates are rejected.
- Duplicate doctor/facility handling preserves historical links.

## 6. M4 — Medical Events

**Goal:** Make the core health diary usable.

### Tasks

- Create `medical_events` schema and ownership-aware API operations.
- Implement event creation and editing with required service/date and optional time, doctor, facility, notes, report text and status override.
- Implement effective `SCHEDULED`/`COMPLETED`/`CANCELLED` status using `AUTO`/`MANUAL`, Europe/Rome local-time rules, and explicit cancellation/restoration.
- Implement event listing, search, combinable filters, pagination (default 20), and agreed ordering.
- Implement separate report-text update/removal and derive the missing-report indicator (initially from report text; extend when results/documents exist).
- Build event cards, tabs, create/edit pages, detail view, confirmation dialogs, and responsive layouts.

### Acceptance criteria

- Creating an event requires only the agreed minimum fields.
- Auto-status boundary cases (past, future, today with/without time) are tested.
- Manual overrides and cancellation restoration behave as specified.
- Search/filter/pagination and owner isolation work.
- Event deletion requires explicit confirmation; no unrelated data is deleted.

**Follow-up:** Specify behavior for ambiguous local times around daylight-saving transitions before implementation.

## 7. M5 — Laboratory Results

**Goal:** Record structured laboratory observations and display safe historical trends.

### Tasks

- Add parameter catalog, measurement units and `exam_results` tables with constraints and migrations.
- Implement predefined/user-defined parameters per laboratory service and archive behavior.
- Implement result CRUD, ordering, numeric/text/unavailable values, optional units, and per-result reference interval variants.
- Enforce that a result's parameter belongs to the event's laboratory service; allow repeated parameter entries when intended.
- Implement an ownership-checked historical-series endpoint, specifying filtering and sorting in OpenAPI.
- Build laboratory views, parameter/result forms, reference-range indicators, charts and accessible data-table alternatives.
- Exclude nonnumeric and unavailable values from numeric charts; separate incomparable or unknown units without automatic conversion.

### Acceptance criteria

- Laboratory events can exist without results.
- Valid numeric, text and unavailable results save and reload correctly.
- Each result retains its own historical reference interval.
- Out-of-range indicators reflect only the entered interval, not medical diagnosis.
- Charts do not mix incompatible units; source event links work.
- Tests cover validation, repeated parameters, ordering and ownership.

## 8. M6 — Document Archive

**Goal:** Provide private file storage and event-document associations.

### Tasks

- Create `documents` and `event_documents` tables and private storage configuration outside public directories.
- Implement authenticated multipart upload for PDF/JPEG/PNG, configurable initial 10 MB limit, content validation and opaque storage keys.
- Implement secure file download/preview, metadata update, replacement, deletion, search/filter/sort and document expiry fields.
- Implement many-to-many event linking and per-link `is_report` flags; unlink without deleting document.
- Add safe staged writes and cleanup paths for failed uploads, replacement and deletion.
- Build archive grid/list views, upload form, preview/detail page, linked-event management, and expiry badges.
- Extend missing-report derivation to include linked report documents and laboratory results.

### Acceptance criteria

- Users cannot read or modify another user's documents, even by guessing IDs or storage keys.
- Invalid file types, oversized files and malformed uploads are rejected.
- Upload, preview/download, replace, unlink and delete flows work.
- Deleting an event does not delete archived documents; deleting a document removes its links.
- Failed operations do not leave unintended accessible files or broken references.

## 9. M7 — Calendar & Dashboard

**Goal:** Expose a coherent overview of events, deadlines and actionable summaries.

### Tasks

- Implement calendar read API merging non-cancelled medical events with document expiries and supporting agreed filters/date bounds.
- Build day/week/month/year calendar views, month default, with category labels/icons, date navigation, event summary dialog and prefilled event creation.
- Show untimed events before timed events in day/week views; adapt mobile views appropriately.
- Implement dashboard summary endpoint for upcoming events, missing reports, document count and compact calendar.
- Build dashboard summary cards, upcoming appointments and quick actions.
- Handle empty states, loading, errors, timezone boundaries and keyboard interactions.

### Acceptance criteria

- All four calendar views show consistent events and document expiries.
- Cancelled events are absent from calendar but remain in event history.
- Clicking an event opens its summary and detail link; clicking a day pre-fills creation date.
- Dashboard counts match authoritative underlying records.
- No reminders, notifications, invented health scores or diagnosis claims appear.

## 10. M8 — Testing, Accessibility & Portfolio Polish

**Goal:** Stabilize and document the local MVP.

### Tasks

- Run full backend unit/integration/API tests (JUnit 5, Mockito, MockMvc, Testcontainers) and frontend unit/component tests (Vitest, React Testing Library).
- Add end-to-end checks for critical flows: register/login, create event, record results, upload/link document, view calendar/dashboard, logout.
- Test authorization, CSRF, upload validation, session expiry, error handling, and account deletion with synthetic data.
- Review keyboard navigation, labels, focus states, contrast, responsive layout and screen-reader semantics against WCAG 2.2 AA targets; document any gaps rather than claiming certification.
- Validate migration from an empty database, startup instructions, seed/demo data and reproducibility.
- Refine README with architecture overview, screenshots (synthetic data), setup/test commands, limitations and portfolio presentation.
- Review dependency licenses, especially icon assets, and exclude secrets/private files from version control.

### Acceptance criteria

- Documented automated test suite passes locally.
- Critical user flows work end to end with demo data.
- Known defects and accessibility gaps are recorded and prioritized.
- A clean checkout can be set up using the README.
- The repository clearly distinguishes implemented MVP features from future ideas.

## 11. Testing strategy

| Level | Focus | Primary tools |
|---|---|---|
| Backend unit | Business rules, effective status, validation | JUnit 5, Mockito |
| Backend integration | Persistence, migrations, constraints | Spring Boot Test, Testcontainers/PostgreSQL |
| API/controller | HTTP status, JSON contracts, authorization, CSRF | MockMvc |
| Frontend unit/component | Forms, states, interactions, rendering | Vitest, React Testing Library |
| End-to-end/manual | Critical workflows and responsive behavior | Browser checks; optional automated E2E tooling |
| Security checks | Cross-user isolation, session lifecycle, uploads | Automated negative tests + manual review |

Each milestone must include tests for its new business rules and at least one end-to-end/manual acceptance scenario. A successful UI demonstration does not replace API and security tests.

## 12. Version control and definition of done

### Suggested workflow

1. Start from updated `main` and create a focused branch such as `feature/authentication`.
2. Implement a small vertical slice, including relevant tests and documentation changes.
3. Run lint/build/tests locally and inspect the diff for secrets and accidental artifacts.
4. Open a pull request with a concise description, verification steps and screenshots where useful.
5. Merge after checks pass; keep `main` usable.

### Definition of done for each milestone

- Agreed acceptance criteria are met and demonstrated.
- Database changes are migrated with Flyway, not manual ad hoc schema edits.
- Backend and frontend behavior match the documented API and UI conventions.
- Ownership, validation, and security implications have been tested.
- Tests/builds pass and important error/empty/loading states are handled.
- Documentation and implementation remain consistent.

## 13. Open decisions and implementation checkpoints

The following items are intentionally left for the appropriate milestone, without blocking the overall plan:

- **M1:** Confirm compatible Spring Boot/Java/Node/tool versions, development environment and health-check endpoint convention.
- **M2:** Finalize session registry/storage strategy and concrete CSRF cookie/header handshake.
- **M3:** Finalize predefined service seed data, icon-key registry, and doctor/facility merge API.
- **M4:** Finalize daylight-saving-time edge cases and report-completeness API representation.
- **M5:** Specify historical parameter-series endpoint and behavior for unknown units.
- **M6:** Finalize document-category codes, multipart/preview contract, and file-replacement cleanup policy.
- **M7:** Define calendar date-range limits and detailed ordering for overlapping entries.
- **M8:** Record accessibility audit outcomes and portfolio screenshots using synthetic data.

Update the relevant design document and OpenAPI specification whenever one of these decisions is resolved.

## 14. MVP exclusions and future roadmap

Not included in these milestones: medication schedules, vaccinations, hospitalizations, emergency visits, surgeries, personal measurements, allergies/intolerances, conditions, PDF export, automatic reminders/notifications, offline mode, native mobile apps, or public hosting. These remain potential post-MVP extensions and must not be represented as implemented features.

## 15. Immediate next action

Begin **M1 — Project Setup** with repository creation and a local tooling/version check. Proceed interactively, one meaningful step at a time, verifying each setup action before moving on.
