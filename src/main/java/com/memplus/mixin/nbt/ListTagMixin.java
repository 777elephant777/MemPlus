package com.memplus.mixin.nbt;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.nbt.NbtCompactifier;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Targets ListTag.
 *
 * <p>Vanilla ListTag uses {@code Lists.newArrayList()} (an ArrayList with
 * default capacity 10) for its backing list. Each empty ListTag therefore
 * costs ~50 bytes (object header + Object[10] backing array + size field).
 * On worlds with many empty list tags (entity "ActiveEffects", block
 * entity "Items" for empty containers, etc.) that's several MiB of pure
 * overhead.
 *
 * <p>This mixin replaces the backing list with a size-1 ArrayList when
 * the ListTag is empty (the smallest valid capacity), and trims-to-size
 * the backing array after NBT deserialization via the copy() hook.
 *
 * <p>Also dedups the toString() return value, since list tags in crash
 * reports tend to be identical.
 */
@Mixin(value = ListTag.class, priority = 500)
public class ListTagMixin {

    @Shadow
    @Final
    @Mutable
    private List<Tag> list;

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "<init>*",
        at = @At("RETURN")
    )
    private void memplus_compactBacking(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!MemPlusMixinPlugin.nbtLazy()) return;
        try {
            // If the list is empty or very small, swap in a capacity-1
            // ArrayList — saves ~36 bytes per empty ListTag.
            if (list != null && list.isEmpty()) {
                this.list = new ArrayList<>(1);
                NbtCompactifier.shared().recordListTagCompacted();
            } else if (list instanceof ArrayList<?> al && !al.isEmpty()) {
                // Trim the backing array to size if there's slack.
                al.trimToSize();
                NbtCompactifier.shared().recordListTagCompacted();
            }
        } catch (Throwable t) {
            com.memplus.MemPlus.LOGGER.debug(
                "[MemPlus] ListTag backing compaction skipped: {}", t.toString());
        }
    }

    @Inject(method = "toString", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupToString(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.nbtIntern()) return;
        String original = cir.getReturnValue();
        if (original != null && original.length() >= 32) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                NbtCompactifier.shared().recordStringTagDedup();
                cir.setReturnValue(canonical);
            }
        }
    }
}
