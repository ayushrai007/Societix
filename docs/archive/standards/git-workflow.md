# Git Workflow

**Document:** `SOC-DOC-GIT` · **Status:** Standard (prescriptive)

How we branch, commit, review, and release Societix. Prescriptive and short — follow it verbatim.
Tooling and CI gates: [`../delivery/cicd-devops.md`](../delivery/cicd-devops.md). Quality gates:
[`../delivery/definition-of-done.md`](../delivery/definition-of-done.md).

---

## 1. Branching model — trunk-based

- **`main` is the trunk** and is always releasable. Every change lands on `main` via PR.
- Work on **short-lived feature branches** cut from `main`; merge back within **1–2 days**. Long
  branches cause migration/merge pain — split the work instead.
- No long-lived `develop`/`release` branches. Releases are tags on `main` (§7).

### Branch naming

```
<type>/<ticket>-<short-slug>
```

| Example | Meaning |
|---|---|
| `feat/SOC-123-van-provisioning` | new treasury feature |
| `fix/SOC-145-rls-guc-null` | bug fix |
| `chore/SOC-150-bump-spring-3.3.4` | maintenance |
| `docs/SOC-160-api-guidelines` | docs only |

`<type>` matches the Conventional Commit type (§2). `<ticket>` is the issue key.

## 2. Conventional Commits

Every commit message follows **[Conventional Commits](https://www.conventionalcommits.org/)**. CI
lints this (`commitlint`); it feeds the changelog and SemVer bump.

```
<type>(<scope>): <summary>

<body — the why, not the what>

<footer — BREAKING CHANGE:, Refs: SOC-123>
```

| Type | Use | SemVer effect |
|---|---|---|
| `feat` | a new capability | **minor** |
| `fix` | a bug fix | **patch** |
| `docs` | documentation only | none |
| `refactor` | no behavior change | none |
| `perf` | performance | patch |
| `test` | tests only | none |
| `build` / `ci` | build or pipeline | none |
| `chore` | misc maintenance | none |

- **Scope** is the module: `treasury`, `governance`, `copilot`, `gate`, `privacy`, `migration`,
  `platform` (matches the package layout in [`coding-standards.md`](coding-standards.md) §1).
- A breaking change adds a `BREAKING CHANGE:` footer **or** a `!` (`feat(api)!: …`) → **major** bump.

```
feat(treasury): auto-allocate PA webhook payments to invoices

Consumes payment.settled from the outbox and posts a balanced
ledger transaction, flipping the invoice to PAID.

Refs: SOC-123
```

## 3. Pull requests

- **Small PRs.** Target < ~400 changed lines. Split large work behind flags or into stacked PRs.
- **One logical change per PR.** Don't mix a refactor with a feature.
- PR title follows Conventional Commits (it becomes the squash-merge commit).
- The PR description must **link the ticket** and reference the **Definition of Done**
  ([`../delivery/definition-of-done.md`](../delivery/definition-of-done.md)); use the repo PR
  template checklist.
- **Required to merge:**
  - ✅ Green CI — `./gradlew check` (tests + Spotless + Error Prone + SpotBugs + Modulith
    `verify()`), web tests, and the OpenAPI contract test.
  - ✅ **At least one approving review** (two for changes touching `treasury`, `platform` security,
    RLS/tenancy, or DB migrations).
  - ✅ No unresolved review threads.
  - ✅ Branch up to date with `main`.
- **Merge strategy: squash merge.** Keeps `main` history linear and one-commit-per-PR.

## 4. Code review expectations

- Review within **one working day** of request.
- Reviewers check: correctness, module-boundary compliance (no cross-module internals), **no PII in
  logs** (DPDP), tenancy correctness (RLS GUC inside a tx), tests present, problem+json errors,
  idempotency on money ops.
- Comment on the code, not the person. Prefix non-blocking suggestions with `nit:`.
- Authors respond to every thread; a reviewer resolves the threads they opened.

## 5. Protected `main`

Branch protection on `main`:

- No direct pushes; PR only.
- Required status checks must pass (CI, contract test).
- Required review approvals (see §3).
- Linear history (squash merge) enforced; force-push and deletion disabled.
- Administrators are **not** exempt.

## 6. Database migrations in branches

- Migrations are **forward-only, immutable once merged** — never edit a merged `V__` script; add a
  new one (see [`../03-data-model.md`](../03-data-model.md) §6).
- Name versioned scripts `V<version>__<desc>.sql` and **version by timestamp** (e.g.
  `V20261027__…`) to avoid collisions between concurrent branches. Repeatable scripts use `R__`.
- A PR adding a migration needs **two reviews** and must note rollout/rollback in the description.
  Migrations must be **backward-compatible with the currently deployed app** (expand/contract:
  add-column before use, drop-column a release later).
- Every new tenant table must ship its RLS policy in the same migration (fail-closed by default).

## 7. Releases — SemVer & tagging

- Versioning is **[SemVer](https://semver.org/)** `MAJOR.MINOR.PATCH`, derived from Conventional
  Commits since the last tag.
- A release is an annotated tag on `main`: `vMAJOR.MINOR.PATCH` (e.g. `v1.4.0`), which triggers the
  release pipeline.

```bash
git switch main && git pull
git tag -a v1.4.0 -m "release: v1.4.0"
git push origin v1.4.0
```

- Pre-release/RC tags: `v1.4.0-rc.1`. Hotfixes branch from the release tag, fix, tag `v1.4.1`,
  then merge back to `main`.

## 8. Changelog

- `CHANGELOG.md` is **generated** from Conventional Commits at release time (grouped by
  `feat`/`fix`/`perf`, with breaking changes called out). Do not hand-edit it.
- Write commit summaries for a human reader of the changelog — they are user-facing.

---

**Related:** [`getting-started.md`](getting-started.md) · [`coding-standards.md`](coding-standards.md) ·
[`api-guidelines.md`](api-guidelines.md) ·
[`../delivery/cicd-devops.md`](../delivery/cicd-devops.md) ·
[`../delivery/definition-of-done.md`](../delivery/definition-of-done.md)
