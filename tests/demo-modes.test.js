const assert = require('assert');
const { spawn } = require('child_process');
const { test } = require('node:test');

const ROOT = `${__dirname}/..`;

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function waitForHealth(baseUrl, expectedMode) {
  const deadline = Date.now() + 5000;
  let lastError;

  while (Date.now() < deadline) {
    try {
      const res = await fetch(`${baseUrl}/api/health`);
      if (res.ok) {
        const data = await res.json();
        if (data.mode === expectedMode) return data;
      }
    } catch (err) {
      lastError = err;
    }
    await wait(100);
  }

  throw lastError || new Error(`Timed out waiting for ${expectedMode} server`);
}

async function withServer(mode, testFn) {
  // A kernel-assigned loopback port avoids collisions with concurrent demo runs.
  const child = spawn(process.execPath, ['-e', `
    const server = require('./server');
    server.listen(0, '127.0.0.1', () => process.send(server.address().port));
  `], {
    cwd: ROOT,
    // No parent credentials or deployment flags enter the isolated fixture.
    env: { DEMO_MODE: mode, VERCEL: '1' },
    stdio: ['ignore', 'ignore', 'ignore', 'ipc'],
  });
  const exited = new Promise(resolve => {
    child.once('exit', resolve);
    child.once('error', resolve);
  });

  try {
    const port = await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('Fixture did not start')), 5000);
      child.once('message', value => { clearTimeout(timer); resolve(value); });
      child.once('error', error => { clearTimeout(timer); reject(error); });
      child.once('exit', () => { clearTimeout(timer); reject(new Error('Fixture exited before ready')); });
    });
    const baseUrl = `http://127.0.0.1:${port}`;
    await waitForHealth(baseUrl, mode);
    await testFn(baseUrl);
  } finally {
    child.kill();
    await exited;
  }
}

async function login(baseUrl) {
  const res = await fetch(`${baseUrl}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'demo', password: 'demo123' }),
  });
  assert.strictEqual(res.status, 200);
  const cookie = res.headers.get('set-cookie');
  assert.ok(cookie && cookie.includes('session='), 'login should set session cookie');
  return cookie.split(';')[0];
}

async function postTransfer(baseUrl, cookie, amount) {
  return fetch(`${baseUrl}/api/transfer`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Cookie: cookie,
    },
    body: JSON.stringify({
      from_account_id: 1,
      to_account_id: 2,
      amount,
      description: `Mode smoke test ${amount}`,
    }),
  });
}

test('clean mode rejects negative transfers', async () => {
  await withServer('clean', async baseUrl => {
    const cookie = await login(baseUrl);
    const res = await postTransfer(baseUrl, cookie, -100);
    const data = await res.json();
    assert.strictEqual(res.status, 400);
    assert.match(data.error, /greater than zero/i);
  });
});

test('buggy mode exposes the deliberate negative-transfer fault', async () => {
  await withServer('buggy', async baseUrl => {
    const cookie = await login(baseUrl);
    const res = await postTransfer(baseUrl, cookie, -100);
    const data = await res.json();
    assert.strictEqual(res.status, 200);
    assert.strictEqual(data.success, true);
    assert.ok(data.new_balance > 5420.50, 'negative transfer should increase source balance in buggy mode');
  });
});

test('selector-change mode exposes the replacement navigation selector', async () => {
  await withServer('selector-change', async baseUrl => {
    const cookie = await login(baseUrl);
    const page = await fetch(`${baseUrl}/dashboard`, { headers: { Cookie: cookie } });
    const html = await page.text();
    assert.strictEqual(page.status, 200);
    assert.ok(html.includes('data-test="nav-send-money"'));
    assert.ok(!html.includes('data-test="nav-transfer"'));
  });
});

async function accounts(baseUrl, cookie) {
  const res = await fetch(`${baseUrl}/api/accounts`, { headers: { Cookie: cookie } });
  assert.strictEqual(res.status, 200);
  return (await res.json()).accounts;
}

async function transactions(baseUrl, cookie) {
  const res = await fetch(`${baseUrl}/api/transactions?account_id=1`, { headers: { Cookie: cookie } });
  assert.strictEqual(res.status, 200);
  return (await res.json()).transactions;
}

test('a clean transfer debits and credits exactly once and preserves total funds', async () => {
  await withServer('clean', async baseUrl => {
    const cookie = await login(baseUrl);
    const before = await accounts(baseUrl, cookie);
    const ledgerBefore = await transactions(baseUrl, cookie);
    const res = await postTransfer(baseUrl, cookie, 125);
    const data = await res.json();
    assert.strictEqual(res.status, 200);
    assert.strictEqual(data.success, true);
    const after = await accounts(baseUrl, cookie);
    const source = after.find(account => account.id === 1);
    const destination = after.find(account => account.id === 2);
    assert.strictEqual(source.balance, before.find(account => account.id === 1).balance - 125);
    assert.strictEqual(destination.balance, before.find(account => account.id === 2).balance + 125);
    assert.strictEqual(data.new_balance, source.balance);
    assert.strictEqual(after.reduce((sum, account) => sum + account.balance, 0),
      before.reduce((sum, account) => sum + account.balance, 0));
    const ledgerAfter = await transactions(baseUrl, cookie);
    assert.strictEqual(ledgerAfter.length, ledgerBefore.length + 1);
    const debit = ledgerAfter.find(entry => entry.id === data.transaction.id);
    assert.ok(debit, 'response transaction must exist in the source ledger');
    assert.strictEqual(debit.type, 'debit');
    assert.strictEqual(debit.amount, 125);
    assert.strictEqual(debit.accountId, 1);
  });
});

test('a declined clean transfer changes neither balances nor the source ledger', async () => {
  await withServer('clean', async baseUrl => {
    const cookie = await login(baseUrl);
    const before = await accounts(baseUrl, cookie);
    const ledgerBefore = await transactions(baseUrl, cookie);
    const res = await postTransfer(baseUrl, cookie, 1000000);
    const data = await res.json();
    assert.strictEqual(res.status, 400);
    assert.match(data.error, /insufficient funds/i);
    assert.deepStrictEqual(await accounts(baseUrl, cookie), before);
    assert.deepStrictEqual(await transactions(baseUrl, cookie), ledgerBefore);
  });
});
