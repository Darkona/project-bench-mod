package com.darkona.projectbench.block.bench;

import dev.darkona.jdlib.api.pattern.Blueprint;
import dev.darkona.jdlib.api.inventory.GridFill;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class BenchStacks {
    private BenchStacks() {
    }

    public static int matrixRoom(Container matrix, @Nullable Blueprint blueprint, int slot, ItemStack stack) {
        return matrixRoom(matrix, blueprint, slot, stack, true);
    }

    public static int matrixRoom(Container matrix, @Nullable Blueprint blueprint, int slot, ItemStack stack, boolean intoEmpty) {
        if (stack.isEmpty()) {
            return 0;
        }
        int n = matrix.getContainerSize();
        int limit = Math.min(matrix.getMaxStackSize(stack), stack.getMaxStackSize());
        if (blueprint != null) {
            int least = -1;
            int leastCount = Integer.MAX_VALUE;
            int next = Integer.MAX_VALUE;
            for (int i = 0; i < n && i < Blueprint.SIZE; ++i) {
                ItemStack held = matrix.getItem(i);
                if (!GridFill.accepts(blueprint.ghost(i), stack) || !(held.isEmpty() || ItemStack.isSameItemSameComponents(held, stack))
                        || held.getCount() >= limit) {
                    continue;
                }
                int c = held.getCount();
                if (c < leastCount) {
                    next = leastCount;
                    leastCount = c;
                    least = i;
                } else if (c < next) {
                    next = c;
                }
            }
            if (least >= 0) {
                return slot != least ? 0 : (next == Integer.MAX_VALUE ? limit : Math.min(limit, next + 1)) - leastCount;
            }
        }
        boolean same = false;
        int firstEmpty = -1;
        for (int i = 0; i < n; ++i) {
            ItemStack held = matrix.getItem(i);
            if (held.isEmpty()) {
                if (firstEmpty < 0) {
                    firstEmpty = i;
                }
            } else if (held.getCount() < limit && ItemStack.isSameItemSameComponents(held, stack)) {
                same = true;
            }
        }
        ItemStack here = matrix.getItem(slot);
        if (same) {
            return !here.isEmpty() && ItemStack.isSameItemSameComponents(here, stack) ? Math.max(0, limit - here.getCount()) : 0;
        }
        return intoEmpty && blueprint == null && slot == firstEmpty ? limit : 0;
    }

    public static int topUp(Container matrix, ItemStack stack) {
        int moved = 0;
        for (int i = 0, n = matrix.getContainerSize(); i < n && !stack.isEmpty(); ++i) {
            int room = Math.min(stack.getCount(), matrixRoom(matrix, null, i, stack, false));
            if (room > 0) {
                matrix.getItem(i).grow(room);
                stack.shrink(room);
                moved += room;
            }
        }
        if (moved > 0) {
            matrix.setChanged();
        }
        return moved;
    }
}
