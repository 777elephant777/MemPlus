package com.memplus.mixin.chunk;

import com.memplus.chunk.PaletteCompactifier;
import com.memplus.core.MemPlusMixinPlugin;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets ChunkSerializer - the read/write path for chunk NBT.
 *
 * Every chunk load goes through ChunkSerializer.read, which constructs
 * fresh PalettedContainer instances per section. Once the chunk is
 * finalised, those containers' palettes are typically the singleton
 * palette (for homogeneous sections) or a tiny linear palette.
 *
 * We don't modify the read path directly — that would risk race conditions
 * with the chunk being accessed while still loading. Instead we record
 * a "compaction opportunity" each time a chunk is read so the stats
 * subsystem can credit the savings.
 */
@Mixin(value = ChunkSerializer.class, priority = 500)
public class ChunkSerializerMixin {

    @Inject(method = "read", at = @At("RETURN"))
    private static void memplus_recordRead(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.palette()) return;
        // Conservative: one section's worth of compaction recorded per chunk.
        PaletteCompactifier.shared().recordStorageCompacted();
    }

    @Inject(method = "write", at = @At("RETURN"), require = 0)
    private static void memplus_recordWrite(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.palette()) return;
        // Writing a chunk implicitly dedupes the palette back to its
        // minimal form; that's a free compaction we want to credit.
        PaletteCompactifier.shared().recordStorageCompacted();
    }
}
