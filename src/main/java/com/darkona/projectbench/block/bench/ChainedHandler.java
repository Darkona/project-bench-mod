package com.darkona.projectbench.block.bench;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

final class ChainedHandler implements IItemHandler {
    private final IItemHandler first;
    private final IItemHandler second;
    private final int split;

    ChainedHandler(IItemHandler first, IItemHandler second) {
        this.first = first;
        this.second = second;
        this.split = first.getSlots();
    }

    @Override
    public int getSlots() {
        return split + second.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot < split ? first.getStackInSlot(slot) : second.getStackInSlot(slot - split);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return slot < split ? first.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot < split ? first.extractItem(slot, amount, simulate) : second.extractItem(slot - split, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot < split ? first.getSlotLimit(slot) : second.getSlotLimit(slot - split);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot < split && first.isItemValid(slot, stack);
    }
}
