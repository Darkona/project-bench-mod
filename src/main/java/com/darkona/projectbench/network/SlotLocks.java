package com.darkona.projectbench.network;

import com.darkona.projectbench.content.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SlotLocks(BlockPos pos, int slot, ItemStack lock) implements CustomPacketPayload {
    public static final Type<SlotLocks> TYPE = new Type<>(Content.id("slot_locks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotLocks> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SlotLocks::pos,
            ByteBufCodecs.VAR_INT, SlotLocks::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, SlotLocks::lock,
            SlotLocks::new);


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SlotLocks payload, IPayloadContext context) {
        if (payload.slot >= 0 && payload.slot < ProjectBenchBlockEntity.STORAGE
                && context.player().level().getBlockEntity(payload.pos) instanceof ProjectBenchBlockEntity bench) {
            bench.storage().setLock(payload.slot, payload.lock);
        }
    }
}
