package com.darkona.projectbench.core;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerSlot extends Slot {
    private final boolean fake;

    public ContainerSlot(Container container, int slot, int x, int y) {
        this(container, slot, x, y, false);
    }

    public ContainerSlot(Container container, int slot, int x, int y, boolean fake) {
        super(container, slot, x, y);
        this.fake = fake;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container.canPlaceItem(getContainerSlot(), stack);
    }

    @Override
    public boolean isFake() {
        return fake;
    }
}
