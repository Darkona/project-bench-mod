package com.darkona.projectbench.registry;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.FeatureCondition;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ProjectBench.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<FeatureCondition>> FEATURE = CONDITIONS.register("feature", () -> FeatureCondition.CODEC);

    private ModConditions() {
    }
}
