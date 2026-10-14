package com.memplus.mixin.advancement;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.advancements.Advancement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets Advancement.
 *
 * In 1.21.1 Advancement is a record holding Optional<ResourceLocation> parent,
 * Optional<DisplayInfo> display, AdvancementRewards, Map<String, Criterion>,
 * AdvancementRequirements, etc. The id itself is not on Advancement — it lives
 * on AdvancementHolder — so we don't try to grab it here.
 *
 * We do dedup the toString output (records have a synthetic toString that
 * builds a fresh string each call) and the parent ResourceLocation's
 * toString via the global table.
 */
@Mixin(value = Advancement.class, priority = 500)
public class AdvancementMixin {

    @Inject(method = "toString()Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupToString(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.advancement()) return;
        String original = cir.getReturnValue();
        if (original != null && original.length() >= 16) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                cir.setReturnValue(canonical);
            }
        }
    }
}
