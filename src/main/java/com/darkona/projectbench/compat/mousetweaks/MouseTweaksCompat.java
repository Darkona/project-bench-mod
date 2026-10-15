package com.darkona.projectbench.compat.mousetweaks;

import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.client.ProjectBenchScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;

public final class MouseTweaksCompat {
    public static final String MOD_ID = "mousetweaks";

    private MouseTweaksCompat() {
    }

    public static MenuScreens.ScreenConstructor<ProjectBenchMenu, ProjectBenchScreen> screen() {
        LogUtils.getLogger().info("Mouse Tweaks found: the bench's window gives its wheel the bench's order");
        return MouseTweaksBenchScreen::new;
    }
}
