package com.darkona.projectbench.registry;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.archive.BlueprintArchiveMenu;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ProjectBench.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ProjectBenchMenu>> PROJECT_BENCH = MENUS.register("project_bench",
            () -> IMenuTypeExtension.create(ProjectBenchMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BlueprintArchiveMenu>> BLUEPRINT_ARCHIVE = MENUS.register("blueprint_archive",
            () -> IMenuTypeExtension.create(BlueprintArchiveMenu::new));

    private ModMenus() {
    }
}
