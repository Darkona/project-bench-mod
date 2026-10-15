package com.darkona.projectbench.gametest;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class GameTests {
    public static final String EMPTY = "empty";

    private GameTests() {
    }

    public static int count(Container container, Item item) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack s = container.getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    public static int count(IItemHandler handler, Item item) {
        int n = 0;
        for (int i = 0; i < handler.getSlots(); ++i) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }
}
