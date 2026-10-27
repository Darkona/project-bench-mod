package com.darkona.projectbench.index;

import com.darkona.projectbench.ProjectBench;
import dev.darkona.jdlib.api.pattern.Blueprint;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BenchDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ProjectBench.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Blueprint>> BLUEPRINT = COMPONENTS.register("blueprint",
            () -> DataComponentType.<Blueprint>builder().persistent(Blueprint.CODEC).networkSynchronized(Blueprint.STREAM_CODEC).cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> CONTENTS = containerContents("contents");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SLOT_LOCKS = containerContents("slot_locks");

    private BenchDataComponents() {
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> containerContents(String name) {
        return COMPONENTS.register(name,
                () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build());
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
