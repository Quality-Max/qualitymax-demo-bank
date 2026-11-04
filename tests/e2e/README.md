# Local browser suites with 9lives

These suites use Playwright and the local 9lives runner. They are separate from
the existing scripts managed in QualityMax project 149.

## Install and run

```bash
npm install --no-package-lock
npx playwright install chromium
npm run test:e2e
```

Playwright starts a fresh local server on port 3100 in `DEMO_MODE=clean` and
stops it after the run. It refuses to reuse an existing server. One worker and
zero retries keep the synthetic in-memory banking fixture deterministic. Each
test has a fresh browser context; the app's ledger is shared within a run.
Tests capture current balances/history before checking changes, and the next
run starts with a fresh ledger. Do not point these mutation tests at real data.

Run each spec with its creation snapshot to collect 9lives receipts:

```bash
mkdir -p .9lives
/home/user/.qmax-9l/bin/9l provenance tests/e2e/auth-session.spec.js --agent codex > .9lives/auth-session.provenance.json
/home/user/.qmax-9l/bin/9l provenance tests/e2e/transfer-history.spec.js --agent codex > .9lives/transfer-history.provenance.json
/home/user/.qmax-9l/bin/9l run tests/e2e/auth-session.spec.js --agent-provenance .9lives/auth-session.provenance.json --attempts 1 --format json
/home/user/.qmax-9l/bin/9l run tests/e2e/transfer-history.spec.js --agent-provenance .9lives/transfer-history.provenance.json --attempts 1 --format json
```

Run these commands sequentially because both start the server on port 3100.
On another machine, replace the absolute binary path with the installed `9l`.
Receipts stay in `.9lives/receipts/`; do not move, edit, or delete them.

Before deliberately revising a spec, identify the requirement and current
revision. Immediately after editing, refresh that spec's creation snapshot:

```bash
/home/user/.qmax-9l/bin/9l provenance tests/e2e/auth-session.spec.js --agent codex > .9lives/auth-session.provenance.json
/home/user/.qmax-9l/bin/9l provenance tests/e2e/transfer-history.spec.js --agent codex > .9lives/transfer-history.provenance.json
```

Snapshots identify the creation branch, commit, and exact spec digest. After a
new checkout or commit, create a snapshot for the revision being tested before
running it. A receipt describes the local execution; it is not cloud evidence.

## Coverage

Source: `REQUIREMENTS.md` at commit
`2e44f79798cf08fb7020c832cd79181381520521`.

| Spec | Tests | Covered behavior |
| --- | --- | --- |
| `auth-session.spec.js` | 6 | US-1: demo/admin/jane login, invalid login, protected-page redirects; US-2: account and total balances; US-7: cookie removal, old-token invalidation, back navigation and direct access after logout |
| `transfer-history.spec.js` | 8 | US-3: positive transfer, both account balances and ledger entries, insufficient funds, negative/zero amounts and same-account rejection; US-4: history entry, description/category search, empty results |

Transfer rejection tests compare both account balances and both transaction
histories before and after the request. Negative, zero, and same-account cases
call the API through the authenticated browser context so HTML validation
cannot hide missing server validation. The positive transfer, insufficient
funds, and search journeys interact with the real browser UI.

This is an initial regression subset. It does not verify every acceptance
criterion. Remaining coverage includes transfer confirmation and the $10,000
cap, inactivity expiry, account lockout, profile editing, admin authorization,
search injection, performance, accessibility, and dashboard transaction order.
The demo intentionally contains defects; do not weaken assertions or add skips
to conceal failures. No production behavior was changed to support these tests.

9lives static assessment is advisory and partially understands parameterized
tests and imported helpers. Runtime counts and receipts establish execution.
If these specs are later imported into QualityMax, verify the exact managed
revision in its cloud runner as well.
