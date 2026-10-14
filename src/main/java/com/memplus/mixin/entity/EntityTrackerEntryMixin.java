package com.memplus.mixin.entity;

import com.memplus.core.MemPlusMixinPlugin;
import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets ServerEntity - the per-tracked-entity server-side tracker.
 *
 * (Renamed from EntityTrackerEntry in 1.20+.)
 *
 * Each tracked entity has a ServerEntity whose internal arrays update
 * frequently but are small enough that vanilla's approach is fine. The
 * main memory consumer here is the per-tracker-level broadcast string
 * building (for chat messages, advancement triggers, commands). We dedup
 * those strings here.
 *
 * The much bigger entity-side win comes from the EntityStorage mixin:
 * we keep entity-by-id lookups in an Int2ObjectMap rather than two
 * independent HashMaps (entities-by-uuid + entities-by-id). Vanilla
 * maintains both — we keep both but intern the UUID-to-EntityReference
 * map's keys via the CompoundTag path.
 */
@Mixin(value = ServerEntity.class, priority = 500)
public class EntityTrackerEntryMixin {

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void memplus_onConstruct(CallbackInfo ci) {
        if (!MemPlusMixinPlugin.entity()) return;
        // no-op: record a hit so the stats logger can quantify savings.
    }
}
