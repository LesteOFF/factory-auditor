package dev.nostalgiacraft.auditor;

import dev.nostalgiacraft.auditor.block.AuditorStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public final class AuditorTexts {
    private static final String SCREEN = "screen.monitoringcenter_auditor.";

    private AuditorTexts() {
    }

    public static MutableComponent screen(String key, Object... args) {
        return Component.translatable(SCREEN + key, args);
    }

    public static MutableComponent option(String group, Enum<?> value) {
        return screen(group + "." + value.name().toLowerCase(Locale.ROOT));
    }

    public static MutableComponent status(AuditorStatus status) {
        return option("status", status);
    }
}
