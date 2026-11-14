package com.ohores.network;

import com.ohores.Config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.ohores.OhOres;

/**
 * Server -> client snapshot of the (server side) configuration, so that the ore picker and the
 * consumable list can be built on the client. Entries are shipped as raw config strings and
 * parsed client side, which keeps the packet trivial.
 */
public record SyncConfigPayload(String consumables, String ores, int defaultSeconds, int defaultConsume,
                                double defaultGrade) implements CustomPacketPayload {

    public static final Type<SyncConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OhOres.MODID, "sync_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SyncConfigPayload::consumables,
                    ByteBufCodecs.STRING_UTF8, SyncConfigPayload::ores,
                    ByteBufCodecs.VAR_INT, SyncConfigPayload::defaultSeconds,
                    ByteBufCodecs.VAR_INT, SyncConfigPayload::defaultConsume,
                    ByteBufCodecs.DOUBLE, SyncConfigPayload::defaultGrade,
                    SyncConfigPayload::new);

    public static SyncConfigPayload fromConfig() {
        return new SyncConfigPayload(
                String.join("\n", Config.getConsumables().stream()
                        .map(entry -> (entry.missing() ? "?" : "") + entry.id() + "=" + entry.grade()).toList()),
                String.join("\n", Config.getEntries().stream()
                        .map(entry -> (entry.missing() ? "?" : "") + entry.id() + "=" + entry.seconds()
                                + "," + entry.consume()).toList()),
                Config.getDefaultSeconds(),
                Config.getDefaultConsume(),
                Config.getDefaultPoorGrade());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> SyncedConfig.accept(this));
    }
}
