# MiniHUD Project Overview

## Project Identity

**Name:** MiniHUD  
**Type:** Client-side Minecraft Fabric Mod  
**Current Version:** 0.38.3-sakura.1  
**Target Minecraft Version:** 1.21.11  
**License:** LGPLv3  
**Author:** masa (Fork maintained by sakura-ryoko)  
**Language:** Java 21  
**Build System:** Gradle with Fabric Loom

## Purpose

MiniHUD is a client-side information display mod for Minecraft that provides configurable "mini-F3" style information overlays and various rendering utilities. It adds coordinates, viewing angles, current speed, and numerous other data points to the HUD, along with visual overlays for structures, light levels, spawn chunks, and more.

## Core Architecture

### Module Structure

The project follows a clean separation of concerns with these primary packages:

```
fi.dy.masa.minihud/
├── config/         - Configuration system and toggleable options
├── data/           - Data managers for various game information
├── event/          - Event handlers (render, input, world load, server, tick)
├── gui/            - Configuration screens
├── hotkeys/        - Keybind callbacks
├── info/           - Information line providers for HUD display
├── mixin/          - Mixin injections for accessing/modifying game internals
├── network/        - Network packet handling (Servux integration)
├── renderer/       - Overlay renderers for world visualization
└── util/           - Utility classes and data structures
```

### Dependencies

- **Malilib** (required): Shared library providing config system, GUI utilities, rendering helpers, event system
- **Fabric Loader**: 0.18.4+
- **Mod Menu** (optional): For in-game config screen access

### Entry Point

**Main Class:** `fi.dy.masa.minihud.MiniHUD`

- Implements `ModInitializer`
- Registers the `InitHandler` with malilib's initialization system
- Provides debug logging functionality

**Initialization:** `fi.dy.masa.minihud.InitHandler`

- Registers config handlers
- Initializes data managers
- Registers event handlers (input, render, world load, server, tick)
- Sets up keybind providers

## Key Systems

### 1. Configuration System (`config/`)

**Main Config Class:** `Configs.java`

- Implements `IConfigHandler` interface from malilib
- Contains nested static classes for different config categories:
  - `Generic` - General settings and feature toggles
  - `Colors` - Color customization options
  - `InfoLineOrder` - HUD line ordering

**Config Types Used:**

- `ConfigBoolean` - Simple boolean toggles
- `ConfigBooleanHotkeyed` - Boolean with associated keybind
- `ConfigInteger` - Integer values with min/max
- `ConfigDouble` - Double values with range
- `ConfigFloat` - Float values
- `ConfigString` - String values (e.g., format strings)
- `ConfigOptionList` - Enum-based dropdown options
- `ConfigHotkey` - Keybind-only configs
- `ConfigColor` - Color picker configs

**InfoToggle Enum:**

- Enum-based system for toggling individual HUD information lines
- Each toggle has:
  - Internal name (config key)
  - Associated `InfoLineType` for rendering
  - Default enabled state
  - Optional keybind
  - Support for requiring server-side data

**RendererToggle & StructureToggle:**

- Similar enum-based systems for overlay renderers
- Support hotkey toggling and color customization

### 2. Data Management (`data/`)

**Key Data Managers:**

- **`HudDataManager`**: Manages server-side data synchronization for HUD display
  - Handles Servux protocol integration
  - Caches world seed, spawn chunks, game rules, etc.
  - Provides fallback to integrated server data when available

- **`EntitiesDataManager`**: Tracks and caches entity data
  - Provides entity NBT data for tooltips
  - Manages entity data sync with server

- **`DebugDataManager`**: Interfaces with vanilla debug data
  - Accesses path finding, goal selector, and other debug info

- **`MobCapDataHandler`**: Handles mob cap calculations and display

All managers follow the singleton pattern with `getInstance()` methods.

### 3. Event Handling (`event/`)

**Event Handlers:**

- **`RenderHandler`**: Main rendering coordinator
  - Implements `IRenderer` from malilib
  - Handles HUD text rendering, inventory previews, tooltips
  - Coordinates overlay renderers
  - Manages info line display and formatting

- **`InputHandler`**: Handles keyboard/mouse input
  - Processes keybinds
  - Manages modifier keys (shift, alt, etc.)

- **`ClientTickHandler`**: Handles per-tick updates
  - Updates cached data
  - Triggers periodic refresh

- **`WorldLoadListener`**: Responds to world load/unload events

- **`ServerListener`**: Handles integrated server events

### 4. Information Lines (`info/`)

**InfoLine System:**

- `InfoLineType` interface defines how to fetch and format data
- `InfoLineTypes` class contains all implementations
- Each line can:
  - Fetch dynamic data (coordinates, speed, etc.)
  - Format output with custom strings
  - Support conditional display
  - Require specific data sources

**InfoLineContext:**

- Provides context for rendering (player position, world, etc.)
- Caches frequently accessed data for performance

### 5. Renderers (`renderer/`)

**Base Classes:**

- `OverlayRendererBase`: Abstract base for all overlay renderers
- `BaseBlockRangeOverlay`: Specialized base for block-based overlays

**Overlay Types:**

- **Light Level** (`OverlayRendererLightLevel`): Shows light levels with colored markers
- **Slime Chunks** (`OverlayRendererSlimeChunks`): Highlights slime spawning chunks
- **Spawn Chunks** (`OverlayRendererSpawnChunks`): Shows spawn chunk boundaries
- **Structures** (`OverlayRendererStructures`): Renders structure bounding boxes
- **Block Grid** (`OverlayRendererBlockGrid`): Shows block boundaries
- **Biome Borders** (`OverlayRendererBiomeBorders`): Visualizes biome edges
- **Beacon/Conduit Range**: Shows range spheres
- **Spawnable Columns**: Height-based spawn checking
- **Villager Info** (`OverlayRendererVillagerInfo`): Villager trade overlay

**Rendering Approach:**

- Uses VBO-based rendering for performance
- Supports transparency and depth testing
- Can render through blocks or with occlusion
- Color customization per overlay type

### 6. Mixins (`mixin/`)

**Mixin Categories:**

- **block/**: Block entity data access (furnaces, beacons, conduits)
- **client/**: Client-specific hooks (language, minecraft client)
- **debug/**: Debug HUD integration
- **entity/**: Entity data access interfaces
- **hud/**: HUD modification (chat, subtitles)
- **item/**: Item behavior (tooltips, bundle preview)
- **network/**: Network packet interception
- **render/**: Rendering hooks
- **server/**: Server-side data access
- **world/**: World/chunk data access

**Mixin Patterns:**

- Interface mixins (IMixin\*) for adding accessors
- Implementation mixins for behavior modification
- Use `@Inject`, `@Shadow`, `@Accessor` annotations
- Minimal invasiveness - targeted injections

### 7. Network Integration (`network/`)

**Servux Protocol:**

- Custom network protocol for enhanced server data
- Bidirectional communication between client and server
- Provides data not normally available to clients:
  - Exact TPS
  - Mob caps
  - Entity data
  - Structure data

**Handler Classes:**

- `ServuxHudHandler`: Main protocol handler
- `ServuxHudPacket`: Packet definitions
- Gracefully degrades when server doesn't support Servux

### 8. Utilities (`util/`)

**Key Utility Classes:**

- **`DataStorage`**: Central data cache and storage
  - Manages structure data
  - Handles distance reference points
  - Provides shape/region rendering data

- **`MiscUtils`**: General utility methods
- **`DebugInfoUtils`**: Debug information helpers
- **`EntityUtils`**: Entity-related utilities
- **`InventoryUtils`**: Inventory management helpers
- **`RayTracer`**: Ray tracing for block/entity targeting

**Enums:**

- `SpeedUnits`, `BlockGridMode`, `LightLevelMarkerMode`, etc.
- Encapsulate different modes/options for features

## Coding Conventions

### Naming Conventions

1. **Packages:** All lowercase, hierarchical (`fi.dy.masa.minihud.config`)
2. **Classes:** PascalCase (`HudDataManager`, `OverlayRenderer`)
3. **Interfaces:** PascalCase with 'I' prefix for mixins (`IMixinEntity`)
4. **Constants:** UPPER_SNAKE_CASE (`DEBUG_MESSAGES`, `MOD_ID`)
5. **Variables:** camelCase (`worldSeed`, `spawnChunkRadius`)
6. **Config Keys:** camelCase matching variable names (`"infoCoordinates"`)

### Code Organization

1. **Singleton Pattern:** Used extensively for managers

   ```java
   private static final ManagerClass INSTANCE = new ManagerClass();
   public static ManagerClass getInstance() { return INSTANCE; }
   ```

2. **Config Registration:** Configs use fluent `.apply(KEY)` pattern

   ```java
   public static final ConfigBoolean FEATURE = new ConfigBoolean("feature", false).apply(GENERIC_KEY);
   ```

3. **Enum-Based Toggles:** Features as enum values implementing interfaces

   ```java
   public enum InfoToggle implements IConfigInteger, IHotkeyTogglable { ... }
   ```

4. **Immutable Lists:** Use Google Guava's `ImmutableList` for config option lists

5. **Null Safety:** Use `@Nullable` and `@Nonnull` annotations from JSR305

### Mixin Conventions

1. **Naming:**
   - Interfaces: `IMixinClassName`
   - Implementations: `MixinClassName`

2. **Method Prefixes:** Injected methods prefixed with `minihud_`

   ```java
   private void minihud_disableVanillaBeeTooltips(...) { ... }
   ```

3. **Minimal Scope:** Inject at precise locations, avoid broad changes

### Config Organization

1. **Nested Static Classes:** Configs grouped by category in `Configs` class
2. **Translation Keys:** Follow pattern `modid.config.category.option`
3. **Localization:** JSON language files in `resources/assets/minihud/lang/`

### Event Handling

1. **Handler Registration:** Done in `InitHandler.registerModHandlers()`
2. **Singleton Handlers:** All event handlers use singleton pattern
3. **Clear Separation:** Different handlers for different event types

### Rendering

1. **VBO-Based:** Use Vertex Buffer Objects for performance
2. **Matrix Stack:** Proper push/pop of matrix transformations
3. **Profiler Sections:** Wrap rendering in profiler sections for debugging
4. **Color Format:** Colors as integers (ARGB format)

### Documentation

1. **Minimal JavaDoc:** Code is generally self-documenting through clear naming
2. **Comments:** Used for complex algorithms or non-obvious behavior
3. **Translation Strings:** All user-facing text localized

## Build System

### Gradle Configuration

- **Loom Version:** 1.14-SNAPSHOT
- **Java Version:** 21 (source and target)
- **Mappings:** Official Mojang Mappings
- **Access Widener:** `minihud.accesswidener` for accessing private members

### Build Tasks

- `./gradlew build` - Full build with sources jar
- Outputs to `build/libs/`
- Version appends timestamp for `-dev` versions
- Excludes GIMP `.xcf` files from resources

### Repository Structure

- **JitPack:** Used for dependency resolution
- **Maven:** Malilib dependency via GitHub/JitPack
- **Mod Menu:** Optional compile-only dependency

## Localization

Supported languages:

- English (en_us) - Primary
- Spanish (es_es)
- French (fr_fr)
- Italian (it_it)
- Japanese (ja_jp)
- Korean (ko_kr)
- Classical Chinese (lzh)
- Russian (ru_ru)
- Swedish (sv_se)
- Turkish (tr_tr)
- Ukrainian (uk_ua)
- Simplified Chinese (zh_cn)
- Traditional Chinese (zh_tw)

Translation keys follow hierarchical structure:

- `minihud.config.generic.*` - Generic configs
- `minihud.config.info_toggle.*` - Info toggles
- `minihud.config.renderer_toggle.*` - Renderer toggles
- `minihud.info.*` - Info line texts

## Performance Considerations

1. **Caching:** Extensive caching of frequently accessed data
2. **VBO Rendering:** Batch rendering with vertex buffers
3. **Conditional Rendering:** Only render enabled overlays
4. **Chunk-Based Updates:** Update overlays incrementally
5. **Async Data Fetch:** Non-blocking server data requests
6. **Profiler Integration:** Performance monitoring built-in

## Extension Points

To add new features, consider these extension points:

1. **New Info Lines:**
   - Add to `InfoToggle` enum
   - Implement `InfoLineType` in `InfoLineTypes`
   - Add translation strings

2. **New Overlays:**
   - Extend `OverlayRendererBase`
   - Add to `RendererToggle` enum
   - Register in `InitHandler`

3. **New Configs:**
   - Add to appropriate `Configs` nested class
   - Add to `OPTIONS` list
   - Add translation key

4. **New Data Sources:**
   - Create manager in `data/` package
   - Register in `InitHandler`
   - Add necessary mixins for data access

5. **New Mixins:**
   - Create in appropriate `mixin/` subdirectory
   - Register in `mixins.minihud.json`
   - Follow naming conventions (prefix with `minihud_`)

## Testing & Development

- **IDE:** Compatible with IntelliJ IDEA and Eclipse
- **Testing:** Run client via Gradle tasks
- **Debugging:** Mixin debugging available via JVM args
- **Logging:** Log4j2 logger available via `MiniHUD.LOGGER`
- **Debug Mode:** Enable with `DEBUG_MESSAGES` config option

## Version Compatibility

- Follows Minecraft versioning closely
- Each branch typically targets specific MC version
- Malilib version dependencies strictly enforced
- Uses Fabric's version predicates for compatibility checking

---

_This overview provides a high-level understanding of the MiniHUD project structure. For specific implementation details, refer to the source code and inline documentation._
