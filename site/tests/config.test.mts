import assert from "node:assert/strict";
import { test } from "node:test";
import { CONTACT_EMAIL, isValidPlayUrl, PLAY_URL } from "../lib/config.ts";

test("the shipped Play URL is empty or a real Play listing", () => assert.ok(isValidPlayUrl(PLAY_URL)));
test("rejects anything that is not a Google Play listing", () => {
  for (const bad of ["http://play.google.com/store/apps/details?id=a.b", "https://evil.example/store/apps/details?id=a.b", "https://play.google.com/store/apps/details?id=a.b&x=<script>", "play.google.com"]) {
    assert.equal(isValidPlayUrl(bad), false, bad);
  }
  assert.equal(isValidPlayUrl("https://play.google.com/store/apps/details?id=app.smsexpense"), true);
});
test("the contact address is the Offspend one", () => assert.equal(CONTACT_EMAIL, "founder@offspend.com"));
test("a live Play URL needs Google's official badge file in public/", async () => {
  if (!PLAY_URL) return;
  const { existsSync } = await import("node:fs");
  assert.ok(existsSync(new URL("../public/google-play-badge.png", import.meta.url)));
});

import { renderPolicy } from "../lib/policy.ts";
test("policy rendering shows raw HTML as text and drops unsafe links", () => {
  const out = renderPolicy("<img src=x onerror=alert(1)>\n\n[bad](javascript:alert(1)) [ok](https://offspend.com) [mail](mailto:a@b.c)");
  assert.ok(!out.includes("<img"));
  assert.ok(out.includes("&lt;img"));
  assert.ok(!out.includes("javascript:"));
  assert.ok(out.includes('href="https://offspend.com"') && out.includes('href="mailto:a@b.c"'));
});
