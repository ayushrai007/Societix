# 00 — Product Overview

**Document:** `SOC-DOC-00` · **Source BRD:** `SOC-BRD-2026-V1` · **Status:** Draft for engineering

## 1. Vision

India's urban gated communities are dense private micro-economies managing ₹2 Cr–₹15 Cr annual
budgets, administered by **unpaid volunteer Managing Committees (MC)** using spreadsheets, untracked
UPI payments, and hostile WhatsApp threads. Incumbents (MyGate, NoBrokerHood) subsidize software by
monetizing resident data through ad networks and brokerage funnels.

**Societix** is an autonomous, privacy-first operating system that *"minimises the secretary"* —
turning manual volunteer work into zero-touch automated workflows, statutory audit compliance, and
cryptographically verified governance, with a **100% zero-ad, DPDP-compliant guarantee**.

**North-star outcome:** reduce committee operational time from **6–10 hrs/week** to **< 45 min/week**.

## 2. Problem (why this exists)

| Failure mode | Impact (BRD §2) |
|---|---|
| Manual reconciliation / vendor review / disputes | **8.5 hrs/week** per officer |
| Collection friction & social awkwardness | **18–32%** uncollected cash deficit |
| Ad-SDKs harvesting gate/resident logs | up to **₹250 Cr** DPDP statutory exposure |

## 3. Personas

| Persona | Role | Primary surface | Key jobs |
|---|---|---|---|
| **Secretary / Treasurer (MC officer)** | Runs the society; approves payments, calls AGMs | Web console + mobile | Treasury oversight, defaulter handling, e-AGM, audit packs |
| **Committee member** | Votes, reviews, approves | Web + mobile | Maker-checker approvals, voting |
| **Resident (owner)** | Pays maintenance, invites visitors, votes | Mobile app | Pay dues, visitor QR, AGM e-vote, raise tickets |
| **Tenant** | Occupant, limited rights | Mobile app | Visitor mgmt, tickets (no vote) |
| **Security guard** | Operates the gate | Gate tablet (offline) | Visitor check-in, QR scan, ANPR, delivery fast-track |
| **Statutory auditor** | External read-only | Web (scoped) | Immutable audit trail, audit packs |
| **Platform admin (Societix)** | Operates the SaaS | Internal console | Tenant onboarding, migration, support |

## 4. Scope — functional modules

Societix ships **6 modules** (authoritative numbering, maps 1:1 to BRD §8–9). Full specs in
[`modules/`](modules/README.md).

- **M1 — Autopilot Treasury, Billing & Double-Entry ERP** (FR-1.1–1.5): multi-formula billing,
  static Virtual Account Numbers (VAN) + UPI VPA, escalating defaulter cadence, real-time
  double-entry ledger, maker-checker vendor payouts.
- **M2 — Statutory Governance & e-AGMs** (FR-2.1–2.4): digital notice registry, real-time quorum
  engine, cryptographic secret balloting, committee handover escrow.
- **M3 — AI Committee Copilot** (FR-3.1–3.4): NL analytics, auto minutes (MoM), state bylaw advisor,
  tender comparison matrix.
- **M4 — Offline Gate Infrastructure** (FR-4.1–4.3): local SQLite edge sync, multilingual audio UI
  (8 languages), smart access rails (QR / delivery fast-track / ANPR).
- **M5 — DPDP Act Privacy Shield** (FR-5.1–5.3): zero-ad SDK guarantee, 30-day auto data purge,
  granular resident consent & directory masking.
- **M6 — Smart-Switch Migration** (FR-6.1–6.3): automated ingestion ETL (Tally/Excel/MyGate/ADDA),
  balance verification, zero-downtime parallel run.

## 5. Non-functional targets (BRD §10)

| Dimension | Target |
|---|---|
| Gate check-in latency | < 1.5 s visitor push (4G/5G), 5,000 req/s stress |
| Offline data integrity | zero loss over 6-hour gate blackout (SQLite WAL + CRDT) |
| Platform availability | 99.95% across treasury & security; single-region + managed-Postgres backups/PITR in v1, multi-region active-passive as a later scale step |
| AI query response | < 2.5 s end-to-end for NL committee queries |
| Data isolation | per-society row/schema tenancy (PostgreSQL RLS, dedicated keys) |

## 6. Business guardrails that shape architecture

- **No float:** maintenance collections settle directly to the society bank account via a licensed
  Payment Aggregator. Societix is a *technology layer only* (RBI PA alignment). See
  [ADR-0003](adr/0003-payment-aggregator-no-float.md).
- **Zero ads / zero commercial tracking:** no third-party ad or tracking SDKs, ever (M5).
- **Data residency:** personal data stays in sovereign Indian regions (Mumbai & Hyderabad).
- **Immutable audit:** every admin action (ledger adjustment, visitor-photo query, vote) is recorded
  in a write-once tamper-evident log.

## 7. Out of scope (v1)

Home-services marketplace, lead-gen, advertising, resident social network, non-India regulatory
regimes. (These are explicitly *anti-goals* given the privacy positioning.)

## 8. Glossary

See [`glossary.md`](glossary.md) for MC, AGM, VAN/VPA, sinking fund, quorum, DPDP, RBI PA, etc.
