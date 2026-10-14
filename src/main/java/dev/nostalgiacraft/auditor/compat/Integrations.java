package dev.nostalgiacraft.auditor.compat;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import net.neoforged.fml.ModList;

import java.util.Map;

public final class Integrations {
    private static final String ENTRY_POINT = "register";
    private static final Map<String, String> COMPAT_CLASS_BY_MOD = Map.of(
            "create", "dev.nostalgiacraft.auditor.compat.create.CreateCompat",
            "curios", "dev.nostalgiacraft.auditor.compat.curios.CuriosCompat");

    private Integrations() {
    }

    public static void register() {
        COMPAT_CLASS_BY_MOD.forEach((mod, className) -> {
            if (ModList.get().isLoaded(mod)) {
                load(mod, className);
            }
        });
    }

    private static void load(String mod, String className) {
        try {
            Class.forName(className).getMethod(ENTRY_POINT).invoke(null);
        } catch (ReflectiveOperationException | LinkageError failure) {
            MonitoringCenterAuditor.LOGGER.warn("{} integration disabled: it does not match the installed version", mod, failure);
        }
    }
}
