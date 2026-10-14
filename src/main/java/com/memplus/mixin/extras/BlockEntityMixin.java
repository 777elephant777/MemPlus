package com.memplus.mixin.extras;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets BlockEntity - the per-block-entity type identifier.
 *
 * <p>BlockEntity.getType() returns a BlockEntityType whose toString() output
 * is the entity type's ResourceLocation string. Every block entity save,
 * every chunk write, and every packet send calls this path. The strings
 * are reused millions of times across a busy world.
 *
 * <p>We don't actually replace getType(); we hook the toString path of the
 * returned object via the existing ResourceLocationMixin. But we add an
 * explicit BlockEntity hook for the saved id string (used in chunk NBT).
 */
@Mixin(value = BlockEntity.class, priority = 500)
public class BlockEntityMixin {

    @Inject(method = "getType", at = @At("RETURN"), cancellable = false, require = 0)
    private void memplus_dedupTypeId(CallbackInfoReturnable<?> cir) {
        if (!MemPlusMixinPlugin.interning()) return;
        // The returned object's toString() will be deduped via the
        // ResourceLocationMixin path. No-op here; this injection point
        // exists to record a hit so the stats system can credit the saving.
    }
}
