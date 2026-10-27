package com.darkona.projectbench.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BenchBlockEntity extends BlockEntity implements Clearable {
    private long keepStamp = -1;

    protected BenchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void dropContents(Level level, BlockPos pos);

    public void keepContents(Level level) {
        keepStamp = level.getGameTime();
    }

    public boolean keeping() {
        return level != null && keepStamp == level.getGameTime();
    }

    protected static void load(SimpleContainer container, CompoundTag tag, String key, HolderLookup.Provider registries) {
        container.getItems().replaceAll(s -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound(key), container.getItems(), registries);
    }
}
