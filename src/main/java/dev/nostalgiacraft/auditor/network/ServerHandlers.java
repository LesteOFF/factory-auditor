package dev.nostalgiacraft.auditor.network;

import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.config.AuditorConfig;
import dev.nostalgiacraft.auditor.diploma.Diplomas;
import dev.nostalgiacraft.auditor.diploma.Perks;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.block.ChannelAccess;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

final class ServerHandlers {
    private static final int LIMIT_WINDOW_TICKS = 20;
    private static final int PACKETS_PER_WINDOW = 40;
    private static final RateLimit LIMIT = new RateLimit(LIMIT_WINDOW_TICKS, PACKETS_PER_WINDOW);

    private ServerHandlers() {
    }

    static <T extends CustomPacketPayload> IPayloadHandler<T> limited(IPayloadHandler<T> handler) {
        return (message, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (LIMIT.allow(player.getUUID(), player.server.getTickCount())) {
                handler.handle(message, context);
            }
        };
    }

    static void forget(UUID player) {
        LIMIT.forget(player);
    }

    @Nullable
    static AuditorBlockEntity editable(IPayloadContext context, BlockPos pos) {
        if (!(context.player() instanceof ServerPlayer player) || !ChannelAccess.reachable(player, pos)) {
            return null;
        }
        return player.level().getBlockEntity(pos) instanceof AuditorBlockEntity auditor && auditor.canUse(player) ? auditor : null;
    }

    static void configure(AuditorPayloads.ConfigureAuditor message, IPayloadContext context) {
        AuditorBlockEntity auditor = editable(context, message.pos());
        if (auditor != null) {
            auditor.applySettings(message.settings());
        }
    }

    static void resetTotal(AuditorPayloads.ResetTotal message, IPayloadContext context) {
        AuditorBlockEntity auditor = editable(context, message.pos());
        if (auditor != null) {
            auditor.resetTotal();
        }
    }

    static void startCertification(AuditorPayloads.StartCertification message, IPayloadContext context) {
        AuditorBlockEntity auditor = editable(context, message.pos());
        if (auditor == null) {
            return;
        }
        ServerPlayer player = (ServerPlayer) context.player();
        if (auditor.run() != null) {
            player.displayClientMessage(Component.translatable("message.monitoringcenter_auditor.certification.already_running"), true);
            return;
        }
        boolean started = auditor.startCertification(Owners.ownerOf(player), player.getGameProfile().getName(), player.getMainHandItem(),
                SealTier.byIndex(message.tier()), Perks.of(Diplomas.held(player), AuditorConfig.CERTIFICATION_MINUTES.get()));
        player.displayClientMessage(Component.translatable("message.monitoringcenter_auditor.certification."
                + (started ? "started" : "needs_item")), true);
    }

    static void takeTray(AuditorPayloads.TakeTray message, IPayloadContext context) {
        AuditorBlockEntity auditor = editable(context, message.pos());
        if (auditor != null) {
            auditor.takeTray((ServerPlayer) context.player());
        }
    }
}
