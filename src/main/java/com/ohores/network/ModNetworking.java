package com.ohores.network;

import com.ohores.OhOres;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = OhOres.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetworking {
    private ModNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetTargetPayload.TYPE, SetTargetPayload.STREAM_CODEC,
                (payload, context) -> payload.handle(context));
        registrar.playToClient(SyncConfigPayload.TYPE, SyncConfigPayload.STREAM_CODEC,
                (payload, context) -> payload.handle(context));
    }
}
