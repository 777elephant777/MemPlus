package com.memplus.nbt;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Stats helper for the NBT mixins. Each CompoundTag/ListTag mixin reports
 * savings here.
 *
 * Savings per saved NBT operation:
 *   - CompoundTag key interning: a typical chunk NBT has ~30 unique tag
 *     names reused across hundreds of sections. Interning per-key saves
 *     ~32B (String object + UTF-16 payload) per duplicate.
 *   - Lazy compound list: when getCompound().put() is never called, the
 *     inner Map is never allocated. Saves the per-CompoundTag HashMap
 *     header (~96B) plus its first bucket array (~48B).
 *   - StringTag payload deduplication: same idea as key interning but on
 *     the value side.
 */
public final class NbtCompactifier {

    private static final NbtCompactifier INSTANCE = new NbtCompactifier();
    public static NbtCompactifier shared() { return INSTANCE; }

    private final AtomicLong keysInterned        = new AtomicLong();
    private final AtomicLong stringTagsDeduped   = new AtomicLong();
    private final AtomicLong lazyCompoundsCreated = new AtomicLong();
    private final AtomicLong lazyCompoundsNeverUsed = new AtomicLong();
    private final AtomicLong listTagsCompacted   = new AtomicLong();

    public void recordKeyIntern()          { keysInterned.incrementAndGet(); }
    public void recordStringTagDedup()     { stringTagsDeduped.incrementAndGet(); }
    public void recordLazyCompoundCreated() { lazyCompoundsCreated.incrementAndGet(); }
    public void recordLazyCompoundNeverUsed() { lazyCompoundsNeverUsed.incrementAndGet(); }
    public void recordListTagCompacted()   { listTagsCompacted.incrementAndGet(); }

    public long estimatedBytesSaved() {
        // 48B per interned key (String header + 16B payload avg)
        // 48B per deduped StringTag
        // 144B per unused lazy compound (HashMap 48B + table[16] 64B + size field 4B + ref 8B + padding)
        // 24B per compacted list tag (ArrayList overhead avoided)
        return keysInterned.get() * 48L
             + stringTagsDeduped.get() * 48L
             + lazyCompoundsNeverUsed.get() * 144L
             + listTagsCompacted.get() * 24L;
    }

    public long keysInterned() { return keysInterned.get(); }
    public long stringTagsDeduped() { return stringTagsDeduped.get(); }
    public long lazyCompoundsCreated() { return lazyCompoundsCreated.get(); }
    public long lazyCompoundsNeverUsed() { return lazyCompoundsNeverUsed.get(); }
    public long listTagsCompacted() { return listTagsCompacted.get(); }
}
