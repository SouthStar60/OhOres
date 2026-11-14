package com.ohores.client;

import net.minecraft.client.gui.GuiGraphics;

/** Draws a plain vanilla-style container panel (the same greys vanilla uses). */
final class GuiPanel {
    private GuiPanel() {
    }

    static void draw(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF555555);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF555555);
    }
}
