#!/usr/bin/env python3
"""
extract_prereqs.py - Extract prerequisite graph from all Physnet dat files.

Usage:
    python scripts/extract_prereqs.py <source_root> <output_json>
    python scripts/extract_prereqs.py /tmp/physnet_src src/data/prerequisites.json
"""

import re
import os
import sys
import json


def strip_comments(text):
    out = []
    i = 0
    while i < len(text):
        if text[i] == '\\' and i + 1 < len(text):
            out.append(text[i:i+2])
            i += 2
        elif text[i] == '%':
            while i < len(text) and text[i] != '\n':
                i += 1
        else:
            out.append(text[i])
            i += 1
    return ''.join(out)


def extract_prereqs(dat_text):
    """Return list of prerequisite module IDs from a dat file."""
    text = strip_comments(dat_text)
    # \prrqone{0-14} or \prrqtwo{0-14} → m14
    refs = re.findall(r'\\prrq(?:one|two)\s*\{0-(\d+)\}', text)
    # Deduplicate while preserving order
    seen = set()
    result = []
    for r in refs:
        mid = f'm{r}'
        if mid not in seen:
            seen.add(mid)
            result.append(mid)
    return result


def extract_title(dat_text):
    """Extract module title from defModTitle."""
    text = strip_comments(dat_text)
    m = re.search(r'\\defModTitle\s*\{([^}]*)\}', text)
    if not m:
        return ''
    # Strip \ph{...} wrapper and other simple macros
    title = m.group(1)
    title = re.sub(r'\\ph\s*\{([^}]*)\}', r'\1', title)
    title = re.sub(r'\\[a-zA-Z]+\s*\{([^}]*)\}', r'\1', title)
    title = re.sub(r'\\[a-zA-Z]+', '', title)
    return title.strip()


def main():
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        sys.exit(1)

    source_root = sys.argv[1]
    output_path = sys.argv[2]

    prereqs = {}   # module_id -> [prereq_ids]
    titles  = {}   # module_id -> title (from dat, as fallback)

    for dirpath, dirnames, filenames in os.walk(source_root):
        for fname in filenames:
            if not fname.endswith('-dat.tex'):
                continue
            # Derive module id from filename: m5-dat.tex → m5
            mod_id = fname.replace('-dat.tex', '')
            if not re.match(r'^m\d+$', mod_id):
                continue
            fpath = os.path.join(dirpath, fname)
            try:
                with open(fpath, encoding='utf-8', errors='replace') as f:
                    text = f.read()
            except OSError:
                continue
            prereqs[mod_id] = extract_prereqs(text)
            t = extract_title(text)
            if t:
                titles[mod_id] = t

    # Build reverse graph: module → list of modules that require it
    unlocks = {}  # module_id -> [module_ids that list it as prereq]
    for mod, plist in prereqs.items():
        for p in plist:
            unlocks.setdefault(p, [])
            if mod not in unlocks[p]:
                unlocks[p].append(mod)

    output = {
        'prereqs': prereqs,
        'unlocks': unlocks,
        'titles':  titles,
    }

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(output, f, indent=2, sort_keys=True)

    print(f"Wrote {len(prereqs)} modules to {output_path}", file=sys.stderr)
    total_edges = sum(len(v) for v in prereqs.values())
    print(f"Total prerequisite edges: {total_edges}", file=sys.stderr)


if __name__ == '__main__':
    main()
