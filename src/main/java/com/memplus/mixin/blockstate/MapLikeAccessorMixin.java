package com.memplus.mixin.blockstate;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets Property - the abstract base for properties like DirectionProperty,
 * IntegerProperty, BooleanProperty and EnumProperty.
 *
 * Each property has a {@code getName()} method whose return value is the
 * property's identifier (e.g. "facing", "powered", "axis"). These names are
 * shared across all blocks that use the same property instance, but the
 * underlying *string* may be allocated multiple times when properties are
 * constructed via dynamic registration.
 *
 * We intern the name on first access. We also intern the property's value
 * name (e.g. "north", "south", "east", "west" for direction) because these
 * strings are produced fresh by {@link Property#getName(Comparable)} on
 * each invocation and are 100% identical across calls.
 */
@Mixin(value = Property.class, priority = 500)
public class MapLikeAccessorMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_internPropertyName(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.interning()) return;
        String original = cir.getReturnValue();
        if (original != null) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                cir.setReturnValue(canonical);
            }
        }
    }

    @Inject(method = "getName(Ljava/lang/Comparable;)Ljava/lang/String;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_internValueName(Comparable<?> value,
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
