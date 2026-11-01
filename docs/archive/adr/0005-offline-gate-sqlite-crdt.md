# ADR-0005: Offline-authoritative gate app — SQLite + CRDT

**Status:** Accepted
**Date:** 2026-10-27
**Deciders:** Principal Engineer, Mobile/Edge Lead, Gate Product Owner

## Context

The gate security app runs on guard devices at the society entrance, where connectivity is
unreliable. The BRD is explicit:

- **FR-4.1:** the gate must keep operating during network outages.
- **NFR:** **zero data loss over a 6-hour network blackout**, and **check-in < 1.5 s** — the guard
  cannot wait on a cloud round-trip at the barrier.
- Multiple devices (several gates, guard phones) may record overlapping events (same visitor, same
  vehicle) while all are offline, then reconnect and must converge **without losing or corrupting**
  records.
- The architecture already commits to it: "the gate app is authoritative offline (local SQLite) and
  reconciles via CRDTs on reconnect" ([`../01-architecture.md`](../01-architecture.md) §1, §7).

## Decision

**We will make the gate app offline-authoritative on a local SQLite database (WAL mode), and
reconcile with the cloud using CRDTs on reconnect.**

- Every gate decision (pass validation, visitor check-in/out, vehicle log) is taken **locally
  against SQLite** on the hot path — no cloud call required, satisfying `< 1.5 s` check-in.
- SQLite runs in **WAL (write-ahead log)** mode for crash-durable local writes, so an outage or
  device restart loses nothing (zero-loss over the 6-hour blackout).
- Edits are modeled as **CRDTs** (conflict-free replicated data types) so that when devices
  reconnect, their logs **merge deterministically and commutatively** — concurrent entries from
  multiple gates converge to the same state without a central coordinator and without dropping
  records.
- On reconnect the device syncs its CRDT state to the cloud, which publishes `gate.visit.logged`
  (feeding the M5 30-day privacy purge and notifications —
  [`../01-architecture.md`](../01-architecture.md) §5).
- The full data structures, merge semantics, and sync protocol are specified in the deep dive:
  [`../deep-dives/dd-02-offline-gate-crdt.md`](../deep-dives/dd-02-offline-gate-crdt.md).

## Consequences

**Pros**
- Meets the hard NFRs: sub-1.5 s check-in (local decision) and zero loss across a 6-hour blackout
  (WAL + offline authority).
- Multi-device concurrent edits converge **without conflicts or lost writes** — CRDTs guarantee
  convergence by construction, no manual conflict resolution at the gate.
- The gate keeps working fully offline; the cloud is a sync target, not a dependency.

**Cons / costs**
- CRDTs carry **metadata overhead** (per-edit causal/version metadata) and real implementation
  complexity — the hardest part of the gate module (hence the dedicated deep dive).
- State is **eventually consistent**: the cloud view of gate activity lags until devices reconnect;
  dashboards must tolerate this.
- Designing types so merges are both correct *and* intuitive (e.g. a check-out can't precede its
  check-in) requires care and thorough property-based testing.
- Larger local footprint and sync payloads than a naive queue.

## Alternatives considered

- **Online-only (every decision hits the cloud).** Simplest and always-consistent, but violates both
  `< 1.5 s` check-in and zero-loss-over-blackout — a dead link means a dead gate. **Rejected
  outright** against the BRD.
- **Last-write-wins (LWW) without CRDTs.** Offline-capable and simple, but concurrent edits from two
  gates silently **discard** one side's data — unacceptable for a security log that must lose
  nothing. Rejected.
- **Manual offline queue (store-and-forward, server resolves).** Works for simple append, but server
  conflict resolution for overlapping visitor/vehicle updates is bespoke, fragile, and can still drop
  or duplicate records; no convergence guarantee. Rejected in favor of CRDTs' provable convergence.

---

**Related:** [`../deep-dives/dd-02-offline-gate-crdt.md`](../deep-dives/dd-02-offline-gate-crdt.md) ·
[`../01-architecture.md`](../01-architecture.md) §7 ·
[`../modules/module-4-gate-infra.md`](../modules/module-4-gate-infra.md) ·
[`../glossary.md`](../glossary.md)
