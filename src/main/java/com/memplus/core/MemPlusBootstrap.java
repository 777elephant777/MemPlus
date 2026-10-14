package com.memplus.core;

/**
 * One-time warm-up for MemPlus subsystems.
 *
 * Warm-up forces class loading now (during the loading screen) rather than
 * lazily on the first chunk render, which gives the JIT more warm-up time and
 * avoids the "first-frame hitch" that lazy class loading causes.
 */
public final class MemPlusBootstrap {
    private static volatile boolean warmed = false;

    public static void warm() {
        if (warmed) return;
        synchronized (MemPlusBootstrap.class) {
            if (warmed) return;
            warmed = true;

            // Touch every subsystem class. The order matters slightly: string
            // dedup must be available before any other subsystem writes through
            // to it, otherwise early interned strings would bypass dedup.
            Class<?> c1 = com.memplus.pool.StringDeduplicator.class;
            Class<?> c2 = com.memplus.registry.RegistryCompactifier.class;
            Class<?> c3 = com.memplus.chunk.PaletteCompactifier.class;
            Class<?> c4 = com.memplus.blockstate.StateCompactifier.class;
            Class<?> c5 = com.memplus.nbt.NbtCompactifier.class;

            // Suppress "unused" warnings; the side-effect we want is class load.
            if (c1 == null || c2 == null || c3 == null || c4 == null || c5 == null) {
                throw new IllegalStateException("MemPlus bootstrap failed");
            }

            com.memplus.pool.StringDeduplicator.shared();
            // Pre-warm the dedup table with the most common vanilla
            // strings so they're canonicalized before any user-facing
            // code allocates them.
            try {
                com.memplus.core.MemPlusWarmup.preWarm();
            } catch (Throwable t) {
                com.memplus.MemPlus.LOGGER.debug(
                    "[MemPlus] Pre-warm skipped: {}", t.toString());
            }
        }
    }
}
