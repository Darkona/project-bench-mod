package com.darkona.projectbench;

import com.darkona.projectbench.block.archive.BlueprintArchiveBlock;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.block.archive.BlueprintArchiveMenu;
import com.darkona.projectbench.block.bench.AutoBenchBlock;
import com.darkona.projectbench.block.bench.BenchSlabBlock;
import com.darkona.projectbench.block.bench.BenchUpgrade;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.blueprint.BlueprintItem;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.Feature.Condition;
import com.mojang.serialization.MapCodec;
import dev.darkona.jdlib.api.pattern.Blueprint;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;

public final class Content {
    private Content() {
    }

    public static void register(IEventBus modBus) {
        Blocks.BLOCKS.register(modBus);
        Items.ITEMS.register(modBus);
        BlockEntities.BLOCK_ENTITIES.register(modBus);
        Menus.MENUS.register(modBus);
        Components.COMPONENTS.register(modBus);
        Tab.TABS.register(modBus);
        Conditions.CONDITIONS.register(modBus);
        Recipes.SERIALIZERS.register(modBus);
    }

    public static final class Blocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ProjectBench.MOD_ID);

        public static final DeferredBlock<ProjectBenchBlock> PROJECT_BENCH = BLOCKS.register("project_bench", () -> new ProjectBenchBlock(stone()));

        public static final DeferredBlock<ProjectBenchBlock> SLAB_PROJECT_BENCH = BLOCKS.register("slab_project_bench", () -> new BenchSlabBlock(stone().noOcclusion()));

        public static final DeferredBlock<ProjectBenchBlock> AUTO_PROJECT_BENCH = BLOCKS.register("auto_project_bench",
                () -> new AutoBenchBlock(stone(), false));

        public static final DeferredBlock<ProjectBenchBlock> EJECTING_AUTO_PROJECT_BENCH = BLOCKS.register("ejecting_auto_project_bench",
                () -> new AutoBenchBlock(stone(), true));

        public static final List<DeferredBlock<ProjectBenchBlock>> BENCHES = List.of(PROJECT_BENCH, SLAB_PROJECT_BENCH, AUTO_PROJECT_BENCH,
                EJECTING_AUTO_PROJECT_BENCH);

        public static final DeferredBlock<BlueprintArchiveBlock> BLUEPRINT_ARCHIVE = BLOCKS.register("blueprint_archive", () -> new BlueprintArchiveBlock(wooden()));


        private static BlockBehaviour.Properties stone() {
            return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.5F).sound(SoundType.STONE);
        }

        private static BlockBehaviour.Properties wooden() {
            return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD);
        }
    }

    public static final class Items {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ProjectBench.MOD_ID);

        public static final DeferredItem<BlockItem> PROJECT_BENCH = ITEMS.registerSimpleBlockItem(Blocks.PROJECT_BENCH, new Item.Properties());

        public static final DeferredItem<BlockItem> SLAB_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(Blocks.SLAB_PROJECT_BENCH, new Item.Properties());

        public static final DeferredItem<BlockItem> AUTO_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(Blocks.AUTO_PROJECT_BENCH, new Item.Properties());

        public static final DeferredItem<BlockItem> EJECTING_AUTO_PROJECT_BENCH = ITEMS.registerSimpleBlockItem(Blocks.EJECTING_AUTO_PROJECT_BENCH,
                new Item.Properties());

        public static final DeferredItem<BlueprintItem> BLUEPRINT = ITEMS.register("blueprint", () -> new BlueprintItem(new Item.Properties()));

        public static final DeferredItem<BlockItem> BLUEPRINT_ARCHIVE = ITEMS.registerSimpleBlockItem(Blocks.BLUEPRINT_ARCHIVE, new Item.Properties());
    }

    public static final class BlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ProjectBench.MOD_ID);

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProjectBenchBlockEntity>> PROJECT_BENCH = BLOCK_ENTITIES.register("project_bench",
                () -> BlockEntityType.Builder.of(ProjectBenchBlockEntity::new, Blocks.BENCHES.stream().map(DeferredBlock::get).toArray(Block[]::new))
                        .build(null));

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintArchiveBlockEntity>> BLUEPRINT_ARCHIVE = BLOCK_ENTITIES.register("blueprint_archive",
                () -> BlockEntityType.Builder.of(BlueprintArchiveBlockEntity::new, Blocks.BLUEPRINT_ARCHIVE.get()).build(null));
    }

    public static final class Menus {
        public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ProjectBench.MOD_ID);

        public static final DeferredHolder<MenuType<?>, MenuType<ProjectBenchMenu>> PROJECT_BENCH = MENUS.register("project_bench",
                () -> IMenuTypeExtension.create(ProjectBenchMenu::new));

        public static final DeferredHolder<MenuType<?>, MenuType<BlueprintArchiveMenu>> BLUEPRINT_ARCHIVE = MENUS.register("blueprint_archive",
                () -> IMenuTypeExtension.create(BlueprintArchiveMenu::new));
    }

    public static final class Components {
        public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ProjectBench.MOD_ID);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Blueprint>> BLUEPRINT = COMPONENTS.register("blueprint",
                () -> DataComponentType.<Blueprint>builder().persistent(Blueprint.CODEC).networkSynchronized(Blueprint.STREAM_CODEC).cacheEncoding().build());

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> CONTENTS = COMPONENTS.register("contents",
                () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build());

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SLOT_LOCKS = COMPONENTS.register("slot_locks",
                () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build());
    }

    public static final class Tab {
        public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProjectBench.MOD_ID);

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("projectbench", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.projectbench"))
                .icon(() -> new ItemStack(Items.PROJECT_BENCH.get()))
                .displayItems((parameters, output) -> {
                    for (Feature f : Feature.values()) {
                        if (f.enabled()) {
                            output.accept(f.item());
                        }
                    }
                })
                .build());
    }

    public static final class Conditions {
        public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ProjectBench.MOD_ID);

        public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<Condition>> FEATURE = CONDITIONS.register("feature", () -> Condition.CODEC);
    }

    public static final class Recipes {
        public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ProjectBench.MOD_ID);

        public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BenchUpgrade.Shaped>> SHAPED_UPGRADE =
                SERIALIZERS.register("bench_upgrade_shaped", () -> BenchUpgrade.Shaped.SERIALIZER);

        public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BenchUpgrade.Shapeless>> SHAPELESS_UPGRADE =
                SERIALIZERS.register("bench_upgrade_shapeless", () -> BenchUpgrade.Shapeless.SERIALIZER);
    }
}
