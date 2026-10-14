package dev.nostalgiacraft.auditor.block;

import dev.nostalgiacraft.auditor.award.Trophies;
import dev.nostalgiacraft.auditor.award.TrophyKind;
import dev.nostalgiacraft.auditor.board.Leaderboard;
import dev.nostalgiacraft.auditor.board.Standings;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class TrophyBlockEntity extends BlockEntity {
    private static final long COOLDOWN_TICKS = 24000;
    private static final long TICKS_PER_MINUTE = 1200;
    @Nullable
    private UUID owner;
    private String recordItem = "";
    private double recordRate;
    private long readyAt;

    public TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TROPHY_ENTITY.get(), pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String recordItem() {
        return recordItem;
    }

    public double recordRate() {
        return recordRate;
    }

    public boolean inspire(ServerPlayer player, TrophyKind kind) {
        if (owner == null || !Owners.belongs(player, owner)) {
            player.displayClientMessage(Component.translatable("message.monitoringcenter_auditor.trophy.not_yours"), true);
            return false;
        }
        long now = player.level().getGameTime();
        if (now < readyAt) {
            player.displayClientMessage(Component.translatable("message.monitoringcenter_auditor.trophy.cooldown",
                    (readyAt - now) / TICKS_PER_MINUTE + 1), true);
            return false;
        }
        readyAt = now + COOLDOWN_TICKS;
        setChanged();
        Trophies.rally(player, kind);
        return true;
    }

    public void claim(UUID newOwner) {
        owner = newOwner;
        refreshRecord();
    }

    public void refreshRecord() {
        if (!(level instanceof ServerLevel server) || owner == null) {
            return;
        }
        Optional<Standings.Personal> best = Leaderboard.get(server.getServer()).standings().best(owner);
        recordItem = best.map(Standings.Personal::item).orElse("");
        recordRate = best.map(Standings.Personal::rate).orElse(0.0);
        setChanged();
        server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            Trophies.add(server, worldPosition);
            server.getServer().execute(this::refreshRecord);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            Trophies.remove(server, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel server) {
            Trophies.remove(server, worldPosition);
        }
    }

    private void saveRecord(CompoundTag tag) {
        tag.putString("record_item", recordItem);
        tag.putDouble("record_rate", recordRate);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveRecord(tag);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putLong("ready_at", readyAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        recordItem = tag.getString("record_item");
        recordRate = tag.getDouble("record_rate");
        if (tag.hasUUID("owner")) {
            owner = tag.getUUID("owner");
        }
        readyAt = tag.getLong("ready_at");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveRecord(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
