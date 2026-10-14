package dev.nostalgiacraft.auditor.rate;

import dev.nostalgiacraft.auditor.block.MeasureMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateWindowTest {
    private static final int INTERVAL = 20;

    private static boolean run(RateWindow window, int ticks, int itemsPerTick, int tolerance, MeasureMode mode) {
        boolean closed = false;
        for (int tick = 0; tick < ticks; tick++) {
            if (itemsPerTick > 0) {
                window.record(itemsPerTick);
            }
            closed = window.tick(INTERVAL, tolerance, mode);
        }
        return closed;
    }

    @Test
    void oneItemPerTickIsTwelveHundredPerMinute() {
        RateWindow window = new RateWindow();
        assertTrue(run(window, INTERVAL, 1, 0, MeasureMode.INSTANT));
        assertEquals(1200, window.ratePerMinute(), 1e-9);
    }

    @Test
    void intervalStaysOpenUntilItsLastTick() {
        RateWindow window = new RateWindow();
        assertFalse(run(window, INTERVAL - 1, 1, 0, MeasureMode.INSTANT));
        assertEquals(0, window.ratePerMinute(), 1e-9);
    }

    @Test
    void toleranceClosesTheIntervalEarly() {
        RateWindow window = new RateWindow();
        window.record(4);
        window.tick(INTERVAL, 3, MeasureMode.INSTANT);
        assertFalse(window.tick(INTERVAL, 3, MeasureMode.INSTANT));
        assertFalse(window.tick(INTERVAL, 3, MeasureMode.INSTANT));
        assertTrue(window.tick(INTERVAL, 3, MeasureMode.INSTANT));
        assertEquals(4 * 1200.0 / 4, window.ratePerMinute(), 1e-9);
    }

    @Test
    void smoothedAveragesWithThePreviousInterval() {
        RateWindow window = new RateWindow();
        run(window, INTERVAL, 1, 0, MeasureMode.SMOOTHED);
        double first = window.ratePerMinute();
        run(window, INTERVAL, 0, 0, MeasureMode.SMOOTHED);
        assertEquals(first / 2, window.ratePerMinute(), 1e-9);
    }

    @Test
    void aRestoredRateSeedsTheSmoothing() {
        RateWindow window = new RateWindow();
        window.restore(600);
        run(window, INTERVAL, 1, 0, MeasureMode.SMOOTHED);
        assertEquals(900, window.ratePerMinute(), 1e-9);
    }

    @Test
    void instantForgetsThePreviousInterval() {
        RateWindow window = new RateWindow();
        run(window, INTERVAL, 1, 0, MeasureMode.INSTANT);
        run(window, INTERVAL, 0, 0, MeasureMode.INSTANT);
        assertEquals(0, window.ratePerMinute(), 1e-9);
    }
}
