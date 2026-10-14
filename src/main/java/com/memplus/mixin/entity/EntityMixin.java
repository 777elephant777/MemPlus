package com.memplus.mixin.entity;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.nbt.NbtCompactifier;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets Entity.
 *
 * Entity.saveWithoutId() and saveAsPassenger() produce CompoundTags whose
 * contents are typically identical across many entities of the same type.
 * We dedup the returned CompoundTag's toString() (used in crash reports)
 * and intern any strings read back via Entity.load().
 *
 * Entity NBT contains many small strings: id, CustomName, Tags, attributes,
 * effects. These are dedup'd by the CompoundTagMixin interning already,
 * but we add an additional optimization: the entity type's "id" string is
 * always the same across all instances of that entity type. We explicitly
 * route it through dedup.
 */
@Mixin(value = Entity.class, priority = 500)
public class EntityMixin {

    @Inject(method = "getEncodeId", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupEntityId(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.entity()) return;
        String original = cir.getReturnValue();
        if (original != null && original.length() >= 8) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                NbtCompactifier.shared().recordKeyIntern();
                cir.setReturnValue(canonical);
            }
        }
    }
}
