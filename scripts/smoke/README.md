# Smoke test — m1

One module carried end-to-end from authoritative LaTeX source to rendered
HTML, used to shake out converter bugs before a bulk re-run.

- `m1_src/m1-tx.tex`, `m1_src/m1-dat.tex`, `m1_src/m1-as.tex` — authoritative
  source (MISN-0-1, v. 9/25/02), re-fetched from Google Drive via
  `read_file_content` and de-escaped. `/tmp/physnet_src` is no longer available.
- Regenerate:  `python3 scripts/tex_to_html.py scripts/smoke/m1_src m1 > src/content/modules/m1.html`

## Converter bugs this found (all fixed in scripts/tex_to_html.py)

1. `\Eqn{N}{…}` discarded the number N — no `(N)` label, no `id="eqn-N"`
   anchor, so every `\Eqnref{N}` / `\Equationref{N}` link was dead.
2. `\pcap[\Index{…}]{s}{l}{title}` — the optional `[…]` arg was not skipped,
   producing a blank `<h3>. </h3>` for every subsection with index entries.
3. `\parenhelp{N}` — unhandled, leaked as `tex-unknown`.
4. `\unit{X\up{N}}` — nested `\up` not handled; then `^` landed inside
   `\text{}` (invalid → KaTeX left the whole block as raw `\[…\]`).
5. `\begin{eqnarray*}` — 3-column `lhs & rel & rhs` rows fed straight into
   KaTeX `aligned` (2-column); relation column now folded into the RHS.
6. Cover cartoon (`mNgr00`, placed by the nphmods.sty module template, never
   referenced in the body) is now injected after the header when its SVG
   exists.

---

# Smoke test — m10 (figure-heavy: 14 figures, multi-figure rows)

- `m10_src/m10-tx.tex`, `m10_src/m10-dat.tex`, `m10_src/m10-as.tex` — MISN-0-10
  v. 9/12/02, from Drive.
- Regenerate:  `python3 scripts/tex_to_html.py scripts/smoke/m10_src m10 > src/content/modules/m10.html`

## Additional converter bugs this found (fixed)

7. `\newsavebox{\hlp}` / `\sbox{\hlp}{…}` / `\usebox{\hlp}` — LaTeX box
   plumbing used to defer a `\help{}` link into an equation. `\usebox`
   leaked into `\Eqn{}` math and broke that equation in KaTeX. Now all
   three are stripped in `_strip_preamble` (the deferred fragment is lost).
8. Empty figure captions (`\ThreeCaptionedFramedFigures{1}{}{m10gr01}…`)
   rendered as "Fig. 1. " with dangling punctuation → now "Fig. 1.".

## Confirmed working (thanks to the m1 fixes)

- Figure number ≠ file name: Fig 12 → m10gr16.svg, Fig 13 → m10gr17.svg, etc.
- `\ThreeCaptionedFramedFigures` / `\TwoCaptionedFramedFigures` rows.
- `\Figref{N}` inside figure captions.
- `\begin{eqnarray*}` with `\unit{lb}`, `\degrees`, `\text{}` subscripts.
- Nested `\unit{ft/s\up{2}}`.
- `\SubSubSect{}{Example:}{…}`, `\icap{Rule 1}`, `\begin{itemize}`,
  `\begin{one-digit-list}`, `\vect`/`\uvec` vectors.

---

# Special Assistance Supplement wired in (m1 + m10)

`-as.tex` is now fetched and converted alongside `-tx.tex` for both smoke
modules, so the inline hint pointers resolve.

## Figure re-conversion (m1: all 9 EPS -> SVG)

The `mNgrNN.svg` files were originally made with Inkscape's EPS importer,
which drops word spaces ("graphof G(x)") and mis-maps the CorelDRAW
"Symbol" font, so theta rendered as `q`, phi as `f`, Delta as `D`, pi as
`p`.  Re-converted m1's figures through `epstopdf` (Ghostscript) then
`pdftocairo -svg` (poppler): text becomes glyph outlines, spacing and
Greek letters correct.  Script: `scripts/eps_to_svg.sh`.  Source EPS
re-fetched from Drive (`m1gr00.eps` .. `m1gr08.eps`).

## Converter changes

9.  `\help{N}` / `\parenhelp{N}` now render as `[S-N]` (superscript / inline
    respectively) linking to `#help-N`, matching the printed module's
    "Sequence [S-N]" notation — instead of the old dead `[help N]` text.
10. `\AsItem{N}{ref}{…}` now emits an `<h3>S-N (from <ref>)</h3>` heading
    (`_pretty_asref`: `TX-4a` → "Section 4a", `PS-Problem~1` →
    "Problem Supplement, Problem 1"), matching the `pdfs/m10.html` reference.
11. `\CenteredUnframedFixedFigure{m1gr08.eps}` — trailing `.eps`/`.ps`/`.pdf`/
    image extension is now stripped before building the `.svg` `src`.
12. `\begin{tabular}` — a leading `\hline` now promotes the first row to
    `<th>`; added `.physnet-table` CSS (borders + padding) so tables are
    legible at all (there was none before).
