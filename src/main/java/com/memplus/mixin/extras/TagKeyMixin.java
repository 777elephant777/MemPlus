package com.memplus.mixin.extras;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets TagKey - the registry-keyed tag identifier.
 *
 * <p>TagKey is a record holding a {@code ResourceKey} and a
 * {@code ResourceLocation}. Vanilla already uses a Guava Interner to dedup
 * the TagKey instances themselves, so two {@code TagKey.create(reg, rl)}
 * calls with the same arguments return the same TagKey instance.
 *
 * <p>But the underlying ResourceLocation's path string (e.g.
 * "minecraft:blocks/wools") is still a separate allocation per TagKey
 * unless it was routed through the dedup table. We dedup the toString()
 * output here so logging and crash reports share one String per tag.
 */
@Mixin(value = TagKey.class, priority = 500)
public class TagKeyMixin {

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
