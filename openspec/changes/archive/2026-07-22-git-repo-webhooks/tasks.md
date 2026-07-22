## 1. Database & domain

- [x] 1.1 Add Flyway `V1_1_0_11__git_repo_webhooks.sql` (h2 + mysql via `./scripts/new-flyway-migration.sh`) for webhook and delivery tables
- [x] 1.2 Add domain entities/mappers/services for webhook CRUD and delivery persistence
- [x] 1.3 Implement secret encrypt-at-rest and one-time plaintext only on create/regenerate responses

## 2. Webhook APIs

- [x] 2.1 Implement manage-gated REST: list/create/get/update/delete webhooks under `/system/git/repo/{id}/webhooks`
- [x] 2.2 Implement `POST .../secret/regenerate` and optional `POST .../test` (ping)
- [x] 2.3 Implement `GET .../webhooks/{webhookId}/deliveries` (recent N)

## 3. Delivery engine

- [x] 3.1 Build GitHub-style JSON payload builders for push, tag (create/delete/push-tag), and pull_request
- [x] 3.2 Implement HMAC-SHA256 signing (`X-Hub-Signature-256`) and async HTTP POST with timeouts
- [x] 3.3 Persist delivery logs (status, duration, truncated body/error); failures must not fail Git/PR ops
- [x] 3.4 Optionally add limited retries with backoff

## 4. Event hooks

- [x] 4.1 After successful receive-pack, enqueue push and/or tag deliveries from updated refs
- [x] 4.2 After PR create / synchronize / merge / close, enqueue pull_request deliveries with correct `action`

## 5. Frontend settings UI

- [x] 5.1 Add WebHooks item under access-control settings group + routes (`settings/webhooks`, create/edit/deliveries as needed)
- [x] 5.2 Build list/form UI: URL, alias, events, active; one-time secret reveal modal on create/reset
- [x] 5.3 Build delivery log view; wire API client + manage-gated guards
- [x] 5.4 Add i18n keys for all supported locales

## 6. Verification

- [x] 6.1 Unit/integration tests for signing, secret non-leak on list/get, and event enqueue filters
- [ ] 6.2 Manual smoke: create webhook → push/tag/PR → verify remote receives signed payload and delivery log
