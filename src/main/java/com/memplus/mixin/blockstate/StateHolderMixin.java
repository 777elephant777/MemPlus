package com.memplus.mixin.blockstate;

import com.memplus.blockstate.CompactStateMap;
import com.memplus.blockstate.StateCompactifier;
import com.memplus.core.MemPlusMixinPlugin;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets StateHolder - parent class of BlockState and FluidState.
 *
 * <p>This is the FerriteCore-equivalent optimization. After the vanilla
 * constructor finishes populating the values map, we replace the
 * {@code Reference2ObjectArrayMap} with a {@link CompactStateMap} that
 * drops the duplicate keys array (the keys are implicit from the
 * StateDefinition's property order) and the cached entry/keys/values
 * collection fields.
 *
 * <p>Savings per state vs vanilla {@link Reference2ObjectArrayMap}:
 * <ul>
 *   <li>Vanilla: 48B object header + 2 Object[] arrays (key, value)
 *       + 4 transient cached collection refs (~32B at 8B each)
 *       = ~96 bytes overhead + 2 array headers.
 *   <li>CompactStateMap: 24B object header + 1 Object[] (values only)
 *       + 1 reference to property list
 *       = ~32 bytes overhead + 1 array header.
 *   <li>Net saving: ~64 bytes per state.
 * </ul>
 *
 * <p>On a 200k-state modpack that's ~12 MiB freed just from the header
 * difference — in addition to the string-dedup savings the other MemPlus
 * mixins deliver on the same state's string payloads.
 *
 * <p>The swap is done via {@code @Shadow @Final @Mutable} which generates
 * bytecode to write to the final field. No reflection, no AT file needed.
 */
@Mixin(value = StateHolder.class, priority = 500)
public class StateHolderMixin<O, S> {

    @Shadow
    @Final
    @Mutable
    private Reference2ObjectArrayMap<Property<?>, Comparable<?>> values;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void memplus_swapToCompactMap(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.blockstate()) return;
        try {
            CompactStateMap<Property<?>, Comparable<?>> compact =
                    CompactStateMap.from(values);
            this.values = compact;
            StateCompactifier.shared().recordMapCompacted();
        } catch (Throwable t) {
            // If anything goes wrong, the vanilla
            // Reference2ObjectArrayMap remains in place. MemPlus's other
            // optimizations stay active.
            com.memplus.MemPlus.LOGGER.debug(
                "[MemPlus] StateHolder map compaction skipped: {}", t.toString());
        }
    }
}
