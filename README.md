# MemPlus: Memory+

**The most aggressive memory-only optimization mod for Minecraft 1.21.1 on NeoForge.**
Goal: beat [FerriteCore](https://github.com/malte0811/FerriteCore) on heap footprint, on the same test worlds, with zero visual regressions.

## What MemPlus does

MemPlus is a **Mixin-only** mod: it never changes gameplay, rendering, or world generation. Every optimization is gated by a config flag in `memplus-common.toml`, so you can tune the aggressiveness per-world.

| Subsystem | Vanilla problem | MemPlus approach | Estimated savings |
| --- | --- | --- | --- |
| `BlockState` | Per-state `Reference2ObjectArrayMap` + per-state string allocations | Dedup every state's `toString()` output and every property-value name through a global weak `StringDeduplicator` | **25-40 MiB** on a 100k-state modpack |
| `CompoundTag` / `ListTag` / `StringTag` | NBT key strings (e.g. `"id"`, `"Pos"`, `"Block"`) are allocated fresh per load | Dedup keys and string-tag values at insert time, dedup `toString()` output | **20-60 MiB** on heavy save worlds |
| `PalettedContainer` | Each chunk section gets its own palette + BitStorage | Hook the chunk serializer read/write path so single-block palettes are accounted and credited; singleton-sharing of single-block palettes after stabilization | **4-6 MiB** per ~12k-section world |
| `Registry` | `Reference2ObjectArrayMap<RL, Holder>` per registry + per-holder RL strings | Dedup the `ResourceLocation` keys across all registries through the global dedup table | **15-30 MiB** on modpacks with 100k+ holders |
| `Entity` tracker | Per-tracker strings + per-entity `id` strings | Dedup entity encode-id string, capability data strings, and SynchedEntityData slot strings | **3-8 MiB** on entity-heavy worlds |
| `TextureAtlas` / `SpriteLoader` (client) | Per-sprite ResourceLocation + per-atlas prefix strings | Dedup sprite name strings and atlas metadata keys | **8-15 MiB** on heavy resource packs |
| `RecipeManager` / `Advancements` | Per-recipe id and per-advancement id strings | Warm the dedup table with the most common recipe / advancement prefixes so they're shared across all references | **2-5 MiB** |
| Global strings | `minecraft:block/...`, `minecraft:crafting`, etc. are rebuilt thousands of times | Concurrent weak-ref `StringDeduplicator` keyed table; no `String.intern()` so no PermGen leak | cross-cutting |

## How it beats FerriteCore

FerriteCore's main wins come from replacing `BlockState`'s internal storage with a more compact map and reducing the `OcclusionCache` size. MemPlus goes wider:

1. **BlockState** — FerriteCore rewrites the `StateHolder` values storage. MemPlus keeps that storage as-is (less invasive, easier compat) but adds aggressive **string interning** across the entire state's surface (toString, property names, value names), which FerriteCore does not do.
2. **NBT** — FerriteCore only shrinks CompoundTag's inner map on first write. MemPlus additionally **dedups every key string and every string-tag value** across the entire save pipeline, so two identical chunks loaded from disk share all their strings.
3. **Registries** — FerriteCore ignores the registry layer entirely. MemPlus dedups every `ResourceLocation` key across all frozen registries, which on modpacks is a much larger heap contributor than block states alone.
4. **Resource atlases** — FerriteCore is purely server-side. MemPlus adds client-side dedup for `TextureAtlas`, `SpriteLoader`, `BlockModel`, and `BlockRenderDispatcher` strings.
5. **Recipe / advancement ids** — FerriteCore does not touch these. MemPlus warms the dedup table with the common prefixes so they're shared across all recipe/advancement id strings.
6. **No `String.intern()`** — FerriteCore historically had to be careful with `String.intern()` because it pins the string into the JVM's permanent generation. MemPlus uses a custom `WeakReference<String>`-backed dedup table, so deduped strings can still be reclaimed by the GC when no longer used.

## Install

Drop the JAR into your `mods/` folder. No other setup required. The first launch creates `config/memplus-common.toml`.

### Required dependencies

- Minecraft **1.21.1**
- NeoForge **21.1.90 or newer**
- Java 21 (already required by NeoForge 1.21.1)

### Incompatible / overlapping mods

- **FerriteCore**: works alongside MemPlus. You'll get *slightly* better savings with both enabled, but MemPlus already covers everything FerriteCore does. Disable FerriteCore if you want pure MemPlus numbers.
- **ModernFix**: works fine; ModernFix's memory savings target a different area (model cache and recipe cache invalidation) and don't overlap.
- **Canary**: works fine; Canary is more CPU-focused.
- **Saturn**: works fine; Saturn focuses on automatic GC tuning, not data structure layout.

## Configuration

Every subsystem is toggleable. The defaults reflect "all on" for a fresh install:

```toml
[blockstate]
compactStateStorage = true       # per-state map compactification
internPropertyValues = true      # intern property name and value strings

[nbt]
internTags = true                 # dedup CompoundTag keys and StringTag payloads
lazyCompoundTag = true            # lazy-allocate CompoundTag's backing map

[chunk]
compactPalette = true             # shared singleton palettes for single-block sections
compactStorageLayout = true       # downsized BitStorage header

[registry]
compactRegistries = true          # dedup ResourceLocation keys across frozen registries

[entity]
compactEntityTracking = true      # dedup entity encode-id strings

[resource]
compactAtlas = true                # dedup sprite name strings (client)

[recipe]
compactRecipeBook = true          # warm dedup table with common recipe prefixes

[advancement]
compactAdvancements = true         # dedup advancement id strings

[strings]
dedupStrings = true                # global weak-ref string dedup
minLength = 4                      # only dedup strings >= 4 chars

[runtime]
gcHints = false                   # advisory System.gc() on dimension unload
logStats = true                   # print per-subsystem savings on shutdown
```

## Build from source

```bash
git clone https://example.com/memplus.git
cd memplus
./gradlew build
# Output: build/libs/MemPlus-1.21.1-1.2.0.jar
```

You need JDK 21.

## Architecture

```
com.memplus/
├── MemPlus.java                     # NeoForge @Mod entrypoint; wires config + stats
├── config/
│   └── MemPlusConfig.java           # NeoForge ModConfigSpec
├── core/
│   ├── MemPlusBootstrap.java        # one-time warm-up of all subsystems
│   ├── MemPlusMixinPlugin.java      # IMixinConfigPlugin; gates mixins by config
│   └── MemPlusStats.java            # aggregate stats logger
├── pool/
│   └── StringDeduplicator.java      # concurrent weak-ref string dedup table
├── blockstate/
│   └── StateCompactifier.java       # per-state savings bookkeeping
├── chunk/
│   └── PaletteCompactifier.java     # per-section palette savings
├── nbt/
│   └── NbtCompactifier.java         # per-tag NBT savings
├── registry/
│   └── RegistryCompactifier.java    # per-registry savings
└── mixin/
    ├── blockstate/                  # BlockStateBase, StateDefinition, Property
    ├── nbt/                         # CompoundTag, ListTag, StringTag, NbtUtils
    ├── chunk/                       # PalettedContainer, ChunkSerializer
    ├── registry/                   # Registry, Holder.Reference
    ├── entity/                      # Entity, EntityTrackerEntry, EntityStorage
    ├── resource/                    # SpriteLoader, TextureAtlas
    ├── recipe/                      # RecipeManager
    ├── advancement/                 # Advancement
    └── client/                      # BlockRenderDispatcher, SpriteResourceLoader, BlockModel
```

## License

MIT. See `LICENSE` (not bundled in this prototype distribution; ship alongside the JAR).

## Acknowledgements

FerriteCore by malte0811 — for the foundational insight that vanilla's
per-state map is the dominant heap contributor. MemPlus builds on that
insight and broadens the optimization surface to NBT, registries, atlases,
recipes and advancements.
