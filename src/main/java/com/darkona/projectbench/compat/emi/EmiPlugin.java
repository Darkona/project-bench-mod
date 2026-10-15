package com.darkona.projectbench.compat.emi;

import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.client.ProjectBenchScreen;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.network.RecordRecipe;
import com.darkona.projectbench.registry.ModBlocks;
import com.darkona.projectbench.registry.ModMenus;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public final class EmiPlugin implements dev.emi.emi.api.EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        if (!ClientConfig.emi()) {
            return;
        }
        for (DeferredBlock<ProjectBenchBlock> bench : ModBlocks.BENCHES) {
            registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(bench.get()));
        }
        registry.addRecipeHandler(ModMenus.PROJECT_BENCH.get(), new BenchHandler());
        registry.addGenericExclusionArea((screen, out) -> {
            if (screen instanceof ProjectBenchScreen bench) {
                for (Rect2i area : bench.extraAreas()) {
                    out.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        });
    }

    private static final class BenchHandler implements StandardRecipeHandler<ProjectBenchMenu> {
        @Override
        public List<Slot> getInputSources(ProjectBenchMenu menu) {
            List<Slot> list = new ArrayList<>(menu.activeMatrix());
            list.addAll(menu.stockSlots());
            return list;
        }

        @Override
        public List<Slot> getCraftingSlots(ProjectBenchMenu menu) {
            return menu.activeMatrix();
        }

        @Nullable
        @Override
        public Slot getOutputSlot(ProjectBenchMenu menu) {
            return menu.getSlot(menu.resultIndex(menu.active()));
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
        }

        @Override
        public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ProjectBenchMenu> context) {
            return recordable(recipe, context.getScreenHandler()) || StandardRecipeHandler.super.canCraft(recipe, context);
        }

        @Override
        public boolean craft(EmiRecipe recipe, EmiCraftContext<ProjectBenchMenu> context) {
            if (!recordable(recipe, context.getScreenHandler())) {
                return StandardRecipeHandler.super.craft(recipe, context);
            }
            boolean whole = StandardRecipeHandler.super.canCraft(recipe, context);
            PacketDistributor.sendToServer(new RecordRecipe(recipe.getId(), !whole));
            if (whole) {
                return StandardRecipeHandler.super.craft(recipe, context);
            }
            Minecraft.getInstance().setScreen(context.getScreen());
            return true;
        }

        private static boolean recordable(EmiRecipe recipe, ProjectBenchMenu menu) {
            ClientLevel level = Minecraft.getInstance().level;
            ResourceLocation id = recipe.getId();
            return level != null && id != null && menu.canRecordRecipe() && RecordRecipe.recordable(level.getRecipeManager(), id) != null;
        }
    }
}
