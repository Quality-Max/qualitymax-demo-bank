const assert = require('assert');
const { test } = require('node:test');
const { spawn } = require('child_process');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

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
  const port = 4100 + Math.floor(Math.random() * 1000);
  const baseUrl = `http://127.0.0.1:${port}`;
  const child = spawn(process.execPath, ['server.js'], {
    cwd: ROOT,
    env: { ...process.env, PORT: String(port), DEMO_MODE: mode },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  let output = '';
  child.stdout.on('data', chunk => { output += chunk.toString(); });
  child.stderr.on('data', chunk => { output += chunk.toString(); });

  try {
    await waitForHealth(baseUrl, mode);
    await testFn(baseUrl);
  } catch (err) {
    err.message += `\nServer output:\n${output}`;
    throw err;
  } finally {
    child.kill();
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

test('clean mode rejects a negative transfer', () => withServer('clean', async baseUrl => {
  const cookie = await login(baseUrl);
  const res = await postTransfer(baseUrl, cookie, -100);
  const data = await res.json();
  assert.strictEqual(res.status, 400);
  assert.match(data.error, /greater than zero/i);
}));

test('buggy mode exposes the negative-transfer regression', () => withServer('buggy', async baseUrl => {
  const cookie = await login(baseUrl);
  const res = await postTransfer(baseUrl, cookie, -100);
  const data = await res.json();
  assert.strictEqual(res.status, 200);
  assert.strictEqual(data.success, true);
  assert.ok(data.new_balance > 5420.50, 'negative transfer should increase source balance in buggy mode');
}));

test('selector-change mode exposes the renamed navigation target', () => withServer('selector-change', async baseUrl => {
  const cookie = await login(baseUrl);
  const page = await fetch(`${baseUrl}/dashboard`, { headers: { Cookie: cookie } });
  const html = await page.text();
  assert.strictEqual(page.status, 200);
  assert.ok(html.includes('data-test="nav-send-money"'));
  assert.ok(!html.includes('data-test="nav-transfer"'));
}));

for (const mode of ['clean', 'buggy', 'selector-change']) {
  test(`${mode} mode serves standalone Zapier fixtures with their expected quantity totals`, () => withServer(mode, async baseUrl => {
    for (const [name, expectedTotal] of [
      ['quantity-total.broken.fixture.html', '$10.00'],
      ['quantity-total.fixture.html', '$30.00'],
    ]) {
      const response = await fetch(`${baseUrl}/zapier/${name}`);
      assert.strictEqual(response.status, 200);
      assert.match(response.headers.get('content-type'), /^text\/html/);
      const html = await response.text();
      assert.strictEqual(html, fs.readFileSync(path.join(ROOT, 'public/zapier', name), 'utf8'));
      assert.match(html, /<label for="quantity">Quantity<\/label>/);
      assert.match(html, /<input id="quantity" type="number" min="1" value="1">/);
      assert.match(html, /<output id="total" aria-live="polite">\$10\.00<\/output>/);

      const listeners = {};
      const quantity = {
        value: '1',
        addEventListener(event, callback) { listeners[event] = callback; },
      };
      const total = { textContent: '' };
      const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
      vm.runInNewContext(script, {
        document: {
          querySelector(selector) {
            if (selector === '#quantity') return quantity;
            if (selector === '#total') return total;
            throw new Error(`Unexpected selector: ${selector}`);
          },
        },
      });
      assert.strictEqual(total.textContent, '$10.00');
      quantity.value = '3';
      listeners.input();
      assert.strictEqual(total.textContent, expectedTotal);
    }

    for (const pathname of ['/zapier/server.js', '/zapier/README.md', '/zapier/unknown.html']) {
      const response = await fetch(`${baseUrl}${pathname}`);
      assert.strictEqual(response.status, 404, pathname);
    }
  }));
}
