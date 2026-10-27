# 05 — Security & Compliance

**Document:** `SOC-DOC-05`

Regulatory and security posture: the three regulatory regimes (BRD §12), the crypto/security
standards (BRD §11), a STRIDE threat model, secrets management, and data classification with
retention. Mechanisms referenced here are implemented per the [data model](03-data-model.md)
(RLS, envelope encryption, hash-chained audit) and [system design](02-system-design.md)
(authN/authZ, audit distinction). Terms: [glossary](glossary.md).

---

## 1. Regulatory regimes (BRD §12)

### 1.1 DPDP Act 2023 — Digital Personal Data Protection

| Aspect | Position |
|---|---|
| **Roles** | The **society = Data Fiduciary** (determines purpose); **Societix = Data Processor** (processes on documented instruction) |
| **Consent** | Explicit, purpose-bound, revocable consent captured as `ConsentRecord` (M5); no bundled/implied consent |
| **Granular isolation** | Per-society data separation via PostgreSQL **RLS** + per-society encryption keys (see [data model](03-data-model.md) §3–4) |
| **No commercial targeting** | Resident PII is never used for advertising/profiling; no third-party tracking SDKs anywhere (see [architecture](01-architecture.md) §1) |
| **30-day auto-purge** | Visitor photos/phone numbers auto-deleted 30 days after the visit via M5 (`purge_at`, `privacy.purge.due` topic) — ties to [module](modules/module-5-privacy-shield.md) |
| **Data-subject rights** | Access, correction, erasure, and withdrawal of consent via `POST /api/v1/privacy/data-requests` ([API spec](04-api-specification.md) M5) |
| **Breach handling** | Notify the society (Fiduciary) and the Data Protection Board as required; audit trail supports forensics |

### 1.2 RBI Payment Aggregator norms

| Aspect | Position |
|---|---|
| **Role** | Societix is a **technology layer only** — it is not the PA and is not licensed to handle funds |
| **No float** | Societix **never pools or holds customer money**; funds are collected by the RBI-licensed PA (Razorpay/Cashfree) |
| **Settlement** | PA settles **directly to the society's bank account** (VAN/UPI VPA); we are notified by webhook |
| **Design basis** | See [ADR-0003](adr/0003-payment-aggregator-no-float.md) and the settlement webhook flow in [API spec](04-api-specification.md) §3 |

### 1.3 State Cooperative Societies Acts

These statutes drive **configuration** in M1 (Treasury) and M2 (Governance), keyed by the
society's `state_code` (`bylaw_config` in [data model](03-data-model.md) §1).

| Statute | Drives | Config effect |
|---|---|---|
| **Maharashtra MCS Act** (Sec 154B) | M1 | Interest-on-arrears caps; non-occupancy charges **≤ 10%** of service charges |
| **Karnataka KAOA 1972** | M2 | Voting weightages for resolutions/quorum in e-AGM |
| **Haryana HRERA** | M1/M2 | Sinking-fund handover audits; builder→RWA transition controls |

Caps/weightages are data-driven (bylaw config), not hard-coded, so new states are onboarded by
configuration. See [modules/module-1-treasury-erp.md](modules/module-1-treasury-erp.md) and
[modules/module-2-governance-agm.md](modules/module-2-governance-agm.md).

## 2. Crypto & security standards (BRD §11)

| Control | Implementation |
|---|---|
| **Transport** | **TLS 1.3** everywhere; **HSTS** with preload; modern ciphers only; mTLS for internal service/webhook calls where applicable |
| **At rest** | **AES-256** disk/volume encryption; column-level **envelope encryption** for PII & bank identifiers (per-society DEK wrapped by KMS master key — [data model](03-data-model.md) §4) |
| **Key rotation** | DEKs rotated on schedule and on suspected compromise; master keys in KMS; GCM key-id tagging enables rewrap without bulk re-encrypt |
| **Data residency** | **Sovereign Indian data centres** — AWS/GCP **Mumbai (active)** & **Hyderabad (passive)** only (see [architecture](01-architecture.md) §9) |
| **Audit trail** | **Immutable, write-once, hash-chained** `audit_log_entry`; no UPDATE/DELETE grant; **read-only access for statutory auditors** ([data model](03-data-model.md) §5, [system design](02-system-design.md) §8) |
| **AuthN/AuthZ** | OIDC/JWT, RBAC, RLS backstop ([system design](02-system-design.md) §1) |

## 3. Threat model (STRIDE)

| STRIDE | Threat | Mitigation |
|---|---|---|
| **Spoofing** | Forged identity / stolen token | OIDC + PKCE, short-lived JWT, JWKS sig validation, refresh rotation |
| **Spoofing** | Forged PA webhook | HMAC-SHA256 signature verify, constant-time compare, per-society secret ([API spec](04-api-specification.md) §3) |
| **Tampering** | Alter ledger / votes / audit | Append-only + hash-chained audit; balanced-entry ledger; no UPDATE/DELETE grant |
| **Tampering** | Cross-tenant data write | RLS `WITH CHECK` + forced RLS; app role lacks BYPASSRLS ([data model](03-data-model.md) §3) |
| **Repudiation** | "I didn't approve that payout" | Maker-checker + `audit.action.recorded` with actor, trace id, chain hash |
| **Information disclosure** | PII leak across tenants | RLS fail-closed on missing GUC; envelope-encrypted PII columns; PII redacted from logs |
| **Information disclosure** | Directory scraping | Masked directory by default; unmasked only for Secretary/Treasurer ([API spec](04-api-specification.md) M5) |
| **Denial of service** | Noisy neighbor / flood | Per-tenant + per-user Redis rate limits at gateway; HPA; circuit breakers |
| **Elevation of privilege** | Role escalation | Layered enforcement (gateway + `@PreAuthorize` + RLS); roles signed in JWT, re-signed by gateway |

## 4. Secrets management

- **Kubernetes Secrets** for runtime config, backed by an external **vault** (e.g. HashiCorp Vault /
  cloud KMS secrets manager); secrets are encrypted at rest and mounted, never baked into images.
- **KMS** holds master keys that wrap per-society DEKs; application code requests unwrap, never raw master key.
- **Rotation:** PA webhook secrets, DB credentials, and signing keys rotate on schedule; CI has no
  standing production secrets (short-lived OIDC-federated deploy credentials).
- **No secrets in code/logs:** enforced by pre-commit scanning and log redaction.

## 5. Data classification & retention

| Class | Examples | Controls | Retention |
|---|---|---|---|
| **Public** | Society name, published notices | Integrity only | Indefinite / per notice policy |
| **Internal** | Ledger config, bylaw config, non-PII operational data | RLS, RBAC | Life of society + statutory accounting period |
| **PII** | Resident name/phone/email, **visitor photo/phone** | RLS + envelope encryption, masking, consent | General PII per consent/purpose; **visitor PII = 30 days** (M5 auto-purge) |
| **Financial** | Invoices, payments, ledger entries, payouts, bank identifiers | RLS + envelope encryption (bank IDs), maker-checker, immutable audit | Per statutory accounting/audit requirements (multi-year); financial records are not purged with visitor PII |

Operational/observability logs are short-retention and PII-redacted; the immutable audit trail is
the long-lived compliance record (see [system design](02-system-design.md) §8).

---

**Related:** [architecture](01-architecture.md) · [system design](02-system-design.md) ·
[data model](03-data-model.md) · [API spec](04-api-specification.md) ·
[ADR-0003 no-float](adr/0003-payment-aggregator-no-float.md) · [glossary](glossary.md)
