package com.darkona.projectbench.compat.polymorph;

import com.darkona.projectbench.client.RecipeChoices;
import com.illusivesoulworks.polymorph.api.client.base.AbstractRecipesWidget;
import com.illusivesoulworks.polymorph.api.client.base.ITickingRecipesWidget;
import com.illusivesoulworks.polymorph.api.common.base.IRecipePair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

final class ChoiceRecipesWidget extends AbstractRecipesWidget implements ITickingRecipesWidget {
    private final RecipeChoices choices;
    private int seenRevision = -1;

    ChoiceRecipesWidget(AbstractContainerScreen<?> screen, RecipeChoices choices) {
        super(screen);
        this.choices = choices;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (seenRevision == choices.revision() || mc.level == null) {
            return;
        }
        seenRevision = choices.revision();
        int n = choices.recipeCount();
        Set<IRecipePair> pairs = new HashSet<>(n * 2);
        for (int i = 0; i < n; ++i) {
            pairs.add(new Choice(i, choices.recipeId(i), choices.recipeResult(i, mc.level.registryAccess())));
        }
        int chosen = choices.choice();
        setRecipesList(pairs, chosen < 0 || chosen >= n ? null : choices.recipeId(chosen));
    }

    @Override
    public void selectRecipe(ResourceLocation id) {
        for (int i = 0, n = choices.recipeCount(); i < n; ++i) {
            if (choices.recipeId(i).equals(id)) {
                choices.selectRecipe(i);
                return;
            }
        }
    }

    @Override
    public Slot getOutputSlot() {
        return choices.resultSlot();
    }

    private record Choice(int index, ResourceLocation id, ItemStack output) implements IRecipePair {
        @Override
        public ItemStack getOutput() {
            return output;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return id;
        }

        @Override
        public int compareTo(@NotNull IRecipePair other) {
            return other instanceof Choice c ? Integer.compare(index, c.index) : id.compareTo(other.getResourceLocation());
        }
    }
}
