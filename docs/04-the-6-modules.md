# 04 — The 6 Modules

**What this is:** a short, plain explanation of each of the 6 parts of Societix — the problem it
solves and what it does.
**Who should read it:** everyone, to understand the product. When you actually build a module, follow
the link at the end of its section to the detailed spec in the archive.

Each module is a folder in the backend with a clear boundary. Terms are explained on first use; all
of them are also in the [Glossary](06-glossary.md).

---

## M1 — Treasury (the money) {#m1}

**The problem:** the Treasurer spends hours every month creating bills, chasing payments, and
matching money to flats in a spreadsheet.

**What it does:**
- **Creates bills automatically** each month. Different societies charge differently (flat rate, or
  by flat size, or by water usage) — the "billing engine" handles all those formulas.
- **Collects payments with zero manual matching.** Each flat gets its own unique payment
  number/address (a **VAN** / **UPI VPA**). When money arrives there, we instantly know which flat
  paid and mark their bill settled — the Treasurer does nothing.
- **Chases defaulters on autopilot.** If a bill isn't paid, a reminder sequence (the **cadence**)
  runs by itself: a nudge on day 1, an SMS/call on day 10, a late fee on day 15, a formal notice on
  day 30 — and it stops the moment they pay.
- **Keeps proper accounts** using a **double-entry ledger** (the standard accounting method where
  every transaction is recorded as a matching debit and credit, so the books always balance). It can
  produce the standard financial statements automatically.
- **Pays vendors safely** with **maker-checker** approval (one person requests, another approves) and
  tracks the relevant taxes (TDS/GST).

**Why it matters:** this is the single biggest time-saver for the committee and the feature that
makes societies want to pay for Societix. We build it first.

➡️ Deep detail: [`archive/modules/module-1-treasury-erp.md`](archive/modules/module-1-treasury-erp.md)
and the accounting internals in [`archive/deep-dives/dd-01-double-entry-ledger.md`](archive/deep-dives/dd-01-double-entry-ledger.md).

---

## M2 — Governance & e-AGM (meetings and voting) {#m2}

**The problem:** the annual meeting (**AGM**) is a legal event with strict rules — notices must go
out a set number of days ahead, enough people must attend (**quorum**) for votes to count, and vote-
counting by show-of-hands is disputable.

**What it does:**
- Sends the legally-required **meeting notices** and proves they were delivered.
- Tracks attendance live so the committee knows when **quorum** is met and voting can begin
  (including votes delegated to others — **proxies**).
- Runs **secret, verifiable online voting**: your vote is private, but the final count can be proven
  correct and can't be tampered with. (The clever cryptography for this is in the archive — you don't
  need it to understand the feature.)
- Produces a clean **handover pack** when one committee hands over to the next.

**Why it matters:** removes the biggest source of society disputes (meetings and votes). Built in
phase 2, after Treasury.

➡️ Deep detail: [`archive/modules/module-2-governance-agm.md`](archive/modules/module-2-governance-agm.md)
and the voting cryptography in [`archive/deep-dives/dd-03-crypto-evoting.md`](archive/deep-dives/dd-03-crypto-evoting.md).

---

## M3 — AI Copilot (the helpful assistant) {#m3}

**The problem:** committee members aren't accountants or lawyers, but they constantly need answers
from the society's finances and rulebook.

**What it does:**
- Answers **plain-English questions** about the society ("how much did we spend on the generator last
  quarter?", "which flats are over 60 days overdue?") without anyone writing a database query.
- **Drafts meeting minutes** automatically from notes.
- Looks up **society rules / state laws** and answers rule questions.
- Compares **vendor quotes** side by side.

**How it works (briefly):** it uses **RAG** (Retrieval-Augmented Generation) — it first *finds* the
relevant society data, then asks an AI to *write* an answer based only on that data, with citations,
so it doesn't make things up. The searchable data lives right inside our Postgres database via the
**pgvector** plug-in (so no separate AI database to run).

**Why it matters:** a "wow" feature and a real time-saver — but not essential for day one, so it's a
phase-2 build.

➡️ Deep detail: [`archive/modules/module-3-ai-copilot.md`](archive/modules/module-3-ai-copilot.md)
and [`archive/deep-dives/dd-04-ai-copilot-rag.md`](archive/deep-dives/dd-04-ai-copilot-rag.md).

---

## M4 — Gate (visitor & security management) {#m4}

**The problem:** the security guard logs every visitor by hand in a register, and the gate is often
in a basement with no internet.

**What it does:**
- Logs **visitors** and notifies the resident for approval.
- Lets residents **pre-approve guests with a QR code** for fast entry.
- Reads **vehicle number plates** automatically (ANPR) for known vehicles.
- **Works completely offline.** The guard's tablet stores everything locally and syncs to the server
  when internet returns. A check-in must complete in under 1.5 seconds even with no connection.

**How the offline magic works (briefly):** the tablet keeps its own small database and, on
reconnect, merges its changes with the server's automatically — using a technique called a **CRDT**
(a way of combining edits from two places without conflicts). You don't need to understand CRDTs to
build most of the gate app; just know the sync layer handles merging for us.

**Why it matters:** it's the feature residents interact with daily, and "works offline" is a genuine
differentiator. Built in phase 1 alongside Treasury.

➡️ Deep detail: [`archive/modules/module-4-gate-infra.md`](archive/modules/module-4-gate-infra.md)
and the offline sync in [`archive/deep-dives/dd-02-offline-gate-crdt.md`](archive/deep-dives/dd-02-offline-gate-crdt.md).

---

## M5 — Privacy Shield (data protection) {#m5}

**The problem:** competitors monetise resident data with ads and trackers. India's **DPDP Act** (its
GDPR-equivalent data-protection law) makes that legally dangerous, and residents hate it.

**What it does:**
- **Guarantees no ads and no third-party trackers** — and we check this automatically so one can
  never sneak in.
- **Auto-deletes visitor data** (photos, phone numbers) after 30 days, so nothing is hoarded.
- Lets residents **control who sees their details** (e.g. hide their phone number in the directory)
  without losing access.

**Why it matters:** it's our brand promise *and* our legal shield. It's **not** a "phase" — it's
built in from day one and checked in every piece of work we ship.

➡️ Deep detail: [`archive/modules/module-5-privacy-shield.md`](archive/modules/module-5-privacy-shield.md)
and [`archive/05-security-compliance.md`](archive/05-security-compliance.md).

---

## M6 — Smart-Switch Migration (moving in) {#m6}

**The problem:** a society won't switch to us if it means abandoning years of accounts and re-typing
every resident. Fear of migration is the #1 reason they don't switch.

**What it does:**
- **Imports their old data** from the tools they use today — Tally (accounting software), Excel, or a
  competitor like MyGate.
- **Checks the imported balances** against the originals so the Treasurer trusts the opening numbers.
- Supports a **parallel run** — running old and new side-by-side for a while — so switching over is
  risk-free.

**Why it matters:** it's what unblocks sales. Built in phase 2, once Treasury (which it feeds data
into) is solid.

➡️ Deep detail: [`archive/modules/module-6-smart-switch.md`](archive/modules/module-6-smart-switch.md).

---

## How they fit together

- **M1 Treasury** is the backbone — M2 (voting on fund spends) and M6 (importing old accounts) both
  build on its ledger.
- **M4 Gate** produces visitor data that **M5 Privacy** then auto-deletes after 30 days.
- **M5 Privacy** wraps around everything, always on.
- **M3 Copilot** reads from M1 and M2 once they exist.

Build order and ownership are in [05 — Building It](05-building-it.md).
