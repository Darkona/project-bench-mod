package com.darkona.projectbench.block.bench;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingContainer;

public final class CraftingGrid extends SimpleContainer implements CraftingContainer {
    public CraftingGrid() {
        super(ProjectBenchBlockEntity.MATRIX);
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }
}
