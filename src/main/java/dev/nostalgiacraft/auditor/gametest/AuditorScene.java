package dev.nostalgiacraft.auditor.gametest;

import dev.nostalgiacraft.auditor.block.AuditorBlock;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.AuditorSettings;
import dev.nostalgiacraft.auditor.block.IoConfig;
import dev.nostalgiacraft.auditor.block.TransferMode;
import dev.nostalgiacraft.auditor.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

final class AuditorScene {
    static final Direction FRONT = Direction.NORTH;
    static final Direction INPUT = Direction.UP;

    private AuditorScene() {
    }

    static AuditorBlockEntity place(GameTestHelper helper, BlockPos pos, TransferMode mode, Direction... outputs) {
        helper.setBlock(pos, ModBlocks.AUDITOR.get().defaultBlockState().setValue(AuditorBlock.FACING, FRONT));
        AuditorBlockEntity auditor = helper.getBlockEntity(pos);
        IoConfig io = IoConfig.NONE.cycle(INPUT, FRONT, mode, false);
        for (Direction output : outputs) {
            io = io.cycle(output, FRONT, mode, true);
        }
        auditor.applySettings(AuditorSettings.DEFAULT.withTransfer(mode).withIo(io));
        return auditor;
    }

    static int insert(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        IItemHandler input = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), INPUT);
        if (input == null) {
            throw new GameTestAssertException("auditor exposes no input on " + INPUT);
        }
        return stack.getCount() - input.insertItem(0, stack, false).getCount();
    }

    static Container chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return helper.getBlockEntity(pos);
    }

    static int count(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            total += container.getItem(slot).getCount();
        }
        return total;
    }

    static void assertEquals(String what, double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-6) {
            throw new GameTestAssertException(what + ": expected " + expected + " but was " + actual);
        }
    }
}
