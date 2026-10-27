package com.darkona.projectbench.network;

import com.darkona.projectbench.content.bench.ProjectBenchMenu;
import com.darkona.projectbench.Content;
import dev.darkona.jdlib.api.pattern.Blueprint;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record RecordRecipe(ResourceLocation recipe, boolean fill) implements CustomPacketPayload {
    public static final Type<RecordRecipe> TYPE = new Type<>(Content.id("record_recipe"));
    public static final StreamCodec<ByteBuf, RecordRecipe> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, RecordRecipe::recipe,
            ByteBufCodecs.BOOL, RecordRecipe::fill,
            RecordRecipe::new);


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

    static void handle(RecordRecipe payload, IPayloadContext context) {
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
