# Native Android and iOS apps

The native clients call this repository's real Demo Bank HTTP API, expose
stable automation identifiers, and render the deliberate negative-transfer
failure as reviewable evidence in an emulator or simulator.

Both clients mirror the core web banking journeys:

- account dashboard with combined and per-account balances
- ordinary positive transfers with success feedback
- recent activity and searchable transaction history
- refresh and sign-out controls
- a banking-only customer interface, with a separate inspectable verification receipt for recorded test runs

## Crisis-prevention scenario

The backend has deterministic `buggy` and `clean` modes. In `buggy` mode it
accepts a negative transfer and increases the source balance. In `clean` mode
it rejects the same request.

1. Start the backend with `DEMO_MODE=buggy npm start`.
2. Sign in with `demo` / `demo123`.
3. Transfer `-100.00` from Checking to Savings.
4. Capture the accepted transfer and increased balance as failing evidence.
5. Repeat against `DEMO_MODE=clean npm start`.
6. Capture the rejection and independently verified unchanged balance.

Never deploy the deliberately vulnerable backend or these demo credentials as
a real banking service.

## Android

The Android emulator reaches the host backend at `http://10.0.2.2:3000`.

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
```

Override the backend when building:

```bash
./gradlew assembleDebug \
  -PdemoBankBaseUrl=https://qualitymax-demo-bank-bug.example.com
```

Install `app/build/outputs/apk/debug/app-debug.apk` on a managed emulator.

## iOS

The iOS simulator reaches the host backend at `http://127.0.0.1:3000`.

```bash
cd ios
xcodebuild \
  -project QualityMaxDemoBank.xcodeproj \
  -scheme QualityMaxDemoBank \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Override `DEMO_BANK_BASE_URL` in the scheme or build settings for a remote
backend.

For repeatable full-resolution video stills, launch the simulator build with
one of these arguments while the backend is running:

```bash
xcrun simctl launch booted io.qualitymax.demobank --qualitymax-capture-dashboard
xcrun simctl launch booted io.qualitymax.demobank --qualitymax-capture-transfer
xcrun simctl launch booted io.qualitymax.demobank --qualitymax-capture-crisis
```

The hidden capture flags sign in automatically and stop on the requested
live-data screen without adding test controls or narration to the banking UI.
The crisis flag also submits the prefilled negative transfer,
producing the red failure receipt in `buggy` mode and the green prevention
receipt in `clean` mode. They have no effect on a normal app launch.

## Stable automation identifiers

Both apps expose the same semantic identifiers:

| Screen | Identifier |
| --- | --- |
| Login | `login.username`, `login.password`, `login.submit` |
| Dashboard | `dashboard.total_balance`, `dashboard.transfer`, `dashboard.transactions` |
| Transfer | `transfer.from_account`, `transfer.to_account`, `transfer.amount`, `transfer.submit` |
| Transactions | `transactions.search`, `transactions.search_submit`, `transactions.row.<id>` |
| Evidence | `receipt.title`, `receipt.before_balance`, `receipt.after_balance`, `receipt.status` |

Red is reserved for a reproduced vulnerability and green for the clean
backend's verified rejection. The receipts also use text and values, so the
result does not rely on color alone.
