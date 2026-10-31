import { expect, test } from "@playwright/test";

const pages = ["/", "/privacy-policy"];

for (const width of [320, 360]) {
  for (const path of pages) {
    test(`no horizontal overflow at ${width}px on ${path}`, async ({ page }) => {
      await page.setViewportSize({ width, height: 800 });
      await page.goto(path);
      const over = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
      expect(over).toBeLessThanOrEqual(0);
    });
  }
}

test("placeholder state: no Play link, marked placeholders, honest status", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator('a[href*="play.google.com"]')).toHaveCount(0);
  await expect(page.getByText("A public download link is not available yet.").first()).toBeVisible();
  await expect(page.getByText("Google Play badge goes here").first()).toBeVisible();
});

test("tapping Yes, Food files the example payment and can be undone", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Yes, Food" }).click();
  await expect(page.locator("#top").getByText("Filed under Food")).toBeVisible();
  await page.getByRole("button", { name: "Tap again to undo" }).click();
  await expect(page.getByRole("button", { name: "Yes, Food" })).toBeVisible();
});

test.describe("JavaScript off", () => {
  test.use({ javaScriptEnabled: false });
  test("FAQ answers are readable and the accordion is replaced by a plain list", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator("noscript dt")).toHaveCount(7);
    expect(await page.locator("noscript dd").nth(3).textContent()).toContain("iOS does not let apps read text messages");
    await expect(page.locator(".faq-acc")).toBeHidden();
  });
});

test("privacy policy is served at /privacy-policy and names Offspend", async ({ page }) => {
  await page.goto("/privacy-policy");
  await expect(page.getByRole("heading", { level: 1, name: "Privacy policy" })).toBeVisible();
  await expect(page.getByText("Offspend helps you track what you spend")).toBeVisible();
});

test("no request leaves the origin", async ({ page }) => {
  const external: string[] = [];
  page.on("request", (r) => { if (!r.url().startsWith("http://localhost:4312") && !r.url().startsWith("data:")) external.push(r.url()); });
  await page.goto("/");
  await page.waitForLoadState("networkidle");
  expect(external).toEqual([]);
});

test.describe("story with reduced motion", () => {
  test.use({ viewport: { width: 1280, height: 800 }, reducedMotion: "reduce" });
  test("no pinned phone; each step shows its own phone", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator(".story-sticky")).toBeHidden();
    await expect(page.locator(".story-inline").first()).toBeVisible();
  });
});

test.describe("story on a big screen", () => {
  test.use({ viewport: { width: 1280, height: 800 } });
  test("one phone is pinned and the inline phones are hidden", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator(".story-sticky")).toBeVisible();
    await expect(page.locator(".story-inline").first()).toBeHidden();
  });
});

test("every page has one h1, a main landmark and a skip link", async ({ page }) => {
  for (const path of pages) {
    await page.goto(path);
    await expect(page.locator("h1")).toHaveCount(1);
    await expect(page.locator("main")).toHaveCount(1);
    await expect(page.getByRole("link", { name: "Skip to content" })).toHaveCount(1);
  }
});

test.describe("mobile menu", () => {
  test.use({ viewport: { width: 360, height: 780 } });
  test("opens, traps focus inside, and returns focus to the button on Escape", async ({ page }) => {
    await page.goto("/");
    const trigger = page.getByRole("button", { name: "Open menu" });
    await trigger.click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    for (let i = 0; i < 3; i++) await page.keyboard.press("Tab");
    expect(await page.evaluate(() => !!document.activeElement?.closest("[role=dialog]"))).toBe(true);
    await page.keyboard.press("Escape");
    await expect(dialog).toBeHidden();
    await expect(trigger).toBeFocused();
  });
});

test("the live region around the demo is always in the page", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator("#top [aria-live=polite]")).toHaveCount(1);
  await page.getByRole("button", { name: "Yes, Food" }).click();
  await expect(page.locator("#top [aria-live=polite]")).toHaveCount(1);
});
