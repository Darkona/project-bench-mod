package com.darkona.projectbench.data;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
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
        basicItem(Content.id("blueprint_recorded"));
        basicItem(Content.Items.BLUEPRINT.get()).override().predicate(Content.id("recorded"), 1F)
                                           .model(new ModelFile.UncheckedModelFile(modLoc("item/blueprint_recorded"))).end();
    }
}
