# Societix — Jira Backlog (readable mirror)
**What this is:** every epic and task for the whole project, mirroring `societix-backlog.csv` exactly (same IDs). Import the CSV into Jira, or read here to plan.
**Totals:** 11 epics · 45 tasks · 125 story points (~1 point ≈ 1 developer-day).
> Task size target: 1–3 days. A few genuinely bigger items are 5 points and marked **PAIR TASK** where two of us should build together.
> Sprints S0–S13 are 2-week sprints from the plan in [05-building-it.md](../05-building-it.md).

---

## Epic: Foundations: Project Setup

_Repo, build, local infra, CI skeleton, walking skeleton._

**6 tasks · 11 points**

### SOC-1 — Create the code repository and branch rules

- **Points:** 1  |  **Priority:** Highest  |  **Sprint:** S0  |  **Component:** Platform  |  **Labels:** `phase-0,setup`

**Description:** Set up the Git repository on GitHub with a protected main branch. Add a README that points to docs/. Configure branch protection so every change goes through a Pull Request with one approval and passing checks. Where to start: create the repo, add .gitignore for Java/Node, enable branch protection in repo settings. Fits in: this is the very first task; everything else branches off it.

**Acceptance criteria:**
- main branch cannot be pushed to directly
- a PR requires 1 approval + green checks before merge
- repo README links to docs/

### SOC-2 — Scaffold the Spring Boot backend as a modular monolith

- **Points:** 2  |  **Priority:** Highest  |  **Sprint:** S0  |  **Component:** Platform  |  **Labels:** `phase-0,setup`

**Description:** Create the Java 25 + Spring Boot 4.1.2 backend using Maven 3.10.0. Add Spring Modulith 2.1.2 and create empty module packages: treasury, governance, copilot, gate, privacy, migration, platform. One runnable app that starts and serves a /health endpoint. Where to start: Spring Initializr, add WebMVC/Validation/Actuator/Modulith deps. See docs/03-tech-choices.md for why these tools.

**Acceptance criteria:**
- `cd backend && ./mvnw spring-boot:run` starts the app
- GET /actuator/health returns 200
- each module is its own package with a package-info marking the module boundary

### SOC-3 — Local dev environment with Docker Compose

- **Points:** 2  |  **Priority:** Highest  |  **Sprint:** S0  |  **Component:** Platform  |  **Labels:** `phase-0,setup`

**Description:** Write docker-compose.yml that starts PostgreSQL 18 with pgvector 0.8.7 and Redis 8.2 locally, plus a mock payment provider. Add a sample .env and document the commands in docs/01-start-here.md. No message broker (we use the outbox). Where to start: copy the compose snippet from the archived getting-started doc and trim Kafka.

**Acceptance criteria:**
- docker compose up -d starts Postgres + Redis + payment mock
- app connects to the local DB
- a new dev can go from clone to running app following docs/01-start-here.md

### SOC-4 — Set up the CI pipeline (build + test on every PR)

- **Points:** 2  |  **Priority:** High  |  **Sprint:** S0  |  **Component:** Platform  |  **Labels:** `phase-0,setup,ci`

**Description:** Add a GitHub Actions workflow that builds the backend, runs tests, and runs static checks on every PR. Add a secret/dependency scan step. Keep it fast. Where to start: a basic Maven verify workflow, then add scans.

**Acceptance criteria:**
- every PR triggers build + test
- a failing test blocks merge
- a secret-scanning step runs

### SOC-5 — Database migrations with Flyway

- **Points:** 1  |  **Priority:** High  |  **Sprint:** S1  |  **Component:** Platform  |  **Labels:** `phase-0,setup,db`

**Description:** Wire in Flyway so database schema changes are version-controlled SQL scripts applied automatically on startup. Add the first migration (an empty baseline). Document the naming convention (V1__description.sql). Why: so every environment has an identical, reproducible schema.

**Acceptance criteria:**
- Flyway runs migrations on app startup
- a sample V1 migration applies cleanly
- docs explain how to add a new migration

### SOC-6 — Walking skeleton: one feature end-to-end

- **Points:** 3  |  **Priority:** High  |  **Sprint:** S2  |  **Component:** Platform  |  **Labels:** `phase-0,setup`

**Description:** Prove the whole chain works with the thinnest possible slice: an endpoint that creates a dummy invoice, a call to the mock payment provider, and a record of the result in the DB. No real business logic yet. Why: de-risks the architecture before we build real features.

**Acceptance criteria:**
- can create a dummy invoice via API
- mock payment webhook updates its status
- the flow has one integration test

## Epic: Foundations: Auth & Tenancy

_Login, roles, and keeping each society's data private (RLS)._

**4 tasks · 12 points**

### SOC-10 — Define the Society (tenant) entity and onboarding

- **Points:** 2  |  **Priority:** Highest  |  **Sprint:** S0  |  **Component:** Platform  |  **Labels:** `phase-0,auth,tenancy`

**Description:** Create the Society entity (one row per apartment complex) and an admin endpoint to onboard a new society. This is the 'tenant' every other table will belong to. Where to start: entity + migration + repository + a simple create endpoint. See docs/02-how-it-works.md ('one program, one database').

**Acceptance criteria:**
- can create a society via an admin endpoint
- society has id, name, address, state (for later law rules)
- stored via a Flyway migration

### SOC-11 — User login with OTP + OIDC

- **Points:** 3  |  **Priority:** Highest  |  **Sprint:** S1  |  **Component:** Platform  |  **Labels:** `phase-0,auth`

**Description:** Let a user log in. Phone OTP for first contact, bound to a standard OIDC identity, issuing a signed JWT that carries the user id, their society, and their roles. Why OIDC/JWT: standard, secure, we don't invent our own. See glossary for JWT/OIDC.

**Acceptance criteria:**
- a user can log in and receive a JWT
- the JWT contains society id + roles
- expired/invalid tokens are rejected

### SOC-12 — Roles and permissions (RBAC)

- **Points:** 2  |  **Priority:** Highest  |  **Sprint:** S1  |  **Component:** Platform  |  **Labels:** `phase-0,auth`

**Description:** Define roles (Platform Admin, Secretary, Treasurer, Committee Member, Owner, Tenant, Guard, Auditor) and enforce them on endpoints. A resident can't hit a treasurer-only endpoint. Where to start: a role enum + method-level security annotations.

**Acceptance criteria:**
- each role is defined
- a treasurer-only endpoint rejects an owner's token (403)
- roles come only from the signed JWT, never request input

### SOC-13 — Data isolation with Row-Level Security (RLS)

- **Points:** 5  |  **Priority:** Highest  |  **Sprint:** S1  |  **Component:** Platform  |  **Labels:** `phase-0,auth,tenancy,privacy`

**Description:** The big one for privacy. Add a tenant_id column to tenant-owned tables and PostgreSQL RLS policies that filter by the current society, set from the JWT on each request. If code forgets to filter, the DATABASE still hides other societies' rows. Why: a bug here is a privacy breach, so we enforce it at the DB. See docs/02-how-it-works.md.

**Acceptance criteria:**
- a request for society A returns zero rows from society B
- a query with no society set returns zero rows (fail-closed)
- there is an automated test proving cross-society reads are blocked

## Epic: Foundations: Outbox & Jobs

_The outbox table + background jobs that power automatic behaviour._

**3 tasks · 7 points**

### SOC-20 — Create the outbox table and event model

- **Points:** 2  |  **Priority:** Highest  |  **Sprint:** S1  |  **Component:** Platform  |  **Labels:** `phase-0,events`

**Description:** Add the 'outbox' table: a simple list of 'things that happened' (event type, payload, tenant, created time, processed flag). Producers insert a row in the SAME database save as their change. Why: reliable automatic behaviour with no Kafka. See docs/02-how-it-works.md ('the outbox').

**Acceptance criteria:**
- outbox table exists via migration
- a helper lets any module write an event inside its transaction
- events carry the society id

### SOC-21 — Background job that drains the outbox

- **Points:** 3  |  **Priority:** Highest  |  **Sprint:** S2  |  **Component:** Platform  |  **Labels:** `phase-0,events`

**Description:** Add a background job that runs every few seconds, reads unprocessed outbox rows, and calls the matching handler, then marks them processed. Guard it so only one app instance runs it at a time (ShedLock). Why: this is what makes 'automatic' happen. Handlers must be safe to run twice (idempotent).

**Acceptance criteria:**
- unprocessed events are dispatched within seconds
- only one instance processes at a time
- a handler running twice has the same effect as once

### SOC-22 — Scheduled jobs for time-based automation

- **Points:** 2  |  **Priority:** High  |  **Sprint:** S2  |  **Component:** Platform  |  **Labels:** `phase-0,events`

**Description:** Add a scheduling mechanism for 'do X after N days' needs (defaulter reminders, 30-day data purge). A daily job queries rows that are now due. Where to start: a simple @Scheduled job + a query-due-rows pattern reused by M1 and M5.

**Acceptance criteria:**
- a scheduled job can find rows due today
- the mechanism is reusable by Treasury and Privacy
- runs once per schedule even with multiple instances

## Epic: Foundations: Notifications

_Sending in-app / WhatsApp / SMS messages, with fallback._

**2 tasks · 5 points**

### SOC-30 — Notification service with channel adapters

- **Points:** 3  |  **Priority:** High  |  **Sprint:** S2  |  **Component:** Platform  |  **Labels:** `phase-1,notify`

**Description:** A service that sends a message to a resident via a channel (in-app, WhatsApp, SMS). Start with in-app + one real channel behind an interface so others slot in. Driven by outbox events. Where to start: an interface + in-app impl + a WhatsApp stub.

**Acceptance criteria:**
- a module can request 'notify resident X'
- at least in-app + one external channel work
- sending is triggered by an outbox event (decoupled)

### SOC-31 — Channel fallback and delivery logging

- **Points:** 2  |  **Priority:** Medium  |  **Sprint:** S3  |  **Component:** Platform  |  **Labels:** `phase-1,notify`

**Description:** If the preferred channel fails, escalate (in-app -> WhatsApp -> SMS). Log what was delivered and when (needed for legal proof of meeting notices later). Keep it idempotent so one event = one message.

**Acceptance criteria:**
- a failed channel escalates to the next
- delivery attempts are logged
- the same event never sends two duplicate messages

## Epic: Foundations: Audit & Monitoring

_Tamper-proof audit log + basic health/metrics._

**2 tasks · 5 points**

### SOC-40 — Append-only, tamper-evident audit log

- **Points:** 3  |  **Priority:** High  |  **Sprint:** S1  |  **Component:** Platform  |  **Labels:** `phase-0,audit,privacy`

**Description:** A write-once audit table recording sensitive actions (money moves, votes, data deletions). Rows can't be edited or deleted; each links to the previous via a hash so tampering is detectable. Auditors read it. Why: trust + legal. See glossary (audit / hash-chain).

**Acceptance criteria:**
- sensitive actions write an audit row
- UPDATE/DELETE on the table is rejected
- the hash chain can be verified end-to-end

### SOC-41 — Health checks and basic metrics

- **Points:** 2  |  **Priority:** Medium  |  **Sprint:** S1  |  **Component:** Platform  |  **Labels:** `phase-0,observability`

**Description:** Expose health and basic metrics via Spring Boot Actuator and structured logging. Enough to see if the app is up and spot errors. Full dashboards (Grafana) come later. Why: we need visibility without heavy tooling now.

**Acceptance criteria:**
- /actuator/health and basic metrics are exposed
- logs are structured (not plain text) and contain no personal data
- an error is visible in logs/metrics

## Epic: M1 Treasury (Money)

_Billing, auto payment collection, defaulter chasing, ledger._

**7 tasks · 23 points**

### SOC-100 — Chart of accounts and double-entry ledger tables

- **Points:** 3  |  **Priority:** Highest  |  **Sprint:** S3  |  **Component:** Treasury  |  **Labels:** `phase-1,m1,treasury`

**Description:** Create the accounting foundation: account types and a ledger where every transaction records matching debits and credits (so books always balance). Include the statutory fund accounts (sinking/repair/reserve/maintenance). See docs/04-the-6-modules.md#m1 and the archived ledger deep-dive.

**Acceptance criteria:**
- ledger tables exist via migration
- a posting must have equal debits and credits or it is rejected
- the standard fund accounts are seeded

### SOC-101 — Multi-formula billing engine

- **Points:** 3  |  **Priority:** Highest  |  **Sprint:** S3  |  **Component:** Treasury  |  **Labels:** `phase-1,m1,treasury`

**Description:** Generate monthly invoices for each flat. Support the common formulas: flat rate, by carpet area (per sq.ft), by metered usage (water), or a mix. Where to start: a BillingFormula strategy per type + an invoice generator job.

**Acceptance criteria:**
- can generate invoices for all flats in a society
- supports flat-rate and per-sq-ft at minimum
- raising an invoice writes a billing.invoice.raised event

### SOC-102 — Give each flat a payment number (VAN / UPI VPA)

- **Points:** 3  |  **Priority:** Highest  |  **Sprint:** S4  |  **Component:** Treasury  |  **Labels:** `phase-1,m1,treasury,payments`

**Description:** Integrate the payment provider to assign each flat a unique virtual account number and UPI address, so incoming money is auto-identified. Use the mock provider locally. See glossary (VAN/UPI VPA). Pairs with SOC-103.

**Acceptance criteria:**
- each flat gets a unique VAN and UPI VPA
- the mapping flat<->VAN is stored
- works against the mock provider locally

### SOC-103 — Zero-touch payment matching (PAIR TASK)

- **Points:** 5  |  **Priority:** Highest  |  **Sprint:** S4  |  **Component:** Treasury  |  **Labels:** `phase-1,m1,treasury,payments`

**Description:** The headline feature. When the payment provider sends a 'money received' webhook, match it to the flat by its VAN/VPA, mark the invoice paid, and post it to the ledger - all automatically, no manual matching. Build this as a PAIR (Dev A + Dev B); it's the trickiest money path. Must be idempotent (a repeated webhook doesn't double-post).

**Acceptance criteria:**
- a settlement webhook marks the right invoice paid and posts to the ledger
- a duplicate webhook causes no second posting
- a payment matching no invoice goes to an 'unmatched' queue, never lost

### SOC-104 — Defaulter reminder cadence (Day +1 to +30)

- **Points:** 3  |  **Priority:** High  |  **Sprint:** S5  |  **Component:** Treasury  |  **Labels:** `phase-1,m1,treasury`

**Description:** Automatic chasing of unpaid bills: day 1 gentle in-app/WhatsApp nudge, day 10 SMS/call, day 15 late fee added (respecting state-law caps), day 30 a formal notice document. Stops instantly when they pay. Uses the scheduled jobs (SOC-22) and notifications (SOC-30).

**Acceptance criteria:**
- an unpaid invoice triggers the right reminder at each stage
- paying mid-cadence cancels remaining reminders
- each stage change writes an event and is audited

### SOC-105 — Financial statements (trial balance, P&L, balance sheet)

- **Points:** 3  |  **Priority:** Medium  |  **Sprint:** S5  |  **Component:** Treasury  |  **Labels:** `phase-1,m1,treasury`

**Description:** Generate the standard financial reports from the ledger automatically. Where to start: aggregation queries over the ledger; a report endpoint per statement. See the archived ledger deep-dive for the exact math.

**Acceptance criteria:**
- trial balance always balances (debits = credits)
- P&L and balance sheet generate for a date range
- reports are read-only and tenant-scoped

### SOC-106 — Maker-checker vendor payouts with tax tracking

- **Points:** 3  |  **Priority:** Medium  |  **Sprint:** S6  |  **Component:** Treasury  |  **Labels:** `phase-1,m1,treasury`

**Description:** Pay vendors safely: one user requests a payout, a different user approves it before it executes. Track TDS (tax withheld) and GST input credit. See glossary (maker-checker, TDS/GST).

**Acceptance criteria:**
- a payout needs a different approver than the requester
- TDS and GST are recorded on the payout
- an unapproved payout cannot execute

## Epic: M4 Gate (Visitors, offline)

_Offline visitor logging, QR passes, number-plate reading._

**5 tasks · 16 points**

### SOC-120 — Gate app shell with local on-device storage

- **Points:** 3  |  **Priority:** Highest  |  **Sprint:** S3  |  **Component:** Gate  |  **Labels:** `phase-1,m4,gate,mobile`

**Description:** Create the React Native gate tablet app that stores visitor records in a small on-device database first (works with no internet). See docs/04-the-6-modules.md#m4. Pairs with SOC-121 for sync.

**Acceptance criteria:**
- the app runs on a tablet and stores a check-in locally
- works with the network switched off
- local records persist across app restarts

### SOC-121 — Offline sync with conflict-free merge (CRDT)

- **Points:** 5  |  **Priority:** High  |  **Sprint:** S4  |  **Component:** Gate  |  **Labels:** `phase-1,m4,gate,mobile`

**Description:** When the internet returns, the tablet syncs local changes up and server changes down, merging automatically without conflicts (CRDT). The server ingest is idempotent. See the archived offline-gate deep-dive for the technique; most of this task is wiring the sync layer.

**Acceptance criteria:**
- after a long offline period, all local check-ins reach the server with zero loss
- re-syncing the same records doesn't duplicate them
- a merge conflict resolves deterministically

### SOC-122 — Visitor check-in and resident approval

- **Points:** 3  |  **Priority:** High  |  **Sprint:** S5  |  **Component:** Gate  |  **Labels:** `phase-1,m4,gate`

**Description:** Core gate flow: guard logs a visitor, resident gets a notification to approve/deny. Must complete the local check-in in under 1.5 seconds even offline.

**Acceptance criteria:**
- guard can log a visitor in under 1.5s
- resident receives an approve/deny notification when online
- visitor record is stored and later synced

### SOC-123 — Pre-approved visitor QR passes

- **Points:** 2  |  **Priority:** Medium  |  **Sprint:** S5  |  **Component:** Gate  |  **Labels:** `phase-1,m4,gate,mobile`

**Description:** Residents pre-approve a guest in their app and get a QR code; the guard scans it for instant entry. Where to start: QR generation in the resident app + scan+validate in the gate app.

**Acceptance criteria:**
- a resident can create a time-limited guest QR
- the guard app validates a scanned QR
- an expired/invalid QR is rejected

### SOC-124 — Multilingual gate UI + number-plate reading (ANPR)

- **Points:** 3  |  **Priority:** Low  |  **Sprint:** S6  |  **Component:** Gate  |  **Labels:** `phase-1,m4,gate,mobile`

**Description:** Localise the guard UI into the major Indian languages and add automatic vehicle number-plate reading for known vehicles. ANPR can use a third-party library/service. Where to start: i18n framework + an ANPR adapter interface.

**Acceptance criteria:**
- guard UI available in at least 3 languages
- a known vehicle plate is recognised and logged
- an unknown plate falls back to manual entry

## Epic: M5 Privacy Shield

_No ads, 30-day auto-delete of visitor data, resident consent._

**3 tasks · 8 points**

### SOC-140 — CI check: no ads or third-party trackers

- **Points:** 2  |  **Priority:** Highest  |  **Sprint:** S2  |  **Component:** Privacy  |  **Labels:** `phase-0,m5,privacy,ci`

**Description:** Add an automated check in the build pipeline that fails if any ad/tracking SDK appears in the apps. This enforces our zero-ads promise forever. Why: brand + DPDP law. See docs/04-the-6-modules.md#m5.

**Acceptance criteria:**
- a known tracker dependency makes the build fail
- the check runs on every PR
- the allowed/blocked list is documented

### SOC-141 — 30-day automatic deletion of visitor data

- **Points:** 3  |  **Priority:** High  |  **Sprint:** S7  |  **Component:** Privacy  |  **Labels:** `phase-1,m5,privacy`

**Description:** Visitor photos and phone numbers must be hard-deleted 30 days after the visit. Driven by the scheduled jobs. The deletion itself is audited (so we can prove it happened). See glossary (DPDP).

**Acceptance criteria:**
- visitor PII is deleted 30 days after the visit
- deletion covers primary data (and is documented for backups)
- each deletion writes an audit entry

### SOC-142 — Resident consent and directory masking

- **Points:** 3  |  **Priority:** Medium  |  **Sprint:** S7  |  **Component:** Privacy  |  **Labels:** `phase-1,m5,privacy`

**Description:** Let residents choose who can see their details (e.g. hide phone number) without losing access to security features. Store granular consent. Where to start: a consent model + applying masking in the directory API.

**Acceptance criteria:**
- a resident can hide their phone number from the directory
- masking doesn't break their gate/visitor access
- consent choices are stored and auditable

## Epic: M2 Governance & e-AGM

_Meeting notices, live quorum, secret verifiable voting._

**5 tasks · 14 points**

### SOC-160 — AGM meeting notices with proof of delivery

- **Points:** 2  |  **Priority:** High  |  **Sprint:** S8  |  **Component:** Governance  |  **Labels:** `phase-2,m2,governance`

**Description:** Create and send the legally-required meeting notices the right number of days in advance, and record proof they were delivered (via the notification service). See docs/04-the-6-modules.md#m2.

**Acceptance criteria:**
- a notice can be scheduled with the required notice period
- delivery is recorded as proof
- residents receive it on their preferred channel

### SOC-161 — Live quorum engine

- **Points:** 3  |  **Priority:** High  |  **Sprint:** S8  |  **Component:** Governance  |  **Labels:** `phase-2,m2,governance`

**Description:** During a meeting, track attendance and voting weight live (owners vs validated proxies) against the legal threshold, so the committee knows when voting can start. State laws set the weights. See glossary (quorum/proxy).

**Acceptance criteria:**
- attendance updates the quorum count live
- owners and proxies are weighted correctly
- the UI shows 'quorum met' when the threshold is reached

### SOC-162 — Secret, verifiable e-voting (PAIR TASK)

- **Points:** 5  |  **Priority:** High  |  **Sprint:** S9  |  **Component:** Governance  |  **Labels:** `phase-2,m2,governance`

**Description:** The hardest governance piece: online ballots that are secret (no one sees how you voted) yet verifiable (the count can be proven correct and not tampered with). Build as a PAIR. Uses a commit-reveal scheme + the hash-chained audit log - see the archived e-voting deep-dive. Don't start this in a phase's last sprint.

**Acceptance criteria:**
- an eligible owner casts exactly one weighted vote per unit
- the tally is verifiable without revealing individual votes
- duplicate or ineligible votes are rejected and logged

### SOC-163 — Capex voting linked to live fund balances

- **Points:** 2  |  **Priority:** Medium  |  **Sprint:** S10  |  **Component:** Governance  |  **Labels:** `phase-2,m2,governance`

**Description:** When voting on spending from a fund, show the live balance from the Treasury ledger (not a stale copy) so owners vote with real numbers. Depends on M1 ledger (SOC-100).

**Acceptance criteria:**
- a spending resolution shows the current fund balance from the ledger
- the balance is live, not cached stale
- the resolution result is recorded and audited

### SOC-164 — Committee handover pack

- **Points:** 2  |  **Priority:** Low  |  **Sprint:** S10  |  **Component:** Governance  |  **Labels:** `phase-2,m2,governance`

**Description:** When a committee changes, produce a pack transferring records, vendor contracts, bank signatory info, and open items to the incoming committee. Where to start: a report that bundles the relevant records.

**Acceptance criteria:**
- an outgoing committee can generate a handover pack
- it includes contracts, signatories, and open tickets
- access transfers to the incoming committee

## Epic: M3 AI Copilot

_Plain-English Q&A, auto minutes, bylaw & tender help._

**4 tasks · 14 points**

### SOC-180 — Set up pgvector and the knowledge index

- **Points:** 3  |  **Priority:** Medium  |  **Sprint:** S11  |  **Component:** Copilot  |  **Labels:** `phase-2,m3,copilot,ai`

**Description:** Enable the pgvector extension and build the index of searchable society knowledge (bylaws, state-law rules, ledger summaries) as embeddings. This is what the AI searches. See docs/04-the-6-modules.md#m3 and glossary (RAG/embedding).

**Acceptance criteria:**
- pgvector is enabled via migration
- society documents can be embedded and stored
- search returns the most relevant chunks for a query, tenant-scoped

### SOC-181 — Natural-language financial Q&A (RAG)

- **Points:** 5  |  **Priority:** Medium  |  **Sprint:** S11  |  **Component:** Copilot  |  **Labels:** `phase-2,m3,copilot,ai`

**Description:** Let a committee member ask plain-English questions and get grounded answers with citations, built on retrieval (find relevant data) + generation (AI writes the answer). Must stay within the user's society and answer in under 2.5s (cache repeats). See the archived RAG deep-dive.

**Acceptance criteria:**
- a NL question returns an answer grounded in the society's own data with citations
- it never surfaces another society's data
- a repeated query returns within 2.5s

### SOC-182 — Safe natural-language to data queries

- **Points:** 3  |  **Priority:** Medium  |  **Sprint:** S12  |  **Component:** Copilot  |  **Labels:** `phase-2,m3,copilot,ai`

**Description:** Make the NL->data path safe: never run raw AI-generated SQL on production. Use an allow-listed, parameterised, tenant-scoped query layer. Build as a pair with SOC-181. Why: an AI writing arbitrary SQL is a security hole.

**Acceptance criteria:**
- AI cannot execute arbitrary SQL
- all generated queries are validated + parameterised + tenant-scoped
- an injection attempt is blocked and logged

### SOC-183 — Auto-draft meeting minutes + tender comparison

- **Points:** 3  |  **Priority:** Low  |  **Sprint:** S12  |  **Component:** Copilot  |  **Labels:** `phase-2,m3,copilot,ai`

**Description:** Two smaller copilot features: turn meeting notes into a formatted draft of minutes, and turn a set of vendor quotes into a comparison table. Where to start: a summarise prompt + a structured-extraction prompt.

**Acceptance criteria:**
- notes produce a readable draft of minutes
- multiple vendor quotes produce a comparison table
- outputs are editable by the user before saving

## Epic: M6 Smart-Switch Migration

_Import old data (Tally/Excel/MyGate), verify, parallel run._

**4 tasks · 10 points**

### SOC-200 — Import engine for Tally / Excel exports

- **Points:** 3  |  **Priority:** Medium  |  **Sprint:** S10  |  **Component:** Migration  |  **Labels:** `phase-2,m6,migration`

**Description:** Build the importer that reads a society's old accounting data (Tally XML / Excel) and loads it into our ledger and member records. Produce a mapping report. See docs/04-the-6-modules.md#m6.

**Acceptance criteria:**
- a Tally/Excel export is parsed and loaded
- records map to our ledger and member master
- a mapping/summary report is produced

### SOC-201 — Import from MyGate exports

- **Points:** 2  |  **Priority:** Low  |  **Sprint:** S10  |  **Component:** Migration  |  **Labels:** `phase-2,m6,migration`

**Description:** Add an importer for a competitor (MyGate) export - residents, vehicles, visitor history - so switching societies don't re-enter residents. Where to start: a MyGate format adapter reusing the SOC-200 pipeline.

**Acceptance criteria:**
- a MyGate export imports residents and vehicles
- duplicates are detected, not doubled
- unmapped fields are reported, not silently dropped

### SOC-202 — Balance verification after import

- **Points:** 3  |  **Priority:** Medium  |  **Sprint:** S11  |  **Component:** Migration  |  **Labels:** `phase-2,m6,migration`

**Description:** After import, check the loaded opening balances against the source totals and flag any mismatch line-by-line so the Treasurer trusts the numbers. Never silently drop a discrepancy.

**Acceptance criteria:**
- imported totals are compared to source totals
- any variance is flagged line-by-line
- a zero-variance import is marked verified

### SOC-203 — Parallel-run comparison for safe cut-over

- **Points:** 2  |  **Priority:** Low  |  **Sprint:** S11  |  **Component:** Migration  |  **Labels:** `phase-2,m6,migration`

**Description:** Let a society run old + Societix side-by-side for a period and compare outputs before switching fully, so cut-over is zero-risk. Where to start: a daily comparison report of the two systems' key figures.

**Acceptance criteria:**
- both systems can process the same period
- a comparison report highlights differences
- cut-over is gated on a clean comparison
