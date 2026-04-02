# Demo Bank — API Specification

## Base URL
```
http://localhost:3000
```

## Authentication
Session-based authentication via HTTP cookies. Call `POST /api/login` first.

---

## POST /api/login
Authenticate a user and receive a session cookie.

**Request:**
```json
{ "username": "demo", "password": "demo123" }
```

**Response (200):**
```json
{ "success": true, "user": { "id": 1, "name": "Alex Morgan", "email": "alex@demobank.com" } }
```

**Response (401):**
```json
{ "error": "Invalid credentials" }
```

---

## POST /api/logout
End the current session.

**Response (200):**
```json
{ "success": true }
```

---

## GET /api/me
Get current user profile.

**Response (200):**
```json
{ "id": 1, "name": "Alex Morgan", "email": "alex@demobank.com", "role": "user" }
```

---

## PUT /api/me
Update user profile.

**Request:**
```json
{ "name": "New Name", "email": "new@email.com" }
```

**Response (200):**
```json
{ "success": true, "user": { "id": 1, "name": "New Name", "email": "new@email.com", "role": "user" } }
```

---

## GET /api/accounts
List accounts for the current user.

**Response (200):**
```json
{
  "accounts": [
    { "id": 1, "userId": 1, "name": "Checking Account", "balance": 5420.50, "type": "checking", "number": "****4521" }
  ]
}
```

---

## GET /api/accounts/:id
Get a specific account by ID.

**Response (200):**
```json
{ "account": { "id": 1, "userId": 1, "name": "Checking Account", "balance": 5420.50, "type": "checking" } }
```

---

## GET /api/transactions
List transactions for an account.

**Query Parameters:**
- `account_id` (required): Account ID
- `search` (optional): Search term for description/category

**Response (200):**
```json
{
  "transactions": [
    { "id": 1, "accountId": 1, "type": "credit", "amount": 3500.00, "description": "Salary", "date": "2026-03-28", "category": "income" }
  ],
  "total": 8,
  "search_query": ""
}
```

---

## POST /api/transfer
Transfer money between accounts.

**Request:**
```json
{
  "from_account_id": 1,
  "to_account_id": 2,
  "amount": 100.00,
  "description": "Monthly savings"
}
```

**Response (200):**
```json
{ "success": true, "transaction": { "id": 9, "accountId": 1, "type": "debit", "amount": 100.00 }, "new_balance": 5320.50 }
```

**Response (400):**
```json
{ "error": "Insufficient funds" }
```

---

## GET /api/admin/users
List all users (admin only).

**Response (200):**
```json
{ "users": [ { "id": 1, "username": "demo", "name": "Alex Morgan", "email": "alex@demobank.com", "role": "user" } ] }
```

---

## GET /api/health
Health check endpoint.

**Response (200):**
```json
{ "status": "ok", "uptime": 123.456 }
```
