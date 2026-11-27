#!/usr/bin/env python3
"""Render the Societix architecture and SOC brief HTML pages to PDF with headless Chromium.

Usage: CHROME=/path/to/chrome python3 docs/artifacts/render-pdfs.py /path/to/data
  The data dir must contain societix-architecture/ and societix-soc-briefs/.
  If Chrome lacks shared libraries, point LD_LIBRARY_PATH at a directory that provides them.
Env: PDF_TIMEOUT seconds per page (default 120).
Output: docs/artifacts/pdf/{architecture,soc-briefs}/<page>.pdf (A3 portrait, light theme).
Sources are copied to a temp dir and print CSS is injected in the copy only.
"""
import os, subprocess, sys, tempfile
from pathlib import Path

PRINT_CSS = """<style id="print-fit">
@page { size: A3 portrait; margin: 12mm }
:root, :root[data-theme="dark"] { color-scheme: light }
html, body { background: #fff !important }
* { -webkit-print-color-adjust: exact; print-color-adjust: exact; overflow: visible !important }
svg { min-width: 0 !important; width: 100% !important; height: auto !important; max-width: 100% !important }
svg, figure, table, tr, pre, article, .diagram { break-inside: avoid }
h1, h2, h3 { break-after: avoid }
pre, code { white-space: pre-wrap !important; overflow-wrap: anywhere }
table { max-width: 100% }
</style>"""
GROUPS = {"societix-architecture": "architecture", "societix-soc-briefs": "soc-briefs"}
chrome = os.environ["CHROME"]
timeout = int(os.environ.get("PDF_TIMEOUT", "120"))
src = Path(sys.argv[1])
out = Path(__file__).resolve().parent / "pdf"
failed = []
with tempfile.TemporaryDirectory() as tmp:
    for d, group in GROUPS.items():
        (out / group).mkdir(parents=True, exist_ok=True)
        for f in sorted((src / d).glob("*.html")):
            copy = Path(tmp) / d / f.name
            copy.parent.mkdir(exist_ok=True)
            copy.write_text(f.read_text(encoding="utf-8").replace("</head>", PRINT_CSS + "</head>", 1), encoding="utf-8")
            target = out / group / (f.stem + ".pdf")
            try:
                subprocess.run([chrome, "--headless=new", "--no-sandbox", "--disable-gpu", "--no-pdf-header-footer",
                                "--virtual-time-budget=5000", f"--print-to-pdf={target}", copy.as_uri()],
                               timeout=timeout, check=True, capture_output=True)
                print("ok", group, f.name, flush=True)
            except Exception as e:
                failed.append(f.name); print("FAILED", group, f.name, e, flush=True)
sys.exit(1 if failed else 0)
