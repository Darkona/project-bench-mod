package com.darkona.projectbench.block.bench;

import com.darkona.projectbench.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SlotLocks(BlockPos pos, int slot, ItemStack lock) implements CustomPacketPayload {
    public static final Type<SlotLocks> TYPE = new Type<>(Content.id("slot_locks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotLocks> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SlotLocks::pos,
            ByteBufCodecs.VAR_INT, SlotLocks::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, SlotLocks::lock,
            SlotLocks::new);

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").playToClient(TYPE, CODEC, SlotLocks::handle));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void handle(SlotLocks payload, IPayloadContext context) {
        if (payload.slot >= 0 && payload.slot < ProjectBenchBlockEntity.STORAGE
                && context.player().level().getBlockEntity(payload.pos) instanceof ProjectBenchBlockEntity bench) {
            bench.storage().setLock(payload.slot, payload.lock);
        }
    }
}
