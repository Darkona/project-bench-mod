package com.darkona.projectbench.data;

import com.darkona.projectbench.Content;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.neoforged.neoforge.registries.DeferredHolder;

final class BenchLootGen extends BlockLootSubProvider {
    private final List<Block> known = Content.Blocks.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::get).toList();

    BenchLootGen(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        Block archive = Content.Blocks.BLUEPRINT_ARCHIVE.get();
        for (Block block : known) {
            withContents(block, block != archive);
        }
    }

    private void withContents(Block block, boolean locks) {
        CopyComponentsFunction.Builder copy = CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                .include(Content.Components.CONTENTS.get());
        add(block, createSingleItemTable(block).apply(locks ? copy.include(Content.Components.SLOT_LOCKS.get()) : copy));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return known;
    }
}
