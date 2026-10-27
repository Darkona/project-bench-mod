package com.darkona.projectbench.compat.emi;

import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.content.bench.ProjectBenchBlock;
import com.darkona.projectbench.index.BenchBlocks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import net.neoforged.neoforge.registries.DeferredBlock;

@EmiEntrypoint
public final class EmiPlugin implements dev.emi.emi.api.EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        if (!ClientConfig.emi()) {
            return;
        }
        for (DeferredBlock<ProjectBenchBlock> bench : BenchBlocks.BENCHES) {
            registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(bench.get()));
        }
    }
}
