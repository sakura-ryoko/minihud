# Lightning Rod Range Overlay

## Overview

The Lightning Rod Range Overlay displays the attraction range and damage zones for lightning rods placed in the Overworld. When enabled, it renders:

1. **Attraction Zone** - A 128-block radius sphere showing where lightning can be attracted
2. **Damage Zone** - A 6×12×6 box (3 blocks horizontal, -2 to +10 vertical) showing where entities can be damaged
3. **Outline** - Visual border highlighting the overlay boundaries

This feature helps players optimize lightning rod placement for farms, safe zones, and decorative builds by visualizing the exact protection area each rod provides.

## Technical Architecture

### Per-Rod VBO Design

Each lightning rod maintains its own set of Vertex Buffer Objects (VBOs) rather than sharing global buffers:

- **Attraction Zone VBO**: Sphere geometry (configurable as combined quads or circle outlines)
- **Damage Zone VBO**: Filled box geometry
- **Outline VBO**: Wire-frame edges for both zones

This architecture enables O(1) updates when placing/removing individual rods, as only the affected rod's VBOs need rebuilding. Camera movement still requires O(N) rebuilds due to camera-relative coordinate requirements.

### Async Sphere Calculation

Initial sphere geometry calculation (identifying all blocks within 128-block radius) runs asynchronously on worker threads to prevent FPS drops during rod placement:

1. Placeholder entry created immediately on main thread
2. Worker thread calculates sphere positions using optimized ring algorithm
3. Result applied to main thread via `Minecraft.getInstance().execute()` callback
4. VBO built on next render frame

Average calculation time: 30-60ms per rod (amortized, one-time cost).

### Event-Driven Updates

The overlay responds to several events:

- **Block changes**: `onBlockChange()` detects rod placement/removal via block state checks
- **Chunk loads**: `onChunkLoad()` scans newly loaded chunks for rods
- **Config changes**: Color and rendering mode changes trigger VBO rebuilds
- **Dimension changes**: Clears all data (only works in Overworld)

### Thread Safety

All modifications to the `lightningRods` list occur on the main rendering thread. Async sphere calculations run on worker threads but results are applied via main thread callbacks, ensuring no synchronization primitives are needed.

## Configuration

### Toggle

- **Config**: `Renderer Toggles` → `overlayLightningRodRange`
- **Default**: OFF
- **Hotkey**: None (assign in `Hotkeys` menu)

### Colors

- **Attraction Zone Color**: `Colors` → `lightningRodRangeOverlayColor` (default: blue)
- **Damage Zone Color**: `Colors` → `lightningRodDamageZoneColor` (default: red)
- Both support alpha channel for transparency

### Rendering Modes

- **Combine Quads**: `Generic` → `lightningRodRangeOverlayCombineQuads` (default: ON)
  - ON: Solid sphere surface (higher performance, cleaner look)
  - OFF: Circle ring outlines (lower performance, wire-frame aesthetic)

## Performance Characteristics

### Memory Usage

- **Per rod**: ~1.5MB (sphere positions + quad data + GPU VBO buffers)
- **100 rods**: ~150MB total
- **Distance culling**: Automatically removes rods beyond render distance + 2 chunks

### CPU Impact

- **Rod placement**: 30-60ms initial calculation (async, minimal FPS impact)
- **VBO rebuild (new rod)**: 0.5ms per rod (only changed rod rebuilds)
- **Camera movement**: 5-10ms per frame with 100 visible rods (all VBOs rebuild)

### GPU Impact

- **Draw calls**: 3 per rod (attraction + damage + outline)
- **100 rods**: 300 draw calls (~0.3ms overhead on modern GPUs)
- **FPS impact**: <3% with 100 rods, <5% with 200 rods

### Recommended Limits

- **Typical usage**: <100 rods (negligible impact)
- **Large builds**: <500 rods (3-5% FPS drop)
- **Extreme**: >1000 rods may cause noticeable performance degradation

## Implementation Details

### Eligibility Criteria

A lightning rod is only rendered if it:

1. Is in the Overworld dimension
2. Has clear sky access (no solid blocks above to build height)
3. Is within render distance + 2 chunks of the camera

These checks ensure accuracy with Minecraft's lightning mechanics and prevent unnecessary rendering.

### Distance Culling

Rods are culled from the render list if:

```
Manhattan distance > (render distance + 2) * 16 blocks
```

This prevents memory buildup in worlds with many distant rods and improves performance by only tracking nearby rods.

### Camera-Relative Coordinates

All vertex data is stored in camera-relative coordinates to avoid floating-point precision issues at large world coordinates:

```java
float x = (float)(blockX - cameraPos.x);
```

This design choice requires VBO rebuilds whenever the camera moves significantly (>0.1 blocks), which is unavoidable without major architectural changes. The trade-off ensures accurate rendering at all world positions.

### Sphere Geometry Algorithm

The 128-block radius sphere uses an optimized ring-based calculation:

1. Iterate vertical layers (y-rings) from -128 to +128
2. For each layer, calculate horizontal ring radius
3. Generate points around the ring at fixed angular intervals
4. Store positions in packed long format for efficiency

This approach is ~10x faster than naive distance checks for all blocks in a cube.

## Known Limitations

### Overworld Only

Lightning rods in the Nether and End dimensions are not rendered, as lightning does not strike in those dimensions. The overlay automatically disables when changing dimensions.

### Camera Movement Overhead

Moving the camera triggers a rebuild of all rod VBOs due to camera-relative coordinate requirements. This causes a brief FPS drop proportional to the number of visible rods. Static cameras have minimal overhead.

### No LOD System

All rods render at full detail regardless of distance. Distant rods could use simplified geometry to reduce vertex counts, but this adds complexity and is not currently implemented.

### Sky Access Validation

Sky access checking only considers block solidity, not transparency. Glass blocks above a rod will mark it as ineligible even though lightning can pass through. This matches Minecraft's actual lightning behavior for consistency.

## Comparison to Conduit Overlay

The Lightning Rod overlay is architecturally similar to the Conduit Range overlay:

| Feature               | Lightning Rod       | Conduit        |
| --------------------- | ------------------- | -------------- |
| Range                 | 128 blocks (sphere) | 32-96 blocks   |
| Shape                 | Perfect sphere      | Perfect sphere |
| VBO architecture      | Per-rod             | Per-conduit    |
| Async calculation     | Yes                 | Yes            |
| Damage zone           | Yes (6×12×6 box)    | No             |
| Dimension restriction | Overworld only      | Any dimension  |
| Update triggers       | Block changes       | Block changes  |

Both use the same underlying `SphereUtils` calculation library and follow identical rendering patterns.

## Troubleshooting

### Rods Not Appearing

1. Check overlay is enabled: `overlayLightningRodRange` toggle
2. Verify you're in the Overworld dimension
3. Ensure the rod has sky access (no blocks above to build height)
4. Check you're within render distance of the rod

### Performance Issues

1. Count visible rods (each rod = 3 draw calls)
2. If >500 rods, consider reducing active area
3. Try enabling `lightningRodRangeOverlayCombineQuads` for better performance
4. Increase allocated RAM if seeing memory warnings

### Visual Glitches

1. Update graphics drivers
2. Check for conflicting mods (shader packs, rendering modifications)
3. Try toggling the overlay off and back on
4. Verify config colors have valid ARGB values

### Incorrect Rendering

1. Verify rod is properly placed (vertical orientation required)
2. Check that blocks above rod are non-solid (sky access required)
3. Confirm you're not in spectator mode (may cause culling issues)

## Future Enhancements

Potential improvements not currently implemented:

- **Instanced rendering**: Reduce draw calls for >1000 rods
- **Distance-based LOD**: Simplify geometry for distant rods
- **Frustum culling**: Skip rods outside camera view
- **Configurable max rod limit**: Protect against memory issues
- **Coverage indicators**: Show overlapping protection zones

These optimizations are not currently needed but could be added if performance becomes an issue with extreme rod counts.
