package dev.nostalgiacraft.auditor.rate;

public final class CertificationRun {
    public static final int TICKS_PER_MINUTE = 1200;
    private static final int TICKS_PER_SECOND = 20;

    public enum State {
        RUNNING,
        PASSED,
        FAILED
    }

    private final String item;
    private final int tier;
    private final double goal;
    private final int minutes;
    private int forgivable;
    private int forgiven;
    private int minute;
    private int ticks;
    private long thisMinute;
    private long total;
    private State state = State.RUNNING;

    public CertificationRun(String item, int tier, double goal, int minutes) {
        this(item, tier, goal, minutes, 0);
    }

    public CertificationRun(String item, int tier, double goal, int minutes, int forgivable) {
        this.item = item;
        this.tier = tier;
        this.goal = goal;
        this.minutes = Math.max(1, minutes);
        this.forgivable = Math.clamp(forgivable, 0, this.minutes - 1);
    }

    public static CertificationRun restore(String item, int tier, double goal, int minutes, int forgivable, int forgiven, int minute,
                                           int ticks, long thisMinute, long total, State state) {
        CertificationRun run = new CertificationRun(item, tier, goal, minutes, forgivable);
        run.forgiven = forgiven;
        run.minute = minute;
        run.ticks = ticks;
        run.thisMinute = thisMinute;
        run.total = total;
        run.state = state;
        return run;
    }

    public void record(long count) {
        if (state == State.RUNNING) {
            thisMinute += count;
            total += count;
        }
    }

    public State tick() {
        if (state != State.RUNNING || ++ticks < TICKS_PER_MINUTE) {
            return state;
        }
        ticks = 0;
        minute++;
        boolean missed = thisMinute < goal;
        if (missed && forgivable == 0) {
            state = State.FAILED;
        } else {
            if (missed) {
                forgivable--;
                forgiven++;
                total -= thisMinute;
            }
            state = minute >= minutes ? State.PASSED : State.RUNNING;
        }
        thisMinute = 0;
        return state;
    }

    public int remainingSeconds() {
        int remainingTicks = (minutes - minute) * TICKS_PER_MINUTE - ticks;
        return Math.max(0, (remainingTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    public double averagePerMinute() {
        int counted = minute - forgiven;
        return counted <= 0 ? 0 : (double) total / counted;
    }

    public int failedMinute() {
        return state == State.FAILED ? minute : 0;
    }

    public String item() {
        return item;
    }

    public int tier() {
        return tier;
    }

    public double goal() {
        return goal;
    }

    public int minutes() {
        return minutes;
    }

    public int forgivable() {
        return forgivable;
    }

    public int forgiven() {
        return forgiven;
    }

    public int minute() {
        return minute;
    }

    public int ticks() {
        return ticks;
    }

    public long thisMinute() {
        return thisMinute;
    }

    public long total() {
        return total;
    }

    public State state() {
        return state;
    }
}
