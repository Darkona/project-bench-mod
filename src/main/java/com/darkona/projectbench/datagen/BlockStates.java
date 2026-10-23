package com.darkona.projectbench.datagen;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.BenchBlock;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlock;
import com.darkona.projectbench.block.archive.BlueprintArchiveBlock.ColumnPart;
import com.darkona.projectbench.block.bench.AutoBenchBlock;
import com.darkona.projectbench.block.bench.BenchSlabBlock;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.registry.ModBlocks;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class BlockStates extends BlockStateProvider {
    BlockStates(PackOutput out, ExistingFileHelper files) {
        super(out, ProjectBench.MOD_ID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        ResourceLocation bottom = modLoc("block/project_bench_bottom");
        ModelFile bench = models().orientableWithBottom("project_bench", modLoc("block/project_bench_side"),
                modLoc("block/project_bench_front"), bottom, modLoc("block/project_bench_top"));
        getVariantBuilder(ModBlocks.PROJECT_BENCH.get()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(bench)
                .rotationY(((int) state.getValue(BenchBlock.FACING).toYRot() + 180) % 360).build(), ProjectBenchBlock.TYPE);
        simpleBlockItem(ModBlocks.PROJECT_BENCH.get(), bench);

        slab();
        automatic(ModBlocks.AUTO_PROJECT_BENCH.get(), "auto_project_bench", false);
        automatic(ModBlocks.EJECTING_AUTO_PROJECT_BENCH.get(), "ejecting_auto_project_bench", true);

        Map<ColumnPart, ModelFile> parts = new EnumMap<>(ColumnPart.class);
        for (ColumnPart part : ColumnPart.values()) {
            String suffix = part == ColumnPart.SINGLE ? "" : "_" + part.getSerializedName();

            parts.put(part, models().orientableWithBottom("blueprint_archive" + suffix, modLoc("block/blueprint_archive_side"),
                    modLoc("block/blueprint_archive_front" + suffix), mcLoc("block/oak_planks"), mcLoc("block/chiseled_bookshelf_top")));
        }
        horizontalBlock(ModBlocks.BLUEPRINT_ARCHIVE.get(), state -> parts.get(state.getValue(BlueprintArchiveBlock.PART)));
        simpleBlockItem(ModBlocks.BLUEPRINT_ARCHIVE.get(), parts.get(ColumnPart.SINGLE));
    }

    private void slab() {
        ModelFile board = slabModel("slab_project_bench_board", true, false, false, false, false);
        MultiPartBlockStateBuilder states = getMultipartBuilder(ModBlocks.SLAB_PROJECT_BENCH.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            states.part().modelFile(board).rotationY(((int) facing.toYRot() + 180) % 360).addModel().condition(BenchBlock.FACING, facing).end();
        }
        for (Direction ns : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            for (Direction we : new Direction[]{Direction.WEST, Direction.EAST}) {
                ModelFile leg = slabModel("slab_project_bench_leg_" + ns.getSerializedName() + "_" + we.getSerializedName(), false,
                        ns == Direction.NORTH && we == Direction.WEST, ns == Direction.NORTH && we == Direction.EAST,
                        ns == Direction.SOUTH && we == Direction.WEST, ns == Direction.SOUTH && we == Direction.EAST);
                states.part().modelFile(leg).addModel().condition(BenchSlabBlock.legs(ns), true).condition(BenchSlabBlock.legs(we), true).end();
            }
        }
        simpleBlockItem(ModBlocks.SLAB_PROJECT_BENCH.get(), slabModel("slab_project_bench", true, true, true, true, true));
    }

    private void automatic(Block block, String name, boolean ejecting) {
        ResourceLocation side = modLoc("block/auto_project_bench_side");
        ModelFile model = models().cube(name, modLoc("block/auto_project_bench_bottom"), modLoc("block/project_bench_top"),
                modLoc("block/auto_project_bench_front"), modLoc("block/" + (ejecting ? "ejecting_auto_project_bench_back" : "auto_project_bench_back")),
                side, side).texture("particle", side);
        getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(model)
                .rotationY(((int) state.getValue(BenchBlock.FACING).toYRot() + 180) % 360).build(), ProjectBenchBlock.TYPE, AutoBenchBlock.TRIGGERED);
        simpleBlockItem(block, model);
    }

    private ModelFile slabModel(String name, boolean withBoard, boolean northWest, boolean northEast, boolean southWest, boolean southEast) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("particle", modLoc("block/project_bench_side"))
                .texture("side", modLoc("block/project_bench_side"))
                .texture("bottom", modLoc("block/project_bench_bottom"));
        if (withBoard) {
            model.texture("front", modLoc("block/project_bench_front")).texture("top", modLoc("block/project_bench_top"));
            model.element().from(0, 8, 0).to(16, 16, 16).allFaces((dir, face) -> {
                face.texture(dir == Direction.UP ? "#top" : dir == Direction.DOWN ? "#bottom" : dir == Direction.NORTH ? "#front" : "#side");
                if (dir != Direction.DOWN) {
                    face.cullface(dir);
                }
            }).end();
        }
        boolean[] corners = {northWest, northEast, southWest, southEast};
        for (int i = 0; i < corners.length; i++) {
            if (corners[i]) {
                int x = (i & 1) * 14;
                int z = (i >> 1) * 14;
                model.element().from(x, 0, z).to(x + 2, 8, z + 2).allFacesExcept((dir, face) -> {
                    face.texture(dir == Direction.DOWN ? "#bottom" : "#side");
                    if (dir == Direction.DOWN || dir == Direction.WEST && x == 0 || dir == Direction.EAST && x == 14
                            || dir == Direction.NORTH && z == 0 || dir == Direction.SOUTH && z == 14) {
                        face.cullface(dir);
                    }
                }, Set.of(Direction.UP)).end();
            }
        }
        return model;
    }
}
