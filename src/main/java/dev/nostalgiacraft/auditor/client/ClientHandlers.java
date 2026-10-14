package dev.nostalgiacraft.auditor.client;

import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.network.AuditorPayloads;
import net.minecraft.client.Minecraft;

public final class ClientHandlers {
    private ClientHandlers() {
    }

    public static void openAuditor(AuditorPayloads.OpenAuditor message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(message.pos()) instanceof AuditorBlockEntity auditor) {
            minecraft.setScreen(new AuditorScreen(auditor, message));
        }
    }
}
