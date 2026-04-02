# QualityMax Demo Bank

A deliberately vulnerable banking application for demonstrating [QualityMax](https://qualitymax.io) testing capabilities.

**DO NOT deploy this in production.** This app contains intentional security vulnerabilities for testing purposes.

## Quick Start

```bash
git clone https://github.com/Quality-Max/qualitymax-demo-bank.git
cd qualitymax-demo-bank
node server.js
```

Open http://localhost:3000

## Test Accounts

| Username | Password | Role |
|----------|----------|------|
| demo | demo123 | user |
| admin | admin | admin |
| jane | jane456 | user |

## What to Test with QualityMax

| Feature | What QualityMax Finds |
|---------|----------------------|
| **Import REQUIREMENTS.md** | Generates test cases from user stories |
| **AI Crawl** | Discovers all pages, forms, navigation flows |
| **Gap Analysis** | Missing error handling, edge cases, smoke tests |
| **Security Scan** | XSS, IDOR, CSRF, broken access control, mass assignment |
| **k6 Load Test** | API performance under load (/api/login, /api/transfer, /api/transactions) |

## Intentional Vulnerabilities

This app is designed for security testing demos. Vulnerabilities include:

- **XSS**: Search query reflected without sanitization
- **IDOR**: Any user can access any account via `/api/accounts/:id`
- **CSRF**: No CSRF tokens on any forms
- **Broken Access Control**: `/api/admin/users` accessible to all authenticated users
- **Mass Assignment**: Profile update API accepts `role` field
- **Session Issues**: No expiry, missing Secure/SameSite cookie flags
- **No Rate Limiting**: Login endpoint allows unlimited attempts
- **Information Disclosure**: Admin endpoint exposes passwords
- **Timing Attack**: Different response times for valid vs invalid usernames
- **Input Validation**: Negative transfer amounts accepted

## API Documentation

See [API.md](API.md) for the full API specification.

## Project Structure

```
qualitymax-demo-bank/
  server.js           # Node.js server (zero dependencies)
  views/
    login.html        # Login page
    dashboard.html    # Account overview
    transfer.html     # Money transfer form
    transactions.html # Transaction history with search
    settings.html     # Profile settings
    docs.html         # API documentation page
  static/
    style.css         # Styles
  REQUIREMENTS.md     # User stories (import into QualityMax)
  API.md              # API spec (for k6 test generation)
  SMOKE_TESTS.md      # Incomplete smoke checklist (gap analysis bait)
```

## License

MIT
