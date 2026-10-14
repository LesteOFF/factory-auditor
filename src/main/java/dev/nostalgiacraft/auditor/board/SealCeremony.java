package dev.nostalgiacraft.auditor.board;

import dev.nostalgiacraft.auditor.award.Trophies;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.tab.TabMedals;
import dev.nostalgiacraft.monitor.panel.ValueFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class SealCeremony {
    private SealCeremony() {
    }

    public static Standings.Outcome celebrate(MinecraftServer server, SealData seal) {
        Leaderboard board = Leaderboard.get(server);
        boolean wasMonopolist = board.standings().title(seal.owner()) == Title.MONOPOLIST;
        Standings.Outcome outcome = board.record(seal.owner(), seal.ownerName(), seal.item().toString(), seal.tier(), seal.rate());
        if (outcome.newHolder() && outcome.previousHolder() != null) {
            announce(server, Component.translatable("message.monitoringcenter_auditor.record", seal.ownerName(),
                    seal.auditedItem().getDescription(), ValueFormat.number(seal.rate())));
            TabMedals.refresh(server, outcome.previousHolder());
        }
        if (!wasMonopolist && board.standings().title(seal.owner()) == Title.MONOPOLIST) {
            announce(server, Component.translatable("message.monitoringcenter_auditor.monopoly", seal.ownerName()));
        }
        TabMedals.refresh(server, seal.owner());
        Trophies.celebrate(server, seal.owner(), outcome.newHolder());
        return outcome;
    }

    private static void announce(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }
}
