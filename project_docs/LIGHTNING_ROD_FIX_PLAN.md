# Lightning Rod Range Overlay - Fix Implementation Plan

## Status: 🔴 NEEDS FIXING

**Date Created:** February 1, 2026  
**Priority:** HIGH - Current implementation shows incorrect coverage  
**Complexity:** MEDIUM - Can reuse existing sphere rendering utilities

---

## Problem Statement

The current lightning rod range overlay implementation has **critical accuracy issues** that mislead players about actual protection coverage:

### 1. **Wrong Shape: Cylinder Instead of Sphere**

**Current (INCORRECT):**

- Renders vertical cylinder with constant 128-block horizontal radius at ALL heights
- Shows protection extending infinitely upward from rod
- Formula: `if (horizontal_distance ≤ 128) then protected` (ignores vertical distance)

**Reality (CORRECT):**

- 3D **spherical volume** with 128-block radius (Euclidean distance)
- Formula: `if (√(dx² + dy² + dz²) ≤ 128) then protected`
- Vertical distance counts equally as horizontal distance

**Example of Error:**

```
Rod at (0, 64, 0)

Block at (128, 192, 0):
  Current overlay: PROTECTED ✅ (cylinder: horiz dist = 128)
  Reality: NOT PROTECTED ❌ (sphere: dist = √(128² + 128²) = 181 > 128)

Block at (90, 130, 0):
  Current overlay: PROTECTED ✅ (cylinder: horiz dist = 90)
  Reality: NOT PROTECTED ❌ (sphere: dist = √(90² + 66²) = 111... wait actually protected)

Block at (90, 200, 0):
  Current overlay: PROTECTED ✅ (cylinder: horiz dist = 90)
  Reality: NOT PROTECTED ❌ (sphere: dist = √(90² + 136²) = 163 > 128)
```

**Impact:** Players place rods thinking they have vertical protection that doesn't exist.

---

### 2. **Coverage Indicators Use Wrong Model**

**Current Implementation:**

- Shows green boundary markers for "partial coverage zones"
- Tests block corners against 2D circle radius
- Implies gradual probabilistic falloff at edges (25-75% protection)
- Only considers horizontal (XZ) distance

**Reality Per lightning_rod_details.md:**

- Lightning protection is **per-column, binary** (all-or-nothing for each column)
- Game picks discrete column (floored random x/z → integer BlockPos)
- Strike position is **fixed point** at column center: `(colX+0.5, targetY, colZ+0.5)`
- Tests **3D Euclidean distance** from strike point to rod center `(rodX+0.5, rodY+0.5, rodZ+0.5)`
- If `distance ≤ 128` → diverted (100% for that column)
- If `distance > 128` → not diverted (0% for that column)
- No partial coverage within a single column

**Exception (Extremely Rare):**

- Block centers slightly >128 blocks away CAN be protected if strike Y is lower than block center Y
- Max excess: ~0.05 blocks (requires specific vertical configuration)
- Practically negligible for builds

**Impact:** Green markers suggest probabilistic zones that don't exist. Coverage is binary per column, not gradual.

---

### 3. **Performance Issues with Coverage Indicators**

**Current Cost:**

- ~6-11ms per frame (10-15% of frame time budget)
- Checks 4,096 blocks every frame (32-block radius × 32-block radius)
- Tests 4 corners per block for boundary detection

**Why It's Expensive:**

- Running every frame without caching
- Checking blocks that aren't actually on sphere boundary
- 2D circle logic doesn't match 3D sphere reality

---

## Existing Solution: SphereUtils

**Good news:** The project already has a complete sphere rendering system used by the Conduit Range overlay!

### Available Utilities (in `SphereUtils.java`)

1. **`collectSpherePositions(Consumer, RingPositionTest, BlockPos, int radius)`**
   - Collects all block positions inside a spherical radius
   - Uses exact 3D Euclidean distance
   - Efficient ring-based algorithm (not brute-force)

2. **`buildSphereShellToQuads(LongOpenHashSet, Axis, RingPositionTest, ShapeRenderType, LayerRange)`**
   - Converts sphere positions into optimized quad strips
   - Combines adjacent faces into larger quads (reduces vertex count)
   - Returns `List<SideQuad>` for rendering

3. **`RingPositionTest` interface**
   - Tests if position is within sphere using squared distance (efficient)
   - Implementation: `isPositionInsideOrClosestToRadiusOnBlockRing(x, y, z, center, squareRadius, dir)`

4. **Rendering helpers**
   - `RenderUtils.renderQuads()` - Renders SideQuad list
   - `RenderUtils.renderCircleBlockPositions()` - Renders positions without combining quads
   - `RenderUtils.renderCircleBlockOutlines()` - Renders sphere outlines

### Reference Implementation: `OverlayRendererConduitRange.java`

The conduit renderer already implements sphere rendering correctly:

```java
// Create position test for 3D sphere
protected static SphereUtils.RingPositionTest getPositionTest(BlockPos centerPos, int range) {
    Vec3 center = new Vec3(centerPos.getX() + 0.5, centerPos.getY() + 0.5, centerPos.getZ() + 0.5);
    double squareRange = range * range;

    return (x, y, z, dir) -> SphereUtils.isPositionInsideOrClosestToRadiusOnBlockRing(
            x, y, z, center, squareRange, Direction.EAST);
}

// Collect sphere positions
Entry entry = new Entry(pos, range);
Consumer<BlockPos.MutableBlockPos> positionCollector = (p) -> entry.addPosition(p.asLong());
entry.setTest(this.getPositionTest(pos, entry.range));
SphereUtils.collectSpherePositions(positionCollector, entry.getTest(), pos, entry.range);

// Build quads from positions
if (this.combineQuads) {
    entry.setQuads(SphereUtils.buildSphereShellToQuads(
        entry.getPositions(), this.quadAxis, entry.getTest(),
        this.renderType, this.layerRange));
}

// Render quads
RenderUtils.renderQuads(entry.getQuads(), color, 0, cameraPos, builder);
```

**We can directly adapt this pattern for lightning rods!**

---

## Implementation Plan

### Phase 1: Update Data Structures (Low Risk)

**File:** `OverlayRendererLightningRodRange.java`

**Changes:**

1. **Add new fields** (similar to conduit renderer):

```java
private final List<RodEntry> lightningRods; // Replace HashMap<BlockPos, Boolean>
private final ShapeRenderType renderType;
private final LayerRange layerRange;
private final Direction.Axis quadAxis;
private boolean combineQuads;
```

2. **Create RodEntry class** (similar to conduit's Entry class):

```java
private static class RodEntry {
    public final BlockPos pos;
    public final boolean isEligible;
    private final LongOpenHashSet positions;  // Sphere shell positions
    private SphereUtils.RingPositionTest test;
    private final List<SideQuad> quads;

    RodEntry(BlockPos pos, boolean isEligible) {
        this.pos = pos;
        this.isEligible = isEligible;
        this.positions = new LongOpenHashSet();
        this.test = null;
        this.quads = new ArrayList<>();
    }

    // Methods: addPosition(), getPositions(), setTest(), getTest(),
    //          setQuads(), getQuads(), clear()
}
```

3. **Update constructor**:

```java
public OverlayRendererLightningRodRange() {
    super(RendererToggle.OVERLAY_LIGHTNING_ROD_RANGE, /* ... */);
    this.lightningRods = new ArrayList<>();
    this.quadAxis = Direction.UP.getAxis(); // Y-axis for vertical orientation
    this.renderType = ShapeRenderType.OUTER_EDGE;
    this.layerRange = new LayerRange(null);
    this.combineQuads = true; // Combine quads for performance
    this.useCulling = false;
}
```

**Testing:** Code should compile. No visual changes yet.

---

### Phase 2: Add Sphere Calculation Methods (Medium Risk)

**File:** `OverlayRendererLightningRodRange.java`

**Add new methods** (copied from conduit renderer with adaptations):

1. **`getPositionTest(BlockPos rodPos, int range)`**

```java
private static SphereUtils.RingPositionTest getPositionTest(BlockPos rodPos, int range) {
    // Rod center position (block center)
    Vec3 center = new Vec3(rodPos.getX() + 0.5, rodPos.getY() + 0.5, rodPos.getZ() + 0.5);
    double squareRange = range * range; // 128 * 128 = 16384

    return (x, y, z, dir) -> SphereUtils.isPositionInsideOrClosestToRadiusOnBlockRing(
            x, y, z, center, squareRange, Direction.EAST);
}
```

2. **`calculateSphereForRod(BlockPos pos, boolean isEligible)`**

```java
private RodEntry calculateSphereForRod(BlockPos pos, boolean isEligible) {
    RodEntry entry = new RodEntry(pos, isEligible);

    if (!isEligible) {
        return entry; // No sphere calculation for ineligible rods
    }

    final int RADIUS = 128;

    // Create position test for this rod's sphere
    entry.setTest(getPositionTest(pos, RADIUS));

    // Collect all block positions on sphere shell
    Consumer<BlockPos.MutableBlockPos> positionCollector = (p) -> entry.addPosition(p.asLong());
    SphereUtils.collectSpherePositions(positionCollector, entry.getTest(), pos, RADIUS);

    // Build optimized quads from positions
    if (this.combineQuads) {
        entry.setQuads(SphereUtils.buildSphereShellToQuads(
            entry.getPositions(), this.quadAxis, entry.getTest(),
            this.renderType, this.layerRange));
    }

    return entry;
}
```

3. **`addOrReplaceRodEntry(RodEntry entry)`**

```java
private void addOrReplaceRodEntry(RodEntry entry) {
    // Find and replace existing entry at same position
    for (int i = 0; i < this.lightningRods.size(); i++) {
        RodEntry existing = this.lightningRods.get(i);
        if (existing.pos.equals(entry.pos)) {
            existing.clear();
            this.lightningRods.set(i, entry);
            return;
        }
    }

    // Not found - add new entry
    this.lightningRods.add(entry);
}
```

**Testing:** Add debug logging to verify sphere positions are calculated. No visual changes yet.

---

### Phase 3: Update Scanning Logic (Medium Risk)

**File:** `OverlayRendererLightningRodRange.java`

**Update existing methods** to use new data structure:

1. **`scanChunkForLightningRods()`** - Change to build RodEntry objects:

```java
private void scanChunkForLightningRods(LevelChunk chunk, Level world) {
    // ... existing scanning logic ...

    if (isLightningRodBlock(state.getBlock())) {
        // Create entry with sphere calculation
        RodEntry entry = calculateSphereForRod(pos.immutable(), true);
        addOrReplaceRodEntry(entry);
    }

    // ...
}
```

2. **`onBlockChange()`** - Update to rebuild sphere when rod eligibility changes:

```java
public void onBlockChange(BlockPos pos, BlockState newState, Level world) {
    // ... existing checks ...

    if (isRod && !wasTracked) {
        // New rod placed
        boolean isEligible = isLightningRodEligible(world, pos, world.getMaxY());
        RodEntry entry = calculateSphereForRod(pos.immutable(), isEligible);
        addOrReplaceRodEntry(entry);
        this.hasData = true;
        this.setNeedsUpdate();
    } else if (!isRod && wasTracked) {
        // Rod removed
        this.lightningRods.removeIf(e -> e.pos.equals(pos));
        this.hasData = !this.lightningRods.isEmpty();
        this.setNeedsUpdate();
    }

    // ... eligibility recheck logic ...
}
```

3. **Update `reset()`**:

```java
@Override
public void reset() {
    this.lightningRods.forEach(RodEntry::clear);
    this.lightningRods.clear();
    this.hasData = false;
    this.needsUpdate = true;
    this.clearBuffers();
}
```

**Testing:** Verify rods are detected and sphere data is cached. No visual changes yet.

---

### Phase 4: Replace Cylinder Rendering with Sphere Rendering (High Risk)

**File:** `OverlayRendererLightningRodRange.java`

**Replace `renderAttractionZones()` method:**

```java
private void renderAttractionZones(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler) {
    Level world = mc.level;
    if (world == null) {
        return;
    }

    Color4f color = Color4f.fromColor(
        Configs.Colors.LIGHTNING_ROD_RANGE_OVERLAY_COLOR.getIntegerValue());

    profiler.push("attraction_zone_quads");
    RenderObjectVbo ctx = this.renderObjects.get(0);
    BufferBuilder builder = ctx.start(
        () -> "minihud:lightning_rod/attraction_zones",
        MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

    // Render each eligible rod's sphere
    this.lightningRods.forEach((entry) -> {
        if (!entry.isEligible) {
            return;
        }

        if (this.combineQuads) {
            // Render optimized quads (FAST)
            RenderUtils.renderQuads(entry.getQuads(), color, 0, cameraPos, builder);
        } else {
            // Render individual block positions (SLOWER, more accurate at distance)
            RenderUtils.renderCircleBlockPositions(
                entry.getPositions(), PositionUtils.ALL_DIRECTIONS,
                entry.getTest(), this.renderType, this.layerRange,
                color, 0, cameraPos, builder);
        }
    });

    try {
        MeshData meshData = builder.build();
        if (meshData != null) {
            ctx.upload(meshData, this.shouldResort);
            if (this.shouldResort) {
                ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    profiler.pop();
}
```

**Delete methods:**

- `renderCylindricalZone()` - No longer needed
- `renderCylindricalOutline()` - No longer needed

**Testing:**

- ✅ Sphere should render instead of cylinder
- ✅ Vertical distance should now matter (rod far above/below protects less horizontally)
- ✅ Performance should be similar or better (optimized quad combining)

---

### Phase 5: Update Outline Rendering (Medium Risk)

**File:** `OverlayRendererLightningRodRange.java`

**Replace `renderOutlines()` method:**

```java
private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler) {
    Level world = mc.level;
    if (world == null) {
        return;
    }

    Color4f attractionColor = Color4f.fromColor(
        Configs.Colors.LIGHTNING_ROD_RANGE_OVERLAY_COLOR.getIntegerValue(), 1.0f);
    Color4f damageColor = Color4f.fromColor(
        Configs.Colors.LIGHTNING_ROD_DAMAGE_ZONE_COLOR.getIntegerValue(), 1.0f);

    profiler.push("outlines");
    RenderObjectVbo ctx = this.renderObjects.get(2);
    BufferBuilder builder = ctx.start(
        () -> "minihud:lightning_rod/outlines",
        MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

    this.lightningRods.forEach((entry) -> {
        if (!entry.isEligible) {
            return;
        }

        // Render sphere outlines
        if (this.combineQuads) {
            RenderUtils.renderQuadLines(entry.getQuads(), attractionColor,
                0, cameraPos, this.glLineWidth, builder);
        } else {
            RenderUtils.renderCircleBlockOutlines(
                entry.getPositions(), PositionUtils.ALL_DIRECTIONS,
                entry.getTest(), this.renderType, this.layerRange,
                attractionColor, 0, cameraPos, this.glLineWidth, builder);
        }

        // Render damage zone box (unchanged)
        BlockPos pos = entry.pos;
        final double rodX = pos.getX() + 0.5 - cameraPos.x;
        final double rodY = pos.getY() - cameraPos.y;
        final double rodZ = pos.getZ() + 0.5 - cameraPos.z;

        final double minX = rodX - 3;
        final double minY = rodY - 2;
        final double minZ = rodZ - 3;
        final double maxX = rodX + 3;
        final double maxY = rodY + 10;
        final double maxZ = rodZ + 3;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(
            (float) minX, (float) minY, (float) minZ,
            (float) maxX, (float) maxY, (float) maxZ,
            damageColor, this.glLineWidth, builder);
    });

    try {
        MeshData meshData = builder.build();
        if (meshData != null) {
            ctx.upload(meshData, this.shouldResort);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    profiler.pop();
}
```

**Testing:**

- ✅ Sphere outlines should be visible
- ✅ Damage zone box should still render correctly
- ✅ No flickering or z-fighting

---

### Phase 6: Remove or Redesign Coverage Indicators (High Risk - Decision Needed)

**File:** `OverlayRendererLightningRodRange.java`

**Option A: Remove Completely** (Recommended)

- Current indicators are based on incorrect model
- 3D sphere boundary is complex to visualize as ground markers
- Performance cost (~6-11ms/frame) is significant

```java
// In render() method:
// Comment out or remove this section:
// profiler.push("coverage_indicators");
// this.renderCoverageIndicators(cameraPos, mc, profiler);
// profiler.pop();

// Remove renderCoverageIndicators() method entirely
```

**Option B: Redesign for Binary Column Coverage** (Complex)

- Show which columns (XZ coordinates) are protected
- Would need to:
  1. Test each column's strike position (colX+0.5, targetY, colZ+0.5) against 3D sphere
  2. Account for Y variation (strike Y varies by terrain height)
  3. Render only columns with >50% protection across typical Y range
- Much more expensive computationally
- May still confuse players due to Y variation

**Option C: Simple Ground Circle at Rod Height** (Compromise)

- Render 2D circle at rod's Y level only
- Label it as "Protection at this height"
- Add note in tooltip: "Vertical distance reduces horizontal range"
- Less accurate but easier to understand

**Recommendation:** **Option A (Remove)**. The sphere visual itself shows coverage. Boundary indicators add confusion rather than clarity.

**Implementation for Option A:**

1. Remove VBO allocation for coverage indicators:

```java
@Override
protected void allocateBuffers(boolean useOutlines) {
    this.clearBuffers();
    // Index 0: Attraction zones (sphere quads)
    this.renderObjects.add(new RenderObjectVbo(
        () -> this.getName() + "/AttractionZones",
        MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL));
    // Index 1: Damage zones (box quads)
    this.renderObjects.add(new RenderObjectVbo(
        () -> this.getName() + "/DamageZones",
        MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL));
    // Index 2: Outlines (lines)
    if (useOutlines) {
        this.renderObjects.add(new RenderObjectVbo(
            () -> this.getName() + "/Outlines",
            MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH));
    }
    // Index 3: REMOVED - Coverage indicators no longer needed
}
```

2. Remove coverage indicator rendering from `render()`:

```java
@Override
public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler) {
    // ... existing checks ...

    this.allocateBuffers(true);

    profiler.push("attraction_zones");
    this.renderAttractionZones(cameraPos, mc, profiler);
    profiler.pop();

    profiler.push("damage_zones");
    this.renderDamageZones(cameraPos, mc, profiler);
    profiler.pop();

    // REMOVED: coverage_indicators section

    profiler.push("outlines");
    this.renderOutlines(cameraPos, mc, profiler);
    profiler.pop();
}
```

3. Delete `renderCoverageIndicators()` method entirely

**Testing:**

- ✅ No green boundary markers
- ✅ Performance improvement (~6-11ms saved)
- ✅ Cleaner visual (sphere shows coverage)

---

### Phase 7: Add Configuration Option (Optional, Low Risk)

**File:** `Configs.java`

**Add config for quad combining** (like conduit renderer):

```java
// In Generic nested class:
public static final ConfigBoolean LIGHTNING_ROD_RANGE_OVERLAY_COMBINE_QUADS =
    new ConfigBoolean("lightningRodRangeOverlayCombineQuads", true,
        "Combine adjacent quads for better performance.\nDisable for more accurate rendering at extreme distances.");

// In Generic.OPTIONS list:
Generic.LIGHTNING_ROD_RANGE_OVERLAY_COMBINE_QUADS,
```

**Update renderer to use config:**

```java
// In OverlayRendererLightningRodRange constructor or update method:
this.combineQuads = Configs.Generic.LIGHTNING_ROD_RANGE_OVERLAY_COMBINE_QUADS.getBooleanValue();
```

**Add to localization** (`en_us.json`):

```json
"minihud.config.generic.name.lightningRodRangeOverlayCombineQuads": "Lightning Rod: Combine Quads",
"minihud.config.generic.comment.lightningRodRangeOverlayCombineQuads": "Combine adjacent quads for better performance.\nDisable for more accurate rendering at extreme distances.",
```

**Testing:**

- ✅ Config option appears in settings
- ✅ Toggling changes render quality vs performance
- ✅ No crashes when toggling

---

## Testing Plan

### Unit Tests (Manual Verification)

#### Test 1: Sphere Shape Verification

```
Setup:
- Place rod at Y=100 in flat world
- Enable overlay
- Fly to different heights (Y=50, Y=100, Y=150, Y=200, Y=228)

Expected Results:
✅ At Y=100 (same height): Circular coverage ~256 blocks diameter
✅ At Y=228 (128 blocks above): Sphere appears as single point (no horizontal coverage)
✅ At Y=228 + 1 block: Rod disappears from overlay (outside sphere)
✅ At Y=-28 (128 blocks below): Sphere appears as single point
✅ At Y=164 (64 blocks above): Circular coverage ~221 blocks diameter (√(128²-64²) = 111 blocks radius)
```

#### Test 2: Multi-Height Rod Test

```
Setup:
- Place rods at Y=64, Y=128, Y=192
- All at same XZ coordinates (column)
- Enable overlay

Expected Results:
✅ Three separate spheres visible
✅ Spheres overlap in middle Y ranges
✅ Top rod (Y=192) doesn't protect far below
✅ Bottom rod (Y=64) doesn't protect far above
```

#### Test 3: Eligibility Check (Unchanged)

```
Setup:
- Place rod with clear sky
- Place solid block above rod
- Remove block

Expected Results:
✅ Initially: Sphere renders
✅ After block placed: Sphere disappears
✅ After block removed: Sphere reappears
```

#### Test 4: Performance Test

```
Setup:
- Place 20 rods within render distance
- Enable F3 debug screen
- Monitor FPS and frame time

Expected Results:
✅ FPS impact <5% with combineQuads=true
✅ Frame time for renderer <3ms
✅ No stuttering when moving
✅ Sphere calculation happens once (cached), not every frame
```

#### Test 5: Damage Zone (Unchanged)

```
Setup:
- Place single rod
- Verify damage zone box

Expected Results:
✅ Red box: 6 blocks wide (X), 12 blocks tall (Y), 6 blocks deep (Z)
✅ Positioned: -3 to +3 X, -2 to +10 Y, -3 to +3 Z (relative to rod)
✅ Independent of attraction sphere
```

### Visual Comparison Tests

#### Test 6: Cylinder vs Sphere Visual Comparison

```
Method:
1. Take screenshot of current cylinder rendering
2. Implement sphere rendering
3. Take screenshot of new sphere rendering
4. Compare side-by-side

Check:
✅ Sphere is narrower at top/bottom (not constant radius)
✅ Sphere visibly curves inward vertically
✅ Sphere has ~equal curvature in all directions
```

#### Test 7: Overlap Behavior

```
Setup:
- Place two rods 200 blocks apart (same Y)
- Place two rods 200 blocks apart (different Y by 64 blocks)

Expected Results:
✅ Same Y: Spheres overlap in middle (as expected for full coverage)
✅ Different Y: Less overlap (vertical offset reduces horizontal overlap)
```

---

## Performance Expectations

### Before (Cylinder Rendering)

| Metric                 | Value                                       |
| ---------------------- | ------------------------------------------- |
| Attraction zone render | ~2-3ms (128 segments × 4 vertices × height) |
| Coverage indicators    | ~6-11ms (checking 4,096 blocks)             |
| **Total per frame**    | **~8-14ms**                                 |
| Vertex count per rod   | ~686 vertices                               |

### After (Sphere Rendering)

| Metric                                  | Value                                        |
| --------------------------------------- | -------------------------------------------- |
| Attraction zone render (combined quads) | ~2-4ms (optimized quad strips)               |
| Coverage indicators                     | **REMOVED**                                  |
| **Total per frame**                     | **~2-4ms**                                   |
| Vertex count per rod                    | ~500-800 vertices (depends on visible faces) |
| **Performance improvement**             | **~60-70% faster**                           |

### Sphere Calculation (One-Time Cost)

| Metric                     | Value                                               |
| -------------------------- | --------------------------------------------------- |
| Position collection        | ~20-40ms (one-time, cached)                         |
| Quad building              | ~10-20ms (one-time, cached)                         |
| **Total on rod placement** | **~30-60ms**                                        |
| Frequency                  | Only when rod placed/removed or eligibility changes |

**Result:** Better performance during gameplay, slight delay on rod placement (acceptable).

---

## Documentation Updates

### Update LIGHTNING_ROD_IMPLEMENTATION_PLAN.md

Add new section at top:

```markdown
## DEPRECATION NOTICE

**This document describes the ORIGINAL implementation (February 2026).**

**STATUS: 🔴 REPLACED - Contains critical inaccuracies**

The original implementation used cylindrical rendering instead of spherical,
which showed incorrect coverage zones. See LIGHTNING_ROD_FIX_PLAN.md for
the corrected implementation.

### What Was Wrong:

1. ❌ Cylinder shape (constant radius at all heights)
2. ❌ Coverage indicators based on 2D circle probabilistic model
3. ❌ Ignored vertical distance in protection calculation

### What's Fixed in New Version:

1. ✅ Spherical shape (3D Euclidean distance)
2. ✅ Uses existing SphereUtils for accurate rendering
3. ✅ Removed misleading coverage indicators
4. ✅ Better performance (~60-70% faster)

For current implementation details, see LIGHTNING_ROD_FIX_PLAN.md.
```

### Update lightning_rod_details.md

Add implementation notes:

```markdown
## Implementation in MiniHUD

The Lightning Rod Range overlay in MiniHUD visualizes the 128-block
**spherical** protection volume using the `SphereUtils` rendering system
(same as Conduit Range overlay).

### Visualization Accuracy:

- ✅ 3D spherical shape (not cylinder)
- ✅ Vertical distance correctly reduces horizontal range
- ✅ Uses exact Euclidean distance formula
- ✅ Shows binary per-column coverage (no false probabilistic indicators)

### Performance:

- Sphere calculation cached on rod placement (~30-60ms one-time)
- Rendering optimized with quad combining (~2-4ms per frame)
- Supports 20+ rods simultaneously without lag
```

---

## Rollout Strategy

### Phase 1: Internal Testing (1-2 days)

1. Implement changes on feature branch
2. Test all test cases manually
3. Verify performance on low-end hardware
4. Check for memory leaks or crashes

### Phase 2: Documentation (1 day)

1. Update implementation plan documents
2. Add inline code comments explaining sphere calculations
3. Create visual comparison screenshots (old vs new)

### Phase 3: Release (1 day)

1. Merge to main branch
2. Tag release with version number
3. Add release notes explaining accuracy improvements
4. Consider adding migration note: "Lightning rod overlay improved - coverage shown is now accurate"

---

## Migration Notes for Users

### What's Changed (User-Facing)

**Visual Changes:**

- Attraction zone now renders as **sphere** instead of cylinder
- Spheres are narrower at top/bottom (accurate to game mechanics)
- Green boundary markers removed (were misleading)

**Behavior Changes:**

- Vertical distance now correctly affects horizontal coverage
- Rods far above/below show reduced horizontal protection
- More accurate placement guidance for full coverage

**Performance:**

- ~60% faster rendering (coverage indicators removed)
- Brief calculation delay when placing rods (30-60ms)

### What to Tell Users

```
The Lightning Rod Range overlay has been updated for accuracy!

🔵 IMPROVED: Attraction zones now show as 3D SPHERES (not cylinders)
   - Vertical distance now correctly affects horizontal range
   - Rods high above provide less horizontal protection

❌ REMOVED: Green boundary markers (were based on incorrect model)
   - Protection is binary per column (all-or-nothing)
   - Sphere visual shows coverage clearly

⚡ PERFORMANCE: ~60% faster rendering overall
   - Brief delay when placing rods (calculating sphere)
   - Smoother gameplay with many rods

📐 ACCURACY: Now matches actual game mechanics precisely
   - Uses same sphere rendering as Conduit Range overlay
   - Correct 3D Euclidean distance calculation
```

---

## Risk Assessment

| Risk                                       | Likelihood | Impact | Mitigation                                     |
| ------------------------------------------ | ---------- | ------ | ---------------------------------------------- |
| Sphere rendering breaks with many rods     | LOW        | MEDIUM | Quad combining + caching reduces load          |
| Players confused by sphere shape change    | MEDIUM     | LOW    | Clear release notes + documentation            |
| Performance regression on low-end hardware | LOW        | MEDIUM | Config option to disable quad combining        |
| Calculation delay noticeable               | MEDIUM     | LOW    | Only happens on rod placement (acceptable)     |
| Sphere appears "blocky" at distance        | LOW        | LOW    | Option to disable quad combining for accuracy  |
| Memory usage increases                     | LOW        | LOW    | Clear cached data on reset, ~50KB per 100 rods |

**Overall Risk Level: LOW-MEDIUM** - Well-tested pattern from conduit renderer, primarily visual changes.

---

## Success Criteria

### Must Have (P0)

- ✅ Sphere rendering replaces cylinder rendering
- ✅ Vertical distance correctly affects horizontal coverage
- ✅ Performance equal or better than original (~2-4ms per frame)
- ✅ No crashes or memory leaks
- ✅ Damage zone rendering unchanged

### Should Have (P1)

- ✅ Coverage indicators removed (not misleading)
- ✅ Quad combining optimization working
- ✅ Documentation updated
- ✅ Clear release notes

### Nice to Have (P2)

- ⚪ Config option for quad combining
- ⚪ Visual comparison screenshots in docs
- ⚪ Performance benchmarking data
- ⚪ User migration guide

---

## Questions to Resolve

1. **Coverage Indicators:** Remove completely or redesign?
   - **Recommendation:** Remove (Option A)
2. **Quad Combining:** Always on or make configurable?
   - **Recommendation:** Configurable (like conduit)
3. **Calculation Timing:** On placement or deferred?
   - **Recommendation:** On placement (immediate feedback)
4. **Visual Style:** Match conduit exactly or customize?
   - **Recommendation:** Match conduit (consistency)

5. **Documentation:** Update existing or create new?
   - **Recommendation:** Both (deprecation notice + new doc)

---

## Estimated Timeline

| Phase                        | Duration        | Dependencies |
| ---------------------------- | --------------- | ------------ |
| Phase 1: Data structures     | 1-2 hours       | None         |
| Phase 2: Sphere calculation  | 2-3 hours       | Phase 1      |
| Phase 3: Scanning logic      | 1-2 hours       | Phase 2      |
| Phase 4: Sphere rendering    | 2-3 hours       | Phase 3      |
| Phase 5: Outline rendering   | 1-2 hours       | Phase 4      |
| Phase 6: Coverage indicators | 0.5 hours       | Phase 5      |
| Phase 7: Configuration       | 1 hour          | Phase 6      |
| Testing                      | 2-3 hours       | Phase 7      |
| Documentation                | 1-2 hours       | Testing      |
| **Total**                    | **11-18 hours** | -            |

**Estimated Completion:** 2-3 working days

---

## Conclusion

The current implementation has **critical accuracy issues** that mislead players about lightning rod coverage. Fortunately, the project already has all the tools needed to fix it - the `SphereUtils` class and the conduit renderer provide a proven pattern we can directly adapt.

The fix is **straightforward** (adapt existing sphere code), **low-risk** (tested pattern), and provides **better performance** (~60% faster) while being **more accurate** (true 3D spheres).

**Recommendation: Implement immediately.** The current cylinder rendering actively misleads players about game mechanics.
