package com.memplus.mixin.extras;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets Potion - the named potion (e.g. "minecraft:long_swiftness").
 *
 * <p>Like Enchantment, Potion.getName() returns a translation key string
 * that's reused across many invocations.
 */
@Mixin(value = Potion.class, priority = 500)
public class PotionMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupName(String prefix, CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.interning()) return;
        String original = cir.getReturnValue();
        if (original != null) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                cir.setReturnValue(canonical);
            }
        }
    }
}
