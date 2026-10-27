# Epics & User Stories

**Document:** `SOC-DOC-DEL-02` · **Status:** Draft for engineering

This backlog organises delivery into **epics** (one per module, plus three foundation epics) and
**user stories**. Stories are tied to BRD functional requirements (FR IDs) and the module specs in
[`../modules/`](../modules/README.md). Sizes are relative (S ≈ ½ sprint for one pair, M ≈ 1 sprint,
L ≈ 2 sprints, XL ≈ 3+ sprints — see [`estimation-and-team.md`](estimation-and-team.md)).
Acceptance criteria are Given/When/Then and testable — see [`testing-strategy.md`](testing-strategy.md)
and [`definition-of-done.md`](definition-of-done.md).

**Personas:** Secretary · Treasurer · Committee Member · Owner · Tenant · Guard · Auditor · Platform Admin.

---

## Foundation Epic F1 — Platform, IAM & Tenancy

*Cross-cutting foundation (Phase 0). See [`../02-system-design.md`](../02-system-design.md) §1 and the RLS model in [`../01-architecture.md`](../01-architecture.md) §6.*

| # | Story | Size |
|---|---|---|
| F1.1 | As a **Platform Admin**, I want to onboard a new society as an isolated tenant, so that its data never mixes with another society's. | M |
| F1.2 | As an **Owner**, I want to log in with OTP then bind an OIDC identity, so that onboarding is frictionless but secure. | M |
| F1.3 | As a **Secretary**, I want role-based access (Secretary/Treasurer/Committee/Guard), so that each volunteer sees only what their role permits. | M |
| F1.4 | As a **Platform Admin**, I want every request tenant-scoped at the database, so that a bug in code cannot leak cross-tenant rows. | L |

**F1.4 acceptance criteria**
- Given tenant A's session, When a query selects rows with tenant B's `tenant_id`, Then zero rows return (RLS enforced).
- Given a request with no `app.tenant_id` set, When any tenant-owned table is queried, Then the query returns zero rows (fail-closed).
- Given a JWT, When it reaches a module, Then `tenant_id` and roles derive only from signed gateway claims.

---

## Foundation Epic F2 — Notification Fan-out

*Phase 0/1. In-app → WhatsApp → SMS → IVR escalation (see [`../02-system-design.md`](../02-system-design.md)).*

| # | Story | AC (abridged) | Size |
|---|---|---|---|
| F2.1 | As a **Resident**, I want notices via my preferred channel, so that I actually receive them. | Given channel prefs, When a notice fires, Then it uses the top available channel and logs delivery. | M |
| F2.2 | As a **Secretary**, I want failed channels to escalate (in-app→WhatsApp→SMS→IVR), so that critical dues reminders land. | Given WhatsApp fails, When retry exhausts, Then SMS is attempted and the fallback is audited. | M |
| F2.3 | As a **Platform Admin**, I want notifications driven by Kafka events, so that modules stay decoupled from channels. | Given `defaulter.stage.advanced`, When consumed, Then exactly one notification is produced (idempotent). | S |

---

## Foundation Epic F3 — Observability & Audit

*Phase 0, always-on. OTel/Prometheus/Grafana; append-only hash-chained audit.*

| # | Story | AC (abridged) | Size |
|---|---|---|---|
| F3.1 | As a **Platform Admin**, I want distributed traces across modules, so that I can debug a slow request end-to-end. | Given a request, When it spans gateway→module→DB, Then one trace with correlated spans appears in Grafana. | M |
| F3.2 | As an **Auditor**, I want an append-only tamper-evident log, so that I can trust the record of financial/vote actions. | Given a logged action, When an UPDATE/DELETE is attempted, Then it is rejected; hash chain verifies. | M |
| F3.3 | As an **SRE**, I want SLO alerts tied to 99.95% availability, so that we page before breaching. | Given error-budget burn > threshold, When sustained, Then an alert fires to on-call. | S |

---

## Epic M1 — Autopilot Treasury, Billing & ERP (FR-1.1–1.5)

*Phase 1. [module-1-treasury-erp.md](../modules/README.md)*

| # | Story | Size |
|---|---|---|
| M1.1 | As a **Treasurer**, I want invoices raised automatically from billing formulas, so that I don't compute dues by hand. | M |
| M1.2 | As an **Owner**, I want a dedicated VAN/UPI VPA, so that my payment is recognised without me quoting a reference. | M |
| M1.3 | As a **Treasurer**, I want payments auto-allocated to the right invoice and ledger, so that reconciliation is zero-touch. | L |
| M1.4 | As a **Secretary**, I want a defaulter cadence (Day +1/+10/+15/+30), so that follow-up happens without me nagging. | M |
| M1.5 | As a **Treasurer**, I want maker-checker payouts, so that no single person can move society money. | M |
| M1.6 | As an **Auditor**, I want a double-entry ledger, so that every rupee is traceable and balances. | L |

**M1.3 — zero-touch payment allocation (headline flow)** — *FR-1.3*
- Given a settled payment webhook from the PA, When it arrives, Then it matches the invoice by VAN/VPA and posts CR receivable / DR bank within the same transaction.
- Given a duplicate webhook (same idempotency key), When re-delivered, Then no second ledger posting occurs.
- Given a payment that matches no open invoice, When processed, Then a `payment.unmatched` event routes it to an ops queue (no silent loss).

**M1.4 — defaulter cadence** — *FR-1.4*
- Given an invoice unpaid at Day +1, When the cadence runs, Then stage 1 reminder is produced via Notification fan-out.
- Given payment received mid-cadence, When settled, Then remaining escalations are cancelled.
- Given stage advance, When fired, Then `defaulter.stage.advanced` is emitted and audited.

**M1.6 — double-entry ledger** — *FR-1.5*
- Given any posting, When committed, Then total debits equal total credits (invariant enforced).
- Given a ledger entry, When queried, Then it is immutable; corrections are reversing entries, not edits.

---

## Epic M2 — Statutory Governance & e-AGM (FR-2.1–2.4)

*Phase 2. Depends on M1 ledger. [module-2-governance-agm.md](../modules/README.md)*

| # | Story | Size |
|---|---|---|
| M2.1 | As a **Secretary**, I want statutory AGM notices issued with the required notice period, so that the meeting is legally valid. | M |
| M2.2 | As a **Committee Member**, I want live quorum tracking, so that we know when the meeting can transact business. | M |
| M2.3 | As an **Owner**, I want to cast a cryptographically verifiable ballot, so that my vote is secret yet countable. | XL |
| M2.4 | As a **Committee Member**, I want capex/fund-hike resolutions to reference live fund balances, so that voting is grounded in real finances. | L |
| M2.5 | As a **Secretary**, I want a clean committee handover pack, so that the incoming committee inherits records intact. | M |

**M2.3 — e-AGM voting (headline flow)** — *FR-2.3*
- Given an eligible Owner, When they cast a ballot, Then exactly one vote is recorded per unit and `agm.vote.cast` is audited.
- Given tallying, When the vote closes, Then the result is verifiable against cast ballots without revealing voter identity.
- Given a non-eligible or duplicate attempt, When submitted, Then it is rejected and logged.

**M2.4 — capex voting against ledger** — *FR-2.4*
- Given a capex resolution, When drafted, Then it reads the current sinking-fund balance from the M1 ledger (not a stale copy).

---

## Epic M3 — AI Committee Copilot (FR-3.1–3.4)

*Phase 2. Indexes M1 + M2 corpora. [module-3-ai-copilot.md](../modules/README.md)*

| # | Story | Size |
|---|---|---|
| M3.1 | As a **Treasurer**, I want to ask financial questions in natural language, so that I get answers without SQL. | L |
| M3.2 | As a **Secretary**, I want auto-generated Minutes of Meeting, so that I don't transcribe by hand. | L |
| M3.3 | As a **Committee Member**, I want a bylaw advisor, so that decisions cite the relevant rule. | M |
| M3.4 | As a **Committee Member**, I want tender comparison matrices, so that vendor selection is objective. | M |

**M3.1 — Copilot NL query (headline flow)** — *FR-3.1*
- Given a NL question over ledger/notice data, When submitted, Then the answer is grounded with citations to source rows (RAG over pgvector).
- Given the same query, When repeated, Then a cached result returns and p95 latency is < **2.5 s**.
- Given data outside the tenant, When retrieved, Then it is never surfaced (retrieval is RLS-scoped).

---

## Epic M4 — Offline Gate Infrastructure (FR-4.1–4.3)

*Phase 1. Feeds M5 purge. [module-4-gate-infra.md](../modules/README.md)*

| # | Story | Size |
|---|---|---|
| M4.1 | As a **Guard**, I want check-in to work with no internet, so that the gate never stalls. | L |
| M4.2 | As a **Guard**, I want a UI in my language, so that I can operate it confidently. | M |
| M4.3 | As an **Owner**, I want to pre-approve a visitor with a QR, so that entry is fast and logged. | M |
| M4.4 | As a **Guard**, I want ANPR to read vehicle plates, so that known vehicles pass smoothly. | M |

**M4.1 — offline gate check-in (headline flow)** — *FR-4.1*
- Given no connectivity, When a Guard checks a visitor in, Then the decision is made locally in < **1.5 s** and stored in SQLite.
- Given a 6-hr blackout with N check-ins, When connectivity returns, Then all N reconcile via CRDT with **zero data loss**.
- Given a reconnect merge conflict, When resolved, Then the CRDT merge is deterministic and audited.

---

## Epic M5 — DPDP Act Privacy Shield (FR-5.1–5.3)

*Cross-cutting, Phase 1 onward (designed from day 1). [module-5-privacy-shield.md](../modules/README.md)*

| # | Story | Size |
|---|---|---|
| M5.1 | As an **Owner**, I want a guarantee that no ads or third-party trackers run, so that my data isn't monetised. | M |
| M5.2 | As a **Tenant**, I want my visitor/PII data auto-purged at 30 days, so that it isn't retained indefinitely. | L |
| M5.3 | As an **Owner**, I want granular consent and directory masking, so that I control who sees my contact details. | L |

**M5.2 — 30-day purge (headline flow)** — *FR-5.2*
- Given a `gate.visit.logged` event, When 30 days elapse, Then `privacy.purge.due` fires and the PII is hard-deleted.
- Given purge completion, When verified, Then no residual PII exists in primary, replica, or backups within policy window.
- Given an audit query, When run, Then the purge action itself is logged (the deletion is accountable).

**M5.1 — zero-ad / no-SDK**
- Given a build, When scanned in CI, Then zero third-party tracking/ad SDKs are present (compliance gate blocks merge otherwise).

---

## Epic M6 — Smart-Switch Migration (FR-6.1–6.3)

*Phase 2. Writes into M1/M2. [module-6-smart-switch.md](../modules/README.md)*

| # | Story | Size |
|---|---|---|
| M6.1 | As a **Secretary**, I want to import our Tally ledgers, so that history carries over. | L |
| M6.2 | As a **Secretary**, I want MyGate visitor/member data imported, so that we don't re-enter residents. | M |
| M6.3 | As a **Treasurer**, I want migrated balances verified, so that I trust opening balances. | L |
| M6.4 | As a **Secretary**, I want a parallel run, so that we switch over with zero downtime. | M |

**M6.1 / M6.3 — Tally/MyGate migration (headline flow)** — *FR-6.1, FR-6.3*
- Given a Tally/MyGate export, When ETL ingests it, Then records map to M1 ledger / M2 member master with a reconciliation report.
- Given migrated balances, When verified against source, Then variance is zero (or flagged line-by-line, never silently dropped).
- Given parallel run, When both systems process a day, Then outputs reconcile before cut-over.

---

## Story → FR traceability

| FR | Stories |
|---|---|
| FR-1.1–1.5 | M1.1–M1.6 |
| FR-2.1–2.4 | M2.1–M2.5 |
| FR-3.1–3.4 | M3.1–M3.4 |
| FR-4.1–4.3 | M4.1–M4.4 |
| FR-5.1–5.3 | M5.1–M5.3 |
| FR-6.1–6.3 | M6.1–M6.4 |
