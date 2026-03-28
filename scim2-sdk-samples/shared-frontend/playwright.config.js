import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  retries: 1,
  use: {
    baseURL: 'http://localhost:5173',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'auth-setup',
      testMatch: /auth\.setup\.js/,
    },
    {
      name: 'e2e',
      dependencies: ['auth-setup'],
      use: {
        storageState: '.auth/state.json',
      },
    },
  ],
  webServer: undefined, // Tests run against an already-running stack
});
