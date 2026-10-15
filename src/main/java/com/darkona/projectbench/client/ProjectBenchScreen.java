package com.darkona.projectbench.client;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.block.bench.BenchStacks;
import com.darkona.projectbench.block.bench.ExternalInventories;
import com.darkona.projectbench.block.bench.MatrixLayout;
import com.darkona.projectbench.block.bench.Processor;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import com.darkona.projectbench.blueprint.Blueprint;
import com.darkona.projectbench.blueprint.CostContainer;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.ShareMode;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.HolderLookup;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProjectBenchScreen extends AbstractContainerScreen<ProjectBenchMenu> implements RecipeChoices {
    private static final ResourceLocation TEXTURE = ProjectBench.id("textures/gui/project_bench.png");
    private static final ResourceLocation STATION_TEXTURE = ProjectBench.id("textures/gui/project_station.png");

    public static final KeyMapping SWITCH_MATRIX = new KeyMapping("key.projectbench.switch_matrix", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, "key.categories.projectbench");

    private static final int ICON = 10;
    private static final int SLOT_SIZE = Draw.SLOT_SIZE;

    public static final int ARROW_X = 110;
    public static final int ARROW_Y = 37;
    public static final int ARROW_WIDTH = 24;
    public static final int ARROW_HEIGHT = 15;
    private static final int BUTTON = 14;
    private static final int GAP = 2;
    private static final int RECORD_X = 10;
    private static final int RECORD_Y = 60;
    private static final int STATION_RECORD_X = (ProjectBenchMenu.STATION_WIDTH - 2 * BUTTON - GAP) / 2;
    private static final int BAR = BUTTON + GAP;
    private static final int AUTOCRAFT_U = 196;
    private static final int FRAME = 3 * SLOT_SIZE + 4;
    private static final int FRAME_U = 176;
    private static final int FRAME_V = 32;
    private static final int ACTIVE_U = 176;
    private static final int ACTIVE_V = FRAME_V + FRAME + 2;
    private static final int GHOST_COVER = 0x30FFFFFF;
    private static final int LOCK_EDGE = 0xC0F0A050;
    private static final int PADLOCK = 6;
    private static final int PADLOCK_U = 176;
    private static final int PADLOCK_V = 104;
    private static final int PANEL_COLUMNS = 9;
    private static final int PANEL_ROWS = 12;
    private static final int PANEL_WIDTH = 7 + PANEL_COLUMNS * SLOT_SIZE + 7;
    private static final int PANEL_OVERLAP = 3;
    private static final int OUTSIDE_COLUMNS = ProjectBenchMenu.OUTSIDE_COLUMNS;
    private static final int OUTSIDE_ROWS = ProjectBenchMenu.OUTSIDE_ROWS;

    private static final int OUTSIDE_SLOTS_DX = 8;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_SPRITE_HEIGHT = 32;
    private static final ResourceLocation TAB_ON = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_2");
    private static final ResourceLocation TAB_OFF = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2");

    private static final int TAB_STRIP = 28;
    private static final int MACHINE_U = 7;
    private static final int MACHINE_V = 12;
    private static final int MACHINE_WIDTH = 162;
    private static final int MACHINE_HEIGHT = 68;
    private static final int PROCESSOR_HEIGHT = MACHINE_V + MACHINE_HEIGHT + 4;
    private static final ResourceLocation BREWING_FUEL = ResourceLocation.withDefaultNamespace("container/brewing_stand/fuel_length");
    private static final ResourceLocation BREWING_PROGRESS = ResourceLocation.withDefaultNamespace("container/brewing_stand/brew_progress");
    private static final ResourceLocation BREWING_BUBBLES = ResourceLocation.withDefaultNamespace("container/brewing_stand/bubbles");
    private static final int[] BUBBLES = {29, 24, 20, 16, 11, 6, 0};
    private static final ResourceLocation[] MACHINE_TEXTURE = new ResourceLocation[Processor.values().length];
    private static final ResourceLocation[] LIT_PROGRESS = new ResourceLocation[Processor.values().length];
    private static final ResourceLocation[] BURN_PROGRESS = new ResourceLocation[Processor.values().length];

    static {
        for (Processor processor : Processor.values()) {
            String name = processor.name().toLowerCase(Locale.ROOT);
            MACHINE_TEXTURE[processor.ordinal()] = ResourceLocation.withDefaultNamespace("textures/gui/container/" + name + ".png");
            LIT_PROGRESS[processor.ordinal()] = ResourceLocation.withDefaultNamespace("container/" + name + "/lit_progress");
            BURN_PROGRESS[processor.ordinal()] = ResourceLocation.withDefaultNamespace("container/" + name + "/burn_progress");
        }
    }
    private static final int OUTSIDE_WIDTH = 7 + OUTSIDE_COLUMNS * SLOT_SIZE + 7;
    private static final int PANEL_TITLE_X = 7;
    private static final int PANEL_TITLE_Y = 6;
    private static final int PANEL_SLOTS_X = 7;
    private static final int PANEL_SLOTS_Y = 17;
    private static final int RECORDED = 0xFF3A5F8F;
    private static final int HOVER = 0x80FFFFFF;
    private static final int LOCKED = 0x80C03030;
    private static final int TEXT = 0xFF404040;
    private static final int SLOT_LIGHTEN = 40;

    private final Component panelTitle = Component.translatable("gui.projectbench.project_bench.archives");
    private final List<OutsidePanel> outsideLayout = new ArrayList<>(2);
    private final List<Rect2i> areas = new ArrayList<>(12);
    private final List<Rect2i> areasView = Collections.unmodifiableList(areas);
    private boolean areasStale = true;
    private int areasPanel;
    @Nullable
    private OutsidePanel areasChosen;
    private IconButton record;
    private IconButton fill;
    private IconButton share;
    private IconButton showcase;
    @Nullable
    private IconButton activeButton;
    private final IconButton[] auto;
    @Nullable
    private IconButton autocraft;
    private final List<IconButton> bar = new ArrayList<>(10);
    private final IconButton[][] matrixBar;
    private IconButton[] benchBar;

    @Nullable
    private Item shownCost;
    private int shownCostCount = -1;
    private int shownMissing = -1;
    private ItemStack costGhost = ItemStack.EMPTY;

    private final Matrix[] matrices;
    private final RecipeSelector[] selectors;

    private int scroll;

    public ProjectBenchScreen(ProjectBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        boolean station = menu.station();
        imageWidth = station ? ProjectBenchMenu.STATION_WIDTH : ProjectBenchMenu.WIDTH;
        imageHeight = station ? ProjectBenchMenu.STATION_HEIGHT : 222;
        inventoryLabelX = station ? ProjectBenchMenu.STATION_STORAGE_X : 8;
        inventoryLabelY = (station ? ProjectBenchMenu.STATION_PLAYER_Y : ProjectBenchMenu.PLAYER_Y) - 11;
        int halves = station ? 2 : 1;
        matrices = new Matrix[halves];
        selectors = new RecipeSelector[halves];
        auto = new IconButton[halves];
        matrixBar = new IconButton[halves][];
        for (int which = 0; which < halves; ++which) {
            matrices[which] = new Matrix(which);
            int x = ProjectBenchMenu.RESULT_X;
            selectors[which] = RecipeSelector.forWindow(matrices[which], ProjectBenchMenu.halfX(which, x + 3, RecipeSelector.BUTTON),
                    ProjectBenchMenu.RESULT_Y + 42, ProjectBenchMenu.halfX(which, x + 8, 0));
        }
    }

    private static int matrixX(int which) {
        return ProjectBenchMenu.halfX(which, ProjectBenchMenu.MATRIX_X, ProjectBenchMenu.MATRIX_WIDTH);
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(topPos, Math.min(BAR, height - imageHeight));
        boolean station = menu.station();
        bar.clear();
        record = addRenderableWidget(new IconButton(leftPos + (station ? STATION_RECORD_X : RECORD_X), topPos + RECORD_Y, BUTTON, BUTTON,
                Component.translatable("gui.projectbench.project_bench.record"),
                Component.translatable("gui.projectbench.project_bench.record_help"),
                TEXTURE, 176, 0, ICON, b -> press(ProjectBenchMenu.BUTTON_RECORD)));
        fill = addRenderableWidget(new IconButton(leftPos + (station ? STATION_RECORD_X : RECORD_X) + BUTTON + GAP, topPos + RECORD_Y, BUTTON, BUTTON,
                Component.translatable("gui.projectbench.project_bench.fill"),
                Component.translatable("gui.projectbench.project_bench.fill_help"),
                TEXTURE, 176 + 3 * ICON, 0, ICON, b -> press(ProjectBenchMenu.BUTTON_FILL)));
        autocraft = station ? null : barButton(Component.translatable("gui.projectbench.project_bench.autocraft_on"),
                Component.translatable("gui.projectbench.project_bench.autocraft_help"), AUTOCRAFT_U, ACTIVE_V, ProjectBenchMenu.BUTTON_AUTOCRAFT, false)
                .states(3, state -> new IconButton.Look(AUTOCRAFT_U + (state == 2 ? 0 : ICON), ACTIVE_V,
                        state == 2 ? "gui.projectbench.project_bench.autocraft_on" : "gui.projectbench.project_bench.autocraft_off"));
        for (int which = 0; which < auto.length; ++which) {
            auto[which] = barButton(Component.translatable("gui.projectbench.project_bench.auto_item"),
                    Component.translatable("gui.projectbench.project_bench.auto_help"), 176 + 7 * ICON, ICON, ProjectBenchMenu.autoButton(which),
                    Config.get().keepLayout() && Config.get().autoButton())
                    .states(AutoMode.values().length, mode -> new IconButton.Look(176 + 7 * ICON, mode * ICON, switch (AutoMode.of(mode)) {
                        case OFF -> "gui.projectbench.project_bench.auto_off";
                        case ITEM -> "gui.projectbench.project_bench.auto_item";
                        case TAGS -> "gui.projectbench.project_bench.auto_tags";
                    }));
            IconButton balance = barButton(Component.translatable("gui.projectbench.project_bench.balance"),
                    Component.translatable("gui.projectbench.project_bench.balance_help"), 176 + ICON, 0, ProjectBenchMenu.balanceButton(which),
                    Config.get().balanceButton());
            IconButton clear = barButton(Component.translatable("gui.projectbench.project_bench.clear"),
                    Component.translatable("gui.projectbench.project_bench.clear_help"), 176 + 2 * ICON, 0, ProjectBenchMenu.clearButton(which),
                    Config.get().clearMatrix());
            matrixBar[which] = autocraft != null ? new IconButton[] {autocraft, auto[which], balance, clear} : new IconButton[] {auto[which], balance, clear};
        }
        activeButton = !station ? null : barButton(Component.translatable("gui.projectbench.project_bench.active_left"),
                Component.translatable("gui.projectbench.project_bench.active_help"), ACTIVE_U, ACTIVE_V, ProjectBenchMenu.BUTTON_ACTIVE, true)
                .states(2, which -> new IconButton.Look(ACTIVE_U + which * ICON, ACTIVE_V,
                        which == 0 ? "gui.projectbench.project_bench.active_left" : "gui.projectbench.project_bench.active_right"));
        showcase = barButton(Component.translatable("gui.projectbench.project_bench.showcase_off"),
                Component.translatable("gui.projectbench.project_bench.showcase_help"), 176 + 5 * ICON, ICON, ProjectBenchMenu.BUTTON_SHOWCASE,
                Config.get().showcase())
                .states(2, on -> new IconButton.Look(176 + 5 * ICON, on == 1 ? 0 : ICON,
                        on == 1 ? "gui.projectbench.project_bench.showcase_on" : "gui.projectbench.project_bench.showcase_off"));
        share = barButton(Component.translatable("gui.projectbench.project_bench.share_all"),
                Component.translatable("gui.projectbench.project_bench.share_help"), 176 + 4 * ICON, ICON, ProjectBenchMenu.BUTTON_SHARE,
                Config.get().external() && menu.anyShared())
                .states(ShareMode.values().length, mode -> switch (ShareMode.of(mode)) {
                    case ALL -> new IconButton.Look(176 + 4 * ICON, 0, "gui.projectbench.project_bench.share_all");
                    case NONE -> new IconButton.Look(176 + 4 * ICON, ICON, "gui.projectbench.project_bench.share_none");
                    case OPEN -> new IconButton.Look(176 + 4 * ICON, 2 * ICON, "gui.projectbench.project_bench.share_open");
                });
        benchBar = activeButton != null ? new IconButton[] {activeButton, showcase, share} : new IconButton[] {showcase, share};
        layoutBar();
        record.active = menu.canRecord();
        record.visible = Feature.BLUEPRINT.enabled();
        shownCost = null;
        syncCost();
        fill.visible = Feature.BLUEPRINT.enabled() && Config.get().autoFill();
        syncToggles();
        layoutOutside();
    }

    private IconButton barButton(Component name, Component help, int u, int v, int id, boolean visible) {
        IconButton button = addRenderableWidget(new IconButton(leftPos, Math.max(0, topPos - BAR), BUTTON, BUTTON, name, help, TEXTURE, u, v, ICON, b -> press(id)));
        button.visible = visible;
        bar.add(button);
        return button;
    }

    private void layoutBar() {
        areasStale = true;
        int end = 0;
        for (int which = 0; which < matrixBar.length; ++which) {
            int x = ProjectBenchMenu.MATRIX_X;
            for (IconButton button : matrixBar[which]) {
                if (button.visible) {
                    button.setX(leftPos + ProjectBenchMenu.halfX(which, x, BUTTON));
                    x += BAR;
                }
            }
            end = Math.max(ProjectBenchMenu.MATRIX_X + ProjectBenchMenu.MATRIX_WIDTH + GAP, x);
        }
        int width = -GAP;
        for (IconButton button : benchBar) {
            width += button.visible ? BAR : 0;
        }
        int x = menu.station() ? (imageWidth - width) / 2 : end;
        for (IconButton button : benchBar) {
            if (button.visible) {
                button.setX(leftPos + x);
                x += BAR;
            }
        }
    }

    private boolean inBar(double mouseX, double mouseY) {
        for (IconButton button : bar) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private void syncToggles() {
        if (activeButton != null) {
            activeButton.show(menu.active());
        }
        share.show(menu.shareMode().ordinal());
        for (int which = 0; which < auto.length; ++which) {
            auto[which].show(menu.autoMode(which).ordinal());
        }
        int state = menu.autocraft();
        if (autocraft != null && autocraft.show(state) && autocraft.visible != (state != 0)) {
            autocraft.visible = state != 0;
            layoutBar();
        }
        showcase.show(menu.showcase() ? 1 : 0);
    }

    private void syncCost() {
        Config.Settings s = Config.get();
        Item cost = Config.costItem(s);
        int missing = menu.missingCost();
        if (cost == shownCost && s.recordCostCount() == shownCostCount && missing == shownMissing) {
            return;
        }
        shownCost = cost;
        shownCostCount = s.recordCostCount();
        shownMissing = missing;
        Component name = Component.translatable("gui.projectbench.project_bench.record");
        if (cost == Items.AIR) {
            costGhost = ItemStack.EMPTY;
            record.setHelp(name, Component.translatable("gui.projectbench.project_bench.record_help"));
            return;
        }
        costGhost = new ItemStack(cost, shownCostCount);
        Component help = Component.translatable("gui.projectbench.project_bench.record_help_cost", shownCostCount, cost.getDescription());
        if (missing > 0) {
            help = help.copy().append("\n").append(Component.translatable("gui.projectbench.project_bench.record_missing", missing,
                    cost.getDescription()).withStyle(ChatFormatting.RED));
        }
        record.setHelp(name, help);
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        record.active = menu.canRecord();
        syncCost();
        fill.active = menu.benchOf(menu.active()).blueprint() != null;
        syncToggles();
        for (Matrix matrix : matrices) {
            matrix.listRecipes();
        }
    }

    private final class Matrix implements RecipeChoices {
        private final int which;
        private final MatrixLayout listed = new MatrixLayout();
        private List<RecipeHolder<CraftingRecipe>> recipes = List.of();
        private ItemStack[] results = new ItemStack[0];
        private int seenChoice = -1;
        private int revision;

        Matrix(int which) {
            this.which = which;
        }

        void listRecipes() {
            if (minecraft == null || minecraft.level == null) {
                return;
            }
            boolean changed = false;
            int start = menu.matrixStart(which);
            for (int i = 0; i < Blueprint.SIZE; ++i) {
                changed |= listed.set(i, menu.getSlot(start + i).getItem());
            }
            if (changed) {
                CraftingInput input = CraftingInput.of(3, 3, listed.items());
                recipes = ProjectBenchBlockEntity.recipesFor(minecraft.level, input);
                results = new ItemStack[recipes.size()];
                for (int i = 0; i < results.length; ++i) {
                    results[i] = recipes.get(i).value().assemble(input, minecraft.level.registryAccess());
                }
            }
            if (changed || seenChoice != menu.choice(which)) {
                seenChoice = menu.choice(which);
                ++revision;
            }
        }

        @Override
        public int recipeCount() {
            return recipes.size();
        }

        @Override
        public ResourceLocation recipeId(int i) {
            return recipes.get(i).id();
        }

        @Override
        public ItemStack recipeResult(int i, HolderLookup.Provider registries) {
            return results[i];
        }

        @Override
        public int choice() {
            int c = menu.choice(which);
            return c < recipes.size() ? c : -1;
        }

        @Override
        public int revision() {
            return revision;
        }

        @Override
        public void selectRecipe(int i) {
            if (i >= 0 && i < recipes.size() && i < ProjectBenchMenu.MAX_RECIPES) {
                press(ProjectBenchMenu.recipeButton(which, i));
            }
        }

        @Override
        public Slot resultSlot() {
            return menu.getSlot(menu.resultIndex(which));
        }
    }

    private Matrix activeMatrix() {
        return matrices[Math.min(menu.active(), matrices.length - 1)];
    }

    @Override
    public int recipeCount() {
        return activeMatrix().recipeCount();
    }

    @Override
    public ResourceLocation recipeId(int i) {
        return activeMatrix().recipeId(i);
    }

    @Override
    public ItemStack recipeResult(int i, HolderLookup.Provider registries) {
        return activeMatrix().recipeResult(i, registries);
    }

    @Override
    public int choice() {
        return activeMatrix().choice();
    }

    @Override
    public int revision() {
        return activeMatrix().revision() * 2 + menu.active();
    }

    @Override
    public void selectRecipe(int i) {
        activeMatrix().selectRecipe(i);
    }

    @Override
    public Slot resultSlot() {
        return activeMatrix().resultSlot();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (menu.archiveSlots() > 0) {
            renderPanel(g, mouseX, mouseY);
        }
        if (menu.outsideSlots() > 0) {
            renderOutside(g, mouseX, mouseY);
        }
        if (menu.station()) {
            g.blit(STATION_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, ProjectBenchMenu.STATION_WIDTH, ProjectBenchMenu.STATION_HEIGHT);
            renderStation(g);
        } else {
            g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
        for (int which = 0; which < matrices.length; ++which) {
            renderMatrix(g, which);
            renderLocks(g, which);
            renderCost(g, which);
        }
    }

    private void renderCost(GuiGraphics g, int which) {
        if (costGhost.isEmpty()) {
            return;
        }
        Slot slot = menu.slots.get(menu.costIndex(which));
        int x = leftPos + slot.x - 1;
        int y = topPos + slot.y - 1;
        Draw.slot(g, x, y, Draw.SLOT);
        if (!slot.hasItem()) {
            g.renderFakeItem(costGhost, x + 1, y + 1);
            g.renderItemDecorations(font, costGhost, x + 1, y + 1);
            g.fill(RenderType.guiGhostRecipeOverlay(), x + 1, y + 1, x + 17, y + 17, GHOST_COVER);
        }
    }

    private void renderLocks(GuiGraphics g, int which) {
        if (!Config.get().slotLocks()) {
            return;
        }
        int start = menu.storageStart(which);
        for (int i = 0; i < ProjectBenchBlockEntity.STORAGE; ++i) {
            ItemStack lock = menu.lock(which, i);
            if (lock.isEmpty()) {
                continue;
            }
            Slot slot = menu.slots.get(start + i);
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            g.renderOutline(x - 1, y - 1, SLOT_SIZE, SLOT_SIZE, LOCK_EDGE);
            if (!slot.hasItem()) {
                g.renderFakeItem(lock, x, y);
                g.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, GHOST_COVER);
            }
        }
    }

    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        super.renderSlot(g, slot);
        int which = menu.storageHalf(slot.index);
        if (which < 0 || menu.lock(which, slot.index - menu.storageStart(which)).isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.blit(TEXTURE, slot.x, slot.y, PADLOCK_U, PADLOCK_V, PADLOCK, PADLOCK);
        g.pose().popPose();
    }

    private void renderStation(GuiGraphics g) {
        for (int which = 0; which < 2; ++which) {
            int tint = which == 0 ? ClientConfig.leftTint() : ClientConfig.rightTint();
            if (tint != 0) {
                int mx = leftPos + matrixX(which) - 1;
                int my = topPos + ProjectBenchMenu.MATRIX_Y - 1;
                g.fill(mx, my, mx + 3 * SLOT_SIZE, my + 3 * SLOT_SIZE, tint);
                int sx = leftPos + ProjectBenchMenu.STATION_STORAGE_X - 1;
                int sy = topPos + ProjectBenchMenu.STORAGE_Y - 1 + which * 2 * SLOT_SIZE;
                g.fill(sx, sy, sx + 9 * SLOT_SIZE, sy + 2 * SLOT_SIZE, tint);
            }
        }
        g.blit(TEXTURE, leftPos + matrixX(menu.active()) - 3, topPos + ProjectBenchMenu.MATRIX_Y - 3, FRAME_U, FRAME_V, FRAME, FRAME);
    }

    private void renderMatrix(GuiGraphics g, int which) {
        if (!Feature.BLUEPRINT.enabled()) {
            int x = leftPos + ProjectBenchMenu.halfX(which, ProjectBenchMenu.BLUEPRINT_X, 16);
            int y = topPos + ProjectBenchMenu.BLUEPRINT_Y;
            g.fill(x - 1, y - 1, x + 17, y + 17, Draw.PANEL);
            return;
        }
        if (locked(which)) {
            int x = leftPos + ProjectBenchMenu.halfX(which, ProjectBenchMenu.RESULT_X, 16);
            int y = topPos + ProjectBenchMenu.RESULT_Y;
            g.fill(x, y, x + 16, y + 16, LOCKED);
        }
        Blueprint blueprint = menu.benchOf(which).blueprint();
        if (blueprint != null) {
            Container matrix = menu.benchOf(which).matrix();
            for (int i = 0; i < Blueprint.SIZE; ++i) {
                if (BenchStacks.missing(matrix, blueprint, i)) {
                    int x = leftPos + matrixX(which) + i % 3 * SLOT_SIZE;
                    int y = topPos + ProjectBenchMenu.MATRIX_Y + i / 3 * SLOT_SIZE;
                    g.renderFakeItem(blueprint.ghost(i), x, y);
                    g.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, GHOST_COVER);
                }
            }
        }
    }

    private int panelX() {
        return leftPos - PANEL_WIDTH + PANEL_OVERLAP;
    }

    private int panelRows() {
        return Math.min(PANEL_ROWS, rows());
    }

    private int rows() {
        return (menu.archiveSlots() + PANEL_COLUMNS - 1) / PANEL_COLUMNS;
    }

    public List<Rect2i> extraAreas() {
        int panel = menu.archiveSlots() > 0 ? panelHeight() : 0;
        OutsidePanel chosen = selectedPanel();
        if (areasStale || panel != areasPanel || chosen != areasChosen) {
            areasStale = false;
            areasPanel = panel;
            areasChosen = chosen;
            areas.clear();
            if (panel > 0) {
                areas.add(new Rect2i(panelX(), topPos, PANEL_WIDTH, panel));
            }
            if (chosen != null) {
                areas.add(new Rect2i(outsideX(), topPos, OUTSIDE_WIDTH, TAB_STRIP + chosen.height()));
            }
            for (IconButton button : bar) {
                if (button.visible) {
                    areas.add(new Rect2i(button.getX(), button.getY(), BUTTON, BUTTON));
                }
            }
        }
        return areasView;
    }

    private record OutsidePanel(int index, int height, int slots, int rows, int shown, Component title, FormattedCharSequence fittedTitle, int color,
                                int text, ItemStack icon, @Nullable Processor processor) {
    }

    private int outsideTop() {
        return topPos + TAB_STRIP;
    }

    private int outsideX() {
        return leftPos + imageWidth - PANEL_OVERLAP;
    }

    private void layoutOutside() {
        List<ExternalInventories.Panel> panels = menu.outsidePanels();
        outsideLayout.clear();
        for (int i = 0; i < panels.size(); ++i) {
            ExternalInventories.Panel panel = panels.get(i);
            int rows = panel.processor() != null ? 0 : (panel.slots() + OUTSIDE_COLUMNS - 1) / OUTSIDE_COLUMNS;
            int shown = Math.min(OUTSIDE_ROWS, rows);
            long tint = BlockTint.of(panel.icon().getItem());
            int text = BlockTint.text(tint);
            FormattedCharSequence fitted = Language.getInstance().getVisualOrder(font.substrByWidth(panel.title(), OUTSIDE_WIDTH - 3 - PANEL_TITLE_X - 10));
            outsideLayout.add(new OutsidePanel(i, panel.processor() != null ? PROCESSOR_HEIGHT : PANEL_SLOTS_Y + shown * SLOT_SIZE + 7,
                    panel.slots(), rows, shown, panel.title(), fitted, BlockTint.window(tint), text == BlockTint.USUAL_TEXT ? TEXT : text, panel.icon(),
                    panel.processor()));
        }
    }

    @Nullable
    private OutsidePanel selectedPanel() {
        return outsideLayout.isEmpty() ? null : outsideLayout.get(Math.min(menu.selectedTab(), outsideLayout.size() - 1));
    }

    private int tabX(int slot) {
        return leftPos + imageWidth + slot * (TAB_WIDTH + 2);
    }

    private int tabAt(double mouseX, double mouseY) {
        if (mouseY < topPos || mouseY >= topPos + TAB_STRIP + 2) {
            return -1;
        }
        for (int index = 0; index < outsideLayout.size(); ++index) {
            if (mouseX >= tabX(index) && mouseX < tabX(index) + TAB_WIDTH) {
                return index;
            }
        }
        return -1;
    }

    private void renderOutside(GuiGraphics g, int mouseX, int mouseY) {
        OutsidePanel panel = selectedPanel();
        if (panel == null) {
            return;
        }
        int x0 = outsideX();
        int top = outsideTop();
        int h = panel.height();
        int tabHover = tabAt(mouseX, mouseY);
        renderTabs(g, panel, top, tabHover, false);
        Draw.frame(g, x0, top, OUTSIDE_WIDTH, h, tint(Draw.SHADOW, panel.color()), tint(Draw.PANEL, panel.color()));
        renderTabs(g, panel, top, tabHover, true);
        if (panel.processor() != null) {
            renderProcessor(g, panel.processor(), panel.color(), x0, top);
        }
        int slotColor = slotColor(panel.color());
        g.drawString(font, panel.fittedTitle(), x0 + 3 + PANEL_TITLE_X, top + PANEL_TITLE_Y, panel.text(), false);
        if (panel.processor() != null) {
            return;
        }
        int scroll = menu.outsideScroll();
        int sx = x0 + OUTSIDE_SLOTS_DX - 1;
        int sy = top + PANEL_SLOTS_Y - 1;

        for (int row = 0; row < panel.shown(); ++row) {
            for (int col = 0; col < OUTSIDE_COLUMNS; ++col) {
                if ((scroll + row) * OUTSIDE_COLUMNS + col >= panel.slots()) {
                    continue;
                }
                Draw.slot(g, sx + col * SLOT_SIZE, sy + row * SLOT_SIZE, slotColor);
            }
        }
        if (panel.rows() > panel.shown()) {
            Draw.scrollThumb(g, x0 + OUTSIDE_WIDTH - 6, sy, 2, panel.shown() * SLOT_SIZE, panel.shown(), panel.rows(), scroll, Draw.SCROLLBAR);
        }
    }

    private void renderTabs(GuiGraphics g, OutsidePanel panel, int top, int tabHover, boolean chosenOnes) {
        for (int index = 0; index < outsideLayout.size(); ++index) {
            boolean chosen = index == panel.index();
            if (chosen != chosenOnes) {
                continue;
            }
            OutsidePanel tab = outsideLayout.get(index);
            int x = tabX(index);
            int y = top - TAB_STRIP;
            setColor(g, tab.color(), chosen || index == tabHover ? 1F : 0.9F);
            g.blitSprite(chosen ? TAB_ON : TAB_OFF, x, y, TAB_WIDTH, TAB_SPRITE_HEIGHT);
            g.setColor(1F, 1F, 1F, 1F);
            if (!tab.icon().isEmpty()) {
                g.renderItem(tab.icon(), x + (TAB_WIDTH - 16) / 2, y + (chosen ? 9 : 12));
            }
        }
    }

    private void renderProcessor(GuiGraphics g, Processor processor, int color, int x0, int top) {
        setColor(g, color, 1F);
        g.blit(MACHINE_TEXTURE[processor.ordinal()], x0 + MACHINE_U, top + MACHINE_V, MACHINE_U, MACHINE_V, MACHINE_WIDTH, MACHINE_HEIGHT);
        try {
            renderProcessorProgress(g, processor, x0, top);
        } finally {
            g.setColor(1F, 1F, 1F, 1F);
        }
    }

    private void renderProcessorProgress(GuiGraphics g, Processor processor, int x0, int top) {
        if (processor.brewing()) {
            int fuel = Mth.clamp((18 * menu.processorData(1) + 20 - 1) / 20, 0, 18);
            if (fuel > 0) {
                g.blitSprite(BREWING_FUEL, 18, 4, 0, 0, x0 + 60, top + 44, fuel, 4);
            }
            int brewTime = menu.processorData(0);
            if (brewTime > 0) {
                int arrow = (int) (28F * (1F - brewTime / 400F));
                if (arrow > 0) {
                    g.blitSprite(BREWING_PROGRESS, 9, 28, 0, 0, x0 + 97, top + 16, 9, arrow);
                }
                int bubbles = BUBBLES[brewTime / 2 % BUBBLES.length];
                if (bubbles > 0) {
                    g.blitSprite(BREWING_BUBBLES, 12, 29, 0, 29 - bubbles, x0 + 63, top + 14 + 29 - bubbles, 12, bubbles);
                }
            }
            return;
        }
        int litTime = menu.processorData(0);
        if (litTime > 0) {
            int duration = menu.processorData(1) == 0 ? 200 : menu.processorData(1);
            int flame = Mth.ceil(Mth.clamp(litTime / (float) duration, 0F, 1F) * 13F) + 1;
            g.blitSprite(LIT_PROGRESS[processor.ordinal()], 14, 14, 0, 14 - flame, x0 + 56, top + 36 + 14 - flame, 14, flame);
        }
        int progress = menu.processorData(2);
        int total = menu.processorData(3);
        int arrow = Mth.ceil((total != 0 && progress != 0 ? Mth.clamp(progress / (float) total, 0F, 1F) : 0F) * 24F);
        g.blitSprite(BURN_PROGRESS[processor.ordinal()], 24, 16, 0, 0, x0 + 79, top + 34, arrow, 16);
    }

    @Nullable
    private OutsidePanel outsidePanelAt(double mouseX, double mouseY) {
        OutsidePanel panel = selectedPanel();
        if (panel == null || mouseX < leftPos + imageWidth || mouseX >= outsideX() + OUTSIDE_WIDTH
                || mouseY < outsideTop() || mouseY >= outsideTop() + panel.height()) {
            return null;
        }
        return panel;
    }

    private boolean inOutside(double mouseX, double mouseY) {
        return outsidePanelAt(mouseX, mouseY) != null || tabAt(mouseX, mouseY) >= 0
                || !outsideLayout.isEmpty() && mouseX >= leftPos + imageWidth && mouseX < outsideX() + OUTSIDE_WIDTH && mouseY >= topPos && mouseY < topPos + TAB_STRIP;
    }

    static int slotColor(int color) {
        return color == BlockTint.PLAIN ? Draw.SLOT : ARGB32.opaque(ClientConfig.towardsWhite(tint(Draw.PANEL, color), SLOT_LIGHTEN));
    }

    static int tint(int argb, int color) {
        return color == BlockTint.PLAIN ? argb : ARGB32.multiply(argb, ARGB32.opaque(color));
    }

    private static void setColor(GuiGraphics g, int color, float shade) {
        int rgb = color == BlockTint.PLAIN ? 0xFFFFFF : color;
        g.setColor(ARGB32.red(rgb) / 255F * shade, ARGB32.green(rgb) / 255F * shade, ARGB32.blue(rgb) / 255F * shade, 1F);
    }

    private int panelHeight() {
        return PANEL_SLOTS_Y + panelRows() * SLOT_SIZE + 7;
    }

    private void renderPanel(GuiGraphics g, int mouseX, int mouseY) {
        int x0 = panelX();
        int sx = x0 + PANEL_SLOTS_X;
        int sy = topPos + PANEL_SLOTS_Y;
        int h = panelHeight();

        Draw.frame(g, x0, topPos, PANEL_WIDTH, h, Draw.SHADOW, Draw.PANEL);
        g.drawString(font, panelTitle, x0 + PANEL_TITLE_X, topPos + PANEL_TITLE_Y, TEXT, false);
        int hovered = panelSlot(mouseX, mouseY);
        for (int row = 0; row < panelRows(); ++row) {
            for (int col = 0; col < PANEL_COLUMNS; ++col) {
                int k = (scroll + row) * PANEL_COLUMNS + col;
                if (k >= menu.archiveSlots()) {
                    continue;
                }
                int x = sx + col * SLOT_SIZE;
                int y = sy + row * SLOT_SIZE;
                Draw.slot(g, x, y, Draw.SLOT);
                ItemStack stack = menu.archiveItem(k);
                Blueprint recorded = Blueprint.of(stack);
                if (recorded != null) {
                    g.fill(x, y, x + 17, y + 17, RECORDED);
                    g.renderItem(recorded.result(), x + 1, y + 1);
                } else if (!stack.isEmpty()) {
                    g.renderItem(stack, x + 1, y + 1);
                }
                if (stack.getCount() > 1) {
                    g.renderItemDecorations(font, stack, x + 1, y + 1);
                }
                if (k == hovered) {
                    g.fill(RenderType.guiOverlay(), x + 1, y + 1, x + 17, y + 17, HOVER);
                }
            }
        }

        if (rows() > PANEL_ROWS) {
            Draw.scrollThumb(g, x0 + PANEL_WIDTH - 6, sy, 2, PANEL_ROWS * SLOT_SIZE, PANEL_ROWS, rows(), scroll, Draw.SCROLLBAR);
        }
    }

    private int panelSlot(double mouseX, double mouseY) {
        if (menu.archiveSlots() == 0) {
            return -1;
        }
        int dx = (int) Math.floor(mouseX) - panelX() - PANEL_SLOTS_X;
        int dy = (int) Math.floor(mouseY) - topPos - PANEL_SLOTS_Y;
        if (dx < 0 || dy < 0 || dx >= PANEL_COLUMNS * SLOT_SIZE || dy >= panelRows() * SLOT_SIZE) {
            return -1;
        }
        int k = (scroll + dy / SLOT_SIZE) * PANEL_COLUMNS + dx / SLOT_SIZE;
        return k < menu.archiveSlots() ? k : -1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (minecraft != null && minecraft.level != null) {
            for (RecipeSelector selector : selectors) {
                if (selector != null) {
                    selector.render(g, font, minecraft.level.registryAccess(), leftPos, topPos, mouseX, mouseY);
                }
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }

    private boolean locked(int which) {
        return menu.choice(which) >= 0 && !menu.craftable(which);
    }

    private boolean overLockedResult(int mouseX, int mouseY) {
        for (int which = 0; which < matrices.length; ++which) {
            if (locked(which) && isHovering(ProjectBenchMenu.halfX(which, ProjectBenchMenu.RESULT_X, 16), ProjectBenchMenu.RESULT_Y, 16, 16, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot != null && menu.handsOff(hoveredSlot)) {
            lines = new ArrayList<>(lines);
            lines.add(Component.translatable("gui.projectbench.project_bench.hands_off").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int k = panelSlot(mouseX, mouseY);
        int tab = tabAt(mouseX, mouseY);
        if (overLockedResult(mouseX, mouseY)) {
            g.renderTooltip(font, List.of(Component.translatable("gui.projectbench.project_bench.locked"),
                    Component.translatable("gui.projectbench.project_bench.locked_help").withStyle(ChatFormatting.GRAY)), Optional.empty(), mouseX, mouseY);
        } else if (tab >= 0) {
            g.renderTooltip(font, outsideLayout.get(tab).title(), mouseX, mouseY);
        } else if (hoveredSlot instanceof CostContainer.CostSlot && !hoveredSlot.hasItem() && menu.getCarried().isEmpty() && !costGhost.isEmpty()) {
            g.renderTooltip(font, List.of(Component.translatable("gui.projectbench.project_bench.cost"),
                    Component.translatable("gui.projectbench.project_bench.cost_help", shownCostCount, costGhost.getHoverName())
                            .withStyle(ChatFormatting.GRAY)), Optional.empty(), mouseX, mouseY);
        } else if (k >= 0 && menu.getCarried().isEmpty() && !menu.archiveItem(k).isEmpty()) {
            g.renderTooltip(font, menu.archiveItem(k), mouseX, mouseY);
        } else {
            super.renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (RecipeSelector selector : selectors) {
            if (selector != null && selector.mouseClicked(mouseX, mouseY, leftPos, topPos)) {
                return true;
            }
        }
        int k = panelSlot(mouseX, mouseY);
        if (k >= 0 && button == 0) {
            press(ProjectBenchMenu.archiveButton(k));
            return true;
        }
        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0) {
            if (button == 0 && tab != menu.selectedTab()) {
                press(ProjectBenchMenu.BUTTON_TAB + tab);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (menu.station() && SWITCH_MATRIX.matches(keyCode, scanCode)) {
            press(ProjectBenchMenu.BUTTON_ACTIVE);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inPanel(mouseX, mouseY) && rows() > PANEL_ROWS) {
            scroll = Math.max(0, Math.min(rows() - PANEL_ROWS, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        OutsidePanel over = outsidePanelAt(mouseX, mouseY);
        if (over != null || inOutside(mouseX, mouseY)) {
            if (over == null) {
                return true;
            }
            if (over.rows() > over.shown()) {
                int row = Math.max(0, Math.min(over.rows() - over.shown(), menu.outsideScroll() - (int) Math.signum(scrollY)));
                if (row != menu.outsideScroll()) {
                    press(ProjectBenchMenu.BUTTON_SCROLL + row);
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean inPanel(double mouseX, double mouseY) {
        return menu.archiveSlots() > 0 && mouseX >= panelX() && mouseX < leftPos && mouseY >= topPos && mouseY < topPos + panelHeight();
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return !inPanel(mouseX, mouseY) && !inOutside(mouseX, mouseY) && !inBar(mouseX, mouseY) && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }
}
