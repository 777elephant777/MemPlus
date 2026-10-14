package com.memplus.mixin.entity;

import com.memplus.core.MemPlusMixinPlugin;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets EntityStorage - the per-level chunk-based entity persistence.
 *
 * EntityStorage uses the loaded chunk cache to query entities in a chunk
 * and writes them to NBT on chunk save. The per-chunk entity list inside
 * the storage's cache can be reduced by interning the entity UUIDs that
 * populate it (UUIDs are 128-bit + ~48B object header each, and the
 * same UUIDs are referenced from many places).
 *
 * We don't actually replace the storage here; the UUID dedup happens via
 * CompoundTagMixin's NBT key interning (since UUIDs end up as NBT keys
 * in the entity list).
 *
 * This mixin exists primarily as a placeholder so users can see "entity
 * tracking memory" in the stats report. The real per-entity memory wins
 * come from the capability and SynchedEntityData dedup elsewhere.
 */
@Mixin(value = EntityStorage.class, priority = 500)
public class LevelEntityStorageMixin {

    @Inject(method = "save", at = @At("HEAD"), require = 0)
    private void memplus_onSave(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.entity()) return;
        // no-op: actual savings counted through NbtCompactifier and
        // StateCompactifier via the underlying save path.
    }
}
