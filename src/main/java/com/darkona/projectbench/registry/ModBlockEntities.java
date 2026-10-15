package com.darkona.projectbench.registry;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ProjectBench.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProjectBenchBlockEntity>> PROJECT_BENCH = BLOCK_ENTITIES.register("project_bench",
            () -> BlockEntityType.Builder.of(ProjectBenchBlockEntity::new, ModBlocks.BENCHES.stream().map(DeferredBlock::get).toArray(Block[]::new))
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintArchiveBlockEntity>> BLUEPRINT_ARCHIVE = BLOCK_ENTITIES.register("blueprint_archive",
            () -> BlockEntityType.Builder.of(BlueprintArchiveBlockEntity::new, ModBlocks.BLUEPRINT_ARCHIVE.get()).build(null));

    private ModBlockEntities() {
    }
}
