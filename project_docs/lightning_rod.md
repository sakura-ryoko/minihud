# Lightning Rods in Minecraft Java Edition

Lightning rods are copper-based utility blocks introduced in 1.17. They redirect natural lightning strikes from thunderstorms within a **spherical 128-block radius** (Java Edition exclusive), preventing fires on structures, emit redstone signals when struck, damage nearby mobs, and oxidize like other copper blocks.

## Obtaining

### Crafting
Crafted with **1 pointed dripstone** in the top-center slot and **3 copper ingots** stacked vertically in the middle column of the crafting table.

| Name                     | Ingredients                                      | Crafting Recipe Description                     |
|--------------------------|--------------------------------------------------|-------------------------------------------------|
| Lightning Rod            | 3× Copper Ingot +<br>1× Pointed Dripstone        | Pointed Dripstone: top-center; Copper Ingots: middle column. |
| Waxed Lightning Rod      | 1× Unwaxed Lightning Rod (any oxidation) +<br>1× Honeycomb | Any positions in grid. |

### Breaking
Must be mined with a **stone pickaxe or better** (drops nothing otherwise). **Hardness: 3**, **Blast Resistance: 6**.

| Tool      | Breaking Time (s) |
|-----------|-------------------|
| Default   | 15                |
| Wooden    | 7.5               |
| Stone     | 1.15              |
| Copper    | 0.9               |
| Iron      | 0.75              |
| Diamond   | 0.6               |
| Netherite | 0.5               |
| Golden    | 1.25              |

**Loot Table**: Standard self-drop with correct tool (no special NBT or conditions noted).

## Mechanics and Behavior

### Lightning Diversion
- Redirects **natural lightning strikes** from thunderstorms within a **spherical volume of 128-block radius** centered on the rod.
- **Must be the highest block in its column**: No blocks (excluding air) at **any position above it** (Java Edition specific).
- Emits **blue particles** during thunderstorms to indicate eligibility.
- **Multiple rods**: Closest to the **original intended strike location** is selected.
- Can be struck by **Channeling-enchanted trident** during thunderstorms if **all blocks directly above are fully transparent** (do not block skylight).
- **Does not divert**:
  - Command-summoned lightning (`/summon lightning_bolt`).
  - Entity-targeted strikes.
  - Channeling trident thrown directly at mobs.
- No grounding required; functions anywhere, including waterlogged underwater.
- Strikes on rods **do not summon skeleton trap horses**.

**Placement Tips**: Highest point on rooftops. Space ~256 blocks apart (diameter coverage). Direct sky access required.

### Effects When Struck
- **Visual**: Rod **turns white** briefly.
- **Redstone**: Emits pulse, **strongly powers** attached block (**level 15**) for **8 game ticks (0.4 seconds)**. Sets `powered` state to `true`.
- **Mob Damage**: Inflicts lightning damage (5 ♥ ♥ ♥ ♥ ♥, sets on fire) on mobs in **6×12×6 volume centered 4 blocks above the bottom center of the rod** (extends **2 blocks below rod bottom**, **9 blocks above rod top**). Multiple checks applied; damage immunity limits to ~5 HP per mob. Horizontal: ~3 blocks each direction from center.
- **Fire Risk**: Attempts to ignite **3×3×3 volume around the air block directly above the rod**. Fire spawns only if air block is atop **solid full-top block** or **adjacent to flammable**. Effective **~2-block radius danger zone sphere** (adjacent blocks). **Rod and base block never ignite**—clear flammables in zone above rod.
- **Oxidation**: Fully resets rod's oxidation (if unwaxed). May visually affect nearby copper (unconfirmed mechanic).

**Unique Sound**: Channeling trident strike plays **trident thunder crack** (`item.trident.thunder`).

### Oxidation (Added 1.21.9)
Non-waxed rods progress through **4 stages** (time-based, influenced by nearby unwaxed copper/air exposure):
1. **Normal**: Orange.
2. **Exposed**: Discolored with green spots.
3. **Weathered**: Green with brown spots.
4. **Oxidized**: Teal with green spots.

- **Deoxidize**: Lightning strike (**full reset**) or axe (**one stage**; white particles).
- **Wax**: Honeycomb stops progression (**yellow particles**).
- **Scrape**: Axe removes wax/oxidation (**white particles**).
- Oxidized rods still function (no impairment noted).

### Properties
- **Orientable**: Anchors `facing` up/down/north/south/east/west.
- **Transparent**: Yes (partial).
- **Luminous**: No.
- **Flammable**: No.
- **Waterloggable**: Yes.
- **Renewable**: Yes (copper from dripstone caves).
- **Stackable**: 64.
- **Map Color**: Orange (#COLOR_ORANGE).
- **Sounds**: Copper block type.
  - **Generic**: `block.copper.break/place/hit/step/fall` (volumes/pitches as standard).
  - **Unique**: `item.trident.thunder` (Channeling hit, vol 5.0).

## Technical Details

### Block States
| Name        | Default | Values                              | Description                  |
|-------------|---------|-------------------------------------|------------------------------|
| `facing`    | `up`    | `up`,`down`,`north`,`south`,`east`,`west` | Anchoring direction. |
| `powered`   | `false` | `false`,`true`                      | Recently struck.             |
| `waterlogged` | `false`| `false`,`true`                    | Contains water.              |

### Block IDs
- Base: `lightning_rod`
- Oxidation: `exposed_lightning_rod`, `weathered_lightning_rod`, `oxidized_lightning_rod`
- Waxed: `waxed_lightning_rod`, `waxed_exposed_lightning_rod`, `waxed_weathered_lightning_rod`, `waxed_oxidized_lightning_rod`

**No numeric IDs** (string-based). No special NBT or block data noted. Likely in tags like `#minecraft:lightning_rods` for datapacks.

**Commands/Trivia**:
- `/item replace` on player head: Appears as **upside-down antenna** to right of head (JE only).

## Advancements
| Name            | Description                                      | Exact Requirements |
|-----------------|--------------------------------------------------|--------------------|
| Surge Protector | Protect a villager from an undesired shock without starting a fire. | Within **30 blocks** of lightning strike (**no fire started**); **unharmed villager** within or **up to 6 blocks above** a **30×30×30 volume** centered on strike. |
| Wax On          | Apply honeycomb to a copper block!               | Honeycomb on any **unwaxed copper block/rod** (any of 15 variants, 4 oxidation stages). |
| Wax Off         | Scrape wax off of a copper block!                | Axe on any **waxed copper block/rod** (any of 15 variants, 4 stages). |

## History
| Version/Snapshot | Changes |
|------------------|---------|
| 1.17<br>20w45a  | Added lightning rods. |
| 1.17<br>20w46a  | Texture matches copper blocks. Range: 16→32 blocks. |
| 1.17<br>21w05a  | Proper shading. |
| 1.17<br>21w10a  | Changelog mentions waterloggable (not functional yet). |
| 1.17<br>21w11a  | Turns white on strike; waterloggable; thunderstorm particles. |
| 1.17<br>21w13a  | Model underside texture fixed. |
| 1.21.9<br>25w31a| Oxidize like copper; added exposed/weathered/oxidized variants; wax (honeycomb, yellow particles); scrape (axe, white particles). |

**No further changes** as of latest (post-1.21.9). Range finalized to 128 blocks (post-32).

## Known Issues/Notes
- Maintained on Mojang bug tracker (minecraft.wiki issues link).
- Minor wiki debates: Exact mob damage volume centering; fire effective range (~2-block sphere confirmed). No major Java bugs noted.

This **exhaustively covers all available information** from the official Minecraft Wiki (primary source), including mechanics, data values, history, and trivia. No additional details (e.g., custom NBT, special loot) found in searches or discussions. For datamined code, consult decompilers; practical gameplay info is complete here.