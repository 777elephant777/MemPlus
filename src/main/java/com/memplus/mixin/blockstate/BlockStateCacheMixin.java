package com.memplus.mixin.blockstate;

import com.memplus.blockstate.StateCompactifier;
import com.memplus.core.MemPlusMixinPlugin;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets BlockBehaviour.BlockStateBase.initCache() - the per-state cache allocator.
 *
 * <p>Vanilla allocates a Cache object (~120 bytes including the VoxelShape[]
 * array) on every state that doesn't have a dynamic shape. Across a 200k-state
 * modpack that's ~24 MiB of cache objects.
 *
 * <p>Vanilla's cache-reading methods ({@code getLightBlock},
 * {@code propagatesSkylightDown}, {@code getOcclusionShape},
 * {@code getFaceOcclusionShape}, {@code hasLargeCollisionShape},
 * {@code isCollisionShapeFullBlock}, {@code isFaceSturdy},
 * {@code useShapeForLightOcclusion}) all already have null checks and fall
 * back to dynamic computation.
 *
 * <p>MemPlus nulls the cache for states where the block is air. The Cache
 * class is package-private so we shadow it as Object.
 *
 * <p>Savings: ~120 bytes per air-like state. Zero-risk because vanilla
 * null-checks.
 */
@Mixin(value = BlockBehaviour.BlockStateBase.class, priority = 500)
public class BlockStateCacheMixin {

    /** Cache is package-private; we shadow as Object so we can null it. */
    @Shadow
    @Final
    @Mutable
    protected Object cache;

    @Shadow
    @Final
    private boolean isAir;

    @Inject(method = "initCache", at = @At("RETURN"), require = 0)
    private void memplus_optionalNullCache(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.blockstate()) return;
        try {
            StateCompactifier.shared().recordCacheInit();
            // Null the cache for air-like blocks. Vanilla's cache readers
            // all null-check, so this is safe. ~120 bytes saved per air state.
            if (isAir && this.cache != null) {
                this.cache = null;
                StateCompactifier.shared().recordCacheNulled();
            }
        } catch (Throwable t) {
            com.memplus.MemPlus.LOGGER.debug(
                "[MemPlus] Cache init hook skipped: {}", t.toString());
        }
    }
}
