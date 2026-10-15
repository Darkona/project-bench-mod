package com.darkona.projectbench.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class RecipeSelector {
    public static final int BUTTON = 10;
    private static final int CELL = 20;

    private static final int ROW_BOTTOM = -2;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int CHOSEN = 0xFFFFD84A;

    private static boolean own = true;

    private final RecipeChoices screen;

    private final int buttonX;
    private final int buttonY;
    private final int resultCenterX;
    private boolean open;

    private ItemStack[] results = new ItemStack[0];
    private int seenRevision = -1;

    private RecipeSelector(RecipeChoices screen, int buttonX, int buttonY, int resultCenterX) {
        this.screen = screen;
        this.buttonX = buttonX;
        this.buttonY = buttonY;
        this.resultCenterX = resultCenterX;
    }

    @Nullable
    public static RecipeSelector forWindow(RecipeChoices screen, int buttonX, int buttonY, int resultCenterX) {
        return own ? new RecipeSelector(screen, buttonX, buttonY, resultCenterX) : null;
    }

    public static void useExternal() {
        own = false;
    }

    private void refresh(HolderLookup.Provider registries) {
        if (seenRevision == screen.revision()) {
            return;
        }
        seenRevision = screen.revision();
        results = new ItemStack[screen.recipeCount()];
        for (int i = 0; i < results.length; ++i) {
            results[i] = screen.recipeResult(i, registries);
        }
        if (results.length < 2) {
            open = false;
        }
    }

    public void render(GuiGraphics g, Font font, HolderLookup.Provider registries, int left, int top, int mouseX, int mouseY) {
        refresh(registries);
        if (results.length < 2) {
            return;
        }
        int bx = left + buttonX;
        int by = top + buttonY;
        boolean overButton = inside(mouseX, mouseY, bx, by, BUTTON, BUTTON);
        g.fill(bx, by, bx + BUTTON, by + BUTTON, overButton || open ? HIGHLIGHT : Draw.EDGE);
        g.fill(bx + 1, by + 1, bx + BUTTON - 1, by + BUTTON - 1, Draw.SLOT);

        for (int d = 0; d < 3; ++d) {
            g.fill(bx + 2 + 2 * d, by + 4, bx + 3 + 2 * d, by + 6, Draw.EDGE);
        }
        if (!open) {
            return;
        }
        g.pose().pushPose();

        g.pose().translate(0, 0, 300);
        int x0 = rowLeft(left);
        int y0 = top + ROW_BOTTOM - CELL - 2;
        g.fill(x0 - 2, y0 - 2, x0 + CELL * results.length + 2, y0 + CELL + 2, Draw.EDGE);
        int chosen = screen.choice();
        int hovered = -1;
        for (int i = 0; i < results.length; ++i) {
            int x = x0 + CELL * i;
            boolean over = inside(mouseX, mouseY, x, y0, CELL, CELL);
            g.fill(x, y0, x + CELL, y0 + CELL, i == chosen ? CHOSEN : over ? HIGHLIGHT : Draw.SLOT);
            g.fill(x + 1, y0 + 1, x + CELL - 1, y0 + CELL - 1, Draw.PANEL);
            g.renderItem(results[i], x + 2, y0 + 2);
            g.renderItemDecorations(font, results[i], x + 2, y0 + 2);
            if (over) {
                hovered = i;
            }
        }
        g.pose().popPose();
        if (hovered >= 0 && !results[hovered].isEmpty()) {
            g.renderTooltip(font, results[hovered], mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int left, int top) {
        if (results.length < 2) {
            return false;
        }
        if (inside(mouseX, mouseY, left + buttonX, top + buttonY, BUTTON, BUTTON)) {
            open = !open;
            return true;
        }
        if (!open) {
            return false;
        }
        open = false;
        int x0 = rowLeft(left);
        int y0 = top + ROW_BOTTOM - CELL - 2;
        if (mouseY >= y0 && mouseY < y0 + CELL && mouseX >= x0 && mouseX < x0 + CELL * results.length) {
            screen.selectRecipe((int) ((mouseX - x0) / CELL));
            return true;
        }
        return false;
    }

    private int rowLeft(int left) {
        return left + resultCenterX - CELL * results.length / 2;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
