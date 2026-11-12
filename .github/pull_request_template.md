<!-- Title: Conventional Commits, e.g. `feat(treasury): auto-allocate PA payments` -->

## What and why

<!-- One task per PR. What does this change, and why? -->

## Backlog item

SOC-

## How the acceptance criteria were verified

<!-- One line per criterion: the command or check you ran and its result. -->

- [ ] AC 1:
- [ ] AC 2:

## Reviewer(s)

<!-- At least one reviewer who is not the author. Two for treasury, platform security,
     RLS/tenancy, or DB migrations (docs/standards/git-workflow.md §3). -->

## PR checklist

<!-- From docs/archive/delivery/definition-of-done.md §3. Tick what applies; mark the rest N/A. -->

- [ ] Branched from `master`, one task only, no self-merge (docs/standards/git-workflow.md §5)
- [ ] Linked story + FR ID; AC met
- [ ] Follows docs/standards/coding-standards.md, docs/archive/standards/api-guidelines.md, docs/standards/git-workflow.md
- [ ] Unit/slice tests added; patch coverage >= 80% (critical paths >= 95%)
- [ ] Contract tests pass (if interface changed)
- [ ] Tenancy isolation covered (if tenant data touched)
- [ ] Security scans clean (SAST/deps/secret/container); no third-party SDK added
- [ ] DPDP review done (if PII touched); consent/masking/purge respected
- [ ] RLS respected; audit entries added for sensitive actions
- [ ] Observability added (OTel traces/metrics); SLO/alert updated if needed
- [ ] OpenAPI + docs updated; event catalogue updated for new topics
- [ ] Feature-flagged if risky/user-facing
- [ ] Accessibility + i18n checked (UI only)

## Follow-ups

<!-- Related problems noticed but left out of this PR. -->
