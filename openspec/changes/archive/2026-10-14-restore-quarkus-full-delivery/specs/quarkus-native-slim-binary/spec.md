## ADDED Requirements

### Requirement: Native binary includes embedded SPA
After Gate A human acceptance, the Quarkus Native release path MUST produce a single native executable that serves the embedded SPA (same embedded build approach as historical Quarkus delivery), so operators can run without a separate static host.

#### Scenario: Native process serves index
- **WHEN** the Quarkus Native binary is started after an embedded SPA build
- **THEN** `GET /` or the product’s SPA entry returns the embedded frontend shell (HTTP 200)

### Requirement: Release size target around 20MB
The optimized Native release artifact (default measurement: after UPX if the historical pipeline used UPX) SHOULD target approximately **20MB** including embedded frontend assets, matching prior Quarkus delivery experience. Deviations MUST be documented with measured size and reduction steps.

#### Scenario: Measure release artifact
- **WHEN** Gate B packaging completes with the documented compress options
- **THEN** maintainers can read a recorded byte size for the final distributable and compare it to the ~20MB target

### Requirement: Native work is Phase B only
Native slim packaging MUST NOT be declared complete in Gate A. Gate B tasks MAY reuse historical `deploy/build-native-quarkus.sh` and related CI after JVM acceptance.

#### Scenario: Gate B starts after approval
- **WHEN** the product owner confirms JVM review is acceptable
- **THEN** Native build and size tuning tasks are allowed to proceed as Gate B
