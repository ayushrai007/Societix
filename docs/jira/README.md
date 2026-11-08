# Jira Backlog — how to use it

**What this is:** the complete task list for building Societix, ready to track in Jira.
**Who should read it:** whoever sets up the Jira project, and anyone picking up a task.

## The three files here

| File | Use it to… |
|---|---|
| **`societix-backlog.csv`** | Import all epics + tasks straight into Jira (see steps below). |
| **`backlog.md`** | Read/plan without Jira — same tasks, same IDs, nicely formatted. |
| this `README.md` | Understand the format and import steps. |

**45 tasks across 11 epics, ~125 story points** (1 point ≈ 1 developer-day). Tasks are small
(1–3 days); a few genuinely harder ones are 5 points and marked **PAIR TASK**.

## Import into Jira (CSV)

1. In Jira: **Project settings** (or the top-right **⚙️**) → **System** → **External System Import** →
   **CSV**. *(On some plans it's Filters/Issues → Import issues from CSV.)*
2. Upload `societix-backlog.csv`.
3. Pick the project to import into, and set **delimiter = comma**.
4. **Map the columns** when prompted:
   - `Issue Type` → Issue Type
   - `Epic Name` → Epic Name (used for the Epic rows)
   - `Epic Link` → Epic Link (connects each task to its epic)
   - `Summary` → Summary
   - `Description` → Description
   - `Acceptance Criteria` → a custom field (create a text field "Acceptance Criteria") **or** append
     it into Description if you prefer.
   - `Story Points` → Story Points (Story point estimate)
   - `Priority` → Priority
   - `Labels` → Labels
   - `Component` → Component/s
   - `Sprint` → Sprint (or import as a label if your board isn't set up yet)
5. Run the import. Jira creates the 11 epics first, then links the 45 tasks to them.

> **Tip:** if a mapping (e.g. Sprint or Acceptance Criteria) gives trouble on your Jira plan, import
> those columns as **Labels** or fold them into **Description** — the task content is what matters.

## What the columns mean

- **Issue Type** — `Epic` (a big theme) or `Story` (a concrete task).
- **Epic Link** — which epic a task belongs to (matches an Epic Name).
- **Story Points** — rough effort; **1 point ≈ 1 day** for one developer.
- **Priority** — `Highest` (do first / blocking) down to `Low`.
- **Labels** — `phase-0/1/2`, module (`m1`…`m6`), and topic (`payments`, `privacy`, `ci`…) for
  filtering.
- **Component** — which part of the system (Platform, Treasury, Gate, …).
- **Sprint** — execution sprint (`W1`–`W8` backend schedule from
  [../05-building-it.md](../05-building-it.md); `Post-1-Jan` for mobile).
- **Assignee** — assigned engineer (Aditya, Shree, Ayush).
- **Status** — current delivery status (`Done` for SOC-1/2, `To Do` for the rest).

## The epics (build order)

Foundations first, then the valuable core (Treasury + Gate + Privacy), then the rest.

| Order | Epic | Phase |
|---|---|---|
| 1 | Foundations: Project Setup | 0 |
| 2 | Foundations: Auth & Tenancy | 0 |
| 3 | Foundations: Outbox & Jobs | 0 |
| 4 | Foundations: Audit & Monitoring | 0 |
| 5 | Foundations: Notifications | 0–1 |
| 6 | M5 Privacy Shield *(starts in phase 0, continues throughout)* | 0–1 |
| 7 | M1 Treasury (Money) | 1 |
| 8 | M4 Gate (Visitors, offline) | 1 |
| 9 | M2 Governance & e-AGM | 2 |
| 10 | M3 AI Copilot | 2 |
| 11 | M6 Smart-Switch Migration | 2 |

## Suggested first sprint (W1) — start here

Pick these up on day one (they unblock everything else):

- **SOC-1** Create the repository and branch rules (Done)
- **SOC-2** Scaffold the Spring Boot backend (Done)
- **SOC-5** Database migrations with Flyway
- **SOC-3** Local dev environment with Docker Compose
- **SOC-4** CI pipeline (build + test on every PR)
- **SOC-10** Define the Society (tenant) entity and onboarding
- **SOC-6** Walking skeleton (end-to-end slice)

Then move into Auth & Tenancy (**SOC-11/12/13**) and the Outbox (**SOC-20/21**) — the privacy-
critical **SOC-13** (data isolation) and **SOC-140** (no-trackers check) should land early.

## Owners (from [../05-building-it.md](../05-building-it.md))

- **Aditya (GitHub: Adityarajkishan, Dev A):** Project setup, Flyway, Docker Compose, CI, Tenancy RLS (SOC-13), Chart of Accounts & Ledger (SOC-100), Financial Statements, Maker-Checker, Migration engine (SOC-200/202/203), and Pair tasks. Starting tasks: SOC-1, SOC-3, SOC-4, SOC-5.
- **Shree (GitHub: ssharan100, Dev B):** Spring Boot scaffold, Walking skeleton, Login/Auth (SOC-11/12), Health/metrics (SOC-41), Tracker check (SOC-140), Billing (SOC-101/102), Defaulter cadence (SOC-104), AGM notices & Quorum (SOC-160/161), Handover (SOC-164), and Pair tasks. Starting tasks: SOC-2, SOC-6.
- **Ayush (GitHub: ayushrai007, Dev C):** Tenant entity (SOC-10), Outbox & background jobs (SOC-20/21/22), Notifications (SOC-30/31), Audit log (SOC-40), Gate visitor backend (SOC-122), Privacy auto-delete & consent (SOC-141/142), pgvector & AI copilot (SOC-180/181/182/183), Capex voting (SOC-163), and MyGate import (SOC-201). Front-end and mobile apps follow post-1 Jan.

The two **PAIR TASKS** (**SOC-103** payment matching, **SOC-162** e-voting) are built by Aditya and Shree
together with dedicated 5-day buffers.
