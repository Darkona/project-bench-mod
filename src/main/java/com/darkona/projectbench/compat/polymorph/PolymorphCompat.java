package com.darkona.projectbench.compat.polymorph;

import com.darkona.projectbench.client.RecipeChoices;
import com.darkona.projectbench.client.RecipeSelector;
import com.darkona.projectbench.config.ClientConfig;
import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class PolymorphCompat {
    public static final String MOD_ID = "polymorph";

    private PolymorphCompat() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> {
            if (ClientConfig.polymorph()) {
                RecipeSelector.useExternal();
                event.enqueueWork(() -> PolymorphWidgets.getInstance()
                        .registerWidget(screen -> screen instanceof RecipeChoices choices ? new ChoiceRecipesWidget(screen, choices) : null));
            }
        });
    }
}
