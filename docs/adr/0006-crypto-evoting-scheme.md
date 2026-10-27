# ADR-0006: Commit-reveal + hash-chained e-voting (not blockchain)

**Status:** Accepted
**Date:** 2026-10-27
**Deciders:** Principal Engineer, Governance Product Owner, Security/Crypto Lead

## Context

Statutory AGM/EGM voting (M2 Governance) must be **secret, verifiable, and tamper-evident** and
hold up legally under the State Cooperative Societies Acts. The BRD (**FR-2.3**) calls for
**"cryptographic secret balloting / an immutable audit trail."** Constraints and forces:

- **Legal bindingness:** outcomes decide fund hikes, account approvals, committee elections — they
  must be defensible to an auditor and a registrar, with a trail that proves no ballot was altered,
  added, or dropped.
- **Ballot secrecy:** how a given member voted must not be derivable by the committee or Societix.
- **Verifiability:** a member should be able to confirm their vote was counted, and anyone should be
  able to verify the tally matches the recorded ballots.
- **Coercion-resistance pragmatism:** full academic coercion-resistance (receipt-freeness via
  mixnets/homomorphic tallying) is disproportionate for a residential society AGM and operationally
  unrealistic for volunteer committees.
- The data model already anticipates it: anonymized `ballot_hash` on `VOTE`, the `agm.vote.cast`
  event, and the append-only hash-chained audit log
  ([`../03-data-model.md`](../03-data-model.md) §1, §5; [`../01-architecture.md`](../01-architecture.md) §5).

## Decision

**We will implement a practical commit–reveal scheme with encrypted ballots, recorded in an
append-only, hash-chained audit log — explicitly NOT a blockchain.**

1. **Commit phase:** during the voting window, each eligible voter submits an **encrypted ballot**
   plus a **commitment** (a hash of the ballot + a per-voter nonce). The commitment binds the vote
   without revealing it; the voter's identity is separated from ballot content (only an anonymized
   `ballot_hash` links to the tally).
2. **Reveal/tally phase:** after the window closes, ballots are decrypted/opened and tallied; each
   opening is checked against its commitment, so a ballot cannot be swapped after commit.
3. **Tamper-evidence:** every vote event and tally step is written to the **append-only,
   hash-chained audit log** (`entry_hash = SHA-256(prev_hash ‖ payload)` per tenant;
   [`../03-data-model.md`](../03-data-model.md) §5). Any insertion, deletion, or edit breaks the
   chain and is detectable on verification.
4. **Verifiability:** a voter can confirm their commitment appears in the chain; an auditor can
   recompute the chain and re-run the tally from the recorded ballots.
- Full protocol (key management, nonce handling, proxy votes, quorum, edge cases) is specified in the
  deep dive: [`../deep-dives/dd-03-crypto-evoting.md`](../deep-dives/dd-03-crypto-evoting.md).

## Consequences

**Pros**
- Delivers **secrecy** (encrypted ballots, identity/content separation), **integrity** (commitments
  prevent post-hoc swaps), and **tamper-evidence** (hash chain) — meeting FR-2.3 with standard,
  auditable cryptography.
- Reuses the existing append-only audit infrastructure — no new trust system to operate.
- Verifiable by voters and auditors; defensible to a registrar.
- Pragmatic and operable by volunteer committees; no external infrastructure or tokens.

**Cons / costs**
- Commit–reveal needs **both phases to complete**; abandoned/unrevealed ballots need a defined policy
  (handled in the deep dive).
- **Not receipt-free**: a determined voter could prove how they voted, so it is not fully
  coercion-resistant — an accepted trade-off for the residential-AGM context.
- Secrecy depends on correct **key management** (who can decrypt, and when); a compromised decryption
  key before reveal would break secrecy. Mitigated by envelope keys and separation of duties.
- Correctness hinges on careful implementation and property tests — hence the dedicated deep dive.

## Alternatives considered

- **Plaintext votes in the DB.** Trivial to build and tally, but gives **no secrecy** (DBAs/committee
  can read votes) and weak tamper-evidence. Fails FR-2.3. Rejected.
- **Full homomorphic / mixnet e-voting (academic-grade).** Strongest secrecy and receipt-freeness,
  but heavy cryptography, far harder to implement correctly, audit, and explain, and overkill for a
  society AGM. Rejected as disproportionate.
- **Public blockchain for the ballot/audit trail.** Marketed as "immutable," but adds external
  dependency, cost, latency, data-sovereignty/DPDP concerns (public ledger), and key-management
  complexity, while our per-tenant hash-chained log already provides tamper-evidence **privately and
  on sovereign infra**. Rejected — the hash chain gives the integrity property without the blockchain.

---

**Related:** [`../deep-dives/dd-03-crypto-evoting.md`](../deep-dives/dd-03-crypto-evoting.md) ·
[`../03-data-model.md`](../03-data-model.md) §5 ·
[`../05-security-compliance.md`](../05-security-compliance.md) ·
[`../modules/module-2-governance-agm.md`](../modules/module-2-governance-agm.md) ·
[`../glossary.md`](../glossary.md)
