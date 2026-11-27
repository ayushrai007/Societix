# SOC-1 — Repository process

Pull request: [#5](https://github.com/ayushrai007/Societix/pull/5)  
Merge state: **Merged** on 2026-11-08. Current `master` contains commit `3e3ca6a`.

## What it delivers and why

SOC-1 adds repository process controls: a pull-request template, review routing, and documented branch/PR rules. GitHub branch protection was unavailable, so the `master` rules are a team process rule rather than an enforced setting.

## Files and modules touched

- `.github/CODEOWNERS` requests reviews from `@ayushrai007`, `@ssharan100`, and `@Adityarajkishan` for all paths.
- `.github/pull_request_template.md` captures the backlog item, verification evidence, reviewers, Definition of Done checks, and follow-ups.
- `docs/standards/git-workflow.md` links the template and defines work from `master`, one task per PR, review by someone other than the author, and no self-merge.

No application module or runtime dependency changed.

## Local run and verification

There is no application code to run:

```bash
git diff --check
test -f .github/CODEOWNERS
test -f .github/pull_request_template.md
```

## Configuration and environment

None introduced. Branch protection was not configured; CODEOWNERS and the written process are the available controls.

## Key decisions

- Use `master` as the working and PR base branch.
- Keep one global CODEOWNERS rule because ownership is not split by path.
- Put Definition of Done and acceptance-evidence prompts in the template so each PR records how it was checked.

## Known limits and follow-ups

The rules are not technically enforced until branch protection becomes available. The default branch is still `main`, so the template is most reliable once `main` catches up or the repository default changes to `master`.

## Evidence

The task branch was confirmed as `origin/fm/soc-1-repo-process`; its task commit is `3e3ca6a8b2aba53bdf4affcbc86b5aef63bafb1d`. The merge state above was read from the pull request and `master` history.

