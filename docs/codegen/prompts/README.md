# Prompts — index & build order

**What this is:** one ready-to-paste Claude prompt per backlog task, in the order to build them.
**How to use:** for each task, load the three context docs (`../00-playbook.md`,
`../01-conventions.md`, `../02-tech-blueprint.md`), then paste the task's prompt. Review + test +
merge before the next one. **Do them in order — later tasks assume earlier ones exist.**

> **About surfaces:** prompts are grouped `backend/` and `mobile/` by where most of the work lives.
> Several backend tasks also include **web dashboard UI** (committee screens) and **resident-app UI**
> as part of the same vertical slice — build the UI alongside the backend in that task. There is no
> separate `web/` prompt set because the web work is small per-task and belongs with its API. If you
> prefer to split UI into its own tickets later, each task notes the UI piece in its deliverables.

## Phase 0 — Foundations (do first)

| Order | Task | Prompt |
|---|---|---|
| 1 | SOC-1 Repository & branch rules | [backend/SOC-1](backend/SOC-1-create-the-code-repository-and-branch-ru.md) |
| 2 | SOC-2 Scaffold Spring Boot backend | [backend/SOC-2](backend/SOC-2-scaffold-the-spring-boot-backend-as-a-mo.md) |
| 3 | SOC-3 Local Docker environment | [backend/SOC-3](backend/SOC-3-local-dev-environment-with-docker-compos.md) |
| 4 | SOC-4 CI pipeline | [backend/SOC-4](backend/SOC-4-set-up-the-ci-pipeline-build-test-on-eve.md) |
| 5 | SOC-5 Flyway migrations | [backend/SOC-5](backend/SOC-5-database-migrations-with-flyway.md) |
| 6 | SOC-10 Society (tenant) entity | [backend/SOC-10](backend/SOC-10-define-the-society-tenant-entity-and-onb.md) |
| 7 | SOC-11 Login (OTP + OIDC → JWT) | [backend/SOC-11](backend/SOC-11-user-login-with-otp-oidc.md) |
| 8 | SOC-12 Roles & permissions | [backend/SOC-12](backend/SOC-12-roles-and-permissions-rbac.md) |
| 9 | **SOC-13 Data isolation (RLS)** | [backend/SOC-13](backend/SOC-13-data-isolation-with-row-level-security-r.md) |
| 9a | SOC-14 Flat/Unit & member master records | [backend/SOC-14](backend/SOC-14-flat-unit-and-member-master-records.md) |
| 10 | SOC-20 Outbox table & events | [backend/SOC-20](backend/SOC-20-create-the-outbox-table-and-event-model.md) |
| 11 | SOC-21 Outbox poller | [backend/SOC-21](backend/SOC-21-background-job-that-drains-the-outbox.md) |
| 12 | SOC-22 Scheduled jobs | [backend/SOC-22](backend/SOC-22-scheduled-jobs-for-time-based-automation.md) |
| 13 | SOC-40 Audit log | [backend/SOC-40](backend/SOC-40-append-only-tamper-evident-audit-log.md) |
| 14 | SOC-41 Health & metrics | [backend/SOC-41](backend/SOC-41-health-checks-and-basic-metrics.md) |
| 15 | **SOC-140 No-trackers CI check** | [backend/SOC-140](backend/SOC-140-ci-check-no-ads-or-third-party-trackers.md) |
| 16 | SOC-6 Walking skeleton (end-to-end slice) | [backend/SOC-6](backend/SOC-6-walking-skeleton-one-feature-end-to-end.md) |

## Phase 1 — The valuable core: Treasury + Gate + Privacy

| Order | Task | Prompt |
|---|---|---|
| 17 | SOC-30 Notification service | [backend/SOC-30](backend/SOC-30-notification-service-with-channel-adapte.md) |
| 18 | SOC-31 Channel fallback & delivery log | [backend/SOC-31](backend/SOC-31-channel-fallback-and-delivery-logging.md) |
| 19 | SOC-100 Chart of accounts & ledger | [backend/SOC-100](backend/SOC-100-chart-of-accounts-and-double-entry-ledge.md) |
| 20 | SOC-101 Billing engine | [backend/SOC-101](backend/SOC-101-multi-formula-billing-engine.md) |
| 21 | SOC-102 VAN / UPI VPA per flat | [backend/SOC-102](backend/SOC-102-give-each-flat-a-payment-number-van-upi-.md) |
| 22 | **SOC-103 Zero-touch payment matching (PAIR)** | [backend/SOC-103](backend/SOC-103-zero-touch-payment-matching-pair-task.md) |
| 23 | SOC-104 Defaulter cadence | [backend/SOC-104](backend/SOC-104-defaulter-reminder-cadence-day-1-to-30.md) |
| 24 | SOC-105 Financial statements | [backend/SOC-105](backend/SOC-105-financial-statements-trial-balance-p-l-b.md) |
| 25 | SOC-106 Maker-checker payouts | [backend/SOC-106](backend/SOC-106-maker-checker-vendor-payouts-with-tax-tr.md) |
| 26 | SOC-120 Gate app shell + local store | [mobile/SOC-120](mobile/SOC-120-gate-app-shell-with-local-on-device-stor.md) |
| 27 | SOC-121 Offline sync (CRDT) | [mobile/SOC-121](mobile/SOC-121-offline-sync-with-conflict-free-merge-cr.md) |
| 28 | SOC-122 Visitor check-in & approval | [backend/SOC-122](backend/SOC-122-visitor-check-in-and-resident-approval.md) |
| 29 | SOC-123 QR guest passes | [mobile/SOC-123](mobile/SOC-123-pre-approved-visitor-qr-passes.md) |
| 30 | SOC-124 Multilingual UI + ANPR | [mobile/SOC-124](mobile/SOC-124-multilingual-gate-ui-number-plate-readin.md) |
| 31 | SOC-141 30-day auto-delete | [backend/SOC-141](backend/SOC-141-30-day-automatic-deletion-of-visitor-dat.md) |
| 32 | SOC-142 Consent & directory masking | [backend/SOC-142](backend/SOC-142-resident-consent-and-directory-masking.md) |

**End of Phase 1 = pilot-ready.** A good place to pause and run a real pilot before Phase 2.

## Phase 2 — Governance, Copilot, Migration

| Order | Task | Prompt |
|---|---|---|
| 33 | SOC-160 AGM notices | [backend/SOC-160](backend/SOC-160-agm-meeting-notices-with-proof-of-delive.md) |
| 34 | SOC-161 Quorum engine | [backend/SOC-161](backend/SOC-161-live-quorum-engine.md) |
| 35 | **SOC-162 Secret e-voting (PAIR)** | [backend/SOC-162](backend/SOC-162-secret-verifiable-e-voting-pair-task.md) |
| 36 | SOC-163 Capex voting vs ledger | [backend/SOC-163](backend/SOC-163-capex-voting-linked-to-live-fund-balance.md) |
| 37 | SOC-164 Handover pack | [backend/SOC-164](backend/SOC-164-committee-handover-pack.md) |
| 38 | SOC-200 Tally/Excel import | [backend/SOC-200](backend/SOC-200-import-engine-for-tally-excel-exports.md) |
| 39 | SOC-201 MyGate import | [backend/SOC-201](backend/SOC-201-import-from-mygate-exports.md) |
| 40 | SOC-202 Balance verification | [backend/SOC-202](backend/SOC-202-balance-verification-after-import.md) |
| 41 | SOC-203 Parallel-run comparison | [backend/SOC-203](backend/SOC-203-parallel-run-comparison-for-safe-cut-ove.md) |
| 42 | SOC-180 pgvector + knowledge index | [backend/SOC-180](backend/SOC-180-set-up-pgvector-and-the-knowledge-index.md) |
| 43 | SOC-181 NL financial Q&A (RAG) | [backend/SOC-181](backend/SOC-181-natural-language-financial-q-a-rag.md) |
| 44 | SOC-182 Safe NL→data queries | [backend/SOC-182](backend/SOC-182-safe-natural-language-to-data-queries.md) |
| 45 | SOC-183 Auto-minutes + tender compare | [backend/SOC-183](backend/SOC-183-auto-draft-meeting-minutes-tender-compar.md) |

## Reminders

- **Order matters.** Each prompt lists its preconditions; if one isn't built, Claude is told to stop
  and say so.
- **Two end-to-end verification checkpoints:**
  1. *Early check:* **SOC-6 Walking skeleton** (scaffold + compose + DB migrations + basic slice).
  2. *Full-chain proven milestone:* after **SOC-103 / SOC-104** where the complete chain — tenancy
     isolation (SOC-13), transactional outbox (SOC-20/21), payment matching (SOC-103), and double-entry
     ledger posting (SOC-100/104) — is proven end-to-end.
- **Backend first:** All backend services, scheduled pollers, and database migrations are built first
  (Weeks 1–8, ending 1 Jan 2027). Tablet and mobile screens (SOC-120, SOC-121, SOC-123, SOC-124) and
  web console screens start after 1 Jan 2027.
- **The two PAIR tasks** (SOC-103, SOC-162) are the riskiest — have two people review, and don't
  rush them into a sprint's final days.
- **Every task ends with passing tests + human review before merge.** That gate is what makes the
  output trustworthy — see [`../00-playbook.md`](../00-playbook.md).
