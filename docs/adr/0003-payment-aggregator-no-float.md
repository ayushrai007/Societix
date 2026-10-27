# ADR-0003: Licensed Payment Aggregator; Societix holds no float

**Status:** Accepted
**Date:** 2026-10-27
**Deciders:** CTO, Principal Engineer, Finance/Compliance Lead

## Context

Societix collects maintenance dues and pays vendors on behalf of societies. Handling money in India
is heavily regulated:

- Under **RBI Payment Aggregator (PA) norms**, any entity that **collects and pools customer funds**
  and settles them onward is a regulated PA requiring authorization, a prescribed net worth, escrow
  with a scheduled bank, and extensive compliance. Pooling money even briefly ("float") triggers this.
- Societix is a **software** company, not a regulated financial entity, and must not become one.
- The BRD requires **zero-touch reconciliation**: inbound collections auto-allocate to the right
  unit/invoice. This needs per-unit payment identity (a **VAN** and/or **UPI VPA**).
- The architecture already states the principle: **"No float. Money never flows through Societix; the
  Payment Aggregator settles to the society account and notifies us via webhook."**
  ([`../01-architecture.md`](../01-architecture.md) §1, §4; [`../glossary.md`](../glossary.md)).

## Decision

**We will integrate a licensed RBI-authorized Payment Aggregator (Razorpay / Cashfree) for all
money movement, and Societix will never pool, hold, or route customer float.**

- The PA provisions a **Virtual Account Number (VAN)** and/or a **UPI VPA** per unit; residents pay
  into these.
- Funds **settle directly to the society's own bank account** by the PA. Societix is never in the
  flow of funds and holds no escrow/nodal/float balance.
- The PA notifies Societix of credits via **webhooks** (`payment.settled`); the **Webhook Matcher**
  consumes them and the treasury module posts a balanced ledger transaction, flipping the invoice to
  `PAID` (zero-touch) — driven over Kafka with idempotent handling
  ([ADR-0004](0004-kafka-event-driven-cadence.md)).
- Vendor payouts are initiated via the PA's payout API under **maker-checker** control; again the
  money moves PA→vendor, not through Societix.
- Webhooks are signature-verified (HMAC) and deduplicated by PA event id so at-least-once delivery
  never double-posts.

## Consequences

**Pros**
- **Societix stays out of RBI PA licensing scope** — no authorization, net-worth, or escrow burden.
  This is the decisive benefit.
- Settlement reaches the society's own account directly; auditors see a clean money trail that never
  touches a Societix-controlled balance.
- Per-unit VAN/VPA makes reconciliation deterministic and automatic.
- PCI/UPI scope and most fraud/chargeback machinery sit with the licensed PA.

**Cons / costs**
- Hard dependency on the PA's availability, webhook reliability, and API/VAN limits; mitigated by
  idempotent reconciliation, retries, and a provider abstraction (`PA_PROVIDER=razorpay|cashfree`).
- PA fees per transaction and onboarding/KYC lead time per society.
- Reconciliation is **eventually consistent** — it waits on a webhook; an `payment.unmatched` path
  handles edge cases for manual allocation ([`../01-architecture.md`](../01-architecture.md) §5).
- Some control (settlement timing, dispute handling) is delegated to the PA.

## Alternatives considered

- **Become a PA / operate escrow ourselves.** Would give full control of flows but makes Societix a
  regulated entity (RBI authorization, net worth, escrow bank, audits) — out of scope for a SaaS and
  antithetical to the no-float principle. **Rejected.**
- **Nodal/escrow account with a bank directly (no PA).** Still means pooling and holding customer
  funds → PA-like regulatory exposure, plus building collection/payout rails from scratch. Rejected.
- **Direct bank APIs / NEFT-IMPS with manual reconciliation.** No VAN identity, no zero-touch
  matching, high ops burden, and still risks float if funds land in a Societix account. Rejected.

---

**Related:** [`../01-architecture.md`](../01-architecture.md) §4–5 ·
[`../05-security-compliance.md`](../05-security-compliance.md) ·
[`../glossary.md`](../glossary.md) · [ADR-0004](0004-kafka-event-driven-cadence.md) ·
[`../modules/module-1-treasury-erp.md`](../modules/module-1-treasury-erp.md)
