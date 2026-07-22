# retire-quarkus-delivery Specification

## Purpose

Retire Quarkus-based delivery paths in favor of the Spring Boot / Spring Native packaging and documentation baseline for Cat2Bug Platform.

## Requirements
### Requirement: Spring Boot admin is the default backend delivery
The project documentation and default developer/release instructions SHALL present `cat2bug-platform-admin` (Spring Boot) as the primary runnable backend. Quarkus SHALL NOT be described as the default or preferred delivery for Cat2Bug Platform on the mainline documented in this repository.

#### Scenario: Read top-level contributor docs
- **WHEN** a contributor reads `CLAUDE.md` / root `README.md` backend quickstart
- **THEN** the documented default run/build path is Spring Boot admin (and any retained Spring Native path), not Quarkus

### Requirement: Quarkus delivery artifacts are retired
Build scripts, deploy helpers, and frontend package scripts that exist solely to produce or run the Quarkus embedded delivery SHALL be removed or rewritten so they no longer advertise or invoke Quarkus packaging. Optional historical mentions inside `openspec/changes/archive/**` MAY remain.

#### Scenario: Search active delivery scripts
- **WHEN** a maintainer searches `deploy/`, root scripts, and `cat2bug-platform-ui/package.json` for active Quarkus delivery entrypoints
- **THEN** no active script remains that builds or runs `cat2bug-platform-quarkus-admin` as a supported product path

### Requirement: No Quarkus module as supported product surface
The Maven reactor used for product builds SHALL NOT require Quarkus modules. If Quarkus module directories are absent, the change SHALL ensure docs and CI do not reference them as missing required modules. If any Quarkus module directories remain untracked or orphaned, they SHALL be deleted from the working tree as part of retirement.

#### Scenario: Maven default build
- **WHEN** a developer runs the default Maven install for the platform backend without a Quarkus profile
- **THEN** the build does not depend on Quarkus modules to succeed

### Requirement: Preset report resources are not duplicated for Quarkus
Classpath report presets introduced by `report-template-preset-groups` SHALL be packaged for Spring admin only and MUST NOT be mirrored into a Quarkus module resources tree as part of this capability.

#### Scenario: Preset path exists only on Spring admin
- **WHEN** presets are added under admin `src/main/resources/报告/模版/`
- **THEN** the change does not add a parallel Quarkus resources copy for those presets

