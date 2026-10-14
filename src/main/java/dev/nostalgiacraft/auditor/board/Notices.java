package dev.nostalgiacraft.auditor.board;

import dev.nostalgiacraft.monitor.data.MonitorNetwork;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class Notices {
    private Notices() {
    }

    public static void send(MinecraftServer server, UUID owner, Component message) {
        MonitorNetwork inboxHolder = MonitorNetwork.get(server.overworld());
        for (UUID member : Owners.members(server, owner)) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                online.sendSystemMessage(message);
            } else {
                inboxHolder.inbox().add(member, Component.Serializer.toJson(message, server.registryAccess()), System.currentTimeMillis());
                inboxHolder.setDirty();
            }
        }
    }
}
