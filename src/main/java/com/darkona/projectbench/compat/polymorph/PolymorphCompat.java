package com.darkona.projectbench.compat.polymorph;

import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.client.ProjectBenchView;
import com.darkona.projectbench.client.RecipeSelector;
import com.darkona.projectbench.client.RecipeSelector.Choices;
import com.darkona.projectbench.config.ClientConfig;
import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

public final class PolymorphCompat {
    public static final String MOD_ID = "polymorph";

    private PolymorphCompat() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> {
            if (ClientConfig.polymorph()) {
                RecipeSelector.useExternal();
                event.enqueueWork(() -> PolymorphWidgets.getInstance()
                        .registerWidget(PolymorphCompat::widget));
            }
        });
    }

    @Nullable
    private static ChoiceRecipesWidget widget(AbstractContainerScreen<?> screen) {
        Choices choices = screen.getMenu() instanceof ProjectBenchMenu menu ? ProjectBenchView.choicesOf(menu) : null;
        return choices == null ? null : new ChoiceRecipesWidget(screen, choices);
    }
}
