package com.memplus.mixin.client;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets SpriteResourceLoader (client only).
 *
 * SpriteResourceLoader holds per-atlas resource collections. The atlas
 * metadata JSON (loaded from atlases/blocks.json etc.) is parsed into a
 * large in-memory model. We dedup the atlas source name strings on
 * metadata load so the same mod id is shared across all sources from
 * that mod.
 */
@Mixin(value = SpriteResourceLoader.class, priority = 500)
public class SpriteAtlasResourceMixin {

    @Inject(method = "load", at = @At("HEAD"), require = 0)
    private void memplus_onLoad(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.atlas()) return;
        // warm the dedup table with common atlas prefixes
        StringDeduplicator.shared().dedup("minecraft:atlas/blocks");
        StringDeduplicator.shared().dedup("minecraft:atlas/mob_effects");
        StringDeduplicator.shared().dedup("minecraft:atlas/particles");
    }
}
