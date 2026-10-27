package com.darkona.projectbench.data;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.content.bench.ProjectBenchBlock;
import com.darkona.projectbench.index.BenchBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

final class BenchTagGen extends BlockTagsProvider {
    BenchTagGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(out, lookup, ProjectBench.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredBlock<ProjectBenchBlock> bench : BenchBlocks.BENCHES) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(bench.get());
        }
        tag(BlockTags.MINEABLE_WITH_AXE).add(BenchBlocks.BLUEPRINT_ARCHIVE.get());
    }
}
