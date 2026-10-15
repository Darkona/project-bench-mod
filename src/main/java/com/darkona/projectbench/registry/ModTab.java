package com.darkona.projectbench.registry;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.Feature;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProjectBench.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("projectbench", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.projectbench"))
            .icon(() -> new ItemStack(ModItems.PROJECT_BENCH.get()))
            .displayItems((parameters, output) -> {
                for (Feature f : Feature.values()) {
                    if (f.enabled()) {
                        output.accept(f.item());
                    }
                }
            })
            .build());

    private ModTab() {
    }
}
