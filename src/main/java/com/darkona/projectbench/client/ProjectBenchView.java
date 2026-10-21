package com.darkona.projectbench.client;

import com.darkona.projectbench.ProjectBench;
import com.darkona.projectbench.block.bench.BenchWheel;
import com.darkona.projectbench.block.bench.ProjectBenchBlockEntity;
import com.darkona.projectbench.block.bench.ProjectBenchMenu;
import dev.darkona.jdlib.api.inventory.GridFill;
import dev.darkona.jdlib.api.pattern.Blueprint;
import com.darkona.projectbench.blueprint.CostContainer;
import com.darkona.projectbench.config.AutoMode;
import com.darkona.projectbench.config.ClientConfig;
import com.darkona.projectbench.config.Config;
import com.darkona.projectbench.registry.ModItems;
import com.darkona.projectbench.config.Feature;
import com.darkona.projectbench.config.ShareMode;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import dev.darkona.jdlib.api.ui.JDNode;
import dev.darkona.jdlib.api.ui.JDNode.Icon;
import dev.darkona.jdlib.api.ui.JDViews;
import dev.darkona.jdlib.api.ui.LockView;
import dev.darkona.jdlib.api.ui.MenuView;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public final class ProjectBenchView implements MenuView<ProjectBenchMenu> {
    public static final KeyMapping SWITCH_MATRIX = new KeyMapping("key.projectbench.switch_matrix", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, "key.categories.projectbench");

    static final ResourceLocation TEXTURE = ProjectBench.id("textures/gui/project_bench.png");
    static final ResourceLocation STATION_TEXTURE = ProjectBench.id("textures/gui/project_station.png");
    private static final ResourceLocation TEMPLATE = ProjectBench.id("ui/project_bench.ui.nbt");

    public static final int ARROW_X = 110;
    public static final int ARROW_Y = 37;
    public static final int ARROW_WIDTH = 24;
    public static final int ARROW_HEIGHT = 15;

    private static final int SLOT = JDNode.SLOT;
    private static final int WINDOW_HEIGHT = 222;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;

    private static final int ICON = 10;
    static final String LIGHT_SLOTS = "jd-light-slots";
    private static final int LOCK_SIZE = 8;
    private static final int BUTTON = 14;
    private static final int GAP = 2;
    private static final int BAR = BUTTON + GAP;
    private static final int RECORD_X = 10;
    private static final int RECORD_Y = 60;
    private static final int STATION_RECORD_X = (ProjectBenchMenu.STATION_WIDTH - 2 * BUTTON - GAP) / 2;
    private static final int AUTOCRAFT_U = 196;
    private static final int FRAME = 3 * SLOT + 4;
    private static final int FRAME_V = 32;
    private static final int FRAME_EDGE = 3;
    private static final int ACTIVE_U = 176;
    private static final int ACTIVE_V = FRAME_V + FRAME + 2;

    private static final List<Component> HANDS_OFF = List.of(Component.translatable("gui.projectbench.project_bench.hands_off").withStyle(ChatFormatting.GRAY));
    private static final List<Component> LOCKED = List.of(Component.translatable("gui.projectbench.project_bench.locked"),
            Component.translatable("gui.projectbench.project_bench.locked_help").withStyle(ChatFormatting.GRAY));

    @Nullable
    private static volatile BenchChoices choices;
    @Nullable
    private BenchCost cost;

    @Nullable
    public static RecipeChoices choicesOf(ProjectBenchMenu menu) {
        BenchChoices current = choices;
        return current != null && current.menu() == menu ? current : null;
    }

    private static int matrixX(int which) {
        return ProjectBenchMenu.halfX(which, ProjectBenchMenu.MATRIX_X, ProjectBenchMenu.MATRIX_WIDTH);
    }

    private static boolean locked(ProjectBenchMenu menu, int which) {
        return menu.choice(which) >= 0 && !menu.craftable(which);
    }

    private static JDNode halfTint(JDNode slots, boolean station, int which) {
        return station ? JDViews.veilSlots(slots, () -> which == 0 ? ClientConfig.leftTint() : ClientConfig.rightTint()) : slots;
    }

    private static ItemStack storageLock(ProjectBenchMenu menu, int which, Slot slot) {
        return Config.get().slotLocks() ? menu.lock(which, slot.index - menu.storageStart(which)) : ItemStack.EMPTY;
    }

    @Nullable
    private static JDNode.Face resultLock(ProjectBenchMenu menu, int which) {
        if (!Feature.BLUEPRINT.enabled() || menu.choice(which) < 0) {
            return null;
        }
        if (locked(menu, which)) {
            return JDViews.LOCK_CLOSED;
        }
        return menu.benchOf(which).blueprint() != null ? JDViews.LOCK_OPEN : null;
    }

    private static ItemStack expected(ProjectBenchMenu menu, int which, Slot slot) {
        Blueprint blueprint = menu.benchOf(which).blueprint();
        return blueprint != null && GridFill.missing(menu.benchOf(which).matrix(), blueprint.pattern(), slot.getContainerSlot())
                ? blueprint.ghost(slot.getContainerSlot()) : ItemStack.EMPTY;
    }

    private static void addStationFrames(List<JDNode> kids, ProjectBenchMenu menu) {
        for (int which = 0; which < 2; ++which) {
            int w = which;
            int x = matrixX(which) - 3;
            int y = ProjectBenchMenu.MATRIX_Y - 3;
            int side = FRAME - 2 * FRAME_EDGE;
            kids.add(frameStrip(menu, w, "top", FRAME, FRAME_EDGE, 0, 0, x, y));
            kids.add(frameStrip(menu, w, "bottom", FRAME, FRAME_EDGE, 0, FRAME - FRAME_EDGE, x, y));
            kids.add(frameStrip(menu, w, "left", FRAME_EDGE, side, 0, FRAME_EDGE, x, y));
            kids.add(frameStrip(menu, w, "right", FRAME_EDGE, side, FRAME - FRAME_EDGE, FRAME_EDGE, x, y));
        }
    }

    private static JDNode frameStrip(ProjectBenchMenu menu, int which, String part, int w, int h, int du, int dv, int x, int y) {
        return JDViews.at(JDViews.shownWhen(JDViews.styled(JDViews.sprite("active-" + which + "-" + part, w, h, TEXTURE, 176 + du, FRAME_V + dv), "jd-on-top"),
                () -> menu.active() == which), x + du, y + dv);
    }

    private static final ResourceLocation[] SHEETS = {ProjectBench.id("lss/bench.lss")};

    @Override
    public ResourceLocation[] stylesheets() {
        return SHEETS;
    }

    @Override
    public JDNode build(ProjectBenchMenu menu, Inventory inventory, Component title) {
        boolean station = menu.station();
        int width = station ? ProjectBenchMenu.STATION_WIDTH : ProjectBenchMenu.WIDTH;
        int height = station ? ProjectBenchMenu.STATION_HEIGHT : WINDOW_HEIGHT;
        int storageX = station ? ProjectBenchMenu.STATION_STORAGE_X : 8;
        int playerY = station ? ProjectBenchMenu.STATION_PLAYER_Y : ProjectBenchMenu.PLAYER_Y;
        BenchChoices recipes = new BenchChoices(menu);
        choices = recipes;
        BenchCost benchCost = new BenchCost(menu);
        benchCost.tick();
        cost = benchCost;
        boolean templated = !station && hasTemplate();
        List<JDNode> kids = new ArrayList<>(40);
        if (!templated) {
            kids.add(JDViews.at(JDViews.styled(JDViews.text("title", 150, 10, title), "jd-text"), TITLE_X, TITLE_Y));
            kids.add(JDViews.at(JDViews.styled(JDViews.text("inventory", 150, 10, inventory.getDisplayName()), "jd-text"), storageX, playerY - 11));
            kids.add(JDViews.at(JDViews.playerInventory("player", menu, ProjectBenchMenu.PLAYER_START), storageX - 1, playerY - 1));
        }
        for (int which = 0; which < (station ? 2 : 1); ++which) {
            addHalf(kids, menu, which, station, storageX, benchCost);
        }
        if (station) {
            addStationFrames(kids, menu);
        }
        JDNode archive = BenchPanels.archive(menu);
        if (archive != null) {
            kids.add(archive);
        }
        JDNode outside = BenchPanels.outside(menu);
        if (outside != null) {
            kids.add(outside);
        }
        addButtons(kids, menu, station, benchCost);
        for (int which = 0; which < recipes.halves(); ++which) {
            int x = ProjectBenchMenu.RESULT_X;
            JDNode selector = RecipeSelector.forWindow("recipes-" + which, recipes.half(which), ProjectBenchMenu.halfX(which, x + 3, RecipeSelector.BUTTON),
                    ProjectBenchMenu.RESULT_Y + 42, ProjectBenchMenu.halfX(which, x + 8, 0));
            if (selector != null) {
                kids.add(selector);
            }
        }
        if (templated) {
            return JDViews.template(TEMPLATE)
                    .text("title", () -> title)
                    .text("inventory", inventory::getDisplayName)
                    .slots("player", menu.slots.subList(ProjectBenchMenu.PLAYER_START, ProjectBenchMenu.PLAYER_END))
                    .place("parts", JDViews.column("bench-parts", width, height, 0, kids.toArray(JDNode[]::new)));
        }
        return JDViews.styled(JDViews.column(station ? "station" : "bench", width, height, 0, kids.toArray(JDNode[]::new)), "jd-menu", LIGHT_SLOTS);
    }

    private static boolean hasTemplate() {
        return Minecraft.getInstance().getResourceManager().getResource(TEMPLATE).isPresent();
    }

    @Override
    public List<Component> slotTooltip(ProjectBenchMenu menu, Slot slot, ItemStack stack) {
        return menu.handsOff(slot) ? HANDS_OFF : List.of();
    }

    @Override
    public List<Component> emptySlotTooltip(ProjectBenchMenu menu, Slot slot) {
        for (int which = 0; which < (menu.station() ? 2 : 1); ++which) {
            if (slot.index == menu.resultIndex(which) && locked(menu, which)) {
                return LOCKED;
            }
        }
        BenchCost current = cost;
        return slot instanceof CostContainer.CostSlot && current != null ? current.slotTip() : List.of();
    }

    @Override
    public boolean wheelEnabled(ProjectBenchMenu menu) {
        return ClientConfig.mouseTweaks();
    }

    @Override
    public boolean wheelSlot(ProjectBenchMenu menu, Slot slot) {
        if (menu.handsOff(slot)) {
            return false;
        }
        int half = menu.halfOf(slot.index);
        return half < 0 || half == menu.active() || menu.storageHalf(slot.index) >= 0;
    }

    @Override
    public boolean wheelMoves(ProjectBenchMenu menu, Slot from, Slot to) {
        boolean neighbour = inOutside(menu, from) || inOutside(menu, to);
        if (!BenchWheel.shareAllows(menu.shareMode(), neighbour, inPlayer(from) || inPlayer(to))) {
            return false;
        }
        boolean intoGrid = menu.halfOf(to.index) >= 0 && to.index >= menu.matrixStart(menu.halfOf(to.index))
                && to.index < menu.matrixStart(menu.halfOf(to.index)) + ProjectBenchBlockEntity.MATRIX;
        return !intoGrid || to.hasItem() || menu.ghostWants(to.getContainerSlot(), from.getItem());
    }

    private static boolean inOutside(ProjectBenchMenu menu, Slot slot) {
        return slot.index >= menu.outsideIndex(0) && slot.index < menu.outsideIndex(ProjectBenchMenu.OUTSIDE_VISIBLE);
    }

    private static boolean inPlayer(Slot slot) {
        return slot.index >= ProjectBenchMenu.PLAYER_START && slot.index < ProjectBenchMenu.PLAYER_END;
    }

    @Override
    public boolean keyPressed(ProjectBenchMenu menu, int key, int scan, int modifiers) {
        if (menu.station() && SWITCH_MATRIX.matches(key, scan)) {
            BenchChoices.press(menu, ProjectBenchMenu.BUTTON_ACTIVE);
            return true;
        }
        return false;
    }

    @Override
    public void tick(ProjectBenchMenu menu) {
        BenchChoices recipes = choices;
        if (recipes != null && recipes.menu() == menu) {
            recipes.tick();
        }
        if (cost != null) {
            cost.tick();
        }
    }

    private void addHalf(List<JDNode> kids, ProjectBenchMenu menu, int which, boolean station, int storageX, BenchCost cost) {
        String half = "-" + which;
        int matrix = menu.matrixStart(which);
        kids.add(JDViews.at(halfTint(JDViews.ghostItem(JDViews.slotGrid("matrix" + half, menu.slots.subList(matrix, matrix + ProjectBenchBlockEntity.MATRIX), 3),
                slot -> expected(menu, which, slot)), station, which), matrixX(which) - 1, ProjectBenchMenu.MATRIX_Y - 1));
        kids.add(JDViews.at(JDViews.slotIcon(JDViews.lockOverlay(JDViews.resultSlot("result" + half, menu.slots.get(menu.resultIndex(which))), new LockView() {
            @Override
            public ItemStack filter(Slot slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean frozen(Slot slot) {
                return Feature.BLUEPRINT.enabled() && locked(menu, which);
            }
        }), slot -> resultLock(menu, which), JDNode.Corner.TOP_LEFT, LOCK_SIZE),
                ProjectBenchMenu.halfX(which, ProjectBenchMenu.RESULT_X, 16) - 5, ProjectBenchMenu.RESULT_Y - 5));
        int storage = menu.storageStart(which);
        int storageY = ProjectBenchMenu.STORAGE_Y + (station ? which * 2 * SLOT : 0);
        kids.add(JDViews.at(halfTint(JDViews.slotIcon(JDViews.lockOverlay(JDViews.slotGrid("storage" + half, menu.slots.subList(storage, storage + ProjectBenchBlockEntity.STORAGE), 9),
                LockView.filters(slot -> storageLock(menu, which, slot))), slot -> storageLock(menu, which, slot).isEmpty() ? null : JDViews.LOCK_CLOSED,
                JDNode.Corner.TOP_LEFT, LOCK_SIZE), station, which),
                storageX - 1, storageY - 1));
        kids.add(JDViews.at(JDViews.ghostItem(JDViews.slot("blueprint" + half, menu.slots.get(menu.blueprintIndex(which))), new ItemStack(ModItems.BLUEPRINT.get())),
                ProjectBenchMenu.halfX(which, ProjectBenchMenu.BLUEPRINT_X, 16) - 1, ProjectBenchMenu.BLUEPRINT_Y - 1));
        kids.add(JDViews.at(JDViews.ghostItem(JDViews.slot("cost" + half, menu.slots.get(menu.costIndex(which))), slot -> cost.ghost()),
                ProjectBenchMenu.halfX(which, ProjectBenchMenu.COST_X, 16) - 1, ProjectBenchMenu.COST_Y - 1));
    }

    private void addButtons(List<JDNode> kids, ProjectBenchMenu menu, boolean station, BenchCost cost) {
        if (Feature.BLUEPRINT.enabled()) {
            int x = station ? STATION_RECORD_X : RECORD_X;
            Icon record = icon(176, 0);
            kids.add(JDViews.at(JDViews.iconButton("record", BUTTON, () -> record, cost::recordTip, menu::canRecord,
                    () -> BenchChoices.press(menu, ProjectBenchMenu.BUTTON_RECORD)), x, RECORD_Y));
            if (Config.get().autoFill()) {
                Icon fill = icon(176 + 3 * ICON, 0);
                List<Component> tip = tip(Component.translatable("gui.projectbench.project_bench.fill"), "gui.projectbench.project_bench.fill_help");
                kids.add(JDViews.at(JDViews.iconButton("fill", BUTTON, () -> fill, () -> tip, () -> menu.benchOf(menu.active()).blueprint() != null,
                        () -> BenchChoices.press(menu, ProjectBenchMenu.BUTTON_FILL)), x + BUTTON + GAP, RECORD_Y));
            }
        }
        int halves = station ? 2 : 1;
        List<JDNode> bench = new ArrayList<>(3);
        JDNode autocraft = station ? null : JDViews.shownWhen(slotOf(stateful(menu, "autocraft", ProjectBenchMenu.BUTTON_AUTOCRAFT,
                "gui.projectbench.project_bench.autocraft_help", menu::autocraft, 3,
                state -> new Look(AUTOCRAFT_U + (state == 2 ? 0 : ICON), ACTIVE_V,
                        state == 2 ? "gui.projectbench.project_bench.autocraft_on" : "gui.projectbench.project_bench.autocraft_off"))),
                () -> menu.autocraft() != 0);
        List<JDNode> topMatrix = List.of();
        for (int which = 0; which < halves; ++which) {
            int w = which;
            List<JDNode> matrix = new ArrayList<>(4);
            if (autocraft != null) {
                matrix.add(autocraft);
            }
            if (Config.get().keepLayout() && Config.get().autoButton()) {
                matrix.add(slotOf(stateful(menu, "auto-" + which, ProjectBenchMenu.autoButton(which), "gui.projectbench.project_bench.auto_help",
                        () -> menu.autoMode(w).ordinal(), AutoMode.values().length, mode -> new Look(176 + 7 * ICON, mode * ICON, switch (AutoMode.of(mode)) {
                            case OFF -> "gui.projectbench.project_bench.auto_off";
                            case ITEM -> "gui.projectbench.project_bench.auto_item";
                            case TAGS -> "gui.projectbench.project_bench.auto_tags";
                        }))));
            }
            if (Config.get().balanceButton()) {
                matrix.add(slotOf(plain(menu, "balance-" + which, ProjectBenchMenu.balanceButton(which), 176 + ICON, 0,
                        "gui.projectbench.project_bench.balance", "gui.projectbench.project_bench.balance_help")));
            }
            if (Config.get().clearMatrix()) {
                matrix.add(slotOf(plain(menu, "clear-" + which, ProjectBenchMenu.clearButton(which), 176 + 2 * ICON, 0,
                        "gui.projectbench.project_bench.clear", "gui.projectbench.project_bench.clear_help")));
            }
            if (!station) {
                topMatrix = matrix;
            } else {
                for (int i = 0; i < matrix.size(); ++i) {
                    kids.add(JDViews.at(matrix.get(i), ProjectBenchMenu.halfX(which, ProjectBenchMenu.MATRIX_X + i * BAR, BUTTON), -BAR));
                }
            }
        }
        if (station) {
            bench.add(slotOf(stateful(menu, "active", ProjectBenchMenu.BUTTON_ACTIVE, "gui.projectbench.project_bench.active_help", menu::active, 2,
                    which -> new Look(ACTIVE_U + which * ICON, ACTIVE_V,
                            which == 0 ? "gui.projectbench.project_bench.active_left" : "gui.projectbench.project_bench.active_right"))));
        }
        if (Config.get().showcase()) {
            bench.add(slotOf(stateful(menu, "showcase", ProjectBenchMenu.BUTTON_SHOWCASE, "gui.projectbench.project_bench.showcase_help",
                    () -> menu.showcase() ? 1 : 0, 2, on -> new Look(176 + 5 * ICON, on == 1 ? 0 : ICON,
                            on == 1 ? "gui.projectbench.project_bench.showcase_on" : "gui.projectbench.project_bench.showcase_off"))));
        }
        if (Config.get().external() && menu.anyShared()) {
            bench.add(slotOf(stateful(menu, "share", ProjectBenchMenu.BUTTON_SHARE, "gui.projectbench.project_bench.share_help",
                    () -> menu.shareMode().ordinal(), ShareMode.values().length, mode -> switch (ShareMode.of(mode)) {
                        case ALL -> new Look(176 + 4 * ICON, 0, "gui.projectbench.project_bench.share_all");
                        case NONE -> new Look(176 + 4 * ICON, ICON, "gui.projectbench.project_bench.share_none");
                        case OPEN -> new Look(176 + 4 * ICON, 2 * ICON, "gui.projectbench.project_bench.share_open");
                    })));
        }
        if (station) {
            if (!bench.isEmpty()) {
                int x = (ProjectBenchMenu.STATION_WIDTH - (bench.size() * BAR - GAP)) / 2;
                for (int i = 0; i < bench.size(); ++i) {
                    kids.add(JDViews.at(bench.get(i), x + i * BAR, -BAR));
                }
            }
            return;
        }
        int fixed = topMatrix.size() - (autocraft != null ? 1 : 0);
        int least = ProjectBenchMenu.MATRIX_WIDTH + GAP;
        int gapHidden = Math.max(0, least - fixed * BAR);
        int gapShown = Math.max(0, least - (fixed + 1) * BAR);
        List<JDNode> bar = new ArrayList<>(topMatrix);
        if (autocraft != null && gapShown > 0) {
            bar.add(JDViews.shownWhen(JDViews.box("bar-gap-auto", gapShown, BUTTON, () -> 0), () -> menu.autocraft() != 0));
        }
        if (gapHidden > 0) {
            JDNode gap = JDViews.box("bar-gap", gapHidden, BUTTON, () -> 0);
            bar.add(autocraft == null ? gap : JDViews.shownWhen(gap, () -> menu.autocraft() == 0));
        }
        bar.addAll(bench);
        if (!topMatrix.isEmpty() || !bench.isEmpty()) {
            int width = least + (fixed + 1) * BAR + bench.size() * BAR;
            kids.add(JDViews.at(JDViews.row("button-bar", width, BUTTON, 0, bar.toArray(JDNode[]::new)), ProjectBenchMenu.MATRIX_X, -BAR));
        }
    }

    private static JDNode slotOf(JDNode button) {
        return JDViews.row("cell-" + button.id(), BAR, BUTTON, 0, button);
    }

    private record Look(int u, int v, String nameKey) {
    }

    private static Icon icon(int u, int v) {
        return new Icon(TEXTURE, u, v, ICON);
    }

    private static List<Component> tip(Component name, String helpKey) {
        return List.of(name, Component.translatable(helpKey).withStyle(ChatFormatting.GRAY));
    }

    private static JDNode plain(ProjectBenchMenu menu, String id, int press, int u, int v, String nameKey, String helpKey) {
        Icon icon = icon(u, v);
        List<Component> tip = tip(Component.translatable(nameKey), helpKey);
        return JDViews.iconButton(id, BUTTON, () -> icon, () -> tip, () -> true, () -> BenchChoices.press(menu, press));
    }

    private static JDNode stateful(ProjectBenchMenu menu, String id, int press, String helpKey, IntSupplier state, int count, IntFunction<Look> look) {
        Icon[] icons = new Icon[count];
        List<List<Component>> tips = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            Look l = look.apply(i);
            icons[i] = icon(l.u(), l.v());
            tips.add(tip(Component.translatable(l.nameKey()), helpKey));
        }
        IntSupplier safe = () -> Math.max(0, Math.min(count - 1, state.getAsInt()));
        BooleanSupplier on = () -> true;
        return JDViews.iconButton(id, BUTTON, () -> icons[safe.getAsInt()], () -> tips.get(safe.getAsInt()), on, () -> BenchChoices.press(menu, press));
    }
}
