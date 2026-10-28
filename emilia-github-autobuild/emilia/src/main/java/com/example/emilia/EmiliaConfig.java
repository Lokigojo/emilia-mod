package com.example.emilia;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EmiliaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TERRAIN_DESTRUCTION;
    public static final ModConfigSpec.BooleanValue PERMANENT_ICE;
    public static final ModConfigSpec.IntValue ABSOLUTE_ZERO_MAX_RADIUS;
    public static final ModConfigSpec.IntValue ABSOLUTE_ZERO_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue ABSOLUTE_ZERO_COLUMNS_PER_TICK;
    public static final ModConfigSpec.IntValue SHATTER_RADIUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("terrain");
        TERRAIN_DESTRUCTION = b.comment("Allow Glacial Surge, Frozen Shatter, the Frostbreaker slam and the deep permafrost of",
                "Absolute Zero to permanently destroy and replace terrain. Disable on shared servers.")
                .define("terrainDestruction", true);
        PERMANENT_ICE = b.comment("Ice made by Emilia's spells (walls, cages, spikes, frost walking, frost overlay) is solid and stays forever.",
                "Set to false to make it melt away on its own like temporary Spirit Ice.")
                .define("permanentIce", true);
        ABSOLUTE_ZERO_MAX_RADIUS = b.comment("Absolute Zero freezes the whole biome the caster stands in, up to this many blocks away.",
                "Only chunks that are loaded when it is cast are affected (so render/simulation distance matters).")
                .defineInRange("absoluteZeroMaxRadius", 192, 32, 512);
        ABSOLUTE_ZERO_INTERVAL_TICKS = b.comment("Ticks between the five stages of Absolute Zero (200 = 10 seconds).")
                .defineInRange("absoluteZeroIntervalTicks", 200, 20, 1200);
        ABSOLUTE_ZERO_COLUMNS_PER_TICK = b.comment("How many block columns Absolute Zero freezes per tick. Lower this if the server stutters.")
                .defineInRange("absoluteZeroColumnsPerTick", 250, 20, 2000);
        SHATTER_RADIUS = b.comment("Radius of the Frozen Shatter crater.")
                .defineInRange("shatterRadius", 6, 3, 10);
        b.pop();
        SPEC = b.build();
    }

    private EmiliaConfig() {}
}
