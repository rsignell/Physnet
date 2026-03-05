# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Physnet is a physics education website hosting interactive problems and module text. Built with **Astro** (static site), deployed to GitHub Pages from the `dist/` build output.

## Development Commands

```bash
npm run dev      # local dev server at localhost:4321
npm run build    # build to dist/ (deployed by CI)
npm run preview  # preview the built site
```

## Repository Structure

- **`src/pages/`** — Astro pages. Routes mirror the file structure.
  - `index.astro` — Home page with full module catalog
  - `modules/[id].astro` — Individual module pages (generated for all modules in catalog)
  - `interactive/index.astro` — Interactive tools hub
  - `interactive/force-diagrams/index.astro` — Force diagrams problem chooser
- **`src/layouts/Base.astro`** — HTML shell: nav, KaTeX CSS, footer
- **`src/styles/global.css`** — All site CSS (no framework)
- **`src/data/modules.js`** — Module catalog: `SUBJECTS` array (organized by subject area), `MODULE_MAP` (flat lookup), `PHYSNET_PDF_BASE` URL
- **`public/`** — Static assets copied as-is into `dist/`. The interactive problem HTML files live here.
  - `public/interactive/force-diagrams/problem1.html` — Working HTML5 force diagram exercise
- **`dist/`** — Build output, gitignored, deployed by CI
- **`OneBodyForceDiagrams/`** — Development history for the Java→HTML5 conversion; has its own `CLAUDE.md`
- **`pdfs/`** — Hand-converted MISN module HTML (m10.html is the reference)

## Deployment

`.github/workflows/pages.yml` builds Astro (`npm ci && npm run build`) then deploys `dist/` to GitHub Pages at `https://rsignell.github.io/Physnet/`.

The Astro `base` is set to `/Physnet` in `astro.config.mjs`. All internal links must be prefixed with `import.meta.env.BASE_URL` (available in every `.astro` file as `const b = import.meta.env.BASE_URL`).

## HTML5 Interactive Problems Architecture

Each problem in `public/interactive/force-diagrams/` is a self-contained HTML file with inline CSS and JS. The layout is a fixed CSS Grid with four panels:

| Panel | Position | Role |
|---|---|---|
| `#problemPanel` + `#problemCanvas` | top-left | Draws the physical scenario (static Canvas 2D) |
| `#messagePanel` | top-right | Instructions and feedback text |
| `#resultsPanel` | bottom-left | Shows correct force equations after completion |
| `#userPanel` | bottom-right | Student drawing area — stacked transparent `<canvas>` layers |

The `#userPanel` uses multiple absolutely-positioned `<canvas>` elements (z-indexed) so each force vector draws on its own layer. Mouse events capture drag gestures; each completed drag is a force vector checked against the correct answer with angular and length tolerances.

**12 problems total** (Java source in `OneBodyForceDiagrams/`):

| # | Java class | Scenario | Status |
|---|---|---|---|
| 1 | `HangingBall` | Ball on angled string | ✓ done |
| 0 | `HangingBall0` | Ball hanging vertically | todo |
| 2 | `PersonHorizS` | Person on floor, static | todo |
| 3–4 | `PersonHorizD/D2` | Person pushed horizontally | todo |
| 5–6 | `BoxInclineS/S2` | Box on inclined plane | todo |
| 7–8 | `PersonIncliS/D` | Person on incline | todo |
| 9–11 | `PulleyHorizSB/SP/SP4` | Pulley systems | todo |

To add a new problem: copy `problem1.html`, update the `drawProblem()` and `drawUserApparatus()` canvas drawing functions, update the `PROBLEM` data block (forceNames, truAnsX/Y, tailPos, scale, units), then mark it `available: true` in `src/pages/interactive/force-diagrams/index.astro`.

## Module Catalog

`src/data/modules.js` exports `SUBJECTS` (array of subject areas with module lists) and `MODULE_MAP` (flat object keyed by module ID). Each module entry:
```js
{ id: 'm10', title: 'One-Body Diagrams and Contact Forces', hasHtml: true, hasInteractive: true }
```
- `hasHtml: true` → links to `/modules/m10/` (Astro page with rendered LaTeX content)
- `hasInteractive: true` → shows Interactive badge linking to the force diagrams widget

## LaTeX Source Pipeline (planned)

The 287 module LaTeX sources are in Google Drive (`m1/`, `m2/`, ... folders). Each module folder contains:
- `mN-dat.tex` — metadata (title, authors, prerequisites, learning objectives)
- `mN-tx.tex` — main text content
- `mN-ps.tex` — problem set
- `mN-as.tex` — special assistance (hints)
- `mN-me.tex` — model exam
- `mNgrNN.eps` — figures (CorelDRAW → EPS)

Physnet uses a custom LaTeX style (`nphmods.sty`) with domain-specific macros. A Python preprocessor is needed to convert these to HTML before slotting content into the Astro module pages.
