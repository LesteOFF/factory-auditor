package dev.nostalgiacraft.auditor.block;

import dev.nostalgiacraft.auditor.award.AwardedFactory;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.auditor.seal.SealData;
import dev.nostalgiacraft.auditor.seal.SealItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PlaqueBlockEntity extends BlockEntity {
    private ItemStack seal = ItemStack.EMPTY;

    public PlaqueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PLAQUE_ENTITY.get(), pos, state);
    }

    public Direction wall() {
        return getBlockState().getValue(PlaqueBlock.FACING);
    }

    public ItemStack seal() {
        return seal;
    }

    @Nullable
    public SealData sealData() {
        return SealItem.data(seal);
    }

    public void hang(ItemStack stack) {
        seal = stack.copyWithCount(1);
        changed();
    }

    public ItemStack takeDown() {
        ItemStack taken = seal;
        seal = ItemStack.EMPTY;
        changed();
        return taken;
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel server) {
            AwardedFactory.changed(server);
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            AwardedFactory.add(server, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            AwardedFactory.remove(server, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel server) {
            AwardedFactory.remove(server, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!seal.isEmpty()) {
            tag.put("seal", seal.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seal = tag.contains("seal") ? ItemStack.parse(registries, tag.getCompound("seal")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!seal.isEmpty()) {
            tag.put("seal", seal.save(registries));
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
