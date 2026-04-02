# Demo Bank — Requirements & User Stories

## User Stories

### US-1: User Login
**As a** bank customer
**I want to** log in with my username and password
**So that** I can access my banking dashboard

**Acceptance Criteria:**
- User enters username and password
- System validates credentials
- On success: redirect to dashboard, set session cookie
- On failure: show error message, stay on login page
- Session should expire after 30 minutes of inactivity
- After 5 failed attempts, account should be locked for 15 minutes

### US-2: View Dashboard
**As a** logged-in user
**I want to** see my account overview
**So that** I can check my balances at a glance

**Acceptance Criteria:**
- Show total balance across all accounts
- Display each account with name, type, and current balance
- Show last 5 transactions
- All monetary values formatted with $ sign and 2 decimal places

### US-3: Transfer Money
**As a** logged-in user
**I want to** transfer money between accounts
**So that** I can manage my finances

**Acceptance Criteria:**
- Select source and destination accounts
- Enter transfer amount (minimum $0.01, maximum $10,000)
- Amount cannot be negative or zero
- Cannot transfer to the same account
- Cannot transfer more than available balance
- Show confirmation before executing
- Display success/error message after transfer
- Update balances immediately

### US-4: Search Transactions
**As a** logged-in user
**I want to** search my transaction history
**So that** I can find specific transactions

**Acceptance Criteria:**
- Search by description or category
- Show matching transactions with date, description, category, amount
- Display "No transactions found" for empty results
- Search input should be sanitized to prevent injection

### US-5: Update Profile
**As a** logged-in user
**I want to** update my display name and email
**So that** my profile stays current

**Acceptance Criteria:**
- Pre-fill form with current values
- Validate email format
- Display name must be 2-50 characters
- Show success confirmation after saving
- Changes reflected immediately in navigation

### US-6: Admin User Management
**As an** admin user
**I want to** view all registered users
**So that** I can manage the system

**Acceptance Criteria:**
- Only accessible to users with admin role
- Display user ID, name, email, role
- Never expose passwords in the response
- Regular users should get 403 Forbidden

### US-7: Logout
**As a** logged-in user
**I want to** log out securely
**So that** my session is terminated

**Acceptance Criteria:**
- Clear session cookie
- Redirect to login page
- Pressing back button after logout should not show dashboard
- Session token should be invalidated server-side

## Non-Functional Requirements

### Security
- All forms must include CSRF tokens
- Passwords must be hashed (never stored in plain text)
- Session cookies must have Secure, HttpOnly, and SameSite flags
- Input sanitization on all user-provided data
- Rate limiting on login endpoint (max 5 attempts per minute)
- Content-Security-Policy headers on all responses

### Performance
- Login should complete in under 500ms
- Dashboard should load in under 1 second
- API responses should be under 200ms
- Transfer endpoint should handle 100 concurrent requests

### Accessibility
- All form inputs must have associated labels
- Color contrast ratio must be at least 4.5:1
- Navigation must be keyboard accessible
