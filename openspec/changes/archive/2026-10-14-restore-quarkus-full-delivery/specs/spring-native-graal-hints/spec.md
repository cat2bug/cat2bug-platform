## ADDED Requirements

### Requirement: Spring Native is optional after Quarkus default restore
After this change, the **product-default backend delivery** is Quarkus (JVM for Gate A; Quarkus Native for Gate B per `quarkus-native-slim-binary`). Spring Boot Native (`-Pnative` on `cat2bug-platform-admin`) MAY remain available as a comparison or emergency path only. Documentation MUST present Quarkus as the preferred backend; Spring admin / Spring Native MUST be labeled optional rollback, not the recommended default—even before Gate B lands (JVM docs default to Quarkus; after Gate B, preferred binary MUST be Quarkus Native).

#### Scenario: Docs describe preferred delivery path
- **WHEN** a maintainer reads root README / CLAUDE quickstart or release/native instructions
- **THEN** Quarkus is listed as the preferred backend (JVM now; Quarkus Native after Gate B), and Spring Boot admin / Spring Native (if mentioned) are clearly optional/rollback

### Requirement: Existing Spring Native technical requirements stay valid for the Spring path
When Spring Native is still built, existing RuntimeHints / MyBatis / Security Native requirements in this capability MUST continue to apply to that optional path. This change MUST NOT delete Spring Native engineering constraints; it only demotes product priority.

#### Scenario: Spring Native build still follows hints rules
- **WHEN** a maintainer optionally builds Spring Native
- **THEN** existing RuntimeHints and resource registration rules for that path still apply
