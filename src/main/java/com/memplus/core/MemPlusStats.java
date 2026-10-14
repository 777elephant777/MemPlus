package com.memplus.core;

import com.memplus.MemPlus;
import com.memplus.blockstate.StateCompactifier;
import com.memplus.chunk.PaletteCompactifier;
import com.memplus.nbt.NbtCompactifier;
import com.memplus.pool.StringDeduplicator;
import com.memplus.registry.RegistryCompactifier;

/**
 * Lightweight aggregate stats reporter.
 *
 * MemPlus does not run a real heap dump at runtime (way too expensive). Instead
 * each subsystem tracks its own *estimated* savings — bytes that would have
 * been allocated under the vanilla code path but were either avoided entirely
 * or replaced by a smaller structure. We report those estimates here.
 *
 * Estimates are conservative: they are based on the number of compactified
 * instances times the size of the *vanilla* field they replace. They will
 * always under-report real-world savings, because they ignore the GC metadata
 * per object (12-16 bytes header), padding and pointer indirection savings.
 */
public final class MemPlusStats {

    private static long baselineHeap;

    public static void markBaseline() {
        try {
            Runtime r = Runtime.getRuntime();
            long used = r.totalMemory() - r.freeMemory();
            baselineHeap = used;
            MemPlus.LOGGER.info("[MemPlus] Baseline heap after server start: {} MiB", used / (1024 * 1024));
        } catch (Throwable ignored) { baselineHeap = -1; }
    }

    public static void report() {
        if (!com.memplus.config.MemPlusConfig.logStats()) return;

        long strings  = StringDeduplicator.shared().estimatedBytesSaved();
        long regs     = RegistryCompactifier.shared().estimatedBytesSaved();
        long states   = StateCompactifier.shared().estimatedBytesSaved();
        long palettes = PaletteCompactifier.shared().estimatedBytesSaved();
        long nbts     = NbtCompactifier.shared().estimatedBytesSaved();

        long total = strings + regs + states + palettes + nbts;
        MemPlus.LOGGER.info("================ MemPlus Report ================");
        MemPlus.LOGGER.info(" BlockState   savings: {} KiB", states / 1024);
        MemPlus.LOGGER.info(" Palette      savings: {} KiB", palettes / 1024);
        MemPlus.LOGGER.info(" NBT          savings: {} KiB", nbts / 1024);
        MemPlus.LOGGER.info(" Registry     savings: {} KiB", regs / 1024);
        MemPlus.LOGGER.info(" StringDedup  savings: {} KiB", strings / 1024);
        MemPlus.LOGGER.info(" Total estimated:      {} KiB", total / 1024);
        try {
            Runtime r = Runtime.getRuntime();
            long used = r.totalMemory() - r.freeMemory();
            MemPlus.LOGGER.info(" Live heap delta vs baseline: {} KiB",
                baselineHeap > 0 ? (used - baselineHeap) / 1024 : "unknown");
        } catch (Throwable ignored) {}
        MemPlus.LOGGER.info("==================================================");
    }
}
