package com.darkona.projectbench.client;

import net.minecraft.client.gui.GuiGraphics;

final class Draw {
    static final int OUTLINE = 0xFF000000;
    static final int LIGHT = 0xFFFFFFFF;
    static final int SHADOW = 0xFF555555;
    static final int PANEL = 0xFFC6C6C6;
    static final int EDGE = 0xFF373737;
    static final int SLOT = 0xFF8B8B8B;
    static final int SCROLLBAR = 0xFF555555;
    static final int SLOT_SIZE = 18;

    private Draw() {
    }

    static void slot(GuiGraphics g, int x, int y, int fill) {
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, LIGHT);
        g.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, EDGE);
        g.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, fill);
    }

    static void frame(GuiGraphics g, int x, int y, int width, int height, int shadow, int face) {
        g.fill(x + 1, y, x + width - 1, y + height, OUTLINE);
        g.fill(x, y + 1, x + width, y + height - 1, OUTLINE);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, LIGHT);
        g.fill(x + 3, y + 3, x + width - 1, y + height - 1, shadow);
        g.fill(x + 3, y + 3, x + width - 3, y + height - 3, face);
    }

    static void scrollThumb(GuiGraphics g, int x, int top, int width, int track, int shown, int total, int scroll, int color) {
        int bar = Math.max(8, track * shown / total);
        int at = (track - bar) * scroll / (total - shown);
        g.fill(x, top + at, x + width, top + at + bar, color);
    }
}
