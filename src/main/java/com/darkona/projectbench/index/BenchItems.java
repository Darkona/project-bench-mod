package com.darkona.projectbench.index;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.content.blueprint.BlueprintItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BenchItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ProjectBench.MOD_ID);

    public static final DeferredItem<BlockItem> PROJECT_BENCH = ITEMS.registerSimpleBlockItem(BenchBlocks.PROJECT_BENCH);

    public static final DeferredItem<BlockItem> SLAB_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(BenchBlocks.SLAB_PROJECT_BENCH);

    public static final DeferredItem<BlockItem> AUTO_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(BenchBlocks.AUTO_PROJECT_BENCH);

    public static final DeferredItem<BlockItem> EJECTING_AUTO_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(BenchBlocks.EJECTING_AUTO_PROJECT_BENCH);

    public static final DeferredItem<BlueprintItem> BLUEPRINT = ITEMS.register("blueprint", () -> new BlueprintItem(new Item.Properties()));

    public static final DeferredItem<BlockItem> BLUEPRINT_ARCHIVE = ITEMS.registerSimpleBlockItem(BenchBlocks.BLUEPRINT_ARCHIVE);

    private BenchItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
