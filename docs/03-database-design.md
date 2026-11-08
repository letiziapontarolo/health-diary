# Health Diary — Database Design Document

**Document ID:** DBD-003  
**Version:** 1.0 (design baseline)  
**Status:** Design baseline; SQL migrations and integration tests pending  
**Related documents:** [01-requirements.md](01-requirements.md), [02-architecture.md](02-architecture.md)  
**Database:** PostgreSQL; schema changes managed by Flyway  
**Product language:** Italian; database identifiers and technical documentation: English

## 1. Purpose and scope

This document defines the relational model for the Health Diary **local MVP with synthetic data only**. It specifies entities, ownership, cardinalities, data types, constraints, deletion semantics, indexing strategy, and implementation decisions. It is a design specification, **not executable migration SQL**. Before implementation, exact constraint names, Spring Session schema, query plans, and file lifecycle behavior must be validated with integration tests.

The model supports users, medical service catalogs, medical events, doctors, facilities, laboratory parameter catalogs, measurements, documents, and event-document links. Calendar and dashboard are **projections** of these tables; no duplicate calendar-event table is required. Planned medications, vaccinations, hospitalizations, surgeries, measurements, allergies, and PDF export are outside the MVP.

## 2. Design conventions

- Main entity identifiers use `UUID`; generate server-side. Join tables may use composite keys.
- Store creation/update instants as `TIMESTAMPTZ` in UTC; render in the user's locale. `DATE` and optional `TIME` represent entered local medical scheduling values, not UTC instants.
- Store fixed business codes as `VARCHAR` with `CHECK` constraints (or an equivalent validated enum mapping); avoid implicit database enum migrations for the MVP.
- A nullable `user_id` on **shared catalogs only** means a globally predefined entry; non-null means a private entry. Personal clinical records always have a non-null owner.
- Names are trimmed and validated by the backend. Case-insensitive uniqueness is enforced using expression indexes on `LOWER(BTRIM(name))`; these indexes do not fully normalize Unicode or whitespace. The backend must apply the same documented canonicalization policy.
- `NOT NULL`, `CHECK`, `UNIQUE`, and `FOREIGN KEY` constraints enforce local invariants. Cross-table category/access rules require backend validation and concurrency-aware tests.
- Use explicit DTOs, not JPA entities, for API serialization. Never trust an owner ID supplied by the client.

## 3. Conceptual relationships

```text
users 1 ── N medical_events N ── 1 medical_services
  │                │
  │                ├── N ── 0..1 doctors
  │                ├── N ── 0..1 facilities
  │                ├── 1 ── N exam_results N ── 1 lab_parameters
  │                │                           └── N ── 0..1 measurement_units
  │                └── 1 ── N event_documents N ── 1 documents
  │
  ├── 1 ── N documents
  ├── 1 ── N doctors
  ├── 1 ── N facilities
  └── 1 ── N personal medical_services / lab_parameters

medical_services 1 ── N lab_parameters (laboratory services only)
```

A medical event with a `LABORATORY` service is itself the laboratory session. There is **no** `laboratory_exams` table. A laboratory event can have zero or more results. The same parameter may occur more than once in one event. Documents can be standalone or linked to multiple events; the role of a document as a report belongs to the link.

## 4. Table specifications

Types and nullability below are the proposed baseline. Every table with `updated_at` must update it on mutation (application-managed or DB trigger, consistently chosen).

### 4.1 `users`

| Column | PostgreSQL type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | PK |
| `first_name` | `VARCHAR(100)` | NOT NULL, nonblank |
| `last_name` | `VARCHAR(100)` | NOT NULL, nonblank |
| `date_of_birth` | `DATE` | NOT NULL; cannot be future at application validation time |
| `email` | `VARCHAR(255)` | NOT NULL; unique case-insensitively |
| `password_hash` | `VARCHAR(255)` | NOT NULL; secure password hash only |
| `height_cm` | `NUMERIC(5,2)` | NULL; if present, positive and within reasonable app-defined range |
| `city` | `VARCHAR(150)` | NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

Unique expression index: `LOWER(BTRIM(email))`. Do not store plaintext passwords. Profile avatar initials are derived; no avatar column.

### 4.2 `medical_services`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | PK |
| `user_id` | `UUID` | NULL for global catalog, otherwise FK to `users.id` |
| `name` | `VARCHAR(200)` | NOT NULL, nonblank |
| `category` | `VARCHAR(30)` | NOT NULL; `LABORATORY`, `SPECIALIST`, `DIAGNOSTIC` |
| `icon_key` | `VARCHAR(100)` | NULL; symbolic key in the frontend icon registry, not image bytes or an arbitrary URL |
| `is_active` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

`icon_key` is optional to support incremental curation of seeded services. Validate supplied keys against an approved, versioned list; historical service references remain stable when icon mappings change. Unknown or removed keys render a category fallback. SVG assets are stored with frontend source assets, not in PostgreSQL.

Separate partial unique indexes: `(LOWER(BTRIM(name)), category) WHERE user_id IS NULL` and `(user_id, LOWER(BTRIM(name)), category) WHERE user_id IS NOT NULL`. Archived rows still participate. The backend additionally rejects user-created entries duplicating accessible global entries, with a concurrency policy to be tested. Global catalog entries are immutable to ordinary users. Referenced personal services may be archived, not hard-deleted. Renaming a personal service changes its name in historical event views because the name is not snapshotted. Changing a service category is prohibited while events reference it.

### 4.3 `doctors` and `facilities`

Each table contains `id UUID PK`, `user_id UUID NOT NULL FK users`, `name VARCHAR(200) NOT NULL`, `created_at TIMESTAMPTZ NOT NULL`, and `updated_at TIMESTAMPTZ NOT NULL`. Names must be nonblank. Consider unique `(user_id, LOWER(BTRIM(name)))` after validating intended duplicate-handling behavior. A merge operation reassigns all referenced events transactionally before deleting an unused duplicate. Shared doctors/facilities are not part of the MVP.

### 4.4 `medical_events`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | PK |
| `user_id` | `UUID` | NOT NULL FK `users.id` |
| `service_id` | `UUID` | NOT NULL FK `medical_services.id` |
| `doctor_id` | `UUID` | NULL FK `doctors.id` |
| `facility_id` | `UUID` | NULL FK `facilities.id` |
| `event_date` | `DATE` | NOT NULL |
| `event_time` | `TIME` | NULL |
| `status` | `VARCHAR(20)` | NOT NULL; `SCHEDULED`, `COMPLETED`, `CANCELLED` |
| `status_mode` | `VARCHAR(10)` | NOT NULL DEFAULT `AUTO`; `AUTO`, `MANUAL` |
| `report_text` | `TEXT` | NULL |
| `report_not_expected` | `BOOLEAN` | NOT NULL DEFAULT FALSE |
| `notes` | `TEXT` | NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

**Effective status is derived**, not assumed equal to the stored `status` when `status_mode='AUTO'`. For `MANUAL`, use stored `status`; for `AUTO`, calculate `SCHEDULED` or `COMPLETED` from event date/time and the application-configured local timezone. An event today without a time remains scheduled until the following day; with a time, it becomes completed after that time passes. Automatic mode cannot yield `CANCELLED`; cancellation is a manual state. Restoring a cancelled event returns it to `AUTO` and recalculates its effective status. `status` in AUTO mode is a nonauthoritative stored placeholder (typically the value when last saved), **not** a persisted current-state cache. Queries, filtering, sorting, and API DTOs must use effective status. An implementation may instead replace `status` + `status_mode` with nullable `manual_status`, but only via a documented schema decision before migrations; do not mix both approaches.

`COMPLETED` reflects scheduling/state rules, not clinical proof that a procedure occurred. Missing-report status is also derived: completed, no `report_text`, no linked `event_documents.is_report`, no lab results, and `report_not_expected=false`. Cancelled events remain in history but are excluded from the calendar; deletion is confirmed and permanent. Multiple events with the same service/date are valid.

### 4.5 `lab_parameters`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | PK |
| `service_id` | `UUID` | NOT NULL FK `medical_services.id` |
| `user_id` | `UUID` | NULL for predefined, otherwise FK `users.id` |
| `name` | `VARCHAR(200)` | NOT NULL, nonblank |
| `is_active` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

Only laboratory services can own parameters. A personal parameter can belong to a global laboratory service or to the same user's personal service, but not another user's service. Global parameters must belong to global services. Enforce these cross-row rules in backend service logic, supplemented with database constraints where feasible. Partial unique indexes for names within service/scope: `(service_id, LOWER(BTRIM(name))) WHERE user_id IS NULL` and `(service_id, user_id, LOWER(BTRIM(name))) WHERE user_id IS NOT NULL`. The backend checks conflicts against accessible predefined parameters, including archived ones. Referenced parameters are archived, not hard-deleted.

### 4.6 `measurement_units`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | PK |
| `symbol` | `VARCHAR(50)` | NOT NULL, UNIQUE after canonicalization |
| `name` | `VARCHAR(150)` | NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |

Shared, seeded catalog for the MVP. A result's unit is optional. **Do not assume that unit symbols alone prove biological comparability or enable conversion.** Numeric charts group by parameter and exact unit ID (including a separate no-unit group); automatic unit conversion is out of scope. If the UI restricts selectable units per parameter, a compatible-units mapping is an **implementation follow-up**, not silently enforced by this schema.

### 4.7 `exam_results`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | PK |
| `exam_id` | `UUID` | NOT NULL FK `medical_events.id` |
| `parameter_id` | `UUID` | NOT NULL FK `lab_parameters.id` |
| `position` | `INTEGER` | NOT NULL, > 0 |
| `value_type` | `VARCHAR(20)` | NOT NULL; `NUMERIC`, `TEXT`, `UNAVAILABLE` |
| `numeric_value` | `NUMERIC(18,6)` | NULL |
| `text_value` | `TEXT` | NULL |
| `unit_id` | `UUID` | NULL FK `measurement_units.id` |
| `interval_type` | `VARCHAR(20)` | NULL; `RANGE`, `LOWER_BOUND`, `UPPER_BOUND`, `TEXT` |
| `lower_bound` | `NUMERIC(18,6)` | NULL |
| `upper_bound` | `NUMERIC(18,6)` | NULL |
| `lower_inclusive` | `BOOLEAN` | NULL |
| `upper_inclusive` | `BOOLEAN` | NULL |
| `text_interval` | `TEXT` | NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

`value_type` invariants: `NUMERIC` requires `numeric_value` and prohibits `text_value`; `TEXT` requires nonblank `text_value` and prohibits `numeric_value`; `UNAVAILABLE` prohibits both values. For nonnumeric results, a unit should be NULL. Interval invariants: `RANGE` requires two bounds and their inclusivity flags; `LOWER_BOUND` or `UPPER_BOUND` requires only the corresponding bound and flag; `TEXT` requires nonblank `text_interval`; NULL interval type requires all interval fields NULL. If both numeric bounds exist, lower <= upper; if equal, both endpoints must be inclusive to represent a nonempty interval. A numeric interval uses the **same unit/scale as its result**; no independent interval unit is stored. Text intervals are displayed, never interpreted numerically. Historical intervals are stored on each result and not overwritten by catalog changes.

Only events with `LABORATORY` services can have results. A result parameter must belong to the event's service and be visible to its owner. These are cross-table invariants requiring application validation and tests. Do **not** add UNIQUE `(exam_id, parameter_id)`; repeated parameters are permitted. For ordering, choose a documented policy: either UNIQUE `(exam_id, position)` with collision-safe reorder transactions, or nonunique positions with deterministic `(position,id)` sort. **Recommended:** unique position per event, with a tested two-phase renumbering strategy. Result deletion cascades from event deletion; parameter and unit deletion is restricted while referenced.

### 4.8 `documents`

| Column | Type | Constraints / meaning |
|---|---|---|
| `id` | `UUID` | PK |
| `user_id` | `UUID` | NOT NULL FK `users.id` |
| `title` | `VARCHAR(200)` | NOT NULL, nonblank |
| `original_filename` | `VARCHAR(255)` | NOT NULL |
| `storage_key` | `VARCHAR(255)` | NOT NULL, UNIQUE, opaque private path/key |
| `mime_type` | `VARCHAR(100)` | NOT NULL |
| `file_size` | `BIGINT` | NOT NULL, > 0, max 10 MB at API validation |
| `category` | `VARCHAR(50)` | NOT NULL; predefined code or `OTHER` |
| `custom_category` | `VARCHAR(150)` | NULL; nonblank when `category='OTHER'` and a custom label is supplied |
| `description` | `TEXT` | NULL |
| `document_date` | `DATE` | NULL |
| `expiry_date` | `DATE` | NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL (upload timestamp) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

Allowed MIME types: PDF, JPEG, PNG, validated using file content, not only extension/client metadata. Store binaries outside public/static paths and use opaque storage keys. The initial category vocabulary must be fixed before writing the `CHECK` constraint and API DTOs; **custom labels are stored inline for the MVP**, not in a separate `document_categories` table. If reusable custom categories become a requirement, normalize them later. `created_at` is the upload time; `document_date` is the document's own date. Document expiry is a calendar projection, not a medical event.

### 4.9 `event_documents`

| Column | Type | Constraints / meaning |
|---|---|---|
| `event_id` | `UUID` | NOT NULL |
| `document_id` | `UUID` | NOT NULL |
| `user_id` | `UUID` | NOT NULL; redundant owner for DB-enforced isolation |
| `is_report` | `BOOLEAN` | NOT NULL DEFAULT FALSE |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |

Primary key `(event_id, document_id)`. Define `UNIQUE(id, user_id)` on both `medical_events` and `documents`, then composite foreign keys `(event_id, user_id) -> medical_events(id, user_id)` and `(document_id, user_id) -> documents(id, user_id)`; both `ON DELETE CASCADE`. This makes cross-user event-document links impossible even if a backend check is missed. `is_report` belongs to the link, so a document may be a report for one event and supporting material for another. Unlinking deletes only the join row; deleting an event leaves documents in the archive; deleting a document removes all links and its binary through coordinated application logic.

## 5. Ownership and cross-table integrity

1. Derive authenticated `user_id` from the server session; never accept it as an authority in API requests.
2. Query personal records by `(id, user_id)` or an equivalent owner-scoped repository method; return an appropriate not-found/forbidden response without exposing other users' records.
3. Add `UNIQUE(id, user_id)` on `doctors` and `facilities`, and use composite event FKs `(doctor_id, user_id)` and `(facility_id, user_id)` where the nullable relationship permits it. A NULL doctor/facility ID skips the optional FK; non-null references must match the event owner.
4. Shared/personal service and parameter access cannot be expressed by a simple owner-equality FK because `user_id=NULL` means globally shared. Enforce accessibility and category consistency transactionally in the service layer; evaluate DB triggers only if a demonstrated gap warrants the complexity.
5. User deletion must account for both `RESTRICT` historical references and personal catalogs; explicitly order dependent-data deletion before catalogs, rather than assuming a single user-row cascade resolves every reference.
6. Revalidate authorization and ownership during document download, replacement, linking, and deletion.

## 6. Status and derived views

**Status calculation** uses the configured application timezone (initial local deployment: `Europe/Rome`, configurable), including daylight-saving rules. Treat `event_date + event_time` as local wall-clock scheduling data. The date/time policy for ambiguous DST transitions is an API/implementation follow-up; no conversion should be silently guessed. Status filtering must use the same effective-status function as detail responses. Default history sorting: upcoming nearest first, completed/cancelled newest first; paginate 20 records per page. For performance, status queries may translate the calculation into SQL predicates rather than loading all records.

**Missing report** is not a column: derive it for effective `COMPLETED` events when `report_not_expected=false`, `report_text` absent/blank, no report-flagged linked document, and no lab results. A completed event is not necessarily medically verified as performed.

**Calendar** combines noncancelled events by `event_date` and document expiries by `expiry_date`; month is the default view. Do not create calendar rows duplicating the source data.

## 7. Deletion and file lifecycle

| Operation | Database behavior | File behavior |
|---|---|---|
| Delete medical event | Delete results and event-document links; keep catalog, doctors, facilities, and documents | Keep document binaries |
| Unlink document | Delete only the matching `event_documents` row | Keep binary |
| Delete document | Delete links and document metadata after confirmation | Delete binary via safe cleanup workflow |
| Replace document binary | Preserve document ID and links; update storage metadata | Stage new binary; commit metadata; remove old binary after success |
| Archive used service/parameter | Set `is_active=false`; preserve historical references | None |
| Delete account | Confirm current password; remove dependent records, personal catalogs, user, and invalidate sessions | Remove all owned binaries with recoverable cleanup |

A PostgreSQL transaction cannot atomically commit filesystem changes. Implement staged uploads and compensating cleanup, or a durable cleanup queue/reconciliation process. Avoid deleting the old binary before the new metadata commit succeeds. Account deletion and session invalidation must not leave an authenticated session with access to residual data. For the local MVP, test failure paths using temporary private storage.

## 8. Index strategy

Start with the following indexes, avoiding duplicates already created by PK/UNIQUE constraints. Verify actual query plans with `EXPLAIN (ANALYZE, BUFFERS)` after implementing representative queries.

| Table | Proposed index | Primary use |
|---|---|---|
| `users` | unique `LOWER(BTRIM(email))` | Login and account uniqueness |
| `medical_services` | two partial unique indexes by scope/name/category | Catalog deduplication |
| `lab_parameters` | two partial unique indexes by scope/service/name | Parameter deduplication |
| `medical_events` | `(user_id, event_date, event_time)` | Calendar, history, upcoming |
| `medical_events` | `(user_id, service_id, event_date)` | Service-filtered history |
| `medical_events` | `(user_id, doctor_id)`; `(user_id, facility_id)` | Optional filters |
| `exam_results` | `(exam_id, position)` (unique if chosen) | Ordered result retrieval |
| `exam_results` | `(parameter_id, exam_id)` | Historical parameter series |
| `documents` | `(user_id, created_at DESC)` | Archive |
| `documents` | `(user_id, category)` | Category filter |
| `documents` | `(user_id, expiry_date)` | Expiry calendar |
| `documents` | unique `storage_key` | Binary lookup |
| `event_documents` | `(document_id)` | Reverse-link lookup |

FKs are not automatically indexed by PostgreSQL; add supporting indexes where needed. A simple `(user_id,status)` index is not sufficient for derived AUTO status and is not part of the default index set. Text search can be added after the query requirements and dataset size justify it.

## 9. Constraints and migration implementation notes

- Define `CHECK` constraints for allowed service categories, status/mode codes, value/interval codes, positive positions, positive file sizes, and valid result/interval field combinations.
- Enforce nonblank required strings at API validation and preferably via database `CHECK (BTRIM(column) <> '')`.
- Add partial unique expression indexes for catalog names and email as described above; ensure case-insensitive behavior is tested against whitespace and Unicode examples.
- For composite owner FKs, add supporting `UNIQUE(id,user_id)` constraints on parent tables before creating dependent FKs.
- Define FK actions explicitly. Use `CASCADE` for event → results/links and document → links; `RESTRICT`/`NO ACTION` for historical references to services/parameters/units and for referenced doctors/facilities. User deletion is orchestrated to respect these dependencies.
- Seed global medical services, laboratory parameters, and measurement units using versioned, deterministic migrations. Stable seed identifiers or idempotent controlled inserts must be documented; never silently replace existing user data.
- Proposed Flyway ordering: users → catalogs and reference data → doctors/facilities → events → results/documents → join tables → indexes and seeds (respect FK dependencies).
- Session persistence is an **infrastructure concern**: choose Spring Session JDBC tables or an explicitly bounded local session registry before implementing multi-session invalidation. Do not invent a custom clinical-domain sessions table. Validate 30-minute inactivity timeout, CSRF behavior, logout-current-only, and invalidation of other sessions after password change.
- Test account deletion, duplicate catalog creation, archived catalog references, cross-user link rejection, reordering duplicate parameters, file replacement failures, and derived status around midnight and DST.

## 10. Decisions and remaining implementation follow-ups

### Confirmed baseline decisions

1. One `medical_events` table for all service categories; laboratory results reference it directly.
2. Shared catalog rows use `user_id=NULL`; personal rows have an owner.
3. Document binaries live in private filesystem storage; PostgreSQL stores metadata.
4. Event-document links are many-to-many with per-link `is_report` and database-enforced same-owner linkage.
5. Medical statuses and missing-report flags are computed where required; no duplicated calendar or report-status tables.
6. Historical laboratory reference intervals are stored per result; no automatic unit conversion.
7. The MVP is local and uses synthetic data only.

### Follow-ups before implementing migrations

- Finalize the predefined `documents.category` codes and whether `OTHER` without a custom label is permitted.
- Choose the exact result-position reorder mechanism and test uniqueness during reorder.
- Finalize password/session infrastructure and its JDBC schema/migration ownership.
- Specify DST ambiguity handling for timed appointments.
- Decide whether the MVP will restrict unit selections per parameter (requires explicit mapping) or allow any catalog unit with careful UI warnings.
- Review owner-safe deletes and FK behavior in actual PostgreSQL integration tests.

## 11. Traceability and next phase

This schema implements the MVP data model described in the requirements baseline and respects the modular-monolith, Spring Data JPA, PostgreSQL, Flyway, and private-file-storage architecture. The next artifact is **`docs/04-api-design.md`**, which should define authentication/CSRF, profile, catalogs, events, lab results, document upload/download/linking, calendar, error DTOs, pagination, and ownership-safe API semantics.
