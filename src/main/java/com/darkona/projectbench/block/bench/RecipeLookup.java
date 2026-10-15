package com.darkona.projectbench.block.bench;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeLookup {
    private static final int LIMIT = 256;
    private static final Comparator<RecipeHolder<?>> BY_ID = Comparator.comparing(RecipeHolder::id);
    private static final Cache SERVER = new Cache();
    private static final Cache CLIENT = new Cache();

    private RecipeLookup() {
    }

    public static List<RecipeHolder<CraftingRecipe>> find(Level level, CraftingInput input) {
        Cache cache = level.isClientSide ? CLIENT : SERVER;
        RecipeManager manager = level.getRecipeManager();
        if (cache.owner != manager) {
            cache.owner = manager;
            cache.map.clear();
        }
        Key probe = cache.probe.of(input);
        List<RecipeHolder<CraftingRecipe>> found = cache.map.get(probe);
        if (found == null) {
            List<RecipeHolder<CraftingRecipe>> list = new ArrayList<>(manager.getRecipesFor(RecipeType.CRAFTING, input, level));
            list.sort(BY_ID);
            found = List.copyOf(list);
            cache.map.put(probe.copy(), found);
        }
        probe.release();
        return found;
    }

    public static void clearClient() {
        CLIENT.map.clear();
    }

    private static final class Cache {
        @Nullable
        RecipeManager owner;
        final Key probe = new Key();
        final Map<Key, List<RecipeHolder<CraftingRecipe>>> map = new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, List<RecipeHolder<CraftingRecipe>>> eldest) {
                return size() > LIMIT;
            }
        };
    }

    private static final class Key {
        private int width;
        private int height;
        private List<ItemStack> items = List.of();
        private int hash;

        Key of(CraftingInput input) {
            width = input.width();
            height = input.height();
            items = input.items();
            int h = width * 31 + height;
            for (int i = 0, n = items.size(); i < n; ++i) {
                ItemStack s = items.get(i);
                h = h * 31 + (s.isEmpty() ? 0 : ItemStack.hashItemAndComponents(s));
            }
            hash = h;
            return this;
        }

        Key copy() {
            Key k = new Key();
            k.width = width;
            k.height = height;
            k.hash = hash;
            ItemStack[] own = new ItemStack[items.size()];
            for (int i = 0; i < own.length; ++i) {
                own[i] = items.get(i).copyWithCount(1);
            }
            k.items = Arrays.asList(own);
            return k;
        }

        void release() {
            items = List.of();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key k) || k.hash != hash || k.width != width || k.height != height || k.items.size() != items.size()) {
                return false;
            }
            for (int i = 0, n = items.size(); i < n; ++i) {
                if (!ItemStack.isSameItemSameComponents(items.get(i), k.items.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return items.toString();
        }
    }
}
