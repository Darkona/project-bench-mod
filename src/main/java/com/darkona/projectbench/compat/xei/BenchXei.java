package com.darkona.projectbench.compat.xei;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.client.ProjectBenchView;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.block.bench.RecordRecipe;
import dev.darkona.jdlib.api.ui.Rect;
import dev.darkona.jdlib.api.xei.CraftingTransfer;
import dev.darkona.jdlib.api.xei.JDXei;
import dev.darkona.jdlib.api.xei.RecipeKind;
import dev.darkona.jdlib.api.xei.Viewer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BenchXei {
    private static final Rect ARROW = new Rect(ProjectBenchView.ARROW_X, ProjectBenchView.ARROW_Y, ProjectBenchView.ARROW_WIDTH, ProjectBenchView.ARROW_HEIGHT);
    private static final Rect STATION_ARROW = new Rect(ProjectBenchMenu.halfX(1, ProjectBenchView.ARROW_X, ProjectBenchView.ARROW_WIDTH),
            ProjectBenchView.ARROW_Y, ProjectBenchView.ARROW_WIDTH, ProjectBenchView.ARROW_HEIGHT);
    private static final List<Rect> ONE = List.of(ARROW);
    private static final List<Rect> BOTH = List.of(ARROW, STATION_ARROW);

    private BenchXei() {
    }

    public static void register() {
        JDXei.registerCrafting(Content.Menus.PROJECT_BENCH, ProjectBenchMenu.class, new Transfer());
        JDXei.recipeClickArea(Content.Menus.PROJECT_BENCH, RecipeKind.CRAFTING, menu -> !ClientConfig.jei() ? List.of() : menu.station() ? BOTH : ONE);
    }

    private static final class Transfer implements CraftingTransfer<ProjectBenchMenu> {
        @Override
        public List<Slot> recipeSlots(ProjectBenchMenu menu) {
            return menu.activeMatrix();
        }

        @Override
        public List<Slot> inventorySlots(ProjectBenchMenu menu) {
            return menu.stockSlots();
        }

        @Nullable
        @Override
        public Slot outputSlot(ProjectBenchMenu menu) {
            return menu.getSlot(menu.resultIndex(menu.active()));
        }

        @Override
        public boolean enabled(Viewer viewer) {
            return viewer == Viewer.JEI ? ClientConfig.jei() : ClientConfig.emi();
        }

        @Override
        public boolean claims(ProjectBenchMenu menu, Player player, ResourceLocation recipeId) {
            return menu.canRecordRecipe() && RecordRecipe.recordable(player.level().getRecipeManager(), recipeId) != null;
        }

        @Override
        public void claimed(ProjectBenchMenu menu, Player player, ResourceLocation recipeId, boolean fits) {
            PacketDistributor.sendToServer(new RecordRecipe(recipeId, !fits));
        }
    }
}
