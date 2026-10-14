package com.memplus.registry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Bookkeeping for the registry mixins.
 *
 * Vanilla frozen registries (after the lifecycle FREEZE event) hold a
 * Reference2ObjectArrayMap mapping ResourceLocation -> Holder.Reference, plus
 * a Holder.Reference per entry (~80B each + the ResourceLocation String
 * 48-200B). On a modpack with 100k registered objects across all registries
 * that's well over 30 MB of pure overhead.
 *
 * After the registry freezes MemPlus swaps the backing map for a custom
 * integer-indexed lookup (the registry's ids array), saving the Node
 * overhead and the per-Holder linked-list slot. The ResourceLocation keys
 * are interned via {@link com.memplus.pool.StringDeduplicator}.
 *
 * Savings tracked here are estimates; the real-world figure is larger
 * because we ignore GC metadata and indirect references.
 */
public final class RegistryCompactifier {

    private static final RegistryCompactifier INSTANCE = new RegistryCompactifier();
    public static RegistryCompactifier shared() { return INSTANCE; }

    private final AtomicLong registriesCompacted = new AtomicLong();
    private final AtomicLong holdersCompacted = new AtomicLong();
    private final AtomicLong resourceLocationsInterned = new AtomicLong();

    public void recordRegistryCompacted() { registriesCompacted.incrementAndGet(); }
    public void recordHolderCompacted()   { holdersCompacted.incrementAndGet(); }
    public void recordResourceLocationInterned() { resourceLocationsInterned.incrementAndGet(); }

    public long estimatedBytesSaved() {
        // 64B per Holder compacted (Reference2ObjectArrayMap Node + slot)
        // 96B per ResourceLocation interned (the RL header + path string + namespace)
        // 256B per registry compaction (table[16] + size field + mod count + threshold)
        return holdersCompacted.get() * 64L
             + resourceLocationsInterned.get() * 96L
             + registriesCompacted.get() * 256L;
    }

    public long registriesCompacted() { return registriesCompacted.get(); }
    public long holdersCompacted() { return holdersCompacted.get(); }
    public long resourceLocationsInterned() { return resourceLocationsInterned.get(); }

    /** Periodic flush hook called on server shutdown. No persistent state to clear. */
    public void flush() {
        // no-op: RegistryCompactifier is purely a stats counter; the actual
        // dedup table lives in StringDeduplicator which has its own flush().
    }
}
