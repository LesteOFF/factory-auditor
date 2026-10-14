package dev.nostalgiacraft.auditor.diploma;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerksTest {
    private static final int BASE_MINUTES = 5;

    @Test
    void withoutDiplomasTheRunIsPlain() {
        Perks perks = Perks.of(EnumSet.noneOf(Diploma.class), BASE_MINUTES);
        assertEquals(BASE_MINUTES, perks.minutes());
        assertEquals(0, perks.forgivable());
        assertFalse(perks.excellent());
    }

    @Test
    void juniorForgivesOneMinute() {
        assertEquals(1, Perks.of(EnumSet.of(Diploma.JUNIOR), BASE_MINUTES).forgivable());
    }

    @Test
    void seniorCutsTheRunByFortyPercent() {
        assertEquals(3, Perks.of(EnumSet.of(Diploma.SENIOR), BASE_MINUTES).minutes());
        assertEquals(1, Perks.of(EnumSet.of(Diploma.SENIOR), 1).minutes());
    }

    @Test
    void masterIssuesAnExcellentSeal() {
        assertTrue(Perks.of(EnumSet.of(Diploma.MASTER), BASE_MINUTES).excellent());
    }

    @Test
    void perksAddUp() {
        Perks perks = Perks.of(EnumSet.allOf(Diploma.class), BASE_MINUTES);
        assertEquals(3, perks.minutes());
        assertEquals(1, perks.forgivable());
        assertTrue(perks.excellent());
    }
}
