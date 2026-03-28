import { test as setup, expect } from '@playwright/test';
import { mkdir } from 'node:fs/promises';

setup('authenticate', async ({ page }) => {
  // Ensure the .auth directory exists
  await mkdir('.auth', { recursive: true });

  // Navigate to the app -- Keycloak login is triggered via keycloak-js
  await page.goto('/');

  // Click the "Log In with Keycloak" button rendered by the React app
  await page.getByRole('button', { name: 'Log In with Keycloak' }).click();

  // Wait for the Keycloak login page to load
  await page.waitForURL('**/realms/**');

  // Fill in credentials on the Keycloak login form
  await page.locator('#username').fill('admin');
  await page.locator('#password').fill('admin');
  await page.locator('#kc-login').click();

  // Wait for redirect back to the app (authenticated state shows the nav bar)
  await page.waitForURL('**/users');
  await expect(page.getByText('SCIM Sample')).toBeVisible();

  // Save authentication state so other tests can reuse the session
  await page.context().storageState({ path: '.auth/state.json' });
});
