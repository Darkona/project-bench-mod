package com.darkona.projectbench.infrastructure.gametest;

import com.darkona.projectbench.ProjectBench;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.items.IItemHandler;

@EventBusSubscriber(modid = ProjectBench.MOD_ID)
public final class FakeInventories {
    public static final Block BLOCK = Blocks.LODESTONE;
    public static final Block SIDED_BLOCK = Blocks.REINFORCED_DEEPSLATE;
    private static final Long2ObjectOpenHashMap<IItemHandler> AT = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<IItemHandler[]> SIDED_AT = new Long2ObjectOpenHashMap<>();

    private FakeInventories() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, side) -> AT.get(pos.asLong()), BLOCK);
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            IItemHandler[] faces = SIDED_AT.get(pos.asLong());
            return faces == null || side == null ? null : faces[side.get3DDataValue()];
        }, SIDED_BLOCK);
    }

    public static void place(GameTestHelper helper, BlockPos relative, IItemHandler fake) {
        AT.put(helper.absolutePos(relative).asLong(), fake);
        helper.setBlock(relative, BLOCK);
    }

    public static void placeSided(GameTestHelper helper, BlockPos relative, IItemHandler... byFace) {
        SIDED_AT.put(helper.absolutePos(relative).asLong(), byFace);
        helper.setBlock(relative, SIDED_BLOCK);
    }

    public abstract static class OneSlot implements IItemHandler {
        protected final Item item;
        protected int count;
        public int received;

        OneSlot(Item item, int count) {
            this.item = item;
            this.count = count;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.is(item)) {
                return stack;
            }
            if (!simulate) {
                received += stack.getCount();
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(item);
        }
    }

    public static final class Vending extends OneSlot {
        public Vending(Item item) {
            super(item, Integer.MAX_VALUE);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return amount <= 0 ? ItemStack.EMPTY : new ItemStack(item, amount);
        }
    }

    public static final class Drawer extends OneSlot {
        public Drawer(Item item, int count) {
            super(item, count);
        }

        public int count() {
            return count;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack rest = super.insertItem(slot, stack, simulate);
            if (!simulate && rest.isEmpty()) {
                count += stack.getCount();
            }
            return rest;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int n = Math.min(amount, count);
            if (n <= 0) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                count -= n;
            }
            return new ItemStack(item, n);
        }
    }

    public static final class Greedy extends OneSlot {
        private final boolean honestWhenSimulating;

        public Greedy(Item item, boolean honestWhenSimulating) {
            super(item, Integer.MAX_VALUE);
            this.honestWhenSimulating = honestWhenSimulating;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, simulate && honestWhenSimulating ? amount : Integer.MAX_VALUE);
        }
    }
}
