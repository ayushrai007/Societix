# SOC-11 — User login with Firebase Phone Auth

> Paste this into Claude **after** loading `docs/codegen/00-playbook.md`,
> `01-conventions.md`, and `02-tech-blueprint.md` as context. Build only this task.

## Context
Backlog item **SOC-11** · component **Platform** · suggested sprint **S1** ·
size **3 point(s)** · priority **Highest**.

## Goal
Firebase Phone Authentication sends and verifies phone OTPs and issues the Firebase ID token. The
Societix backend accepts that token directly as the API bearer token; it does not exchange it for
another token. The backend validates token signature, issuer, audience, and expiry, maps Firebase
`sub` (UID) to the internal Societix `userId`, and authorizes only users approved for one society.
Societix custom claims are `userId`, `tenantId`, `roles`, and `authorizationVersion`. Never attempt
to replace Firebase's reserved `sub` claim.

Users can submit a request to join one society after phone verification. The request remains pending
until an admin of that society approves it and assigns allowed roles. Pending users receive no
privileged Societix roles. The initial `PLATFORM_ADMIN` is provisioned out of band by an authorized
operator; no public endpoint or user-controlled header can create one.

## Preconditions (must already exist)
SOC-2, SOC-10.
If a precondition is missing, **stop and tell me** instead of building around it.

## Deliverables
Produce the code **and tests** for this task, following the per-task loop in the playbook
(restate → plan files → code → tests → how to verify → self-review). Deliverables must satisfy
every acceptance criterion below. Keep strictly to this task's scope.

## Acceptance criteria (all must be demonstrably met)
- a user can log in and receive a JWT
- approved users' Firebase ID tokens carry Societix `userId`, `tenantId`, `roles`, and
  `authorizationVersion` claims
- expired/invalid tokens are rejected
- pending users cannot access protected society APIs
- stale authorization versions are rejected
- society and role claims are never accepted from request headers or request bodies

## Tests to write
- Tests that map 1:1 to the acceptance criteria above (name which test proves which criterion).
- Use Testcontainers (real Postgres/Redis) for anything touching the DB, RLS, or the outbox.
- Include a test proving a caller-supplied `X-Admin-Role` header cannot authorize society creation.




## Out of scope
Anything not required by the acceptance criteria, and anything owned by a later backlog task.
Do not refactor unrelated code.

## Before you finish
Run the self-review checklist from `00-playbook.md` and report the result honestly — including
anything you could not complete or any assumption you made.
