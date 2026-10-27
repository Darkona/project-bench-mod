package com.darkona.projectbench.content.bench;

import dev.darkona.jdlib.api.pattern.Blueprint;
import com.darkona.projectbench.content.blueprint.BlueprintItem;
import dev.darkona.jdlib.JDLib;
import dev.darkona.jdlib.api.ui.JDNode;
import dev.darkona.jdlib.api.ui.JDNode.Grow;
import dev.darkona.jdlib.api.ui.JDViews;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

final class BenchPanels {
    private static final int SLOT = JDNode.SLOT;
    private static final int OVERLAP = 3;
    private static final int TITLE_X = 7;
    private static final int TITLE_Y = 6;
    private static final int SLOTS_X = 7;
    private static final int SLOTS_Y = 17;
    private static final int RECORDED = 0xFF3A5F8F;
    private static final int TEXT = 0xFF404040;
    private static final int SCROLL_WIDTH = 4;

    private static final int ARCHIVE_COLUMNS = 9;
    private static final int ARCHIVE_ROWS = 12;
    static final int ARCHIVE_WIDTH = 7 + ARCHIVE_COLUMNS * SLOT + 7;

    private static final int OUTSIDE_COLUMNS = ProjectBenchMenu.OUTSIDE_COLUMNS;
    private static final int OUTSIDE_ROWS = ProjectBenchMenu.OUTSIDE_ROWS;
    static final int OUTSIDE_WIDTH = 7 + OUTSIDE_COLUMNS * SLOT + 7;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_STRIP = 28;
    private static final int OUTSIDE_SLOTS_DX = 8;

    private static final int MACHINE_U = 7;
    private static final int MACHINE_V = 12;
    private static final int MACHINE_WIDTH = 162;
    private static final int MACHINE_HEIGHT = 68;
    private static final int PROCESSOR_HEIGHT = MACHINE_V + MACHINE_HEIGHT + 4;
    private static final int[] BUBBLES = {29, 24, 20, 16, 11, 6, 0};

    private BenchPanels() {
    }

    @Nullable
    static JDNode archive(ProjectBenchMenu menu) {
        int count = menu.archiveSlots();
        if (count == 0) {
            return null;
        }
        int rows = (count + ARCHIVE_COLUMNS - 1) / ARCHIVE_COLUMNS;
        int shown = Math.min(ARCHIVE_ROWS, rows);
        int[] scroll = {0};
        List<JDNode> kids = new ArrayList<>(shown * ARCHIVE_COLUMNS + 2);
        kids.add(JDViews.at(JDViews.styled(JDViews.text("archive-title", 150, 10, Component.translatable("gui.projectbench.project_bench.archives")), "jd-text"),
                TITLE_X, TITLE_Y));
        for (int row = 0; row < shown; ++row) {
            for (int col = 0; col < ARCHIVE_COLUMNS; ++col) {
                int r = row;
                int c = col;
                IntSupplier index = () -> (scroll[0] + r) * ARCHIVE_COLUMNS + c;
                JDNode cell = JDViews.itemButton("archive-" + row + "-" + col, () -> {
                    int k = index.getAsInt();
                    if (k >= count) {
                        return ItemStack.EMPTY;
                    }
                    ItemStack stack = menu.archiveItem(k);
                    Blueprint recorded = BlueprintItem.blueprint(stack);
                    return recorded != null ? recorded.result() : stack;
                }, () -> {
                    int k = index.getAsInt();
                    return k < count && menu.getCarried().isEmpty() ? menu.archiveItem(k) : ItemStack.EMPTY;
                }, () -> {
                    int k = index.getAsInt();
                    return k < count && BlueprintItem.blueprint(menu.archiveItem(k)) != null ? RECORDED : 0;
                }, () -> {
                    int k = index.getAsInt();
                    if (k < count) {
                        BenchChoices.press(menu, ProjectBenchMenu.archiveButton(k));
                    }
                });
                kids.add(JDViews.at(JDViews.shownWhen(cell, () -> index.getAsInt() < count), SLOTS_X + col * SLOT, SLOTS_Y + row * SLOT));
            }
        }
        int height = SLOTS_Y + shown * SLOT + 7;
        if (rows > ARCHIVE_ROWS) {
            int max = rows - ARCHIVE_ROWS;
            kids.add(JDViews.at(JDViews.scroll("archive-scroll", SCROLL_WIDTH, shown * SLOT, () -> max, () -> scroll[0],
                    v -> scroll[0] = Mth.clamp(v, 0, max)), ARCHIVE_WIDTH - 3 - SCROLL_WIDTH, SLOTS_Y));
        }
        JDNode frame = JDViews.styled(JDViews.frame("archive", ARCHIVE_WIDTH, height, () -> -1, null, kids.toArray(JDNode[]::new)), ProjectBenchView.LIGHT_SLOTS);
        if (rows > ARCHIVE_ROWS) {
            int max = rows - ARCHIVE_ROWS;
            frame = JDViews.onWheel(frame, d -> scroll[0] = Mth.clamp(scroll[0] - (int) Math.signum(d), 0, max));
        }
        return JDViews.sidePanel(frame, JDNode.Side.LEFT, 0, OVERLAP);
    }

    private record Tab(int height, int slots, int rows, int shown, Component title, int color, ItemStack icon, @Nullable Processor processor) {
    }

    @Nullable
    static JDNode outside(ProjectBenchMenu menu) {
        List<ExternalInventories.Panel> panels = menu.outsidePanels();
        if (menu.outsideSlots() <= 0 || panels.isEmpty()) {
            return null;
        }
        var font = Minecraft.getInstance().font;
        Tab[] tabs = new Tab[panels.size()];
        for (int i = 0; i < tabs.length; ++i) {
            ExternalInventories.Panel panel = panels.get(i);
            int rows = panel.processor() != null ? 0 : (panel.slots() + OUTSIDE_COLUMNS - 1) / OUTSIDE_COLUMNS;
            int shown = Math.min(OUTSIDE_ROWS, rows);
            long tint = BlockTint.of(panel.icon().getItem());
            int text = BlockTint.text(tint);
            Component title = Component.literal(font.substrByWidth(panel.title(), OUTSIDE_WIDTH - 3 - TITLE_X - 10).getString())
                    .withStyle(style -> style.withColor((text == BlockTint.USUAL_TEXT ? TEXT : text) & 0xFFFFFF));
            tabs[i] = new Tab(panel.processor() != null ? PROCESSOR_HEIGHT : SLOTS_Y + shown * SLOT + 7, panel.slots(), rows, shown, title,
                    BlockTint.window(tint), panel.icon(), panel.processor());
        }
        IntSupplier selected = () -> Mth.clamp(menu.selectedTab(), 0, tabs.length - 1);
        int frameLeft = (menu.station() ? ProjectBenchMenu.STATION_WIDTH : ProjectBenchMenu.WIDTH) - OVERLAP;

        List<JDNode> kids = new ArrayList<>(40);
        kids.add(JDViews.at(JDViews.tabs("outside-tabs", TAB_WIDTH, TAB_HEIGHT, tabs.length, selected, i -> tabs[i].icon(), i -> tabs[i].title(),
                i -> tabs[i].color(), i -> BenchChoices.press(menu, ProjectBenchMenu.BUTTON_TAB + i), TAB_HEIGHT - TAB_STRIP), OVERLAP, -TAB_STRIP));
        kids.add(JDViews.at(JDViews.styled(JDViews.text("outside-title", OUTSIDE_WIDTH - OVERLAP - TITLE_X - 10, 10, () -> tabs[selected.getAsInt()].title()),
                "jd-text"), OVERLAP + TITLE_X, TITLE_Y));
        for (Processor processor : Processor.values()) {
            addProcessor(kids, menu, processor, tabs, selected);
        }
        for (int i = 0; i < ProjectBenchMenu.PROCESSOR_SLOTS; ++i) {
            Slot slot = menu.slots.get(menu.processorIndex(Processor.FURNACE, i));
            kids.add(JDViews.at(JDViews.slot("processor-slot-" + i, slot), slot.x - 1 - frameLeft, slot.y - 1 - TAB_STRIP));
        }
        int first = menu.outsideIndex(0);
        kids.add(JDViews.at(JDViews.styled(JDViews.fillSlots(JDViews.slotGrid("outside-slots", menu.slots.subList(first, first + ProjectBenchMenu.OUTSIDE_VISIBLE), OUTSIDE_COLUMNS),
                () -> BlockTint.slotFill(tabs[selected.getAsInt()].color())), ProjectBenchView.LIGHT_SLOTS),
                OUTSIDE_SLOTS_DX - 1, SLOTS_Y - 1));
        kids.add(JDViews.at(JDViews.shownWhen(JDViews.scroll("outside-scroll", SCROLL_WIDTH, OUTSIDE_ROWS * SLOT,
                () -> Math.max(0, tabs[selected.getAsInt()].rows() - tabs[selected.getAsInt()].shown()), menu::outsideScroll,
                row -> scrollTo(menu, row)), () -> tabs[selected.getAsInt()].rows() > tabs[selected.getAsInt()].shown()),
                OUTSIDE_WIDTH - 3 - SCROLL_WIDTH, SLOTS_Y - 1));
        JDNode frame = JDViews.frame("outside", OUTSIDE_WIDTH, tabs[selected.getAsInt()].height(), () -> tabs[selected.getAsInt()].color(),
                () -> tabs[selected.getAsInt()].height(), kids.toArray(JDNode[]::new));
        frame = JDViews.onWheel(frame, d -> {
            Tab tab = tabs[selected.getAsInt()];
            if (tab.rows() > tab.shown()) {
                scrollTo(menu, menu.outsideScroll() - (int) Math.signum(d));
            }
        });
        return JDViews.sidePanel(frame, JDNode.Side.RIGHT, TAB_STRIP, OVERLAP);
    }

    private static void scrollTo(ProjectBenchMenu menu, int row) {
        int max = 0;
        List<ExternalInventories.Panel> panels = menu.outsidePanels();
        if (!panels.isEmpty()) {
            ExternalInventories.Panel panel = panels.get(Mth.clamp(menu.selectedTab(), 0, panels.size() - 1));
            int rows = panel.processor() != null ? 0 : (panel.slots() + OUTSIDE_COLUMNS - 1) / OUTSIDE_COLUMNS;
            max = Math.max(0, rows - Math.min(OUTSIDE_ROWS, rows));
        }
        row = Mth.clamp(row, 0, max);
        if (row != menu.outsideScroll()) {
            BenchChoices.press(menu, ProjectBenchMenu.BUTTON_SCROLL + row);
        }
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static void addProcessor(List<JDNode> kids, ProjectBenchMenu menu, Processor processor, Tab[] tabs, IntSupplier selected) {
        BooleanSupplier open = () -> tabs[selected.getAsInt()].processor() == processor;
        IntSupplier tint = () -> tabs[selected.getAsInt()].color();
        String name = processor.name().toLowerCase(Locale.ROOT);
        String id = "machine-" + name;
        kids.add(JDViews.at(JDViews.shownWhen(JDViews.sprite(id, MACHINE_WIDTH, MACHINE_HEIGHT, vanilla("textures/gui/container/" + name + ".png"),
                MACHINE_U, MACHINE_V, tint), open), MACHINE_U, MACHINE_V));
        if (processor.brewing()) {
            kids.add(bar(id + "-fuel", 18, 4, "container/brewing_stand/fuel_length", Grow.RIGHT,
                    () -> Mth.clamp((18 * menu.processorData(1) + 20 - 1) / 20, 0, 18), tint, open, 60, 44));
            kids.add(bar(id + "-arrow", 9, 28, "container/brewing_stand/brew_progress", Grow.DOWN, () -> {
                int brewTime = menu.processorData(0);
                return brewTime > 0 ? (int) (28F * (1F - brewTime / 400F)) : 0;
            }, tint, open, 97, 16));
            kids.add(bar(id + "-bubbles", 12, 29, "container/brewing_stand/bubbles", Grow.UP, () -> {
                int brewTime = menu.processorData(0);
                return brewTime > 0 ? BUBBLES[brewTime / 2 % BUBBLES.length] : 0;
            }, tint, open, 63, 14));
            return;
        }
        kids.add(bar(id + "-flame", 14, 14, "container/" + name + "/lit_progress", Grow.UP, () -> {
            int litTime = menu.processorData(0);
            if (litTime <= 0) {
                return 0;
            }
            int duration = menu.processorData(1) == 0 ? 200 : menu.processorData(1);
            return Mth.ceil(Mth.clamp(litTime / (float) duration, 0F, 1F) * 13F) + 1;
        }, tint, open, 56, 36));
        kids.add(bar(id + "-arrow", 24, 16, "container/" + name + "/burn_progress", Grow.RIGHT, () -> {
            int progress = menu.processorData(2);
            int total = menu.processorData(3);
            return Mth.ceil((total != 0 && progress != 0 ? Mth.clamp(progress / (float) total, 0F, 1F) : 0F) * 24F);
        }, tint, open, 79, 34));
    }

    private static JDNode bar(String id, int w, int h, String sprite, Grow grow, IntSupplier pixels, IntSupplier tint,
                              BooleanSupplier open, int x, int y) {
        return JDViews.at(JDViews.shownWhen(JDViews.spriteBar(id, w, h, vanilla(sprite), grow, pixels, tint), open), x, y);
    }
}
