# CtrlX Dock Icon Contrast Implementation Plan

## Goal

Improve the CtrlX Dock icon's small-size readability without changing its identity or composition.

## Scope

- Preserve the rounded black tile, large `X`, and two integrated control sliders.
- Lift the `X` face from near-black to graphite and strengthen its edge highlights.
- Keep the palette monochrome and restrained; no color accents, glow, text, or new shapes.
- Regenerate all raster app-icon derivatives from one 1024×1024 master.
- Validate the result at 32 px and 64 px before installing it locally.

## Stage 1

1. Edit `Brand/CtrlX-AppIcon.png` while preserving the existing geometry.
2. Synchronize the macOS, iOS, and website PNG derivatives.
3. Verify dimensions, small-size legibility, asset consistency, and the macOS build.
4. Review the complete change set and record the result.
5. Merge into `develop/v3.0.0`, package, and replace `/Applications/CtrlX.app` without touching tmux sessions.

## Acceptance Criteria

- The `X` silhouette remains clear at Dock-scale sizes.
- The icon still reads as premium black/graphite rather than gray or silver.
- Existing geometry and slider placement remain recognizable.
- All raster derivatives match the final master and have the dimensions declared by their asset catalogs.
- The macOS app builds, signs, launches, and responds after replacement.
- Existing tmux server PID and sessions remain unchanged.

