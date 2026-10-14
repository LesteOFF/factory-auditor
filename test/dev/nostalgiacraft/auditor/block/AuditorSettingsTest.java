package dev.nostalgiacraft.auditor.block;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditorSettingsTest {
    private static CompoundTag legacy(int transferOrdinal) {
        CompoundTag tag = AuditorSettings.DEFAULT.save();
        tag.putInt("transfer", transferOrdinal);
        return tag;
    }

    @Test
    void oldSplitAndPriorityLoadAsPass() {
        assertEquals(TransferMode.PASS, AuditorSettings.load(legacy(0)).transfer());
        assertEquals(TransferMode.PASS, AuditorSettings.load(legacy(1)).transfer());
    }

    @Test
    void oldConsumeLoadsAsConsume() {
        assertEquals(TransferMode.CONSUME, AuditorSettings.load(legacy(2)).transfer());
    }

    @Test
    void savedSettingsLoadBackUnchanged() {
        AuditorSettings consuming = AuditorSettings.DEFAULT.withTransfer(TransferMode.CONSUME).withNumbers(40, 3, 8);
        assertEquals(consuming, AuditorSettings.load(consuming.save()));
    }
}
