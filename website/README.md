# CtrlX website

Static marketing site built with [Astro](https://astro.build). Four pages
(index, docs, pricing, security) sharing one design system.

## Develop

```bash
cd website
npm install
npm run dev        # http://localhost:4321
npm run build      # → dist/
npm run preview    # serve dist/ locally
```

## Structure

- `src/styles/modernist.css` — the design system: tokens + component classes,
  the single source of truth for the site's look. Page-specific one-off styles
  stay inline in the page.
- `src/layouts/Base.astro` — html head (meta/OG/fonts) + Nav + Footer around
  every page.
- `src/components/` — `Nav` (active link from the URL), `Footer`, `Hero`
  (eyebrow/h1/lede page header).
- `src/pages/` — one `.astro` file per page. Content-as-data lives in each
  page's frontmatter (`const faqs = […]`) and renders with `.map()`.
- `public/` — fonts (self-hosted Archivo), logo, favicon, robots.txt.

## Add a page

1. Create `src/pages/<name>.astro`, wrap content in `Base`, start with `Hero`.
2. Use `container` / `section` / design-system classes from `modernist.css`;
   promote any style you repeat a second time into a class there.
3. Add the page to the `links` array in `src/components/Nav.astro` (and the
   Footer) if it belongs in the chrome.

## Build for deployment

```bash
CTRLX_SITE_URL=https://your-domain.example npm run build
```

The repository does not assume a production domain or deployment host. Publish
`dist/` using infrastructure you control, and set `CTRLX_SITE_URL` to its public
origin so canonical and sitemap URLs are correct.

## History

Ported 2026-07 from four self-contained artifact-export bundles (see
`docs/superpowers/specs/2026-07-20-website-structure-design.md`). The original
bundles live in git history under `deploy/` (removed after the port);
`scripts/extract-website-originals.py` (also removed, in history) decodes them.
