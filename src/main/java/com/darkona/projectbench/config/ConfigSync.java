package com.darkona.projectbench.config;

import com.darkona.projectbench.ProjectBench;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collection;
import java.util.List;

public record ConfigSync(Config.Settings settings) implements CustomPacketPayload {
    public static final Type<ConfigSync> TYPE = new Type<>(ProjectBench.id("config_sync"));
    public static final StreamCodec<ByteBuf, ConfigSync> CODEC = Config.Settings.STREAM_CODEC.map(ConfigSync::new, ConfigSync::settings);

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").playToClient(TYPE, CODEC, ConfigSync::handle));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                send(List.of(player));
            }
        });
    }

    public static boolean reload(Config.Settings file, Collection<ServerPlayer> players) {
        if (!Config.adopt(file, true)) {
            return false;
        }
        send(players);
        return true;
    }

    private static void send(Collection<ServerPlayer> players) {
        ConfigSync payload = new ConfigSync(Config.get());
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void handle(ConfigSync payload, IPayloadContext context) {
        context.enqueueWork(() -> Config.receive(payload.settings));
    }
}
