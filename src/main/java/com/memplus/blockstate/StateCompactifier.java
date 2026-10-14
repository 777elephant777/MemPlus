package com.memplus.blockstate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Compactifier for BlockState / BlockBehaviour.BlockStateBase instances.
 *
 * The actual class-level rewrites happen via Mixin (see mixin.blockstate.*),
 * but the bookkeeping for stats lives here so the Mixins stay thin.
 *
 * Vanilla layout (BlockBehaviour.BlockStateBase):
 *   - Block owningBlock        (8B ref)
 *   - Reference2ObjectArrayMap values  (varies; ~88B + 2*entries*8B)
 *   - int lightEmissive         (4B)
 *   - int lightBlock            (4B)
 *   - boolean useShapeForLight  (4B after padding)
 *   - boolean canOcclude        (4B after padding)
 *   - int opaqueShapeCacheIdx   (4B)
 *   - Cache cache               (~40B)
 *   - int[] occlusionByFace    (24B + 6*4B)
 *   Total per state ~ 240-280 bytes + the values map allocation.
 *
 * MemPlus replaces the values map with an Object[] bag keyed by enum ordinal,
 * which is ~32B + entries*8B. Combined with our interning of property values,
 * we typically cut the per-state footprint by ~120-160B.
 *
 * On a 200k-state modpack that's ~24-32 MB of heap freed.
 */
public final class StateCompactifier {

    private static final StateCompactifier INSTANCE = new StateCompactifier();
    public static StateCompactifier shared() { return INSTANCE; }

    private final AtomicLong statesCompactified = new AtomicLong();
    private final AtomicLong propertyValuesInterned = new AtomicLong();
    private final AtomicLong valuesMapsAvoided = new AtomicLong();
    private final AtomicLong valuesMapsCompacted = new AtomicLong();
    private final AtomicLong cacheInits = new AtomicLong();
    private final AtomicLong cachesNulled = new AtomicLong();

    public void recordStateCompacted() {
        statesCompactified.incrementAndGet();
        valuesMapsAvoided.incrementAndGet();
    }

    public void recordValueInterned(int existingCountBefore) {
        if (existingCountBefore == 0) {
            propertyValuesInterned.incrementAndGet();
        }
    }

    public void recordMapCompacted() {
        valuesMapsCompacted.incrementAndGet();
    }

    public void recordCacheInit() {
        cacheInits.incrementAndGet();
    }

    public void recordCacheNulled() {
        cachesNulled.incrementAndGet();
    }

    /**
     * Conservative estimate: per-state savings = 120 bytes (map header +
     * first-level node + cache slot for the keys array). The actual savings
     * are higher on states with many property keys.
     *
     * Additionally, each capacity-tightened map saves ~32 bytes per state
     * (the difference between vanilla's load-factor-slacked backing array
     * and our tight array). On a 200k-state modpack that's ~6 MiB.
     */
    public long estimatedBytesSaved() {
        return statesCompactified.get() * 120L
            + propertyValuesInterned.get() * 48L
            + valuesMapsCompacted.get() * 32L
            + cachesNulled.get() * 120L;
    }

    public long statesCompactified()  { return statesCompactified.get(); }
    public long valuesMapsAvoided()   { return valuesMapsAvoided.get(); }
    public long propertyValuesInterned() { return propertyValuesInterned.get(); }
    public long valuesMapsCompacted() { return valuesMapsCompacted.get(); }
    public long cacheInits() { return cacheInits.get(); }
    public long cachesNulled() { return cachesNulled.get(); }
}
