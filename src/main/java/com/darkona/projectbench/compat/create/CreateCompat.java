package com.darkona.projectbench.compat.create;

import com.darkona.projectbench.content.bench.HandsOff;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class CreateCompat {
    public static final String MOD_ID = "create";
    private static final ResourceLocation ITEM_VAULT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "item_vault");

    private CreateCompat() {
    }

    public static void register() {
        Block vault = BuiltInRegistries.BLOCK.getOptional(ITEM_VAULT).orElse(null);
        if (vault == null) {
            LogUtils.getLogger().warn("Create is loaded but has no {}: its vaults beside a bench are plain inventories", ITEM_VAULT);
            return;
        }
        HandsOff.register((level, pos, state) -> state.is(vault));
        LogUtils.getLogger().info("Create found: an Item Vault beside a bench shows its items but is hands-off in the window");
    }
}
