# public-release-export Specification

## Purpose
TBD - created by archiving change retire-spring-and-unify-modules. Update Purpose after archive.
## Requirements
### Requirement: Internal materials are tracked privately
The knowledge base `docs/`, OpenSpec artifacts `openspec/` and internal `readme/` subdirectories MUST be trackable on the private mainline (`core`, pushed to `private-gitee`). `docs/` MUST NOT be ignored by `.gitignore`, while personal Obsidian workspace files under `docs/.obsidian/` MAY remain ignored. Agent/editor folders `.cursor/`, `.claude/`, `.github/`, `assets/` and `CLAUDE.md` MUST remain untracked.

#### Scenario: docs can be committed
- **WHEN** a maintainer runs `git check-ignore docs/00-项目总览.md`
- **THEN** the file is reported as not ignored

#### Scenario: Agent folders stay untracked
- **WHEN** a maintainer runs `git ls-files .cursor .claude .github assets CLAUDE.md`
- **THEN** the output is empty

### Requirement: Public export strips internal paths
The repository MUST provide an exclusion list `deploy/public-release-exclude.txt` and a script `deploy/scripts/export-public.sh` that produces a commit on local branch `public/master` whose tree equals the current `core` tree minus every listed path. The script MUST NOT modify the working tree, MUST NOT push, and MUST print the push commands for `github` and `public-gitee`.

#### Scenario: Exported tree excludes internal paths
- **WHEN** a maintainer runs `deploy/scripts/export-public.sh`
- **THEN** `git ls-tree -r public/master` contains none of the paths listed in `deploy/public-release-exclude.txt` and contains the backend, frontend and deploy sources

#### Scenario: Export is incremental
- **WHEN** the script runs a second time after new commits on `core`
- **THEN** the new `public/master` commit has the previous `public/master` commit as its parent

#### Scenario: Self-check fails loudly
- **WHEN** an excluded path would still be present in the exported tree
- **THEN** the script exits non-zero and does not move `public/master`

