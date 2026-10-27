package com.darkona.projectbench.network;

import com.darkona.projectbench.config.BenchConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BenchPackets {
    private BenchPackets() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BenchPackets::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        BenchConfig.SYNC.register(registrar);
        registrar.playToServer(RecordRecipe.TYPE, RecordRecipe.CODEC, RecordRecipe::handle);
        registrar.playToClient(SlotLocks.TYPE, SlotLocks.CODEC, SlotLocks::handle);
    }
}
