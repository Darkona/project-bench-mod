package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.config.Feature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

public final class BenchUpgrade {
    private BenchUpgrade() {
    }

    static ItemStack keepContents(CraftingInput input, ItemStack result) {
        for (int i = 0; i < input.size(); ++i) {
            ItemStack bench = input.getItem(i);
            ItemContainerContents contents = bench.get(Content.Components.CONTENTS.get());
            ItemContainerContents locks = bench.get(Content.Components.SLOT_LOCKS.get());
            if (contents != null || locks != null) {
                result.set(Content.Components.CONTENTS.get(), contents);
                result.set(Content.Components.SLOT_LOCKS.get(), locks);
                break;
            }
        }
        return result;
    }

    public static final class Shaped extends ShapedRecipe {
        public static final RecipeSerializer<Shaped> SERIALIZER = new UpgradeSerializer<>(
                RecipeSerializer.SHAPED_RECIPE.codec().<Shaped>xmap(Shaped::new, r -> r),
                RecipeSerializer.SHAPED_RECIPE.streamCodec().<Shaped>map(Shaped::new, r -> r));

        public Shaped(ShapedRecipe plain) {
            super(plain.getGroup(), plain.category(), plain.pattern, plain.getResultItem(RegistryAccess.EMPTY), plain.showNotification());
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return Feature.allows(getResultItem(level.registryAccess()).getItem()) && super.matches(input, level);
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return keepContents(input, super.assemble(input, registries));
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return Content.Recipes.SHAPED_UPGRADE.get();
        }
    }

    public static final class Shapeless extends ShapelessRecipe {
        public static final RecipeSerializer<Shapeless> SERIALIZER = new UpgradeSerializer<>(
                RecipeSerializer.SHAPELESS_RECIPE.codec().<Shapeless>xmap(Shapeless::new, r -> r),
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().<Shapeless>map(Shapeless::new, r -> r));

        public Shapeless(ShapelessRecipe plain) {
            super(plain.getGroup(), plain.category(), plain.getResultItem(RegistryAccess.EMPTY), plain.getIngredients());
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return Feature.allows(getResultItem(level.registryAccess()).getItem()) && super.matches(input, level);
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return keepContents(input, super.assemble(input, registries));
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return Content.Recipes.SHAPELESS_UPGRADE.get();
        }
    }

    private record UpgradeSerializer<T extends Recipe<?>>(MapCodec<T> codec,
            StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) implements RecipeSerializer<T> {
    }
}
