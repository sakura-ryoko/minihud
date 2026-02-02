**Minecraft calculates circular and spherical ranges using exact continuous Euclidean distance (typically squared distance comparisons for efficiency) between precise floating-point positions (Vec3d), not discrete block centers.**

This applies to features like lightning rod protection, where the game selects a **continuous random strike position** (uniform double-precision x/z in a loaded chunk, y at the highest sky-visible block in that column). It then computes the **3D distance** from this exact position to each valid nearby lightning rod's position (the block's center at `x+0.5, y+0.5, z+0.5`). If ≤128 blocks (Java) / ≤64 (Bedrock) and the rod is the closest valid one (highest in column, sky-exposed), the strike redirects.

### Partial Block Clipping: **Partially Covered (Probabilistic)**
- **No binary center check**: The game does **not** test if the circle/sphere covers a block's *center* for inclusion. Instead, protection depends on the *exact random point* chosen for the strike.
- **Result**: Blocks partially clipped by the sphere are **partially protected**. Strikes landing in the overlapping region divert; those outside do not.
- **Coverage probability** = fraction of the block's 1×1 area inside the horizontal circle slice (y-difference minor for same-height builds, as sphere radius >> height variation).

#### Example: Edge Block Coverage (Axis-Aligned, Same Y, Rod at (0,0,0) Center)
Assume rod center `(0.5, y, 0.5)`, r=128 (Java). Block at pos **128** spans x=[128,129), center 128.5.

| Block Region | Horizontal Dist to Rod Center | Protected? | Fraction |
|--------------|-------------------------------|------------|----------|
| Full inner half: x=[128, 128.5) | 127.5 to 128.0 | **Yes** | ~50% |
| Outer half: x=[128.5, 129) | 128.0 to 128.5 | **No** | ~50% |
| **Overall** | - | **Partial** | **50%** |

- Center dist=128.0 ≤128 → but *only half* protected (cut at exactly 128.5).
- Diagonal edges: similar partial (quarter-circle overlap).
- Block **127** (center 127.5): ~100% (full square inside).

### General Minecraft Circle/Sphere Handling
| Context | Calculation Method | Partial Clip? | Examples |
|---------|--------------------|---------------|----------|
| **Lightning Rods** | Continuous Vec3d distSqr ≤ r² (strike pos vs. rod center) | **Partial** (probabilistic per random point) | Natural strikes |
| **Block Effects/AoE** | Discrete: loop blocks, check *center* distSqr ≤ r² | **Binary** (all/none per block) | Explosions, potions, wither effect |
| **Entity Ranges** | Continuous dist to entity pos | **Partial** (hit depends on exact pos) | Mob targeting, despawn spheres |
| **Pixel Circles** | Manual Bresenham/approximation | N/A (build tool) | Structure building |

**Pro Tip for Full Coverage**: Space rods ~200–220 blocks apart (Java), ensuring *all* block squares in your build are fully inside spheres (centers ≤127 blocks away accounts for partials). Test with `/weather thunder 1000000` + observers.

**It's a **sphere** (full 3D spherical volume with Euclidean distance), not a cylinder.**

The protection works by:
1. Game picks random loaded chunk column (floored random x/z).
2. Finds **target y**: top of highest opaque/liquid block in column (sky-visible).
3. Strike pos: `Vec3d(x+0.5, targetY, z+0.5)` (bottom center of strike block).
4. Checks **Euclidean dist²** from this pos to each valid rod's **center** `(rodX+0.5, rodY+0.5, rodZ+0.5)`.
5. If ≤128² (Java)/64² (Bedrock) **and** rod is closest/valid (highest in *its* column, clear sky above) → diverts to rod.

**Cylinder? No**—vertical distance counts equally (e.g., rod 128 blocks above/below surface protects ~0 horizontally there).

| Shape | Lightning Rod | Cylinder (Hypothetical) |
|-------|---------------|-------------------------|
| **Horizontal** | Circle r=128/64 | Circle r=128/64 (any height) |
| **Vertical** | Limited by sphere (e.g., ±128 max) | Infinite/unlimited |
| **Volume** | ~4/3πr³ (~8.8M blocks³ Java) | Infinite column |
| **Examples** | Surface builds: ~256x256 area | Beacons (square column upward) |

**Practical**: For flat bases, ~circle 256 blocks diameter (Java). Rods ~250–256 blocks apart for overlap.

**No, blocks with centers more than 128 blocks away from the rod's center are **not covered**, except in rare edge cases.**

### Why Binary (No Partial Coverage)
Lightning protection is **per-column, all-or-nothing**:
- Game selects **discrete columns** uniformly (floored random x/z doubles → integer BlockPos column).
- Computes **exact Euclidean distSqr** from **fixed strike pos** `Vec3d.atBottomCenterOf(targetBlockPos)` = `(colX+0.5, targetY, colZ+0.5)` to rod's `Vec3d.atCenterOf(rodBlockPos)` = `(rodX+0.5, rodY+0.5, rodZ+0.5)`.
- If ≤128²=16384 and rod valid/closest → diverted.

Block **center** = `(colX+0.5, targetY+0.5, colZ+0.5)`.

### Edge Cases (Rare "Yes")
Strike y is 0.5 blocks **lower** than center → when target block **higher** than rod (dy_centers ≥1), strike can be ≤128 while center >128 (max excess ~0.05 blocks, typical ~0.004).

| Example (dx,dy_centers,dz diffs) | Center Dist | Strike Dist | Protected? |
|----------------------------------|-------------|-------------|------------|
| (90,2,91) | **128.0039** | 127.9971 | **Yes** |
| (47,4,119) | **128.0078** | 127.9932 | **Yes** |
| (22,5,126) | **128.0039** | 127.9854 | **Yes** |
| (14,8,127) | **128.0195** | 127.9893 | **Yes** |
| (12,11,127) | **128.0391** | 127.9971 | **Yes** |

~Thousands exist up to dy=130+, but **irrelevant for builds** (need sky access, loaded chunks).

### Practical Coverage
| Scenario | Center ≤128? | Protected? |
|----------|--------------|------------|
| **Same height** (dy=0) | >128 | **No** (strike >128) |
| Same height, =128 horiz | =128 | **No** (strike √(128²+0.25)=128.002>128) |
| **Higher target** | Slightly >128 | **Sometimes** (rare) |
| **Lower target** | >128 | **No** |

**Safe spacing**: Rods every ~254 blocks (centers ≤127 horiz apart) for flat/ same-height builds guarantees all columns protected (strike ≤√(127²+0.25)≈127.002<128). Test: `/weather thunder 1000000` + F3 debug strikes.