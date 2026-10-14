package com.memplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Single config spec for all MemPlus optimizations.
 *
 * Every optimization is gated individually so users can disable a single
 * subsystem that conflicts with another mod (or just dial back the most
 * aggressive ones on slower CPUs, since "memory" and "cpu" tradeoffs
 * occasionally cross over).
 *
 * Defaults reflect the recommended "all on" experience for typical modpacks.
 */
public class MemPlusConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_BLOCKSTATE_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_BLOCKSTATE_INTERNING;
    public static final ModConfigSpec.BooleanValue ENABLE_NBT_INTERN;
    public static final ModConfigSpec.BooleanValue ENABLE_NBT_LAZY_COMPOUND;
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_PALETTE_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_LAYOUT_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_REGISTRY_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_ENTITY_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_ATLAS_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_RECIPE_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_ADVANCEMENT_COMPACT;
    public static final ModConfigSpec.BooleanValue ENABLE_STRING_DEDUP;
    public static final ModConfigSpec.IntValue STRING_DEDUP_THRESHOLD;
    public static final ModConfigSpec.BooleanValue ENABLE_GC_HINTS;
    public static final ModConfigSpec.BooleanValue LOG_STATS_ON_SHUTDOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("MemPlus: aggressive memory optimization. Each subsystem can be toggled individually.")
                .push("blockstate");
        ENABLE_BLOCKSTATE_COMPACT = b
                .comment("Replaces the per-state Reference2ObjectArrayMap with a compact int-indexed bag.",
                        "This is the single biggest source of heap savings on large modpacks.")
                .define("compactStateStorage", true);
        ENABLE_BLOCKSTATE_INTERNING = b
                .comment("Interns ComparablePropertyInstance keys so identical state maps share one reference.",
                        "Pairs with compactStateStorage; turning it on without compactStateStorage has no effect.")
                .define("internPropertyValues", true);
        b.pop();

        b.push("nbt");
        ENABLE_NBT_INTERN = b
                .comment("Deduplicates keys in CompoundTag and StringTag payloads. Most saved worlds reuse the",
                        "same ~200 tag names repeatedly, so this saves a large amount of heap on disk-loaded worlds.")
                .define("internTags", true);
        ENABLE_NBT_LAZY_COMPOUND = b
                .comment("Wraps CompoundTag.getCompound/getList results so nested compound tags only allocate",
                        "their backing map when first mutated. Heavily reduces duplication on chunk reloads.")
                .define("lazyCompoundTag", true);
        b.pop();

        b.push("chunk");
        ENABLE_CHUNK_PALETTE_COMPACT = b
                .comment("Switches single-block PalettedContainer palettes to a singleton holder instead of a",
                        "size-1 PaletteIPv array, and shares global singleton palettes per block state.")
                .define("compactPalette", true);
        ENABLE_CHUNK_LAYOUT_COMPACT = b
                .comment("Downsizes PalettedContainer's storage bits field from int to byte and packs the",
                        "data array header. Saves a few dozen bytes per chunk section across thousands of sections.")
                .define("compactStorageLayout", true);
        b.pop();

        b.push("registry");
        ENABLE_REGISTRY_COMPACT = b
                .comment("Replaces the per-Holder ReferenceLinkedHashMap in vanilla registries with a",
                        "compact IdentityHashMap + int[] id table after the registry is frozen.",
                        "Eliminates the per-entry Node overhead across hundreds of thousands of holders.")
                .define("compactRegistries", true);
        b.pop();

        b.push("entity");
        ENABLE_ENTITY_COMPACT = b
                .comment("Compacts EntityTrackerEntry and level entity storage to use int entity-id maps",
                        "rather than full object references where possible, and trims SynchedEntityData",
                        "arrays to the actual slot count used by the entity type.")
                .define("compactEntityTracking", true);
        b.pop();

        b.push("resource");
        ENABLE_ATLAS_COMPACT = b
                .comment("Sprite atlas uses a shared animation ticker table rather than one per animated sprite,",
                        "and drops per-sprite UV float[] arrays in favor of packed longs. Client only.")
                .define("compactAtlas", true);
        b.pop();

        b.push("recipe");
        ENABLE_RECIPE_COMPACT = b
                .comment("Recipe book caches ingredient counts rather than full Ingredient[] arrays for known recipes.")
                .define("compactRecipeBook", true);
        b.pop();

        b.push("advancement");
        ENABLE_ADVANCEMENT_COMPACT = b
                .comment("Interns advancement ResourceLocation strings and dedupes predicate arrays.")
                .define("compactAdvancements", true);
        b.pop();

        b.push("strings");
        ENABLE_STRING_DEDUP = b
                .comment("Global String.dedup table for Minecraft's many generated 'minecraft:foo' style strings.",
                        "Safe alternative to String.intern() (which leaks into the permanent generation).")
                .define("dedupStrings", true);
        STRING_DEDUP_THRESHOLD = b
                .comment("Only dedup strings at least this long (chars).")
                .defineInRange("minLength", 4, 1, 64);
        b.pop();

        b.push("runtime");
        ENABLE_GC_HINTS = b
                .comment("Advisory System.gc() calls during dimension unloads. Disabled by default since",
                        "most JVMs ignore it; turn on only on compacting G1 with -XX:+ExplicitGCInvokesConcurrent.")
                .define("gcHints", false);
        LOG_STATS_ON_SHUTDOWN = b
                .comment("Print estimated per-subsystem memory savings to the log on server shutdown.")
                .define("logStats", true);
        b.pop();

        SPEC = b.build();
    }

    public static boolean blockstate()       { return ENABLE_BLOCKSTATE_COMPACT.get(); }
    public static boolean interning()        { return ENABLE_BLOCKSTATE_INTERNING.get(); }
    public static boolean nbtIntern()        { return ENABLE_NBT_INTERN.get(); }
    public static boolean nbtLazy()          { return ENABLE_NBT_LAZY_COMPOUND.get(); }
    public static boolean palette()          { return ENABLE_CHUNK_PALETTE_COMPACT.get(); }
    public static boolean storageLayout()    { return ENABLE_CHUNK_LAYOUT_COMPACT.get(); }
    public static boolean registry()         { return ENABLE_REGISTRY_COMPACT.get(); }
    public static boolean entity()           { return ENABLE_ENTITY_COMPACT.get(); }
    public static boolean atlas()            { return ENABLE_ATLAS_COMPACT.get(); }
    public static boolean recipe()           { return ENABLE_RECIPE_COMPACT.get(); }
    public static boolean advancement()      { return ENABLE_ADVANCEMENT_COMPACT.get(); }
    public static boolean dedupStrings()     { return ENABLE_STRING_DEDUP.get(); }
    public static int    dedupThreshold()    { return STRING_DEDUP_THRESHOLD.get(); }
    public static boolean gcHints()          { return ENABLE_GC_HINTS.get(); }
    public static boolean logStats()         { return LOG_STATS_ON_SHUTDOWN.get(); }
}
