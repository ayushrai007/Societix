# Societix — Developer Docs

**What this is:** the simple, plain-English guide for the 3 of us building Societix from scratch.
**New here? Read the docs in order, starting with [01-start-here](01-start-here.md).**

Societix is software that helps Indian housing societies (apartment complexes) run themselves —
collecting maintenance money, running online society meetings & votes, managing the security gate,
and keeping resident data private. Think "the operating system for a gated community."

## Read these in order

| # | Doc | What you'll learn | Time |
|---|-----|-------------------|------|
| 1 | [Start Here](01-start-here.md) | What we're building, who it's for, how to run it on your laptop | 10 min |
| 2 | [How It Works](02-how-it-works.md) | The big picture — how the pieces fit together | 10 min |
| 3 | [Tech Choices](03-tech-choices.md) | Every tool we use, what it is, and why we picked it | 15 min |
| 4 | [The 6 Modules](04-the-6-modules.md) | What each part of the product does | 15 min |
| 5 | [Building It](05-building-it.md) | The plan: who does what, in what order, how we work | 15 min |
| 6 | [Glossary](06-glossary.md) | Plain-English meaning of every term and acronym | reference |

## Want to start working right now?

The task list is in **[jira/](jira/README.md)** — import `jira/societix-backlog.csv` into Jira, or
read **[jira/backlog.md](jira/backlog.md)** to see every task with a full description.

## Want Claude to write the code?

**[codegen/](codegen/README.md)** turns the backlog into a code-generation playbook: shared rules
for how Claude builds, plus one ready-to-paste prompt per task. It builds the codebase incrementally
(task-by-task, tests gating each step) — the realistic, trustworthy path, not a one-shot dump.

## Looking for deep technical detail?

The original, detailed docs (exact schemas, algorithms, decision records) live in
**[archive/](archive/README.md)**. The simple docs link into them where useful. You don't need them
to get started.

## Completed task notes

- [SOC-1 — repository process](tasks/SOC-1.md)
- [SOC-3 — local development environment](tasks/SOC-3.md)
- [SOC-4 — pull-request CI](tasks/SOC-4.md)
- [SOC-5 — Flyway baseline](tasks/SOC-5.md)
