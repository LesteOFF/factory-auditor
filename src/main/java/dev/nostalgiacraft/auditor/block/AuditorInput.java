package dev.nostalgiacraft.auditor.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

final class AuditorInput implements IItemHandler {
    private static final int SLOT_LIMIT = 64;
    static final int SLOTS = 4096;
    private final AuditorBlockEntity auditor;
    private final Direction side;

    AuditorInput(AuditorBlockEntity auditor, Direction side) {
        this.auditor = auditor;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot > 0 && auditor.refusedThisTick()) {
            return stack;
        }
        int accepted = auditor.accept(side, stack, simulate);
        return accepted == 0 ? stack : stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return SLOT_LIMIT;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
