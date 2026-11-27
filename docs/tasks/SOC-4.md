# SOC-4 — Pull-request CI

Pull request: [#6](https://github.com/ayushrai007/Societix/pull/6)  
Merge state: **Open, not merged**. Branch `origin/fm/soc-4-minimal-ci`, head `e0bb81b`.

## What it delivers and why

SOC-4 adds a GitHub Actions workflow for every pull request targeting `master`. It runs backend verification and a full-history secret scan so failures are visible before review and accidental credentials are detected.

## Files and modules touched

- `.github/workflows/ci.yml` adds `Build and test (backend)` and `Secret scan` jobs.
- The build uses Temurin Java 25, the checked-in Maven wrapper, Maven dependency caching, and `./mvnw -B verify` from `backend/`.
- The secret scan uses Gitleaks action v3.0.0 pinned to commit `e0c47f4f8be36e29cdc102c57e68cb5cbf0e8d1e`, with `fetch-depth: 0`.

## Local run and verification

The workflow's build command can be reproduced locally with:

```bash
(cd backend && ./mvnw -B verify)
git diff --check
```

The Gitleaks job runs in GitHub Actions and requires `GITHUB_TOKEN`; it is not a local application command.

## Configuration and environment

The workflow triggers on `pull_request` branches `[master]`, grants read-only repository permissions, and passes `GITHUB_TOKEN`, `GITLEAKS_VERSION=8.30.1`, and `GITLEAKS_ENABLE_COMMENTS=false` to Gitleaks. Actions are pinned to full commit SHAs. No repository secret is introduced.

## Key decisions

- Run `verify`, not only unit tests, so packaging and configured backend checks are covered.
- Scan full git history (`fetch-depth: 0`) to catch secrets in earlier commits visible to the PR.
- Pin third-party actions by SHA and keep the workflow minimal; dependency scanning is deferred to a later task.
- Use concurrency per PR and cancel superseded runs to avoid wasting CI capacity.

## Known limits and follow-ups

The workflow is not merged yet, so `master` does not receive this CI behavior. It does not configure dependency scanning, and the local-development Compose test requires Docker on the runner.

## Evidence

The final branch head includes the restore-secret-scan commit after the intermediate removal commit; the resulting workflow on `origin/fm/soc-4-minimal-ci` contains both jobs described above.

