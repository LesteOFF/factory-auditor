package dev.nostalgiacraft.auditor.award;

public final class EnergyRefund {
    private static final double ROUNDING = 1e-9;
    private int lastUse;
    private boolean measured;
    private double owed;

    public void observe(int before, int after) {
        lastUse = Math.max(0, before - after);
        measured = true;
    }

    public int due(int ticks, double fraction) {
        if (!measured) {
            return 0;
        }
        measured = false;
        owed += (double) lastUse * ticks * fraction;
        int whole = (int) Math.floor(owed + ROUNDING);
        owed = Math.max(0, owed - whole);
        return whole;
    }
}
