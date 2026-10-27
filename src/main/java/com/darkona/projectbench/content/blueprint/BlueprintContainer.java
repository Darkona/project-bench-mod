package com.darkona.projectbench.content.blueprint;

import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.foundation.ContainerSlot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntSupplier;

public class BlueprintContainer extends SimpleContainer {
    private final IntSupplier maxStack;

    public BlueprintContainer(int size, IntSupplier maxStack) {
        super(size);
        this.maxStack = maxStack;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem;
    }

    @Override
    public int getMaxStackSize() {
        return maxStack.getAsInt();
    }

    public static class BlueprintSlot extends ContainerSlot {
        public BlueprintSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        public BlueprintSlot(Container container, int slot, int x, int y, boolean fake) {
            super(container, slot, x, y, fake);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return Feature.BLUEPRINT.enabled() && super.mayPlace(stack);
        }

        @Override
        public boolean isActive() {
            return Feature.BLUEPRINT.enabled();
        }
    }
}
