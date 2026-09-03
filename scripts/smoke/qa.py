#!/usr/bin/env python3
"""Fast per-module QA: load the built page, report KaTeX errors, raw-TeX
leaks, broken images. Usage: qa.py m2 [m3 ...]  (preview server on :4321)"""
import sys
from playwright.sync_api import sync_playwright

BASE = "http://localhost:4321/Physnet/modules"

with sync_playwright() as pw:
    b = pw.chromium.launch()
    bad = 0
    for mod in sys.argv[1:]:
        p = b.new_page(viewport={"width": 1100, "height": 1000})
        p.goto(f"{BASE}/{mod}/", wait_until="networkidle")
        p.wait_for_timeout(500)
        errs = p.evaluate(
            "() => [...document.querySelectorAll('.katex-error')].map(e => e.getAttribute('title'))")
        raw = p.evaluate(r"""() => {
            const t = document.querySelector('.module-content')?.innerText || '';
            return [...new Set(t.match(/\\[a-zA-Z]{2,}|\\\[|\\\]|tex-unknown/g) || [])].slice(0, 12);
        }""")
        imgs = p.evaluate("""() => [...document.querySelectorAll('.module-content img')]
            .filter(i => !(i.complete && i.naturalWidth > 0))
            .map(i => i.getAttribute('src'))""")
        figc = p.evaluate("() => document.querySelectorAll('.module-content img').length")
        stat = "OK" if not (errs or raw or imgs) else "FAIL"
        if stat == "FAIL":
            bad += 1
        print(f"[{stat}] {mod}: katex_err={len(errs)} raw={raw} broken_imgs={imgs} figs={figc}")
        if errs:
            print("     katex:", errs[:5])
        p.close()
    b.close()
    sys.exit(1 if bad else 0)
