## MODIFIED Requirements

### Requirement: Native work is Phase B only
Native slim packaging MUST NOT be declared complete in Gate A. Gate B tasks use `deploy/build-native.sh` (formerly `deploy/build-native-quarkus.sh`) and `deploy/ci/native.yml` after JVM acceptance.

#### Scenario: Gate B starts after approval
- **WHEN** the product owner confirms JVM review is acceptable
- **THEN** Native build and size tuning tasks are allowed to proceed as Gate B using `deploy/build-native.sh`
