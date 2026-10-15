package com.darkona.projectbench.block.bench;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Stock {
    int count(ItemStack like);

    int take(ItemStack like, int amount);

    ItemStack find(Predicate<ItemStack> test);

    ItemStack insert(ItemStack stack);

    static Stock of(Container container) {
        return new Stock() {
            @Override
            public int count(ItemStack like) {
                int n = 0;
                for (int i = 0, size = container.getContainerSize(); i < size; ++i) {
                    ItemStack there = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(there, like)) {
                        n += there.getCount();
                    }
                }
                return n;
            }

            @Override
            public int take(ItemStack like, int amount) {
                int taken = 0;
                for (int i = 0, size = container.getContainerSize(); i < size && taken < amount; ++i) {
                    if (ItemStack.isSameItemSameComponents(container.getItem(i), like)) {
                        taken += container.removeItem(i, amount - taken).getCount();
                    }
                }
                return taken;
            }

            @Override
            public ItemStack find(Predicate<ItemStack> test) {
                for (int i = 0, size = container.getContainerSize(); i < size; ++i) {
                    ItemStack there = container.getItem(i);
                    if (!there.isEmpty() && test.test(there)) {
                        return there.copyWithCount(1);
                    }
                }
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insert(ItemStack stack) {
                return BenchStacks.insert(container, stack);
            }
        };
    }

    static Stock pockets(Inventory inventory) {
        return new Stock() {
            @Override
            public int count(ItemStack like) {
                return 0;
            }

            @Override
            public int take(ItemStack like, int amount) {
                return 0;
            }

            @Override
            public ItemStack find(Predicate<ItemStack> test) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insert(ItemStack stack) {
                ItemStack rest = BenchStacks.insert(inventory, Inventory.INVENTORY_SIZE, stack);
                if (rest.getCount() != stack.getCount()) {
                    inventory.setChanged();
                }
                return rest;
            }
        };
    }

    static Stock of(IItemHandler handler) {
        return new HandlerStock(() -> handler);
    }

    static Stock lazy(Supplier<Stock> source) {
        return new Stock() {
            @Override
            public int count(ItemStack like) {
                Stock stock = source.get();
                return stock == null ? 0 : stock.count(like);
            }

            @Override
            public int take(ItemStack like, int amount) {
                Stock stock = source.get();
                return stock == null ? 0 : stock.take(like, amount);
            }

            @Override
            public ItemStack find(Predicate<ItemStack> test) {
                Stock stock = source.get();
                return stock == null ? ItemStack.EMPTY : stock.find(test);
            }

            @Override
            public ItemStack insert(ItemStack stack) {
                Stock stock = source.get();
                return stock == null ? stack : stock.insert(stack);
            }
        };
    }

    static Stock chain(Stock first, Stock second) {
        return new Stock() {
            @Override
            public int count(ItemStack like) {
                return BenchStacks.sum(first.count(like), second.count(like));
            }

            @Override
            public int take(ItemStack like, int amount) {
                int taken = first.take(like, amount);
                return taken >= amount ? taken : taken + second.take(like, amount - taken);
            }

            @Override
            public ItemStack find(Predicate<ItemStack> test) {
                ItemStack found = first.find(test);
                return found.isEmpty() ? second.find(test) : found;
            }

            @Override
            public ItemStack insert(ItemStack stack) {
                ItemStack rest = first.insert(stack);
                return rest.isEmpty() ? rest : second.insert(rest);
            }
        };
    }
}
