# Offspend website

Static landing page and privacy policy for Offspend, built with Next.js (static export), Tailwind and shadcn/ui. Design rules are in `DESIGN.md`.

- `npm run dev` runs it locally. `npm run build` writes the site to `out/`.
- `npm test` runs the config checks and the browser tests (build first).
- Settings you change by hand live in `lib/config.ts`: the Google Play URL (empty shows the placeholder state), the contact address and the repo link.
- The privacy policy page is built from `../docs/privacy-policy.md`, so there is one copy to edit.
- Hosting: Cloudflare Pages, root directory `site`, build command `npm ci && npm run build`, output directory `out`, `NODE_VERSION=22`.
