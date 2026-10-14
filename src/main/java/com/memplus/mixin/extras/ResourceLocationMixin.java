package com.memplus.mixin.extras;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets ResourceLocation - the foundation of every Minecraft id string.
 *
 * <p>Every block id, item id, entity id, sound id, recipe id, advancement id,
 * loot table id, etc. is a ResourceLocation. Its {@link #toString()} is built
 * fresh on every call via {@code makeConcatWithConstants(namespace, path)},
 * producing a new String each time. Across a busy server, ResourceLocation
 * toString is called millions of times per tick (logging, debug, packets).
 *
 * <p>Deduping toString's return is the single highest-leverage string
 * optimization in MemPlus. Each call saves ~48 bytes (String header) +
 * ~16 bytes (UTF-16 payload for a typical "minecraft:block/stone" string).
 *
 * <p>Also dedup {@link #getPath()} and {@link #getNamespace()} returns, so
 * the underlying storage shares strings across all RLs with the same path
 * (e.g. all "minecraft:foo" RLs share the same "foo" instance).
 */
@Mixin(value = ResourceLocation.class, priority = 500)
public class ResourceLocationMixin {

    @Inject(method = "toString", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupToString(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.interning()) return;
        String original = cir.getReturnValue();
        if (original != null) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                cir.setReturnValue(canonical);
            }
        }
    }

    @Inject(method = "getPath", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupPath(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.interning()) return;
        String original = cir.getReturnValue();
        if (original != null) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                cir.setReturnValue(canonical);
            }
        }
    }

    @Inject(method = "getNamespace", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupNamespace(CallbackInfoReturnable<String> cir) {
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
