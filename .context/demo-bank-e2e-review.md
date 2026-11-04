# Demo Bank E2E review — 2026-11-04

Project: Qualitymax Demo Bank, QualityMax ID 149 (teal-lemon).
Repository revision reviewed: `2e44f79798cf08fb7020c832cd79181381520521`.
Target requested: `https://qualitymax-demo-bank.vercel.app`.

## Scope and verification

Reviewed current source for all 32 managed scripts (31 Playwright, one pytest).
The project lists 61 test cases; eight existing Playwright scripts were sampled
for one cloud execution each. No tests, requirements, application code, or
project settings were changed. No healing was performed.

Local `npm test` passed: three sequential mode checks covering clean negative
transfer rejection, the deliberately buggy negative transfer behavior, and the
changed navigation selector. These exercise HTTP APIs and HTML strings; they
do not execute browser journeys. The repository has no web Playwright suite or
web E2E CI workflow.

Cloud structured counts: 8 tests, 4 passed, 4 failed, 0 skipped, 0 errors;
0 pending. The four passes have terminal runner proof. The four failures have
reported failures and structured counts, but their normalized receipt verdict
is `unknown`, with `execution.evidence_unavailable` and missing terminal proof.
They must not be described as fully verified failure receipts.

## Findings

1. **High: transfer coverage can pass without testing transfers.** Script 6292
   only logs in despite its transfer-validation description. Scripts 6365,
   6312, 6311, and 6289 are partial traces with zero assertions. Script 6289
   passed this sample without attempting a transfer or checking rejection.
   Script 6252 explicitly leaves the unchanged-balance assertion commented out.
2. **High: incorrect transfer locators block the business checks.** Script 6352
   fails because `data-test="login-submit"` does not exist; the repository uses
   `login-button` (`views/login.html:24`). Both 6352 and 6349 expect a recipient
   text input. The application uses `from-account` and `to-account` select
   controls (`views/transfer.html:28`, `:32`) and `transfer-button` (`:43`).
   Script 6349 also omits its test case's unchanged-balance assertion.
3. **High: authentication passes do not establish access control.** Script 1876,
   linked to case 943 (unauthenticated users cannot access protected pages),
   opens the public login page and clicks its heading. It never visits a
   protected page or asserts a redirect. Script 3141 submits invalid credentials
   without checking the response, error message, or denied access.
4. **Medium: login assertions and requirement coverage are inconsistent.**
   Scripts 6270 and 6246 synchronously inspect `page.url()` after submission and
   also expect Sign In to remain visible after a successful dashboard redirect.
   The sampled original 6270 failed at the immediate URL check. Its existing
   healed copy 6271 waits for navigation and passes, but removes the Sign In
   assertion. This review did not create or endorse that historical change.
   Both cover only demo login although case 933 calls for demo, admin, and jane.
5. **Medium: duplicate and shallow coverage obscures suite quality.** Scripts
   6246/6270 have identical digests; so do 6247/6271. Scripts 6245, 6284, 6285,
   and 6288 stop after filling login fields. The reviewed Playwright source
   lacks meaningful checks for a successful transfer with balance/history
   updates, transaction search results, profile persistence, and logout/session
   invalidation. These are gaps against REQUIREMENTS.md US-3 through US-7.
6. **Unresolved: cloud navigation failures.** Script 1875 timed out clicking a
   visible Transactions link, and 6349 timed out waiting for dashboard load
   after login. The link resolved correctly in 1875's log. Neither result
   establishes an application defect or justifies changing assertions.

## Sampled source revisions and cloud executions

Versions below were read before execution. They identify the reviewed managed
sources, not a cloud-attested source revision.

| Script | Case | Reviewed version | Reported outcome | Execution ID |
| --- | --- | --- | --- | --- |
| 1875 | 924 | sha256:0b9c71279b688f0d | Failed: Transactions click timeout | exec_1875_e54f165a369c49b690521db4ca5dc4f2 |
| 6293 | 1684 | sha256:def91ae46281e466 | Passed: login, Transfer navigation, dashboard | exec_6293_e9e4492e2e584b02826fcf9e86a5894b |
| 6349 | 1814 | sha256:a4722c079705317d | Failed: dashboard navigation timeout | exec_6349_39d6ed4f089d4ff8955ce17a08055c2d |
| 6352 | 1827 | sha256:46cb746c41f7cb47 | Failed: login-submit missing | exec_6352_0ed9e91d191d42bda79af0265ad2b3ec |
| 6270 | 933 | sha256:88c4334ee659fef4 | Failed: immediate dashboard URL assertion | exec_6270_71832ea48bf14c9b90ff936cec4bf674 |
| 6271 | 933 | sha256:23e33b82ee2e55be | Passed: demo login only | exec_6271_c60e7a1f34454f03ba9986c4c5d73f77 |
| 1876 | 943 | sha256:47f22eea634c5086 | Passed: public login page only | exec_1876_865dc250dab246fbbb05275edf3520b6 |
| 6289 | 1680 | sha256:049cf2311b326175 | Passed: partial navigation, no assertions | exec_6289_e1a885ba0c6849bdae8c4721d23d9f02 |

## Evidence limits and next work

Cloud receipts do not attest the script digest, actual visited origin, or
deployed commit. Sources inspected use the requested origin (6352 uses a
relative URL), but exact revision/origin verification remains unavailable.
The request specified headless execution; fetched failure logs show the hosted
runner invoked `playwright test --headed`. Headless execution is unverified.
No auth profiles are configured; sampled scripts retain their existing demo
fixture login. No credentials were added or changed.

Only eight scripts were executed; this is not a passing full-suite claim.
The application intentionally contains vulnerabilities. This review does not
change those behaviors or treat expected seeded behavior as an accidental bug.
No local 9lives spec was created or revised, so no creation snapshot or local
browser run was needed. Jev decision evidence was not obtained.

Recommended repair order: restore meaningful access-control checks; repair
transfer selectors while preserving rejection and unchanged-state assertions;
replace assertion-free partial traces with complete requirement-backed tests;
resolve navigation failures from cloud traces; then add positive transfer,
history/search, and logout/session coverage. Every revised managed script needs
its own provenance snapshot and exact-revision cloud verification.
