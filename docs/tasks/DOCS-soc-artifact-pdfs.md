# DOCS — SOC architecture and brief PDFs

Merge state: pull request open (see the PR description).  
Intended reviewer: `@ayushrai007`.

## What it delivers and why

PDF copies of the Societix architecture pages and the SOC brief pages, so they can be read and shared without the HTML files. Documentation only: no product code, tests or other documentation changed.

## Files

- `docs/artifacts/pdf/architecture/`: `index`, `modules`, `socs`, `deployment`, `flows` (5 PDFs).
- `docs/artifacts/pdf/soc-briefs/`: `index`, `soc-1`, `soc-3`, `soc-4`, `soc-5`, `soc-10`, `soc-14`, `soc-100`, `soc-100-reference` (9 PDFs).
- `docs/artifacts/render-pdfs.py`: the render script.

Each PDF keeps the name of its source page. The source HTML is not committed.

**Excluded:** the outbox design artifact (`societix-outbox-design/`) is not converted or included.
Note for the reviewer: some in-scope source pages (architecture `index`, `modules`, `flows`, `socs`) mention the open outbox delivery decision (polling as merged, versus a database signal or queue). That text is part of those pages and was converted as written; no source content was edited.

## How to regenerate

```bash
CHROME=<path to chrome> python3 docs/artifacts/render-pdfs.py <data dir containing societix-architecture/ and societix-soc-briefs/>
```

The script copies each page to a temporary directory, injects print CSS in the copy only (A3 portrait, light theme, SVG scaled to page width, diagrams and tables kept unbroken), and runs headless Chromium `--print-to-pdf`. Run with Playwright Chromium 1243. Its missing system libraries (`libnspr4`, `libnss3`, `libasound2t64`) were extracted into a private directory and supplied through `LD_LIBRARY_PATH`; nothing was installed system-wide.

## Verification

All 14 PDFs were rendered to page images (contact sheets) and checked by eye for clipped or overlapping diagrams, cut-off tables, blank pages and unreadable splits. Page geometry and extracted text were checked with PyMuPDF: every page is 841.9 x 1191.1 pt (A3), no text block touches the page edge (0 edge blocks), and no page is blank.

| PDF | Pages | Text chars | Edge blocks | Blank pages | Visual check |
|---|---|---|---|---|---|
| architecture/index | 4 | 10389 | 0 | 0 | OK |
| architecture/modules | 11 | 21097 | 0 | 0 | OK, all diagrams whole |
| architecture/socs | 11 | 26666 | 0 | 0 | OK |
| architecture/deployment | 5 | 11442 | 0 | 0 | OK |
| architecture/flows | 9 | 12114 | 0 | 0 | OK, all sequence diagrams whole |
| soc-briefs/index | 4 | 6957 | 0 | 0 | OK |
| soc-briefs/soc-1 | 4 | 8333 | 0 | 0 | OK |
| soc-briefs/soc-3 | 4 | 9137 | 0 | 0 | OK |
| soc-briefs/soc-4 | 4 | 7832 | 0 | 0 | OK |
| soc-briefs/soc-5 | 4 | 9555 | 0 | 0 | OK |
| soc-briefs/soc-10 | 5 | 11429 | 0 | 0 | OK |
| soc-briefs/soc-14 | 4 | 8428 | 0 | 0 | OK |
| soc-briefs/soc-100 | 6 | 13023 | 0 | 0 | OK |
| soc-briefs/soc-100-reference | 4 | 6703 | 0 | 0 | OK |

Some pages end with white space where a diagram or table was moved whole to the next page. This is intended.
