package dev.nostalgiacraft.auditor.seal;

public enum SealTier {
    COPPER(0.05, 0.05, 0xD9844C),
    IRON(0.10, 0.10, 0xD8D8D8),
    GOLD(0.15, 0.15, 0xF5C542),
    EMERALD(0.20, 0.20, 0x3FD07A),
    DIAMOND(0.30, 0.25, 0x5FE3E0),
    NETHERITE(0.40, 0.30, 0x7A6670),
    STAR(0.50, 0.35, 0xFFF3A0);

    private static final double STEP = 4;
    private static final char FIRST_MEDAL = '\uE000';
    private final double acceleration;
    private final double energySaving;
    private final int color;

    SealTier(double acceleration, double energySaving, int color) {
        this.acceleration = acceleration;
        this.energySaving = energySaving;
        this.color = color;
    }

    public static SealTier byIndex(int index) {
        return values()[Math.clamp(index, 0, values().length - 1)];
    }

    public double goal(double base) {
        return base * Math.pow(STEP, ordinal());
    }

    public double acceleration() {
        return acceleration;
    }

    public double energySaving() {
        return energySaving;
    }

    public int color() {
        return color;
    }

    public char medal() {
        return (char) (FIRST_MEDAL + ordinal());
    }
}
