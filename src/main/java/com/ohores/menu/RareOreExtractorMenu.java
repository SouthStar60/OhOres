package com.ohores.menu;

import com.ohores.Config;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RareOreExtractorMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 39;
    public static final int TARGET_X = 65;
    public static final int OUTPUT_X = 124;
    public static final int MACHINE_SLOT_Y = 20;

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_OUTPUT = 2;

    private static final int MACHINE_SLOTS = 3;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = MACHINE_SLOTS + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Container container;
    private final ContainerData data;

    /** client side constructor */
    public RareOreExtractorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(4));
    }

    public RareOreExtractorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.RARE_ORE_EXTRACTOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        this.addSlot(new Slot(container, SLOT_INPUT, INPUT_X, MACHINE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Config.isConsumable(stack.getItem());
            }
        });

        // read only preview of the currently selected ore
        this.addSlot(new Slot(container, SLOT_TARGET, TARGET_X, MACHINE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        this.addSlot(new Slot(container, SLOT_OUTPUT, OUTPUT_X, MACHINE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    /** the container is the block entity on the server and a mirror on the client */
    public Container getContainer() {
        return this.container;
    }

    public ItemStack getTargetStack() {
        return this.slots.get(1).getItem();
    }

    public int getProgressPercent() {
        return this.data.get(0);
    }

    public int getTotalSeconds() {
        return this.data.get(1);
    }

    public int getTargetIndex() {
        return this.data.get(2);
    }

    public int getTargetCount() {
        return this.data.get(3);
    }

    public int getRemainingSeconds() {
        int total = this.getTotalSeconds();
        if (total <= 0) {
            return 0;
        }
        int percent = this.getProgressPercent();
        return (int) Math.max(0, Math.round(total * (100 - percent) / 100.0D));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == 0 || index == 2) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index == SLOT_TARGET) {
            return ItemStack.EMPTY;
        } else if (Config.isConsumable(stack.getItem())) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }
}
