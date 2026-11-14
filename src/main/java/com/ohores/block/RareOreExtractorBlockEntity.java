package com.ohores.block;

import javax.annotation.Nullable;

import com.ohores.Config;
import com.ohores.menu.RareOreExtractorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class RareOreExtractorBlockEntity extends BlockEntity
        implements Container, WorldlyContainer, ContainerData, net.minecraft.world.MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_SECONDS = 1;
    public static final int DATA_INDEX = 2;
    public static final int DATA_COUNT = 3;

    private static final int[] ACCESSIBLE_SLOTS = new int[]{SLOT_INPUT, SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;
    private int targetIndex;

    /** a round already ate its input, so it keeps running until it finishes */
    private boolean active;
    private int runningTicks;
    private ItemStack runningProduct = ItemStack.EMPTY;

    public RareOreExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RARE_ORE_EXTRACTOR.get(), pos, state);
    }

    // ------------------------------------------------------------------ logic

    public void serverTick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        Config.OreEntry entry = Config.getSelected(this.targetIndex);
        if (entry == null || entry.missing()) {
            if (this.active || this.progress != 0) {
                this.active = false;
                this.progress = 0;
                this.runningTicks = 0;
                this.runningProduct = ItemStack.EMPTY;
                this.setChanged();
            }
            return;
        }

        this.syncTargetStack(entry);

        if (!this.active && !this.startRound(entry)) {
            return;
        }

        this.progress++;
        if (this.progress < this.runningTicks) {
            return;
        }

        // the round was paid up front, so it always finishes even if the input was taken away
        ItemStack output = this.items.get(SLOT_OUTPUT);
        boolean produced;
        if (output.isEmpty()) {
            this.items.set(SLOT_OUTPUT, this.runningProduct.copyWithCount(1));
            produced = true;
        } else if (ItemStack.isSameItemSameComponents(output, this.runningProduct)
                && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
            produced = true;
        } else {
            // output slot is blocked, hold at 100% until the player empties it
            produced = false;
            this.progress = this.runningTicks;
        }

        if (produced) {
            this.active = false;
            this.progress = 0;
            this.runningTicks = 0;
            this.runningProduct = ItemStack.EMPTY;
            this.setChanged();
        }
    }

    /** Eats the input right away; false when the machine cannot start a round at this moment. */
    private boolean startRound(Config.OreEntry entry) {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty() || !Config.isConsumable(input.getItem())) {
            return false;
        }

        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (!output.isEmpty()
                && (!ItemStack.isSameItemSameComponents(output, new ItemStack(entry.item()))
                || output.getCount() >= output.getMaxStackSize())) {
            return false;
        }

        int consume = Config.consumeFor(input.getItem(), entry.consume());
        if (input.getCount() < consume) {
            return false;
        }

        input.shrink(consume);
        this.active = true;
        this.progress = 0;
        this.runningTicks = Math.max(1, entry.ticks());
        this.runningProduct = new ItemStack(entry.item());
        this.setChanged();
        return true;
    }

    /** Jumps to the entry at {@code index} of the configured list; out of range values wrap. */
    public void setTarget(int index) {
        int size = Config.getEntries().size();
        if (size <= 0) {
            return;
        }
        int next = Math.floorMod(index, size);
        if (next == this.targetIndex) {
            return;
        }
        this.targetIndex = next;
        if (!this.active) {
            this.progress = 0;
        }
        this.syncTargetStack(Config.getSelected(this.targetIndex));
        this.setChanged();
    }

    /** Advances the selected ore by one, wraps around the configured list. */
    public void cycleTarget() {
        setTarget(this.targetIndex + 1);
    }

    private void syncTargetStack(@Nullable Config.OreEntry entry) {
        ItemStack wanted = entry == null ? ItemStack.EMPTY : new ItemStack(entry.item(), 1);
        ItemStack shown = this.items.get(SLOT_TARGET);
        if (!ItemStack.matches(wanted, shown)) {
            this.items.set(SLOT_TARGET, wanted);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot : new int[]{SLOT_INPUT, SLOT_OUTPUT}) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    public int getProgressPercent() {
        int total = this.totalTicks();
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, this.progress * 100 / total);
    }

    /** length of the round that is running, or of the selected ore when idle */
    private int totalTicks() {
        if (this.active) {
            return this.runningTicks;
        }
        Config.OreEntry entry = Config.getSelected(this.targetIndex);
        return entry == null || entry.missing() ? 0 : entry.ticks();
    }

    public boolean isRunning() {
        return this.active;
    }

    // -------------------------------------------------------------- container

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && Config.isConsumable(stack.getItem());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == SLOT_OUTPUT;
    }

    // ------------------------------------------------------------ menu / data

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ohores.rare_ore_extractor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RareOreExtractorMenu(containerId, playerInventory, this, this);
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case DATA_PROGRESS -> this.getProgressPercent();
            case DATA_SECONDS -> {
                if (this.active) {
                    yield this.runningTicks / 20;
                }
                Config.OreEntry entry = Config.getSelected(this.targetIndex);
                // -1 tells the GUI that the selected entry does not resolve
                yield entry == null || entry.missing() ? -1 : entry.seconds();
            }
            case DATA_INDEX -> this.targetIndex;
            case DATA_COUNT -> Config.getEntries().size();
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        // server side data is push-only; the client receives it through SimpleContainerData
    }

    // ------------------------------------------------------------------- nbt

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("Progress", this.progress);
        tag.putInt("TargetIndex", this.targetIndex);
        tag.putBoolean("Active", this.active);
        tag.putInt("RunningTicks", this.runningTicks);
        tag.putString("RunningItem", BuiltInRegistries.ITEM.getKey(this.runningProduct.getItem()).toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.progress = tag.getInt("Progress");
        this.targetIndex = tag.getInt("TargetIndex");
        this.active = tag.getBoolean("Active");
        this.runningTicks = tag.getInt("RunningTicks");
        ResourceLocation running = ResourceLocation.tryParse(tag.getString("RunningItem"));
        Item runningItem = running == null ? Items.AIR : BuiltInRegistries.ITEM.get(running);
        this.runningProduct = runningItem == Items.AIR ? ItemStack.EMPTY : new ItemStack(runningItem);
        this.syncTargetStack(Config.getSelected(this.targetIndex));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
