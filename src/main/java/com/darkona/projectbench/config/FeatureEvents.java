package com.darkona.projectbench.config;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class FeatureEvents {
    private FeatureEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, FeatureEvents::onPlace);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, FeatureEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, FeatureEvents::onRightClickItem);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        BlockState placed = event.getPlacedBlock();
        if (!Feature.allows(placed.getBlock()) || !Feature.allows(placed.getBlock().asItem())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player player) {
                Feature.refuse(player, placed.getBlock());
            }
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!held.isEmpty() && !Feature.allows(held.getItem())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            Feature.refuse(event.getEntity(), held.getItem());
            return;
        }
        BlockState clicked = event.getLevel().getBlockState(event.getPos());
        if (!Feature.allows(clicked.getBlock())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            Feature.refuse(event.getEntity(), clicked.getBlock());
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack held = event.getItemStack();
        if (!held.isEmpty() && !Feature.allows(held.getItem())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            Feature.refuse(event.getEntity(), held.getItem());
        }
    }
}
