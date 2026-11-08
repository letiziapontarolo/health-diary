# Health Diary — Software Requirements Specification

**Document ID:** SRS-001  
**Version:** 1.0 (consolidated baseline)  
**Status:** Requirements baseline approved for architecture design  
**Product language:** Italian  
**Documentation and source language:** English

## 1. Purpose and scope

Health Diary is a private, multi-user web application for organizing personal health information. It brings medical appointments, laboratory results, reports, documents, and a unified calendar into one place. It is an **organizational tool, not a medical device or a diagnostic service**: it does not diagnose conditions, recommend treatment, or infer clinical conclusions.

The initial release is intended for local development and demonstration with **synthetic data**. Public deployment and production processing of real medical data are **not** part of the initial scope.

### 1.1 Release labels

- **MVP:** required for the first usable release.
- **Future:** planned after the MVP; subject to further design.
- **Cross-cutting:** applies to relevant features in both phases.

### 1.2 Intended users

- **Registered user:** manages only their own account and health records.
- **Unauthenticated visitor:** may register or sign in; cannot view private health records.

No administrator or clinician role is currently specified.

## 2. Functional requirements

### 2.1 Accounts and profiles — MVP

| ID | Requirement |
|---|---|
| FR-001 | The system shall allow a visitor to register with first name, last name, date of birth, email address, and password. |
| FR-002 | The system shall allow registered users to sign in and sign out. |
| FR-003 | The system shall enforce case-insensitive uniqueness of account email addresses. |
| FR-004 | The system shall allow users to view and edit their profile, including optional height and city. |
| FR-005 | The system shall allow users to request account deletion with explicit confirmation. |
| FR-006 | Before account deletion, the interface shall suggest exporting the user's information. |
| FR-007 | Confirmed account deletion shall remove the user's associated health records and stored files according to the defined deletion policy. |
| FR-008 | Authenticated users shall access only their own private records, including through direct API requests. |

### 2.1.1 Authentication and profile details — MVP

| ID | Requirement |
|---|---|
| FR-068 | Registration shall require one password field with a visibility toggle; email verification and email-based password recovery are excluded from the MVP. |
| FR-069 | The system shall authenticate users using Spring Security server-side sessions with secure, HttpOnly session cookies and CSRF protection. |
| FR-070 | Each authenticated session shall expire after 30 minutes without authenticated server requests; multiple independent concurrent sessions shall be supported. |
| FR-071 | Signing out shall invalidate only the current session; changing the password shall verify the current password and invalidate all other active sessions. |
| FR-072 | Passwords shall contain 12–128 characters; no mandatory character composition rules shall apply, but known weak or compromised passwords shall be rejected. |
| FR-073 | Users shall be able to toggle password visibility on authentication and account-management forms; password fields shall be masked by default. |
| FR-074 | Users shall be able to change their email address after entering their current password; the new address shall be unique without case sensitivity. Email changes shall not require email verification in the MVP. |
| FR-075 | Users shall be able to update their first name, last name, birth date, optional height, and optional city without entering their password, using an explicit save action. |
| FR-076 | Permanently deleting an account shall require re-entry of the current password, an explicit warning, and confirmation; all sessions, associated private records, and stored files shall be invalidated or removed. |
| FR-077 | The interface shall display an avatar generated from the user's first and last initials and update it when the name changes; profile-photo uploads are excluded from the MVP. |

### 2.2 Medical service catalog — MVP

| ID | Requirement |
|---|---|
| FR-009 | The system shall provide a searchable catalog of medical services categorized as laboratory, specialist, or diagnostic. |
| FR-010 | The catalog shall support shared predefined services and user-created private services. |
| FR-011 | Users shall be able to archive their custom catalog entries so they are unavailable for new selection without altering historical records. |
| FR-012 | Users shall be able to select existing doctors and facilities through suggestions and add new private entries. |
| FR-013 | Users shall be able to merge duplicate doctor or facility entries while preserving existing associations. |
| FR-085 | Each medical service may have an optional symbolic icon key. The interface shall resolve the key through a controlled frontend icon registry, with a category-specific fallback when missing or unknown. |
| FR-086 | Predefined services shall use developer-curated icons; users creating or editing private services may choose an icon from the approved application catalog. Uploading arbitrary icon files is excluded from the MVP. |

### 2.2.1 Custom catalog management — MVP

| ID | Requirement |
|---|---|
| FR-078 | Custom services shall be private to their creator; predefined services shall be shared and read-only to ordinary users. |
| FR-079 | Creating or renaming a custom service shall reject duplicate names within the same category among visible predefined and user-owned entries, using case-insensitive comparison and including archived entries. |
| FR-080 | Users shall be able to rename their custom services even when referenced by events; all referencing events shall display the updated name. |
| FR-081 | A custom service's category shall be editable only when no existing event references it. |
| FR-082 | A custom service may be permanently deleted only when no existing medical event references it; otherwise it may be archived. |
| FR-083 | The catalog shall sort predefined and personal entries together alphabetically; the management view shall show active entries by default and offer archived/all filters. |
| FR-084 | Users shall be able to reactivate their archived custom services immediately without an additional confirmation. |
| FR-085 | Users shall be able to create a custom service inline in the event form, automatically select it, and preserve all other entered form data. |

### 2.3 Medical events — MVP

| ID | Requirement |
|---|---|
| FR-014 | Users shall be able to create, view, edit, and delete medical events for laboratory tests, specialist visits, and diagnostic procedures. |
| FR-015 | Each medical event shall have a service and date; time, doctor, facility, report text, and personal notes shall be optional. |
| FR-016 | The system shall support scheduled, completed, and cancelled event states. |
| FR-017 | A scheduled event shall be treated as completed when its specified time is reached, or on the following day if no time was provided; this is a presumed state, not confirmation of attendance. |
| FR-018 | Changing an event's date or time shall recalculate its scheduled/completed state only while automatic status management is enabled; explicit manual overrides shall be preserved. |
| FR-019 | Users shall be able to cancel and restore events. Cancelled events shall remain accessible in a dedicated list but be hidden from the calendar. |
| FR-020 | Users shall be able to permanently delete an event after confirmation; deletion shall remove its dependent laboratory results and document associations but retain documents in the central archive. |
| FR-021 | Users shall be able to mark an event as having no expected report, and reverse that choice. |
| FR-022 | Completed events shall display a “missing report” indication when no qualifying report information is available and the user has not marked the report as not expected. |
| FR-023 | Users shall be able to search and filter events by service, category, date/range, doctor, facility, and status. |
| FR-024 | Event lists shall support pagination (initially 20 items per page) and sensible ordering: upcoming first for scheduled events and newest first for completed or cancelled events. |
| FR-025 | Users shall be able to create a new event from a selected calendar day, with the date prefilled and editable. |
| FR-026 | The event form shall adapt its fields to the selected category and provide separate sections for general information, laboratory results when applicable, and reports/documents. |
| FR-027 | Users shall be able to save general event information first, then add or edit results and documents in later sessions. |
| FR-028 | The interface shall warn before navigating away from unsaved changes and shall preserve entered values when validation fails. |

### 2.3.1 Event status and reports — MVP

| ID | Requirement |
|---|---|
| FR-086 | The system shall initialize an event's status from its date and optional time, while allowing explicit user overrides that are not overwritten by automatic updates. |
| FR-087 | Restoring a cancelled event shall re-enable automatic status calculation from the stored date and optional time. |
| FR-088 | The system shall support exactly SCHEDULED, COMPLETED, and CANCELLED event statuses; no confirmation of actual attendance shall be required. |
| FR-089 | The event list and detail page shall display a missing-report indicator for completed events that lack qualifying report information, unless the user has selected 'No report expected'. |
| FR-090 | The 'Report and Documents' section shall contain an editable 'No report expected' checkbox. |
| FR-091 | An event may have both a textual report and multiple linked document attachments simultaneously. |
| FR-092 | Users shall be able to edit textual reports without version history, or permanently remove the report text after confirmation, without deleting the event or attachments. |

### 2.4 Laboratory results — MVP

| ID | Requirement |
|---|---|
| FR-029 | Laboratory events shall support multiple individual parameter results. |
| FR-030 | Users shall be able to select parameters from a searchable catalog containing predefined and private custom entries. |
| FR-031 | Parameter catalog entries shall belong to a specific laboratory service and support archival without erasing historical results. |
| FR-032 | Each result shall support numeric, textual, or explicitly unavailable values. |
| FR-033 | Numeric results shall support a unit of measurement and an optional reference interval, including bounded, one-sided, and textual forms. |
| FR-034 | When a compatible numeric reference interval is provided, the interface shall indicate whether the result is outside that interval, without interpreting its clinical significance. |
| FR-035 | Results without a suitable numeric interval, including textual or unavailable results, shall not be classified as in or out of range. |
| FR-036 | Users shall be able to add, edit, remove, and reorder individual results; the same parameter may appear more than once in one event. |
| FR-037 | The system shall provide historical charts for compatible numeric measurements of the same parameter, with appropriate units and per-result reference intervals. |
| FR-038 | The system shall provide a chronological history for textual results. |

### 2.4.1 Laboratory unit handling — MVP

| ID | Requirement |
|---|---|
| FR-093 | A numeric laboratory result may omit a unit; it may be compared with its own compatible reference interval but shall not be plotted alongside results with uncertain unit compatibility. |
| FR-094 | Numeric reference intervals shall support inclusive/exclusive lower and upper bounds; textual intervals shall not be numerically evaluated. |

### 2.5 Documents — MVP

| ID | Requirement |
|---|---|
| FR-039 | Users shall be able to upload and manage PDF and image documents in a private central archive. |
| FR-040 | Documents shall support predefined or user-defined categories and optional expiration dates. |
| FR-041 | Documents shall be able to exist independently or be associated with multiple relevant medical events. |
| FR-042 | Users shall be able to link an existing archived document to an event without uploading a duplicate. |
| FR-043 | Removing an event-document association shall not delete the archived document or its file. |
| FR-044 | Document expiration dates shall be available as calendar entries. |

### 2.5.1 Document lifecycle — MVP

| ID | Requirement |
|---|---|
| FR-095 | Users may permanently delete a document even if it is linked to events, after a confirmation listing affected events; the document, stored file, and all links shall be removed, but events shall remain. |
| FR-096 | Users may replace a document's underlying file while preserving its metadata and all event links; previous file versions shall not be retained. |
| FR-097 | Removing or replacing a report document shall cause affected events' missing-report indicators to be recalculated. |

### 2.6 Calendar and dashboard — MVP

| ID | Requirement |
|---|---|
| FR-045 | The system shall provide daily, weekly, monthly, and annual calendar views. |
| FR-046 | The calendar shall open in the current month's view by default. |
| FR-047 | Events without a time shall appear before timed events in daily and weekly views. |
| FR-048 | Calendar events shall use predefined category colors and textual labels; the annual view shall show category-colored indicators on dates with events. |
| FR-049 | Users shall be able to filter visible calendar entries by category, including multiple selected categories. |
| FR-050 | Selecting a calendar event shall open a compact summary with a link to its full detail page. |
| FR-051 | The calendar shall include relevant medical events and document expirations; cancelled medical events shall be excluded. |
| FR-052 | The dashboard shall provide a compact calendar, upcoming appointments, and quick links to key sections. |
| FR-053 | The interface shall provide a persistent sidebar on larger screens and a collapsible menu on small screens. |
| FR-054 | The medical events page shall provide All, Laboratory, Specialist, and Diagnostic tabs, with All selected by default. |

### 2.6.1 Calendar and dashboard scope — MVP

| ID | Requirement |
|---|---|
| FR-098 | The dashboard shall show appropriate empty states when no upcoming appointments exist and shall not prominently display personal health conditions. |
| FR-099 | The application shall not provide offline operation, external calendar synchronization, or automated appointment reminders in the MVP. |

### 2.7 Planned functionality — Future

| ID | Requirement |
|---|---|
| FR-055 | Users shall be able to record emergency department visits, including triage details and links to related events. |
| FR-056 | Users shall be able to record hospital stays with admission and discharge dates and associated records; calendar views shall reflect the stay's date span. |
| FR-057 | Users shall be able to record planned and completed surgeries/procedures and associate them with relevant stays or emergency visits. |
| FR-058 | Users shall be able to manage scheduled recurring and one-time medication use. |
| FR-059 | Medication occurrences shall be derived from recurring schedules on demand; daily confirmation of intake shall not be required and the system shall not assume a dose was taken. |
| FR-060 | Users shall be able to mark a medication occurrence as skipped or exclude it from the effective schedule, preserving exclusions against regeneration. |
| FR-061 | Users shall be able to record personal measurements and view compatible historical charts. |
| FR-062 | Users shall be able to manage vaccination records and manually planned doses, without automatic medical booster recommendations. |
| FR-063 | Users shall be able to record allergies and intolerances. |
| FR-064 | Users shall be able to manage health conditions and manually associate related events, treatments, and documents, without automatic inference. |
| FR-065 | Users shall be able to generate a customizable PDF export by selecting sections and an optional date range. |
| FR-066 | The export shall include a readable listing of relevant documents; embedding every original attachment is not required. |
| FR-067 | The dashboard and calendar shall incorporate relevant future modules as they are implemented. |

### 2.8 Additional planned behavior — Future

| ID | Requirement |
|---|---|
| FR-100 | Medication management shall support recurring schedules with start/end dates, doses, units, and one or more times, plus one-time medication entries and persisted skipped/excluded occurrences. |
| FR-101 | Vaccination management shall support recorded and manually scheduled doses, notes, and documents, without automatically calculating booster dates. |
| FR-102 | Hospital stays shall support admission and optional discharge dates, associated events, and multi-day calendar display. |
| FR-103 | Emergency department records shall support admission/discharge times, triage information, outcomes, and links to related medical events. |
| FR-104 | Personal measurements shall support user-defined parameters, timestamps, compatible trend charts, and compound measurements such as blood pressure. |
| FR-105 | Health conditions shall support user-managed categories, dates, notes, and manually linked events, treatments, and documents, without automatic inference. |

## 3. Business rules

| ID | Rule |
|---|---|
| BR-001 | Health data is private to its owning user; shared catalogs do not make user records public. |
| BR-002 | An event may share its service and date with another event; no uniqueness constraint shall prevent this. |
| BR-003 | An event's service may be changed only within the same service category; laboratory parameter compatibility must be checked. |
| BR-004 | Event completion based on elapsed time is presumed, not verified attendance; an explicit manual status takes precedence until automatic management is re-enabled. |
| BR-005 | Cancelled events are retained for history but omitted from calendar views until restored. |
| BR-006 | A completed event's “missing report” warning is suppressed when it has nonblank report text, a linked document flagged as a report, at least one laboratory result for laboratory events, or an explicit “no report expected” selection. |
| BR-007 | Laboratory parameters are scoped to a particular laboratory service; similarly named parameters in different services may be distinct catalog entries. |
| BR-008 | Results without appropriate numeric reference information must not be judged normal or abnormal. |
| BR-009 | An archived custom catalog item remains usable for interpreting historical records but cannot be newly selected. |
| BR-010 | Deleting an event must not automatically delete a document stored in the central archive. |
| BR-011 | Document expiry and medical event entries share the calendar but retain their distinct underlying data models. |
| BR-012 | The application shall not send reminders or notifications in the planned scope. |
| BR-013 | The application shall not import medical records in the planned scope. |
| BR-014 | The application shall not automatically diagnose conditions, recommend treatment, or calculate vaccine boosters. |
| BR-015 | Service names are unique case-insensitively per category within the set of shared and user-owned services visible to a user; archived services participate in duplicate checks. |
| BR-016 | Event status changes driven by elapsed time must use a consistent configured time zone; time-zone details are specified during architecture design. |
| BR-017 | Deleting a document is distinct from unlinking it; unlinking preserves the archived document, while confirmed deletion removes all links. |
| BR-018 | The absence of a laboratory result unit shall never imply compatibility with a known unit for historical charting. |

## 4. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-001 | All user-facing interface copy shall be in Italian; source code, database identifiers, APIs, README, and technical documentation shall be in English. |
| NFR-002 | Passwords shall be stored using an appropriate salted password-hashing algorithm (e.g., BCrypt or Argon2id), never as plaintext. |
| NFR-003 | Authorization shall be enforced server-side for every private resource, not only through frontend visibility rules. |
| NFR-004 | Validation shall run in both frontend and backend; field-level errors shall preserve user-entered form values. |
| NFR-005 | The backend shall use consistent HTTP error responses, based on Problem Details, without exposing sensitive implementation details. |
| NFR-006 | The UI shall be responsive on desktop, tablet, and mobile, with accessible labels and indicators not reliant on color alone. |
| NFR-007 | Database schema changes shall be versioned with Flyway; Hibernate shall validate, rather than automatically mutate, the schema. |
| NFR-008 | Development PostgreSQL shall run in Docker with persistent storage and credentials excluded from version control. |
| NFR-009 | Automated backend and frontend tests shall be introduced alongside implementation, including authorization/isolation tests. |
| NFR-010 | API endpoints shall be documented through OpenAPI and testable during development through Swagger UI. |
| NFR-011 | The project shall use a single Git repository, with feature-oriented React code and layer-oriented Spring Boot code. |
| NFR-012 | Development and demonstration shall use synthetic health data; production security, privacy, and regulatory readiness are not claimed. |
| NFR-013 | The application shall avoid making clinical claims based solely on user-entered measurements or reference intervals. |

### 4.1 Additional non-functional acceptance targets

| ID | Requirement |
|---|---|
| NFR-014 | The system shall target response times of at most 2 seconds for dashboard loading, event listing, event saving, and calendar navigation, and at most 1 second for catalog searches, under documented test conditions. |
| NFR-015 | The interface shall target WCAG 2.2 Level AA, with keyboard navigation, appropriate focus handling, readable error messages, and sufficient contrast; compliance shall not be claimed without verification. |
| NFR-016 | The interface shall target current versions of Chrome, Firefox, Edge, and Safari on supported desktop and mobile devices. |
| NFR-017 | Uploaded files shall be private, access-controlled on the server, and stored without publicly accessible direct file paths. |
| NFR-018 | Multi-record operations shall preserve database consistency; file/database operations shall include failure handling so replacement failures do not destroy the previous valid file. |
| NFR-019 | The system shall use session cookie settings appropriate to the deployment environment (HttpOnly, SameSite, and Secure over HTTPS) and shall protect state-changing operations against CSRF. |
| NFR-020 | Automated tests shall cover authentication, authorization, tenant isolation, critical CRUD operations, laboratory validations, and document access. |

## 5. Main user journeys

1. **Register and sign in:** create an account, authenticate, and access only personal records.
2. **Schedule an appointment:** select a service, date, and optional details; save general information; see the appointment in the calendar.
3. **Record laboratory results:** open a laboratory event, add parameters and results, save them, and review historical values.
4. **Attach a report:** upload a PDF or image to the archive and link it to an event; access it later independently.
5. **Review the calendar:** switch among four views, filter categories, inspect an event summary, and navigate to details.
6. **Maintain a profile:** update personal details and, when desired, request confirmed account deletion.

## 6. Explicit exclusions and assumptions

- **No public production deployment** is included in the MVP plan.
- **No automatic reminders, push notifications, or email notifications** are currently planned.
- **No AI-generated medical interpretation**, diagnosis, or treatment advice.
- **No import functionality** is currently planned.
- **PDF export** is planned for a later phase, not the MVP.
- **No clinician/admin role** has been specified.
- User-entered reference intervals are informational and may differ by laboratory and context.

## 7. Architecture-design follow-ups (not unresolved product requirements)

The MVP requirements above are the agreed baseline. The following are engineering specifications to complete in architecture, database, API, and testing documents:

1. **File handling:** maximum upload sizes, MIME/content validation, storage layout, replacement and deletion recovery, and cleanup of orphaned files.
2. **Time handling:** configured time zone and consistent handling of date-only events and server clock changes.
3. **Session implementation:** session registry for concurrent sessions and invalidation after password changes; CSRF token transport to React.
4. **Database/API contracts:** schema constraints, transactional boundaries, DTOs, routes, pagination envelopes, and filtering semantics.
5. **Performance/accessibility tests:** benchmark dataset, measurement method, and verification of WCAG 2.2 AA goals.
6. **Post-MVP modules:** detailed schema and acceptance criteria before implementing each future feature.

These points do not block beginning the MVP architecture design. Public deployment and processing of real patient data remain outside the approved MVP scope.

## 8. Traceability and acceptance approach

- Each requirement has a stable identifier (`FR`, `NFR`, or `BR`).
- Implementation tasks and tests should reference relevant identifiers.
- Requirements marked **MVP** form the initial delivery scope; **Future** items are not MVP acceptance criteria.
- Before implementation, review each MVP requirement for testable acceptance criteria and any unresolved dependencies.

---

*Consolidated requirements baseline. Future changes should update this document and the corresponding design and test artifacts.*
