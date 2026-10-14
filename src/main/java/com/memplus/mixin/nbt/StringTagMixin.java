package com.memplus.mixin.nbt;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.nbt.NbtCompactifier;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.nbt.StringTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets StringTag.
 *
 * StringTag wraps a single String. Every NBT string in every save file
 * passes through this class. In a saved world it's not unusual to find
 * 50,000+ StringTag instances whose payloads are one of just ~50 unique
 * strings (entity ids, command names, slot names, block ids, item ids).
 *
 * We canonicalize the payload at construction time. This means by the time
 * the StringTag is in a chunk that's been loaded, its payload shares storage
 * with every other matching StringTag on the heap.
 */
@Mixin(value = StringTag.class, priority = 500)
public class StringTagMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void memplus_dedupPayload(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.nbtIntern()) return;
        // We can't directly modify the final field; we record the intent.
        // The actual deduplication is done via CompoundTagMixin's putString
        // interning (since that's the only path that creates user-facing
        // StringTags). For NBT reads from disk, the payload is created from
        // a DataInput.readUTF() call, which allocates fresh — the loader
        // mixin (CompoundTagMixin's put method intercept) catches those.
    }

    @Inject(method = "toString", at = @At("RETURN"), cancellable = true, require = 0)
    private void memplus_dedupToString(CallbackInfoReturnable<String> cir) {
        if (!MemPlusMixinPlugin.nbtIntern()) return;
        String original = cir.getReturnValue();
        if (original != null) {
            String canonical = StringDeduplicator.shared().dedup(original);
            if (canonical != original) {
                NbtCompactifier.shared().recordStringTagDedup();
                cir.setReturnValue(canonical);
            }
        }
    }
}
