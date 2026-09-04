#!/usr/bin/env bash
# Re-convert a list of modules from the local LatexModsSource tree.
# Usage: scripts/smoke/batch.sh m6 m7 m8 ...
# For each id: assemble a scratch source dir (body + dat/as/ps/me), run the
# converter to src/content/modules/<id>.html, print unknown-macro summary.
set -u
SRC=/home/rsignell/physnet_src/LatexModsSource
WORK=/tmp/claude-1000/-home-rsignell-repos-Physnet/4da27259-db10-4129-9a37-524e0d5f8622/scratchpad/batch
mkdir -p "$WORK"

for m in "$@"; do
  d=$(find "$SRC" -type d -name "$m" 2>/dev/null | head -1)
  if [ -z "$d" ]; then echo "$m  NO-DIR"; continue; fi
  o="$WORK/$m"; rm -rf "$o"; mkdir -p "$o"

  # body: -tx.tex / -b.tex, else newest version-numbered -tx.* / -b.*
  body=""
  for suf in -tx.tex -b.tex; do [ -f "$d/$m$suf" ] && body="$d/$m$suf"; done
  if [ -z "$body" ]; then
    body=$(ls -t "$d"/$m-tx.[0-9]* "$d"/$m-b.[0-9]* "$d"/$m-tx.bak "$d"/$m-b.bak 2>/dev/null | head -1)
  fi
  if [ -z "$body" ]; then echo "$m  NO-BODY"; continue; fi
  cp "$body" "$o/$m-tx.tex"

  for part in dat as ps me; do
    f=$(ls "$d/$m-$part.tex" 2>/dev/null || ls -t "$d/$m-$part".[0-9]* 2>/dev/null | head -1)
    [ -n "$f" ] && [ -f "$f" ] && cp "$f" "$o/$m-$part.tex"
  done

  python3 scripts/tex_to_html.py "$o" "$m" > "src/content/modules/$m.html" 2>"$o/err"
  rc=$?
  unk=$(grep -oE '<span class="tex-unknown"[^>]*>[^<]*' "src/content/modules/$m.html" 2>/dev/null \
        | sed 's/.*>//' | sort -u | tr '\n' ' ')
  sz=$(wc -c < "src/content/modules/$m.html" 2>/dev/null)
  printf "%-6s rc=%s sz=%-6s %s%s\n" "$m" "$rc" "$sz" \
     "$( [ -s "$o/err" ] && echo "ERR:$(head -c80 "$o/err") " )" \
     "$( [ -n "$unk" ] && echo "UNK: $unk" )"
done
