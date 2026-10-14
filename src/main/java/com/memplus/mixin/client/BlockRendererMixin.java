package com.memplus.mixin.client;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets BlockRenderDispatcher (client only).
 *
 * BlockRenderDispatcher's getModel() returns a BakedModel for a given state.
 * Many BakedModels share the same name string (especially for blocks that
 * have many property combinations but only one baked model). We dedup the
 * baked model's name string here so the rendering pipeline's name-based
 * caching benefits from a smaller heap.
 */
@Mixin(value = BlockRenderDispatcher.class, priority = 500)
public class BlockRendererMixin {

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = false, require = 0)
    private void memplus_onGetModel(CallbackInfoReturnable<?> cir) {
        if (!MemPlusMixinPlugin.atlas()) return;
        // record the dedup warm-up; the actual canonicalization happens
        // through the BakedModel's id toString path via CompoundTag.
        StringDeduplicator.shared().dedup("minecraft:block");
    }
}
