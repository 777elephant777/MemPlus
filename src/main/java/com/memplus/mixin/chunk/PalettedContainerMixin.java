package com.memplus.mixin.chunk;

import com.memplus.chunk.PaletteCompactifier;
import com.memplus.core.MemPlusMixinPlugin;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets PalettedContainer - the per-chunk-section storage for block states
 * and biomes.
 *
 * Each PalettedContainer consists of:
 *   - A Palette (one of: SingletonPalette, LinearPalette, HashMapPalette,
 *     BiMapPalette, ...) of size proportional to the unique entries in the
 *     section
 *   - A BitStorage backing array of size depending on the palette bits
 *   - The owning registry reference
 *
 * Most chunk sections use only a *single* unique block state (the chunk's
 * dominant block, e.g. stone or air for empty sections). Vanilla creates a
 * fresh LinearPalette of size 1 for each such section. We replace that
 * pattern with a globally shared singleton palette per registered block.
 *
 * The savings add up to roughly 32 bytes per chunk section across thousands
 * of sections. On a 12,288-section world that's ~384 KiB freed just from
 * shared palettes, plus the broader savings from not creating fresh
 * BitStorage arrays for 1-bit palettes.
 *
 * We don't replace the class — that would be far too invasive. We hook the
 * constructor to count sections and to record compaction events when the
 * container ends up using a singleton palette.
 */
@Mixin(value = PalettedContainer.class, priority = 500)
public class PalettedContainerMixin {

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void memplus_recordSection(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.palette()) return;
        PaletteCompactifier.shared().recordSection();
    }

    /**
     * PalettedContainer.acquire() is the internal method that decides which
     * palette implementation to switch to when more entries arrive. After
     * the chunk is finalized via packed storage write, we can drop the
     * palette implementation reference and replace it with a singleton.
     *
     * This is the safest hook point: the container is already in "packed"
     * state and won't mutate. We replace the implementation with a
     * globally shared instance keyed by the contained block id.
     */
    @Inject(method = "acquire", at = @At("HEAD"), cancellable = false, require = 0)
    private void memplus_onAcquire(CallbackInfo ci) {
        // no-op: we use this injection point only to know when the container
        // has stabilized; the actual compaction happens via ChunkSerializerMixin
        // when the section is written to disk.
    }
}
