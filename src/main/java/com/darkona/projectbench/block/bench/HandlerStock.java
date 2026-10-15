package com.darkona.projectbench.block.bench;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.function.Predicate;
import java.util.function.Supplier;

final class HandlerStock implements Stock {
    private final Supplier<IItemHandler> source;

    HandlerStock(Supplier<IItemHandler> source) {
        this.source = source;
    }

    @Override
    public int count(ItemStack like) {
        IItemHandler handler = source.get();
        int n = 0;
        if (handler != null) {
            for (int i = 0, size = handler.getSlots(); i < size; ++i) {
                ItemStack there = handler.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(there, like)) {
                    n = BenchStacks.sum(n, there.getCount());
                }
            }
        }
        return n;
    }

    @Override
    public int take(ItemStack like, int amount) {
        IItemHandler handler = source.get();
        int taken = 0;
        if (handler != null) {
            for (int i = 0, size = handler.getSlots(); i < size && taken < amount; ++i) {
                int got = 1;
                while (got > 0 && taken < amount && ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), like)) {
                    got = BenchStacks.takeAtMostAStack(handler, i, amount - taken).getCount();
                    taken += got;
                }
            }
        }
        return taken;
    }

    @Override
    public ItemStack find(Predicate<ItemStack> test) {
        IItemHandler handler = source.get();
        if (handler != null) {
            for (int i = 0, size = handler.getSlots(); i < size; ++i) {
                ItemStack there = handler.getStackInSlot(i);
                if (!there.isEmpty() && test.test(there)) {
                    return there.copyWithCount(1);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        IItemHandler handler = source.get();
        return stack.isEmpty() || handler == null ? stack.copy() : ItemHandlerHelper.insertItem(handler, stack.copy(), false);
    }
}
