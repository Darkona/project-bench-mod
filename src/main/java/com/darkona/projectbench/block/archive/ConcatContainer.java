package com.darkona.projectbench.block.archive;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ConcatContainer implements Container {
    private static final int PART = BlueprintArchiveBlockEntity.SLOTS;

    private final Container[] parts;

    public ConcatContainer(List<? extends Container> parts) {
        this.parts = parts.toArray(new Container[0]);
    }

    @Override
    public int getContainerSize() {
        return parts.length * PART;
    }

    @Override
    public boolean isEmpty() {
        for (Container part : parts) {
            if (!part.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return parts[slot / PART].getItem(slot % PART);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return parts[slot / PART].removeItem(slot % PART, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return parts[slot / PART].removeItemNoUpdate(slot % PART);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        parts[slot / PART].setItem(slot % PART, stack);
    }

    @Override
    public int getMaxStackSize() {
        return parts.length == 0 ? 0 : parts[0].getMaxStackSize();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        for (Container part : parts) {
            if (!part.stillValid(player)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return parts[slot / PART].canPlaceItem(slot % PART, stack);
    }

    @Override
    public void clearContent() {
        for (Container part : parts) {
            part.clearContent();
        }
    }

    public Container partOf(int slot) {
        return parts[slot / PART];
    }
}
