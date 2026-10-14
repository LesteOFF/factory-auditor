package dev.nostalgiacraft.auditor.seal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SealTierTest {
    private static final double BASE = 60;

    @Test
    void thereAreSevenTiers() {
        assertEquals(7, SealTier.values().length);
    }

    @Test
    void eachTierMultipliesTheGoalByFour() {
        assertEquals(BASE, SealTier.values()[0].goal(BASE), 1e-9);
        for (int index = 1; index < SealTier.values().length; index++) {
            assertEquals(SealTier.values()[index - 1].goal(BASE) * 4, SealTier.values()[index].goal(BASE), 1e-9);
        }
    }

    @Test
    void accelerationGrowsAndStopsAtFiftyPercent() {
        for (int index = 1; index < SealTier.values().length; index++) {
            assertTrue(SealTier.values()[index].acceleration() > SealTier.values()[index - 1].acceleration());
        }
        assertEquals(0.5, SealTier.values()[SealTier.values().length - 1].acceleration(), 1e-9);
    }

    @Test
    void energySavingGrowsFromFiveToThirtyFivePercent() {
        assertEquals(0.05, SealTier.values()[0].energySaving(), 1e-9);
        for (int index = 1; index < SealTier.values().length; index++) {
            assertTrue(SealTier.values()[index].energySaving() > SealTier.values()[index - 1].energySaving());
        }
        assertEquals(0.35, SealTier.values()[SealTier.values().length - 1].energySaving(), 1e-9);
    }

    @Test
    void outOfRangeIndexesAreClamped() {
        assertEquals(SealTier.values()[0], SealTier.byIndex(-3));
        assertEquals(SealTier.values()[SealTier.values().length - 1], SealTier.byIndex(99));
    }
}
