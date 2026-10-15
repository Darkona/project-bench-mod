package com.darkona.projectbench.registry;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlock;
import com.darkona.projectbench.block.bench.AutoBenchBlock;
import com.darkona.projectbench.block.bench.BenchSlabBlock;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModBlocks {
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

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.5F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties wooden() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD);
    }
}
