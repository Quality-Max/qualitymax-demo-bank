/**
 * QualityMax Demo Bank — Express-free Node.js server
 * Zero dependencies. Run: node server.js
 *
 * Intentional bugs and security gaps for QualityMax demo purposes.
 * DO NOT use this in production.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = process.env.PORT || 3000;

// ============================================================================
// In-memory database
// ============================================================================

const USERS = {
  demo: { id: 1, username: 'demo', password: 'demo123', name: 'Alex Morgan', email: 'alex@demobank.com', role: 'user' },
  admin: { id: 2, username: 'admin', password: 'admin', name: 'Admin User', email: 'admin@demobank.com', role: 'admin' },
  jane: { id: 3, username: 'jane', password: 'jane456', name: 'Jane Cooper', email: 'jane@demobank.com', role: 'user' },
};

const ACCOUNTS = {
  1: { id: 1, userId: 1, name: 'Checking Account', balance: 5420.50, type: 'checking', number: '****4521' },
  2: { id: 2, userId: 1, name: 'Savings Account', balance: 12800.00, type: 'savings', number: '****8832' },
  3: { id: 3, userId: 2, name: 'Admin Checking', balance: 99999.99, type: 'checking', number: '****0001' },
  4: { id: 4, userId: 3, name: 'Jane Checking', balance: 3200.75, type: 'checking', number: '****7744' },
};

let TRANSACTIONS = [
  { id: 1, accountId: 1, type: 'credit', amount: 3500.00, description: 'Salary Deposit', date: '2026-03-28', category: 'income' },
  { id: 2, accountId: 1, type: 'debit', amount: 45.99, description: 'Netflix Subscription', date: '2026-03-27', category: 'entertainment' },
  { id: 3, accountId: 1, type: 'debit', amount: 128.50, description: 'Grocery Store', date: '2026-03-26', category: 'groceries' },
  { id: 4, accountId: 1, type: 'debit', amount: 250.00, description: 'Transfer to Savings', date: '2026-03-25', category: 'transfer' },
  { id: 5, accountId: 2, type: 'credit', amount: 250.00, description: 'Transfer from Checking', date: '2026-03-25', category: 'transfer' },
  { id: 6, accountId: 1, type: 'debit', amount: 89.00, description: 'Electric Bill', date: '2026-03-24', category: 'utilities' },
  { id: 7, accountId: 1, type: 'credit', amount: 150.00, description: 'Freelance Payment', date: '2026-03-23', category: 'income' },
  { id: 8, accountId: 1, type: 'debit', amount: 35.00, description: 'Gas Station', date: '2026-03-22', category: 'transport' },
];

let nextTransactionId = 9;
const sessions = {};

// ============================================================================
// Helpers
// ============================================================================

function createSession(userId) {
  const token = crypto.randomBytes(16).toString('hex');
  sessions[token] = { userId, createdAt: Date.now() };
  return token;
}

function getSession(token) {
  const session = sessions[token];
  if (!session) return null;
  // BUG: No session expiry check — sessions never expire
  return session;
}

function getUserFromRequest(req) {
  const cookies = parseCookies(req);
  const token = cookies.session;
  if (!token) return null;
  const session = getSession(token);
  if (!session) return null;
  return Object.values(USERS).find(u => u.id === session.userId) || null;
}

function parseCookies(req) {
  const cookies = {};
  (req.headers.cookie || '').split(';').forEach(c => {
    const [k, v] = c.trim().split('=');
    if (k) cookies[k] = decodeURIComponent(v || '');
  });
  return cookies;
}

function parseBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try { resolve(JSON.parse(body)); }
      catch { resolve(body); }
    });
  });
}

function json(res, data, status = 200) {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(data));
}

function serveFile(res, filePath) {
  const ext = path.extname(filePath);
  const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'application/javascript', '.png': 'image/png', '.svg': 'image/svg+xml' };
  fs.readFile(filePath, (err, data) => {
    if (err) { res.writeHead(404); res.end('Not Found'); return; }
    res.writeHead(200, { 'Content-Type': types[ext] || 'text/plain' });
    res.end(data);
  });
}

function redirect(res, url) {
  res.writeHead(302, { Location: url });
  res.end();
}

// ============================================================================
// Routes
// ============================================================================

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const pathname = url.pathname;
  const method = req.method;

  // SECURITY GAP: No Content-Security-Policy, no X-Frame-Options
  // SECURITY GAP: No rate limiting anywhere

  // --- Static files ---
  if (pathname.startsWith('/static/')) {
    return serveFile(res, path.join(__dirname, pathname));
  }

  // --- Pages ---
  if (method === 'GET') {
    if (pathname === '/' || pathname === '/login') {
      return serveFile(res, path.join(__dirname, 'views/login.html'));
    }
    if (pathname === '/dashboard') {
      const user = getUserFromRequest(req);
      if (!user) return redirect(res, '/login');
      return serveFile(res, path.join(__dirname, 'views/dashboard.html'));
    }
    if (pathname === '/transfer') {
      const user = getUserFromRequest(req);
      if (!user) return redirect(res, '/login');
      return serveFile(res, path.join(__dirname, 'views/transfer.html'));
    }
    if (pathname === '/transactions') {
      const user = getUserFromRequest(req);
      if (!user) return redirect(res, '/login');
      return serveFile(res, path.join(__dirname, 'views/transactions.html'));
    }
    if (pathname === '/settings') {
      const user = getUserFromRequest(req);
      if (!user) return redirect(res, '/login');
      return serveFile(res, path.join(__dirname, 'views/settings.html'));
    }
    if (pathname === '/docs') {
      return serveFile(res, path.join(__dirname, 'views/docs.html'));
    }
  }

  // --- API: Auth ---
  if (method === 'POST' && pathname === '/api/login') {
    const body = await parseBody(req);
    const user = USERS[body.username];
    // SECURITY GAP: No rate limiting on login — brute force possible
    // SECURITY GAP: Timing attack — different response times for valid vs invalid usernames
    if (!user) {
      await new Promise(r => setTimeout(r, 500)); // Slower for invalid username
      return json(res, { error: 'Invalid credentials' }, 401);
    }
    if (user.password !== body.password) {
      return json(res, { error: 'Invalid credentials' }, 401);
    }
    const token = createSession(user.id);
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Set-Cookie': `session=${token}; Path=/; HttpOnly`,
      // SECURITY GAP: Missing Secure flag, missing SameSite attribute
    });
    res.end(JSON.stringify({ success: true, user: { id: user.id, name: user.name, email: user.email } }));
    return;
  }

  if (method === 'POST' && pathname === '/api/logout') {
    const cookies = parseCookies(req);
    if (cookies.session) delete sessions[cookies.session];
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Set-Cookie': 'session=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT',
    });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // --- API: User ---
  if (method === 'GET' && pathname === '/api/me') {
    const user = getUserFromRequest(req);
    if (!user) return json(res, { error: 'Unauthorized' }, 401);
    return json(res, { id: user.id, name: user.name, email: user.email, role: user.role });
  }

  if (method === 'PUT' && pathname === '/api/me') {
    const user = getUserFromRequest(req);
    if (!user) return json(res, { error: 'Unauthorized' }, 401);
    const body = await parseBody(req);
    // SECURITY GAP: XSS — name is stored without sanitization
    if (body.name) user.name = body.name;
    if (body.email) user.email = body.email;
    // BUG: Can update role via API (mass assignment)
    if (body.role) user.role = body.role;
    return json(res, { success: true, user: { id: user.id, name: user.name, email: user.email, role: user.role } });
  }

  // --- API: Accounts ---
  if (method === 'GET' && pathname === '/api/accounts') {
    const user = getUserFromRequest(req);
    if (!user) return json(res, { error: 'Unauthorized' }, 401);
    const userAccounts = Object.values(ACCOUNTS).filter(a => a.userId === user.id);
    return json(res, { accounts: userAccounts });
  }

  if (method === 'GET' && pathname.match(/^\/api\/accounts\/(\d+)$/)) {
    // SECURITY GAP: IDOR — no authorization check, any authenticated user can access any account
    const user = getUserFromRequest(req);
    if (!user) return json(res, { error: 'Unauthorized' }, 401);
    const accountId = parseInt(pathname.split('/')[3]);
    const account = ACCOUNTS[accountId];
    if (!account) return json(res, { error: 'Account not found' }, 404);
    // BUG: Should check account.userId === user.id but doesn't
    return json(res, { account });
  }

  // --- API: Transactions ---
  if (method === 'GET' && pathname === '/api/transactions') {
    const user = getUserFromRequest(req);
    if (!user) return json(res, { error: 'Unauthorized' }, 401);
    const accountId = parseInt(url.searchParams.get('account_id')) || 1;
    const search = url.searchParams.get('search') || '';

    let txns = TRANSACTIONS.filter(t => t.accountId === accountId);

    // SECURITY GAP: Search parameter reflected without sanitization (stored XSS vector)
    if (search) {
      txns = txns.filter(t =>
        t.description.toLowerCase().includes(search.toLowerCase()) ||
        t.category.toLowerCase().includes(search.toLowerCase())
      );
    }

    return json(res, {
      transactions: txns,
      total: txns.length,
      search_query: search, // XSS: reflected back to client unsanitized
    });
  }

  // --- API: Transfer ---
  if (method === 'POST' && pathname === '/api/transfer') {
    const user = getUserFromRequest(req);
    if (!user) return json(res, { error: 'Unauthorized' }, 401);
    // SECURITY GAP: No CSRF token validation
    const body = await parseBody(req);
    const fromId = parseInt(body.from_account_id);
    const toId = parseInt(body.to_account_id);
    const amount = parseFloat(body.amount);
    const description = body.description || 'Transfer';

    const fromAccount = ACCOUNTS[fromId];
    const toAccount = ACCOUNTS[toId];

    if (!fromAccount) return json(res, { error: 'Source account not found' }, 404);
    if (!toAccount) return json(res, { error: 'Destination account not found' }, 404);

    // BUG: No check that fromAccount belongs to the user (IDOR)
    // BUG: Negative amounts are accepted — can steal money
    // BUG: No check for NaN or Infinity
    // BUG: No minimum transfer amount
    // BUG: Can transfer to the same account
    // BUG: Floating point precision issues (no rounding)

    if (fromAccount.balance < amount) {
      return json(res, { error: 'Insufficient funds' }, 400);
    }

    fromAccount.balance -= amount;
    toAccount.balance += amount;

    const txn1 = {
      id: nextTransactionId++, accountId: fromId, type: 'debit',
      amount, description, date: new Date().toISOString().split('T')[0], category: 'transfer',
    };
    const txn2 = {
      id: nextTransactionId++, accountId: toId, type: 'credit',
      amount, description, date: new Date().toISOString().split('T')[0], category: 'transfer',
    };
    TRANSACTIONS.push(txn1, txn2);

    return json(res, { success: true, transaction: txn1, new_balance: fromAccount.balance });
  }

  // --- API: Admin (broken access control) ---
  if (method === 'GET' && pathname === '/api/admin/users') {
    // SECURITY GAP: No role check — any authenticated user can list all users
    const user = getUserFromRequest(req);
    if (!user) return json(res, { error: 'Unauthorized' }, 401);
    // BUG: Exposes passwords in response
    return json(res, { users: Object.values(USERS) });
  }

  // --- API: Health ---
  if (method === 'GET' && pathname === '/api/health') {
    return json(res, { status: 'ok', uptime: process.uptime() });
  }

  // --- 404 ---
  // BUG: No custom 404 page — just plain text
  res.writeHead(404);
  res.end('Not Found');
});

server.listen(PORT, () => {
  console.log(`QualityMax Demo Bank running on http://localhost:${PORT}`);
  console.log('');
  console.log('Test accounts:');
  console.log('  demo / demo123  (regular user)');
  console.log('  admin / admin   (admin user)');
  console.log('  jane / jane456  (regular user)');
});
