package com.darkona.projectbench;

import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.content.bench.RecordRecipe;
import com.darkona.projectbench.content.bench.SlotLocks;
import com.darkona.projectbench.compat.create.CreateCompat;
import com.darkona.projectbench.compat.jade.JadeCompat;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.datagen.DataGen;
import dev.darkona.jdlib.api.compat.Bridges;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
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
        Content.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        Config.register(modBus);
        ClientConfig.register(modBus);
        modBus.addListener((RegisterPayloadHandlersEvent e) -> Config.SYNC.register(e.registrar("1")));
        RecordRecipe.register(modBus);
        SlotLocks.register(modBus);
        Feature.guardUse();
        modBus.addListener(ProjectBench::registerCapabilities);
        modBus.addListener(DataGen::gather);
        Bridges.ifLoaded(CreateCompat.MOD_ID, () -> modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(CreateCompat::register)));
        Bridges.ifLoaded(JadeCompat.MOD_ID, () -> JadeCompat.init());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Content.BlockEntities.PROJECT_BENCH.get(), ProjectBenchBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Content.BlockEntities.BLUEPRINT_ARCHIVE.get(), BlueprintArchiveBlockEntity::itemHandler);
    }
}
