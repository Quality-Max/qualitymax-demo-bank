# QualityMax Live Demo Setup

This repo supports three predictable live-demo states through `DEMO_MODE`.
Use separate Vercel projects or preview deployments so you can switch URLs during
the QualityMax/qmax-code demo without editing or redeploying live.

## Demo Modes

| Mode | Environment | Purpose |
| --- | --- | --- |
| Clean | `DEMO_MODE=clean` | Stable app for crawl, happy-path Playwright generation, and passing cloud runs. Negative transfers are rejected. |
| Bug seeded | `DEMO_MODE=buggy` | Realistic business-rule bug. Negative transfers are accepted and increase the source balance. This is the "find a bug" module. |
| Selector changed | `DEMO_MODE=selector-change` | Behavior stays clean, but the transfer navigation selector changes from `data-test="nav-transfer"` to `data-test="nav-send-money"`. This is the Test Doctor/self-heal module. |

The default is `buggy` to preserve the repo's intentionally vulnerable demo
behavior. Set `DEMO_MODE=clean` explicitly for the clean deployment.

## Recommended Deployment URLs

Replace these with the actual Vercel URLs before the live demo:

```text
CLEAN_URL=https://qualitymax-demo-bank.vercel.app
BUG_URL=https://qualitymax-demo-bank-bug.vercel.app
BROKEN_SELECTOR_URL=https://qualitymax-demo-bank-selector-change.vercel.app
REPO_URL=https://github.com/Quality-Max/qualitymax-demo-bank
```

## Vercel Configuration

Create three deployments from the same repo:

1. `qualitymax-demo-bank`
   - `DEMO_MODE=clean`
2. `qualitymax-demo-bank-bug`
   - `DEMO_MODE=buggy`
3. `qualitymax-demo-bank-selector-change`
   - `DEMO_MODE=selector-change`

The Vercel rewrites route app pages through the Node handler so environment
variables can control both API behavior and rendered HTML selectors.

## Local Verification

Run the smoke tests:

```bash
npm test
```

Run each mode locally:

```bash
DEMO_MODE=clean npm start
DEMO_MODE=buggy npm start
DEMO_MODE=selector-change npm start
```

Check the active mode:

```bash
curl http://localhost:3000/api/health
```

## qmax-code Demo Prompts

### Module 1 - Crawl clean app

```text
Create or reuse the QualityMax project for CLEAN_URL. Crawl it with depth 2 and pages limit 6. If login is required, use demo/demo123. Show me the critical user journeys, risky flows, forms, and suggested first tests. Do not generate code yet.
```

### Module 2 - Find the seeded transfer bug

```text
Create a high-priority functional test for the transfer flow on BUG_URL.

Steps:
1. Log in with demo/demo123.
2. Open Transfer.
3. Try to send -100 to Savings.
4. Assert the app rejects the amount.
5. Assert the sender balance does not increase.
6. Assert no successful transaction is created.

Generate Playwright, run it in the cloud, and classify the result as app bug, test bug, or environment issue. Show the failure evidence.
```

### Module 3 - Generate happy-path suite

```text
Generate a compact Playwright regression suite for CLEAN_URL.

Cover:
- valid login with demo/demo123
- dashboard loads with visible balance
- successful transfer of 25 to Savings
- transaction appears in history or recent activity
- logout returns to login

Use stable role/data-test locators where available. Save the scripts to the QualityMax project, then run them in Chromium headless. Show pass/fail status, screenshots, and video artifacts.
```

### Module 4 - Break and self-heal

```text
Run the existing transfer regression test against BROKEN_SELECTOR_URL. Do not modify it yet. Show me the failure, the selector or step that broke, and the screenshot.
```

Then:

```text
Use Test Doctor to repair this failing transfer test. Crawl the current page, identify the new selector, update only the stale selector or assertion needed, save the script diff, and rerun the test to verify it passes.
```

## Preflight Checklist

- Confirm all three URLs return `200` for `/api/health`.
- Confirm `/api/health` reports the expected `mode`.
- Run `npm test`.
- Warm qmax-code sessions for crawl, bug-find, happy-path generation, and self-heal.
- Record a fallback video for the self-heal module.
- Keep Vercel dashboard, qmax-code, and browser tabs open before the call starts.
