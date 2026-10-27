package com.darkona.projectbench;

import com.darkona.projectbench.content.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.network.BenchPackets;
import com.darkona.projectbench.compat.create.CreateCompat;
import com.darkona.projectbench.compat.jade.JadeCompat;
import com.darkona.projectbench.config.BenchConfig;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.datagen.DataGen;
import dev.darkona.jdlib.api.compat.Bridges;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(ProjectBench.MOD_ID)
public final class ProjectBench {
    public static final String MOD_ID = "projectbench";

    public ProjectBench(IEventBus modBus, ModContainer container) {
        Content.register(modBus);
        BenchConfig.register(modBus, container);
        BenchPackets.register(modBus);
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
