package com.darkona.projectbench.data;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.index.BenchItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class BenchItemModelGen extends ItemModelProvider {
    BenchItemModelGen(PackOutput out, ExistingFileHelper files) {
        super(out, ProjectBench.MOD_ID, files);
    }

    @Override
    protected void registerModels() {
        basicItem(ProjectBench.id("blueprint_recorded"));
        basicItem(BenchItems.BLUEPRINT.get()).override().predicate(ProjectBench.id("recorded"), 1F)
                                           .model(new ModelFile.UncheckedModelFile(modLoc("item/blueprint_recorded"))).end();
    }
}
