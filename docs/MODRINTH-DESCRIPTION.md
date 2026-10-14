# MemPlus: Memory+

> **Status: Beta.** This mod is brand-new and has not been tested across the full range of modpacks. Please report crashes with logs.

**MemPlus** is an aggressive memory-only optimization mod for Minecraft 1.21.1 on NeoForge. It uses Mixin to deduplicate strings, defer allocations, and compact data structures without changing any game logic.

---

## Why use MemPlus?

If you run a heavy modpack (200+ mods) on a server with 20+ players and a large saved world, MemPlus reduces heap usage by:

- **Deduplicating every ResourceLocation** across all registries — `minecraft:block/stone`, `minecraft:entity/zombie`, etc. are shared as a single canonical instance across every reference.
- **Deduplicating NBT keys and string-tag payloads** — `"id"`, `"Pos"`, `"Motion"`, `"Health"`, ... appear millions of times in a saved world; MemPlus shares them all through a single canonical instance.
- **Deferring CompoundTag backing HashMap allocation** — empty compound tags don't pay for a 16-slot HashMap until they're actually mutated.
- **Compacting BlockState values maps** — every BlockState's `Reference2ObjectArrayMap` gets a capacity-tight replacement via the `CompactStateMap` subclass.
- **Nulling the BlockState Cache for air-like states** — vanilla's cache readers already null-check, so we can free ~120 bytes per air state with zero risk.
- **Deduplicating BlockState / Property / EnumProperty strings** — `toString()`, `"facing"`, `"north"`, etc. are shared across every state via a weak-ref dedup table.
- **Deduplicating entity encode ids, sound ids, enchantment ids, biome ids, particle ids, loot table ids, recipe id prefixes, advancement id strings, TagKey strings, ChunkPos strings, and ~300 pre-warmed common vanilla strings at startup.**

## Safe by design

- **Mixin-only** — no class replacement, no game logic changes, no rendering changes.
- **WeakReference-backed dedup table** — deduplicated strings can still be reclaimed by young-gen GCs. MemPlus does **not** use `String.intern()` (which can leak into the JVM's permanent generation on long-running servers).
- **Per-subsystem config toggles** — every optimization can be individually disabled in `config/memplus-common.toml`.
- **`require = 0` on every Mixin injection** — if a Mixin target method signature shifts in a NeoForge patch, the Mixin silently skips instead of crashing the game.
- **Try-catch in every Mixin body** — if one optimization fails (e.g. reflection), all the others stay active.

## How does it compare to FerriteCore?

**They overlap on three optimizations** (BlockState map compaction, CompoundTag lazy map, ListTag trim) and **diverge everywhere else**:

| | MemPlus | FerriteCore |
|---|---|---|
| BlockState values map compaction | ✓ (subclass of `Reference2ObjectArrayMap`) | ✓ (full custom class — tighter) |
| BlockState Cache nulling | air only (conservative) | wider (more block types) |
| NBT key dedup | ✓ | — |
| NBT string-tag value dedup | ✓ | — |
| ResourceLocation dedup | ✓ | — |
| TagKey dedup | ✓ | — |
| SoundEvent / Enchantment / MobEffect / Potion / Biome / ParticleType dedup | ✓ | — |
| LootTable / ChunkPos dedup | ✓ | — |
| Client-side atlas / sprite / model string dedup | ✓ (client only) | — (server-side mod) |
| Recipe / Advancement id dedup | ✓ | — |
| Global weak-ref string dedup table | ✓ | — (avoids `intern()` by design) |
| Pre-warmed startup strings | ✓ (~300 strings) | — |
| `/memplus` in-game command | ✓ | — |
| Startup mixin status log | ✓ | — |
| Multi-version support | 1.21.1 only | 1.16 through 1.21 |
| Real-world testing | brand new (zero users) | years of modpack use |

**Recommendation: run both.** They coexist without conflict — FerriteCore's BlockState work runs first, MemPlus sees the already-compact map and no-ops on that subsystem, then MemPlus adds all the wider string/NBT/registry/atlas savings on top.

## Install

1. Drop the JAR into your `mods/` folder.
2. Launch Minecraft 1.21.1 with NeoForge 21.1.90 or newer.
3. After ~5 minutes of world load, check `logs/latest.log` for the `[MemPlus]` block — it lists which mixins were applied successfully and the pre-warmed string count.
4. Type `/memplus stats` in chat (op-level 2) to see real-time savings without restarting the server.

## Commands

- `/memplus stats` — print per-subsystem savings as chat text
- `/memplus dump` — write the full report to `logs/latest.log`
- `/memplus gc` — issue a `System.gc()` hint (mostly no-op on modern JVMs unless `-XX:+ExplicitGCInvokesConcurrent` is set)

## Config

All subsystems are toggleable in `config/memplus-common.toml` (created on first launch). Defaults are all-on.

```toml
[blockstate]
compactStateStorage = true       # compact per-state values map
internPropertyValues = true      # intern property name and value strings

[nbt]
internTags = true                 # dedup CompoundTag keys and StringTag payloads
lazyCompoundTag = true            # lazy-allocate CompoundTag's backing map

[chunk]
compactPalette = true            # stats hook for chunk sections
compactStorageLayout = true       # stats hook for BitStorage

[registry]
compactRegistries = true          # dedup ResourceLocation keys across frozen registries

[entity]
compactEntityTracking = true     # dedup entity encode-id strings

[resource]
compactAtlas = true               # dedup sprite name strings (client)

[recipe]
compactRecipeBook = true          # warm dedup table with common recipe prefixes

[advancement]
compactAdvancements = true        # dedup advancement id strings

[strings]
dedupStrings = true               # global weak-ref string dedup
minLength = 4                      # only dedup strings >= 4 chars

[runtime]
gcHints = false                   # advisory System.gc() on dimension unload
logStats = true                   # print per-subsystem savings on shutdown
```

## Compatibility

Tested compatible with:
- NeoForge 21.1.90 through 21.1.252
- FerriteCore (they stack — run both for maximum savings)

**Not yet tested with:** ModernFix, Canary, Saturn, most large modpacks. Please report any incompatibilities with a log file.

## License & Source

**License:** MIT — see [LICENSE](https://github.com/your-username/MemPlus/blob/main/LICENSE) for full text.

**Source:** Full source available at [GitHub](https://github.com/your-username/MemPlus). Pull requests welcome.

## Known limitations

- **1.21.1 only** — no plans for backports; the Mixin targets are version-specific.
- **Stats-only on PalettedContainer** — MemPlus does not replace the chunk palette class (too risky for chunk corruption). The PalettedContainer mixin only records stats; it does not actually compact the palette.
- **BlockState Cache nulling is air-only** — wider nulling requires real-world testing to confirm which mods don't reflectively read the cache.
- **Real-world savings are estimated, not measured** — the `/memplus stats` command shows count × bytes-per-instance, which is conservative. Run a heap dump (VisualVM, YourKit) for actual numbers on your specific modpack.
- **No Fabric port** — NeoForge only.

## Acknowledgements

- **FerriteCore by malte0811** — for the foundational insight that vanilla's per-state map is the dominant heap contributor. MemPlus extends that idea across the wider Minecraft string and NBT surface.
- The NeoForge team for the ModDev Gradle plugin and Mojang mappings.
