package dev.nostalgiacraft.auditor.tab;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.board.Leaderboard;
import dev.nostalgiacraft.auditor.board.Standings;
import dev.nostalgiacraft.auditor.board.Title;
import dev.nostalgiacraft.auditor.diploma.Diploma;
import dev.nostalgiacraft.auditor.diploma.Diplomas;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class TabMedals {
    public static final ResourceLocation FONT = MonitoringCenterAuditor.id("medals");

    private TabMedals() {
    }

    public static void decorate(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Standings standings = Leaderboard.get(player.server).standings();
        UUID owner = Owners.ownerOf(player);
        int tier = standings.bestTier(owner);
        Optional<Diploma> diploma = player instanceof FakePlayer ? Optional.empty() : Diplomas.shown(player);
        if (tier < 0 && diploma.isEmpty()) {
            return;
        }
        Component name = event.getDisplayName() != null ? event.getDisplayName()
                : PlayerTeam.formatNameForTeam(player.getTeam(), player.getName());
        MutableComponent decorated = Component.empty();
        if (tier >= 0) {
            decorated.append(medal(SealTier.byIndex(tier))).append(" ");
        }
        decorated.append(name);
        diploma.ifPresent(held -> decorated.append(" ").append(glyph(held.glyph())));
        if (tier >= 0) {
            decorated.append(Component.literal(" x" + standings.seals(owner)).withStyle(ChatFormatting.GRAY));
        }
        Title title = standings.title(owner);
        if (title != Title.NONE) {
            decorated.append(Component.literal(" ")).append(titleName(title).withStyle(ChatFormatting.GOLD));
        }
        event.setDisplayName(decorated);
    }

    public static MutableComponent medal(SealTier tier) {
        return glyph(tier.medal());
    }

    public static MutableComponent glyph(char glyph) {
        return Component.literal(String.valueOf(glyph)).withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE));
    }

    public static MutableComponent titleName(Title title) {
        return Component.translatable("title.monitoringcenter_auditor." + title.name().toLowerCase(Locale.ROOT));
    }

    public static void refresh(MinecraftServer server, UUID owner) {
        for (UUID member : Owners.members(server, owner)) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                player.refreshTabListName();
            }
        }
    }
}
