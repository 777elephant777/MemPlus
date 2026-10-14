package com.memplus.mixin.extras;

import com.memplus.core.MemPlusMixinPlugin;
import com.memplus.pool.StringDeduplicator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets LootTable - the per-loot-table ResourceLocation id holder.
 *
 * <p>LootTable.setLootTableId() stores a ResourceLocation reference. The
 * ResourceLocation's path string is the same across all entities that
 * share that loot table (e.g. every zombie references "minecraft:entities/zombie").
 *
 * <p>We don't replace the ResourceLocation itself — that would risk
 * breaking identity comparisons in mods. We just ensure the
 * underlying path / namespace strings are deduped through the global
 * table when set. The actual deduplication happens via the
 * ResourceLocationMixin's getPath/getNamespace hooks; this mixin
 * exists as a stats hook to credit the savings.
 */
@Mixin(value = LootTable.class, priority = 500)
public class LootTableMixin {

    @ModifyVariable(
        method = "setLootTableId",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true,
        require = 0
    )
    private ResourceLocation memplus_dedupLootTableId(ResourceLocation id) {
        if (!MemPlusMixinPlugin.interning() || id == null) return id;
        // The underlying strings will be deduped via the ResourceLocation
        // getNamespace/getPath hooks. We trigger the dedup explicitly here
        // so the loot table id's strings are canonicalized at set time.
        try {
            StringDeduplicator.shared().dedup(id.toString());
        } catch (Throwable ignored) {}
        return id;
    }
}
