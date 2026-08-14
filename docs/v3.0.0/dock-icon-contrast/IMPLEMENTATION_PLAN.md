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

## Stage 2

1. Use the accepted Stage 1 icon as the immutable geometry reference.
2. Raise only the `X` face luminance and neutral edge highlights by one restrained step.
3. Keep the background, slider geometry, framing, texture, and monochrome identity unchanged.
4. Re-run the Stage 1 derivative, build, review, packaging, and tmux-preservation checks.

## Stage 3

1. Freeze the accepted Stage 2 background and `X` luminance.
2. Increase controller separation: darker and slightly wider tracks, brighter knobs with stronger outlines, and modestly larger knob silhouettes.
3. Keep both controls integrated into the existing diagonals and preserve their positions and direction.
4. Require both slider controls to remain recognizable at 64 px; at 32 px, require at least two distinct knob marks and continuous dark tracks.
5. Re-run derivative, build, review, packaging, and tmux-preservation checks.

## Acceptance Criteria

- The `X` silhouette remains clear at Dock-scale sizes.
- The icon still reads as premium black/graphite rather than gray or silver.
- Existing geometry and slider placement remain recognizable.
- All raster derivatives match the final master and have the dimensions declared by their asset catalogs.
- The macOS app builds, signs, launches, and responds after replacement.
- Existing tmux server PID and sessions remain unchanged.
- Stage 2 remains visibly brighter than Stage 1 at 32 px and 64 px without turning the `X` silver.
- Stage 3 makes the embedded controls readable without further brightening the `X` or changing the overall silhouette.
