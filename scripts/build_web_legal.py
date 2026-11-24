#!/usr/bin/env python3
"""
Builds standalone, hostable copies of the in-app legal pages.

    python3 scripts/build_web_legal.py

Reads  app/src/main/assets/legal/{privacy,terms}.html + legal.css + lilita_one.ttf + ng_studio_logo.png
Writes docs/web/privacy.html and docs/web/terms.html

Each output file is self-contained: the CSS is inlined and the font and logo are embedded as data URIs,
so you can upload just the two .html files to any host (keep them in the same folder so the links
between them work). Run it again whenever the legal text changes, so the website matches the app.
"""
import base64
import os
import re
import subprocess
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main", "assets", "legal")
OUT = os.path.join(ROOT, "docs", "web")

DESCRIPTIONS = {
    "privacy": "Privacy Policy for Maze Brush: Paint Puzzle Game by NG Studio (NGENDEV TECHNOLABS LLP): "
               "what data the game uses, notifications, app updates and your choices.",
    "terms": "Terms of Use for Maze Brush: Paint Puzzle Game by NG Studio (NGENDEV TECHNOLABS LLP).",
}

# Extra rules for the web: centre the page at a readable width on tablets and desktops.
WEB_CSS = """
/* web: readable column on wide screens */
.page { max-width: 760px; margin: 0 auto; }
@media (min-width: 800px) { body { padding: 32px 24px 56px; } h1 { font-size: 40px; } }
"""


def data_uri(path, mime):
    with open(path, "rb") as f:
        return f"data:{mime};base64,{base64.b64encode(f.read()).decode()}"


def small_logo():
    """A 300 px wide copy of the logo (shown at 150 px; 2x for sharp screens) to keep the pages light."""
    src = os.path.join(SRC, "ng_studio_logo.png")
    tmp = os.path.join(tempfile.mkdtemp(), "logo.png")
    try:
        subprocess.run(["sips", "-Z", "300", src, "--out", tmp], check=True, capture_output=True)
        return data_uri(tmp, "image/png")
    except (OSError, subprocess.CalledProcessError):
        return data_uri(src, "image/png")  # no sips (not macOS): embed the original


def build(page, css, logo):
    with open(os.path.join(SRC, f"{page}.html"), encoding="utf-8") as f:
        html = f.read()
    style = f"<style>\n{css}\n{WEB_CSS}</style>"
    meta = (f'<meta name="description" content="{DESCRIPTIONS[page]}">'
            '<meta name="theme-color" content="#c99257">')
    html = html.replace('<link rel="stylesheet" href="legal.css">', meta + style)
    html = html.replace('src="ng_studio_logo.png"', f'src="{logo}"')
    # wrap the content in a centred column
    html = re.sub(r"<body>\n?", '<body>\n<div class="page">\n', html, count=1)
    html = html.replace("</body>", "</div>\n</body>", 1)
    os.makedirs(OUT, exist_ok=True)
    dst = os.path.join(OUT, f"{page}.html")
    with open(dst, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"  {os.path.relpath(dst, ROOT)}  ({os.path.getsize(dst) // 1024} KB)")


def main():
    with open(os.path.join(SRC, "legal.css"), encoding="utf-8") as f:
        css = f.read()
    css = css.replace('url("lilita_one.ttf")', f'url("{data_uri(os.path.join(SRC, "lilita_one.ttf"), "font/ttf")}")')
    logo = small_logo()
    print("Writing hostable legal pages:")
    for page in ("privacy", "terms"):
        build(page, css, logo)


if __name__ == "__main__":
    main()
