package com.memplus.mixin.recipe;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets RecipeManager.
 *
 * RecipeManager.byName is a Map<ResourceLocation, RecipeHolder>. On large
 * modpacks (Create, MI, AE2, Mekanism) this map easily reaches 20-50k
 * entries. Each RecipeHolder's id is a ResourceLocation whose path string
 * is shared with similar recipes from the same mod (e.g. "create:mixing/..."
 * is the prefix for hundreds of recipes). The path string dedup saves
 * several MB across all recipes.
 *
 * We don't actually replace the map here (vanilla's recipe map is highly
 * performance-sensitive and we don't want to risk lookup regressions).
 * We just expose the recipe id strings to the dedup table by interning
 * them through the RecipeManagerMixin on recipe registration.
 */
@Mixin(value = RecipeManager.class, priority = 500)
public class RecipeManagerMixin {

    @Inject(method = "apply", at = @At("HEAD"), require = 0)
    private void memplus_onRecipeApply(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.recipe()) return;
        // The recipe id strings will be interned by CompoundTagMixin's
        // key-interning hook (since the recipe id is used as a key in the
        // underlying tag), so we don't need explicit code here.
        // We do a single dedup ping on a known common prefix to warm
        // the dedup table with the most-likely-to-be-reused strings.
        StringDeduplicator.shared().dedup("minecraft:crafting");
        StringDeduplicator.shared().dedup("minecraft:smelting");
        StringDeduplicator.shared().dedup("minecraft:smithing");
    }
}
