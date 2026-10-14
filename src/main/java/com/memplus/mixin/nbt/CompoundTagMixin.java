package com.memplus.mixin.nbt;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.nbt.LazyTagMap;
import com.memplus.nbt.NbtCompactifier;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Targets CompoundTag.
 *
 * <p>This mixin delivers two compactions:
 *
 * <ol>
 *   <li><b>Lazy backing map</b>: replace the inner {@link HashMap} with a
 *       {@link LazyTagMap} that doesn't allocate until first mutation.
 *       On worlds with many empty CompoundTags (entity sub-tags, block
 *       entity extra data, item stack sub-compounds), this saves ~80
 *       bytes per never-mutated tag.
 *
 *   <li><b>Key interning</b>: route every {@code put()} key through the
 *       {@link StringDeduplicator}. Vanilla allocates fresh String
 *       instances for tag names on every NBT load from disk, but the
 *       same ~200 tag names ("id", "Pos", "Motion", "Health", ...)
 *       appear millions of times across a save world. Interning dedups
 *       these to a single canonical instance per unique name.
 * </ol>
 *
 * <p>The lazy map swap is done via {@code @Mutable @Shadow @Final} on
 * the {@code tags} field — no reflection, no AT file needed.
 */
@Mixin(value = CompoundTag.class, priority = 500)
public class CompoundTagMixin {

    @Shadow
    @Final
    @Mutable
    private Map<String, Tag> tags;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void memplus_swapToLazyMap(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.nbtLazy()) return;
        try {
            // If the existing backing is empty, swap in a lazy map.
            // If it's non-empty (constructed from source map), wrap it in
            // a LazyTagMap and copy entries over so reads still work.
            LazyTagMap lazy = new LazyTagMap();
            if (tags != null && !tags.isEmpty()) {
                lazy.copyFrom(tags);
            }
            this.tags = lazy;
        } catch (Throwable t) {
            com.memplus.MemPlus.LOGGER.debug(
                "[MemPlus] CompoundTag lazy map swap skipped: {}", t.toString());
        }
    }

    @ModifyVariable(
        method = { "put(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;",
                   "putString(Ljava/lang/String;Ljava/lang/String;)V",
                   "putByte(Ljava/lang/String;B)V",
                   "putShort(Ljava/lang/String;S)V",
                   "putInt(Ljava/lang/String;I)V",
                   "putLong(Ljava/lang/String;J)V",
                   "putFloat(Ljava/lang/String;F)V",
                   "putDouble(Ljava/lang/String;D)V",
                   "putBoolean(Ljava/lang/String;Z)V",
                   "putUUID(Ljava/lang/String;Ljava/util/UUID;)V" },
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true,
        require = 0
    )
    private String memplus_internKey(String key) {
        if (!MemPlusMixinPlugin.nbtIntern() || key == null) return key;
        String canonical = StringDeduplicator.shared().dedup(key);
        if (canonical != key) NbtCompactifier.shared().recordKeyIntern();
        return canonical;
    }

    @ModifyVariable(
        method = "putString(Ljava/lang/String;Ljava/lang/String;)V",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true,
        require = 0
    )
    private String memplus_internStringValue(String value) {
        if (!MemPlusMixinPlugin.nbtIntern() || value == null) return value;
        String canonical = StringDeduplicator.shared().dedup(value);
        if (canonical != value) NbtCompactifier.shared().recordStringTagDedup();
        return canonical;
    }

    @Inject(method = "toString", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupToString(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.nbtIntern()) return;
        String original = cir.getReturnValue();
        if (original != null && original.length() >= 32) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                cir.setReturnValue(canonical);
            }
        }
    }
}
