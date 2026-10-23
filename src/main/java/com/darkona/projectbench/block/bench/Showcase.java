package com.darkona.projectbench.block.bench;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.darkona.projectbench.block.bench.ProjectBenchBlockEntity.MATRIX;

final class Showcase {
    private final ProjectBenchBlockEntity bench;

    private final MatrixLayout sent = new MatrixLayout();
    private ItemStack sentResult = ItemStack.EMPTY;
    private boolean sentOn;

    private final NonNullList<ItemStack> shown = NonNullList.withSize(MATRIX, ItemStack.EMPTY);
    private ItemStack shownResult = ItemStack.EMPTY;
    private boolean shownOn;

    Showcase(ProjectBenchBlockEntity bench) {
        this.bench = bench;
    }

    void send(boolean force) {
        Level level = bench.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean on = bench.showcase();
        boolean changed = force || on != sentOn;
        if (on) {
            changed |= sent.update(bench.matrix().getItems());
            ItemStack made = bench.result().getItem(0);
            if (!ItemStack.isSameItemSameComponents(made, sentResult)) {
                sentResult = made.isEmpty() ? ItemStack.EMPTY : made.copyWithCount(1);
                changed = true;
            }
        }
        sentOn = on;
        if (changed) {
            level.sendBlockUpdated(bench.getBlockPos(), bench.getBlockState(), bench.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    CompoundTag updateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (bench.showcase()) {
            tag.putBoolean("Show", true);
            tag.put("Items", ContainerHelper.saveAllItems(new CompoundTag(), sentItemsAsList(), registries));

            ItemStack made = bench.result().getItem(0);
            if (!made.isEmpty()) {
                tag.put("Made", made.copyWithCount(1).save(registries));
            }
        }
        return tag;
    }

    private NonNullList<ItemStack> sentItemsAsList() {
        NonNullList<ItemStack> list = NonNullList.withSize(MATRIX, ItemStack.EMPTY);
        for (int i = 0; i < MATRIX; ++i) {
            ItemStack now = bench.matrix().getItem(i);
            list.set(i, now.isEmpty() ? ItemStack.EMPTY : now.copyWithCount(1));
        }
        return list;
    }

    void receive(CompoundTag tag, HolderLookup.Provider registries) {
        shownOn = tag.getBoolean("Show");
        shown.clear();
        ContainerHelper.loadAllItems(tag.getCompound("Items"), shown, registries);
        shownResult = tag.contains("Made") ? ItemStack.parseOptional(registries, tag.getCompound("Made")) : ItemStack.EMPTY;
    }

    @Nullable
    List<ItemStack> items() {
        return shownOn ? shown : null;
    }

    ItemStack result() {
        return shownResult;
    }
}
