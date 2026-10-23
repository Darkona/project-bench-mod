package com.darkona.projectbench.client;

import dev.darkona.jdlib.api.ui.JDNode;
import dev.darkona.jdlib.api.ui.JDViews;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class RecipeSelector {
    public static final int BUTTON = 10;
    private static final int ROW_TOP = -26;

    private static boolean own = true;

    private RecipeSelector() {
    }

    @Nullable
    public static JDNode forWindow(String id, Choices choices, int buttonX, int buttonY, int resultCenterX) {
        if (!own) {
            return null;
        }
        return JDViews.at(JDViews.popupChoices(id, BUTTON, BUTTON, choices::recipeCount, i -> result(choices, i), choices::choice,
                choices::selectRecipe, resultCenterX - buttonX, ROW_TOP - buttonY), buttonX, buttonY);
    }

    public static void useExternal() {
        own = false;
    }

    private static ItemStack result(Choices choices, int i) {
        var level = Minecraft.getInstance().level;
        if (level == null || i < 0 || i >= choices.recipeCount()) {
            return ItemStack.EMPTY;
        }
        return choices.recipeResult(i, level.registryAccess());
    }

    public interface Choices {
        int recipeCount();

        ResourceLocation recipeId(int i);

        ItemStack recipeResult(int i, HolderLookup.Provider registries);

        int choice();

        int revision();

        void selectRecipe(int i);

        Slot resultSlot();
    }
}
