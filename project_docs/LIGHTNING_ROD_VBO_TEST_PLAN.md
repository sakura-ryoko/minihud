# Lightning Rod VBO Optimization - Test Plan

**Date:** February 1, 2026  
**Implementation Status:** Phases 1-3 COMPLETED ✅  
**Build Status:** SUCCESSFUL ✅

---

## Overview

This document provides a comprehensive test plan for validating the per-rod VBO optimization implemented in Phases 1-3. The optimization changes lightning rod rendering from batched O(N) rebuilds to incremental O(1) per-rod updates.

---

## Quick Validation Checklist

### Basic Functionality ✓

- [ ] Lightning rod overlay toggles on/off correctly
- [ ] Spheres render at correct positions
- [ ] Damage zones render at correct positions
- [ ] Colors match config settings
- [ ] Outlines render correctly

### Core Optimization ✓

- [ ] Placing rod #2 doesn't rebuild rod #1's VBO (no visible flicker)
- [ ] Removing one rod doesn't affect other rods' rendering
- [ ] Camera movement updates all rods smoothly

### Edge Cases ✓

- [ ] Placing rod at origin (0, 64, 0)
- [ ] Placing rod at large coordinates (1000000, 64, 1000000)
- [ ] Rapid placement (creative mode spam-click)
- [ ] Removing middle rod from group doesn't affect neighbors

---

## Detailed Test Scenarios

### Test 1: Single Rod Placement

**Purpose:** Verify basic functionality and VBO initialization

**Steps:**

1. Start new world (seed: 12345)
2. Enable lightning rod overlay (`/minihud-toggle overlayLightningRodRange`)
3. Place single lightning rod at (0, 64, 0)
4. Verify sphere renders (128 block radius)
5. Verify damage zone renders (6×12×6 box)
6. Verify outlines render

**Expected Results:**

- ✅ Sphere appears instantly (~50ms placement time)
- ✅ Colors match config settings
- ✅ No console errors
- ✅ VBO initialization successful

**Performance Target:**

- Placement time: ~50ms (sphere calculation only)
- Memory usage: ~1.5MB per rod
- No frame drops

---

### Test 2: Multiple Rod Placement (Performance Test)

**Purpose:** Verify O(1) optimization works - placing rod N+1 doesn't rebuild rods 1-N

**Steps:**

1. Place 10 rods in a grid pattern (10 blocks apart)
2. Note placement time for each rod
3. Place rod #11
4. **Critical Check:** Only rod #11 should show VBO build activity
5. Repeat with 50 rods, then 100 rods

**Expected Results:**

- ✅ Placement time stays constant (~50-53ms)
- ✅ No visible flicker on existing rods when placing new rod
- ✅ Frame rate remains stable
- ✅ **Key Metric:** Rod #100 placement time ≈ Rod #10 placement time

**Performance Targets:**

| Existing Rods | Placement Time | Notes                      |
| ------------- | -------------- | -------------------------- |
| 0 (first)     | ~50ms          | Baseline                   |
| 10            | ~51ms          | Should be nearly identical |
| 50            | ~52ms          | Should be nearly identical |
| 100           | ~53ms          | **Target: <60ms**          |

**Before Optimization (for comparison):**

- 10 rods: ~55ms
- 50 rods: ~75ms
- 100 rods: ~100ms

---

### Test 3: Rod Removal

**Purpose:** Verify VBO disposal and memory cleanup

**Steps:**

1. Place 20 rods
2. Remove rod #10 (middle of the list)
3. Verify other rods still render correctly
4. Remove all rods one by one
5. Verify overlay disables cleanly

**Expected Results:**

- ✅ Removing one rod doesn't affect others
- ✅ VBOs properly disposed (no memory leak)
- ✅ Last rod removal triggers cleanup
- ✅ No console errors

**Memory Check:**

- Monitor GPU memory usage
- Should decrease by ~1.5MB per rod removed

---

### Test 4: Camera Movement

**Purpose:** Verify camera-relative coordinate rebuilds work correctly

**Steps:**

1. Place 50 rods
2. Move camera slowly in all directions
3. Move camera quickly (teleport)
4. Observe sphere rendering during movement

**Expected Results:**

- ✅ All 50 rods rebuild VBOs when camera moves >0.1 blocks
- ✅ No visual artifacts or popping
- ✅ Smooth rendering during movement
- ✅ Frame time increases ~5-6% (acceptable)

**Performance Targets:**

- Frame time (50 rods, moving): ~3.15ms (vs 3.0ms before)
- Frame time (100 rods, moving): ~5.3ms (vs 5.0ms before)
- Overhead: <10% increase acceptable

---

### Test 5: Eligibility Changes

**Purpose:** Verify sky access checks and sphere rebuilds

**Steps:**

1. Place lightning rod at (0, 64, 0)
2. Verify sphere renders (rod is eligible)
3. Place glass block at (0, 100, 0) above rod
4. **Critical Check:** Sphere should disappear or dim
5. Remove glass block
6. **Critical Check:** Sphere should reappear

**Expected Results:**

- ✅ Eligibility check triggers sphere rebuild
- ✅ Only affected rod rebuilds (not all rods)
- ✅ Sphere visibility matches eligibility
- ✅ No console errors

---

### Test 6: Config Changes

**Purpose:** Verify config updates trigger VBO rebuilds

**Steps:**

1. Place 10 rods
2. Change attraction color (config or command)
3. Verify all spheres update to new color
4. Change damage zone color
5. Verify all damage zones update
6. Toggle `LIGHTNING_ROD_RANGE_OVERLAY_COMBINE_QUADS`

**Expected Results:**

- ✅ Color changes apply to all rods
- ✅ combineQuads toggle rebuilds geometry
- ✅ Changes take effect immediately
- ✅ No visual glitches

**Note:** Config changes should trigger full rebuild (acceptable)

---

### Test 7: Chunk Loading

**Purpose:** Verify rods in newly loaded chunks are detected

**Steps:**

1. Place 5 rods at spawn
2. Teleport 500 blocks away
3. Place 5 more rods
4. Teleport back to spawn
5. Verify original 5 rods still render
6. Return to second location
7. Verify second set of 5 rods render

**Expected Results:**

- ✅ Rods persist across chunk unload/reload
- ✅ `onChunkLoad()` detects rods when chunks load
- ✅ Distance culling removes far rods (>render distance + 2 chunks)
- ✅ No duplicate rods

---

### Test 8: Dimension Changes

**Purpose:** Verify overlay only works in Overworld

**Steps:**

1. Place 10 rods in Overworld
2. Verify spheres render
3. Enter Nether (place lightning rods there)
4. Verify spheres DO NOT render (lightning doesn't work in Nether)
5. Return to Overworld
6. Verify original 10 rods still render

**Expected Results:**

- ✅ Overlay only active in Overworld
- ✅ No rendering in Nether/End
- ✅ Rods persist when returning to Overworld
- ✅ `forceRescan()` triggers on dimension change

---

### Test 9: Large Coordinates (Edge Case)

**Purpose:** Verify floating-point precision at extreme coordinates

**Steps:**

1. Teleport to (1000000, 64, 1000000)
2. Place lightning rod
3. Verify sphere renders correctly (no Z-fighting or jitter)
4. Move camera around rod
5. Observe for visual artifacts

**Expected Results:**

- ✅ No Z-fighting
- ✅ No coordinate jitter
- ✅ Sphere remains stable
- ✅ Camera-relative coordinates handle large offsets

---

### Test 10: Rapid Placement (Stress Test)

**Purpose:** Verify stability under rapid block changes

**Steps:**

1. Creative mode
2. Fill inventory with lightning rods
3. Spam-click to place 50 rods rapidly (~1 per frame)
4. Observe frame rate and stability
5. Remove all 50 rods rapidly

**Expected Results:**

- ✅ No crashes or freezes
- ✅ Frame rate remains playable (>30 FPS)
- ✅ All rods render correctly after placement storm
- ✅ Memory cleanup works correctly after mass removal

**Performance Target:**

- Frame drops acceptable during spam (temporary)
- Should recover to normal FPS after spam ends

---

### Test 11: Memory Leak Check

**Purpose:** Verify VBOs are properly disposed over time

**Steps:**

1. Enable F3 debug screen (monitor memory)
2. Place 100 rods
3. Note GPU memory usage
4. Remove all 100 rods
5. Wait 10 seconds (let GC run)
6. Verify memory returns to baseline
7. Repeat cycle 10 times

**Expected Results:**

- ✅ Memory usage increases by ~150MB for 100 rods
- ✅ Memory returns to baseline after removal
- ✅ No memory creep after 10 cycles
- ✅ `RodEntry.clear()` disposes VBOs correctly

**Monitoring:**

```
F3 debug: GPU Memory Used
Before: X MB
100 rods: X + 150 MB
After removal: X MB (within 10MB tolerance)
```

---

### Test 12: Mixed Operations

**Purpose:** Verify system handles complex real-world scenarios

**Steps:**

1. Place 20 rods around spawn
2. Move camera 50 blocks away
3. Place 10 more rods
4. Remove 5 rods from first group
5. Change config colors
6. Teleport 200 blocks away
7. Return to spawn
8. Verify all remaining rods render correctly

**Expected Results:**

- ✅ System handles mixed operations gracefully
- ✅ No visual glitches
- ✅ Correct rod count maintained
- ✅ All VBOs up-to-date

---

## Performance Benchmarks

### Baseline Measurements (Record These)

**Test Setup:**

- World seed: 12345
- Location: (0, 64, 0)
- Pre-place rods in 10×10 grid (10 blocks apart)
- Use F3 debug screen + mod timing logs

**Metrics to Record:**

1. **Placement Time** (measure with System.nanoTime()):

   ```
   Rod Count | Time (ms) | Expected
   ----------|-----------|----------
   1st rod   | ?         | ~50ms
   10th rod  | ?         | ~51ms
   50th rod  | ?         | ~52ms
   100th rod | ?         | ~53ms
   ```

2. **Frame Time** (F3 debug, avg over 60 frames):

   ```
   Scenario               | FPS  | Frame Time (ms)
   -----------------------|------|----------------
   No rods, static        | ?    | ?
   100 rods, static       | ?    | ~0.5ms
   100 rods, moving       | ?    | ~5.3ms
   ```

3. **Memory Usage** (F3 debug, GPU memory):
   ```
   Rod Count | GPU Memory Increase
   ----------|---------------------
   10        | ~15 MB
   50        | ~75 MB
   100       | ~150 MB
   ```

### Success Criteria

**Must Meet:**

- ✅ Rod #100 placement time < 60ms
- ✅ No memory leaks (10 placement/removal cycles)
- ✅ No crashes or visual glitches
- ✅ Frame time increase < 10% with 100 rods (moving camera)

**Should Meet:**

- ✅ Rod #100 placement time < 55ms
- ✅ GPU memory per rod < 2MB
- ✅ Frame time increase < 6% with 100 rods

**Nice to Have:**

- ⚠️ Rod #100 placement time < 52ms
- ⚠️ Playable with 500+ rods (>30 FPS)

---

## Known Limitations (Expected Behavior)

### By Design:

1. **Camera movement rebuilds all VBOs** - Necessary for camera-relative coordinates
2. **Draw call increase** - 3 calls → N×3 calls (acceptable trade-off)
3. **Memory usage scales linearly** - ~1.5MB per rod (expected)
4. **Initial sphere calculation still takes 30-60ms** - Not optimized (out of scope)

### Edge Cases (Acceptable):

1. **Slight frame drop during rapid placement** - Temporary, recovers quickly
2. **Config changes trigger full rebuild** - Rare operation, acceptable
3. **Distance culling may cause pop-in** - Intentional memory management

### Not Implemented (Future):

1. **LOD system** - Distant rods use same detail level
2. **Frustum culling** - Rods behind camera still render
3. **Instanced rendering** - Each rod is independent VBO

---

## Regression Checks

Verify these still work (unchanged functionality):

- [ ] Color config options apply correctly
- [ ] combineQuads toggle works
- [ ] Distance culling removes far rods
- [ ] Toggle on/off works
- [ ] Initial world scan finds rods
- [ ] F3+G boundary overlay doesn't interfere
- [ ] Other overlays work normally (conduit, slime chunks, etc.)

---

## Debug Output (If Available)

Add temporary logging to verify optimization:

```java
// In RodEntry.buildVBOs():
System.out.printf("[VBO] Building VBOs for rod at %s%n", this.pos.toShortString());

// In render():
long start = System.nanoTime();
// ... buildVBOs loop ...
long elapsed = (System.nanoTime() - start) / 1_000_000;
System.out.printf("[PERF] Built %d VBOs in %d ms%n", rebuildCount, elapsed);
```

**Expected Output (100 rods, place rod #101):**

```
[VBO] Building VBOs for rod at [0, 64, 0]
[PERF] Built 1 VBOs in 0.5 ms
```

**Before Optimization (for comparison):**

```
[VBO] Building VBOs for rod at [0, 64, 0]
[VBO] Building VBOs for rod at [10, 64, 0]
... (100 more lines) ...
[PERF] Built 101 VBOs in 50 ms
```

---

## Sign-Off

### Implementation Phase Status:

- ✅ Phase 1: RodEntry refactor COMPLETE
- ✅ Phase 2: Renderer refactor COMPLETE
- ✅ Phase 3: Block change optimization COMPLETE
- ⏸️ Phase 4: Testing & Validation IN PROGRESS
- ⏸️ Phase 5: Documentation (pending test results)

### Test Results:

- [ ] Unit tests: PENDING (manual in-game testing)
- [ ] Performance tests: PENDING
- [ ] Visual tests: PENDING
- [ ] Memory tests: PENDING

### Approval:

- [ ] Functionality verified
- [ ] Performance targets met
- [ ] No regressions found
- [ ] Ready for Phase 5 (Documentation)

---

## Next Steps

1. **In-Game Testing:**
   - Load Minecraft 1.21.1 with built mod
   - Run through all test scenarios
   - Record performance metrics
   - Document any issues found

2. **Performance Validation:**
   - Compare placement times (before vs after)
   - Measure frame time impact
   - Verify memory usage acceptable

3. **Issue Resolution:**
   - Fix any bugs discovered
   - Optimize further if targets not met
   - Re-test after fixes

4. **Documentation Update:**
   - Update VBO_OPTIMIZATION_PLAN.md status to COMPLETED
   - Document actual performance results
   - Create changelog entry

---

## Troubleshooting

### If placement time > 60ms for rod #100:

- Check if VBO uploads are batched correctly
- Verify `needsVboRebuild` flag works
- Add logging to identify bottleneck

### If visual glitches appear:

- Verify camera-relative coordinates correct
- Check VBO disposal in `clear()`
- Test with combineQuads=false

### If memory leaks occur:

- Add logging to `clear()` method
- Verify `reset()` in each VBO
- Check for retained references

### If frame rate drops:

- Profile GPU draw calls
- Check if too many VBOs uploading per frame
- Consider batching VBO builds over multiple frames

---

**Test Plan Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** Ready for execution 🚀
