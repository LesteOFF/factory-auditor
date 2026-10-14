package dev.nostalgiacraft.auditor.award;

import dev.nostalgiacraft.auditor.MonitoringCenterAuditor;
import dev.nostalgiacraft.auditor.block.PlaqueBlockEntity;
import dev.nostalgiacraft.auditor.config.AuditorConfig;
import dev.nostalgiacraft.auditor.seal.SealData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class AwardedFactory {
    public static final TagKey<Block> NOT_ACCELERATED = TagKey.create(Registries.BLOCK, MonitoringCenterAuditor.id("not_accelerated"));
    public static final TagKey<Block> ENERGY_SAVING = TagKey.create(Registries.BLOCK, MonitoringCenterAuditor.id("energy_saving"));
    private static final Set<String> OWN_NAMESPACES = Set.of("monitoringcenter", MonitoringCenterAuditor.MODID);
    private static final int REBUILD_TICKS = 100;
    private static final int DIRTY_REBUILD_TICKS = 20;
    private static final int REFUND_TICKS = 20;
    private static final int EXCELLENT_RADIUS = 2;
    private static final double EXCELLENT_SAVING = 0.10;
    private static final Map<ResourceKey<Level>, Factory> FACTORIES = new HashMap<>();

    private static final class Factory {
        private final Set<BlockPos> plaques = new TreeSet<>();
        private Map<BlockPos, Boost> boosts = Map.of();
        private boolean dirty = true;
        private long nextRebuild;
        private long lastRebuild = Long.MIN_VALUE / 2;
    }

    private record Award(double bonus, double saving) {
        private Award strongest(Award other) {
            return new Award(Math.max(bonus, other.bonus), Math.max(saving, other.saving));
        }
    }

    private static final class Boost {
        private final Award award;
        private final EnergyRefund refund;
        @Nullable
        private final BlockCapabilityCache<IEnergyStorage, Direction> energy;
        private double progress;
        private long refunded;

        private Boost(Award award, EnergyRefund refund, @Nullable BlockCapabilityCache<IEnergyStorage, Direction> energy) {
            this.award = award;
            this.refund = refund;
            this.energy = energy;
        }

        @Nullable
        private IEnergyStorage consumer(ServerLevel level, BlockPos pos) {
            if (energy == null || !level.shouldTickBlocksAt(pos) || !level.getBlockState(pos).is(ENERGY_SAVING)) {
                return null;
            }
            IEnergyStorage storage = energy.getCapability();
            return storage != null && storage.canReceive() ? storage : null;
        }
    }

    private AwardedFactory() {
    }

    public static void add(ServerLevel level, BlockPos pos) {
        Factory factory = FACTORIES.computeIfAbsent(level.dimension(), key -> new Factory());
        factory.plaques.add(pos.immutable());
        factory.dirty = true;
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Factory factory = FACTORIES.get(level.dimension());
        if (factory != null && factory.plaques.remove(pos)) {
            factory.dirty = true;
        }
    }

    public static void changed(ServerLevel level) {
        Factory factory = FACTORIES.get(level.dimension());
        if (factory != null) {
            factory.dirty = true;
        }
    }

    public static void clear() {
        FACTORIES.clear();
    }

    public static void forget(ServerLevel level) {
        FACTORIES.remove(level.dimension());
    }

    public static double bonusAt(ServerLevel level, BlockPos pos) {
        Factory factory = FACTORIES.get(level.dimension());
        Boost boost = factory == null ? null : factory.boosts.get(pos);
        return boost == null ? 0 : boost.award.bonus();
    }

    public static long refundedAt(ServerLevel level, BlockPos pos) {
        Factory factory = FACTORIES.get(level.dimension());
        Boost boost = factory == null ? null : factory.boosts.get(pos);
        return boost == null ? 0 : boost.refunded;
    }

    public static void tick(ServerLevel level) {
        Factory factory = FACTORIES.get(level.dimension());
        if (factory == null || !AuditorConfig.ACCELERATION.get() || idle(level)) {
            return;
        }
        long now = level.getGameTime();
        if (now >= factory.nextRebuild || factory.dirty && now >= factory.lastRebuild + DIRTY_REBUILD_TICKS) {
            rebuild(level, factory);
        }
        Iterator<Map.Entry<BlockPos, Boost>> boosts = factory.boosts.entrySet().iterator();
        while (boosts.hasNext()) {
            Map.Entry<BlockPos, Boost> entry = boosts.next();
            Boost boost = entry.getValue();
            boost.progress += boost.award.bonus();
            if (boost.progress >= 1) {
                boost.progress -= 1;
                if (!tickOnce(level, entry.getKey(), boost)) {
                    boosts.remove();
                }
            }
        }
        if (now % REFUND_TICKS == 0 && AuditorConfig.ENERGY_SAVING.get()) {
            factory.boosts.forEach((pos, boost) -> refund(level, pos, boost));
        }
    }

    private static void refund(ServerLevel level, BlockPos pos, Boost boost) {
        IEnergyStorage storage = boost.consumer(level, pos);
        int due = boost.refund.due(REFUND_TICKS, boost.award.saving());
        if (storage != null && due > 0) {
            boost.refunded += storage.receiveEnergy(due, false);
        }
    }

    private static boolean idle(ServerLevel level) {
        return level.players().isEmpty() && level.getForcedChunks().isEmpty();
    }

    private static void rebuild(ServerLevel level, Factory factory) {
        factory.dirty = false;
        factory.lastRebuild = level.getGameTime();
        factory.nextRebuild = level.getGameTime() + REBUILD_TICKS;
        Map<UUID, Integer> plaquesPerOwner = new HashMap<>();
        Map<BlockPos, Award> awards = new LinkedHashMap<>();
        for (BlockPos pos : factory.plaques) {
            if (!level.shouldTickBlocksAt(pos) || !(level.getBlockEntity(pos) instanceof PlaqueBlockEntity plaque) || plaque.sealData() == null) {
                continue;
            }
            SealData seal = plaque.sealData();
            if (plaquesPerOwner.merge(seal.owner(), 1, Integer::sum) > AuditorConfig.MAX_PLAQUES_PER_OWNER.get()) {
                continue;
            }
            Award award = new Award(seal.sealTier().acceleration(),
                    seal.sealTier().energySaving() + (seal.excellent() ? EXCELLENT_SAVING : 0));
            int radius = AuditorConfig.PLAQUE_RADIUS.get() + (seal.excellent() ? EXCELLENT_RADIUS : 0);
            for (BlockPos machine : machinesAround(level, pos, radius)) {
                awards.merge(machine, award, Award::strongest);
            }
        }
        Map<BlockPos, Boost> boosts = new LinkedHashMap<>();
        awards.forEach((pos, award) -> boosts.put(pos, boost(level, pos, award, factory.boosts.get(pos))));
        factory.boosts = boosts;
    }

    private static Boost boost(ServerLevel level, BlockPos pos, Award award, @Nullable Boost previous) {
        EnergyRefund refund = previous == null ? new EnergyRefund() : previous.refund;
        BlockCapabilityCache<IEnergyStorage, Direction> energy = !level.getBlockState(pos).is(ENERGY_SAVING) ? null
                : previous != null && previous.energy != null ? previous.energy
                : BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, pos, receivingSide(level, pos));
        Boost boost = new Boost(award, refund, energy);
        boost.progress = previous == null ? 0 : Math.min(previous.progress, 1);
        boost.refunded = previous == null ? 0 : previous.refunded;
        return boost;
    }

    @Nullable
    private static Direction receivingSide(ServerLevel level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            if (storage != null && storage.canReceive()) {
                return side;
            }
        }
        return null;
    }

    private static List<BlockPos> machinesAround(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> machines = new ArrayList<>();
        for (int chunkX = SectionPos.blockToSectionCoord(center.getX() - radius); chunkX <= SectionPos.blockToSectionCoord(center.getX() + radius); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius); chunkZ <= SectionPos.blockToSectionCoord(center.getZ() + radius); chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();
                    if (withinCube(center, pos, radius) && accelerable(level, entity)) {
                        machines.add(pos);
                    }
                }
            }
        }
        machines.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return machines.subList(0, Math.min(machines.size(), AuditorConfig.MAX_MACHINES_PER_PLAQUE.get()));
    }

    private static boolean withinCube(BlockPos center, BlockPos pos, int radius) {
        return Math.abs(pos.getX() - center.getX()) <= radius && Math.abs(pos.getY() - center.getY()) <= radius
                && Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    private static boolean accelerable(ServerLevel level, BlockEntity entity) {
        BlockState state = entity.getBlockState();
        return !entity.isRemoved() && !state.is(NOT_ACCELERATED)
                && !OWN_NAMESPACES.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())
                && state.getTicker(level, entity.getType()) != null;
    }

    private static boolean tickOnce(ServerLevel level, BlockPos pos, Boost boost) {
        if (!level.shouldTickBlocksAt(pos)) {
            return true;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || !accelerable(level, entity) || !entity.getType().isValid(level.getBlockState(pos))) {
            return false;
        }
        try {
            IEnergyStorage storage = boost.consumer(level, pos);
            int before = storage == null ? 0 : storage.getEnergyStored();
            tick(level, entity);
            if (storage != null) {
                boost.refund.observe(before, storage.getEnergyStored());
            }
            return true;
        } catch (RuntimeException failure) {
            MonitoringCenterAuditor.LOGGER.warn("Stopped accelerating {} at {}: its extra tick failed", entity.getType(), pos, failure);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void tick(ServerLevel level, T entity) {
        BlockState state = level.getBlockState(entity.getBlockPos());
        BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) entity.getType());
        if (ticker != null) {
            ticker.tick(level, entity.getBlockPos(), state, entity);
        }
    }
}
