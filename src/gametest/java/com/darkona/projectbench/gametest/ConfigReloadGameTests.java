package com.darkona.projectbench.gametest;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.BenchConfig;
import com.mojang.authlib.GameProfile;
import dev.darkona.jdlib.api.config.ConfigSync;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(ProjectBench.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigReloadGameTests {
    private ConfigReloadGameTests() {
    }

    private static ServerPlayer listening(GameTestHelper helper, List<BenchConfig.Settings> received) {
        MinecraftServer server = helper.getLevel().getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "listener");
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof ConfigSync.Snapshot snapshot) {
                    received.add(BenchConfig.Settings.STREAM_CODEC.decode(Unpooled.wrappedBuffer(snapshot.values())));
                }
            }
        };
        return player;
    }

    @GameTest(template = GameTests.EMPTY)
    public static void aReloadThatChangesAValueReachesTheConnectedPlayers(GameTestHelper helper) {
        List<BenchConfig.Settings> received = new ArrayList<>();
        ServerPlayer player = listening(helper, received);
        BenchConfig.Settings before = BenchConfig.get();
        BenchConfig.Settings edited = before.toBuilder().slotLocks(!before.slotLocks()).build();
        try {
            helper.assertFalse(BenchConfig.SYNC.adopt(BenchConfig.CONFIG, before, List.of(player)), "a reload that changes nothing sends nothing");
            helper.assertTrue(received.isEmpty(), "and the player got nothing");
            helper.assertTrue(BenchConfig.SYNC.adopt(BenchConfig.CONFIG, edited, List.of(player)), "a changed value is sent");
            helper.assertTrue(BenchConfig.get().equals(edited), "the server follows the file");
            helper.assertTrue(received.equals(List.of(edited)), "the player got the new Settings once");
        } finally {
            BenchConfig.SYNC.adopt(BenchConfig.CONFIG, before, List.of());
        }
        helper.assertTrue(BenchConfig.get().equals(before), "the Settings are back");
        helper.succeed();
    }
}
