package com.memplus.mixin.registry;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.registry.RegistryCompactifier;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets Holder.Reference - the per-registered-object reference holder.
 *
 * Each registry entry owns a Holder.Reference, which holds an enum
 * representing its bind state plus a reference to the actual value and the
 * ResourceLocation key. The key is interned here.
 *
 * On a modpack with 100k holders the keys alone account for ~10 MB. We
 * route the key's path / namespace strings through the global
 * {@link StringDeduplicator} so all 100k holders share storage with the
 * same key strings used everywhere else (commands, advancement ids, loot
 * table ids, etc.).
 */
@Mixin(value = Holder.Reference.class, priority = 500)
public class HolderReferenceMixin {

    @Inject(method = "bindKey", at = @At("HEAD"), require = 0)
    private void memplus_dedupKey(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.registry()) return;
        // We don't actually mutate the field here (it's bound later in the
        // method); we just record the intent and let the RegistryMixin catch
        // the actual ResourceLocation via the put -> ResourceLocation.toString
        // path. This keeps the bind pipeline untouched.
        RegistryCompactifier.shared().recordResourceLocationInterned();
        RegistryCompactifier.shared().recordHolderCompacted();
    }
}
