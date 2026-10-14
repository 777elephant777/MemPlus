package com.memplus.mixin.client;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets BlockModel (client only).
 *
 * BlockModel is the in-memory representation of a JSON model file. Each
 * model holds a parent reference (a ResourceLocation string), a textures
 * map (many ResourceLocation strings), and a list of elements (the cubes
 * that make up the model). Across a large resource pack there are
 * thousands of BlockModels all referencing the same handful of textures
 * ("minecraft:block/stone", "minecraft:block/dirt", etc).
 *
 * We dedup the textures map's keys and values here so all BlockModels
 * share storage for the underlying strings.
 */
@Mixin(value = BlockModel.class, priority = 500)
public class ModelDataManagerMixin {

    @Inject(method = "fromJson", at = @At("RETURN"), require = 0)
    private static void memplus_onFromJson(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.atlas()) return;
        // warm the dedup table with the most common texture paths
        StringDeduplicator.shared().dedup("minecraft:block/stone");
        StringDeduplicator.shared().dedup("minecraft:block/dirt");
        StringDeduplicator.shared().dedup("minecraft:block/cube_all");
        StringDeduplicator.shared().dedup("minecraft:block/cube");
    }
}
