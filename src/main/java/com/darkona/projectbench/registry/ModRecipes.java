package com.darkona.projectbench.registry;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.BenchUpgrade;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ProjectBench.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BenchUpgrade.Shaped>> SHAPED_UPGRADE =
            SERIALIZERS.register("bench_upgrade_shaped", () -> BenchUpgrade.Shaped.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BenchUpgrade.Shapeless>> SHAPELESS_UPGRADE =
            SERIALIZERS.register("bench_upgrade_shapeless", () -> BenchUpgrade.Shapeless.SERIALIZER);

    private ModRecipes() {
    }
}
