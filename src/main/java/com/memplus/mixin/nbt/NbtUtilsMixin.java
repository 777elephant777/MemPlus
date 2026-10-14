package com.memplus.mixin.nbt;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.nbt.NbtUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Targets NbtUtils.
 *
 * NbtUtils.formatMetadata and structureToTag both produce long string
 * representations of NBT structures for display in F3 and crash reports.
 * These strings are repeated across the same call site, so we dedup them.
 */
@Mixin(value = NbtUtils.class, priority = 500)
public class NbtUtilsMixin {

    /**
     * Many NbtUtils methods take a CompoundTag and return a formatted String.
     * We intercept the returned string and dedup it so repeated formatting
     * of the same input reuses one canonical instance.
     */
    @ModifyVariable(
        method = "structureToSnbt*",
        at = @At("RETURN"),
        ordinal = 0,
        require = 0
    )
    private static String memplus_dedupFormat(String original) {
        if (!MemPlusMixinPlugin.nbtIntern() || original == null) return original;
        if (original.length() < 16) return original;
        return StringDeduplicator.shared().dedup(original);
    }
}
