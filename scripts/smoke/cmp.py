#!/usr/bin/env python3
"""HTML-vs-PDF side-by-side for a module.
Usage: cmp.py m2 /path/to/m2.pdf out.png [firstpage]
Renders the full HTML page as one tall PNG next to the stacked PDF pages."""
import sys, subprocess, io
import fitz
from PIL import Image
from playwright.sync_api import sync_playwright

mod, pdf, out = sys.argv[1], sys.argv[2], sys.argv[3]
first = int(sys.argv[4]) if len(sys.argv) > 4 else 2  # skip PDF cover/ID page

# ---- HTML: full-page screenshot ----
with sync_playwright() as pw:
    b = pw.chromium.launch()
    p = b.new_page(viewport={"width": 900, "height": 1200}, device_scale_factor=2)
    p.goto(f"http://localhost:4321/Physnet/modules/{mod}/", wait_until="networkidle")
    p.wait_for_timeout(700)
    png = p.screenshot(full_page=True)
    b.close()
html_img = Image.open(io.BytesIO(png))

# ---- PDF: render pages first.. to images, stack ----
doc = fitz.open(pdf)
pdf_imgs = []
for n in range(first - 1, min(len(doc), first - 1 + 8)):
    pix = doc[n].get_pixmap(dpi=110)
    pdf_imgs.append(Image.open(io.BytesIO(pix.tobytes("png"))))
pw_ = max(i.width for i in pdf_imgs)
ph = sum(i.height for i in pdf_imgs)
pdf_col = Image.new("RGB", (pw_, ph), "white")
y = 0
for im in pdf_imgs:
    pdf_col.paste(im, (0, y)); y += im.height

# ---- scale HTML column to same height, compose ----
scale = ph / html_img.height
html_scaled = html_img.resize((int(html_img.width * scale), ph))
canvas = Image.new("RGB", (html_scaled.width + pw_ + 20, ph), "white")
canvas.paste(html_scaled, (0, 0))
canvas.paste(pdf_col, (html_scaled.width + 20, 0))
# cap width for viewing
if canvas.width > 1600:
    r = 1600 / canvas.width
    canvas = canvas.resize((1600, int(canvas.height * r)))
canvas.save(out)
print(f"{out}  ({canvas.size})  HTML left, PDF p{first}+ right")
