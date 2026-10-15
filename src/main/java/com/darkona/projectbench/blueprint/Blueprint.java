package com.darkona.projectbench.blueprint;

import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.registry.ModComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record Blueprint(List<ItemStack> pattern, ResourceLocation recipe, ItemStack result) {
    public static final int SIZE = 9;

    public static final Codec<Blueprint> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf(SIZE, SIZE).fieldOf("pattern").forGetter(Blueprint::pattern),
            ResourceLocation.CODEC.fieldOf("recipe").forGetter(Blueprint::recipe),
            ItemStack.CODEC.fieldOf("result").forGetter(Blueprint::result)
    ).apply(i, Blueprint::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Blueprint> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, Blueprint::pattern,
            ResourceLocation.STREAM_CODEC, Blueprint::recipe,
            ItemStack.STREAM_CODEC, Blueprint::result,
            Blueprint::new);

    public Blueprint {
        if (pattern.size() != SIZE) {
            throw new IllegalArgumentException("a blueprint has nine ghosts, not " + pattern.size());
        }
        List<ItemStack> ghosts = new ArrayList<>(SIZE);
        for (ItemStack s : pattern) {
            ghosts.add(s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1));
        }
        pattern = List.copyOf(ghosts);
        result = result.copy();
    }

    @Nullable
    public static Blueprint of(ItemStack stack) {
        return stack.get(ModComponents.BLUEPRINT.get());
    }

    public static boolean blank(ItemStack stack) {
        return Feature.BLUEPRINT.enabled() && stack.getItem() instanceof BlueprintItem && !recorded(stack);
    }

    public static boolean recorded(ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem && of(stack) != null;
    }

    @Nullable
    public static Blueprint of(RecipeHolder<CraftingRecipe> holder, HolderLookup.Provider registries) {
        List<Ingredient> grid = grid(holder.value());
        ItemStack result = holder.value().getResultItem(registries);
        if (grid == null || result.isEmpty()) {
            return null;
        }
        List<ItemStack> pattern = new ArrayList<>(SIZE);
        for (Ingredient ingredient : grid) {
            ItemStack[] items = ingredient.getItems();
            pattern.add(items.length == 0 ? ItemStack.EMPTY : items[0]);
        }
        return new Blueprint(pattern, holder.id(), result);
    }

    public static boolean recordable(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth() <= 3 && shaped.getHeight() <= 3;
        }
        return recipe instanceof ShapelessRecipe && recipe.getIngredients().size() <= SIZE;
    }

    @Nullable
    public static List<Ingredient> grid(CraftingRecipe recipe) {
        if (!recordable(recipe)) {
            return null;
        }
        NonNullList<Ingredient> grid = NonNullList.withSize(SIZE, Ingredient.EMPTY);
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        for (int i = 0, n = ingredients.size(); i < n; ++i) {
            grid.set(i / width * 3 + i % width, ingredients.get(i));
        }
        return grid;
    }

    public ItemStack ghost(int i) {
        return pattern.get(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Blueprint other) || !recipe.equals(other.recipe) || !ItemStack.matches(result, other.result)) {
            return false;
        }
        for (int i = 0; i < SIZE; ++i) {
            if (!ItemStack.matches(pattern.get(i), other.pattern.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = recipe.hashCode() * 31 + ItemStack.hashItemAndComponents(result);
        for (ItemStack s : pattern) {
            h = h * 31 + ItemStack.hashItemAndComponents(s);
        }
        return h;
    }
}
