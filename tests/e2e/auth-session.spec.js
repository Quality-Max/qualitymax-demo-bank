// Source: REQUIREMENTS.md US-1, US-2, US-7 at 2e44f79798cf08fb7020c832cd79181381520521.
import { test, expect } from '@playwright/test';
import { login, accounts } from './helpers';

for (const [username, password, name] of [
  ['demo', 'demo123', 'Alex Morgan'],
  ['admin', 'admin', 'Admin User'],
  ['jane', 'jane456', 'Jane Cooper'],
]) {
  test(`US-1/US-2: ${username} logs in and sees their account balances`, async ({ page }) => {
    await login(page, username, password);
    await expect(page.locator('#user-name')).toHaveText(name);
    const data = await accounts(page);
    expect(data.length).toBeGreaterThan(0);
    await expect(page.getByTestId('account-card')).toHaveCount(data.length);
    const money = amount => '$' + amount.toLocaleString('en-US', {
      minimumFractionDigits: 2, maximumFractionDigits: 2,
    });
    for (const account of data) {
      const card = page.getByTestId('account-card').filter({ hasText: account.name });
      await expect(card).toContainText(account.type);
      await expect(card.getByTestId('account-balance')).toHaveText(money(account.balance));
    }
    await expect(page.getByTestId('total-balance')).toHaveText(
      money(data.reduce((sum, account) => sum + account.balance, 0))
    );
    expect((await page.context().cookies()).some(cookie => cookie.name === 'session')).toBe(true);
  });
}

test('US-1: invalid login shows an error and creates no session', async ({ page }) => {
  await page.goto('/login');
  await page.getByTestId('username').fill('demo');
  await page.getByTestId('password').fill('incorrect-fixture-password');
  const responsePromise = page.waitForResponse(response => response.url().endsWith('/api/login'));
  await page.getByTestId('login-button').click();
  expect((await responsePromise).status()).toBe(401);
  await expect(page.locator('#error')).toHaveText('Invalid credentials');
  await expect(page.locator('#error')).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
  expect((await page.context().cookies()).some(cookie => cookie.name === 'session')).toBe(false);
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login$/);
});

test('US-1: unauthenticated visitors cannot open protected pages', async ({ page }) => {
  for (const route of ['/dashboard', '/transfer', '/transactions', '/settings']) {
    await page.goto(route);
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByTestId('login-button')).toBeVisible();
  }
  const response = await page.request.get('/api/accounts');
  expect(response.status()).toBe(401);
});

test('US-7: logout clears the cookie and invalidates the old token', async ({ page }) => {
  await login(page);
  const session = (await page.context().cookies()).find(cookie => cookie.name === 'session');
  expect(session).toBeDefined();
  await page.getByTestId('logout-button').click();
  await expect(page).toHaveURL(/\/login$/);
  expect((await page.context().cookies()).some(cookie => cookie.name === 'session')).toBe(false);
  const response = await page.request.get('/api/me', {
    headers: { Cookie: `session=${session.value}` },
  });
  expect(response.status()).toBe(401);
  await page.goBack();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByTestId('login-button')).toBeVisible();
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login$/);
});
