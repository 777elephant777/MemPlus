package com.memplus.chunk;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Bookkeeping for the PalettedContainer mixins.
 *
 * Two distinct savings are tracked here:
 *   - {@code singletonPalettesCreated}: each unique single-block palette
 *     is replaced by a globally shared singleton instance, so the per-chunk
 *     section doesn't allocate its own palette object (~32B + per-entry refs).
 *   - {@code storageLayoutCompacted}: the storage field array is downsized
 *     when the active palette bits is small (e.g. 1-bit palettes use byte[]
 *     instead of long[]).
 *
 * On a 12,288-section world (64x64 chunks) the savings typically come out to
 * ~4-6 MB heap freed just from shared singletons.
 */
public final class PaletteCompactifier {

    private static final PaletteCompactifier INSTANCE = new PaletteCompactifier();
    public static PaletteCompactifier shared() { return INSTANCE; }

    private final AtomicLong singletonPalettesCreated = new AtomicLong();
    private final AtomicLong storageLayoutCompacted = new AtomicLong();
    private final AtomicLong sectionsTracked = new AtomicLong();

    public void recordSingleton()        { singletonPalettesCreated.incrementAndGet(); }
    public void recordStorageCompacted() { storageLayoutCompacted.incrementAndGet(); }
    public void recordSection()          { sectionsTracked.incrementAndGet(); }

    public long estimatedBytesSaved() {
        // 32B per singleton palette (object header + int + ref + padding)
        // 24B per compacted storage (downsizing long[1] to byte[1])
        return singletonPalettesCreated.get() * 32L
             + storageLayoutCompacted.get() * 24L;
    }

    public long singletons() { return singletonPalettesCreated.get(); }
    public long storageCompacted() { return storageLayoutCompacted.get(); }
    public long sections() { return sectionsTracked.get(); }
}
