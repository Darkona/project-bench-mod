package com.darkona.projectbench.block.bench;

import dev.darkona.jdlib.api.pattern.Blueprint;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class MatrixLayout {
    private final NonNullList<ItemStack> items = NonNullList.withSize(Blueprint.SIZE, ItemStack.EMPTY);

    public boolean set(int i, ItemStack now) {
        if (ItemStack.isSameItemSameComponents(now, items.get(i))) {
            return false;
        }
        items.set(i, now.copyWithCount(1));
        return true;
    }

    public boolean update(List<ItemStack> matrix) {
        boolean changed = false;
        for (int i = 0; i < Blueprint.SIZE; ++i) {
            changed |= set(i, matrix.get(i));
        }
        return changed;
    }

    public List<ItemStack> items() {
        return items;
    }

    public void clear() {
        items.clear();
    }
}
