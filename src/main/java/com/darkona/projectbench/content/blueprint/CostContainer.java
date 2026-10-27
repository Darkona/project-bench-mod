package com.darkona.projectbench.content.blueprint;

import com.darkona.projectbench.foundation.ContainerSlot;
import com.darkona.projectbench.config.BenchConfig;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CostContainer extends SimpleContainer {
    public CostContainer() {
        super(1);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        Item cost = BenchConfig.costItem();
        return cost != Items.AIR && stack.is(cost);
    }

    public static int missing(ItemStack held) {
        BenchConfig.Settings s = BenchConfig.get();
        Item cost = BenchConfig.costItem(s);
        if (cost == Items.AIR) {
            return 0;
        }
        return held.is(cost) ? Math.max(0, s.recordCostCount() - held.getCount()) : s.recordCostCount();
    }

    public void charge() {
        Item cost = BenchConfig.costItem();
        if (cost != Items.AIR) {
            removeItem(0, BenchConfig.get().recordCostCount());
        }
    }

    public static class CostSlot extends ContainerSlot {
        public CostSlot(Container container, int x, int y, boolean fake) {
            super(container, 0, x, y, fake);
        }

        @Override
        public boolean isActive() {
            return BenchConfig.costItem() != Items.AIR;
        }
    }
}
