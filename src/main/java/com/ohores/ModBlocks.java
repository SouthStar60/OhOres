package com.ohores;

import com.ohores.block.RareOreExtractorBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OhOres.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OhOres.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OhOres.MODID);

    public static final DeferredBlock<RareOreExtractorBlock> RARE_ORE_EXTRACTOR = BLOCKS.register(
            "rare_ore_extractor",
            () -> new RareOreExtractorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredItem<BlockItem> RARE_ORE_EXTRACTOR_ITEM = ITEMS.register(
            "rare_ore_extractor",
            () -> new BlockItem(RARE_ORE_EXTRACTOR.get(), new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register(
            "ohores",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ohores"))
                    .icon(() -> new ItemStack(RARE_ORE_EXTRACTOR.get()))
                    .displayItems((parameters, output) -> output.accept(RARE_ORE_EXTRACTOR.get()))
                    .build());

    private ModBlocks() {
    }
}
