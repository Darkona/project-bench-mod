package com.darkona.projectbench.content.bench;

import com.darkona.projectbench.content.bench.RecipeSelector.Choices;
import dev.darkona.jdlib.api.pattern.Blueprint;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

final class BenchChoices implements Choices {
    private final ProjectBenchMenu menu;
    private final Matrix[] matrices;

    BenchChoices(ProjectBenchMenu menu) {
        this.menu = menu;
        this.matrices = new Matrix[menu.station() ? 2 : 1];
        for (int which = 0; which < matrices.length; ++which) {
            matrices[which] = new Matrix(which);
        }
    }

    static void press(ProjectBenchMenu menu, int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    ProjectBenchMenu menu() {
        return menu;
    }

    int halves() {
        return matrices.length;
    }

    Choices half(int which) {
        return matrices[which];
    }

    void tick() {
        for (Matrix matrix : matrices) {
            matrix.listRecipes();
        }
    }

    private final class Matrix implements Choices {
        private final int which;
        private final MatrixLayout listed = new MatrixLayout();
        private List<RecipeHolder<CraftingRecipe>> recipes = List.of();
        private ItemStack[] results = new ItemStack[0];
        private int seenChoice = -1;
        private int revision;

        Matrix(int which) {
            this.which = which;
        }

        void listRecipes() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            boolean changed = false;
            int start = menu.matrixStart(which);
            for (int i = 0; i < Blueprint.SIZE; ++i) {
                changed |= listed.set(i, menu.getSlot(start + i).getItem());
            }
            if (changed) {
                CraftingInput input = CraftingInput.of(3, 3, listed.items());
                recipes = ProjectBenchBlockEntity.recipesFor(minecraft.level, input);
                results = new ItemStack[recipes.size()];
                for (int i = 0; i < results.length; ++i) {
                    results[i] = recipes.get(i).value().assemble(input, minecraft.level.registryAccess());
                }
            }
            if (changed || seenChoice != menu.choice(which)) {
                seenChoice = menu.choice(which);
                ++revision;
            }
        }

        @Override
        public int recipeCount() {
            return recipes.size();
        }

        @Override
        public ResourceLocation recipeId(int i) {
            return recipes.get(i).id();
        }

        @Override
        public ItemStack recipeResult(int i, HolderLookup.Provider registries) {
            return results[i];
        }

        @Override
        public int choice() {
            int c = menu.choice(which);
            return c < recipes.size() ? c : -1;
        }

        @Override
        public int revision() {
            return revision;
        }

        @Override
        public void selectRecipe(int i) {
            if (i >= 0 && i < recipes.size() && i < ProjectBenchMenu.MAX_RECIPES) {
                press(menu, ProjectBenchMenu.recipeButton(which, i));
            }
        }

        @Override
        public Slot resultSlot() {
            return menu.getSlot(menu.resultIndex(which));
        }
    }

    private Matrix active() {
        return matrices[Math.min(menu.active(), matrices.length - 1)];
    }

    @Override
    public int recipeCount() {
        return active().recipeCount();
    }

    @Override
    public ResourceLocation recipeId(int i) {
        return active().recipeId(i);
    }

    @Override
    public ItemStack recipeResult(int i, HolderLookup.Provider registries) {
        return active().recipeResult(i, registries);
    }

    @Override
    public int choice() {
        return active().choice();
    }

    @Override
    public int revision() {
        return active().revision() * 2 + menu.active();
    }

    @Override
    public void selectRecipe(int i) {
        active().selectRecipe(i);
    }

    @Override
    public Slot resultSlot() {
        return active().resultSlot();
    }
}
