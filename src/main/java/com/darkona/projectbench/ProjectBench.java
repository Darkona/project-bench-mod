package com.darkona.projectbench;

import com.darkona.projectbench.compat.create.CreateCompat;
import com.darkona.projectbench.compat.jade.JadeCompat;
import com.darkona.projectbench.config.BenchConfig;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.data.BenchDatagen;
import com.darkona.projectbench.index.BenchBlockEntityTypes;
import com.darkona.projectbench.index.BenchBlocks;
import com.darkona.projectbench.index.BenchConditions;
import com.darkona.projectbench.index.BenchCreativeModeTabs;
import com.darkona.projectbench.index.BenchDataComponents;
import com.darkona.projectbench.index.BenchItems;
import com.darkona.projectbench.index.BenchMenuTypes;
import com.darkona.projectbench.index.BenchRecipeTypes;
import com.darkona.projectbench.network.BenchPackets;
import dev.darkona.jdlib.api.compat.Bridges;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(ProjectBench.MOD_ID)
public final class ProjectBench {
    public static final String MOD_ID = "projectbench";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public ProjectBench(IEventBus modBus, ModContainer container) {
        BenchBlocks.register(modBus);
        BenchItems.register(modBus);
        BenchBlockEntityTypes.register(modBus);
        BenchMenuTypes.register(modBus);
        BenchDataComponents.register(modBus);
        BenchCreativeModeTabs.register(modBus);
        BenchConditions.register(modBus);
        BenchRecipeTypes.register(modBus);
        BenchConfig.register(modBus, container);
        BenchPackets.register(modBus);
        Feature.guardUse();
        modBus.addListener(ProjectBench::registerCapabilities);
        modBus.addListener(BenchDatagen::gather);
        Bridges.ifLoaded(CreateCompat.MOD_ID, () -> modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(CreateCompat::register)));
        Bridges.ifLoaded(JadeCompat.MOD_ID, () -> JadeCompat.init());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BenchBlockEntityTypes.PROJECT_BENCH.get(), ProjectBenchBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BenchBlockEntityTypes.BLUEPRINT_ARCHIVE.get(), BlueprintArchiveBlockEntity::itemHandler);
    }
}
