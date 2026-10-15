package com.darkona.projectbench.client;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface RecipeChoices {
    int recipeCount();

    ResourceLocation recipeId(int i);

    ItemStack recipeResult(int i, HolderLookup.Provider registries);

    int choice();

    int revision();

    void selectRecipe(int i);

    Slot resultSlot();
}
