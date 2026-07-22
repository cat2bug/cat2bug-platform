## ADDED Requirements

### Requirement: Browse UI uses namespaced code slug paths

The repository browse UI (code tree, commits, branches, pull requests, releases, and commit detail entry points under browse) SHALL use canonical browser paths under `/{teamKey}/{projectKey}/code/{slug}/**`, where `{slug}` is the repository slug. Legacy `/project/git-repo/browse/{repositoryId}/**` paths MUST redirect to the slug-based namespaced paths when resolvable. REST browse APIs MAY continue to identify repositories by id.

#### Scenario: Navigate to code browse from repository list

- **WHEN** a user with pull permission opens browse for a repository that has team/project path keys and a slug
- **THEN** the browser location is under `/{teamKey}/{projectKey}/code/{slug}`

#### Scenario: Commit detail under code slug prefix

- **WHEN** a user opens commit detail from the commits tab (or other required browse entry points)
- **THEN** the commit detail view is reached under `/{teamKey}/{projectKey}/code/{slug}/commits/{commitId}` or another documented child path under that code prefix
