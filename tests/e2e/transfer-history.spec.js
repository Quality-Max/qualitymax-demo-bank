// Source: REQUIREMENTS.md US-3, US-4 at 2e44f79798cf08fb7020c832cd79181381520521.
import { test, expect } from '@playwright/test';
import { login, accounts, transactions } from './helpers';

test.beforeEach(async ({ page }) => {
  await login(page);
});

test('US-3/US-4: a $25 transfer updates both balances and appears in history', async ({ page }) => {
  const before = await accounts(page);
  const source = before.find(account => account.id === 1);
  const destination = before.find(account => account.id === 2);
  const beforeSource = await transactions(page, 1);
  const beforeDestination = await transactions(page, 2);
  const description = '9lives positive transfer';

  await page.getByRole('link', { name: 'Transfer', exact: true }).click();
  await expect(page).toHaveURL(/\/transfer$/);
  await page.getByTestId('from-account').selectOption('1');
  await page.getByTestId('to-account').selectOption('2');
  await page.getByTestId('amount').fill('25');
  await page.getByTestId('description').fill(description);
  const responsePromise = page.waitForResponse(response =>
    response.url().endsWith('/api/transfer') && response.request().method() === 'POST'
  );
  await page.getByTestId('transfer-button').click();
  expect((await responsePromise).status()).toBe(200);
  await expect(page.locator('#alert')).toHaveClass(/alert-success/);
  await expect(page.locator('#alert')).toContainText('Transfer successful!');

  const after = await accounts(page);
  expect(after.find(account => account.id === 1).balance).toBe(source.balance - 25);
  expect(after.find(account => account.id === 2).balance).toBe(destination.balance + 25);
  const afterSource = await transactions(page, 1);
  const afterDestination = await transactions(page, 2);
  expect(afterSource).toHaveLength(beforeSource.length + 1);
  expect(afterDestination).toHaveLength(beforeDestination.length + 1);
  expect(afterSource.at(-1)).toMatchObject({ description, type: 'debit', amount: 25 });
  expect(afterDestination.at(-1)).toMatchObject({ description, type: 'credit', amount: 25 });

  await page.getByTestId('nav-dashboard').click();
  const checking = page.getByTestId('account-card').filter({ hasText: 'Checking Account' });
  const savings = page.getByTestId('account-card').filter({ hasText: 'Savings Account' });
  const money = amount => '$' + amount.toLocaleString('en-US', { minimumFractionDigits: 2 });
  await expect(checking.getByTestId('account-balance')).toHaveText(money(source.balance - 25));
  await expect(savings.getByTestId('account-balance')).toHaveText(money(destination.balance + 25));

  await page.getByTestId('nav-transactions').click();
  await page.getByTestId('search-input').fill(description);
  await page.getByTestId('search-button').click();
  const row = page.getByTestId('transaction-row').filter({ hasText: description });
  await expect(row).toHaveCount(1);
  await expect(row).toContainText('-$25.00');
  await expect(row).toContainText('transfer');
});

test('US-3: insufficient funds shows an error without changing balances or history', async ({ page }) => {
  const before = await accounts(page);
  const history = await transactions(page, 1);
  const savingsHistory = await transactions(page, 2);
  await page.getByRole('link', { name: 'Transfer', exact: true }).click();
  await page.getByTestId('from-account').selectOption('1');
  await page.getByTestId('to-account').selectOption('2');
  await page.getByTestId('amount').fill(String(before.find(account => account.id === 1).balance + 1));
  const responsePromise = page.waitForResponse(response => response.url().endsWith('/api/transfer'));
  await page.getByTestId('transfer-button').click();
  expect((await responsePromise).status()).toBe(400);
  await expect(page.locator('#alert')).toHaveClass(/alert-error/);
  await expect(page.locator('#alert')).toHaveText('Insufficient funds');
  await expect(page.locator('.alert-success')).toHaveCount(0);
  expect(await accounts(page)).toEqual(before);
  expect(await transactions(page, 1)).toEqual(history);
  expect(await transactions(page, 2)).toEqual(savingsHistory);
});

// Direct authenticated API requests ensure server validation is exercised even
// when the browser's min attribute would prevent form submission.
for (const [name, amount, toAccount, message] of [
  ['negative amount', -100, 2, 'Amount must be greater than zero'],
  ['zero amount', 0, 2, 'Amount must be greater than zero'],
  ['same account', 25, 1, 'Choose a different destination account'],
]) {
  test(`US-3: server rejects ${name} without any ledger changes`, async ({ page }) => {
    const before = await accounts(page);
    const sourceHistory = await transactions(page, 1);
    const destinationHistory = await transactions(page, 2);
    const response = await page.request.post('/api/transfer', {
      data: { from_account_id: 1, to_account_id: toAccount, amount, description: `9lives ${name}` },
    });
    expect(response.status()).toBe(400);
    expect(await response.json()).toEqual({ error: message });
    expect(await accounts(page)).toEqual(before);
    expect(await transactions(page, 1)).toEqual(sourceHistory);
    expect(await transactions(page, 2)).toEqual(destinationHistory);
  });
}

for (const [query, expectedDescription] of [
  ['Netflix', 'Netflix Subscription'],
  ['groceries', 'Grocery Store'],
]) {
  test(`US-4: searching ${query} returns the matching transaction`, async ({ page }) => {
    await page.getByTestId('nav-transactions').click();
    await page.getByTestId('search-input').fill(query);
    await page.getByTestId('search-button').click();
    await expect(page.getByTestId('transaction-row')).toHaveCount(1);
    await expect(page.getByTestId('transaction-description')).toHaveText(expectedDescription);
    await expect(page.locator('#empty-state')).toBeHidden();
  });
}

test('US-4: an unmatched search shows the empty state and clears previous results', async ({ page }) => {
  await page.getByTestId('nav-transactions').click();
  await expect(page.getByTestId('transaction-row').filter({ hasText: 'Salary Deposit' })).toBeVisible();
  await page.getByTestId('search-input').fill('9lives-no-matching-transaction');
  await page.getByTestId('search-button').click();
  await expect(page.getByTestId('transaction-row')).toHaveCount(0);
  await expect(page.locator('#empty-state')).toHaveText('No transactions found.');
  await expect(page.locator('#empty-state')).toBeVisible();
});
