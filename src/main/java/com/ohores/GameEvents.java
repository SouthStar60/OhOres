package com.ohores;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Tag entries ("#c:gravels") can only be resolved once the datapack tags are bound, so the parsed
 * lists are dropped whenever tags change.
 */
@EventBusSubscriber(modid = OhOres.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class GameEvents {

    private GameEvents() {
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        Config.invalidateCaches();
    }
}
