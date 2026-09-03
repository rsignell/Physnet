#!/usr/bin/env bash
# EPS -> SVG figure conversion for MISN modules.
#
# The CorelDRAW EPS files embed Type-1 fonts with stub encodings: word
# spaces have no glyph and Greek letters live in a "Symbol" font whose
# glyphs are named /c32../c255.  Inkscape's direct EPS import mis-handles
# both -> "graphof G(x)", theta shown as "q", phi as "f", pi as "p".
#
# Going through a real PostScript interpreter fixes it: Ghostscript
# (epstopdf) rasterises the embedded fonts into a PDF, then pdftocairo
# emits an SVG with every glyph as an outline <symbol>/<use> -- no font
# dependency, exact positioning.
#
# Requires: epstopdf (texlive) + pdftocairo (poppler).  In this repo they
# live in the conda env `texsvg`:
#     mamba create -n texsvg -c conda-forge texlive-core poppler
#
# Usage:  scripts/eps_to_svg.sh out_dir file1.eps [file2.eps ...]
#         scripts/eps_to_svg.sh public/modules/m1/figures  /path/to/m1gr*.eps
set -euo pipefail

BIN="${TEXSVG_BIN:-$HOME/miniforge3/envs/texsvg/bin}"
EPSTOPDF="$BIN/epstopdf"
PDFTOCAIRO="$BIN/pdftocairo"
[ -x "$EPSTOPDF" ]   || { echo "epstopdf not found at $EPSTOPDF" >&2; exit 1; }
[ -x "$PDFTOCAIRO" ] || { echo "pdftocairo not found at $PDFTOCAIRO" >&2; exit 1; }

out="${1:?usage: eps_to_svg.sh out_dir file.eps ...}"; shift
mkdir -p "$out"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT

for eps in "$@"; do
    base="$(basename "${eps%.*}")"
    "$EPSTOPDF"   --outfile="$tmp/$base.pdf" "$eps"
    "$PDFTOCAIRO" -svg "$tmp/$base.pdf" "$out/$base.svg"
    printf '%s  ->  %s  (%d bytes)\n' "$eps" "$out/$base.svg" "$(stat -c%s "$out/$base.svg")"
done
