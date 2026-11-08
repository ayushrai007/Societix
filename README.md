# Societix — Prototype

Marketing site + interactive mobile-app mockup for **Societix**, the autonomous, privacy-first
operating system for residential communities. Single-page, no build step, no runtime dependencies.

## Files

| File | Purpose |
|------|---------|
| `index.html` | Markup: the Website view and the App view (phone mockup). |
| `styles.css` | Full design system (navy + emerald tokens) and all component styles. |
| `app.js` | View toggle, role/screen switching, app-screen renderers, and the demo form. |

The favicon is an inline SVG data-URI in `<head>` — no separate asset, works offline.

## Run

Open `index.html` in any modern browser (double-click, or `open index.html` on macOS).
Fonts load from Google Fonts over CDN; everything else is local, so the page still works offline
with system-font fallbacks.

## Project documentation

See the [project documentation](docs/README.md) for product, architecture, development, and
delivery guidance.

## What it demonstrates

- **Website view** — hero, problem stats, 6 product modules, legacy-vs-Societix paradigm,
  MyGate/NoBrokerHood comparison, strategic moats, pricing, roadmap, and a working demo form.
- **App view** — a phone mockup with a **Resident ⇄ Committee** role toggle and a working
  bottom tab bar. Resident: dues/pay, visitor QR, AGM e-voting, help. Committee: autopilot
  treasury, defaulter cadence, live quorum, AI copilot, gate log.

Use the **Website / App** toggle in the top bar to switch views.

## Demo form

The "Book a demo" form (`#demo` section) validates name, society, and a valid email/phone, then
stores the lead in `localStorage` under the key `societix_leads` and shows a confirmation.

**To wire it to a real backend**, replace the `localStorage` block in `app.js` (inside
`demoInit`, marked with a comment) with a `fetch()` POST to your endpoint, e.g.:

```js
await fetch('/api/leads', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(lead)
});
```

## Notes

Content is illustrative (demo society "Maple Residency", sample figures) and reflects the vision in
the Societix BRD (SOC-BRD-2026-V1). This is a front-end prototype — there is no real treasury,
cryptographic voting, or gate backend behind the screens.
