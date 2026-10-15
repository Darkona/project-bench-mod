package com.darkona.projectbench.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public final class SidedItemHandler implements IItemHandler {
    @FunctionalInterface
    public interface Rule {
        boolean allows(@Nullable Direction side);
    }

    private final IItemHandler inner;
    @Nullable
    private final Direction side;
    private final BooleanSupplier working;
    private final Rule inserts;
    private final Rule extracts;

    public SidedItemHandler(IItemHandler inner, @Nullable Direction side, BooleanSupplier working, Rule inserts, Rule extracts) {
        this.inner = inner;
        this.side = side;
        this.working = working;
        this.inserts = inserts;
        this.extracts = extracts;
    }

    public static SidedItemHandler[] forEverySide(IItemHandler inner, BooleanSupplier working, Rule inserts, Rule extracts) {
        SidedItemHandler[] out = new SidedItemHandler[7];
        for (Direction d : Direction.values()) {
            out[d.get3DDataValue()] = new SidedItemHandler(inner, d, working, inserts, extracts);
        }
        out[6] = new SidedItemHandler(inner, null, working, inserts, extracts);
        return out;
    }

    public static SidedItemHandler of(SidedItemHandler[] handlers, @Nullable Direction side) {
        return handlers[side == null ? 6 : side.get3DDataValue()];
    }

    private boolean canInsert() {
        return working.getAsBoolean() && inserts.allows(side);
    }

    private boolean canExtract() {
        return working.getAsBoolean() && extracts.allows(side);
    }

    @Override
    public int getSlots() {
        return inner.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inner.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return canInsert() ? inner.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return canExtract() ? inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return inner.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return canInsert() && inner.isItemValid(slot, stack);
    }
}
