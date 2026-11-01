# Codegen — Building Societix with Claude

**What this is:** everything Claude needs to write the Societix codebase for you — correctly,
consistently, and safely enough to ship.
**Who should read it:** the developer driving the build with Claude.

## Read this first — honest expectations

You cannot hand Claude one document and get a finished, push-to-prod codebase in a single reply.
Societix has multi-tenant payments, cryptographic voting, and offline sync — a single giant code
dump would *look* right but be unverified, and unsafe to push to production.

**What actually works — and what this folder gives you — is an incremental, verifiable build:**
Claude builds **one task at a time** from the [backlog](../jira/backlog.md), each task ending with
**tests that pass** and its acceptance criteria met, each reviewed by a human before merge. Do that
45 times, in order, and you have a codebase you can trust.

This folder makes every one of those steps consistent and high-quality.

## How to use it

1. **Read the [playbook](00-playbook.md)** — how Claude must work on this project (the rules that
   keep output consistent and prod-safe).
2. **Skim the [conventions](01-conventions.md) and [tech blueprint](02-tech-blueprint.md)** — the
   fixed stack, layout, and patterns every task plugs into.
3. **Open [prompts/README.md](prompts/README.md)** — the ordered list of ready-to-paste prompts, one
   per backlog task.
4. **Build loop, per task:**
   - Start a Claude session. Paste the playbook + conventions + blueprint as context (or point Claude
     at this folder if it can read the repo).
   - Paste the next task's prompt from `prompts/`.
   - Claude writes the code **and tests**, and shows you how to run them.
   - **You** review the diff, run the tests, and merge via a Pull Request. Never let the build skip
     the human review.
   - Move to the next prompt. Later tasks assume earlier ones exist — **do them in order.**

## What's in here

| File | Purpose |
|---|---|
| [00-playbook.md](00-playbook.md) | The rules of engagement — how Claude generates code here. |
| [01-conventions.md](01-conventions.md) | Hard, checkable coding/stack/security conventions. |
| [02-tech-blueprint.md](02-tech-blueprint.md) | The concrete skeleton (folder trees, build files, core patterns) all code targets. |
| [03-prompt-template.md](03-prompt-template.md) | The fill-in prompt structure + a worked example. |
| [prompts/](prompts/README.md) | One ready-to-paste prompt per backlog task, grouped backend/web/mobile, in build order. |

## What "production-ready" means here (and its limits)

The code Claude produces under this playbook is intended to be clean, tested, and conventionally
correct. **It is not automatically safe to deploy without these human steps**, which this doc does
not replace:

- **Human code review** of every change before merge.
- **Running the full test suite** (and fixing failures) — green locally and in CI.
- A **security review** of anything touching auth, payments, voting, or personal data.
- **Real credentials & secrets** for the payment provider, messaging, etc. — never committed.
- **Load/perf testing** against the stated targets before relying on them.
- **Legal/DPDP sign-off** on data handling.

Claude writes the code; you remain the engineer who ships it.
