# Definition of Ready & Definition of Done

**Document:** `SOC-DOC-DEL-06` · **Status:** Draft for engineering

The shared quality contract for every Societix story. A story must meet the **Definition of Ready
(DoR)** before it's pulled into a sprint, and the **Definition of Done (DoD)** before it ships.
Quality gates below **block merge/release**. Standards referenced here live in
[`../standards/coding-standards.md`](../standards/coding-standards.md),
[`../standards/api-guidelines.md`](../standards/api-guidelines.md), and
[`../standards/git-workflow.md`](../standards/git-workflow.md). Testing bars come from
[`testing-strategy.md`](testing-strategy.md); gates are enforced in
[`cicd-devops.md`](cicd-devops.md).

---

## 1. Definition of Ready (DoR)

A story is ready to start only when:

- [ ] It follows the "As a &lt;persona&gt;, I want &lt;capability&gt;, so that &lt;value&gt;" form and ties to an **FR ID**.
- [ ] It has **testable acceptance criteria** (Given/When/Then).
- [ ] **Design & API agreed** — endpoints follow [`api-guidelines.md`](../standards/api-guidelines.md); any new event/topic is named and documented.
- [ ] **Dependencies known** (e.g. needs M1 ledger, needs IAM role, needs M5 consent gate) and unblocked or sequenced.
- [ ] **Tenancy impact** assessed — does it touch tenant-owned tables? (If yes, RLS is in the design.)
- [ ] **PII/DPDP impact** flagged — does it read/write/retain resident or visitor PII?
- [ ] Sized (S/M/L/XL) and small enough to finish in a sprint (XL is split first).

---

## 2. Definition of Done (DoD)

A story is done only when **all** of the following hold:

### Code
- [ ] Meets [`coding-standards.md`](../standards/coding-standards.md); reviewed and approved.
- [ ] Behind a **feature flag** if user-facing or risky (deploy ≠ release).
- [ ] No new illegal cross-module dependency (Spring Modulith boundary check passes).

### Tests
- [ ] Unit + slice tests added; **patch coverage ≥ 80%** (critical money/vote/purge paths ≥ 95%).
- [ ] **Contract tests** pass for any changed inter-module or PA-webhook interface.
- [ ] If it touches tenant data, a **tenancy isolation test** proves no cross-tenant read.
- [ ] NFR-affecting work has the relevant specialized suite green (perf / offline-CRDT / idempotency).

### Security & compliance
- [ ] **Security scans clean** — SAST, dependency, secret, container (no new high/critical).
- [ ] **No third-party tracking/ad SDK** introduced (compliance gate).
- [ ] **DPDP review** completed for anything touching PII (consent honoured, masking applied, purge/TTL respected); sign-off by Security & Compliance where required (RACI in [`estimation-and-team.md`](estimation-and-team.md)).
- [ ] RLS/tenancy respected; audit log entries added for sensitive actions (append-only).

### Observability
- [ ] Traces/metrics/logs added (OTel); new SLI/alert added if it affects an SLO.

### Docs & interface
- [ ] **OpenAPI** updated for new/changed endpoints; docs/README updated.
- [ ] Event catalogue updated for any new outbox event.

### UI (if applicable)
- [ ] **Accessibility** checked (WCAG AA baseline; keyboard, contrast, screen-reader labels).
- [ ] Multilingual strings externalised (no hardcoded copy — gate/resident apps are multilingual).

---

## 3. PR checklist (paste into every PR)

```markdown
## PR checklist
- [ ] Linked story + FR ID; AC met
- [ ] Follows coding-standards.md, api-guidelines.md, git-workflow.md (../standards/)
- [ ] Unit/slice tests added; patch coverage >= 80% (critical paths >= 95%)
- [ ] Contract tests pass (if interface changed)
- [ ] Tenancy isolation covered (if tenant data touched)
- [ ] Security scans clean (SAST/deps/secret/container); no third-party SDK added
- [ ] DPDP review done (if PII touched); consent/masking/purge respected
- [ ] RLS respected; audit entries added for sensitive actions
- [ ] Observability added (OTel traces/metrics); SLO/alert updated if needed
- [ ] OpenAPI + docs updated; event catalogue updated for new topics
- [ ] Feature-flagged if risky/user-facing
- [ ] Accessibility + i18n checked (UI only)
```

---

## 4. Quality gates (block merge / release)

| Gate | Blocks | Enforced where |
|---|---|---|
| Build + unit/slice tests fail | merge | PR fast lane |
| Modulith boundary violation | merge | PR fast lane |
| Patch coverage < 80% | merge | PR |
| Integration / contract / tenancy test fail | merge | PR integration lane |
| SAST / dependency / secret / container: new high or critical | merge | PR + image build |
| **Third-party tracking/ad SDK present** | merge | compliance scan |
| E2E / performance regression past NFR budget | release | staging / pre-release |
| **DPDP / compliance sign-off missing (PII-touching release)** | release | release approval |
| Non-rollback-safe (non-expand-contract) migration | release | release review |

> Two gates are deliberately **independent of engineering sign-off**: the no-third-party-SDK
> compliance scan and the DPDP/compliance release sign-off. Given Societix's DPDP and RBI exposure,
> neither can be waived by the delivery team alone.
