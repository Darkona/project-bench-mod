package com.darkona.projectbench.compat.jei;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.Feature;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@mezz.jei.api.JeiPlugin
public final class JeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ProjectBench.id("jei");

    @Nullable
    private static IJeiRuntime runtime;

    private static final List<ItemStack> HIDDEN = new ArrayList<>();

    private static final Runnable FEATURES_CHANGED = () -> Minecraft.getInstance().execute(JeiPlugin::applyFeatures);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ClientConfig.jei()) {
            for (DeferredBlock<ProjectBenchBlock> bench : Content.Blocks.BENCHES) {
                registration.addRecipeCatalyst(bench.get(), RecipeTypes.CRAFTING);
            }
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;

        HIDDEN.clear();
        if (ClientConfig.jei()) {
            Feature.listen(FEATURES_CHANGED);
            applyFeatures();
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        Feature.unlisten(FEATURES_CHANGED);
        runtime = null;
        HIDDEN.clear();
    }

    private static void applyFeatures() {
        IJeiRuntime r = runtime;
        if (r == null) {
            return;
        }
        if (!HIDDEN.isEmpty()) {
            r.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.copyOf(HIDDEN));
            HIDDEN.clear();
        }
        for (Feature f : Feature.values()) {
            if (!f.enabled()) {
                HIDDEN.add(new ItemStack(f.item()));
            }
        }
        if (!HIDDEN.isEmpty()) {
            r.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.copyOf(HIDDEN));
        }
    }
}
