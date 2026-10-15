package com.darkona.projectbench.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntFunction;

public class IconButton extends Button {
    private final ResourceLocation texture;
    private int u;
    private int v;
    private final int size;
    private final Component help;
    private int[] stateU = new int[0];
    private int[] stateV = new int[0];
    private Component[] stateNames = new Component[0];
    private Tooltip[] stateTooltips = new Tooltip[0];
    private int shown = -1;

    public record Look(int u, int v, String nameKey) {
    }

    public IconButton(int x, int y, int width, int height, Component name, Component help, ResourceLocation texture, int u, int v, int size,
                      OnPress onPress) {
        super(x, y, width, height, name, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.size = size;
        this.help = help;
        setTooltip(tooltip(name, help));
    }

    private static Tooltip tooltip(Component name, Component help) {
        return Tooltip.create(name.copy().append("\n").append(help.copy().withStyle(ChatFormatting.GRAY)));
    }

    public IconButton states(int count, IntFunction<Look> look) {
        stateU = new int[count];
        stateV = new int[count];
        stateNames = new Component[count];
        stateTooltips = new Tooltip[count];
        for (int state = 0; state < count; ++state) {
            Look l = look.apply(state);
            stateU[state] = l.u();
            stateV[state] = l.v();
            stateNames[state] = Component.translatable(l.nameKey());
            stateTooltips[state] = tooltip(stateNames[state], help);
        }
        shown = -1;
        return this;
    }

    public boolean show(int state) {
        if (state == shown) {
            return false;
        }
        shown = state;
        setIcon(stateU[state], stateV[state]);
        setTooltip(stateTooltips[state]);
        setMessage(stateNames[state]);
        return true;
    }

    public void setIcon(int u, int v) {
        this.u = u;
        this.v = v;
    }

    public void setHelp(Component name, Component help) {
        setTooltip(tooltip(name, help));
        setMessage(name);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(g, mouseX, mouseY, partialTick);
        if (!active) {
            g.setColor(0.5F, 0.5F, 0.5F, 1F);
        }
        g.blit(texture, getX() + 2, getY() + 2, u, v, size, size);
        g.setColor(1F, 1F, 1F, 1F);
    }

    @Override
    public void renderString(GuiGraphics g, Font font, int color) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }
}
