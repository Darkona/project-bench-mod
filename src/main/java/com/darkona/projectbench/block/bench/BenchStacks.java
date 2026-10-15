package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.block.archive.ConcatContainer;
import com.darkona.projectbench.blueprint.Blueprint;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;

public final class BenchStacks {
    private BenchStacks() {
    }

    public static ItemStack takeAtMostAStack(IItemHandler inventory, int slot, int amount) {
        ItemStack present = inventory.getStackInSlot(slot);
        int cap = Math.min(amount, present.isEmpty() ? Item.ABSOLUTE_MAX_STACK_SIZE : present.getMaxStackSize());
        if (cap <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack would = inventory.extractItem(slot, cap, true);
        if (would.isEmpty() || would.getCount() > cap) {
            return ItemStack.EMPTY;
        }
        ItemStack got = inventory.extractItem(slot, cap, false);
        if (got.getCount() > cap) {
            ItemHandlerHelper.insertItemStacked(inventory, got.split(got.getCount() - cap), false);
        }
        return got;
    }

    public static int sum(int a, int b) {
        return (int) Math.min(Integer.MAX_VALUE, (long) a + b);
    }

    public static void consume(Container matrix, @Nullable Stock stock, int slot, @Nullable Predicate<ItemStack> substitute) {
        ItemStack held = matrix.getItem(slot);
        if (held.isEmpty()) {
            return;
        }
        if (held.getCount() == 1 && stock != null) {
            if (stock.take(held, 1) == 1) {
                return;
            }
            ItemStack other = substitute == null ? ItemStack.EMPTY : stock.find(s -> !ItemStack.isSameItemSameComponents(s, held) && substitute.test(s));
            if (!other.isEmpty() && stock.take(other, 1) == 1) {
                matrix.setItem(slot, other.copyWithCount(1));
                return;
            }
        }
        matrix.removeItem(slot, 1);
    }

    public static boolean clear(Container matrix, Stock into) {
        boolean moved = false;
        for (int i = 0, n = matrix.getContainerSize(); i < n; ++i) {
            ItemStack held = matrix.getItem(i);
            if (held.isEmpty()) {
                continue;
            }
            ItemStack rest = into.insert(held);
            if (rest.getCount() != held.getCount()) {
                matrix.setItem(i, rest);
                moved = true;
            }
        }
        return moved;
    }

    public static boolean fill(Container matrix, Blueprint blueprint, Stock stock) {
        int n = Blueprint.SIZE;
        boolean changed = false;

        int blocked = 0;
        ItemStack[] template = new ItemStack[n];
        for (int i = 0; i < n; ++i) {
            ItemStack ghost = blueprint.ghost(i);
            ItemStack held = matrix.getItem(i);
            if (held.isEmpty()) {
                continue;
            }
            boolean keep = accepts(ghost, held);
            if (keep) {
                for (int j = 0; j < i && keep; ++j) {
                    if (template[j] != null && template[j].is(held.getItem())) {
                        keep = ItemStack.isSameItemSameComponents(template[j], held);
                    }
                }
            }
            if (keep) {
                template[i] = held.copyWithCount(1);
                continue;
            }
            ItemStack rest = stock.insert(held);
            if (rest.getCount() != held.getCount()) {
                matrix.setItem(i, rest);
                changed = true;
            }
            if (!rest.isEmpty()) {
                blocked |= 1 << i;
            }
        }

        int[] group = new int[n];
        Arrays.fill(group, -1);
        int groups = 0;
        int[] members = new int[n];
        for (int i = 0; i < n; ++i) {
            ItemStack ghost = blueprint.ghost(i);
            if (ghost.isEmpty() || (blocked & 1 << i) != 0 || group[i] >= 0) {
                continue;
            }
            for (int j = i; j < n; ++j) {
                if ((blocked & 1 << j) == 0 && group[j] < 0 && blueprint.ghost(j).is(ghost.getItem())) {
                    group[j] = groups;
                    members[groups] |= 1 << j;
                }
            }
            ++groups;
        }

        for (int g = 0; g < groups; ++g) {
            int first = Integer.numberOfTrailingZeros(members[g]);
            ItemStack ghost = blueprint.ghost(first);
            ItemStack kind = ItemStack.EMPTY;
            for (int j = first; j < n && kind.isEmpty(); ++j) {
                if ((members[g] & 1 << j) != 0 && template[j] != null) {
                    kind = template[j];
                }
            }
            if (kind.isEmpty()) {
                kind = stock.find(s -> accepts(ghost, s));
            }
            if (kind.isEmpty()) {
                continue;
            }
            int positions = Integer.bitCount(members[g]);
            int held = 0;
            for (int j = first; j < n; ++j) {
                if ((members[g] & 1 << j) != 0 && ItemStack.isSameItemSameComponents(matrix.getItem(j), kind)) {
                    held += matrix.getItem(j).getCount();
                }
            }
            int limit = Math.min(kind.getMaxStackSize(), matrix.getMaxStackSize());
            int total = Math.min(sum(held, stock.count(kind)), positions * limit);
            int each = total / positions;
            int extra = total % positions;
            for (int j = 0; j < n; ++j) {
                if (group[j] != g) {
                    continue;
                }
                int target = each + (extra > 0 ? 1 : 0);
                if (extra > 0) {
                    --extra;
                }
                ItemStack at = matrix.getItem(j);
                int have = ItemStack.isSameItemSameComponents(at, kind) ? at.getCount() : 0;
                if (have < target) {
                    int got = stock.take(kind, target - have);
                    if (got > 0) {
                        matrix.setItem(j, kind.copyWithCount(have + got));
                        changed = true;
                    }
                } else if (have > target) {
                    ItemStack rest = stock.insert(kind.copyWithCount(have - target));
                    int back = have - target - rest.getCount();
                    if (back > 0) {
                        at.shrink(back);
                        matrix.setItem(j, at.isEmpty() ? ItemStack.EMPTY : at);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    public static int room(Container storage, ItemStack like) {
        if (like.isEmpty()) {
            return 0;
        }
        int room = 0;
        int full = Math.min(storage.getMaxStackSize(like), like.getMaxStackSize());
        for (int i = 0, n = storage.getContainerSize(); i < n; ++i) {
            ItemStack there = storage.getItem(i);
            if (there.isEmpty()) {
                room += storage.canPlaceItem(i, like) ? full : 0;
            } else if (ItemStack.isSameItemSameComponents(there, like) && storage.canPlaceItem(i, like)) {
                room += Math.max(0, Math.min(storage.getMaxStackSize(there), there.getMaxStackSize()) - there.getCount());
            }
        }
        return room;
    }

    public static ItemStack insert(Container storage, ItemStack stack) {
        return insert(storage, storage.getContainerSize(), stack);
    }

    public static ItemStack insert(Container storage, int slots, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack rest = stack.copy();
        if (storage instanceof Storage locked) {
            locked.insertLocked(rest);
        }
        boolean grew = false;
        ConcatContainer shelf = storage instanceof ConcatContainer c ? c : null;
        Container marked = null;
        for (int i = 0; i < slots && !rest.isEmpty(); ++i) {
            ItemStack there = storage.getItem(i);
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, rest) && storage.canPlaceItem(i, rest)) {
                int room = Math.min(storage.getMaxStackSize(there), there.getMaxStackSize()) - there.getCount();
                if (room > 0) {
                    int moved = Math.min(room, rest.getCount());
                    there.grow(moved);
                    rest.shrink(moved);
                    grew = true;
                    if (shelf != null && shelf.partOf(i) != marked) {
                        marked = shelf.partOf(i);
                        marked.setChanged();
                    }
                }
            }
        }
        for (int i = 0; i < slots && !rest.isEmpty(); ++i) {
            if (storage.getItem(i).isEmpty() && storage.canPlaceItem(i, rest)) {
                int moved = Math.min(rest.getCount(), Math.min(storage.getMaxStackSize(rest), rest.getMaxStackSize()));
                storage.setItem(i, rest.split(moved));
            }
        }
        if (grew) {
            storage.setChanged();
        }
        return rest;
    }

    public static boolean balance(Container matrix) {
        int n = matrix.getContainerSize();
        int done = 0;
        boolean changed = false;
        for (int i = 0; i < n; ++i) {
            ItemStack first = matrix.getItem(i);
            if ((done & 1 << i) != 0 || first.isEmpty()) {
                continue;
            }
            int group = 0;
            int total = 0;
            for (int j = i; j < n; ++j) {
                ItemStack s = matrix.getItem(j);
                if ((done & 1 << j) == 0 && ItemStack.isSameItemSameComponents(s, first)) {
                    group |= 1 << j;
                    total += s.getCount();
                }
            }
            done |= group;
            int slots = Integer.bitCount(group);
            int each = total / slots;
            int extra = total % slots;
            for (int j = i; j < n; ++j) {
                if ((group & 1 << j) == 0) {
                    continue;
                }
                int count = each + (extra > 0 ? 1 : 0);
                if (extra > 0) {
                    --extra;
                }
                ItemStack s = matrix.getItem(j);
                if (s.getCount() != count) {
                    s.setCount(count);
                    changed = true;
                }
            }
        }
        if (changed) {
            matrix.setChanged();
        }
        return changed;
    }

    public static boolean accepts(ItemStack ghost, ItemStack stack) {
        return !ghost.isEmpty() && !stack.isEmpty() && stack.is(ghost.getItem());
    }

    public static boolean anyGhostAccepts(Blueprint blueprint, ItemStack stack) {
        for (int i = 0; i < Blueprint.SIZE; ++i) {
            if (accepts(blueprint.ghost(i), stack)) {
                return true;
            }
        }
        return false;
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
                if (!accepts(blueprint.ghost(i), stack) || !(held.isEmpty() || ItemStack.isSameItemSameComponents(held, stack))
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

    public static int intoGhosts(Container matrix, Blueprint blueprint, ItemStack stack) {
        int n = Blueprint.SIZE;
        int limit = Math.min(matrix.getMaxStackSize(stack), stack.getMaxStackSize());
        int[] counts = new int[n];
        int targets = 0;
        for (int i = 0; i < n; ++i) {
            ItemStack held = matrix.getItem(i);
            if (accepts(blueprint.ghost(i), stack) && (held.isEmpty() || ItemStack.isSameItemSameComponents(held, stack))) {
                targets |= 1 << i;
                counts[i] = held.getCount();
            }
        }
        int moved = 0;
        while (moved < stack.getCount()) {
            int least = -1;
            for (int i = 0; i < n; ++i) {
                if ((targets & 1 << i) != 0 && counts[i] < limit && (least < 0 || counts[i] < counts[least])) {
                    least = i;
                }
            }
            if (least < 0) {
                break;
            }
            ++counts[least];
            ++moved;
        }
        if (moved == 0) {
            return 0;
        }
        for (int i = 0; i < n; ++i) {
            if ((targets & 1 << i) != 0 && counts[i] != matrix.getItem(i).getCount()) {
                matrix.setItem(i, stack.copyWithCount(counts[i]));
            }
        }
        stack.shrink(moved);
        return moved;
    }

    public static boolean missing(Container matrix, Blueprint blueprint, int i) {
        return !blueprint.ghost(i).isEmpty() && matrix.getItem(i).isEmpty();
    }
}
