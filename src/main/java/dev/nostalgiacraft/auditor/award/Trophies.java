package dev.nostalgiacraft.auditor.award;

import dev.nostalgiacraft.auditor.block.TrophyBlockEntity;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Trophies {
    private static final int PARTICLES = 24;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> PLACED = new HashMap<>();

    private Trophies() {
    }

    public static void add(ServerLevel level, BlockPos pos) {
        PLACED.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> placed = PLACED.get(level.dimension());
        if (placed != null) {
            placed.remove(pos);
        }
    }

    public static void forget(ServerLevel level) {
        PLACED.remove(level.dimension());
    }

    public static void clear() {
        PLACED.clear();
    }

    public static void rally(ServerPlayer player, TrophyKind kind) {
        for (ServerPlayer member : team(player)) {
            kind.effects().forEach(member::addEffect);
            member.displayClientMessage(Component.translatable("message.monitoringcenter_auditor.trophy." + kind.key(),
                    player.getDisplayName()), false);
        }
    }

    private static Set<ServerPlayer> team(ServerPlayer player) {
        Set<ServerPlayer> team = new LinkedHashSet<>(List.of(player));
        for (UUID member : Owners.members(player.server, Owners.ownerOf(player))) {
            ServerPlayer online = player.server.getPlayerList().getPlayer(member);
            if (online != null) {
                team.add(online);
            }
        }
        return team;
    }

    public static void celebrate(MinecraftServer server, UUID owner, boolean fanfare) {
        for (ServerLevel level : server.getAllLevels()) {
            for (BlockPos pos : List.copyOf(PLACED.getOrDefault(level.dimension(), Set.of()))) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TrophyBlockEntity trophy && owner.equals(trophy.owner())) {
                    trophy.refreshRecord();
                    if (fanfare) {
                        play(level, pos);
                    }
                }
            }
        }
    }

    private static void play(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, PARTICLES,
                0.3, 0.5, 0.3, 0.2);
    }
}
