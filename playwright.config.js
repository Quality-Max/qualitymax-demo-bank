const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30000,
  use: {
    baseURL: 'http://127.0.0.1:3100',
    testIdAttribute: 'data-test',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
  webServer: {
    command: 'node server.js',
    url: 'http://127.0.0.1:3100/api/health',
    env: { PORT: '3100', DEMO_MODE: 'clean', VERCEL: '' },
    reuseExistingServer: false,
    timeout: 10000,
  },
});
