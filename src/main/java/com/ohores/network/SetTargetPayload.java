package com.ohores.network;

import com.ohores.OhOres;
import com.ohores.block.RareOreExtractorBlockEntity;
import com.ohores.menu.RareOreExtractorMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client picked an entry of the ore list; the server validates it against its own config. */
public record SetTargetPayload(int index) implements CustomPacketPayload {
    public static final Type<SetTargetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OhOres.MODID, "set_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetTargetPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SetTargetPayload::index, SetTargetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof RareOreExtractorMenu menu) || !menu.stillValid(player)) {
                return;
            }
            if (menu.getContainer() instanceof RareOreExtractorBlockEntity extractor) {
                extractor.setTarget(this.index);
            }
        });
    }
}
