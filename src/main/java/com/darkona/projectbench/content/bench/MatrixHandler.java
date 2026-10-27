package com.darkona.projectbench.content.bench;

import dev.darkona.jdlib.api.inventory.GridFill;
import dev.darkona.jdlib.api.pattern.Blueprint;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

final class MatrixHandler implements IItemHandler {
    private final ProjectBenchBlockEntity bench;
    private final SimpleContainer matrix;
    private final InvWrapper plain;

    MatrixHandler(ProjectBenchBlockEntity bench) {
        this.bench = bench;
        this.matrix = bench.matrix();
        this.plain = new InvWrapper(matrix);
    }

    @Override
    public int getSlots() {
        return plain.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return plain.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= matrix.getContainerSize()) {
            return stack;
        }
        int n = Math.min(stack.getCount(), BenchStacks.matrixRoom(matrix, bench.blueprint(), slot, stack));
        if (n <= 0) {
            return stack;
        }
        if (!simulate) {
            ItemStack held = matrix.getItem(slot);
            if (held.isEmpty()) {
                matrix.setItem(slot, stack.copyWithCount(n));
            } else {
                held.grow(n);
                matrix.setChanged();
            }
        }
        return n == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return plain.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return plain.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot < 0 || slot >= matrix.getContainerSize() || stack.isEmpty()) {
            return false;
        }
        ItemStack held = matrix.getItem(slot);
        if (!held.isEmpty()) {
            return ItemStack.isSameItemSameComponents(held, stack);
        }
        Blueprint blueprint = bench.blueprint();
        return blueprint == null || GridFill.accepts(blueprint.ghost(slot), stack);
    }
}
