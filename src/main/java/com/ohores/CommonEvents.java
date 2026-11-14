package com.ohores;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = OhOres.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CommonEvents {
    private static final ResourceLocation FUNCTIONAL_BLOCKS =
            ResourceLocation.withDefaultNamespace("functional_blocks");

    private CommonEvents() {
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (FUNCTIONAL_BLOCKS.equals(event.getTabKey().location())) {
            event.accept(ModBlocks.RARE_ORE_EXTRACTOR_ITEM.get());
        }
    }
}
