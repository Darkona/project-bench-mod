package com.darkona.projectbench.data;

import com.darkona.projectbench.data.recipe.BenchStandardRecipeGen;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class BenchDatagen {
    private BenchDatagen() {
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        gen.addProvider(event.includeClient(), new BenchBlockStateGen(out, files));
        gen.addProvider(event.includeClient(), new BenchItemModelGen(out, files));
        gen.addProvider(event.includeServer(), new BenchStandardRecipeGen(out, lookup));
        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BenchLootGen::new, LootContextParamSets.BLOCK)), lookup));
        gen.addProvider(event.includeServer(), new BenchTagGen(out, lookup, files));
    }
}
