package dev.nostalgiacraft.auditor.rate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CertificationRunTest {
    private static final double GOAL = 10;
    private static final int MINUTES = 3;

    private static CertificationRun.State minute(CertificationRun run, long items) {
        run.record(items);
        CertificationRun.State state = CertificationRun.State.RUNNING;
        for (int tick = 0; tick < CertificationRun.TICKS_PER_MINUTE; tick++) {
            state = run.tick();
        }
        return state;
    }

    @Test
    void failsOnTheFirstMinuteBelowTheGoal() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, MINUTES);
        assertEquals(CertificationRun.State.RUNNING, minute(run, 10));
        assertEquals(CertificationRun.State.FAILED, minute(run, 9));
        assertEquals(2, run.failedMinute());
    }

    @Test
    void aForgivenMinuteDoesNotFailTheRunButASecondOneDoes() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, MINUTES, 1);
        assertEquals(CertificationRun.State.RUNNING, minute(run, 2));
        assertEquals(CertificationRun.State.FAILED, minute(run, 3));
    }

    @Test
    void aForgivenMinuteStillLetsTheRunPass() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, 2, 1);
        minute(run, 5);
        assertEquals(CertificationRun.State.PASSED, minute(run, 10));
    }

    @Test
    void theForgivenMinuteIsLeftOutOfTheCertifiedRate() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, MINUTES, 1);
        minute(run, 12);
        minute(run, 2);
        assertEquals(CertificationRun.State.PASSED, minute(run, 12));
        assertEquals(12, run.averagePerMinute(), 1e-9);
    }

    @Test
    void aOneMinuteRunCannotBeForgiven() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, 1, 1);
        assertEquals(CertificationRun.State.FAILED, minute(run, 0));
    }

    @Test
    void approvesAFullRunAtOrAboveTheGoal() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, MINUTES);
        minute(run, 10);
        minute(run, 12);
        assertEquals(CertificationRun.State.PASSED, minute(run, 14));
        assertEquals(12, run.averagePerMinute(), 1e-9);
    }

    @Test
    void itemsAfterTheRunEndsAreIgnored() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, 1);
        minute(run, 10);
        run.record(1000);
        assertEquals(10, run.averagePerMinute(), 1e-9);
    }

    @Test
    void remainingSecondsCountDown() {
        CertificationRun run = new CertificationRun("minecraft:iron_ingot", 0, GOAL, MINUTES);
        assertEquals(180, run.remainingSeconds());
        for (int tick = 0; tick < 600; tick++) {
            run.tick();
        }
        assertEquals(150, run.remainingSeconds());
    }
}
