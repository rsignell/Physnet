# Physnet

A physics-education website hosting the [MISN](https://www.physnet.org/) module
library as rendered HTML, plus interactive problems. Built with
[Astro](https://astro.build/) (static output) and deployed to GitHub Pages at
**<https://rsignell.github.io/Physnet/>**.

## Development

```bash
npm install
npm run dev      # local dev server at http://localhost:4321
npm run build    # static build to dist/  (what CI deploys)
npm run preview  # serve the built dist/
```

Astro's `base` is `/Physnet`, so every internal link is prefixed with
`import.meta.env.BASE_URL` (`const b = import.meta.env.BASE_URL` in any `.astro`
file).

## Repository layout

| Path | Contents |
|---|---|
| `src/pages/` | Astro routes. `modules/[id].astro` generates one page per catalog module; `interactive/` is the tools hub. |
| `src/layouts/Base.astro` | HTML shell — nav, KaTeX CSS/JS, footer. |
| `src/content/modules/mNNN.html` | Converted module body fragments (output of the pipeline below). |
| `src/data/modules.js` | Module catalog: `SUBJECTS` (by subject area) + `MODULE_MAP` (flat lookup). |
| `src/styles/global.css` | All site CSS (no framework). |
| `public/modules/mNNN/figures/*.svg` | Converted module figures. |
| `public/interactive/` | Self-contained interactive problem HTML. |
| `scripts/tex_to_html.py` | The LaTeX → HTML converter. |
| `scripts/eps_to_svg.sh` | EPS/PS → SVG figure pipeline. |
| `scripts/smoke/` | QA tooling (`qa.py`, `batch.sh`, `cmp.py`). |
| `OneBodyForceDiagrams/` | Development history for the Java → HTML5 force-diagram conversion. |
| `pdfs/` | Hand-converted reference module HTML/PDF. |

Status: **~233 of 287 MISN modules converted.** The remainder have no
convertible source (supplement-only folders or empty directories).

---

## LaTeX → HTML conversion pipeline

The MISN modules are authored in LaTeX against a custom house style
(`nphmods.sty`) with several hundred domain-specific macros. There is **no
LaTeX engine** in the pipeline — a hand-written Python processor walks the
source and emits an HTML fragment, and math is rendered client-side by KaTeX.

### 1. Source acquisition

The 287 module sources live in Google Drive. Pull the whole tree with rclone:

```bash
rclone copy gdrive:<modules-folder> ~/physnet_src/LatexModsSource/
```

Layout is three levels deep (`m-0-400/m-0-420/m422/`) and files are CRLF.
Body-file naming is inconsistent — identifying the real body is most of the
work:

| Pattern | Meaning |
|---|---|
| `mNNN-tx.tex` | standard body (majority) |
| `mNNN-b.tex` | older Reif ("Berkeley Physics") format |
| `mNNN.tex` | all-in-one `\Module{}` driver that `\input`s the parts |
| `mNNN-tx.9_9`, `mNNN.6_7` | version-numbered snapshots |
| `mNNN-a.tex` … `-f.tex` | one file per section — module must be reassembled |
| `.bak`, `.org` | editing backups; `.org` uses `\rem{}` for `\revhist{}` |

Supplementary parts: `-dat` (title / author / learning objectives), `-ps`
(problem set), `-as` (special assistance / hints), `-me` (model exam).

### 2. The converter — `scripts/tex_to_html.py`

```bash
python3 scripts/tex_to_html.py <module_source_dir> <module_id> > src/content/modules/<module_id>.html
```

* **`convert_module(dir, id)`** — orchestrator. Reads `-dat` for metadata,
  converts the `-tx`/`-b` body, then `-ps`/`-as`/`-me`, and assembles one
  fragment: `<header>`, a learning-objectives section, the body, footnotes,
  and `<hr>`-separated supplement sections. A final pass localises figure
  references and drops `<img>` tags whose SVG does not exist (keeping the
  `<figcaption>` as a stub).

* **`PhysnetConverter._process(text)`** — the core recursive char-walker.
  Scans for `\`, reads the macro name, and dispatches to one of ~250
  handlers. Handlers consume `{…}` / `[…]` with `get_arg()` /
  `get_n_args()` / `skip_opt_arg()`, recurse into `_process()` for nested
  content, and emit HTML:
  * `\Sect` / `\SectTitle` → `<h2>`, `\SubSect*` → `<h3>` / `<h4>`
  * `\begin{itemize}` → `<ul>` (brace-depth-aware `\item` splitting)
  * `eqnarray` family → `\[\begin{aligned}…\end{aligned}\]`
  * `\TxtProb` / `\ProbNo` → `.problem` blocks; `\ProbAns` / `\Answer` refs
  * cross-references (`\TxtEqnChRef`, `\TxtSectChRef`, …) → styled `<span>`s
  * help markers (`\help{4}`) → `<sup>` anchors wired to `#help-N`
  * figure macros (`\FullFigure`, `\CaptionAfterLeftFigure`, …) →
    `<figure><img src="figures/mNNNgrXX.svg">`; `_fig_name()` strips the
    graphics extension and lowercases all-caps references

* **`convert_math(s)`** — a large, order-sensitive `re.sub` chain applied to
  every math span, translating MISN math macros to KaTeX-compatible TeX:
  `\vect{}` → `\vec{}`, `\dfrac` → `\frac`, `\degrees` → literal `°`,
  `\Grad` → `\nabla`, `\partiald{}{}` → `\frac{\partial…}{…}`,
  `{\rm x}` → `{\mathrm{x}}`; strips `\label` / `\nonumber` / `\protect`;
  rewrites bare `<` / `>` to `\lt` / `\gt` so the HTML parser doesn't eat
  them.

* Math is emitted as `\(…\)` / `\[…\]` and rendered in the browser by
  **KaTeX auto-render** (`throwOnError: false`), loaded in `Base.astro`.

### 3. Figures — `scripts/eps_to_svg.sh`

The CorelDRAW-exported EPS files embed Type-1 fonts with stub encodings (no
space glyph; Symbol letters named `/cNN`), so a direct Inkscape import
mangles spacing and Greek. Routing through a real PostScript interpreter
fixes it:

```
epstopdf     # Ghostscript — rasterises the embedded fonts into a PDF
  ↓
pdftocairo -svg   # poppler — every glyph becomes an outline <symbol>/<use>
```

Tools live in a conda env: `mamba create -n texsvg -c conda-forge texlive-core poppler`.
`.cdr` files with no EPS sibling are converted with
`soffice --headless --convert-to svg`. Output goes to
`public/modules/mNNN/figures/mNNNgrXX.svg`. Figures with no `.eps` / `.cdr`
source at all are left as caption-only stubs (documented gaps).

### 4. Assembly into the site

* Fragments land in `src/content/modules/mNNN.html`.
* `src/pages/modules/[id].astro` wraps each fragment in `Base.astro`.
* `src/data/modules.js` — set `hasHtml: true` for the module; verify the
  catalog title against the real MISN title.
* `npm run build` → `dist/` → GitHub Pages (via `.github/workflows/pages.yml`).

### 5. QA loop — `scripts/smoke/`

```bash
npm run preview &
python3 scripts/smoke/qa.py m1 m10 m37      # Playwright checks
```

`qa.py` loads each built page and flags: `.katex-error` count, raw `\macro`
or `\[` leaks in the rendered text, broken `<img>` elements, and figure
count. `batch.sh <ids>` re-converts a list from the source tree and reports
unknown-macro spans and output size. `cmp.py` builds an HTML-vs-PDF
side-by-side montage for visual spot checks.

The working loop: run QA → find a leaking macro or a mis-parse → add or fix
a handler in `tex_to_html.py` → re-convert → re-QA. Each clean module is
committed individually together with its figures.

---

## Interactive problems

Each problem under `public/interactive/force-diagrams/` is a self-contained
HTML file (inline CSS + JS) laid out as a four-panel CSS grid: the scenario
canvas, an instruction/feedback panel, a results panel, and a student
drawing area built from stacked transparent `<canvas>` layers. Drag
gestures are captured as force vectors and checked against the correct
answer with angular and length tolerances. See `CLAUDE.md` for the
step-by-step recipe to add a new problem.
