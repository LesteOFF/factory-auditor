package dev.nostalgiacraft.auditor;

import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.monitor.metric.Metrics;
import dev.nostalgiacraft.monitor.metric.Sample;

public final class AuditorMetrics {
    public static final String AUDIT_RATE = "audit_rate";

    private AuditorMetrics() {
    }

    public static void register() {
        Metrics.direct(AUDIT_RATE, "/min", "up", (level, sensor) ->
                level.getBlockEntity(sensor.targetPos()) instanceof AuditorBlockEntity auditor
                        ? new Sample(auditor.ratePerMinute(), auditor.nextSealGoal()) : null);
    }
}
