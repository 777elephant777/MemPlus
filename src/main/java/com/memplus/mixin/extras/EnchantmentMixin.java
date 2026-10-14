package com.memplus.mixin.extras;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets Enchantment - now a record in 1.21.
 *
 * <p>Enchantment.toString() is the synthetic record toString, building a
 * fresh String each call. We dedup it so repeated logging of the same
 * enchantment id shares one String instance.
 */
@Mixin(value = Enchantment.class, priority = 500)
public class EnchantmentMixin {

    @Inject(method = "toString", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupToString(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.interning()) return;
        String original = cir.getReturnValue();
        if (original != null && original.length() >= 16) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                cir.setReturnValue(canonical);
            }
        }
    }
}
