package com.darkona.projectbench.datagen;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class Loot extends BlockLootSubProvider {
    Loot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (DeferredBlock<ProjectBenchBlock> bench : Content.Blocks.BENCHES) {
            withContents(bench.get(), true);
        }
        withContents(Content.Blocks.BLUEPRINT_ARCHIVE.get(), false);
    }

    private void withContents(Block block, boolean locks) {
        CopyComponentsFunction.Builder copy = CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                .include(Content.Components.CONTENTS.get());
        add(block, createSingleItemTable(block).apply(locks ? copy.include(Content.Components.SLOT_LOCKS.get()) : copy));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> known = new ArrayList<>();
        for (DeferredBlock<ProjectBenchBlock> bench : Content.Blocks.BENCHES) {
            known.add(bench.get());
        }
        known.add(Content.Blocks.BLUEPRINT_ARCHIVE.get());
        return known;
    }
}
