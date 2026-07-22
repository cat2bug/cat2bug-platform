# git-repo-webhooks — Manual smoke

Prerequisites: restart backend so Flyway `V1_1_0_11` and new controllers load; frontend `npm run dev`.

1. Open a repo → Settings → **访问控制** → **WebHooks** (after Members).
2. **Create** webhook with a reachable URL (e.g. https://webhook.site), events Push + Tag + Pull Request; leave secret blank to auto-generate.
3. Confirm one-time secret dialog appears; copy secret; close and confirm list shows URL/events/active but **no** secret.
4. Click **Test** → deliveries drawer eventually shows a `ping` attempt (success or failure OK if logged).
5. **Push** a branch commit → delivery `push` with `X-Hub-Signature-256` verifiable with the secret.
6. **Push a tag** → delivery under tag subscription (`create`/`delete`/`push` for tags).
7. **Open / merge / close a PR** → `pull_request` deliveries with correct `action`.
8. **Regenerate secret** → new plaintext once; old secret must fail signature verification.
9. Toggle **inactive** → push should **not** deliver; re-enable and confirm deliveries resume.

Config: `cat2bug.git.webhook.secret-key` (AES for DB cipher; change in production).
