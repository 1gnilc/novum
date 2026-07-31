// cspell:ignore Dfailsafe Dsurefire
import { defineConfig, devices } from 'playwright/test';

const backendUrl = 'http://127.0.0.1:3766';
const frontendUrl = 'http://127.0.0.1:5877';

export default defineConfig({
  expect: { timeout: 10_000 },
  forbidOnly: !!process.env.CI,
  fullyParallel: false,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  retries: process.env.CI ? 1 : 0,
  testDir: './tests/e2e',
  timeout: 45_000,
  use: {
    ...devices['Desktop Chrome'],
    baseURL: frontendUrl,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  webServer: [
    {
      command:
        'pnpm exec cross-env RUN_E2E_SERVER=true mvn --batch-mode --no-transfer-progress -f apps/server/pom.xml -pl :novum-bootstrap -am -Dit.test=E2eServerIT -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false verify',
      reuseExistingServer: false,
      timeout: 180_000,
      url: `${backendUrl}/api/sys/admin/user-info`,
    },
    {
      command: `pnpm exec cross-env E2E_API_TARGET=${backendUrl} pnpm --filter=@app/admin exec vite --mode development --host 127.0.0.1 --port 5877`,
      reuseExistingServer: false,
      timeout: 120_000,
      url: frontendUrl,
    },
  ],
  workers: 1,
});
