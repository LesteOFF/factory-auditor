package dev.nostalgiacraft.auditor.block;

import dev.nostalgiacraft.auditor.award.Milestone;
import dev.nostalgiacraft.auditor.award.MilestonePrizes;
import dev.nostalgiacraft.auditor.award.Trophies;
import dev.nostalgiacraft.auditor.board.Leaderboard;
import dev.nostalgiacraft.auditor.board.Notices;
import dev.nostalgiacraft.auditor.board.SealCeremony;
import dev.nostalgiacraft.auditor.board.Standings;
import dev.nostalgiacraft.auditor.diploma.Perks;
import dev.nostalgiacraft.auditor.goal.AuditGoals;
import dev.nostalgiacraft.auditor.rate.CertificationRun;
import dev.nostalgiacraft.auditor.rate.ItemTally;
import dev.nostalgiacraft.auditor.rate.RateHistory;
import dev.nostalgiacraft.auditor.rate.RateWindow;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealItem;
import dev.nostalgiacraft.auditor.seal.SealTier;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuditorBlockEntity extends BlockEntity {
    private final RateWindow window = new RateWindow();
    private final RateHistory history = new RateHistory();
    private final ItemTally tally = new ItemTally();
    private final Map<Direction, AuditorInput> inputs = new EnumMap<>(Direction.class);
    private final Map<Direction, BlockCapabilityCache<IItemHandler, Direction>> outputs = new EnumMap<>(Direction.class);
    private final List<ItemStack> tray = new ArrayList<>();
    private AuditorSettings settings = AuditorSettings.DEFAULT;
    private AuditorStatus status = AuditorStatus.DISCONNECTED;
    @Nullable
    private UUID owner;
    private long total;
    private double rate;
    private int rotation;
    private long limitTick = -1;
    private int insertedThisTick;
    private long refusedTick = -1;
    private boolean routing;
    private boolean dirty;
    private boolean connectionChanged = true;
    private int traySize;
    private ItemStack topItem = ItemStack.EMPTY;
    @Nullable
    private CertificationRun run;
    @Nullable
    private Item runItem;
    @Nullable
    private UUID runOwner;
    private String runOwnerName = "";
    private boolean runExcellent;
    @Nullable
    private SyncKey synced;

    private static final int RUN_SYNC_SECONDS = 10;
    private static final int TICKS_PER_SECOND = 20;
    private long runReceivedAt;

    private record SyncKey(double rate, long total, AuditorStatus status, int runStep, int traySize) {
    }

    public AuditorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.AUDITOR_ENTITY.get(), pos, state);
        for (Direction side : Direction.values()) {
            inputs.put(side, new AuditorInput(this, side));
        }
    }

    public Direction front() {
        return getBlockState().getValue(AuditorBlock.FACING);
    }

    private boolean isInput(Direction side) {
        return settings.io().role(side, front(), settings.transfer()) == IoConfig.Role.INPUT;
    }

    @Nullable
    public IItemHandler itemHandlerFor(@Nullable Direction side) {
        if (side == null) {
            return null;
        }
        return switch (settings.io().role(side, front(), settings.transfer())) {
            case INPUT -> inputs.get(side);
            case OUTPUT -> ClosedFace.INSTANCE;
            case OFF -> null;
        };
    }

    int accept(Direction side, ItemStack stack, boolean simulate) {
        if (!(level instanceof ServerLevel server) || routing || !isInput(side)) {
            return 0;
        }
        int allowed = allowance(server, stack.getCount());
        if (allowed == 0) {
            refusedTick = server.getGameTime();
            return 0;
        }
        routing = true;
        try {
            int accepted = route(stack, allowed, simulate);
            if (accepted == 0) {
                refusedTick = server.getGameTime();
            } else if (!simulate) {
                record(stack, accepted);
            }
            return accepted;
        } finally {
            routing = false;
        }
    }

    boolean refusedThisTick() {
        return level != null && level.getGameTime() == refusedTick;
    }

    private int route(ItemStack stack, int count, boolean simulate) {
        return switch (settings.transfer()) {
            case CONSUME -> count;
            case PASS -> {
                int accepted = Distribution.split(count, outlets(stack), rotation, simulate);
                if (!simulate) {
                    rotation += count;
                }
                yield accepted;
            }
        };
    }

    private List<Distribution.Outlet> outlets(ItemStack stack) {
        List<Direction> sides = settings.io().sides(IoConfig.Role.OUTPUT, front(), settings.transfer());
        List<Distribution.Outlet> outlets = new ArrayList<>();
        for (Direction side : sides) {
            IItemHandler target = output(side);
            if (target != null) {
                outlets.add((count, simulate) -> count - ItemHandlerHelper.insertItem(target, stack.copyWithCount(count), simulate).getCount());
            }
        }
        return outlets;
    }

    @Nullable
    private IItemHandler output(Direction side) {
        return level instanceof ServerLevel server ? outputs.computeIfAbsent(side, key -> BlockCapabilityCache.create(
                Capabilities.ItemHandler.BLOCK, server, worldPosition.relative(key), key.getOpposite(),
                () -> !isRemoved(), () -> connectionChanged = true)).getCapability() : null;
    }

    private int allowance(ServerLevel server, int requested) {
        if (settings.limit() == 0) {
            return requested;
        }
        if (server.getGameTime() != limitTick) {
            limitTick = server.getGameTime();
            insertedThisTick = 0;
        }
        return Math.max(0, Math.min(requested, settings.limit() - insertedThisTick));
    }

    private void record(ItemStack stack, int count) {
        insertedThisTick += count;
        total += count;
        window.record(count);
        tally.add(stack.getItem(), count);
        if (run != null && stack.is(runItem)) {
            run.record(count);
        }
        dirty = true;
    }

    void serverTick() {
        if (dirty) {
            dirty = false;
            setChanged();
        }
        if (connectionChanged) {
            connectionChanged = false;
            refreshStatus();
            syncIfChanged();
        }
        if (run != null) {
            advanceRun();
        }
        if (window.tick(settings.interval(), settings.tolerance(), settings.measure())) {
            rate = window.ratePerMinute();
            history.add(rate);
            refreshStatus();
            syncIfChanged();
        }
    }

    private void advanceRun() {
        CertificationRun.State state = run.tick();
        if (state == CertificationRun.State.PASSED) {
            issueSeal();
            finishRun("passed");
        } else if (state == CertificationRun.State.FAILED) {
            finishRun("failed");
        } else if (run.ticks() % 20 == 0) {
            syncIfChanged();
        }
    }

    private void issueSeal() {
        SealData data = new SealData(ResourceLocation.parse(run.item()), run.tier(), runOwner, runOwnerName,
                run.averagePerMinute(), System.currentTimeMillis(), runExcellent);
        deposit(SealItem.create(data));
        if (level instanceof ServerLevel server) {
            Standings.Outcome outcome = SealCeremony.celebrate(server.getServer(), data);
            deliverMilestones(server, data.owner(), outcome.seals());
        }
    }

    private void deliverMilestones(ServerLevel server, UUID sealOwner, int seals) {
        Leaderboard board = Leaderboard.get(server.getServer());
        for (Milestone milestone : MilestonePrizes.current().due(seals, board.standings().lastMilestone(sealOwner))) {
            ItemStack prize = MilestonePrizes.prize(milestone);
            deposit(prize);
            notifyOwner(Component.translatable("message.monitoringcenter_auditor.milestone", milestone.seals(), prize.getHoverName()));
            board.reachMilestone(sealOwner, milestone.seals());
            Trophies.celebrate(server.getServer(), sealOwner, true);
        }
    }

    private void finishRun(String outcome) {
        CertificationRun finished = run;
        run = null;
        runItem = null;
        notifyOwner(Component.translatable("message.monitoringcenter_auditor.certification." + outcome,
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(finished.item())).getDescription(),
                SealItem.tierName(SealTier.byIndex(finished.tier())), finished.failedMinute()));
        sync();
    }

    private void notifyOwner(Component message) {
        if (level instanceof ServerLevel server && runOwner != null) {
            Notices.send(server.getServer(), runOwner, message);
        }
    }

    public void deposit(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        tray.add(stack);
        traySize = tray.size();
        sync();
    }

    public boolean startCertification(UUID newOwner, String ownerName, ItemStack item, SealTier tier, Perks perks) {
        if (settings.transfer() != TransferMode.CONSUME || item.isEmpty() || run != null) {
            return false;
        }
        double goal = tier.goal(AuditGoals.goalFor(item).base());
        run = new CertificationRun(BuiltInRegistries.ITEM.getKey(item.getItem()).toString(), tier.ordinal(), goal, perks.minutes(),
                perks.forgivable());
        runExcellent = perks.excellent();
        runItem = item.getItem();
        runOwner = newOwner;
        runOwnerName = ownerName;
        sync();
        return true;
    }

    public void takeTray(ServerPlayer player) {
        tray.forEach(player.getInventory()::placeItemBackInInventory);
        tray.clear();
        traySize = 0;
        sync();
    }

    public void dropTray() {
        if (level != null) {
            tray.forEach(stack -> Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack));
        }
        tray.clear();
    }

    private void refreshStatus() {
        status = !connected() ? AuditorStatus.DISCONNECTED : rate > 0 ? AuditorStatus.TRANSFERRING : AuditorStatus.IDLE;
    }

    private boolean connected() {
        Direction front = front();
        if (settings.io().sides(IoConfig.Role.INPUT, front, settings.transfer()).isEmpty()) {
            return false;
        }
        return settings.transfer() == TransferMode.CONSUME || settings.io().sides(IoConfig.Role.OUTPUT, front, settings.transfer())
                .stream().anyMatch(side -> output(side) != null);
    }

    public void applySettings(AuditorSettings next) {
        if (next.equals(settings)) {
            return;
        }
        boolean routingChanged = !next.io().equals(settings.io()) || next.transfer() != settings.transfer();
        settings = next;
        if (next.transfer() != TransferMode.CONSUME && run != null) {
            finishRun("cancelled");
        }
        if (routingChanged && level != null) {
            level.invalidateCapabilities(worldPosition);
            getBlockState().updateNeighbourShapes(level, worldPosition, Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        refreshStatus();
        sync();
    }

    public void resetTotal() {
        total = 0;
        rate = 0;
        tally.clear();
        history.clear();
        window.reset();
        refreshStatus();
        sync();
    }

    public void claim(UUID newOwner, int interval) {
        owner = newOwner;
        settings = settings.withNumbers(interval, settings.tolerance(), settings.limit());
        setChanged();
    }

    public boolean canUse(ServerPlayer player) {
        return owner == null || Owners.belongs(player, owner);
    }

    private void syncIfChanged() {
        if (!syncKey().equals(synced)) {
            sync();
        }
    }

    private SyncKey syncKey() {
        int runStep = run == null ? -1 : run.remainingSeconds() / RUN_SYNC_SECONDS;
        return new SyncKey(rate, total, status, runStep, traySize);
    }

    private void sync() {
        setChanged();
        synced = syncKey();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public AuditorSettings settings() {
        return settings;
    }

    public AuditorStatus status() {
        return status;
    }

    public double ratePerMinute() {
        return rate;
    }

    public long total() {
        return total;
    }

    public ItemTally tally() {
        return tally;
    }

    public ItemStack topItem() {
        return topItem;
    }

    public double[] history() {
        return history.values();
    }

    public int traySize() {
        return traySize;
    }

    @Nullable
    public CertificationRun run() {
        return run;
    }

    public int runSeconds() {
        if (run == null) {
            return -1;
        }
        long elapsed = level != null && level.isClientSide ? (level.getGameTime() - runReceivedAt) / TICKS_PER_SECOND : 0;
        return (int) Math.max(0, run.remainingSeconds() - elapsed);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public Item auditedItem() {
        return runItem != null ? runItem : tally.top();
    }

    public double nextSealGoal() {
        if (run != null) {
            return run.goal();
        }
        Item item = tally.top();
        if (!(level instanceof ServerLevel server) || owner == null || item == Items.AIR) {
            return 0;
        }
        int next = Leaderboard.get(server.getServer()).standings().bestTier(owner, BuiltInRegistries.ITEM.getKey(item).toString()) + 1;
        return next >= SealTier.values().length ? 0 : SealTier.byIndex(next).goal(AuditGoals.goalFor(new ItemStack(item)).base());
    }

    private void saveShared(CompoundTag tag) {
        tag.put("settings", settings.save());
        tag.putLong("total", total);
        tag.putDouble("rate", rate);
        tag.putInt("status", status.ordinal());
        tag.put("tally", tally.save());
        tag.put("history", new LongArrayTag(Arrays.stream(history.values()).mapToLong(Double::doubleToRawLongBits).toArray()));
        tag.putInt("tray_size", traySize);
        if (run != null) {
            tag.put("run", saveRun(run));
        }
    }

    private static CompoundTag saveRun(CertificationRun run) {
        CompoundTag tag = new CompoundTag();
        tag.putString("item", run.item());
        tag.putInt("tier", run.tier());
        tag.putDouble("goal", run.goal());
        tag.putInt("minutes", run.minutes());
        tag.putInt("forgivable", run.forgivable());
        tag.putInt("forgiven", run.forgiven());
        tag.putInt("minute", run.minute());
        tag.putInt("ticks", run.ticks());
        tag.putLong("this_minute", run.thisMinute());
        tag.putLong("total", run.total());
        return tag;
    }

    private static CertificationRun loadRun(CompoundTag tag) {
        return CertificationRun.restore(tag.getString("item"), tag.getInt("tier"), tag.getDouble("goal"), tag.getInt("minutes"),
                tag.getInt("forgivable"), tag.getInt("forgiven"), tag.getInt("minute"), tag.getInt("ticks"), tag.getLong("this_minute"), tag.getLong("total"), CertificationRun.State.RUNNING);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveShared(tag);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        if (runOwner != null) {
            tag.putUUID("run_owner", runOwner);
            tag.putString("run_owner_name", runOwnerName);
            tag.putBoolean("run_excellent", runExcellent);
        }
        ListTag stacks = new ListTag();
        tray.forEach(stack -> stacks.add(stack.save(registries)));
        tag.put("tray", stacks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        settings = AuditorSettings.load(tag.getCompound("settings"));
        total = tag.getLong("total");
        rate = tag.getDouble("rate");
        window.restore(rate);
        status = AuditorSettings.pick(AuditorStatus.values(), tag.getInt("status"));
        tally.load(tag.getCompound("tally"));
        topItem = new ItemStack(tally.top());
        history.replace(Arrays.stream(tag.getLongArray("history")).mapToDouble(Double::longBitsToDouble).toArray());
        traySize = tag.getInt("tray_size");
        run = tag.contains("run") ? loadRun(tag.getCompound("run")) : null;
        runItem = run == null ? null : BuiltInRegistries.ITEM.get(ResourceLocation.parse(run.item()));
        runReceivedAt = level != null ? level.getGameTime() : 0;
        if (tag.hasUUID("owner")) {
            owner = tag.getUUID("owner");
        }
        if (tag.hasUUID("run_owner")) {
            runOwner = tag.getUUID("run_owner");
            runOwnerName = tag.getString("run_owner_name");
            runExcellent = tag.getBoolean("run_excellent");
        }
        if (tag.contains("tray")) {
            tray.clear();
            for (Tag stack : tag.getList("tray", Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, stack).ifPresent(tray::add);
            }
            traySize = tray.size();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveShared(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
