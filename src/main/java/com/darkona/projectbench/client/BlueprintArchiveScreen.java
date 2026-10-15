package com.darkona.projectbench.client;

import com.darkona.projectbench.block.archive.BlueprintArchiveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BlueprintArchiveScreen extends AbstractContainerScreen<BlueprintArchiveMenu> {
    private static final ResourceLocation CHEST = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int BAR_X = 175;
    private static final int BAR_WIDTH = 6;
    private boolean dragging;

    public BlueprintArchiveScreen(BlueprintArchiveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 114 + menu.rows() * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int rows = menu.rows() * 18 + 17;
        g.blit(CHEST, leftPos, topPos, 0, 0, imageWidth, rows);
        g.blit(CHEST, leftPos, topPos + rows, 0, 126, imageWidth, 96);
        if (scrolls()) {
            int x = leftPos + BAR_X;
            int top = topPos + 18;
            int track = menu.rows() * 18;
            g.fill(x, top, x + BAR_WIDTH, top + track, Draw.SLOT);
            Draw.scrollThumb(g, x, top, BAR_WIDTH, track, menu.rows(), menu.totalRows(), menu.scroll(), Draw.SCROLLBAR);
        }
    }

    private boolean scrolls() {
        return menu.totalRows() > menu.rows();
    }

    private void scrollTo(int row) {
        int clamped = Math.max(0, Math.min(menu.totalRows() - menu.rows(), row));
        if (clamped != menu.scroll() && minecraft != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, clamped)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, clamped);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrolls()) {
            scrollTo(menu.scroll() - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrolls() && button == 0 && mouseX >= leftPos + BAR_X && mouseX < leftPos + BAR_X + BAR_WIDTH
                && mouseY >= topPos + 18 && mouseY < topPos + 18 + menu.rows() * 18) {
            dragging = true;
            dragTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) {
            dragTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void dragTo(double mouseY) {
        int track = menu.rows() * 18;
        double fraction = (mouseY - topPos - 18) / track;
        scrollTo((int) Math.round(fraction * (menu.totalRows() - menu.rows())));
    }
}
