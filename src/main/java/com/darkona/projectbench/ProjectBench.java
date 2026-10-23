package com.darkona.projectbench;

import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.compat.create.CreateCompat;
import com.darkona.projectbench.compat.jade.JadeCompat;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.FeatureEvents;
import com.darkona.projectbench.datagen.DataGen;
import com.darkona.projectbench.block.bench.RecordRecipe;
import com.darkona.projectbench.block.bench.SlotLocks;
import com.darkona.projectbench.registry.ModBlockEntities;
import com.darkona.projectbench.registry.ModBlocks;
import com.darkona.projectbench.registry.ModComponents;
import com.darkona.projectbench.registry.ModConditions;
import com.darkona.projectbench.registry.ModItems;
import com.darkona.projectbench.registry.ModMenus;
import com.darkona.projectbench.registry.ModRecipes;
import com.darkona.projectbench.registry.ModTab;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import dev.darkona.jdlib.api.compat.Bridges;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(ProjectBench.MOD_ID)
public final class ProjectBench {
    public static final String MOD_ID = "projectbench";

    public ProjectBench(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModTab.TABS.register(modBus);
        ModConditions.CONDITIONS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        Config.register(modBus);
        ClientConfig.register(modBus);
        modBus.addListener((RegisterPayloadHandlersEvent e) -> Config.SYNC.register(e.registrar("1")));
        RecordRecipe.register(modBus);
        SlotLocks.register(modBus);
        FeatureEvents.register();
        modBus.addListener(ProjectBench::registerCapabilities);
        modBus.addListener(DataGen::gather);
        Bridges.ifLoaded(CreateCompat.MOD_ID, () -> modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(CreateCompat::register)));
        Bridges.ifLoaded(JadeCompat.MOD_ID, () -> JadeCompat.init());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PROJECT_BENCH.get(), ProjectBenchBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BLUEPRINT_ARCHIVE.get(), BlueprintArchiveBlockEntity::itemHandler);
    }
}
