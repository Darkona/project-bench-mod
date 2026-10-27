package com.darkona.projectbench.index;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.Feature.Condition;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class BenchConditions {
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ProjectBench.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<Condition>> FEATURE = CONDITIONS.register("feature", () -> Condition.CODEC);

    private BenchConditions() {
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
