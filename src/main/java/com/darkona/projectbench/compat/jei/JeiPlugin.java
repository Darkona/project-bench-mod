package com.darkona.projectbench.compat.jei;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.ProjectBenchBlock;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.client.ProjectBenchScreen;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.network.RecordRecipe;
import com.darkona.projectbench.registry.ModBlocks;
import com.darkona.projectbench.registry.ModMenus;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.transfer.RecipeTransferResult;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@mezz.jei.api.JeiPlugin
public final class JeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ProjectBench.id("jei");

    @Nullable
    private static IJeiRuntime runtime;

    private static final List<ItemStack> HIDDEN = new ArrayList<>();

    private static final Runnable FEATURES_CHANGED = () -> Minecraft.getInstance().execute(JeiPlugin::applyFeatures);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ClientConfig.jei()) {
            for (DeferredBlock<ProjectBenchBlock> bench : ModBlocks.BENCHES) {
                registration.addRecipeCatalyst(bench.get(), RecipeTypes.CRAFTING);
            }
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        if (ClientConfig.jei()) {
            BenchTransfer info = new BenchTransfer();
            registration.addRecipeTransferHandler(new RecordingTransfer(info,
                    registration.getTransferHelper().createUnregisteredRecipeTransferHandler(info)), RecipeTypes.CRAFTING);
        }
    }

    private static final class BenchTransfer implements IRecipeTransferInfo<ProjectBenchMenu, RecipeHolder<CraftingRecipe>> {
        @Override
        public Class<? extends ProjectBenchMenu> getContainerClass() {
            return ProjectBenchMenu.class;
        }

        @Override
        public Optional<MenuType<ProjectBenchMenu>> getMenuType() {
            return Optional.of(ModMenus.PROJECT_BENCH.get());
        }

        @Override
        public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public boolean canHandle(ProjectBenchMenu menu, RecipeHolder<CraftingRecipe> recipe) {
            return true;
        }

        @Override
        public List<Slot> getRecipeSlots(ProjectBenchMenu menu, RecipeHolder<CraftingRecipe> recipe) {
            return menu.activeMatrix();
        }

        @Override
        public List<Slot> getInventorySlots(ProjectBenchMenu menu, RecipeHolder<CraftingRecipe> recipe) {
            return menu.stockSlots();
        }
    }

    private record RecordingTransfer(BenchTransfer info, IRecipeTransferHandler<ProjectBenchMenu, RecipeHolder<CraftingRecipe>> basic)
            implements IRecipeTransferHandler<ProjectBenchMenu, RecipeHolder<CraftingRecipe>> {
        @Override
        public Class<? extends ProjectBenchMenu> getContainerClass() {
            return info.getContainerClass();
        }

        @Override
        public Optional<MenuType<ProjectBenchMenu>> getMenuType() {
            return info.getMenuType();
        }

        @Override
        public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return info.getRecipeType();
        }

        @Nullable
        @Override
        public IRecipeTransferError transferRecipe(IRecipeTransferContext<RecipeHolder<CraftingRecipe>, ProjectBenchMenu> context, boolean doTransfer) {
            RecipeHolder<CraftingRecipe> recipe = context.getRecipe();
            if (!recordable(context.getContainer(), context.getPlayer(), recipe)) {
                return basic.transferRecipe(context, doTransfer);
            }
            boolean whole = basic.transferRecipe(context, false) == null;
            if (doTransfer) {
                PacketDistributor.sendToServer(new RecordRecipe(recipe.id(), !whole));
                if (whole) {
                    return basic.transferRecipe(context, true);
                }
                context.completeRecipeTransfer(RecipeTransferResult.SUCCESS);
            }
            return null;
        }

        @SuppressWarnings("removal")
        @Nullable
        @Override
        public IRecipeTransferError transferRecipe(ProjectBenchMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots, Player player,
                boolean maxTransfer, boolean doTransfer) {
            if (!recordable(menu, player, recipe)) {
                return basic.transferRecipe(menu, recipe, slots, player, maxTransfer, doTransfer);
            }
            boolean whole = basic.transferRecipe(menu, recipe, slots, player, maxTransfer, false) == null;
            if (doTransfer) {
                PacketDistributor.sendToServer(new RecordRecipe(recipe.id(), !whole));
                if (whole) {
                    return basic.transferRecipe(menu, recipe, slots, player, maxTransfer, true);
                }
            }
            return null;
        }

        private static boolean recordable(ProjectBenchMenu menu, Player player, RecipeHolder<CraftingRecipe> recipe) {
            return menu.canRecordRecipe() && RecordRecipe.recordable(player.level().getRecipeManager(), recipe.id()) != null;
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!ClientConfig.jei()) {
            return;
        }
        registration.addRecipeClickArea(ProjectBenchScreen.class, ProjectBenchScreen.ARROW_X, ProjectBenchScreen.ARROW_Y,
                ProjectBenchScreen.ARROW_WIDTH, ProjectBenchScreen.ARROW_HEIGHT, RecipeTypes.CRAFTING);
        registration.addGuiContainerHandler(ProjectBenchScreen.class, new IGuiContainerHandler<ProjectBenchScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(ProjectBenchScreen screen) {
                return screen.extraAreas();
            }

            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(ProjectBenchScreen screen, double guiMouseX, double guiMouseY) {
                return !screen.getMenu().station() ? List.of() : List.of(IGuiClickableArea.createBasic(
                        ProjectBenchMenu.halfX(1, ProjectBenchScreen.ARROW_X, ProjectBenchScreen.ARROW_WIDTH),
                        ProjectBenchScreen.ARROW_Y, ProjectBenchScreen.ARROW_WIDTH, ProjectBenchScreen.ARROW_HEIGHT, RecipeTypes.CRAFTING));
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;

        HIDDEN.clear();
        if (ClientConfig.jei()) {
            Feature.listen(FEATURES_CHANGED);
            applyFeatures();
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        Feature.unlisten(FEATURES_CHANGED);
        runtime = null;
        HIDDEN.clear();
    }

    private static void applyFeatures() {
        IJeiRuntime r = runtime;
        if (r == null) {
            return;
        }
        if (!HIDDEN.isEmpty()) {
            r.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.copyOf(HIDDEN));
            HIDDEN.clear();
        }
        for (Feature f : Feature.values()) {
            if (!f.enabled()) {
                HIDDEN.add(new ItemStack(f.item()));
            }
        }
        if (!HIDDEN.isEmpty()) {
            r.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.copyOf(HIDDEN));
        }
    }
}
