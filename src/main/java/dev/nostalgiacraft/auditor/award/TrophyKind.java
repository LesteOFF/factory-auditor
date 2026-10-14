package dev.nostalgiacraft.auditor.award;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;
import java.util.Locale;

public enum TrophyKind {
    BRONZE(0, -1),
    GOLD(1, 0);

    private static final int DURATION_TICKS = 6000;
    private final int hasteLevel;
    private final int speedLevel;

    TrophyKind(int hasteLevel, int speedLevel) {
        this.hasteLevel = hasteLevel;
        this.speedLevel = speedLevel;
    }

    public List<MobEffectInstance> effects() {
        MobEffectInstance haste = new MobEffectInstance(MobEffects.DIG_SPEED, DURATION_TICKS, hasteLevel);
        return speedLevel < 0 ? List.of(haste) : List.of(haste, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, speedLevel));
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
