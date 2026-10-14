package dev.nostalgiacraft.auditor.block;

import com.mojang.serialization.MapCodec;
import dev.nostalgiacraft.auditor.award.TrophyKind;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TrophyBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TrophyBlock> CODEC = simpleCodec(TrophyBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public TrophyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<TrophyBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyBlockEntity(pos, state);
    }

    public TrophyKind kind() {
        return this == ModBlocks.GOLD_TROPHY.get() ? TrophyKind.GOLD : TrophyKind.BRONZE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof TrophyBlockEntity trophy) {
            trophy.claim(Owners.ownerOf(player));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof TrophyBlockEntity trophy) {
            trophy.inspire(server, kind());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
