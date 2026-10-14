package dev.nostalgiacraft.auditor.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AuditorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ACCELERATION;
    public static final ModConfigSpec.BooleanValue ENERGY_SAVING;
    public static final ModConfigSpec.IntValue PLAQUE_RADIUS;
    public static final ModConfigSpec.IntValue MAX_PLAQUES_PER_OWNER;
    public static final ModConfigSpec.IntValue MAX_MACHINES_PER_PLAQUE;
    public static final ModConfigSpec.IntValue CERTIFICATION_MINUTES;
    public static final ModConfigSpec.IntValue DEFAULT_INTERVAL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ACCELERATION = builder.comment("Whether Plaques give extra ticks to machines around them")
                .define("acceleration", true);
        ENERGY_SAVING = builder.comment("Whether accelerated machines get part of their FE use back (5% to 35% by seal tier, up to 45% with a seal of excellence); needs acceleration on")
                .define("energySaving", true);
        PLAQUE_RADIUS = builder.comment("Radius, in blocks, of the machines a Plaque accelerates")
                .defineInRange("plaqueRadius", 4, 1, 8);
        MAX_PLAQUES_PER_OWNER = builder.comment("Plaques of the same owner that accelerate at the same time in a dimension")
                .defineInRange("maxPlaquesPerOwner", 10, 0, 64);
        MAX_MACHINES_PER_PLAQUE = builder.comment("Machines a single Plaque accelerates")
                .defineInRange("maxMachinesPerPlaque", 16, 0, 256);
        CERTIFICATION_MINUTES = builder.comment("Minutes a certification run lasts")
                .defineInRange("certificationMinutes", 5, 1, 60);
        DEFAULT_INTERVAL = builder.comment("Measuring interval, in ticks, of a newly placed Auditor")
                .defineInRange("defaultInterval", 20, 5, 1200);
        SPEC = builder.build();
    }

    private AuditorConfig() {
    }
}
