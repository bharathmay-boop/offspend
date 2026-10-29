# DESIGN.md: Offspend website

> Calm enough to trust with your bank texts, alive enough to show exactly what happens to one.

Status: **draft for the owner's confirmation.** No site code exists yet. The plan behind this file was audited by GPT-6 Astra (`openai/gpt-6-astra`, #2 on Arena WebDev, Oct 2026); what was taken and what was left is in section 10.

## 1. Visual Theme & Atmosphere

**Style**: Quiet product editorial. Warm off-white surfaces, one green, large friendly display type, real app screenshots. The same palette and fonts as the Android app, so the site and the app feel like one product.
**Keywords**: calm, plain, private, precise, warm, unhurried, honest, local.
**Tone**: Plain and reassuring, like a careful friend explaining what the app does. NOT a fintech sales page, NOT neon, NOT "AI".
**Feel**: A tidy ledger book that quietly fills itself in.

**Interaction Tier**: L3 (immersive), chosen by the owner. Applied with restraint: one pinned scroll story and one tap-to-file demo in the hero carry the whole tier. The skill's effect quotas are a ceiling, not a target, and nothing is added to meet a count. Reason: this is a money app asking for SMS access, so trust and speed on cheap phones beat spectacle.
**Dependencies**: GSAP + ScrollTrigger, loaded only on viewports that pin the story (see section 7). No Lenis, no WebGL, no custom cursor.

## 2. Color Palette & Roles

Taken from the app's theme (`Color.kt`) so the two match. Light by default; dark follows the system.

```css
:root {
  /* Surfaces */
  --bg: #F6FAF7;                 /* page */
  --surface: #FFFFFF;            /* cards, phone screen */
  --surface-alt: #EAEFEB;        /* alternate sections, code-like blocks */
  --surface-hover: #E4EAE6;
  --surface-inverse: #2C322F;    /* the one dark band (privacy) */

  /* Borders */
  --border: #BFC9C4;
  --border-hover: #6F7975;

  /* Text */
  --text: #171D1A;               /* 16:1 on --bg */
  --text-secondary: #3F4945;     /* 9:1 on --bg */
  --text-tertiary: #5A6460;      /* 5.7:1 on --bg, labels only at 14px+ bold or 16px+ */
  --text-on-inverse: #EDF2EE;

  /* Accent (the single green) */
  --accent: #0D6B58;             /* 6.2:1 on --bg, white text on it is 6.2:1 */
  --accent-hover: #0A5546;
  --accent-container: #B8EFDC;   /* mint chips, selected states */
  --on-accent-container: #00201A;

  /* Money, as in the app */
  --credit: #1B7F3B;
  --warm: #FFDF9E;               /* tertiary container, used once for "example" tags */
  --on-warm: #261A00;

  /* RGB helpers for rgba() */
  --bg-rgb: 246, 250, 247;
  --accent-rgb: 13, 107, 88;
  --text-rgb: 23, 29, 26;

  /* Semantic */
  --success: #1B7F3B;
  --error: #BA1A1A;
  --warning: #7A5900;

  color-scheme: light dark;
}
@media (prefers-color-scheme: dark) {
  :root {
    --bg: #0F1512; --surface: #171D1A; --surface-alt: #1B211E; --surface-hover: #252B28; --surface-inverse: #0A0F0D;
    --border: #3F4945; --border-hover: #89938E;
    --text: #DEE4E0; --text-secondary: #BFC9C4; --text-tertiary: #9AA5A0; --text-on-inverse: #DEE4E0;
    --accent: #8CD5BD; --accent-hover: #A8F2D9; --accent-container: #005142; --on-accent-container: #A8F2D9;
    --credit: #7BDA8F; --warm: #5C4300; --on-warm: #FFDF9E;
    --bg-rgb: 15, 21, 18; --accent-rgb: 140, 213, 189; --text-rgb: 222, 228, 224;
  }
}
```

**Color Rules:**
- Every color comes from a variable. No hex in components. (shadcn tokens in `globals.css` are mapped to these.)
- One accent per section. Mint container is a fill, never a text color.
- Contrast is checked in both themes for body text, labels, focus rings, the mint chips and the status label. Targets: body 7:1 where possible, never under 4.5:1.
- No gradients on text. A single soft radial wash behind the hero phone is allowed, using `--accent-rgb` at 8% opacity.

## 3. Typography Rules

Self-hosted. The two files are already bundled in the app (SIL OFL): `bricolage.ttf`, `inter.ttf`. They are converted to subsetted WOFF2 (Latin, the digits, ₹ and the currency signs) at build time. No Google Fonts request.

```css
@font-face { font-family: "Bricolage Grotesque"; src: url("/fonts/bricolage-latin.woff2") format("woff2"); font-weight: 600 800; font-display: swap; }
@font-face { font-family: "Inter"; src: url("/fonts/inter-latin.woff2") format("woff2"); font-weight: 400 700; font-display: swap; }
:root {
  --font-display: "Bricolage Grotesque", "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --font-body: "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --font-mono: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
}
```

| Role | Font | Size | Weight | Line Height | Letter Spacing |
|------|------|------|--------|-------------|----------------|
| Hero H1 | display | clamp(2.5rem, 7vw, 4.75rem) | 800 | 1.04 | -0.02em |
| Section H2 | display | clamp(1.875rem, 4vw, 3rem) | 700 | 1.1 | -0.015em |
| H3 | display | 1.375rem | 700 | 1.25 | -0.005em |
| Lead / subhead | body | clamp(1.125rem, 2vw, 1.375rem) | 400 | 1.55 | none |
| Body | body | 1.0625rem (17px) | 400 | 1.65 | none |
| Label / eyebrow | body | 0.8125rem | 600 | 1.3 | 0.08em, uppercase |
| Mock (notification text) | body | as the app shows it | 400-600 | 1.35 | none |
| Mono (the config note, if shown) | mono | 0.875rem | 400 | 1.5 | none |

**Typography Rules:**
- Headings weight 700 or 800 only. Body never below 17px on mobile.
- Numbers use tabular figures (`font-variant-numeric: tabular-nums`) wherever an amount is shown.
- **NEVER use**: Arial/Helvetica as the first choice, Roboto as the display face, any script or novelty face, all-caps for anything longer than a label.

**Text Decoration:** none. No gradient text, no text shadow (calm, finance, trust). Emphasis is weight and one accent color word at most per heading.

## 4. Component Stylings

The component layer is **shadcn/ui** (Button, Accordion, Card, Badge, Sheet for the mobile menu) plus blocks from 21st.dev stripped back to what is needed. Variants below are set through the tokens, not by editing component source.

### Buttons
```css
.btn { display: inline-flex; align-items: center; justify-content: center; gap: .5rem; min-height: 48px; padding: 0 1.5rem; border-radius: 999px; font: 600 1rem/1 var(--font-body); border: 1px solid transparent; cursor: pointer; transition: background-color .18s, border-color .18s, transform .18s; }
.btn-primary { background: var(--accent); color: var(--bg); }
.btn-primary:hover { background: var(--accent-hover); transform: translateY(-1px); }
.btn-primary:active { transform: translateY(0); }
.btn-primary:focus-visible { outline: 3px solid var(--accent); outline-offset: 3px; }
.btn-primary:disabled, .btn-primary[aria-disabled="true"] { background: var(--surface-alt); color: var(--text-secondary); border-color: var(--border); cursor: not-allowed; transform: none; }
.btn-ghost { background: transparent; color: var(--accent); border-color: var(--border); }
.btn-ghost:hover { background: var(--accent-container); color: var(--on-accent-container); border-color: transparent; }
.btn-ghost:active { background: var(--accent-container); filter: brightness(.96); }
.btn-ghost:focus-visible { outline: 3px solid var(--accent); outline-offset: 3px; }
.btn-ghost:disabled { color: var(--text-tertiary); border-color: var(--border); cursor: not-allowed; }
```

### Release block (the Google Play placeholder, config-driven)
One component, used in the hero and at the bottom. It reads `site/lib/config.ts`.
```
PLAY_URL = ""        // empty: placeholder state.  "https://play.google.com/store/apps/details?id=..." : live state.
CONTACT_EMAIL = ""   // empty: no contact link is rendered at all.
```
- **Placeholder state (today):** a status line "In friends-and-family testing on Google Play. A public download link is not available yet." Then a badge-shaped box, dashed `--border`, labelled "Google Play badge goes here", not focusable, `aria-hidden` with the status line as the real text. Then a square dashed box labelled "QR code appears here". Both boxes are clearly placeholders, never styled like working controls.
- **Live state (when `PLAY_URL` is set and valid):** the official, unmodified Google Play badge image (from Google's badge generator, added to `public/`) linking to `PLAY_URL`, plus a QR code generated at build time from the same URL (local `qrcode` package, black on white, a 4-module quiet zone, no third-party service), with the link shown beside it.
- The badge is never resized below Google's minimum or recolored. In tight places a plain text link is used instead.
- A build step validates the URL: empty or a valid `https://play.google.com/...` passes, anything else fails the build.
- Hero primary CTA is **"See how it works"** (a normal button that scrolls to the story). The release block sits beside it in the hero and again in the final section; the QR shows only on viewports with a pointer and 900px or wider, and always next to the link.

### Cards
```css
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 20px; padding: 1.75rem; transition: border-color .18s, background-color .18s; }
.card:hover { border-color: var(--border-hover); }
.card:focus-within { border-color: var(--accent); }
.card-static:hover { border-color: var(--border); }
```
Cards that are not links do not hover. Only interactive cards respond.

### Navigation
```css
.nav { position: sticky; top: 0; z-index: 40; background: rgba(var(--bg-rgb), .92); border-bottom: 1px solid transparent; transition: border-color .18s, box-shadow .18s; }
.nav[data-scrolled="true"] { border-bottom-color: var(--border); box-shadow: 0 1px 0 rgba(var(--text-rgb), .04); }
.nav a { min-height: 44px; display: inline-flex; align-items: center; color: var(--text-secondary); }
.nav a:hover { color: var(--text); } .nav a:focus-visible { outline: 3px solid var(--accent); outline-offset: 2px; border-radius: 8px; }
.nav a[aria-current="true"] { color: var(--accent); }
```
No `backdrop-filter`. The nav shows the wordmark, How it works, Privacy, FAQ and, on desktop, a ghost "Release status" link. A skip link is the first focusable element. On mobile the links sit in a shadcn Sheet.

### Links
```css
a.link { color: var(--accent); text-decoration: underline; text-underline-offset: 3px; text-decoration-thickness: 1px; transition: text-decoration-thickness .15s, color .15s; }
a.link:hover { text-decoration-thickness: 2px; color: var(--accent-hover); }
a.link:focus-visible { outline: 3px solid var(--accent); outline-offset: 2px; border-radius: 4px; }
```

### Tags / Badges
```css
.tag { display: inline-block; padding: .25rem .75rem; border-radius: 999px; font: 600 .8125rem/1.3 var(--font-body); background: var(--accent-container); color: var(--on-accent-container); }
.tag-example { background: var(--warm); color: var(--on-warm); }
```

### Phone frame and notification mock
The phone is a plain rounded rectangle (not a photographed device), `--surface` screen, 9:19.5. The notification copies the app's real text: "−₹320.00 · California Burrito", "Food · Tap to change", buttons **Yes, Food** and **Add note**. A small tag, "Example transaction", sits on it. It uses invented sample data only.
- **The one touch of delight:** the hero notification is tappable. Tapping **Yes, Food** (a real `<button>`) turns the text to "Filed under Food" with a small check, and shows "Tap again to undo". It teaches the product in one second and costs one state variable.
- The mock is decorative to assistive tech except for that button; the same story is written in plain text next to it.

### FAQ (shadcn Accordion)
Question is an `<h3>` containing a button with `aria-expanded` and `aria-controls`. Answers are readable without JavaScript (all open when JS is off). Focus ring as buttons.

## 5. Layout Principles

**Container:**
- Max width: 1120px; side padding 20px mobile, 32px tablet, 40px desktop. Narrow text column: 680px.
- Hero and story use a two-column grid on desktop (copy left, phone right).

**Spacing Scale (4px base):** 4, 8, 12, 16, 24, 32, 48, 64, 96.
- Section padding: 64px mobile, 96px desktop.
- Component gap: 16 to 24px. Card padding: 28px.

**Grid:**
```css
.grid-2 { display: grid; gap: 48px; grid-template-columns: 1fr; }
@media (min-width: 900px) { .grid-2 { grid-template-columns: 1.05fr .95fr; align-items: center; } }
.grid-3 { display: grid; gap: 20px; grid-template-columns: 1fr; }
@media (min-width: 760px) { .grid-3 { grid-template-columns: repeat(3, 1fr); } }
```

**Page order (set by the audit):**
1. Hero: headline, qualified subhead, trust line, "See how it works", release block, phone with the tappable notification.
2. **Privacy first, compact** (the one dark band, `--surface-inverse`): what Offspend processes, what stays on the phone, what it keeps.
3. How it works: the story.
4. Three benefits.
5. FAQ.
6. Release block (final).
7. Footer.
- `/privacy-policy`: the policy rendered from `docs/privacy-policy.md` at build time, one source of truth.

**Copy rules (verified claims only):**
- Headline: **Your bank already texts you. Offspend files it.**
- Subhead: *Turn bank and UPI transaction SMS into expenses on your Android phone. Offspend suggests a category; you confirm it with one tap.*
- Trust line under it: **No bank login. No account. No internet permission.**
- Say "skips OTPs and personal messages" and "does not store message text". Never "never reads". Distinguish the SMS permission Android asks for from what Offspend chooses to process and keep.
- Keep: "Works with any currency", with "bank message formats vary, so a few may not be read" next to it. "No ads" is true (Play form: ads none). Dark mode follows the system (true, Settings has the choice).
- Android only, said early. The iPhone answer is in the FAQ (iOS does not let apps read SMS).
- Pricing: "Free to try during the test. Public pricing is not set yet." Nothing invented.
- Delete your data: uninstall, or clear storage in Android settings (from the policy).
- The "no internet permission" proof links to the manifest only while `REPO_URL` in `site/lib/config.ts` is set (the repo is public today; the owner plans to make it private, which empties it and hides the link and the footer repo link). Not claimed from a Play page that does not exist yet.
- Website privacy is separate from app privacy: the site sets no cookies and loads no third-party scripts, fonts or embeds; Cloudflare Pages is the host and Cloudflare's own logs are theirs.

## 6. Depth & Elevation

| Level | Treatment | Use |
|-------|-----------|-----|
| Flat | none, 1px `--border` | cards, FAQ, nav at rest |
| Subtle | `0 1px 2px rgba(var(--text-rgb), .06)` | nav when scrolled |
| Phone | `0 24px 48px -24px rgba(var(--text-rgb), .28)` | the phone only |
| Inverse band | solid `--surface-inverse`, no shadow | privacy section |

No glow, no colored shadows, no blur. The phone shadow is static (never animated).

## 7. Animation & Interaction

**Motion Philosophy**: Show one real transaction clearly, then get out of the way. Only `transform` and `opacity` move. Everything is readable with all motion off.
**Tier**: L3, delivered by two things only: the hero load + tap-to-file demo, and the pinned story.

### Dependencies
```html
<!-- bundled by npm, not a CDN. GSAP and ScrollTrigger are dynamically imported by the story component only. -->
gsap@3, gsap/ScrollTrigger
```
Budget: initial JavaScript at most 100 KB compressed (the story code is loaded later); LCP at most 2.5 s, INP at most 200 ms, CLS at most 0.1, checked on a real low-end Android phone as well as Lighthouse.

### Base Setup
```js
// story.tsx (client component, loaded with next/dynamic only when eligible)
const eligible = matchMedia("(min-width: 1024px) and (min-height: 700px) and (prefers-reduced-motion: no-preference) and (hover: hover)").matches;
if (eligible) {
  const { gsap } = await import("gsap");
  const { ScrollTrigger } = await import("gsap/ScrollTrigger");
  gsap.registerPlugin(ScrollTrigger);
  // one pin only: the phone, while four readable steps scroll past on the left
  ScrollTrigger.create({ trigger: ".story", pin: ".story-phone", start: "top 12%", end: "+=220%", scrub: 0.6 });
}
```
- The four steps are normal HTML in normal document order: 1) an SMS arrives, 2) Offspend reads it on the phone and suggests a category, 3) you tap **Yes, Food** or change it, 4) it is filed and the month adds up (shown with a real app screenshot, not an invented chart).
- On phones, tablets and short screens, and whenever reduced motion is on or JavaScript fails: no pin, no GSAP download, the four steps stack, each with its own static screenshot. This is the default layout, not a fallback.
- The pin is at most two extra viewport heights. The pinned phone never covers text or a focused control.

### Entrance Animation
```css
@keyframes rise { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: none; } }
.enter { animation: rise .6s cubic-bezier(.22,.8,.3,1) both; }
.enter-2 { animation-delay: .08s; } .enter-3 { animation-delay: .16s; }
```
Used once, on the hero only: the phone rises, the notification drops in 250 ms later. The hero text is visible immediately (no opacity-0 start), so LCP is never delayed. Space for the phone is reserved to avoid layout shift.

### Scroll Behavior
Native scrolling. `scroll-behavior: smooth` for in-page anchors, off under reduced motion. Anchors have `scroll-margin-top` equal to the nav height. No scroll-jacking, no Lenis, no parallax, no heading reveals.

### Hover & Focus States
Every interactive element has hover and `:focus-visible` (3px accent outline, 3px offset). Touch devices get no hover-only information.

### Special Effects
None beyond the above. Cut on purpose: pointer spotlight cards, heading reveals, WebGL, custom cursor, marquee.

### Reduced Motion
```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; transition: none !important; scroll-behavior: auto !important; }
  .enter { opacity: 1; transform: none; }
}
```
Plus: GSAP is not loaded, the story stacks, and the tap-to-file demo still works (it is an interaction, not an animation) but swaps text with no transition.

## 8. Do's and Don'ts

### Do
- Use the app's real screenshots and real notification text; label invented sample data "Example transaction".
- Put the privacy answer next to the first promise, then show the story.
- Write every claim so it is true of today's build, and keep a source for each (the policy, `PLAY_RELEASE.md`, the manifest).
- Make every state readable without JavaScript and without motion.
- Keep the placeholder honest: say what the status is, and make placeholders look like placeholders.
- Test at 320px, 200% text size and 400% zoom, and with TalkBack.
- Keep one accent and plenty of space.

### Don't
- ❌ Don't style a placeholder like a working button or a scannable QR code.
- ❌ Don't write "never reads OTPs" or "100% safe". Say what it skips and what it stores.
- ❌ Don't invent a waitlist, an invite form, ratings, download counts, testimonials or press logos.
- ❌ Don't add analytics, cookies, third-party fonts, embeds or QR services. (Analytics may come later through #164, and then the privacy page changes the same day.)
- ❌ Don't pin the story on small or short screens, or load GSAP where it does not run.
- ❌ Don't animate layout, blur, filters or large shadows.
- ❌ Don't use `backdrop-filter`, gradient text, glow, neon, or a dark-pattern CTA.
- ❌ Don't edit or recolor the Google Play badge, or shrink it below Google's minimum.
- ❌ Don't show an empty contact link; omit it until `CONTACT_EMAIL` is set.
- ❌ Don't use emoji as icons. Use lucide-react or inline SVG.

## 9. Responsive Behavior

**Breakpoints:**
| Name | Width | Key Changes |
|------|-------|-------------|
| Desktop | 1024px and up | two-column hero and story, pin enabled (also needs height 700px+ and hover), QR shown |
| Tablet | 640 to 1023px | two columns where they fit, story stacked, no pin, no QR |
| Mobile | under 640px | one column, 17px body, nav in a Sheet, release block full width, story stacked with its own screenshots |

**Touch Targets:** minimum 44 by 44px; spacing between adjacent targets at least 8px.
**Collapsing Strategy:** grids go to one column; the phone sits below the hero copy and keeps a reserved aspect box; nothing scrolls sideways at 320px; long words wrap.

## 10. Audit applied (GPT-6 Astra, Oct 2026)

Taken: privacy moved beside the first promise and ahead of the story; claims cut back to what is verified; honest release status instead of dead install buttons (while keeping the placeholders you asked for, as clearly marked boxes); hero subhead qualifies "files it" with the one-tap confirm; story rebuilt as readable steps first with a desktop-only pin; secondary motion cut (spotlight, heading reveals); a performance budget and real-phone check; accessibility behaviour specified; FAQ rewritten around real questions; SEO and share metadata; a longer release checklist (both release states, base path, JS off, no outside requests).
Left or changed: the audit would drop the QR block entirely until the URL exists. It stays as one clearly marked placeholder because you asked for it. "Any currency" stays, with the caveat next to it. The bento of six cards became three benefits.

**Metadata:** title "Offspend: expense tracking from bank SMS for Android"; description "Turn bank and UPI transaction SMS into expenses on your phone. Confirm categories with one tap. No account, no server, no internet permission."; Open Graph and Twitter cards with a static 1200 by 630 image; canonical, sitemap and robots built from the real origin and base path; `SoftwareApplication` structured data with only known facts (no price, rating or download URL); no placeholder URL is ever written into metadata.

**Release checks (all must pass before deploy):** the page builds; both release states render and the QR target equals `PLAY_URL`; no horizontal overflow at 320 and 360px; keyboard-only run through the page; reduced motion shows no animation; the page is fully readable with JavaScript off; `/privacy-policy`, deep links and assets resolve at the domain root (no base path); no request leaves the origin; heading order and landmarks are valid; contrast holds in both themes.

*Credits: none required. No third-party motion library code is used.*


## Hosting (decided 2026-10-29)

- Host: Cloudflare Pages (free), custom domain offspend.com, static export from `site/` (build `npm run build`, output `site/out`). No base path.
- The privacy policy is served at `https://offspend.com/privacy-policy`, so it keeps working if the repo is made private (GitHub Pages stops on a private free-plan repo). The old GitHub Pages copy is retired once the domain is live. Paste the offspend.com address into the Play form, not the github.io one.
- Repo renamed to `bharathmay-boop/offspend` on 2026-10-29. Visibility is unchanged (public) until the owner decides.
- The site is a landing page for the Android app on Google Play (Android only), not an App Store page.
