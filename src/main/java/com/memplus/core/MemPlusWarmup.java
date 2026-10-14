package com.memplus.core;

import com.memplus.MemPlus;
import com.memplus.config.MemPlusConfig;
import com.memplus.pool.StringDeduplicator;

import java.util.List;

/**
 * Pre-warms the {@link StringDeduplicator} with the most commonly used
 * Minecraft strings, so they're canonicalized before any user-facing
 * code even gets a chance to allocate them.
 *
 * <p>The list below is intentionally conservative: it only includes
 * strings that are (a) hardcoded in vanilla and (b) very high frequency.
 * Including mod-specific strings here would be guesswork.
 *
 * <p>Each pre-warm call costs one WeakReference allocation (16 bytes)
 * plus the underlying String (which already exists in memory). On a
 * ~200-string pre-warm, the table overhead is ~3 KiB and the savings
 * across a 10k-entity world are several MiB.
 */
public final class MemPlusWarmup {

    /** Vanilla tag keys — appear in nearly every saved chunk and entity. */
    private static final List<String> NBT_KEYS = List.of(
            "id", "x", "y", "z", "Pos", "Motion", "Rotation", "Health", "MaxHealth",
            "HurtTime", "HurtTime", "DeathTime", "Attributes", "ActiveEffects",
            "Items", "Item", "Count", "Slot", "tag", "Tags", "CustomName",
            "CustomNameVisible", "Silent", "NoGravity", "Glowing", "Invulnerable",
            "Fire", "OnGround", "FallDistance", "Dimension", "UUID", "owner",
            "pair", "Saddle", "Tame", "Owner", "Eating", "isBreeding", "Breed",
            "ForcedAge", "LoveCause", "LoveCooldown", "InLove", "HandItems",
            "ArmorItems", "HandDropChances", "ArmorDropChances", "DeathLootTable",
            "DeathLootTableSeed", "CanUpdateLoot", "DeathLootTableSeed",
            "Paper.AsyncChunksLoad", "UpdateLevel", "Bukkit.updateLevel",
            "blockstate", "block_states", "block_states", "block_state",
            "Name", "id", "count", "components", "damage", "unbreakable",
            "enchantments", "enchantment", "RepairCost", "HideFlags",
            "BlockEntityTag", "EntityTag", "display", "Lore", "color",
            "Trim", "CreativeSlot", "TrimMaterial", "TrimPattern"
    );

    /** Vanilla entity ids — used by every entity save and spawn packet. */
    private static final List<String> ENTITY_IDS = List.of(
            "minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:chicken",
            "minecraft:horse", "minecraft:zombie", "minecraft:skeleton",
            "minecraft:creeper", "minecraft:spider", "minecraft:enderman",
            "minecraft:witch", "minecraft:slime", "minecraft:zombie_pigman",
            "minecraft:ghast", "minecraft:blaze", "minecraft:magma_cube",
            "minecraft:villager", "minecraft:iron_golem", "minecraft:snow_golem",
            "minecraft:bat", "minecraft:squid", "minecraft:cod", "minecraft:salmon",
            "minecraft:pufferfish", "minecraft:tropical_fish", "minecraft:dolphin",
            "minecraft:turtle", "minecraft:axolotl", "minecraft:allay",
            "minecraft:warden", "minecraft:goat", "minecraft:fox", "minecraft:panda",
            "minecraft:polar_bear", "minecraft:bee", "minecraft:parrot", "minecraft:cat",
            "minecraft:ocelot", "minecraft:wolf", "minecraft:donkey", "minecraft:mule",
            "minecraft:llama", "minecraft:trader_llama", "minecraft:rabbit",
            "minecraft:ender_dragon", "minecraft:wither", "minecraft:elder_guardian",
            "minecraft:guardian", "minecraft:shulker", "minecraft:evoker",
            "minecraft:vindicator", "minecraft:illusioner", "minecraft:pillager",
            "minecraft:ravager", "minecraft:vex", "minecraft:phantom",
            "minecraft:husk", "minecraft:drowned", "minecraft:stray",
            "minecraft:cave_spider", "minecraft:endermite", "minecraft:silverfish",
            "minecraft:wither_skeleton", "minecraft:skeleton_horse",
            "minecraft:zombie_horse", "minecraft:zombie_villager", "minecraft:strider",
            "minecraft:hoglin", "minecraft:piglin", "minecraft:piglin_brute",
            "minecraft:zoglin", "minecraft:mooshroom", "minecraft:armor_stand",
            "minecraft:item", "minecraft:xp_orb", "minecraft:falling_block",
            "minecraft:tnt", "minecraft:experience_orb", "minecraft:arrow",
            "minecraft:snowball", "minecraft:egg", "minecraft:fireball",
            "minecraft:small_fireball", "minecraft:ender_pearl",
            "minecraft:ender_eye", "minecraft:potion", "minecraft:llama_spit",
            "minecraft:firework_rocket", "minecraft:shulker_bullet",
            "minecraft:fishing_bobber", "minecraft:boat", "minecraft:minecart",
            "minecraft:chest_minecart", "minecraft:commandblock_minecart",
            "minecraft:furnace_minecart", "minecraft:hopper_minecart",
            "minecraft:spawner_minecart", "minecraft:tnt_minecart"
    );

    /** Common block prefixes — used by recipes, advancement, command paths. */
    private static final List<String> BLOCK_PREFIXES = List.of(
            "minecraft:block/stone", "minecraft:block/dirt", "minecraft:block/grass_block",
            "minecraft:block/sand", "minecraft:block/gravel", "minecraft:block/oak_log",
            "minecraft:block/spruce_log", "minecraft:block/birch_log",
            "minecraft:block/cobblestone", "minecraft:block/planks", "minecraft:block/air",
            "minecraft:block/water", "minecraft:block/lava", "minecraft:block/bedrock",
            "minecraft:block/netherrack", "minecraft:block/end_stone",
            "minecraft:block/obsidian", "minecraft:block/diamond_ore",
            "minecraft:block/iron_ore", "minecraft:block/gold_ore",
            "minecraft:block/coal_ore", "minecraft:block/copper_ore",
            "minecraft:block/redstone_ore", "minecraft:block/lapis_ore",
            "minecraft:block/emerald_ore", "minecraft:block/nether_gold_ore",
            "minecraft:block/deepslate", "minecraft:block/tuff",
            "minecraft:block/cube_all", "minecraft:block/cube",
            "minecraft:block/cube_column", "minecraft:block/cube_column_horizontal",
            "minecraft:block/cross", "minecraft:block/template_farmland",
            "minecraft:block/template_glazed_terracotta", "minecraft:block/leaves",
            "minecraft:block/cube_bottom_top", "minecraft:block/cube_top",
            "minecraft:crafting", "minecraft:smelting", "minecraft:smithing",
            "minecraft:blasting", "minecraft:smoking", "minecraft:stonecutting",
            "minecraft:story/", "minecraft:nether/", "minecraft:end/",
            "minecraft:adventure/", "minecraft:husbandry/"
    );

    /** Common dimension ids. */
    private static final List<String> DIMENSIONS = List.of(
            "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
            "minecraft:overworld_caves"
    );

    /** Common advancement / recipe id prefixes. */
    private static final List<String> ID_PREFIXES = List.of(
            "minecraft:recipes/", "minecraft:story/", "minecraft:nether/",
            "minecraft:end/", "minecraft:adventure/", "minecraft:husbandry/"
    );

    /** Atlas and sprite prefixes. */
    private static final List<String> ATLAS_PREFIXES = List.of(
            "minecraft:atlas/blocks", "minecraft:atlas/mob_effects",
            "minecraft:atlas/particles", "minecraft:atlas/banners",
            "minecraft:atlas/shields", "minecraft:atlas/beds",
            "minecraft:atlas/chest", "minecraft:atlas/decorated_pot"
    );

    /**
     * Pre-warm the dedup table. Called once during MemPlusBootstrap.
     *
     * @return number of unique strings added to the dedup table.
     */
    public static int preWarm() {
        StringDeduplicator d = StringDeduplicator.shared();
        int count = 0;
        for (String s : NBT_KEYS)        { d.dedup(s); count++; }
        for (String s : ENTITY_IDS)      { d.dedup(s); count++; }
        for (String s : BLOCK_PREFIXES)  { d.dedup(s); count++; }
        for (String s : DIMENSIONS)     { d.dedup(s); count++; }
        for (String s : ID_PREFIXES)    { d.dedup(s); count++; }
        for (String s : ATLAS_PREFIXES)  { d.dedup(s); count++; }
        preWarmCount = count;
        MemPlus.LOGGER.info("[MemPlus] Pre-warmed {} canonical strings.", count);
        return count;
    }

    private static volatile int preWarmCount = 0;

    /** Returns the number of strings pre-warmed by {@link #preWarm()}, or 0 if not run yet. */
    public static int preWarmCount() {
        return preWarmCount;
    }
}
