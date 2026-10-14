package dev.nostalgiacraft.auditor.network;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.block.AuditorSettings;
import dev.nostalgiacraft.auditor.client.ClientHandlers;
import dev.nostalgiacraft.auditor.diploma.Diploma;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class AuditorPayloads {
    public static final String VERSION = "2";

    private AuditorPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
        return new CustomPacketPayload.Type<>(MonitoringCenterAuditor.id(name));
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(OpenAuditor.TYPE, OpenAuditor.CODEC, (message, context) -> ClientHandlers.openAuditor(message));
        registrar.playToServer(ConfigureAuditor.TYPE, ConfigureAuditor.CODEC, ServerHandlers.limited(ServerHandlers::configure));
        registrar.playToServer(ResetTotal.TYPE, ResetTotal.CODEC, ServerHandlers.limited(ServerHandlers::resetTotal));
        registrar.playToServer(StartCertification.TYPE, StartCertification.CODEC, ServerHandlers.limited(ServerHandlers::startCertification));
        registrar.playToServer(TakeTray.TYPE, TakeTray.CODEC, ServerHandlers.limited(ServerHandlers::takeTray));
    }

    public static void forget(UUID player) {
        ServerHandlers.forget(player);
    }

    public record OpenAuditor(BlockPos pos, double handGoal, BoardSnapshot board, int diplomas, int runMinutes)
            implements CustomPacketPayload {
        public static final Type<OpenAuditor> TYPE = typeOf("open_auditor");
        public static final StreamCodec<ByteBuf, OpenAuditor> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenAuditor::pos, ByteBufCodecs.DOUBLE, OpenAuditor::handGoal,
                BoardSnapshot.CODEC, OpenAuditor::board, ByteBufCodecs.VAR_INT, OpenAuditor::diplomas,
                ByteBufCodecs.VAR_INT, OpenAuditor::runMinutes, OpenAuditor::new);

        public static int mask(Set<Diploma> held) {
            return held.stream().mapToInt(diploma -> 1 << diploma.ordinal()).reduce(0, (left, right) -> left | right);
        }

        public Set<Diploma> diplomaSet() {
            EnumSet<Diploma> held = EnumSet.noneOf(Diploma.class);
            for (Diploma diploma : Diploma.values()) {
                if ((diplomas & 1 << diploma.ordinal()) != 0) {
                    held.add(diploma);
                }
            }
            return held;
        }

        public Type<OpenAuditor> type() {
            return TYPE;
        }
    }

    public record ConfigureAuditor(BlockPos pos, AuditorSettings settings) implements CustomPacketPayload {
        public static final Type<ConfigureAuditor> TYPE = typeOf("configure_auditor");
        public static final StreamCodec<ByteBuf, ConfigureAuditor> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ConfigureAuditor::pos, AuditorSettings.STREAM_CODEC, ConfigureAuditor::settings, ConfigureAuditor::new);

        public Type<ConfigureAuditor> type() {
            return TYPE;
        }
    }

    public record ResetTotal(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ResetTotal> TYPE = typeOf("reset_total");
        public static final StreamCodec<ByteBuf, ResetTotal> CODEC = BlockPos.STREAM_CODEC.map(ResetTotal::new, ResetTotal::pos);

        public Type<ResetTotal> type() {
            return TYPE;
        }
    }

    public record StartCertification(BlockPos pos, int tier) implements CustomPacketPayload {
        public static final Type<StartCertification> TYPE = typeOf("start_certification");
        public static final StreamCodec<ByteBuf, StartCertification> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, StartCertification::pos, ByteBufCodecs.VAR_INT, StartCertification::tier, StartCertification::new);

        public Type<StartCertification> type() {
            return TYPE;
        }
    }

    public record TakeTray(BlockPos pos) implements CustomPacketPayload {
        public static final Type<TakeTray> TYPE = typeOf("take_tray");
        public static final StreamCodec<ByteBuf, TakeTray> CODEC = BlockPos.STREAM_CODEC.map(TakeTray::new, TakeTray::pos);

        public Type<TakeTray> type() {
            return TYPE;
        }
    }
}
