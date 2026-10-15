package com.darkona.projectbench.network;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.blueprint.Blueprint;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record RecordRecipe(ResourceLocation recipe, boolean fill) implements CustomPacketPayload {
    public static final Type<RecordRecipe> TYPE = new Type<>(ProjectBench.id("record_recipe"));
    public static final StreamCodec<ByteBuf, RecordRecipe> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, RecordRecipe::recipe,
            ByteBufCodecs.BOOL, RecordRecipe::fill,
            RecordRecipe::new);

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").playToServer(TYPE, CODEC, RecordRecipe::handle));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static RecipeHolder<CraftingRecipe> recordable(RecipeManager recipes, ResourceLocation id) {
        RecipeHolder<?> holder = recipes.byKey(id).orElse(null);
        return holder != null && holder.value() instanceof CraftingRecipe crafting && Blueprint.recordable(crafting)
                ? (RecipeHolder<CraftingRecipe>) holder : null;
    }

    private static void handle(RecordRecipe payload, IPayloadContext context) {
        receive(context.player(), payload);
    }

    public static void receive(Player player, RecordRecipe payload) {
        if (!player.level().isClientSide && player.containerMenu instanceof ProjectBenchMenu menu && menu.stillValid(player)) {
            RecipeHolder<CraftingRecipe> holder = recordable(player.level().getRecipeManager(), payload.recipe);
            if (holder != null) {
                menu.recordRecipe(holder, payload.fill);
            }
        }
    }
}
