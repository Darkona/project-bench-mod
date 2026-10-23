package com.darkona.projectbench.datagen;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.concurrent.CompletableFuture;

final class Tags extends BlockTagsProvider {
    Tags(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(out, lookup, ProjectBench.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredBlock<ProjectBenchBlock> bench : Content.Blocks.BENCHES) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(bench.get());
        }
        tag(BlockTags.MINEABLE_WITH_AXE).add(Content.Blocks.BLUEPRINT_ARCHIVE.get());
    }
}
