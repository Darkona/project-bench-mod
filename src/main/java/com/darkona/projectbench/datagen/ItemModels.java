package com.darkona.projectbench.datagen;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class ItemModels extends ItemModelProvider {
    ItemModels(PackOutput out, ExistingFileHelper files) {
        super(out, ProjectBench.MOD_ID, files);
    }

    @Override
    protected void registerModels() {
        basicItem(ProjectBench.id("blueprint_recorded"));
        basicItem(ModItems.BLUEPRINT.get()).override().predicate(ProjectBench.id("recorded"), 1F)
                                           .model(new ModelFile.UncheckedModelFile(modLoc("item/blueprint_recorded"))).end();
    }
}
