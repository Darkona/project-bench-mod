package com.darkona.projectbench.client;

import com.darkona.projectbench.Content;
import com.darkona.projectbench.block.archive.BlueprintArchiveMenu;
import dev.darkona.jdlib.api.ui.JDNode;
import dev.darkona.jdlib.api.ui.JDViews;
import dev.darkona.jdlib.api.ui.MenuView;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class BlueprintArchiveView implements MenuView<BlueprintArchiveMenu> {
    private static final ResourceLocation TEMPLATE = Content.id("ui/blueprint_archive.ui.nbt");
    private static final int SCROLL_WIDTH = 9;
    private static final int SCROLL_GAP = 3;
    private static final int CONTENT = BlueprintArchiveMenu.COLUMNS * JDNode.SLOT;
    private static final int PLAYER_SLOTS = 36;

    @Override
    public JDNode build(BlueprintArchiveMenu menu, Inventory inventory, Component title) {
        int rows = menu.rows();
        int shelfSlots = rows * BlueprintArchiveMenu.COLUMNS;
        JDNode shelf = JDViews.slotGrid("shelf-slots", menu.slots.subList(0, shelfSlots), BlueprintArchiveMenu.COLUMNS);
        if (scrolls(menu)) {
            shelf = JDViews.row("shelf-row", CONTENT + SCROLL_GAP + SCROLL_WIDTH, rows * JDNode.SLOT, SCROLL_GAP, shelf,
                    JDViews.scroll("scroll", SCROLL_WIDTH, rows * JDNode.SLOT, () -> menu.totalRows() - menu.rows(), menu::scroll, row -> scrollTo(menu, row)));
        }
        return JDViews.template(TEMPLATE)
                .text("title", () -> title)
                .text("inventory", inventory::getDisplayName)
                .place("shelf", shelf)
                .slots("player", menu.slots.subList(shelfSlots, shelfSlots + PLAYER_SLOTS));
    }

    @Override
    public boolean mouseScrolled(BlueprintArchiveMenu menu, double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrolls(menu)) {
            scrollTo(menu, menu.scroll() - (int) Math.signum(scrollY));
            return true;
        }
        return false;
    }

    private static boolean scrolls(BlueprintArchiveMenu menu) {
        return menu.totalRows() > menu.rows();
    }

    private static void scrollTo(BlueprintArchiveMenu menu, int row) {
        int clamped = Math.max(0, Math.min(menu.totalRows() - menu.rows(), row));
        Minecraft minecraft = Minecraft.getInstance();
        if (clamped != menu.scroll() && minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, clamped)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, clamped);
        }
    }
}
