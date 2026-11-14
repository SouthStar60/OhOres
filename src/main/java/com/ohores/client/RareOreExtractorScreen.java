package com.ohores.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntConsumer;

import com.ohores.menu.RareOreExtractorMenu;
import com.ohores.network.SetTargetPayload;
import com.ohores.network.SyncedConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * The extractor GUI. It reuses the vanilla furnace texture so it looks like a vanilla container:
 * input slot, the "fuel" position shows the current target, right slot is the output.
 * Clicking the target opens an in place, searchable ore list; the "!" button opens the list of
 * allowed consumables. Both are overlays of this screen, so the container itself stays open.
 */
public class RareOreExtractorScreen extends AbstractContainerScreen<RareOreExtractorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    /** GUI sprite id of the vanilla 18x18 slot frame (blitSprite wants "container/slot", not a path). */
    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");
    /** GUI sprite id of the vanilla progress fill, cropped exactly like the furnace screen does. */
    private static final ResourceLocation BURN_PROGRESS =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    private static final int ARROW_X = 88;
    private static final int ARROW_Y = 21;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;

    private static final int INFO_SIZE = 13;
    private static final int INFO_Y = 54;

    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 196;
    private static final int LIST_X = 10;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_TOP = 48;
    private static final int SCROLL_H = PANEL_HEIGHT - 76;
    private static final int SCROLL_X = LIST_X + 190 + 2;
    private static final int LIST_WIDTH = PANEL_WIDTH - 32;

    private boolean overlayOpen;
    private boolean consumableView;
    private EditBox search;
    private PickList list;
    private int panelLeft;
    private int panelTop;
    private boolean scrollDragging;
    private double scrollGrab;

    public RareOreExtractorScreen(RareOreExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = (this.height - PANEL_HEIGHT) / 2;

        this.search = new EditBox(this.font, this.panelLeft + 10, this.panelTop + 24,
                PANEL_WIDTH - 20, 18, Component.translatable("gui.ohores.search"));
        this.search.setHint(Component.translatable("gui.ohores.search"));
        this.search.setMaxLength(64);
        this.search.setResponder(text -> this.refreshList());

        this.list = new PickList(this.minecraft, LIST_WIDTH, PANEL_HEIGHT - 76, this.panelTop + 48, 20);
        this.list.setX(this.panelLeft + LIST_X);

        // vanilla button, but small: bottom right corner of the machine area, above the inventory
        Button info = Button.builder(Component.literal("!"), button -> this.openConsumables())
                .bounds(this.leftPos + this.imageWidth - 6 - INFO_SIZE, this.topPos + INFO_Y, INFO_SIZE, INFO_SIZE)
                .build();
        info.setTooltip(Tooltip.create(Component.translatable("gui.ohores.consumable_title")));
        this.addRenderableWidget(info);


    }

    // ------------------------------------------------------------------ overlay

    private void openPicker() {
        this.consumableView = false;
        this.overlayOpen = true;
        this.search.setValue("");
        this.search.setFocused(true);
        this.setFocused(this.search);
        this.refreshList();
    }

    private void openConsumables() {
        this.consumableView = true;
        this.overlayOpen = true;
        this.search.setValue("");
        this.refreshList();
    }

    private void closeOverlay() {
        this.overlayOpen = false;
        this.scrollDragging = false;
        this.search.setFocused(false);
        this.setFocused(null);
    }

    private void refreshList() {
        if (this.list == null) {
            return;
        }
        this.list.setRows(this.buildRows(this.search == null ? "" : this.search.getValue()),
                this.consumableView ? null : this::pick);
    }

    private List<PickList.Row> buildRows(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<PickList.Row> rows = new ArrayList<>();

        if (!SyncedConfig.isSynced()) {
            return rows;
        }

        if (this.consumableView) {
            for (SyncedConfig.Consumable consumable : SyncedConfig.consumables()) {
                if (!matches(needle, consumable.displayName(), consumable.id())) {
                    continue;
                }
                Component detail = consumable.missing()
                        ? Component.translatable("gui.ohores.missing_consumable")
                        : Component.translatable("gui.ohores.grade",
                                SyncedConfig.formatGrade(consumable.grade()));
                rows.add(new PickList.Row(consumable.stack(), consumable.displayName(), detail.getString(), -1,
                        consumable.missing()));
            }
            sortBrokenLast(rows);
            return rows;
        }

        List<SyncedConfig.Ore> ores = SyncedConfig.ores();
        for (int i = 0; i < ores.size(); i++) {
            SyncedConfig.Ore ore = ores.get(i);
            if (!matches(needle, ore.displayName(), ore.id())) {
                continue;
            }
            Component detail = ore.missing()
                    ? Component.translatable("gui.ohores.missing_ore")
                    : Component.translatable("gui.ohores.ore_row", ore.seconds(), ore.consume());
            rows.add(new PickList.Row(ore.stack(), ore.displayName(), detail.getString(), i, ore.missing()));
        }
        sortBrokenLast(rows);
        return rows;
    }

    private static boolean matches(String needle, String name, String id) {
        return needle.isEmpty()
                || name.toLowerCase(Locale.ROOT).contains(needle)
                || id.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void pick(int index) {
        PacketDistributor.sendToServer(new SetTargetPayload(index));
        this.closeOverlay();
    }

    private void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // slot items use their own render type and would be flushed after a plain fill, so submit
        // everything drawn so far first and then draw the overlay into a fresh batch
        guiGraphics.flush();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 250.0F);

        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        GuiPanel.draw(guiGraphics, this.panelLeft, this.panelTop, PANEL_WIDTH, PANEL_HEIGHT);

        Component title = Component.translatable(this.consumableView
                ? "gui.ohores.consumable_title" : "gui.ohores.select_title");
        guiGraphics.drawString(this.font, title, this.panelLeft + 10, this.panelTop + 7, 0x404040, false);

        if (this.consumableView) {
            guiGraphics.drawString(this.font,
                    Component.translatable("gui.ohores.consumable_hint"),
                    this.panelLeft + 10, this.panelTop + 21, 0x707070, false);
        } else {
            this.search.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawScrollbar(guiGraphics);

        if (this.list.rowCount() == 0) {
            Component empty = Component.translatable(this.consumableView
                    ? "gui.ohores.consumable_empty" : "gui.ohores.no_match");
            int x = this.panelLeft + (PANEL_WIDTH - this.font.width(empty)) / 2;
            guiGraphics.drawString(this.font, empty, x, this.panelTop + 100, 0x707070, false);
        }

        guiGraphics.pose().popPose();
    }

    // ------------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.overlayOpen) {
            // no super.render(): slot items are their own render batch and always end up on top
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
            this.renderLabels(guiGraphics, mouseX, mouseY);
            this.renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.isOverArrow(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.translatable("gui.ohores.remaining", this.menu.getRemainingSeconds()),
                    Component.translatable("gui.ohores.total", this.menu.getTotalSeconds())),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isOverTargetSlot()) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.ohores.click_target"), mouseX, mouseY);
        }
    }

    /**
     * The vanilla furnace background is used only for its frame and player inventory, the machine
     * area is repainted flat grey so the flame and the furnace specific slot layout disappear.
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        guiGraphics.fill(this.leftPos + 6, this.topPos + 6, this.leftPos + this.imageWidth - 6,
                this.topPos + 70, 0xFFC6C6C6);

        this.drawSlot(guiGraphics, RareOreExtractorMenu.INPUT_X);
        // no frame under the target: it is a display only, not a slot you put things into
        this.drawSlot(guiGraphics, RareOreExtractorMenu.OUTPUT_X);

        guiGraphics.blit(TEXTURE, this.leftPos + ARROW_X, this.topPos + ARROW_Y, 79, 34,
                ARROW_WIDTH, ARROW_HEIGHT);

        int percent = this.menu.getProgressPercent();
        if (percent > 0) {
            int width = Math.max(1, ARROW_WIDTH * percent / 100);
            // same call as the vanilla furnace: cropped, not squeezed
            guiGraphics.blitSprite(BURN_PROGRESS, 24, 16, 0, 0, this.leftPos + ARROW_X,
                    this.topPos + ARROW_Y, width, ARROW_HEIGHT);
        }
    }

    /** Unresolved tags and unknown ids belong to the bottom of the list, not between real entries. */
    private static void sortBrokenLast(List<PickList.Row> rows) {
        rows.sort(java.util.Comparator.comparing(PickList.Row::missing));
    }

    /** Blits the vanilla 18x18 slot frame, so every slot looks exactly like a vanilla one. */
    private void drawSlot(GuiGraphics guiGraphics, int slotX) {
        guiGraphics.blitSprite(SLOT_SPRITE, this.leftPos + slotX - 1,
                this.topPos + RareOreExtractorMenu.MACHINE_SLOT_Y - 1, 18, 18);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0x404040, false);

        guiGraphics.drawString(this.font, Component.translatable("gui.ohores.input"),
                RareOreExtractorMenu.INPUT_X, 43, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.ohores.target"),
                RareOreExtractorMenu.TARGET_X, 43, 0x404040, false);

        int total = this.menu.getTotalSeconds();
        int percent = this.menu.getProgressPercent();
        Component state;
        if (total < 0) {
            state = Component.translatable("gui.ohores.missing_ore");
        } else if (total == 0) {
            state = Component.translatable("gui.ohores.none");
        } else if (percent > 0) {
            state = Component.translatable("gui.ohores.remaining", (total * (100 - percent) + 99) / 100);
        } else {
            state = Component.translatable("gui.ohores.total", total);
        }
        // no drop shadow here: at this scale it just blurs the digits
        guiGraphics.drawString(this.font, state, this.imageWidth / 2 - this.font.width(state) / 2, 58,
                0x404040, false);
    }

    // -------------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.overlayOpen) {
            if (!this.consumableView && this.search.mouseClicked(mouseX, mouseY, button)) {
                this.search.setFocused(true);
                return true;
            }
            if (this.scrollbarClicked(mouseX, mouseY)) {
                return true;
            }
            if (this.list.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (!this.isInsidePanel(mouseX, mouseY)) {
                this.closeOverlay();
            }
            return true;
        }

        Slot hovered = this.getSlotUnderMouse();
        if (button == 0 && hovered != null && hovered.index == RareOreExtractorMenu.SLOT_TARGET) {
            this.openPicker();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.overlayOpen) {
            if (this.scrollDragging) {
                this.applyScrollTo(mouseY);
                return true;
            }
            // the scrollbar lives inside the list widget: without this the thumb is grabbed but never moves
            this.list.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.overlayOpen) {
            this.scrollDragging = false;
            this.list.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.overlayOpen) {
            return this.list.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.overlayOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.closeOverlay();
                return true;
            }
            if (!this.consumableView) {
                this.search.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.overlayOpen) {
            return !this.consumableView && this.search.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= this.panelLeft && mouseX < this.panelLeft + PANEL_WIDTH
                && mouseY >= this.panelTop && mouseY < this.panelTop + PANEL_HEIGHT;
    }

    // ------------------------------------------------------------ own scrollbar

    private int scrollThumbHeight() {
        int max = this.list.getMaxScroll();
        if (max <= 0) {
            return 0;
        }
        long content = Math.max(1L, (long) Math.max(1, this.list.rowCount()) * 20L);
        int thumb = (int) ((long) SCROLL_H * SCROLL_H / content);
        return Math.max(16, Math.min(SCROLL_H, thumb));
    }

    private int scrollThumbY() {
        int max = this.list.getMaxScroll();
        if (max <= 0) {
            return this.panelTop + SCROLL_TOP;
        }
        int travel = Math.max(0, SCROLL_H - this.scrollThumbHeight());
        double ratio = Math.max(0.0D, Math.min(1.0D, this.list.getScrollAmount() / (double) max));
        return this.panelTop + SCROLL_TOP + (int) Math.round(travel * ratio);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (this.list.getMaxScroll() <= 0) {
            return false;
        }
        int x = this.panelLeft + SCROLL_X;
        return mouseX >= x - 2 && mouseX < x + SCROLL_W + 2
                && mouseY >= this.panelTop + SCROLL_TOP
                && mouseY < this.panelTop + SCROLL_TOP + SCROLL_H;
    }

    private boolean scrollbarClicked(double mouseX, double mouseY) {
        if (!this.isOverScrollbar(mouseX, mouseY)) {
            return false;
        }
        int thumbY = this.scrollThumbY();
        int thumbH = this.scrollThumbHeight();
        if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
            this.scrollGrab = mouseY - thumbY;
        } else {
            // clicking the empty track jumps the thumb to the pointer
            this.scrollGrab = thumbH / 2.0D;
            this.applyScrollTo(mouseY);
        }
        this.scrollDragging = true;
        return true;
    }

    private void applyScrollTo(double mouseY) {
        int max = this.list.getMaxScroll();
        if (max <= 0) {
            return;
        }
        int travel = Math.max(1, SCROLL_H - this.scrollThumbHeight());
        double relative = mouseY - (this.panelTop + SCROLL_TOP) - this.scrollGrab;
        double ratio = Math.max(0.0D, Math.min(1.0D, relative / travel));
        this.list.setClampedScrollAmount(ratio * max);
    }

    private void drawScrollbar(GuiGraphics guiGraphics) {
        int max = this.list.getMaxScroll();
        if (max <= 0) {
            return;
        }
        int x = this.panelLeft + SCROLL_X;
        int top = this.panelTop + SCROLL_TOP;
        int thumbY = this.scrollThumbY();
        int thumbH = this.scrollThumbHeight();
        int color = this.scrollDragging ? 0xFFFFFFFF : 0xFF8B8B8B;

        guiGraphics.fill(x, top, x + SCROLL_W, top + SCROLL_H, 0x33000000);
        guiGraphics.fill(x + 1, thumbY, x + SCROLL_W - 1, thumbY + thumbH, color);
        guiGraphics.fill(x + 1, thumbY, x + SCROLL_W - 1, thumbY + 1, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, thumbY + thumbH - 1, x + SCROLL_W - 1, thumbY + thumbH, 0xFF5A5A5A);
    }

    private boolean isOverArrow(int mouseX, int mouseY) {
        return mouseX >= this.leftPos + ARROW_X && mouseX < this.leftPos + ARROW_X + ARROW_WIDTH
                && mouseY >= this.topPos + ARROW_Y && mouseY < this.topPos + ARROW_Y + ARROW_HEIGHT;
    }

    private boolean isOverTargetSlot() {
        Slot slot = this.getSlotUnderMouse();
        return slot != null && slot.index == RareOreExtractorMenu.SLOT_TARGET;
    }

    // ----------------------------------------------------------------- the list

    /** Vanilla looking scroll list; rows with an index are clickable, others are inert. */
    static class PickList extends ObjectSelectionList<PickList.Row> {
        private IntConsumer onPick;
        private int rowCount;

        PickList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
            super(minecraft, width, height, y0, itemHeight);
        }

        void setRows(List<Row> rows, IntConsumer onPick) {
            this.clearEntries();
            for (Row row : rows) {
                this.addEntry(row);
            }
            this.rowCount = rows.size();
            this.onPick = onPick;
            this.setScrollAmount(0);
        }

        int rowCount() {
            return this.rowCount;
        }

        @Override
        public int getRowWidth() {
            return 190;
        }

        // the screen paints its own scrollbar so that dragging is handled in exactly one place
        @Override
        protected boolean scrollbarVisible() {
            return false;
        }

        // the panel already draws the vanilla gray container background, so no dark list backdrop
        @Override
        protected void renderSelection(GuiGraphics guiGraphics, int rowTop, int rowBottom, int rowLeft,
                                       int rowRight, int color) {
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics guiGraphics) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Row row = this.getEntryAtPosition(mouseX, mouseY);
            if (row != null && row.index >= 0 && this.onPick != null) {
                this.onPick.accept(row.index);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        static class Row extends ObjectSelectionList.Entry<Row> {
            private final ItemStack stack;
            private final String title;
            private final String detail;
            final int index;
            private final boolean missing;

            Row(ItemStack stack, String title, String detail, int index, boolean missing) {
                this.stack = stack;
                this.title = title;
                this.detail = detail;
                this.index = index;
                this.missing = missing;
            }

            boolean missing() {
                return this.missing;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                Minecraft minecraft = Minecraft.getInstance();
                if (hovering) {
                    guiGraphics.fill(left, top, left + width, top + height, 0x30FFFFFF);
                }
                int titleX = left + 2;
                if (!this.stack.isEmpty()) {
                    guiGraphics.renderItem(this.stack, left + 2, top + 2);
                    titleX = left + 22;
                }
                guiGraphics.drawString(minecraft.font, this.title, titleX, top + 6, 0x404040, false);

                int detailWidth = minecraft.font.width(this.detail);
                guiGraphics.drawString(minecraft.font, this.detail, left + width - 6 - detailWidth, top + 6,
                        0x707070, false);
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.title + " " + this.detail);
            }
        }
    }
}
