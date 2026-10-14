package dev.nostalgiacraft.auditor.compat.curios;

import dev.nostalgiacraft.auditor.diploma.Diplomas;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static void register() {
        Diplomas.useEquipmentCheck((player, item) -> CuriosApi.getCuriosInventory(player).map(curios -> curios.isEquipped(item)).orElse(false));
    }
}
