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

All LaTeX source comes from **one** Google Drive folder:

```
Physnet/PhysnetWebSiteModuleFiles_9_3_2015_DNU/LatexModsSource/
folder id 1bm8aVOUj0KGYAxkuh3ZWcpsxbVKL6vSA
```

Pulled in full with rclone and verified against the Drive:

```bash
rclone copy  gdrive:Physnet/PhysnetWebSiteModuleFiles_9_3_2015_DNU/LatexModsSource \
             ~/physnet_src/LatexModsSource/
rclone check gdrive:.../LatexModsSource ~/physnet_src/LatexModsSource
#  → 10011 matching files, 0 differences   (~292 MiB)
```

**What the pulled tree contains** (`~/physnet_src/LatexModsSource/`):

| Path | Contents |
|---|---|
| `m-0-000/` … `m-0-700/` | module sources, three levels deep: `m-0-N00/m-0-NN0/mNNN/` — one folder per module (~290). Every file for a given module (e.g. all `m1*.tex` / `.org` / `.bak`) lives in that one `mNNN/` folder. |
| `p-0-000/` | "End Papers" — appendix tables (`p22.tex`, `p6a1.tex` … `p6a8.tex`). |
| `cdr_templates/`, `mod_templates/`, `modcover/`, `policies_handbook/` | house-style templates and boilerplate. |

Each `mNNN/` folder holds that module's `-tx.tex` / `-dat.tex` / `-ps.tex` /
`-as.tex` / `-me.tex` / `-tc.tex` (plus `.bak`, `.org`, and version-numbered
variants), its `.eps` / `.cdr` figure files, and LaTeX build cruft (`.aux`,
`.dvi`, `.log`, `.idx`, `.pdf`, `.ps`) that the converter ignores. Files are
CRLF (Windows).

**Not pulled** — the sibling folders under
`PhysnetWebSiteModuleFiles_9_3_2015_DNU/` contain no module `.tex` source:
`support_programs/` (compiled BASIC/Fortran/C++ and `.exe`), `PdfMods/`
(published PDF output), `SingleFiles/` (website index / license / revision-
history HTML). The rest of the Drive `Physnet/` folder is unrelated applet
projects.

> **Caveat:** the folder is tagged `_DNU` ("Do Not Use", dated 2015-09-03).
> It is the only LaTeX module-source set on the Drive — no newer copy
> exists — so it is what the pipeline uses, but the name suggests it may
> have been considered superseded. The ~53 modules with no convertible
> body genuinely have no source *in this tree*.

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

## Known limitations / outstanding issues

### Coverage
* **~234 of 287 modules converted.** The remaining ~53 have no convertible
  source in the archive — the module folder holds only supplementary files
  (`-as`, `-me`, `-ps`, `-tc`) or is empty. These cannot be recovered
  without new source.
* A handful of converted fragments are **thin partial conversions** (only a
  section or two survived in the archive): `m257`, `m405`, `m407`, `m404`,
  `m417`, `m418`.

### Partial-source modules
Several modules were reassembled from whatever section files the archive
contained; whole sections are missing:

| Module(s) | Missing |
|---|---|
| `m402`–`m417` (Reif adaptations) | scattered sections + figures |
| `m366` | sections A, B |
| `m419` | sections B–E, G–I |
| `m422` | everything except section F |
| `m427` | sections A–E |
| `m433` | section B |

### Figures
* **~149 broken figure references across ~33 modules.** These fragments
  were converted before the "drop `<img>` when the SVG is missing" pass was
  added, so they still emit `<img>` tags that 404 at build. Re-running each
  through the current `tex_to_html.py` replaces them with caption-only
  stubs; recovering the actual figures needs the missing EPS/CDR source.
  Affected: `m122 m155 m180 m220 m231 m250 m251 m253 m255 m282 m283 m301
  m305 m351 m401 m403 m404 m405 m406 m407 m408 m409 m411 m412 m414 m415
  m416 m424 m466 m486 m501 m504 m505`.
* Some referenced figures have **no digital source at all** (no `.eps` /
  `.cdr`). Extracting them from the published 2-up landscape MISN PDFs is
  not automated — frame detection catches the column rules, not the figure
  borders.
* `.cdr` files converted via LibreOffice (`soffice`) are **lower fidelity**
  than the `epstopdf → pdftocairo` path used for EPS. `.cdx` "Collated"
  files are unusable (every figure overlaid in one canvas).

### Catalog
* Module titles in `src/data/modules.js` needed correcting against
  physnet.org — a whole block of `m402`–`m418` plus the 7 recovered
  modules were mislabeled with unrelated Physics-1 topics. **The full
  catalog has not been audited**; other stale titles are likely.

### `.org`-format sources
* `\rem{...}`-wrapped content is stripped entirely, which also drops
  footnote-style `\TxtAdvice` asides that arguably belong in the output.
* Subsection titles that are just an `\index{}` keyword render as terse
  fragments (e.g. `m422` "V of") rather than a full phrase.

### Process
* **No CI QA gate.** `scripts/smoke/qa.py` runs manually against a local
  `npm run preview` server; there is no automated check on build or PR.
* KaTeX runs with `throwOnError: false`, so an unhandled math macro leaves
  the raw `\[...\]` source as visible text **with no error styling**.
  `qa.py` catches this, but adding new macro handlers can silently
  reintroduce leaks elsewhere — re-run QA broadly after converter changes.
* Converting a module with the current script can change unrelated output
  (the drop-missing-figure pass, title normalisation); re-convert and
  diff deliberately rather than in bulk.

### Interactive problems
* Only **1 of the 12** force-diagram problems is built (`problem1.html` /
  `HangingBall`). The other 11 Java sources in `OneBodyForceDiagrams/`
  are not yet ported.

---

## Interactive problems

Each problem under `public/interactive/force-diagrams/` is a self-contained
HTML file (inline CSS + JS) laid out as a four-panel CSS grid: the scenario
canvas, an instruction/feedback panel, a results panel, and a student
drawing area built from stacked transparent `<canvas>` layers. Drag
gestures are captured as force vectors and checked against the correct
answer with angular and length tolerances. See `CLAUDE.md` for the
step-by-step recipe to add a new problem.
