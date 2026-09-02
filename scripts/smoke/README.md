# Smoke test — m1

One module carried end-to-end from authoritative LaTeX source to rendered
HTML, used to shake out converter bugs before a bulk re-run.

- `m1_src/m1-tx.tex`, `m1_src/m1-dat.tex` — authoritative source (MISN-0-1,
  v. 9/25/02), re-fetched from Google Drive via `read_file_content` and
  de-escaped. `/tmp/physnet_src` is no longer available.
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
