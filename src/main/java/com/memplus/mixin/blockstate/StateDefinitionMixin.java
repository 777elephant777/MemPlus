package com.memplus.mixin.blockstate;

import com.memplus.blockstate.StateCompactifier;
import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Targets StateDefinition - the registry of all BlockStates for a given Block.
 *
 * StateDefinition holds a list of every state for the block plus a
 * {@code Map<Map<Property, Comparable>, S>} for state lookup by property
 * values. The lookup map's keys are themselves Map instances — vanilla
 * creates one Reference2ObjectArrayMap per state. We intern these key maps
 * so identical property-value combinations share a single map instance.
 *
 * On a block with 4 properties averaging 4 values each that's 256 states,
 * each with an identical key set in 99% of cases (the property itself is
 * the same; only its values differ). Sharing the *Map instance* across
 * states with identical values gives huge savings, but for safety MemPlus
 * only interning the key strings (rather than the entire map) here.
 */
@Mixin(value = StateDefinition.class, priority = 500)
public class StateDefinitionMixin {

    /**
     * Capture StateHolder.setValue calls that allocate a fresh property-value
     * map. When the same property value has been observed before, return the
     * canonical string representation so downstream consumers share one
     * underlying instance via {@link StringDeduplicator}.
     */
    @Redirect(
        method = "getPossibleStates",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Object;toString()Ljava/lang/String;",
            ordinal = 0
        ),
        require = 0
    )
    private String memplus_internPropertyValue(Object self) {
        if (!MemPlusMixinPlugin.interning()) {
            String plain = self == null ? "null" : self.toString();
            return StringDeduplicator.shared().dedup(plain);
        }
        String plain = self == null ? "null" : self.toString();
        String canonical = StringDeduplicator.shared().dedup(plain);
        if (canonical != plain) {
            StateCompactifier.shared().recordValueInterned(0);
        }
        return canonical;
    }
}
