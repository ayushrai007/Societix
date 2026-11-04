# 00 — The Codegen Playbook

**What this is:** the rules Claude must follow when writing code for Societix, so every task produces
consistent, correct, prod-safe output.
**Who should read it:** paste this into Claude at the start of every build session. Humans should
read it too, so you know what to hold Claude to.

---

## Prime directive

> Build **one backlog task at a time**. Finish it — code **and** tests — to the point its acceptance
> criteria are demonstrably met and tests pass, before touching anything else. Do not invent scope.
> If something needed isn't specified, **stop and ask** rather than guess.

## The 10 golden rules

1. **One task at a time.** Work only the task in the current prompt. Don't "while I'm here" into
   other files or future tasks.
2. **Obey the conventions.** [`01-conventions.md`](01-conventions.md) is law — stack versions, layout,
   naming, error model, tenancy, security. If you'd deviate, say why and ask first.
3. **Plug into the blueprint.** Build on the skeleton in [`02-tech-blueprint.md`](02-tech-blueprint.md);
   don't re-invent the folder tree, build files, or core patterns.
4. **Tests ship with code, always.** No task is done without tests that prove its acceptance criteria.
   Code and tests in the same change.
5. **Tenancy is sacred.** Every database table holding society data has a `tenant_id` and a
   Row-Level Security policy; every request runs inside the tenant context. A missing tenant filter
   is a privacy breach — treat it as a build-breaking bug.
6. **No personal data in logs.** Never log phone numbers, emails, visitor photos, payment details, or
   full names. Log IDs, not PII.
7. **Idempotency where money or events are involved.** Payment webhooks, outbox handlers, and
   anything retried must be safe to run twice with the same effect as once.
8. **Fail closed.** On missing auth/tenant/permission, deny and return the standard error — never
   fall back to "show everything."
9. **No secrets in code.** Config via environment variables; never hardcode keys, passwords, or
   tokens, not even in tests (use fakes/sandboxes).
10. **Ask when unsure.** A wrong guess in payments, voting, or privacy is worse than a question. If
    the spec is silent on something material, pause and ask the human.

## The per-task generation loop

For each task prompt, Claude produces its answer in **this order**:

1. **Restate** the task and its acceptance criteria in one short paragraph, and name the backlog ID
   (e.g. SOC-13). Confirms shared understanding.
2. **Plan the change:** list the files to add/modify and, for backend data work, the entities/columns
   and endpoints involved. Flag anything the task depends on that should already exist.
3. **Write the code** following the conventions and blueprint.
4. **Write the tests** that prove each acceptance criterion (name which test covers which criterion).
5. **Show how to verify:** the exact commands to run (`./mvnw test`, how to hit the endpoint,
   expected output).
6. **Self-review** against the checklist below and state the result honestly (including anything not
   done or any assumption made).

## Self-review checklist (Claude runs this before finishing a task)

- [ ] Scope matches the task only — nothing extra, nothing missing.
- [ ] Conventions followed (layout, naming, error model, DTO/entity split).
- [ ] Every society-owned table has `tenant_id` + an RLS policy; queries run in tenant context.
- [ ] No PII in logs or error messages.
- [ ] Idempotent where retries/webhooks/events apply.
- [ ] Secrets come from config, not code.
- [ ] Tests exist, map to acceptance criteria, and pass.
- [ ] Inputs validated; errors use the standard problem+json model.
- [ ] Public behavior documented (endpoint in API list / OpenAPI, if applicable).

## Boundaries of the build

Claude **does:** write backend/web/mobile code, tests, migrations, config samples, and the commands
to run them.

Claude **does not:** merge to main, approve its own PR, deploy, run against production data, create
or commit real secrets, or declare something "done" without passing tests. Those are **human**
actions — see the limits in [`README.md`](README.md).

## When Claude should stop and ask

- The task needs a decision not covered by the prompt, conventions, or blueprint (e.g. a fee rule, a
  rounding policy, a rate limit value).
- A task appears to depend on something not yet built.
- A convention would have to be broken to satisfy the task.
- Anything touching money movement, vote secrecy, or personal-data deletion is ambiguous.

A short, specific question now prevents a wrong implementation that's expensive to unwind later.

## How a human drives it

1. New Claude session → paste `00-playbook.md`, `01-conventions.md`, `02-tech-blueprint.md` as
   context (or point Claude at `docs/codegen/`).
2. Paste the next prompt from `prompts/` (follow the order in `prompts/README.md`).
3. Review the result, run the tests, open a PR, get a teammate's review, merge.
4. Repeat with the next prompt.

Keep sessions focused — one task (or a couple of tightly related tasks) per session keeps Claude
accurate and the diffs reviewable.
