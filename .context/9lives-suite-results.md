# New local Demo Bank suites — 2026-11-04

Requirements: REQUIREMENTS.md US-1, US-2, US-3, US-4, US-7, base revision
`2e44f79798cf08fb7020c832cd79181381520521` plus the uncommitted test/config changes.
Target: an isolated local Node server at `http://127.0.0.1:3100`, clean mode.
Runner: local 9lives 0.6.0-beta.1, Playwright 1.61.1, headless Chromium.
Both specs used `--attempts 1`, zero Playwright retries, and creation snapshots.

| Spec | Final run ID | Tests | Passed | Failed | Skipped |
| --- | --- | --- | --- | --- | --- |
| auth-session.spec.js | run-20261104T132039Z-703aa20752dd | 6 | 5 | 1 | 0 |
| transfer-history.spec.js | run-20261104T132023Z-2d1774a82000 | 8 | 8 | 0 | 0 |

Final spec digests:

- Auth: `2f5e64ba17abd5a7753d23c51d19f5fa8852e571a098343ff80a3b3014e0cd2b`
- Transfer/history: `b9361a891463b71a437601d6d01d9c605cd1821b395d24c734b09983e6844920`

Both final receipts report executed/validated evidence with matching branch,
commit, repository, and spec source provenance. Assertion counts remain unknown.
Transfer/history reports `passed`, `complete: true`. Auth reports `failed`,
`complete: false`, with six executed tests and one assertion failure; it must
not be described as a complete passing suite.

The first auth candidate had two failures, including an incorrect login-error
locator authored during this session. That locator was corrected from #alert
to the actual #error element; the text assertion was retained and a visibility
assertion added. The corrected spec was immediately re-snapshotted and rerun.
The original receipt remains untouched at run-20261104T131941Z-2e8bb8f15998.

Remaining product regression: US-7 requires Back after logout not to show the
dashboard. The test confirms logout returns to login, removes the session
cookie, and rejects reuse of the old token (401). After `page.goBack()`, the URL
remains /dashboard and the browser snapshot shows dashboard navigation and user
name. The assertion expecting /login fails. Cached page restoration is the
likely cause; browser cache lifecycle was not independently instrumented.
The later direct-navigation check in that same test is not reached. No assertion
was weakened, skipped, caught, or marked expected-to-fail. Application code was
not modified.

Receipt paths:

- `.9lives/receipts/run-20261104T132039Z-703aa20752dd/job-001/job-001-attempt-001/receipt.json`
- `.9lives/receipts/run-20261104T132023Z-2d1774a82000/job-001/job-001-attempt-001/receipt.json`

9lives static assessment was partial: parameterized cases and helper assertions
have analysis limits. An advisory positional locator was replaced before the
transfer run. Static assessment is not proof of runtime success.

Existing `npm test` passed, and `git diff --check` passed. These are new local
specs, not imported managed QualityMax scripts. No cloud run was performed for
these new specs; prior managed-script executions do not verify them. Setup,
run commands, and remaining requirement gaps are in tests/e2e/README.md.
