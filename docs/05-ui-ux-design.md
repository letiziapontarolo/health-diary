# Health Diary — UI/UX Design Specification

**Document ID:** UX-005  
**Version:** 1.0 (approved design baseline)  
**Status:** UI/UX decisions approved; implementation and usability validation pending  
**Related documents:** [01-requirements.md](01-requirements.md) · [02-architecture.md](02-architecture.md) · [03-database-design.md](03-database-design.md) · [04-api-design.md](04-api-design.md)  
**Product language:** Italian; code, API, design tokens and technical documentation: English  
**Scope:** Local MVP using synthetic demonstration data only

## 1. Goals and guiding principles

Health Diary is a private, multi-user personal health diary for organizing medical events, laboratory results, reports, documents, and document expirations. The UI supports **recordkeeping and navigation**, not diagnosis, risk scoring, clinical interpretation, or medical advice.

Design principles:

1. **Modern Healthcare / Medical Blue:** clean, calm, credible, and approachable rather than institutional.
2. **Clarity over density:** clear hierarchy, recognizable actions, concise summaries, and progressive disclosure.
3. **Consistency:** shared navigation, components, labels, status semantics, and interaction patterns.
4. **Accessible by design:** WCAG 2.2 AA as a target, subject to implementation audit and testing.
5. **Privacy by default:** authenticated views only; no sensitive details in unauthenticated UI, browser notifications, or unnecessary logs.
6. **Responsive:** usable on desktop, tablet, and mobile without losing core features.
7. **Faithful to the data:** no fabricated appointment times, laboratory ranges, diagnoses, or inferred health status.

## 2. Design system

### 2.1 Color tokens

| Token | Value | Purpose |
|---|---|---|
| `color.primary` | `#2563EB` | Primary actions, selected navigation, links |
| `color.primaryHover` | `#1D4ED8` | Primary hover |
| `color.primarySubtle` | `#EFF6FF` | Selected backgrounds and subtle accents |
| `color.background` | `#F8FAFC` | Application background |
| `color.surface` | `#FFFFFF` | Cards, dialogs, panels |
| `color.textPrimary` | `#0F172A` | Main text |
| `color.textSecondary` | `#64748B` | Supporting text; verify contrast at final sizes |
| `color.border` | `#E2E8F0` | Dividers and control borders |
| `color.success` | `#16A34A` | Positive status and success feedback |
| `color.warning` | `#EA580C` | Warnings and expiring documents |
| `color.error` | `#DC2626` | Errors and destructive actions |

Category colors: **laboratory blue**, **specialist green**, **diagnostic purple**, **document expiry orange**. Status colors are a separate semantic layer: scheduled blue, completed green, cancelled neutral gray, missing report orange, expired red. Category and status must never be communicated by color alone. Choose accessible foreground/background combinations per component; raw palette values are *tokens*, not a blanket contrast guarantee.

**Theme:** light mode only for MVP. Define semantic CSS/Tailwind tokens so dark mode can be added later without redesigning every component.

### 2.2 Typography, spacing, shape

- Font family: **Inter** with system sans-serif fallback.
- Desktop page title: 30 px / bold; section title: 24 px / semibold; card title: 18 px / semibold; body: 16 px / regular; supporting text: 14 px; labels/badges: 12–14 px / medium. Scale oversized headings down on small screens while keeping primary form text comfortably readable.
- Spacing scale: `4, 8, 12, 16, 24, 32, 48` px.
- Card radius: **12 px**; restrained shadows, subtle borders, ~20–24 px desktop inner padding; adapt padding for narrow screens.
- Controls need generous touch targets and visible focus indicators; do not rely on hover-only affordances.

### 2.3 Reusable components

- Buttons: `primary`, `secondary`, `outline`, `danger`, with hover, focus, disabled, loading states. Disable repeated submissions and use clear Italian loading labels.
- Inputs: persistent visible labels; required marker `*`; descriptive field-level errors; accessible hint/error association; password visibility toggle.
- Searchable selects/comboboxes for medical services, doctors, facilities, parameters, and linked events; keyboard navigation and accessible creation affordances.
- Cards, tabs, filters, category/status badges, dialogs, confirmation dialogs, toasts/status messages, empty states, skeletons, pagination, responsive tables, date/time inputs, file dropzone with ordinary file-picker fallback.
- Standard application states: **loading**, **empty**, **error**, **success**. Empty states should include an appropriate action, e.g. “Non hai ancora caricato documenti” / “Carica il primo documento”.
- Use Italian labels, validation messages, dates, and accessible names. Technical identifiers remain English.

### 2.4 Medical-service icons

Each `medical_services` record can have an **optional** `icon_key` / API `iconKey`. It is a symbolic identifier, **not** an SVG blob, raw markup, or URL. The React frontend resolves it through a curated registry in `src/components/icons/` to Lucide components or approved bundled SVGs in `src/assets/medical-icons/`. The backend accepts only approved keys for custom-service selection; absent/unknown keys use a category fallback. Do not use user-supplied remote icon URLs or permit arbitrary SVG uploads in MVP.

Suggested sizes: ~20 px in navigation/buttons, ~24 px in cards, context-dependent larger illustrations. Medical-service icons appear in event lists, selectors, details, and calendar items when space permits. Compact month/year calendar views prioritize readability and may use colored indicators instead of full icons. Icon choices are curated during implementation; external asset licenses must be checked.

## 3. Information architecture and navigation

### 3.1 Public routes

- Login.
- Registration.

No email verification, password-reset-by-email, or public health-data pages in MVP. Registration does **not** automatically authenticate; the user proceeds to login.

### 3.2 Authenticated navigation

Primary sidebar:

1. **Dashboard**
2. **Calendario**
3. **Prestazioni**
4. **Analisi di laboratorio**
5. **Documenti**
6. **Impostazioni**

Settings subsections: **Profilo**, **Sicurezza**, **Gestione account**. The profile/avatar menu provides a shortcut to settings and **Esci**. Sidebar is persistent on desktop, compact/collapsible on tablet, and opened by an accessible menu control in the mobile header. The header shows the current page title and initials-based avatar (no uploaded profile photo). Active navigation and focus are visually distinct.

The *Prestazioni* area displays all medical events. *Analisi di laboratorio* is a **filtered, specialized view of the same medical events**, not a separate store of duplicated examinations.

### 3.3 Representative frontend routes

Route names below are UI implementation suggestions, not an additional backend contract:

| Route | Page |
|---|---|
| `/login` | Login |
| `/register` | Registration |
| `/dashboard` | Dashboard |
| `/calendar` | Calendar |
| `/medical-events` | Event list |
| `/medical-events/new` | Create event |
| `/medical-events/:eventId` | Event detail with sections |
| `/medical-events/:eventId/edit` | Edit general event data |
| `/laboratory` | Lab examinations and parameter trends |
| `/documents` | Document archive |
| `/documents/new` | Upload document |
| `/documents/:documentId` | Document detail |
| `/settings/profile` | Profile |
| `/settings/security` | Email and password |
| `/settings/account` | Account deletion |

Provide redirects for unauthenticated users, preserve safe intended destination after login where practical, and handle expired sessions gracefully without exposing private content.

## 4. Dashboard (UI-03)

The authenticated landing page contains:

- Personalized greeting using the user's first name; supporting copy emphasizes organization rather than health judgments.
- Three clickable summary cards: **Prossime prestazioni**, **Referti mancanti**, **Documenti archiviati**.
- Compact monthly calendar with medical events and document-expiry indicators.
- Upcoming appointments list showing date, optional time, service name, category, and detail link.
- Quick actions: **Nuova prestazione** and **Carica documento**.

Desktop: summary cards in a row; calendar as the main content region with upcoming appointments alongside; quick actions visible. Mobile: cards and sections stack vertically, with appropriately sized controls.

Missing-report counts derive from the domain rule: completed event with no report text, no report-designated linked document, no lab results, and `report_not_expected = false`. No fabricated health score, daily medication widget, or diagnostic recommendations.

## 5. Calendar (UI-04)

### 5.1 Views and controls

- **Day**, **Week**, **Month**, **Year**; default **Month**.
- Previous/next navigation, **Oggi** shortcut, current period heading.
- Combinable filters: category, effective event status, and document-expiry visibility.
- Month: day grid with category-coded items/indicators; year: compact month grids and indicators; day/week: ordered event entries with **untimed events before timed events**.
- Cancelled events are **excluded** from calendar views, but remain accessible in the medical-events history.

### 5.2 Interactions

- Select an event → accessible summary dialog with name, category, date, time if present, doctor/facility if present, effective status, and **Visualizza dettagli**.
- Select a day → create-event flow with date prefilled.
- Select a document expiry → document summary/detail link.
- Today uses a distinct accent; selected day uses a subtle background; never identify events only by color.
- Event date/time are displayed in the app's Europe/Rome context; do not invent a time for untimed entries.

### 5.3 Responsive

Month stays compact on phones, with selected-day events listed below; day is a vertical list; week can scroll horizontally with clear affordance; year uses a responsive grid. Avoid inaccessible tiny targets and clipped labels. Do not add drag-and-drop rescheduling in MVP; editing occurs through the event form.

## 6. Medical events / Prestazioni (UI-05)

### 6.1 List

- Page heading and **Nuova prestazione** primary action.
- Text search by service name; filters for category, effective status, date range, doctor, facility.
- Tabs: **Programmate**, **Completate**, **Annullate**, **Tutte**; initial tab **Programmate**.
- Event cards display service icon, service name, category, date, optional time, effective status, doctor/facility where present, missing-report indicator when applicable, and **Dettagli**.
- Scheduled sorted nearest first; completed/cancelled most recent first. Default page size 20, with paginated loading consistent with the API.
- Mobile uses vertical cards and collapsible filter panel rather than wide tables.

### 6.2 Create/edit event

Use a **dedicated page**, not a cramped modal. General-information form:

- Service searchable selector (required), with inline **Crea nuova prestazione** for a custom service; custom service has name, category, and optional approved icon choice.
- Date required, time optional.
- Doctor and facility optional searchable/creatable selectors.
- Notes optional.
- Status control respects `AUTO` / `MANUAL` semantics; show computed effective status clearly and avoid suggesting that auto-completed means clinically verified.
- Save general information **first** to create the event; results, report text, and linked documents are then saved independently in the detail page.

Do not add separate event types beyond the three agreed categories. Multiple events of the same service/date are allowed.

### 6.3 Detail and actions

Header: service icon/name, category, effective status, **Modifica**, secondary action menu. Sections/tabs:

- **Informazioni**: date, optional time, doctor, facility, notes, status.
- **Referto**: editable report text, independently saved/deleted with appropriate confirmation.
- **Documenti**: existing linked documents, attach new/existing document, set per-link **È un referto**, unlink without deleting archive file.
- **Risultati**: visible **only** for laboratory events.

Cancel event with confirmation; cancellation is reversible by restoring automatic status. Permanent deletion requires explicit confirmation, removes its lab results and document links, but **does not delete archived documents**. Rescheduling edits the existing event date; no change-history UI in MVP.

## 7. Laboratory / Analisi di laboratorio (UI-06)

### 7.1 Two consultation modes

- **Analisi effettuate**: filtered LABORATORY event list, search, period/status filters, result counts, detail links. Scheduled lab events may have no results yet.
- **Parametri e andamento**: select a parameter and view its historical numerical series and underlying entries.

This is a specialized view of the shared event model.

### 7.2 Results within an event

Desktop table; mobile cards. Fields: parameter, result value, optional unit, optional reference interval, relevant status/indicator. **Aggiungi risultato** opens an accessible form with:

- Searchable parameter selection, including inline custom parameter creation for the relevant lab service.
- Result type `NUMERIC`, `TEXT`, or `UNAVAILABLE`.
- Numeric value or textual value according to type; no numeric value for unavailable results.
- Optional measurement unit for numeric results.
- Optional reference: range, lower bound, upper bound, textual reference, or none. Include inclusive/exclusive operators where relevant.

Allow multiple results for the same parameter in one event and manual ordering. Save results separately from general event information. Preserve each result's historical reference interval; changing catalog metadata must not rewrite past values.

### 7.3 Reference comparison

Only compare a numeric result with its **own compatible numeric reference bounds**, respecting inclusive/exclusive rules. Display restrained text badges such as **Nel riferimento**, **Sopra il riferimento**, **Sotto il riferimento**. If reference information is missing, textual, or not comparable, display no numerical classification. This is an arithmetic comparison to user-entered report bounds, **not** a medical interpretation.

### 7.4 Historical charts

- Parameter selector; chart with dated points and tooltip showing original event/result; detail link.
- Supporting historical table/list with date, value, unit, and the reference interval recorded for each result.
- **Do not mix uncertain units**: separate series by unit, do not automatically convert. Values without a unit remain separate from unit-labeled values.
- TEXT and UNAVAILABLE values are not plotted as numeric points.
- Where a parameter has multiple results on one date, preserve distinct observations rather than silently collapsing them.
- Chart library (e.g. Recharts) to be selected during implementation; provide an accessible tabular alternative and avoid meaning conveyed only through chart colors.

## 8. Document archive / Documenti (UI-07)

### 8.1 Archive and display modes

- Default **Griglia** (document cards); optional **Lista** (compact rows).
- Search title and original filename; filters for category, date range, expiry state, linked/unlinked, sort order.
- Optional tabs/quick filters: **Tutti**, **Collegati a prestazioni**, **Non collegati**.
- Card shows file-type icon, title, category, optional document date, expiry indicator, and detail action.
- Pagination consistent with API; mobile stacks cards and collapses filters.

### 8.2 Upload

Dedicated upload page with drag-and-drop **and** ordinary file picker. Accept PDF, JPEG, PNG; initial maximum **10 MB**, enforced server-side. Fields: file (required), title (required; may be suggested from filename), category (required), custom category when **Altro**, optional document date, expiry date, description, optional event association. A link may mark the document as the event's report. Client checks are helpful feedback but never replace backend file-content, size, authorization, and storage validation.

### 8.3 Document detail and relationships

- Inline preview when browser supports the private authenticated file response; PDF viewer or image display; fallback **Apri/Scarica** action. Do not expose private storage paths or public URLs.
- Metadata, expiry status, linked medical events, per-link report indicator, and attach/detach controls.
- A document may link to multiple events; unlinking does **not** delete it.
- Edit metadata without reuploading; replace file while retaining document identity and event links.
- Permanent deletion prompts confirmation, particularly when linked; removes file and links but not medical events.

### 8.4 Expiry badges

- Future expiry: show date.
- Within **30 days**: **In scadenza**.
- Past: **Scaduto**.
- No expiry date: no expiry badge.

Document expirations appear in calendar in orange, with text labels. No automated reminders or push notifications in MVP. The 30-day threshold is a UI convention, not a clinical rule.

## 9. Settings / Impostazioni (UI-08)

Internal navigation: **Profilo**, **Sicurezza**, **Gestione account**. Desktop: section navigation alongside content; mobile: stacked or compact tab navigation.

### 9.1 Profile

Edit first name, last name, date of birth (required); height in cm and city (optional). Avatar shows initials from the name; no photo upload. Explicit **Salva modifiche**, success/error feedback, and unsaved-change warning where practical.

### 9.2 Security

- **Change email**: current email displayed, new email, current password; uniqueness and credential validation; preserve current session.
- **Change password**: current password, new password, frontend-only confirmation field; show minimum 12 characters and visibility toggles. Backend receives only current/new password. Other sessions are invalidated; current session remains active.
- Do not promise email verification, password recovery email, or per-device session management UI in MVP.

### 9.3 Account deletion and logout

Separate destructive **Zona pericolosa** with explicit irreversible-deletion explanation and current-password confirmation. Explain that profile, medical events, results, documents, and private user data are deleted. Logout is accessible in avatar menu and ends only the current session.

## 10. Authentication screens

Use the same Medical Blue design language without the authenticated sidebar. Forms are centered, readable, and responsive.

- **Registration**: first name, last name, date of birth, email, password (one field with visibility toggle), required-field validation and understandable password guidance. Optional profile fields can be edited later. Successful registration leads to login; **no automatic login**.
- **Login**: email and password, password visibility toggle, clear invalid-credentials feedback without account enumeration. Avoid exposing whether an email is registered.
- Session timeout: after **30 minutes of inactivity**, a protected action or refresh may require login; show a clear Italian message and do not falsely imply unsaved data were saved.
- The frontend obtains CSRF data via `/api/auth/csrf` and sends it on mutations; authentication uses secure server-side sessions and cookie handling per architecture/API docs.

## 11. Responsive behavior

| Context | Navigation | Content pattern |
|---|---|---|
| Desktop | Fixed left sidebar + top header | Multi-column cards, calendar, tables where appropriate |
| Tablet | Compact/collapsible sidebar | Responsive grids and reduced columns |
| Mobile | Accessible menu button in header | Stacked cards, full-width fields, compact calendar, tabular alternatives |

Use tested responsive breakpoints selected during implementation rather than treating a single device width as authoritative. Keep core actions visible, prevent horizontal overflow except deliberate calendar/week scrolling, and ensure dialogs and tabs are usable with keyboard and touch.

## 12. Accessibility, safety, and quality acceptance criteria

The following are **design/implementation targets**, not claims of achieved conformance:

1. Interactive controls have meaningful accessible names and visible keyboard focus; modal focus is managed and restored.
2. All forms expose labels, validation messages, required state, and programmatic error associations.
3. Contrast is tested for actual text/background pairs; color is never the sole carrier of category, status, or reference comparisons.
4. Calendar and charts have usable text/list alternatives; charts expose values without relying on visual position alone.
5. Loading, empty, error, and success states are implemented for all data-driven pages; errors do not leak sensitive details.
6. File previews are authenticated; browser-facing content uses safe content types/headers and does not expose filesystem paths.
7. Destructive operations clearly distinguish **cancel**, **unlink**, **delete event**, **delete document**, and **delete account**.
8. Dates and optional times remain faithful to recorded data; status displays use effective server-side status.
9. No UI element implies medical diagnosis, clinical significance, or verified performance of an auto-completed appointment.
10. Validate navigation and layouts with keyboard, screen-reader smoke tests, narrow screens, and synthetic sample data.

## 13. Scope exclusions and implementation follow-ups

**Out of MVP:** dark mode, arbitrary icon upload, event drag-and-drop, notifications/reminders, offline mode, medication schedules, vaccinations, hospitalizations, emergency visits, surgeries, measurements, allergy/condition management, PDF export, public hosting, clinical inference.

**Before coding or during contract refinement:**

- Finalize document category codes and ensure filters/forms match API DTOs.
- Finalize read-only historical lab-series endpoint and chart payload; preserve per-result unit and reference information.
- Specify doctor/facility merge UI only when backend merge behavior is finalized; do not imply a currently available merge endpoint.
- Finalize icon-key allowlist/versioning and approved SVG licensing.
- Finalize file preview/download headers and authenticated multipart upload contract.
- Define the exact calendar query bounds, timezone/DST edge cases, and effective-status behavior.
- Decide whether user-selected Grid/List display preference persists locally; default remains Grid regardless.
- Convert the design baseline into component tickets, wireframes, interaction tests, and verified responsive/accessibility acceptance tests.

## 14. Approved decision register

| Decision | Approved direction |
|---|---|
| UI-01 | Medical Blue palette, category accents, light mode |
| UI-02 | Desktop sidebar/header, tablet compact navigation, mobile menu |
| UI-03 | Dashboard: three summary cards, compact calendar, upcoming appointments, quick actions |
| UI-04 | Calendar: day/week/month/year, month default, filters, event dialogs, date-prefilled creation |
| UI-05 | Medical events: card list, state tabs, search/filters, dedicated form, section-based detail |
| UI-06 | Laboratory: examination view + parameter trends, structured results, compatible-unit charts |
| UI-07 | Documents: Grid default / List alternative, upload, preview, links, expiry states |
| UI-08 | Settings: profile, security, account deletion, initials avatar |
| UI-09 | Inter typography, reusable controls, 12 px cards, accessible states, curated service icons |

**Next step:** establish the development plan and repository scaffold, then implement and test features incrementally against these documents. The specification is a design baseline and will evolve through explicitly tracked decisions.
