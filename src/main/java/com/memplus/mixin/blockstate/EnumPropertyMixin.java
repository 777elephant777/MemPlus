package com.memplus.mixin.blockstate;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets EnumProperty.
 *
 * EnumProperty.getName(Comparable) is called by the codec and toString
 * paths on every state lookup. The string is built from
 * {@link net.minecraft.util.StringRepresentable#getSerializedName()},
 * which is usually interned by vanilla but not always. We dedup it
 * explicitly here so every EnumProperty's "north"/"south"/etc. string
 * is shared globally across all states.
 */
@Mixin(value = EnumProperty.class, priority = 500)
public class EnumPropertyMixin {

    @Inject(method = "getName(Ljava/lang/Comparable;)Ljava/lang/String;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_internEnumValueName(Comparable<?> value,
                                             CallbackInfoReturnable<String> cir) {
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
