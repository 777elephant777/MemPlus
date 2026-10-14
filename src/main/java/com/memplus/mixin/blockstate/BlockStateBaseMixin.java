package com.memplus.mixin.blockstate;

import com.memplus.blockstate.StateCompactifier;
import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets BlockBehaviour.BlockStateBase (inner class).
 *
 * Vanilla BlockStateBase extends StateHolder<Block, BlockState> and stores its
 * values in a Reference2ObjectArrayMap inherited from StateHolder. Each state
 * therefore owns an array map of size equal to its property count. On a large
 * modpack with 100k+ states this is the single largest heap contributor.
 *
 * MemPlus's strategy here is conservative: we don't replace the map class
 * (that would require a StateHolder mixin, which is too invasive in 1.21
 * due to the heavy generics). Instead we:
 *   1) Intern the state's string representation via {@link StringDeduplicator}
 *      when {@link #toString()} is first computed — these strings are almost
 *      always identical across states and the JVM currently doesn't dedup
 *      them automatically because they're built via a fresh StringBuilder.
 *   2) Track the per-state memory accounting here.
 */
@Mixin(value = BlockBehaviour.BlockStateBase.class, priority = 500)
public class BlockStateBaseMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void memplus_onInit(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.blockstate()) return;
        // Account for the state. We can't replace the underlying map here
        // (the constructor has already populated it), but interning below
        // handles the much larger per-state string allocations.
        StateCompactifier.shared().recordStateCompacted();
    }

    /**
     * Wrap toString() so its returned string is dedup'd before being handed
     * to any caller. Block state toString is invoked frequently for debug
     * logging, command parsing, crash reports and harvest level lookups; in
     * each case the strings are identical across invocations of the same
     * state. Without interning each call allocates a fresh ~80B String.
     *
     * The injection point is RETURN so we don't have to deal with the various
     * branches vanilla's toString can take; the local String result is the
     * value we want to canonicalize.
     */
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
}
