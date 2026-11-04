const { expect } = require('@playwright/test');

async function login(page, username = 'demo', password = 'demo123') {
  await page.goto('/login');
  await page.getByTestId('username').fill(username);
  await page.getByTestId('password').fill(password);
  await page.getByTestId('login-button').click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('heading', { name: 'Dashboard', exact: true })).toBeVisible();
  await expect(page.getByTestId('total-balance')).toBeVisible();
}

async function accounts(page) {
  const response = await page.request.get('/api/accounts');
  expect(response.status()).toBe(200);
  return (await response.json()).accounts;
}

async function transactions(page, accountId) {
  const response = await page.request.get(`/api/transactions?account_id=${accountId}`);
  expect(response.status()).toBe(200);
  return (await response.json()).transactions;
}

module.exports = { login, accounts, transactions };
