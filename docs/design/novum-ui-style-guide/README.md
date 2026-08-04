# Novum UI Style Guide

Measured mobile UI reference for `https://www.novumaivip.com`. It records the observed visual system and does not redesign the source site.

## View

Open [`index.html`](index.html) directly in a browser. No server or build step is required.

## Scope

- Small mobile: 360 x 800 CSS px, DPR 3.
- Standard mobile: 430 x 932 CSS px, DPR 3.
- Large mobile / tablet: 768 x 1024 CSS px, DPR 2.
- AdsPower profile 37 (`k1f658vy`), Chromium rendering engine.
- Read-only navigation; no mutating form was submitted.

## Contents

- `index.html`, `styles.css`, `guide.js`: concise visual guide.
- `data/tokens.json`: measured design tokens and confidence labels.
- `data/assets.json`: complete manifest for downloaded resources.
- `data/summary.json`: capture scope and three-viewport summary.
- `assets/`: 83 SHA-256-deduplicated images, icons, and fonts.

Values read from runtime styles or resource metadata are marked `observed`. Normalized spacing values are marked `inferred`. Unobserved states are not invented.

The device dimensions and touch input were emulated. Rendering remains Chromium, not Safari/WebKit. Production content may change after the capture date recorded in `data/summary.json`.
