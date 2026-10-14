package dev.nostalgiacraft.auditor.rate;

import java.util.Arrays;

public final class RateHistory {
    public static final int SIZE = 10;
    private double[] values = new double[0];

    public void add(double rate) {
        double[] next = Arrays.copyOfRange(values, Math.max(0, values.length - SIZE + 1), values.length + 1);
        next[next.length - 1] = rate;
        values = next;
    }

    public double[] values() {
        return values.clone();
    }

    public void replace(double[] restored) {
        values = Arrays.copyOfRange(restored, Math.max(0, restored.length - SIZE), restored.length);
    }

    public void clear() {
        values = new double[0];
    }
}
