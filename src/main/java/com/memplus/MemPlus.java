package com.memplus;

import com.memplus.config.MemPlusConfig;
import com.memplus.core.MemPlusBootstrap;
import com.memplus.core.MemPlusStats;
import com.memplus.pool.StringDeduplicator;
import com.memplus.registry.RegistryCompactifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MemPlus entrypoint.
 *
 * The mod is intentionally tiny here: every optimization lives in a Mixin that is
 * conditionally loaded by {@link com.memplus.core.MemPlusMixinPlugin} based on
 * the active {@link MemPlusConfig}. The mod class just wires the config and
 * logs aggregate memory deltas at server lifecycle events so users can verify
 * the savings in practice.
 */
@Mod(MemPlus.MODID)
public class MemPlus {
    public static final String MODID = "memplus";
    public static final String NAME = "MemPlus";
    public static final String VERSION = "1.2.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public MemPlus(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, MemPlusConfig.SPEC, "memplus-common.toml");

        // Warm subsystems. We instantiate them early so that any class-loading
        // overhead happens during the loading screen rather than the first tick.
        MemPlusBootstrap.warm();

        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        }
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onServerStarted(ServerStartedEvent e) {
        MemPlusStats.markBaseline();
        long dedupSaved = StringDeduplicator.shared().estimatedBytesSaved();
        long registrySaved = RegistryCompactifier.shared().estimatedBytesSaved();
        MemPlus.LOGGER.info("[MemPlus] Server started. Estimated initial savings: strings ~{} KiB, registries ~{} KiB.",
                dedupSaved / 1024, registrySaved / 1024);
        com.memplus.core.MemPlusMixinPlugin.reportAppliedMixins();
    }

    private void onServerStopping(ServerStoppingEvent e) {
        MemPlusStats.report();
        // Final flush of any lazily-held memory before unload.
        StringDeduplicator.shared().flush();
        RegistryCompactifier.shared().flush();
    }
}
