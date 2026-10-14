package dev.nostalgiacraft.auditor.block;

import dev.nostalgiacraft.auditor.AuditorTexts;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record AuditorDisplayText(MutableComponent value, MutableComponent caption) {
    private static final int SECONDS_PER_MINUTE = 60;

    public static AuditorDisplayText of(AuditorBlockEntity auditor) {
        double rate = auditor.ratePerMinute();
        String[] parts = ValueFormat.number(rate).split(" ");
        MutableComponent value = Component.literal(rate <= 0 ? "0" : parts[0]);
        int seconds = auditor.runSeconds();
        if (seconds >= 0) {
            return new AuditorDisplayText(value, Component.translatable("display.monitoringcenter_auditor.certifying",
                    "%d:%02d".formatted(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)));
        }
        if (rate <= 0) {
            return new AuditorDisplayText(value, AuditorTexts.status(auditor.status()));
        }
        String prefix = parts.length > 1 ? parts[1] + " " : "";
        return new AuditorDisplayText(value, Component.translatable("display.monitoringcenter_auditor.unit", prefix));
    }
}
