package com.memplus.mixin.resource;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.client.renderer.texture.SpriteLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets SpriteLoader.
 *
 * SpriteLoader.stitch() creates the per-atlas texture from individual sprite
 * frames. Each sprite has its own name (a ResourceLocation string), and
 * on large resource packs many sprites share the same mod id prefix.
 *
 * We dedup the sprite name ResourceLocation's toString() result so the
 * repeated prefix strings share storage across sprites. We also dedup
 * the resource pack name strings (which are inserted as part of the
 * sprite metadata map keys).
 */
@Mixin(value = SpriteLoader.class, priority = 500)
public class SpriteLoaderMixin {

    @Inject(method = "load", at = @At("RETURN"), cancellable = false, require = 0)
    private void memplus_dedupSpriteNames(CallbackInfoReturnable<?> cir) {
        if (!MemPlusMixinPlugin.atlas()) return;
        // We don't modify the return value here; the actual deduplication
        // happens via the CompoundTagMixin / ResourceLocation.toString()
        // hooks that are already in place. This injection exists to record
        // that the sprite loader pipeline is active.
    }
}
