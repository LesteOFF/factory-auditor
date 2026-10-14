package dev.nostalgiacraft.auditor.rate;

import dev.nostalgiacraft.auditor.block.MeasureMode;

public final class RateWindow {
    public static final double TICKS_PER_MINUTE = 1200;
    private long counted;
    private int elapsed;
    private int idle;
    private boolean receivedThisTick;
    private double rate;

    public void record(int items) {
        counted += items;
        receivedThisTick = true;
    }

    public boolean tick(int interval, int tolerance, MeasureMode mode) {
        elapsed++;
        idle = receivedThisTick ? 0 : idle + 1;
        receivedThisTick = false;
        boolean quiet = tolerance > 0 && idle >= tolerance;
        if (elapsed < interval && !quiet) {
            return false;
        }
        double current = counted * TICKS_PER_MINUTE / elapsed;
        rate = mode == MeasureMode.SMOOTHED ? (rate + current) / 2 : current;
        counted = 0;
        elapsed = 0;
        idle = 0;
        return true;
    }

    public double ratePerMinute() {
        return rate;
    }

    public void restore(double restoredRate) {
        rate = restoredRate;
    }

    public void reset() {
        counted = 0;
        elapsed = 0;
        idle = 0;
        rate = 0;
    }
}
