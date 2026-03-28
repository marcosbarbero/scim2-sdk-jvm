import { test, expect } from '@playwright/test';

// Generate a unique suffix so parallel runs do not collide
const unique = () => Date.now().toString(36);

test.describe('Group Management', () => {

  test('should display the groups list page', async ({ page }) => {
    await page.goto('/groups');

    await expect(page.getByRole('heading', { name: 'Groups' })).toBeVisible();
    await expect(page.getByRole('button', { name: '+ New Group' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Display Name' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Members' })).toBeVisible();
  });

  test('should create a new group', async ({ page }) => {
    const groupName = `pw-group-${unique()}`;

    await page.goto('/groups');
    await page.getByRole('button', { name: '+ New Group' }).click();
    await page.waitForURL('**/groups/new');

    // Fill the group form
    await page.getByLabel('Display Name').fill(groupName);

    // Submit
    await page.getByRole('button', { name: 'Create' }).click();

    // Should redirect to the groups list
    await page.waitForURL('**/groups');

    // The new group should appear in the table
    await expect(page.getByRole('cell', { name: groupName })).toBeVisible();
  });

  test('should delete a group', async ({ page }) => {
    // Create a group to delete
    const groupName = `pw-grpdel-${unique()}`;
    await page.goto('/groups/new');
    await page.getByLabel('Display Name').fill(groupName);
    await page.getByRole('button', { name: 'Create' }).click();
    await page.waitForURL('**/groups');
    await expect(page.getByRole('cell', { name: groupName })).toBeVisible();

    // Accept the confirmation dialog before clicking delete
    page.on('dialog', (dialog) => dialog.accept());

    // Find the row containing our group and click its Delete button
    const row = page.getByRole('row').filter({ hasText: groupName });
    await row.getByRole('button', { name: 'Delete' }).click();

    // The group should no longer appear
    await expect(page.getByRole('cell', { name: groupName })).toBeHidden({ timeout: 10000 });
  });
});
