## ADDED Requirements

### Requirement: Browse aside includes releases and contributors

The project repository browse UI SHALL place a Releases section and a Contributors section in the right-hand aside below the About/intro block and above Recent activity (when Recent activity is present). Releases and Contributors behavior MUST follow `git-repo-releases` and `git-repo-contributors` respectively.

#### Scenario: Aside order on browse page

- **WHEN** a user with pull permission opens the repository code browse page
- **THEN** the right aside shows About, then Releases, then Contributors (if any), then Recent activity
