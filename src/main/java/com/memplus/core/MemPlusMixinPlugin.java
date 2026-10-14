package com.memplus.core;

import com.memplus.config.MemPlusConfig;
import com.memplus.MemPlus;
import com.memplus.pool.StringDeduplicator;
import com.memplus.registry.RegistryCompactifier;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mixin plugin: decides which mixins load based on the active config.
 *
 * <p>Config values are read via a deferred lookup because NeoForge's ModConfig
 * may not be available at mixin application time. We fall back to "enabled"
 * defaults so the optimizations are always active on a fresh install, and the
 * config file (once loaded) can disable specific subsystems on the next run.
 *
 * <p>This plugin also tracks which mixins were applied and which were
 * skipped, so MemPlus can log a summary at startup.
 */
public class MemPlusMixinPlugin implements IMixinConfigPlugin {

    private static final Set<String> appliedMixins = new LinkedHashSet<>();
    private static final Set<String> skippedMixins = new LinkedHashSet<>();
    private static volatile boolean reported = false;

    @Override
    public void onLoad(String mixinPackage) {
        MemPlusBootstrap.warm();
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Gate each mixin through the corresponding config subsystem. The gate
        // always returns `true` until the NeoForge config is loaded, so a fresh
        // install gets the full optimization set on first boot.
        boolean applied = gate(mixinClassName);
        if (applied) {
            appliedMixins.add(mixinClassName);
        } else {
            skippedMixins.add(mixinClassName);
        }
        return applied;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // no-op
    }

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClassNode,
                         String mixinClassName, IMixinInfo mixinInfo) {
        // no-op
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClassNode,
                          String mixinClassName, IMixinInfo mixinInfo) {
        // no-op
    }

    private static boolean enabled(java.util.function.Supplier<Boolean> cfg, boolean fallback) {
        try {
            Boolean v = cfg.get();
            return v == null ? fallback : v;
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static boolean blockstate()    { return enabled(MemPlusConfig::blockstate,    true); }
    public static boolean interning()     { return enabled(MemPlusConfig::interning,    true); }
    public static boolean nbtIntern()     { return enabled(MemPlusConfig::nbtIntern,    true); }
    public static boolean nbtLazy()       { return enabled(MemPlusConfig::nbtLazy,      true); }
    public static boolean palette()       { return enabled(MemPlusConfig::palette,      true); }
    public static boolean storageLayout() { return enabled(MemPlusConfig::storageLayout, true); }
    public static boolean registry()      { return enabled(MemPlusConfig::registry,    true); }
    public static boolean entity()        { return enabled(MemPlusConfig::entity,      true); }
    public static boolean atlas()         { return enabled(MemPlusConfig::atlas,       true); }
    public static boolean recipe()        { return enabled(MemPlusConfig::recipe,      true); }
    public static boolean advancement()   { return enabled(MemPlusConfig::advancement,  true); }
    public static boolean dedupStrings()  { return enabled(MemPlusConfig::dedupStrings, true); }

    private static final Set<String> REGISTRY_MIXINS =
        Set.of("com.memplus.mixin.registry.RegistryMixin",
               "com.memplus.mixin.registry.HolderReferenceMixin");
    private static final Set<String> ATLAS_MIXINS =
        Set.of("com.memplus.mixin.resource.SpriteLoaderMixin",
               "com.memplus.mixin.resource.TextureAtlasMixin",
               "com.memplus.mixin.client.SpriteAtlasResourceMixin",
               "com.memplus.mixin.client.BlockRendererMixin",
               "com.memplus.mixin.client.ModelDataManagerMixin");
    private static final Set<String> RECIPE_MIXINS =
        Set.of("com.memplus.mixin.recipe.RecipeManagerMixin");
    private static final Set<String> ADV_MIXINS =
        Set.of("com.memplus.mixin.advancement.AdvancementMixin");

    public static boolean gate(String mixinClassName) {
        if (mixinClassName == null) return true;
        if (mixinClassName.contains(".blockstate."))    return blockstate();
        if (mixinClassName.contains(".nbt."))           return nbtIntern() || nbtLazy();
        if (mixinClassName.contains(".chunk."))          return palette() || storageLayout();
        if (mixinClassName.contains(".entity."))         return entity();
        if (mixinClassName.contains(".extras."))        return interning();
        if (REGISTRY_MIXINS.contains(mixinClassName))   return registry();
        if (ATLAS_MIXINS.contains(mixinClassName))       return atlas();
        if (RECIPE_MIXINS.contains(mixinClassName))     return recipe();
        if (ADV_MIXINS.contains(mixinClassName))        return advancement();
        return true;
    }

    /**
     * Log a summary of which mixins were applied vs skipped, and which
     * common strings were pre-warmed. Called from MemPlus.onServerStarted
     * so the report lands in the log once after the world finishes loading.
     */
    public static void reportAppliedMixins() {
        if (reported) return;
        reported = true;
        try {
            MemPlus.LOGGER.info("[MemPlus] ================================ Mixin Status ================================");
            MemPlus.LOGGER.info("[MemPlus] {} mixins applied:", appliedMixins.size());
            for (String m : appliedMixins) {
                MemPlus.LOGGER.info("[MemPlus]   + {}", m);
            }
            if (!skippedMixins.isEmpty()) {
                MemPlus.LOGGER.info("[MemPlus] {} mixins skipped (disabled by config):", skippedMixins.size());
                for (String m : skippedMixins) {
                    MemPlus.LOGGER.info("[MemPlus]   - {}", m);
                }
            }
            MemPlus.LOGGER.info("[MemPlus] Pre-warmed {} canonical strings in the dedup table.",
                com.memplus.core.MemPlusWarmup.preWarmCount());
            MemPlus.LOGGER.info("[MemPlus] ===============================================================================");
        } catch (Throwable ignored) {}
    }
}
