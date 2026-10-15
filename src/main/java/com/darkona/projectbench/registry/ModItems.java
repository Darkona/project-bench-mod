package com.darkona.projectbench.registry;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.blueprint.BlueprintItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ProjectBench.MOD_ID);

    public static final DeferredItem<BlockItem> PROJECT_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.PROJECT_BENCH, new Item.Properties());

    public static final DeferredItem<BlockItem> SLAB_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.SLAB_PROJECT_BENCH, new Item.Properties());

    public static final DeferredItem<BlockItem> AUTO_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.AUTO_PROJECT_BENCH, new Item.Properties());

    public static final DeferredItem<BlockItem> EJECTING_AUTO_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.EJECTING_AUTO_PROJECT_BENCH,
            new Item.Properties());

    public static final DeferredItem<BlueprintItem> BLUEPRINT = ITEMS.register("blueprint", () -> new BlueprintItem(new Item.Properties()));

    public static final DeferredItem<BlockItem> BLUEPRINT_ARCHIVE = ITEMS.registerSimpleBlockItem(ModBlocks.BLUEPRINT_ARCHIVE, new Item.Properties());

    private ModItems() {
    }
}
