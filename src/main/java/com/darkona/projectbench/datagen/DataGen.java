package com.darkona.projectbench.datagen;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.content.bench.ProjectBenchBlock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class DataGen {
    private DataGen() {
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        gen.addProvider(event.includeClient(), new BlockStates(out, files));
        gen.addProvider(event.includeClient(), new ItemModels(out, files));
        gen.addProvider(event.includeServer(), new Recipes(out, lookup));
        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(Loot::new, LootContextParamSets.BLOCK)), lookup));
        gen.addProvider(event.includeServer(), new Tags(out, lookup, files));
    }

    static final class ItemModels extends ItemModelProvider {
        ItemModels(PackOutput out, ExistingFileHelper files) {
            super(out, ProjectBench.MOD_ID, files);
        }

        @Override
        protected void registerModels() {
            basicItem(Content.id("blueprint_recorded"));
            basicItem(Content.Items.BLUEPRINT.get()).override().predicate(Content.id("recorded"), 1F)
                                               .model(new ModelFile.UncheckedModelFile(modLoc("item/blueprint_recorded"))).end();
        }
    }

    static final class Tags extends BlockTagsProvider {
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

    static final class Loot extends BlockLootSubProvider {
        private final List<Block> known = Content.Blocks.BLOCKS.getEntries().stream().<Block>map(DeferredHolder::get).toList();

        Loot(HolderLookup.Provider registries) {
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
}
