import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "tests",
  testMatch: "*.spec.ts",
  webServer: { command: "npx --yes serve out -l 4312", url: "http://localhost:4312", reuseExistingServer: false, timeout: 120000 },
  use: { baseURL: "http://localhost:4312" },
});
