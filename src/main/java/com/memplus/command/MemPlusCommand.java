package com.memplus.command;

import com.memplus.MemPlus;
import com.memplus.blockstate.StateCompactifier;
import com.memplus.chunk.PaletteCompactifier;
import com.memplus.core.MemPlusStats;
import com.memplus.nbt.NbtCompactifier;
import com.memplus.pool.StringDeduplicator;
import com.memplus.registry.RegistryCompactifier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * `/memplus` command for in-game stats without server shutdown.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code /memplus stats} — print per-subsystem savings as chat text.
 *   <li>{@code /memplus dump} — print full report + JVM heap state to the log.
 *   <li>{@code /memplus gc} — invoke System.gc() (no-op on most JVMs but
 *       useful for testing with {@code -XX:+ExplicitGCInvokesConcurrent}).
 * </ul>
 *
 * <p>Permissions: op-level 2 (can run from command blocks).
 */
@EventBusSubscriber(modid = MemPlus.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MemPlusCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("memplus")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("stats")
                    .executes(MemPlusCommand::stats))
                .then(Commands.literal("dump")
                    .executes(MemPlusCommand::dump))
                .then(Commands.literal("gc")
                    .executes(MemPlusCommand::gc))
        );
    }

    private static int stats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        long states  = StateCompactifier.shared().estimatedBytesSaved();
        long palettes = PaletteCompactifier.shared().estimatedBytesSaved();
        long nbts   = NbtCompactifier.shared().estimatedBytesSaved();
        long regs   = RegistryCompactifier.shared().estimatedBytesSaved();
        long strings = StringDeduplicator.shared().estimatedBytesSaved();
        long total = states + palettes + nbts + regs + strings;

        src.sendSuccess(() -> Component.literal(
            "[MemPlus] BlockState: " + (states/1024) + " KiB | "
            + "Palette: " + (palettes/1024) + " KiB | "
            + "NBT: " + (nbts/1024) + " KiB | "
            + "Registry: " + (regs/1024) + " KiB | "
            + "Strings: " + (strings/1024) + " KiB"
        ), false);
        src.sendSuccess(() -> Component.literal(
            "[MemPlus] Total estimated savings: " + (total/1024) + " KiB"
        ), false);
        src.sendSuccess(() -> Component.literal(
            "[MemPlus] StringDedup lookups: " + StringDeduplicator.shared().lookups()
            + " | hits: " + StringDeduplicator.shared().hits()
        ), false);
        src.sendSuccess(() -> Component.literal(
            "[MemPlus] BlockStates compactified: " + StateCompactifier.shared().statesCompactified()
            + " | Caches nulled: " + StateCompactifier.shared().cachesNulled()
            + " | Maps compacted: " + StateCompactifier.shared().valuesMapsCompacted()
        ), false);
        return 1;
    }

    private static int dump(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        MemPlusStats.report();
        src.sendSuccess(() -> Component.literal(
            "[MemPlus] Full report written to log file (logs/latest.log)."
        ), false);
        return 1;
    }

    private static int gc(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        try {
            System.gc();
            src.sendSuccess(() -> Component.literal(
                "[MemPlus] GC hint issued (most JVMs ignore unless "
                + "-XX:+ExplicitGCInvokesConcurrent is set)."
            ), false);
        } catch (Throwable t) {
            src.sendFailure(Component.literal("[MemPlus] GC hint failed: " + t));
        }
        return 1;
    }
}
