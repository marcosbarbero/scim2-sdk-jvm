import { test, expect } from '@playwright/test';

// Generate a unique suffix so parallel runs do not collide
const unique = () => Date.now().toString(36);

test.describe('User Management', () => {

  test('should display the users list page', async ({ page }) => {
    await page.goto('/users');

    // The heading and "New User" button should be visible
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible();
    await expect(page.getByRole('button', { name: '+ New User' })).toBeVisible();

    // The table header should include expected columns
    await expect(page.getByRole('columnheader', { name: 'Username' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Display Name' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Email' })).toBeVisible();
  });

  test('should create a new user', async ({ page }) => {
    const userName = `pw-user-${unique()}`;

    await page.goto('/users');
    await page.getByRole('button', { name: '+ New User' }).click();
    await page.waitForURL('**/users/new');

    // Fill out the user form
    await page.getByLabel('Username').fill(userName);
    await page.getByLabel('Display Name').fill(`Playwright Test ${userName}`);
    await page.getByLabel('Given Name').fill('Play');
    await page.getByLabel('Family Name').fill('Wright');
    await page.getByLabel('Email').fill(`${userName}@test.local`);

    // Submit
    await page.getByRole('button', { name: 'Create' }).click();

    // Should redirect to the user list
    await page.waitForURL('**/users');

    // The new user should appear in the table
    await expect(page.getByRole('cell', { name: userName })).toBeVisible();
  });

  test('should search and filter users', async ({ page }) => {
    // Create a user to search for
    const userName = `pw-filter-${unique()}`;
    await page.goto('/users/new');
    await page.getByLabel('Username').fill(userName);
    await page.getByLabel('Display Name').fill(`Filterable ${userName}`);
    await page.getByRole('button', { name: 'Create' }).click();
    await page.waitForURL('**/users');

    // Type a SCIM filter in the search box and click Search
    const searchInput = page.getByPlaceholder('Search users');
    await searchInput.fill(`userName eq "${userName}"`);
    await page.getByRole('button', { name: 'Search' }).click();

    // Wait for the filtered result to load
    await expect(page.getByText('1 user(s) found')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('cell', { name: userName })).toBeVisible();
  });

  test('should delete a user', async ({ page }) => {
    // Create a user to delete
    const userName = `pw-del-${unique()}`;
    await page.goto('/users/new');
    await page.getByLabel('Username').fill(userName);
    await page.getByLabel('Display Name').fill(`Deletable ${userName}`);
    await page.getByRole('button', { name: 'Create' }).click();
    await page.waitForURL('**/users');
    await expect(page.getByRole('cell', { name: userName })).toBeVisible();

    // Accept the confirmation dialog before clicking delete
    page.on('dialog', (dialog) => dialog.accept());

    // Find the row containing our user and click its Delete button
    const row = page.getByRole('row').filter({ hasText: userName });
    await row.getByRole('button', { name: 'Delete' }).click();

    // The user should no longer appear
    await expect(page.getByRole('cell', { name: userName })).toBeHidden({ timeout: 10000 });
  });
});
