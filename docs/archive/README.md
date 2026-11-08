# Archive — detailed reference docs

These are the **original, detailed engineering docs** for Societix. They are thorough but dense —
written as deep technical reference.

**If you're getting started, don't read these first.** Start with the simplified docs one level up
(`../README.md` → `../01-start-here.md`). Come here only when you're actually building a specific
piece and want the deep detail (exact schemas, algorithms, edge cases).

| Folder / file | What's in it |
|---|---|
| `00`–`06-*.md` | Product overview, architecture, system design, data model, API spec, security, frontend |
| `modules/` | Full specs for the 6 modules |
| `deep-dives/` | The hard internals: ledger, offline gate sync, e-voting, AI copilot |
| `delivery/` | Original delivery/planning docs (phases, estimation, testing, CI/CD) |
| `standards/` | Coding standards, API rules, git workflow, getting-started |
| `adr/` | Architecture Decision Records — *why* each big choice was made |

The simplified docs link into these files wherever you'd want the full detail.

## Historical ID Schemes

These archived documents retain their original identifier schemes:
- **Document numbers:** `SOC-DOC-*` (e.g., `SOC-DOC-00`, `SOC-DOC-M1`–`M6`, `SOC-DOC-DEL-01`–`07`, `SOC-DOC-ADR`, `SOC-DOC-CS`, `SOC-DOC-GS`, `SOC-DOC-GIT`, `SOC-DOC-API`)
- **Functional requirements:** `FR-x.y` (e.g., `FR-1.1`, `FR-4.1`)
- **Legacy story codes:** `F1.x` / `M1.x` (e.g., `F1.1`, `M1.3`)
- **Legacy milestones:** `MS0`, `M0`, `M1A`, `M1B`, `M2A`, etc.

**Canonical IDs:** All active engineering and project management work uses the unified **`SOC-n`** issue identifier scheme defined in the Jira backlog ([`../jira/societix-backlog.csv`](../jira/societix-backlog.csv)) and time plan ([`../05-building-it.md`](../05-building-it.md)).

