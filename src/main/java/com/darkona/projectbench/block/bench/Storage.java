package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.config.Config;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class Storage extends SimpleContainer {
    private final NonNullList<ItemStack> locks;

    public Storage(int size) {
        super(size);
        locks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStack lock(int slot) {
        return Config.get().slotLocks() ? locks.get(slot) : ItemStack.EMPTY;
    }

    public boolean lockedTo(int slot, ItemStack stack) {
        ItemStack lock = lock(slot);
        return !lock.isEmpty() && ItemStack.isSameItemSameComponents(lock, stack);
    }

    public boolean insertLocked(ItemStack stack) {
        boolean grew = false;
        boolean filled = false;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); ++pass) {
            for (int i = 0, n = getContainerSize(); i < n && !stack.isEmpty(); ++i) {
                ItemStack there = getItem(i);
                if (there.isEmpty() == (pass == 0) || !lockedTo(i, stack)) {
                    continue;
                }
                if (there.isEmpty()) {
                    setItem(i, stack.split(Math.min(stack.getCount(), getMaxStackSize(stack))));
                    filled = true;
                } else if (ItemStack.isSameItemSameComponents(there, stack)) {
                    int moved = Math.min(stack.getCount(), getMaxStackSize(there) - there.getCount());
                    if (moved > 0) {
                        there.grow(moved);
                        stack.shrink(moved);
                        grew = true;
                    }
                }
            }
        }
        if (grew) {
            setChanged();
        }
        return grew || filled;
    }

    public void setLock(int slot, ItemStack like) {
        locks.set(slot, like.isEmpty() ? ItemStack.EMPTY : like.copyWithCount(1));
    }

    public boolean anyLock() {
        for (ItemStack lock : locks) {
            if (!lock.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void clearLocks() {
        locks.replaceAll(s -> ItemStack.EMPTY);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        ItemStack lock = lock(slot);
        return lock.isEmpty() || ItemStack.isSameItemSameComponents(lock, stack);
    }

    public NonNullList<ItemStack> locks() {
        return locks;
    }

    public ItemContainerContents locksAsContents() {
        return ItemContainerContents.fromItems(locks);
    }

    public void setLocks(ItemContainerContents contents) {
        clearLocks();
        contents.copyInto(locks);
    }

    public void writeLocks(RegistryFriendlyByteBuf buf) {
        for (ItemStack lock : locks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, lock);
        }
    }

    public void readLocks(RegistryFriendlyByteBuf buf) {
        for (int i = 0; i < locks.size(); ++i) {
            locks.set(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
    }
}
