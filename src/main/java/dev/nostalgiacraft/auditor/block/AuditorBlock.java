package dev.nostalgiacraft.auditor.block;

import dev.nostalgiacraft.auditor.config.AuditorConfig;
import dev.nostalgiacraft.auditor.diploma.Diploma;
import dev.nostalgiacraft.auditor.diploma.Diplomas;
import dev.nostalgiacraft.auditor.diploma.Perks;
import dev.nostalgiacraft.auditor.goal.AuditGoals;
import dev.nostalgiacraft.auditor.network.AuditorPayloads;
import dev.nostalgiacraft.auditor.network.BoardSnapshot;
import dev.nostalgiacraft.monitor.data.Owners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class AuditorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AuditorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuditorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof AuditorBlockEntity auditor) {
                auditor.serverTick();
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof AuditorBlockEntity auditor) {
            auditor.claim(Owners.ownerOf(player), AuditorConfig.DEFAULT_INTERVAL.get());
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this) && oldState.getValue(FACING) != state.getValue(FACING)) {
            level.invalidateCapabilities(pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AuditorBlockEntity auditor) {
            auditor.dropTray();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof AuditorBlockEntity auditor) {
            if (!auditor.canUse(server)) {
                server.displayClientMessage(Component.translatable("message.monitoringcenter_auditor.not_owner"), true);
            } else if (server.isShiftKeyDown() && auditor.traySize() > 0) {
                auditor.takeTray(server);
            } else {
                Set<Diploma> diplomas = Diplomas.held(server);
                PacketDistributor.sendToPlayer(server, new AuditorPayloads.OpenAuditor(pos,
                        AuditGoals.goalFor(server.getMainHandItem()).base(), BoardSnapshot.of(server.server, auditor, Owners.ownerOf(server)),
                        AuditorPayloads.OpenAuditor.mask(diplomas), Perks.of(diplomas, AuditorConfig.CERTIFICATION_MINUTES.get()).minutes()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
