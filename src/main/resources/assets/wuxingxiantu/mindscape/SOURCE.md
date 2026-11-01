# Mindscape material source

The complete scalar FBM material in `user-reference.glsl` was supplied by the
project owner for this background. The upstream author, link and license were
not supplied; this fragment is not claimed as a newly authored algorithm.

The Forge adapter retains its four/six-octave functions, sequential nine-step
warp, finite-difference normal, lighting and inverse-square contrast chain.
The finite ellipsoid mapping, shared internal-energy material, instance/session
rendering, state transition and integration code adapt that source to this mod.

The 2026-11-01 approved workbench revision keeps that three-surface carrier and
all nine animated cosine feedback steps. Its active evaluator replaces only
the two length-based radial phase disturbances with sequential transverse
shears (`funcWithoutRadialWave`), reducing center-directed breathing without
moving the camera or transporting a frozen texture. The original `func` and
the separate user reference remain intact. Animation starts at phase 12 for
each session and advances at the approved default rate of 0.48; purification
and resource reloads retain the same session clock. Both palettes below remain
byte-identical to the workbench defaults.

Both palette files are exact copies of the owner's approved
`mindscape-palette/1` exports. Their zero spiritual-energy colors are intentional
configuration values and must not be replaced by the workbench's older blue
defaults.

The current purified export supersedes the earlier yellow-base palette:

| State | File | SHA-256 |
| --- | --- | --- |
| Corrupted | `palette-corrupted.json` | `75bdd745aedec420fab9406e425e6399f3fea95d7c39af933cf860fc3f8ea8fb` |
| Purified, silver/ink/gold | `palette-purified.json` | `1cd903fbce6c1a50032607f0ed35a4b8369a54e2e99a16d1c3760332f3126c8d` |

Copy the entire state-specific export, including HDR RGB values, spiritual
strength and exposure. The purified base is `[16,16,16]`, highlight is
`[16,6.859047945822506,0.28227127181414524]`, spiritual colors are zero,
spiritual strength is `4` and exposure is `0.1`. Normalizing the HDR colors,
dropping exposure or reusing the corrupted state's palette changes the result.

The primary membrane is closed and opaque in every direction. Its bounded,
smooth material projection has no rear-pole cutout. Secondary fold silhouettes
remain local, over that complete underlying material. Field alpha records actual
accumulated material coverage; silver-white material is not an uncovered pixel.
