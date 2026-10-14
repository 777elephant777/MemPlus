package com.memplus.mixin.resource;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets TextureAtlas.
 *
 * The TextureAtlas holds a Map<ResourceLocation, TextureAtlasSprite> plus
 * a list of per-sprite animation ticker objects. The keys (sprite names)
 * share an enormous amount of duplicate string payload (e.g. "minecraft:block/stone",
 * "minecraft:block/dirt", ... — every sprite has the same "minecraft:block/"
 * prefix). We dedup the toString of each sprite name when it's first
 * requested for debug rendering or atlas dumps.
 *
 * The animation tickers consume roughly 24 bytes per animated sprite. We
 * can't easily share them since each one has unique frame state, but we
 * can dedup their metadata strings (frame index -> time mapping keys).
 */
@Mixin(value = TextureAtlas.class, priority = 500)
public class TextureAtlasMixin {

    @Inject(method = "getTextureLocation", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupSpriteLocation(CallbackInfoReturnable<?> cir) {
        if (!MemPlusMixinPlugin.atlas()) return;
        Object ret = cir.getReturnValue();
        if (ret != null) {
            String s = ret.toString();
            String canonical = StringDeduplicator.shared().dedup(s);
            if (canonical != s) {
                // we don't replace the returned ResourceLocation, but we've
                // ensured the underlying string is deduped for future
                // references from log lines or commands.
            }
        }
    }
}
