package com.darkona.projectbench.core;

import dev.darkona.jdlib.api.menu.JDMenuSupport;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class BenchMenu extends AbstractContainerMenu {
    public static final double REACH = 8;

    public static final int PLAYER_SLOTS = 36;

    protected BenchMenu(@Nullable MenuType<?> type, int id) {
        super(type, id);
    }

    protected final void addPlayerInventory(Inventory inventory, int x, int y) {
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                addSlot(new Slot(inventory, c + r * 9 + 9, x + c * 18, y + r * 18));
            }
        }
        for (int c = 0; c < 9; ++c) {
            addSlot(new Slot(inventory, c, x + c * 18, y + 58));
        }
    }

    public static boolean near(Player player, BlockEntity block) {
        return JDMenuSupport.near(player, block, REACH);
    }

    protected final ItemStack moveBetween(int index, int ownEnd) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < ownEnd ? moveItemStackTo(stack, ownEnd, slots.size(), true) : moveItemStackTo(stack, 0, ownEnd, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
