package com.ohores;

import com.ohores.block.ModBlockEntities;
import com.ohores.menu.ModMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * OhOres - adds the Rare Ore Extractor, a configurable block that slowly turns cheap
 * "consumable" blocks (dirt, cobblestone, ...) into a selected ore item.
 */
@Mod(OhOres.MODID)
public class OhOres {
    public static final String MODID = "ohores";

    public OhOres(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlocks.CREATIVE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}
