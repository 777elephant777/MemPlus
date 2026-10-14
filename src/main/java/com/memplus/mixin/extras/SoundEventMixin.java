package com.memplus.mixin.extras;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets SoundEvent - the per-sound ResourceLocation holder.
 *
 * <p>Every {@link SoundEvent} holds a ResourceLocation. The same sound id
 * ("minecraft:block.stone.hit", "minecraft:entity.zombie.ambient", etc.)
 * is referenced thousands of times per minute on a busy server. We dedup
 * the SoundEvent's toString and getLocation() return so they share storage.
 */
@Mixin(value = SoundEvent.class, priority = 500)
public class SoundEventMixin {

    @Inject(method = "getLocation", at = @At("RETURN"), cancellable = false, require = 0)
    private void memplus_dedupLocation(CallbackInfoReturnable<?> cir) {
        if (!MemPlusMixinPlugin.interning()) return;
        // The returned ResourceLocation's toString() is deduped via
        // ResourceLocationMixin. No-op here for stats only.
    }

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
